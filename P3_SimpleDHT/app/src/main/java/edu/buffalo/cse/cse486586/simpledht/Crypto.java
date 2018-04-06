package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;



public class Crypto {

    static final String TAG = "CRYPTO";

    public static String bytesToHexStr(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String hexStr = formatter.toString();
        formatter.close();
        return hexStr;
    }

    public static String genHash(String inputStr) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            // 20 bytes = 160 bits
            byte[] sha1Hash = sha1.digest(inputStr.getBytes());
            // hexStr, 20 chars;
            String hexStr = bytesToHexStr(sha1Hash);
            Log.i("HASH", inputStr + ": " + hexStr);
            return hexStr;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int cmpHashStr(String s1, String s2) {
        if (s1.compareTo(s2) < 0)
            return -1;
        else if (s1.compareTo(s2) == 0)
            return 0;
        else
            return 1;
    }

    public static byte hexCharToByte(char c) {
        // Make sure the char is lowercase
        c = Character.toLowerCase(c);
        byte b = (byte) "0123456789abcdef".indexOf(c);
        return b;
    }

    public static byte hexCharsToByte(char lChar, char rChar) {
        // int 32 bits, byte 8 bits, hex 4 bits;
        // byte 8 bit = (left 4 bits << 4 & b11110000 ) + (right 4 bits & b00001111)
        int b = ((hexCharToByte(lChar)<<4) & 0xF0) + (hexCharToByte(rChar) & 0x0F);
        return (byte) b;
    }

    public static byte[] hexStrToBytes(String hexStr) {
        char[] hexChars = hexStr.toCharArray();
        int len = hexChars.length;
        byte[] bytes = new byte[len/2];

        for (int i=0; i<len; i+=2) {
            bytes[i/2] = hexCharsToByte(hexChars[i], hexChars[i+1]);
        }
        // result should be equal to the sha1Hash in genHash function
        return bytes;
    }

    public static String byteToBits(byte b) {
        int i = b; i |= 0x0100;
        String s = Integer.toBinaryString(i);
        return s.substring(1, 9);
    }

    public static String bytesToBits(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for(byte b:bytes){
            sb.append(byteToBits(b));
        }
        return sb.toString();
    }

}
