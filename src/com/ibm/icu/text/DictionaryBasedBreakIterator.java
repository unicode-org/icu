/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;
import java.text.CharacterIterator;
import java.io.InputStream;
import java.io.IOException;

import java.io.*;

/**
 * A subclass of RuleBasedBreakIterator_Old that adds the ability to use a dictionary
 * to further subdivide ranges of text beyond what is possible using just the
 * state-table-based algorithm.  This is necessary, for example, to handle
 * word and line breaking in Thai, which doesn't use spaces between words.  The
 * state-table-based algorithm used by RuleBasedBreakIterator_Old is used to divide
 * up text as far as possible, and then contiguous ranges of letters are
 * repeatedly compared against a list of known words (i.e., the dictionary)
 * to divide them up into words.
 *
 * DictionaryBasedBreakIterator uses the same rule language as RuleBasedBreakIterator_Old,
 * but adds one more special substitution name: _dictionary_.  This substitution
 * name is used to identify characters in words in the dictionary.  The idea is that
 * if the iterator passes over a chunk of text that includes two or more characters
 * in a row that are included in _dictionary_, it goes back through that range and
 * derives additional break positions (if possible) using the dictionary.
 *
 * DictionaryBasedBreakIterator is also constructed with the filename of a dictionary
 * file.  It uses Class.getResource() to locate the dictionary file.  The
 * dictionary file is in a serialized binary format.  We have a very primitive (and
 * slow) BuildDictionaryFile utility for creating dictionary files, but aren't
 * currently making it public.  Contact us for help.
 *
 * @stable ICU 2.0
 */
public class DictionaryBasedBreakIterator extends RuleBasedBreakIterator_Old {

    /**
     * a list of known words that is used to divide up contiguous ranges of letters,
     * stored in a compressed, indexed, format that offers fast access
     */
    private BreakDictionary dictionary;

    /**
     * a list of flags indicating which character categories are contained in
     * the dictionary file (this is used to determine which ranges of characters
     * to apply the dictionary to)
     */
    private boolean[] categoryFlags;

    /**
     * a temporary hiding place for the number of dictionary characters in the
     * last range passed over by next()
     */
    private int dictionaryCharCount;

    /**
     * when a range of characters is divided up using the dictionary, the break
     * positions that are discovered are stored here, preventing us from having
     * to use either the dictionary or the state table again until the iterator
     * leaves this range of text
     */
    private int[] cachedBreakPositions;

    /**
     * if cachedBreakPositions is not null, this indicates which item in the
     * cache the current iteration position refers to
     */
    private int positionInCache;

    /**
     * Special variable name for characters in words in dictionary
     */
    private static final String DICTIONARY_VAR = "_dictionary_";

    /**
     * Constructs a DictionaryBasedBreakIterator.
     * @param description Same as the description parameter on RuleBasedBreakIterator_Old,
     * except for the special meaning of DICTIONARY_VAR.  This parameter is just
     * passed through to RuleBasedBreakIterator_Old's constructor.
     * @param dictionaryStream the stream containing the dictionary data
     * @stable ICU 2.0
     */
    public DictionaryBasedBreakIterator(String description,
                                        InputStream dictionaryStream) throws IOException {
        super(description);
        dictionary = new BreakDictionary(dictionaryStream);
    }

    /**
     * Returns a Builder that is customized to build a DictionaryBasedBreakIterator.
     * This is the same as RuleBasedBreakIterator_Old.Builder, except for the extra code
     * to handle the DICTIONARY_VAR tag.
     * @internal
     */
    protected RuleBasedBreakIterator_Old.Builder makeBuilder() {
        return new Builder();
    }

/** @internal */
public void writeTablesToFile(FileOutputStream file, boolean littleEndian) throws IOException {
super.writeTablesToFile(file, littleEndian);

DataOutputStream out = new DataOutputStream(file);

// --- write index to fields (there's only one entry, but this allows subclassing of this class)
writeSwappedInt((short)8, out, littleEndian);
writeSwappedInt((short)(categoryFlags.length + 3 & 0x0f), out, littleEndian);

for (int i = 0; i < categoryFlags.length; i++)
    out.writeBoolean(categoryFlags[i]);
switch (categoryFlags.length % 4) {
    case 1: out.write(0);
    case 2: out.write(0);
    case 3: out.write(0);
    default: break;
}
}

    /** @stable ICU 2.0 */
    public void setText(CharacterIterator newText) {
        super.setText(newText);
        cachedBreakPositions = null;
        dictionaryCharCount = 0;
        positionInCache = 0;
    }

    /**
     * Sets the current iteration position to the beginning of the text.
     * (i.e., the CharacterIterator's starting offset).
     * @return The offset of the beginning of the text.
     * @stable ICU 2.0
     */
    public int first() {
        cachedBreakPositions = null;
        dictionaryCharCount = 0;
        positionInCache = 0;
        return super.first();
    }

    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     * @stable ICU 2.0
     */
    public int last() {
        cachedBreakPositions = null;
        dictionaryCharCount = 0;
        positionInCache = 0;
        return super.last();
    }

    /**
     * Advances the iterator one step backwards.
     * @return The position of the last boundary position before the
     * current iteration position
     * @stable ICU 2.0
     */
    public int previous() {
        CharacterIterator text = getText();

        // if we have cached break positions and we're still in the range
        // covered by them, just move one step backward in the cache
        if (cachedBreakPositions != null && positionInCache > 0) {
            --positionInCache;
            text.setIndex(cachedBreakPositions[positionInCache]);
            return cachedBreakPositions[positionInCache];
        }

        // otherwise, dump the cache and use the inherited previous() method to move
        // backward.  This may fill up the cache with new break positions, in which
        // case we have to mark our position in the cache
        else {
            cachedBreakPositions = null;
            int result = super.previous();
            if (cachedBreakPositions != null)
                positionInCache = cachedBreakPositions.length - 2;
            return result;
        }
    }

    /**
     * Sets the current iteration position to the last boundary position
     * before the specified position.
     * @param offset The position to begin searching from
     * @return The position of the last boundary before "offset"
     * @stable ICU 2.0
     */
    public int preceding(int offset) {
        CharacterIterator text = getText();
        checkOffset(offset, text);

        // if we have no cached break positions, or "offset" is outside the
        // range covered by the cache, we can just call the inherited routine
        // (which will eventually call other routines in this class that may
        // refresh the cache)
        if (cachedBreakPositions == null || offset <= cachedBreakPositions[0] ||
                offset > cachedBreakPositions[cachedBreakPositions.length - 1]) {
            cachedBreakPositions = null;
            return super.preceding(offset);
        }

        // on the other hand, if "offset" is within the range covered by the cache,
        // then all we have to do is search the cache for the last break position
        // before "offset"
        else {
            positionInCache = 0;
            while (positionInCache < cachedBreakPositions.length
                   && offset > cachedBreakPositions[positionInCache])
                ++positionInCache;
            --positionInCache;
            text.setIndex(cachedBreakPositions[positionInCache]);
            return text.getIndex();
        }
    }

    /**
     * Sets the current iteration position to the first boundary position after
     * the specified position.
     * @param offset The position to begin searching forward from
     * @return The position of the first boundary after "offset"
     * @stable ICU 2.0
     */
    public int following(int offset) {
        CharacterIterator text = getText();
        checkOffset(offset, text);

        // if we have no cached break positions, or if "offset" is outside the
        // range covered by the cache, then dump the cache and call our
        // inherited following() method.  This will call other methods in this
        // class that may refresh the cache.
        if (cachedBreakPositions == null || offset < cachedBreakPositions[0] ||
                offset >= cachedBreakPositions[cachedBreakPositions.length - 1]) {
            cachedBreakPositions = null;
            return super.following(offset);
        }

        // on the other hand, if "offset" is within the range covered by the
        // cache, then just search the cache for the first break position
        // after "offset"
        else {
            positionInCache = 0;
            while (positionInCache < cachedBreakPositions.length
                   && offset >= cachedBreakPositions[positionInCache])
                ++positionInCache;
            text.setIndex(cachedBreakPositions[positionInCache]);
            return text.getIndex();
        }
    }

    /**
     * This is the implementation function for next().
     * @internal
     */
    protected int handleNext() {
        CharacterIterator text = getText();

        // if there are no cached break positions, or if we've just moved
        // off the end of the range covered by the cache, we have to dump
        // and possibly regenerate the cache
        if (cachedBreakPositions == null || positionInCache == cachedBreakPositions.length - 1) {

            // start by using the inherited handleNext() to find a tentative return
            // value.   dictionaryCharCount tells us how many dictionary characters
            // we passed over on our way to the tentative return value
            int startPos = text.getIndex();
            dictionaryCharCount = 0;
            int result = super.handleNext();

            // if we passed over more than one dictionary character, then we use
            // divideUpDictionaryRange() to regenerate the cached break positions
            // for the new range
            if (dictionaryCharCount > 1 && result - startPos > 1) {
                divideUpDictionaryRange(startPos, result);
            }

            // otherwise, the value we got back from the inherited fuction
            // is our return value, and we can dump the cache
            else {
                cachedBreakPositions = null;
                return result;
            }
        }

        // if the cache of break positions has been regenerated (or existed all
        // along), then just advance to the next break position in the cache
        // and return it
        if (cachedBreakPositions != null) {
            ++positionInCache;
            text.setIndex(cachedBreakPositions[positionInCache]);
            return cachedBreakPositions[positionInCache];
        }
        return -9999;   // SHOULD NEVER GET HERE!
    }

    /**
     * Looks up a character category for a character.
     * @internal
     */
    protected int lookupCategory(char c) {
        // this override of lookupCategory() exists only to keep track of whether we've
        // passed over any dictionary characters.  It calls the inherited lookupCategory()
        // to do the real work, and then checks whether its return value is one of the
        // categories represented in the dictionary.  If it is, bump the dictionary-
        // character count.
        int result = super.lookupCategory(c);
        if (result != RuleBasedBreakIterator_Old.IGNORE && categoryFlags[result]) {
            ++dictionaryCharCount;
        }
        return result;
    }

    /**
     * This is the function that actually implements the dictionary-based
     * algorithm.  Given the endpoints of a range of text, it uses the
     * dictionary to determine the positions of any boundaries in this
     * range.  It stores all the boundary positions it discovers in
     * cachedBreakPositions so that we only have to do this work once
     * for each time we enter the range.
     */
    private void divideUpDictionaryRange(int startPos, int endPos) {
        CharacterIterator text = getText();

        // the range we're dividing may begin or end with non-dictionary characters
        // (i.e., for line breaking, we may have leading or trailing punctuation
        // that needs to be kept with the word).  Seek from the beginning of the
        // range to the first dictionary character
        text.setIndex(startPos);
        char c = text.current();
        int category = lookupCategory(c);
        while (category == IGNORE || !categoryFlags[category]) {
            c = text.next();
            category = lookupCategory(c);
        }
//System.out.println("\nDividing up range from " + (text.getIndex() + 1) + " to " + endPos);

        // initialize.  We maintain two stacks: currentBreakPositions contains
        // the list of break positions that will be returned if we successfully
        // finish traversing the whole range now.  possibleBreakPositions lists
        // all other possible word ends we've passed along the way.  (Whenever
        // we reach an error [a sequence of characters that can't begin any word
        // in the dictionary], we back up, possibly delete some breaks from
        // currentBreakPositions, move a break from possibleBreakPositions
        // to currentBreakPositions, and start over from there.  This process
        // continues in this way until we either successfully make it all the way
        // across the range, or exhaust all of our combinations of break
        // positions.)
        Stack currentBreakPositions = new Stack();
        Stack possibleBreakPositions = new Stack();
        Vector wrongBreakPositions = new Vector();

        // the dictionary is implemented as a trie, which is treated as a state
        // machine.  -1 represents the end of a legal word.  Every word in the
        // dictionary is represented by a path from the root node to -1.  A path
        // that ends in state 0 is an illegal combination of characters.
        int state = 0;

        // these two variables are used for error handling.  We keep track of the
        // farthest we've gotten through the range being divided, and the combination
        // of breaks that got us that far.  If we use up all possible break
        // combinations, the text contains an error or a word that's not in the
        // dictionary.  In this case, we "bless" the break positions that got us the
        // farthest as real break positions, and then start over from scratch with
        // the character where the error occurred.
        int farthestEndPoint = text.getIndex();
        Stack bestBreakPositions = null;

        // initialize (we always exit the loop with a break statement)
        c = text.current();
        while (true) {
//System.out.print("c = " + Integer.toString(c, 16) + ", pos = " + text.getIndex());

            // if we can transition to state "-1" from our current state, we're
            // on the last character of a legal word.  Push that position onto
            // the possible-break-positions stack
            if (dictionary.at(state, 0) == -1) {
                possibleBreakPositions.push(new Integer(text.getIndex()));
            }

            // look up the new state to transition to in the dictionary
            state = (dictionary.at(state, c)) & 0xFFFF;
//System.out.print(", state = " + state);

            // if the character we're sitting on causes us to transition to
            // the "end of word" state, then it was a non-dictionary character
            // and we've successfully traversed the whole range.  Drop out
            // of the loop.
            if (state == /*-1*/ 0xFFFF) {
                currentBreakPositions.push(new Integer(text.getIndex()));
                break;
            }

            // if the character we're sitting on causes us to transition to
            // the error state, or if we've gone off the end of the range
            // without transitioning to the "end of word" state, we've hit
            // an error...
            else if (state == 0 || text.getIndex() >= endPos) {

                // if this is the farthest we've gotten, take note of it in
                // case there's an error in the text
                if (text.getIndex() > farthestEndPoint) {
                    farthestEndPoint = text.getIndex();
                    bestBreakPositions = (Stack)(currentBreakPositions.clone());
                }

                // wrongBreakPositions is a list of all break positions we've tried starting
                // that didn't allow us to traverse all the way through the text.  Every time
                // we pop a break position off of currentBreakPositions, we put it into
                // wrongBreakPositions to avoid trying it again later.  If we make it to this
                // spot, we're either going to back up to a break in possibleBreakPositions
                // and try starting over from there, or we've exhausted all possible break
                // positions and are going to do the fallback procedure.  This loop prevents
                // us from messing with anything in possibleBreakPositions that didn't work as
                // a starting point the last time we tried it (this is to prevent a bunch of
                // repetitive checks from slowing down some extreme cases)
                // variable not used Integer newStartingSpot = null;
                while (!possibleBreakPositions.isEmpty() && wrongBreakPositions.contains(
                            possibleBreakPositions.peek())) {
                    possibleBreakPositions.pop();
                }

                // if we've used up all possible break-position combinations, there's
                // an error or an unknown word in the text.  In this case, we start
                // over, treating the farthest character we've reached as the beginning
                // of the range, and "blessing" the break positions that got us that
                // far as real break positions
                if (possibleBreakPositions.isEmpty()) {
                    if (bestBreakPositions != null) {
                        currentBreakPositions = bestBreakPositions;
                        if (farthestEndPoint < endPos) {
                            text.setIndex(farthestEndPoint + 1);
                        }
                        else {
                            break;
                        }
                    }
                    else {
                        if ((currentBreakPositions.size() == 0
                                || ((Integer)(currentBreakPositions.peek())).intValue() != text.getIndex())
                                && text.getIndex() != startPos) {
                            currentBreakPositions.push(new Integer(text.getIndex()));
                        }
                        text.next();
                        currentBreakPositions.push(new Integer(text.getIndex()));
                    }
                }

                // if we still have more break positions we can try, then promote the
                // last break in possibleBreakPositions into currentBreakPositions,
                // and get rid of all entries in currentBreakPositions that come after
                // it.  Then back up to that position and start over from there (i.e.,
                // treat that position as the beginning of a new word)
                else {
                    Integer temp = (Integer)possibleBreakPositions.pop();
                    Object temp2 = null;
                    while (!currentBreakPositions.isEmpty() && temp.intValue() <
                           ((Integer)currentBreakPositions.peek()).intValue()) {
                        temp2 = currentBreakPositions.pop();
                        wrongBreakPositions.addElement(temp2);
                    }
                    currentBreakPositions.push(temp);
                    text.setIndex(((Integer)currentBreakPositions.peek()).intValue());
                }

                // re-sync "c" for the next go-round, and drop out of the loop if
                // we've made it off the end of the range
                c = text.current();
                state = 0;
                if (text.getIndex() >= endPos) {
                    break;
                }
            }

            // if we didn't hit any exceptional conditions on this last iteration,
            // just advance to the next character and loop
            else {
                c = text.next();
            }
//System.out.print(", possibleBreakPositions = { "); for (int i = 0; i < possibleBreakPositions.size(); i++) System.out.print(possibleBreakPositions.elementAt(i) + " "); System.out.print("}");
//System.out.print(", currentBreakPositions = { "); for (int i = 0; i < currentBreakPositions.size(); i++) System.out.print(currentBreakPositions.elementAt(i) + " "); System.out.println("}");
        }

        // dump the last break position in the list, and replace it with the actual
        // end of the range (which may be the same character, or may be further on
        // because the range actually ended with non-dictionary characters we want to
        // keep with the word)
        if (!currentBreakPositions.isEmpty()) {
            currentBreakPositions.pop();
        }
        currentBreakPositions.push(new Integer(endPos));

        // create a regular array to hold the break positions and copy
        // the break positions from the stack to the array (in addition,
        // our starting position goes into this array as a break position).
        // This array becomes the cache of break positions used by next()
        // and previous(), so this is where we actually refresh the cache.
        cachedBreakPositions = new int[currentBreakPositions.size() + 1];
        cachedBreakPositions[0] = startPos;

        for (int i = 0; i < currentBreakPositions.size(); i++) {
            cachedBreakPositions[i + 1] = ((Integer)currentBreakPositions.elementAt(i)).intValue();
        }
        positionInCache = 0;
    }

    /**
     * The Builder class for DictionaryBasedBreakIterator inherits almost all of
     * its functionality from the Builder class for RuleBasedBreakIterator_Old, but
     * extends it with extra logic to handle the DICTIONARY_VAR token
     * @internal
     */
    protected class Builder extends RuleBasedBreakIterator_Old.Builder {

        /**
         * A UnicodeSet that contains all the characters represented in the dictionary
         */
        private UnicodeSet dictionaryChars = new UnicodeSet();
        private String dictionaryExpression = "";

        /**
         * No special initialization
     * @internal
         */
        public Builder() {
        }

        /**
         * We override handleSpecialSubstitution() to add logic to handle
         * the $dictionary tag.  If we see a substitution named DICTIONARY_VAR,
         * parse the substitution expression and store the result in
         * dictionaryChars.
     * @internal
         */
        protected void handleSpecialSubstitution(String replace, String replaceWith,
                                                 int startPos, String description) {
            super.handleSpecialSubstitution(replace, replaceWith, startPos, description);

            if (replace.equals(DICTIONARY_VAR)) {
                if (replaceWith.charAt(0) == '(') {
                    error("Dictionary group can't be enclosed in (", startPos, description);
                }
                dictionaryExpression = replaceWith;
                dictionaryChars = new UnicodeSet(replaceWith, false);
            }
        }

        /**
         * The other half of the logic to handle the dictionary characters happens here.
         * After the inherited builder has derived the real character categories, we
         * set up the categoryFlags array in the iterator.  This array contains "true"
         * for every character category that includes a dictionary character.
     * @internal
         */
        protected void buildCharCategories(Vector tempRuleList) {
            super.buildCharCategories(tempRuleList);

            categoryFlags = new boolean[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                UnicodeSet cs = (UnicodeSet)categories.elementAt(i);

                cs.retainAll(dictionaryChars);
                if (!cs.isEmpty()) {
                    categoryFlags[i] = true;
                }
            }
        }

        // This function is actually called by RuleBasedBreakIterator.buildCharCategories(),
        // which is called by the function above.  This gives us a way to create a separate
        // character category for the dictionary characters even when RuleBasedBreakIterator
        // isn't making a distinction
    /**
     * @internal
     */
        protected void mungeExpressionList(Hashtable expressions) {
            expressions.put(dictionaryExpression, dictionaryChars);
        }
    }
}
