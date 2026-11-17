# BiblioTech

## Réponses aux questions de réflexion

### Question 0.1 : 
**Pourquoi `jakarta.jakartaee-web-api` est-elle en `provided` plutôt qu'en `compile` ?**
Parce que Tomcat fournit déjà l'API Jakarta EE à l'exécution. On en a besoin pour compiler le code, mais pas pour l'embarquer dans le WAR.

**Que se passerait-il en `compile` ?**
Les JAR de l'API seraient copiés dans `WEB-INF/lib`. Le WAR serait plus lourd, et les classes seraient en double (celles de Tomcat et celles du WAR), ce qui peut provoquer des conflits de versions ou des erreurs de chargement de classes.

### Question 0.2 : 
**Pourquoi ne pas mettre les credentials en dur dans le code poussé sur Git ?**
Le code est visible par tous ceux qui ont accès au dépôt, et l'historique Git garde les anciennes versions : un mot de passe supprimé reste récupérable. Chaque environnement (dev, production) a aussi ses propres identifiants.

**Deux moyens de les externaliser (hors variables d'environnement :**
1. Un fichier de configuration (`db.properties`) placé hors du dépôt ou listé dans `.gitignore`.
2. Une DataSource JNDI configurée dans Tomcat (`context.xml`), où les identifiants sont gérés par le serveur et non par l'application.