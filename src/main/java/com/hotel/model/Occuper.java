package com.hotel.model;

/**
 * Représente l'arrivée d'un client ayant réservé (occupation d'une chambre).
 */
public class Occuper {

    private int idOccup;
    private int idReserv;
    /** Montant crédité au solde lors de l'arrivée (prix de la nuitée * nombre de jours). */
    private int montant;

    public Occuper() {
    }

    public Occuper(int idOccup, int idReserv) {
        this.idOccup = idOccup;
        this.idReserv = idReserv;
    }

    public Occuper(int idOccup, int idReserv, int montant) {
        this.idOccup = idOccup;
        this.idReserv = idReserv;
        this.montant = montant;
    }

    public int getIdOccup() {
        return idOccup;
    }

    public void setIdOccup(int idOccup) {
        this.idOccup = idOccup;
    }

    public int getIdReserv() {
        return idReserv;
    }

    public void setIdReserv(int idReserv) {
        this.idReserv = idReserv;
    }

    public int getMontant() {
        return montant;
    }

    public void setMontant(int montant) {
        this.montant = montant;
    }
}
