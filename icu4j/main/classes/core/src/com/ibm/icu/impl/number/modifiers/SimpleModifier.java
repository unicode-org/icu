// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.modifiers;

import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.NumberFormat.Field;

/**
 * The second primary implementation of {@link Modifier}, this one consuming a {@link
 * com.ibm.icu.text.SimpleFormatter} pattern.
 */
public class SimpleModifier extends Modifier.BaseModifier {
  private final String compiledPattern;
  private final Field field;
  private final boolean strong;

  /** Creates a modifier that uses the SimpleFormatter string formats. */
  public SimpleModifier(String compiledPattern, Field field, boolean strong) {
    this.compiledPattern = (compiledPattern == null) ? "\u0001\u0000" : compiledPattern;
    this.field = field;
    this.strong = strong;
  }

  @Override
  public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
    return formatAsPrefixSuffix(compiledPattern, output, leftIndex, rightIndex, field);
  }

  @Override
  public boolean isStrong() {
    return strong;
  }

  @Override
  public String getPrefix() {
    // TODO: Implement this when MeasureFormat is ready.
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSuffix() {
    // TODO: Implement this when MeasureFormat is ready.
    throw new UnsupportedOperationException();
  }

  /**
   * TODO: This belongs in SimpleFormatterImpl. The only reason I haven't moved it there yet is
   * because DoubleSidedStringBuilder is an internal class and SimpleFormatterImpl feels like it
   * should not depend on it.
   *
   * <p>Formats a value that is already stored inside the StringBuilder <code>result</code> between
   * the indices <code>startIndex</code> and <code>endIndex</code> by inserting characters before
   * the start index and after the end index.
   *
   * <p>This is well-defined only for patterns with exactly one argument.
   *
   * @param compiledPattern Compiled form of a pattern string.
   * @param result The StringBuilder containing the value argument.
   * @param startIndex The left index of the value within the string builder.
   * @param endIndex The right index of the value within the string builder.
   * @return The number of characters (UTF-16 code points) that were added to the StringBuilder.
   */
  public static int formatAsPrefixSuffix(
      String compiledPattern,
      NumberStringBuilder result,
      int startIndex,
      int endIndex,
      Field field) {
    assert SimpleFormatterImpl.getArgumentLimit(compiledPattern) == 1;
    int ARG_NUM_LIMIT = 0x100;
    int length = 0, offset = 2;
    if (compiledPattern.charAt(1) != '\u0000') {
      int prefixLength = compiledPattern.charAt(1) - ARG_NUM_LIMIT;
      if (result != null) {
        result.insert(startIndex, compiledPattern, 2, 2 + prefixLength, field);
      }
      length += prefixLength;
      offset = 3 + prefixLength;
    }
    if (offset < compiledPattern.length()) {
      int suffixLength = compiledPattern.charAt(offset) - ARG_NUM_LIMIT;
      if (result != null) {
        result.insert(
            endIndex + length, compiledPattern, offset + 1, offset + suffixLength + 1, field);
      }
      length += suffixLength;
    }
    return length;
  }

  /** TODO: Move this to a test file somewhere, once we figure out what to do with the method. */
  public static void testFormatAsPrefixSuffix() {
    String[] patterns = {"{0}", "X{0}Y", "XX{0}YYY", "{0}YY", "XXXX{0}"};
    Object[][] outputs = {{"", 0, 0}, {"abcde", 0, 0}, {"abcde", 2, 2}, {"abcde", 1, 3}};
    String[][] expecteds = {
      {"", "XY", "XXYYY", "YY", "XXXX"},
      {"abcde", "XYabcde", "XXYYYabcde", "YYabcde", "XXXXabcde"},
      {"abcde", "abXYcde", "abXXYYYcde", "abYYcde", "abXXXXcde"},
      {"abcde", "aXbcYde", "aXXbcYYYde", "abcYYde", "aXXXXbcde"}
    };
    for (int i = 0; i < patterns.length; i++) {
      for (int j = 0; j < outputs.length; j++) {
        String pattern = patterns[i];
        String compiledPattern =
            SimpleFormatterImpl.compileToStringMinMaxArguments(pattern, new StringBuilder(), 1, 1);
        NumberStringBuilder output = new NumberStringBuilder();
        output.append((String) outputs[j][0], null);
        formatAsPrefixSuffix(
            compiledPattern, output, (Integer) outputs[j][1], (Integer) outputs[j][2], null);
        String expected = expecteds[j][i];
        String actual = output.toString();
        assert expected.equals(actual);
      }
    }
  }

  @Override
  public void export(Properties properties) {
    throw new UnsupportedOperationException();
  }
}
