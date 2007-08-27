/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.util.Vector;
import java.util.Stack;
import com.ibm.icu.impl.Assert;
import java.text.CharacterIterator;
import java.io.InputStream;
import java.io.IOException;


/**
 * A subclass of RuleBasedBreakIterator that adds the ability to use a dictionary
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
public class DictionaryBasedBreakIterator extends RuleBasedBreakIterator {

    /**
     * a list of known words that is used to divide up contiguous ranges of letters,
     * stored in a compressed, indexed, format that offers fast access
     */
    private BreakDictionary dictionary;

    /*
     * a list of flags indicating which character categories are contained in
     * the dictionary file (this is used to determine which ranges of characters
     * to apply the dictionary to)
     */
    //private boolean[] categoryFlags;


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
 
    /**
     * Constructs a DictionaryBasedBreakIterator.
     * @param rules Same as the rules parameter on RuleBasedBreakIterator,
     * except for the special meaning of "_dictionary_".  This parameter is just
     * passed through to RuleBasedBreakIterator constructor.
     * @param dictionaryStream the stream containing the dictionary data
     * @stable ICU 2.0
     */
    public DictionaryBasedBreakIterator(String rules,
                                        InputStream dictionaryStream) throws IOException {
        super(rules);
        dictionary = new BreakDictionary(dictionaryStream);
    }

    
    /**
     * Construct a DictionarBasedBreakIterator from precompiled rules.
     * @param compiledRules an input stream containing the binary (flattened) compiled rules.
     * @param dictionaryStream an input stream containing the dictionary data
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public DictionaryBasedBreakIterator(InputStream compiledRules,
                                         InputStream dictionaryStream) throws IOException {
       fRData = RBBIDataWrapper.get(compiledRules);   // Init the RBBI part of this iterator.
       dictionary = new BreakDictionary(dictionaryStream);
    }
                    

    /** @stable ICU 2.0 */
    public void setText(CharacterIterator newText) {
        super.setText(newText);
        cachedBreakPositions = null;
        fDictionaryCharCount = 0;
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
        fDictionaryCharCount = 0;
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
        fDictionaryCharCount = 0;
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
        // case we have to mark our position in the cache. If it doesn't, use next()
        // to move forward until we hit or pass the current position. This *will* fill
        // the cache.
        else {
            cachedBreakPositions = null;
            int offset = current();
            int result = super.previous();
            
            if (cachedBreakPositions != null) {
                positionInCache = cachedBreakPositions.length - 2;
                return result;
            }
            
            while (result < offset) {
                int nextResult = next();
                
                if (nextResult >= offset) {
                    break;
                }
                
                result = nextResult;
            }
            
            if (cachedBreakPositions != null) {
                positionInCache = cachedBreakPositions.length - 2;
            }
            
            if (result != BreakIterator.DONE) {
                text.setIndex(result);
            }
            
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
     * Return the status tag from the break rule that determined the most recently
     * returned break position. 
     * 
     * TODO:  not supported with dictionary based break iterators.
     *
     * @return the status from the break rule that determined the most recently
     * returned break position.
     * @draft ICU 3.0
     * @provisional This API might change or be removed in a future release.
     */
     public int getRuleStatus() {
        return 0;
     }


    /**
     * Get the status (tag) values from the break rule(s) that determined the most 
     * recently returned break position.  The values appear in the rule source
     * within brackets, {123}, for example.  The default status value for rules
     * that do not explicitly provide one is zero.
     * <p>
     * TODO: not supported for dictionary based break iterator. 
     *
     * @param fillInArray an array to be filled in with the status values.  
     * @return          The number of rule status values from rules that determined 
     *                  the most recent boundary returned by the break iterator.
     *                  In the event that the array is too small, the return value
     *                  is the total number of status values that were available,
     *                  not the reduced number that were actually returned.
     * @draft ICU 3.0
     * @provisional This API might change or be removed in a future release.
     */
    public int getRuleStatusVec(int[] fillInArray) {
        if (fillInArray != null && fillInArray.length>=1) {  
            fillInArray[0] = 0;
        }
        return 1;
    }



    /**
     * This is the implementation function for next().
     * @internal
     * @deprecated This API is ICU internal only.
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
            fDictionaryCharCount = 0;
            int result = super.handleNext();

            // if we passed over more than one dictionary character, then we use
            // divideUpDictionaryRange() to regenerate the cached break positions
            // for the new range
            if (fDictionaryCharCount > 1 && result - startPos > 1) {
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
        Assert.assrt(false);
        return -9999;   // SHOULD NEVER GET HERE!
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
        int c = CICurrent32(text);
        while (isDictionaryChar(c) == false) {  
            c = CINext32(text);
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
        c = CICurrent32(text);
        while (true) {
//System.out.print("c = " + Integer.toString(c, 16) + ", pos = " + text.getIndex());

            // if we can transition to state "-1" from our current state, we're
            // on the last character of a legal word.  Push that position onto
            // the possible-break-positions stack
            if (dictionary.at(state, 0) == -1) {
                possibleBreakPositions.push(new Integer(text.getIndex()));
            }

            // look up the new state to transition to in the dictionary
            //    There will be no supplementaries here because the Thai dictionary
            //     does not include any.  This code is going away soon, not worth
            //     fixing.
            state = (dictionary.at(state, (char)c)) & 0xFFFF;  // TODO: fix supplementaries
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
                        CINext32(text);
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
                c = CICurrent32(text);
                state = 0;
                if (text.getIndex() >= endPos) {
                    break;
                }
            }

            // if we didn't hit any exceptional conditions on this last iteration,
            // just advance to the next character and loop
            else {
                c = CINext32(text);
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
}
