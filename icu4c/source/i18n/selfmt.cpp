/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2010, International Business Machines Corporation and
 * others. All Rights Reserved.
 * Copyright (C) 2010 , Yahoo! Inc. 
 ********************************************************************
 *
 * File SELFMT.CPP
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   11/11/09    kirtig      Finished first cut of implementation.
 *   11/16/09    kirtig      Improved version 
 ********************************************************************/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/ucnv_err.h"
#include "unicode/uchar.h"
#include "unicode/umsg.h"
#include "unicode/rbnf.h"
#include "cmemory.h"
#include "util.h"
#include "uassert.h"
#include "ustrfmt.h"
#include "uvector.h"

#include "unicode/selfmt.h"
#include "selfmtimpl.h"

#if !UCONFIG_NO_FORMATTING

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(SelectFormat)

#define MAX_KEYWORD_SIZE 30
static const UChar SELECT_KEYWORD_OTHER[] = {LOW_O, LOW_T, LOW_H, LOW_E, LOW_R, 0};

SelectFormat::SelectFormat(const UnicodeString& pat, UErrorCode& status) {
   if (U_FAILURE(status)) {
      return;
   } 
   init(status);
   applyPattern(pat, status);
}

SelectFormat::SelectFormat(const SelectFormat& other) : Format(other) {
   UErrorCode status = U_ZERO_ERROR;
   pattern = other.pattern;
   copyHashtable(other.parsedValuesHash, status);
}

SelectFormat::~SelectFormat() {
    delete parsedValuesHash;
}

void
SelectFormat::init(UErrorCode& status) {
    if (U_FAILURE(status)) {
      return;
    } 
    parsedValuesHash = NULL;
    pattern.remove();
}


void
SelectFormat::applyPattern(const UnicodeString& newPattern, UErrorCode& status) {
    if (U_FAILURE(status)) {
      return;
    } 

    this->pattern = newPattern;
    enum State{ startState, keywordState, pastKeywordState, phraseState};

    //Initialization
    UnicodeString keyword ;
    UnicodeString phrase ;
    UnicodeString* ptrPhrase ;
    int32_t braceCount = 0;

    delete parsedValuesHash;
    this->parsedValuesHash = NULL;
    parsedValuesHash = new Hashtable(TRUE, status);
    if (U_FAILURE(status)) {
        return;
    }

    //Process the state machine
    State state = startState;
    for (int32_t i = 0; i < pattern.length(); ++i) {
        //Get the character and check its type
        UChar ch = pattern.charAt(i);
        characterClass type;
        classifyCharacter(ch, type); 

        //Allow any character in phrase but nowhere else
        if ( type == tOther ) {
            if ( state == phraseState ){
                phrase += ch;
                continue;
            }else {
                status = U_PATTERN_SYNTAX_ERROR;
                return;
            }
        }

        //Process the state machine
        switch (state) {
            //At the start of pattern
            case startState:
                switch (type) {
                    case tSpace:
                        break;
                    case tStartKeyword:
                        state = keywordState;
                        keyword += ch;
                        break;
                    //If anything else is encountered, it's a syntax error
                    default:
                        status = U_PATTERN_SYNTAX_ERROR;
                        return;
                }//end of switch(type)
                break;

            //Handle the keyword state
            case keywordState:
                switch (type) {
                    case tSpace:
                        state = pastKeywordState;
                        break;
                    case tStartKeyword:
                    case tContinueKeyword:
                        keyword += ch;
                        break;
                    case tLeftBrace:
                        state = phraseState;
                        break;
                    //If anything else is encountered, it's a syntax error
                    default:
                        status = U_PATTERN_SYNTAX_ERROR;
                        return;
                }//end of switch(type)
                break;

            //Handle the pastkeyword state
            case pastKeywordState:
                switch (type) {
                    case tSpace:
                        break;
                    case tLeftBrace:
                        state = phraseState;
                        break;
                    //If anything else is encountered, it's a syntax error
                    default:
                        status = U_PATTERN_SYNTAX_ERROR;
                        return;
                }//end of switch(type)
                break;

            //Handle the phrase state
            case phraseState:
                switch (type) {
                    case tLeftBrace:
                        braceCount++;
                        phrase += ch;
                        break;
                    case tRightBrace:
                        //Matching keyword, phrase pair found
                        if (braceCount == 0){
                            //Check validity of keyword
                            if (parsedValuesHash->get(keyword) != NULL) {
                                status = U_DUPLICATE_KEYWORD;
                                return; 
                            }
                            if (keyword.length() == 0) {
                                status = U_PATTERN_SYNTAX_ERROR;
                                return;
                            }

                            //Store the keyword, phrase pair in hashTable
                            ptrPhrase = new UnicodeString(phrase);
                            parsedValuesHash->put( keyword, ptrPhrase, status);

                            //Reinitialize
                            keyword.remove();
                            phrase.remove();
                            ptrPhrase = NULL;
                            state = startState;
                        }

                        if (braceCount > 0){
                            braceCount-- ;
                            phrase += ch;
                        }
                        break;
                    default:
                        phrase += ch;
                }//end of switch(type)
                break;

            //Handle the  default case of switch(state)
            default:
                status = U_PATTERN_SYNTAX_ERROR;
                return;

        }//end of switch(state)
    }

    //Check if the state machine is back to startState
    if ( state != startState){
        status = U_PATTERN_SYNTAX_ERROR;
        return;
    }

    //Check if "other" keyword is present 
    if ( !checkSufficientDefinition() ) {
        status = U_DEFAULT_KEYWORD_MISSING;
    }
    return;
}

UnicodeString&
SelectFormat::format(const Formattable& obj,
                   UnicodeString& appendTo,
                   FieldPosition& pos,
                   UErrorCode& status) const
{
    if (U_FAILURE(status)) return appendTo;
    
    switch (obj.getType())
    {
    case Formattable::kString:
        return format(obj.getString(), appendTo, pos, status);
    default:
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return appendTo;
    }
}

UnicodeString&
SelectFormat::format(const UnicodeString& sInput,
                     UnicodeString& appendTo, 
                     FieldPosition& pos,
                     UErrorCode& status) const {

    //Check for the validity of the keyword
    if ( !checkValidKeyword(sInput) ){
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return appendTo;
    }

    if (parsedValuesHash == NULL) {
        status = U_INVALID_FORMAT_ERROR;
        return appendTo;
    }

    UnicodeString *selectedPattern = (UnicodeString *)parsedValuesHash->get(sInput);
    if (selectedPattern == NULL) {
        selectedPattern = (UnicodeString *)parsedValuesHash->get(SELECT_KEYWORD_OTHER);
    }
    
    return appendTo += *selectedPattern;
}

UnicodeString&
SelectFormat::toPattern(UnicodeString& appendTo) {
    return appendTo += pattern;
}

void
SelectFormat::classifyCharacter(UChar ch, characterClass& type) const{
    if ((ch >= CAP_A) && (ch <= CAP_Z)) {
        type = tStartKeyword;
        return;
    }
    if ((ch >= LOW_A) && (ch <= LOW_Z)) {
        type = tStartKeyword;
        return;
    }
    if ((ch >= U_ZERO) && (ch <= U_NINE)) {
        type = tContinueKeyword;
        return;
    }
    switch (ch) {
        case LEFTBRACE: 
            type = tLeftBrace;
            break;
        case RIGHTBRACE:
            type = tRightBrace;
            break;
        case SPACE:
        case TAB:
            type = tSpace;
            break;
        case HYPHEN:
        case LOWLINE:
            type = tContinueKeyword;
            break;
        default :
            type = tOther;
    }
}

UBool
SelectFormat::checkSufficientDefinition() {
    // Check that at least the default rule is defined.
    return (parsedValuesHash != NULL &&
           parsedValuesHash->get(SELECT_KEYWORD_OTHER) != NULL) ;
}

UBool
SelectFormat::checkValidKeyword(const UnicodeString& argKeyword ) const{
    int32_t len = argKeyword.length();
    if (len < 1){
        return FALSE;
    }
    characterClass type;
    classifyCharacter( argKeyword.charAt(0) , type); 
    if( type != tStartKeyword ){
        return FALSE;
    }

    for (int32_t i = 0; i < argKeyword.length(); ++i) {
        classifyCharacter( argKeyword.charAt(i) , type); 
        if( type != tStartKeyword && type != tContinueKeyword ){
            return FALSE;
        }
    }
    return TRUE;
}

Format* SelectFormat::clone() const
{
    return new SelectFormat(*this);
}

SelectFormat&
SelectFormat::operator=(const SelectFormat& other) {
    if (this != &other) {
        UErrorCode status = U_ZERO_ERROR;
        delete parsedValuesHash;
        pattern = other.pattern;
        copyHashtable(other.parsedValuesHash, status);
    }
    return *this;
}

UBool
SelectFormat::operator==(const Format& other) const {
    // This protected comparison operator should only be called by subclasses
    // which have confirmed that the other object being compared against is
    // an instance of a sublcass of SelectFormat.  THIS IS IMPORTANT.
    // Format::operator== guarantees that this cast is safe
    SelectFormat* fmt = (SelectFormat*)&other;
    Hashtable* hashOther = fmt->parsedValuesHash;
    if ( parsedValuesHash == NULL && hashOther == NULL)
        return TRUE;
    if ( parsedValuesHash == NULL || hashOther == NULL)
        return FALSE;
    if ( hashOther->count() != parsedValuesHash->count() ){
        return FALSE;
    }

    const UHashElement* elem = NULL;
    int32_t pos = -1;
    while ((elem = hashOther->nextElement(pos)) != NULL) {
        const UHashTok otherKeyTok = elem->key;
        UnicodeString* otherKey = (UnicodeString*)otherKeyTok.pointer;
        const UHashTok otherKeyToVal = elem->value;
        UnicodeString* otherValue = (UnicodeString*)otherKeyToVal.pointer; 

        UnicodeString* thisElemValue = (UnicodeString*)parsedValuesHash->get(*otherKey);
        if ( thisElemValue == NULL ){
            return FALSE;
        }
        if ( *thisElemValue != *otherValue){
            return FALSE;
        }
        
    }
    pos = -1;
    while ((elem = parsedValuesHash->nextElement(pos)) != NULL) {
        const UHashTok thisKeyTok = elem->key;
        UnicodeString* thisKey = (UnicodeString*)thisKeyTok.pointer;
        const UHashTok thisKeyToVal = elem->value;
        UnicodeString* thisValue = (UnicodeString*)thisKeyToVal.pointer;

        UnicodeString* otherElemValue = (UnicodeString*)hashOther->get(*thisKey);
        if ( otherElemValue == NULL ){
            return FALSE;
        }
        if ( *otherElemValue != *thisValue){
            return FALSE;
        }

    }
    return TRUE;
}

UBool
SelectFormat::operator!=(const Format& other) const {
    return  !operator==(other);
}

void
SelectFormat::parseObject(const UnicodeString& /*source*/,
                        Formattable& /*result*/,
                        ParsePosition& /*pos*/) const
{
    // TODO: not yet supported in icu4j and icu4c
}

void
SelectFormat::copyHashtable(Hashtable *other, UErrorCode& status) {
    if (other == NULL) {
        parsedValuesHash = NULL;
        return;
    }
    parsedValuesHash = new Hashtable(TRUE, status);
    if (U_FAILURE(status)){
        return;
    }

    int32_t pos = -1;
    const UHashElement* elem = NULL;

    // walk through the hash table and create a deep clone
    while ((elem = other->nextElement(pos)) != NULL){
        const UHashTok otherKeyTok = elem->key;
        UnicodeString* otherKey = (UnicodeString*)otherKeyTok.pointer;
        const UHashTok otherKeyToVal = elem->value;
        UnicodeString* otherValue = (UnicodeString*)otherKeyToVal.pointer;
        parsedValuesHash->put(*otherKey, new UnicodeString(*otherValue), status);
        if (U_FAILURE(status)){
            return;
        }
    }
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
