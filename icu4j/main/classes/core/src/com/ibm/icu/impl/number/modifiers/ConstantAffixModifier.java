// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.modifiers;

import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.Modifier.AffixModifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.NumberFormat.Field;

/** The canonical implementation of {@link Modifier}, containing a prefix and suffix string. */
public class ConstantAffixModifier extends Modifier.BaseModifier implements AffixModifier {

  // TODO: Avoid making a new instance by default if prefix and suffix are empty
  public static final AffixModifier EMPTY = new ConstantAffixModifier();

  private final String prefix;
  private final String suffix;
  private final Field field;
  private final boolean strong;

  /**
   * Constructs an instance with the given strings.
   *
   * <p>The arguments need to be Strings, not CharSequences, because Strings are immutable but
   * CharSequences are not.
   *
   * @param prefix The prefix string.
   * @param suffix The suffix string.
   * @param field The field type to be associated with this modifier. Can be null.
   * @param strong Whether this modifier should be strongly applied.
   * @see Field
   */
  public ConstantAffixModifier(String prefix, String suffix, Field field, boolean strong) {
    // Use an empty string instead of null if we are given null
    // TODO: Consider returning a null modifier if both prefix and suffix are empty.
    this.prefix = (prefix == null ? "" : prefix);
    this.suffix = (suffix == null ? "" : suffix);
    this.field = field;
    this.strong = strong;
  }

  /**
   * Constructs a new instance with an empty prefix, suffix, and field.
   */
  public ConstantAffixModifier() {
    prefix = "";
    suffix = "";
    field = null;
    strong = false;
  }

  @Override
  public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
    // Insert the suffix first since inserting the prefix will change the rightIndex
    int length = output.insert(rightIndex, suffix, field);
    length += output.insert(leftIndex, prefix, field);
    return length;
  }

  @Override
  public int length() {
    return prefix.length() + suffix.length();
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

  public boolean contentEquals(CharSequence _prefix, CharSequence _suffix) {
    if (_prefix == null && !prefix.isEmpty()) return false;
    if (_suffix == null && !suffix.isEmpty()) return false;
    if (_prefix != null && prefix.length() != _prefix.length()) return false;
    if (_suffix != null && suffix.length() != _suffix.length()) return false;
    for (int i = 0; i < prefix.length(); i++) {
      if (prefix.charAt(i) != _prefix.charAt(i)) return false;
    }
    for (int i = 0; i < suffix.length(); i++) {
      if (suffix.charAt(i) != _suffix.charAt(i)) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return String.format(
        "<ConstantAffixModifier(%d) prefix:'%s' suffix:'%s'>", length(), prefix, suffix);
  }

  @Override
  public void export(Properties properties) {
    throw new UnsupportedOperationException();
  }
}
