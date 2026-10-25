package model;

// Fichier envoyé depuis un formulaire (<input type="file">), injecté avec @Param("nomDuChamp")
public class Fichier {
    private String nom;          // nom d'origine : "rapport.pdf"
    private String extension;    // "pdf" (en minuscules, vide si pas d'extension)
    private String contentType;  // type MIME envoyé par le navigateur : "application/pdf"
    private long taille;         // en octets
    private byte[] contenu;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getTaille() {
        return taille;
    }

    public void setTaille(long taille) {
        this.taille = taille;
    }

    public byte[] getContenu() {
        return contenu;
    }

    public void setContenu(byte[] contenu) {
        this.contenu = contenu;
    }

    @Override
    public String toString() {
        return nom + " (" + contentType + ", " + taille + " octets)";
    }
}
