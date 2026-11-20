package com.bibliotech.dao;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.StatutEmprunt;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public class EmpruntDAO implements GenericDAO<Emprunt, Long> {

    // Calcule le statut à partir des dates
    private StatutEmprunt calculerStatut(LocalDate prevue, LocalDate effective) {
        if (effective != null) {
            return new StatutEmprunt.Rendu(effective, effective.isAfter(prevue));
        }
        LocalDate aujourdhui = LocalDate.now();
        if (prevue.isBefore(aujourdhui)) {
            long jours = ChronoUnit.DAYS.between(prevue, aujourdhui);
            return new StatutEmprunt.EnRetard(prevue, jours);
        }
        return new StatutEmprunt.EnCours(prevue);
    }

    // Texte stocké en base pour chaque statut
    private String statutEnTexte(StatutEmprunt statut) {
        return switch (statut) {
            case StatutEmprunt.EnCours s -> "EN_COURS";
            case StatutEmprunt.Rendu s -> "RENDU";
            case StatutEmprunt.EnRetard s -> "EN_RETARD";
        };
    }

    // Transforme la ligne courante du ResultSet en objet Emprunt
    private Emprunt construireEmprunt(ResultSet rs) throws SQLException {
        LocalDate prevue = rs.getDate("date_retour_prevue").toLocalDate();
        Date dateEffective = rs.getDate("date_retour_effective");
        LocalDate effective = (dateEffective == null) ? null : dateEffective.toLocalDate();

        return new Emprunt(
                rs.getLong("id"),
                rs.getLong("livre_id"),
                rs.getLong("etudiant_id"),
                rs.getDate("date_emprunt").toLocalDate(),
                prevue,
                effective,
                calculerStatut(prevue, effective));
    }

    @Override
    public List<Emprunt> findAll() {
        String sql = "SELECT * FROM emprunt ORDER BY id";
        List<Emprunt> emprunts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                emprunts.add(construireEmprunt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAll emprunts", e);
        }
        return emprunts;
    }

    @Override
    public Optional<Emprunt> findById(Long id) {
        String sql = "SELECT * FROM emprunt WHERE id = ?";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(construireEmprunt(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById emprunt", e);
        }
    }

    // Emprunts dont la date de retour prévue est passée et qui ne sont pas rendus
    public List<Emprunt> findEnRetard() {
        String sql = "SELECT * FROM emprunt "
                + "WHERE date_retour_prevue < CURRENT_DATE AND date_retour_effective IS NULL "
                + "ORDER BY date_retour_prevue";
        List<Emprunt> emprunts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                emprunts.add(construireEmprunt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findEnRetard", e);
        }
        return emprunts;
    }

    // Emprunts non rendus (en cours ou en retard)
    public List<Emprunt> findEnCours() {
        String sql = "SELECT * FROM emprunt WHERE date_retour_effective IS NULL "
                + "ORDER BY date_retour_prevue";
        List<Emprunt> emprunts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                emprunts.add(construireEmprunt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findEnCours", e);
        }
        return emprunts;
    }

    // Noms des étudiants, rangés par id (pour l'affichage)
    public Map<Long, String> findNomsEtudiants() {
        String sql = "SELECT id, prenom, nom FROM etudiant";
        Map<Long, String> noms = new HashMap<>();
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                noms.put(rs.getLong("id"), rs.getString("prenom") + " " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findNomsEtudiants", e);
        }
        return noms;
    }

        // Emprunter un livre : 3 étapes dans une seule transaction
    public void enregistrerEmprunt(long livreId, long etudiantId, int dureeJours) {
        try (Connection conn = DatabaseConnection.get()) {
            conn.setAutoCommit(false); // on démarre la transaction
            try {
                // 1. Vérifier que le livre existe et qu'il reste un exemplaire
                int disponibles;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT exemplaires_disponibles FROM livre WHERE id = ? FOR UPDATE")) {                    ps.setLong(1, livreId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new LivreIndisponibleException("Livre introuvable : " + livreId);
                        }
                        disponibles = rs.getInt("exemplaires_disponibles");
                    }
                }
                if (disponibles <= 0) {
                    throw new LivreIndisponibleException("Aucun exemplaire disponible");
                }

                // 2. Décrémenter les exemplaires disponibles
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE livre SET exemplaires_disponibles = exemplaires_disponibles - 1 WHERE id = ?")) {
                    ps.setLong(1, livreId);
                    ps.executeUpdate();
                }

                // 3. Insérer l'emprunt (statut EN_COURS)
                LocalDate aujourdhui = LocalDate.now();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO emprunt (livre_id, etudiant_id, date_emprunt, "
                        + "date_retour_prevue, statut) VALUES (?, ?, ?, ?, 'EN_COURS')")) {
                    ps.setLong(1, livreId);
                    ps.setLong(2, etudiantId);
                    ps.setDate(3, Date.valueOf(aujourdhui));
                    ps.setDate(4, Date.valueOf(aujourdhui.plusDays(dureeJours)));
                    ps.executeUpdate();
                }

                conn.commit(); // tout s'est bien passé : on valide
            } catch (SQLException | RuntimeException e) {
                conn.rollback(); // une erreur : on annule tout
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur enregistrerEmprunt", e);
        }
    }

        // Rendre un livre : date de retour + stock +1, dans UNE transaction
    public void retournerEmprunt(long empruntId) {
        try (Connection conn = DatabaseConnection.get()) {
            conn.setAutoCommit(false);
            try {
                long livreId;
                // 1. Marquer l'emprunt comme rendu (seulement s'il n'est pas déjà rendu)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE emprunt SET date_retour_effective = ?, statut = 'RENDU' "
                        + "WHERE id = ? AND date_retour_effective IS NULL RETURNING livre_id")) {
                    ps.setDate(1, Date.valueOf(LocalDate.now()));
                    ps.setLong(2, empruntId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Emprunt introuvable ou déjà rendu");
                        }
                        livreId = rs.getLong("livre_id");
                    }
                }

                // 2. Remettre un exemplaire en stock
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE livre SET exemplaires_disponibles = exemplaires_disponibles + 1 WHERE id = ?")) {
                    ps.setLong(1, livreId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur retournerEmprunt", e);
        }
    }

    @Override
    public void save(Emprunt emprunt) {
        String sql = "INSERT INTO emprunt (livre_id, etudiant_id, date_emprunt, "
                + "date_retour_prevue, date_retour_effective, statut) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            remplirParametres(ps, emprunt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save emprunt", e);
        }
    }

    @Override
    public void update(Long id, Emprunt emprunt) {
        String sql = "UPDATE emprunt SET livre_id = ?, etudiant_id = ?, date_emprunt = ?, "
                + "date_retour_prevue = ?, date_retour_effective = ?, statut = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            remplirParametres(ps, emprunt);
            ps.setLong(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update emprunt", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM emprunt WHERE id = ?";
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete emprunt", e);
        }
    }

    // Remplit les 6 premiers "?" (communs à save et update)
    private void remplirParametres(PreparedStatement ps, Emprunt emprunt) throws SQLException {
        ps.setLong(1, emprunt.livreId());
        ps.setLong(2, emprunt.etudiantId());
        ps.setDate(3, Date.valueOf(emprunt.dateEmprunt()));
        ps.setDate(4, Date.valueOf(emprunt.dateRetourPrevue()));
        if (emprunt.dateRetourEffective() == null) {
            ps.setNull(5, java.sql.Types.DATE);
        } else {
            ps.setDate(5, Date.valueOf(emprunt.dateRetourEffective()));
        }
        ps.setString(6, statutEnTexte(emprunt.statut()));
    }
}