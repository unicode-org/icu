/*
 **********************************************************************
 *   Copyright (C) 1996-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/
// $Revision: 1.11 $
//
// Provides functionality for mapping between
// LCID and Posix IDs.
//
// Note: All classes and code in this file are
//       intended for internal use only.
//
// Methods of interest:
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
// 09/18/00     george      Removed the memory leaks.

#ifdef WIN32

/*
 * Note:
 * This code is used only internally by putil.c/uprv_getDefaultLocaleID().
 * This means that this could be much simpler code, and the mapping
 * from Win32 locale ID numbers to POSIX locale strings should
 * be the faster one.
 */

#include "locmap.h"
#include "unicode/locid.h"
#include "unicode/uloc.h"
#include "mutex.h"
#include "cstring.h"

/////////////////////////////////////////////////
//
// Internal Classes for LCID <--> POSIX Mapping
//
/////////////////////////////////////////////////

struct ILcidPosixElement
{
    uint16_t hostID;
    const char *posixID;
};

struct ILcidPosixMap
{
    uint32_t hostID(const char* fromPosixID) const;
    const char* posixID(uint32_t fromHostID) const;

    static const char* fgWildCard;

    uint16_t hostLangID;
    const char *posixLangID;

    uint32_t numRegions;
    const ILcidPosixElement* regionMaps;
};

/////////////////////////////////////////////////
//
// Easy macros to make the LCID <--> POSIX Mapping
//
/////////////////////////////////////////////////

/*
 The standard one language/one country mapping for LCID.
 The first element must be the language, and the following
 elements are the language with the country.
 */
#define ILCID_POSIX_ELEMENT_ARRAY(hostID, languageID, posixID) \
static const ILcidPosixElement posixID[] = { \
    {LANGUAGE_LCID(hostID), #languageID},                  /* parent locale */ \
    {hostID, #posixID}, \
};

/*
 Create the map for the posixID. This macro supposes that the language string
 name is the same as the global variable name, and that the first element
 in the ILcidPosixElement is just the language.
 */
#define ILCID_POSIX_MAP(_posixID) \
    {LANGUAGE_LCID(_posixID[0].hostID), #_posixID, sizeof(_posixID)/sizeof(ILcidPosixElement), _posixID}

////////////////////////////////////////////
//
// Create the table of LCID to POSIX Mapping
//
////////////////////////////////////////////

ILCID_POSIX_ELEMENT_ARRAY(0x0436, af, af_ZA)

static const ILcidPosixElement ar[] = {
    {0x01,   "ar"},
    {0x3801, "ar_AE"},
    {0x3c01, "ar_BH"},
    {0x1401, "ar_DZ"},
    {0x0c01, "ar_EG"},
    {0x0801, "ar_IQ"},
    {0x2c01, "ar_JO"},
    {0x3401, "ar_KW"},
    {0x3001, "ar_LB"},
    {0x1001, "ar_LY"},
    {0x1801, "ar_MA"},
    {0x2001, "ar_OM"},
    {0x4001, "ar_QA"},
    {0x0401, "ar_SA"},
    {0x2801, "ar_SY"},
    {0x1c01, "ar_TN"},
    {0x2401, "ar_YE"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x044d, as, as_IN)    //Todo: Data does not exist

static const ILcidPosixElement az[] = {         //Todo: Data does not exist
    {0x2c,   "az"},
    {0x082c, "az_AZ_C"},
    {0x042c, "az_AZ_L"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0423, be, be_BY)
ILCID_POSIX_ELEMENT_ARRAY(0x0402, bg, bg_BG)
ILCID_POSIX_ELEMENT_ARRAY(0x0445, bn, bn_IN)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0403, ca, ca_ES)
ILCID_POSIX_ELEMENT_ARRAY(0x0405, cs, cs_CZ)
ILCID_POSIX_ELEMENT_ARRAY(0x0406, da, da_DK)

static const ILcidPosixElement de[] = {
    {0x07,   "de"},
    {0x0c07, "de_AT"},
    {0x0807, "de_CH"},
    {0x0407, "de_DE"},
    {0x1407, "de_LI"},    //Todo: Data does not exist
    {0x1007, "de_LU"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0408, el, el_GR)

static const ILcidPosixElement en[] = {
    {0x09,   "en"},
    {0x0c09, "en_AU"},
    {0x2809, "en_BZ"},    //Todo: Data does not exist
    {0x1009, "en_CA"},
    {0x0809, "en_GB"},
    {0x1809, "en_IE"},
    {0x2009, "en_JM"},    //Todo: Data does not exist
    {0x1409, "en_NZ"},
    {0x3409, "en_PH"},    //Todo: Data does not exist
    {0x2C09, "en_TT"},    //Todo: Data does not exist
    {0x0409, "en_US"},
    {0x2409, "en_VI"},    //Todo: Data does not exist
    {0x1c09, "en_ZA"},
    {0x3009, "en_ZW"}     //Todo: Data does not exist
};

static const ILcidPosixElement es[] = {
    {0x0a,   "es"},
    {0x2c0a, "es_AR"},
    {0x400a, "es_BO"},
    {0x340a, "es_CL"},
    {0x240a, "es_CO"},
    {0x140a, "es_CR"},
    {0x1c0a, "es_DO"},
    {0x300a, "es_EC"},
    {0x0c0a, "es_ES"},      //Modern sort.
    {0x040a, "es_ES_T"},    //Todo: Data does not exist. Traditional sort?
    {0x100a, "es_GT"},
    {0x480a, "es_HN"},
    {0x080a, "es_MX"},
    {0x4c0a, "es_NI"},
    {0x180a, "es_PA"},
    {0x280a, "es_PE"},
    {0x500a, "es_PR"},
    {0x3c0a, "es_PY"},
    {0x440a, "es_SV"},
    {0x380a, "es_UY"},
    {0x200a, "es_VE"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0425, et, et_EE)
ILCID_POSIX_ELEMENT_ARRAY(0x042d, eu, eu_ES)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0429, fa, fa_IR)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x040b, fi, fi_FI)
ILCID_POSIX_ELEMENT_ARRAY(0x0438, fo, fo_FO)    //Todo: Data does not exist

static const ILcidPosixElement fr[] = {
    {0x0c,   "fr"},
    {0x080c, "fr_BE"},
    {0x0c0c, "fr_CA"},
    {0x100c, "fr_CH"},
    {0x040c, "fr_FR"},
    {0x140c, "fr_LU"},
    {0x180c, "fr_MC"}    //Todo: Data does not exist
};

ILCID_POSIX_ELEMENT_ARRAY(0x0447, gu, gu_IN)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x040d, he, he_IL)
ILCID_POSIX_ELEMENT_ARRAY(0x0439, hi, hi_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x041a, hr, hr_HR)
ILCID_POSIX_ELEMENT_ARRAY(0x040e, hu, hu_HU)
ILCID_POSIX_ELEMENT_ARRAY(0x042b, hy, hy_AM)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0421, id, id_ID)    //Todo: Data does not exist
//ILCID_POSIX_ELEMENT_ARRAY(0x0421, in, in_ID)    //Should really be id_ID
ILCID_POSIX_ELEMENT_ARRAY(0x040f, is, is_IS)

static const ILcidPosixElement it[] = {
    {0x10,   "it"},
    {0x0810, "it_CH"},
    {0x0410, "it_IT"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x040d, iw, iw_IL)    //Should really be he_IL
ILCID_POSIX_ELEMENT_ARRAY(0x0411, ja, ja_JP)
ILCID_POSIX_ELEMENT_ARRAY(0x0437, ka, ka_GE)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x043f, kk, kk_KZ)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x044b, kn, kn_IN)    //Todo: Data does not exist

static const ILcidPosixElement ko[] = {
    {0x12,   "ko"},
    {0x0812, "ko_KP"},
    {0x0412, "ko_KR"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0457, kok, kok_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x0460, ks,  ks_IN)   //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0427, lt,  lt_LT)
ILCID_POSIX_ELEMENT_ARRAY(0x0426, lv,  lv_LV)
ILCID_POSIX_ELEMENT_ARRAY(0x042f, mk,  mk_MK)
ILCID_POSIX_ELEMENT_ARRAY(0x044c, ml,  ml_IN)   //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0458, mni, mni_IN)  //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x044e, mr,  mr_IN)

static const ILcidPosixElement ms[] = {         //Todo: Data does not exist
    {0x3e,   "ms"},
    {0x083e, "ms_BN"},   // Brunei Darussalam
    {0x043e, "ms_MY"}    // Malaysia
};

ILCID_POSIX_ELEMENT_ARRAY(0x043a, mt,  mt_MT)

static const ILcidPosixElement ne[] = {         //Todo: Data does not exist
    {0x61,   "ne"},
    {0x0861, "ne_IN"},   // India
    {0x0461, "ne_NP"}    // Nepal
};

static const ILcidPosixElement nl[] = {
    {0x13,   "nl"},
    {0x0813, "nl_BE"},
    {0x0413, "nl_NL"}
};

static const ILcidPosixElement no[] = {
    {0x14,   "no"},
    {0x0414, "no_NO"},
    {0x0814, "no_NO_NY"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0448, or, or_IN)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0446, pa, pa_IN)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0415, pl, pl_PL)

static const ILcidPosixElement pt[] = {
    {0x16,   "pt"},
    {0x0416, "pt_BR"},
    {0x0816, "pt_PT"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0418, ro, ro_RO)

static const ILcidPosixElement root[] = {
    {0x00,   "root"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0419, ru, ru_RU)
ILCID_POSIX_ELEMENT_ARRAY(0x044f, sa, sa_IN)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0459, sd, sd_IN)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x081a, sh, sh_YU)
ILCID_POSIX_ELEMENT_ARRAY(0x041b, sk, sk_SK)
ILCID_POSIX_ELEMENT_ARRAY(0x0424, sl, sl_SI)
ILCID_POSIX_ELEMENT_ARRAY(0x041c, sq, sq_AL)
ILCID_POSIX_ELEMENT_ARRAY(0x0c1a, sr, sr_YU)

static const ILcidPosixElement sv[] = {
    {0x1d,   "sv"},
    {0x081d, "sv_FI"},
    {0x041d, "sv_SE"}
};

static const ILcidPosixElement sw[] = {         //Todo: Data does not exist
    {0x41,   "sw"},
    {0x0441, "sw_KE"}   // The MSJDK documentation says the default country is Kenya.
};

ILCID_POSIX_ELEMENT_ARRAY(0x0449, ta, ta_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x044a, te, te_IN)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x041e, th, th_TH)
ILCID_POSIX_ELEMENT_ARRAY(0x041f, tr, tr_TR)
ILCID_POSIX_ELEMENT_ARRAY(0x0444, tt, tt_RU)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0422, uk, uk_UA)

static const ILcidPosixElement ur[] = {         //Todo: Data does not exist
    {0x20,   "ur"},
    {0x0820, "ur_IN"},
    {0x0420, "ur_PK"}
};

static const ILcidPosixElement uz_UZ[] = {      //Todo: Data does not exist
    {0x43,   "uz"},
    {0x0843, "uz_UZ_C"},
    {0x0443, "uz_UZ_L"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x042a, vi, vi_VN)

static const ILcidPosixElement zh[] = {
    {0x04,   "zh"},
    {0x0804, "zh_CN"},
    {0x0c04, "zh_HK"},
    {0x1404, "zh_MO"},
    {0x1004, "zh_SG"},
    {0x0404, "zh_TW"}
};

void
IGlobalLocales::initializeMapRegions()
{
/*  This commented out code leaks memory. Use the LCID values as reference only.
    These values originally came from winnt.h
*/
/*    if (fgPosixIDmap != 0)  // already mapped
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
    newPosixIDmap[16].initialize(0x0429, "fa_IR");       //    fa  Farsi                    0x29 
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
    newPosixIDmap[27].initialize(0x0421, "in_ID");       //    in  Indonesian               0x21 
    newPosixIDmap[28].initialize(0x040f, "is_IS");       //    is  Icelandic                 0x0f 
    newPosixIDmap[29].initialize(0x10,   "it",     2);   //    it  Italian                   0x10 
    newPosixIDmap[30].initialize(0x040d, "iw_IL");       //    iw  Hebrew                   0x0d 
    newPosixIDmap[31].initialize(0x0411, "ja_JP");       //    ja  Japanese                  0x11 
    newPosixIDmap[32].initialize(0x0437, "ka_GE");       //    ka  Georgian                  0x37 
    newPosixIDmap[33].initialize(0x043f, "kk_KZ");       //    kk  Kazakh                    0x3f 
    newPosixIDmap[34].initialize(0x044b, "kn_IN");       //    kn  Kannada                   0x4b 
    newPosixIDmap[35].initialize(0x12,   "ko",     2);   //    ko  Korean                    0x12 
    newPosixIDmap[36].initialize(0x0457, "kok_IN");      //    kok Konkani                  0x57 
    newPosixIDmap[37].initialize(0x0460, "ks_IN");       //    ks  Kashmiri                  0x60 
    newPosixIDmap[38].initialize(0x0427, "lt_LT");       //    lt  Lithuanian                0x27 
    newPosixIDmap[39].initialize(0x0426, "lv_LV");       //    lv  Latvian, Lettish          0x26 
    newPosixIDmap[40].initialize(0x042f, "mk_MK");       //    mk  Macedonian                0x2f 
    newPosixIDmap[41].initialize(0x044c, "ml_IN");       //    ml  Malayalam                 0x4c 
    newPosixIDmap[42].initialize(0x0458, "mni_IN");      //    mni Manipuri                 0x58 
    newPosixIDmap[43].initialize(0x044e, "mr_IN");       //    mr  Marathi                   0x4e 
    newPosixIDmap[44].initialize(0x3e,   "ms",    2);    //    ms  Malay                     0x3e 
    newPosixIDmap[45].initialize(0x61,   "ne",    2);    //    ne  Nepali                    0x61 
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
    newPosixIDmap[57].initialize(0x081a, "sh_YU");       //    sh  Serbo-Croatian           0x1a 
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
    newPosixIDmap[73].addRegion(0x0404, "zh_TW");*/

    // This must be static
    static ILcidPosixMap localPosixIDmap[] = {
        ILCID_POSIX_MAP(af_ZA),       //  af  Afrikaans                 0x36
        ILCID_POSIX_MAP(ar),          //  ar  Arabic                    0x01
        ILCID_POSIX_MAP(as_IN),       //  as  Assamese                  0x4d
        ILCID_POSIX_MAP(az),          //  az  Azerbaijani               0x2c
        ILCID_POSIX_MAP(be_BY),       //  be  Byelorussian              0x23
        ILCID_POSIX_MAP(bg_BG),       //  bg  Bulgarian                 0x02
        ILCID_POSIX_MAP(bn_IN),       //  bn  Bengali; Bangla           0x45
        ILCID_POSIX_MAP(ca_ES),       //  ca  Catalan                   0x03
        ILCID_POSIX_MAP(cs_CZ),       //  cs  Czech                     0x05
        ILCID_POSIX_MAP(da_DK),       //  da  Danish                    0x06
        ILCID_POSIX_MAP(de),          //  de  German                    0x07
        ILCID_POSIX_MAP(el_GR),       //  el  Greek                     0x08
        ILCID_POSIX_MAP(en),          //  en  English                   0x09
        ILCID_POSIX_MAP(es),          //  es  Spanish                   0x0a
        ILCID_POSIX_MAP(et_EE),       //  et  Estonian                  0x25
        ILCID_POSIX_MAP(eu_ES),       //  eu  Basque                    0x2d
        ILCID_POSIX_MAP(fa_IR),       //  fa  Farsi                     0x29
        ILCID_POSIX_MAP(fi_FI),       //  fi  Finnish                   0x0b
        ILCID_POSIX_MAP(fo_FO),       //  fo  Faroese                   0x38
        ILCID_POSIX_MAP(fr),          //  fr  French                    0x0c
        ILCID_POSIX_MAP(gu_IN),       //  gu  Gujarati                  0x47
        ILCID_POSIX_MAP(he_IL),       //  he  Hebrew (formerly iw)      0x0d
        ILCID_POSIX_MAP(hi_IN),       //  hi  Hindi                     0x39
        ILCID_POSIX_MAP(hr_HR),       //  hr  Croatian                  0x1a
        ILCID_POSIX_MAP(hu_HU),       //  hu  Hungarian                 0x0e
        ILCID_POSIX_MAP(hy_AM),       //  hy  Armenian                  0x2b
        ILCID_POSIX_MAP(id_ID),       //  id  Indonesian (formerly in)  0x21
//        ILCID_POSIX_MAP(in_ID),       //  in  Indonesian                0x21
        ILCID_POSIX_MAP(is_IS),       //  is  Icelandic                 0x0f
        ILCID_POSIX_MAP(it),          //  it  Italian                   0x10
        ILCID_POSIX_MAP(iw_IL),       //  iw  Hebrew (should be removed)0x0d
        ILCID_POSIX_MAP(ja_JP),       //  ja  Japanese                  0x11
        ILCID_POSIX_MAP(ka_GE),       //  ka  Georgian                  0x37
        ILCID_POSIX_MAP(kk_KZ),       //  kk  Kazakh                    0x3f
        ILCID_POSIX_MAP(kn_IN),       //  kn  Kannada                   0x4b
        ILCID_POSIX_MAP(ko),          //  ko  Korean                    0x12
        ILCID_POSIX_MAP(kok_IN),      //  kok Konkani                   0x57
        ILCID_POSIX_MAP(ks_IN),       //  ks  Kashmiri                  0x60
        ILCID_POSIX_MAP(lt_LT),       //  lt  Lithuanian                0x27
        ILCID_POSIX_MAP(lv_LV),       //  lv  Latvian, Lettish          0x26
        ILCID_POSIX_MAP(mk_MK),       //  mk  Macedonian                0x2f
        ILCID_POSIX_MAP(ml_IN),       //  ml  Malayalam                 0x4c
        ILCID_POSIX_MAP(mni_IN),      //  mni Manipuri                  0x58
        ILCID_POSIX_MAP(mr_IN),       //  mr  Marathi                   0x4e
        ILCID_POSIX_MAP(ms),          //  ms  Malay                     0x3e
        ILCID_POSIX_MAP(ne),          //  ne  Nepali                    0x61
        ILCID_POSIX_MAP(nl),          //  nl  Dutch                     0x13
        ILCID_POSIX_MAP(no),          //  no  Norwegian                 0x14
        ILCID_POSIX_MAP(or_IN),       //  or  Oriya                     0x48
        ILCID_POSIX_MAP(pa_IN),       //  pa  Punjabi                   0x46
        ILCID_POSIX_MAP(pl_PL),       //  pl  Polish                    0x15
        ILCID_POSIX_MAP(pt),          //  pt  Portuguese                0x16
        ILCID_POSIX_MAP(ro_RO),       //  ro  Romanian                  0x18
        ILCID_POSIX_MAP(root),        //  root                          0x00
        ILCID_POSIX_MAP(ru_RU),       //  ru  Russian                   0x19
        ILCID_POSIX_MAP(sa_IN),       //  sa  Sanskrit                  0x4f
        ILCID_POSIX_MAP(sd_IN),       //  sd  Sindhi                    0x59
        ILCID_POSIX_MAP(sh_YU),       //  sh  Serbo-Croatian            0x1a
        ILCID_POSIX_MAP(sk_SK),       //  sk  Slovak                    0x1b
        ILCID_POSIX_MAP(sl_SI),       //  sl  Slovenian                 0x24
        ILCID_POSIX_MAP(sq_AL),       //  sq  Albanian                  0x1c
        ILCID_POSIX_MAP(sr_YU),       //  sr  Serbian                   0x1a
        ILCID_POSIX_MAP(sv),          //  sv  Swedish                   0x1d
        ILCID_POSIX_MAP(sw),          //  sw  Swahili                   0x41
        ILCID_POSIX_MAP(ta_IN),       //  ta  Tamil                     0x49
        ILCID_POSIX_MAP(te_IN),       //  te  Telugu                    0x4a
        ILCID_POSIX_MAP(th_TH),       //  th  Thai                      0x1e
        ILCID_POSIX_MAP(tr_TR),       //  tr  Turkish                   0x1f
        ILCID_POSIX_MAP(tt_RU),       //  tt  Tatar                     0x44
        ILCID_POSIX_MAP(uk_UA),       //  uk  Ukrainian                 0x22
        ILCID_POSIX_MAP(ur),          //  ur  Urdu                      0x20
        ILCID_POSIX_MAP(uz_UZ),       //  uz  Uzbek                     0x43
        ILCID_POSIX_MAP(vi_VN),       //  vi  Vietnamese                0x2a
        ILCID_POSIX_MAP(zh),          //  zh  Chinese                   0x04
    };

    {
        Mutex m;
        if (!PosixIDmap)
        {
            // This assignment is okay because the local variable is static too.
            PosixIDmap  = localPosixIDmap;
            LocaleCount = sizeof(localPosixIDmap)/sizeof(ILcidPosixMap);
        }
    }
}

uint32_t       IGlobalLocales::LocaleCount = sizeof(PosixIDmap)/sizeof(ILcidPosixMap);
ILcidPosixMap *IGlobalLocales::PosixIDmap  = NULL;

//////////////////////////////////////
//
// LCID --> POSIX
//
/////////////////////////////////////

const char*
IGlobalLocales::convertToPosix(uint32_t hostid, UErrorCode *status)
{
    uint16_t langID = languageLCID(hostid);
    uint32_t index;

    initializeMapRegions();

    for (index = 0; index < LocaleCount; index++)
    {
        if (langID == PosixIDmap[index].hostLangID)
        {
            return PosixIDmap[index].posixID(hostid);
        }
    }

    //no match found
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return ILcidPosixMap::fgWildCard;
}

U_CFUNC const char *
T_convertToPosix(uint32_t hostid, UErrorCode* status)
{
    return IGlobalLocales::convertToPosix(hostid, status);
}


//////////////////////////////////////
//
// POSIX --> LCID
//
/////////////////////////////////////

uint32_t
IGlobalLocales::convertToLCID(const char* posixID, UErrorCode* status)
{
    uint32_t   low    = 0;
    uint32_t   mid;
    uint32_t   high   = LocaleCount - 1;
    int32_t    compVal;
    char       langID[256];

    // Check for incomplete id.
    if (!posixID || uprv_strlen(posixID) < 2)
        return 0;

    initializeMapRegions();
    uloc_getLanguage(posixID, langID, sizeof(langID), status);
    if (U_FAILURE(*status)) {
        return 0;
    }

    //Binary search for the map entry
    while (low <= high) {

        mid = (low + high) / 2;
        if (mid == 0)  // not found
            break;

        compVal = uprv_strcmp(langID, PosixIDmap[mid].posixLangID);

        if (compVal < 0)
            high = mid - 1;
        else if (compVal > 0)
            low = mid + 1;
        else  // found match!
            return PosixIDmap[mid].hostID(posixID);
    }

    // no match found
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
}

const char*  ILcidPosixMap::fgWildCard = "??_??";

/* Assumes Posix IDs are sorted alphabetically
 */
uint32_t
ILcidPosixMap::hostID(const char* posixID) const
{
    uint32_t low  = 1;
    uint32_t mid;
    uint32_t high = numRegions;
    int32_t  compVal;

    // Check for incomplete id. All LCIDs have a default country,
    // and a 0x0400 in 0xFC00 indicates a default country.
    // So Windows may not like hostLangID without a default
    // country.
    if (!numRegions || strlen(posixID) < 5)
        return hostLangID;

    // Binary search for the map entry
    // The element at index 0 is always the POSIX wild card,
    // so start search at index 1.
    while (low <= high) {

        mid = (low + high) / 2;

        compVal = uprv_strcmp(posixID, regionMaps[mid].posixID);

        if (compVal < 0)
            high = mid - 1;
        else if (compVal > 0)
            low = mid + 1;
        else  // found match!
            return regionMaps[mid].hostID;
    }

    //no match found
    return hostLangID;
}

const char*
ILcidPosixMap::posixID(uint32_t hostID) const
{
    uint32_t i;
    for (i = 0; i <= numRegions; i++)
    {
        if (regionMaps[i].hostID == hostID)
        {
            return regionMaps[i].posixID;
        }
    }

    // If you get here, then no matching region was found,
    // so return the language id with the wild card region.
    return regionMaps[0].posixID;
}

#endif
