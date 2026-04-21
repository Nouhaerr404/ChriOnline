package ma.ensate.server.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPNotificationServer {

    private static final Logger logger = LogManager.getLogger(UDPNotificationServer.class);
    private static final int UDP_PORT = 5001;

    public static void envoyerNotification(String clientIP,
                                           int clientPort,
                                           String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] data = message.getBytes("UTF-8");
            InetAddress adresse = InetAddress.getByName(clientIP);
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, adresse, clientPort);
            socket.send(packet);
            logger.info("Notification UDP → " + clientIP
                    + ":" + clientPort + " : " + message);
        } catch (Exception e) {
            logger.error("Erreur UDP : " + e.getMessage());
        }
    }

    public static void notifierCommandeValidee(String ip,
                                               int port,
                                               String commandeId) {
        envoyerNotification(ip, port, "COMMANDE_VALIDEE:" + commandeId);
    }


}