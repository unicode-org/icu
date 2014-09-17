/*
*******************************************************************************
*
*   Copyright (C) 2000-2014, International Business Machines
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
#include "unicode/utypes.h"
#include "unicode/errorcode.h"
#include "unicode/localpointer.h"
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

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

#if UCONFIG_NO_COLLATION

extern "C" int
main(int argc, char* argv[]) {
    (void)argc;
    (void)argv;
    return 1;
}

#else

U_NAMESPACE_USE

static UBool beVerbose=FALSE, withCopyright=TRUE;

static UVersionInfo UCAVersion={ 0, 0, 0, 0 };

static UDataInfo ucaDataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0x55, 0x43, 0x6f, 0x6c },         // dataFormat="UCol"
    { 4, 0, 0, 0 },                     // formatVersion
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
    if(c == 0 || strchr(separators, c) == NULL) {
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

static const struct {
    const char *name;
    int32_t code;
} specialReorderTokens[] = {
    { "TERMINATOR", -2 },  // -2 means "ignore"
    { "LEVEL-SEPARATOR", -2 },
    { "FIELD-SEPARATOR", -2 },
    { "COMPRESS", -3 },
    // The standard name is "PUNCT" but FractionalUCA.txt uses the long form.
    { "PUNCTUATION", UCOL_REORDER_CODE_PUNCTUATION },
    { "IMPLICIT", USCRIPT_HAN },  // Implicit weights are usually for Han characters. Han & unassigned share a lead byte.
    { "TRAILING", -2 },  // We do not reorder trailing weights (those after implicits).
    { "SPECIAL", -2 }  // We must never reorder internal, special CE lead bytes.
};

int32_t getReorderCode(const char* name) {
    int32_t code = CollationRuleParser::getReorderCode(name);
    if (code >= 0) {
        return code;
    }
    for (int32_t i = 0; i < LENGTHOF(specialReorderTokens); ++i) {
        if (0 == strcmp(name, specialReorderTokens[i].name)) {
            return specialReorderTokens[i].code;
        }
    }
    return -1;  // Same as UCHAR_INVALID_CODE or USCRIPT_INVALID_CODE.
}

enum ActionType {
  READCE,
  READPRIMARY,
  READBYTE,
  READUNIFIEDIDEOGRAPH,
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
    for (int32_t i = 0; i < LENGTHOF(vt); ++i) {
        if(uprv_strcmp(name, vt[i].name) == 0) {
            return vt[i].value;
        }
    }
    return 0;
}

static UnicodeString *leadByteScripts = NULL;

static void readAnOption(
        CollationBaseDataBuilder &builder, char *buffer, UErrorCode *status) {
    for (int32_t cnt = 0; cnt<LENGTHOF(vt); cnt++) {
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
                UVector32 unihan(*status);
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
                    unihan.addElement((UChar32)start, *status);
                    unihan.addElement((UChar32)end, *status);
                    pointer = skipWhiteSpace(s);
                }
                builder.initHanRanges(unihan.getBuffer(), unihan.size(), *status);
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
                uint16_t leadByte = (hex2num(*pointer++) * 16);
                leadByte += hex2num(*pointer++);

                if(0xe0 <= leadByte && leadByte < Collation::UNASSIGNED_IMPLICIT_BYTE) {
                    // Extend the Hani range to the end of what this implementation uses.
                    // FractionalUCA.txt assumes a different algorithm for implicit primary weights,
                    // and different high-lead byte ranges.
                    leadByteScripts[leadByte] = leadByteScripts[0xdf];
                    return;
                }

                UnicodeString scripts;
                for(;;) {
                    pointer = skipWhiteSpace(pointer);
                    if (*pointer == ']') {
                        break;
                    }
                    const char *scriptName = pointer;
                    char c;
                    while((c = *pointer) != 0 && c != ' ' && c != '\t' && c != ']') { ++pointer; }
                    if(c == 0) {
                        fprintf(stderr, "Syntax error: unterminated list of scripts: '%s'\n", buffer);
                        *status = U_INVALID_FORMAT_ERROR;
                        return;
                    }
                    *pointer = 0;
                    int32_t reorderCode = getReorderCode(scriptName);
                    *pointer = c;
                    if (reorderCode == -3) {  // COMPRESS
                        builder.setCompressibleLeadByte(leadByte);
                        continue;
                    }
                    if (reorderCode == -2) {
                        continue;  // Ignore "TERMINATOR" etc.
                    }
                    if (reorderCode < 0 || 0xffff < reorderCode) {
                        fprintf(stderr, "Syntax error: unable to parse reorder code from '%s'\n", scriptName);
                        *status = U_INVALID_FORMAT_ERROR;
                        return;
                    }
                    scripts.append((UChar)reorderCode);
                }
                if(!scripts.isEmpty()) {
                    if(leadByteScripts == NULL) {
                        leadByteScripts = new UnicodeString[256];
                    }
                    leadByteScripts[leadByte] = scripts;
                }
            }
            return;
        }
    }
    fprintf(stderr, "Warning: unrecognized option: %s\n", buffer);
}

static UBool
readAnElement(FILE *data,
        CollationBaseDataBuilder &builder,
        UnicodeString &prefix, UnicodeString &s,
        int64_t ces[32], int32_t &cesLength,
        UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return FALSE;
    }
    char buffer[2048];
    char *result = fgets(buffer, 2048, data);
    if(result == NULL) {
        if(feof(data)) {
            return FALSE;
        } else {
            fprintf(stderr, "empty line but no EOF!\n");
            *status = U_INVALID_FORMAT_ERROR;
            return FALSE;
        }
    }
    int32_t buflen = (int32_t)uprv_strlen(buffer);
    while(buflen>0 && (buffer[buflen-1] == '\r' || buffer[buflen-1] == '\n')) {
      buffer[--buflen] = 0;
    }

    if(buffer[0] == 0 || buffer[0] == '#') {
        return FALSE; // just a comment, skip whole line
    }

    // Directives.
    if(buffer[0] == '[') {
        readAnOption(builder, buffer, status);
        return FALSE;
    }

    char *startCodePoint = buffer;
    char *endCodePoint = strchr(startCodePoint, ';');
    if(endCodePoint == NULL) {
        fprintf(stderr, "error - line with no code point!\n");
        *status = U_INVALID_FORMAT_ERROR; /* No code point - could be an error, but probably only an empty line */
        return FALSE;
    } else {
        *endCodePoint = 0;
    }

    char *pipePointer = strchr(buffer, '|');
    if (pipePointer != NULL) {
        // Read the prefix string which precedes the actual string.
        *pipePointer = 0;
        UChar *prefixChars = prefix.getBuffer(32);
        int32_t prefixSize =
            u_parseString(startCodePoint,
                          prefixChars, prefix.getCapacity(),
                          NULL, status);
        if(U_FAILURE(*status)) {
            prefix.releaseBuffer(0);
            fprintf(stderr, "error - parsing of prefix \"%s\" failed: %s\n",
                    startCodePoint, u_errorName(*status));
            *status = U_INVALID_FORMAT_ERROR;
            return FALSE;
        }
        prefix.releaseBuffer(prefixSize);
        startCodePoint = pipePointer + 1;
    }

    // Read the string which gets the CE(s) assigned.
    UChar *uchars = s.getBuffer(32);
    int32_t cSize =
        u_parseString(startCodePoint,
                      uchars, s.getCapacity(),
                      NULL, status);
    if(U_FAILURE(*status)) {
        s.releaseBuffer(0);
        fprintf(stderr, "error - parsing of code point(s) \"%s\" failed: %s\n",
                startCodePoint, u_errorName(*status));
        *status = U_INVALID_FORMAT_ERROR;
        return FALSE;
    }
    s.releaseBuffer(cSize);

    char *pointer = endCodePoint + 1;

    char *commentStart = strchr(pointer, '#');
    if(commentStart == NULL) {
        commentStart = strchr(pointer, 0);
    }

    cesLength = 0;
    for(;;) {
        pointer = skipWhiteSpace(pointer);
        if(pointer == commentStart) {
            break;
        }
        if(cesLength >= 31) {
            fprintf(stderr, "Error: Too many CEs on line '%s'\n", buffer);
            *status = U_INVALID_FORMAT_ERROR;
            return FALSE;
        }
        ces[cesLength++] = parseCE(builder, pointer, *status);
        if(U_FAILURE(*status)) {
            fprintf(stderr, "Syntax error parsing CE from line '%s' - %s\n",
                    buffer, u_errorName(*status));
            return FALSE;
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
            UBool isCompressible = FALSE;
            for (int j = 7; j >= 0; --j) {
                uint8_t b = (uint8_t)(ce >> (j * 8));
                if(j <= 1) { b &= 0x3f; }  // tertiary bytes use 6 bits
                if (b == 1) {
                    fprintf(stderr, "Warning: invalid UCA weight byte 01 for %s\n", buffer);
                    return FALSE;
                }
                if ((j == 7 || j == 3 || j == 1) && b == 2) {
                    fprintf(stderr, "Warning: invalid UCA weight lead byte 02 for %s\n", buffer);
                    return FALSE;
                }
                if (j == 7) {
                    isCompressible = builder.isCompressibleLeadByte(b);
                } else if (j == 6) {
                    // Primary second bytes 03 and FF are compression terminators.
                    // 02, 03 and FF are usable when the lead byte is not compressible.
                    // 02 is unusable and 03 is the low compression terminator when the lead byte is compressible.
                    if (isCompressible && (b <= 3 || b == 0xff)) {
                        fprintf(stderr, "Warning: invalid UCA primary second weight byte %02X for %s\n",
                                b, buffer);
                        return FALSE;
                    }
                }
            }
        }
    }

    return TRUE;
}

static void
parseFractionalUCA(const char *filename,
                   CollationBaseDataBuilder &builder,
                   UErrorCode *status)
{
    if(U_FAILURE(*status)) { return; }
    FILE *data = fopen(filename, "r");
    if(data == NULL) {
        fprintf(stderr, "Couldn't open file: %s\n", filename);
        *status = U_FILE_ACCESS_ERROR;
        return;
    }
    uint32_t line = 0;

    UChar32 maxCodePoint = 0;
    while(!feof(data)) {
        if(U_FAILURE(*status)) {
            fprintf(stderr, "Something returned an error %i (%s) while processing line %u of %s. Exiting...\n",
                *status, u_errorName(*status), (int)line, filename);
            exit(*status);
        }

        line++;

        UnicodeString prefix;
        UnicodeString s;
        int64_t ces[32];
        int32_t cesLength = 0;
        if(readAnElement(data, builder, prefix, s, ces, cesLength, status)) {
            // we have read the line, now do something sensible with the read data!
            uint32_t p = (uint32_t)(ces[0] >> 32);

            if(s.length() > 1 && s[0] == 0xFDD0) {
                // FractionalUCA.txt contractions starting with U+FDD0
                // are only entered into the inverse table,
                // not into the normal collation data.
                builder.addRootElements(ces, cesLength, *status);
                if(s.length() == 2 && s[1] == 0x34 && cesLength == 1) {
                    // Lead byte for numeric sorting.
                    builder.setNumericPrimary(p);
                }
            } else {
                UChar32 c = s.char32At(0);
                if(c > maxCodePoint) { maxCodePoint = c; }

                // We ignore the CEs for U+FFFD..U+FFFF and for the unassigned first primary.
                // CollationBaseDataBuilder::init() maps them to special CEs.
                // Except for U+FFFE, these have higher primaries in v2 than in FractionalUCA.txt.
                if(0xfffd <= c && c <= 0xffff) { continue; }
                if(s.length() == 2 && s[0] == 0xFDD1 && s[1] == 0xFDD0) {
                    continue;
                }

                if(0xe0000000 <= p && p < 0xf0000000) {
                    fprintf(stderr,
                            "Error: Unexpected mapping to an implicit or trailing primary"
                            " on line %u of %s.\n",
                            (int)line, filename);
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
        printf("\nLines read: %u\n", (int)line);
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

    if(leadByteScripts != NULL) {
        uint32_t firstLead = Collation::MERGE_SEPARATOR_BYTE + 1;
        do {
            // Find the range of lead bytes with this set of scripts.
            const UnicodeString &firstScripts = leadByteScripts[firstLead];
            if(firstScripts.isEmpty()) {
                fprintf(stderr, "[top_byte 0x%02X] has no reorderable scripts\n", (int)firstLead);
                errorCode = U_INVALID_FORMAT_ERROR;
                return;
            }
            uint32_t lead = firstLead;
            for(;;) {
                ++lead;
                const UnicodeString &scripts = leadByteScripts[lead];
                // The scripts should either be the same or disjoint.
                // We do not test if all reordering groups have disjoint sets of scripts.
                if(scripts.isEmpty() || firstScripts.indexOf(scripts[0]) < 0) { break; }
                if(scripts != firstScripts) {
                    fprintf(stderr,
                            "[top_byte 0x%02X] includes script %d from [top_byte 0x%02X] "
                            "but not all scripts match\n",
                            (int)firstLead, scripts[0], (int)lead);
                    errorCode = U_INVALID_FORMAT_ERROR;
                    return;
                }
            }
            // lead is one greater than the last lead byte with the same set of scripts as firstLead.
            builder.addReorderingGroup(firstLead, lead - 1, firstScripts, errorCode);
            if(U_FAILURE(errorCode)) { return; }
            firstLead = lead;
        } while(firstLead < Collation::UNASSIGNED_IMPLICIT_BYTE);
        delete[] leadByteScripts;
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
    if(dest == NULL) {
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
    UNewDataMemory *pData=udata_create(path, "icu", "ucadata", &ucaDataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
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

    FILE *f=usrc_create(path, "collationfcd.cpp",
                        "icu/tools/unicode/c/genuca/genuca.cpp");
    if(f==NULL) {
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
        "\n};\n\n");
    usrc_writeArray(f,
        "const uint32_t CollationFCD::lcccBits[%ld]={\n",
        lcccBits, 32, lcccBitsLength,
        "\n};\n\n");
    usrc_writeArray(f,
        "const uint8_t CollationFCD::tcccIndex[%ld]={\n",
        tcccIndex, 8, 0x800,
        "\n};\n\n");
    usrc_writeArray(f,
        "const uint32_t CollationFCD::tcccBits[%ld]={\n",
        tcccBits, 32, tcccBitsLength,
        "\n};\n\n");
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
    CollationBaseDataBuilder builder(errorCode);
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
    COPYRIGHT
};

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_COPYRIGHT
};

extern "C" int
main(int argc, char* argv[]) {
    U_MAIN_INIT_ARGS(argc, argv);

    argc=u_parseArgs(argc, argv, LENGTHOF(options), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if( argc<2 ||
        options[HELP_H].doesOccur || options[HELP_QUESTION_MARK].doesOccur
    ) {
        /*
         * Broken into chunks because the C89 standard says the minimum
         * required supported string length is 509 bytes.
         */
        fprintf(stderr,
            "Usage: %s [-options] path/to/ICU/src/root\n"
            "\n"
            "Reads path/to/ICU/src/root/source/data/unidata/FractionalUCA.txt and\n"
            "writes source and binary data files with the collation root data.\n"
            "\n",
            argv[0]);
        fprintf(stderr,
            "Options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-v or --verbose     verbose output\n"
            "\t-c or --copyright   include a copyright notice\n");
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    beVerbose=options[VERBOSE].doesOccur;
    withCopyright=options[COPYRIGHT].doesOccur;

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
