// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2000-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genuca.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created at the end of XX century
*   created by: Vladimir Weinstein,
*   modified in 2013-2014 by Markus Scherer
*
*   This program reads the Fractional UCA table and generates
*   internal format for UCA table as well as inverse UCA table.
*   It then writes the ucadata.icu binary file containing the data.
*/

#define U_NO_DEFAULT_INCLUDE_UTF_HEADERS 1

#include <stdio.h>
#include <stdint.h>
#include "unicode/utypes.h"
#include "unicode/errorcode.h"
#include "unicode/localpointer.h"
#include "unicode/ucol.h"
#include "unicode/uscript.h"
#include "unicode/utf8.h"
#include "charstr.h"
#include "cmemory.h"
#include "collation.h"
#include "collationbasedatabuilder.h"
#include "collationdata.h"
#include "collationdatabuilder.h"
#include "collationdatareader.h"
#include "collationdatawriter.h"
#include "collationinfo.h"
#include "collationrootelements.h"
#include "collationruleparser.h"
#include "collationtailoring.h"
#include "cstring.h"
#include "normalizer2impl.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "uparse.h"
#include "writesrc.h"

#if UCONFIG_NO_COLLATION

extern "C" int
main(int argc, char* argv[]) {
    (void)argc;
    (void)argv;
    return 1;
}

#else

U_NAMESPACE_USE

enum HanOrderValue {
    HAN_NO_ORDER = -1,
    HAN_IMPLICIT,
    HAN_RADICAL_STROKE
};

static UBool beVerbose=false, withCopyright=true, icu4xMode=false;

static HanOrderValue hanOrder = HAN_NO_ORDER;

static UVersionInfo UCAVersion={ 0, 0, 0, 0 };

static UDataInfo ucaDataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0x55, 0x43, 0x6f, 0x6c },         // dataFormat="UCol"
    { 5, 0, 0, 0 },                     // formatVersion
    { 6, 3, 0, 0 }                      // dataVersion
};

static char *skipWhiteSpace(char *s) {
    while(*s == ' ' || *s == '\t') { ++s; }
    return s;
}

static int32_t hex2num(char hex) {
    if(hex>='0' && hex <='9') {
        return hex-'0';
    } else if(hex>='a' && hex<='f') {
        return hex-'a'+10;
    } else if(hex>='A' && hex<='F') {
        return hex-'A'+10;
    } else {
        return -1;
    }
}

static uint32_t parseWeight(char *&s, const char *separators,
                            int32_t maxBytes, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return 0; }
    uint32_t weight = 0;
    int32_t numBytes = 0;
    for(;;) {
        // Check one character after another, so that we don't just run over a 00.
        int32_t nibble1, nibble2;
        if((nibble1 = hex2num(s[0])) < 0 || (nibble2 = hex2num(s[1])) < 0) {
            // Stop when we find something other than a pair of hex digits.
            break;
        }
        if(numBytes == maxBytes || (numBytes != 0 && nibble1 == 0 && nibble2 <= 1)) {
            // Too many bytes, or a 00 or 01 byte which is illegal inside a weight.
            errorCode = U_INVALID_FORMAT_ERROR;
            return 0;
        }
        weight = (weight << 8) | ((uint32_t)nibble1 << 4) | (uint32_t)nibble2;
        ++numBytes;
        s += 2;
        if(*s != ' ') {
            break;
        }
        ++s;
    }
    char c = *s;
    if(c == 0 || strchr(separators, c) == nullptr) {
        errorCode = U_INVALID_FORMAT_ERROR;
        return 0;
    }
    // numBytes==0 is ok, for example in [,,] or [, 82, 05]
    // Left-align the weight.
    while(numBytes < 4) {
        weight <<= 8;
        ++numBytes;
    }
    return weight;
}

/**
 * Parse a CE like [0A 86, 05, 17] or [U+4E00, 10].
 * Stop with an error, or else with the pointer s after the closing bracket.
 */
static int64_t parseCE(const CollationDataBuilder &builder, char *&s, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return 0; }
    ++s;  // skip over the '['
    if(s[0] == 'U' && s[1] == '+') {
        // Read a code point and look up its CE.
        // We use this especially for implicit primary weights,
        // so that we can use different algorithms in the FractionalUCA.txt
        // generator and the parser.
        // The generator may not even need to compute any implicit primaries at all.
        s += 2;
        char *end;
        unsigned long longCp = uprv_strtoul(s, &end, 16);
        if(end == s || longCp > 0x10ffff) {
            errorCode = U_INVALID_FORMAT_ERROR;
            return 0;
        }
        UChar32 c = (UChar32)longCp;
        int64_t ce = builder.getSingleCE(c, errorCode);
        if(U_FAILURE(errorCode)) { return 0; }
        s = end;
        if(*s == ']') {  // [U+4E00]
            ++s;
            return ce;
        }
        if(*s != ',') {
            errorCode = U_INVALID_FORMAT_ERROR;
            return 0;
        }
        // Parse the following, secondary or tertiary weight.
        s = skipWhiteSpace(s + 1);
        uint32_t w = parseWeight(s, ",]", 2, errorCode);
        if(U_FAILURE(errorCode)) { return 0; }
        if(*s == ']') {  // [U+4E00, 10]
            ++s;
            // Set the tertiary weight to w.
            return (ce & INT64_C(0xffffffffffff0000)) | (w >> 16);
        }
        // Set the secondary weight to w: [U+9F9C, 70, 20]
        ce = (ce & INT64_C(0xffffffff00000000)) | w;
        // Parse and set the tertiary weight.
        s = skipWhiteSpace(s + 1);
        w = parseWeight(s, "]", 2, errorCode);
        ++s;
        return ce | (w >> 16);
    } else {
        uint32_t p = parseWeight(s, ",", 4, errorCode);
        if(U_FAILURE(errorCode)) { return 0; }
        int64_t ce = (int64_t)p << 32;
        s = skipWhiteSpace(s + 1);
        uint32_t w = parseWeight(s, ",", 2, errorCode);
        if(U_FAILURE(errorCode)) { return 0; }
        ce |= w;
        s = skipWhiteSpace(s + 1);
        w = parseWeight(s, "]", 2, errorCode);
        ++s;
        return ce | (w >> 16);
    }
}

namespace {

// Cached, lazy-init mapping from scripts to sample characters.
UChar32 sampleChars[USCRIPT_CODE_LIMIT] = { U_SENTINEL };

}

// Hardcoded mapping from script sample characters to script codes.
// Pro: Available without complete and updated UCD scripts data,
//      easy to add non-script codes specific to collation.
// Con: Needs manual update for each new script or change in sample character.
static const struct {
    UChar32 sampleChar;
    int32_t script;
} sampleCharsToScripts[] = {
    { 0x00A0, UCOL_REORDER_CODE_SPACE },
    { 0x201C, UCOL_REORDER_CODE_PUNCTUATION },
    { 0x263A, UCOL_REORDER_CODE_SYMBOL },
    { 0x20AC, UCOL_REORDER_CODE_CURRENCY },
    { 0x0034, UCOL_REORDER_CODE_DIGIT },
    { 0x004C, USCRIPT_LATIN },
    { 0x03A9, USCRIPT_GREEK },
    { 0x03E2, USCRIPT_COPTIC },
    { 0x042F, USCRIPT_CYRILLIC },
    { 0x2C00, USCRIPT_GLAGOLITIC },
    { 0x1036B, USCRIPT_OLD_PERMIC },
    { 0x10D3, USCRIPT_GEORGIAN },
    { 0x0531, USCRIPT_ARMENIAN },
    { 0x05D0, USCRIPT_HEBREW },
    { 0x10900, USCRIPT_PHOENICIAN },
    { 0x0800, USCRIPT_SAMARITAN },
    { 0x0628, USCRIPT_ARABIC },
    { 0x0710, USCRIPT_SYRIAC },
    { 0x0840, USCRIPT_MANDAIC },
    { 0x078C, USCRIPT_THAANA },
    { 0x07CA, USCRIPT_NKO },
    { 0x07D8, USCRIPT_NKO },
    { 0x2D30, USCRIPT_TIFINAGH },
    { 0x2D5E, USCRIPT_TIFINAGH },
    { 0x12A0, USCRIPT_ETHIOPIC },
    { 0x0905, USCRIPT_DEVANAGARI },
    { 0x0995, USCRIPT_BENGALI },
    { 0x0A15, USCRIPT_GURMUKHI },
    { 0x0A95, USCRIPT_GUJARATI },
    { 0x0B15, USCRIPT_ORIYA },
    { 0x0B95, USCRIPT_TAMIL },
    { 0x0C15, USCRIPT_TELUGU },
    { 0x0C95, USCRIPT_KANNADA },
    { 0x0D15, USCRIPT_MALAYALAM },
    { 0x0D85, USCRIPT_SINHALA },
    { 0xABC0, USCRIPT_MEITEI_MAYEK },
    { 0xA800, USCRIPT_SYLOTI_NAGRI },
    { 0xA882, USCRIPT_SAURASHTRA },
    { 0x11083, USCRIPT_KAITHI },
    { 0x11152, USCRIPT_MAHAJANI },
    { 0x11183, USCRIPT_SHARADA },
    { 0x11208, USCRIPT_KHOJKI },
    { 0x112BE, USCRIPT_KHUDAWADI },
    { 0x1128F, USCRIPT_MULTANI },
    { 0x11315, USCRIPT_GRANTHA },
    { 0x11412, USCRIPT_NEWA },
    { 0x11484, USCRIPT_TIRHUTA },
    { 0x1158E, USCRIPT_SIDDHAM },
    { 0x1160E, USCRIPT_MODI },
    { 0x11680, USCRIPT_TAKRI },
    { 0x1180B, USCRIPT_DOGRA },
    { 0x11717, USCRIPT_AHOM },
    { 0x11D71, USCRIPT_GUNJALA_GONDI },
    { 0x1B83, USCRIPT_SUNDANESE },
    { 0x11005, USCRIPT_BRAHMI },
    { 0x10A00, USCRIPT_KHAROSHTHI },
    { 0x11C0E, USCRIPT_BHAIKSUKI },
    { 0x0E17, USCRIPT_THAI },
    { 0x0EA5, USCRIPT_LAO },
    { 0xAA80, USCRIPT_TAI_VIET },
    { 0x0F40, USCRIPT_TIBETAN },
    { 0x11C72, USCRIPT_MARCHEN },
    { 0x1C00, USCRIPT_LEPCHA },
    { 0xA840, USCRIPT_PHAGS_PA },
    { 0x1900, USCRIPT_LIMBU },
    { 0x1703, USCRIPT_TAGALOG },
    { 0x1723, USCRIPT_HANUNOO },
    { 0x1743, USCRIPT_BUHID },
    { 0x1763, USCRIPT_TAGBANWA },
    { 0x1A00, USCRIPT_BUGINESE },
    { 0x11EE5, USCRIPT_MAKASAR },
    { 0x1BC0, USCRIPT_BATAK },
    { 0xA930, USCRIPT_REJANG },
    { 0xA90A, USCRIPT_KAYAH_LI },
    { 0x1000, USCRIPT_MYANMAR },
    { 0x10D12, USCRIPT_HANIFI_ROHINGYA },
    { 0x11103, USCRIPT_CHAKMA },
    { 0x1780, USCRIPT_KHMER },
    { 0x1950, USCRIPT_TAI_LE },
    { 0x1980, USCRIPT_NEW_TAI_LUE },
    { 0x1A20, USCRIPT_LANNA },
    { 0xAA00, USCRIPT_CHAM },
    { 0x1B05, USCRIPT_BALINESE },
    { 0xA984, USCRIPT_JAVANESE },
    { 0x1826, USCRIPT_MONGOLIAN },
    { 0x1C5A, USCRIPT_OL_CHIKI },
    { 0x13C4, USCRIPT_CHEROKEE },
    { 0x104B5, USCRIPT_OSAGE },
    { 0x14C0, USCRIPT_CANADIAN_ABORIGINAL },
    { 0x168F, USCRIPT_OGHAM },
    { 0x16A0, USCRIPT_RUNIC },
    { 0x10CA1, USCRIPT_OLD_HUNGARIAN },
    { 0x10C00, USCRIPT_ORKHON },
    { 0xA549, USCRIPT_VAI },
    { 0xA6A0, USCRIPT_BAMUM },
    { 0x16AE6, USCRIPT_BASSA_VAH },
    { 0x1E802, USCRIPT_MENDE },
    { 0x16E40, USCRIPT_MEDEFAIDRIN },
    { 0x1E909, USCRIPT_ADLAM, },
    { 0xAC00, USCRIPT_HANGUL },
    { 0x304B, USCRIPT_HIRAGANA },
    { 0x30AB, USCRIPT_KATAKANA },
    { 0x3105, USCRIPT_BOPOMOFO },
    { 0xA288, USCRIPT_YI },
    { 0xA4D0, USCRIPT_LISU },
    { 0xA4E8, USCRIPT_LISU },
    { 0x16F00, USCRIPT_MIAO },
    { 0x118B4, USCRIPT_WARANG_CITI },
    { 0x11AC0, USCRIPT_PAU_CIN_HAU },
    { 0x16B1C, USCRIPT_PAHAWH_HMONG },
    { 0x10280, USCRIPT_LYCIAN },
    { 0x102A0, USCRIPT_CARIAN },
    { 0x102B7, USCRIPT_CARIAN },
    { 0x10920, USCRIPT_LYDIAN },
    { 0x10300, USCRIPT_OLD_ITALIC },
    { 0x10308, USCRIPT_OLD_ITALIC },
    { 0x10330, USCRIPT_GOTHIC },
    { 0x10414, USCRIPT_DESERET },
    { 0x10450, USCRIPT_SHAVIAN },
    { 0x1BC20, USCRIPT_DUPLOYAN },
    { 0x10480, USCRIPT_OSMANYA },
    { 0x10500, USCRIPT_ELBASAN },
    { 0x10537, USCRIPT_CAUCASIAN_ALBANIAN },
    { 0x110D0, USCRIPT_SORA_SOMPENG },
    { 0x16A4F, USCRIPT_MRO },
    { 0x10000, USCRIPT_LINEAR_B },
    { 0x10647, USCRIPT_LINEAR_A },
    { 0x10800, USCRIPT_CYPRIOT },
    { 0x10A60, USCRIPT_OLD_SOUTH_ARABIAN },
    { 0x10A95, USCRIPT_OLD_NORTH_ARABIAN },
    { 0x10B00, USCRIPT_AVESTAN },
    { 0x10873, USCRIPT_PALMYRENE },
    { 0x10896, USCRIPT_NABATAEAN },
    { 0x108F4, USCRIPT_HATRAN },
    { 0x10840, USCRIPT_IMPERIAL_ARAMAIC },
    { 0x10B40, USCRIPT_INSCRIPTIONAL_PARTHIAN },
    { 0x10B60, USCRIPT_INSCRIPTIONAL_PAHLAVI },
    { 0x10B8F, USCRIPT_PSALTER_PAHLAVI },
    { 0x10AC1, USCRIPT_MANICHAEAN },
    { 0x10AD8, USCRIPT_MANICHAEAN },
    { 0x10F19, USCRIPT_OLD_SOGDIAN },
    { 0x10F42, USCRIPT_SOGDIAN },
    { 0x10380, USCRIPT_UGARITIC },
    { 0x103A0, USCRIPT_OLD_PERSIAN },
    { 0x12000, USCRIPT_CUNEIFORM },
    { 0x13153, USCRIPT_EGYPTIAN_HIEROGLYPHS },
    { 0x109A0, USCRIPT_MEROITIC_CURSIVE },
    { 0x10980, USCRIPT_MEROITIC_HIEROGLYPHS },
    { 0x14400, USCRIPT_ANATOLIAN_HIEROGLYPHS },
    { 0x18229, USCRIPT_TANGUT },
    { 0x5B57, USCRIPT_HAN },
    { 0x11D10, USCRIPT_MASARAM_GONDI },
    { 0x11A0B, USCRIPT_ZANABAZAR_SQUARE },
    { 0x11A5C, USCRIPT_SOYOMBO },
    { 0x1B1C4, USCRIPT_NUSHU },
    { 0xFDD0, USCRIPT_UNKNOWN }  // unassigned-implicit primary weights
};

static int32_t getCharScript(UChar32 c) {
    if (sampleChars[0] < 0) {
        // Lazy-init the script->sample cache.
        for (int32_t script = 0; script < USCRIPT_CODE_LIMIT; ++script) {
            UnicodeString sample = uscript_getSampleUnicodeString((UScriptCode)script);
            if (sample.isEmpty() || sample.hasMoreChar32Than(0, INT32_MAX, 1)) {
                sampleChars[script] = U_SENTINEL;
            } else {
                sampleChars[script] = sample.char32At(0);
            }
        }
    }
    for (int32_t script = 0; script < USCRIPT_CODE_LIMIT; ++script) {
        if (c == sampleChars[script]) {
            return script;
        }
    }
    for(int32_t i = 0; i < UPRV_LENGTHOF(sampleCharsToScripts); ++i) {
        if(c == sampleCharsToScripts[i].sampleChar) {
            return sampleCharsToScripts[i].script;
        }
    }
    return USCRIPT_INVALID_CODE;  // -1
}

/**
 * Maps Unified_Ideograph's to primary CEs in the given order of ranges.
 */
class HanOrder {
public:
    HanOrder(UErrorCode &errorCode) : ranges(errorCode), set(), done(false) {}

    void addRange(UChar32 start, UChar32 end, UErrorCode &errorCode) {
        int32_t length = ranges.size();
        if(length > 0 && (ranges.elementAti(length - 1) + 1) == start) {
            // The previous range end is just before this range start: Merge adjacent ranges.
            ranges.setElementAt(end, length - 1);
        } else {
            ranges.addElement(start, errorCode);
            ranges.addElement(end, errorCode);
        }
        set.add(start, end);
    }

    void setBuilderHanOrder(CollationBaseDataBuilder &builder, UErrorCode &errorCode) {
        if(U_FAILURE(errorCode)) { return; }
        builder.initHanRanges(ranges.getBuffer(), ranges.size(), errorCode);
        done = true;
    }

    void setDone() {
        done = true;
    }

    UBool isDone() { return done; }

    const UnicodeSet &getSet() { return set; }

private:
    UVector32 ranges;
    UnicodeSet set;
    UBool done;
};

static HanOrder *implicitHanOrder = nullptr;
static HanOrder *radicalStrokeOrder = nullptr;

enum ActionType {
  READCE,
  READPRIMARY,
  READBYTE,
  READUNIFIEDIDEOGRAPH,
  READRADICAL,
  READUCAVERSION,
  READLEADBYTETOSCRIPTS,
  IGNORE
};

static struct {
    const char *const name;
    int64_t value;
    const ActionType what_to_do;
} vt[]  = {
    {"[first tertiary ignorable",     0, IGNORE},
    {"[last tertiary ignorable",      0, IGNORE},
    {"[first secondary ignorable",    0, READCE},
    {"[last secondary ignorable",     0, READCE},
    {"[first primary ignorable",      0, READCE},
    {"[last primary ignorable",       0, READCE},
    {"[first variable",               0, READCE},
    {"[last variable",                0, READCE},
    {"[first regular",                0, READCE},
    {"[last regular",                 0, READCE},
    {"[first implicit",               0, READCE},
    {"[last implicit",                0, READCE},
    {"[first trailing",               0, READCE},
    {"[last trailing",                0, READCE},

    {"[Unified_Ideograph",            0, READUNIFIEDIDEOGRAPH},
    {"[radical",                      0, READRADICAL},

    {"[fixed first implicit byte",    0, IGNORE},
    {"[fixed last implicit byte",     0, IGNORE},
    {"[fixed first trail byte",       0, IGNORE},
    {"[fixed last trail byte",        0, IGNORE},
    {"[fixed first special byte",     0, IGNORE},
    {"[fixed last special byte",      0, IGNORE},
    {"[fixed secondary common byte",                  0, READBYTE},
    {"[fixed last secondary common byte",             0, READBYTE},
    {"[fixed first ignorable secondary byte",         0, READBYTE},
    {"[fixed tertiary common byte",                   0, READBYTE},
    {"[fixed first ignorable tertiary byte",          0, READBYTE},
    {"[variable top = ",              0, IGNORE},
    {"[UCA version = ",               0, READUCAVERSION},
    {"[top_byte",                     0, READLEADBYTETOSCRIPTS},
    {"[reorderingTokens",             0, IGNORE},
    {"[categories",                   0, IGNORE},
    {"[first tertiary in secondary non-ignorable",    0, IGNORE},
    {"[last tertiary in secondary non-ignorable",     0, IGNORE},
    {"[first secondary in primary non-ignorable",     0, IGNORE},
    {"[last secondary in primary non-ignorable",      0, IGNORE},
};

static int64_t getOptionValue(const char *name) {
    for (int32_t i = 0; i < UPRV_LENGTHOF(vt); ++i) {
        if(uprv_strcmp(name, vt[i].name) == 0) {
            return vt[i].value;
        }
    }
    return 0;
}

static void readAnOption(
        CollationBaseDataBuilder &builder, char *buffer, UErrorCode *status) {
    for (int32_t cnt = 0; cnt<UPRV_LENGTHOF(vt); cnt++) {
        int32_t vtLen = (int32_t)uprv_strlen(vt[cnt].name);
        if(uprv_strncmp(buffer, vt[cnt].name, vtLen) == 0) {
            ActionType what_to_do = vt[cnt].what_to_do;
            char *pointer = skipWhiteSpace(buffer + vtLen);
            if (what_to_do == IGNORE) { //vt[cnt].what_to_do == IGNORE
                return;
            } else if (what_to_do == READCE) {
                vt[cnt].value = parseCE(builder, pointer, *status);
                if(U_SUCCESS(*status) && *pointer != ']') {
                    *status = U_INVALID_FORMAT_ERROR;
                }
                if(U_FAILURE(*status)) {
                    fprintf(stderr, "Syntax error: unable to parse the CE from line '%s'\n", buffer);
                    return;
                }
            } else if(what_to_do == READPRIMARY) {
                vt[cnt].value = parseWeight(pointer, "]", 4, *status);
                if(U_FAILURE(*status)) {
                    fprintf(stderr, "Value of \"%s\" is not a primary weight\n", buffer);
                    return;
                }
            } else if(what_to_do == READBYTE) {
                vt[cnt].value = parseWeight(pointer, "]", 1, *status) >> 24;
                if(U_FAILURE(*status)) {
                    fprintf(stderr, "Value of \"%s\" is not a valid byte\n", buffer);
                    return;
                }
            } else if(what_to_do == READUNIFIEDIDEOGRAPH) {
                if(implicitHanOrder != nullptr) {
                    fprintf(stderr, "duplicate [Unified_Ideograph] lines\n");
                    *status = U_INVALID_FORMAT_ERROR;
                    return;
                }
                implicitHanOrder = new HanOrder(*status);
                if(U_FAILURE(*status)) { return; }
                for(;;) {
                    if(*pointer == ']') { break; }
                    if(*pointer == 0) {
                        // Missing ] after ranges.
                        *status = U_INVALID_FORMAT_ERROR;
                        return;
                    }
                    char *s = pointer;
                    while(*s != ' ' && *s != '\t' && *s != ']' && *s != '\0') { ++s; }
                    char c = *s;
                    *s = 0;
                    uint32_t start, end;
                    u_parseCodePointRange(pointer, &start, &end, status);
                    *s = c;
                    if(U_FAILURE(*status)) {
                        fprintf(stderr, "Syntax error: unable to parse one of the ranges from line '%s'\n", buffer);
                        *status = U_INVALID_FORMAT_ERROR;
                        return;
                    }
                    implicitHanOrder->addRange((UChar32)start, (UChar32)end, *status);
                    pointer = skipWhiteSpace(s);
                }
                if(hanOrder == HAN_IMPLICIT) {
                    implicitHanOrder->setBuilderHanOrder(builder, *status);
                }
                implicitHanOrder->setDone();
            } else if(what_to_do == READRADICAL) {
                if(radicalStrokeOrder == nullptr) {
                    if(implicitHanOrder == nullptr) {
                        fprintf(stderr, "[radical] section before [Unified_Ideograph] line\n");
                        *status = U_INVALID_FORMAT_ERROR;
                        return;
                    }
                    radicalStrokeOrder = new HanOrder(*status);
                    if(U_FAILURE(*status)) { return; }
                } else if(radicalStrokeOrder->isDone()) {
                    fprintf(stderr, "duplicate [radical] sections\n");
                    *status = U_INVALID_FORMAT_ERROR;
                    return;
                }
                if(uprv_strcmp(pointer, "end]") == 0) {
                    if(radicalStrokeOrder->getSet() != implicitHanOrder->getSet()) {
                        fprintf(stderr, "[radical end]: "
                                "some of [Unified_Ideograph] missing from [radical] lines\n");
                        *status = U_INVALID_FORMAT_ERROR;
                        return;
                    }
                    if(hanOrder == HAN_RADICAL_STROKE) {
                        radicalStrokeOrder->setBuilderHanOrder(builder, *status);
                    }
                    radicalStrokeOrder->setDone();
                } else {
                    // Read Han characters and ranges between : and ].
                    // Ignore the radical data before the :.
                    char *startPointer = uprv_strchr(pointer, ':');
                    char *limitPointer = uprv_strchr(pointer, ']');
                    if(startPointer == nullptr || limitPointer == nullptr ||
                            (startPointer + 1) >= limitPointer) {
                        fprintf(stderr, "[radical]: no Han characters listed between : and ]\n");
                        *status = U_INVALID_FORMAT_ERROR;
                        return;
                    }
                    pointer = startPointer + 1;
                    int32_t length = (int32_t)(limitPointer - pointer);
                    for(int32_t i = 0; i < length;) {
                        UChar32 start;
                        U8_NEXT(pointer, i, length, start);
                        UChar32 end;
                        if(pointer[i] == '-') {
                            ++i;
                            U8_NEXT(pointer, i, length, end);
                        } else {
                            end = start;
                        }
                        if(radicalStrokeOrder->getSet().containsSome(start, end)) {
                            fprintf(stderr, "[radical]: some of U+%04x..U+%04x occur "
                                    "multiple times in the radical-stroke order\n",
                                    start, end);
                            *status = U_INVALID_FORMAT_ERROR;
                            return;
                        }
                        if(!implicitHanOrder->getSet().contains(start, end)) {
                            fprintf(stderr, "[radical]: some of U+%04x..U+%04x are "
                                    "not Unified_Ideograph\n",
                                    start, end);
                            *status = U_INVALID_FORMAT_ERROR;
                            return;
                        }
                        radicalStrokeOrder->addRange(start, end, *status);
                    }
                }
            } else if (what_to_do == READUCAVERSION) {
                u_versionFromString(UCAVersion, pointer);
                if(beVerbose) {
                    char uca[U_MAX_VERSION_STRING_LENGTH];
                    u_versionToString(UCAVersion, uca);
                    printf("UCA version %s\n", uca);
                }
                UVersionInfo UCDVersion;
                u_getUnicodeVersion(UCDVersion);
                if (UCAVersion[0] != UCDVersion[0] || UCAVersion[1] != UCDVersion[1]) {
                    char uca[U_MAX_VERSION_STRING_LENGTH];
                    char ucd[U_MAX_VERSION_STRING_LENGTH];
                    u_versionToString(UCAVersion, uca);
                    u_versionToString(UCDVersion, ucd);
                    // Warning, not error, to permit bootstrapping during a version upgrade.
                    fprintf(stderr, "warning: UCA version %s != UCD version %s\n", uca, ucd);
                }
            } else if (what_to_do == READLEADBYTETOSCRIPTS) {
                if (strstr(pointer, "COMPRESS") != nullptr) {
                    uint16_t leadByte = (hex2num(*pointer++) * 16);
                    leadByte += hex2num(*pointer++);
                    builder.setCompressibleLeadByte(leadByte);
                }
                // We do not need the list of scripts on this line.
            }
            return;
        }
    }
    fprintf(stderr, "Warning: unrecognized option: %s\n", buffer);
}

static UBool
readAnElement(char *line,
        CollationBaseDataBuilder &builder,
        UnicodeString &prefix, UnicodeString &s,
        int64_t ces[32], int32_t &cesLength,
        UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return false;
    }
    int32_t lineLength = (int32_t)uprv_strlen(line);
    while(lineLength>0 && (line[lineLength-1] == '\r' || line[lineLength-1] == '\n')) {
      line[--lineLength] = 0;
    }

    if(lineLength >= 3 && line[0] == (char)0xef &&
            line[1] == (char)0xbb && line[2] == (char)0xbf) {
        // U+FEFF UTF-8 signature byte sequence.
        // Ignore, assuming it is at the start of the file.
        line += 3;
        lineLength -= 3;
    }
    if(line[0] == 0 || line[0] == '#') {
        return false; // just a comment, skip whole line
    }

    // Directives.
    if(line[0] == '[') {
        readAnOption(builder, line, status);
        return false;
    }

    CharString input;
    char *startCodePoint = line;
    char *endCodePoint = strchr(startCodePoint, ';');
    if(endCodePoint == nullptr) {
        fprintf(stderr, "error - line with no code point:\n%s\n", line);
        *status = U_INVALID_FORMAT_ERROR; /* No code point - could be an error, but probably only an empty line */
        return false;
    }

    char *pipePointer = strchr(line, '|');
    if (pipePointer != nullptr) {
        // Read the prefix string which precedes the actual string.
        input.append(startCodePoint, (int32_t)(pipePointer - startCodePoint), *status);
        UChar *prefixChars = prefix.getBuffer(32);
        int32_t prefixSize =
            u_parseString(input.data(),
                          prefixChars, prefix.getCapacity(),
                          nullptr, status);
        if(U_FAILURE(*status)) {
            prefix.releaseBuffer(0);
            fprintf(stderr, "error - parsing of prefix \"%s\" failed: %s\n%s\n",
                    input.data(), line, u_errorName(*status));
            *status = U_INVALID_FORMAT_ERROR;
            return false;
        }
        prefix.releaseBuffer(prefixSize);
        startCodePoint = pipePointer + 1;
        input.clear();
    }

    // Read the string which gets the CE(s) assigned.
    input.append(startCodePoint, (int32_t)(endCodePoint - startCodePoint), *status);
    UChar *uchars = s.getBuffer(32);
    int32_t cSize =
        u_parseString(input.data(),
                      uchars, s.getCapacity(),
                      nullptr, status);
    if(U_FAILURE(*status)) {
        s.releaseBuffer(0);
        fprintf(stderr, "error - parsing of code point(s) \"%s\" failed: %s\n%s\n",
                input.data(), line, u_errorName(*status));
        *status = U_INVALID_FORMAT_ERROR;
        return false;
    }
    s.releaseBuffer(cSize);

    char *pointer = endCodePoint + 1;

    char *commentStart = strchr(pointer, '#');
    if(commentStart == nullptr) {
        commentStart = strchr(pointer, 0);
    }

    cesLength = 0;
    for(;;) {
        pointer = skipWhiteSpace(pointer);
        if(pointer == commentStart) {
            break;
        }
        if(cesLength >= 31) {
            fprintf(stderr, "Error: Too many CEs on line '%s'\n", line);
            *status = U_INVALID_FORMAT_ERROR;
            return false;
        }
        ces[cesLength++] = parseCE(builder, pointer, *status);
        if(U_FAILURE(*status)) {
            fprintf(stderr, "Syntax error parsing CE from line '%s' - %s\n",
                    line, u_errorName(*status));
            return false;
        }
    }

    if(s.length() == 1 && s[0] == 0xfffe) {
        // UCA 6.0 gives U+FFFE a special minimum weight using the
        // byte 02 which is the merge-sort-key separator and illegal for any
        // other characters.
    } else {
        // Rudimentary check for valid bytes in CE weights.
        // For a more comprehensive check see CollationTest::TestRootElements(),
        // intltest collate/CollationTest/TestRootElements
        for (int32_t i = 0; i < cesLength; ++i) {
            int64_t ce = ces[i];
            UBool isCompressible = false;
            for (int j = 7; j >= 0; --j) {
                uint8_t b = (uint8_t)(ce >> (j * 8));
                if(j <= 1) { b &= 0x3f; }  // tertiary bytes use 6 bits
                if (b == 1) {
                    fprintf(stderr, "Warning: invalid UCA weight byte 01 for %s\n", line);
                    return false;
                }
                if (j == 7 && b == 2) {
                    fprintf(stderr, "Warning: invalid UCA primary weight lead byte 02 for %s\n", line);
                    return false;
                }
                if (j == 7) {
                    isCompressible = builder.isCompressibleLeadByte(b);
                } else if (j == 6) {
                    // Primary second bytes 03 and FF are compression terminators.
                    // 02, 03 and FF are usable when the lead byte is not compressible.
                    // 02 is unusable and 03 is the low compression terminator when the lead byte is compressible.
                    if (isCompressible && (b <= 3 || b == 0xff)) {
                        fprintf(stderr, "Warning: invalid UCA primary second weight byte %02X for %s\n",
                                b, line);
                        return false;
                    }
                }
            }
        }
    }

    return true;
}

static void
parseFractionalUCA(const char *filename,
                   CollationBaseDataBuilder &builder,
                   UErrorCode *status)
{
    if(U_FAILURE(*status)) { return; }
    FILE *data = fopen(filename, "r");
    if(data == nullptr) {
        fprintf(stderr, "Couldn't open file: %s\n", filename);
        *status = U_FILE_ACCESS_ERROR;
        return;
    }
    int32_t lineNumber = 0;
    char buffer[30000];

    const Normalizer2* norm = nullptr;
    if (icu4xMode) {
        norm = Normalizer2::getNFDInstance(*status);
    }

    UChar32 maxCodePoint = 0;
    while(!feof(data)) {
        if(U_FAILURE(*status)) {
            fprintf(stderr, "Something returned an error %i (%s) while processing line %u of %s. Exiting...\n",
                *status, u_errorName(*status), (int)lineNumber, filename);
            exit(*status);
        }

        lineNumber++;
        char *line = fgets(buffer, sizeof(buffer), data);
        if(line == nullptr) {
            if(feof(data)) {
                break;
            } else {
                fprintf(stderr, "no more input line and also no EOF!\n");
                *status = U_INVALID_FORMAT_ERROR;
                return;
            }
        }

        UnicodeString prefix;
        UnicodeString s;
        int64_t ces[32];
        int32_t cesLength = 0;
        if(readAnElement(line, builder, prefix, s, ces, cesLength, status)) {
            // we have read the line, now do something sensible with the read data!
            uint32_t p = (uint32_t)(ces[0] >> 32);

            if(s.length() > 1 && s[0] == 0xFDD0) {
                // FractionalUCA.txt contractions starting with U+FDD0
                // are only entered into the inverse table,
                // not into the normal collation data.
                builder.addRootElements(ces, cesLength, *status);
                if(s.length() == 2 && cesLength == 1) {
                    switch(s[1]) {
                    case 0x34:
                        // Lead byte for numeric sorting.
                        builder.setNumericPrimary(p);
                        break;
                    case 0xFF21:
                        builder.addScriptStart(CollationData::REORDER_RESERVED_BEFORE_LATIN, p);
                        break;
                    case 0xFF3A:
                        builder.addScriptStart(CollationData::REORDER_RESERVED_AFTER_LATIN, p);
                        break;
                    default:
                        break;
                    }
                }
            } else {
                UChar32 c = s.char32At(0);
                if(c > maxCodePoint) { maxCodePoint = c; }

                // We ignore the CEs for U+FFFD..U+FFFF and for the unassigned first primary.
                // CollationBaseDataBuilder::init() maps them to special CEs.
                // Except for U+FFFE, these have higher primaries in v2 than in FractionalUCA.txt.
                if(0xfffd <= c && c <= 0xffff) { continue; }
                if (icu4xMode) {
                    if (c >= 0xAC00 && c <= 0xD7A3) {
                        // Hangul syllable
                        continue;
                    }
                    if (c >= 0xD800 && c < 0xE000) {
                        // Surrogate
                        continue;
                    }
                    UnicodeString src;
                    UnicodeString dst;
                    src.append(c);
                    norm->normalize(src, dst, *status);
                    if (src != dst) {
                        // c decomposed, skip it
                        continue;
                    }
                }
                if(s.length() >= 2 && c == 0xFDD1) {
                    UChar32 c2 = s.char32At(1);
                    int32_t script = getCharScript(c2);
                    if(script < 0) {
                        fprintf(stderr,
                                "Error: Unknown script for first-primary sample character "
                                "U+%04X on line %u of %s:\n"
                                "%s\n"
                                "    (add the character to genuca.cpp sampleCharsToScripts[])\n",
                                c2, (int)lineNumber, filename, line);
                        exit(U_INVALID_FORMAT_ERROR);
                    }
                    if(script == USCRIPT_UNKNOWN) {
                        // FDD1 FDD0, first unassigned-implicit primary
                        builder.addScriptStart(script, Collation::FIRST_UNASSIGNED_PRIMARY);
                        continue;
                    }
                    builder.addScriptStart(script, p);
                    if(script == USCRIPT_HIRAGANA) {
                        builder.addScriptStart(USCRIPT_KATAKANA_OR_HIRAGANA, p);
                    } else if(script == USCRIPT_HAN) {
                        builder.addScriptStart(USCRIPT_SIMPLIFIED_HAN, p);
                        builder.addScriptStart(USCRIPT_TRADITIONAL_HAN, p);
                    }
                }

                if(0xe0000000 <= p && p < 0xf0000000) {
                    fprintf(stderr,
                            "Error: Unexpected mapping to an implicit or trailing primary"
                            " on line %u of %s:\n"
                            "%s\n",
                            (int)lineNumber, filename, line);
                    exit(U_INVALID_FORMAT_ERROR);
                }
                builder.add(prefix, s, ces, cesLength, *status);
            }
        }
    }

    int32_t numRanges = 0;
    int32_t numRangeCodePoints = 0;
    UChar32 rangeFirst = U_SENTINEL;
    UChar32 rangeLast = U_SENTINEL;
    uint32_t rangeFirstPrimary = 0;
    uint32_t rangeLastPrimary = 0;
    int32_t rangeStep = -1;

    // Detect ranges of characters in primary code point order,
    // with 3-byte primaries and
    // with consistent "step" differences between adjacent primaries.
    // This relies on the FractionalUCA generator using the same primary-weight incrementation.
    // Start at U+0180: No ranges for common Latin characters.
    // Go one beyond maxCodePoint in case a range ends there.
    for(UChar32 c = 0x180; c <= (maxCodePoint + 1); ++c) {
        UBool action;
        uint32_t p = builder.getLongPrimaryIfSingleCE(c);
        if(p != 0) {
            // p is a "long" (three-byte) primary.
            if(rangeFirst >= 0 && c == (rangeLast + 1) && p > rangeLastPrimary) {
                // Find the offset between the two primaries.
                int32_t step = CollationBaseDataBuilder::diffThreeBytePrimaries(
                    rangeLastPrimary, p, builder.isCompressiblePrimary(p));
                if(rangeFirst == rangeLast && step >= 2) {
                    // c == rangeFirst + 1, store the "step" between range primaries.
                    rangeStep = step;
                    rangeLast = c;
                    rangeLastPrimary = p;
                    action = 0;  // continue range
                } else if(rangeStep == step) {
                    // Continue the range with the same "step" difference.
                    rangeLast = c;
                    rangeLastPrimary = p;
                    action = 0;  // continue range
                } else {
                    action = 1;  // maybe finish range, start a new one
                }
            } else {
                action = 1;  // maybe finish range, start a new one
            }
        } else {
            action = -1;  // maybe finish range, do not start a new one
        }
        if(action != 0 && rangeFirst >= 0) {
            // Finish a range.
            // Set offset CE32s for a long range, leave single CEs for a short range.
            UBool didSetRange = builder.maybeSetPrimaryRange(
                rangeFirst, rangeLast,
                rangeFirstPrimary, rangeStep, *status);
            if(U_FAILURE(*status)) {
                fprintf(stderr,
                        "failure setting code point order range U+%04lx..U+%04lx "
                        "%08lx..%08lx step %d - %s\n",
                        (long)rangeFirst, (long)rangeLast,
                        (long)rangeFirstPrimary, (long)rangeLastPrimary,
                        (int)rangeStep, u_errorName(*status));
            } else if(didSetRange) {
                int32_t rangeLength = rangeLast - rangeFirst + 1;
                if(beVerbose) {
                    printf("* set code point order range U+%04lx..U+%04lx [%d] "
                            "%08lx..%08lx step %d\n",
                            (long)rangeFirst, (long)rangeLast,
                            (int)rangeLength,
                            (long)rangeFirstPrimary, (long)rangeLastPrimary,
                            (int)rangeStep);
                }
                ++numRanges;
                numRangeCodePoints += rangeLength;
            }
            rangeFirst = U_SENTINEL;
            rangeStep = -1;
        }
        if(action > 0) {
            // Start a new range.
            rangeFirst = rangeLast = c;
            rangeFirstPrimary = rangeLastPrimary = p;
        }
    }
    printf("** set %d ranges with %d code points\n", (int)numRanges, (int)numRangeCodePoints);

    // Idea: Probably best to work in two passes.
    // Pass 1 for reading all data, setting isCompressible flags (and reordering groups)
    // and finding ranges.
    // Then set the ranges in a newly initialized builder
    // for optimal compression (makes sure that adjacent blocks can overlap easily).
    // Then set all mappings outside the ranges.
    //
    // In the first pass, we could store mappings in a simple list,
    // with single-character/single-long-primary-CE mappings in a UTrie2;
    // or store the mappings in a temporary builder;
    // or we could just parse the input file again in the second pass.
    //
    // Ideally set/copy U+0000..U+017F before setting anything else,
    // then set default Han/Hangul, then set the ranges, then copy non-range mappings.
    // It should be easy to copy mappings from an un-built builder to a new one.
    // Add CollationDataBuilder::copyFrom(builder, code point, errorCode) -- copy contexts & expansions.

    if(UCAVersion[0] == 0 && UCAVersion[1] == 0 && UCAVersion[2] == 0 && UCAVersion[3] == 0) {
        fprintf(stderr, "UCA version not specified. Cannot create data file!\n");
        fclose(data);
        return;
    }

    if (beVerbose) {
        printf("\nLines read: %u\n", (int)lineNumber);
    }

    fclose(data);

    return;
}

static void
buildAndWriteBaseData(CollationBaseDataBuilder &builder,
                      const char *path, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    if(getOptionValue("[fixed secondary common byte") != Collation::COMMON_BYTE) {
        fprintf(stderr, "error: unexpected [fixed secondary common byte]");
        errorCode = U_INVALID_FORMAT_ERROR;
        return;
    }
    if(getOptionValue("[fixed tertiary common byte") != Collation::COMMON_BYTE) {
        fprintf(stderr, "error: unexpected [fixed tertiary common byte]");
        errorCode = U_INVALID_FORMAT_ERROR;
        return;
    }

    CollationData data(*Normalizer2Factory::getNFCImpl(errorCode));
    builder.enableFastLatin();
    builder.build(data, errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "builder.build() failed: %s\n",
                u_errorName(errorCode));
        return;
    }

    // The CollationSettings constructor gives us the properly encoded
    // default options, so that we need not duplicate them here.
    CollationSettings settings;

    UVector32 rootElements(errorCode);
    for(int32_t i = 0; i < CollationRootElements::IX_COUNT; ++i) {
        rootElements.addElement(0, errorCode);
    }
    builder.buildRootElementsTable(rootElements, errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "builder.buildRootElementsTable() failed: %s\n",
                u_errorName(errorCode));
        return;
    }
    int32_t index = CollationRootElements::IX_COUNT;
    rootElements.setElementAt(index, CollationRootElements::IX_FIRST_TERTIARY_INDEX);

    while((rootElements.elementAti(index) & 0xffff0000) == 0) { ++index; }
    rootElements.setElementAt(index, CollationRootElements::IX_FIRST_SECONDARY_INDEX);

    while((rootElements.elementAti(index) & CollationRootElements::SEC_TER_DELTA_FLAG) != 0) {
        ++index;
    }
    rootElements.setElementAt(index, CollationRootElements::IX_FIRST_PRIMARY_INDEX);

    rootElements.setElementAt(Collation::COMMON_SEC_AND_TER_CE,
                              CollationRootElements::IX_COMMON_SEC_AND_TER_CE);

    int32_t secTerBoundaries = (int32_t)getOptionValue("[fixed last secondary common byte") << 24;
    secTerBoundaries |= (int32_t)getOptionValue("[fixed first ignorable secondary byte") << 16;
    secTerBoundaries |= (int32_t)getOptionValue("[fixed first ignorable tertiary byte");
    rootElements.setElementAt(secTerBoundaries, CollationRootElements::IX_SEC_TER_BOUNDARIES);

    LocalMemory<uint8_t> buffer;
    int32_t capacity = 1000000;
    uint8_t *dest = buffer.allocateInsteadAndCopy(capacity);
    if(dest == nullptr) {
        fprintf(stderr, "memory allocation (%ld bytes) for file contents failed\n",
                (long)capacity);
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    int32_t indexes[CollationDataReader::IX_TOTAL_SIZE + 1];
    int32_t totalSize = CollationDataWriter::writeBase(
            data, settings,
            rootElements.getBuffer(), rootElements.size(),
            indexes, dest, capacity,
            errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "CollationDataWriter::writeBase(capacity = %ld) failed: %s\n",
                (long)capacity, u_errorName(errorCode));
        return;
    }
    printf("*** CLDR root collation part sizes ***\n");
    CollationInfo::printSizes(totalSize, indexes);
    printf("*** CLDR root collation size:   %6ld (with file header but no copyright string)\n",
           (long)totalSize + 32);  // 32 bytes = DataHeader rounded up to 16-byte boundary

    CollationTailoring::makeBaseVersion(UCAVersion, ucaDataInfo.dataVersion);
    const char *dataName =
        hanOrder == HAN_IMPLICIT ?
            (icu4xMode ? "ucadata-implicithan-icu4x" : "ucadata-implicithan") :
            (icu4xMode ? "ucadata-unihan-icu4x" : "ucadata-unihan");
    UNewDataMemory *pData=udata_create(path, "icu", dataName, &ucaDataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : nullptr, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genuca: udata_create(%s, ucadata.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pData, dest, totalSize);
    long dataLength = udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genuca: error %s writing the output file\n", u_errorName(errorCode));
        return;
    }

    if(dataLength != (long)totalSize) {
        fprintf(stderr,
                "udata_finish(ucadata.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)totalSize);
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
}

/**
 * Adds each lead surrogate to the bmp set if any of the 1024
 * associated supplementary code points is in the supp set.
 * These can be one and the same set.
 */
static void
setLeadSurrogatesForAssociatedSupplementary(UnicodeSet &bmp, const UnicodeSet &supp) {
    UChar32 c = 0x10000;
    for(UChar lead = 0xd800; lead < 0xdc00; ++lead, c += 0x400) {
        if(supp.containsSome(c, c + 0x3ff)) {
            bmp.add(lead);
        }
    }
}

static int32_t
makeBMPFoldedBitSet(const UnicodeSet &set, uint8_t index[0x800], uint32_t bits[256],
                    UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return 0; }
    bits[0] = 0;  // no bits set
    bits[1] = 0xffffffff;  // all bits set
    int32_t bitsLength = 2;
    int32_t i = 0;
    for(UChar32 c = 0; c <= 0xffff; c += 0x20, ++i) {
        if(set.containsNone(c, c + 0x1f)) {
            index[i] = 0;
        } else if(set.contains(c, c + 0x1f)) {
            index[i] = 1;
        } else {
            uint32_t b = 0;
            for(int32_t j = 0; j <= 0x1f; ++j) {
                if(set.contains(c + j)) {
                    b |= (uint32_t)1 << j;
                }
            }
            int32_t k;
            for(k = 2;; ++k) {
                if(k == bitsLength) {
                    // new bit combination
                    if(bitsLength == 256) {
                        errorCode = U_BUFFER_OVERFLOW_ERROR;
                        return 0;
                    }
                    bits[bitsLength++] = b;
                    break;
                }
                if(bits[k] == b) {
                    // duplicate bit combination
                    break;
                }
            }
            index[i] = k;
        }
    }
    return bitsLength;
}

// TODO: Make preparseucd.py write fcd_data.h mapping code point ranges to FCD16 values,
// use that rather than properties APIs.
// Then consider moving related logic for the unsafeBwdSet back from the loader into this builder.

/**
 * Builds data for the FCD check fast path.
 * For details see the CollationFCD class comments.
 */
static void
buildAndWriteFCDData(const char *path, UErrorCode &errorCode) {
    UnicodeSet lcccSet(UNICODE_STRING_SIMPLE("[[:^lccc=0:][\\udc00-\\udfff]]"), errorCode);
    UnicodeSet tcccSet(UNICODE_STRING_SIMPLE("[:^tccc=0:]"), errorCode);
    if(U_FAILURE(errorCode)) { return; }
    setLeadSurrogatesForAssociatedSupplementary(tcccSet, tcccSet);
    // The following supp(lccc)->lead(tccc) should be unnecessary
    // after the previous supp(tccc)->lead(tccc)
    // because there should not be any characters with lccc!=0 and tccc=0.
    // It is safe and harmless.
    setLeadSurrogatesForAssociatedSupplementary(tcccSet, lcccSet);
    setLeadSurrogatesForAssociatedSupplementary(lcccSet, lcccSet);
    uint8_t lcccIndex[0x800], tcccIndex[0x800];
    uint32_t lcccBits[256], tcccBits[256];
    int32_t lcccBitsLength = makeBMPFoldedBitSet(lcccSet, lcccIndex, lcccBits, errorCode);
    int32_t tcccBitsLength = makeBMPFoldedBitSet(tcccSet, tcccIndex, tcccBits, errorCode);
    printf("@@@ lcccBitsLength=%d -> %d bytes\n", lcccBitsLength, 0x800 + lcccBitsLength * 4);
    printf("@@@ tcccBitsLength=%d -> %d bytes\n", tcccBitsLength, 0x800 + tcccBitsLength * 4);

    if(U_FAILURE(errorCode)) { return; }

    FILE *f=usrc_create(path, "collationfcd.cpp", 2016,
                        "icu/tools/unicode/c/genuca/genuca.cpp");
    if(f==nullptr) {
        errorCode=U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#include \"unicode/utypes.h\"\n\n", f);
    fputs("#if !UCONFIG_NO_COLLATION\n\n", f);
    fputs("#include \"collationfcd.h\"\n\n", f);
    fputs("U_NAMESPACE_BEGIN\n\n", f);
    usrc_writeArray(f,
        "const uint8_t CollationFCD::lcccIndex[%ld]={\n",
        lcccIndex, 8, 0x800,
        "", "\n};\n\n");
    usrc_writeArray(f,
        "const uint32_t CollationFCD::lcccBits[%ld]={\n",
        lcccBits, 32, lcccBitsLength,
        "", "\n};\n\n");
    usrc_writeArray(f,
        "const uint8_t CollationFCD::tcccIndex[%ld]={\n",
        tcccIndex, 8, 0x800,
        "", "\n};\n\n");
    usrc_writeArray(f,
        "const uint32_t CollationFCD::tcccBits[%ld]={\n",
        tcccBits, 32, tcccBitsLength,
        "", "\n};\n\n");
    fputs("U_NAMESPACE_END\n\n", f);
    fputs("#endif  // !UCONFIG_NO_COLLATION\n", f);
    fclose(f);
}

static void
parseAndWriteCollationRootData(
        const char *fracUCAPath,
        const char *binaryDataPath,
        const char *sourceCodePath,
        UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    CollationBaseDataBuilder builder(icu4xMode, errorCode);
    builder.init(errorCode);
    parseFractionalUCA(fracUCAPath, builder, &errorCode);
    buildAndWriteBaseData(builder, binaryDataPath, errorCode);
    buildAndWriteFCDData(sourceCodePath, errorCode);
}

// ------------------------------------------------------------------------- ***

enum {
    HELP_H,
    HELP_QUESTION_MARK,
    VERBOSE,
    COPYRIGHT,
    HAN_ORDER,
    ICU4X
};

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_COPYRIGHT,
    UOPTION_DEF("hanOrder", '\x01', UOPT_REQUIRES_ARG),
    UOPTION_DEF("icu4x", 'X', UOPT_NO_ARG)
};

extern "C" int
main(int argc, char* argv[]) {
    U_MAIN_INIT_ARGS(argc, argv);

    argc=u_parseArgs(argc, argv, UPRV_LENGTHOF(options), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(options[HAN_ORDER].doesOccur) {
        const char *order = options[HAN_ORDER].value;
        if(uprv_strcmp(order, "implicit") == 0) {
            hanOrder = HAN_IMPLICIT;
        } else if(uprv_strcmp(order, "radical-stroke") == 0) {
            hanOrder = HAN_RADICAL_STROKE;
        }
    }
    if(hanOrder == HAN_NO_ORDER) {
        argc = -1;
    }
    if( argc<2 ||
        options[HELP_H].doesOccur || options[HELP_QUESTION_MARK].doesOccur
    ) {
        /*
         * Broken into chunks because the C89 standard says the minimum
         * required supported string length is 509 bytes.
         */
        fprintf(stderr,
            "Usage: %s [-options] --hanOrder (implicit|radical-stroke) path/to/ICU/src/root\n"
            "\n"
            "Reads path/to/ICU/src/root/source/data/unidata/FractionalUCA.txt and\n"
            "writes source and binary data files with the collation root data.\n"
            "\n",
            argv[0]);
        fprintf(stderr,
            "Options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-v or --verbose     verbose output\n"
            "\t-c or --copyright   include a copyright notice\n"
            "\t      --hanOrder    implicit or radical-stroke\n");
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    beVerbose=options[VERBOSE].doesOccur;
    withCopyright=options[COPYRIGHT].doesOccur;
    icu4xMode=options[ICU4X].doesOccur;

    IcuToolErrorCode errorCode("genuca");

    CharString icuSrcRoot(argv[1], errorCode);

    CharString icuSource(icuSrcRoot, errorCode);
    icuSource.appendPathPart("source", errorCode);

    CharString icuSourceData(icuSource, errorCode);
    icuSourceData.appendPathPart("data", errorCode);

    CharString fracUCAPath(icuSourceData, errorCode);
    fracUCAPath.appendPathPart("unidata", errorCode);
    fracUCAPath.appendPathPart("FractionalUCA.txt", errorCode);

    CharString sourceDataInColl(icuSourceData, errorCode);
    sourceDataInColl.appendPathPart("in", errorCode);
    sourceDataInColl.appendPathPart("coll", errorCode);

    CharString sourceI18n(icuSource, errorCode);
    sourceI18n.appendPathPart("i18n", errorCode);

    errorCode.assertSuccess();

    parseAndWriteCollationRootData(
        fracUCAPath.data(),
        sourceDataInColl.data(),
        sourceI18n.data(),
        errorCode);

    return errorCode;
}

#endif  // UCONFIG_NO_COLLATION
