/*
*******************************************************************************
* Copyright (C) 1996-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File tblcoll.cpp
*
* Created by: Helena Shih
*
* Modification History:
*
*  Date        Name        Description
*  2/5/97      aliu        Added streamIn and streamOut methods.  Added
*                          constructor which reads RuleBasedCollator object from
*                          a binary file.  Added writeToFile method which streams
*                          RuleBasedCollator out to a binary file.  The streamIn
*                          and streamOut methods use istream and ostream objects
*                          in binary mode.
*  2/11/97     aliu        Moved declarations out of for loop initializer.
*                          Added Mac compatibility #ifdef for ios::nocreate.
*  2/12/97     aliu        Modified to use TableCollationData sub-object to
*                          hold invariant data.
*  2/13/97     aliu        Moved several methods into this class from Collation.
*                          Added a private RuleBasedCollator(Locale&) constructor,
*                          to be used by Collator::getInstance().  General
*                          clean up.  Made use of UErrorCode variables consistent.
*  2/20/97     helena      Added clone, operator==, operator!=, operator=, and copy
*                          constructor and getDynamicClassID.
*  3/5/97      aliu        Changed compaction cycle to improve performance.  We
*                          use the maximum allowable value which is kBlockCount.
*                          Modified getRules() to load rules dynamically.  Changed
*                          constructFromFile() call to accomodate this (added
*                          parameter to specify whether binary loading is to
*                          take place).
* 05/06/97     helena      Added memory allocation error check.
*  6/20/97     helena      Java class name change.
*  6/23/97     helena      Adding comments to make code more readable.
* 09/03/97     helena      Added createCollationKeyValues().
* 06/26/98     erm         Changes for CollationKeys using byte arrays.
* 08/10/98     erm         Synched with 1.2 version of RuleBasedCollator.java
* 04/23/99     stephen     Removed EDecompositionMode, merged with
*                          Normalizer::EMode
* 06/14/99     stephen     Removed kResourceBundleSuffix
* 06/22/99     stephen     Fixed logic in constructFromFile() since .ctx
*                          files are no longer used.
* 11/02/99     helena      Collator performance enhancements.  Special case
*                          for NO_OP situations. 
* 11/17/99     srl         More performance enhancements. Inlined some internal functions.
*******************************************************************************
*/

#include "ucmp32.h"
#include "tcoldata.h"

#include "tblcoll.h"

#include "coleitr.h"
#include "locid.h"
#include "unicode.h"
#include "tables.h"
#include "normlzr.h"
#include "mergecol.h"
#include "resbund.h"
#include "filestrm.h"

#ifdef _DEBUG
#include "unistrm.h"
#endif

#include "compitr.h"

#include <string.h>

#include <ustring.h>


class RuleBasedCollatorStreamer
{
public:
    static void streamIn(RuleBasedCollator* collator, FileStream* is);
    static void streamOut(const RuleBasedCollator* collator, FileStream* os);
};

//===========================================================================================
//  The following diagram shows the data structure of the RuleBasedCollator object.
//  Suppose we have the rule, where 'o-umlaut' is the unicode char 0x00F6.
//  "a, A < b, B < c, C, ch, cH, Ch, CH < d, D ... < o, O; 'o-umlaut'/E, 'O-umlaut'/E ...".
//  What the rule says is, sorts 'ch'ligatures and 'c' only with tertiary difference and
//  sorts 'o-umlaut' as if it's always expanded with 'e'.
//
// mapping table                       contracting list                  expanding list
// (contains all unicode char
//  entries)                         ___     _____________         _________________________
//   ________                   |==>|_*_|-->|'c'  |v('c') |   |==>|v('o')|v('umlaut')|v('e')|
//  |_\u0001_|--> v('\u0001')   |   |_:_|   |-------------|   |   |-------------------------|
//  |_\u0002_|--> v('\u0002')   |   |_:_|   |'ch' |v('ch')|   |   |             :           |
//  |____:___|                  |   |_:_|   |-------------|   |   |-------------------------|
//  |____:___|                  |           |'cH' |v('cH')|   |   |             :           |
//  |__'a'___|--> v('a')        |           |-------------|   |   |-------------------------|
//  |__'b'___|--> v('b')        |           |'Ch' |v('Ch')|   |   |             :           |
//  |____:___|                  |           |-------------|   |   |-------------------------|
//  |____:___|                  |           |'CH' |v('CH')|   |   |             :           |
//  |___'c'__|-------------------            -------------    |   |-------------------------|
//  |____:___|                                                |   |             :           |
//  |o-umlaut|------------------------------------------------    |_________________________|
//  |____:___|
//
//
// Noted by Helena Shih on 6/23/97 with pending design changes (slimming collation).
//============================================================================================

const int32_t RuleBasedCollator::CHARINDEX = 0x70000000;             // need look up in .commit()
const int32_t RuleBasedCollator::EXPANDCHARINDEX = 0x7E000000;       // Expand index follows
const int32_t RuleBasedCollator::CONTRACTCHARINDEX = 0x7F000000;     // contract indexes follows
const int32_t RuleBasedCollator::UNMAPPED = 0xFFFFFFFF;              // unmapped character values
const int32_t RuleBasedCollator::PRIMARYORDERINCREMENT = 0x00010000; // primary strength increment
const int32_t RuleBasedCollator::SECONDARYORDERINCREMENT = 0x00000100; // secondary strength increment
const int32_t RuleBasedCollator::TERTIARYORDERINCREMENT = 0x00000001; // tertiary strength increment
const int32_t RuleBasedCollator::MAXIGNORABLE = 0x00010000;          // maximum ignorable char order value
const int32_t RuleBasedCollator::PRIMARYORDERMASK = 0xffff0000;      // mask off anything but primary order
const int32_t RuleBasedCollator::SECONDARYORDERMASK = 0x0000ff00;    // mask off anything but secondary order
const int32_t RuleBasedCollator::TERTIARYORDERMASK = 0x000000ff;     // mask off anything but tertiary order
const int32_t RuleBasedCollator::SECONDARYRESETMASK = 0x0000ffff;    // mask off secondary and tertiary order
const int32_t RuleBasedCollator::IGNORABLEMASK = 0x0000ffff;         // mask off ignorable char order
const int32_t RuleBasedCollator::PRIMARYDIFFERENCEONLY = 0xffff0000; // use only the primary difference
const int32_t RuleBasedCollator::SECONDARYDIFFERENCEONLY = 0xffffff00;  // use only the primary and secondary difference
const int32_t RuleBasedCollator::PRIMARYORDERSHIFT = 16;             // primary order shift
const int32_t RuleBasedCollator::SECONDARYORDERSHIFT = 8;            // secondary order shift
const int32_t RuleBasedCollator::SORTKEYOFFSET = 1;                  // minimum sort key offset
const int32_t RuleBasedCollator::CONTRACTCHAROVERFLOW = 0x7FFFFFFF;  // Indicates the char is a contract char

const int16_t RuleBasedCollator::FILEID = 0x5443;                    // unique file id for parity check
const char* RuleBasedCollator::kFilenameSuffix = ".col";             // binary collation file extension
char  RuleBasedCollator::fgClassID = 0; // Value is irrelevant       // class id

//================ Some inline definitions of implementation functions........ ========

// Get the character order in the mapping table
inline int32_t
RuleBasedCollator::getUnicodeOrder(UChar ch) const
{
    return ucmp32_get(data->mapping, ch);
}

inline int32_t
RuleBasedCollator::strengthOrder(int32_t value) const
{
    if (getStrength() == PRIMARY)
    {
        return (value & PRIMARYDIFFERENCEONLY);
    } else if (getStrength() == SECONDARY)
    {
        return (value & SECONDARYDIFFERENCEONLY);
    }
    return value;
}


inline int32_t
RuleBasedCollator::getStrengthOrder(NormalizerIterator* cursor, 
                                    UErrorCode status) const
{
    if (U_FAILURE(status))
    {
        return CollationElementIterator::NULLORDER;
    }

    if (cursor->bufferAlias != NULL)
    {
        // bufferAlias needs a bit of an explanation.
        // When we hit an expanding character in the text, we call the order's
        // getExpandValues method to retrieve an array of the orderings for all
        // of the characters in the expansion (see the end of this method).
        // The first ordering is returned, and an alias to the orderings array
        // is saved so that the remaining orderings can be returned on subsequent
        // calls to next.  So, if the expanding buffer is not exhausted, 
        // all we have to do here is return the next ordering in the buffer.  
        if (cursor->expIndex < cursor->bufferAlias->size())
        {
	  //_L((stderr, "next from [%08X] from bufferAlias\n", this));
            return strengthOrder(cursor->bufferAlias->at(cursor->expIndex++));
        }
        else
        {
            cursor->bufferAlias = NULL;
            cursor->expIndex = 0;
        }
    }
    else if (cursor->swapOrder != 0)
    {
        // If we find a character with no order, we return the marking
        // flag, UNMAPPEDCHARVALUE, 0x7fff0000, and then the character 
        // itself shifted left 16 bits as orders.  At this point, the
        // UNMAPPEDCHARVALUE flag has already been returned by the code
        // below, so just return the shifted character here.
        int32_t order = cursor->swapOrder << 16;

	  //_L((stderr, "next from [%08X] swaporder..\n", this));
        cursor->swapOrder = 0;

        return order;
    }

    UChar ch = cursor->current();
    cursor->next();

    //_L((stderr, "Next from [%08X] = [%04X], [%c]\n", cursor, (int)ch & 0xFFFF, (char)(ch & 0xFF)));
    
    if (ch == Normalizer::DONE) {
        return CollationElementIterator::NULLORDER;
    }
    // Ask the collator for this character's ordering.
    int32_t value = getUnicodeOrder(ch);

    if (value == UNMAPPED)
    {
        // Returned an "unmapped" flag and save the character so it can be 
        // returned next time this method is called.
        if (ch == 0x0000) return ch;
        cursor->swapOrder = ch;  // \u0000 is not valid in C++'s UnicodeString
        return CollationElementIterator::UNMAPPEDCHARVALUE;
    }
    
    if (value >= CONTRACTCHARINDEX)
    {
        value = nextContractChar(cursor, ch, status);
    }

    if (value >= EXPANDCHARINDEX)
    {
        cursor->bufferAlias = getExpandValueList(value);
        cursor->expIndex = 0;
        value = cursor->bufferAlias->at(cursor->expIndex++);
    }

    int32_t str = strengthOrder(value);   
    
    return strengthOrder(value);
}

// ==================== End inlines ============================================


//===============================================================================

RuleBasedCollator::RuleBasedCollator()
    : Collator(),
      isOverIgnore(FALSE),
      mPattern(0),
      //      sourceCursor(0),
      //targetCursor(0),
      cursor1(0),
      cursor2(0),
      data(0),
      dataIsOwned(FALSE)
{
}

RuleBasedCollator::RuleBasedCollator(const  RuleBasedCollator&  that)
    : Collator(that),
      isOverIgnore(that.isOverIgnore),
      mPattern(0),
      //      sourceCursor(0),
      //targetCursor(0),
      cursor1(0),
      cursor2(0),
      dataIsOwned(FALSE),
      data(that.data) // Alias the data pointer
{
}

bool_t
RuleBasedCollator::operator==(const Collator& that) const
{
    if (this == &that)
    {
        return TRUE;
    }

    if (this->getDynamicClassID() != that.getDynamicClassID())
    {
        return FALSE;  // not the same class
    }

    if (!Collator::operator==(that))
    {
        return FALSE;
    }

    RuleBasedCollator& thatAlias = (RuleBasedCollator&)that;

    if (isOverIgnore != thatAlias.isOverIgnore)
    {
        return FALSE;
    }

    if (data != thatAlias.data)
    {
        return FALSE;
    }

    return TRUE;
}

RuleBasedCollator&
RuleBasedCollator::operator=(const  RuleBasedCollator& that)
{
    if (this != &that)
    {
        Collator::operator=(that);
        isOverIgnore = that.isOverIgnore;

        if (dataIsOwned)
        {
            delete data;
        }

        data = 0;
        delete mPattern;
        mPattern = 0;
        dataIsOwned = FALSE;
        data = that.data;
    }

    return *this;
}

RuleBasedCollator::RuleBasedCollator(const  UnicodeString&  rules,
                                        UErrorCode&      status)
    : Collator(),
      isOverIgnore(FALSE),
      mPattern(0),
      //      sourceCursor(0),
      ///      targetCursor(0),
      cursor1(0),
      cursor2(0),
      data(0),
      dataIsOwned(FALSE)
{
    if (U_FAILURE(status))
    {
        return;
    }

    constructFromRules(rules, status);
}

RuleBasedCollator::RuleBasedCollator(const  UnicodeString&  rules,
                     ECollationStrength collationStrength,
                     UErrorCode&      status)
  : Collator(collationStrength, Normalizer::NO_OP),
    isOverIgnore(FALSE),
    mPattern(0),
    //    sourceCursor(0),
    //    targetCursor(0),
      cursor1(0),
      cursor2(0),
    data(0),
    dataIsOwned(FALSE)
{
    if (U_FAILURE(status))
    {
        return;
    }
    constructFromRules(rules, status);
}

RuleBasedCollator::RuleBasedCollator(const  UnicodeString&  rules,
                     Normalizer::EMode decompositionMode,
                     UErrorCode&      status)
  : Collator(TERTIARY, decompositionMode),
    isOverIgnore(FALSE),
    mPattern(0),
    //    sourceCursor(0),
    //    targetCursor(0),
      cursor1(0),
      cursor2(0),
    data(0),
    dataIsOwned(FALSE)
{
  if (U_FAILURE(status))
    {
      return;
    }
  
  constructFromRules(rules, status);
}

RuleBasedCollator::RuleBasedCollator(const  UnicodeString&  rules,
                     ECollationStrength collationStrength,
                     Normalizer::EMode decompositionMode,
                     UErrorCode&      status)
  : Collator(collationStrength, decompositionMode),
      isOverIgnore(FALSE),
      mPattern(0),
    //      sourceCursor(0),
    //targetCursor(0),
      cursor1(0),
      cursor2(0),
      data(0),
      dataIsOwned(FALSE)
{
    if (U_FAILURE(status))
    {
        return;
    }

    constructFromRules(rules, status);
}

void RuleBasedCollator::constructFromRules(const UnicodeString& rules,
                                        UErrorCode& status)
{
    // Construct this collator's ruleset from its string representation
    if (U_FAILURE(status))
    {
        return;
    }

    if (rules.isBogus())
    {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    if (dataIsOwned)
    {
        delete data;
        data = 0;
    }

    isOverIgnore = FALSE;
    setStrength(Collator::TERTIARY);

    data = new TableCollationData;
    if (data->isBogus())
    {
        status = U_MEMORY_ALLOCATION_ERROR;
        delete data;
        data = 0;
        return;
    }

    // We constructed the data using the build method, so we own it.
    dataIsOwned = TRUE;

    // Now that we've got all the buffers allocated, do the actual work
    mPattern = 0;
    build(rules, status);
}

void
RuleBasedCollator::constructFromFile(const char* fileName,
                                  UErrorCode& status)
{
    // This method tries to read in a flattened RuleBasedCollator that
    // has been previously streamed out using the streamOut() method.
    // The 'fileName' parameter should contain a full pathname valid on
    // the local environment.

    if (U_FAILURE(status))
    {
        return;
    }

    if (dataIsOwned)
    {
        delete data;
        data = 0;
    }

    mPattern = 0;
    isOverIgnore = FALSE;
    setStrength(Collator::TERTIARY); // This is the default strength

    FileStream* ifs = T_FileStream_open(fileName, "rb");
    if (ifs == 0) {
        status = U_FILE_ACCESS_ERROR;
        return;
    }

    // The streamIn function does the actual work here...
    RuleBasedCollatorStreamer::streamIn(this, ifs);

    if (!T_FileStream_error(ifs))
    {
        status = U_ZERO_ERROR;
    }
    else if (data && data->isBogus())
    {
        status = U_MEMORY_ALLOCATION_ERROR;
        delete data;
        data = 0;
    }
    else
    {
        status = U_MISSING_RESOURCE_ERROR;
        delete data;
        data = 0;
    }

#ifdef COLLDEBUG
    fprintf(stderr, "binary read %s size %d, %s\n", fileName, T_FileStream_size(ifs), errorName(status));
#endif

    // We constructed the data when streaming it in, so we own it
    dataIsOwned = TRUE;

    T_FileStream_close(ifs);
}

RuleBasedCollator::RuleBasedCollator(   const Locale& desiredLocale,
                                UErrorCode& status)
    : Collator(),
      isOverIgnore(FALSE),
      dataIsOwned(FALSE),
      data(0),
      //      sourceCursor(0),
      //targetCursor(0),
      cursor1(0),
      cursor2(0),
      mPattern(0)
{


  if (U_FAILURE(status))
    {
      return;
    }
  
  // Try to load, in order:
  // 1. The desired locale's collation.
  // 2. A fallback of the desired locale.
  // 3. The default locale's collation.
  // 4. A fallback of the default locale.
  // 5. The default collation rules, which contains en_US collation rules.

  // To reiterate, we try:
  // Specific:
  //  language+country+variant
  //  language+country
  //  language
  // Default:
  //  language+country+variant
  //  language+country
  //  language
  // Root: (aka DEFAULTRULES)

  UnicodeString localeName;
  desiredLocale.getName(localeName);
  enum { eTryDefaultLocale, eTryDefaultCollation, eDone } next = eTryDefaultLocale;
    
  for (;;)
    {
      if (localeName.size() == 0)
    {
      if (next == eDone)
            {
          // We've failed to load a locale, but should never return U_MISSING_RESOURCE_ERROR
          UErrorCode intStatus = U_ZERO_ERROR;

          constructFromRules(RuleBasedCollator::DEFAULTRULES, intStatus);
          if (intStatus == U_ZERO_ERROR)
        {
          status = U_USING_DEFAULT_ERROR;
        }
          else
        {
          status = intStatus;     // bubble back
        }

          if (status == U_MEMORY_ALLOCATION_ERROR)
        {
          return;
        }

	  // srl write out default.col
	  {
	    UnicodeString defLocaleName = UnicodeString(ResourceBundle::kDefaultFilename,""); 
	    char *binaryFilePath = createPathName(UnicodeString(Locale::getDataDirectory(),""), 
						  defLocaleName, UnicodeString(kFilenameSuffix,""));
	    bool_t ok = writeToFile(binaryFilePath);
	    delete [] binaryFilePath;
#ifdef COLLDEBUG
	    cerr << defLocaleName << " [default] binary write " << (ok? "OK" : "Failed") << endl;
#endif
	  }

          data->desiredLocale = desiredLocale;
          desiredLocale.getName(localeName);
          data->realLocaleName = localeName;
          addToCache(localeName);

          setDecomposition(Normalizer::NO_OP);

          const UnicodeString& rules = getRules();
          break;
            }

      // We've exhausted our inheritance attempts with this locale.
      // Try the next step.
      switch (next)
            {
            case eTryDefaultLocale:
          status = U_USING_DEFAULT_ERROR;
          Locale::getDefault().getName(localeName);
          next = eTryDefaultCollation;
          break;

            case eTryDefaultCollation:
          // There is no distinction between this condition of
          // using a default collation object and the condition of
          // using a default locale to get a collation object currently.
          // That is, the caller can't distinguish based on UErrorCode.
          status = U_USING_DEFAULT_ERROR;
          localeName = ResourceBundle::kDefaultFilename;
          next = eDone;
          break;
            }
        }

      // First try to load the collation from the in-memory static cache.
      // Note that all of the caching logic is handled here, and in the
      // call to RuleBasedCollator::addToCache, below.
      UErrorCode intStatus = U_ZERO_ERROR;

      constructFromCache(localeName, intStatus);
      if (U_SUCCESS(intStatus))
    {
      break; // Done!
    }

      // The collation we want is not in the cache.  The second thing
      // to try is loading from a file, either binary or ASCII.  So:
      // Try to load the locale's collation data.  This will try to load
      // a binary collation file, or if that is unavailable, it will go
      // to the text resource bundle file (with the corresponding name)
      // and try to get the collation table there.
      intStatus = U_ZERO_ERROR;
      constructFromFile(desiredLocale, localeName, TRUE, intStatus);
      if (U_SUCCESS(intStatus))
        {
      // If we succeeded in loading the collation from a file, now is the
      // time to add it to the in-memory cache.  We record the real
      // location at which the collation data was found, so we can reload
      // the rule table quickly, if it is requested, in the future.
      // See getRules().
      data->desiredLocale = desiredLocale;
      data->realLocaleName = localeName;
      addToCache(localeName);

      setDecomposition(Normalizer::NO_OP);
      break; // Done!
        }
      if (intStatus == U_MEMORY_ALLOCATION_ERROR)
    {
      status = intStatus;
      return;
        }

      // Having failed, chop off the end of the locale name, making
      // it less specific, and try again.  Indicate the use of a
      // fallback locale, unless we've already fallen through to
      // a default locale -- then leave the status as is.
      if (status == U_ZERO_ERROR)
    {
      status = U_USING_FALLBACK_ERROR;
    }

      chopLocale(localeName);
    }
}

void
RuleBasedCollator::constructFromFile(   const Locale&           locale,
                                    const UnicodeString&    localeFileName,
                                    bool_t                  tryBinaryFile,
                                    UErrorCode&              status)
{
  // constructFromFile creates a collation object by reading from a
  // file.  It does not employ the usual FILE search mechanism with
  // locales, default locales, and base locales.  Instead, it tries to
  // look only in files with the given localFileName.  It does,
  // however, employ the LOCALE search mechanism.
  
  // This method maintains the binary collation files.  If a collation
  // is not present in binary form, but is present in text form (in a
  // resource bundle file), it will be loaded in text form, and then
  // written to disk.
  
  // If tryBinaryFile is true, then try to load from the binary file first.

  if(U_FAILURE(status)) {
    return;
  }
  
  if(dataIsOwned) {
    delete data;
    data = 0;
  }
  
  char *binaryFilePath = createPathName(UnicodeString(Locale::getDataDirectory(),""), 
					localeFileName, UnicodeString(kFilenameSuffix,""));
  
  if(tryBinaryFile) {
    // Try to load up the collation from a binary file first
    constructFromFile(binaryFilePath, status);
#ifdef COLLDEBUG
    cerr << localeFileName  << kFilenameSuffix << " binary load " << errorName(status) << endl;
#endif
    if(U_SUCCESS(status) || status == U_MEMORY_ALLOCATION_ERROR) 
      return;
    }

  // Now try to load it up from a resource bundle text source file
  ResourceBundle bundle(UnicodeString(Locale::getDataDirectory(),""), localeFileName, status);

  // if there is no resource bundle file for the give locale, break out
  if(U_FAILURE(status))
    return;

#ifdef COLLDEBUG
  cerr << localeFileName << " ascii load " << errorName(status) << endl;
#endif

  // check and see if this resource bundle contains collation data
  
  UnicodeString colString;
  UErrorCode intStatus = U_ZERO_ERROR;

  bundle.getString("CollationElements", colString, intStatus);
  if(colString.isBogus()) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  // if this bundle doesn't contain collation data, break out
  if(U_FAILURE(intStatus)) {
    status = U_MISSING_RESOURCE_ERROR;
    return;
  }

  // Having loaded the collation from the resource bundle text file,
  // now retrieve the CollationElements tagged data, merged with the
  // default rules.  If that fails, use the default rules alone.

  colString.insert(0, DEFAULTRULES);
  if(colString.isBogus()) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }
    
  constructFromRules(colString, intStatus);
  if(intStatus == U_MEMORY_ALLOCATION_ERROR) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }
  
  if(intStatus != U_ZERO_ERROR)  {
    status = U_USING_DEFAULT_ERROR;
      
    // predefined tables should contain correct grammar
    intStatus = U_ZERO_ERROR;
    constructFromRules(DEFAULTRULES, intStatus);
    if(intStatus != U_ZERO_ERROR) {
      status = intStatus;
    }
  } 
  
#ifdef COLLDEBUG
  cerr << localeFileName << " ascii load " << (U_SUCCESS(status) ? "OK" : "Failed") << " - try= " << (tryBinaryFile?"true":"false") << endl;
#endif
  
  if(U_SUCCESS(status) && tryBinaryFile) {
    // If we get a RuleBasedCollator result, even if it is derived
    // from a default or a fallback, then we write it out as a
    // binary file to the disk.  The next time the system wants to
    // get this collation, it will load up very quickly from the
    // binary file.
    bool_t ok = writeToFile(binaryFilePath);
    delete [] binaryFilePath;
#ifdef COLLDEBUG
    cerr << localeFileName << " binary write " << (ok? "OK" : "Failed") << endl;
#endif
  }
}

RuleBasedCollator::~RuleBasedCollator()
{
    if (dataIsOwned)
    {
        delete data;
    }

    data = 0;

    //    delete sourceCursor;
    //    sourceCursor = 0;

    //    delete targetCursor;
    //    targetCursor = 0;

    if (cursor1 != NULL) {
        delete cursor1;
        cursor1 = 0;
    }
    if (cursor2 != NULL) {
        delete cursor2;
        cursor2 = 0;
    }

    delete mPattern;
    mPattern = 0;
}

Collator*
RuleBasedCollator::clone() const
{
    return new RuleBasedCollator(*this);
}

// Create a CollationElementIterator object that will iterator over the elements
// in a string, using the collation rules defined in this RuleBasedCollator
CollationElementIterator*
RuleBasedCollator::createCollationElementIterator(const UnicodeString& source) const
{
    UErrorCode status = U_ZERO_ERROR;
    CollationElementIterator *newCursor = 0;

    newCursor = new CollationElementIterator(source, this, status);
    if (U_FAILURE(status))
    {
        return NULL;
    }

    return newCursor;
}

// Create a CollationElementIterator object that will iterator over the elements
// in a string, using the collation rules defined in this RuleBasedCollator
CollationElementIterator*
RuleBasedCollator::createCollationElementIterator(const CharacterIterator& source) const
{
    UErrorCode status = U_ZERO_ERROR;
    CollationElementIterator *newCursor = 0;

    newCursor = new CollationElementIterator(source, this, status);
    if (U_FAILURE(status))
    {
        return NULL;
    }

    return newCursor;
}

// Return a string representation of this collator's rules.
// The string can later be passed to the constructor that takes a
// UnicodeString argument, which will construct a collator that's
// functionally identical to this one.
// You can also allow users to edit the string in order to change
// the collation data, or you can print it out for inspection, or whatever.

const UnicodeString&
RuleBasedCollator::getRules() const
{
    if (mPattern != 0)
    {
        MergeCollation*& nonConstMPattern = *(MergeCollation**)&mPattern;
        mPattern->emitPattern(data->ruleTable);
        data->isRuleTableLoaded = TRUE;
        delete nonConstMPattern;
        nonConstMPattern = 0;
    }
    else if (!data->isRuleTableLoaded)
    {
        // At this point the caller wants the rules, but the rule table data
        // is not loaded.  Furthermore, there is no mPattern object to load
        // the rules from.  Therefore, we fetch the rules off the disk.
        // Notice that we pass in a tryBinaryFile value of FALSE, since
        // by design the binary file has NO rules in it!
        RuleBasedCollator temp;
        UErrorCode status = U_ZERO_ERROR;
        temp.constructFromFile(data->desiredLocale, data->realLocaleName, FALSE, status);

        // We must check that mPattern is nonzero here, or we run the risk
        // of an infinite loop.
        if (U_SUCCESS(status) && temp.mPattern != 0)
        {
            data->ruleTable = temp.getRules();
            data->isRuleTableLoaded = TRUE;
#ifdef _DEBUG
//              // the following is useful for specific debugging purposes
//               UnicodeString name;
//               cerr << "Table collation rules loaded dynamically for "
//                   << data->desiredLocale.getName(name)
//                   << " at "
//                   << data->realLocaleName
//                   << ", " << dec << data->ruleTable.size() << " characters"
//                   << endl;
#endif
        }
        else
        {
#ifdef _DEBUG
//              UnicodeString name;
//              cerr << "Unable to load table collation rules dynamically for "
//                  << data->desiredLocale.getName(name)
//                  << " at "
//                  << data->realLocaleName
//                  << endl;
//              cerr << "Status " << errorName(status) << ", mPattern " << temp.mPattern << endl;
#endif
	    /* SRL have to add this because we now have the situation where
	       DEFAULT is loaded from a binary file w/ no rules. */
	    UErrorCode intStatus = U_ZERO_ERROR;
	    temp.constructFromRules(RuleBasedCollator::DEFAULTRULES, intStatus);
	    
	    if(U_SUCCESS(intStatus) && (temp.mPattern != 0))
	      {
		data->ruleTable = temp.getRules();
		data->isRuleTableLoaded = TRUE;
	      }
        }
    }

    return data->ruleTable;
}


Collator::EComparisonResult
RuleBasedCollator::compare( const UnicodeString& source,
                            const UnicodeString& target,
                            int32_t length) const
{
    UnicodeString source_togo;
    UnicodeString target_togo;
    UTextOffset begin=0;

    source.extract(begin, icu_min(length,source.size()), source_togo);
    target.extract(begin, icu_min(length,target.size()), target_togo);
    return (RuleBasedCollator::compare(source_togo, target_togo));
}

Collator::EComparisonResult   
RuleBasedCollator::compare(const   UChar* source, 
                      int32_t sourceLength,
                      const   UChar*  target,
                      int32_t targetLength) const
{
    // check if source and target are valid strings
    if (((source == 0) && (target == 0)) ||
        ((sourceLength == 0) && (targetLength == 0)))
    {
        return Collator::EQUAL;
    }

    Collator::EComparisonResult result = Collator::EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    if (cursor1 == NULL)
    {
        ((RuleBasedCollator *)this)->cursor1 = new NormalizerIterator(source, sourceLength, getDecomposition());
    }
    else
    {
        cursor1->setModeAndText(getDecomposition(), source, sourceLength, status);
    }

    if ( /*cursor1->cursor == NULL ||*/ U_FAILURE(status))
    {
        return Collator::EQUAL;
    }

    if (cursor2 == NULL)
    {
        ((RuleBasedCollator *)this)->cursor2 = new NormalizerIterator(target, targetLength, getDecomposition());
    }
    else
    {
        cursor2->setModeAndText(getDecomposition(), target, targetLength, status);
    }

    if (/*cursor2 == NULL ||*/ U_FAILURE(status))
    {
        return Collator::EQUAL;
    }

    int32_t sOrder, tOrder;
    //    int32_t sOrder = CollationElementIterator::NULLORDER, tOrder = CollationElementIterator::NULLORDER;
    bool_t gets = TRUE, gett = TRUE;
    bool_t initialCheckSecTer = getStrength() >= Collator::SECONDARY;
    bool_t checkSecTer = initialCheckSecTer;
    bool_t checkTertiary = getStrength() >= Collator::TERTIARY;
    bool_t isFrenchSec = data->isFrenchSec;
    uint32_t pSOrder, pTOrder;

    while(TRUE)
    {
        // Get the next collation element in each of the strings, unless
        // we've been requested to skip it.
        if (gets)
        {
            sOrder = getStrengthOrder((NormalizerIterator*)cursor1, status);

            if (U_FAILURE(status))
            {
                return Collator::EQUAL;
            }
        }

        gets = TRUE;

        if (gett)
        {
            tOrder = getStrengthOrder((NormalizerIterator*)cursor2, status);

            if (U_FAILURE(status))
            {
                return Collator::EQUAL;
            }
        }
        
        gett = TRUE;

        // If we've hit the end of one of the strings, jump out of the loop
        if ((sOrder == CollationElementIterator::NULLORDER)||
            (tOrder == CollationElementIterator::NULLORDER))
        {
            break;
        }

        // If there's no difference at this position, we can skip to the
        // next one.
        pSOrder = CollationElementIterator::primaryOrder(sOrder);
        pTOrder = CollationElementIterator::primaryOrder(tOrder);
        if (sOrder == tOrder)
        {
            if (isFrenchSec && pSOrder != 0)
            {
                if (!checkSecTer)
                {
                    // in french, a secondary difference more to the right is stronger,
                    // so accents have to be checked with each base element
                    checkSecTer = initialCheckSecTer;

                    // but tertiary differences are less important than the first 
                    // secondary difference, so checking tertiary remains disabled
                    checkTertiary = FALSE;
                }
            }

            continue;
        }

        // Compare primary differences first.
        if (pSOrder != pTOrder)
        {
            if (sOrder == 0)
            {
                // The entire source element is ignorable.
                // Skip to the next source element, but don't fetch another target element.
                gett = FALSE;
                continue;
            }

            if (tOrder == 0)
            {
                gets = FALSE;
                continue;
            }

            // The source and target elements aren't ignorable, but it's still possible
            // for the primary component of one of the elements to be ignorable....
            if (pSOrder == 0)  // primary order in source is ignorable
            {
                // The source's primary is ignorable, but the target's isn't.  We treat ignorables
                // as a secondary difference, so remember that we found one.
                if (checkSecTer)
                {
                    result = Collator::GREATER;  // (strength is SECONDARY)
                    checkSecTer = FALSE;
                }

                // Skip to the next source element, but don't fetch another target element.
                gett = FALSE;
            }
            else if (pTOrder == 0)
            {
                // record differences - see the comment above.
                if (checkSecTer)
                {
                    result = Collator::LESS;  // (strength is SECONDARY)
                    checkSecTer = FALSE;
                }

                // Skip to the next target element, but don't fetch another source element.
                gets = FALSE;
            }
            else
            {
                // Neither of the orders is ignorable, and we already know that the primary
                // orders are different because of the (pSOrder != pTOrder) test above.
                // Record the difference and stop the comparison.
                if (pSOrder < pTOrder)
                {
                    return Collator::LESS;  // (strength is PRIMARY)
                }

                return Collator::GREATER;  // (strength is PRIMARY)
            }
        }
        else
        { // else of if ( pSOrder != pTOrder )
            // primary order is the same, but complete order is different. So there
            // are no base elements at this point, only ignorables (Since the strings are
            // normalized)

            if (checkSecTer)
            {
                // a secondary or tertiary difference may still matter
                uint32_t secSOrder = CollationElementIterator::secondaryOrder(sOrder);
                uint32_t secTOrder = CollationElementIterator::secondaryOrder(tOrder);

                if (secSOrder != secTOrder)
                {
                    // there is a secondary difference
                    result = (secSOrder < secTOrder) ? Collator::LESS : Collator::GREATER;
                                            // (strength is SECONDARY)
                    checkSecTer = FALSE; 
                    // (even in french, only the first secondary difference within
                    //  a base character matters)
                }
                else
                {
                    if (checkTertiary)
                    {
                        // a tertiary difference may still matter
                        uint32_t terSOrder = CollationElementIterator::tertiaryOrder(sOrder);
                        uint32_t terTOrder = CollationElementIterator::tertiaryOrder(tOrder);

                        if (terSOrder != terTOrder)
                        {
                            // there is a tertiary difference
                            result = (terSOrder < terTOrder) ? Collator::LESS : Collator::GREATER;
                                            // (strength is TERTIARY)
                            checkTertiary = FALSE;
                        }
                    }
                }
            } // if (checkSecTer)

        }  // if ( pSOrder != pTOrder )
    } // while()

    if (sOrder != CollationElementIterator::NULLORDER)
    {
        // (tOrder must be CollationElementIterator::NULLORDER,
        //  since this point is only reached when sOrder or tOrder is NULLORDER.)
        // The source string has more elements, but the target string hasn't.
        do
        {
            if (CollationElementIterator::primaryOrder(sOrder) != 0)
            {
                // We found an additional non-ignorable base character in the source string.
                // This is a primary difference, so the source is greater
                return Collator::GREATER; // (strength is PRIMARY)
            }

            if (CollationElementIterator::secondaryOrder(sOrder) != 0)
            {
                // Additional secondary elements mean the source string is greater
                if (checkSecTer)
                {
                    result = Collator::GREATER;  // (strength is SECONDARY)
                    checkSecTer = FALSE;
                }
            } 
        }
        while ((sOrder = getStrengthOrder(cursor1, status)) != CollationElementIterator::NULLORDER);
    }
    else if (tOrder != CollationElementIterator::NULLORDER)
    {
        // The target string has more elements, but the source string hasn't.
        do
        {
            if (CollationElementIterator::primaryOrder(tOrder) != 0)
            {
                // We found an additional non-ignorable base character in the target string.
                // This is a primary difference, so the source is less
                return Collator::LESS; // (strength is PRIMARY)
            }

            if (CollationElementIterator::secondaryOrder(tOrder) != 0)
            {
                // Additional secondary elements in the target mean the source string is less
                if (checkSecTer)
                {
                    result = Collator::LESS;  // (strength is SECONDARY)
                    checkSecTer = FALSE;
                }
            } 
        }
        while ((tOrder = getStrengthOrder(cursor2, status)) != CollationElementIterator::NULLORDER);
    }


    // For IDENTICAL comparisons, we use a bitwise character comparison
    // as a tiebreaker if all else is equal
    // NOTE: The java code compares result with 0, and 
    // puts the result of the string comparison directly into result
    if (result == Collator::EQUAL && getStrength() == IDENTICAL)
    {
#if 0
      // ******** for the  UChar normalization interface.
      // It doesn't work much faster, and the code was broken
      // so it's commented out. --srl
//          UChar sourceDecomp[1024], targetDecomp[1024];
//  	int32_t sourceDecompLength = 1024;
//  	int32_t targetDecompLength = 1024;
	
//          int8_t comparison;
//  	Normalizer::EMode decompMode = getDecomposition();
        
//  	if (decompMode != Normalizer::NO_OP)
//  	  {
//  	    Normalizer::normalize(source, sourceLength, decompMode,
//  				  0, sourceDecomp, sourceDecompLength, status);
	    
//  	    Normalizer::normalize(target, targetLength, decompMode,
//  				  0, targetDecomp, targetDecompLength, status);
	    
//  	    comparison = u_strcmp(sourceDecomp,targetDecomp);
//  	  }
//  	else
//  	  {
//  	    comparison = u_strcmp(source, target); /* ! */
//  	  }

#else

	UnicodeString sourceDecomp, targetDecomp;

        int8_t comparison;
        
        Normalizer::normalize(source, getDecomposition(), 
                      0, sourceDecomp,  status);

        Normalizer::normalize(target, getDecomposition(), 
                      0, targetDecomp,  status);
        
        comparison = sourceDecomp.compare(targetDecomp);
#endif

        if (comparison < 0)
        {
            result = Collator::LESS;
        }
        else if (comparison == 0)
        {
            result = Collator::EQUAL;
        }
        else
        {
            result = Collator::GREATER;
        }
    }

    return result;
}


int32_t
RuleBasedCollator::nextContractChar(NormalizerIterator *cursor, 
                                    UChar ch,
                                    UErrorCode& status) const
{
    // First get the ordering of this single character
    VectorOfPToContractElement *list = getContractValues(ch);
    EntryPair *pair = (EntryPair *)list->at(0);
    int32_t order = pair->value;

    // Now iterate through the chars following it and
    // look for the longest match
    ((UnicodeString&)key).remove();
    ((UnicodeString&)key) += ch;

    while ((ch = cursor->current()) != Normalizer::DONE)
    {
        ((UnicodeString&)key) += ch;

        int32_t n = getEntry(list, key, TRUE);

        if (n == UNMAPPED)
        {
            break;
        }
        cursor->next();

        pair = (EntryPair *)list->at(n);
        order = pair->value;
    }

    return order;
}

// Compare two strings using this collator
Collator::EComparisonResult
RuleBasedCollator::compare(const UnicodeString& source,
                        const UnicodeString& target) const
{
    return compare(source.getUChars(), source.length(), target.getUChars(), target.length());
}

// Retrieve a collation key for the specified string
// The key can be compared with other collation keys using a bitwise comparison
// (e.g. memcmp) to find the ordering of their respective source strings.
// This is handy when doing a sort, where each sort key must be compared
// many times.
//
// The basic algorithm here is to find all of the collation elements for each
// character in the source string, convert them to an ASCII representation,
// and put them into the collation key.  But it's trickier than that.
// Each collation element in a string has three components: primary ('A' vs 'B'),
// secondary ('u' vs 'ü'), and tertiary ('A' vs 'a'), and a primary difference
// at the end of a string takes precedence over a secondary or tertiary
// difference earlier in the string.
//
// To account for this, we put all of the primary orders at the beginning of the
// string, followed by the secondary and tertiary orders. Each set of orders is
// terminated by nulls so that a key for a string which is a initial substring of
// another key will compare less without any special case.
//
// Here's a hypothetical example, with the collation element represented as
// a three-digit number, one digit for primary, one for secondary, etc.
//
// String:              A     a     B    É
// Collation Elements: 101   100   201  511
// Collation Key:      1125<null>0001<null>1011<null>
//
// To make things even trickier, secondary differences (accent marks) are compared
// starting at the *end* of the string in languages with French secondary ordering.
// But when comparing the accent marks on a single base character, they are compared
// from the beginning.  To handle this, we reverse all of the accents that belong
// to each base character, then we reverse the entire string of secondary orderings
// at the end.
//
CollationKey&
RuleBasedCollator::getCollationKey( const   UnicodeString&  source,
                                    CollationKey&   sortkey,
                                    UErrorCode&      status) const
{
    return RuleBasedCollator::getCollationKey(source.getUChars(), source.size(), sortkey, status);
}

CollationKey&
RuleBasedCollator::getCollationKey( const   UChar*  source,
                                    int32_t sourceLen,
                                    CollationKey&   sortkey,
                                    UErrorCode&      status) const
{
    if (U_FAILURE(status))
    {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return sortkey.setToBogus();
    }
    
    if ((!source) || (sourceLen == 0))
    {
        return sortkey.reset();
    }

    if (cursor1 == NULL)
    {
      ((RuleBasedCollator *)this)->cursor1 = new NormalizerIterator(source, sourceLen, getDecomposition());
    }
    else
    {
      cursor1->setModeAndText(getDecomposition(), source,sourceLen, status);
    }

    if (U_FAILURE(status))
    {
        return sortkey.setToBogus();
    }

    bool_t  compareSec   = (getStrength() >= Collator::SECONDARY);
    bool_t  compareTer   = (getStrength() >= Collator::TERTIARY);
    bool_t  compareIdent = (getStrength() == Collator::IDENTICAL);
    int32_t order        = 0;
    int32_t totalPrimary = 0;
    int32_t totalSec     = 0;
    int32_t totalTer     = 0;
    int32_t totalIdent     = 0;
    UnicodeString decomp;

    // iterate over the source, counting primary, secondary, and tertiary entries
    while((order = getStrengthOrder((NormalizerIterator*)cursor1, status)) !=
	                                      CollationElementIterator::NULLORDER)
    {
        int32_t secOrder = CollationElementIterator::secondaryOrder(order);
        int32_t terOrder = CollationElementIterator::tertiaryOrder(order);

        if (U_FAILURE(status))
        {
            return sortkey.setToBogus();
        }

        if (! CollationElementIterator::isIgnorable(order))
        {
            totalPrimary += 1;

            if (compareSec)
            {
                totalSec += 1;
            }

            if (compareTer)
            {
                totalTer += 1;
            }
        }
        else
        {
            if (compareSec && secOrder != 0)
            {
                totalSec += 1;
            }

            if (compareTer && terOrder != 0)
            {
                totalTer += 1;
            }
        }
    }

    // count the null bytes after the entires
    totalPrimary += 1;

    if (compareSec)
    {
        totalSec += 1;
    }

    if (compareTer)
    {
        totalTer += 1;
    }

    if (compareIdent)
    {
      Normalizer::normalize(source, getDecomposition(), // SRL: ??
                0, decomp, status);

        if (U_SUCCESS(status))
        {
            totalIdent = decomp.size() + 1;
        }
    }

    // Compute total number of bytes to hold the entries
    // and make sure the key can hold them
    uint32_t size   = 2 * (totalPrimary + totalSec + totalTer + totalIdent);

    sortkey.ensureCapacity(size);

    if (sortkey.isBogus())
    {
        status = U_MEMORY_ALLOCATION_ERROR;
        return sortkey;
    }

    int32_t primaryCursor = 0;
    int32_t secCursor     = 2 * totalPrimary;
    int32_t secBase       = secCursor;
    int32_t preSecIgnore  = secBase;
    int32_t terCursor     = secCursor + (2 * totalSec);
    int32_t identCursor      = terCursor + (2 * totalTer);

    // reset source to the beginning
    cursor1->reset();

    // now iterate over the source computing the actual entries
    while((order = getStrengthOrder((NormalizerIterator*)cursor1, status)) != CollationElementIterator::NULLORDER)
    {
        if (U_FAILURE(status))
        {
            return sortkey.reset();
        }

        int32_t primaryOrder = CollationElementIterator::primaryOrder(order);
        int32_t secOrder     = CollationElementIterator::secondaryOrder(order);
        int32_t terOrder     = CollationElementIterator::tertiaryOrder(order);

        if (! CollationElementIterator::isIgnorable(order))
        {
            primaryCursor = sortkey.storeBytes(primaryCursor, primaryOrder + SORTKEYOFFSET);

            if (compareSec)
            {
                if (data->isFrenchSec && (preSecIgnore < secCursor))
                {
                    sortkey.reverseBytes(preSecIgnore, secCursor);
                }

                secCursor = sortkey.storeBytes(secCursor, secOrder + SORTKEYOFFSET);

                preSecIgnore = secCursor;
            }

            if (compareTer)
            {
                terCursor = sortkey.storeBytes(terCursor, terOrder + SORTKEYOFFSET);
            }
        }
        else
        {
            if (compareSec && secOrder != 0)
            {
                secCursor = sortkey.storeBytes(secCursor, secOrder + data->maxSecOrder + SORTKEYOFFSET);
            }

            if (compareTer && terOrder != 0)
            {
                terCursor = sortkey.storeBytes(terCursor, terOrder + data->maxTerOrder + SORTKEYOFFSET);
            }
        }
    }

    // append 0 at the end of each portion.
    sortkey.storeBytes(primaryCursor, 0);

    if (compareSec)
    {
        if (data->isFrenchSec)
        {
            if (preSecIgnore < secCursor)
            {
                sortkey.reverseBytes(preSecIgnore, secCursor);
            }

            sortkey.reverseBytes(secBase, secCursor);
        }

        sortkey.storeBytes(secCursor, 0);
    }

    if (compareTer)
    {
        sortkey.storeBytes(terCursor, 0);
    }

    if (compareIdent)
    {
        sortkey.storeUnicodeString(identCursor, decomp);
    }

    //    Debugging - print out the sortkey [--srl]
//      {
//        const uint8_t *bytes;
//        int32_t xcount;
//        bytes = sortkey.getByteArray(xcount);
//        //      fprintf(stderr, "\n\n-  [%02X] [%02X]\n\n", (int)(bytes[0]&0xFF), (int)(bytes[1]&0xFF) );
//      }

    return sortkey;
}


// Build this collator's rule tables based on a string representation of the rules
// See the big diagram at the top of this file for an overview of how the tables
// are organized.
void
RuleBasedCollator::build(const UnicodeString&   pattern,
                            UErrorCode&      status)
{
    if (U_FAILURE(status))
    {
        return;
    }

    // This array maps Unicode characters to their collation ordering
    data->mapping = ucmp32_open(UNMAPPED);

    if (data->mapping->fBogus)
    {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    Collator::ECollationStrength aStrength = Collator::IDENTICAL;
    bool_t isSource = TRUE;
    int32_t i = 0;
    UnicodeString lastGroupChars;
    UnicodeString expChars;
    UnicodeString groupChars;

    if (pattern.size() == 0)
    {
        status = U_INVALID_FORMAT_ERROR;
        return;
    }

    // Build the merged collation entries
    // Since rules can be specified in any order in the string
    // (e.g. "c , C < d , D < e , E .... C < CH")
    // this splits all of the rules in the string out into separate
    // objects and then sorts them.  In the above example, it merges the
    // "C < CH" rule in just before the "C < D" rule.

    mPattern = new MergeCollation(pattern, getDecomposition(), status);
    if (U_FAILURE(status))
    {
        ucmp32_close(data->mapping);
        data->mapping = 0;
        delete mPattern;
        mPattern = 0;
        return;
    }

    int32_t order = 0;

    // Walk through each entry
    for (i = 0; i < mPattern->getCount(); ++i)
    {
        const PatternEntry* entry = mPattern->getItemAt(i);
        groupChars.remove();
        expChars.remove();

        // if entry is valid
        if (entry != NULL)
        {
            entry->getChars(groupChars);

            // check if french secondary needs to be turned on
            if ((groupChars.size() > 1) &&
                (groupChars[groupChars.size()-(T_INT32(1))] == 0x0040))
            {
                data->isFrenchSec = TRUE;
                groupChars.remove(groupChars.size()-(T_INT32(1)));
            }

            order = increment((Collator::ECollationStrength)entry->getStrength(), order);

            if (entry->getExtension(expChars).size() != 0)
            {
                // encountered an expanding character, where one character on input
                // expands to several sort elements (e.g. 'ö' --> 'o' 'e')
                addExpandOrder(groupChars, expChars, order, status);
                if (U_FAILURE(status))
                {
                    return;
                }
            }
            else if (groupChars.size() > 1)
            {
                // encountered a contracting character, where several characters on input
                // contract into one sort order.  For example, "ch" is treated as a single
                // character in traditional Spanish sorting.
                addContractOrder(groupChars, order, status);
                if (U_FAILURE(status))
                {
                    return;
                }
            }
            else
            {
                // Nothing out of the ordinary -- one character maps to one sort order
                addOrder(groupChars[0], order, status);
                if (U_FAILURE(status))
                {
                    return;
                }
            }
        }
    }

    // add expanding entries for pre-composed characters
    addComposedChars();

    // Fill in all the expanding chars values
    commit();

    // Compact the data mapping table
    ucmp32_compact(data->mapping, 1);
}

/**
 * Add expanding entries for pre-composed unicode characters so that this
 * collator can be used reasonably well with decomposition turned off.
 */
 void RuleBasedCollator::addComposedChars()
 {
    UnicodeString buf;
    UErrorCode status = U_ZERO_ERROR;

    // Iterate through all of the pre-composed characters in Unicode
    ComposedCharIter iter;
    UnicodeString decomp;

    while (iter.hasNext())
    {
        UChar c = iter.next();
        
        if (getCharOrder(c) == UNMAPPED)
        {
            // 
            // We don't already have an ordering for this pre-composed character.
            //
            // First, see if the decomposed string is already in our
            // tables as a single contracting-string ordering.
            // If so, just map the precomposed character to that order.
            //
            // TODO: What we should really be doing here is trying to find the
            // longest initial substring of the decomposition that is present
            // in the tables as a contracting character sequence, and find its
            // ordering.  Then do this recursively with the remaining chars
            // so that we build a list of orderings, and add that list to
            // the expansion table. 
            // That would be more correct but also significantly slower, so
            // I'm not totally sure it's worth doing.
            //
            iter.getDecomposition(decomp);
            int contractOrder = getContractOrder(decomp);

            if (contractOrder != UNMAPPED)
            {
                addOrder(c, contractOrder, status);
            }
            else
            {
                //
                // We don't have a contracting ordering for the entire string
                // that results from the decomposition, but if we have orders
                // for each individual character, we can add an expanding
                // table entry for the pre-composed character 
                //
                bool_t allThere = TRUE;
                int32_t i;

                for (i = 0; i < decomp.size(); i += 1)
                {
                    if (getCharOrder(decomp[i]) == UNMAPPED)
                    {
                        allThere = FALSE;
                        break;
                    }
                }

                if (allThere)
                {
                    buf.remove();
                    buf += c;
                    addExpandOrder(buf, decomp, UNMAPPED, status);
                }
            }
        }
    }
}
    
// When the expanding character tables are built by addExpandOrder,
// it doesn't know what the final ordering of each character
// in the expansion will be.  Instead, it just puts the raw character
// code into the table, adding CHARINDEX as a flag.  Now that we've
// finished building the mapping table, we can go back and look up
// that character to see what its real collation order is and
// stick that into the expansion table.  That lets us avoid doing
// a two-stage lookup later.

void
RuleBasedCollator::commit()
{
    // if there are any expanding characters
    if (data->expandTable != NULL)
    {
        int32_t i;
        for (i = 0; i < data->expandTable->size(); i += 1)
        {
            VectorOfInt* valueList = data->expandTable->at(i);
            int32_t j;
            for (j = 0; j < valueList->size(); j++)
            {
                // found a expanding character
                // the expanding char value is not filled in yet
                if ((valueList->at(j) < EXPANDCHARINDEX) &&
                    (valueList->at(j) > CHARINDEX))
                {
                    // Get the real values for the non-filled entry
                    UChar ch = (UChar)(valueList->at(j) - CHARINDEX);
                    int32_t realValue = ucmp32_get(data->mapping, ch);

                    if (realValue == UNMAPPED)
                    {
                        // The real value is still unmapped, maybe it'signorable
                        valueList->atPut(j, IGNORABLEMASK & ch);
                    }
                    // fill in the value
                    else
                    {
                        valueList->atPut(j, realValue);
                    }
                }
            }
        }
    }
 }

/**
 *  Increment of the last order based on the comparison level.
 */
int32_t
RuleBasedCollator::increment(Collator::ECollationStrength aStrength, int32_t lastValue)
{
    switch(aStrength)
    {
    case Collator::PRIMARY:
        // increment priamry order  and mask off secondary and tertiary difference
        lastValue += PRIMARYORDERINCREMENT;
        lastValue &= PRIMARYORDERMASK;
        isOverIgnore = TRUE;
        break;

    case Collator::SECONDARY:
        // increment secondary order and mask off tertiary difference
        lastValue += SECONDARYORDERINCREMENT;
        lastValue &= SECONDARYDIFFERENCEONLY;

        // record max # of ignorable chars with secondary difference
        if (isOverIgnore == FALSE)
        {
            data->maxSecOrder += 1;
        }
        break;

    case Collator::TERTIARY:
        // increment tertiary order
        lastValue += TERTIARYORDERINCREMENT;

        // record max # of ignorable chars with tertiary difference
        if (isOverIgnore == FALSE)
        {
            data->maxTerOrder += 1;
        }
        break;

  // case IDENTICAL?  
    }

    return lastValue;
}

// Adds a character and its designated order into the collation table.
// This is the simple case, with no expansion or contraction
void
RuleBasedCollator::addOrder(UChar ch,
                         int32_t anOrder,
                         UErrorCode& status)
{
    if (U_FAILURE(status))
    {
        return;
    }

    // try to find the order of the char in the mapping table
    int32_t order = ucmp32_get(data->mapping, ch);

    if (order >= CONTRACTCHARINDEX)
    {
        // There's already an entry for this character that points to a contracting
        // character table.  Instead of adding the character directly to the mapping
        // table, we must add it to the contract table instead.
        key.remove();
        key += ch;
        if (key.isBogus())
        {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        addContractOrder(key, anOrder, status);
    }
    else
    {
        // add the entry to the mapping table, the same later entry replaces the previous one
        ucmp32_set(data->mapping, ch, anOrder);
    }
}

// Add an expanding-character entry to the table.
void
RuleBasedCollator::addExpandOrder(  const   UnicodeString& contractChars,
                                const   UnicodeString& expandChars,
                                int32_t anOrder,
                                UErrorCode& status)
{
    if (U_FAILURE(status))
    {
        return;
    }

    // Create an expansion table entry
    int32_t tableIndex = addExpansion(anOrder, expandChars);
    
    // And add its index into the main mapping table
    if (contractChars.size() > 1)
    {
        addContractOrder(contractChars, tableIndex, status);
    }
    else
    {
        addOrder(contractChars[0], tableIndex, status);
    }
}

int32_t RuleBasedCollator::addExpansion(int32_t anOrder, const UnicodeString &expandChars)
{
    if (data->expandTable == NULL)
    {
        data->expandTable = new VectorOfPToExpandTable();

        if (data->expandTable == NULL)
        {
            return 0;
        }
    }
    
    // If anOrder is valid, we want to add it at the beginning of the list
    int32_t offset = (anOrder == UNMAPPED) ? 0 : 1;
    
    VectorOfInt *valueList = new VectorOfInt(expandChars.size() + offset);

    if (offset == 1)
    {
        valueList->atPut(0, anOrder);
    }

    int32_t i;
    for (i = 0; i < expandChars.size(); i += 1)
    {
        UChar ch = expandChars[i];
        int32_t mapValue = getCharOrder(ch);
        
        if (mapValue != UNMAPPED)
        {
            valueList->atPut(i + offset, mapValue);
        }
        else
        {
            // can't find it in the table, will be filled in by commit().
            valueList->atPut(i + offset, CHARINDEX + (int32_t)ch);
        }
    }

    // Add the expanding char list into the expansion table.
    int32_t tableIndex = EXPANDCHARINDEX + data->expandTable->size();
    data->expandTable->atPut(data->expandTable->size(), valueList);
    
    return tableIndex;
}

// Add a string of characters that contracts into a single ordering.
void
RuleBasedCollator::addContractOrder(const   UnicodeString& groupChars,
                                    int32_t anOrder,
                                    bool_t fwd,
                                    UErrorCode& status)
{
    if (U_FAILURE(status))
    {
        return;
    }

    if (data->contractTable == NULL)
    {
        data->contractTable = new VectorOfPToContractTable();
        if (data->contractTable->isBogus())
        {
            delete data->contractTable;
            data->contractTable = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
    }

    // See if the initial character of the string already has a contract table.
    // e.g. for "ch", look for 'c'.
    int32_t entry = ucmp32_get(data->mapping, groupChars[0]);
    VectorOfPToContractElement *entryTable = getContractValues(entry - CONTRACTCHARINDEX);

    if (entryTable == NULL)
    {
        // We need to create a new table of contract entries for this base char
        int32_t tableIndex = CONTRACTCHARINDEX + data->contractTable->size();
        EntryPair *pair = NULL;
        UnicodeString substring;

        entryTable = new VectorOfPToContractElement();
        if (entryTable->isBogus())
        {
            delete entryTable;
            delete data->contractTable;
            data->contractTable = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        data->contractTable->atPut(data->contractTable->size(), entryTable);
        if (data->contractTable->isBogus())
        {
            delete entryTable;
            delete data->contractTable;
            data->contractTable = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
            

        // Add the initial character's current ordering first. then
        // update its mapping to point to this contract table
        groupChars.extract(0, 1, substring);
        if (substring.isBogus())
        {
            delete entryTable;
            delete data->contractTable;
            data->contractTable = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        pair = new EntryPair(substring, entry);

        entryTable->atPut(0, pair);
        if (entryTable->isBogus())
        {
            delete entryTable;
            delete data->contractTable;
            data->contractTable = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        ucmp32_set(data->mapping, groupChars[0], tableIndex);
    }

    // Now add (or replace) this string in the table
    int32_t index = getEntry(entryTable, groupChars, fwd);

    if (index != UNMAPPED)
    {
        EntryPair *pair = (EntryPair *) entryTable->at(index);
        pair->value = anOrder;
    }
    else
    {
        EntryPair *pair = new EntryPair(groupChars, anOrder, fwd);

        entryTable->atPut(entryTable->size(), pair);
    }
    
    // If this was a forward mapping for a contracting string, also add a
    // reverse mapping for it, so that CollationElementIterator::previous
    // can work right
    if (fwd)
    {
        UnicodeString reverse(groupChars);

        if (reverse.isBogus())
        {
            delete entryTable;
            delete data->contractTable;
            data->contractTable = NULL;
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        addContractOrder(reverse.reverse(), anOrder, FALSE, status);
    }
}

/**
 * If the given string has been specified as a contracting string
 * in this collation table, return its ordering.
 * Otherwise return UNMAPPED.
 */
 int32_t RuleBasedCollator::getContractOrder(const UnicodeString &groupChars) const
{
    int32_t result = UNMAPPED;

    if (data->contractTable != NULL)
    {
        VectorOfPToContractElement *entryTable = getContractValues(groupChars[0]);

        if (entryTable != NULL)
        {
            int32_t index = getEntry(entryTable, groupChars, TRUE);

            if (index != UNMAPPED)
            {
                EntryPair *pair = entryTable->at(index);

                result = pair->value;
            }
        }
    }

    return result;
}

int32_t RuleBasedCollator::getCharOrder(UChar ch) const
{
    int32_t order = ucmp32_get(data->mapping, ch);
    
    if (order >= CONTRACTCHARINDEX)
    {
        VectorOfPToContractElement *groupList = getContractValues(order - CONTRACTCHARINDEX);
        EntryPair *pair = groupList->at(0);

        order = pair->value;
    }

    return order;
}
    
// Create a hash code for this collation.  Just hash the main rule table --
// that should be good enough for almost any use.
int32_t
RuleBasedCollator::hashCode() const
{
    int32_t         value = 0;
    int32_t         c;
    int32_t         count = getRules().size();
    UTextOffset      pos = count - 1;

    if (count > 64)
    {
        count = 64; // only hash upto limit
    }

    int16_t i = 0;

    while (i < count)
    {
        c = data->ruleTable[pos];
        value = ((value << (c & 0x0f)) ^ (c << 8)) + (c ^ value);
        i += 1;
        pos -= 1;
    }

    if (value == 0)
    {
        value = 1;
    }

    return value;
}

// find the contracting char entry in the list
int32_t
RuleBasedCollator::getEntry(VectorOfPToContractElement* list,
                         const UnicodeString& name,
                         bool_t fwd)
{
    int32_t i;

    if (list != NULL)
    {
        for (i = 0; i < list->size(); i += 1)
        {
            EntryPair *pair = list->at(i);

            if ((pair != NULL) && (pair->fwd == fwd) && (pair->entryName == name))
            {
                return i;
            }
        }
    }

    return RuleBasedCollator::UNMAPPED;
}

// look for the contracting list entry with the beginning char
VectorOfPToContractElement*
RuleBasedCollator::getContractValues(UChar ch) const
{
    int32_t index = ucmp32_get(data->mapping, ch);
    return getContractValues(index - CONTRACTCHARINDEX);
}

// look for the contracting list entry with the index
VectorOfPToContractElement*
RuleBasedCollator::getContractValues(int32_t    index) const
{
    if (data->contractTable != NULL)
    {
        if (index >= 0)
        {
            return data->contractTable->at(index);
        }
    }
    return NULL;
}

/**
  * Return the maximum length of any expansion sequences that end
  * with the specified comparison order.
  *
  * @param order a collation order returned by previous or next.
  * @return the maximum length of any expansion seuences ending
  *         with the specified order.
  *
  * @see CollationElementIterator#getMaxExpansion
  */
int32_t RuleBasedCollator::getMaxExpansion(int32_t order) const
{
    int32_t result = 1;
    
    if (data->expandTable != NULL)
    {
        // Right now this does a linear search through the entire
        // expandsion table.  If a collator had a large number of expansions,
        // this could cause a performance problem, but in practice that
        // rarely happens
        int32_t i;
        for (i = 0; i < data->expandTable->size(); i += 1)
        {
            VectorOfInt *valueList = data->expandTable->at(i);
            int32_t length = valueList->size();
            
            if (length > result && valueList->at(length-1) == order)
            {
                result = length;
            }
        }
    }

    return result;
}
    
/**
 *  Get the entry of hash table of the expanding string in the collation
 *  table.
 *  @param idx the index of the expanding string value list
 */
VectorOfInt *RuleBasedCollator::getExpandValueList(int32_t order) const
{
    return data->expandTable->at(order - EXPANDCHARINDEX);
}



void RuleBasedCollatorStreamer::streamIn(RuleBasedCollator* collator, FileStream* is)
{
    if (!T_FileStream_error(is))
    {
        // Check that this is the correct file type
        int16_t id;

        T_FileStream_read(is, &id, sizeof(id));
        if (id != collator->FILEID)
        {
            // This isn't the right type of file.  Mark the ios
            // as failing and return.
            T_FileStream_setError(is); // force the stream to set its error flag
            return;
        }

        // Stream in large objects
        char isNull;

        T_FileStream_read(is, &isNull, sizeof(isNull));
        if (isNull)
        {
            delete collator->data;
            collator->data = NULL;
        }
        else
        {
            if (collator->data == NULL)
            {
                collator->data = new TableCollationData;
            }
            
            collator->data->streamIn(is);
            if (collator->data->isBogus()) {
                T_FileStream_setError(is); // force the stream to set its error flag
                return;
            }
        }

        // Verify that the end marker is present
        T_FileStream_read(is, &id, sizeof(id));
        if (id != collator->FILEID)
        {
            // This isn't the right type of file.  Mark the ios
            // as failing and return.
            T_FileStream_setError(is); // force the stream to set its error flag
            return;
        }

        // Reset other data members
        collator->isOverIgnore = FALSE;
        collator->lastChar = 0;
        delete collator->mPattern;
        collator->mPattern = 0;
        collator->key.remove();
        collator->dataIsOwned = TRUE;
    }
}

void RuleBasedCollatorStreamer::streamOut(const RuleBasedCollator* collator, FileStream* os)
{
    if (!T_FileStream_error(os))
    {
        // We use a 16-bit ID code to identify this file.
        int16_t id = collator->FILEID;
        T_FileStream_write(os, &id, sizeof(id));

        // Stream out the data
        char isNull;
        isNull = (collator->data == 0);
        T_FileStream_write(os, &isNull, sizeof(isNull));

        if (!isNull)
        {
            collator->data->streamOut(os);
        }

        // Write out the ID to indicate the end
        T_FileStream_write(os, &id, sizeof(id));
    }
}

bool_t RuleBasedCollator::writeToFile(const char* fileName) const
{
    FileStream* ofs = T_FileStream_open(fileName, "wb");
    if (ofs != 0)
    {
        RuleBasedCollatorStreamer::streamOut(this, ofs);
    }

#ifdef COLLDEBUG
    fprintf(stderr, "binary write %s size %d %s\n", fileName, T_FileStream_size(ofs),
        (!T_FileStream_error(ofs) ? ", OK" : ", FAIL"));
#endif

    bool_t err = T_FileStream_error(ofs) == 0;

    T_FileStream_close(ofs);
    return err;
}

void RuleBasedCollator::addToCache(const UnicodeString& key)
{
    // This method doesn't add the RuleBasedCollator itself to the cache.  Instead,
    // it adds the given RuleBasedCollator's data object to the TableCollationData
    // cache, and marks it as non-owned in the given RuleBasedCollator object.
    TableCollationData::addToCache(key, data);
    dataIsOwned = FALSE;
}

void
RuleBasedCollator::constructFromCache(const UnicodeString& key,
                                   UErrorCode& status)
{
    // Attempt to construct this RuleBasedCollator object from cached TableCollationData.
    // If no such data is in the cache, return false.
    if (U_FAILURE(status)) return;
    if (dataIsOwned)
    {
        delete data;
        data = NULL;
    }

    isOverIgnore = FALSE;
    lastChar = 0;
    mPattern = 0;
    setStrength(Collator::TERTIARY);

    dataIsOwned = FALSE;
    data = TableCollationData::findInCache(key);
    if (data == NULL)
    {
        status = U_MISSING_RESOURCE_ERROR;
    }
}

char*
RuleBasedCollator::createPathName(  const UnicodeString&    prefix,
                                const UnicodeString&    name,
                                const UnicodeString&    suffix)
{
    // Concatenate three elements to form a file name, and return it.

    UnicodeString   workingName(prefix);
    int32_t         size;
    char*           returnVal;

    workingName += name;
    workingName += suffix;

    size = workingName.size();
    returnVal = new char[size + 1];
    workingName.extract(0, size, returnVal, "");
    returnVal[size] = 0;

    return returnVal;
}

void
RuleBasedCollator::chopLocale(UnicodeString& localeName)
{
    // chopLocale removes the final element from a locale string.
    // For instance, "de_CH" becomes "de", and "de" becomes "".
    // "" remains "".

    int32_t     size = localeName.size();
    int32_t     i;

    for (i = size - 1; i > 0; i--)
    {
        if (localeName[i] == 0x005F)
        {
            break;
        }
    }

    if (i < 0)
    {
        i = 0;
    }

    localeName.remove(i, size - i);
}

//eof
