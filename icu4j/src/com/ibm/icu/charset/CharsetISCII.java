/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
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

import com.ibm.icu.charset.CharsetUTF7.CharsetDecoderUTF7;
import com.ibm.icu.charset.CharsetUTF7.CharsetEncoderUTF7;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * @author Michael Ow
 *
 */
class CharsetISCII extends CharsetICU {
    private final char UCNV_OPTIONS_VERSION_MASK = 0X0f;
    private final char NUKTA = 0x093c;
    private final char HALANT = 0x094d;
    private final char ZWNJ = 0x200c; /* Zero Width Non Joiner */
    private final char ZWJ = 0x200d; /* Zero Width Joiner */
    private final char INVALID_CHAR = 0xffff;
    private final char ATR = 0xef; /* Attribute code */
    private final char EXT = 0xff; /* Extension code */
    private final char DANDA = 0x0964;
    private final char DOUBLE_DANDA = 0x0965;
    private final char ISCII_NUKTA = 0xe9;
    private final char ISCII_HALANT = 0xe8;
    private final char ISCII_DANDA = 0xea;
    private final char ISCII_INV = 0xd9;
    private final char INDIC_BLOCK_BEGIN = 0x0900;
    private final char INDIC_BLOCK_END = 0x0d7f;
    private final char INDIC_RANGE = (INDIC_BLOCK_END - INDIC_BLOCK_BEGIN);
    private final char VOCALLIC_RR = 0x0931;
    private final char LF = 0x0a;
    private final char ASCII_END = 0xa0;
    private final char NO_CHAR_MARKER = 0xfffe;
    private final char TELUGU_DELTA = (char)(UniLang.DELTA * UniLang.TELUGU);
    private final char DEV_ABBR_SIGN = 0x0970;
    private final char DEV_ANUDATTA = 0x0952;
    private final char EXT_RANGE_BEGIN = 0xa1;
    private final char EXT_RANGE_END = 0xee;
    
    private static final class UniLang {
        static final int DEVALANGARI = 0;
        static final int BENGALI = DEVALANGARI + 1;
        static final int GURMUKHI = BENGALI + 1;
        static final int GUJARATI = GURMUKHI + 1;
        static final int ORIYA = GUJARATI + 1;
        static final int TAMIL = ORIYA + 1;
        static final int TELUGU = TAMIL + 1;
        static final int KANNADA = TELUGU + 1;
        static final int MALAYALAM = KANNADA + 1;
        static final int DELTA = 0x80;
    }
    
    private static final class ISCIILang {
        static final int DEF = 0x40;
        static final int RMN = 0x41;
        static final int DEV = 0x42;
        static final int BNG = 0x43;
        static final int TML = 0x44;
        static final int TLG = 0x45;
        static final int ASM = 0x46;
        static final int ORI = 0x47;
        static final int KND = 0x48;
        static final int MLM = 0x49;
        static final int GJR = 0x4a;
        static final int PNJ = 0x4b;
        static final int ARB = 0x71;
        static final int PES = 0x72;
        static final int URD = 0x73;
        static final int SND = 0x74;
        static final int KSM = 0x75;
        static final int PST = 0x76;
    }
    
    private static final class MaskEnum {
        static final int DEV_MASK = 0x80;
        static final int PNJ_MASK = 0x40;
        static final int GJR_MASK = 0x20;
        static final int ORI_MASK = 0x10;
        static final int BNG_MASK = 0x08;
        static final int KND_MASK = 0x04;
        static final int MLM_MASK = 0x02;
        static final int TML_MASK = 0x01;
        static final int ZERO = 0x00;
    }
    
    private final String ISCII_CNV_PREFIX = "ISCII,version=";
    
    private final class UConverterDataISCII {
        char contextCharToUnicode;      /* previous Unicode codepoint for contextual analysis */
        char contextCharFromUnicode;    /* previous Unicode codepoint for contextual analysis */
        char defDeltaToUnicode;             /* delta for switching to default state when DEF is encountered */
        char currentDeltaFromUnicode;   /* current delta in Indic block */
        char currentDeltaToUnicode;         /* current delta in Indic block */
        int currentMaskFromUnicode;    /* mask for current state in fromUnicode */
        int currentMaskToUnicode;          /* mask for current state in toUnicode */
        boolean isFirstBuffer;          /* boolean for fromUnicode to see if we need to announce the first script */
        boolean resetToDefaultToUnicode;    /* boolean for reseting to default delta and mask when a newline is encountered */
        String name;
    }
    
    private static final class LookupDataStruct {
        int uniLang;
        int maskEnum;
        int isciiLang;
        
        LookupDataStruct(int uniLang, int maskEnum, int isciiLang) {
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
    private static final char validityTable[] = {
        /* This state table is tool generated so please do not edit unless you know exactly what you are doing */
        /* Note:  This table was edited to mirror the Windows XP implementation */
        /* ISCII: Valid: Unicode */
        /* 0xa0: 0x00: 0x900 */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xa1: 0xb8: 0x901 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0xa2: 0xfe: 0x902 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK, 
        /* 0xa3: 0xbf: 0x903 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0x00: 0x00: 0x904 */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
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
        /* 0xcc: 0xfe: 0x92e */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.ZERO,
        /* 0xcd: 0xff: 0x92f */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xcf: 0xff: 0x930 */ MaskEnum.DEV_MASK + MaskEnum.PNJ_MASK + MaskEnum.GJR_MASK + MaskEnum.ORI_MASK + MaskEnum.BNG_MASK + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
        /* 0xd0: 0x87: 0x931 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.KND_MASK + MaskEnum.MLM_MASK + MaskEnum.TML_MASK,
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
        /* 0x00: 0x80: 0x970 */ MaskEnum.DEV_MASK + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
        /* 0x00: 0x00: 0x9yz */ MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO + MaskEnum.ZERO,
    };
    
    /*
     * The length of the array is 128 to provide values for 0x900..0x97f.
     * The last 15 entries for 0x971..0x97f of the table are all zero
     * because no Indic script uses such Unicode code points.
     */
    private static final char fromUnicodeTable[] = {
      0x00a0, /* 0x0900 */ 
      0x00a1, /* 0x0901 */
      0x00a2, /* 0x0902 */
      0x00a3, /* 0x0903 */
      0xFFFF, /* 0x0904 */
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
        { 0xA6, 0x090c },
        { 0xEA, 0x093D },
        { 0xDF, 0x0944 },
        { 0xA1, 0x0950 },
        { 0xb3, 0x0958 },
        { 0xb4, 0x0959 },
        { 0xb5, 0x095a },
        { 0xba, 0x095b },
        { 0xbf, 0x095c },
        { 0xC0, 0x095d },
        { 0xc9, 0x095e },
        { 0xAA, 0x0960 },
        { 0xA7, 0x0961 },
        { 0xDB, 0x0962 },
        { 0xDC, 0x0963 }
    };
    
    private static final int lookupTable[][] = {
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
    
    protected byte[] fromUSubstitution = new byte[]{0x2b, 0x2f, 0x76}; //TODO: change this to the appropriate value
    
    public CharsetISCII(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
        //TODO: change these three to the appropriate value
        maxBytesPerChar = 3; 
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
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
        }
        
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) { 
            CoderResult cr = CoderResult.UNDERFLOW;
            
            return cr;
        }
        
        private CoderResult WriteToTargetToU() {
            CoderResult cr = CoderResult.UNDERFLOW;
            
            return cr;
        }
        
        private void GetMapping() {
            
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
        }
        
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult cr = CoderResult.UNDERFLOW;
            
            return cr;
        }
        
        private CoderResult WriteToTargetFromU() {
            CoderResult cr = CoderResult.UNDERFLOW;
            
            return cr;
        }
    }
    
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderISCII(this);
    }
    
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderISCII(this);
    }
}
