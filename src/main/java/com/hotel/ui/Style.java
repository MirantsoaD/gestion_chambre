package com.hotel.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Helper de mise en forme conforme à la charte graphique « Hôtel de Prestige »
 * (voir design.md : palette §2, composants §5, cheat sheet §8).
 */
public final class Style {

    // Palette §2 et cheat sheet §8
    public static final Color FOND = new Color(0x1C1A17);
    public static final Color FOND_SECONDAIRE = new Color(0x2A2620);
    public static final Color OR_LAITON = new Color(0xC9A24B);
    public static final Color OR_PALE = new Color(0xE4C97A);
    public static final Color IVOIRE = new Color(0xF3EFE6);
    public static final Color BEIGE_GRISE = new Color(0xB9B2A3);
    public static final Color CREME_MARBRE = new Color(0xF5F1E8);
    public static final Color NOIR_CAFE = new Color(0x241F1A);
    public static final Color GRIS_TAUPE = new Color(0x6E655A);
    public static final Color SAUGE = new Color(0x7C9473);
    public static final Color BORDEAUX = new Color(0x8C3B3B);
    public static final Color AMBRE = new Color(0xC08A3E);
    public static final Color ARDOISE = new Color(0x5C7A8A);

    private Style() {
    }

    /** Police serif élégante pour les titres (Georgia si disponible, sinon Serif). */
    public static Font policeTitre(int taille) {
        String famille = "Serif";
        String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String nom : disponibles) {
            if ("Georgia".equalsIgnoreCase(nom)) {
                famille = "Georgia";
                break;
            }
        }
        return new Font(famille, Font.BOLD, taille);
    }

    /** Bouton d'action principale : fond Or Laiton, texte Noir Onyx, coins arrondis (arc 8). */
    public static void boutonPrincipal(JButton b) {
        b.setUI(new RoundedButtonUI(8));
        b.setBackground(OR_LAITON);
        b.setForeground(NOIR_CAFE);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        survolPrincipal(b);
    }

    /** Bouton secondaire : fond Anthracite, texte Ivoire, bordure fine Gris Taupe. */
    public static void boutonSecondaire(JButton b) {
        b.setUI(new RoundedButtonUI(6));
        b.setBackground(FOND_SECONDAIRE);
        b.setForeground(IVOIRE);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GRIS_TAUPE, 1),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Bouton destructif : fond Bordeaux discret, texte Ivoire, bordure plus sombre. */
    public static void boutonDestructif(JButton b) {
        b.setUI(new RoundedButtonUI(6));
        b.setBackground(BORDEAUX);
        b.setForeground(IVOIRE);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x6E2B2B), 1),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Label de formulaire : texte Beige Grisé, taille 13. */
    public static void labelFormulaire(JLabel l) {
        l.setForeground(BEIGE_GRISE);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 13f));
    }

    /** Crée un label de formulaire déjà stylisé. */
    public static JLabel libelle(String texte) {
        JLabel l = new JLabel(texte);
        labelFormulaire(l);
        return l;
    }

    /** Panneau de formulaire : carte Anthracite avec titre et bordure Or Laiton. */
    public static JPanel panneauFormulaire(String titre) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(FOND_SECONDAIRE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(GRIS_TAUPE, 1),
                        titre,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        policeTitre(14),
                        OR_LAITON),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        return p;
    }

    /** Léger éclaircissement vers Or Pâle au survol pour les boutons principaux (§5.3). */
    private static void survolPrincipal(JButton b) {
        Color base = b.getBackground();
        Color survol = melanger(base, OR_PALE, 0.3f);
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(survol);
                b.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(base);
                b.repaint();
            }
        });
    }

    private static Color melanger(Color c1, Color c2, float ratio) {
        int r = Math.round(c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int g = Math.round(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int b = Math.round(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        return new Color(r, g, b);
    }

    /** UI de bouton peignant un fond arrondi (rayon 6-8px selon le type de bouton). */
    private static final class RoundedButtonUI extends BasicButtonUI {
        private final int arc;

        RoundedButtonUI(int arc) {
            this.arc = arc;
        }

        @Override
        public void update(Graphics g, JComponent c) {
            if (c.getBackground() != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), arc, arc);
                g2.dispose();
            }
            super.update(g, c);
        }
    }
}
