/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  newconv.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000nov01
*   created by: Markus W. Scherer
*
*   This file contains the code for generating the actual data structures for
*   SBCS, DBCS, and EBCDIC_STATEFUL converters.
*
*   Special values in mapping tables:
*   fromUnicode
*       byte arrays:    0       stands for "unassigned", except for U+0000
*       int16_t arrays: 0xffff  "unassigned"
*   toUnicode
*       UChar arrays:   0xfffe  "unassigned"
*                       0xffff  "illegal"
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "cstring.h"
#include "cmemory.h"
#include "ucmp8.h"
#include "ucmp16.h"
#include "ucnv_bld.h"
#include "ucnv_cnv.h"
#include "unewdata.h"
#include "ucmpwrit.h"
#include "makeconv.h"

/* SBCS --------------------------------------------------------------------- */

typedef struct SBCSData {
    NewConverter newConverter;
    UConverterSBCSTable table;
} SBCSData;

/* prototypes */
static void
SBCSClose(NewConverter *cnvData);

static UBool
SBCSStartMappings(NewConverter *cnvData);

static UBool
SBCSAddToUnicode(NewConverter *cnvData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c, uint32_t b,
                 int8_t isFallback);

static UBool
SBCSAddFromUnicode(NewConverter *cnvData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c, uint32_t b,
                   int8_t isFallback);

static void
SBCSFinishMappings(NewConverter *cnvData, const UConverterStaticData *staticData);

static uint32_t
SBCSWrite(NewConverter *cnvData, const UConverterStaticData *staticData, UNewDataMemory *pData);

/* implementation */

NewConverter *
SBCSOpen() {
    SBCSData *sbcsData=(SBCSData *)uprv_malloc(sizeof(SBCSData));
    if(sbcsData!=NULL) {
        int i;

        uprv_memset(sbcsData, 0, sizeof(SBCSData));

        sbcsData->newConverter.close=SBCSClose;
        sbcsData->newConverter.startMappings=NULL;
        sbcsData->newConverter.addToUnicode=SBCSAddToUnicode;
        sbcsData->newConverter.addFromUnicode=SBCSAddFromUnicode;
        sbcsData->newConverter.finishMappings=SBCSFinishMappings;
        sbcsData->newConverter.write=SBCSWrite;

        /* initialize the fromUnicode compact arrays with zero-byte "unassigned" markers */
        ucmp8_init(&sbcsData->table.fromUnicode, 0);
        ucmp8_init(&sbcsData->table.fromUnicodeFallback, 0);

        /* allocate the toUnicode arrays and initialize them with U+fffe "unassigned" markers */
        sbcsData->table.toUnicode = (UChar*)uprv_malloc(sizeof(UChar)*256);
        sbcsData->table.toUnicodeFallback = (UChar*)uprv_malloc(sizeof(UChar)*256);
        for(i=0; i<=255; ++i) {
            sbcsData->table.toUnicode[i]=sbcsData->table.toUnicodeFallback[i]=0xfffe;
        }
    }
    return &sbcsData->newConverter;
}

static void
SBCSClose(NewConverter *cnvData) {
    SBCSData *sbcsData=(SBCSData *)cnvData;
    if(sbcsData!=NULL) {
        if(sbcsData->table.toUnicode!=NULL) {
            uprv_free(sbcsData->table.toUnicode);
        }
        if(sbcsData->table.toUnicodeFallback!=NULL) {
            uprv_free(sbcsData->table.toUnicodeFallback);
        }
        ucmp8_close(&sbcsData->table.fromUnicode);
        ucmp8_close(&sbcsData->table.fromUnicodeFallback);
        uprv_free(sbcsData);
    }
}

static UBool
SBCSAddToUnicode(NewConverter *cnvData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c, uint32_t b,
                 int8_t isFallback) {
    SBCSData *sbcsData=(SBCSData *)cnvData;
    UChar old;

    if(length!=1) {
        fprintf(stderr, "error: SBCS table contains multi-byte mapping at U+%04lx<->0x%02lx\n", c, b);
        return FALSE;
    }

    if((uint32_t)c>0xffff) {
        fprintf(stderr, "error: SBCS table contains Unicode code point >U+ffff at U+%04lx<->0x%02lx\n", c, b);
        return FALSE;
    }

    /* check that this codepage byte sequence does not have a mapping yet */
    if( (old=sbcsData->table.toUnicode[b])!=0xfffe ||
        (old=sbcsData->table.toUnicodeFallback[b])!=0xfffe
    ) {
        if(isFallback>=0) {
            fprintf(stderr, "error: duplicate codepage byte sequence at U+%04lx<->0x%02lx see U+%04x\n", c, b, old);
            return FALSE;
        } else if(VERBOSE) {
            fprintf(stderr, "duplicate codepage byte sequence at U+%04lx<->0x%02lx see U+%04x\n", c, b, old);
        }
    }

    if(isFallback<=0) {
        sbcsData->table.toUnicode[b]=(UChar)c;
    } else {
        sbcsData->table.toUnicodeFallback[b]=(UChar)c;
    }

    return TRUE;
}

static UBool
SBCSAddFromUnicode(NewConverter *cnvData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c, uint32_t b,
                   int8_t isFallback) {
    SBCSData *sbcsData=(SBCSData *)cnvData;
    uint8_t old;

    if(length!=1) {
        fprintf(stderr, "error: SBCS table contains multi-byte mapping at U+%04lx<->0x%02lx\n", c, b);
        return FALSE;
    }

    if((uint32_t)c>0xffff) {
        fprintf(stderr, "error: SBCS table contains Unicode code point >U+ffff at U+%04lx<->0x%02lx\n", c, b);
        return FALSE;
    }

    /* check that this Unicode code point does not have a mapping yet */
    if( (old=ucmp8_getu((&sbcsData->table.fromUnicode), (UChar)c))!=0 ||
        (old=ucmp8_getu((&sbcsData->table.fromUnicodeFallback), (UChar)c))!=0
    ) {
        if(isFallback>=0) {
            fprintf(stderr, "error: duplicate Unicode code point at U+%04lx<->0x%02lx see 0x%02x\n", c, b, old);
            return FALSE;
        } else if(VERBOSE) {
            fprintf(stderr, "duplicate Unicode code point at U+%04lx<->0x%02lx see 0x%02x\n", c, b, old);
        }
    }

    if(isFallback<=0) {
        ucmp8_set(&sbcsData->table.fromUnicode, (UChar)c, (int8_t)b);
    } else {
        ucmp8_set(&sbcsData->table.fromUnicodeFallback, (UChar)c, (int8_t)b);
    }

    return TRUE;
}

static void
SBCSFinishMappings(NewConverter *cnvData, const UConverterStaticData *staticData) {
    SBCSData *sbcsData=(SBCSData *)cnvData;
    if(staticData->hasFromUnicodeFallback) {
        ucmp8_compact(&sbcsData->table.fromUnicodeFallback, 1);
    }
    ucmp8_compact(&sbcsData->table.fromUnicode, 1);
}

static uint32_t
SBCSWrite(NewConverter *cnvData, const UConverterStaticData *staticData, UNewDataMemory *pData) {
    SBCSData *sbcsData=(SBCSData *)cnvData;
    uint32_t size=0;

    udata_writeBlock(pData, (void *)sbcsData->table.toUnicode, sizeof(uint16_t)*256);
    size+=sizeof(uint16_t)*256;
    size+=udata_write_ucmp8(pData, &sbcsData->table.fromUnicode);
    if(staticData->hasFromUnicodeFallback) {
        if(size%4) {
            udata_writePadding(pData, 4-(size%4));
            size+=4-(size%4);
        }
        size+=udata_write_ucmp8(pData, &sbcsData->table.fromUnicodeFallback);
    }
    if(staticData->hasToUnicodeFallback) {
        if(size%4) {
            udata_writePadding(pData, 4-(size%4));
            size+=4-(size%4);
        }
        udata_writeBlock(pData, (void*)sbcsData->table.toUnicodeFallback, sizeof(uint16_t)*256);
        size+=sizeof(uint16_t)*256;
        /* don't care about alignment anymore */
    }
    return size;
}

/* DBCS and EBCDIC_STATEFUL ------------------------------------------------- */

typedef struct DBCSData {
    NewConverter newConverter;
    UConverterDBCSTable table;
    UBool isEBCDICStateful;
} DBCSData;

/* prototypes */
static void
DBCSClose(NewConverter *cnvData);

static UBool
DBCSStartMappings(NewConverter *cnvData);

static UBool
DBCSAddToUnicode(NewConverter *cnvData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c, uint32_t b,
                 int8_t isFallback);

static UBool
DBCSAddFromUnicode(NewConverter *cnvData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c, uint32_t b,
                   int8_t isFallback);

static void
DBCSFinishMappings(NewConverter *cnvData, const UConverterStaticData *staticData);

static uint32_t
DBCSWrite(NewConverter *cnvData, const UConverterStaticData *staticData, UNewDataMemory *pData);

/* implementation */

NewConverter *
DBCSOpen() {
    DBCSData *dbcsData=(DBCSData *)uprv_malloc(sizeof(DBCSData));
    if(dbcsData!=NULL) {
        uprv_memset(dbcsData, 0, sizeof(DBCSData));

        dbcsData->newConverter.close=DBCSClose;
        dbcsData->newConverter.startMappings=NULL;
        dbcsData->newConverter.addToUnicode=DBCSAddToUnicode;
        dbcsData->newConverter.addFromUnicode=DBCSAddFromUnicode;
        dbcsData->newConverter.finishMappings=DBCSFinishMappings;
        dbcsData->newConverter.write=DBCSWrite;

        /* initialize the fromUnicode compact arrays with 0xffff "unassigned" markers */
        ucmp16_init(&dbcsData->table.fromUnicode, (int16_t)0xffff);
        ucmp16_init(&dbcsData->table.fromUnicodeFallback, (int16_t)0xffff);

        /* initialize the toUnicode compact arrays with U+fffe "unassigned" markers */
        ucmp16_init(&dbcsData->table.toUnicode, (int16_t)0xfffe);
        ucmp16_init(&dbcsData->table.toUnicodeFallback, (int16_t)0xfffe);
    }
    return &dbcsData->newConverter;
}

NewConverter *
EBCDICStatefulOpen() {
    DBCSData *dbcsData=(DBCSData *)DBCSOpen();
    if(dbcsData!=NULL) {
        dbcsData->isEBCDICStateful=TRUE;
    }
    return &dbcsData->newConverter;
}

static void
DBCSClose(NewConverter *cnvData) {
    DBCSData *dbcsData=(DBCSData *)cnvData;
    if(dbcsData!=NULL) {
        ucmp16_close(&dbcsData->table.fromUnicode);
        ucmp16_close(&dbcsData->table.fromUnicodeFallback);
        ucmp16_close(&dbcsData->table.toUnicode);
        ucmp16_close(&dbcsData->table.toUnicodeFallback);
        uprv_free(dbcsData);
    }
}

static UBool
DBCSAddToUnicode(NewConverter *cnvData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c, uint32_t b,
                 int8_t isFallback) {
    DBCSData *dbcsData=(DBCSData *)cnvData;
    uint16_t old;

    if(!dbcsData->isEBCDICStateful) {
        if(length!=2) {
            fprintf(stderr, "error: DBCS table contains non-double-byte mapping at U+%04lx<->0x%02lx\n", c, b);
            return FALSE;
        }
    } else {
        if(length!=1 && length!=2) {
            fprintf(stderr, "error: EBCDICStateful table contains more-than-double-byte mapping at U+%04lx<->0x%02lx\n", c, b);
            return FALSE;
        }
    }

    if((uint32_t)c>0xffff) {
        fprintf(stderr, "error: DBCS/EBCDICStateful table contains Unicode code point >U+ffff at U+%04lx<->0x%02lx\n", c, b);
        return FALSE;
    }

    /* check that this codepage byte sequence does not have a mapping yet */
    if( (old=ucmp16_getu((&dbcsData->table.toUnicode), b))!=0xfffe ||
        (old=ucmp16_getu((&dbcsData->table.toUnicodeFallback), b))!=0xfffe
    ) {
        if(isFallback>=0) {
            fprintf(stderr, "error: duplicate codepage byte sequence at U+%04lx<->0x%02lx see U+%04x\n", c, b, old);
            return FALSE;
        } else if(VERBOSE) {
            fprintf(stderr, "duplicate codepage byte sequence at U+%04lx<->0x%02lx see U+%04x\n", c, b, old);
        }
    }

    if(isFallback<=0) {
        ucmp16_set(&dbcsData->table.toUnicode, (UChar)b, (int16_t)c);
    } else {
        ucmp16_set(&dbcsData->table.toUnicodeFallback, (UChar)b, (int16_t)c);
    }

    return TRUE;
}

static UBool
DBCSAddFromUnicode(NewConverter *cnvData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c, uint32_t b,
                   int8_t isFallback) {
    DBCSData *dbcsData=(DBCSData *)cnvData;
    uint16_t old;

    if(!dbcsData->isEBCDICStateful) {
        if(length!=2) {
            fprintf(stderr, "error: DBCS table contains non-double-byte mapping at U+%04lx<->0x%02lx\n", c, b);
            return FALSE;
        }
    } else {
        if(length!=1 && length!=2) {
            fprintf(stderr, "error: EBCDICStateful table contains more-than-double-byte mapping at U+%04lx<->0x%02lx\n", c, b);
            return FALSE;
        }
    }

    if((uint32_t)c>0xffff) {
        fprintf(stderr, "error: DBCS/EBCDICStateful table contains Unicode code point >U+ffff at U+%04lx<->0x%02lx\n", c, b);
        return FALSE;
    }

    /* check that this Unicode code point does not have a mapping yet */
    if( (old=ucmp16_getu((&dbcsData->table.fromUnicode), (UChar)c))!=0xffff ||
        (old=ucmp16_getu((&dbcsData->table.fromUnicodeFallback), (UChar)c))!=0xffff
    ) {
        if(isFallback>=0) {
            fprintf(stderr, "error: duplicate Unicode code point at U+%04lx<->0x%02lx see 0x%02x\n", c, b, old);
            return FALSE;
        } else if(VERBOSE) {
            fprintf(stderr, "duplicate Unicode code point at U+%04lx<->0x%02lx see 0x%02x\n", c, b, old);
        }
    }

    if(isFallback<=0) {
        ucmp16_set(&dbcsData->table.fromUnicode, (UChar)c, (int16_t)b);
    } else {
        ucmp16_set(&dbcsData->table.fromUnicodeFallback, (UChar)c, (int16_t)b);
    }

    return TRUE;
}

static void
DBCSFinishMappings(NewConverter *cnvData, const UConverterStaticData *staticData) {
    DBCSData *dbcsData=(DBCSData *)cnvData;
    ucmp16_compact(&dbcsData->table.fromUnicode);
    ucmp16_compact(&dbcsData->table.toUnicode);
    if(staticData->hasFromUnicodeFallback) {
        ucmp16_compact(&dbcsData->table.fromUnicodeFallback);
    }
    if(staticData->hasToUnicodeFallback) {
        ucmp16_compact(&dbcsData->table.toUnicodeFallback);
    }
}

static uint32_t
DBCSWrite(NewConverter *cnvData, const UConverterStaticData *staticData, UNewDataMemory *pData) {
    DBCSData *dbcsData=(DBCSData *)cnvData;
    uint32_t size=0;

    size+=udata_write_ucmp16(pData, &dbcsData->table.toUnicode);
    if(size%4) {
        udata_writePadding(pData, 4-(size%4));
        size+=4-(size%4);
    }
    size+=udata_write_ucmp16(pData, &dbcsData->table.fromUnicode);
    if(staticData->hasFromUnicodeFallback) {
        if(size%4) {
            udata_writePadding(pData, 4-(size%4));
            size+=4-(size%4);
        }
        size+=udata_write_ucmp16(pData, &dbcsData->table.fromUnicodeFallback);
    }
    if(staticData->hasToUnicodeFallback) {
        if(size%4) {
            udata_writePadding(pData, 4-(size%4));
            size+=4-(size%4);
        }
        size+=udata_write_ucmp16(pData, &dbcsData->table.toUnicodeFallback);
        /* don't care about alignment anymore */
    }
    return size;
}
