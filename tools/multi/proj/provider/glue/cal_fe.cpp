// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2009-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/
#define CAL_FE_DEBUG 1

#ifndef CAL_FE_DEBUG
#define CAL_FE_DEBUG 0
#endif

#if CAL_FE_DEBUG
#define debugfprintf(x) fflush(stderr),fflush(stdout),fprintf x,fflush(stderr),fflush(stdout)
#else
#define debugfprintf(x)
#endif

#include <icuglue/icuglue.h>
#include "unicode/ucal.h"
//#include <unicode/tblcoll.h>
#include "unicode/calendar.h"
#include <string.h>
#include <stdio.h>
#include "unicode/ustring.h"
#include "unicode/gregocal.h"



/**
 * Macro to define the Calendar_glue_4_2 class 
 */
#ifdef GLUE_VER
#error GLUE_VER is defined
#endif

#define GLUE_VER(x) class GLUE_SYM_V( Calendar, x ) : public Calendar {  \
public: /* static create */ \
  UCalendar *_this; GLUE_SYM_V( Calendar, x ) ( const Locale&, UErrorCode& ); \
private: \
    virtual ~ GLUE_SYM_V ( Calendar, x) ();                             \
  public:                                                               \
    virtual void* getDynamicClassID() const;                            \
    static void* getStaticClassID() ;                                       \
    /* overrides */                                                         \
    virtual UBool haveDefaultCentury() const;                               \
    virtual UDate defaultCenturyStart() const ;                             \
    virtual int32_t handleGetExtendedYear() ; \
virtual const char * getType() const ; \
virtual UBool inDaylightTime(UErrorCode& status) const ; \
    virtual int32_t defaultCenturyStartYear() const ;  \
    virtual int32_t handleComputeMonthStart(int32_t eyear, int32_t month, UBool useMonth) const ; \
    virtual int32_t handleGetLimit(UCalendarDateFields field, ELimitType limitType) const ; \
    virtual Calendar* clone(void) const; \
  public: static int32_t countAvailable();                              \
public: static int32_t appendAvailable(UnicodeString* strs, int32_t i, int32_t count); \
  };


/** ==================================== The following code runs inside the 'target' version (i.e. old ICU) ========== **/
#if defined ( ICUGLUE_VER )

/* code for some version */
#include <icuglue/gluren.h>
#include "oicu.h"

#ifdef GLUE_VER
GLUE_VER( ICUGLUE_VER )
#endif

GLUE_SYM (Calendar ) :: GLUE_SYM(Calendar) ( const Locale& loc, UErrorCode& status ) :
Calendar(status), _this(nullptr)
{ 

  _this = OICU_ucal_open(nullptr, -1, /*locale*/nullptr, UCAL_DEFAULT, &status);

  // copy some things over
  setMinimalDaysInFirstWeek(OICU_ucal_getAttribute(_this, UCAL_MINIMAL_DAYS_IN_FIRST_WEEK));
  setFirstDayOfWeek((UCalendarDaysOfWeek)OICU_ucal_getAttribute(_this, UCAL_FIRST_DAY_OF_WEEK));
}

GLUE_SYM ( Calendar ) :: ~ GLUE_SYM(Calendar) () {
#if CAL_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " ucal_close");
#endif
    OICU_ucal_close(_this);
}


UBool GLUE_SYM ( Calendar ) :: haveDefaultCentury() const {
  return false;
}
UDate GLUE_SYM ( Calendar ) :: defaultCenturyStart() const {
  return 0L;
}
int32_t GLUE_SYM ( Calendar ) :: handleGetExtendedYear() {
  return 0;
}
const char * GLUE_SYM ( Calendar ) :: getType() const  {
  return "dilbert";
}
UBool GLUE_SYM ( Calendar ) :: inDaylightTime(UErrorCode& status) const  {
  return false;
}
int32_t GLUE_SYM ( Calendar ) :: defaultCenturyStartYear() const  {
  return 2012;
}
int32_t GLUE_SYM ( Calendar ) :: handleComputeMonthStart(int32_t eyear, int32_t month, UBool useMonth) const {
  return 0;
}

int32_t GLUE_SYM ( Calendar ) :: handleGetLimit(UCalendarDateFields field, ELimitType limitType) const {
  return 1;
}
Calendar* GLUE_SYM ( Calendar ) :: clone(void) const {
  return nullptr;
}


// DateFormat *
// GLUE_SYM ( DateFormat ) :: create(UDateFormatStyle  timeStyle,
//                                                     UDateFormatStyle  dateStyle,
//                                                     const char        *locale,
//                                                     const char16_t    *tzID,
//                                                     int32_t           tzIDLength,
//                                                     const char16_t    *pattern,
//                                                     int32_t           patternLength,
//                                                     UErrorCode        *status,
//                                   const Locale &loc, const char */*ver*/) {
//   // TODO: save version
//   //char locBuf[200];
//   //char kwvBuf[200];
//   UDateFormat * uc =  OICU_udat_open( timeStyle, dateStyle, locale,
//                                       tzID,
//                                       tzIDLength,
//                                       pattern,
//                                       patternLength,
//                                       status);
//     if(U_FAILURE(*status)) return nullptr; // TODO: ERR?
//     DateFormat *c =  new GLUE_SYM( DateFormat ) ( uc );
// #if CAL_FE_DEBUG
//     fprintf(stderr, "VCF " ICUGLUE_VER_STR " udat_open=%s ->> %p\n", loc.getName(), (void*)c);
// #endif
//     return c;
// }


 int32_t GLUE_SYM ( Calendar ) :: countAvailable() {
    int32_t count =  OICU_udat_countAvailable();
    return count;
 }
 
 
int32_t GLUE_SYM ( Calendar ) :: appendAvailable(UnicodeString* strs, int32_t i, int32_t /*count*/) {
   int avail = OICU_udat_countAvailable();
   UErrorCode status = U_ZERO_ERROR;
   OICU_u_init(&status);
#if CAL_FE_DEBUG
   fprintf(stderr,  "VCF " ICUGLUE_VER_STR " avail %d - init %s\n", avail, u_errorName(status));
#endif   
    for(int j=0;j<avail;j++) {
         strs[i+j].append(OICU_udat_getAvailable(j));
         strs[i+j].append("@sp=icu");
         if(IS_OLD_VERSTR(ICUGLUE_VER_STR)) {
           strs[i+j].append( ICUGLUE_VER_STR[OLD_VERSTR_MAJ] );  // X_y
           strs[i+j].append( ICUGLUE_VER_STR[OLD_VERSTR_MIN] );  // x_Y
         } else {
           strs[i+j].append( ICUGLUE_VER_STR[NEW_VERSTR_MAJ] );  // Xy_
           strs[i+j].append( ICUGLUE_VER_STR[NEW_VERSTR_MIN] );  // xY_
         }
#if CAL_FE_DEBUG
         { 
            char foo[999];
            const char16_t *ss = strs[i+j].getTerminatedBuffer();
            u_austrcpy(foo, ss);
            fprintf(stderr,  "VCF " ICUGLUE_VER_STR " appending [%d+%d=%d] <<%s>>\n", i, j, i+j, foo);
        }
#endif
    }
    return OICU_ucol_countAvailable();
 }

UOBJECT_DEFINE_RTTI_IMPLEMENTATION( GLUE_SYM( Calendar ) )




#else
/** ==================================== The following code runs inside the 'provider' version (i.e. current ICU) ========== **/

// #if (U_ICU_VERSION_MAJOR_NUM < 49)
// #define CAL_PROVIDER_UNSUPPORTED
// #endif

#ifndef CAL_PROVIDER_UNSUPPORTED
// define Collator_XX
#include "icuglue/glver.h"

#include "servloc.h"

// generate list of versions
static
#include <icuglue/fe_verlist.h>

class VersionCalendarFactory : public LocaleKeyFactory  {
public:
  VersionCalendarFactory();
  virtual UObject* handleCreate(const Locale &loc, int32_t kind, const ICUService* service, UErrorCode& status) const;
  // virtual Calendar *createFormat(UCalendarStyle  timeStyle,
  //                                  UCalendarStyle  dateStyle,
  //                                  const char        *locale,
  //                                  const char16_t    *tzID,
  //                                  int32_t           tzIDLength,
  //                                  const char16_t    *pattern,
  //                                  int32_t           patternLength,
  //                                  UErrorCode        *status);
  virtual void* getDynamicClassID() const; 
  static void* getStaticClassID() ; 
  virtual const Hashtable* getSupportedIDs(UErrorCode& status) const;
private:
  const UnicodeString *getSupportedIDs(int32_t &count, UErrorCode &status) const;

public:
virtual UObject*
 create(const ICUServiceKey& key, const ICUService* service, UErrorCode& status) const ;

};

UOBJECT_DEFINE_RTTI_IMPLEMENTATION( VersionCalendarFactory )

UObject*
VersionCalendarFactory::create(const ICUServiceKey& key, const ICUService* service, UErrorCode& status) const  {
  //  UnicodeString id;
  //  key.currentID(id);
  //  Locale l(id);
  Locale l;
  const LocaleKey& lkey = (LocaleKey&)key;
  lkey.currentLocale(l);
  debugfprintf((stderr, "VCalF::create() .. %s err=%s\n", (const char*)l.getName(), u_errorName(status)));

  char kw[100];
  int32_t kwlen = l.getKeywordValue("sp", kw, 100, status);

  UObject *f;
  if(kwlen>0) {
    debugfprintf((stderr, "Trying for kw=%s\n", kw));
    f = handleCreate(l, -1, service, status);
  } else {
    f = LocaleKeyFactory::create(key,service,status);
  }


  
  debugfprintf((stderr, "VCalF::create() .. = %p err=%s\n", (void*)f, u_errorName(status)));
  return f;
}

VersionCalendarFactory::VersionCalendarFactory() :LocaleKeyFactory(LocaleKeyFactory::VISIBLE){
#if CAL_FE_DEBUG
  printf("VCalF: hi! pid=%d, this=%p\n", getpid(), (void*)this);
#endif
}
UObject* VersionCalendarFactory::handleCreate(const Locale &loc, int32_t kind, const ICUService* service, UErrorCode& status) const {
  //Locale loc(locale);
    // pull off provider #
    char provider[200];
#if CAL_FE_DEBUG
    fprintf(stderr,  "VCalF:CC %s\n", loc.getName());
#endif
    int32_t len = loc.getKeywordValue("sp", provider, 200, status);
    if(U_FAILURE(status)||len==0) return nullptr;
#if CAL_FE_DEBUG
    fprintf(stderr,  "VCalF:KWV> %s/%d\n", u_errorName(status), len);
#endif
    provider[len]=0;
#if CAL_FE_DEBUG
    fprintf(stderr,  "VCalF:KWV %s\n", provider);
#endif
    if(strncmp(provider,"icu",3)) return nullptr;
    const char *icuver=provider+3;
#if CAL_FE_DEBUG
    fprintf(stderr,  "VCalF:ICUV %s\n", icuver);
#endif
    
#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) debugfprintf((stderr,"%c/%c|%c/%c\n", icuver[0],(#x)[0],icuver[1],(#x)[2]));  if(CMP_VERSTR(icuver, (#x))) { Calendar *c = new glue ## Calendar ## x (loc, status); debugfprintf((stderr, "VCalF::CC %s -> %p\n", loc.getName(), c)); return c; }
#include "icuglue/glver.h"
#if CAL_FE_DEBUG
    fprintf(stderr,  "VCalF:CC %s failed\n", loc.getName());
#endif

    return nullptr;
}


static const UnicodeString *gLocalesDate = nullptr;
static  int32_t gLocCountDate = 0; 

const Hashtable *VersionCalendarFactory::getSupportedIDs (UErrorCode& status) const {
  // from coll.cpp
  Hashtable *_ids = nullptr;
  if (U_SUCCESS(status)) {
    int32_t count = 0;
    _ids = new Hashtable(status);
    if (_ids) {
      const UnicodeString * idlist = /* _delegate -> */ getSupportedIDs(count, status);
      for (int i = 0; i < count; ++i) {
        _ids->put(idlist[i], (void*)this, status);
        if (U_FAILURE(status)) {
          delete _ids;
          _ids = nullptr;
          return;
        }
      }
    } else {
      status = U_MEMORY_ALLOCATION_ERROR;
    }
    debugfprintf((stderr,"VCalF: hash=%p, count=%d, err=%s\n", (void*)_ids, count, u_errorName(status)));
  }
  return _ids;
}

const UnicodeString
*VersionCalendarFactory::getSupportedIDs(int32_t &count, UErrorCode &/*status*/) const {
  if(gLocalesDate==nullptr) {
    count = 0;
    
    
    /* gather counts */

#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) count += glue ## Calendar ## x :: countAvailable();
#include "icuglue/glver.h"

#if CAL_FE_DEBUG
    printf("VCalF: count=%d\n", count);
#endif
    UnicodeString *strs = new  UnicodeString[count];
    int32_t i = 0;

#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) i += glue ## Calendar ## x :: appendAvailable(strs, i, count);
#include "icuglue/glver.h"

#if CAL_FE_DEBUG
    printf("VCalF: appended count=%d\n", count);
#endif

    gLocCountDate = count;
    gLocalesDate = strs;
  }
  count = gLocCountDate;
  return gLocalesDate;
}


/* Plugin Code */

#include <stdio.h>
#include <unicode/uversion.h>

static URegistryKey rkcal = nullptr;

void cal_provider_register(UErrorCode &status) {
  debugfprintf((stderr, "about to register VCalF\n"));
  rkcal = Calendar::registerFactory(new VersionCalendarFactory(), status);
  debugfprintf((stderr, ".. registered VCalF, key=%p\n", (void*)rkcal));
}

void cal_provider_unregister(UErrorCode &status) {
  Calendar::unregister(rkcal, status);
}

#else

/* no op- this ICU doesn't support date providers */

void cal_provider_register(UErrorCode &) {
  // not supported
}

void cal_provider_unregister(UErrorCode &) {
  // not supported
}

#endif

/* Plugin- only ICU 4.4+ */
#if (U_ICU_VERSION_MAJOR_NUM > 4) || ((U_ICU_VERSION_MAJOR_NUM==4)&&(U_ICU_VERSION_MINOR_NUM>3))
#include "unicode/icuplug.h"

U_CAPI UPlugTokenReturn U_EXPORT2 cal_provider_plugin (UPlugData *data, UPlugReason reason, UErrorCode *status);

U_CAPI UPlugTokenReturn U_EXPORT2 cal_provider_plugin (UPlugData *data, UPlugReason reason, UErrorCode *status)
{
  switch(reason) {
  case UPLUG_REASON_QUERY:
    uplug_setPlugName(data, "Calendar Provider Plugin");
    uplug_setPlugLevel(data, UPLUG_LEVEL_HIGH);
    break;
  case UPLUG_REASON_LOAD:
    cal_provider_register(*status);
    break;
  case UPLUG_REASON_UNLOAD:
    cal_provider_unregister(*status);
    break;
  default:
    break; /* not handled */
  }
  return UPLUG_TOKEN;
}
#else

/* 
   Note: this ICU version must explicitly call 'cal_provider_plugin'
*/

#endif /* plugin */

#endif /* provider side (vs target) */
