/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1998-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File test.c
*
* Modification History:
*
*   Date          Name        Description
*   07/28/2000    Madhu       Creation 
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "ucmp8.h"
#include "umemstrm.h"
#include "cmemory.h"
#include "cintltst.h"
#include "ucol_imp.h"


static void TestUCMP8API(void);

void addCompactArrayTest(TestNode** root);


void
addCompactArrayTest(TestNode** root)
{
    addTest(root, &TestUCMP8API,   "ucmptst/TestUCMP8API");
}

static void query(CompactByteArray *array) {
      int32_t i = 0;
      const uint8_t *valuesSet=(uint8_t *)ucmp8_getArray(array);
      for(i =0 ; i< 10; i++ ){
          if(valuesSet[0] != (uint8_t)0xFD ){
               log_err("ERROR: did not get the values expected values\n");
               break;
          }
      }
}

static void TestUCMP8API(){
    UErrorCode status = U_ZERO_ERROR;
    CompactByteArray* ucmp8Array=NULL;
    CompactByteArray* pAliaser=NULL;
    CompactByteArray* pAdopter=NULL;

    CompactByteArray aliaser;
    CompactByteArray adopter;


    CompactByteArray ucmp8Array1;
    CompactByteArray ucmp8Array2;
    CompactByteArray ucmp8Clone;
    int8_t *values;
    uint8_t *valuesSet;
    static const int8_t TEST_DEFAULT_VALUE = (int8_t)0xFF;
    

    /*ucmp8_open*/
    log_verbose("Testing ucmp8_open()\n");
    ucmp8Array=ucmp8_open(TEST_DEFAULT_VALUE);
    if(ucmp8Array == NULL){
        log_err("ERROR: ucmp8_open() failed\n");
    }
    if( (int32_t)ucmp8_getCount(ucmp8Array) != (int32_t)ucmp8_getkUnicodeCount()) {
        log_err("ERROR: ucmp8_open failed\n");
    }

    /*ucmp8_init*/
    log_verbose("Testing ucmp8_init()\n");
    ucmp8_init(&ucmp8Array1, TEST_DEFAULT_VALUE);
    if( (int32_t)ucmp8_getCount(&ucmp8Array1) != (int32_t)ucmp8_getkUnicodeCount() || 
        ucmp8_getIndex(&ucmp8Array1) == NULL ||
        ucmp8_getArray(&ucmp8Array1) == NULL || 
        ucmp8Array1.fBogus != FALSE){
        log_err("Error: ucmp8_init() failed\n");
    }
    /*ucmp8_initBogus*/
    log_verbose("Testing ucmp8_initBogus()\n");
    ucmp8_initBogus(&ucmp8Array2);
    if((int32_t)ucmp8_getCount(&ucmp8Array2) != ucmp8Array2.fCount ||
        ucmp8_getIndex(&ucmp8Array2) != NULL ||
        ucmp8_getArray(&ucmp8Array2) != NULL || 
        ucmp8Array2.fBogus != TRUE){
        log_err("Error: ucmp8_initBogus() failed\n");
    }
    /*ucmp8_getkBlockCount*/
    if(ucmp8_getkBlockCount() != 128 ){
        log_err("Error in ucmp8_getkBlockCount()\n");
    } 
    values=(int8_t*)ucmp8_getArray(&ucmp8Array1);
    if((uint8_t)values[0] != (uint8_t)TEST_DEFAULT_VALUE){
        log_err("Error: getArray() or init failed\n");
    }
    
    /*ucmp8_compact*/
    if(ucmp8Array1.fCompact == TRUE){
        log_err("Error: ucmp8_open failed Got compact for expanded data\n");
    } 
    ucmp8_compact(&ucmp8Array1, 1);
    if(ucmp8Array1.fCompact != TRUE){
        log_err("Error: ucmp8_compact failed\n");
    } 
    /*ucmp8_set*/
    ucmp8_set(&ucmp8Array1, 0, (uint8_t)0xFE);
    valuesSet=(uint8_t*)ucmp8_getArray(&ucmp8Array1);
    if(valuesSet[0] != (uint8_t)0xFE ){
        log_err("ERROR: ucmp8_set() failed\n");
    }
    if(ucmp8Array1.fCompact == TRUE){
        log_err("Error: ucmp8_set didn't expand the compact data \n");
    } 

    /*ucmp8_set*/
    ucmp8_compact(&ucmp8Array1, 1);
    ucmp8_set(&ucmp8Array1, 0, (uint8_t)0xFD);
    valuesSet=(uint8_t*)ucmp8_getArray(&ucmp8Array1);
    if(valuesSet[0] != (uint8_t)0xFD ){
        log_err("ERROR: ucmp8_set() failed\n");
    }
    if(ucmp8Array1.fCompact == TRUE){
        log_err("Error: ucmp8_set didn't expand the compact data \n");
    }
    /*ucmp8_setRange*/
    ucmp8_compact(&ucmp8Array1, 1);
    ucmp8_setRange(&ucmp8Array1, 0,  10, (uint8_t)0xFD);
    query(&ucmp8Array1);

    log_verbose("Testing ucmp8_flattenMem()\n");
    {
        int32_t len = 0;
        const uint8_t *buff = NULL; 
        UMemoryStream *MS = uprv_mstrm_openNew(65536);
        int32_t size = ucmp8_flattenMem(&ucmp8Array1, MS);
        
        /* try after compacting */
        buff = uprv_mstrm_getBuffer(MS, &len);
        
        if(size == 0 || len == 0 || buff == NULL) {
            log_err("Unable to flatten!\n");
        } else {
            log_verbose("Testing ucmp8_initFromData()\n");
            ucmp8_initFromData(&ucmp8Clone, &buff, &status);
            if(U_FAILURE(status) || ucmp8_isBogus(&ucmp8Clone) == TRUE){
                log_err("ERROR: ucmp8_initFromData() failed\n");
                status = U_ZERO_ERROR;
            } else {
              query(&ucmp8Clone);
              ucmp8_close(&ucmp8Clone);
            }
        }
        uprv_mstrm_close(MS);
    }

/*
openAdopt, initAdopt, openAlias, initAlias
*/
    log_verbose("Testing aliasers and adopters\n");
    {
      int32_t count = ucmp8_getCount(&ucmp8Array1);
      const uint16_t *tIndex = ucmp8_getIndex(&ucmp8Array1);
      const int8_t *tValues = ucmp8_getArray(&ucmp8Array1);
      uint16_t *index = (uint16_t *)uprv_malloc(UCMP8_kIndexCount*sizeof(uint16_t));

      values = (int8_t *)uprv_malloc(count);

      memcpy(index, tIndex, UCMP8_kIndexCount*sizeof(uint16_t));
      memcpy(values, tValues, count);

      ucmp8_initAlias(&aliaser, index, values, count);
      query(&aliaser);
      ucmp8_close(&aliaser);

      pAliaser = ucmp8_openAlias(index, values, count);
      query(pAliaser);
      ucmp8_close(pAliaser);

      ucmp8_initAdopt(&adopter, index, values, count);    /* TODO:  BAD API.  Adopted memory MUST be allocated with uprv_malloc  */
      query(&adopter);
      ucmp8_close(&adopter);

      index = (uint16_t *)uprv_malloc(UCMP8_kIndexCount*sizeof(uint16_t));
      values = (int8_t *)uprv_malloc(count);

      memcpy(index, tIndex, UCMP8_kIndexCount*sizeof(uint16_t));
      memcpy(values, tValues, count);

      pAdopter = ucmp8_openAdopt(index, values, count);   /*  TODO:  BAD API  */
      query(pAdopter);
      ucmp8_close(pAdopter);

    }
    ucmp8_close(&ucmp8Array1);
    ucmp8_close(&ucmp8Array2);
    ucmp8_close(ucmp8Array);

}


