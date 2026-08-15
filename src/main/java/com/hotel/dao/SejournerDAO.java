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
                // 2) Insérer le séjour.
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sejourner (num_chambre, date_entree_sejour, nbr_jour, "
                        + "nom_client, telephone) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, s.getNumChambre());
                    ps.setDate(2, Date.valueOf(s.getDateEntreeSejour()));
                    ps.setInt(3, s.getNbrJour());
                    ps.setString(4, s.getNomClient());
                    ps.setString(5, s.getTelephone());
                    ps.executeUpdate();
                }

                // 3) Calculer le montant : prix de la nuitée * nombre de jours.
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
     */
    public void modifier(Sejourner s) throws SQLException {
        String sql = "UPDATE sejourner SET num_chambre = ?, nbr_jour = ?, "
                + "nom_client = ?, telephone = ? WHERE id_sejour = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNumChambre());
            ps.setInt(2, s.getNbrJour());
            ps.setString(3, s.getNomClient());
            ps.setString(4, s.getTelephone());
            ps.setInt(5, s.getIdSejour());
            ps.executeUpdate();
        }
    }

    /** Supprime le séjour portant cet identifiant. */
    public void supprimer(int idSejour) throws SQLException {
        String sql = "DELETE FROM sejourner WHERE id_sejour = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSejour);
            ps.executeUpdate();
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
        return s;
    }
}
