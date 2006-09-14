/*
 *******************************************************************************
 * Copyright (C) 2002-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /icu/icuhtml/icu.sf.net/docs/eclipse_howto/eclipse3x.html,v 
 com.ibm.icu.dev.test.charset/TestConversion.java,v $ 
 * $Date: 2006/09/14 18:44:15 $ 
 * $Revision: 1.3 $ 
 *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Iterator;

import com.ibm.icu.charset.CharsetDecoderICU;
import com.ibm.icu.charset.CharsetProviderICU;
import com.ibm.icu.dev.test.ModuleTest;
import com.ibm.icu.dev.test.TestDataModule.DataMap;
import com.ibm.icu.impl.ICUResourceBundle;


/**
 * This maps to convtest.c which tests the test file for data-driven conversion tests. 
 * 
 */
public class TestConversion extends ModuleTest {
    /**
     * This maps to the C struct of conversion case in convtest.h that stores the
     * data for a conversion test
     * 
     */
    private class ConversionCase {
        int caseNr;                                         // testcase index   
        String option = null;                               // callback options
        CodingErrorAction cbErrorAction = null;             // callback action type

        // data retrieved from a test case conversion.txt
        String charset;                                     // charset
        String unicode;                                     // unicode string
        ByteBuffer bytes;                                   // bytes
        int[] offsets;                                      // offsets
        boolean finalFlush;                                 // flush
        boolean fallbacks;                                  // fallback
        String outErrorCode;                                // errorCode
        String cbopt;                                       // callback 

        // TestGetUnicodeSet variables
        String map;
        String mapnot;
        int which;
    }

    // public methods --------------------------------------------------------

    public static void main(String[] args) throws Exception {
        new TestConversion().run(args);
    }

    public TestConversion() {
        super("com/ibm/icu/dev/data/testdata/", "conversion");
    }

    /*
     * This method maps to the convtest.cpp runIndexedTest() method to run each
     * type of conversion.
     */
    public void processModules() {
        try { 
            int testFromUnicode = 0;
            String testName = t.getName().toString();
            int testToUnicode = 0;
            // Iterate through and get each of the test case to process
            for (Iterator iter = t.getDataIterator(); iter.hasNext();) {
                DataMap testcase = (DataMap) iter.next();

                if (testName.equalsIgnoreCase("toUnicode")) { 
                    TestToUnicode(testcase, testToUnicode);
                    testToUnicode++;

                } else if (testName.equalsIgnoreCase("fromUnicode")) {                    
                    TestFromUnicode(testcase, testFromUnicode);
                    testFromUnicode++;
                } else if (testName.equalsIgnoreCase("getUnicodeSet")) {
               //     TestGetUnicodeSet(testcase);
                } else {
                    warnln("Could not load the test cases for conversion");
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // private methods -------------------------------------------------------

    private void TestToUnicode(DataMap testcase, int caseNr) {
        // create Conversion case to store the test case data
        ConversionCase cc = new ConversionCase();

        try {
            // retrieve test case data
            cc.caseNr = caseNr;
            cc.charset = ((ICUResourceBundle) testcase.getObject("charset"))
                    .getString();
            cc.bytes = ((ICUResourceBundle) testcase.getObject("bytes"))
                    .getBinary();
            cc.unicode = ((ICUResourceBundle) testcase.getObject("unicode"))
                    .getString();
            cc.offsets = ((ICUResourceBundle) testcase.getObject("offsets"))
                    .getIntVector();
            cc.finalFlush = ((ICUResourceBundle) testcase.getObject("flush"))
                    .getUInt() != 0;
            cc.fallbacks = ((ICUResourceBundle) testcase.getObject("fallbacks"))
                    .getUInt() != 0;
            cc.outErrorCode = ((ICUResourceBundle) testcase
                    .getObject("errorCode")).getString();
            cc.cbopt = ((ICUResourceBundle) testcase.getObject("callback"))
                    .getString();
        } catch (Exception e) { 
            errln("Skipping test: error parsing conversion/toUnicode test case " + cc.caseNr);
            return;
        }
//      ----for debugging only 
        logln("\nTestToUnicode[" + caseNr + "] "
                + cc.charset + " ");
        logln("Bytes:");
        printbytes(cc.bytes, cc.bytes.limit());
        logln("");
        logln("Unicode: " + hex(cc.unicode));    
        logln("Callback: (" + cc.cbopt + ")");  
        logln("\n...............................................");

//         ----for debugging only
        
        //This test case is skipped due to limitation in java's API for decoder replacement 
        // { "ibm-1363", :bin{ a2aea2 }, "\u00a1\u001a", :intvector{ 0, 2 }, :int{1}, :int{0}, "", "?", :bin{""} }
        if(cc.caseNr == 63)
        {
            logln("TestToUnicode[" + cc.caseNr + "] " + cc.charset);
            logln("Skipping test due to limitation in Java API - callback replacement value");
            return;
        }
        // process the retrieved test data case
        if (cc.offsets.length == 0) {
            cc.offsets = null;
        } else if (cc.offsets.length != cc.unicode.length()) { 
            errln("Skipping test: toUnicode[" + cc.caseNr + "] unicode[" + cc.unicode.length()
                    + "] and offsets[" + cc.offsets.length
                    + "] must have the same length");
            return;
        }
        // check for the callback replacement value for unmappable
        // characters or malformed errors
        if (cc.cbopt.length() > 0) {
            switch ((cc.cbopt).charAt(0)) {
            case '?':           //CALLBACK_SUBSTITUTE 
                cc.cbErrorAction = CodingErrorAction.REPLACE;
                break;
            case '0':           //CALLBACK_SKIP 
                cc.cbErrorAction = CodingErrorAction.IGNORE;
                break;
            case '.':           //CALLBACK_STOP 
                cc.cbErrorAction = CodingErrorAction.REPORT;
                break;
            case '&':           //CALLBACK_ESCAPE 
                cc.cbErrorAction = CodingErrorAction.REPORT;
                break;
            default:     
                cc.cbErrorAction = null;
                break;
            }
        }
        // check for any options for the callback value 
        cc.option = cc.cbErrorAction == null ? cc.cbopt : cc.cbopt.substring(1);
        if (cc.option == null) {
            cc.option = null;
        }

        logln("TestToUnicode[" + cc.caseNr + "] " + cc.charset);
        ToUnicodeCase(cc);

    }

    private void ToUnicodeCase(ConversionCase cc) {

        // create converter for charset and decoder for each test case
        CharsetProviderICU provider = new CharsetProviderICU();
        CharsetDecoderICU decoder = null;
        Charset charset = null;

        try {
            charset = (Charset) provider.charsetForName(cc.charset);
            decoder = (CharsetDecoderICU) charset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        } catch (Exception e) {

            logln("Skipping test:(" + cc.charset
                    + ") due to ICU Charset not supported at this time");
            return;
        }

        // set the callback for the decoder
        if (cc.cbErrorAction != null) {
            decoder.onMalformedInput(cc.cbErrorAction);
            decoder.onUnmappableCharacter(cc.cbErrorAction);

            // set the options (if any: SKIP_STOP_ON_ILLEGAL) for callback
            if (cc.option.equals("i")) {
                decoder.onMalformedInput(CodingErrorAction.REPORT);
            }

            // if callback action is replace, and there is a subchar
            // replace the decoder's default replacement value
            // if substring, skip test due to current api not supporting
            // substring replacement
            if (cc.cbErrorAction.equals(CodingErrorAction.REPLACE)) {
                if (cc.cbopt.length() > 1) {
                    if (cc.cbopt.charAt(1) == '=') {
                        logln("Skipping test due to limitation in Java API - substitution string not supported");

                    } else {
                        // // read NUL-separated subchar first, if any
                        // copy the subchar from Latin-1 characters
                        // start after the NUL
                        if (cc.cbopt.charAt(1) == 0x00) {
                            cc.cbopt = cc.cbopt.substring(2);

                            try {
                                decoder.replaceWith(cc.cbopt);
                            } catch (Exception e) {
                                logln("Skipping test due to limitation in Java API - substitution character sequence size error");

                            }
                        }
                    }
                }
            }
        } 

        // decode source to unicode
        ByteBuffer source = ByteBuffer.wrap(cc.bytes.array());
        CharBuffer out = CharBuffer.allocate((int) (decoder
                .averageCharsPerByte() * source.remaining()));
        do {
            CoderResult cr = decoder.decode(source, out, true);
            if (cr.isOverflow()) {
                int pos = out.position();
                char[] temp = out.array();
                out = CharBuffer.allocate(temp.length * 4);
                out.put(temp);
                out.position(pos);
            } else if (cr.isError()) {
                checkResultsToUnicode(cc,cc.unicode, out); 
                return;
            }
        } while (source.remaining() > 0);

        checkResultsToUnicode(cc,cc.unicode, out);
        return;
    }

    private void TestFromUnicode(DataMap testcase, int caseNr) {

        ConversionCase cc = new ConversionCase();
        cc.caseNr = caseNr;

        try {
            // retrieve test case data
            cc.charset = ((ICUResourceBundle) testcase.getObject("charset"))
                    .getString();
            cc.unicode = ((ICUResourceBundle) testcase.getObject("unicode"))
                    .getString();
            cc.bytes = ((ICUResourceBundle) testcase.getObject("bytes"))
                    .getBinary();
            cc.offsets = ((ICUResourceBundle) testcase.getObject("offsets"))
                    .getIntVector();
            cc.finalFlush = ((ICUResourceBundle) testcase.getObject("flush"))
                    .getUInt() != 0;
            cc.fallbacks = ((ICUResourceBundle) testcase.getObject("fallbacks"))
                    .getUInt() != 0;
            cc.outErrorCode = ((ICUResourceBundle) testcase
                    .getObject("errorCode")).getString();
            cc.cbopt = ((ICUResourceBundle) testcase.getObject("callback"))
                    .getString();


        } catch (Exception e) {
            errln("Skipping test:");
            errln("error parsing conversion/toUnicode test case " + cc.caseNr);
            return;
        }
        // ----for debugging only
        logln("\nTestFromUnicode[" + caseNr + "] "
                + cc.charset + " "); 
        logln("Unicode: " + cc.unicode);
        logln("Bytes:");
        printbytes(cc.bytes, cc.bytes.limit());
        logln("");
        logln("Callback: (" + cc.cbopt + ")"); 
        logln("...............................................");

//         ----for debugging only
    
 
        // TODO: ***Currently skipping test for charset ibm-1390, gb18030,
        // ibm-930 due to external mapping need to be fix
        if (cc.charset.equalsIgnoreCase("ibm-1390")
                || cc.charset.equalsIgnoreCase("gb18030")
                || cc.charset.equalsIgnoreCase("ibm-970")
                || cc.charset.equalsIgnoreCase("ibm-930")) {
            logln("Skipping test:("
                    + cc.charset
                    + ") due to ICU Charset external mapping not supported at this time");
            return;
        }

        // process the retrieved test data case
        if (cc.offsets.length == 0) {
            cc.offsets = null;
        } else if (cc.offsets.length != cc.bytes.limit()) {
            errln("fromUnicode[" + cc.caseNr + "] bytes[" + cc.bytes
                    + "] and offsets[" + cc.offsets.length
                    + "] must have the same length");
            return;
        }

        // check the callback replacement value
        if (cc.cbopt.length() > 0) {

            switch ((cc.cbopt).charAt(0)) {
            case '?': 
                cc.cbErrorAction = CodingErrorAction.REPLACE;
                break;
            case '0': 
                cc.cbErrorAction = CodingErrorAction.IGNORE;
                break;
            case '.': 
                cc.cbErrorAction = CodingErrorAction.REPORT;
                break;
            case '&': 
                cc.cbErrorAction = CodingErrorAction.REPORT;
                break;
            default: 
                cc.cbErrorAction = null;
                break;
            }

            // check for any options for the callback value -- 
            cc.option = cc.cbErrorAction == null ? cc.cbopt : cc.cbopt.substring(1);
            if (cc.option == null) {
                cc.option = null;
            }
        }
        logln("TestFromUnicode[" + cc.caseNr + "] " + cc.charset);
        FromUnicodeCase(cc);

        return;

    }

    private void FromUnicodeCase(ConversionCase cc) {

        // create charset encoder for conversion test
        CharsetProviderICU provider = new CharsetProviderICU();
        CharsetEncoder encoder = null;
        Charset charset = null;
        try {
            charset = (Charset) provider.charsetForName(cc.charset);
            encoder = (CharsetEncoder) charset.newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPLACE);
            encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        } catch (Exception e) {

            logln("Skipping test:(" + cc.charset
                    + ") due to ICU Charset not supported at this time");
            return;

        }
 
        // set the callback for the encoder 
        if (cc.cbErrorAction != null) {
            encoder.onUnmappableCharacter(cc.cbErrorAction);
            encoder.onMalformedInput(cc.cbErrorAction);

            // if action has an option, put in the option for the case
            if (cc.option.equals("i")) {
                encoder.onMalformedInput(CodingErrorAction.REPORT);
            }

            // if callback action is replace, and there is a subchar
            // replace the decoder's default replacement value
            // if substring, skip test due to current api not supporting
            // substring
            if (cc.cbErrorAction.equals(CodingErrorAction.REPLACE)) {
                if (cc.cbopt.length() > 1) {
                    if (cc.cbopt.length() > 1 && cc.cbopt.charAt(1) == '=') {
                        logln("Skipping test due to limitation in Java API - substitution string not supported");
                        return;
                    } else {
                        // // read NUL-separated subchar first, if any
                        // copy the subchar from Latin-1 characters
                        // start after the NUL
                        if (cc.cbopt.charAt(1) == 0x00) {
                            cc.cbopt = cc.cbopt.substring(2);

                            try {
                                encoder.replaceWith(cc.cbopt.getBytes());
                            } catch (Exception e) {
                                logln("Skipping test due to limitation in Java API - substitution character sequence size error");
                                return;
                            }
                        }
                    }
                }
            }
        } 
        // do charset encoding from unicode
        CharBuffer uniStr = CharBuffer.wrap(cc.unicode.toCharArray());
        ByteBuffer out = ByteBuffer.allocate((int) (encoder
                .averageBytesPerChar() * uniStr.remaining()));
        do {
            CoderResult cr = encoder.encode(uniStr, out, true);
            if (cr.isOverflow()) {
                int pos = out.position();
                byte[] temp = out.array();
                out = ByteBuffer.allocate(temp.length * 4);
                out.put(temp);
                out.position(pos);
            } else if (cr.isError()) {
                // check the stopped test for current output and match the
                // expected results
                checkResultsFromUnicode(cc,cc.bytes, out);
                return;
            }

        } while (uniStr.remaining() > 0);

        checkResultsFromUnicode(cc,cc.bytes, out);
        return;

    }

    private void TestGetUnicodeSet(DataMap testcase) {
        /*
         * charset - will be opened, and ucnv_getUnicodeSet() called on it //
         * map - set of code points and strings that must be in the returned set //
         * mapnot - set of code points and strings that must *not* be in the //
         * returned set // which - numeric UConverterUnicodeSet value Headers {
         * "charset", "map", "mapnot", "which" }
         */
        ConversionCase cc = new ConversionCase();
        // retrieve test case data
        cc.charset = ((ICUResourceBundle) testcase.getObject("charset"))
                .getString();
        cc.map = ((ICUResourceBundle) testcase.getObject("map")).getString();
        cc.mapnot = ((ICUResourceBundle) testcase.getObject("mapnot"))
                .getString();
        cc.which = ((ICUResourceBundle) testcase.getObject("which")).getUInt();
 
        // create charset and encoder for each test case
        logln("Test not supported at this time");

    }

    /**
     * This follows ucnv.c method ucnv_detectUnicodeSignature() to detect the
     * start of the stream for example U+FEFF (the Unicode BOM/signature
     * character) that can be ignored.
     * 
     * Detects Unicode signature byte sequences at the start of the byte stream
     * and returns number of bytes of the BOM of the indicated Unicode charset.
     * 0 is returned when no Unicode signature is recognized.
     * 
     */

    private String detectUnicodeSignature(ByteBuffer source) {
        int signatureLength = 0; // number of bytes of the signature
        final int SIG_MAX_LEN = 5;
        String sigUniCharset = null; // states what unicode charset is the BOM
        int i = 0;

        /*
         * initial 0xa5 bytes: make sure that if we read <SIG_MAX_LEN bytes we
         * don't misdetect something
         */
        byte start[] = { (byte) 0xa5, (byte) 0xa5, (byte) 0xa5, (byte) 0xa5,
                (byte) 0xa5 };

        while (i < source.remaining() && i < SIG_MAX_LEN) {
            start[i] = source.get(i);
            i++;
        }

        if (start[0] == (byte) 0xFE && start[1] == (byte) 0xFF) {
            signatureLength = 2;
            sigUniCharset = "UTF-16BE";
            source.position(signatureLength);
            return sigUniCharset;
        } else if (start[0] == (byte) 0xFF && start[1] == (byte) 0xFE) {
            if (start[2] == (byte) 0x00 && start[3] == (byte) 0x00) {
                signatureLength = 4;
                sigUniCharset = "UTF-32LE";
                source.position(signatureLength);
                return sigUniCharset;
            } else {
                signatureLength = 2;
                sigUniCharset = "UTF-16LE";
                source.position(signatureLength);
                return sigUniCharset;
            }
        } else if (start[0] == (byte) 0xEF && start[1] == (byte) 0xBB
                && start[2] == (byte) 0xBF) {
            signatureLength = 3;
            sigUniCharset = "UTF-8";
            source.position(signatureLength);
            return sigUniCharset;
        } else if (start[0] == (byte) 0x00 && start[1] == (byte) 0x00
                && start[2] == (byte) 0xFE && start[3] == (byte) 0xFF) {
            signatureLength = 4;
            sigUniCharset = "UTF-32BE";
            source.position(signatureLength);
            return sigUniCharset;
        } else if (start[0] == (byte) 0x0E && start[1] == (byte) 0xFE
                && start[2] == (byte) 0xFF) {
            signatureLength = 3;
            sigUniCharset = "SCSU";
            source.position(signatureLength);
            return sigUniCharset;
        } else if (start[0] == (byte) 0xFB && start[1] == (byte) 0xEE
                && start[2] == (byte) 0x28) {
            signatureLength = 3;
            sigUniCharset = "BOCU-1";
            source.position(signatureLength);
            return sigUniCharset;
        } else if (start[0] == (byte) 0x2B && start[1] == (byte) 0x2F
                && start[2] == (byte) 0x76) {

            if (start[3] == (byte) 0x38 && start[4] == (byte) 0x2D) {
                signatureLength = 5;
                sigUniCharset = "UTF-7";
                source.position(signatureLength);
                return sigUniCharset;
            } else if (start[3] == (byte) 0x38 || start[3] == (byte) 0x39
                    || start[3] == (byte) 0x2B || start[3] == (byte) 0x2F) {
                signatureLength = 4;
                sigUniCharset = "UTF-7";
                source.position(signatureLength);
                return sigUniCharset;
            }
        } else if (start[0] == (byte) 0xDD && start[2] == (byte) 0x73
                && start[2] == (byte) 0x66 && start[3] == (byte) 0x73) {
            signatureLength = 4;
            sigUniCharset = "UTF-EBCDIC";
            source.position(signatureLength);
            return sigUniCharset;
        }

        /* no known Unicode signature byte sequence recognized */
        return null;
    }

    void printbytes(ByteBuffer buf, int pos) {
        int cur = buf.position();
        log(" (" + pos + ")==[");
        for (int i = 0; i < pos; i++) {
            log("(" + i + ")" + hex(buf.get(i) & 0xff) + " ");
        }
        log("]");
        buf.position(cur);
    }

    void printchar(CharBuffer buf, int pos) {
        int cur = buf.position();
        log(" (" + pos + ")==[");
        for (int i = 0; i < pos; i++) {
            log("(" + i + ")" + hex(buf.get(i)) + " ");
        }
        log("]");
        buf.position(cur);
    }

    private void checkResultsFromUnicode(ConversionCase cc, ByteBuffer source, ByteBuffer target) {
 
        int len = target.position();
        source.rewind();
        target.rewind();
        
        // remove any BOM signature before checking
        String BOM = detectUnicodeSignature(target);

        // test to see if the conversion matches actual results
        // remove any BOM signature before checking
        BOM =  detectUnicodeSignature(target);
        
        len = len-target.position();
        
        if (len != source.remaining()) {
            errln("Test failed: output does not match expected\n"); 
            logln("["+ cc.caseNr + "]:"+cc.charset+"\noutput=" );
            printbytes(target, len); 
            return;
        }
        for (int i = 0; i < source.remaining(); i++) {
            if (target.get() != source.get()) {
                errln("Test failed: output does not match expected\n"); 
                logln("["+ cc.caseNr + "]:"+cc.charset+"\noutput=" );
                printbytes(target, len); 
                return;
            }
        }
        logln("["+ cc.caseNr + "]:"+cc.charset);
        log("output=" ); 
        printbytes(target, len);  
        logln("\nPassed\n");
        return;
    }

    private void checkResultsToUnicode(ConversionCase cc, String source, CharBuffer target) {
 
        int len = target.position(); 
        target.rewind(); 

        // test to see if the conversion matches actual results
        if (len != source.length()) {
            errln("Test failed: output does not match expected\n"); 
            logln("["+ cc.caseNr + "]:"+cc.charset+"\noutput=" );
            printchar(target,len); 
            return;
        }
        for (int i = 0; i < source.length(); i++) {
            if ( ! (hex(target.get(i)).equals(hex(source.charAt(i)))) ) {
                errln("Test failed: output does not match expected\n"); 
                logln("["+ cc.caseNr + "]:"+cc.charset+"\noutput=" );
                printchar(target,len); 
                return;
            }
        }
        logln("["+ cc.caseNr + "]:"+cc.charset);
        log("output=" ); 
         printchar(target,len);  
        logln("\nPassed\n"); 
        return;
    }
}
