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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
/**
 * @author Niti Hantaweepant
 */
class CharsetUTF8 extends CharsetICU {
    
    protected byte[] fromUSubstitution = new byte[]{(byte)0xef, (byte)0xbf, (byte)0xbd};
    
    public CharsetUTF8(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 3; /* max 3 bytes per code unit from UTF-8 (4 bytes from surrogate _pair_) */
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
    }
    
    /* UTF-8 Conversion DATA
     *   for more information see Unicode Strandard 2.0 , Transformation Formats Appendix A-9
     */       
    private static final long OFFSETS_FROM_UTF8[] = {0,
  	  0x00000000L, 0x00003080L, 0x000E2080L,
  	  0x03C82080L, 0xFA082080L, 0x82082080L};
    
    private static final byte BYTES_FROM_UTF8[] = 
    {
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
      3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
    };
    
    /*
	 * Starting with Unicode 3.0.1:
	 * UTF-8 byte sequences of length N _must_ encode code points of or above utf8_minChar32[N];
	 * byte sequences with more than 4 bytes are illegal in UTF-8,
	 * which is tested with impossible values for them
	 */
	private static final long UTF8_MIN_CHAR32[] = { 0L, 0L, 0x80L, 0x800L, 0x10000L, 0xffffffffL, 0xffffffffL };
	
    private final boolean isCESU8 = this instanceof CharsetCESU8;
    
    class CharsetDecoderUTF8 extends CharsetDecoderICU{

        public CharsetDecoderUTF8(CharsetICU cs) {
            super(cs);
        }        
        
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
            CoderResult cr = CoderResult.UNDERFLOW;
        
            int sourceArrayIndex = source.position();
            int sourceLimit = source.limit();
            int ch = -1, ch2 = 0;
    	    int i = -1, inBytes = -1; // -1 should never happen
            boolean gotoMorebytes = false;
    	    
            if (toUnicodeStatus!=0 && target.hasRemaining())
            {
                inBytes = mode;             /* restore # of bytes to consume */
                i = toULength;              /* restore # of bytes consumed */
                toULength = 0;
        
                ch = toUnicodeStatus;       /*Stores the previously calculated ch from a previous call*/
                toUnicodeStatus = 0;
                gotoMorebytes = true;
            }

donefornow:
            while (sourceArrayIndex < sourceLimit && target.hasRemaining())
            {
                if (!gotoMorebytes) {
                    ch = source.get(sourceArrayIndex++) & UConverterConstants.UNSIGNED_BYTE_MASK;
                }
                if (ch >= 0x80 || gotoMorebytes) {
                    if (!gotoMorebytes) {
                        /* store the first char */
                        toUBytesArray[0] = (byte)ch;
                        inBytes = BYTES_FROM_UTF8[(int)ch]; /* lookup current sequence length */
                        i = 1;
                    }
                    gotoMorebytes = false;

//morebytes:
                    while (i < inBytes)
                    {
                        if (sourceArrayIndex < source.limit())
                        {
                            toUBytesArray[i] = (byte) (ch2 = source.get(sourceArrayIndex) & UConverterConstants.UNSIGNED_BYTE_MASK);
                            if (!isTrail((byte)ch2))
                            {
                                break; /* i < inBytes */
                            }
                            ch = (ch << 6) + ch2;
                            ++sourceArrayIndex;
                            i++;
                        }
                        else
                        {
                            /* stores a partially calculated target*/
                            toUnicodeStatus = ch;
                            mode = inBytes;
                            toULength = i;
                            break donefornow;
                        }
                    }
        
                    /* Remove the accumulated high bits */
                    ch -= OFFSETS_FROM_UTF8[inBytes];
        
                    /*
                     * Legal UTF-8 byte sequences in Unicode 3.0.1 and up:
                     * - use only trail bytes after a lead byte (checked above)
                     * - use the right number of trail bytes for a given lead byte
                     * - encode a code point <= U+10ffff
                     * - use the fewest possible number of bytes for their code points
                     * - use at most 4 bytes (for i>=5 it is 0x10ffff<utf8_minChar32[])
                     *
                     * Starting with Unicode 3.2, surrogate code points must not be encoded in UTF-8.
                     * There are no irregular sequences any more.
                     * In CESU-8, only surrogates, not supplementary code points, are encoded directly.
                     */
                    if (i == inBytes
                            && ch <= UConverterConstants.MAXIMUM_UTF
                            && ch >= UTF8_MIN_CHAR32[i]
                            && (isCESU8 ? i <= 3 : !UTF16.isSurrogate((char)ch)))
                    {
                        /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
                        toULength = 0;
                        if (ch <= UConverterConstants.MAXIMUM_UCS2) 
                        {
                            /* fits in 16 bits */
                            target.put((char) ch);
                        }
                        else
                        {
                            /* write out the surrogates */
                            ch -= UConverterConstants.HALF_BASE;
                            target.put((char) ((ch >>> UConverterConstants.HALF_SHIFT) + UConverterConstants.SURROGATE_HIGH_START));
                            ch = (ch & UConverterConstants.HALF_MASK) + UConverterConstants.SURROGATE_LOW_START;
                            if (target.hasRemaining())
                            {
                                target.put((char)ch);
                            }
                            else
                            {
                                /* Put in overflow buffer (not handled here) */
                                charErrorBufferArray[charErrorBufferBegin+0]=(char)ch;
                                charErrorBufferLength=1;
                                cr = CoderResult.OVERFLOW;
                                break;
                            }
                        }
                    }
                    else {
                        toULength = (byte)i;
                        cr = CoderResult.malformedForLength(toULength);
                        break;
                    }
                }
                else {
                    /* Simple case */
                    target.put((char)ch);
                }
            }
        
    	    if (sourceArrayIndex < source.limit() && !target.hasRemaining())
    	    {
    	        /* End of target buffer */
    	    	cr = CoderResult.OVERFLOW;
    	    }        	        	
        	
    	    source.position(sourceArrayIndex);
         
            return cr;
        }
        
    }
    class CharsetEncoderUTF8 extends CharsetEncoderICU{

        public CharsetEncoderUTF8(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }
        
        protected void implReset() {
            super.implReset();
        }
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){
            CoderResult cr = CoderResult.UNDERFLOW;
            
            int sourceArrayIndex = source.position();
            int sourceLimit = source.limit();
            int ch = -1;
            short indexToWrite;
            byte temp[] = new byte[4];
            boolean gotoLowSurrogate = false;
                            
            if (fromUChar32 != 0 && target.hasRemaining()){
                ch = fromUChar32;
                fromUChar32 = 0;
                
                gotoLowSurrogate = true;
            }
        
            while (sourceArrayIndex < sourceLimit && target.hasRemaining()){
                if (!gotoLowSurrogate) {
                    ch = source.get(sourceArrayIndex++);
                }
                if (ch < 0x80 && !gotoLowSurrogate){        /* Single byte */
                    target.put((byte)ch);
                }else if (ch < 0x800 && !gotoLowSurrogate) {  /* Double byte */
                    target.put((byte) ((ch >>> 6) | 0xc0));
                    if (target.hasRemaining()){
                        target.put((byte) ((ch & 0x3f) | 0x80));
                    }else{
                        errorBuffer[0] = (byte) ((ch & 0x3f) | 0x80);
                        errorBufferLength = 1;
                        cr = CoderResult.OVERFLOW;
                        break;
                    }
                }else{ /* Check for surrogates */
                    gotoLowSurrogate = false;
                    if(UTF16.isSurrogate((char)ch) && !isCESU8) {
//lowsurrogate:
                        if (sourceArrayIndex < source.limit()) {
                            /* test the following code unit */
                            char trail = source.get(sourceArrayIndex);
                            if(UTF16.isLeadSurrogate((char)ch) && UTF16.isTrailSurrogate(trail)) {
                                ++sourceArrayIndex;
                                ch = UCharacter.getCodePoint((char)ch, trail);
                                //ch2 = 0;
                                /* convert this supplementary code point */
                                /* exit this condition tree */
                            } 
                            else {
                                /* this is an unpaired trail or lead code unit */
                                /* callback(illegal) */
                                fromUChar32 = ch;
                                cr = CoderResult.malformedForLength(1);
                                break;
                            }
                        } 
                        else {
                            /* no more input */
                            fromUChar32 = ch;
                            break;
                        }
                    }
        
                    if (ch < UTF16.SUPPLEMENTARY_MIN_VALUE)
                    {
                        indexToWrite = 2;
                        temp[2] = (byte) ((ch >>> 12) | 0xe0);
                    }
                    else
                    {
                        indexToWrite = 3;
                        temp[3] = (byte) ((ch >>> 18) | 0xf0);
                        temp[2] = (byte) (((ch >>> 12) & 0x3f) | 0x80);
                    }
                    temp[1] = (byte) (((ch >>> 6) & 0x3f) | 0x80);
                    temp[0] = (byte) ((ch & 0x3f) | 0x80);
        
                    for (; indexToWrite >= 0; indexToWrite--)
                    {
                        if (target.hasRemaining())
                        {
                            target.put(temp[indexToWrite]);
                        }
                        else
                        {
                            errorBuffer[errorBufferLength++] = temp[indexToWrite];
                            cr = CoderResult.OVERFLOW;
                        }
                    }
                }
            }
        
            if (sourceArrayIndex < source.limit() && !target.hasRemaining()){
                cr = CoderResult.OVERFLOW;
            }
        
            source.position(sourceArrayIndex);

            return cr;
        }
    }
    
    /* single-code point definitions -------------------------------------------- */

    /*
     * Does this code unit (byte) encode a code point by itself (US-ASCII 0..0x7f)?
     * @param c 8-bit code unit (byte)
     * @return TRUE or FALSE
     * @draft ICU 3.6
     */
    //static final boolean isSingle(byte c) {return (((c)&0x80)==0);}

    /*
     * Is this code unit (byte) a UTF-8 lead byte?
     * @param c 8-bit code unit (byte)
     * @return TRUE or FALSE
     * @draft ICU 3.6
     */
    //static final boolean isLead(byte c) {return ((((c)-0xc0) & UConverterConstants.UNSIGNED_BYTE_MASK)<0x3e);}

    /**
     * Is this code unit (byte) a UTF-8 trail byte?
     * @param c 8-bit code unit (byte)
     * @return TRUE or FALSE
     * @draft ICU 3.6
     */
    static final boolean isTrail(byte c) {return (((c)&0xc0)==0x80);}

    /*
     * How many code units (bytes) are used for the UTF-8 encoding
     * of this Unicode code point?
     * @param c 32-bit code point
     * @return 1..4, or 0 if c is a surrogate or not a Unicode code point
     * @draft ICU 3.6
     */
    /*static final int length(int c)
    {
    	long uc = c & UConverterConstants.UNSIGNED_INT_MASK;
    	return
        (uc<=0x7f ? 1 : 
            (uc<=0x7ff ? 2 : 
                (uc<=0xd7ff ? 3 : 
                    (uc<=0xdfff || uc>0x10ffff ? 0 : 
                        (uc<=0xffff ? 3 : 4)
                    ) 
                ) 
            ) 
        );
    }*/
    
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderUTF8(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderUTF8(this);
    }
}

/*
 * The purpose of this class is to set isCESU8 to true in the super class,
 * and to allow the Charset framework to open the variant UTF-8 converter
 * without extra setup work.
 * CESU-8 encodes/decodes supplementary characters as 6 bytes instead of the
 * proper 4 bytes.
 */
class CharsetCESU8 extends CharsetUTF8 {
    public CharsetCESU8(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
}
