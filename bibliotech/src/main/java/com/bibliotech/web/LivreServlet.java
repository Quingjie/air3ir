package com.bibliotech.web;

import com.bibliotech.dao.LivreDAO;
import com.bibliotech.model.Livre;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet("/livres/*")
public class LivreServlet extends HttpServlet {

    private final LivreDAO livreDAO = new LivreDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();

        if (path == null || path.equals("/")) {
            // GET /livres : la liste
            List<Livre> livres = livreDAO.findAll();
            request.setAttribute("livres", livres);
            request.getRequestDispatcher("/WEB-INF/livres/list.jsp")
                   .forward(request, response);
        } else if (path.equals("/nouveau")) {
            // GET /livres/nouveau : formulaire vide
            request.getRequestDispatcher("/WEB-INF/livres/form.jsp")
                   .forward(request, response);
        } else if (path.endsWith("/edit")) {
            // GET /livres/{id}/edit : formulaire pré-rempli
            Long id = lireId(path.substring(1, path.length() - "/edit".length()));
            Optional<Livre> livre = (id == null) ? Optional.empty() : livreDAO.findById(id);
            if (livre.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("livre", livre.get());
            request.getRequestDispatcher("/WEB-INF/livres/form.jsp")
                   .forward(request, response);
        } else {
            // GET /livres/{id} : détail d'un livre
            Long id = lireId(path.substring(1));
            Optional<Livre> livre = (id == null) ? Optional.empty() : livreDAO.findById(id);
            if (livre.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("livre", livre.get());
            request.getRequestDispatcher("/WEB-INF/livres/detail.jsp")
                   .forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();

        if (path == null || path.equals("/")) {
            // POST /livres : créer un livre
            Map<String, String> errors = new HashMap<>();
            Livre livre = lireFormulaire(request, 0, errors);

            if (!errors.isEmpty()) {
                reafficherFormulaire(request, response, errors, null);
                return;
            }
            livreDAO.save(livre);
            response.sendRedirect(request.getContextPath() + "/livres"); // PRG

        } else if (path.endsWith("/delete")) {
            // POST /livres/{id}/delete : supprimer un livre
            Long id = lireId(path.substring(1, path.length() - "/delete".length()));
            if (id == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            livreDAO.delete(id);
            response.sendRedirect(request.getContextPath() + "/livres"); // PRG

        } else {
            // POST /livres/{id} : modifier un livre
            Long id = lireId(path.substring(1));
            Optional<Livre> ancien = (id == null) ? Optional.empty() : livreDAO.findById(id);
            if (ancien.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Map<String, String> errors = new HashMap<>();
            Livre livre = lireFormulaire(request, id, errors);

            if (!errors.isEmpty()) {
                reafficherFormulaire(request, response, errors, id);
                return;
            }

            // On garde les disponibles, sans dépasser le nouveau total
            int disponibles = Math.min(ancien.get().exemplairesDisponibles(), livre.exemplairesTotal());
            livreDAO.update(id, new Livre(id, livre.titre(), livre.auteur(),
                    livre.anneePublication(), livre.exemplairesTotal(), disponibles));
            response.sendRedirect(request.getContextPath() + "/livres"); // PRG
        }
    }

        // Lit le formulaire, remplit "errors" si un champ est invalide.
    // Renvoie le Livre si tout est bon, sinon null.
    private Livre lireFormulaire(HttpServletRequest request, long id, Map<String, String> errors) {
        String titre = request.getParameter("titre");
        String auteur = request.getParameter("auteur");

        if (titre == null || titre.isBlank()) {
            errors.put("titre", "Le titre est obligatoire.");
        }
        if (auteur == null || auteur.isBlank()) {
            errors.put("auteur", "L'auteur est obligatoire.");
        }

        int annee = 0;
        try {
            annee = Integer.parseInt(request.getParameter("anneePublication"));
            // Plage de l'année : 1500 jusqu'à l'année courante
            int anneeCourante = LocalDate.now().getYear();
            if (annee < 1500 || annee > anneeCourante) {
                errors.put("anneePublication",
                        "L'année doit être entre 1500 et " + anneeCourante);
            }
        } catch (NumberFormatException e) {
            errors.put("anneePublication", "L'année doit être un nombre.");
        }

        int total = 0;
        try {
            total = Integer.parseInt(request.getParameter("exemplairesTotal"));
            if (total < 1) {
                errors.put("exemplairesTotal", "Il faut au moins 1 exemplaire.");
            }
        } catch (NumberFormatException e) {
            errors.put("exemplairesTotal", "Le nombre d'exemplaires doit être un nombre.");
        }

        if (!errors.isEmpty()) {
            return null;
        }

        // Dernier garde-fou : les règles du record
        try {
            return new Livre(id, titre, auteur, annee, total, total);
        } catch (IllegalArgumentException e) {
            errors.put("titre", e.getMessage());
            return null;
        }
    }

    // Réaffiche le formulaire avec les valeurs saisies et les erreurs
    private void reafficherFormulaire(HttpServletRequest request, HttpServletResponse response,
                                      Map<String, String> errors, Long id)
            throws ServletException, IOException {
        request.setAttribute("errors", errors);
        request.setAttribute("saisie_titre", request.getParameter("titre"));
        request.setAttribute("saisie_auteur", request.getParameter("auteur"));
        request.setAttribute("saisie_annee", request.getParameter("anneePublication"));
        request.setAttribute("saisie_total", request.getParameter("exemplairesTotal"));
        request.setAttribute("idModification", id);
        request.getRequestDispatcher("/WEB-INF/livres/form.jsp")
               .forward(request, response);
    }

    // Transforme un texte en nombre, ou renvoie null si ce n'est pas un nombre
    private Long lireId(String texte) {
        try {
            return Long.parseLong(texte);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}