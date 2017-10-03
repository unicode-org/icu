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

#ifndef COLL_FE_DEBUG
#define COLL_FE_DEBUG 0
#endif

#include <icuglue/icuglue.h>
#include <unicode/coll.h>
//#include <unicode/tblcoll.h>
#include <unicode/ucol.h>
#include <string.h>
#include <stdio.h>
#include "unicode/ustring.h"

#if COLL_FE_DEBUG
#define debugfprintf(x) fprintf x
#else
#define debugfprintf(x)
#endif

/*
 * Before ICU 50.0.2 (50m2) - there was a different collator signature.
 * see: ticket:9460 ticket:9346
 */
#if (U_ICU_VERSION_MAJOR_NUM < 50) || ((U_ICU_VERSION_MAJOR_NUM==50)&&(U_ICU_VERSION_MINOR_NUM==0)&&(U_ICU_VERSION_PATCHLEVEL_NUM<2))
#define PRE_50_0_2_COLLATOR
#define CONST_BEFORE_50_0_2 const
#define CONST_AFTER_50_0_2 
#define REF_AFTER_50_0_2
#else
/* "current" API */
#define CONST_BEFORE_50_0_2
#define CONST_AFTER_50_0_2 const
#define REF_AFTER_50_0_2 &
#endif

/**
 * Macro to define the Collator_glue_4_2 class 
 */
#define GLUE_VER(x) class GLUE_SYM_V( Collator, x ) : public Collator {  \
    \
  public:  static Collator *create(const Locale &loc, const char *ver); \
  private: UCollator *_this; GLUE_SYM_V( Collator, x ) ( UCollator* tn ) : _this(tn){} \
    virtual ~ GLUE_SYM_V ( Collator, x) ();                             \
  public:                                                               \
    virtual void* getDynamicClassID() const;                            \
    static void* getStaticClassID() ;                                   \
    virtual Collator* clone() const;                                    \
    virtual UCollationResult compare(const UnicodeString&, const UnicodeString&, UErrorCode&) const; \
    virtual UCollationResult compare(const UnicodeString&, const UnicodeString&, int32_t, UErrorCode&) const; \
    virtual UCollationResult compare(const UChar*, int32_t, const UChar*, int32_t, UErrorCode&) const; \
    virtual CollationKey& getCollationKey(const UnicodeString&, CollationKey&, UErrorCode&) const; \
    virtual CollationKey& getCollationKey(const UChar*, int32_t, CollationKey&, UErrorCode&) const; \
    virtual int32_t hashCode() const;                                   \
    virtual CONST_BEFORE_50_0_2 Locale getLocale(ULocDataLocaleType, UErrorCode&) const; \
    virtual ECollationStrength getStrength() const;                     \
    virtual void setStrength(ECollationStrength);                       \
    virtual void getVersion(uint8_t*) const;                            \
    virtual void setAttribute(UColAttribute, UColAttributeValue, UErrorCode&) ; \
    virtual UColAttributeValue getAttribute(UColAttribute, UErrorCode&) CONST_AFTER_50_0_2; \
    virtual uint32_t setVariableTop(const UChar*, int32_t, UErrorCode&); \
    virtual uint32_t setVariableTop(const UnicodeString REF_AFTER_50_0_2, UErrorCode&);        \
    virtual void setVariableTop(uint32_t, UErrorCode&);                 \
    virtual uint32_t getVariableTop(UErrorCode&) const;                 \
    virtual Collator* safeClone() CONST_AFTER_50_0_2 ;                                      \
    virtual int32_t getSortKey(const UnicodeString&, uint8_t*, int32_t) const; \
    virtual int32_t getSortKey(const UChar*, int32_t, uint8_t*, int32_t) const; \
  public: static int32_t countAvailable();                              \
  public: static int32_t appendAvailable(UnicodeString* strs, int32_t i, int32_t count); \
  public: virtual int32_t internalGetShortDefinitionString(const char *locale, char *buffer, int32_t capacity, UErrorCode &status) const; \
  };

/** ==================================== The following code runs inside the 'target' version (i.e. old ICU) ========== **/
#if defined ( ICUGLUE_VER )


// these from tblcoll.h
static Collator::ECollationStrength _getECollationStrength(
                                       const UCollationStrength &strength) 
{
    switch (strength)
    {
    case UCOL_PRIMARY :
        return Collator::PRIMARY;
    case UCOL_SECONDARY :
        return Collator::SECONDARY;
    case UCOL_TERTIARY :
        return Collator::TERTIARY;
    case UCOL_QUATERNARY :
        return Collator::QUATERNARY;
    default :
        return Collator::IDENTICAL;
    }
}

static UCollationStrength _getUCollationStrength(
                             const Collator::ECollationStrength &strength) 
{
    switch (strength)
    {
    case Collator::PRIMARY :
        return UCOL_PRIMARY;
    case Collator::SECONDARY :
        return UCOL_SECONDARY;
    case Collator::TERTIARY :
        return UCOL_TERTIARY;
    case Collator::QUATERNARY :
        return UCOL_QUATERNARY;
    default :
        return UCOL_IDENTICAL;
    }
}



/* code for some version */
#include <icuglue/gluren.h>

#include "oicu.h"

/* Expand GLUE_VER to define the class */
#ifdef GLUE_VER
GLUE_VER( ICUGLUE_VER )
#endif

GLUE_SYM ( Collator ) :: ~ GLUE_SYM(Collator) () {
#if COLL_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " ucol_close");
#endif
    OICU_ucol_close(_this);
}

#if 0
U_CFUNC int32_t U_CALLCONV
GLUE_SYM ( glue_calcSortKey) (const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        uint32_t        resultLength,
        UBool allocateSKBuffer,
        UErrorCode *status);

#endif

Collator *
GLUE_SYM ( Collator ) :: create (const Locale &loc, const char */*ver*/) {
  // TODO: save 'ver' off.
    UErrorCode status = U_ZERO_ERROR;
    char locBuf[200];
    char kwvBuf[200];
    int32_t len = loc.getKeywordValue("collation", kwvBuf, 200, status);
    strcpy(locBuf,loc.getBaseName());
    if(len>0) {
        strcat(locBuf,"@collator=");
        strcat(locBuf,kwvBuf);
    }
    UCollator * uc =  OICU_ucol_open( locBuf, status);
    if(U_FAILURE(status)) return NULL; // TODO: ERR?
    Collator *c =  new GLUE_SYM( Collator ) ( uc );
#if COLL_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " ucol_open=%s ->> %p\n", locBuf, c);
#endif
    return c;
}

UOBJECT_DEFINE_RTTI_IMPLEMENTATION( GLUE_SYM( Collator ) )

Collator* GLUE_SYM ( Collator ) :: clone() const  {
    UErrorCode status = U_ZERO_ERROR;
#if COLL_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " clone %p -> " , this);
#endif
    UCollator *clc = OICU_ucol_safeClone( _this, NULL, 0, &status);
#if COLL_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " .. safeclone %s _this %p-> %p " , u_errorName(status), _this, clc);
#endif
    if(U_FAILURE(status)||clc==NULL) return NULL;
    Collator *c = new GLUE_SYM( Collator ) ( clc );
#if COLL_FE_DEBUG
    fprintf(stderr, "VCF " ICUGLUE_VER_STR " .. wrap(%p) -> %p\n", clc, c);
#endif

    return c;
}


UCollationResult GLUE_SYM ( Collator ) :: compare(const UnicodeString&, const UnicodeString&, UErrorCode&) const  {
    return (UCollationResult)0;
}


UCollationResult GLUE_SYM ( Collator ) :: compare(const UnicodeString&, const UnicodeString&, int32_t, UErrorCode&) const  {
    return (UCollationResult)0;
}


UCollationResult GLUE_SYM ( Collator ) :: compare(const UChar* s, int32_t sl, const UChar* d , int32_t dl, UErrorCode&/*e*/ ) const  {
    return OICU_ucol_strcoll(_this, s, sl, d, dl);
}

#include "unicode/sortkey.h"

static CollationKey kk;

CollationKey& GLUE_SYM ( Collator ) :: getCollationKey(const UnicodeString&, CollationKey&, UErrorCode&) const  {
  //#if COLL_FE_DEBUG
    fprintf(stderr,  "VCF " ICUGLUE_VER_STR " GCK - notimp");
    //#endif
return kk;
}


CollationKey& GLUE_SYM ( Collator ) :: getCollationKey(const UChar*, int32_t, CollationKey&, UErrorCode&) const  {
    fprintf(stderr,  "VCF " ICUGLUE_VER_STR " GKK2 - notimp");
return kk;
}


int32_t GLUE_SYM ( Collator ) :: hashCode() const  {
    return 0;
}


CONST_BEFORE_50_0_2 Locale GLUE_SYM ( Collator ) :: getLocale(ULocDataLocaleType, UErrorCode&) const  {
    return Locale();
}


Collator::ECollationStrength
 GLUE_SYM ( Collator ) :: getStrength() const  {
    return _getECollationStrength(OICU_ucol_getStrength(_this));
}


void GLUE_SYM ( Collator ) :: setStrength(ECollationStrength s)  {
    OICU_ucol_setStrength(_this, _getUCollationStrength(s));
}


void GLUE_SYM ( Collator ) :: getVersion(uint8_t*) const  {
}


void GLUE_SYM ( Collator ) :: setAttribute(UColAttribute, UColAttributeValue, UErrorCode&) {
}


UColAttributeValue GLUE_SYM ( Collator ) :: getAttribute(UColAttribute, UErrorCode&) CONST_AFTER_50_0_2 {
return (UColAttributeValue)0;
}


uint32_t GLUE_SYM ( Collator ) :: setVariableTop(const UChar*, int32_t, UErrorCode&)  {
return 0;
}


uint32_t GLUE_SYM ( Collator ) :: setVariableTop(const UnicodeString REF_AFTER_50_0_2, UErrorCode&)  {
return 0;
}


void GLUE_SYM ( Collator ) :: setVariableTop(uint32_t, UErrorCode&)  {
}


uint32_t GLUE_SYM ( Collator ) :: getVariableTop(UErrorCode&) const  {
return 0;
}


Collator* GLUE_SYM ( Collator ) :: safeClone() CONST_AFTER_50_0_2 {
    return clone();
}


int32_t GLUE_SYM ( Collator ) :: getSortKey(const UnicodeString& s, uint8_t*buf, int32_t len) const  {
#if COLL_FE_DEBUG
  fprintf(stderr,  "VCF " ICUGLUE_VER_STR " GSK");
#endif
    return getSortKey(s.getBuffer(),s.length(), buf, len);
}




int32_t GLUE_SYM ( Collator ) :: getSortKey(const UChar*s, int32_t l, uint8_t*d, int32_t b) const  {
#if COLL_FE_DEBUG
    fprintf(stderr,  "VCF " ICUGLUE_VER_STR " GKS");
#endif
    return OICU_ucol_getSortKey(_this, s,l,d,b);
}

int32_t GLUE_SYM (Collator ) ::  internalGetShortDefinitionString(const char *locale, char *buffer, int32_t capacity, UErrorCode &status) const {
  if(U_FAILURE(status)) return 0;
  int32_t intRes = OICU_ucol_getShortDefinitionString(_this, locale, buffer, capacity, &status);
  int32_t newRes = (intRes += 7); /* _PICU38 */
  int32_t remainCap = capacity - newRes;

  if(remainCap < 0 && U_SUCCESS(status)) {
    status = U_BUFFER_OVERFLOW_ERROR; /* ran out of space on our watch */
  }
  if(U_SUCCESS(status)) {
    char *p = buffer+strlen(buffer);
    strncat(p,"_PICU",5);
    p +=5 ;
    CPY_VERSTR(p, ICUGLUE_VER_STR);
    p +=2;
    if(remainCap>0) {
      *(p++)=0;
    }
  }
  return newRes;
}



 int32_t GLUE_SYM ( Collator ) :: countAvailable() {
    int32_t count =  OICU_ucol_countAvailable();
    return count;
 }
 
 
int32_t GLUE_SYM ( Collator ) :: appendAvailable(UnicodeString* strs, int32_t i, int32_t /*count*/) {
   int avail = OICU_ucol_countAvailable();
   UErrorCode status = U_ZERO_ERROR;
   OICU_u_init(&status);
#if COLL_FE_DEBUG
   fprintf(stderr,  "VCF " ICUGLUE_VER_STR " avail %d - init %s\n", avail, u_errorName(status));
#endif   
    for(int j=0;j<avail;j++) {
         strs[i+j].append(OICU_ucol_getAvailable(j));
         strs[i+j].append("@sp=icu");
         
         if(IS_OLD_VERSTR(ICUGLUE_VER_STR)) {
           strs[i+j].append( ICUGLUE_VER_STR[OLD_VERSTR_MAJ] );  // X_y
           strs[i+j].append( ICUGLUE_VER_STR[OLD_VERSTR_MIN] );  // x_Y
         } else {
           strs[i+j].append( ICUGLUE_VER_STR[NEW_VERSTR_MAJ] );  // Xy_
           strs[i+j].append( ICUGLUE_VER_STR[NEW_VERSTR_MIN] );  // xY_
         }

#if COLL_FE_DEBUG
         { 
            char foo[999];
            const UChar *ss = strs[i+j].getTerminatedBuffer();
            u_austrcpy(foo, ss);
            debugfprintf((stderr,  "VCF " ICUGLUE_VER_STR " appending [%d+%d=%d] <<%s>>\n", i, j, i+j, foo));
        }
#endif
    }
    return OICU_ucol_countAvailable();
 }



#else
/** ==================================== The following code runs inside the 'provider' version (i.e. current ICU) ========== **/

// define Collator_XX
#include "icuglue/glver.h"

// generate list of versions
static
#include <icuglue/fe_verlist.h>

class VersionCollatorFactory : public CollatorFactory {
public:
  virtual Collator *createCollator(const Locale &loc);
  virtual const UnicodeString *getSupportedIDs(int32_t &count, UErrorCode &status);
  virtual void* getDynamicClassID() const; 
  static void* getStaticClassID() ; 
};

UOBJECT_DEFINE_RTTI_IMPLEMENTATION( VersionCollatorFactory )

Collator *VersionCollatorFactory::createCollator(const Locale &loc) {
    // pull off provider #
    char provider[200];
    UErrorCode status = U_ZERO_ERROR;
#if COLL_FE_DEBUG
    fprintf(stderr,  "VCF:CC %s\n", loc.getName());
#endif
    int32_t len = loc.getKeywordValue("sp", provider, 200, status);
    if(U_FAILURE(status)||len==0) return NULL;
#if COLL_FE_DEBUG
    fprintf(stderr,  "VCF:KWV> %s/%d\n", u_errorName(status), len);
#endif
    provider[len]=0;
#if COLL_FE_DEBUG
    fprintf(stderr,  "VCF:KWV %s\n", provider);
#endif
    if(strncmp(provider,"icu",3)) return NULL;
    const char *icuver=provider+3;
#if COLL_FE_DEBUG
    fprintf(stderr,  "VCF:ICUV %s\n", icuver);
#endif
    
#if defined(GLUE_VER)
#undef GLUE_VER
#endif

#define GLUE_VER(x) \
    debugfprintf((stderr,"%c/%c|%c/%c\n", icuver[0],(#x)[0],icuver[1],(#x)[2]));  \
    if(CMP_VERSTR(icuver, (#x))) {                                      \
      Collator *c = glue ## Collator ## x :: create(loc, icuver); \
      debugfprintf((stderr, "VCF::CC %s -> %p\n", loc.getName(), c)); \
      return c; \
    }

#include "icuglue/glver.h"
#if COLL_FE_DEBUG
    fprintf(stderr,  "VCF:CC %s failed\n", loc.getName());
#endif

    return NULL;
}


static const UnicodeString *gLocales = NULL;
static  int32_t gLocCount = 0; 

const UnicodeString
*VersionCollatorFactory::getSupportedIDs(int32_t &count, UErrorCode &/*status*/) {
  if(gLocales==NULL) {
    count = 0;
    
    
    /* gather counts */
#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) count += glue ## Collator ## x :: countAvailable();
#include "icuglue/glver.h"

#if COLL_FE_DEBUG
    printf("VCF: count=%d\n", count);
#endif
    UnicodeString *strs = new  UnicodeString[count];
    int32_t i = 0;

#if defined(GLUE_VER)
#undef GLUE_VER
#endif
#define GLUE_VER(x) i += glue ## Collator ## x :: appendAvailable(strs, i, count);
#include "icuglue/glver.h"

#if COLL_FE_DEBUG
    printf("VCF: appended count=%d\n", count);
#endif

    gLocCount = count;
    gLocales = strs;
  }
  count = gLocCount;
  return gLocales;
}


/* Plugin Code */

#include <stdio.h>
#include <unicode/uversion.h>

static URegistryKey rk = NULL;

void coll_provider_register(UErrorCode &status) {
  rk = Collator::registerFactory(new VersionCollatorFactory(), status);
}

void coll_provider_unregister(UErrorCode &status) {
  Collator::unregister(rk, status);
}

/* Plugin- only ICU 4.4+ */
#if (U_ICU_VERSION_MAJOR_NUM > 4) || ((U_ICU_VERSION_MAJOR_NUM==4)&&(U_ICU_VERSION_MINOR_NUM>3))
#include "unicode/icuplug.h"

U_CAPI UPlugTokenReturn U_EXPORT2 coll_provider_plugin (UPlugData *data, UPlugReason reason, UErrorCode *status);

U_CAPI UPlugTokenReturn U_EXPORT2 coll_provider_plugin (UPlugData *data, UPlugReason reason, UErrorCode *status)
{
  switch(reason) {
  case UPLUG_REASON_QUERY:
    uplug_setPlugName(data, "Collation Provider Plugin");
    uplug_setPlugLevel(data, UPLUG_LEVEL_HIGH);
    break;
  case UPLUG_REASON_LOAD:
    coll_provider_register(*status);
    break;
  case UPLUG_REASON_UNLOAD:
    coll_provider_unregister(*status);
    break;
  default:
    break; /* not handled */
  }
  return UPLUG_TOKEN;
}
#else

/* 
   Note: this ICU version must explicitly call 'coll_provider_plugin'
*/

#endif /* plugin */

#endif /* provider side (vs target) */
