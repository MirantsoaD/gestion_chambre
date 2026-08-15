package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.hotel.dao.ChambreDAO;
import com.hotel.dao.SejournerDAO;
import com.hotel.model.Chambre;
import com.hotel.model.Sejourner;
import com.hotel.util.PdfRecuGenerator;

/**
 * Panneau des séjours directs (client sans réservation) : incrémente le solde.
 * La date d'entrée est fixée à la date du jour par le DAO.
 */
public class SejournerPanel extends JPanel {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final MainFrame parent;
    private final SejournerDAO sejournerDAO = new SejournerDAO();
    private final ChambreDAO chambreDAO = new ChambreDAO();

    private final JComboBox<Chambre> comboChambres = new JComboBox<>();
    private final JTextField joursField = new JTextField(5);
    private final JTextField clientField = new JTextField(12);
    private final JTextField telephoneField = new JTextField(12);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Chambre", "Date entrée", "Jours", "Client", "Téléphone", "Montant"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public SejournerPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        // NORTH : formulaire
        JPanel form = Style.panneauFormulaire("Informations séjour");
        form.add(Style.libelle("Chambre :"));
        form.add(comboChambres);
        form.add(Style.libelle("Jours :"));
        form.add(joursField);
        form.add(Style.libelle("Client :"));
        form.add(clientField);
        form.add(Style.libelle("Téléphone :"));
        form.add(telephoneField);
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
        JButton pdfButton = new JButton("Reçu PDF");
        Style.boutonPrincipal(addButton);
        Style.boutonSecondaire(modButton);
        Style.boutonDestructif(delButton);
        Style.boutonSecondaire(refreshButton);
        Style.boutonSecondaire(pdfButton);
        addButton.addActionListener(e -> ajouter());
        modButton.addActionListener(e -> modifier());
        delButton.addActionListener(e -> supprimer());
        refreshButton.addActionListener(e -> chargerTable());
        pdfButton.addActionListener(e -> genererRecu());
        buttons.add(addButton);
        buttons.add(modButton);
        buttons.add(delButton);
        buttons.add(refreshButton);
        buttons.add(pdfButton);
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
            List<Sejourner> sejours = sejournerDAO.lister();
            tableModel.setRowCount(0);
            for (Sejourner s : sejours) {
                tableModel.addRow(new Object[]{
                        s.getIdSejour(),
                        s.getNumChambre(),
                        formaterDate(s.getDateEntreeSejour()),
                        s.getNbrJour(),
                        s.getNomClient(),
                        s.getTelephone(),
                        s.getMontant()
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
        selectionnerChambreCombo(valeur(row, 1));
        joursField.setText(valeur(row, 3));
        clientField.setText(valeur(row, 4));
        telephoneField.setText(valeur(row, 5));
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
        joursField.setText("");
        clientField.setText("");
        telephoneField.setText("");
        table.clearSelection();
    }

    private int lireJours() {
        try {
            return Integer.parseInt(joursField.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean validerSaisie() {
        if (comboChambres.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Veuillez choisir une chambre.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (lireJours() <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Le nombre de jours est invalide : un entier positif est attendu.",
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
        try {
            Sejourner s = new Sejourner(0, c.getNumChambre(), null, lireJours(),
                    clientField.getText().trim(), telephoneField.getText().trim());
            sejournerDAO.ajouter(s);
            JOptionPane.showMessageDialog(this,
                    "Le client a été enregistré. Le solde a été mis à jour.",
                    "Séjour enregistré", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void modifier() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un séjour à modifier.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validerSaisie()) {
            return;
        }
        Chambre c = (Chambre) comboChambres.getSelectedItem();
        try {
            int id = Integer.parseInt(valeur(row, 0));
            LocalDate dateEntree = LocalDate.parse(valeur(row, 2), FMT_DATE);
            Sejourner s = new Sejourner(id, c.getNumChambre(), dateEntree, lireJours(),
                    clientField.getText().trim(), telephoneField.getText().trim());
            sejournerDAO.modifier(s);
            JOptionPane.showMessageDialog(this, "Le séjour a été modifié.",
                    "Séjour modifié", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void supprimer() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un séjour à supprimer.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int id = Integer.parseInt(valeur(row, 0));
        String montant = String.valueOf(tableModel.getValueAt(row, 6));
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment supprimer ce séjour ?\nLe solde sera diminué de "
                        + montant + ".",
                "Confirmation de suppression", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            sejournerDAO.supprimer(id);
            JOptionPane.showMessageDialog(this, "Le séjour a été retiré du registre.",
                    "Séjour supprimé", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    /** Génère un reçu PDF pour le séjour sélectionné. */
    private void genererRecu() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un séjour pour générer le reçu.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String numero = "SEJ-" + valeur(row, 0);
        String client = valeur(row, 4);
        String chambre = valeur(row, 1);
        String dateEntree = valeur(row, 2);
        int jours = Integer.parseInt(valeur(row, 3));
        int montant = Integer.parseInt(valeur(row, 6));

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("recu-sejour-" + valeur(row, 0) + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File fichier = chooser.getSelectedFile();
            PdfRecuGenerator.genererRecu(fichier, numero, client, chambre, dateEntree, jours, montant);
            JOptionPane.showMessageDialog(this, "Le reçu a été généré : " + fichier.getAbsolutePath(),
                    "Reçu généré", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Une erreur est survenue lors de la génération du PDF : " + e.getMessage(),
                    "Reçu PDF", JOptionPane.ERROR_MESSAGE);
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

    private static String formaterDate(LocalDate d) {
        return d == null ? "" : FMT_DATE.format(d);
    }
}
