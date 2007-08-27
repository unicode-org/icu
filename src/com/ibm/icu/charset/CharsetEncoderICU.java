/**
*******************************************************************************
* Copyright (C) 2006-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.				                                  *
*******************************************************************************
*
*******************************************************************************
*/

package com.ibm.icu.charset;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * An abstract class that provides framework methods of decoding operations for concrete
 * subclasses. 
 * In the future this class will contain API that will implement converter sematics of ICU4C.
 * @draft ICU 3.6
 * @provisional This API might change or be removed in a future release.
 */
public abstract class CharsetEncoderICU extends CharsetEncoder {

    static final int NEED_TO_WRITE_BOM = 1;
    boolean writeBOM = false; /* only used by UTF-16, UTF-32 */
    
    byte[] errorBuffer = new byte[30];
    int errorBufferLength = 0;
    
    /** these are for encodeLoopICU */
    int fromUnicodeStatus;
    int fromUChar32;
    boolean useSubChar1;
    boolean useFallback;
    
    /* maximum number of indexed UChars */
    private static final int EXT_MAX_UCHARS = 19;
    
    /* store previous UChars/chars to continue partial matches */
    int preFromUFirstCP; /* >=0: partial match */
    char[] preFromUArray = new char[EXT_MAX_UCHARS];
    int preFromUBegin;
    int preFromULength;    /* negative: replay */
    
    char[] invalidUCharBuffer = new char[2];    
    int    invalidUCharLength;
    Object fromUContext;
    private CharsetCallback.Encoder onUnmappableInput = CharsetCallback.FROM_U_CALLBACK_STOP;
    private CharsetCallback.Encoder onMalformedInput = CharsetCallback.FROM_U_CALLBACK_STOP;
    CharsetCallback.Encoder fromCharErrorBehaviour = new CharsetCallback.Encoder() {
        public CoderResult call(CharsetEncoderICU encoder, Object context, CharBuffer source,
                ByteBuffer target, IntBuffer offsets, char[] buffer, int length, int cp,
                CoderResult cr) {
            if (cr.isUnmappable()) {
                return onUnmappableInput.call(encoder, context, source, target, offsets, buffer,
                        length, cp, cr);
            } else if (cr.isMalformed()) {
                return onMalformedInput.call(encoder, context, source, target, offsets, buffer,
                        length, cp, cr);
            }
            return CharsetCallback.FROM_U_CALLBACK_STOP.call(encoder, context, source, target,
                    offsets, buffer, length, cp, cr);

        }
    };

   /**
     * Construcs a new encoder for the given charset
     * 
     * @param cs
     *            for which the decoder is created
     * @param replacement
     *            the substitution bytes
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    CharsetEncoderICU(CharsetICU cs, byte[] replacement) {
        super(cs, (cs.minBytesPerChar+cs.maxBytesPerChar)/2, cs.maxBytesPerChar, replacement);
    }

    /**
     * Is this Encoder allowed to use fallbacks? A fallback mapping is a mapping
     * that will convert a Unicode codepoint sequence to a byte sequence, but
     * the encoded byte sequence will round trip convert to a different
     * Unicode codepoint sequence.
     * @return true if the converter uses fallback, false otherwise.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isFallbackUsed() {
        return useFallback;
    }
    
    /**
     * Sets whether this Encoder can use fallbacks?
     * @param usesFallback true if the user wants the converter to take
     *  advantage of the fallback mapping, false otherwise.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public void setFallbackUsed(boolean usesFallback) {
        useFallback = usesFallback;
    }

    /**
     * Use fallbacks from Unicode to codepage when useFallback or for private-use code points
     * @param c A codepoint
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    final boolean isFromUUseFallback(int c) {
        return (useFallback) || (UCharacter.getType(c) == UCharacter.PRIVATE_USE);
    }
    
    /**
     * Sets the action to be taken if an illegal sequence is encountered
     * 
     * @param newAction
     *            action to be taken
     * @exception IllegalArgumentException
     * @stable ICU 3.6
     */
	protected void implOnMalformedInput(CodingErrorAction newAction) {
	    onMalformedInput = getCallback(newAction);
	}

	/**
     * Sets the action to be taken if an illegal sequence is encountered
     * 
     * @param newAction
     *            action to be taken
     * @exception IllegalArgumentException
     * @stable ICU 3.6
     */
    protected void implOnUnmappableCharacter(CodingErrorAction newAction) {
        onUnmappableInput = getCallback(newAction);
	}
    
    private static CharsetCallback.Encoder getCallback(CodingErrorAction action){
        if(action==CodingErrorAction.REPLACE){
            return CharsetCallback.FROM_U_CALLBACK_SUBSTITUTE;
        }else if(action==CodingErrorAction.IGNORE){
            return CharsetCallback.FROM_U_CALLBACK_SKIP;
        }else if(action==CodingErrorAction.REPORT){
            return CharsetCallback.FROM_U_CALLBACK_STOP;
        }
        return CharsetCallback.FROM_U_CALLBACK_STOP;
    }

    private static final CharBuffer EMPTY = CharBuffer.allocate(0);
	/**
	 * Flushes any characters saved in the converter's internal buffer and
	 * resets the converter.
	 * @param out action to be taken
	 * @return result of flushing action and completes the decoding all input. 
	 *	   Returns CoderResult.UNDERFLOW if the action succeeds.
     * @stable ICU 3.6
	 */
    protected CoderResult implFlush(ByteBuffer out) {
        return encode(EMPTY, out, null, true);
	}

	/**
	 * Resets the from Unicode mode of converter
     * @stable ICU 3.6
	 */
    protected void implReset() {
	    errorBufferLength=0;
        fromUnicodeStatus = 0;
        fromUChar32 = 0;
        fromUnicodeReset();
	}
    
    private void fromUnicodeReset() {
        preFromUBegin = 0;
        preFromUFirstCP = UConverterConstants.U_SENTINEL;
        preFromULength = 0;
    }

	/**
	 * Encodes one or more chars. The default behaviour of the
	 * converter is stop and report if an error in input stream is encountered.
	 * To set different behaviour use @see CharsetEncoder.onMalformedInput()
	 * @param in buffer to decode
	 * @param out buffer to populate with decoded result
	 * @return result of decoding action. Returns CoderResult.UNDERFLOW if the decoding
	 *	   action succeeds or more input is needed for completing the decoding action.
     * @stable ICU 3.6
	 */
    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        if(!in.hasRemaining()){
            // The Java framework should have already substituted what was left.
            fromUChar32 = 0;
            //fromUnicodeReset();
            return CoderResult.UNDERFLOW;
        }
        in.position(in.position()+fromUCountPending());
        /* do the conversion */
        CoderResult ret = encode(in, out, null, false);
        setSourcePosition(in);
        if(ret.isUnderflow() && in.hasRemaining()){
            // The Java framework is going to substitute what is left.
            fromUnicodeReset();
        }
        return ret;
    }

    /**
     * Implements ICU semantics of buffer management
     * @param source
     * @param target
     * @param offsets
     * @return A CoderResult object that contains the error result when an error occurs.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    abstract CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush);
    
    /**
     * Implements ICU semantics for encoding the buffer
     * @param source The input character buffer
     * @param target The output byte buffer
     * @param offsets
     * @param flush true if, and only if, the invoker can provide no
     *  additional input bytes beyond those in the given buffer.
     * @return A CoderResult object that contains the error result when an error occurs.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    final CoderResult encode(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){

    
        /* check parameters */    
        if(target==null || source==null) {
            throw new IllegalArgumentException();
        }

        /*
         * Make sure that the buffer sizes do not exceed the number range for
         * int32_t because some functions use the size (in units or bytes)
         * rather than comparing pointers, and because offsets are int32_t values.
         *
         * size_t is guaranteed to be unsigned and large enough for the job.
         *
         * Return with an error instead of adjusting the limits because we would
         * not be able to maintain the semantics that either the source must be
         * consumed or the target filled (unless an error occurs).
         * An adjustment would be targetLimit=t+0x7fffffff; for example.
         */
        
        /* flush the target overflow buffer */
        if(errorBufferLength>0) {
            byte[] overflowArray;
            int i, length;
    
            overflowArray=errorBuffer;
            length=errorBufferLength;
            i=0;
            do {
                if(target.remaining()==0) {
                    /* the overflow buffer contains too much, keep the rest */
                    int j=0;
    
                    do {
                        overflowArray[j++]=overflowArray[i++];
                    } while(i<length);
    
                    errorBufferLength=(byte)j;
                    return CoderResult.OVERFLOW;
                }
    
                /* copy the overflow contents to the target */
                target.put(overflowArray[i++]);
                if(offsets!=null) {
                    offsets.put(-1); /* no source index available for old output */
                }
            } while(i<length);
    
            /* the overflow buffer is completely copied to the target */
            errorBufferLength=0;
        }
    
        if(!flush && source.remaining()==0 && preFromULength>=0) {
            /* the overflow buffer is emptied and there is no new input: we are done */
            return CoderResult.UNDERFLOW;
        }
    
        /*
         * Do not simply return with a buffer overflow error if
         * !flush && t==targetLimit
         * because it is possible that the source will not generate any output.
         * For example, the skip callback may be called;
         * it does not output anything.
         */
    
        return fromUnicodeWithCallback(source, target, offsets, flush);

    }

    /**
     * Implementation note for m:n conversions
     *
     * While collecting source units to find the longest match for m:n conversion,
     * some source units may need to be stored for a partial match.
     * When a second buffer does not yield a match on all of the previously stored
     * source units, then they must be "replayed", i.e., fed back into the converter.
     *
     * The code relies on the fact that replaying will not nest -
     * converting a replay buffer will not result in a replay.
     * This is because a replay is necessary only after the _continuation_ of a
     * partial match failed, but a replay buffer is converted as a whole.
     * It may result in some of its units being stored again for a partial match,
     * but there will not be a continuation _during_ the replay which could fail.
     *
     * It is conceivable that a callback function could call the converter
     * recursively in a way that causes another replay to be stored, but that
     * would be an error in the callback function.
     * Such violations will cause assertion failures in a debug build,
     * and wrong output, but they will not cause a crash.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    final CoderResult fromUnicodeWithCallback(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){
        int sBufferIndex;
        int sourceIndex;
        int errorInputLength;
        boolean converterSawEndOfInput, calledCallback;
        

        /* variables for m:n conversion */
        CharBuffer replayArray = CharBuffer.allocate(EXT_MAX_UCHARS);
        int replayArrayIndex=0;
        CharBuffer realSource;
        boolean realFlush;
        
        CoderResult cr = CoderResult.UNDERFLOW;
        
        /* get the converter implementation function */
        sourceIndex=0;

        if(preFromULength>=0) {
            /* normal mode */
            realSource=null;    
            realFlush=false;
        } else {
            /*
             * Previous m:n conversion stored source units from a partial match
             * and failed to consume all of them.
             * We need to "replay" them from a temporary buffer and convert them first.
             */
            realSource=source;
            realFlush = flush;
            
            //UConverterUtility.uprv_memcpy(replayArray, replayArrayIndex, preFromUArray, 0, -preFromULength*UMachine.U_SIZEOF_UCHAR);
            replayArray.put(preFromUArray,0, -preFromULength);
            source.position(replayArrayIndex);
            source.limit(replayArrayIndex-preFromULength); //preFromULength is negative, see declaration
            source=replayArray;
            flush=false;
            
            preFromULength=0;
        }

        /*
         * loop for conversion and error handling
         *
         * loop {
         *   convert
         *   loop {
         *     update offsets
         *     handle end of input
         *     handle errors/call callback
         *   }
         * }
         */
        for(;;) {
            /* convert */
            cr = encodeLoop(source, target, offsets, flush);
            /*
             * set a flag for whether the converter
             * successfully processed the end of the input
             *
             * need not check cnv.preFromULength==0 because a replay (<0) will cause
             * s<sourceLimit before converterSawEndOfInput is checked
             */
            converterSawEndOfInput= (boolean)(cr.isUnderflow() && flush && source.remaining()==0 && fromUChar32==0);
    
            /* no callback called yet for this iteration */
            calledCallback=false;
    
            /* no sourceIndex adjustment for conversion, only for callback output */
            errorInputLength=0;

            /*
             * loop for offsets and error handling
             *
             * iterates at most 3 times:
             * 1. to clean up after the conversion function
             * 2. after the callback
             * 3. after the callback again if there was truncated input
             */
            for(;;) {
                /* update offsets if we write any */
                if(offsets!=null) {
                    int length = target.remaining();
                    if(length>0) {
    
                        /*
                         * if a converter handles offsets and updates the offsets
                         * pointer at the end, then offset should not change
                         * here;
                         * however, some converters do not handle offsets at all
                         * (sourceIndex<0) or may not update the offsets pointer
                         */
                        offsets.position(offsets.position()+length);
                    }
    
                    if(sourceIndex>=0) {
                        sourceIndex+=(int)(source.position());
                    }
                }

                if(preFromULength<0) {
                    /*
                     * switch the source to new replay units (cannot occur while replaying)
                     * after offset handling and before end-of-input and callback handling
                     */
                    if(realSource==null) {
                        realSource=source;
                        realFlush=flush;
    
                        //UConverterUtility.uprv_memcpy(replayArray, replayArrayIndex, preFromUArray, 0, -preFromULength*UMachine.U_SIZEOF_UCHAR);
                        replayArray.put(preFromUArray,0, -preFromULength);
                        
                        source=replayArray;
                        source.position(replayArrayIndex);
                        source.limit(replayArrayIndex-preFromULength);
                        flush=false;
                        if((sourceIndex+=preFromULength)<0) {
                            sourceIndex=-1;
                        }
    
                        preFromULength=0;
                    } else {
                        /* see implementation note before _fromUnicodeWithCallback() */
                        //agljport:todo U_ASSERT(realSource==NULL);
                        Assert.assrt(realSource==null);
                    }
                }

                /* update pointers */
                sBufferIndex=source.position();
                if(cr.isUnderflow()) {
                    if(sBufferIndex<source.limit()) {
                        /*
                         * continue with the conversion loop while there is still input left
                         * (continue converting by breaking out of only the inner loop)
                         */
                        break;
                    } else if(realSource!=null) {
                        /* switch back from replaying to the real source and continue */
                        source=realSource;
                        flush=realFlush;
                        sourceIndex=source.position();
                        realSource=null;
                        break;
                    } else if(flush && fromUChar32!=0) {
                        /*
                         * the entire input stream is consumed
                         * and there is a partial, truncated input sequence left
                         */
    
                        /* inject an error and continue with callback handling */
                        //err[0]=ErrorCode.U_TRUNCATED_CHAR_FOUND;
                        cr = CoderResult.malformedForLength(1);
                        calledCallback=false; /* new error condition */
                    } else {
                        /* input consumed */
                        if(flush) {
                            /*
                             * return to the conversion loop once more if the flush
                             * flag is set and the conversion function has not
                             * successfully processed the end of the input yet
                             *
                             * (continue converting by breaking out of only the inner loop)
                             */
                            if(!converterSawEndOfInput) {
                                break;
                            }
    
                            /* reset the converter without calling the callback function */
                            implReset();
                        }
    
                        /* done successfully */
                        return cr;
                    }
                }

                /*U_FAILURE(*err) */
                {
    
                    if( calledCallback || cr.isOverflow() ||
                        (!cr.isMalformed() && !cr.isUnmappable())
                      ){
                        /*
                         * the callback did not or cannot resolve the error:
                         * set output pointers and return
                         *
                         * the check for buffer overflow is redundant but it is
                         * a high-runner case and hopefully documents the intent
                         * well
                         *
                         * if we were replaying, then the replay buffer must be
                         * copied back into the UConverter
                         * and the real arguments must be restored
                         */
                        if(realSource!=null) {
                            int length;
    
                            //agljport:todo U_ASSERT(cnv.preFromULength==0);
    
                            length=source.remaining();
                            if(length>0) {
                                //UConverterUtility.uprv_memcpy(preFromUArray, 0, sourceArray, pArgs.sourceBegin, length*UMachine.U_SIZEOF_UCHAR);
                                source.get(preFromUArray, 0, length );
                                preFromULength=(byte)-length;
                            }
                            source=realSource;
                            flush=realFlush;
                        }
                        return cr;
                    }
                }

                /* callback handling */
                {
                    /* get and write the code point */
                    errorInputLength = UTF16.append(invalidUCharBuffer, 0, fromUChar32);
                    invalidUCharLength = errorInputLength;
    
                    /* set the converter state to deal with the next character */
                    fromUChar32=0;
    
                    /* call the callback function */
                    cr = fromCharErrorBehaviour.call(this, fromUContext, source, target, offsets, invalidUCharBuffer, invalidUCharLength, fromUChar32, cr);
                }
    
                /*
                 * loop back to the offset handling
                 *
                 * this flag will indicate after offset handling
                 * that a callback was called;
                 * if the callback did not resolve the error, then we return
                 */
                calledCallback=true;
            }
        }
    }
	/*
	 * Ascertains if a given Unicode code point (32bit value for handling surrogates)
	 * can be converted to the target encoding. If the caller wants to test if a
	 * surrogate pair can be converted to target encoding then the
	 * responsibility of assembling the int value lies with the caller.
	 * For assembling a code point the caller can use UTF16 class of ICU4J and do something like:
	 * <pre>
	 * while(i<mySource.length){
	 *	  if(UTF16.isLeadSurrogate(mySource[i])&& i+1< mySource.length){
	 *	      if(UTF16.isTrailSurrogate(mySource[i+1])){
	 *	          int temp = UTF16.charAt(mySource,i,i+1,0);
	 *	          if(!((CharsetEncoderICU) myConv).canEncode(temp)){
	 *		  passed=false;
	 *	          }
	 *	          i++;
	 *	          i++;
	 *	      }
	 *	 }
	 * }
	 * </pre>
	 * or
	 * <pre>
	 * String src = new String(mySource);
	 * int i,codepoint;
	 * boolean passed = false;
	 * while(i<src.length()){
	 *	codepoint = UTF16.charAt(src,i);
	 *	i+= (codepoint>0xfff)? 2:1;
	 *	if(!(CharsetEncoderICU) myConv).canEncode(codepoint)){
	 *	    passed = false;
	 *	}
	 * }
	 * </pre>
	 *
	 * @param codepoint Unicode code point as int value
	 * @return true if a character can be converted
     * @draft ICU 3.6
	 * @provisional This API might change or be removed in a future release.
	 */
    /* TODO This is different from Java's canEncode(char) API.
     * ICU's API should implement getUnicodeSet,
     * and override canEncode(char) which queries getUnicodeSet.
     * The getUnicodeSet should return a frozen UnicodeSet or use a fillin parameter, like ICU4C.
     */
	/*public boolean canEncode(int codepoint) {
	    return true;
    }*/
	/**
     * Overrides super class method
     * @stable ICU 3.6 
	 */
	public boolean isLegalReplacement(byte[] repl){
	    return true;
    }
    
    /**
     * Writes out the specified output bytes to the target byte buffer or to converter internal buffers.
     * @param cnv
     * @param bytesArray
     * @param bytesBegin
     * @param bytesLength
     * @param out
     * @param offsets
     * @param sourceIndex
     * @return A CoderResult object that contains the error result when an error occurs.
     * @draft ICU 3.6 
     * @provisional This API might change or be removed in a future release.
     */
    static final CoderResult fromUWriteBytes(CharsetEncoderICU cnv, 
                                         byte[] bytesArray, int bytesBegin, int bytesLength, 
                                         ByteBuffer out, IntBuffer offsets, int sourceIndex){

        //write bytes
        int obl = bytesLength;
        CoderResult cr = CoderResult.UNDERFLOW;
        int bytesLimit = bytesBegin + bytesLength;
        try{
            for (;bytesBegin< bytesLimit;){
                out.put(bytesArray[bytesBegin]);
                bytesBegin++;
            }
            // success 
            bytesLength=0;
        }catch( BufferOverflowException ex){
            cr = CoderResult.OVERFLOW;
        }
        
    
        if(offsets!=null) {
            while(obl>bytesLength) {
                offsets.put(sourceIndex);
                --obl;
            }
        }
        //write overflow 
        cnv.errorBufferLength = bytesLimit - bytesBegin;
        if(cnv.errorBufferLength >0) {
            int index = 0;     
            while(bytesBegin<bytesLimit) {
                cnv.errorBuffer[index++]=bytesArray[bytesBegin++];
            } 
            cr = CoderResult.OVERFLOW;
        }
        return  cr;
    }   

    /**
     * Returns the number of chars held in the converter's internal state
     * because more input is needed for completing the conversion. This function is 
     * useful for mapping semantics of ICU's converter interface to those of iconv,
     * and this information is not needed for normal conversion.
     * @return The number of chars in the state. -1 if an error is encountered.
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    /*public*/ int fromUCountPending(){    
        if(preFromULength > 0){
            return UTF16.getCharCount(preFromUFirstCP)+preFromULength ;
        }else if(preFromULength < 0){
            return -preFromULength ;
        }else if(fromUChar32 > 0){
            return 1;
        }else if(preFromUFirstCP >0){
            return UTF16.getCharCount(preFromUFirstCP);
        }
        return 0; 
     }
    /**
     * 
     * @param source
     */
    private final void setSourcePosition(CharBuffer source){
        
        // ok was there input held in the previous invocation of decodeLoop 
        // that resulted in output in this invocation?
        source.position(source.position() - fromUCountPending());
    }
    /**
     * Write the codepage substitution character.
     * Subclasses to override this method.
     * For stateful converters, it is typically necessary to handle this
     * specificially for the converter in order to properly maintain the state.
     * @param source The input character buffer
     * @param target The output byte buffer
     * @param offsets
     * @return A CoderResult object that contains the error result when an error occurs.
     * @draft ICU 3.6 
     * @provisional This API might change or be removed in a future release.
     */
    CoderResult cbFromUWriteSub (CharsetEncoderICU encoder, 
                                           CharBuffer source, ByteBuffer target, 
                                           IntBuffer offsets){
        CharsetICU cs = (CharsetICU) encoder.charset();
        byte[] sub = encoder.replacement();
        if (cs.subChar1 != 0 && encoder.invalidUCharBuffer[0] <= 0xff) {
            return CharsetEncoderICU.fromUWriteBytes(encoder,
                    new byte[] { cs.subChar1 }, 0, 1, target, offsets, source
                            .position());
        } else {
            return CharsetEncoderICU.fromUWriteBytes(encoder, sub, 0,
                    sub.length, target, offsets, source.position());
        }
    }
}
