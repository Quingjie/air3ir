package com.bibliotech.web;

import com.bibliotech.dao.DatabaseConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        // try-with-resources : tout est fermé automatiquement
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement("SELECT version()");
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            String version = rs.getString(1);
            resp.setStatus(200);
            resp.getWriter().print("OK · " + version);

        } catch (SQLException e) {
            // Le détail de l'erreur va dans les logs, pas dans la réponse
            getServletContext().log("Erreur de connexion à la base", e);
            resp.setStatus(500);
            resp.getWriter().print("KO");
        }
    }
}