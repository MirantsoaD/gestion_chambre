package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import com.hotel.dao.ChambreDAO;
import com.hotel.model.Chambre;

/**
 * Panneau de recherche des chambres libres sur une période donnée.
 * La recherche est déclenchée par le bouton "Rechercher" (pas de chargement automatique).
 */
public class ChambreLibrePanel extends JPanel {

    private final MainFrame parent;
    private final ChambreDAO chambreDAO = new ChambreDAO();

    private final DatePicker datePicker = creerDatePicker();
    private final JTextField joursField = new JTextField(5);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Numéro", "Design", "Type", "Prix / nuitée"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ChambreLibrePanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        // NORTH : critères de recherche
        JPanel form = Style.panneauFormulaire("Critères de recherche");
        form.add(Style.libelle("Date d'entrée :"));
        form.add(datePicker);
        form.add(Style.libelle("Jours :"));
        form.add(joursField);
        JButton searchButton = new JButton("Rechercher");
        Style.boutonPrincipal(searchButton);
        searchButton.addActionListener(e -> rechercher());
        form.add(searchButton);
        add(form, BorderLayout.NORTH);

        // CENTER : tableau
        table.setRowHeight(30);
        table.getTableHeader().setFont(
                table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * No-op : on conserve le dernier résultat affiché.
     * La liste des chambres libres est rechargée uniquement via le bouton "Rechercher".
     */
    public void rafraichir() {
        // Rien à recharger automatiquement.
    }

    private DatePicker creerDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(Locale.FRENCH);
        settings.setFormatForDatesCommonEra(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        DatePicker datePicker = new DatePicker(settings);
        // Seules les dates à partir d'aujourd'hui sont autorisées.
        // Doit être défini APRÈS la construction du DatePicker
        // (sinon RuntimeException : settings non attaché).
        settings.setDateRangeLimits(LocalDate.now(), null);
        return datePicker;
    }

    private void rechercher() {
        LocalDate date = datePicker.getDate();
        if (date == null) {
            JOptionPane.showMessageDialog(this, "Veuillez choisir une date d'entrée.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int jours;
        try {
            jours = Integer.parseInt(joursField.getText().trim());
        } catch (NumberFormatException e) {
            jours = -1;
        }
        if (jours <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Le nombre de jours est invalide : un entier positif est attendu.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            List<Chambre> libres = chambreDAO.listerChambresLibres(date, jours);
            tableModel.setRowCount(0);
            for (Chambre c : libres) {
                tableModel.addRow(new Object[]{
                        c.getNumChambre(), c.getDesign(), c.getType(), c.getPrixNuitee()
                });
            }
            if (libres.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Aucune chambre libre sur cette période.",
                        "Aucune disponibilité", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
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
