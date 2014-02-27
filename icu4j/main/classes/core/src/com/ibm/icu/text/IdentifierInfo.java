/*
 ***************************************************************************
 * Copyright (C) 2008-2014, Google, International Business Machines Corporation
 * and others. All Rights Reserved.
 ***************************************************************************
 */
package com.ibm.icu.text;

import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.SpoofChecker.RestrictionLevel;

/**
 * This class analyzes a possible identifier for script and identifier status. Use it by calling setIdentifierProfile
 * then setIdentifier. Available methods include:
 * <ol>
 * <li>call getScripts for the specific scripts in the identifier. The identifier contains at least one character in
 * each of these.
 * <li>call getAlternates to get cases where a character is not limited to a single script. For example, it could be
 * either Katakana or Hiragana.
 * <li>call getCommonAmongAlternates to find out if any scripts are common to all the alternates.
 * <li>call getNumerics to get a representative character (with value zero) for each of the decimal number systems in
 * the identifier.
 * <li>call getRestrictionLevel to see what the UTS36 restriction level is.
 * </ol>
 * 
 * @author markdavis
 * @internal
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class IdentifierInfo {

    private static final UnicodeSet ASCII = new UnicodeSet(0, 0x7F).freeze();

    private String identifier;
    private final BitSet requiredScripts = new BitSet();
    private final Set<BitSet> scriptSetSet = new HashSet<BitSet>();
    private final BitSet commonAmongAlternates = new BitSet();
    private final UnicodeSet numerics = new UnicodeSet();
    private final UnicodeSet identifierProfile = new UnicodeSet(0, 0x10FFFF);

    /**
     * Create an identifier info object. Subsequently, call {@link #setIdentifier(String)}, etc.
     * {@link #setIdentifierProfile(UnicodeSet)}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public IdentifierInfo() {
        super();
    }

    private IdentifierInfo clear() {
        requiredScripts.clear();
        scriptSetSet.clear();
        numerics.clear();
        commonAmongAlternates.clear();
        return this;
    }

    /**
     * Set the identifier profile: the characters that are to be allowed in the identifier.
     * 
     * @param identifierProfile the characters that are to be allowed in the identifier
     * @return self
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    
    public IdentifierInfo setIdentifierProfile(UnicodeSet identifierProfile) {
        this.identifierProfile.set(identifierProfile);
        return this;
    }

    /**
     * Get the identifier profile: the characters that are to be allowed in the identifier.
     * 
     * @return The characters that are to be allowed in the identifier.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    
    public UnicodeSet getIdentifierProfile() {
        return new UnicodeSet(identifierProfile);
    }

    /**
     * Set an identifier to analyze. Afterwards, call methods like getScripts()
     * 
     * @param identifier the identifier to analyze
     * @return self
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated    
    public IdentifierInfo setIdentifier(String identifier) {
        this.identifier = identifier;
        clear();
        BitSet scriptsForCP = new BitSet();
        int cp;
        for (int i = 0; i < identifier.length(); i += Character.charCount(i)) {
            cp = Character.codePointAt(identifier, i);
            // Store a representative character for each kind of decimal digit
            if (UCharacter.getType(cp) == UCharacterCategory.DECIMAL_DIGIT_NUMBER) {
                // Just store the zero character as a representative for comparison. Unicode guarantees it is cp - value
                numerics.add(cp - UCharacter.getNumericValue(cp));
            }
            UScript.getScriptExtensions(cp, scriptsForCP);
            scriptsForCP.clear(UScript.COMMON);
            scriptsForCP.clear(UScript.INHERITED);
            //            if (temp.cardinality() == 0) {
            //                // HACK for older version of ICU
            //                requiredScripts.set(UScript.getScript(cp));
            //            } else 
            switch (scriptsForCP.cardinality()) {
            case 0: break;
            case 1:
                // Single script, record it.
                requiredScripts.or(scriptsForCP);
                break;
            default:
                if (!requiredScripts.intersects(scriptsForCP) 
                        && scriptSetSet.add(scriptsForCP)) {
                    scriptsForCP = new BitSet();
                }
                break;
            }
        }
        // Now make a final pass through to remove alternates that came before singles.
        // [Kana], [Kana Hira] => [Kana]
        // This is relatively infrequent, so doesn't have to be optimized.
        // We also compute any commonalities among the alternates.
        if (scriptSetSet.size() > 0) {
            commonAmongAlternates.set(0, UScript.CODE_LIMIT);
            for (Iterator<BitSet> it = scriptSetSet.iterator(); it.hasNext();) {
                final BitSet next = it.next();
                // [Kana], [Kana Hira] => [Kana]
                if (requiredScripts.intersects(next)) {
                    it.remove();
                } else {
                    // [[Arab Syrc Thaa]; [Arab Syrc]] => [[Arab Syrc]]
                    commonAmongAlternates.and(next); // get the intersection.
                    for (BitSet other : scriptSetSet) {
                        if (next != other && contains(next, other)) {
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }
        if (scriptSetSet.size() == 0) {
            commonAmongAlternates.clear();
        }
        return this;
    }

    /**
     * Get the identifier that was analyzed.
     * 
     * @return the identifier that was analyzed.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Get the scripts found in the identifiers.
     * 
     * @return the set of explicit scripts.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public BitSet getScripts() {
        return (BitSet) requiredScripts.clone();
    }

    /**
     * Get the set of alternate scripts found in the identifiers. That is, when a character can be in two scripts, then
     * the set consisting of those scripts will be returned.
     * 
     * @return the set of explicit scripts.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public Set<BitSet> getAlternates() {
        Set<BitSet> result = new HashSet<BitSet>();
        for (BitSet item : scriptSetSet) {
            result.add((BitSet) item.clone());
        }
        return result;
    }

    /**
     * Get the representative characters (zeros) for the numerics found in the identifier.
     * 
     * @return the set of explicit scripts.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public UnicodeSet getNumerics() {
        return new UnicodeSet(numerics);
    }

    /**
     * Find out which scripts are in common among the alternates.
     * 
     * @return the set of scripts that are in common among the alternates.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public BitSet getCommonAmongAlternates() {
        return (BitSet) commonAmongAlternates.clone();
    }

    // BitSet doesn't support "contains(...)", so we have inverted constants
    // They are private; they can't be made immutable in Java.
    private final static BitSet JAPANESE = set(new BitSet(), UScript.LATIN, UScript.HAN, UScript.HIRAGANA,
            UScript.KATAKANA);
    private final static BitSet CHINESE = set(new BitSet(), UScript.LATIN, UScript.HAN, UScript.BOPOMOFO);
    private final static BitSet KOREAN = set(new BitSet(), UScript.LATIN, UScript.HAN, UScript.HANGUL);
    private final static BitSet CONFUSABLE_WITH_LATIN = set(new BitSet(), UScript.CYRILLIC, UScript.GREEK,
            UScript.CHEROKEE);

    /**
     * Find the "tightest" restriction level that the identifier satisfies.
     * 
     * @return the restriction level.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public RestrictionLevel getRestrictionLevel() {
        if (!identifierProfile.containsAll(identifier) || getNumerics().size() > 1) {
            return RestrictionLevel.UNRESTRICTIVE;
        }
        if (ASCII.containsAll(identifier)) {
            return RestrictionLevel.ASCII;
        }
        // This is a bit tricky. We look at a number of factors.
        //   The number of scripts in the text.
        //   Plus 1 if there is some commonality among the alternates (eg [Arab Thaa]; [Arab Syrc])
        //   Plus number of alternates otherwise (this only works because we only test cardinality up to 2.)
        
        // Note: the requiredScripts set omits COMMON and INHERITED; they are taken out at the
        //       time it is created, in setIdentifier().
        final int cardinalityPlus = requiredScripts.cardinality() + (commonAmongAlternates.cardinality() == 0 ? scriptSetSet.size() : 1);
        if (cardinalityPlus < 2) {
            return RestrictionLevel.SINGLE_SCRIPT_RESTRICTIVE;
        }
        if (containsWithAlternates(JAPANESE, requiredScripts) || containsWithAlternates(CHINESE, requiredScripts)
                || containsWithAlternates(KOREAN, requiredScripts)) {
            return RestrictionLevel.HIGHLY_RESTRICTIVE;
        }
        if (cardinalityPlus == 2 && requiredScripts.get(UScript.LATIN) && !requiredScripts.intersects(CONFUSABLE_WITH_LATIN)) {
            return RestrictionLevel.MODERATELY_RESTRICTIVE;
        }
        return RestrictionLevel.MINIMALLY_RESTRICTIVE;
    }

    /**
     * Get the number of scripts appearing in the identifier.
     *   Note: Common and Inherited scripts are omitted from the count.
     *   Note: If the identifier contains characters with alternate scripts
     *         (the character is used with more than one script), minimize
     *         the reported number of scripts by considering the character
     *         to be of a script that already appears elsewhere in the identifier
     *         when possible.
     *         The alternate script computation may not be perfect. The distinction
     *         between 0, 1 and > 1 scripts will be valid, however.
     * @return the number of scripts.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int getScriptCount() {
        // Note: Common and Inherited scripts were removed by setIdentifier(), and do not appear in fRequiredScripts.
        int count = requiredScripts.cardinality() +
                (commonAmongAlternates.cardinality() == 0 ? scriptSetSet.size() : 1);
        return count;

    }

    /**
     * See Object.toString()
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    @Override
    public String toString() {
        return identifier + ", " + identifierProfile.toPattern(false) + ", " + getRestrictionLevel() + ", "
                + displayScripts(requiredScripts) + ", " + displayAlternates(scriptSetSet) + ", "
                + numerics.toPattern(false);
    }

    private boolean containsWithAlternates(BitSet container, BitSet containee) {
        if (!contains(container, containee)) {
            return false;
        }
        for (BitSet alternatives : scriptSetSet) {
            if (!container.intersects(alternatives)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Produce a readable string of alternates.
     * 
     * @param alternates a set of BitSets of script values.
     * @return display form
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static String displayAlternates(Set<BitSet> alternates) {
        if (alternates.size() == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        // for consistent results
        Set<BitSet> sorted = new TreeSet<BitSet>(BITSET_COMPARATOR);
        sorted.addAll(alternates);
        for (BitSet item : sorted) {
            if (result.length() != 0) {
                result.append("; ");
            }
            result.append(displayScripts(item));
        }
        return result.toString();
    }
    
    /**
     * Order BitSets, first by shortest, then by items.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final Comparator<BitSet> BITSET_COMPARATOR = new Comparator<BitSet>() {

        public int compare(BitSet arg0, BitSet arg1) {
            int diff = arg0.cardinality() - arg1.cardinality();
            if (diff != 0) return diff;
            int i0 = arg0.nextSetBit(0);
            int i1 = arg1.nextSetBit(0);
            while ((diff = i0-i1) == 0 && i0 > 0) {
                i0 = arg0.nextSetBit(i0+1);
                i1 = arg1.nextSetBit(i1+1);
            }
            return diff;
        }
        
    };

    /**
     * Produce a readable string of a set of scripts
     * 
     * @param scripts a BitSet of UScript values
     * @return a readable string of a set of scripts
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static String displayScripts(BitSet scripts) {
        StringBuilder result = new StringBuilder();
        for (int i = scripts.nextSetBit(0); i >= 0; i = scripts.nextSetBit(i + 1)) {
            if (result.length() != 0) {
                result.append(' ');
            }
            result.append(UScript.getShortName(i));
        }
        return result.toString();
    }

    /**
     * Parse a text list of scripts into a BitSet.
     * 
     * @param scriptsString the string to be parsed
     * @return BitSet of UScript values.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static BitSet parseScripts(String scriptsString) {
        BitSet result = new BitSet();
        for (String item : scriptsString.trim().split(",?\\s+")) {
            if (item.length() != 0) {
                result.set(UScript.getCodeFromName(item));
            }
        }
        return result;
    }

    /**
     * Parse a list of alternates into a set of sets of UScript values.
     * 
     * @param scriptsSetString a list of alternates, separated by ;
     * @return a set of BitSets of UScript values
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Set<BitSet> parseAlternates(String scriptsSetString) {
        Set<BitSet> result = new HashSet<BitSet>();
        for (String item : scriptsSetString.trim().split("\\s*;\\s*")) {
            if (item.length() != 0) {
                result.add(parseScripts(item));
            }
        }
        return result;
    }

    /**
     * Test containment. Should be a method on BitSet.
     * 
     * @param container possible container to be tested
     * @param containee possible containee to be tested
     * @return true if container contains containee
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final boolean contains(BitSet container, BitSet containee) {
        for (int i = containee.nextSetBit(0); i >= 0; i = containee.nextSetBit(i + 1)) {
            if (!container.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets a number of values at once. Should be on BitSet.
     * 
     * @param bitset bitset to be affected
     * @param values values to be set in the bitset
     * @return modified bitset.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static final BitSet set(BitSet bitset, int... values) {
        for (int value : values) {
            bitset.set(value);
        }
        return bitset;
    }
    
    
    // public static final class FreezableBitSet extends BitSet implements Freezable<FreezableBitSet> {
    // private boolean frozen;
    //
    // public FreezableBitSet() {
    // super();
    // }
    // public FreezableBitSet(int nbits) {
    // super(nbits);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#and(java.util.BitSet)
    // */
    // @Override
    // public void and(BitSet set) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.and(set);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#andNot(java.util.BitSet)
    // */
    // @Override
    // public void andNot(BitSet set) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.andNot(set);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#cardinality()
    // */
    //
    // @Override
    // public void clear() {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.clear();
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#clear(int)
    // */
    // @Override
    // public void clear(int bitIndex) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.clear(bitIndex);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#clear(int, int)
    // */
    // @Override
    // public void clear(int fromIndex, int toIndex) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.clear(fromIndex, toIndex);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#clone()
    // */
    // @Override
    // public Object clone() {
    // return super.clone();
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#equals(java.lang.Object)
    // */
    // @Override
    // public boolean equals(Object obj) {
    // if (obj == null || obj.getClass() != FreezableBitSet.class) {
    // return false;
    // }
    // return super.equals((BitSet)obj);
    // }
    //
    // /* (non-Javadoc)
    // * @see java.util.BitSet#flip(int)
    // */
    // @Override
    // public void flip(int bitIndex) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.flip(bitIndex);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#flip(int, int)
    // */
    // @Override
    // public void flip(int fromIndex, int toIndex) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.flip(fromIndex, toIndex);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#or(java.util.BitSet)
    // */
    // @Override
    // public void or(BitSet set) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.or(set);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#set(int)
    // */
    // @Override
    // public void set(int bitIndex) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.set(bitIndex);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#set(int, boolean)
    // */
    // @Override
    // public void set(int bitIndex, boolean value) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.set(bitIndex, value);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#set(int, int)
    // */
    // @Override
    // public void set(int fromIndex, int toIndex) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.set(fromIndex, toIndex);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#set(int, int, boolean)
    // */
    // @Override
    // public void set(int fromIndex, int toIndex, boolean value) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.set(fromIndex, toIndex, value);
    // }
    // /* (non-Javadoc)
    // * @see java.util.BitSet#xor(java.util.BitSet)
    // */
    // @Override
    // public void xor(BitSet set) {
    // if (frozen) {
    // throw new UnsupportedOperationException();
    // }
    // super.xor(set);
    // }
    // /* (non-Javadoc)
    // * @see com.ibm.icu.util.Freezable#isFrozen()
    // */
    // public boolean isFrozen() {
    // return frozen;
    // }
    // /* (non-Javadoc)
    // * @see com.ibm.icu.util.Freezable#freeze()
    // */
    // public FreezableBitSet freeze() {
    // frozen = true;
    // return this;
    // }
    // /* (non-Javadoc)
    // * @see com.ibm.icu.util.Freezable#cloneAsThawed()
    // */
    // public FreezableBitSet cloneAsThawed() {
    // FreezableBitSet result = new FreezableBitSet(size());
    // result.or(this);
    // return result;
    // }
    // }
}
