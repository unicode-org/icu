/*
******************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   file name:  nfrs.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* Modification history
* Date        Name      Comments
* 10/11/2001  Doug      Ported from ICU4J
*/

#ifndef NFRS_H
#define NFRS_H

#include "unicode/utypes.h"
#include "unicode/umisc.h"

#include "unicode/rbnf.h"
#include "nfrlist.h"
#include "llong.h"

U_NAMESPACE_BEGIN

class NFRuleSet {
 public:
  NFRuleSet(UnicodeString* descriptions, int32_t index, UErrorCode& status);
  void parseRules(UnicodeString& rules, const RuleBasedNumberFormat* owner, UErrorCode& status);
  void makeIntoFractionRuleSet() { fIsFractionRuleSet = TRUE; }

  ~NFRuleSet();

  UBool operator==(const NFRuleSet& rhs) const;
  UBool operator!=(const NFRuleSet& rhs) const { return !operator==(rhs); }

  UBool isPublic() const { return fIsPublic; }
  UBool isFractionRuleSet() const { return fIsFractionRuleSet; }

  void  getName(UnicodeString& result) const { result.setTo(name); }
  UBool isNamed(const UnicodeString& _name) const { return this->name == _name; }

  void  format(llong number, UnicodeString& toAppendTo, int32_t pos) const;
  void  format(double number, UnicodeString& toAppendTo, int32_t pos) const;

  UBool parse(const UnicodeString& text, ParsePosition& pos, double upperBound, Formattable& result) const;

  void appendRules(UnicodeString& result) const; // toString

 private:
  NFRule * findNormalRule(llong number) const;
  NFRule * findDoubleRule(double number) const;
  NFRule * findFractionRuleSetRule(double number) const;

 private:
  UnicodeString name;
  NFRuleList rules;
  NFRule *negativeNumberRule;
  NFRule *fractionRules[3];
  UBool fIsFractionRuleSet;
  UBool fIsPublic;
};

U_NAMESPACE_END

// NFRS_H
#endif

