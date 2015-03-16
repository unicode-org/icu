/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.text.CharacterIterator;
import java.util.HashSet;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.FilteredBreakIteratorBuilder;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.CharsTrie;
import com.ibm.icu.util.CharsTrieBuilder;
import com.ibm.icu.util.StringTrieBuilder;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * @author tomzhang
 */
public class SimpleFilteredSentenceBreakIterator extends BreakIterator {

    private BreakIterator delegate;
    private UCharacterIterator text; // TODO(Tom): suffice to move into the local scope in next() ?
    private CharsTrie backwardsTrie; // i.e. ".srM" for Mrs.
    private CharsTrie forwardsPartialTrie; // Has ".a" for "a.M."

    /**
     * @param adoptBreakIterator
     *            break iterator to adopt
     * @param forwardsPartialTrie
     *            forward & partial char trie to adopt
     * @param backwardsTrie
     *            backward trie to adopt
     */
    public SimpleFilteredSentenceBreakIterator(BreakIterator adoptBreakIterator, CharsTrie forwardsPartialTrie,
            CharsTrie backwardsTrie) {
        this.delegate = adoptBreakIterator;
        this.forwardsPartialTrie = forwardsPartialTrie;
        this.backwardsTrie = backwardsTrie;
    }

    @Override
    public int next() {
        int n = delegate.next();
        if (n == BreakIterator.DONE || // at end or
                backwardsTrie == null) { // .. no backwards table loaded == no exceptions
            return n;
        }
        // UCharacterIterator text;
        text = UCharacterIterator.getInstance((CharacterIterator) delegate.getText().clone());
        do { // outer loop runs once per underlying break (from fDelegate).
             // loops while 'n' points to an exception.
            text.setIndex(n);
            backwardsTrie.reset();
            int uch;

            // Assume a space is following the '.' (so we handle the case: "Mr. /Brown")
            if ((uch = text.previousCodePoint()) == ' ') { // TODO: skip a class of chars here??
                // TODO only do this the 1st time?
            } else {
                uch = text.nextCodePoint();
            }

            BytesTrie.Result r = BytesTrie.Result.INTERMEDIATE_VALUE;

            int bestPosn = -1;
            int bestValue = -1;

            while ((uch = text.previousCodePoint()) != BreakIterator.DONE && // more to consume backwards and..
                    ((r = backwardsTrie.nextForCodePoint(uch)).hasNext())) {// more in the trie
                if (r.hasValue()) { // remember the best match so far
                    bestPosn = text.getIndex();
                    bestValue = backwardsTrie.getValue();
                }
            }

            if (r.matches()) { // exact match?
                bestValue = backwardsTrie.getValue();
                bestPosn = text.getIndex();
            }

            if (bestPosn >= 0) {
                if (bestValue == Builder.MATCH) { // exact match!
                    n = delegate.next(); // skip this one. Find the next lowerlevel break.
                    if (n == BreakIterator.DONE)
                        return n;
                    continue; // See if the next is another exception.
                } else if (bestValue == Builder.PARTIAL && forwardsPartialTrie != null) {
                    // make sure there's a forward trie
                    // We matched the "Ph." in "Ph.D." - now we need to run everything through the forwards trie
                    // to see if it matches something going forward.
                    forwardsPartialTrie.reset();

                    BytesTrie.Result rfwd = BytesTrie.Result.INTERMEDIATE_VALUE;
                    text.setIndex(bestPosn); // hope that's close ..
                    while ((uch = text.nextCodePoint()) != BreakIterator.DONE
                            && ((rfwd = forwardsPartialTrie.nextForCodePoint(uch)).hasNext())) {
                    }
                    if (rfwd.matches()) {
                        // only full matches here, nothing to check
                        // skip the next:
                        n = delegate.next();
                        if (n == BreakIterator.DONE)
                            return n;
                        continue;
                    } else {
                        // no match (no exception) -return the 'underlying' break
                        return n;
                    }
                } else {
                    return n; // internal error and/or no forwards trie
                }
            } else {
                return n; // No match - so exit. Not an exception.
            }
        } while (n != BreakIterator.DONE);
        return n;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        SimpleFilteredSentenceBreakIterator other = (SimpleFilteredSentenceBreakIterator) obj;
        return delegate.equals(other.delegate) && text.equals(other.text) && backwardsTrie.equals(other.backwardsTrie)
                && forwardsPartialTrie.equals(other.forwardsPartialTrie);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return -1;  // arbitrary constant
    }

    @Override
    public Object clone() {
        SimpleFilteredSentenceBreakIterator other = (SimpleFilteredSentenceBreakIterator) super.clone();
        return other;
    }

    @Override
    public int first() {
        return delegate.first();
    }

    @Override
    public int last() {
        return delegate.last();
    }

    @Override
    public int next(int n) {
        // TODO
        throw new UnsupportedOperationException("next(int) is not yet implemented");
    }

    @Override
    public int previous() {
        // TODO
        throw new UnsupportedOperationException("previous() is not yet implemented");
    }

    @Override
    public int following(int offset) {
        // TODO
        throw new UnsupportedOperationException("following(int) is not yet implemented");
    }

    @Override
    public int current() {
        return delegate.current();
    }

    @Override
    public int preceding(int offset) {
        // TODO
        throw new UnsupportedOperationException("preceding(int) is not yet implemented");
    }

    @Override
    public CharacterIterator getText() {
        return delegate.getText();
    }

    @Override
    public void setText(CharacterIterator newText) {
        delegate.setText(newText);
    }

    public static class Builder extends FilteredBreakIteratorBuilder {
        /**
         * filter set to store all exceptions
         */
        private HashSet<String> filterSet;

        static final int PARTIAL = (1 << 0); // < partial - need to run through forward trie
        static final int MATCH = (1 << 1); // < exact match - skip this one.
        static final int SuppressInReverse = (1 << 0);
        static final int AddToForward = (1 << 1);

        /**
         * Create SimpleFilteredBreakIteratorBuilder using given locale
         * @param loc the locale to get filtered iterators
         */
        public Builder(ULocale loc) {
            ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(
                    ICUResourceBundle.ICU_BRKITR_BASE_NAME, loc);
            ICUResourceBundle exceptions = rb.findWithFallback("exceptions");
            ICUResourceBundle breaks = exceptions.findWithFallback("SentenceBreak");

            filterSet = new HashSet<String>();
            if (breaks != null) {
                for (int index = 0, size = breaks.getSize(); index < size; ++index) {
                    ICUResourceBundle b = (ICUResourceBundle) breaks.get(index);
                    String br = b.getString();
                    filterSet.add(br);
                }
            }
        }

        /**
         * Create SimpleFilteredBreakIteratorBuilder with no exception
         */
        public Builder() {
            filterSet = new HashSet<String>();
        }

        @Override
        public boolean suppressBreakAfter(String str) {
            if (filterSet == null) {
                filterSet = new HashSet<String>();
            }
            return filterSet.add(str);
        }

        @Override
        public boolean unsuppressBreakAfter(String str) {
            if (filterSet == null) {
                return false;
            } else {
                return filterSet.remove(str);
            }
        }

        @Override
        public BreakIterator build(BreakIterator adoptBreakIterator) {
            CharsTrieBuilder builder = new CharsTrieBuilder();
            CharsTrieBuilder builder2 = new CharsTrieBuilder();

            int revCount = 0;
            int fwdCount = 0;

            int subCount = filterSet.size();
            String[] ustrs = new String[subCount];
            int[] partials = new int[subCount];

            CharsTrie backwardsTrie = null; // i.e. ".srM" for Mrs.
            CharsTrie forwardsPartialTrie = null; // Has ".a" for "a.M."

            int i = 0;
            for (String s : filterSet) {
                ustrs[i] = s; // copy by value?
                partials[i] = 0; // default: no partial
                i++;
            }

            for (i = 0; i < subCount; i++) {
                int nn = ustrs[i].indexOf('.'); // TODO: non-'.' abbreviations
                if (nn > -1 && (nn + 1) != ustrs[i].length()) {
                    // is partial.
                    // is it unique?
                    int sameAs = -1;
                    for (int j = 0; j < subCount; j++) {
                        if (j == i)
                            continue;
                        if (ustrs[i].regionMatches(0, ustrs[j], 0, nn + 1)) {
                            if (partials[j] == 0) { // hasn't been processed yet
                                partials[j] = SuppressInReverse | AddToForward;
                            } else if ((partials[j] & SuppressInReverse) != 0) {
                                sameAs = j; // the other entry is already in the reverse table.
                            }
                        }
                    }

                    if ((sameAs == -1) && (partials[i] == 0)) {
                        StringBuilder prefix = new StringBuilder(ustrs[i].substring(0, nn + 1));
                        // first one - add the prefix to the reverse table.
                        prefix.reverse();
                        builder.add(prefix, PARTIAL);
                        revCount++;
                        partials[i] = SuppressInReverse | AddToForward;
                    }
                }
            }

            for (i = 0; i < subCount; i++) {
                if (partials[i] == 0) {
                    StringBuilder reversed = new StringBuilder(ustrs[i]).reverse();
                    builder.add(reversed, MATCH);
                    revCount++;
                } else {
                    // an optimization would be to only add the portion after the '.'
                    // for example, for "Ph.D." we store ".hP" in the reverse table. We could just store "D." in the
                    // forward,
                    // instead of "Ph.D." since we already know the "Ph." part is a match.
                    // would need the trie to be able to hold 0-length strings, though.
                    builder2.add(ustrs[i], MATCH); // forward
                    fwdCount++;
                }
            }

            if (revCount > 0) {
                backwardsTrie = builder.build(StringTrieBuilder.Option.FAST);
            }

            if (fwdCount > 0) {
                forwardsPartialTrie = builder2.build(StringTrieBuilder.Option.FAST);
            }
            return new SimpleFilteredSentenceBreakIterator(adoptBreakIterator, forwardsPartialTrie, backwardsTrie);
        }
    }
}
