package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import com.hotel.dao.ChambreDAO;
import com.hotel.dao.ReserverDAO;
import com.hotel.model.Chambre;
import com.hotel.model.Reserver;
import com.hotel.util.MailSender;

/**
 * Panneau de gestion des réservations : liste + formulaire + CRUD + annulation.
 */
public class ReserverPanel extends JPanel {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final MainFrame parent;
    private final ReserverDAO reserverDAO = new ReserverDAO();
    private final ChambreDAO chambreDAO = new ChambreDAO();

    private final JComboBox<Chambre> comboChambres = new JComboBox<>();
    private final DatePicker datePicker = creerDatePicker();
    private final JTextField joursField = new JTextField(5);
    private final JTextField clientField = new JTextField(12);
    private final JTextField mailField = new JTextField(15);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Chambre", "Date réservation", "Date entrée",
                    "Jours", "Client", "Mail", "Annulée"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ReserverPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        // NORTH : formulaire
        JPanel form = Style.panneauFormulaire("Informations réservation");
        form.add(Style.libelle("Chambre :"));
        form.add(comboChambres);
        form.add(Style.libelle("Date entrée :"));
        form.add(datePicker);
        form.add(Style.libelle("Jours :"));
        form.add(joursField);
        form.add(Style.libelle("Client :"));
        form.add(clientField);
        form.add(Style.libelle("Mail :"));
        form.add(mailField);
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
        JButton cancelButton = new JButton("Annuler la réservation");
        JButton delButton = new JButton("Supprimer");
        JButton refreshButton = new JButton("Rafraîchir");
        Style.boutonPrincipal(addButton);
        Style.boutonSecondaire(modButton);
        Style.boutonDestructif(cancelButton);
        Style.boutonDestructif(delButton);
        Style.boutonSecondaire(refreshButton);
        addButton.addActionListener(e -> ajouter());
        modButton.addActionListener(e -> modifier());
        cancelButton.addActionListener(e -> annuler());
        delButton.addActionListener(e -> supprimer());
        refreshButton.addActionListener(e -> chargerTable());
        buttons.add(addButton);
        buttons.add(modButton);
        buttons.add(cancelButton);
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

    /** Recharge le tableau et le combo des chambres. */
    public void rafraichir() {
        chargerTable();
    }

    private void chargerComboChambres() {
        try {
            List<Chambre> chambres = chambreDAO.lister();
            comboChambres.removeAllItems();
            for (Chambre c : chambres) {
                comboChambres.addItem(c);
            }
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void chargerTable() {
        try {
            List<Reserver> reservations = reserverDAO.lister();
            tableModel.setRowCount(0);
            for (Reserver r : reservations) {
                tableModel.addRow(new Object[]{
                        r.getIdReserv(),
                        r.getNumChambre(),
                        formaterDate(r.getDateReserv()),
                        formaterDate(r.getDateEntree()),
                        r.getNbrJour(),
                        r.getNomClient(),
                        r.getMail(),
                        r.isAnnulee() ? "Oui" : "Non"
                });
            }
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
        chargerComboChambres();
    }

    private void remplirFormulaire() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int id = Integer.parseInt(valeur(row, 0));
        try {
            Reserver r = reserverDAO.trouverParId(id);
            if (r == null) {
                return;
            }
            selectionnerChambreCombo(r.getNumChambre());
            datePicker.setDate(r.getDateEntree());
            joursField.setText(String.valueOf(r.getNbrJour()));
            clientField.setText(r.getNomClient() == null ? "" : r.getNomClient());
            mailField.setText(r.getMail() == null ? "" : r.getMail());
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void selectionnerChambreCombo(String numChambre) {
        for (int i = 0; i < comboChambres.getItemCount(); i++) {
            Chambre c = comboChambres.getItemAt(i);
            if (c != null && c.getNumChambre().equals(numChambre)) {
                comboChambres.setSelectedIndex(i);
                return;
            }
        }
    }

    private String valeur(int row, int col) {
        Object v = tableModel.getValueAt(row, col);
        return v == null ? "" : String.valueOf(v);
    }

    private void viderFormulaire() {
        datePicker.setDate(null);
        joursField.setText("");
        clientField.setText("");
        mailField.setText("");
        table.clearSelection();
    }

    private LocalDate lireDate() {
        return datePicker.getDate();
    }

    private int lireJours() {
        try {
            return Integer.parseInt(joursField.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Validations communes à l'ajout et à la modification. */
    private boolean validerSaisie() {
        if (comboChambres.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Veuillez choisir une chambre.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (lireDate() == null) {
            JOptionPane.showMessageDialog(this, "Veuillez choisir une date d'entrée.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (lireJours() <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Le nombre de jours est invalide : un entier positif est attendu.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String mail = mailField.getText().trim();
        if (!mail.isEmpty() && !mail.contains("@")) {
            JOptionPane.showMessageDialog(this, "L'adresse mail est invalide.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void ajouter() {
        if (!validerSaisie()) {
            return;
        }
        Chambre c = (Chambre) comboChambres.getSelectedItem();
        LocalDate dateEntree = lireDate();
        int jours = lireJours();
        String mail = mailField.getText().trim();
        try {
            if (!reserverDAO.estChambreDisponible(c.getNumChambre(), dateEntree, jours)) {
                JOptionPane.showMessageDialog(this, "Chambre indisponible sur cette période.",
                        "Chambre indisponible", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Reserver r = new Reserver(0, c.getNumChambre(), null, dateEntree,
                    jours, clientField.getText().trim(), mail, false);
            reserverDAO.ajouter(r);
            JOptionPane.showMessageDialog(this,
                    "Réservation confirmée (date de réservation : " + r.getDateReserv() + ").\n"
                    + "Nous avons le plaisir de confirmer votre arrivée.",
                    "Réservation confirmée", JOptionPane.INFORMATION_MESSAGE);

            if (!mail.isEmpty()) {
                try {
                    MailSender.envoyerMailReservation(mail, r, c);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "La réservation est confirmée, mais l'envoi du mail de confirmation a échoué : "
                            + ex.getMessage(),
                            "Mail non envoyé", JOptionPane.WARNING_MESSAGE);
                }
            }
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void modifier() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une réservation à modifier.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validerSaisie()) {
            return;
        }
        Chambre c = (Chambre) comboChambres.getSelectedItem();
        LocalDate dateEntree = lireDate();
        int jours = lireJours();
        String mail = mailField.getText().trim();
        try {
            int id = Integer.parseInt(valeur(row, 0));
            Reserver existante = reserverDAO.trouverParId(id);
            if (existante == null) {
                JOptionPane.showMessageDialog(this, "La réservation sélectionnée est introuvable.",
                        "Réservation introuvable", JOptionPane.ERROR_MESSAGE);
                return;
            }
            LocalDate dateReserv = existante.getDateReserv();
            boolean annulee = existante.isAnnulee();

            if (!reserverDAO.estChambreDisponibleExcluant(c.getNumChambre(), dateEntree, jours, id)) {
                JOptionPane.showMessageDialog(this, "Chambre indisponible sur cette période.",
                        "Chambre indisponible", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Reserver r = new Reserver(id, c.getNumChambre(), dateReserv, dateEntree,
                    jours, clientField.getText().trim(), mail, annulee);
            reserverDAO.modifier(r);
            JOptionPane.showMessageDialog(this, "La réservation a été modifiée.",
                    "Réservation modifiée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void annuler() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une réservation à annuler.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ("Oui".equals(valeur(row, 7))) {
            JOptionPane.showMessageDialog(this, "Cette réservation a déjà été annulée.",
                    "Réservation déjà annulée", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = Integer.parseInt(valeur(row, 0));
        try {
            if (reserverDAO.isOccupee(id)) {
                JOptionPane.showMessageDialog(this,
                        "Cette réservation est déjà occupée : le client est arrivé.\n"
                        + "Veuillez d'abord supprimer l'occupation dans l'onglet Occupations.",
                        "Client déjà arrivé", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int choix = JOptionPane.showConfirmDialog(this,
                    "Voulez-vous vraiment annuler la réservation #" + id + " ?",
                    "Confirmation d'annulation",
                    JOptionPane.YES_NO_OPTION);
            if (choix != JOptionPane.YES_OPTION) {
                return;
            }
            reserverDAO.annuler(id);
            JOptionPane.showMessageDialog(this, "La réservation a été annulée.",
                    "Réservation annulée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void supprimer() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une réservation à supprimer.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int id = Integer.parseInt(valeur(row, 0));
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment supprimer la réservation #" + id + " ?",
                "Confirmation de suppression",
                JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            reserverDAO.supprimer(id);
            JOptionPane.showMessageDialog(this, "La réservation a été supprimée.",
                    "Réservation supprimée", JOptionPane.INFORMATION_MESSAGE);
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

    private static DatePicker creerDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(Locale.FRENCH);
        settings.setFormatForDatesCommonEra(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        DatePicker datePicker = new DatePicker(settings);
        // Date minimale = aujourd'hui : seules les dates du présent et du futur
        // sont autorisées (doit être défini APRÈS la construction du DatePicker).
        settings.setDateRangeLimits(LocalDate.now(), null);
        return datePicker;
    }

    private static String formaterDate(LocalDate d) {
        return d == null ? "" : FMT_DATE.format(d);
    }
}
