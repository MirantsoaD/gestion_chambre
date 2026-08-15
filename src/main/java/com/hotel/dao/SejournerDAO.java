package com.hotel.dao;

import com.hotel.model.Sejourner;
import com.hotel.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès base de données pour la table {@code sejourner}.
 * Méthodes non statiques : l'UI fera {@code new SejournerDAO()}.
 * <p>
 * L'ajout d'un séjour (arrivée d'un client sans réservation) augmente le solde
 * de {@code prix_nuitee * nbr_jour} de façon ATOMIQUE (une seule transaction).
 */
public class SejournerDAO {

    /**
     * Enregistre l'arrivée d'un client sans réservation (walk-in) :
     * insère dans {@code sejourner}, puis crédite le solde.
     * Le tout dans une seule transaction.
     * <p>
     * La date d'entrée est fixée au jour courant si elle n'est pas renseignée.
     *
     * @throws IllegalArgumentException si la chambre n'existe pas
     */
    public void ajouter(Sejourner s) throws SQLException {
        if (s.getDateEntreeSejour() == null) {
            s.setDateEntreeSejour(LocalDate.now());
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 2) Calculer le montant : prix de la nuitée * nombre de jours.
                int prixNuitee;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT prix_nuitee FROM chambre WHERE num_chambre = ?")) {
                    ps.setString(1, s.getNumChambre());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Chambre introuvable : " + s.getNumChambre());
                        }
                        prixNuitee = rs.getInt("prix_nuitee");
                    }
                }
                int montant = prixNuitee * s.getNbrJour();
                s.setMontant(montant);

                // 3) Insérer le séjour (montant stocké pour les remboursements futurs).
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sejourner (num_chambre, date_entree_sejour, nbr_jour, "
                        + "nom_client, telephone, montant) VALUES (?,?,?,?,?,?)")) {
                    ps.setString(1, s.getNumChambre());
                    ps.setDate(2, Date.valueOf(s.getDateEntreeSejour()));
                    ps.setInt(3, s.getNbrJour());
                    ps.setString(4, s.getNomClient());
                    ps.setString(5, s.getTelephone());
                    ps.setInt(6, montant);
                    ps.executeUpdate();
                }

                // 4) Créditer le solde (même connexion => transaction).
                new SoldeDAO().ajouterAuSolde(conn, montant);

                // 5) Valider la transaction.
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Retourne tous les séjours, du plus récent (date d'entrée) au plus ancien. */
    public List<Sejourner> lister() throws SQLException {
        List<Sejourner> liste = new ArrayList<>();
        String sql = "SELECT * FROM sejourner ORDER BY date_entree_sejour DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(mapper(rs));
            }
        }
        return liste;
    }

    /**
     * Modifie un séjour.
     * Ne touche PAS la date d'entrée.
     * Le solde est rectifié du delta entre le nouveau montant (chambre * jours)
     * et le montant stocké.
     *
     * @throws IllegalArgumentException si le séjour ou la chambre n'existe pas
     */
    public void modifier(Sejourner s) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Lire l'ancien montant stocké du séjour.
                int ancienMontant;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT montant FROM sejourner WHERE id_sejour = ?")) {
                    ps.setInt(1, s.getIdSejour());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Séjour introuvable : " + s.getIdSejour());
                        }
                        ancienMontant = rs.getInt("montant");
                    }
                }

                // 2) Calculer le nouveau montant avec la chambre choisie dans le formulaire.
                int prixNuitee;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT prix_nuitee FROM chambre WHERE num_chambre = ?")) {
                    ps.setString(1, s.getNumChambre());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Chambre introuvable : " + s.getNumChambre());
                        }
                        prixNuitee = rs.getInt("prix_nuitee");
                    }
                }
                int nouveauMontant = prixNuitee * s.getNbrJour();

                // 3) Mettre à jour le séjour (avec le nouveau montant stocké).
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE sejourner SET num_chambre = ?, nbr_jour = ?, "
                        + "nom_client = ?, telephone = ?, montant = ? WHERE id_sejour = ?")) {
                    ps.setString(1, s.getNumChambre());
                    ps.setInt(2, s.getNbrJour());
                    ps.setString(3, s.getNomClient());
                    ps.setString(4, s.getTelephone());
                    ps.setInt(5, nouveauMontant);
                    ps.setInt(6, s.getIdSejour());
                    ps.executeUpdate();
                }

                // 4) Rectifier le solde du delta (nouveau montant - ancien montant).
                int delta = nouveauMontant - ancienMontant;
                if (delta != 0) {
                    new SoldeDAO().ajouterAuSolde(conn, delta);
                }

                // 5) Mettre à jour l'objet passé en paramètre.
                s.setMontant(nouveauMontant);

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
     * Supprime le séjour portant cet identifiant et rembourse le montant stocké
     * (le solde diminue du montant crédité à l'enregistrement du séjour).
     *
     * @throws IllegalArgumentException si le séjour n'existe pas
     */
    public void supprimer(int idSejour) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Lire le montant stocké du séjour.
                int montant;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT montant FROM sejourner WHERE id_sejour = ?")) {
                    ps.setInt(1, idSejour);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new IllegalArgumentException(
                                    "Séjour introuvable : " + idSejour);
                        }
                        montant = rs.getInt("montant");
                    }
                }

                // 2) Supprimer le séjour.
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM sejourner WHERE id_sejour = ?")) {
                    ps.setInt(1, idSejour);
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

    /** Convertit la ligne courante du ResultSet en objet Sejourner. */
    private Sejourner mapper(ResultSet rs) throws SQLException {
        Sejourner s = new Sejourner();
        s.setIdSejour(rs.getInt("id_sejour"));
        s.setNumChambre(rs.getString("num_chambre"));
        s.setDateEntreeSejour(rs.getDate("date_entree_sejour").toLocalDate());
        s.setNbrJour(rs.getInt("nbr_jour"));
        s.setNomClient(rs.getString("nom_client"));
        s.setTelephone(rs.getString("telephone"));
        s.setMontant(rs.getInt("montant"));
        return s;
    }
}
