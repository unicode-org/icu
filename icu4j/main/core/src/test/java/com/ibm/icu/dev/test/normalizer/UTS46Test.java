// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
* Copyright (C) 2010-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.dev.test.normalizer;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.Normalizer2Impl.UTF16Plus;
import com.ibm.icu.impl.Punycode;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.util.ICUInputTooLongException;

/**
 * UTS #46 (IDNA2008) test.
 * @author Markus Scherer
 * @since 2010jul10
 */
@RunWith(JUnit4.class)
public class UTS46Test extends CoreTestFmwk {
    public UTS46Test() {
        int commonOptions=
            IDNA.USE_STD3_RULES|IDNA.CHECK_BIDI|
            IDNA.CHECK_CONTEXTJ|IDNA.CHECK_CONTEXTO;
        trans=IDNA.getUTS46Instance(commonOptions);
        nontrans=IDNA.getUTS46Instance(commonOptions|
                                       IDNA.NONTRANSITIONAL_TO_ASCII|IDNA.NONTRANSITIONAL_TO_UNICODE);
    }

    @Test
    public void TestAPI() {
        StringBuilder result=new StringBuilder();
        IDNA.Info info=new IDNA.Info();
        String input="www.eXample.cOm";
        String expected="www.example.com";
        trans.nameToASCII(input, result, info);
        if(info.hasErrors() || !UTF16Plus.equal(result, expected)) {
            errln(String.format("T.nameToASCII(www.example.com) info.errors=%s result matches=%b",
                                info.getErrors(), UTF16Plus.equal(result, expected)));
        }
        input="xn--bcher.de-65a";
        expected="xn--bcher\uFFFDde-65a";
        nontrans.labelToASCII(input, result, info);
        if( !info.getErrors().equals(EnumSet.of(IDNA.Error.LABEL_HAS_DOT, IDNA.Error.INVALID_ACE_LABEL)) ||
            !UTF16Plus.equal(result, expected)
        ) {
            errln(String.format("N.labelToASCII(label-with-dot) failed with errors %s",
                                info.getErrors()));
        }
        // Java API tests that are not parallel to C++ tests
        // because the C++ specifics (error codes etc.) do not apply here.
        String resultString=trans.nameToUnicode("fA\u00DF.de", result, info).toString();
        if(info.hasErrors() || !resultString.equals("fass.de")) {
            errln(String.format("T.nameToUnicode(fA\u00DF.de) info.errors=%s result matches=%b",
                                info.getErrors(), resultString.equals("fass.de")));
        }
        try {
            nontrans.labelToUnicode(result, result, info);
            errln("N.labelToUnicode(result, result) did not throw an Exception");
        } catch(Exception e) {
            // as expected (should be an IllegalArgumentException, or an ICU version of it)
        }
    }

    @Test
    public void TestNotSTD3() {
        IDNA not3=IDNA.getUTS46Instance(IDNA.CHECK_BIDI);
        String input="\u0000A_2+2=4\n.e\u00DFen.net";
        StringBuilder result=new StringBuilder();
        IDNA.Info info=new IDNA.Info();
        if( !not3.nameToUnicode(input, result, info).toString().equals("\u0000a_2+2=4\n.essen.net") ||
            info.hasErrors()
        ) {
            errln(String.format("notSTD3.nameToUnicode(non-LDH ASCII) unexpected errors %s string %s",
                                info.getErrors(), prettify(result.toString())));
        }
        // A space (BiDi class WS) is not allowed in a BiDi domain name.
        input="a z.xn--4db.edu";
        not3.nameToASCII(input, result, info);
        if(!UTF16Plus.equal(result, input) || !info.getErrors().equals(EnumSet.of(IDNA.Error.BIDI))) {
            errln("notSTD3.nameToASCII(ASCII-with-space.alef.edu) failed");
        }
    }

    @Test
    public void TestInvalidPunycodeDigits() {
        IDNA idna=IDNA.getUTS46Instance(0);
        StringBuilder result=new StringBuilder();
        IDNA.Info info=new IDNA.Info();
        idna.nameToUnicode("xn--pleP", result, info);  // P=U+0050
        assertFalse("nameToUnicode() should succeed",
                info.getErrors().contains(IDNA.Error.PUNYCODE));
        assertEquals("normal result", "ᔼᔴ", result.toString());

        info=new IDNA.Info();
        idna.nameToUnicode("xn--pleѐ", result, info);  // ends with non-ASCII U+0450
        assertTrue("nameToUnicode() should detect non-ASCII",
                info.getErrors().contains(IDNA.Error.PUNYCODE));

        // Test with ASCII characters adjacent to LDH.
        info=new IDNA.Info();
        idna.nameToUnicode("xn--PLE/", result, info);
        assertTrue("nameToUnicode() should detect '/'",
                info.getErrors().contains(IDNA.Error.PUNYCODE));

        info=new IDNA.Info();
        idna.nameToUnicode("xn--ple:", result, info);
        assertTrue("nameToUnicode() should detect ':'",
                info.getErrors().contains(IDNA.Error.PUNYCODE));

        info=new IDNA.Info();
        idna.nameToUnicode("xn--ple@", result, info);
        assertTrue("nameToUnicode() should detect '@'",
                info.getErrors().contains(IDNA.Error.PUNYCODE));

        info=new IDNA.Info();
        idna.nameToUnicode("xn--ple[", result, info);
        assertTrue("nameToUnicode() should detect '['",
                info.getErrors().contains(IDNA.Error.PUNYCODE));

        info=new IDNA.Info();
        idna.nameToUnicode("xn--ple`", result, info);
        assertTrue("nameToUnicode() should detect '`'",
                info.getErrors().contains(IDNA.Error.PUNYCODE));

        info=new IDNA.Info();
        idna.nameToUnicode("xn--ple{", result, info);
        assertTrue("nameToUnicode() should detect '{'",
                info.getErrors().contains(IDNA.Error.PUNYCODE));
    }

    @Test
    public void TestACELabelEdgeCases() {
        // In IDNA2008, these labels fail the round-trip validation from comparing
        // the ToUnicode input with the back-to-ToASCII output.
        IDNA idna=IDNA.getUTS46Instance(0);
        StringBuilder result=new StringBuilder();
        IDNA.Info info=new IDNA.Info();
        idna.labelToUnicode("xn--", result, info);
        assertTrue("empty xn--", info.getErrors().contains(IDNA.Error.INVALID_ACE_LABEL));

        info=new IDNA.Info();
        idna.labelToUnicode("xN--ASCII-", result, info);
        assertTrue("nothing but ASCII", info.getErrors().contains(IDNA.Error.INVALID_ACE_LABEL));

        // Different error: The Punycode decoding procedure does not consume the last delimiter
        // if it is right after the xn-- so the main decoding loop fails because the hyphen
        // is not a valid Punycode digit.
        info=new IDNA.Info();
        idna.labelToUnicode("Xn---", result, info);
        assertTrue("empty Xn---", info.getErrors().contains(IDNA.Error.PUNYCODE));
    }

    @Test
    public void TestTooLong() {
        // ICU-13727: Limit input length for n^2 algorithm
        // where well-formed strings are at most 59 characters long.
        int count = 50000;
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; ++i) {
            sb.append('a');
        }
        try {
            Punycode.encode(sb, null);
            fail("encode: expected an exception for too-long input");
        } catch(ICUInputTooLongException expected) {
        } catch(StringPrepParseException e) {
            fail("encode: unexpected StringPrepParseException for too-long input: " + e);
        }
        try {
            Punycode.decode(sb, null);
            fail("decode: expected an exception for too-long input");
        } catch(ICUInputTooLongException expected) {
        } catch(StringPrepParseException e) {
            fail("decode: unexpected StringPrepParseException for too-long input: " + e);
        }
    }

    private static final Map<String, IDNA.Error> errorNamesToErrors;
    static {
        errorNamesToErrors=new TreeMap<>();
        errorNamesToErrors.put("UIDNA_ERROR_EMPTY_LABEL", IDNA.Error.EMPTY_LABEL);
        errorNamesToErrors.put("UIDNA_ERROR_LABEL_TOO_LONG", IDNA.Error.LABEL_TOO_LONG);
        errorNamesToErrors.put("UIDNA_ERROR_DOMAIN_NAME_TOO_LONG", IDNA.Error.DOMAIN_NAME_TOO_LONG);
        errorNamesToErrors.put("UIDNA_ERROR_LEADING_HYPHEN", IDNA.Error.LEADING_HYPHEN);
        errorNamesToErrors.put("UIDNA_ERROR_TRAILING_HYPHEN", IDNA.Error.TRAILING_HYPHEN);
        errorNamesToErrors.put("UIDNA_ERROR_HYPHEN_3_4", IDNA.Error.HYPHEN_3_4);
        errorNamesToErrors.put("UIDNA_ERROR_LEADING_COMBINING_MARK", IDNA.Error.LEADING_COMBINING_MARK);
        errorNamesToErrors.put("UIDNA_ERROR_DISALLOWED", IDNA.Error.DISALLOWED);
        errorNamesToErrors.put("UIDNA_ERROR_PUNYCODE", IDNA.Error.PUNYCODE);
        errorNamesToErrors.put("UIDNA_ERROR_LABEL_HAS_DOT", IDNA.Error.LABEL_HAS_DOT);
        errorNamesToErrors.put("UIDNA_ERROR_INVALID_ACE_LABEL", IDNA.Error.INVALID_ACE_LABEL);
        errorNamesToErrors.put("UIDNA_ERROR_BIDI", IDNA.Error.BIDI);
        errorNamesToErrors.put("UIDNA_ERROR_CONTEXTJ", IDNA.Error.CONTEXTJ);
        errorNamesToErrors.put("UIDNA_ERROR_CONTEXTO_PUNCTUATION", IDNA.Error.CONTEXTO_PUNCTUATION);
        errorNamesToErrors.put("UIDNA_ERROR_CONTEXTO_DIGITS", IDNA.Error.CONTEXTO_DIGITS);
    }

    private static final class TestCase {
        private TestCase() {
            errors=EnumSet.noneOf(IDNA.Error.class);
        }
        private void set(String[] data) {
            s=data[0];
            o=data[1];
            u=data[2];
            errors.clear();
            if(data[3].length()!=0) {
                for(String e: data[3].split("\\|")) {
                    errors.add(errorNamesToErrors.get(e));
                }
            }
        }
        // Input string and options string (Nontransitional/Transitional/Both).
        private String s, o;
        // Expected Unicode result string.
        private String u;
        private EnumSet<IDNA.Error> errors;
    };

    private static final String testCases[][]={
        { "www.eXample.cOm", "B",  // all ASCII
          "www.example.com", "" },
        { "B\u00FCcher.de", "B",  // u-umlaut
          "b\u00FCcher.de", "" },
        { "\u00D6BB", "B",  // O-umlaut
          "\u00F6bb", "" },
        { "fa\u00DF.de", "N",  // sharp s
          "fa\u00DF.de", "" },
        { "fa\u00DF.de", "T",  // sharp s
          "fass.de", "" },
        { "XN--fA-hia.dE", "B",  // sharp s in Punycode
          "fa\u00DF.de", "" },
        { "\u03B2\u03CC\u03BB\u03BF\u03C2.com", "N",  // Greek with final sigma
          "\u03B2\u03CC\u03BB\u03BF\u03C2.com", "" },
        { "\u03B2\u03CC\u03BB\u03BF\u03C2.com", "T",  // Greek with final sigma
          "\u03B2\u03CC\u03BB\u03BF\u03C3.com", "" },
        { "xn--nxasmm1c", "B",  // Greek with final sigma in Punycode
          "\u03B2\u03CC\u03BB\u03BF\u03C2", "" },
        { "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "N",  // "Sri" in "Sri Lanka" has a ZWJ
          "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "" },
        { "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "T",  // "Sri" in "Sri Lanka" has a ZWJ
          "www.\u0DC1\u0DCA\u0DBB\u0DD3.com", "" },
        { "www.xn--10cl1a0b660p.com", "B",  // "Sri" in Punycode
          "www.\u0DC1\u0DCA\u200D\u0DBB\u0DD3.com", "" },
        { "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", "N",  // ZWNJ
          "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", "" },
        { "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC", "T",  // ZWNJ
          "\u0646\u0627\u0645\u0647\u0627\u06CC", "" },
        { "xn--mgba3gch31f060k.com", "B",  // ZWNJ in Punycode
          "\u0646\u0627\u0645\u0647\u200C\u0627\u06CC.com", "" },
        { "a.b\uFF0Ec\u3002d\uFF61", "B",
          "a.b.c.d.", "" },
        { "U\u0308.xn--tda", "B",  // U+umlaut.u-umlaut
          "\u00FC.\u00FC", "" },
        { "xn--u-ccb", "B",  // u+umlaut in Punycode
          "xn--u-ccb\uFFFD", "UIDNA_ERROR_INVALID_ACE_LABEL" },
        { "a\u2488com", "B",  // contains 1-dot
          "a\uFFFDcom", "UIDNA_ERROR_DISALLOWED" },
        { "xn--a-ecp.ru", "B",  // contains 1-dot in Punycode
          "xn--a-ecp\uFFFD.ru", "UIDNA_ERROR_INVALID_ACE_LABEL" },
        { "xn--0.pt", "B",  // invalid Punycode
          "xn--0\uFFFD.pt", "UIDNA_ERROR_PUNYCODE" },
        { "xn--a.pt", "B",  // U+0080
          "xn--a\uFFFD.pt", "UIDNA_ERROR_INVALID_ACE_LABEL" },
        { "xn--a-\u00C4.pt", "B",  // invalid Punycode
          "xn--a-\u00E4.pt", "UIDNA_ERROR_PUNYCODE" },
        { "\u65E5\u672C\u8A9E\u3002\uFF2A\uFF30", "B",  // Japanese with fullwidth ".jp"
          "\u65E5\u672C\u8A9E.jp", "" },
        { "\u2615", "B", "\u2615", "" },  // Unicode 4.0 HOT BEVERAGE
        // many deviation characters, test the special mapping code
        { "1.a\u00DF\u200C\u200Db\u200C\u200Dc\u00DF\u00DF\u00DF\u00DFd"+
          "\u03C2\u03C3\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFe"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFx"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFy"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u0302\u00DFz", "N",
          "1.a\u00DF\u200C\u200Db\u200C\u200Dc\u00DF\u00DF\u00DF\u00DFd"+
          "\u03C2\u03C3\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFe"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFx"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFy"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u0302\u00DFz",
          "UIDNA_ERROR_LABEL_TOO_LONG|UIDNA_ERROR_CONTEXTJ" },
        { "1.a\u00DF\u200C\u200Db\u200C\u200Dc\u00DF\u00DF\u00DF\u00DFd"+
          "\u03C2\u03C3\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFe"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFx"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DFy"+
          "\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u00DF\u0302\u00DFz", "T",
          "1.assbcssssssssd"+
          "\u03C3\u03C3sssssssssssssssse"+
          "ssssssssssssssssssssx"+
          "ssssssssssssssssssssy"+
          "sssssssssssssss\u015Dssz", "UIDNA_ERROR_LABEL_TOO_LONG" },
        // "xn--bss" with deviation characters
        { "\u200Cx\u200Dn\u200C-\u200D-b\u00DF", "N",
          "\u200Cx\u200Dn\u200C-\u200D-b\u00DF", "UIDNA_ERROR_CONTEXTJ" },
        { "\u200Cx\u200Dn\u200C-\u200D-b\u00DF", "T",
          "\u5919", "" },
        // "xn--bssffl" written as:
        // 02E3 MODIFIER LETTER SMALL X
        // 034F COMBINING GRAPHEME JOINER (ignored)
        // 2115 DOUBLE-STRUCK CAPITAL N
        // 200B ZERO WIDTH SPACE (ignored)
        // FE63 SMALL HYPHEN-MINUS
        // 00AD SOFT HYPHEN (ignored)
        // FF0D FULLWIDTH HYPHEN-MINUS
        // 180C MONGOLIAN FREE VARIATION SELECTOR TWO (ignored)
        // 212C SCRIPT CAPITAL B
        // FE00 VARIATION SELECTOR-1 (ignored)
        // 017F LATIN SMALL LETTER LONG S
        // 2064 INVISIBLE PLUS (ignored)
        // 1D530 MATHEMATICAL FRAKTUR SMALL S
        // E01EF VARIATION SELECTOR-256 (ignored)
        // FB04 LATIN SMALL LIGATURE FFL
        { "\u02E3\u034F\u2115\u200B\uFE63\u00AD\uFF0D\u180C"+
          "\u212C\uFE00\u017F\u2064"+"\uD835\uDD30\uDB40\uDDEF"/*1D530 E01EF*/+"\uFB04", "B",
          "\u5921\u591E\u591C\u5919", "" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901", "" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901.", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901.", "" },
        // Domain name >256 characters, forces slow path in UTF-8 processing.
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "12345678901234567890123456789012345678901234567890123456789012", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "12345678901234567890123456789012345678901234567890123456789012",
          "UIDNA_ERROR_DOMAIN_NAME_TOO_LONG" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789\u05D0", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789\u05D0",
          "UIDNA_ERROR_DOMAIN_NAME_TOO_LONG|UIDNA_ERROR_BIDI" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901234."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901234."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890",
          "UIDNA_ERROR_LABEL_TOO_LONG" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901234."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890.", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901234."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890.",
          "UIDNA_ERROR_LABEL_TOO_LONG" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901234."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901234."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901",
          "UIDNA_ERROR_LABEL_TOO_LONG|UIDNA_ERROR_DOMAIN_NAME_TOO_LONG" },
        // label length 63: xn--1234567890123456789012345678901234567890123456789012345-9te
        { "\u00E41234567890123456789012345678901234567890123456789012345", "B",
          "\u00E41234567890123456789012345678901234567890123456789012345", "" },
        { "1234567890\u00E41234567890123456789012345678901234567890123456", "B",
          "1234567890\u00E41234567890123456789012345678901234567890123456", "UIDNA_ERROR_LABEL_TOO_LONG" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E4123456789012345678901234567890123456789012345."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E4123456789012345678901234567890123456789012345."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901", "" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E4123456789012345678901234567890123456789012345."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901.", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E4123456789012345678901234567890123456789012345."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901.", "" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E4123456789012345678901234567890123456789012345."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "12345678901234567890123456789012345678901234567890123456789012", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E4123456789012345678901234567890123456789012345."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "12345678901234567890123456789012345678901234567890123456789012",
          "UIDNA_ERROR_DOMAIN_NAME_TOO_LONG" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E41234567890123456789012345678901234567890123456."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E41234567890123456789012345678901234567890123456."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890",
          "UIDNA_ERROR_LABEL_TOO_LONG" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E41234567890123456789012345678901234567890123456."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890.", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E41234567890123456789012345678901234567890123456."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "123456789012345678901234567890123456789012345678901234567890.",
          "UIDNA_ERROR_LABEL_TOO_LONG" },
        { "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E41234567890123456789012345678901234567890123456."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901", "B",
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890\u00E41234567890123456789012345678901234567890123456."+
          "123456789012345678901234567890123456789012345678901234567890123."+
          "1234567890123456789012345678901234567890123456789012345678901",
          "UIDNA_ERROR_LABEL_TOO_LONG|UIDNA_ERROR_DOMAIN_NAME_TOO_LONG" },
        // hyphen errors and empty-label errors
        // Ticket #10883: ToUnicode also checks for empty labels.
        { ".", "B", ".", "UIDNA_ERROR_EMPTY_LABEL" },
        { "\uFF0E", "B", ".", "UIDNA_ERROR_EMPTY_LABEL" },
        // "xn---q----jra"=="-q--a-umlaut-"
        { "a.b..-q--a-.e", "B", "a.b..-q--a-.e",
          "UIDNA_ERROR_EMPTY_LABEL|UIDNA_ERROR_LEADING_HYPHEN|UIDNA_ERROR_TRAILING_HYPHEN|"+
          "UIDNA_ERROR_HYPHEN_3_4" },
        { "a.b..-q--\u00E4-.e", "B", "a.b..-q--\u00E4-.e",
          "UIDNA_ERROR_EMPTY_LABEL|UIDNA_ERROR_LEADING_HYPHEN|UIDNA_ERROR_TRAILING_HYPHEN|"+
          "UIDNA_ERROR_HYPHEN_3_4" },
        { "a.b..xn---q----jra.e", "B", "a.b..-q--\u00E4-.e",
          "UIDNA_ERROR_EMPTY_LABEL|UIDNA_ERROR_LEADING_HYPHEN|UIDNA_ERROR_TRAILING_HYPHEN|"+
          "UIDNA_ERROR_HYPHEN_3_4" },
        { "a..c", "B", "a..c", "UIDNA_ERROR_EMPTY_LABEL" },
        { "a.xn--.c", "B", "a.xn--\uFFFD.c", "UIDNA_ERROR_INVALID_ACE_LABEL" },
        { "a.-b.", "B", "a.-b.", "UIDNA_ERROR_LEADING_HYPHEN" },
        { "a.b-.c", "B", "a.b-.c", "UIDNA_ERROR_TRAILING_HYPHEN" },
        { "a.-.c", "B", "a.-.c", "UIDNA_ERROR_LEADING_HYPHEN|UIDNA_ERROR_TRAILING_HYPHEN" },
        { "a.bc--de.f", "B", "a.bc--de.f", "UIDNA_ERROR_HYPHEN_3_4" },
        { "\u00E4.\u00AD.c", "B", "\u00E4..c", "UIDNA_ERROR_EMPTY_LABEL" },
        { "\u00E4.xn--.c", "B", "\u00E4.xn--\uFFFD.c", "UIDNA_ERROR_INVALID_ACE_LABEL" },
        { "\u00E4.-b.", "B", "\u00E4.-b.", "UIDNA_ERROR_LEADING_HYPHEN" },
        { "\u00E4.b-.c", "B", "\u00E4.b-.c", "UIDNA_ERROR_TRAILING_HYPHEN" },
        { "\u00E4.-.c", "B", "\u00E4.-.c", "UIDNA_ERROR_LEADING_HYPHEN|UIDNA_ERROR_TRAILING_HYPHEN" },
        { "\u00E4.bc--de.f", "B", "\u00E4.bc--de.f", "UIDNA_ERROR_HYPHEN_3_4" },
        { "a.b.\u0308c.d", "B", "a.b.\uFFFDc.d", "UIDNA_ERROR_LEADING_COMBINING_MARK" },
        { "a.b.xn--c-bcb.d", "B",
          "a.b.xn--c-bcb\uFFFD.d", "UIDNA_ERROR_LEADING_COMBINING_MARK|UIDNA_ERROR_INVALID_ACE_LABEL" },
        // BiDi
        { "A0", "B", "a0", "" },
        { "0A", "B", "0a", "" },  // all-LTR is ok to start with a digit (EN)
        { "0A.\u05D0", "B",  // ASCII label does not start with L/R/AL
          "0a.\u05D0", "UIDNA_ERROR_BIDI" },
        { "c.xn--0-eha.xn--4db", "B",  // 2nd label does not start with L/R/AL
          "c.0\u00FC.\u05D0", "UIDNA_ERROR_BIDI" },
        { "b-.\u05D0", "B",  // label does not end with L/EN
          "b-.\u05D0", "UIDNA_ERROR_TRAILING_HYPHEN|UIDNA_ERROR_BIDI" },
        { "d.xn----dha.xn--4db", "B",  // 2nd label does not end with L/EN
          "d.\u00FC-.\u05D0", "UIDNA_ERROR_TRAILING_HYPHEN|UIDNA_ERROR_BIDI" },
        { "a\u05D0", "B", "a\u05D0", "UIDNA_ERROR_BIDI" },  // first dir != last dir
        { "\u05D0\u05C7", "B", "\u05D0\u05C7", "" },
        { "\u05D09\u05C7", "B", "\u05D09\u05C7", "" },
        { "\u05D0a\u05C7", "B", "\u05D0a\u05C7", "UIDNA_ERROR_BIDI" },  // first dir != last dir
        { "\u05D0\u05EA", "B", "\u05D0\u05EA", "" },
        { "\u05D0\u05F3\u05EA", "B", "\u05D0\u05F3\u05EA", "" },
        { "a\u05D0Tz", "B", "a\u05D0tz", "UIDNA_ERROR_BIDI" },  // mixed dir
        { "\u05D0T\u05EA", "B", "\u05D0t\u05EA", "UIDNA_ERROR_BIDI" },  // mixed dir
        { "\u05D07\u05EA", "B", "\u05D07\u05EA", "" },
        { "\u05D0\u0667\u05EA", "B", "\u05D0\u0667\u05EA", "" },  // Arabic 7 in the middle
        { "a7\u0667z", "B", "a7\u0667z", "UIDNA_ERROR_BIDI" },  // AN digit in LTR
        { "a7\u0667", "B", "a7\u0667", "UIDNA_ERROR_BIDI" },  // AN digit in LTR
        { "\u05D07\u0667\u05EA", "B",  // mixed EN/AN digits in RTL
          "\u05D07\u0667\u05EA", "UIDNA_ERROR_BIDI" },
        { "\u05D07\u0667", "B",  // mixed EN/AN digits in RTL
          "\u05D07\u0667", "UIDNA_ERROR_BIDI" },
        // ZWJ
        { "\u0BB9\u0BCD\u200D", "N", "\u0BB9\u0BCD\u200D", "" },  // Virama+ZWJ
        { "\u0BB9\u200D", "N", "\u0BB9\u200D", "UIDNA_ERROR_CONTEXTJ" },  // no Virama
        { "\u200D", "N", "\u200D", "UIDNA_ERROR_CONTEXTJ" },  // no Virama
        // ZWNJ
        { "\u0BB9\u0BCD\u200C", "N", "\u0BB9\u0BCD\u200C", "" },  // Virama+ZWNJ
        { "\u0BB9\u200C", "N", "\u0BB9\u200C", "UIDNA_ERROR_CONTEXTJ" },  // no Virama
        { "\u200C", "N", "\u200C", "UIDNA_ERROR_CONTEXTJ" },  // no Virama
        { "\u0644\u0670\u200C\u06ED\u06EF", "N",  // Joining types D T ZWNJ T R
          "\u0644\u0670\u200C\u06ED\u06EF", "" },
        { "\u0644\u0670\u200C\u06EF", "N",  // D T ZWNJ R
          "\u0644\u0670\u200C\u06EF", "" },
        { "\u0644\u200C\u06ED\u06EF", "N",  // D ZWNJ T R
          "\u0644\u200C\u06ED\u06EF", "" },
        { "\u0644\u200C\u06EF", "N",  // D ZWNJ R
          "\u0644\u200C\u06EF", "" },
        { "\u0644\u0670\u200C\u06ED", "N",  // D T ZWNJ T
          "\u0644\u0670\u200C\u06ED", "UIDNA_ERROR_BIDI|UIDNA_ERROR_CONTEXTJ" },
        { "\u06EF\u200C\u06EF", "N",  // R ZWNJ R
          "\u06EF\u200C\u06EF", "UIDNA_ERROR_CONTEXTJ" },
        { "\u0644\u200C", "N",  // D ZWNJ
          "\u0644\u200C", "UIDNA_ERROR_BIDI|UIDNA_ERROR_CONTEXTJ" },
        { "\u0660\u0661", "B",  // Arabic-Indic Digits alone
          "\u0660\u0661", "UIDNA_ERROR_BIDI" },
        { "\u06F0\u06F1", "B",  // Extended Arabic-Indic Digits alone
          "\u06F0\u06F1", "" },
        { "\u0660\u06F1", "B",  // Mixed Arabic-Indic Digits
          "\u0660\u06F1", "UIDNA_ERROR_CONTEXTO_DIGITS|UIDNA_ERROR_BIDI" },
        // All of the CONTEXTO "Would otherwise have been DISALLOWED" characters
        // in their correct contexts,
        // then each in incorrect context.
        { "l\u00B7l\u4E00\u0375\u03B1\u05D0\u05F3\u05F4\u30FB", "B",
          "l\u00B7l\u4E00\u0375\u03B1\u05D0\u05F3\u05F4\u30FB", "UIDNA_ERROR_BIDI" },
        { "l\u00B7", "B",
          "l\u00B7", "UIDNA_ERROR_CONTEXTO_PUNCTUATION" },
        { "\u00B7l", "B",
          "\u00B7l", "UIDNA_ERROR_CONTEXTO_PUNCTUATION" },
        { "\u0375", "B",
          "\u0375", "UIDNA_ERROR_CONTEXTO_PUNCTUATION" },
        { "\u03B1\u05F3", "B",
          "\u03B1\u05F3", "UIDNA_ERROR_CONTEXTO_PUNCTUATION|UIDNA_ERROR_BIDI" },
        { "\u05F4", "B",
          "\u05F4", "UIDNA_ERROR_CONTEXTO_PUNCTUATION" },
        { "l\u30FB", "B",
          "l\u30FB", "UIDNA_ERROR_CONTEXTO_PUNCTUATION" },
        // { "", "B",
        //   "", "" },
    };

    @Test
    public void TestSomeCases() {
        StringBuilder aT=new StringBuilder(), uT=new StringBuilder();
        StringBuilder aN=new StringBuilder(), uN=new StringBuilder();
        IDNA.Info aTInfo=new IDNA.Info(), uTInfo=new IDNA.Info();
        IDNA.Info aNInfo=new IDNA.Info(), uNInfo=new IDNA.Info();

        StringBuilder aTuN=new StringBuilder(), uTaN=new StringBuilder();
        StringBuilder aNuN=new StringBuilder(), uNaN=new StringBuilder();
        IDNA.Info aTuNInfo=new IDNA.Info(), uTaNInfo=new IDNA.Info();
        IDNA.Info aNuNInfo=new IDNA.Info(), uNaNInfo=new IDNA.Info();

        StringBuilder aTL=new StringBuilder(), uTL=new StringBuilder();
        StringBuilder aNL=new StringBuilder(), uNL=new StringBuilder();
        IDNA.Info aTLInfo=new IDNA.Info(), uTLInfo=new IDNA.Info();
        IDNA.Info aNLInfo=new IDNA.Info(), uNLInfo=new IDNA.Info();

        EnumSet<IDNA.Error> uniErrors=EnumSet.noneOf(IDNA.Error.class);

        TestCase testCase=new TestCase();
        int i;
        for(i=0; i<testCases.length; ++i) {
            testCase.set(testCases[i]);
            String input=testCase.s;
            String expected=testCase.u;
            // ToASCII/ToUnicode, transitional/nontransitional
            try {
                trans.nameToASCII(input, aT, aTInfo);
                trans.nameToUnicode(input, uT, uTInfo);
                nontrans.nameToASCII(input, aN, aNInfo);
                nontrans.nameToUnicode(input, uN, uNInfo);
            } catch(Exception e) {
                errln(String.format("first-level processing [%d/%s] %s - %s",
                                    i, testCase.o, testCase.s, e));
                continue;
            }
            // ToUnicode does not set length-overflow errors.
            uniErrors.clear();
            uniErrors.addAll(testCase.errors);
            uniErrors.removeAll(lengthOverflowErrors);
            char mode=testCase.o.charAt(0);
            if(mode=='B' || mode=='N') {
                if(!sameErrors(uNInfo, uniErrors)) {
                    errln(String.format("N.nameToUnicode([%d] %s) unexpected errors %s",
                                        i, testCase.s, uNInfo.getErrors()));
                    continue;
                }
                if(!UTF16Plus.equal(uN, expected)) {
                    errln(String.format("N.nameToUnicode([%d] %s) unexpected string %s",
                                        i, testCase.s, prettify(uN.toString())));
                    continue;
                }
                if(!sameErrors(aNInfo, testCase.errors)) {
                    errln(String.format("N.nameToASCII([%d] %s) unexpected errors %s",
                                        i, testCase.s, aNInfo.getErrors()));
                    continue;
                }
            }
            if(mode=='B' || mode=='T') {
                if(!sameErrors(uTInfo, uniErrors)) {
                    errln(String.format("T.nameToUnicode([%d] %s) unexpected errors %s",
                                        i, testCase.s, uTInfo.getErrors()));
                    continue;
                }
                if(!UTF16Plus.equal(uT, expected)) {
                    errln(String.format("T.nameToUnicode([%d] %s) unexpected string %s",
                                        i, testCase.s, prettify(uT.toString())));
                    continue;
                }
                if(!sameErrors(aTInfo, testCase.errors)) {
                    errln(String.format("T.nameToASCII([%d] %s) unexpected errors %s",
                                        i, testCase.s, aTInfo.getErrors()));
                    continue;
                }
            }
            // ToASCII is all-ASCII if no severe errors
            if(!hasCertainErrors(aNInfo, severeErrors) && !isASCII(aN)) {
                errln(String.format("N.nameToASCII([%d] %s) (errors %s) result is not ASCII %s",
                                    i, testCase.s, aNInfo.getErrors(), prettify(aN.toString())));
                continue;
            }
            if(!hasCertainErrors(aTInfo, severeErrors) && !isASCII(aT)) {
                errln(String.format("T.nameToASCII([%d] %s) (errors %s) result is not ASCII %s",
                                    i, testCase.s, aTInfo.getErrors(), prettify(aT.toString())));
                continue;
            }
            if(isVerbose()) {
                char m= mode=='B' ? mode : 'N';
                logln(String.format("%c.nameToASCII([%d] %s) (errors %s) result string: %s",
                                    m, i, testCase.s, aNInfo.getErrors(), prettify(aN.toString())));
                if(mode!='B') {
                    logln(String.format("T.nameToASCII([%d] %s) (errors %s) result string: %s",
                                        i, testCase.s, aTInfo.getErrors(), prettify(aT.toString())));
                }
            }
            // second-level processing
            try {
                nontrans.nameToUnicode(aT, aTuN, aTuNInfo);
                nontrans.nameToASCII(uT, uTaN, uTaNInfo);
                nontrans.nameToUnicode(aN, aNuN, aNuNInfo);
                nontrans.nameToASCII(uN, uNaN, uNaNInfo);
            } catch(Exception e) {
                errln(String.format("second-level processing [%d/%s] %s - %s",
                                    i, testCase.o, testCase.s, e));
                continue;
            }
            if(!UTF16Plus.equal(aN, uNaN)) {
                errln(String.format("N.nameToASCII([%d] %s)!=N.nameToUnicode().N.nameToASCII() "+
                                    "(errors %s) %s vs. %s",
                                    i, testCase.s, aNInfo.getErrors(),
                                    prettify(aN.toString()), prettify(uNaN.toString())));
                continue;
            }
            if(!UTF16Plus.equal(aT, uTaN)) {
                errln(String.format("T.nameToASCII([%d] %s)!=T.nameToUnicode().N.nameToASCII() "+
                                    "(errors %s) %s vs. %s",
                                    i, testCase.s, aNInfo.getErrors(),
                                    prettify(aT.toString()), prettify(uTaN.toString())));
                continue;
            }
            if(!UTF16Plus.equal(uN, aNuN)) {
                errln(String.format("N.nameToUnicode([%d] %s)!=N.nameToASCII().N.nameToUnicode() "+
                                    "(errors %s) %s vs. %s",
                                    i, testCase.s, uNInfo.getErrors(), prettify(uN.toString()), prettify(aNuN.toString())));
                continue;
            }
            if(!UTF16Plus.equal(uT, aTuN)) {
                errln(String.format("T.nameToUnicode([%d] %s)!=T.nameToASCII().N.nameToUnicode() "+
                                    "(errors %s) %s vs. %s",
                                    i, testCase.s, uNInfo.getErrors(),
                                    prettify(uT.toString()), prettify(aTuN.toString())));
                continue;
            }
            // labelToUnicode
            try {
                trans.labelToASCII(input, aTL, aTLInfo);
                trans.labelToUnicode(input, uTL, uTLInfo);
                nontrans.labelToASCII(input, aNL, aNLInfo);
                nontrans.labelToUnicode(input, uNL, uNLInfo);
            } catch(Exception e) {
                errln(String.format("labelToXYZ processing [%d/%s] %s - %s",
                                    i, testCase.o, testCase.s, e));
                continue;
            }
            if(aN.indexOf(".")<0) {
                if(!UTF16Plus.equal(aN, aNL) || !sameErrors(aNInfo, aNLInfo)) {
                    errln(String.format("N.nameToASCII([%d] %s)!=N.labelToASCII() "+
                                        "(errors %s vs %s) %s vs. %s",
                                        i, testCase.s, aNInfo.getErrors().toString(), aNLInfo.getErrors().toString(),
                                        prettify(aN.toString()), prettify(aNL.toString())));
                    continue;
                }
            } else {
                if(!hasError(aNLInfo, IDNA.Error.LABEL_HAS_DOT)) {
                    errln(String.format("N.labelToASCII([%d] %s) errors %s missing UIDNA_ERROR_LABEL_HAS_DOT",
                                        i, testCase.s, aNLInfo.getErrors()));
                    continue;
                }
            }
            if(aT.indexOf(".")<0) {
                if(!UTF16Plus.equal(aT, aTL) || !sameErrors(aTInfo, aTLInfo)) {
                    errln(String.format("T.nameToASCII([%d] %s)!=T.labelToASCII() "+
                                        "(errors %s vs %s) %s vs. %s",
                                        i, testCase.s, aTInfo.getErrors().toString(), aTLInfo.getErrors().toString(),
                                        prettify(aT.toString()), prettify(aTL.toString())));
                    continue;
                }
            } else {
                if(!hasError(aTLInfo, IDNA.Error.LABEL_HAS_DOT)) {
                    errln(String.format("T.labelToASCII([%d] %s) errors %s missing UIDNA_ERROR_LABEL_HAS_DOT",
                                        i, testCase.s, aTLInfo.getErrors()));
                    continue;
                }
            }
            if(uN.indexOf(".")<0) {
                if(!UTF16Plus.equal(uN, uNL) || !sameErrors(uNInfo, uNLInfo)) {
                    errln(String.format("N.nameToUnicode([%d] %s)!=N.labelToUnicode() "+
                                        "(errors %s vs %s) %s vs. %s",
                                        i, testCase.s, uNInfo.getErrors().toString(), uNLInfo.getErrors().toString(),
                                        prettify(uN.toString()), prettify(uNL.toString())));
                    continue;
                }
            } else {
                if(!hasError(uNLInfo, IDNA.Error.LABEL_HAS_DOT)) {
                    errln(String.format("N.labelToUnicode([%d] %s) errors %s missing UIDNA_ERROR_LABEL_HAS_DOT",
                                        i, testCase.s, uNLInfo.getErrors()));
                    continue;
                }
            }
            if(uT.indexOf(".")<0) {
                if(!UTF16Plus.equal(uT, uTL) || !sameErrors(uTInfo, uTLInfo)) {
                    errln(String.format("T.nameToUnicode([%d] %s)!=T.labelToUnicode() "+
                                        "(errors %s vs %s) %s vs. %s",
                                        i, testCase.s, uTInfo.getErrors().toString(), uTLInfo.getErrors().toString(),
                                        prettify(uT.toString()), prettify(uTL.toString())));
                    continue;
                }
            } else {
                if(!hasError(uTLInfo, IDNA.Error.LABEL_HAS_DOT)) {
                    errln(String.format("T.labelToUnicode([%d] %s) errors %s missing UIDNA_ERROR_LABEL_HAS_DOT",
                                        i, testCase.s, uTLInfo.getErrors()));
                    continue;
                }
            }
            // Differences between transitional and nontransitional processing
            if(mode=='B') {
                if( aNInfo.isTransitionalDifferent() ||
                    aTInfo.isTransitionalDifferent() ||
                    uNInfo.isTransitionalDifferent() ||
                    uTInfo.isTransitionalDifferent() ||
                    aNLInfo.isTransitionalDifferent() ||
                    aTLInfo.isTransitionalDifferent() ||
                    uNLInfo.isTransitionalDifferent() ||
                    uTLInfo.isTransitionalDifferent()
                ) {
                    errln(String.format("B.process([%d] %s) isTransitionalDifferent()", i, testCase.s));
                    continue;
                }
                if( !UTF16Plus.equal(aN, aT) || !UTF16Plus.equal(uN, uT) ||
                    !UTF16Plus.equal(aNL, aTL) || !UTF16Plus.equal(uNL, uTL) ||
                    !sameErrors(aNInfo, aTInfo) || !sameErrors(uNInfo, uTInfo) ||
                    !sameErrors(aNLInfo, aTLInfo) || !sameErrors(uNLInfo, uTLInfo)
                ) {
                    errln(String.format("N.process([%d] %s) vs. T.process() different errors or result strings",
                                        i, testCase.s));
                    continue;
                }
            } else {
                if( !aNInfo.isTransitionalDifferent() ||
                    !aTInfo.isTransitionalDifferent() ||
                    !uNInfo.isTransitionalDifferent() ||
                    !uTInfo.isTransitionalDifferent() ||
                    !aNLInfo.isTransitionalDifferent() ||
                    !aTLInfo.isTransitionalDifferent() ||
                    !uNLInfo.isTransitionalDifferent() ||
                    !uTLInfo.isTransitionalDifferent()
                ) {
                    errln(String.format("%s.process([%d] %s) !isTransitionalDifferent()",
                                        testCase.o, i, testCase.s));
                    continue;
                }
                if( UTF16Plus.equal(aN, aT) || UTF16Plus.equal(uN, uT) ||
                    UTF16Plus.equal(aNL, aTL) || UTF16Plus.equal(uNL, uTL)
                ) {
                    errln(String.format("N.process([%d] %s) vs. T.process() same result strings",
                                        i, testCase.s));
                    continue;
                }
            }
        }
    }

    private void checkIdnaTestResult(String line, String type,
            String expected, CharSequence result, String status, IDNA.Info info) {
        // An error in toUnicode or toASCII is indicated by a value in square brackets,
        // such as "[B5 B6]".
        boolean expectedHasErrors = false;
        if (!status.isEmpty()) {
            if (status.charAt(0) != '[') {
                errln(String.format("%s  status field does not start with '[': %s\n    %s",
                        type, status, line));
            }
            if (!status.equals("[]")) {
                expectedHasErrors = true;
            }
        }
        if (expectedHasErrors != info.hasErrors()) {
            errln(String.format(
                    "%s  expected errors %s %b != %b = actual has errors: %s\n    %s",
                    type, status, expectedHasErrors, info.hasErrors(), info.getErrors(), line));
        }
        if (!expectedHasErrors && !UTF16Plus.equal(expected, result)) {
            errln(String.format("%s  expected != actual\n    %s", type, line));
            errln("    " + expected);
            errln("    " + result);
        }
    }

    @Test
    public void IdnaTest() throws IOException {
        BufferedReader idnaTestFile = TestUtil.getDataReader("unicode/IdnaTestV2.txt", "UTF-8");
        Pattern semi = Pattern.compile(";");
        try {
            String line;
            while ((line = idnaTestFile.readLine()) != null) {
                // Remove trailing comments and whitespace.
                int commentStart = line.indexOf('#');
                if (commentStart >= 0) {
                    line = line.substring(0, commentStart);
                }
                String[] fields = semi.split(line, -1);
                if (fields.length <= 1) {
                    continue;  // Skip empty and comment-only lines.
                }

                // IdnaTestV2.txt (since Unicode 11)
                // Column 1: source
                // The source string to be tested
                String source = Utility.unescape(fields[0].trim());

                // Column 2: toUnicode
                // The result of applying toUnicode to the source, with Transitional_Processing=false.
                // A blank value means the same as the source value.
                String toUnicode = Utility.unescape(fields[1].trim());
                if (toUnicode.isEmpty()) {
                    toUnicode = source;
                }

                // Column 3: toUnicodeStatus
                // A set of status codes, each corresponding to a particular test.
                // A blank value means [].
                String toUnicodeStatus = fields[2].trim();

                // Column 4: toAsciiN
                // The result of applying toASCII to the source, with Transitional_Processing=false.
                // A blank value means the same as the toUnicode value.
                String toAsciiN = Utility.unescape(fields[3].trim());
                if (toAsciiN.isEmpty()) {
                    toAsciiN = toUnicode;
                }

                // Column 5: toAsciiNStatus
                // A set of status codes, each corresponding to a particular test.
                // A blank value means the same as the toUnicodeStatus value.
                String toAsciiNStatus = fields[4].trim();
                if (toAsciiNStatus.isEmpty()) {
                    toAsciiNStatus = toUnicodeStatus;
                }

                // Column 6: toAsciiT
                // The result of applying toASCII to the source, with Transitional_Processing=true.
                // A blank value means the same as the toAsciiN value.
                String toAsciiT = Utility.unescape(fields[5].trim());
                if (toAsciiT.isEmpty()) {
                    toAsciiT = toAsciiN;
                }

                // Column 7: toAsciiTStatus
                // A set of status codes, each corresponding to a particular test.
                // A blank value means the same as the toAsciiNStatus value.
                String toAsciiTStatus = fields[6].trim();
                if (toAsciiTStatus.isEmpty()) {
                    toAsciiTStatus = toAsciiNStatus;
                }

                // ToASCII/ToUnicode, transitional/nontransitional
                StringBuilder uN, aN, aT;
                IDNA.Info uNInfo, aNInfo, aTInfo;
                nontrans.nameToUnicode(source, uN = new StringBuilder(), uNInfo = new IDNA.Info());
                checkIdnaTestResult(line, "toUnicodeNontrans", toUnicode, uN, toUnicodeStatus, uNInfo);
                nontrans.nameToASCII(source, aN = new StringBuilder(), aNInfo = new IDNA.Info());
                checkIdnaTestResult(line, "toASCIINontrans", toAsciiN, aN, toAsciiNStatus, aNInfo);
                trans.nameToASCII(source, aT = new StringBuilder(), aTInfo = new IDNA.Info());
                checkIdnaTestResult(line, "toASCIITrans", toAsciiT, aT, toAsciiTStatus, aTInfo);
            }
        } finally {
            idnaTestFile.close();
        }
    }

    private final IDNA trans, nontrans;

    private static final EnumSet<IDNA.Error> severeErrors=EnumSet.of(
        IDNA.Error.LEADING_COMBINING_MARK,
        IDNA.Error.DISALLOWED,
        IDNA.Error.PUNYCODE,
        IDNA.Error.LABEL_HAS_DOT,
        IDNA.Error.INVALID_ACE_LABEL);
    private static final EnumSet<IDNA.Error> lengthOverflowErrors=EnumSet.of(
            IDNA.Error.LABEL_TOO_LONG,
            IDNA.Error.DOMAIN_NAME_TOO_LONG);

    private boolean hasError(IDNA.Info info, IDNA.Error error) {
        return info.getErrors().contains(error);
    }
    // assumes that certainErrors is not empty
    private boolean hasCertainErrors(Set<IDNA.Error> errors, Set<IDNA.Error> certainErrors) {
        return !errors.isEmpty() && !Collections.disjoint(errors, certainErrors);
    }
    private boolean hasCertainErrors(IDNA.Info info, Set<IDNA.Error> certainErrors) {
        return hasCertainErrors(info.getErrors(), certainErrors);
    }
    private boolean sameErrors(Set<IDNA.Error> a, Set<IDNA.Error> b) {
        return a.equals(b);
    }
    private boolean sameErrors(IDNA.Info a, IDNA.Info b) {
        return sameErrors(a.getErrors(), b.getErrors());
    }
    private boolean sameErrors(IDNA.Info a, Set<IDNA.Error> b) {
        return sameErrors(a.getErrors(), b);
    }

    private static boolean
    isASCII(CharSequence str) {
        int length=str.length();
        for(int i=0; i<length; ++i) {
            if(str.charAt(i)>=0x80) {
                return false;
            }
        }
        return true;
    }
}
