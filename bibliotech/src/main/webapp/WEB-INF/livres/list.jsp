<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech - Catalogue</title>
</head>
<body>
    <h1>Catalogue des livres</h1>

    <p><a href="${pageContext.request.contextPath}/livres/nouveau">Ajouter un livre</a></p>

    <table border="1" cellpadding="5">
        <thead>
            <tr>
                <th>Titre</th>
                <th>Auteur</th>
                <th>Année</th>
                <th>Disponibles</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="livre" items="${livres}">
                <tr>
                    <td><c:out value="${livre.titre()}" /></td>
                    <td><c:out value="${livre.auteur()}" /></td>
                    <td><c:out value="${livre.anneePublication()}" /></td>
                    <td>
                        <c:out value="${livre.exemplairesDisponibles()}" />
                        / <c:out value="${livre.exemplairesTotal()}" />
                    </td>
                    <td>
                        <a href="${pageContext.request.contextPath}/livres/${livre.id()}">Détail</a>
                        <a href="${pageContext.request.contextPath}/livres/${livre.id()}/edit">Modifier</a>
                        <form method="post"
                              action="${pageContext.request.contextPath}/livres/${livre.id()}/delete"
                              style="display:inline">
                            <button type="submit">Supprimer</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</body>
</html>