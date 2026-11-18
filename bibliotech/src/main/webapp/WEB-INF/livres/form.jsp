<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech - Livre</title>
</head>
<body>
    <c:choose>
        <c:when test="${not empty livre}">
            <h1>Modifier un livre</h1>
            <c:set var="action" value="${pageContext.request.contextPath}/livres/${livre.id()}" />
        </c:when>
        <c:otherwise>
            <h1>Ajouter un livre</h1>
            <c:set var="action" value="${pageContext.request.contextPath}/livres" />
        </c:otherwise>
    </c:choose>

    <form method="post" action="${action}">
        <p>
            <label>Titre :
                <input type="text" name="titre" value="<c:out value='${livre.titre()}' />">
            </label>
        </p>
        <p>
            <label>Auteur :
                <input type="text" name="auteur" value="<c:out value='${livre.auteur()}' />">
            </label>
        </p>
        <p>
            <label>Année de publication :
                <input type="number" name="anneePublication" value="<c:out value='${livre.anneePublication()}' />">
            </label>
        </p>
        <p>
            <label>Nombre d'exemplaires :
                <input type="number" name="exemplairesTotal" min="1" value="<c:out value='${livre.exemplairesTotal()}' />">
            </label>
        </p>
        <button type="submit">Enregistrer</button>
        <a href="${pageContext.request.contextPath}/livres">Annuler</a>
    </form>
</body>
</html>