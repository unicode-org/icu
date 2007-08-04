/**
*******************************************************************************
* Copyright (C) 2006-2007, International Business Machines Corporation and    *
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

/*public*/ class CharsetCallback {
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
     * TO_U_CALLBACK_ESCAPE context option to escape the character value accoding to XML Hex escape \htmlonly(&amp;#xXXXX;)\endhtmlonly
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String ESCAPE_XML_HEX  = "X";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape teh code unit according to Unicode (U+XXXXX)
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ static final String ESCAPE_UNICODE  = "U";

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
     * Skip callback
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
     * Skip callback
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
     * Skip callback
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
     * Skip callback
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
}
