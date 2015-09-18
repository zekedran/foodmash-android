package in.foodmash.app.commons;


import android.content.Context;
import android.provider.Settings;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class Cryptography {

    private static SecretKeySpec secretKey ;
    private static byte[] key ;

    private static void setKey(String myKey) throws UnsupportedEncodingException {
        MessageDigest sha = null;
        try {
            key = hexToByte(myKey);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit
            System.out.println("Key: " + byteToHex(key));
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
    }

    private static String encrypt(String strToEncrypt, String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            System.out.println("String to encrypt: "+strToEncrypt);
            System.out.println("Base64: " +Base64.encode(cipher.doFinal(hexToByte(strToEncrypt))));
            System.out.println("Decrypt: " +decrypt(Base64.encode(cipher.doFinal(hexToByte(strToEncrypt))), key));
            return Base64.encode(cipher.doFinal(hexToByte(strToEncrypt)));
        } catch (Exception e) {  System.out.println("Error while encrypting: " + e.toString()); e.printStackTrace(); }
        return null;
    }

    private static String decrypt(String strToDecrypt, String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.decode(cipher.doFinal(strToDecrypt.getBytes()))));
        } catch (Exception e) { System.out.println("Error while decrypting: "+e.toString()); e.printStackTrace(); }
        return null;
    }

    public static String getEncryptedAndroidId(Context context, String key) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
        return encrypt(androidId,key);
    }

    private static String byteToHex(byte[] value) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static byte[] hexToByte(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];
        for(int i = 0; i < len; i+=2)
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        return data;
    }
}