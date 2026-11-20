<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech - Emprunts en cours</title>
</head>
<body>
    <h1>Emprunts en cours</h1>

    <p><a href="${pageContext.request.contextPath}/livres">Retour au catalogue</a></p>

    <c:if test="${not empty message}">
        <p><strong><c:out value="${message}" /></strong></p>
    </c:if>

    <table border="1" cellpadding="5">
        <thead>
            <tr>
                <th>N° emprunt</th>
                <th>Livre</th>
                <th>Étudiant</th>
                <th>Emprunté le</th>
                <th>Retour prévu le</th>
                <th>Statut</th>
                <th>Pénalité</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="emprunt" items="${emprunts}">
                <tr>
                    <td><c:out value="${emprunt.id()}" /></td>
                    <td><c:out value="${titres[emprunt.livreId()]}" /></td>
                    <td><c:out value="${etudiants[emprunt.etudiantId()]}" /></td>
                    <td><c:out value="${emprunt.dateEmprunt()}" /></td>
                    <td><c:out value="${emprunt.dateRetourPrevue()}" /></td>
                    <td>
                        <c:choose>
                            <c:when test="${penalites[emprunt.id()] > 0}">En retard</c:when>
                            <c:otherwise>En cours</c:otherwise>
                        </c:choose>
                    </td>
                    <td><c:out value="${penalites[emprunt.id()]}" /> €</td>
                    <td>
                        <form method="post"
                              action="${pageContext.request.contextPath}/emprunts/${emprunt.id()}/retour"
                              style="display:inline">
                            <button type="submit">Retour</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</body>
</html>