/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.compression;

import com.ibm.icu.text.UnicodeDecompressor;
import com.ibm.icu.dev.test.TestFmwk;

public class DecompressionTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new DecompressionTest().run(args);
    }

    /** Print out a segment of a character array, if in verbose mode */
    private void log(char [] chars, int start, int count) {
        log("|");
        for(int i = start; i < start + count; ++i) {
            log(String.valueOf(chars[i]));
        }
        log("|");
    }

    /** Print out a segment of a character array, followed by a newline */
    private void logln(char [] chars, int start, int count)
    {
        log(chars, start, count);
        logln("");
    }

    /** Decompress the two segments */
    private String decompressTest(byte [] segment1, byte [] segment2) {
        StringBuffer s = new StringBuffer();
        UnicodeDecompressor myDecompressor = new UnicodeDecompressor();

        int [] bytesRead = new int[1];
        char [] charBuffer = new char [2*(segment1.length + segment2.length)];
        int count1 = 0, count2 = 0;

        count1 = myDecompressor.decompress(segment1, 0, segment1.length,
                                           bytesRead,
                                           charBuffer, 0, charBuffer.length);
        
        logln("Segment 1 (" + segment1.length + " bytes) " +
                "decompressed into " + count1  + " chars");
        logln("Bytes consumed: " + bytesRead[0]);

        logln("Got chars: ");
        logln(charBuffer, 0, count1);
        s.append(charBuffer, 0, count1);

        count2 = myDecompressor.decompress(segment2, 0, segment2.length,
                                           bytesRead,
                                           charBuffer, count1, 
                                           charBuffer.length);
        
        logln("Segment 2 (" + segment2.length + " bytes) " +
                "decompressed into " + count2  + " chars");
        logln("Bytes consumed: " + bytesRead[0]);

        logln("Got chars: ");
        logln(charBuffer, count1, count2);
        
        s.append(charBuffer, count1, count2);

        logln("Result: ");
        logln(charBuffer, 0, count1 + count2);
        logln("====================");

        return s.toString();
    }


    public void testDecompression() throws Exception {
        String result;

        // compressed segment breaking on a define window sequence
        /*                   B     o     o     t     h     SD1  */
        byte [] segment1 = { 0x42, 0x6f, 0x6f, 0x74, 0x68, 0x19 };

        // continuation
        /*                   IDX   ,           S     .          */
        byte [] segment2 = { 0x01, 0x2c, 0x20, 0x53, 0x2e };
        
        result = decompressTest(segment1, segment2);
        if(! result.equals("Booth, S.")) {
            errln("Decompression test failed");
            return;
        }

        // compressed segment breaking on a quote unicode sequence
        /*                   B     o     o     t     SQU        */
        byte [] segment3 = { 0x42, 0x6f, 0x6f, 0x74, 0x0e, 0x00 };

        // continuation
        /*                   h     ,           S     .          */
        byte [] segment4 = { 0x68, 0x2c, 0x20, 0x53, 0x2e };

        result = decompressTest(segment3, segment4);
        if(! result.equals("Booth, S.")) {
            errln("Decompression test failed");
            return;
        }


        // compressed segment breaking on a quote unicode sequence
        /*                   SCU   UQU                         */
        byte [] segment5 = { 0x0f, (byte)0xf0, 0x00 };

        // continuation
        /*                   B                                 */
        byte [] segment6 = { 0x42 };

        result = decompressTest(segment5, segment6);
        if(! result.equals("B")) {
            errln("Decompression test failed");
            return;
        }
    }

};
