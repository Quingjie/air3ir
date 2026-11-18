package com.bibliotech.dao;

import com.bibliotech.model.Livre;

public class TestLivreDAO {
    public static void main(String[] args) {
        LivreDAO dao = new LivreDAO();

        System.out.println("Nombre de livres : " + dao.findAll().size());
        System.out.println("Livre 1 : " + dao.findById(1L));
        System.out.println("Livre 999 : " + dao.findById(999L));

        // id = 0 : il sera ignoré, la base génère le vrai id
        dao.save(new Livre(0, "Test", "Auteur Test", 2020, 2, 2));
        System.out.println("Après save : " + dao.findAll().size());

        dao.update(6L, new Livre(6, "Test modifié", "Auteur Test", 2020, 2, 2));
        System.out.println("Après update : " + dao.findById(6L));

        dao.delete(6L);
        System.out.println("Après delete : " + dao.findAll().size());
    }
}