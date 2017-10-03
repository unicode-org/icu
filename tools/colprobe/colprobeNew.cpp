// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File colprobe.cpp
*
* Modification History:
*
*   Date        Name        Description
*   03/18/2003  weiv        Creation.
*******************************************************************************
*/

#include "uoptions.h"
#include "unicode/ucol.h"
#include "unicode/ucoleitr.h"
#include "unicode/ures.h"
#include "unicode/uniset.h"
#include "unicode/usetiter.h"
#include "unicode/ustring.h"
#include "unicode/uchar.h"
#include "unicode/uscript.h"
#include "unicode/locid.h"
#include "unicode/ucnv.h"
#include "uprops.h"
#include "hash.h"
#include "ucol_imp.h"

#include "unicode/ustdio.h"
#include "unicode/utrans.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

// unix tolower
#include <ctype.h>
// unix setlocale
#include <locale.h>

#include "colprobe.h"

#include "line.h"
#include "sortedlines.h"
#include "strengthprobe.h"

void testWin(StrengthProbe &probe, UErrorCode &status) ;

#if defined WIN32
#include <io.h>
#include <windows.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <direct.h>

int createDir(const char* dirName) {
  struct _stat myStat;
  int result = _stat(dirName, &myStat);
 
  if(result == -1) {
    result = _mkdir(dirName);
    return result;
  } else if(myStat.st_mode & _S_IFDIR) {
    return 0;
  } else {
    return 1;
  }
}

//#elif defined POSIX
#else
#include <sys/stat.h>
#include <unistd.h>

int createDir(const char* dirName) {
  struct stat myStat;
  int result = stat(dirName, &myStat);
 
  if(result == -1) {
    result = mkdir(dirName, S_IRUSR|S_IWUSR|S_IXUSR|S_IRGRP|S_IWGRP|S_IXGRP|S_IROTH|S_IWOTH|S_IXOTH);
    return result;
  } else if(S_ISDIR(myStat.st_mode)) {
    return 0;
  } else {
    return 1;
  }
}
//
//  Stubs for Windows API functions when building on UNIXes.
//
typedef int DWORD;
inline int CompareStringW(DWORD, DWORD, UChar *, int, UChar *, int) {return 0;};
//#else
//#error "Not POSIX or Windows. Won't work."
#endif

#include "line.h"

static UBool gVerbose = FALSE;
static UBool gDebug = FALSE;
static UBool gQuiet = FALSE;
static UBool gExemplar = FALSE;

DWORD          gWinLCID;
int            gCount;
UCollator     *gCol;
UCollator     *gUCA;
UConverter    *utf8cnv;
CompareFn gComparer;
int       gRefNum;
UnicodeSet gExcludeSet;
UnicodeSet gRepertoire;

const UChar separatorChar = 0x0030;

UPrinter *logger;
UPrinter *debug;
UPrinter *tailoringBundle;
UPrinter *referenceBundle;
UPrinter *bundle;
FILE     *fTailoringDump;
FILE     *fDefaultDump;

const char *progName = "colprobe";

const char *gLocale = NULL;
int32_t platformIndex = -1;
int32_t gPlatformNo = 0;
int32_t gPlatformIndexes[10];
int32_t gLocaleNo = 0;
const char* gLocales[100];
UBool gRulesStdin = FALSE;
const char *outputFormat = "HTML";
const char *outExtension = "html";

enum {
  HELP1,
    HELP2,
    VERBOSE,
    QUIET,
    VERSION,
    ICUDATADIR,
    COPYRIGHT,
    LOCALE,
    PLATFORM,
    DEBUG, 
    EXEMPLAR,
    RULESSTDIN,
    REFERENCE,
    EXCLUDESET,
    REPERTOIRE,
  INTERACTIVE,
  PRINTREF,
  DIFF, 
  OUTPUT
};

UOption options[]={
  /*0*/ UOPTION_HELP_H,
  /*1*/ UOPTION_HELP_QUESTION_MARK,
  /*2*/ UOPTION_VERBOSE,
  /*3*/ UOPTION_QUIET,
  /*4*/ UOPTION_VERSION,
  /*5*/ UOPTION_ICUDATADIR,
  /*6*/ UOPTION_COPYRIGHT,
  /*7*/ UOPTION_DEF("locale", 'l', UOPT_REQUIRES_ARG),
  /*8*/ UOPTION_DEF("platform", 'p', UOPT_REQUIRES_ARG),
  /*9*/ UOPTION_DEF("debug", 'D', UOPT_NO_ARG),
  /*10*/ UOPTION_DEF("exemplar", 'E', UOPT_NO_ARG),
  /*11*/ UOPTION_DEF("rulesstdin", 'R', UOPT_NO_ARG),
  /*12*/ UOPTION_DEF("ref", 'c', UOPT_REQUIRES_ARG),
  /*13*/ UOPTION_DEF("excludeset", 'x', UOPT_REQUIRES_ARG),
  /*14*/ UOPTION_DEF("repertoire", 't', UOPT_REQUIRES_ARG),
  /*15*/ UOPTION_DEF("interactive", 'I', UOPT_NO_ARG),
  /*16*/ UOPTION_DEF("printref", 0, UOPT_NO_ARG),
  /*17*/ UOPTION_DEF("diff", 0, UOPT_NO_ARG),
  /*18*/ UOPTION_DEF("output", 0, UOPT_REQUIRES_ARG)
};

UChar compA[256];
UChar compB[256];
int32_t compALen = 0;
int32_t compBLen = 0;

char compUTF8A[256];
char compUTF8B[256];
int32_t compUTF8ALen = 0;
int32_t compUTF8BLen = 0;

int UNIXstrcmp(const void *a, const void *b) {
  UErrorCode status = U_ZERO_ERROR;
    gCount++;
    int t;
    compALen = unorm_normalize((*(Line **)a)->name, (*(Line **)a)->len, UNORM_NFC, 0, compA, 256, &status);
    compBLen = unorm_normalize((*(Line **)b)->name, (*(Line **)b)->len, UNORM_NFC, 0, compB, 256, &status);
    compUTF8ALen = ucnv_fromUChars(utf8cnv, compUTF8A, 256, compA, compALen, &status);
    compUTF8A[compUTF8ALen] = 0;
    compUTF8BLen = ucnv_fromUChars(utf8cnv, compUTF8B, 256, compB, compBLen, &status);
    compUTF8B[compUTF8BLen] = 0;
    t = strcoll(compUTF8A, compUTF8B);
    return t;
}

int UNIXgetSortKey(const UChar *string, int32_t len, uint8_t *buffer, int32_t buffCapacity) {
  UErrorCode status = U_ZERO_ERROR;
  compALen = unorm_normalize(string, len, UNORM_NFC, 0, compA, 256, &status);
  compUTF8ALen = ucnv_fromUChars(utf8cnv, compUTF8A, 256, compA, compALen, &status);
  compUTF8A[compUTF8ALen] = 0;
  return (strxfrm((char *)buffer, compUTF8A, buffCapacity)+1);
}

#ifdef WIN32
int Winstrcmp(const void *a, const void *b) {
  UErrorCode status = U_ZERO_ERROR;
    gCount++;
    int t;
    //compALen = unorm_compose(compA, 256, (*(Line **)a)->name, (*(Line **)a)->len, FALSE, 0, &status);
    //compBLen = unorm_compose(compB, 256, (*(Line **)b)->name, (*(Line **)b)->len, FALSE, 0, &status);
    compALen = unorm_normalize((*(Line **)a)->name, (*(Line **)a)->len, UNORM_NFC, 0, compA, 256, &status);
    compBLen = unorm_normalize((*(Line **)b)->name, (*(Line **)b)->len, UNORM_NFC, 0, compB, 256, &status);
    t = CompareStringW(gWinLCID,  SORT_STRINGSORT, //0,
      compA, compALen, 
      compB, compBLen);

/*    
    t = CompareStringW(gWinLCID, 0, 
      (*(Line **)a)->name, (*(Line **)a)->len, 
      (*(Line **)b)->name, (*(Line **)b)->len);
*/
    return t-2;
}

int WingetSortKey(const UChar *string, int32_t len, uint8_t *buffer, int32_t buffCapacity) {
  UErrorCode status = U_ZERO_ERROR;
  compALen = unorm_normalize(string, len, UNORM_NFC, 0, compA, 256, &status);
  return LCMapStringW(gWinLCID, LCMAP_SORTKEY | SORT_STRINGSORT, compA, compALen, (unsigned short *)buffer, buffCapacity);
}

#if 0
int Winstrcmp(const void *a, const void *b) {
  UErrorCode status = U_ZERO_ERROR;
  uint8_t b1[256], b2[256];
  int32_t b1Len, b2Len;
  b1Len = WingetSortKey((*(Line **)a)->name, (*(Line **)a)->len, b1, 256);
  b2Len = WingetSortKey((*(Line **)b)->name, (*(Line **)b)->len, b2, 256);

  b1[b1Len] = 0;
  b2[b2Len] = 0;
  
  return strcmp((const char *)b1, (const char *)b2);
}
#endif

#else
int Winstrcmp(const void *a, const void *b) {
  if(a == b);
  return 0;
}
int WingetSortKey(const UChar *, int32_t , uint8_t *, int32_t ) {
  return 0;
}
#endif

int ICUstrcmp(const void *a, const void *b) {
    gCount++;
    UCollationResult t;
    t = ucol_strcoll(gCol, 
      (*(Line **)a)->name, (*(Line **)a)->len,  
      (*(Line **)b)->name, (*(Line **)b)->len);
    if (t == UCOL_LESS) return -1;
    if (t == UCOL_GREATER) return +1;
    return 0;
}

int ICUgetSortKey(const UChar *string, int32_t len, uint8_t *buffer, int32_t buffCapacity) {
  return ucol_getSortKey(gCol, string, len, buffer, buffCapacity);
}

struct {
  const char* name;
  CompareFn comparer;
  GetSortKeyFn skgetter;
} platforms[] = {
  { "icu", ICUstrcmp, ICUgetSortKey },
  { "w2k", Winstrcmp, WingetSortKey},
  { "winxp", Winstrcmp, WingetSortKey},
  { "aix", UNIXstrcmp, UNIXgetSortKey},
  { "linux", UNIXstrcmp, UNIXgetSortKey}
};


void stringToLower(char *string) {
  uint32_t i = 0;
  for(i = 0; i < strlen(string); i++) {
    string[i] = tolower(string[i]);
  }
}

void usage(const char *name) {
  logger->log("Usage: %s --locale loc_name --platform platform\n", name);
}

void listKnownPlatforms() {
  uint32_t i = 0;
  logger->log("Known platforms:\n");
  for(i = 0; i < sizeof(platforms)/sizeof(platforms[0]); i++) {
    logger->log("\t%s\n", platforms[i]);
  }
}

void addPlatform(const char *platform) {
  uint32_t i;
  //stringToLower(platform);
  int32_t oldPlatformNo = gPlatformNo;

  for(i = 0; i < sizeof(platforms)/sizeof(platforms[0]); i++) {
    if(strcmp(platform, platforms[i].name) == 0) {
      gPlatformIndexes[gPlatformNo++] = i;
    }
  }
  if(gPlatformNo == oldPlatformNo) {
    logger->log("Unknown platform %s\n", platform);
    listKnownPlatforms();
  }
}

void processArgs(int argc, char* argv[], UErrorCode &status)
{
  int32_t i = 0;
  U_MAIN_INIT_ARGS(argc, argv);

  argc = u_parseArgs(argc, argv, (int32_t)(sizeof(options)/sizeof(options[0])), options);

  if(argc < 0) {
    logger->log("Unknown option: %s\n", argv[-argc]);
    usage(progName);
    return;
  }

  if(options[0].doesOccur || options[1].doesOccur) {
    usage(progName);
    return;
  }
  if(options[VERBOSE].doesOccur) {
    gVerbose = TRUE;
  }
  if(options[DEBUG].doesOccur) {
    gDebug = TRUE;
    gVerbose = TRUE;
  }
  if(options[EXEMPLAR].doesOccur) {
    gExemplar = TRUE;
  }
  if(options[QUIET].doesOccur) {
    gQuiet = TRUE;
  }

  // ASCII based options specified on the command line
  // this is for testing purposes, will allow to load
  // up ICU rules and then poke through them.
  // In that case, we test only ICU and don't need 
  // a locale.
  if(options[RULESSTDIN].doesOccur) {
    gRulesStdin = TRUE;
    addPlatform("icu");
    return;
  } 

  if(options[LOCALE].doesOccur) {
    gLocale = options[LOCALE].value;
  } else {
    gLocale = argv[1];
    //for(i = 1; i < argc; i++) {
    //gLocales[gLocaleNo++] = argv[i];
    //}
  }

  if(options[PLATFORM].doesOccur) {
    addPlatform(options[PLATFORM].value);
  } else { // there is a list of platforms 
    addPlatform("icu");
  }

  if(options[REFERENCE].doesOccur) {
    for(i = 0; i < (int32_t)(sizeof(platforms)/sizeof(platforms[0])); i++) {
      if(strcmp(options[REFERENCE].value, platforms[i].name) == 0) {
        gRefNum = i;
        break;
      }
    }
    if(i == sizeof(platforms)/sizeof(platforms[0])) {
      logger->log("Unknown reference %s!\n", options[REFERENCE].value);
      status = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
  } else {
    gRefNum = 0;
  }

  if(options[EXCLUDESET].doesOccur) {
    gExcludeSet.applyPattern(UnicodeString(options[EXCLUDESET].value), status);
    if(U_FAILURE(status)) {
      logger->log("Cannot construct exclude set from argument %s. Error %s\n", options[EXCLUDESET].value, u_errorName(status));
      return;
    } else {
      UnicodeString pattern;
      logger->log(gExcludeSet.toPattern(pattern, TRUE), TRUE);
    }
  }

  if(options[REPERTOIRE].doesOccur)  {
    gRepertoire.applyPattern(UnicodeString(options[REPERTOIRE].value), status);
    if(U_FAILURE(status)) {
      logger->log("Cannot construct repertoire from argument %s. Error %s\n", options[REPERTOIRE].value, u_errorName(status));
      return;
    }
  }

  if(options[OUTPUT].doesOccur) {
    outputFormat = options[OUTPUT].value;
    if(strcmp(outputFormat, "HTML") == 0) {
      outExtension = "html";
    } else if(strcmp(outputFormat, "XML") == 0) {
      outExtension = "xml";
    } else {
      outExtension = "txt";
    }
  }

}

// Check whether upper case comes before lower case or vice-versa
int32_t 
checkCaseOrdering(void) {
  UChar stuff[][3] = {
    { 0x0061, separatorChar, 0x0061}, //"aa",
    { 0x0061, separatorChar, 0x0041 }, //"a\\u00E0",
    { 0x0041, separatorChar, 0x0061 }, //"\\u00E0a",
    { 0x0041, separatorChar, 0x0041 }, //"\\u00E0a",
    //{ 0x00E0, separatorChar, 0x00E0 }  //"\\u00E0\\u00E0"
  };
  const int32_t size = sizeof(stuff)/sizeof(stuff[0]);

  Line **sortedLines = new Line*[size];
  Line lines[size];

  int32_t i = 0;
  int32_t ordered = 0, reversed = 0;

  for(i = 0; i < size; i++) {
    lines[i].setName(stuff[i], 3);
  }
  //setArray(sortedLines, lines, size);
  qsort(sortedLines, size, sizeof(Line*), gComparer);

  for(i = 0; i < size; i++) {
    if(*(sortedLines+i) == &lines[i]) {
      ordered++;
    }
    if(*(sortedLines+i) == &lines[size-i-1]) {
      reversed++;
    }
  }

  delete[] sortedLines;
  if(ordered == size) {
    return 0; // in normal order
  } else if(reversed == size) {
    return 1; // in reversed order
  } else {
    return -1; // unknown order
  }
}

void
getExemplars(const char *locale, UnicodeSet &exemplars, UErrorCode &status) {
  // first we fill out structures with exemplar characters.
  UResourceBundle *res = ures_open(NULL, locale, &status);
  UnicodeString exemplarString = ures_getUnicodeStringByKey(res, "ExemplarCharacters", &status);
  exemplars.clear();
  exemplars.applyPattern(exemplarString, status);
  ures_close(res);
}


void
getFileNames(const char *name, char *tailoringName, char *tailoringDumpName, char *defaultName, char *defaultDumpName, char *diffName) {
  if(tailoringName) {
    strcpy(tailoringName, platforms[gPlatformIndexes[0]].name);
    strcat(tailoringName, "/");
    strcat(tailoringName, name);
    strcat(tailoringName, "_raw.");
    strcat(tailoringName, outExtension);
  }
  if(tailoringDumpName) {
    strcpy(tailoringDumpName, platforms[gPlatformIndexes[0]].name);
    strcat(tailoringDumpName, "/");
    strcat(tailoringDumpName, name);
    strcat(tailoringDumpName, ".dump");
  }

  if(diffName) {
    strcpy(diffName, platforms[gPlatformIndexes[0]].name);
    strcat(diffName, "/");
    strcat(diffName, name);
    strcat(diffName, "_collation.");
    strcat(diffName, outExtension);
  }

  if(defaultName) {
    strcpy(defaultName, platforms[gRefNum].name);
    strcat(defaultName, "/");
    strcat(defaultName, name);
    strcat(defaultName, "_default_raw.");
    strcat(defaultName, outExtension);
  }

  if(defaultDumpName) {
    strcpy(defaultDumpName, platforms[gRefNum].name);
    strcat(defaultDumpName, "/");
    strcat(defaultDumpName, name);
    strcat(defaultDumpName, "_default.dump");
  }
}

void 
setFiles(const char *name, UErrorCode &status) {
  if(U_FAILURE(status)) {
    return;
  }
  int32_t i = 0;
  char tailoringName[256];
  char tailoringDumpName[256];
  char defaultName[256];
  char defaultDumpName[256];
  char diffName[256];

  getFileNames(name, tailoringName, tailoringDumpName, defaultName, defaultDumpName, diffName);
  if(options[PLATFORM].doesOccur && !options[DIFF].doesOccur) {  
    if(createDir(platforms[gPlatformIndexes[0]].name) == 0) {
      tailoringBundle = new UPrinter(tailoringName, "en", "utf-8", NULL, FALSE);
      fTailoringDump = fopen(tailoringDumpName, "wb");
    } else {
      status = U_FILE_ACCESS_ERROR;
      return;
    }
  }

  if(options[REFERENCE].doesOccur && !options[DIFF].doesOccur) {
    if(createDir(platforms[gRefNum].name) == 0) {
      referenceBundle = new UPrinter(defaultName, "en", "utf-8", NULL, FALSE);
      fDefaultDump = fopen(defaultDumpName, "wb");
    } else {
      status = U_FILE_ACCESS_ERROR;
      return;
    }
  }

  if((options[PLATFORM].doesOccur && options[REFERENCE].doesOccur) || options[DIFF].doesOccur) {
    if(createDir(platforms[gPlatformIndexes[0]].name) == 0) {
      bundle = new UPrinter(diffName, "en", "utf-8", NULL, FALSE);
    }
  }
  if(options[DIFF].doesOccur) {
    fTailoringDump = fopen(tailoringDumpName, "rb");
    fDefaultDump = fopen(defaultDumpName, "rb");
  }
}


UErrorCode status = U_ZERO_ERROR;
static UnicodeSet UNASSIGNED(UnicodeString("[:Cn:]"), status);
static UnicodeSet GENERAL_ACCENTS(UnicodeString("[[:block=Combining Diacritical Marks:]-[:Cn:]]"), status);
//static UnicodeSet ASCII_BASE(UnicodeString("[[:ASCII:]-[:L:]-[:N:]]"), status);
static UnicodeSet ASCII_BASE(UnicodeString("[[:ASCII:]]"), status);
static UnicodeSet ALPHABETIC(UnicodeString("[:alphabetic:]"), status);
//static UnicodeSet CONTROL(UnicodeString("[[:control:][\\u0000-\\u002F]]"), status);
static UnicodeSet BMP(UnicodeString("[\\u0000-\\uFFFF]"), status);

static UnicodeSet CONTROL(UnicodeString("[:control:]"), status);

UCollator *
setLocale(const char* locale, UErrorCode &status)
{
  gWinLCID = uloc_getLCID(locale);
  setlocale(LC_COLLATE, locale);

  if(gCol) {
    ucol_close(gCol);
  }
  gCol = ucol_open(locale, &status);
  ucol_setAttribute(gCol, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
  //ucol_setAttribute(col, UCOL_ALTERNATE_HANDLING, UCOL_SHIFTED, &status);
  //ucol_setAttribute(col, UCOL_STRENGTH, UCOL_QUATERNARY, &status);

  return gCol;
}



UCollator *
setReference(UErrorCode &status) 
{
  gWinLCID = uloc_getLCID("en");
  setlocale(LC_COLLATE, "en_US.UTF-8");
  if(gCol) {
    ucol_close(gCol);
  }
  gCol = ucol_open("root", &status);
  ucol_setAttribute(gCol, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
  return gCol;
}

void
processInteractive() {
  char command[256];
  while(fgets(command, 256, stdin)) {

  }
}

UChar probeChars[][4] = {
  { 0x0061, 0x0062, 0x00E1, 0x0041 }, // latin with a-grave
  { 0x0041, 0x0042, 0x00C1, 0x0061 }, // upper first
  { 0x006E, 0x006F, 0x00F1, 0x004E }, // latin with n-tilda
  { 0x004E, 0x004F, 0x00D1, 0x006E }, // upper first
  { 0x0433, 0x0493, 0x0491, 0x0413 }, // Cyrillic
  { 0x0413, 0x0492, 0x0490, 0x0433 }, // upper first
  { 0x3045, 0x3047, 0x3094, 0x3046 }  // Hiragana/Katakana (last resort)

};

void
processCollator(UCollator *col, UErrorCode &status) {
  int32_t i = 0;
  uint32_t j = 0;
  gCol = col;
  UChar ruleString[16384];
  char myLoc[256];

  int32_t ruleStringLength = ucol_getRulesEx(gCol, UCOL_TAILORING_ONLY, ruleString, 16384);
  logger->log(UnicodeString(ruleString, ruleStringLength), TRUE);
  const char *locale = ucol_getLocale(gCol, ULOC_REQUESTED_LOCALE, &status);
  if(locale == NULL) {
    locale = "en";
  }
  strcpy(myLoc, locale);
  UnicodeSet exemplarUSet;
  UnicodeSet RefRepertoire;

  UnicodeSet tailored;

  tailored = *((UnicodeSet *)ucol_getTailoredSet(gCol, &status));
  tailored.removeAll(CONTROL);


  UnicodeString pattern;
  int sanityResult;

  UnicodeSet hanSet;
  UBool hanAppears = FALSE;

  debug->log("\nGenerating order for platform: %s\n", platforms[gPlatformIndexes[0]].name);
  gComparer = platforms[gPlatformIndexes[0]].comparer;

  StrengthProbe probe(platforms[gPlatformIndexes[0]].comparer, platforms[gPlatformIndexes[0]].skgetter, 0x0030, probeChars[0][0], probeChars[0][1], probeChars[0][2], probeChars[0][3]);
  sanityResult = probe.checkSanity();
  j = 0;
  while(sanityResult && j+1 < sizeof(probeChars)/sizeof(probeChars[0])) {
   j++;
   sanityResult =  probe.setProbeChars(probeChars[j][0], probeChars[j][1], probeChars[j][2], probeChars[j][3]);
  }
  if(sanityResult) {
    logger->log("Bad choice of probe characters! Sanity returned %i. Exiting\n", sanityResult, sanityResult);
    return;
  }
  logger->log("Probe chars: %C, %C, %C, %C\n", probeChars[j][0], probeChars[j][1], probeChars[j][2], probeChars[j][3]); 

  debug->off();

  if(gRepertoire.size()) {
    exemplarUSet = gRepertoire;
  } else {
    generateRepertoire(locale, exemplarUSet, hanAppears, status);
  }
  exemplarUSet.addAll(tailored);
  hanSet.applyIntPropertyValue(UCHAR_SCRIPT, USCRIPT_HAN, status);
  exemplarUSet.removeAll(hanSet);
  
  logger->log(exemplarUSet.toPattern(pattern, TRUE), TRUE);

  exemplarUSet = flatten(exemplarUSet, status);
  logger->log(exemplarUSet.toPattern(pattern, TRUE), TRUE);

  if(!options[PRINTREF].doesOccur) {

    logger->log("\n*** Detecting ordering for the locale\n\n");

    debug->on();
    SortedLines lines(exemplarUSet, gExcludeSet, probe, logger, debug);
    lines.analyse(status);
    lines.calculateSortKeys();
    debug->log("\n*** Final order\n\n");
    debug->log(lines.toPrettyString(TRUE, TRUE), TRUE);
    lines.toFile(fTailoringDump, TRUE, status);
    tailoringBundle->log(lines.toOutput(outputFormat, myLoc, platforms[gPlatformIndexes[0]].name, NULL, TRUE, TRUE, hanAppears), TRUE);
    //debug->off();

    if(options[REFERENCE].doesOccur) {
      status = U_ZERO_ERROR;
      lines.getRepertoire(RefRepertoire);
      setReference(status);

      logger->log(exemplarUSet.toPattern(pattern, TRUE), TRUE);
      logger->log(RefRepertoire.toPattern(pattern, TRUE), TRUE);

      StrengthProbe RefProbe(platforms[gRefNum].comparer, platforms[gRefNum].skgetter);
      logger->log("\n*** Detecting ordering for reference\n\n");
      SortedLines RefLines(exemplarUSet, gExcludeSet, RefProbe, logger, debug);
      RefLines.analyse(status);
      referenceBundle->log(RefLines.toOutput(outputFormat, myLoc, platforms[gRefNum].name, NULL, TRUE, TRUE, FALSE), TRUE);
      RefLines.toFile(fDefaultDump, TRUE, status);

      lines.reduceDifference(RefLines);
      logger->log("\n*** Final rules\n\n");
      logger->log(lines.toPrettyString(TRUE), TRUE);
      bundle->log(lines.toOutput(outputFormat, myLoc, platforms[gPlatformIndexes[0]].name, platforms[gRefNum].name, TRUE, TRUE, hanAppears), TRUE);
    }
  } else {
    setReference(status);
    StrengthProbe RefProbe(platforms[gRefNum].comparer, platforms[gRefNum].skgetter);
    logger->log("\n*** Detecting ordering for reference\n\n");
    SortedLines RefLines(exemplarUSet, gExcludeSet, RefProbe, logger, debug);
    RefLines.analyse(status);
    logger->log(RefLines.toPrettyString(TRUE), TRUE);
    referenceBundle->log(RefLines.toOutput(outputFormat, myLoc, platforms[gRefNum].name, NULL, TRUE, TRUE, FALSE), TRUE);
  }
  if(hanAppears) {
    // there are Han characters. This is a huge block. The best we can do is to just sort it, compare to empty
    // and spit it out. Anything else would be a suicide (actually is - kernel just kills you :)
    logger->log("\n*** Detecting order for Han\n");
    debug->off();
    setLocale(gLocale, status);
    exemplarUSet.clear();
    exemplarUSet.applyIntPropertyValue(UCHAR_SCRIPT, USCRIPT_HAN, status);
    exemplarUSet = flatten(exemplarUSet, status);
    SortedLines han(exemplarUSet, gExcludeSet, probe, logger, debug);
    han.sort(TRUE, TRUE);
    han.classifyRepertoire();
    han.getBounds(status);
    tailoringBundle->log("Han ordering:<br>\n");
    tailoringBundle->log(han.toOutput(outputFormat, myLoc, platforms[gPlatformIndexes[0]].name, NULL, TRUE, FALSE, FALSE), TRUE);
    bundle->log(han.toOutput(outputFormat, myLoc, platforms[gPlatformIndexes[0]].name, NULL, TRUE, FALSE, FALSE), TRUE);
  }
  ucol_close(gCol);
}

void
processLocale(const char *locale, UErrorCode &status) {
  setLocale(locale, status);
  setFiles(locale, status);
  if(U_FAILURE(status)) {
    return;
  }

  debug->log("Locale %s (LCID:%06X, unix:%s)\n", locale, gWinLCID, setlocale(LC_COLLATE, NULL));
  tailoringBundle->log("// Ordering for locale %s (LCID:%06X, unix:%s), platform %s reference %s<br>\n", 
    locale, gWinLCID, setlocale(LC_COLLATE, NULL), 
    platforms[gPlatformIndexes[0]].name, platforms[gRefNum].name);
  if(options[REFERENCE].doesOccur) {
    referenceBundle->log("// Reference for locale %s (LCID:%06X, unix:%s), platform %s reference %s<br>\n", 
      locale, gWinLCID, setlocale(LC_COLLATE, NULL), 
      platforms[gPlatformIndexes[0]].name, platforms[gRefNum].name);
  }


  processCollator(gCol, status);
}



UBool 
hasCollationElements(const char *locName) {

  UErrorCode status = U_ZERO_ERROR;
  UResourceBundle *ColEl = NULL;

  UResourceBundle *loc = ures_open(NULL, locName, &status);;

  if(U_SUCCESS(status)) {
    status = U_ZERO_ERROR;
    ColEl = ures_getByKey(loc, "CollationElements", ColEl, &status);
    if(status == U_ZERO_ERROR) { /* do the test - there are real elements */
      ures_close(ColEl);
      ures_close(loc);
      return TRUE;
    }
    ures_close(ColEl);
    ures_close(loc);
  }
  return FALSE;
}

int
main(int argc,
     char* argv[])
{
  UErrorCode status = U_ZERO_ERROR;
  logger = new UPrinter(stdout, "en", "latin-1");
  debug =  new UPrinter(stderr, "en", "latin-1");

/*
  USet *wsp = uprv_openRuleWhiteSpaceSet(&status);
  uset_add(wsp, 0x0041);
  uset_remove(wsp, 0x0041);
  UnicodeString pat;
  ((UnicodeSet *)wsp)->toPattern(pat, TRUE);
  pat.setCharAt(pat.length(), 0);
  escapeString(pat.getBuffer(), pat.length(), log);
  u_fflush(log);
*/

  processArgs(argc, argv, status);
  int32_t i = 0;



  if(U_FAILURE(status) || gPlatformNo == 0) {
    return -1;
  }

  utf8cnv = ucnv_open("utf-8", &status);    // we are just doing UTF-8 locales for now.
  gUCA = ucol_open("root", &status);

  if(options[INTERACTIVE].doesOccur) {
    processInteractive();
  } else {
    if(gRulesStdin) {
      char buffer[1024];
      UChar ruleBuffer[16384];
      UChar *rules = ruleBuffer;
      int32_t maxRuleLen = 16384;
      int32_t rLen = 0;
      while(fgets(buffer, 1024, stdin)) {
        if(buffer[0] != '/' && buffer[1] != '/') {
          rLen = u_unescape(buffer, rules, maxRuleLen);
          rules += rLen;
          maxRuleLen -= rLen;
        }
      }
      UParseError parseError;
      //escapeString(ruleBuffer, rules-ruleBuffer, log);//
      debug->log("%U\n", ruleBuffer);

      UCollator *col = ucol_openRules(ruleBuffer, rules-ruleBuffer, UCOL_DEFAULT, UCOL_DEFAULT, &parseError, &status);
      if(U_SUCCESS(status)) {
        setFiles("stdinRules", status);
        processCollator(col, status);
      } else {
        logger->log("Error %s\n", u_errorName(status));
      }
    } else if(options[DIFF].doesOccur) {
      logger->log("Diffing two dumps\n");
      // must have locale, platform and ref in order to be 
      // able to find dump files.
      setFiles(gLocale, status);
  
      if(fTailoringDump && fDefaultDump) {
	    SortedLines tailoring(fTailoringDump, logger, debug, status);
	    logger->log(tailoring.toString(TRUE), TRUE);
	    SortedLines reference(fDefaultDump, logger, debug, status);
	    logger->log(reference.toString(TRUE), TRUE);
	    tailoring.reduceDifference(reference);
	    logger->log("\n*** Final rules\n\n");
	    logger->log(tailoring.toPrettyString(TRUE), TRUE);
	    //result->log(lines.toPrettyString(TRUE), TRUE);
	    bundle->log(tailoring.toOutput(outputFormat, gLocale, platforms[gPlatformIndexes[0]].name, platforms[gRefNum].name, TRUE, TRUE, FALSE), TRUE);
      }

    } else {
      if(gLocale) {
        processLocale(gLocale, status);
      } else if(gLocaleNo) {
        for(i = 0; i < gLocaleNo; i++) {
          processLocale(gLocales[i], status);
        }
      } else { // do the loop through all the locales
        int32_t noOfLoc = uloc_countAvailable();
        const char *locName = NULL;
        for(i = 0; i<noOfLoc; i++) {
          status = U_ZERO_ERROR;
          locName = uloc_getAvailable(i);
          if(hasCollationElements(locName)) {
            processLocale(locName, status);
          }
        }
      }
    }
  }


  ucol_close(gUCA);
  ucnv_close(utf8cnv);

  delete logger;
  delete debug;
  if(tailoringBundle) {
    delete tailoringBundle;
  }
  if(referenceBundle) {
    delete referenceBundle;
  }
  if(bundle) {
    delete bundle;
  }
  if(fTailoringDump) {
    fclose(fTailoringDump);
  }
  if(fDefaultDump) {
    fclose(fDefaultDump);
  }
  return 0;
}


UnicodeString propertyAndValueName(UProperty prop, int32_t i) {
  UnicodeString result;
  result.append(u_getPropertyName(prop, U_LONG_PROPERTY_NAME));
  result.append("=");
  result.append(u_getPropertyValueName(prop, i, U_LONG_PROPERTY_NAME));

    //+ "(" + prop + "," + i + ") ";
  return result;
}


void generateRepertoire(const char *locale, UnicodeSet &rep, UBool &hanAppears, UErrorCode &status) {
    UnicodeString dispName;
    debug->log("Getting repertoire for %s\n", locale);
    tailoringBundle->log("// Scripts in repertoire: ");
    if(options[REFERENCE].doesOccur) {
      referenceBundle->log("// Scripts in repertoire: ");
    }
	rep.clear();
    UnicodeSet delta;
    
    UScriptCode script[256];
    int32_t i = 0;
    // now add the scripts for the locale
    UProperty prop = UCHAR_SCRIPT;
	int32_t scriptLength = uscript_getCode(locale, script, 256, &status);
    if(scriptLength) {
	  for (i = 0; i < scriptLength; ++i) {
        if(script[i] == USCRIPT_HAN) {
          hanAppears = TRUE;
          continue;
        }
        delta.applyIntPropertyValue(prop, script[i], status);
        debug->log("Adding ");
        debug->log(propertyAndValueName(prop, script[i]), TRUE);
        tailoringBundle->log("// ");
        tailoringBundle->log(propertyAndValueName(prop, script[i]), TRUE);
        if(options[REFERENCE].doesOccur) {
          referenceBundle->log("// ");
          referenceBundle->log(propertyAndValueName(prop, script[i]), TRUE);
        }
		rep.addAll(delta);
	  }
    } else {
      delta.applyIntPropertyValue(UCHAR_SCRIPT, USCRIPT_LATIN, status);
      rep.addAll(delta);
    }
    
    // now see which blocks those overlap, and add
    prop = UCHAR_BLOCK;
    int32_t min = u_getIntPropertyMinValue(prop);
    int32_t max = u_getIntPropertyMaxValue(prop);
    UnicodeSet checkDelta;
    for (i = min; i <= max; ++i) {
        // skip certain blocks
        const char *name = u_getPropertyValueName(prop, i, U_LONG_PROPERTY_NAME);
        if (strcmp(name, "Superscripts_and_Subscripts") == 0
        || strcmp(name, "Letterlike_Symbols") == 0
        || strcmp(name, "Alphabetic_Presentation_Forms") == 0
        || strcmp(name, "Halfwidth_and_Fullwidth_Forms") == 0) continue;

        delta.applyIntPropertyValue(prop, i, status).removeAll(UNASSIGNED);
        if (!rep.containsSome(delta)) continue;
        if (rep.containsAll(delta)) continue; // just to see what we are adding
        debug->log("Adding ");
        debug->log(propertyAndValueName(prop, i), TRUE);                
        tailoringBundle->log("// ");
        tailoringBundle->log(propertyAndValueName(prop, i), TRUE);
        if(options[REFERENCE].doesOccur) {
          referenceBundle->log("// ");
          referenceBundle->log(propertyAndValueName(prop, i), TRUE);
        }
        rep.addAll(delta);
    }
    
    // add ASCII and general accents
    rep.addAll(GENERAL_ACCENTS).addAll(ASCII_BASE);
    rep.removeAll(CONTROL);
    //delta.applyIntPropertyValue(UCHAR_SCRIPT, USCRIPT_HAN, status);
    //rep.removeAll(delta);

    // now add the exemplar characters
    // can't get at them from Java right now
    tailoringBundle->log("<br>\n");
    if(options[REFERENCE].doesOccur) {
      referenceBundle->log("<br>\n");
    }
}

UnicodeSet flatten(const UnicodeSet &source, UErrorCode &status) {
    UnicodeSet result;
    UnicodeSetIterator it(source);
    UnicodeString item, itemNFKD, toNormalize;
    while (it.next()) {
        // would be nicer if UnicodeSetIterator had a getString function
        if (it.isString()) {
          Normalizer::normalize(it.getString(), UNORM_NFD, 0, item, status);
          Normalizer::normalize(it.getString(), UNORM_NFKD, 0, itemNFKD, status);
        } else {
          toNormalize.setTo(it.getCodepoint());
          Normalizer::normalize(toNormalize, UNORM_NFD, 0, item, status);
          Normalizer::normalize(toNormalize, UNORM_NFKD, 0, itemNFKD, status);
        }
        result.addAll(item);
        result.addAll(itemNFKD);
    }
    return result;
}


void testWin(StrengthProbe &probe, UErrorCode &status) 
{
  UnicodeSet trailings(UnicodeString("[\\uFE7D\\uFE7C\\u30FD\\uFF70\\u30FC\\u309D\\u3032\\u3031\\u3005\\u0651]"), status);
  char intChar[] = "\\uFE7D\\uFE7C\\u30FD\\uFF70\\u30FC\\u309D\\u3032\\u3031\\u3005\\u0651";
  UChar interesting[256];
  int32_t intLen = u_unescape(intChar, interesting, 256);
  UChar i = 0;
  UChar j = 0,  k = 0;
  int32_t count;
  Line myCh, combo, trial, inter, kLine;
  for(i = 0; i < intLen; i++) {
    inter.setTo(interesting[i]);
    logger->log(inter.toString(TRUE), TRUE);
    logger->log("----------------------\n");
    for(j = 0; j < 0xFFFF; j++) {
      myCh.setTo(j);
      if(probe.distanceFromEmptyString(myCh) == UCOL_IDENTICAL) {
        continue;
      }
      logger->log(myCh.toString(TRUE));
      combo.setTo(j);
      combo.append(interesting[i]);
      count = 0;
      for(k = 0; k < 0xFFFF; k++) {
        kLine.setTo(k);
        trial.setTo(j);
        trial.append(k);
        if(probe.compare(kLine, inter) < 0) {
          if(probe.compare(trial, combo) >= 0) {
            count++;
          }
        }
      }
      logger->log("%i %i\n", count, count);
    }
  }
}

