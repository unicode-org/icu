/*******************************************************************************
 * Copyright (C) 1996-1999, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
//=============================================================================
//
// File ptnentry.cpp
//
// Contains PatternEntry, an internal class used by MergeCollation to store
// one collation element from a pattern.
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date         Name          Description
// 04/23/99      stephen       Removed EDecompositionMode, merged with
//                             Normalizer::EMode
//                             Removed character literals.
//=============================================================================

#include "ptnentry.h"

#include "unicode/unicode.h"
#include "unicode/coll.h"
#include "unicode/normlzr.h"


// static member initialization
const int32_t PatternEntry::RESET = -2;
const int32_t PatternEntry::UNSET = -1;

// ===== privates =====

PatternEntry::PatternEntry() 
  : strength(PatternEntry::UNSET)
{
}

PatternEntry::PatternEntry(const    PatternEntry&   other)
  : strength(other.strength), chars(other.chars), extension(other.extension)
{
}

PatternEntry::PatternEntry(int32_t newStrength,
               const UnicodeString& newChars,
               const UnicodeString& newExtensions, 
               Normalizer::EMode decompMode)
  : strength(newStrength), extension(newExtensions)
{
  // Normalize the characters in the new entry.  Find occurances of all 
  // decomposed characters and normalize them.  By "normalize",
  // we mean that all precomposed Unicode characters must be converted into
  // a base character and one or more combining characters (such as accents).
  // When there are multiple combining characters attached to a base character,
  // the combining characters must be in their canonical order
  //
  UErrorCode status = U_ZERO_ERROR;
  Normalizer::normalize(newChars, decompMode, 0, chars, status);
  if (U_FAILURE(status)) {
    chars = newChars;
  }
}

PatternEntry::~PatternEntry() {
}

const PatternEntry&
PatternEntry::operator=(const   PatternEntry& other)
{
  if (this != &other) {
    strength = other.strength;        
    chars = other.chars;
    extension = other.extension;
  }
  return *this;
}

/**
 * Gets the current extension, quoted
 * This is useful when constructing a user-readable string representing
 * a pattern.
 */
void PatternEntry::appendQuotedExtension(UnicodeString& toAddTo) const {
  appendQuoted(extension,toAddTo);
}

/**
 * Gets the current chars, quoted
 * This is useful when constructing a user-readable string representing
 * a pattern.
 */
void PatternEntry::appendQuotedChars(UnicodeString& toAddTo) const {
  appendQuoted(chars,toAddTo);
}

bool_t PatternEntry::equals(const PatternEntry& other) const {
  bool_t result = ((strength == other.strength) &&
                   (chars == other.chars) &&
                   (extension == other.extension));
  return result;
}

/**
 * For debugging.
 */
UnicodeString& 
PatternEntry::toString(UnicodeString& result) const 
{
  addToBuffer(result, TRUE, FALSE, NULL);
  return result;
}

int32_t 
PatternEntry::getStrength() const
{
  return strength;
}

const UnicodeString&    
PatternEntry::getExtension(UnicodeString& ext) const
{
  ext = extension;
  return ext;
}

const UnicodeString&    
PatternEntry::getChars(UnicodeString& result) const
{
  result = chars;
  return result;
}

/*
 Add the entry in textual form into the toAddTo buffer.
 */
void PatternEntry::addToBuffer(UnicodeString& toAddTo,
                   bool_t showExtension,
                   bool_t showWhiteSpace,
                   const PatternEntry* lastEntry) const
{
  if (showWhiteSpace && toAddTo.length() > 0)
    // Adds new line before each primary strength entry.
    if (strength == Collator::PRIMARY || lastEntry != NULL)
      toAddTo += 0x000A/*'\n'*/;
    else
      toAddTo += 0x0020/*' '*/;
  if (lastEntry != NULL) {
    toAddTo += 0x0026/*'&'*/;
    if (showWhiteSpace)
      toAddTo += 0x0020/*' '*/;
    lastEntry->appendQuotedChars(toAddTo);
    appendQuotedExtension(toAddTo);
    if (showWhiteSpace)
      toAddTo += 0x0020/*' '*/;
  }
  // Check the strength for the correct symbol to append
  switch (strength) {
  case Collator::IDENTICAL:   toAddTo += 0x003D/*'='*/; break;
  case Collator::TERTIARY:    toAddTo += 0x002C/*','*/; break;
  case Collator::SECONDARY:   toAddTo += 0x003B/*';'*/; break;
  case Collator::PRIMARY:     toAddTo += 0x003C/*'<'*/; break;
  case PatternEntry::RESET:   toAddTo += 0x0026/*'&'*/; break;
  case PatternEntry::UNSET:   toAddTo += 0x003F/*'?'*/; break;
  }
  if (showWhiteSpace)
    toAddTo += 0x0020/*' '*/;
  appendQuoted(chars,toAddTo);
  // If there's an expending char and needs to be shown, 
  // append that after the entry
  if (showExtension && extension.length() != 0) {
    toAddTo += 0x002F/*'/'*/;
    appendQuoted(extension,toAddTo);
  }
}

// Append a string to a pattern buffer, adding quotes if necessary
void PatternEntry::appendQuoted(const UnicodeString& chars, UnicodeString& toAddTo) {
  bool_t inQuote = FALSE;
  UChar ch = chars[T_INT32(0)];
  if (Unicode::isSpaceChar(ch)) {
    inQuote = TRUE;
    toAddTo += 0x0027/*'\''*/;
  } else if (isSpecialChar(ch)) {
    inQuote = TRUE;
    toAddTo += 0x0027/*'\''*/;
  } else {
    switch (ch) {
    case 0x0010: case 0x000C/*'\f'*/: 
    case 0x000D/*'\r'*/: case 0x0009/*'\t'*/: 
    case 0x000A/*'\n'*/: case 0x0040/*'@'*/:
      inQuote = TRUE;
      toAddTo += 0x0027/*'\''*/;
      break;
    case 0x0027/*'\''*/:
      inQuote = TRUE;
      toAddTo += 0x0027/*'\''*/;
      break;
    default:
      if (inQuote) {
    inQuote = FALSE; toAddTo += 0x0027/*'\''*/;
      }
      break;
    }
  }
  toAddTo += chars;
  if (inQuote)
    toAddTo += 0x0027/*'\''*/;
}

PatternEntry::Parser::Parser(const UnicodeString &pattern, 
                             Normalizer::EMode decompMode)
  : pattern(pattern), index(0), 
    fDecompMode(decompMode), newChars(), newExtensions()
{
}

PatternEntry::Parser::Parser(const Parser &that)
  : pattern(that.pattern), index(that.index), fDecompMode(that.fDecompMode),
    newChars(that.newChars), newExtensions(that.newExtensions)
{
}

PatternEntry::Parser::~Parser()
{
}

PatternEntry::Parser &PatternEntry::Parser::operator=(const Parser &that)
{
  if (this != &that)
  {
    this->pattern = that.pattern;
    this->index = that.index;
    this->fDecompMode = that.fDecompMode;
    this->newChars = that.newChars;
    this->newExtensions = that.newExtensions;
  }
    
  return *this;
}

PatternEntry *PatternEntry::Parser::next(UErrorCode &status)
{
  int32_t newStrength = PatternEntry::UNSET;
  bool_t inChars = TRUE;
  bool_t inQuote = FALSE;

  newChars.remove();
  newExtensions.remove();

  while (index < pattern.length())
    {
      UChar ch = pattern[index];

      if (inQuote)
    {
      if (ch == 0x0027/*'\''*/)
        {
          inQuote = FALSE;
        }
      else
        {
          if ((newChars.length() == 0) || inChars)
        {
          newChars += ch;
        }
          else
        {
          newExtensions += ch;
        }
            }
        }
      else
    {
      // Sets the strength for this entry
      switch (ch)
        {
        case 0x003D/*'='*/ : 
          if (newStrength != PatternEntry::UNSET)
        {
          goto EndOfLoop;
        }

          newStrength = Collator::IDENTICAL;
          break;

        case 0x002C/*','*/:  
          if (newStrength != PatternEntry::UNSET)
        {
          goto EndOfLoop;
        }

          newStrength = Collator::TERTIARY;
          break;

        case  0x003B/*';'*/:
          if (newStrength != PatternEntry::UNSET)
        {
          goto EndOfLoop;
        }

          newStrength = Collator::SECONDARY;
          break;

        case 0x003C/*'<'*/:  
          if (newStrength != PatternEntry::UNSET)
        {
          goto EndOfLoop;
        }

          newStrength = Collator::PRIMARY;
          break;

        case 0x0026/*'&'*/:  
          if (newStrength != PatternEntry::UNSET)
        {
          goto EndOfLoop;
        }

          newStrength = PatternEntry::RESET;
          break;

          // Ignore the white spaces
        case 0x0009/*'\t'*/:
        case 0x000C/*'\f'*/:
        case 0x000D/*'\r'*/:
        case 0x000A/*'\n'*/:
        case 0x0020/*' '*/:  
          break; // skip whitespace TODO use Unicode

        case 0x002F/*'/'*/:
                // This entry has an extension.
          inChars = FALSE;
          break;

        case 0x0027/*'\''*/:
          inQuote = TRUE;
          ch = pattern[++index];

          if (newChars.length() == 0)
        {
          newChars += ch;
        }
          else if (inChars)
        {
          newChars += ch;
        }
          else
        {
          newExtensions += ch;
        }

          break;

        default:
          if (newStrength == PatternEntry::UNSET)
        {
          status = U_INVALID_FORMAT_ERROR;
          return NULL;
        }

          if (isSpecialChar(ch) && (inQuote == FALSE))
        {
          status = U_INVALID_FORMAT_ERROR;
          return NULL;
        }

          if (inChars)
        {
          newChars += ch;
        }
          else
        {
          newExtensions += ch;
        }

          break;
        }
    }

      if (newChars.isBogus() || newExtensions.isBogus())
    {
      status = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
        }

      index += 1;
    }

 EndOfLoop:
  if (newStrength == PatternEntry::UNSET)
    {
      return NULL;
    }

  if (newChars.length() == 0)
    {
      status = U_INVALID_FORMAT_ERROR;
      return NULL;
    }

  return new PatternEntry(newStrength, newChars, newExtensions, fDecompMode);
}

// Check if the character is a special character.  A special character
// would be meaningful in the rule only if quoted, otherwise it's used
// as a denotation for strength or merging symbols.
bool_t PatternEntry::isSpecialChar(UChar ch)
{
  return (((ch <= 0x002F) && (ch >= 0x0020)) ||
      ((ch <= 0x003F) && (ch >= 0x003A)) ||
      ((ch <= 0x0060) && (ch >= 0x005B)) ||
      ((ch <= 0x007E) && (ch >= 0x007B)));
}
