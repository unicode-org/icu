/*
******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

/**
* File coll.cpp
*
* Created by: Helena Shih
*
* Modification History:
*
*  Date        Name        Description
*  2/5/97      aliu        Modified createDefault to load collation data from
*                          binary files when possible.  Added related methods
*                          createCollationFromFile, chopLocale, createPathName.
*  2/11/97     aliu        Added methods addToCache, findInCache, which implement
*                          a Collation cache.  Modified createDefault to look in
*                          cache first, and also to store newly created Collation
*                          objects in the cache.  Modified to not use gLocPath.
*  2/12/97     aliu        Modified to create objects from RuleBasedCollator cache.
*                          Moved cache out of Collation class.
*  2/13/97     aliu        Moved several methods out of this class and into
*                          RuleBasedCollator, with modifications.  Modified
*                          createDefault() to call new RuleBasedCollator(Locale&)
*                          constructor.  General clean up and documentation.
*  2/20/97     helena      Added clone, operator==, operator!=, operator=, and copy
*                          constructor.
* 05/06/97     helena      Added memory allocation error detection.
* 05/08/97     helena      Added createInstance().
*  6/20/97     helena      Java class name change.
* 04/23/99     stephen     Removed EDecompositionMode, merged with 
*                          Normalizer::EMode
* 11/23/9      srl         Inlining of some critical functions
* 01/29/01     synwee      Modified into a C++ wrapper calling C APIs (ucol.h)
*/

#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "cmemory.h"

U_NAMESPACE_BEGIN

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
    collation = NULL;
  }
  return collation;
}

Collator *
Collator::createInstance(const Locale &loc,
                         UVersionInfo version,
                         UErrorCode &status) {
  Collator *collator;
  UVersionInfo info;

  collator=new RuleBasedCollator(loc, status);
  if(U_SUCCESS(status)) {
    collator->getVersion(info);
    if(0!=uprv_memcmp(version, info, sizeof(UVersionInfo))) {
      delete collator;
      status=U_MISSING_RESOURCE_ERROR;
      return NULL;
    }
  }
  return collator;
}

UBool Collator::equals(const UnicodeString& source, 
                          const UnicodeString& target) const
{
  return (compare(source, target) == EQUAL);
}

UBool Collator::greaterOrEqual(const UnicodeString& source, 
                                  const UnicodeString& target) const
{
  return (compare(source, target) != LESS);
}

UBool Collator::greater(const UnicodeString& source, 
                           const UnicodeString& target) const
{
  return (compare(source, target) == GREATER);
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
Collator::Collator(UCollationStrength /* collationStrength */,
                         UNormalizationMode /* decompositionMode */)
{
}

Collator::~Collator()
{
}

Collator::Collator(const Collator& /* other */)
{
}

// UCollator private data members ----------------------------------------

const UVersionInfo Collator::fVersion = {1, 1, 0, 0};

U_NAMESPACE_END

/* eof */
