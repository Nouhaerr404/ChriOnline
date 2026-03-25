package ma.ensate.client.network;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPNotificationClient implements Runnable {

    private static final Logger logger = LogManager.getLogger(UDPNotificationClient.class);
    private int udpPort = 5001;
    private static final int BUFFER_SIZE = 1024;

    private boolean running = true;

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            logger.info("UDP en écoute sur port " + udpPort);
            while (running) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0,
                        packet.getLength(), "UTF-8");
                traiterNotification(message);
            }
        } catch (Exception e) {
            if (running) logger.error("Erreur UDP : " + e.getMessage());
        }
    }

    public int getUdpPort() { return udpPort; }
    private void traiterNotification(String message) {
        String titre = "";
        String contenu = "";
        String commandeId = "";

        // Parser le message
        if (message.startsWith("COMMANDE_VALIDEE:")) {
            commandeId = message.replace("COMMANDE_VALIDEE:", "");
            titre   = " Commande confirmée !";
            contenu = "Votre commande a été validée.\n"
                    + "Réf : #" + commandeId.substring(0, 8).toUpperCase();

        } else if (message.startsWith("COMMANDE_EXPEDIEE:")) {
            commandeId = message.replace("COMMANDE_EXPEDIEE:", "");
            titre   = " Commande expédiée !";
            contenu = "Votre commande est en route.\n"
                    + "Réf : #" + commandeId.substring(0, 8).toUpperCase();

        } else if (message.startsWith("COMMANDE_LIVREE:")) {
            commandeId = message.replace("COMMANDE_LIVREE:", "");
            titre   = " Commande livrée !";
            contenu = "Votre commande a été livrée.\n"
                    + "Réf : #" + commandeId.substring(0, 8).toUpperCase();
        } else {
            titre   = " Notification";
            contenu = message;
        }

        final String titreF   = titre;
        final String contenuF = contenu;

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ChriOnline — Notification");
            alert.setHeaderText(titreF);
            alert.setContentText(contenuF);
            alert.show();
        });
    }

    public void stop() {
        running = false;
    }

    public static UDPNotificationClient demarrer() {
        UDPNotificationClient client = new UDPNotificationClient();

        int port = 5001;
        while (port <= 5020) {
            try {
                new DatagramSocket(port).close();
                break; // port libre !
            } catch (Exception e) {
                port++;
            }
        }

        client.udpPort = port;
        Thread thread = new Thread(client);
        thread.setDaemon(true);
        thread.start();
        logger.info("UDP démarré sur port " + port);
        return client;
    }
}