package com.hotel.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.hotel.dao.SoldeDAO;

/**
 * Fenêtre principale : onglets Chambres, Réservations, Occupations et Séjours,
 * avec un label affichant le solde actuel en haut.
 */
public class MainFrame extends JFrame {

    private final JLabel soldeLabel = new JLabel("—");
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final ChambrePanel chambrePanel;
    private final ReserverPanel reserverPanel;
    private final OccuperPanel occuperPanel;
    private final SejournerPanel sejournerPanel;

    public MainFrame() {
        super("Gestion des Réservations d'Hôtel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // NORTH : solde actuel
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.add(new JLabel("Solde actuel : "));
        soldeLabel.setFont(soldeLabel.getFont().deriveFont(Font.BOLD));
        northPanel.add(soldeLabel);
        add(northPanel, BorderLayout.NORTH);

        // CENTER : onglets
        chambrePanel = new ChambrePanel(this);
        reserverPanel = new ReserverPanel(this);
        occuperPanel = new OccuperPanel(this);
        sejournerPanel = new SejournerPanel(this);

        tabbedPane.addTab("Chambres", chambrePanel);
        tabbedPane.addTab("Réservations", reserverPanel);
        tabbedPane.addTab("Occupations", occuperPanel);
        tabbedPane.addTab("Séjours", sejournerPanel);
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
}
