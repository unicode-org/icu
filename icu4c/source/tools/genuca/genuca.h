#ifndef UCADATA_H
#define UCADATA_H

#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/unicode.h"
#include "ucolimp.h"
#include "ucmp32.h"
#include "compitr.h"
#include "uhash.h"
#include "umemstrm.h"
#include "unewdata.h"
#ifdef WIN32
#include <direct.h>
#else
#include <unistd.h>
#endif

#define paddedsize(something) ((something)+((((something)%4)!=0)?(4-(something)%4):0))

/* UDataInfo for UCA mapping table */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x55, 0x43, 0x6f, 0x6c,     /* dataFormat="UCol"            */
    1, 0, 0, 0,                 /* formatVersion                */
    3, 0, 0, 0                  /* dataVersion = Unicode Version*/
};

/* UDataInfo for inverse UCA table */
static const UDataInfo invDataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x49, 0x6E, 0x76, 0x43,     /* dataFormat="InvC"            */
    1, 0, 0, 0,                 /* formatVersion                */
    3, 0, 0, 0                  /* dataVersion = Unicode Version*/
};

typedef struct {
    UChar codepoint;
    UChar uchars[128];
    UChar *cPoints;
    int32_t cSize;          /* Number of characters in sequence - for contraction */
    int32_t noOfCEs;        /* Number of collation elements                       */
    uint32_t CEs[128];      /* These are collation elements - there could be more than one - in case of expansion */
    uint32_t mapCE;         /* This is the value element maps in original table   */
    int32_t sizePrim[128];
    int32_t sizeSec[128];
    int32_t sizeTer[128];
    UBool variableTop;
    UBool caseBit;
    UBool isThai;
} UCAElements;

typedef struct {
    uint32_t *CEs;
    int32_t position;
    int32_t size;
} ExpansionTable;

struct ContractionTable;

struct ContractionTable {
    UChar *codePoints;
    uint32_t *CEs;
    int32_t position;
    int32_t size;
    int32_t backSize;
    UBool forward;
    ContractionTable *reversed;
};

void deleteElement(void *element);
int32_t readElement(char **from, char *to, char separator, UErrorCode *status);
int32_t addExpansion(uint32_t value, UErrorCode *status);
uint32_t getSingleCEValue(char *primary, char *secondary, char *tertiary, UBool caseBit, UErrorCode *status);
uint32_t processContraction(UCAElements *element, uint32_t existingCE, UBool forward, UErrorCode *status);
void printOutTable(UCATableHeader *myData, UErrorCode *status);
UCATableHeader *assembleTable(UChar variableTopValue, UErrorCode *status);
void processFile(FILE *data, UErrorCode *status);
/* This adds a read element, while testing for existence */
uint32_t addAnElement(UCAElements *element, UErrorCode *status);
UCAElements *readAnElement(FILE *data, UErrorCode *status);
void reverseElement(UCAElements *el);


#endif
