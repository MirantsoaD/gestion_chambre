# DESIGN.md — Charte graphique "Hôtel de Prestige"

> Direction artistique pour l'application Java Swing de gestion des réservations.
> Objectif : que l'interface évoque un hôtel 5 étoiles — feutré, chaleureux, haut de gamme — malgré la simplicité technique du projet (Swing + FlatLaf).

---

## 1. Ambiance générale (mood)

Penser **hall d'hôtel de luxe le soir** : bois sombre, laiton doré, marbre crème, lumière tamisée. Pas de couleurs criardes, pas de bleu "logiciel de gestion" par défaut. On vise la sobriété élégante : beaucoup de fond neutre profond, peu de couleurs, mais utilisées avec précision (surtout l'or/laiton en accent).

Mots-clés : *feutré, discret, doré, intemporel, soigné* — à l'opposé de *flashy, ludique, saturé*.

---

## 2. Palette de couleurs

### 2.1 Couleurs principales

| Rôle | Nom | Hex | Usage |
|---|---|---|---|
| Fond principal | Noir Onyx | `#1C1A17` | Fond des fenêtres, panneaux principaux |
| Fond secondaire | Anthracite Chaud | `#2A2620` | Cartes, panneaux de formulaire, barres |
| Accent principal | Or Laiton | `#C9A24B` | Boutons principaux, titres, bordures actives, sélection |
| Accent secondaire | Or Pâle | `#E4C97A` | Survol (hover), icônes, highlights légers |
| Texte principal | Ivoire | `#F3EFE6` | Texte sur fond sombre |
| Texte secondaire | Beige Grisé | `#B9B2A3` | Labels, texte discret, placeholders |

### 2.2 Couleurs neutres / support

| Rôle | Nom | Hex | Usage |
|---|---|---|---|
| Fond clair (zones tables) | Crème Marbre | `#F5F1E8` | Fond des `JTable`, zones de lecture |
| Texte sur fond clair | Noir Café | `#241F1A` | Texte dans les tables |
| Bordure discrète | Gris Taupe | `#6E655A` | Séparateurs, bordures de champs |
| Ligne alternée (table) | Ivoire Cassé | `#EDE7D8` | Lignes paires des `JTable` |

### 2.3 Couleurs d'état (à utiliser avec parcimonie, jamais criardes)

| État | Nom | Hex | Usage |
|---|---|---|---|
| Succès | Vert Sauge | `#7C9473` | Confirmation (réservation OK, mail envoyé) |
| Erreur | Bordeaux | `#8C3B3B` | Chambre indisponible, erreur de saisie |
| Attention | Ambre Doux | `#C08A3E` | Avertissements (mail non envoyé, etc.) |
| Information | Bleu Ardoise | `#5C7A8A` | Infos neutres |

> Règle : les couleurs d'état restent **désaturées** (jamais de rouge/vert vif façon Bootstrap). Elles doivent rester dans le même univers "matière noble" que le reste.

---

## 3. Typographie

- **Titres / en-têtes** : une police serif élégante si disponible sur le système (`Georgia`, `Playfair Display` si installée, sinon `Serif` générique Java). Utilisée pour : titre de la fenêtre principale, titres d'onglets, nom de l'hôtel.
- **Corps de texte / formulaires / tables** : police sans-serif propre et lisible (`Segoe UI`, `Helvetica Neue`, ou `SansSerif` générique Java en fallback).
- **Tailles indicatives** :
  - Titre principal (nom de l'app/hôtel) : 22–26px, gras, couleur Or Laiton
  - Titres de section/onglet : 16–18px, semi-gras, Ivoire
  - Texte de formulaire / labels : 13–14px, Beige Grisé
  - Texte des tables : 13px, Noir Café
- Éviter les polices "système" par défaut trop neutres (Dialog.plain) quand une alternative plus soignée est disponible.

---

## 4. Application avec FlatLaf (Swing)

Le plus simple pour obtenir ce rendu en Swing est de partir de **FlatLaf** (thème sombre `FlatDarkLaf` ou custom) et de surcharger ses `UIManager` properties.

### 4.1 Dépendance Maven
```xml
<dependency>
    <groupId>com.formdev</groupId>
    <artifactId>flatlaf</artifactId>
    <version>3.4</version>
</dependency>
```

### 4.2 Initialisation + surcharge des couleurs (`Main.java`)

```java
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();

        // Couleurs de fond
        UIManager.put("Panel.background", new Color(0x1C1A17));
        UIManager.put("@background", "1C1A17");
        UIManager.put("@foreground", "F3EFE6");
        UIManager.put("@accentColor", "C9A24B");

        // Boutons
        UIManager.put("Button.background", new Color(0x2A2620));
        UIManager.put("Button.foreground", new Color(0xF3EFE6));
        UIManager.put("Button.default.background", new Color(0xC9A24B));
        UIManager.put("Button.default.foreground", new Color(0x1C1A17));
        UIManager.put("Button.arc", 6);

        // Champs de saisie
        UIManager.put("TextField.background", new Color(0x2A2620));
        UIManager.put("TextField.foreground", new Color(0xF3EFE6));
        UIManager.put("Component.focusColor", new Color(0xC9A24B));

        // Onglets
        UIManager.put("TabbedPane.selectedBackground", new Color(0x2A2620));
        UIManager.put("TabbedPane.underlineColor", new Color(0xC9A24B));
        UIManager.put("TabbedPane.foreground", new Color(0xF3EFE6));

        // Tables
        UIManager.put("Table.background", new Color(0xF5F1E8));
        UIManager.put("Table.foreground", new Color(0x241F1A));
        UIManager.put("Table.alternateRowColor", new Color(0xEDE7D8));
        UIManager.put("Table.selectionBackground", new Color(0xC9A24B));
        UIManager.put("Table.selectionForeground", new Color(0x1C1A17));
        UIManager.put("TableHeader.background", new Color(0x2A2620));
        UIManager.put("TableHeader.foreground", new Color(0xE4C97A));

        // Police
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("defaultFont", base);

        java.awt.EventQueue.invokeLater(() -> new ui.MainFrame().setVisible(true));
    }
}
```

> Si FlatLaf n'est pas souhaité (pour rester "swing pur"), reprendre manuellement chaque couleur via `setBackground()` / `setForeground()` sur chaque composant en suivant la palette §2.

---

## 5. Composants clés — recommandations précises

### 5.1 Fenêtre principale (`MainFrame`)
- Fond : Noir Onyx (`#1C1A17`).
- Titre de la fenêtre (barre du haut, `JLabel` custom) : "Hôtel [Nom] — Gestion des Réservations", police serif, Or Laiton, centré ou aligné à gauche avec un léger padding.
- Le label du **solde actuel** en haut à droite : encadré dans un petit panneau Anthracite avec bordure Or Laiton fine (1px), texte Ivoire, montant en Or Pâle et gras.

### 5.2 Onglets (`JTabbedPane`)
- Icônes discrètes si possible (🛏 Chambres, 📅 Réservations, 🔑 Occupations, 🧳 Séjours) — sinon simple texte en petites capitales.
- Onglet actif : soulignement Or Laiton, fond légèrement plus clair que les autres.

### 5.3 Boutons
- Boutons d'action principale ("Ajouter", "Réserver", "Confirmer") : fond Or Laiton, texte Noir Onyx, coins légèrement arrondis (rayon ~6px).
- Boutons secondaires ("Modifier", "Rafraîchir") : fond Anthracite, texte Ivoire, bordure fine Gris Taupe.
- Bouton destructif ("Supprimer", "Annuler la réservation") : fond Bordeaux discret, texte Ivoire, à utiliser seul (pas à côté d'un autre bouton rouge) pour éviter la confusion.
- Effet hover : léger éclaircissement (+10% luminosité) vers Or Pâle pour les boutons principaux.

### 5.4 Tables (`JTable`)
- Fond clair Crème Marbre pour contraster avec le reste sombre de l'appli (effet "feuille posée sur un bureau en bois").
- En-têtes de colonnes : fond Anthracite, texte Or Pâle, petites majuscules si possible.
- Lignes alternées : Crème Marbre / Ivoire Cassé.
- Ligne sélectionnée : fond Or Laiton, texte Noir Onyx.
- Hauteur de ligne un peu plus généreuse que le défaut Swing (28–32px) pour un rendu plus "aéré/premium".

### 5.5 Formulaires
- Labels en Beige Grisé, alignés à gauche, taille légèrement inférieure au champ.
- Champs de saisie (`JTextField`, `JComboBox`) : fond Anthracite, texte Ivoire, bordure fine Gris Taupe, bordure Or Laiton au focus.
- Grouper les champs dans un `JPanel` avec un léger `TitledBorder` couleur Or Laiton (ex : "Informations réservation").

### 5.6 Messages / dialogues (`JOptionPane`)
- Icônes et couleurs custom cohérentes avec §2.3 (succès = vert sauge, erreur = bordeaux, info = bleu ardoise).
- Titre de la boîte de dialogue toujours en cohérence avec le vocabulaire hôtelier ("Réservation confirmée", "Chambre indisponible", plutôt que "Erreur" / "Succès" génériques).

### 5.7 Espacement / layout général
- Padding généreux autour des panneaux (16–20px minimum) — jamais de composants collés aux bords.
- Utiliser `GridBagLayout` ou `MigLayout` (si dépendance ajoutée) plutôt que `FlowLayout` pour un alignement propre des formulaires.
- Séparer visuellement les sections (table / formulaire) par un espace ou une fine ligne Gris Taupe plutôt qu'une bordure épaisse.

---

## 6. Ton du texte dans l'interface

- Vocabulaire soigné et hôtelier plutôt que technique :
  - "Confirmer la réservation" plutôt que "Submit"
  - "Chambre indisponible sur cette période" plutôt que "Erreur SQL" ou "Conflit de dates"
  - "Le client a été enregistré à l'arrivée" plutôt que "Occupation ajoutée"
- Les mails automatiques (cf steps.md §9) doivent suivre le même ton : formule de politesse, formulation soignée ("Nous avons le plaisir de confirmer votre réservation...").

---

## 7. À éviter absolument

- Couleurs Swing par défaut (gris `#ECECEC`, bleu système `#3399FF`) — toujours surcharger.
- Icônes "cliparts" génériques ou emoji excessifs.
- Polices monospacées ou trop techniques (`Courier New`, `Consolas`) hors zones de debug.
- Boutons multiples de même couleur vive côte à côte (perte de hiérarchie visuelle).
- Fond blanc pur (`#FFFFFF`) — toujours préférer les tons crème/ivoire pour rester dans l'ambiance "matière noble".

---

## 8. Résumé rapide (cheat sheet couleurs)

```
Fond principal      #1C1A17
Fond secondaire     #2A2620
Or Laiton (accent)  #C9A24B
Or Pâle (hover)     #E4C97A
Texte clair         #F3EFE6
Texte secondaire    #B9B2A3
Fond table          #F5F1E8
Texte table         #241F1A
Bordure             #6E655A
Succès              #7C9473
Erreur              #8C3B3B
Attention           #C08A3E
Info                #5C7A8A
```
