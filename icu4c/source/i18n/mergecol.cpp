/******************************************************************************
 * COPYRIGHT:                                                               
 *  (C) Copyright Taligent, Inc., 1996
 *  (C) Copyright IBM Corp. 1996-1998
 *  Licensed Material - Program-Property of IBM - All Rights Reserved.
 *  US Government Users Restricted Rights - Use, duplication, or disclosure
 *  restricted by GSA ADP Schedule Contact with IBM Corp.
 *
 ******************************************************************************
 */
//=============================================================================
//
// File mergecol.cpp
//
// Contains MergeCollation.  This classes job is to take one or more
// strings that represent the orderings in a collation, in the form 
// "a , A < b , B ....".  MergeCollation parses the string into a list of
// PatternEntry objects that are sorted by their position in the collation
// ordering.  The input string is allowed to have elements out of order, e.g.
// "... b < c < d < e .....   c < ch".  After being parsed by MergeCollation,
// the pattern entries will be in the proper order: "b", "c", "ch", "d", "e"
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date        Name        Description
//  3/5/97      mark        Cleaned up fixEntry().  Added constants BYTEPOWER
//                          and BYTEMASK to replace BYTESIZE.
//  6/17/97     helena      In getPattern, added the queue-up list for entries 
//                          with the same extension chars.
//  6/23/97     helena      Adding comments to make code more readable.
//  8/13/98     erm         Synched up with 1.2 version of MergeCollation.java
// 04/23/99     stephen     Removed EDecompositionMode, merged with
//                          Normalizer::EMode
//=============================================================================

#include "mergecol.h"

#include "tables.h"

#ifdef _DEBUG
#include "unistrm.h"
#endif

const   int32_t         MergeCollation::BITARRAYSIZE = 8192;
const   uint8_t         MergeCollation::BITARRAYMASK = 0x1;
const   int32_t         MergeCollation::BYTEPOWER = 3;
const   int32_t         MergeCollation::BYTEMASK = (1 << BYTEPOWER) - 1;

 /**
 * Creates from a pattern.
 * If the input pattern is incorrect, error code will be set.
 * @param pattern the merge collation pattern
 * @param success error code input/output parameter.
 */
MergeCollation::MergeCollation(const    UnicodeString&  pattern,
                               Normalizer::EMode decompMode,
                   UErrorCode&      status)
    : lastEntry(NULL), saveEntry(NULL)
{
  patterns = new VectorOfPointersToPatternEntry();
  
  
  if (patterns == NULL)
    {
      status = U_MEMORY_ALLOCATION_ERROR;
      return;
    }

  statusArray = new uint8_t[BITARRAYSIZE];

  if (statusArray == NULL)
    {
      delete patterns;
      status = U_MEMORY_ALLOCATION_ERROR;
      return;
    }

  int32_t i;
  for (i = 0; i < BITARRAYSIZE; i += 1)
    {
      statusArray[i] = 0;
    }

  setPattern(pattern, decompMode, status);
  if (FAILURE(status))
    {
      delete [] statusArray;
      statusArray = NULL;
    }
}

/**
 * Copy constructor
 * @param other the source merge collation object to be constructed with
 */
MergeCollation::MergeCollation(const    MergeCollation& other)
    : lastEntry(NULL), saveEntry(NULL)
{
  // This copy ctor does a deep copy - it duplicates the PatternEntry
  // objects as well as the vector object
  patterns = new VectorOfPointersToPatternEntry(*other.patterns);

  int32_t i;
  statusArray = new uint8_t[BITARRAYSIZE];
  for (i = 0; i < BITARRAYSIZE; i += 1)
    {
      statusArray[i] = other.statusArray[i];
    }
}

// Assignment operator.  Does a deep copy.
const MergeCollation&
MergeCollation::operator=(const MergeCollation& other)
{
  if (this != &other)
  {
    *patterns = *other.patterns;
    lastEntry = 0;
    saveEntry = 0;

    int32_t i;
    for (i = 0; i < BITARRAYSIZE; i += 1)
    {
      statusArray[i] = other.statusArray[i];
    }
  }

  return *this;
}

/**
 * Destructor
 */
MergeCollation::~MergeCollation()
{
  delete patterns;
  delete [] statusArray;
}

/**
 * recovers current pattern as a string.
 * Basically, this runs through the PatternEntry array and outputs
 * @param result the string into which the pattern is recovered
 * the proper string for each element.
 * @param withWhiteSpace puts spacing around the entries, and \n
 * before & and <
 */
UnicodeString&
MergeCollation::getPattern(UnicodeString& result, bool_t withWhiteSpace) const
{

  int32_t i;
  PatternEntry *tmp = NULL;
  VectorOfPointer *extList = NULL;

  result.remove();

  for (i = 0; i < patterns->size(); i += 1)
    {
      PatternEntry* entry = patterns->at(i);

      if (entry != NULL)
    {
      // if the entry is an expanding ligature, queue up the entries until
      // the last same ligature has been processed.
      if (entry->extension.size() != 0)
        {
          if (extList == NULL)
        {
          extList = new VectorOfPointer();
        }

          extList->atInsert(extList->size(), (const void*&)entry);
            }
      else
        {
          // Process the queue-up list in reverse order to get the correct
          // pattern.
          if (extList != NULL)
        {
          const PatternEntry *last = findLastWithNoExtension(i - 1);

          for (int32_t j = extList->size() - 1; j >= 0 ; j -= 1)
            {
              tmp = (PatternEntry*)(extList->at(j));
              tmp->addToBuffer(result, FALSE, withWhiteSpace, last);
                    }

          delete extList;
          extList = NULL;
                }

          entry->addToBuffer(result, FALSE, withWhiteSpace, NULL);
            }
        }
    }

  // Complete the queue-up list if it isn't empty
  if (extList != NULL)
    {
      const PatternEntry *last = findLastWithNoExtension(i - 1);

      for (int32_t j = extList->size() - 1; j >= 0 ; j -= 1)
    {
      tmp = (PatternEntry*)(extList->at(j));
      tmp->addToBuffer(result, FALSE, withWhiteSpace, last);
        }

      delete extList;
    }


  return result;
} 

/**
 * emits the pattern for collation builder.
 * @param result the string into which the pattern is recovered
 * @param withWhiteSpace puts spacing around the entries, and \n
 * before & and <
 * @return emits the string in the format understable to the collation
 * builder.
 */
UnicodeString&
MergeCollation::emitPattern(UnicodeString& result, bool_t withWhiteSpace) const 
{
  int32_t i;

  result.remove();

  for (i = 0; i < patterns->size(); i += 1)
    {
      PatternEntry *entry = (PatternEntry *)patterns->at(i);

      if (entry != NULL)
    {
      entry->addToBuffer(result, TRUE, withWhiteSpace, NULL);
        }
    }

  return result;
}

/**
 * sets the pattern.
 */
void MergeCollation::setPattern(const   UnicodeString&  pattern,
                                Normalizer::EMode decompMode,
                UErrorCode&      success)
{
  if (FAILURE(success))
    {
      return;
    }

  patterns->clear();

  addPattern(pattern, decompMode, success);
  if (FAILURE(success))
    {
      delete patterns;
      patterns = NULL;
    }
}

/**
 * adds a pattern string to the current list of patterns
 * @param pattern the new pattern to be added
 */
void MergeCollation::addPattern(const   UnicodeString&  pattern,
                                Normalizer::EMode decompMode,
                UErrorCode&      success)
{
  if (FAILURE(success) || (pattern.size() == 0))
    {
      return;
    }

  PatternEntry::Parser *parser = new PatternEntry::Parser(pattern, decompMode);
    
  PatternEntry *entry = parser->next(success);

  while (entry != NULL)
    {
      if (FAILURE(success))
    {
      delete entry;
      break;
    }

      fixEntry(entry, success);

      if (FAILURE(success))
    {
      delete entry;
      break;
    }

      entry = parser->next(success);
    }
}

/**
 * gets count of separate entries
 * @return the size of pattern entries
 */
int32_t 
MergeCollation::getCount() const {
  return patterns->size();
}   

/**
 * gets count of separate entries
 * @param index the offset of the desired pattern entry
 * @return the requested pattern entry
 */
const PatternEntry* MergeCollation::getItemAt(UTextOffset index) const {
  return patterns->at(index);
}

// Find the last no-extension entry.
const PatternEntry* MergeCollation::findLastWithNoExtension(int32_t i) const {
  for (--i;i >= 0; --i) {
    PatternEntry* entry = patterns->at(i);
    if ((entry != 0) && (entry->extension.size() == 0)) {
      return entry;
    }
  }
  return 0;
}

// Add a new PatternEntry to this MergeCollation's ordered list
// of entries.
//
// If the strength is RESET, then just change the lastEntry to
// be the current. (If the current is not in patterns, signal an error).
//
// If not, then remove the current entry, and add it after lastEntry
// (which is usually at the end).
//
void MergeCollation::fixEntry(PatternEntry* newEntry,
                              UErrorCode&    success)
{
  UnicodeString excess;
  bool_t changeLastEntry = TRUE;

  if (newEntry->strength != PatternEntry::RESET)
    {
      int32_t oldIndex = -1;

      // Use statusArray to mark if a unicode character has been
      // added in the table or not.  The same later entry will 
      // replace the previous one.  This will improve the single
      // char entries dramatically which is the majority of the 
      // entries.
      if (newEntry->chars.size() == 1)
    {
      UChar c = newEntry->chars[0];
      int32_t statusIndex = c >> BYTEPOWER;
      uint8_t bitClump = statusArray[statusIndex];
      uint8_t setBit = BITARRAYMASK << (c & BYTEMASK);

      if (bitClump != 0 && (bitClump & setBit) != 0)
            {
          int32_t i = 0;

          // Find the previous entry with the same key
          for (i = patterns->size() - 1; i >= 0; i -= 1)
        {
          PatternEntry *entry = patterns->at(i);

          if ((entry != 0) &&
              (entry->chars == newEntry->chars))
            {
              oldIndex = i;
              break;
                    }
                }
        }
      else
        {
          // We're going to add an element that starts with this
          // character, so go ahead and set its bit.
          statusArray[statusIndex] = (uint8_t)(bitClump | setBit);
            } 
        }
      else
    {
      oldIndex = patterns->lastIndexOf(newEntry);
        }

      if (oldIndex != -1)
    {
      PatternEntry *p = patterns->orphanAt(oldIndex);
      delete p;
        }

      // Find the insertion point for the new entry.
      int32_t lastIndex = findLastEntry(lastEntry, excess, success);

      if (FAILURE(success))
    {
      return;
    }

      // Do not change the last entry if the new entry is a expanding character
      if (excess.size() != 0)
    {
      // newEntry.extension = excess + newEntry.extensions;
      newEntry->extension.insert(0, excess);
      if (lastIndex != patterns->size())
        {
          lastEntry = saveEntry;
          changeLastEntry = FALSE;
            }
        }

      // Add the entry at the end or insert it in the middle
      if (lastIndex == patterns->size())
    {
      patterns->atPut(lastIndex, newEntry);
      saveEntry = newEntry;

        }
      else
    {
      patterns->atInsert(lastIndex, newEntry);  // add at end
        }
    }
    
  if (changeLastEntry)
    {
      lastEntry = newEntry;
    }
}

int32_t
MergeCollation::findLastEntry(const PatternEntry*   lastEntry,
                  UnicodeString&  excess,
                  UErrorCode&      success) const
{
  if (FAILURE(success))
    {
      return -1;
    }

  if (lastEntry == NULL)
    {
      return 0;
    }
  else if (lastEntry->strength != PatternEntry::RESET)
    {
      int32_t oldIndex = -1;

      // If the last entry is a single char entry and has been installed, 
      // that means the last index is the real last index.
      if (lastEntry->chars.size() == 1)
    {
      int32_t index = lastEntry->chars[0] >> BYTEPOWER;

      if ((statusArray[index] & 
           (uint8_t)(BITARRAYMASK << (lastEntry->chars[0] & BYTEMASK))) != 0)
        {
          oldIndex = patterns->lastIndexOf(lastEntry);
            }
        }
      else
    {
      oldIndex = patterns->lastIndexOf(lastEntry);
        }

      // must exist!
      if (oldIndex == -1)
    {
      success = U_INVALID_FORMAT_ERROR;
      return oldIndex;
        }

      return oldIndex + 1;
    }
  else
    {
      // We're doing a reset, i.e. inserting a new ordering at the position
      // just after the entry corresponding to lastEntry's first character
      int32_t i;

      // Search backwards for string that contains this one;
      // most likely entry is last one
      for (i = patterns->size() - 1; i >= 0; i -= 1)
    {
      PatternEntry* entry = patterns->at(i);
      UnicodeString buffer;
      if (entry != 0)
        {
          //
          // Look for elements with the same beginning key.  The extra
          // characters will be the expanding portion.  This handles cases like
          // "& Question-mark < '?'".  We find the existing PatternEntry that matches
          // the longest possible substring of "Question-mark", which will probably
          // be 'Q'.  We save the characters that didn't match ("uestion-mark" in
          // this case), and then return the next index.
          //
          if (entry->chars.compareBetween(0, entry->chars.size(),
                          lastEntry->chars,0,entry->chars.size()) == 0)
        {
          lastEntry->chars.extractBetween(entry->chars.size(), 
                          lastEntry->chars.size(),
                          buffer);
          excess += buffer;
          break;
                }
        }
        }

      if (i == -1)
    {
      success = U_INVALID_FORMAT_ERROR;
      return i;
        }

      return i + 1;
    }
}
