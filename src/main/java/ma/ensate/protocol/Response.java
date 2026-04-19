package ma.ensate.protocol;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Object data;
    
    // Token regénéré pour l'anti-hijacking
    private String newToken;

    public Response() {}

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Constructeur rapide sans data
    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getNewToken() { return newToken; }
    public void setNewToken(String newToken) { this.newToken = newToken; }

    @Override
    public String toString() {
        return "Response{success=" + success +
                ", message=" + message +
                ", data=" + data + "}";
    }
}