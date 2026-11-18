<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech - Détail</title>
</head>
<body>
    <h1><c:out value="${livre.titre()}" /></h1>

    <ul>
        <li>Auteur : <c:out value="${livre.auteur()}" /></li>
        <li>Année de publication : <c:out value="${livre.anneePublication()}" /></li>
        <li>Exemplaires disponibles :
            <c:out value="${livre.exemplairesDisponibles()}" />
            / <c:out value="${livre.exemplairesTotal()}" />
        </li>
    </ul>

    <p>
        <a href="${pageContext.request.contextPath}/livres/${livre.id()}/edit">Modifier</a>
        <a href="${pageContext.request.contextPath}/livres">Retour à la liste</a>
    </p>
</body>
</html>