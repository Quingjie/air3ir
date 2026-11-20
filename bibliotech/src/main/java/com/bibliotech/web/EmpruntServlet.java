package com.bibliotech.web;

import com.bibliotech.dao.EmpruntDAO;
import com.bibliotech.dao.LivreIndisponibleException;
import com.bibliotech.model.Emprunt;
import com.bibliotech.dao.LivreDAO;
import com.bibliotech.model.Livre;
import com.bibliotech.model.PenaliteCalculator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/emprunts/*")
public class EmpruntServlet extends HttpServlet {

    private final EmpruntDAO empruntDAO = new EmpruntDAO();
    private final LivreDAO livreDAO = new LivreDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();

        if (path == null || path.equals("/")) {
            // GET /emprunts : liste des emprunts en cours
            List<Emprunt> emprunts = empruntDAO.findEnCours();

            // Pénalité de chaque emprunt, rangée par id d'emprunt
            Map<Long, Double> penalites = new HashMap<>();
            for (Emprunt e : emprunts) {
                penalites.put(e.id(), PenaliteCalculator.calculerPenalite(e.statut()));
            }

            // Message laissé par un POST précédent (affiché une seule fois)
            HttpSession session = request.getSession();
            request.setAttribute("message", session.getAttribute("message"));
            session.removeAttribute("message");

            // Titres des livres, rangés par id (pour l'affichage)
            Map<Long, String> titres = new HashMap<>();
            for (Livre l : livreDAO.findAll()) {
                titres.put(l.id(), l.titre());
            }
            request.setAttribute("titres", titres);
            request.setAttribute("etudiants", empruntDAO.findNomsEtudiants());

            request.setAttribute("emprunts", emprunts);
            request.setAttribute("penalites", penalites);
            request.getRequestDispatcher("/WEB-INF/emprunts/list.jsp")
                   .forward(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();
        HttpSession session = request.getSession();

        if (path == null || path.equals("/")) {
            // POST /emprunts : emprunter un livre
            try {
                long livreId = Long.parseLong(request.getParameter("livreId"));
                long etudiantId = Long.parseLong(request.getParameter("etudiantId"));
                int duree = Integer.parseInt(request.getParameter("dureeJours"));
                empruntDAO.enregistrerEmprunt(livreId, etudiantId, duree);
                session.setAttribute("message", "Emprunt enregistré.");
            } catch (NumberFormatException e) {
                session.setAttribute("message", "Erreur : numéro d'étudiant ou durée invalide.");
            } catch (LivreIndisponibleException e) {
                session.setAttribute("message", "Erreur : " + e.getMessage());
            } catch (RuntimeException e) {
                // Par exemple : étudiant inexistant (clé étrangère)
                session.setAttribute("message", "Erreur : emprunt impossible (étudiant inconnu ?).");
            }
            response.sendRedirect(request.getContextPath() + "/emprunts"); // PRG

        } else if (path.endsWith("/retour")) {
            // POST /emprunts/{id}/retour : rendre un livre
            try {
                long id = Long.parseLong(path.substring(1, path.length() - "/retour".length()));
                empruntDAO.retournerEmprunt(id);
                session.setAttribute("message", "Retour enregistré.");
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } catch (IllegalArgumentException e) {
                session.setAttribute("message", "Erreur : " + e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/emprunts"); // PRG

        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}