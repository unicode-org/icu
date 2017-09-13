// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnitWidth;
import newapi.impl.MacroProps;
import newapi.impl.Padder;

/**
 * An abstract base class for specifying settings related to number formatting. This class is implemented by
 * {@link UnlocalizedNumberFormatter} and {@link LocalizedNumberFormatter}.
 */
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

    /**
     * Specifies the notation style (simple, scientific, or compact) for rendering numbers.
     *
     * <ul>
     * <li>Simple notation: "12,300"
     * <li>Scientific notation: "1.23E4"
     * <li>Compact notation: "12K"
     * </ul>
     *
     * <p>
     * All notation styles will be properly localized with locale data, and all notation styles are compatible with
     * units, rounding strategies, and other number formatter settings.
     *
     * <p>
     * Pass this method the return value of a {@link Notation} factory method. For example:
     *
     * <pre>
     * NumberFormatter.with().notation(Notation.compactShort())
     * </pre>
     *
     * The default is to use simple notation.
     *
     * @param notation
     *            The notation strategy to use.
     * @return The fluent chain.
     * @see Notation
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T notation(Notation notation) {
        return create(KEY_NOTATION, notation);
    }

    /**
     * Specifies the unit (unit of measure, currency, or percent) to associate with rendered numbers.
     *
     * <ul>
     * <li>Unit of measure: "12.3 meters"
     * <li>Currency: "$12.30"
     * <li>Percent: "12.3%"
     * </ul>
     *
     * <p>
     * <strong>Note:</strong> The unit can also be specified by passing a {@link Measure} to
     * {@link LocalizedNumberFormatter#format(Measure)}. Units specified via the format method take precedence over
     * units specified here.
     *
     * <p>
     * All units will be properly localized with locale data, and all units are compatible with notation styles,
     * rounding strategies, and other number formatter settings.
     *
     * <p>
     * Pass this method any instance of {@link MeasureUnit}. For units of measure:
     *
     * <pre>
     * NumberFormatter.with().unit(MeasureUnit.METER)
     * </pre>
     *
     * Currency:
     *
     * <pre>
     * NumberFormatter.with().unit(Currency.getInstance("USD"))
     * </pre>
     *
     * Percent:
     *
     * <pre>
     * NumberFormatter.with().unit(NoUnit.PERCENT)
     * </pre>
     *
     * The default is to render without units (equivalent to {@link NoUnit#BASE}).
     *
     * @param unit
     *            The unit to render.
     * @return The fluent chain.
     * @see MeasureUnit
     * @see Currency
     * @see NoUnit
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T unit(MeasureUnit unit) {
        return create(KEY_UNIT, unit);
    }

    /**
     * Specifies the rounding strategy to use when formatting numbers.
     *
     * <ul>
     * <li>Round to 3 decimal places: "3.142"
     * <li>Round to 3 significant figures: "3.14"
     * <li>Round to the closest nickel: "3.15"
     * <li>Do not perform rounding: "3.1415926..."
     * </ul>
     *
     * <p>
     * Pass this method the return value of one of the factory methods on {@link Rounder}. For example:
     *
     * <pre>
     * NumberFormatter.with().rounding(Rounder.fixedFraction(2))
     * </pre>
     *
     * The default is to not perform rounding.
     *
     * @param rounder
     *            The rounding strategy to use.
     * @return The fluent chain.
     * @see Rounder
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T rounding(Rounder rounder) {
        return create(KEY_ROUNDER, rounder);
    }

    /**
     * Specifies the grouping strategy to use when formatting numbers.
     *
     * <ul>
     * <li>Default grouping: "12,300" and "1,230"
     * <li>Grouping with at least 2 digits: "12,300" and "1230"
     * <li>No grouping: "12300" and "1230"
     * </ul>
     *
     * <p>
     * The exact grouping widths will be chosen based on the locale.
     *
     * <p>
     * Pass this method the return value of one of the factory methods on {@link Grouper}. For example:
     *
     * <pre>
     * NumberFormatter.with().grouping(Grouper.min2())
     * </pre>
     *
     * The default is to perform grouping without concern for the minimum grouping digits.
     *
     * @param grouper
     *            The grouping strategy to use.
     * @return The fluent chain.
     * @see Grouper
     * @see Notation
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T grouping(Grouper grouper) {
        return create(KEY_GROUPER, grouper);
    }

    /**
     * Specifies the minimum and maximum number of digits to render before the decimal mark.
     *
     * <ul>
     * <li>Zero minimum integer digits: ".08"
     * <li>One minimum integer digit: "0.08"
     * <li>Two minimum integer digits: "00.08"
     * </ul>
     *
     * <p>
     * Pass this method the return value of {@link IntegerWidth#zeroFillTo(int)}. For example:
     *
     * <pre>
     * NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(2))
     * </pre>
     *
     * The default is to have one minimum integer digit.
     *
     * @param style
     *            The integer width to use.
     * @return The fluent chain.
     * @see IntegerWidth
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T integerWidth(IntegerWidth style) {
        return create(KEY_INTEGER, style);
    }

    /**
     * Specifies the symbols (decimal separator, grouping separator, percent sign, numerals, etc.) to use when rendering
     * numbers.
     *
     * <ul>
     * <li><em>en_US</em> symbols: "12,345.67"
     * <li><em>fr_FR</em> symbols: "12&nbsp;345,67"
     * <li><em>de_CH</em> symbols: "12’345.67"
     * <li><em>my_MY</em> symbols: "၁၂,၃၄၅.၆၇"
     * </ul>
     *
     * <p>
     * Pass this method an instance of {@link DecimalFormatSymbols}. For example:
     *
     * <pre>
     * NumberFormatter.with().symbols(DecimalFormatSymbols.getInstance(new ULocale("de_CH")))
     * </pre>
     *
     * <p>
     * <strong>Note:</strong> DecimalFormatSymbols automatically chooses the best numbering system based on the locale.
     * In the examples above, the first three are using the Latin numbering system, and the fourth is using the Myanmar
     * numbering system.
     *
     * <p>
     * <strong>Note:</strong> The instance of DecimalFormatSymbols will be copied: changes made to the symbols object
     * after passing it into the fluent chain will not be seen.
     *
     * <p>
     * <strong>Note:</strong> Calling this method will override the NumberingSystem previously specified in
     * {@link #symbols(NumberingSystem)}.
     *
     * <p>
     * The default is to choose the symbols based on the locale specified in the fluent chain.
     *
     * @param symbols
     *            The DecimalFormatSymbols to use.
     * @return The fluent chain.
     * @see DecimalFormatSymbols
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T symbols(DecimalFormatSymbols symbols) {
        symbols = (DecimalFormatSymbols) symbols.clone();
        return create(KEY_SYMBOLS, symbols);
    }

    /**
     * Specifies that the given numbering system should be used when fetching symbols.
     *
     * <ul>
     * <li>Latin numbering system: "12,345"
     * <li>Myanmar numbering system: "၁၂,၃၄၅"
     * <li>Math Sans Bold numbering system: "𝟭𝟮,𝟯𝟰𝟱"
     * </ul>
     *
     * <p>
     * Pass this method an instance of {@link NumberingSystem}. For example, to force the locale to always use the Latin
     * alphabet numbering system (ASCII digits):
     *
     * <pre>
     * NumberFormatter.with().symbols(NumberingSystem.LATIN)
     * </pre>
     *
     * <p>
     * <strong>Note:</strong> Calling this method will override the DecimalFormatSymbols previously specified in
     * {@link #symbols(DecimalFormatSymbols)}.
     *
     * <p>
     * The default is to choose the best numbering system for the locale.
     *
     * @param ns
     *            The NumberingSystem to use.
     * @return The fluent chain.
     * @see NumberingSystem
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T symbols(NumberingSystem ns) {
        return create(KEY_SYMBOLS, ns);
    }

    /**
     * Sets the width of the unit (measure unit or currency).
     *
     * <ul>
     * <li>Narrow: "$12.00", "12 m"
     * <li>Short: "12.00 USD", "12 m"
     * <li>Wide: "12.00 US dollars", "12 meters"
     * <li>Hidden: "12.00", "12"
     * </ul>
     *
     * <p>
     * Pass an element from the {@link FormatWidth} enum to this setter. For example:
     *
     * <pre>
     * NumberFormatter.with().unitWidth(FormatWidth.SHORT)
     * </pre>
     *
     * <p>
     * The default is the narrow width.
     *
     * @param style
     *            The with to use when rendering numbers.
     * @return The fluent chain
     * @see FormatWidth
     * @provisional This API might change or be removed in a future release.
     * @draft ICU 60
     */
    public T unitWidth(UnitWidth style) {
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

    /** Non-public method */
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
        // TODO: Remove the reference to the parent after the macros are resolved?
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
                    macros.unitWidth = (UnitWidth) current.value;
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
