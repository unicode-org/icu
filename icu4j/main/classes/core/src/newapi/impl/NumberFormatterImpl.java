// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.FormatQuantity4;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.PatternString;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter;
import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.IGrouping;
import newapi.NumberFormatter.IRounding;
import newapi.NumberFormatter.IntegerWidth;
import newapi.NumberFormatter.Notation;
import newapi.NumberFormatter.NumberFormatterResult;
import newapi.NumberFormatter.Padding;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnlocalizedNumberFormatter;

/** @author sffc */
public class NumberFormatterImpl extends NumberFormatter.LocalizedNumberFormatter.Internal {

  private static final NumberFormatterImpl BASE = new NumberFormatterImpl();

  static final int KEY_MACROS = 0;
  static final int KEY_LOCALE = 1;
  static final int KEY_NOTATION = 2;
  static final int KEY_UNIT = 3;
  static final int KEY_ROUNDING = 4;
  static final int KEY_GROUPING = 5;
  static final int KEY_PADDING = 6;
  static final int KEY_INTEGER = 7;
  static final int KEY_SYMBOLS = 8;
  static final int KEY_UNIT_WIDTH = 9;
  static final int KEY_SIGN = 10;
  static final int KEY_DECIMAL = 11;
  static final int KEY_THRESHOLD = 12;
  static final int KEY_MAX = 13;

  public static NumberFormatterImpl with() {
    return BASE;
  }

  /** Internal method to set a starting macros. */
  public static NumberFormatterImpl fromMacros(MacroProps macros) {
    return new NumberFormatterImpl(BASE, KEY_MACROS, macros);
  }

  /**
   * Internal method to construct a chain from a pattern using {@link NumberPropertyMapper}. Could
   * be added to the public API if the feature is requested. In that case, a more efficient
   * implementation may be desired.
   */
  public static UnlocalizedNumberFormatter fromPattern(
      String string, DecimalFormatSymbols symbols) {
    Properties props = PatternString.parseToProperties(string);
    MacroProps macros = NumberPropertyMapper.oldToNew(props, symbols, null);
    return fromMacros(macros);
  }

  static final AtomicLongFieldUpdater<NumberFormatterImpl> callCount =
      AtomicLongFieldUpdater.newUpdater(NumberFormatterImpl.class, "callCountInternal");

  // TODO: Reduce the number of fields.
  final NumberFormatterImpl parent;
  final int key;
  final Object value;
  volatile MacroProps resolvedMacros;
  volatile long callCountInternal; // do not access directly; use callCount instead
  volatile NumberFormatterImpl savedWithUnit;
  volatile Worker1 compiled;

  /** Base constructor; called during startup only. Sets the threshold to the default value of 3. */
  private NumberFormatterImpl() {
    parent = null;
    key = KEY_THRESHOLD;
    value = new Long(3);
  }

  /** Primary constructor */
  private NumberFormatterImpl(NumberFormatterImpl parent, int key, Object value) {
    this.parent = parent;
    this.key = key;
    this.value = value;
  }

  @Override
  public NumberFormatterImpl notation(Notation notation) {
    return new NumberFormatterImpl(this, KEY_NOTATION, notation);
  }

  @Override
  public NumberFormatterImpl unit(MeasureUnit unit) {
    return new NumberFormatterImpl(this, KEY_UNIT, unit);
  }

  @Override
  public NumberFormatterImpl rounding(IRounding rounding) {
    return new NumberFormatterImpl(this, KEY_ROUNDING, rounding);
  }

  @Override
  public NumberFormatterImpl grouping(IGrouping grouping) {
    return new NumberFormatterImpl(this, KEY_GROUPING, grouping);
  }

  @Override
  public NumberFormatterImpl padding(Padding padding) {
    return new NumberFormatterImpl(this, KEY_PADDING, padding);
  }

  @Override
  public NumberFormatterImpl integerWidth(IntegerWidth style) {
    return new NumberFormatterImpl(this, KEY_INTEGER, style);
  }

  @Override
  public NumberFormatterImpl symbols(DecimalFormatSymbols symbols) {
    return new NumberFormatterImpl(this, KEY_SYMBOLS, symbols);
  }

  @Override
  public NumberFormatterImpl symbols(NumberingSystem ns) {
    return new NumberFormatterImpl(this, KEY_SYMBOLS, ns);
  }

  @Override
  public NumberFormatterImpl unitWidth(FormatWidth style) {
    return new NumberFormatterImpl(this, KEY_UNIT_WIDTH, style);
  }

  @Override
  public NumberFormatterImpl sign(SignDisplay style) {
    return new NumberFormatterImpl(this, KEY_SIGN, style);
  }

  @Override
  public NumberFormatterImpl decimal(DecimalMarkDisplay style) {
    return new NumberFormatterImpl(this, KEY_DECIMAL, style);
  }

  @Override
  public NumberFormatterImpl locale(Locale locale) {
    return new NumberFormatterImpl(this, KEY_LOCALE, ULocale.forLocale(locale));
  }

  @Override
  public NumberFormatterImpl locale(ULocale locale) {
    return new NumberFormatterImpl(this, KEY_LOCALE, locale);
  }

  /**
   * Internal fluent setter to support a custom regulation threshold. A threshold of 1 causes the
   * data structures to be built right away. A threshold of 0 prevents the data structures from
   * being built.
   */
  public NumberFormatterImpl threshold(Long threshold) {
    return new NumberFormatterImpl(this, KEY_THRESHOLD, threshold);
  }

  @Override
  public String toSkeleton() {
    return SkeletonBuilder.macrosToSkeleton(resolve());
  }

  @Override
  public NumberFormatterResult format(long input) {
    return format(new FormatQuantity4(input));
  }

  @Override
  public NumberFormatterResult format(double input) {
    return format(new FormatQuantity4(input));
  }

  @Override
  public NumberFormatterResult format(Number input) {
    return format(new FormatQuantity4(input));
  }

  @Override
  public NumberFormatterResult format(Measure input) {
    MeasureUnit unit = input.getUnit();
    Number number = input.getNumber();
    // Use this formatter if possible
    if (Objects.equals(resolve().unit, unit)) {
      return format(number);
    }
    // This mechanism saves the previously used unit, so if the user calls this method with the
    // same unit multiple times in a row, they get a more efficient code path.
    NumberFormatterImpl withUnit = savedWithUnit;
    if (withUnit == null || !Objects.equals(withUnit.resolve().unit, unit)) {
      withUnit = new NumberFormatterImpl(this, KEY_UNIT, unit);
      savedWithUnit = withUnit;
    }
    return withUnit.format(number);
  }

  /**
   * This is the core entrypoint to the number formatting pipeline. It performs self-regulation: a
   * static code path for the first few calls, and compiling a more efficient data structure if
   * called repeatedly.
   *
   * @param fq The quantity to be formatted.
   * @return The formatted number result.
   */
  public NumberFormatterResult format(FormatQuantity fq) {
    MacroProps macros = resolve();
    NumberStringBuilder string = new NumberStringBuilder();
    long currentCount = callCount.incrementAndGet(this);
    MicroProps micros;
    if (currentCount == macros.threshold.longValue()) {
      compiled = Worker1.fromMacros(macros);
      micros = compiled.apply(fq, string);
    } else if (compiled != null) {
      micros = compiled.apply(fq, string);
    } else {
      micros = Worker1.applyStatic(macros, fq, string);
    }
    return new NumberFormatterResult(string, fq, micros);
  }

  @Override
  public int hashCode() {
    return resolve().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof NumberFormatterImpl)) return false;
    return resolve().equals(((NumberFormatterImpl) other).resolve());
  }

  private MacroProps resolve() {
    if (resolvedMacros != null) {
      return resolvedMacros;
    }
    // Although the linked-list fluent storage approach requires this method,
    // my benchmarks show that linked-list is still faster than a full clone
    // of a MacroProps object at each step.
    MacroProps macros = new MacroProps();
    NumberFormatterImpl current = this;
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
        case KEY_ROUNDING:
          if (macros.rounding == null) {
            macros.rounding = (IRounding) current.value;
          }
          break;
        case KEY_GROUPING:
          if (macros.grouping == null) {
            macros.grouping = (IGrouping) current.value;
          }
          break;
        case KEY_PADDING:
          if (macros.padding == null) {
            macros.padding = (Padding) current.value;
          }
          break;
        case KEY_INTEGER:
          if (macros.integerWidth == null) {
            macros.integerWidth = (IntegerWidth) current.value;
          }
          break;
        case KEY_SYMBOLS:
          if (macros.symbols == null) {
            macros.symbols = /*(Object)*/ current.value;
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
}
