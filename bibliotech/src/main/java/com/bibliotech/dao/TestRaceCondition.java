package com.bibliotech.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestRaceCondition {

    public static void main(String[] args) throws Exception {
        EmpruntDAO dao = new EmpruntDAO();
        int essais = 20;
        int doubles = 0; // nombre d'essais où les 2 emprunts ont réussi

        for (int i = 1; i <= essais; i++) {
            remettreEtat(); // livre 5 : 1 exemplaire, aucun emprunt

            CountDownLatch depart = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);

            // Alice (étudiant 1) et Bob (étudiant 2) attendent le même signal
            Future<Boolean> alice = pool.submit(() -> emprunter(dao, 1, depart));
            Future<Boolean> bob = pool.submit(() -> emprunter(dao, 2, depart));

            depart.countDown(); // top départ : les deux threads partent en même temps

            boolean aliceOk = alice.get();
            boolean bobOk = bob.get();
            pool.shutdown();

            int stock = lireStock();
            System.out.println("Essai " + i + " : Alice=" + aliceOk + ", Bob=" + bobOk
                    + ", stock final=" + stock);
            if (aliceOk && bobOk) {
                doubles++;
            }
        }

        System.out.println();
        System.out.println("Essais où les DEUX emprunts ont réussi : " + doubles + " sur " + essais);
    }

    // Attend le signal de départ, puis tente d'emprunter le livre 5
    private static boolean emprunter(EmpruntDAO dao, long etudiantId, CountDownLatch depart)
            throws InterruptedException {
        depart.await();
        try {
            dao.enregistrerEmprunt(5, etudiantId, 14);
            return true;
        } catch (RuntimeException e) {
            return false; // livre indisponible ou autre erreur
        }
    }

    // Remet le livre 5 à 1 exemplaire et supprime ses emprunts
    private static void remettreEtat() throws Exception {
        try (Connection conn = DatabaseConnection.get()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM emprunt WHERE livre_id = 5")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE livre SET exemplaires_disponibles = 1 WHERE id = 5")) {
                ps.executeUpdate();
            }
        }
    }

    // Lit le stock actuel du livre 5
    private static int lireStock() throws Exception {
        try (Connection conn = DatabaseConnection.get();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT exemplaires_disponibles FROM livre WHERE id = 5");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}