/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"

#include "cintltst.h"
#include "ustr_imp.h"
void TestFlushCache(void); /* keep gcc happy */


void TestFlushCache(void) {
    UErrorCode          err                 =   U_ZERO_ERROR;
    UConverter*            someConverters[5];
    int flushCount = 0;

    /* flush the converter cache to get a consistent state before the flushing is tested */
    ucnv_flushCache();

    /*Testing ucnv_open()*/
    /* Note: These converters have been chosen because they do NOT
       encode the Latin characters (U+0041, ...), and therefore are
       highly unlikely to be chosen as system default codepages */

    someConverters[0] = ucnv_open("ibm-1047", &err);
    if (U_FAILURE(err)) {
        log_data_err("FAILURE! %s\n", myErrorName(err));
    }

    someConverters[1] = ucnv_open("ibm-1047", &err);
    if (U_FAILURE(err)) {
        log_data_err("FAILURE! %s\n", myErrorName(err));
    }

    someConverters[2] = ucnv_open("ibm-1047", &err);
    if (U_FAILURE(err)) {
        log_data_err("FAILURE! %s\n", myErrorName(err));
    }

    someConverters[3] = ucnv_open("gb18030", &err);
    if (U_FAILURE(err)) {
        log_data_err("FAILURE! %s\n", myErrorName(err));
    }

    someConverters[4] = ucnv_open("ibm-954", &err);
    if (U_FAILURE(err)) {
        log_data_err("FAILURE! %s\n", myErrorName(err));
    }


    /* Testing ucnv_flushCache() */
    log_verbose("\n---Testing ucnv_flushCache...\n");
    if ((flushCount=ucnv_flushCache())==0)
        log_verbose("Flush cache ok\n");
    else 
        log_data_err("Flush Cache failed [line %d], expect 0 got %d \n", __LINE__, flushCount);

    /*testing ucnv_close() and ucnv_flushCache() */
    ucnv_close(someConverters[0]);
    ucnv_close(someConverters[1]);

    if ((flushCount=ucnv_flushCache())==0)
        log_verbose("Flush cache ok\n");
    else 
        log_data_err("Flush Cache failed [line %d], expect 0 got %d \n", __LINE__, flushCount);

    ucnv_close(someConverters[2]);
    ucnv_close(someConverters[3]);

    if ((flushCount=ucnv_flushCache())==2) 
        log_verbose("Flush cache ok\n");  /*because first, second and third are same  */
    else 
        log_data_err("Flush Cache failed  line %d, got %d expected 2 or there is an error in ucnv_close()\n",
            __LINE__,
            flushCount);

    ucnv_close(someConverters[4]);
    if ( (flushCount=ucnv_flushCache())==1) 
        log_verbose("Flush cache ok\n");
    else 
        log_data_err("Flush Cache failed line %d, expected 1 got %d \n", __LINE__, flushCount);

}



