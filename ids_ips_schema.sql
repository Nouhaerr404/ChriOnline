USE chrionline;

CREATE TABLE IF NOT EXISTS security_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    user_identifier VARCHAR(100),
    action_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    details TEXT
);

CREATE TABLE IF NOT EXISTS security_alerts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    target_ip VARCHAR(45),
    target_user_id INT,
    description TEXT
);

CREATE TABLE IF NOT EXISTS blocked_ips (
    ip_address VARCHAR(45) PRIMARY KEY,
    blocked_until DATETIME NOT NULL,
    reason VARCHAR(255)
);
