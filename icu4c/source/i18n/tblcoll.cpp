/*
******************************************************************************
* Copyright © {1996-1999}, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *     
******************************************************************************
*/

/**
* Created by : Syn Wee Quek
*/

#include "ucolimp.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "../common/uhash.h"
#include "unicode/resbund.h"

#ifdef _DEBUG
  #include "unistrm.h"
#endif

// forward declarations -----------------------------------------------------

U_CAPI const UChar * U_EXPORT2 ucol_getDefaultRulesArray(uint32_t *size);

UChar forwardCharIteratorGlue(void *iterator);

// global variable ----------------------------------------------------------

/*
synwee : using another name for this
const uint32_t tblcoll_StackBufferLen = 1024;
*/
const uint32_t StackBufferLen = 1024;

// RuleBasedCollator declaration -----------------------------------------

//---------------------------------------------------------------------------
// The following diagram shows the data structure of the RuleBasedCollator 
// object. Suppose we have the rule, where 'o-umlaut' is the unicode char 
// 0x00F6. "a, A < b, B < c, C, ch, cH, Ch, CH < d, D ... < o, O; 
// 'o-umlaut'/E, 'O-umlaut'/E ...".
// What the rule says is, sorts 'ch'ligatures and 'c' only with tertiary 
// difference and sorts 'o-umlaut' as if it's always expanded with 'e'.
//
// mapping table                 contracting list             expanding list
// (contains all unicode 
//  char entries)               ___    ____________      _________________________
//  ________                |=>|_*_|->|'c' |v('c') | |=>|v('o')|v('umlaut')|v('e')|
// |_\u0001_|-> v('\u0001') |  |_:_|  |------------| |  |-------------------------|
// |_\u0002_|-> v('\u0002') |  |_:_|  |'ch'|v('ch')| |  |             :           |
// |____:___|               |  |_:_|  |------------| |  |-------------------------|
// |____:___|               |         |'cH'|v('cH')| |  |             :           |
// |__'a'___|-> v('a')      |         |------------| |  |-------------------------|
// |__'b'___|-> v('b')      |         |'Ch'|v('Ch')| |  |             :           |
// |____:___|               |         |------------| |  |-------------------------|
// |____:___|               |         |'CH'|v('CH')| |  |             :           |
// |___'c'__|----------------          ------------  |  |-------------------------|
// |____:___|                                        |  |             :           |
// |o-umlaut|----------------------------------------   |_________________________|
// |____:___|
//
//---------------------------------------------------------------------------

// public RuleBasedCollator constructor ----------------------------------

/**
* Copy constructor
*/
RuleBasedCollator::RuleBasedCollator(const RuleBasedCollator& that) : 
              Collator(that), dataIsOwned(FALSE), ucollator(that.ucollator)
{
}

RuleBasedCollator::RuleBasedCollator(const UnicodeString& rules,
                                           UErrorCode& status) : 
                                           dataIsOwned(FALSE) 
{
  if (U_FAILURE(status))
    return;
 
  int32_t length = rules.length();

	UChar ucharrules[StackBufferLen];
	UChar *pucharrules = ucharrules;
	
  if (length >= StackBufferLen)
		pucharrules = new UChar[length + 1];
	
	rules.extract(0, length, pucharrules);
  pucharrules[length] = 0;

  ucollator = ucol_openRules(pucharrules, length, UCOL_DEFAULT_NORMALIZATION, 
                             UCOL_DEFAULT_STRENGTH, &status);
  if (U_SUCCESS(status))
    dataIsOwned = TRUE;
  
	if (pucharrules != ucharrules)
		delete[] pucharrules;
}

RuleBasedCollator::RuleBasedCollator(const UnicodeString& rules,
                      ECollationStrength collationStrength,
                      UErrorCode& status) : dataIsOwned(FALSE)
{
  if (U_FAILURE(status))
    return;
 
  int32_t length = rules.length();

	UChar ucharrules[StackBufferLen];
	UChar *pucharrules = ucharrules;
	
  if (length >= StackBufferLen)
		pucharrules = new UChar[length + 1];
	
	rules.extract(0, length, pucharrules);
  pucharrules[length] = 0;

  UCollationStrength strength = getUCollationStrength(collationStrength);
  ucollator = ucol_openRules(pucharrules, length, UCOL_DEFAULT_NORMALIZATION, 
                             strength, &status);

  if (U_SUCCESS(status))
    dataIsOwned = TRUE;
  
	if (pucharrules != ucharrules)
		delete[] pucharrules;
}

RuleBasedCollator::RuleBasedCollator(const UnicodeString& rules,
                                     Normalizer::EMode decompositionMode,
                                     UErrorCode& status) : 
                                     dataIsOwned(FALSE)
{
  if (U_FAILURE(status))
    return;
 
  int32_t length = rules.length();

	UChar ucharrules[StackBufferLen];
	UChar *pucharrules = ucharrules;
	
  if (length >= StackBufferLen)
		pucharrules = new UChar[length + 1];
	
	rules.extract(0, length, pucharrules);
  pucharrules[length] = 0;

  UNormalizationMode mode = getUNormalizationMode(decompositionMode);
  ucollator = ucol_openRules(pucharrules, length, mode, 
                             UCOL_DEFAULT_STRENGTH, &status);

  if (U_SUCCESS(status))
    dataIsOwned = TRUE;
  
	if (pucharrules != ucharrules)
		delete[] pucharrules;
}

RuleBasedCollator::RuleBasedCollator(const UnicodeString& rules,
                      ECollationStrength collationStrength,
                      Normalizer::EMode decompositionMode,
                      UErrorCode& status) : dataIsOwned(FALSE)
{
  if (U_FAILURE(status))
    return;

  int32_t length = rules.length();

	UChar ucharrules[StackBufferLen];
	UChar *pucharrules = ucharrules;
	
  if (length >= StackBufferLen)
		pucharrules = new UChar[length + 1];
	
	rules.extract(0, length, pucharrules);
  pucharrules[length] = 0;

  UCollationStrength strength = getUCollationStrength(collationStrength);
  UNormalizationMode mode = getUNormalizationMode(decompositionMode);
  ucollator = ucol_openRules(pucharrules, length, mode, strength, 
                             &status);
  if (U_SUCCESS(status))
    dataIsOwned = TRUE;

	if (pucharrules != ucharrules)
		delete[] pucharrules;
}



// RuleBasedCollator public destructor -----------------------------------

RuleBasedCollator::~RuleBasedCollator()
{
  if (dataIsOwned)
  {
    ucol_close(ucollator);
    ucollator = NULL;
  }
}

// RuleBaseCollator public methods ---------------------------------------

UBool RuleBasedCollator::operator==(const Collator& that) const
{
  // only checks for address equals here
  if (Collator::operator==(that))
    return TRUE;
    
  if (getDynamicClassID() != that.getDynamicClassID())
    return FALSE;  // not the same class
       
  RuleBasedCollator& thatAlias = (RuleBasedCollator&)that;

  /*
  synwee : orginal code does not check for data compatibility
  */
  if (ucollator != thatAlias.ucollator)
    return FALSE;
    
  return TRUE;
}

RuleBasedCollator& RuleBasedCollator::operator=(
                                              const RuleBasedCollator& that)
{
  if (this != &that)
  {
    if (dataIsOwned)
    {
      ucol_close(ucollator);
      ucollator = NULL;
    }
    
    dataIsOwned = FALSE;
    ucollator = that.ucollator;
  }
  return *this;
}

Collator* RuleBasedCollator::clone() const
{
  return new RuleBasedCollator(*this);
}

/** 
* Create a CollationElementIterator object that will iterator over the 
* elements in a string, using the collation rules defined in this 
* RuleBasedCollator
*/
CollationElementIterator* RuleBasedCollator::createCollationElementIterator
                                           (const UnicodeString& source) const
{
  UErrorCode status = U_ZERO_ERROR;
  CollationElementIterator *newCursor = 0;

  newCursor = new CollationElementIterator(source, this, status);
  
  if (U_FAILURE(status))
    return NULL;
    
  return newCursor;
}

/** 
* Create a CollationElementIterator object that will iterator over the 
* elements in a string, using the collation rules defined in this 
* RuleBasedCollator
*/
CollationElementIterator* RuleBasedCollator::createCollationElementIterator
                                       (const CharacterIterator& source) const
{
  UErrorCode status = U_ZERO_ERROR;
  CollationElementIterator *newCursor = 0;

  newCursor = new CollationElementIterator(source, this, status);
  
  if (U_FAILURE(status))
    return NULL;
    
  return newCursor;
}

/** 
* Return a string representation of this collator's rules. The string can 
* later be passed to the constructor that takes a UnicodeString argument, 
* which will construct a collator that's functionally identical to this one. 
* You can also allow users to edit the string in order to change the collation 
* data, or you can print it out for inspection, or whatever.
*/
const UnicodeString& RuleBasedCollator::getRules() const
{
  int32_t length;
  const UChar *rules = ucol_getRules(ucollator, &length);
  UnicodeString *result = new UnicodeString(rules, length);
  return (*result);
}

Collator::EComparisonResult RuleBasedCollator::compare(
                                               const UnicodeString& source,
                                               const UnicodeString& target,
                                               int32_t length) const
{
  UnicodeString source_togo;
  UnicodeString target_togo;
  UTextOffset begin=0;

  source.extract(begin, uprv_min(length,source.length()), source_togo);
  target.extract(begin, uprv_min(length,target.length()), target_togo);
  return compare(source_togo, target_togo);
}

Collator::EComparisonResult RuleBasedCollator::compare(const UChar* source, 
                                                       int32_t sourceLength,
                                                       const UChar* target,
                                                       int32_t targetLength) 
                                                       const
{
  return getEComparisonResult(ucol_strcoll(ucollator, source, sourceLength, 
                                                     target, targetLength));
}

/**
* Compare two strings using this collator
*/
Collator::EComparisonResult RuleBasedCollator::compare(
                                             const UnicodeString& source,
                                             const UnicodeString& target) const
{
  UChar uSstart[StackBufferLen];
	UChar uTstart[StackBufferLen];
	UChar *uSource = uSstart;
	UChar *uTarget = uTstart;
	uint32_t sourceLen = source.length();
	uint32_t targetLen = target.length();

	if(sourceLen >= StackBufferLen)
		uSource = new UChar[sourceLen+1];
	
	if(targetLen >= StackBufferLen)
		uTarget = new UChar[targetLen+1];
	
  source.extract(0, sourceLen, uSource);
  uSource[sourceLen] = 0;
  target.extract(0, targetLen, uTarget);
  uTarget[targetLen] = 0;
	EComparisonResult result = compare(uSource, sourceLen, uTarget, targetLen);

	if(uSstart != uSource)
		delete[] uSource;
	
	if(uTstart != uTarget)
		delete[] uTarget;
	
	return result;
}

/**
* Retrieve a collation key for the specified string. The key can be compared 
* with other collation keys using a bitwise comparison (e.g. memcmp) to find 
* the ordering of their respective source strings. This is handy when doing a 
* sort, where each sort key must be compared many times.
*
* The basic algorithm here is to find all of the collation elements for each
* character in the source string, convert them to an ASCII representation, and 
* put them into the collation key.  But it's trickier than that. Each 
* collation element in a string has three components: primary ('A' vs 'B'), 
* secondary ('u' vs 'ü'), and tertiary ('A' vs 'a'), and a primary difference
* at the end of a string takes precedence over a secondary or tertiary 
* difference earlier in the string.
*
* To account for this, we put all of the primary orders at the beginning of 
* the string, followed by the secondary and tertiary orders. Each set of 
* orders is terminated by nulls so that a key for a string which is a initial 
* substring of another key will compare less without any special case.
*
* Here's a hypothetical example, with the collation element represented as a 
* three-digit number, one digit for primary, one for secondary, etc.
*
* String:              A     a     B    É
* Collation Elements: 101   100   201  511
* Collation Key:      1125<null>0001<null>1011<null>
*
* To make things even trickier, secondary differences (accent marks) are 
* compared starting at the *end* of the string in languages with French 
* secondary ordering. But when comparing the accent marks on a single base 
* character, they are compared from the beginning. To handle this, we reverse 
* all of the accents that belong to each base character, then we reverse the 
* entire string of secondary orderings at the end.
*/
CollationKey& RuleBasedCollator::getCollationKey(
                                                  const UnicodeString& source,
                                                  CollationKey& sortkey,
                                                  UErrorCode& status) const
{
	UChar sStart[StackBufferLen];
	UChar *uSource = sStart;
	uint32_t sourceLen = source.length();

	if(sourceLen >= StackBufferLen)
		uSource = new UChar[sourceLen+1];
	
  source.extract(0, sourceLen, uSource);
  uSource[sourceLen] = 0;
	CollationKey& result = getCollationKey(uSource, sourceLen, sortkey, status);
	if(sStart != uSource)
		delete[] uSource;

	return result;
}

CollationKey& RuleBasedCollator::getCollationKey(const UChar* source,
                                                    int32_t sourceLen,
                                                    CollationKey& sortkey,
                                                    UErrorCode& status) const
{
  if (U_FAILURE(status))
  {
    status = U_ILLEGAL_ARGUMENT_ERROR;
    return sortkey.setToBogus();
  }
    
  if ((!source) || (sourceLen == 0))
    return sortkey.reset();
    
  uint8_t *result = new uint8_t[UCOL_MAX_BUFFER];
  uint8_t resLen = ucol_getSortKey(ucollator, source, sourceLen, result,
                                   UCOL_MAX_BUFFER);
  sortkey.adopt(result, resLen);
  
  return sortkey;
}

/**
 * Return the maximum length of any expansion sequences that end with the 
 * specified comparison order.
 * @param order a collation order returned by previous or next.
 * @return the maximum length of any expansion seuences ending with the 
 *         specified order.
 * @see CollationElementIterator#getMaxExpansion
 */
int32_t RuleBasedCollator::getMaxExpansion(int32_t order) const
{
  /*
  synwee : have to remove NULL later when Vladimir finishes his codes
  */
  return ucol_getMaxExpansion(NULL, order);
}

uint8_t* RuleBasedCollator::cloneRuleData(int32_t &length, 
                                              UErrorCode &status)
{
  return ucol_cloneRuleData(ucollator, &length, &status);
}

void RuleBasedCollator::setAttribute(UColAttribute attr, 
                                     UColAttributeValue value, 
                                     UErrorCode &status) 
{
  if (U_FAILURE(status))
    return;
  ucol_setAttribute(ucollator, attr, value, &status);
}

UColAttributeValue RuleBasedCollator::getAttribute(UColAttribute attr, 
                                                      UErrorCode &status) 
{
  if (U_FAILURE(status))
    return UCOL_DEFAULT;
  return ucol_getAttribute(ucollator, attr, &status);
}

Collator* RuleBasedCollator::safeClone(void) 
{
  UErrorCode intStatus = U_ZERO_ERROR;
  UCollator *ucol = ucol_safeClone(ucollator, NULL, 0, &intStatus);
  if (U_FAILURE(intStatus))
    return NULL;
  RuleBasedCollator *result = new RuleBasedCollator(ucol);
  result->dataIsOwned = TRUE;
  return result;
}

Collator::EComparisonResult RuleBasedCollator::compare(
                                              ForwardCharacterIterator &source,
											                        ForwardCharacterIterator &target) 
{
  return getEComparisonResult(
    ucol_strcollinc(ucollator, forwardCharIteratorGlue, &source, 
                    forwardCharIteratorGlue, &target));
}

int32_t RuleBasedCollator::getSortKey(const UnicodeString& source, 
                                         uint8_t *result, int32_t resultLength) 
                                         const 
{
	UChar sStart[StackBufferLen];
	UChar *uSource = sStart;
	uint32_t sourceLen = source.length();
	if(sourceLen >= StackBufferLen)
		uSource = new UChar[sourceLen+1];
	
  source.extract(0, sourceLen, uSource);
  uSource[sourceLen] = 0;

	uint8_t resLen = ucol_getSortKey(ucollator, uSource, sourceLen, result,
                                   resultLength);
	if(sStart != uSource)
		delete[] uSource;

	return resLen;
}

int32_t RuleBasedCollator::getSortKey(const UChar *source, 
                                         int32_t sourceLength, uint8_t *result,
						                             int32_t resultLength) const 
{
	return ucol_getSortKey(ucollator, source, sourceLength, result, resultLength);
}

Collator::ECollationStrength RuleBasedCollator::getStrength(void) const
{
  UErrorCode intStatus = U_ZERO_ERROR;
  return getECollationStrength(ucol_getAttribute(ucollator, UCOL_STRENGTH, 
                               &intStatus));
}

void RuleBasedCollator::setStrength(ECollationStrength newStrength)
{
  UErrorCode intStatus = U_ZERO_ERROR;
  UCollationStrength strength = getUCollationStrength(newStrength);
  ucol_setAttribute(ucollator, UCOL_STRENGTH, strength, &intStatus);
}

/** 
* Create a hash code for this collation. Just hash the main rule table -- that 
* should be good enough for almost any use.
*/
int32_t RuleBasedCollator::hashCode() const
{
  int32_t length;
  const UChar *rules = ucol_getRules(ucollator, &length);
  return uhash_hashUCharsN(rules, length);
}

/**
* Set the decomposition mode of the Collator object. success is equal to 
* U_ILLEGAL_ARGUMENT_ERROR if error occurs.
* @param the new decomposition mode
* @see Collator#getDecomposition
*/
void RuleBasedCollator::setDecomposition(Normalizer::EMode  mode)
{
  ucol_setNormalization(ucollator, getUNormalizationMode(mode));
}

/**
* Get the decomposition mode of the Collator object.
* @return the decomposition mode
* @see Collator#setDecomposition
*/
Normalizer::EMode RuleBasedCollator::getDecomposition(void) const
{
  return getNormalizerEMode(ucol_getNormalization(ucollator));
}

// RuleBaseCollatorNew private constructor ----------------------------------

RuleBasedCollator::RuleBasedCollator() : dataIsOwned(FALSE)
{
}

RuleBasedCollator::RuleBasedCollator(UCollator *collator) : dataIsOwned(FALSE)
{
  ucollator = collator;
}

RuleBasedCollator::RuleBasedCollator(const Locale& desiredLocale,
                                           UErrorCode& status) : 
                                           dataIsOwned(FALSE)
{
  if (U_FAILURE(status))
    return;

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
  // steps 1-5 are handled by resource bundle fallback mechanism. 
  // however, in a very unprobable situation that no resource bundle
  // data exists, step 5 is repeated with hardcoded default rules.

  setUCollator(desiredLocale, status);

  if (!U_SUCCESS(status)) 
  {
    UErrorCode intStatus = U_ZERO_ERROR;
   
    setUCollator(ResourceBundle::kDefaultFilename, intStatus);
    if (U_FAILURE(intStatus)) 
    {
      intStatus = U_ZERO_ERROR;

      unsigned long size = 0;
      const UChar * defaultrules = ucol_getDefaultRulesArray(&size);

      /*
      synwee : have to use ucol for this
      constructFromRules(defaultrules, intStatus);
      */
      UCollator *collator = ucol_openRules(defaultrules, size, 
        UCOL_DEFAULT_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &intStatus);
      
      if (intStatus == U_ZERO_ERROR)
        status = U_USING_DEFAULT_ERROR;
      else
        status = intStatus;     // bubble back

      if (status == U_MEMORY_ALLOCATION_ERROR)
        return;
    }
  }

  if (U_SUCCESS(status))
    dataIsOwned = TRUE;
  
  return;
}

// RuleBasedCollator private data members --------------------------------

// need look up in .commit()
const int32_t RuleBasedCollator::CHARINDEX = 0x70000000;             
// Expand index follows
const int32_t RuleBasedCollator::EXPANDCHARINDEX = 0x7E000000;       
// contract indexes follows
const int32_t RuleBasedCollator::CONTRACTCHARINDEX = 0x7F000000;     
// unmapped character values
const int32_t RuleBasedCollator::UNMAPPED = 0xFFFFFFFF;              
// primary strength increment
const int32_t RuleBasedCollator::PRIMARYORDERINCREMENT = 0x00010000; 
// secondary strength increment
const int32_t RuleBasedCollator::SECONDARYORDERINCREMENT = 0x00000100;
// tertiary strength increment 
const int32_t RuleBasedCollator::TERTIARYORDERINCREMENT = 0x00000001;
// mask off anything but primary order
const int32_t RuleBasedCollator::PRIMARYORDERMASK = 0xffff0000;      
// mask off anything but secondary order
const int32_t RuleBasedCollator::SECONDARYORDERMASK = 0x0000ff00;    
// mask off anything but tertiary order
const int32_t RuleBasedCollator::TERTIARYORDERMASK = 0x000000ff;     
// mask off ignorable char order
const int32_t RuleBasedCollator::IGNORABLEMASK = 0x0000ffff;         
// use only the primary difference
const int32_t RuleBasedCollator::PRIMARYDIFFERENCEONLY = 0xffff0000; 
// use only the primary and secondary difference
const int32_t RuleBasedCollator::SECONDARYDIFFERENCEONLY = 0xffffff00;  
// primary order shift
const int32_t RuleBasedCollator::PRIMARYORDERSHIFT = 16;             
// secondary order shift
const int32_t RuleBasedCollator::SECONDARYORDERSHIFT = 8;            
// starting value for collation elements
const int32_t RuleBasedCollator::COLELEMENTSTART = 0x02020202;       
// testing mask for primary low element
const int32_t RuleBasedCollator::PRIMARYLOWZEROMASK = 0x00FF0000;    
// reseting value for secondaries and tertiaries
const int32_t RuleBasedCollator::RESETSECONDARYTERTIARY = 0x00000202;
// reseting value for tertiaries
const int32_t RuleBasedCollator::RESETTERTIARY = 0x00000002;         

const int32_t RuleBasedCollator::PRIMIGNORABLE = 0x0202;

// unique file id for parity check
const int16_t RuleBasedCollator::FILEID = 0x5443;                    
// binary collation file extension
const char* RuleBasedCollator::kFilenameSuffix = ".col";             
// class id ? Value is irrelevant 
char  RuleBasedCollator::fgClassID = 0; 

// other methods not belonging to any classes -------------------------------

UChar forwardCharIteratorGlue(void *iterator) 
{
  ForwardCharacterIterator *iter = ((ForwardCharacterIterator *)iterator);
  UChar result = iter->nextPostInc();
  if (result == ForwardCharacterIterator::DONE)
    return 0xFFFF;
  else
    return result;
}
