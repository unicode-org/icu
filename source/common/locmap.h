/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1996-1999               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/
// $Revision: 1.2 $
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
#ifndef LOCMAP_H
#define LOCMAP_H

#include "utypes.h"
#ifdef XP_CPLUSPLUS
class Locale;
/////////////////////////////////////////////////
//
// Internal Classes for LCID <--> POSIX Mapping
//
/////////////////////////////////////////////////

class ILcidPosixElement
{
public:
  ILcidPosixElement(uint32_t, const char*);

  ILcidPosixElement();
  ILcidPosixElement(const ILcidPosixElement&);
  ILcidPosixElement& operator=(const ILcidPosixElement&);

  ~ILcidPosixElement();

private:
  int32_t setId(const char* id);
  enum { MAX_ID_LENGTH = 8 };

  uint32_t fHostID;
  char fPosixID[MAX_ID_LENGTH];

  friend class ILcidPosixMap;
};

class ILcidPosixMap
{
public:

  ILcidPosixMap();
  void initialize (uint32_t hostID,
                   const char* posixID,
                   uint32_t totalNumberOfRegions = 1);

  ~ILcidPosixMap();

  void addRegion (uint32_t hostID,
                  const char* posixID);

  uint16_t hostLangID(void) const
  { return fHostLangID; };

  const char* posixLangID(void) const
  { return fPosixLangID; };

  uint32_t hostID(const char* fromPosixID) const;
  const char* posixID(uint32_t fromHostID) const;

  static const char* fgWildCard;


private:
  ILcidPosixMap(const ILcidPosixMap&);
  ILcidPosixMap& operator=(const ILcidPosixMap&);

  uint16_t fHostLangID;
  char fPosixLangID[3];

  ILcidPosixElement* fRegionMaps;
  uint32_t fMapSize;
  uint32_t fNumRegions;
};

//

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

#else
 CAPI const char* U_EXPORT2 T_convertToPosix(uint32_t hostid);
#endif
#endif



