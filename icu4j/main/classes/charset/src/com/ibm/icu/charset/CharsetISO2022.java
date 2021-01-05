// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;

import com.ibm.icu.charset.CharsetMBCS.CharsetDecoderMBCS;
import com.ibm.icu.charset.CharsetMBCS.CharsetEncoderMBCS;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

class CharsetISO2022 extends CharsetICU {
    private UConverterDataISO2022 myConverterData;
    private int variant;           // one of enum {ISO_2022_JP, ISO_2022_KR, or ISO_2022_CN}

    private static final byte[] SHIFT_IN_STR    = { 0x0f };
//    private static final byte[] SHIFT_OUT_STR   = { 0x0e };

    private static final byte CR    = 0x0D;
    private static final byte LF    = 0x0A;
/*
    private static final byte H_TAB = 0x09;
    private static final byte SPACE = 0x20;
*/
    private static final char HWKANA_START  = 0xff61;
    private static final char HWKANA_END    = 0xff9f;

    /*
     * 94-character sets with native byte values A1..FE are encoded in ISO 2022
     * as bytes 21..7E. (Subtract 0x80.)
     * 96-character  sets with native bit values A0..FF are encoded in ISO 2022
     * as bytes 20..7F. (Subtract 0x80.)
     * Do not encode C1 control codes with native bytes 80..9F
     * as bytes 00..1F (C0 control codes).
     */
/*
    private static final char GR94_START    = 0xa1;
    private static final char GR94_END      = 0xfe;
*/
    private static final char GR96_START    = 0xa0;
    private static final char GR96_END      = 0xff;

    /* for ISO-2022-JP and -CN implementations */
    // typedef enum {
        /* shared values */
        private static final byte INVALID_STATE = -1;
        private static final byte ASCII         = 0;

        private static final byte SS2_STATE = 0x10;
        private static final byte SS3_STATE = 0x11;

        /* JP */
        private static final byte ISO8859_1 = 1;
        private static final byte ISO8859_7 = 2;
        private static final byte JISX201   = 3;
        private static final byte JISX208   = 4;
        private static final byte JISX212   = 5;
        private static final byte GB2312    = 6;
        private static final byte KSC5601   = 7;
        private static final byte HWKANA_7BIT  = 8; /* Halfwidth Katakana 7 bit */

        /* CN */
        /* the first few enum constants must keep their values because they corresponds to myConverterArray[] */
        private static final byte GB2312_1  = 1;
        private static final byte ISO_IR_165= 2;
        private static final byte CNS_11643 = 3;

        /*
         * these are used in StateEnum and ISO2022State variables,
         * but CNS_11643 must be used to index into myConverterArray[]
         */
        private static final byte CNS_11643_0 = 0x20;
        private static final byte CNS_11643_1 = 0x21;
        private static final byte CNS_11643_2 = 0x22;
        private static final byte CNS_11643_3 = 0x23;
        private static final byte CNS_11643_4 = 0x24;
        private static final byte CNS_11643_5 = 0x25;
        private static final byte CNS_11643_6 = 0x26;
        private static final byte CNS_11643_7 = 0x27;
    // } StateEnum;


    public CharsetISO2022(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);

        myConverterData = new UConverterDataISO2022();

        int versionIndex = icuCanonicalName.indexOf("version=");
        int version = Integer.decode(icuCanonicalName.substring(versionIndex+8, versionIndex+9)).intValue();

        myConverterData.version = version;

        if (icuCanonicalName.indexOf("locale=ja") > 0) {
            ISO2022InitJP(version);
        } else if (icuCanonicalName.indexOf("locale=zh") > 0) {
            ISO2022InitCN(version);
        } else /* if (icuCanonicalName.indexOf("locale=ko") > 0) */ {
            ISO2022InitKR(version);
        }

        myConverterData.currentEncoder = (CharsetEncoderMBCS)myConverterData.currentConverter.newEncoder();
        myConverterData.currentDecoder = (CharsetDecoderMBCS)myConverterData.currentConverter.newDecoder();
    }

    private void ISO2022InitJP(int version) {
        variant = ISO_2022_JP;

        maxBytesPerChar = 6;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
        // open the required converters and cache them
        if((jpCharsetMasks[version]&CSM(ISO8859_7)) != 0) {
            myConverterData.myConverterArray[ISO8859_7] = ((CharsetMBCS)CharsetICU.forNameICU("ISO8859_7")).sharedData;
        }
        // myConverterData.myConverterArray[JISX201] = ((CharsetMBCS)CharsetICU.forNameICU("jisx-201")).sharedData;
        myConverterData.myConverterArray[JISX208] = ((CharsetMBCS)CharsetICU.forNameICU("Shift-JIS")).sharedData;
        if ((jpCharsetMasks[version]&CSM(JISX212)) != 0) {
            myConverterData.myConverterArray[JISX212] = ((CharsetMBCS)CharsetICU.forNameICU("jisx-212")).sharedData;
        }
        if ((jpCharsetMasks[version]&CSM(GB2312)) != 0) {
            myConverterData.myConverterArray[GB2312] = ((CharsetMBCS)CharsetICU.forNameICU("ibm-5478")).sharedData;
        }
        if ((jpCharsetMasks[version]&CSM(KSC5601)) != 0) {
            myConverterData.myConverterArray[KSC5601] = ((CharsetMBCS)CharsetICU.forNameICU("ksc_5601")).sharedData;
        }

        // create a generic CharsetMBCS object
        myConverterData.currentConverter = (CharsetMBCS)CharsetICU.forNameICU("icu-internal-25546");
    }

    private void ISO2022InitCN(int version) {
        variant = ISO_2022_CN;

        maxBytesPerChar = 8;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
        // open the required converters and cache them.
        myConverterData.myConverterArray[GB2312_1] = ((CharsetMBCS)CharsetICU.forNameICU("ibm-5478")).sharedData;
        if (version == 1) {
            myConverterData.myConverterArray[ISO_IR_165] = ((CharsetMBCS)CharsetICU.forNameICU("iso-ir-165")).sharedData;
        }
        myConverterData.myConverterArray[CNS_11643] = ((CharsetMBCS)CharsetICU.forNameICU("cns-11643-1992")).sharedData;

        // create a generic CharsetMBCS object
        myConverterData.currentConverter = (CharsetMBCS)CharsetICU.forNameICU("icu-internal-25546");
    }

    private void ISO2022InitKR(int version) {
        variant = ISO_2022_KR;

        maxBytesPerChar = 8;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;

        if (version == 1) {
            myConverterData.currentConverter = (CharsetMBCS)CharsetICU.forNameICU("icu-internal-25546");
            myConverterData.currentConverter.subChar1 = fromUSubstitutionChar[0][0];
        } else {
            myConverterData.currentConverter = (CharsetMBCS)CharsetICU.forNameICU("ibm-949");
        }

        myConverterData.currentEncoder = (CharsetEncoderMBCS)myConverterData.currentConverter.newEncoder();
        myConverterData.currentDecoder = (CharsetDecoderMBCS)myConverterData.currentConverter.newDecoder();
    }

    /*
     * ISO 2022 control codes must not be converted from Unicode
     * because they would mess up the byte stream.
     * The bit mask 0x0800c000 has bits set at bit positions 0xe, 0xf, 0x1b
     * corresponding to SO, SI, and ESC.
     */
    private static boolean IS_2022_CONTROL(int c) {
        return (c<0x20) && (((1<<c) & 0x0800c000) != 0);
    }

    /*
     * Check that the result is a 2-byte value with each byte in the range A1..FE
     * (strict EUC DBCS) before accepting it and subtracting 0x80 from each byte
     * to move it to the ISO 2022 range 21..7E.
     * return 0 if out of range.
     */
    private static int _2022FromGR94DBCS(int value) {
        if ((value <= 0xfefe && value >= 0xa1a1) &&
                ((short)(value&UConverterConstants.UNSIGNED_BYTE_MASK) <= 0xfe && ((short)(value&UConverterConstants.UNSIGNED_BYTE_MASK) >= 0xa1))) {
            return (value - 0x8080); /* shift down to 21..7e byte range */
        } else {
            return 0; /* not valid for ISO 2022 */
        }
    }

    /*
     * Commented out because Ticket 5691: Call sites now check for validity. They can just += 0x8080 after that.
     *
     * This method does the reverse of _2022FromGR94DBCS(). Given the 2022 code point, it returns the
     * 2 byte value that is in the range A1..FE for each byte. Otherwise it returns the 2022 code point
     * unchanged.
     *
    private static int _2022ToGR94DBCS(int value) {
        int returnValue = value + 0x8080;

        if ((returnValue <= 0xfefe && returnValue >= 0xa1a1) &&
                ((short)(returnValue&UConverterConstants.UNSIGNED_BYTE_MASK) <= 0xfe && ((short)(returnValue&UConverterConstants.UNSIGNED_BYTE_MASK) >= 0xa1))) {
            return returnValue;
        } else {
            return value;
        }
    }*/

    /* is the StateEnum charset value for a DBCS charset? */
    private static boolean IS_JP_DBCS(byte cs) {
        return ((JISX208 <= cs) && (cs <= KSC5601));
    }

    private static short CSM(short cs) {
        return (short)(1<<cs);
    }

    /* This gets the valid index of the end of buffer when decoding. */
    private static int getEndOfBuffer_2022(ByteBuffer source) {
        int sourceIndex = source.position();
        byte mySource = 0;
        mySource = source.get(sourceIndex);

        while (source.hasRemaining() && mySource != ESC_2022) {
            mySource = source.get();
            if (mySource == ESC_2022) {
                break;
            }
            sourceIndex++;
        }
        return sourceIndex;
    }

    /*
     * This is a simple version of _MBCSGetNextUChar() calls the method in CharsetDecoderMBCS and returns
     * the value given.
     *
     * Return value:
     * U+fffe   unassigned
     * U+ffff   illegal
     * otherwise the Unicode code point
     */
     private int MBCSSimpleGetNextUChar(UConverterSharedData sharedData,
                               ByteBuffer   source,
                               boolean      useFallback) {
         int returnValue;
         UConverterSharedData tempSharedData = myConverterData.currentConverter.sharedData;
         myConverterData.currentConverter.sharedData = sharedData;
         returnValue = myConverterData.currentDecoder.simpleGetNextUChar(source, useFallback);
         myConverterData.currentConverter.sharedData = tempSharedData;

         return returnValue;
    }

    /*
     * @param is the the output byte
     * @return 1 roundtrip byte  0 no mapping  -1 fallback byte
     */
    static int MBCSSingleFromUChar32(UConverterSharedData sharedData, int c, int[] retval, boolean useFallback) {
        char[] table;
        int value;
        /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
        if (c >= 0x10000 && !sharedData.mbcs.hasSupplementary()) {
            return 0;
        }
        /* convert the Unicode code point in c into codepage bytes */
        table = sharedData.mbcs.fromUnicodeTable;
        /* get the byte for the output */
        value = CharsetMBCS.MBCS_SINGLE_RESULT_FROM_U(table, sharedData.mbcs.fromUnicodeChars, c);
        /* get the byte for the output */
        retval[0] = value & 0xff;
        if (value >= 0xf00) {
            return 1; /* roundtrip */
        } else if (useFallback ? value>=0x800 : value>=0xc00) {
            return -1; /* fallback taken */
        } else {
            return 0; /* no mapping */
        }
    }

    /*
     * Each of these charset masks (with index x) contains a bit for a charset in exact correspondence
     * to whether that charset is used in the corresponding version x of ISO_2022, locale=ja,version=x
     *
     * Note: The converter uses some leniency:
     * - The escape sequence ESC ( I for half-width 7-bit Katakana is recognized in
     *   all versions, not just JIS7 and JIS8.
     * - ICU does not distinguish between different version so of JIS X 0208.
     */
    private static final short jpCharsetMasks[] = {
        (short)(CSM(ASCII)|CSM(JISX201)|CSM(JISX208)|CSM(HWKANA_7BIT)),
        (short)(CSM(ASCII)|CSM(JISX201)|CSM(JISX208)|CSM(HWKANA_7BIT)|CSM(JISX212)),
        (short)(CSM(ASCII)|CSM(JISX201)|CSM(JISX208)|CSM(HWKANA_7BIT)|CSM(JISX212)|CSM(GB2312)|CSM(KSC5601)|CSM(ISO8859_1)|CSM(ISO8859_7)),
        (short)(CSM(ASCII)|CSM(JISX201)|CSM(JISX208)|CSM(HWKANA_7BIT)|CSM(JISX212)|CSM(GB2312)|CSM(KSC5601)|CSM(ISO8859_1)|CSM(ISO8859_7)),
        (short)(CSM(ASCII)|CSM(JISX201)|CSM(JISX208)|CSM(HWKANA_7BIT)|CSM(JISX212)|CSM(GB2312)|CSM(KSC5601)|CSM(ISO8859_1)|CSM(ISO8859_7))
    };

/*
    // typedef enum {
        private static final byte ASCII1 = 0;
        private static final byte LATIN1 = 1;
        private static final byte SBCS   = 2;
        private static final byte DBCS   = 3;
        private static final byte MBCS   = 4;
        private static final byte HWKANA = 5;
    // } Cnv2002Type;
*/

    private static class ISO2022State {
        private byte []cs;  /* Charset number for SI (G0)/SO (G1)/SS2 (G2)/SS3 (G3) */
        private byte g;     /* 0..3 for G0..G3 (SI/SO/SS2/SS3) */
        private byte prevG; /* g before single shift (SS2 or SS3) */

        ISO2022State() {
            cs = new byte[4];
        }

        void reset() {
            Arrays.fill(cs, (byte)0);
            g = 0;
            prevG = 0;
        }
    }

//    private static final byte UCNV_OPTIONS_VERSION_MASK = 0xf;
    private static final byte UCNV_2022_MAX_CONVERTERS  = 10;

    private static class UConverterDataISO2022 {
        UConverterSharedData []myConverterArray;
        CharsetEncoderMBCS currentEncoder;
        CharsetDecoderMBCS currentDecoder;
        CharsetMBCS currentConverter;
        ISO2022State toU2022State;
        ISO2022State fromU2022State;
        int key;
        int version;
        boolean isEmptySegment;

        UConverterDataISO2022() {
            myConverterArray = new UConverterSharedData[UCNV_2022_MAX_CONVERTERS];
            toU2022State = new ISO2022State();
            fromU2022State = new ISO2022State();
            key = 0;
            version = 0;
            isEmptySegment = false;
        }

        void reset() {
            toU2022State.reset();
            fromU2022State.reset();
            isEmptySegment = false;
        }
    }

    private static final byte ESC_2022 = 0x1B; /* ESC */

    // typedef enum {
        private static final byte INVALID_2022              = -1; /* Doesn't correspond to a valid iso 2022 escape sequence */
        private static final byte VALID_NON_TERMINAL_2022   =  0;  /* so far corresponds to a valid iso 2022 escape sequence */
        private static final byte VALID_TERMINAL_2022       =  1;  /* corresponds to a valid iso 2022 escape sequence */
        private static final byte VALID_MAYBE_TERMINAL_2022 =  2;  /* so far matches one iso 2022 escape sequence, but by adding
                                                                     more characters might match another escape sequence */
    // } UCNV_TableStates_2022;

    /*
     * The way these state transition arrays work is:
     * ex : ESC$B is the sequence for JISX208
     *      a) First Iteration: char is ESC
     *          i) Get the value of ESC from normalize_esq_chars_2022[] with int value of ESC as index
     *             int x = normalize_esq_chars_2022[27] which is equal to 1
     *         ii) Search for this value in escSeqStateTable_Key_2022[]
     *             value of x is stored at escSeqStateTable_Key_2022[0]
     *        iii) Save this index as offset
     *         iv) Get state of this sequence from escSeqStateTable_Value_2022[]
     *             escSeqStateTable_value_2022[offset], which is VALID_NON_TERMINAL_2022
     *      b) Switch on this state and continue to next char
     *          i) Get the value of $ from normalize_esq_chars_2022[] with int value of $ as index
     *             which is normalize_esq_chars_2022[36] == 4
     *         ii) x is currently 1(from above)
     *             x<<=5 -- x is now 32
     *             x+=normalize_esq_chars_2022[36]
     *             now x is 36
     *        iii) Search for this value in escSeqStateTable_Key_2022[]
     *             value of x is stored at escSeqStateTable_Key_2022[2], so offset is 2
     *         iv) Get state of this sequence from escSeqStateTable_Value_2022[]
     *             escSeqStateTable_Value_2022[offset], which is VALID_NON_TERMINAL_2022
     *      c) Switch on this state and continue to next char
     *          i) Get the value of B from normalize_esq_chars_2022[] with int value of B as index
     *         ii) x is currently 36 (from above)
     *             x<<=5 -- x is now 1152
     *             x+= normalize_esq_chars_2022[66]
     *             now x is 1161
     *        iii) Search for this value in escSeqStateTable_Key_2022[]
     *             value of x is stored at escSeqStateTable_Key_2022[21], so offset is 21
     *         iv) Get state of this sequence from escSeqStateTable_Value_2022[1]
     *             escSeqStateTable_Value_2022[offset], which is VALID_TERMINAL_2022
     *          v) Get the converter name from escSeqStateTable_Result_2022[21] which is JISX208
     */
     /* Below are the 3 arrays depicting a state transition table */
     private static final byte normalize_esq_chars_2022[] = {
         /* 0       1       2       3       4       5       6       7       8       9 */
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      1,      0,      0,
            0,      0,      0,      0,      0,      0,      4,      7,     29,      0,
            2,     24,     26,     27,      0,      3,     23,      6,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      5,      8,      9,     10,     11,     12,
           13,     14,     15,     16,     17,     18,     19,     20,     25,     28,
            0,      0,     21,      0,      0,      0,      0,      0,      0,      0,
           22,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0,      0,      0,      0,      0,
            0,      0,      0,      0,      0,      0
     };

     private static final short MAX_STATES_2022 = 74;
     private static final int escSeqStateTable_Key_2022[/* MAX_STATES_2022 */] = {
         /* 0        1          2         3        4          5         6         7         8         9 */
            1,      34,        36,       39,      55,        57,       60,       61,     1093,     1096,
         1097,    1098,      1099,     1100,     1101,     1102,     1103,     1104,     1105,     1106,
         1109,    1154,      1157,     1160,     1161,     1176,     1178,     1179,     1254,     1257,
         1768,    1773,      1957,    35105,    36933,    36936,    36937,    36938,    36939,    36940,
        36942,   36943,     36944,    36945,    36946,    36947,    36948,    37640,    37642,    37644,
        37646,   37711,     37744,    37745,    37746,    37747,    37748,    40133,    40136,    40138,
        40139,   40140,     40141,  1123363, 35947624, 35947625, 35947626, 35947627, 35947629, 35947630,
     35947631, 35947635, 35947636, 35947638
     };

     private static final byte escSeqStateTable_Value_2022[/* MAX_STATES_2022 */] = {
         /*         0                           1                           2                           3                       4               */
         VALID_NON_TERMINAL_2022,   VALID_NON_TERMINAL_2022,    VALID_NON_TERMINAL_2022,    VALID_NON_TERMINAL_2022,    VALID_NON_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,    VALID_NON_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
       VALID_MAYBE_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,   VALID_NON_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
         VALID_NON_TERMINAL_2022,   VALID_NON_TERMINAL_2022,    VALID_NON_TERMINAL_2022,    VALID_NON_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,    VALID_NON_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,    VALID_NON_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022,
             VALID_TERMINAL_2022,       VALID_TERMINAL_2022,        VALID_TERMINAL_2022,        VALID_TERMINAL_2022
     };

     /* Type def for refactoring changeState_2022 code */
     // typedef enum {
         private static final byte ISO_2022_JP = 1;
         private static final byte ISO_2022_KR = 2;
         private static final byte ISO_2022_CN = 3;
     // } Variant2022;

    /* const UConverterSharedData _ISO2022Data; */
    //private UConverterSharedData _ISO2022JPData;
    //private UConverterSharedData _ISO2022KRData;
    //private UConverterSharedData _ISO2022CNData;

    /******************** to unicode ********************/
    /****************************************************
     * Recognized escape sequenes are
     * <ESC>(B  ASCII
     * <ESC>.A  ISO-8859-1
     * <ESC>.F  ISO-8859-7
     * <ESC>(J  JISX-201
     * <ESC>(I  JISX-201
     * <ESC>$B  JISX-208
     * <ESC>$@  JISX-208
     * <ESC>$(D JISX-212
     * <ESC>$A  GB2312
     * <ESC>$(C KSC5601
     */
    private final static byte nextStateToUnicodeJP[/* MAX_STATES_2022 */] = {
        /*     0               1               2               3               4               5               6               7               8               9    */
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,      SS2_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
                ASCII,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,        JISX201,    HWKANA_7BIT,        JISX201,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,        JISX208,         GB2312,        JISX208,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
            ISO8859_1,      ISO8859_7,        JISX208,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,        KSC5601,        JISX212,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE
    };

    private final static byte nextStateToUnicodeCN[/* MAX_STATES_2022 */] = {
        /*     0               1               2               3               4               5               6               7               8               9    */
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,      SS2_STATE,      SS3_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,       GB2312_1,  INVALID_STATE,     ISO_IR_165,
          CNS_11643_1,    CNS_11643_2,    CNS_11643_3,    CNS_11643_4,    CNS_11643_5,    CNS_11643_6,    CNS_11643_7,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE,
        INVALID_STATE,  INVALID_STATE,  INVALID_STATE,  INVALID_STATE
    };

    /* runs through a state machine to determine the escape sequence - codepage correspondence */
    @SuppressWarnings("fallthrough")
    private CoderResult changeState_2022(CharsetDecoderICU decoder, ByteBuffer source, int var) {
        CoderResult err = CoderResult.UNDERFLOW;
        boolean DONE = false;
        byte value;
        int key[] = {myConverterData.key};
        int offset[] = {0};
        int initialToULength = decoder.toULength;
        byte c;
        int malformLength = 0;

        value = VALID_NON_TERMINAL_2022;
        while (source.hasRemaining()) {
            c = source.get();
            malformLength++;
            decoder.toUBytesArray[decoder.toULength++] = c;
            value = getKey_2022(c, key, offset);

            switch(value) {

            case VALID_NON_TERMINAL_2022:
                /* continue with the loop */
                break;

            case VALID_TERMINAL_2022:
                key[0] = 0;
                DONE = true;
                break;

            case INVALID_2022:
                DONE = true;
                break;

            case VALID_MAYBE_TERMINAL_2022:
                /* not ISO_2022 itself, finish here */
                value = VALID_TERMINAL_2022;
                key[0] = 0;
                DONE = true;
                break;
            }
            if (DONE) {
                break;
            }
        }
// DONE:
        myConverterData.key = key[0];

        if (value == VALID_NON_TERMINAL_2022) {
            /* indicate that the escape sequence is incomplete: key !=0 */
            return err;
        } else if (value == INVALID_2022) {
            err = CoderResult.malformedForLength(malformLength);
        } else /* value == VALID_TERMINAL_2022 */ {
            switch (var) {
            case ISO_2022_JP: {
                byte tempState = nextStateToUnicodeJP[offset[0]];
                switch (tempState) {
                case INVALID_STATE:
                    err = CoderResult.malformedForLength(malformLength);
                    break;
                case SS2_STATE:
                    if (myConverterData.toU2022State.cs[2] != 0) {
                        if (myConverterData.toU2022State.g < 2) {
                            myConverterData.toU2022State.prevG = myConverterData.toU2022State.g;
                        }
                        myConverterData.toU2022State.g = 2;
                    } else {
                        /* illegal to have SS2 before a matching designator */
                        err = CoderResult.malformedForLength(malformLength);
                    }
                    break;
                /* case SS3_STATE: not used in ISO-2022-JP-x */
                case ISO8859_1:
                case ISO8859_7:
                    if ((jpCharsetMasks[myConverterData.version] & CSM(tempState)) == 0) {
                        err = CoderResult.unmappableForLength(malformLength);
                    } else {
                        /* G2 charset for SS2 */
                        myConverterData.toU2022State.cs[2] = tempState;
                    }
                    break;
                default:
                    if ((jpCharsetMasks[myConverterData.version] & CSM(tempState)) == 0) {
                        err = CoderResult.unmappableForLength(source.position() - 1);
                    } else {
                        /* G0 charset */
                        myConverterData.toU2022State.cs[0] = tempState;
                    }
                    break;
                } // end of switch
                break;
            }
            case ISO_2022_CN: {
                byte tempState = nextStateToUnicodeCN[offset[0]];
                switch (tempState) {
                case INVALID_STATE:
                    err = CoderResult.unmappableForLength(malformLength);
                    break;
                case SS2_STATE:
                    if (myConverterData.toU2022State.cs[2] != 0) {
                        if (myConverterData.toU2022State.g < 2) {
                            myConverterData.toU2022State.prevG = myConverterData.toU2022State.g;
                        }
                        myConverterData.toU2022State.g = 2;
                    } else {
                        /* illegal to have SS2 before a matching designator */
                        err = CoderResult.malformedForLength(malformLength);
                    }
                    break;
                case SS3_STATE:
                    if (myConverterData.toU2022State.cs[3] != 0) {
                        if (myConverterData.toU2022State.g < 2) {
                            myConverterData.toU2022State.prevG = myConverterData.toU2022State.g;
                        }
                        myConverterData.toU2022State.g = 3;
                    } else {
                        /* illegal to have SS3 before a matching designator */
                        err = CoderResult.malformedForLength(malformLength);
                    }
                    break;
                case ISO_IR_165:
                    if (myConverterData.version == 0) {
                        err = CoderResult.unmappableForLength(malformLength);
                        break;
                    }
                    /* fall through */
                case GB2312_1:
                    /* fall through */
                case CNS_11643_1:
                    myConverterData.toU2022State.cs[1] = tempState;
                    break;
                case CNS_11643_2:
                    myConverterData.toU2022State.cs[2] = tempState;
                    break;
                default:
                    /* other CNS 11643 planes */
                    if (myConverterData.version == 0) {
                        err = CoderResult.unmappableForLength(source.position() - 1);
                    } else {
                        myConverterData.toU2022State.cs[3] = tempState;
                    }
                    break;
                } //end of switch
            }
            break;
            case ISO_2022_KR:
                if (offset[0] == 0x30) {
                    /* nothing to be done, just accept this one escape sequence */
                } else {
                    err = CoderResult.unmappableForLength(malformLength);
                }
                break;
            default:
                err = CoderResult.malformedForLength(malformLength);
                break;
            } // end of switch
        }
        if (!err.isError()) {
            decoder.toULength = 0;
        } else if (err.isMalformed()) {
            if (decoder.toULength > 1) {
                /*
                 * Ticket 5691: consistent illegal sequences:
                 * - We include at least the first byte (ESC) in the illegal sequence.
                 * - If any of the non-initial bytes could be the start of a character,
                 *   we stop the illegal sequece before the first one of those.
                 *   In escape sequences, all following bytes are "printable", that is,
                 *   unless they are completely illegal (>7f in SBCS, outside 21..7e in DBCS),
                 *   they are valid single/lead bytes.
                 *   For simplicity, we always only report the initial ESC byte as the
                 *   illegal sequence and back out all other bytes we looked at.
                 */
                /* Back out some bytes. */
                int backOutDistance = decoder.toULength - 1;
                int bytesFromThisBuffer = decoder.toULength - initialToULength;
                if (backOutDistance <= bytesFromThisBuffer) {
                    /* same as initialToULength<=1 */
                    source.position(source.position() - backOutDistance);
                } else {
                    /* Back out bytes from the previous buffer: Need to replay them. */
                    decoder.preToULength = (byte)(bytesFromThisBuffer - backOutDistance);
                    /* same as -(initalToULength-1) */
                    /* preToULength is negative! */
                    for (int i = 0; i < -(decoder.preToULength); i++) {
                        decoder.preToUArray[i] = decoder.toUBytesArray[i+1];
                    }
                    source.position(source.position() - bytesFromThisBuffer);
                }
                decoder.toULength = 1;
            }
        }

        return err;
    }

    private static byte getKey_2022(byte c, int[]key, int[]offset) {
        int togo;
        int low = 0;
        int hi = MAX_STATES_2022;
        int oldmid = 0;

        togo = normalize_esq_chars_2022[c&UConverterConstants.UNSIGNED_BYTE_MASK];

        if (togo == 0) {
            /* not a valid character anywhere in an escape sequence */
            key[0] = 0;
            offset[0] = 0;
            return INVALID_2022;
        }
        togo = (key[0] << 5) + togo;

        while (hi != low) { /* binary search */
            int mid = (hi+low) >> 1; /* Finds median */

            if (mid == oldmid) {
                break;
            }

            if (escSeqStateTable_Key_2022[mid] > togo) {
                hi = mid;
            } else if (escSeqStateTable_Key_2022[mid] < togo) {
                low = mid;
            } else /* we found it */ {
                key[0] = togo;
                offset[0] = mid;
                return escSeqStateTable_Value_2022[mid];
            }
            oldmid = mid;
        }
        return INVALID_2022;
    }

    /*
     * To Unicode Callback helper function
     */
    private static CoderResult toUnicodeCallback(CharsetDecoderICU cnv, int sourceChar, int targetUniChar) {
        CoderResult err = CoderResult.UNDERFLOW;
        if (sourceChar > 0xff) {
            cnv.toUBytesArray[0] = (byte)(sourceChar>>8);
            cnv.toUBytesArray[1] = (byte)sourceChar;
            cnv.toULength = 2;
        } else {
            cnv.toUBytesArray[0] = (byte)sourceChar;
            cnv.toULength = 1;
        }

        if (targetUniChar == (UConverterConstants.missingCharMarker-1/* 0xfffe */)) {
            err = CoderResult.unmappableForLength(1);
        } else {
            err = CoderResult.malformedForLength(1);
        }

        return err;
    }

    /****************************ISO-2022-JP************************************/
    private class CharsetDecoderISO2022JP extends CharsetDecoderICU {
        public CharsetDecoderISO2022JP(CharsetICU cs) {
            super(cs);
        }

        @Override
        protected void implReset() {
            super.implReset();
            myConverterData.reset();
        }
        /*
         * Map 00..7F to Unicode according to JIS X 0201.
         * */
        private int jisx201ToU(int value) {
            if (value < 0x5c) {
                return value;
            } else if (value == 0x5c) {
                return 0xa5;
            } else if (value == 0x7e) {
                return 0x203e;
            } else { /* value <= 0x7f */
                return value;
            }
        }
        /*
         * Convert a pair of JIS X 208 21..7E bytes to Shift-JIS.
         * If either byte is outside 21..7E make sure that the result is not valid
         * for Shift-JIS so that the converter catches it.
         * Some invalid byte values already turn into equally invalid Shift-JIS
         * byte values and need not be tested explicitly.
         */
        private void _2022ToSJIS(char c1, char c2, byte []bytes) {
            if ((c1&1) > 0) {
                ++c1;
                if (c2 <= 0x5f) {
                    c2 += 0x1f;
                } else if (c2 <= 0x7e) {
                    c2 += 0x20;
                } else {
                    c2 = 0; /* invalid */
                }
            } else {
                if ((c2 >= 0x21) && (c2 <= 0x7e)) {
                    c2 += 0x7e;
                } else {
                    c2 = 0; /* invalid */
                }
            }

            c1 >>=1;
            if (c1 <= 0x2f) {
                c1 += 0x70;
            } else if (c1 <= 0x3f) {
                c1 += 0xb0;
            } else {
                c1 = 0; /* invalid */
            }
            bytes[0] = (byte)(UConverterConstants.UNSIGNED_BYTE_MASK & c1);
            bytes[1] = (byte)(UConverterConstants.UNSIGNED_BYTE_MASK & c2);
        }

        @Override
        @SuppressWarnings("fallthrough")
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            boolean gotoGetTrail = false;
            boolean gotoEscape = false;
            CoderResult err = CoderResult.UNDERFLOW;
            byte []tempBuf = new byte[2];
            int targetUniChar = 0x0000;
            int mySourceChar = 0x0000;
            int mySourceCharTemp = 0x0000; // use for getTrail label call.
            byte cs; /* StateEnum */
            byte csTemp= 0; // use for getTrail label call.

            if (myConverterData.key != 0) {
                /* continue with a partial escape sequence */
                // goto escape;
                gotoEscape = true;
            } else if (toULength == 1 && source.hasRemaining() && target.hasRemaining()) {
                /* continue with a partial double-byte character */
                mySourceChar = (toUBytesArray[0] & UConverterConstants.UNSIGNED_BYTE_MASK);
                toULength = 0;
                cs = myConverterData.toU2022State.cs[myConverterData.toU2022State.g];
                // goto getTrailByte;
                mySourceCharTemp = 0x99;
                gotoGetTrail = true;
            }

            while (source.hasRemaining() || gotoEscape || gotoGetTrail) {
                // This code is here for the goto escape label call above.
                if (gotoEscape) {
                    mySourceCharTemp = ESC_2022;
                }

                targetUniChar = UConverterConstants.missingCharMarker;

                if (gotoEscape || gotoGetTrail || target.hasRemaining()) {
                    if (!gotoEscape && !gotoGetTrail) {
                        mySourceChar = source.get() & UConverterConstants.UNSIGNED_BYTE_MASK;
                        mySourceCharTemp = mySourceChar;
                    }

                    switch (mySourceCharTemp) {
                    case UConverterConstants.SI:
                        if (myConverterData.version == 3) {
                            myConverterData.toU2022State.g = 0;
                            continue;
                        } else {
                            /* only JIS7 uses SI/SO, not ISO-2022-JP-x */
                            myConverterData.isEmptySegment = false;
                            break;
                        }

                    case UConverterConstants.SO:
                        if (myConverterData.version == 3) {
                            /* JIS7: switch to G1 half-width Katakana */
                            myConverterData.toU2022State.cs[1] = HWKANA_7BIT;
                            myConverterData.toU2022State.g = 1;
                            continue;
                        } else {
                            /* only JIS7 uses SI/SO, not ISO-2022-JP-x */
                            myConverterData.isEmptySegment = false; /* reset this, we have a different error */
                            break;
                        }

                    case ESC_2022:
                        if (!gotoEscape) {
                            source.position(source.position() - 1);
                        } else {
                            gotoEscape = false;
                        }
// escape:
                        {
                            int mySourceBefore = source.position();
                            int toULengthBefore = this.toULength;

                            err = changeState_2022(this, source, variant);

                            /* If in ISO-2022-JP only and we successully completed an escape sequence, but previous segment was empty, create an error */
                            if(myConverterData.version == 0 && myConverterData.key == 0 && !err.isError() && myConverterData.isEmptySegment) {
                                err = CoderResult.malformedForLength(source.position() - mySourceBefore);
                                this.toULength = toULengthBefore + (source.position() - mySourceBefore);
                            }
                        }

                        /* invalid or illegal escape sequence */
                        if(err.isError()){
                            myConverterData.isEmptySegment = false; /* Reset to avoid future spurious errors */
                            return err;
                        }
                        /* If we successfully completed an escape sequence, we begin a new segment, empty so far */
                        if(myConverterData.key == 0) {
                            myConverterData.isEmptySegment = true;
                        }

                        continue;
                    /* ISO-2022-JP does not use single-byte (C1) SS2 and SS3 */
                    case CR:
                        /* falls through */
                    case LF:
                        /* automatically reset to single-byte mode */
                        if (myConverterData.toU2022State.cs[0] != ASCII && myConverterData.toU2022State.cs[0] != JISX201) {
                            myConverterData.toU2022State.cs[0] = ASCII;
                        }
                        myConverterData.toU2022State.cs[2] = 0;
                        myConverterData.toU2022State.g = 0;
                        /* falls through */
                    default :
                        /* convert one or two bytes */
                        myConverterData.isEmptySegment = false;
                        cs = myConverterData.toU2022State.cs[myConverterData.toU2022State.g];
                        csTemp = cs;
                        if (gotoGetTrail) {
                            csTemp = (byte)0x99;
                        }
                        if (!gotoGetTrail && ((mySourceChar >= 0xa1) && (mySourceChar <= 0xdf) && myConverterData.version == 4 && !IS_JP_DBCS(cs))) {
                            /* 8-bit halfwidth katakana in any single-byte mode for JIS8 */
                            targetUniChar = mySourceChar + (HWKANA_START - 0xa1);

                            /* return from a single-shift state to the previous one */
                            if (myConverterData.toU2022State.g >= 2) {
                                myConverterData.toU2022State.g = myConverterData.toU2022State.prevG;
                            }
                        } else {
                            switch(csTemp) {
                            case ASCII:
                                if (mySourceChar <= 0x7f) {
                                    targetUniChar = mySourceChar;
                                }
                                break;
                            case ISO8859_1:
                                if (mySourceChar <= 0x7f) {
                                    targetUniChar = mySourceChar + 0x80;
                                }
                                /* return from a single-shift state to the prevous one */
                                myConverterData.toU2022State.g = myConverterData.toU2022State.prevG;
                                break;
                            case ISO8859_7:
                                if (mySourceChar <= 0x7f) {
                                    /* convert mySourceChar+0x80 to use a normal 8-bit table */
                                    targetUniChar = CharsetMBCS.MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(myConverterData.myConverterArray[cs].mbcs,
                                            mySourceChar+0x80);
                                }
                                /* return from a single-shift state to the previous one */
                                myConverterData.toU2022State.g = myConverterData.toU2022State.prevG;
                                break;
                            case JISX201:
                                if (mySourceChar <= 0x7f) {
                                    targetUniChar = jisx201ToU(mySourceChar);
                                }
                                break;
                            case HWKANA_7BIT:
                                if ((mySourceChar >= 0x21) && (mySourceChar <= 0x5f)) {
                                    /* 7-bit halfwidth Katakana */
                                    targetUniChar = mySourceChar + (HWKANA_START - 0x21);
                                    break;
                                }
                            default :
                                /* G0 DBCS */
                                if (gotoGetTrail || source.hasRemaining()) {
// getTrailByte:
                                    int tmpSourceChar;
                                    gotoGetTrail = false;
                                    short trailByte;
                                    boolean leadIsOk, trailIsOk;

                                    trailByte = (short)(source.get(source.position()) & UConverterConstants.UNSIGNED_BYTE_MASK);
                                    /*
                                     * Ticket 5691: consistent illegal sequences:
                                     * - We include at least the first byte in the illegal sequence.
                                     * - If any of the non-initial bytes could be the start of a character,
                                     *   we stop the illegal sequence before the first one of those.
                                     *
                                     * In ISO-2022 DBCS, if the second byte is in the 21..7e range or is
                                     * an ESC/SO/SI, we report only the first byte as the illegal sequence.
                                     * Otherwise we convert or report the pair of bytes.
                                     */
                                    leadIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (mySourceChar - 0x21)) <= (0x7e - 0x21);
                                    trailIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (trailByte - 0x21)) <= (0x7e - 0x21);
                                    if (leadIsOk && trailIsOk) {
                                        source.get();
                                        tmpSourceChar = (mySourceChar << 8) | trailByte;
                                        if (cs == JISX208) {
                                            _2022ToSJIS((char)mySourceChar, (char)trailByte, tempBuf);
                                            mySourceChar = tmpSourceChar;
                                        } else {
                                            /* Copy before we modify tmpSourceChar so toUnicodeCallback() sees the correct bytes. */
                                            mySourceChar = tmpSourceChar;
                                            if (cs == KSC5601) {
                                                tmpSourceChar += 0x8080; /* = _2022ToGR94DBCS(tmpSourceChar) */
                                            }
                                            tempBuf[0] = (byte)(UConverterConstants.UNSIGNED_BYTE_MASK & (tmpSourceChar >> 8));
                                            tempBuf[1] = (byte)(UConverterConstants.UNSIGNED_BYTE_MASK & tmpSourceChar);
                                        }
                                        targetUniChar = MBCSSimpleGetNextUChar(myConverterData.myConverterArray[cs], ByteBuffer.wrap(tempBuf), false);
                                    } else if (!(trailIsOk || IS_2022_CONTROL(trailByte))) {
                                        /* report a pair of illegal bytes if the second byte is not a DBCS starter */
                                        source.get();
                                        /* add another bit so that the code below writes 2 bytes in case of error */
                                        mySourceChar = 0x10000 | (mySourceChar << 8) | trailByte;
                                    }
                                } else {
                                    toUBytesArray[0] = (byte)mySourceChar;
                                    toULength = 1;
                                    // goto endloop
                                    return err;
                                }
                            } /* end of inner switch */
                        }
                        break;
                    } /* end of outer switch */

                    if (targetUniChar < (UConverterConstants.missingCharMarker-1/*0xfffe*/)) {
                        if (offsets != null) {
                            offsets.put(target.remaining(), source.remaining() - (mySourceChar <= 0xff ? 1 : 2));
                        }
                        target.put((char)targetUniChar);
                    } else if (targetUniChar > UConverterConstants.missingCharMarker) {
                        /* disassemble the surrogate pair and write to output */
                        targetUniChar -= 0x0010000;
                        target.put((char)(0xd800 + (char)(targetUniChar>>10)));
                        target.position(target.position()-1);
                        if (offsets != null) {
                            offsets.put(target.remaining(), source.remaining() - (mySourceChar <= 0xff ? 1 : 2));
                        }
                        target.get();
                        if (target.hasRemaining()) {
                            target.put((char)(0xdc00+(char)(targetUniChar&0x3ff)));
                            target.position(target.position()-1);
                            if (offsets != null) {
                                offsets.put(target.remaining(), source.remaining() - (mySourceChar <= 0xff ? 1 : 2));
                            }
                            target.get();
                        } else {
                            charErrorBufferArray[charErrorBufferLength++] =
                                (char)(0xdc00+(char)(targetUniChar&0x3ff));
                        }
                    } else {
                        /* Call the callback function */
                        err = toUnicodeCallback(this, mySourceChar, targetUniChar);
                        break;
                    }
                } else { /* goes with "if (target.hasRemaining())" way up near the top of the function */
                    err = CoderResult.OVERFLOW;
                    break;
                }
            }
//endloop:
            return err;
        }
    } // end of class CharsetDecoderISO2022JP

    /****************************ISO-2022-CN************************************/
    private class CharsetDecoderISO2022CN extends CharsetDecoderICU {
        public CharsetDecoderISO2022CN(CharsetICU cs) {
            super(cs);
        }

        @Override
        protected void implReset() {
            super.implReset();
            myConverterData.reset();
        }

        @Override
        @SuppressWarnings("fallthrough")
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            byte[] tempBuf = new byte[3];
            int targetUniChar = 0x0000;
            int mySourceChar = 0x0000;
            int mySourceCharTemp = 0x0000;
            boolean gotoEscape = false;
            boolean gotoGetTrailByte = false;

            if (myConverterData.key != 0) {
                /* continue with a partial escape sequence */
                // goto escape;
                gotoEscape = true;
            } else if (toULength == 1 && source.hasRemaining() && target.hasRemaining()) {
                /* continue with a partial double-byte character */
                mySourceChar = (toUBytesArray[0] & UConverterConstants.UNSIGNED_BYTE_MASK);
                toULength = 0;
                targetUniChar = UConverterConstants.missingCharMarker;
                // goto getTrailByte
                gotoGetTrailByte = true;
            }

            while (source.hasRemaining() || gotoGetTrailByte || gotoEscape) {
                targetUniChar = UConverterConstants.missingCharMarker;

                if (target.hasRemaining() || gotoEscape) {
                    if (gotoEscape) {
                        mySourceChar = ESC_2022; // goto escape label
                        mySourceCharTemp = mySourceChar;
                    } else if (gotoGetTrailByte) {
                        mySourceCharTemp = 0xff; // goto getTrailByte; set mySourceCharTemp to go to default
                    } else {
                        mySourceChar = UConverterConstants.UNSIGNED_BYTE_MASK & source.get();
                        mySourceCharTemp = mySourceChar;
                    }

                    switch (mySourceCharTemp) {
                    case UConverterConstants.SI:
                        myConverterData.toU2022State.g = 0;
                        if (myConverterData.isEmptySegment) {
                            myConverterData.isEmptySegment = false; /* we are handling it, reset to avoid future spurious errors */
                            err = CoderResult.malformedForLength(1);
                            this.toUBytesArray[0] = (byte)mySourceChar;
                            this.toULength = 1;
                            return err;
                        }
                        continue;

                    case UConverterConstants.SO:
                        if (myConverterData.toU2022State.cs[1] != 0) {
                            myConverterData.toU2022State.g = 1;
                            myConverterData.isEmptySegment = true;  /* Begin a new segment, empty so far */
                            continue;
                        } else {
                            /* illegal to have SO before a matching designator */
                            myConverterData.isEmptySegment = false; /* Handling a different error, reset this to avoid future spurious errs */
                            break;
                        }

                    case ESC_2022:
                        if (!gotoEscape) {
                            source.position(source.position()-1);
                        }
// escape label
                        gotoEscape = false;
                        {
                            int mySourceBefore = source.position();
                            int toULengthBefore = this.toULength;

                            err = changeState_2022(this, source, ISO_2022_CN);

                            /* After SO there must be at least one character before a designator (designator error handled separately) */
                            if(myConverterData.key == 0 && !err.isError() && myConverterData.isEmptySegment) {
                                err = CoderResult.malformedForLength(source.position() - mySourceBefore);
                                this.toULength = toULengthBefore + (source.position() - mySourceBefore);
                            }
                        }

                        /* invalid or illegal escape sequence */
                        if(err.isError()){
                            myConverterData.isEmptySegment = false; /* Reset to avoid future spurious errors */
                            return err;
                        }
                        continue;

                    /*ISO-2022-CN does not use single-byte (C1) SS2 and SS3 */
                    case CR:
                        /* falls through */
                    case LF:
                        myConverterData.toU2022State.reset();
                        /* falls through */
                    default:
                        /* converter one or two bytes */
                        myConverterData.isEmptySegment = false;
                        if (myConverterData.toU2022State.g != 0 || gotoGetTrailByte) {
                            if (source.hasRemaining() || gotoGetTrailByte) {
                                UConverterSharedData cnv;
                                byte tempState;
                                int tempBufLen;
                                boolean leadIsOk, trailIsOk;
                                short trailByte;
// getTrailByte: label
                                gotoGetTrailByte = false; // reset gotoGetTrailByte

                                trailByte = (short)(source.get(source.position()) & UConverterConstants.UNSIGNED_BYTE_MASK);
                                /*
                                 * Ticket 5691: consistent illegal sequences:
                                 * - We include at least the first byte in the illegal sequence.
                                 * - If any of the non-initial bytes could be the start of a character,
                                 *   we stop the illegal sequence before the first one of those.
                                 *
                                 * In ISO-2022 DBCS, if the second byte is in the range 21..7e range or is
                                 * an ESC/SO/SI, we report only the first byte as the illegal sequence.
                                 * Otherwise we convert or report the pair of bytes.
                                 */
                                leadIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (mySourceChar - 0x21)) <= (0x7e - 0x21);
                                trailIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (trailByte - 0x21)) <= (0x7e - 0x21);
                                if (leadIsOk && trailIsOk) {
                                    source.get();
                                    tempState = myConverterData.toU2022State.cs[myConverterData.toU2022State.g];
                                    if (tempState > CNS_11643_0) {
                                        cnv = myConverterData.myConverterArray[CNS_11643];
                                        tempBuf[0] = (byte)(0x80 + (tempState - CNS_11643_0));
                                        tempBuf[1] = (byte)mySourceChar;
                                        tempBuf[2] = (byte)trailByte;
                                        tempBufLen = 3;
                                    } else {
                                        cnv = myConverterData.myConverterArray[tempState];
                                        tempBuf[0] = (byte)mySourceChar;
                                        tempBuf[1] = (byte)trailByte;
                                        tempBufLen = 2;
                                    }
                                    ByteBuffer tempBuffer = ByteBuffer.wrap(tempBuf);
                                    tempBuffer.limit(tempBufLen);
                                    targetUniChar = MBCSSimpleGetNextUChar(cnv, tempBuffer, false);
                                    mySourceChar = (mySourceChar << 8) | trailByte;

                                } else if (!(trailIsOk || IS_2022_CONTROL(trailByte))) {
                                    /* report a pair of illegal bytes if the second byte is not a DBCS starter */
                                    source.get();
                                    /* add another bit so that the code below writes 2 bytes in case of error */
                                    mySourceChar = 0x10000 | (mySourceChar << 8) | trailByte;
                                }
                                if (myConverterData.toU2022State.g >= 2) {
                                    /* return from a single-shift state to the previous one */
                                    myConverterData.toU2022State.g = myConverterData.toU2022State.prevG;
                                }
                            } else {
                                toUBytesArray[0] = (byte)mySourceChar;
                                toULength = 1;
                                // goto endloop;
                                return err;
                            }
                        } else {
                            if (mySourceChar <= 0x7f) {
                                targetUniChar = (char)mySourceChar;
                            }
                        }
                        break;
                    }
                    if ((UConverterConstants.UNSIGNED_INT_MASK&targetUniChar) < (UConverterConstants.UNSIGNED_INT_MASK&(UConverterConstants.missingCharMarker-1))) {
                        if (offsets != null) {
                            offsets.array()[target.position()] = source.remaining() - (mySourceChar <= 0xff ? 1 : 2);
                        }
                        target.put((char)targetUniChar);
                    } else if ((UConverterConstants.UNSIGNED_INT_MASK&targetUniChar) > (UConverterConstants.UNSIGNED_INT_MASK&(UConverterConstants.missingCharMarker))) {
                        /* disassemble the surrogate pair and write to output */
                        targetUniChar -= 0x0010000;
                        target.put((char)(0xd800+(char)(targetUniChar>>10)));
                        if (offsets != null) {
                            offsets.array()[target.position()-1] = source.position() - (mySourceChar <= 0xff ? 1 : 2);
                        }
                        if (target.hasRemaining()) {
                            target.put((char)(0xdc00+(char)(targetUniChar&0x3ff)));
                            if (offsets != null) {
                                offsets.array()[target.position()-1] = source.position() - (mySourceChar <= 0xff ? 1 : 2);
                            }
                        } else {
                            charErrorBufferArray[charErrorBufferLength++] = (char)(0xdc00+(char)(targetUniChar&0x3ff));
                        }
                    } else {
                        /* Call the callback function */
                        err = toUnicodeCallback(this, mySourceChar, targetUniChar);
                        break;
                    }

                } else {
                    err = CoderResult.OVERFLOW;
                    break;
                }
            }

            return err;
        }

    }
    /************************ ISO-2022-KR ********************/
    private class CharsetDecoderISO2022KR extends CharsetDecoderICU {
        public CharsetDecoderISO2022KR(CharsetICU cs) {
            super(cs);
        }

        @Override
        protected void implReset() {
            super.implReset();
            setInitialStateToUnicodeKR();
            myConverterData.reset();
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            int mySourceChar = 0x0000;
            int targetUniChar = 0x0000;
            byte[] tempBuf = new byte[2];
            boolean usingFallback;
            boolean gotoGetTrailByte = false;
            boolean gotoEscape = false;

            if (myConverterData.version == 1) {
                return decodeLoopIBM(myConverterData.currentDecoder, source, target, offsets, flush);
            }

            /* initialize state */
            usingFallback = isFallbackUsed();

            if (myConverterData.key != 0) {
                /* continue with a partial escape sequence */
                gotoEscape = true;
            } else if (toULength == 1 && source.hasRemaining() && target.hasRemaining()) {
                /* continue with a partial double-byte character */
                mySourceChar = (toUBytesArray[0] & UConverterConstants.UNSIGNED_BYTE_MASK);
                toULength = 0;
                gotoGetTrailByte = true;
            }

            while (source.hasRemaining() || gotoGetTrailByte || gotoEscape) {
                if (target.hasRemaining() || gotoGetTrailByte || gotoEscape) {
                    if (!gotoGetTrailByte && !gotoEscape) {
                        mySourceChar = (char)(source.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
                    }

                    if (!gotoGetTrailByte && !gotoEscape && mySourceChar == UConverterConstants.SI) {
                        myConverterData.toU2022State.g = 0;
                        if (myConverterData.isEmptySegment) {
                            myConverterData.isEmptySegment = false; /* we are handling it, reset to avoid future spurious errors */
                            err = CoderResult.malformedForLength(1);
                            this.toUBytesArray[0] = (byte)mySourceChar;
                            this.toULength = 1;
                            return err;
                        }
                        /* consume the source */
                        continue;
                    } else if (!gotoGetTrailByte && !gotoEscape && mySourceChar == UConverterConstants.SO) {
                        myConverterData.toU2022State.g = 1;
                        myConverterData.isEmptySegment = true;
                        /* consume the source */
                        continue;
                    } else if (!gotoGetTrailByte && (gotoEscape || mySourceChar == ESC_2022)) {
                        if (!gotoEscape) {
                            source.position(source.position()-1);
                        }
// escape label
                        gotoEscape = false; // reset gotoEscape flag
                        myConverterData.isEmptySegment = false; /* Any invalid ESC sequences will be detected separately, so just reset this */
                        err = changeState_2022(this, source, ISO_2022_KR);
                        if (err.isError()) {
                            return err;
                        }
                        continue;
                    }
                    myConverterData.isEmptySegment = false; /* Any invalid char errors will be detected separately, so just reset this */
                    if (myConverterData.toU2022State.g == 1 || gotoGetTrailByte) {
                        if (source.hasRemaining() || gotoGetTrailByte) {
                            boolean leadIsOk, trailIsOk;
                            short trailByte;
// getTrailByte label
                            gotoGetTrailByte = false; // reset gotoGetTrailByte flag

                            trailByte = (short)(source.get(source.position()) & UConverterConstants.UNSIGNED_BYTE_MASK);
                            targetUniChar = UConverterConstants.missingCharMarker;
                            /*
                             * Ticket 5691: consistent illegal sequences:
                             * - We include at least the first byte in the illegal sequence.
                             * - If any of the non-initial bytes could be the start of a character,
                             *   we stop the illegal sequence before the first one of those.
                             *
                             * In ISO-2022 DBCS, if the second byte is in the 21..7e range or is
                             * an ESC/SO/SI, we report only the first byte as the illegal sequence.
                             * Otherwise we convert or report the pair of bytes.
                             */
                            leadIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (mySourceChar - 0x21)) <= (0x7e - 0x21);
                            trailIsOk = (short)(UConverterConstants.UNSIGNED_BYTE_MASK & (trailByte - 0x21)) <= (0x7e - 0x21);
                            if (leadIsOk && trailIsOk) {
                                source.get();
                                tempBuf[0] = (byte)(mySourceChar + 0x80);
                                tempBuf[1] = (byte)(trailByte + 0x80);
                                targetUniChar = MBCSSimpleGetNextUChar(myConverterData.currentConverter.sharedData, ByteBuffer.wrap(tempBuf), usingFallback);
                                mySourceChar = (char)((mySourceChar << 8) | trailByte);
                            } else if (!(trailIsOk || IS_2022_CONTROL(trailByte))) {
                                /* report a pair of illegal bytes if the second byte is not a DBCS starter */
                                source.get();
                                /* add another bit so that the code below writes 2 bytes in case of error */
                                mySourceChar = (char)(0x10000 | (mySourceChar << 8) | trailByte);
                            }
                        } else {
                            toUBytesArray[0] = (byte)mySourceChar;
                            toULength = 1;
                            break;
                        }
                    } else if (mySourceChar <= 0x7f) {
                        int savedSourceLimit = source.limit();
                        int savedSourcePosition = source.position();
                        source.limit(source.position());
                        source.position(source.position()-1);
                        targetUniChar = MBCSSimpleGetNextUChar(myConverterData.currentConverter.sharedData, source, usingFallback);
                        source.limit(savedSourceLimit);
                        source.position(savedSourcePosition);
                    } else {
                        targetUniChar = 0xffff;
                    }
                    if (targetUniChar < 0xfffe) {
                        target.put((char)targetUniChar);
                        if (offsets != null) {
                            offsets.array()[target.position()] = source.position() - (mySourceChar <= 0xff ? 1 : 2);
                        }
                    } else {
                        /* Call the callback function */
                        err = toUnicodeCallback(this, mySourceChar, targetUniChar);
                        break;
                    }
                } else {
                    err = CoderResult.OVERFLOW;
                    break;
                }
            }

            return err;
        }

        protected CoderResult decodeLoopIBM(CharsetDecoderMBCS cnv, ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            int sourceStart;
            int sourceLimit;
            int argSource;
            int argTarget;
            boolean gotoEscape = false;
            int oldSourceLimit;

            /* remember the original start of the input for offsets */
            sourceStart = argSource = source.position();

            if (myConverterData.key != 0) {
                /* continue with a partial escape sequence */
                gotoEscape = true;
            }

            while (gotoEscape || (!err.isError() && source.hasRemaining())) {
                if (!gotoEscape) {
                    /* Find the end of the buffer e.g : Next Escape Seq | end of Buffer */
                    int oldSourcePos = source.position();
                    sourceLimit = getEndOfBuffer_2022(source);
                    source.position(oldSourcePos);
                    if (source.position() != sourceLimit) {
                        /*
                         * get the current partial byte sequence
                         *
                         * it needs to be moved between the public and the subconverter
                         * so that the conversion frameword, which only sees the public
                         * converter, can handle truncated and illegal input etc.
                         */
                        if (toULength > 0) {
                            cnv.toUBytesArray = toUBytesArray.clone();
                        }
                        cnv.toULength = toULength;

                        /*
                         * Convert up to the end of the input, or to before the next escape character.
                         * Does not handle conversion extensions because the preToU[] state etc.
                         * is not copied.
                         */
                        argTarget = target.position();
                        oldSourceLimit = source.limit(); // save the old source limit change to new one
                        source.limit(sourceLimit);
                        err = myConverterData.currentDecoder.cnvMBCSToUnicodeWithOffsets(source, target, offsets, flush);
                        source.limit(oldSourceLimit); // restore source limit;
                        if (offsets != null && sourceStart != argSource) {
                            /* update offsets to base them on the actual start of the input */
                            int delta = argSource - sourceStart;
                            while (argTarget < target.position()) {
                                int currentOffset = offsets.get();
                                offsets.position(offsets.position()-1);
                                if (currentOffset >= 0) {
                                    offsets.put(currentOffset + delta);
                                    offsets.position(offsets.position()-1);
                                }
                                offsets.get();
                                target.get();
                            }
                        }
                        argSource = source.position();

                        /* copy input/error/overflow buffers */
                        if (cnv.toULength > 0) {
                            toUBytesArray = cnv.toUBytesArray.clone();
                        }
                        toULength = cnv.toULength;

                        if (err.isOverflow()) {
                            if (cnv.charErrorBufferLength > 0) {
                                charErrorBufferArray = cnv.charErrorBufferArray.clone();
                            }
                            charErrorBufferLength = cnv.charErrorBufferLength;
                            cnv.charErrorBufferLength = 0;
                        }
                    }

                    if (err.isError() || err.isOverflow() || (source.position() == source.limit())) {
                        return err;
                    }
                }
// escape label
                gotoEscape = false;
                err = changeState_2022(this, source, ISO_2022_KR);
            }
            return err;
        }
    }

    /******************** from unicode **********************/
    /* preference order of JP charsets */
    private final static byte []jpCharsetPref = {
        ASCII,
        JISX201,
        ISO8859_1,
        JISX208,
        ISO8859_7,
        JISX212,
        GB2312,
        KSC5601,
        HWKANA_7BIT
    };
    /*
     * The escape sequences must be in order of the enum constants like JISX201 = 3,
     * not in order of jpCharsetPref[]!
     */
    private final static byte [][]escSeqChars = {
            { 0x1B, 0x28, 0x42},        /* <ESC>(B  ASCII       */
            { 0x1B, 0x2E, 0x41},        /* <ESC>.A  ISO-8859-1  */
            { 0x1B, 0x2E, 0x46},        /* <ESC>.F  ISO-8859-7  */
            { 0x1B, 0x28, 0x4A},        /* <ESC>(J  JISX-201    */
            { 0x1B, 0x24, 0x42},        /* <ESC>$B  JISX-208    */
            { 0x1B, 0x24, 0x28, 0x44},  /* <ESC>$(D JISX-212    */
            { 0x1B, 0x24, 0x41},        /* <ESC>$A  GB2312      */
            { 0x1B, 0x24, 0x28, 0x43},  /* <ESC>$(C KSC5601     */
            { 0x1B, 0x28, 0x49}         /* <ESC>(I  HWKANA_7BIT */
    };
    /*
     * JIS X 0208 has fallbacks from Unicode half-width Katakana to full-width (DBCS)
     * Katakana.
     * Now that we use a Shift-JIS table for JIS X 0208 we need to hardcode these fallbacks
     * because Shift-JIS roundtrips half-width Katakana to single bytes.
     * These were the only fallbacks in ICU's jisx-208.ucm file.
     */
    private final static char []hwkana_fb = {
        0x2123,  /* U+FF61 */
        0x2156,
        0x2157,
        0x2122,
        0x2126,
        0x2572,
        0x2521,
        0x2523,
        0x2525,
        0x2527,
        0x2529,
        0x2563,
        0x2565,
        0x2567,
        0x2543,
        0x213C,  /* U+FF70 */
        0x2522,
        0x2524,
        0x2526,
        0x2528,
        0x252A,
        0x252B,
        0x252D,
        0x252F,
        0x2531,
        0x2533,
        0x2535,
        0x2537,
        0x2539,
        0x253B,
        0x253D,
        0x253F,  /* U+FF80 */
        0x2541,
        0x2544,
        0x2546,
        0x2548,
        0x254A,
        0x254B,
        0x254C,
        0x254D,
        0x254E,
        0x254F,
        0x2552,
        0x2555,
        0x2558,
        0x255B,
        0x255E,
        0x255F,  /* U+FF90 */
        0x2560,
        0x2561,
        0x2562,
        0x2564,
        0x2566,
        0x2568,
        0x2569,
        0x256A,
        0x256B,
        0x256C,
        0x256D,
        0x256F,
        0x2573,
        0x212B,
        0x212C   /* U+FF9F */
    };

    protected byte [][]fromUSubstitutionChar = new byte[][]{ { (byte)0x1A }, { (byte)0x2F, (byte)0x7E} };
    /****************************ISO-2022-JP************************************/
    private class CharsetEncoderISO2022JP extends CharsetEncoderICU {
        public CharsetEncoderISO2022JP(CharsetICU cs) {
            super(cs, fromUSubstitutionChar[0]);
        }

        @Override
        protected void implReset() {
            super.implReset();
            myConverterData.reset();
        }
        /* Map Unicode to 00..7F according to JIS X 0201. Return U+FFFE if unmappable. */
        private int jisx201FromU(int value) {
            if (value <= 0x7f) {
                if (value != 0x5c && value != 0x7e) {
                    return value;
                }
            } else if (value == 0xa5) {
                return 0x5c;
            } else if (value == 0x203e) {
                return 0x7e;
            }
            return (int)(UConverterConstants.UNSIGNED_INT_MASK & 0xfffe);
        }

        /*
         * Take a valid Shift-JIS byte pair, check that it is in the range corresponding
         * to JIS X 0208, and convert it to a pair of 21..7E bytes.
         * Return 0 if the byte pair is out of range.
         */
        private int _2022FromSJIS(int value) {
            short trail;

            if (value > 0xEFFC) {
                return 0; /* beyond JIS X 0208 */
            }

            trail = (short)(value & UConverterConstants.UNSIGNED_BYTE_MASK);

            value &= 0xff00; /* lead byte */
            if (value <= 0x9f00) {
                value -= 0x7000;
            } else { /* 0xe000 <= value <= 0xef00 */
                value -= 0xb000;
            }

            value <<= 1;

            if (trail <= 0x9e) {
                value -= 0x100;
                if (trail <= 0x7e) {
                    value |= ((trail - 0x1f) & UConverterConstants.UNSIGNED_BYTE_MASK);
                } else {
                    value |= ((trail - 0x20) & UConverterConstants.UNSIGNED_BYTE_MASK);
                }
            } else { /* trail <= 0xfc */
                value |= ((trail - 0x7e) & UConverterConstants.UNSIGNED_BYTE_MASK);
            }

            return value;
        }
        /* This overrides the cbFromUWriteSub method in CharsetEncoderICU */
        @Override
        CoderResult cbFromUWriteSub (CharsetEncoderICU encoder,
                CharBuffer source, ByteBuffer target, IntBuffer offsets){
                CoderResult err = CoderResult.UNDERFLOW;
                byte[] buffer = new byte[8];
                int i = 0;
                byte[] subchar;
                subchar = encoder.replacement();

                byte cs;
                if (myConverterData.fromU2022State.g == 1) {
                    /* JIS7: switch from G1 to G0 */
                    myConverterData.fromU2022State.g = 0;
                    buffer[i++] = UConverterConstants.SI;
                }
                cs = myConverterData.fromU2022State.cs[0];

                if (cs != ASCII && cs != JISX201) {
                    /* not in ASCII or JIS X 0201: switch to ASCII */
                    myConverterData.fromU2022State.cs[0] = ASCII;
                    buffer[i++] = 0x1B;
                    buffer[i++] = 0x28;
                    buffer[i++] = 0x42;
                }

                buffer[i++] = subchar[0];

                err = CharsetEncoderICU.fromUWriteBytes(this, buffer, 0, i, target, offsets, source.position() - 1);

                return err;
            }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            int sourceChar;
            byte cs, g;
            int choiceCount;
            int len, outLen;
            byte[] choices = new byte[10];
            int targetValue = 0;
            boolean usingFallback;
            byte[] buffer = new byte[8];
            boolean getTrail = false; // use for getTrail label
            int oldSourcePos; // for proper error handling

            choiceCount = 0;

            /* check if the last codepoint of previous buffer was a lead surrogate */
            if ((sourceChar = fromUChar32) != 0 && target.hasRemaining()) {
                getTrail = true;
            }

            while (getTrail || source.hasRemaining()) {
                if (getTrail || target.hasRemaining()) {
                    oldSourcePos = source.position();
                    if (!getTrail) { /* skip if going to getTrail label */
                        sourceChar = source.get();
                    }
                    /* check if the char is a First surrogate */
                    if (getTrail || UTF16.isSurrogate((char)sourceChar)) {
                        if (getTrail || UTF16.isLeadSurrogate((char)sourceChar)) {
// getTrail:
                            if (getTrail) {
                                getTrail = false;
                            }
                            /* look ahead to find the trail surrogate */
                            if (source.hasRemaining()) {
                                /* test the following code unit */
                                char trail = source.get();
                                /* go back to the previous position */
                                source.position(source.position()-1);
                                if (UTF16.isTrailSurrogate(trail)) {
                                    source.get();
                                    sourceChar = UCharacter.getCodePoint((char)sourceChar, trail);
                                    fromUChar32 = 0x00;
                                    /* convert this supplementary code point */
                                    /* exit this condition tree */
                                } else {
                                    /* this is an unmatched lead code unit (1st surrogate) */
                                    /* callback(illegal) */
                                    err = CoderResult.malformedForLength(1);
                                    fromUChar32 = sourceChar;
                                    break;
                                }
                            } else {
                                /* no more input */
                                fromUChar32 = sourceChar;
                                break;
                            }
                        } else {
                            /* this is an unmatched trail code unit (2nd surrogate) */
                            /* callback(illegal) */
                            err = CoderResult.malformedForLength(1);
                            fromUChar32 = sourceChar;
                            break;
                        }
                    }

                    /* do not convert SO/SI/ESC */
                    if (IS_2022_CONTROL(sourceChar)) {
                        /* callback(illegal) */
                        err = CoderResult.malformedForLength(1);
                        fromUChar32 = sourceChar;
                        break;
                    }

                    /* do the conversion */

                    if (choiceCount == 0) {
                        char csm;
                        /*
                         * The csm variable keeps track of which charsets are allowed
                         * and not used yet while building the choices[].
                         */
                        csm = (char)jpCharsetMasks[myConverterData.version];
                        choiceCount = 0;

                        /* JIS7/8: try single-byte half-width Katakana before JISX208 */
                        if (myConverterData.version == 3 || myConverterData.version == 4) {
                            choices[choiceCount++] = HWKANA_7BIT;
                        }
                        /* Do not try single-bit half-width Katakana for other versions. */
                        csm &= ~CSM(HWKANA_7BIT);

                        /* try the current G0 charset */
                        choices[choiceCount++] = cs = myConverterData.fromU2022State.cs[0];
                        csm &= ~CSM(cs);

                        /* try the current G2 charset */
                        if ((cs = myConverterData.fromU2022State.cs[2]) != 0) {
                            choices[choiceCount++] = cs;
                            csm &= ~CSM(cs);
                        }

                        /* try all the other charsets */
                        for (int i = 0; i < jpCharsetPref.length; i++) {
                            cs = jpCharsetPref[i];
                            if ((CSM(cs) & csm) != 0) {
                                choices[choiceCount++] = cs;
                                csm &= ~CSM(cs);
                            }
                        }
                    }

                    cs = g = 0;
                    /*
                     * len==0:  no mapping found yet
                     * len<0:   found a fallback result:  continue looking for a roundtrip but no further fallbacks
                     * len>0:   found a roundtrip result, done
                     */
                    len = 0;
                    /*
                     * We will turn off usingFallBack after finding a fallback,
                     * but we still get fallbacks from PUA code points as usual.
                     * Therefore, we will also need to check that we don't overwrite
                     * an early fallback with a later one.
                     */
                    usingFallback = useFallback;

                    for (int i = 0; i < choiceCount && len <= 0; i++) {
                        int[] value = new int[1];
                        int len2;
                        byte cs0 = choices[i];
                        switch (cs0) {
                        case ASCII:
                            if (sourceChar <= 0x7f) {
                                targetValue = sourceChar;
                                len = 1;
                                cs = cs0;
                                g = 0;
                            }
                            break;
                        case ISO8859_1:
                            if (GR96_START <= sourceChar && sourceChar <= GR96_END) {
                                targetValue = sourceChar - 0x80;
                                len = 1;
                                cs = cs0;
                                g = 2;
                            }
                            break;
                        case HWKANA_7BIT:
                            if (sourceChar <= HWKANA_END && sourceChar >= HWKANA_START) {
                                if (myConverterData.version == 3) {
                                    /* JIS7: use G1 (SO) */
                                    /* Shift U+FF61..U+FF9F to bytes 21..5F. */
                                    targetValue = (int)(UConverterConstants.UNSIGNED_INT_MASK & (sourceChar - (HWKANA_START - 0x21)));
                                    len = 1;
                                    myConverterData.fromU2022State.cs[1] = cs = cs0; /* do not output an escape sequence */
                                    g = 1;
                                } else if (myConverterData.version == 4) {
                                    /* JIS8: use 8-bit bytes with any single-byte charset, see escape sequence output below */
                                    /* Shift U+FF61..U+FF9F to bytes A1..DF. */
                                    targetValue = (int)(UConverterConstants.UNSIGNED_INT_MASK & (sourceChar - (HWKANA_START - 0xa1)));
                                    len = 1;

                                    cs = myConverterData.fromU2022State.cs[0];
                                    if (IS_JP_DBCS(cs)) {
                                        /* switch from a DBCS charset to JISX201 */
                                        cs = JISX201;
                                    }
                                    /* else stay in the current G0 charset */
                                    g = 0;
                                }
                                /* else do not use HWKANA_7BIT with other versions */
                            }
                            break;
                        case JISX201:
                            /* G0 SBCS */
                            value[0] = jisx201FromU(sourceChar);
                            if (value[0] <= 0x7f) {
                                targetValue = value[0];
                                len = 1;
                                cs = cs0;
                                g = 0;
                                usingFallback = false;
                            }
                            break;
                        case JISX208:
                            /* G0 DBCS from JIS table */
                            myConverterData.currentConverter.sharedData = myConverterData.myConverterArray[cs0];
                            myConverterData.currentConverter.sharedData.mbcs.outputType = CharsetMBCS.MBCS_OUTPUT_2;
                            len2 = myConverterData.currentEncoder.fromUChar32(sourceChar, value, usingFallback);
                            //len2 = MBCSFromUChar32_ISO2022(myConverterData.myConverterArray[cs0], sourceChar, value, usingFallback, CharsetMBCS.MBCS_OUTPUT_2);
                            if (len2 == 2 || (len2 == -2 && len == 0)) { /* only accept DBCS: abs(len) == 2 */
                                value[0] = _2022FromSJIS(value[0]);
                                if (value[0] != 0) {
                                    targetValue = value[0];
                                    len = len2;
                                    cs = cs0;
                                    g = 0;
                                    usingFallback = false;
                                }
                            } else if (len == 0 && usingFallback  && sourceChar <= HWKANA_END && sourceChar >= HWKANA_START) {
                                targetValue = hwkana_fb[sourceChar - HWKANA_START];
                                len = -2;
                                cs = cs0;
                                g = 0;
                                usingFallback = false;
                            }
                            break;
                        case ISO8859_7:
                            /* G0 SBCS forced to 7-bit output */
                            len2 = MBCSSingleFromUChar32(myConverterData.myConverterArray[cs0], sourceChar, value, usingFallback);
                            if (len2 != 0 && !(len2 < 0 && len != 0) && GR96_START <= value[0] && value[0] <= GR96_END) {
                                targetValue = value[0] - 0x80;
                                len = len2;
                                cs = cs0;
                                g = 2;
                                usingFallback = false;
                            }
                            break;
                        default :
                            /* G0 DBCS */
                            myConverterData.currentConverter.sharedData = myConverterData.myConverterArray[cs0];
                            myConverterData.currentConverter.sharedData.mbcs.outputType = CharsetMBCS.MBCS_OUTPUT_2;
                            len2 = myConverterData.currentEncoder.fromUChar32(sourceChar, value, usingFallback);
                            //len2 = MBCSFromUChar32_ISO2022(myConverterData.myConverterArray[cs0], sourceChar, value, usingFallback, CharsetMBCS.MBCS_OUTPUT_2);
                            if (len2 == 2 || (len2 == -2 && len == 0)) { /* only accept DBCS: abs(len)==2 */
                                if (cs0 == KSC5601) {
                                    /*
                                     * Check for valid bytes for the encoding scheme.
                                     * This is necessary because the sub-converter (windows-949)
                                     * has a broader encoding scheme than is valid for 2022.
                                     */
                                    value[0] = _2022FromGR94DBCS(value[0]);
                                    if (value[0] == 0) {
                                        break;
                                    }
                                }
                                targetValue = value[0];
                                len = len2;
                                cs = cs0;
                                g = 0;
                                usingFallback = false;
                            }
                            break;
                        }
                    }

                    if (len != 0) {
                        if (len < 0) {
                            len = -len; /* fallback */
                        }
                        outLen = 0;

                        /* write SI if necessary (only for JIS7 */
                        if (myConverterData.fromU2022State.g == 1 && g == 0) {
                            buffer[outLen++] = UConverterConstants.SI;
                            myConverterData.fromU2022State.g = 0;
                        }

                        /* write the designation sequence if necessary */
                        if (cs != myConverterData.fromU2022State.cs[g]) {
                            for (int i = 0; i < escSeqChars[cs].length; i++) {
                                buffer[outLen++] = escSeqChars[cs][i];
                            }
                            myConverterData.fromU2022State.cs[g] = cs;

                            /* invalidate the choices[] */
                            choiceCount = 0;
                        }

                        /* write the shift sequence if necessary */
                        if (g != myConverterData.fromU2022State.g) {
                            switch (g) {
                            /* case 0 handled before writing escapes */
                            case 1:
                                buffer[outLen++] = UConverterConstants.SO;
                                myConverterData.fromU2022State.g = 1;
                                break;
                            default : /* case 2 */
                                buffer[outLen++] = 0x1b;
                                buffer[outLen++] = 0x4e;
                                break;
                            /* case 3: no SS3 in ISO-2022-JP-x */
                            }
                        }

                        /* write the output bytes */
                        if (len == 1) {
                            buffer[outLen++] = (byte)targetValue;
                        } else { /* len == 2 */
                            buffer[outLen++] = (byte)(targetValue >> 8);
                            buffer[outLen++] = (byte)targetValue;
                        }
                    }else {
                        /*
                         * if we cannot find the character after checking all codepages
                         * then this is an error.
                         */
                        err = CoderResult.unmappableForLength(source.position()-oldSourcePos);
                        fromUChar32 = sourceChar;
                        break;
                    }

                    if (sourceChar == CR || sourceChar == LF) {
                        /* reset the G2 state at the end of a line (conversion got use into ASCII or JISX201 already) */
                        myConverterData.fromU2022State.cs[2] = 0;
                        choiceCount = 0;
                    }

                    /* output outLen>0 bytes in buffer[] */
                    if (outLen == 1) {
                        target.put(buffer[0]);
                        if (offsets != null) {
                            offsets.put(source.remaining() - 1); /* -1 known to be ASCII */
                        }
                    } else if (outLen == 2 && (target.position() + 2) <= target.limit()) {
                        target.put(buffer[0]);
                        target.put(buffer[1]);
                        if (offsets != null) {
                            int sourceIndex = source.position() - 1;
                            offsets.put(sourceIndex);
                            offsets.put(sourceIndex);
                        }
                    } else {
                        err = CharsetEncoderICU.fromUWriteBytes(this, buffer, 0, outLen, target, offsets, source.position()-1);
                    }
                } else {
                    err = CoderResult.OVERFLOW;
                    break;
                }
            }

            /*
             * the end of the input stream and detection of truncated input
             * are handled by the framework, but for ISO-2022-JP conversion
             * we need to be in ASCII mode at the very end
             *
             * conditions:
             *  successful
             *  in SO mode or not in ASCII mode
             *  end of input and no truncated input
             */
            if (!err.isError() &&
                    (myConverterData.fromU2022State.g != 0 || myConverterData.fromU2022State.cs[0] != ASCII) &&
                    flush && !source.hasRemaining() && fromUChar32 == 0) {
                int sourceIndex;

                outLen = 0;

                if (myConverterData.fromU2022State.g != 0) {
                    buffer[outLen++] = UConverterConstants.SI;
                    myConverterData.fromU2022State.g = 0;
                }

                if (myConverterData.fromU2022State.cs[0] != ASCII) {
                    for (int i = 0; i < escSeqChars[ASCII].length; i++) {
                        buffer[outLen++] = escSeqChars[ASCII][i];
                    }
                    myConverterData.fromU2022State.cs[0] = ASCII;
                }

                /* get the source index of the last input character */
                sourceIndex = source.position();
                if (sourceIndex > 0) {
                    --sourceIndex;
                    if (UTF16.isTrailSurrogate(source.get(sourceIndex)) &&
                            (sourceIndex == 0 || UTF16.isLeadSurrogate(source.get(sourceIndex-1)))) {
                        --sourceIndex;
                    }
                } else {
                    sourceIndex = -1;
                }

                err = CharsetEncoderICU.fromUWriteBytes(this, buffer, 0, outLen, target, offsets, sourceIndex);
            }
            return err;
        }
    }
    /****************************ISO-2022-CN************************************/
    /*
     * Rules for ISO-2022-CN Encoding:
     * i)   The designator sequence must appear once on a line before any instance
     *      of chracter set it designates.
     * ii)  If two lines contain characters from the same character set, both lines
     *      must include the designator sequence.
     * iii) Once the designator sequence is known, a shifting sequence has to be found
     *      to invoke the shifting
     * iv)  All lines start in ASCII and end in ASCII.
     * v)   Four shifting sequences are employed for this purpose:
     *      Sequence    ASCII Eq    Charsets
     *      ---------   ---------   --------
     *      SI          <SI>        US-ASCII
     *      SO          <SO>        CNS-11643-1992 Plane 1, GB2312, ISO-IR-165
     *      SS2         <ESC>N      CNS-11643-1992 Plane 2
     *      SS3         <ESC>O      CNS-11643-1992 Planes 3-7
     * vi)
     *      SOdesignator    : ESC "$" ")" finalchar_for_SO
     *      SS2designator   : ESC "$" "*" finalchar_for_SS2
     *      SS3designator   : ESC "$" "+" finalchar_for_SS3
     *
     *      ESC $ ) A       Indicates the bytes following SO are Chinese
     *       characters as defined in GB 2312-80, until
     *       another SOdesignation appears
     *
     *      ESC $ ) E       Indicates the bytes following SO are as defined
     *       in ISO-IR-165 (for details, see section 2.1),
     *       until another SOdesignation appears
     *
     *      ESC $ ) G       Indicates the bytes following SO are as defined
     *       in CNS 11643-plane-1, until another SOdesignation appears
     *
     *      ESC $ * H       Indicates teh two bytes immediately following
     *       SS2 is a Chinese character as defined in CNS
     *       11643-plane-2, until another SS2designation
     *       appears
     *       (Meaning <ESC>N must preceed ever 2 byte sequence.)
     *
     *      ESC $ + I       Indicates the immediate two bytes following SS3
     *       is a Chinese character as defined in CNS
     *       11643-plane-3, until another SS3designation
     *       appears
     *       (Meaning <ESC>O must preceed every 2 byte sequence.)
     *
     *      ESC $ + J       Indicates the immediate two bytes following SS3
     *       is a Chinese character as defined in CNS
     *       11643-plane-4, until another SS3designation
     *       appears
     *       (In English: <ESC>O must preceed every 2 byte sequence.)
     *
     *      ESC $ + K       Indicates the immediate two bytes following SS3
     *       is a Chinese character as defined in CNS
     *       11643-plane-5, until another SS3designation
     *       appears
     *
     *      ESC $ + L       Indicates the immediate two bytes following SS3
     *       is a Chinese character as defined in CNS
     *       11643-plane-6, until another SS3designation
     *       appears
     *
     *      ESC $ + M       Indicates the immediate two bytes following SS3
     *       is a Chinese character as defined in CNS
     *       11643-plane-7, until another SS3designation
     *       appears
     *
     *      As in ISO-2022-CN, each line starts in ASCII, and ends in ASCII, and
     *      has its own designation information before any Chinese chracters
     *      appears
     */

    /* The following are defined this way to make strings truely readonly */
    private final static byte[] GB_2312_80_STR = { 0x1B, 0x24, 0x29, 0x41 };
    private final static byte[] ISO_IR_165_STR = { 0x1B, 0x24, 0x29, 0x45 };
    private final static byte[] CNS_11643_1992_Plane_1_STR = { 0x1B, 0x24, 0x29, 0x47 };
    private final static byte[] CNS_11643_1992_Plane_2_STR = { 0x1B, 0x24, 0x2A, 0x48 };
    private final static byte[] CNS_11643_1992_Plane_3_STR = { 0x1B, 0x24, 0x2B, 0x49 };
    private final static byte[] CNS_11643_1992_Plane_4_STR = { 0x1B, 0x24, 0x2B, 0x4A };
    private final static byte[] CNS_11643_1992_Plane_5_STR = { 0x1B, 0x24, 0x2B, 0x4B };
    private final static byte[] CNS_11643_1992_Plane_6_STR = { 0x1B, 0x24, 0x2B, 0x4C };
    private final static byte[] CNS_11643_1992_Plane_7_STR = { 0x1B, 0x24, 0x2B, 0x4D };

    /************************ ISO2022-CN Data *****************************/
    private final static byte[][] escSeqCharsCN = {
        SHIFT_IN_STR,
        GB_2312_80_STR,
        ISO_IR_165_STR,
        CNS_11643_1992_Plane_1_STR,
        CNS_11643_1992_Plane_2_STR,
        CNS_11643_1992_Plane_3_STR,
        CNS_11643_1992_Plane_4_STR,
        CNS_11643_1992_Plane_5_STR,
        CNS_11643_1992_Plane_6_STR,
        CNS_11643_1992_Plane_7_STR,
    };

    private class CharsetEncoderISO2022CN extends CharsetEncoderICU {
        public CharsetEncoderISO2022CN(CharsetICU cs) {
            super(cs, fromUSubstitutionChar[0]);
        }

        @Override
        protected void implReset() {
            super.implReset();
            myConverterData.reset();
        }

        /* This overrides the cbFromUWriteSub method in CharsetEncoderICU */
        @Override
        CoderResult cbFromUWriteSub (CharsetEncoderICU encoder,
            CharBuffer source, ByteBuffer target, IntBuffer offsets){
            CoderResult err = CoderResult.UNDERFLOW;
            byte[] buffer = new byte[8];
            int i = 0;
            byte[] subchar;
            subchar = encoder.replacement();

            if (myConverterData.fromU2022State.g != 0) {
                /* not in ASCII mode: switch to ASCII */
                myConverterData.fromU2022State.g = 0;
                buffer[i++] = UConverterConstants.SI;
            }
            buffer[i++] = subchar[0];

            err = CharsetEncoderICU.fromUWriteBytes(this, buffer, 0, i, target, offsets, source.position() - 1);

            return err;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            int sourceChar;
            byte[] buffer = new byte[8];
            int len;
            byte[] choices = new byte[3];
            int choiceCount;
            int targetValue = 0;
            boolean usingFallback;
            boolean gotoGetTrail = false;
            int oldSourcePos; // For proper error handling

            choiceCount = 0;

            /* check if the last codepoint of previous buffer was a lead surrogate */
            if ((sourceChar = fromUChar32) != 0 && target.hasRemaining()) {
                // goto getTrail label
                gotoGetTrail = true;
            }

            while (source.hasRemaining() || gotoGetTrail) {
                if (target.hasRemaining() || gotoGetTrail) {
                    oldSourcePos = source.position();
                    if (!gotoGetTrail) {
                        sourceChar = source.get();
                    }
                    /* check if the char is a First surrogate */
                    if (UTF16.isSurrogate((char)sourceChar) || gotoGetTrail) {
                        if (UTF16.isLeadSurrogate((char)sourceChar) || gotoGetTrail) {
// getTrail label
                            /* reset gotoGetTrail flag*/
                             gotoGetTrail = false;

                            /* look ahead to find the trail surrogate */
                            if (source.hasRemaining()) {
                                /* test the following code unit */
                                char trail = source.get();
                                source.position(source.position()-1);
                                if (UTF16.isTrailSurrogate(trail)) {
                                    source.get();
                                    sourceChar = UCharacter.getCodePoint((char)sourceChar, trail);
                                    fromUChar32 = 0x00;
                                    /* convert this supplementary code point */
                                    /* exit this condition tree */
                                } else {
                                    /* this is an unmatched lead code unit (1st surrogate) */
                                    /* callback(illegal) */
                                    err = CoderResult.malformedForLength(1);
                                    fromUChar32 = sourceChar;
                                    break;
                                }
                            } else {
                                /* no more input */
                                fromUChar32 = sourceChar;
                                break;
                            }
                        } else {
                            /* this is an unmatched trail code unit (2nd surrogate) */
                            /* callback(illegal) */
                            err = CoderResult.malformedForLength(1);
                            fromUChar32 = sourceChar;
                            break;
                        }
                    }

                    /* do the conversion */
                    if (sourceChar <= 0x007f) {
                        /* do not converter SO/SI/ESC */
                        if (IS_2022_CONTROL(sourceChar)) {
                            /* callback(illegal) */
                            err = CoderResult.malformedForLength(1);
                            fromUChar32 = sourceChar;
                            break;
                        }

                        /* US-ASCII */
                        if (myConverterData.fromU2022State.g == 0) {
                            buffer[0] = (byte)sourceChar;
                            len = 1;
                        } else {
                            buffer[0] = UConverterConstants.SI;
                            buffer[1] = (byte)sourceChar;
                            len = 2;
                            myConverterData.fromU2022State.g = 0;
                            choiceCount = 0;
                        }

                        if (sourceChar == CR || sourceChar == LF) {
                            /* reset the state at the end of a line */
                            myConverterData.fromU2022State.reset();
                            choiceCount = 0;
                        }
                    } else {
                        /* convert U+0080..U+10ffff */
                        int i;
                        byte cs, g;

                        if (choiceCount == 0) {
                            /* try the current SO/G1 converter first */
                            choices[0] = myConverterData.fromU2022State.cs[1];

                            /* default to GB2312_1 if none is designated yet */
                            if (choices[0] == 0) {
                                choices[0] = GB2312_1;
                            }
                            if (myConverterData.version == 0) {
                                /* ISO-2022-CN */
                                /* try other SO/G1 converter; a CNS_11643_1 lookup may result in any plane */
                                if (choices[0] == GB2312_1) {
                                    choices[1] = CNS_11643_1;
                                } else {
                                    choices[1] = GB2312_1;
                                }

                                choiceCount = 2;
                            } else if (myConverterData.version == 1) {
                                /* ISO-2022-CN-EXT */

                                /* try one of the other converters */
                                switch (choices[0]) {
                                case GB2312_1:
                                    choices[1] = CNS_11643_1;
                                    choices[2] = ISO_IR_165;
                                    break;
                                case ISO_IR_165:
                                    choices[1] = GB2312_1;
                                    choices[2] = CNS_11643_1;
                                    break;
                                default :
                                    choices[1] = GB2312_1;
                                    choices[2] = ISO_IR_165;
                                    break;
                                }

                                choiceCount = 3;
                            } else {
                                /* ISO-2022-CN-CNS */
                                choices[0] = CNS_11643_1;
                                choices[1] = GB2312_1;

                                choiceCount = 2;
                            }
                        }

                        cs = g = 0;
                        /*
                         * len==0:  no mapping found yet
                         * len<0:   found a fallback result: continue looking for a roundtrip but no further fallbacks
                         * len>0:   found a roundtrip result, done
                         */
                        len = 0;
                        /*
                         * We will turn off usingFallback after finding a fallback,
                         * but we still get fallbacks from PUA code points as usual.
                         * Therefore, we will also need to check that we don't overwrite
                         * an early fallback with a later one.
                         */
                        usingFallback = useFallback;

                        for (i = 0; i < choiceCount && len <= 0; ++i) {
                            byte cs0 = choices[i];
                            if (cs0 > 0) {
                                int[] value = new int[1];
                                int len2;
                                if (cs0 > CNS_11643_0) {
                                    myConverterData.currentConverter.sharedData = myConverterData.myConverterArray[CNS_11643];
                                    myConverterData.currentConverter.sharedData.mbcs.outputType = CharsetMBCS.MBCS_OUTPUT_3;
                                    len2 = myConverterData.currentEncoder.fromUChar32(sourceChar, value, usingFallback);
                                    //len2 = MBCSFromUChar32_ISO2022(myConverterData.myConverterArray[CNS_11643],
                                    //        sourceChar, value, usingFallback, CharsetMBCS.MBCS_OUTPUT_3);
                                    if (len2 == 3 || (len2 == -3 && len == 0)) {
                                        targetValue = value[0];
                                        cs = (byte)(CNS_11643_0 + (value[0] >> 16) - 0x80);
                                        if (len2 >= 0) {
                                            len = 2;
                                        } else {
                                            len = -2;
                                            usingFallback = false;
                                        }
                                        if (cs == CNS_11643_1) {
                                            g = 1;
                                        } else if (cs == CNS_11643_2) {
                                            g = 2;
                                        } else if (myConverterData.version == 1) { /* plane 3..7 */
                                            g = 3;
                                        } else {
                                            /* ISO-2022-CN (without -EXT) does not support plane 3..7 */
                                            len = 0;
                                        }
                                    }
                                } else {
                                    /* GB2312_1 or ISO-IR-165 */
                                    myConverterData.currentConverter.sharedData = myConverterData.myConverterArray[cs0];
                                    myConverterData.currentConverter.sharedData.mbcs.outputType = CharsetMBCS.MBCS_OUTPUT_2;
                                    len2 = myConverterData.currentEncoder.fromUChar32(sourceChar, value, usingFallback);
                                    //len2 = MBCSFromUChar32_ISO2022(myConverterData.myConverterArray[cs0],
                                    //        sourceChar, value, usingFallback, CharsetMBCS.MBCS_OUTPUT_2);
                                    if (len2 == 2 || (len2 == -2 && len == 0)) {
                                        targetValue = value[0];
                                        len = len2;
                                        cs = cs0;
                                        g = 1;
                                        usingFallback = false;
                                    }
                                }
                            }
                        }

                        if (len != 0) {
                            len = 0; /* count output bytes; it must have ben abs(len) == 2 */

                            /* write the designation sequence if necessary */
                            if (cs != myConverterData.fromU2022State.cs[g]) {
                                if (cs < CNS_11643) {
                                    for (int n = 0; n < escSeqCharsCN[cs].length; n++) {
                                        buffer[n] = escSeqCharsCN[cs][n];
                                    }
                                } else {
                                    for (int n = 0; n < escSeqCharsCN[CNS_11643 + (cs - CNS_11643_1)].length; n++) {
                                        buffer[n] = escSeqCharsCN[CNS_11643 + (cs - CNS_11643_1)][n];
                                    }
                                }
                                len = 4;
                                myConverterData.fromU2022State.cs[g] = cs;
                                if (g == 1) {
                                    /* changing the SO/G1 charset invalidates the choices[] */
                                    choiceCount = 0;
                                }
                            }

                            /* write the shift sequence if necessary */
                            if (g != myConverterData.fromU2022State.g) {
                                switch (g) {
                                case 1:
                                    buffer[len++] = UConverterConstants.SO;

                                    /* set the new state only if it is the locking shift SO/G1, not for SS2 or SS3 */
                                    myConverterData.fromU2022State.g = 1;
                                    break;
                                case 2:
                                    buffer[len++] = 0x1b;
                                    buffer[len++] = 0x4e;
                                    break;
                                default: /* case 3 */
                                    buffer[len++] = 0x1b;
                                    buffer[len++] = 0x4f;
                                    break;
                                }
                            }

                            /* write the two output bytes */
                            buffer[len++] = (byte)(targetValue >> 8);
                            buffer[len++] = (byte)targetValue;
                        } else {
                            /* if we cannot find the character after checking all codepages
                             * then this is an error
                             */
                            err = CoderResult.unmappableForLength(source.position()-oldSourcePos);
                            fromUChar32 = sourceChar;
                            break;
                        }
                    }
                    /* output len>0 bytes in buffer[] */
                    if (len == 1) {
                        target.put(buffer[0]);
                        if (offsets != null) {
                            offsets.put(source.position()-1);
                        }
                    } else if (len == 2 && (target.remaining() >= 2)) {
                        target.put(buffer[0]);
                        target.put(buffer[1]);
                        if (offsets != null) {
                            int sourceIndex = source.position();
                            offsets.put(sourceIndex);
                            offsets.put(sourceIndex);
                        }
                    } else {
                        err = CharsetEncoderICU.fromUWriteBytes(this, buffer, 0, len, target, offsets, source.position()-1);
                        if (err.isError()) {
                            break;
                        }
                    }
                } else {
                    err = CoderResult.OVERFLOW;
                    break;
                }
            } /* end while (source.hasRemaining() */

            /*
             * the end of the input stream and detection of truncated input
             * are handled by the framework, but for ISO-2022-CN conversion
             * we need to be in ASCII mode at the very end
             *
             * conditions:
             *   succesful
             *   not in ASCII mode
             *   end of input and no truncated input
             */
            if (!err.isError() && myConverterData.fromU2022State.g != 0 && flush && !source.hasRemaining() && fromUChar32 == 0) {
                int sourceIndex;

                /* we are switching to ASCII */
                myConverterData.fromU2022State.g = 0;

                /* get the source index of the last input character */
                sourceIndex = source.position();
                if (sourceIndex > 0) {
                    --sourceIndex;
                    if (UTF16.isTrailSurrogate(source.get(sourceIndex)) &&
                            (sourceIndex == 0 || UTF16.isLeadSurrogate(source.get(sourceIndex-1)))) {
                        --sourceIndex;
                    }
                } else {
                    sourceIndex = -1;
                }

                err = CharsetEncoderICU.fromUWriteBytes(this, SHIFT_IN_STR, 0, 1, target, offsets, sourceIndex);
            }

            return err;
        }
    }
    /******************************** ISO-2022-KR *****************************/
    /*
     *   Rules for ISO-2022-KR encoding
     *   i) The KSC5601 designator sequence should appear only once in a file,
     *      at the beginning of a line before any KSC5601 characters. This usually
     *      means that it appears by itself on the first line of the file
     *  ii) There are only 2 shifting sequences SO to shift into double byte mode
     *      and SI to shift into single byte mode
     */
    private class CharsetEncoderISO2022KR extends CharsetEncoderICU {
        public CharsetEncoderISO2022KR(CharsetICU cs) {
            super(cs, fromUSubstitutionChar[myConverterData.version]);
        }

        @Override
        protected void implReset() {
            super.implReset();
            myConverterData.reset();
            setInitialStateFromUnicodeKR(this);
        }

        /* This overrides the cbFromUWriteSub method in CharsetEncoderICU */
        @Override
        CoderResult cbFromUWriteSub (CharsetEncoderICU encoder,
            CharBuffer source, ByteBuffer target, IntBuffer offsets){
            CoderResult err = CoderResult.UNDERFLOW;
            byte[] buffer = new byte[8];
            int length, i = 0;
            byte[] subchar;

            subchar = encoder.replacement();
            length = subchar.length;

            if (myConverterData.version == 0) {
                if (length == 1) {
                    if (encoder.fromUnicodeStatus != 0) {
                        /* in DBCS mode: switch to SBCS */
                        encoder.fromUnicodeStatus = 0;
                        buffer[i++] = UConverterConstants.SI;
                    }
                    buffer[i++] = subchar[0];
                } else { /* length == 2 */
                    if (encoder.fromUnicodeStatus == 0) {
                        /* in SBCS mode: switch to DBCS */
                        encoder.fromUnicodeStatus = 1;
                        buffer[i++] = UConverterConstants.SO;
                    }
                    buffer[i++] = subchar[0];
                    buffer[i++] = subchar[1];
                }
                err = CharsetEncoderICU.fromUWriteBytes(this, buffer, 0, i, target, offsets, source.position() - 1);
            } else {
                /* save the subvonverter's substitution string */
                byte[] currentSubChars = myConverterData.currentEncoder.replacement();

                /* set our substitution string into the subconverter */
                myConverterData.currentEncoder.replaceWith(subchar);
                myConverterData.currentConverter.subChar1 = fromUSubstitutionChar[0][0];
                /* let the subconverter write the subchar, set/retrieve fromUChar32 state */
                myConverterData.currentEncoder.fromUChar32 = encoder.fromUChar32;
                err = myConverterData.currentEncoder.cbFromUWriteSub(myConverterData.currentEncoder, source, target, offsets);
                encoder.fromUChar32 = myConverterData.currentEncoder.fromUChar32;

                /* restore the subconverter's substitution string */
                myConverterData.currentEncoder.replaceWith(currentSubChars);

                if (err.isOverflow()) {
                    if (myConverterData.currentEncoder.errorBufferLength > 0) {
                        encoder.errorBuffer = myConverterData.currentEncoder.errorBuffer.clone();
                    }
                    encoder.errorBufferLength = myConverterData.currentEncoder.errorBufferLength;
                    myConverterData.currentEncoder.errorBufferLength = 0;
                }
            }

            return err;
        }

        private CoderResult encodeLoopIBM(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;

            myConverterData.currentEncoder.fromUChar32 = fromUChar32;
            err = myConverterData.currentEncoder.cnvMBCSFromUnicodeWithOffsets(source, target, offsets, flush);
            fromUChar32 = myConverterData.currentEncoder.fromUChar32;

            if (err.isOverflow()) {
                if (myConverterData.currentEncoder.errorBufferLength > 0) {
                    errorBuffer = myConverterData.currentEncoder.errorBuffer.clone();
                }
                errorBufferLength = myConverterData.currentEncoder.errorBufferLength;
                myConverterData.currentEncoder.errorBufferLength = 0;
            }

            return err;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            int[] targetByteUnit = { 0x0000 };
            int sourceChar = 0x0000;
            boolean isTargetByteDBCS;
            boolean oldIsTargetByteDBCS;
            boolean usingFallback;
            int length = 0;
            boolean gotoGetTrail = false; // for goto getTrail label call

            /*
             * if the version is 1 then the user is requesting
             * conversion with ibm-25546 pass the argument to
             * MBCS converter and return
             */
            if (myConverterData.version == 1) {
                return encodeLoopIBM(source, target, offsets, flush);
            }

            usingFallback = useFallback;
            isTargetByteDBCS = fromUnicodeStatus == 0 ? false : true;
            if ((sourceChar = fromUChar32) != 0 && target.hasRemaining()) {
                gotoGetTrail = true;
            }

            while (source.hasRemaining() || gotoGetTrail) {
                targetByteUnit[0] = UConverterConstants.missingCharMarker;

                if (target.hasRemaining() || gotoGetTrail) {
                    if (!gotoGetTrail) {
                        sourceChar = source.get();

                        /* do not convert SO/SI/ESC */
                        if (IS_2022_CONTROL(sourceChar)) {
                            /* callback(illegal) */
                            err = CoderResult.malformedForLength(1);
                            fromUChar32 = sourceChar;
                            break;
                        }
                        myConverterData.currentConverter.sharedData.mbcs.outputType = CharsetMBCS.MBCS_OUTPUT_2;
                        length = myConverterData.currentEncoder.fromUChar32(sourceChar, targetByteUnit, usingFallback);
                        //length = MBCSFromUChar32_ISO2022(myConverterData.currentConverter.sharedData, sourceChar, targetByteUnit, usingFallback, CharsetMBCS.MBCS_OUTPUT_2);
                        if (length < 0) {
                            length = -length; /* fallback */
                        }
                        /* only DBCS or SBCS characters are expected */
                        /* DB characters with high bit set to 1 are expected */
                        if (length > 2 || length == 0 ||
                                (length == 1 && targetByteUnit[0] > 0x7f) ||
                                (length ==2 &&
                                        ((char)(targetByteUnit[0] - 0xa1a1) > (0xfefe - 0xa1a1) ||
                                        ((targetByteUnit[0] - 0xa1) & UConverterConstants.UNSIGNED_BYTE_MASK) > (0xfe - 0xa1)))) {
                            targetByteUnit[0] = UConverterConstants.missingCharMarker;
                        }
                    }
                    if (!gotoGetTrail && targetByteUnit[0] != UConverterConstants.missingCharMarker) {
                        oldIsTargetByteDBCS = isTargetByteDBCS;
                        isTargetByteDBCS = (targetByteUnit[0] > 0x00FF);
                        /* append the shift sequence */
                        if (oldIsTargetByteDBCS != isTargetByteDBCS) {
                            if (isTargetByteDBCS) {
                                target.put((byte)UConverterConstants.SO);
                            } else {
                                target.put((byte)UConverterConstants.SI);
                            }
                            if (offsets != null) {
                                offsets.put(source.position()-1);
                            }
                        }
                        /* write the targetUniChar to target */
                        if (targetByteUnit[0] <= 0x00FF) {
                            if (target.hasRemaining()) {
                                target.put((byte)targetByteUnit[0]);
                                if (offsets != null) {
                                    offsets.put(source.position()-1);
                                }
                            } else {
                                errorBuffer[errorBufferLength++] = (byte)targetByteUnit[0];
                                err = CoderResult.OVERFLOW;
                            }
                        } else {
                            if (target.hasRemaining()) {
                                target.put((byte)(UConverterConstants.UNSIGNED_BYTE_MASK & ((targetByteUnit[0]>>8) - 0x80)));
                                if (offsets != null) {
                                    offsets.put(source.position()-1);
                                }
                                if (target.hasRemaining()) {
                                    target.put((byte)(UConverterConstants.UNSIGNED_BYTE_MASK & (targetByteUnit[0]- 0x80)));
                                    if (offsets != null) {
                                        offsets.put(source.position()-1);
                                    }
                                } else {
                                    errorBuffer[errorBufferLength++] = (byte)(UConverterConstants.UNSIGNED_BYTE_MASK & (targetByteUnit[0] - 0x80));
                                    err = CoderResult.OVERFLOW;
                                }

                            } else {
                                errorBuffer[errorBufferLength++] = (byte)(UConverterConstants.UNSIGNED_BYTE_MASK & ((targetByteUnit[0]>>8) - 0x80));
                                errorBuffer[errorBufferLength++] = (byte)(UConverterConstants.UNSIGNED_BYTE_MASK & (targetByteUnit[0]- 0x80));
                                err = CoderResult.OVERFLOW;
                            }
                        }
                    } else {
                        /* oops.. the code point is unassigned
                         * set the error and reason
                         */

                        /* check if the char is a First surrogate */
                        if (gotoGetTrail || UTF16.isSurrogate((char)sourceChar)) {
                            if (gotoGetTrail || UTF16.isLeadSurrogate((char)sourceChar)) {
// getTrail label
                                // reset gotoGetTrail flag
                                gotoGetTrail = false;

                                /* look ahead to find the trail surrogate */
                                if (source.hasRemaining()) {
                                    /* test the following code unit */
                                    char trail = source.get();
                                    source.position(source.position()-1);
                                    if (UTF16.isTrailSurrogate(trail)) {
                                        source.get();
                                         sourceChar = UCharacter.getCodePoint((char)sourceChar, trail);
                                         err = CoderResult.unmappableForLength(2);
                                         /* convert this surrogate code point */
                                         /* exit this condition tree */
                                    } else {
                                        /* this is an unmatched lead code unit (1st surrogate) */
                                        /* callback(illegal) */
                                        err = CoderResult.malformedForLength(1);
                                    }
                                } else {
                                    /* no more input */
                                    err = CoderResult.UNDERFLOW;
                                }
                            } else {
                                /* this is an unmatched trail code unit (2nd surrogate ) */
                                /* callback(illegal) */
                                err = CoderResult.malformedForLength(1);
                            }
                        } else {
                            /* callback(unassigned) for a BMP code point */
                            err = CoderResult.unmappableForLength(1);
                        }

                        fromUChar32 = sourceChar;
                        break;
                    }
                } else {
                    err = CoderResult.OVERFLOW;
                    break;
                }
            }
            /*
             * the end of the input stream and detection of truncated input
             * are handled by the framework, but for ISO-2022-KR conversion
             * we need to be inASCII mode at the very end
             *
             * conditions:
             *  successful
             *  not in ASCII mode
             *  end of  input and no truncated input
             */
            if (!err.isError() && isTargetByteDBCS && flush && !source.hasRemaining() && fromUChar32 == 0) {
                int sourceIndex;

                /* we are switching to ASCII */
                isTargetByteDBCS = false;

                /* get the source index of the last input character */
                sourceIndex = source.position();
                if (sourceIndex > 0) {
                    --sourceIndex;
                    if (UTF16.isTrailSurrogate(source.get(sourceIndex)) && UTF16.isLeadSurrogate(source.get(sourceIndex-1))) {
                        --sourceIndex;
                    }
                } else {
                    sourceIndex = -1;
                }

                CharsetEncoderICU.fromUWriteBytes(this, SHIFT_IN_STR, 0, 1, target, offsets, sourceIndex);
            }
            /*save the state and return */
            fromUnicodeStatus = isTargetByteDBCS ? 1 : 0;

            return err;
        }
    }

    @Override
    public CharsetDecoder newDecoder() {
        switch (variant) {
        case ISO_2022_JP:
            return new CharsetDecoderISO2022JP(this);

        case ISO_2022_CN:
            return new CharsetDecoderISO2022CN(this);

        case ISO_2022_KR:
            setInitialStateToUnicodeKR();
            return new CharsetDecoderISO2022KR(this);

        default: /* should not happen */
            return null;
        }
    }

    @Override
    public CharsetEncoder newEncoder() {
        CharsetEncoderICU cnv;

        switch (variant) {
        case ISO_2022_JP:
            return new CharsetEncoderISO2022JP(this);

        case ISO_2022_CN:
            return new CharsetEncoderISO2022CN(this);

        case ISO_2022_KR:
            cnv = new CharsetEncoderISO2022KR(this);
            setInitialStateFromUnicodeKR(cnv);
            return cnv;

        default: /* should not happen */
            return null;
        }
    }

    private void setInitialStateToUnicodeKR() {
        if (myConverterData.version == 1) {
            myConverterData.currentDecoder.toUnicodeStatus = 0;     /* offset */
            myConverterData.currentDecoder.mode = 0;                /* state */
            myConverterData.currentDecoder.toULength = 0;           /* byteIndex */
        }
    }
    private void setInitialStateFromUnicodeKR(CharsetEncoderICU cnv) {
        /* ISO-2022-KR the designator sequence appears only once
         * in a file so we append it only once
         */
        if (cnv.errorBufferLength == 0) {
            cnv.errorBufferLength = 4;
            cnv.errorBuffer[0] = 0x1b;
            cnv.errorBuffer[1] = 0x24;
            cnv.errorBuffer[2] = 0x29;
            cnv.errorBuffer[3] = 0x43;
        }
        if (myConverterData.version == 1) {
            ((CharsetMBCS)myConverterData.currentEncoder.charset()).subChar1 = 0x1A;
            myConverterData.currentEncoder.fromUChar32 = 0;
            myConverterData.currentEncoder.fromUnicodeStatus = 1; /* prevLength */
        }
    }

    @Override
    void getUnicodeSetImpl(UnicodeSet setFillIn, int which) {
        int i;
        /*open a set and initialize it with code points that are algorithmically round-tripped */

        switch(variant){
        case ISO_2022_JP:
           /*include JIS X 0201 which is hardcoded */
            setFillIn.add(0xa5);
            setFillIn.add(0x203e);
            if((jpCharsetMasks[myConverterData.version]&CSM(ISO8859_1))!=0){
                /*include Latin-1 some variants of JP */
                setFillIn.add(0, 0xff);

            }
            else {
                /* include ASCII for JP */
                setFillIn.add(0, 0x7f);
             }
            if(myConverterData.version==3 || myConverterData.version==4 ||which == ROUNDTRIP_AND_FALLBACK_SET){
            /*
             * Do not test(jpCharsetMasks[myConverterData.version]&CSM(HWKANA_7BIT))!=0 because the bit
             * is on for all JP versions although version 3 & 4 (JIS7 and JIS8) use half-width Katakana.
             * This is because all ISO_2022_JP variant are lenient in that they accept (in toUnicode) half-width
             * Katakana via ESC.
             * However, we only emit (fromUnicode) half-width Katakana according to the
             * definition of each variant.
             *
             * When including fallbacks,
             * we need to include half-width Katakana Unicode code points for all JP variants because
             * JIS X 0208 has hardcoded fallbacks for them (which map to full-width Katakana).
             */
            /* include half-width Katakana for JP */
                setFillIn.add(HWKANA_START, HWKANA_END);
             }
            break;
        case ISO_2022_CN:
            /* Include ASCII for CN */
            setFillIn.add(0, 0x7f);
            break;
        case ISO_2022_KR:
            /* there is only one converter for KR */
          myConverterData.currentConverter.getUnicodeSetImpl(setFillIn, which);
          break;
        default:
            break;
        }

        //TODO Replaced by ucnv_MBCSGetFilteredUnicodeSetForUnicode() until
        for(i=0; i<UCNV_2022_MAX_CONVERTERS;i++){
            int filter;
            if(myConverterData.myConverterArray[i]!=null){
                if(variant==ISO_2022_CN && myConverterData.version==0 && i==CNS_11643){
                    /*
                     *
                     * version -specific for CN:
                     * CN version 0 does not map CNS planes 3..7 although
                     * they are all available in the CNS conversion table;
                     * CN version 1 (-EXT) does map them all.
                     * The two versions create different Unicode sets.
                     */
                    filter=CharsetMBCS.UCNV_SET_FILTER_2022_CN;
                } else if(variant==ISO_2022_JP && i == JISX208){
                    /*
                     * Only add code points that map to Shift-JIS codes
                     * corresponding to JIS X 208
                     */
                    filter=CharsetMBCS.UCNV_SET_FILTER_SJIS;
                } else if(i==KSC5601){
                    /*
                     * Some of the KSC 5601 tables (Convrtrs.txt has this aliases on multiple tables)
                     * are broader than GR94.
                     */
                    filter=CharsetMBCS.UCNV_SET_FILTER_GR94DBCS;
                } else {
                    filter=CharsetMBCS.UCNV_SET_FILTER_NONE;
                }

                myConverterData.currentConverter.MBCSGetFilteredUnicodeSetForUnicode(myConverterData.myConverterArray[i],setFillIn, which, filter);
           }
        }
        /*
         * ISO Converter must not convert SO/SI/ESC despite what sub-converters do by themselves
         * Remove these characters from the set.
         */
        setFillIn.remove(0x0e);
        setFillIn.remove(0x0f);
        setFillIn.remove(0x1b);

        /* ISO 2022 converter do not convert C! controls either */
        setFillIn.remove(0x80, 0x9f);
    }
}









