package com.hotel.dao;

import com.hotel.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Accès base de données pour la table {@code solde} (ligne unique, id = 1).
 * Méthodes non statiques : l'UI fera {@code new SoldeDAO()}.
 */
public class SoldeDAO {

    /** Retourne le solde actuel de l'hôtel. */
    public int getSolde() throws SQLException {
        String sql = "SELECT solde_actuel FROM solde WHERE id = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Ajoute {@code montant} au solde (ouvre sa propre connexion).
     *
     * @param montant montant à ajouter (peut être négatif)
     */
    public void ajouterAuSolde(int montant) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ajouterAuSolde(conn, montant);
        }
    }

    /**
     * Ajoute {@code montant} au solde sur la connexion fournie.
     * Surcharge utilisée dans les transactions des autres DAO
     * (OccuperDAO, SejournerDAO) pour que la mise à jour du solde
     * soit atomique avec le reste de l'opération.
     */
    public void ajouterAuSolde(Connection conn, int montant) throws SQLException {
        String sql = "UPDATE solde SET solde_actuel = solde_actuel + ? WHERE id = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, montant);
            ps.executeUpdate();
        }
    }
}
