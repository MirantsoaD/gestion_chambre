package com.hotel.util;

import com.hotel.model.Chambre;
import com.hotel.model.Reserver;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Envoi de mails de confirmation de réservation via SMTP (Jakarta Mail).
 * La configuration est lue dans {@code mail.properties} (classpath).
 */
public final class MailSender {

    private MailSender() {
    }

    /**
     * Envoie un mail de confirmation de réservation au client.
     *
     * @param destinataire adresse mail du client
     * @param r            réservation enregistrée
     * @param c            chambre concernée
     * @throws MessagingException si l'envoi échoue (SMTP injoignable,
     *                            mauvais identifiants...)
     */
    public static void envoyerMailReservation(String destinataire, Reserver r, Chambre c)
            throws MessagingException {

        Properties config = chargerConfig();

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", config.getProperty("mail.smtp.host", "smtp.gmail.com"));
        mailProps.put("mail.smtp.port", config.getProperty("mail.smtp.port", "587"));
        mailProps.put("mail.smtp.auth", config.getProperty("mail.smtp.auth", "true"));
        mailProps.put("mail.smtp.starttls.enable",
                config.getProperty("mail.smtp.starttls.enable", "true"));

        final String user = config.getProperty("mail.user", "");
        final String password = config.getProperty("mail.password", "");

        Session session = Session.getInstance(mailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(user));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        message.setSubject("Confirmation de votre réservation");

        StringBuilder corps = new StringBuilder();
        corps.append("Bonjour ");
        corps.append(r.getNomClient() == null ? "" : r.getNomClient());
        corps.append(",\n\n");
        corps.append("Nous avons le plaisir de confirmer votre réservation.\n");
        corps.append("Voici le récapitulatif de votre séjour :\n\n");
        corps.append("  Numéro de chambre   : ").append(r.getNumChambre()).append('\n');
        corps.append("  Date de réservation : ").append(r.getDateReserv()).append('\n');
        corps.append("  Date d'entrée       : ").append(r.getDateEntree()).append('\n');
        corps.append("  Nombre de jours     : ").append(r.getNbrJour()).append('\n');
        corps.append("  Nom du client       : ").append(r.getNomClient()).append("\n\n");
        corps.append("Nous nous réjouissons de vous accueillir prochainement.\n");
        corps.append("À bientôt dans notre hôtel !\n");
        message.setText(corps.toString(), "UTF-8");

        Transport.send(message);
    }

    private static Properties chargerConfig() {
        Properties props = new Properties();
        try (InputStream in = MailSender.class.getClassLoader()
                .getResourceAsStream("mail.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Fichier mail.properties introuvable dans src/main/resources.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de lire mail.properties : " + e.getMessage(), e);
        }
        return props;
    }
}
