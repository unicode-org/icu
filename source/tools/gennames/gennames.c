/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gennames.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep30
*   created by: Markus W. Scherer
*
*   This program reads the Unicode character database text file,
*   parses it, and extracts the character code,
*   the "modern" character name, and optionally the
*   Unicode 1.0 character name.
*   It then tokenizes and compresses the names and builds
*   compact binary tables for random-access lookup
*   in a u_charName() API function.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "unicode/udata.h"
#include "unewdata.h"

#define STRING_STORE_SIZE 1000000
#define GROUP_STORE_SIZE 5000

#define GROUP_SHIFT 5
#define LINES_PER_GROUP (1UL<<GROUP_SHIFT)
#define GROUP_MASK (LINES_PER_GROUP-1)

#define MAX_LINE_COUNT 50000
#define MAX_WORD_COUNT 20000
#define MAX_GROUP_COUNT 5000

#define DATA_NAME "unames"
#define DATA_TYPE "dat"
#define VERSION_STRING "unam"
#define NAME_SEPARATOR_CHAR ';'

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x75, 0x6e, 0x61, 0x6d,     /* dataFormat="unam" */
    1, 0, 0, 0,                 /* formatVersion */
    3, 0, 0, 0                  /* dataVersion */
};

static bool_t beVerbose=FALSE, haveCopyright=TRUE;

static uint8_t stringStore[STRING_STORE_SIZE],
               groupStore[GROUP_STORE_SIZE],
               lineLengths[LINES_PER_GROUP];

static uint32_t lineTop=0, wordBottom=STRING_STORE_SIZE, lineLengthsTop;

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

static uint32_t lineCount=0, wordCount=0, groupCount=0;

static int16_t leadByteCount;

#define LEADBYTE_LIMIT 16

static int16_t tokens[LEADBYTE_LIMIT*256];
static uint32_t tokenCount;

/* prototypes --------------------------------------------------------------- */

static void
init();

static void
parseDB(FileStream *in, bool_t store10Names);

static void
parseName(char *name, int16_t length);

static int16_t
getField(char *line, int16_t start, int16_t limit);

static int16_t
skipNoise(char *line, int16_t start, int16_t limit);

static int16_t
getWord(char *line, int16_t start, int16_t limit);

static void
compress();

static void
compressLines();

static int16_t
compressLine(uint8_t *s, int16_t length, int16_t *pGroupTop);

static int
compareWords(const void *word1, const void *word2);

static void
generateData(const char *dataDir);

static uint32_t
generateAlgorithmicData(UNewDataMemory *pData);

static int16_t
findToken(uint8_t *s, int16_t length);

static Word *
findWord(char *s, int16_t length);

static Word *
addWord(char *s, int16_t length);

static void
countWord(Word *word);

static void
addLine(uint32_t code, char *name1, int16_t name1Length, char *name2, int16_t name2Length);

static void
addGroup(uint32_t groupMSB, uint8_t *strings, int16_t length);

static uint32_t
addToken(uint8_t *s, int16_t length);

static void
appendLineLength(int16_t length);

static void
appendLineLengthNibble(uint8_t nibble);

static uint8_t *
allocLine(uint32_t length);

static uint8_t *
allocWord(uint32_t length);

/* -------------------------------------------------------------------------- */

extern int
main(int argc, char *argv[]) {
    FileStream *in;
    char *arg, *filename=NULL;
    const char *destdir = 0;
    int i;
    bool_t store10Names=FALSE;

    if(argc<=1) {
        fprintf(stderr,
            "usage: %s [-1[+|-]] [-v[+|-]] [-c[+|-]] filename\n"
            "\tread the UnicodeData.txt file and \n"
            "\tcreate a binary file " DATA_NAME "." DATA_TYPE " with the character names\n"
            "\toptions:\n"
            "\t\t-1[-]  do not store Unicode 1.0 character names (default)\n"
            "\t\t-1+  do store Unicode 1.0 character names\n"
            "\t\t-v[+|-]  verbose output\n"
            "\t\t-c[+|-]  do (not) include a copyright notice\n"
            "\t\tfilename  absolute path/filename for the\n"
            "\t\t\tUnicode database text file (default: standard input)\n",
            argv[0]);
    }

    for(i=1; i<argc; ++i) {
        arg=argv[i];
        if(arg[0]=='-') {
            switch(arg[1]) {
            case '1':
                store10Names= arg[2]=='+';
                break;
            case 'v':
                beVerbose= arg[2]=='+';
                break;
            case 'c':
                haveCopyright= arg[2]=='+';
                break;
            default:
                break;
            }
        } else {
            filename=arg;
        }
    }

    if(filename==NULL) {
        in=T_FileStream_stdin();
    } else {
        in=T_FileStream_open(filename, "r");
        if(in==NULL) {
            fprintf(stderr, "gennames: unable to open input file %s\n", filename);
            exit(U_FILE_ACCESS_ERROR);
        }
    }

    if (!destdir) {
        destdir = u_getDataDirectory();
    }

    init();
    parseDB(in, store10Names);
    compress();
    generateData(destdir);

    if(in!=T_FileStream_stdin()) {
        T_FileStream_close(in);
    }

    return 0;
}

static void
init() {
    int i;

    for(i=0; i<256; ++i) {
        tokens[i]=0;
    }
}

/* parsing ------------------------------------------------------------------ */

static void
parseDB(FileStream *in, bool_t store10Names) {
    char line[300];
    uint32_t code=0;
    int16_t limit, length, name1Start, name1Length, name2Start, name2Length=0;
    int i;

    while(T_FileStream_readLine(in, line, sizeof(line))!=NULL) {
        length=uprv_strlen(line);

        /* get the character code */
        limit=getField(line, 0, length);
        if(limit<1 || limit==length) {
            fprintf(stderr, "gennames: too few fields at code 0x%lx\n", code);
            exit(U_PARSE_ERROR);
        }
        code=uprv_strtoul(line, NULL, 16);

        /* get the character name */
        name1Start=limit+1;
        if(name1Start>=length) {
            fprintf(stderr, "gennames: too few fields at code 0x%lx\n", code);
            exit(U_PARSE_ERROR);
        }
        limit=getField(line, name1Start, length);

        /* do not store pseudo-names in <> brackets */
        if(line[name1Start]!='<') {
            name1Length=limit-name1Start;
        } else {
            name1Length=0;
        }

        if(store10Names) {
            /* skip 8 fields and get the following one */
            for(i=0; i<9; ++i) {
                name2Start=limit+1;
                if(name2Start>=length) {
                    fprintf(stderr, "gennames: too few fields at code 0x%lx\n", code);
                    exit(U_PARSE_ERROR);
                }
                limit=getField(line, name2Start, length);
            }

            /* get the second character name, the one from Unicode 1.0 */
            /* do not store pseudo-names in <> brackets */
            if(line[name2Start]!='<') {
                name2Length=limit-name2Start;
            } else {
                name2Length=0;
            }
        }

        if(name1Length>0 || name2Length>0) {
            /* printf("%lx:%.*s(%.*s)\n", code, name1Length, line+name1Start, name2Length, line+name2Start); */

            parseName(line+name1Start, name1Length);
            parseName(line+name2Start, name2Length);

            addLine(code, line+name1Start, name1Length, line+name2Start, name2Length);
        }
    }

    printf("size of all names in the database: %lu\n", lineTop);
    printf("number of named Unicode characters: %lu\n", lineCount);
    printf("number of words in the dictionary from these names: %lu\n", wordCount);
}

static void
parseName(char *name, int16_t length) {
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
        wordLength=limit-start;
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

static int16_t
getField(char *line, int16_t start, int16_t limit) {
    while(start<limit && line[start]!=';') {
        ++start;
    }
    return start;
}

static int16_t
skipNoise(char *line, int16_t start, int16_t limit) {
    char c;

    /* skip anything that is not part of a word in this sense */
    while(start<limit && !('A'<=(c=line[start]) && c<='Z' || '0'<=c && c<='9')) {
        ++start;
    }

    return start;
}

static int16_t
getWord(char *line, int16_t start, int16_t limit) {
    char c;

    /* a unicode character name word consists of A-Z0-9 */
    while(start<limit && ('A'<=(c=line[start]) && c<='Z' || '0'<=c && c<='9')) {
        ++start;
    }

    /* include a following space or dash */
    if(start<limit && (c==' ' || c=='-')) {
        ++start;
    }

    return start;
}

/* compressing -------------------------------------------------------------- */

static void
compress() {
    uint32_t i, letterCount;
    int16_t wordNumber;

    /* sort the words in reverse order by weight */
    qsort(words, wordCount, sizeof(Word), compareWords);

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
    printf("number of letters used in the names: %d\n", letterCount);

    /* do we need double-byte tokens? */
    if(wordCount+letterCount<=256) {
        /* no, single-byte tokens are enough */
        leadByteCount=0;
        for(i=0, wordNumber=0; wordNumber<(int16_t)wordCount; ++i) {
            if(tokens[i]!=-1) {
                tokens[i]=wordNumber;
                if(beVerbose) {
                    printf("tokens[0x%03x]: word%8ld \"%.*s\"\n",
                            i, words[wordNumber].weight,
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
        qsort(words+tokenCount, wordCount-tokenCount, sizeof(Word), compareWords);

        /* remove the words that do not save anything */
        while(wordCount>0 && words[wordCount-1].weight<1) {
            --wordCount;
        }

        /* how many tokens and lead bytes do we have now? */
        tokenCount=wordCount+letterCount+(LEADBYTE_LIMIT-1);
        leadByteCount=(int16_t)(tokenCount>>8);
        if(leadByteCount<LEADBYTE_LIMIT) {
            /* adjust for the real number of lead bytes */
            tokenCount-=(LEADBYTE_LIMIT-1)-leadByteCount;
        } else {
            /* limit the number of lead bytes */
            leadByteCount=LEADBYTE_LIMIT-1;
            tokenCount=LEADBYTE_LIMIT*256;
            wordCount=tokenCount-letterCount-(LEADBYTE_LIMIT-1);
        }

        /* set token 0 to word 0 */
        tokens[0]=0;
        if(beVerbose) {
            printf("tokens[0x000]: word%8ld \"%.*s\"\n",
                    words[0].weight,
                    words[0].length, words[0].s);
        }
        wordNumber=1;

        /* set the lead byte tokens */
        for(i=1; (int16_t)i<=leadByteCount; ++i) {
            tokens[i]=-2;
        }

        /* set the tokens */
        for(; i<256; ++i) {
            if(tokens[i]!=-1) {
                tokens[i]=wordNumber;
                if(beVerbose) {
                    printf("tokens[0x%03x]: word%8ld \"%.*s\"\n",
                            i, words[wordNumber].weight,
                            words[wordNumber].length, words[wordNumber].s);
                }
                ++wordNumber;
            }
        }

        /* continue above 255 where there are no letters */
        for(; i<tokenCount; ++i) {
            tokens[i]=wordNumber;
            if(beVerbose) {
                printf("tokens[0x%03x]: word%8ld \"%.*s\"\n",
                        i, words[wordNumber].weight,
                        words[wordNumber].length, words[wordNumber].s);
            }
            ++wordNumber;
        }
    }

    printf("number of lead bytes: %d\n", leadByteCount);
    printf("number of single-byte tokens: %lu\n", 256-letterCount-leadByteCount);
    printf("number of tokens: %lu\n", tokenCount);

    compressLines();
}

static void
compressLines() {
    Line *line;
    uint32_t i=0, inLine, outLine=-1,
             groupMSB=0xffff, lineCount2;
    int16_t groupTop=0;

    /* store the groups like lines, reusing the lines' memory */
    lineTop=0;
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
                if(lineTop>(uint32_t)(line->s-stringStore)) {
                    fprintf(stderr, "gennames: group store runs into string store\n");
                    exit(U_INTERNAL_PROGRAM_ERROR);
                }
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

    printf("number of groups: %lu\n", lineCount);
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

    length=groupTop-*pGroupTop;
    *pGroupTop=groupTop;
    return length;
}

static int
compareWords(const void *word1, const void *word2) {
    /* reverse sort by word weight */
    return ((Word *)word2)->weight-((Word *)word1)->weight;
}

/* generate output data ----------------------------------------------------- */

static void
generateData(const char *dataDir) {
    UNewDataMemory *pData;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint16_t groupWords[3];
    uint32_t i, groupTop=lineTop, offset, size,
             tokenStringOffset, groupsOffset, groupStringOffset, algNamesOffset;
    long dataLength;
    int16_t token;

    pData=udata_create(dataDir, DATA_TYPE, DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gennames: unable to create data memory, error %d\n", errorCode);
        exit(errorCode);
    }

    /* first, see how much space we need, and prepare the token strings */
    for(i=0; i<tokenCount; ++i) {
        token=tokens[i];
        if(token!=-1 && token!=-2) {
            tokens[i]=(int16_t)(addToken(words[token].s, words[token].length)-groupTop);
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
     * - the group strings (groupTop), 2-padded
     *
     * - the size of the data for the algorithmic names
     */
    tokenStringOffset=4+4+4+4+2+2*tokenCount;
    groupsOffset=tokenStringOffset+(lineTop-groupTop+1)&~1;
    groupStringOffset=groupsOffset+2+6*lineCount;
    algNamesOffset=(groupStringOffset+groupTop+3)&~3;

    offset=generateAlgorithmicData(NULL);
    size=algNamesOffset+offset;

    printf("size of the Unicode Names data:\n"
           "total data length %lu, token strings %lu, compressed strings %lu, algorithmic names %lu\n",
            size, (lineTop-groupTop), groupTop, offset);

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
        offset=lines[i].s-stringStore;
        groupWords[1]=(uint16_t)(offset>>16);
        groupWords[2]=(uint16_t)(offset);
        udata_writeBlock(pData, groupWords, 6);
    }

    /* group strings */
    udata_writeBlock(pData, stringStore, groupTop);

    /* 4-align the algorithmic names data */
    udata_writePadding(pData, algNamesOffset-(groupStringOffset+groupTop));

    generateAlgorithmicData(pData);

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gennames: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=(long)size) {
        fprintf(stderr, "gennames: data length %ld != calculated size %lu\n", dataLength, size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

/* the structure for algorithmic names needs to be 4-aligned */
typedef struct AlgorithmicRange {
    uint32_t rangeStart, rangeEnd;
    uint8_t algorithmType, algorithmVariant;
    uint16_t rangeSize;
} AlgorithmicRange;

static uint32_t
generateAlgorithmicData(UNewDataMemory *pData) {
    static char *prefix="CJK UNIFIED IDEOGRAPH-";
#   define PREFIX_LENGTH 23
#   define PREFIX_LENGTH_4 24

    static AlgorithmicRange cjkExtA={
        0x3400, 0x4db5,
        0, 4,
        sizeof(AlgorithmicRange)+PREFIX_LENGTH_4
    };
    static AlgorithmicRange cjk={
        0x4e00, 0x9fa5,
        0, 4,
        sizeof(AlgorithmicRange)+PREFIX_LENGTH_4
    };

    static char jamo[]=
        "HANGUL SYLLABLE \0"

        "G\0GG\0N\0D\0DD\0R\0M\0B\0BB\0"
        "S\0SS\0\0J\0JJ\0C\0K\0T\0P\0H\0"

        "A\0AE\0YA\0YAE\0EO\0E\0YEO\0YE\0O\0"
        "WA\0WAE\0OE\0YO\0U\0WEO\0WE\0WI\0"
        "YU\0EU\0YI\0I\0"

        "\0G\0GG\0GS\0N\0NJ\0NH\0D\0L\0LG\0LM\0"
        "LB\0LS\0LT\0LP\0LH\0M\0B\0BS\0"
        "S\0SS\0NG\0J\0C\0K\0T\0P\0H"
    ;

    static AlgorithmicRange hangul={
        0xac00, 0xd7a3,
        1, 3,
        sizeof(AlgorithmicRange)+6+sizeof(jamo)
    };

    /* modulo factors, maximum 8 */
    /* 3 factors: 19, 21, 28, most-to-least-significant */
    static uint16_t hangulFactors[3]={
        19, 21, 28
    };

    uint32_t size;

    size=0;

    /* number of ranges of algorithmic names */
    if(pData!=NULL) {
        udata_write32(pData, 3);
    } else {
        size+=4;
    }

    /*
     * each range:
     * uint32_t rangeStart
     * uint32_t rangeEnd
     * uint8_t algorithmType
     * uint8_t algorithmVariant
     * uint16_t size of range data
     * uint8_t[size] data
     */

    /* range 0: cjk extension a */
    if(pData!=NULL) {
        udata_writeBlock(pData, &cjkExtA, sizeof(AlgorithmicRange));
        udata_writeString(pData, prefix, PREFIX_LENGTH);
        if(PREFIX_LENGTH<PREFIX_LENGTH_4) {
            udata_writePadding(pData, PREFIX_LENGTH_4-PREFIX_LENGTH);
        }
    } else {
        size+=sizeof(AlgorithmicRange)+PREFIX_LENGTH_4;
    }

    /* range 1: cjk */
    if(pData!=NULL) {
        udata_writeBlock(pData, &cjk, sizeof(AlgorithmicRange));
        udata_writeString(pData, prefix, PREFIX_LENGTH);
        if(PREFIX_LENGTH<PREFIX_LENGTH_4) {
            udata_writePadding(pData, PREFIX_LENGTH_4-PREFIX_LENGTH);
        }
    } else {
        size+=sizeof(AlgorithmicRange)+PREFIX_LENGTH_4;
    }

    /* range 2: hangul syllables */
    if(pData!=NULL) {
        udata_writeBlock(pData, &hangul, sizeof(AlgorithmicRange));
        udata_writeBlock(pData, hangulFactors, 6);
        udata_writeString(pData, jamo, sizeof(jamo));
    } else {
        size+=sizeof(AlgorithmicRange)+6+sizeof(jamo);
    }

    return size;
}

/* helpers ------------------------------------------------------------------ */

static int16_t
findToken(uint8_t *s, int16_t length) {
    int16_t i, token;

    for(i=0; i<(int16_t)tokenCount; ++i) {
        token=tokens[i];
        if(token!=-1 && length==words[token].length && 0==uprv_memcmp(s, words[token].s, length)) {
            return i;
        }
    }

    return -1;
}

static Word *
findWord(char *s, int16_t length) {
    uint32_t i;

    for(i=0; i<wordCount; ++i) {
        if(length==words[i].length && 0==uprv_memcmp(s, words[i].s, length)) {
            return words+i;
        }
    }

    return NULL;
}

static Word *
addWord(char *s, int16_t length) {
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
addLine(uint32_t code, char *name1, int16_t name1Length, char *name2, int16_t name2Length) {
    uint8_t *stringStart;
    Line *line;
    int16_t length;

    if(lineCount==MAX_LINE_COUNT) {
        fprintf(stderr, "gennames: too many lines\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    length=name1Length;
    if(name2Length>0) {
        length+=1+name2Length;
    }

    stringStart=allocLine(length);
    if(name1Length>0) {
        uprv_memcpy(stringStart, name1, name1Length);
    }
    if(name2Length>0) {
        stringStart[name1Length]=NAME_SEPARATOR_CHAR;
        uprv_memcpy(stringStart+name1Length+1, name2, name2Length);
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

    return stringStart-stringStore;
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
        lineLengths[lineLengthsTop/2]=nibble<<4;
    } else {
        lineLengths[lineLengthsTop/2]|=nibble&0xf;
    }
    ++lineLengthsTop;
}

static uint8_t *
allocLine(uint32_t length) {
    uint32_t top=lineTop+length;
    uint8_t *p;

    if(top>wordBottom) {
        fprintf(stderr, "gennames: out of memory\n");
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
        fprintf(stderr, "gennames: out of memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    wordBottom=bottom;
    return stringStore+bottom;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
