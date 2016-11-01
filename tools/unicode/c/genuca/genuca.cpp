/*
*******************************************************************************
*
*   Copyright (C) 2000-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genuca.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created at the end of XX century
*   created by: Vladimir Weinstein
*
*   This program reads the Franctional UCA table and generates
*   internal format for UCA table as well as inverse UCA table.
*   It then writes binary files containing the data: ucadata.dat 
*   & invuca.dat
*   Change history:
*   02/23/2001  grhoten                 Made it into a tool
*   02/23/2001  weiv                    Moved element & table handling code to i18n
*   05/09/2001  weiv                    Case bits are now in the CEs, not in front
*   10/26/2010  sgill                   Support for reordering codes
*/

#define U_NO_DEFAULT_INCLUDE_UTF_HEADERS 1

#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "unicode/udata.h"
#include "unicode/uclean.h"
#include "unicode/uscript.h"
#include "unicode/ustring.h"
#include "unicode/utf16.h"
#include "charstr.h"
#include "ucol_bld.h"
#include "ucol_imp.h"
#include "genuca.h"
#include "uoptions.h"
#include "uparse.h"
#include "toolutil.h"
#include "unewdata.h"
#include "cstring.h"
#include "cmemory.h"

#include <stdio.h>

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

/** The maximum UTF-16 length (number of UChars) in a UCA contraction. */
static const int32_t MAX_UCA_CONTRACTION_LENGTH=4;

// script reordering structures
typedef struct {
    uint16_t reorderCode;
    uint16_t offset;
} ReorderIndex;

typedef struct {
    uint16_t LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH;
    uint16_t* LEAD_BYTE_TO_SCRIPTS_INDEX;
    uint16_t LEAD_BYTE_TO_SCRIPTS_DATA_LENGTH;
    uint16_t* LEAD_BYTE_TO_SCRIPTS_DATA;
    uint16_t LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET;
    
    uint16_t SCRIPT_TO_LEAD_BYTES_INDEX_LENGTH;
    ReorderIndex* SCRIPT_TO_LEAD_BYTES_INDEX;
    uint16_t SCRIPT_TO_LEAD_BYTES_INDEX_COUNT;
    uint16_t SCRIPT_TO_LEAD_BYTES_DATA_LENGTH;
    uint16_t* SCRIPT_TO_LEAD_BYTES_DATA;
    uint16_t SCRIPT_TO_LEAD_BYTES_DATA_OFFSET;
} LeadByteConstants;

int ReorderIndexComparer(const void *a, const void *b) {
    return reinterpret_cast<const ReorderIndex*>(a)->reorderCode - reinterpret_cast<const ReorderIndex*>(b)->reorderCode;    
}

/*
 * Global - verbosity
 */
UBool beVerbose = FALSE;

static UVersionInfo UCAVersion;

#if UCONFIG_NO_COLLATION

/* dummy UDataInfo cf. udata.h */
static UDataInfo dummyDataInfo = {
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0, 0, 0, 0 },                 /* dummy dataFormat */
    { 0, 0, 0, 0 },                 /* dummy formatVersion */
    { 0, 0, 0, 0 }                  /* dummy dataVersion */
};

#else

static const UDataInfo ucaDataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {UCA_DATA_FORMAT_0, UCA_DATA_FORMAT_1, UCA_DATA_FORMAT_2, UCA_DATA_FORMAT_3},     /* dataFormat="UCol"            */
    /* 03/26/2002 bumped up version since format has changed */
    /* 09/16/2002 bumped up version since we went from UColAttributeValue */
    /*            to int32_t in UColOptionSet */
    /* 05/13/2003 This one also updated since we added UCA and UCD versions */
    /*            to header */
    /* 09/11/2003 Adding information required by data swapper */
    {UCA_FORMAT_VERSION_0, UCA_FORMAT_VERSION_1, UCA_FORMAT_VERSION_2, UCA_FORMAT_VERSION_3},                 /* formatVersion                */
    {0, 0, 0, 0}                  /* dataVersion = Unicode Version*/
};

static const UDataInfo invUcaDataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {INVUCA_DATA_FORMAT_0, INVUCA_DATA_FORMAT_1, INVUCA_DATA_FORMAT_2, INVUCA_DATA_FORMAT_3},     /* dataFormat="InvC"            */
    /* 03/26/2002 bumped up version since format has changed */
    /* 04/29/2003 2.1 format - we have added UCA version to header */
    {INVUCA_FORMAT_VERSION_0, INVUCA_FORMAT_VERSION_1, INVUCA_FORMAT_VERSION_2, INVUCA_FORMAT_VERSION_3},                 /* formatVersion                */
    {0, 0, 0, 0}                  /* dataVersion = Unicode Version*/
};

UCAElements le;

// returns number of characters read
int32_t readElement(char **from, char *to, char separator, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return 0;
    }
    char buffer[1024];
    int32_t i = 0;
    for(;;) {
        char c = **from;
        if(c == separator || (separator == ' ' && c == '\t')) {
            break;
        }
        if (c == '\0') {
            return 0;
        }
        if(c != ' ') {
            *(buffer+i++) = c;
        }
        (*from)++;
    }
    (*from)++;
    *(buffer + i) = 0;
    //*to = (char *)malloc(strlen(buffer)+1);
    strcpy(to, buffer);
    return i;
}

int32_t skipUntilWhiteSpace(char **from, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return 0;
    }
    int32_t count = 0;
    while (**from != ' ' && **from != '\t' && **from != '\0') {
        (*from)++;
        count++;
    }
    return count;
}

int32_t skipWhiteSpace(char **from, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return 0;
    }
    int32_t count = 0;
    while (**from == ' ' || **from == '\t') {
        (*from)++;
        count++;
    }
    return count;
}

uint32_t getSingleCEValue(char *primary, char *secondary, char *tertiary, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return 0;
    }
    uint32_t value = 0;
    char primsave = '\0';
    char secsave = '\0';
    char tersave = '\0';
    char *primend = primary+4;
    if(strlen(primary) > 4) {
        primsave = *primend;
        *primend = '\0';
    }
    char *secend = secondary+2;
    if(strlen(secondary) > 2) {
        secsave = *secend;
        *secend = '\0';
    }
    char *terend = tertiary+2;
    if(strlen(tertiary) > 2) {
        tersave = *terend;
        *terend = '\0';
    }
    uint32_t primvalue = (uint32_t)((*primary!='\0')?strtoul(primary, &primend, 16):0);
    uint32_t secvalue = (uint32_t)((*secondary!='\0')?strtoul(secondary, &secend, 16):0);
    uint32_t tervalue = (uint32_t)((*tertiary!='\0')?strtoul(tertiary, &terend, 16):0);
    if(primvalue <= 0xFF) {
      primvalue <<= 8;
    }

    value = ((primvalue<<UCOL_PRIMARYORDERSHIFT)&UCOL_PRIMARYORDERMASK)|
        ((secvalue<<UCOL_SECONDARYORDERSHIFT)&UCOL_SECONDARYORDERMASK)|
        (tervalue&UCOL_TERTIARYORDERMASK);

    if(primsave!='\0') {
        *primend = primsave;
    }
    if(secsave!='\0') {
        *secend = secsave;
    }
    if(tersave!='\0') {
        *terend = tersave;
    }
    return value;
}

static uint32_t inverseTable[0xFFFF][3];
static uint32_t inversePos = 0;
static UChar stringContinue[0xFFFF];
static uint32_t sContPos = 0;

static void addNewInverse(UCAElements *element, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
  if(beVerbose && isContinuation(element->CEs[1])) {
    //printf("+");
  }
  inversePos++;
  inverseTable[inversePos][0] = element->CEs[0];
  if(element->noOfCEs > 1 && isContinuation(element->CEs[1])) {
    inverseTable[inversePos][1] = element->CEs[1];
  } else {
    inverseTable[inversePos][1] = 0;
  }
  if(element->cSize < 2) {
    inverseTable[inversePos][2] = element->cPoints[0];
  } else { /* add a new store of cruft */
    inverseTable[inversePos][2] = ((element->cSize+1) << UCOL_INV_SHIFTVALUE) | sContPos;
    memcpy(stringContinue+sContPos, element->cPoints, element->cSize*sizeof(UChar));
    sContPos += element->cSize+1;
  }
}

static void insertInverse(UCAElements *element, uint32_t position, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }

  if(beVerbose && isContinuation(element->CEs[1])) {
    //printf("+");
  }
  if(position <= inversePos) {
    /*move stuff around */
    uint32_t amountToMove = (inversePos - position+1)*sizeof(inverseTable[0]);
    uprv_memmove(inverseTable[position+1], inverseTable[position], amountToMove);
  }
  inverseTable[position][0] = element->CEs[0];
  if(element->noOfCEs > 1 && isContinuation(element->CEs[1])) {
    inverseTable[position][1] = element->CEs[1];
  } else {
    inverseTable[position][1] = 0;
  }
  if(element->cSize < 2) {
    inverseTable[position][2] = element->cPoints[0];
  } else { /* add a new store of cruft */
    inverseTable[position][2] = ((element->cSize+1) << UCOL_INV_SHIFTVALUE) | sContPos;
    memcpy(stringContinue+sContPos, element->cPoints, element->cSize*sizeof(UChar));
    sContPos += element->cSize+1;
  }
  inversePos++;
}

static void addToExistingInverse(UCAElements *element, uint32_t position, UErrorCode *status) {

  if(U_FAILURE(*status)) {
    return;
  }

      if((inverseTable[position][2] & UCOL_INV_SIZEMASK) == 0) { /* single element, have to make new extension place and put both guys there */
        stringContinue[sContPos] = (UChar)inverseTable[position][2];
        inverseTable[position][2] = ((element->cSize+3) << UCOL_INV_SHIFTVALUE) | sContPos;
        sContPos++;
        stringContinue[sContPos++] = 0xFFFF;
        memcpy(stringContinue+sContPos, element->cPoints, element->cSize*sizeof(UChar));
        sContPos += element->cSize;
        stringContinue[sContPos++] = 0xFFFE;
      } else { /* adding to the already existing continuing table */
        uint32_t contIndex = inverseTable[position][2] & UCOL_INV_OFFSETMASK;
        uint32_t contSize = (inverseTable[position][2] & UCOL_INV_SIZEMASK) >> UCOL_INV_SHIFTVALUE;

        if(contIndex+contSize < sContPos) {
          /*fprintf(stderr, ".", sContPos, contIndex+contSize);*/
          memcpy(stringContinue+contIndex+contSize+element->cSize+1, stringContinue+contIndex+contSize, (element->cSize+1)*sizeof(UChar));
        }

        stringContinue[contIndex+contSize-1] = 0xFFFF;
        memcpy(stringContinue+contIndex+contSize, element->cPoints, element->cSize*sizeof(UChar));
        sContPos += element->cSize+1;
        stringContinue[contIndex+contSize+element->cSize] = 0xFFFE;

        inverseTable[position][2] = ((contSize+element->cSize+1) << UCOL_INV_SHIFTVALUE) | contIndex;
      }
}

/* 
 * Takes two CEs (lead and continuation) and 
 * compares them as CEs should be compared:
 * primary vs. primary, secondary vs. secondary
 * tertiary vs. tertiary
 */
static int32_t compareCEs(uint32_t *source, uint32_t *target) {
  uint32_t s1 = source[0], s2, t1 = target[0], t2;
  if(isContinuation(source[1])) {
    s2 = source[1];
  } else {
    s2 = 0;
  }
  if(isContinuation(target[1])) {
    t2 = target[1];
  } else {
    t2 = 0;
  }
  
  uint32_t s = 0, t = 0;
  if(s1 == t1 && s2 == t2) {
    return 0;
  }
  s = (s1 & 0xFFFF0000)|((s2 & 0xFFFF0000)>>16); 
  t = (t1 & 0xFFFF0000)|((t2 & 0xFFFF0000)>>16); 
  if(s < t) {
    return -1;
  } else if(s > t) {
    return 1;
  } else {
    s = (s1 & 0x0000FF00) | (s2 & 0x0000FF00)>>8;
    t = (t1 & 0x0000FF00) | (t2 & 0x0000FF00)>>8;
    if(s < t) {
      return -1;
    } else if(s > t) {
      return 1;
    } else {
      s = (s1 & 0x000000FF)<<8 | (s2 & 0x000000FF);
      t = (t1 & 0x000000FF)<<8 | (t2 & 0x000000FF);
      if(s < t) {
        return -1;
      } else {
        return 1;
      }
    }
  }
}

static uint32_t addToInverse(UCAElements *element, UErrorCode *status) {
  uint32_t position = inversePos;
  uint32_t saveElement = element->CEs[0];
  int32_t compResult = 0;
  element->CEs[0] &= 0xFFFFFF3F;
  if(element->noOfCEs == 1) {
    element->CEs[1] = 0;
  }
  if(inversePos == 0) {
    inverseTable[0][0] = inverseTable[0][1] = inverseTable[0][2] = 0;
    addNewInverse(element, status);
  } else if(compareCEs(inverseTable[inversePos], element->CEs) > 0) {
    while((compResult = compareCEs(inverseTable[--position], element->CEs)) > 0);
    if(beVerbose) { printf("p:%u ", (int)position); }
    if(compResult == 0) {
      addToExistingInverse(element, position, status);
    } else {
      insertInverse(element, position+1, status);
    }
  } else if(compareCEs(inverseTable[inversePos], element->CEs) == 0) {
    addToExistingInverse(element, inversePos, status);
  } else {
    addNewInverse(element, status);
  }
  element->CEs[0] = saveElement;
  if(beVerbose) { printf("+"); }
  return inversePos;
}

static InverseUCATableHeader *assembleInverseTable(UErrorCode *status)
{
  InverseUCATableHeader *result = NULL;
  uint32_t headerByteSize = paddedsize(sizeof(InverseUCATableHeader));
  uint32_t inverseTableByteSize = (inversePos+2)*sizeof(uint32_t)*3;
  uint32_t contsByteSize = sContPos * sizeof(UChar);
  uint32_t i = 0;

  result = (InverseUCATableHeader *)uprv_malloc(headerByteSize + inverseTableByteSize + contsByteSize);
  uprv_memset(result, 0, headerByteSize + inverseTableByteSize + contsByteSize);
  if(result != NULL) {
    result->byteSize = headerByteSize + inverseTableByteSize + contsByteSize;

    inversePos++;
    inverseTable[inversePos][0] = 0xFFFFFFFF;
    inverseTable[inversePos][1] = 0xFFFFFFFF;
    inverseTable[inversePos][2] = 0x0000FFFF;
    inversePos++;

    for(i = 2; i<inversePos; i++) {
      if(compareCEs(inverseTable[i-1], inverseTable[i]) > 0) { 
        fprintf(stderr, "Error at %i: %08X & %08X\n", (int)i, (int)inverseTable[i-1][0], (int)inverseTable[i][0]);
      } else if(inverseTable[i-1][0] == inverseTable[i][0] && !(inverseTable[i-1][1] < inverseTable[i][1])) {
        fprintf(stderr, "Continuation error at %i: %08X %08X & %08X %08X\n", (int)i, (int)inverseTable[i-1][0], (int)inverseTable[i-1][1], (int)inverseTable[i][0], (int)inverseTable[i][1]);
      }
    }

    result->tableSize = inversePos;
    result->contsSize = sContPos;

    result->table = headerByteSize;
    result->conts = headerByteSize + inverseTableByteSize;

    memcpy((uint8_t *)result + result->table, inverseTable, inverseTableByteSize);
    memcpy((uint8_t *)result + result->conts, stringContinue, contsByteSize);

  } else {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }

  return result; 
}


static void writeOutInverseData(InverseUCATableHeader *data, 
                  const char *outputDir, 
                  const char *copyright,
                  UErrorCode *status)
{
    UNewDataMemory *pData;
    
    long dataLength;

    UDataInfo invUcaInfo;
    uprv_memcpy(&invUcaInfo, &invUcaDataInfo, sizeof(UDataInfo));
    uprv_memcpy(invUcaInfo.dataVersion, UCAVersion, U_MAX_VERSION_LENGTH);

    pData=udata_create(outputDir, INVC_DATA_TYPE, INVC_DATA_NAME, &invUcaInfo,
                       copyright, status);

    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: unable to create %s"INVC_DATA_NAME", error %s\n", outputDir, u_errorName(*status));
        return;
    }

    /* write the data to the file */
    if (beVerbose) {
        printf("Writing out inverse UCA table: %s%c%s.%s\n", outputDir, U_FILE_SEP_CHAR,
                                                                INVC_DATA_NAME,
                                                                INVC_DATA_TYPE);
    }
    udata_writeBlock(pData, data, data->byteSize);

    /* finish up */
    dataLength=udata_finish(pData, status);
    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: error %d writing the output file\n", *status);
        return;
    }
}

static int32_t hex2num(char hex) {
    if(hex>='0' && hex <='9') {
        return hex-'0';
    } else if(hex>='a' && hex<='f') {
        return hex-'a'+10;
    } else if(hex>='A' && hex<='F') {
        return hex-'A'+10;
    } else {
        return 0;
    }
}

// static char* CHARACTER_CATEGORY_REORDER_CODES[] = {
//     "Zs", "Nd", "Sc"
// };
// static const uint16_t CHARACTER_CATEGORY_REORDER_CODE_OFFSET = 0x1000;
// static uint16_t CHARACTER_CATEGORY_REORDER_CODES_VALUE[] = {
//     U_SPACE_SEPARATOR + CHARACTER_CATEGORY_REORDER_CODE_OFFSET,
//     U_DECIMAL_DIGIT_NUMBER + CHARACTER_CATEGORY_REORDER_CODE_OFFSET, 
//     U_CURRENCY_SYMBOL + CHARACTER_CATEGORY_REORDER_CODE_OFFSET
// };

static const struct {
    const char *name;
    int32_t code;
} specialReorderTokens[] = {
    { "TERMINATOR", -2 },  // -2 means "ignore"
    { "LEVEL-SEPARATOR", -2 },
    { "FIELD-SEPARATOR", -2 },
    { "COMPRESS", -2 },  // TODO: We should parse/store which lead bytes are compressible; there is a ticket for that.
    { "PUNCTUATION", UCOL_REORDER_CODE_PUNCTUATION },
    { "IMPLICIT", USCRIPT_HAN },  // Implicit weights are usually for Han characters. Han & unassigned share a lead byte.
    { "TRAILING", -2 },  // We do not reorder trailing weights (those after implicits).
    { "SPECIAL", -2 }  // We must never reorder internal, special CE lead bytes.
};

int32_t getReorderCode(const char* name) {
    int32_t code = ucol_findReorderingEntry(name);
    if (code >= 0) {
        return code;
    }
    code = u_getPropertyValueEnum(UCHAR_SCRIPT, name);
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

UCAElements *readAnElement(FILE *data, tempUCATable *t, UCAConstants *consts, LeadByteConstants *leadByteConstants, UErrorCode *status) {
    static int itemsToDataBlock = 0;
    static int scriptDataWritten = 0;
    char buffer[2048], primary[100], secondary[100], tertiary[100];
    UChar uBuffer[2048];
    UChar uBuffer2[2048];
    UChar leadByte[100], scriptCode[100];
    int32_t i = 0;
    unsigned int theValue;
    char *pointer = NULL;
    char *commentStart = NULL;
    char *startCodePoint = NULL;
    char *endCodePoint = NULL;
    char *result = fgets(buffer, 2048, data);
    int32_t buflen = (int32_t)uprv_strlen(buffer);
    if(U_FAILURE(*status)) {
        return 0;
    }
    *primary = *secondary = *tertiary = '\0';
    *leadByte = *scriptCode = '\0';
    if(result == NULL) {
        if(feof(data)) {
            return NULL;
        } else {
            fprintf(stderr, "empty line but no EOF!\n");
            *status = U_INVALID_FORMAT_ERROR;
            return NULL;
        }
    }
    while(buflen>0 && (buffer[buflen-1] == '\r' || buffer[buflen-1] == '\n')) {
      buffer[--buflen] = 0;
    }

    if(buffer[0] == 0 || buffer[0] == '#') {
        return NULL; // just a comment, skip whole line
    }

    UCAElements *element = &le;
    memset(element, 0, sizeof(*element));

    enum ActionType {
      READCE,
      READHEX1,
      READHEX2,
      READUCAVERSION,
      READLEADBYTETOSCRIPTS,
      READSCRIPTTOLEADBYTES,
      IGNORE,
    };

    // Directives.
    if(buffer[0] == '[') {
      uint32_t cnt = 0;
      static const struct {
        char name[128];
        uint32_t *what;
        ActionType what_to_do;
      } vt[]  = { {"[first tertiary ignorable",  consts->UCA_FIRST_TERTIARY_IGNORABLE,  READCE},
                  {"[last tertiary ignorable",   consts->UCA_LAST_TERTIARY_IGNORABLE,   READCE},
                  {"[first secondary ignorable", consts->UCA_FIRST_SECONDARY_IGNORABLE, READCE},
                  {"[last secondary ignorable",  consts->UCA_LAST_SECONDARY_IGNORABLE,  READCE},
                  {"[first primary ignorable",   consts->UCA_FIRST_PRIMARY_IGNORABLE,   READCE},
                  {"[last primary ignorable",    consts->UCA_LAST_PRIMARY_IGNORABLE,    READCE},
                  {"[first variable",            consts->UCA_FIRST_VARIABLE,            READCE},
                  {"[last variable",             consts->UCA_LAST_VARIABLE,             READCE},
                  {"[first regular",             consts->UCA_FIRST_NON_VARIABLE,        READCE},
                  {"[last regular",              consts->UCA_LAST_NON_VARIABLE,         READCE},
                  {"[first implicit",            consts->UCA_FIRST_IMPLICIT,            READCE},
                  {"[last implicit",             consts->UCA_LAST_IMPLICIT,             READCE},
                  {"[first trailing",            consts->UCA_FIRST_TRAILING,            READCE},
                  {"[last trailing",             consts->UCA_LAST_TRAILING,             READCE},

                  {"[fixed top",                    &consts->UCA_PRIMARY_TOP_MIN,       READHEX1},
                  {"[fixed first implicit byte",    &consts->UCA_PRIMARY_IMPLICIT_MIN,  READHEX1},
                  {"[fixed last implicit byte",     &consts->UCA_PRIMARY_IMPLICIT_MAX,  READHEX1},
                  {"[fixed first trail byte",       &consts->UCA_PRIMARY_TRAILING_MIN,  READHEX1},
                  {"[fixed last trail byte",        &consts->UCA_PRIMARY_TRAILING_MAX,  READHEX1},
                  {"[fixed first special byte",     &consts->UCA_PRIMARY_SPECIAL_MIN,   READHEX1},
                  {"[fixed last special byte",      &consts->UCA_PRIMARY_SPECIAL_MAX,   READHEX1},
                  {"[variable top = ",              &t->options->variableTopValue,      READHEX2},
                  {"[UCA version = ",               NULL,                               READUCAVERSION},
                  {"[top_byte",                     NULL,                               READLEADBYTETOSCRIPTS},
                  {"[reorderingTokens",             NULL,                               READSCRIPTTOLEADBYTES},
                  {"[categories",                   NULL,                               IGNORE},
                  {"[first tertiary in secondary non-ignorable",                 NULL,                               IGNORE},
                  {"[last tertiary in secondary non-ignorable",                 NULL,                               IGNORE},
                  {"[first secondary in primary non-ignorable",                 NULL,                               IGNORE},
                  {"[last secondary in primary non-ignorable",                 NULL,                               IGNORE},
      };
      for (cnt = 0; cnt<sizeof(vt)/sizeof(vt[0]); cnt++) {
        uint32_t vtLen = (uint32_t)uprv_strlen(vt[cnt].name);
        if(uprv_strncmp(buffer, vt[cnt].name, vtLen) == 0) {
            ActionType what_to_do = vt[cnt].what_to_do;
            if (what_to_do == IGNORE) { //vt[cnt].what_to_do == IGNORE
                return NULL;
            } else if(what_to_do == READHEX1 || what_to_do == READHEX2) {
              pointer = buffer+vtLen;
              int32_t numBytes = readElement(&pointer, primary, ']', status) / 2;
              if(numBytes != (what_to_do == READHEX1 ? 1 : 2)) {
                  fprintf(stderr, "Value of \"%s\" has unexpected number of %d bytes\n",
                          buffer, (int)numBytes);
                  //*status = U_INVALID_FORMAT_ERROR;
                  return NULL;
              }
              *(vt[cnt].what) = (uint32_t)uprv_strtoul(primary, &pointer, 16);
              if(*pointer != 0) {
                  fprintf(stderr, "Value of \"%s\" is not a hexadecimal number\n", buffer);
                  //*status = U_INVALID_FORMAT_ERROR;
                  return NULL;
              }
            } else if (what_to_do == READCE) {
              // TODO: combine & clean up the two CE parsers
              pointer = strchr(buffer+vtLen, '[');
              if(pointer) {
                pointer++;
                element->sizePrim[0]=readElement(&pointer, primary, ',', status) / 2;
                element->sizeSec[0]=readElement(&pointer, secondary, ',', status) / 2;
                element->sizeTer[0]=readElement(&pointer, tertiary, ']', status) / 2;
                vt[cnt].what[0] = getSingleCEValue(primary, secondary, tertiary, status);
                if(element->sizePrim[0] > 2 || element->sizeSec[0] > 1 || element->sizeTer[0] > 1) {
                  uint32_t CEi = 1;
                  uint32_t value = UCOL_CONTINUATION_MARKER; /* Continuation marker */
                    if(2*CEi<element->sizePrim[i]) {
                        value |= ((hex2num(*(primary+4*CEi))&0xF)<<28);
                        value |= ((hex2num(*(primary+4*CEi+1))&0xF)<<24);
                    }

                    if(2*CEi+1<element->sizePrim[i]) {
                        value |= ((hex2num(*(primary+4*CEi+2))&0xF)<<20);
                        value |= ((hex2num(*(primary+4*CEi+3))&0xF)<<16);
                    }

                    if(CEi<element->sizeSec[i]) {
                        value |= ((hex2num(*(secondary+2*CEi))&0xF)<<12);
                        value |= ((hex2num(*(secondary+2*CEi+1))&0xF)<<8);
                    }

                    if(CEi<element->sizeTer[i]) {
                        value |= ((hex2num(*(tertiary+2*CEi))&0x3)<<4);
                        value |= (hex2num(*(tertiary+2*CEi+1))&0xF);
                    }

                    CEi++;

                    vt[cnt].what[1] = value;
                    //element->CEs[CEindex++] = value;
                } else {
                  vt[cnt].what[1] = 0;
                }
              } else {
                fprintf(stderr, "Failed to read a CE from line %s\n", buffer);
              }
            } else if (what_to_do == READUCAVERSION) { //vt[cnt].what_to_do == READUCAVERSION
              u_versionFromString(UCAVersion, buffer+vtLen);
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
                fprintf(stderr, "warning: UCA version %s != UCD version %s (temporarily change the FractionalUCA.txt UCA version during Unicode version upgrade)\n", uca, ucd);
                // *status = U_INVALID_FORMAT_ERROR;
                // return NULL;
              }
            } else if (what_to_do == READLEADBYTETOSCRIPTS) { //vt[cnt].what_to_do == READLEADBYTETOSCRIPTS
                pointer = buffer + vtLen;
                skipWhiteSpace(&pointer, status);

                uint16_t leadByte = (hex2num(*pointer++) * 16) + hex2num(*pointer++);
                //printf("~~~~ processing lead byte = %02x\n", leadByte);
                if (leadByte >= leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH) {
                    fprintf(stderr, "Lead byte larger than allocated table!");
                    // set status and return
                    *status = U_INTERNAL_PROGRAM_ERROR;
                    return NULL;
                }
                skipWhiteSpace(&pointer, status);

                int32_t reorderCodeArray[100];
                uint32_t reorderCodeArrayCount = 0;
                char scriptName[100];
                int32_t elementLength = 0;
                while ((elementLength = readElement(&pointer, scriptName, ' ', status)) > 0) {
                    if (scriptName[0] == ']') {
                        break;
                    }
                    int32_t reorderCode = getReorderCode(scriptName);
                    if (reorderCode == -2) {
                        continue;  // Ignore "TERMINATOR" etc.
                    }
                    if (reorderCode < 0) {
                        printf("Syntax error: unable to parse reorder code from '%s'\n", scriptName);
                        *status = U_INVALID_FORMAT_ERROR;
                        return NULL;
                    }
                    if (reorderCodeArrayCount >= LENGTHOF(reorderCodeArray)) {
                        printf("reorder code array count is greater than allocated size!\n");
                        *status = U_INTERNAL_PROGRAM_ERROR;
                        return NULL;
                    }
                    reorderCodeArray[reorderCodeArrayCount++] = reorderCode;
                }
                //printf("reorderCodeArrayCount = %d\n", reorderCodeArrayCount);
                switch (reorderCodeArrayCount) {
                    case 0:
                        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX[leadByte] = 0;
                        break;
                    case 1:
                        // TODO = move 0x8000 into defined constant
                        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX[leadByte] = 0x8000 | reorderCodeArray[0];
                        break;
                    default:
                        if (reorderCodeArrayCount + leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET > leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_LENGTH) {
                            // Error condition
                        }
                        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX[leadByte] = leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET;
                        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA[leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET++] = reorderCodeArrayCount;
                        for (int reorderCodeIndex = 0; reorderCodeIndex < reorderCodeArrayCount; reorderCodeIndex++) {
                            leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA[leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET++] = reorderCodeArray[reorderCodeIndex];
                        }
                }
            } else if (what_to_do == READSCRIPTTOLEADBYTES) { //vt[cnt].what_to_do == READSCRIPTTOLEADBYTES
                uint16_t leadByteArray[256];
                uint32_t leadByteArrayCount = 0;
                char scriptName[100];

                pointer = buffer + vtLen;
                skipWhiteSpace(&pointer, status);
                uint32_t scriptNameLength = readElement(&pointer, scriptName, '\t', status);
                int32_t reorderCode = getReorderCode(scriptName);
                if (reorderCode >= 0) {
                    //printf("^^^ processing reorder code = %04x (%s)\n", reorderCode, scriptName);
                    skipWhiteSpace(&pointer, status);

                    int32_t elementLength = 0;
                    char leadByteString[100];
                    while ((elementLength = readElement(&pointer, leadByteString, '=', status)) == 2) {
                        //printf("\tleadByteArrayCount = %d, elementLength = %d, leadByteString = %s\n", leadByteArrayCount, elementLength, leadByteString);
                        uint32_t leadByte = (hex2num(leadByteString[0]) * 16) + hex2num(leadByteString[1]);
                        leadByteArray[leadByteArrayCount++] = (uint16_t) leadByte;
                        skipUntilWhiteSpace(&pointer, status);
                    }

                    if (leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT >= leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_LENGTH) {
                        //printf("\tError condition\n");
                        //printf("\tindex count = %d, total index size = %d\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT, sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX) / sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[0]));
                        // Error condition
                        *status = U_INTERNAL_PROGRAM_ERROR;
                        return NULL;
                    }
                    leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT].reorderCode = reorderCode;

                    //printf("\tlead byte count = %d\n", leadByteArrayCount);
                    //printf("\tlead byte array = ");
                    //for (int i = 0; i < leadByteArrayCount; i++) {
                    //    printf("%02x, ", leadByteArray[i]);
                    //}
                    //printf("\n");

                    switch (leadByteArrayCount) {
                        case 0:
                            leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT].offset = 0;
                            break;
                        case 1:
                            // TODO = move 0x8000 into defined constant
                            //printf("\t+++++ lead byte = &x\n", leadByteArray[0]);
                            leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT].offset = 0x8000 | leadByteArray[0];
                            break;
                        default:
                            //printf("\t+++++ lead bytes written to data block - %d\n", itemsToDataBlock++);
                            //printf("\tlead bytes = ");
                            //for (int i = 0; i < leadByteArrayCount; i++) {
                            //    printf("%02x, ", leadByteArray[i]);
                            //}
                            //printf("\n");
                            //printf("\tBEFORE data bytes = ");
                            //for (int i = 0; i < leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET; i++) {
                            //    printf("%02x, ", leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA[i]);
                            //}
                            //printf("\n");
                            //printf("\tdata offset = %d, data length = %d\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET, leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_LENGTH);
                            if ((leadByteArrayCount + leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET) > leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_LENGTH) {
                                //printf("\tError condition\n");
                                // Error condition
                                *status = U_INTERNAL_PROGRAM_ERROR;
                                return NULL;
                            }
                            leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT].offset = leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET;
                            leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA[leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET++] = leadByteArrayCount;
                            scriptDataWritten++;
                            memcpy(&leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA[leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET],
                                leadByteArray, leadByteArrayCount * sizeof(leadByteArray[0]));
                            scriptDataWritten += leadByteArrayCount;
                            //printf("\tlead byte data written = %d\n", scriptDataWritten);
                            //printf("\tcurrentIndex.reorderCode = %04x, currentIndex.offset = %04x\n", 
                            //    leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT.reorderCode, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT.offset);
                            leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET += leadByteArrayCount;
                            //printf("\tdata offset = %d\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET);
                            //printf("\tAFTER data bytes = ");
                            //for (int i = 0; i < leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET; i++) {
                            //    printf("%02x, ", leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA[i]);
                            //}
                            //printf("\n");
                    }
                    //if (reorderCode >= 0x1000) {
                     //   printf("@@@@ reorderCode = %x, offset = %x\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT].reorderCode, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT].offset);
                     //   for (int i = 0; i < leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET; i++) {
                    //        printf("%02x, ", leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA[i]);
                     //   }
                    //    printf("\n");
                   // }
                    leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT++;
                }
            }
            return NULL;
        }
      }
      fprintf(stderr, "Warning: unrecognized option: %s\n", buffer);
      //*status = U_INVALID_FORMAT_ERROR;
      return NULL;
    }

    startCodePoint = buffer;
    endCodePoint = strchr(startCodePoint, ';');

    if(endCodePoint == 0) {
        fprintf(stderr, "error - line with no code point!\n");
        *status = U_INVALID_FORMAT_ERROR; /* No code point - could be an error, but probably only an empty line */
        return NULL;
    } else {
        *(endCodePoint) = 0;
    }

    char *pipePointer = strchr(buffer, '|');
    if (pipePointer != NULL) {
        // Read the prefix string which precedes the actual string.
        *pipePointer = 0;
        element->prefixSize =
            u_parseString(startCodePoint,
                          element->prefixChars, LENGTHOF(element->prefixChars),
                          NULL, status);
        if(U_FAILURE(*status)) {
            fprintf(stderr, "error - parsing of prefix \"%s\" failed: %s\n",
                    startCodePoint, u_errorName(*status));
            *status = U_INVALID_FORMAT_ERROR;
            return NULL;
        }
        element->prefix = element->prefixChars;
        startCodePoint = pipePointer + 1;
    }

    // Read the string which gets the CE(s) assigned.
    element->cSize =
        u_parseString(startCodePoint,
                      element->uchars, LENGTHOF(element->uchars),
                      NULL, status);
    if(U_FAILURE(*status)) {
        fprintf(stderr, "error - parsing of code point(s) \"%s\" failed: %s\n",
                startCodePoint, u_errorName(*status));
        *status = U_INVALID_FORMAT_ERROR;
        return NULL;
    }
    element->cPoints = element->uchars;

    startCodePoint = endCodePoint+1;

    commentStart = strchr(startCodePoint, '#');
    if(commentStart == NULL) {
        commentStart = strlen(startCodePoint) + startCodePoint;
    }

    i = 0;
    uint32_t CEindex = 0;
    element->noOfCEs = 0;
    for(;;) {
        endCodePoint = strchr(startCodePoint, ']');
        if(endCodePoint == NULL || endCodePoint >= commentStart) {
            break;
        }
        pointer = strchr(startCodePoint, '[');
        pointer++;

        element->sizePrim[i]=readElement(&pointer, primary, ',', status) / 2;
        element->sizeSec[i]=readElement(&pointer, secondary, ',', status) / 2;
        element->sizeTer[i]=readElement(&pointer, tertiary, ']', status) / 2;


        /* I want to get the CEs entered right here, including continuation */
        element->CEs[CEindex++] = getSingleCEValue(primary, secondary, tertiary, status);

        uint32_t CEi = 1;
        while(2*CEi<element->sizePrim[i] || CEi<element->sizeSec[i] || CEi<element->sizeTer[i]) {
          uint32_t value = UCOL_CONTINUATION_MARKER; /* Continuation marker */
            if(2*CEi<element->sizePrim[i]) {
                value |= ((hex2num(*(primary+4*CEi))&0xF)<<28);
                value |= ((hex2num(*(primary+4*CEi+1))&0xF)<<24);
            }

            if(2*CEi+1<element->sizePrim[i]) {
                value |= ((hex2num(*(primary+4*CEi+2))&0xF)<<20);
                value |= ((hex2num(*(primary+4*CEi+3))&0xF)<<16);
            }

            if(CEi<element->sizeSec[i]) {
                value |= ((hex2num(*(secondary+2*CEi))&0xF)<<12);
                value |= ((hex2num(*(secondary+2*CEi+1))&0xF)<<8);
            }

            if(CEi<element->sizeTer[i]) {
                value |= ((hex2num(*(tertiary+2*CEi))&0x3)<<4);
                value |= (hex2num(*(tertiary+2*CEi+1))&0xF);
            }

            CEi++;

            element->CEs[CEindex++] = value;
        }

      startCodePoint = endCodePoint+1;
      i++;
    }
    element->noOfCEs = CEindex;
#if 0
    element->isThai = UCOL_ISTHAIPREVOWEL(element->cPoints[0]);
#endif
    // we don't want any strange stuff after useful data!
    if (pointer == NULL) {
        /* huh? Did we get ']' without the '['? Pair your brackets! */
        *status=U_INVALID_FORMAT_ERROR;
    }
    else {
        while(pointer < commentStart)  {
            if(*pointer != ' ' && *pointer != '\t')
            {
                *status=U_INVALID_FORMAT_ERROR;
                break;
            }
            pointer++;
        }
    }
    if(element->cSize == 1 && element->cPoints[0] == 0xfffe) {
        // UCA 6.0 gives U+FFFE a special minimum weight using the
        // byte 02 which is the merge-sort-key separator and illegal for any
        // other characters.
    } else {
        // Rudimentary check for valid bytes in CE weights.
        // For a more comprehensive check see cintltst /tscoll/citertst/TestCEValidity
        for (i = 0; i < (int32_t)CEindex; ++i) {
            uint32_t value = element->CEs[i];
            uint8_t bytes[4] = {
                (uint8_t)(value >> 24),
                (uint8_t)(value >> 16),
                (uint8_t)(value >> 8),
                (uint8_t)(value & UCOL_NEW_TERTIARYORDERMASK)
            };
            for (int j = 0; j < 4; ++j) {
                if (0 != bytes[j] && bytes[j] < 3) {
                    fprintf(stderr, "Warning: invalid UCA weight byte %02X for %s\n", bytes[j], buffer);
                    return NULL;
                }
            }
            // Primary second bytes 03 and FF are compression terminators.
            if (!isContinuation(value) && (bytes[1] == 3 || bytes[1] == 0xFF)) {
                fprintf(stderr, "Warning: invalid UCA primary second weight byte %02X for %s\n",
                        bytes[1], buffer);
                return NULL;
            }
        }
    }

    if(U_FAILURE(*status)) {
        fprintf(stderr, "problem putting stuff in hash table %s\n", u_errorName(*status));
        *status = U_INTERNAL_PROGRAM_ERROR;
        return NULL;
    }

    return element;
}


void writeOutData(UCATableHeader *data,
                  UCAConstants *consts,
                  LeadByteConstants *leadByteConstants,
                  UChar contractions[][MAX_UCA_CONTRACTION_LENGTH],
                  uint32_t noOfcontractions,
                  const char *outputDir,
                  const char *copyright,
                  UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return;
    }

    uint32_t size = data->size;

    data->UCAConsts = data->size;
    data->size += paddedsize(sizeof(UCAConstants));

    if(noOfcontractions != 0) {
      uprv_memset(&contractions[noOfcontractions][0], 0, MAX_UCA_CONTRACTION_LENGTH*U_SIZEOF_UCHAR);
      noOfcontractions++;


      data->contractionUCACombos = data->size;
      data->contractionUCACombosWidth = (uint8_t)MAX_UCA_CONTRACTION_LENGTH;
      data->contractionUCACombosSize = noOfcontractions;
      data->size += paddedsize((noOfcontractions*MAX_UCA_CONTRACTION_LENGTH*U_SIZEOF_UCHAR));
    }
    data->scriptToLeadByte = data->size;
    //printf("@@@@ script to lead byte offset = 0x%x (%d)\n", data->size, data->size);
    data->size +=
        sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT) +                                                       // index table header
        leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT * sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[0]) +    // index table
        sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET) +                                                       // data table header
        leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET * sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA[0]);        // data table
    data->leadByteToScript = data->size;
    //printf("@@@@ lead byte to script offset = 0x%x (%d)\n", data->size, data->size);
    data->size +=
        sizeof(leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH) +                                                      // index table header
        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH * sizeof(leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX[0]) +   // index table
        sizeof(leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET) +                                                       // data table header
        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET * sizeof(leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA[0]);        // data table

    UNewDataMemory *pData;
    
    long dataLength;
    UDataInfo ucaInfo;
    uprv_memcpy(&ucaInfo, &ucaDataInfo, sizeof(UDataInfo));
    uprv_memcpy(ucaInfo.dataVersion, UCAVersion, U_MAX_VERSION_LENGTH);

    pData=udata_create(outputDir, UCA_DATA_TYPE, UCA_DATA_NAME, &ucaInfo,
                       copyright, status);

    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: unable to create %s"UCA_DATA_NAME", error %s\n", outputDir, u_errorName(*status));
        return;
    }

    /* write the data to the file */
    if (beVerbose) {
        printf("Writing out UCA table: %s%c%s.%s\n", outputDir,
                                                        U_FILE_SEP_CHAR,
                                                        U_ICUDATA_NAME "_" UCA_DATA_NAME,
                                                        UCA_DATA_TYPE);
    }
    udata_writeBlock(pData, data, size);

    // output the constants here
    udata_writeBlock(pData, consts, sizeof(UCAConstants));

    if (beVerbose) {
        printf("first tertiary ignorable = %x %x\n", consts->UCA_FIRST_TERTIARY_IGNORABLE[0], consts->UCA_FIRST_TERTIARY_IGNORABLE[1]);
        printf("last tertiary ignorable = %x %x\n", consts->UCA_LAST_TERTIARY_IGNORABLE[0], consts->UCA_LAST_TERTIARY_IGNORABLE[1]);
        printf("first secondary ignorable = %x %x\n", consts->UCA_FIRST_SECONDARY_IGNORABLE[0], consts->UCA_FIRST_SECONDARY_IGNORABLE[1]);
        printf("contractionUCACombosSize = %d\n", data->contractionUCACombosSize);
        printf("contractionSize = %d\n", data->contractionSize);
        printf("number of UCA contractions = %d\n", noOfcontractions);
    }
    
    if(noOfcontractions != 0) {
      udata_writeBlock(pData, contractions, noOfcontractions*MAX_UCA_CONTRACTION_LENGTH*U_SIZEOF_UCHAR);
      udata_writePadding(pData, paddedsize((noOfcontractions*MAX_UCA_CONTRACTION_LENGTH*U_SIZEOF_UCHAR)) - noOfcontractions*MAX_UCA_CONTRACTION_LENGTH*U_SIZEOF_UCHAR);
    }

    // output the script to lead bytes table here
    if (beVerbose) {
        printf("Writing Script to Lead Byte Data\n");
        printf("\tindex table size = %x\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT);
        printf("\tdata block size = %x\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET);
    }
    udata_write16(pData, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT);
    udata_write16(pData, leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET);
//     printf("#### Script to Lead Byte Index Before Sort\n");
//     for (int reorderCodeIndex = 0; reorderCodeIndex < leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT; reorderCodeIndex++) {
//         printf("\t%04x = %04x\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[reorderCodeIndex].reorderCode, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[reorderCodeIndex].offset);
//     }
    qsort(leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT, sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[0]), ReorderIndexComparer);
    udata_writeBlock(pData, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT * sizeof(leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[0]));
//     printf("#### Script to Lead Byte Index After Sort\n");
//     for (int reorderCodeIndex = 0; reorderCodeIndex < leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX_COUNT; reorderCodeIndex++) {
//         printf("\t%04x = %04x\n", leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[reorderCodeIndex].reorderCode, leadByteConstants->SCRIPT_TO_LEAD_BYTES_INDEX[reorderCodeIndex].offset);
//     }
    
    // write out the script to lead bytes data block
    udata_writeBlock(pData, leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA, leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA_OFFSET * sizeof(*leadByteConstants->SCRIPT_TO_LEAD_BYTES_DATA));
    
    if (beVerbose) {
        printf("Writing Lead Byte To Script Data\n");
        printf("\tindex table size = %x\n", leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH);
        printf("\tdata block size = %x\n", leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET);
    }
    // output the header info
    udata_write16(pData, leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH);
    udata_write16(pData, leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET);
    
    // output the index table
    udata_writeBlock(pData, leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX, 
        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH * sizeof(leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX)[0]);
//     for (int leadByte = 0; leadByte < leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH; leadByte++) {
//         printf("\t%02x = %04x\n", leadByte, leadByteConstants->LEAD_BYTE_TO_SCRIPTS_INDEX[leadByte]);
//     }

    // output the data
    udata_writeBlock(pData, leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA, 
        leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET * sizeof(*leadByteConstants->LEAD_BYTE_TO_SCRIPTS_DATA));

    
    /* finish up */
    dataLength=udata_finish(pData, status);
    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: error %d writing the output file\n", *status);
        return;
    }
}

enum {
    /*
     * Maximum number of UCA contractions we can store.
     * May need to be increased for a new Unicode version.
     */
    MAX_UCA_CONTRACTIONS=2048
};

static int32_t
write_uca_table(const char *filename,
                const char *outputDir,
                const char *copyright,
                UErrorCode *status)
{
    FILE *data = fopen(filename, "r");
    if(data == NULL) {
        fprintf(stderr, "Couldn't open file: %s\n", filename);
        return -1;
    }
    uint32_t line = 0;
    UCAElements *element = NULL;
    UCATableHeader *myD = (UCATableHeader *)uprv_malloc(sizeof(UCATableHeader));
    /* test for NULL */
    if(myD == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        fclose(data);
        return 0;
    }
    uprv_memset(myD, 0, sizeof(UCATableHeader));
    UColOptionSet *opts = (UColOptionSet *)uprv_malloc(sizeof(UColOptionSet));
    /* test for NULL */
    if(opts == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        uprv_free(myD);
        fclose(data);
        return 0;
    }
    uprv_memset(opts, 0, sizeof(UColOptionSet));
    UChar contractions[MAX_UCA_CONTRACTIONS][MAX_UCA_CONTRACTION_LENGTH];
    uprv_memset(contractions, 0, sizeof(contractions));
    uint32_t noOfContractions = 0;
    UCAConstants consts;
    uprv_memset(&consts, 0, sizeof(consts));
#if 0
    UCAConstants consts = {
      UCOL_RESET_TOP_VALUE,
      UCOL_FIRST_PRIMARY_IGNORABLE,
      UCOL_LAST_PRIMARY_IGNORABLE,
      UCOL_LAST_PRIMARY_IGNORABLE_CONT,
      UCOL_FIRST_SECONDARY_IGNORABLE,
      UCOL_LAST_SECONDARY_IGNORABLE,
      UCOL_FIRST_TERTIARY_IGNORABLE,
      UCOL_LAST_TERTIARY_IGNORABLE,
      UCOL_FIRST_VARIABLE,
      UCOL_LAST_VARIABLE,
      UCOL_FIRST_NON_VARIABLE,
      UCOL_LAST_NON_VARIABLE,

      UCOL_NEXT_TOP_VALUE,
/*
      UCOL_NEXT_FIRST_PRIMARY_IGNORABLE,
      UCOL_NEXT_LAST_PRIMARY_IGNORABLE,
      UCOL_NEXT_FIRST_SECONDARY_IGNORABLE,
      UCOL_NEXT_LAST_SECONDARY_IGNORABLE,
      UCOL_NEXT_FIRST_TERTIARY_IGNORABLE,
      UCOL_NEXT_LAST_TERTIARY_IGNORABLE,
      UCOL_NEXT_FIRST_VARIABLE,
      UCOL_NEXT_LAST_VARIABLE,
*/

      PRIMARY_IMPLICIT_MIN,
      PRIMARY_IMPLICIT_MAX
    };
#endif

    //printf("Allocating LeadByteConstants\n");
    LeadByteConstants leadByteConstants;
    uprv_memset(&leadByteConstants, 0x00, sizeof(LeadByteConstants));
    
    leadByteConstants.SCRIPT_TO_LEAD_BYTES_INDEX_LENGTH = 256;
    leadByteConstants.SCRIPT_TO_LEAD_BYTES_INDEX = (ReorderIndex*) uprv_malloc(leadByteConstants.SCRIPT_TO_LEAD_BYTES_INDEX_LENGTH * sizeof(ReorderIndex));
    uprv_memset(leadByteConstants.SCRIPT_TO_LEAD_BYTES_INDEX, 0x00, leadByteConstants.SCRIPT_TO_LEAD_BYTES_INDEX_LENGTH * sizeof(ReorderIndex));
    leadByteConstants.SCRIPT_TO_LEAD_BYTES_DATA_LENGTH = 1024;
    leadByteConstants.SCRIPT_TO_LEAD_BYTES_DATA = (uint16_t*) uprv_malloc(leadByteConstants.SCRIPT_TO_LEAD_BYTES_DATA_LENGTH * sizeof(uint16_t));
    uprv_memset(leadByteConstants.SCRIPT_TO_LEAD_BYTES_DATA, 0x00, leadByteConstants.SCRIPT_TO_LEAD_BYTES_DATA_LENGTH * sizeof(uint16_t));
    //printf("\tFinished Allocating LeadByteConstants\n");
    
    leadByteConstants.LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH = 256;
    leadByteConstants.LEAD_BYTE_TO_SCRIPTS_INDEX = (uint16_t*) uprv_malloc(leadByteConstants.LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH * sizeof(uint16_t));
    uprv_memset(leadByteConstants.LEAD_BYTE_TO_SCRIPTS_INDEX, 0x8000 | USCRIPT_INVALID_CODE, leadByteConstants.LEAD_BYTE_TO_SCRIPTS_INDEX_LENGTH * sizeof(uint16_t));
    leadByteConstants.LEAD_BYTE_TO_SCRIPTS_DATA_LENGTH = 1024;
    leadByteConstants.LEAD_BYTE_TO_SCRIPTS_DATA_OFFSET = 1;     // offset by 1 to leave zero location for those lead bytes with no reorder codes
    leadByteConstants.LEAD_BYTE_TO_SCRIPTS_DATA = (uint16_t*) uprv_malloc(leadByteConstants.LEAD_BYTE_TO_SCRIPTS_DATA_LENGTH * sizeof(uint16_t));
    uprv_memset(leadByteConstants.LEAD_BYTE_TO_SCRIPTS_DATA, 0x00, leadByteConstants.LEAD_BYTE_TO_SCRIPTS_DATA_LENGTH * sizeof(uint16_t));

    uprv_memset(inverseTable, 0xDA, sizeof(int32_t)*3*0xFFFF);

    opts->variableTopValue = 0;
    opts->strength = UCOL_TERTIARY;
    opts->frenchCollation = UCOL_OFF;
    opts->alternateHandling = UCOL_NON_IGNORABLE; /* attribute for handling variable elements*/
    opts->caseFirst = UCOL_OFF;         /* who goes first, lower case or uppercase */
    opts->caseLevel = UCOL_OFF;         /* do we have an extra case level */
    opts->normalizationMode = UCOL_OFF; /* attribute for normalization */
    opts->hiraganaQ = UCOL_OFF; /* attribute for JIS X 4061, used only in Japanese */
    opts->numericCollation = UCOL_OFF;
    myD->jamoSpecial = FALSE;

    tempUCATable *t = uprv_uca_initTempTable(myD, opts, NULL, IMPLICIT_TAG, LEAD_SURROGATE_TAG, status);
    if(U_FAILURE(*status))
    {
        fprintf(stderr, "Failed to init UCA temp table: %s\n", u_errorName(*status));
        uprv_free(opts);
        uprv_free(myD);
        fclose(data);
        return -1;
    }

    // * set to zero
    struct {
        UChar32 start;
        UChar32 end;
        int32_t value;
    } ranges[] =
    {
        {0xAC00, 0xD7B0, UCOL_SPECIAL_FLAG | (HANGUL_SYLLABLE_TAG << 24) },  //0 HANGUL_SYLLABLE_TAG,/* AC00-D7AF*/
        //{0xD800, 0xDC00, UCOL_SPECIAL_FLAG | (LEAD_SURROGATE_TAG << 24)  },  //1 LEAD_SURROGATE_TAG, already set in utrie_open() /* D800-DBFF*/
        {0xDC00, 0xE000, UCOL_SPECIAL_FLAG | (TRAIL_SURROGATE_TAG << 24) },  //2 TRAIL_SURROGATE DC00-DFFF
        // Now directly handled in the collation code by the swapCJK function.
        //{0x3400, 0x4DB6, UCOL_SPECIAL_FLAG | (CJK_IMPLICIT_TAG << 24)    },  //3 CJK_IMPLICIT_TAG,   /* 0x3400-0x4DB5*/
        //{0x4E00, 0x9FA6, UCOL_SPECIAL_FLAG | (CJK_IMPLICIT_TAG << 24)    },  //4 CJK_IMPLICIT_TAG,   /* 0x4E00-0x9FA5*/
        //{0xF900, 0xFA2E, UCOL_SPECIAL_FLAG | (CJK_IMPLICIT_TAG << 24)    },  //5 CJK_IMPLICIT_TAG,   /* 0xF900-0xFA2D*/
        //{0x20000, 0x2A6D7, UCOL_SPECIAL_FLAG | (CJK_IMPLICIT_TAG << 24)  },  //6 CJK_IMPLICIT_TAG,   /* 0x20000-0x2A6D6*/
        //{0x2F800, 0x2FA1E, UCOL_SPECIAL_FLAG | (CJK_IMPLICIT_TAG << 24)  },  //7 CJK_IMPLICIT_TAG,   /* 0x2F800-0x2FA1D*/
    };
    uint32_t i = 0;

    for(i = 0; i<sizeof(ranges)/sizeof(ranges[0]); i++) {
      /*ucmpe32_setRange32(t->mapping, ranges[i].start, ranges[i].end, ranges[i].value); */
      utrie_setRange32(t->mapping, ranges[i].start, ranges[i].end, ranges[i].value, TRUE);
    }


    int32_t surrogateCount = 0;
    while(!feof(data)) {
        if(U_FAILURE(*status)) {
            fprintf(stderr, "Something returned an error %i (%s) while processing line %u of %s. Exiting...\n",
                *status, u_errorName(*status), (int)line, filename);
            exit(*status);
        }

        line++;
        if(beVerbose) {
          printf("%u ", (int)line);
        }
        element = readAnElement(data, t, &consts, &leadByteConstants, status);
        if(element != NULL) {
            // we have read the line, now do something sensible with the read data!

            // if element is a contraction, we want to add it to contractions[]
            int32_t length = (int32_t)element->cSize;
            if(length > 1 && element->cPoints[0] != 0xFDD0) { // this is a contraction
              if(U16_IS_LEAD(element->cPoints[0]) && U16_IS_TRAIL(element->cPoints[1]) && length == 2) {
                surrogateCount++;
              } else {
                if(noOfContractions>=MAX_UCA_CONTRACTIONS) {
                  fprintf(stderr,
                          "\nMore than %d contractions. Please increase MAX_UCA_CONTRACTIONS in genuca.cpp. "
                          "Exiting...\n",
                          (int)MAX_UCA_CONTRACTIONS);
                  exit(U_BUFFER_OVERFLOW_ERROR);
                }
                if(length > MAX_UCA_CONTRACTION_LENGTH) {
                  fprintf(stderr,
                          "\nLine %d: Contraction of length %d is too long. Please increase MAX_UCA_CONTRACTION_LENGTH in genuca.cpp. "
                          "Exiting...\n",
                          (int)line, (int)length);
                  exit(U_BUFFER_OVERFLOW_ERROR);
                }
                UChar *t = &contractions[noOfContractions][0];
                u_memcpy(t, element->cPoints, length);
                t += length;
                for(; length < MAX_UCA_CONTRACTION_LENGTH; ++length) {
                    *t++ = 0;
                }
                noOfContractions++;
              }
            }
            else {
                // TODO (claireho): does this work? Need more tests
                // The following code is to handle the UCA pre-context rules
                // for L/l with middle dot. We share the structures for contractionCombos.
                // The format for pre-context character is
                // contractions[0]: codepoint in element->cPoints[0]
                // contractions[1]: '\0' to differentiate from a contraction
                // contractions[2]: prefix char
                if (element->prefixSize>0) {
                    if(length > 1 || element->prefixSize > 1) {
                        fprintf(stderr,
                                "\nLine %d: Character with prefix, "
                                "either too many characters or prefix too long.\n",
                                (int)line);
                        exit(U_INTERNAL_PROGRAM_ERROR);
                    }
                    if(noOfContractions>=MAX_UCA_CONTRACTIONS) {
                      fprintf(stderr,
                              "\nMore than %d contractions. Please increase MAX_UCA_CONTRACTIONS in genuca.cpp. "
                              "Exiting...\n",
                              (int)MAX_UCA_CONTRACTIONS);
                      exit(U_BUFFER_OVERFLOW_ERROR);
                    }
                    UChar *t = &contractions[noOfContractions][0];
                    t[0]=element->cPoints[0];
                    t[1]=0;
                    t[2]=element->prefixChars[0];
                    t += 3;
                    for(length = 3; length < MAX_UCA_CONTRACTION_LENGTH; ++length) {
                        *t++ = 0;
                    }
                    noOfContractions++;
                }
            }

            /* we're first adding to inverse, because addAnElement will reverse the order */
            /* of code points and stuff... we don't want that to happen */
            if((element->CEs[0] >> 24) != 2) {
                // Add every element except for the special minimum-weight character U+FFFE
                // which has 02 weights.
                // If we had 02 weights in the invuca table, then tailoring primary
                // after an ignorable would try to put a weight before 02 which is not valid.
                // We could fix this in a complicated way in the from-rule-string builder,
                // but omitting this special element from invuca is simple and effective.
                addToInverse(element, status);
            }
            if(!(length > 1 && element->cPoints[0] == 0xFDD0)) {
              uprv_uca_addAnElement(t, element, status);
            }
        }
    }

    if(UCAVersion[0] == 0 && UCAVersion[1] == 0 && UCAVersion[2] == 0 && UCAVersion[3] == 0) {
        fprintf(stderr, "UCA version not specified. Cannot create data file!\n");
        uprv_uca_closeTempTable(t);
        uprv_free(opts);
        uprv_free(myD);
        fclose(data);
        return -1;
    }
/*    {
        uint32_t trieWord = utrie_get32(t->mapping, 0xDC01, NULL);
    }*/

    if (beVerbose) {
        printf("\nLines read: %u\n", (int)line);
        printf("Surrogate count: %i\n", (int)surrogateCount);
        printf("Raw data breakdown:\n");
        /*printf("Compact array stage1 top: %i, stage2 top: %i\n", t->mapping->stage1Top, t->mapping->stage2Top);*/
        printf("Number of contractions: %u\n", (int)noOfContractions);
        printf("Contraction image size: %u\n", (int)t->image->contractionSize);
        printf("Expansions size: %i\n", (int)t->expansions->position);
    }


    /* produce canonical closure for table */
    /* first set up constants for implicit calculation */
    uprv_uca_initImplicitConstants(status);
    /* do the closure */
    UnicodeSet closed;
    int32_t noOfClosures = uprv_uca_canonicalClosure(t, NULL, &closed, status);
    if(noOfClosures != 0) {
        fprintf(stderr, "Warning: %i canonical closures occured!\n", (int)noOfClosures);
        UnicodeString pattern;
        std::string utf8;
        closed.toPattern(pattern, TRUE).toUTF8String(utf8);
        fprintf(stderr, "UTF-8 pattern string: %s\n", utf8.c_str());
    }

    /* test */
    UCATableHeader *myData = uprv_uca_assembleTable(t, status);  

    if (beVerbose) {
        printf("Compacted data breakdown:\n");
        /*printf("Compact array stage1 top: %i, stage2 top: %i\n", t->mapping->stage1Top, t->mapping->stage2Top);*/
        printf("Number of contractions: %u\n", (int)noOfContractions);
        printf("Contraction image size: %u\n", (int)t->image->contractionSize);
        printf("Expansions size: %i\n", (int)t->expansions->position);
    }

    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error creating table: %s\n", u_errorName(*status));
        uprv_uca_closeTempTable(t);
        uprv_free(opts);
        uprv_free(myD);
        fclose(data);
        return -1;
    }

    /* populate the version info struct with version info*/
    myData->version[0] = UCOL_BUILDER_VERSION;
    myData->version[1] = UCAVersion[0];
    myData->version[2] = UCAVersion[1];
    myData->version[3] = UCAVersion[2];
    /*TODO:The fractional rules version should be taken from FractionalUCA.txt*/
    // Removed this macro. Instead, we use the fields below
    //myD->version[1] = UCOL_FRACTIONAL_UCA_VERSION;
    //myD->UCAVersion = UCAVersion; // out of FractionalUCA.txt
    uprv_memcpy(myData->UCAVersion, UCAVersion, sizeof(UVersionInfo));
    u_getUnicodeVersion(myData->UCDVersion);

    writeOutData(myData, &consts, &leadByteConstants, contractions, noOfContractions, outputDir, copyright, status);

    InverseUCATableHeader *inverse = assembleInverseTable(status);
    uprv_memcpy(inverse->UCAVersion, UCAVersion, sizeof(UVersionInfo));
    writeOutInverseData(inverse, outputDir, copyright, status);

    uprv_uca_closeTempTable(t);
    uprv_free(myD);
    uprv_free(opts);

    uprv_free(myData);
    uprv_free(inverse);
    
    uprv_free(leadByteConstants.LEAD_BYTE_TO_SCRIPTS_INDEX);
    uprv_free(leadByteConstants.LEAD_BYTE_TO_SCRIPTS_DATA);
    uprv_free(leadByteConstants.SCRIPT_TO_LEAD_BYTES_INDEX);
    uprv_free(leadByteConstants.SCRIPT_TO_LEAD_BYTES_DATA);
    
    fclose(data);

    return 0;
}

#endif /* #if !UCONFIG_NO_COLLATION */

enum {
    HELP_H,
    HELP_QUESTION_MARK,
    COPYRIGHT,
    VERSION,
    VERBOSE,
    ICUDATADIR
};

/* Keep these values in sync with the above enums */
static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_COPYRIGHT,
    UOPTION_VERSION,
    UOPTION_VERBOSE,
    UOPTION_ICUDATADIR
};

int main(int argc, char* argv[]) {
    uprv_memset(&UCAVersion, 0, 4);

    U_MAIN_INIT_ARGS(argc, argv);
    argc=u_parseArgs(argc, argv, LENGTHOF(options), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<2 || options[HELP_H].doesOccur || options[HELP_QUESTION_MARK].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] path/to/ICU/src/root\n"
            "\tRead in UCA collation text data and write out the binary collation data\n"
            "options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-V or --version     show a version message\n"
            "\t-c or --copyright   include a copyright notice\n"
            "\t-v or --verbose     turn on verbose output\n"
            "\t-i or --icudatadir  directory for locating any needed intermediate data files,\n"
            "\t                    followed by path, defaults to %s\n",
            argv[0], u_getDataDirectory());
        return argc<2 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }
    if(options[VERSION].doesOccur) {
        printf("genuca version %hu.%hu, ICU tool to read UCA text data and create UCA data tables for collation.\n",
#if UCONFIG_NO_COLLATION
            0, 0
#else
            UCA_FORMAT_VERSION_0, UCA_FORMAT_VERSION_1
#endif
            );
        printf(U_COPYRIGHT_STRING"\n");
        exit(0);
    }

    /* get the options values */
    beVerbose = options[VERBOSE].doesOccur;

    const char *copyright = NULL;
    if (options[COPYRIGHT].doesOccur) {
        copyright = U_COPYRIGHT_STRING;
    }

    if (options[ICUDATADIR].doesOccur) {
        u_setDataDirectory(options[ICUDATADIR].value);
    }
    /* Initialize ICU */
    IcuToolErrorCode errorCode("genuca");
    u_init(errorCode);
    if (errorCode.isFailure() && errorCode.get() != U_FILE_ACCESS_ERROR) {
        fprintf(stderr, "%s: can not initialize ICU.  status = %s\n",
            argv[0], errorCode.errorName());
        exit(errorCode.reset());
    }
    errorCode.reset();

    CharString icuSrcRoot(argv[1], errorCode);

    CharString icuSourceData(icuSrcRoot, errorCode);
    icuSourceData.appendPathPart("source", errorCode);
    icuSourceData.appendPathPart("data", errorCode);

    CharString srcDir(icuSourceData, errorCode);
    srcDir.appendPathPart("unidata", errorCode);

    CharString destDir(icuSourceData, errorCode);
    destDir.appendPathPart("in", errorCode);
    destDir.appendPathPart("coll", errorCode);

    CharString ucaFile(srcDir, errorCode);
    ucaFile.appendPathPart("FractionalUCA.txt", errorCode);

    if(errorCode.isFailure()) {
        fprintf(stderr, "genuca: unable to build file paths - %s\n",
                errorCode.errorName());
        return errorCode.reset();
    }

#if UCONFIG_NO_COLLATION

    UNewDataMemory *pData;
    const char *msg;
    
    msg = "genuca writes dummy " UCA_DATA_NAME "." UCA_DATA_TYPE " because of UCONFIG_NO_COLLATION, see uconfig.h";
    fprintf(stderr, "%s\n", msg);
    pData = udata_create(destDir.data(), UCA_DATA_TYPE, UCA_DATA_NAME, &dummyDataInfo,
                         NULL, errorCode);
    udata_writeBlock(pData, msg, strlen(msg));
    udata_finish(pData, errorCode);

    msg = "genuca writes dummy " INVC_DATA_NAME "." INVC_DATA_TYPE " because of UCONFIG_NO_COLLATION, see uconfig.h";
    fprintf(stderr, "%s\n", msg);
    pData = udata_create(destDir.data(), INVC_DATA_TYPE, INVC_DATA_NAME, &dummyDataInfo,
                         NULL, errorCode);
    udata_writeBlock(pData, msg, strlen(msg));
    udata_finish(pData, errorCode);

    return errorCode.reset();

#else

    return write_uca_table(ucaFile.data(), destDir.data(), copyright, errorCode);

#endif
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
