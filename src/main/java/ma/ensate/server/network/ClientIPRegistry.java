package ma.ensate.server.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientIPRegistry {

    private static final Map<Integer, String> registry =
            new ConcurrentHashMap<>();

    public static void register(int clientId, String ip) {
        registry.put(clientId, ip);
    }

    public static void unregister(int clientId) {
        registry.remove(clientId);
    }

    public static String getIP(int clientId) {
        return registry.get(clientId);
    }
}