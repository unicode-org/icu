// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import java.util.Deque;

import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.ModifierHolder;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Properties;

// TODO: This class isn't currently being used anywhere.  Consider removing it.

/** Attaches all prefixes and suffixes at this point in the render tree without bubbling up. */
public class StrongAffixFormat extends Format implements Format.AfterFormat {
  private final Format child;

  public StrongAffixFormat(Format child) {
    this.child = child;

    if (child == null) {
      throw new IllegalArgumentException("A child formatter is required for StrongAffixFormat");
    }
  }

  @Override
  public int process(
      Deque<FormatQuantity> inputs,
      ModifierHolder mods,
      NumberStringBuilder string,
      int startIndex) {
    int length = child.process(inputs, mods, string, startIndex);
    length += mods.applyAll(string, startIndex, startIndex + length);
    return length;
  }

  @Override
  public int after(
      ModifierHolder mods, NumberStringBuilder string, int leftIndex, int rightIndex) {
    return mods.applyAll(string, leftIndex, rightIndex);
  }

  @Override
  public void export(Properties properties) {
    // Nothing to do.
  }
}
