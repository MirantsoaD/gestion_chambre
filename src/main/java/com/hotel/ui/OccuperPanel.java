package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.hotel.dao.OccuperDAO;
import com.hotel.dao.ReserverDAO;
import com.hotel.model.Occuper;
import com.hotel.model.Reserver;

/**
 * Panneau des occupations : arrivée d'un client ayant réservé (incrémente le solde).
 */
public class OccuperPanel extends JPanel {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final MainFrame parent;
    private final OccuperDAO occuperDAO = new OccuperDAO();
    private final ReserverDAO reserverDAO = new ReserverDAO();

    private final JComboBox<String> comboReservations = new JComboBox<>();
    private final List<Reserver> listComboReservations = new ArrayList<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID occupation", "Réservation", "Chambre", "Client",
                    "Date entrée", "Jours"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public OccuperPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        // NORTH : sélection de la réservation + bouton d'arrivée
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Réservation :"));
        form.add(comboReservations);
        JButton arriveeButton = new JButton("Enregistrer l'arrivée");
        arriveeButton.addActionListener(e -> enregistrerArrivee());
        form.add(arriveeButton);
        add(form, BorderLayout.NORTH);

        // CENTER : tableau
        add(new JScrollPane(table), BorderLayout.CENTER);

        // SOUTH : boutons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton delButton = new JButton("Supprimer");
        JButton refreshButton = new JButton("Rafraîchir");
        delButton.addActionListener(e -> supprimer());
        refreshButton.addActionListener(e -> chargerTable());
        buttons.add(delButton);
        buttons.add(refreshButton);
        add(buttons, BorderLayout.SOUTH);

        chargerTable();
    }

    /** Recharge le tableau et le combo des réservations actives non occupées. */
    public void rafraichir() {
        chargerTable();
    }

    private void chargerComboReservations() {
        try {
            List<Reserver> actives = reserverDAO.listerActivesNonOccupees();
            listComboReservations.clear();
            comboReservations.removeAllItems();
            for (Reserver r : actives) {
                listComboReservations.add(r);
                comboReservations.addItem(r.toString());
            }
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void chargerTable() {
        try {
            List<Occuper> occupations = occuperDAO.lister();
            tableModel.setRowCount(0);
            for (Occuper o : occupations) {
                Reserver r = reserverDAO.trouverParId(o.getIdReserv());
                if (r != null) {
                    tableModel.addRow(new Object[]{
                            o.getIdOccup(),
                            "#" + r.getIdReserv(),
                            r.getNumChambre(),
                            r.getNomClient(),
                            formaterDate(r.getDateEntree()),
                            r.getNbrJour()
                    });
                } else {
                    tableModel.addRow(new Object[]{o.getIdOccup(), "—", "—", "—", "—", "—"});
                }
            }
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
        chargerComboReservations();
    }

    private void viderFormulaire() {
        table.clearSelection();
    }

    private void enregistrerArrivee() {
        int index = comboReservations.getSelectedIndex();
        if (index < 0 || listComboReservations.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune réservation sélectionnée.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Reserver r = listComboReservations.get(index);
        try {
            occuperDAO.ajouter(new Occuper(0, r.getIdReserv()));
            JOptionPane.showMessageDialog(this,
                    "Arrivée enregistrée. Le solde a été mis à jour.",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void supprimer() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une occupation à supprimer.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int idOccup = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 0)));
        int choix = JOptionPane.showConfirmDialog(this, "Supprimer cette occupation ?",
                "Confirmation", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            occuperDAO.supprimer(idOccup);
            JOptionPane.showMessageDialog(this, "Occupation supprimée.",
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

    private static String formaterDate(LocalDate d) {
        return d == null ? "" : FMT_DATE.format(d);
    }
}
