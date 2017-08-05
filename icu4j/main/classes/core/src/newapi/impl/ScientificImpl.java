// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;

import newapi.NumberFormatter.SignDisplay;
import newapi.impl.RoundingImpl.RoundingImplDummy;
import newapi.impl.RoundingImpl.RoundingImplSignificant;

public class ScientificImpl implements QuantityChain, RoundingImpl.MultiplierProducer {

  final NotationImpl.NotationScientificImpl notation;
  final DecimalFormatSymbols symbols;
  final ScientificModifier[] precomputedMods;
  /* final */ QuantityChain parent;

  public static ScientificImpl getInstance(
      NotationImpl.NotationScientificImpl notation, DecimalFormatSymbols symbols, boolean build) {
    return new ScientificImpl(notation, symbols, build);
  }

  private ScientificImpl(
      NotationImpl.NotationScientificImpl notation, DecimalFormatSymbols symbols, boolean build) {
    this.notation = notation;
    this.symbols = symbols;

    if (build) {
      // Pre-build the modifiers for exponents -12 through 12
      precomputedMods = new ScientificModifier[25];
      for (int i = -12; i <= 12; i++) {
        precomputedMods[i + 12] = new ScientificModifier(i);
      }
    } else {
      precomputedMods = null;
    }
  }

  @Override
  public QuantityChain chain(QuantityChain parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public MicroProps withQuantity(FormatQuantity quantity) {
    MicroProps micros = parent.withQuantity(quantity);
    assert micros.rounding != null;

    // Treat zero as if it had magnitude 0
    int exponent;
    if (quantity.isZero()) {
      if (notation.requireMinInt && micros.rounding instanceof RoundingImplSignificant) {
        // Shown "00.000E0" on pattern "00.000E0"
        ((RoundingImplSignificant) micros.rounding).apply(quantity, notation.engineeringInterval);
        exponent = 0;
      } else {
        micros.rounding.apply(quantity);
        exponent = 0;
      }
    } else {
      exponent = -micros.rounding.chooseMultiplierAndApply(quantity, this);
    }

    // Add the Modifier for the scientific format.
    if (precomputedMods != null && exponent >= -12 && exponent <= 12) {
      micros.modInner = precomputedMods[exponent + 12];
    } else {
      micros.modInner = new ScientificModifier(exponent);
    }

    // We already performed rounding.  Do not perform it again.
    micros.rounding = RoundingImplDummy.INSTANCE;

    return micros;
  }

  private class ScientificModifier implements Modifier {
    final int exponent;

    ScientificModifier(int exponent) {
      this.exponent = exponent;
    }

    @Override
    public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
      // FIXME: Localized exponent separator location.
      int i = rightIndex;
      // Append the exponent separator and sign
      i += output.insert(i, symbols.getExponentSeparator(), NumberFormat.Field.EXPONENT_SYMBOL);
      if (exponent < 0 && notation.exponentSignDisplay != SignDisplay.NEVER_SHOWN) {
        i += output.insert(i, symbols.getMinusSignString(), NumberFormat.Field.EXPONENT_SIGN);
      } else if (notation.exponentSignDisplay == SignDisplay.ALWAYS_SHOWN) {
        i += output.insert(i, symbols.getPlusSignString(), NumberFormat.Field.EXPONENT_SIGN);
      }
      // Append the exponent digits (using a simple inline algorithm)
      int disp = Math.abs(exponent);
      for (int j = 0; j < notation.minExponentDigits || disp > 0; j++, disp /= 10) {
        int d = disp % 10;
        String digitString = symbols.getDigitStringsLocal()[d];
        i += output.insert(i - j, digitString, NumberFormat.Field.EXPONENT);
      }
      return i - rightIndex;
    }

    @Override
    public boolean isStrong() {
      return true;
    }

    @Override
    public String getPrefix() {
      // Should never get called
      throw new AssertionError();
    }

    @Override
    public String getSuffix() {
      // Should never get called
      throw new AssertionError();
    }
  }

  @Override
  public int getMultiplier(int magnitude) {
    int interval = notation.engineeringInterval;
    int digitsShown;
    if (notation.requireMinInt) {
      // For patterns like "000.00E0" and ".00E0"
      digitsShown = interval;
    } else if (interval <= 1) {
      // For patterns like "0.00E0" and "@@@E0"
      digitsShown = 1;
    } else {
      // For patterns like "##0.00"
      digitsShown = ((magnitude % interval + interval) % interval) + 1;
    }
    return digitsShown - magnitude - 1;
  }
}
