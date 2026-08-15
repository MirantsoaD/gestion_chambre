package com.hotel.dao;

import com.hotel.model.Chambre;
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
 * Accès base de données pour la table {@code chambre}.
 * Méthodes non statiques : l'UI fera {@code new ChambreDAO()}.
 */
public class ChambreDAO {

    /** Insère une nouvelle chambre. */
    public void ajouter(Chambre c) throws SQLException {
        String sql = "INSERT INTO chambre (num_chambre, design, type, prix_nuitee) "
                + "VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNumChambre());
            ps.setString(2, c.getDesign());
            ps.setString(3, c.getType());
            ps.setInt(4, c.getPrixNuitee());
            ps.executeUpdate();
        }
    }

    /** Retourne la liste de toutes les chambres, triées par numéro. */
    public List<Chambre> lister() throws SQLException {
        List<Chambre> liste = new ArrayList<>();
        String sql = "SELECT * FROM chambre ORDER BY num_chambre";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(mapper(rs));
            }
        }
        return liste;
    }

    /** Modifie le design, le type et le prix d'une chambre existante. */
    public void modifier(Chambre c) throws SQLException {
        String sql = "UPDATE chambre SET design = ?, type = ?, prix_nuitee = ? "
                + "WHERE num_chambre = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getDesign());
            ps.setString(2, c.getType());
            ps.setInt(3, c.getPrixNuitee());
            ps.setString(4, c.getNumChambre());
            ps.executeUpdate();
        }
    }

    /** Supprime la chambre portant ce numéro. */
    public void supprimer(String numChambre) throws SQLException {
        String sql = "DELETE FROM chambre WHERE num_chambre = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numChambre);
            ps.executeUpdate();
        }
    }

    /** Retourne la chambre correspondant au numéro, ou {@code null} si absente. */
    public Chambre trouverParNum(String numChambre) throws SQLException {
        String sql = "SELECT * FROM chambre WHERE num_chambre = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numChambre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapper(rs);
                }
            }
        }
        return null;
    }

    /**
     * Liste les chambres libres sur la période [dateEntree ; dateEntree + nbrJour[ :
     * aucune réservation active (non annulée) ni séjour ne chevauche la période.
     */
    public List<Chambre> listerChambresLibres(LocalDate dateEntree, int nbrJour) throws SQLException {
        List<Chambre> liste = new ArrayList<>();
        String sql = "SELECT c.* FROM chambre c "
                + "WHERE NOT EXISTS ( "
                + "SELECT 1 FROM reserver r "
                + "WHERE r.num_chambre = c.num_chambre "
                + "AND r.annulee = FALSE "
                + "AND r.date_entree < ? "
                + "AND (r.date_entree + (r.nbr_jour * INTERVAL '1 day')) > ? "
                + ") "
                + "AND NOT EXISTS ( "
                + "SELECT 1 FROM sejourner s "
                + "WHERE s.num_chambre = c.num_chambre "
                + "AND s.date_entree_sejour < ? "
                + "AND (s.date_entree_sejour + (s.nbr_jour * INTERVAL '1 day')) > ? "
                + ") "
                + "ORDER BY c.num_chambre";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Fin puis début de la période, deux fois (une par NOT EXISTS).
            ps.setDate(1, Date.valueOf(dateEntree.plusDays(nbrJour)));
            ps.setDate(2, Date.valueOf(dateEntree));
            ps.setDate(3, Date.valueOf(dateEntree.plusDays(nbrJour)));
            ps.setDate(4, Date.valueOf(dateEntree));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapper(rs));
                }
            }
        }
        return liste;
    }

    /** Convertit la ligne courante du ResultSet en objet Chambre. */
    private Chambre mapper(ResultSet rs) throws SQLException {
        Chambre c = new Chambre();
        c.setNumChambre(rs.getString("num_chambre"));
        c.setDesign(rs.getString("design"));
        c.setType(rs.getString("type"));
        c.setPrixNuitee(rs.getInt("prix_nuitee"));
        return c;
    }
}
