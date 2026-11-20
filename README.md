# BiblioTech

Application web de gestion de bibliothèque universitaire (TP noté, introduction aux applications web Java EE).
Elle permet de consulter et gérer le catalogue de livres, d'enregistrer des emprunts et des retours, et de voir les emprunts en cours avec leurs éventuelles pénalités de retard.

## Stack technique

| Brique | Version utilisée |
|---|---|
| Java | 21 |
| Jakarta EE | 10 (`jakartaee-web-api` 10.0.0, en `provided`) |
| Tomcat | 10.1.55 |
| PostgreSQL | 16 (dans Docker) |
| Maven | 3.9+ |
| JSTL | 3.0 (API + implémentation Glassfish) |

## Structure du projet

```
bibliotech/
├── pom.xml
├── sql/
│   └── schema.sql                  tables + 5 livres et 2 étudiants de démo
└── src/main/
    ├── java/com/bibliotech/
    │   ├── model/                  Livre, Etudiant, Emprunt (records),
    │   │                           StatutEmprunt (sealed), PenaliteCalculator
    │   ├── dao/                    DatabaseConnection, GenericDAO, LivreDAO,
    │   │                           EmpruntDAO, LivreIndisponibleException
    │   └── web/                    HealthServlet, LivreServlet, EmpruntServlet
    └── webapp/WEB-INF/
        ├── web.xml
        ├── livres/                 list.jsp, form.jsp, detail.jsp
        └── emprunts/               list.jsp
```

L'architecture suit le schéma de l'énoncé : Servlet → (forward) → JSP/JSTL, et la Servlet appelle les DAO, qui parlent à PostgreSQL et manipulent des Records.
Il n'y a aucun HTML dans les Servlets et aucun scriptlet dans les JSP.

## Lancer le projet

### 1. Base de données (Docker)

```bash
docker run --name pg-bibliotech -e POSTGRES_PASSWORD=secret -p 5433:5432 -d postgres:16
docker exec -i pg-bibliotech psql -U postgres < sql/schema.sql
```

Le script `schema.sql` supprime puis recrée les tables : on peut le relancer pour repartir de zéro.

### 2. Variables d'environnement

Les identifiants de la base ne sont jamais dans le code. Tomcat les lit dans ses variables d'environnement :

```
DB_URL=jdbc:postgresql://localhost:5433/postgres
DB_USER=postgres
DB_PASSWORD=secret
```

Chez moi, Tomcat est un service système : je les ai définies dans `/etc/systemd/system/tomcat10.service.d/override.conf` (fichier hors du projet, donc le mot de passe n'est jamais poussé sur GitHub).

### 3. Compiler et déployer

```bash
mvn clean package
sudo cp target/bibliotech.war /var/lib/tomcat10/webapps/
sudo systemctl restart tomcat10
```

### 4. Vérifier

- `http://localhost:8080/bibliotech/health` doit afficher `OK · PostgreSQL 16.x ...`
- `http://localhost:8080/bibliotech/livres` affiche le catalogue.

## Routes

**LivreServlet** (`/livres/*`)

| Méthode | URL | Action |
|---|---|---|
| GET | `/livres` | Lister tous les livres |
| GET | `/livres/{id}` | Voir le détail d'un livre |
| GET | `/livres/nouveau` | Formulaire de création |
| POST | `/livres` | Créer un livre |
| GET | `/livres/{id}/edit` | Formulaire de modification |
| POST | `/livres/{id}` | Mettre à jour un livre |
| POST | `/livres/{id}/delete` | Supprimer un livre |

**EmpruntServlet** (`/emprunts/*`)

| Méthode | URL | Action |
|---|---|---|
| GET | `/emprunts` | Lister les emprunts en cours (retards inclus) |
| POST | `/emprunts` | Emprunter un livre (transaction) |
| POST | `/emprunts/{id}/retour` | Enregistrer un retour (transaction) |

Tous les POST réussis se terminent par une redirection 302 (Post-Redirect-Get).

## Écarts et choix par rapport à l'énoncé

Je les signale ici pour que ce soit clair.

**Environnement**
- PostgreSQL tourne sur le port **5433** au lieu de 5432, car le 5432 était déjà utilisé sur ma machine.
- Tomcat 10.1.55 est installé comme service système, et les variables d'environnement sont données via `systemctl` (fichier `override.conf`).
- J'ai ajouté `Class.forName("org.postgresql.Driver")` dans `DatabaseConnection`, car Tomcat n'enregistrait pas le driver tout seul.
- `DatabaseConnection.get()` déclare `throws SQLException`, c'est nécessaire pour que ça compile.
- JSTL : la `jakartaee-web-api` ne contient pas l'implémentation de JSTL, j'ai donc ajouté deux dépendances (`jakarta.servlet.jsp.jstl-api` 3.0.0 et l'implémentation Glassfish 3.0.1), sans `provided` puisque Tomcat ne les fournit pas.

**Modèle et DAO**
- J'ai ajouté des contrôles supplémentaires dans `Emprunt` que l'énoncé ne demandait pas (dates et statut non null, date de retour prévue pas avant la date d'emprunt).
- Le record `Rendu` ne contient qu'un booléen (`enRetardLorsDuRetour`) et pas le nombre de jours de retard. `PenaliteCalculator` renvoie donc 0 € pour un emprunt rendu, car on ne peut pas calculer de pénalité (l'énoncé dit « si vous avez stocké le retard »).
- Les méthodes de `GenericDAO` ne déclarent pas `SQLException` : dans les DAO, je l'attrape et je la relance en `RuntimeException`.
- Le statut est stocké en base comme du texte (`EN_COURS`, `RENDU`, `EN_RETARD`), et le DAO le **recalcule à partir des dates** à la lecture, pour qu'il soit toujours à jour.
- `LivreIndisponibleException` est une `RuntimeException`, pour ne pas avoir de `throws` partout. Elle est aussi levée si le livre n'existe pas (avec un message différent).
- Le retour d'un livre (`retournerEmprunt`) est aussi fait dans une transaction : date de retour et stock +1 ensemble. Il utilise `UPDATE ... RETURNING`, une extension de PostgreSQL qui évite un `SELECT` supplémentaire.

**Base de données**
- Dans `schema.sql`, j'ai ajouté 2 étudiants de démo (pour tester les emprunts) et deux contraintes `CHECK` sur `livre` (`exemplaires_disponibles >= 0` et `<= exemplaires_total`).
- Pour le Bonus 1, j'ai retiré **temporairement** la contrainte `CHECK (exemplaires_disponibles >= 0)` afin de reproduire la faille, puis je l'ai remise (même nom, `livre_exemplaires_disponibles_check`).
- Autour de minuit, `CURRENT_DATE` (PostgreSQL dans Docker) et `LocalDate.now()` (Java) peuvent différer d'un jour à cause du fuseau horaire.

**Couche web**
- J'ai réalisé le DAO des livres (partie C) avant la couche web (partie B), car les pages ont besoin de lire la base.
- En EL, j'appelle les accesseurs des records avec des parenthèses (`livre.titre()`), car Tomcat 10.1 cherche des `getXxx()`, que les records n'ont pas.
- À la création d'un livre, `exemplairesDisponibles` est égal à `exemplairesTotal`. À la modification, on garde les disponibles actuels sans dépasser le nouveau total.
- Dans le formulaire, les champs numériques sont en `type="text"` et pas `type="number"` : sinon le navigateur bloquerait lui-même les saisies invalides et on ne verrait pas mes messages d'erreur.
- Validation (B.4) : la servlet vérifie elle-même chaque champ et les plages (année entre 1500 et l'année courante, au moins 1 exemplaire) pour afficher **toutes les erreurs d'un coup**. Le record reste le dernier garde-fou et son `IllegalArgumentException` est attrapée.
- Pour réafficher le formulaire après une erreur, j'utilise un `forward` (pour garder la saisie et les erreurs). Le PRG ne s'applique qu'après un POST réussi.
- La page des emprunts affiche aussi les emprunts en retard (un retard est un emprunt non rendu), avec leur pénalité calculée par `PenaliteCalculator`. Elle montre le titre du livre et le nom de l'étudiant (deux petites requêtes) à la place des numéros.
- Pour emprunter, le formulaire du catalogue demande le numéro d'étudiant et la durée en jours (pas de liste déroulante, pour rester simple).
- Pour le Bonus 2, la recherche est un formulaire en GET (`/livres?q=...`). `LivreDAO.findByTitre` utilise `ILIKE` (PostgreSQL, insensible à la casse) avec un `PreparedStatement` : les `%` sont ajoutés à la valeur, jamais au texte SQL.
- Les messages après un POST (« Emprunt enregistré. », erreurs...) passent par la session, car la redirection perd les attributs de la requête. Ils sont supprimés après le premier affichage.

**Tests**
- Les petits programmes de test manuel (`Test*.java`) sont gardés dans le dépôt pour montrer mes vérifications.

---

## Réponses aux questions de réflexion

### Question 0.1
**Pourquoi la dépendance jakarta.jakartaee-web-api est-elle déclarée en provided dans le pom.xml plutôt qu'en compile ? Que se passerait-il si vous la passiez en compile ?**

Elle est en `provided` parce que Tomcat fournit déjà l'API Jakarta EE à l'exécution. J'en ai besoin pour compiler mon code, mais pas pour l'embarquer dans le WAR.

Si je la passais en `compile`, les JAR de l'API seraient copiés dans `WEB-INF/lib`. Le WAR serait plus lourd, et les classes seraient en double (celles de Tomcat et celles du WAR), ce qui peut provoquer des conflits de versions ou des erreurs de chargement de classes.

### Question 0.2
**Pourquoi est-il déconseillé de mettre en dur les credentials BD dans le code source poussé sur Git ? Citez deux moyens de les externaliser proprement (autres que les variables d'environnement).**

Le code est visible par tous ceux qui ont accès au dépôt, et l'historique Git garde les anciennes versions : un mot de passe supprimé reste récupérable. Chaque environnement (développement, production) a aussi ses propres identifiants.

Deux autres moyens de les externaliser :
1. Un fichier de configuration (`db.properties`) placé hors du dépôt, ou listé dans `.gitignore`.
2. Une `DataSource` JNDI configurée dans Tomcat (`context.xml`), où les identifiants sont gérés par le serveur et pas par l'application.

### Question A.1
**Pourquoi un Record est-il immuable par défaut ? Quelle conséquence cela a-t-il sur la thread-safety dans une application web où la même Servlet est invoquée par plusieurs threads simultanés ?**

Un record déclare ses champs `private final` et ne génère pas de setters : une fois le constructeur terminé, on ne peut plus rien modifier. Pour changer une valeur, il faut créer un nouveau record.

Côté threads, un objet qui ne change jamais peut être lu par plusieurs threads en même temps sans risque : pas de `synchronized` à écrire, pas de lecture d'un état à moitié modifié. C'est pratique dans une servlet, qui est une instance unique partagée par tous les threads. (Attention : l'immuabilité est « superficielle », si un champ contient une liste modifiable, la liste peut quand même changer.)

### Question A.2
**Quelle différence entre une sealed interface et une interface classique ? Quel bénéfice pour le compilateur (et pour vous, le dev) lorsque vous écrivez un switch sur cette interface ?**

Une interface classique peut être implémentée par n'importe quelle classe, n'importe où, à tout moment. Une sealed interface fixe la liste fermée de ses implémentations avec `permits` : aucune autre classe ne peut l'implémenter.

Le compilateur connaît donc tous les cas possibles. Il peut vérifier que le `switch` les couvre tous (exhaustivité), sans `default`. Pour moi, c'est plus sûr : si j'oublie un cas, ça ne compile pas, au lieu de découvrir le bug à l'exécution.

### Question A.3
**Pourquoi le compilateur garantit-il l'exhaustivité de votre switch sans avoir besoin du default ? Que se passerait-il si demain on ajoutait record Perdu(LocalDate dateConstat) implements StatutEmprunt {} à l'interface ?**

L'interface est `sealed` : le mot `permits` donne la liste complète et fermée des cas possibles (`EnCours`, `Rendu`, `EnRetard`). Le compilateur vérifie que le `switch` couvre chacun de ces cas. Si tous sont traités, aucun autre cas ne peut exister, donc le `default` ne servirait à rien.

Si on ajoute `Perdu`, il faut aussi l'ajouter dans `permits`, sinon il ne compile pas. Mon `switch` ne gère alors plus tous les cas : le compilateur affiche une erreur sur `PenaliteCalculator`, et le projet ne compile plus tant que je n'ai pas ajouté `case Perdu ...`. L'oubli est donc détecté tout de suite, pas en production. Avec un `default`, le nouveau cas serait passé dedans sans que personne ne s'en rende compte.

### Question B.1
**Pourquoi le HTML standard ne supporte que GET et POST côté formulaire ?**

La spécification HTML ne prévoit que `get` et `post` pour l'attribut `method` d'un `<form>`. C'est un choix historique : les premiers navigateurs avaient seulement besoin de lire une page (GET) et d'envoyer des données (POST).

PUT et DELETE existent bien en HTTP, mais un navigateur ne les envoie pas depuis un formulaire, seulement en JavaScript (`fetch`). C'est pour ça que mon application utilise `POST /livres/{id}/delete` au lieu d'un vrai `DELETE`, et `POST /livres/{id}` pour la modification au lieu d'un `PUT`.

### Question B.2
**Quelle est la différence concrète entre ${livre.titre} directement dans le HTML et <c:out value="${livre.titre}" /> ? Donnez des exemples précis de leur fonctionnement.**

`${livre.titre}` directement dans le HTML insère la valeur telle quelle, sans rien protéger. `<c:out>` échappe les caractères spéciaux du HTML (`<` devient `&lt;`, `>` devient `&gt;`, `&` devient `&amp;`, `"` devient `&quot;`) avant de les écrire (c'est `escapeXml="true"`, la valeur par défaut).

Exemples :
- Si un titre vaut `<b>Gras</b>`, avec `${livre.titre}` le navigateur affiche **Gras** en gras : il a interprété le HTML. Avec `<c:out>`, il affiche le texte `<b>Gras</b>` tel quel.
- Si un titre vaut `<script>alert('XSS')</script>`, avec `${livre.titre}` le script s'exécute dans le navigateur de tous ceux qui voient la page (faille XSS). Avec `<c:out>`, il s'affiche comme du simple texte.
- Un titre comme `Tom & Jerry` est aussi mieux affiché avec `<c:out>` (`&amp;`), ce qui garde le HTML valide.

Dans mon projet, j'utilise `<c:out>` partout.

### Question B.3
**Décrivez en 3 phrases le scénario qui arrive à votre utilisateur si vous oubliez le PRG et que vous faites un forward après l'INSERT d'un livre.**

1. L'utilisateur remplit le formulaire et clique sur « Enregistrer » : la servlet insère le livre, puis fait un forward vers la liste, mais l'URL du navigateur reste celle du `POST /livres`.
2. S'il appuie sur F5, le navigateur affiche un avertissement et, s'il confirme, renvoie le même POST avec les mêmes données.
3. Le livre est alors inséré une deuxième fois : il y a un doublon en base, et l'utilisateur ne s'en rend peut-être même pas compte.

### Question C.1
**Citez deux raisons pour lesquelles cette interface paramétrée vaut le coup d'être créée dès le premier DAO du projet, plutôt que d'écrire directement une classe concrète.**

1. **Un contrat commun, sans répétition.** Tous les DAO (`LivreDAO`, `EmpruntDAO`, et d'autres plus tard) ont les mêmes opérations de base, avec les mêmes noms. Le `<T, ID>` évite de réécrire une interface par entité.
2. **Flexibilité et tests.** Le reste du code (les servlets) peut dépendre de l'interface et pas d'une classe précise. On peut changer l'implémentation (par exemple passer à une autre base) ou la remplacer par un faux DAO pour tester, sans modifier les servlets.

### Question C.2
**Pourquoi PreparedStatement est-il systématiquement préféré à Statement ? Donnez les deux avantages principaux.**

1. **La sécurité (injection SQL).** Avec un `PreparedStatement`, les valeurs sont envoyées séparément du texte SQL. Une valeur comme `'; DROP TABLE livre; --` est traitée comme du simple texte, jamais comme du SQL. Avec un `Statement` et une concaténation, l'utilisateur peut modifier la requête.
2. **La performance et le confort.** La requête est préparée une fois par la base, puis peut être réutilisée avec d'autres valeurs. Je n'ai pas à gérer moi-même les apostrophes, les dates ou les conversions de types : `setString`, `setInt`, `setDate` s'en chargent.

### Question C.3
**Que se passe-t-il si vous oubliez le setAutoCommit(false) et qu'une exception est levée entre la mise à jour du livre et l'INSERT de l'emprunt ? Décrivez précisément l'état de la base après l'erreur.**

Sans `setAutoCommit(false)`, la connexion est en mode auto-commit : chaque requête est validée définitivement dès qu'elle est exécutée. L'`UPDATE` qui décrémente le stock est donc déjà commité quand l'exception arrive. Il n'y a rien à annuler : `rollback()` n'a aucun effet.

État de la base après l'erreur :
- le livre a `exemplaires_disponibles` diminué de 1 ;
- aucun emprunt n'a été inséré ;
- la base est incohérente : un exemplaire a « disparu » du stock sans qu'aucun étudiant ne l'ait emprunté, et à chaque nouvelle erreur, un exemplaire de plus se perd.

---

## Bonus

### Bonus 1 : race condition sur le dernier exemplaire

**Étape 1 : reproduction.**
J'ai écrit `TestRaceCondition`, un `main` de démo (l'énoncé l'accepte à la place de JUnit). Il lance deux threads, Alice et Bob, avec un `ExecutorService`. Un `CountDownLatch` les fait partir exactement en même temps sur le livre n° 5 (« Java Concurrency in Practice »), qui n'a qu'un exemplaire. Comme une race condition dépend du timing, le programme refait l'expérience 20 fois.
Pour pouvoir observer la faille, j'ai retiré temporairement la contrainte `CHECK (exemplaires_disponibles >= 0)`, sinon la base aurait refusé le second emprunt.
Résultat avant correction : **les deux emprunts ont réussi 20 fois sur 20, et le stock est tombé à -1**.

**Étape 2 : diagnostic.**
Alice et Bob font tous les deux le `SELECT` avant que l'un ait modifié le stock : ils lisent 1 et passent tous les deux la vérification. Chacun fait ensuite `UPDATE ... exemplaires_disponibles - 1`. Le second attend que le premier valide, puis retire 1 à la valeur à jour (0), ce qui donne -1. Les deux `INSERT` réussissent : deux emprunts pour un seul exemplaire.
PostgreSQL utilise par défaut le niveau d'isolation `READ COMMITTED` : une requête voit seulement les données déjà validées. Ça évite de lire des données non validées, mais pas de lire la même ancienne valeur en même temps. Notre `SELECT` ne verrouille rien : la vérification « il reste un exemplaire ? » peut donc être fausse au moment où on décrémente. Le problème vient du schéma « vérifier, puis modifier » en deux temps.

**Étape 3 : correction.**
Deux corrections étaient possibles :
1. **`SELECT ... FOR UPDATE`** : la ligne du livre est verrouillée dès la lecture, jusqu'au `commit` ou au `rollback`. Si Alice a la ligne, Bob attend, puis relit le stock à jour (0) et est refusé.
2. **`CHECK (exemplaires_disponibles >= 0)`** : la base refuse tout `UPDATE` qui mettrait le stock sous 0, et la transaction est annulée.

J'ai implémenté la première, en ajoutant seulement `FOR UPDATE` à la requête de vérification du stock dans `enregistrerEmprunt`. Résultat avec le même test : à chaque essai, **un seul** emprunt réussit, le stock final est 0, et « les deux emprunts réussis » passent de **20 à 0 sur 20**.

Pourquoi pas le `CHECK` seul :
- Bob recevrait une erreur SQL brute, qu'on ne peut pas distinguer d'un autre problème, et l'utilisateur verrait un message peu clair.
- Le `CHECK` empêche seulement la donnée fausse (-1) : il ne fait pas la vérification métier. Avec `FOR UPDATE`, la vérification « stock > 0 » en Java redevient fiable, et Bob reçoit mon `LivreIndisponibleException` avec un message clair.

Pourquoi pas `SERIALIZABLE` : il marche aussi, mais il peut annuler des transactions avec une erreur de sérialisation, et il faudrait écrire du code pour les relancer. `FOR UPDATE` est plus simple et ne verrouille que la ligne concernée.

J'ai gardé le `CHECK` dans `schema.sql` comme **filet de sécurité** : si un autre bout de code oublie le verrou, la base refusera quand même le stock négatif.

### Bonus 2 : le piège XSS dans la recherche

J'ai ajouté dans `livres/list.jsp` un champ de recherche par titre (formulaire GET, `?q=...`).

**Étape 1 : exploit.**
Dans une première version volontairement vulnérable, la page affichait `Résultats pour : ${param.q}`. En saisissant `<script>alert('XSS pwned')</script>`, le navigateur a exécuté le script et affiché la boîte de dialogue : le texte saisi est réinjecté dans la page sans protection, donc il est interprété comme du HTML. La preuve est dans la capture `captures/04-xss-exploit.png`.

**Étape 2 : correction.**
J'ai remplacé `${param.q}` par `<c:out value="${param.q}" />`. Avec la même saisie, plus aucune boîte de dialogue : le texte s'affiche tel quel (`<` devient `&lt;`, `>` devient `&gt;`). La preuve est dans la capture `captures/05-xss-corrige.png`.

**Étape 3 : où d'autre y a-t-il le même risque ?**
J'ai cherché dans les JSP tous les `${...}` qui ne sont pas dans un `<c:out>` (commande `grep -rn '\${' src/main/webapp/WEB-INF | grep -v 'c:out'`). Il ne reste que des adresses (`${pageContext.request.contextPath}`, fournie par le serveur) et des numéros (`${livre.id()}`, `${emprunt.id()}`, venant de la base), et `${action}` du formulaire, qui est construit avec le `contextPath` et un id converti en nombre. Aucune ne contient de texte libre saisi par l'utilisateur.

Les endroits où une donnée venant de l'utilisateur est réinjectée dans une page sont tous protégés par `<c:out>` :
- le titre, l'auteur, l'année et le nombre d'exemplaires dans `livres/list.jsp` et `livres/detail.jsp` ;
- les valeurs de `form.jsp` (réaffichées après une erreur de validation) et les messages d'erreur sous les champs ;
- le titre du livre, le nom de l'étudiant et le message de confirmation ou d'erreur dans `emprunts/list.jsp` ;
- le texte de la recherche (`param.q`), maintenant corrigé.

Sans `<c:out>`, ces champs auraient été vulnérables : n'importe qui pourrait créer un livre dont le titre contient un script, qui s'exécuterait chez tous ceux qui ouvrent le catalogue.

## Captures d'écran

Les captures sont dans le dossier `captures/` :
1. `01-catalogue.png` : page d'accueil avec le catalogue
2. `02-ajout-livre.png` : formulaire d'ajout de livre
3. `03-emprunts-en-cours.png` : liste des emprunts en cours
4. `04-xss-exploit.png` : bonus 2, le script s'exécute (version vulnérable)
5. `05-xss-corrige.png` : bonus 2, le texte est échappé (version corrigée)