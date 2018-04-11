package edu.buffalo.cse.cse486586.simpledht;

import android.database.Cursor;
import android.database.MatrixCursor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/*
 * Reference
 *  Dev Docs:
 *      - MatrixCursor: https://developer.android.com/reference/android/database/MatrixCursor.html
 */

public class Utils {

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
            return hexStr;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
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

    public static boolean inInterval(String id, String fromID, String toID) {

        if (toID.compareTo(fromID) > 0) {
            if ((id.compareTo(fromID) > 0) && (id.compareTo(toID) < 0))
                return true;
            else
                return false;

        } else {
            if ((id.compareTo(fromID) < 0) && (id.compareTo(toID) > 0))
                return false;
            else
                return true;
        }
    }

    public static Cursor makeCursor(HashMap<String, String> kvMap) {
        String[] attributes = {"_id", "key", "value"};
        MatrixCursor mCursor = new MatrixCursor(attributes);
        for (Map.Entry entry: kvMap.entrySet()) {
            mCursor.addRow(new Object[] {
                    R.drawable.ic_launcher, entry.getKey(), entry.getValue()});
        }
        return mCursor;
    }

    public static HashMap<String, String> cursorToHashMap(Cursor c) {
        HashMap<String, String> map = new HashMap<String, String>();

        c.moveToFirst();
        while (!c.isAfterLast()) {
            String k = c.getString(c.getColumnIndex("key"));
            String v = c.getString(c.getColumnIndex("value"));
            map.put(k, v);
            c.moveToNext();
        }
        c.close();

        return map;
    }


}
