/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/BreakIterator.java,v $ 
 * $Date: 2000/03/10 04:07:18 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.util.Vector;
import java.util.Locale;
import java.util.ResourceBundle;                                    //ibm.597
import java.util.MissingResourceException;                          //ibm.597
import java.text.resources.LocaleData;                              //ibm.597
import java.text.CharacterIterator;                                 //ibm.597
import java.text.StringCharacterIterator;                           //ibm.597

import java.net.URL;                                                //ibm.597
import java.io.InputStream;                                         //ibm.597
import java.io.IOException;                                         //ibm.597

import java.lang.ref.SoftReference;                                 //ibm.597

/**
 * A class that locates boundaries in text.  This class defines a protocol for
 * objects that break up a piece of natural-language text according to a set
 * of criteria.  Instances or subclasses of BreakIterator can be provided, for
 * example, to break a piece of text into words, sentences, or logical characters
 * according to the conventions of some language or group of languages.
 *
 * We provide four built-in types of BreakIterator:
 * <ul><li>getSentenceInstance() returns a BreakIterator that locates boundaries
 * between sentences.  This is useful for triple-click selection, for example.
 * <li>getWordInstance() returns a BreakIterator that locates boundaries between
 * words.  This is useful for double-click selection or "find whole words" searches.
 * This type of BreakIterator makes sure there is a boundary position at the
 * beginning and end of each legal word.  (Numbers count as words, too.)  Whitespace
 * and punctuation are kept separate from real words.
 * <li>getLineInstance() returns a BreakIterator that locates positions where it is
 * legal for a text editor to wrap lines.  This is similar to word breaking, but
 * not the same: punctuation and whitespace are generally kept with words (you don't
 * want a line to start with whitespace, for example), and some special characters
 * can force a position to be considered a line-break position or prevent a position
 * from being a line-break position.
 * <li>getCharacterInstance() returns a BreakIterator that locates boundaries between
 * logical characters.  Because of the structure of the Unicode encoding, a logical
 * character may be stored internally as more than one Unicode code point.  (A with an
 * umlaut may be stored as an a followed by a separate combining umlaut character,
 * for example, but the user still thinks of it as one character.)  This iterator allows
 * various processes (especially text editors) to treat as characters the units of text
 * that a user would think of as characters, rather than the units of text that the
 * computer sees as "characters".</ul>
 *
 * BreakIterator's interface follows an "iterator" model (hence the name), meaning it
 * has a concept of a "current position" and methods like first(), last(), next(),
 * and previous() that update the current position.  All BreakIterators uphold the
 * following invariants:
 * <ul><li>The beginning and end of the text are always treated as boundary positions.
 * <li>The current position of the iterator is always a boundary position (random-
 * access methods move the iterator to the nearest boundary position before or
 * after the specified position, not _to_ the specified position).
 * <li>DONE is used as a flag to indicate when iteration has stopped.  DONE is only
 * returned when the current position is the end of the text and the user calls next(),
 * or when the current position is the beginning of the text and the user calls
 * previous().
 * <li>Break positions are numbered by the positions of the characters that follow
 * them.  Thus, under normal circumstances, the position before the first character
 * is 0, the position after the first character is 1, and the position after the
 * last character is 1 plus the length of the string.
 * <li>The client can change the position of an iterator, or the text it analyzes,
 * at will, but cannot change the behavior.  If the user wants different behavior, he
 * must instantiate a new iterator.</ul>
 *
 * BreakIterator accesses the text it analyzes through a CharacterIterator, which makes
 * it possible to use BreakIterator to analyze text in any text-storage vehicle that
 * provides a CharacterIterator interface.
 *
 * <b>NOTE:</b>  Some types of BreakIterator can take a long time to create, and
 * instances of BreakIterator are not currently cached by the system.  For
 * optimal performance, keep instances of BreakIterator around as long as makes
 * sense.  For example, when word-wrapping a document, don't create and destroy a
 * new BreakIterator for each line.  Create one break iterator for the whole document
 * (or whatever stretch of text you're wrapping) and use it to do the whole job of
 * wrapping the text.
 *
  * <P>
 * <strong>Examples</strong>:<P>
 * Creating and using text boundaries
 * <blockquote>
 * <pre>
 * public static void main(String args[]) {
 *      if (args.length == 1) {
 *          String stringToExamine = args[0];
 *          //print each word in order
 *          BreakIterator boundary = BreakIterator.getWordInstance();
 *          boundary.setText(stringToExamine);
 *          printEachForward(boundary, stringToExamine);
 *          //print each sentence in reverse order
 *          boundary = BreakIterator.getSentenceInstance(Locale.US);
 *          boundary.setText(stringToExamine);
 *          printEachBackward(boundary, stringToExamine);
 *          printFirst(boundary, stringToExamine);
 *          printLast(boundary, stringToExamine);
 *      }
 * }
 * </pre>
 * </blockquote>
 *
 * Print each element in order
 * <blockquote>
 * <pre>
 * public static void printEachForward(BreakIterator boundary, String source) {
 *     int start = boundary.first();
 *     for (int end = boundary.next();
 *          end != BreakIterator.DONE;
 *          start = end, end = boundary.next()) {
 *          System.out.println(source.substring(start,end));
 *     }
 * }
 * </pre>
 * </blockquote>
 *
 * Print each element in reverse order
 * <blockquote>
 * <pre>
 * public static void printEachBackward(BreakIterator boundary, String source) {
 *     int end = boundary.last();
 *     for (int start = boundary.previous();
 *          start != BreakIterator.DONE;
 *          end = start, start = boundary.previous()) {
 *         System.out.println(source.substring(start,end));
 *     }
 * }
 * </pre>
 * </blockquote>
 *
 * Print first element
 * <blockquote>
 * <pre>
 * public static void printFirst(BreakIterator boundary, String source) {
 *     int start = boundary.first();
 *     int end = boundary.next();
 *     System.out.println(source.substring(start,end));
 * }
 * </pre>
 * </blockquote>
 *
 * Print last element
 * <blockquote>
 * <pre>
 * public static void printLast(BreakIterator boundary, String source) {
 *     int end = boundary.last();
 *     int start = boundary.previous();
 *     System.out.println(source.substring(start,end));
 * }
 * </pre>
 * </blockquote>
 *
 * Print the element at a specified position
 * <blockquote>
 * <pre>
 * public static void printAt(BreakIterator boundary, int pos, String source) {
 *     int end = boundary.following(pos);
 *     int start = boundary.previous();
 *     System.out.println(source.substring(start,end));
 * }
 * </pre>
 * </blockquote>
 *
 * Find the next word
 * <blockquote>
 * <pre>
 * public static int nextWordStartAfter(int pos, String text) {
 *     BreakIterator wb = BreakIterator.getWordInstance();
 *     wb.setText(text);
 *     int last = wb.following(pos);
 *     int current = wb.next();
 *     while (current != BreakIterator.DONE) {
 *         for (int p = last; p < current; p++) {
 *             if (Character.isLetter(text.charAt(p))
 *                 return last;
 *         }
 *         last = current;
 *         current = wb.next();
 *     }
 *     return BreakIterator.DONE;
 * }
 * </pre>
 * (The iterator returned by BreakIterator.getWordInstance() is unique in that
 * the break positions it returns don't represent both the start and end of the
 * thing being iterated over.  That is, a sentence-break iterator returns breaks
 * that each represent the end of one sentence and the beginning of the next.
 * With the word-break iterator, the characters between two boundaries might be a
 * word, or they might be the punctuation or whitespace between two words.  The
 * above code uses a simple heuristic to determine which boundary is the beginning
 * of a word: If the characters between this boundary and the next boundary
 * include at least one letter (this can be an alphabetical letter, a CJK ideograph,
 * a Hangul syllable, a Kana character, etc.), then the text between this boundary
 * and the next is a word; otherwise, it's the material between words.)
 * </blockquote>
 *
 * @see CharacterIterator
 *
 */

public abstract class BreakIterator implements Cloneable
{
    /**
     * Default constructor.  There is no state that is carried by this abstract
     * base class.
     */
    protected BreakIterator()
    {
    }

    /**
     * Clone method.  Creates another BreakIterator with the same behavior and
     * current state as this one.
     * @return The clone.
     */
    public Object clone()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * DONE is returned by previous() and next() after all valid
     * boundaries have been returned.
     */
    public static final int DONE = -1;

    /**
     * Return the first boundary position.  This is always the beginning
     * index of the text this iterator iterates over.  For example, if
     * the iterator iterates over a whole string, this function will
     * always return 0.  This function also updates the iteration position
     * to point to the beginning of the text.
     * @return The character offset of the beginning of the stretch of text
     * being broken.
     */
    public abstract int first();

    /**
     * Return the last boundary position.  This is always the "past-the-end"
     * index of the text this iterator iterates over.  For example, if the
     * iterator iterates over a whole string (call it "text"), this function
     * will always return text.length().  This function also updated the
     * iteration position to point to the end of the text.
     * @return The character offset of the end of the stretch of text
     * being broken.
     */
    public abstract int last();

    /**
     * Advances the specified number of steps forward in the text (a negative
     * number, therefore, advances backwards).  If this causes the iterator
     * to advance off either end of the text, this function returns DONE;
     * otherwise, this function returns the position of the appropriate
     * boundary.  Calling this function is equivalent to calling next() or
     * previous() n times.
     * @param n The number of boundaries to advance over (if positive, moves
     * forward; if negative, moves backwards).
     * @return The position of the boundary n boundaries from the current
     * iteration position, or DONE if moving n boundaries causes the iterator
     * to advance off either end of the text.
     */
    public abstract int next(int n);

    /**
     * Advances the iterator forward one boundary.  The current iteration
     * position is updated to point to the next boundary position after the
     * current position, and this is also the value that is returned.  If
     * the current position is equal to the value returned by last(), or to
     * DONE, this function returns DONE and sets the current position to
     * DONE.
     * @return The position of the first boundary position following the
     * iteration position.
     */
    public abstract int next();

    /**
     * Advances the iterator backward one boundary.  The current iteration
     * position is updated to point to the last boundary position before
     * the current position, and this is also the value that is returned.  If
     * the current position is equal to the value returned by first(), or to
     * DONE, this function returns DONE and sets the current position to
     * DONE.
     * @return The position of the last boundary position preceding the
     * iteration position.
     */
    public abstract int previous();

    /**
     * Sets the iterator's current iteration position to be the first
     * boundary position following the specified position.  (Whether the
     * specified position is itself a boundary position or not doesn't
     * matter-- this function always moves the iteration position to the
     * first boundary after the specified position.)  If the specified
     * position is the past-the-end position, returns DONE.
     * @param offset The character position to start searching from.
     * @return The position of the first boundary position following
     * "offset" (whether or not "offset" itself is a boundary position),
     * or DONE if "offset" is the past-the-end offset.
     */
    public abstract int following(int offset);

    /**
     * Sets the iterator's current iteration position to be the last
     * boundary position preceding the specified position.  (Whether the
     * specified position is itself a boundary position or not doesn't
     * matter-- this function always moves the iteration position to the
     * last boundary before the specified position.)  If the specified
     * position is the starting position, returns DONE.
     * @param offset The character position to start searching from.
     * @return The position of the last boundary position preceding
     * "offset" (whether of not "offset" itself is a boundary position),
     * or DONE if "offset" is the starting offset of the iterator.
     */
    public int preceding(int offset) {
        // NOTE:  This implementation is here solely because we can't add new
        // abstract methods to an existing class.  There is almost ALWAYS a
        // better, faster way to do this.
        int pos = following(offset);
        while (pos >= offset && pos != DONE)
            pos = previous();
        return pos;
    }

    /**
     * Return true if the specfied position is a boundary position.  If the
     * function returns true, the current iteration position is set to the
     * specified position; if the function returns false, the current
     * iteration position is set as though following() had been called.
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     */
    public boolean isBoundary(int offset) {
        // Again, this is the default implementation, which is provided solely because
        // we couldn't add a new abstract method to an existing class.  The real
        // implementations will usually need to do a little more work.
        if (offset == 0) {
            return true;
        }
        else
            return following(offset - 1) == offset;
    }

    /**
     * Return the iterator's current position.
     * @return The iterator's current position.
     */
    public abstract int current();

    /**
     * Returns a CharacterIterator over the text being analyzed.
     * For at least some subclasses of BreakIterator, this is a reference
     * to the <b>actual iterator being used</b> by the BreakIterator,
     * and therefore, this function's return value should be treated as
     * <tt>const</tt>.  No guarantees are made about the current position
     * of this iterator when it is returned.  If you need to move that
     * position to examine the text, clone this function's return value first.
     * @return A CharacterIterator over the text being analyzed.
     */
    public abstract CharacterIterator getText();

    /**
     * Sets the iterator to analyze a new piece of text.  The new
     * piece of text is passed in as a String, and the current
     * iteration position is reset to the beginning of the string.
     * (The old text is dropped.)
     * @param newText A String containing the text to analyze with
     * this BreakIterator.
     */
    public void setText(String newText)
    {
        setText(new StringCharacterIterator(newText));
    }

    /**
     * Sets the iterator to analyze a new piece of text.  The
     * BreakIterator is passed a CharacterIterator through which
     * it will access the text itself.  The current iteration
     * position is reset to the CharacterIterator's start index.
     * (The old iterator is dropped.)
     * @param newText A CharacterIterator referring to the text
     * to analyze with this BreakIterator (the iterator's current
     * position is ignored, but its other state is significant).
     */
    public abstract void setText(CharacterIterator newText);

    private static final int CHARACTER_INDEX = 0;                   //ibm.597
    private static final int WORD_INDEX = 1;                        //ibm.597
    private static final int LINE_INDEX = 2;                        //ibm.597
    private static final int SENTENCE_INDEX = 3;                    //ibm.597
    private static final SoftReference[] iterCache = new SoftReference[4];  //ibm.597

    /**
     * Returns a new instance of BreakIterator that locates word boundaries.
     * This function assumes that the text being analyzed is in the default
     * locale's language.
     * @return An instance of BreakIterator that locates word boundaries.
     */
    public static BreakIterator getWordInstance()
    {
        return getWordInstance(Locale.getDefault());
    }

    /**
     * Returns a new instance of BreakIterator that locates word boundaries.
     * @param where A locale specifying the language of the text to be
     * analyzed.
     * @return An instance of BreakIterator that locates word boundaries.
     */
    public static BreakIterator getWordInstance(Locale where)
    {
        return getBreakInstance(where,                              //ibm.597
                                WORD_INDEX,                         //ibm.597
                                "WordBreakRules",                   //ibm.597
                                "WordBreakDictionary");             //ibm.597
    }

    /**
     * Returns a new instance of BreakIterator that locates legal line-
     * wrapping positions.  This function assumes the text being broken
     * is in the default locale's language.
     * @returns A new instance of BreakIterator that locates legal
     * line-wrapping positions.
     */
    public static BreakIterator getLineInstance()
    {
        return getLineInstance(Locale.getDefault());
    }

    /**
     * Returns a new instance of BreakIterator that locates legal line-
     * wrapping positions.
     * @param where A Locale specifying the language of the text being broken.
     * @return A new instance of BreakIterator that locates legal
     * line-wrapping positions.
     */
    public static BreakIterator getLineInstance(Locale where)
    {
        return getBreakInstance(where,                              //ibm.597
                                LINE_INDEX,                         //ibm.597
                                "LineBreakRules",                   //ibm.597
                                "LineBreakDictionary");             //ibm.597
    }

    /**
     * Returns a new instance of BreakIterator that locates logical-character
     * boundaries.  This function assumes that the text being analyzed is
     * in the default locale's language.
     * @return A new instance of BreakIterator that locates logical-character
     * boundaries.
     */
    public static BreakIterator getCharacterInstance()
    {
        return getCharacterInstance(Locale.getDefault());
    }

    /**
     * Returns a new instance of BreakIterator that locates logical-character
     * boundaries.
     * @param where A Locale specifying the language of the text being analyzed.
     * @return A new instance of BreakIterator that locates logical-character
     * boundaries.
     */
    public static BreakIterator getCharacterInstance(Locale where)
    {
        return getBreakInstance(where,                              //ibm.597
                                CHARACTER_INDEX,                    //ibm.597
                                "CharacterBreakRules",              //ibm.597
                                "CharacterBreakDictionary");        //ibm.597
    }

    /**
     * Returns a new instance of BreakIterator that locates sentence boundaries.
     * This function assumes the text being analyzed is in the default locale's
     * language.
     * @return A new instance of BreakIterator that locates sentence boundaries.
     */
    public static BreakIterator getSentenceInstance()
    {
        return getSentenceInstance(Locale.getDefault());
    }

    /**
     * Returns a new instance of BreakIterator that locates sentence boundaries.
     * @param where A Locale specifying the language of the text being analyzed.
     * @return A new instance of BreakIterator that locates sentence boundaries.
     */
    public static BreakIterator getSentenceInstance(Locale where)
    {
        return getBreakInstance(where,                              //ibm.597
                                SENTENCE_INDEX,                     //ibm.597
                                "SentenceBreakRules",               //ibm.597
                                "SentenceBreakDictionary");         //ibm.597
    }                                                               //ibm.597

    private static BreakIterator getBreakInstance(Locale where,     //ibm.597
                                                  int type,         //ibm.597
                                                  String rulesName, //ibm.597
                                                  String dictionaryName) {  //ibm.597

        if (iterCache[type] != null) {                              //ibm.597
            BreakIteratorCache cache = (BreakIteratorCache) iterCache[type].get();  //ibm.597
            if (cache != null) {                                    //ibm.597
                if (cache.getLocale().equals(where)) {              //ibm.597
                    return cache.createBreakInstance();             //ibm.597
                }                                                   //ibm.597
            }                                                       //ibm.597
        }                                                           //ibm.597

        BreakIterator result = createBreakInstance(where,           //ibm.597
                                                   type,            //ibm.597
                                                   rulesName,       //ibm.597
                                                   dictionaryName); //ibm.597
        BreakIteratorCache cache = new BreakIteratorCache(where, result);   //ibm.597
        iterCache[type] = new SoftReference(cache);                 //ibm.597
        return result;                                              //ibm.597
    }                                                               //ibm.597

    private static BreakIterator createBreakInstance(Locale where,  //ibm.597
                                                     int type,      //ibm.597
                                                     String rulesName,  //ibm.597
                                                     String dictionaryName) {   //ibm.597

//		System.out.println("rulesName: "+rulesName);
//		System.out.println("dictionaryName: "+dictionaryName);
        ResourceBundle bundle = ResourceBundle.getBundle(           //ibm.597
                        "com.ibm.text.resources.BreakIteratorRules", where);   //ibm.597
        String[] classNames = bundle.getStringArray("BreakIteratorClasses");    //ibm.597

        String rules = bundle.getString(rulesName);                 //ibm.597
        
        if (classNames[type].equals("RuleBasedBreakIterator")) {    //ibm.597
            return new RuleBasedBreakIterator(rules);               //ibm.597
        }                                                           //ibm.597
        else if (classNames[type].equals("DictionaryBasedBreakIterator")) { //ibm.597
            try {                                                   //ibm.597
				// System.out.println(dictionaryName);
                Object t = bundle.getObject(dictionaryName);   //ibm.597
				// System.out.println(t);
                URL url = (URL)t;   //ibm.597
                InputStream dictionary = url.openStream();          //ibm.597
                return new DictionaryBasedBreakIterator(rules, dictionary); //ibm.597
            }                                                       //ibm.597
            catch(IOException e) {                                  //ibm.597
            }                                                       //ibm.597
            catch(MissingResourceException e) {                     //ibm.597
            }                                                       //ibm.597
            return new RuleBasedBreakIterator(rules);               //ibm.597
        }                                                           //ibm.597
        else                                                        //ibm.597
            throw new IllegalArgumentException("Invalid break iterator class \"" +  //ibm.597
                            classNames[type] + "\"");               //ibm.597
    }

    /**
     * Returns a list of locales for which BreakIterators can be used.
     * @return An array of Locales.  All of the locales in the array can
     * be used when creating a BreakIterator.
     */
    public static synchronized Locale[] getAvailableLocales()
    {
        //FIX ME - this is a known bug.  It should return
        //all locales.
        return LocaleData.getAvailableLocales("NumberPatterns");
    }

    private static final class BreakIteratorCache {                 //ibm.597

        private BreakIterator iter;                                 //ibm.597
        private Locale where;                                       //ibm.597

        BreakIteratorCache(Locale where, BreakIterator iter) {      //ibm.597
            this.where = where;                                     //ibm.597
            this.iter = (BreakIterator) iter.clone();               //ibm.597
        }                                                           //ibm.597

        Locale getLocale() {                                        //ibm.597
            return where;                                           //ibm.597
        }                                                           //ibm.597

        BreakIterator createBreakInstance() {                       //ibm.597
            return (BreakIterator) iter.clone();                    //ibm.597
        }                                                           //ibm.597
    }                                                               //ibm.597
}
