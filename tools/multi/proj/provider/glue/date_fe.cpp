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

#ifndef DATE_FE_DEBUG
#define DATE_FE_DEBUG 0
#endif

#include <icuglue/icuglue.h>
#include "unicode/udat.h"
//#include <unicode/tblcoll.h>
#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"
#include <string.h>
#include <stdio.h>
#include "unicode/ustring.h"
#include "unicode/gregocal.h"



/**
 * Macro to define the Collator_glue_4_2 class 
 */
#ifdef GLUE_VER
#error GLUE_VER is defined
#endif

#define GLUE_VER(x) class GLUE_SYM_V( DateFormat, x ) : public DateFormat {  \
  public:  static DateFormat *create(UDateFormatStyle  timeStyle, \
                                                    UDateFormatStyle  dateStyle, \
                                                    const char        *locale, \
                                                    const UChar       *tzID, \
                                                    int32_t           tzIDLength, \
                                                    const UChar       *pattern,  \
                                                    int32_t           patternLength,  \
                                                      UErrorCode        *status, const Locale &loc, const char *ver); \
  private: UDateFormat *_this; GLUE_SYM_V( DateFormat, x ) ( UDateFormat* tn ); \
    virtual ~ GLUE_SYM_V ( DateFormat, x) ();                             \
  public:                                                               \
    virtual void* getDynamicClassID() const;                            \
    static void* getStaticClassID() ;                                   \
    virtual UnicodeString& format(  Calendar& cal, UnicodeString& appendTo, FieldPosition& pos) const; \
    virtual void parse( const UnicodeString& text, Calendar& cal, ParsePosition& pos) const; \
    virtual Format* clone(void) const; \
  public: static int32_t countAvailable();                              \
public: static int32_t appendAvailable(UnicodeString* strs, int32_t i, int32_t count); \
  }; \



/** ==================================== The following code runs inside the 'target' version (i.e. old ICU) ========== **/
#if defined ( ICUGLUE_VER )



/* code for some version */
#include <icuglue/gluren.h>
#include "oicu.h"

#ifdef GLUE_VER
GLUE_VER( ICUGLUE_VER )
#endif

GLUE_SYM (DateFormat ) :: GLUE_SYM(DateFormat) ( UDateFormat* tn) :
 _this(tn)
{ 

  UErrorCode status = U_ZERO_ERROR;
  adoptCalendar(new GregorianCalendar(status));
}

GLUE_SYM ( DateFormat ) :: ~ GLUE_SYM(DateFormat) () {
#if DATE_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " udat_close");
#endif
    OICU_udat_close(_this);
}

DateFormat *
GLUE_SYM ( DateFormat ) :: create(UDateFormatStyle  timeStyle,
                                                    UDateFormatStyle  dateStyle,
                                                    const char        *locale,
                                                    const UChar       *tzID,
                                                    int32_t           tzIDLength,
                                                    const UChar       *pattern,
                                                    int32_t           patternLength,
                                                    UErrorCode        *status,
                                  const Locale &loc, const char */*ver*/) {
  // TODO: save version
  //char locBuf[200];
  //char kwvBuf[200];
  UDateFormat * uc =  OICU_udat_open( timeStyle, dateStyle, locale,
                                      tzID,
                                      tzIDLength,
                                      pattern,
                                      patternLength,
                                      status);
    if(U_FAILURE(*status)) return NULL; // TODO: ERR?
    DateFormat *c =  new GLUE_SYM( DateFormat ) ( uc );
#if DATE_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " udat_open=%s ->> %p\n", loc.getName(), (void*)c);
#endif
    return c;
}

UnicodeString& GLUE_SYM (DateFormat ) :: format(  Calendar& cal, UnicodeString& appendTo, FieldPosition& pos) const
{
#if DATE_FE_DEBUG
  fprintf(stderr, "VCF " ICUGLUE_VER_STR " - formatting. \n");
#endif
  int32_t len = appendTo.length();

  UChar junk[200];
  UErrorCode status = U_ZERO_ERROR;

  UFieldPosition pos2;

  int32_t nlen = OICU_udat_format(_this,
                                  cal.getTime(status),
                                  junk,
                                  200,
                                  &pos2,
                                  &status);

  // todo: use pos2
  pos.setBeginIndex(len);
  pos.setEndIndex(len += nlen);
  appendTo.append(junk, nlen);

  return appendTo;
}

void  GLUE_SYM (DateFormat ) :: parse( const UnicodeString& text, Calendar& cal, ParsePosition& pos) const
{
  return;
}

Format*  GLUE_SYM (DateFormat ) :: clone(void) const
{
  return NULL;
}



 int32_t GLUE_SYM ( DateFormat ) :: countAvailable() {
    int32_t count =  OICU_udat_countAvailable();
    return count;
 }
 
 
int32_t GLUE_SYM ( DateFormat ) :: appendAvailable(UnicodeString* strs, int32_t i, int32_t /*count*/) {
   int avail = OICU_udat_countAvailable();
   UErrorCode status = U_ZERO_ERROR;
   OICU_u_init(&status);
#if DATE_FE_DEBUG
   fprintf(stderr,  "VCF " ICUGLUE_VER_STR " avail %d - init %s\n", avail, u_errorName(status));
#endif   
    for(int j=0;j<avail;j++) {
         strs[i+j].append(OICU_udat_getAvailable(j));
         strs[i+j].append("@sp=icu");
         strs[i+j].append( ICUGLUE_VER_STR[0] );  // X_y
         strs[i+j].append( ICUGLUE_VER_STR[2] );  // x_Y
#if DATE_FE_DEBUG
         { 
            char foo[999];
            const UChar *ss = strs[i+j].getTerminatedBuffer();
            u_austrcpy(foo, ss);
            //            fprintf(stderr,  "VCF " ICUGLUE_VER_STR " appending [%d+%d=%d] <<%s>>\n", i, j, i+j, foo);
        }
#endif
    }
    return OICU_ucol_countAvailable();
 }

UOBJECT_DEFINE_RTTI_IMPLEMENTATION( GLUE_SYM( DateFormat ) )




#else
/** ==================================== The following code runs inside the 'provider' version (i.e. current ICU) ========== **/

#if (U_ICU_VERSION_MAJOR_NUM < 49)
#define DATE_PROVIDER_UNSUPPORTED
#endif

#ifndef DATE_PROVIDER_UNSUPPORTED
// define Collator_XX
#include "icuglue/glver.h"

// generate list of versions
static
#include <icuglue/fe_verlist.h>

class VersionDateFormatFactory : public UObject  {
public:
  virtual DateFormat *createFormat(UDateFormatStyle  timeStyle,
                                   UDateFormatStyle  dateStyle,
                                   const char        *locale,
                                   const UChar       *tzID,
                                   int32_t           tzIDLength,
                                   const UChar       *pattern,
                                   int32_t           patternLength,
                                   UErrorCode        *status);
  virtual const UnicodeString *getSupportedIDs(int32_t &count, UErrorCode &status);
  virtual void* getDynamicClassID() const; 
  static void* getStaticClassID() ; 
};

UOBJECT_DEFINE_RTTI_IMPLEMENTATION( VersionDateFormatFactory )

DateFormat *VersionDateFormatFactory::createFormat(UDateFormatStyle  timeStyle,
                                                    UDateFormatStyle  dateStyle,
                                                    const char        *locale,
                                                    const UChar       *tzID,
                                                    int32_t           tzIDLength,
                                                    const UChar       *pattern,
                                                    int32_t           patternLength,
                                                       UErrorCode        *status) {
    Locale loc(locale);
    // pull off provider #
    char provider[200];
#if DATE_FE_DEBUG
    fprintf(stderr,  "VCF:CC %s\n", loc.getName());
#endif
    int32_t len = loc.getKeywordValue("sp", provider, 200, *status);
    if(U_FAILURE(*status)||len==0) return NULL;
#if DATE_FE_DEBUG
    fprintf(stderr,  "VCF:KWV> %s/%d\n", u_errorName(*status), len);
#endif
    provider[len]=0;
#if DATE_FE_DEBUG
    fprintf(stderr,  "VCF:KWV %s\n", provider);
#endif
    if(strncmp(provider,"icu",3)) return NULL;
    const char *icuver=provider+3;
#if DATE_FE_DEBUG
    fprintf(stderr,  "VCF:ICUV %s\n", icuver);
#endif
    
#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) /*printf("%c/%c|%c/%c\n", icuver[0],(#x)[0],icuver[1],(#x)[2]);*/  if(icuver[0]== (#x)[0] && icuver[1]==(#x)[2]) { DateFormat *c = glue ## DateFormat ## x :: create(timeStyle,dateStyle,locale,tzID,tzIDLength,pattern,patternLength,status,loc,icuver); /*fprintf(stderr, "VCF::CC %s -> %p\n", loc.getName(), c);*/ return c; }
#include "icuglue/glver.h"
#if DATE_FE_DEBUG
    fprintf(stderr,  "VCF:CC %s failed\n", loc.getName());
#endif

    return NULL;
}


static const UnicodeString *gLocalesDate = NULL;
static  int32_t gLocCountDate = 0; 


const UnicodeString
*VersionDateFormatFactory::getSupportedIDs(int32_t &count, UErrorCode &/*status*/) {
  if(gLocalesDate==NULL) {
    count = 0;
    
    
    /* gather counts */

#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) count += glue ## DateFormat ## x :: countAvailable();
#include "icuglue/glver.h"

#if DATE_FE_DEBUG
    printf("VCF: count=%d\n", count);
#endif
    UnicodeString *strs = new  UnicodeString[count];
    int32_t i = 0;

#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) i += glue ## DateFormat ## x :: appendAvailable(strs, i, count);
#include "icuglue/glver.h"

#if DATE_FE_DEBUG
    printf("VCF: appended count=%d\n", count);
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

//static URegistryKey rkdate = NULL;

static VersionDateFormatFactory vdf;

extern "C" UDateFormat *versionDateFormatOpener(UDateFormatStyle  timeStyle,
                                                    UDateFormatStyle  dateStyle,
                                                    const char        *locale,
                                                    const UChar       *tzID,
                                                    int32_t           tzIDLength,
                                                    const UChar       *pattern,
                                                    int32_t           patternLength,
                                                       UErrorCode        *status) {
  DateFormat *df = vdf.createFormat(timeStyle,dateStyle,locale,tzID,tzIDLength,pattern,patternLength,status);
  // printf("Hey! I got: %s -> %p\n", locale, df);
  return (UDateFormat*)df;
}

void date_provider_register(UErrorCode &status) {
  udat_registerOpener(versionDateFormatOpener, &status);
  //   rkdate = DateFormat::registerFactory(new VersionDateFormatFactory(), status);
}

void date_provider_unregister(UErrorCode &status) {
  udat_unregisterOpener(versionDateFormatOpener, &status);
}

#else

/* no op- this ICU doesn't support date providers */

void date_provider_register(UErrorCode &) {
  // not supported
}

void date_provider_unregister(UErrorCode &) {
  // not supported
}

#endif

/* Plugin- only ICU 4.4+ */
#if (U_ICU_VERSION_MAJOR_NUM > 4) || ((U_ICU_VERSION_MAJOR_NUM==4)&&(U_ICU_VERSION_MINOR_NUM>3))
#include "unicode/icuplug.h"

U_CAPI UPlugTokenReturn U_EXPORT2 date_provider_plugin (UPlugData *data, UPlugReason reason, UErrorCode *status);

U_CAPI UPlugTokenReturn U_EXPORT2 date_provider_plugin (UPlugData *data, UPlugReason reason, UErrorCode *status)
{
  switch(reason) {
  case UPLUG_REASON_QUERY:
    uplug_setPlugName(data, "Date Provider Plugin");
    uplug_setPlugLevel(data, UPLUG_LEVEL_HIGH);
    break;
  case UPLUG_REASON_LOAD:
    date_provider_register(*status);
    break;
  case UPLUG_REASON_UNLOAD:
    date_provider_unregister(*status);
    break;
  default:
    break; /* not handled */
  }
  return UPLUG_TOKEN;
}
#else

/* 
   Note: this ICU version must explicitly call 'date_provider_plugin'
*/

#endif /* plugin */

#endif /* provider side (vs target) */
