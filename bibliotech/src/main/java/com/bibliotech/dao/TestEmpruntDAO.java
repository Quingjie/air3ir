package com.bibliotech.dao;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.StatutEmprunt;

import java.time.LocalDate;

public class TestEmpruntDAO {
    public static void main(String[] args) {
        EmpruntDAO dao = new EmpruntDAO();
        LocalDate today = LocalDate.now();

        // Un emprunt en cours (retour dans 10 jours)
        dao.save(new Emprunt(0, 1, 1, today, today.plusDays(10), null,
                new StatutEmprunt.EnCours(today.plusDays(10))));

        // Un emprunt en retard (prévu il y a 5 jours, pas rendu)
        LocalDate prevue = today.minusDays(5);
        dao.save(new Emprunt(0, 2, 2, today.minusDays(20), prevue, null,
                new StatutEmprunt.EnCours(prevue)));

        System.out.println("Tous les emprunts :");
        dao.findAll().forEach(System.out::println);

        System.out.println("En retard :");
        dao.findEnRetard().forEach(System.out::println);
    }
}