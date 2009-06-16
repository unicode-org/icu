/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
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

import com.ibm.icu.charset.CharsetMBCS.CharsetDecoderMBCS;
import com.ibm.icu.charset.CharsetMBCS.CharsetEncoderMBCS;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.text.UnicodeSet;
/**
 * @author Michael Ow
 *
 */

/*
 * LMBCS
 * 
 * (Lotus Multi-Byte Character Set)
 * 
 * LMBS was invented in the alte 1980's and is primarily used in Lotus Notes
 * databases and in Lotus 1-2-3 files. Programmers who work with the APIs
 * into these products will sometimes need to deal with strings in this format.
 * 
 * The code in this file provides an implementation for an ICU converter of
 * LMBCS to and from Unicode.
 * 
 * Since the LMBCS character set is only sparsely documented in existing
 * printed or online material, we have added extensive annotation to this
 * file to serve as a guide to understanding LMBCS.
 * 
 * LMBCS was originally designed with these four sometimes-competing design goals:
 * -Provide encodings for characters in 12 existing national standards
 *  (plus a few other characters)
 * -Minimal memory footprint
 * -Maximal speed of conversion into the existing national character sets
 * -No need to track a changing state as you interpret a string.
 * 
 * All of the national character sets LMBCS was trying to encode are 'ANSI'
 * based, in that the bytes from 0x20 - 0x7F are almost exactly the
 * same common Latin unaccented characters and symbols in all character sets.
 * 
 * So, in order to help meet the speed & memory design goals, the common ANSI
 * bytes from 0x20-0x7F are represented by the same single-byte values in LMBCS.
 */
class CharsetLMBCS extends CharsetICU {
    /*
     * The general LMBCS code unit is from 1-3 bytes. We can describe the 3 bytes as
     * follows:
     * [G] D1 [D2]
     * That is, a sometimes-optional 'group' byte, followed by 1 and sometimes 2
     * data bytes. The maximum size of a LMBCS character is 3 bytes:
     */
    private static final short ULMBCS_CHARSIZE_MAX = 3;
    /*
     * The single-byte values from 0x20 to 0x7F are examples of single D1 bytes.
     * We often have to figure out if byte values are below or above this, so we
     * use the ANSI nomenclature 'C0' and 'C1' to refer to the range of control
     * characters just above & below the common lower-ANSI range.
     */
    private static final short ULMBCS_C0END    = 0x1F;
    private static final short ULMBCS_C1START  = 0x80;
    /*
     * Most of the values less than 0x20 are reserved in LMBCS to announce
     * which national character standard is being used for the 'D' bytes.
     * In the comments we show that common name and the IBM character-set ID
     * for these character-set announcers:
     */
    private static final short ULMBCS_GRP_L1   = 0x01; /* Latin-1      :ibm-850    */
    private static final short ULMBCS_GRP_GR   = 0x02; /* Greek        :ibm-851    */
    private static final short ULMBCS_GRP_HE   = 0x03; /* Hebrew       :ibm-1255   */
    private static final short ULMBCS_GRP_AR   = 0x04; /* Arabic       :ibm-1256   */
    private static final short ULMBCS_GRP_RU   = 0x05; /* Cyrillic     :ibm-1251   */
    private static final short ULMBCS_GRP_L2   = 0x06; /* Latin-2      :ibm-852    */
    private static final short ULMBCS_GRP_TR   = 0x08; /* Turkish      :ibm-1254   */
    private static final short ULMBCS_GRP_TH   = 0x0B; /* Thai         :ibm-874    */
    private static final short ULMBCS_GRP_JA   = 0x10; /* Japanese     :ibm-943    */
    private static final short ULMBCS_GRP_KO   = 0x11; /* Korean       :ibm-1261   */
    private static final short ULMBCS_GRP_TW   = 0x12; /* Chinese SC   :ibm-950    */
    private static final short ULMBCS_GRP_CN   = 0x13; /* Chinese TC   :ibm-1386   */
    /* 
     * So, the beginnning of understanding LMBCS is that IF the first byte of a LMBCS
     * character is one of those 12 values, you can interpret the remaining bytes of 
     * that character as coming from one of those character sets. Since the lower
     * ANSI bytes already are represented in singl bytes, using one of the chracter
     * set announcers is used to announce a character that starts with a byte of
     * 0x80 or greater.
     * 
     * The character sets are arranged so that the single byte sets all appear
     * before the multi-byte character sets. When we need to tell whether a
     * group byte is for a single byte char set or not we use this definition:
     */
    private static final short ULMBCS_DOUBLEOPTGROUP_START = 0x10;
    /*
     * However, to fully understand LMBCS, you must also understand a series of
     * exceptions & optimizations made in service of the design goals.
     * 
     * First, those of you who are character set mavens may have noticed that
     * the 'double-byte' character sets are actually multi-byte chracter sets
     * that can have 1 or two bytes, even in upper-ascii range. To force
     * each group byte to introduce a fixed-width encoding (to make it faster to
     * count characters), we use a convention of doubling up on the group byte
     * to introduce any single-byte character > 0x80 in an otherwise double-byte
     * character set. So, for example, the LMBCS sequence x10 x10 xAE is the
     * same as '0xAE' in the Japanese code page 943.
     * 
     * Next, you will notice that the list of group bytes has some gaps.
     * These are used in various ways.
     * 
     * We reserve a few special single byte values for common control
     * characters. These are in the same place as their ANSI equivalents for speed.
     */
    private static final short ULMBCS_HT   = 0x09; /* Fixed control-char - Horizontal Tab */
    private static final short ULMBCS_LF   = 0x0A; /* Fixed control-char - Line Feed */
    private static final short ULMBCS_CR   = 0x0D; /* Fixed control-char - Carriage Return */
    /*
     * Then, 1-2-3 reserved a special single-byte character to put at the
     * beginning of internal 'system' range names:
     */
    private static final short ULMBCS_123SYSTEMRANGE   = 0x19;
    /*
     * Then we needed a place to put all the other ansi control characters
     * that must be moved to different values because LMBCS reserves those
     * values for other purposes. To represent the control characters, we start
     * with a first byte of 0x0F & add the control character value as the
     * second byte.
     */
    private static final short ULMBCS_GRP_CTRL = 0x0F;
    /*
     * For the C0 controls (less than 0x20), we add 0x20 to preserve the
     * useful doctrine that any byte less than 0x20 in a LMBCS char must be
     * the first byte of a character:
     */
    private static final short ULMBCS_CTRLOFFSET   = 0x20;
    /*
     * Where to put the characters that aren't part of any of the 12 national
     * character sets? The first thing that was done, in the earlier years of
     * LMBCS, was to use up the spaces of the form
     *  [G] D1,
     * where 'G' was one of the single-byte character groups, and
     * D1 was less than 0x80. These sequences are gathered together
     * into a Lotus-invented doublebyte character set to represent a
     * lot of stray values. Internally, in this implementation, we track this
     * as group '0', as a place to tuck this exceptions list.
     */
    private static final short ULMBCS_GRP_EXCEPT   = 0x00;
    /*
     * Finally, as the durability and usefulness of UNICODE became clear,
     * LOTUS added a new group 0x14 to hold Unicode values not otherwise
     * represented in LMBCS:
     */
    private static final short ULMBCS_GRP_UNICODE  = 0x14;
    /*
     * The two bytes appearing after a 0x14 are interpreted as UTF-16 BE
     * (Big Endian) characters. The exception comes when UTF16 
     * representation would have a zero as the second byte. In that case,
     * 'F6' is used in its place, and the bytes are swapped. (This prevents
     * LMBCS from encoding any Unicode values of the form U+F6xx, but that's OK:
     * 0xF6xx is in the middle of the Private Use Area.)
     */
    private static char ULMBCS_UNICOMPATZERO    = 0x00F6;
    /*
     * It is also useful in our code to have a constant for the size of
     * a LMBCS char that holds a literal Unicode value.
     */
    private static final short ULMBCS_UNICODE_SIZE = 3;
    /*
     * To squish the LMBCS representation down even further, and to make
     * translations even faster, sometimes the optimization group byte can be dropped
     * from a LMBCS character. This is decided on a process-by-process basis. The
     * group byte that is dropped is called the 'optimization group.'
     * 
     * For Notes, the optimization group is always 0x1.
     */
    //private static final short ULMBCS_DEFAULTOPTGROUP  = 0x01;
    /* For 1-2-3 files, the optimization group is stored in the header of the 1-2-3
     * file.
     * In any case, when using ICU, you either pass in the
     * optimization group as part of the name of the converter (LMBCS-1, LMBCS-2,
     * etc.). Using plain 'LMBCS' as the name of the converter will give you
     * LMBCS-1.
     */
    
    /* Implementation strategy */
    /* 
     * Because of the extensive use of other character sets, the LMBCS converter
     * keeps a mapping between optimization groups and IBM character sets, so that
     * ICU converters can be created and used as needed.
     * 
     * As you can see, even though any byte below 0x20 could be an optimization
     * byte, only those at 0x13 or below can map to an actual converter. To limit
     * some loops and searches, we define a value for that last group converter:
     */
    private static final short ULMBCS_GRP_LAST = 0x13; /* last LMBCS group that has a converter */
    
    private static final String[] OptGroupByteToCPName = {
        /* 0x0000 */ "lmb-excp", /* internal home for the LOTUS exceptions list */
        /* 0x0001 */ "ibm-850",
        /* 0x0002 */ "ibm-851",
        /* 0x0003 */ "windows-1255",
        /* 0x0004 */ "windows-1256",
        /* 0x0005 */ "windows-1251",
        /* 0x0006 */ "ibm-852",
        /* 0x0007 */ null,      /* Unused */
        /* 0x0008 */ "windows-1254",
        /* 0x0009 */ null,      /* Control char HT */
        /* 0x000A */ null,      /* Control char LF */
        /* 0x000B */ "windows-874",
        /* 0x000C */ null,      /* Unused */
        /* 0x000D */ null,      /* Control char CR */
        /* 0x000E */ null,      /* Unused */
        /* 0x000F */ null,      /* Control chars: 0x0F20 + C0/C1 character: algorithmic */
        /* 0x0010 */ "windows-932",
        /* 0x0011 */ "windows-949",
        /* 0x0012 */ "windows-950",
        /* 0x0013 */ "windows-936",
        /* The rest are null, including the 0x0014 Unicode compatibility region
         * and 0x0019, the 1-2-3 system range control char */
        /* 0x0014 */ null 
    };
    
    /* That's approximately all the data that's needed for translating
     * LMBCS to Unicode.
     * 
     * However, to translate Unicode to LMBCS, we need some more support.
     * 
     * That's because there are often more than one possible mappings from a Unicode
     * code point back into LMBCS. The first thing we do is look up into a table
     * to figure out if there are more than one possible mapplings. This table,
     * arranged by Unicode values (including ranges) either lists which group
     * to use, or says that it could go into one or more of the SBCS sets, or
     * into one or more of the DBCS sets. (If the character exists in both DBCS &
     * SBCS, the table will place it in the SBCS sets, to make the LMBCS code point
     * length as small as possible. Here's the two special markers we use to indicate
     * ambiguous mappings:
     */
    private static final short ULMBCS_AMBIGUOUS_SBCS   = 0x80; /* could fit in more than one
                                                           LMBCS sbcs native encoding
                                                           (example: most accented latin) */
    private static final short ULMBCS_AMBIGUOUS_MBCS   = 0x81; /* could fit in more than one
                                                           LMBCS mbcs native encoding
                                                           (example: Unihan) */
    
    /* And here's a simple way to see if a group falls in an appropriate range */
    private boolean ULMBCS_AMBIGUOUS_MATCH(short agroup, short xgroup) {
        return (((agroup == ULMBCS_AMBIGUOUS_SBCS) &&
                 (xgroup < ULMBCS_DOUBLEOPTGROUP_START)) ||
                ((agroup == ULMBCS_AMBIGUOUS_MBCS) &&
                 (xgroup >= ULMBCS_DOUBLEOPTGROUP_START)));
    }
    
    /* The table & some code to use it: */
    private static class _UniLMBCSGrpMap {
        int uniStartRange;
        int uniEndRange;
        short GrpType;
        _UniLMBCSGrpMap(int uniStartRange, int uniEndRange, short GrpType) {
            this.uniStartRange  = uniStartRange;
            this.uniEndRange    = uniEndRange;
            this.GrpType        = GrpType;
        }
    }
    
    private static final _UniLMBCSGrpMap[] UniLMBCSGrpMap = {
        new _UniLMBCSGrpMap(0x0001, 0x001F, ULMBCS_GRP_CTRL),
        new _UniLMBCSGrpMap(0x0080, 0x009F, ULMBCS_GRP_CTRL),
        new _UniLMBCSGrpMap(0x00A0, 0x01CD, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x01CE, 0x01CE, ULMBCS_GRP_TW),
        new _UniLMBCSGrpMap(0x01CF, 0x02B9, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x02BA, 0x02BA, ULMBCS_GRP_CN),
        new _UniLMBCSGrpMap(0x02BC, 0x02C8, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x02C9, 0x02D0, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x02D8, 0x02DD, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x0384, 0x03CE, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x0400, 0x044E, ULMBCS_GRP_RU),
        new _UniLMBCSGrpMap(0x044F, 0x044F, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x0450, 0x0491, ULMBCS_GRP_RU),
        new _UniLMBCSGrpMap(0x05B0, 0x05F2, ULMBCS_GRP_HE),
        new _UniLMBCSGrpMap(0x060C, 0x06AF, ULMBCS_GRP_AR),
        new _UniLMBCSGrpMap(0x0E01, 0x0E5B, ULMBCS_GRP_TH),
        new _UniLMBCSGrpMap(0x200C, 0x200F, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2010, 0x2010, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2013, 0x2015, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2016, 0x2016, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2017, 0x2024, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2025, 0x2025, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2026, 0x2026, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2027, 0x2027, ULMBCS_GRP_CN),
        new _UniLMBCSGrpMap(0x2030, 0x2033, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2035, 0x2035, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2039, 0x203A, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x203B, 0x203B, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2074, 0x2074, ULMBCS_GRP_KO),
        new _UniLMBCSGrpMap(0x207F, 0x207F, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2081, 0x2084, ULMBCS_GRP_KO),
        new _UniLMBCSGrpMap(0x20A4, 0x20AC, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2103, 0x2109, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2111, 0x2126, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x212B, 0x212B, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2135, 0x2135, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2153, 0x2154, ULMBCS_GRP_KO),
        new _UniLMBCSGrpMap(0x215B, 0x215E, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2160, 0x2179, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2190, 0x2195, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2196, 0x2199, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x21A8, 0x21A8, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x21B8, 0x21B9, ULMBCS_GRP_CN),
        new _UniLMBCSGrpMap(0x21D0, 0x21D5, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x21E7, 0x21E7, ULMBCS_GRP_CN),
        new _UniLMBCSGrpMap(0x2200, 0x220B, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x220F, 0x2215, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2219, 0x2220, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2223, 0x2228, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2229, 0x222B, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x222C, 0x223D, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2245, 0x2248, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x224C, 0x224C, ULMBCS_GRP_TW),
        new _UniLMBCSGrpMap(0x2252, 0x2252, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2260, 0x2265, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2266, 0x226F, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2282, 0x2297, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2299, 0x22BF, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x22C0, 0x22C0, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2310, 0x2310, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2312, 0x2312, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2318, 0x2321, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2318, 0x2321, ULMBCS_GRP_CN),
        new _UniLMBCSGrpMap(0x2460, 0x24E9, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2500, 0x2500, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2501, 0x2501, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2502, 0x2502, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2503, 0x2503, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2504, 0x2505, ULMBCS_GRP_TW),
        new _UniLMBCSGrpMap(0x2506, 0x2665, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0x2666, 0x2666, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2666, 0x2666, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0x2667, 0x2E7F, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0x2E80, 0xF861, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0xF862, 0xF8FF, ULMBCS_GRP_EXCEPT),
        new _UniLMBCSGrpMap(0xF900, 0xFA2D, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0xFB00, 0xFEFF, ULMBCS_AMBIGUOUS_SBCS),
        new _UniLMBCSGrpMap(0xFF01, 0xFFEE, ULMBCS_AMBIGUOUS_MBCS),
        new _UniLMBCSGrpMap(0xFFFF, 0xFFFF, ULMBCS_GRP_UNICODE)
    };
    
    static short FindLMBCSUniRange(char uniChar) {
        int index = 0;
        
        while (uniChar > UniLMBCSGrpMap[index].uniEndRange) {
            index++;
        }
        
        if (uniChar >= UniLMBCSGrpMap[index].uniStartRange) {
            return UniLMBCSGrpMap[index].GrpType;
        }
        return ULMBCS_GRP_UNICODE;
    }
    
    /*
     * We also ask the creator of a converter to send in a preferred locale
     * that we can use in resolving ambiguous mappings. They send the locale
     * in as a string, and we map it, if possible, to one of the
     * LMBCS groups. We use this table, and the associated code, to
     * do the lookup:
     * 
     *     This table maps locale ID's to LMBCS opt groups.
     *     The default return is group 0x01. Note that for
     *     performance reasons, the table is sorted in
     *     increasing alphabetic order, with the notable
     *     exception of zhTW. This is to force the check
     *     for Traditional Chinese before dropping back to
     *     Simplified.
     *     Note too that the Latin-1 groups have been
     *     commented out because it's the default, and
     *     this shortens the table, allowing a serial
     *     search to go quickly.
     */
    private static class _LocaleLMBCSGrpMap {
        String LocaleID;
        short OptGroup;
        _LocaleLMBCSGrpMap(String LocaleID, short OptGroup) {
            this.LocaleID = LocaleID;
            this.OptGroup = OptGroup;
        }
    }
    private static final _LocaleLMBCSGrpMap[] LocaleLMBCSGrpMap = {
        new _LocaleLMBCSGrpMap("ar", ULMBCS_GRP_AR),
        new _LocaleLMBCSGrpMap("be", ULMBCS_GRP_RU),
        new _LocaleLMBCSGrpMap("bg", ULMBCS_GRP_L2),
        // new _LocaleLMBCSGrpMap("ca", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("cs", ULMBCS_GRP_L2),
        // new _LocaleLMBCSGrpMap("da", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("de", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("el", ULMBCS_GRP_GR),
        // new _LocaleLMBCSGrpMap("en", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("es", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("et", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("fi", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("fr", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("he", ULMBCS_GRP_HE),
        new _LocaleLMBCSGrpMap("hu", ULMBCS_GRP_L2),
        // new _LocaleLMBCSGrpMap("is", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("it", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("iw", ULMBCS_GRP_HE),
        new _LocaleLMBCSGrpMap("ja", ULMBCS_GRP_JA),
        new _LocaleLMBCSGrpMap("ko", ULMBCS_GRP_KO),
        // new _LocaleLMBCSGrpMap("lt", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("lv", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("mk", ULMBCS_GRP_RU),
        // new _LocaleLMBCSGrpMap("nl", ULMBCS_GRP_L1),
        // new _LocaleLMBCSGrpMap("no", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("pl", ULMBCS_GRP_L2),
        // new _LocaleLMBCSGrpMap("pt", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("ro", ULMBCS_GRP_L2),
        new _LocaleLMBCSGrpMap("ru", ULMBCS_GRP_RU),
        new _LocaleLMBCSGrpMap("sh", ULMBCS_GRP_L2),
        new _LocaleLMBCSGrpMap("sk", ULMBCS_GRP_L2),
        new _LocaleLMBCSGrpMap("sl", ULMBCS_GRP_L2),
        new _LocaleLMBCSGrpMap("sq", ULMBCS_GRP_L2),
        new _LocaleLMBCSGrpMap("sr", ULMBCS_GRP_RU),
        // new _LocaleLMBCSGrpMap("sv", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("th", ULMBCS_GRP_TH),
        new _LocaleLMBCSGrpMap("tr", ULMBCS_GRP_TR),
        new _LocaleLMBCSGrpMap("uk", ULMBCS_GRP_RU),
        // new _LocaleLMBCSGrpMap("vi", ULMBCS_GRP_L1),
        new _LocaleLMBCSGrpMap("zhTW", ULMBCS_GRP_TW),
        new _LocaleLMBCSGrpMap("zh", ULMBCS_GRP_CN),
        new _LocaleLMBCSGrpMap(null, ULMBCS_GRP_L1)
    };
    static short FindLMBCSLocale(String LocaleID) {
        int index = 0;
        
        if (LocaleID == null) {
            return 0;
        }
        
        while (LocaleLMBCSGrpMap[index].LocaleID != null) {
            if (LocaleLMBCSGrpMap[index].LocaleID == LocaleID) {
                return LocaleLMBCSGrpMap[index].OptGroup;
            } else if (LocaleLMBCSGrpMap[index].LocaleID.compareTo(LocaleID) > 0){
               break;
            }
            index++;
        }
        return ULMBCS_GRP_L1;
    }
    
    /*
     * Before we get to the main body of code, here's how we hook up the rest
     * of ICU. ICU converters are required to define a structure that includes
     * some function pointers, and some common data, in the style of a C++
     * vtable. There is also room in there for converter-specific data. LMBCS
     * uses that converter-specific data to keep track of the 12 subconverters
     * we use, the optimization group, and the group (if any) that matches the
     * locale. We have one structure instantiated for each of the 12 possible
     * optimization groups.
     */
    private class UConverterDataLMBCS {
        UConverterSharedData[] OptGrpConverter; /* Converter per Opt. grp. */
        short OptGroup;                         /* default Opt. grp. for this LMBCS session */
        short localeConverterIndex;             /* reasonable locale match for index */
        CharsetDecoderMBCS decoder;
        CharsetEncoderMBCS encoder;
        CharsetMBCS charset;
        UConverterDataLMBCS() {
            OptGrpConverter = new UConverterSharedData[ULMBCS_GRP_LAST + 1];
            charset = (CharsetMBCS)CharsetICU.forNameICU("ibm-850");
            encoder = (CharsetEncoderMBCS)charset.newEncoder();
            decoder = (CharsetDecoderMBCS)charset.newDecoder();
        }
    }
    
    private UConverterDataLMBCS extraInfo; /* extraInfo in ICU4C implementation */
    
    public CharsetLMBCS(String icuCanonicalName, String javaCanonicalName, String[] aliases) {
        super(icuCanonicalName, javaCanonicalName, aliases);
        maxBytesPerChar = ULMBCS_CHARSIZE_MAX; 
        minBytesPerChar = 1;
        maxCharsPerByte = 1;
        
        extraInfo = new UConverterDataLMBCS();
        
        for (int i = 0; i <= ULMBCS_GRP_LAST; i++) {
            if (OptGroupByteToCPName[i] != null) {
                extraInfo.OptGrpConverter[i] = ((CharsetMBCS)CharsetICU.forNameICU(OptGroupByteToCPName[i])).sharedData;
            }
        }
        
      //get the Opt Group number for the LMBCS converter
        int option = Integer.parseInt(icuCanonicalName.substring(6));
        extraInfo.OptGroup = (short)option;
        extraInfo.localeConverterIndex = FindLMBCSLocale(ULocale.getDefault().getBaseName());
    }
    
    class CharsetDecoderLMBCS extends CharsetDecoderICU {
        public CharsetDecoderLMBCS(CharsetICU cs) {
            super(cs);
            implReset();
        }
    
        protected void implReset() {
            super.implReset();
        }
        
        /* A function to call when we are looking at the Unicode group byte in LMBCS */
        private char GetUniFromLMBCSUni(ByteBuffer ppLMBCSin) {
            short HighCh = (short)(ppLMBCSin.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
            short LowCh  = (short)(ppLMBCSin.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
            
            if (HighCh == ULMBCS_UNICOMPATZERO) {
                HighCh = LowCh;
                LowCh = 0; /* zero-byte in LSB special character */
            }
            
            return (char)((HighCh << 8) | LowCh);
        }
        
        private int LMBCS_SimpleGetNextUChar(UConverterSharedData cnv, ByteBuffer source, int positionOffset, int length) {
            int uniChar;
            int oldSourceLimit;
            int oldSourcePos;
            
            extraInfo.charset.sharedData = cnv;
            
            oldSourceLimit = source.limit();
            oldSourcePos = source.position();
            
            source.position(oldSourcePos + positionOffset);
            source.limit(source.position() + length);
            
            uniChar = extraInfo.decoder.simpleGetNextUChar(source, false);
            
            source.limit(oldSourceLimit);
            source.position(oldSourcePos);

            return uniChar;
        }
        /* Return the Unicode representation for the current LMBCS character. */
        /*
         * Note: Because there is no U_TRUNCATED_CHAR_FOUND error code in ICU4J, we
         *       are going to use BufferOverFlow. The error will be handled correctly
         *       by the calling function.
         */
        private int LMBCSGetNextUCharWorker(ByteBuffer source, CoderResult[] err) {
            int uniChar = 0;  /* an output Unicode char */
            short CurByte;   /* A byte from the input stream */
            
            /* error check */
            if (!source.hasRemaining()) {
                err[0] = CoderResult.malformedForLength(0);
                return 0xffff;
            }
            /* Grab first byte & save address for error recovery */
            CurByte = (short)(source.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
            
            /*
             * at entry of each if clause:
             * 1. 'CurByte' points at the first byte of a LMBCS character
             * 2. 'source' points to the next byte of the source stream after 'CurByte'
             * 
             * the job of each if clause is:
             * 1. set 'source' to the point at the beginning of the next char (not if LMBCS char is only 1 byte)
             * 2. set 'uniChar' up with the right Unicode value, or set 'err' appropriately
             */
            /* First lets check the simple fixed values. */
            if ((CurByte > ULMBCS_C0END && CurByte < ULMBCS_C1START) /* ascii range */ ||
                CurByte == 0 || CurByte == ULMBCS_HT || CurByte == ULMBCS_CR || CurByte == ULMBCS_LF ||
                CurByte == ULMBCS_123SYSTEMRANGE) {
                
                uniChar = CurByte;
            } else {
                short group;
                UConverterSharedData cnv;
                
                if (CurByte == ULMBCS_GRP_CTRL) {  /* Control character group - no opt group update */
                    short C0C1byte;
                    /* CHECK_SOURCE_LIMIT(1) */
                    if (source.position() + 1 > source.limit()) {
                        err[0] = CoderResult.OVERFLOW;
                        source.position(source.limit());
                        return 0xFFFF;
                    }
                    C0C1byte = (short)(source.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
                    uniChar = (C0C1byte < ULMBCS_C1START) ? C0C1byte - ULMBCS_CTRLOFFSET : C0C1byte;
                } else if (CurByte == ULMBCS_GRP_UNICODE) { /* Unicode Compatibility group: Big Endian UTF16 */
                    /* CHECK_SOURCE_LIMIT(2) */
                    if (source.position() + 2 > source.limit()) {
                        err[0] = CoderResult.OVERFLOW;
                        source.position(source.limit());
                        return 0xFFFF;
                    }
                    
                    /* don't check for error indicators fffe/ffff below */
                    return GetUniFromLMBCSUni(source);
                } else if (CurByte <= ULMBCS_CTRLOFFSET) {
                    group = CurByte;
                    if (group > ULMBCS_GRP_LAST || (cnv = extraInfo.OptGrpConverter[group]) == null) {
                        /* this is not a valid group byte - no converter */
                        err[0] = CoderResult.unmappableForLength(1);
                    } else if (group >= ULMBCS_DOUBLEOPTGROUP_START) {
                        /* CHECK_SOURCE_LIMIT(2) */
                        if (source.position() + 2 > source.limit()) {
                            err[0] = CoderResult.OVERFLOW;
                            source.position(source.limit());
                            return 0xFFFF;
                        }
                        
                        /* check for LMBCS doubled-group-byte case */
                        if (source.get(source.position()) == group) {
                            /* single byte */
                            source.get();
                            uniChar = LMBCS_SimpleGetNextUChar(cnv, source, 0, 1);
                            source.get();
                        } else {
                            /* double byte */
                            uniChar = LMBCS_SimpleGetNextUChar(cnv, source, 0, 2);
                            source.get();
                            source.get();
                        }
                    } else { /* single byte conversion */
                        /* CHECK_SOURCE_LIMIT(1) */
                        if (source.position() + 1 > source.limit()) {
                            err[0] = CoderResult.OVERFLOW;
                            source.position(source.limit());
                            return 0xFFFF;
                        }
                        CurByte = (short)(source.get() & UConverterConstants.UNSIGNED_BYTE_MASK);
                        
                        if (CurByte >= ULMBCS_C1START) {
                            uniChar = CharsetMBCS.MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(cnv.mbcs, CurByte);
                        } else {
                            /*
                             * The non-optimizable oddballs where there is an explicit byte
                             * AND the second byte is not in the upper ascii range
                             */
                            byte[] bytes = new byte[2];
                            
                            cnv = extraInfo.OptGrpConverter[ULMBCS_GRP_EXCEPT];
                            
                            /* Lookup value must include opt group */
                            bytes[0] = (byte)group;
                            bytes[1] = (byte)CurByte;
                            uniChar = LMBCS_SimpleGetNextUChar(cnv, ByteBuffer.wrap(bytes), 0, 2);
                        }
                    }
                    
                } else if (CurByte >= ULMBCS_C1START) { /* group byte is implicit */
                    group = extraInfo.OptGroup;
                    cnv = extraInfo.OptGrpConverter[group];
                    if (group >= ULMBCS_DOUBLEOPTGROUP_START) { /* double byte conversion */
                        if (CharsetMBCS.MBCS_ENTRY_IS_TRANSITION(cnv.mbcs.stateTable[0][CurByte]) /* isLeadByte */) {
                            /* CHECK_SOURCE_LIMIT(0) */
                            if (source.position() + 0 > source.limit()) {
                                err[0] = CoderResult.OVERFLOW;
                                source.position(source.limit());
                                return 0xFFFF;
                            }
                            
                            /* let the MBCS conversion consume CurByte again */
                            uniChar = LMBCS_SimpleGetNextUChar(cnv, source, -1, 1);
                        } else {
                            /* CHECK_SOURCE_LIMIT(1) */
                            if (source.position() + 1 > source.limit()) {
                                err[0] = CoderResult.OVERFLOW;
                                source.position(source.limit());
                                return 0xFFFF;
                            }
                            
                            /* let the MBCS conversion consume CurByte again */
                            uniChar = LMBCS_SimpleGetNextUChar(cnv, source, -1, 2);
                            source.get();
                        }
                    } else {
                        uniChar = CharsetMBCS.MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(cnv.mbcs, CurByte);
                    }
                }
            }
            
            return uniChar;
        }
        
        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) { 
            CoderResult[] err = new CoderResult[1];
            err[0] = CoderResult.UNDERFLOW;
            byte[] LMBCS = new byte[ULMBCS_CHARSIZE_MAX * 2]; /* Increase the size for proper handling in subsequent calls to MBCS functions */
            char uniChar;   /* one output Unicode char */
            int saveSource; /* beginning of current code point */
            int errSource = 0; /* index to actual input in case an error occurs */
            byte savebytes = 0;
            
            /* Process from source to limit, or until error */
            while (err[0].isUnderflow() && source.hasRemaining() && target.hasRemaining()) {
                saveSource = source.position(); /* beginning of current code point */
                if (toULength > 0) { /* reassemble char from previous call */
                    int size_old = toULength;
                    ByteBuffer tmpSourceBuffer;
                    
                    /* limit from source is either remainder of temp buffer, or user limit on source */
                    int size_new_maybe_1 = ULMBCS_CHARSIZE_MAX - size_old;
                    int size_new_maybe_2 = source.remaining();
                    int size_new = (size_new_maybe_1 < size_new_maybe_2) ? size_new_maybe_1 : size_new_maybe_2;
                    savebytes = (byte)(size_old + size_new);
                    for (int i = 0; i < savebytes; i++) {
                        if (i < size_old) {
                            LMBCS[i] = toUBytesArray[i];
                        } else {
                            LMBCS[i] = source.get();
                        }
                    }
                    tmpSourceBuffer = ByteBuffer.wrap(LMBCS);
                    tmpSourceBuffer.limit(savebytes);
                    uniChar = (char)LMBCSGetNextUCharWorker(tmpSourceBuffer, err);
                    source.position(saveSource + tmpSourceBuffer.position() - size_old);
                    errSource = saveSource - size_old;
                    
                    if (err[0].isOverflow()) { /* err == U_TRUNCATED_CHAR_FOUND */ 
                        /* evil special case: source buffers so small a char spans more than 2 buffers */
                        toULength = savebytes;
                        for (int i = 0; i < savebytes; i++) {
                            toUBytesArray[i] = LMBCS[i];
                        }
                        source.position(source.limit());
                        err[0] = CoderResult.UNDERFLOW;
                        return err[0];
                    } else {
                        /* clear the partial-char marker */
                        toULength = 0;
                    }
                } else {
                    errSource = saveSource;
                    uniChar = (char)LMBCSGetNextUCharWorker(source, err);
                    savebytes = (byte)(source.position() - saveSource);
                }
                
                if (err[0].isUnderflow()) {
                    if (uniChar < 0x0fffe) {
                        target.put(uniChar);
                        if (offsets != null) {
                            offsets.put(saveSource);
                        }
                    } else if (uniChar == 0xfffe) {
                        err[0] = CoderResult.unmappableForLength(source.position() - saveSource);
                    } else /* if (uniChar == 0xffff) */ {
                        err[0] = CoderResult.malformedForLength(source.position() - saveSource);
                    }
                }
            }
            /* If target ran out before source, return over flow buffer error. */
            if (err[0].isUnderflow() && source.hasRemaining() && !target.hasRemaining()) {
                err[0] = CoderResult.OVERFLOW;
            } else if (!err[0].isUnderflow()) {
                /* If character incomplete or unmappable/illegal, store it in toUBytesArray[] */
                toULength = savebytes;
                if (savebytes > 0) {
                    for (int i = 0; i < savebytes; i++) {
                        toUBytesArray[i] = source.get(errSource + i);
                    }
                }
                if (err[0].isOverflow()) { /* err == U_TRUNCATED_CHAR_FOUND */
                    err[0] = CoderResult.UNDERFLOW;
                }
            }
            return err[0];
        }
    }
    
    class CharsetEncoderLMBCS extends CharsetEncoderICU {
        public CharsetEncoderLMBCS(CharsetICU cs) {
            super(cs, fromUSubstitution);
            implReset();
        }
        
        protected void implReset() {
            super.implReset();
        }
        /*
         * Here's the basic helper function that we use when converting from
         * Unicode to LMBCS, and we suspect that a Unicode character will fit into
         * one of the 12 groups. The return value is the number of bytes written
         * starting at pStartLMBCS (if any).
         */
        private int LMBCSConversionWorker(short group, byte[] LMBCS, char pUniChar, short[] lastConverterIndex, boolean[] groups_tried) {
            byte pLMBCS = 0;
            UConverterSharedData xcnv = extraInfo.OptGrpConverter[group];
            
            int bytesConverted;
            int[] value = new int[1];
            short firstByte;
            
            extraInfo.charset.sharedData = xcnv;
            bytesConverted = extraInfo.encoder.fromUChar32(pUniChar, value, false);
            
            /* get the first result byte */
            if (bytesConverted > 0) {
                firstByte = (short)((value[0] >> ((bytesConverted - 1) * 8)) & UConverterConstants.UNSIGNED_BYTE_MASK);
            } else {
                /* most common failure mode is an unassigned character */
                groups_tried[group] = true;
                return 0;
            }
            
            lastConverterIndex[0] = group;
            
            /* 
             * All initial byte values in lower ascii range should have been caught by now,
             * except with the exception group.
             */
            
            /* use converted data: first write 0, 1 or two group bytes */
            if (group != ULMBCS_GRP_EXCEPT && extraInfo.OptGroup != group) {
                LMBCS[pLMBCS++] = (byte)group;
                if (bytesConverted == 1 && group >= ULMBCS_DOUBLEOPTGROUP_START) {
                    LMBCS[pLMBCS++] = (byte)group;
                }
            }
            
            /* don't emit control chars */
            if (bytesConverted == 1 && firstByte < 0x20) {
                return 0;
            }
            
            /* then move over the converted data */
            switch (bytesConverted) {
            case 4:
                LMBCS[pLMBCS++] = (byte)(value[0] >> 24);
            case 3:
                LMBCS[pLMBCS++] = (byte)(value[0] >> 16);
            case 2:
                LMBCS[pLMBCS++] = (byte)(value[0] >> 8);
            case 1:
                LMBCS[pLMBCS++] = (byte)value[0];
            default:
                /* will never occur */
                break;
            }
            
            return pLMBCS;
        }
        /*
         * This is a much simpler version of above, when we
         * know we are writing LMBCS using the Unicode group.
         */
        private int LMBCSConvertUni(byte[] LMBCS, char uniChar) {
            int index = 0;
            short LowCh  = (short)(uniChar & UConverterConstants.UNSIGNED_BYTE_MASK);
            short HighCh = (short)((uniChar >> 8) & UConverterConstants.UNSIGNED_BYTE_MASK);
            
            LMBCS[index++] = (byte)ULMBCS_GRP_UNICODE;
            
            if (LowCh == 0) {
                LMBCS[index++] = (byte)ULMBCS_UNICOMPATZERO;
                LMBCS[index++] = (byte)HighCh;
            } else {
                LMBCS[index++] = (byte)HighCh;
                LMBCS[index++] = (byte)LowCh;
            }
            return ULMBCS_UNICODE_SIZE;
        }
        /* The main Unicode to LMBCS conversion function */
        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult err = CoderResult.UNDERFLOW;
            short[] lastConverterIndex = new short[1];
            char uniChar;
            byte[] LMBCS = new byte[ULMBCS_CHARSIZE_MAX];
            byte pLMBCS;
            int bytes_written;
            boolean[] groups_tried = new boolean[ULMBCS_GRP_LAST+1];
            int sourceIndex = 0;
            
            /*
             * Basic strategy: attempt to fill in local LMBCS 1-char buffer.(LMBCS)
             * If that succeeds, see if it will all fit into the target & copy it over
             * if it does.
             * 
             * We try conversions in the following order:
             * 1. Single-byte ascii & special fixed control chars (&null)
             * 2. Look up group in table & try that (could b
             *     A) Unicode group
             *     B) control group
             *     C) national encodeing
             *        or ambiguous SBCS or MBCS group (on to step 4...)
             * 3. If its ambiguous, try this order:
             *     A) The optimization group
             *     B) The locale group
             *     C) The last group that succeeded with this string.
             *     D) every other group that's relevant
             *     E) If its single-byte ambiguous, try the exceptions group
             * 4. And as a grand fallback: Unicode
             */
            while (source.hasRemaining() && err.isUnderflow()) {
                if (!target.hasRemaining()) {
                    err = CoderResult.OVERFLOW;
                    break;
                }
                uniChar = source.get(source.position());
                bytes_written = 0;
                pLMBCS = 0;
                
                /* check cases in rough order of how common they are, for speed */
                
                /* single-byte matches: strategy 1 */
                if (((uniChar > ULMBCS_C0END) && (uniChar < ULMBCS_C1START)) ||
                    uniChar == 0 || uniChar == ULMBCS_HT || uniChar == ULMBCS_CR ||
                    uniChar == ULMBCS_LF || uniChar == ULMBCS_123SYSTEMRANGE) {
                    LMBCS[pLMBCS++] = (byte)uniChar;
                    bytes_written = 1;
                }
                
                if (bytes_written == 0) {
                    /* Check by Unicode rage (Strategy 2) */
                    short group = FindLMBCSUniRange(uniChar);
                    if (group == ULMBCS_GRP_UNICODE) { /* (Strategy 2A) */
                        bytes_written = LMBCSConvertUni(LMBCS, uniChar);
                    } else if (group == ULMBCS_GRP_CTRL) { /* Strategy 2B) */
                        /* Handle control characters here */
                        if (uniChar <= ULMBCS_C0END) {
                            LMBCS[pLMBCS++] = ULMBCS_GRP_CTRL;
                            LMBCS[pLMBCS++] = (byte)(ULMBCS_CTRLOFFSET + uniChar);
                        } else if (uniChar >= ULMBCS_C1START && uniChar <= (ULMBCS_C1START + ULMBCS_CTRLOFFSET)) {
                            LMBCS[pLMBCS++] = ULMBCS_GRP_CTRL;
                            LMBCS[pLMBCS++] = (byte)uniChar;
                        }
                        bytes_written = pLMBCS;
                    } else if (group < ULMBCS_GRP_UNICODE) { /* (Strategy 2C) */
                        /* a specific converter has been identified - use it */
                        bytes_written = LMBCSConversionWorker(group, LMBCS, uniChar, lastConverterIndex, groups_tried);
                    }
                    if (bytes_written == 0) { /* the ambiguous group cases (Strategy 3) */
                        groups_tried = new boolean[ULMBCS_GRP_LAST+1];
                        
                        /* check for non-default optimization group (Strategy 3A) */
                        if (extraInfo.OptGroup != 1 && ULMBCS_AMBIGUOUS_MATCH(group, extraInfo.OptGroup)) {
                            bytes_written = LMBCSConversionWorker(extraInfo.OptGroup, LMBCS, uniChar, lastConverterIndex, groups_tried);
                        }
                        /* check for locale optimization group (Strategy 3B) */
                        if (bytes_written == 0 && extraInfo.localeConverterIndex > 0 &&
                            ULMBCS_AMBIGUOUS_MATCH(group, extraInfo.localeConverterIndex)) {
                            
                            bytes_written = LMBCSConversionWorker(extraInfo.localeConverterIndex, LMBCS, uniChar, lastConverterIndex, groups_tried);
                        }
                        /* check for last optimization group used for this string (Strategy 3C) */
                        if (bytes_written == 0 && lastConverterIndex[0] > 0 &&
                            ULMBCS_AMBIGUOUS_MATCH(group, lastConverterIndex[0])) {
                            
                            bytes_written = LMBCSConversionWorker(lastConverterIndex[0], LMBCS, uniChar, lastConverterIndex, groups_tried);
                        }
                        if (bytes_written == 0) {
                            /* just check every possible matching converter (Strategy 3D) */
                            short grp_start;
                            short grp_end;
                            short grp_ix;
                            
                            grp_start = (group == ULMBCS_AMBIGUOUS_MBCS) ? ULMBCS_DOUBLEOPTGROUP_START : ULMBCS_GRP_L1;
                            grp_end   = (group == ULMBCS_AMBIGUOUS_MBCS) ? ULMBCS_GRP_LAST : ULMBCS_GRP_TH;
                            for (grp_ix = grp_start; grp_ix <= grp_end && bytes_written == 0; grp_ix++) {
                                if (extraInfo.OptGrpConverter[grp_ix] != null && !groups_tried[grp_ix]) {
                                    bytes_written = LMBCSConversionWorker(grp_ix, LMBCS, uniChar, lastConverterIndex, groups_tried);
                                }
                            }
                            /* 
                             * a final conversion fallback to the exceptions group if its likely
                             * to be single byte (Strategy 3E) 
                             */
                            if (bytes_written == 0 && grp_start == ULMBCS_GRP_L1) {
                                bytes_written = LMBCSConversionWorker(ULMBCS_GRP_EXCEPT, LMBCS, uniChar, lastConverterIndex, groups_tried);
                            }
                        }
                        /* all of our other strategies failed. Fallback to Unicode. (Strategy 4) */
                        if (bytes_written == 0) {
                            bytes_written = LMBCSConvertUni(LMBCS, uniChar);
                        }
                    }
                }
                /* we have a translation. increment source and write as much as possible to target */
                source.get();
                pLMBCS = 0;
                while (target.hasRemaining() && bytes_written > 0) {
                    bytes_written--;
                    target.put(LMBCS[pLMBCS++]);
                    if (offsets != null) {
                        offsets.put(sourceIndex);
                    }
                }
                sourceIndex++;
                if (bytes_written > 0) {
                    /*
                     * write any bytes that didn't fit in target to the error buffer,
                     * common code will move this to target if we get called back with
                     * enough target room
                     */
                    err = CoderResult.OVERFLOW;
                    errorBufferLength = bytes_written;
                    for (int i = 0; bytes_written > 0; i++, bytes_written--) {
                        errorBuffer[i] = LMBCS[pLMBCS++];
                    }
                }
            }
            
            return err;
        }
    }
    public CharsetDecoder newDecoder() {
        return new CharsetDecoderLMBCS(this);
    }
    
    public CharsetEncoder newEncoder() {
        return new CharsetEncoderLMBCS(this);
    }
    
    void getUnicodeSetImpl(UnicodeSet setFillIn, int which){
        getCompleteUnicodeSet(setFillIn);
    }
    private byte[] fromUSubstitution = new byte[]{ 0x3F };
}
