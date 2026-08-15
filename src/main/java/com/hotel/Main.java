package com.hotel;

import java.awt.Color;
import java.awt.Font;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.hotel.ui.MainFrame;

/**
 * Point d'entrée de l'application de gestion des réservations d'hôtel.
 * Thème sombre FlatLaf surchargé avec la charte « Hôtel de Prestige » (design.md §4.2 et §8).
 */
public class Main {

    public static void main(String[] args) {
        FlatDarkLaf.setup();

        // Couleurs de fond globales
        UIManager.put("Panel.background", new Color(0x1C1A17));
        UIManager.put("@background", "1C1A17");
        UIManager.put("@foreground", "F3EFE6");
        UIManager.put("@accentColor", "C9A24B");

        // Boutons
        UIManager.put("Button.background", new Color(0x2A2620));
        UIManager.put("Button.foreground", new Color(0xF3EFE6));
        UIManager.put("Button.default.background", new Color(0xC9A24B));
        UIManager.put("Button.default.foreground", new Color(0x1C1A17));
        UIManager.put("Button.arc", 6);

        // Champs de saisie
        UIManager.put("TextField.background", new Color(0x2A2620));
        UIManager.put("TextField.foreground", new Color(0xF3EFE6));
        UIManager.put("Component.focusColor", new Color(0xC9A24B));

        // Onglets
        UIManager.put("TabbedPane.selectedBackground", new Color(0x2A2620));
        UIManager.put("TabbedPane.underlineColor", new Color(0xC9A24B));
        UIManager.put("TabbedPane.foreground", new Color(0xF3EFE6));

        // Tables
        UIManager.put("Table.background", new Color(0xF5F1E8));
        UIManager.put("Table.foreground", new Color(0x241F1A));
        UIManager.put("Table.alternateRowColor", new Color(0xEDE7D8));
        UIManager.put("Table.selectionBackground", new Color(0xC9A24B));
        UIManager.put("Table.selectionForeground", new Color(0x1C1A17));
        UIManager.put("TableHeader.background", new Color(0x2A2620));
        UIManager.put("TableHeader.foreground", new Color(0xE4C97A));

        // Police de base
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
