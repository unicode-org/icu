// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 2006-2014, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CoderResult;

/**
 * <h2> Callback API for CharsetICU API </h2>
 *
 *  CharsetCallback class defines some error behaviour functions called
 *  by CharsetDecoderICU and CharsetEncoderICU. The class also provides
 *  the facility by which clients can write their own callbacks.
 *
 *  These functions, although public, should NEVER be called directly.
 *  They should be used as parameters to the onUmappableCharacter() and
 *  onMalformedInput() methods, to set the behaviour of a converter
 *  when it encounters UNMAPPED/INVALID sequences.
 *  Currently the only way to set callbacks is by using CodingErrorAction.
 *  In the future we will provide set methods on CharsetEncoder and CharsetDecoder
 *  that will accept CharsetCallback fields.
 *
 * @stable ICU 3.6
 */

public class CharsetCallback {
    /*
     * FROM_U, TO_U context options for sub callback
     */
    private static final String SUB_STOP_ON_ILLEGAL = "i";

//    /*
//     * FROM_U, TO_U context options for skip callback
//     */
//    private static final String SKIP_STOP_ON_ILLEGAL = "i";

//    /*
//     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to ICU (%UXXXX)
//     */
//    private static final String ESCAPE_ICU  = null;

    /*
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to JAVA (\\uXXXX)
     */
    private static final String ESCAPE_JAVA     =  "J";

    /*
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to C (\\uXXXX \\UXXXXXXXX)
     * TO_U_CALLBACK_ESCAPE option to escape the character value according to C (\\xXXXX)
     */
    private static final String ESCAPE_C        = "C";

    /*
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to XML Decimal escape \htmlonly(&amp;#DDDD;)\endhtmlonly
     * TO_U_CALLBACK_ESCAPE context option to escape the character value according to XML Decimal escape \htmlonly(&amp;#DDDD;)\endhtmlonly
     */
    private static final String ESCAPE_XML_DEC  = "D";

    /*
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to XML Hex escape \htmlonly(&amp;#xXXXX;)\endhtmlonly
     * TO_U_CALLBACK_ESCAPE context option to escape the character value according to XML Hex escape \htmlonly(&amp;#xXXXX;)\endhtmlonly
     */
    private static final String ESCAPE_XML_HEX  = "X";

    /*
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to Unicode (U+XXXXX)
     */
    private static final String ESCAPE_UNICODE  = "U";

    /*
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to Unicode (U+XXXXX)
     */
    private static final String ESCAPE_CSS2  = "S";

    /*
     * IS_DEFAULT_IGNORABLE_CODE_POINT
     * This is to check if a code point has the default ignorable unicode property.
     * As such, this list needs to be updated if the ignorable code point list ever
     * changes.
     * To avoid dependency on other code, this list is hard coded here.
     * When an ignorable code point is found and is unmappable, the default callbacks
     * will ignore them.
     * For a list of the default ignorable code points, use this link:
     * https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5B%3ADI%3A%5D&abb=on&g=&i=
     *
     * This list should be sync with the one in ucnv_err.cpp.
     */
    private static boolean IS_DEFAULT_IGNORABLE_CODE_POINT(int c) {
        return
            (c == 0x00AD) ||
            (c == 0x034F) ||
            (c == 0x061C) ||
            (c == 0x115F) ||
            (c == 0x1160) ||
            (0x17B4 <= c && c <= 0x17B5) ||
            (0x180B <= c && c <= 0x180F) ||
            (0x200B <= c && c <= 0x200F) ||
            (0x202A <= c && c <= 0x202E) ||
            (0x2060 <= c && c <= 0x206F) ||
            (c == 0x3164) ||
            (0xFE00 <= c && c <= 0xFE0F) ||
            (c == 0xFEFF) ||
            (c == 0xFFA0) ||
            (0xFFF0 <= c && c <= 0xFFF8) ||
            (0x1BCA0 <= c && c <= 0x1BCA3) ||
            (0x1D173 <= c && c <= 0x1D17A) ||
            (0xE0000 <= c && c <= 0xE0FFF);
    }
    /**
     * Decoder Callback interface
     * @stable ICU 3.6
     */
    public interface Decoder {
        /**
         * This function is called when the bytes in the source cannot be handled,
         * and this function is meant to handle or fix the error if possible.
         *
         * @return Result of decoding action. This returned object is set to an error
         *  if this function could not handle the conversion.
         * @stable ICU 3.6
         */
        public CoderResult call(CharsetDecoderICU decoder, Object context,
                                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                                char[] buffer, int length, CoderResult cr);
    }
    /**
     * Encoder Callback interface
     * @stable ICU 3.6
     */
    public interface Encoder {
        /**
         * This function is called when the Unicode characters in the source cannot be handled,
         * and this function is meant to handle or fix the error if possible.
         * @return Result of decoding action. This returned object is set to an error
         *  if this function could not handle the conversion.
         * @stable ICU 3.6
         */
        public CoderResult call(CharsetEncoderICU encoder, Object context,
                                CharBuffer source, ByteBuffer target, IntBuffer offsets,
                                char[] buffer, int length, int cp, CoderResult cr);
    }
    /**
     * Skip callback
     * @stable ICU 3.6
     */
    public static final Encoder FROM_U_CALLBACK_SKIP = new Encoder() {
        @Override
        public CoderResult call(CharsetEncoderICU encoder, Object context,
                CharBuffer source, ByteBuffer target, IntBuffer offsets,
                char[] buffer, int length, int cp, CoderResult cr){
            if(context==null){
                return CoderResult.UNDERFLOW;
            }else if(((String)context).equals(SUB_STOP_ON_ILLEGAL)){
                if(!cr.isUnmappable()){
                    return cr;
                }else{
                    return CoderResult.UNDERFLOW;
                }
            }
            return cr;
        }
    };
    /**
     * Skip callback
     * @stable ICU 3.6
     */
    public static final Decoder TO_U_CALLBACK_SKIP = new Decoder() {
        @Override
        public CoderResult call(CharsetDecoderICU decoder, Object context,
                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                char[] buffer, int length, CoderResult cr){
            if(context==null){
                return CoderResult.UNDERFLOW;
            }else if(((String)context).equals(SUB_STOP_ON_ILLEGAL)){
                if(!cr.isUnmappable()){
                    return cr;
                }else{
                    return CoderResult.UNDERFLOW;
                }
            }
            return cr;
        }
    };
    /**
     * Write substitute callback
     * @stable ICU 3.6
     */
    public static final Encoder FROM_U_CALLBACK_SUBSTITUTE = new Encoder(){
        @Override
        public CoderResult call(CharsetEncoderICU encoder, Object context,
                CharBuffer source, ByteBuffer target, IntBuffer offsets,
                char[] buffer, int length, int cp, CoderResult cr){
            if (cr.isUnmappable() && IS_DEFAULT_IGNORABLE_CODE_POINT(cp)) {
                return CoderResult.UNDERFLOW;
            }else if(context==null){
                return encoder.cbFromUWriteSub(encoder, source, target, offsets);
            }else if(((String)context).equals(SUB_STOP_ON_ILLEGAL)){
                if(!cr.isUnmappable()){
                    return cr;
                }else{
                   return encoder.cbFromUWriteSub(encoder, source, target, offsets);
                }
            }
            return cr;
        }
    };
    private static final char[] kSubstituteChar1 = new char[]{0x1A};
    private static final char[] kSubstituteChar = new char[] {0xFFFD};
    /**
     * Write substitute callback
     * @stable ICU 3.6
     */
    public static final Decoder TO_U_CALLBACK_SUBSTITUTE  = new Decoder() {
        @Override
        public CoderResult call(CharsetDecoderICU decoder, Object context,
                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                char[] buffer, int length, CoderResult cr){

            CharsetICU cs = (CharsetICU) decoder.charset();
            /* Use the specified replacement character if it is different than the default one. */
            boolean useReplacement = true;
            char [] replacementChar = decoder.replacement().toCharArray();
            if (replacementChar.length == 1 && (replacementChar[0] == kSubstituteChar1[0] || replacementChar[0] == kSubstituteChar[0])) {
                useReplacement = false;
            }

            /* could optimize this case, just one uchar */
            if(decoder.invalidCharLength == 1 && cs.subChar1 != 0) {
                return CharsetDecoderICU.toUWriteUChars(decoder, useReplacement ? replacementChar : kSubstituteChar1, 0, useReplacement ? replacementChar.length : 1, target, offsets, source.position());
            } else {
                return CharsetDecoderICU.toUWriteUChars(decoder, useReplacement ? replacementChar : kSubstituteChar, 0, useReplacement ? replacementChar.length : 1, target, offsets, source.position());
            }
        }
    };
    /**
     * Stop callback
     * @stable ICU 3.6
     */
    public static final Encoder FROM_U_CALLBACK_STOP = new Encoder() {
        @Override
        public CoderResult call(CharsetEncoderICU encoder, Object context,
                CharBuffer source, ByteBuffer target, IntBuffer offsets,
                char[] buffer, int length, int cp, CoderResult cr){
            if (cr.isUnmappable() && IS_DEFAULT_IGNORABLE_CODE_POINT(cp)) {
                return CoderResult.UNDERFLOW;
            }
            return cr;
        }
    };
    /**
     * Stop callback
     * @stable ICU 3.6
     */
    public static final Decoder TO_U_CALLBACK_STOP = new Decoder() {
        @Override
        public CoderResult call(CharsetDecoderICU decoder, Object context,
                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                char[] buffer, int length, CoderResult cr){
            return cr;
        }
    };
    private static final int VALUE_STRING_LENGTH = 32;
    private static final char UNICODE_PERCENT_SIGN_CODEPOINT    = 0x0025;
    private static final char UNICODE_U_CODEPOINT               = 0x0055;
    private static final char UNICODE_X_CODEPOINT               = 0x0058;
    private static final char UNICODE_RS_CODEPOINT              = 0x005C;
    private static final char UNICODE_U_LOW_CODEPOINT           = 0x0075;
    private static final char UNICODE_X_LOW_CODEPOINT           = 0x0078;
    private static final char UNICODE_AMP_CODEPOINT             = 0x0026;
    private static final char UNICODE_HASH_CODEPOINT            = 0x0023;
    private static final char UNICODE_SEMICOLON_CODEPOINT       = 0x003B;
    private static final char UNICODE_PLUS_CODEPOINT            = 0x002B;
    private static final char UNICODE_LEFT_CURLY_CODEPOINT      = 0x007B;
    private static final char UNICODE_RIGHT_CURLY_CODEPOINT     = 0x007D;
    private static final char UNICODE_SPACE_CODEPOINT           = 0x0020;
    /**
     * Write escape callback
     * @stable ICU 4.0
     */
    public static final Encoder FROM_U_CALLBACK_ESCAPE = new Encoder() {
        @Override
        public CoderResult call(CharsetEncoderICU encoder, Object context,
                CharBuffer source, ByteBuffer target, IntBuffer offsets,
                char[] buffer, int length, int cp, CoderResult cr){
            char[] valueString = new char[VALUE_STRING_LENGTH];
            int valueStringLength = 0;
            int i = 0;

            if (cr.isUnmappable() && IS_DEFAULT_IGNORABLE_CODE_POINT(cp)) {
                return CoderResult.UNDERFLOW;
            }

            if (context == null || !(context instanceof String)) {
                while (i < length) {
                    valueString[valueStringLength++] = UNICODE_PERCENT_SIGN_CODEPOINT; /* adding % */
                    valueString[valueStringLength++] = UNICODE_U_CODEPOINT; /* adding U */
                    valueStringLength += itou(valueString, valueStringLength, buffer[i++], 16, 4);
                }
            } else {
                if (((String)context).equals(ESCAPE_JAVA)) {
                    while (i < length) {
                        valueString[valueStringLength++] = UNICODE_RS_CODEPOINT;    /* adding \ */
                        valueString[valueStringLength++] = UNICODE_U_LOW_CODEPOINT; /* adding u */
                        valueStringLength += itou(valueString, valueStringLength, buffer[i++], 16, 4);
                    }
                } else if (((String)context).equals(ESCAPE_C)) {
                    valueString[valueStringLength++] = UNICODE_RS_CODEPOINT;    /* adding \ */

                    if (length == 2) {
                        valueString[valueStringLength++] = UNICODE_U_CODEPOINT; /* adding U */
                        valueStringLength = itou(valueString, valueStringLength, cp, 16, 8);
                    } else {
                        valueString[valueStringLength++] = UNICODE_U_LOW_CODEPOINT; /* adding u */
                        valueStringLength += itou(valueString, valueStringLength, buffer[0], 16, 4);
                    }
                } else if (((String)context).equals(ESCAPE_XML_DEC)) {
                    valueString[valueStringLength++] = UNICODE_AMP_CODEPOINT;   /* adding & */
                    valueString[valueStringLength++] = UNICODE_HASH_CODEPOINT;  /* adding # */
                    if (length == 2) {
                        valueStringLength += itou(valueString, valueStringLength, cp, 10, 0);
                    } else {
                        valueStringLength += itou(valueString, valueStringLength, buffer[0], 10, 0);
                    }
                    valueString[valueStringLength++] = UNICODE_SEMICOLON_CODEPOINT; /* adding ; */
                } else if (((String)context).equals(ESCAPE_XML_HEX)) {
                    valueString[valueStringLength++] = UNICODE_AMP_CODEPOINT;   /* adding & */
                    valueString[valueStringLength++] = UNICODE_HASH_CODEPOINT;  /* adding # */
                    valueString[valueStringLength++] = UNICODE_X_LOW_CODEPOINT; /* adding x */
                    if (length == 2) {
                        valueStringLength += itou(valueString, valueStringLength, cp, 16, 0);
                    } else {
                        valueStringLength += itou(valueString, valueStringLength, buffer[0], 16, 0);
                    }
                    valueString[valueStringLength++] = UNICODE_SEMICOLON_CODEPOINT; /* adding ; */
                } else if (((String)context).equals(ESCAPE_UNICODE)) {
                    valueString[valueStringLength++] = UNICODE_LEFT_CURLY_CODEPOINT;    /* adding { */
                    valueString[valueStringLength++] = UNICODE_U_CODEPOINT;             /* adding U */
                    valueString[valueStringLength++] = UNICODE_PLUS_CODEPOINT;          /* adding + */
                    if (length == 2) {
                        valueStringLength += itou(valueString, valueStringLength,cp, 16, 4);
                    } else {
                        valueStringLength += itou(valueString, valueStringLength, buffer[0], 16, 4);
                    }
                    valueString[valueStringLength++] = UNICODE_RIGHT_CURLY_CODEPOINT;   /* adding } */
                } else if (((String)context).equals(ESCAPE_CSS2)) {
                    valueString[valueStringLength++] = UNICODE_RS_CODEPOINT;    /* adding \ */
                    valueStringLength += itou(valueString, valueStringLength, cp, 16, 0);
                    /* Always add space character, because the next character might be whitespace,
                       which would erroneously be considered the termination of the escape sequence. */
                    valueString[valueStringLength++] = UNICODE_SPACE_CODEPOINT;
                } else {
                    while (i < length) {
                        valueString[valueStringLength++] = UNICODE_PERCENT_SIGN_CODEPOINT;  /* adding % */
                        valueString[valueStringLength++] = UNICODE_U_CODEPOINT;             /* adding U */
                        valueStringLength += itou(valueString, valueStringLength, buffer[i++], 16, 4);
                    }
                }
            }
            return encoder.cbFromUWriteUChars(encoder, CharBuffer.wrap(valueString, 0, valueStringLength), target, offsets);
        }
    };
    /**
     * Write escape callback
     * @stable ICU 4.0
     */
    public static final Decoder TO_U_CALLBACK_ESCAPE = new Decoder() {
        @Override
        public CoderResult call(CharsetDecoderICU decoder, Object context,
                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                char[] buffer, int length, CoderResult cr){
            char[] uniValueString = new char[VALUE_STRING_LENGTH];
            int valueStringLength = 0;
            int i = 0;

            if (context == null || !(context instanceof String)) {
                while (i < length) {
                    uniValueString[valueStringLength++] = UNICODE_PERCENT_SIGN_CODEPOINT;   /* adding % */
                    uniValueString[valueStringLength++] = UNICODE_X_CODEPOINT;              /* adding U */
                    valueStringLength += itou(uniValueString, valueStringLength, buffer[i++] & UConverterConstants.UNSIGNED_BYTE_MASK, 16, 2);
                }
            } else {
                if (((String)context).equals(ESCAPE_XML_DEC)) {
                    while (i < length) {
                        uniValueString[valueStringLength++] = UNICODE_AMP_CODEPOINT;    /* adding & */
                        uniValueString[valueStringLength++] = UNICODE_HASH_CODEPOINT;   /* adding # */
                        valueStringLength += itou(uniValueString, valueStringLength, buffer[i++] & UConverterConstants.UNSIGNED_BYTE_MASK, 10, 0);
                        uniValueString[valueStringLength++] = UNICODE_SEMICOLON_CODEPOINT;  /* adding ; */
                    }
                } else if (((String)context).equals(ESCAPE_XML_HEX)) {
                    while (i < length) {
                        uniValueString[valueStringLength++] = UNICODE_AMP_CODEPOINT;    /* adding & */
                        uniValueString[valueStringLength++] = UNICODE_HASH_CODEPOINT;   /* adding # */
                        uniValueString[valueStringLength++] = UNICODE_X_LOW_CODEPOINT;  /* adding x */
                        valueStringLength += itou(uniValueString, valueStringLength, buffer[i++] & UConverterConstants.UNSIGNED_BYTE_MASK, 16, 0);
                        uniValueString[valueStringLength++] = UNICODE_SEMICOLON_CODEPOINT;  /* adding ; */
                    }
                } else if (((String)context).equals(ESCAPE_C)) {
                    while (i < length) {
                        uniValueString[valueStringLength++] = UNICODE_RS_CODEPOINT;         /* adding \ */
                        uniValueString[valueStringLength++] = UNICODE_X_LOW_CODEPOINT;      /* adding x */
                        valueStringLength += itou(uniValueString, valueStringLength, buffer[i++] & UConverterConstants.UNSIGNED_BYTE_MASK, 16, 2);
                    }
                } else {
                    while (i < length) {
                        uniValueString[valueStringLength++] = UNICODE_PERCENT_SIGN_CODEPOINT;   /* adding % */
                        uniValueString[valueStringLength++] = UNICODE_X_CODEPOINT;              /* adding X */
                        itou(uniValueString, valueStringLength, buffer[i++] & UConverterConstants.UNSIGNED_BYTE_MASK, 16, 2);
                        valueStringLength += 2;
                    }
                }
            }

            cr = CharsetDecoderICU.toUWriteUChars(decoder, uniValueString, 0, valueStringLength, target, offsets, 0);

            return cr;
        }
    };
    /***
     * Java port of uprv_itou() in ICU4C used by TO_U_CALLBACK_ESCAPE and FROM_U_CALLBACK_ESCAPE.
     * Fills in a char string with the radix-based representation of a number padded with zeroes
     * to minwidth.
     */
    private static final int itou(char[] buffer, int sourceIndex, int i, int radix, int minwidth) {
        int length = 0;
        int digit;
        int j;
        char temp;

        do {
            digit = i % radix;
            buffer[sourceIndex + length++] = (char)(digit <= 9 ? (0x0030+digit) : (0x0030+digit+7));
            i = i/radix;
        } while (i != 0 && (sourceIndex + length) < buffer.length);

        while (length < minwidth) {
            buffer[sourceIndex + length++] = (char)0x0030; /* zero padding */
        }
        /* reverses the string */
        for (j = 0; j < (length / 2); j++) {
            temp = buffer[(sourceIndex + length - 1) - j];
            buffer[(sourceIndex + length-1) -j] = buffer[sourceIndex + j];
            buffer[sourceIndex + j] = temp;
        }

        return length;
    }

    /*
     * No need to create an instance
     */
    private CharsetCallback() {
    }
}
