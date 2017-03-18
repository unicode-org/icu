// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.ibm.icu.text.PluralRules;

// TODO: Get a better name for this base class.
public abstract class Format {

  protected static final ThreadLocal<NumberStringBuilder> threadLocalStringBuilder =
      new ThreadLocal<NumberStringBuilder>() {
        @Override
        protected NumberStringBuilder initialValue() {
          return new NumberStringBuilder();
        }
      };

  protected static final ThreadLocal<ModifierHolder> threadLocalModifierHolder =
      new ThreadLocal<ModifierHolder>() {
        @Override
        protected ModifierHolder initialValue() {
          return new ModifierHolder();
        }
      };

  public String format(FormatQuantity... inputs) {
    // Setup
    Deque<FormatQuantity> inputDeque = new ArrayDeque<FormatQuantity>();
    inputDeque.addAll(Arrays.asList(inputs));
    ModifierHolder modDeque = threadLocalModifierHolder.get().clear();
    NumberStringBuilder sb = threadLocalStringBuilder.get().clear();

    // Primary "recursion" step, calling the implementation's process method
    int length = process(inputDeque, modDeque, sb, 0);

    // Resolve remaining affixes
    length += modDeque.applyAll(sb, 0, length);
    return sb.toString();
  }

  /** A Format that works on only one number. */
  public abstract static class SingularFormat extends Format implements Exportable {

    public String format(FormatQuantity input) {
      NumberStringBuilder sb = formatToStringBuilder(input);
      return sb.toString();
    }

    public void format(FormatQuantity input, StringBuffer output) {
      NumberStringBuilder sb = formatToStringBuilder(input);
      output.append(sb);
    }

    public String format(FormatQuantity input, FieldPosition fp) {
      NumberStringBuilder sb = formatToStringBuilder(input);
      sb.populateFieldPosition(fp, 0);
      return sb.toString();
    }

    public void format(FormatQuantity input, StringBuffer output, FieldPosition fp) {
      NumberStringBuilder sb = formatToStringBuilder(input);
      sb.populateFieldPosition(fp, output.length());
      output.append(sb);
    }

    public AttributedCharacterIterator formatToCharacterIterator(FormatQuantity input) {
      NumberStringBuilder sb = formatToStringBuilder(input);
      return sb.getIterator();
    }

    private NumberStringBuilder formatToStringBuilder(FormatQuantity input) {
      // Setup
      ModifierHolder modDeque = threadLocalModifierHolder.get().clear();
      NumberStringBuilder sb = threadLocalStringBuilder.get().clear();

      // Primary "recursion" step, calling the implementation's process method
      int length = process(input, modDeque, sb, 0);

      // Resolve remaining affixes
      length += modDeque.applyAll(sb, 0, length);
      return sb;
    }

    @Override
    public int process(
        Deque<FormatQuantity> input,
        ModifierHolder mods,
        NumberStringBuilder string,
        int startIndex) {
      return process(input.removeFirst(), mods, string, startIndex);
    }

    public abstract int process(
        FormatQuantity input, ModifierHolder mods, NumberStringBuilder string, int startIndex);
  }

  public static class BeforeTargetAfterFormat extends SingularFormat {
    // The formatters are kept as individual fields to avoid extra object creation overhead.
    private BeforeFormat before1 = null;
    private BeforeFormat before2 = null;
    private BeforeFormat before3 = null;
    private TargetFormat target = null;
    private AfterFormat after1 = null;
    private AfterFormat after2 = null;
    private AfterFormat after3 = null;
    private final PluralRules rules;

    public BeforeTargetAfterFormat(PluralRules rules) {
      this.rules = rules;
    }

    public void addBeforeFormat(BeforeFormat before) {
      if (before1 == null) {
        before1 = before;
      } else if (before2 == null) {
        before2 = before;
      } else if (before3 == null) {
        before3 = before;
      } else {
        throw new IllegalArgumentException("Only three BeforeFormats are allowed at a time");
      }
    }

    public void setTargetFormat(TargetFormat target) {
      this.target = target;
    }

    public void addAfterFormat(AfterFormat after) {
      if (after1 == null) {
        after1 = after;
      } else if (after2 == null) {
        after2 = after;
      } else if (after3 == null) {
        after3 = after;
      } else {
        throw new IllegalArgumentException("Only three AfterFormats are allowed at a time");
      }
    }

    @Override
    public String format(FormatQuantity input) {
      ModifierHolder mods = threadLocalModifierHolder.get().clear();
      NumberStringBuilder sb = threadLocalStringBuilder.get().clear();
      int length = process(input, mods, sb, 0);
      length += mods.applyAll(sb, 0, length);
      return sb.toString();
    }

    @Override
    public int process(
        FormatQuantity input, ModifierHolder mods, NumberStringBuilder string, int startIndex) {
      // Special case: modifiers are skipped for NaN
      int length = 0;
      if (!input.isNaN()) {
        if (before1 != null) {
          before1.before(input, mods, rules);
        }
        if (before2 != null) {
          before2.before(input, mods, rules);
        }
        if (before3 != null) {
          before3.before(input, mods, rules);
        }
      }
      length = target.target(input, string, startIndex);
      length += mods.applyStrong(string, startIndex, startIndex + length);
      if (after1 != null) {
        length += after1.after(mods, string, startIndex, startIndex + length);
      }
      if (after2 != null) {
        length += after2.after(mods, string, startIndex, startIndex + length);
      }
      if (after3 != null) {
        length += after3.after(mods, string, startIndex, startIndex + length);
      }
      return length;
    }

    @Override
    public void export(Properties properties) {
      if (before1 != null) {
        before1.export(properties);
      }
      if (before2 != null) {
        before2.export(properties);
      }
      if (before3 != null) {
        before3.export(properties);
      }
      target.export(properties);
      if (after1 != null) {
        after1.export(properties);
      }
      if (after2 != null) {
        after2.export(properties);
      }
      if (after3 != null) {
        after3.export(properties);
      }
    }
  }

  public static class PositiveNegativeRounderTargetFormat extends SingularFormat {
    private final Modifier.PositiveNegativeModifier positiveNegative;
    private final Rounder rounder;
    private final TargetFormat target;

    public PositiveNegativeRounderTargetFormat(
        Modifier.PositiveNegativeModifier positiveNegative, Rounder rounder, TargetFormat target) {
      this.positiveNegative = positiveNegative;
      this.rounder = rounder;
      this.target = target;
    }

    @Override
    public String format(FormatQuantity input) {
      NumberStringBuilder sb = threadLocalStringBuilder.get().clear();
      process(input, null, sb, 0);
      return sb.toString();
    }

    @Override
    public int process(
        FormatQuantity input, ModifierHolder mods, NumberStringBuilder string, int startIndex) {
      // Special case: modifiers are skipped for NaN
      Modifier mod = null;
      rounder.apply(input);
      if (!input.isNaN() && positiveNegative != null) {
        mod = positiveNegative.getModifier(input.isNegative());
      }
      int length = target.target(input, string, startIndex);
      if (mod != null) {
        length += mod.apply(string, 0, length);
      }
      return length;
    }

    @Override
    public void export(Properties properties) {
      rounder.export(properties);
      positiveNegative.export(properties);
      target.export(properties);
    }
  }

  public abstract static class BeforeFormat implements Exportable {
    protected abstract void before(FormatQuantity input, ModifierHolder mods);

    @SuppressWarnings("unused")
    public void before(FormatQuantity input, ModifierHolder mods, PluralRules rules) {
      before(input, mods);
    }
  }

  public static interface TargetFormat extends Exportable {
    public abstract int target(FormatQuantity input, NumberStringBuilder string, int startIndex);
  }

  public static interface AfterFormat extends Exportable {
    public abstract int after(
        ModifierHolder mods, NumberStringBuilder string, int leftIndex, int rightIndex);
  }

  // Instead of Dequeue<BigDecimal>, it could be Deque<Quantity> where
  // we control the API of Quantity
  public abstract int process(
      Deque<FormatQuantity> inputs,
      ModifierHolder outputMods,
      NumberStringBuilder outputString,
      int startIndex);
}
