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
class CharsetISCI extends CharsetICU {
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
        /* 0xa0: 0x00: 0x900 */ MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0xa1: 0xb8: 0x901 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0xa2: 0xfe: 0x902 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK, 
        /* 0xa3: 0xbf: 0x903 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0x00: 0x00: 0x904 */ MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0xa4: 0xff: 0x905 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xa5: 0xff: 0x906 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xa6: 0xff: 0x907 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xa7: 0xff: 0x908 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xa8: 0xff: 0x909 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xa9: 0xff: 0x90a */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xaa: 0xfe: 0x90b */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0x00: 0x00: 0x90c */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xae: 0x80: 0x90d */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.GJR_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0xab: 0x87: 0x90e */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xac: 0xff: 0x90f */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xad: 0xff: 0x910 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xb2: 0x80: 0x911 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.GJR_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0xaf: 0x87: 0x912 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xb0: 0xff: 0x913 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xb1: 0xff: 0x914 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xb3: 0xff: 0x915 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xb4: 0xfe: 0x916 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xb5: 0xfe: 0x917 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xb6: 0xfe: 0x918 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xb7: 0xff: 0x919 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xb8: 0xff: 0x91a */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xb9: 0xfe: 0x91b */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xba: 0xff: 0x91c */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xbb: 0xfe: 0x91d */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xbc: 0xff: 0x91e */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xbd: 0xff: 0x91f */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xbe: 0xfe: 0x920 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xbf: 0xfe: 0x921 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xc0: 0xfe: 0x922 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xc1: 0xff: 0x923 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xc2: 0xff: 0x924 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xc3: 0xfe: 0x925 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xc4: 0xfe: 0x926 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xc5: 0xfe: 0x927 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xc6: 0xff: 0x928 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xc7: 0x81: 0x929 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.TML_MASK,
        /* 0xc8: 0xff: 0x92a */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xc9: 0xfe: 0x92b */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xca: 0xfe: 0x92c */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xcb: 0xfe: 0x92d */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xcc: 0xfe: 0x92e */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xcd: 0xff: 0x92f */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xcf: 0xff: 0x930 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd0: 0x87: 0x931 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd1: 0xff: 0x932 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd2: 0xb7: 0x933 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.ZERO+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd3: 0x83: 0x934 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd4: 0xff: 0x935 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.ZERO+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd5: 0xfe: 0x936 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
        /* 0xd6: 0xbf: 0x937 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd7: 0xff: 0x938 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xd8: 0xff: 0x939 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0x00: 0x00: 0x93a */ MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0x00: 0x00: 0x93b */ MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0xe9: 0xda: 0x93c */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.ZERO+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0x00: 0x00: 0x93d */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO+MaskEnum.ZERO,
        /* 0xda: 0xff: 0x93e */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xdb: 0xff: 0x93f */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xdc: 0xff: 0x940 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xdd: 0xff: 0x941 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xde: 0xff: 0x942 */ MaskEnum.DEV_MASK+MaskEnum.PNJ_MASK+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.TML_MASK,
        /* 0xdf: 0xbe: 0x943 */ MaskEnum.DEV_MASK+MaskEnum.ZERO+MaskEnum.GJR_MASK+MaskEnum.ORI_MASK+MaskEnum.BNG_MASK+MaskEnum.KND_MASK+MaskEnum.MLM_MASK+MaskEnum.ZERO,
    };
    
    public CharsetISCI(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
    }
    
    public CharsetDecoder newDecoder() {
        return null;//new CharsetDecoderISCI(this);
    }
    
    public CharsetEncoder newEncoder() {
        return null;//new CharsetEncoderISCI(this);
    }
}
