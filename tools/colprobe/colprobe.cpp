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
#include "uprops.h"
#include "hash.h"
#include "ucol_imp.h"

#include "unicode/ustdio.h"
#include "unicode/utrans.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <io.h>
#include <fcntl.h>

#include "colprobe.h"


#ifdef WIN32
#include <windows.h>
#else
//
//  Stubs for Windows API functions when building on UNIXes.
//
typedef int DWORD;
inline int CompareStringW(DWORD, DWORD, char16_t *, int, char16_t *, int) {return 0;};
#include <sys/time.h>
unsigned long timeGetTime() {
    struct timeval t;
    gettimeofday(&t, 0);
    unsigned long val = t.tv_sec * 1000;  // Let it overflow.  Who cares.
    val += t.tv_usec / 1000;
    return val;
};
inline int LCMapStringW(DWORD, DWORD, char16_t *, int, char16_t *, int) {return 0;};
const int LCMAP_SORTKEY = 0;
#define MAKELCID(a,b) 0
const int SORT_DEFAULT = 0;
#endif

#include "line.h"

static UBool gVerbose = false;
static UBool gDebug = false;
static UBool gQuiet = false;
static UBool gExemplar = false;

DWORD          gWinLCID;
int            gCount;
Line          **gICULines;
UCollator     *gCol;
UCollator     *gUCA;
Line          source;
Line          target;
Line          *gSource = &source;
Line          *gTarget = &target;
Hashtable     gElements(false);
Hashtable     gExpansions(false);
CompareFn gComparer;

const char16_t separatorChar = 0x0030;

UFILE *out = nullptr;
UFILE *err = nullptr;
UFILE *log = nullptr; 

const char *progName = "colprobe";

const char *gLocale = nullptr;
//char platform[256];
int32_t platformIndex = -1;
int32_t gPlatformNo = 0;
int32_t gPlatformIndexes[10];
int32_t gLocaleNo = 0;
const char* gLocales[100];
UBool gRulesStdin = false;

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
    RULESSTDIN
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
  /*11*/ UOPTION_DEF("rulesstdin", 'R', UOPT_NO_ARG)
};

int Winstrcmp(const void *a, const void *b) {
    gCount++;
    int t;
    t = CompareStringW(gWinLCID, 0, 
      (*(Line **)a)->name, (*(Line **)a)->len, 
      (*(Line **)b)->name, (*(Line **)b)->len);
    return t-2;
}

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

struct {
  const char* name;
  CompareFn comparer;
} platforms[] = {
  { "icu", ICUstrcmp },
  { "win", Winstrcmp}
};


void deleteLineElement(void *line) {
  delete((Line *)line);
}

void stringToLower(char *string) {
  uint32_t i = 0;
  for(i = 0; i < strlen(string); i++) {
    string[i] = tolower(string[i]);
  }
}

void usage(const char *name) {
  u_fprintf(out, "Usage: %s --locale loc_name --platform platform\n", name);
}

void listKnownPlatforms() {
  int32_t i = 0;
  u_fprintf(err, "Known platforms:\n");
  for(i = 0; i < sizeof(platforms)/sizeof(platforms[0]); i++) {
    u_fprintf(err, "\t%s\n", platforms[i]);
  }
}

void addPlatform(const char *platform) {
  int32_t i;
  //stringToLower(platform);
  int32_t oldPlatformNo = gPlatformNo;

  for(i = 0; i < sizeof(platforms)/sizeof(platforms[0]); i++) {
    if(strcmp(platform, platforms[i].name) == 0) {
      gPlatformIndexes[gPlatformNo++] = i;
    }
  }
  if(gPlatformNo == oldPlatformNo) {
    u_fprintf(err, "Unknown platform %s\n", platform);
    listKnownPlatforms();
  }
}

void processArgs(int argc, char* argv[], UErrorCode &status)
{
  int32_t i = 0;
  U_MAIN_INIT_ARGS(argc, argv);

  argc = u_parseArgs(argc, argv, (int32_t)(sizeof(options)/sizeof(options[0])), options);

  if(argc < 0) {
    u_fprintf(err, "Unknown option: %s\n", argv[-argc]);
    usage(progName);
    return;
  }

  if(options[0].doesOccur || options[1].doesOccur) {
    usage(progName);
    return;
  }
  if(options[VERBOSE].doesOccur) {
    gVerbose = true;
  }
  if(options[DEBUG].doesOccur) {
    gDebug = true;
    gVerbose = true;
  }
  if(options[EXEMPLAR].doesOccur) {
    gExemplar = true;
  }
  if(options[QUIET].doesOccur) {
    gQuiet = true;
  }
/*
  for(i = 8; i < 9; i++) {
    if(!options[i].doesOccur) {
      u_fprintf(err, "Option %s is required!\n", options[i].longName);
      usage(progName);
      status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(options[i].value == nullptr) {
      u_fprintf(err, "Option %s needs an argument!\n", options[i].longName);
      usage(progName);
      status = U_ILLEGAL_ARGUMENT_ERROR;
    }
  }
*/
  // ASCII based options specified on the command line
  // this is for testing purposes, will allow to load
  // up ICU rules and then poke through them.
  // In that case, we test only ICU and don't need 
  // a locale.
  if(options[RULESSTDIN].doesOccur) {
    gRulesStdin = true;
    addPlatform("icu");
    return;
  } 

  if(options[LOCALE].doesOccur) {
    gLocale = options[LOCALE].value;
  } else {
    for(i = 1; i < argc; i++) {
      gLocales[gLocaleNo++] = argv[i];
    }
  }
  if(options[PLATFORM].doesOccur) {
    //strcpy(platform, options[PLATFORM].value);
    //addPlatform("icu");
    addPlatform(options[PLATFORM].value);
  } else { // there is a list of platforms 
    u_fprintf(err, "Option %s is required!\n", options[i].longName);
    usage(progName);
    status = U_ILLEGAL_ARGUMENT_ERROR;
  }

  //
  //  Set up a Windows LCID
  //
  gWinLCID = uloc_getLCID(gLocale);
  /*
  if (gLocale != 0) {
      gWinLCID = MAKELCID(gLocale, SORT_DEFAULT);
  }
  else {
      gWinLCID = uloc_getLCID(gLocale);
  }
  */

}

void printRules(const char16_t *name, int32_t len, UFILE *file) {
  // very rudimentary pretty rules print
  int32_t i = 0;
  char16_t toPrint[16384];
  int32_t toPrintIndex = 0;
  for(i = 0; i < len; i++) {
    if(name[i] == 0x0026) {
      if(toPrintIndex) {
        toPrint[toPrintIndex] = 0;
        u_fprintf(file, "%U\n", toPrint);
        toPrintIndex = 0;
        toPrint[toPrintIndex++] = name[i];
      } else {
        toPrint[toPrintIndex++] = name[i];
      } 
    } else {
      toPrint[toPrintIndex++] = name[i];
    }
  }
  if(toPrintIndex) {
    toPrint[toPrintIndex] = 0;
    u_fprintf(file, "%U\n", toPrint);
    toPrintIndex = 0;
  }


}

void escapeString(const char16_t *name, int32_t len, UFILE *file) {
  u_fprintf(file, "%U", name);
/*
  int32_t j = 0;
  for(j = 0; j < len; j++) {
    if(name[j] >= 0x20 && name[j] < 0x80) {
      u_fprintf(file, "%c", name[j]);
    } else {
      u_fprintf(file, "\\u%04X", name[j]);
    }
  }
*/
}
void escapeALine(Line *line, UFILE *file) {
  escapeString(line->name, line->len, file);
}

void escapeExpansion(Line *line, UFILE *file) {
  escapeString(line->expansionString, line->expLen, file);
}

void showNames(Line *line, UFILE *file) {
  UErrorCode status = U_ZERO_ERROR;
  int32_t j = 0;
  char charName[256];
  for(j = 0; j < line->len; j++) {
    u_charName(line->name[j], U_EXTENDED_CHAR_NAME, charName, 256, &status);
    u_fprintf(file, "%s ", charName);
  }
}

void setArray(Line **array, Line *contents, int32_t size) {
  int32_t i = 0;
  for(i = 0; i < size; i++) {
    array[i] = contents+i;
  }
}

// set an array from a Hashtable
int32_t 
setArray(Line **array, Hashtable *table = &gElements) {
  int32_t size = table->count();
  int32_t hashIndex = -1;
  const UHashElement *hashElement = nullptr;
  int32_t count = 0;
  while((hashElement = table->nextElement(hashIndex)) != nullptr) {
    array[count++] = (Line *)hashElement->value.pointer;
  }
  return size;
}

UBool trySwamped(Line **smaller, Line **greater, char16_t chars[2], CompareFn comparer) {
  u_strcpy(gSource->name, (*smaller)->name);
  gSource->name[(*smaller)->len] = separatorChar;
  gSource->name[(*smaller)->len+1] = chars[0];
  gSource->name[(*smaller)->len+2] = 0;
  gSource->len = (*smaller)->len+2;

  u_strcpy(gTarget->name, (*greater)->name);
  gTarget->name[(*greater)->len] = separatorChar;
  gTarget->name[(*greater)->len+1] = chars[1];
  gTarget->name[(*greater)->len+2] = 0;
  gTarget->len = (*greater)->len+2;

  if(comparer(&gSource, &gTarget) > 0) {
    return true;
  } else {
    return false;
  }
}

UBool trySwamps(Line **smaller, Line **greater, char16_t chars[2], CompareFn comparer) {
  gSource->name[0] = chars[0];
  gSource->name[1] = separatorChar;
  u_strcpy(gSource->name+2, (*smaller)->name);
  gSource->len = (*smaller)->len+2;

  gTarget->name[0] = chars[1];
  gTarget->name[1] = separatorChar;
  u_strcpy(gTarget->name+2, (*greater)->name);
  gTarget->len = (*greater)->len+2;

  if(comparer(&gSource, &gTarget) < 0) {
    return true;
  } else {
    return false;
  }
}

UColAttributeValue 
probeStrength(Line** prevLine, Line **currLine, CompareFn comparer) {
  // Primary swamps secondary
  // have pairs where [0] 2> [1]
  char16_t primSwamps[][2] = {
    { 0x00E0, 0x0061 },
    { 0x0450, 0x0435 },
    { 0x31a3, 0x310d }
  };
  // Secondary swamps tertiary
  // have pairs where [0] 3> [1]
  char16_t secSwamps[][2] = {
    { 0x0053, 0x0073 },
    { 0x0415, 0x0435 },
    { 0x31b6, 0x310e }
  };
  // Secondary is swamped by primary
  // have pairs where [0] 1> [1]
  char16_t secSwamped[][2] = {
    { 0x0062, 0x0061 },
    { 0x0436, 0x0454 },
    { 0x310e, 0x310d }
  };
  // Tertiary is swamped by secondary
  // have pairs where [0] 2> [1]
  char16_t terSwamped[][2] = {
    { 0x00E0, 0x0061 },
    { 0x0450, 0x0435 },
    { 0x31a3, 0x310d }
  };
  int32_t i = 0;
  // Tertiary swamps equal?
  int result = 0;
  // Choose the pair
  i = 0;
  /*
  if((*prevLine)->name[0] > 0xFF && (*currLine)->name[0] > 0xFF) {
    i = 0;
  } else if((*prevLine)->name[0] < 0x0400 && (*currLine)->name[0] < 0x0400) {
    i = 1;
  } else {
    i = 2;
  }
  */
  // are they equal?
  if((result = comparer(prevLine, currLine)) == 0) {
    return UCOL_IDENTICAL;
  } else if(result > 0) {
    //fprintf(stderr, "lines should be ordered!");
    return UCOL_OFF;
  } else if(trySwamps(prevLine, currLine, primSwamps[i], comparer)) {
    return UCOL_PRIMARY;
  } else if(trySwamps(prevLine, currLine, secSwamps[i], comparer)) {
    return UCOL_SECONDARY;
  } else if(trySwamped(prevLine, currLine, terSwamped[i], comparer)) {
    // is there a tertiary difference
    return UCOL_TERTIARY;
  } else {
    //fprintf(stderr, "Unknown strength!\n");
    return UCOL_ON;
  }
}

// This function tries to probe the set of lines 
// (already sorted by qsort) and deduct the strengths
void 
analyzeStrength(Line **lines, int32_t size, CompareFn comparer) {
  int32_t i = 0;

  for(i = 1; i < size; i++) {
    Line **prevLine = lines+i-1;
    Line **currLine = lines+i;
    (*currLine)->strength = probeStrength(prevLine, currLine, comparer);
    (*currLine)->sortedIndex = i;
    (*currLine)->previous = *prevLine;
    (*prevLine)->next = *currLine;

  }

}

void printStrength(UColAttributeValue strength, UFILE *file) {
  u_fprintf(file, " ");
  switch(strength) {
  case UCOL_IDENTICAL:
    u_fprintf(file, "=");
    break;
  case UCOL_TERTIARY:
    //u_fprintf(file, "<3");
    u_fprintf(file, "<<<");
    break;
  case UCOL_SECONDARY:
    //u_fprintf(file, "<2");
    u_fprintf(file, "<<");
    break;
  case UCOL_PRIMARY:
    //u_fprintf(file, "<1");
    u_fprintf(file, "<");
    break;
  case UCOL_OFF:
    u_fprintf(file, ">?");
  default:
    u_fprintf(file, "?!");
    break;
  }
  u_fprintf(file, " ");
}

void printStrength(Line *line, UFILE *file) {
  printStrength(line->strength, file);
}

void printLine(Line *line, UFILE *file) {
  escapeALine(line, file);
  if(line->isExpansion) {
    u_fprintf(file, "/");
    escapeExpansion(line, file);
  }
}

void printOrdering(Line **lines, int32_t size, UFILE *file, UBool useLinks = false) {
  int32_t i = 0;

  //printLine(*lines);
  //escapeALine(*lines); // Print first line

  Line *line = nullptr;
  Line *previous = *lines;
  if(previous->isReset) {
    u_fprintf(file, "\n& ");
    escapeALine(previous, file);
  } else if(!previous->isRemoved) {
    printLine(previous, file);
  }
  i = 1;
  while(i < size && previous->next) {
    if(useLinks) {
      line = previous->next;
    } else {
      line = *(lines+i);
    }
    if(line->isReset) {
      u_fprintf(file, "\n& ");
      escapeALine(line, file);
    } else if(!line->isRemoved) {
      if(file == out) {
        u_fprintf(file, "\n");
      }
      if(i > 0) {
        printStrength(line, file);
      }
      printLine(line, file);
      //escapeALine(line, file);
    }
    previous = line;
    i++;
  }
  u_fprintf(file, "\n");
}


void setIndexes(Line **lines, int32_t size) {
  int32_t i = 0;
  (*lines)->sortedIndex = 0;
  for(i = 1; i < size; i++) {
    Line *line = *(lines+i);
    Line *prev = *(lines+i-1);
    line->previous = prev;
    prev->next = line;
    line->sortedIndex = i;
  }
}


// this seems to be a dead end
void
noteExpansion(Line **gLines, Line *line, int32_t size, CompareFn comparer) {
  UErrorCode status = U_ZERO_ERROR;

  UnicodeString key(line->name, line->len);
  //Line *toInsert = (Line *)gElements.get(key);
  Line *toInsert = (Line *)gExpansions.get(key);
  if(toInsert != nullptr) {
    toInsert->isExpansion = true;
    u_strcpy(toInsert->expansionString, line->expansionString);
    toInsert->expLen = line->expLen;
    toInsert->previous->next = toInsert->next;
    toInsert->next->previous = toInsert->previous;
    gElements.remove(key);
  } else {
    toInsert = new Line(*line); 
    toInsert->isExpansion = true;
    gElements.put(UnicodeString(toInsert->name, toInsert->len), toInsert, status);
  }

  int32_t i = 0;
  Line testLine;
  Line *l = &testLine;
  for(i = 0; i < size; i++) {
    u_strcpy(testLine.name, (*(gLines+i))->name);
    u_strcat(testLine.name, line->expansionString);
    testLine.len = (*(gLines+i))->len + line->expLen;
    if(comparer(&l, &line) > 0) {
      toInsert->previous = *(gLines+i-1);
      toInsert->next = *(gLines+i);
      toInsert->previous->next = toInsert;
      toInsert->next->previous = toInsert;
      break;
    }
  }
  if(gVerbose) {
    u_fprintf(log, "Adding expansion\n");
    escapeALine(line, log);
    u_fprintf(log, "/");
    escapeExpansion(line, log);
    u_fprintf(log, " ");
  }
}

void
positionExpansions(Line **gLines, int32_t size, CompareFn comparer) {
  int result = 0;
  Line *line = nullptr;
  Line *toMove = nullptr;
  int32_t i = 0, j = 0;
  Line **sortedExpansions = new Line*[gExpansions.count()];
  int32_t sortedExpansionsSize = setArray(sortedExpansions, &gExpansions);
  qsort(sortedExpansions, sortedExpansionsSize, sizeof(Line *), comparer);
  // Make a list of things in the vincinity of expansion candidate
  for(j = 0; j < sortedExpansionsSize; j++) {
    line = *(sortedExpansions+j);
    UnicodeString key(line->name, line->len);
    toMove = (Line *)gElements.get(key);
    int32_t i = 0;
    Line testLine, prevTestLine;
    Line *l = &testLine;
    Line *prevL = &prevTestLine;
    // This can be further optimized, since we now know that we have a 
    // sorted list of expansions, so current can start from toMove, since all
    // the elements before it are already smaller. In the beginning it needs to 
    // be on gLines, though.
    Line *current = *gLines;
    while(current) {
      if(current == toMove) {
        // we are wading through a sorted list
        // if we found ourselves, it means that we 
        // are already in a right place, so no moving
        // is needed, but we need to make sure we have
        // the right strength.
        toMove->strength = probeStrength(&prevL, &toMove, comparer);
        if(0) {
          u_fprintf(log, "Positioned expansion without moving ");
          printLine(toMove, log);
          u_fprintf(log, " new ordering: \n");
          printOrdering(gLines, size, log, true);
        }
        break;
      } else {
        u_strcpy(testLine.name, current->name);
        if(!current->isExpansion) {
          u_strcat(testLine.name, line->expansionString);
          testLine.len = current->len + line->expLen;
        } else {
          testLine.len = current->len;
        }
        if(comparer(&l, &line) > 0) {
          // remove from chain
          if(toMove->next) {
            toMove->next->strength = probeStrength(&(toMove->previous), &(toMove->next), comparer);
            toMove->next->previous = toMove->previous;
          }
          if(toMove->previous) {
            toMove->previous->next = toMove->next;
          }

          // insert
          toMove->previous = current->previous;
          toMove->next = current;

          if(current->previous) {
            current->previous->next = toMove;
          }
          current->previous = toMove;

          toMove->strength = probeStrength(&prevL, &toMove, comparer);
          toMove->next->strength = probeStrength(&toMove, &l, comparer);
          if(0) {
            u_fprintf(log, "Positioned expansion ");
            printLine(toMove, log);
            u_fprintf(log, " new ordering: \n");
            printOrdering(gLines, size, log, true);
          }
          if(toMove->strength == UCOL_IDENTICAL) {
            // check for craziness such as s = ss/s
            // such line would consist of previous (or next) concatenated with the expansion value
            // make a test
            char16_t fullString[256];
            u_strcpy(fullString, toMove->previous->name);
            u_strcat(fullString, toMove->expansionString);
            if(u_strcmp(fullString, toMove->name) == 0) {
              toMove->previous->next = toMove->next;
              toMove->next->previous = toMove->previous;
              toMove->isRemoved = true;
              u_fprintf(log, "Removed: ");
              printLine(toMove, log);
              u_fprintf(log, "\n");
            } 
          } else if(toMove->next->strength == UCOL_IDENTICAL) {
            char16_t fullString[256];
            u_strcpy(fullString, toMove->next->name);
            u_strcat(fullString, toMove->expansionString);
            if(u_strcmp(fullString, toMove->name) == 0) {
              toMove->next->strength = toMove->strength;
              toMove->previous->next = toMove->next;
              toMove->next->previous = toMove->previous;
              toMove->isRemoved = true;
              u_fprintf(log, "Removed because of back: ");
              printLine(toMove, log);
              u_fprintf(log, "\n");
            } 
          }
          break;
        }
        prevTestLine = testLine;
      }
      current = current->next;
    }
  }
  delete[] sortedExpansions;
}


void
noteExpansion(Line *line) {
  UErrorCode status = U_ZERO_ERROR;
  UnicodeString key(line->name, line->len);
  Line *el = (Line *)gElements.get(key);
  if(el != nullptr) {
    el->isExpansion = true;
    u_strcpy(el->expansionString, line->expansionString);
    el->expLen = line->expLen;
  } else {
    Line *toInsert = new Line(*line); 
    toInsert->isExpansion = true;
    gElements.put(UnicodeString(line->name, line->len), toInsert, status);
  }

  Line *el2 = (Line *)gExpansions.get(key);
  el2->isExpansion = true;
  u_strcpy(el2->expansionString, line->expansionString);
  el2->expLen = line->expLen;

  if(gDebug) {
    u_fprintf(log, "Adding expansion\n");
    printLine(line, log);
    u_fprintf(log, "\n");
  }
}

void 
noteContraction(Line *line) {
  UErrorCode status = U_ZERO_ERROR;
  Line *toInsert = new Line(*line); 
  toInsert->isContraction = true;
  gElements.put(UnicodeString(line->name, line->len), toInsert, status);
  if(gVerbose) {
    u_fprintf(log, "Adding contraction\n");
    escapeALine(line, log);
    u_fprintf(log, " ");
  }
}

void
noteElement(Line *line) {
  UErrorCode status = U_ZERO_ERROR;
  Line *toInsert = new Line(*line);
  gElements.put(UnicodeString(line->name, line->len), toInsert, status);
  if(0) { //if(gDebug) 
    escapeALine(line, log);
    u_fprintf(log, " ");
  }
}



// This function checks if a combination of characters has changed place with the 
// adjacent elements. If so, these are most probably contractions.
// However, it still needs to be checked if these contractions are fake - the 
// test is simple - if xy is suspected contraction, if we get that x/y is expansion, then
// xy is a fake contraction.
int32_t 
analyzeContractions(Line** lines, int32_t size, CompareFn comparer) {
  int32_t i = 0, j = 0;
  int32_t outOfOrder = 0;
  UColAttributeValue strength = UCOL_OFF;
  UColAttributeValue currStrength = UCOL_OFF;
  Line **prevLine = lines;
  Line **currLine = nullptr;
  Line **backupLine = nullptr;
  UBool prevIsContraction = false, currIsContraction = false;
  // Problem here is detecting a contraction that is at the very end of the sorted list
  for(i = 1; i < size; i++) {
    currLine = lines+i;
    strength = probeStrength(prevLine, currLine, comparer);
    if(strength == UCOL_OFF || strength != (*currLine)->strength) {
      prevIsContraction = false;
      currIsContraction = false;
      if(!outOfOrder) {
        if(gVerbose) {
          u_fprintf(log, "Possible contractions: ");
        }
      }
      // now we have two elements that are different. The question is, 
      // which one of them is the contraction - which one has moved. 
      // Could be the previous, but could also be the current.

      outOfOrder++;

      // First, lets check whether the previous has jumped back
      j = i+1;
      // skip all the nexts that have smaller strength, they don't have an effect
      while(j < size && (*(lines+j))->strength > (*currLine)->strength) {
        j++;
      }
      // check if there are other elements of same or greater strength
      while(j < size && 
        (strength = probeStrength(prevLine, (backupLine = lines+j), comparer)) == UCOL_OFF) {
        j++;
        // if we skipped more than one, it might be in fact a contraction
        prevIsContraction = true;
      }
      if(prevIsContraction) {
        noteContraction(*prevLine);
        j = i-2;
        // add all the previous elements with smaller strength, since they also
        // will jump over and are contractions
        while(j >= 0 && (*(lines+j+1))->strength > (*currLine)->strength) {
          strength = probeStrength(lines+j, currLine, comparer);
          if(strength == UCOL_OFF) {
            noteContraction(*(lines+j));
          }
          j--;
        }
      }

      // now we check if the current element is jumping forward,
      // the dance steps are analogous to above.
      j = i - 2;
      while(j >= 0 && (*(lines+j+1))->strength > (*currLine)->strength) {
        j--;
      }
      while(j >= 0 && 
        (strength = probeStrength((backupLine = lines+j), currLine, comparer)) == UCOL_OFF) {
        j--;
        currIsContraction = true;
      }
      if(currIsContraction) {
        if(gVerbose) {
          escapeALine(*currLine, log);
          u_fprintf(log, " ");
        }
        j = i+1;
        while(j < size && (*(lines+j))->strength > (*currLine)->strength) {
          strength = probeStrength(prevLine, lines+j, comparer);
          if(strength == UCOL_OFF) {
            noteContraction(*(lines+j));
          }
          j++;
        }
      }

      // Not sure about either. List both and then check
      if(!(prevIsContraction || currIsContraction)) {
        noteContraction(*prevLine);
        noteContraction(*currLine);
      }
    }
    prevLine = currLine;
  }
  if(outOfOrder) {
    if(gVerbose) {
      u_fprintf(log, "\n");
    }
  }
  return outOfOrder;
}

int32_t
detectContractions(Line **gLines, Line *lines, int32_t size, CompareFn comparer) {
  int32_t i = 0, j = 0;
  int32_t noContractions = 0;
  // Create and compare doubles:
  Line *backupLines = new Line[size]; 
  Line::copyArray(backupLines, lines, size); 
  // detect contractions

  Line **gLinesBackup = nullptr; //new Line*[size]; 

  for(i = 0; i < size; i++) {
    // preserve index and previous
    Line::copyArray(lines, backupLines, size); 
    for(j = 0; j < size; j++) {
      u_strcpy(lines[j].name, backupLines[i].name);
      u_strcat(lines[j].name, backupLines[j].name);
      lines[j].len = backupLines[i].len+backupLines[j].len;     
    }

    if((noContractions += analyzeContractions(gLines, size, comparer)) && gDebug) {
      if(gLinesBackup == nullptr) {
        gLinesBackup = new Line*[size];
      }
      // Show the sorted doubles, for debugging
      setArray(gLinesBackup, lines, size);
      qsort(gLinesBackup, size, sizeof(Line *), comparer);
      //setIndexes(gLinesBackup, size);
      analyzeStrength(gLinesBackup, size, comparer);
      printOrdering(gLinesBackup, size, log);
    }
    if(!gQuiet) {
      u_fprintf(log, ".");
    }
  }
  if(!gQuiet) {
    u_fprintf(log, "\n");
  }
  delete[] backupLines; 
  if(gLinesBackup) {
    delete[] gLinesBackup; 
  }
  return noContractions;
}

// gLines in this function is an array of sorted pointers.
// Contractions are already included. 
int32_t
detectExpansions(Line **gLines, int32_t size, CompareFn comparer) {
  UErrorCode status = U_ZERO_ERROR;
  // detect expansions

  UColAttributeValue startStrength = UCOL_OFF, endStrength = UCOL_OFF, 
    strength = UCOL_OFF, previousStrength = UCOL_OFF;
  Line start, end, src;
  Line *startP = &start, *endP = &end, *srcP = &src;
  Line *current = nullptr;
  memset(startP, 0, sizeof(Line));
  memset(endP, 0, sizeof(Line));
  memset(srcP, 0, sizeof(Line));
  int32_t srcLen;
  int32_t i = 0, j = 0, k = 0;
  for(i = 0; i < size; i++) {
    u_strcpy(start.name, (*(gLines+i))->name);
    u_strcpy(end.name, (*(gLines+i))->name);
    srcLen = (*(gLines+i))->len;
    u_strcpy(start.name+srcLen, (*(gLines))->name);
    start.len = srcLen + (*(gLines))->len;
    u_strcpy(end.name+srcLen, (*(gLines+size-1))->name);
    end.len = srcLen + (*(gLines+size-1))->len;

    for(k = 0; k < size; k++) { // k is index of a thing that is not doubled
      current = *(gLines+k);
      // see if we have moved to front
      // has it moved to the very beginning
      if((startStrength = probeStrength((gLines+k), &startP, comparer)) != UCOL_OFF) {
        continue; // this one is in the front
      }
      // has it moved to the very end?
      if((endStrength = probeStrength(&endP, (gLines+k), comparer)) != UCOL_OFF) {
        continue; // this one is in the back
      }
      // Potential Expansion     
      if(gDebug) { //gVerbose
        u_fprintf(log, "Possible expansion: ");
        escapeALine(*(gLines+k), log);
        u_fprintf(log, " ");
      }
      // Now we have to make sure that this is really an expansion
      // First, we have to find it
      u_strcpy(src.name, (*(gLines+i))->name);
      for(j = 0; j < size; j++) {
        u_strcpy(src.name+srcLen, (*(gLines+j))->name);
        src.len = srcLen + (*(gLines+j))->len;
        if((strength = probeStrength(&srcP, (gLines+k), comparer)) == UCOL_OFF) {
          strength = probeStrength((gLines+k), &srcP, comparer);
          // we found it *(gLines+j-1) is the element that is interesting
          // since gLines+j-1 < gLines+k < gLines+j
          if(gDebug) { //gVerbose
            u_fprintf(log, "i = %i, k = %i, j = %i ", i, k, j);
            escapeALine(*(gLines+i), log);
            escapeALine(*(gLines+j-1), log);
            printStrength(previousStrength, log);
            escapeALine(current, log);
            printStrength(strength, log);
            escapeALine(*(gLines+i), log);
            escapeALine(*(gLines+j), log);
            u_fprintf(log, "\n");
          }
          // check whether it is a contraction that is the same as an expansion
          // or a multi character that doesn't do anything
          current->addExpansionHit(i, j);
          current->isExpansion = true;
          current->expIndex = k;
          // cache expansion
          gExpansions.put(UnicodeString(current->name, current->len), current, status); //new Line(*current)
          break;
        }
        previousStrength = strength;
      }
    }
    if(!gQuiet) {
      u_fprintf(log, ".");
    }
  }  
  if(!gQuiet) {
    u_fprintf(log, "\n");
  }
  // now we have identified possible expansions. We need to find out how do they expand. 
  // Let's iterate over expansions cache - it's easier.
  const UHashElement *el = nullptr;
  int32_t hashIndex = -1;
  Line *doubles = new Line[size*10]; 
  Line **sorter = new Line*[size*10];
  int32_t currSize = 0;
  int32_t newSize = 0;
  Line *prev = nullptr;
  Line *next = nullptr;
  Line *origin = nullptr;
  int result = 0;
  // Make a list of things in the vincinity of expansion candidate
  // in expansionPrefixes and expansionAfter we have stored the
  // prefixes of stuff that caused the detection of an expansion
  // and a position where the expansion was. 
  // For example (icu, de__PHONEBOOK), we had:
  // aE <<< \u00E4 < af
  // AD < \u00E4 <<< Ae
  // From that we will construct the following sequence:
  // AD < aE <<< \u00E4/ <<< Ae < af
  // then we will take the vincinity of \u00E4:
  // aE <<< \u00E4/ <<< Ae
  // then we will choose the smallest expansion to be the expansion
  // part: 'e'.
  // if there is equality, we choose the equal part:
  // (win32, de__PHONEBOOK):
  // AD < \u00E4/ = ae <<< aE <<< Ae
  // we choose 'e'.

  while((el = gExpansions.nextElement(hashIndex)) != nullptr) {
    newSize = 0;
    current = (Line *)el->value.pointer;
    currSize = size*current->expansionPrefixesSize;
    if(gDebug) {
      escapeALine(current, log);
      u_fprintf(log, " Number: %i\n", current->expansionPrefixesSize);
    }
    // construct the doubles 
    for(i = 0; i < current->expansionPrefixesSize; i++) {
      doubles[newSize].suffix = current->expansionAfter[i]-1;
      doubles[newSize++].setToConcat(*(gLines+current->expansionPrefixes[i]), *(gLines+current->expansionAfter[i]-1));
      doubles[newSize].suffix = current->expansionAfter[i];
      doubles[newSize++].setToConcat(*(gLines+current->expansionPrefixes[i]), *(gLines+current->expansionAfter[i]));
    }
    // add the expansion we're observing
    doubles[newSize++] = *current;
    setArray(sorter, doubles, newSize);
    qsort(sorter, newSize, sizeof(Line*), comparer);
    analyzeStrength(sorter, newSize, comparer);
    if(gDebug) {
      printOrdering(sorter, newSize, log);
    }
    i = 0;
    while(**(sorter+i) != *current) {
      i++;
    }
    // find the two additions
    if((*(sorter+i))->strength == UCOL_IDENTICAL) {
      // if we ae id
      origin = *(gLines+((*(sorter+i-1))->suffix));
      u_strcpy(current->expansionString, origin->name);
      current->expLen = origin->len;
    } else if(i < newSize-1 && (*(sorter+i+1))->strength == UCOL_IDENTICAL) {
      origin = *(gLines+((*(sorter+i+1))->suffix));
      u_strcpy(current->expansionString, origin->name);
      current->expLen = origin->len;
    } else {
      if(i > 0) {
        prev = *(gLines+(*(sorter+i-1))->suffix);
        if(i < newSize-1) {
          next = *(gLines+(*(sorter+i+1))->suffix);
          result = comparer(&prev, &next);
          if(result <= 0) {
            u_strcpy(current->expansionString, prev->name);
            current->expLen = prev->len;
          } else {
            u_strcpy(current->expansionString, next->name);
            current->expLen = next->len;
          }
        }      
      }
      if(0) { //if(gDebug)
        u_fprintf(log, "Expansion is: ");
        escapeALine(current, log);
        u_fprintf(log, "/");
        escapeExpansion(current, log);
        u_fprintf(log, "\n");
      }
    }
    noteExpansion(current);
    //noteExpansion(gLines, current, size, comparer);
    if(!gQuiet) {
      u_fprintf(log, ".");
    }
  }
  if(!gQuiet) {
    u_fprintf(log, "\n");
  }
  delete[] doubles;
  delete[] sorter;
  return gExpansions.count();
}

UBool
isTailored(Line *line, UErrorCode &status) {
  UBool result = false;
  UCollationElements *tailoring = ucol_openElements(gCol, line->name, line->len, &status);
  UCollationElements *uca = ucol_openElements(gUCA, line->name, line->len, &status);

  int32_t tailElement = UCOL_NULLORDER;
  int32_t ucaElement = UCOL_NULLORDER;

  do {
    do {
      tailElement = ucol_next(tailoring, &status);
    } while(tailElement == 0);
    do {
      ucaElement = ucol_next(uca, &status);
    } while(ucaElement == 0);
    if(tailElement != ucaElement) {
      result = true;
      break;
    }
  } while (tailElement != UCOL_NULLORDER && ucaElement != UCOL_NULLORDER);

  ucol_closeElements(tailoring);
  ucol_closeElements(uca);
  return result;
}

void
reduceUntailored(Line **gLines, int32_t size){
  UErrorCode status = U_ZERO_ERROR;
  Line *current = *(gLines);
  Line *previous = nullptr;
  while(current) {
    // if the current line is not tailored according to the UCA
    if(!isTailored(current, status)) {
      // we remove it
      current->isRemoved = true;
    } else {
      // if it's tailored 
      if(current->previous && current->previous->isRemoved == true) {
        previous = current->previous;
        while(previous && (previous->strength > current->strength || previous->isExpansion || previous->isContraction) && previous->isRemoved) {
          if(previous->previous && previous->previous->isRemoved) {
            previous = previous->previous;
          } else {
            break;
          }
        }
        if(previous) {
          previous->isReset = true;
        } else {
          (*(gLines))->isReset = true;
        }
      }
    }
    current = current->next;
  }
}

void
constructAndAnalyze(Line **gLines, Line *lines, int32_t size, CompareFn comparer) {
  int32_t i = 0, j = 0, k = 0;
  // setup our compare arrays to point to single set.

  // For contractions we need a block of data
  setArray(gLines, lines, size);
  //size = setArray(gLines);

  qsort(gLines, size, sizeof(Line *), comparer);

  // Establish who is previous according to the sort order
  //setIndexes(gLines, size);

  analyzeStrength(gLines, size, comparer);
  if(gVerbose) {
    u_fprintf(log, "Ordering:\n");
    printOrdering(gLines, size, log);
  }

  //showDifferences(exemplarSetSize);
  //dumpData(exemplarSetSize);

  if(!gQuiet) {
    u_fprintf(log, "Detecting contractions?\n");
  }
  int32_t noContractions = 0;
  noContractions = detectContractions(gLines, lines, size, comparer);
  if(!gQuiet) {
    u_fprintf(log, "Detected %i contractions\n", noContractions);
  }

  // now we have suspected contractions in the table
  // we have to re-sort the things 
  size = setArray(gLines);
  qsort(gLines, size, sizeof(Line *), comparer);
  analyzeStrength(gLines, size, comparer);

  if(!gQuiet) {
    u_fprintf(log, "Detecting expansions\n");
  }
  int32_t noExpansions = detectExpansions(gLines, size, comparer);
  if(!gQuiet) {
    u_fprintf(log, "Detected %i expansions\n", noExpansions);
  }

  positionExpansions(gLines, size, comparer);

  if(gVerbose) {
    u_fprintf(log, "After positioning expansions:\n");
    printOrdering(gLines, size, log, true);
  }
  //reduceUntailored(gLines, size);
  if(!gQuiet) {
    u_fprintf(out, "Final result\n");
  }
  printOrdering(gLines, size, out, true);
  printOrdering(gLines, size, log, true);
}

// Check whether upper case comes before lower case or vice-versa
int32_t 
checkCaseOrdering(void) {
  char16_t stuff[][3] = {
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
  setArray(sortedLines, lines, size);
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


// Check whether the secondaries are in the straight or reversed order
int32_t 
checkSecondaryOrdering(void) {
  char16_t stuff[][5] = {
    { 0x0061, separatorChar, 0x0061, separatorChar, 0x00E0 }, //"aa",
    { 0x0061, separatorChar, 0x00E0, separatorChar, 0x0061 }, //"a\\u00E0",
    { 0x00E0, separatorChar, 0x0061, separatorChar, 0x0061 }, //"\\u00E0a",
    //{ 0x00E0, separatorChar, 0x00E0 }  //"\\u00E0\\u00E0"
  };
  const int32_t size = sizeof(stuff)/sizeof(stuff[0]);

  Line **sortedLines = new Line*[size];
  Line lines[size];

  int32_t i = 0;
  int32_t ordered = 0, reversed = 0;

  for(i = 0; i < size; i++) {
    lines[i].setName(stuff[i], 5);
  }
  setArray(sortedLines, lines, size);
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

// We have to remove ignorable characters from the exemplar set,
// otherwise, we get messed up results
void removeIgnorableChars(UnicodeSet &exemplarUSet, CompareFn comparer, UErrorCode &status) {
  UnicodeSet ignorables, primaryIgnorables;
  UnicodeSetIterator exemplarUSetIter(exemplarUSet);
  exemplarUSetIter.reset();
  Line empty;
  Line *emptyP = &empty;
  Line current;
  Line *currLine = &current;
  UColAttributeValue strength = UCOL_OFF;


  while(exemplarUSetIter.next()) {
    if(exemplarUSetIter.isString()) { // process a string
      u_memcpy(currLine->name, exemplarUSetIter.getString().getBuffer(), exemplarUSetIter.getString().length());
      currLine->len = exemplarUSetIter.getString().length();
      strength = probeStrength(&emptyP, &currLine, comparer);
      if(strength == UCOL_IDENTICAL) {
        ignorables.add(exemplarUSetIter.getString());
      } else if(strength > UCOL_PRIMARY) {
        primaryIgnorables.add(exemplarUSetIter.getString());
      }
    } else { // process code point
      UBool isError = false;
      UChar32 codePoint = exemplarUSetIter.getCodepoint();
      currLine->len = 0;
      U16_APPEND(currLine->name, currLine->len, 25, codePoint, isError);
      strength = probeStrength(&emptyP, &currLine, comparer);
      if(strength == UCOL_IDENTICAL) {
        ignorables.add(codePoint);
      } else if(strength > UCOL_PRIMARY) {
        primaryIgnorables.add(codePoint);
      }
    }
  }



  exemplarUSet.removeAll(ignorables);
  exemplarUSet.removeAll(primaryIgnorables);

  UnicodeString removedPattern;
  if(ignorables.size()) {
    u_fprintf(log, "Ignorables:\n");
    ignorables.toPattern(removedPattern, true);
    removedPattern.setCharAt(removedPattern.length(), 0);
    escapeString(removedPattern.getBuffer(), removedPattern.length(), log);
    u_fprintf(log, "\n");
  }
  if(primaryIgnorables.size()) {
    u_fprintf(log, "Primary ignorables:\n");
    primaryIgnorables.toPattern(removedPattern, true);
    removedPattern.setCharAt(removedPattern.length(), 0);
    escapeString(removedPattern.getBuffer(), removedPattern.length(), log);
    u_fprintf(log, "\n");
  }

}

// TODO: develop logic for choosing boundary characters - right now it is hardcoded
// It should be a function of used scripts. Also, check whether we need to save 
// used script names
void addUtilityChars(UnicodeSet &exemplarUSet, UErrorCode &status) {

  // in order to get nice rules, we need to add some characters to the
  // starting set. These are mostly parts of compatibility composed characters,
  // such as L-middle dot (middle dot is 0x00B7). If we don't add these, we would
  // get a reset at a funky character, such as L-middle dot. This list will probably
  // grow.
  exemplarUSet.add(0x00B7);

  // these things represent a script before the target script and 
  // a script after. More logic should be added so that these characters are
  // chosen automatically

  exemplarUSet.add(0x0038);
  exemplarUSet.add(0x0039);

  //exemplarUSet.add(0x0433);
  //exemplarUSet.add(0x0436);
  exemplarUSet.add(0xfa29);
  exemplarUSet.add(0xfa28);
}

void
getExemplars(const char *locale, UnicodeSet &exemplars, UErrorCode &status) {
  // first we fill out structures with exemplar characters.
  UResourceBundle *res = ures_open(nullptr, locale, &status);
  int32_t exemplarLength = 0;
  UnicodeString exemplarString = ures_getUnicodeStringByKey(res, "ExemplarCharacters", &status);
  exemplars.clear();
  exemplars.applyPattern(exemplarString, status);
  ures_close(res);
}

void
prepareStartingSet(UnicodeSet &exemplarUSet, CompareFn comparer, UErrorCode &status) {
  int32_t i = 0;
  UnicodeString exemplarString;
  exemplarUSet.toPattern(exemplarString);
  // Produce case closure of exemplar characters
  // Then we want to figure out what is the script of the exemplar characters
  // just pick several and see their script
  const char* usedScriptNames[USCRIPT_CODE_LIMIT];
  int32_t numberOfUsedScripts = 0;
  char scriptSetPattern[256];
  UnicodeString pattern; // for debugging
  UChar32 exChar = -1;
  while(exemplarUSet.size() != 0 && (exChar = exemplarUSet.charAt(0)) != -1) { 
    int32_t scriptNo = u_getIntPropertyValue(exChar, UCHAR_SCRIPT);
    usedScriptNames[numberOfUsedScripts] = u_getPropertyValueName(UCHAR_SCRIPT, scriptNo, U_SHORT_PROPERTY_NAME);
    sprintf(scriptSetPattern, "[:%s:]", usedScriptNames[numberOfUsedScripts]);
    numberOfUsedScripts++;
    UnicodeSet scriptSet(UnicodeString(scriptSetPattern, ""), status);
    exemplarUSet.removeAll(scriptSet);
    exemplarUSet.toPattern(pattern, true);
  }
  exemplarUSet.clear();

  // always add ASCII
  //exemplarUSet.addAll(UnicodeSet(UnicodeString("[\\u0020-\\u007f]", ""), status));
  exemplarUSet.addAll(UnicodeSet(UnicodeString("[\\u0041-\\u005b]", ""), status));
  if(gExemplar) {
    exemplarUSet.applyPattern(exemplarString, status);
    exemplarUSet.closeOver(USET_CASE);
    if(!gQuiet) {
      u_fprintf(out, "ICU exemplar characters:\n");
      escapeString(exemplarString.getBuffer(), exemplarString.length(), out);
      u_fprintf(out, "\n");
    }
  } else {
    if(!gQuiet) {
      u_fprintf(out, "Using scripts:\n");
    }
    // add interesting scripts
    for(i = 0; i < numberOfUsedScripts; i++) {
      sprintf(scriptSetPattern, "[:%s:]", usedScriptNames[i]);
      exemplarUSet.addAll(UnicodeSet(UnicodeString(scriptSetPattern, ""), status));
      if(!gQuiet) {
        u_fprintf(out, "%s\n", scriptSetPattern);
      }
    }
  }


  removeIgnorableChars(exemplarUSet, comparer, status);

  addUtilityChars(exemplarUSet, status);

/*
  // try to check whether tailored set and exemplar characters match.
  USet *tailored = ucol_getTailoredSet(gCol, &status);
  UBool tailoredContained = exemplarUSet.containsAll(*((UnicodeSet *)tailored));
  if(!tailoredContained) {
    ((UnicodeSet *)tailored)->removeAll(exemplarUSet);
    UnicodeString pattern;
    ((UnicodeSet *)tailored)->toPattern(pattern, true);
  }
  uset_close(tailored);
*/

  //return exemplarUSet;
}

void 
setOutputFile(const char *name, UErrorCode &status) {
  int32_t i = 0;
  char filename[256];
  strcpy(filename, name);
  for(i = 0; i < gPlatformNo; i++) {
    strcat(filename, "_");
    strcat(filename, platforms[gPlatformIndexes[i]].name);
  }
  if(gExemplar) {
    strcat(filename, "_exemplar");
  } else {
    strcat(filename, "_script");
  }
  strcat(filename, ".utf16.txt");
  out = u_fopen(filename, "wb", "en", "utf-16");
}

void
processCollator(UCollator *col, UErrorCode &status) {
  int32_t i = 0;
  gCol = col;
  char16_t ruleString[16384];
  int32_t ruleStringLength = ucol_getRulesEx(gCol, UCOL_TAILORING_ONLY, ruleString, 16384);
  if(!gQuiet) {
    u_fprintf(out, "ICU rules:\n");
    printRules(ruleString, ruleStringLength, out);
    printRules(ruleString, ruleStringLength, log);
    //escapeString(ruleString, ruleStringLength, out);
    u_fprintf(out, "\n");
  }
  const char *locale = ucol_getLocale(gCol, ULOC_REQUESTED_LOCALE, &status);
  UnicodeSet exemplarUSet;
  if(locale) {
    getExemplars(locale, exemplarUSet, status);
  } else {
    exemplarUSet = *((UnicodeSet *)ucol_getTailoredSet(gCol, &status));
  }


  for(i = 0; i < gPlatformNo; i++) {
    u_fprintf(out, "\nGenerating order for platform: %s\n", platforms[gPlatformIndexes[i]].name);
    gComparer = platforms[gPlatformIndexes[i]].comparer;

    prepareStartingSet(exemplarUSet, gComparer, status);
    int32_t itemLen = 0;
    // get the number of all the items from the set (both codepoints and strings)
    int32_t exemplarSetSize = exemplarUSet.size();
    UnicodeSetIterator exemplarUSetIter(exemplarUSet);

    // allocate ICU lines
    gICULines = new Line*[exemplarSetSize*5]; 
    int32_t j = 0;
    int32_t linesCount = 0;
    Line *lines = new Line[exemplarSetSize]; 

    int32_t reversedSecondary = checkSecondaryOrdering();
    if(reversedSecondary == 0) {
      u_fprintf(out, "Secondaries do not seem to be reversed\n");
    } else if(reversedSecondary == 1) {
      u_fprintf(out, "Secondaries are reversed\n");
      if(gComparer == ICUstrcmp) {
        ucol_setAttribute(gCol, UCOL_FRENCH_COLLATION, UCOL_OFF, &status);
      }
    } else {
      u_fprintf(out, "Cannot conclude if secondaries are reversed\n");
    }

    int32_t reversedCase = checkCaseOrdering();
    if(reversedCase == 0) {
      u_fprintf(out, "Case does not seem to be reversed\n");
    } else if(reversedCase == 1) {
      u_fprintf(out, "Case is reversed\n");
      if(gComparer == ICUstrcmp) {
        ucol_setAttribute(gCol, UCOL_CASE_FIRST, UCOL_OFF, &status);
      }
    } else {
      u_fprintf(out, "Cannot conclude if case is reversed\n");
    }
      
    exemplarUSetIter.reset();
    gElements.removeAll();
    gExpansions.removeAll();
    linesCount = 0;

    while(exemplarUSetIter.next()) {
      Line *currLine = lines+linesCount;
      if(exemplarUSetIter.isString()) { // process a string
        u_memcpy(currLine->name, exemplarUSetIter.getString().getBuffer(), exemplarUSetIter.getString().length());
        currLine->len = exemplarUSetIter.getString().length();
      } else { // process code point
        UBool isError = false;
        currLine->len = 0;
        U16_APPEND(currLine->name, currLine->len, 25, exemplarUSetIter.getCodepoint(), isError);
      }
      currLine->name[currLine->len] = 0; // zero terminate, for our evil ways
      currLine->index = linesCount;
      linesCount++;
      noteElement(currLine);
    }
    constructAndAnalyze(gICULines, lines, exemplarSetSize, gComparer);

    delete[] lines; 
  }


  // cleanup globals
  delete[] gICULines; 
  u_fflush(out);
  u_fclose(out);
  ucol_close(gCol);
}

void
processLocale(const char *locale, UErrorCode &status) {
  gWinLCID = uloc_getLCID(locale);

  UCollator *col = ucol_open(locale, &status);

  setOutputFile(locale, status);

  u_fprintf(out, "Locale %s (LCID:%06X)\n", locale, gWinLCID);

  processCollator(col, status);
}

UBool 
hasCollationElements(const char *locName) {

  UErrorCode status = U_ZERO_ERROR;
  UResourceBundle *ColEl = nullptr;

  UResourceBundle *loc = ures_open(nullptr, locName, &status);;

  if(U_SUCCESS(status)) {
    status = U_ZERO_ERROR;
    ColEl = ures_getByKey(loc, "CollationElements", ColEl, &status);
    if(status == U_ZERO_ERROR) { /* do the test - there are real elements */
      ures_close(ColEl);
      ures_close(loc);
      return true;
    }
    ures_close(ColEl);
    ures_close(loc);
  }
  return false;
}

int
main(int argc,
     char* argv[])
{
  UErrorCode status = U_ZERO_ERROR;
  err = u_finit(stderr, "en", "latin-1");
  log = u_finit(stdout, "en", "latin-1");

/*
  USet *wsp = uprv_openRuleWhiteSpaceSet(&status);
  uset_add(wsp, 0x0041);
  uset_remove(wsp, 0x0041);
  UnicodeString pat;
  ((UnicodeSet *)wsp)->toPattern(pat, true);
  pat.setCharAt(pat.length(), 0);
  escapeString(pat.getBuffer(), pat.length(), log);
  u_fflush(log);
*/

  UTransliterator *anyHex = utrans_open("[^\\u000a\\u0020-\\u007f] Any-Hex/Java", UTRANS_FORWARD, nullptr, 0, nullptr, &status);
  u_fsettransliterator(log, U_WRITE, anyHex, &status);

  processArgs(argc, argv, status);
  int32_t i = 0;


  gElements.setValueDeleter(deleteLineElement);


  if(U_FAILURE(status) || gPlatformNo == 0) {
    return -1;
  }

  gUCA = ucol_open("root", &status);

  if(gRulesStdin) {
    char buffer[1024];
    char16_t ruleBuffer[16384];
    char16_t *rules = ruleBuffer;
    int32_t maxRuleLen = 16384;
    int32_t rLen = 0;
    while(gets(buffer)) {
      if(buffer[0] != '/' && buffer[1] != '/') {
        rLen = u_unescape(buffer, rules, maxRuleLen);
        rules += rLen;
        maxRuleLen -= rLen;
      }
    }
    UParseError parseError;
    //escapeString(ruleBuffer, rules-ruleBuffer, log);//
    u_fprintf(log, "%U\n", ruleBuffer);

    UCollator *col = ucol_openRules(ruleBuffer, rules-ruleBuffer, UCOL_DEFAULT, UCOL_DEFAULT, &parseError, &status);
    if(U_SUCCESS(status)) {
      setOutputFile("stdinRules", status);
      processCollator(col, status);
    } else {
      u_fprintf(err, "Error %s\n", u_errorName(status));
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
      const char *locName = nullptr;
      for(i = 0; i<noOfLoc; i++) {
        status = U_ZERO_ERROR;
        locName = uloc_getAvailable(i);
        if(hasCollationElements(locName)) {
          processLocale(locName, status);
        }
      }
    }
  }


  ucol_close(gUCA);

  u_fflush(log);
  u_fclose(log);
  u_fflush(err);
  u_fclose(err);

  return 0;
}