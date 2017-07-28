// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.modifiers;

import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.Modifier.AffixModifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.NumberFormat.Field;

/**
 * An implementation of {@link Modifier} that allows for multiple types of fields in the same
 * modifier. Constructed based on the contents of two {@link NumberStringBuilder} instances (one for
 * the prefix, one for the suffix).
 */
public class ConstantMultiFieldModifier extends Modifier.BaseModifier implements AffixModifier {

  // TODO: Avoid making a new instance by default if prefix and suffix are empty
  public static final ConstantMultiFieldModifier EMPTY = new ConstantMultiFieldModifier();

  protected final char[] prefixChars;
  protected final char[] suffixChars;
  protected final Field[] prefixFields;
  protected final Field[] suffixFields;
  private final String prefix;
  private final String suffix;
  private final boolean strong;

  public ConstantMultiFieldModifier(
      NumberStringBuilder prefix, NumberStringBuilder suffix, boolean strong) {
    prefixChars = prefix.toCharArray();
    suffixChars = suffix.toCharArray();
    prefixFields = prefix.toFieldArray();
    suffixFields = suffix.toFieldArray();
    this.prefix = new String(prefixChars);
    this.suffix = new String(suffixChars);
    this.strong = strong;
  }

  private ConstantMultiFieldModifier() {
    prefixChars = new char[0];
    suffixChars = new char[0];
    prefixFields = new Field[0];
    suffixFields = new Field[0];
    prefix = "";
    suffix = "";
    strong = false;
  }

  @Override
  public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
    // Insert the suffix first since inserting the prefix will change the rightIndex
    int length = output.insert(rightIndex, suffixChars, suffixFields);
    length += output.insert(leftIndex, prefixChars, prefixFields);
    return length;
  }

  @Override
  public boolean isStrong() {
    return strong;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  @Override
  public String getSuffix() {
    return suffix;
  }

  public boolean contentEquals(NumberStringBuilder prefix, NumberStringBuilder suffix) {
    return prefix.contentEquals(prefixChars, prefixFields)
        && suffix.contentEquals(suffixChars, suffixFields);
  }

  @Override
  public String toString() {
    return String.format("<ConstantMultiFieldModifier prefix:'%s' suffix:'%s'>", prefix, suffix);
  }

  @Override
  public void export(Properties properties) {
    throw new UnsupportedOperationException();
  }
}
