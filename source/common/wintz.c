/*
********************************************************************************
*   Copyright (C) 2005-2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINTZ.CPP
*
********************************************************************************
*/

#include "unicode/utypes.h"

#ifdef U_WINDOWS

#include "wintz.h"

#include "cmemory.h"
#include "cstring.h"

#include "unicode/ustring.h"

#   define WIN32_LEAN_AND_MEAN
#   define VC_EXTRALEAN
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>

#define ARRAY_SIZE(array) (sizeof array / sizeof array[0])
#define NEW_ARRAY(type,count) (type *) uprv_malloc((count) * sizeof(type))
#define DELETE_ARRAY(array) uprv_free((void *) (array))

#define ICUID_STACK_BUFFER_SIZE 32

/* The layout of the Tzi value in the registry */
typedef struct
{
    int32_t bias;
    int32_t standardBias;
    int32_t daylightBias;
    SYSTEMTIME standardDate;
    SYSTEMTIME daylightDate;
} TZI;

typedef struct
{
    const char *icuid;
    const char *winid;
} WindowsICUMap;

typedef struct {
    const char* winid;
    const char* altwinid;
} WindowsZoneRemap;

/**
 * Various registry keys and key fragments.
 */
static const char CURRENT_ZONE_REGKEY[] = "SYSTEM\\CurrentControlSet\\Control\\TimeZoneInformation\\";
static const char STANDARD_NAME_REGKEY[] = "StandardName";
static const char STANDARD_TIME_REGKEY[] = " Standard Time";
static const char TZI_REGKEY[] = "TZI";
static const char STD_REGKEY[] = "Std";

/**
 * HKLM subkeys used to probe for the flavor of Windows.  Note that we
 * specifically check for the "GMT" zone subkey; this is present on
 * NT, but on XP has become "GMT Standard Time".  We need to
 * discriminate between these cases.
 */
static const char* const WIN_TYPE_PROBE_REGKEY[] = {
    /* WIN_9X_ME_TYPE */
    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Time Zones",

    /* WIN_NT_TYPE */
    "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones\\GMT"

    /* otherwise: WIN_2K_XP_TYPE */
};

/**
 * The time zone root subkeys (under HKLM) for different flavors of
 * Windows.
 */
static const char* const TZ_REGKEY[] = {
    /* WIN_9X_ME_TYPE */
    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Time Zones\\",

    /* WIN_NT_TYPE | WIN_2K_XP_TYPE */
    "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones\\"
};

/**
 * Flavor of Windows, from our perspective.  Not a real OS version,
 * but rather the flavor of the layout of the time zone information in
 * the registry.
 */
enum {
    WIN_9X_ME_TYPE = 1,
    WIN_NT_TYPE = 2,
    WIN_2K_XP_TYPE = 3
};

# if 0
/*
 * ZONE_MAP from supplementalData.txt
 */
static const WindowsICUMap NEW_ZONE_MAP[] = {
    {"Africa/Cairo",         "Egypt"},
    {"Africa/Casablanca",    "Greenwich"},
    {"Africa/Johannesburg",  "South Africa"},
    {"Africa/Lagos",         "W. Central Africa"},
    {"Africa/Nairobi",       "E. Africa"},
    {"Africa/Windhoek",      "Namibia"},
    {"America/Anchorage",    "Alaskan"},
    {"America/Bogota",       "SA Pacific"},
    {"America/Buenos_Aires", "SA Eastern"},
    {"America/Caracas",      "SA Western"},
    {"America/Chicago",      "Central"},
    {"America/Chihuahua",    "Mountain Standard Time (Mexico)"},
    {"America/Denver",       "Mountain"},
    {"America/Godthab",      "Greenland"},
    {"America/Guatemala",    "Central America"},
    {"America/Halifax",      "Atlantic"},
    {"America/Indianapolis", "US Eastern"},
    {"America/Los_Angeles",  "Pacific"},
    {"America/Manaus",       "Central Brazilian"},
    {"America/Mexico_City",  "Central Standard Time (Mexico)"},
    {"America/Montevideo",   "Montevideo"},
    {"America/New_York",     "Eastern"},
    {"America/Noronha",      "Mid-Atlantic"},
    {"America/Phoenix",      "US Mountain"},
    {"America/Regina",       "Canada Central"},
    {"America/Santiago",     "Pacific SA"},
    {"America/Sao_Paulo",    "E. South America"},
    {"America/St_Johns",     "Newfoundland"},
    {"America/Tijuana",      "Pacific Standard Time (Mexico)"},
    {"Asia/Amman",           "Jordan"},
    {"Asia/Baghdad",         "Arabic"},
    {"Asia/Baku",            "Azerbaijan"},
    {"Asia/Bangkok",         "SE Asia"},
    {"Asia/Beirut",          "Middle East"},
    {"Asia/Calcutta",        "India"},
    {"Asia/Colombo",         "Sri Lanka"},
    {"Asia/Dhaka",           "Central Asia"},
    {"Asia/Jerusalem",       "Israel"},
    {"Asia/Kabul",           "Afghanistan"},
    {"Asia/Karachi",         "West Asia"},
    {"Asia/Katmandu",        "Nepal"},
    {"Asia/Krasnoyarsk",     "North Asia"},
    {"Asia/Muscat",          "Arabian"},
    {"Asia/Novosibirsk",     "N. Central Asia"},
    {"Asia/Rangoon",         "Myanmar"},
    {"Asia/Riyadh",          "Arab"},
    {"Asia/Seoul",           "Korea"},
    {"Asia/Shanghai",        "China"},
    {"Asia/Singapore",       "Singapore"},
    {"Asia/Taipei",          "Taipei"},
    {"Asia/Tbilisi",         "Georgian"},
    {"Asia/Tehran",          "Iran"},
    {"Asia/Tokyo",           "Tokyo"},
    {"Asia/Ulaanbaatar",     "North Asia East"},
    {"Asia/Vladivostok",     "Vladivostok"},
    {"Asia/Yakutsk",         "Yakutsk"},
    {"Asia/Yekaterinburg",   "Ekaterinburg"},
    {"Asia/Yerevan",         "Caucasus"},
    {"Atlantic/Azores",      "Azores"},
    {"Atlantic/Cape_Verde",  "Cape Verde"},
    {"Australia/Adelaide",   "Cen. Australia"},
    {"Australia/Brisbane",   "E. Australia"},
    {"Australia/Darwin",     "AUS Central"},
    {"Australia/Hobart",     "Tasmania"},
    {"Australia/Perth",      "W. Australia"},
    {"Australia/Sydney",     "AUS Eastern"},
    {"Europe/Berlin",        "W. Europe"},
    {"Europe/Helsinki",      "FLE"},
    {"Europe/Istanbul",      "GTB"},
    {"Europe/London",        "GMT"},
    {"Europe/Minsk",         "E. Europe"},
    {"Europe/Moscow",        "Russian"},
    {"Europe/Paris",         "Romance"},
    {"Europe/Prague",        "Central Europe"},
    {"Europe/Warsaw",        "Central European"},
    {"Pacific/Apia",         "Samoa"},
    {"Pacific/Auckland",     "New Zealand"},
    {"Pacific/Fiji",         "Fiji"},
    {"Pacific/Guadalcanal",  "Central Pacific"},
    {"Pacific/Guam",         "West Pacific"},
    {"Pacific/Honolulu",     "Hawaiian"},
    {"Pacific/Kwajalein",    "Dateline"},
    {"Pacific/Tongatapu",    "Tonga"}
};
#endif

/* NOTE: Some Windows zone ids appear more than once. In such cases the
 * ICU zone id from the first one is the preferred match.
 */
static const WindowsICUMap ZONE_MAP[] = {
    /* S (GMT-12:00) International Date Line West */
    {"Etc/GMT+12",                  "Dateline"},

    /* S (GMT-11:00) Midway Island, Samoa */
    {"Pacific/Apia",                "Samoa"},
    {"Pacific/Midway",              "Samoa"},

    /* S (GMT-10:00) Hawaii */
    {"Pacific/Honolulu",            "Hawaiian"},

    /* D (GMT-09:00) Alaska */
    {"America/Anchorage",           "Alaskan"},
    {"America/Juneau",              "Alaskan"},
    {"America/Yakutat",             "Alaskan"},
    {"America/Nome",                "Alaskan"},

    /* D (GMT-08:00) Pacific Time (US & Canada) */
    {"America/Los_Angeles",         "Pacific"},
    {"America/Dawson",              "Pacific"},
    {"America/Vancouver",           "Pacific"},
    {"America/Whitehorse",          "Pacific"},

    /* D (GMT-08:00) Tijuana, Baja California */
    {"America/Tijuana",             "Pacific Standard Time (Mexico)"},

    /* D (GMT-07:00) Mountain Time (US & Canada) */
    {"America/Denver",              "Mountain"},
    {"America/Boise",               "Mountain"},
    {"America/Cambridge_Bay",       "Mountain"},
    {"America/Edmonton",            "Mountain"},
    {"America/Inuvik",              "Mountain"},
    {"America/Shiprock",            "Mountain"},
    {"America/Yellowknife",         "Mountain"},

    /* S (GMT-07:00) Arizona */
    {"America/Phoenix",             "US Mountain"},

    /* D (GMT-07:00) Chihuahua, La Paz, Mazatlan */
    {"America/Chihuahua",           "Mountain Standard Time (Mexico)"},
    {"America/Mazatlan",            "Mountain Standard Time (Mexico)"},

    /* D (GMT-06:00) Central Time (US & Canada) */
    {"America/Chicago",             "Central"},
    {"America/Indiana/Knox",        "Central"},
    {"America/Indiana/Tell_City",   "Central"},
    {"America/Menominee",           "Central"},
    {"America/North_Dakota/Center", "Central"},
    {"America/North_Dakota/New_Salem",      "Central"},
    {"America/Rainy_River",         "Central"},
    {"America/Rankin_Inlet",        "Central"},
    {"America/Winnipeg",            "Central"},

    /* D (GMT-06:00) Guadalajara, Mexico City, Monterrey */
    {"America/Mexico_City",         "Central Standard Time (Mexico)"},
    {"America/Monterrey",           "Central Standard Time (Mexico)"},

    /* S (GMT-06:00) Central America */
    {"America/Guatemala",           "Central America"},
    {"America/Belize",              "Central America"},
    {"America/Costa_Rica",          "Central America"},
    {"America/El_Salvador",         "Central America"},
    {"America/Managua",             "Central America"},
    {"America/Tegucigalpa",         "Central America"},
    {"Pacific/Galapagos",           "Central America"},

    /* S (GMT-06:00) Saskatchewan */
    {"America/Regina",              "Canada Central"},
    {"America/Swift_Current",       "Canada Central"},

    /* D (GMT-05:00) Eastern Time (US & Canada) */
    {"America/New_York",            "Eastern"},
    {"America/Detroit",             "Eastern"},
    {"America/Grand_Turk",          "Eastern"},
    {"America/Indiana/Marengo",     "Eastern"},
    {"America/Indiana/Petersburg",  "Eastern"},
    {"America/Indiana/Vevay",       "Eastern"},
    {"America/Indiana/Vincennes",   "Eastern"},
    {"America/Indiana/Winamac",     "Eastern"},
    {"America/Indianapolis",        "Eastern"},
    {"America/Iqaluit",             "Eastern"},
    {"America/Kentucky/Monticello", "Eastern"},
    {"America/Louisville",          "Eastern"},
    {"America/Montreal",            "Eastern"},
    {"America/Nassau",              "Eastern"},
    {"America/Nipigon",             "Eastern"},
    {"America/Pangnirtung",         "Eastern"},
    {"America/Thunder_Bay",         "Eastern"},
    {"America/Toronto",             "Eastern"},

    /* S (GMT-05:00) Bogota, Lima, Quito, Rio Branco */
    {"America/Bogota",              "SA Pacific"},
    {"America/Lima",                "SA Pacific"},
    {"America/Guayaquil",           "SA Pacific"},
    {"America/Rio_Branco",          "SA Pacific"},

    /* S (GMT-05:00) Indiana (East) */
    {"Etc/GMT+5",                   "US Eastern"},

    /* S (GMT-04:30) Caracas */
    {"America/Caracas",             "Venezuela"},

    /* D (GMT-04:00) Atlantic Time (Canada) */
    {"America/Halifax",             "Atlantic"},
    {"America/Glace_Bay",           "Atlantic"},
    {"America/Moncton",             "Atlantic"},

    /* D (GMT-04:00) Santiago */
    {"America/Santiago",            "Pacific SA"},

    /* D (GMT-04:00) Manaus */ /* MS bug - DST is not used */
    {"America/Manaus",              "Central Brazilian"}, 

    /* S (GMT-04:00) La Paz */
    {"America/La_Paz",              "SA Western"}, 

    /* D (GMT-03:30) Newfoundland */
    {"America/St_Johns",            "Newfoundland"},

    /* D (GMT-03:00) Brasilia */
    {"America/Sao_Paulo",           "E. South America"},

    /* D (GMT-03:00) Buenos Aires */
    {"America/Buenos_Aires",        "Argentina"}, 

    /* D (GMT-03:00) Greenland */
    {"America/Godthab",             "Greenland"},

    /* D (GMT-03:00) Montevideo */
    {"America/Montevideo",          "Montevideo"},

    /* S (GMT-03:00) Georgetown */ /* MS bug - Georgetown uses GMT-04:00 */
    {"Etc/GMT+3",                   "SA Eastern"}, 

    /* D (GMT-02:00) Mid-Atlantic */ /* MS bug - There is no such zone using GMT-02:00 with DST */
    {"America/South_Georgia",       "Mid-Atlantic"},
    {"America/Noronha",             "Mid-Atlantic"},

    /* D (GMT-01:00) Azores */
    {"Atlantic/Azores",             "Azores"},

    /* S (GMT-01:00) Cape Verde Is. */
    {"Atlantic/Cape_Verde",         "Cape Verde"},

    /* D (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London */
    {"Europe/London",               "GMT"},
    {"Europe/Dublin",               "GMT"},
    {"Europe/Lisbon",               "GMT"},

    /* S (GMT) Casablanca, Monrovia, Reykjavik */
    {"Africa/Casablanca",           "Greenwich"},
    {"Africa/Monrovia",             "Greenwich"},
    {"Atlantic/Reykjavik",          "Greenwich"},

    /* D (GMT+01:00) Brussels, Copenhagen, Madrid, Paris */
    {"Europe/Paris",                "Romance"},
    {"Europe/Brussels",             "Romance"},
    {"Europe/Copenhagen",           "Romance"},
    {"Europe/Madrid",               "Romance"},

    /* D (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna */
    {"Europe/Berlin",               "W. Europe"},
    {"Europe/Amsterdam",            "W. Europe"},
    {"Europe/Zurich",               "W. Europe"},
    {"Europe/Rome",                 "W. Europe"},
    {"Europe/Stockholm",            "W. Europe"},
    {"Europe/Vienna",               "W. Europe"},

    /* D (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague */
    {"Europe/Budapest",             "Central Europe"}, 
    {"Europe/Belgrade",             "Central Europe"}, 
    {"Europe/Bratislava",           "Central Europe"}, 
    {"Europe/Ljubljana",            "Central Europe"}, 
    {"Europe/Prague",               "Central Europe"}, 

    /* D (GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb */
    {"Eurpoe/Warsaw",               "Central European"},
    {"Eurpoe/Sarajevo",             "Central European"},
    {"Eurpoe/Skopje",               "Central European"},
    {"Eurpoe/Zagreb",               "Central European"},

    /* S (GMT+01:00) West Central Africa */
    {"Africa/Lagos",                "W. Central Africa"},
    {"Africa/Luanda",               "W. Central Africa"},
    {"Africa/Porto-Novo",           "W. Central Africa"},
    {"Africa/Douala",               "W. Central Africa"},
    {"Africa/Bangui",               "W. Central Africa"},
    {"Africa/Ndjamena",             "W. Central Africa"},
    {"Africa/Kinshasa",             "W. Central Africa"},
    {"Africa/Brazzaville",          "W. Central Africa"},
    {"Africa/Malabo",               "W. Central Africa"},
    {"Africa/Libreville",           "W. Central Africa"},
    {"Africa/Niamey",               "W. Central Africa"},

    /* D (GMT+02:00) Athens, Bucharest, Istanbul */
    {"Europe/Istanbul",             "GTB"},
    {"Europe/Athens",               "GTB"},
    {"Europe/Bucharest",            "GTB"},

    /* D (GMT+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius */
    {"Europe/Kiev",                 "FLE"},
    {"Europe/Helsinki",             "FLE"},
    {"Europe/Riga",                 "FLE"},
    {"Europe/Sofia",                "FLE"},
    {"Europe/Tallinn",              "FLE"},
    {"Europe/Vilnius",              "FLE"},

    /* D (GMT+02:00) Jerusalem */
    {"Asia/Jerusalem",              "Israel"},

    /* D (GMT+02:00) Minsk */
    {"Europe/Minsk",                "E. Europe"}, 

    /* D (GMT+02:00) Cairo */
    {"Africa/Cairo",                "Egypt"},

    /* D (GMT+02:00) Amman */
    {"Asia/Amman",                  "Jordan"},

    /* D (GMT+02:00) Beirut */
    {"Asia/Beirut",                 "Middle East"},

    /* D (GMT+02:00) Windhoek */
    {"Africa/Windhoek",             "Namibia"},

    /* S (GMT+02:00) Harare, Pretoria */
    {"Africa/Johannesburg",         "South Africa"},
    {"Africa/Harare",               "South Africa"},

    /* D (GMT+03:00) Moscow, St. Petersburg, Volgograd */
    {"Europe/Moscow",               "Russian"},
    {"Europe/Volgograd",            "Russian"},

    /* D (GMT+03:00) Baghdad */
    {"Asia/Baghdad",                "Arabic"},

    /* S (GMT+03:00) Kuwait, Riyadh */
    {"Asia/Riyadh",                 "Arab"},
    {"Asia/Kuwait",                 "Arab"},

    /* S (GMT+03:00) Nairobi */
    {"Africa/Nairobi",              "E. Africa"},

    /* S (GMT+03:00) Tbilisi */ /* MS bug - Tbilisi uses GMT+04:00 */
    {"Etc/GMT-3",                   "Georgian"},

    /* D (GMT+03:30) Tehran */
    {"Asia/Tehran",                 "Iran"},

    /* D (GMT+04:00) Yerevan */
    {"Asia/Yerevan",                "Armenian"}, 

    /* D (GMT+04:00) Baku */
    {"Asia/Baku",                   "Azerbaijan"}, 

    /* S (GMT+04:00) Abu Dhabi, Muscat */
    {"Asia/Dubai",                  "Arabian"},
    {"Asia/Muscat",                 "Arabian"},

    /* S (GMT+04:00) Caucasus Standard Time */
    {"Asia/Tbilisi",                "Caucasus"},

    /* S (GMT+04:30) Kabul */
    {"Asia/Kabul",                  "Afghanistan"},

    /* D (GMT+05:00) Ekaterinburg */
    {"Asia/Yekaterinburg",          "Ekaterinburg"},

    /* S (GMT+05:00) Islamabad, Karachi, Tashkent */
    {"Asia/Karachi",                "West Asia"},
    {"Asia/Tashkent",               "West Asia"},

    /* S (GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi */
    {"Asia/Calcutta",               "India"}, 

    /* S (GMT+05:30) Sri Jayawardenepura */
    {"Asia/Colombo",                "Sri Lanka"}, 

    /* S (GMT+05:45) Kathmandu */
    {"Asia/Katmandu",               "Nepal"},

    /* D (GMT+06:00) Almaty, Novosibirsk */ /* No DST in Almaty */
    {"Asia/Novosibirsk",            "N. Central Asia"}, 

    /* S (GMT+06:00) Astana, Dhaka */
    {"Asia/Dhaka",                  "Central Asia"},
    {"Asia/Almaty",                 "Central Asia"},
    {"Asia/Qyzylorda",              "Central Asia"},

    /* S (GMT+06:30) Yangon (Rangoon) */
    {"Asia/Rangoon",                "Myanmar"},

    /* D (GMT+07:00) Krasnoyarsk */
    {"Asia/Krasnoyarsk",            "North Asia"},

    /* S (GMT+07:00) Bangkok, Hanoi, Jakarta */
    {"Asia/Bangkok",                "SE Asia"},
    {"Asia/Saigon",                 "SE Asia"},
    {"Asia/Jakarta",                "SE Asia"},

    /* D (GMT+08:00) Irkutsk, Ulaan Bataar */ /* Ulaan Bataar does not use DST */
    {"Asia/Irkutsk",                "North Asia East"},

    /* D (GMT+08:00) Perth */
    {"Australia/Perth",             "W. Australia"},

    /* S (GMT+08:00) Beijing, Chongqing, Hong Kong, Urumqi */
    {"Asia/Shanghai",               "China"},
    {"Asia/Chongqing",              "China"},
    {"Asia/Hong_Kong",              "China"},
    {"Asia/Urumqi",                 "China"},

    /* S (GMT+08:00) Taipei */
    {"Asia/Taipei",                 "Taipei"},

    /* S (GMT+08:00) Kuala Lumpur, Singapore */
    {"Asia/Singapore",              "Singapore"},
    {"Asia/Kuala_Lumpur",           "Singapore"},

    /* D (GMT+09:00) Yakutsk */
    {"Asia/Yakutsk",                "Yakutsk"}, 

    /* S (GMT+09:00) Osaka, Sapporo, Tokyo */
    {"Asia/Tokyo",                  "Tokyo"},

    /* S (GMT+09:00) Seoul */
    {"Asia/Seoul",                  "Korea"},

    /* D (GMT+09:30) Adelaide */
    {"Australia/Adelaide",          "Cen. Australia"},

    /* S (GMT+09:30) Darwin */
    {"Australia/Darwin",            "AUS Central"},

    /* D (GMT+10:00) Canberra, Melbourne, Sydney */
    {"Australia/Sydney",            "AUS Eastern"},
    {"Australia/Melbourne",         "AUS Eastern"},

    /* D (GMT+10:00) Hobart */
    {"Australia/Hobart",            "Tasmania"},

    /* D (GMT+10:00) Vladivostok */
    {"Asia/Vladivostok",            "Vladivostok"},

    /* S (GMT+10:00) Brisbane */
    {"Australia/Brisbane",          "E. Australia"},

    /* S (GMT+10:00) Guam, Port Moresby */
    {"Pacific/Port_Moresby",        "West Pacific"},
    {"Pacific/Guam",                "West Pacific"},

    /* S (GMT+11:00) Magadan, Solomon Is., New Caledonia */ /* Magadan uses DST */
    {"Pacific/Guadalcanal",         "Central Pacific"},
    {"Pacific/Noumea",              "Central Pacific"},

    /* D (GMT+12:00) Auckland, Wellington */
    {"Pacific/Auckland",            "New Zealand"},

    /* S (GMT+12:00) Fiji, Kamchatka, Marshall Is. */
    {"Pacific/Fiji",                "Fiji"},
    {"Pacific/Majuro",              "Fiji"},
    {"Pacific/Kwajalein",           "Fiji"},

    /* S (GMT+13:00) Nuku'alofa */
    {"Pacific/Tongatapu",           "Tonga"},

    NULL,                           NULL
};

/**
 * If a lookup fails, we attempt to remap certain Windows ids to
 * alternate Windows ids.  If the alternate listed here begins with
 * '-', we use it as is (without the '-').  If it begins with '+', we
 * append a " Standard Time" if appropriate.
 */
static const WindowsZoneRemap ZONE_REMAP[] = {
    "Central European",                "-Warsaw",
    "Central Europe",                  "-Prague Bratislava",
    "China",                           "-Beijing",

    "Greenwich",                       "+GMT",
    "GTB",                             "+GFT",
    "Arab",                            "+Saudi Arabia",
    "SE Asia",                         "+Bangkok",
    "AUS Eastern",                     "+Sydney",
    "Mountain Standard Time (Mexico)", "-Mexico Standard Time 2",
    "Central Standard Time (Mexico)",  "+Mexico",
    NULL,                   NULL,
};

static int32_t gWinType = 0;

static int32_t detectWindowsType()
{
    int32_t winType;
    LONG result;
    HKEY hkey;

    /* Detect the version of windows by trying to open a sequence of
        probe keys.  We don't use the OS version API because what we
        really want to know is how the registry is laid out.
        Specifically, is it 9x/Me or not, and is it "GMT" or "GMT
        Standard Time". */
    for (winType = 0; winType < 2; winType++) {
        result = RegOpenKeyExA(HKEY_LOCAL_MACHINE,
                              WIN_TYPE_PROBE_REGKEY[winType],
                              0,
                              KEY_QUERY_VALUE,
                              &hkey);
        RegCloseKey(hkey);

        if (result == ERROR_SUCCESS) {
            break;
        }
    }

    return winType+1; // +1 to bring it inline with the enum
}

/*
 * TODO: Binary search sorted ZONE_MAP...
 * (u_detectWindowsTimeZone() needs them sorted by offset...)
 */
static const char *findWindowsZoneID(const UChar *icuid, int32_t length)
{
    char stackBuffer[ICUID_STACK_BUFFER_SIZE];
    char *buffer = stackBuffer;
    const char *result = NULL;
    int i;

    /*
     * NOTE: >= because length doesn't include
     * trailing null.
     */
    if (length >= ICUID_STACK_BUFFER_SIZE) {
        buffer = NEW_ARRAY(char, length + 1);
    }

    u_UCharsToChars(icuid, buffer, length);
    buffer[length] = '\0';

    for (i = 0; ZONE_MAP[i].icuid != NULL; i += 1) {
        if (uprv_strcmp(buffer, ZONE_MAP[i].icuid) == 0) {
            result = ZONE_MAP[i].winid;
            break;
        }
    }

    if (buffer != stackBuffer) {
        DELETE_ARRAY(buffer);
    }

    return result;
}

static LONG openTZRegKey(HKEY *hkey, const char *winid)
{
    char subKeyName[96]; /* TODO: why 96?? */
    char *name;
    LONG result;

    /* This isn't thread safe, but it's good enough because the result should be constant per system. */
    if (gWinType <= 0) {
        gWinType = detectWindowsType();
    }

    uprv_strcpy(subKeyName, TZ_REGKEY[(gWinType != WIN_9X_ME_TYPE)]);
    name = &subKeyName[strlen(subKeyName)];
    uprv_strcat(subKeyName, winid);

    if (gWinType != WIN_9X_ME_TYPE &&
        (winid[strlen(winid) - 1] != '2') &&
        (winid[strlen(winid) - 1] != ')') &&
        !(gWinType == WIN_NT_TYPE && strcmp(winid, "GMT") == 0))
    {
        uprv_strcat(subKeyName, STANDARD_TIME_REGKEY);
    }

    result = RegOpenKeyExA(HKEY_LOCAL_MACHINE,
                            subKeyName,
                            0,
                            KEY_QUERY_VALUE,
                            hkey);

    if (result != ERROR_SUCCESS) {
        int i;

        /* If the primary lookup fails, try to remap the Windows zone
           ID, according to the remapping table. */
        for (i=0; ZONE_REMAP[i].winid; i++) {
            if (uprv_strcmp(winid, ZONE_REMAP[i].winid) == 0) {
                uprv_strcpy(name, ZONE_REMAP[i].altwinid + 1);
                if (*(ZONE_REMAP[i].altwinid) == '+' && gWinType != WIN_9X_ME_TYPE) {
                    uprv_strcat(subKeyName, STANDARD_TIME_REGKEY);
                }
                return RegOpenKeyExA(HKEY_LOCAL_MACHINE,
                                      subKeyName,
                                      0,
                                      KEY_QUERY_VALUE,
                                      hkey);
            }
        }
    }

    return result;
}

static LONG getTZI(const char *winid, TZI *tzi)
{
    DWORD cbData = sizeof(TZI);
    LONG result;
    HKEY hkey;

    result = openTZRegKey(&hkey, winid);

    if (result == ERROR_SUCCESS) {
        result = RegQueryValueExA(hkey,
                                    TZI_REGKEY,
                                    NULL,
                                    NULL,
                                    (LPBYTE)tzi,
                                    &cbData);

    }

    RegCloseKey(hkey);

    return result;
}

U_CAPI UBool U_EXPORT2
uprv_getWindowsTimeZoneInfo(TIME_ZONE_INFORMATION *zoneInfo, const UChar *icuid, int32_t length)
{
    const char *winid;
    TZI tzi;
    LONG result;

    winid = findWindowsZoneID(icuid, length);

    if (winid != NULL) {
        result = getTZI(winid, &tzi);

        if (result == ERROR_SUCCESS) {
            zoneInfo->Bias         = tzi.bias;
            zoneInfo->DaylightBias = tzi.daylightBias;
            zoneInfo->StandardBias = tzi.standardBias;
            zoneInfo->DaylightDate = tzi.daylightDate;
            zoneInfo->StandardDate = tzi.standardDate;

            return TRUE;
        }
    }

    return FALSE;
}

/*
  This code attempts to detect the Windows time zone, as set in the
  Windows Date and Time control panel.  It attempts to work on
  multiple flavors of Windows (9x, Me, NT, 2000, XP) and on localized
  installs.  It works by directly interrogating the registry and
  comparing the data there with the data returned by the
  GetTimeZoneInformation API, along with some other strategies.  The
  registry contains time zone data under one of two keys (depending on
  the flavor of Windows):

    HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Time Zones\
    HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones\

  Under this key are several subkeys, one for each time zone.  These
  subkeys are named "Pacific" on Win9x/Me and "Pacific Standard Time"
  on WinNT/2k/XP.  There are some other wrinkles; see the code for
  details.  The subkey name is NOT LOCALIZED, allowing us to support
  localized installs.

  Under the subkey are data values.  We care about:

    Std   Standard time display name, localized
    TZI   Binary block of data

  The TZI data is of particular interest.  It contains the offset, two
  more offsets for standard and daylight time, and the start and end
  rules.  This is the same data returned by the GetTimeZoneInformation
  API.  The API may modify the data on the way out, so we have to be
  careful, but essentially we do a binary comparison against the TZI
  blocks of various registry keys.  When we find a match, we know what
  time zone Windows is set to.  Since the registry key is not
  localized, we can then translate the key through a simple table
  lookup into the corresponding ICU time zone.

  This strategy doesn't always work because there are zones which
  share an offset and rules, so more than one TZI block will match.
  For example, both Tokyo and Seoul are at GMT+9 with no DST rules;
  their TZI blocks are identical.  For these cases, we fall back to a
  name lookup.  We attempt to match the display name as stored in the
  registry for the current zone to the display name stored in the
  registry for various Windows zones.  By comparing the registry data
  directly we avoid conversion complications.

  Author: Alan Liu
  Since: ICU 2.6
  Based on original code by Carl Brown <cbrown@xnetinc.com>
*/

/**
 * Main Windows time zone detection function.  Returns the Windows
 * time zone, translated to an ICU time zone, or NULL upon failure.
 */
U_CFUNC const char* U_EXPORT2
uprv_detectWindowsTimeZone() {
    LONG result;
    HKEY hkey;
    TZI tziKey;
    TZI tziReg;
    TIME_ZONE_INFORMATION apiTZI;
    int firstMatch, lastMatch;
    int j;

    /* Obtain TIME_ZONE_INFORMATION from the API, and then convert it
       to TZI.  We could also interrogate the registry directly; we do
       this below if needed. */
    uprv_memset(&apiTZI, 0, sizeof(apiTZI));
    uprv_memset(&tziKey, 0, sizeof(tziKey));
    uprv_memset(&tziReg, 0, sizeof(tziReg));
    GetTimeZoneInformation(&apiTZI);
    tziKey.bias = apiTZI.Bias;
    uprv_memcpy((char *)&tziKey.standardDate, (char*)&apiTZI.StandardDate,
           sizeof(apiTZI.StandardDate));
    uprv_memcpy((char *)&tziKey.daylightDate, (char*)&apiTZI.DaylightDate,
           sizeof(apiTZI.DaylightDate));

    /* For each zone that can be identified by Offset+Rules, see if we
       have a match.  Continue scanning after finding a match,
       recording the index of the first and the last match.  We have
       to do this because some zones are not unique under
       Offset+Rules. */
    firstMatch = -1;
    lastMatch = -1;
    for (j=0; ZONE_MAP[j].icuid; j++) {
        result = getTZI(ZONE_MAP[j].winid, &tziReg);

        if (result == ERROR_SUCCESS) {
            /* Assume that offsets are grouped together, and bail out
               when we've scanned everything with a matching
               offset. */
            if (firstMatch >= 0 && tziKey.bias != tziReg.bias) {
                break;
            }

            /* Windows alters the DaylightBias in some situations.
               Using the bias and the rules suffices, so overwrite
               these unreliable fields. */
            tziKey.standardBias = tziReg.standardBias;
            tziKey.daylightBias = tziReg.daylightBias;

            if (uprv_memcmp((char *)&tziKey, (char*)&tziReg, sizeof(tziKey)) == 0) {
                if (firstMatch < 0) {
                    firstMatch = j;
                }

                lastMatch = j;
            }
        }
    }

    /* This should never happen; if it does it means our table doesn't
       match Windows AT ALL, perhaps because this is post-XP? */
    if (firstMatch < 0) {
        return NULL;
    }

    if (firstMatch != lastMatch) {
        char stdName[32];
        DWORD stdNameSize;
        char stdRegName[64];
        DWORD stdRegNameSize;

        /* Offset+Rules lookup yielded >= 2 matches.  Try to match the
           localized display name.  Get the name from the registry
           (not the API). This avoids conversion issues.  Use the
           standard name, since Windows modifies the daylight name to
           match the standard name if there is no DST. */
        if (RegOpenKeyExA(HKEY_LOCAL_MACHINE,
                              CURRENT_ZONE_REGKEY,
                              0,
                              KEY_QUERY_VALUE,
                              &hkey) == ERROR_SUCCESS)
        {
            stdNameSize = sizeof(stdName);
            result = RegQueryValueExA(hkey,
                                     STANDARD_NAME_REGKEY,
                                     NULL,
                                     NULL,
                                     (LPBYTE)stdName,
                                     &stdNameSize);
            RegCloseKey(hkey);

            /*
             * Scan through the Windows time zone data in the registry
             * again (just the range of zones with matching TZIs) and
             * look for a standard display name match.
             */
            for (j = firstMatch; j <= lastMatch; j += 1) {
                stdRegNameSize = sizeof(stdRegName);
                result = openTZRegKey(&hkey, ZONE_MAP[j].winid);

                if (result == ERROR_SUCCESS) {
                    result = RegQueryValueExA(hkey,
                                             STD_REGKEY,
                                             NULL,
                                             NULL,
                                             (LPBYTE)stdRegName,
                                             &stdRegNameSize);
                }

                RegCloseKey(hkey);

                if (result == ERROR_SUCCESS &&
                    stdRegNameSize == stdNameSize &&
                    uprv_memcmp(stdName, stdRegName, stdNameSize) == 0)
                {
                    firstMatch = j; /* record the match */
                    break;
                }
            }
        } else {
            RegCloseKey(hkey); /* should never get here */
        }
    }

    return ZONE_MAP[firstMatch].icuid;
}

#endif /* #ifdef U_WINDOWS */
