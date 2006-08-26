/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
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
import java.nio.charset.MalformedInputException;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.text.UTF16;


public abstract class CharsetEncoderICU extends CharsetEncoder {

    protected byte[] errorBuffer = new byte[30];
    protected int errorBufferLength = 0;
    
    /** these are for encodeLoopICU */
    protected int fromUnicodeStatus;
    protected int fromUChar32;
    protected boolean useSubChar1;
    
    /* store previous UChars/chars to continue partial matches */
    protected int preFromUFirstCP; /* >=0: partial match */
    protected char[] preFromUArray;
    protected int preFromUBegin;
    protected int preFromULength;    /* negative: replay */
    
    protected char[] invalidUCharBuffer = new char[2];    
    protected int    invalidUCharLength;
    protected Object fromUContext;
    private CharsetCallback.Encoder onUnmappableInput = CharsetCallback.FROM_U_CALLBACK_STOP;
    private CharsetCallback.Encoder onMalformedInput = CharsetCallback.FROM_U_CALLBACK_STOP;
    protected CharsetCallback.Encoder fromCharErrorBehaviour = new CharsetCallback.Encoder(){ 
                                                                        public CoderResult call(CharsetEncoderICU encoder, Object context, 
                                                                                                CharBuffer source, ByteBuffer target, IntBuffer offsets, 
                                                                                                char[] buffer, int length, int cp, CoderResult cr) {
                                                                                if(cr.isUnmappable()){
                                                                                    return onUnmappableInput.call(encoder, context, 
                                                                                                                  source, target, offsets, 
                                                                                                                  buffer, length, cp, cr);
                                                                                }else if(cr.isMalformed()){
                                                                                    return onMalformedInput.call(encoder, context, 
                                                                                                                 source, target, offsets, 
                                                                                                                 buffer, length, cp, cr);    
                                                                                }
                                                                                return CharsetCallback.FROM_U_CALLBACK_STOP.call(encoder, context, 
                                                                                                                                 source, target, offsets, 
                                                                                                                                 buffer, length, cp, cr);

                                                                        }
                                                                    };

   /** 
     * Construcs a new encoder for the given charset
     * @param cs for which the decoder is created
     * @param cHandle the address of ICU converter
     * @param replacement the substitution bytes
     * @draft ICU 3.6
     */
    protected CharsetEncoderICU(CharsetICU cs, byte[] replacement) {
        super(cs, (cs.minBytesPerChar+cs.maxBytesPerChar)/2, cs.maxBytesPerChar, replacement);
    }

	/**
	 * Sets the action to be taken if an illegal sequence is encountered
	 * @param newAction action to be taken
	 * @exception IllegalArgumentException
     * @draft ICU 3.6
	 */
	protected void implOnMalformedInput(CodingErrorAction newAction) {
	    onMalformedInput = getCallback(newAction);
	}

	/**
	 * Sets the action to be taken if an illegal sequence is encountered
	 * @param newAction action to be taken
	 * @exception IllegalArgumentException
     * @draft ICU 3.6
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

	/**
	 * Flushes any characters saved in the converter's internal buffer and
	 * resets the converter.
	 * @param out action to be taken
	 * @return result of flushing action and completes the decoding all input. 
	 *	   Returns CoderResult.UNDERFLOW if the action succeeds.
     * @draft ICU 3.6
	 */
	protected CoderResult implFlush(ByteBuffer out) {
        return CoderResult.UNDERFLOW;
	}

	/**
	 * Resets the from Unicode mode of converter
     * @draft ICU 3.6
	 */
	protected void implReset() {
	    errorBufferLength=0;
        fromUChar32=0;
        fromUnicodeStatus = 0;
        preFromUBegin = 0;
        preFromUFirstCP = 0;
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
     * @draft ICU 3.6
	 */
	protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
        if(!in.hasRemaining()){
            return CoderResult.UNDERFLOW;
        }
        in.position(in.position()+fromUCountPending());
        /* do the conversion */
        CoderResult ret = encode(in, out, null, false);
        setSourcePosition(in);
        return ret;
    }
    /**
     * Implements ICU semantics of buffer management
     * @param source
     * @param target
     * @param offsets
     * @return
     * @throws MalformedInputException
     */
    protected abstract CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets);
    
    /**
     * Implements ICU semantics for encoding the buffer
     * @param in
     * @param out
     * @return
     */
    protected final CoderResult encode(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){

    
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
        //Ram: not required
        //if( ((long)(sourceLimit-sArrayIndex)>(long)0x3fffffff && sourceLimit>sArrayIndex) || ((long)(targetLimit-tArrayIndex)>(long)0x7fffffff && targetLimit>tArrayIndex)) {
        //    err[0]=ErrorCode.U_ILLEGAL_ARGUMENT_ERROR;
        //    return;
        //}
        
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
    /* maximum number of indexed UChars */
    public static final int EXT_MAX_UCHARS = 19;
  
    protected final CoderResult fromUnicodeWithCallback(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){
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
            cr = encodeLoop(source, target, offsets);
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
                        (cr.isMalformed() && cr.isUnmappable())
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
	/**
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
	 * 
	 */
	public boolean canEncode(int codepoint) {
	    return true;
    }

	public boolean isLegalReplacement(byte[] repl){
	    return true;
    }

	/**
	 * Releases the system resources by cleanly closing ICU converter opened
	 * @exception Throwable exception thrown by super class' finalize method
     * @draft ICU 3.6
	 */
	protected void finalize() throws Throwable {
	}
    
    protected static final CoderResult fromUWriteBytes(CharsetEncoderICU cnv, 
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
            if(cnv!=null) {
                int index = 0;     
                while(bytesBegin<bytesLimit) {
                    cnv.errorBuffer[index++]=bytesArray[bytesBegin++];
                } 
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
     * @param cnv       The converter in which the input is held as internal state
     * @param status    ICU error code in/out parameter.
     *                  Must fulfill U_SUCCESS before the function call.
     * @return The number of chars in the state. -1 if an error is encountered.
     * @draft ICU 3.4
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
     */
    protected CoderResult cbFromUWriteSub (CharsetEncoderICU encoder, 
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
