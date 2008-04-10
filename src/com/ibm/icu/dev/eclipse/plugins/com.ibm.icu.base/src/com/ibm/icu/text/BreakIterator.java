/*
 *******************************************************************************
 * Copyright (C) 1996-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * A class that locates boundaries in text.  This class defines a protocol for
 * objects that break up a piece of natural-language text according to a set
 * of criteria.  Instances or subclasses of BreakIterator can be provided, for
 * example, to break a piece of text into words, sentences, or logical characters
 * according to the conventions of some language or group of languages.
 *
 * We provide five built-in types of BreakIterator:
 * <ul><li>getTitleInstance() returns a BreakIterator that locates boundaries
 * between title breaks.
 * <li>getSentenceInstance() returns a BreakIterator that locates boundaries
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
 * @stable ICU 2.0
 *
 */
public abstract class BreakIterator implements Cloneable {
	
    protected BreakIterator() {
    }

    /**
     * Create a copy of this iterator
     * @return A copy of this
     */
    public Object clone() {
    	// this is here for subclass use.  we must override it ourselves, though.
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public int preceding(int offset) {
        int pos = following(offset);
        while (pos >= offset && pos != DONE) {
            pos = previous();
        }
        return pos;
    }
        
    /**
     * Return true if the specfied position is a boundary position.  If the
     * function returns true, the current iteration position is set to the
     * specified position; if the function returns false, the current
     * iteration position is set as though following() had been called.
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     * @stable ICU 2.0
     */
    public boolean isBoundary(int offset) {
    	return offset == 0 || following(offset - 1) == offset;    	
    }
        
    /**
     * Return the iterator's current position.
     * @return The iterator's current position.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public abstract CharacterIterator getText();
        
    /**
     * Sets the iterator to analyze a new piece of text.  The new
     * piece of text is passed in as a String, and the current
     * iteration position is reset to the beginning of the string.
     * (The old text is dropped.)
     * @param newText A String containing the text to analyze with
     * this BreakIterator.
     * @stable ICU 2.0
     */
    public void setText(String newText) {
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
     * @stable ICU 2.0
     */
    public abstract void setText(CharacterIterator newText);
        
    /** @stable ICU 2.4 */
    public static final int KIND_CHARACTER = 0;
    /** @stable ICU 2.4 */
    public static final int KIND_WORD = 1;
    /** @stable ICU 2.4 */
    public static final int KIND_LINE = 2;
    /** @stable ICU 2.4 */
    public static final int KIND_SENTENCE = 3;
    /** @stable ICU 2.4 */
    public static final int KIND_TITLE = 4;
        
    /**
     * Returns a new instance of BreakIterator that locates word boundaries.
     * This function assumes that the text being analyzed is in the default
     * locale's language.
     * @return An instance of BreakIterator that locates word boundaries.
     * @stable ICU 2.0
     */
    public static BreakIterator getWordInstance() {
        return getWordInstance(Locale.getDefault());
    }
        
    /**
     * Returns a new instance of BreakIterator that locates word boundaries.
     * @param where A locale specifying the language of the text to be
     * analyzed.
     * @return An instance of BreakIterator that locates word boundaries.
     * @stable ICU 2.0
     */
    public static BreakIterator getWordInstance(Locale where) {
        return getBreakInstance(where, KIND_WORD);
    }
        
    /**
     * Returns a new instance of BreakIterator that locates word boundaries.
     * @param where A locale specifying the language of the text to be
     * analyzed.
     * @return An instance of BreakIterator that locates word boundaries.
     * @stable ICU 3.4.3
     */
    public static BreakIterator getWordInstance(ULocale where) {
        return getBreakInstance(where.toLocale(), KIND_WORD);
    }
        
    /**
     * Returns a new instance of BreakIterator that locates legal line-
     * wrapping positions.  This function assumes the text being broken
     * is in the default locale's language.
     * @return A new instance of BreakIterator that locates legal
     * line-wrapping positions.
     * @stable ICU 2.0
     */
    public static BreakIterator getLineInstance() {
        return getLineInstance(Locale.getDefault());
    }
        
    /**
     * Returns a new instance of BreakIterator that locates legal line-
     * wrapping positions.
     * @param where A Locale specifying the language of the text being broken.
     * @return A new instance of BreakIterator that locates legal
     * line-wrapping positions.
     * @stable ICU 2.0
     */
    public static BreakIterator getLineInstance(Locale where) {
        return getBreakInstance(where, KIND_LINE);
    }
        
    /**
     * Returns a new instance of BreakIterator that locates legal line-
     * wrapping positions.
     * @param where A Locale specifying the language of the text being broken.
     * @return A new instance of BreakIterator that locates legal
     * line-wrapping positions.
     * @stable ICU 3.4.3
     */
    public static BreakIterator getLineInstance(ULocale where) {
        return getBreakInstance(where.toLocale(), KIND_LINE);
    }
        
    /**
     * Returns a new instance of BreakIterator that locates logical-character
     * boundaries.  This function assumes that the text being analyzed is
     * in the default locale's language.
     * @return A new instance of BreakIterator that locates logical-character
     * boundaries.
     * @stable ICU 2.0
     */
    public static BreakIterator getCharacterInstance() {
        return getCharacterInstance(Locale.getDefault());
    }
        
    /**
     * Returns a new instance of BreakIterator that locates logical-character
     * boundaries.
     * @param where A Locale specifying the language of the text being analyzed.
     * @return A new instance of BreakIterator that locates logical-character
     * boundaries.
     * @stable ICU 2.0
     */
    public static BreakIterator getCharacterInstance(Locale where) {
        return getBreakInstance(where, KIND_CHARACTER);
    }
        
    /**
     * Returns a new instance of BreakIterator that locates logical-character
     * boundaries.
     * @param where A Locale specifying the language of the text being analyzed.
     * @return A new instance of BreakIterator that locates logical-character
     * boundaries.
     * @draft ICU 3.2
     */
    public static BreakIterator getCharacterInstance(ULocale where) {
        return getBreakInstance(where.toLocale(), KIND_CHARACTER);
    }
        
    /**
     * Returns a new instance of BreakIterator that locates sentence boundaries.
     * This function assumes the text being analyzed is in the default locale's
     * language.
     * @return A new instance of BreakIterator that locates sentence boundaries.
     * @stable ICU 2.0
     */
    public static BreakIterator getSentenceInstance() {
        return getSentenceInstance(Locale.getDefault());
    }
        
    /**
     * Returns a new instance of BreakIterator that locates sentence boundaries.
     * @param where A Locale specifying the language of the text being analyzed.
     * @return A new instance of BreakIterator that locates sentence boundaries.
     * @stable ICU 2.0
     */
    public static BreakIterator getSentenceInstance(Locale where) {
        return getBreakInstance(where, KIND_SENTENCE);
    }
        
    /**
     * Returns a new instance of BreakIterator that locates sentence boundaries.
     * @param where A Locale specifying the language of the text being analyzed.
     * @return A new instance of BreakIterator that locates sentence boundaries.
     * @stable ICU 3.4.3
     */
    public static BreakIterator getSentenceInstance(ULocale where) {
        return getBreakInstance(where.toLocale(), KIND_SENTENCE);
    }
        
    private static BreakIterator getBreakInstance(Locale where, int kind) {
        java.text.BreakIterator br = null;
        switch(kind) {
        case KIND_CHARACTER: br = java.text.BreakIterator.getCharacterInstance(where); break;
        case KIND_WORD: br = java.text.BreakIterator.getWordInstance(where); break;
        case KIND_LINE: br = java.text.BreakIterator.getLineInstance(where); break;
        case KIND_SENTENCE: br = java.text.BreakIterator.getSentenceInstance(where); break;             
        case KIND_TITLE: throw new UnsupportedOperationException();
        }
        return new BreakIteratorHandle(br);
    }
        
    /**
     * Returns a list of locales for which BreakIterators can be used.
     * @return An array of Locales.  All of the locales in the array can
     * be used when creating a BreakIterator.
     * @stable ICU 3.4.3
     */
    public static synchronized Locale[] getAvailableLocales() {
        return java.text.BreakIterator.getAvailableLocales();
    }
        
    /**
     * Returns a list of locales for which BreakIterators can be used.
     * @return An array of ULocales.  All of the locales in the array can
     * be used when creating a BreakIterator.
     * @stable ICU 3.4.3
     */
    public static synchronized ULocale[] getAvailableULocales() {
        Locale[] locales = java.text.BreakIterator.getAvailableLocales();
        ULocale[] ulocales = new ULocale[locales.length];
        for (int i = 0; i < locales.length; ++i) {
            ulocales[i] = ULocale.forLocale(locales[i]);
        }
        return ulocales;
    }
    
    // forwarding implementation class
	static final class BreakIteratorHandle extends BreakIterator {
	    /**
	     * @internal
	     */
	    public final java.text.BreakIterator breakIterator;
	        
	    /**
	     * @internal
	     * @param delegate the BreakIterator to which to delegate
	     */
	    public BreakIteratorHandle(java.text.BreakIterator delegate) {
	        this.breakIterator = delegate;
	    }
	        
	    public int first() {
	        return breakIterator.first();
	    }
	    public int last() {
	        return breakIterator.last();
	    }
	    public int next(int n) {
	        return breakIterator.next(n);
	    }
	    public int next() {
	        return breakIterator.next();
	    }
	    public int previous() {
	        return breakIterator.previous();
	    }
	    public int following(int offset) {
	        return breakIterator.following(offset);
	    }
	    public int preceding(int offset) {
	        return breakIterator.preceding(offset);
	    }
	    public boolean isBoundary(int offset) {
	        return breakIterator.isBoundary(offset);
	    }
	    public int current() {
	        return breakIterator.current();
	    }
	    public CharacterIterator getText() {
	        return breakIterator.getText();
	    }
	    public void setText(CharacterIterator newText) {
	        breakIterator.setText(newText);
	    }
	
	    /**
	     * Return a string suitable for debugging.
	     * @return a string suitable for debugging
	     * @stable ICU 3.4.3
	     */
	    public String toString() {
	    	return breakIterator.toString();
	    }

	    /**
	     * Return a clone of this BreakIterator.
	     * @return a clone of this BreakIterator
	     * @stable ICU 3.4.3
	     */
	    public Object clone() {
	    	return new BreakIteratorHandle((java.text.BreakIterator)breakIterator.clone());
	    }

	    /**
	     * Return true if rhs is a BreakIterator with the same break behavior as this.
	     * @return true if rhs equals this
	     * @stable ICU 3.4.3
	     */
	    public boolean equals(Object rhs) {
	    	try {
	    		return breakIterator.equals(((BreakIteratorHandle)rhs).breakIterator);
	    	}
	    	catch (Exception e) {
	    		return false;
	    	}
	    }

	    /**
	     * Return a hashCode.
	     * @return a hashCode
	     * @stable ICU 3.4.3
	     */
	    public int hashCode() {
	    	return breakIterator.hashCode();
	    }
	}
}
