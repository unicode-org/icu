// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.Modifier.Signum;

/**
 * This implementation of ModifierStore adopts references to Modifiers.
 *
 * (This is named "adopting" because in C++, this class takes ownership of the Modifiers.)
 */
public class AdoptingModifierStore implements ModifierStore {
    private final Modifier positive;
    private final Modifier posZero;
    private final Modifier negZero;
    private final Modifier negative;
    final Modifier[] mods;
    boolean frozen;

    /**
     * This constructor populates the ParameterizedModifier with a single positive and negative form.
     *
     * <p>
     * If this constructor is used, a plural form CANNOT be passed to {@link #getModifier}.
     */
    public AdoptingModifierStore(Modifier positive, Modifier posZero, Modifier negZero, Modifier negative) {
        this.positive = positive;
        this.posZero = posZero;
        this.negZero = negZero;
        this.negative = negative;
        this.mods = null;
        this.frozen = true;
    }

    /**
     * This constructor prepares the ParameterizedModifier to be populated with a positive and negative
     * Modifier for multiple plural forms.
     *
     * <p>
     * If this constructor is used, a plural form MUST be passed to {@link #getModifier}.
     */
    public AdoptingModifierStore() {
        this.positive = null;
        this.posZero = null;
        this.negZero = null;
        this.negative = null;
        this.mods = new Modifier[4 * StandardPlural.COUNT];
        this.frozen = false;
    }

    public void setModifier(Signum signum, StandardPlural plural, Modifier mod) {
        assert !frozen;
        mods[getModIndex(signum, plural)] = mod;
    }

    public void freeze() {
        frozen = true;
    }

    public Modifier getModifierWithoutPlural(Signum signum) {
        assert frozen;
        assert mods == null;
        assert signum != null;
        switch (signum) {
            case POS:
                return positive;
            case POS_ZERO:
                return posZero;
            case NEG_ZERO:
                return negZero;
            case NEG:
                return negative;
            default:
                throw new AssertionError("Unreachable");
        }
    }

    @Override
    public Modifier getModifier(Signum signum, StandardPlural plural) {
        assert frozen;
        assert positive == null;
        return mods[getModIndex(signum, plural)];
    }

    private static int getModIndex(Signum signum, StandardPlural plural) {
        assert signum != null;
        assert plural != null;
        return plural.ordinal() * Signum.COUNT + signum.ordinal();
    }
}
