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

@WebServlet("/livres/*")
public class LivreServlet extends HttpServlet {

    private final LivreDAO livreDAO = new LivreDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Pour l'instant : seulement la liste
        List<Livre> livres = livreDAO.findAll();
        request.setAttribute("livres", livres);
        request.getRequestDispatcher("/WEB-INF/livres/list.jsp")
               .forward(request, response);
    }
}