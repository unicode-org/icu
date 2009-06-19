/*
 *******************************************************************************
 * Copyright (C) 2008-2009, International Business Machines Corporation and    *
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
class CharsetSCSU extends CharsetICU{
    /* SCSU definitions --------------------------------------------------------- */

    /* SCSU command byte values */
    //enum {
    private static final short SQ0=0x01; /* Quote from window pair 0 */
    private static final short SQ7=0x08; /* Quote from window pair 7 */
    private static final short SDX=0x0B; /* Define a window as extended */
    //private static final short Srs=0x0C; /* reserved */
    private static final short SQU=0x0E; /* Quote a single Unicode character */
    private static final short SCU=0x0F; /* Change to Unicode mode */
    private static final short SC0=0x10; /* Select window 0 */
    private static final short SC7=0x17; /* Select window 7 */
    private static final short SD0=0x18; /* Define and select window 0 */
    //private static final short SD7=0x1F; /* Define and select window 7 */
    
    private static final short UC0=0xE0; /* Select window 0 */
    private static final short UC7=0xE7; /* Select window 7 */
    private static final short UD0=0xE8; /* Define and select window 0 */
    private static final short UD7=0xEF; /* Define and select window 7 */
    private static final short UQU=0xF0; /* Quote a single Unicode character */
    private static final short UDX=0xF1; /* Define a Window as extended */
    private static final short Urs=0xF2;  /* reserved */
   // };
   
  //  enum {
        /*
         * Unicode code points from 3400 to E000 are not adressible by
         * dynamic window, since in these areas no short run alphabets are
         * found. Therefore add gapOffset to all values from gapThreshold.
         */
    private static final int gapThreshold=0x68;
    private static final int gapOffset = 0xAC00 ;
    /* values between reservedStart and fixedThreshold are reserved */
    private static final int reservedStart=0xA8;
    /* use table of predefined fixed offsets for values from fixedThreshold */
    private static final int fixedThreshold=0xF;
    //};
    
    protected byte[] fromUSubstitution = new byte[]{(byte)0x0E,(byte)0xFF, (byte)0xFD};
    
    /* constant offsets for the 8 static windows */
    private static final int staticOffsets[]={
        0x0000, /* ASCII for quoted tags */
        0x0080, /* Latin - 1 Supplement (for access to punctuation) */
        0x0100, /* Latin Extended-A */
        0x0300, /* Combining Diacritical Marks */
        0x2000, /* General Punctuation */
        0x2080, /* Currency Symbols */
        0x2100, /* Letterlike Symbols and Number Forms */
        0x3000  /* CJK Symbols and punctuation */
    };

    /* initial offsets for the 8 dynamic (sliding) windows */
   private static final int initialDynamicOffsets[]={
        0x0080, /* Latin-1 */
        0x00C0, /* Latin Extended A */
        0x0400, /* Cyrillic */
        0x0600, /* Arabic */
        0x0900, /* Devanagari */
        0x3040, /* Hiragana */
        0x30A0, /* Katakana */
        0xFF00  /* Fullwidth ASCII */
    };

    /* Table of fixed predefined Offsets */
    private static final int fixedOffsets[]={
        /* 0xF9 */ 0x00C0, /* Latin-1 Letters + half of Latin Extended A */
        /* 0xFA */ 0x0250, /* IPA extensions */
        /* 0xFB */ 0x0370, /* Greek */
        /* 0xFC */ 0x0530, /* Armenian */
        /* 0xFD */ 0x3040, /* Hiragana */
        /* 0xFE */ 0x30A0, /* Katakana */
        /* 0xFF */ 0xFF60  /* Halfwidth Katakana */
    };

    /* state values */
    //enum {
    private static final int readCommand=0;
    private static final int quotePairOne=1;
    private static final int quotePairTwo=2;
    private static final int quoteOne=3;
    private static final int definePairOne=4;
    private static final int definePairTwo=5;
    private static final int defineOne=6;
  //  };
       
    private final class SCSUData{   
        /* dynamic window offsets, intitialize to default values from initialDynamicOffsets */
        int toUDynamicOffsets[] = new int[8] ;
        int fromUDynamicOffsets[] = new int[8] ; 

        /* state machine state - toUnicode */
        boolean toUIsSingleByteMode;
        short toUState;
        byte toUQuoteWindow, toUDynamicWindow;
        short toUByteOne;
        short toUPadding[];

        /* state machine state - fromUnicode */
        boolean fromUIsSingleByteMode;
        byte fromUDynamicWindow;

        /*
         * windowUse[] keeps track of the use of the dynamic windows:
         * At nextWindowUseIndex there is the least recently used window,
         * and the following windows (in a wrapping manner) are more and more
         * recently used.
         * At nextWindowUseIndex-1 there is the most recently used window.
         */
        byte locale;
        byte nextWindowUseIndex;
        byte windowUse[] = new byte[8];
        
        SCSUData(){
            initialize();
        }
        
        void initialize(){
            for(int i=0;i<8;i++){
                this.toUDynamicOffsets[i] = initialDynamicOffsets[i];
            }
            this.toUIsSingleByteMode = true;
            this.toUState = readCommand;
            this.toUQuoteWindow = 0;
            this.toUDynamicWindow = 0;
            this.toUByteOne = 0;
            this.fromUIsSingleByteMode = true;
            this.fromUDynamicWindow = 0;
            for(int i=0;i<8;i++){
                this.fromUDynamicOffsets[i] = initialDynamicOffsets[i];
            }
            this.nextWindowUseIndex = 0; 
            switch(this.locale){
            /* Note being used right now because "SCSU,locale=ja" does not work in ICU4J. */
            /*    case l_ja:
                    for(int i=0;i<8;i++){
                        this.windowUse[i] = initialWindowUse_ja[i];
                    }
                    break; */
                default:
                    for(int i=0;i<8;i++){
                        this.windowUse[i] = initialWindowUse[i];
                    }
                    
            }
        }
    }
    
    static final byte initialWindowUse[]={ 7, 0, 3, 2, 4, 5, 6, 1 };
    /* Note being used right now because "SCSU,locale=ja" does not work in ICU4J. */
    // static final byte initialWindowUse_ja[]={ 3, 2, 4, 1, 0, 7, 5, 6 };

    //enum {
    //private static final int lGeneric = 0;
    /* Note being used right now because "SCSU,locale=ja" does not work in ICU4J. */
    // private static final int l_ja = 1;
    //};
    
    private SCSUData extraInfo = null; 
    
    public CharsetSCSU(String icuCanonicalName, String javaCanonicalName, String[] aliases){
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 3; 
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
        extraInfo = new SCSUData();
    }
    
    class CharsetDecoderSCSU extends CharsetDecoderICU {       
        /* label values for supporting behavior similar to goto in C */
        private static final int FastSingle=0;
        private static final int SingleByteMode=1;
        private static final int EndLoop=2;
        
        /* Mode Type */
        private static final int ByteMode = 0;
        private static final int UnicodeMode =1;       
        
        public CharsetDecoderSCSU(CharsetICU cs) {
            super(cs);
            implReset();
        }
        
        //private SCSUData data ;
        protected void implReset(){
            super.implReset();
            toULength = 0;
            extraInfo.initialize();
        }
        
        short b;
        
        //Get the state machine state 
        private boolean isSingleByteMode ;
        private short state ;
        private byte quoteWindow ;
        private byte dynamicWindow ;
        private short byteOne;
        
        
        //sourceIndex=-1 if the current character began in the previous buffer
        private int sourceIndex  ;
        private int nextSourceIndex ;
        
        CoderResult cr;
        SCSUData data ;
        private boolean LabelLoop;// used to break the while loop
        
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets,
                boolean flush){
            data = extraInfo;
            
            //Get the state machine state 
            isSingleByteMode = data.toUIsSingleByteMode;
            state = data.toUState;
            quoteWindow = data.toUQuoteWindow;
            dynamicWindow = data.toUDynamicWindow;
            byteOne = data.toUByteOne;
            
            LabelLoop = true;
            
            //sourceIndex=-1 if the current character began in the previous buffer
            sourceIndex = data.toUState == readCommand ? 0: -1 ;
            nextSourceIndex = 0;
            
            cr = CoderResult.UNDERFLOW;
            int labelType = 0;
            while(LabelLoop){
                if(isSingleByteMode){
                    switch(labelType){
                        case FastSingle:
                            /*fast path for single-byte mode*/
                            labelType = fastSingle(source, target, offsets, ByteMode);
                            break;
                        case SingleByteMode:
                            /* normal state machine for single-byte mode, minus handling for what fastSingleCovers */
                            labelType = singleByteMode(source, target, offsets, ByteMode);
                            break;
                        case EndLoop:
                            endLoop(source, target, offsets);
                            break;
                    }
                }else{
                    switch(labelType){
                        case FastSingle:
                            /*fast path for single-byte mode*/
                            labelType = fastSingle(source, target, offsets, UnicodeMode);
                            break;
                        case SingleByteMode:
                            /* normal state machine for single-byte mode, minus handling for what fastSingleCovers */
                            labelType = singleByteMode(source, target, offsets, UnicodeMode);
                            break;
                        case EndLoop:
                            endLoop(source, target, offsets);
                            break;
                    }
                    //LabelLoop = false;
                }
            }
            return cr;
        }
        
        private int fastSingle(ByteBuffer source, CharBuffer target, IntBuffer offsets, int modeType){
            int label = 0;
            if(modeType==ByteMode){
                
                if(state==readCommand){
                    while(source.hasRemaining() && target.hasRemaining() && (b=(short)(source.get(source.position()) & UConverterConstants.UNSIGNED_BYTE_MASK)) >= 0x20){
                        source.position(source.position()+1);
                        ++nextSourceIndex;
                        if(b <= 0x7f){
                            /*Write US graphic character or DEL*/
                            target.put((char)b);
                            if(offsets != null){
                                offsets.put(sourceIndex);
                            }
                        }else{
                            /*Write from dynamic window*/
                            int c = data.toUDynamicOffsets[dynamicWindow] + (b&0x7f);
                            if(c <= 0xffff){
                                target.put((char)c);
                                if(offsets != null){
                                    offsets.put(sourceIndex);
                                }
                            }else{
                                /*Output surrogate pair */
                                target.put((char)(0xd7c0 + (c>>10)));
                                if(target.hasRemaining()){
                                    target.put((char)(0xdc00 | (c&0x3ff)));
                                    if(offsets != null){
                                        offsets.put(sourceIndex);
                                        offsets.put(sourceIndex);
                                    }
                                }else{
                                    /* target overflow */
                                    if(offsets != null){
                                        offsets.put(sourceIndex);
                                    }
                                    charErrorBufferArray[0] = (char)(0xdc00 | (c&0x3ff));
                                    charErrorBufferLength = 1;
                                    label = EndLoop;
                                    cr = CoderResult.OVERFLOW;
                                    LabelLoop = false;
                                    return label;
                                }
                            }
                        }
                        sourceIndex = nextSourceIndex;
                    }
                   // label = SingleByteMode;
                }
            }else if(modeType==UnicodeMode){
                /* fast path for unicode mode */
                if(state == readCommand){
                    while((source.position()+1)<source.limit() && target.hasRemaining() && (((b=source.get(source.position()))-UC0)&UConverterConstants.UNSIGNED_BYTE_MASK)>(Urs-UC0)){
                        target.put((char)((b<<8)|(source.get(source.position()+1)&UConverterConstants.UNSIGNED_BYTE_MASK)));
                        if(offsets != null){
                            offsets.put(sourceIndex);
                        }
                        sourceIndex = nextSourceIndex;
                        nextSourceIndex+=2;
                        source.position(source.position()+2);
                    }
                }
            }
            label = SingleByteMode;
            return label;
        }
        
        private int singleByteMode(ByteBuffer source, CharBuffer target, IntBuffer offsets, int modeType){
            int label = SingleByteMode;
            if(modeType == ByteMode){
                while(source.hasRemaining()){
                    if(!target.hasRemaining()){
                        cr = CoderResult.OVERFLOW;
                        LabelLoop = false;
                        return label;
                     }
                    b = (short)(source.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
                    ++nextSourceIndex;
                    switch(state){
                    case readCommand:
                        /*redundant conditions are commented out */
                        if(((1L<<b)&0x2601)!=0){
                            target.put((char)b);
                            if(offsets != null){
                                offsets.put(sourceIndex);
                            }
                            sourceIndex = nextSourceIndex;
                            label = FastSingle;
                            return label;
                        }else if(SC0 <= b){
                            if(b<=SC7){
                                dynamicWindow = (byte)(b-SC0);
                                sourceIndex = nextSourceIndex;
                                label = FastSingle;
                                return label;
                            }else /* if(SD0<=b && b<=SQ7)*/{
                                dynamicWindow = (byte)(b - SD0);
                                state = defineOne;
                            }
                        }else if(/* SQ0<=b &&*/b <= SQ7){
                            quoteWindow = (byte)(b - SQ0);
                            state = quoteOne;
                        }else if(b==SDX){
                            state = definePairOne;
                        }else if(b==SQU){
                            state = quotePairOne;
                        }else if(b==SCU){
                            sourceIndex = nextSourceIndex;
                            isSingleByteMode = false;
                            label = FastSingle;
                            return label;
                        }else{
                            /*callback (illegal)*/
                            cr = CoderResult.malformedForLength(1);
                            toUBytesArray[0] = (byte)b;
                            toULength =1;
                            label = EndLoop;
                            return label;
                        }
                        
                        /* Store the first byte of a multibyte sequence in toUByte[] */
                        toUBytesArray[0] = (byte)b;
                        toULength = 1;
                        break;
                    case quotePairOne:
                        byteOne = b;
                        toUBytesArray[1] = (byte)b;
                        toULength = 2;
                        state = quotePairTwo;
                        break;
                    case quotePairTwo:
                        target.put((char)((byteOne<< 8) | b));
                        if(offsets != null){
                            offsets.put(sourceIndex);
                        }
                        sourceIndex = nextSourceIndex;
                        state = readCommand;
                        label = FastSingle;
                        return label;
                    case quoteOne:
                        if(b<0x80){
                            /* all static offsets are in the BMP */
                            target.put((char)(staticOffsets[quoteWindow] + b));
                            if(offsets != null){
                                offsets.put(sourceIndex);
                            }
                        }else {
                            /*write from dynamic window */
                            int c = data.toUDynamicOffsets[quoteWindow] + (b&0x7f);
                            if(c<=0xffff){
                                target.put((char)c);
                                if(offsets != null){
                                    offsets.put(sourceIndex);
                                }
                            }else {
                                /* output surrogate pair */
                                target.put((char)(0xd7c0+(c>>10)));
                                if(target.hasRemaining()){
                                    target.put((char)(0xdc00 | (c&0x3ff)));
                                    if(offsets != null){
                                        offsets.put(sourceIndex);
                                        offsets.put(sourceIndex);
                                    }
                                }else {
                                    /* target overflow */
                                    if(offsets != null){
                                        offsets.put(sourceIndex);
                                    }
                                    charErrorBufferArray[0] = (char)(0xdc00 | (c&0x3ff));
                                    charErrorBufferLength = 1;
                                    label = EndLoop;
                                    cr = CoderResult.OVERFLOW;
                                    LabelLoop = false;
                                    return label;
                                }
                            }
                        }
                        sourceIndex = nextSourceIndex;
                        state = readCommand;
                        label = FastSingle;
                        return label;
                    case definePairOne:
                        dynamicWindow = (byte)((b>>5)&7);
                        byteOne = (byte)(b&0x1f);
                        toUBytesArray[1] = (byte)b;
                        toULength = 2;
                        state = definePairTwo;
                        break;
                    case definePairTwo:
                        data.toUDynamicOffsets[dynamicWindow] = 0x10000 + (byteOne<<15L | b<<7L);
                        sourceIndex = nextSourceIndex;
                        state = readCommand;
                        label = FastSingle;
                        return label;
                    case defineOne:
                        if(b==0){
                            /*callback (illegal)*/
                            toUBytesArray[1] = (byte)b;
                            toULength =2;
                            label = EndLoop;
                            return label;
                        }else if(b<gapThreshold){
                            data.toUDynamicOffsets[dynamicWindow] = b<<7L;
                        }else if((byte)(b - gapThreshold)<(reservedStart - gapThreshold)){
                            data.toUDynamicOffsets[dynamicWindow] = (b<<7L) + gapOffset;
                        }else if(b>=fixedThreshold){
                            data.toUDynamicOffsets[dynamicWindow] = fixedOffsets[b-fixedThreshold];
                        }else{
                            /*callback (illegal)*/
                            toUBytesArray[1] = (byte)b;
                            toULength =2;
                            label = EndLoop;
                            return label;
                        }
                        sourceIndex = nextSourceIndex;
                        state = readCommand;
                        label = FastSingle;
                        return label;
                    }
                }
                
            }else if(modeType==UnicodeMode){
                while(source.hasRemaining()){
                    if(!target.hasRemaining()){
                        cr = CoderResult.OVERFLOW;
                        LabelLoop = false;
                        return label;
                    }
                    b = (short)(source.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
                    ++nextSourceIndex;
                    switch(state){
                    case readCommand:
                        if((short)((b -UC0)&UConverterConstants.UNSIGNED_BYTE_MASK)>(Urs - UC0)){
                            byteOne = b;
                            toUBytesArray[0] = (byte)b;
                            toULength = 1;
                            state = quotePairTwo;
                        }else if((b&UConverterConstants.UNSIGNED_BYTE_MASK) <= UC7){
                            dynamicWindow = (byte)(b - UC0);
                            sourceIndex = nextSourceIndex;
                            isSingleByteMode = true;
                            label = FastSingle;
                            return label;
                        }else if((b&UConverterConstants.UNSIGNED_BYTE_MASK) <= UD7){
                            dynamicWindow = (byte)(b - UD0);
                            isSingleByteMode = true;
                            toUBytesArray[0] = (byte)b;
                            toULength = 1;
                            state = defineOne;
                            label = SingleByteMode;
                            return label;
                        }else if((b&UConverterConstants.UNSIGNED_BYTE_MASK) == UDX){
                            isSingleByteMode = true;
                            toUBytesArray[0] = (byte)b;
                            toULength = 1;
                            state = definePairOne;
                            label = SingleByteMode;
                            return label;
                        }else if((b&UConverterConstants.UNSIGNED_BYTE_MASK) == UQU){
                            toUBytesArray[0] = (byte)b;
                            toULength = 1;
                            state = quotePairOne;
                        }else {
                            /* callback (illegal)*/
                            cr = CoderResult.malformedForLength(1);
                            toUBytesArray[0] = (byte)b;
                            toULength = 1;
                            label = EndLoop;
                            return label;
                        }
                        break;
                    case quotePairOne:
                        byteOne = b;
                        toUBytesArray[1] = (byte)b;
                        toULength = 2;
                        state = quotePairTwo;
                        break;
                    case quotePairTwo:
                        target.put((char)((byteOne<<8) | b));
                        if(offsets != null){
                            offsets.put(sourceIndex);
                        }
                        sourceIndex = nextSourceIndex;
                        state = readCommand;
                        label = FastSingle;
                        return label;
                    }
                }
            }
            label = EndLoop;
            return label;
        }
        
        private void endLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets){
            if(cr==CoderResult.OVERFLOW){
                state = readCommand;
            }else if(state == readCommand){
                toULength = 0;
            }
            data.toUIsSingleByteMode = isSingleByteMode;
            data.toUState = state;
            data.toUQuoteWindow = quoteWindow;
            data.toUDynamicWindow = dynamicWindow;
            data.toUByteOne = byteOne;
            LabelLoop = false;
        }
    }
    
    class CharsetEncoderSCSU extends CharsetEncoderICU{
        public CharsetEncoderSCSU(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }
        
        //private SCSUData data;
        protected void implReset() {
            super.implReset();
            extraInfo.initialize();
        }
        
        /* label values for supporting behavior similar to goto in C */
        private static final int Loop=0; 
        private static final int GetTrailUnicode=1;
        private static final int OutputBytes=2;
        private static final int EndLoop =3;
        
        private int delta;
        private int length;
        
        ///variables of compression heuristics
        private int offset;
        private char lead, trail;
        private int code;
        private byte window;
        
        //Get the state machine state 
        private boolean isSingleByteMode;
        private byte dynamicWindow ;
        private int currentOffset;
        int c;
        
        SCSUData data ;
        
        //sourceIndex=-1 if the current character began in the previous buffer
        private int sourceIndex ;
        private int nextSourceIndex;
        private int targetCapacity;
        
        private boolean LabelLoop;//used to break the while loop
        private boolean AfterGetTrail;// its value is set to true in order to ignore the code before getTrailSingle:
        private boolean AfterGetTrailUnicode;// is value is set to true in order to ignore the code before getTrailUnicode:
        
        CoderResult cr;
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            data = extraInfo;
            cr = CoderResult.UNDERFLOW;
            
            //Get the state machine state 
            isSingleByteMode = data.fromUIsSingleByteMode;
            dynamicWindow = data.fromUDynamicWindow;
            currentOffset = data.fromUDynamicOffsets[dynamicWindow];
            c = fromUChar32;
            
            sourceIndex = c== 0 ? 0: -1 ;
            nextSourceIndex = 0;
                        
            
            targetCapacity = target.limit()-target.position();
            
            //sourceIndex=-1 if the current character began in the previous buffer
            sourceIndex = c== 0 ? 0: -1 ;
            nextSourceIndex = 0;
            
            int labelType = Loop; // set to Loop so that the code starts from loop:
            LabelLoop = true; 
            AfterGetTrail = false; 
            AfterGetTrailUnicode = false; 
            
            while(LabelLoop){
                switch(labelType){
                case Loop:
                    labelType = loop(source, target, offsets);
                    break;
                case GetTrailUnicode:
                    labelType = getTrailUnicode(source, target, offsets);
                    break;
                case OutputBytes:
                    labelType = outputBytes(source, target, offsets);
                    break;
                case EndLoop:
                    endLoop(source, target, offsets);
                    break;
                }
            }
            return cr;
        }
        
        private byte getWindow(int[] offsets){
            int i;
            for (i=0;i<8;i++){
                if(((c-offsets[i]) & UConverterConstants.UNSIGNED_INT_MASK) <= 0x7f){
                    return (byte)i;
                }
            }
            return -1;
        }
        
        private boolean isInOffsetWindowOrDirect(int offsetValue, int a){
            return (boolean)((a & UConverterConstants.UNSIGNED_INT_MASK)<=(offsetValue & UConverterConstants.UNSIGNED_INT_MASK)+0x7f & 
                    ((a & UConverterConstants.UNSIGNED_INT_MASK)>=(offsetValue & UConverterConstants.UNSIGNED_INT_MASK) || 
                            ((a & UConverterConstants.UNSIGNED_INT_MASK)<=0x7f && ((a & UConverterConstants.UNSIGNED_INT_MASK)>=0x20 
                                    || ((1L<<(a & UConverterConstants.UNSIGNED_INT_MASK))&0x2601)!=0))));
        }
        
        private byte getNextDynamicWindow(){
            byte windowValue = data.windowUse[data.nextWindowUseIndex];
            if(++data.nextWindowUseIndex==8){
                data.nextWindowUseIndex=0;
            }
            return windowValue;
        }
        
        private void useDynamicWindow(byte windowValue){
            /*first find the index of the window*/
            int i,j;
            i = data.nextWindowUseIndex;
            do{
                if(--i<0){
                    i=7;
                }
            }while(data.windowUse[i]!=windowValue);
            
            /*now copy each window[i+1] to [i]*/
            j= i+1;
            if(j==8){
                j=0;
            }
            while(j!=data.nextWindowUseIndex){
                data.windowUse[i] = data.windowUse[j];
                i=j;
                if(++j==8){
                    j=0;
                }
            }
            
            /*finally, set the window into the most recently used index*/
            data.windowUse[i]= windowValue;
        }
        
        
       private int getDynamicOffset(){
            int i;
            for(i=0;i<7;++i){
                if(((c-fixedOffsets[i])&UConverterConstants.UNSIGNED_INT_MASK)<=0x7f){
                    offset = fixedOffsets[i];
                    return 0xf9+i;
                }
            }
            if((c&UConverterConstants.UNSIGNED_INT_MASK)<0x80){
                /*No dynamic window for US-ASCII*/
                return -1;
            }else if((c&UConverterConstants.UNSIGNED_INT_MASK)<0x3400 || ((c-0x10000)&UConverterConstants.UNSIGNED_INT_MASK)<(0x14000-0x10000) || 
                    ((c-0x1d000)&UConverterConstants.UNSIGNED_INT_MASK)<=(0x1ffff-0x1d000)){
                /*This character is in the code range for a "small", i.e, reasonably windowable, script*/
                offset = c&0x7fffff80;
                return (int)(c>>7);
            }else if(0xe000<=(c&UConverterConstants.UNSIGNED_INT_MASK) && (c&UConverterConstants.UNSIGNED_INT_MASK)!=0xfeff && (c&UConverterConstants.UNSIGNED_INT_MASK) < 0xfff0){
                /*for these characters we need to take the gapOffset into account*/
                offset=(c)&0x7fffff80;
                return (int)((c-gapOffset)>>7);
            }else{
                return -1;
            }
        }
        
        private int loop(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            int label = 0;
            if(isSingleByteMode){
                if(c!=0 && targetCapacity>0 && !AfterGetTrail){
                    label = getTrail(source, target, offsets);
                    return label;
                }
                /*state machine for single byte mode*/
                while(AfterGetTrail || source.hasRemaining()){
                    if(targetCapacity<=0 && !AfterGetTrail){
                        /*target is full*/
                        cr = CoderResult.OVERFLOW;
                        label = EndLoop;
                        return label;
                    }
                    if(!AfterGetTrail){
                        c = source.get();
                        ++nextSourceIndex;
                        
                    }
                    if(((c -0x20)&UConverterConstants.UNSIGNED_INT_MASK)<=0x5f && !AfterGetTrail){
                        /*pass US-ASCII graphic character through*/
                        target.put((byte)c);
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                        }
                        --targetCapacity;
                    }else if((c & UConverterConstants.UNSIGNED_INT_MASK)<0x20 && !AfterGetTrail){
                        if(((1L<<(c & UConverterConstants.UNSIGNED_INT_MASK))&0x2601)!=0){
                            /*CR/LF/TAB/NUL*/
                            target.put((byte)c);
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                            --targetCapacity;
                        } else {
                            /*quote c0 control character*/
                            c|=SQ0<<8;
                            length = 2;
                            label = OutputBytes;
                            return label;
                        }
                    } else if(((delta=(c-currentOffset))&UConverterConstants.UNSIGNED_INT_MASK)<=0x7f && !AfterGetTrail){
                        /*use the current dynamic window*/
                        target.put((byte)(delta|0x80));
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                        }
                        --targetCapacity;
                    } else if(AfterGetTrail || UTF16.isSurrogate((char)c)){
                        if(!AfterGetTrail){
                            if(UTF16.isLeadSurrogate((char)c)){
                                label = getTrail(source, target, offsets);
                                if(label==EndLoop){
                                    return label;
                                }
                            } else {
                                /*this is unmatched lead code unit (2nd Surrogate)*/
                                /*callback(illegal)*/
                                cr = CoderResult.malformedForLength(1);
                                label = EndLoop;
                                return label;
                            }
                        }
                                                
                        
                        if(AfterGetTrail){
                            AfterGetTrail = false;
                        }
                        
                        /*Compress supplementary character U+10000...U+10ffff */
                        if(((delta=(c-currentOffset))&UConverterConstants.UNSIGNED_INT_MASK)<=0x7f){
                            /*use the current dynamic window*/
                            target.put((byte)(delta|0x80));
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                            --targetCapacity;
                        } else if((window=getWindow(data.fromUDynamicOffsets))>=0){
                            /*there is a dynamic window that contains this character, change to it*/
                            dynamicWindow = window;
                            currentOffset = data.fromUDynamicOffsets[dynamicWindow];
                            useDynamicWindow(dynamicWindow);
                            c = (((int)(SC0+dynamicWindow))<<8 | (c-currentOffset)|0x80);
                            length = 2;
                            label  = OutputBytes;
                            return label;
                        } else if((code=getDynamicOffset())>=0){
                            /*might check if there are come character in this window to come */
                            /*define an extended window with this character*/
                            code-=0x200;
                            dynamicWindow=getNextDynamicWindow();
                            currentOffset = data.fromUDynamicOffsets[dynamicWindow]=offset;
                            useDynamicWindow(dynamicWindow);
                            c = ((int)(SDX<<24) | (int)(dynamicWindow<<21)|
                                 (int)(code<<8)| (c- currentOffset) |0x80  );
                           // c = (((SDX)<<25) | (dynamicWindow<<21)|
                             //           (code<<8)| (c- currentOffset) |0x80  );
                            length = 4;
                            label = OutputBytes;
                            return label;
                        } else {
                            /*change to unicode mode and output this (lead, trail) pair*/
                            isSingleByteMode = false;
                            target.put((byte)SCU);
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                            --targetCapacity;
                            c = ((int)(lead<<16))|trail;
                            length = 4;
                            label = OutputBytes;
                            return label;
                        }
                    } else if((c&UConverterConstants.UNSIGNED_INT_MASK)<0xa0){
                        /*quote C1 control character*/
                        c = (c&0x7f) | (SQ0+1)<<8; /*SQ0+1 == SQ1*/
                        length = 2;
                        label = OutputBytes;
                        return label;
                    } else if((c&UConverterConstants.UNSIGNED_INT_MASK)==0xfeff || (c&UConverterConstants.UNSIGNED_INT_MASK)>= 0xfff0){
                        /*quote signature character = byte order mark and specials*/
                        c |= SQU<<16;
                        length = 3;
                        label = OutputBytes;
                        return label;
                    } else {
                        /*compress all other BMP characters*/
                        if((window=getWindow(data.fromUDynamicOffsets))>=0){
                            /*there is a window defined that contains this character - switch to it or quote from it*/
                            if(source.position()>=source.limit() || isInOffsetWindowOrDirect(data.fromUDynamicOffsets[window], source.get(source.position()))){
                                /*change to dynamic window*/
                                dynamicWindow = window;
                                currentOffset = data.fromUDynamicOffsets[dynamicWindow];
                                useDynamicWindow(dynamicWindow);
                                c = ((int)((SC0+window)<<8)) | (c- currentOffset) | 0x80;
                                length = 2;
                                label = OutputBytes;
                                return label;
                            } else {
                                /*quote from dynamic window*/
                                c = ((int)((SQ0+window)<<8)) | (c - data.fromUDynamicOffsets[window]) |
                                    0x80;
                                length = 2;
                                label = OutputBytes;
                                return label;
                            }
                        } else if((window = getWindow(staticOffsets))>=0){
                            /*quote from static window*/
                            c = ((int)((SQ0+window)<<8)) | (c - staticOffsets[window]);
                            length = 2;
                            label = OutputBytes;
                            return label;
                        }else if((code=getDynamicOffset())>=0){
                            /*define a dynamic window with this character*/
                            dynamicWindow = getNextDynamicWindow();
                            currentOffset = data.fromUDynamicOffsets[dynamicWindow]=offset;
                            useDynamicWindow(dynamicWindow);
                            c = ((int)((SD0+dynamicWindow)<<16)) | (int)(code<<8)|
                                (c- currentOffset) | 0x80;
                            length = 3;
                            label = OutputBytes;
                            return label;
                        } else if(((int)((c-0x3400)&UConverterConstants.UNSIGNED_INT_MASK))<(0xd800-0x3400) && (source.position()>=source.limit() || 
                                ((int)((source.get(source.position())-0x3400)&UConverterConstants.UNSIGNED_INT_MASK))< (0xd800 - 0x3400))){
                            
                            /*
                             * this character is not compressible (a BMP ideograph of similar)
                             * switch to Unicode mode if this is the last character in the block
                             * or there is at least one more ideograph following immediately
                             */
                             isSingleByteMode = false;
                             c|=SCU<<16;
                             length =3;
                             label = OutputBytes;
                             return label;
                        } else {
                            /*quote Unicode*/
                            c|=SQU<<16;
                            length = 3;
                            label = OutputBytes;
                            return label;
                        }
                    }
                    /*normal end of conversion : prepare for new character */
                    c = 0;
                    sourceIndex = nextSourceIndex;
                }
            } else {
                if(c!=0 && targetCapacity>0 && !AfterGetTrailUnicode){
                    label = GetTrailUnicode;
                    return label;
                }
            
                /*state machine for Unicode*/
                /*unicodeByteMode*/
                while(AfterGetTrailUnicode || source.hasRemaining()){
                    if(targetCapacity<=0 && !AfterGetTrailUnicode){
                        /*target is full*/
                        cr = CoderResult.OVERFLOW;
                        LabelLoop = false;
                        break;
                    }
                    if(!AfterGetTrailUnicode){
                        c = source.get();
                        ++nextSourceIndex;
                    }
                    
                    if((((c-0x3400)& UConverterConstants.UNSIGNED_INT_MASK))<(0xd800-0x3400) && !AfterGetTrailUnicode){
                        /*not compressible, write character directly */
                        if(targetCapacity>=2){
                            target.put((byte)(c>>8));
                            target.put((byte)c);
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                                offsets.put(sourceIndex);
                            }
                            targetCapacity-=2;
                        } else {
                            length =2;
                            label = OutputBytes;
                            return label;
                        }
                    } else if((((c-0x3400)& UConverterConstants.UNSIGNED_INT_MASK))>=(0xf300-0x3400) /* c<0x3400 || c>=0xf300*/&& !AfterGetTrailUnicode){
                        /*compress BMP character if the following one is not an uncompressible ideograph*/
                        if(!(source.hasRemaining() && (((source.get(source.position())-0x3400)& UConverterConstants.UNSIGNED_INT_MASK))<(0xd800-0x3400))){
                            if(((((c-0x30)&UConverterConstants.UNSIGNED_INT_MASK))<10 || (((c-0x61)&UConverterConstants.UNSIGNED_INT_MASK))<26 
                                    || (((c-0x41)&UConverterConstants.UNSIGNED_INT_MASK))<26)){
                                /*ASCII digit or letter*/
                                isSingleByteMode = true;
                                c |=((int)((UC0+dynamicWindow)<<8))|c;
                                length = 2;
                                label = OutputBytes;
                                return label;
                            } else if((window=getWindow(data.fromUDynamicOffsets))>=0){
                                /*there is a dynamic window that contains this character, change to it*/
                                isSingleByteMode = true;
                                dynamicWindow = window;
                                currentOffset = data.fromUDynamicOffsets[dynamicWindow];
                                useDynamicWindow(dynamicWindow);
                                c = ((int)((UC0+dynamicWindow)<<8)) | (c- currentOffset) | 0x80;
                                length = 2;
                                label = OutputBytes;
                                return label;
                            } else if((code=getDynamicOffset())>=0){
                                /*define a dynamic window with this character*/
                                isSingleByteMode = true;
                                dynamicWindow = getNextDynamicWindow();
                                currentOffset = data.fromUDynamicOffsets[dynamicWindow]=offset;
                                useDynamicWindow(dynamicWindow);
                                c = ((int)((UD0+dynamicWindow)<<16)) | (int)(code<<8) 
                                    |(c- currentOffset) | 0x80;
                                length = 3;
                                label = OutputBytes;
                                return label;
                            }
                        }
                        
                        /*don't know how to compress these character, just write it directly*/
                        length = 2;
                        label = OutputBytes;
                        return label;
                    } else if(c<0xe000 && !AfterGetTrailUnicode){
                        label = GetTrailUnicode;
                        return label;
                    } else if (!AfterGetTrailUnicode){
                        /*quote to avoid SCSU tags*/
                        c|=UQU<<16;
                        length = 3;
                        label = OutputBytes;
                        return label;
                    }
                    
                    if(AfterGetTrailUnicode){
                        AfterGetTrailUnicode = false;
                    }
                    /*normal end of conversion, prepare for a new character*/
                    c = 0;
                    sourceIndex = nextSourceIndex;
                }
            }
            label = EndLoop;
            return label;
        }
        
        private int getTrail(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            lead = (char)c;
            int label = Loop;
            if(source.hasRemaining()){
                /*test the following code unit*/
                trail = source.get(source.position());
                if(UTF16.isTrailSurrogate((char)trail)){
                    source.position(source.position()+1);
                    ++nextSourceIndex;
                    c = UCharacter.getCodePoint((char)c, trail);
                    label = Loop;
                } else {
                    /*this is unmatched lead code unit (1st Surrogate)*/
                    /*callback(illegal)*/
                    cr = CoderResult.malformedForLength(1);
                    label = EndLoop;
                }
            }else {
                /*no more input*/
                label = EndLoop;
            }
            AfterGetTrail = true;
            return label;
        }
        
        private int getTrailUnicode(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            int label = EndLoop;
            AfterGetTrailUnicode = true;
            /*c is surrogate*/
            if(UTF16.isLeadSurrogate((char)c)){
      // getTrailUnicode:   
                lead = (char)c;
                if(source.hasRemaining()){
                    /*test the following code unit*/
                    trail = source.get(source.position());
                    if(UTF16.isTrailSurrogate(trail)){
                        source.get();
                        ++nextSourceIndex;
                        c = UCharacter.getCodePoint((char)c, trail);
                        /*convert this surrogate code point*/
                        /*exit this condition tree*/
                    } else {
                        /*this is unmatched lead code unit(1st surrogate)*/
                        /*callback(illegal)*/
                        cr = CoderResult.malformedForLength(1);
                        label = EndLoop;
                        return label;
                    }
                } else {
                    /*no more input*/
                    label = EndLoop;
                    return label;
                }
            } else {
                /*this is an unmatched trail code point (2nd surrogate)*/
                /*callback (illegal)*/
                cr = CoderResult.malformedForLength(1);
                label = EndLoop;
                return label;
            }
            
            /*compress supplementary character*/
            if((window=getWindow(data.fromUDynamicOffsets))>=0 && 
                    !(source.hasRemaining() && ((source.get(source.position())-0x3400)&UConverterConstants.UNSIGNED_INT_MASK) < 
                            (0xd800 - 0x3400))){
                /*
                 * this is the dynamic window that contains this character and the following
                 * character is not uncompressible,
                 * change to the window
                 */
                isSingleByteMode = true;
                dynamicWindow = window;
                currentOffset = data.fromUDynamicOffsets[dynamicWindow];
                useDynamicWindow(dynamicWindow);
                c = ((UC0+dynamicWindow)<<8 | (c-currentOffset) | 0x80);
                length = 2;
                label = OutputBytes;
                return label;
            } else if(source.hasRemaining() && lead == source.get(source.position()) && (code=getDynamicOffset())>=0){
                /*two supplementary characters in (probably) the same window - define an extended one*/
                isSingleByteMode = true;
                dynamicWindow = getNextDynamicWindow();
                currentOffset = data.fromUDynamicOffsets[dynamicWindow] = offset;
                useDynamicWindow(dynamicWindow);
                c = (UDX<<24) | (dynamicWindow<<21) |(code<<8) |(c - currentOffset) | 0x80;
                length = 4;
                label = OutputBytes;
                return label;
            } else {
                /*don't know how to compress this character, just write it directly*/
                c = (lead<<16)|trail;
                length = 4;
                label = OutputBytes;
                return label;
            }
            
        }
        
        private void endLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            /*set the converter state back to UConverter*/
            data.fromUIsSingleByteMode = isSingleByteMode;
            data.fromUDynamicWindow = dynamicWindow;
            fromUChar32 = c;
            LabelLoop = false;
        }
        
        private int outputBytes(CharBuffer source, ByteBuffer target, IntBuffer offsets){
            int label;
            //int targetCapacity = target.limit()-target.position();
            /*write the output character byte from c and length*/
            /*from the first if in the loop we know that targetCapacity>0*/
            if(length<=targetCapacity){
                if(offsets==null){
                    switch(length){
                    /*each branch falls through the next one*/
                        case 4:
                            target.put((byte)(c>>24));
                        case 3:
                            target.put((byte)(c>>16));
                        case 2:
                            target.put((byte)(c>>8));
                        case 1:
                            target.put((byte)c);
                        default:
                            /*will never occur*/
                            break;
                    }
                }else {
                    switch(length){
                        /*each branch falls through to the next one*/
                        case 4:
                            target.put((byte)(c>>24));
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                        case 3:
                            target.put((byte)(c>>16));
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                        case 2:
                            target.put((byte)(c>>8));
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                        case 1:
                            target.put((byte)c);
                            if(offsets!=null){
                                offsets.put(sourceIndex);
                            }
                        default:
                            /*will never occur*/
                            break;
                    }
                }
                targetCapacity-=length;
                
                /*normal end of conversion: prepare for a new character*/
                c = 0;
                sourceIndex = nextSourceIndex;
                label = Loop;
                return label;
            } else {
                ByteBuffer p = ByteBuffer.wrap(errorBuffer);
                /*
                 * We actually do this backwards here:
                 * In order to save an intermediate variable, we output
                 * first to the overflow buffer what does not fit into the 
                 * regular target
                 */
                /* we know that 0<=targetCapacity<length<=4 */
                /* targetCapacity==0 when SCU+supplementary where SCU used up targetCapacity==1 */
                length -= targetCapacity;
                switch(length){
                    /*each branch falls through the next one*/
                    case 4:
                        p.put((byte)(c>>24));
                    case 3:
                        p.put((byte)(c>>16));
                    case 2:
                        p.put((byte)(c>>8));
                    case 1:
                        p.put((byte)c);
                    default:
                        /*will never occur*/
                        break;
                }
                errorBufferLength = length;
                
                /*now output what fits into the regular target*/
                c>>=8*length; //length was reduced by targetCapacity
                switch(targetCapacity){
                    /*each branch falls through the next one*/
                    case 3:
                        target.put((byte)(c>>16));
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                        }
                    case 2:
                        target.put((byte)(c>>8));
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                        }
                    case 1:
                        target.put((byte)c);
                        if(offsets!=null){
                            offsets.put(sourceIndex);
                        }
                    default:
                        break;
                }
                
                /*target overflow*/
                targetCapacity = 0;
                cr = CoderResult.OVERFLOW;
                c = 0;
                label = EndLoop;
                return label;
            }
        }
        
    }
    
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderSCSU(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderSCSU(this);
    }
    
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        CharsetICU.getCompleteUnicodeSet(setFillIn);
    }
    
}
