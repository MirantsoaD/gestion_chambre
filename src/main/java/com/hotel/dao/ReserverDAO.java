package com.hotel.dao;

import com.hotel.model.Reserver;
import com.hotel.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès base de données pour la table {@code reserver}.
 * Méthodes non statiques : l'UI fera {@code new ReserverDAO()}.
 */
public class ReserverDAO {

    /**
     * Insère une réservation.
     * La date de réservation est fixée au jour courant si elle n'est pas renseignée.
     * La réservation est créée non annulée ({@code annulee = FALSE}).
     */
    public void ajouter(Reserver r) throws SQLException {
        if (r.getDateReserv() == null) {
            r.setDateReserv(LocalDate.now());
        }
        String sql = "INSERT INTO reserver "
                + "(num_chambre, date_reserv, date_entree, nbr_jour, nom_client, mail, annulee) "
                + "VALUES (?,?,?,?,?,?,FALSE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getNumChambre());
            ps.setDate(2, Date.valueOf(r.getDateReserv()));
            ps.setDate(3, Date.valueOf(r.getDateEntree()));
            ps.setInt(4, r.getNbrJour());
            ps.setString(5, r.getNomClient());
            ps.setString(6, r.getMail());
            ps.executeUpdate();
            // On récupère la clé générée (id_reserv) et on la met sur l'objet.
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setIdReserv(rs.getInt(1));
                }
            }
        }
    }

    /** Retourne toutes les réservations, des plus récentes (date d'entrée) aux plus anciennes. */
    public List<Reserver> lister() throws SQLException {
        List<Reserver> liste = new ArrayList<>();
        String sql = "SELECT * FROM reserver ORDER BY date_entree DESC";
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
     * Modifie une réservation.
     * Ne touche NI la date de réservation NI le flag {@code annulee}.
     */
    public void modifier(Reserver r) throws SQLException {
        String sql = "UPDATE reserver SET num_chambre = ?, date_entree = ?, "
                + "nbr_jour = ?, nom_client = ?, mail = ? WHERE id_reserv = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getNumChambre());
            ps.setDate(2, Date.valueOf(r.getDateEntree()));
            ps.setInt(3, r.getNbrJour());
            ps.setString(4, r.getNomClient());
            ps.setString(5, r.getMail());
            ps.setInt(6, r.getIdReserv());
            ps.executeUpdate();
        }
    }

    /** Supprime physiquement la réservation portant cet identifiant. */
    public void supprimer(int idReserv) throws SQLException {
        String sql = "DELETE FROM reserver WHERE id_reserv = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserv);
            ps.executeUpdate();
        }
    }

    /** Passe {@code annulee} à TRUE (libère la chambre pour la période concernée). */
    public void annuler(int idReserv) throws SQLException {
        String sql = "UPDATE reserver SET annulee = TRUE WHERE id_reserv = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserv);
            ps.executeUpdate();
        }
    }

    /**
     * Vérifie qu'aucune réservation active (non annulée) ne chevauche la période
     * [dateEntree ; dateEntree + nbrJour[ pour cette chambre.
     *
     * @return {@code true} si la chambre est libre sur la période demandée
     */
    public boolean estChambreDisponible(String numChambre, LocalDate dateEntree, int nbrJour)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM reserver "
                + "WHERE num_chambre = ? AND annulee = FALSE "
                + "AND date_entree < ? "
                + "AND (date_entree + (nbr_jour * INTERVAL '1 day')) > ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numChambre);
            ps.setDate(2, Date.valueOf(dateEntree.plusDays(nbrJour)));
            ps.setDate(3, Date.valueOf(dateEntree));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        }
    }

    /**
     * Idem que {@link #estChambreDisponible(String, LocalDate, int)} mais en excluant
     * la réservation {@code idReservExclu} (utile quand on modifie une réservation
     * sans créer de conflit avec elle-même).
     */
    public boolean estChambreDisponibleExcluant(String numChambre, LocalDate dateEntree,
                                                int nbrJour, int idReservExclu) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reserver "
                + "WHERE num_chambre = ? AND annulee = FALSE "
                + "AND date_entree < ? "
                + "AND (date_entree + (nbr_jour * INTERVAL '1 day')) > ? "
                + "AND id_reserv <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numChambre);
            ps.setDate(2, Date.valueOf(dateEntree.plusDays(nbrJour)));
            ps.setDate(3, Date.valueOf(dateEntree));
            ps.setInt(4, idReservExclu);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        }
    }

    /** Retourne la réservation portant cet identifiant, ou {@code null} si absente. */
    public Reserver trouverParId(int idReserv) throws SQLException {
        String sql = "SELECT * FROM reserver WHERE id_reserv = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReserv);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapper(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retourne les réservations actives (non annulées) qui ne sont pas encore
     * "occupées" (aucune ligne dans {@code occuper}), triées par date d'entrée.
     */
    public List<Reserver> listerActivesNonOccupees() throws SQLException {
        List<Reserver> liste = new ArrayList<>();
        String sql = "SELECT r.* FROM reserver r "
                + "LEFT JOIN occuper o ON r.id_reserv = o.id_reserv "
                + "WHERE r.annulee = FALSE AND o.id_occup IS NULL "
                + "ORDER BY r.date_entree";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(mapper(rs));
            }
        }
        return liste;
    }

    /** Convertit la ligne courante du ResultSet en objet Reserver. */
    private Reserver mapper(ResultSet rs) throws SQLException {
        Reserver r = new Reserver();
        r.setIdReserv(rs.getInt("id_reserv"));
        r.setNumChambre(rs.getString("num_chambre"));
        r.setDateReserv(rs.getDate("date_reserv").toLocalDate());
        r.setDateEntree(rs.getDate("date_entree").toLocalDate());
        r.setNbrJour(rs.getInt("nbr_jour"));
        r.setNomClient(rs.getString("nom_client"));
        r.setMail(rs.getString("mail"));
        r.setAnnulee(rs.getBoolean("annulee"));
        return r;
    }
}
