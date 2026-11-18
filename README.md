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

____________________________________________

### Question A.2 : 
**Quelle différence entre une sealed interface et une interface classique ? Quel bénéfice pour le compilateur (et pour vous, le dev) lorsque vous écrivez un switch sur cette interface ?**

**Différence avec une interface classique :**

Une interface classique peut être implémentée par n’importe quelle classe, n’importe où, à tout moment. Une sealed interface fixe la liste fermée de ses implémentations avec permits : aucune autre classe ne peut l’implémenter.

**Bénéfice pour le compilateur et le dev dans un switch :**
Le compilateur connaît tous les cas possibles. Il peut donc vérifier que le switch les couvre tous (exhaustivité), sans default. Pour le dev, c’est plus sûr : si on oublie un cas, ça ne compile pas, au lieu de découvrir le bug à l’exécution.


### Question A.3 : 
**Pourquoi le compilateur garantit-il l’exhaustivité de votre switch sans avoir besoin du default ? Que se passerait-il si demain on ajoutait record Perdu(LocalDate dateConstat) implements StatutEmprunt {} à l’interface ?**

**Pourquoi pas besoin de default :**
L’interface est sealed : le mot permits donne la liste complète et fermée des cas possibles (EnCours, Rendu, EnRetard).
Le compilateur vérifie que le switch couvre chacun de ces cas. Si tous sont traités, aucun autre cas ne peut exister, donc le default ne servirait à rien.

**Si on ajoute Perdu :**
Il faut aussi l’ajouter dans permits, sinon Perdu ne compile pas.
Notre switch ne gère plus tous les cas. Le compilateur affiche alors une erreur sur PenaliteCalculator, et le projet ne compile plus tant qu’on n’a pas ajouté case Perdu ....
L’oubli est donc détecté tout de suite, pas en production. Avec un default, le nouveau cas serait passé dans le default sans que personne ne s’en rende compte.

____________________________________________

### Question B.2 : 
**Quelle est la différence concrète entre ${livre.titre} directement dans le HTML et <c:out value="${livre.titre}” /> ? Donnez des exemples précis de leur fonctionnement.*

**Différence :**
${livre.titre} directement dans le HTML insère la valeur telle quelle, sans rien protéger.
<c:out> échappe les caractères spéciaux du HTML (< devient &lt;, > devient &gt;, & devient &amp;, " devient &quot;) avant de les écrire (c’est escapeXml="true", la valeur par défaut).

**Exemples précis :**
Si un titre vaut <b>Gras</b>, avec ${livre.titre} le navigateur affiche Gras en gras : il a interprété le HTML. Avec <c:out>, il affiche le texte <b>Gras</b> tel quel.
Si un titre vaut <script>alert('XSS')</script>, avec ${livre.titre} le script s’exécute dans le navigateur de tous ceux qui voient la page (faille XSS). Avec <c:out>, il s’affiche comme du simple texte et ne s’exécute pas.
Un titre comme Tom & Jerry est aussi mieux affiché avec <c:out> (&amp;), ce qui garde le HTML valide.

### Question B.3 : 
**Décrivez en 3 phrases le scénario qui arrive à votre utilisateur si vous oubliez le PRG et que vous faites un forward après l’INSERT d’un livre.*

**1.** L’utilisateur remplit le formulaire et clique sur « Enregistrer » : la servlet insère le livre, puis fait un forward vers la liste, mais l’URL du navigateur reste celle du POST /livres.
**2.** S’il appuie sur F5, le navigateur affiche un avertissement et, s’il confirme, renvoie le même POST avec les mêmes données.
**3.** Le livre est alors inséré une deuxième fois : il y a un doublon en base, et l’utilisateur ne s’en rend peut-être même pas compte.

____________________________________________

### Question C.1 : 
**Citez deux raisons pour lesquelles cette interface paramétrée vaut le coup d’être créée dès le premier DAO du projet, plutôt que d’écrire directement une classe concrète.*

**Raison 1 : un contrat commun, sans répétition.**
Tous les DAO (LivreDAO, EmpruntDAO, et d’autres plus tard) ont les mêmes opérations de base, avec les mêmes noms. Le <T, ID> évite de réécrire une interface par entité.

**Raison 2 : flexibilité et tests.**
Le reste du code (les servlets) peut dépendre de l’interface et pas d’une classe précise. On peut changer l’implémentation (par exemple passer à une autre base) ou la remplacer par un faux DAO pour tester, sans modifier les servlets.


### Question C.2 : 
**CPourquoi PreparedStatement est-il systématiquement préféré à Statement ? Donnez les deux avantages principaux..*

**Avantage 1 : la sécurité (injection SQL)**
Avec un PreparedStatement, les valeurs sont envoyées séparément du texte SQL. Une valeur comme '; DROP TABLE livre; -- est traitée comme du simple texte, jamais comme du SQL.
Avec un Statement et une concaténation, l’utilisateur peut modifier la requête.

**Avantage 2 : la performance et le confort.**
La requête est préparée une fois par la base, puis peut être réutilisée avec d’autres valeurs.
On n’a pas à gérer soi-même les apostrophes, les dates ou les conversions de types : setString, setInt, setDate s’en chargent.

