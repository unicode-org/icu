/**
*******************************************************************************
* Copyright (C) 2006-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
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
 * @draft ICU 3.6
 * @provisional This API might change or be removed in a future release.
 */

public class CharsetCallback {
   /**
     * FROM_U, TO_U context options for sub callback
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String SUB_STOP_ON_ILLEGAL = "i";

    /**
     * FROM_U, TO_U context options for skip callback
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String SKIP_STOP_ON_ILLEGAL = "i";

    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to ICU (%UXXXX) 
     * @draft ICU 3.6
     */
    /*public*/ static final String ESCAPE_ICU  = null;
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to JAVA (\\uXXXX)
     * @draft ICU 3.6
     */
    /*public*/ static final String ESCAPE_JAVA     =  "J";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to C (\\uXXXX \\UXXXXXXXX)
     * TO_U_CALLBACK_ESCAPE option to escape the character value accoding to C (\\xXXXX)
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String ESCAPE_C        = "C";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to XML Decimal escape \htmlonly(&amp;#DDDD;)\endhtmlonly
     * TO_U_CALLBACK_ESCAPE context option to escape the character value accoding to XML Decimal escape \htmlonly(&amp;#DDDD;)\endhtmlonly
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String ESCAPE_XML_DEC  = "D";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to XML Hex escape \htmlonly(&amp;#xXXXX;)\endhtmlonly
     * TO_U_CALLBACK_ESCAPE context option to escape the character value according to XML Hex escape \htmlonly(&amp;#xXXXX;)\endhtmlonly
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String ESCAPE_XML_HEX  = "X";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to Unicode (U+XXXXX)
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String ESCAPE_UNICODE  = "U";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to Unicode (U+XXXXX)
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String ESCAPE_CSS2  = "S";

    /**
     * Decoder Callback interface
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public interface Decoder {
        /**
         * This function is called when the bytes in the source cannot be handled,
         * and this function is meant to handle or fix the error if possible.
         * 
         * @return Result of decoding action. This returned object is set to an error
         *  if this function could not handle the conversion.
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public CoderResult call(CharsetDecoderICU decoder, Object context, 
                                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                                char[] buffer, int length, CoderResult cr);
    }
    /**
     * Encoder Callback interface
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public interface Encoder {
        /**
         * This function is called when the Unicode characters in the source cannot be handled,
         * and this function is meant to handle or fix the error if possible.
         * @return Result of decoding action. This returned object is set to an error
         *  if this function could not handle the conversion.
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public CoderResult call(CharsetEncoderICU encoder, Object context, 
                                CharBuffer source, ByteBuffer target, IntBuffer offsets, 
                                char[] buffer, int length, int cp, CoderResult cr);
    }    
    /**
     * Skip callback
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static final Encoder FROM_U_CALLBACK_SKIP = new Encoder() {
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
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static final Decoder TO_U_CALLBACK_SKIP = new Decoder() {
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
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static final Encoder FROM_U_CALLBACK_SUBSTITUTE = new Encoder(){        
        public CoderResult call(CharsetEncoderICU encoder, Object context, 
                CharBuffer source, ByteBuffer target, IntBuffer offsets, 
                char[] buffer, int length, int cp, CoderResult cr){
            if(context==null){
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
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static final Decoder TO_U_CALLBACK_SUBSTITUTE  = new Decoder() {
        public CoderResult call(CharsetDecoderICU decoder, Object context, 
                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                char[] buffer, int length, CoderResult cr){

            CharsetICU cs = (CharsetICU) decoder.charset();
            /* could optimize this case, just one uchar */
            if(decoder.invalidCharLength == 1 && cs.subChar1 != 0) {
                return CharsetDecoderICU.toUWriteUChars(decoder, kSubstituteChar1, 0, 1, target, offsets, source.position());
            } else {
                return CharsetDecoderICU.toUWriteUChars(decoder, kSubstituteChar, 0, 1, target, offsets, source.position());
            }
        }
    };
    /**
     * Stop callback
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static final Encoder FROM_U_CALLBACK_STOP = new Encoder() {
        public CoderResult call(CharsetEncoderICU encoder, Object context, 
                CharBuffer source, ByteBuffer target, IntBuffer offsets, 
                char[] buffer, int length, int cp, CoderResult cr){
            return cr;
        }
    };
    /**
     * Stop callback
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static final Decoder TO_U_CALLBACK_STOP = new Decoder() {
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
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public static final Encoder FROM_U_CALLBACK_ESCAPE = new Encoder() {
        public CoderResult call(CharsetEncoderICU encoder, Object context, 
                CharBuffer source, ByteBuffer target, IntBuffer offsets, 
                char[] buffer, int length, int cp, CoderResult cr){
            char[] valueString = new char[VALUE_STRING_LENGTH];
            int valueStringLength = 0;
            int i = 0;
            
            cr = CoderResult.UNDERFLOW;
            
            if (context == null || !(context instanceof String)) {
                while (i < length) {
                    valueString[valueStringLength++] = UNICODE_PERCENT_SIGN_CODEPOINT; /* adding % */
                    valueString[valueStringLength++] = UNICODE_U_CODEPOINT; /* adding U */
                    valueStringLength += itou(valueString, valueStringLength, (int)buffer[i++] & UConverterConstants.UNSIGNED_SHORT_MASK, 16, 4);
                }
            } else {
                if (((String)context).equals(ESCAPE_JAVA)) {
                    while (i < length) {
                        valueString[valueStringLength++] = UNICODE_RS_CODEPOINT;    /* adding \ */
                        valueString[valueStringLength++] = UNICODE_U_LOW_CODEPOINT; /* adding u */
                        valueStringLength += itou(valueString, valueStringLength, (int)buffer[i++] & UConverterConstants.UNSIGNED_SHORT_MASK, 16, 4);
                    }
                } else if (((String)context).equals(ESCAPE_C)) {
                    valueString[valueStringLength++] = UNICODE_RS_CODEPOINT;    /* adding \ */
                    
                    if (length == 2) {
                        valueString[valueStringLength++] = UNICODE_U_CODEPOINT; /* adding U */
                        valueStringLength = itou(valueString, valueStringLength, cp, 16, 8);
                    } else {
                        valueString[valueStringLength++] = UNICODE_U_LOW_CODEPOINT; /* adding u */
                        valueStringLength += itou(valueString, valueStringLength, (int)buffer[0] & UConverterConstants.UNSIGNED_SHORT_MASK, 16, 4);
                    }
                } else if (((String)context).equals(ESCAPE_XML_DEC)) {
                    valueString[valueStringLength++] = UNICODE_AMP_CODEPOINT;   /* adding & */
                    valueString[valueStringLength++] = UNICODE_HASH_CODEPOINT;  /* adding # */
                    if (length == 2) {
                        valueStringLength += itou(valueString, valueStringLength, cp, 10, 0);
                    } else {
                        valueStringLength += itou(valueString, valueStringLength, (int)buffer[0] & UConverterConstants.UNSIGNED_SHORT_MASK, 10, 0);
                    }
                    valueString[valueStringLength++] = UNICODE_SEMICOLON_CODEPOINT; /* adding ; */
                } else if (((String)context).equals(ESCAPE_XML_HEX)) {
                    valueString[valueStringLength++] = UNICODE_AMP_CODEPOINT;   /* adding & */
                    valueString[valueStringLength++] = UNICODE_HASH_CODEPOINT;  /* adding # */
                    valueString[valueStringLength++] = UNICODE_X_LOW_CODEPOINT; /* adding x */
                    if (length == 2) {
                        valueStringLength += itou(valueString, valueStringLength, cp, 16, 0);
                    } else {
                        valueStringLength += itou(valueString, valueStringLength, (int)buffer[0] & UConverterConstants.UNSIGNED_SHORT_MASK, 16, 0);
                    }
                    valueString[valueStringLength++] = UNICODE_SEMICOLON_CODEPOINT; /* adding ; */
                } else if (((String)context).equals(ESCAPE_UNICODE)) {
                    valueString[valueStringLength++] = UNICODE_LEFT_CURLY_CODEPOINT;    /* adding { */
                    valueString[valueStringLength++] = UNICODE_U_CODEPOINT;             /* adding U */
                    valueString[valueStringLength++] = UNICODE_PLUS_CODEPOINT;          /* adding + */
                    if (length == 2) {
                        valueStringLength += itou(valueString, valueStringLength,cp, 16, 4);
                    } else {
                        valueStringLength += itou(valueString, valueStringLength, (int)buffer[0] & UConverterConstants.UNSIGNED_SHORT_MASK, 16, 4);
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
                        valueStringLength += itou(valueString, valueStringLength, (int)buffer[i++] & UConverterConstants.UNSIGNED_SHORT_MASK, 16, 4);
                    }
                }
            }

            cr = encoder.cbFromUWriteUChars(encoder, CharBuffer.wrap(valueString, 0, valueStringLength), target, offsets);
            return cr;
        }
    };
    /**
     * Write escape callback
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public static final Decoder TO_U_CALLBACK_ESCAPE = new Decoder() {
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
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    private static final int itou(char[] buffer, int sourceIndex, int i, int radix, int minwidth) {
        int length = 0;
        int digit;
        int j;
        char temp;
        
        do {
            digit = (int)(i % radix);
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
}
