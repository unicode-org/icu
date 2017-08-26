// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;
import newapi.impl.MacroProps;
import newapi.impl.Padder;

public abstract class NumberFormatterSettings<T extends NumberFormatterSettings<?>> {

    static final int KEY_MACROS = 0;
    static final int KEY_LOCALE = 1;
    static final int KEY_NOTATION = 2;
    static final int KEY_UNIT = 3;
    static final int KEY_ROUNDER = 4;
    static final int KEY_GROUPER = 5;
    static final int KEY_PADDER = 6;
    static final int KEY_INTEGER = 7;
    static final int KEY_SYMBOLS = 8;
    static final int KEY_UNIT_WIDTH = 9;
    static final int KEY_SIGN = 10;
    static final int KEY_DECIMAL = 11;
    static final int KEY_THRESHOLD = 12;
    static final int KEY_MAX = 13;

    final NumberFormatterSettings<?> parent;
    final int key;
    final Object value;
    volatile MacroProps resolvedMacros;

    NumberFormatterSettings(NumberFormatterSettings<?> parent, int key, Object value) {
        this.parent = parent;
        this.key = key;
        this.value = value;
    }

    public T notation(Notation notation) {
        return create(KEY_NOTATION, notation);
    }

    public T unit(MeasureUnit unit) {
        return create(KEY_UNIT, unit);
    }

    public T rounding(Rounder rounder) {
        return create(KEY_ROUNDER, rounder);
    }

    public T grouping(Grouper grouper) {
        return create(KEY_GROUPER, grouper);
    }

    public T integerWidth(IntegerWidth style) {
        return create(KEY_INTEGER, style);
    }

    public T symbols(DecimalFormatSymbols symbols) {
        return create(KEY_SYMBOLS, symbols);
    }

    public T symbols(NumberingSystem ns) {
        return create(KEY_SYMBOLS, ns);
    }

    public T unitWidth(FormatWidth style) {
        return create(KEY_UNIT_WIDTH, style);
    }

    public T sign(SignDisplay style) {
        return create(KEY_SIGN, style);
    }

    public T decimal(DecimalMarkDisplay style) {
        return create(KEY_DECIMAL, style);
    }

    /** Internal method to set a starting macros. */
    public T macros(MacroProps macros) {
        return create(KEY_MACROS, macros);
    }

    /** Non-public method */
    public T padding(Padder padder) {
        return create(KEY_PADDER, padder);
    }

    /**
     * Internal fluent setter to support a custom regulation threshold. A threshold of 1 causes the data structures to
     * be built right away. A threshold of 0 prevents the data structures from being built.
     */
    public T threshold(Long threshold) {
        return create(KEY_THRESHOLD, threshold);
    }

    public String toSkeleton() {
        return SkeletonBuilder.macrosToSkeleton(resolve());
    }

    abstract T create(int key, Object value);

    MacroProps resolve() {
        if (resolvedMacros != null) {
            return resolvedMacros;
        }
        // Although the linked-list fluent storage approach requires this method,
        // my benchmarks show that linked-list is still faster than a full clone
        // of a MacroProps object at each step.
        MacroProps macros = new MacroProps();
        NumberFormatterSettings<?> current = this;
        while (current != null) {
            switch (current.key) {
            case KEY_MACROS:
                macros.fallback((MacroProps) current.value);
                break;
            case KEY_LOCALE:
                if (macros.loc == null) {
                    macros.loc = (ULocale) current.value;
                }
                break;
            case KEY_NOTATION:
                if (macros.notation == null) {
                    macros.notation = (Notation) current.value;
                }
                break;
            case KEY_UNIT:
                if (macros.unit == null) {
                    macros.unit = (MeasureUnit) current.value;
                }
                break;
            case KEY_ROUNDER:
                if (macros.rounder == null) {
                    macros.rounder = (Rounder) current.value;
                }
                break;
            case KEY_GROUPER:
                if (macros.grouper == null) {
                    macros.grouper = (Grouper) current.value;
                }
                break;
            case KEY_PADDER:
                if (macros.padder == null) {
                    macros.padder = (Padder) current.value;
                }
                break;
            case KEY_INTEGER:
                if (macros.integerWidth == null) {
                    macros.integerWidth = (IntegerWidth) current.value;
                }
                break;
            case KEY_SYMBOLS:
                if (macros.symbols == null) {
                    macros.symbols = /* (Object) */ current.value;
                }
                break;
            case KEY_UNIT_WIDTH:
                if (macros.unitWidth == null) {
                    macros.unitWidth = (FormatWidth) current.value;
                }
                break;
            case KEY_SIGN:
                if (macros.sign == null) {
                    macros.sign = (SignDisplay) current.value;
                }
                break;
            case KEY_DECIMAL:
                if (macros.decimal == null) {
                    macros.decimal = (DecimalMarkDisplay) current.value;
                }
                break;
            case KEY_THRESHOLD:
                if (macros.threshold == null) {
                    macros.threshold = (Long) current.value;
                }
                break;
            default:
                throw new AssertionError("Unknown key: " + current.key);
            }
            current = current.parent;
        }
        resolvedMacros = macros;
        return macros;
    }

    @Override
    public int hashCode() {
        return resolve().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof NumberFormatterSettings)) {
            return false;
        }
        return resolve().equals(((NumberFormatterSettings<?>) other).resolve());
    }
}
