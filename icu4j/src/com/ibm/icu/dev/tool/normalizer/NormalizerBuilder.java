/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/normalizer/Attic/NormalizerBuilder.java,v $ 
 * $Date: 2001/03/15 23:36:02 $ 
 * $Revision: 1.12 $
 *
 *****************************************************************************************
 */

package com.ibm.tools.normalizer;

import java.io.*;
import java.util.*;
import com.ibm.text.*;
import com.ibm.util.CompactByteArray;
import com.ibm.util.CompactCharArray;
import com.ibm.util.Utility;
import com.ibm.icu.internal.UInfo;

public final class NormalizerBuilder
{
    public static void main(String args[]) throws IOException {
        try {
            NormalizerBuilder foo = new NormalizerBuilder(args);
        } catch (Throwable e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
            System.in.read();
        }
    }

    private UInfo uinfo;

    /**
     * Map char->String.  Each entry maps a character with a
     * decomposition (either canonical or compatibility) to that
     * decomposition.  The decomposition is in canonical order.
     */
    private DecompMap decomps = new DecompMap();

    /**
     * Map of characters whose full canonical decomposition is
     * DIFFERENT from their full compatibility decomposition.
     */
    private DecompMap explodeCompat = new DecompMap();

    /**
     * Map of characters with a decomposition that are neither
     * in explodeCompat nor in permutedCompositions.
     */
    private DecompMap explodeOnly   = new DecompMap();

    /**
     * Map of String->char of permutations that compose to a
     * character.  This does not include singletons or other
     * composition exclusions.  It is an inverse list, with valid
     * permutations, for canonical decomposition.
     */
    private CompMap permutedCompositions = new CompMap();
    private CompMap binaryCompositions = new CompMap();

    /**
     * A set of characters that form the base of a combining
     * sequence.
     */
    private CharSet bases = new CharSet();

    /**
     * A set of characters that form the combining character of
     * a combining sequence.
     */
    private CharSet combining = new CharSet();

    private Map pairExplosions = new HashMap();

    private boolean fVerbose = false;
    private boolean fWriteData = false;
    private boolean fShowSizes = false;
    private boolean fPrompt = false;
    private boolean fJava = true;
    private boolean fCPP = false;
    private String fOutDir = null; // output directory for either Java or C++

    /**
     * The highest Unicode character that has a canonical
     * decomposition.  (i.e. largest char that can result from a
     * primary canonical composition.)  This is the largest char in
     * permutedCompositions.
     */
    char largestChar = 0;

    public NormalizerBuilder(String[] args) throws IOException
    {
        // Parse my command line
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-data")) {
                uinfo = new UInfo(args[++i], args[++i]);
            }
            else if (args[i].equals("-write")) {
                fWriteData = true;
            }
            else if (args[i].equals("-verbose") || args[i]. equals("-v")) {
                fVerbose = true;
            }
            else if (args[i].equals("-size")) {
                fShowSizes = true;
            }
            else if (args[i].equals("-prompt")) {
                fPrompt = true;
            }
            else if (args[i].equals("-java")) {
                fJava = true;
                fCPP = false;
            }
            else if (args[i].equals("-cpp")) {
                fCPP = true;
                fJava = false;
            }
            else if (args[i].equals("-outdir")) {
                fOutDir = args[++i];
            }
        }
        if (uinfo == null) {
            uinfo = new UInfo();
        }
        if (fOutDir == null) {
            fOutDir = fJava ? "src/com/ibm/text/"
                            : "./";
        }
        if (!fOutDir.endsWith("/")) { fOutDir += '/'; }

        boolean canonicalOnly = true;

        // Build decomps, a char->String mapping of characters to their
        // decompositions, either canonical or compatibility.
        createDecompositions();

        outv("\nGenerating permuted compositions...");

        // Form the list of all the permuted sequences that are
        // canonically equivalent to the canonical decompositions.  As
        // a by-product, find out which are not combining character
        // sequences.

        for (char ch = 0; ch < 0xFFFF; ch++) {
            String decomp = decomps.get(ch);

            if (decomp != null) {
                boolean done = false;

                if (!uinfo.getFullDecomposition(ch,true).equals(
                            uinfo.getFullDecomposition(ch,false)))
                {
                    explodeCompat.put(ch, uinfo.getFullDecomposition(ch, false));
                    done = true;
                }
                // It's always a combining base sequence, so removed last check - liu
                if (uinfo.hasCanonicalDecomposition(ch) && decomp.length() > 1
                    && !uinfo.isExcludedComposition(ch) /*&& uinfo.isCBS(decomp)*/)
                {
                    if (decomp.length() <= 2) {
                        permutedCompositions.put(decomp, ch);
                    }
                    else {
                        /* Create a comprehensive list of
                         * permutations.  Assume the first char is a
                         * base char, so don't permute it into the
                         * middle of the string -- just concatenate it
                         * onto the front.  However, there may be
                         * embedded base characters, so we do a
                         * further check for canonical decomposition
                         * equivalence below. */
                        List alternatives = concat(decomp.charAt(0),
                            jumble(decomp.substring(1, decomp.length())));

                        for (int i = 0; i < alternatives.size(); ++i)
                        {
                            String variant = (String)alternatives.get(i);
                            String normalized = uinfo.fixCanonical(variant);

                            if (normalized.equals(decomp)) {
                                permutedCompositions.put(variant, ch);
                            }
                        }
                    }
                    largestChar = ch;
                    done = true;
                }
                if (!done) {
                    explodeOnly.put(ch, decomp);    // Disparaged
                }
            }
        }

        outv("\nLargest composed char: " + Utility.hex(largestChar));

        // Form the binary compositions
        outv("\nGenerating pairwise compositions...");

        Iterator list = permutedCompositions.keySet().iterator();
        while (list.hasNext()) {
            String decomp = (String)list.next();
            char ch = permutedCompositions.get(decomp);

            if (decomp.length() > 2) {
                //
                // If this is a composition of more than two characters,
                // see if its initial portion is also a composition.  If so, that lets
                // us build up this composed character iteratively.
                //
                for (int i = decomp.length()-1; i > 1; --i) {
                    String partial = decomp.substring(0,i);
                    char partialMap = permutedCompositions.get(partial);
                    if (partialMap != 0) {
                        decomp = partialMap + decomp.substring(i);
                        break;
                    }
                }
            }
            if (decomp.length() <= 2) {
                binaryCompositions.put(decomp, ch);
            } else {
                //
                // The composition takes more than two characters, and there's
                // no way to build it up from smaller ones.
                //
                if (decomp.equals(uinfo.fixCanonical(decomp)))
                {
                    // If the decomp is in canonical order, we're in trouble,
                    // since that means there's no way to generate this composed
                    // character from its canonically decomposed equivalent.
                    err("No pairwise compose of " + Utility.hex(decomp) +
                            " > " + Utility.hex(ch) + " " + uinfo.getName(ch,true) );
                }
                else {
                    // If the decomp is *not* in canonical order, it's not as
                    // bad, since composition will still work as long as
                    warn("No pairwise compose of non-canon " + Utility.hex(decomp) +
                            " > " + Utility.hex(ch) + " " + uinfo.getName(ch,true) );
                }
            }

            bases.add(decomp.charAt(0));

            // add to list of all combining characters in composites
            for (int q = 1; q < decomp.length(); ++q) {
                combining.add(decomp.charAt(q));
            }
        }


        // Generate the pairwise explosions, where a composed char + combining char
        // transforms into a different pair of characters, usually because the
        // canonical combining classes are reversed.

        outv("\nGenerating exploding pairs....");

        List binaryValues = new ArrayList(binaryCompositions.values());
        Collections.sort(binaryValues);

        for (char addOn = 0; addOn < 0xFFFF; addOn++) {
            if (combining.contains(addOn))
            {
                list = binaryValues.iterator();

                while (list.hasNext()) {
                    MutableChar unichar = (MutableChar)list.next();
                    String chStr = String.valueOf(unichar.value);
                    String source = chStr + addOn;

                    String comp = binaryComposition(source);

                    if (comp.length() == 1) continue; // don't care if combines
                    if (comp.charAt(0) == addOn || comp.charAt(1) == addOn) continue; // rearranges

                    if (!source.equals(comp)) {
                        String decomp = fullDecomposition(source);
                        pairExplosions.put(source,comp);
                        bases.add(unichar);
                    }
                }
            }
        }

        buildDecompData();
        buildComposeData();
        outv("Success!");

        if (fPrompt) {
            System.out.println("\nHit any key to continue...");
            System.in.read();
        }
    }

    public String fullDecomposition(String s) {
        return fullDecomposition(s, new StringBuffer()).toString();
    }

    public StringBuffer fullDecomposition(char ch, StringBuffer output) {
        String value = decomps.get(ch);
        if (value == null) {
            bubbleAppend(output, ch);
        }
        else {
            bubbleAppend(output, value);
        }
        return output;
    }

    public StringBuffer fullDecomposition(String s, StringBuffer output) {
        for (int i = 0; i < s.length(); ++i) {
            fullDecomposition(s.charAt(i),output);
        }
        return output;
    }

    public String binaryComposition(String sr) {
        // set up decomposed string, init variables
        StringBuffer output = new StringBuffer();
        StringBuffer decomp = new StringBuffer();

        if (sr.length() == 0) return output.toString();

        // First generate the full decomposition of the input string
        fullDecomposition(sr, decomp);
        int basePosition = 0;
        char base = decomp.charAt(0);
        output.append(base);

        // handle degenerate case--no base character at start
        if (uinfo.getCanonicalClass(base) != 0) {
            // later
        }

        // loop through, composing items with base
        for (int i = 1; i < decomp.length(); ++i) {
            char ch = decomp.charAt(i);
            short can = uinfo.getCanonicalClass(ch);

            char value = binaryCompositions.get(String.valueOf(base) + ch);

            if (value != 0 && noObstructions(output, basePosition, can)) {
                base = value;
                output.setCharAt(basePosition, base);
            } else if (can == 0) {
                basePosition = output.length();
                base = ch;
                output.append(ch);
            } else {
                bubbleAppend(output, ch, can);
            }
        }
        return output.toString();
    }

    public boolean noObstructions(StringBuffer buffer, int pos, short can) {
        for (int j = buffer.length()-1; j > pos; --j) {
            if (can == uinfo.getCanonicalClass(buffer.charAt(j))) {
                return false;
            }
        }
        return true;
    }

    public void bubbleAppend(StringBuffer buffer, char ch, short can) {
        for (int j = buffer.length()-1; j >= 0; --j) {
            if (can >= uinfo.getCanonicalClass(buffer.charAt(j))) {
                buffer.insert(j + 1, ch);
                return;
            }
        }
        buffer.insert(0, ch);
    }

    public void bubbleAppend(StringBuffer buffer, char ch) {
        bubbleAppend(buffer, ch, uinfo.getCanonicalClass(ch));
    }

    public void bubbleAppend(StringBuffer buffer, String s) {
        for (int i = 0; i < s.length(); ++i) {
            bubbleAppend(buffer, s.charAt(i));
        }
    }

    String getDecomposition(char ch) {
        return decomps.get(ch);
    }


    /**
     * Generate a Map of all decompositions in Unicode.  The keys in
     * the map are MutableChar objects, one for each character that
     * has a decomposition.  The values are String objects containing
     * the full decomposition for the character, in canonical order.
     */
    private void createDecompositions()
    {
        outv("\nGenerating Full decompositions...");
        StringBuffer temp = new StringBuffer();

        short compatCount=0, canonCount=0;

        for (char ch = 0; ch < 0xFFFF; ++ch) {
            if (ch >= '\u4E00' && ch <= '\uD7A3') continue; // skip ideos

            short category = uinfo.getCategory(ch);

            if (category == uinfo.UNASSIGNED) continue; //skip reserved
            if (category == uinfo.CONTROL) continue;
            if (category == uinfo.FORMAT) continue;
            if (category == uinfo.PRIVATE_USE) continue;
            if (category == uinfo.SURROGATE) continue;

            boolean canon = uinfo.hasCanonicalDecomposition(ch);
            boolean compat = uinfo.hasCompatibilityDecomposition(ch);

            if (canon) canonCount++;
            if (compat) compatCount++;

            if (canon || compat) {
                String decomp = uinfo.getFullDecomposition(ch, canon);
                temp.setLength(0);
                temp.append(decomp);
                uinfo.fixCanonical(temp); // put into canonical order

                decomps.put(ch, temp.toString() );
            }
        }
    }

    /**
     * Modify a list in place by prepending the given character to all
     * of its elements, which are assumed to be strings.
     */
    static List concat(char ch, List a) {
        for (int i = 0; i < a.size(); ++i) {
            a.set(i, ch + (String)a.get(i));
        }
        return a;
    }

    /**
     * Return a list of Strings for all possible permutations of the
     * characters in the input string.
     */
    static List jumble (String source)
    {
        ArrayList result = new ArrayList();
        if (source.length() == 1) {
            result.add(source);
        } else for (int i = 0; i < source.length(); ++i) {
            result.addAll( concat( source.charAt(i),
                                   jumble(source.substring(0,i)
                                          + source.substring(i+1,source.length()))));
        }
        return result;
    }

    static final int STR_INDEX_SHIFT = 2;
    static final int STR_LENGTH_MASK = 0x0003;

    static final int DECOMP_RECURSE = 0x00008000;
    static final int DECOMP_MASK  = 0x00007FFF;

    /**
     * Generate a new "DecompData.java" that contains the CompactArray definitions
     * used in the {@link Normalizer.DECOMPOSE} operation.
     */
    void buildDecompData() throws IOException {

        outv("\nGenerating DecompData.java....");
        //
        // For each Unicode character that has a decomposition, we put its
        // fully-decomposed form at the end of the "contents" string, followed
        // by a null, and we put its index in "contents" into the CompactArray.
        // If it does not have a decomposition, we store a bogus index.
        //
        // We do this first for all of the compatibility decompositions, save
        // the index in MAX_COMPAT, and then do it again for the canonical
        // decompositions.  When the array is used later, any character whose
        // decomp has an index greater than MAX_COMPAT is a canonical decomp.
        //
        int canonIndex = 0;
        int compatIndex = 0;

        // Map from Unicode character to replacement string index
        CompactCharArray offsets = new CompactCharArray((char)0);

        // We also need a place to store the replacement strings.  Add a char at
        // the front so that "0" won't be the index of any of the replacement strings.
        StringBuffer replace = new StringBuffer().append("\uffff");

        for (char ch = 0; ch < 0xFFFF; ch++) {
            if (uinfo.hasCompatibilityDecomposition(ch)) {
                compatIndex = putLength(replace, decomps.get(ch), 0);
                offsets.setElementAt(ch, (char)compatIndex);
            }
        }

        // Add the canonical decomps.  Their indices must be > compatIndex.
        for (char ch = 0; ch < 0xFFFF; ch++) {
            if (uinfo.hasCanonicalDecomposition(ch)) {

                if (ch == 0x0f77) {
                    outv("0F77: decomps.get() = " + Utility.hex(decomps.get(ch)));
                    outv("0F77: fullDecomp = " + Utility.hex(uinfo.getFullDecomposition(ch,false)));
                }

                canonIndex = putLength(replace, decomps.get(ch), compatIndex);

                // If this character's full compatibility decomposition is different from
                // its canonical decomp, that means one of the characters in its
                // canonical decomp itself has a compatibility decomp.  To deal with this,
                // we set a bit flag telling the decomposer to recurse on this character.

                if (!uinfo.getFullDecomposition(ch,true).equals(uinfo.getFullDecomposition(ch,false))) {
                    offsets.setElementAt(ch, (char)(canonIndex | DECOMP_RECURSE));
                } else {
                    offsets.setElementAt(ch, (char)canonIndex);
                }
            }
        }

        //
        // Now generate another CompactArray containing the combining class of every
        // character in Unicode
        //
        final byte BASE = 0;
        CompactByteArray canonClasses = new CompactByteArray(BASE);

        for (char ch = 0; ch < 0xFFFF; ch++) {
            short canonClass = uinfo.getCanonicalClass(ch);
            if (canonClass != 0) {
                canonClasses.setElementAt(ch, (byte)canonClass);
            }
        }

        // Finally, write the data out to a compilable Java source file

        if (fJava) {
            String f = fOutDir + "DecompData";
            out("Writing " + f);
            writeDecompData(new JavaWriter(f),
                        canonIndex, compatIndex, BASE, offsets, replace, canonClasses);
            
        }

        if (fCPP) {
            String f = fOutDir + "dcmpdata";
            out("Writing " + f + ".(cpp|h)");
            writeDecompData(new CPPWriter(f, "DecompData"),
                        canonIndex, compatIndex, BASE, offsets, replace, canonClasses);
        }

        outv("Decomp data: MAX_CANONICAL = " + canonIndex + ", MAX_DECOMP = " + compatIndex);

        if (fShowSizes) {
            int offsetSize = offsets.getIndexArray().length * 2 + offsets.getValueArray().length * 2;
            int canonSize = canonClasses.getIndexArray().length * 2 + canonClasses.getValueArray().length;
            int replaceLength = replace.length();

            outv("Total runtime size of decomp data is "
                + (offsetSize + canonSize + replaceLength));

            outv("  offsets:      " + offsetSize);
            outv("  canonClasses: " + canonSize);
            outv("  replace:      " + replaceLength);
        }
    }

    void writeDecompData(SourceWriter out, int maxCanon, int maxCompat, short BASE,
                        CompactCharArray offsets, StringBuffer contents,
                        CompactByteArray canonClasses)
    {
        out.write("MAX_CANONICAL",  maxCanon        );
        out.write("MAX_COMPAT",     maxCompat       );
        out.write("DECOMP_MASK",    DECOMP_MASK     );
        out.write("DECOMP_RECURSE", DECOMP_RECURSE  );
        out.write("BASE",           BASE            );
        out.write("offsets",        offsets         );
        out.write("contents",       contents        );
        out.write("canonClass",     canonClasses    );
        out.close();
    }


    //==========================================================================================
    // Methods for generating and writing the composition data
    //
    final int TYPE_MASK   = 0x0007;
    final int INDEX_MASK  = 0xFFF8;
    final int INDEX_SHIFT = 3;

    // MAX_BASES is used to map a 2-diminsional (base,combining) index pair onto a
    // one-dimensional CompactArray.  We could just use baseCount, but making it a power
    // of two allows slightly better compaction.

    final int MAX_BASES   = 1024;   // Product must be <= 64K
    final int MAX_COMBINE = 65536/MAX_BASES;

    final char                // for character types
        IGNORE = 0,
        BASE = 1,
        EXPLODING_BASE = 2,
        COMBINING = 3,
        INITIAL_JAMO = 4,
        MEDIAL_JAMO = 5,
        FINAL_JAMO = 6,
        HANGUL = 7;

    // These variables actually hold the composition data.
    short baseCount = 1;        // Leave 0 as an invalid index
    short combineCount = 1;     // Leave 0 as an invalid index
    short nccCount = 0;
    int   maxCompat = 0;
    int   maxCanon = 0;

    // This array contains types (from the set above) and indices into the "replace"
    // and "actions" arrays
    CompactCharArray lookup = new CompactCharArray(IGNORE);

    // We also need a place to store the strings that result from replacements,
    // explosions, and combinations.  Add a char at the front so that "0" won't
    // be the index of any of the replacement strings.
    StringBuffer replace = new StringBuffer().append(" ");

    // We need to represent each canonical character class as a single bit
    // so that we can OR together a mask of all combining char classes seen
    // Build an array that maps from combining class to a compacted integer
    // from 0..n-1, where n is the number of distinct combining classes.
    // E.g., in 3.0, there are 53 distinct combining classes.
    int[] classMap = new int[256];
    int[] typeBit;

    // Build a two-dimensional array of the action to take for each base/combining pair
    CompactCharArray actions = new CompactCharArray((char)0);

    char[] actionIndex;

    /**
     * Generate a new "ComposeData.java" that contains the CompactArray definitions
     * used in the {@link Normalizer.COMPOSE} operation.
     */
    void buildComposeData() throws IOException
    {
       outv("\nGenerating ComposeData.java....");

        BitSet usedIndices = new BitSet();
        CharSet explodingBases = new CharSet();
        NonComposingCombiningMap nccMap = new NonComposingCombiningMap();

        // Find all characters that are both bases *and* have compatibility
        // decompositions.  These are weird
        for (char ch = 0; ch < 0xFFFF; ch++) {
            if (bases.contains(ch) && uinfo.hasCompatibilityDecomposition(ch)) {
                //
                // Add this character's explosion to the replacement string list.
                // We're going to make sure that its "base index", i.e. the
                // index for it in the actions array, is the same as the
                // explosion's index in the replace string.  This lets
                // us use the same index for the character's two behaviors
                //
                int index = put(replace, explodeCompat.get(ch), 0);

                outv(Utility.hex(ch) + " is base and has compat explosion "
                                  + Utility.hex(explodeCompat.get(ch)) );

                addChar(lookup, ch, EXPLODING_BASE, index);
                usedIndices.set(index);
                explodingBases.add(ch);
            }
        }

        // First add the base characters to the array.
        // At the same time, compute their indices.
        // Leave an empty base index of 0 as a placeholder for null operations.
        //

        for (char ch = 0; ch < 0xFFFF; ch++)
        {

            if (explodingBases.contains(ch)) {
                continue;
            }

            short cclass = uinfo.getCanonicalClass(ch);

            if (bases.contains(ch)) {
                // Make sure that we don't use a base index that was already used
                // for an exploding base character.
                while (usedIndices.get(baseCount)) {
                    baseCount++;
                }
                // Now add the character to lookup as a base
                addChar(lookup, ch, BASE, baseCount++);
            }
            if (combining.contains(ch)) {
                classMap[cclass] = 1;       // Mark this combining class as being used
                addChar(lookup, ch, COMBINING, combineCount++);
            }

            if (ch >= '\u1100' && ch < '\u1160') {
                addChar(lookup, ch, INITIAL_JAMO, 0);
            }
            if (ch >= '\u1161' && ch < '\u11a6') {
                addChar(lookup, ch, MEDIAL_JAMO, 0);
            }
            if (ch >= '\u11a7' && ch < '\u11fa') {
                addChar(lookup, ch, FINAL_JAMO, 0);
            }
            if (ch >= 0xac00 && ch <= 0xd7a4) {
                addChar(lookup, ch, HANGUL, 0);
            }

            // Add explosions for all compatibility decompositions,
            // including the Jamo --> Conjoining Jamo decomps.
            // If the canonical decomposition is exactly one character
            // one (4 hex digits) then we deal with it separately below.
            if (explodeCompat.contains(ch) &&
                uinfo.getDecomposition(ch).length() != 4)
            {
                maxCompat = put(replace, explodeCompat.get(ch), 0);
                addExplosion(lookup, ch, maxCompat);
            }
        }

        // Now add the explosions resulting from canonical decompositions
        // These will all have indices greater than "maxCompat" so we can distinguish them.
        //
        for (char ch = 0; ch < 0xFFFF; ch++) {
            short cclass = uinfo.getCanonicalClass(ch);
            String explosion = null;

            if (explodeOnly.contains(ch) && uinfo.hasCanonicalDecomposition(ch)) {
                maxCanon = put(replace, explodeOnly.get(ch), maxCompat);
                addExplosion(lookup, ch, maxCanon);
            }

//          else if (!combining.contains(ch) && cclass != 0 && classMap[cclass] != 0) {
//              //
//              // If a combining character didn't happen to end up in one of
//              // the pairwise combinations or explosions we use but still has
//              // a combining class that is the same as a character we *do* use,
//              // we need to save its class so that we don't combine things "past" it.
//              //
//              // However, if the character has an explosion we *don't* need it, because
//              // we'll never see it, only the results of its explosion.
//              //
//              addChar(lookup, ch, COMBINING, 0);
//              nccCount++;
//          }

            // I'm rewriting this logic.  Having an index of zero means that
            // the typeBit[index] gets overwritten with multiple different
            // values.  So we must use real index values that are unique
            // per combining class.  Also, it doesn't matter if the class
            // has been seen or not; we still need to record the character
            // in order to have its type and class during composition.
            else if (!combining.contains(ch) && cclass != 0) {
                // If a combining character didn't happen to end up in one of
                // the pairwise combinations or explosions we use but still has
                // a combining class that is the same as a character we *do* use,
                // we need to save its class.

                // As our index, use combineCount and up.  Reuse values by
                // mapping them through nccMap, which keeps track of previously
                // used values and allocates new ones only as needed, starting
                // with zero. - Liu
                classMap[cclass] = 1;       // Mark this combining class as being used
                addChar(lookup, ch, COMBINING, combineCount + nccMap.getIndexFor(cclass));
            }
        }

        nccCount = (short) nccMap.getIndexCount(); // Liu

        // Remap characters that have a canonical decomposition to a singleton,
        // and also different compatibility and canonical full decompositions
        // (that is, also are members of explodeCompat).  These characters can't
        // be exploded to their full decomposition since that breaks canonical
        // composition (normalization form C).  Instead, we place their
        // singleton decomposition in the table, at the end.  This works because
        // the singleton will get recursively exploded by Normalizer.  As of
        // Unicode 3.0, this fix applies to U+1FFE, 1FFD, 2000, and 2001. - Liu
        int singleton = replace.length();
        for (char ch = 0; ch < 0xFFFF; ch++) {
            if (!explodingBases.contains(ch) &&
                explodeCompat.contains(ch) &&
                uinfo.getDecomposition(ch).length() == 4) {
              
                // There might be a cleaner way to do this, perhaps by folding
                // this logic into the code above (perhaps calling
                // addExplosion() instead of addChar()), but I couldn't find it.
                char remap = (char)
                    Integer.parseInt(uinfo.getDecomposition(ch), 16);

                int index = put(replace, String.valueOf(remap), singleton);
                addChar(lookup, ch, EXPLODING_BASE, index);

                outv("Canonical singleton " + Utility.hex(ch) +
                     " remaps to " + Utility.hex(remap) + " index=" + index); 
            }
        }

        // Now run through the combining classes again and assign bit numbers
        // in the same ascending order as the canonical classes
        int maskShift = 0;
        int bit = 0;
        for (int i = 0; i < 256; i++) {
            if (classMap[i] != 0) {
                classMap[i] = ++bit;
            }
        }
        if (bit >= 64) {
            err(String.valueOf(bit+1) + " combining classes; max is 64");
        }
        outv("# of combining classes is " + (bit+1));

        outv("baseCount=" + baseCount + ", combineCount=" + combineCount
                            + ", nccCount=" + nccCount);

        if (baseCount > MAX_BASES) {
            err(Integer.toString(baseCount) + " bases, limit is " + MAX_BASES);
            err(Integer.toString(combineCount) + " combining chars, limit is " + MAX_COMBINE);
        }

        // Now build the "actions" array that tells what to do when each base /
        // combining pair is seen.
        //
        // First do character pairs that combine into a single character...
        //
        Iterator iter = binaryCompositions.keySet().iterator();
        while (iter.hasNext()) {
            String source = (String)iter.next();
            char ch = binaryCompositions.get(source);

            int baseIndex = lookup.elementAt(source.charAt(0)) >>> INDEX_SHIFT;
            int combiningIndex = lookup.elementAt(source.charAt(1)) >>> INDEX_SHIFT;

            actions.setElementAt((char)(baseIndex + MAX_BASES*combiningIndex), ch);
        }


        //
        // Pair explosions: base/combining pairs that explode into something else
        // We're squeezing the indices for these in between MAX_COMPOSED and 0xFFFF,
        // which means they can't be indexes into the "replace" string; those are too big.
        // Instead they're indexes into the "actionIndex" array, which in turn contains
        // indices in "replace"
        //
        actionIndex = new char[ pairExplosions.size() ];
        short index = 0;

        iter = pairExplosions.keySet().iterator();
        while (iter.hasNext()) {
            String source = (String)iter.next();
            char base = source.charAt(0);
            char combining = source.charAt(1);

            int strIndex = put(replace, (String)pairExplosions.get(source), 0);
            actionIndex[index] = (char)strIndex;

            int baseIndex = lookup.elementAt(base) >>> INDEX_SHIFT;
            int combiningIndex = lookup.elementAt(combining) >>> INDEX_SHIFT;

            actions.setElementAt((char)(baseIndex + MAX_BASES*combiningIndex),
                                 (char)(index + largestChar));
            index++;
        }

        // Fill in the array that maps from combining class value
        // to a bit numbe representing the canonical combining class.
        // That is, map from 0..240 (in 3.0) to 0..52.
        typeBit = new int[combineCount + nccCount];

        for (char ch = 0; ch < 0xFFFF; ch++) {
            int value = lookup.elementAt(ch);
            int type = value & TYPE_MASK;

            if (type == COMBINING) {
                int ind = value >>> INDEX_SHIFT;
                int cclass = uinfo.getCanonicalClass(ch);
                if (typeBit[ind] != 0 && typeBit[ind] != classMap[cclass]) {
                    err("Overwriting typeBit[" + ind + "], was " +
                        typeBit[ind] + ", changing to " + classMap[cclass] + " for class " + cclass);
                }
                typeBit[ind] = classMap[cclass];
            }
        }

        if (fJava) {
            String f = fOutDir + "ComposeData";
            out("Writing " + f);
            writeComposeData(new JavaWriter(f));
        }
        if (fCPP) {
            String f = fOutDir + "compdata";
            out("Writing " + f + ".(cpp|h)");
            writeComposeData(new CPPWriter(f, "ComposeData"));
        }

        if (fShowSizes) {
            int lookupSize = lookup.getIndexArray().length * 2 + lookup.getValueArray().length * 2;
            int actionSize = actions.getIndexArray().length * 2 + actions.getValueArray().length * 2;
            int actIndexSize = actionIndex.length * 2;
            int replaceSize = replace.length();
            int typeBitSize = typeBit.length * 2;

            outv("Total runtime size of compose data is "
                + (lookupSize + actionSize + actIndexSize + replaceSize + typeBitSize));

            outv("  lookup:       " + lookupSize);
            outv("  actions:      " + actionSize);
            outv("  actionIndex:  " + actIndexSize);
            outv("  typeBit:      " + typeBitSize);
            outv("  replace:      " + replaceSize);
        }
    }

    void writeComposeData(SourceWriter out) {
        out.write("BASE_COUNT",         baseCount);
        out.write("COMBINING_COUNT",    combineCount);
        out.write("MAX_COMPAT",         maxCompat);
        out.write("MAX_CANONICAL",      maxCanon);

        out.writeHex("MAX_COMPOSED",    largestChar);

        int maxIndex = replace.length();
        out.write("MAX_INDEX",          maxIndex    );
        out.write("INITIAL_JAMO_INDEX", maxIndex + 1);
        out.write("MEDIAL_JAMO_INDEX",  maxIndex + 2);

        out.write("MAX_BASES",          MAX_BASES  );
        out.write("MAX_COMBINE",        MAX_COMBINE);

        out.writeHex("TYPE_MASK",       TYPE_MASK);
        out.write("INDEX_SHIFT",        INDEX_SHIFT);

        // The character types
        out.write("IGNORE",             (int)IGNORE);
        out.write("BASE",               (int)BASE);
        out.write("NON_COMPOSING_COMBINING", (int)EXPLODING_BASE);
        out.write("COMBINING",          (int)COMBINING);
        out.write("INITIAL_JAMO",       (int)INITIAL_JAMO);
        out.write("MEDIAL_JAMO",        (int)MEDIAL_JAMO);
        out.write("FINAL_JAMO",         (int)FINAL_JAMO);
        out.write("HANGUL",             (int)HANGUL);

        out.write("lookup",         lookup        );
        out.write("actions",        actions       );
        out.write("actionIndex",    actionIndex   );
        out.write("replace",        replace       );
        out.write("typeBit",        typeBit);

        out.close();
    }

    void addChar(CompactCharArray lookup, char ch, int type, int index)
    {
        // First make sure it's not already present
        if (lookup.elementAt(ch) != IGNORE)
        {
            char oldValue = lookup.elementAt(ch);
            err(typeName(type) + " char is also "
                  + typeName(oldValue & TYPE_MASK) + ": "
                  + Utility.hex(ch) + "  " + uinfo.getName(ch,true));
        }
        else if ((index << INDEX_SHIFT) > 65536) {
            err("not enough bits: index " + index + " << INDEX_SHIFT = " + (index << INDEX_SHIFT));
        } else {
            lookup.setElementAt(ch, (char)(type | (index << INDEX_SHIFT)));
        }
    }

    void addExplosion(CompactCharArray lookup, char ch, int index)
    {
        // First make sure it doesn't already have an index
        char oldValue = lookup.elementAt(ch);
        int oldIndex = oldValue >>> INDEX_SHIFT;

        if (oldValue != IGNORE) {
            err("Exploding char is already " + typeName(oldValue & TYPE_MASK)
                             + " (index " + oldIndex + "): "
                             + Utility.hex(ch) + "  " + uinfo.getName(ch,true));
        }

        if (oldIndex != 0) {
            err("Exploding char is already " + typeName(oldValue & TYPE_MASK)
                             + " (index " + oldIndex + "): "
                             + Utility.hex(ch) + "  " + uinfo.getName(ch,true));
        }
        else if ((index << INDEX_SHIFT) > 65536) {
            err("not enough bits: index " + index + " << INDEX_SHIFT = " + (index << INDEX_SHIFT));
        } else {
            lookup.setElementAt(ch, (char)((oldValue & ~INDEX_MASK) | (index << INDEX_SHIFT)));
        }
    }

    String typeName(int type) {
        switch (type) {
            case IGNORE:            return "Ignored";
            case BASE:              return "Base";
            case EXPLODING_BASE:    return "Exploding Base";
            case COMBINING:         return "Combining";
            case INITIAL_JAMO:      return "Initial Jamo";
            case MEDIAL_JAMO:       return "Medial Jamo";
            case FINAL_JAMO:        return "Final Jamo";
            case HANGUL:            return "Hangul";
            default:                return "Unknown";
        }
    }


    static final int put(StringBuffer buf, String str, int minIndex)
    {
        str = str + '\u0000';   // Add trailing null

        int index = buf.toString().indexOf(str);
        if (index <= minIndex) {
            index = buf.length();
            buf.append(str);
        }
        return index;
    }

    static final int putLength(StringBuffer buf, String str, int minIndex) {
        int length = str.length();

        if (length >= (1 << STR_INDEX_SHIFT)) {
            // There's no room to store the length in the index, so
            // add a null terminator and use a 0 length to flag this
            str = str + '\u0000';
            length = 0;
        }

        int index = buf.toString().indexOf(str);
        if (index <= minIndex) {
            index = buf.length();
            buf.append(str);
        }
        return (index << STR_INDEX_SHIFT) | length;
    }

    //--------------------------------------------------------------------------------
    // Output & formatting

    void out(String str) {
        System.out.println(str);
    }
    void outv(String str) {
        if (fVerbose) System.out.println(str);
    }
    void warn(String str) {
        System.err.println("Warning: " + str);
    }
    void err(String str) {
        System.err.println("ERROR:   " + str);
    }
}

//-----------------------------------------------------------------------------
// Utility classes
//-----------------------------------------------------------------------------

class DecompMap extends HashMap {
    public DecompMap() {
    }

    void put(char ch, String value) {
        put(new MutableChar(ch), value);
    }

    String get(char ch) {
        Object obj = get(probe.set(ch));
        return (obj != null) ? (String)obj : null;
    }

    boolean contains(char ch) {
        return containsKey(probe.set(ch));
    }

    MutableChar probe = new MutableChar(' ');
}

class CompMap extends HashMap {
    public CompMap() {
    }

    void put(String key, char value) {
        put(key, new MutableChar(value));
    }

    char get(String key) {
        Object obj = get((Object)key);
        return (obj != null) ? ((MutableChar)obj).value : 0;
    }
}

class CharSet extends HashSet {
    public CharSet() {
    }

    public void add(char ch) {
        add(new MutableChar(ch));
    }

    public boolean contains(char ch) {
        return contains(probe.set(ch));
    }
    MutableChar probe = new MutableChar(' ');
}

/**
 * An int->int map.  Each time a non-existent key is looked up,
 * create a new mapping to the next available integer value.
 */
class NonComposingCombiningMap {
    int index;
    Hashtable hash;

    public NonComposingCombiningMap() {
        index = 0;
        hash = new Hashtable();
    }

    /**
     * Return the existing mapping of class.  If no such mapping
     * exists, create one and return it.  New mappings map to
     * zero, then one, etc.
     */
    public int getIndexFor(int cclass) {
        Integer cl = new Integer(cclass);
        Integer ind = (Integer) hash.get(cl);
        if (ind != null) {
            return ind.intValue();
        }
        hash.put(cl, new Integer(index));
        return index++;
    }

    /**
     * Return the number of mappings made so far.  That is, getIndexFor()
     * has returned integers 0..getIndexCount()-1.
     */
    public int getIndexCount() {
        return index;
    }
}
