// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 1999-2015, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  namespropsbuilder.cpp (was gennames/gennames.c)
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep30
*   created by: Markus W. Scherer
*
*   This builder reads Unicode character names and aliases,
*   tokenizes and compresses them, and builds
*   compact binary tables for random-access lookup
*   in a u_charName() API function.
*
* unames.icu file format (after UDataInfo header etc. - see udata.c)
* (all data is static const)
*
* UDataInfo fields:
*   dataFormat "unam"
*   formatVersion 1.0
*   dataVersion = Unicode version from -u or --unicode command line option, defaults to 3.0.0
*
* -- data-based names
* uint32_t tokenStringOffset,
*          groupsOffset,
*          groupStringOffset,
*          algNamesOffset;
*
* uint16_t tokenCount;
* uint16_t tokenTable[tokenCount];
*
* char     tokenStrings[]; -- padded to even count
*
* -- strings (groupStrings) are tokenized as follows:
*   for each character c
*       if(c>=tokenCount) write that character c directly
*   else
*       token=tokenTable[c];
*       if(token==0xfffe) -- lead byte of double-byte token
*           token=tokenTable[c<<8|next character];
*       if(token==-1)
*           write c directly
*       else
*           tokenString=tokenStrings+token; (tokenStrings=start of names data + tokenStringOffset;)
*           append zero-terminated tokenString;
*
*    Different strings for a code point - normal name, 1.0 name, and ISO comment -
*    are separated by ';'.
*
* uint16_t groupCount;
* struct {
*   uint16_t groupMSB; -- for a group of 32 character names stored, this is code point>>5
*   uint16_t offsetHigh; -- group strings are at start of names data + groupStringsOffset + this 32 bit-offset
*   uint16_t offsetLow;
* } groupTable[groupCount];
*
* char     groupStrings[]; -- padded to 4-count
*
* -- The actual, tokenized group strings are not zero-terminated because
*   that would take up too much space.
*   Instead, they are preceeded by their length, written in a variable-length sequence:
*   For each of the 32 group strings, one or two nibbles are stored for its length.
*   Nibbles (4-bit values, half-bytes) are read MSB first.
*   A nibble with a value of 0..11 directly indicates the length of the name string.
*   A nibble n with a value of 12..15 is a lead nibble and forms a value with the following nibble m
*   by (((n-12)<<4)|m)+12, reaching values of 12..75.
*   These lengths are sequentially for each tokenized string, not for the de-tokenized result.
*   For the de-tokenizing, see token description above; the strings immediately follow the
*   32 lengths.
*
* -- algorithmic names
*
* typedef struct AlgorithmicRange {
*     uint32_t rangeStart, rangeEnd;
*     uint8_t algorithmType, algorithmVariant;
*     uint16_t rangeSize;
* } AlgorithmicRange;
*
* uint32_t algRangesCount; -- number of data blocks for ranges of
*               algorithmic names (Unicode 3.0.0: 3, hardcoded in gennames)
*
* struct {
*     AlgorithmicRange algRange;
*     uint8_t algRangeData[]; -- padded to 4-count except in last range
* } algRanges[algNamesCount];
* -- not a real array because each part has a different size
*    of algRange.rangeSize (including AlgorithmicRange)
*
* -- algorithmic range types:
*
* 0 Names are formed from a string prefix that is stored in
*   the algRangeData (zero-terminated), followed by the Unicode code point
*   of the character in hexadecimal digits;
*   algRange.algorithmVariant digits are written
*
* 1 Names are formed by calculating modulo-factors of the code point value as follows:
*   algRange.algorithmVariant is the count of modulo factors
*   algRangeData contains
*       uint16_t factors[algRange.algorithmVariant];
*       char strings[];
*   the first zero-terminated string is written as the prefix; then:
*
*   The rangeStart is subtracted; with the difference, here "code":
*   for(i=algRange.algorithmVariant-1 to 0 step -1)
*       index[i]=code%factor[i];
*       code/=factor[i];
*
*   The strings after the prefix are short pieces that are then appended to the result
*   according to index[0..algRange.algorithmVariant-1].
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "unicode/udata.h"
#include "charstr.h"
#include "cmemory.h"
#include "cstring.h"
#include "genprops.h"
#include "ppucd.h"
#include "uarrsort.h"
#include "uassert.h"
#include "unewdata.h"
#include "uoptions.h"

#define STRING_STORE_SIZE 2000000
#define GROUP_STORE_SIZE 5000

#define GROUP_SHIFT 5
#define LINES_PER_GROUP (1UL<<GROUP_SHIFT)
#define GROUP_MASK (LINES_PER_GROUP-1)

#define MAX_LINE_COUNT 50000
#define MAX_WORD_COUNT 20000
#define MAX_GROUP_COUNT 5000

#define NAME_SEPARATOR_CHAR ';'

/* generator data ----------------------------------------------------------- */

U_NAMESPACE_USE

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x75, 0x6e, 0x61, 0x6d},     /* dataFormat="unam" */
    {1, 0, 0, 0},                 /* formatVersion */
    {3, 0, 0, 0}                  /* dataVersion */
};

static uint8_t stringStore[STRING_STORE_SIZE],
               groupStore[GROUP_STORE_SIZE],
               lineLengths[LINES_PER_GROUP];

static uint32_t lineTop=0, groupBottom, wordBottom=STRING_STORE_SIZE, lineLengthsTop;

typedef struct {
    uint32_t code;
    int16_t length;
    uint8_t *s;
} Line;

typedef struct {
    int32_t weight; /* -(cost for token) + (number of occurences) * (length-1) */
    int16_t count;
    int16_t length;
    uint8_t *s;
} Word;

static Line lines[MAX_LINE_COUNT];
static Word words[MAX_WORD_COUNT];

static uint32_t lineCount=0, wordCount=0;

static int16_t leadByteCount;

#define LEADBYTE_LIMIT 16

static int16_t tokens[LEADBYTE_LIMIT*256];
static uint32_t tokenCount;

/* the structure for algorithmic names needs to be 4-aligned */
struct AlgorithmicRange {
    UChar32 start, end;
    uint8_t type, variant;
    uint16_t size;
};

class NamesPropsBuilder : public PropsBuilder {
public:
    NamesPropsBuilder(UErrorCode &errorCode);
    virtual ~NamesPropsBuilder();

    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void build(UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);

private:
    virtual void setAlgNamesRange(UChar32 start, UChar32 end,
                                  const char *type, const char *prefix, UErrorCode &errorCode);

    CharString algRanges;
    int32_t countAlgRanges;
};

NamesPropsBuilder::NamesPropsBuilder(UErrorCode &errorCode)
        : countAlgRanges(0) {
    for(int i=0; i<256; ++i) {
        tokens[i]=0;
    }
}

NamesPropsBuilder::~NamesPropsBuilder() {
}

void
NamesPropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

/* prototypes --------------------------------------------------------------- */

static void
parseName(const char *name, int16_t length);

static int16_t
skipNoise(const char *line, int16_t start, int16_t limit);

static int16_t
getWord(const char *line, int16_t start, int16_t limit);

static void
compress(UErrorCode &errorCode);

static void
compressLines(void);

static int16_t
compressLine(uint8_t *s, int16_t length, int16_t *pGroupTop);

static int32_t
compareWords(const void *context, const void *word1, const void *word2);

static int16_t
findToken(uint8_t *s, int16_t length);

static Word *
findWord(const char *s, int16_t length);

static Word *
addWord(const char *s, int16_t length);

static void
countWord(Word *word);

static void
addLine(UChar32 code, const char *names[], int16_t lengths[], int16_t count);

static void
addGroup(uint32_t groupMSB, uint8_t *strings, int16_t length);

static uint32_t
addToken(uint8_t *s, int16_t length);

static void
appendLineLength(int16_t length);

static void
appendLineLengthNibble(uint8_t nibble);

static uint8_t *
allocLine(int32_t length);

static uint8_t *
allocWord(uint32_t length);

/* parsing ------------------------------------------------------------------ */

void
NamesPropsBuilder::setProps(const UniProps &props, const UnicodeSet &newValues,
                            UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    if(!newValues.contains(UCHAR_NAME) && !newValues.contains(PPUCD_NAME_ALIAS)) {
        return;
    }

    U_ASSERT(props.start==props.end);

    const char *names[4]={ NULL, NULL, NULL, NULL };
    int16_t lengths[4]={ 0, 0, 0, 0 };

    /* get the character name */
    if(props.name!=NULL) {
        names[0]=props.name;
        lengths[0]=(int16_t)uprv_strlen(props.name);
        parseName(names[0], lengths[0]);
    }

    CharString buffer;
    if(props.nameAlias!=NULL) {
        /*
         * Only use "correction" aliases for now, from Unicode 6.1 NameAliases.txt with 3 fields per line.
         * TODO: Work on ticket #8963 to deal with multiple type:alias pairs per character.
         */
        const char *corr=uprv_strstr(props.nameAlias, "correction=");
        if(corr!=NULL) {
            corr+=11;  // skip "correction="
            const char *limit=uprv_strchr(corr, ',');
            if(limit!=NULL) {
                buffer.append(corr, limit-corr, errorCode);
                names[3]=buffer.data();
                lengths[3]=(int16_t)(limit-corr);
            } else {
                names[3]=corr;
                lengths[3]=(int16_t)uprv_strlen(corr);
            }
            parseName(names[3], lengths[3]);
        }
    }

    addLine(props.start, names, lengths, LENGTHOF(names));
}

static void
parseName(const char *name, int16_t length) {
    int16_t start=0, limit, wordLength/*, prevStart=-1*/;
    Word *word;

    while(start<length) {
        /* skip any "noise" characters */
        limit=skipNoise(name, start, length);
        if(start<limit) {
            /*prevStart=-1;*/
            start=limit;
        }
        if(start==length) {
            break;
        }

        /* get a word and add it if it is longer than 1 */
        limit=getWord(name, start, length);
        wordLength=(int16_t)(limit-start);
        if(wordLength>1) {
            word=findWord(name+start, wordLength);
            if(word==NULL) {
                word=addWord(name+start, wordLength);
            }
            countWord(word);
        }

#if 0
        /*
         * if there was a word before this
         * (with no noise in between), then add the pair of words, too
         */
        if(prevStart!=-1) {
            wordLength=limit-prevStart;
            word=findWord(name+prevStart, wordLength);
            if(word==NULL) {
                word=addWord(name+prevStart, wordLength);
            }
            countWord(word);
        }
#endif

        /*prevStart=start;*/
        start=limit;
    }
}

static UBool
isWordChar(char c) {
    return ('A'<=c && c<='I') || /* EBCDIC-safe check for letters */
           ('J'<=c && c<='R') ||
           ('S'<=c && c<='Z') ||

           ('0'<=c && c<='9');
}

static int16_t
skipNoise(const char *line, int16_t start, int16_t limit) {
    /* skip anything that is not part of a word in this sense */
    while(start<limit && !isWordChar(line[start])) {
        ++start;
    }

    return start;
}

static int16_t
getWord(const char *line, int16_t start, int16_t limit) {
    char c=0; /* initialize to avoid a compiler warning although the code was safe */

    /* a unicode character name word consists of A-Z0-9 */
    while(start<limit && isWordChar(line[start])) {
        ++start;
    }

    /* include a following space or dash */
    if(start<limit && ((c=line[start])==' ' || c=='-')) {
        ++start;
    }

    return start;
}

void
NamesPropsBuilder::setAlgNamesRange(UChar32 start, UChar32 end,
                                    const char *type,
                                    const char *prefix,  // number of hex digits
                                    UErrorCode &errorCode) {
    /* modulo factors, maximum 8 */
    /* 3 factors: 19, 21, 28, most-to-least-significant */
    static const uint16_t hangulFactors[3]={
        19, 21, 28
    };

    static const char jamo[]=
        "HANGUL SYLLABLE \0"

        "G\0GG\0N\0D\0DD\0R\0M\0B\0BB\0"
        "S\0SS\0\0J\0JJ\0C\0K\0T\0P\0H\0"

        "A\0AE\0YA\0YAE\0EO\0E\0YEO\0YE\0O\0"
        "WA\0WAE\0OE\0YO\0U\0WEO\0WE\0WI\0"
        "YU\0EU\0YI\0I\0"

        "\0G\0GG\0GS\0N\0NJ\0NH\0D\0L\0LG\0LM\0"
        "LB\0LS\0LT\0LP\0LH\0M\0B\0BS\0"
        "S\0SS\0NG\0J\0C\0K\0T\0P\0H";

    int32_t prefixLength=0;
    AlgorithmicRange range;
    uprv_memset(&range, 0, sizeof(AlgorithmicRange));
    int32_t rangeSize=(int32_t)sizeof(AlgorithmicRange);
    range.start=start;
    range.end=end;
    if(0==uprv_strcmp(type, "han")) {
        range.type=0;
        range.variant= end<=0xffff ? 4 : 5;
        prefixLength=uprv_strlen(prefix)+1;
        rangeSize+=prefixLength;
    } else if(0==uprv_strcmp(type, "hangul")) {
        range.type=1;
        range.variant=(uint8_t)LENGTHOF(hangulFactors);
        rangeSize+=(int32_t)sizeof(hangulFactors);
        rangeSize+=(int32_t)sizeof(jamo);
    } else {
        fprintf(stderr, "genprops error: unknown algnamesrange type '%s'\n", prefix);
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    int32_t paddingLength=paddingLength=rangeSize&3;
    if(paddingLength) {
        paddingLength=4-paddingLength;
        rangeSize+=paddingLength;
    }
    range.size=(uint16_t)rangeSize;
    algRanges.append((char *)&range, (int32_t)sizeof(AlgorithmicRange), errorCode);
    if(range.type==0) {  // han
        algRanges.append(prefix, prefixLength, errorCode);
    } else /* type==1 */ {  // hangul
        algRanges.append((char *)hangulFactors, (int32_t)sizeof(hangulFactors), errorCode);
        algRanges.append(jamo, (int32_t)sizeof(jamo), errorCode);
    }
    while(paddingLength) {
        algRanges.append((char)0xaa, errorCode);
        --paddingLength;
    }
    ++countAlgRanges;
}

/* compressing -------------------------------------------------------------- */

static void
compress(UErrorCode &errorCode) {
    uint32_t i, letterCount;
    int16_t wordNumber;

    /* sort the words in reverse order by weight */
    uprv_sortArray(words, wordCount, sizeof(Word),
                   compareWords, NULL, FALSE, &errorCode);

    /* remove the words that do not save anything */
    while(wordCount>0 && words[wordCount-1].weight<1) {
        --wordCount;
    }

    /* count the letters in the token range */
    letterCount=0;
    for(i=LEADBYTE_LIMIT; i<256; ++i) {
        if(tokens[i]==-1) {
            ++letterCount;
        }
    }
    if(!beQuiet) {
        printf("number of letters used in the names: %d\n", (int)letterCount);
    }

    /* do we need double-byte tokens? */
    if(wordCount+letterCount<=256) {
        /* no, single-byte tokens are enough */
        leadByteCount=0;
        for(i=0, wordNumber=0; wordNumber<(int16_t)wordCount; ++i) {
            if(tokens[i]!=-1) {
                tokens[i]=wordNumber;
                if(beVerbose) {
                    printf("tokens[0x%03x]: word%8ld \"%.*s\"\n",
                            (int)i, (long)words[wordNumber].weight,
                            words[wordNumber].length, words[wordNumber].s);
                }
                ++wordNumber;
            }
        }
        tokenCount=i;
    } else {
        /*
         * The tokens that need two token bytes
         * get their weight reduced by their count
         * because they save less.
         */
        tokenCount=256-letterCount;
        for(i=tokenCount; i<wordCount; ++i) {
            words[i].weight-=words[i].count;
        }

        /* sort these words in reverse order by weight */
        errorCode=U_ZERO_ERROR;
        uprv_sortArray(words+tokenCount, wordCount-tokenCount, sizeof(Word),
                        compareWords, NULL, FALSE, &errorCode);

        /* remove the words that do not save anything */
        while(wordCount>0 && words[wordCount-1].weight<1) {
            --wordCount;
        }

        /* how many tokens and lead bytes do we have now? */
        tokenCount=wordCount+letterCount+(LEADBYTE_LIMIT-1);
        /*
         * adjust upwards to take into account that
         * double-byte tokens must not
         * use NAME_SEPARATOR_CHAR as a second byte
         */
        tokenCount+=(tokenCount-256+254)/255;

        leadByteCount=(int16_t)(tokenCount>>8);
        if(leadByteCount<LEADBYTE_LIMIT) {
            /* adjust for the real number of lead bytes */
            tokenCount-=(LEADBYTE_LIMIT-1)-leadByteCount;
        } else {
            /* limit the number of lead bytes */
            leadByteCount=LEADBYTE_LIMIT-1;
            tokenCount=LEADBYTE_LIMIT*256;
            wordCount=tokenCount-letterCount-(LEADBYTE_LIMIT-1);
            /* adjust again to skip double-byte tokens with ';' */
            wordCount-=(tokenCount-256+254)/255;
        }

        /* set token 0 to word 0 */
        tokens[0]=0;
        if(beVerbose) {
            printf("tokens[0x000]: word%8ld \"%.*s\"\n",
                    (long)words[0].weight,
                    words[0].length, words[0].s);
        }
        wordNumber=1;

        /* set the lead byte tokens */
        for(i=1; (int16_t)i<=leadByteCount; ++i) {
            tokens[i]=-2;
        }

        /* set the tokens */
        for(; i<256; ++i) {
            /* if store10Names then the parser set tokens[NAME_SEPARATOR_CHAR]=-1 */
            if(tokens[i]!=-1) {
                tokens[i]=wordNumber;
                if(beVerbose) {
                    printf("tokens[0x%03x]: word%8ld \"%.*s\"\n",
                            (int)i, (long)words[wordNumber].weight,
                            words[wordNumber].length, words[wordNumber].s);
                }
                ++wordNumber;
            }
        }

        /* continue above 255 where there are no letters */
        for(; (uint32_t)wordNumber<wordCount; ++i) {
            if((i&0xff)==NAME_SEPARATOR_CHAR) {
                tokens[i]=-1; /* do not use NAME_SEPARATOR_CHAR as a second token byte */
            } else {
                tokens[i]=wordNumber;
                if(beVerbose) {
                    printf("tokens[0x%03x]: word%8ld \"%.*s\"\n",
                            (int)i, (long)words[wordNumber].weight,
                            words[wordNumber].length, words[wordNumber].s);
                }
                ++wordNumber;
            }
        }
        tokenCount=i; /* should be already tokenCount={i or i+1} */
    }

    if(!beQuiet) {
        printf("number of lead bytes: %d\n", leadByteCount);
        printf("number of single-byte tokens: %lu\n",
            (unsigned long)256-letterCount-leadByteCount);
        printf("number of tokens: %lu\n", (unsigned long)tokenCount);
    }

    compressLines();
}

static void
compressLines() {
    Line *line=NULL;
    uint32_t i=0, inLine, outLine=0xffffffff /* (uint32_t)(-1) */,
             groupMSB=0xffff, lineCount2;
    int16_t groupTop=0;

    /* store the groups like lines, with compressed data after raw strings */
    groupBottom=lineTop;
    lineCount2=lineCount;
    lineCount=0;

    /* loop over all lines */
    while(i<lineCount2) {
        line=lines+i++;
        inLine=line->code;

        /* segment the lines to groups of 32 */
        if(inLine>>GROUP_SHIFT!=groupMSB) {
            /* finish the current group with empty lines */
            while((++outLine&GROUP_MASK)!=0) {
                appendLineLength(0);
            }

            /* store the group like a line */
            if(groupTop>0) {
                if(groupTop>GROUP_STORE_SIZE) {
                    fprintf(stderr, "gennames: group store overflow\n");
                    exit(U_BUFFER_OVERFLOW_ERROR);
                }
                addGroup(groupMSB, groupStore, groupTop);
            }

            /* start the new group */
            lineLengthsTop=0;
            groupTop=0;
            groupMSB=inLine>>GROUP_SHIFT;
            outLine=(inLine&~GROUP_MASK)-1;
        }

        /* write empty lines between the previous line in the group and this one */
        while(++outLine<inLine) {
            appendLineLength(0);
        }

        /* write characters and tokens for this line */
        appendLineLength(compressLine(line->s, line->length, &groupTop));
    }

    /* finish and store the last group */
    if(line && groupMSB!=0xffff) {
        /* finish the current group with empty lines */
        while((++outLine&GROUP_MASK)!=0) {
            appendLineLength(0);
        }

        /* store the group like a line */
        if(groupTop>0) {
            if(groupTop>GROUP_STORE_SIZE) {
                fprintf(stderr, "gennames: group store overflow\n");
                exit(U_BUFFER_OVERFLOW_ERROR);
            }
            addGroup(groupMSB, groupStore, groupTop);
        }
    }

    if(!beQuiet) {
        printf("number of groups: %lu\n", (unsigned long)lineCount);
    }
}

static int16_t
compressLine(uint8_t *s, int16_t length, int16_t *pGroupTop) {
    int16_t start, limit, token, groupTop=*pGroupTop;

    start=0;
    do {
        /* write any "noise" characters */
        limit=skipNoise((char *)s, start, length);
        while(start<limit) {
            groupStore[groupTop++]=s[start++];
        }

        if(start==length) {
            break;
        }

        /* write a word, as token or directly */
        limit=getWord((char *)s, start, length);
        if(limit-start==1) {
            groupStore[groupTop++]=s[start++];
        } else {
            token=findToken(s+start, (int16_t)(limit-start));
            if(token!=-1) {
                if(token>0xff) {
                    groupStore[groupTop++]=(uint8_t)(token>>8);
                }
                groupStore[groupTop++]=(uint8_t)token;
                start=limit;
            } else {
                while(start<limit) {
                    groupStore[groupTop++]=s[start++];
                }
            }
        }
    } while(start<length);

    length=(int16_t)(groupTop-*pGroupTop);
    *pGroupTop=groupTop;
    return length;
}

static int32_t
compareWords(const void *context, const void *word1, const void *word2) {
    /* reverse sort by word weight */
    return ((Word *)word2)->weight-((Word *)word1)->weight;
}

void
NamesPropsBuilder::build(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    if(!beQuiet) {
        puts("* unames.icu stats *");
        printf("size of all names in the database: %lu\n",
            (unsigned long)lineTop);
        printf("number of named Unicode characters: %lu\n",
            (unsigned long)lineCount);
        printf("number of words in the dictionary from these names: %lu\n",
            (unsigned long)wordCount);
    }
    compress(errorCode);
}

/* generate output data ----------------------------------------------------- */

void
NamesPropsBuilder::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData=udata_create(path, "icu", "unames", &dataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, unames.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    uint16_t groupWords[3];
    uint32_t i, groupTop=lineTop, size,
             tokenStringOffset, groupsOffset, groupStringOffset, algNamesOffset;
    long dataLength;
    int16_t token;

    /* first, see how much space we need, and prepare the token strings */
    for(i=0; i<tokenCount; ++i) {
        token=tokens[i];
        if(token!=-1 && token!=-2) {
            tokens[i]=(int16_t)(addToken(words[token].s, words[token].length)-groupTop);
        }
    }

    /*
     * Required padding for data swapping:
     * The token table undergoes a permutation during data swapping when the
     * input and output charsets are different.
     * The token table cannot grow during swapping, so we need to make sure that
     * the table is long enough for successful in-place permutation.
     *
     * We simply round up tokenCount to the next multiple of 256 to account for
     * all possible permutations.
     *
     * An optimization is possible if we only ever swap between ASCII and EBCDIC:
     *
     * If tokenCount>256, then a semicolon (NAME_SEPARATOR_CHAR) is used
     * and will be swapped between ASCII and EBCDIC between
     * positions 0x3b (ASCII semicolon) and 0x5e (EBCDIC semicolon).
     * This should be the only -1 entry in tokens[256..511] on which the data
     * swapper bases its trail byte permutation map (trailMap[]).
     *
     * It would be sufficient to increase tokenCount so that its lower 8 bits
     * are at least 0x5e+1 to make room for swapping between the two semicolons.
     * For values higher than 0x5e, the trail byte permutation map (trailMap[])
     * should always be an identity map, where we do not need additional room.
     */
    i=tokenCount;
    tokenCount=(tokenCount+0xff)&~0xff;
    if(!beQuiet && i<tokenCount) {
        printf("number of tokens[] padding entries for data swapping: %lu\n", (unsigned long)(tokenCount-i));
    }
    for(; i<tokenCount; ++i) {
        if((i&0xff)==NAME_SEPARATOR_CHAR) {
            tokens[i]=-1; /* do not use NAME_SEPARATOR_CHAR as a second token byte */
        } else {
            tokens[i]=0; /* unused token for padding */
        }
    }

    /*
     * Calculate the total size in bytes of the data including:
     * - the offset to the token strings, uint32_t (4)
     * - the offset to the group table, uint32_t (4)
     * - the offset to the group strings, uint32_t (4)
     * - the offset to the algorithmic names, uint32_t (4)
     *
     * - the number of tokens, uint16_t (2)
     * - the token table, uint16_t[tokenCount] (2*tokenCount)
     *
     * - the token strings, each zero-terminated (tokenSize=(lineTop-groupTop)), 2-padded
     *
     * - the number of groups, uint16_t (2)
     * - the group table, { uint16_t groupMSB, uint16_t offsetHigh, uint16_t offsetLow }[6*groupCount]
     *
     * - the group strings (groupTop-groupBottom), 2-padded
     *
     * - the size of the data for the algorithmic names
     */
    tokenStringOffset=4+4+4+4+2+2*tokenCount;
    groupsOffset=(tokenStringOffset+(lineTop-groupTop)+1)&~1;
    groupStringOffset=groupsOffset+2+6*lineCount;
    algNamesOffset=(groupStringOffset+(groupTop-groupBottom)+3)&~3;

    size=algNamesOffset+4+algRanges.length();

    if(!beQuiet) {
        printf("size of the Unicode Names data:\n"
               "total data length %lu, token strings %lu, compressed strings %lu, algorithmic names %lu\n",
                (unsigned long)size, (unsigned long)(lineTop-groupTop),
                (unsigned long)(groupTop-groupBottom), (unsigned long)(4+algRanges.length()));
    }

    /* write the data to the file */
    /* offsets */
    udata_write32(pData, tokenStringOffset);
    udata_write32(pData, groupsOffset);
    udata_write32(pData, groupStringOffset);
    udata_write32(pData, algNamesOffset);

    /* token table */
    udata_write16(pData, (uint16_t)tokenCount);
    udata_writeBlock(pData, tokens, 2*tokenCount);

    /* token strings */
    udata_writeBlock(pData, stringStore+groupTop, lineTop-groupTop);
    if((lineTop-groupTop)&1) {
        /* 2-padding */
        udata_writePadding(pData, 1);
    }

    /* group table */
    udata_write16(pData, (uint16_t)lineCount);
    for(i=0; i<lineCount; ++i) {
        /* groupMSB */
        groupWords[0]=(uint16_t)lines[i].code;

        /* offset */
        uint32_t offset = (uint32_t)((lines[i].s - stringStore)-groupBottom);
        groupWords[1]=(uint16_t)(offset>>16);
        groupWords[2]=(uint16_t)(offset);
        udata_writeBlock(pData, groupWords, 6);
    }

    /* group strings */
    udata_writeBlock(pData, stringStore+groupBottom, groupTop-groupBottom);

    /* 4-align the algorithmic names data */
    udata_writePadding(pData, algNamesOffset-(groupStringOffset+(groupTop-groupBottom)));

    udata_write32(pData, countAlgRanges);
    udata_writeBlock(pData, algRanges.data(), algRanges.length());

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gennames: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=(long)size) {
        fprintf(stderr, "gennames: data length %ld != calculated size %lu\n",
dataLength, (unsigned long)size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

/* helpers ------------------------------------------------------------------ */

static int16_t
findToken(uint8_t *s, int16_t length) {
    int16_t i, token;

    for(i=0; i<(int16_t)tokenCount; ++i) {
        token=tokens[i];
        if(token>=0 && length==words[token].length && 0==uprv_memcmp(s, words[token].s, length)) {
            return i;
        }
    }

    return -1;
}

static Word *
findWord(const char *s, int16_t length) {
    uint32_t i;

    for(i=0; i<wordCount; ++i) {
        if(length==words[i].length && 0==uprv_memcmp(s, words[i].s, length)) {
            return words+i;
        }
    }

    return NULL;
}

static Word *
addWord(const char *s, int16_t length) {
    uint8_t *stringStart;
    Word *word;

    if(wordCount==MAX_WORD_COUNT) {
        fprintf(stderr, "gennames: too many words\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    stringStart=allocWord(length);
    uprv_memcpy(stringStart, s, length);

    word=words+wordCount;

    /*
     * Initialize the weight with the costs for this token:
     * a zero-terminated string and a 16-bit offset.
     */
    word->weight=-(length+1+2);
    word->count=0;
    word->length=length;
    word->s=stringStart;

    ++wordCount;

    return word;
}

static void
countWord(Word *word) {
    /* add to the weight the savings: the length of the word minus 1 byte for the token */
    word->weight+=word->length-1;
    ++word->count;
}

static void
addLine(UChar32 code, const char *names[], int16_t lengths[], int16_t count) {
    uint8_t *stringStart;
    Line *line;
    int16_t i, length;

    if(lineCount==MAX_LINE_COUNT) {
        fprintf(stderr, "gennames: too many lines\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    /* find the last non-empty name */
    while(count>0 && lengths[count-1]==0) {
        --count;
    }
    if(count==0) {
        return; /* should not occur: caller should not have called */
    }

    /* there will be (count-1) separator characters */
    i=count;
    length=count-1;

    /* add lengths of strings */
    while(i>0) {
        length+=lengths[--i];
    }

    /* allocate line memory */
    stringStart=allocLine(length);

    /* copy all strings into the line memory */
    length=0; /* number of chars copied so far */
    for(i=0; i<count; ++i) {
        if(i>0) {
            stringStart[length++]=NAME_SEPARATOR_CHAR;
        }
        if(lengths[i]>0) {
            uprv_memcpy(stringStart+length, names[i], lengths[i]);
            length+=lengths[i];
        }
    }

    line=lines+lineCount;

    line->code=code;
    line->length=length;
    line->s=stringStart;

    ++lineCount;

    /* prevent a character value that is actually in a name from becoming a token */
    while(length>0) {
        tokens[stringStart[--length]]=-1;
    }
}

static void
addGroup(uint32_t groupMSB, uint8_t *strings, int16_t length) {
    uint8_t *stringStart;
    Line *line;

    if(lineCount==MAX_LINE_COUNT) {
        fprintf(stderr, "gennames: too many groups\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    /* store the line lengths first, then the strings */
    lineLengthsTop=(lineLengthsTop+1)/2;
    stringStart=allocLine(lineLengthsTop+length);
    uprv_memcpy(stringStart, lineLengths, lineLengthsTop);
    uprv_memcpy(stringStart+lineLengthsTop, strings, length);

    line=lines+lineCount;

    line->code=groupMSB;
    line->length=length;
    line->s=stringStart;

    ++lineCount;
}

static uint32_t
addToken(uint8_t *s, int16_t length) {
    uint8_t *stringStart;

    stringStart=allocLine(length+1);
    uprv_memcpy(stringStart, s, length);
    stringStart[length]=0;

    return (uint32_t)(stringStart - stringStore);
}

static void
appendLineLength(int16_t length) {
    if(length>=76) {
        fprintf(stderr, "gennames: compressed line too long\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }
    if(length>=12) {
        length-=12;
        appendLineLengthNibble((uint8_t)((length>>4)|12));
    }
    appendLineLengthNibble((uint8_t)length);
}

static void
appendLineLengthNibble(uint8_t nibble) {
    if((lineLengthsTop&1)==0) {
        lineLengths[lineLengthsTop/2]=(uint8_t)(nibble<<4);
    } else {
        lineLengths[lineLengthsTop/2]|=nibble&0xf;
    }
    ++lineLengthsTop;
}

static uint8_t *
allocLine(int32_t length) {
    uint32_t top=lineTop+length;
    uint8_t *p;

    if(top>wordBottom) {
        fprintf(stderr, "gennames allocLine(): out of memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    p=stringStore+lineTop;
    lineTop=top;
    return p;
}

static uint8_t *
allocWord(uint32_t length) {
    uint32_t bottom=wordBottom-length;

    if(lineTop>bottom) {
        fprintf(stderr, "gennames allocWord(): out of memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    wordBottom=bottom;
    return stringStore+bottom;
}

PropsBuilder *
createNamesPropsBuilder(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return NULL; }
    PropsBuilder *pb=new NamesPropsBuilder(errorCode);
    if(pb==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pb;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
