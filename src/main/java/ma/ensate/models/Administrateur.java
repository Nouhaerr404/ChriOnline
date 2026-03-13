package ma.ensate.models;

import java.io.Serializable;

public class Administrateur extends Utilisateur implements Serializable {
    private static final long serialVersionUID = 1L;

    public Administrateur() {
        super();
        setTypeCompte("ADMINISTRATEUR");
    }

    public Administrateur(String nom, String email, String password) {
        super(nom, email, password, "ADMINISTRATEUR");
    }
}