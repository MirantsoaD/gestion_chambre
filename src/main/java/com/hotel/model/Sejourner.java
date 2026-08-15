package com.hotel.model;

import java.time.LocalDate;

/**
 * Représente un séjour direct (client sans réservation / walk-in).
 */
public class Sejourner {

    private int idSejour;
    private String numChambre;
    private LocalDate dateEntreeSejour;
    private int nbrJour;
    private String nomClient;
    private String telephone;

    public Sejourner() {
    }

    public Sejourner(int idSejour, String numChambre, LocalDate dateEntreeSejour,
                     int nbrJour, String nomClient, String telephone) {
        this.idSejour = idSejour;
        this.numChambre = numChambre;
        this.dateEntreeSejour = dateEntreeSejour;
        this.nbrJour = nbrJour;
        this.nomClient = nomClient;
        this.telephone = telephone;
    }

    public int getIdSejour() {
        return idSejour;
    }

    public void setIdSejour(int idSejour) {
        this.idSejour = idSejour;
    }

    public String getNumChambre() {
        return numChambre;
    }

    public void setNumChambre(String numChambre) {
        this.numChambre = numChambre;
    }

    public LocalDate getDateEntreeSejour() {
        return dateEntreeSejour;
    }

    public void setDateEntreeSejour(LocalDate dateEntreeSejour) {
        this.dateEntreeSejour = dateEntreeSejour;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}
