/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1998      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
*
* File TIMEZONE.CPP
*
* Modification History:
*
*   Date        Name        Description
*   12/05/96    clhuang     Creation.
*   04/21/97    aliu        General clean-up and bug fixing.
*   05/08/97    aliu        Fixed Hashtable code per code review.
*   07/09/97    helena      Changed createInstance to createDefault.
*   07/29/97    aliu        Updated with all-new list of 96 UNIX-derived
*                           TimeZones.  Changed mechanism to load from static
*                           array rather than resource bundle.
*   07/07/1998  srl         Bugfixes from the Java side: UTC GMT CAT NST
*                           Added getDisplayName API
*                           going to add custom parsing.
*
*                           ISSUES:
*                               - should getDisplayName cache something?
*                               - should custom time zones be cached? [probably]
*  08/10/98     stephen     Brought getDisplayName() API in-line w/ conventions
*  08/19/98     stephen     Changed createTimeZone() to never return 0
*  09/02/98     stephen     Added getOffset(monthLen) and hasSameRules()
*  09/15/98     stephen     Added getStaticClassID()
*  02/22/99     stephen     Removed character literals for EBCDIC safety
*  05/04/99     stephen     Changed initDefault() for Mutex issues
*  07/12/99     helena      HPUX 11 CC Port.
*********************************************************************************/

#include "uhash.h"
#include "simpletz.h"
#include "smpdtfmt.h"
#include "calendar.h"
#include "mutex.h"

#ifdef _DEBUG
#include "unistrm.h"
#endif

// static initialization
char TimeZone::fgClassID = 0; // Value is irrelevant

TimeZone*  TimeZone::fgDefaultZone = NULL;
UHashtable* TimeZone::fgHashtable   = NULL;

/**
 * Delete the given object, assuming it is a TimeZone.  Used by Hashtable.
 */
void
TimeZone::deleteTimeZone(void* obj)
{
    delete (TimeZone*)obj;
}

// ResourceBundle file, locale, and resource name
//const char* TimeZone::kTimeZoneResource = "timezone";

// There MUST be a TimeZone with this ID within kSystemTimeZones
UnicodeString TimeZone::kLastResortID("Africa/Casablanca"); // GMT
//const int32_t TimeZone::kLastResortOffset = 0;

// Array of UnicodeString objects for available IDs
UnicodeString* TimeZone::fgAvailableIDs = 0;
int32_t TimeZone::fgAvailableIDsCount = 0; 

const int32_t TimeZone::millisPerHour = kMillisPerHour;

const UnicodeString     TimeZone::GMT_ID        = "GMT";
const int32_t             TimeZone::GMT_ID_LENGTH = 3;
const UnicodeString     TimeZone::CUSTOM_ID     = "Custom";

const TimeZone*            TimeZone::GMT = new SimpleTimeZone(0, GMT_ID);

UErrorCode                TimeZone::fgStatus = U_ZERO_ERROR;

/* Lazy evaluated.  HPUX CC compiler can't handle array initialization 
with complex objects */
/*const int32_t TimeZone::kSystemTimeZonesCount = 320;*/
/* {sfb} illegal to have non-const array dimension */
#define kSystemTimeZonesCount 320
bool_t TimeZone::kSystemInited = FALSE;
SimpleTimeZone* TimeZone::kSystemTimeZones[kSystemTimeZonesCount];

void
TimeZone::initSystemTimeZones(void)
{
  if (kSystemInited == FALSE) {
    kSystemInited = TRUE;
    // Migration from Java:
    //  - replace ONE_HOUR with kMillisPerHour
    //  - replace (int) with (int32_t)

    // The following data is current as of 1998.
    // Total Unix zones: 343
    // Total Java zones: 289
    // Not all Unix zones become Java zones due to duplication and overlap.
    //----------------------------------------------------------

    kSystemTimeZones[0] =
    new SimpleTimeZone(-11*kMillisPerHour, "Pacific/Niue" /*NUT*/);
    // Pacific/Niue Niue(NU)    -11:00  -   NUT
    //----------------------------------------------------------
    kSystemTimeZones[1] =
    new SimpleTimeZone(-11*kMillisPerHour, "Pacific/Apia" /*WST*/);
    // Pacific/Apia W Samoa(WS) -11:00  -   WST # W Samoa Time
    kSystemTimeZones[2] =
    new SimpleTimeZone(-11*kMillisPerHour, "MIT" /*alias for Pacific/Apia*/);
    //----------------------------------------------------------
    kSystemTimeZones[3] =
    new SimpleTimeZone(-11*kMillisPerHour, "Pacific/Pago_Pago" /*SST*/);
    // Pacific/Pago_Pago    American Samoa(US)  -11:00  -   SST # S=Samoa
    //----------------------------------------------------------
    kSystemTimeZones[4] =
    new SimpleTimeZone(-10*kMillisPerHour, "Pacific/Tahiti" /*TAHT*/);
    // Pacific/Tahiti   French Polynesia(PF)    -10:00  -   TAHT    # Tahiti Time
    //----------------------------------------------------------
    kSystemTimeZones[5] =
    new SimpleTimeZone(-10*kMillisPerHour, "Pacific/Fakaofo" /*TKT*/);
    // Pacific/Fakaofo  Tokelau Is(TK)  -10:00  -   TKT # Tokelau Time
    //----------------------------------------------------------
    kSystemTimeZones[6] =
    new SimpleTimeZone(-10*kMillisPerHour, "Pacific/Honolulu" /*HST*/);
    // Pacific/Honolulu Hawaii(US)  -10:00  -   HST
    kSystemTimeZones[7] =
    new SimpleTimeZone(-10*kMillisPerHour, "HST" /*alias for Pacific/Honolulu*/);
    //----------------------------------------------------------
    kSystemTimeZones[8] =
    new SimpleTimeZone(-10*kMillisPerHour, "America/Adak" /*HA%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour, fgStatus);
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Adak Alaska(US)  -10:00  US  HA%sT
    //----------------------------------------------------------
    kSystemTimeZones[9] =
    new SimpleTimeZone(-10*kMillisPerHour, "Pacific/Rarotonga" /*CK%sT*/,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::MARCH, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, (int32_t)(0.5*kMillisPerHour), fgStatus);
    // Rule Cook    1979    max -   Mar Sun>=1  0:00    0   -
    // Rule Cook    1979    max -   Oct lastSun 0:00    0:30    HS
    // Pacific/Rarotonga    Cook Is(CK) -10:00  Cook    CK%sT
    //----------------------------------------------------------
    kSystemTimeZones[10] =
    new SimpleTimeZone((int32_t)(-9.5*kMillisPerHour), "Pacific/Marquesas" /*MART*/);
    // Pacific/Marquesas    French Polynesia(PF)    -9:30   -   MART    # Marquesas Time
    //----------------------------------------------------------
    kSystemTimeZones[11] =
    new SimpleTimeZone(-9*kMillisPerHour, "Pacific/Gambier" /*GAMT*/);
    // Pacific/Gambier  French Polynesia(PF)    -9:00   -   GAMT    # Gambier Time
    //----------------------------------------------------------
    kSystemTimeZones[12] =
    new SimpleTimeZone(-9*kMillisPerHour, "America/Anchorage" /*AK%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Anchorage    Alaska(US)  -9:00   US  AK%sT
    kSystemTimeZones[13] =
    new SimpleTimeZone(-9*kMillisPerHour, "AST" /*alias for America/Anchorage*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[14] =
    new SimpleTimeZone((int32_t)(-8.5*kMillisPerHour), "Pacific/Pitcairn" /*PNT*/);
    // Pacific/Pitcairn Pitcairn(PN)    -8:30   -   PNT # Pitcairn Time
    //----------------------------------------------------------
    kSystemTimeZones[15] =
    new SimpleTimeZone(-8*kMillisPerHour, "America/Vancouver" /*P%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Vanc    1962    max -   Oct lastSun 2:00    0   S
    // Rule Vanc    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Vancouver    British Columbia(CA)    -8:00   Vanc    P%sT
    //----------------------------------------------------------
    kSystemTimeZones[16] =
    new SimpleTimeZone(-8*kMillisPerHour, "America/Tijuana" /*P%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
    // America/Tijuana  Mexico(MX)  -8:00   Mexico  P%sT
    //----------------------------------------------------------
    kSystemTimeZones[17] =
    new SimpleTimeZone(-8*kMillisPerHour, "America/Los_Angeles" /*P%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Los_Angeles  US Pacific time, represented by Los Angeles(US) -8:00   US  P%sT
    kSystemTimeZones[18] =
    new SimpleTimeZone(-8*kMillisPerHour, "PST" /*alias for America/Los_Angeles*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[19] =
    new SimpleTimeZone(-7*kMillisPerHour, "America/Dawson_Creek" /*MST*/);
    // America/Dawson_Creek British Columbia(CA)    -7:00   -   MST
    //----------------------------------------------------------
    kSystemTimeZones[20] =
    new SimpleTimeZone(-7*kMillisPerHour, "America/Phoenix" /*MST*/);
    // America/Phoenix  ?(US)   -7:00   -   MST     
    kSystemTimeZones[21] =
    new SimpleTimeZone(-7*kMillisPerHour, "America/Edmonton" /*M%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Edm 1972    max -   Oct lastSun 2:00    0   S
    // Rule Edm 1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Edmonton Alberta(CA) -7:00   Edm M%sT
    //----------------------------------------------------------
    kSystemTimeZones[22] =
    new SimpleTimeZone(-7*kMillisPerHour, "America/Mazatlan" /*M%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
    // America/Mazatlan Mexico(MX)  -7:00   Mexico  M%sT
    //----------------------------------------------------------
    kSystemTimeZones[23] =
    new SimpleTimeZone(-7*kMillisPerHour, "America/Denver" /*M%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Denver   US Mountain time, represented by Denver(US) -7:00   US  M%sT
    kSystemTimeZones[24] =
    new SimpleTimeZone(-7*kMillisPerHour, "MST" /*alias for America/Denver*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[25] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Belize" /*C%sT*/);
    // America/Belize   Belize(BZ)  -6:00   -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[26] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Regina" /*CST*/);
    // America/Regina   Saskatchewan(CA)    -6:00   -   CST
    //----------------------------------------------------------
    kSystemTimeZones[27] =
    new SimpleTimeZone(-6*kMillisPerHour, "Pacific/Galapagos" /*GALT*/);
    // Pacific/Galapagos    Ecuador(EC) -6:00   -   GALT    # Galapagos Time
    //----------------------------------------------------------
    kSystemTimeZones[28] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Guatemala" /*C%sT*/);
    // America/Guatemala    Guatemala(GT)   -6:00   -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[29] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Tegucigalpa" /*C%sT*/);
    // America/Tegucigalpa  Honduras(HN)    -6:00   -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[30] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/El_Salvador" /*C%sT*/);
    // America/El_Salvador  El Salvador(SV) -6:00   -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[31] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Costa_Rica" /*C%sT*/);
    // America/Costa_Rica   Costa Rica(CR)  -6:00   -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[32] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Winnipeg" /*C%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Winn    1966    max -   Oct lastSun 2:00    0   S
    // Rule Winn    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Winnipeg Manitoba(CA)    -6:00   Winn    C%sT
    //----------------------------------------------------------
    kSystemTimeZones[33] =
    new SimpleTimeZone(-6*kMillisPerHour, "Pacific/Easter" /*EAS%sT*/,
      Calendar::OCTOBER, 9, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::MARCH, 9, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Chile   1969    max -   Oct Sun>=9  0:00    1:00    S
    // Rule Chile   1970    max -   Mar Sun>=9  0:00    0   -
    // Pacific/Easter   Chile(CL)   -6:00   Chile   EAS%sT
    //----------------------------------------------------------
    kSystemTimeZones[34] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Mexico_City" /*C%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
    // America/Mexico_City  Mexico(MX)  -6:00   Mexico  C%sT
    //----------------------------------------------------------
    kSystemTimeZones[35] =
    new SimpleTimeZone(-6*kMillisPerHour, "America/Chicago" /*C%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Chicago  US Central time, represented by Chicago(US) -6:00   US  C%sT
    kSystemTimeZones[36] =
    new SimpleTimeZone(-6*kMillisPerHour, "CST" /*alias for America/Chicago*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[37] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Porto_Acre" /*AST*/);
    // America/Porto_Acre   Brazil(BR)  -5:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[38] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Bogota" /*CO%sT*/);
    // America/Bogota   Colombia(CO)    -5:00   -   CO%sT   # Colombia Time
    //----------------------------------------------------------
    kSystemTimeZones[39] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Guayaquil" /*ECT*/);
    // America/Guayaquil    Ecuador(EC) -5:00   -   ECT # Ecuador Time
    //----------------------------------------------------------
    kSystemTimeZones[40] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Jamaica" /*EST*/);
    // America/Jamaica  Jamaica(JM) -5:00   -   EST
    //----------------------------------------------------------
    kSystemTimeZones[41] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Cayman" /*EST*/);
    // America/Cayman   Cayman Is(KY)   -5:00   -   EST
    //----------------------------------------------------------
    kSystemTimeZones[42] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Managua" /*EST*/);
    // America/Managua  Nicaragua(NI)   -5:00   -   EST
    //----------------------------------------------------------
    kSystemTimeZones[43] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Panama" /*EST*/);
    // America/Panama   Panama(PA)  -5:00   -   EST
    //----------------------------------------------------------
    kSystemTimeZones[44] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Lima" /*PE%sT*/);
    // America/Lima Peru(PE)    -5:00   -   PE%sT   # Peru Time
    //----------------------------------------------------------
    kSystemTimeZones[45] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Indianapolis" /*EST*/);
    // America/Indianapolis Indiana(US) -5:00   -   EST
    kSystemTimeZones[46] =
    new SimpleTimeZone(-5*kMillisPerHour, "IET" /*alias for America/Indianapolis*/);
    //----------------------------------------------------------
    kSystemTimeZones[47] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Nassau" /*E%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Bahamas 1964    max -   Oct lastSun 2:00    0   S
    // Rule Bahamas 1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Nassau   Bahamas(BS) -5:00   Bahamas E%sT
    //----------------------------------------------------------
    kSystemTimeZones[48] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Montreal" /*E%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Mont    1957    max -   Oct lastSun 2:00    0   S
    // Rule Mont    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Montreal Ontario, Quebec(CA) -5:00   Mont    E%sT
    //----------------------------------------------------------
    kSystemTimeZones[49] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Havana" /*C%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, 8, -Calendar::SUNDAY /*DOW>=DOM*/, 1*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Cuba    1990    max -   Apr Sun>=1  0:00    1:00    D
    // Rule Cuba    1997    max -   Oct Sun>=8  0:00s   0   S
    // America/Havana   Cuba(CU)    -5:00   Cuba    C%sT
    //----------------------------------------------------------
    kSystemTimeZones[50] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Port-au-Prince" /*E%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 1*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Haiti   1988    max -   Apr Sun>=1  1:00s   1:00    D
    // Rule Haiti   1988    max -   Oct lastSun 1:00s   0   S
    // America/Port-au-Prince   Haiti(HT)   -5:00   Haiti   E%sT
    //----------------------------------------------------------
    kSystemTimeZones[51] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/Grand_Turk" /*E%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule TC  1979    max -   Oct lastSun 0:00    0   S
    // Rule TC  1987    max -   Apr Sun>=1  0:00    1:00    D
    // America/Grand_Turk   Turks and Caicos(TC)    -5:00   TC  E%sT
    //----------------------------------------------------------
    kSystemTimeZones[52] =
    new SimpleTimeZone(-5*kMillisPerHour, "America/New_York" /*E%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule US  1967    max -   Oct lastSun 2:00    0   S
    // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/New_York US Eastern time, represented by New York(US)    -5:00   US  E%sT
    kSystemTimeZones[53] =
    new SimpleTimeZone(-5*kMillisPerHour, "EST" /*alias for America/New_York*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[54] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Antigua" /*AST*/);
    // America/Antigua  Antigua and Barbuda(AG) -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[55] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Anguilla" /*AST*/);
    // America/Anguilla Anguilla(AI)    -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[56] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Curacao" /*AST*/);
    // America/Curacao  Curacao(AN) -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[57] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Aruba" /*AST*/);
    // America/Aruba    Aruba(AW)   -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[58] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Barbados" /*A%sT*/);
    // America/Barbados Barbados(BB)    -4:00   -   A%sT
    //----------------------------------------------------------
    kSystemTimeZones[59] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/La_Paz" /*BOT*/);
    // America/La_Paz   Bolivia(BO) -4:00   -   BOT # Bolivia Time
    //----------------------------------------------------------
    kSystemTimeZones[60] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Manaus" /*WST*/);
    // America/Manaus   Brazil(BR)  -4:00   -   WST
    //----------------------------------------------------------
    kSystemTimeZones[61] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Dominica" /*AST*/);
    // America/Dominica Dominica(DM)    -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[62] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Santo_Domingo" /*AST*/);
    // America/Santo_Domingo    Dominican Republic(DO)  -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[63] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Grenada" /*AST*/);
    // America/Grenada  Grenada(GD) -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[64] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Guadeloupe" /*AST*/);
    // America/Guadeloupe   Guadeloupe(GP)  -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[65] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Guyana" /*GYT*/);
    // America/Guyana   Guyana(GY)  -4:00   -   GYT
    //----------------------------------------------------------
    kSystemTimeZones[66] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/St_Kitts" /*AST*/);
    // America/St_Kitts St Kitts-Nevis(KN)  -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[67] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/St_Lucia" /*AST*/);
    // America/St_Lucia St Lucia(LC)    -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[68] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Martinique" /*AST*/);
    // America/Martinique   Martinique(MQ)  -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[69] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Montserrat" /*AST*/);
    // America/Montserrat   Montserrat(MS)  -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[70] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Puerto_Rico" /*AST*/);
    // America/Puerto_Rico  Puerto Rico(PR) -4:00   -   AST
    kSystemTimeZones[71] =
    new SimpleTimeZone(-4*kMillisPerHour, "PRT" /*alias for America/Puerto_Rico*/);
    //----------------------------------------------------------
    kSystemTimeZones[72] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Port_of_Spain" /*AST*/);
    // America/Port_of_Spain    Trinidad and Tobago(TT) -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[73] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/St_Vincent" /*AST*/);
    // America/St_Vincent   St Vincent and the Grenadines(VC)   -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[74] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Tortola" /*AST*/);
    // America/Tortola  British Virgin Is(VG)   -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[75] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/St_Thomas" /*AST*/);
    // America/St_Thomas    Virgin Is(VI)   -4:00   -   AST
    //----------------------------------------------------------
    kSystemTimeZones[76] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Caracas" /*VET*/);
    // America/Caracas  Venezuela(VE)   -4:00   -   VET
    //----------------------------------------------------------
    kSystemTimeZones[77] =
    new SimpleTimeZone(-4*kMillisPerHour, "Antarctica/Palmer" /*CL%sT*/,
      Calendar::OCTOBER, 9, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::MARCH, 9, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule ChileAQ 1969    max -   Oct Sun>=9  0:00    1:00    S
    // Rule ChileAQ 1970    max -   Mar Sun>=9  0:00    0   -
    // Antarctica/Palmer    USA - year-round bases(AQ)  -4:00   ChileAQ CL%sT
    //----------------------------------------------------------
    kSystemTimeZones[78] =
    new SimpleTimeZone(-4*kMillisPerHour, "Atlantic/Bermuda" /*A%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Bahamas 1964    max -   Oct lastSun 2:00    0   S
    // Rule Bahamas 1987    max -   Apr Sun>=1  2:00    1:00    D
    // Atlantic/Bermuda Bermuda(BM) -4:00   Bahamas A%sT
    //----------------------------------------------------------
    kSystemTimeZones[79] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Cuiaba" /*W%sT*/,
      Calendar::OCTOBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::FEBRUARY, 11, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Brazil  1998    max -   Oct Sun>=1  0:00    1:00    D
    // Rule Brazil  1999    max -   Feb Sun>=11 0:00    0   S
    // America/Cuiaba   Brazil(BR)  -4:00   Brazil  W%sT
    //----------------------------------------------------------
    kSystemTimeZones[80] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Halifax" /*A%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Halifax 1962    max -   Oct lastSun 2:00    0   S
    // Rule Halifax 1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Halifax  ?(CA)   -4:00   Halifax A%sT
    //----------------------------------------------------------
    kSystemTimeZones[81] =
    new SimpleTimeZone(-4*kMillisPerHour, "Atlantic/Stanley" /*FK%sT*/,
      Calendar::SEPTEMBER, 8, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::APRIL, 16, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Falk    1986    max -   Apr Sun>=16 0:00    0   -
    // Rule Falk    1996    max -   Sep Sun>=8  0:00    1:00    S
    // Atlantic/Stanley Falklands(FK)   -4:00   Falk    FK%sT
    //----------------------------------------------------------
    kSystemTimeZones[82] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Thule" /*A%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Thule   1993    max -   Apr Sun>=1  2:00    1:00    D
    // Rule Thule   1993    max -   Oct lastSun 2:00    0   S
    // America/Thule    ?(GL)   -4:00   Thule   A%sT
    //----------------------------------------------------------
    kSystemTimeZones[83] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Asuncion" /*PY%sT*/,
      Calendar::OCTOBER, 1, 0 /*DOM*/, 0*kMillisPerHour,
      Calendar::MARCH, 1, 0 /*DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Para    1996    max -   Mar 1   0:00    0   -
    // Rule Para    1997    max -   Oct 1   0:00    1:00    S
    // America/Asuncion Paraguay(PY)    -4:00   Para    PY%sT
    //----------------------------------------------------------
    kSystemTimeZones[84] =
    new SimpleTimeZone(-4*kMillisPerHour, "America/Santiago" /*CL%sT*/,
      Calendar::OCTOBER, 9, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::MARCH, 9, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Chile   1969    max -   Oct Sun>=9  0:00    1:00    S
    // Rule Chile   1970    max -   Mar Sun>=9  0:00    0   -
    // America/Santiago Chile(CL)   -4:00   Chile   CL%sT
    //----------------------------------------------------------
    kSystemTimeZones[85] =
    new SimpleTimeZone((int32_t)(-3.5*kMillisPerHour), "America/St_Johns" /*N%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule StJohns 1960    max -   Oct lastSun 2:00    0   S
    // Rule StJohns 1989    max -   Apr Sun>=1  2:00    1:00    D
    // America/St_Johns Canada(CA)  -3:30   StJohns N%sT
    kSystemTimeZones[86] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Fortaleza" /*EST*/);
    // America/Fortaleza    Brazil(BR)  -3:00   -   EST
    //----------------------------------------------------------
    kSystemTimeZones[87] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Cayenne" /*GFT*/);
    // America/Cayenne  French Guiana(GF)   -3:00   -   GFT
    //----------------------------------------------------------
    kSystemTimeZones[88] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Paramaribo" /*SRT*/);
    // America/Paramaribo   Suriname(SR)    -3:00   -   SRT
    //----------------------------------------------------------
    kSystemTimeZones[89] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Montevideo" /*UY%sT*/);
    // America/Montevideo   Uruguay(UY) -3:00   -   UY%sT
    //----------------------------------------------------------
    kSystemTimeZones[90] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Buenos_Aires" /*AR%sT*/);
    // America/Buenos_Aires Argentina(AR)   -3:00   -   AR%sT
    kSystemTimeZones[91] =
    new SimpleTimeZone(-3*kMillisPerHour, "AGT" /*alias for America/Buenos_Aires*/);
    //----------------------------------------------------------
    kSystemTimeZones[92] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Godthab" /*WG%sT*/,
      Calendar::MARCH, -1, Calendar::SATURDAY /*DOW_IN_DOM*/, 22*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SATURDAY /*DOW_IN_DOM*/, 22*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // America/Godthab  ?(GL)   -3:00   EU  WG%sT
    //----------------------------------------------------------
    kSystemTimeZones[93] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Miquelon" /*PM%sT*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Mont    1957    max -   Oct lastSun 2:00    0   S
    // Rule Mont    1987    max -   Apr Sun>=1  2:00    1:00    D
    // America/Miquelon St Pierre and Miquelon(PM)  -3:00   Mont    PM%sT   # Pierre & Miquelon Time
    //----------------------------------------------------------
    kSystemTimeZones[94] =
    new SimpleTimeZone(-3*kMillisPerHour, "America/Sao_Paulo" /*E%sT*/,
      Calendar::OCTOBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::FEBRUARY, 11, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Brazil  1998    max -   Oct Sun>=1  0:00    1:00    D
    // Rule Brazil  1999    max -   Feb Sun>=11 0:00    0   S
    // America/Sao_Paulo    Brazil(BR)  -3:00   Brazil  E%sT
    kSystemTimeZones[95] =
    new SimpleTimeZone(-3*kMillisPerHour, "BET" /*alias for America/Sao_Paulo*/,
      Calendar::OCTOBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::FEBRUARY, 11, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[96] =
    new SimpleTimeZone(-2*kMillisPerHour, "America/Noronha" /*FST*/);
    // America/Noronha  Brazil(BR)  -2:00   -   FST
    //----------------------------------------------------------
    kSystemTimeZones[97] =
    new SimpleTimeZone(-2*kMillisPerHour, "Atlantic/South_Georgia" /*GST*/);
    // Atlantic/South_Georgia   South Georgia(GS)   -2:00   -   GST # South Georgia Time
    //----------------------------------------------------------
    kSystemTimeZones[98] =
    new SimpleTimeZone(-1*kMillisPerHour, "Atlantic/Jan_Mayen" /*EGT*/);
    // Atlantic/Jan_Mayen   ?(NO)   -1:00   -   EGT
    //----------------------------------------------------------
    kSystemTimeZones[99] =
    new SimpleTimeZone(-1*kMillisPerHour, "Atlantic/Cape_Verde" /*CVT*/);
    // Atlantic/Cape_Verde  Cape Verde(CV)  -1:00   -   CVT
    //----------------------------------------------------------
    kSystemTimeZones[100] =
    new SimpleTimeZone(-1*kMillisPerHour, "America/Scoresbysund" /*EG%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // America/Scoresbysund ?(GL)   -1:00   EU  EG%sT
    //----------------------------------------------------------
    kSystemTimeZones[101] =
    new SimpleTimeZone(-1*kMillisPerHour, "Atlantic/Azores" /*AZO%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Atlantic/Azores  Portugal(PT)    -1:00   EU  AZO%sT
    //----------------------------------------------------------
    kSystemTimeZones[102] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Ouagadougou" /*GMT*/);
    // Africa/Ouagadougou   Burkina Faso(BF)    0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[103] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Abidjan" /*GMT*/);
    // Africa/Abidjan   Cote D'Ivoire(CI)   0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[104] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Accra" /*%s*/);
    // Africa/Accra Ghana(GH)   0:00    -   %s
    //----------------------------------------------------------
    kSystemTimeZones[105] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Banjul" /*GMT*/);
    // Africa/Banjul    Gambia(GM)  0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[106] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Conakry" /*GMT*/);
    // Africa/Conakry   Guinea(GN)  0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[107] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Bissau" /*GMT*/);
    // Africa/Bissau    Guinea-Bissau(GW)   0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[108] =
    new SimpleTimeZone(0*kMillisPerHour, "Atlantic/Reykjavik" /*GMT*/);
    // Atlantic/Reykjavik   Iceland(IS) 0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[109] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Monrovia" /*GMT*/);
    // Africa/Monrovia  Liberia(LR) 0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[110] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Casablanca" /*WET*/);
    // Africa/Casablanca    Morocco(MA) 0:00    -   WET
    //----------------------------------------------------------
    kSystemTimeZones[111] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Timbuktu" /*GMT*/);
    // Africa/Timbuktu  Mali(ML)    0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[112] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Nouakchott" /*GMT*/);
    // Africa/Nouakchott    Mauritania(MR)  0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[113] =
    new SimpleTimeZone(0*kMillisPerHour, "Atlantic/St_Helena" /*GMT*/);
    // Atlantic/St_Helena   St Helena(SH)   0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[114] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Freetown" /*%s*/);
    // Africa/Freetown  Sierra Leone(SL)    0:00    -   %s
    //----------------------------------------------------------
    kSystemTimeZones[115] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Dakar" /*GMT*/);
    // Africa/Dakar Senegal(SN) 0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[116] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Sao_Tome" /*GMT*/);
    // Africa/Sao_Tome  Sao Tome and Principe(ST)   0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[117] =
    new SimpleTimeZone(0*kMillisPerHour, "Africa/Lome" /*GMT*/);
    // Africa/Lome  Togo(TG)    0:00    -   GMT
    //----------------------------------------------------------
    kSystemTimeZones[118] =
    new SimpleTimeZone(0*kMillisPerHour, "GMT" /*GMT*/);
    // GMT  -(-)    0:00    -   GMT
    kSystemTimeZones[119] =
    new SimpleTimeZone(0*kMillisPerHour, "UTC" /*alias for GMT*/);
    //----------------------------------------------------------
    kSystemTimeZones[120] =
    new SimpleTimeZone(0*kMillisPerHour, "Atlantic/Faeroe" /*WE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Atlantic/Faeroe  Denmark, Faeroe Islands, and Greenland(DK)  0:00    EU  WE%sT
    //----------------------------------------------------------
    kSystemTimeZones[121] =
    new SimpleTimeZone(0*kMillisPerHour, "Atlantic/Canary" /*WE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Atlantic/Canary  Spain(ES)   0:00    EU  WE%sT
    //----------------------------------------------------------
    kSystemTimeZones[122] =
    new SimpleTimeZone(0*kMillisPerHour, "Europe/Dublin" /*GMT/IST*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Dublin    ---(IE) 0:00    EU  GMT/IST
    //----------------------------------------------------------
    kSystemTimeZones[123] =
    new SimpleTimeZone(0*kMillisPerHour, "Europe/Lisbon" /*WE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Lisbon    Portugal(PT)    0:00    EU  WE%sT
    //----------------------------------------------------------
    kSystemTimeZones[124] =
    new SimpleTimeZone(0*kMillisPerHour, "Europe/London" /*GMT/BST*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/London    ---(GB) 0:00    EU  GMT/BST
    //----------------------------------------------------------
    kSystemTimeZones[125] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Luanda" /*WAT*/);
    // Africa/Luanda    Angola(AO)  1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[126] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Porto-Novo" /*WAT*/);
    // Africa/Porto-Novo    Benin(BJ)   1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[127] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Bangui" /*WAT*/);
    // Africa/Bangui    Central African Republic(CF)    1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[128] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Kinshasa" /*WAT*/);
    // Africa/Kinshasa  Democratic Republic of Congo(CG)    1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[129] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Douala" /*WAT*/);
    // Africa/Douala    Cameroon(CM)    1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[130] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Libreville" /*WAT*/);
    // Africa/Libreville    Gabon(GA)   1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[131] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Malabo" /*WAT*/);
    // Africa/Malabo    Equatorial Guinea(GQ)   1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[132] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Niamey" /*WAT*/);
    // Africa/Niamey    Niger(NE)   1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[133] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Lagos" /*WAT*/);
    // Africa/Lagos Nigeria(NG) 1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[134] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Ndjamena" /*WAT*/);
    // Africa/Ndjamena  Chad(TD)    1:00    -   WAT
    //----------------------------------------------------------
    kSystemTimeZones[135] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Tunis" /*CE%sT*/);
    // Africa/Tunis Tunisia(TN) 1:00    -   CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[136] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Algiers" /*CET*/);
    // Africa/Algiers   Algeria(DZ) 1:00    -   CET
    //----------------------------------------------------------
    kSystemTimeZones[137] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Andorra" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Andorra   Andorra(AD) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[138] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Tirane" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Tirane    Albania(AL) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[139] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Vienna" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Vienna    Austria(AT) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[140] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Brussels" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Brussels  Belgium(BE) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[141] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Zurich" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Zurich    Switzerland(CH) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[142] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Prague" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Prague    Czech Republic(CZ)  1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[143] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Berlin" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Berlin    Germany(DE) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[144] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Copenhagen" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Copenhagen    Denmark, Faeroe Islands, and Greenland(DK)  1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[145] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Madrid" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Madrid    Spain(ES)   1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[146] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Gibraltar" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Gibraltar Gibraltar(GI)   1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[147] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Budapest" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Budapest  Hungary(HU) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[148] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Rome" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Rome  Italy(IT)   1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[149] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Vaduz" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Vaduz Liechtenstein(LI)   1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[150] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Luxembourg" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Luxembourg    Luxembourg(LU)  1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[151] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Tripoli" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::THURSDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, 1, -Calendar::THURSDAY /*DOW>=DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Libya   1997    max -   Mar lastThu 2:00s   1:00    S
    // Rule Libya   1997    max -   Oct Thu>=1  2:00s   0   -
    // Africa/Tripoli   Libya(LY)   1:00    Libya   CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[152] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Monaco" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Monaco    Monaco(MC)  1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[153] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Malta" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Malta Malta(MT)   1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[154] =
    new SimpleTimeZone(1*kMillisPerHour, "Africa/Windhoek" /*WA%sT*/,
      Calendar::SEPTEMBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Namibia 1994    max -   Sep Sun>=1  2:00    1:00    S
    // Rule Namibia 1995    max -   Apr Sun>=1  2:00    0   -
    // Africa/Windhoek  Namibia(NA) 1:00    Namibia WA%sT
    //----------------------------------------------------------
    kSystemTimeZones[155] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Amsterdam" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Amsterdam Netherlands(NL) 1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[156] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Oslo" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Oslo  Norway(NO)  1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[157] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Warsaw" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule W-Eur   1981    max -   Mar lastSun 1:00s   1:00    S
    // Rule W-Eur   1996    max -   Oct lastSun 1:00s   0   -
    // Europe/Warsaw    Poland(PL)  1:00    W-Eur   CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[158] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Stockholm" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Stockholm Sweden(SE)  1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[159] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Belgrade" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Belgrade  Yugoslavia(YU)  1:00    EU  CE%sT
    //----------------------------------------------------------
    kSystemTimeZones[160] =
    new SimpleTimeZone(1*kMillisPerHour, "Europe/Paris" /*CE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Paris France(FR)  1:00    EU  CE%sT
    kSystemTimeZones[161] =
    new SimpleTimeZone(1*kMillisPerHour, "ECT" /*alias for Europe/Paris*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[162] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Bujumbura" /*CAT*/);
    // Africa/Bujumbura Burundi(BI) 2:00    -   CAT
    //----------------------------------------------------------
    kSystemTimeZones[163] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Gaborone" /*CAT*/);
    // Africa/Gaborone  Botswana(BW)    2:00    -   CAT
    //----------------------------------------------------------
    kSystemTimeZones[164] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Lubumbashi" /*CAT*/);
    // Africa/Lubumbashi    Democratic Republic of Congo(CG)    2:00    -   CAT
    //----------------------------------------------------------
    kSystemTimeZones[165] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Maseru" /*SAST*/);
    // Africa/Maseru    Lesotho(LS) 2:00    -   SAST
    //----------------------------------------------------------
    kSystemTimeZones[166] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Blantyre" /*CAT*/);
    // Africa/Blantyre  Malawi(ML)  2:00    -   CAT
    //----------------------------------------------------------
    kSystemTimeZones[167] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Maputo" /*CAT*/);
    // Africa/Maputo    Mozambique(MZ)  2:00    -   CAT
    //----------------------------------------------------------
    kSystemTimeZones[168] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Kigali" /*CAT*/);
    // Africa/Kigali    Rwanda(RW)  2:00    -   CAT
    //----------------------------------------------------------
    kSystemTimeZones[169] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Khartoum" /*CA%sT*/);
    // Africa/Khartoum  Sudan(SD)   2:00    -   CA%sT
    //----------------------------------------------------------
    kSystemTimeZones[170] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Mbabane" /*SAST*/);
    // Africa/Mbabane   Swaziland(SZ)   2:00    -   SAST
    //----------------------------------------------------------
    kSystemTimeZones[171] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Lusaka" /*CAT*/);
    // Africa/Lusaka    Zambia(ZM)  2:00    -   CAT
    //----------------------------------------------------------
    kSystemTimeZones[172] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Harare" /*CAT*/);
    // Africa/Harare    Zimbabwe(ZW)    2:00    -   CAT
    kSystemTimeZones[173] =
    new SimpleTimeZone(2*kMillisPerHour, "CAT" /*alias for Africa/Harare*/);
    //----------------------------------------------------------
    kSystemTimeZones[174] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Johannesburg" /*SAST*/);
    // Africa/Johannesburg  South Africa(ZA)    2:00    -   SAST
    //----------------------------------------------------------
    kSystemTimeZones[175] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Sofia" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
    // Europe/Sofia Bulgaria(BG)    2:00    E-Eur   EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[176] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Minsk" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Minsk Belarus(BY) 2:00    Russia  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[177] =
    new SimpleTimeZone(2*kMillisPerHour, "Asia/Nicosia" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Cyprus  1979    max -   Sep lastSun 0:00    0   -
    // Rule Cyprus  1981    max -   Mar lastSun 0:00    1:00    S
    // Asia/Nicosia Cyprus(CY)  2:00    Cyprus  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[178] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Tallinn" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule C-Eur   1981    max -   Mar lastSun 2:00s   1:00    S
    // Rule C-Eur   1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Tallinn   Estonia(EE) 2:00    C-Eur   EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[179] =
    new SimpleTimeZone(2*kMillisPerHour, "Africa/Cairo" /*EE%sT*/,
      Calendar::APRIL, -1, Calendar::FRIDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::SEPTEMBER, -1, Calendar::FRIDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Egypt   1995    max -   Apr lastFri 1:00    1:00    S
    // Rule Egypt   1995    max -   Sep lastFri 3:00    0   -
    // Africa/Cairo Egypt(EG)   2:00    Egypt   EE%sT
    kSystemTimeZones[180] =
    new SimpleTimeZone(2*kMillisPerHour, "ART" /*alias for Africa/Cairo*/,
      Calendar::APRIL, -1, Calendar::FRIDAY /*DOW_IN_DOM*/, 1*kMillisPerHour,
      Calendar::SEPTEMBER, -1, Calendar::FRIDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[181] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Helsinki" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Helsinki  Finland(FI) 2:00    EU  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[182] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Athens" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Athens    Greece(GR)  2:00    EU  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[183] =
    new SimpleTimeZone(2*kMillisPerHour, "Asia/Jerusalem" /*I%sT*/,
      Calendar::MARCH, 15, -Calendar::FRIDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Zion    1999    max -   Mar Fri>=15 0:00    1:00    D
    // Rule Zion    1999    max -   Sep Sun>=1  0:00    0   S
    // Asia/Jerusalem   Israel(IL)  2:00    Zion    I%sT
    //----------------------------------------------------------
    kSystemTimeZones[184] =
    new SimpleTimeZone(2*kMillisPerHour, "Asia/Amman" /*EE%sT*/,
      Calendar::APRIL, 1, -Calendar::FRIDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, 15, -Calendar::FRIDAY /*DOW>=DOM*/, 1*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule    Jordan   1993    max -   Apr Fri>=1  0:00    1:00    S
    // Rule    Jordan   1995    max -   Sep Fri>=15 0:00s   0   -
    // Asia/Amman   Jordan(JO)  2:00    Jordan  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[185] =
    new SimpleTimeZone(2*kMillisPerHour, "Asia/Beirut" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Lebanon 1993    max -   Mar lastSun 0:00    1:00    S
    // Rule Lebanon 1993    max -   Sep lastSun 0:00    0   -
    // Asia/Beirut  Lebanon(LB) 2:00    Lebanon EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[186] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Vilnius" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule C-Eur   1981    max -   Mar lastSun 2:00s   1:00    S
    // Rule C-Eur   1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Vilnius   Lithuania(LT)   2:00    C-Eur   EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[187] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Riga" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::SEPTEMBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Latvia  1992    max -   Mar lastSun 2:00s   1:00    S
    // Rule Latvia  1992    max -   Sep lastSun 2:00s   0   -
    // Europe/Riga  Latvia(LV)  2:00    Latvia  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[188] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Chisinau" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
    // Europe/Chisinau  Moldova(MD) 2:00    E-Eur   EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[189] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Bucharest" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
    // Europe/Bucharest Romania(RO) 2:00    E-Eur   EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[190] =
    new SimpleTimeZone(2*kMillisPerHour, "Asia/Damascus" /*EE%sT*/,
      Calendar::APRIL, 1, 0 /*DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, 1, 0 /*DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Syria   1994    max -   Apr 1   0:00    1:00    S
    // Rule Syria   1994    max -   Oct 1   0:00    0   -
    // Asia/Damascus    Syria(SY)   2:00    Syria   EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[191] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Kiev" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Kiev  Ukraine(UA) 2:00    EU  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[192] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Istanbul" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EU  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EU  1996    max -   Oct lastSun 1:00u   0   -
    // Europe/Istanbul  Turkey(TR)  2:00    EU  EE%sT
    kSystemTimeZones[193] =
    new SimpleTimeZone(2*kMillisPerHour, "EET" /*alias for Europe/Istanbul*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[194] =
    new SimpleTimeZone(3*kMillisPerHour, "Asia/Bahrain" /*AST*/);
    // Asia/Bahrain Bahrain(BH) 3:00    -   AST
    //----------------------------------------------------------
    kSystemTimeZones[195] =
    new SimpleTimeZone(3*kMillisPerHour, "Africa/Djibouti" /*EAT*/);
    // Africa/Djibouti  Djibouti(DJ)    3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[196] =
    new SimpleTimeZone(3*kMillisPerHour, "Africa/Asmera" /*EAT*/);
    // Africa/Asmera    Eritrea(ER) 3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[197] =
    new SimpleTimeZone(3*kMillisPerHour, "Africa/Addis_Ababa" /*EAT*/);
    // Africa/Addis_Ababa   Ethiopia(ET)    3:00    -   EAT
    kSystemTimeZones[198] =
    new SimpleTimeZone(3*kMillisPerHour, "EAT" /*alias for Africa/Addis_Ababa*/);
    //----------------------------------------------------------
    kSystemTimeZones[199] =
    new SimpleTimeZone(3*kMillisPerHour, "Africa/Nairobi" /*EAT*/);
    // Africa/Nairobi   Kenya(KE)   3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[200] =
    new SimpleTimeZone(3*kMillisPerHour, "Indian/Comoro" /*EAT*/);
    // Indian/Comoro    Comoros(KM) 3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[201] =
    new SimpleTimeZone(3*kMillisPerHour, "Asia/Kuwait" /*AST*/);
    // Asia/Kuwait  Kuwait(KW)  3:00    -   AST
    //----------------------------------------------------------
    kSystemTimeZones[202] =
    new SimpleTimeZone(3*kMillisPerHour, "Indian/Antananarivo" /*EAT*/);
    // Indian/Antananarivo  Madagascar(MK)  3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[203] =
    new SimpleTimeZone(3*kMillisPerHour, "Asia/Qatar" /*AST*/);
    // Asia/Qatar   Qatar(QA)   3:00    -   AST
    //----------------------------------------------------------
    kSystemTimeZones[204] =
    new SimpleTimeZone(3*kMillisPerHour, "Africa/Mogadishu" /*EAT*/);
    // Africa/Mogadishu Somalia(SO) 3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[205] =
    new SimpleTimeZone(3*kMillisPerHour, "Africa/Dar_es_Salaam" /*EAT*/);
    // Africa/Dar_es_Salaam Tanzania(TZ)    3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[206] =
    new SimpleTimeZone(3*kMillisPerHour, "Africa/Kampala" /*EAT*/);
    // Africa/Kampala   Uganda(UG)  3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[207] =
    new SimpleTimeZone(3*kMillisPerHour, "Asia/Aden" /*AST*/);
    // Asia/Aden    Yemen(YE)   3:00    -   AST
    //----------------------------------------------------------
    kSystemTimeZones[208] =
    new SimpleTimeZone(3*kMillisPerHour, "Indian/Mayotte" /*EAT*/);
    // Indian/Mayotte   Mayotte(YT) 3:00    -   EAT
    //----------------------------------------------------------
    kSystemTimeZones[209] =
    new SimpleTimeZone(3*kMillisPerHour, "Asia/Riyadh" /*AST*/);
    // Asia/Riyadh  Saudi Arabia(SA)    3:00    -   AST
    //----------------------------------------------------------
    kSystemTimeZones[210] =
    new SimpleTimeZone(3*kMillisPerHour, "Asia/Baghdad" /*A%sT*/,
      Calendar::APRIL, 1, 0 /*DOM*/, 3*kMillisPerHour,
      Calendar::OCTOBER, 1, 0 /*DOM*/, 4*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Iraq    1991    max -   Apr 1   3:00s   1:00    D
    // Rule Iraq    1991    max -   Oct 1   3:00s   0   D
    // Asia/Baghdad Iraq(IQ)    3:00    Iraq    A%sT
    //----------------------------------------------------------
    kSystemTimeZones[211] =
    new SimpleTimeZone(3*kMillisPerHour, "Europe/Simferopol" /*MSK/MSD*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Crimea  1996    max -   Mar lastSun 0:00u   1:00    -
    // Rule Crimea  1996    max -   Oct lastSun 0:00u   0   -
    // Europe/Simferopol    Ukraine(UA) 3:00    Crimea  MSK/MSD
    //----------------------------------------------------------
    kSystemTimeZones[212] =
    new SimpleTimeZone(3*kMillisPerHour, "Europe/Moscow" /*MSK/MSD*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Moscow    Russia(RU)  3:00    Russia  MSK/MSD
    //----------------------------------------------------------
    kSystemTimeZones[213] =
    new SimpleTimeZone((int32_t)(3.5*kMillisPerHour), "Asia/Tehran" /*IR%sT*/,
      Calendar::MARCH, 21, 0 /*DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, 23, 0 /*DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Iran    1997    1999    -   Mar 21  0:00    1:00    S
    // Rule Iran    1997    1999    -   Sep 23  0:00    0   -
    // Asia/Tehran  Iran(IR)    3:30    Iran    IR%sT
    kSystemTimeZones[214] =
    new SimpleTimeZone((int32_t)(3.5*kMillisPerHour), "MET" /*alias for Asia/Tehran*/,
      Calendar::MARCH, 21, 0 /*DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, 23, 0 /*DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[215] =
    new SimpleTimeZone(4*kMillisPerHour, "Asia/Dubai" /*GST*/);
    // Asia/Dubai   United Arab Emirates(AE)    4:00    -   GST
    //----------------------------------------------------------
    kSystemTimeZones[216] =
    new SimpleTimeZone(4*kMillisPerHour, "Indian/Mauritius" /*MUT*/);
    // Indian/Mauritius Mauritius(MU)   4:00    -   MUT # Mauritius Time
    //----------------------------------------------------------
    kSystemTimeZones[217] =
    new SimpleTimeZone(4*kMillisPerHour, "Asia/Muscat" /*GST*/);
    // Asia/Muscat  Oman(OM)    4:00    -   GST
    //----------------------------------------------------------
    kSystemTimeZones[218] =
    new SimpleTimeZone(4*kMillisPerHour, "Indian/Reunion" /*RET*/);
    // Indian/Reunion   Reunion(RE) 4:00    -   RET # Reunion Time
    //----------------------------------------------------------
    kSystemTimeZones[219] =
    new SimpleTimeZone(4*kMillisPerHour, "Indian/Mahe" /*SCT*/);
    // Indian/Mahe  Seychelles(SC)  4:00    -   SCT # Seychelles Time
    //----------------------------------------------------------
    kSystemTimeZones[220] =
    new SimpleTimeZone(4*kMillisPerHour, "Asia/Yerevan" /*AM%sT*/);
    // Asia/Yerevan Armenia(AM) 4:00    -   AM%sT
    kSystemTimeZones[221] =
    new SimpleTimeZone(4*kMillisPerHour, "NET" /*alias for Asia/Yerevan*/);
    //----------------------------------------------------------
    kSystemTimeZones[222] =
    new SimpleTimeZone(4*kMillisPerHour, "Asia/Baku" /*AZ%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 5*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 5*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule EUAsia  1981    max -   Mar lastSun 1:00u   1:00    S
    // Rule EUAsia  1996    max -   Oct lastSun 1:00u   0   -
    // Asia/Baku    Azerbaijan(AZ)  4:00    EUAsia  AZ%sT
    //----------------------------------------------------------
    kSystemTimeZones[223] =
    new SimpleTimeZone(4*kMillisPerHour, "Asia/Aqtau" /*AQT%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
    // Asia/Aqtau   Kazakhstan(KZ)  4:00    E-EurAsia   AQT%sT
    //----------------------------------------------------------
    kSystemTimeZones[224] =
    new SimpleTimeZone(4*kMillisPerHour, "Europe/Samara" /*SAM%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Samara    Russia(RU)  4:00    Russia  SAM%sT
    //----------------------------------------------------------
    kSystemTimeZones[225] =
    new SimpleTimeZone((int32_t)(4.5*kMillisPerHour), "Asia/Kabul" /*AFT*/);
    // Asia/Kabul   Afghanistan(AF) 4:30    -   AFT
    //----------------------------------------------------------
    kSystemTimeZones[226] =
    new SimpleTimeZone(5*kMillisPerHour, "Indian/Kerguelen" /*TFT*/);
    // Indian/Kerguelen France - year-round bases(FR)   5:00    -   TFT # ISO code TF Time
    //----------------------------------------------------------
    kSystemTimeZones[227] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Tbilisi" /*GET*/);
    // Asia/Tbilisi Georgia(GE) 5:00    -   GET
    //----------------------------------------------------------
    kSystemTimeZones[228] =
    new SimpleTimeZone(5*kMillisPerHour, "Indian/Chagos" /*IOT*/);
    // Indian/Chagos    British Indian Ocean Territory(IO)  5:00    -   IOT # BIOT Time
    //----------------------------------------------------------
    kSystemTimeZones[229] =
    new SimpleTimeZone(5*kMillisPerHour, "Indian/Maldives" /*MVT*/);
    // Indian/Maldives  Maldives(MV)    5:00    -   MVT # Maldives Time
    //----------------------------------------------------------
    kSystemTimeZones[230] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Dushanbe" /*TJT*/);
    // Asia/Dushanbe    Tajikistan(TJ)  5:00    -   TJT # Tajikistan Time
    //----------------------------------------------------------
    kSystemTimeZones[231] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Ashkhabad" /*TMT*/);
    // Asia/Ashkhabad   Turkmenistan(TM)    5:00    -   TMT # Turkmenistan Time
    //----------------------------------------------------------
    kSystemTimeZones[232] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Tashkent" /*UZT*/);
    // Asia/Tashkent    Uzbekistan(UZ)  5:00    -   UZT # Uzbekistan Time
    //----------------------------------------------------------
    kSystemTimeZones[233] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Karachi" /*PKT*/);
    // Asia/Karachi Pakistan(PK)    5:00    -   PKT # Pakistan Time
    kSystemTimeZones[234] =
    new SimpleTimeZone(5*kMillisPerHour, "PLT" /*alias for Asia/Karachi*/);
    //----------------------------------------------------------
    kSystemTimeZones[235] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Bishkek" /*KG%sT*/,
      Calendar::APRIL, 7, -Calendar::SUNDAY /*DOW>=DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Kirgiz  1992    max -   Apr Sun>=7  0:00    1:00    S
    // Rule Kirgiz  1991    max -   Sep lastSun 0:00    0   -
    // Asia/Bishkek Kirgizstan(KG)  5:00    Kirgiz  KG%sT   # Kirgizstan Time
    //----------------------------------------------------------
    kSystemTimeZones[236] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Aqtobe" /*AQT%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
    // Asia/Aqtobe  Kazakhstan(KZ)  5:00    E-EurAsia   AQT%sT
    //----------------------------------------------------------
    kSystemTimeZones[237] =
    new SimpleTimeZone(5*kMillisPerHour, "Asia/Yekaterinburg" /*YEK%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Yekaterinburg   Russia(RU)  5:00    Russia  YEK%sT  # Yekaterinburg Time
    //----------------------------------------------------------
    kSystemTimeZones[238] =
    new SimpleTimeZone((int32_t)(5.5*kMillisPerHour), "Asia/Calcutta" /*IST*/);
    // Asia/Calcutta    India(IN)   5:30    -   IST
    kSystemTimeZones[239] =
    new SimpleTimeZone((int32_t)(5.5*kMillisPerHour), "IST" /*alias for Asia/Calcutta*/);
    //----------------------------------------------------------
    kSystemTimeZones[240] =
    new SimpleTimeZone((int32_t)(5.75*kMillisPerHour), "Asia/Katmandu" /*NPT*/);
    // Asia/Katmandu    Nepal(NP)   5:45    -   NPT # Nepal Time
    //----------------------------------------------------------
    kSystemTimeZones[241] =
    new SimpleTimeZone(6*kMillisPerHour, "Antarctica/Mawson" /*MAWT*/);
    // Antarctica/Mawson    Australia - territories(AQ) 6:00    -   MAWT    # Mawson Time
    //----------------------------------------------------------
    kSystemTimeZones[242] =
    new SimpleTimeZone(6*kMillisPerHour, "Asia/Colombo" /*LKT*/);
    // Asia/Colombo Sri Lanka(LK)   6:00    -   LKT
    //----------------------------------------------------------
    kSystemTimeZones[243] =
    new SimpleTimeZone(6*kMillisPerHour, "Asia/Dacca" /*BDT*/);
    // Asia/Dacca   Bangladesh(BD)  6:00    -   BDT # Bangladesh Time
    kSystemTimeZones[244] =
    new SimpleTimeZone(6*kMillisPerHour, "BST" /*alias for Asia/Dacca*/);
    //----------------------------------------------------------
    kSystemTimeZones[245] =
    new SimpleTimeZone(6*kMillisPerHour, "Asia/Alma-Ata" /*ALM%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
    // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
    // Asia/Alma-Ata    Kazakhstan(KZ)  6:00    E-EurAsia   ALM%sT
    //----------------------------------------------------------
    kSystemTimeZones[246] =
    new SimpleTimeZone(6*kMillisPerHour, "Asia/Novosibirsk" /*NOV%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Novosibirsk Russia(RU)  6:00    Russia  NOV%sT
    //----------------------------------------------------------
    kSystemTimeZones[247] =
    new SimpleTimeZone((int32_t)(6.5*kMillisPerHour), "Indian/Cocos" /*CCT*/);
    // Indian/Cocos Cocos(CC)   6:30    -   CCT # Cocos Islands Time
    //----------------------------------------------------------
    kSystemTimeZones[248] =
    new SimpleTimeZone((int32_t)(6.5*kMillisPerHour), "Asia/Rangoon" /*MMT*/);
    // Asia/Rangoon Burma / Myanmar(MM) 6:30    -   MMT # Myanmar Time
    //----------------------------------------------------------
    kSystemTimeZones[249] =
    new SimpleTimeZone(7*kMillisPerHour, "Indian/Christmas" /*CXT*/);
    // Indian/Christmas Australian miscellany(AU)   7:00    -   CXT # Christmas Island Time
    //----------------------------------------------------------
    kSystemTimeZones[250] =
    new SimpleTimeZone(7*kMillisPerHour, "Asia/Jakarta" /*JAVT*/);
    // Asia/Jakarta Indonesia(ID)   7:00    -   JAVT
    //----------------------------------------------------------
    kSystemTimeZones[251] =
    new SimpleTimeZone(7*kMillisPerHour, "Asia/Phnom_Penh" /*ICT*/);
    // Asia/Phnom_Penh  Cambodia(KH)    7:00    -   ICT
    //----------------------------------------------------------
    kSystemTimeZones[252] =
    new SimpleTimeZone(7*kMillisPerHour, "Asia/Vientiane" /*ICT*/);
    // Asia/Vientiane   Laos(LA)    7:00    -   ICT
    //----------------------------------------------------------
    kSystemTimeZones[253] =
    new SimpleTimeZone(7*kMillisPerHour, "Asia/Saigon" /*ICT*/);
    // Asia/Saigon  Vietnam(VN) 7:00    -   ICT
    kSystemTimeZones[254] =
    new SimpleTimeZone(7*kMillisPerHour, "VST" /*alias for Asia/Saigon*/);
    //----------------------------------------------------------
    kSystemTimeZones[255] =
    new SimpleTimeZone(7*kMillisPerHour, "Asia/Bangkok" /*ICT*/);
    // Asia/Bangkok Thailand(TH)    7:00    -   ICT
    //----------------------------------------------------------
    kSystemTimeZones[256] =
    new SimpleTimeZone(7*kMillisPerHour, "Asia/Krasnoyarsk" /*KRA%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Krasnoyarsk Russia(RU)  7:00    Russia  KRA%sT
    //----------------------------------------------------------
    kSystemTimeZones[257] =
    new SimpleTimeZone(8*kMillisPerHour, "Antarctica/Casey" /*WST*/);
    // Antarctica/Casey Australia - territories(AQ) 8:00    -   WST # Western (Aus) Standard Time
    //----------------------------------------------------------
    kSystemTimeZones[258] =
    new SimpleTimeZone(8*kMillisPerHour, "Australia/Perth" /*WST*/);
    // Australia/Perth  Australia(AU)   8:00    -   WST
    //----------------------------------------------------------
    kSystemTimeZones[259] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Brunei" /*BNT*/);
    // Asia/Brunei  Brunei(BN)  8:00    -   BNT
    //----------------------------------------------------------
    kSystemTimeZones[260] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Hong_Kong" /*C%sT*/);
    // Asia/Hong_Kong   China(HK)   8:00    -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[261] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Ujung_Pandang" /*BORT*/);
    // Asia/Ujung_Pandang   Indonesia(ID)   8:00    -   BORT
    //----------------------------------------------------------
    kSystemTimeZones[262] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Ishigaki" /*CST*/);
    // Asia/Ishigaki    Japan(JP)   8:00    -   CST
    //----------------------------------------------------------
    kSystemTimeZones[263] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Macao" /*C%sT*/);
    // Asia/Macao   Macao(MO)   8:00    -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[264] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Kuala_Lumpur" /*MYT*/);
    // Asia/Kuala_Lumpur    Malaysia(MY)    8:00    -   MYT # Malaysia Time
    //----------------------------------------------------------
    kSystemTimeZones[265] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Manila" /*PH%sT*/);
    // Asia/Manila  Philippines(PH) 8:00    -   PH%sT
    //----------------------------------------------------------
    kSystemTimeZones[266] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Singapore" /*SGT*/);
    // Asia/Singapore   Singapore(SG)   8:00    -   SGT
    //----------------------------------------------------------
    kSystemTimeZones[267] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Taipei" /*C%sT*/);
    // Asia/Taipei  Taiwan(TW)  8:00    -   C%sT
    //----------------------------------------------------------
    kSystemTimeZones[268] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Shanghai" /*C%sT*/);
    // Asia/Shanghai    China(CN)   8:00    -   C%sT
    kSystemTimeZones[269] =
    new SimpleTimeZone(8*kMillisPerHour, "CTT" /*alias for Asia/Shanghai*/);
    //----------------------------------------------------------
    kSystemTimeZones[270] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Ulan_Bator" /*ULA%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour,
      Calendar::SEPTEMBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 0*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Mongol  1991    max -   Mar lastSun 0:00    1:00    S
    // Rule Mongol  1997    max -   Sep lastSun 0:00    0   -
    // Asia/Ulan_Bator  Mongolia(MN)    8:00    Mongol  ULA%sT
    //----------------------------------------------------------
    kSystemTimeZones[271] =
    new SimpleTimeZone(8*kMillisPerHour, "Asia/Irkutsk" /*IRK%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Irkutsk Russia(RU)  8:00    Russia  IRK%sT
    //----------------------------------------------------------
    kSystemTimeZones[272] =
    new SimpleTimeZone(9*kMillisPerHour, "Asia/Jayapura" /*JAYT*/);
    // Asia/Jayapura    Indonesia(ID)   9:00    -   JAYT
    //----------------------------------------------------------
    kSystemTimeZones[273] =
    new SimpleTimeZone(9*kMillisPerHour, "Asia/Pyongyang" /*KST*/);
    // Asia/Pyongyang   ?(KP)   9:00    -   KST
    //----------------------------------------------------------
    kSystemTimeZones[274] =
    new SimpleTimeZone(9*kMillisPerHour, "Asia/Seoul" /*K%sT*/);
    // Asia/Seoul   ?(KR)   9:00    -   K%sT
    //----------------------------------------------------------
    kSystemTimeZones[275] =
    new SimpleTimeZone(9*kMillisPerHour, "Pacific/Palau" /*PWT*/);
    // Pacific/Palau    Palau(PW)   9:00    -   PWT # Palau Time
    //----------------------------------------------------------
    kSystemTimeZones[276] =
    new SimpleTimeZone(9*kMillisPerHour, "Asia/Tokyo" /*JST*/);
    // Asia/Tokyo   Japan(JP)   9:00    -   JST
    kSystemTimeZones[277] =
    new SimpleTimeZone(9*kMillisPerHour, "JST" /*alias for Asia/Tokyo*/);
    //----------------------------------------------------------
    kSystemTimeZones[278] =
    new SimpleTimeZone(9*kMillisPerHour, "Asia/Yakutsk" /*YAK%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Yakutsk Russia(RU)  9:00    Russia  YAK%sT
    //----------------------------------------------------------
    kSystemTimeZones[279] =
    new SimpleTimeZone((int32_t)(9.5*kMillisPerHour), "Australia/Darwin" /*CST*/);
    // Australia/Darwin Australia(AU)   9:30    -   CST
    kSystemTimeZones[280] =
    new SimpleTimeZone((int32_t)(9.5*kMillisPerHour), "ACT" /*alias for Australia/Darwin*/);
    //----------------------------------------------------------
    kSystemTimeZones[281] =
    new SimpleTimeZone((int32_t)(9.5*kMillisPerHour), "Australia/Adelaide" /*CST*/,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule AS  1987    max -   Oct lastSun 2:00s   1:00    -
    // Rule AS  1995    max -   Mar lastSun 2:00s   0   -
    // Australia/Adelaide   South Australia(AU) 9:30    AS  CST
    //----------------------------------------------------------
    kSystemTimeZones[282] =
    new SimpleTimeZone(10*kMillisPerHour, "Pacific/Truk" /*TRUT*/);
    // Pacific/Truk Micronesia(FM)  10:00   -   TRUT    # Truk Time
    //----------------------------------------------------------
    kSystemTimeZones[283] =
    new SimpleTimeZone(10*kMillisPerHour, "Pacific/Guam" /*GST*/);
    // Pacific/Guam Guam(GU)    10:00   -   GST
    //----------------------------------------------------------
    kSystemTimeZones[284] =
    new SimpleTimeZone(10*kMillisPerHour, "Pacific/Saipan" /*MPT*/);
    // Pacific/Saipan   N Mariana Is(MP)    10:00   -   MPT
    //----------------------------------------------------------
    kSystemTimeZones[285] =
    new SimpleTimeZone(10*kMillisPerHour, "Pacific/Port_Moresby" /*PGT*/);
    // Pacific/Port_Moresby Papua New Guinea(PG)    10:00   -   PGT # Papua New Guinea Time
    //----------------------------------------------------------
    kSystemTimeZones[286] =
    new SimpleTimeZone(10*kMillisPerHour, "Australia/Brisbane" /*EST*/);
    // Australia/Brisbane   Australia(AU)   10:00   -   EST
    //----------------------------------------------------------
    kSystemTimeZones[287] =
    new SimpleTimeZone(10*kMillisPerHour, "Asia/Vladivostok" /*VLA%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Vladivostok Russia(RU)  10:00   Russia  VLA%sT
    //----------------------------------------------------------
    kSystemTimeZones[288] =
    new SimpleTimeZone(10*kMillisPerHour, "Australia/Sydney" /*EST*/,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule AN  1987    max -   Oct lastSun 2:00s   1:00    -
    // Rule AN  1996    max -   Mar lastSun 2:00s   0   -
    // Australia/Sydney New South Wales(AU) 10:00   AN  EST
    kSystemTimeZones[289] =
    new SimpleTimeZone(10*kMillisPerHour, "AET" /*alias for Australia/Sydney*/,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[290] =
    new SimpleTimeZone((int32_t)(10.5*kMillisPerHour), "Australia/Lord_Howe" /*LHST*/,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, (int32_t)(0.5*kMillisPerHour),fgStatus);
    // Rule LH  1987    max -   Oct lastSun 2:00s   0:30    -
    // Rule LH  1996    max -   Mar lastSun 2:00s   0   -
    // Australia/Lord_Howe  Lord Howe Island(AU)    10:30   LH  LHST
    //----------------------------------------------------------
    kSystemTimeZones[291] =
    new SimpleTimeZone(11*kMillisPerHour, "Pacific/Ponape" /*PONT*/);
    // Pacific/Ponape   Micronesia(FM)  11:00   -   PONT    # Ponape Time
    //----------------------------------------------------------
    kSystemTimeZones[292] =
    new SimpleTimeZone(11*kMillisPerHour, "Pacific/Efate" /*VU%sT*/);
    // Pacific/Efate    Vanuatu(VU) 11:00   -   VU%sT   # Vanuatu Time
    //----------------------------------------------------------
    kSystemTimeZones[293] =
    new SimpleTimeZone(11*kMillisPerHour, "Pacific/Guadalcanal" /*SBT*/);
    // Pacific/Guadalcanal  Solomon Is(SB)  11:00   -   SBT # Solomon Is Time
    kSystemTimeZones[294] =
    new SimpleTimeZone(11*kMillisPerHour, "SST" /*alias for Pacific/Guadalcanal*/);
    //----------------------------------------------------------
    kSystemTimeZones[295] =
    new SimpleTimeZone(11*kMillisPerHour, "Pacific/Noumea" /*NC%sT*/,
      Calendar::NOVEMBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule NC  1997    max -   Mar Sun>=1  2:00s   0   -
    // Rule NC  1997    max -   Nov lastSun 2:00s   1:00    S
    // Pacific/Noumea   New Caledonia(NC)   11:00   NC  NC%sT
    //----------------------------------------------------------
    kSystemTimeZones[296] =
    new SimpleTimeZone(11*kMillisPerHour, "Asia/Magadan" /*MAG%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Magadan Russia(RU)  11:00   Russia  MAG%sT
    //----------------------------------------------------------
    kSystemTimeZones[297] =
    new SimpleTimeZone((int32_t)(11.5*kMillisPerHour), "Pacific/Norfolk" /*NFT*/);
    // Pacific/Norfolk  Norfolk(NF) 11:30   -   NFT # Norfolk Time
    //----------------------------------------------------------
    kSystemTimeZones[298] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Kosrae" /*KOST*/);
    // Pacific/Kosrae   Micronesia(FM)  12:00   -   KOST    # Kosrae Time
    //----------------------------------------------------------
    kSystemTimeZones[299] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Tarawa" /*GILT*/);
    // Pacific/Tarawa   Kiribati(KI)    12:00   -   GILT    # Gilbert Is Time
    //----------------------------------------------------------
    kSystemTimeZones[300] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Majuro" /*MHT*/);
    // Pacific/Majuro   Marshall Is(MH) 12:00   -   MHT
    //----------------------------------------------------------
    kSystemTimeZones[301] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Nauru" /*NRT*/);
    // Pacific/Nauru    Nauru(NR)   12:00   -   NRT
    //----------------------------------------------------------
    kSystemTimeZones[302] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Funafuti" /*TVT*/);
    // Pacific/Funafuti Tuvalu(TV)  12:00   -   TVT # Tuvalu Time
    //----------------------------------------------------------
    kSystemTimeZones[303] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Wake" /*WAKT*/);
    // Pacific/Wake Wake(US)    12:00   -   WAKT    # Wake Time
    //----------------------------------------------------------
    kSystemTimeZones[304] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Wallis" /*WFT*/);
    // Pacific/Wallis   Wallis and Futuna(WF)   12:00   -   WFT # Wallis & Futuna Time
    //----------------------------------------------------------
    kSystemTimeZones[305] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Fiji" /*FJT*/);
    // Pacific/Fiji Fiji(FJ)    12:00   -   FJT # Fiji Time
    //----------------------------------------------------------
    kSystemTimeZones[306] =
    new SimpleTimeZone(12*kMillisPerHour, "Antarctica/McMurdo" /*NZ%sT*/,
      Calendar::OCTOBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, 15, -Calendar::SUNDAY /*DOW>=DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule NZAQ    1990    max -   Oct Sun>=1  2:00s   1:00    D
    // Rule NZAQ    1990    max -   Mar Sun>=15 2:00s   0   S
    // Antarctica/McMurdo   USA - year-round bases(AQ)  12:00   NZAQ    NZ%sT
    //----------------------------------------------------------
    kSystemTimeZones[307] =
    new SimpleTimeZone(12*kMillisPerHour, "Asia/Kamchatka" /*PET%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Kamchatka   Russia(RU)  12:00   Russia  PET%sT
    //----------------------------------------------------------
    kSystemTimeZones[308] =
    new SimpleTimeZone(12*kMillisPerHour, "Pacific/Auckland" /*NZ%sT*/,
      Calendar::OCTOBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, 15, -Calendar::SUNDAY /*DOW>=DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule NZ  1990    max -   Oct Sun>=1  2:00s   1:00    D
    // Rule NZ  1990    max -   Mar Sun>=15 2:00s   0   S
    // Pacific/Auckland New Zealand(NZ) 12:00   NZ  NZ%sT
    kSystemTimeZones[309] =
    new SimpleTimeZone(12*kMillisPerHour, "NST" /*alias for Pacific/Auckland*/,
      Calendar::OCTOBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::MARCH, 15, -Calendar::SUNDAY /*DOW>=DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[310] =
    new SimpleTimeZone((int32_t)(12.75*kMillisPerHour), "Pacific/Chatham" /*CHA%sT*/,
      Calendar::OCTOBER, 1, -Calendar::SUNDAY /*DOW>=DOM*/, (int32_t)(2.75*kMillisPerHour),
      Calendar::MARCH, 15, -Calendar::SUNDAY /*DOW>=DOM*/, (int32_t)(3.75*kMillisPerHour), 1*kMillisPerHour,fgStatus);
    // Rule Chatham 1990    max -   Oct Sun>=1  2:45s   1:00    D
    // Rule Chatham 1991    max -   Mar Sun>=15 2:45s   0   S
    // Pacific/Chatham  New Zealand(NZ) 12:45   Chatham CHA%sT
    //----------------------------------------------------------
    kSystemTimeZones[311] =
    new SimpleTimeZone(13*kMillisPerHour, "Pacific/Enderbury" /*PHOT*/);
    // Pacific/Enderbury    Kiribati(KI)    13:00   -   PHOT
    //----------------------------------------------------------
    kSystemTimeZones[312] =
    new SimpleTimeZone(13*kMillisPerHour, "Pacific/Tongatapu" /*TOT*/);
    // Pacific/Tongatapu    Tonga(TO)   13:00   -   TOT
    //----------------------------------------------------------
    kSystemTimeZones[313] =
    new SimpleTimeZone(13*kMillisPerHour, "Asia/Anadyr" /*ANA%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Asia/Anadyr  Russia(RU)  13:00   Russia  ANA%sT
    //----------------------------------------------------------
    kSystemTimeZones[314] =
    new SimpleTimeZone(14*kMillisPerHour, "Pacific/Kiritimati" /*LINT*/);
    // Pacific/Kiritimati   Kiribati(KI)    14:00   -   LINT
    kSystemTimeZones[315] =
    new SimpleTimeZone(-7*kMillisPerHour, "PNT" /*alias for America/Phoenix*/);
    //----------------------------------------------------------
    kSystemTimeZones[316] =
    new SimpleTimeZone((int32_t)(-3.5*kMillisPerHour), "CNT" /*alias for America/St_Johns*/,
      Calendar::APRIL, 1, -Calendar::SUNDAY /*DOW>=DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    //----------------------------------------------------------
    kSystemTimeZones[317] =
    new SimpleTimeZone(10*kMillisPerHour, "Antarctica/DumontDUrville" /*DDUT*/);
    // Antarctica/DumontDUrville    France - year-round bases(AQ)   10:00   -   DDUT    # Dumont-d'Urville Time
    //----------------------------------------------------------
    kSystemTimeZones[318] =
    new SimpleTimeZone(2*kMillisPerHour, "Europe/Kaliningrad" /*EE%sT*/,
      Calendar::MARCH, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 2*kMillisPerHour,
      Calendar::OCTOBER, -1, Calendar::SUNDAY /*DOW_IN_DOM*/, 3*kMillisPerHour, 1*kMillisPerHour,fgStatus);
    // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
    // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
    // Europe/Kaliningrad   Russia(RU)  2:00    Russia  EE%sT
    //----------------------------------------------------------
    kSystemTimeZones[319] =
    new SimpleTimeZone(6*kMillisPerHour, "Asia/Thimbu" /*BTT*/);
    // Asia/Thimbu  Bhutan(BT)  6:00    -   BTT # Bhutan Time
    //----------------------------------------------------------
  }
}
// ---------------- END GENERATED DATA ----------------

// *** clean up from definitions
#undef int
#undef kMillisPerHour

// *****************************************************************************
// class TimeZone
// *****************************************************************************

TimeZone::TimeZone()
{
}

// -------------------------------------

TimeZone::~TimeZone()
{
}

// -------------------------------------

TimeZone::TimeZone(const TimeZone &source)
    :   fID(source.fID)
{
}

// -------------------------------------

TimeZone &
TimeZone::operator=(const TimeZone &right)
{
    if (this != &right) fID = right.fID;
    return *this;
}

// -------------------------------------

bool_t
TimeZone::operator==(const TimeZone& that) const
{
    return getDynamicClassID() == that.getDynamicClassID() &&
        fID == that.fID;
}

// -------------------------------------

/**
 * Return a reference to the static Hashtable of registered TimeZone
 * objects.  Performs initialization if necessary.
 *
 * This method is also responsible for initializing the array
 * fgAvailableIDs and fgAvailableIDsCount.
 */
const UHashtable&
TimeZone::getHashtable()
{
  if (kSystemInited == FALSE) {
      initSystemTimeZones();
  }
  if (fgAvailableIDs == 0)
    {
      UHashtable *newHashtable;
      int32_t    newTimeZonesCount;
      UnicodeString *newAvailableIds;
     
      // build a hashtable that contains all the TimeZone objects in kSystemTimeZones
      // and maps their IDs to the actual TimeZone objects (gives us fast lookup)
      UErrorCode err = U_ZERO_ERROR;
      newHashtable = uhash_open((UHashFunction) uhash_hashUString, &err);
      uhash_setValueDeleter(newHashtable, TimeZone::deleteTimeZone);
      
      newTimeZonesCount = kSystemTimeZonesCount;
      newAvailableIds = new UnicodeString[newTimeZonesCount];

        int32_t i;
        for (i=0; i<kSystemTimeZonesCount; ++i)
        {
            SimpleTimeZone *tz = kSystemTimeZones[i];
        uhash_putKey(newHashtable, (tz->getID(newAvailableIds[i])).hashCode() & 0x7FFFFFFF, tz , &err);
    }
    
        Mutex lock;
        // We must recheck fgHashtable for correct locking
        if (fgHashtable == 0)
      {
        fgHashtable = newHashtable;
        fgAvailableIDsCount = newTimeZonesCount;
        fgAvailableIDs = newAvailableIds;
      }
    else
      {
        uhash_close(newHashtable);
        delete [] newAvailableIds;
      }
    }
    return *fgHashtable;
}

// -------------------------------------

/**
 * Convert a non-localized string to an integer using a system function.
 * Return a failing UErrorCode status if all characters are not parsed.
 */
/*int32_t
TimeZone::stringToInteger(const UnicodeString& string, UErrorCode& status)
{
    if (FAILURE(status)) return 0;

    int32_t len = string.size();
    char *number = new char[1 + len];
    if (number == 0) { status = U_MEMORY_ALLOCATION_ERROR; return 0; }
    char *end;

    string.extract(0, len, number);
    number[len] = 0;
    int32_t value = strtol(number, &end, 10); // Radix 10

    delete[] number;

    if (end-number != len || len == 0)
        status = U_INVALID_FORMAT_ERROR;

    return value;
}*/

// -------------------------------------

TimeZone*
TimeZone::createTimeZone(const UnicodeString& ID)
{
    if (kSystemInited == FALSE) {
      initSystemTimeZones();
    }
    /* We first try to lookup the zone ID in our hashtable.  If this fails,
     * we try to parse it as a custom string GMT[+-]hh:mm.  This allows us
     * to recognize zones in user.timezone that otherwise cannot be
     * identified.  We do the recognition here, rather than in getDefault(),
     * so that the default zone is always the result of calling
     * getTimeZone() with the property user.timezone.
     *
     * If all else fails, we return GMT, which is probably not what the user
     * wants, but at least is a functioning TimeZone object. */

    TimeZone* result = NULL;
    UHashtable h = getHashtable();
    result = (TimeZone*)uhash_get(&h, ID.hashCode() & 0x7FFFFFFF);
    if(result != NULL)
        return result->clone();

    // result == NULL
      result = createCustomTimeZone(ID);
    if(result == NULL)
        result = GMT->clone();
    
    return result;
}

// -------------------------------------

void
TimeZone::initDefault()
{
  if (kSystemInited == FALSE) {
      initSystemTimeZones();
  }
  // This function is called by createDefault() and adoptDefault() to initialize
  // fgDefaultZone from the system default time zone.  If fgDefaultZone is already
  // filled in, we obviously don't have to do anything.
  if (fgDefaultZone == NULL)
    {
      
      // We access system timezone data through TPlatformUtilities,
      // including tzset(), timezone, and tzname[].
      TimeZone *newZone = NULL;
      int32_t rawOffset = 0;
      const char *hostID;
      UnicodeString ID;
      
      // First, try to create the timezone from our hashtable, based
      // on the string ID in tzname[0].
      {
    Mutex lock; // mutexed to avoid threading issues in the platform fcns.
    icu_tzset(); // Initialize tz... system data
    
    // get the timezone ID from the host.
    hostID = icu_tzname(0);
    
    // Invert sign because UNIX semantics are backwards
    rawOffset = icu_timezone() * -kMillisPerSecond;
      }

      // create UnicodeString ID from hostID (to avoid Mutex deadlock)
      ID = hostID;

      if (ID.size() > 0) 
    {
      UHashtable h = getHashtable();
      TimeZone *z = (TimeZone*)uhash_get(&h,ID.hashCode() & 0x7FFFFFFF);
      // Important -- must clone the TimeZone because fgDefaultZone may be
      // deleted by adoptDefault().
      if (z != 0) newZone = z->clone();
    }

      // If we couldn't get the time zone ID from the host, use the
      // default host timezone offset, timezone.
      if (newZone == NULL)
    {
      // just pick the first entry in the time zone list that has the
      // appropriate GMT offset (if there is one)
      int32_t numMatches = 0;
      const UnicodeString** matches = createAvailableIDs(rawOffset, numMatches);
      if (numMatches > 0) {
        newZone = createTimeZone(*matches[0]);
        delete [] matches;
      }
        }

      // If we _still_ don't have a time zone, use the one specified by kLastResortID.
      // (This call should always succeed, since we code the Hashtable
      // to always contain this TimeZone.)
      if (newZone == NULL)
    newZone = createTimeZone(kLastResortID);

      {
    Mutex lock2;
    if(fgDefaultZone != NULL)
      delete newZone;
    else
      fgDefaultZone = newZone;
      }
    }
}

// -------------------------------------

TimeZone*
TimeZone::createDefault()
{
    initDefault(); // After this call fgDefaultZone is not NULL
    Mutex lock; // Careful...must have mutex here
    return fgDefaultZone->clone();
}

// -------------------------------------

void
TimeZone::adoptDefault(TimeZone* zone)
{
  if (kSystemInited == FALSE) {
      initSystemTimeZones();
  }

    if (zone != NULL)
    {
        Mutex mutex;

        if (fgDefaultZone != NULL) {
            delete fgDefaultZone;
		}

        fgDefaultZone = zone;
    }
}
// -------------------------------------

void
TimeZone::setDefault(const TimeZone& zone)
{
    adoptDefault(zone.clone());
}

// -------------------------------------

const UnicodeString** const
TimeZone::createAvailableIDs(int32_t rawOffset, int32_t& numIDs)
{
    // We are creating a new array to existing UnicodeString pointers.
    // The caller will delete the array when done, but not the pointers
    // in the array.

    getHashtable(); // Force initialization of fgAvailableIDs

    const UnicodeString** const ptrArray =
        (const UnicodeString** const) new UnicodeString*[fgAvailableIDsCount];

    numIDs = 0;
    int32_t pos = -1;
    UHashtable e = getHashtable();
    void* value;
    UnicodeString anID;
    
    while (numIDs < fgAvailableIDsCount && (value = uhash_nextElement(&e, &pos)))
    {
      if (rawOffset == ((SimpleTimeZone *)value)->getRawOffset())
    ptrArray[numIDs++] = new UnicodeString(((SimpleTimeZone *)value)->getID(anID));
    }
    
    return ptrArray;
}

// -------------------------------------

const UnicodeString** const
TimeZone::createAvailableIDs(int32_t& numIDs)
{
    // We are creating a new array to existing UnicodeString pointers.
    // The caller will delete the array when done, but not the pointers
    // in the array.

    getHashtable(); // Force initialization of fgAvailableIDs

    const UnicodeString** const ptrArray =
        (const UnicodeString** const) new UnicodeString*[fgAvailableIDsCount];

    for (int32_t i=0; i<fgAvailableIDsCount; ++i)
        ptrArray[i] = &fgAvailableIDs[i];

    numIDs = fgAvailableIDsCount;
    return ptrArray;
}

// ---------------------------------------


UnicodeString&
TimeZone::getDisplayName(UnicodeString& result) const
{
    return getDisplayName(FALSE,LONG,Locale::getDefault(), result);
}

UnicodeString&
TimeZone::getDisplayName(const Locale& locale, UnicodeString& result) const
{
    return getDisplayName(FALSE, LONG, locale, result);
}

UnicodeString&
TimeZone::getDisplayName(bool_t daylight, EDisplayType style, UnicodeString& result)  const
{
    return getDisplayName(daylight,style, Locale::getDefault(), result);
}

UnicodeString&
TimeZone::getDisplayName(bool_t daylight, EDisplayType style, const Locale& locale, UnicodeString& result) const
{
  if (kSystemInited == FALSE) {
      initSystemTimeZones();
  }
    // SRL TODO: cache the SDF, just like java.
    UErrorCode status = U_ZERO_ERROR;

    SimpleDateFormat format(style == LONG ? "zzzz" : "z",locale,status);

    if(!SUCCESS(status))
    {
        // *** SRL what do I do here?!!
        return result.remove();
    }

    // Create a new SimpleTimeZone as a stand-in for this zone; the
    // stand-in will have no DST, or all DST, but the same ID and offset,
    // and hence the same display name.
    // We don't cache these because they're small and cheap to create.
    UnicodeString tempID;
    SimpleTimeZone *tz =  daylight ?
        // For the pure-DST zone, we use the month before JANUARY and
        // the month after DECEMBER; if this fails to work at some future
        // date because of increased error checking in SimpleTimeZone,
        // change these parameters to JANUARY and DECEMBER, which will
        // work equally well, but will be slower.

        new SimpleTimeZone(getRawOffset(), getID(tempID),
                           Calendar::JANUARY - 1, 1, 0, 0,
                           Calendar::DECEMBER + 1, 31, 0, kMillisPerDay, status) :
        new SimpleTimeZone(getRawOffset(), getID(tempID));

    format.applyPattern(style == LONG ? "zzzz" : "z");
    Calendar *myCalendar = (Calendar*)format.getCalendar();
    myCalendar->setTimeZone(*tz); // copy
    
    delete tz;

    FieldPosition pos(FieldPosition::DONT_CARE);
    return format.format(UDate(), result, pos); // Doesn't matter what date we use
}


/**
 * Parse a custom time zone identifier and return a corresponding zone.
 * @param id a string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
 * GMT[+-]hh.
 * @return a newly created SimpleTimeZone with the given offset and
 * no Daylight Savings Time, or null if the id cannot be parsed.
*/
TimeZone*
TimeZone::createCustomTimeZone(const UnicodeString& id)
{
    static const int32_t         kParseFailed = -99999;

    NumberFormat* numberFormat = 0;
    
    UnicodeString idUppercase = id;
    idUppercase.toUpper();

    if (id.size() > GMT_ID_LENGTH &&
        idUppercase.startsWith(GMT_ID))
    {
        ParsePosition pos(GMT_ID_LENGTH);
        bool_t negative = FALSE;
        int32_t offset;

        if (id[pos.getIndex()] == 0x002D /*'-'*/)
            negative = TRUE;
        else if (id[pos.getIndex()] != 0x002B /*'+'*/)
            return 0;
        pos.setIndex(pos.getIndex() + 1);

        UErrorCode success = U_ZERO_ERROR;
        numberFormat = NumberFormat::createInstance(success);
        numberFormat->setParseIntegerOnly(TRUE);

    
        // Look for either hh:mm, hhmm, or hh
        int32_t start = pos.getIndex();
        
        Formattable n(kParseFailed);

        numberFormat->parse(id, n, pos);
        if (pos.getIndex() == start) return 0;
        offset = n.getLong();

        if (pos.getIndex() < id.size() &&
            id[pos.getIndex()] == 0x003A /*':'*/)
        {
            // hh:mm
            offset *= 60;
            pos.setIndex(pos.getIndex() + 1);
            int32_t oldPos = pos.getIndex();
            n.setLong(kParseFailed);
            numberFormat->parse(id, n, pos);
            if (pos.getIndex() == oldPos) return 0;
            offset += n.getLong();
        }
        else 
        {
            // hhmm or hh

            // Be strict about interpreting something as hh; it must be
            // an offset < 30, and it must be one or two digits. Thus
            // 0010 is interpreted as 00:10, but 10 is interpreted as
            // 10:00.
            if (offset < 30 && (pos.getIndex() - start) <= 2)
                offset *= 60; // hh, from 00 to 29; 30 is 00:30
            else
                offset = offset % 100 + offset / 100 * 60; // hhmm
        }

        if(negative)
            offset = -offset;

        return new SimpleTimeZone(offset * 60000, CUSTOM_ID);
        delete numberFormat;
    }
    return 0;
}


bool_t 
TimeZone::hasSameRules(const TimeZone& other) const
{
    return (getRawOffset() == other.getRawOffset() && 
            useDaylightTime() == other.useDaylightTime());
}

//eof
