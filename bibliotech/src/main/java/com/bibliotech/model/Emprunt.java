package com.bibliotech.model;

import java.time.LocalDate;

public record Emprunt(long id, long livreId, long etudiantId,
                      LocalDate dateEmprunt, LocalDate dateRetourPrevue,
                      LocalDate dateRetourEffective, StatutEmprunt statut) {

    public Emprunt {
        if (dateEmprunt == null) {
            throw new IllegalArgumentException("La date d'emprunt est obligatoire");
        }
        if (dateRetourPrevue == null) {
            throw new IllegalArgumentException("La date de retour prévue est obligatoire");
        }
        if (statut == null) {
            throw new IllegalArgumentException("Le statut est obligatoire");
        }
        if (dateRetourPrevue.isBefore(dateEmprunt)) {
            throw new IllegalArgumentException(
                    "La date de retour prévue ne peut pas précéder la date d'emprunt");
        }
    }
}