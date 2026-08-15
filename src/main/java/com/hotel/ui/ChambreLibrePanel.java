package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
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

        // NORTH : formulaire
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Date d'entrée :"));
        form.add(datePicker);
        form.add(new JLabel("Jours :"));
        form.add(joursField);
        JButton searchButton = new JButton("Rechercher");
        searchButton.addActionListener(e -> rechercher());
        form.add(searchButton);
        add(form, BorderLayout.NORTH);

        // CENTER : tableau
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
            JOptionPane.showMessageDialog(this, "Choisissez une date d'entrée.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
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
                    "Nombre de jours invalide : entier > 0 attendu.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
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
                        "Info", JOptionPane.INFORMATION_MESSAGE);
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
            message = "Erreur base de données : " + e.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}
