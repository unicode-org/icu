/*
******************************************************************************
* Copyright (C) 1996-1999, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

/**
* Created by : Syn Wee Quek
*/

#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "cmemory.h"

// Collator public methods -----------------------------------------------

Collator* Collator::createInstance(UErrorCode& success) 
{
  if (U_FAILURE(success))
    return 0;
  return createInstance(Locale::getDefault(), success);
}

Collator* Collator::createInstance(const Locale&  desiredLocale, 
                                         UErrorCode& status)
{
  if (U_FAILURE(status)) 
    return 0;

  // A bit of explanation is required here. Although in the current 
  // implementation
  // Collator::createInstance() is just turning around and calling 
  // RuleBasedCollator(Locale&), this will not necessarily always be the 
  // case. For example, suppose we modify this code to handle a 
  // non-table-based Collator, such as that for Thai. In this case, 
  // createInstance() will have to be modified to somehow determine this fact
  // (perhaps a field in the resource bundle). Then it can construct the 
  // non-table-based Collator in some other way, when it sees that it needs 
  // to.
  // The specific caution is this: RuleBasedCollator(Locale&) will ALWAYS 
  // return a valid collation object, if the system if functioning properly.  
  // The reason is that it will fall back, use the default locale, and even 
  // use the built-in default collation rules. THEREFORE, createInstance() 
  // should in general ONLY CALL RuleBasedCollator(Locale&) IF IT KNOWS IN 
  // ADVANCE that the given locale's collation is properly implemented as a 
  // RuleBasedCollator.
  // Currently, we don't do this...we always return a RuleBasedCollator, 
  // whether it is strictly correct to do so or not, without checking, because 
  // we currently have no way of checking.

  RuleBasedCollator* collation = new RuleBasedCollator(desiredLocale, 
                                                             status);
  if (U_FAILURE(status))
  {
    delete collation;
    collation = 0;
  }
  return collation;
}

UBool Collator::equals(const UnicodeString& source, 
                          const UnicodeString& target) const
{
  return (compare(source, target) == UCOL_EQUAL);
}

UBool Collator::greaterOrEqual(const UnicodeString& source, 
                                  const UnicodeString& target) const
{
  return (compare(source, target) != UCOL_LESS);
}

UBool Collator::greater(const UnicodeString& source, 
                           const UnicodeString& target) const
{
  return (compare(source, target) == UCOL_GREATER);
}

const Locale* Collator::getAvailableLocales(int32_t& count) 
{
  return Locale::getAvailableLocales(count);
}

UnicodeString& Collator::getDisplayName(const Locale& objectLocale,
                                           const Locale& displayLocale,
                                           UnicodeString& name)
{
  // synwee : in a dilemma whether to change to UCollator. Since 
  // UCollator is basically using the below operation. 
  // Change means more mantainability where else no change means faster speed.
  return objectLocale.getDisplayName(displayLocale, name);
}

UnicodeString& Collator::getDisplayName(const Locale& objectLocale,
                                           UnicodeString& name)
{   
  // synwee : in a dilemma whether to change to UCollator. Since 
  // UCollator is basically using the below operation.
  // Change means more mantainability where else no change means faster speed.
  return objectLocale.getDisplayName(Locale::getDefault(), name);
}

void Collator::getVersion(UVersionInfo versionInfo) const
{
  if (versionInfo!=NULL)
    uprv_memcpy(versionInfo, fVersion, U_MAX_VERSION_LENGTH);
}

// UCollator protected constructor destructor ----------------------------

/**
* Default constructor.
* Constructor is different from the old default Collator constructor.
* The task for determing the default collation strength and normalization mode
* is left to the child class.
*/
Collator::Collator()
{
}

/**
* Constructor.
* Empty constructor, does not handle the arguments.
* This constructor is done for backward compatibility with 1.7 and 1.8.
* The task for handling the argument collation strength and normalization 
* mode is left to the child class.
* @param collationStrength collation strength
* @param decompositionMode 
*/
Collator::Collator(UCollationStrength collationStrength,
                         UNormalizationMode decompositionMode)
{
}

Collator::~Collator()
{
}

Collator::Collator(const Collator& other)
{
}

// UCollator private data members ----------------------------------------

const UVersionInfo Collator::fVersion = {1, 1, 0, 0};
