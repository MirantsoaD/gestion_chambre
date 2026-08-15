package com.hotel.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Génère un reçu de paiement client au format PDF (librairie OpenPDF).
 */
public final class PdfRecuGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private PdfRecuGenerator() {
    }

    /**
     * Écrit un reçu de paiement dans le fichier donné.
     *
     * @param fichier    fichier PDF à créer (ex : choix de l'utilisateur via JFileChooser)
     * @param numeroRecu numéro du reçu (ex : "OCC-12")
     * @param nomClient  nom du client
     * @param chambre    chambre concernée (ex : "A1 - Simple (1000)")
     * @param dateEntree date d'entrée au format dd-MM-yyyy (déjà formatée)
     * @param nbrJour    nombre de jours
     * @param montant    montant payé
     * @throws DocumentException si le document PDF ne peut être créé
     * @throws IOException       si le fichier ne peut être écrit
     */
    public static void genererRecu(File fichier, String numeroRecu, String nomClient,
                                   String chambre, String dateEntree, int nbrJour, int montant)
            throws DocumentException, IOException {

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(fichier));
        document.open();

        Font titreFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        Paragraph titre = new Paragraph("REÇU DE PAIEMENT", titreFont);
        titre.setAlignment(Element.ALIGN_CENTER);
        document.add(titre);

        Paragraph sousTitre = new Paragraph("Hôtel — Gestion des Réservations", normalFont);
        sousTitre.setAlignment(Element.ALIGN_CENTER);
        document.add(sousTitre);

        document.add(new Paragraph(" "));

        document.add(new Paragraph("N° reçu         : " + valeur(numeroRecu), normalFont));
        document.add(new Paragraph("Date du reçu    : " + LocalDate.now().format(FMT), normalFont));
        document.add(new Paragraph("Client          : " + valeur(nomClient), normalFont));
        document.add(new Paragraph("Chambre         : " + valeur(chambre), normalFont));
        document.add(new Paragraph("Date d'entrée   : " + valeur(dateEntree), normalFont));
        document.add(new Paragraph("Nombre de jours : " + nbrJour, normalFont));
        document.add(new Paragraph("Montant payé    : " + montant, boldFont));

        document.add(new Paragraph(" "));

        Paragraph merci = new Paragraph("Merci de votre visite !", normalFont);
        merci.setAlignment(Element.ALIGN_CENTER);
        document.add(merci);

        document.close();
    }

    private static String valeur(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }
}
