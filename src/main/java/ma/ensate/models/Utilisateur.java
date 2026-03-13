package ma.ensate.models;

import java.io.Serializable;

public class Utilisateur implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String nom;
    private String email;
    private String password;
    private String typeCompte;
    private String sessionToken;

    public Utilisateur() {}

    public Utilisateur(String nom, String email, String password, String typeCompte) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.typeCompte = typeCompte;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTypeCompte() { return typeCompte; }
    public void setTypeCompte(String typeCompte) { this.typeCompte = typeCompte; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    @Override
    public String toString() {
        return "Utilisateur{id=" + id + ", nom=" + nom + ", email=" + email + "}";
    }
}