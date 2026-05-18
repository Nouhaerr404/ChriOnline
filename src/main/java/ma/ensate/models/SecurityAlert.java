package ma.ensate.models;

import java.io.Serializable;
import java.sql.Timestamp;

public class SecurityAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private Timestamp timestamp;
    private String alertType;
    private String severity;
    private String targetIp;
    private Integer targetUserId;
    private String description;

    public SecurityAlert(int id, Timestamp timestamp, String alertType, String severity, String targetIp, Integer targetUserId, String description) {
        this.id = id;
        this.timestamp = timestamp;
        this.alertType = alertType;
        this.severity = severity;
        this.targetIp = targetIp;
        this.targetUserId = targetUserId;
        this.description = description;
    }

    public int getId() { return id; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getAlertType() { return alertType; }
    public String getSeverity() { return severity; }
    public String getTargetIp() { return targetIp; }
    public Integer getTargetUserId() { return targetUserId; }
    public String getDescription() { return description; }
}
