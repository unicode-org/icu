package com.ibm.text.UCD;

import java.util.*;
import com.ibm.text.*;
import com.ibm.icu.text.UTF16;

import com.ibm.text.utility.*;


/**
 * Implements Unicode Normalization Forms C, D, KC, KD.<br>
 * See UTR#15 for details.<br>
 * Copyright © 1998-1999 Unicode, Inc. All Rights Reserved.<br>
 * The Unicode Consortium makes no expressed or implied warranty of any
 * kind, and assumes no liability for errors or omissions.
 * No liability is assumed for incidental and consequential damages
 * in connection with or arising out of the use of the information here.
 * @author Mark Davis
 */

public class NormalizerSample implements UCD_Types {
    static final String copyright = "Copyright (C) 2001, IBM Corp. and Unicode Inc. All Rights Reserved.";

    public static boolean SHOW_PROGRESS = false;

    /**
     * Create a normalizer for a given form.
     */
    public NormalizerSample(byte form, String unicodeVersion) {
        this.composition = (form & COMPOSITION_MASK) != 0;
        this.compatibility = (form & COMPATIBILITY_MASK) != 0;
        this.data = getData(unicodeVersion);
    }

    /**
     * Create a normalizer for a given form.
     */
    public NormalizerSample(byte form) {
        this(form,"");
    }

    /**
    * Masks for the form selector
    */
    public static final byte
        COMPATIBILITY_MASK = 1,
        COMPOSITION_MASK = 2;

    /**
    * Normalization Form Selector
    */
    public static final byte
        NFD = 0 ,
        NFKD = COMPATIBILITY_MASK,
        NFC = COMPOSITION_MASK,
        NFKC = (byte)(COMPATIBILITY_MASK + COMPOSITION_MASK);

    /**
    * Normalizes text according to the chosen form,
    * replacing contents of the target buffer.
    * @param   source      the original text, unnormalized
    * @param   target      the resulting normalized text
    */
    public StringBuffer normalize(String source, StringBuffer target) {

        // First decompose the source into target,
        // then compose if the form requires.

        if (source.length() != 0) {
            internalDecompose(source, target);
            if (composition) {
                internalCompose(target);
            }
        }
        return target;
    }

    /**
    * Normalizes text according to the chosen form
    * @param   source      the original text, unnormalized
    * @return  target      the resulting normalized text
    */
    public String normalize(String source) {
        return normalize(source, new StringBuffer()).toString();
    }

    /**
    * Normalizes text according to the chosen form
    * @param   source      the original text, unnormalized
    * @return  target      the resulting normalized text
    */
    public String normalize(int cp) {
        return normalize(UTF16.valueOf(cp));
    }

    /**
    */
    private StringBuffer hasDecompositionBuffer = new StringBuffer();

    public boolean hasDecomposition(int cp) {
        hasDecompositionBuffer.setLength(0);
        normalize(UTF16.valueOf(cp), hasDecompositionBuffer);
        if (hasDecompositionBuffer.length() != 1) return true;
        return cp != hasDecompositionBuffer.charAt(0);
    }

    /**
    * Utility: Checks whether there is a recursive decomposition of a character from the
    * Unicode Character Database. It is compatibility or canonical according to the particular
    * normalizer.
    * @param   ch      the source character
    */
    public boolean normalizationDiffers(int ch) {
        return data.normalizationDiffers(ch, composition, compatibility);
    }

    /**
    * Utility: Gets recursive decomposition of a character from the
    * Unicode Character Database.
    * @param   compatibility    If false selects the recursive
    *                  canonical decomposition, otherwise selects
    *                  the recursive compatibility AND canonical decomposition.
    * @param   ch      the source character
    * @param   buffer  buffer to be filled with the decomposition
    */
    public void getRecursiveDecomposition(char ch, StringBuffer buffer) {
        data.getRecursiveDecomposition(ch, buffer, compatibility);
    }


    // ======================================
    //                  PRIVATES
    // ======================================

    /**
     * The current form.
     */
    private boolean composition;
    private boolean compatibility;

    /**
    * Decomposes text, either canonical or compatibility,
    * replacing contents of the target buffer.
    * @param   form        the normalization form. If COMPATIBILITY_MASK
    *                      bit is on in this byte, then selects the recursive
    *                      compatibility decomposition, otherwise selects
    *                      the recursive canonical decomposition.
    * @param   source      the original text, unnormalized
    * @param   target      the resulting normalized text
    */
    private void internalDecompose(String source, StringBuffer target) {
        StringBuffer buffer = new StringBuffer();
        int ch32;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(ch32)) {
            buffer.setLength(0);
            ch32 = UTF16.charAt(source, i);
            data.getRecursiveDecomposition(ch32, buffer, compatibility);

            // add all of the characters in the decomposition.
            // (may be just the original character, if there was
            // no decomposition mapping)

            int ch;
            for (int j = 0; j < buffer.length(); j += UTF16.getCharCount(ch)) {
                ch = UTF16.charAt(buffer, j);
                int chClass = data.getCanonicalClass(ch);
                int k = target.length(); // insertion point
                if (chClass != 0) {

                    // bubble-sort combining marks as necessary

                    int ch2;
                    for (; k > 0; k -= UTF16.getCharCount(ch2)) {
                        ch2 = UTF16.charAt(target, k-1);
                        if (data.getCanonicalClass(ch2) <= chClass) break;
                    }
                }
                target.insert(k, UTF16.valueOf(ch));
            }
        }
    }

    /**
    * Composes text in place. Target must already
    * have been decomposed.
    * Uses UTF16, which is a utility class for supplementary character support in Java.
    * @param   target      input: decomposed text.
    *                      output: the resulting normalized text.
    */
    private void internalCompose(StringBuffer target) {
        int starterPos = 0;
        int starterCh = UTF16.charAt(target,0);
        int compPos = UTF16.getCharCount(starterCh); // length of last composition
        int lastClass = data.getCanonicalClass(starterCh);
        if (lastClass != 0) lastClass = 256; // fix for strings staring with a combining mark
        int oldLen = target.length();

        // Loop on the decomposed characters, combining where possible

        int ch;
        for (int decompPos = compPos; decompPos < target.length(); decompPos += UTF16.getCharCount(ch)) {
            ch = UTF16.charAt(target, decompPos);
            if (SHOW_PROGRESS) System.out.println(Utility.hex(target)
                + ", decompPos: " + decompPos
                + ", compPos: " + compPos
                + ", ch: " + Utility.hex(ch)
                );
            int chClass = data.getCanonicalClass(ch);
            int composite = data.getPairwiseComposition(starterCh, ch);
            if (composite != data.NOT_COMPOSITE
            && (lastClass < chClass || lastClass == 0)) {
                UTF16.setCharAt(target, starterPos, composite);
                // we know that we will only be replacing non-supplementaries by non-supplementaries
                // so we don't have to adjust the decompPos
                starterCh = composite;
            } else {
                if (chClass == 0) {
                    starterPos = compPos;
                    starterCh  = ch;
                }
                lastClass = chClass;
                UTF16.setCharAt(target, compPos, ch);
                if (target.length() != oldLen) { // MAY HAVE TO ADJUST!
                    System.out.println("ADJUSTING: " + Utility.hex(target));
                    decompPos += target.length() - oldLen;
                    oldLen = target.length();
                }
                compPos += UTF16.getCharCount(ch);
            }
        }
        target.setLength(compPos);
    }
    
    // The following class makes use of the UCD class, which accesses data in the Unicode Character Database

    static class Stub {
        private UCD ucd;
        private HashMap compTable = new HashMap();
        private BitSet isSecond = new BitSet();
        private BitSet canonicalRecompose = new BitSet();
        private BitSet compatibilityRecompose = new BitSet();
        static final int NOT_COMPOSITE = 0xFFFF;

        Stub(String version) {
            ucd = UCD.make(version);
            for (int i = 0; i < 0x10FFFF; ++i) {
                if (!ucd.isAssigned(i)) continue;
                if (ucd.isPUA(i)) continue;
                if (ucd.isNonLeadJamo(i)) isSecond.set(i);
                byte dt = ucd.getDecompositionType(i);
                if (dt != CANONICAL) continue;
                if (!ucd.getBinaryProperty(i, CompositionExclusion)) {
                    try {
                        String s = ucd.getDecompositionMapping(i);
                        int len = UTF16.countCodePoint(s);
                        if (len != 2) {
                            if (len > 2) throw new IllegalArgumentException("BAD LENGTH: " + len + ucd.toString(i));
                            continue;
                        }
                        int a = UTF16.charAt(s, 0);
                        if (ucd.getCombiningClass(a) != 0) continue;

                        int b = UTF16.charAt(s, UTF16.getCharCount(a));
                        isSecond.set(b);

                        // have a recomposition, so set the bit
                        canonicalRecompose.set(i);

                        // set the compatibility recomposition bit
                        // ONLY if the component characters
                        // don't compatibility decompose
                        if (ucd.getDecompositionType(a) <= CANONICAL
                         && ucd.getDecompositionType(b) <= CANONICAL) {
                            compatibilityRecompose.set(i);
                         }

                        long key = (((long)a)<<32) | b;

                        compTable.put(new Long(key), new Integer(i));
                    } catch (Exception e) {
                        throw new ChainException("Error: {0}", new Object[]{ucd.toString(i)}, e);
                    }
                }
            }
        }

        short getCanonicalClass(int cp) {
            return ucd.getCombiningClass(cp);
        }

        boolean isTrailing(int cp) {
            return isSecond.get(cp);
        }

        boolean normalizationDiffers(int cp, boolean composition, boolean compatibility) {
            byte dt = ucd.getDecompositionType(cp);
            if (!composition) {
                if (compatibility) return dt >= CANONICAL;
                else return dt == CANONICAL;
            } else {
                // almost the same, except that we add back in the characters
                // that RECOMPOSE
                if (compatibility) return dt >= CANONICAL && !compatibilityRecompose.get(cp);
                else return dt == CANONICAL && !canonicalRecompose.get(cp);
            }
        }

        public void getRecursiveDecomposition(int cp, StringBuffer buffer, boolean compatibility) {
            byte dt = ucd.getDecompositionType(cp);
            // we know we decompose all CANONICAL, plus > CANONICAL if compatibility is TRUE.
            if (dt == CANONICAL || dt > CANONICAL && compatibility) {
                String s = ucd.getDecompositionMapping(cp);
                for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                    cp = UTF16.charAt(s, i);
                    getRecursiveDecomposition(cp, buffer, compatibility);
                }
            } else {
                UTF16.append(buffer, cp);
            }
        }

        int getPairwiseComposition(int starterCh, int ch) {
            int hangulPoss = UCD.composeHangul(starterCh, ch);
            if (hangulPoss != 0xFFFF) return hangulPoss;
            Object obj = compTable.get(new Long((((long)starterCh)<<32) | ch));
            if (obj == null) return 0xFFFF;
            return ((Integer)obj).intValue();
        }

    }

    /**
    * Contains normalization data from the Unicode Character Database.
    * use false for the minimal set, true for the real set.
    */
    private Stub data;

    private static HashMap versionCache = new HashMap();

    private static Stub getData (String version) {
        if (version.length() == 0) version = UCD.latestVersion;
        Stub result = (Stub)versionCache.get(version);
        if (result == null) {
            result = new Stub(version);
            versionCache.put(version, result);
        }
        return result;
    }
}