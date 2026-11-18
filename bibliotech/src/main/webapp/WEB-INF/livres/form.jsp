<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>BiblioTech - Livre</title>
    <style>.erreur { color: red; margin: 0; }</style>
</head>
<body>
    <%-- Modification si un livre est chargé (premier affichage) ou si idModification existe (après erreur) --%>
    <c:choose>
        <c:when test="${not empty livre}">
            <h1>Modifier un livre</h1>
            <c:set var="action" value="${pageContext.request.contextPath}/livres/${livre.id()}" />
        </c:when>
        <c:when test="${not empty idModification}">
            <h1>Modifier un livre</h1>
            <c:set var="action" value="${pageContext.request.contextPath}/livres/${idModification}" />
        </c:when>
        <c:otherwise>
            <h1>Ajouter un livre</h1>
            <c:set var="action" value="${pageContext.request.contextPath}/livres" />
        </c:otherwise>
    </c:choose>

    <%-- Valeurs à afficher : la saisie après erreur, sinon celles du livre --%>
    <c:choose>
        <c:when test="${not empty errors}">
            <c:set var="vTitre" value="${saisie_titre}" />
            <c:set var="vAuteur" value="${saisie_auteur}" />
            <c:set var="vAnnee" value="${saisie_annee}" />
            <c:set var="vTotal" value="${saisie_total}" />
        </c:when>
        <c:when test="${not empty livre}">
            <c:set var="vTitre" value="${livre.titre()}" />
            <c:set var="vAuteur" value="${livre.auteur()}" />
            <c:set var="vAnnee" value="${livre.anneePublication()}" />
            <c:set var="vTotal" value="${livre.exemplairesTotal()}" />
        </c:when>
    </c:choose>

    <form method="post" action="${action}">
        <p>
            <label>Titre :
                <input type="text" name="titre" value="<c:out value='${vTitre}' />">
            </label>
        </p>
        <c:if test="${not empty errors.titre}">
            <p class="erreur"><c:out value="${errors.titre}" /></p>
        </c:if>

        <p>
            <label>Auteur :
                <input type="text" name="auteur" value="<c:out value='${vAuteur}' />">
            </label>
        </p>
        <c:if test="${not empty errors.auteur}">
            <p class="erreur"><c:out value="${errors.auteur}" /></p>
        </c:if>

        <p>
            <label>Année de publication :
                <input type="text" name="anneePublication" value="<c:out value='${vAnnee}' />">
            </label>
        </p>
        <c:if test="${not empty errors.anneePublication}">
            <p class="erreur"><c:out value="${errors.anneePublication}" /></p>
        </c:if>

        <p>
            <label>Nombre d'exemplaires :
                <input type="text" name="exemplairesTotal" value="<c:out value='${vTotal}' />">
            </label>
        </p>
        <c:if test="${not empty errors.exemplairesTotal}">
            <p class="erreur"><c:out value="${errors.exemplairesTotal}" /></p>
        </c:if>

        <button type="submit">Enregistrer</button>
        <a href="${pageContext.request.contextPath}/livres">Annuler</a>
    </form>
</body>
</html>