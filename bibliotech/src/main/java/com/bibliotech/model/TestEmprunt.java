package com.bibliotech.model;

import java.time.LocalDate;

public class TestEmprunt {
    public static void main(String[] args) {
        LocalDate debut = LocalDate.of(2026, 11, 1);
        LocalDate limite = LocalDate.of(2026, 11, 15);

        Emprunt e1 = new Emprunt(1, 1, 1, debut, limite, null,
                new StatutEmprunt.EnCours(limite));
        System.out.println(e1);

        Emprunt e2 = new Emprunt(2, 1, 1, debut, limite, LocalDate.of(2026, 11, 20),
                new StatutEmprunt.Rendu(LocalDate.of(2026, 11, 20), true));
        System.out.println(e2);

        Emprunt e3 = new Emprunt(3, 1, 1, debut, limite, null,
                new StatutEmprunt.EnRetard(limite, 2));
        System.out.println(e3);

        try {
            new Emprunt(4, 1, 1, debut, LocalDate.of(2026, 10, 1), null,
                    new StatutEmprunt.EnCours(limite));
            System.out.println("PAS D'ERREUR (anormal)");
        } catch (IllegalArgumentException ex) {
            System.out.println("Erreur attendue : " + ex.getMessage());
        }
    }
}