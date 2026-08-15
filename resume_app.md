# RESUME_APP — Application de Gestion des Réservations d'Hôtel

> Document de passation destiné à toute nouvelle personne reprenant le projet.
> Il couvre les aspects **fonctionnels**, **non fonctionnels** et **techniques**.
> Projet étudiant : application desktop Java Swing + PostgreSQL.

---

## 1. Présentation générale

**Objectif** : gérer les réservations d'un hôtel :
- CRUD complet sur 4 entités métier : **chambres, réservations, occupations, séjours**
- Envoi automatique d'un **mail de confirmation** au client après une réservation
- Règle de **non-double réservation** d'une chambre sur une même période (sauf réservation annulée)
- **Recherche de chambres libres** sur une période donnée
- **Génération d'un reçu client au format PDF**
- **Suivi du solde** de l'hôtel, crédité/rectifié automatiquement selon les opérations

**Forme** : application desktop mono-utilisateur, fenêtre unique avec onglets, interface 100 % en français.

**Contraintes du projet** :
- La base de données **PostgreSQL est hébergée sur une machine distante** (pas de serveur local)
- L'application est lancée **depuis IntelliJ IDEA** (projet Maven)
- Niveau étudiant : code simple, volontairement sans framework (pas de Spring, pas d'ORM)

---

## 2. Aspects fonctionnels

### 2.1 Les 5 onglets de la fenêtre principale

La fenêtre `MainFrame` affiche le **solde actuel** en haut (rafraîchi après chaque opération et à chaque changement d'onglet) et 5 onglets :

| Onglet | Panneau | But |
|---|---|---|
| **Chambres** | `ChambrePanel` | CRUD des chambres (numéro, design, type, prix/nuitée) |
| **Réservations** | `ReserverPanel` | CRUD + annulation des réservations |
| **Occupations** | `OccuperPanel` | Arrivée d'un client **ayant réservé** + transfert + reçu |
| **Séjours** | `SejournerPanel` | Arrivée d'un client **sans réservation** (walk-in) + reçu |
| **Chambres libres** | `ChambreLibrePanel` | Recherche des chambres libres sur une période |

Chaque panneau suit le même canevas : formulaire en haut (NORTH), tableau non éditable au centre (CENTER), boutons en bas (SOUTH). La sélection d'une ligne remplit le formulaire.

### 2.2 Règles métier

#### Dates
- Saisie **uniquement via calendrier** (composant LGoodDatePicker) : impossible de taper une date invalide
- Format d'affichage : **dd-MM-yyyy** (champs et tableaux)
- **Seules les dates du présent et du futur** sont autorisées (date minimale = aujourd'hui)

#### Réservation
- La date de réservation n'est pas saisie : fixée à **aujourd'hui** automatiquement
- **Contrôle de disponibilité** avant ajout/modification : une chambre ne peut pas être réservée sur une période qui **chevauche** une réservation active (voir §2.3)
- **Mail de confirmation automatique** après une réservation (chambre, dates, nombre de jours) — non bloquant si l'envoi échoue
- **Annulation** : passe `annulee = TRUE` (la ligne n'est pas supprimée ; la chambre redevient disponible)
  - Impossible d'annuler une réservation **déjà occupée** (client arrivé) : message explicatif, il faut d'abord supprimer l'occupation

#### Occupation (client réservé qui arrive)
- Sélection d'une réservation active **non encore occupée**, bouton « Enregistrer l'arrivée »
- **Crédite le solde** : prix de la nuitée × nombre de jours (le montant est **stocké** dans la table)
- **Transfert** possible : relier l'occupation à une autre réservation (solde ajusté automatiquement)
- **Suppression** : **rembourse** le montant stocké (solde diminué)
- **Reçu PDF** : génère un reçu de paiement du client

#### Séjour (walk-in, sans réservation)
- Date d'entrée = **aujourd'hui** automatiquement (aucun champ date)
- **Crédite le solde immédiatement** (montant stocké)
- Modification (jours/chambre) : **solde ajusté du delta**
- Suppression : **rembourse** le montant stocké
- **Reçu PDF** disponible

#### Recherche de chambres libres
- Saisie date (≥ aujourd'hui) + nombre de jours → liste des chambres libres sur la période
- Une chambre est libre si **aucune réservation active** (non annulée) **ni aucun séjour** ne chevauche la période

#### Solde
- Ligne unique en base (id = 1), affichée en haut de la fenêtre
- **Rectification manuelle** : bouton « Rectifier le solde » (montant positif ou négatif, ex : `-500`)
- Le solde peut devenir **négatif** (assumé)

### 2.3 Règle de disponibilité (chevauchement)

Une chambre est **indisponible** sur une période si une réservation **active** (`annulee = FALSE`) existe avec chevauchement :

```
reserver.date_entree < [fin de la nouvelle période]
ET (reserver.date_entree + reserver.nbr_jour jours) > [début de la nouvelle période]
```

- Une réservation **annulée** est ignorée → la chambre redevient réservable
- La modification d'une réservation exclut la réservation elle-même du contrôle (pas de faux conflit)
- La recherche de chambres libres étend le contrôle aux **séjours** (un walk-in occupe physiquement la chambre)

### 2.4 Règles du solde — matrice complète

| Opération | Effet sur le solde |
|---|---|
| Enregistrer l'arrivée d'un client réservé (OCCUPER) | **+** prix × jours (montant stocké) |
| Enregistrer un séjour (SEJOURNER) | **+** prix × jours (montant stocké) |
| Supprimer une occupation | **−** montant stocké (remboursement) |
| Supprimer un séjour | **−** montant stocké (remboursement) |
| Modifier un séjour (jours/chambre) | **± delta** entre nouveau montant et montant stocké |
| Modifier une réservation **déjà occupée** | **± delta** (mise à jour du montant de l'occupation) |
| Transférer une occupation vers une autre réservation | **± delta** |
| Annuler une réservation non occupée | Aucun (rien n'a été encaissé) |
| Annuler une réservation occupée | **Bloqué** par l'interface |
| Rectification manuelle | **±** montant saisi |

Toutes les opérations multi-tables sont **transactionnelles** (une seule connexion, commit/rollback) : l'écriture et la mise à jour du solde sont atomiques.

---

## 3. Aspects non fonctionnels

- **Langage** : Java 11 (source/target 11), code compatible Java 11 uniquement (pas de records, switch expressions, text blocks)
- **Interface** : 100 % française, look moderne via **FlatLaf** (`FlatLightLaf`)
- **Concurrence** : appels base de données **directement sur l'EDT** (choix volontaire, appli mono-utilisateur, pas de SwingWorker)
- **Gestion d'erreurs** : boîtes de dialogue `JOptionPane` françaises ; les erreurs de contrainte SQL (SQLState `23`) affichent un message générique (« enregistrement référencé par d'autres données »)
- **Robustesse mail** : l'échec d'envoi du mail n'empêche jamais la réservation (simple avertissement)
- **BD injoignable** : l'application démarre quand même ; le solde affiche « ? » et chaque accès BD ouvre un dialogue d'erreur
- **Tests** : aucun test automatisé — la validation se fait par **tests manuels** (voir §6)
- **Licences** : dépendances open source (MIT/Apache/LGPL) — librement utilisables pour un projet étudiant

---

## 4. Aspects techniques

### 4.1 Stack technique

| Brique | Choix | Version |
|---|---|---|
| Langage | Java (OpenJDK) | 11 |
| UI | Java Swing (`javax.swing`) + FlatLaf | FlatLaf 3.4.1 |
| Build | Maven | pom.xml (Maven 3.9+) |
| Base de données | PostgreSQL (serveur distant) | 14+ |
| Accès BD | JDBC pur (driver PostgreSQL) | postgresql 42.7.4 |
| Mail | Jakarta Mail (SMTP + TLS) | jakarta.mail 2.0.1 |
| Calendrier | LGoodDatePicker | 11.2.1 |
| PDF | OpenPDF (dérivé d'iText) | openpdf 1.3.30 |

### 4.2 Architecture — 3 couches

```
src/main/java/com/hotel/
    Main.java          -> point d'entrée (FlatLaf + ouverture de MainFrame)
    model/             -> POJO (1 classe par table)
    dao/               -> accès base de données JDBC (1 DAO par table)
    ui/                -> fenêtres et panneaux Swing
    util/              -> outils transverses (connexion, mail, PDF)
src/main/resources/
    db.properties      -> configuration connexion PostgreSQL
    mail.properties    -> configuration SMTP
```

Flux type : **UI** → **DAO** (JDBC) → **PostgreSQL distant**. Le `model` sert de transport de données entre les couches. Les DAO ouvrent leur connexion via `DBConnection.getConnection()` à chaque appel (pas de pool).

### 4.3 Les 19 classes

**Couche `model`** (POJO : attributs + getters/setters, plusieurs constructeurs)

| Classe | Table | Attributs principaux |
|---|---|---|
| `Chambre` | chambre | numChambre, design, type, prixNuitee |
| `Reserver` | reserver | idReserv, numChambre, dateReserv, dateEntree, nbrJour, nomClient, mail, annulee |
| `Occuper` | occuper | idOccup, idReserv, **montant** |
| `Sejourner` | sejourner | idSejour, numChambre, dateEntreeSejour, nbrJour, nomClient, telephone, **montant** |
| `Solde` | solde | id, soldeActuel |

**Couche `dao`** (toutes les méthodes publiques déclarent `throws SQLException` ; JDBC pur, `PreparedStatement` avec `?` uniquement)

| DAO | Méthodes |
|---|---|
| `ChambreDAO` | ajouter, lister, modifier, supprimer(String), trouverParNum, **listerChambresLibres(date, jours)** |
| `ReserverDAO` | ajouter (dateReserv = aujourd'hui, clé générée), lister, modifier (ne touche ni dateReserv ni annulee), supprimer, **annuler** (annulee = TRUE), **estChambreDisponible**, **estChambreDisponibleExcluant**, trouverParId, **listerActivesNonOccupees**, **isOccupee** |
| `OccuperDAO` | ajouter (transactionnel : vérif + INSERT + crédit solde), lister, **modifier(idOccup, idReserv)** (transfert + delta solde), **supprimer** (remboursement) |
| `SejournerDAO` | ajouter (transactionnel : date = aujourd'hui + crédit solde), lister, **modifier** (delta solde), **supprimer** (remboursement) |
| `SoldeDAO` | getSolde, ajouterAuSolde(int), **ajouterAuSolde(Connection, int)** (pour transactions) |

**Couche `ui`**

| Classe | Rôle |
|---|---|
| `MainFrame` | Fenêtre principale : solde en haut, 5 onglets, bouton « Rectifier le solde », rafraîchissement à chaque changement d'onglet |
| `ChambrePanel` | CRUD chambres |
| `ReserverPanel` | CRUD + annulation + contrôle disponibilité + envoi mail ; date via calendrier (min = aujourd'hui, dd-MM-yyyy) |
| `OccuperPanel` | Arrivée client réservé, transfert d'occupation, suppression (avec message de remboursement), reçu PDF |
| `SejournerPanel` | CRUD séjours, reçu PDF |
| `ChambreLibrePanel` | Recherche de chambres libres (calendrier + jours + tableau) |

**Couche `util`**

| Classe | Rôle |
|---|---|
| `DBConnection` | Charge `db.properties` et fournit `getConnection()` (une nouvelle connexion par appel) |
| `MailSender` | `envoyerMailReservation(destinataire, Reserver, Chambre)` via SMTP/TLS (Jakarta Mail) |
| `PdfRecuGenerator` | `genererRecu(File, numero, client, chambre, dateEntree, jours, montant)` — reçu PDF (OpenPDF) |

### 4.4 Base de données

**Base** : `hotel_db` — script de création : `schema.sql` (idempotent, relançable).

```
solde      (id SERIAL PK, solde_actuel INTEGER NOT NULL DEFAULT 0)      -- ligne unique id = 1
chambre    (num_chambre VARCHAR(20) PK, design, type, prix_nuitee INTEGER)
reserver   (id_reserv SERIAL PK, num_chambre FK->chambre, date_reserv DATE,
            date_entree DATE, nbr_jour INTEGER, nom_client, mail, annulee BOOLEAN DEFAULT FALSE)
occuper    (id_occup SERIAL PK, id_reserv INTEGER FK->reserver UNIQUE, montant INTEGER DEFAULT 0)
sejourner  (id_sejour SERIAL PK, num_chambre FK->chambre, date_entree_sejour DATE,
            nbr_jour INTEGER, nom_client, telephone, montant INTEGER DEFAULT 0)
```

Points clés :
- **`occuper.id_reserv UNIQUE`** : une réservation ne peut être occupée qu'une fois (protège du double encaissement)
- **Colonnes `montant`** (occuper, sejourner) : montant crédité au solde — indispensable aux remboursements/rectifications
- **`annulee`** : annulation « logique » des réservations (chambre libérée sans supprimer la ligne)
- **Intégrité référentielle** : supprimer une chambre référencée ou une réservation occupée est bloqué par la base (FK) → message d'erreur côté UI

> ⚠️ **Migration d'une base existante** (avant l'ajout du solde rectifiable) — à exécuter une fois :
> ```sql
> ALTER TABLE occuper ADD COLUMN IF NOT EXISTS montant INTEGER NOT NULL DEFAULT 0;
> ALTER TABLE sejourner ADD COLUMN IF NOT EXISTS montant INTEGER NOT NULL DEFAULT 0;
> ```
> Les lignes créées avant cette migration ont `montant = 0` : suppression/remboursement à 0 (pas de backfill possible).

### 4.5 Configuration

**`src/main/resources/db.properties`** — connexion à la base **distante** :
```properties
db.url=jdbc:postgresql://localhost:5432/hotel_db   # remplacer localhost par l'IP du serveur distant
db.user=postgres
db.password=postgres
```

**`src/main/resources/mail.properties`** — SMTP (Gmail par défaut) :
```properties
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.smtp.auth=true
mail.smtp.starttls.enable=true
mail.user=votreadresse@gmail.com
mail.password=motdepasse_application   # mot de passe d'APPLICATION Gmail (pas celui du compte)
```

### 4.6 Pièges et points d'attention

- **LGoodDatePicker** : `setDateRangeLimits(...)` doit être appelé **APRÈS** la construction du `DatePicker` (sinon `RuntimeException` — le settings n'est pas encore attaché). Même règle pour le panneau de recherche de chambres libres.
- **Connexions** : chaque appel DAO ouvre et ferme sa connexion (try-with-resources) — pas de pool.
- **Transactions** : les opérations « écriture + solde » passent par `SoldeDAO.ajouterAuSolde(Connection, int)` sur la même connexion.
- **`annulee`** : la modification d'une réservation ne touche jamais `date_reserv` ni `annulee`.
- **Suppression d'une occupation/séjour** : le solde est **remboursé** (delta négatif) — comportement voulu, à connaître.

---

## 5. Guide de prise en main

1. **Prérequis** : JDK 11+, Maven 3.9+, IntelliJ IDEA, accès réseau au serveur PostgreSQL
2. **Base de données** (sur le serveur distant) :
   - Créer la base : `CREATE DATABASE hotel_db;`
   - Appliquer `schema.sql` (et les ALTER de migration si la base est ancienne)
3. **Configurer** `db.properties` (IP du serveur distant) et `mail.properties` (SMTP)
4. **Ouvrir le projet dans IntelliJ** : le `pom.xml` doit être importé comme projet Maven (clic droit sur `pom.xml` → « Add as Maven Project », puis Reload) — c'est ce qui fournit les bibliothèques
5. **Lancer** : exécuter `com.hotel.Main` (classe avec `main`)

---

## 6. Checklist de validation (tests manuels)

- [ ] CRUD chambres (ajout / modification / suppression ; suppression d'une chambre référencée → erreur)
- [ ] Réservation → **mail de confirmation** reçu avec chambre, dates et nombre de jours
- [ ] Réserver la même chambre sur une période **chevauchante** → refusé
- [ ] Annuler une réservation → la chambre redevient **disponible** sur la période
- [ ] Annuler une réservation **occupée** → bloqué
- [ ] Arrivée d'un client réservé (Occupation) → solde **+prix × jours**, colonne Montant renseignée
- [ ] Supprimer l'occupation → solde **−montant**
- [ ] Séjour 3 j à 1000 → solde +3000 ; modification à 5 j → +2000 ; à 2 j → −1000 ; suppression → remboursement
- [ ] Modifier une réservation occupée (jours) → delta au solde
- [ ] Recherche « Chambres libres » : période occupée → chambre absente ; après annulation → présente
- [ ] Reçu PDF depuis Occupations et Séjours → fichier PDF correct (chemin choisi par l'utilisateur)
- [ ] Bouton « Rectifier le solde » (+/−) → solde ajusté
- [ ] Saisie de date : passé grisé/impossible ; format dd-MM-yyyy partout
- [ ] Solde affiché après chaque opération et à chaque changement d'onglet

---

## 7. Limites connues et évolutions possibles

**Limites assumées**
- Aucun test automatisé (validation manuelle uniquement)
- Pas de contrôle de disponibilité pour les séjours walk-in (occupation immédiate)
- Le solde peut devenir négatif
- L'historique antérieur à la migration `montant` n'est pas remboursable (montant = 0)
- Appels BD sur l'EDT : acceptable en mono-utilisateur, à revoir si multi-utilisateurs
- Le mail dépend d'un SMTP valide (mot de passe d'application) — l'envoi échoué n'est pas retenté

**Évolutions possibles**
- Export CSV des réservations (mentionné dans steps.md)
- Validation supplémentaire côté DAO (ceinture + bretelles)
- Pool de connexions, SwingWorker pour les gros chargements
- Historisation des mouvements de solde (table de transactions)
- Sécurité : chiffrement du mot de passe, comptes utilisateurs

---

## 8. Références

- `steps.md` — cahier des charges initial (stack, règles métier, ordre de développement)
- `schema.sql` — schéma de base de données (version à jour)
- Ce document — état final de l'implémentation (fonctionnel + non fonctionnel + technique)
