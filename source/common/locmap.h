/*
*****************************************************************************************
*
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
*/
// $Revision: 1.7 $
//===============================================================================
//
// File locmap.hpp      : Locale Mapping Classes
//
// 
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date        Name        Description
//  3/11/97     aliu        Added setId().
//  4/20/99     Madhu       Added T_convertToPosix()
//===============================================================================

/* include this first so that we are sure to get WIN32 defined */
#include "unicode/utypes.h"

#if defined(WIN32) && !defined(LOCMAP_H)
#define LOCMAP_H

#ifdef XP_CPLUSPLUS

class ILcidPosixMap;

class IGlobalLocales {
    public:
                static void                  initializeMapRegions(void);
                static const char*         convertToPosix(uint32_t hostid);
                static uint32_t              convertToLCID(const char* posixID);
                static uint16_t              languageLCID(uint32_t hostID);
    protected:
                IGlobalLocales() { }
                IGlobalLocales(const IGlobalLocales& that) { }
                IGlobalLocales& operator=(const IGlobalLocales& that) { return *this;}
private:

                static int32_t                  fgLocaleCount;
                static uint32_t                 fgStdLang;
                static const    uint32_t        kMapSize;
                static ILcidPosixMap *          fgPosixIDmap;
        
    protected:
                ~IGlobalLocales() { }
};

#endif

U_CFUNC const char *T_convertToPosix(uint32_t hostid);

#endif
