/**
*******************************************************************************
* Copyright (C) 2006-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 

package com.ibm.icu.charset;

import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;

import com.ibm.icu.charset.CharsetCallback;
import com.ibm.icu.impl.Assert;

/**
 * An abstract class that provides framework methods of decoding operations for concrete
 * subclasses. 
 * In the future this class will contain API that will implement converter sematics of ICU4C.
 * @draft ICU 3.6
 * @provisional This API might change or be removed in a future release.
 */
public abstract class CharsetDecoderICU extends CharsetDecoder{ 

    int    toUnicodeStatus;
    byte[] toUBytesArray = new byte[128];
    int    toUBytesBegin = 0;
    int    toULength;
    char[] charErrorBufferArray = new char[128];
    int    charErrorBufferLength;
    int    charErrorBufferBegin;
    char[] invalidCharBuffer = new char[128];
    int    invalidCharLength;
    
    /* maximum number of indexed bytes */
    private static final int EXT_MAX_BYTES = 0x1f;

    /* store previous UChars/chars to continue partial matches */
    byte[] preToUArray = new byte[EXT_MAX_BYTES];
    int    preToUBegin;
    int    preToULength;       /* negative: replay */
    int    preToUFirstLength;  /* length of first character */
    int mode;
    
    Object toUContext = null;
    private CharsetCallback.Decoder onUnmappableInput = CharsetCallback.TO_U_CALLBACK_STOP;
    private CharsetCallback.Decoder onMalformedInput = CharsetCallback.TO_U_CALLBACK_STOP;
    CharsetCallback.Decoder toCharErrorBehaviour = new CharsetCallback.Decoder() {
        public CoderResult call(CharsetDecoderICU decoder, Object context, ByteBuffer source,
                CharBuffer target, IntBuffer offsets, char[] buffer, int length, CoderResult cr) {
            if (cr.isUnmappable()) {
                return onUnmappableInput.call(decoder, context, source, target, offsets, buffer,
                        length, cr);
            } else if (cr.isMalformed()) {
                return onMalformedInput.call(decoder, context, source, target, offsets, buffer,
                        length, cr);
            }
            return CharsetCallback.TO_U_CALLBACK_STOP.call(decoder, context, source, target,
                    offsets, buffer, length, cr);
        }
    };
                                                                
    /**
     * Construct a CharsetDecorderICU based on the information provided from a CharsetICU object.
     * 
     * @param cs The CharsetICU object containing information about how to charset to decode.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    CharsetDecoderICU(CharsetICU cs) {
        super(cs, (float) (1/(float)cs.maxCharsPerByte), cs.maxCharsPerByte);
    }

    /**
     * Is this Decoder allowed to use fallbacks? A fallback mapping is a mapping
     * that will convert a byte sequence to a Unicode codepoint sequence, but
     * the encoded Unicode codepoint sequence will round trip convert to a different
     * byte sequence. In ICU, this is can be called a reverse fallback.
     * @return A boolean
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    final boolean isFallbackUsed() {
        return true;
    }
    
    /**
     * Sets the action to be taken if an illegal sequence is encountered
     * 
     * @param newAction action to be taken
     * @exception IllegalArgumentException
     * @stable ICU 3.6
     */
    protected final void implOnMalformedInput(CodingErrorAction newAction) {
        onMalformedInput = getCallback(newAction);
    }
    
    /**
     * Sets the action to be taken if an illegal sequence is encountered
     * 
     * @param newAction action to be taken
     * @exception IllegalArgumentException
     * @stable ICU 3.6
     */
    protected final void implOnUnmappableCharacter(CodingErrorAction newAction) {
        onUnmappableInput = getCallback(newAction);
    }
    private static CharsetCallback.Decoder getCallback(CodingErrorAction action){
        if(action==CodingErrorAction.REPLACE){
            return CharsetCallback.TO_U_CALLBACK_SUBSTITUTE;
        }else if(action==CodingErrorAction.IGNORE){
            return CharsetCallback.TO_U_CALLBACK_SKIP;
        }else if(action==CodingErrorAction.REPORT){
            return CharsetCallback.TO_U_CALLBACK_STOP;
        }
        return CharsetCallback.TO_U_CALLBACK_STOP;
    }
    private final ByteBuffer EMPTY = ByteBuffer.allocate(0);
    /**
     * Flushes any characters saved in the converter's internal buffer and
     * resets the converter.
     * @param out action to be taken
     * @return result of flushing action and completes the decoding all input. 
     *         Returns CoderResult.UNDERFLOW if the action succeeds.
     * @stable ICU 3.6
     */
    protected final CoderResult implFlush(CharBuffer out) {
        return decode(EMPTY, out, null, true);
    }
    
    /**
     * Resets the to Unicode mode of converter
     * @stable ICU 3.6
     */
    protected void implReset() {
        toUnicodeStatus = 0 ;
        toULength = 0;
        charErrorBufferLength = 0;
        charErrorBufferBegin = 0;
        
        /* store previous UChars/chars to continue partial matches */
        preToUBegin = 0;
        preToULength = 0;       /* negative: replay */
        preToUFirstLength = 0; 

        mode = 0;
    }
      
    /**
     * Decodes one or more bytes. The default behaviour of the converter
     * is stop and report if an error in input stream is encountered. 
     * To set different behaviour use @see CharsetDecoder.onMalformedInput()
     * This  method allows a buffer by buffer conversion of a data stream.  
     * The state of the conversion is saved between calls to convert.  
     * Among other things, this means multibyte input sequences can be 
     * split between calls. If a call to convert results in an Error, the 
     * conversion may be continued by calling convert again with suitably 
     * modified parameters.All conversions should be finished with a call to 
     * the flush method.
     * @param in buffer to decode
     * @param out buffer to populate with decoded result
     * @return Result of decoding action. Returns CoderResult.UNDERFLOW if the decoding
     *         action succeeds or more input is needed for completing the decoding action.
     * @stable ICU 3.6
     */
    protected CoderResult decodeLoop(ByteBuffer in,CharBuffer out){
        if(!in.hasRemaining()){
            toULength = 0;
            return CoderResult.UNDERFLOW;
        }
        in.position(in.position()+toUCountPending());
        /* do the conversion */
        CoderResult ret = decode(in, out, null, false);

        setSourcePosition(in);
        //if(ret.isUnderflow() && in.hasRemaining()){
            // The Java framework is going to substitute what is left.
            //fromUnicodeReset();
        //}
        return ret;
	}
 
    /**
     * Implements the ICU semantic for decode operation
     * @param in The input byte buffer
     * @param out The output character buffer
     * @return Result of decoding action. Returns CoderResult.UNDERFLOW if the decoding
     *         action succeeds or more input is needed for completing the decoding action.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    abstract CoderResult decodeLoop(ByteBuffer in, CharBuffer out, IntBuffer offsets, boolean flush);
    
    /**
     * Implements the ICU semantic for decode operation
     * @param source The input byte buffer
     * @param target The output character buffer
     * @param offsets
     * @param flush true if, and only if, the invoker can provide no
     *  additional input bytes beyond those in the given buffer.
     * @return Result of decoding action. Returns CoderResult.UNDERFLOW if the decoding
     *         action succeeds or more input is needed for completing the decoding action.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    final CoderResult decode(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
    
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
         * An adjustment would be sourceLimit=t+0x7fffffff; for example.
         */
            /*agljport:fix
        if(
            ((size_t)(sourceLimit-s)>(size_t)0x7fffffff && sourceLimit>s) ||
            ((size_t)(targetLimit-t)>(size_t)0x3fffffff && targetLimit>t)
        ) {
            *err=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
            */
        
        /* flush the target overflow buffer */
        if(charErrorBufferLength>0) {
            char[] overflow = null;
            int i, length;
    
            overflow=charErrorBufferArray;
            length=charErrorBufferLength;
            i=0;
            do {
                if(target.remaining()<=0) {
                    /* the overflow buffer contains too much, keep the rest */
                    int j=0;
    
                    do {
                        overflow[j++]=overflow[i++];
                    } while(i<length);
    
                    charErrorBufferLength=(byte)j;
                    return CoderResult.OVERFLOW;
                }
    
                /* copy the overflow contents to the target */
                target.put(overflow[i++]);
                if(offsets!=null) {
                    offsets.put(-1); /* no source index available for old output */
                }
            } while(i<length);
    
            /* the overflow buffer is completely copied to the target */
            charErrorBufferLength=0;
        }
    
        if(!flush && source.remaining()==0 && preToULength>=0) {
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
    
        return toUnicodeWithCallback(source, target, offsets, flush);
    }

    private void updateOffsets(IntBuffer offsets,int length, int sourceIndex, int errorInputLength) {
        int limit;
        int delta, offset;

        if(sourceIndex>=0) {
            /*
             * adjust each offset by adding the previous sourceIndex
             * minus the length of the input sequence that caused an
             * error, if any
             */
            delta=sourceIndex-errorInputLength;
        } else {
            /*
             * set each offset to -1 because this conversion function
             * does not handle offsets
             */
            delta=-1;
        }
        limit=offsets.position()+length;
        if(delta==0) {
            /* most common case, nothing to do */
        } else if(delta>0) {
            /* add the delta to each offset (but not if the offset is <0) */
            while(offsets.position()<limit) {
                offset=offsets.get(offsets.position());
                if(offset>=0) {
                    offsets.put(offset+delta);
                }
                //FIXME: ++offsets;
            }
        } else /* delta<0 */ {
            /*
             * set each offset to -1 because this conversion function
             * does not handle offsets
             * or the error input sequence started in a previous buffer
             */
            while(offsets.position()<limit) {
                offsets.put(-1);
            }
        }
    }
    final CoderResult toUnicodeWithCallback(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush){
        
        int sourceIndex;
        int errorInputLength;
        boolean converterSawEndOfInput, calledCallback;
        int t=target.position();
        int s=source.position();
        /* variables for m:n conversion */
        ByteBuffer replayArray = ByteBuffer.allocate(EXT_MAX_BYTES);
        int replayArrayIndex = 0;
            
        ByteBuffer realSource=null;
        boolean realFlush=false;
        int realSourceIndex=0;
    

        CoderResult cr = CoderResult.UNDERFLOW;
        
        /* get the converter implementation function */
        sourceIndex=0;

        if(preToULength>=0) {
            /* normal mode */
        } else {
            /*
             * Previous m:n conversion stored source units from a partial match
             * and failed to consume all of them.
             * We need to "replay" them from a temporary buffer and convert them first.
             */
            realSource=source;
            realFlush=flush;
            realSourceIndex=sourceIndex;
            //UConverterUtility.uprv_memcpy(replayArray, replayBegin, preToUArray, preToUBegin, -preToULength);
            replayArray.put(preToUArray,0, -preToULength);
            source=replayArray;
            source.position(0);
            source.limit(replayArrayIndex-preToULength);
            flush=false;
            sourceIndex=-1;
            preToULength=0;
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
            if(cr.isUnderflow()) {
                /* convert */
                cr = decodeLoop(source, target, offsets, flush);
    
                /*
                 * set a flag for whether the converter
                 * successfully processed the end of the input
                 *
                 * need not check cnv->preToULength==0 because a replay (<0) will cause
                 * s<sourceLimit before converterSawEndOfInput is checked
                 */
                converterSawEndOfInput= (cr.isUnderflow() && flush && source.remaining()==0 && toULength==0);
            } else {
                /* handle error from getNextUChar() */
                converterSawEndOfInput=false;
            }
    
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

                    int length=(target.position()-t);
                    if(length>0) {
                        updateOffsets(offsets, length, sourceIndex, errorInputLength);
    
                                            
                        /*
                         * if a converter handles offsets and updates the offsets
                         * pointer at the end, then pArgs->offset should not change
                         * here;
                         * however, some converters do not handle offsets at all
                         * (sourceIndex<0) or may not update the offsets pointer
                         */
                        //TODO: pArgs->offsets=offsets+=length;
                    }
    
                    if(sourceIndex>=0) {
                        sourceIndex+=(source.position()-s);
                    }
                                    
                }
    
                if(preToULength<0) {
                    /*
                     * switch the source to new replay units (cannot occur while replaying)
                     * after offset handling and before end-of-input and callback handling
                     */
                    if(realSource==null)
                                    {
                        realSource=source;
                        realFlush=flush;
                        realSourceIndex=sourceIndex;
    
                        //UConverterUtility.uprv_memcpy(replayArray, replayBegin, preToUArray, preToUBegin, -preToULength);
                        replayArray.put(preToUArray,0, -preToULength);

                        source=replayArray;
                        source.limit(replayArrayIndex-preToULength);
                        flush=false;
                        if((sourceIndex+=preToULength)<0) {
                            sourceIndex=-1;
                        }
    
                        preToULength=0;
                    } else {
                        /* see implementation note before _fromUnicodeWithCallback() */
                        //agljport:todo U_ASSERT(realSource==NULL);
                       Assert.assrt(realSource==null);
                    }
                }
    
                /* update pointers */
                s=source.position();
                t=target.position();
    
                if(cr.isUnderflow()) {
                    if(s<source.limit())
                                    {
                        /*
                         * continue with the conversion loop while there is still input left
                         * (continue converting by breaking out of only the inner loop)
                         */
                        break;
                    } else if(realSource!=null) {
                        /* switch back from replaying to the real source and continue */
                        source = realSource;
                        flush=realFlush;
                        sourceIndex=realSourceIndex;
                        realSource=null;
                        break;
                    } else if(flush && toULength>0) {
                        /*
                         * the entire input stream is consumed
                         * and there is a partial, truncated input sequence left
                         */
    
                        /* inject an error and continue with callback handling */
                        cr = CoderResult.malformedForLength(toULength);
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
    
                /* U_FAILURE(*err) */
                {
    
                    if( calledCallback || cr.isOverflow() ||
                        (cr.isMalformed() && cr.isUnmappable())
                      ) {
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
                            Assert.assrt(preToULength==0);
                            length=(int)(source.limit()-source.position());
                            if(length>0) {
                                //UConverterUtility.uprv_memcpy(preToUArray, preToUBegin, pArgs.sourceArray, pArgs.sourceBegin, length);
                                source.get(preToUArray, preToUBegin, length);
                                preToULength=(byte)-length;
                            }
    
                            source=realSource;
                            flush=realFlush;
                        }
                        return cr;
                    }
                }
    
                /* copy toUBytes[] to invalidCharBuffer[] */
                errorInputLength=invalidCharLength=toULength;
                if(errorInputLength>0) {
                    copy(toUBytesArray, 0, invalidCharBuffer, 0, errorInputLength);
                }
    
                /* set the converter state to deal with the next character */
                toULength=0;
    
                /* call the callback function */
                cr = toCharErrorBehaviour.call(this, toUContext, source, target, offsets, invalidCharBuffer, errorInputLength, cr);
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
     * Returns the number of chars held in the converter's internal state
     * because more input is needed for completing the conversion. This function is 
     * useful for mapping semantics of ICU's converter interface to those of iconv,
     * and this information is not needed for normal conversion.
     * @return The number of chars in the state. -1 if an error is encountered.
     * @draft ICU 3.6
     */
    /*public*/ int toUCountPending()    {
        if(preToULength > 0){
            return preToULength ;
        }else if(preToULength < 0){
            return -preToULength;
        }else if(toULength > 0){
            return toULength;
        }
        return 0;
    }
    

    private final void setSourcePosition(ByteBuffer source){
        // ok was there input held in the previous invocation of decodeLoop 
        // that resulted in output in this invocation?
        source.position(source.position() - toUCountPending());
        
    }
    private void copy(byte[] src, int srcOffset, char[] dst, int dstOffset, int length) {
        for(int i=srcOffset; i<length; i++){
            dst[dstOffset++]=(char)src[srcOffset++];
        }
    }
    /**
     * ONLY used by ToU callback functions.
     * This function will write out the specified characters to the target
     * character buffer.
     * @return A CoderResult object that contains the error result when an error occurs.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final CoderResult toUWriteUChars( CharsetDecoderICU cnv,
                                                char[] ucharsArray, int ucharsBegin, int length,  
                                                CharBuffer target, IntBuffer offsets, int sourceIndex) {
        
        CoderResult cr = CoderResult.UNDERFLOW;
        
        /* write UChars */
        if(offsets==null) {
            while(length>0 && target.hasRemaining()) {
                target.put(ucharsArray[ucharsBegin++]);
                --length;
            }

        } else {
            /* output with offsets */
            while(length>0 && target.hasRemaining()) {
                target.put(ucharsArray[ucharsBegin++]);
                offsets.put(sourceIndex);
                --length;
            }
        }
        /* write overflow */
        if(length>0) {        
            cnv.charErrorBufferLength= 0;
            cr = CoderResult.OVERFLOW;
            do {
                cnv.charErrorBufferArray[cnv.charErrorBufferLength++]=ucharsArray[ucharsBegin++];
            } while(--length>0);
        }
        return cr;
    }
    /**
     * This function will write out the Unicode substitution character to the
     * target character buffer.
     * Sub classes to override this method if required
     * @param decoder
     * @param source
     * @param target
     * @param offsets
     * @return A CoderResult object that contains the error result when an error occurs.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
     CoderResult cbToUWriteSub(CharsetDecoderICU decoder, 
                                        ByteBuffer source, CharBuffer target, 
                                        IntBuffer offsets){
        String sub = decoder.replacement();
        CharsetICU cs = (CharsetICU) decoder.charset();
        if (decoder.invalidCharLength==1 && cs.subChar1 != 0x00) {
            char[] subArr = new char[] { 0x1a };
            return CharsetDecoderICU.toUWriteUChars(decoder, subArr, 0, sub
                    .length(), target, offsets, source.position());
        } else {
            return CharsetDecoderICU.toUWriteUChars(decoder, sub.toCharArray(),
                    0, sub.length(), target, offsets, source.position());
            
        }
    }
}
