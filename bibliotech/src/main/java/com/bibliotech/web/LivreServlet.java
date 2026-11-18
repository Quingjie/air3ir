package com.bibliotech.web;

import com.bibliotech.dao.LivreDAO;
import com.bibliotech.model.Livre;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
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
            String titre = request.getParameter("titre");
            String auteur = request.getParameter("auteur");
            int annee = Integer.parseInt(request.getParameter("anneePublication"));
            int total = Integer.parseInt(request.getParameter("exemplairesTotal"));

            livreDAO.save(new Livre(0, titre, auteur, annee, total, total));
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

            String titre = request.getParameter("titre");
            String auteur = request.getParameter("auteur");
            int annee = Integer.parseInt(request.getParameter("anneePublication"));
            int total = Integer.parseInt(request.getParameter("exemplairesTotal"));

            // On garde les disponibles, sans dépasser le nouveau total
            int disponibles = Math.min(ancien.get().exemplairesDisponibles(), total);

            livreDAO.update(id, new Livre(id, titre, auteur, annee, total, disponibles));
            response.sendRedirect(request.getContextPath() + "/livres"); // PRG
        }
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