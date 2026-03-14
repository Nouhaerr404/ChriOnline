package ma.ensate.models;

public class Administrateur extends Utilisateur {
    private static final long serialVersionUID = 1L;

    public Administrateur() {
        super();
        setTypeCompte("ADMINISTRATEUR");
    }

    public Administrateur(String nom, String email, String password) {
        super(nom, email, password, "ADMINISTRATEUR");
    }
}