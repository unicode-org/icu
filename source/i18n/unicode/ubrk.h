/*
* Copyright © {1996-1999}, International Business Machines Corporation and others. All Rights Reserved.
*****************************************************************************************
*/

#ifndef UBRK_H
#define UBRK_H

#include "unicode/utypes.h"
/**
 * @name BreakIterator C API
 *
 * The BreakIterator C API defines  methods for finding the location
 * of boundaries in text. Pointer to a UBreakIterator maintain a 
 * current position and scan over text returning the index of characters 
 * where boundaries occur.
 * <P>
 * Line boundary analysis determines where a text string can be broken
 * when line-wrapping. The mechanism correctly handles punctuation and
 * hyphenated words.
 * <P>
 * Sentence boundary analysis allows selection with correct
 * interpretation of periods within numbers and abbreviations, and
 * trailing punctuation marks such as quotation marks and parentheses.
 * <P>
 * Word boundary analysis is used by search and replace functions, as
 * well as within text editing applications that allow the user to
 * select words with a double click. Word selection provides correct
 * interpretation of punctuation marks within and following
 * words. Characters that are not part of a word, such as symbols or
 * punctuation marks, have word-breaks on both sides.
 * <P>
 * Character boundary analysis allows users to interact with
 * characters as they expect to, for example, when moving the cursor
 * through a text string. Character boundary analysis provides correct
 * navigation of through character strings, regardless of how the
 * character is stored.  For example, an accented character might be
 * stored as a base character and a diacritical mark. What users
 * consider to be a character can differ between languages.
 * <P>
 * This is the interface for all text boundaries.
 * <P>
 * Examples:
 * <P>
 * Helper function to output text
 * <pre>
 * .   void printTextRange(UChar* str, UTextOffset start, UTextOffset end ) {
 * .        UChar* result;
 * .        UChar* temp;
 * .        const char* res;
 * .        temp=(UChar*)malloc(sizeof(UChar) * ((u_strlen(str)-start)+1));
 * .        result=(UChar*)malloc(sizeof(UChar) * ((end-start)+1));
 * .        u_strcpy(temp, &str[start]);
 * .        u_strncpy(result, temp, end-start);
 * .        res=(char*)malloc(sizeof(char) * (u_strlen(result)+1));
 * .        u_austrcpy(res, result);
 * .        printf("%s\n", res); 
 * .   }
 * </pre>
 * Print each element in order:
 * <pre>
 * .   void printEachForward( UBreakIterator* boundary, UChar* str) {
 * .      UTextOffset end;
 * .      UTextOffset start = ubrk_first(boundary);
 * .      for (end = ubrk_next(boundary)); end != UBRK_DONE; start = end, end = ubrk_next(boundary)) {
 * .            printTextRange(str, start, end );
 * .        }
 * .   }
 * </pre>
 * Print each element in reverse order:
 * <pre>
 * .   void printEachBackward( UBreakIterator* boundary, UChar* str) {
 * .      UTextOffset start;
 * .      UTextOffset end = ubrk_last(boundary);
 * .      for (start = ubrk_previous(boundary); start != UBRK_DONE;  end = start, start =ubrk_previous(boundary)) {
 * .            printTextRange( str, start, end );
 * .        }
 * .   }
 * </pre>
 * Print first element
 * <pre>
 * .   void printFirst(UBreakIterator* boundary, UChar* str) {
 * .       UTextOffset end;
 * .       UTextOffset start = ubrk_first(boundary);
 * .       end = ubrk_next(boundary);
 * .       printTextRange( str, start, end );
 * .   }
 * </pre>
 * Print last element
 * <pre>
 * .   void printLast(UBreakIterator* boundary, UChar* str) {
 * .       UTextOffset start;
 * .       UTextOffset end = ubrk_last(boundary);
 * .       start = ubrk_previous(boundary);
 * .       printTextRange(str, start, end );
 * .   }
 * </pre>
 * Print the element at a specified position
 * <pre>
 * .   void printAt(UBreakIterator* boundary, UTextOffset pos , UChar* str) {
 * .       UTextOffset start;
 * .       UTextOffset end = ubrk_following(boundary, pos);
 * .       start = ubrk_previous(boundary);
 * .       printTextRange(str, start, end );
 * .   }
 * </pre>
 * Creating and using text boundaries
 * <pre>
 * .      void BreakIterator_Example( void ) {
 * .          UBreakIterator* boundary;
 * .          UChar *stringToExamine;
 * .          stringToExamine=(UChar*)malloc(sizeof(UChar) * (strlen("Aaa bbb ccc. Ddd eee fff.")+1) );
 * .          u_uastrcpy(stringToExamine, "Aaa bbb ccc. Ddd eee fff.");
 * .          printf("Examining: "Aaa bbb ccc. Ddd eee fff.");
 * .
 * .          //print each sentence in forward and reverse order
 * .          boundary = ubrk_open(UBRK_SENTENCE, "en_us", stringToExamine, u_strlen(stringToExamine), &status);
 * .          printf("----- forward: -----------\n"); 
 * .          printEachForward(boundary, stringToExamine);
 * .          printf("----- backward: ----------\n");
 * .          printEachBackward(boundary, stringToExamine);
 * .          ubrk_close(boundary);
 * .
 * .          //print each word in order
 * .          boundary = ubrk_open(UBRK_WORD, "en_us", stringToExamine, u_strlen(stringToExamine), &status);
 * .          printf("----- forward: -----------\n"); 
 * .          printEachForward(boundary, stringToExamine);
 * .          printf("----- backward: ----------\n");
 * .          printEachBackward(boundary, stringToExamine);
 * .          //print first element
 * .          printf("----- first: -------------\n");
 * .          printFirst(boundary, stringToExamine);
 * .          //print last element
 * .          printf("----- last: --------------\n");
 * .          printLast(boundary, stringToExamine);
 * .          //print word at charpos 10
 * .          printf("----- at pos 10: ---------\n");
 * .          printAt(boundary, 10 , stringToExamine);
 * .
 * .          ubrk_close(boundary);
 * .      }
 * </pre>
 */

/** 
 * A text-break iterator.
 *  For usage in C programs.
 */
typedef void* UBreakIterator;

/** The possible types of text boundaries. */
enum UBreakIteratorType {
  /** Character breaks */
  UBRK_CHARACTER,
  /** Word breaks */
  UBRK_WORD,
  /** Line breaks */
  UBRK_LINE,
  /** Sentence breaks */
  UBRK_SENTENCE
};
typedef enum UBreakIteratorType UBreakIteratorType;

/** Value indicating all text boundaries have been returned. 
 *
 */
#define UBRK_DONE ((UTextOffset) -1)

/**
 * Open a new UBreakIterator for locating text boundaries for a specified locale.
 * A UBreakIterator may be used for detecting character, line, word, 
 * and sentence breaks in text.
 * @param type The type of UBreakIterator to open: one of UBRK_CHARACTER, UBRK_WORD,
 * UBRK_LINE, UBRK_SENTENCE
 * @param locale The locale specifying the text-breaking conventions.
 * @param text The text to be iterated over.
 * @param textLength The number of characters in text, or -1 if null-terminated.
 * @param status A UErrorCode to receive any errors.
 * @return A UBreakIterator for the specified locale.
 * @see ubrk_openRules
 * @stable
 */
U_CAPI UBreakIterator*
ubrk_open(UBreakIteratorType type,
      const char *locale,
      const UChar *text,
      int32_t textLength,
      UErrorCode *status);

/**
 * Open a new UBreakIterator for locating text boundaries using specified breaking rules.
 * The rule syntax is ... (TBD)
 * @param rules A set of rules specifying the text breaking conventions.
 * @param rulesLength The number of characters in rules, or -1 if null-terminated.
 * @param text The text to be iterated over.
 * @param textLength The number of characters in text, or -1 if null-terminated.
 * @param status A UErrorCode to receive any errors.
 * @return A UBreakIterator for the specified rules.
 * @see ubrk_open
 * @stable
 */
U_CAPI UBreakIterator*
ubrk_openRules(const UChar *rules,
           int32_t rulesLength,
           const UChar *text,
           int32_t textLength,
           UErrorCode *status);

/**
* Close a UBreakIterator.
* Once closed, a UBreakIterator may no longer be used.
* @param bi The break iterator to close.
 * @stable
*/
U_CAPI void
ubrk_close(UBreakIterator *bi);

/**
 * Sets an existing iterator to point to a new piece of text
 * @stable
 */
U_CAPI void
ubrk_setText(UBreakIterator* bi,
             const UChar*    text,
             int32_t         textLength,
             UErrorCode*     status);

/**
 * Determine the most recently-returned text boundary.
 * 
 * @param bi The break iterator to use.
 * @return The character index most recently returned by \Ref{ubrk_next}, \Ref{ubrk_previous}, 
 * \Ref{ubrk_first}, or \Ref{ubrk_last}.
 * @stable
 */
U_CAPI UTextOffset
ubrk_current(const UBreakIterator *bi);

/**
 * Determine the text boundary following the current text boundary.
 * 
 * @param bi The break iterator to use.
 * @return The character index of the next text boundary, or UBRK_DONE
 * if all text boundaries have been returned.
 * @see ubrk_previous
 * @stable
 */
U_CAPI UTextOffset
ubrk_next(UBreakIterator *bi);

/**
 * Determine the text boundary preceding the current text boundary.
 *
 * @param bi The break iterator to use.
 * @return The character index of the preceding text boundary, or UBRK_DONE
 * if all text boundaries have been returned.
 * @see ubrk_next
 * @stable
 */
U_CAPI UTextOffset
ubrk_previous(UBreakIterator *bi);

/**
 * Determine the index of the first character in the text being scanned.
 * This is not always the same as index 0 of the text.
 * @param bi The break iterator to use.
 * @return The character index of the first character in the text being scanned.
 * @see ubrk_last
 * @stable
 */
U_CAPI UTextOffset
ubrk_first(UBreakIterator *bi);

/**
 * Determine the index immediately <EM>beyond</EM> the last character in the text being
 * scanned.
 * This is not the same as the last character.
 * @param bi The break iterator to use.
 * @return The character offset immediately <EM>beyond</EM> the last character in the
 * text being scanned.
 * @see ubrk_first
 * @stable
 */
U_CAPI UTextOffset
ubrk_last(UBreakIterator *bi);

/**
 * Determine the text boundary preceding the specified offset.
 * The value returned is always smaller than offset, or UBRK_DONE.
 * @param bi The break iterator to use.
 * @param offset The offset to begin scanning.
 * @return The text boundary preceding offset, or UBRK_DONE.
 * @see ubrk_following
 * @stable
 */
U_CAPI UTextOffset
ubrk_preceding(UBreakIterator *bi,
           UTextOffset offset);

/**
 * Determine the text boundary following the specified offset.
 * The value returned is always greater than offset, or UBRK_DONE.
 * @param bi The break iterator to use.
 * @param offset The offset to begin scanning.
 * @return The text boundary following offset, or UBRK_DONE.
 * @see ubrk_preceding
 * @stable
 */
U_CAPI UTextOffset
ubrk_following(UBreakIterator *bi,
           UTextOffset offset);

/**
* Get a locale for which text breaking information is available.
* A UBreakIterator in a locale returned by this function will perform the correct
* text breaking for the locale.
* @param index The index of the desired locale.
* @return A locale for which number text breaking information is available, or 0 if none.
* @see ubrk_countAvailable
* @stable
*/
U_CAPI const char*
ubrk_getAvailable(int32_t index);

/**
* Determine how many locales have text breaking information available.
* This function is most useful as determining the loop ending condition for
* calls to \Ref{ubrk_getAvailable}.
* @return The number of locales for which text breaking information is available.
* @see ubrk_getAvailable
* @stable
*/
U_CAPI int32_t
ubrk_countAvailable(void);

#endif
