-- Schéma de la base de données hotel_db
-- À exécuter sur le serveur PostgreSQL (local ou distant) :
--   psql -h <hote> -U postgres -d hotel_db -f schema.sql
-- La base doit d'abord être créée : CREATE DATABASE hotel_db;

CREATE TABLE IF NOT EXISTS solde (
    id SERIAL PRIMARY KEY,
    solde_actuel INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chambre (
    num_chambre VARCHAR(20) PRIMARY KEY,
    design VARCHAR(100),
    type VARCHAR(50),
    prix_nuitee INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS reserver (
    id_reserv SERIAL PRIMARY KEY,
    num_chambre VARCHAR(20) REFERENCES chambre(num_chambre),
    date_reserv DATE NOT NULL,
    date_entree DATE NOT NULL,
    nbr_jour INTEGER NOT NULL,
    nom_client VARCHAR(100),
    mail VARCHAR(150),
    annulee BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS occuper (
    id_occup SERIAL PRIMARY KEY,
    -- UNIQUE : une réservation ne peut être occupée qu'une seule fois
    -- (protège aussi le solde contre un double encaissement)
    id_reserv INTEGER REFERENCES reserver(id_reserv) UNIQUE,
    -- Montant crédité au solde lors de l'arrivée (permet remboursements/rectifications)
    montant INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sejourner (
    id_sejour SERIAL PRIMARY KEY,
    num_chambre VARCHAR(20) REFERENCES chambre(num_chambre),
    date_entree_sejour DATE NOT NULL,
    nbr_jour INTEGER NOT NULL,
    nom_client VARCHAR(100),
    telephone VARCHAR(30),
    -- Montant crédité au solde à l'enregistrement (permet remboursements/rectifications)
    montant INTEGER NOT NULL DEFAULT 0
);

-- Ligne unique de solde au départ (id = 1)
INSERT INTO solde (id, solde_actuel) VALUES (1, 0)
ON CONFLICT (id) DO NOTHING;
