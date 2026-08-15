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

                // 3) Insérer l'occupation.
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO occuper (id_reserv) VALUES (?)")) {
                    ps.setInt(1, o.getIdReserv());
                    ps.executeUpdate();
                }

                // 4) Calculer le montant : prix de la nuitée * nombre de jours de la réservation.
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
                liste.add(o);
            }
        }
        return liste;
    }

    /**
     * Supprime une occupation.
     * NB : le solde n'est PAS décrémenté (règle métier : on ne rembourse pas).
     */
    public void supprimer(int idOccup) throws SQLException {
        String sql = "DELETE FROM occuper WHERE id_occup = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOccup);
            ps.executeUpdate();
        }
    }
}
