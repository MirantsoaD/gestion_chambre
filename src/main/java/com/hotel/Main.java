package com.hotel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.hotel.ui.MainFrame;

/**
 * Point d'entrée de l'application de gestion des réservations d'hôtel.
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ignored) {
            // Look and feel optionnel : on garde le défaut en cas d'échec.
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
