/*
 **********************************************************************
 *   Copyright (C) 1996-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/
// $Revision: 1.22 $
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
//  3/11/97     aliu        Fixed off-by-one bug in assignment operator. Added
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
 * In order to test this code, please use the lcid test program.
 * The LCID values come from winnt.h
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
    uint32_t hostID;
    const char *posixID;
};

struct ILcidPosixMap
{
    const char* posixID(uint32_t fromHostID) const;

    /**
     * Searches for a Windows LCID
     *
     * @param posixid the Posix style locale id.
     * @param status gets set to U_ILLEGAL_ARGUMENT_ERROR when the Posix ID has
     *               no equivalent Windows LCID.
     * @return the LCID
     */
    uint32_t hostID(const char* fromPosixID) const;

    /**
     * Do not call this function. It is called by hostID.
     * The function is not private because this struct must stay as a C struct,
     * and this is an internal class.
     */
    uint32_t searchPosixIDmap(const char* posixID, UErrorCode* status) const;

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
static const ILcidPosixElement languageID[] = { \
    {LANGUAGE_LCID(hostID), #languageID},     /* parent locale */ \
    {hostID, #posixID}, \
};

/*
 Create the map for the posixID. This macro supposes that the language string
 name is the same as the global variable name, and that the first element
 in the ILcidPosixElement is just the language.
 */
#define ILCID_POSIX_MAP(_posixID) \
    {sizeof(_posixID)/sizeof(ILcidPosixElement), _posixID}

////////////////////////////////////////////
//
// Create the table of LCID to POSIX Mapping
// None of it should be dynamically created.
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
ILCID_POSIX_ELEMENT_ARRAY(0x0445, bn, bn_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x0403, ca, ca_ES)
ILCID_POSIX_ELEMENT_ARRAY(0x0405, cs, cs_CZ)
ILCID_POSIX_ELEMENT_ARRAY(0x0406, da, da_DK)

static const ILcidPosixElement de[] = {
    {0x07,   "de"},
    {0x0c07, "de_AT"},
    {0x0807, "de_CH"},
    {0x0407, "de_DE"},
    {0x1407, "de_LI"},    //Todo: Data does not exist
    {0x1007, "de_LU"},
    {0x10407,"de__PHONEBOOK"}  //This is really de_DE_PHONEBOOK on Windows, maybe 10007
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
    {0x3409, "en_PH"},
    {0x2C09, "en_TT"},    //Todo: Data does not exist
    {0x0409, "en_US"},
    {0x2409, "en_VI"},
    {0x1c09, "en_ZA"},
    {0x3009, "en_ZW"}
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
    {0x200a, "es_VE"},
    {0x040a, "es__TRADITIONAL"}  //This is really es_ES_PHONEBOOK on Windows
};

ILCID_POSIX_ELEMENT_ARRAY(0x0425, et, et_EE)
ILCID_POSIX_ELEMENT_ARRAY(0x042d, eu, eu_ES)
ILCID_POSIX_ELEMENT_ARRAY(0x0429, fa, fa_IR)
ILCID_POSIX_ELEMENT_ARRAY(0x040b, fi, fi_FI)
ILCID_POSIX_ELEMENT_ARRAY(0x0438, fo, fo_FO)

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
ILCID_POSIX_ELEMENT_ARRAY(0x0421, id, id_ID)
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

static const ILcidPosixElement ms[] = {
    {0x3e,   "ms"},
    {0x083e, "ms_BN"},   // Brunei Darussalam
    {0x043e, "ms_MY"}    // Malaysia
};

// The MSJDK documentation says this is maltese, but it's not supported.
ILCID_POSIX_ELEMENT_ARRAY(0x043a, mt, mt_MT)
ILCID_POSIX_ELEMENT_ARRAY(0x0414, nb, nb_NO)

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
    {0x14,   "no"},         // really nb
    {0x0414, "no_NO"},      // really nb_NO
    {0x0814, "nn_NO_NY"}    // really nn_NO
};

ILCID_POSIX_ELEMENT_ARRAY(0x0814, nn, nn_NO)
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

ILCID_POSIX_ELEMENT_ARRAY(0x0441, sw, sw_KE)// The MSJDK documentation says the default country is Kenya.
ILCID_POSIX_ELEMENT_ARRAY(0x0449, ta, ta_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x044a, te, te_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x041e, th, th_TH)
ILCID_POSIX_ELEMENT_ARRAY(0x041f, tr, tr_TR)
ILCID_POSIX_ELEMENT_ARRAY(0x0444, tt, tt_RU)    //Todo: Data does not exist
ILCID_POSIX_ELEMENT_ARRAY(0x0422, uk, uk_UA)

static const ILcidPosixElement ur[] = {         //Todo: Data does not exist
    {0x20,   "ur"},
    {0x0820, "ur_IN"},
    {0x0420, "ur_PK"}
};

static const ILcidPosixElement uz[] = {      //Todo: Data does not exist
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
    // This must be static
    static ILcidPosixMap localPosixIDmap[] = {
        ILCID_POSIX_MAP(af),          //  af  Afrikaans                 0x36
        ILCID_POSIX_MAP(ar),          //  ar  Arabic                    0x01
        ILCID_POSIX_MAP(as),          //  as  Assamese                  0x4d
        ILCID_POSIX_MAP(az),          //  az  Azerbaijani               0x2c
        ILCID_POSIX_MAP(be),          //  be  Byelorussian              0x23
        ILCID_POSIX_MAP(bg),          //  bg  Bulgarian                 0x02
        ILCID_POSIX_MAP(bn),          //  bn  Bengali; Bangla           0x45
        ILCID_POSIX_MAP(ca),          //  ca  Catalan                   0x03
        ILCID_POSIX_MAP(cs),          //  cs  Czech                     0x05
        ILCID_POSIX_MAP(da),          //  da  Danish                    0x06
        ILCID_POSIX_MAP(de),          //  de  German                    0x07
        ILCID_POSIX_MAP(el),          //  el  Greek                     0x08
        ILCID_POSIX_MAP(en),          //  en  English                   0x09
        ILCID_POSIX_MAP(es),          //  es  Spanish                   0x0a
        ILCID_POSIX_MAP(et),          //  et  Estonian                  0x25
        ILCID_POSIX_MAP(eu),          //  eu  Basque                    0x2d
        ILCID_POSIX_MAP(fa),          //  fa  Farsi                     0x29
        ILCID_POSIX_MAP(fi),          //  fi  Finnish                   0x0b
        ILCID_POSIX_MAP(fo),          //  fo  Faroese                   0x38
        ILCID_POSIX_MAP(fr),          //  fr  French                    0x0c
        ILCID_POSIX_MAP(gu),          //  gu  Gujarati                  0x47
        ILCID_POSIX_MAP(he),          //  he  Hebrew (formerly iw)      0x0d
        ILCID_POSIX_MAP(hi),          //  hi  Hindi                     0x39
        ILCID_POSIX_MAP(hr),          //  hr  Croatian                  0x1a
        ILCID_POSIX_MAP(hu),          //  hu  Hungarian                 0x0e
        ILCID_POSIX_MAP(hy),          //  hy  Armenian                  0x2b
        ILCID_POSIX_MAP(id),          //  id  Indonesian (formerly in)  0x21
//        ILCID_POSIX_MAP(in),          //  in  Indonesian                0x21
        ILCID_POSIX_MAP(is),          //  is  Icelandic                 0x0f
        ILCID_POSIX_MAP(it),          //  it  Italian                   0x10
        ILCID_POSIX_MAP(iw),          //  iw  Hebrew (should be removed)0x0d
        ILCID_POSIX_MAP(ja),          //  ja  Japanese                  0x11
        ILCID_POSIX_MAP(ka),          //  ka  Georgian                  0x37
        ILCID_POSIX_MAP(kk),          //  kk  Kazakh                    0x3f
        ILCID_POSIX_MAP(kn),          //  kn  Kannada                   0x4b
        ILCID_POSIX_MAP(ko),          //  ko  Korean                    0x12
        ILCID_POSIX_MAP(kok),         //  kok Konkani                   0x57
        ILCID_POSIX_MAP(ks),          //  ks  Kashmiri                  0x60
        ILCID_POSIX_MAP(lt),          //  lt  Lithuanian                0x27
        ILCID_POSIX_MAP(lv),          //  lv  Latvian, Lettish          0x26
        ILCID_POSIX_MAP(mk),          //  mk  Macedonian                0x2f
        ILCID_POSIX_MAP(ml),          //  ml  Malayalam                 0x4c
        ILCID_POSIX_MAP(mni),         //  mni Manipuri                  0x58
        ILCID_POSIX_MAP(mr),          //  mr  Marathi                   0x4e
        ILCID_POSIX_MAP(ms),          //  ms  Malay                     0x3e
        ILCID_POSIX_MAP(mt),          //  mt  Maltese                   0x3a
        ILCID_POSIX_MAP(nb),          //  no  Norwegian                 0x14
        ILCID_POSIX_MAP(ne),          //  ne  Nepali                    0x61
        ILCID_POSIX_MAP(nl),          //  nl  Dutch                     0x13
        ILCID_POSIX_MAP(nn),          //  no  Norwegian                 0x14
        ILCID_POSIX_MAP(no),          //  no  Norwegian                 0x14
        ILCID_POSIX_MAP(or),          //  or  Oriya                     0x48
        ILCID_POSIX_MAP(pa),          //  pa  Punjabi                   0x46
        ILCID_POSIX_MAP(pl),          //  pl  Polish                    0x15
        ILCID_POSIX_MAP(pt),          //  pt  Portuguese                0x16
        ILCID_POSIX_MAP(ro),          //  ro  Romanian                  0x18
        ILCID_POSIX_MAP(root),        //  root                          0x00
        ILCID_POSIX_MAP(ru),          //  ru  Russian                   0x19
        ILCID_POSIX_MAP(sa),          //  sa  Sanskrit                  0x4f
        ILCID_POSIX_MAP(sd),          //  sd  Sindhi                    0x59
        ILCID_POSIX_MAP(sh),          //  sh  Serbo-Croatian            0x1a
        ILCID_POSIX_MAP(sk),          //  sk  Slovak                    0x1b
        ILCID_POSIX_MAP(sl),          //  sl  Slovenian                 0x24
        ILCID_POSIX_MAP(sq),          //  sq  Albanian                  0x1c
        ILCID_POSIX_MAP(sr),          //  sr  Serbian                   0x1a
        ILCID_POSIX_MAP(sv),          //  sv  Swedish                   0x1d
        ILCID_POSIX_MAP(sw),          //  sw  Swahili                   0x41
        ILCID_POSIX_MAP(ta),          //  ta  Tamil                     0x49
        ILCID_POSIX_MAP(te),          //  te  Telugu                    0x4a
        ILCID_POSIX_MAP(th),          //  th  Thai                      0x1e
        ILCID_POSIX_MAP(tr),          //  tr  Turkish                   0x1f
        ILCID_POSIX_MAP(tt),          //  tt  Tatar                     0x44
        ILCID_POSIX_MAP(uk),          //  uk  Ukrainian                 0x22
        ILCID_POSIX_MAP(ur),          //  ur  Urdu                      0x20
        ILCID_POSIX_MAP(uz),          //  uz  Uzbek                     0x43
        ILCID_POSIX_MAP(vi),          //  vi  Vietnamese                0x2a
        ILCID_POSIX_MAP(zh),          //  zh  Chinese                   0x04
    };

    {
        Mutex m;
        if (!PosixIDmap)
        {
            // This assignment is okay because the local variable is static too.
            PosixIDmap  = localPosixIDmap;
            LocaleCount = sizeof(localPosixIDmap)/sizeof(ILcidPosixMap);
            WildCard = "??_??";
        }
    }
}

uint32_t       IGlobalLocales::LocaleCount = sizeof(PosixIDmap)/sizeof(ILcidPosixMap);
ILcidPosixMap *IGlobalLocales::PosixIDmap  = NULL;
const char    *IGlobalLocales::WildCard = "??_??";

//////////////////////////////////////
//
// LCID --> POSIX
//
/////////////////////////////////////

const char*
IGlobalLocales::convertToPosix(uint32_t hostid, UErrorCode *status)
{
    uint16_t langID = LANGUAGE_LCID(hostid);
    uint32_t index;

    initializeMapRegions();

    for (index = 0; index < LocaleCount; index++)
    {
        if (langID == PosixIDmap[index].regionMaps->hostID)
        {
            return PosixIDmap[index].posixID(hostid);
        }
    }

    //no match found
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return WildCard;
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
    initializeMapRegions();

    uint32_t   low    = 0;
    uint32_t   mid;
    uint32_t   high   = LocaleCount - 1;
    int32_t    compVal;
    char       langID[ULOC_FULLNAME_CAPACITY];

    // Check for incomplete id.
    if (!posixID || uprv_strlen(posixID) < 2)
        return 0;

    uloc_getLanguage(posixID, langID, sizeof(langID), status);
    if (U_FAILURE(*status)) {
        return 0;
    }

    //Binary search for the map entry
    while (low <= high) {

        mid = (low + high) / 2;

        compVal = uprv_strcmp(langID, PosixIDmap[mid].regionMaps->posixID);

        if (compVal < 0)
            high = mid - 1;
        else if (compVal > 0)
            low = mid + 1;
        else  // found match!
            return PosixIDmap[mid].hostID(posixID);

        if (mid == 0)  // not found
            break;
    }

    // no match found
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;   // return international (root)
}


/* Assumes Posix IDs are sorted alphabetically
 */
uint32_t
ILcidPosixMap::hostID(const char* posixID) const
{
    UErrorCode status = U_ZERO_ERROR;
    char     hostID[ULOC_FULLNAME_CAPACITY];
    char    *hostPtr = hostID;
    uint32_t value;
    int32_t hostLen = (int32_t)(strlen(posixID));
    int32_t  size, hostSize;

    // Check for incomplete id. All LCIDs have a default country,
    // and a 0x0400 in 0xFC00 indicates a default country.
    // So Windows may not like hostLangID without a default
    // country.
    if (!numRegions || hostLen < 5)
        return regionMaps->hostID;
    if (hostLen >= ULOC_FULLNAME_CAPACITY)
        hostLen = ULOC_FULLNAME_CAPACITY - 1;

    // We do this because the posixID may have a '-' separator and
    // incorrect string case
    hostSize = uloc_getLanguage(posixID,
                                hostID,
                                ULOC_LANG_CAPACITY + 1,
                                &status);
    if (U_SUCCESS(status))
    {
        hostPtr += hostSize;
        hostPtr[-1] = '_';
        size = uloc_getCountry(posixID,
                               hostPtr,
                               ULOC_COUNTRY_CAPACITY + 1,
                               &status);
        hostSize += size - 1;
        if (U_SUCCESS(status) && hostSize < hostLen)
        {
            hostPtr += size;
            hostPtr[-1] = '_';
            uloc_getVariant(posixID, hostPtr, hostLen - size, &status);
        }
    }

    // Try to find it the first time.
    value = searchPosixIDmap(hostID, &status);
    if (U_SUCCESS(status)) {
        return value;
    }

    // Couldn't find it. Cut off the last part of the locale
    while (hostPtr > hostID && *hostPtr != '_')
    {
        hostPtr--;
    }
    if (*hostPtr == '_')
    {
        *hostPtr = 0;
    }

    // Try it again without the last part of the locale
    status = U_ZERO_ERROR;
    value = searchPosixIDmap(hostID, &status);
    if (U_SUCCESS(status)) {
        return value;
    }

    // No match found. Return the language
    return regionMaps->hostID;
}

/**
 * Searches for a Windows LCID
 *
 * @param posixid the Posix style locale id.
 * @param status gets set to U_ILLEGAL_ARGUMENT_ERROR when the Posix ID has
 *               no equivalent Windows LCID.
 * @return the LCID
 */
uint32_t
ILcidPosixMap::searchPosixIDmap(const char* posixID, UErrorCode* status) const
{
    uint32_t low  = 1;
    uint32_t mid;
    uint32_t high = numRegions - 1;
    int32_t  compVal;

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
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return regionMaps->hostID;
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
