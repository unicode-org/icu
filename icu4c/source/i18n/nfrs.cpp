/*
*******************************************************************************
* Copyright (C) 1997-2001, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
*/

#include <math.h>

#include "nfrs.h"
#include "nfrule.h"
#include "nfrlist.h"
#include "cmemory.h"

U_NAMESPACE_BEGIN

#if 0
// euclid's algorithm works with doubles
// note, doubles only get us up to one quadrillion or so, which
// isn't as much range as we get with longs.  We probably still
// want either 64-bit math, or BigInteger.

static llong
util_lcm(llong x, llong y)
{
  x.abs();
  y.abs();

  if (x == 0 || y == 0) {
    return 0;
  } else {
    do {
      if (x < y) {
        llong t = x; x = y; y = t;
      }
      x -= y * (x/y);
    } while (x != 0);
		
    return y;
  }
}

#else
/**
 * Calculates the least common multiple of x and y.
 */
static llong 
util_lcm(llong x, llong y) 
{
  // binary gcd algorithm from Knuth, "The Art of Computer Programming,"
  // vol. 2, 1st ed., pp. 298-299
  llong x1 = x;
  llong y1 = y;

  int p2 = 0;
  while ((x1 & 1) == 0 && (y1 & 1) == 0) {
    ++p2;
    x1 >>= 1;
    y1 >>= 1;
  }

  llong t;
  if ((x1 & 1) == 1) {
    t = -y1;
  } else {
    t = x1;
  }

  while (t != 0) {
    while ((t & 1) == 0) {
      t >>= 1;
    }
    if (t > 0) {
      x1 = t;
    } else {
      y1 = -t;
    }
    t = x1 - y1;
  }

  llong gcd = x1 << p2;

  // x * y == gcd(x, y) * lcm(x, y)
  return x / gcd * y;
}
#endif

static const UChar gPercent = 0x0025;
static const UChar gColon = 0x003a;
static const UChar gSemicolon = 0x003b;
static const UChar gLineFeed = 0x0010;

static const UnicodeString gFourSpaces("    ");
static const UnicodeString gPercentPercent("%%");

NFRuleSet::NFRuleSet(UnicodeString* descriptions, int32_t index, UErrorCode& status)
  : name()
  , rules(0)
  , negativeNumberRule(NULL)
  , fIsFractionRuleSet(FALSE)
  , fIsPublic(FALSE)
{
  for (int i = 0; i < 3; ++i) {
    fractionRules[i] = NULL;
  }
	
  if (U_FAILURE(status)) {
    return;
  }
	
  UnicodeString& description = descriptions[index]; // !!! make sure index is valid
	
  // if the description begins with a rule set name (the rule set
  // name can be omitted in formatter descriptions that consist
  // of only one rule set), copy it out into our "name" member
  // and delete it from the description
  if (description.charAt(0) == gPercent) {
    UTextOffset pos = description.indexOf(gColon);
    if (pos == -1) {
      // throw new IllegalArgumentException("Rule set name doesn't end in colon");
      status = U_PARSE_ERROR;
    } else {
      name.setTo(description, 0, pos);
      while (pos < description.length() && u_isWhitespace(description.charAt(++pos))) {
      }
      description.remove(0, pos);
    }
  } else {
    name.setTo("%default");
  }
	
  if (description.length() == 0) {
    // throw new IllegalArgumentException("Empty rule set description");
    status = U_PARSE_ERROR;
  }
	
  fIsPublic = name.indexOf(gPercentPercent) != 0;

  // all of the other members of NFRuleSet are initialized
  // by parseRules()
}

void 
NFRuleSet::parseRules(UnicodeString& description, const RuleBasedNumberFormat* owner, UErrorCode& status) 
{
  // start by creating a Vector whose elements are Strings containing
  // the descriptions of the rules (one rule per element).  The rules
  // are separated by semicolons (there's no escape facility: ALL
  // semicolons are rule delimiters)

  if (U_FAILURE(status)) {
    return;
  }

  // dlf - the original code kept a separate description array for no reason,
  // so I got rid of it.  The loop was too complex so I simplified it.

  UnicodeString currentDescription;
  UTextOffset oldP = 0;
  while (oldP < description.length()) {
    UTextOffset p = description.indexOf(gSemicolon, oldP);
    if (p == -1) {
      p = description.length();
    }
    currentDescription.setTo(description, oldP, p - oldP);
    NFRule::makeRules(currentDescription, this, rules.last(), owner, rules, status);
    oldP = p + 1;
  }
	
  // for rules that didn't specify a base value, their base values
  // were initialized to 0.  Make another pass through the list and
  // set all those rules' base values.  We also remove any special
  // rules from the list and put them into their own member variables
  llong defaultBaseValue = (int32_t)0;
	
  // (this isn't a for loop because we might be deleting items from
  // the vector-- we want to make sure we only increment i when
  // we _didn't_ delete aything from the vector)
  uint32_t i = 0;
  while (i < rules.size()) {
    NFRule* rule = rules[i];
		
    switch (rule->getType()) {
      // if the rule's base value is 0, fill in a default
      // base value (this will be 1 plus the preceding
      // rule's base value for regular rule sets, and the
      // same as the preceding rule's base value in fraction
      // rule sets)
    case NFRule::kNoBase:
      rule->setBaseValue(defaultBaseValue);
      if (!isFractionRuleSet()) {
        ++defaultBaseValue;
      }
      ++i;
      break;
			
      // if it's the negative-number rule, copy it into its own
      // data member and delete it from the list
    case NFRule::kNegativeNumberRule:
      negativeNumberRule = rules.remove(i);
      break;
			
      // if it's the improper fraction rule, copy it into the
      // correct element of fractionRules
    case NFRule::kImproperFractionRule:
      fractionRules[0] = rules.remove(i);
      break;
			
      // if it's the proper fraction rule, copy it into the
      // correct element of fractionRules
    case NFRule::kProperFractionRule:
      fractionRules[1] = rules.remove(i);
      break;
			
      // if it's the master rule, copy it into the
      // correct element of fractionRules
    case NFRule::kMasterRule:
      fractionRules[2] = rules.remove(i);
      break;
			
      // if it's a regular rule that already knows its base value,
      // check to make sure the rules are in order, and update
      // the default base value for the next rule
    default:
      if (rule->getBaseValue() < defaultBaseValue) {
				// throw new IllegalArgumentException("Rules are not in order");
        status = U_PARSE_ERROR;
        return;
      }
      defaultBaseValue = rule->getBaseValue();
      if (!isFractionRuleSet()) {
        ++defaultBaseValue;
      }
      ++i;
      break;
    }
  }
}

NFRuleSet::~NFRuleSet()
{
  delete negativeNumberRule;
  delete fractionRules[0];
  delete fractionRules[1];
  delete fractionRules[2];
}

UBool 
util_equalRules(const NFRule* rule1, const NFRule* rule2) 
{
  if (rule1) {
    if (rule2) {
      return *rule1 == *rule2;
    } 
  } else if (!rule2) {
    return TRUE;
  }
  return FALSE;
}

UBool 
NFRuleSet::operator==(const NFRuleSet& rhs) const
{
  if (rules.size() == rhs.rules.size() &&
      fIsFractionRuleSet == rhs.fIsFractionRuleSet &&
      name == rhs.name &&
      util_equalRules(negativeNumberRule, rhs.negativeNumberRule) &&
      util_equalRules(fractionRules[0], rhs.fractionRules[0]) &&
      util_equalRules(fractionRules[1], rhs.fractionRules[1]) &&
      util_equalRules(fractionRules[2], rhs.fractionRules[2])) {

    for (uint32_t i = 0; i < rules.size(); ++i) {
      if (*rules[i] != *rhs.rules[i]) {
        return FALSE;
      }
    }
    return TRUE;
  }
  return FALSE;
}

void  
NFRuleSet::format(llong number, UnicodeString& toAppendTo, int32_t pos) const
{
  NFRule *rule = findNormalRule(number);
  rule->doFormat(number, toAppendTo, pos);
}

void  
NFRuleSet::format(double number, UnicodeString& toAppendTo, int32_t pos) const
{
  NFRule *rule = findDoubleRule(number);
  rule->doFormat(number, toAppendTo, pos);
}

NFRule* 
NFRuleSet::findDoubleRule(double number) const
{
  // if this is a fraction rule set, use findFractionRuleSetRule()
  if (isFractionRuleSet()) {
    return findFractionRuleSetRule(number);
  }

  // if the number is negative, return the negative number rule
  // (if there isn't a negative-number rule, we pretend it's a
  // positive number)
  if (number < 0) {
    if (negativeNumberRule) {
      return  negativeNumberRule;
    } else {
      number = -number;
    }
  }
  
  // if the number isn't an integer, we use one of the fraction rules...
  if (number != uprv_floor(number)) {
    // if the number is between 0 and 1, return the proper
    // fraction rule
    if (number < 1 && fractionRules[1]) {
      return fractionRules[1];
    } 
    // otherwise, return the improper fraction rule
    else if (fractionRules[0]) {
      return fractionRules[0];
    }
  }

  // if there's a master rule, use it to format the number
  if (fractionRules[2]) {
    return fractionRules[2];
  }      

  // and if we haven't yet returned a rule, use findNormalRule()
  // to find the applicable rule
  llong r = number + 0.5;
  return findNormalRule(r);
}

NFRule *
NFRuleSet::findNormalRule(llong number) const 
{
  // if this is a fraction rule set, use findFractionRuleSetRule()
  // to find the rule (we should only go into this clause if the
  // value is 0)
  if (fIsFractionRuleSet) {
    return findFractionRuleSetRule(llong_asDouble(number));
  }

  // if the number is negative, return the negative-number rule
  // (if there isn't one, pretend the number is positive)
  if (number < 0) {
    if (negativeNumberRule) {
      return negativeNumberRule;
    } else {
      number = -number;
    }
  }

  // we have to repeat the preceding two checks, even though we
  // do them in findRule(), because the version of format() that
  // takes a long bypasses findRule() and goes straight to this
  // function.  This function does skip the fraction rules since
  // we know the value is an integer (it also skips the master
  // rule, since it's considered a fraction rule.  Skipping the
  // master rule in this function is also how we avoid infinite
  // recursion)

  // binary-search the rule list for the applicable rule
  // (a rule is used for all values from its base value to
  // the next rule's base value)
  int32_t lo = 0;
  int32_t hi = rules.size();
  while (lo < hi) {
    int32_t mid = (lo + hi) / 2;
    if (rules[mid]->getBaseValue() == number) {
      return rules[mid];
    }
    else if (rules[mid]->getBaseValue() > number) {
      hi = mid;
    }
    else {
      lo = mid + 1;
    }
  }
  NFRule *result = rules[hi - 1];

  // use shouldRollBack() to see whether we need to invoke the
  // rollback rule (see shouldRollBack()'s documentation for
  // an explanation of the rollback rule).  If we do, roll back
  // one rule and return that one instead of the one we'd normally
  // return
  if (result->shouldRollBack(llong_asDouble(number))) {
    result = rules[hi - 2];
  }

  return result;
}

/**
 * If this rule is a fraction rule set, this function is used by
 * findRule() to select the most appropriate rule for formatting
 * the number.  Basically, the base value of each rule in the rule
 * set is treated as the denominator of a fraction.  Whichever
 * denominator can produce the fraction closest in value to the
 * number passed in is the result.  If there's a tie, the earlier
 * one in the list wins.  (If there are two rules in a row with the
 * same base value, the first one is used when the numerator of the
 * fraction would be 1, and the second rule is used the rest of the
 * time.
 * @param number The number being formatted (which will always be
 * a number between 0 and 1)
 * @return The rule to use to format this number
 */
NFRule*
NFRuleSet::findFractionRuleSetRule(double number) const 
{
  // the obvious way to do this (multiply the value being formatted
  // by each rule's base value until you get an integral result)
  // doesn't work because of rounding error.  This method is more
  // accurate

  // find the least common multiple of the rules' base values
  // and multiply this by the number being formatted.  This is
  // all the precision we need, and we can do all of the rest
  // of the math using integer arithmetic
  llong leastCommonMultiple = rules[0]->getBaseValue();
  llong numerator;
  {
    for (uint32_t i = 1; i < rules.size(); ++i) {
      leastCommonMultiple = util_lcm(leastCommonMultiple, rules[i]->getBaseValue());
    }
    numerator = number * llong_asDouble(leastCommonMultiple) + 0.5;
  }
  // for each rule, do the following...
  llong tempDifference;
  llong difference = llong::kMaxValue;
  int32_t winner = 0;
  for (uint32_t i = 0; i < rules.size(); ++i) {
    // "numerator" is the numerator of the fraction if the 
    // denominator is the LCD.  The numerator if the rule's
    // base value is the denominator is "numerator" times the
    // base value divided bythe LCD.  Here we check to see if
    // that's an integer, and if not, how close it is to being
    // an integer.
    tempDifference = numerator * rules[i]->getBaseValue() % leastCommonMultiple;
	

    // normalize the result of the above calculation: we want
    // the numerator's distance from the CLOSEST multiple
    // of the LCD
    if (leastCommonMultiple - tempDifference < tempDifference) {
      tempDifference = leastCommonMultiple - tempDifference;
    }

    // if this is as close as we've come, keep track of how close
    // that is, and the line number of the rule that did it.  If
    // we've scored a direct hit, we don't have to look at any more
    // rules
    if (tempDifference < difference) {
      difference = tempDifference;
      winner = i;
      if (difference == 0) {
        break;
      }
    }
  }

  // if we have two successive rules that both have the winning base
  // value, then the first one (the one we found above) is used if
  // the numerator of the fraction is 1 and the second one is used if
  // the numerator of the fraction is anything else (this lets us
  // do things like "one third"/"two thirds" without haveing to define
  // a whole bunch of extra rule sets)
  if ((unsigned)(winner + 1) < rules.size() &&
      rules[winner + 1]->getBaseValue() == rules[winner]->getBaseValue()) {
    double n = llong_asDouble(rules[winner]->getBaseValue()) * number;
    if (n < 0.5 || n >= 2) {
      ++winner;
    }
  }

  // finally, return the winning rule
  return rules[winner];
}

/**
 * Parses a string.  Matches the string to be parsed against each
 * of its rules (with a base value less than upperBound) and returns
 * the value produced by the rule that matched the most charcters
 * in the source string.
 * @param text The string to parse
 * @param parsePosition The initial position is ignored and assumed
 * to be 0.  On exit, this object has been updated to point to the
 * first character position this rule set didn't consume.
 * @param upperBound Limits the rules that can be allowed to match.
 * Only rules whose base values are strictly less than upperBound
 * are considered.
 * @return The numerical result of parsing this string.  This will
 * be the matching rule's base value, composed appropriately with
 * the results of matching any of its substitutions.  The object
 * will be an instance of Long if it's an integral value; otherwise,
 * it will be an instance of Double.  This function always returns
 * a valid object: If nothing matched the input string at all,
 * this function returns new Long(0), and the parse position is
 * left unchanged.
 */
#ifdef RBNF_DEBUG
static void dumpUS(FILE* f, const UnicodeString& us) {
  int len = us.length();
  char* buf = new char[len+1];
  us.extract(0, len, buf);
  buf[len] = 0;
  fprintf(f, "%s", buf);
  delete[] buf;
}
#endif

UBool
NFRuleSet::parse(const UnicodeString& text, ParsePosition& pos, double upperBound, Formattable& result) const
{
  // try matching each rule in the rule set against the text being
  // parsed.  Whichever one matches the most characters is the one
  // that determines the value we return.
	
  result.setLong(0);
	
  // dump out if there's no text to parse
  if (text.length() == 0) {
    return 0;
  }
	
  ParsePosition highWaterMark;
  ParsePosition workingPos = pos;

#ifdef RBNF_DEBUG
  fprintf(stderr, "<nfrs> %x '", this);
  dumpUS(stderr, name);
  fprintf(stderr, "' text '");
  dumpUS(stderr, text);
  fprintf(stderr, "'\n");
  fprintf(stderr, "  parse negative: %d\n", this, negativeNumberRule != 0);
#endif

  // start by trying the negative number rule (if there is one)
  if (negativeNumberRule) {
    Formattable tempResult;
#ifdef RBNF_DEBUG
    fprintf(stderr, "  <nfrs before negative> %x ub: %g\n", negativeNumberRule, upperBound);
#endif
    UBool success = negativeNumberRule->doParse(text, workingPos, 0, upperBound, tempResult);
#ifdef RBNF_DEBUG
    fprintf(stderr, "  <nfrs after negative> success: %d wpi: %d\n", success, workingPos.getIndex());
#endif
    if (success && workingPos.getIndex() > highWaterMark.getIndex()) {
      result = tempResult;
      highWaterMark = workingPos;
    }
    workingPos = pos;
  }
#ifdef RBNF_DEBUG
  fprintf(stderr, "<nfrs> continue fractional with text '");
  dumpUS(stderr, text);
  fprintf(stderr, "' hwm: %d\n", highWaterMark.getIndex());
#endif
  // then try each of the fraction rules
  {
    for (int i = 0; i < 3; i++) {
      if (fractionRules[i]) {
		Formattable tempResult;
        UBool success = fractionRules[i]->doParse(text, workingPos, 0, upperBound, tempResult);
        if (success && (workingPos.getIndex() > highWaterMark.getIndex())) {
          result = tempResult;
          highWaterMark = workingPos;
        }
        workingPos = pos;
      }
    }
  }
#ifdef RBNF_DEBUG
  fprintf(stderr, "<nfrs> continue other with text '");
  dumpUS(stderr, text);
  fprintf(stderr, "' hwm: %d\n", highWaterMark.getIndex());
#endif

  // finally, go through the regular rules one at a time.  We start
  // at the end of the list because we want to try matching the most
  // sigificant rule first (this helps ensure that we parse
  // "five thousand three hundred six" as
  // "(five thousand) (three hundred) (six)" rather than
  // "((five thousand three) hundred) (six)").  Skip rules whose
  // base values are higher than the upper bound (again, this helps
  // limit ambiguity by making sure the rules that match a rule's
  // are less significant than the rule containing the substitutions)/
  {
    llong ub(upperBound);
#ifdef RBNF_DEBUG
    {
      char ubstr[64];
      lltoa(ub, ubstr, 64);
      fprintf(stderr, "ub: %g, ll: %s(%x/%x)\n", upperBound, ubstr, ub.hi, ub.lo);
    }
#endif
    for (int32_t i = rules.size(); --i >= 0 && highWaterMark.getIndex() < text.length();) {
      if ((!fIsFractionRuleSet) && (rules[i]->getBaseValue() >= ub)) {
        continue;
      }
	  Formattable tempResult;
      UBool success = rules[i]->doParse(text, workingPos, fIsFractionRuleSet, upperBound, tempResult);
      if (success && workingPos.getIndex() > highWaterMark.getIndex()) {
        result = tempResult;
        highWaterMark = workingPos;
      }
      workingPos = pos;
    }
  }
#ifdef RBNF_DEBUG
  fprintf(stderr, "<nfrs> exit\n");
#endif
  // finally, update the parse postion we were passed to point to the
  // first character we didn't use, and return the result that
  // corresponds to that string of characters
  pos = highWaterMark;
	
  return 1;
}

void 
NFRuleSet::appendRules(UnicodeString& result) const
{
        // the rule set name goes first...
        result.append(name);
		result.append(gColon);
		result.append(gLineFeed);

        // followed by the regular rules...
        for (uint32_t i = 0; i < rules.size(); i++) {
			result.append(gFourSpaces);
			rules[i]->appendRuleText(result);
			result.append(gLineFeed);
        }

        // followed by the special rules (if they exist)
        if (negativeNumberRule) {
			result.append(gFourSpaces);
			negativeNumberRule->appendRuleText(result);
			result.append(gLineFeed);
        }

        {
		for (uint32_t i = 0; i < 3; ++i) {
			if (fractionRules[i]) {
				result.append(gFourSpaces);
				fractionRules[i]->appendRuleText(result);
				result.append(gLineFeed);
			}
		}
        }
    }

U_NAMESPACE_END
