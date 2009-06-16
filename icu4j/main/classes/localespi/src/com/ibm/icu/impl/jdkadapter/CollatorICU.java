/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.text.CollationKey;

import com.ibm.icu.text.Collator;

/**
 * CollatorICU is an adapter class which wraps ICU4J Collator and
 * implements java.text.Collator APIs.
 */
public class CollatorICU extends java.text.Collator {

    private Collator fIcuCollator;

    private CollatorICU(Collator icuCollator) {
        fIcuCollator = icuCollator;
    }

    public static java.text.Collator wrap(Collator icuCollator) {
        return new CollatorICU(icuCollator);
    }

    public Collator unwrap() {
        return fIcuCollator;
    }

    public Object clone() {
        CollatorICU other = (CollatorICU)super.clone();
        try {
            other.fIcuCollator = (Collator)fIcuCollator.clone();
        } catch (CloneNotSupportedException e) {
            // ICU Collator clone() may throw CloneNotSupportedException,
            // but JDK does not.  We use UnsupportedOperationException instead
            // as workwround.
            throw new UnsupportedOperationException("clone() is not supported by this ICU Collator.");
        }
        return other;
    }

    public int compare(Object o1, Object o2) {
        return fIcuCollator.compare(o1, o2);
    }

    public int compare(String source, String target) {
        return fIcuCollator.compare(source, target);
    }

    public boolean equals(Object that) {
        if (that instanceof CollatorICU) {
            return ((CollatorICU)that).fIcuCollator.equals(fIcuCollator);
        }
        return false;
    }

    public boolean equals(String source, String target) {
        return fIcuCollator.equals(source, target);
    }

    public CollationKey getCollationKey(String source) {
        com.ibm.icu.text.CollationKey icuCollKey = fIcuCollator.getCollationKey(source);
        return CollationKeyICU.wrap(icuCollKey);
    }

    public int getDecomposition() {
        int mode = java.text.Collator.NO_DECOMPOSITION;

        if (fIcuCollator.getStrength() == Collator.IDENTICAL) {
            return java.text.Collator.FULL_DECOMPOSITION;
        }
        int icuMode = fIcuCollator.getDecomposition();
        if (icuMode == Collator.CANONICAL_DECOMPOSITION) {
            mode = java.text.Collator.CANONICAL_DECOMPOSITION;
        }
//        else if (icuMode == Collator.NO_DECOMPOSITION) {
//            mode = java.text.Collator.NO_DECOMPOSITION;
//        }
//        else {
//            throw new IllegalStateException("Unknown decomposition mode is used by the ICU Collator.");
//        }

        return mode;
    }

    public int getStrength() {
        int strength;
        int icuStrength = fIcuCollator.getStrength();
        switch (icuStrength) {
        case Collator.IDENTICAL:
            strength = java.text.Collator.IDENTICAL;
            break;
        case Collator.PRIMARY:
            strength = java.text.Collator.PRIMARY;
            break;
        case Collator.SECONDARY:
            strength = java.text.Collator.SECONDARY;
            break;
        case Collator.TERTIARY:
            strength = java.text.Collator.TERTIARY;
            break;
        case Collator.QUATERNARY:
            // Note: No quaternary support in Java..
            // Return tertiary instead for now.
            strength = java.text.Collator.TERTIARY;
            break;
        default:
            throw new IllegalStateException("Unknown strength is used by the ICU Collator.");
        }
        return strength;
    }

    public int hashCode() {
        return fIcuCollator.hashCode();
    }

    public void setDecomposition(int decompositionMode) {
        switch (decompositionMode) {
        case java.text.Collator.CANONICAL_DECOMPOSITION:
            fIcuCollator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            break;
        case java.text.Collator.NO_DECOMPOSITION:
            fIcuCollator.setDecomposition(Collator.NO_DECOMPOSITION);
            break;
        case java.text.Collator.FULL_DECOMPOSITION:
            // Not supported by ICU.
            // This option is interpreted as IDENTICAL strength.
            fIcuCollator.setStrength(Collator.IDENTICAL);
            break;
        default:
            throw new IllegalArgumentException("Invalid decomposition mode.");
        }
    }

    public void setStrength(int newStrength) {
        switch (newStrength) {
        case java.text.Collator.IDENTICAL:
            fIcuCollator.setStrength(Collator.IDENTICAL);
            break;
        case java.text.Collator.PRIMARY:
            fIcuCollator.setStrength(Collator.PRIMARY);
            break;
        case java.text.Collator.SECONDARY:
            fIcuCollator.setStrength(Collator.SECONDARY);
            break;
        case java.text.Collator.TERTIARY:
            fIcuCollator.setStrength(Collator.TERTIARY);
            break;
        default:
            throw new IllegalArgumentException("Invalid strength.");
        }
    }

}
