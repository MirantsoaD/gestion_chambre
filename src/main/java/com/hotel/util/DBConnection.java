package com.hotel.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gère la connexion JDBC à PostgreSQL.
 * La configuration est lue dans {@code db.properties} (classpath).
 */
public final class DBConnection {

    private static final Properties PROPS = chargerProperties();

    private DBConnection() {
    }

    /**
     * Ouvre une nouvelle connexion à la base de données.
     *
     * @throws SQLException si la connexion échoue (serveur injoignable,
     *                      mauvais identifiants, base inexistante...)
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password"));
    }

    private static Properties chargerProperties() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Fichier db.properties introuvable dans src/main/resources.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de lire db.properties : " + e.getMessage(), e);
        }
        return props;
    }
}
