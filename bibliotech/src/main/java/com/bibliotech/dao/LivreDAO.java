package com.bibliotech.dao;

import com.bibliotech.model.Livre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LivreDAO implements GenericDAO<Livre, Long> {

    // Transforme la ligne courante du ResultSet en objet Livre
    private Livre construireLivre(ResultSet rs) throws SQLException {
        return new Livre(
                rs.getLong("id"),
                rs.getString("titre"),
                rs.getString("auteur"),
                rs.getInt("annee_publication"),
                rs.getInt("exemplaires_total"),
                rs.getInt("exemplaires_disponibles"));
    }

    @Override
    public List<Livre> findAll() {
        String sql = "SELECT * FROM livre ORDER BY id";
        List<Livre> livres = new ArrayList<>();
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                livres.add(construireLivre(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll livres", e);
        }
        return livres;
    }

    @Override
    public Optional<Livre> findById(Long id) {
        String sql = "SELECT * FROM livre WHERE id = ?";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(construireLivre(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById livre", e);
        }
    }

    @Override
    public void save(Livre livre) {
        String sql = "INSERT INTO livre (titre, auteur, annee_publication, "
                + "exemplaires_total, exemplaires_disponibles) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, livre.titre());
            ps.setString(2, livre.auteur());
            ps.setInt(3, livre.anneePublication());
            ps.setInt(4, livre.exemplairesTotal());
            ps.setInt(5, livre.exemplairesDisponibles());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save livre", e);
        }
    }

    @Override
    public void update(Long id, Livre livre) {
        String sql = "UPDATE livre SET titre = ?, auteur = ?, annee_publication = ?, "
                + "exemplaires_total = ?, exemplaires_disponibles = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, livre.titre());
            ps.setString(2, livre.auteur());
            ps.setInt(3, livre.anneePublication());
            ps.setInt(4, livre.exemplairesTotal());
            ps.setInt(5, livre.exemplairesDisponibles());
            ps.setLong(6, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update livre", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM livre WHERE id = ?";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete livre", e);
        }
    }
}