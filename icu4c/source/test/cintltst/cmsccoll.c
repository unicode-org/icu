/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*******************************************************************************
*
* File cmsccoll.C
*
*******************************************************************************/
/**
 * These are the tests specific to ICU 1.8 and above, that I didn't know where
 * to fit.
 */

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/ucoleitr.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "callcoll.h"
#include "unicode/ustring.h"
#include "string.h"
#include "ucol_imp.h"
#include "ucol_tok.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/parseerr.h"

#define MAX_TOKEN_LEN 16
#define RULE_BUFFER_LEN 8192

typedef int tst_strcoll(void *collator, const int object,
                        const UChar *source, const int sLen,
                        const UChar *target, const int tLen);


/**
 * Return an integer array containing all of the collation orders
 * returned by calls to next on the specified iterator
 */
static int32_t* getOrders(UCollationElements *iter, int32_t *orderLength)
{
    UErrorCode status;
    int32_t order;
    int32_t maxSize = 100;
    int32_t size = 0;
    int32_t *temp;
    int32_t *orders =(int32_t*)malloc(sizeof(int32_t) * maxSize);
    status= U_ZERO_ERROR;


    while ((order=ucol_next(iter, &status)) != UCOL_NULLORDER)
    {
        if (size == maxSize)
        {
            maxSize *= 2;
            temp = (int32_t*)malloc(sizeof(int32_t) * maxSize);

            memcpy(temp, orders, size * sizeof(int32_t));
            free(orders);
            orders = temp;

        }

        orders[size++] = order;
    }

    if (maxSize > size && size > 0)
    {
        temp = (int32_t*)malloc(sizeof(int32_t) * size);

        memcpy(temp, orders, size * sizeof(int32_t));
        free(orders);
        orders = temp;


    }

    *orderLength = size;
    return orders;
}

static void backAndForth(UCollationElements *iter)
{
    /* Run through the iterator forwards and stick it into an array */
    int32_t index, o;
    UErrorCode status = U_ZERO_ERROR;
    int32_t orderLength = 0;
    int32_t *orders;
    orders= getOrders(iter, &orderLength);


    /* Now go through it backwards and make sure we get the same values */
    index = orderLength;
    ucol_reset(iter);

    /* synwee : changed */
    while ((o = ucol_previous(iter, &status)) != UCOL_NULLORDER)
    {
      if (o != orders[-- index])
      {
        if (o == 0)
          index ++;
        else
        {
          while (index > 0 && orders[-- index] == 0)
          {
          }
          if (o != orders[index])
          {
            log_err("Mismatch at index : %d\n", index);
            break;
          }
        }
      }
    }

    while (index != 0 && orders[index - 1] == 0) {
      index --;
    }

    if (index != 0)
    {
        log_err("Didn't get back to beginning - index is %d\n", index);

        ucol_reset(iter);
        log_err("\nnext: ");
        while ((o = ucol_next(iter, &status)) != UCOL_NULLORDER)
        {
            log_err("Error at %d\n", o);
        }
        log_err("\nprev: ");
        while ((o = ucol_previous(iter, &status)) != UCOL_NULLORDER)
        {
            log_err("Error at %d\n", o);
        }
        log_verbose("\n");
    }

    free(orders);
}

const static char cnt1[][10] = {
  "AA",
  "AC",
  "AZ",
  "AQ",
  "AB",
  "ABZ",
  "ABQ",
  "Z",
  "ABC",
  "Q",
  "B"
};

const static char cnt2[][10] = {
  "DA",
  "DAD",
  "DAZ",
  "MAR",
  "Z",
  "DAVIS",
  "MARK",
  "DAV",
  "DAVI"
};

static void IncompleteCntTest( )
{
  UErrorCode status = U_ZERO_ERROR;
  UChar *temp=(UChar*)malloc(sizeof(UChar) * 90);
  UChar *t1 =(UChar*)malloc(sizeof(UChar) * 90);
  UChar *t2 =(UChar*)malloc(sizeof(UChar) * 90);

  UCollator *coll =  NULL;
  uint32_t i = 0, j = 0;
  uint32_t size = 0;

  u_uastrcpy(temp, " & Z < ABC < Q < B");

  coll = ucol_openRules(temp, u_strlen(temp), UCOL_OFF, UCOL_DEFAULT_STRENGTH, NULL,&status);

  if(U_SUCCESS(status)) {
    size = sizeof(cnt1)/sizeof(cnt1[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        UCollationElements *iter;
        u_uastrcpy(t1, cnt1[i]);
        u_uastrcpy(t2, cnt1[j]);
        doTest(coll, t1, t2, UCOL_LESS);
        /* synwee : added collation element iterator test */
        iter = ucol_openElements(coll, t2, u_strlen(t2), &status);
        if (U_FAILURE(status)) {
          log_err("Creation of iterator failed\n");
          break;
        }
        backAndForth(iter);
        free(iter);
      }
    }
  }

  ucol_close(coll);


  u_uastrcpy(temp, " & Z < DAVIS < MARK <DAV");
  coll = ucol_openRules(temp, u_strlen(temp), UCOL_OFF, UCOL_DEFAULT_STRENGTH,NULL, &status);

  if(U_SUCCESS(status)) {
    size = sizeof(cnt2)/sizeof(cnt2[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        UCollationElements *iter;
        u_uastrcpy(t1, cnt2[i]);
        u_uastrcpy(t2, cnt2[j]);
        doTest(coll, t1, t2, UCOL_LESS);

        /* synwee : added collation element iterator test */
        iter = ucol_openElements(coll, t2, u_strlen(t2), &status);
        if (U_FAILURE(status)) {
          log_err("Creation of iterator failed\n");
          break;
        }
        backAndForth(iter);
        free(iter);
      }
    }
  }

  ucol_close(coll);


}

const static char shifted[][20] = {
  "black bird",
  "black-bird",
  "blackbird",
  "black Bird",
  "black-Bird",
  "blackBird",
  "black birds",
  "black-birds",
  "blackbirds"
};

const static UCollationResult shiftedTert[] = {
  0,
  UCOL_EQUAL,
  UCOL_EQUAL,
  UCOL_LESS,
  UCOL_EQUAL,
  UCOL_EQUAL,
  UCOL_LESS,
  UCOL_EQUAL,
  UCOL_EQUAL
};

const static char nonignorable[][20] = {
  "black bird",
  "black Bird",
  "black birds",
  "black-bird",
  "black-Bird",
  "black-birds",
  "blackbird",
  "blackBird",
  "blackbirds"
};

static void BlackBirdTest( ) {
  UErrorCode status = U_ZERO_ERROR;
  UChar *t1 =(UChar*)malloc(sizeof(UChar) * 90);
  UChar *t2 =(UChar*)malloc(sizeof(UChar) * 90);

  uint32_t i = 0, j = 0;
  uint32_t size = 0;
  UCollator *coll = ucol_open("en_US", &status);

  ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
  ucol_setAttribute(coll, UCOL_ALTERNATE_HANDLING, UCOL_NON_IGNORABLE, &status);

  if(U_SUCCESS(status)) {
    size = sizeof(nonignorable)/sizeof(nonignorable[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        u_uastrcpy(t1, nonignorable[i]);
        u_uastrcpy(t2, nonignorable[j]);
        doTest(coll, t1, t2, UCOL_LESS);
      }
    }
  }

  ucol_setAttribute(coll, UCOL_ALTERNATE_HANDLING, UCOL_SHIFTED, &status);
  ucol_setAttribute(coll, UCOL_STRENGTH, UCOL_QUATERNARY, &status);

  if(U_SUCCESS(status)) {
    size = sizeof(shifted)/sizeof(shifted[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        u_uastrcpy(t1, shifted[i]);
        u_uastrcpy(t2, shifted[j]);
        doTest(coll, t1, t2, UCOL_LESS);
      }
    }
  }

  ucol_setAttribute(coll, UCOL_STRENGTH, UCOL_TERTIARY, &status);
  if(U_SUCCESS(status)) {
    size = sizeof(shifted)/sizeof(shifted[0]);
    for(i = 1; i < size; i++) {
      u_uastrcpy(t1, shifted[i-1]);
      u_uastrcpy(t2, shifted[i]);
      doTest(coll, t1, t2, shiftedTert[i]);
    }
  }

  ucol_close(coll);
}

const static UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {0x0041/*'A'*/, 0x0300, 0x0301, 0x0000},
    {0x0041/*'A'*/, 0x0300, 0x0316, 0x0000},
    {0x0041/*'A'*/, 0x0300, 0x0000},
    {0x00C0, 0x0301, 0x0000},
    /* this would work with forced normalization */
    {0x00C0, 0x0316, 0x0000}
};

const static UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {0x0041/*'A'*/, 0x0301, 0x0300, 0x0000},
    {0x0041/*'A'*/, 0x0316, 0x0300, 0x0000},
    {0x00C0, 0},
    {0x0041/*'A'*/, 0x0301, 0x0300, 0x0000},
    /* this would work with forced normalization */
    {0x0041/*'A'*/, 0x0316, 0x0300, 0x0000}
};

const static UCollationResult results[] = {
    UCOL_GREATER,
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_GREATER,
    UCOL_EQUAL
};

static void FunkyATest( )
{

    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    UCollator  *myCollation;
    myCollation = ucol_open("en_US", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing some A letters, for some reason\n");
    ucol_setAttribute(myCollation, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 4 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}

UColAttributeValue caseFirst[] = {
    UCOL_OFF,
    UCOL_LOWER_FIRST,
    UCOL_UPPER_FIRST
};


UColAttributeValue alternateHandling[] = {
    UCOL_NON_IGNORABLE,
    UCOL_SHIFTED
};

UColAttributeValue caseLevel[] = {
    UCOL_OFF,
    UCOL_ON
};

UColAttributeValue strengths[] = {
    UCOL_PRIMARY,
    UCOL_SECONDARY,
    UCOL_TERTIARY,
    UCOL_QUATERNARY,
    UCOL_IDENTICAL
};

#if 0
static const char * caseFirstC[] = {
    "UCOL_OFF",
    "UCOL_LOWER_FIRST",
    "UCOL_UPPER_FIRST"
};


static const char * alternateHandlingC[] = {
    "UCOL_NON_IGNORABLE",
    "UCOL_SHIFTED"
};

static const char * caseLevelC[] = {
    "UCOL_OFF",
    "UCOL_ON"
};

static const char * strengthsC[] = {
    "UCOL_PRIMARY",
    "UCOL_SECONDARY",
    "UCOL_TERTIARY",
    "UCOL_QUATERNARY",
    "UCOL_IDENTICAL"
};

/* not used currently - does not test only prints */
static void PrintMarkDavis( )
{
  UErrorCode status = U_ZERO_ERROR;
  UChar m[256];
  uint8_t sortkey[256];
  UCollator *coll = ucol_open("en_US", &status);
  uint32_t h,i,j,k, sortkeysize;
  uint32_t sizem = 0;
  char buffer[512];
  uint32_t len = 512;

  log_verbose("PrintMarkDavis");

  u_uastrcpy(m, "Mark Davis");
  sizem = u_strlen(m);


  m[1] = 0xe4;

  for(i = 0; i<sizem; i++) {
    fprintf(stderr, "\\u%04X ", m[i]);
  }
  fprintf(stderr, "\n");

  for(h = 0; h<sizeof(caseFirst)/sizeof(caseFirst[0]); h++) {
    ucol_setAttribute(coll, UCOL_CASE_FIRST, caseFirst[i], &status);
    fprintf(stderr, "caseFirst: %s\n", caseFirstC[h]);

    for(i = 0; i<sizeof(alternateHandling)/sizeof(alternateHandling[0]); i++) {
      ucol_setAttribute(coll, UCOL_ALTERNATE_HANDLING, alternateHandling[i], &status);
      fprintf(stderr, "  AltHandling: %s\n", alternateHandlingC[i]);

      for(j = 0; j<sizeof(caseLevel)/sizeof(caseLevel[0]); j++) {
        ucol_setAttribute(coll, UCOL_CASE_LEVEL, caseLevel[j], &status);
        fprintf(stderr, "    caseLevel: %s\n", caseLevelC[j]);

        for(k = 0; k<sizeof(strengths)/sizeof(strengths[0]); k++) {
          ucol_setAttribute(coll, UCOL_STRENGTH, strengths[k], &status);
          sortkeysize = ucol_getSortKey(coll, m, sizem, sortkey, 256);
          fprintf(stderr, "      strength: %s\n      Sortkey: ", strengthsC[k]);
          fprintf(stderr, "%s\n", ucol_sortKeyToString(coll, sortkey, buffer, &len));
        }

      }

    }

  }
}
#endif

static void BillFairmanTest( ) {
/*
** check for actual locale via ICU resource bundles
**
** lp points to the original locale ("fr_FR_....")
*/

    UResourceBundle *lr,*cr;
    UErrorCode              lec = U_ZERO_ERROR;
    const char *lp = "fr_FR_you_ll_never_find_this_locale";

    log_verbose("BillFairmanTest\n");

    lr = ures_open(NULL,lp,&lec);
    if (lr) {
        cr = ures_getByKey(lr,"CollationElements",0,&lec);
        if (cr) {
            lp = ures_getLocale(cr,&lec);
            if (lp) {
                if (U_SUCCESS(lec)) {
                    if(strcmp(lp, "fr") != 0) {
                        log_err("Wrong locale for French Collation Data, expected \"fr\" got %s", lp);
                    }
                }
            }
            ures_close(cr);
        }
        ures_close(lr);
    }
}

static void testPrimary(UCollator* col, const UChar* p,const UChar* q){
    UChar source[256] = { '\0'};
    UChar target[256] = { '\0'};
    UChar preP = 0x31a3;
    UChar preQ = 0x310d;
/*
    UChar preP = (*p>0x0400 && *p<0x0500)?0x00e1:0x491;
    UChar preQ = (*p>0x0400 && *p<0x0500)?0x0041:0x413;
*/
    /*log_verbose("Testing primary\n");*/

    doTest(col, p, q, UCOL_LESS);
/*
    UCollationResult result = ucol_strcoll(col,p,u_strlen(p),q,u_strlen(q));

    if(result!=UCOL_LESS){
       aescstrdup(p,utfSource,256);
       aescstrdup(q,utfTarget,256);
       fprintf(file,"Primary failed  source: %s target: %s \n", utfSource,utfTarget);
    }
*/
    source[0] = preP;
    u_strcpy(source+1,p);
    target[0] = preQ;
    u_strcpy(target+1,q);
    doTest(col, source, target, UCOL_LESS);
/*
    fprintf(file,"Primary swamps 2nd failed  source: %s target: %s \n", utfSource,utfTarget);
*/
}

static void testSecondary(UCollator* col, const UChar* p,const UChar* q){
    UChar source[256] = { '\0'};
    UChar target[256] = { '\0'};

    /*log_verbose("Testing secondary\n");*/

    doTest(col, p, q, UCOL_LESS);
/*
    fprintf(file,"secondary failed  source: %s target: %s \n", utfSource,utfTarget);
*/
    source[0] = 0x0053;
    u_strcpy(source+1,p);
    target[0]= 0x0073;
    u_strcpy(target+1,q);

    doTest(col, source, target, UCOL_LESS);
/*
    fprintf(file,"secondary swamps 3rd failed  source: %s target: %s \n",utfSource,utfTarget);
*/


    u_strcpy(source,p);
    source[u_strlen(p)] = 0x62;
    source[u_strlen(p)+1] = 0;


    u_strcpy(target,q);
    target[u_strlen(q)] = 0x61;
    target[u_strlen(q)+1] = 0;

    doTest(col, source, target, UCOL_GREATER);

/*
    fprintf(file,"secondary is swamped by 1  failed  source: %s target: %s \n",utfSource,utfTarget);
*/
}

static void testTertiary(UCollator* col, const UChar* p,const UChar* q){
    UChar source[256] = { '\0'};
    UChar target[256] = { '\0'};

    /*log_verbose("Testing tertiary\n");*/

    doTest(col, p, q, UCOL_LESS);
/*
    fprintf(file,"Tertiary failed  source: %s target: %s \n",utfSource,utfTarget);
*/
    source[0] = 0x0020;
    u_strcpy(source+1,p);
    target[0]= 0x002D;
    u_strcpy(target+1,q);

    doTest(col, source, target, UCOL_LESS);
/*
    fprintf(file,"Tertiary swamps 4th failed  source: %s target: %s \n", utfSource,utfTarget);
*/

    u_strcpy(source,p);
    source[u_strlen(p)] = 0xE0;
    source[u_strlen(p)+1] = 0;

    u_strcpy(target,q);
    target[u_strlen(q)] = 0x61;
    target[u_strlen(q)+1] = 0;

    doTest(col, source, target, UCOL_GREATER);

/*
    fprintf(file,"Tertiary is swamped by 3rd failed  source: %s target: %s \n",utfSource,utfTarget);
*/
}

static void testEquality(UCollator* col, const UChar* p,const UChar* q){
/*
    UChar source[256] = { '\0'};
    UChar target[256] = { '\0'};
*/

    doTest(col, p, q, UCOL_EQUAL);
/*
    fprintf(file,"Primary failed  source: %s target: %s \n", utfSource,utfTarget);
*/
}

static void testCollator(UCollator *coll, UErrorCode *status) {
  const UChar *rules = NULL, *current = NULL;
  int32_t ruleLen = 0;
  uint32_t strength = 0;
  uint32_t chOffset = 0; uint32_t chLen = 0;
  uint32_t exOffset = 0; uint32_t exLen = 0;
  uint32_t prefixOffset = 0; uint32_t prefixLen = 0;
  uint32_t firstEx = 0;
/*  uint32_t rExpsLen = 0; */
  uint32_t firstLen = 0;
  UBool varT = FALSE; UBool top_ = TRUE;
  uint8_t specs = 0;
  UBool startOfRules = TRUE;
  UBool lastReset = FALSE;
  UColTokenParser src;
  UColOptionSet opts;

  UChar first[256];
  UChar second[256];
  UChar *rulesCopy = NULL;
  UParseError parseError;
  src.opts = &opts;

  rules = ucol_getRules(coll, &ruleLen);
  if(U_SUCCESS(*status) && ruleLen > 0) {
    rulesCopy = (UChar *)uprv_malloc((ruleLen+UCOL_TOK_EXTRA_RULE_SPACE_SIZE)*sizeof(UChar));
    uprv_memcpy(rulesCopy, rules, ruleLen*sizeof(UChar));
    src.source = src.current = rulesCopy;
    src.end = rulesCopy+ruleLen;
    src.extraCurrent = src.end;
    src.extraEnd = src.end+UCOL_TOK_EXTRA_RULE_SPACE_SIZE;
    *first = *second = 0;

    while ((current = ucol_tok_parseNextToken(&src, &strength,
                      &chOffset, &chLen, &exOffset, &exLen,
                      &prefixOffset, &prefixLen,
                      &specs, startOfRules,&parseError, status)) != NULL) {
      startOfRules = FALSE;
      varT = (UBool)((specs & UCOL_TOK_VARIABLE_TOP) != 0);
      top_ = (UBool)((specs & UCOL_TOK_TOP) != 0);
      u_strncpy(second,rulesCopy+chOffset, chLen);
      second[chLen] = 0;

      if(exLen > 0 && firstEx == 0) {
        u_strncat(first, rulesCopy+exOffset, exLen);
        first[firstLen+exLen] = 0;
      }

      lastReset = FALSE;

      switch(strength){
      case UCOL_IDENTICAL:
          testEquality(coll,first,second);
          break;
      case UCOL_PRIMARY:
          testPrimary(coll,first,second);
          break;
      case UCOL_SECONDARY:
          testSecondary(coll,first,second);
          break;
      case UCOL_TERTIARY:
          testTertiary(coll,first,second);
          break;
      case UCOL_TOK_RESET:
      default:
          break;
      }

      firstLen = chLen;
      firstEx = exLen;
      u_strcpy(first, second);

    }
    uprv_free(rulesCopy);
  }
}

static int ucaTest(void *collator, const int object, const UChar *source, const int sLen, const UChar *target, const int tLen) {
  UCollator *UCA = (UCollator *)collator;
  return ucol_strcoll(UCA, source, sLen, target, tLen);
}

/*
static int winTest(void *collator, const int object, const UChar *source, const int sLen, const UChar *target, const int tLen) {
#ifdef WIN32
  LCID lcid = (LCID)collator;
  return CompareString(lcid, 0, source, sLen, target, tLen);
#else
  return 0;
#endif
}
*/

static UCollationResult swampEarlier(tst_strcoll* func, void *collator, int opts,
                                     UChar s1, UChar s2,
                                     const UChar *s, const uint32_t sLen,
                                     const UChar *t, const uint32_t tLen) {
  UChar source[256] = {0};
  UChar target[256] = {0};

  source[0] = s1;
  u_strcpy(source+1, s);
  target[0] = s2;
  u_strcpy(target+1, t);

  return func(collator, opts, source, sLen+1, target, tLen+1);
}

static UCollationResult swampLater(tst_strcoll* func, void *collator, int opts,
                                   UChar s1, UChar s2,
                                   const UChar *s, const uint32_t sLen,
                                   const UChar *t, const uint32_t tLen) {
  UChar source[256] = {0};
  UChar target[256] = {0};

  u_strcpy(source, s);
  source[sLen] = s1;
  u_strcpy(target, t);
  target[tLen] = s2;

  return func(collator, opts, source, sLen+1, target, tLen+1);
}

static uint32_t probeStrength(tst_strcoll* func, void *collator, int opts,
                              const UChar *s, const uint32_t sLen,
                              const UChar *t, const uint32_t tLen,
                              UCollationResult result) {
  /*UChar fPrimary = 0x6d;*/
  /*UChar sPrimary = 0x6e;*/
  UChar fSecondary = 0x310d;
  UChar sSecondary = 0x31a3;
  UChar fTertiary = 0x310f;
  UChar sTertiary = 0x31b7;

  UCollationResult oposite;
  if(result == UCOL_EQUAL) {
    return UCOL_IDENTICAL;
  } else if(result == UCOL_GREATER) {
    oposite = UCOL_LESS;
  } else {
    oposite = UCOL_GREATER;
  }

  if(swampEarlier(func, collator, opts, sSecondary, fSecondary, s, sLen, t, tLen) == result) {
    return UCOL_PRIMARY;
  } else if((swampEarlier(func, collator, opts, sTertiary, 0x310f, s, sLen, t, tLen) == result) &&
    (swampEarlier(func, collator, opts, 0x310f, sTertiary, s, sLen, t, tLen) == result)) {
    return UCOL_SECONDARY;
  } else if((swampLater(func, collator, opts, sTertiary, fTertiary, s, sLen, t, tLen) == result) &&
    (swampLater(func, collator, opts, fTertiary, sTertiary, s, sLen, t, tLen) == result)) {
    return UCOL_TERTIARY;
  } else if((swampLater(func, collator, opts, sTertiary, 0x310f, s, sLen, t, tLen) == oposite) &&
    (swampLater(func, collator, opts, fTertiary, sTertiary, s, sLen, t, tLen) == oposite)) {
    return UCOL_QUATERNARY;
  } else {
    return UCOL_IDENTICAL;
  }
}

static char *getRelationSymbol(UCollationResult res, uint32_t strength, char *buffer) {
  uint32_t i = 0;

  if(res == UCOL_EQUAL || strength == 0xdeadbeef) {
    buffer[0] = '=';
    buffer[1] = '=';
    buffer[2] = '\0';
  } else if(res == UCOL_GREATER) {
    for(i = 0; i<strength+1; i++) {
      buffer[i] = '>';
    }
    buffer[strength+1] = '\0';
  } else {
    for(i = 0; i<strength+1; i++) {
      buffer[i] = '<';
    }
    buffer[strength+1] = '\0';
  }

  return buffer;
}



static void logFailure (const char *platform, const char *test,
                        const UChar *source, const uint32_t sLen,
                        const UChar *target, const uint32_t tLen,
                        UCollationResult realRes, uint32_t realStrength,
                        UCollationResult expRes, uint32_t expStrength, UBool error) {

  uint32_t i = 0;

  char sEsc[256], s[256], tEsc[256], t[256], b[256], output[256], relation[256];

  *sEsc = *tEsc = *s = *t = 0;
  if(error == TRUE) {
    log_err("Difference between expected and generated order. Run test with -v for more info\n");
  }
  for(i = 0; i<sLen; i++) {
    sprintf(b, "%04X", source[i]);
    strcat(sEsc, "\\u");
    strcat(sEsc, b);
    strcat(s, b);
    strcat(s, " ");
    if(source[i] < 0x80) {
      sprintf(b, "(%c)", source[i]);
      strcat(sEsc, b);
    }
  }
  for(i = 0; i<tLen; i++) {
    sprintf(b, "%04X", target[i]);
    strcat(tEsc, "\\u");
    strcat(tEsc, b);
    strcat(t, b);
    strcat(t, " ");
    if(target[i] < 0x80) {
      sprintf(b, "(%c)", target[i]);
      strcat(tEsc, b);
    }
  }
/*
  strcpy(output, "[[ ");
  strcat(output, sEsc);
  strcat(output, getRelationSymbol(expRes, expStrength, relation));
  strcat(output, tEsc);

  strcat(output, " : ");

  strcat(output, sEsc);
  strcat(output, getRelationSymbol(realRes, realStrength, relation));
  strcat(output, tEsc);
  strcat(output, " ]] ");

  log_verbose("%s", output);
*/


  strcpy(output, "DIFF: ");

  strcat(output, s);
  strcat(output, " : ");
  strcat(output, t);

  strcat(output, test);
  strcat(output, ": ");

  strcat(output, sEsc);
  strcat(output, getRelationSymbol(expRes, expStrength, relation));
  strcat(output, tEsc);

  strcat(output, " ");

  strcat(output, platform);
  strcat(output, ": ");

  strcat(output, sEsc);
  strcat(output, getRelationSymbol(realRes, realStrength, relation));
  strcat(output, tEsc);

  log_verbose("%s\n", output);

}

/*
static void printOutRules(const UChar *rules) {
  uint32_t len = u_strlen(rules);
  uint32_t i = 0;
  char toPrint;
  uint32_t line = 0;

  fprintf(stdout, "Rules:");

  for(i = 0; i<len; i++) {
    if(rules[i]<0x7f && rules[i]>=0x20) {
      toPrint = (char)rules[i];
      if(toPrint == '&') {
        line = 1;
        fprintf(stdout, "\n&");
      } else if(toPrint == ';') {
        fprintf(stdout, "<<");
        line+=2;
      } else if(toPrint == ',') {
        fprintf(stdout, "<<<");
        line+=3;
      } else {
        fprintf(stdout, "%c", toPrint);
        line++;
      }
    } else if(rules[i]<0x3400 || rules[i]>=0xa000) {
      fprintf(stdout, "\\u%04X", rules[i]);
      line+=6;
    }
    if(line>72) {
      fprintf(stdout, "\n");
      line = 0;
    }
  }

  log_verbose("\n");

}
*/

static uint32_t testSwitch(tst_strcoll* func, void *collator, int opts, uint32_t strength, const UChar *first, const UChar *second, const char* msg, UBool error) {
  uint32_t diffs = 0;
  UCollationResult realResult;
  uint32_t realStrength;

  uint32_t sLen = u_strlen(first);
  uint32_t tLen = u_strlen(second);

  realResult = func(collator, opts, first, sLen, second, tLen);
  realStrength = probeStrength(func, collator, opts, first, sLen, second, tLen, realResult);

  if(strength == UCOL_IDENTICAL && realResult != UCOL_IDENTICAL) {
    logFailure(msg, "tailoring", first, sLen, second, tLen, realResult, realStrength, UCOL_EQUAL, strength, error);
    diffs++;
  } else if(realResult != UCOL_LESS || realStrength != strength) {
    logFailure(msg, "tailoring", first, sLen, second, tLen, realResult, realStrength, UCOL_LESS, strength, error);
    diffs++;
  }
  return diffs;
}


static void testAgainstUCA(UCollator *coll, UCollator *UCA, const char *refName, UBool error, UErrorCode *status) {
  const UChar *rules = NULL, *current = NULL;
  int32_t ruleLen = 0;
  uint32_t strength = 0;
  uint32_t chOffset = 0; uint32_t chLen = 0;
  uint32_t exOffset = 0; uint32_t exLen = 0;
  uint32_t prefixOffset = 0; uint32_t prefixLen = 0;
/*  uint32_t rExpsLen = 0; */
  uint32_t firstLen = 0, secondLen = 0;
  UBool varT = FALSE; UBool top_ = TRUE;
  uint8_t specs = 0;
  UBool startOfRules = TRUE;
  UColTokenParser src;
  UColOptionSet opts;

  UChar first[256];
  UChar second[256];
  UChar *rulesCopy = NULL;

  uint32_t UCAdiff = 0;
  uint32_t Windiff = 1;
  UParseError parseError;

  src.opts = &opts;

  rules = ucol_getRules(coll, &ruleLen);

  /*printOutRules(rules);*/

  if(U_SUCCESS(*status) && ruleLen > 0) {
    rulesCopy = (UChar *)uprv_malloc((ruleLen+UCOL_TOK_EXTRA_RULE_SPACE_SIZE)*sizeof(UChar));
    uprv_memcpy(rulesCopy, rules, ruleLen*sizeof(UChar));
    src.source = src.current = rulesCopy;
    src.end = rulesCopy+ruleLen;
    src.extraCurrent = src.end;
    src.extraEnd = src.end+UCOL_TOK_EXTRA_RULE_SPACE_SIZE;
    *first = *second = 0;

    while ((current = ucol_tok_parseNextToken(&src, &strength,
                      &chOffset, &chLen, &exOffset, &exLen,
                      &prefixOffset, &prefixLen,
                      &specs, startOfRules, &parseError,status)) != NULL) {
      startOfRules = FALSE;
      varT = (UBool)((specs & UCOL_TOK_VARIABLE_TOP) != 0);
      top_ = (UBool)((specs & UCOL_TOK_TOP) != 0);

      u_strncpy(second,rulesCopy+chOffset, chLen);
      second[chLen] = 0;
      secondLen = chLen;

      if(exLen > 0) {
        u_strncat(first, rulesCopy+exOffset, exLen);
        first[firstLen+exLen] = 0;
        firstLen += exLen;
      }

      if(strength != UCOL_TOK_RESET) {
        if((*first<0x3400 || *first>=0xa000) && (*second<0x3400 || *second>=0xa000)) {
          UCAdiff += testSwitch(&ucaTest, (void *)UCA, 0, strength, first, second, refName, error);
          /*Windiff += testSwitch(&winTest, (void *)lcid, 0, strength, first, second, "Win32");*/
        }
      }


      firstLen = chLen;
      u_strcpy(first, second);

    }
    if(UCAdiff != 0 && Windiff != 0) {
      log_verbose("\n");
    }
    if(UCAdiff == 0) {
      log_verbose("No immediate difference with %s!\n", refName);
    }
    if(Windiff == 0) {
      log_verbose("No immediate difference with Win32!\n");
    }
    uprv_free(rulesCopy);
  }
}

static void testCEs(UCollator *coll, UErrorCode *status) {

  const UChar *rules = NULL, *current = NULL;
  int32_t ruleLen = 0;

  uint32_t strength = 0;
  uint32_t maxStrength = UCOL_IDENTICAL;
  uint32_t baseCE, baseContCE, nextCE, nextContCE, currCE, currContCE;
  uint32_t lastCE;
  uint32_t lastContCE;

  int32_t result = 0;
  uint32_t chOffset = 0; uint32_t chLen = 0;
  uint32_t exOffset = 0; uint32_t exLen = 0;
  uint32_t prefixOffset = 0; uint32_t prefixLen = 0;
  uint32_t oldOffset = 0;

  /* uint32_t rExpsLen = 0; */
  /* uint32_t firstLen = 0; */
  uint8_t specs = 0;
  UBool varT = FALSE; UBool top_ = TRUE;
  UBool startOfRules = TRUE;
  UColTokenParser src;
  UColOptionSet opts;
  UParseError parseError;
  UChar *rulesCopy = NULL;
  collIterate c;

  baseCE=baseContCE=nextCE=nextContCE=currCE=currContCE=lastCE=lastContCE = UCOL_NOT_FOUND;

  src.opts = &opts;

  rules = ucol_getRules(coll, &ruleLen);

  ucol_initInverseUCA(status);

  if(U_SUCCESS(*status) && ruleLen > 0) {
    rulesCopy = (UChar *)uprv_malloc((ruleLen+UCOL_TOK_EXTRA_RULE_SPACE_SIZE)*sizeof(UChar));
    uprv_memcpy(rulesCopy, rules, ruleLen*sizeof(UChar));
    src.source = src.current = rulesCopy;
    src.end = rulesCopy+ruleLen;
    src.extraCurrent = src.end;
    src.extraEnd = src.end+UCOL_TOK_EXTRA_RULE_SPACE_SIZE;

    while ((current = ucol_tok_parseNextToken(&src, &strength,
                      &chOffset, &chLen, &exOffset, &exLen,
                      &prefixOffset, &prefixLen,
                      &specs, startOfRules, &parseError,status)) != NULL) {
      startOfRules = FALSE;
      varT = (UBool)((specs & UCOL_TOK_VARIABLE_TOP) != 0);
      top_ = (UBool)((specs & UCOL_TOK_TOP) != 0);

      init_collIterate(coll, rulesCopy+chOffset, chLen, &c);

      currCE = ucol_getNextCE(coll, &c, status);
      if(currCE == 0 && UCOL_ISTHAIPREVOWEL(*(rulesCopy+chOffset))) {
        log_verbose("Thai prevowel detected. Will pick next CE\n");
        currCE = ucol_getNextCE(coll, &c, status);
      }

      currContCE = ucol_getNextCE(coll, &c, status);
      if(!isContinuation(currContCE)) {
        currContCE = 0;
      }

      if(strength == UCOL_TOK_RESET) {
        if(top_ == TRUE) {
          nextCE = baseCE = currCE = UCOL_RESET_TOP_VALUE;
          nextContCE = baseContCE = currContCE = 0;
        } else {
          nextCE = baseCE = currCE;
          nextContCE = baseContCE = currContCE;
        }
        maxStrength = UCOL_IDENTICAL;
      } else {
        if(strength < maxStrength) {
          maxStrength = strength;
          if(baseCE == UCOL_RESET_TOP_VALUE) {
              log_verbose("Resetting to [top]\n");
              nextCE = UCOL_NEXT_TOP_VALUE;
              nextContCE = 0;
          } else {
            result = ucol_inv_getNextCE(baseCE & 0xFFFFFF3F, baseContCE, &nextCE, &nextContCE, maxStrength);
          }
          if(result < 0) {
            if(isTailored(coll, *(rulesCopy+oldOffset), status)) {
              log_verbose("Reset is tailored codepoint %04X, don't know how to continue, taking next test\n", *(rulesCopy+oldOffset));
              return;
            } else {
              log_err("couldn't find the CE\n");
              return;
            }
          }
        }

        currCE &= 0xFFFFFF3F;
        currContCE &= 0xFFFFFFBF;

        if(maxStrength == UCOL_IDENTICAL) {
          if(baseCE != currCE || baseContCE != currContCE) {
            log_err("current CE  (initial strength UCOL_EQUAL)\n");
          }
        } else {
          if(strength == UCOL_IDENTICAL) {
            if(lastCE != currCE || lastContCE != currContCE) {
              log_err("current CE  (initial strength UCOL_EQUAL)\n");
            }
          } else {
            if(currCE > nextCE || (currCE == nextCE && currContCE >= nextContCE)) {
              log_err("current CE is not less than base CE\n");
            }
            if(currCE < lastCE || (currCE == lastCE && currContCE <= lastContCE)) {
              log_err("sequence of generated CEs is broken\n");
            }
          }
        }

      }

      oldOffset = chOffset;
      lastCE = currCE & 0xFFFFFF3F;
      lastContCE = currContCE & 0xFFFFFFBF;
    }
    uprv_free(rulesCopy);
  }
}

#if 0
/* these locales are now picked from index RB */
static const char* localesToTest[] = {
"ar", "bg", "ca", "cs", "da",
"el", "en_BE", "en_US_POSIX",
"es", "et", "fi", "fr", "hi",
"hr", "hu", "is", "iw", "ja",
"ko", "lt", "lv", "mk", "mt",
"nb", "nn", "nn_NO", "pl", "ro",
"ru", "sh", "sk", "sl", "sq",
"sr", "sv", "th", "tr", "uk",
"vi", "zh", "zh_TW"
};
#endif

static const char* rulesToTest[] = {
  /*"& Z < p, P",*/
    /* Cui Mins rules */
    "<o,O<p,P<q,Q<'?'/u<r,R<u,U", /*"<o,O<p,P<q,Q<r,R<u,U & Qu<'?'",*/
    "<o,O<p,P<q,Q;'?'/u<r,R<u,U", /*"<o,O<p,P<q,Q<r,R<u,U & Qu;'?'",*/
    "<o,O<p,P<q,Q,'?'/u<r,R<u,U", /*"<o,O<p,P<q,Q<r,R<u,U&'Qu','?'",*/
    "<3<4<5<c,C<f,F<m,M<o,O<p,P<q,Q;'?'/u<r,R<u,U",  /*"<'?'<3<4<5<a,A<f,F<m,M<o,O<p,P<q,Q<r,R<u,U & Qu;'?'",*/
    "<'?';Qu<3<4<5<c,C<f,F<m,M<o,O<p,P<q,Q<r,R<u,U",  /*"<'?'<3<4<5<a,A<f,F<m,M<o,O<p,P<q,Q<r,R<u,U & '?';Qu",*/
    "<3<4<5<c,C<f,F<m,M<o,O<p,P<q,Q;'?'/um<r,R<u,U", /*"<'?'<3<4<5<a,A<f,F<m,M<o,O<p,P<q,Q<r,R<u,U & Qum;'?'",*/
    "<'?';Qum<3<4<5<c,C<f,F<m,M<o,O<p,P<q,Q<r,R<u,U"  /*"<'?'<3<4<5<a,A<f,F<m,M<o,O<p,P<q,Q<r,R<u,U & '?';Qum"*/
};

static UBool hasCollationElements(const char *locName) {

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


static void TestCollations( ) {
  int32_t noOfLoc = uloc_countAvailable();
  int32_t i = 0, j = 0;

  UErrorCode status = U_ZERO_ERROR;
  char cName[256];
  UChar name[256];
  int32_t nameSize;


  const char *locName = NULL;
  UCollator *coll = NULL;
  UCollator *UCA = ucol_open("", &status);
  UColAttributeValue oldStrength = ucol_getAttribute(UCA, UCOL_STRENGTH, &status);
  ucol_setAttribute(UCA, UCOL_STRENGTH, UCOL_QUATERNARY, &status);

  for(i = 0; i<noOfLoc; i++) {
    status = U_ZERO_ERROR;
    locName = uloc_getAvailable(i);
    if(hasCollationElements(locName)) {
        nameSize = uloc_getDisplayName(locName, NULL, name, 256, &status);
        for(j = 0; j<nameSize; j++) {
          cName[j] = (char)name[j];
        }
        cName[nameSize] = 0;
        log_verbose("\nTesting locale %s (%s)\n", locName, cName);
        coll = ucol_open(locName, &status);
        testAgainstUCA(coll, UCA, "UCA", FALSE, &status);
        ucol_close(coll);
    }
  }
  ucol_setAttribute(UCA, UCOL_STRENGTH, oldStrength, &status);
  ucol_close(UCA);
}

static void RamsRulesTest( ) {
  UErrorCode status = U_ZERO_ERROR;
  int32_t i = 0;
  UCollator *coll = NULL;
/*  UCollator *UCA = ucol_open("", &status); */
  UChar rule[2048];
  uint32_t ruleLen;
  int32_t noOfLoc = uloc_countAvailable();
  const char *locName = NULL;

  log_verbose("RamsRulesTest\n");

  for(i = 0; i<noOfLoc; i++) {
    status = U_ZERO_ERROR;
    locName = uloc_getAvailable(i);
    if(hasCollationElements(locName)) {
      log_verbose("Testing locale %s\n", locName);
      coll = ucol_open(locName, &status);
      if(U_SUCCESS(status)) {
        if(coll->image->jamoSpecial == TRUE) {
          log_err("%s has special JAMOs\n", locName);
        }
        ucol_setAttribute(coll, UCOL_CASE_FIRST, UCOL_OFF, &status);
        testCollator(coll, &status);
        testCEs(coll, &status);
        ucol_close(coll);
      }
    }
  }

  for(i = 0; i<sizeof(rulesToTest)/sizeof(rulesToTest[0]); i++) {
    log_verbose("Testing rule: %s\n", rulesToTest[i]);
    u_uastrcpy(rule, rulesToTest[i]);
    ruleLen = u_strlen(rule);
    coll = ucol_openRules(rule, ruleLen, UCOL_OFF, UCOL_TERTIARY, NULL,&status);
    if(U_SUCCESS(status)) {
      testCollator(coll, &status);
      testCEs(coll, &status);
      ucol_close(coll);
    }
  }

}

static void IsTailoredTest( ) {
  UErrorCode status = U_ZERO_ERROR;
  uint32_t i = 0;
  UCollator *coll = NULL;
  UChar rule[2048];
  UChar tailored[2048];
  UChar notTailored[2048];
  uint32_t ruleLen, tailoredLen, notTailoredLen;

  log_verbose("IsTailoredTest\n");

  u_uastrcpy(rule, "&Z < A, B, C;c < d");
  ruleLen = u_strlen(rule);

  u_uastrcpy(tailored, "ABCcd");
  tailoredLen = u_strlen(tailored);

  u_uastrcpy(notTailored, "ZabD");
  notTailoredLen = u_strlen(notTailored);

  coll = ucol_openRules(rule, ruleLen, UCOL_OFF, UCOL_TERTIARY, NULL,&status);
  if(U_SUCCESS(status)) {
    for(i = 0; i<tailoredLen; i++) {
      if(!isTailored(coll, tailored[i], &status)) {
        log_err("%i: %04X should be tailored - it is reported as not\n", i, tailored[i]);
      }
    }
    for(i = 0; i<notTailoredLen; i++) {
      if(isTailored(coll, notTailored[i], &status)) {
        log_err("%i: %04X should not be tailored - it is reported as it is\n", i, notTailored[i]);
      }
    }
    ucol_close(coll);
  }
}

static void genericOrderingTestWithResult(UCollator *coll, const char *s[], uint32_t size, UCollationResult result) {
  UChar t1[2048] = {0};
  UChar t2[2048] = {0};
  UCollationElements *iter;
  UErrorCode status = U_ZERO_ERROR;

  uint32_t i = 0, j = 0;
  log_verbose("testing sequence:\n");
  for(i = 0; i < size; i++) {
    log_verbose("%s\n", s[i]);
  }

  iter = ucol_openElements(coll, t1, u_strlen(t1), &status);
  if (U_FAILURE(status)) {
    log_err("Creation of iterator failed\n");
  }
  for(i = 0; i < size-1; i++) {
    for(j = i+1; j < size; j++) {
      u_unescape(s[i], t1, 2048);
      u_unescape(s[j], t2, 2048);
      doTest(coll, t1, t2, result);
      /* synwee : added collation element iterator test */
      ucol_setText(iter, t1, u_strlen(t1), &status);
      backAndForth(iter);
      ucol_setText(iter, t2, u_strlen(t2), &status);
      backAndForth(iter);
    }
  }
  ucol_closeElements(iter);
}

static void genericOrderingTest(UCollator *coll, const char *s[], uint32_t size) {
  genericOrderingTestWithResult(coll, s, size, UCOL_LESS);
}

static void genericLocaleStarter(const char *locale, const char *s[], uint32_t size) {
  UErrorCode status = U_ZERO_ERROR;
  UCollator *coll = ucol_open(locale, &status);

  log_verbose("Locale starter for %s\n", locale);

  if(U_SUCCESS(status)) {
    genericOrderingTest(coll, s, size);
  } else {
    log_err("Unable to open collator for locale %s\n", locale);
  }
  ucol_close(coll);
}

static void genericRulesStarterWithOptions(const char *rules, const char *s[], uint32_t size, const UColAttribute *attrs, const UColAttributeValue *values, uint32_t attsize) {
  UErrorCode status = U_ZERO_ERROR;
  UChar rlz[RULE_BUFFER_LEN] = { 0 };
  uint32_t rlen = u_unescape(rules, rlz, RULE_BUFFER_LEN);
  uint32_t i;

  UCollator *coll = ucol_openRules(rlz, rlen, UCOL_DEFAULT, UCOL_DEFAULT,NULL, &status);

  log_verbose("Rules starter for %s\n", rules);

  log_verbose("Setting attributes\n");
  for(i = 0; i < attsize; i++) {
    ucol_setAttribute(coll, attrs[i], values[i], &status);
  }

  if(U_SUCCESS(status)) {
    genericOrderingTest(coll, s, size);
  } else {
    log_err("Unable to open collator with rules %s\n", rules);
  }
  ucol_close(coll);
}

static void genericRulesStarter(const char *rules, const char *s[], uint32_t size) {
  UErrorCode status = U_ZERO_ERROR;
  UChar rlz[RULE_BUFFER_LEN] = { 0 };
  uint32_t rlen = u_unescape(rules, rlz, RULE_BUFFER_LEN);

  UCollator *coll = NULL;
  coll = ucol_openRules(rlz, rlen, UCOL_DEFAULT, UCOL_DEFAULT,NULL, &status);
  log_verbose("Rules starter for %s\n", rules);

  if(U_SUCCESS(status)) {
    genericOrderingTest(coll, s, size);
    ucol_close(coll);
  } else {
    log_err("Unable to open collator with rules %s\n", rules);
  }
}

const static char chTest[][20] = {
  "c",
  "C",
  "ca", "cb", "cx", "cy", "CZ",
  "c\\u030C", "C\\u030C",
  "h",
  "H",
  "ha", "Ha", "harly", "hb", "HB", "hx", "HX", "hy", "HY",
  "ch", "cH", "Ch", "CH",
  "cha", "charly", "che", "chh", "chch", "chr",
  "i", "I", "iarly",
  "r", "R",
  "r\\u030C", "R\\u030C",
  "s",
  "S",
  "s\\u030C", "S\\u030C",
  "z", "Z",
  "z\\u030C", "Z\\u030C"
};

static void TestChMove(void) {
  UChar t1[256] = {0};
  UChar t2[256] = {0};

  uint32_t i = 0, j = 0;
  uint32_t size = 0;
  UErrorCode status = U_ZERO_ERROR;

  UCollator *coll = ucol_open("cs", &status);

  if(U_SUCCESS(status)) {
    size = sizeof(chTest)/sizeof(chTest[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        u_unescape(chTest[i], t1, 256);
        u_unescape(chTest[j], t2, 256);
        doTest(coll, t1, t2, UCOL_LESS);
      }
    }
  }
}

const static char impTest[][20] = {
  "\\u4e00",
    "a",
    "A",
    "b",
    "B",
    "\\u4e01"
};


static void TestImplicitTailoring(void) {
  UChar t1[256] = {0};
  UChar t2[256] = {0};

  const char *rule = "&\\u4e00 < a <<< A < b <<< B";

  uint32_t i = 0, j = 0;
  uint32_t size = 0;
  uint32_t ruleLen = 0;
  UErrorCode status = U_ZERO_ERROR;
  UCollator *coll = NULL;
  ruleLen = u_unescape(rule, t1, 256);

  coll = ucol_openRules(t1, ruleLen, UCOL_OFF, UCOL_TERTIARY,NULL, &status);

  if(U_SUCCESS(status)) {
    size = sizeof(impTest)/sizeof(impTest[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        u_unescape(impTest[i], t1, 256);
        u_unescape(impTest[j], t2, 256);
        doTest(coll, t1, t2, UCOL_LESS);
      }
    }
  }
}

static void TestFCDProblem(void) {
  UChar t1[256] = {0};
  UChar t2[256] = {0};

  const char *s1 = "\\u0430\\u0306\\u0325";
  const char *s2 = "\\u04D1\\u0325";

  UErrorCode status = U_ZERO_ERROR;
  UCollator *coll = ucol_open("", &status);
  u_unescape(s1, t1, 256);
  u_unescape(s2, t2, 256);

  ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
  doTest(coll, t1, t2, UCOL_EQUAL);

  ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
  doTest(coll, t1, t2, UCOL_EQUAL);

}

#define NORM_BUFFER_TEST_LEN 32
typedef struct {
  UChar u;
  UChar NFC[NORM_BUFFER_TEST_LEN];
  UChar NFD[NORM_BUFFER_TEST_LEN];
} tester;

static void TestComposeDecompose(void) {
    int32_t noOfLoc = uloc_countAvailable();
    int32_t i = 0, j = 0;

    UErrorCode status = U_ZERO_ERROR;

    const char *locName = NULL;

    UChar u = 0;
    /*
    UChar NFC[256] = {0};
    UChar NFD[256] = {0};
    */
    uint32_t nfcSize;
    uint32_t nfdSize;
    tester **t = uprv_malloc(0xFFFF * sizeof(tester *));
    uint32_t noCases = 0;
    UCollator *coll = NULL;

    t[0] = (tester *)uprv_malloc(sizeof(tester));

    for(u = 0; u < 0xFFFF; u++) {
        nfcSize = unorm_normalize(&u, 1, UNORM_NFC, 0, t[noCases]->NFC, NORM_BUFFER_TEST_LEN, &status);
        nfdSize = unorm_normalize(&u, 1, UNORM_NFD, 0, t[noCases]->NFD, NORM_BUFFER_TEST_LEN, &status);

        if(nfcSize != nfdSize || (uprv_memcmp(t[noCases]->NFC, t[noCases]->NFD, nfcSize * sizeof(UChar)) != 0)) {
            t[noCases]->u = u;
            noCases++;
            t[noCases] = (tester *)uprv_malloc(sizeof(tester));
            uprv_memset(t[noCases], 0, sizeof(tester));
        }
    }

    log_verbose("Testing UCA extensively\n");
    coll = ucol_open("", &status);

    for(u=0; u<noCases; u++) {
        doTest(coll, t[u]->NFC, t[u]->NFD, UCOL_EQUAL);
    }

    ucol_close(coll);

    for(i = 0; i<noOfLoc; i++) {
        status = U_ZERO_ERROR;
        locName = uloc_getAvailable(i);
        if(hasCollationElements(locName)) {
            char cName[256];
            UChar name[256];
            int32_t nameSize = uloc_getDisplayName(locName, NULL, name, sizeof(cName), &status);

            for(j = 0; j<nameSize; j++) {
                cName[j] = (char)name[j];
            }
            cName[nameSize] = 0;
            log_verbose("\nTesting locale %s (%s)\n", locName, cName);

            coll = ucol_open(locName, &status);

            for(u=0; u<noCases; u++) {
              if(!ucol_equal(coll, t[u]->NFC, -1, t[u]->NFD, -1)) {
                log_err("Failure: codePoint %04X fails TestComposeDecompose for locale %s\n", t[u]->u, cName);
                /*doTest(coll, t[u]->NFC, t[u]->NFD, UCOL_EQUAL);*/
              }
            }

            ucol_close(coll);

        }
    }
    for(u = 0; u <= noCases; u++) {
        uprv_free(t[u]);
    }
    uprv_free(t);
}

static void TestEmptyRule() {
  UErrorCode status = U_ZERO_ERROR;
  UChar rulez[] = { 0 };
  UCollator *coll = ucol_openRules(rulez, 0, UCOL_OFF, UCOL_TERTIARY,NULL, &status);

  ucol_close(coll);
}

static void TestUCARules() {
  UErrorCode status = U_ZERO_ERROR;
  UChar b[256];
  UChar *rules = b;
  UCollator *UCAfromRules = NULL;
  UCollator *coll = ucol_open("", &status);
  uint32_t ruleLen = ucol_getRulesEx(coll, UCOL_FULL_RULES, rules, 256);

  log_verbose("TestUCARules\n");
  if(ruleLen > 256) {
    rules = (UChar *)malloc((ruleLen+1)*sizeof(UChar));
    ruleLen = ucol_getRulesEx(coll, UCOL_FULL_RULES, rules, ruleLen);
  }
  log_verbose("Rules length is %d\n", ruleLen);
  UCAfromRules = ucol_openRules(rules, ruleLen, UCOL_OFF, UCOL_TERTIARY, NULL,&status);
  if(U_SUCCESS(status)) {
    ucol_close(UCAfromRules);
  } else {
    log_verbose("Unable to create a collator from UCARules!\n");
  }
/*
  u_unescape(blah, b, 256);
  ucol_getSortKey(coll, b, 1, res, 256);
*/
  ucol_close(coll);
  if(rules != b) {
    free(rules);
  }
}


/* Pinyin tonal order */
/*
    A < .. (\u0101) < .. (\u00e1) < .. (\u01ce) < .. (\u00e0)
          (w/macron)<  (w/acute)<   (w/caron)<   (w/grave)
    E < .. (\u0113) < .. (\u00e9) < .. (\u011b) < .. (\u00e8)
    I < .. (\u012b) < .. (\u00ed) < .. (\u01d0) < .. (\u00ec)
    O < .. (\u014d) < .. (\u00f3) < .. (\u01d2) < .. (\u00f2)
    U < .. (\u016b) < .. (\u00fa) < .. (\u01d4) < .. (\u00f9)
      < .. (\u01d6) < .. (\u01d8) < .. (\u01da) < .. (\u01dc) <
.. (\u00fc)

However, in testing we got the following order:
    A < .. (\u00e1) < .. (\u00e0) < .. (\u01ce) < .. (\u0101)
          (w/acute)<   (w/grave)<   (w/caron)<   (w/macron)
    E < .. (\u00e9) < .. (\u00e8) < .. (\u00ea) < .. (\u011b) <
.. (\u0113)
    I < .. (\u00ed) < .. (\u00ec) < .. (\u01d0) < .. (\u012b)
    O < .. (\u00f3) < .. (\u00f2) < .. (\u01d2) < .. (\u014d)
    U < .. (\u00fa) < .. (\u00f9) < .. (\u01d4) < .. (\u00fc) <
.. (\u01d8)
      < .. (\u01dc) < .. (\u01da) < .. (\u01d6) < .. (\u016b)
*/

static void TestBefore() {
  const static char *data[] = {
      "\\u0101", "\\u00e1", "\\u01ce", "\\u00e0", "A",
      "\\u0113", "\\u00e9", "\\u011b", "\\u00e8", "E",
      "\\u012b", "\\u00ed", "\\u01d0", "\\u00ec", "I",
      "\\u014d", "\\u00f3", "\\u01d2", "\\u00f2", "O",
      "\\u016b", "\\u00fa", "\\u01d4", "\\u00f9", "U",
      "\\u01d6", "\\u01d8", "\\u01da", "\\u01dc", "\\u00fc"
  };
  genericRulesStarter(
    "&[before 1]a<\\u0101<\\u00e1<\\u01ce<\\u00e0"
    "&[before 1]e<\\u0113<\\u00e9<\\u011b<\\u00e8"
    "&[before 1]i<\\u012b<\\u00ed<\\u01d0<\\u00ec"
    "&[before 1]o<\\u014d<\\u00f3<\\u01d2<\\u00f2"
    "&[before 1]u<\\u016b<\\u00fa<\\u01d4<\\u00f9"
    "&u<\\u01d6<\\u01d8<\\u01da<\\u01dc<\\u00fc",
    data, sizeof(data)/sizeof(data[0]));
}

static void TestJ784() {
  const static char *data[] = {
      "A", "\\u0101", "\\u00e1", "\\u01ce", "\\u00e0",
      "E", "\\u0113", "\\u00e9", "\\u011b", "\\u00e8",
      "I", "\\u012b", "\\u00ed", "\\u01d0", "\\u00ec",
      "O", "\\u014d", "\\u00f3", "\\u01d2", "\\u00f2",
      "U", "\\u016b", "\\u00fa", "\\u01d4", "\\u00f9",
      "\\u00fc",
           "\\u01d6", "\\u01d8", "\\u01da", "\\u01dc"
  };
  genericLocaleStarter("zh", data, sizeof(data)/sizeof(data[0]));
}


static void TestJ831() {
  const static char *data[] = {
    "I",
      "i",
      "Y",
      "y"
  };
  genericLocaleStarter("lv", data, sizeof(data)/sizeof(data[0]));
}

static void TestJ815() {
  const static char *data[] = {
    "aa",
      "Aa",
      "ab",
      "Ab",
      "ad",
      "Ad",
      "ae",
      "Ae",
      "\\u00e6",
      "\\u00c6",
      "af",
      "Af",
      "b",
      "B"
  };
  genericLocaleStarter("fr", data, sizeof(data)/sizeof(data[0]));
  genericRulesStarter("[backwards 2]&A<<\\u00e6/e<<<\\u00c6/E", data, sizeof(data)/sizeof(data[0]));
}


/*
"& a < b < c < d& r < c",                                   "& a < b < d& r < c",
"& a < b < c < d& c < m",                                   "& a < b < c < m < d",
"& a < b < c < d& a < m",                                   "& a < m < b < c < d",
"& a <<< b << c < d& a < m",                                "& a <<< b << c < m < d",
"& a < b < c < d& [before 1] c < m",                        "& a < b < m < c < d",
"& a < b <<< c << d <<< e& [before 3] e <<< x",            "& a < b <<< c << d <<< x <<< e",
"& a < b <<< c << d <<< e& [before 2] e <<< x",            "& a < b <<< c <<< x << d <<< e",
"& a < b <<< c << d <<< e& [before 1] e <<< x",            "& a <<< x < b <<< c << d <<< e",
"& a < b <<< c << d <<< e <<< f < g& [before 1] g < x",    "& a < b <<< c << d <<< e <<< f < x < g",
*/
static void TestRedundantRules() {
  int32_t i;

  const static char *rules[] = {
    "& a <<< b <<< c << d <<< e& [before 1] e <<< x",
    "& a < b <<< c << d <<< e& [before 1] e <<< x",
    "& a < b < c < d& [before 1] c < m",
    "& a < b <<< c << d <<< e& [before 3] e <<< x",
    "& a < b <<< c << d <<< e& [before 2] e <<< x",
    "& a < b <<< c << d <<< e <<< f < g& [before 1] g < x",
    "& a <<< b << c < d& a < m",
    "&a<b<<b\\u0301 &z<b",
    "&z<m<<<q<<<m",
    "&z<<<m<q<<<m",
    "& a < b < c < d& r < c",
    "& a < b < c < d& r < c",
    "& a < b < c < d& c < m",
    "& a < b < c < d& a < m"
  };

  const static char *expectedRules[] = {
    "&\\u3029<<<x",
    "& a <<< x < b <<< c << d <<< e",
    "& a < b < m < c < d",
    "& a < b <<< c << d <<< x <<< e",
    "& a < b <<< c <<< x << d <<< e",
    "& a < b <<< c << d <<< e <<< f < x < g",
    "& a <<< b << c < m < d",
    "&a<b\\u0301 &z<b",
    "&z<q<<<m",
    "&z<q<<<m",
    "& a < b < d& r < c",
    "& a < b < d& r < c",
    "& a < b < c < m < d",
    "& a < m < b < c < d"
  };

  const static char *testdata[][8] = {
    {"\\u3029", "x"},
    {"a", "x", "b", "c", "d", "e"},
    {"a", "b", "m", "c", "d"},
    {"a", "b", "c", "d", "x", "e"},
    {"a", "b", "c", "x", "d", "e"},
    {"a", "b", "c", "d", "e", "f", "x", "g"},
    {"a", "b", "c", "m", "d"},
    {"a", "b\\u0301", "z", "b"},
    {"z", "q", "m"},
    {"z", "q", "m"},
    {"a", "b", "d"},
    {"r", "c"},
    {"a", "b", "c", "m", "d"},
    {"a", "m", "b", "c", "d"}
  };

  const static uint32_t testdatalen[] = {
    2,
      6,
      5,
      6,
      6,
      8,
      5,
      4,
      3,
      3,
      3,
      2,
      5,
      5
  };



  UCollator *credundant = NULL;
  UCollator *cresulting = NULL;
  UErrorCode status = U_ZERO_ERROR;
  UChar rlz[2048] = { 0 };
  uint32_t rlen = 0;

  for(i = 0; i<sizeof(rules)/sizeof(rules[0]); i++) {
    log_verbose("testing rule %s, expected to be %s\n", rules[i], expectedRules[i]);
    rlen = u_unescape(rules[i], rlz, 2048);

    credundant = ucol_openRules(rlz, rlen, UCOL_DEFAULT, UCOL_DEFAULT, NULL,&status);
    rlen = u_unescape(expectedRules[i], rlz, 2048);
    cresulting = ucol_openRules(rlz, rlen, UCOL_DEFAULT, UCOL_DEFAULT, NULL,&status);

    testAgainstUCA(cresulting, credundant, "expected", TRUE, &status);

    ucol_close(credundant);
    ucol_close(cresulting);

    log_verbose("testing using data\n");

    genericRulesStarter(rules[i], testdata[i], testdatalen[i]);
  }

}

static void TestExpansionSyntax() {
  int32_t i;

  const static char *rules[] = {
    "&AE <<< a << b <<< c &d <<< f",
    "&AE <<< a <<< b << c << d < e < f <<< g",
    "&AE <<< B <<< C / D <<< F"
  };

  const static char *expectedRules[] = {
    "&A <<< a / E << b / E <<< c /E  &d <<< f",
    "&A <<< a / E <<< b / E << c / E << d / E < e < f <<< g",
    "&A <<< B / E <<< C / ED <<< F / E"
  };

  const static char *testdata[][8] = {
    {"AE", "a", "b", "c"},
    {"AE", "a", "b", "c", "d", "e", "f", "g"},
    {"AE", "B", "C"} /* / ED <<< F / E"},*/
  };

  const static uint32_t testdatalen[] = {
      4,
      8,
      3
  };



  UCollator *credundant = NULL;
  UCollator *cresulting = NULL;
  UErrorCode status = U_ZERO_ERROR;
  UChar rlz[2048] = { 0 };
  uint32_t rlen = 0;

  for(i = 0; i<sizeof(rules)/sizeof(rules[0]); i++) {
    log_verbose("testing rule %s, expected to be %s\n", rules[i], expectedRules[i]);
    rlen = u_unescape(rules[i], rlz, 2048);

    credundant = ucol_openRules(rlz, rlen, UCOL_DEFAULT, UCOL_DEFAULT, NULL, &status);
    rlen = u_unescape(expectedRules[i], rlz, 2048);
    cresulting = ucol_openRules(rlz, rlen, UCOL_DEFAULT, UCOL_DEFAULT, NULL,&status);

    /* testAgainstUCA still doesn't handle expansions correctly, so this is not run */
    /* as a hard error test, but only in information mode */
    testAgainstUCA(cresulting, credundant, "expected", FALSE, &status);

    ucol_close(credundant);
    ucol_close(cresulting);

    log_verbose("testing using data\n");

    genericRulesStarter(rules[i], testdata[i], testdatalen[i]);
  }
}

static void TestCase( )
{
    const static UChar gRules[MAX_TOKEN_LEN] =
    /*" & 0 < 1,\u2461<a,A"*/
    { 0x0026, 0x0030, 0x003C, 0x0031, 0x002C, 0x2460, 0x003C, 0x0061, 0x002C, 0x0041, 0x0000 };

    const static UChar testCase[][MAX_TOKEN_LEN] =
    {
        /*0*/ {0x0031 /*'1'*/, 0x0061/*'a'*/, 0x0000},
        /*1*/ {0x0031 /*'1'*/, 0x0041/*'A'*/, 0x0000},
        /*2*/ {0x2460 /*circ'1'*/, 0x0061/*'a'*/, 0x0000},
        /*3*/ {0x2460 /*circ'1'*/, 0x0041/*'A'*/, 0x0000}
    };

    const static UCollationResult caseTestResults[][9] =
    {
            { UCOL_LESS, UCOL_LESS, UCOL_LESS, 0, UCOL_LESS, UCOL_LESS, 0, 0, UCOL_LESS },
            { UCOL_GREATER, UCOL_LESS, UCOL_LESS, 0, UCOL_LESS, UCOL_LESS, 0, 0, UCOL_GREATER },
            { UCOL_LESS, UCOL_LESS, UCOL_LESS, 0, UCOL_GREATER, UCOL_LESS, 0, 0, UCOL_LESS },
            { UCOL_GREATER, UCOL_LESS, UCOL_GREATER, 0, UCOL_LESS, UCOL_LESS, 0, 0, UCOL_GREATER }

    };

    const static UColAttributeValue caseTestAttributes[][2] =
    {
            { UCOL_LOWER_FIRST, UCOL_OFF},
            { UCOL_UPPER_FIRST, UCOL_OFF},
            { UCOL_LOWER_FIRST, UCOL_ON},
            { UCOL_UPPER_FIRST, UCOL_ON}

    };
    int32_t i,j,k;
    UErrorCode status = U_ZERO_ERROR;
    UCollator  *myCollation;
    myCollation = ucol_open("en_US", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing different case settings\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);

    for(k = 0; k<4; k++) {
      ucol_setAttribute(myCollation, UCOL_CASE_FIRST, caseTestAttributes[k][0], &status);
      ucol_setAttribute(myCollation, UCOL_CASE_LEVEL, caseTestAttributes[k][1], &status);
      log_verbose("Case first = %d, Case level = %d\n", caseTestAttributes[k][0], caseTestAttributes[k][1]);
      for (i = 0; i < 3 ; i++) {
        for(j = i+1; j<4; j++) {
          doTest(myCollation, testCase[i], testCase[j], caseTestResults[k][3*i+j-1]);
        }
      }
    }
    ucol_close(myCollation);

    myCollation = ucol_openRules(gRules, u_strlen(gRules), UCOL_OFF, UCOL_TERTIARY,NULL, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing different case settings with custom rules\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);

    for(k = 0; k<4; k++) {
      ucol_setAttribute(myCollation, UCOL_CASE_FIRST, caseTestAttributes[k][0], &status);
      ucol_setAttribute(myCollation, UCOL_CASE_LEVEL, caseTestAttributes[k][1], &status);
      for (i = 0; i < 3 ; i++) {
        for(j = i+1; j<4; j++) {
          log_verbose("k:%d, i:%d, j:%d\n", k, i, j);
          doTest(myCollation, testCase[i], testCase[j], caseTestResults[k][3*i+j-1]);
        }
      }
    }
    ucol_close(myCollation);
    {
      const static char *lowerFirst[] = {
        "h",
        "H",
        "ch",
        "Ch",
        "CH",
        "cha",
        "chA",
        "Cha",
        "ChA",
        "CHa",
        "CHA",
        "i",
        "I"
      };

      const static char *upperFirst[] = {
        "H",
        "h",
        "CH",
        "Ch",
        "ch",
        "CHA",
        "CHa",
        "ChA",
        "Cha",
        "chA",
        "cha",
        "I",
        "i"
      };
      log_verbose("mixed case test\n");
      log_verbose("lower first, case level off\n");
      genericRulesStarter("[casefirst lower]&H<ch<<<Ch<<<CH", lowerFirst, sizeof(lowerFirst)/sizeof(lowerFirst[0]));
      log_verbose("upper first, case level off\n");
      genericRulesStarter("[casefirst upper]&H<ch<<<Ch<<<CH", upperFirst, sizeof(upperFirst)/sizeof(upperFirst[0]));
      log_verbose("lower first, case level on\n");
      genericRulesStarter("[casefirst lower][caselevel on]&H<ch<<<Ch<<<CH", lowerFirst, sizeof(lowerFirst)/sizeof(lowerFirst[0]));
      log_verbose("upper first, case level on\n");
      genericRulesStarter("[casefirst upper][caselevel on]&H<ch<<<Ch<<<CH", upperFirst, sizeof(upperFirst)/sizeof(upperFirst[0]));
    }

}

static void TestIncrementalNormalize() {

    UChar baseA     =0x41;
/*    UChar baseB     = 0x42;*/
    UChar ccMix[]   = {0x316, 0x321, 0x300};
    /*
        0x316 is combining grave accent below, cc=220
        0x321 is combining palatalized hook below, cc=202
        0x300 is combining grave accent, cc=230
    */

    int          maxSLen   = 2000;
    int          sLen;
    int          i;

    UCollator        *coll;
    UErrorCode       status = U_ZERO_ERROR;
    UCollationResult result;

    {
        /* Test 1.  Run very long unnormalized strings, to force overflow of*/
        /*          most buffers along the way.*/
        UChar            *strA;
        UChar            *strB;

        strA = uprv_malloc((maxSLen+1) * sizeof(UChar));
        strB = uprv_malloc((maxSLen+1) * sizeof(UChar));

        coll = ucol_open("en_US", &status);
        ucol_setNormalization(coll, UNORM_NFD);

        /* for (sLen = 4; sLen<maxSLen; sLen++) { */
        for (sLen = 1000; sLen<1001; sLen++) {
            strA[0] = baseA;
            strB[0] = baseA;
            for (i=1; i<=sLen-1; i++) {
                strA[i] = ccMix[i % 3];
                strB[sLen-i] = ccMix[i % 3];
            }
            strA[sLen]   = 0;
            strB[sLen]   = 0;

            ucol_setStrength(coll, UCOL_TERTIARY);   /* Do test with default strength, which runs*/
            doTest(coll, strA, strB, UCOL_EQUAL);    /*   optimized functions in the impl*/
            ucol_setStrength(coll, UCOL_IDENTICAL);   /* Do again with the slow, general impl.*/
            doTest(coll, strA, strB, UCOL_EQUAL);
        }
        uprv_free(strA);
        uprv_free(strB);
    }


    /*  Test 2:  Non-normal sequence in a string that extends to the last character*/
    /*         of the string.  Checks a couple of edge cases.*/

    {
        UChar strA[] = {0x41, 0x41, 0x300, 0x316, 0};
        UChar strB[] = {0x41, 0xc0, 0x316, 0};
        ucol_setStrength(coll, UCOL_TERTIARY);
        doTest(coll, strA, strB, UCOL_EQUAL);
    }

    /*  Test 3:  Non-normal sequence is terminated by a surrogate pair.*/

    {
        UChar strA[] = {0x41, 0x41, 0x300, 0x316, 0xD801, 0xDC00, 0};
        UChar strB[] = {0x41, 0xc0, 0x316, 0xD800, 0xDC00, 0};
        ucol_setStrength(coll, UCOL_TERTIARY);
        doTest(coll, strA, strB, UCOL_GREATER);
    }

    /*  Test 4:  Imbedded nulls do not terminate a string when length is specified.*/

    {
        UChar strA[] = {0x41, 0x00, 0x42, 0x00};
        UChar strB[] = {0x41, 0x00, 0x00, 0x00};
        char  sortKeyA[50];
        char  sortKeyAz[50];
        char  sortKeyB[50];
        char  sortKeyBz[50];
        int   r;

        result = ucol_strcoll(coll, strA, -3, strB, -3);
        if (result != UCOL_GREATER) {
            log_err("ERROR 1 in test 4\n");
        }
        result = ucol_strcoll(coll, strA, -1, strB, -1);
        if (result != UCOL_EQUAL) {
            log_err("ERROR 2 in test 4\n");
        }

        ucol_getSortKey(coll, strA,  3, (uint8_t *)sortKeyA, sizeof(sortKeyA));
        ucol_getSortKey(coll, strA, -1, (uint8_t *)sortKeyAz, sizeof(sortKeyAz));
        ucol_getSortKey(coll, strB,  3, (uint8_t *)sortKeyB, sizeof(sortKeyB));
        ucol_getSortKey(coll, strB, -1, (uint8_t *)sortKeyBz, sizeof(sortKeyBz));

        r = strcmp(sortKeyA, sortKeyAz);
        if (r <= 0) {
            log_err("Error 3 in test 4\n");
        }
        r = strcmp(sortKeyA, sortKeyB);
        if (r <= 0) {
            log_err("Error 4 in test 4\n");
        }
        r = strcmp(sortKeyAz, sortKeyBz);
        if (r != 0) {
            log_err("Error 5 in test 4\n");
        }

        ucol_setStrength(coll, UCOL_IDENTICAL);
        ucol_getSortKey(coll, strA,  3, (uint8_t *)sortKeyA, sizeof(sortKeyA));
        ucol_getSortKey(coll, strA, -1, (uint8_t *)sortKeyAz, sizeof(sortKeyAz));
        ucol_getSortKey(coll, strB,  3, (uint8_t *)sortKeyB, sizeof(sortKeyB));
        ucol_getSortKey(coll, strB, -1, (uint8_t *)sortKeyBz, sizeof(sortKeyBz));

        r = strcmp(sortKeyA, sortKeyAz);
        if (r <= 0) {
            log_err("Error 6 in test 4\n");
        }
        r = strcmp(sortKeyA, sortKeyB);
        if (r <= 0) {
            log_err("Error 7 in test 4\n");
        }
        r = strcmp(sortKeyAz, sortKeyBz);
        if (r != 0) {
            log_err("Error 8 in test 4\n");
        }
        ucol_setStrength(coll, UCOL_TERTIARY);
    }


    /*  Test 5:  Null characters in non-normal source strings.*/

    {
        UChar strA[] = {0x41, 0x41, 0x300, 0x316, 0x00, 0x42, 0x00};
        UChar strB[] = {0x41, 0x41, 0x300, 0x316, 0x00, 0x00, 0x00};
        char  sortKeyA[50];
        char  sortKeyAz[50];
        char  sortKeyB[50];
        char  sortKeyBz[50];
        int   r;

        result = ucol_strcoll(coll, strA, 6, strB, 6);
        if (result != UCOL_GREATER) {
            log_err("ERROR 1 in test 5\n");
        }
        result = ucol_strcoll(coll, strA, -1, strB, -1);
        if (result != UCOL_EQUAL) {
            log_err("ERROR 2 in test 5\n");
        }

        ucol_getSortKey(coll, strA,  6, (uint8_t *)sortKeyA, sizeof(sortKeyA));
        ucol_getSortKey(coll, strA, -1, (uint8_t *)sortKeyAz, sizeof(sortKeyAz));
        ucol_getSortKey(coll, strB,  6, (uint8_t *)sortKeyB, sizeof(sortKeyB));
        ucol_getSortKey(coll, strB, -1, (uint8_t *)sortKeyBz, sizeof(sortKeyBz));

        r = strcmp(sortKeyA, sortKeyAz);
        if (r <= 0) {
            log_err("Error 3 in test 5\n");
        }
        r = strcmp(sortKeyA, sortKeyB);
        if (r <= 0) {
            log_err("Error 4 in test 5\n");
        }
        r = strcmp(sortKeyAz, sortKeyBz);
        if (r != 0) {
            log_err("Error 5 in test 5\n");
        }

        ucol_setStrength(coll, UCOL_IDENTICAL);
        ucol_getSortKey(coll, strA,  6, (uint8_t *)sortKeyA, sizeof(sortKeyA));
        ucol_getSortKey(coll, strA, -1, (uint8_t *)sortKeyAz, sizeof(sortKeyAz));
        ucol_getSortKey(coll, strB,  6, (uint8_t *)sortKeyB, sizeof(sortKeyB));
        ucol_getSortKey(coll, strB, -1, (uint8_t *)sortKeyBz, sizeof(sortKeyBz));

        r = strcmp(sortKeyA, sortKeyAz);
        if (r <= 0) {
            log_err("Error 6 in test 5\n");
        }
        r = strcmp(sortKeyA, sortKeyB);
        if (r <= 0) {
            log_err("Error 7 in test 5\n");
        }
        r = strcmp(sortKeyAz, sortKeyBz);
        if (r != 0) {
            log_err("Error 8 in test 5\n");
        }
        ucol_setStrength(coll, UCOL_TERTIARY);
    }


    /*  Test 6:  Null character as base of a non-normal combining sequence.*/

    {
        UChar strA[] = {0x41, 0x0, 0x300, 0x316, 0x41, 0x302, 0x00};
        UChar strB[] = {0x41, 0x0, 0x302, 0x316, 0x41, 0x300, 0x00};

        result = ucol_strcoll(coll, strA, 5, strB, 5);
        if (result != UCOL_LESS) {
            log_err("Error 1 in test 6\n");
        }
        result = ucol_strcoll(coll, strA, -1, strB, -1);
        if (result != UCOL_EQUAL) {
            log_err("Error 2 in test 6\n");
        }
    }

    ucol_close(coll);
}



#if 0
static void TestGetCaseBit() {
  static const char *caseBitData[] = {
    "a", "A", "ch", "Ch", "CH",
      "\\uFF9E", "\\u0009"
  };

  static const uint8_t results[] = {
    UCOL_LOWER_CASE, UCOL_UPPER_CASE, UCOL_LOWER_CASE, UCOL_MIXED_CASE, UCOL_UPPER_CASE,
      UCOL_UPPER_CASE, UCOL_LOWER_CASE
  };

  uint32_t i, blen = 0;
  UChar b[256] = {0};
  UErrorCode status = U_ZERO_ERROR;
  UCollator *UCA = ucol_open("", &status);
  uint8_t res = 0;

  for(i = 0; i<sizeof(results)/sizeof(results[0]); i++) {
    blen = u_unescape(caseBitData[i], b, 256);
    res = ucol_uprv_getCaseBits(UCA, b, blen, &status);
    if(results[i] != res) {
      log_err("Expected case = %02X, got %02X for %04X\n", results[i], res, b[0]);
    }
  }
}
#endif

static void TestHangulTailoring() {
    static const char *koreanData[] = {
        "\\uac00", "\\u4f3d", "\\u4f73", "\\u5047", "\\u50f9", "\\u52a0", "\\u53ef", "\\u5475",
            "\\u54e5", "\\u5609", "\\u5ac1", "\\u5bb6", "\\u6687", "\\u67b6", "\\u67b7", "\\u67ef",
            "\\u6b4c", "\\u73c2", "\\u75c2", "\\u7a3c", "\\u82db", "\\u8304", "\\u8857", "\\u8888",
            "\\u8a36", "\\u8cc8", "\\u8dcf", "\\u8efb", "\\u8fe6", "\\u99d5",
            "\\u4EEE", "\\u50A2", "\\u5496", "\\u54FF", "\\u5777", "\\u5B8A", "\\u659D", "\\u698E",
            "\\u6A9F", "\\u73C8", "\\u7B33", "\\u801E", "\\u8238", "\\u846D", "\\u8B0C"
    };

    const char *rules =
        "&\\uac00 <<< \\u4f3d <<< \\u4f73 <<< \\u5047 <<< \\u50f9 <<< \\u52a0 <<< \\u53ef <<< \\u5475 "
        "<<< \\u54e5 <<< \\u5609 <<< \\u5ac1 <<< \\u5bb6 <<< \\u6687 <<< \\u67b6 <<< \\u67b7 <<< \\u67ef "
        "<<< \\u6b4c <<< \\u73c2 <<< \\u75c2 <<< \\u7a3c <<< \\u82db <<< \\u8304 <<< \\u8857 <<< \\u8888 "
        "<<< \\u8a36 <<< \\u8cc8 <<< \\u8dcf <<< \\u8efb <<< \\u8fe6 <<< \\u99d5 "
        "<<< \\u4EEE <<< \\u50A2 <<< \\u5496 <<< \\u54FF <<< \\u5777 <<< \\u5B8A <<< \\u659D <<< \\u698E "
        "<<< \\u6A9F <<< \\u73C8 <<< \\u7B33 <<< \\u801E <<< \\u8238 <<< \\u846D <<< \\u8B0C";


  UErrorCode status = U_ZERO_ERROR;
  UChar rlz[2048] = { 0 };
  uint32_t rlen = u_unescape(rules, rlz, 2048);

  UCollator *coll = ucol_openRules(rlz, rlen, UCOL_DEFAULT, UCOL_DEFAULT,NULL, &status);

  log_verbose("Using start of korean rules\n");

  if(U_SUCCESS(status)) {
    genericOrderingTest(coll, koreanData, sizeof(koreanData)/sizeof(koreanData[0]));
  } else {
    log_err("Unable to open collator with rules %s\n", rules);
  }

  log_verbose("Setting jamoSpecial to TRUE and testing once more\n");
  ((UCATableHeader *)coll->image)->jamoSpecial = TRUE; /* don't try this at home  */
  genericOrderingTest(coll, koreanData, sizeof(koreanData)/sizeof(koreanData[0]));

  ucol_close(coll);

  log_verbose("Using ko__LOTUS locale\n");
  genericLocaleStarter("ko__LOTUS", koreanData, sizeof(koreanData)/sizeof(koreanData[0]));
}

static void TestCompressOverlap() {
    UChar       secstr[150];
    UChar       tertstr[150];
    UErrorCode  status = U_ZERO_ERROR;
    UCollator  *coll;
    char        result[200];
    uint32_t    resultlen;
    int         count = 0;
    char       *tempptr;

    coll = ucol_open("", &status);

    if (U_FAILURE(status)) {
        log_err("Collator can't be created\n");
        return;
    }
    while (count < 149) {
        secstr[count] = 0x0020; /* [06, 05, 05] */
        tertstr[count] = 0x0020;
        count ++;
    }

    /* top down compression ----------------------------------- */
    secstr[count] = 0x0332; /* [, 87, 05] */
    tertstr[count] = 0x3000; /* [06, 05, 07] */

    /* no compression secstr should have 150 secondary bytes, tertstr should
    have 150 tertiary bytes.
    with correct overlapping compression, secstr should have 4 secondary
    bytes, tertstr should have > 2 tertiary bytes */
    resultlen = ucol_getSortKey(coll, secstr, 150, (uint8_t *)result, 250);
    tempptr = uprv_strchr(result, 1) + 1;
    while (*(tempptr + 1) != 1) {
        /* the last secondary collation element is not checked since it is not
        part of the compression */
        if (*tempptr < UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2) {
            log_err("Secondary compression overlapped\n");
        }
        tempptr ++;
    }

    /* tertiary top/bottom/common for en_US is similar to the secondary
    top/bottom/common */
    resultlen = ucol_getSortKey(coll, tertstr, 150, (uint8_t *)result, 250);
    tempptr = uprv_strrchr(result, 1) + 1;
    while (*(tempptr + 1) != 0) {
        /* the last secondary collation element is not checked since it is not
        part of the compression */
        if (*tempptr < coll->tertiaryTop - coll->tertiaryTopCount) {
            log_err("Tertiary compression overlapped\n");
        }
        tempptr ++;
    }

    /* bottom up compression ------------------------------------- */
    secstr[count] = 0;
    tertstr[count] = 0;
    resultlen = ucol_getSortKey(coll, secstr, 150, (uint8_t *)result, 250);
    tempptr = uprv_strchr(result, 1) + 1;
    while (*(tempptr + 1) != 1) {
        /* the last secondary collation element is not checked since it is not
        part of the compression */
        if (*tempptr > UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2) {
            log_err("Secondary compression overlapped\n");
        }
        tempptr ++;
    }

    /* tertiary top/bottom/common for en_US is similar to the secondary
    top/bottom/common */
    resultlen = ucol_getSortKey(coll, tertstr, 150, (uint8_t *)result, 250);
    tempptr = uprv_strrchr(result, 1) + 1;
    while (*(tempptr + 1) != 0) {
        /* the last secondary collation element is not checked since it is not
        part of the compression */
        if (*tempptr > coll->tertiaryBottom + coll->tertiaryBottomCount) {
            log_err("Tertiary compression overlapped\n");
        }
        tempptr ++;
    }
}

static void TestCyrillicTailoring(void) {
  static const char *test[] = {
    "\\u0410b",
      "\\u0410\\u0306a",
      "\\u04d0A"
  };
    genericLocaleStarter("ru", test, 3);
    genericRulesStarter("&\\u0410 = \\u0410", test, 3);
    genericRulesStarter("&Z < \\u0410", test, 3);
    genericRulesStarter("&\\u0410 = \\u0410 < \\u04d0", test, 3);
    genericRulesStarter("&Z < \\u0410 < \\u04d0", test, 3);
    genericRulesStarter("&\\u0410 = \\u0410 < \\u0410\\u0301", test, 3);
    genericRulesStarter("&Z < \\u0410 < \\u0410\\u0301", test, 3);
}

static void TestContraction() {
    const static char *testrules[] = {
        "&A = AB / B",
        "&A = A\\u0306/\\u0306",
        "&c = ch / h"
    };
    const static UChar testdata[][2] = {
        {0x0041 /* 'A' */, 0x0042 /* 'B' */},
        {0x0041 /* 'A' */, 0x0306 /* combining breve */},
        {0x0063 /* 'c' */, 0x0068 /* 'h' */}
    };
    const static UChar testdata2[][2] = {
        {0x0063 /* 'c' */, 0x0067 /* 'g' */},
        {0x0063 /* 'c' */, 0x0068 /* 'h' */},
        {0x0063 /* 'c' */, 0x006C /* 'l' */}
    };
    const static char *testrules3[] = {
        "&z < xyz &xyzw << B",
        "&z < xyz &xyz << B / w",
        "&z < ch &achm << B",
        "&z < ch &a << B / chm",
        "&\\ud800\\udc00w << B",
        "&\\ud800\\udc00 << B / w",
        "&a\\ud800\\udc00m << B",
        "&a << B / \\ud800\\udc00m",
    };

    UErrorCode  status   = U_ZERO_ERROR;
    UCollator  *coll;
    UChar       rule[256] = {0};
    uint32_t    rlen     = 0;
    int         i;

    for (i = 0; i < sizeof(testrules) / sizeof(testrules[0]); i ++) {
        UCollationElements *iter1;
        int j = 0;
        log_verbose("Rule %s for testing\n", testrules[i]);
        rlen = u_unescape(testrules[i], rule, 32);
        coll = ucol_openRules(rule, rlen, UCOL_ON, UCOL_TERTIARY,NULL, &status);
        if (U_FAILURE(status)) {
            log_err("Collator creation failed %s\n", testrules[i]);
            return;
        }
        iter1 = ucol_openElements(coll, testdata[i], 2, &status);
        if (U_FAILURE(status)) {
            log_err("Collation iterator creation failed\n");
            return;
        }
        while (j < 2) {
            UCollationElements *iter2 = ucol_openElements(coll,
                                                         &(testdata[i][j]),
                                                         1, &status);
            uint32_t ce;
            if (U_FAILURE(status)) {
                log_err("Collation iterator creation failed\n");
                return;
            }
            ce = ucol_next(iter2, &status);
            while (ce != UCOL_NULLORDER) {
                if ((uint32_t)ucol_next(iter1, &status) != ce) {
                    log_err("Collation elements in contraction split does not match\n");
                    return;
                }
                ce = ucol_next(iter2, &status);
            }
            j ++;
            ucol_closeElements(iter2);
        }
        if (ucol_next(iter1, &status) != UCOL_NULLORDER) {
            log_err("Collation elements not exhausted\n");
            return;
        }
        ucol_closeElements(iter1);
        ucol_close(coll);
    }

    rlen = u_unescape("& a < b < c < ch < d & c = ch / h", rule, 256);
    coll = ucol_openRules(rule, rlen, UCOL_ON, UCOL_TERTIARY,NULL, &status);
    if (ucol_strcoll(coll, testdata2[0], 2, testdata2[1], 2) != UCOL_LESS) {
        log_err("Expected \\u%04x\\u%04x < \\u%04x\\u%04x\n",
                testdata2[0][0], testdata2[0][1], testdata2[1][0],
                testdata2[1][1]);
        return;
    }
    if (ucol_strcoll(coll, testdata2[1], 2, testdata2[2], 2) != UCOL_LESS) {
        log_err("Expected \\u%04x\\u%04x < \\u%04x\\u%04x\n",
                testdata2[1][0], testdata2[1][1], testdata2[2][0],
                testdata2[2][1]);
        return;
    }
    ucol_close(coll);

    for (i = 0; i < sizeof(testrules3) / sizeof(testrules3[0]); i += 2) {
        UCollator          *coll1,
                           *coll2;
        UCollationElements *iter1,
                           *iter2;
        UChar               ch = 0x0042 /* 'B' */;
        uint32_t            ce;
        rlen = u_unescape(testrules3[i], rule, 32);
        coll1 = ucol_openRules(rule, rlen, UCOL_ON, UCOL_TERTIARY,NULL, &status);
        rlen = u_unescape(testrules3[i + 1], rule, 32);
        coll2 = ucol_openRules(rule, rlen, UCOL_ON, UCOL_TERTIARY,NULL, &status);
        if (U_FAILURE(status)) {
            log_err("Collator creation failed %s\n", testrules[i]);
            return;
        }
        iter1 = ucol_openElements(coll1, &ch, 1, &status);
        iter2 = ucol_openElements(coll2, &ch, 1, &status);
        if (U_FAILURE(status)) {
            log_err("Collation iterator creation failed\n");
            return;
        }
        ce = ucol_next(iter1, &status);
        if (U_FAILURE(status)) {
            log_err("Retrieving ces failed\n");
            return;
        }
        while (ce != UCOL_NULLORDER) {
            if (ce != (uint32_t)ucol_next(iter2, &status)) {
                log_err("CEs does not match\n");
                return;
            }
            ce = ucol_next(iter1, &status);
            if (U_FAILURE(status)) {
                log_err("Retrieving ces failed\n");
                return;
            }
        }
        if (ucol_next(iter2, &status) != UCOL_NULLORDER) {
            log_err("CEs not exhausted\n");
            return;
        }
        ucol_closeElements(iter1);
        ucol_closeElements(iter2);
        ucol_close(coll1);
        ucol_close(coll2);
    }
}

static void TestExpansion() {
    const static char *testrules[] = {
        "&J << K / B & K << M",
        "&J << K / B << M"
    };
    const static UChar testdata[][3] = {
        {0x004A /*'J'*/, 0x0041 /*'A'*/, 0},
        {0x004D /*'M'*/, 0x0041 /*'A'*/, 0},
        {0x004B /*'K'*/, 0x0041 /*'A'*/, 0},
        {0x004B /*'K'*/, 0x0043 /*'C'*/, 0},
        {0x004A /*'J'*/, 0x0043 /*'C'*/, 0},
        {0x004D /*'M'*/, 0x0043 /*'C'*/, 0}
    };

    UErrorCode  status   = U_ZERO_ERROR;
    UCollator  *coll;
    UChar       rule[256] = {0};
    uint32_t    rlen     = 0;
    int         i;

    for (i = 0; i < sizeof(testrules) / sizeof(testrules[0]); i ++) {
        int j = 0;
        log_verbose("Rule %s for testing\n", testrules[i]);
        rlen = u_unescape(testrules[i], rule, 32);
        coll = ucol_openRules(rule, rlen, UCOL_ON, UCOL_TERTIARY,NULL, &status);
        if (U_FAILURE(status)) {
            log_err("Collator creation failed %s\n", testrules[i]);
            return;
        }

        for (j = 0; j < 5; j ++) {
            doTest(coll, testdata[j], testdata[j + 1], UCOL_LESS);
        }
        ucol_close(coll);
    }
}

static void TestLimitations() {
  /* recursive expansions */
  {
    static const char *rule = "&a=b/c&d=c/e";
    static const char *tlimit01[] = {"add","b","adf"};
    static const char *tlimit02[] = {"aa","b","af"};
    log_verbose("recursive expansions\n");
    genericRulesStarter(rule, tlimit01, sizeof(tlimit01)/sizeof(tlimit01[0]));
    genericRulesStarter(rule, tlimit02, sizeof(tlimit02)/sizeof(tlimit02[0]));
  }
  /* contractions spanning expansions */
  {
    static const char *rule = "&a<<<c/e&g<<<eh";
    static const char *tlimit01[] = {"ad","c","af","f","ch","h"};
    static const char *tlimit02[] = {"ad","c","ch","af","f","h"};
    log_verbose("contractions spanning expansions\n");
    genericRulesStarter(rule, tlimit01, sizeof(tlimit01)/sizeof(tlimit01[0]));
    genericRulesStarter(rule, tlimit02, sizeof(tlimit02)/sizeof(tlimit02[0]));
  }
  /* normalization: nulls in contractions */
  {
    static const char *rule = "&a<<<\\u0000\\u0302";
    static const char *tlimit01[] = {"a","\\u0000\\u0302\\u0327"};
    static const char *tlimit02[] = {"\\u0000\\u0302\\u0327","a"};
    static const UColAttribute att[] = { UCOL_DECOMPOSITION_MODE };
    static const UColAttributeValue valOn[] = { UCOL_ON };
    static const UColAttributeValue valOff[] = { UCOL_OFF };

    log_verbose("NULL in contractions\n");
    genericRulesStarterWithOptions(rule, tlimit01, 2, att, valOn, 1);
    genericRulesStarterWithOptions(rule, tlimit02, 2, att, valOn, 1);
    genericRulesStarterWithOptions(rule, tlimit01, 2, att, valOff, 1);
    genericRulesStarterWithOptions(rule, tlimit02, 2, att, valOff, 1);

  }
  /* normalization: contractions spanning normalization */
  {
    static const char *rule = "&a<<<\\u0000\\u0302";
    static const char *tlimit01[] = {"a","\\u0000\\u0302\\u0327"};
    static const char *tlimit02[] = {"\\u0000\\u0302\\u0327","a"};
    static const UColAttribute att[] = { UCOL_DECOMPOSITION_MODE };
    static const UColAttributeValue valOn[] = { UCOL_ON };
    static const UColAttributeValue valOff[] = { UCOL_OFF };

    log_verbose("contractions spanning normalization\n");
    genericRulesStarterWithOptions(rule, tlimit01, 2, att, valOn, 1);
    genericRulesStarterWithOptions(rule, tlimit02, 2, att, valOn, 1);
    genericRulesStarterWithOptions(rule, tlimit01, 2, att, valOff, 1);
    genericRulesStarterWithOptions(rule, tlimit02, 2, att, valOff, 1);

  }
  /* variable top:  */
  {
    static const char *rule2 = "&\\u2010<x=[variable top]<z";
    static const char *rule = "&\\u2010<x<[variable top]=z";
    static const char *rule3 = "&' '<x<[variable top]=z";
    static const char *tlimit01[] = {" ", "z", "zb", "a", " b", "xb", "b", "c" };
    static const char *tlimit02[] = {"-", "-x", "x","xb", "-z", "z", "zb", "-a", "a", "-b", "b", "c"};
    static const char *tlimit03[] = {" ", "xb", "z", "zb", "a", " b", "b", "c" };
    static const UColAttribute att[] = { UCOL_ALTERNATE_HANDLING, UCOL_STRENGTH };
    static const UColAttributeValue valOn[] = { UCOL_SHIFTED, UCOL_QUATERNARY };
    static const UColAttributeValue valOff[] = { UCOL_NON_IGNORABLE, UCOL_TERTIARY };

    log_verbose("variable top\n");
    genericRulesStarterWithOptions(rule, tlimit03, sizeof(tlimit03)/sizeof(tlimit03[0]), att, valOn, sizeof(att)/sizeof(att[0]));
    genericRulesStarterWithOptions(rule, tlimit01, sizeof(tlimit01)/sizeof(tlimit01[0]), att, valOn, sizeof(att)/sizeof(att[0]));
    genericRulesStarterWithOptions(rule, tlimit02, sizeof(tlimit02)/sizeof(tlimit02[0]), att, valOn, sizeof(att)/sizeof(att[0]));
    genericRulesStarterWithOptions(rule, tlimit01, sizeof(tlimit01)/sizeof(tlimit01[0]), att, valOff, sizeof(att)/sizeof(att[0]));
    genericRulesStarterWithOptions(rule, tlimit02, sizeof(tlimit02)/sizeof(tlimit02[0]), att, valOff, sizeof(att)/sizeof(att[0]));

  }
  /* case level */
  {
    static const char *rule = "&c<ch<<<cH<<<Ch<<<CH";
    static const char *tlimit01[] = {"c","CH","Ch","cH","ch"};
    static const char *tlimit02[] = {"c","CH","cH","Ch","ch"};
    static const UColAttribute att[] = { UCOL_CASE_FIRST};
    static const UColAttributeValue valOn[] = { UCOL_UPPER_FIRST};
    static const UColAttributeValue valOff[] = { UCOL_OFF};
    log_verbose("case level\n");
    genericRulesStarterWithOptions(rule, tlimit01, sizeof(tlimit01)/sizeof(tlimit01[0]), att, valOn, sizeof(att)/sizeof(att[0]));
    genericRulesStarterWithOptions(rule, tlimit02, sizeof(tlimit02)/sizeof(tlimit02[0]), att, valOn, sizeof(att)/sizeof(att[0]));
    /*genericRulesStarterWithOptions(rule, tlimit01, sizeof(tlimit01)/sizeof(tlimit01[0]), att, valOff, sizeof(att)/sizeof(att[0]));*/
    /*genericRulesStarterWithOptions(rule, tlimit02, sizeof(tlimit02)/sizeof(tlimit02[0]), att, valOff, sizeof(att)/sizeof(att[0]));*/
  }

}

static void TestBocsuCoverage() {
  UErrorCode status = U_ZERO_ERROR;
  const char *testString = "\\u0041\\u0441\\u4441\\U00044441\\u4441\\u0441\\u0041";
  UChar       test[256] = {0};
  uint32_t    tlen     = u_unescape(testString, test, 32);
  uint8_t key[256]     = {0};
  uint32_t klen         = 0;

  UCollator *coll = ucol_open("", &status);
  ucol_setAttribute(coll, UCOL_STRENGTH, UCOL_IDENTICAL, &status);

  klen = ucol_getSortKey(coll, test, tlen, key, 256);

  ucol_close(coll);
}

static void TestVariableTopSetting() {
  UErrorCode status = U_ZERO_ERROR;
  const UChar *current = NULL;
  uint32_t varTopOriginal = 0, varTop1, varTop2;
  UCollator *coll = ucol_open("", &status);

  uint32_t strength = 0;
  uint8_t specs = 0;
  uint32_t chOffset = 0;
  uint32_t chLen = 0;
  uint32_t exOffset = 0;
  uint32_t exLen = 0;
  uint32_t oldChOffset = 0;
  uint32_t oldChLen = 0;
  uint32_t oldExOffset = 0;
  uint32_t oldExLen = 0;
  uint32_t prefixOffset = 0;
  uint32_t prefixLen = 0;

  UBool startOfRules = TRUE;
  UColTokenParser src;
  UColOptionSet opts;

  UChar *rulesCopy = NULL;
  uint32_t rulesLen;

  UCollationResult result;

  UChar first[256] = { 0 };
  UChar second[256] = { 0 };
  UParseError parseError;
  src.opts = &opts;

  log_verbose("Slide variable top over UCARules\n");
  rulesLen = ucol_getRulesEx(coll, UCOL_FULL_RULES, rulesCopy, 0);
  rulesCopy = (UChar *)uprv_malloc((rulesLen+UCOL_TOK_EXTRA_RULE_SPACE_SIZE)*sizeof(UChar));
  rulesLen = ucol_getRulesEx(coll, UCOL_FULL_RULES, rulesCopy, rulesLen+UCOL_TOK_EXTRA_RULE_SPACE_SIZE);

  if(U_SUCCESS(status) && rulesLen > 0) {
    ucol_setAttribute(coll, UCOL_ALTERNATE_HANDLING, UCOL_SHIFTED, &status);
    src.source = src.current = rulesCopy;
    src.end = rulesCopy+rulesLen;
    src.extraCurrent = src.end;
    src.extraEnd = src.end+UCOL_TOK_EXTRA_RULE_SPACE_SIZE;

    while ((current = ucol_tok_parseNextToken(&src, &strength,
                      &chOffset, &chLen, &exOffset, &exLen,
                      &prefixOffset, &prefixLen,
                      &specs, startOfRules, &parseError,&status)) != NULL) {
      startOfRules = FALSE;
      if(0) {
        log_verbose("%04X %d ", *(rulesCopy+chOffset), chLen);
      }
      if(strength == UCOL_PRIMARY) {
        status = U_ZERO_ERROR;
        varTopOriginal = ucol_getVariableTop(coll, &status);
        varTop1 = ucol_setVariableTop(coll, rulesCopy+oldChOffset, oldChLen, &status);
        if(U_FAILURE(status)) {
          if(status == U_PRIMARY_TOO_LONG_ERROR) {
            log_verbose("= Expected failure for %04X =", *(rulesCopy+oldChOffset));
          } else {
            log_err("Unexpected failure setting variable top for %04X at offset %d. Error %s\n", *(rulesCopy+oldChOffset), oldChOffset, u_errorName(status));
          }
          continue;
        }
        varTop2 = ucol_getVariableTop(coll, &status);
        if((varTop1 & 0xFFFF0000) != (varTop2 & 0xFFFF0000)) {
          log_err("cannot retrieve set varTop value!\n");
          continue;
        }

        if((varTop1 & 0xFFFF0000) > 0 && oldExLen == 0) {

          u_strncpy(first, rulesCopy+oldChOffset, oldChLen);
          u_strncpy(first+oldChLen, rulesCopy+chOffset, chLen);
          u_strncpy(first+oldChLen+chLen, rulesCopy+oldChOffset, oldChLen);
          first[2*oldChLen+chLen] = 0;

          if(oldExLen == 0) {
            u_strncpy(second, rulesCopy+chOffset, chLen);
            second[chLen] = 0;
          } else { /* This is skipped momentarily, but should work once UCARules are fully UCA conformant */
            u_strncpy(second, rulesCopy+oldExOffset, oldExLen);
            u_strncpy(second+oldChLen, rulesCopy+chOffset, chLen);
            u_strncpy(second+oldChLen+chLen, rulesCopy+oldExOffset, oldExLen);
            second[2*oldExLen+chLen] = 0;
          }
          result = ucol_strcoll(coll, first, -1, second, -1);
          if(result == UCOL_EQUAL) {
            doTest(coll, first, second, UCOL_EQUAL);
          } else {
            log_verbose("Suspicious strcoll result for %04X and %04X\n", *(rulesCopy+oldChOffset), *(rulesCopy+chOffset));
          }
        }
      }
      if(strength != UCOL_TOK_RESET) {
        oldChOffset = chOffset;
        oldChLen = chLen;
        oldExOffset = exOffset;
        oldExLen = exLen;
      }
    }
  }
  else {
    log_err("Unexpected failure getting rules %s\n", u_errorName(status));
    return;
  }
  status = U_ZERO_ERROR;

  log_verbose("Testing setting variable top to contractions\n");
  {
    /* uint32_t tailoredCE = UCOL_NOT_FOUND; */
    UChar *conts = (UChar *)((uint8_t *)coll->image + coll->image->contractionUCACombos);
    while(*conts != 0) {
      if(*(conts+2) == 0) {
        varTop1 = ucol_setVariableTop(coll, conts, -1, &status);
      } else {
        varTop1 = ucol_setVariableTop(coll, conts, 3, &status);
      }
      if(U_FAILURE(status)) {
        log_err("Couldn't set variable top to a contraction\n");
      }
      conts+=3;
    }

    status = U_ZERO_ERROR;

    first[0] = 0x0040;
    first[1] = 0x0050;
    first[2] = 0x0000;

    ucol_setVariableTop(coll, first, -1, &status);

    if(U_SUCCESS(status)) {
      log_err("Invalid contraction succeded in setting variable top!\n");
    }

  }

  log_verbose("Test restoring variable top\n");

  status = U_ZERO_ERROR;
  ucol_restoreVariableTop(coll, varTopOriginal, &status);
  if(varTopOriginal != ucol_getVariableTop(coll, &status)) {
    log_err("Couldn't restore old variable top\n");
  }

  log_verbose("Testing calling with error set\n");

  status = U_INTERNAL_PROGRAM_ERROR;
  varTop1 = ucol_setVariableTop(coll, first, 1, &status);
  varTop2 = ucol_getVariableTop(coll, &status);
  ucol_restoreVariableTop(coll, varTop2, &status);
  varTop1 = ucol_setVariableTop(NULL, first, 1, &status);
  varTop2 = ucol_getVariableTop(NULL, &status);
  ucol_restoreVariableTop(NULL, varTop2, &status);
  if(status != U_INTERNAL_PROGRAM_ERROR) {
    log_err("Bad reaction to passed error!\n");
  }
  ucol_close(coll);
}

static void TestNonChars() {
  static const char *test[] = {
    "\\u0000",
    "\\uFFFE", "\\uFFFF",
      "\\U0001FFFE", "\\U0001FFFF",
      "\\U0002FFFE", "\\U0002FFFF",
      "\\U0003FFFE", "\\U0003FFFF",
      "\\U0004FFFE", "\\U0004FFFF",
      "\\U0005FFFE", "\\U0005FFFF",
      "\\U0006FFFE", "\\U0006FFFF",
      "\\U0007FFFE", "\\U0007FFFF",
      "\\U0008FFFE", "\\U0008FFFF",
      "\\U0009FFFE", "\\U0009FFFF",
      "\\U000AFFFE", "\\U000AFFFF",
      "\\U000BFFFE", "\\U000BFFFF",
      "\\U000CFFFE", "\\U000CFFFF",
      "\\U000DFFFE", "\\U000DFFFF",
      "\\U000EFFFE", "\\U000EFFFF",
      "\\U000FFFFE", "\\U000FFFFF",
      "\\U0010FFFE", "\\U0010FFFF"
  };
  UErrorCode status = U_ZERO_ERROR;
  UCollator *coll = ucol_open("en_US", &status);

  log_verbose("Test non characters\n");

  if(U_SUCCESS(status)) {
    genericOrderingTestWithResult(coll, test, 35, UCOL_EQUAL);
  } else {
    log_err("Unable to open collator\n");
  }

  ucol_close(coll);
}

static void TestExtremeCompression() {
  static const char *test[] = {
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab",
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaac",
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaad"
  };
  genericLocaleStarter("en_US", test, 4);
}

static void TestSurrogates() {
  UErrorCode status = U_ZERO_ERROR;

  static const char *test[] = {
    "z","\\ud900\\udc25",  "\\ud805\\udc50",
       "\\ud800\\udc00y",  "\\ud800\\udc00r",
       "\\ud800\\udc00f",  "\\ud800\\udc00",
       "\\ud800\\udc00c", "\\ud800\\udc00b",
       "\\ud800\\udc00fa", "\\ud800\\udc00fb",
       "\\ud800\\udc00a",
       "c", "b"
  };

  static const char *rule =
    "&z < \\ud900\\udc25   < \\ud805\\udc50"
       "< \\ud800\\udc00y  < \\ud800\\udc00r"
       "< \\ud800\\udc00f  << \\ud800\\udc00"
       "< \\ud800\\udc00fa << \\ud800\\udc00fb"
       "< \\ud800\\udc00a  < c < b" ;

  genericRulesStarter(rule, test, 14);
}

static void TestNewJapanese() {
  UErrorCode status = U_ZERO_ERROR;
  /*UParseError parseError;*/
  UCollator *coll = NULL;  
  UChar string[256] = {0};
  uint32_t uStringLen = 0;


        /*UCollator *coll = ucol_open("ja_JP_JIS", &status);  */
  /*"&\u304b\u3099<<<\u304c|\u309d=\u304b|\u309d\u3099=\u304c|\u309d\u3099"*/

  static const char *rules = 		
        "&z <<< z|a";
    /* this should yield in zz<<< za */
    static const char *testaz[] = {       "\\u30d7\\u30fd",
      "\\u3077\\u3075",
/*"zz", "za"*/ };

  static const char *test[] = {
/*
      "\\u30b7\\u30e3\\u30fc\\u30ec",
      "\\u30b7\\u30e3\\u30a4",
      "\\u30b7\\u30e4\\u30a3",
      "\\u30b7\\u30e3\\u30ec",
      "\\u3061\\u3087\\u3053",
      "\\u3061\\u3088\\u3053",
      "\\u30c1\\u30e7\\u30b3\\u30ec\\u30fc\\u30c8",
      "\\u3066\\u30fc\\u305f",
*/
      "\\u30c6\\u30fc\\u30bf", // *
      "\\u30c6\\u30a7\\u30bf",
      "\\u3066\\u3048\\u305f",
      "\\u3067\\u30fc\\u305f", // *
/*
      "\\u30c7\\u30fc\\u30bf",
      "\\u30c7\\u30a7\\u30bf",
      "\\u3067\\u3048\\u305f",
      "\\u3066\\u30fc\\u305f\\u30fc",
      "\\u30c6\\u30fc\\u30bf\\u30a1",
      "\\u30c6\\u30a7\\u30bf\\u30fc",
      "\\u3066\\u3047\\u305f\\u3041",
      "\\u3066\\u3048\\u305f\\u30fc",
      "\\u3067\\u30fc\\u305f\\u30fc",
      "\\u30c7\\u30fc\\u30bf\\u30a1",
      "\\u3067\\u30a7\\u305f\\u30a1",
      "\\u30c7\\u3047\\u30bf\\u3041",
      "\\u30c7\\u30a8\\u30bf\\u30a2",
      "\\u3072\\u3086",
      "\\u3073\\u3085\\u3042",
      "\\u3074\\u3085\\u3042",
      "\\u3073\\u3085\\u3042\\u30fc",
      "\\u30d3\\u30e5\\u30a2\\u30fc",
      "\\u3074\\u3085\\u3042\\u30fc",
      "\\u30d4\\u30e5\\u30a2\\u30fc",
      "\\u30d2\\u30e5\\u30a6",
      "\\u30d2\\u30e6\\u30a6",
      "\\u30d4\\u30e5\\u30a6\\u30a2",
      "\\u3074\\u3085\\u30fc\\u3042\\u30fc",
      "\\u30d3\\u30e5\\u30fc\\u30a2\\u30fc",
      "\\u30d3\\u30e5\\u30a6\\u30a2\\u30fc",
      "\\u3072\\u3085\\u3093",
      "\\u3074\\u3085\\u3093",
      "\\u3075\\u30fc\\u308a",
      "\\u30d5\\u30fc\\u30ea",
      "\\u3075\\u3045\\u308a",
      "\\u3075\\u30a5\\u308a",
      "\\u3075\\u30a5\\u30ea",
      "\\u30d5\\u30a6\\u30ea",
      "\\u3076\\u30fc\\u308a",
      "\\u30d6\\u30fc\\u30ea",
      "\\u3076\\u3045\\u308a",
      "\\u30d6\\u30a5\\u308a",
      "\\u3077\\u3046\\u308a",
      "\\u30d7\\u30a6\\u30ea",
      "\\u3075\\u30fc\\u308a\\u30fc",
      "\\u30d5\\u30a5\\u30ea\\u30fc",
      "\\u3075\\u30a5\\u308a\\u30a3",
      "\\u30d5\\u3045\\u308a\\u3043",
      "\\u30d5\\u30a6\\u30ea\\u30fc",
      "\\u3075\\u3046\\u308a\\u3043",
      "\\u30d6\\u30a6\\u30ea\\u30a4",
      "\\u3077\\u30fc\\u308a\\u30fc",
      "\\u3077\\u30a5\\u308a\\u30a4",
      "\\u3077\\u3046\\u308a\\u30fc",
      "\\u30d7\\u30a6\\u30ea\\u30a4",
      "\\u30d5\\u30fd",
      "\\u3075\\u309e",
      "\\u3076\\u309d",
      "\\u3076\\u3075",
      "\\u3076\\u30d5",
      "\\u30d6\\u3075",
      "\\u30d6\\u30d5",
      "\\u3076\\u309e",
      "\\u3076\\u3077",
      "\\u30d6\\u3077", //*
      "\\u3077\\u309d",
      "\\u30d7\\u30fd",
      "\\u3077\\u3075", //*
*/
/*
"\\u3042\\u30fc",
"\\u3042\\u3041",
"\\u3042\\u309d",
"\\u3042\\u3042",
*/
  };

  uint32_t i = 0;
  UCollationElements *it = NULL;
  uint32_t CE;
  /*genericRulesStarter(rules, testaz, 2);*/

  genericLocaleStarter("ja_JP_JIS", test, sizeof(test)/sizeof(test[0]));


  /*uStringLen = u_unescape(rules, string, 256);*/
  coll = ucol_open("ja_JP_JIS", &status);
  it = ucol_openElements(coll, string, 0, &status);

  for(i = 0; i < sizeof(test)/sizeof(test[0]); i++) {
    log_verbose("%s\n", test[i]);
    uStringLen = u_unescape(test[i], string, 256);
    ucol_setText(it, string, uStringLen, &status);

    while((CE=ucol_next(it, &status)) != UCOL_NULLORDER) {
      log_verbose("%08X\n", CE);
    }
    log_verbose("\n");

  }

  ucol_closeElements(it);
  ucol_close(coll);
}

void addMiscCollTest(TestNode** root)
{
    addTest(root, &TestNewJapanese, "tscoll/cmsccoll/TestNewJapanese"); 
    /*addTest(root, &TestLimitations, "tscoll/cmsccoll/TestLimitations");*/
    addTest(root, &TestNonChars, "tscoll/cmsccoll/TestNonChars");
    addTest(root, &TestExtremeCompression, "tscoll/cmsccoll/TestExtremeCompression");
    addTest(root, &TestSurrogates, "tscoll/cmsccoll/TestSurrogates");
    addTest(root, &TestVariableTopSetting, "tscoll/cmsccoll/TestVariableTopSetting");
    addTest(root, &TestBocsuCoverage, "tscoll/cmsccoll/TestBocsuCoverage");
    addTest(root, &TestCyrillicTailoring, "tscoll/cmsccoll/TestCyrillicTailoring");
    addTest(root, &TestCase, "tscoll/cmsccoll/TestCase");
    addTest(root, &IncompleteCntTest, "tscoll/cmsccoll/IncompleteCntTest");
    addTest(root, &BlackBirdTest, "tscoll/cmsccoll/BlackBirdTest");
    addTest(root, &FunkyATest, "tscoll/cmsccoll/FunkyATest");
    addTest(root, &BillFairmanTest, "tscoll/cmsccoll/BillFairmanTest");
    addTest(root, &RamsRulesTest, "tscoll/cmsccoll/RamsRulesTest");
    addTest(root, &IsTailoredTest, "tscoll/cmsccoll/IsTailoredTest");
    addTest(root, &TestCollations, "tscoll/cmsccoll/TestCollations");
    addTest(root, &TestChMove, "tscoll/cmsccoll/TestChMove");
    addTest(root, &TestImplicitTailoring, "tscoll/cmsccoll/TestImplicitTailoring");
    addTest(root, &TestFCDProblem, "tscoll/cmsccoll/TestFCDProblem");
    addTest(root, &TestEmptyRule, "tscoll/cmsccoll/TestEmptyRule");
    addTest(root, &TestJ784, "tscoll/cmsccoll/TestJ784");
    addTest(root, &TestJ815, "tscoll/cmsccoll/TestJ815");
    addTest(root, &TestJ831, "tscoll/cmsccoll/TestJ831");
    addTest(root, &TestBefore, "tscoll/cmsccoll/TestBefore");
    addTest(root, &TestRedundantRules, "tscoll/cmsccoll/TestRedundantRules");
    addTest(root, &TestExpansionSyntax, "tscoll/cmsccoll/TestExpansionSyntax");
    addTest(root, &TestHangulTailoring, "tscoll/cmsccoll/TestHangulTailoring");
    addTest(root, &TestUCARules, "tscoll/cmsccoll/TestUCARules");
    addTest(root, &TestIncrementalNormalize, "tscoll/cmsccoll/TestIncrementalNormalize");
    addTest(root, &TestComposeDecompose, "tscoll/cmsccoll/TestComposeDecompose");
    addTest(root, &TestCompressOverlap, "tscoll/cmsccoll/TestCompressOverlap");
    addTest(root, &TestContraction, "tscoll/cmsccoll/TestContraction");
    addTest(root, &TestExpansion, "tscoll/cmsccoll/TestExpansion");
    /*addTest(root, &PrintMarkDavis, "tscoll/cmsccoll/PrintMarkDavis");*/ /* this test doesn't test - just prints sortkeys */
    /*addTest(root, &TestGetCaseBit, "tscoll/cmsccoll/TestGetCaseBit");*/ /*this one requires internal things to be exported */
}
