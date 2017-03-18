// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
// THIS CLASS IS A PROOF OF CONCEPT ONLY.
// IT REQUIRES ADDITIONAL DISCUSION ABOUT ITS DESIGN AND IMPLEMENTATION.

package com.ibm.icu.impl.number.formatters;

import java.util.Deque;

import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.ModifierHolder;
import com.ibm.icu.impl.number.NumberStringBuilder;

public class RangeFormat extends Format {
  // Primary settings
  private final String separator;

  // Child formatters
  private final Format left;
  private final Format right;

  public RangeFormat(Format left, Format right, String separator) {
    this.separator = separator; // TODO: This would be loaded from locale data.
    this.left = left;
    this.right = right;

    if (left == null || right == null) {
      throw new IllegalArgumentException("Both child formatters are required for RangeFormat");
    }
  }

  @Override
  public int process(
      Deque<FormatQuantity> inputs,
      ModifierHolder mods,
      NumberStringBuilder string,
      int startIndex) {
    ModifierHolder lMods = new ModifierHolder();
    ModifierHolder rMods = new ModifierHolder();
    int lLen = left.process(inputs, lMods, string, startIndex);
    int rLen = right.process(inputs, rMods, string, startIndex + lLen);

    // Bubble up any modifiers that are shared between the two sides
    while (lMods.peekLast() != null && lMods.peekLast() == rMods.peekLast()) {
      mods.add(lMods.removeLast());
      rMods.removeLast();
    }

    // Apply the remaining modifiers
    lLen += lMods.applyAll(string, startIndex, startIndex + lLen);
    rLen += rMods.applyAll(string, startIndex + lLen, startIndex + lLen + rLen);

    int sLen = string.insert(startIndex + lLen, separator, null);

    return lLen + sLen + rLen;
  }
}
