package com.ibm.text.UCD;

import java.io.IOException;
import java.util.*;
import java.io.*;
import java.text.*;

public class CompactName {
    
    static final boolean DEBUG = false;
    
    public static void main(String[] args) throws IOException {
        
        int test = tokenFromString("ABZ");
        String ss = stringFromToken(test);
        System.out.println(ss);
        
        CompactName.addWord("ABSOLUTEISM");
        
        for (int i = 0; i < CompactName.lastToken; ++i) {
            String s = CompactName.stringFromToken(i);
            System.out.println(s);
        }
        
    }
    
    
    static final char[] compactMap = new char[128];
    static final char[] compactUnmap = new char[128];
    
    static {
        char counter = 0;
        compactMap[0] = counter++;
        for (int i = 'A'; i <= 'Z'; ++i) {
            compactMap[i] = counter++;
        }
        compactMap['-'] = counter++;
        compactMap['>'] = counter++;
        compactMap['<'] = counter++;
        compactMap['*'] = counter++;
        
        compactUnmap[0] = 0;
        for (char i = 0; i < compactUnmap.length; ++i) {
            int x = compactMap[i];
            if (x != 0) compactUnmap[x] = i;
        }
    }
    
    /*
    static String expand(String s) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            int m = s.charAt(i);
            if (m == 31 && i < s.length() + 1) {
                m = 31 + s.charAt(++i);
            }
            result.append(compactUnmap[m]);
        }
        return result.toString();
    }
    
    static String compact(String s) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            int m = compactMap[s.charAt(i)];
            if (m >= 31) {
                result.append((char)31);
                m -= 31;
            }
            result.append(m);
        }
        return result.toString();
    }
    */
    
    static Map string_token = new HashMap();
    static Map token_string = new HashMap();
    
    static int[] tokenList = new int[40000];
    static final int tokenStart = 0;
    static int lastToken = 0;
    
    static int spacedMinimum = Integer.MAX_VALUE;
    
    static boolean isLiteral(int i) {
        return (i & 0x8000) != 0;
    }
    
    static int addTokenForString(String s, int lead, int trail) {
        Object in = string_token.get(s);
        if (in != null) throw new IllegalArgumentException();
        int value = (lead << 16) + (trail & 0xFFFF);
        int result = lastToken;
        tokenList[lastToken++] = value;
        
        if (DEBUG) {
            System.out.println("'" + s + "', tokenList[" + result + "] = lead: " + lead + ", trail: " + trail);
            String roundTrip = stringFromToken(result);
            if (!roundTrip.equals(s)) {
                System.out.println("\t*** No Round Trip: '" + roundTrip + "'");
            }
        }
        string_token.put(s, new Integer(result));
        return result;
    }
    
    static String stringFromToken(int i) {
        String result;
        if ((i & 0x8000) != 0) {
            char first = compactUnmap[(i >> 10) & 0x1F];
            char second = compactUnmap[(i >> 5) & 0x1F];
            char third = compactUnmap[i & 0x1F];
            result = String.valueOf(first);
            if (second != 0) result += String.valueOf(second);
            if (third != 0) result += String.valueOf(third);
        } else if (i > lastToken) {
            throw new IllegalArgumentException("bad token: " + i);
        } else {
            int value = tokenList[i];
            int lead = value >>> 16;
            int trail = value & 0xFFFF;
            if (i >= spacedMinimum) result = stringFromToken(lead) + ' ' + stringFromToken(trail);
            else result = stringFromToken(lead) + stringFromToken(trail);
        }
        if (DEBUG) System.out.println("token: " + i + " => '" + result + "'");
        return result;
    }
    
    static int tokenFromString(String s) {
        if (s.length() <= 3) {
            int first = compactMap[s.charAt(0)];
            int second = compactMap[s.length() > 1 ? s.charAt(1) : 0];
            int third = compactMap[s.length() > 2 ? s.charAt(2) : 0];
            return 0x8000 + (first << 10) + (second << 5) + third;
        }
        Object in = string_token.get(s);
        if (in == null) return -1;
        return ((Integer)in).intValue();
    }
    
    
    static int addWord(String s) {
        
        int result = tokenFromString(s);
        if (result != -1) return result;
        int bestLen = 0;
        int best_i = 0;
        
        int limit = s.length() - 1;
        
        for (int i = limit; i >= 1; --i) {

            String firstPart = s.substring(0, i);
            String lastPart = s.substring(i);

            int lead = tokenFromString(firstPart);
            int trail = tokenFromString(lastPart);
            
            if (lead >= 0 && trail >= 0) { // if both match, return immediately with pair
                if (DEBUG) show(s, firstPart, lastPart, "MATCH BOTH");
                return addTokenForString(s, lead, trail);
            }
            if (!isLiteral(lead)) {
                if (i > bestLen) {
                    bestLen = i;
                    best_i = i;
                }
            }
            if (!isLiteral(trail)) {
                int end_i = s.length() - i;
                if (end_i > bestLen) {
                    bestLen = end_i;
                    best_i = i;
                }
            }
        }
        if (bestLen > 0) { // if one matches, recurse -- and return pair
            String firstPart = s.substring(0, best_i);
            String lastPart = s.substring(best_i);
            int lead = tokenFromString(firstPart);
            int trail = tokenFromString(lastPart);
            if (lead >= 0) {
                if (DEBUG) show(s, firstPart, lastPart, "MATCH FIRST");
                return addTokenForString(s, lead, addWord(lastPart));
            } else {
                if (DEBUG) show(s, firstPart, lastPart, "MATCH SECOND");
                return addTokenForString(s, addWord(firstPart), trail);
            }
        }
        
        // break at multiple of 3
        
        best_i = ((s.length() + 1) / 6) * 3;
        String firstPart = s.substring(0, best_i);
        String lastPart = s.substring(best_i);
        if (DEBUG) show(s, firstPart, lastPart, "Fallback");
        return addTokenForString(s, addWord(firstPart), addWord(lastPart));
    }
    
    static void show(String s, String firstPart, String lastPart, String comment) {
        System.out.println((s) + " => '" + (firstPart)
            + "' # '" + (lastPart) + "' " + comment);
    }
    
    static void startLines() {
        spacedMinimum = lastToken;
    }
    
    static int addLine(String s) {
        
        int result = tokenFromString(s);
        if (result != -1) return result;
        int bestLen = 0;
        int best_i = 0;
        
        int limit = s.length() - 2;
        
        for (int i = limit; i >= 1; --i) {
            char c = s.charAt(i);
            if (c != ' ') continue;

            String firstPart = s.substring(0, i);
            String lastPart = s.substring(i+1);

            int lead = tokenFromString(firstPart);
            int trail = tokenFromString(lastPart);
            
            if (lead >= 0 && trail >= 0) { // if both match, return immediately with pair
                if (DEBUG) show(s, firstPart, lastPart, "MATCH BOTH");
                return addTokenForString(s, lead, trail);
            }
            if (i > bestLen) {
                bestLen = i;
                best_i = i;
            }

            int end_i = s.length() - i - 1;
            if (end_i > bestLen) {
                bestLen = end_i;
                best_i = i;
            }
        }
        if (bestLen > 0) { // if one matches, recurse -- and return pair
            String firstPart = s.substring(0, best_i);
            String lastPart = s.substring(best_i + 1);
            int lead = tokenFromString(firstPart);
            int trail = tokenFromString(lastPart);
            if (lead >= 0) {
                if (DEBUG) show(s, firstPart, lastPart, "MATCH FIRST");
                return addTokenForString(s, lead, addLine(lastPart));
            } else {
                if (DEBUG) show(s, firstPart, lastPart, "MATCH SECOND");
                return addTokenForString(s, addLine(firstPart), trail);
            }
        }
        
        System.out.println("SHOULD HAVE MATCHED!!");
        throw new IllegalArgumentException("SHOULD HAVE MATCHED!! " + s);
    }
}