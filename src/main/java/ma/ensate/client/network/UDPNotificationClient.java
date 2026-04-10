package ma.ensate.client.network;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import ma.ensate.util.ConfigLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UDPNotificationClient implements Runnable {

    private static final Logger logger = LogManager.getLogger(UDPNotificationClient.class);
    private static final int DEFAULT_UDP_PORT = 5001;
    private static final int MAX_UDP_PORT = 5020;
    private static final int BUFFER_SIZE = 1024;
    private static final int RATE_WINDOW_MS = 1000;
    private static final int MAX_PACKETS_PER_SECOND = ConfigLoader.getInt("UDP_MAX_PPS", 1000);

    private final Set<String> trustedServerIps = chargerIpsServeurFiables();
    private final ConcurrentHashMap<String, RateWindow> packetsParIp = new ConcurrentHashMap<>();
    private final AtomicLong acceptedPackets = new AtomicLong(0);
    private final AtomicLong droppedByFirewall = new AtomicLong(0);
    private final AtomicLong droppedByRateLimit = new AtomicLong(0);

    private volatile boolean running = true;
    private volatile DatagramSocket listenSocket;
    private int udpPort = DEFAULT_UDP_PORT;

    private static class RateWindow {
        private long windowStartMs;
        private int count;

        private RateWindow(long windowStartMs, int count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            this.listenSocket = socket;
            logger.info("UDP listening on port {} | trustedServerIps={} | maxPps={}",
                    udpPort, trustedServerIps, MAX_PACKETS_PER_SECOND);

            while (running) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String sourceIp = packet.getAddress().getHostAddress();
                int sourcePort = packet.getPort();
                int packetSize = packet.getLength();

                if (!isTrustedSource(sourceIp)) {
                    long dropped = droppedByFirewall.incrementAndGet();
                    logger.warn("UDP packet dropped by firewall src={}:{} size={} droppedByFirewall={}",
                            sourceIp, sourcePort, packetSize, dropped);
                    continue;
                }

                if (isRateLimited(sourceIp)) {
                    long dropped = droppedByRateLimit.incrementAndGet();
                    logger.warn("UDP packet dropped by rate-limit src={}:{} size={} maxPps={} droppedByRateLimit={}",
                            sourceIp, sourcePort, packetSize, MAX_PACKETS_PER_SECOND, dropped);
                    continue;
                }

                String message = new String(packet.getData(), 0, packetSize, StandardCharsets.UTF_8);
                long accepted = acceptedPackets.incrementAndGet();
                logger.info("UDP packet accepted src={}:{} size={} accepted={}",
                        sourceIp, sourcePort, packetSize, accepted);

                traiterNotification(message);
            }
        } catch (Exception e) {
            if (running) {
                logger.error("UDP error: {}", e.getMessage());
            }
        }
    }

    public int getUdpPort() {
        return udpPort;
    }

    private boolean isTrustedSource(String sourceIp) {
        return trustedServerIps.contains(sourceIp);
    }

    private boolean isRateLimited(String sourceIp) {
        long now = System.currentTimeMillis();
        RateWindow updated = packetsParIp.compute(sourceIp, (ip, current) -> {
            if (current == null || now - current.windowStartMs >= RATE_WINDOW_MS) {
                return new RateWindow(now, 1);
            }
            current.count++;
            return current;
        });
        return updated != null && updated.count > MAX_PACKETS_PER_SECOND;
    }

    private Set<String> chargerIpsServeurFiables() {
        Set<String> trustedIps = ConcurrentHashMap.newKeySet();
        String trustedHost = ConfigLoader.get("UDP_TRUSTED_HOST",
                ConfigLoader.get("SERVER_HOST", "localhost"));

        try {
            InetAddress[] adresses = InetAddress.getAllByName(trustedHost);
            for (InetAddress adresse : adresses) {
                trustedIps.add(adresse.getHostAddress());
            }
        } catch (Exception e) {
            logger.warn("Could not resolve UDP_TRUSTED_HOST='{}': {}", trustedHost, e.getMessage());
        }

        try {
            trustedIps.add(InetAddress.getByName("127.0.0.1").getHostAddress());
        } catch (Exception ignored) {
        }

        if (trustedIps.isEmpty()) {
            trustedIps.add("127.0.0.1");
            logger.warn("No trusted IP resolved, fallback to 127.0.0.1");
        }

        return trustedIps;
    }

    private void traiterNotification(String message) {
        String titre;
        String contenu;

        if (message.startsWith("COMMANDE_VALIDEE:")) {
            String commandeId = message.replace("COMMANDE_VALIDEE:", "");
            titre = "Commande confirmee";
            contenu = "Votre commande a ete validee.\n"
                    + "Ref : #" + commandeId.substring(0, Math.min(8, commandeId.length())).toUpperCase();
        } else {
            titre = "Notification";
            contenu = message;
        }

        final String titreF = titre;
        final String contenuF = contenu;

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ChriOnline - Notification");
            alert.setHeaderText(titreF);
            alert.setContentText(contenuF);
            alert.show();
        });
    }

    public void stop() {
        running = false;
        DatagramSocket socket = listenSocket;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        logger.info("UDP stop requested | accepted={} firewallDrops={} rateLimitDrops={}",
                acceptedPackets.get(), droppedByFirewall.get(), droppedByRateLimit.get());
    }

    public static UDPNotificationClient demarrer() {
        UDPNotificationClient client = new UDPNotificationClient();

        int port = DEFAULT_UDP_PORT;
        while (port <= MAX_UDP_PORT) {
            try {
                new DatagramSocket(port).close();
                break;
            } catch (Exception e) {
                port++;
            }
        }

        client.udpPort = port;
        Thread thread = new Thread(client);
        thread.setDaemon(true);
        thread.start();
        logger.info("UDP started on port {}", port);
        return client;
    }
}
