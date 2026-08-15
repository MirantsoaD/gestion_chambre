# STEPS.md — Application de Gestion des Réservations d'Hôtel

> Objectif : application desktop simple en **Java Swing** + **PostgreSQL**, niveau projet étudiant.
> Ce document liste toutes les étapes à suivre, dans l'ordre, pour un agent IA (ou un développeur) qui reprend le projet.

---

## 0. Stack technique

- **Langage** : Java 17 (ou 11 minimum)
- **UI** : Java Swing (pas de framework externe, juste `javax.swing`)
- **Base de données** : PostgreSQL
- **Accès BD** : JDBC pur (driver `org.postgresql:postgresql`), pas d'ORM (pour rester simple)
- **Build** : Maven (plus simple à gérer pour un agent IA que Gradle)
- **Envoi de mail** : Jakarta Mail (ex-JavaMail) via SMTP (Gmail SMTP ou Mailtrap pour les tests)
- **Architecture** : simple 3 couches — `model` / `dao` / `ui` (pas besoin de Spring, MVC "à la main")

---

## 1. Initialisation du projet

1. Créer un projet Maven `hotel-reservation`.
2. Structure de dossiers :
   ```
   src/main/java/com/hotel/
       model/        -> classes Java (Chambre, Reserver, Occuper, Sejourner, Solde)
       dao/          -> classes d'accès BD (ChambreDAO, ReserverDAO, OccuperDAO, SejournerDAO, SoldeDAO)
       ui/           -> fenêtres Swing (MainFrame, ChambrePanel, ReserverPanel, OccuperPanel, SejournerPanel)
       util/         -> DBConnection.java, MailSender.java
       Main.java
   src/main/resources/
       db.properties  -> config connexion BD
       mail.properties -> config SMTP
   ```
3. Dépendances `pom.xml` :
   - `org.postgresql:postgresql`
   - `com.sun.mail:jakarta.mail`
   - (optionnel) `com.formdev:flatlaf` pour une UI Swing plus jolie sans effort

---

## 2. Base de données PostgreSQL

### 2.1 Créer la base

```sql
CREATE DATABASE hotel_db;
```

### 2.2 Script SQL des tables (`schema.sql`)

```sql
CREATE TABLE solde (
    id SERIAL PRIMARY KEY,
    solde_actuel INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE chambre (
    num_chambre VARCHAR(20) PRIMARY KEY,
    design VARCHAR(100),
    type VARCHAR(50),
    prix_nuitee INTEGER NOT NULL
);

CREATE TABLE reserver (
    id_reserv SERIAL PRIMARY KEY,
    num_chambre VARCHAR(20) REFERENCES chambre(num_chambre),
    date_reserv DATE NOT NULL,
    date_entree DATE NOT NULL,
    nbr_jour INTEGER NOT NULL,
    nom_client VARCHAR(100),
    mail VARCHAR(150),
    annulee BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE occuper (
    id_occup SERIAL PRIMARY KEY,
    id_reserv INTEGER REFERENCES reserver(id_reserv)
);

CREATE TABLE sejourner (
    id_sejour SERIAL PRIMARY KEY,
    num_chambre VARCHAR(20) REFERENCES chambre(num_chambre),
    date_entree_sejour DATE NOT NULL,
    nbr_jour INTEGER NOT NULL,
    nom_client VARCHAR(100),
    telephone VARCHAR(30)
);

-- Ligne unique de solde au départ
INSERT INTO solde (solde_actuel) VALUES (0);
```

> Note : `annulee` (booléen) est ajouté pour gérer l'annulation de réservation sans supprimer physiquement la ligne (permet de libérer la chambre facilement — voir règle métier §5).

### 2.3 Config connexion (`db.properties`)

```properties
db.url=jdbc:postgresql://localhost:5432/hotel_db
db.user=postgres
db.password=postgres
```

---

## 3. Couche `model` (classes simples / POJO)

Créer une classe par table, avec juste les attributs + getters/setters :

- `Chambre` : numChambre, design, type, prixNuitee
- `Reserver` : idReserv, numChambre, dateReserv, dateEntree, nbrJour, nomClient, mail, annulee
- `Occuper` : idOccup, idReserv
- `Sejourner` : idSejour, numChambre, dateEntreeSejour, nbrJour, nomClient, telephone
- `Solde` : id, soldeActuel

---

## 4. Couche `dao` (JDBC)

### 4.1 `DBConnection.java`
- Charge `db.properties`, ouvre une `Connection` via `DriverManager.getConnection(...)`.
- Une méthode statique `getConnection()` réutilisable partout.

### 4.2 Pour chaque table, une classe DAO avec les méthodes CRUD standard :

**ChambreDAO**
- `ajouter(Chambre c)`
- `lister()` -> `List<Chambre>`
- `modifier(Chambre c)`
- `supprimer(String numChambre)`
- `trouverParNum(String numChambre)`

**ReserverDAO**
- `ajouter(Reserver r)` (avec date de réservation = date du jour, `LocalDate.now()`)
- `lister()`
- `modifier(Reserver r)`
- `supprimer(int idReserv)` -> suppression réelle si besoin
- `annuler(int idReserv)` -> passe `annulee = true` (libère la chambre, cf §5)
- `estChambreDisponible(String numChambre, LocalDate dateEntree, int nbrJour)` -> vérifie qu'aucune réservation active ne chevauche cette période pour cette chambre (règle métier centrale, cf §7)

**OccuperDAO**
- `ajouter(Occuper o)` -> déclenche l'incrément du solde (cf §6)
- `lister()`
- `supprimer(int idOccup)`

**SejournerDAO**
- `ajouter(Sejourner s)` (avec dateEntreeSejour = date du jour) -> déclenche l'incrément du solde (cf §6)
- `lister()`
- `modifier(Sejourner s)`
- `supprimer(int idSejour)`

**SoldeDAO**
- `getSolde()` -> lit la ligne unique de solde
- `ajouterAuSolde(int montant)` -> `UPDATE solde SET solde_actuel = solde_actuel + montant WHERE id = 1`

---

## 5. Règle métier — Annulation de réservation

- Ajouter un bouton "Annuler" dans l'écran Réservation.
- Au clic : `ReserverDAO.annuler(idReserv)` met `annulee = true`.
- Une fois annulée, la chambre redevient disponible pour les dates concernées (car `estChambreDisponible` ignore les réservations où `annulee = true`).

---

## 6. Règle métier — Mise à jour du solde

Le solde doit s'additionner (`soldeActuel += montant`) dans 2 cas :

1. **Arrivée d'un client qui avait réservé** : quand on crée une ligne `OCCUPER` liée à une `RESERVER`.
   - Calcul du montant : `prixNuitee (de la chambre) * nbrJour (de la réservation)`.
   - Après insertion dans `OCCUPER`, appeler `SoldeDAO.ajouterAuSolde(montant)`.

2. **Arrivée d'un client sans réservation (walk-in)** : quand on crée une ligne `SEJOURNER`.
   - Calcul du montant : `prixNuitee (de la chambre) * nbrJour (du séjour)`.
   - Après insertion dans `SEJOURNER`, appeler `SoldeDAO.ajouterAuSolde(montant)`.

Afficher le solde actuel quelque part visible dans l'UI (ex : label en haut de la fenêtre principale, rafraîchi après chaque opération).

---

## 7. Règle métier — Disponibilité des chambres

Une chambre réservée sur une période donnée ne doit plus être proposée/réservable par un autre client sur une période qui chevauche.

- Avant toute nouvelle insertion dans `RESERVER` (et aussi avant `SEJOURNER` si on veut être cohérent), vérifier :
  ```sql
  SELECT COUNT(*) FROM reserver
  WHERE num_chambre = ?
    AND annulee = FALSE
    AND date_entree <= ?  -- nouvelle date_entree + nbr_jour (fin de la nouvelle période)
    AND (date_entree + (nbr_jour || ' days')::interval) >= ?  -- nouvelle date_entree
  ```
- Si le count > 0 → chambre indisponible → afficher un message d'erreur Swing (`JOptionPane.showMessageDialog`) et bloquer l'enregistrement.
- Implémenter cette vérification dans `ReserverDAO.estChambreDisponible(...)` et l'appeler dans le panel UI avant `ajouter()`.

---

## 8. Couche `ui` (Swing)

### 8.1 `MainFrame.java`
- Fenêtre principale avec un `JTabbedPane` à 4 onglets : **Chambres**, **Réservations**, **Occupations**, **Séjours**.
- Un label en haut affichant le solde actuel (rafraîchi à chaque changement d'onglet ou après une action).

### 8.2 Pour chaque onglet, un `JPanel` dédié avec :
- Un `JTable` (via `DefaultTableModel`) listant les données (bouton "Rafraîchir" ou chargement auto).
- Un formulaire simple (champs `JTextField`, `JComboBox` pour choisir une chambre, `JSpinner`/`JTextField` pour les dates) avec boutons : **Ajouter**, **Modifier**, **Supprimer**, **Annuler** (pour Réservation uniquement).
- Un clic sur une ligne du tableau remplit le formulaire (pour modifier/supprimer facilement).

### 8.3 Panel Chambre (`ChambrePanel`)
- CRUD simple sur numChambre, design, type, prixNuitee.

### 8.4 Panel Réservation (`ReserverPanel`)
- Champs : chambre (JComboBox rempli depuis ChambreDAO), date d'entrée (JTextField format `yyyy-MM-dd` ou `JSpinner` avec `SpinnerDateModel`), nombre de jours, nom client, mail.
- La date de réservation n'est pas saisie : elle est fixée automatiquement à `LocalDate.now()` dans le DAO.
- Avant ajout : appeler `estChambreDisponible(...)`. Si indisponible → message d'erreur.
- Après ajout réussi : appeler `MailSender.envoyerMailReservation(...)` (cf §9).
- Bouton "Annuler la réservation sélectionnée".

### 8.5 Panel Occupation (`OccuperPanel`)
- Champ : sélection d'une réservation existante et non encore "occupée" (JComboBox listant les `idReserv` actifs).
- Au clic "Ajouter" (= le client arrive) : insertion dans `OCCUPER` + appel `SoldeDAO.ajouterAuSolde(...)` avec le montant calculé (cf §6).

### 8.6 Panel Séjour (`SejournerPanel`)
- Champs : chambre, nombre de jours, nom client, téléphone.
- La date d'entrée est fixée automatiquement à `LocalDate.now()`.
- Au clic "Ajouter" : insertion dans `SEJOURNER` + appel `SoldeDAO.ajouterAuSolde(...)`.

---

## 9. Envoi automatique de mail

### 9.1 `MailSender.java` (util)
- Charger `mail.properties` (host SMTP, port, user, password, from).
- Méthode `envoyerMailReservation(String destinataire, Reserver r, Chambre c)` :
  - Sujet : "Confirmation de votre réservation"
  - Corps (texte simple) contenant :
    - Numéro de chambre
    - Date de réservation
    - Date d'entrée
    - Nombre de jours
    - Nom du client
  - Utiliser Jakarta Mail (`Session`, `Transport.send(...)`) en SMTP avec authentification TLS.

### 9.2 `mail.properties`
```properties
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.smtp.auth=true
mail.smtp.starttls.enable=true
mail.user=votreadresse@gmail.com
mail.password=motdepasse_application
```
> Utiliser un "mot de passe d'application" Gmail (pas le mot de passe du compte). Pour les tests sans vrai envoi, on peut utiliser un service comme Mailtrap.io.

### 9.3 Intégration
- Dans `ReserverPanel`, juste après un `ajouter()` réussi sur `ReserverDAO`, appeler `MailSender.envoyerMailReservation(...)` dans un `try/catch` (ne doit pas bloquer l'appli si l'envoi échoue → juste afficher un avertissement).

---

## 10. Tests manuels à faire avant de rendre le projet

- [ ] Ajouter/modifier/supprimer une chambre.
- [ ] Créer une réservation → vérifier que le mail part bien.
- [ ] Essayer de réserver la même chambre sur une période qui chevauche → doit être refusé.
- [ ] Annuler une réservation → vérifier que la chambre redevient disponible sur cette période.
- [ ] Faire "arriver" un client réservé (ajout dans OCCUPER) → vérifier que le solde augmente du bon montant.
- [ ] Créer un séjour direct (sans réservation) → vérifier que le solde augmente aussi.
- [ ] Vérifier l'affichage du solde dans l'UI après chaque opération.

---

## 11. Finitions (optionnel si le temps le permet)

- Ajouter FlatLaf pour un look plus moderne (`UIManager.setLookAndFeel(new FlatLightLaf())` dans `Main.java`).
- Ajouter un export CSV simple des réservations.
- Ajouter une petite validation des champs (dates non vides, nombre de jours > 0, mail au bon format) avant chaque insertion.

---

## 12. Ordre de développement recommandé (pour l'agent IA)

1. Créer le projet Maven + `pom.xml` avec les dépendances.
2. Créer le script SQL et la base PostgreSQL.
3. Créer `DBConnection.java` et tester la connexion.
4. Créer les classes `model`.
5. Créer les DAO un par un, en testant chaque méthode avec un petit `main()` de test.
6. Créer `MainFrame.java` avec les 4 onglets vides.
7. Développer `ChambrePanel` (le plus simple, sert de base pour les autres).
8. Développer `ReserverPanel` + règle de disponibilité (§7).
9. Développer `MailSender` et le brancher sur `ReserverPanel`.
10. Développer `OccuperPanel` + mise à jour du solde (§6).
11. Développer `SejournerPanel` + mise à jour du solde (§6).
12. Ajouter l'annulation de réservation (§5).
13. Faire les tests manuels de la section 10.
14. Finitions (§11) si le temps le permet.
