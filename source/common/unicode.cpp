/*
*******************************************************************************
* Copyright (C) 1996-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
//  FILE NAME : unicode.cpp
//
//  CREATED
//      Wednesday, December 11, 1996
//  
//  CHANGES
//      Wednesday, February 4,  1998
//      Changed logic in toUpperCase and toLowerCase in order
//      to avoid 0xFFFF to be returned when receiving 
//      confusing Unichar  to lowercase or to uppercase
//      (e.g. Letterlike symbols)
//
//  CHANGES BY
//  Bertramd A. DAMIBA
//
//  CREATED BY
//      Helena Shih
//
//  CHANGES
//      Thursday, April 15, 1999
//      Modified the definitions of all the functions
//      C++ Wrappers for Unicode
//  CHANGES BY
//      Madhu Katragadda
//   5/20/99     Madhu		Added the function u_getVersion()
//  07/09/99     stephen        Added definition for {MIN,MAX}_VALUE
//  11/22/99     aliu       Added MIN_RADIX, MAX_RADIX, digit, forDigit
//********************************************************************************************

#include "unicode.h"

#include "uchar.h"


const UChar Unicode::MIN_VALUE = 0x0000;
const UChar Unicode::MAX_VALUE = 0xFFFF;
const int8_t Unicode::MIN_RADIX = 2;
const int8_t Unicode::MAX_RADIX = 36;

Unicode::Unicode() 
{
}

Unicode::Unicode(const  Unicode&    other) 
{
}

Unicode::~Unicode() 
{
}

const Unicode&
Unicode::operator=(const    Unicode&    other)
{
    return *this;
}

// Checks if ch is a lower case letter.
bool_t
Unicode::isLowerCase(UChar ch) 
{
    return (u_islower(ch) );
}

// Checks if ch is a upper case letter.
bool_t
Unicode::isUpperCase(UChar ch) 
{
    return (u_isupper(ch) );
}

// Checks if ch is a title case letter; usually upper case letters.
bool_t
Unicode::isTitleCase(UChar ch) 
{
    return (u_istitle(ch) );
}

// Checks if ch is a decimal digit.
bool_t
Unicode::isDigit(UChar ch)
{
    return (u_isdigit(ch) );
}

// Checks if ch is a unicode character with assigned character type.
bool_t
Unicode::isDefined(UChar ch) 
{
    return (u_isdefined(ch) );
}


// Gets the character's linguistic directionality.
Unicode::EDirectionProperty
Unicode::characterDirection( UChar ch )
{   
    
    return ((EDirectionProperty)u_charDirection(ch) );
}

// Get the script associated with the character
Unicode::EUnicodeScript
Unicode::getScript(UChar ch)
{
    

    return ((EUnicodeScript) u_charScript(ch) );
}

// Checks if the Unicode character is a base form character that can take a diacritic.
bool_t
Unicode::isBaseForm(UChar ch)
{
    return (u_isbase(ch) );

}

// Checks if the Unicode character is a control character.
bool_t
Unicode::isControl(UChar ch)
{
    return( u_iscntrl(ch) );
}

// Checks if the Unicode character is printable.
bool_t
Unicode::isPrintable(UChar ch)
{
    return( u_isprint(ch) );
}

// Checks if the Unicode character is a letter.
bool_t
Unicode::isLetter(UChar ch) 
{
    return(u_isalpha(ch) );
}

// Checks if the Unicode character can start a Java identifier.
bool_t 
Unicode::isJavaIdentifierStart(UChar ch)
{
    return( u_isJavaIDStart(ch) );
}

// Checks if the Unicode character can be a Java identifier part other than starting the
// identifier.
bool_t 
Unicode::isJavaIdentifierPart(UChar ch)
{
    return (u_isJavaIDPart(ch) );
}

// Checks if the Unicode character can start a Unicode identifier.
bool_t 
Unicode::isUnicodeIdentifierStart(UChar ch)
{
    return(u_isIDStart(ch));
}

// Checks if the Unicode character can be a Unicode identifier part other than starting the
// identifier.
bool_t 
Unicode::isUnicodeIdentifierPart(UChar ch)
{
    return (u_isIDPart(ch) );
}

// Checks if the Unicode character can be ignorable in a Java or Unicode identifier.
bool_t 
Unicode::isIdentifierIgnorable(UChar ch)
{
    return( u_isIDIgnorable(ch) );
}

// Transforms the Unicode character to its lower case equivalent.
UChar         
Unicode::toLowerCase(UChar ch) 
{
    return (u_tolower(ch) );
    
}
    
// Transforms the Unicode character to its upper case equivalent.
UChar 
Unicode::toUpperCase(UChar ch) 
{
    return(u_toupper(ch) );
}

// Transforms the Unicode character to its title case equivalent.
UChar 
Unicode::toTitleCase(UChar ch)
{
    return(u_totitle(ch) );
}

// Checks if the Unicode character is a space character.
bool_t
Unicode::isSpaceChar(UChar ch) 
{
    return(u_isspace(ch) );
}

// Gets if the Unicode character's character property.
int8_t
Unicode::getType(UChar ch)
{
    return(u_charType(ch) );
}



// Gets table cell width of the Unicode character.
uint16_t
Unicode::getCellWidth(UChar ch)
{
    return (u_charCellWidth(ch) );
}

int32_t            
Unicode::digitValue(UChar ch)
{
    return (u_charDigitValue(ch) );
}

int8_t 
Unicode::digit(UChar ch, int8_t radix) {
    int8_t value = -1;
    if (radix >= MIN_RADIX && radix <= MAX_RADIX) {
        value = (int8_t) u_charDigitValue(ch);
        if (value < 0) {
            if (ch >= (UChar)'A' && ch <= (UChar)'Z') {
                value = ch - ((UChar)'A' - 10);
            } else if (ch >= (UChar)'a' && ch <= (UChar)'z') {
                value = ch - ((UChar)'a' - 10);
            }
        }
    }
    return (value < radix) ? value : -1;
}

UChar
Unicode::forDigit(int32_t digit, int8_t radix) {
    if ((radix < MIN_RADIX) || (radix > MAX_RADIX) ||
        (digit < 0) || (digit >= radix)) {
        return (UChar)0;
    }
    return (UChar)(((digit < 10) ? (UChar)'0' : ((UChar)'a' - 10))
                   + digit);
}

const char*
Unicode::getVersion()
{
	return (u_getVersion() );
}


