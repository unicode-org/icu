/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
// Requires Java2
package com.ibm.richtext.textformat;

import com.ibm.richtext.styledtext.MConstText;
import java.text.CharacterIterator;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

///*JDK12IMPORTS
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.Set;
//JDK12IMPORTS*/

/*JDK11IMPORTS
import com.ibm.richtext.textlayout.attributes.AttributedCharacterIterator;
import com.ibm.richtext.textlayout.attributes.AttributedCharacterIterator.Attribute;
import com.ibm.richtext.textlayout.attributes.Map;
JDK11IMPORTS*/

/**
 * An AttributedCharacterIterator over an MConstText.
 */
public final class MTextIterator implements AttributedCharacterIterator,
                                            Cloneable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    // memory leak, since this cache is never flushed

    private static class Matcher {
        
        boolean matches(Map lhs, Map rhs, Object query) {
            
            Object lhsVal = lhs.get(query);
            Object rhsVal = rhs.get(query);
            
            if (lhsVal == null) {
                return rhsVal == null;
            }
            else {
                return lhsVal.equals(rhsVal);
            }
        }
    }
    
    private static final Matcher ATTR_MATCHER = new Matcher();
    
    // Not quite optimal.  Could have a matcher that would decompose
    // a set once for repeated queries.  Of course that would require
    // allocation...
///*JDK12IMPORTS
    private static final Matcher SET_MATCHER = new Matcher() {
        
        boolean matches(Map lhs, Map rhs, Object query) {
            
            // Not using Iterator to simplify 1.1 port.
            Object[] elements = ((Set)query).toArray();
            for (int i=0; i < elements.length; i++) {
                if (!super.matches(lhs, rhs, elements[i])) {
                    return false;
                }
            }
            return true;
        }
    };
//JDK12IMPORTS*/
    
    private final class StyleCache {

        private int fRunStart = 0;
        private int fRunLimit = -1;
        private int fRangeStart;
        private int fRangeLimit;
        private AttributeMap fStyle;

        StyleCache(MConstText text, int start, int limit) {
            fText = text;
            fRangeStart = start;
            fRangeLimit = limit;
            update(start);
        }

        private void update(int pos) {
            if (pos < fRunStart || pos >= fRunLimit) {
                AttributeMap style = AttributeMap.EMPTY_ATTRIBUTE_MAP;
                if (pos < fRangeStart) {
                    fRunLimit = fRangeStart;
                    fRunStart = Integer.MIN_VALUE;
                }
                else if (pos > fRangeLimit) {
                    fRunStart = fRangeLimit;
                    fRunLimit = Integer.MAX_VALUE;
                }
                else {
                    fRunStart = Math.max(fRangeStart, fText.characterStyleStart(pos));
                    fRunStart = Math.max(fRunStart, fText.paragraphStart(pos));
                    
                    fRunLimit = Math.min(fRangeLimit, fText.characterStyleLimit(pos));
                    fRunLimit = Math.min(fRunLimit, fText.paragraphLimit(pos));
                    if (fRunStart < fRunLimit) {
                        style = fText.paragraphStyleAt(pos);
                        style = style.addAttributes(fText.characterStyleAt(pos));
                    }
                }
                fStyle = fFontResolver.applyFont(style);
            }
        }

        int getRunStart(int pos) {
            update(pos);
            return fRunStart;
        }

        int getRunLimit(int pos) {
            update(pos);
            return fRunLimit;
        }

        Map getStyle(int pos) {
            update(pos);
            return fStyle;
        }
    }

    private MConstText fText;
    private CharacterIterator fCharIter;
    private FontResolver fFontResolver;
    
    private StyleCache fStyleCache;

    /**
     * Create an MTextIterator over the range [start, limit).
     */
    public MTextIterator(MConstText text, 
                         FontResolver resolver,
                         int start, 
                         int limit) {

        fText = text;
        fFontResolver = resolver;
        fCharIter = text.createCharacterIterator(start, limit);

        fStyleCache = new StyleCache(text, start, limit);
    }

    /**
     * Sets the position to getBeginIndex() and returns the character at that
     * position.
     * @return the first character in the text, or DONE if the text is empty
     * @see #getBeginIndex
     */
    public char first() {
        return fCharIter.first();
    }

    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
     * and returns the character at that position.
     * @return the last character in the text, or DONE if the text is empty
     * @see #getEndIndex
     */
    public char last() {
        return fCharIter.last();
    }

    /**
     * Gets the character at the current position (as returned by getIndex()).
     * @return the character at the current position or DONE if the current
     * position is off the end of the text.
     * @see #getIndex
     */
    public char current() {
        return fCharIter.current();
    }

    /**
     * Increments the iterator's index by one and returns the character
     * at the new index.  If the resulting index is greater or equal
     * to getEndIndex(), the current index is reset to getEndIndex() and
     * a value of DONE is returned.
     * @return the character at the new position or DONE if the new
     * position is off the end of the text range.
     */
    public char next() {
        return fCharIter.next();
    }

    /**
     * Decrements the iterator's index by one and returns the character
     * at the new index. If the current index is getBeginIndex(), the index
     * remains at getBeginIndex() and a value of DONE is returned.
     * @return the character at the new position or DONE if the current
     * position is equal to getBeginIndex().
     */
    public char previous() {
        return fCharIter.previous();
    }

    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     * @param position the position within the text.  Valid values range from
     * getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     * if an invalid value is supplied.
     * @return the character at the specified position or DONE if the specified position is equal to getEndIndex()
     */
    public char setIndex(int position) {
        return fCharIter.setIndex(position);
    }

    /**
     * Returns the start index of the text.
     * @return the index at which the text begins.
     */
    public int getBeginIndex() {
        return fCharIter.getBeginIndex();
    }

    /**
     * Returns the end index of the text.  This index is the index of the first
     * character following the end of the text.
     * @return the index after the last character in the text
     */
    public int getEndIndex() {
        return fCharIter.getEndIndex();
    }

    /**
     * Returns the current index.
     * @return the current index.
     */
    public int getIndex() {
        return fCharIter.getIndex();
    }

    /**
     * Returns the index of the first character of the run
     * with respect to all attributes containing the current character.
     */
    public int getRunStart() {
        return fStyleCache.getRunStart(fCharIter.getIndex());
    }

    /**
     * Returns the index of the first character of the run
     * with respect to the given attribute containing the current character.
     */
    public int getRunStart(Object attribute) {

        return getRunStart(attribute, ATTR_MATCHER);
    }

    /**
     * Returns the index of the first character of the run
     * with respect to the given attribute containing the current character.
     */
///*JDK12IMPORTS
    public int getRunStart(Attribute attribute) {

        return getRunStart(attribute, ATTR_MATCHER);
    }
//JDK12IMPORTS*/

    /**
     * Returns the index of the first character of the run
     * with respect to the given attributes containing the current character.
     */
///*JDK12IMPORTS
    public int getRunStart(Set attributes) {

        return getRunStart(attributes, SET_MATCHER);
    }
//JDK12IMPORTS*/
    
    private int getRunStart(Object query, Matcher matcher) {

        int runStart = getRunStart();
        int rangeStart = getBeginIndex();
        Map initialStyle = getAttributes();
        
        while (runStart > rangeStart) {
            AttributeMap style = fText.characterStyleAt(runStart-1);
            if (!matcher.matches(initialStyle, style, query)) {
                return runStart;
            }
            runStart = fText.characterStyleStart(runStart-1);
        }
        return rangeStart;
    }
    
    /**
     * Returns the index of the first character following the run
     * with respect to all attributes containing the current character.
     */
    public int getRunLimit() {
        return fStyleCache.getRunLimit(fCharIter.getIndex());
    }

    /**
     * Returns the index of the first character following the run
     * with respect to the given attribute containing the current character.
     */
    public int getRunLimit(Object attribute) {
        
        return getRunLimit(attribute, ATTR_MATCHER);
    }

    /**
     * Returns the index of the first character following the run
     * with respect to the given attribute containing the current character.
     */
///*JDK12IMPORTS
    public int getRunLimit(Attribute attribute) {
        
        return getRunLimit(attribute, ATTR_MATCHER);
    }
//JDK12IMPORTS*/

    /**
     * Returns the index of the first character following the run
     * with respect to the given attributes containing the current character.
     */
///*JDK12IMPORTS
    public int getRunLimit(Set attributes) {
        
        return getRunLimit(attributes, SET_MATCHER);
    }
//JDK12IMPORTS*/

    private int getRunLimit(Object query, Matcher matcher) {

        int runLimit = getRunLimit();
        int rangeLimit = getEndIndex();
        Map initialStyle = getAttributes();
        
        while (runLimit < rangeLimit) {
            AttributeMap style = fText.characterStyleAt(runLimit);
            if (!matcher.matches(initialStyle, style, query)) {
                return runLimit;
            }
            runLimit = fText.characterStyleLimit(runLimit);
        }
        return rangeLimit;
    }
    
    /**
     * Returns a map with the attributes defined on the current
     * character.
     */
    public Map getAttributes() {
        return fStyleCache.getStyle(fCharIter.getIndex());
    }

    /**
     * Returns the value of the named attribute for the current character.
     * Returns null if the attribute is not defined.
     * @param attribute the key of the attribute whose value is requested.
     */
    public Object getAttribute(Object attribute) {
        return getAttributes().get(attribute);
    }

    /**
     * Returns the value of the named attribute for the current character.
     * Returns null if the attribute is not defined.
     * @param attribute the key of the attribute whose value is requested.
     */
    public Object getAttribute(Attribute attribute) {
        return getAttributes().get(attribute);
    }

    /**
     * Returns the keys of all attributes defined on the
     * iterator's text range. The set is empty if no
     * attributes are defined.
     */
///*JDK12IMPORTS
    public Set getAllAttributeKeys() {
        throw new Error("Implement this method!");
    }
//JDK12IMPORTS*/

    public Object clone() {
        return new MTextIterator(fText,
                                 fFontResolver,
                                 getBeginIndex(),
                                 getEndIndex());
    }
}
