/*
*******************************************************************************
* Copyright (C) 1997-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File NUMFMT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/18/97    clhuang     Implemented with C++ APIs.
*   04/17/97    aliu        Enlarged MAX_INTEGER_DIGITS to fully accomodate the
*                           largest double, by default.
*                           Changed DigitCount to int per code review.
*    07/20/98    stephen        Changed operator== to check for grouping
*                            Changed setMaxIntegerDigits per Java implementation.
*                            Changed setMinIntegerDigits per Java implementation.
*                            Changed setMinFractionDigits per Java implementation.
*                            Changed setMaxFractionDigits per Java implementation.
********************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/numfmt.h"
#include "unicode/locid.h"
#include "unicode/resbund.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/ustring.h"
#include "unicode/ucurr.h"
#include "unicode/curramt.h"
#include "uhash.h"
#include "cmemory.h"
#include "iculserv.h"
#include "ucln_in.h"
#include "cstring.h"
#include <float.h>

//#define FMT_DEBUG

#ifdef FMT_DEBUG
#include <stdio.h>
static void debugout(UnicodeString s) {
    char buf[2000];
    s.extract((int32_t) 0, s.length(), buf);
    printf("%s", buf);
}
#define debug(x) printf("%s", x);
#else
#define debugout(x)
#define debug(x)
#endif

// If no number pattern can be located for a locale, this is the last
// resort.
static const UChar gLastResortDecimalPat[] = {
    0x23, 0x30, 0x2E, 0x23, 0x23, 0x23, 0x3B, 0x2D, 0x23, 0x30, 0x2E, 0x23, 0x23, 0x23, 0 /* "#0.###;-#0.###" */
};
static const UChar gLastResortCurrencyPat[] = {
    0x24, 0x23, 0x30, 0x2E, 0x30, 0x30, 0x3B, 0x28, 0x24, 0x23, 0x30, 0x2E, 0x30, 0x30, 0x29, 0 /* "$#0.00;($#0.00)" */
};
static const UChar gLastResortPercentPat[] = {
    0x23, 0x30, 0x25, 0 /* "#0%" */
};
static const UChar gLastResortScientificPat[] = {
    0x23, 0x45, 0x30, 0 /* "#E0" */
};
// *****************************************************************************
// class NumberFormat
// *****************************************************************************

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_ABSTRACT_RTTI_IMPLEMENTATION(NumberFormat)
// If the maximum base 10 exponent were 4, then the largest number would
// be 99,999 which has 5 digits.
const int32_t NumberFormat::fgMaxIntegerDigits = DBL_MAX_10_EXP + 1; // Should be ~40 ? --srl
const int32_t NumberFormat::fgMinIntegerDigits = 127;

const int32_t NumberFormat::fgNumberPatternsCount = 3;

const UChar * const NumberFormat::fgLastResortNumberPatterns[] =
{
    gLastResortDecimalPat,
    gLastResortCurrencyPat,
    gLastResortPercentPat,
    gLastResortScientificPat
};

// -------------------------------------
// SimpleNumberFormatFactory implementation
NumberFormatFactory::~NumberFormatFactory() {}
SimpleNumberFormatFactory::~SimpleNumberFormatFactory() {}

UBool SimpleNumberFormatFactory::visible(void) const {
    return _visible;
}

const UnicodeString *
SimpleNumberFormatFactory::getSupportedIDs(int32_t &count, UErrorCode& status) const 
{
    if (U_SUCCESS(status)) {
        count = 1;
        return &_id;
    }
    count = 0;
    return NULL;
}


// -------------------------------------
// default constructor
NumberFormat::NumberFormat()
:   fGroupingUsed(TRUE),
    fMaxIntegerDigits(fgMaxIntegerDigits),
    fMinIntegerDigits(1),
    fMaxFractionDigits(3), // invariant, >= minFractionDigits
    fMinFractionDigits(0),
    fParseIntegerOnly(FALSE)
{
    fCurrency[0] = 0;
}

// -------------------------------------

NumberFormat::~NumberFormat()
{
}

// -------------------------------------
// copy constructor

NumberFormat::NumberFormat(const NumberFormat &source)
:   Format(source)
{
    *this = source;
}

// -------------------------------------
// assignment operator

NumberFormat&
NumberFormat::operator=(const NumberFormat& rhs)
{
    if (this != &rhs)
    {
        fGroupingUsed = rhs.fGroupingUsed;
        fMaxIntegerDigits = rhs.fMaxIntegerDigits;
        fMinIntegerDigits = rhs.fMinIntegerDigits;
        fMaxFractionDigits = rhs.fMaxFractionDigits;
        fMinFractionDigits = rhs.fMinFractionDigits;
        fParseIntegerOnly = rhs.fParseIntegerOnly;
        u_strncpy(fCurrency, rhs.fCurrency, 4);
    }
    return *this;
}

// -------------------------------------

UBool
NumberFormat::operator==(const Format& that) const
{
    // Format::operator== guarantees this cast is safe
    NumberFormat* other = (NumberFormat*)&that;

#ifdef FMT_DEBUG
    // This code makes it easy to determine why two format objects that should
    // be equal aren't.
    UBool first = TRUE;
    if (!Format::operator==(that)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Format::!=");
    }
    if (!(fMaxIntegerDigits == other->fMaxIntegerDigits &&
          fMinIntegerDigits == other->fMinIntegerDigits)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Integer digits !=");
    }
    if (!(fMaxFractionDigits == other->fMaxFractionDigits &&
          fMinFractionDigits == other->fMinFractionDigits)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("Fraction digits !=");
    }
    if (!(fGroupingUsed == other->fGroupingUsed)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("fGroupingUsed != ");
    }
    if (!(fParseIntegerOnly == other->fParseIntegerOnly)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("fParseIntegerOnly != ");
    }
    if (!(u_strcmp(fCurrency, other->fCurrency) == 0)) {
        if (first) { printf("[ "); first = FALSE; } else { printf(", "); }
        debug("fCurrency !=");
    }
    if (!first) { printf(" ]"); }    
#endif

    return ((this == &that) ||
            ((Format::operator==(that) &&
              fMaxIntegerDigits == other->fMaxIntegerDigits &&
              fMinIntegerDigits == other->fMinIntegerDigits &&
              fMaxFractionDigits == other->fMaxFractionDigits &&
              fMinFractionDigits == other->fMinFractionDigits &&
              fGroupingUsed == other->fGroupingUsed &&
              fParseIntegerOnly == other->fParseIntegerOnly &&
              u_strcmp(fCurrency, other->fCurrency) == 0)));
}

// -------------------------------------x
// Formats the number object and save the format
// result in the toAppendTo string buffer.

UnicodeString&
NumberFormat::format(const Formattable& obj,
                        UnicodeString& appendTo,
                        FieldPosition& pos,
                        UErrorCode& status) const
{
    if (U_FAILURE(status)) return appendTo;

    NumberFormat* nonconst = (NumberFormat*) this;
    const Formattable* n = &obj;

    UChar save[4];
    UBool setCurr = FALSE;
    const UObject* o = obj.getObject(); // most commonly o==NULL
    if (o != NULL &&
        o->getDynamicClassID() == CurrencyAmount::getStaticClassID()) {
        // getISOCurrency() returns a pointer to internal storage, so we
        // copy it to retain it across the call to setCurrency().
        const CurrencyAmount* amt = (const CurrencyAmount*) o;
        const UChar* curr = amt->getISOCurrency();
        u_strcpy(save, getCurrency());
        setCurr = (u_strcmp(curr, save) != 0);
        if (setCurr) {
            nonconst->setCurrency(curr, status);
        }
        n = &amt->getNumber();
    }

    switch (n->getType()) {
    case Formattable::kDouble:
        format(n->getDouble(), appendTo, pos);
        break;
    case Formattable::kLong:
        format(n->getLong(), appendTo, pos);
        break;
    case Formattable::kInt64:
        format(n->getInt64(), appendTo, pos);
        break;
    default:
        status = U_INVALID_FORMAT_ERROR;
        break;
    }

    if (setCurr) {
        UErrorCode ok = U_ZERO_ERROR;
        nonconst->setCurrency(save, ok); // always restore currency
    }
    return appendTo;
}

// -------------------------------------

UnicodeString& 
NumberFormat::format(int64_t number,
                     UnicodeString& appendTo,
                     FieldPosition& pos) const
{
	// default so we don't introduce a new abstract method
	return format((int32_t)number, appendTo, pos);
}

// -------------------------------------
// Parses the string and save the result object as well
// as the final parsed position.

void
NumberFormat::parseObject(const UnicodeString& source,
                             Formattable& result,
                             ParsePosition& parse_pos) const
{
    parse(source, result, parse_pos);
}

// -------------------------------------
// Formats a double number and save the result in a string.

UnicodeString&
NumberFormat::format(double number, UnicodeString& appendTo) const
{
    FieldPosition pos(0);
    return format(number, appendTo, pos);
}

// -------------------------------------
// Formats a long number and save the result in a string.

UnicodeString&
NumberFormat::format(int32_t number, UnicodeString& appendTo) const
{
    FieldPosition pos(0);
    return format(number, appendTo, pos);
}

// -------------------------------------
// Formats a long number and save the result in a string.

UnicodeString&
NumberFormat::format(int64_t number, UnicodeString& appendTo) const
{
    FieldPosition pos(0);
    return format(number, appendTo, pos);
}

// -------------------------------------
// Parses the text and save the result object.  If the returned
// parse position is 0, that means the parsing failed, the status
// code needs to be set to failure.  Ignores the returned parse
// position, otherwise.

void
NumberFormat::parse(const UnicodeString& text,
                        Formattable& result,
                        UErrorCode& status) const
{
    if (U_FAILURE(status)) return;

    ParsePosition parsePosition(0);
    parse(text, result, parsePosition);
    if (parsePosition.getIndex() == 0) {
        status = U_INVALID_FORMAT_ERROR;
    }
}

Formattable& NumberFormat::parseCurrency(const UnicodeString& text,
                                         Formattable& result,
                                         ParsePosition& pos) const {
    // Default implementation only -- subclasses should override
    int32_t start = pos.getIndex();
    parse(text, result, pos);
    if (pos.getIndex() != start) {
        UChar curr[4];
        UErrorCode ec = U_ZERO_ERROR;
        getEffectiveCurrency(curr, ec);
        if (U_SUCCESS(ec)) {
            Formattable n(result);
            result.adoptObject(new CurrencyAmount(n, curr, ec));
            if (U_FAILURE(ec)) {
                pos.setIndex(start); // indicate failure
            }
        }
    }
    return result;
}

// -------------------------------------
// Sets to only parse integers.

void
NumberFormat::setParseIntegerOnly(UBool value)
{
    fParseIntegerOnly = value;
}

// -------------------------------------
// Create a number style NumberFormat instance with the default locale.

NumberFormat*
NumberFormat::createInstance(UErrorCode& status)
{
    return createInstance(Locale::getDefault(), kNumberStyle, status);
}

// -------------------------------------
// Create a number style NumberFormat instance with the inLocale locale.

NumberFormat*
NumberFormat::createInstance(const Locale& inLocale, UErrorCode& status)
{
    return createInstance(inLocale, kNumberStyle, status);
}

// -------------------------------------
// Create a currency style NumberFormat instance with the default locale.

NumberFormat*
NumberFormat::createCurrencyInstance(UErrorCode& status)
{
    return createCurrencyInstance(Locale::getDefault(),  status);
}

// -------------------------------------
// Create a currency style NumberFormat instance with the inLocale locale.

NumberFormat*
NumberFormat::createCurrencyInstance(const Locale& inLocale, UErrorCode& status)
{  
    return createInstance(inLocale, kCurrencyStyle, status);
}

// -------------------------------------
// Create a percent style NumberFormat instance with the default locale.

NumberFormat*
NumberFormat::createPercentInstance(UErrorCode& status)
{
    return createInstance(Locale::getDefault(), kPercentStyle, status);
}

// -------------------------------------
// Create a percent style NumberFormat instance with the inLocale locale.

NumberFormat*
NumberFormat::createPercentInstance(const Locale& inLocale, UErrorCode& status)
{
    return createInstance(inLocale, kPercentStyle, status);
}

// -------------------------------------
// Create a scientific style NumberFormat instance with the default locale.

NumberFormat*
NumberFormat::createScientificInstance(UErrorCode& status)
{
    return createInstance(Locale::getDefault(), kScientificStyle, status);
}

// -------------------------------------
// Create a scientific style NumberFormat instance with the inLocale locale.

NumberFormat*
NumberFormat::createScientificInstance(const Locale& inLocale, UErrorCode& status)
{
    return createInstance(inLocale, kScientificStyle, status);
}

// -------------------------------------

const Locale*
NumberFormat::getAvailableLocales(int32_t& count)
{
    return Locale::getAvailableLocales(count);
}

// ------------------------------------------
//
// Registration
//
//-------------------------------------------

static ICULocaleService* gService = NULL;

// -------------------------------------

class ICUNumberFormatFactory : public ICUResourceBundleFactory {
protected:
  virtual UObject* handleCreate(const Locale& loc, int32_t kind, const ICUService* /* service */, UErrorCode& status) const {
// !!! kind is not an EStyles, need to determine how to handle this
	  return NumberFormat::makeInstance(loc, (NumberFormat::EStyles)kind, status);
  }
};

// -------------------------------------

class NFFactory : public LocaleKeyFactory {
private:
  NumberFormatFactory* _delegate;
  Hashtable* _ids;

public:
  NFFactory(NumberFormatFactory* delegate) 
    : LocaleKeyFactory(delegate->visible() ? VISIBLE : INVISIBLE)
    , _delegate(delegate)
    , _ids(NULL)
  {
  }

  virtual ~NFFactory()
  {
    delete _delegate;
    delete _ids;
  }

  virtual UObject* create(const ICUServiceKey& key, const ICUService* service, UErrorCode& status) const
  {
    if (handlesKey(key, status)) {
      const LocaleKey& lkey = (const LocaleKey&)key;
      Locale loc;
      lkey.canonicalLocale(loc);
      int32_t kind = lkey.kind();

      UObject* result = _delegate->createFormat(loc, (UNumberFormatStyle)(kind+1));
      if (result == NULL) {
        result = service->getKey((ICUServiceKey&)key /* cast away const */, NULL, this, status);
      }
      return result;
    }
    return NULL;
  }

protected:
  /**
   * Return the set of ids that this factory supports (visible or 
   * otherwise).  This can be called often and might need to be
   * cached if it is expensive to create.
   */
  virtual const Hashtable* getSupportedIDs(UErrorCode& status) const
  {
    if (U_SUCCESS(status)) {
      if (!_ids) {
        int32_t count = 0;
        const UnicodeString * const idlist = _delegate->getSupportedIDs(count, status);
        ((NFFactory*)this)->_ids = new Hashtable(status); /* cast away const */
        if (_ids) {
          for (int i = 0; i < count; ++i) {
            _ids->put(idlist[i], (void*)this, status);
          }
        }
      }
      return _ids;
    }
    return NULL;
  }
};

class ICUNumberFormatService : public ICULocaleService {
public:
  ICUNumberFormatService()
    : ICULocaleService("Number Format")
  {
    UErrorCode status = U_ZERO_ERROR;
    registerFactory(new ICUNumberFormatFactory(), status);
  }

  virtual UObject* cloneInstance(UObject* instance) const {
	  return ((NumberFormat*)instance)->clone();
  }

  virtual UObject* handleDefault(const ICUServiceKey& key, UnicodeString* /* actualID */, UErrorCode& status) const {
	LocaleKey& lkey = (LocaleKey&)key;
	int32_t kind = lkey.kind();
	Locale loc;
	lkey.currentLocale(loc);
	return NumberFormat::makeInstance(loc, (NumberFormat::EStyles)kind, status);
  }

  virtual UBool isDefault() const {
	return countFactories() == 1;
  }
};

// -------------------------------------

static ICULocaleService* 
getNumberFormatService(void)
{
    UBool needInit;
    {
        Mutex mutex;
        needInit = (UBool)(gService == NULL);
    }
    if (needInit) {
        ICULocaleService * newservice = new ICUNumberFormatService();
        if (newservice) {
            Mutex mutex;
            if (gService == NULL) {
                gService = newservice;
                newservice = NULL;
            }
        }
        if (newservice) {
            delete newservice;
        } else {
            // we won the contention, this thread can register cleanup.
            ucln_i18n_registerCleanup();
        }
    }
    return gService;
}

// -------------------------------------

NumberFormat*
NumberFormat::createInstance(const Locale& loc, EStyles kind, UErrorCode& status)
{
    umtx_lock(NULL);
    UBool haveService = gService != NULL;
    umtx_unlock(NULL);
    if (haveService) {
        return (NumberFormat*)gService->get(loc, kind, status);
    } else {
        return makeInstance(loc, kind, status);
  }
}


// -------------------------------------

URegistryKey 
NumberFormat::registerFactory(NumberFormatFactory* toAdopt, UErrorCode& status)
{
  ICULocaleService *service = getNumberFormatService();
  if (service) {
    return service->registerFactory(new NFFactory(toAdopt), status);
  }
  status = U_MEMORY_ALLOCATION_ERROR;
  return NULL;
}

// -------------------------------------

UBool 
NumberFormat::unregister(URegistryKey key, UErrorCode& status)
{
    if (U_SUCCESS(status)) {
        umtx_lock(NULL);
        UBool haveService = gService != NULL;
        umtx_unlock(NULL);
        if (haveService) {
            return gService->unregister(key, status);
        }
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return FALSE;
}

// -------------------------------------
StringEnumeration* 
NumberFormat::getAvailableLocales(void)
{
  ICULocaleService *service = getNumberFormatService();
  if (service) {
    return service->getAvailableLocales();
  }
  return NULL; // no way to return error condition
}

// -------------------------------------
// Checks if the thousand/10 thousand grouping is used in the
// NumberFormat instance.

UBool
NumberFormat::isGroupingUsed() const
{
    return fGroupingUsed;
}

// -------------------------------------
// Sets to use the thousand/10 thousand grouping in the
// NumberFormat instance.

void
NumberFormat::setGroupingUsed(UBool newValue)
{
    fGroupingUsed = newValue;
}

// -------------------------------------
// Gets the maximum number of digits for the integral part for
// this NumberFormat instance.

int32_t NumberFormat::getMaximumIntegerDigits() const
{
    return fMaxIntegerDigits;
}

// -------------------------------------
// Sets the maximum number of digits for the integral part for
// this NumberFormat instance.

void
NumberFormat::setMaximumIntegerDigits(int32_t newValue)
{
    fMaxIntegerDigits = uprv_max(0, uprv_min(newValue, fgMaxIntegerDigits));
    if(fMinIntegerDigits > fMaxIntegerDigits)
        fMinIntegerDigits = fMaxIntegerDigits;
}

// -------------------------------------
// Gets the minimum number of digits for the integral part for
// this NumberFormat instance.

int32_t
NumberFormat::getMinimumIntegerDigits() const
{
    return fMinIntegerDigits;
}

// -------------------------------------
// Sets the minimum number of digits for the integral part for
// this NumberFormat instance.

void
NumberFormat::setMinimumIntegerDigits(int32_t newValue)
{
    fMinIntegerDigits = uprv_max(0, uprv_min(newValue, fgMinIntegerDigits));
    if(fMinIntegerDigits > fMaxIntegerDigits)
        fMaxIntegerDigits = fMinIntegerDigits;
}

// -------------------------------------
// Gets the maximum number of digits for the fractional part for
// this NumberFormat instance.

int32_t
NumberFormat::getMaximumFractionDigits() const
{
    return fMaxFractionDigits;
}

// -------------------------------------
// Sets the maximum number of digits for the fractional part for
// this NumberFormat instance.

void
NumberFormat::setMaximumFractionDigits(int32_t newValue)
{
    fMaxFractionDigits = uprv_max(0, uprv_min(newValue, fgMaxIntegerDigits));
    if(fMaxFractionDigits < fMinFractionDigits)
        fMinFractionDigits = fMaxFractionDigits;
}

// -------------------------------------
// Gets the minimum number of digits for the fractional part for
// this NumberFormat instance.

int32_t
NumberFormat::getMinimumFractionDigits() const
{
    return fMinFractionDigits;
}

// -------------------------------------
// Sets the minimum number of digits for the fractional part for
// this NumberFormat instance.

void
NumberFormat::setMinimumFractionDigits(int32_t newValue)
{
    fMinFractionDigits = uprv_max(0, uprv_min(newValue, fgMinIntegerDigits));
    if (fMaxFractionDigits < fMinFractionDigits)
        fMaxFractionDigits = fMinFractionDigits;
}

// -------------------------------------

void NumberFormat::setCurrency(const UChar* theCurrency, UErrorCode& ec) {
    if (U_FAILURE(ec)) {
        return;
    }
    if (theCurrency) {
        u_strncpy(fCurrency, theCurrency, 3);
        fCurrency[3] = 0;
    } else {
        fCurrency[0] = 0;
    }
}

const UChar* NumberFormat::getCurrency() const {
    return fCurrency;
}

void NumberFormat::getEffectiveCurrency(UChar* result, UErrorCode& ec) const {
    const UChar* c = getCurrency();
    if (*c != 0) {
        u_strncpy(result, c, 3);
        result[3] = 0;
    } else {
        const char* loc = getLocaleID(ULOC_VALID_LOCALE, ec);
        if (loc == NULL) {
            loc = uloc_getDefault();
        }
        ucurr_forLocale(loc, result, 4, &ec);
    }
}

// -------------------------------------
// Creates the NumberFormat instance of the specified style (number, currency,
// or percent) for the desired locale.

NumberFormat*
NumberFormat::makeInstance(const Locale& desiredLocale,
                           EStyles style,
                           UErrorCode& status)
{
    if (U_FAILURE(status)) return NULL;

    if (style < 0 || style >= kStyleCount) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    ResourceBundle resource((char *)0, desiredLocale, status);
    NumberFormat* f;

    if (U_FAILURE(status))
    {
        // We don't appear to have resource data available -- use the last-resort data
        status = U_USING_FALLBACK_WARNING;

        // Use the DecimalFormatSymbols constructor which uses last-resort data
        DecimalFormatSymbols* symbolsToAdopt = new DecimalFormatSymbols(status);
        if (symbolsToAdopt == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        if (U_FAILURE(status)) {
            delete symbolsToAdopt; // This should never happen
            return NULL;
        }

        // Creates a DecimalFormat instance with the last resort number patterns.
        f = new DecimalFormat(fgLastResortNumberPatterns[style], symbolsToAdopt, status);
        if (f == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        if (U_FAILURE(status)) {
            delete f;
            f = NULL;
        }
        return f;
    }

    ResourceBundle numberPatterns(resource.get(DecimalFormat::fgNumberPatterns, status));

    // If not all the styled patterns exists for the NumberFormat in this locale,
    // sets the status code to failure and returns nil.
    //if (patternCount < fgNumberPatternsCount) status = U_INVALID_FORMAT_ERROR;
    if (numberPatterns.getSize() < fgNumberPatternsCount)
        status = U_INVALID_FORMAT_ERROR;
    if (U_FAILURE(status))
        return NULL;

    // Loads the decimal symbols of the desired locale.
    DecimalFormatSymbols* symbolsToAdopt = new DecimalFormatSymbols(desiredLocale, status);
    if (symbolsToAdopt == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    if (U_FAILURE(status)) {
        delete symbolsToAdopt;
        return NULL;
    }

    // Creates the specified decimal format style of the desired locale.
    if (style < numberPatterns.getSize()) {
        UnicodeString pattern(numberPatterns.getStringEx(style, status));
        if (U_SUCCESS(status)) {
            // Here we assume that the locale passed in is in the canonical
            // form, e.g: pt_PT_@currency=PTE not pt_PT_PREEURO
            if(style==kCurrencyStyle){
                char buf[20]={0};
                int32_t bufCap = 20;
                const char* locName = desiredLocale.getName();
                bufCap = uloc_getKeywordValue(locName, "currency", buf, bufCap, &status);
                if(bufCap > 0){
                    ResourceBundle currencies(resource.getWithFallback("Currencies", status));
                    ResourceBundle currency(currencies.getWithFallback(buf,status));
                    if(currency.getSize()>2){
                        ResourceBundle elements = currency.get(2, status);
                        UnicodeString currPattern = elements.getStringEx((int32_t)0, status);
                        UnicodeString decimalSep = elements.getStringEx((int32_t)1, status);
                        UnicodeString groupingSep = elements.getStringEx((int32_t)2, status);
                        if(U_SUCCESS(status)){
                            symbolsToAdopt->setSymbol(DecimalFormatSymbols::kGroupingSeparatorSymbol, groupingSep);
                            symbolsToAdopt->setSymbol(DecimalFormatSymbols::kMonetarySeparatorSymbol, decimalSep);
                            pattern = currPattern;
                        }
                    }
                }
            }
            if (U_FAILURE(status)) {
                delete symbolsToAdopt;
                return NULL;
            }
            f = new DecimalFormat(pattern, symbolsToAdopt, status);
        }
        else {
            return NULL;
        }
    }
    else {
        // If the requested style doesn't exist, use a last-resort style.
        // This is to support scientific styles before we have all the
        // resource data in place.
        f = new DecimalFormat(fgLastResortNumberPatterns[style], symbolsToAdopt, status);
    }
    
    if (f == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    if (U_FAILURE(status)) {
        delete f;
        f = NULL;
        return NULL;
    }
    f->setLocales(numberPatterns);
    return f;
}

U_NAMESPACE_END

// defined in ucln_cmn.h

/**
 * Release all static memory held by numberformat.  
 */
U_CFUNC UBool numfmt_cleanup(void) {
  if (gService) {
    delete gService;
    gService = NULL;
  }
  return TRUE;
}

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
