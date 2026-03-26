package ma.ensate.server.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientIPRegistry {


    private static final Map<Integer, String> registryIP =
            new ConcurrentHashMap<>();


    private static final Map<Integer, Integer> registryPort =
            new ConcurrentHashMap<>();

    public static void register(int clientId, String ip) {
        registryIP.put(clientId, ip);
    }


    public static void registerPort(int clientId, int port) {
        registryPort.put(clientId, port);
    }

    public static void unregister(int clientId) {
        registryIP.remove(clientId);
        registryPort.remove(clientId);
    }

    public static String getIP(int clientId) {
        return registryIP.get(clientId);
    }


    public static int getPort(int clientId) {
        return registryPort.getOrDefault(clientId, 5001);
    }
}