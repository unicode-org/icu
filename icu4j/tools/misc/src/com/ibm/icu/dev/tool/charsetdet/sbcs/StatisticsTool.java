/*
 ***********************************************************************
 * Copyright (C) 2005-2007, International Business Machines            *
 * Corporation and others. All Rights Reserved.                        *
 ***********************************************************************
 *
 */

package com.ibm.icu.dev.tool.charsetdet.sbcs;


import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.ibm.icu.impl.Utility;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StatisticsTool implements NGramParser.NGramParserClient, NGramList.NGramKeyMapper
{
    /* TODO Make this usage string more sane. */
    private static final String usageString = 
        "\nUsage: StatisticsTool [OPTIONS] [FILES]\n\n" +
        "This program will read in a Unicode text file of text in a particular language\n" +
        "and compute the statistics needed to detected that language and character set.\n " +
        "Options:\n" +
        "-e       specify the target encoding\n" +
        "-h or -? print this usage text.\n" +
        "-v       also generate statistics for visual order.\n" +
        "-l       only generate statistics for logical order (cancel -v)." +
        "-c       run the checker.\n" +
        "-t       run the encoding test.\n" +
        "example: com.ibm.icu.dev.tool.charset.StatisticsTool -e 8859-1 Spanish.txt";

    private static final int BUFFER_SIZE = 1024;
    
    private char[] buffer;
    private int bufIndex;
    private int bufMax;

    private InputFile inputFile;
    
    private NGramList ngrams;
    
    private static byte[] allBytes = {
            (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07,
            (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x0D, (byte) 0x0E, (byte) 0x0F,
            (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17,
            (byte) 0x18, (byte) 0x19, (byte) 0x1A, (byte) 0x1B, (byte) 0x1C, (byte) 0x1D, (byte) 0x1E, (byte) 0x1F,
            (byte) 0x20, (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24, (byte) 0x25, (byte) 0x26, (byte) 0x27,
            (byte) 0x28, (byte) 0x29, (byte) 0x2A, (byte) 0x2B, (byte) 0x2C, (byte) 0x2D, (byte) 0x2E, (byte) 0x2F,
            (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37,
            (byte) 0x38, (byte) 0x39, (byte) 0x3A, (byte) 0x3B, (byte) 0x3C, (byte) 0x3D, (byte) 0x3E, (byte) 0x3F,
            (byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45, (byte) 0x46, (byte) 0x47,
            (byte) 0x48, (byte) 0x49, (byte) 0x4A, (byte) 0x4B, (byte) 0x4C, (byte) 0x4D, (byte) 0x4E, (byte) 0x4F,
            (byte) 0x50, (byte) 0x51, (byte) 0x52, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57,
            (byte) 0x58, (byte) 0x59, (byte) 0x5A, (byte) 0x5B, (byte) 0x5C, (byte) 0x5D, (byte) 0x5E, (byte) 0x5F,
            (byte) 0x60, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67,
            (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F,
            (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77,
            (byte) 0x78, (byte) 0x79, (byte) 0x7A, (byte) 0x7B, (byte) 0x7C, (byte) 0x7D, (byte) 0x7E, (byte) 0x7F,
            (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87,
            (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x8B, (byte) 0x8C, (byte) 0x8D, (byte) 0x8E, (byte) 0x8F,
            (byte) 0x90, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96, (byte) 0x97,
            (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0x9B, (byte) 0x9C, (byte) 0x9D, (byte) 0x9E, (byte) 0x9F,
            (byte) 0xA0, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5, (byte) 0xA6, (byte) 0xA7,
            (byte) 0xA8, (byte) 0xA9, (byte) 0xAA, (byte) 0xAB, (byte) 0xAC, (byte) 0xAD, (byte) 0xAE, (byte) 0xAF,
            (byte) 0xB0, (byte) 0xB1, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5, (byte) 0xB6, (byte) 0xB7,
            (byte) 0xB8, (byte) 0xB9, (byte) 0xBA, (byte) 0xBB, (byte) 0xBC, (byte) 0xBD, (byte) 0xBE, (byte) 0xBF,
            (byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7,
            (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC, (byte) 0xCD, (byte) 0xCE, (byte) 0xCF,
            (byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7,
            (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xDB, (byte) 0xDC, (byte) 0xDD, (byte) 0xDE, (byte) 0xDF,
            (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7,
            (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF,
            (byte) 0xF0, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7,
            (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF
    };
    
    /**
     * 
     */
    public StatisticsTool()
    {
        buffer = new char[BUFFER_SIZE];
        
        buffer[0] = ' ';
        bufIndex = 0;
        bufMax = 1;
    }
    
    private static void usage()
    {
        System.out.println(usageString);
    }
    
//    private static void exceptionError(Exception e)
//    {
//        System.err.println("ioError: " + e.toString());
//    }

    private int nextBuffer(InputFile inputFileArg)
    {
        bufIndex = 0;
        
        return inputFileArg.read(buffer);
    }
    
    public char nextChar()
    {
        if (bufIndex >= bufMax) {
            bufMax = nextBuffer(inputFile);
        }
        
        if (bufMax < 0) {
            return 0;
        }
        
        return buffer[bufIndex++];
    }
    
    public void handleNGram(String key)
    {
        ngrams.put(key);
    }
    
    public Object mapKey(String key)
    {
        return key;
    }
    
    private NGramList dumpNGrams()
    {
        String filename = inputFile.getPath();
        int extension = filename.lastIndexOf(".");
        String outputFileName = filename.substring(0, extension) + ".raw" + filename.substring(extension);
        PrintStream output;
        double cumulative = 0;
        
        try {
            output = new PrintStream(
                new FileOutputStream(outputFileName), true, "UTF8");
        } catch (IOException e) {
            System.out.println("? Could not open " + outputFileName + " for writing.");
            return null;
        }
        
        System.out.println(inputFile.getFilename() + ": " + ngrams.getUniqueNGrams() + "/" + ngrams.getTotalNGrams());
        
        ArrayList array = new ArrayList(ngrams.values());
        
        Collections.sort(array);
        
        NGramList stats = new NGramList(inputFile);
        int count = 0;
        int totalNGrams = ngrams.getTotalNGrams();
        
        for (Iterator it = array.iterator(); it.hasNext(); count += 1) {
            NGramList.NGram ngram  = (NGramList.NGram) it.next();
            String value = ngram.getValue();
            int refCount = ngram.getRefCount();
            double ratio  = (double) refCount / totalNGrams * 100.0;
            
            cumulative += ratio;
            
            // TODO check should be count < max && cumulative < maxPercent
            if (count < 64) {
                stats.put(value);
            }
            
            output.println(value + "\t" + refCount + "\t" + ratio + "%\t" + cumulative + "%");
        }
        
        output.close();
        
        return stats;
    }
    
    private void writeStatistics(ArrayList keyList, boolean visual)
    {
        String filename = inputFile.getPath();
        int extension = filename.lastIndexOf(".");
        String outputFileName = filename.substring(0, extension) + "-" + inputFile.getEncoding() +
                                    (visual? "-visual.dat" : ".dat");
        PrintStream output;
        
        try {
            output = new PrintStream(
                new FileOutputStream(outputFileName), true, "ASCII");
        } catch (IOException e) {
            System.out.println("? Could not open " + outputFileName + " for writing.");
            return;
        }
        
        int i = 0;
        
        output.print("    private static int[] ngrams = {");
        
        for (Iterator it = keyList.iterator(); it.hasNext(); i += 1) {
            Integer ngram = (Integer) it.next();
        
            if (i % 16 == 0) {
                output.print("\n        ");
            }
            
            output.print("0x" + Utility.hex(ngram.intValue(), 6) + ", ");
        }
        
        output.println("\n    };\n");
        
        /*
         * Generate the byte map
         */
        char[] unicodes = inputFile.decode(allBytes);
        
        for (int b = 0; b < 256; b += 1) {
            char unicode  = unicodes[b];
            int charClass = NGramParser.getCharClass(unicode);
        
            switch (charClass) {
            case NGramParser.C_LETTER:
                unicodes[b] = Character.toLowerCase(unicode);
                break;
        
            case NGramParser.C_PUNCT:
                unicodes[b] = ' ';
                break;
        
            case NGramParser.C_IGNORE:
            default:
                unicodes[b] = '\0';
            }
        }
        
        byte[] byteMap = inputFile.encode(unicodes);
        
        output.print("    private static byte[] byteMap = {");
        
        for (int b = 0; b < 256; b += 1) {
            if (b % 8 == 0) {
                output.print("\n        ");
            }
            
            output.print("(byte) 0x" + Utility.hex(byteMap[b] & 0xFF, 2) + ", ");
        }
        
        output.println("\n    };");
    }
    
    public NGramList collectStatistics(InputFile file)
    {
        if (!file.open()) {
            return null;
        }
        
        inputFile = file;
        
        NGramParser parser = new NGramParser(this);
        
        ngrams = new NGramList(this);
        parser.parse();
        
        file.close();
        
        NGramList stats    = dumpNGrams();
        ArrayList statKeys = new ArrayList(stats.keys());
        
        Collections.sort(statKeys);
        writeStatistics(statKeys, false);
        
        if (inputFile.getVisualOrder()) {
            ArrayList reversed = new ArrayList(statKeys.size());
            
            for (Iterator it = statKeys.iterator(); it.hasNext();) {
                Integer key = (Integer) it.next();
                int k = key.intValue();
                int r = 0;
                
                while (k != 0) {
                    r = (r << 8) | (k & 0xFF);
                    k >>= 8;
                }
                
                reversed.add(new Integer(r));
            }
            
            Collections.sort(reversed);
            writeStatistics(reversed, true);
        }
        
        return stats;
    }
    
    public static void main(String[] args)
    {
        List list = Arrays.asList(args);
        InputFile[] input_files = new InputFile[args.length];
        int file_count = 0;
        String encoding = null;
        boolean run_checker = false;
        boolean encoding_test = false;
        boolean visual_order = false;
        
        for (Iterator it = list.iterator(); it.hasNext(); /*anything?*/) {
            String arg = (String) it.next();
            
            if (arg.equals("-v")) {
                visual_order = true;
            } else if (arg.equals("-l")) {
                visual_order = false;
            } else if (arg.equals("-c")) {
                run_checker = true;
            } else if (arg.equals("-t")) {
                encoding_test = true;
            } else if (arg.equals("-e")) {
                if (it.hasNext()) {
                    encoding = (String) it.next();
                } else {
                    System.err.println("Error: missing encoding.");
                }
            } else if (arg.startsWith("-")) {
                if (! (arg.equals("-h") || arg.equals("-?"))) {
                    System.err.println("Error: unknown option " + arg);
                }
                
                usage();
            } else {
                input_files[file_count++] = new InputFile(arg, encoding, visual_order);
            }
        }

        if(file_count == 0){
            System.err.println("Error: there are no files to process.");
            usage();
        }
        
        StatisticsTool tool = new StatisticsTool();
        Checker[] checkers  = new Checker[file_count];
        
        for(int i = 0; i < file_count; i += 1) {
            InputFile file = input_files[i];
            
            checkers[i] = new Checker(tool.collectStatistics(file), file);
        }
        
        System.out.println();
        
        /**
         * Checkers
         */
        if (run_checker) {
            for(int c = 0; c < file_count; c += 1) {
                Checker checker = checkers[c];
                
                for(int f = 0; f < file_count; f += 1) {
                    checker.check(input_files[f]);
                }
            }
            
        }

        /*
         * Detection test
         */
        if (encoding_test) {
            char[] buffer   = new char[128];
            
            System.out.println("Detection test");
            
            for (int f = 0; f < file_count; f += 1) {
                InputFile file = input_files[f];
                int[] histogram = new int[file_count];
                int charCount, misses = 0;
                
                System.out.println(file.getFilename() + "(" + file.getEncoding() + "):");
                file.open();
                
                for (int c = 0; c < file_count; c += 1) {
                    checkers[c].setMapper(file);                
                }
                
                // for each buffer
                //     for each checker
                //         call checkBuffer, save score
                //     find highest score, update histogram for that checker
                // show checker histogram
                
                while ((charCount = file.read(buffer)) > 0) {
                    int[] scores = new int[file_count];
                    int bestFit = -1, maxScore = 0;
                    
                    for (int c = 0; c < file_count; c += 1) {
                        scores[c] = checkers[c].checkBuffer(buffer, charCount);
                    }
                    
                    for (int c = 0; c < file_count; c += 1) {
                        int score = scores[c];
                        
                        if (score > maxScore) {
                            maxScore = score;
                            bestFit = c;
                        }
                    }
                    
                    if (bestFit >= 0) {
                        histogram[bestFit] += 1;                    
                    } else {
                        misses += 1;
                    }
                }
                
                for (int c = 0; c < file_count; c += 1) {
                    System.out.println("    " + checkers[c].getLanguage() + ": " + histogram[c]);
                }
                
                if (misses > 0) {
                    System.out.println("    NONE: " + misses);
                }
                
                System.out.println();
            }            
        }
    }
}
