/*
*******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*
* File PLURRULE.CPP
*
* Modification History:
*
*   Date        Name        Description
*******************************************************************************
*/


#include "unicode/uniset.h"
#include "unicode/utypes.h"
#include "unicode/plurrule.h"
#include "cmemory.h"
#include "cstring.h"
#include "hash.h"
#include "mutex.h"
#include "plurrule_impl.h"
#include "putilimp.h"
#include "ustrfmt.h"

#if !UCONFIG_NO_FORMATTING

U_NAMESPACE_BEGIN


// Plural rule data - will move to ResourceBundle.
static const UnicodeString PLURAL_RULE_DATA[] = {
    UNICODE_STRING_SIMPLE("other: n/ja,ko,tr,vi"),
    UNICODE_STRING_SIMPLE("one: n is 1/da,de,el,en,eo,es,et,fi,fo,he,hu,it,nb,nl,nn,no,pt,sv"),
    UNICODE_STRING_SIMPLE("one: n in 0..1/fr,pt_BR"),
    UNICODE_STRING_SIMPLE("zero: n is 0; one: n mod 10 is 1 and n mod 100 is not 11/lv"),
    UNICODE_STRING_SIMPLE("one: n is 1; two: n is 2/ga"),
    UNICODE_STRING_SIMPLE("zero: n is 0; one: n is 1; zero: n mod 100 in 1..19/ro"),
    UNICODE_STRING_SIMPLE("other: n mod 100 in 11..19; one: n mod 10 is 1; few: n mod 10 in 2..9/lt"),
    UNICODE_STRING_SIMPLE("one: n mod 10 is 1 and n mod 100 is not 11; few: n mod 10 in 2..4 ") +
    UNICODE_STRING_SIMPLE("and n mod 100 not in 12..14/hr,ru,sr,uk"),
    UNICODE_STRING_SIMPLE("one: n is 1; few: n in 2..4/cs,sk"),
    UNICODE_STRING_SIMPLE("one: n is 1; few: n mod 10 in 2..4 and n mod 100 not in 12..14/pl"),
    UNICODE_STRING_SIMPLE("one: n mod 100 is 1; two: n mod 100 is 2; few: n mod 100 in 3..4/sl"),
    UNICODE_STRING_SIMPLE("zero: n is 0; one: n is 1; two: n is 2; few: n is 3..10; many: n in 11..99/ar"),
    ""
};

static Hashtable *fPluralRuleLocaleHash=NULL;

static const UnicodeString PLURAL_KEYWORD_ZERO = UNICODE_STRING_SIMPLE("zero");
static const UnicodeString PLURAL_KEYWORD_ONE = UNICODE_STRING_SIMPLE("one");
static const UnicodeString PLURAL_KEYWORD_TWO = UNICODE_STRING_SIMPLE("two");
static const UnicodeString PLURAL_KEYWORD_FEW = UNICODE_STRING_SIMPLE("few");
static const UnicodeString PLURAL_KEYWORD_MANY = UNICODE_STRING_SIMPLE("many");
static const UnicodeString PLURAL_KEYWORD_OTHER = UNICODE_STRING_SIMPLE("other");
static const UnicodeString PLURAL_DEFAULT_RULE = UNICODE_STRING_SIMPLE("other: n");

static const UnicodeString   PK_IN=UNICODE_STRING_SIMPLE("in");
static const UnicodeString   PK_NOT=UNICODE_STRING_SIMPLE("not");
static const UnicodeString   PK_IS=UNICODE_STRING_SIMPLE("is");
static const UnicodeString   PK_MOD=UNICODE_STRING_SIMPLE("mod");
static const UnicodeString   PK_AND=UNICODE_STRING_SIMPLE("and");
static const UnicodeString   PK_OR=UNICODE_STRING_SIMPLE("or");
static const UnicodeString   PK_VAR_N=UNICODE_STRING_SIMPLE("n");



UOBJECT_DEFINE_RTTI_IMPLEMENTATION(PluralRules);
UOBJECT_DEFINE_RTTI_IMPLEMENTATION(PluralKeywordEnumeration)

PluralRules::PluralRules(UErrorCode& status) {
    fLocaleStringsHash=NULL;
    rules = NULL;
    parser = new RuleParser();
    initHashtable(status);
    if (U_SUCCESS(status)) {
        getRuleData(status);
    }
}

PluralRules::PluralRules(const PluralRules& other) {
    *this=other;
}

PluralRules::~PluralRules() {
    delete rules;
    delete parser;
}


PluralRules*
PluralRules::clone() const {
    return new PluralRules(*this);
}

PluralRules&
PluralRules:: operator=(const PluralRules& other) {
    fLocaleStringsHash=other.fLocaleStringsHash;
    rules = new RuleChain(*other.rules);
    parser = new RuleParser();
    
    return *this;
}

PluralRules* U_EXPORT2
PluralRules::createRules(const UnicodeString& description, UErrorCode& status) {
    RuleChain   rules;
    
    PluralRules *newRules = new PluralRules(status);
    if ( (newRules != NULL)&& U_SUCCESS(status) ) {
        status = newRules->parseDescription((UnicodeString &)description, rules);
        if (U_SUCCESS(status)) {
          newRules->addRules(rules, status);
        }
    }
    if (U_FAILURE(status)) {
        delete newRules;
        return NULL;
    }
    else {
        return newRules;
    }
}

PluralRules* U_EXPORT2
PluralRules::createDefaultRules(UErrorCode& status) {
    return createRules(PLURAL_DEFAULT_RULE, status);
}

PluralRules* U_EXPORT2
PluralRules::forLocale(const Locale& locale, UErrorCode& status) {
    RuleChain *locRules;

    PluralRules *newRules = new PluralRules(status);
    UnicodeString localeName=UnicodeString(locale.getName());
    locRules = (RuleChain *) (fPluralRuleLocaleHash->get(localeName));
    if ( locRules==NULL ) {
        // Check parent locales.
        char parentLocale[50];
        const char *curLocaleName=locale.getName();
        int32_t localeNameLen=0;
        uprv_strcpy(parentLocale, curLocaleName);
        while((localeNameLen=uloc_getParent(parentLocale, parentLocale, 50, &status))>=0 ) {
            locRules = (RuleChain *) (fPluralRuleLocaleHash->get(localeName));
            if ( locRules != NULL ) {
                break;
            }
        }
    }
    if (locRules==NULL) {
        return createRules(PLURAL_DEFAULT_RULE, status);
    }

    newRules->addRules(*locRules, status);
    return newRules;
}

UnicodeString
PluralRules::select(int32_t number) const {
    if (rules == NULL) {
        return PLURAL_DEFAULT_RULE;
    }
    else {
        return rules->select(number);
    }
}

StringEnumeration*
PluralRules::getKeywords(UErrorCode& status) const {
    if (U_FAILURE(status))  return NULL;
    StringEnumeration* nameEnumerator = new PluralKeywordEnumeration(status);
    return nameEnumerator;
}


UBool
PluralRules::isKeyword(const UnicodeString& keyword) const {
    if ( rules == NULL) {
        if ( keyword != PLURAL_DEFAULT_RULE ) {
            return FALSE;
        }
        else {
            return TRUE;
        }
    }
    else {
        return rules->isKeyword(keyword);
    }
}

UnicodeString
PluralRules::getKeywordOther() const {
    return PLURAL_KEYWORD_OTHER;
    
}

UBool
PluralRules::operator==(const PluralRules& other) const  {
    int32_t limit;
    UBool sameList = TRUE;
    const UnicodeString *ptrKeyword;
    UErrorCode status= U_ZERO_ERROR;
    
    if ( this == &other ) {
        return TRUE;
    }
    StringEnumeration* myKeywordList = getKeywords(status);
    StringEnumeration* otherKeywordList =other.getKeywords(status);
    
    if (myKeywordList->count(status)!=otherKeywordList->count(status)) {
        sameList = FALSE;
    }
    else {
        myKeywordList->reset(status);
        while (sameList && (ptrKeyword=myKeywordList->snext(status))!=NULL) {
            if (!other.isKeyword(*ptrKeyword)) {
                sameList = FALSE;
            }
        }
        otherKeywordList->reset(status);
        while (sameList && (ptrKeyword=otherKeywordList->snext(status))!=NULL) {
            if (!this->isKeyword(*ptrKeyword))  {
                sameList = FALSE;
            }
        }
        delete myKeywordList;
        delete otherKeywordList;
        if (!sameList) {
            return FALSE;
        }
    }
    
    if ((limit=this->getRepeatLimit()) != other.getRepeatLimit()) {
        return FALSE;
    }
    UnicodeString myKeyword, otherKeyword;
    for (int32_t i=0; i<limit; ++i) {
        myKeyword = this->select(i);
        otherKeyword = other.select(i);
        if (myKeyword!=otherKeyword) {
            return FALSE;
        }
    }
    return TRUE;
}

void 
PluralRules::getRuleData(UErrorCode& status) {
    UnicodeString ruleData;
    UnicodeString localeData;
    UnicodeString localeName;
    int32_t i=0;
    UChar cSlash = (UChar)0x002F;
    status=U_ZERO_ERROR;
    
    
    while ( (PLURAL_RULE_DATA[i].length() > 0) && U_SUCCESS(status) ) {
        RuleChain   rules;
        int32_t slashIndex = PLURAL_RULE_DATA[i].indexOf(cSlash);
        if ( slashIndex < 0 ) {
            break;
        }
        ruleData=UnicodeString(PLURAL_RULE_DATA[i], 0, slashIndex);
        localeData=UnicodeString(PLURAL_RULE_DATA[i], slashIndex+1);
        status = parseDescription(ruleData, rules);
        int32_t curIndex=0;
        while (curIndex < localeData.length() && U_SUCCESS(status)) {
            getNextLocale(localeData, &curIndex, localeName);
            addRules(localeName, rules, TRUE, status);
        }
        
        i++;
    }
}

UErrorCode
PluralRules::parseDescription(UnicodeString& data, RuleChain& rules) {
    UErrorCode status=U_ZERO_ERROR;
    int32_t ruleIndex=0;
    UnicodeString token;
    tokenType type;
    tokenType prevType=none;
    RuleChain *ruleChain=NULL;
    AndConstraint *curAndConstraint=NULL;
    OrConstraint *orNode=NULL;
    
    UnicodeString ruleData = data.toLower();
    while (ruleIndex< ruleData.length()) {
        if ((status=parser->getNextToken(ruleData, &ruleIndex, token, type))!=U_ZERO_ERROR) {
            return status;
        }
        if ((status=parser->checkSyntax(prevType, type))!=U_ZERO_ERROR) {
            return status;
        }
        switch (type) {
            case tAnd:
                curAndConstraint = curAndConstraint->add();
                break;
            case tOr:
                orNode=rules.ruleHeader;
                while (orNode->next != NULL) {
                    orNode = orNode->next;
                }
                orNode->next= new OrConstraint();
                orNode=orNode->next;
                orNode->next=NULL;
                curAndConstraint = orNode->add();
                break;
            case tIs:
                curAndConstraint->rangeHigh=-1;
                break;
            case tNot:
                curAndConstraint->notIn=TRUE;
                break;
            case tIn:
                curAndConstraint->rangeHigh=PLURAL_RANGE_HIGH;
                break;
            case tNumber:
                if ( (curAndConstraint->op==AndConstraint::MOD)&& 
                     (curAndConstraint->opNum == -1 ) ) {
                    curAndConstraint->opNum=getNumberValue(token);
                }
                else {
                    if (curAndConstraint->rangeLow == -1) {
                        curAndConstraint->rangeLow=getNumberValue(token);
                    }
                    else {
                        curAndConstraint->rangeHigh=getNumberValue(token);
                    }
                }
                break;
            case tMod:
                curAndConstraint->op=AndConstraint::MOD;
                break;
            case tKeyword:
                if (ruleChain==NULL) {
                    ruleChain = &rules;
                }
                else {
                    while (ruleChain->next!=NULL){
                        ruleChain=ruleChain->next;
                    }
                    ruleChain=ruleChain->next=new RuleChain();
                }
                orNode = ruleChain->ruleHeader = new OrConstraint();
                curAndConstraint = orNode->add();
                ruleChain->keyword = token;
                break;
        }
        prevType=type;
    }
    
    return status;
}

int32_t 
PluralRules::getNumberValue(const UnicodeString& token) const {
    int32_t i;
    char digits[128];

    for (i=0; i<token.length() && i<127; ++i) {
      digits[i]=(char)token.charAt(i);
    }
    digits[i]='\0';
    
    return((int32_t)atoi(digits));
}


void
PluralRules::getNextLocale(const UnicodeString& localeData, int32_t* curIndex, UnicodeString& localeName) {
    int32_t i=*curIndex;
    
    localeName.remove();
    
    while (i< localeData.length()) {
       if ( (localeData.charAt(i)!= SPACE) && (localeData.charAt(i)!= COMMA) ) {
           break;
       }
       i++;
    }
    
    while (i< localeData.length()) {
       if ( (localeData.charAt(i)== SPACE) || (localeData.charAt(i)== COMMA) ) {
           break;
       }
       localeName+=localeData.charAt(i++);
    }
    *curIndex=i;
}


int32_t
PluralRules::getRepeatLimit() const {
    return rules->getRepeatLimit();
}

void 
PluralRules::initHashtable(UErrorCode& status) {
    if (fLocaleStringsHash!=NULL) {
        return;
    }
    if ( fPluralRuleLocaleHash == NULL ) {
        Mutex mutex;
        // This static PluralRule hashtable residents in memory until end of application.
        if ((fPluralRuleLocaleHash = new Hashtable(TRUE, status))!=NULL) {
            fLocaleStringsHash = fPluralRuleLocaleHash;
            return;
        }
    }
    else {
        fLocaleStringsHash = fPluralRuleLocaleHash;
    }
}

void 
PluralRules::addRules(RuleChain& rules, UErrorCode& status) {
    addRules(localeName, rules, FALSE, status);
}

void 
PluralRules::addRules(const UnicodeString& localeName, RuleChain& rules, UBool addToHash, UErrorCode& status) {
    RuleChain *newRule = new RuleChain(rules);
    if ( addToHash )
    {
        {
            Mutex mutex;
            if ( (RuleChain *)fLocaleStringsHash->get(localeName) == NULL ) {
                fLocaleStringsHash->put(localeName, newRule, status);
                this->rules=newRule;
            }
            else {
                delete newRule;
                return;
            }
        }
    }
    else {
        this->rules=newRule;
    }
    newRule->setRepeatLimit();
}

AndConstraint::AndConstraint() {
    op = AndConstraint::NONE;
    opNum=-1;
    rangeLow=-1;
    rangeHigh=-1;
    notIn=FALSE;
    next=NULL;
}


AndConstraint::AndConstraint(const AndConstraint& other) {
    this->op = other.op;
    this->opNum=other.opNum;
    this->rangeLow=other.rangeLow;
    this->rangeHigh=other.rangeHigh;
    this->notIn=other.notIn;
    if (other.next==NULL) {
        this->next=NULL;
    }
    else {
        this->next = new AndConstraint(*other.next);
    }
}

AndConstraint::~AndConstraint() {
    if (next!=NULL) {
        delete next;
    }
};


UBool
AndConstraint::isFulfilled(int32_t number) {
    UBool result=TRUE;
    int32_t value=number;
    
    if ( op == MOD ) {
        value = value % opNum;
    }
    if ( rangeHigh == -1 ) {
        if ( rangeLow == -1 ) {
            result = TRUE; // empty rule
        }
        else {
            if ( value == rangeLow ) {
                result = TRUE;
            }
            else {
                result = FALSE;
            }
        }
    }
    else {
        if ((rangeLow <= value) && (value <= rangeHigh)) {
            result = TRUE;
        }
        else {
            result = FALSE;
        }
    }
    if (notIn) {
        return !result;
    }
    else {
        return result;
    }
}

int32_t
AndConstraint::updateRepeatLimit(int32_t maxLimit) {
    
    if ( op == MOD ) {
        return uprv_max(opNum, maxLimit);
    }
    else {
        if ( rangeHigh == -1 ) {
            return(rangeLow>maxLimit? rangeLow : maxLimit);
            return uprv_max(rangeLow, maxLimit);
        }
        else{
            return uprv_max(rangeHigh, maxLimit);
        }
    }
}


AndConstraint*
AndConstraint::add()
{
    this->next = new AndConstraint();
    return this->next;
}

OrConstraint::OrConstraint() {
    childNode=NULL;
    next=NULL;
}

OrConstraint::OrConstraint(const OrConstraint& other) {
    if ( other.childNode == NULL ) {
        this->childNode = NULL;
    }
    else {
        this->childNode = new AndConstraint(*(other.childNode));
    }
    if (other.next == NULL ) {
        this->next = NULL;
    }
    else {
        this->next = new OrConstraint(*(other.next));
    }
}

OrConstraint::~OrConstraint() {
    if (childNode!=NULL) {
        delete childNode;
    }
    if (next!=NULL) {
        delete next;
    }
}

AndConstraint*
OrConstraint::add()
{
    OrConstraint *curOrConstraint=this;
    {
        Mutex mutex;
        
        while (curOrConstraint->next!=NULL) {
            curOrConstraint = curOrConstraint->next;
        }
        curOrConstraint->next = NULL;
        curOrConstraint->childNode = new AndConstraint();
    }
    return curOrConstraint->childNode;
}

UBool
OrConstraint::isFulfilled(int32_t number) {
    OrConstraint* orRule=this;
    UBool result=FALSE;
    
    while (orRule!=NULL && !result) {
        result=TRUE;
        AndConstraint* andRule = orRule->childNode;
        while (andRule!=NULL && result) {
            result = andRule->isFulfilled(number);
            andRule=andRule->next;
        }
        orRule = orRule->next;
    }
    
    return result;
}


RuleChain::RuleChain() {
    ruleHeader=NULL;
    next = NULL;
    repeatLimit=0;
}

RuleChain::RuleChain(const RuleChain& other) {
    
    this->repeatLimit = other.repeatLimit;
    this->keyword=other.keyword;
    if (other.ruleHeader != NULL) {
        this->ruleHeader = new OrConstraint(*(other.ruleHeader));
    }
    else {
        this->ruleHeader = NULL;
    }
    if (other.next != NULL ) {
        this->next = new RuleChain(*other.next);
    }
    else
    {
        this->next = NULL;
    }
}

RuleChain::~RuleChain() {
    if (next != NULL) {
        delete next;
    }
    if ( ruleHeader != NULL ) {
        delete ruleHeader;
    }
}

UnicodeString
RuleChain::select(int32_t number) const {
   
   if ( ruleHeader != NULL ) {
       if (ruleHeader->isFulfilled(number)) {
           return keyword;
       }
   }
   if ( next != NULL ) {
       return next->select(number);
   }
   else {
       return PLURAL_KEYWORD_OTHER;
   }

}

void
RuleChain::dumpRules(UnicodeString& result) {
    UChar digitString[16];
    
    if ( ruleHeader != NULL ) {
        result +=  keyword;
        OrConstraint* orRule=ruleHeader;
        while ( orRule != NULL ) {
            AndConstraint* andRule=orRule->childNode;
            while ( andRule != NULL ) {
                if ( (andRule->op==AndConstraint::NONE) && (andRule->rangeHigh==-1) ) {
                    result += UNICODE_STRING_SIMPLE(" n is ");
                    if (andRule->notIn) {
                        result += UNICODE_STRING_SIMPLE("not ");
                    }
                    uprv_itou(digitString,16, andRule->rangeLow,10,0);
                    result += UnicodeString(digitString);
                }
                else {
                    if (andRule->op==AndConstraint::MOD) {
                        result += UNICODE_STRING_SIMPLE("  n mod ");
                        uprv_itou(digitString,16, andRule->opNum,10,0);
                        result += UnicodeString(digitString);
                    }
                    else {
                        result += UNICODE_STRING_SIMPLE("  n ");
                    }
                  
                    if (andRule->rangeHigh==-1) {
                        if (andRule->notIn) {
                            result += UNICODE_STRING_SIMPLE(" is not ");
                            uprv_itou(digitString,16, andRule->rangeLow,10,0);
                            result += UnicodeString(digitString);
                        }
                        else {
                            result += UNICODE_STRING_SIMPLE(" is ");
                            uprv_itou(digitString,16, andRule->rangeLow,10,0);
                            result += UnicodeString(digitString);
                        }
                    }
                    else {
                        if (andRule->notIn) {
                            result += UNICODE_STRING_SIMPLE("  not in ");
                            uprv_itou(digitString,16, andRule->rangeLow,10,0);
                            result += UnicodeString(digitString);
                            result += UNICODE_STRING_SIMPLE(" .. ");
                            uprv_itou(digitString,16, andRule->rangeHigh,10,0);
                            result += UnicodeString(digitString);
                        }
                        else {
                            result += UNICODE_STRING_SIMPLE(" in ");
                            uprv_itou(digitString,16, andRule->rangeLow,10,0);
                            result += UnicodeString(digitString);
                            result += UNICODE_STRING_SIMPLE(" .. ");
                            uprv_itou(digitString,16, andRule->rangeHigh,10,0);
                        }
                    }
                }
                    
                if ( (andRule=andRule->next) != NULL) {
                    result += PK_AND;
                }
            }
            if ( (orRule = orRule->next) != NULL ) {
                result += PK_OR;
            }
        }
    }
    
    if ( next != NULL ) {
        next->dumpRules(result);
    }
}

int32_t
RuleChain::getRepeatLimit () {
    return repeatLimit;
}

void
RuleChain::setRepeatLimit () {
    int32_t limit=0;
    
    if ( next != NULL ) {
        next->setRepeatLimit();
        limit = next->repeatLimit;
    }
    
    if ( ruleHeader != NULL ) {
        OrConstraint* orRule=ruleHeader;
        while ( orRule != NULL ) {
            AndConstraint* andRule=orRule->childNode;
            while ( andRule != NULL ) {
                limit = andRule->updateRepeatLimit(limit);
                andRule = andRule->next;
            }
            orRule = orRule->next;
        }
    }
    
    repeatLimit = limit;
}

UErrorCode
RuleChain::getKeywords(int32_t capacityOfKeywords, UnicodeString* keywords, int32_t& arraySize) const {
    if ( arraySize < capacityOfKeywords-1 ) {
        keywords[arraySize++]=keyword;
    }
    else {
        return U_BUFFER_OVERFLOW_ERROR;
    }
    
    if ( next != NULL ) {
        return next->getKeywords(capacityOfKeywords, keywords, arraySize);
    }
    else {
        return U_ZERO_ERROR;
    }
}

UBool
RuleChain::isKeyword(const UnicodeString& keyword) const {
    if ( this->keyword == keyword ) {
        return TRUE;
    }
    
    if ( next != NULL ) {
        return next->isKeyword(keyword);
    }
    else {
        return FALSE;
    }
}


RuleParser::RuleParser() {
    UErrorCode err=U_ZERO_ERROR;
    const UnicodeString idStart=UNICODE_STRING_SIMPLE("[[a-z]]");
    const UnicodeString idContinue=UNICODE_STRING_SIMPLE("[[a-z][A-Z][_][0-9]]");
    idStartFilter = new UnicodeSet(idStart, err);
    idContinueFilter = new UnicodeSet(idContinue, err);
}

RuleParser::~RuleParser() {
    delete idStartFilter;
    delete idContinueFilter;
}

UErrorCode
RuleParser::checkSyntax(tokenType prevType, tokenType curType ) {
    UErrorCode status=U_ZERO_ERROR;
    
    switch(prevType) {
        case none:
        case tSemiColon:
            if (curType!=tKeyword) {
                return U_UNEXPECTED_TOKEN;
            }
            else {
                return U_ZERO_ERROR;
            }
        case tVariableN : 
            if ( (curType == tIs) || (curType == tMod) || (curType == tIn) || (curType == tNot) ) {
                return U_ZERO_ERROR;
            }
            else {
                return U_UNEXPECTED_TOKEN;
            }
        case tZero:
        case tOne:
        case tTwo:
        case tFew:
        case tMany:
        case tOther:
        case tKeyword:
            if ( curType == tColon ) {
                return U_ZERO_ERROR;
            }
            else {
                return U_UNEXPECTED_TOKEN;
            }
        case tColon :
            if ( curType == tVariableN ) {
                return U_ZERO_ERROR;
            }
            else {
                return U_UNEXPECTED_TOKEN;
            }
        case tIs:
            if ( (curType == tNumber) || (curType == tNot)) {
                return U_ZERO_ERROR;
            }
            else {
                return U_UNEXPECTED_TOKEN;
            }
        case tNot:
            if ((curType == tNumber) || (curType == tIn)){
                return U_ZERO_ERROR;
            }
            else {
                return U_UNEXPECTED_TOKEN;
            }
        case tMod:
        case tDot:
        case tIn:
        case tAnd:
        case tOr:
            if ( (curType == tNumber) || (curType == tVariableN) ){
                return U_ZERO_ERROR;
            }
            else {
                return U_UNEXPECTED_TOKEN;
            }
        case tNumber:
            if ((curType == tDot) || (curType == tSemiColon) || (curType == tIs) || (curType == tNot) || 
                (curType == tIn) || (curType == tAnd) || (curType == tOr) ){
                return U_ZERO_ERROR;
            }
            else {
                return U_UNEXPECTED_TOKEN;
            }
        default:
            return U_UNEXPECTED_TOKEN;
    }
}

UErrorCode
RuleParser::getNextToken(const UnicodeString& ruleData, 
                         int32_t *ruleIndex,
                         UnicodeString& token,
                         tokenType& type) {
    UErrorCode status=U_ZERO_ERROR;
    int32_t curIndex= *ruleIndex;
    UChar ch;
    tokenType prevType=none;
    
    while (curIndex<ruleData.length()) {
        ch = ruleData.charAt(curIndex);
        if ( !inRange(ch, type) ) {
            return U_ILLEGAL_CHARACTER;
        }
        switch (type) {
            case tSpace:
                if ( *ruleIndex != curIndex ) { // letter
                    token=UnicodeString(ruleData, *ruleIndex, curIndex-*ruleIndex);
                    *ruleIndex=curIndex;
                    type=prevType;
                    status=getKeyType(token, type);
                    return status;
                }
                else {
                    *ruleIndex=*ruleIndex+1;
                }
                break; // consective space
            case tColon:
            case tSemiColon:
                if ( *ruleIndex != curIndex ) {
                    token=UnicodeString(ruleData, *ruleIndex, curIndex-*ruleIndex);
                    *ruleIndex=curIndex;
                    type=prevType;
                    status=getKeyType(token, type);
                    return status;
                }
                else {
                    *ruleIndex=curIndex+1;
                    return status;
                }
            case tLetter:
                 if ((type==prevType)||(prevType==none)) {
                    prevType=type;
                    break;
                 }
            case tNumber:
                 if ((type==prevType)||(prevType==none)) {
                    prevType=type;
                    break;
                 }
                 else {
                    *ruleIndex=curIndex+1;
                    return status;
                 }
             case tDot:
                 if (prevType==none) {  // first dot
                    prevType=type;
                    continue;
                 }
                 else {
                     if ( *ruleIndex != curIndex ) {
                        token=UnicodeString(ruleData, *ruleIndex, curIndex-*ruleIndex);
                        *ruleIndex=curIndex;  // letter
                        type=prevType;
                        status=getKeyType(token, type);
                        return status;
                     }
                     else {  // two consective dots
                        *ruleIndex=curIndex+2;
                        return status;
                     }
                 }
        }
        curIndex++;
    }
    if ( curIndex>=ruleData.length() ) {
        if ( (type == tLetter)||(type == tNumber) ) {
            token=UnicodeString(ruleData, *ruleIndex, curIndex-*ruleIndex);
            status=getKeyType(token, type);
        }
        *ruleIndex = ruleData.length();
    }
    return status;
}

UBool
RuleParser::inRange(UChar ch, tokenType& type) {
    if ((ch>=CAP_A) && (ch<=CAP_Z)) {
        // we assume all characters are in lower case already.
        return FALSE;
    }
    if ((ch>=LOW_A) && (ch<=LOW_Z)) {
        type = tLetter;
        return TRUE;
    }
    if ((ch>=U_ZERO) && (ch<=U_NINE)) {
        type = tNumber;
        return TRUE;
    }
    switch (ch) {
        case COLON: 
            type = tColon;
            return TRUE;
        case SPACE:
            type = tSpace;
            return TRUE;
        case SEMI_COLON:
            type = tSemiColon;
            return TRUE;
        case DOT:
            type = tDot;
            return TRUE;
        default :
            type = none;
            return FALSE;
    }
}


UErrorCode
RuleParser::getKeyType(const UnicodeString& token, tokenType& keyType) {
    if ( keyType==tNumber) {
        return U_ZERO_ERROR;
    }
    if (token==PK_VAR_N) {
        keyType = tVariableN;
        return U_ZERO_ERROR;
    }
    if (token==PK_IS) {
        keyType = tIs;
        return U_ZERO_ERROR;
    }
    if (token==PK_AND) {
        keyType = tAnd;
        return U_ZERO_ERROR;
    }
    if (token==PK_IN) {
        keyType = tIn;
        return U_ZERO_ERROR;
    }
    if (token==PK_NOT) {
        keyType = tNot;
        return U_ZERO_ERROR;
    }
    if (token==PK_MOD) {
        keyType = tMod;
        return U_ZERO_ERROR;
    }
    if (token==PK_OR) {
        keyType = tOr;
        return U_ZERO_ERROR;
    }
    
    if ( isValidKeyword(token) ) {
        keyType = tKeyword;
        return U_ZERO_ERROR;
    }
  
    return U_UNEXPECTED_TOKEN;
}

UBool
RuleParser::isValidKeyword(const UnicodeString& token) {
    if ( token.length()==0 ) {
        return FALSE;
    }
    if ( idStartFilter->contains(token.charAt(0) )==TRUE ) {
        int32_t i;
        for (i=1; i< token.length(); i++) {
            if (idContinueFilter->contains(token.charAt(i))== FALSE) {
                return FALSE;
            }
        }
        return TRUE;
    }
    else {
        return FALSE;
    }
}

PluralKeywordEnumeration::PluralKeywordEnumeration(UErrorCode& status) :
fKeywordNames(status)
{
    pos=0;
}

const UnicodeString*
PluralKeywordEnumeration::snext(UErrorCode& status) {
    if (U_SUCCESS(status) && pos < fKeywordNames.size()) {
        return (const UnicodeString*)fKeywordNames.elementAt(pos++);
    }
    return NULL;
}

void
PluralKeywordEnumeration::reset(UErrorCode& /*status*/) {
    pos=0;
}

int32_t
PluralKeywordEnumeration::count(UErrorCode& /*status*/) const {
       return fKeywordNames.size();
}

PluralKeywordEnumeration::~PluralKeywordEnumeration() {
    UnicodeString *s;
    for (int32_t i=0; i<fKeywordNames.size(); ++i) {
        if ((s=(UnicodeString *)fKeywordNames.elementAt(i))!=NULL) {
            delete s;
        }
    }
}

U_NAMESPACE_END


#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
