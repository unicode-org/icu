/*
******************************************************************************
*                                                                            *
* Copyright (C) 2003-2004, International Business Machines                        *
*                Corporation and others. All Rights Reserved.                *
*                                                                            *
******************************************************************************
*   file name:  ulocdata.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003Oct21
*   created by: Ram Viswanadha
*/

#include "unicode/ulocdata.h"

#define EXEMPLAR_CHARS      "ExemplarCharacters"
#define MEASUREMENT_SYSTEM  "MeasurementSystem"
#define PAPER_SIZE          "PaperSize"

U_CAPI USet* U_EXPORT2 
ulocdata_getExemplarSet(USet *fillIn, const char *localeID,
                        uint32_t options, UErrorCode *status){
    
    UResourceBundle *bundle = NULL;
    const UChar *exemplarChars = NULL;
    int32_t len = 0;

    if (U_FAILURE(*status)){
        return NULL;
    }
    
    bundle = ures_open(NULL, localeID, status);
    
    exemplarChars = ures_getStringByKey(bundle, EXEMPLAR_CHARS, &len, status);
    
    if(fillIn != NULL){
        uset_applyPattern(fillIn, exemplarChars, len, 
                          USET_IGNORE_SPACE | options, status);
    }else{
        fillIn = uset_openPatternOptions(exemplarChars, len,
                                         USET_IGNORE_SPACE | options, status);
    }
    
    ures_close(bundle);

    return fillIn;

}

U_CAPI UMeasurementSystem U_EXPORT2
ulocdata_getMeasurementSystem(const char *localeID, UErrorCode *status){
    
    UResourceBundle* bundle=NULL;
    UResourceBundle* measurement=NULL;
    UMeasurementSystem system = UMS_LIMIT; 
    
    if(status == NULL || U_FAILURE(*status)){
        return system;
    }
    
    bundle = ures_open(NULL, localeID, status);

    measurement = ures_getByKey(bundle, MEASUREMENT_SYSTEM, NULL, status);

    system = (UMeasurementSystem) ures_getInt(measurement, status);

    ures_close(bundle);
    ures_close(measurement);

    return system;

}

U_CAPI void U_EXPORT2
ulocdata_getPaperSize(const char* localeID, int32_t *height, int32_t *width, UErrorCode *status){
    UResourceBundle* bundle=NULL;
    UResourceBundle* paperSizeBundle = NULL;
    const int32_t* paperSize=NULL;
    int32_t len = 0;

    if(status == NULL || U_FAILURE(*status)){
        return;
    }
    
    bundle = ures_open(NULL, localeID, status);
    paperSizeBundle = ures_getByKey(bundle, PAPER_SIZE, NULL, status);
    paperSize = ures_getIntVector(paperSizeBundle, &len,  status);
    
    if(U_SUCCESS(*status)){
        if(len < 2){
            *status = U_INTERNAL_PROGRAM_ERROR;
        }else{
            *height = paperSize[0];
            *width  = paperSize[1];
        }
    }
    
    ures_close(bundle);
    ures_close(paperSizeBundle);
    
}
