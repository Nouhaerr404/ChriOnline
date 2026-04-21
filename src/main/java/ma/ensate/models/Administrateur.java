package ma.ensate.models;

public class Administrateur extends Utilisateur {
    private static final long serialVersionUID = 1L;

    private String publicKey;

    public Administrateur() {
        super();
        setTypeCompte("ADMINISTRATEUR");
    }

    public Administrateur(String nom, String email, String password) {
        super(nom, email, password, "ADMINISTRATEUR");
    }

    public Administrateur(String nom, String email, String password, String publicKey) {
        super(nom, email, password, "ADMINISTRATEUR");
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}