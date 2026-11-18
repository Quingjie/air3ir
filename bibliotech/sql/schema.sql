-- On repart de zéro à chaque exécution
DROP TABLE IF EXISTS emprunt;
DROP TABLE IF EXISTS etudiant;
DROP TABLE IF EXISTS livre;

CREATE TABLE livre (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    auteur VARCHAR(200) NOT NULL,
    annee_publication INT NOT NULL,
    exemplaires_total INT NOT NULL,
    exemplaires_disponibles INT NOT NULL,
    CHECK (exemplaires_disponibles >= 0),
    CHECK (exemplaires_disponibles <= exemplaires_total)
);

CREATE TABLE etudiant (
    id BIGSERIAL PRIMARY KEY,
    numero_etudiant VARCHAR(8) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL
);

CREATE TABLE emprunt (
    id BIGSERIAL PRIMARY KEY,
    livre_id BIGINT NOT NULL REFERENCES livre(id),
    etudiant_id BIGINT NOT NULL REFERENCES etudiant(id),
    date_emprunt DATE NOT NULL,
    date_retour_prevue DATE NOT NULL,
    date_retour_effective DATE,
    statut VARCHAR(20) NOT NULL
);

-- 5 livres de démo
INSERT INTO livre (titre, auteur, annee_publication, exemplaires_total, exemplaires_disponibles) VALUES
('Effective Java', 'Joshua Bloch', 2018, 3, 3),
('Clean Code', 'Robert C. Martin', 2008, 2, 2),
('Le Petit Prince', 'Antoine de Saint-Exupéry', 1943, 4, 4),
('Les Misérables', 'Victor Hugo', 1862, 2, 2),
('Java Concurrency in Practice', 'Brian Goetz', 2006, 1, 1);

-- 2 étudiants de démo (pour tester les emprunts plus tard)
INSERT INTO etudiant (numero_etudiant, nom, prenom, email) VALUES
('ET123456', 'Martin', 'Alice', 'alice.martin@univ.fr'),
('ET654321', 'Durand', 'Bob', 'bob.durand@univ.fr');