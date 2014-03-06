/*
********************************************************************************
*   Copyright (C) 1997-2014, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*/

#ifndef FILTEREDBRK_H
#define FILTEREDBRK_H

#include "unicode/brkiter.h"

#if !UCONFIG_NO_BREAK_ITERATION && U_HAVE_STD_STRING

U_NAMESPACE_BEGIN

/**
 * \file
 * \brief C++ API: FilteredBreakIteratorBuilder
 */

/**
 * The BreakIteratorFilter is used to modify the behavior of a BreakIterator
 *  by constructing a new BreakIterator which skips certain breaks as "exceptions".
 *  See  http://www.unicode.org/reports/tr35/tr35-general.html#Segmentation_Exceptions .
 *  For example, a typical English Sentence Break Iterator would break on the space
 *  in the string "Mr. Smith" (resulting in two segments),
 *  but with "Mr." as an exception, a filtered break iterator
 *  would consider the string "Mr. Smith" to be a single segment.
 *
 * @internal technology preview
 */
class U_I18N_API FilteredBreakIteratorBuilder : public UObject {
 public:
  /**
   *  destructor.
   */
  virtual ~FilteredBreakIteratorBuilder();

  /**
   * Construct a FilteredBreakIteratorBuilder based on rules in a locale.
   * The rules are taken from CLDR exception data for the locale,
   *  see http://www.unicode.org/reports/tr35/tr35-general.html#Segmentation_Exceptions
   *  This is the equivalent of calling createInstance(UErrorCode&)
   *    and then repeatedly calling addNoBreakAfter(...) with the contents
   *    of the CLDR exception data.
   * @param where the locale.
   * @param status The error code.
   * @return the new builder
   */
  static FilteredBreakIteratorBuilder *createInstance(const Locale& where, UErrorCode& status);

  /**
   * Construct an empty FilteredBreakIteratorBuilder. It will have an empty
   * exception list.
   * @param status The error code.
   * @return the new builder
   */
  static FilteredBreakIteratorBuilder *createInstance(UErrorCode &status);


  /**
   * Add an exception. The break iterator will not break after this string.
   * @param exception the exception string
   * @param status error code
   * @return returns TRUE if the exception was not present and added,
   * FALSE if the call was a no-op because the exception was already present.
   */
  virtual UBool suppressBreakAfter(const UnicodeString& exception, UErrorCode& status) = 0;

  /**
   * Remove a single exception.
   * @param exception the exception to remove
   * @param status error code
   * @return returns TRUE if the exception was present and removed,
   * FALSE if the call was a no-op because the exception was not present.
   */
  virtual UBool unsuppressBreakAfter(const UnicodeString& exception, UErrorCode& status) = 0;

  /**
   * build a BreakIterator from this builder.
   * The resulting BreakIterator is owned by the caller.
   * The BreakIteratorFilter may be destroyed before the BreakIterator is.
   * Note that the adoptBreakIterator is adopted by the new BreakIterator
   * and should no longer be used by the caller.
   * @param adoptBreakIterator the break iterator to adopt
   * @param status error code
   * @return the new BreakIterator, owned by the caller.
   */
  virtual BreakIterator *build(BreakIterator* adoptBreakIterator, UErrorCode& status) = 0;

 protected:
  /**
   * For subclass use
   */
  FilteredBreakIteratorBuilder();
};


U_NAMESPACE_END

#endif // #if !UCONFIG_NO_BREAK_ITERATION

#endif // #ifndef FILTEREDBRK_H
