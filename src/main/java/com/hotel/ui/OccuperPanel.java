package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.hotel.dao.OccuperDAO;
import com.hotel.dao.ReserverDAO;
import com.hotel.model.Occuper;
import com.hotel.model.Reserver;
import com.hotel.util.PdfRecuGenerator;

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
                    "Date entrée", "Jours", "Montant"}, 0) {
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
        JPanel form = Style.panneauFormulaire("Arrivée d'un client");
        form.add(Style.libelle("Réservation :"));
        form.add(comboReservations);
        JButton arriveeButton = new JButton("Enregistrer l'arrivée");
        Style.boutonPrincipal(arriveeButton);
        arriveeButton.addActionListener(e -> enregistrerArrivee());
        form.add(arriveeButton);
        add(form, BorderLayout.NORTH);

        // CENTER : tableau
        table.setRowHeight(30);
        table.getTableHeader().setFont(
                table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // SOUTH : boutons hiérarchisés
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        JButton delButton = new JButton("Supprimer");
        JButton modButton = new JButton("Modifier");
        JButton refreshButton = new JButton("Rafraîchir");
        JButton pdfButton = new JButton("Reçu PDF");
        Style.boutonDestructif(delButton);
        Style.boutonSecondaire(modButton);
        Style.boutonSecondaire(refreshButton);
        Style.boutonSecondaire(pdfButton);
        delButton.addActionListener(e -> supprimer());
        modButton.addActionListener(e -> modifier());
        refreshButton.addActionListener(e -> chargerTable());
        pdfButton.addActionListener(e -> genererRecu());
        buttons.add(delButton);
        buttons.add(modButton);
        buttons.add(refreshButton);
        buttons.add(pdfButton);
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
                            r.getNbrJour(),
                            o.getMontant()
                    });
                } else {
                    tableModel.addRow(new Object[]{o.getIdOccup(), "—", "—", "—", "—", "—", "—"});
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
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner une réservation à enregistrer.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Reserver r = listComboReservations.get(index);
        try {
            occuperDAO.ajouter(new Occuper(0, r.getIdReserv()));
            JOptionPane.showMessageDialog(this,
                    "Le client a été enregistré à l'arrivée. Le solde a été mis à jour.",
                    "Arrivée enregistrée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    private void supprimer() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une occupation à retirer.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int idOccup = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 0)));
        String montant = String.valueOf(tableModel.getValueAt(row, 6));
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment retirer cette occupation ?\nLe solde sera diminué de "
                        + montant + ".",
                "Confirmation de suppression", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            occuperDAO.supprimer(idOccup);
            JOptionPane.showMessageDialog(this, "L'occupation a été retirée du registre.",
                    "Occupation supprimée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        }
    }

    /** Rattache l'occupation sélectionnée à une autre réservation (ajuste le solde). */
    private void modifier() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une occupation à modifier.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int idOccup = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 0)));
        int index = comboReservations.getSelectedIndex();
        if (index < 0 || listComboReservations.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Aucune réservation n'est disponible pour le transfert.",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Reserver cible = listComboReservations.get(index);
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment rattacher cette occupation à la réservation #"
                        + cible.getIdReserv() + " ?\nLe solde sera ajusté.",
                "Confirmation de transfert", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            occuperDAO.modifier(idOccup, cible.getIdReserv());
            JOptionPane.showMessageDialog(this,
                    "L'occupation a été transférée. Le solde a été ajusté.",
                    "Occupation transférée", JOptionPane.INFORMATION_MESSAGE);
            apresEcriture();
        } catch (SQLException e) {
            afficherErreurBD(e);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Génère un reçu PDF pour l'occupation sélectionnée. */
    private void genererRecu() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner une occupation pour générer le reçu.",
                    "Sélection requise", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String numero = "OCC-" + String.valueOf(tableModel.getValueAt(row, 0));
        String client = String.valueOf(tableModel.getValueAt(row, 3));
        String chambre = String.valueOf(tableModel.getValueAt(row, 2));
        String dateEntree = String.valueOf(tableModel.getValueAt(row, 4));
        int jours = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 5)));
        int montant = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 6)));

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new File("recu-" + tableModel.getValueAt(row, 0) + ".pdf"));
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
