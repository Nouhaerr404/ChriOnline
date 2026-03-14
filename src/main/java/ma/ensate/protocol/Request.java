package ma.ensate.protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private String action;
    private Object data;
    private String token; // ← AJOUTER CE CHAMP

    public Request() {}

    public Request(String action, Object data) {
        this.action = action;
        this.data   = data;
    }

    public Request(String action, Object data, String token) {
        this.action = action;
        this.data   = data;
        this.token  = token;
    }

    public Request(String action) {
        this.action = action;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getToken() { return token; }  // ← AJOUTER
    public void setToken(String token) { this.token = token; } // ← AJOUTER

    @Override
    public String toString() {
        return "Request{action=" + action + "}";
    }
}