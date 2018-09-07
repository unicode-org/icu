// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl;

import com.ibm.icu.text.UnicodeSet;

/**
 * Properties functionality above class UCharacterProperty
 * but below class CharacterProperties and class UnicodeSet.
 */
public final class CharacterPropertiesImpl {
    /**
     * A set of all characters _except_ the second through last characters of
     * certain ranges. These ranges are ranges of characters whose
     * properties are all exactly alike, e.g. CJK Ideographs from
     * U+4E00 to U+9FA5.
     */
    private static final UnicodeSet inclusions[] = new UnicodeSet[UCharacterProperty.SRC_COUNT];

    /** For {@link UnicodeSet#setDefaultXSymbolTable}. */
    public static synchronized void clear() {
        for (int i = 0; i < inclusions.length; ++i) {
            inclusions[i] = null;
        }
    }

    private static synchronized UnicodeSet getInclusionsForSource(int src) {
        if (inclusions[src] == null) {
            UnicodeSet incl = new UnicodeSet();
            switch(src) {
            case UCharacterProperty.SRC_CHAR:
                UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_PROPSVEC:
                UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_CHAR_AND_PROPSVEC:
                UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_CASE_AND_NORM:
                Norm2AllModes.getNFCInstance().impl.addPropertyStarts(incl);
                UCaseProps.INSTANCE.addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_NFC:
                Norm2AllModes.getNFCInstance().impl.addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_NFKC:
                Norm2AllModes.getNFKCInstance().impl.addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_NFKC_CF:
                Norm2AllModes.getNFKC_CFInstance().impl.addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_NFC_CANON_ITER:
                Norm2AllModes.getNFCInstance().impl.addCanonIterPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_CASE:
                UCaseProps.INSTANCE.addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_BIDI:
                UBiDiProps.INSTANCE.addPropertyStarts(incl);
                break;
            case UCharacterProperty.SRC_INPC:
            case UCharacterProperty.SRC_INSC:
            case UCharacterProperty.SRC_VO:
                UCharacterProperty.INSTANCE.ulayout_addPropertyStarts(src, incl);
                break;
            default:
                throw new IllegalStateException("getInclusions(unknown src " + src + ")");
            }
            // We do not freeze() the set because we only iterate over it,
            // rather than testing contains(),
            // so the extra time and memory to optimize that are not necessary.
            inclusions[src] = incl;
        }
        return inclusions[src];
    }

    /**
     * Returns a mutable UnicodeSet -- do not modify!
     */
    public static UnicodeSet getInclusionsForProperty(int prop) {
        int src = UCharacterProperty.INSTANCE.getSource(prop);
        return getInclusionsForSource(src);
    }
}
