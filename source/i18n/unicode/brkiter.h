/*
*****************************************************************************************
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*
* File BRKITER.H
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Added typedef for TextCount.  Made DONE const.
*   05/07/97    aliu        Fixed DLL declaration.
*   07/09/97    jfitz       Renamed BreakIterator and interface synced with JDK
*   08/11/98    helena      Sync-up JDK1.2.
*   01/13/2000  helena      Added UErrorCode parameter to createXXXInstance methods.
*****************************************************************************************
*/

#ifndef BRKITER_H
#define BRKITER_H


#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/locid.h"


//class Locale;

/**
 * The BreakIterator class implements methods for finding the location
 * of boundaries in text. BreakIterator is an abstract base class.
 * Instances of BreakIterator maintain a current position and scan over
 * text returning the index of characters where boundaries occur.
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
 * .   void printTextRange( BreakIterator& iterator, UTextOffset start, UTextOffset end )
 * .   {
 * .       UnicodeString textBuffer, temp;
 * .       CharacterIterator *strIter = iterator.createText();
 * .       strIter->getText(temp);
 * .       cout &lt;&lt; " " &lt;&lt; start &lt;&lt; " " &lt;&lt; end &lt;&lt; " |" 
 * .            &lt;&lt; temp.extractBetween(start, end, textBuffer)
 * .            &lt;&lt; "|" &lt;&lt; endl;
 * .       delete strIter;
 * .   }
 * </pre>
 * Print each element in order:
 * <pre>
 * .   void printEachForward( BreakIterator& boundary)
 * .   {
 * .      UTextOffset start = boundary.first();
 * .      for (UTextOffset end = boundary.next();
 * .        end != BreakIterator::DONE;
 * .        start = end, end = boundary.next())
 * .        {
 * .            printTextRange( boundary, start, end );
 * .        }
 * .   }
 * </pre>
 * Print each element in reverse order:
 * <pre>
 * .   void printEachBackward( BreakIterator& boundary)
 * .   {
 * .      UTextOffset end = boundary.last();
 * .      for (UTextOffset start = boundary.previous();
 * .        start != BreakIterator::DONE;
 * .        end = start, start = boundary.previous())
 * .        {
 * .            printTextRange( boundary, start, end );
 * .        }
 * .   }
 * </pre>
 * Print first element
 * <pre>
 * .   void printFirst(BreakIterator& boundary)
 * .   {
 * .       UTextOffset start = boundary.first();
 * .       UTextOffset end = boundary.next();
 * .       printTextRange( boundary, start, end );
 * .   }
 * </pre>
 * Print last element
 * <pre>
 * .   void printLast(BreakIterator& boundary)
 * .   {
 * .       UTextOffset end = boundary.last();
 * .       UTextOffset start = boundary.previous();
 * .       printTextRange( boundary, start, end );
 * .   }
 * </pre>
 * Print the element at a specified position
 * <pre>
 * .   void printAt(BreakIterator &boundary, UTextOffset pos )
 * .   {
 * .       UTextOffset end = boundary.following(pos);
 * .       UTextOffset start = boundary.previous();
 * .       printTextRange( boundary, start, end );
 * .   }
 * </pre>
 * Creating and using text boundaries
 * <pre>
 * .      void BreakIterator_Example( void )
 * .      {
 * .          BreakIterator* boundary;
 * .          UnicodeString stringToExamine("Aaa bbb ccc. Ddd eee fff.");
 * .          cout &lt;&lt; "Examining: " &lt;&lt; stringToExamine &lt;&lt; endl;
 * .
 * .          //print each sentence in forward and reverse order
 * .          boundary = BreakIterator::createSentenceInstance( Locale::US );
 * .          boundary->setText(&stringToExamine);
 * .          cout &lt;&lt; "----- forward: -----------" &lt;&lt; endl;
 * .          printEachForward(*boundary);
 * .          cout &lt;&lt; "----- backward: ----------" &lt;&lt; endl;
 * .          printEachBackward(*boundary);
 * .          delete boundary;
 * .
 * .          //print each word in order
 * .          boundary = BreakIterator::createWordInstance();
 * .          boundary->setText(&stringToExamine);
 * .          cout &lt;&lt; "----- forward: -----------" &lt;&lt; endl;
 * .          printEachForward(*boundary);
 * .          //print first element
 * .          cout &lt;&lt; "----- first: -------------" &lt;&lt; endl;
 * .          printFirst(*boundary);
 * .          //print last element
 * .          cout &lt;&lt; "----- last: --------------" &lt;&lt; endl;
 * .          printLast(*boundary);
 * .          //print word at charpos 10
 * .          cout &lt;&lt; "----- at pos 10: ---------" &lt;&lt; endl;
 * .          printAt(*boundary, 10 );
 * .
 * .          delete boundary;
 * .      }
 * </pre>
 */
class U_I18N_API BreakIterator {
public:
    virtual ~BreakIterator();

    /**
     * Return true if another object is semantically equal to this
     * one. The other object should be an instance of the same subclass of
     * BreakIterator. Objects of different subclasses are considered
     * unequal.
     * <P>
     * Return true if this BreakIterator is at the same position in the
     * same text, and is the same class and type (word, line, etc.) of
     * BreakIterator, as the argument.  Text is considered the same if
     * it contains the same characters, it need not be the same
     * object, and styles are not considered.
     * @stable
     */
    virtual bool_t operator==(const BreakIterator&) const = 0;

    /**
     * Returns the complement of the result of operator==
     * @stable
     */
    bool_t operator!=(const BreakIterator& rhs) const { return !operator==(rhs); }

    /**
     * Return a polymorphic copy of this object.  This is an abstract
     * method which subclasses implement.
     * @stable
     */
    virtual BreakIterator* clone(void) const = 0;

    /**
     * Return a polymorphic class ID for this object. Different subclasses
     * will return distinct unequal values.
     * @stable
     */
    virtual UClassID getDynamicClassID(void) const = 0;

    /**
     * Return a CharacterIterator over the text being analyzed.
     * @draft
     */
    virtual const CharacterIterator& getText(void) const = 0;

    /**
     * Get the text for which this object is finding the boundaries.
     * @draft
     */
    virtual CharacterIterator* createText(void) const = 0;

    /**
     * Change the text over which this operates. The text boundary is
     * reset to the start.
     * @deprecated This function should be modified to take a const UnicodeString& agrument.
     */
    virtual void  setText(const UnicodeString* it) = 0;

    /**
     * Change the text over which this operates. The text boundary is
     * reset to the start.
     * @stable
     */
    virtual void  adoptText(CharacterIterator* it) = 0;

    /**
     * DONE is returned by previous() and next() after all valid
     * boundaries have been returned.
     * @stable
     */
    static const UTextOffset DONE;

    /**
     * Return the index of the first character in the text being scanned.
     * @stable
     */
    virtual UTextOffset first(void) = 0;

    /**
     * Return the index immediately BEYOND the last character in the text being scanned.
     * @stable
     */
    virtual UTextOffset last(void) = 0;

    /**
     * Return the boundary preceding the current boundary.
     * @return The character index of the previous text boundary or DONE if all
     * boundaries have been returned.
     * @stable
     */
    virtual UTextOffset previous(void) = 0;

    /**
     * Return the boundary following the current boundary.
     * @return The character index of the next text boundary or DONE if all
     * boundaries have been returned.
     * @stable
     */
    virtual UTextOffset next(void) = 0;

    /**
     * Return character index of the text boundary that was most recently
     * returned by next(), previous(), first(), or last()
     * @return The boundary most recently returned.
     * @stable
     */
    virtual UTextOffset current(void) const = 0;

    /**
     * Return the first boundary following the specified offset.
     * The value returned is always greater than the offset or
     * the value BreakIterator.DONE
     * @param offset the offset to begin scanning.
     * @return The first boundary after the specified offset.
     * @stable
     */
    virtual UTextOffset following(UTextOffset offset) = 0;

    /**
     * Return the first boundary preceding the specified offset.
     * The value returned is always smaller than the offset or
     * the value BreakIterator.DONE
     * @param offset the offset to begin scanning.
     * @return The first boundary before the specified offset.
     * @stable
     */
    virtual UTextOffset preceding(UTextOffset offset) = 0;
 
    /**
     * Return true if the specfied position is a boundary position.
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     * @stable
     */
    virtual bool_t isBoundary(UTextOffset offset) = 0;

    /**
     * Return the nth boundary from the current boundary
     * @param n which boundary to return.  A value of 0
     * does nothing.  Negative values move to previous boundaries
     * and positive values move to later boundaries.
     * @return The index of the nth boundary from the current position, or
     * DONE if there are fewer than |n| boundaries in the specfied direction.
     * @stable
     */
    virtual UTextOffset next(int32_t n) = 0;

    /**
     * Create BreakIterator for word-breaks using the given locale.
     * Returns an instance of a BreakIterator implementing word breaks.
     * WordBreak is useful for word selection (ex. double click)
     * @param where the locale. 
     * @param status the error code
     * @return A BreakIterator for word-breaks.  The UErrorCode& status 
     * parameter is used to return status information to the user.
     * To check whether the construction succeeded or not, you should check
     * the value of U_SUCCESS(err).  If you wish more detailed information, you
     * can check for informational error results which still indicate success.
     * U_USING_FALLBACK_ERROR indicates that a fall back locale was used.  For
     * example, 'de_CH' was requested, but nothing was found there, so 'de' was
     * used.  U_USING_DEFAULT_ERROR indicates that the default locale data was
     * used; neither the requested locale nor any of its fall back locales
     * could be found.
     * The caller owns the returned object and is responsible for deleting it.
     * @draft
     */
    static BreakIterator* createWordInstance(const Locale& where, 
                                                   UErrorCode& status);

    /**
     * Create BreakIterator for line-breaks using specified locale.
     * Returns an instance of a BreakIterator implementing line breaks. Line
     * breaks are logically possible line breaks, actual line breaks are
     * usually determined based on display width.
     * LineBreak is useful for word wrapping text.
     * @param where the locale. .
     * @return A BreakIterator for line-breaks.  The UErrorCode& status 
     * parameter is used to return status information to the user.
     * To check whether the construction succeeded or not, you should check
     * the value of U_SUCCESS(err).  If you wish more detailed information, you
     * can check for informational error results which still indicate success.
     * U_USING_FALLBACK_ERROR indicates that a fall back locale was used.  For
     * example, 'de_CH' was requested, but nothing was found there, so 'de' was
     * used.  U_USING_DEFAULT_ERROR indicates that the default locale data was
     * used; neither the requested locale nor any of its fall back locales
     * could be found.
     * The caller owns the returned object and is responsible for deleting it.
     * @draft
     */
    static BreakIterator* createLineInstance(const Locale& where, 
                                                   UErrorCode& status);

    /**
     * Create BreakIterator for character-breaks using specified locale
     * Returns an instance of a BreakIterator implementing character breaks.
     * Character breaks are boundaries of combining character sequences.
     * @param where the locale. 
     * @return A BreakIterator for character-breaks.  The UErrorCode& status 
     * parameter is used to return status information to the user.
     * To check whether the construction succeeded or not, you should check
     * the value of U_SUCCESS(err).  If you wish more detailed information, you
     * can check for informational error results which still indicate success.
     * U_USING_FALLBACK_ERROR indicates that a fall back locale was used.  For
     * example, 'de_CH' was requested, but nothing was found there, so 'de' was
     * used.  U_USING_DEFAULT_ERROR indicates that the default locale data was
     * used; neither the requested locale nor any of its fall back locales
     * could be found.
     * The caller owns the returned object and is responsible for deleting it.
     * @draft
     */
    static BreakIterator* createCharacterInstance(const Locale& where, 
                                                        UErrorCode& status);

    /**
     * Create BreakIterator for sentence-breaks using specified locale
     * Returns an instance of a BreakIterator implementing sentence breaks.
     * @param where the locale. 
     * @return A BreakIterator for sentence-breaks.  The UErrorCode& status 
     * parameter is used to return status information to the user.
     * To check whether the construction succeeded or not, you should check
     * the value of U_SUCCESS(err).  If you wish more detailed information, you
     * can check for informational error results which still indicate success.
     * U_USING_FALLBACK_ERROR indicates that a fall back locale was used.  For
     * example, 'de_CH' was requested, but nothing was found there, so 'de' was
     * used.  U_USING_DEFAULT_ERROR indicates that the default locale data was
     * used; neither the requested locale nor any of its fall back locales
     * could be found.
     * The caller owns the returned object and is responsible for deleting it.
     * @draft
     */
    static BreakIterator* createSentenceInstance(const Locale& where, 
                                                       UErrorCode& status);

    /**
     * Get the set of Locales for which TextBoundaries are installed
     * @param count the output parameter of number of elements in the locale list
     * @return available locales
     * @stable
     */
    static const Locale* getAvailableLocales(int32_t& count);

    /**
     * Get name of the object for the desired Locale, in the desired langauge.
     * @param objectLocale must be from getAvailableLocales.
     * @param displayLocale specifies the desired locale for output.
     * @param name the fill-in parameter of the return value
     * Uses best match.
     * @return user-displayable name
     * @stable
     */
    static UnicodeString& getDisplayName(const Locale& objectLocale,
                                         const Locale& displayLocale,
                                         UnicodeString& name);

    /**
     * Get name of the object for the desired Locale, in the langauge of the
     * default locale.
     * @param objectLocale must be from getMatchingLocales
     * @param name the fill-in parameter of the return value
     * @return user-displayable name
     * @stable
     */
    static UnicodeString& getDisplayName(const Locale& objectLocale,
                                         UnicodeString& name);



protected:
    BreakIterator();

private:
    /**
     * The copy constructor and assignment operator have no real implementation.
     * They are provided to make the compiler happy. Do not call.
     */
    BreakIterator& operator=(const BreakIterator& other) { return *this; }
    BreakIterator (const BreakIterator& other) {}
};

#endif // _BRKITER
//eof
