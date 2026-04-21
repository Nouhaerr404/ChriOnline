package ma.ensate.server.services;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OtpStore {

    private static final long OTP_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private record OtpEntry(String code, long expiresAt) {}

    private static final Map<Integer, OtpEntry> store = new ConcurrentHashMap<>();

    private OtpStore() {}

    public static String generateAndStore(int userId) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        store.put(userId, new OtpEntry(code, System.currentTimeMillis() + OTP_TTL_MS));
        return code;
    }

    public static boolean validate(int userId, String code) {
        OtpEntry entry = store.get(userId);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expiresAt()) {
            store.remove(userId);
            return false;
        }
        boolean valid = entry.code().equals(code);
        if (valid) store.remove(userId);
        return valid;
    }

    public static void cancel(int userId) {
        store.remove(userId);
    }
}
