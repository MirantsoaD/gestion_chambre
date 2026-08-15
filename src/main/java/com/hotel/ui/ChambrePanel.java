package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
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
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Numéro :"));
        form.add(numField);
        form.add(new JLabel("Design :"));
        form.add(designField);
        form.add(new JLabel("Type :"));
        form.add(typeField);
        form.add(new JLabel("Prix / nuitée :"));
        form.add(prixField);
        add(form, BorderLayout.NORTH);

        // CENTER : tableau
        add(new JScrollPane(table), BorderLayout.CENTER);

        // SOUTH : boutons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addButton = new JButton("Ajouter");
        JButton modButton = new JButton("Modifier");
        JButton delButton = new JButton("Supprimer");
        JButton refreshButton = new JButton("Rafraîchir");
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
            JOptionPane.showMessageDialog(this, "Numéro de chambre obligatoire.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (lirePrix() <= 0) {
            JOptionPane.showMessageDialog(this, "Prix invalide : entier > 0 attendu.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Chambre ajoutée.",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void modifier() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une chambre à modifier.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Chambre modifiée.",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void supprimer() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une chambre à supprimer.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String num = valeur(row, 0);
        int choix = JOptionPane.showConfirmDialog(this,
                "Supprimer la chambre " + num + " ?", "Confirmation",
                JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            chambreDAO.supprimer(num);
            JOptionPane.showMessageDialog(this, "Chambre supprimée.",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
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
            message = "Erreur base de données : " + e.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}
