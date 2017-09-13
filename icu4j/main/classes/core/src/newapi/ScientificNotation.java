// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;

import newapi.NumberFormatter.SignDisplay;
import newapi.Rounder.SignificantRounderImpl;
import newapi.impl.MicroProps;
import newapi.impl.MicroPropsGenerator;
import newapi.impl.MultiplierProducer;

@SuppressWarnings("unused")
public class ScientificNotation extends Notation implements Cloneable {

    int engineeringInterval;
    boolean requireMinInt;
    int minExponentDigits;
    SignDisplay exponentSignDisplay;

    /* package-private */ ScientificNotation(int engineeringInterval, boolean requireMinInt, int minExponentDigits,
            SignDisplay exponentSignDisplay) {
        this.engineeringInterval = engineeringInterval;
        this.requireMinInt = requireMinInt;
        this.minExponentDigits = minExponentDigits;
        this.exponentSignDisplay = exponentSignDisplay;
    }

    public ScientificNotation withMinExponentDigits(int minExponentDigits) {
        if (minExponentDigits >= 0 && minExponentDigits < Rounder.MAX_VALUE) {
            ScientificNotation other = (ScientificNotation) this.clone();
            other.minExponentDigits = minExponentDigits;
            return other;
        } else {
            throw new IllegalArgumentException("Integer digits must be between 0 and " + Rounder.MAX_VALUE);
        }
    }

    public ScientificNotation withExponentSignDisplay(SignDisplay exponentSignDisplay) {
        ScientificNotation other = (ScientificNotation) this.clone();
        other.exponentSignDisplay = exponentSignDisplay;
        return other;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Should not happen since parent is Object
            throw new AssertionError(e);
        }
    }

    /* package-private */ MicroPropsGenerator withLocaleData(DecimalFormatSymbols symbols, boolean build,
            MicroPropsGenerator parent) {
        return new MurkyScientificHandler(symbols, build, parent);
    }

    private class MurkyScientificHandler implements MicroPropsGenerator, MultiplierProducer, Modifier {

        final DecimalFormatSymbols symbols;
        final ImmutableScientificModifier[] precomputedMods;
        final MicroPropsGenerator parent;
        /* unsafe */ int exponent;

        private MurkyScientificHandler(DecimalFormatSymbols symbols, boolean safe, MicroPropsGenerator parent) {
            this.symbols = symbols;
            this.parent = parent;

            if (safe) {
                // Pre-build the modifiers for exponents -12 through 12
                precomputedMods = new ImmutableScientificModifier[25];
                for (int i = -12; i <= 12; i++) {
                    precomputedMods[i + 12] = new ImmutableScientificModifier(i);
                }
            } else {
                precomputedMods = null;
            }
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity quantity) {
            MicroProps micros = parent.processQuantity(quantity);
            assert micros.rounding != null;

            // Treat zero as if it had magnitude 0
            int exponent;
            if (quantity.isZero()) {
                if (requireMinInt && micros.rounding instanceof SignificantRounderImpl) {
                    // Show "00.000E0" on pattern "00.000E0"
                    ((SignificantRounderImpl) micros.rounding).apply(quantity, engineeringInterval);
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
                // Safe code path A
                micros.modInner = precomputedMods[exponent + 12];
            } else if (precomputedMods != null) {
                // Safe code path B
                micros.modInner = new ImmutableScientificModifier(exponent);
            } else {
                // Unsafe code path: mutates the object and re-uses it as a Modifier!
                this.exponent = exponent;
                micros.modInner = this;
            }

            // We already performed rounding. Do not perform it again.
            micros.rounding = Rounder.constructPassThrough();

            return micros;
        }

        @Override
        public int getMultiplier(int magnitude) {
            int interval = engineeringInterval;
            int digitsShown;
            if (requireMinInt) {
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

        @Override
        public int getPrefixLength() {
            // FIXME: Localized exponent separator location.
            return 0;
        }

        @Override
        public int getCodePointCount() {
            // This method is not used for strong modifiers.
            throw new AssertionError();
        }

        @Override
        public boolean isStrong() {
            return true;
        }

        @Override
        public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
            return doApply(exponent, output, rightIndex);
        }

        private int doApply(int exponent, NumberStringBuilder output, int rightIndex) {
            // FIXME: Localized exponent separator location.
            int i = rightIndex;
            // Append the exponent separator and sign
            i += output.insert(i, symbols.getExponentSeparator(), NumberFormat.Field.EXPONENT_SYMBOL);
            if (exponent < 0 && exponentSignDisplay != SignDisplay.NEVER) {
                i += output.insert(i, symbols.getMinusSignString(), NumberFormat.Field.EXPONENT_SIGN);
            } else if (exponentSignDisplay == SignDisplay.ALWAYS) {
                i += output.insert(i, symbols.getPlusSignString(), NumberFormat.Field.EXPONENT_SIGN);
            }
            // Append the exponent digits (using a simple inline algorithm)
            int disp = Math.abs(exponent);
            for (int j = 0; j < minExponentDigits || disp > 0; j++, disp /= 10) {
                int d = disp % 10;
                String digitString = symbols.getDigitStringsLocal()[d];
                i += output.insert(i - j, digitString, NumberFormat.Field.EXPONENT);
            }
            return i - rightIndex;
        }

        private class ImmutableScientificModifier implements Modifier {
            final int exponent;

            ImmutableScientificModifier(int exponent) {
                this.exponent = exponent;
            }

            @Override
            public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
                return doApply(exponent, output, rightIndex);
            }

            @Override
            public int getPrefixLength() {
                // FIXME: Localized exponent separator location.
                return 0;
            }

            @Override
            public int getCodePointCount() {
                // This method is not used for strong modifiers.
                throw new AssertionError();
            }

            @Override
            public boolean isStrong() {
                return true;
            }
        }
    }
}