/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/perf/NormalizerPerformanceTest.java,v $ 
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;

import com.ibm.icu.text.*;
import java.io.*;
import java.util.ArrayList;

public class NormalizerPerformanceTest extends PerfTest {
    
    private String[] NFDFileLines;
    private String[] NFCFileLines;
    private String[] fileLines;
    private boolean  bulkMode = false;
    private boolean  lineMode = false;
    private String NFCSource;
    private String NFDSource;
    private String origSource;
    
    public static void main(String[] args) throws Exception {
        new NormalizerPerformanceTest().run(args);
    }
    
    protected void setup(String[] args) {
        if (args.length < 2) {
            printUsage();
        }
        
        String path = "";
        String encoding = "UTF-8";
        String fileName = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-f")||  args[i].equalsIgnoreCase("--fileName")) {
                if (((i + 1) >= args.length) || (args[i].indexOf(0) == '-')) {
                    printUsage();
                } else {
                    fileName = args[i+1];
                }
            }
            if (args[i].equalsIgnoreCase("-s")||  args[i].equalsIgnoreCase("--sourceDir") ) {
                if (((i + 1) >= args.length) || (args[i].indexOf(0) == '-')) {
                    printUsage();
                } else {
                    path = args[i+1];
                    String fileseparator = System.getProperty("file.separator", "/");
                    if (path.charAt(path.length() - 1) != fileseparator.charAt(0)) {
                        path = path + fileseparator;
                    }
                }
            }
            if (args[i].equalsIgnoreCase("-e") || args[i].equalsIgnoreCase("--encoding")) {
                if (((i + 1) >= args.length) || (args[i].indexOf(0) == '-')) {
                    printUsage();
                } else {
                    encoding = args[i+1];
                }
            }
            if (args[i].equalsIgnoreCase("-b") || args[i].equalsIgnoreCase("--bulkMode")) {
                bulkMode = true;
                lineMode = false;
            }
            if (args[i].equalsIgnoreCase("-l") || args[i].equalsIgnoreCase("--lineMode")) {
                bulkMode = false;
                lineMode = true;
            }
        }
        if(lineMode == bulkMode){
            printUsage();
        }
        if(lineMode){
            fileLines = readLines(path + fileName, encoding);
            //System.out.println("Line Count:" + fileLines.length);
            NFDFileLines = normalizeInput(fileLines, Normalizer.NFD);
            NFCFileLines = normalizeInput(fileLines, Normalizer.NFC);
        }else{
            try{
                InputStream is = new FileInputStream(path+fileName);
                InputStreamReader reader = new InputStreamReader(is,encoding);
                char[] orig = readToEOS(reader);
                origSource = new String(orig,0,orig.length);
                NFCSource = normalizeInput(origSource,Normalizer.NFD);
                NFDSource = normalizeInput(origSource,Normalizer.NFC);
            }catch(Exception e){
                e.printStackTrace();
                throw new RuntimeException(e.toString());
            }
            
        }
        
    }
    
    // Test NFC Performance
    public PerfTest.Function TestICU_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        String nfc = Normalizer.normalize(NFDFileLines[i], Normalizer.NFC);
                    }
                }else{
                    String nfc = Normalizer.normalize(NFDSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestICU_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        String nfc = Normalizer.normalize(NFCFileLines[i], Normalizer.NFC);
                    }
                }else{
                    String nfc = Normalizer.normalize(NFCSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestICU_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        String nfc = Normalizer.normalize(fileLines[i], Normalizer.NFC);
                    }
                }else{
                    String nfc = Normalizer.normalize(origSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    // Test NFD Performance
    public PerfTest.Function TestICU_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        String nfc = Normalizer.normalize(NFDFileLines[i], Normalizer.NFD);
                    }
                }else{
                    String nfc = Normalizer.normalize(NFDSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestICU_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        String nfc = Normalizer.normalize(NFCFileLines[i], Normalizer.NFD);
                    }
                }else{
                    String nfc = Normalizer.normalize(NFCSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestICU_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        String nfc = Normalizer.normalize(fileLines[i], Normalizer.NFD);
                    }
                }else{
                    String nfc = Normalizer.normalize(origSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    // Test NFC Performance
    public PerfTest.Function TestJDK_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        String nfc = sun.text.Normalizer.normalize(NFDFileLines[i], sun.text.Normalizer.COMPOSE, 0);
                    }
                }else{
                    String nfc = sun.text.Normalizer.normalize(NFDSource, sun.text.Normalizer.COMPOSE, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestJDK_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        String nfc = sun.text.Normalizer.normalize(NFCFileLines[i], sun.text.Normalizer.COMPOSE, 0);
                    }
                }else{
                    String nfc = sun.text.Normalizer.normalize(NFCSource, sun.text.Normalizer.COMPOSE, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestJDK_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        String nfc = sun.text.Normalizer.normalize(fileLines[i], sun.text.Normalizer.COMPOSE, 0);
                    }
                }else{
                    String nfc = sun.text.Normalizer.normalize(origSource, sun.text.Normalizer.COMPOSE, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    // Test NFD Performance
    public PerfTest.Function TestJDK_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        String nfc = sun.text.Normalizer.normalize(NFDFileLines[i], sun.text.Normalizer.DECOMP, 0);
                    }
                }else{
                    String nfc = sun.text.Normalizer.normalize(NFDSource, sun.text.Normalizer.DECOMP, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestJDK_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        String nfc = sun.text.Normalizer.normalize(NFCFileLines[i], sun.text.Normalizer.DECOMP, 0);
                    }
                }else{
                    String nfc = sun.text.Normalizer.normalize(NFCSource, sun.text.Normalizer.DECOMP, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestJDK_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        String nfc = sun.text.Normalizer.normalize(fileLines[i], sun.text.Normalizer.DECOMP, 0);
                    }
                }else{
                    String nfc = sun.text.Normalizer.normalize(origSource, sun.text.Normalizer.DECOMP, 0);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    // Test FCD Performance
    public PerfTest.Function TestICU_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        String nfc = Normalizer.normalize(NFDFileLines[i], Normalizer.FCD);
                    }
                }else{
                    String nfc = Normalizer.normalize(NFDSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestICU_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        String nfc = Normalizer.normalize(NFCFileLines[i], Normalizer.FCD);
                    }
                }else{
                    String nfc = Normalizer.normalize(NFCSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestICU_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        String nfc = Normalizer.normalize(fileLines[i], Normalizer.FCD);
                    }
                }else{
                    String nfc = Normalizer.normalize(origSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    // Test Quick Check Performance
    public PerfTest.Function TestQC_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFDFileLines[i], Normalizer.NFC);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFDSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFCFileLines[i], Normalizer.NFC);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFCSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(fileLines[i], Normalizer.NFC);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(origSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFDFileLines[i], Normalizer.NFD);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFDSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFCFileLines[i], Normalizer.NFD);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFCSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(fileLines[i], Normalizer.NFD);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(origSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFDFileLines[i], Normalizer.FCD);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFDSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFCFileLines[i], Normalizer.FCD);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(NFCSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestQC_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        Normalizer.QuickCheckResult result = Normalizer.quickCheck(fileLines[i], Normalizer.FCD);
                    }
                }else{
                    Normalizer.QuickCheckResult result = Normalizer.quickCheck(origSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    // Test isNormalized Performance
    public PerfTest.Function TestIsNormalized_NFC_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(NFDFileLines[i], Normalizer.NFC);
                    }
                }else{
                    Normalizer.isNormalized(NFDSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_NFC_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(NFCFileLines[i], Normalizer.NFC);
                    }
                }else{
                    Normalizer.isNormalized(NFCSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_NFC_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(fileLines[i], Normalizer.NFC);
                    }
                }else{
                    Normalizer.isNormalized(NFDSource, Normalizer.NFC);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_NFD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(NFDFileLines[i], Normalizer.NFD);
                    }
                }else{
                    Normalizer.isNormalized(NFDSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_NFD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(NFCFileLines[i], Normalizer.NFD);
                    }
                }else{
                    Normalizer.isNormalized(NFCSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_NFD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(fileLines[i], Normalizer.NFD);
                    }
                }else{
                    Normalizer.isNormalized(NFDSource, Normalizer.NFD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_FCD_NFD_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(NFDFileLines[i], Normalizer.FCD);
                    }
                }else{
                    Normalizer.isNormalized(NFDSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFDFileLines.length; i++) {
                        totalChars = totalChars + NFDFileLines[i].length();
                    }
                }else{
                    totalChars = NFDSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_FCD_NFC_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(NFCFileLines[i], Normalizer.FCD);
                    }
                }else{
                    Normalizer.isNormalized(NFCSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < NFCFileLines.length; i++) {
                        totalChars = totalChars + NFCFileLines[i].length();
                    }
                }else{
                    totalChars = NFCSource.length();
                }
                return totalChars;
            }
        };
    }
    
    public PerfTest.Function TestIsNormalized_FCD_Orig_Text() {
        return new PerfTest.Function() {
            public void call() {
                if(lineMode ==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        boolean result = Normalizer.isNormalized(fileLines[i], Normalizer.FCD);
                    }
                }else{
                    Normalizer.isNormalized(NFDSource, Normalizer.FCD);
                }
            }
            
            public long getOperationsPerIteration() {
                int totalChars = 0;
                if(lineMode==true){
                    for (int i = 0; i < fileLines.length; i++) {
                        totalChars = totalChars + fileLines[i].length();
                    }
                }else{
                    totalChars = origSource.length();
                }
                return totalChars;
            }
        };
    }
    
   
    private void printUsage() {
        System.out.println("Usage: " + this.getClass().getName() + " [OPTIONS] fileName\n"
                            + "\t-f or --fileName  \tfile to be used as test data\n"
                            + "\t-s or --sourceDir \tsource directory for files followed by path\n"
                            + "\t-e or --encoding  \tencoding of source files\n"
                            + "\t-b or --bulkMode  \tnormalize whole file at once\n"
                            + "\t-l or --lineMode  \tnormalize file one line at a time\n"
            );
        System.exit(1);
    }
    
    private String[] normalizeInput(String[] src, Normalizer.Mode mode) {
        String[] dest = new String[src.length];
        for (int i = 0; i < src.length; i++) {
            dest[i] = Normalizer.normalize(src[i], mode);
        }
        
        return dest;
    }
    private String normalizeInput(String src, Normalizer.Mode mode) {
       return Normalizer.normalize(src, mode);
    }
}
