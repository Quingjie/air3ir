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

