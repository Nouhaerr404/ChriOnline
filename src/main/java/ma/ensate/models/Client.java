package ma.ensate.models;

import java.io.Serializable;

public class Client extends Utilisateur implements Serializable {
    private static final long serialVersionUID = 1L;

    private String adresse;
    private String tel;

    public Client() {
        super();
        setTypeCompte("CLIENT");
    }

    public Client(String nom, String email, String password, String adresse, String tel) {
        super(nom, email, password, "CLIENT");
        this.adresse = adresse;
        this.tel = tel;
    }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }
}