/*
 **********************************************************************
 *   Copyright (C) 1996-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/
// $Revision: 1.10 $
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
#include "unicode/uloc.h"
#include "mutex.h"
#include "cmemory.h"
#include "cstring.h"

int32_t        IGlobalLocales::fgLocaleCount = 0;
uint32_t       IGlobalLocales::fgStdLang = 0x0400;
const uint32_t IGlobalLocales::kMapSize = 74;
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
  char fPosixLangID[128];

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

  
  ILcidPosixMap *newPosixIDmap = new ILcidPosixMap[IGlobalLocales::kMapSize];
  newPosixIDmap[0].initialize(0x0436,  "af_ZA");       //    af  Afrikaans                 0x36 
  newPosixIDmap[1].initialize(0x01,    "ar",     16);  //    ar  Arabic                    0x01 
  newPosixIDmap[2].initialize(0x044d,  "as_IN");       //    as  Assamese                  0x4d 
  newPosixIDmap[3].initialize(0x2c,  "az", 2);       //    az  Azerbaijani               0x2c 
  newPosixIDmap[4].initialize(0x0423,  "be_BY");       //    be  Byelorussian              0x23 
  newPosixIDmap[5].initialize(0x0402,  "bg_BG");       //    bg  Bulgarian                 0x02 
  newPosixIDmap[6].initialize(0x0445,  "bn_IN");       //    bn  Bengali; Bangla           0x45 
  newPosixIDmap[7].initialize(0x0403,  "ca_ES");       //    ca  Catalan                   0x03 
  newPosixIDmap[8].initialize(0x0405,  "cs_CZ");       //    cs  Czech                     0x05 
  newPosixIDmap[9].initialize(0x0406,  "da_DK");       //    da  Danish                    0x06 
  newPosixIDmap[10].initialize(0x07,   "de",     5);   //    de  German                    0x07 
  newPosixIDmap[11].initialize(0x0408, "el_GR");       //    el  Greek                     0x08 
  newPosixIDmap[12].initialize(0x09,   "en",     13);   //    en  English                   0x09 
  newPosixIDmap[13].initialize(0x0a,   "es",     20);  //    es  Spanish                   0x0a 
  newPosixIDmap[14].initialize(0x0425, "et_EE");       //    et  Estonian                  0x25 
  newPosixIDmap[15].initialize(0x042d, "eu_ES");       //    eu  Basque                    0x2d 
  newPosixIDmap[16].initialize(0x0429, "fa_IR");       //    fa  Farsi		      0x29 
  newPosixIDmap[17].initialize(0x040b, "fi_FI");       //    fi  Finnish                   0x0b 
  newPosixIDmap[18].initialize(0x0438, "fo_FO");       //    fo  Faroese                   0x38 
  newPosixIDmap[19].initialize(0x0c,   "fr",     6);   //    fr  French                    0x0c 
  newPosixIDmap[20].initialize(0x0447, "gu_IN");       //    gu  Gujarati                  0x47 
  newPosixIDmap[21].initialize(0x040d, "he_IL");       //    he  Hebrew (formerly iw)      0x0d 
  newPosixIDmap[22].initialize(0x0439, "hi_IN");       //    hi  Hindi                     0x39 
  newPosixIDmap[23].initialize(0x041a, "hr_HR");       //    hr  Croatian                  0x1a 
  newPosixIDmap[24].initialize(0x040e, "hu_HU");       //    hu  Hungarian                 0x0e 
  newPosixIDmap[25].initialize(0x042b, "hy_AM");       //    hy  Armenian                  0x2b 
  newPosixIDmap[26].initialize(0x0421, "id_ID");       //    id  Indonesian (formerly in)  0x21 
  newPosixIDmap[27].initialize(0x0421, "in_ID");       //    in  Indonesian 		      0x21 
  newPosixIDmap[28].initialize(0x040f, "is_IS");       //    is  Icelandic                 0x0f 
  newPosixIDmap[29].initialize(0x10,   "it",     2);   //    it  Italian                   0x10 
  newPosixIDmap[30].initialize(0x040d, "iw_IL");       //    iw  Hebrew 		      0x0d 
  newPosixIDmap[31].initialize(0x0411, "ja_JP");       //    ja  Japanese                  0x11 
  newPosixIDmap[32].initialize(0x0437, "ka_GE");       //    ka  Georgian                  0x37 
  newPosixIDmap[33].initialize(0x043f, "kk_KZ");       //    kk  Kazakh                    0x3f 
  newPosixIDmap[34].initialize(0x044b, "kn_IN");       //    kn  Kannada                   0x4b 
  newPosixIDmap[35].initialize(0x12,   "ko",     2);   //    ko  Korean                    0x12 
  newPosixIDmap[36].initialize(0x0457, "kok_IN");      //    kok Konkani		      0x57 
  newPosixIDmap[37].initialize(0x0460, "ks_IN");       //    ks  Kashmiri                  0x60 
  newPosixIDmap[38].initialize(0x0427, "lt_LT");       //    lt  Lithuanian                0x27 
  newPosixIDmap[39].initialize(0x0426, "lv_LV");       //    lv  Latvian, Lettish          0x26 
  newPosixIDmap[40].initialize(0x042f, "mk_MK");       //    mk  Macedonian                0x2f 
  newPosixIDmap[41].initialize(0x044c, "ml_IN");       //    ml  Malayalam                 0x4c 
  newPosixIDmap[42].initialize(0x0458, "mni_IN");      //    mni Manipuri		      0x58 
  newPosixIDmap[43].initialize(0x044e, "mr_IN");       //    mr  Marathi                   0x4e 
  newPosixIDmap[44].initialize(0x3e,   "ms", 	2);    //    ms  Malay                     0x3e 
  newPosixIDmap[45].initialize(0x61,   "ne", 	2);    //    ne  Nepali                    0x61 
  newPosixIDmap[46].initialize(0x13,   "nl",     2);   //    nl  Dutch                     0x13 
  newPosixIDmap[47].initialize(0x14,   "no",     2);   //    no  Norwegian                 0x14 
  newPosixIDmap[48].initialize(0x0448, "or_IN");       //    or  Oriya                     0x48 
  newPosixIDmap[49].initialize(0x0446, "pa_IN");       //    pa  Punjabi                   0x46 
  newPosixIDmap[50].initialize(0x0415, "pl_PL");       //    pl  Polish                    0x15 
  newPosixIDmap[51].initialize(0x16,   "pt",     2);   //    pt  Portuguese                0x16 
  newPosixIDmap[52].initialize(0x0418, "ro_RO");       //    ro  Romanian                  0x18 
  newPosixIDmap[53].initialize(0x00, "root");          //    root                          0x00
  newPosixIDmap[54].initialize(0x0419, "ru_RU");       //    ru  Russian                   0x19 
  newPosixIDmap[55].initialize(0x044f, "sa_IN");       //    sa  Sanskrit                  0x4f 
  newPosixIDmap[56].initialize(0x0459, "sd_IN");       //    sd  Sindhi                    0x59 
  newPosixIDmap[57].initialize(0x081a, "sh_YU");       //    sh  Serbo-Croatian	      0x1a 
  newPosixIDmap[58].initialize(0x041b, "sk_SK");       //    sk  Slovak                    0x1b 
  newPosixIDmap[59].initialize(0x0424, "sl_SI");       //    sl  Slovenian                 0x24 
  newPosixIDmap[60].initialize(0x041c, "sq_AL");       //    sq  Albanian                  0x1c 
  newPosixIDmap[61].initialize(0x0c1a, "sr_YU");       //    sr  Serbian                   0x1a 
  newPosixIDmap[62].initialize(0x1d,   "sv_SE",    2); //    sv  Swedish                   0x1d 
  newPosixIDmap[63].initialize(0x0441, "sw");          //    sw  Swahili                   0x41 
  newPosixIDmap[64].initialize(0x0449, "ta_IN");       //    ta  Tamil                     0x49 
  newPosixIDmap[65].initialize(0x044a, "te_IN");       //    te  Telugu                    0x4a 
  newPosixIDmap[66].initialize(0x041e, "th_TH");       //    th  Thai                      0x1e 
  newPosixIDmap[67].initialize(0x041f, "tr_TR");       //    tr  Turkish                   0x1f 
  newPosixIDmap[68].initialize(0x0444, "tt_RU");       //    tt  Tatar                     0x44 
  newPosixIDmap[69].initialize(0x0422, "uk_UA");       //    uk  Ukrainian                 0x22 
  newPosixIDmap[70].initialize(0x20,   "ur", 2);       //    ur  Urdu                      0x20 
  newPosixIDmap[71].initialize(0x43,   "uz_UZ", 2);    //    uz  Uzbek                     0x43 
  newPosixIDmap[72].initialize(0x042a, "vi_VN");       //    vi  Vietnamese                0x2a 
  newPosixIDmap[73].initialize(0x04,   "zh",     5);   //    zh  Chinese                   0x04 

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

  newPosixIDmap[3].addRegion(0x082c, "az_AZ_C");
  newPosixIDmap[3].addRegion(0x042c, "az_AZ_L");

  newPosixIDmap[10].addRegion(0x0c07, "de_AT");
  newPosixIDmap[10].addRegion(0x0807, "de_CH");
  newPosixIDmap[10].addRegion(0x0407, "de_DE");
  newPosixIDmap[10].addRegion(0x1407, "de_LI");
  newPosixIDmap[10].addRegion(0x1007, "de_LU");

  newPosixIDmap[12].addRegion(0x0c09, "en_AU");
  newPosixIDmap[12].addRegion(0x2809, "en_BZ");
  newPosixIDmap[12].addRegion(0x1009, "en_CA");
  newPosixIDmap[12].addRegion(0x0809, "en_GB");
  newPosixIDmap[12].addRegion(0x1809, "en_IE");
  newPosixIDmap[12].addRegion(0x2009, "en_JM");
  newPosixIDmap[12].addRegion(0x1409, "en_NZ");
  newPosixIDmap[12].addRegion(0x3409, "en_PH");
  newPosixIDmap[12].addRegion(0x2C09, "en_TT");
  newPosixIDmap[12].addRegion(0x0409, "en_US");
  newPosixIDmap[12].addRegion(0x2409, "en_VI");
  newPosixIDmap[12].addRegion(0x1c09, "en_ZA");
  newPosixIDmap[12].addRegion(0x3009, "en_ZW");

  newPosixIDmap[13].addRegion(0x2c0a, "es_AR");
  newPosixIDmap[13].addRegion(0x400a, "es_BO");
  newPosixIDmap[13].addRegion(0x340a, "es_CL");
  newPosixIDmap[13].addRegion(0x240a, "es_CO");
  newPosixIDmap[13].addRegion(0x140a, "es_CR");
  newPosixIDmap[13].addRegion(0x1c0a, "es_DO");
  newPosixIDmap[13].addRegion(0x300a, "es_EC");
  newPosixIDmap[13].addRegion(0x0c0a, "es_ES");
  newPosixIDmap[13].addRegion(0x040a, "es_ES_T");
  newPosixIDmap[13].addRegion(0x100a, "es_GT");
  newPosixIDmap[13].addRegion(0x480a, "es_HN");
  newPosixIDmap[13].addRegion(0x080a, "es_MX");
  newPosixIDmap[13].addRegion(0x4c0a, "es_NI");
  newPosixIDmap[13].addRegion(0x180a, "es_PA");
  newPosixIDmap[13].addRegion(0x280a, "es_PE");
  newPosixIDmap[13].addRegion(0x500a, "es_PR");
  newPosixIDmap[13].addRegion(0x3c0a, "es_PY");
  newPosixIDmap[13].addRegion(0x440a, "es_SV");
  newPosixIDmap[13].addRegion(0x380a, "es_UY");
  newPosixIDmap[13].addRegion(0x200a, "es_VE");

  newPosixIDmap[19].addRegion(0x080c, "fr_BE");
  newPosixIDmap[19].addRegion(0x0c0c, "fr_CA");
  newPosixIDmap[19].addRegion(0x100c, "fr_CH");
  newPosixIDmap[19].addRegion(0x040c, "fr_FR");
  newPosixIDmap[19].addRegion(0x140c, "fr_LU");
  newPosixIDmap[19].addRegion(0x180c, "fr_MC");

  newPosixIDmap[29].addRegion(0x0810, "it_CH");
  newPosixIDmap[29].addRegion(0x0410, "it_IT");

  newPosixIDmap[35].addRegion(0x0812, "ko_KP");
  newPosixIDmap[35].addRegion(0x0412, "ko_KR");

  newPosixIDmap[44].addRegion(0x083e, "ms_BN");  // Brunei Darussalam
  newPosixIDmap[44].addRegion(0x043e, "ms_MY");  // Malaysia

  newPosixIDmap[45].addRegion(0x0861, "ne_IN");  // India
  newPosixIDmap[45].addRegion(0x0461, "ne_NP");  // Nepal?

  newPosixIDmap[46].addRegion(0x0813, "nl_BE");
  newPosixIDmap[46].addRegion(0x0413, "nl_NL");

  newPosixIDmap[47].addRegion(0x0414, "no_NO");
  newPosixIDmap[47].addRegion(0x0814, "no_NO_NY");

  newPosixIDmap[51].addRegion(0x0416, "pt_BR");
  newPosixIDmap[51].addRegion(0x0816, "pt_PT");

  newPosixIDmap[62].addRegion(0x081d, "sv_FI");
  newPosixIDmap[62].addRegion(0x041d, "sv_SE");

  newPosixIDmap[70].addRegion(0x0820, "ur_IN");
  newPosixIDmap[70].addRegion(0x0420, "ur_PK");

  newPosixIDmap[71].addRegion(0x0843, "uz_UZ_C");
  newPosixIDmap[71].addRegion(0x0443, "uz_UZ_L");

  newPosixIDmap[73].addRegion(0x0804, "zh_CN");
  newPosixIDmap[73].addRegion(0x0c04, "zh_HK");
  newPosixIDmap[73].addRegion(0x1404, "zh_MO");
  newPosixIDmap[73].addRegion(0x1004, "zh_SG");
  newPosixIDmap[73].addRegion(0x0404, "zh_TW");

  {
    Mutex m;
    if(fgPosixIDmap == 0)
    {
      fgPosixIDmap = newPosixIDmap;
      fgLocaleCount = 105;
      newPosixIDmap = 0; // successfully assigned it
    }
  }

  if (newPosixIDmap)
    delete []newPosixIDmap; // If it wasn't assigned. Don't delete these 40 inside a mutex.
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

    UErrorCode status = U_ZERO_ERROR;
  if (!posixID || strlen(posixID) < 2)
    return 0;
  
  initializeMapRegions();

  //Binary search for the map entry

  uint32_t  low = 0, mid = 0;
  uint32_t  high = kMapSize - 1;

  char langID[1024];
  uloc_getLanguage(posixID, langID, 1024, &status);

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
    UErrorCode status = U_ZERO_ERROR;

  fHostLangID = IGlobalLocales::languageLCID(hostID);

  uloc_getLanguage(posixID, fPosixLangID, 128, &status);

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
      uprv_strcpy(fPosixLangID, that.fPosixLangID);

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
