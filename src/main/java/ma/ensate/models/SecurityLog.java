package ma.ensate.models;

import java.io.Serializable;
import java.sql.Timestamp;

public class SecurityLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Timestamp timestamp;
    private String ipAddress;
    private String userIdentifier;
    private String actionType;
    private String status;
    private String details;

    public SecurityLog(int id, Timestamp timestamp, String ipAddress, String userIdentifier, String actionType, String status, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.userIdentifier = userIdentifier;
        this.actionType = actionType;
        this.status = status;
        this.details = details;
    }

    public int getId() { return id; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getIpAddress() { return ipAddress; }
    public String getUserIdentifier() { return userIdentifier; }
    public String getActionType() { return actionType; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }
}
