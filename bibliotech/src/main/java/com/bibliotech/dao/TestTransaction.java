package com.bibliotech.dao;

public class TestTransaction {
    public static void main(String[] args) {
        EmpruntDAO dao = new EmpruntDAO();

        // Livre 5 (Java Concurrency) : 1 seul exemplaire
        dao.enregistrerEmprunt(5, 1, 14);
        System.out.println("Emprunt 1 : OK");

        try {
            dao.enregistrerEmprunt(5, 2, 14);
            System.out.println("Emprunt 2 : OK (anormal !)");
        } catch (LivreIndisponibleException e) {
            System.out.println("Emprunt 2 refusé : " + e.getMessage());
        }

        try {
            dao.enregistrerEmprunt(999, 1, 14);
        } catch (LivreIndisponibleException e) {
            System.out.println("Livre 999 : " + e.getMessage());
        }

        // Test du rollback : étudiant 999 n'existe pas -> l'INSERT échoue
        try {
            dao.enregistrerEmprunt(1, 999, 14);
        } catch (RuntimeException e) {
            System.out.println("Étudiant 999 : erreur, rollback attendu");
        }
    }
}