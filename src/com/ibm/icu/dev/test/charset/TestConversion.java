/*
 *******************************************************************************
 * Copyright (C) 2002-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Iterator;

import com.ibm.icu.charset.CharsetEncoderICU;
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
        int caseNr;                                             // testcase index   
        String option = null;                                   // callback options
        CodingErrorAction cbErrorAction = null;                 // callback action type
        CharBuffer toUnicodeResult = null;
        ByteBuffer fromUnicodeResult = null;
        
        // data retrieved from a test case conversion.txt
        String charset;                                         // charset
        String unicode;                                         // unicode string
        ByteBuffer bytes;                                       // byte
        int[] offsets;                                          // offsets
        boolean finalFlush;                                     // flush
        boolean fallbacks;                                      // fallback
        String outErrorCode;                                    // errorCode
        String cbopt;                                           // callback 
        
        // TestGetUnicodeSet variables
        String map;
        String mapnot;
        int which;
        
        String caseNrAsString() {
            return "[" + caseNr + "]";
        }
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
            int testToUnicode = 0;
            String testName = t.getName().toString();

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
                    TestGetUnicodeSet(testcase);
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

    
    // fromUnicode test worker functions --------------------------------------- 
    private void TestFromUnicode(DataMap testcase, int caseNr) {

        ConversionCase cc = new ConversionCase();
        
        try {
            // retrieve test case data
            cc.caseNr = caseNr;
            cc.charset = ((ICUResourceBundle) testcase.getObject("charset")).getString();
            cc.unicode = ((ICUResourceBundle) testcase.getObject("unicode")).getString();
            cc.bytes = ((ICUResourceBundle) testcase.getObject("bytes")).getBinary();
            cc.offsets = ((ICUResourceBundle) testcase.getObject("offsets")).getIntVector();
            cc.finalFlush = ((ICUResourceBundle) testcase.getObject("flush")).getUInt() != 0;
            cc.fallbacks = ((ICUResourceBundle) testcase.getObject("fallbacks")).getUInt() != 0;
            cc.outErrorCode = ((ICUResourceBundle) testcase.getObject("errorCode")).getString();
            cc.cbopt = ((ICUResourceBundle) testcase.getObject("callback")).getString();

        } catch (Exception e) {
            errln("Skipping test:");
            errln("error parsing conversion/toUnicode test case " + cc.caseNr);
            return;
        }

        // ----for debugging only
        logln("");
        logln("TestFromUnicode[" + caseNr + "] " + cc.charset + " ");
        logln("Unicode:   " + cc.unicode);
        logln("Bytes:    " + printbytes(cc.bytes, cc.bytes.limit()));
        ByteBuffer c = ByteBuffer.wrap(cc.cbopt.getBytes());
        logln("Callback: " + printbytes(c, c.limit()) + " (" + cc.cbopt + ")");
        logln("...............................................");

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
            cc.option = cc.cbErrorAction == null ? cc.cbopt : cc.cbopt
                    .substring(1);
            if (cc.option == null) {
                cc.option = null;
            }
        }
        FromUnicodeCase(cc);
    }

    
    private void FromUnicodeCase(ConversionCase cc) {

        // create charset encoder for conversion test
        CharsetProviderICU provider = new CharsetProviderICU();
        CharsetEncoder encoder = null;
        Charset charset = null;
        try {
            // if cc.charset starts with '*', obtain it from com/ibm/icu/dev/data/testdata
            charset = (cc.charset != null && cc.charset.length() > 0 && cc.charset.charAt(0) == '*')
                    ? (Charset) provider.charsetForName(cc.charset.substring(1), "../dev/data/testdata")
                    : (Charset) provider.charsetForName(cc.charset);
            encoder = (CharsetEncoder) charset.newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPLACE);
            encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            if (encoder instanceof CharsetEncoderICU) {
                ((CharsetEncoderICU)encoder).setFallbackUsed(cc.fallbacks);
                if (((CharsetEncoderICU)encoder).isFallbackUsed() != cc.fallbacks) {
                    errln("Fallback could not be set for " + cc.charset);
                }
            }
            
        } catch (Exception e) {
            // TODO implement loading of test data.
            if (skipIfBeforeICU(3,9,0)) {
                logln("Skipping test:(" + cc.charset + ") due to ICU Charset not supported at this time");
            } else {
                errln(cc.charset + " was not found");
            }
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
                                encoder.replaceWith(toByteArray(cc.cbopt));
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

        // testing by steps using charset.encoder(in,out,flush)
        int resultLength;
        boolean ok;
        String steps[][] = { { "0", "bulk" }, // must be first for offsets to be checked
                { "1", "step=1" }, { "3", "step=3" }, { "7", "step=7" } };
        int i, step;

        ok = true;

        for (i = 0; i < steps.length && ok; ++i) {
            step = Integer.parseInt(steps[i][0]);

            logln("Testing step:[" + step + "]");
            try {
                resultLength = stepFromUnicode(cc, encoder, step);
                ok = checkFromUnicode(cc, resultLength);
            } catch (Exception ex) {
                errln("Test failed: " + ex.getClass().getName() + " thrown: " + cc.charset+ " [" + cc.caseNr + "]");
                ex.printStackTrace(System.out);
                return;
            }

        }
        // testing by whole buffer using out = charset.encoder(in)
        while (ok && cc.finalFlush) {
            logln("Testing java API charset.encoder(in):");
            cc.fromUnicodeResult = null;
            ByteBuffer out = null;

            try {
                out = encoder.encode(CharBuffer.wrap(cc.unicode.toCharArray()));
                out.position(out.limit());
                if (out.limit() != out.capacity()) {
                    int pos = out.position();
                    byte[] temp = out.array();
                    out = ByteBuffer.allocate(temp.length * 4);
                    out.put(temp);
                    out.position(pos);
                    CoderResult cr = encoder.flush(out);
                    if (cr.isOverflow()) {
                        logln("Overflow error with flushing encoder");
                    }
                }
                cc.fromUnicodeResult = out;

                ok = checkFromUnicode(cc, out.limit());
                if (!ok) {
                    break;
                }
            } catch (Exception e) {
                //check the error code to see if it matches cc.errorCode
                logln("Encoder returned an error code");
                logln("ErrorCode expected is: " + cc.outErrorCode);
                logln("Error Result is: " + e.toString());
            }
            break;
        }
    }
    
    private int stepFromUnicode(ConversionCase cc, CharsetEncoder encoder, int step) {
        if (step < 0) {
            errln("Negative step size, test internal error.");
            return 0;
        }

        int sourceLen = cc.unicode.length();
        int targetLen = cc.bytes.capacity() + 20;  // for BOM, and to let failures produce excess output
        CharBuffer source = CharBuffer.wrap(cc.unicode.toCharArray());
        ByteBuffer target = ByteBuffer.allocate(targetLen);
        cc.fromUnicodeResult = null;
        encoder.reset();

        int currentSourceLimit;
        int currentTargetLimit;
        if (step > 0) {
            currentSourceLimit = Math.min(step, sourceLen);
            currentTargetLimit = Math.min(step, targetLen);
        } else {
            currentSourceLimit = sourceLen;
            currentTargetLimit = targetLen;
        }
        
        CoderResult cr = null;

        for (;;) {
            source.limit(currentSourceLimit);
            target.limit(currentTargetLimit);

            cr = encoder.encode(source, target, currentSourceLimit == sourceLen);
            
            if (cr.isUnderflow()) {
                if (currentSourceLimit == sourceLen) {
                    // Do a final flush for cleanup, then break out
                    // Encode loop, exits with cr==underflow in normal operation.
                    target.limit(targetLen);
                    cr = encoder.flush(target);
                    if (cr.isUnderflow()) {
                        // good
                    } else if (cr.isOverflow()) {
                        errln(cc.caseNrAsString() + " Flush is producing excessive output");
                    } else {
                        errln(cc.caseNrAsString() + " Flush operation failed.  CoderResult = \""
                                + cr.toString() + "\"");
                    }
                    break;
                }
                currentSourceLimit = Math.min(currentSourceLimit + step, sourceLen);
            } else if (cr.isOverflow()) {
                if (currentTargetLimit == targetLen) {
                    errln(cc.caseNrAsString() + " encode() is producing excessive output");
                    break;
                }
                currentTargetLimit = Math.min(currentTargetLimit + step, targetLen);
            } else {
                // check the error code to see if it matches cc.errorCode
                logln("Encoder returned an error code");
                logln("ErrorCode expected is: " + cc.outErrorCode);
                logln("Error Result is: " + cr.toString());
                break;
            }

        }
        
        cc.fromUnicodeResult = target;
        return target.position();
    }
    
    private boolean checkFromUnicode(ConversionCase cc, int resultLength) {
        return checkResultsFromUnicode(cc, cc.bytes, cc.fromUnicodeResult);
    }

    // toUnicode test worker functions ----------------------------------------- ***

    private void TestToUnicode(DataMap testcase, int caseNr) {
        // create Conversion case to store the test case data
        ConversionCase cc = new ConversionCase();

        try {
            // retrieve test case data
            cc.caseNr = caseNr;
            cc.charset = ((ICUResourceBundle) testcase.getObject("charset")).getString();
            cc.bytes = ((ICUResourceBundle) testcase.getObject("bytes")).getBinary();
            cc.unicode = ((ICUResourceBundle) testcase.getObject("unicode")).getString();
            cc.offsets = ((ICUResourceBundle) testcase.getObject("offsets")).getIntVector();
            cc.finalFlush = ((ICUResourceBundle) testcase.getObject("flush")).getUInt() != 0;
            cc.fallbacks = ((ICUResourceBundle) testcase.getObject("fallbacks")).getUInt() != 0;
            cc.outErrorCode = ((ICUResourceBundle) testcase.getObject("errorCode")).getString();
            cc.cbopt = ((ICUResourceBundle) testcase.getObject("callback")).getString();

        } catch (Exception e) {
            errln("Skipping test: error parsing conversion/toUnicode test case " + cc.caseNr);
            return;
        }

        // ----for debugging only
        logln("");
        logln("TestToUnicode[" + caseNr + "] " + cc.charset + " ");
        logln("Unicode:   " + hex(cc.unicode));
        logln("Bytes:    " + printbytes(cc.bytes, cc.bytes.limit()));
        ByteBuffer c = ByteBuffer.wrap(cc.cbopt.getBytes());
        logln("Callback: " + printbytes(c, c.limit()) + " (" + cc.cbopt + ")");
        logln("...............................................");

        // ----for debugging only

        // TODO: This test case is skipped due to limitation in java's API for
        // decoder replacement
        // { "ibm-1363", :bin{ a2aea2 }, "\u00a1\u001a", :intvector{ 0, 2 },
        // :int{1}, :int{0}, "", "?", :bin{""} }
        if (cc.caseNr == 63 && skipIfBeforeICU(3,9,0)) {
            logln("TestToUnicode[" + cc.caseNr + "] " + cc.charset);
            logln("Skipping test due to limitation in Java API - callback replacement value");
            return;
        }
        
        // process the retrieved test data case
        if (cc.offsets.length == 0) {
            cc.offsets = null;
        } else if (cc.offsets.length != cc.unicode.length()) {
            errln("Skipping test: toUnicode[" + cc.caseNr + "] unicode["
                    + cc.unicode.length() + "] and offsets["
                    + cc.offsets.length + "] must have the same length");
            return;
        }
        // check for the callback replacement value for unmappable
        // characters or malformed errors
        if (cc.cbopt.length() > 0) {
            switch ((cc.cbopt).charAt(0)) {
            case '?': // CALLBACK_SUBSTITUTE
                cc.cbErrorAction = CodingErrorAction.REPLACE;
                break;
            case '0': // CALLBACK_SKIP
                cc.cbErrorAction = CodingErrorAction.IGNORE;
                break;
            case '.': // CALLBACK_STOP
                cc.cbErrorAction = CodingErrorAction.REPORT;
                break;
            case '&': // CALLBACK_ESCAPE
                cc.cbErrorAction = CodingErrorAction.REPORT;
                break;
            default:
                cc.cbErrorAction = null;
                break;
            }
        }
        // check for any options for the callback value
        cc.option = cc.cbErrorAction == null ? null : cc.cbopt.substring(1);
        if (cc.option == null) {
            cc.option = null;
        }

        ToUnicodeCase(cc);

    }

    private void ToUnicodeCase(ConversionCase cc) {

        // create converter for charset and decoder for each test case
        CharsetProviderICU provider = new CharsetProviderICU();
        CharsetDecoder decoder = null;
        Charset charset = null;

        try {
            // if cc.charset starts with '*', obtain it from com/ibm/icu/dev/data/testdata
            charset = (cc.charset != null && cc.charset.length() > 0 && cc.charset.charAt(0) == '*')
                    ? (Charset) provider.charsetForName(cc.charset.substring(1), "../dev/data/testdata")
                    : (Charset) provider.charsetForName(cc.charset);
            decoder = (CharsetDecoder) charset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        } catch (Exception e) {
            // TODO implement loading of test data.
            if (skipIfBeforeICU(3,9,0)) {
                logln("Skipping test:(" + cc.charset + ") due to ICU Charset not supported at this time");
            } else {
                errln(cc.charset + " was not found");
            }
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

        //      Check the step to unicode    
        boolean ok;
        int resultLength;

        String steps[][] = { { "0", "bulk" }, // must be first for offsets to be checked
                { "1", "step=1" }, { "3", "step=3" }, { "7", "step=7" } };
        /* TODO: currently not supported test steps, getNext API is not supported for now  
         { "-1", "getNext" },
         { "-2", "toU(bulk)+getNext" },
         { "-3", "getNext+toU(bulk)" },
         { "-4", "toU(1)+getNext" },
         { "-5", "getNext+toU(1)" },
         { "-12", "toU(5)+getNext" },
         { "-13", "getNext+toU(5)" }};*/

        ok = true;
        int step;
        // testing by steps using the CoderResult cr = charset.decoder(in,out,flush) api
        for (int i = 0; i < steps.length && ok; ++i) {
            step = Integer.parseInt(steps[i][0]);

            if (step < 0 && !cc.finalFlush) {
                continue;
            }
            logln("Testing step:[" + step + "]");
            
            try {
                resultLength = stepToUnicode(cc, decoder, step);
                ok = checkToUnicode(cc, resultLength);
            } catch (Exception ex) {
                errln("Test failed: " + ex.getClass().getName() + " thrown: " + cc.charset+ " [" + cc.caseNr + "]");
                ex.printStackTrace(System.out);
                return;
            }
        }

        //testing the java's out = charset.decoder(in) api
        while (ok && cc.finalFlush) {
            logln("Testing java charset.decoder(in):");
            cc.toUnicodeResult = null;
            CharBuffer out = null;

            try {
                out = decoder.decode(ByteBuffer.wrap(cc.bytes.array()));
                out.position(out.limit());
                if (out.limit() < cc.unicode.length()) {
                    int pos = out.position();
                    char[] temp = out.array();
                    out = CharBuffer.allocate(cc.bytes.limit());
                    out.put(temp);
                    out.position(pos);
                    CoderResult cr = decoder.flush(out);
                    if (cr.isOverflow()) {
                        logln("Overflow error with flushing decodering");
                    }
                }

                cc.toUnicodeResult = out;

                ok = checkToUnicode(cc, out.limit());
                if (!ok) {
                    break;
                }
            } catch (Exception e) {
                //check the error code to see if it matches cc.errorCode
                logln("Decoder returned an error code");
                logln("ErrorCode expected is: " + cc.outErrorCode);
                logln("Error Result is: " + e.toString());
            }
            break;
        }

        return;
    }



    
    private int stepToUnicode(ConversionCase cc, CharsetDecoder decoder,
            int step)

    {
        ByteBuffer source;
        CharBuffer target;
        boolean flush = false;
        int sourceLen;
        source = cc.bytes;
        sourceLen = cc.bytes.limit();
        source.position(0);
        target = CharBuffer.allocate(cc.unicode.length() + 4);
        target.position(0);
        cc.toUnicodeResult = null;
        decoder.reset();

        if (step >= 0) {

            int iStep = step;
            int oStep = step;

            for (;;) {

                if (step != 0) {
                    source.limit((iStep <= sourceLen) ? iStep : sourceLen);
                    target.limit((oStep <= target.capacity()) ? oStep : target
                            .capacity());
                    flush = (cc.finalFlush && source.limit() == sourceLen);

                } else {
                    //bulk mode
                    source.limit(sourceLen);
                    target.limit(target.capacity());
                    flush = cc.finalFlush;
                }
                // convert 
                CoderResult cr = null;
                if (source.hasRemaining()) {

                    cr = decoder.decode(source, target, flush);
                    // check pointers and errors
                    if (cr.isOverflow()) {
                        // the partial target is filled, set a new limit, 
                        oStep = (target.position() + step);
                        target.limit((oStep < target.capacity()) ? oStep
                                : target.capacity());
                        if (target.limit() > target.capacity()) {
                            //target has reached its limit, an error occurred or test case has an error code
                            //check error code
                            logln("UnExpected error: Target Buffer is larger than capacity");
                            break;
                        }

                    } else if (cr.isError()) {
                        //check the error code to see if it matches cc.errorCode
                        logln("Decoder returned an error code");
                        logln("ErrorCode expected is: " + cc.outErrorCode);
                        logln("Error Result is: " + cr.toString());
                        break;
                    }

                } else {
                    if (source.limit() == sourceLen) {

                        cr = decoder.decode(source, target, true);

                        //due to limitation of the API we need to check for target limit for expected 
                        if (target.limit() != cc.unicode.length()) {
                            target.limit(cc.unicode.length());
                            cr = decoder.flush(target);
                            if (cr.isError()) {
                                errln("Flush operation failed");
                            }
                        }
                        break;
                    }
                }
                iStep += step;
                oStep += step;

            }

        }// if(step ==0)

        //--------------------------------------------------------------------------
        else /* step<0 */{
            /*
             * step==-1: call only ucnv_getNextUChar()
             * otherwise alternate between ucnv_toUnicode() and ucnv_getNextUChar()
             *   if step==-2 or -3, then give ucnv_toUnicode() the whole remaining input,
             *   else give it at most (-step-2)/2 bytes
             */

            for (;;) {
                // convert
                if ((step & 1) != 0 /* odd: -1, -3, -5, ... */) {

                    target.limit(target.position() < target.capacity() ? target
                            .position() + 1 : target.capacity());

                    // decode behavior is return to output target 1 character
                    CoderResult cr = null;

                    //similar to getNextUChar() , input is the whole string, while outputs only 1 character
                    source.limit(sourceLen);
                    while (target.position() != target.limit()
                            && source.hasRemaining()) {
                        cr = decoder.decode(source, target,
                                source.limit() == sourceLen);

                        if (cr.isOverflow()) {

                            if (target.limit() >= target.capacity()) {
                                // target has reached its limit, an error occurred 
                                logln("UnExpected error: Target Buffer is larger than capacity");
                                break;
                            } else {
                                //1 character has been consumed
                                target.limit(target.position() + 1);
                                break;
                            }
                        } else if (cr.isError()) {
                            logln("Decoder returned an error code");
                            logln("ErrorCode expected is: " + cc.outErrorCode);
                            logln("Error Result is: " + cr.toString());

                            cc.toUnicodeResult = target;
                            return target.position();
                        }

                        else {
                            // one character has been consumed
                            if (target.limit() == target.position()) {
                                target.limit(target.position() + 1);
                                break;
                            }
                        }

                    }
                    if (source.position() == sourceLen) {

                        // due to limitation of the API we need to check
                        // for target limit for expected
                        cr = decoder.decode(source, target, true);
                        if (target.position() != cc.unicode.length()) {

                            target.limit(cc.unicode.length());
                            cr = decoder.flush(target);
                            if (cr.isError()) {
                                errln("Flush operation failed");
                            }
                        }
                        break;
                    }
                    // alternate between -n-1 and -n but leave -1 alone
                    if (step < -1) {
                        ++step;
                    }
                } else {/* step is even */
                    // allow only one UChar output

                    target.limit(target.position() < target.capacity() ? target
                            .position() + 1 : target.capacity());
                    if (step == -2) {
                        source.limit(sourceLen);
                    } else {
                        source.limit(source.position() + (-step - 2) / 2);
                        if (source.limit() > sourceLen) {
                            source.limit(sourceLen);
                        }
                    }
                    CoderResult cr = decoder.decode(source, target, source
                            .limit() == sourceLen);
                    // check pointers and errors 
                    if (cr.isOverflow()) {
                        // one character has been consumed
                        if (target.limit() >= target.capacity()) {
                            // target has reached its limit, an error occurred
                            logln("Unexpected error: Target Buffer is larger than capacity");
                            break;
                        }
                    } else if (cr.isError()) {
                        logln("Decoder returned an error code");
                        logln("ErrorCode expected is: " + cc.outErrorCode);
                        logln("Error Result is: " + cr.toString());
                        break;
                    }

                    --step;
                }
            }
        }

        //--------------------------------------------------------------------------

        cc.toUnicodeResult = target;
        return target.position();
    }


   
    private boolean checkToUnicode(ConversionCase cc, int resultLength) {
        return checkResultsToUnicode(cc, cc.unicode, cc.toUnicodeResult);
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
        logln("TestGetUnicodeSet not supported at this time");

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

        while (i < source.limit() && i < SIG_MAX_LEN) {
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

    String printbytes(ByteBuffer buf, int pos) {
        int cur = buf.position();
        String res = " (" + pos + ")==[";
        for (int i = 0; i < pos; i++) {
            res += "(" + i + ")" + hex(buf.get(i) & 0xff).substring(2) + " ";
        }
        buf.position(cur);
        return res + "]";
    }

    String printchars(CharBuffer buf, int pos) {
        int cur = buf.position();
        String res = " (" + pos + ")==[";
        for (int i = 0; i < pos; i++) {
            res += "(" + i + ")" + hex(buf.get(i)) + " ";
        }
        buf.position(cur);
        return res + "]";
    }

    private boolean checkResultsFromUnicode(ConversionCase cc, ByteBuffer expected,
            ByteBuffer output) {

        boolean res = true;
        expected.rewind();
        output.limit(output.position());
        output.rewind();
        
        // remove any BOM signature before checking
        detectUnicodeSignature(output); // sets the position to after the BOM
        output = output.slice(); // removes anything before the current position

        if (output.limit() != expected.limit()) {
            errln("Test failed: output length does not match expected for charset: " + cc.charset
                    + " [" + cc.caseNr + "]");
            res = false;
        } else {
            while (output.hasRemaining()) {
                if (output.get() != expected.get()) {
                    errln("Test failed: output does not match expected for charset: " + cc.charset
                            + " [" + cc.caseNr + "]");
                    res = false;
                    break;
                }
            }
        }
        
        if (res) {
            logln("[" + cc.caseNr + "]:" + cc.charset);
            logln("Input:       " + printchars(CharBuffer.wrap(cc.unicode), cc.unicode.length()));
            logln("Output:      " + printbytes(output, output.limit()));
            logln("Expected:    " + printbytes(expected, expected.limit()));
            logln("Passed");
        }
        else {
            errln("[" + cc.caseNr + "]:" + cc.charset);
            errln("Input:       " + printchars(CharBuffer.wrap(cc.unicode), cc.unicode.length()));
            errln("Output:      " + printbytes(output, output.limit()));
            errln("Expected:    " + printbytes(expected, expected.limit()));
            errln("Failed");
        }
        return res;
    }

    private boolean checkResultsToUnicode(ConversionCase cc, String expected, CharBuffer output) {

        boolean res = true;
        output.limit(output.position());
        output.rewind();

        // test to see if the conversion matches actual results
        if (output.limit() != expected.length()) {
            errln("Test failed: output length does not match expected for charset: "+cc.charset+ " [" + cc.caseNr + "]");
            res = false;
        } else {
            for (int i = 0; i < expected.length(); i++) {
                if (output.get(i) != expected.charAt(i)) {
                    errln("Test failed: output does not match expected for charset: " + cc.charset
                            + " [" + cc.caseNr + "]");
                    res = false;
                    break;
                }
            }
        }
        
        if (res) {
            logln("[" + cc.caseNr + "]:" + cc.charset);
            logln("Input:       " + printbytes(cc.bytes, cc.bytes.limit()));
            logln("Output:      " + printchars(output, output.limit()));
            logln("Expected:    " + printchars(CharBuffer.wrap(expected), expected.length()));
            logln("Passed");
        }
        else {
            errln("[" + cc.caseNr + "]:" + cc.charset);
            errln("Input:       " + printbytes(cc.bytes, cc.bytes.limit()));
            errln("Output:      " + printchars(output, output.limit()));
            errln("Expected:    " + printchars(CharBuffer.wrap(expected), expected.length()));
            errln("Failed");
        }
        return res;
    }

    private byte[] toByteArray(String str) {
        byte[] ret = new byte[str.length()];
        for (int i = 0; i < ret.length; i++) {
            char ch = str.charAt(i);
            if (ch <= 0xFF) {
                ret[i] = (byte) ch;
            } else {
                throw new IllegalArgumentException(" byte value out of range: " + ch);
            }
        }
        return ret;
    }
}
