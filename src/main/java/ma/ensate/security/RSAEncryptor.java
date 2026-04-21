package ma.ensate.security;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

/**
 * Chiffreur/Déchiffreur RSA pour le protocole sécurisé
 * Utilisé pour chiffrer les clés AES avec RSA lors du handshake
 */
public class RSAEncryptor {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * Chiffre des données avec une clé publique RSA
     * @param data les données à chiffrer (bytes)
     * @param publicKey la clé publique RSA
     * @return les données chiffrées (bytes)
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static byte[] encrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Chiffre des données avec une clé publique RSA et retourne le résultat en Base64
     * @param data les données à chiffrer (bytes)
     * @param publicKey la clé publique RSA
     * @return les données chiffrées encodées en Base64
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static String encryptToBase64(byte[] data, PublicKey publicKey) throws Exception {
        byte[] encryptedData = encrypt(data, publicKey);
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Chiffre une String avec une clé publique RSA
     * @param data la String à chiffrer
     * @param publicKey la clé publique RSA
     * @return les données chiffrées encodées en Base64
     * @throws Exception en cas d'erreur de chiffrement
     */
    public static String encryptString(String data, PublicKey publicKey) throws Exception {
        byte[] dataBytes = data.getBytes();
        return encryptToBase64(dataBytes, publicKey);
    }

    /**
     * Déchiffre des données avec une clé privée RSA
     * @param encryptedData les données chiffrées (bytes)
     * @param privateKey la clé privée RSA
     * @return les données déchiffrées (bytes)
     * @throws Exception en cas d'erreur de déchiffrement
     */
    public static byte[] decrypt(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Déchiffre des données encodées en Base64 avec une clé privée RSA
     * @param encryptedDataBase64 les données chiffrées encodées en Base64
     * @param privateKey la clé privée RSA
     * @return les données déchiffrées (bytes)
     * @throws Exception en cas d'erreur de déchiffrement
     */
    public static byte[] decryptFromBase64(String encryptedDataBase64, PrivateKey privateKey) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
        return decrypt(encryptedData, privateKey);
    }

    /**
     * Déchiffre une String encodée en Base64 avec une clé privée RSA
     * @param encryptedDataBase64 la String chiffrée encodée en Base64
     * @param privateKey la clé privée RSA
     * @return la String déchiffrée
     * @throws Exception en cas d'erreur de déchiffrement
     */
    public static String decryptString(String encryptedDataBase64, PrivateKey privateKey) throws Exception {
        byte[] decryptedData = decryptFromBase64(encryptedDataBase64, privateKey);
        return new String(decryptedData);
    }

    /**
     * Vérifie la taille maximale des données pouvant être chiffrées avec RSA
     * Pour une clé de 2048 bits, la taille max est de 245 bytes (avec PKCS1Padding)
     * @param keySize la taille de la clé RSA en bits
     * @return la taille maximale des données en bytes
     */
    public static int getMaxDataSize(int keySize) {
        // Formule: (keySize / 8) - 11 pour PKCS1Padding
        return (keySize / 8) - 11;
    }

    /**
     * Vérifie si les données peuvent être chiffrées avec RSA
     * @param data les données à vérifier
     * @param keySize la taille de la clé RSA en bits
     * @return true si les données peuvent être chiffrées
     */
    public static boolean canEncrypt(byte[] data, int keySize) {
        return data.length <= getMaxDataSize(keySize);
    }

    /**
     * Lance une exception si les données sont trop grandes pour le chiffrement RSA
     * @param data les données à vérifier
     * @param keySize la taille de la clé RSA en bits
     * @throws IllegalArgumentException si les données sont trop grandes
     */
    public static void validateDataSize(byte[] data, int keySize) {
        if (!canEncrypt(data, keySize)) {
            throw new IllegalArgumentException(
                "Les données sont trop grandes pour le chiffrement RSA. " +
                "Taille max: " + getMaxDataSize(keySize) + " bytes, " +
                "Taille actuelle: " + data.length + " bytes. " +
                "Utilisez RSA pour chiffrer une clé symétrique (AES) à la place."
            );
        }
    }

    /**
     * Chiffre des données avec validation de la taille
     * @param data les données à chiffrer
     * @param publicKey la clé publique RSA
     * @return les données chiffrées en Base64
     * @throws Exception en cas d'erreur
     */
    public static String encryptWithValidation(byte[] data, PublicKey publicKey) throws Exception {
        validateDataSize(data, publicKey.getEncoded().length * 8);
        return encryptToBase64(data, publicKey);
    }
}
