/*
 ********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
 *
 *  uconv_bld.c:
 *
 *  Defines functions that are used in the creation/initialization/deletion
 *  of converters and related structures.
 *  uses uconv_io.h routines to access disk information
 *  is used by ucnv.h to implement public API create/delete/flushCache routines
 * Modification History:
 * 
 *   Date        Name        Description
 * 
 *   06/20/2000  helena      OS/400 port changes; mostly typecast.
 *   06/29/2000  helena      Major rewrite of the callback interface.
*/


#include "ucnv_io.h"
#include "uhash.h"
#include "ucnv_bld.h"
#include "unicode/ucnv_err.h"
#include "ucnv_cnv.h"
#include "ucnv_imp.h"
#include "unicode/udata.h"
#include "unicode/ucnv.h"
#include "umutex.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"
#include "ucln_cmn.h"


#if 0
#include <stdio.h>
extern void UCNV_DEBUG_LOG(char *what, char *who, void *p, int l);
#define UCNV_DEBUG_LOG(x,y,z) UCNV_DEBUG_LOG(x,y,z,__LINE__)
#else
# define UCNV_DEBUG_LOG(x,y,z)
#endif

static const UConverterSharedData * const
converterData[UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES]={
    NULL, NULL, &_MBCSData, &_Latin1Data,
    &_UTF8Data, &_UTF16BEData, &_UTF16LEData, &_UTF32BEData, &_UTF32LEData,
    NULL, &_ISO2022Data, 
    &_LMBCSData1,&_LMBCSData2, &_LMBCSData3, &_LMBCSData4, &_LMBCSData5, &_LMBCSData6,
    &_LMBCSData8,&_LMBCSData11,&_LMBCSData16,&_LMBCSData17,&_LMBCSData18,&_LMBCSData19,
    &_HZData, &_SCSUData, &_ISCIIData, &_ASCIIData, &_UTF7Data
};

static struct {
  const char *name;
  const UConverterType type;
} const cnvNameType[] = {
  { "ISO88591", UCNV_LATIN_1 },
  { "UTF8", UCNV_UTF8 },
  { "UTF16BE", UCNV_UTF16_BigEndian },
  { "UTF16LE", UCNV_UTF16_LittleEndian },
#if U_IS_BIG_ENDIAN
  { "UTF16", UCNV_UTF16_BigEndian },
  { "UTF16_OppositeEndian", UCNV_UTF16_LittleEndian },
#else
  { "UTF16", UCNV_UTF16_LittleEndian },
  { "UTF16_OppositeEndian", UCNV_UTF16_BigEndian},
#endif
  { "UTF32BE", UCNV_UTF32_BigEndian },
  { "UTF32LE", UCNV_UTF32_LittleEndian },
#if U_IS_BIG_ENDIAN
  { "UTF32", UCNV_UTF32_BigEndian },
  { "UTF32_OppositeEndian", UCNV_UTF32_LittleEndian },
#else
  { "UTF32", UCNV_UTF32_LittleEndian },
  { "UTF32_OppositeEndian", UCNV_UTF32_BigEndian},
#endif
  { "ISO_2022", UCNV_ISO_2022 },
  { "LMBCS1", UCNV_LMBCS_1 },
  { "LMBCS2", UCNV_LMBCS_2 },
  { "LMBCS3", UCNV_LMBCS_3 },
  { "LMBCS4", UCNV_LMBCS_4 },
  { "LMBCS5", UCNV_LMBCS_5 },
  { "LMBCS6", UCNV_LMBCS_6 },
  { "LMBCS8", UCNV_LMBCS_8 },
  { "LMBCS11",UCNV_LMBCS_11 },
  { "LMBCS16",UCNV_LMBCS_16 },
  { "LMBCS17",UCNV_LMBCS_17 },
  { "LMBCS18",UCNV_LMBCS_18 },
  { "LMBCS19",UCNV_LMBCS_19 },
  { "HZ",UCNV_HZ },
  { "SCSU", UCNV_SCSU },
  { "ISCII", UCNV_ISCII },
  { "USASCII", UCNV_US_ASCII },
  { "UTF7", UCNV_UTF7 }
};


/*Takes an alias name gets an actual converter file name
 *goes to disk and opens it.
 *allocates the memory and returns a new UConverter object
 */
static UConverterSharedData *createConverterFromFile (const char *converterName, UErrorCode * err);

static const UConverterSharedData *getAlgorithmicTypeFromName (const char *realName);

/**
 * Un flatten shared data from a UDATA..
 */
U_CAPI  UConverterSharedData* U_EXPORT2 ucnv_data_unFlattenClone(UDataMemory *pData, UErrorCode *status);

/*initializes some global variables */
UHashtable *SHARED_DATA_HASHTABLE = NULL;

/* The calling function uses the global mutex, so there is no need to use it here too */
UBool ucnv_cleanup(void) {
    if (SHARED_DATA_HASHTABLE != NULL) {
        ucnv_flushCache();
        if (SHARED_DATA_HASHTABLE != NULL && uhash_count(SHARED_DATA_HASHTABLE) == 0) {
            uhash_close(SHARED_DATA_HASHTABLE);
            SHARED_DATA_HASHTABLE = NULL;
        }
    }
    return (SHARED_DATA_HASHTABLE == NULL);
}

static UBool
isCnvAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    return (UBool)(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->sizeofUChar==U_SIZEOF_UCHAR &&
        pInfo->dataFormat[0]==0x63 &&   /* dataFormat="cnvt" */
        pInfo->dataFormat[1]==0x6e &&
        pInfo->dataFormat[2]==0x76 &&
        pInfo->dataFormat[3]==0x74 &&
        pInfo->formatVersion[0]==6);
}

#define DATA_TYPE "cnv"

static UConverterSharedData *createConverterFromFile (const char *fileName, UErrorCode * err)
{
    UDataMemory *data;
    UConverterSharedData *sharedData;

    if (err == NULL || U_FAILURE (*err)) {
        return NULL;
    }

    data = udata_openChoice(NULL, DATA_TYPE, fileName, isCnvAcceptable, NULL, err);
    if(U_FAILURE(*err))
    {
        return NULL;
    }

    sharedData = ucnv_data_unFlattenClone(data, err);
    if(U_FAILURE(*err))
    {
        udata_close(data);
        return NULL;
    }

    return sharedData;
}

void 
copyPlatformString(char *platformString, UConverterPlatform pltfrm)
{
    switch (pltfrm)
    {
    case UCNV_IBM:
        {
            uprv_strcpy (platformString, "ibm");
            break;
        }
    default:
        {
            uprv_strcpy (platformString, "");
            break;
        }
    };
    
    return;
}

/*returns a converter type from a string
 */
static const UConverterSharedData *
getAlgorithmicTypeFromName(const char *realName)
{
    int i;
    for(i=0; i<sizeof(cnvNameType)/sizeof(cnvNameType[0]); ++i) {
        if(uprv_strcmp(realName, cnvNameType[i].name)==0) {
            return converterData[cnvNameType[i].type];
        }
    }
    return NULL;
}

/*Puts the shared data in the static hashtable SHARED_DATA_HASHTABLE */
void   shareConverterData(UConverterSharedData * data)
{
    UErrorCode err = U_ZERO_ERROR;
    /*Lazy evaluates the Hashtable itself */
    /*void *sanity = NULL;*/

    if (SHARED_DATA_HASHTABLE == NULL)
    {
        UHashtable* myHT = uhash_openSize (uhash_hashIChars, uhash_compareIChars,
                            ucnv_io_countAvailableAliases(&err),
                            &err);
        if (U_FAILURE(err))
            return;
        umtx_lock(NULL);
        if (SHARED_DATA_HASHTABLE == NULL)
            SHARED_DATA_HASHTABLE = myHT;
        else
            uhash_close(myHT);
        umtx_unlock(NULL);
    }
umtx_lock (NULL);
    /* ### check to see if the element is not already there! */

    /*
    sanity =   getSharedConverterData (data->staticData->name);
    if(sanity != NULL)
    {
    UCNV_DEBUG_LOG("put:overwrite!",data->staticData->name,sanity);
    }
    UCNV_DEBUG_LOG("put:chk",data->staticData->name,sanity);
    */

    uhash_put(SHARED_DATA_HASHTABLE,
            (void*) data->staticData->name, /* Okay to cast away const as long as
            keyDeleter == NULL */
            data,
            &err);
    UCNV_DEBUG_LOG("put", data->staticData->name,data);
umtx_unlock (NULL);
}

UConverterSharedData *getSharedConverterData(const char *name)
{
    /*special case when no Table has yet been created we return NULL */
    if (SHARED_DATA_HASHTABLE == NULL)
    {
        return NULL;
    }
    else
    {
        UConverterSharedData *rc;

umtx_lock(NULL);
        rc = (UConverterSharedData*)uhash_get(SHARED_DATA_HASHTABLE, name);
umtx_unlock(NULL);
        UCNV_DEBUG_LOG("get",name,rc);
        return rc;
    }
}

/*frees the string of memory blocks associates with a sharedConverter
 *if and only if the referenceCounter == 0
 */
UBool   deleteSharedConverterData(UConverterSharedData * deadSharedData)
{
    if (deadSharedData->referenceCounter > 0)
        return FALSE;
    
    if (deadSharedData->impl->unload != NULL) {
        deadSharedData->impl->unload(deadSharedData);
    }

    if(deadSharedData->dataMemory != NULL)
    {
        UDataMemory *data = (UDataMemory*)deadSharedData->dataMemory;
        udata_close(data);
    }

    if(deadSharedData->table != NULL)
    {
        uprv_free(deadSharedData->table);
    }

    uprv_free(deadSharedData);
    
    return TRUE;
}

static void
parseConverterOptions(const char *inName,
                      char *cnvName, char *locale, uint32_t *pFlags) {
    char c;

    /* copy the converter name itself to cnvName */
    while((c=*inName)!=0 && c!=UCNV_OPTION_SEP_CHAR) {
        *cnvName++=c;
        ++inName;
    }
    *cnvName=0;

    /* parse options */
    if(c==UCNV_OPTION_SEP_CHAR) {
        ++inName;
        for(;;) {
            /* inName is behind an option separator */
            if(uprv_strncmp(inName, "locale=", 7)==0) {
                /* do not modify locale itself in case we have multiple locale options */
                char *dest=locale;

                /* copy the locale option value */
                inName+=7;
                for(;;) {
                    c=*inName;
                    if(c!=0) {
                        ++inName;
                        if(c!=UCNV_OPTION_SEP_CHAR) {
                            *dest++=c;
                        } else {
                            *dest=0;
                            break;
                        }
                    } else {
                        *dest=0;
                        return;
                    }
                }
            } else if(uprv_strncmp(inName, "version=", 8)==0) {
                /* copy the version option value into bits 3..0 of *pFlags */
                inName+=8;
                c=*inName;
                *pFlags=0;
                if(c==0) {
                    return;
                } else if((uint8_t)(c-'0')<10) {
                    *pFlags=c-'0';
                    ++inName;
                }
            /* add processing for new options here with another } else if(uprv_strncmp(inName, "option-name=", XX)==0) { */
            } else {
                /* ignore any other options until we define some */
                do {
                    c=*inName++;
                    if(c==0) {
                        return;
                    }
                } while(c!=UCNV_OPTION_SEP_CHAR);
            }
        }
    }
}

/*Logic determines if the converter is Algorithmic AND/OR cached
 *depending on that:
 * -we either go to get data from disk and cache it (Data=TRUE, Cached=False)
 * -Get it from a Hashtable (Data=X, Cached=TRUE)
 * -Call dataConverter initializer (Data=TRUE, Cached=TRUE)
 * -Call AlgorithmicConverter initializer (Data=FALSE, Cached=TRUE)
 */
UConverter *
createConverter (const char *converterName, UErrorCode * err)
{
    char cnvName[100], locale[20];
    const char *realName;
    UConverter *myUConverter = NULL;
    UConverterSharedData *mySharedConverterData = NULL;
    UErrorCode internalErrorCode = U_ZERO_ERROR;
    uint32_t options=0;
    if (U_FAILURE (*err))
        return NULL;

    locale[0] = 0;

    /* In case "name" is NULL we want to open the default converter. */
    if (converterName == NULL) {
        realName = ucnv_io_getDefaultConverterName();
        if (realName == NULL) {
            *err = U_MISSING_RESOURCE_ERROR;
            return NULL;
        }
        /* the default converter name is already canonical */
    } else {
        /* separate the converter name from the options */
        parseConverterOptions(converterName, cnvName, locale,&options);

        /* get the canonical converter name */
        realName = ucnv_io_getConverterName(cnvName, &internalErrorCode);
        if (U_FAILURE(internalErrorCode) || realName == NULL) {
            /*
            * set the input name in case the converter was added
            * without updating the alias table, or when there is no alias table
            */
            realName = cnvName;
        }
    }

    /* separate the converter name from the options */
    if(realName != cnvName) {
        parseConverterOptions(realName, cnvName, locale,&options);
        realName = cnvName;
    }
    
    /* get the shared data for an algorithmic converter, if it is one */
    mySharedConverterData = (UConverterSharedData *)getAlgorithmicTypeFromName (realName);
    if (mySharedConverterData == NULL)
    {
        /* it is a data-based converter, get its shared data */
        mySharedConverterData = getSharedConverterData (realName);
        if (mySharedConverterData == NULL)
        {
            /*Not cached, we need to stream it in from file */
            mySharedConverterData = createConverterFromFile (realName, err);
            if (U_FAILURE (*err) || (mySharedConverterData == NULL))
            {
                return NULL;
            }
            else
            {
                /* share it with other library clients */
                shareConverterData(mySharedConverterData);
            }
        }
        else
        {
            /* ### this is unsafe: the shared data could have been deleted since sharing or getting it - these operations should increase the counter! */
            /* update the reference counter: one more client */
            umtx_lock (NULL);
            mySharedConverterData->referenceCounter++;
            umtx_unlock (NULL);
        }
    }

    /* allocate the converter */
    myUConverter = (UConverter *) uprv_malloc (sizeof (UConverter));
    if (myUConverter == NULL)
    {
        if (mySharedConverterData->referenceCounter != ~0) {
            umtx_lock (NULL);
            --mySharedConverterData->referenceCounter;
            umtx_unlock (NULL);
        }
        *err = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    /* initialize the converter */
    uprv_memset(myUConverter, 0, sizeof(UConverter));
    myUConverter->sharedData = mySharedConverterData;
    myUConverter->mode = UCNV_SI;
    myUConverter->fromCharErrorBehaviour = (UConverterToUCallback) UCNV_TO_U_CALLBACK_SUBSTITUTE;
    myUConverter->fromUCharErrorBehaviour = (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_SUBSTITUTE;
    myUConverter->toUnicodeStatus = myUConverter->sharedData->toUnicodeStatus;
    myUConverter->subChar1 = myUConverter->sharedData->staticData->subChar1;
    myUConverter->subCharLen = myUConverter->sharedData->staticData->subCharLen;
    uprv_memcpy (myUConverter->subChar, myUConverter->sharedData->staticData->subChar, myUConverter->subCharLen);

    if(myUConverter != NULL && myUConverter->sharedData->impl->open != NULL) {
        myUConverter->sharedData->impl->open(myUConverter, realName, locale,options, err);
        if(U_FAILURE(*err)) {
            ucnv_close(myUConverter);
            return NULL;
        }
    }

    return myUConverter;
}

UConverterSharedData* ucnv_data_unFlattenClone(UDataMemory *pData, UErrorCode *status)
{
    /* UDataInfo info; -- necessary only if some converters have different formatVersion */
    const uint8_t *raw = (const uint8_t *)udata_getMemory(pData);
    const UConverterStaticData *source = (const UConverterStaticData *) raw;
    UConverterSharedData *data;
    UConverterType type = (UConverterType)source->conversionType;

    if(U_FAILURE(*status))
        return NULL;

    if( (uint16_t)type >= UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES ||
        converterData[type] == NULL ||
        converterData[type]->referenceCounter != 1 ||
        source->structSize != sizeof(UConverterStaticData))
    {
        *status = U_INVALID_TABLE_FORMAT;
        return NULL;
    }

#if 0
    /* necessary only if some converters have different formatVersion; now everything is at version 5 */
    /* test for the format version: MBCS is at version 5, the rest still at 4 */
    info.size=sizeof(UDataInfo);
    udata_getInfo(pData, &info);
    if(type == UCNV_MBCS ? info.formatVersion[0] != 5 : info.formatVersion[0] != 4) {
        *status = U_INVALID_TABLE_FORMAT;
        return NULL;
    }
#endif

    data = (UConverterSharedData *)uprv_malloc(sizeof(UConverterSharedData));
    if(data == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    /* copy initial values from the static structure for this type */
    uprv_memcpy(data, converterData[type], sizeof(UConverterSharedData));

    /* ### it would be much more efficient if the table were a direct member, not a pointer */
    data->table = (UConverterTable *)uprv_malloc(sizeof(UConverterTable));
    if(data->table == NULL) {
        uprv_free(data);
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    
    data->staticData = source;

    /* fill in fields from the loaded data */
    data->dataMemory = (void*)pData; /* for future use */

    if(data->impl->load != NULL) {
        data->impl->load(data, raw + source->structSize, status);
        if(U_FAILURE(*status)) {
            uprv_free(data->table);
            uprv_free(data);
            return NULL;
        }
    }
    return data;
}
