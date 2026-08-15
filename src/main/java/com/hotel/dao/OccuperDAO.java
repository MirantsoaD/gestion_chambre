package com.hotel.dao;

import com.hotel.model.Occuper;
import com.hotel.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès base de données pour la table {@code occuper}.
 * Méthodes non statiques : l'UI fera {@code new OccuperDAO()}.
 * <p>
 * L'ajout d'une occupation (arrivée d'un client réservé) augmente le solde
 * de {@code prix_nuitee * nbr_jour} de façon ATOMIQUE (une seule transaction).
 */
public class OccuperDAO {

    /**
     * Enregistre l'arrivée d'un client ayant réservé :
     * vérifie la réservation, insère dans {@code occuper}, puis crédite le solde.
     * Le tout dans une seule transaction.
     *
     * @throws IllegalArgumentException si la réservation n'existe pas
     */
    public void ajouter(Occuper o) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 2) Vérifier que la réservation existe.
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id_reserv FROM reserver WHERE id_reserv = ?")) {
                    ps.setInt(1, o.getIdReserv());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Réservation introuvable : " + o.getIdReserv());
                        }
                    }
                }

                // 3) Calculer le montant : prix de la nuitée * nombre de jours de la réservation.
                int montant = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT c.prix_nuitee * r.nbr_jour AS montant "
                        + "FROM reserver r JOIN chambre c ON c.num_chambre = r.num_chambre "
                        + "WHERE r.id_reserv = ?")) {
                    ps.setInt(1, o.getIdReserv());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            montant = rs.getInt("montant");
                        }
                    }
                }
                o.setMontant(montant);

                // 4) Insérer l'occupation (montant stocké pour les remboursements futurs).
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO occuper (id_reserv, montant) VALUES (?, ?)")) {
                    ps.setInt(1, o.getIdReserv());
                    ps.setInt(2, montant);
                    ps.executeUpdate();
                }

                // 5) Créditer le solde (même connexion => transaction).
                new SoldeDAO().ajouterAuSolde(conn, montant);

                // 6) Valider la transaction.
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Retourne toutes les occupations, triées par identifiant. */
    public List<Occuper> lister() throws SQLException {
        List<Occuper> liste = new ArrayList<>();
        String sql = "SELECT * FROM occuper ORDER BY id_occup";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Occuper o = new Occuper();
                o.setIdOccup(rs.getInt("id_occup"));
                o.setIdReserv(rs.getInt("id_reserv"));
                o.setMontant(rs.getInt("montant"));
                liste.add(o);
            }
        }
        return liste;
    }

    /**
     * Supprime une occupation et rembourse le montant stocké
     * (le solde diminue du montant crédité à l'arrivée).
     *
     * @throws IllegalArgumentException si l'occupation n'existe pas
     */
    public void supprimer(int idOccup) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Lire le montant stocké de l'occupation.
                int montant;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT montant FROM occuper WHERE id_occup = ?")) {
                    ps.setInt(1, idOccup);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Occupation introuvable : " + idOccup);
                        }
                        montant = rs.getInt("montant");
                    }
                }

                // 2) Supprimer l'occupation.
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM occuper WHERE id_occup = ?")) {
                    ps.setInt(1, idOccup);
                    ps.executeUpdate();
                }

                // 3) Rembourser le montant (delta négatif ; 0 pour l'ancien historique).
                new SoldeDAO().ajouterAuSolde(conn, -montant);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Relie l'occupation {@code idOccup} à la réservation {@code idReserv} et rectifie
     * le solde du delta entre le nouveau montant (prix * jours de la nouvelle
     * réservation) et le montant stocké. Transactionnel.
     *
     * @throws IllegalArgumentException si l'occupation n'existe pas, si la réservation
     *                                  cible est introuvable/annulée, ou si elle est déjà occupée
     */
    public void modifier(int idOccup, int idReserv) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Lire le montant stocké de l'occupation.
                int ancienMontant;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT montant FROM occuper WHERE id_occup = ?")) {
                    ps.setInt(1, idOccup);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Occupation introuvable : " + idOccup);
                        }
                        ancienMontant = rs.getInt("montant");
                    }
                }

                // 2) Vérifier que la réservation cible existe et n'est pas annulée.
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id_reserv FROM reserver WHERE id_reserv = ? AND annulee = FALSE")) {
                    ps.setInt(1, idReserv);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Réservation introuvable ou annulée : " + idReserv);
                        }
                    }
                }

                // 3) Vérifier que la réservation cible n'est pas déjà occupée.
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id_occup FROM occuper WHERE id_reserv = ?")) {
                    ps.setInt(1, idReserv);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Cette réservation est déjà occupée : " + idReserv);
                        }
                    }
                }

                // 4) Calculer le nouveau montant : prix de la nuitée * nombre de jours.
                int nouveauMontant;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT c.prix_nuitee * r.nbr_jour AS montant "
                        + "FROM reserver r JOIN chambre c ON c.num_chambre = r.num_chambre "
                        + "WHERE r.id_reserv = ?")) {
                    ps.setInt(1, idReserv);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Réservation introuvable : " + idReserv);
                        }
                        nouveauMontant = rs.getInt("montant");
                    }
                }

                // 5) Relier l'occupation à la nouvelle réservation.
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE occuper SET id_reserv = ?, montant = ? WHERE id_occup = ?")) {
                    ps.setInt(1, idReserv);
                    ps.setInt(2, nouveauMontant);
                    ps.setInt(3, idOccup);
                    ps.executeUpdate();
                }

                // 6) Rectifier le solde du delta.
                int delta = nouveauMontant - ancienMontant;
                if (delta != 0) {
                    new SoldeDAO().ajouterAuSolde(conn, delta);
                }

                // 7) Valider la transaction.
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
