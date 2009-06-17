/*
 *******************************************************************************
 * Copyright (C) 2008-2009, International Business Machines Corporation and         *
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


import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.lang.UCharacter;

/**
 * @author krajwade
 *
 */
class CharsetBOCU1 extends CharsetICU {   
    /* BOCU constants and macros */
    
    /* initial value for "prev": middle of the ASCII range */
    private static final byte BOCU1_ASCII_PREV = 0x40;
    
    /* bounding byte values for differences */
    private static final int BOCU1_MIN = 0x21;
    private static final int BOCU1_MIDDLE = 0x90;
    //private static final int BOCU1_MAX_LEAD = 0xfe;
    private static final int BOCU1_MAX_TRAIL = 0xff;
    private static final int BOCU1_RESET = 0xff;

    /* number of lead bytes */
    //private static final int BOCU1_COUNT = (BOCU1_MAX_LEAD-BOCU1_MIN+1);

    /* adjust trail byte counts for the use of some C0 control byte values */
    private static final int BOCU1_TRAIL_CONTROLS_COUNT =  20;
    private static final int BOCU1_TRAIL_BYTE_OFFSET = (BOCU1_MIN-BOCU1_TRAIL_CONTROLS_COUNT);

    /* number of trail bytes */
    private static final int BOCU1_TRAIL_COUNT =((BOCU1_MAX_TRAIL-BOCU1_MIN+1)+BOCU1_TRAIL_CONTROLS_COUNT);
    
    /*
     * number of positive and negative single-byte codes
     * (counting 0==BOCU1_MIDDLE among the positive ones)
     */
    private static final int BOCU1_SINGLE = 64;

    /* number of lead bytes for positive and negative 2/3/4-byte sequences */
    private static final int BOCU1_LEAD_2 = 43;
    private static final int BOCU1_LEAD_3 = 3;
    //private static final int BOCU1_LEAD_4 = 1;

    /* The difference value range for single-byters. */
    private static final int BOCU1_REACH_POS_1 = (BOCU1_SINGLE-1);
    private static final int BOCU1_REACH_NEG_1 = (-BOCU1_SINGLE);

    /* The difference value range for double-byters. */
    private static final int BOCU1_REACH_POS_2 = (BOCU1_REACH_POS_1+BOCU1_LEAD_2*BOCU1_TRAIL_COUNT);
    private static final int BOCU1_REACH_NEG_2 = (BOCU1_REACH_NEG_1-BOCU1_LEAD_2*BOCU1_TRAIL_COUNT);

    /* The difference value range for 3-byters. */
    private static final int BOCU1_REACH_POS_3  =
        (BOCU1_REACH_POS_2+BOCU1_LEAD_3*BOCU1_TRAIL_COUNT*BOCU1_TRAIL_COUNT);

    private static final int BOCU1_REACH_NEG_3 = (BOCU1_REACH_NEG_2-BOCU1_LEAD_3*BOCU1_TRAIL_COUNT*BOCU1_TRAIL_COUNT);

    /* The lead byte start values. */
    private static final int BOCU1_START_POS_2 =  (BOCU1_MIDDLE+BOCU1_REACH_POS_1+1);
    private static final int BOCU1_START_POS_3  = (BOCU1_START_POS_2+BOCU1_LEAD_2);
    private static final int BOCU1_START_POS_4  = (BOCU1_START_POS_3+BOCU1_LEAD_3);
         /* ==BOCU1_MAX_LEAD */

    private static final int BOCU1_START_NEG_2 = (BOCU1_MIDDLE+BOCU1_REACH_NEG_1);
    private static final int BOCU1_START_NEG_3 = (BOCU1_START_NEG_2-BOCU1_LEAD_2);
    //private static final int BOCU1_START_NEG_4 = (BOCU1_START_NEG_3-BOCU1_LEAD_3);
         /* ==BOCU1_MIN+1 */

    /* The length of a byte sequence, according to the lead byte (!=BOCU1_RESET). */
   /* private static int BOCU1_LENGTH_FROM_LEAD(int lead) {
       return ((BOCU1_START_NEG_2<=(lead) && (lead)<BOCU1_START_POS_2) ? 1 : 
         (BOCU1_START_NEG_3<=(lead) && (lead)<BOCU1_START_POS_3) ? 2 : 
         (BOCU1_START_NEG_4<=(lead) && (lead)<BOCU1_START_POS_4) ? 3 : 4);
    }*/

    /* The length of a byte sequence, according to its packed form. */
    private static int BOCU1_LENGTH_FROM_PACKED(int packed) {
        return (((packed)&UConverterConstants.UNSIGNED_INT_MASK)<0x04000000 ? (packed)>>24 : 4);
    }
    
    /*
     * Byte value map for control codes,
     * from external byte values 0x00..0x20
     * to trail byte values 0..19 (0..0x13) as used in the difference calculation.
     * External byte values that are illegal as trail bytes are mapped to -1.
     */
    private static final int[]
    bocu1ByteToTrail={
    /*  0     1     2     3     4     5     6     7    */
        -1,   0x00, 0x01, 0x02, 0x03, 0x04, 0x05, -1,

    /*  8     9     a     b     c     d     e     f    */
        -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,

    /*  10    11    12    13    14    15    16    17   */
        0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d,

    /*  18    19    1a    1b    1c    1d    1e    1f   */
        0x0e, 0x0f, -1,   -1,   0x10, 0x11, 0x12, 0x13,

    /*  20   */
        -1
    };

    /*
     * Byte value map for control codes,
     * from trail byte values 0..19 (0..0x13) as used in the difference calculation
     * to external byte values 0x00..0x20.
     */
    private static final int[] 
    bocu1TrailToByte = {
    /*  0     1     2     3     4     5     6     7    */
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x10, 0x11,

    /*  8     9     a     b     c     d     e     f    */
        0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,

    /*  10    11    12    13   */
        0x1c, 0x1d, 0x1e, 0x1f
    };
    
    
    /*
     * 12 commonly used C0 control codes (and space) are only used to encode
     * themselves directly,
     * which makes BOCU-1 MIME-usable and reasonably safe for
     * ASCII-oriented software.
     *
     * These controls are
     *  0   NUL
     *
     *  7   BEL
     *  8   BS
     *
     *  9   TAB
     *  a   LF
     *  b   VT
     *  c   FF
     *  d   CR
     *
     *  e   SO
     *  f   SI
     *
     * 1a   SUB
     * 1b   ESC
     *
     * The other 20 C0 controls are also encoded directly (to preserve order)
     * but are also used as trail bytes in difference encoding
     * (for better compression).
     */
    private static int BOCU1_TRAIL_TO_BYTE(int trail) {
        return ((trail)>=BOCU1_TRAIL_CONTROLS_COUNT ? (trail)+BOCU1_TRAIL_BYTE_OFFSET : bocu1TrailToByte[trail]);
    }    
    
    /* BOCU-1 implementation functions ------------------------------------------ */
    private static int BOCU1_SIMPLE_PREV(int c){
        return (((c)&~0x7f)+BOCU1_ASCII_PREV);
    }

    /**
     * Compute the next "previous" value for differencing
     * from the current code point.
     *
     * @param c current code point, 0x3040..0xd7a3 (rest handled by macro below)
     * @return "previous code point" state value
     */
    private static  int bocu1Prev(int c) {
        /* compute new prev */
        if(/* 0x3040<=c && */ c<=0x309f) {
            /* Hiragana is not 128-aligned */
            return 0x3070;
        } else if(0x4e00<=c && c<=0x9fa5) {
            /* CJK Unihan */
            return 0x4e00-BOCU1_REACH_NEG_2;
        } else if(0xac00<=c /* && c<=0xd7a3 */) {
            /* Korean Hangul */
            return (0xd7a3+0xac00)/2;
        } else {
            /* mostly small scripts */
            return BOCU1_SIMPLE_PREV(c);
        }
    }

    /** Fast version of bocu1Prev() for most scripts. */
    private static int BOCU1_PREV(int c) {
        return ((c)<0x3040 || (c)>0xd7a3 ? BOCU1_SIMPLE_PREV(c) : bocu1Prev(c));
    }
    
    protected byte[] fromUSubstitution = new byte[]{(byte)0x1A};

    /* Faster versions of packDiff() for single-byte-encoded diff values. */

    /** Is a diff value encodable in a single byte? */
    private static boolean DIFF_IS_SINGLE(int diff){
        return (BOCU1_REACH_NEG_1<=(diff) && (diff)<=BOCU1_REACH_POS_1);
    }

    /** Encode a diff value in a single byte. */
    private static int PACK_SINGLE_DIFF(int diff){
        return (BOCU1_MIDDLE+(diff));
    }

    /** Is a diff value encodable in two bytes? */
    private static boolean DIFF_IS_DOUBLE(int diff){
        return (BOCU1_REACH_NEG_2<=(diff) && (diff)<=BOCU1_REACH_POS_2);
    }   
      
    public CharsetBOCU1(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 4; 
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
     }
    
    class CharsetEncoderBOCU extends CharsetEncoderICU {
        public CharsetEncoderBOCU(CharsetICU cs) {
            super(cs,fromUSubstitution);
        }
        
        int sourceIndex, nextSourceIndex;
        int prev, c , diff;
        boolean checkNegative;
        boolean LoopAfterTrail;
        int targetCapacity;
        CoderResult cr;        
        
        /* label values for supporting behavior similar to goto in C */
        private static final int fastSingle=0;
        private static final int getTrail=1;
        private static final int regularLoop=2;
        
        private boolean LabelLoop; //used to break the while loop
        private int labelType = fastSingle; //labeType is set to fastSingle to start the code from fastSingle:
        
        /**
         * Integer division and modulo with negative numerators
         * yields negative modulo results and quotients that are one more than
         * what we need here.
         * This macro adjust the results so that the modulo-value m is always >=0.
         *
         * For positive n, the if() condition is always FALSE.
         *
         * @param n Number to be split into quotient and rest.
         *          Will be modified to contain the quotient.
         * @param d Divisor.
         * @param m Output variable for the rest (modulo result).
         */
        private int NEGDIVMOD(int n, int d, int m) {
            diff = n;
            (m)=(diff)%(d); 
            (diff)/=(d); 
            if((m)<0) { 
                --(diff);
                (m)+=(d);
            }
            return m;
        }
        
        /**
         * Encode a difference -0x10ffff..0x10ffff in 1..4 bytes
         * and return a packed integer with them.
         *
         * The encoding favors small absolute differences with short encodings
         * to compress runs of same-script characters.
         *
         * Optimized version with unrolled loops and fewer floating-point operations
         * than the standard packDiff().
         *
         * @param diff difference value -0x10ffff..0x10ffff
         * @return
         *      0x010000zz for 1-byte sequence zz
         *      0x0200yyzz for 2-byte sequence yy zz
         *      0x03xxyyzz for 3-byte sequence xx yy zz
         *      0xwwxxyyzz for 4-byte sequence ww xx yy zz (ww>0x03)
         */
        private int packDiff(int n) {
            int result, m = 0;
            diff = n;

            if(diff>=BOCU1_REACH_NEG_1) {
                /* mostly positive differences, and single-byte negative ones */
                if(diff<=BOCU1_REACH_POS_2) {
                    /* two bytes */
                    diff-=BOCU1_REACH_POS_1+1;
                    result=0x02000000;

                    m=diff%BOCU1_TRAIL_COUNT;
                    diff/=BOCU1_TRAIL_COUNT;
                    result|=BOCU1_TRAIL_TO_BYTE(m);

                    result|=(BOCU1_START_POS_2+diff)<<8;
                } else if(diff<=BOCU1_REACH_POS_3) {
                    /* three bytes */
                    diff-=BOCU1_REACH_POS_2+1;
                    result=0x03000000;

                    m=diff%BOCU1_TRAIL_COUNT;
                    diff/=BOCU1_TRAIL_COUNT;
                    result|=BOCU1_TRAIL_TO_BYTE(m);

                    m=diff%BOCU1_TRAIL_COUNT;
                    diff/=BOCU1_TRAIL_COUNT;
                    result|=BOCU1_TRAIL_TO_BYTE(m)<<8;

                    result|=(BOCU1_START_POS_3+diff)<<16;
                } else {
                    /* four bytes */
                    diff-=BOCU1_REACH_POS_3+1;

                    m=diff%BOCU1_TRAIL_COUNT;
                    diff/=BOCU1_TRAIL_COUNT;
                    result=BOCU1_TRAIL_TO_BYTE(m);

                    m=diff%BOCU1_TRAIL_COUNT;
                    diff/=BOCU1_TRAIL_COUNT;
                    result|=BOCU1_TRAIL_TO_BYTE(m)<<8;

                    /*
                     * We know that / and % would deliver quotient 0 and rest=diff.
                     * Avoid division and modulo for performance.
                     */
                    result|=BOCU1_TRAIL_TO_BYTE(diff)<<16;

                    result|=((BOCU1_START_POS_4&UConverterConstants.UNSIGNED_INT_MASK))<<24;
                }
            } else {
                /* two- to four-byte negative differences */
                if(diff>=BOCU1_REACH_NEG_2) {
                    /* two bytes */
                    diff-=BOCU1_REACH_NEG_1;
                    result=0x02000000;

                    m = NEGDIVMOD(diff, BOCU1_TRAIL_COUNT, m);
                    result|=BOCU1_TRAIL_TO_BYTE(m);

                    result|=(BOCU1_START_NEG_2+diff)<<8;
                } else if(diff>=BOCU1_REACH_NEG_3) {
                    /* three bytes */
                    diff-=BOCU1_REACH_NEG_2;
                    result=0x03000000;

                    m = NEGDIVMOD(diff, BOCU1_TRAIL_COUNT, m);
                    result|=BOCU1_TRAIL_TO_BYTE(m);

                    m = NEGDIVMOD(diff, BOCU1_TRAIL_COUNT, m);
                    result|=BOCU1_TRAIL_TO_BYTE(m)<<8;

                    result|=(BOCU1_START_NEG_3+diff)<<16;
                } else {
                    /* four bytes */
                    diff-=BOCU1_REACH_NEG_3;

                    m = NEGDIVMOD(diff, BOCU1_TRAIL_COUNT, m);
                    result=BOCU1_TRAIL_TO_BYTE(m);

                    m = NEGDIVMOD(diff, BOCU1_TRAIL_COUNT, m);
                    result|=BOCU1_TRAIL_TO_BYTE(m)<<8;

                    /*
                     * We know that NEGDIVMOD would deliver
                     * quotient -1 and rest=diff+BOCU1_TRAIL_COUNT.
                     * Avoid division and modulo for performance.
                     */
                    m=diff+BOCU1_TRAIL_COUNT;
                    result|=BOCU1_TRAIL_TO_BYTE(m)<<16;

                    result|=BOCU1_MIN<<24;
                }
            }
            return result;
        }
           
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush){
            cr = CoderResult.UNDERFLOW;
            
            LabelLoop = true; //used to break the while loop
            checkNegative = false; // its value is set to true to get out of while loop when c = -c
            LoopAfterTrail = false; // its value is set to true to ignore code before getTrail:
            
            /*set up the local pointers*/
            targetCapacity = target.limit() - target.position();
            c = fromUChar32;
            prev = fromUnicodeStatus;
            
            if(prev==0){
                prev = BOCU1_ASCII_PREV;
            }
            
            /*sourceIndex ==-1 if the current characte began in the previous buffer*/
            sourceIndex = c == 0 ? 0: -1;
            nextSourceIndex = 0;
            
            /*conversion loop*/
            if(c!=0 && targetCapacity>0){
                labelType = getTrail;
            }
            
            while(LabelLoop){
                switch(labelType){
                    case fastSingle:
                        labelType = fastSingle(source, target, offsets);
                        break;
                    case getTrail:
                        labelType = getTrail(source, target, offsets);
                        break;
                    case regularLoop:
                        labelType = regularLoop(source, target, offsets);
                        break;
                }
            }
                    
            return cr;
        }
        
        private int fastSingle(CharBuffer source, ByteBuffer target, IntBuffer offsets){                     
//fastSingle:        
            /*fast loop for single-byte differences*/
            /*use only one loop counter variable , targetCapacity, not also source*/
            diff = source.limit() - source.position();
            if(targetCapacity>diff){
                targetCapacity = diff;
            }
            while(targetCapacity>0 && (c=source.get(source.position()))<0x3000){
                if(c<=0x20){
                    if(c!=0x20){
                        prev = BOCU1_ASCII_PREV;
                    }
                    target.put((byte)c);
                    if(offsets!=null){
                        offsets.put(nextSourceIndex++);
                    }
                    source.position(source.position()+1);
                    --targetCapacity;
                }else {
                    diff = c-prev;
                    if(DIFF_IS_SINGLE(diff)){
                        prev = BOCU1_SIMPLE_PREV(c);
                        target.put((byte)PACK_SINGLE_DIFF(diff));
                        if(offsets!=null){
                            offsets.put(nextSourceIndex++);
                        }
                        source.position(source.position()+1);
                        --targetCapacity;
                    }else {
                        break;
                    }
                }
            }
            return regularLoop;
        }
        
        private int getTrail(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            if(source.hasRemaining()){
                /*test the following code unit*/
                char trail = source.get(source.position());
                if(UTF16.isTrailSurrogate(trail)){
                    source.position(source.position()+1);
                    ++nextSourceIndex;
                    c=UCharacter.getCodePoint((char)c, trail);
                }
            } else {
                /*no more input*/
                c = -c; /*negative lead surrogate as "incomplete" indicator to avoid c=0 everywhere else*/
                checkNegative = true;
            }
            LoopAfterTrail = true;
            return regularLoop;
        }
        
        private int regularLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            if(!LoopAfterTrail){
                /*restore real values*/
                targetCapacity = target.limit()-target.position();
                sourceIndex = nextSourceIndex; /*wrong if offsets==null but does not matter*/
            }
            /*regular loop for all classes*/
            while(LoopAfterTrail || source.hasRemaining()){
                if(LoopAfterTrail || targetCapacity>0){
                    
                    if(!LoopAfterTrail){
                        c = source.get();
                        ++nextSourceIndex;
                        
                        if(c<=0x20){
                            /*
                             * ISO C0 control & space:
                             * Encode directly for MIME compatibility,
                             * and reset state except for space, to not disrupt compression.
                             */
                            if(c!=0x20) {
                                prev=BOCU1_ASCII_PREV;
                            }
                            target.put((byte)c);
                            if(offsets != null){
                                offsets.put(sourceIndex++);
                            }
                            --targetCapacity;
                         
                            sourceIndex=nextSourceIndex;
                            continue;
                        }
                        
                        if(UTF16.isLeadSurrogate((char)c)){
                            getTrail(source, target, offsets);
                            if(checkNegative){
                                break;
                            }
                        }
                    }
                        
                    if(LoopAfterTrail){
                        LoopAfterTrail = false; 
                    }
                    
                    /*
                     * all other Unicode code points c==U+0021..U+10ffff
                     * are encoded with the difference c-prev
                     *
                     * a new prev is computed from c,
                     * placed in the middle of a 0x80-block (for most small scripts) or
                     * in the middle of the Unihan and Hangul blocks
                     * to statistically minimize the following difference
                     */
                    diff = c- prev;
                    prev = BOCU1_PREV(c);
                    if(DIFF_IS_SINGLE(diff)){
                        target.put((byte)PACK_SINGLE_DIFF(diff));
                        if(offsets!=null){
                            offsets.put(sourceIndex++);
                        }
                        --targetCapacity;
                        sourceIndex=nextSourceIndex;
                        if(c<0x3000){
                            labelType = fastSingle;
                            return labelType;
                        }
                    } else if(DIFF_IS_DOUBLE(diff) && 2<=targetCapacity){
                        /*optimize 2 byte case*/
                        int m = 0;
                        if(diff>=0){
                            diff -= BOCU1_REACH_POS_1 +1;
                            m = diff%BOCU1_TRAIL_COUNT;
                            diff/=BOCU1_TRAIL_COUNT;
                            diff+=BOCU1_START_POS_2;
                        } else {
                            diff -= BOCU1_REACH_NEG_1;
                            m = NEGDIVMOD(diff, BOCU1_TRAIL_COUNT, m);
                            diff+=BOCU1_START_NEG_2;
                        }
                        target.put((byte)diff);
                        target.put((byte)BOCU1_TRAIL_TO_BYTE(m));
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                            offsets.put(sourceIndex);
                        }
                        targetCapacity -= 2;
                        sourceIndex = nextSourceIndex;
                    } else {
                        int length; /*will be 2..4*/
                        diff = packDiff(diff);
                        length = BOCU1_LENGTH_FROM_PACKED(diff);
                        
                        /*write the output character bytes from diff and length*/
                        /*from the first if in the loop we know that targetCapacity>0*/
                        if(length<=targetCapacity){
                            switch(length){
                                /*each branch falls through the next one*/
                                case 4:
                                    target.put((byte)(diff>>24));
                                    if(offsets!= null){
                                        offsets.put(sourceIndex);
                                    }
                                case 3:
                                    target.put((byte)(diff>>16));
                                    if(offsets!= null){
                                        offsets.put(sourceIndex);
                                    }
                                case 2:
                                    target.put((byte)(diff>>8));
                                    if(offsets!= null){
                                        offsets.put(sourceIndex);
                                    }
                                    /*case 1 handled above*/
                                    target.put((byte)diff);
                                    if(offsets!= null){
                                        offsets.put(sourceIndex);
                                    }
                                default:
                                    /*will never occur*/
                                    break;
                            }
                            targetCapacity -= length;
                            sourceIndex = nextSourceIndex;
                        } else {
                            ByteBuffer error = ByteBuffer.wrap(errorBuffer);
                            /*
                             * We actually do this backwards here:
                             * In order to save an intermediate variable, we output
                             * first to the overflow buffer what does not fit into the
                             * regular target.
                             */
                            /* we know that 1<=targetCapacity<length<=4 */
                            length-=targetCapacity;
                            switch(length) {
                                /* each branch falls through to the next one */
                            case 3:
                                error.put((byte)(diff>>16));
                            case 2:
                                error.put((byte)(diff>>8));
                            case 1:
                                error.put((byte)diff);
                            default:
                                /* will never occur */
                                break;
                            }
                            errorBufferLength = length;
                            
                            /* now output what fits into the regular target */
                            diff>>=8*length; /* length was reduced by targetCapacity */
                            switch(targetCapacity) {
                                /* each branch falls through to the next one */
                            case 3:
                                target.put((byte)(diff>>16));
                                if(offsets!= null){
                                    offsets.put(sourceIndex);
                                }
                            case 2:
                                target.put((byte)(diff>>8));
                                if(offsets!= null){
                                    offsets.put(sourceIndex);
                                }
                            case 1:
                                target.put((byte)diff);
                                if(offsets!= null){
                                    offsets.put(sourceIndex);
                                }
                            default:
                                /* will never occur */
                                break;
                            }

                            /* target overflow */
                            targetCapacity=0;
                            cr = CoderResult.OVERFLOW;
                            break;
                        }
                    }
                } else{
                    /*target is full*/
                    cr = CoderResult.OVERFLOW;
                    break;
                }
                   
            }
            /*set the converter state back into UConverter*/
            fromUChar32 = c<0 ? -c :0;
            fromUnicodeStatus = prev;
            LabelLoop = false;
            labelType = fastSingle;
            return labelType;
        }
       
    }
    
    class CharsetDecoderBOCU extends CharsetDecoderICU{
        public CharsetDecoderBOCU(CharsetICU cs) {
            super(cs);
        }
        
        int byteIndex;
        int sourceIndex, nextSourceIndex;
        int prev, c , diff, count;
        byte[] bytes;
        int targetCapacity;
        CoderResult cr;
        
        /* label values for supporting behavior similar to goto in C */
        private static final int fastSingle=0;
        private static final int getTrail=1;
        private static final int regularLoop=2;
        private static final int endLoop=3;
        
        private boolean LabelLoop;//used to break the while loop
        private boolean afterTrail; // its value is set to true to ignore code after getTrail:
        private int labelType;
        /*
         * The BOCU-1 converter uses the standard setup code in ucnv.c/ucnv_bld.c.
         * The UConverter fields are used as follows:
         *
         * fromUnicodeStatus    encoder's prev (0 will be interpreted as BOCU1_ASCII_PREV)
         *
         * toUnicodeStatus      decoder's prev (0 will be interpreted as BOCU1_ASCII_PREV)
         * mode                 decoder's incomplete (diff<<2)|count (ignored when toULength==0)
         */

        /* BOCU-1-from-Unicode conversion functions --------------------------------- */

        
        
        /**
         * Function for BOCU-1 decoder; handles multi-byte lead bytes.
         *
         * @param b lead byte;
         *          BOCU1_MIN<=b<BOCU1_START_NEG_2 or BOCU1_START_POS_2<=b<BOCU1_MAX_LEAD
         * @return (diff<<2)|count
         */
        private int decodeBocu1LeadByte(int b) {
            int diffValue, countValue;

            if(b >= BOCU1_START_NEG_2) {
                /* positive difference */
                if(b < BOCU1_START_POS_3) {
                    /* two bytes */
                    diffValue = (b - BOCU1_START_POS_2)*BOCU1_TRAIL_COUNT + BOCU1_REACH_POS_1+1;
                    countValue = 1;
                } else if(b < BOCU1_START_POS_4) {
                    /* three bytes */
                    diffValue = (b-BOCU1_START_POS_3)*BOCU1_TRAIL_COUNT*BOCU1_TRAIL_COUNT+BOCU1_REACH_POS_2+1;
                    countValue = 2;
                } else {
                    /* four bytes */
                    diffValue = BOCU1_REACH_POS_3+1;
                    countValue = 3;
                }
            } else {
                /* negative difference */
                if(b >= BOCU1_START_NEG_3) {
                    /* two bytes */
                    diffValue=(b -BOCU1_START_NEG_2)*BOCU1_TRAIL_COUNT + BOCU1_REACH_NEG_1;
                    countValue=1;
                } else if(b>BOCU1_MIN) {
                    /* three bytes */
                    diffValue=(b - BOCU1_START_NEG_3)*BOCU1_TRAIL_COUNT*BOCU1_TRAIL_COUNT + BOCU1_REACH_NEG_2;
                    countValue = 2;
                } else {
                    /* four bytes */
                    diffValue=-BOCU1_TRAIL_COUNT*BOCU1_TRAIL_COUNT*BOCU1_TRAIL_COUNT+BOCU1_REACH_NEG_3;
                    countValue=3;
                }
            }

            /* return the state for decoding the trail byte(s) */
            return (diffValue<<2)|countValue;
        }
        
        /**
         * Function for BOCU-1 decoder; handles multi-byte trail bytes.
         *
         * @param count number of remaining trail bytes including this one
         * @param b trail byte
         * @return new delta for diff including b - <0 indicates an error
         *
         * @see decodeBocu1
         */
        private int decodeBocu1TrailByte(int countValue, int b) {
            b = b&UConverterConstants.UNSIGNED_BYTE_MASK;
            if((b)<=0x20) {
                /* skip some C0 controls and make the trail byte range contiguous */
                b = bocu1ByteToTrail[b];
                /* b<0 for an illegal trail byte value will result in return<0 below */
            } else {
                //b-= BOCU1_TRAIL_BYTE_OFFSET;
                b = b - BOCU1_TRAIL_BYTE_OFFSET;
            }

            /* add trail byte into difference and decrement count */
            if(countValue==1) {
                return b;
            } else if(countValue==2) {
                return b*BOCU1_TRAIL_COUNT;
            } else /* count==3 */ {
                return b*(BOCU1_TRAIL_COUNT*BOCU1_TRAIL_COUNT);
            }
        }
        
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets,
                boolean flush){
            cr = CoderResult.UNDERFLOW;
            
            LabelLoop = true; 
            afterTrail = false; 
            labelType = fastSingle; // labelType is set to fastSingle so t
            
            /*get the converter state*/
            prev = toUnicodeStatus;
            
            if(prev==0){
                prev = BOCU1_ASCII_PREV;
            }
            diff = mode;
            count = diff&3;
            diff>>=2;
            
            byteIndex = toULength;
            bytes = toUBytesArray;
            
            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex=byteIndex==0 ? 0 : -1;
            nextSourceIndex=0;
            
            /* conversion "loop" similar to _SCSUToUnicodeWithOffsets() */
            if(count>0 && byteIndex>0 && target.position()<target.limit()) {
                labelType = getTrail;
            }
            
            while(LabelLoop){
                switch(labelType){
                    case fastSingle:
                        labelType = fastSingle(source, target, offsets);
                        break;
                    case getTrail:
                        labelType = getTrail(source, target, offsets);
                        break;
                    case regularLoop:
                        labelType = afterGetTrail(source, target, offsets);
                        break;
                    case endLoop:
                        endLoop(source, target, offsets);
                        break;
                }
            }
            
            return cr;
        }
        
        private int fastSingle(ByteBuffer source, CharBuffer target, IntBuffer offsets){
            labelType = regularLoop;
            /* fast loop for single-byte differences */
            /* use count as the only loop counter variable */
            diff = source.limit() - source.position();
            count = target.limit()-target.position();
            if(count>diff) {
                count = diff;
            }
            while(count>0) {
                if(BOCU1_START_NEG_2 <=(c=source.get(source.position())&UConverterConstants.UNSIGNED_BYTE_MASK) && c< BOCU1_START_POS_2) {
                    c = prev + (c-BOCU1_MIDDLE);
                    if(c<0x3000) {
                        target.put((char)c);
                        if(offsets!=null){
                            offsets.put(nextSourceIndex++);
                        } 
                        prev = BOCU1_SIMPLE_PREV(c);
                    } else {
                        break;
                    }
                } else if((c&UConverterConstants.UNSIGNED_BYTE_MASK) <= 0x20) {
                    if((c&UConverterConstants.UNSIGNED_BYTE_MASK) != 0x20) {
                        prev = BOCU1_ASCII_PREV;
                    }
                    target.put((char)c);
                    if(offsets!=null){
                        offsets.put(nextSourceIndex++);
                    } 
                } else {
                    break;
                }
                source.position(source.position()+1);
                --count;
            }
            sourceIndex=nextSourceIndex; /* wrong if offsets==NULL but does not matter */
            return labelType;
        }
        
        private int getTrail(ByteBuffer source, CharBuffer target, IntBuffer offsets){
            labelType = regularLoop;
            for(;;) {
                if(source.position() >= source.limit()) {
                    labelType = endLoop;
                    return labelType;
                }
                ++nextSourceIndex;
                c = bytes[byteIndex++] = source.get();

                /* trail byte in any position */
                c = decodeBocu1TrailByte(count, c);
                if(c<0) {
                    cr = CoderResult.malformedForLength(1);
                    labelType = endLoop;
                    return labelType;
                }

                diff+=c;
                if(--count==0) {
                    /* final trail byte, deliver a code point */
                    byteIndex=0;
                    c = prev + diff;
                    if(c > 0x10ffff) {
                        cr = CoderResult.malformedForLength(1);
                        labelType = endLoop;
                        return labelType;
                    }
                    break;
                }
            }
            afterTrail = true;
            return labelType;
            
        }
        
        private int afterGetTrail(ByteBuffer source, CharBuffer target, IntBuffer offsets){
            /* decode a sequence of single and lead bytes */
            while(afterTrail || source.hasRemaining()) {
                if(!afterTrail){
                    if(target.position() >= target.limit()) {
                        /* target is full */
                        cr = CoderResult.OVERFLOW;
                        break;
                    }

                    ++nextSourceIndex;
                    c = source.get()&UConverterConstants.UNSIGNED_BYTE_MASK;
                    if(BOCU1_START_NEG_2 <= c && c < BOCU1_START_POS_2) {
                        /* Write a code point directly from a single-byte difference. */
                        c = prev + (c-BOCU1_MIDDLE);
                        if(c<0x3000) {
                            target.put((char)c);
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                            prev = BOCU1_SIMPLE_PREV(c);
                            sourceIndex = nextSourceIndex;
                            labelType = fastSingle;
                            return labelType;
                        }
                    } else if(c <= 0x20) {
                        /*
                         * Direct-encoded C0 control code or space.
                         * Reset prev for C0 control codes but not for space.
                         */
                        if(c != 0x20) {
                            prev=BOCU1_ASCII_PREV;
                        }
                        target.put((char)c);
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                        }
                        sourceIndex=nextSourceIndex;
                        continue;
                    } else if(BOCU1_START_NEG_3 <= c && c < BOCU1_START_POS_3 && source.hasRemaining()) {
                        /* Optimize two-byte case. */
                        if(c >= BOCU1_MIDDLE) {
                            diff=(c - BOCU1_START_POS_2)*BOCU1_TRAIL_COUNT + BOCU1_REACH_POS_1 + 1;
                        } else {
                            diff=(c-BOCU1_START_NEG_2)*BOCU1_TRAIL_COUNT + BOCU1_REACH_NEG_1;
                        }

                        /* trail byte */
                        ++nextSourceIndex;
                        c = decodeBocu1TrailByte(1, source.get());
                        if(c<0 || ((c = prev + diff + c)&UConverterConstants.UNSIGNED_INT_MASK)>0x10ffff) {
                            bytes[0]= source.get(source.position()-2);
                            bytes[1]= source.get(source.position()-1);
                            byteIndex = 2;
                            cr = CoderResult.malformedForLength(2);
                            break;
                        }
                    } else if(c == BOCU1_RESET) {
                        /* only reset the state, no code point */
                        prev=BOCU1_ASCII_PREV;
                        sourceIndex=nextSourceIndex;
                        continue;
                    } else {
                        /*
                         * For multi-byte difference lead bytes, set the decoder state
                         * with the partial difference value from the lead byte and
                         * with the number of trail bytes.
                         */
                        bytes[0]= (byte)c;
                        byteIndex = 1;

                        diff = decodeBocu1LeadByte(c);
                        count = diff&3;
                        diff>>=2;
                        getTrail(source, target, offsets);
                        if(labelType != regularLoop){
                            return labelType;
                        }
                    }
                }
                
                if(afterTrail){
                    afterTrail = false;
                }
                
                /* calculate the next prev and output c */
                prev = BOCU1_PREV(c);
                if(c<=0xffff) {
                    target.put((char)c);
                    if(offsets!=null){
                        offsets.put(sourceIndex);
                    }
                } else {
                    /* output surrogate pair */
                    target.put((char)UTF16.getLeadSurrogate(c));
                    if(target.hasRemaining()) {
                        target.put((char)UTF16.getTrailSurrogate(c));
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                            offsets.put(sourceIndex);
                        }
                    } else {
                        /* target overflow */
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                        }
                        charErrorBufferArray[0] = UTF16.getTrailSurrogate(c);
                        charErrorBufferLength = 1;
                        cr = CoderResult.OVERFLOW;
                        break;
                }
            }
            sourceIndex=nextSourceIndex;
          }
          labelType = endLoop;
          return labelType;
        }
        
        private void endLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets){
            if(cr.isMalformed()) {
                /* set the converter state in UConverter to deal with the next character */
                toUnicodeStatus = BOCU1_ASCII_PREV;
                mode = 0;
            } else {
                /* set the converter state back into UConverter */
                toUnicodeStatus=prev;
                mode=(diff<<2)|count;
            }
            toULength=byteIndex;
            LabelLoop = false;
        }
    
    }
    
    
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderBOCU(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderBOCU(this);
    }
    
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        CharsetICU.getCompleteUnicodeSet(setFillIn);
    }

}
