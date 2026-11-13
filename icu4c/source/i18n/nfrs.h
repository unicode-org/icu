// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
******************************************************************************
*   Copyright (C) 1997-2015, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   file name:  nfrs.h
*   encoding:   UTF-8
*   tab size:   8 (not used)
*   indentation:4
*
* Modification history
* Date        Name      Comments
* 10/11/2001  Doug      Ported from ICU4J
*/

#ifndef NFRS_H
#define NFRS_H

#include "unicode/uobject.h"
#include "unicode/rbnf.h"

#if U_HAVE_RBNF

#include "unicode/utypes.h"
#include "unicode/umisc.h"

#include "nfrlist.h"

U_NAMESPACE_BEGIN

class NFRuleSet : public UMemory {
public:
    NFRuleSet(RuleBasedNumberFormat *owner, UnicodeString* descriptions, int32_t index, UErrorCode& status);
    void parseRules(UnicodeString& rules, UErrorCode& status);
    void setNonNumericalRule(NFRule *rule);
    void setBestFractionRule(int32_t originalIndex, NFRule *newRule, UBool rememberRule);
    void makeIntoFractionRuleSet() { fIsFractionRuleSet = true; }

    ~NFRuleSet();

    bool operator==(const NFRuleSet& rhs) const;
    bool operator!=(const NFRuleSet& rhs) const { return !operator==(rhs); }

    UBool isPublic() const { return fIsPublic; }

    UBool isParseable() const { return fIsParseable; }

    UBool isFractionRuleSet() const { return fIsFractionRuleSet; }

    void  getName(UnicodeString& result) const { result.setTo(name); }
    UBool isNamed(const UnicodeString& _name) const { return this->name == _name; }

    void  format(int64_t number, UnicodeString& toAppendTo, int32_t pos, int32_t recursionCount, UErrorCode& status) const;
    void  format(double number, UnicodeString& toAppendTo, int32_t pos, int32_t recursionCount, UErrorCode& status) const;

    UBool parse(const UnicodeString& text, ParsePosition& pos, double upperBound, uint32_t nonNumericalExecutedRuleMask, int32_t recursionCount, Formattable& result) const;

    void appendRules(UnicodeString& result) const; // toString

    void setDecimalFormatSymbols(const DecimalFormatSymbols &newSymbols, UErrorCode& status);

    const RuleBasedNumberFormat *getOwner() const { return owner; }

    static constexpr int32_t kNonNumericalRuleCount = 6;
    static constexpr int64_t kMaxExactDoubleInt = int64_t{1} << 53;

    int32_t getRuleCount() const { return static_cast<int32_t>(rules.size()); }
    const NFRule* getRule(int32_t index) const { return rules[static_cast<uint32_t>(index)]; }
    const NFRule* getNonNumericalRule(int32_t index) const { return nonNumericalRules[index]; }
    int32_t getFractionRuleCount() const { return static_cast<int32_t>(fractionRules.size()); }
    const NFRule* getFractionRule(int32_t index) const { return fractionRules[static_cast<uint32_t>(index)]; }
    int64_t getRuleUpperBound(int32_t index) const;
private:
    const NFRule * findNormalRule(int64_t number) const;
    const NFRule * findDoubleRule(double number) const;
    const NFRule * findFractionRuleSetRule(double number) const;
    
    friend class NFSubstitution;
    friend class NFRule;

private:
    UnicodeString name;
    NFRuleList rules;
    NFRule *nonNumericalRules[6] {  };
    RuleBasedNumberFormat *owner {  };
    NFRuleList fractionRules;
    UBool fIsFractionRuleSet {  };
    UBool fIsPublic {  };
    UBool fIsParseable { true };

    NFRuleSet(const NFRuleSet &other); // forbid copying of this class
    NFRuleSet &operator=(const NFRuleSet &other); // forbid copying of this class
};

// utilities from old llong.h
// convert mantissa portion of double to int64
int64_t util64_fromDouble(double d);

// raise radix to the power exponent, only non-negative exponents
// Arithmetic is performed in unsigned space since overflow in
// signed space is undefined behavior.
uint64_t util64_pow(uint32_t radix, uint16_t exponent);

// convert n to digit string in buffer, return length of string
uint32_t util64_tou(int64_t n, char16_t* buffer, uint32_t buflen, uint32_t radix = 10, UBool raw = false);

#ifdef RBNF_DEBUG
int64_t util64_utoi(const char16_t* str, uint32_t radix = 10);
uint32_t util64_toa(int64_t n, char* buffer, uint32_t buflen, uint32_t radix = 10, UBool raw = false);
int64_t util64_atoi(const char* str, uint32_t radix);
#endif


U_NAMESPACE_END

/* U_HAVE_RBNF */
#endif

// NFRS_H
#endif
