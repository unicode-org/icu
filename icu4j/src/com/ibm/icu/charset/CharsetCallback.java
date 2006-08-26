/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
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


/*public*/ class CharsetCallback {
    /**
     * FROM_U, TO_U context options for sub callback
     * @draft ICU 3.6
     */
    /*public*/ static final String SUB_STOP_ON_ILLEGAL = "i";

    /**
     * FROM_U, TO_U context options for skip callback
     * @draft ICU 3.6
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
     */
    /*public*/ static final String ESCAPE_C        = "C";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to XML Decimal escape \htmlonly(&amp;#DDDD;)\endhtmlonly
     * TO_U_CALLBACK_ESCAPE context option to escape the character value accoding to XML Decimal escape \htmlonly(&amp;#DDDD;)\endhtmlonly
     * @draft ICU 3.6
     */
    /*public*/ static final String ESCAPE_XML_DEC  = "D";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape the code unit according to XML Hex escape \htmlonly(&amp;#xXXXX;)\endhtmlonly
     * TO_U_CALLBACK_ESCAPE context option to escape the character value accoding to XML Hex escape \htmlonly(&amp;#xXXXX;)\endhtmlonly
     * @draft ICU 3.6
     */
    /*public*/ static final String ESCAPE_XML_HEX  = "X";
    /**
     * FROM_U_CALLBACK_ESCAPE context option to escape teh code unit according to Unicode (U+XXXXX)
     * @draft ICU 3.6
     */
    /*public*/ static final String ESCAPE_UNICODE  = "U";

    public interface Decoder {
        public CoderResult call(CharsetDecoderICU decoder, Object context, 
                                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                                char[] buffer, int length, CoderResult cr);
    }

    public interface Encoder {
        public CoderResult call(CharsetEncoderICU encoder, Object context, 
                                CharBuffer source, ByteBuffer target, IntBuffer offsets, 
                                char[] buffer, int length, int cp, CoderResult cr);
    }    
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

    public static final Decoder TO_U_CALLBACK_SUBSTITUTE  = new Decoder() {
        public CoderResult call(CharsetDecoderICU decoder, Object context, 
                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                char[] buffer, int length, CoderResult cr){

            if(context==null){
                return decoder.cbToUWriteSub(decoder, source, target, offsets);
            }else if(((String)context).equals(SUB_STOP_ON_ILLEGAL)){
                if(!cr.isUnmappable()){
                    return cr;
                }else{
                   return decoder.cbToUWriteSub(decoder, source, target, offsets);
                }
            }
            return cr;
        }
    };

    public static final Encoder FROM_U_CALLBACK_STOP = new Encoder() {
        public CoderResult call(CharsetEncoderICU encoder, Object context, 
                CharBuffer source, ByteBuffer target, IntBuffer offsets, 
                char[] buffer, int length, int cp, CoderResult cr){
            return cr;
        }
    };
    public static final Decoder TO_U_CALLBACK_STOP = new Decoder() {
        public CoderResult call(CharsetDecoderICU decoder, Object context, 
                ByteBuffer source, CharBuffer target, IntBuffer offsets,
                char[] buffer, int length, CoderResult cr){
            return cr;
        }
    };  
}
