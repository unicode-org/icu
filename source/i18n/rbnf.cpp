/*
*******************************************************************************
* Copyright (C) 1997-2004, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
*/

#include "unicode/rbnf.h"

#if U_HAVE_RBNF

#include "unicode/normlzr.h"
#include "unicode/tblcoll.h"
#include "unicode/uchar.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "unicode/unum.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"
#include "unicode/utf16.h"

#include "nfrs.h"

#include "cmemory.h"
#include "cstring.h"
#include "uprops.h"

static const UChar gPercentPercent[] =
{
    0x25, 0x25, 0
}; /* "%%" */

// All urbnf objects are created through openRules, so we init all of the
// Unicode string constants required by rbnf, nfrs, or nfr here.
static const UChar gLenientParse[] =
{
    0x25, 0x25, 0x6C, 0x65, 0x6E, 0x69, 0x65, 0x6E, 0x74, 0x2D, 0x70, 0x61, 0x72, 0x73, 0x65, 0x3A, 0
}; /* "%%lenient-parse:" */
static const UChar gSemiColon = 0x003B;
static const UChar gSemiPercent[] =
{
    0x3B, 0x25, 0
}; /* ";%" */

#define kSomeNumberOfBitsDiv2 22
#define kHalfMaxDouble (double)(1 << kSomeNumberOfBitsDiv2)
#define kMaxDouble (kHalfMaxDouble * kHalfMaxDouble)

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(RuleBasedNumberFormat)

RuleBasedNumberFormat::RuleBasedNumberFormat(const UnicodeString& description, const Locale& alocale, UParseError& perror, UErrorCode& status)
  : ruleSets(NULL)
  , defaultRuleSet(NULL)
  , locale(alocale)
  , collator(NULL)
  , decimalFormatSymbols(NULL)
  , lenient(FALSE)
  , lenientParseRules(NULL)
{
    init(description, perror, status);
}

RuleBasedNumberFormat::RuleBasedNumberFormat(URBNFRuleSetTag tag, const Locale& alocale, UErrorCode& status)
  : ruleSets(NULL)
  , defaultRuleSet(NULL)
  , locale(alocale)
  , collator(NULL)
  , decimalFormatSymbols(NULL)
  , lenient(FALSE)
  , lenientParseRules(NULL)
{
    if (U_FAILURE(status)) {
        return;
    }

    const char* fmt_tag = "";
    switch (tag) {
    case URBNF_SPELLOUT: fmt_tag = "SpelloutRules"; break;
    case URBNF_ORDINAL: fmt_tag = "OrdinalRules"; break;
    case URBNF_DURATION: fmt_tag = "DurationRules"; break;
    default: status = U_ILLEGAL_ARGUMENT_ERROR; return;
    }

    // the following didn't work for aliased resources, but Vladimir supposedly fixed it...
    // const UChar* description = ures_getStringByKey(nfrb, fmt_tag, &len, &status);
    int32_t len = 0;
    UResourceBundle* nfrb = ures_open(NULL, locale.getName(), &status);
    //    UResourceBundle* yuck = ures_getByKey(nfrb, fmt_tag, NULL, &status);
    //    const UChar* description = ures_getString(yuck, &len, &status);
    if (U_SUCCESS(status)) {
        setLocaleIDs(ures_getLocaleByType(nfrb, ULOC_VALID_LOCALE, &status),
                     ures_getLocaleByType(nfrb, ULOC_ACTUAL_LOCALE, &status));
        const UChar* description = ures_getStringByKey(nfrb, fmt_tag, &len, &status);
        UnicodeString desc(description, len);
        UParseError perror;
        init (desc, perror, status);
    }
    //    ures_close(yuck);
    ures_close(nfrb);
}

RuleBasedNumberFormat::RuleBasedNumberFormat(const RuleBasedNumberFormat& rhs)
  : NumberFormat(rhs)
  , ruleSets(NULL)
  , defaultRuleSet(NULL)
  , locale(rhs.locale)
  , collator(NULL)
  , decimalFormatSymbols(NULL)
  , lenient(FALSE)
  , lenientParseRules(NULL)
{
    this->operator=(rhs);
}

RuleBasedNumberFormat&
RuleBasedNumberFormat::operator=(const RuleBasedNumberFormat& rhs)
{
    UErrorCode status = U_ZERO_ERROR;
    dispose();
    locale = rhs.locale;
    UnicodeString rules = rhs.getRules();
    UParseError perror;
    init(rules, perror, status);
    lenient = rhs.lenient;
    return *this;
}

RuleBasedNumberFormat::~RuleBasedNumberFormat()
{
    dispose();
}

Format*
RuleBasedNumberFormat::clone(void) const
{
    RuleBasedNumberFormat * result = NULL;
    UnicodeString rules = getRules();
    UErrorCode status = U_ZERO_ERROR;
    UParseError perror;
    result = new RuleBasedNumberFormat(rules, locale, perror, status);
    /* test for NULL */
    if (result == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }
    if (U_FAILURE(status)) {
        delete result;
        result = 0;
    } else {
        result->lenient = lenient;
    }
    return result;
}

UBool
RuleBasedNumberFormat::operator==(const Format& other) const
{
    if (this == &other) {
        return TRUE;
    }

    if (other.getDynamicClassID() == getStaticClassID()) {
        const RuleBasedNumberFormat& rhs = (const RuleBasedNumberFormat&)other;
        if (locale == rhs.locale &&
            lenient == rhs.lenient) {
            NFRuleSet** p = ruleSets;
            NFRuleSet** q = rhs.ruleSets;
            if ((p == NULL) != (q == NULL)) {
                return TRUE;
            }
            if (p == NULL) {
                return FALSE;
            }
            while (*p && *q && (**p == **q)) {
                ++p;
                ++q;
            }
            return *q == NULL && *p == NULL;
        }
    }

    return FALSE;
}

UnicodeString
RuleBasedNumberFormat::getRules() const
{
    UnicodeString result;
    if (ruleSets != NULL) {
        for (NFRuleSet** p = ruleSets; *p; ++p) {
            (*p)->appendRules(result);
        }
    }
    return result;
}

UnicodeString
RuleBasedNumberFormat::getRuleSetName(int32_t index) const
{
    UnicodeString result;
    if (ruleSets) {
        for (NFRuleSet** p = ruleSets; *p; ++p) {
            NFRuleSet* rs = *p;
            if (rs->isPublic()) {
                if (--index == -1) {
                    rs->getName(result);
                    return result;
                }
            }
        }
    }
    return result;
}

int32_t
RuleBasedNumberFormat::getNumberOfRuleSetNames() const
{
    int32_t result = 0;
    if (ruleSets) {
        for (NFRuleSet** p = ruleSets; *p; ++p) {
            if ((**p).isPublic()) {
                ++result;
            }
        }
    }
    return result;
}

NFRuleSet*
RuleBasedNumberFormat::findRuleSet(const UnicodeString& name, UErrorCode& status) const
{
    if (U_SUCCESS(status) && ruleSets) {
        for (NFRuleSet** p = ruleSets; *p; ++p) {
            NFRuleSet* rs = *p;
            if (rs->isNamed(name)) {
                return rs;
            }
        }
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return NULL;
}

UnicodeString&
RuleBasedNumberFormat::format(int32_t number,
                              UnicodeString& toAppendTo,
                              FieldPosition& /* pos */) const
{
    if (defaultRuleSet) defaultRuleSet->format((int64_t)number, toAppendTo, toAppendTo.length());
    return toAppendTo;
}


UnicodeString&
RuleBasedNumberFormat::format(int64_t number,
                              UnicodeString& toAppendTo,
                              FieldPosition& /* pos */) const
{
    if (defaultRuleSet) defaultRuleSet->format(number, toAppendTo, toAppendTo.length());
    return toAppendTo;
}


UnicodeString&
RuleBasedNumberFormat::format(double number,
                              UnicodeString& toAppendTo,
                              FieldPosition& /* pos */) const
{
    if (defaultRuleSet) defaultRuleSet->format(number, toAppendTo, toAppendTo.length());
    return toAppendTo;
}


UnicodeString&
RuleBasedNumberFormat::format(int32_t number,
                              const UnicodeString& ruleSetName,
                              UnicodeString& toAppendTo,
                              FieldPosition& /* pos */,
                              UErrorCode& status) const
{
    // return format((int64_t)number, ruleSetName, toAppendTo, pos, status);
    if (U_SUCCESS(status)) {
        if (ruleSetName.indexOf(gPercentPercent) == 0) {
            // throw new IllegalArgumentException("Can't use internal rule set");
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            NFRuleSet *rs = findRuleSet(ruleSetName, status);
            if (rs) {
                rs->format((int64_t)number, toAppendTo, toAppendTo.length());
            }
        }
    }
    return toAppendTo;
}


UnicodeString&
RuleBasedNumberFormat::format(int64_t number,
                              const UnicodeString& ruleSetName,
                              UnicodeString& toAppendTo,
                              FieldPosition& /* pos */,
                              UErrorCode& status) const
{
    if (U_SUCCESS(status)) {
        if (ruleSetName.indexOf(gPercentPercent) == 0) {
            // throw new IllegalArgumentException("Can't use internal rule set");
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            NFRuleSet *rs = findRuleSet(ruleSetName, status);
            if (rs) {
                rs->format(number, toAppendTo, toAppendTo.length());
            }
        }
    }
    return toAppendTo;
}


// make linker happy
UnicodeString&
RuleBasedNumberFormat::format(const Formattable& obj,
                              UnicodeString& toAppendTo,
                              FieldPosition& pos,
                              UErrorCode& status) const
{
    return NumberFormat::format(obj, toAppendTo, pos, status);
}

UnicodeString&
RuleBasedNumberFormat::format(double number,
                              const UnicodeString& ruleSetName,
                              UnicodeString& toAppendTo,
                              FieldPosition& /* pos */,
                              UErrorCode& status) const
{
    if (U_SUCCESS(status)) {
        if (ruleSetName.indexOf(gPercentPercent) == 0) {
            // throw new IllegalArgumentException("Can't use internal rule set");
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            NFRuleSet *rs = findRuleSet(ruleSetName, status);
            if (rs) {
                rs->format(number, toAppendTo, toAppendTo.length());
            }
        }
    }
    return toAppendTo;
}

void
RuleBasedNumberFormat::parse(const UnicodeString& text,
                             Formattable& result,
                             ParsePosition& parsePosition) const
{
    if (!ruleSets) {
        parsePosition.setErrorIndex(0);
        return;
    }

    ParsePosition high_pp;
    Formattable high_result;

    for (NFRuleSet** p = ruleSets; *p; ++p) {
        NFRuleSet *rp = *p;
        if (rp->isPublic()) {
            ParsePosition working_pp = parsePosition;
            Formattable working_result;

            rp->parse(text, working_pp, kMaxDouble, working_result);
            if (working_pp.getIndex() > high_pp.getIndex()) {
                high_pp = working_pp;
                high_result = working_result;

                if (high_pp.getIndex() == text.length()) {
                    break;
                }
            }
        }
    }

    if (high_pp.getIndex() > parsePosition.getIndex()) {
        high_pp.setErrorIndex(-1);
    }
    parsePosition = high_pp;
    result = high_result;
    if (result.getType() == Formattable::kDouble) {
        int32_t r = (int32_t)result.getDouble();
        if ((double)r == result.getDouble()) {
            result.setLong(r);
        }
    }
}

#if !UCONFIG_NO_COLLATION

void
RuleBasedNumberFormat::setLenient(UBool enabled)
{
    lenient = enabled;
    if (!enabled && collator) {
        delete collator;
        collator = NULL;
    }
}

#endif

void 
RuleBasedNumberFormat::setDefaultRuleSet(const UnicodeString& ruleSetName, UErrorCode& status) {
    if (U_SUCCESS(status)) {
        if (ruleSetName.isEmpty()) {
            initDefaultRuleSet();
        } else if (ruleSetName.startsWith("%%")) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            NFRuleSet* result = findRuleSet(ruleSetName, status);
            if (result != NULL) {
                defaultRuleSet = result;
            }
        }
    }
}

UnicodeString
RuleBasedNumberFormat::getDefaultRuleSetName() const {
  UnicodeString result;
  if (defaultRuleSet && defaultRuleSet->isPublic()) {
    defaultRuleSet->getName(result);
  } else {
    result.setToBogus();
  }
  return result;
}

void 
RuleBasedNumberFormat::initDefaultRuleSet()
{
    if (!ruleSets) {
        defaultRuleSet = NULL;
    }
    NFRuleSet**p = &ruleSets[1];
    while (*p) {
        ++p;
    }

    defaultRuleSet = *--p;
    if (!defaultRuleSet->isPublic()) {
        while (p != ruleSets) {
            if ((*--p)->isPublic()) {
                defaultRuleSet = *p;
                break;
            }
        }
    }
}


void
RuleBasedNumberFormat::init(const UnicodeString& rules, UParseError& /* pErr */, UErrorCode& status)
{
    // TODO: implement UParseError
    // Note: this can leave ruleSets == NULL, so remaining code should check
    if (U_FAILURE(status)) {
        return;
    }

    UnicodeString description(rules);
    if (!description.length()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    // start by stripping the trailing whitespace from all the rules
    // (this is all the whitespace follwing each semicolon in the
    // description).  This allows us to look for rule-set boundaries
    // by searching for ";%" without having to worry about whitespace
    // between the ; and the %
    stripWhitespace(description);

    // check to see if there's a set of lenient-parse rules.  If there
    // is, pull them out into our temporary holding place for them,
    // and delete them from the description before the real desciption-
    // parsing code sees them
    int32_t lp = description.indexOf(gLenientParse);
    if (lp != -1) {
        // we've got to make sure we're not in the middle of a rule
        // (where "%%lenient-parse" would actually get treated as
        // rule text)
        if (lp == 0 || description.charAt(lp - 1) == gSemiColon) {
            // locate the beginning and end of the actual collation
            // rules (there may be whitespace between the name and
            // the first token in the description)
            int lpEnd = description.indexOf(gSemiPercent, lp);

            if (lpEnd == -1) {
                lpEnd = description.length() - 1;
            }
            int lpStart = lp + u_strlen(gLenientParse);
            while (uprv_isRuleWhiteSpace(description.charAt(lpStart))) {
                ++lpStart;
            }

            // copy out the lenient-parse rules and delete them
            // from the description
            lenientParseRules = new UnicodeString();
            /* test for NULL */
            if (lenientParseRules == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            lenientParseRules->setTo(description, lpStart, lpEnd - lpStart);

            description.remove(lp, lpEnd + 1 - lp);
        }
    }

    // pre-flight parsing the description and count the number of
    // rule sets (";%" marks the end of one rule set and the beginning
    // of the next)
    int numRuleSets = 0;
    for (int32_t p = description.indexOf(gSemiPercent); p != -1; p = description.indexOf(gSemiPercent, p)) {
        ++numRuleSets;
        ++p;
    }
    ++numRuleSets;

    // our rule list is an array of the appropriate size
    ruleSets = (NFRuleSet **)uprv_malloc((numRuleSets + 1) * sizeof(NFRuleSet *));
    /* test for NULL */
    if (ruleSets == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    for (int i = 0; i <= numRuleSets; ++i) {
        ruleSets[i] = NULL;
    }

    // divide up the descriptions into individual rule-set descriptions
    // and store them in a temporary array.  At each step, we also
    // new up a rule set, but all this does is initialize its name
    // and remove it from its description.  We can't actually parse
    // the rest of the descriptions and finish initializing everything
    // because we have to know the names and locations of all the rule
    // sets before we can actually set everything up
    if(!numRuleSets) {
      status = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
    UnicodeString* ruleSetDescriptions = new UnicodeString[numRuleSets];
    /* test for NULL */
    if (ruleSetDescriptions == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    {
        int curRuleSet = 0;
        int32_t start = 0;
        for (int32_t p = description.indexOf(gSemiPercent); p != -1; p = description.indexOf(gSemiPercent, start)) {
            ruleSetDescriptions[curRuleSet].setTo(description, start, p + 1 - start);
            ruleSets[curRuleSet] = new NFRuleSet(ruleSetDescriptions, curRuleSet, status);
            /* test for NULL */
            if (ruleSets[curRuleSet] == 0) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            ++curRuleSet;
            start = p + 1;
        }
        ruleSetDescriptions[curRuleSet].setTo(description, start, description.length() - start);
        ruleSets[curRuleSet] = new NFRuleSet(ruleSetDescriptions, curRuleSet, status);
        /* test for NULL */
        if (ruleSets[curRuleSet] == 0) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
    }

    // now we can take note of the formatter's default rule set, which
    // is the last public rule set in the description (it's the last
    // rather than the first so that a user can create a new formatter
    // from an existing formatter and change its default behavior just
    // by appending more rule sets to the end)
	initDefaultRuleSet();

    // finally, we can go back through the temporary descriptions
    // list and finish seting up the substructure (and we throw
    // away the temporary descriptions as we go)
    {
        for (int i = 0; i < numRuleSets; i++) {
            ruleSets[i]->parseRules(ruleSetDescriptions[i], this, status);
        }
    }

    delete[] ruleSetDescriptions;
}

void
RuleBasedNumberFormat::stripWhitespace(UnicodeString& description)
{
    // iterate through the characters...
    UnicodeString result;

    int start = 0;
    while (start != -1 && start < description.length()) {
        // seek to the first non-whitespace character...
        while (start < description.length()
            && uprv_isRuleWhiteSpace(description.charAt(start))) {
            ++start;
        }

        // locate the next semicolon in the text and copy the text from
        // our current position up to that semicolon into the result
        int32_t p = description.indexOf(gSemiColon, start);
        if (p == -1) {
            // or if we don't find a semicolon, just copy the rest of
            // the string into the result
            result.append(description, start, description.length() - start);
            start = -1;
        }
        else if (p < description.length()) {
            result.append(description, start, p + 1 - start);
            start = p + 1;
        }

        // when we get here, we've seeked off the end of the sring, and
        // we terminate the loop (we continue until *start* is -1 rather
        // than until *p* is -1, because otherwise we'd miss the last
        // rule in the description)
        else {
            start = -1;
        }
    }

    description.setTo(result);
}


void
RuleBasedNumberFormat::dispose()
{
    if (ruleSets) {
        for (NFRuleSet** p = ruleSets; *p; ++p) {
            delete *p;
        }
        uprv_free(ruleSets);
        ruleSets = NULL;
    }

#if !UCONFIG_NO_COLLATION
    delete collator;
#endif
    collator = NULL;

    delete decimalFormatSymbols;
    decimalFormatSymbols = NULL;

    delete lenientParseRules;
    lenientParseRules = NULL;
}


//-----------------------------------------------------------------------
// package-internal API
//-----------------------------------------------------------------------

/**
 * Returns the collator to use for lenient parsing.  The collator is lazily created:
 * this function creates it the first time it's called.
 * @return The collator to use for lenient parsing, or null if lenient parsing
 * is turned off.
*/
Collator*
RuleBasedNumberFormat::getCollator() const
{
#if !UCONFIG_NO_COLLATION
    if (!ruleSets) {
        return NULL;
    }

    // lazy-evaulate the collator
    if (collator == NULL && lenient) {
        // create a default collator based on the formatter's locale,
        // then pull out that collator's rules, append any additional
        // rules specified in the description, and create a _new_
        // collator based on the combinaiton of those rules

        UErrorCode status = U_ZERO_ERROR;

        Collator* temp = Collator::createInstance(locale, status);
        if (U_SUCCESS(status) &&
            temp->getDynamicClassID() == RuleBasedCollator::getStaticClassID()) {

            RuleBasedCollator* newCollator = (RuleBasedCollator*)temp;
            if (lenientParseRules) {
                UnicodeString rules(newCollator->getRules());
                rules.append(*lenientParseRules);

                newCollator = new RuleBasedCollator(rules, status);
            } else {
                temp = NULL;
            }
            if (U_SUCCESS(status)) {
                newCollator->setAttribute(UCOL_DECOMPOSITION_MODE, UCOL_ON, status);
                // cast away const
                ((RuleBasedNumberFormat*)this)->collator = newCollator;
            } else {
                delete newCollator;
            }
        }
        delete temp;
    }
#endif

    // if lenient-parse mode is off, this will be null
    // (see setLenientParseMode())
    return collator;
}


/**
 * Returns the DecimalFormatSymbols object that should be used by all DecimalFormat
 * instances owned by this formatter.  This object is lazily created: this function
 * creates it the first time it's called.
 * @return The DecimalFormatSymbols object that should be used by all DecimalFormat
 * instances owned by this formatter.
*/
DecimalFormatSymbols*
RuleBasedNumberFormat::getDecimalFormatSymbols() const
{
    // lazy-evaluate the DecimalFormatSymbols object.  This object
    // is shared by all DecimalFormat instances belonging to this
    // formatter
    if (decimalFormatSymbols == NULL) {
        UErrorCode status = U_ZERO_ERROR;
        DecimalFormatSymbols* temp = new DecimalFormatSymbols(locale, status);
        if (U_SUCCESS(status)) {
            ((RuleBasedNumberFormat*)this)->decimalFormatSymbols = temp;
        } else {
            delete temp;
        }
    }
    return decimalFormatSymbols;
}

U_NAMESPACE_END

/* U_HAVE_RBNF */
#endif
