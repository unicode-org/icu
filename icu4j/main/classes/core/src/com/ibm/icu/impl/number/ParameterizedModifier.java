// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.StandardPlural;

/**
 * A ParameterizedModifier by itself is NOT a Modifier. Rather, it wraps a data structure containing two
 * or more Modifiers and returns the modifier appropriate for the current situation.
 */
public class ParameterizedModifier {
    private final Modifier positive;
    private final Modifier negative;
    final Modifier[] mods;
    boolean frozen;

    /**
     * This constructor populates the ParameterizedModifier with a single positive and negative form.
     *
     * <p>
     * If this constructor is used, a plural form CANNOT be passed to {@link #getModifier}.
     */
    public ParameterizedModifier(Modifier positive, Modifier negative) {
        this.positive = positive;
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
    public ParameterizedModifier() {
        this.positive = null;
        this.negative = null;
        this.mods = new Modifier[2 * StandardPlural.COUNT];
        this.frozen = false;
    }

    public void setModifier(boolean isNegative, StandardPlural plural, Modifier mod) {
        assert !frozen;
        mods[getModIndex(isNegative, plural)] = mod;
    }

    public void freeze() {
        frozen = true;
    }

    public Modifier getModifier(boolean isNegative) {
        assert frozen;
        assert mods == null;
        return isNegative ? negative : positive;
    }

    public Modifier getModifier(boolean isNegative, StandardPlural plural) {
        assert frozen;
        assert positive == null;
        return mods[getModIndex(isNegative, plural)];
    }

    private static int getModIndex(boolean isNegative, StandardPlural plural) {
        return plural.ordinal() * 2 + (isNegative ? 1 : 0);
    }
}
