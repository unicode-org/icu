/*
*******************************************************************************
* Copyright (C) 2007-2013, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*
* File PLURRULE_IMPL.H
*
*******************************************************************************
*/


#ifndef PLURRULE_IMPLE
#define PLURRULE_IMPLE

// Internal definitions for the PluralRules implementation.

#if !UCONFIG_NO_FORMATTING

#include "unicode/format.h"
#include "unicode/locid.h"
#include "unicode/parseerr.h"
#include "unicode/utypes.h"
#include "uvector.h"
#include "hash.h"

U_NAMESPACE_BEGIN

static const UChar DOT             = ((UChar)0x002E);
static const UChar SINGLE_QUOTE    = ((UChar)0x0027);
static const UChar SLASH           = ((UChar)0x002F);
static const UChar BACKSLASH       = ((UChar)0x005C);
static const UChar SPACE           = ((UChar)0x0020);
static const UChar EXCLAMATION     = ((UChar)0x0021);
static const UChar QUOTATION_MARK  = ((UChar)0x0022);
static const UChar NUMBER_SIGN     = ((UChar)0x0023);
static const UChar PERCENT_SIGN    = ((UChar)0x0025);
static const UChar ASTERISK        = ((UChar)0x002A);
static const UChar COMMA           = ((UChar)0x002C);
static const UChar HYPHEN          = ((UChar)0x002D);
static const UChar U_ZERO          = ((UChar)0x0030);
static const UChar U_ONE           = ((UChar)0x0031);
static const UChar U_TWO           = ((UChar)0x0032);
static const UChar U_THREE         = ((UChar)0x0033);
static const UChar U_FOUR          = ((UChar)0x0034);
static const UChar U_FIVE          = ((UChar)0x0035);
static const UChar U_SIX           = ((UChar)0x0036);
static const UChar U_SEVEN         = ((UChar)0x0037);
static const UChar U_EIGHT         = ((UChar)0x0038);
static const UChar U_NINE          = ((UChar)0x0039);
static const UChar COLON           = ((UChar)0x003A);
static const UChar SEMI_COLON      = ((UChar)0x003B);
static const UChar EQUALS          = ((UChar)0x003D);
static const UChar CAP_A           = ((UChar)0x0041);
static const UChar CAP_B           = ((UChar)0x0042);
static const UChar CAP_R           = ((UChar)0x0052);
static const UChar CAP_Z           = ((UChar)0x005A);
static const UChar LOWLINE         = ((UChar)0x005F);
static const UChar LEFTBRACE       = ((UChar)0x007B);
static const UChar RIGHTBRACE      = ((UChar)0x007D);

static const UChar LOW_A           = ((UChar)0x0061);
static const UChar LOW_B           = ((UChar)0x0062);
static const UChar LOW_C           = ((UChar)0x0063);
static const UChar LOW_D           = ((UChar)0x0064);
static const UChar LOW_E           = ((UChar)0x0065);
static const UChar LOW_F           = ((UChar)0x0066);
static const UChar LOW_G           = ((UChar)0x0067);
static const UChar LOW_H           = ((UChar)0x0068);
static const UChar LOW_I           = ((UChar)0x0069);
static const UChar LOW_J           = ((UChar)0x006a);
static const UChar LOW_K           = ((UChar)0x006B);
static const UChar LOW_L           = ((UChar)0x006C);
static const UChar LOW_M           = ((UChar)0x006D);
static const UChar LOW_N           = ((UChar)0x006E);
static const UChar LOW_O           = ((UChar)0x006F);
static const UChar LOW_P           = ((UChar)0x0070);
static const UChar LOW_Q           = ((UChar)0x0071);
static const UChar LOW_R           = ((UChar)0x0072);
static const UChar LOW_S           = ((UChar)0x0073);
static const UChar LOW_T           = ((UChar)0x0074);
static const UChar LOW_U           = ((UChar)0x0075);
static const UChar LOW_V           = ((UChar)0x0076);
static const UChar LOW_W           = ((UChar)0x0077);
static const UChar LOW_Y           = ((UChar)0x0079);
static const UChar LOW_Z           = ((UChar)0x007A);


static const int32_t PLURAL_RANGE_HIGH = 0x7fffffff;

enum tokenType {
  none,
  tLetter,
  tNumber,
  tComma,
  tSemiColon,
  tSpace,
  tColon,
  tDot,
  tKeyword,
  tAnd,
  tOr,
  tMod,
  tNot,
  tIn,
  tWithin,
  tVariableN,
  tVariableI,
  tVariableF,
  tVariableV,
  tVariableJ,
  tVariableT,
  tIs,
  tEOF
};


class RuleParser : public UMemory {
public:
    RuleParser();
    virtual ~RuleParser();
    void getNextToken(const UnicodeString& ruleData, int32_t *ruleIndex, UnicodeString& token,
                            tokenType& type, UErrorCode &status);
    void checkSyntax(tokenType prevType, tokenType curType, UErrorCode &status);
private:
    void getKeyType(const UnicodeString& token, tokenType& type, UErrorCode &status);
    UBool inRange(UChar ch, tokenType& type);
    UBool isValidKeyword(const UnicodeString& token);
};

class NumberInfo: public UMemory {
  public:
    /**
      * @param n   the number
      * @param v   The number of visible fraction digits
      * @param f   The fraction digits.
      *
      */
    NumberInfo(double  n, int32_t v, int64_t f);
    NumberInfo(double n, int32_t);
    explicit NumberInfo(double n);

    double get(tokenType operand) const;
    int32_t getVisibleFractionDigitCount() const;

  private:
    void init(double n, int32_t v, int64_t f);
    static int32_t getFractionalDigits(double n, int32_t v);
    static int32_t decimals(double n);

    double      source;
    int32_t     visibleFractionDigitCount;
    int64_t     fractionalDigits;
    int64_t     fractionalDigitsWithoutTrailingZeros;
    int64_t     intValue;
    UBool       hasIntegerValue;
    UBool       isNegative;
};

class AndConstraint : public UMemory  {
public:
    typedef enum RuleOp {
        NONE,
        MOD
    } RuleOp;
    RuleOp  op;
    int32_t opNum;           // for mod expressions, the right operand of the mod.
    int32_t     value;       // valid for 'is' rules only.
    UVector32   *rangeList;  // for 'in', 'within' rules. Null otherwise.
    UBool   negated;           // TRUE for negated rules.
    UBool   integerOnly;     // TRUE for 'within' rules.
    tokenType digitsType;    // n | i | v | f constraint.
    AndConstraint *next;

    AndConstraint();
    AndConstraint(const AndConstraint& other);
    virtual ~AndConstraint();
    AndConstraint* add();
    // UBool isFulfilled(double number);
    UBool isFulfilled(const NumberInfo &number);
    UBool isLimited();
};

class OrConstraint : public UMemory  {
public:
    AndConstraint *childNode;
    OrConstraint *next;
    OrConstraint();

    OrConstraint(const OrConstraint& other);
    virtual ~OrConstraint();
    AndConstraint* add();
    // UBool isFulfilled(double number);
    UBool isFulfilled(const NumberInfo &number);
    UBool isLimited();
};

class RuleChain : public UMemory  {
public:
    OrConstraint *ruleHeader;
    UnicodeString keyword;
    RuleChain();
    RuleChain(const RuleChain& other);
    RuleChain *next;

    virtual ~RuleChain();
    UnicodeString select(const NumberInfo &number) const;
    void dumpRules(UnicodeString& result);
    UBool isLimited();
    UErrorCode getKeywords(int32_t maxArraySize, UnicodeString *keywords, int32_t& arraySize) const;
    UBool isKeyword(const UnicodeString& keyword) const;
};

class PluralKeywordEnumeration : public StringEnumeration {
public:
    PluralKeywordEnumeration(RuleChain *header, UErrorCode& status);
    virtual ~PluralKeywordEnumeration();
    static UClassID U_EXPORT2 getStaticClassID(void);
    virtual UClassID getDynamicClassID(void) const;
    virtual const UnicodeString* snext(UErrorCode& status);
    virtual void reset(UErrorCode& status);
    virtual int32_t count(UErrorCode& status) const;
private:
    int32_t pos;
    UVector fKeywordNames;
};


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // _PLURRULE_IMPL
//eof
