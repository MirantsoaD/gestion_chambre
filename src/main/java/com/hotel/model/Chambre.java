package com.hotel.model;

/**
 * Représente une chambre de l'hôtel.
 */
public class Chambre {

    private String numChambre;
    private String design;
    private String type;
    private int prixNuitee;

    public Chambre() {
    }

    public Chambre(String numChambre, String design, String type, int prixNuitee) {
        this.numChambre = numChambre;
        this.design = design;
        this.type = type;
        this.prixNuitee = prixNuitee;
    }

    public String getNumChambre() {
        return numChambre;
    }

    public void setNumChambre(String numChambre) {
        this.numChambre = numChambre;
    }

    public String getDesign() {
        return design;
    }

    public void setDesign(String design) {
        this.design = design;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPrixNuitee() {
        return prixNuitee;
    }

    public void setPrixNuitee(int prixNuitee) {
        this.prixNuitee = prixNuitee;
    }

    /** Affichage dans les JComboBox : "num - type (prix)". */
    @Override
    public String toString() {
        return numChambre + " - " + (type == null || type.isEmpty() ? "?" : type)
                + " (" + prixNuitee + ")";
    }
}
