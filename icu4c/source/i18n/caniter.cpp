/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu/source/i18n/Attic/caniter.cpp,v $ 
 * $Date: 2002/02/27 21:47:04 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

#include "hash.h"
#include "unicode/caniter.h"

/**
 * This class allows one to iterate through all the strings that are canonically equivalent to a given
 * string. For example, here are some sample results:
Results for: {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
1: \u0041\u030A\u0064\u0307\u0327
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
2: \u0041\u030A\u0064\u0327\u0307
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D}{COMBINING CEDILLA}{COMBINING DOT ABOVE}
3: \u0041\u030A\u1E0B\u0327
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D WITH DOT ABOVE}{COMBINING CEDILLA}
4: \u0041\u030A\u1E11\u0307
 = {LATIN CAPITAL LETTER A}{COMBINING RING ABOVE}{LATIN SMALL LETTER D WITH CEDILLA}{COMBINING DOT ABOVE}
5: \u00C5\u0064\u0307\u0327
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
6: \u00C5\u0064\u0327\u0307
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D}{COMBINING CEDILLA}{COMBINING DOT ABOVE}
7: \u00C5\u1E0B\u0327
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D WITH DOT ABOVE}{COMBINING CEDILLA}
8: \u00C5\u1E11\u0307
 = {LATIN CAPITAL LETTER A WITH RING ABOVE}{LATIN SMALL LETTER D WITH CEDILLA}{COMBINING DOT ABOVE}
9: \u212B\u0064\u0307\u0327
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D}{COMBINING DOT ABOVE}{COMBINING CEDILLA}
10: \u212B\u0064\u0327\u0307
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D}{COMBINING CEDILLA}{COMBINING DOT ABOVE}
11: \u212B\u1E0B\u0327
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D WITH DOT ABOVE}{COMBINING CEDILLA}
12: \u212B\u1E11\u0307
 = {ANGSTROM SIGN}{LATIN SMALL LETTER D WITH CEDILLA}{COMBINING DOT ABOVE}
 *<br>Note: the code is intended for use with small strings, and is not suitable for larger ones,
 * since it has not been optimized for that situation.
 *@author M. Davis
 *@draft
 */

//#include <stdio.h>


//CanonicalIterator::SAFE_START = NULL;
//CanonicalIterator::AT_START = NULL;

static UnicodeSet *SAFE_START = NULL; // = new UnicodeSet();
//private static CharMap AT_START = new CharMap();
static Hashtable *AT_START = NULL;

#if 0
static UBool PROGRESS = FALSE;

#include "unicode/translit.h"

UErrorCode status = U_ZERO_ERROR;

// Just for testing - remove, not thread safe. 
static const char* UToS(const UnicodeString &source) {
  static char buffer[256];
  buffer[source.extract(0, source.length(), buffer)] = 0;
  return buffer;
}

static const UnicodeString &Tr(const UnicodeString &source) {
  static Transliterator *NAME = Transliterator::createInstance("name", UTRANS_FORWARD, status);
  static UnicodeString result;
  result = source;
  NAME->transliterate(result);
  return result;
}
#endif

// public

/**
 *@param source string to get results for
 */
CanonicalIterator::CanonicalIterator(UnicodeString source, UErrorCode status) :
    pieces(NULL),
    pieces_lengths(NULL),
    current(NULL)
{
  initStaticData(status);
    setSource(source, status);
}

CanonicalIterator::~CanonicalIterator() {
  cleanPieces();
}

void CanonicalIterator::cleanPieces() {
  int32_t i = 0;
  if(pieces != NULL) {
    for(i = 0; i < pieces_length; i++) {
      if(pieces[i] != NULL) {
        delete[] pieces[i];
      }
    }
    delete[] pieces;
    pieces = NULL;
    if(pieces_lengths != NULL) {
      delete[] pieces_lengths;
    }
    pieces_lengths = NULL;
    if(current != NULL) {
      delete[] current;
    }
    current = NULL;
  }
}

/**
 *@return gets the source: NOTE: it is the NFD form of source
 */
UnicodeString CanonicalIterator::getSource() {
  return source;
}

/**
 * Resets the iterator so that one can start again from the beginning.
 */
void CanonicalIterator::reset() {
    done = false;
    for (int i = 0; i < current_length; ++i) {
        current[i] = 0;
    }
}

/**
 *@return the next string that is canonically equivalent. The value null is returned when
 * the iteration is done.
 */
UnicodeString CanonicalIterator::next() {
  int32_t i = 0;
    if (done) return "";
    
    // construct return value
    
    buffer.truncate(0); //buffer.setLength(0); // delete old contents
    for (i = 0; i < pieces_length; ++i) {
        buffer.append(pieces[i][current[i]]);
    }
    //String result = buffer.toString(); // not needed
    
    // find next value for next time
    
    for (i = current_length - 1; ; --i) {
        if (i < 0) {
            done = TRUE;
            break;
        }
        current[i]++;
        if (current[i] < pieces_lengths[i]) break; // got sequence
        current[i] = 0;
    }
    return buffer;
}

/**
 *@param set the source string to iterate against. This allows the same iterator to be used
 * while changing the source string, saving object creation.
 */
void CanonicalIterator::setSource(UnicodeString newSource, UErrorCode status) {
    Normalizer::normalize(newSource, UNORM_NFD, 0, source, status);
    done = FALSE;
    
    cleanPieces();

    UnicodeString *list = new UnicodeString[source.length()];
    int32_t list_length = 0;
    UChar32 cp = 0;
    int32_t start = 0;
    int32_t i = 1;
    // find the segments
    // This code iterates through the source string and 
    // extracts segments that end up on a codepoint that
    // doesn't start any decompositions. (Analysis is done
    // on the NFD form - see above).
    for (; i < source.length(); i += UTF16_CHAR_LENGTH(cp)) {
        cp = source.char32At(i);
        if (SAFE_START->contains(cp)) {
            source.extract(start, i, list[list_length++]); // add up to i
            start = i;
        }
    }
    source.extract(start, i, list[list_length++]); // add last one

    
    // allocate the arrays, and find the strings that are CE to each segment
    pieces = new UnicodeString*[list_length];
    pieces_length = list_length;
    pieces_lengths = new int32_t[list_length];

    current = new int32_t[list_length];
    current_length = list_length;
    for (i = 0; i < current_length; i++) {
      current[i] = 0;
    }
    // for each segment, get all the combinations that can produce 
    // it after NFD normalization
    for (i = 0; i < pieces_length; ++i) {
        //if (PROGRESS) printf("SEGMENT\n");
        pieces[i] = getEquivalents(list[i], pieces_lengths[i], status);
    }

    delete[] list;
}

/**
 * Dumb recursive implementation of permutation. 
 * TODO: optimize
 * @param source the string to find permutations for
 * @return the results in a set.
 */
Hashtable *CanonicalIterator::permute(UnicodeString &source, UErrorCode status) {
    //if (PROGRESS) printf("Permute: %s\n", UToS(Tr(source)));
    int32_t i = 0;

    Hashtable *result = new Hashtable(FALSE, status);
    result->setValueDeleter(uhash_deleteUnicodeString);
    
    // optimization:
    // if zero or one character, just return a set with it
    // we check for length < 2 to keep from counting code points all the time
    //if (source.length() <= 2 && UTF16_CHAR_LENGTH(source.char32At(0)) <= 1) {
    if (source.length() < 2 || (source.length() == 2 && UTF16_CHAR_LENGTH(source.char32At(0)) > 1)) {
      UnicodeString *toPut = new UnicodeString(source);
      result->put(source, toPut, status); 
      return result;
    }
    
    // otherwise iterate through the string, and recursively permute all the other characters
    UChar32 cp;
    for (i = 0; i < source.length(); i += UTF16_CHAR_LENGTH(cp)) {
        cp = source.char32At(i);
        const UHashElement *ne = NULL;
        int32_t el = -1;
        UnicodeString subPermuteString = source;
        
        // see what the permutations of the characters before and after this one are
        //Hashtable *subpermute = permute(source.substring(0,i) + source.substring(i + UTF16.getCharCount(cp)));
        Hashtable *subpermute = permute(subPermuteString.replace(i, UTF16_CHAR_LENGTH(cp), NULL, 0), status);
        // The upper replace is destructive. The question is do we have to make a copy, or we don't care about the contents 
        // of source at this point.
        
        // prefix this character to all of them
        ne = subpermute->nextElement(el);
        while (ne != NULL) {
          UnicodeString *permRes = (UnicodeString *)(ne->value.pointer);
          UnicodeString *chStr = new UnicodeString(cp);
            chStr->append(*permRes); //*((UnicodeString *)(ne->value.pointer));
            //if (PROGRESS) printf("  Piece: %s\n", UToS(*chStr));
            result->put(*chStr, chStr, status);
            ne = subpermute->nextElement(el);
        }
        delete subpermute;
    }
    return result;
}

static UBool U_CALLCONV
_enumCategoryRangeSAFE_STARTsetup(const void *context, UChar32 start, UChar32 limit, UCharCategory type) {
  int32_t cc = 0;
  // TODO: use a switch that will automatically add all the unassigned, lead surrogates, tail surrogates and privates
  //fprintf(stdout, "SAFE_START:%08X - %08X, %i\n", start, limit, type);
  if(type > 0) {
    for(; start < limit; start++) {
      cc = u_getCombiningClass(start);
      if(cc == 0) {
        int32_t lowerLimit = start;
        while(cc == 0 && start <= limit) {
          cc = u_getCombiningClass(++start);
        }
        SAFE_START->add(lowerLimit, start-1);
      }
    }
  } else {
    SAFE_START->add(start, limit-1);
  }
  return TRUE;
}

static UBool U_CALLCONV
_enumCategoryRangeAT_STARTsetup(const void *context, UChar32 start, UChar32 limit, UCharCategory type) {
  UErrorCode status = *(UErrorCode *)context;
  int32_t cc = 0;
  //fprintf(stdout, "AT_START:%08X - %08X, %i\n", start, limit, type);
  UChar32 cp = 0;
  if(type > 0) {
    for(cp = start; cp < limit; cp++) {
        UnicodeString istr(cp);
        UnicodeString decomp;
        Normalizer::normalize(istr, UNORM_NFD, 0, decomp, status);
        if (decomp==istr) continue;
    
        // add each character in the decomposition to canBeIn      
        UChar32 component = 0;
        int32_t i = 0;
        for (i = 0; i < decomp.length(); i += UTF16_CHAR_LENGTH(component)) {
            component = decomp.char32At(i);
            if (i == 0) {
              UnicodeSet *isIn = (UnicodeSet *)AT_START->get(component);
              if(isIn == NULL) {
                isIn = new UnicodeSet();
              }
              isIn->add(cp);
              AT_START->put(component, isIn, status);
            } else if (u_getCombiningClass(component) == 0) {
                SAFE_START->remove(component);
            }
        }
    }
  }
  return TRUE;
}

void CanonicalIterator::initStaticData(UErrorCode status) {
  if(SAFE_START == NULL && AT_START == NULL) {
    SAFE_START = new UnicodeSet();
    // TODO: have value deleter for UnicodeSets
    AT_START = new Hashtable(FALSE, status);

    UChar32 cp = 0;
    //if (PROGRESS) printf("Getting Safe Start");

    // TODO: use u_enumCharType() instead
    // the fastest with current, public apis is to 
    // enumerate with u_enumCharType() for all categories !=0 and then 
    // getCombiningClass(start..limit-1) that cuts it down by a factor of about 11...
    u_enumCharTypes(_enumCategoryRangeSAFE_STARTsetup, 0);
  
    //if (PROGRESS) printf("Getting Containment\n");
    u_enumCharTypes(_enumCategoryRangeAT_STARTsetup, &status);
  }
}

/**
 *@return the set of "safe starts", characters that are class zero AND are never non-initial in a decomposition.
 */
UnicodeSet *CanonicalIterator::getSafeStart(UErrorCode status) {
  initStaticData(status);
    return  SAFE_START;
}

/**
 *@return the set of characters whose decompositions start with the given character
 */
UnicodeSet *CanonicalIterator::getStarts(UChar32 cp, UErrorCode status) {
  initStaticData(status);
  UnicodeSet *result = (UnicodeSet *)AT_START->get(cp);
  return result;
}

// privates
    
// we have a segment, in NFD. Find all the strings that are canonically equivalent to it.
UnicodeString* CanonicalIterator::getEquivalents(UnicodeString segment, int32_t &result_len, UErrorCode status) { //private String[] getEquivalents(String segment) 
    Hashtable *result = new Hashtable(FALSE, status);
    Hashtable *basic = getEquivalents2(segment, status);
    
    // now get all the permutations
    // add only the ones that are canonically equivalent
    // TODO: optimize by not permuting any class zero.
    const UHashElement *ne = NULL;
    int32_t el = -1;
    //Iterator it = basic.iterator();
    ne = basic->nextElement(el);
    //while (it.hasNext()) 
    while (ne != NULL) {
        //String item = (String) it.next();
        UnicodeString item = *((UnicodeString *)(ne->value.pointer));
        Hashtable *permutations = permute(item, status);
        const UHashElement *ne2 = NULL;
        int32_t el2 = -1;
        //Iterator it2 = permutations.iterator();
        ne2 = permutations->nextElement(el2);
        //while (it2.hasNext()) 
        while (ne2 != NULL) {
            //String possible = (String) it2.next();
            UnicodeString *possible = new UnicodeString(*((UnicodeString *)(ne2->value.pointer)));
            UnicodeString attempt;
            Normalizer::normalize(*possible, UNORM_NFD, 0, attempt, status);

            // TODO: check if operator == is semanticaly the same as attempt.equals(segment)
            if (attempt==segment) {
                //if (PROGRESS) printf("Adding Permutation: %s\n", UToS(Tr(*possible)));
                // TODO: use the hashtable just to catch duplicates - store strings directly (somehow).
                result->put(*possible, possible, status); //add(possible);
            } else {
                //if (PROGRESS) printf("-Skipping Permutation: %s\n", UToS(Tr(*possible)));
            }

          ne2 = permutations->nextElement(el2);
        }
        delete permutations;
        ne = basic->nextElement(el);
    }
    
    // convert into a String[] to clean up storage
    //String[] finalResult = new String[result.size()];
    UnicodeString *finalResult = new UnicodeString[result->count()];
    //result.toArray(finalResult);
    result_len = 0;
    el = -1;
    ne = result->nextElement(el);
    while(ne != NULL) {
      UnicodeString finResult = *((UnicodeString *)(ne->value.pointer));
      finalResult[result_len++] = finResult;
      ne = result->nextElement(el);
    }


    delete result;
    return finalResult;
}

Hashtable *CanonicalIterator::getEquivalents2(UnicodeString segment, UErrorCode status) {
    //Set result = new TreeSet();
    Hashtable *result = new Hashtable(FALSE, status);
    result->setValueDeleter(uhash_deleteUnicodeString);

    //if (PROGRESS) printf("Adding: %s\n", UToS(Tr(segment)));

    //result.add(segment);
    result->put(segment, new UnicodeString(segment), status);

    //StringBuffer workingBuffer = new StringBuffer();
    UnicodeString workingBuffer;

    
    // cycle through all the characters
    UChar32 cp;
    int32_t i = 0, j = 0;
    for (i = 0; i < segment.length(); i += UTF16_CHAR_LENGTH(cp)) {
        // see if any character is at the start of some decomposition
        cp = segment.char32At(i);
        UnicodeSet *starts = (UnicodeSet *)AT_START->get(cp);
        if (starts == NULL) continue;
        //UnicodeSetIterator usi = new UnicodeSetIterator(starts);
        int32_t setSize = starts->size();
        // if so, see which decompositions match 
        //while (TRUE) {
        for(j = 0; j < setSize; j++) {
            //UChar32 cp2 = usi.next();
            UChar32 cp2 = starts->charAt(j);
            //if (cp2 < 0) break; // done
            const Hashtable *remainder = extract(cp2, segment, i, workingBuffer, status);
            if (remainder == NULL) continue;
            
            // there were some matches, so add all the possibilities to the set.
            //UnicodeString prefix = segment.substring(0, i) + UTF16.valueOf(cp2);
            UnicodeString *prefix = new UnicodeString;
            segment.extract(0, i, *prefix);
            *prefix += cp2;

            const UHashElement *ne = NULL;
            int32_t el = -1;
            //Iterator it = remainder.iterator();
            ne = remainder->nextElement(el);
            while (ne != NULL) {
                //String item = (String) it.next();
                UnicodeString item = *((UnicodeString *)(ne->value.pointer));
                //result.add(prefix + item);
                *prefix += item;
                result->put(*prefix, prefix, status);

                //if (PROGRESS) printf("Adding: %s\n", UToS(Tr(*prefix)));

                ne = remainder->nextElement(el);
            }

            delete remainder;
        }
    }
    return result;
}

/**
 * See if the decomposition of cp2 is at segment starting at segmentPos 
 * (with canonical rearrangment!)
 * If so, take the remainder, and return the equivalents 
 */
const Hashtable *CanonicalIterator::extract(UChar32 comp, UnicodeString segment, int32_t segmentPos, UnicodeString buffer, UErrorCode status) {
    //if (PROGRESS) printf(" extract: %s, ", UToS(Tr(UnicodeString(comp))));
    //if (PROGRESS) printf("%s, %i\n", UToS(Tr(segment)), segmentPos);

    //String decomp = Normalizer.normalize(UTF16.valueOf(comp), Normalizer.DECOMP, 0);
    UnicodeString decomp;
    Normalizer::normalize(comp, UNORM_NFD, 0, decomp, status);
    
    // See if it matches the start of segment (at segmentPos)
    UBool ok = FALSE;
    UChar32 cp;
    int32_t decompPos = 0;
    UChar32 decompCp = decomp.char32At(0);
    decompPos += UTF16_CHAR_LENGTH(decompCp); // adjust position to skip first char
    //int decompClass = getClass(decompCp);
    buffer.truncate(0); // initialize working buffer, shared among callees
    
    int32_t i = 0;
    for (i = segmentPos; i < segment.length(); i += UTF16_CHAR_LENGTH(cp)) {
        cp = segment.char32At(i);
        if (cp == decompCp) { // if equal, eat another cp from decomp

            //if (PROGRESS) printf("  matches: %s\n", UToS(Tr(UnicodeString(cp))));

            if (decompPos == decomp.length()) { // done, have all decomp characters!
                //buffer.append(segment.substring(i + UTF16.getCharCount(cp))); // add remaining segment chars
              buffer.append(segment, i+UTF16_CHAR_LENGTH(cp), segment.length()-i-UTF16_CHAR_LENGTH(cp));
                ok = TRUE;
                break;
            }
            decompCp = decomp.char32At(decompPos);
            decompPos += UTF16_CHAR_LENGTH(decompCp);
            //decompClass = getClass(decompCp);
        } else {
            //if (PROGRESS) printf("  buffer: %s\n", UToS(Tr(UnicodeString(cp))));

            // brute force approach

          
            //UTF16.append(buffer, cp);
            buffer.append(cp);

            /* TODO: optimize
            // since we know that the classes are monotonically increasing, after zero
            // e.g. 0 5 7 9 0 3
            // we can do an optimization
            // there are only a few cases that work: zero, less, same, greater
            // if both classes are the same, we fail
            // if the decomp class < the segment class, we fail
    
            segClass = getClass(cp);
            if (decompClass <= segClass) return null;
            */
        }
    }
    if (!ok) return NULL; // we failed, characters left over

    //if (PROGRESS) printf("Matches\n");

    if (buffer.length() == 0) {
      Hashtable *result = new Hashtable(FALSE, status);
      result->setValueDeleter(uhash_deleteUnicodeString);
      result->put("", new UnicodeString(""), status);
      return result; // succeed, but no remainder
    }

    //String remainder = buffer.toString();
    UnicodeString remainder = buffer;
    
    // brute force approach
    // check to make sure result is canonically equivalent
    //String trial = Normalizer.normalize(UTF16.valueOf(comp) + remainder, Normalizer.DECOMP, 0);
    UnicodeString trial;
    UnicodeString temp = remainder;
    temp.insert(0, comp);
    Normalizer::normalize(temp, UNORM_NFD, 0, trial, status);

    //if (!segment.regionMatches(segmentPos, trial, 0, segment.length() - segmentPos)) return null;
    if (segment.indexOf(trial, 0, segment.length() - segmentPos, segmentPos, segment.length() - segmentPos)==-1) {
      return NULL;
    }
    
    // get the remaining combinations
    return getEquivalents2(remainder, status);
}


