package com.bibliotech.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Classe utilitaire : on ne veut pas qu'on puisse l'instancier
    private DatabaseConnection() {
    }

    // Retourne une nouvelle connexion à chaque appel
    public static Connection get() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new SQLException("Variables d'environnement DB_URL, DB_USER ou DB_PASSWORD manquantes");
        }

        // On charge le driver à la main : Tomcat ne le fait pas toujours tout seul
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL introuvable", e);
        }

        return DriverManager.getConnection(url, user, password);
    }
}