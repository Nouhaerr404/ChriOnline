package ma.ensate.server.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPNotificationServer {

    private static final Logger logger = LogManager.getLogger(UDPNotificationServer.class);
    private static final int UDP_PORT = 5001;

    public static void envoyerNotification(String clientIP, String message) {
        try (DatagramSocket socket = new DatagramSocket()) {

            byte[] data = message.getBytes("UTF-8");
            InetAddress adresse = InetAddress.getByName(clientIP);
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, adresse, UDP_PORT);

            socket.send(packet);
            logger.info(" Notification UDP envoyée à "
                    + clientIP + " : " + message);

        } catch (Exception e) {
            logger.error(" Erreur envoi notification UDP : "
                    + e.getMessage());
        }
    }

    public static void notifierCommandeValidee(String clientIP,
                                               String commandeId) {
        envoyerNotification(clientIP,
                "COMMANDE_VALIDEE:" + commandeId);
    }

    public static void notifierCommandeExpediee(String clientIP,
                                                String commandeId) {
        envoyerNotification(clientIP,
                "COMMANDE_EXPEDIEE:" + commandeId);
    }

    public static void notifierCommandeLivree(String clientIP,
                                              String commandeId) {
        envoyerNotification(clientIP,
                "COMMANDE_LIVREE:" + commandeId);
    }
}