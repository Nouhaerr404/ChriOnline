package ma.ensate.test;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SynFloodSimulator {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000; // port configuré dans .env
    private static final int    NB_CONNEXIONS = 200;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Démarrage simulation SYN Flood - " 
                           + NB_CONNEXIONS + " connexions...");

        ExecutorService pool = Executors.newFixedThreadPool(50);

        for (int i = 0; i < NB_CONNEXIONS; i++) {
            pool.submit(() -> {
                try {
                    // Ouvre la connexion TCP mais n'envoie RIEN
                    // = simule un SYN sans ACK applicatif
                    Socket s = new Socket(HOST, PORT);
                    Thread.sleep(30_000); // reste ouvert 30s sans parler
                    s.close();
                } catch (Exception e) {
                    System.out.println("Connexion refusée (protection active) : " 
                                       + e.getMessage());
                }
            });
            Thread.sleep(10); // 1 connexion toutes les 10ms
        }

        pool.shutdown();
        System.out.println("Simulation terminée.");
    }
}
