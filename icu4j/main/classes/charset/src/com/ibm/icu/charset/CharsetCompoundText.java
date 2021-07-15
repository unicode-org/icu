// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2010-2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.ibm.icu.charset.CharsetMBCS.CharsetDecoderMBCS;
import com.ibm.icu.charset.CharsetMBCS.CharsetEncoderMBCS;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

class CharsetCompoundText extends CharsetICU {
    private static final byte[] fromUSubstitution = new byte[] { (byte) 0x3F };
    private CharsetMBCS myConverterArray[];
    private byte state;

    private final static byte INVALID = -2;
    private final static byte DO_SEARCH = -1;
    private final static byte COMPOUND_TEXT_SINGLE_0 = 0;
    private final static byte COMPOUND_TEXT_SINGLE_1 = 1;
    private final static byte COMPOUND_TEXT_SINGLE_2 = 2;
    private final static byte COMPOUND_TEXT_SINGLE_3 = 3;

    /*private final static byte COMPOUND_TEXT_DOUBLE_1 = 4;
    private final static byte COMPOUND_TEXT_DOUBLE_2 = 5;
    private final static byte COMPOUND_TEXT_DOUBLE_3 = 6;
    private final static byte COMPOUND_TEXT_DOUBLE_4 = 7;
    private final static byte COMPOUND_TEXT_DOUBLE_5 = 8;
    private final static byte COMPOUND_TEXT_DOUBLE_6 = 9;
    private final static byte COMPOUND_TEXT_DOUBLE_7 = 10;

    private final static byte COMPOUND_TEXT_TRIPLE_DOUBLE = 11;*/

    private final static byte IBM_915 = 12;
    private final static byte IBM_916 = 13;
    private final static byte IBM_914 = 14;
    private final static byte IBM_874 = 15;
    private final static byte IBM_912 = 16;
    private final static byte IBM_913 = 17;
    private final static byte ISO_8859_14 = 18;
    private final static byte IBM_923 = 19;

    private final static byte NUM_OF_CONVERTERS = 20;

    private final static byte SEARCH_LENGTH = 12;

    private final static byte[][] escSeqCompoundText = {
        /* Single */
        { 0x1B, 0x2D, 0x41 },
        { 0x1B, 0x2D, 0x4D },
        { 0x1B, 0x2D, 0x46 },
        { 0x1B, 0x2D, 0x47 },

        /* Double */
        { 0x1B, 0x24, 0x29, 0x41 },
        { 0x1B, 0x24, 0x29, 0x42 },
        { 0x1B, 0x24, 0x29, 0x43 },
        { 0x1B, 0x24, 0x29, 0x44 },
        { 0x1B, 0x24, 0x29, 0x47 },
        { 0x1B, 0x24, 0x29, 0x48 },
        { 0x1B, 0x24, 0x29, 0x49 },

        /* Triple/Double */
        { 0x1B, 0x25, 0x47 },

        /*IBM-915*/
        { 0x1B, 0x2D, 0x4C },
        /*IBM-916*/
        { 0x1B, 0x2D, 0x48 },
        /*IBM-914*/
        { 0x1B, 0x2D, 0x44 },
        /*IBM-874*/
        { 0x1B, 0x2D, 0x54 },
        /*IBM-912*/
        { 0x1B, 0x2D, 0x42 },
        /* IBM-913 */
        { 0x1B, 0x2D, 0x43 },
        /* ISO-8859_14 */
        { 0x1B, 0x2D, 0x5F },
        /* IBM-923 */
        { 0x1B, 0x2D, 0x62 },
    };

    private final static byte ESC_START = 0x1B;

    private static boolean isASCIIRange(int codepoint) {
        if ((codepoint == 0x0000) || (codepoint == 0x0009) || (codepoint == 0x000A) ||
                (codepoint >= 0x0020 && codepoint <= 0x007f) || (codepoint >= 0x00A0 && codepoint <= 0x00FF)) {
            return true;
        }
        return false;
    }

    private static boolean isIBM915(int codepoint) {
        if ((codepoint >= 0x0401 && codepoint <= 0x045F) || (codepoint == 0x2116)) {
            return true;
        }
        return false;
    }

    private static boolean isIBM916(int codepoint) {
        if ((codepoint >= 0x05D0 && codepoint <= 0x05EA) || (codepoint == 0x2017) || (codepoint == 0x203E)) {
            return true;
        }
        return false;
    }

    private static boolean isCompoundS3(int codepoint) {
        if ((codepoint == 0x060C) || (codepoint == 0x061B) || (codepoint == 0x061F) || (codepoint >= 0x0621 && codepoint <= 0x063A) ||
                (codepoint >= 0x0640 && codepoint <= 0x0652) || (codepoint >= 0x0660 && codepoint <= 0x066D) || (codepoint == 0x200B) ||
                (codepoint >= 0x0FE70 && codepoint <= 0x0FE72) || (codepoint == 0x0FE74) || (codepoint >= 0x0FE76 && codepoint <= 0x0FEBE)) {
            return true;
        }
        return false;
    }

    private static boolean isCompoundS2(int codepoint) {
        if ((codepoint == 0x02BC) || (codepoint == 0x02BD) || (codepoint >= 0x0384 && codepoint <= 0x03CE) || (codepoint == 0x2015)) {
            return true;
        }
        return false;
    }

    private static boolean isIBM914(int codepoint) {
        if ((codepoint == 0x0100) || (codepoint == 0x0101) || (codepoint == 0x0112) || (codepoint == 0x0113) || (codepoint == 0x0116) || (codepoint == 0x0117) ||
                (codepoint == 0x0122) || (codepoint == 0x0123) || (codepoint >= 0x0128 && codepoint <= 0x012B) || (codepoint == 0x012E) || (codepoint == 0x012F) ||
                (codepoint >= 0x0136 && codepoint <= 0x0138) || (codepoint == 0x013B) || (codepoint == 0x013C) || (codepoint == 0x0145) || (codepoint ==  0x0146) ||
                (codepoint >= 0x014A && codepoint <= 0x014D) || (codepoint == 0x0156) || (codepoint == 0x0157) || (codepoint >= 0x0166 && codepoint <= 0x016B) ||
                (codepoint == 0x0172) || (codepoint == 0x0173)) {
            return true;
        }
        return false;
    }

    private static boolean isIBM874(int codepoint) {
        if ((codepoint >= 0x0E01 && codepoint <= 0x0E3A) || (codepoint >= 0x0E3F && codepoint <= 0x0E5B)) {
            return true;
        }
        return false;
    }

    private static boolean isIBM912(int codepoint) {
        return ((codepoint >= 0x0102  && codepoint <= 0x0107)  || (codepoint >= 0x010C && codepoint <= 0x0111)    || (codepoint >= 0x0118 && codepoint <= 0x011B) ||
                (codepoint == 0x0139) || (codepoint == 0x013A) || (codepoint == 0x013D) || (codepoint == 0x013E)  || (codepoint >= 0x0141 && codepoint <= 0x0144) ||
                (codepoint == 0x0147) || (codepoint == 0x0150) || (codepoint == 0x0151) || (codepoint == 0x0154)  || (codepoint == 0x0155)                        ||
                (codepoint >= 0x0158  && codepoint <= 0x015B)  || (codepoint == 0x015E) || (codepoint == 0x015F)  || (codepoint >= 0x0160 && codepoint <= 0x0165) ||
                (codepoint == 0x016E) || (codepoint == 0x016F) || (codepoint == 0x0170) || (codepoint ==  0x0171) || (codepoint >= 0x0179 && codepoint <= 0x017E) ||
                (codepoint == 0x02C7) || (codepoint == 0x02D8) || (codepoint == 0x02D9) || (codepoint == 0x02DB)  || (codepoint == 0x02DD));
    }

    private static boolean isIBM913(int codepoint) {
        if ((codepoint >= 0x0108 && codepoint <= 0x010B) || (codepoint == 0x011C) ||
                (codepoint == 0x011D) || (codepoint == 0x0120) || (codepoint == 0x0121) ||
                (codepoint >= 0x0124 && codepoint <= 0x0127) || (codepoint == 0x0134) || (codepoint == 0x0135) ||
                (codepoint == 0x015C) || (codepoint == 0x015D) || (codepoint == 0x016C) || (codepoint ==  0x016D)) {
            return true;
        }
        return false;
    }

    private static boolean isCompoundS1(int codepoint) {
        if ((codepoint == 0x011E) || (codepoint == 0x011F) || (codepoint == 0x0130) ||
                (codepoint == 0x0131) || (codepoint >= 0x0218 && codepoint <= 0x021B)) {
            return true;
        }
        return false;
    }

    private static boolean isISO8859_14(int codepoint) {
        if ((codepoint >= 0x0174 && codepoint <= 0x0177) || (codepoint == 0x1E0A) ||
                (codepoint == 0x1E0B) || (codepoint == 0x1E1E) || (codepoint == 0x1E1F) ||
                (codepoint == 0x1E40) || (codepoint == 0x1E41) || (codepoint == 0x1E56) ||
                (codepoint == 0x1E57) || (codepoint == 0x1E60) || (codepoint == 0x1E61) ||
                (codepoint == 0x1E6A) || (codepoint == 0x1E6B) || (codepoint == 0x1EF2) ||
                (codepoint == 0x1EF3) || (codepoint >= 0x1E80 && codepoint <= 0x1E85)) {
            return true;
        }
        return false;
    }

    private static boolean isIBM923(int codepoint) {
        if ((codepoint == 0x0152) || (codepoint == 0x0153) || (codepoint == 0x0178) || (codepoint == 0x20AC)) {
            return true;
        }
        return false;
    }

    private static int findNextEsc(ByteBuffer source) {
        int sourceLimit = source.limit();
        for (int i = (source.position() + 1); i < sourceLimit; i++) {
            if (source.get(i) == 0x1B) {
                return i;
            }
        }
        return sourceLimit;
    }

    private static byte getState(int codepoint) {
        byte state = -1;

        if (isASCIIRange(codepoint)) {
            state = COMPOUND_TEXT_SINGLE_0;
        } else if (isIBM912(codepoint)) {
            state = IBM_912;
        }else if (isIBM913(codepoint)) {
            state = IBM_913;
        } else if (isISO8859_14(codepoint)) {
            state = ISO_8859_14;
        } else if (isIBM923(codepoint)) {
            state = IBM_923;
        } else if (isIBM874(codepoint)) {
            state = IBM_874;
        } else if (isIBM914(codepoint)) {
            state = IBM_914;
        } else if (isCompoundS2(codepoint)) {
            state = COMPOUND_TEXT_SINGLE_2;
        } else if (isCompoundS3(codepoint)) {
            state = COMPOUND_TEXT_SINGLE_3;
        } else if (isIBM916(codepoint)) {
            state = IBM_916;
        } else if (isIBM915(codepoint)) {
            state = IBM_915;
        } else if (isCompoundS1(codepoint)) {
            state = COMPOUND_TEXT_SINGLE_1;
        }

        return state;
    }

    private static byte findStateFromEscSeq(ByteBuffer source, byte[] toUBytes, int toUBytesLength) {
        byte state = INVALID;
        int sourceIndex = source.position();
        boolean matchFound = false;
        byte i, n;
        int offset = toUBytesLength;
        int sourceLimit = source.limit();

        for (i = 0; i < escSeqCompoundText.length; i++) {
            matchFound = true;
            for (n = 0; n < escSeqCompoundText[i].length; n++) {
                if (n < toUBytesLength) {
                    if (toUBytes[n] != escSeqCompoundText[i][n]) {
                        matchFound = false;
                        break;
                    }
                } else if ((sourceIndex + (n - offset)) >= sourceLimit) {
                    return DO_SEARCH;
                } else if (source.get(sourceIndex + (n - offset)) != escSeqCompoundText[i][n]) {
                    matchFound = false;
                    break;
                }
            }
            if (matchFound) {
                break;
            }
        }

        if (matchFound) {
            state = i;
            source.position(sourceIndex + (escSeqCompoundText[i].length - offset));
        }

        return state;
    }

    public CharsetCompoundText(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);

        LoadConverters();

        maxBytesPerChar = 6;
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
    }

    private void LoadConverters() {
        myConverterArray = new CharsetMBCS[NUM_OF_CONVERTERS];

        myConverterArray[COMPOUND_TEXT_SINGLE_0] = null;

        for (int i = 1; i < SEARCH_LENGTH; i++) {
            String name = "icu-internal-compound-";
            if (i <= 3) {
                name = name + "s" + i;
            } else if (i <= 10) {
                name = name + "d" + (i - 3);
            } else {
                name = name + "t";
            }

            myConverterArray[i] = (CharsetMBCS)CharsetICU.forNameICU(name);
        }

        myConverterArray[IBM_915] = (CharsetMBCS)CharsetICU.forNameICU("ibm-915_P100-1995");
        myConverterArray[IBM_916] = (CharsetMBCS)CharsetICU.forNameICU("ibm-916_P100-1995");
        myConverterArray[IBM_914] = (CharsetMBCS)CharsetICU.forNameICU("ibm-914_P100-1995");
        myConverterArray[IBM_874] = (CharsetMBCS)CharsetICU.forNameICU("ibm-874_P100-1995");
        myConverterArray[IBM_912] = (CharsetMBCS)CharsetICU.forNameICU("ibm-912_P100-1995");
        myConverterArray[IBM_913] = (CharsetMBCS)CharsetICU.forNameICU("ibm-913_P100-2000");
        myConverterArray[ISO_8859_14] = (CharsetMBCS)CharsetICU.forNameICU("iso-8859_14-1998");
        myConverterArray[IBM_923] = (CharsetMBCS)CharsetICU.forNameICU("ibm-923_P100-1998");
    }

    class CharsetEncoderCompoundText extends CharsetEncoderICU {
        CharsetEncoderMBCS gbEncoder[];

        public CharsetEncoderCompoundText(CharsetICU cs) {
            super(cs, fromUSubstitution);

            gbEncoder = new CharsetEncoderMBCS[NUM_OF_CONVERTERS];

            for (int i = 0; i < NUM_OF_CONVERTERS; i++) {
                if (i == 0) {
                    gbEncoder[i] = null;
                } else {
                    gbEncoder[i] = (CharsetEncoderMBCS)myConverterArray[i].newEncoder();
                }
            }
        }

        @Override
        protected void implReset() {
            super.implReset();
            for (int i = 0; i < NUM_OF_CONVERTERS; i++) {
                if (gbEncoder[i] != null) {
                    gbEncoder[i].implReset();
                }
            }
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            int sourceChar;
            char []sourceCharArray = { 0x0000 };
            ByteBuffer tmpTargetBuffer = ByteBuffer.allocate(3);
            byte[] targetBytes = new byte[10];
            int targetLength = 0;
            byte currentState = state;
            byte tmpState = 0;
            int i = 0;
            boolean gotoGetTrail = false;

            if (!source.hasRemaining())
                return CoderResult.UNDERFLOW;
            else if (!target.hasRemaining())
                return CoderResult.OVERFLOW;

            /* check if the last codepoint of previous buffer was a lead surrogate */
            if ((sourceChar = fromUChar32) != 0 && target.hasRemaining()) {
                // goto getTrail label
                gotoGetTrail = true;
            }

            while (source.hasRemaining()) {
                if (target.hasRemaining()) {
                    if (!gotoGetTrail) {
                        sourceChar = source.get();
                    }

                    targetLength = 0;
                    tmpTargetBuffer.position(0);
                    tmpTargetBuffer.limit(3);

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

                    tmpState = getState(sourceChar);

                    sourceCharArray[0] = (char)sourceChar;

                    if (tmpState < 0) {
                        /* Test all available converters */
                        for (i = 1; i < SEARCH_LENGTH; i++) {
                            err = gbEncoder[i].cnvMBCSFromUnicodeWithOffsets(CharBuffer.wrap(sourceCharArray), tmpTargetBuffer, offsets, true);
                            if (!err.isError()) {
                                tmpState = (byte)i;
                                tmpTargetBuffer.limit(tmpTargetBuffer.position());
                                implReset();
                                break;
                            }
                        }
                    } else if (tmpState == COMPOUND_TEXT_SINGLE_0) {
                        tmpTargetBuffer.put(0, (byte)sourceChar);
                        tmpTargetBuffer.limit(1);
                    } else {
                        err = gbEncoder[tmpState].cnvMBCSFromUnicodeWithOffsets(CharBuffer.wrap(sourceCharArray), tmpTargetBuffer, offsets, true);
                        if (!err.isError()) {
                            tmpTargetBuffer.limit(tmpTargetBuffer.position());
                        }
                    }
                    if (err.isError()) {
                        break;
                    }

                    if (currentState != tmpState) {
                        currentState = tmpState;

                        /* Write escape sequence if necessary */
                        for (i = 0; i < escSeqCompoundText[currentState].length; i++) {
                            targetBytes[i] = escSeqCompoundText[currentState][i];
                        }
                        targetLength = i;
                    }

                    for (i = 0; i < tmpTargetBuffer.limit(); i++) {
                        targetBytes[i+targetLength] = tmpTargetBuffer.get(i);
                    }
                    targetLength += i;

                    for (i = 0; i < targetLength; i++) {
                        if (target.hasRemaining()) {
                            target.put(targetBytes[i]);
                        } else {
                            err = CoderResult.OVERFLOW;
                            break;
                        }
                    }
                } else {
                    err = CoderResult.OVERFLOW;
                    break;
                }
            }

            if (err.isOverflow()) {
                int m = 0;
                for (int n = i; n < targetLength; n++) {
                    this.errorBuffer[m++] = targetBytes[n];
                }
                this.errorBufferLength = m;
            }
            state = currentState;

            return err;
        }
    }

    class CharsetDecoderCompoundText extends CharsetDecoderICU {
        CharsetDecoderMBCS gbDecoder[];

        public CharsetDecoderCompoundText(CharsetICU cs) {
            super(cs);
            gbDecoder = new CharsetDecoderMBCS[NUM_OF_CONVERTERS];

            for (int i = 0; i < NUM_OF_CONVERTERS; i++) {
                if (i == 0) {
                    gbDecoder[i] = null;
                } else {
                    gbDecoder[i] = (CharsetDecoderMBCS)myConverterArray[i].newDecoder();
                }
            }
        }

        @Override
        protected void implReset() {
            super.implReset();
            for (int i = 0; i < NUM_OF_CONVERTERS; i++) {
                if (gbDecoder[i] != null) {
                    gbDecoder[i].implReset();
                }
            }
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            byte[] sourceChar = { 0x00 };
            byte currentState = state;
            byte tmpState = currentState;
            CharsetDecoderMBCS decoder;
            int sourceLimit = source.limit();;

            if (!source.hasRemaining())
                return CoderResult.UNDERFLOW;
            else if (!target.hasRemaining())
                return CoderResult.OVERFLOW;

            while (source.hasRemaining()) {
                if (target.hasRemaining()) {
                    if (this.toULength > 0) {
                        sourceChar[0] = this.toUBytesArray[0];
                    } else {
                        sourceChar[0] = source.get(source.position());
                    }

                    if (sourceChar[0] == ESC_START) {
                        tmpState = findStateFromEscSeq(source, this.toUBytesArray, this.toULength);
                        if (tmpState == DO_SEARCH) {
                            while (source.hasRemaining()) {
                                this.toUBytesArray[this.toULength++] = source.get();
                            }
                            break;
                        }
                        if (tmpState < 0) {
                            err = CoderResult.malformedForLength(1);
                            if (this.toULength == 0) {
                                source.get(); /* skip over the 0x1b byte */
                            }
                            break;
                        }

                        this.toULength = 0;
                    }

                    if (tmpState != currentState) {
                        currentState = tmpState;
                    }

                    if (currentState == COMPOUND_TEXT_SINGLE_0) {
                        while (source.hasRemaining()) {
                            if (!target.hasRemaining()) {
                                err = CoderResult.OVERFLOW;
                                break;
                            }
                            if (source.get(source.position()) == ESC_START) {
                                break;
                            }
                            if (target.hasRemaining()) {
                                target.put((char)(UConverterConstants.UNSIGNED_BYTE_MASK&source.get()));
                            }
                        }
                    } else if (source.hasRemaining()) {
                        source.limit(findNextEsc(source));

                        decoder = gbDecoder[currentState];

                        decoder.toUBytesArray = this.toUBytesArray;
                        decoder.toULength = this.toULength;

                        err = decoder.decodeLoop(source, target, offsets, true);

                        this.toULength = decoder.toULength;
                        decoder.toULength = 0;

                        if (err.isError()) {
                            if (err.isOverflow()) {
                                this.charErrorBufferArray = decoder.charErrorBufferArray;
                                this.charErrorBufferBegin = decoder.charErrorBufferBegin;
                                this.charErrorBufferLength = decoder.charErrorBufferLength;

                                decoder.charErrorBufferBegin = 0;
                                decoder.charErrorBufferLength = 0;
                            }
                        }

                        source.limit(sourceLimit);
                    }

                    if (err.isError()) {
                        break;
                    }
                } else {
                    err = CoderResult.OVERFLOW;
                    break;
                }
            }
            state = currentState;
            return err;
        }
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderCompoundText(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderCompoundText(this);
    }

    @Override
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        for (int i = 1; i < NUM_OF_CONVERTERS; i++) {
            myConverterArray[i].MBCSGetFilteredUnicodeSetForUnicode(myConverterArray[i].sharedData, setFillIn, which, CharsetMBCS.UCNV_SET_FILTER_NONE);
        }
        setFillIn.add(0x0000);
        setFillIn.add(0x0009);
        setFillIn.add(0x000A);
        setFillIn.add(0x0020, 0x007F);
        setFillIn.add(0x00A0, 0x00FF);
    }
}
