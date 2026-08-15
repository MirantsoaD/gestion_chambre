package com.hotel.model;

import java.time.LocalDate;

/**
 * Représente une réservation de chambre.
 * <p>
 * {@code annulee} permet de libérer la chambre sans supprimer physiquement
 * la ligne (règle métier d'annulation).
 */
public class Reserver {

    private int idReserv;
    private String numChambre;
    private LocalDate dateReserv;
    private LocalDate dateEntree;
    private int nbrJour;
    private String nomClient;
    private String mail;
    private boolean annulee;

    public Reserver() {
    }

    public Reserver(int idReserv, String numChambre, LocalDate dateReserv,
                    LocalDate dateEntree, int nbrJour, String nomClient,
                    String mail, boolean annulee) {
        this.idReserv = idReserv;
        this.numChambre = numChambre;
        this.dateReserv = dateReserv;
        this.dateEntree = dateEntree;
        this.nbrJour = nbrJour;
        this.nomClient = nomClient;
        this.mail = mail;
        this.annulee = annulee;
    }

    public int getIdReserv() {
        return idReserv;
    }

    public void setIdReserv(int idReserv) {
        this.idReserv = idReserv;
    }

    public String getNumChambre() {
        return numChambre;
    }

    public void setNumChambre(String numChambre) {
        this.numChambre = numChambre;
    }

    public LocalDate getDateReserv() {
        return dateReserv;
    }

    public void setDateReserv(LocalDate dateReserv) {
        this.dateReserv = dateReserv;
    }

    public LocalDate getDateEntree() {
        return dateEntree;
    }

    public void setDateEntree(LocalDate dateEntree) {
        this.dateEntree = dateEntree;
    }

    public int getNbrJour() {
        return nbrJour;
    }

    public void setNbrJour(int nbrJour) {
        this.nbrJour = nbrJour;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public boolean isAnnulee() {
        return annulee;
    }

    public void setAnnulee(boolean annulee) {
        this.annulee = annulee;
    }

    /** Affichage dans les JComboBox du panneau Occupation. */
    @Override
    public String toString() {
        return "Réservation #" + idReserv + " - Chambre " + numChambre
                + " - " + (nomClient == null ? "?" : nomClient)
                + " (du " + dateEntree + ", " + nbrJour + " j)";
    }
}
