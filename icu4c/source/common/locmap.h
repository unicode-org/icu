/*
*****************************************************************************************
*
*   Copyright (C) 1996-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
*/
// $Revision: 1.9 $
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
// 09/18/00     george      Removed the memory leaks.
//===============================================================================

/* include this first so that we are sure to get WIN32 defined */
#include "unicode/utypes.h"

#if defined(WIN32) && !defined(LOCMAP_H)
#define LOCMAP_H

#define LANGUAGE_LCID(hostID) (uint16_t)(0x03FF & hostID)

U_CFUNC const char *T_convertToPosix(uint32_t hostid, UErrorCode* status);

#ifdef XP_CPLUSPLUS

struct ILcidPosixMap;

class IGlobalLocales {
public:
    /**
     * Convert a Windows LCID number to an ICU locale name.  For instance,
     * 0x0409 will be return "en_US".
     *
     * @param hostid the Windows LCID number.
     * @param status gets set to U_ILLEGAL_ARGUMENT_ERROR when the LCID has no
     *               equivalent ICU locale.
     */
    static const char*          convertToPosix(uint32_t hostid, UErrorCode* status);

    /**
     * Convert an ICU locale name to a Windows LCID number.  For instance,
     * "en_US" will be return 0x0409.
     *
     * @param posixid the Posix style locale id.
     * @param status gets set to U_ILLEGAL_ARGUMENT_ERROR when the Posix ID has
     *               no equivalent Windows LCID.
     */
    static uint32_t             convertToLCID(const char* posixID, UErrorCode* status);

    /**
     * Convert a Windows LCID number to a Windows language ID.
     * This removes the sort ID from the LCID.  For instance, information about
     * the modern sort verses traditional sort on Spanish is removed from the
     * ID.
     *
     * @param hostid the Windows LCID number.
     */
    static inline uint16_t      languageLCID(uint32_t hostID) {return LANGUAGE_LCID(hostID);}

protected:
//    IGlobalLocales() {}
//    IGlobalLocales(const IGlobalLocales&/* that*/) {}
//    IGlobalLocales& operator=(const IGlobalLocales&/* that*/) {return *this;}

//    ~IGlobalLocales() {}

    static void                 initializeMapRegions(void);
private:
                                
    static uint32_t             LocaleCount;
    static ILcidPosixMap*       PosixIDmap;
};

#endif

#endif
