package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.hotel.dao.SoldeDAO;

/**
 * Fenêtre principale : onglets Chambres, Réservations, Occupations et Séjours,
 * avec en-tête « Hôtel de Prestige » et solde actuel encadré en haut à droite.
 */
public class MainFrame extends JFrame {

    private final JLabel soldeLabel = new JLabel("—");
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final ChambrePanel chambrePanel;
    private final ReserverPanel reserverPanel;
    private final OccuperPanel occuperPanel;
    private final SejournerPanel sejournerPanel;
    private final ChambreLibrePanel chambreLibrePanel;

    public MainFrame() {
        super("Hôtel de Prestige — Gestion des Réservations");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // NORTH : en-tête (gauche) + solde encadré (droite)
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel headerLabel = new JLabel("Hôtel de Prestige — Gestion des Réservations");
        headerLabel.setFont(Style.policeTitre(22));
        headerLabel.setForeground(Style.OR_LAITON);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        northPanel.add(headerLabel, BorderLayout.WEST);

        JPanel soldePanel = new JPanel(new BorderLayout());
        soldePanel.setBackground(Style.FOND_SECONDAIRE);
        soldePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Style.OR_LAITON, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel soldeLibelle = new JLabel("Solde actuel :  ");
        soldeLibelle.setForeground(Style.IVOIRE);
        soldeLabel.setForeground(Style.OR_PALE);
        soldeLabel.setFont(soldeLabel.getFont().deriveFont(Font.BOLD, 15f));
        JButton rectifierButton = new JButton("Rectifier le solde");
        Style.boutonSecondaire(rectifierButton);
        rectifierButton.addActionListener(e -> rectifierSolde());
        soldePanel.add(soldeLibelle, BorderLayout.WEST);
        soldePanel.add(soldeLabel, BorderLayout.CENTER);
        soldePanel.add(rectifierButton, BorderLayout.EAST);
        northPanel.add(soldePanel, BorderLayout.EAST);
        add(northPanel, BorderLayout.NORTH);

        // CENTER : onglets
        chambrePanel = new ChambrePanel(this);
        reserverPanel = new ReserverPanel(this);
        occuperPanel = new OccuperPanel(this);
        sejournerPanel = new SejournerPanel(this);
        chambreLibrePanel = new ChambreLibrePanel(this);

        tabbedPane.addTab("Chambres", chambrePanel);
        tabbedPane.addTab("Réservations", reserverPanel);
        tabbedPane.addTab("Occupations", occuperPanel);
        tabbedPane.addTab("Séjours", sejournerPanel);
        tabbedPane.addTab("Chambres libres", chambreLibrePanel);
        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> {
            rafraichirSolde();
            Component selected = tabbedPane.getSelectedComponent();
            if (selected instanceof ChambrePanel) {
                ((ChambrePanel) selected).rafraichir();
            } else if (selected instanceof ReserverPanel) {
                ((ReserverPanel) selected).rafraichir();
            } else if (selected instanceof OccuperPanel) {
                ((OccuperPanel) selected).rafraichir();
            } else if (selected instanceof SejournerPanel) {
                ((SejournerPanel) selected).rafraichir();
            }
        });

        rafraichirSolde();
    }

    /** Rafraîchit l'affichage du solde courant. */
    public void rafraichirSolde() {
        try {
            soldeLabel.setText(String.valueOf(new SoldeDAO().getSolde()));
        } catch (SQLException e) {
            soldeLabel.setText("?");
        }
    }

    /** Demande un montant à ajouter ou retirer du solde (correction manuelle). */
    private void rectifierSolde() {
        String saisie = JOptionPane.showInputDialog(this,
                "Montant à ajouter au solde (un montant négatif pour retirer) :",
                "Rectifier le solde", JOptionPane.QUESTION_MESSAGE);
        if (saisie == null) {
            return; // annulé
        }
        try {
            int montant = Integer.parseInt(saisie.trim());
            new SoldeDAO().ajouterAuSolde(montant);
            JOptionPane.showMessageDialog(this, "Le solde a été rectifié avec succès.",
                    "Solde rectifié", JOptionPane.INFORMATION_MESSAGE);
            rafraichirSolde();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Le montant saisi est invalide : un entier est attendu (ex : -500).",
                    "Données invalides", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur d'accès à la base de données : " + e.getMessage(),
                    "Base de données", JOptionPane.ERROR_MESSAGE);
        }
    }
}
