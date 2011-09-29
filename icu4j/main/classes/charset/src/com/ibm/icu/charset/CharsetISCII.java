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

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author Michael Ow
 *
 */
class CharsetISCII extends CharsetICU {
    private static final short UCNV_OPTIONS_VERSION_MASK = 0X0f;
    //private static final short NUKTA = 0x093c;
    //private static final short HALANT = 0x094d;
    private static final short ZWNJ = 0x200c; /* Zero Width Non Joiner */
    private static final short ZWJ = 0x200d; /* Zero Width Joiner */
    //private static final int   INVALID_CHAR = 0xffff;
    private static final short ATR = 0xef; /* Attribute code */
    private static final short EXT = 0xf0; /* Extension code */
    private static final short DANDA = 0x0964;
    private static final short DOUBLE_DANDA = 0x0965;
    private static final short ISCII_NUKTA = 0xe9;
    private static final short ISCII_HALANT = 0xe8;
    private static final short ISCII_DANDA = 0xea;
    private static final short ISCII_VOWEL_SIGN_E = 0xe0;
    private static final short ISCII_INV = 0xd9;
    private static final short INDIC_BLOCK_BEGIN = 0x0900;
    private static final short INDIC_BLOCK_END = 0x0d7f;
    private static final short INDIC_RANGE = (INDIC_BLOCK_END - INDIC_BLOCK_BEGIN);
    private static final short VOCALLIC_RR = 0x0931;
    private static final short LF = 0x0a;
    private static final short ASCII_END = 0xa0;
    private static final short TELUGU_DELTA = (UniLang.DELTA * UniLang.TELUGU);
    private static final short DEV_ABBR_SIGN = 0x0970;
    private static final short DEV_ANUDATTA = 0x0952;
    private static final short EXT_RANGE_BEGIN = 0xa1;
    private static final short EXT_RANGE_END = 0xee;
    private static final short PNJ_DELTA = 0x100;
    private static final int   NO_CHAR_MARKER = 0xfffe;
    
    /* Used for proper conversion to and from Gurmukhi */
    private static UnicodeSet  PNJ_BINDI_TIPPI_SET;
    private static UnicodeSet  PNJ_CONSONANT_SET;
    private static final short PNJ_BINDI        = 0x0a02;
    private static final short PNJ_TIPPI        = 0x0a70;
    private static final short PNJ_SIGN_VIRAMA  = 0x0a4d;
    private static final short PNJ_ADHAK        = 0x0a71;
    private static final short PNJ_HA           = 0x0a39;
    private static final short PNJ_RRA          = 0x0a5c;
    
    private static final class UniLang {
        static final short DEVALANGARI = 0;
        static final short BENGALI = DEVALANGARI + 1;
        static final short GURMUKHI = BENGALI + 1;
        static final short GUJARATI = GURMUKHI + 1;
        static final short ORIYA = GUJARATI + 1;
        static final short TAMIL = ORIYA + 1;
        static final short TELUGU = TAMIL + 1;
        static final short KANNADA = TELUGU + 1;
        static final short MALAYALAM = KANNADA + 1;
        static final short DELTA = 0x80;
    }
    @SuppressWarnings("unused")
    private static final class ISCIILang {
        static final short DEF = 0x40;
        static final short RMN = 0x41;
        static final short DEV = 0x42;
        static final short BNG = 0x43;
        static final short TML = 0x44;
        static final short TLG = 0x45;
        static final short ASM = 0x46;
        static final short ORI = 0x47;
        static final short KND = 0x48;
        static final short MLM = 0x49;
        static final short GJR = 0x4a;
        static final short PNJ = 0x4b;
        static final short ARB = 0x71;
        static final short PES = 0x72;
        static final short URD = 0x73;
        static final short SND = 0x74;
        static final short KSM = 0x75;
        static final short PST = 0x76;
    }
    
    private static final class MaskEnum {
        static final short DEV_MASK = 0x80;
        static final short PNJ_MASK = 0x40;
        static final short GJR_MASK = 0x20;
        static final short ORI_MASK = 0x10;
        static final short BNG_MASK = 0x08;
        static final short KND_MASK = 0x04;
        static final short MLM_MASK = 0x02;
        static final short TML_MASK = 0x01;
        static final short ZERO = 0x00;
    }
    
    private final String ISCII_CNV_PREFIX = "ISCII,version=";
    
    @SuppressWarnings("unused")
    private final class UConverterDataISCII {
        int option;
        int contextCharToUnicode;      /* previous Unicode codepoint for contextual analysis */
        int contextCharFromUnicode;    /* previous Unicode codepoint for contextual analysis */
        short defDeltaToUnicode;             /* delta for switching to default state when DEF is encountered */
        short currentDeltaFromUnicode;   /* current delta in Indic block */
        short currentDeltaToUnicode;         /* current delta in Indic block */
        short currentMaskFromUnicode;    /* mask for current state in fromUnicode */
        short currentMaskToUnicode;          /* mask for current state in toUnicode */
        short defMaskToUnicode;           /* mask for default state in toUnicode */
        boolean isFirstBuffer;          /* boolean for fromUnicode to see if we need to announce the first script */
        boolean resetToDefaultToUnicode;    /* boolean for reseting to default delta and mask when a newline is encountered */
        String name;
        int prevToUnicodeStatus;        /* Hold the previous toUnicodeStatus. This is necessary because we may need to know the last two code points. */
        
        UConverterDataISCII(int option, String name) {
            this.option = option;
            this.name = name;
            
            initialize();
        }
        
        void initialize() {          
            this.contextCharToUnicode = NO_CHAR_MARKER; /* contextCharToUnicode */
            this.currentDeltaFromUnicode = 0x0000; /* contextCharFromUnicode */
            this.defDeltaToUnicode = (short)(lookupInitialData[option & UCNV_OPTIONS_VERSION_MASK].uniLang * UniLang.DELTA); /* defDeltaToUnicode */ 
            this.currentDeltaFromUnicode = (short)(lookupInitialData[option & UCNV_OPTIONS_VERSION_MASK].uniLang * UniLang.DELTA); /* currentDeltaFromUnicode */ 
            this.currentDeltaToUnicode = (short)(lookupInitialData[option & UCNV_OPTIONS_VERSION_MASK].uniLang * UniLang.DELTA); /* currentDeltaToUnicode */ 
            this.currentMaskToUnicode = lookupInitialData[option & UCNV_OPTIONS_VERSION_MASK].maskEnum; /* currentMaskToUnicode */
            this.currentMaskFromUnicode = lookupInitialData[option & UCNV_OPTIONS_VERSION_MASK].maskEnum; /* currentMaskFromUnicode */
            this.defMaskToUnicode = lookupInitialData[option & UCNV_OPTIONS_VERSION_MASK].maskEnum; /* defMaskToUnicode */
            this.isFirstBuffer = true; /* isFirstBuffer */
            this.resetToDefaultToUnicode = false; /* resetToDefaultToUnicode */   
            this.prevToUnicodeStatus = 0x0000;
        }
    }
    
    private static final class LookupDataStruct {
        short uniLang;
        short maskEnum;
        short isciiLang;
        
        LookupDataStruct(short uniLang, short maskEnum, short isciiLang) {
            this.uniLang = uniLang;
            this.maskEnum = maskEnum;
            this.isciiLang = isciiLang;
        }
    }
    
    private static final LookupDataStruct [] lookupInitialData = {
        new LookupDataStruct(UniLang.DEVALANGARI, MaskEnum.DEV_MASK, ISCIILang.DEV),
        new LookupDataStruct(UniLang.BENGALI, MaskEnum.BNG_MASK, ISCIILang.BNG),
        new LookupDataStruct(UniLang.GURMUKHI, MaskEnum.PNJ_MASK, ISCIILang.PNJ),
        new LookupDataStruct(UniLang.GUJARATI, MaskEnum.GJR_MASK, ISCIILang.GJR),
        new LookupDataStruct(UniLang.ORIYA, MaskEnum.ORI_MASK, ISCIILang.ORI),
        new LookupDataStruct(UniLang.TAMIL, MaskEnum.TML_MASK, ISCIILang.TML),
        new LookupDataStruct(UniLang.TELUGU, MaskEnum.KND_MASK, ISCIILang.TLG),
        new LookupDataStruct(UniLang.KANNADA, MaskEnum.KND_MASK, ISCIILang.KND),
        new LookupDataStruct(UniLang.MALAYALAM, MaskEnum.MLM_MASK, ISCIILang.MLM)
    };
    
    /*
     * The values in validity table are indexed by the lower bits of Unicode
     * range 0x0900 - 0x09ff. The values have a structure like:
     * -----------------------------------------------------------------
     * |DEV | PNJ | GJR | ORI | BNG | TLG | MLM | TML |
     * |    |    |    |    | ASM | KND |    |    |
     * -----------------------------------------------------------------
     * If a code point is valid in a particular script
     * then that bit is turned on
     * 
     * Unicode does not distinguish between Bengali and Assamese aso we use 1 bit for
     * to represent these languages
     * 
     * Telugu and Kannda have same codepoints except for Vocallic_RR which we special case
     * and combine and use 1 bit to represent these languages
     */
    private static final short validityTable[] = {
        /* This state table is tool generated so please do not edit unless you know exactly what you are doing */
        /* Note:  This table was edited to mirror the Windows XP implementation */
        /* ISCII: Valid: Unicode */
        /* 0xa0: 0x00: 0x900 */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xa1: 0xb8: 0x901 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xa2: 0xfe: 0x902 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK, 
        /* 0xa3: 0xbf: 0x903 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0x00: 0x00: 0x904 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xa4: 0xff: 0x905 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xa5: 0xff: 0x906 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xa6: 0xff: 0x907 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xa7: 0xff: 0x908 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xa8: 0xff: 0x909 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xa9: 0xff: 0x90a */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xaa: 0xfe: 0x90b */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x90c */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xae: 0x80: 0x90d */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xab: 0x87: 0x90e */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xac: 0xff: 0x90f */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xad: 0xff: 0x910 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xb2: 0x80: 0x911 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xaf: 0x87: 0x912 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xb0: 0xff: 0x913 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xb1: 0xff: 0x914 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xb3: 0xff: 0x915 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xb4: 0xfe: 0x916 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xb5: 0xfe: 0x917 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xb6: 0xfe: 0x918 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xb7: 0xff: 0x919 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xb8: 0xff: 0x91a */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xb9: 0xfe: 0x91b */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xba: 0xff: 0x91c */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xbb: 0xfe: 0x91d */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xbc: 0xff: 0x91e */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xbd: 0xff: 0x91f */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xbe: 0xfe: 0x920 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xbf: 0xfe: 0x921 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xc0: 0xfe: 0x922 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xc1: 0xff: 0x923 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xc2: 0xff: 0x924 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xc3: 0xfe: 0x925 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xc4: 0xfe: 0x926 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xc5: 0xfe: 0x927 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xc6: 0xff: 0x928 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xc7: 0x81: 0x929 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.TML_MASK,
        /* 0xc8: 0xff: 0x92a */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xc9: 0xfe: 0x92b */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xca: 0xfe: 0x92c */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xcb: 0xfe: 0x92d */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xcc: 0xfe: 0x92e */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xcd: 0xff: 0x92f */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xcf: 0xff: 0x930 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd0: 0x87: 0x931 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd1: 0xff: 0x932 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd2: 0xb7: 0x933 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd3: 0x83: 0x934 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd4: 0xff: 0x935 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd5: 0xfe: 0x936 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xd6: 0xbf: 0x937 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd7: 0xff: 0x938 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd8: 0xff: 0x939 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0x00: 0x00: 0x93a */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x93b */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xe9: 0xda: 0x93c */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.ZERO + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x93d */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xda: 0xff: 0x93e */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xdb: 0xff: 0x93f */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xdc: 0xff: 0x940 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xdd: 0xff: 0x941 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xde: 0xff: 0x942 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xdf: 0xbe: 0x943 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x944 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ZERO + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xe3: 0x80: 0x945 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xe0: 0x87: 0x946 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xe1: 0xff: 0x947 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xe2: 0xff: 0x948 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xe7: 0x80: 0x949 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xe4: 0x87: 0x94a */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xe5: 0xff: 0x94b */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xe6: 0xff: 0x94c */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xe8: 0xff: 0x94d */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xec: 0x00: 0x94e */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xed: 0x00: 0x94f */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x950 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x951 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x952 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x953 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x954 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x955 */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x956 */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ORI_MASK + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x957 */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x958 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x959 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x95a */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x95b */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x95c */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x95d */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x95e */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xce: 0x98: 0x95f */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x960 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x961 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x962 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x963 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xea: 0xf8: 0x964 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xeaea: 0x00: 0x965 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xf1: 0xff: 0x966 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf2: 0xff: 0x967 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf3: 0xff: 0x968 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf4: 0xff: 0x969 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf5: 0xff: 0x96a */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf6: 0xff: 0x96b */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf7: 0xff: 0x96c */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf8: 0xff: 0x96d */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xf9: 0xff: 0x96e */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xfa: 0xff: 0x96f */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0x00: 0x80: 0x970 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        
        /*
         * The length of the array is 128 to provide values for 0x900..0x97f.
         * The last 15 entries for 0x971..0x97f of the table are all zero
         * because no Indic script uses such Unicode code points.
         */
        
        /* 0x00: 0x00: 0x971 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x972 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x973 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x974 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x975 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x976 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x977 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x978 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x979 */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x97A */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x97B */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x97C */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x97D */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x97E */ MaskEnum.ZERO,
        /* 0x00: 0x00: 0x97F */ MaskEnum.ZERO,
    };
    
    private static final char fromUnicodeTable[] = {
      0x00a0, /* 0x0900 */ 
      0x00a1, /* 0x0901 */
      0x00a2, /* 0x0902 */
      0x00a3, /* 0x0903 */
      0xa4e0, /* 0x0904 */
      0x00a4, /* 0x0905 */
      0x00a5, /* 0x0906 */
      0x00a6, /* 0x0907 */
      0x00a7, /* 0x0908 */ 
      0x00a8, /* 0x0909 */
      0x00a9, /* 0x090a */
      0x00aa, /* 0x090b */
      0xA6E9, /* 0x090c */
      0x00ae, /* 0x090d */
      0x00ab, /* 0x090e */
      0x00ac, /* 0x090f */
      0x00ad, /* 0x0910 */ 
      0x00b2, /* 0x0911 */
      0x00af, /* 0x0912 */
      0x00b0, /* 0x0913 */
      0x00b1, /* 0x0914 */
      0x00b3, /* 0x0915 */
      0x00b4, /* 0x0916 */
      0x00b5, /* 0x0917 */
      0x00b6, /* 0x0918 */ 
      0x00b7, /* 0x0919 */
      0x00b8, /* 0x091a */
      0x00b9, /* 0x091b */
      0x00ba, /* 0x091c */
      0x00bb, /* 0x091d */
      0x00bc, /* 0x091e */
      0x00bd, /* 0x091f */
      0x00be, /* 0x0920 */ 
      0x00bf, /* 0x0921 */
      0x00c0, /* 0x0922 */
      0x00c1, /* 0x0923 */
      0x00c2, /* 0x0924 */
      0x00c3, /* 0x0925 */
      0x00c4, /* 0x0926 */
      0x00c5, /* 0x0927 */
      0x00c6, /* 0x0928 */ 
      0x00c7, /* 0x0929 */
      0x00c8, /* 0x092a */
      0x00c9, /* 0x092b */
      0x00ca, /* 0x092c */
      0x00cb, /* 0x092d */
      0x00cc, /* 0x092e */
      0x00cd, /* 0x092f */
      0x00cf, /* 0x0930 */ 
      0x00d0, /* 0x0931 */
      0x00d1, /* 0x0932 */
      0x00d2, /* 0x0933 */
      0x00d3, /* 0x0934 */
      0x00d4, /* 0x0935 */
      0x00d5, /* 0x0936 */
      0x00d6, /* 0x0937 */
      0x00d7, /* 0x0938 */ 
      0x00d8, /* 0x0939 */
      0xFFFF, /* 0x093a */
      0xFFFF, /* 0x093b */
      0x00e9, /* 0x093c */
      0xEAE9, /* 0x093d */
      0x00da, /* 0x093e */
      0x00db, /* 0x093f */
      0x00dc, /* 0x0940 */ 
      0x00dd, /* 0x0941 */
      0x00de, /* 0x0942 */
      0x00df, /* 0x0943 */
      0xDFE9, /* 0x0944 */
      0x00e3, /* 0x0945 */
      0x00e0, /* 0x0946 */
      0x00e1, /* 0x0947 */
      0x00e2, /* 0x0948 */ 
      0x00e7, /* 0x0949 */
      0x00e4, /* 0x094a */
      0x00e5, /* 0x094b */
      0x00e6, /* 0x094c */
      0x00e8, /* 0x094d */
      0x00ec, /* 0x094e */
      0x00ed, /* 0x094f */
      0xA1E9, /* 0x0950 */ /* OM Symbol */ 
      0xFFFF, /* 0x0951 */
      0xF0B8, /* 0x0952 */
      0xFFFF, /* 0x0953 */
      0xFFFF, /* 0x0954 */
      0xFFFF, /* 0x0955 */
      0xFFFF, /* 0x0956 */
      0xFFFF, /* 0x0957 */
      0xb3e9, /* 0x0958 */ 
      0xb4e9, /* 0x0959 */
      0xb5e9, /* 0x095a */
      0xbae9, /* 0x095b */
      0xbfe9, /* 0x095c */
      0xC0E9, /* 0x095d */
      0xc9e9, /* 0x095e */
      0x00ce, /* 0x095f */
      0xAAe9, /* 0x0960 */ 
      0xA7E9, /* 0x0961 */
      0xDBE9, /* 0x0962 */
      0xDCE9, /* 0x0963 */
      0x00ea, /* 0x0964 */
      0xeaea, /* 0x0965 */
      0x00f1, /* 0x0966 */
      0x00f2, /* 0x0967 */
      0x00f3, /* 0x0968 */ 
      0x00f4, /* 0x0969 */
      0x00f5, /* 0x096a */
      0x00f6, /* 0x096b */
      0x00f7, /* 0x096c */
      0x00f8, /* 0x096d */
      0x00f9, /* 0x096e */
      0x00fa, /* 0x096f */
      0xF0BF, /* 0x0970 */ 
      0xFFFF, /* 0x0971 */
      0xFFFF, /* 0x0972 */
      0xFFFF, /* 0x0973 */
      0xFFFF, /* 0x0974 */
      0xFFFF, /* 0x0975 */
      0xFFFF, /* 0x0976 */
      0xFFFF, /* 0x0977 */
      0xFFFF, /* 0x0978 */ 
      0xFFFF, /* 0x0979 */
      0xFFFF, /* 0x097a */
      0xFFFF, /* 0x097b */
      0xFFFF, /* 0x097c */
      0xFFFF, /* 0x097d */
      0xFFFF, /* 0x097e */
      0xFFFF, /* 0x097f */
    };
    private static final char toUnicodeTable[] = {
        0x0000, /* 0x00 */
        0x0001, /* 0x01 */
        0x0002, /* 0x02 */
        0x0003, /* 0x03 */
        0x0004, /* 0x04 */
        0x0005, /* 0x05 */
        0x0006, /* 0x06 */
        0x0007, /* 0x07 */
        0x0008, /* 0x08 */
        0x0009, /* 0x09 */
        0x000a, /* 0x0a */
        0x000b, /* 0x0b */
        0x000c, /* 0x0c */
        0x000d, /* 0x0d */
        0x000e, /* 0x0e */
        0x000f, /* 0x0f */
        0x0010, /* 0x10 */
        0x0011, /* 0x11 */
        0x0012, /* 0x12 */
        0x0013, /* 0x13 */
        0x0014, /* 0x14 */
        0x0015, /* 0x15 */
        0x0016, /* 0x16 */
        0x0017, /* 0x17 */
        0x0018, /* 0x18 */
        0x0019, /* 0x19 */
        0x001a, /* 0x1a */
        0x001b, /* 0x1b */
        0x001c, /* 0x1c */
        0x001d, /* 0x1d */
        0x001e, /* 0x1e */
        0x001f, /* 0x1f */
        0x0020, /* 0x20 */
        0x0021, /* 0x21 */
        0x0022, /* 0x22 */
        0x0023, /* 0x23 */
        0x0024, /* 0x24 */
        0x0025, /* 0x25 */
        0x0026, /* 0x26 */
        0x0027, /* 0x27 */
        0x0028, /* 0x28 */
        0x0029, /* 0x29 */
        0x002a, /* 0x2a */
        0x002b, /* 0x2b */
        0x002c, /* 0x2c */
        0x002d, /* 0x2d */
        0x002e, /* 0x2e */
        0x002f, /* 0x2f */
        0x0030, /* 0x30 */
        0x0031, /* 0x31 */
        0x0032, /* 0x32 */
        0x0033, /* 0x33 */
        0x0034, /* 0x34 */
        0x0035, /* 0x35 */
        0x0036, /* 0x36 */
        0x0037, /* 0x37 */
        0x0038, /* 0x38 */
        0x0039, /* 0x39 */
        0x003A, /* 0x3A */
        0x003B, /* 0x3B */
        0x003c, /* 0x3c */
        0x003d, /* 0x3d */
        0x003e, /* 0x3e */
        0x003f, /* 0x3f */
        0x0040, /* 0x40 */
        0x0041, /* 0x41 */
        0x0042, /* 0x42 */
        0x0043, /* 0x43 */
        0x0044, /* 0x44 */
        0x0045, /* 0x45 */
        0x0046, /* 0x46 */
        0x0047, /* 0x47 */
        0x0048, /* 0x48 */
        0x0049, /* 0x49 */
        0x004a, /* 0x4a */
        0x004b, /* 0x4b */
        0x004c, /* 0x4c */
        0x004d, /* 0x4d */
        0x004e, /* 0x4e */
        0x004f, /* 0x4f */
        0x0050, /* 0x50 */
        0x0051, /* 0x51 */
        0x0052, /* 0x52 */
        0x0053, /* 0x53 */
        0x0054, /* 0x54 */
        0x0055, /* 0x55 */
        0x0056, /* 0x56 */
        0x0057, /* 0x57 */
        0x0058, /* 0x58 */
        0x0059, /* 0x59 */
        0x005a, /* 0x5a */
        0x005b, /* 0x5b */
        0x005c, /* 0x5c */
        0x005d, /* 0x5d */
        0x005e, /* 0x5e */
        0x005f, /* 0x5f */
        0x0060, /* 0x60 */
        0x0061, /* 0x61 */
        0x0062, /* 0x62 */
        0x0063, /* 0x63 */
        0x0064, /* 0x64 */
        0x0065, /* 0x65 */
        0x0066, /* 0x66 */
        0x0067, /* 0x67 */
        0x0068, /* 0x68 */
        0x0069, /* 0x69 */
        0x006a, /* 0x6a */
        0x006b, /* 0x6b */
        0x006c, /* 0x6c */
        0x006d, /* 0x6d */
        0x006e, /* 0x6e */
        0x006f, /* 0x6f */
        0x0070, /* 0x70 */
        0x0071, /* 0x71 */
        0x0072, /* 0x72 */
        0x0073, /* 0x73 */
        0x0074, /* 0x74 */
        0x0075, /* 0x75 */
        0x0076, /* 0x76 */
        0x0077, /* 0x77 */
        0x0078, /* 0x78 */
        0x0079, /* 0x79 */
        0x007a, /* 0x7a */
        0x007b, /* 0x7b */
        0x007c, /* 0x7c */
        0x007d, /* 0x7d */
        0x007e, /* 0x7e */
        0x007f, /* 0x7f */
        0x0080, /* 0x80 */
        0x0081, /* 0x81 */
        0x0082, /* 0x82 */
        0x0083, /* 0x83 */
        0x0084, /* 0x84 */
        0x0085, /* 0x85 */
        0x0086, /* 0x86 */
        0x0087, /* 0x87 */
        0x0088, /* 0x88 */
        0x0089, /* 0x89 */
        0x008a, /* 0x8a */
        0x008b, /* 0x8b */
        0x008c, /* 0x8c */
        0x008d, /* 0x8d */
        0x008e, /* 0x8e */
        0x008f, /* 0x8f */
        0x0090, /* 0x90 */
        0x0091, /* 0x91 */
        0x0092, /* 0x92 */
        0x0093, /* 0x93 */
        0x0094, /* 0x94 */
        0x0095, /* 0x95 */
        0x0096, /* 0x96 */
        0x0097, /* 0x97 */
        0x0098, /* 0x98 */
        0x0099, /* 0x99 */
        0x009a, /* 0x9a */
        0x009b, /* 0x9b */
        0x009c, /* 0x9c */
        0x009d, /* 0x9d */
        0x009e, /* 0x9e */
        0x009f, /* 0x9f */
        0x00A0, /* 0xa0 */
        0x0901, /* 0xa1 */
        0x0902, /* 0xa2 */
        0x0903, /* 0xa3 */
        0x0905, /* 0xa4 */
        0x0906, /* 0xa5 */
        0x0907, /* 0xa6 */
        0x0908, /* 0xa7 */
        0x0909, /* 0xa8 */
        0x090a, /* 0xa9 */
        0x090b, /* 0xaa */
        0x090e, /* 0xab */
        0x090f, /* 0xac */
        0x0910, /* 0xad */
        0x090d, /* 0xae */
        0x0912, /* 0xaf */
        0x0913, /* 0xb0 */
        0x0914, /* 0xb1 */
        0x0911, /* 0xb2 */
        0x0915, /* 0xb3 */
        0x0916, /* 0xb4 */
        0x0917, /* 0xb5 */
        0x0918, /* 0xb6 */
        0x0919, /* 0xb7 */
        0x091a, /* 0xb8 */
        0x091b, /* 0xb9 */
        0x091c, /* 0xba */
        0x091d, /* 0xbb */
        0x091e, /* 0xbc */
        0x091f, /* 0xbd */
        0x0920, /* 0xbe */
        0x0921, /* 0xbf */
        0x0922, /* 0xc0 */
        0x0923, /* 0xc1 */
        0x0924, /* 0xc2 */
        0x0925, /* 0xc3 */
        0x0926, /* 0xc4 */
        0x0927, /* 0xc5 */
        0x0928, /* 0xc6 */
        0x0929, /* 0xc7 */
        0x092a, /* 0xc8 */
        0x092b, /* 0xc9 */
        0x092c, /* 0xca */
        0x092d, /* 0xcb */
        0x092e, /* 0xcc */
        0x092f, /* 0xcd */
        0x095f, /* 0xce */
        0x0930, /* 0xcf */
        0x0931, /* 0xd0 */
        0x0932, /* 0xd1 */
        0x0933, /* 0xd2 */
        0x0934, /* 0xd3 */
        0x0935, /* 0xd4 */
        0x0936, /* 0xd5 */
        0x0937, /* 0xd6 */
        0x0938, /* 0xd7 */
        0x0939, /* 0xd8 */
        0x200D, /* 0xd9 */
        0x093e, /* 0xda */
        0x093f, /* 0xdb */
        0x0940, /* 0xdc */
        0x0941, /* 0xdd */
        0x0942, /* 0xde */
        0x0943, /* 0xdf */
        0x0946, /* 0xe0 */
        0x0947, /* 0xe1 */
        0x0948, /* 0xe2 */
        0x0945, /* 0xe3 */
        0x094a, /* 0xe4 */
        0x094b, /* 0xe5 */
        0x094c, /* 0xe6 */
        0x0949, /* 0xe7 */
        0x094d, /* 0xe8 */
        0x093c, /* 0xe9 */
        0x0964, /* 0xea */
        0xFFFF, /* 0xeb */
        0xFFFF, /* 0xec */
        0xFFFF, /* 0xed */
        0xFFFF, /* 0xee */
        0xFFFF, /* 0xef */
        0xFFFF, /* 0xf0 */
        0x0966, /* 0xf1 */
        0x0967, /* 0xf2 */
        0x0968, /* 0xf3 */
        0x0969, /* 0xf4 */
        0x096a, /* 0xf5 */
        0x096b, /* 0xf6 */
        0x096c, /* 0xf7 */
        0x096d, /* 0xf8 */
        0x096e, /* 0xf9 */
        0x096f, /* 0xfa */
        0xFFFF, /* 0xfb */
        0xFFFF, /* 0xfc */
        0xFFFF, /* 0xfd */
        0xFFFF, /* 0xfe */
        0xFFFF, /* 0xff */
    };
    private static final char nuktaSpecialCases[][] = {
        { 16 /* length of array */ , 0 },
        { 0xa6, 0x090c },
        { 0xea, 0x093d },
        { 0xdf, 0x0944 },
        { 0xa1, 0x0950 },
        { 0xb3, 0x0958 },
        { 0xb4, 0x0959 },
        { 0xb5, 0x095a },
        { 0xba, 0x095b },
        { 0xbf, 0x095c },
        { 0xc0, 0x095d },
        { 0xc9, 0x095e },
        { 0xaa, 0x0960 },
        { 0xa7, 0x0961 },
        { 0xdb, 0x0962 },
        { 0xdc, 0x0963 }
    };
    private static final char vowelSignESpecialCases[][] = {
        { 2 /* length of array */ , 0 },
        { 0xA4, 0x0904 }
    };
    
    private static final short lookupTable[][] = {
        { MaskEnum.ZERO, MaskEnum.ZERO }, /* DEFAULT */
        { MaskEnum.ZERO, MaskEnum.ZERO }, /* ROMAN */
        { UniLang.DEVALANGARI, MaskEnum.DEV_MASK },
        { UniLang.BENGALI, MaskEnum.BNG_MASK },
        { UniLang.TAMIL, MaskEnum.TML_MASK },
        { UniLang.TELUGU, MaskEnum.KND_MASK },
        { UniLang.BENGALI, MaskEnum.BNG_MASK },
        { UniLang.ORIYA, MaskEnum.ORI_MASK },
        { UniLang.KANNADA, MaskEnum.KND_MASK },
        { UniLang.MALAYALAM, MaskEnum.MLM_MASK },
        { UniLang.GUJARATI, MaskEnum.GJR_MASK },
        { UniLang.GURMUKHI, MaskEnum.PNJ_MASK }
    };
    
    private UConverterDataISCII extraInfo = null;
    protected byte[] fromUSubstitution = new byte[]{(byte)0x1A};
    
    public CharsetISCII(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = 4; 
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
        //get the version number of the ISCII converter
        int option = Integer.parseInt(icuCanonicalName.substring(14));
        
        extraInfo = new UConverterDataISCII( 
                            option,
                            new String(ISCII_CNV_PREFIX + (option & UCNV_OPTIONS_VERSION_MASK))  /* name */
                        );
        
        initializePNJSets();
    }
    
    /* Initialize the two UnicodeSets use for proper Gurmukhi conversion if they have not already been created. */
    private void initializePNJSets() {
        if (PNJ_BINDI_TIPPI_SET != null && PNJ_CONSONANT_SET != null) {
            return;
        }
        PNJ_BINDI_TIPPI_SET = new UnicodeSet();
        PNJ_CONSONANT_SET = new UnicodeSet();
        
        PNJ_CONSONANT_SET.add(0x0a15, 0x0a28);
        PNJ_CONSONANT_SET.add(0x0a2a, 0x0a30);
        PNJ_CONSONANT_SET.add(0x0a35, 0x0a36);
        PNJ_CONSONANT_SET.add(0x0a38, 0x0a39);
        
        PNJ_BINDI_TIPPI_SET.addAll(PNJ_CONSONANT_SET);
        PNJ_BINDI_TIPPI_SET.add(0x0a05);
        PNJ_BINDI_TIPPI_SET.add(0x0a07);
        
        PNJ_BINDI_TIPPI_SET.add(0x0a41, 0x0a42);
        PNJ_BINDI_TIPPI_SET.add(0x0a3f);
        
        PNJ_CONSONANT_SET.compact();
        PNJ_BINDI_TIPPI_SET.compact();
    }
    
    /*
     * Rules for ISCII to Unicode converter
     * ISCII is a stateful encoding. To convert ISCII bytes to Unicode,
     * which is both precomposed and decomposed from characters
     * pre-context and post-context need to be considered.
     * 
     * Post context
     * i) ATR : Attribute code is used to declare the font and script switching.
     *    Currently we only switch scripts and font codes consumed without generating an error
     * ii) EXT : Extention code is used to declare switching to Sanskrit and for obscure,
     *     obsolete characters
     * Pre context
     * i) Halant: if preceeded by a halant then it is a explicit halant
     * ii) Nukta:
     *     a) if preceeded by a halant then it is a soft halant
     *     b) if preceeded by specific consonants and the ligatures have pre-composed
     *        characters in Unicode then convert to pre-composed characters
     * iii) Danda: If Danda is preceeded by a Danda then convert to Double Danda 
     */
    class CharsetDecoderISCII extends CharsetDecoderICU {
        public CharsetDecoderISCII(CharsetICU cs) {
            super(cs);
            implReset();
        }
    
        protected void implReset() {
            super.implReset();
            this.toUnicodeStatus = 0xFFFF;
            extraInfo.initialize();
        }

        @SuppressWarnings("fallthrough")
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) { 
            CoderResult cr = CoderResult.UNDERFLOW;
            int targetUniChar = 0x0000;
            short sourceChar = 0x0000;
            UConverterDataISCII data;
            boolean gotoCallBack = false;
            int offset = 0;
            
            data = extraInfo;
            //data.contextCharToUnicode; /* contains previous ISCII codepoint visited */
            //this.toUnicodeStatus; /* contains the mapping to Unicode of the above codepoint */
            
            while (source.hasRemaining()) {
                targetUniChar = UConverterConstants.missingCharMarker;
                
                if (target.hasRemaining()) {
                    sourceChar = (short)((short)source.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
                    
                    /* look at the post-context perform special processing */
                    if (data.contextCharToUnicode == ATR) {
                        /* If we have ATR in data.contextCharToUnicode then we need to change our
                         * state to Indic Script specified by sourceChar
                         */
                        /* check if the sourceChar is supported script range */
                        if (((short)(ISCIILang.PNJ - sourceChar) & UConverterConstants.UNSIGNED_BYTE_MASK) <= (ISCIILang.PNJ - ISCIILang.DEV)) {
                            data.currentDeltaToUnicode = (short)(lookupTable[sourceChar & 0x0F][0] * UniLang.DELTA);
                            data.currentMaskToUnicode = lookupTable[sourceChar & 0x0F][1];
                        } else if (sourceChar == ISCIILang.DEF) {
                            /* switch back to default */
                            data.currentDeltaToUnicode = data.defDeltaToUnicode;
                            data.currentMaskToUnicode = data.defMaskToUnicode;
                        } else {
                            if ((sourceChar >= 0x21 && sourceChar <= 0x3F)) {
                                /* these are display codes consume and continue */
                            } else {
                                cr = CoderResult.malformedForLength(1);
                                /* reset */
                                data.contextCharToUnicode = NO_CHAR_MARKER;
                                gotoCallBack = true;
                            }
                        }
                        /* reset */
                        if (!gotoCallBack) {
                            data.contextCharToUnicode = NO_CHAR_MARKER;
                            continue;
                        }
                    } else if (data.contextCharToUnicode == EXT) {
                        /* check if sourceChar is in 0xA1 - 0xEE range */
                        if (((short)(EXT_RANGE_END - sourceChar) & UConverterConstants.UNSIGNED_BYTE_MASK) <= (EXT_RANGE_END - EXT_RANGE_BEGIN)) {
                            /* We currently support only Anudatta and Devanagari abbreviation sign */
                            if (sourceChar == 0xBF || sourceChar == 0xB8) {
                                targetUniChar = (sourceChar == 0xBF) ? DEV_ABBR_SIGN : DEV_ANUDATTA;
                                
                                /* find out if the mappling is valid in this state */
                                if ((validityTable[((short)targetUniChar) & UConverterConstants.UNSIGNED_BYTE_MASK] & data.currentMaskToUnicode) > 0) {
                                    data.contextCharToUnicode = NO_CHAR_MARKER;
                                    
                                    /* Write the previous toUnicodeStatus, this was delayed to handle consonant clustering for Gurmukhi script. */
                                    if (data.prevToUnicodeStatus != 0) {
                                        cr = WriteToTargetToU(offsets, (source.position() - 1), source, target, data.prevToUnicodeStatus, (short)0);
                                        data.prevToUnicodeStatus = 0x0000;
                                    }
                                    /* write to target */
                                    cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, targetUniChar, data.currentDeltaToUnicode);
                                    
                                    continue;
                                }
                            }
                            /* byte unit is unassigned */
                            targetUniChar = UConverterConstants.missingCharMarker;
                            cr = CoderResult.unmappableForLength(1);
                        } else {
                            /* only 0xA1 - 0xEE are legal after EXT char */
                            data.contextCharToUnicode = NO_CHAR_MARKER;
                            cr = CoderResult.malformedForLength(1); 
                        }
                        gotoCallBack = true;
                    } else if (data.contextCharToUnicode == ISCII_INV) {
                        if (sourceChar == ISCII_HALANT) {
                            targetUniChar = 0x0020; /* replace with space according to Indic FAQ */
                        } else {
                            targetUniChar = ZWJ;
                        }
                        
                        /* Write the previous toUnicodeStatus, this was delayed to handle consonant clustering for Gurmukhi script. */
                        if (data.prevToUnicodeStatus != 0) {
                            cr = WriteToTargetToU(offsets, (source.position() - 1), source, target, data.prevToUnicodeStatus, (short)0);
                            data.prevToUnicodeStatus = 0x0000;
                        }
                        
                        /* write to target */
                        cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, targetUniChar, data.currentDeltaToUnicode);
                        /* reset */
                        data.contextCharToUnicode = NO_CHAR_MARKER;
                    }
                    
                    /* look at the pre-context and perform special processing */
                    if (!gotoCallBack) {
                        switch (sourceChar) {
                        case ISCII_INV:
                        case EXT: /* falls through */
                        case ATR:
                            data.contextCharToUnicode = (char)sourceChar;
                            
                            if (this.toUnicodeStatus != UConverterConstants.missingCharMarker) {
                                /* Write the previous toUnicodeStatus, this was delayed to handle consonant clustering for Gurmukhi script. */
                                if (data.prevToUnicodeStatus != 0) {
                                    cr = WriteToTargetToU(offsets, (source.position() - 1), source, target, data.prevToUnicodeStatus, (short)0);
                                    data.prevToUnicodeStatus = 0x0000;
                                }
                                cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, this.toUnicodeStatus, data.currentDeltaToUnicode); 
                                this.toUnicodeStatus = UConverterConstants.missingCharMarker;
                            }
                            continue;
                        case ISCII_DANDA:
                            /* handle double danda */
                            if (data.contextCharToUnicode == ISCII_DANDA) {
                                targetUniChar = DOUBLE_DANDA;
                                /* clear the context */
                                data.contextCharToUnicode = NO_CHAR_MARKER;
                                this.toUnicodeStatus = UConverterConstants.missingCharMarker;
                            } else {
                                targetUniChar = GetMapping(sourceChar, targetUniChar, data);
                                data.contextCharToUnicode = (char)sourceChar;
                            }
                            break;
                        case ISCII_HALANT:
                            /* handle explicit halant */
                            if (data.contextCharToUnicode == ISCII_HALANT) {
                                targetUniChar = ZWNJ;
                                /* clear context */
                                data.contextCharToUnicode = NO_CHAR_MARKER;
                            } else {
                                targetUniChar = GetMapping(sourceChar, targetUniChar, data);
                                data.contextCharToUnicode = (char)sourceChar;
                            }
                            break;
                        case 0x0A:
                            /* fall through */
                        case 0x0D:
                            data.resetToDefaultToUnicode = true;
                            targetUniChar = GetMapping(sourceChar, targetUniChar, data);
                            data.contextCharToUnicode = (char)sourceChar;
                            break;
                        case ISCII_VOWEL_SIGN_E:
                            /* find <CHAR> + SIGN_VOWEL_E special mapping */
                            int n = 1;
                            boolean find = false;
                            for (; n < vowelSignESpecialCases[0][0]; n++) {
                                if (vowelSignESpecialCases[n][0] == ((short)data.contextCharToUnicode & UConverterConstants.UNSIGNED_BYTE_MASK)) {
                                    targetUniChar = vowelSignESpecialCases[n][1];
                                    find = true;
                                    break;
                                }
                            }
                            if (find) {
                                /* find out if the mapping is valid in this state */
                                if ((validityTable[(byte)targetUniChar] & data.currentMaskFromUnicode) > 0) {
                                    data.contextCharToUnicode = NO_CHAR_MARKER;
                                    this.toUnicodeStatus = UConverterConstants.missingCharMarker;
                                    break;
                                }
                            }
                            targetUniChar = GetMapping(sourceChar, targetUniChar, data);
                            data.contextCharToUnicode = (char)sourceChar;
                            break;                         
                        case ISCII_NUKTA:
                            /* handle soft halant */
                            if (data.contextCharToUnicode == ISCII_HALANT) {
                                targetUniChar = ZWJ;
                                /* clear the context */
                                data.contextCharToUnicode = NO_CHAR_MARKER;
                                break;
                            } else if (data.currentDeltaToUnicode == PNJ_DELTA && data.contextCharToUnicode == 0xc0) {
                                /* We got here because ISCII_NUKTA was preceded by 0xc0 and we are converting Gurmukhi.
                                 * In that case we must convert (0xc0 0xe9) to (\u0a5c\u0a4d\u0a39).
                                 * WriteToTargetToU is given 0x095c instead of 0xa5c because that method will automatically
                                 * convert the code point given based on the delta provided.
                                 */
                                cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, PNJ_RRA, (short)0);
                                if (!cr.isOverflow()) {
                                    cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, PNJ_SIGN_VIRAMA, (short)0);
                                    if (!cr.isOverflow()) {
                                        cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, PNJ_HA, (short)0);
                                    } else {
                                        this.charErrorBufferArray[this.charErrorBufferLength++] = PNJ_HA;
                                    }
                                } else {
                                    this.charErrorBufferArray[this.charErrorBufferLength++] = PNJ_SIGN_VIRAMA;
                                    this.charErrorBufferArray[this.charErrorBufferLength++] = PNJ_HA;
                                }
                                this.toUnicodeStatus = UConverterConstants.missingCharMarker;
                                data.contextCharToUnicode = NO_CHAR_MARKER;
                                if (!cr.isError()) {
                                    continue;
                                }
                                break;
                            } else {
                                /* try to handle <CHAR> + ISCII_NUKTA special mappings */
                                int i = 1;
                                boolean found = false;
                                for (; i < nuktaSpecialCases[0][0]; i++) {
                                    if (nuktaSpecialCases[i][0] == ((short)data.contextCharToUnicode & UConverterConstants.UNSIGNED_BYTE_MASK)) {
                                        targetUniChar  = nuktaSpecialCases[i][1];
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) {
                                    /* find out if the mapping is valid in this state */
                                    if ((validityTable[(byte)targetUniChar] & data.currentMaskToUnicode) > 0) {
                                        data.contextCharToUnicode = NO_CHAR_MARKER;
                                        this.toUnicodeStatus = UConverterConstants.missingCharMarker;
                                        if (data.currentDeltaToUnicode == PNJ_DELTA) {
                                            /* Write the previous toUnicodeStatus, this was delayed to handle consonant clustering for Gurmukhi script. */
                                            if (data.prevToUnicodeStatus != 0) {
                                                cr = WriteToTargetToU(offsets, (source.position() - 1), source, target, data.prevToUnicodeStatus, (short)0);
                                                data.prevToUnicodeStatus = 0x0000;
                                            }
                                            cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, targetUniChar, data.currentDeltaToUnicode);
                                            continue;
                                        }
                                        break;
                                    }
                                    /* else fall through to default */
                                }
                                /* else fall through to default */
                            }
                        
                        default:
                            targetUniChar = GetMapping(sourceChar, targetUniChar, data);
                            data.contextCharToUnicode = (char)sourceChar;
                            break;
                        } //end of switch
                    }//end of CallBack if statement
                    
                    if (!gotoCallBack && this.toUnicodeStatus != UConverterConstants.missingCharMarker) {
                        /* Check to make sure that consonant clusters are handled correctly for Gurmukhi script. */
                        if (data.currentDeltaToUnicode == PNJ_DELTA && data.prevToUnicodeStatus != 0 && PNJ_CONSONANT_SET.contains(data.prevToUnicodeStatus) &&
                                (this.toUnicodeStatus + PNJ_DELTA) == PNJ_SIGN_VIRAMA && (targetUniChar + PNJ_DELTA) == data.prevToUnicodeStatus) {
                            if (offsets != null) {
                                offset = source.position() - 3;
                            }
                            cr = WriteToTargetToU(offsets, offset, source, target, PNJ_ADHAK, (short)0);
                            cr = WriteToTargetToU(offsets, offset, source, target, data.prevToUnicodeStatus, (short)0);
                            data.prevToUnicodeStatus = 0x0000; /* reset the previous unicode code point */
                            toUnicodeStatus = UConverterConstants.missingCharMarker;
                            continue;
                        } else {
                            /* Write the previous toUnicodeStatus, this was delayed to handle consonant clustering for Gurmukhi script. */
                            if (data.prevToUnicodeStatus != 0) {
                                cr = WriteToTargetToU(offsets, (source.position() - 1), source, target, data.prevToUnicodeStatus, (short)0);
                                data.prevToUnicodeStatus = 0x0000;
                            }
                            /* Check to make sure that Bindi and Tippi are handled correctly for Gurmukhi script. 
                             * If 0xA2 is preceded by a codepoint in the PNJ_BINDI_TIPPI_SET then the target codepoint should be Tippi instead of Bindi.
                             */
                            if (data.currentDeltaToUnicode == PNJ_DELTA  && (targetUniChar + PNJ_DELTA) == PNJ_BINDI && PNJ_BINDI_TIPPI_SET.contains(this.toUnicodeStatus + PNJ_DELTA)) {
                                targetUniChar = PNJ_TIPPI - PNJ_DELTA;
                                cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, this.toUnicodeStatus, PNJ_DELTA);
                            } else if (data.currentDeltaToUnicode == PNJ_DELTA && (targetUniChar + PNJ_DELTA) == PNJ_SIGN_VIRAMA && PNJ_CONSONANT_SET.contains(this.toUnicodeStatus + PNJ_DELTA)) {
                                /* Store the current toUnicodeStatus code point for later handling of consonant cluster in Gurmukhi. */
                                data.prevToUnicodeStatus = this.toUnicodeStatus + PNJ_DELTA;
                            } else {
                                /* write the previously mapped codepoint */
                                cr = WriteToTargetToU(offsets, (source.position() - 2), source, target, this.toUnicodeStatus, data.currentDeltaToUnicode);
                            }
                        }
                        this.toUnicodeStatus = UConverterConstants.missingCharMarker;
                    }
                    
                    if (!gotoCallBack && targetUniChar != UConverterConstants.missingCharMarker) {
                        /* now save the targetUniChar for delayed write */
                        this.toUnicodeStatus = (char)targetUniChar;
                        if (data.resetToDefaultToUnicode) {
                            data.currentDeltaToUnicode = data.defDeltaToUnicode;
                            data.currentMaskToUnicode = data.defMaskToUnicode;
                            data.resetToDefaultToUnicode = false;
                        }
                    } else {
                        /* we reach here only if targetUniChar == missingCharMarker
                         * so assign codes to reason and err
                         */
                        if (!gotoCallBack) {
                            cr = CoderResult.unmappableForLength(1);
                        }
//CallBack :
                        toUBytesArray[0] = (byte)sourceChar;
                        toULength = 1;
                        gotoCallBack = false;
                        break;
                    }
                } else {
                    cr = CoderResult.OVERFLOW;
                    break;
                }
                
            } //end of while
            
            if (cr.isUnderflow() && flush && !source.hasRemaining()) {
                /*end of the input stream */
                if (data.contextCharToUnicode == ATR || data.contextCharToUnicode == EXT || data.contextCharToUnicode == ISCII_INV) {
                    /* set toUBytes[] */
                    toUBytesArray[0] = (byte)data.contextCharToUnicode;
                    toULength = 1;
                    
                    /* avoid looping on truncated sequences */
                    data.contextCharToUnicode = NO_CHAR_MARKER;
                } else {
                    toULength = 0;
                }
                
                if (this.toUnicodeStatus != UConverterConstants.missingCharMarker) {
                    /* output a remaining target character */
                    WriteToTargetToU(offsets, (source.position() - 2), source, target, this.toUnicodeStatus, data.currentDeltaToUnicode);
                    this.toUnicodeStatus = UConverterConstants.missingCharMarker;    
                }
            }
            return cr;
        }
        
        private CoderResult WriteToTargetToU(IntBuffer offsets, int offset, ByteBuffer source, CharBuffer target, int targetUniChar, short delta) {
            CoderResult cr = CoderResult.UNDERFLOW;
            /* add offset to current Indic Block */
            if (targetUniChar > ASCII_END &&
                    targetUniChar != ZWJ &&
                    targetUniChar != ZWNJ &&
                    targetUniChar != DANDA &&
                    targetUniChar != DOUBLE_DANDA) {
                targetUniChar += delta;
            }
            
            /* now write the targetUniChar */
            if (target.hasRemaining()) {
                target.put((char)targetUniChar);
                if (offsets != null) {
                    offsets.put(offset);
                }
            } else {
                charErrorBufferArray[charErrorBufferLength++] = (char)targetUniChar;
                cr = CoderResult.OVERFLOW;
            }
            return cr;
        }
        
        private int GetMapping(short sourceChar, int targetUniChar, UConverterDataISCII data) {
            targetUniChar = toUnicodeTable[sourceChar];
            /* is the code point valid in current script? */
            if (sourceChar > ASCII_END &&
                    (validityTable[(short)targetUniChar & UConverterConstants.UNSIGNED_BYTE_MASK] & data.currentMaskToUnicode) == 0) {
                /* Vocallic RR is assigne in ISCII Telugu and Unicode */
                if (data.currentDeltaToUnicode != (TELUGU_DELTA) || targetUniChar != VOCALLIC_RR) {
                    targetUniChar = UConverterConstants.missingCharMarker;
                }
            }
            return targetUniChar;
        }
    }
    
    /*
     * Rules:
     *   Explicit Halant :
     *     <HALANT> + <ZWNJ>
     *   Soft Halant :
     *     <HALANT> + <ZWJ>
     */
    class CharsetEncoderISCII extends CharsetEncoderICU {
        public CharsetEncoderISCII(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }
        
        protected void implReset() {
            super.implReset();
            extraInfo.initialize();
        }
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            int targetByteUnit = 0x0000;
            int sourceChar = 0x0000;
            UConverterDataISCII converterData;
            short newDelta = 0;
            short range = 0;
            boolean deltaChanged = false;
            int tempContextFromUnicode = 0x0000;    /* For special handling of the Gurmukhi script. */
            CoderResult cr = CoderResult.UNDERFLOW;
            
            /* initialize data */
            converterData = extraInfo;
            newDelta = converterData.currentDeltaFromUnicode;
            range = (short)(newDelta / UniLang.DELTA);
            
            if ((sourceChar = fromUChar32) != 0) {
                cr = handleSurrogates(source, (char) sourceChar);
                return (cr != null) ? cr : CoderResult.unmappableForLength(2);
            }
            
            /* writing the char to the output stream */
            while (source.hasRemaining()) {
                if (!target.hasRemaining()) {
                    return CoderResult.OVERFLOW;
                }
                
                /* Write the language code following LF only if LF is not the last character. */
                if (fromUnicodeStatus == LF) {
                    targetByteUnit = ATR << 8;
                    targetByteUnit += (byte)lookupInitialData[range].isciiLang;
                    fromUnicodeStatus = 0x0000;
                    /* now append ATR and language code */
                    cr = WriteToTargetFromU(offsets, source, target, targetByteUnit);
                    if (cr.isOverflow()) {
                        break;
                    }
                }
                
                sourceChar = source.get();
                tempContextFromUnicode = converterData.contextCharFromUnicode;
                
                targetByteUnit = UConverterConstants.missingCharMarker;
                
                /* check if input is in ASCII and C0 control codes range */
                if (sourceChar <= ASCII_END) {
                    fromUnicodeStatus = sourceChar;
                    cr = WriteToTargetFromU(offsets, source, target, sourceChar);
                    if (cr.isOverflow()) {
                        break;
                    }
                    continue;
                }
                
                switch (sourceChar) {
                case ZWNJ:
                    /* contextChar has HALANT */
                    if (converterData.contextCharFromUnicode != 0) {
                        converterData.contextCharFromUnicode = 0x00;
                        targetByteUnit = ISCII_HALANT;
                    } else {
                        /* consume ZWNJ and continue */
                        converterData.contextCharFromUnicode = 0x00;
                        continue;
                    }
                    break;
                case ZWJ:
                    /* contextChar has HALANT */
                    if (converterData.contextCharFromUnicode != 0) {
                        targetByteUnit = ISCII_NUKTA;
                    } else {
                        targetByteUnit = ISCII_INV;
                    }
                    converterData.contextCharFromUnicode = 0x00;
                    break;
                default:
                    /* is the sourceChar in the INDIC_RANGE? */
                    if((char)(INDIC_BLOCK_END - sourceChar) <= INDIC_RANGE) {
                        /* Danda and Doube Danda are valid in Northern scripts.. since Unicode
                         * does not include these codepoints in all Northern scripts we need to
                         * filter them out
                         */
                        if (sourceChar != DANDA && sourceChar != DOUBLE_DANDA) {
                            /* find out to which block the sourceChar belongs */
                            range = (short)((sourceChar - INDIC_BLOCK_BEGIN) / UniLang.DELTA);
                            newDelta = (short)(range * UniLang.DELTA);
                        
                            /* Now are we in the same block as previous? */
                            if (newDelta != converterData.currentDeltaFromUnicode || converterData.isFirstBuffer) {
                                converterData.currentDeltaFromUnicode = newDelta;
                                converterData.currentMaskFromUnicode = lookupInitialData[range].maskEnum;
                                deltaChanged = true;
                                converterData.isFirstBuffer = false;
                            }
                            if (converterData.currentDeltaFromUnicode == PNJ_DELTA) {
                                if (sourceChar == PNJ_TIPPI) {
                                    /* Make sure Tippi is converterd to Bindi. */
                                    sourceChar = PNJ_BINDI;
                                } else if (sourceChar == PNJ_ADHAK) {
                                    /* This is for consonant cluster handling. */
                                    converterData.contextCharFromUnicode = PNJ_ADHAK;
                                }
                            }
                            /* Normalize all Indic codepoints to Devanagari and map them to ISCII */
                            /* now subtract the new delta from sourceChar */
                            sourceChar -= converterData.currentDeltaFromUnicode;
                        }
                        /* get the target byte unit */
                        targetByteUnit = fromUnicodeTable[(short)sourceChar & UConverterConstants.UNSIGNED_BYTE_MASK];
                        
                        /* is the code point valid in current script? */
                        if ((validityTable[(short)sourceChar & UConverterConstants.UNSIGNED_BYTE_MASK] & converterData.currentMaskFromUnicode) == 0) {
                            /* Vocallic RR is assigned in ISCII Telugu and Unicode */
                            if (converterData.currentDeltaFromUnicode != (TELUGU_DELTA) || sourceChar != VOCALLIC_RR) {
                                targetByteUnit = UConverterConstants.missingCharMarker;
                            }
                        }
                        
                        if (deltaChanged) {
                            /* we are in a script block which is different than
                             * previous sourceChar's script block write ATR and language codes
                             */
                            char temp = 0;
                            temp = (char)(ATR << 8);
                            temp += (char)(lookupInitialData[range].isciiLang & UConverterConstants.UNSIGNED_BYTE_MASK);
                            /* reset */
                            deltaChanged = false;
                            /* now append ATR and language code */
                            cr = WriteToTargetFromU(offsets, source, target, temp);
                            if (cr.isOverflow()) {
                                break;
                            }
                        }
                        if (converterData.currentDeltaFromUnicode == PNJ_DELTA && (sourceChar + PNJ_DELTA) == PNJ_ADHAK) {
                            continue;
                        }
                    }
                    /* reset context char */
                    converterData.contextCharFromUnicode = 0x00;
                    break;
                } //end of switch
                if (converterData.currentDeltaFromUnicode == PNJ_DELTA && tempContextFromUnicode == PNJ_ADHAK && PNJ_CONSONANT_SET.contains(sourceChar + PNJ_DELTA)) {
                    /* If the previous codepoint is Adhak and the current codepoint is a consonant, the targetByteUnit should be C + Halant + C. */
                    /* reset context char */
                    converterData.contextCharFromUnicode = 0x0000;
                    targetByteUnit = targetByteUnit << 16 | ISCII_HALANT << 8 | targetByteUnit;
                    /*write targetByteUnit to target */
                    cr = WriteToTargetFromU(offsets, source, target, targetByteUnit);
                    if (cr.isOverflow()) {
                        break;
                    }
                } else if (targetByteUnit != UConverterConstants.missingCharMarker) {
                    if (targetByteUnit == ISCII_HALANT) {
                        converterData.contextCharFromUnicode = (char)targetByteUnit;
                    }
                    /*write targetByteUnit to target */
                    cr = WriteToTargetFromU(offsets, source, target, targetByteUnit);
                    if (cr.isOverflow()) {
                        break;
                    }
                } else if (UTF16.isSurrogate((char)sourceChar)) {
                    cr = handleSurrogates(source, (char) sourceChar);
                    return (cr != null) ? cr : CoderResult.unmappableForLength(2);
                } else {
                    return CoderResult.unmappableForLength(1);
                }
            } /* end of while */
            
            /* save the state and return */
            return cr;
        }
        
        private CoderResult WriteToTargetFromU(IntBuffer offsets, CharBuffer source, ByteBuffer target, int targetByteUnit) {
            CoderResult cr = CoderResult.UNDERFLOW;
            int offset = source.position() - 1;
            /* write the targetUniChar to target */
            if (target.hasRemaining()) {
                if (targetByteUnit <= 0xFF) {
                    target.put((byte)targetByteUnit);
                    if (offsets != null) {
                        offsets.put(offset);
                    }
                } else {
                    if (targetByteUnit > 0xFFFF) {
                        target.put((byte)(targetByteUnit >> 16));
                        if (offsets != null) {
                            --offset;
                            offsets.put(offset);
                        }
                    } 
                    if (!target.hasRemaining()) {
                        errorBuffer[errorBufferLength++] = (byte)(targetByteUnit >> 8);
                        errorBuffer[errorBufferLength++] = (byte)targetByteUnit;
                        cr = CoderResult.OVERFLOW;
                        return cr;
                    }
                    target.put((byte)(targetByteUnit >> 8));
                    if (offsets != null) {
                        offsets.put(offset);
                    }
                    if (target.hasRemaining()) {
                        target.put((byte)targetByteUnit);
                        if (offsets != null) {
                            offsets.put(offset);
                        }
                    } else {
                        errorBuffer[errorBufferLength++] = (byte)targetByteUnit;
                        cr = CoderResult.OVERFLOW;
                    }
                }
            } else {
                if ((targetByteUnit > 0xFFFF)) {
                    errorBuffer[errorBufferLength++] = (byte)(targetByteUnit >> 16);
                } else if ((targetByteUnit & 0xFF00) > 0) {
                    errorBuffer[errorBufferLength++] = (byte)(targetByteUnit >> 8);
                }
                errorBuffer[errorBufferLength++] = (byte)(targetByteUnit);
                cr = CoderResult.OVERFLOW;
            }
            return cr;
        }
    }
    
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderISCII(this);
    }
    
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderISCII(this);
    }
    
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        int idx,script;
        char mask;
        
        setFillIn.add(0,ASCII_END );
        for(script = UniLang.DEVALANGARI ; script<= UniLang.MALAYALAM ;script++){
            mask = (char)lookupInitialData[script].maskEnum;
            for(idx=0; idx < UniLang.DELTA ; idx++){
                // Special check for telugu character
                if((validityTable[idx] & mask)!=0 || (script == UniLang.TELUGU && idx==0x31)){ 
                   setFillIn.add(idx+(script*UniLang.DELTA)+INDIC_BLOCK_BEGIN );
                }
            }
        }
        setFillIn.add(DANDA);
        setFillIn.add(DOUBLE_DANDA);
        setFillIn.add(ZWNJ);
        setFillIn.add(ZWJ);
             
    }
}
