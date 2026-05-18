package ma.ensate.models;

import java.io.Serializable;
import java.sql.Timestamp;

public class BlockedIP implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ipAddress;
    private Timestamp blockedUntil;
    private String reason;

    public BlockedIP(String ipAddress, Timestamp blockedUntil, String reason) {
        this.ipAddress = ipAddress;
        this.blockedUntil = blockedUntil;
        this.reason = reason;
    }

    public String getIpAddress() { return ipAddress; }
    public Timestamp getBlockedUntil() { return blockedUntil; }
    public String getReason() { return reason; }
}
