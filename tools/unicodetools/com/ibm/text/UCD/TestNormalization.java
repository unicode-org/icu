package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;

public final class TestNormalization {
    static final String DIR = "C:\\Documents and Settings\\Davis\\My Documents\\UnicodeData\\Update 3.0.1\\";
    static final boolean SKIP_FILE = true;
    
    static PrintWriter out = null;
    static BufferedReader in = null;
    
    static Normalizer nfc;
    static Normalizer nfd;
    static Normalizer nfkc;
    static Normalizer nfkd;
    static UCD ucd;
    
    static BitSet charsListed = new BitSet(0x110000);
    static int errorCount = 0;
    static int lineErrorCount = 0;
    static String originalLine = "";
    static String lastLine = "";
    
    public static void main(String[] args)  throws java.io.IOException {
        System.out.println("Creating Normalizers");
        ucd = UCD.make("");
        
        nfc = new Normalizer(Normalizer.NFC);
        nfd = new Normalizer(Normalizer.NFD);
        nfkc = new Normalizer(Normalizer.NFKC);
        nfkd = new Normalizer(Normalizer.NFKD);
        
            String x = UTF32.valueOf32(0x10000);
            check("NFC", nfc, x);
            check("NFD", nfd, x);
            check("NFKC", nfkc, x);
            check("NFKD", nfkd, x);
        
        
        out = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream("NormalizationTestLog.txt"),
                "UTF8"),
            32*1024));
        
        in = new BufferedReader (
            new FileReader (DIR + "NormalizationTest.txt"),
            32*1024);
        
        try {
            String[] parts = new String[10];
            
            System.out.println("Checking files");
            
            int count = 0;
            
            while (true) {
                String line = in.readLine();
                if ((count++ & 0x3FF) == 0) System.out.println("#LINE: " + line);
                if (line == null) break;
                originalLine = line;
                int pos = line.indexOf('#');
                if (pos >= 0) {
                    line = line.substring(0,pos);
                }
                line = line.trim();
                if (line.length() == 0) continue;
                
                
                int splitCount = Utility.split(line, ';', parts);
                // FIX check splitCount
                for (int i = 0; i < splitCount; ++i) {
                    parts[i] = Utility.fromHex(parts[i]);
                }
                
                if (UTF32.length32(parts[0]) == 1) {
                    int code = UTF32.char32At(parts[0],0);
                    charsListed.set(code);
                    if ((code & 0x3FF) == 0) System.out.println("# " + Utility.hex(code));
                }
                
                // c2 == NFC(c1) == NFC(c2) == NFC(c3)
                errorCount += check("NFCa", nfc, parts[1], parts[0]); 
                errorCount += check("NFCb", nfc, parts[1], parts[1]); 
                errorCount += check("NFCc", nfc, parts[1], parts[2]); 
                
                // c4 == NFC(c4) == NFC(c5)
                errorCount += check("NFCd", nfc, parts[3], parts[3]); 
                errorCount += check("NFCe", nfc, parts[3], parts[4]); 

                // c3 == NFD(c1) == NFD(c2) == NFD(c3)
                errorCount += check("NFDa", nfd, parts[2], parts[0]); 
                errorCount += check("NFDb", nfd, parts[2], parts[1]); 
                errorCount += check("NFDc", nfd, parts[2], parts[2]); 
                
                // c5 == NFD(c4) == NFD(c5)
                errorCount += check("NFDd", nfd, parts[4], parts[3]); 
                errorCount += check("NFDe", nfd, parts[4], parts[4]); 
                
                // c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)
                errorCount += check("NFKCa", nfkc, parts[3], parts[0]); 
                errorCount += check("NFKCb", nfkc, parts[3], parts[1]); 
                errorCount += check("NFKCc", nfkc, parts[3], parts[2]); 
                errorCount += check("NFKCd", nfkc, parts[3], parts[3]); 
                errorCount += check("NFKCe", nfkc, parts[3], parts[4]); 
                
                // c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)
                errorCount += check("NFKDa", nfkd, parts[4], parts[0]); 
                errorCount += check("NFKDb", nfkd, parts[4], parts[1]); 
                errorCount += check("NFKDc", nfkd, parts[4], parts[2]); 
                errorCount += check("NFKDd", nfkd, parts[4], parts[3]); 
                errorCount += check("NFKDe", nfkd, parts[4], parts[4]); 
            }
            System.out.println("Total errors in file: " + errorCount
                + ", lines: " + lineErrorCount);
            errorCount = lineErrorCount = 0;
            
            System.out.println("Checking Missing");
            checkMissing();
            System.out.println("Total errors in unlisted items: " + errorCount
                + ", lines: " + lineErrorCount);
            
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
    
    static String lastBase = "";
    
    public static int check(String type, Normalizer n, String base, String other) {
        try {
            String trans = n.normalize(other);
            if (!trans.equals(base)) {
                String temp = "";
                if (!lastLine.equals(originalLine)) {
                    temp = "// " + originalLine;
                    lastLine = originalLine;
                }
                if (!base.equals(lastBase)) {
                    lastBase = base;
                    lineErrorCount++;
                }
                String otherList = "";
                if (!base.equals(other)) {
                    otherList = "(" + ucd.getCodeAndName(other) + ")";
                }
                out.println("DIFF " + type + ": " 
                    + ucd.getCodeAndName(base) + " != " 
                    + type
                    + otherList
                    + " == " + ucd.getCodeAndName(trans)
                    + temp
                );
                return 1;
            }
        } catch (Exception e) {
            throw new ChainException("DIFF " + type + ": " 
                + ucd.getCodeAndName(base) + " != " 
                + type + "(" + ucd.getCodeAndName(other) + ")", new Object[]{}, e);
        }
        return 0;
    }
    
    public static int check(String type, Normalizer n, String base) {
        return check(type, n, base, base);
    }
    
    static void checkMissing() {
        for (int missing = 0; missing < 0x100000; ++missing) {
            if ((missing & 0xFFF) == 0) System.out.println("# " + Utility.hex(missing));
            if (charsListed.get(missing)) continue;
            String x = UTF32.valueOf32(missing);
            errorCount += check("NFC", nfc, x);
            errorCount += check("NFD", nfd, x);
            errorCount += check("NFKC", nfkc, x);
            errorCount += check("NFKD", nfkd, x);
        }
    }     
    
}
