package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.hotel.dao.ChambreDAO;
import com.hotel.model.Chambre;

/**
 * Panneau de gestion des chambres : liste + formulaire + CRUD.
 */
public class ChambrePanel extends JPanel {

    private final MainFrame parent;
    private final ChambreDAO chambreDAO = new ChambreDAO();

    private final JTextField numField = new JTextField(15);
    private final JTextField designField = new JTextField(15);
    private final JTextField typeField = new JTextField(15);
    private final JTextField prixField = new JTextField(10);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Numéro", "Design", "Type", "Prix / nuitée"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ChambrePanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        // NORTH : formulaire
        JPanel form = Style.panneauFormulaire("Informations chambre");
        form.add(Style.libelle("Numéro :"));
        form.add(numField);
        form.add(Style.libelle("Design :"));
        form.add(designField);
        form.add(Style.libelle("Type :"));
        form.add(typeField);
        form.add(Style.libelle("Prix / nuitée :"));
        form.add(prixField);
        add(form, BorderLayout.NORTH);

        // CENTER : tableau
        table.setRowHeight(30);
        table.getTableHeader().setFont(
                table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // SOUTH : boutons hiérarchisés
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        JButton addButton = new JButton("Ajouter");
        JButton modButton = new JButton("Modifier");
        JButton delButton = new JButton("Supprimer");
        JButton refreshButton = new JButton("Rafraîchir");
        Style.boutonPrincipal(addButton);
        Style.boutonSecondaire(modButton);
        Style.boutonDestructif(delButton);
        Style.boutonSecondaire(refreshButton);
        addButton.addActionListener(e -> ajouter());
        modButton.addActionListener(e -> modifier());
        delButton.addActionListener(e -> supprimer());
        refreshButton.addActionListener(e -> chargerTable());
        buttons.add(addButton);
        buttons.add(modButton);
        buttons.add(delButton);
        buttons.add(refreshButton);
        add(buttons, BorderLayout.SOUTH);

        // Sélection d'une ligne -> remplissage du formulaire
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                remplirFormulaire();
            }
        });

        chargerTable();
    }

    /** Recharge la liste des chambres. */
    public void rafraichir() {
        chargerTable();
    }

    private void chargerTable() {
        try {
            List<Chambre> chambres = chambreDAO.lister();
            tableModel.setRowCount(0);
            for (Chambre c : chambres) {
                tableModel.addRow(new Object[]{
                        c.getNumChambre(), c.getDesign(), c.getType(), c.getPrixNuitee()
                });
            }
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void remplirFormulaire() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        numField.setText(valeur(row, 0));
        designField.setText(valeur(row, 1));
        typeField.setText(valeur(row, 2));
        prixField.setText(valeur(row, 3));
    }

    private String valeur(int row, int col) {
        Object v = tableModel.getValueAt(row, col);
        return v == null ? "" : String.valueOf(v);
    }

    private void viderFormulaire() {
        numField.setText("");
        designField.setText("");
        typeField.setText("");
        prixField.setText("");
        table.clearSelection();
    }

    private int lirePrix() {
        try {
            return Integer.parseInt(prixField.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean validerFormulaire() {
        if (numField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le numéro de chambre est obligatoire.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (lirePrix() <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Le prix est invalide : un entier positif est attendu.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void ajouter() {
        if (!validerFormulaire()) {
            return;
        }
        try {
            chambreDAO.ajouter(new Chambre(
                    numField.getText().trim(),
                    designField.getText().trim(),
                    typeField.getText().trim(),
                    lirePrix()));
            JOptionPane.showMessageDialog(this, "La chambre a bien été ajoutée à l'inventaire.",
                    "Chambre ajoutée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void modifier() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une chambre à modifier.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validerFormulaire()) {
            return;
        }
        try {
            chambreDAO.modifier(new Chambre(
                    numField.getText().trim(),
                    designField.getText().trim(),
                    typeField.getText().trim(),
                    lirePrix()));
            JOptionPane.showMessageDialog(this, "La chambre a bien été modifiée.",
                    "Chambre modifiée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void supprimer() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une chambre à supprimer.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String num = valeur(row, 0);
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment supprimer la chambre " + num + " ?",
                "Confirmation de suppression",
                JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            chambreDAO.supprimer(num);
            JOptionPane.showMessageDialog(this, "La chambre a été retirée de l'inventaire.",
                    "Chambre supprimée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    /** Séquençage standard après une écriture réussie. */
    private void apresEcriture() {
        chargerTable();
        viderFormulaire();
        parent.rafraichirSolde();
    }

    private void afficherErreurBD(SQLException e) {
        String message;
        if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
            message = "Opération impossible : cet enregistrement est référencé par d'autres données.";
        } else {
            message = "Erreur d'accès à la base de données : " + e.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Base de données",
                JOptionPane.ERROR_MESSAGE);
    }
}
