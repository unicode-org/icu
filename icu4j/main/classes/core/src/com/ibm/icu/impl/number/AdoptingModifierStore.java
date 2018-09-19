// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.StandardPlural;

/**
 * This implementation of ModifierStore adopts references to Modifiers.
 *
 * (This is named "adopting" because in C++, this class takes ownership of the Modifiers.)
 */
public class AdoptingModifierStore implements ModifierStore {
    private final Modifier positive;
    private final Modifier zero;
    private final Modifier negative;
    final Modifier[] mods;
    boolean frozen;

    /**
     * This constructor populates the ParameterizedModifier with a single positive and negative form.
     *
     * <p>
     * If this constructor is used, a plural form CANNOT be passed to {@link #getModifier}.
     */
    public AdoptingModifierStore(Modifier positive, Modifier zero, Modifier negative) {
        this.positive = positive;
        this.zero = zero;
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
        this.zero = null;
        this.negative = null;
        this.mods = new Modifier[3 * StandardPlural.COUNT];
        this.frozen = false;
    }

    public void setModifier(int signum, StandardPlural plural, Modifier mod) {
        assert !frozen;
        mods[getModIndex(signum, plural)] = mod;
    }

    public void freeze() {
        frozen = true;
    }

    public Modifier getModifierWithoutPlural(int signum) {
        assert frozen;
        assert mods == null;
        return signum == 0 ? zero : signum < 0 ? negative : positive;
    }

    public Modifier getModifier(int signum, StandardPlural plural) {
        assert frozen;
        assert positive == null;
        return mods[getModIndex(signum, plural)];
    }

    private static int getModIndex(int signum, StandardPlural plural) {
        assert signum >= -1 && signum <= 1;
        assert plural != null;
        return plural.ordinal() * 3 + (signum + 1);
    }
}
