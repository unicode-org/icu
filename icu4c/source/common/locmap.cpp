/*
 **********************************************************************
 *   Copyright (C) 1996-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/
// $Revision: 1.6 $
//
// Provides functionality for mapping between
// LCID and Posix IDs.
//
// Note: All classes and code in this file are
//       intended for internal use only.
//
// Methods of interest:
//   void initializeMapRegions();
//   unsigned long convertToLCID(const int8_t*);
//   const int8_t* convertToPosix(unsigned long);
//
// Kathleen Wilson, 4/30/96
//
//  Date        Name        Description
//  3/11/97     aliu        Fixed off-by-one bug in assignment operator.  Added
//                          setId() method and safety check against 
//                          MAX_ID_LENGTH.
// 04/23/99     stephen     Added C wrapper for convertToPosix.

#ifdef WIN32

/*
 * Note:
 * This code is used only internally by putil.c/uprv_getDefaultLocaleID().
 * This means that this could be much simpler code, and the mapping
 * from Win32 locale ID numbers to POSIX locale strings should
 * be the faster one.
 */

#include <math.h>

#include "locmap.h"
#include "unicode/locid.h"
#include "mutex.h"
#include "cmemory.h"
#include "cstring.h"

int32_t        IGlobalLocales::fgLocaleCount = 0;
uint32_t       IGlobalLocales::fgStdLang = 0x0400;
const uint32_t IGlobalLocales::kMapSize = 40;
ILcidPosixMap* IGlobalLocales::fgPosixIDmap = 0;

/////////////////////////////////////////////////
//
// Internal Classes for LCID <--> POSIX Mapping
//
/////////////////////////////////////////////////

/* forward declaration */
class ILcidPosixMap;

class ILcidPosixElement
{
public:
  ILcidPosixElement(uint32_t, const char*);

  ILcidPosixElement();
  ILcidPosixElement(const ILcidPosixElement&);
  ILcidPosixElement& operator=(const ILcidPosixElement&);

  ~ILcidPosixElement();

private:
  uint32_t fHostID;
  const char *fPosixID;

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

////////////////////////////////////////////
//
// Create the table of LCID to POSIX Mapping
//
////////////////////////////////////////////


void
IGlobalLocales::initializeMapRegions()
{

  if (fgPosixIDmap != 0)  // already mapped
        return;

  
  ILcidPosixMap *newPosixIDmap = new ILcidPosixMap[40];
  newPosixIDmap[0].initialize(0x0436,  "af_ZA");       // 0
  newPosixIDmap[1].initialize(0x01,    "ar",     16);  // 1
  newPosixIDmap[2].initialize(0x0423,  "be_BY");       // 2
  newPosixIDmap[3].initialize(0x0402,  "bg_BG");       // 3
  newPosixIDmap[4].initialize(0x0403,  "ca_ES");       // 4
  newPosixIDmap[5].initialize(0x0405,  "cs_CS");       // 5
  newPosixIDmap[6].initialize(0x0406,  "da_DK");       // 6
  newPosixIDmap[7].initialize(0x07,    "de",     5);   // 7
  newPosixIDmap[8].initialize(0x0408,  "el_GR");       // 8
  newPosixIDmap[9].initialize(0x09,    "en",     9);   // 9
  newPosixIDmap[10].initialize(0x0a,   "es",     16);  // 10
  newPosixIDmap[11].initialize(0x0425, "et_EE");       // 11
  newPosixIDmap[12].initialize(0x042d, "eu_ES");       // 12
  newPosixIDmap[13].initialize(0x0429, "fa_IR");       // 13
  newPosixIDmap[14].initialize(0x040b, "fi_FI");       // 14
  newPosixIDmap[15].initialize(0x0c,   "fr",     5);   // 15
  newPosixIDmap[16].initialize(0x041a, "hr_HR");       // 16
  newPosixIDmap[17].initialize(0x040e, "hu_HU");       // 17
  newPosixIDmap[18].initialize(0x0421, "in_ID");       // 18
  newPosixIDmap[19].initialize(0x040f, "is_IS");       // 19
  newPosixIDmap[20].initialize(0x10,   "it",     2);   // 20
  newPosixIDmap[21].initialize(0x040d, "iw_IL");       // 21
  newPosixIDmap[22].initialize(0x0411, "ja_JP");       // 22
  newPosixIDmap[23].initialize(0x12,   "ko",     2);   // 23
  newPosixIDmap[24].initialize(0x0427, "lt_LT");       // 24
  newPosixIDmap[25].initialize(0x0426, "lv_LV");       // 25
  newPosixIDmap[26].initialize(0x13,   "nl",     2);   // 26
  newPosixIDmap[27].initialize(0x14,   "no",     2);   // 27
  newPosixIDmap[28].initialize(0x0415, "pl_PL");       // 28
  newPosixIDmap[29].initialize(0x16,   "pt",     2);   // 29
  newPosixIDmap[30].initialize(0x0418, "ro_RO");       // 30
  newPosixIDmap[31].initialize(0x0419, "ru_RU");       // 31
  newPosixIDmap[32].initialize(0x041b, "sk_SK");       // 32
  newPosixIDmap[33].initialize(0x0424, "sl_SI");       // 33
  newPosixIDmap[34].initialize(0x041c, "sq_AL");       // 34
  newPosixIDmap[35].initialize(0x041d, "sv_SE");       // 35
  newPosixIDmap[36].initialize(0x041e, "th_TH");       // 36
  newPosixIDmap[37].initialize(0x041f, "tr_TR");       // 37
  newPosixIDmap[38].initialize(0x0422, "uk_UA");       // 38
  newPosixIDmap[39].initialize(0x04,   "zh",     4);   // 39
  newPosixIDmap[1].addRegion(0x3801, "ar_AE");
  newPosixIDmap[1].addRegion(0x3c01, "ar_BH");
  newPosixIDmap[1].addRegion(0x1401, "ar_DZ");
  newPosixIDmap[1].addRegion(0x0c01, "ar_EG");
  newPosixIDmap[1].addRegion(0x0801, "ar_IQ");
  newPosixIDmap[1].addRegion(0x2c01, "ar_JO");
  newPosixIDmap[1].addRegion(0x3401, "ar_KW");
  newPosixIDmap[1].addRegion(0x3001, "ar_LB");
  newPosixIDmap[1].addRegion(0x1001, "ar_LY");
  newPosixIDmap[1].addRegion(0x1801, "ar_MA");
  newPosixIDmap[1].addRegion(0x2001, "ar_OM");
  newPosixIDmap[1].addRegion(0x4001, "ar_QA");
  newPosixIDmap[1].addRegion(0x0401, "ar_SA");
  newPosixIDmap[1].addRegion(0x2801, "ar_SY");
  newPosixIDmap[1].addRegion(0x1c01, "ar_TN");
  newPosixIDmap[1].addRegion(0x2401, "ar_YE");

  newPosixIDmap[7].addRegion(0x0c07, "de_AT");
  newPosixIDmap[7].addRegion(0x0807, "de_CH");
  newPosixIDmap[7].addRegion(0x0407, "de_DE");
  newPosixIDmap[7].addRegion(0x1407, "de_LI");
  newPosixIDmap[7].addRegion(0x1007, "de_LU");

  newPosixIDmap[9].addRegion(0x0c09, "en_AU");
  newPosixIDmap[9].addRegion(0x1009, "en_CA");
  newPosixIDmap[9].addRegion(0x0809, "en_GB");
  newPosixIDmap[9].addRegion(0x1809, "en_IE");
  newPosixIDmap[9].addRegion(0x2009, "en_JM");
  newPosixIDmap[9].addRegion(0x1409, "en_NZ");
  newPosixIDmap[9].addRegion(0x0409, "en_US");
  newPosixIDmap[9].addRegion(0x2409, "en_VI");
  newPosixIDmap[9].addRegion(0x1c09, "en_ZA");

  newPosixIDmap[10].addRegion(0x2c0a, "es_AR");
  newPosixIDmap[10].addRegion(0x400a, "es_BO");
  newPosixIDmap[10].addRegion(0x340a, "es_CL");
  newPosixIDmap[10].addRegion(0x240a, "es_CO");
  newPosixIDmap[10].addRegion(0x140a, "es_CR");
  newPosixIDmap[10].addRegion(0x1c0a, "es_DO");
  newPosixIDmap[10].addRegion(0x300a, "es_EC");
  newPosixIDmap[10].addRegion(0x0c0a, "es_ES");
  newPosixIDmap[10].addRegion(0x040a, "es_ES_T");
  newPosixIDmap[10].addRegion(0x100a, "es_GT");
  newPosixIDmap[10].addRegion(0x080a, "es_MX");
  newPosixIDmap[10].addRegion(0x180a, "es_PA");
  newPosixIDmap[10].addRegion(0x280a, "es_PE");
  newPosixIDmap[10].addRegion(0x3c0a, "es_PY");
  newPosixIDmap[10].addRegion(0x380a, "es_UY");
  newPosixIDmap[10].addRegion(0x200a, "es_VE");

  newPosixIDmap[15].addRegion(0x080c, "fr_BE");
  newPosixIDmap[15].addRegion(0x0c0c, "fr_CA");
  newPosixIDmap[15].addRegion(0x100c, "fr_CH");
  newPosixIDmap[15].addRegion(0x040c, "fr_FR");
  newPosixIDmap[15].addRegion(0x140c, "fr_LU");

  newPosixIDmap[20].addRegion(0x0810, "it_CH");
  newPosixIDmap[20].addRegion(0x0410, "it_IT");

  newPosixIDmap[23].addRegion(0x0812, "ko_KR");
  newPosixIDmap[23].addRegion(0x0412, "ko_KR");

  newPosixIDmap[26].addRegion(0x0813, "nl_BE");
  newPosixIDmap[26].addRegion(0x0413, "nl_NL");

  newPosixIDmap[27].addRegion(0x0414, "no_NO");
  newPosixIDmap[27].addRegion(0x0814, "no_NO_NY");

  newPosixIDmap[29].addRegion(0x0416, "pt_BR");
  newPosixIDmap[29].addRegion(0x0816, "pt_PT");

  newPosixIDmap[39].addRegion(0x0804, "zh_CN");
  newPosixIDmap[39].addRegion(0x0c04, "zh_HK");
  newPosixIDmap[39].addRegion(0x1004, "zh_SG");
  newPosixIDmap[39].addRegion(0x0404, "zh_TW");

  {
    Mutex m;
    if(fgPosixIDmap == 0)
    {
      fgPosixIDmap = newPosixIDmap;
      fgLocaleCount = 105;
      newPosixIDmap = 0; // successfully assigned it
    }
  }

  delete newPosixIDmap; // If it wasn't assigned. Don't delete these 40 inside a mutex.
}

//////////////////////////////////////
//
// LCID --> POSIX
//
/////////////////////////////////////

const char*
IGlobalLocales::convertToPosix(uint32_t hostid)
{
  initializeMapRegions();
    
  uint16_t langID = languageLCID(hostid);
  uint32_t index;
  for (index = 0; index < kMapSize; index++)
    {
      if (langID == fgPosixIDmap[index].hostLangID())
        {
          return fgPosixIDmap[index].posixID(hostid);
        }
    }

  //no match found
  return ILcidPosixMap::fgWildCard;
}

U_CFUNC const char *
T_convertToPosix(uint32_t hostid)
{
  return IGlobalLocales::convertToPosix(hostid);
}


//////////////////////////////////////
//
// POSIX --> LCID
//
/////////////////////////////////////

uint32_t
IGlobalLocales::convertToLCID(const char* posixID)
{
  if (!posixID || strlen(posixID) < 2)
    return 0;
  
  initializeMapRegions();

  //Binary search for the map entry

  uint32_t  low = 0, mid = 0;
  uint32_t  high = kMapSize - 1;

  char langID[3];
  langID[0] = posixID[0];
  langID[1] = posixID[1];
  langID[2] = 0;

  while (low <= high) {

    mid = (low + high) / 2;

    int32_t compVal = uprv_strcmp(langID, fgPosixIDmap[mid].posixLangID());

    if (mid == 0)  // not found
       break;
    if (compVal < 0)
      high = mid - 1;
    else if (compVal > 0)
      low = mid + 1;
    else  // found match!
      return fgPosixIDmap[mid].hostID(posixID);
    }
    // no match found
    return 0;
}

uint16_t
IGlobalLocales::languageLCID(uint32_t hostID)
{
    return (uint16_t)(0x03FF & hostID);
}

/////////////////////////////////////////////////////
//
// Given a hexadecimal number in decimal notation,
// find the decimal notation for the two lowest bits.
//
// e.g. given 0x3456 return 0x56 in decimal notation.
//
/////////////////////////////////////////////////////

ILcidPosixElement::ILcidPosixElement(uint32_t hid,
                                     const char* pid)
{
  fHostID = hid;
  fPosixID = pid;
}

ILcidPosixElement::ILcidPosixElement()
{
  fHostID = 0;
  fPosixID = NULL;
}


ILcidPosixElement::ILcidPosixElement(const ILcidPosixElement& that)
{
  fHostID = that.fHostID;
  fPosixID = that.fPosixID;
}

ILcidPosixElement&
ILcidPosixElement::operator=(const ILcidPosixElement& that)
{
  if (this != &that)
    {
      fHostID = that.fHostID;
      fPosixID = that.fPosixID;
    }
  return *this;
}


ILcidPosixElement::~ILcidPosixElement()
{
}


const char*  ILcidPosixMap::fgWildCard = "??_??";

void
ILcidPosixMap::initialize (uint32_t hostID,
                           const char* posixID,
                           uint32_t totalRegions)
{
  fHostLangID = IGlobalLocales::languageLCID(hostID);

  fPosixLangID[0] = posixID[0]; // don't care about these being called twice. not critical.
  fPosixLangID[1] = posixID[1];
  fPosixLangID[2] = 0;

  fMapSize = totalRegions + 1;
   fNumRegions=0;

  fRegionMaps = new ILcidPosixElement[fMapSize];

  //The first element will always be wild card
  fRegionMaps[0] =
    ILcidPosixElement(fHostLangID, fPosixLangID);

  if (totalRegions == 1 && strlen(posixID) >= 5)
    {
      fNumRegions++;

      fRegionMaps[1] =
        ILcidPosixElement(hostID, posixID);
    }
}

//default constructor is private, cannot be used.
ILcidPosixMap::ILcidPosixMap()
{
  fHostLangID = 0;
  fPosixLangID[0] = '?';
  fPosixLangID[1] = '?';
  fPosixLangID[2] = 0;

  fRegionMaps = 0;
  fMapSize = 0;
  fNumRegions = 0;
}

//copy constructor is private, cannot be used.
ILcidPosixMap::ILcidPosixMap(const ILcidPosixMap& that)
{
  fHostLangID = that.fHostLangID;
  fPosixLangID[0] = that.fPosixLangID[0];
  fPosixLangID[1] = that.fPosixLangID[1];
  fPosixLangID[2] = 0;

  fRegionMaps = 0;
  fMapSize = 0;
  fNumRegions = 0;
}

//assignment operator is private, cannot be used.
ILcidPosixMap&
ILcidPosixMap::operator=(const ILcidPosixMap& that)
{
  if (this != &that)
    {
      fHostLangID = that.fHostLangID;
      fPosixLangID[0] = that.fPosixLangID[0];
      fPosixLangID[1] = that.fPosixLangID[1];
      fPosixLangID[2] = 0;

      fRegionMaps = 0;
      fMapSize = 0;
      fNumRegions = 0;
    }
  return *this;
}

ILcidPosixMap::~ILcidPosixMap()
{
  if (fMapSize)
    delete [] fRegionMaps;
}

void ILcidPosixMap::addRegion (uint32_t hostID,
                               const char* posixID)
{
  if (fMapSize && fNumRegions < (fMapSize - 1))
    {
      ILcidPosixElement save(hostID,posixID);
      fNumRegions++;

      fRegionMaps[fNumRegions] = save;
    }
}


//assumes Posix IDs are sorted alphabetically
uint32_t
ILcidPosixMap::hostID(const char* posixID) const
{
  if (!fMapSize || strlen(posixID) < 5) //incomplete id
    return fHostLangID;

  //Binary search for the map entry
  //The element at index 0 is always the POSIX wild card,
  //so start search at index 1.

  uint32_t  low = 1, mid = 1;
  uint32_t  high = fNumRegions;

  while (low <= high) {

    mid = (low + high) / 2;

    int32_t compVal = uprv_strcmp(posixID, fRegionMaps[mid].fPosixID);

    if (compVal < 0)
      high = mid - 1;
    else if (compVal > 0)
      low = mid + 1;
    else  // found match!
      return fRegionMaps[mid].fHostID;
  }

  //no match found
  return fHostLangID;
}

const char*
ILcidPosixMap::posixID(uint32_t hostID) const
{
  uint32_t i;
  for (i = 0; i <= fNumRegions; i++)
    {
      if (fRegionMaps[i].fHostID == hostID)
        {
          return fRegionMaps[i].fPosixID;
        }
    }

  //if you get here, then no matching region was found,
  //so return the language id with the wild card region.
  return fRegionMaps[0].fPosixID;
}

#endif
