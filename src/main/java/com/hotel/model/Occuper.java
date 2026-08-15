package com.hotel.model;

/**
 * Représente l'arrivée d'un client ayant réservé (occupation d'une chambre).
 */
public class Occuper {

    private int idOccup;
    private int idReserv;

    public Occuper() {
    }

    public Occuper(int idOccup, int idReserv) {
        this.idOccup = idOccup;
        this.idReserv = idReserv;
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
}
