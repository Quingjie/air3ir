package com.bibliotech.model;

public class TestModel {
    public static void main(String[] args) {
        Livre ok = new Livre(1, "Effective Java", "Joshua Bloch", 2018, 3, 3);
        System.out.println(ok);

        Etudiant e = new Etudiant(1, "ET123456", "Dupont", "Alice", "alice@fac.fr");
        System.out.println(e);

        essayer(() -> new Livre(1, "", "Bloch", 2018, 3, 3));
        essayer(() -> new Livre(1, "Titre", "Bloch", 1400, 3, 3));
        essayer(() -> new Livre(1, "Titre", "Bloch", 2018, 3, 5));
        essayer(() -> new Etudiant(1, "et12", "Dupont", "Alice", "alice@fac.fr"));
        essayer(() -> new Etudiant(1, "ET123456", "Dupont", "Alice", "alicefac.fr"));
    }

    // Affiche le message de l'erreur attendue
    static void essayer(Runnable r) {
        try {
            r.run();
            System.out.println("PAS D'ERREUR (anormal)");
        } catch (IllegalArgumentException ex) {
            System.out.println("Erreur attendue : " + ex.getMessage());
        }
    }
}