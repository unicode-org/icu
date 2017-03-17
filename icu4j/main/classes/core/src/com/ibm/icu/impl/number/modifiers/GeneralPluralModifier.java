// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.modifiers;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.ModifierHolder;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.PluralRules;

// TODO: Is it okay that this class is not completely immutable? Right now it is internal-only.
// Freezable or Builder could be used if necessary.

/**
 * A basic implementation of {@link com.ibm.icu.impl.number.Modifier.PositiveNegativePluralModifier}
 * that is built on the fly using its <code>put</code> methods.
 */
public class GeneralPluralModifier extends Format.BeforeFormat
    implements Modifier.PositiveNegativePluralModifier {
  /**
   * A single array for modifiers. Even elements are positive; odd elements are negative. The
   * elements 2i and 2i+1 belong to the StandardPlural with ordinal i.
   */
  private final Modifier[] mods;

  public GeneralPluralModifier() {
    this.mods = new Modifier[StandardPlural.COUNT * 2];
  }

  /** Adds a positive/negative-agnostic modifier for the specified plural form. */
  public void put(StandardPlural plural, Modifier modifier) {
    put(plural, modifier, modifier);
  }

  /** Adds a positive and a negative modifier for the specified plural form. */
  public void put(StandardPlural plural, Modifier positive, Modifier negative) {
    assert mods[plural.ordinal() * 2] == null;
    assert mods[plural.ordinal() * 2 + 1] == null;
    assert positive != null;
    assert negative != null;
    mods[plural.ordinal() * 2] = positive;
    mods[plural.ordinal() * 2 + 1] = negative;
  }

  @Override
  public Modifier getModifier(StandardPlural plural, boolean isNegative) {
    Modifier mod = mods[plural.ordinal() * 2 + (isNegative ? 1 : 0)];
    if (mod == null) {
      mod = mods[StandardPlural.OTHER.ordinal()*2 + (isNegative ? 1 : 0)];
    }
    if (mod == null) {
      throw new UnsupportedOperationException();
    }
    return mod;
  }

  @Override
  public void before(FormatQuantity input, ModifierHolder mods, PluralRules rules) {
    mods.add(getModifier(input.getStandardPlural(rules), input.isNegative()));
  }

  @Override
  public void before(FormatQuantity input, ModifierHolder mods) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void export(Properties properties) {
    // Since we can export only one affix pair, do the one for "other".
    Modifier positive = getModifier(StandardPlural.OTHER, false);
    Modifier negative = getModifier(StandardPlural.OTHER, true);
    PositiveNegativeAffixModifier.exportPositiveNegative(properties, positive, negative);
  }
}
