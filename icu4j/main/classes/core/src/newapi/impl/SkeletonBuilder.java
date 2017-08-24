// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.ibm.icu.impl.number.ThingsNeedingNewHome.PadPosition;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.Dimensionless;
import com.ibm.icu.util.MeasureUnit;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.FractionRounding;
import newapi.NumberFormatter.Grouping;
import newapi.NumberFormatter.IGrouping;
import newapi.NumberFormatter.IRounding;
import newapi.NumberFormatter.IntegerWidth;
import newapi.NumberFormatter.Notation;
import newapi.NumberFormatter.NotationCompact;
import newapi.NumberFormatter.NotationScientific;
import newapi.NumberFormatter.NotationSimple;
import newapi.NumberFormatter.Padding;
import newapi.NumberFormatter.Rounding;
import newapi.NumberFormatter.SignDisplay;
import newapi.impl.RoundingImpl.RoundingImplCurrency;
import newapi.impl.RoundingImpl.RoundingImplFraction;
import newapi.impl.RoundingImpl.RoundingImplFractionSignificant;
import newapi.impl.RoundingImpl.RoundingImplIncrement;
import newapi.impl.RoundingImpl.RoundingImplInfinity;
import newapi.impl.RoundingImpl.RoundingImplSignificant;

public final class SkeletonBuilder {

  public static String macrosToSkeleton(MacroProps macros) {
    // Print out the values in their canonical order.
    StringBuilder sb = new StringBuilder();
    if (macros.notation != null) {
      // sb.append("notation=");
      notationToSkeleton(macros.notation, sb);
      sb.append(' ');
    }
    if (macros.unit != null) {
      // sb.append("unit=");
      unitToSkeleton(macros.unit, sb);
      sb.append(' ');
    }
    if (macros.rounding != null) {
      // sb.append("rounding=");
      roundingToSkeleton(macros.rounding, sb);
      sb.append(' ');
    }
    if (macros.grouping != null) {
      sb.append("grouping=");
      groupingToSkeleton(macros.grouping, sb);
      sb.append(' ');
    }
    if (macros.padding != null) {
      sb.append("padding=");
      paddingToSkeleton(macros.padding, sb);
      sb.append(' ');
    }
    if (macros.integerWidth != null) {
      sb.append("integer-width=");
      integerWidthToSkeleton(macros.integerWidth, sb);
      sb.append(' ');
    }
    if (macros.symbols != null) {
      sb.append("symbols=");
      symbolsToSkeleton(macros.symbols, sb);
      sb.append(' ');
    }
    if (macros.unitWidth != null) {
      sb.append("unit-width=");
      unitWidthToSkeleton(macros.unitWidth, sb);
      sb.append(' ');
    }
    if (macros.sign != null) {
      sb.append("sign=");
      signToSkeleton(macros.sign, sb);
      sb.append(' ');
    }
    if (macros.decimal != null) {
      sb.append("decimal=");
      decimalToSkeleton(macros.decimal, sb);
      sb.append(' ');
    }
    if (sb.length() > 0) {
      // Remove the trailing space
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }

  public static MacroProps skeletonToMacros(String skeleton) {
    MacroProps macros = new MacroProps();
    for (int offset = 0; offset < skeleton.length(); ) {
      char c = skeleton.charAt(offset);
      switch (c) {
        case ' ':
          offset++;
          break;
        case 'E':
        case 'C':
        case 'I':
          offset += skeletonToNotation(skeleton, offset, macros);
          break;
        case '%':
        case 'B':
        case '$':
        case 'U':
          offset += skeletonToUnit(skeleton, offset, macros);
          break;
        case 'F':
        case 'S':
        case 'M':
        case 'G':
        case 'Y':
          offset += skeletonToRounding(skeleton, offset, macros);
          break;
        default:
          if (skeleton.regionMatches(offset, "notation=", 0, 9)) {
            offset += 9;
            offset += skeletonToNotation(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "unit=", 0, 5)) {
            offset += 5;
            offset += skeletonToUnit(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "rounding=", 0, 9)) {
            offset += 9;
            offset += skeletonToRounding(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "grouping=", 0, 9)) {
            offset += 9;
            offset += skeletonToGrouping(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "padding=", 0, 9)) {
            offset += 8;
            offset += skeletonToPadding(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "integer-width=", 0, 9)) {
            offset += 14;
            offset += skeletonToIntegerWidth(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "symbols=", 0, 9)) {
            offset += 8;
            offset += skeletonToSymbols(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "unit-width=", 0, 9)) {
            offset += 11;
            offset += skeletonToUnitWidth(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "sign=", 0, 9)) {
            offset += 5;
            offset += skeletonToSign(skeleton, offset, macros);
          } else if (skeleton.regionMatches(offset, "decimal=", 0, 9)) {
            offset += 8;
            offset += skeletonToDecimal(skeleton, offset, macros);
          } else {
            throw new IllegalArgumentException(
                "Unexpected token at offset " + offset + " in skeleton string: " + c);
          }
      }
    }
    return macros;
  }

  private static void notationToSkeleton(Notation value, StringBuilder sb) {
    if (value instanceof NotationScientific) {
      NotationImpl.NotationScientificImpl notation = (NotationImpl.NotationScientificImpl) value;
      sb.append('E');
      if (notation.engineeringInterval != 1) {
        sb.append(notation.engineeringInterval);
      }
      if (notation.exponentSignDisplay == SignDisplay.ALWAYS) {
        sb.append('+');
      } else if (notation.exponentSignDisplay == SignDisplay.NEVER) {
        sb.append('!');
      } else {
        assert notation.exponentSignDisplay == SignDisplay.AUTO;
      }
      if (notation.minExponentDigits != 1) {
        for (int i = 0; i < notation.minExponentDigits; i++) {
          sb.append('0');
        }
      }
    } else if (value instanceof NotationCompact) {
      NotationImpl.NotationCompactImpl notation = (NotationImpl.NotationCompactImpl) value;
      if (notation.compactStyle == CompactStyle.SHORT) {
        sb.append('C');
      } else {
        // FIXME: CCC or CCCC instead?
        sb.append("CC");
      }
    } else {
      assert value instanceof NotationSimple;
      sb.append('I');
    }
  }

  private static int skeletonToNotation(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    char c0 = skeleton.charAt(offset++);
    Notation result = null;
    if (c0 == 'E') {
      int engineering = 1;
      SignDisplay sign = SignDisplay.AUTO;
      int minExponentDigits = 0;
      char c = safeCharAt(skeleton, offset++);
      if (c >= '1' && c <= '9') {
        engineering = c - '0';
        c = safeCharAt(skeleton, offset++);
      }
      if (c == '+') {
        sign = SignDisplay.ALWAYS;
        c = safeCharAt(skeleton, offset++);
      }
      if (c == '!') {
        sign = SignDisplay.NEVER;
        c = safeCharAt(skeleton, offset++);
      }
      while (c == '0') {
        minExponentDigits++;
        c = safeCharAt(skeleton, offset++);
      }
      minExponentDigits = Math.max(1, minExponentDigits);
      result = new NotationImpl.NotationScientificImpl(engineering, false, minExponentDigits, sign);
    } else if (c0 == 'C') {
      char c = safeCharAt(skeleton, offset++);
      if (c == 'C') {
        result = Notation.COMPACT_LONG;
      } else {
        result = Notation.COMPACT_SHORT;
      }
    } else if (c0 == 'I') {
      result = Notation.SIMPLE;
    }
    output.notation = result;
    return offset - originalOffset;
  }

  private static void unitToSkeleton(MeasureUnit value, StringBuilder sb) {
    if (value.getType().equals("dimensionless")) {
      if (value.getSubtype().equals("percent")) {
        sb.append('%');
      } else if (value.getSubtype().equals("permille")) {
        sb.append("%%");
      } else {
        assert value.getSubtype().equals("base");
        sb.append('B');
      }
    } else if (value.getType().equals("currency")) {
      sb.append('$');
      sb.append(value.getSubtype());
    } else {
      sb.append("U:");
      sb.append(value.getType());
      sb.append(':');
      sb.append(value.getSubtype());
    }
  }

  private static int skeletonToUnit(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    char c0 = skeleton.charAt(offset++);
    MeasureUnit result = null;
    if (c0 == '%') {
      char c = safeCharAt(skeleton, offset++);
      if (c == '%') {
        result = Dimensionless.PERCENT;
      } else {
        result = Dimensionless.PERMILLE;
      }
    } else if (c0 == 'B') {
      result = Dimensionless.BASE;
    } else if (c0 == '$') {
      String currencyCode = skeleton.substring(offset, offset + 3);
      offset += 3;
      result = Currency.getInstance(currencyCode);
    } else if (c0 == 'U') {
      StringBuilder sb = new StringBuilder();
      offset += consumeUntil(skeleton, offset, ':', sb);
      String type = sb.toString();
      sb.setLength(0);
      offset += consumeUntil(skeleton, offset, ' ', sb);
      String subtype = sb.toString();
      for (MeasureUnit candidate : MeasureUnit.getAvailable(type)) {
        if (candidate.getSubtype().equals(subtype)) {
          result = candidate;
          break;
        }
      }
    }
    output.unit = result;
    return offset - originalOffset;
  }

  private static void roundingToSkeleton(IRounding value, StringBuilder sb) {
    if (!(value instanceof Rounding)) {
      // FIXME: Throw an exception here instead?
      return;
    }
    MathContext mathContext;
    if (value instanceof RoundingImplFraction) {
      RoundingImplFraction rounding = (RoundingImplFraction) value;
      sb.append('F');
      minMaxToSkeletonHelper(rounding.minFrac, rounding.maxFrac, sb);
      mathContext = rounding.mathContext;
    } else if (value instanceof RoundingImplSignificant) {
      RoundingImplSignificant rounding = (RoundingImplSignificant) value;
      sb.append('S');
      minMaxToSkeletonHelper(rounding.minSig, rounding.maxSig, sb);
      mathContext = rounding.mathContext;
    } else if (value instanceof RoundingImplFractionSignificant) {
      RoundingImplFractionSignificant rounding = (RoundingImplFractionSignificant) value;
      sb.append('F');
      minMaxToSkeletonHelper(rounding.minFrac, rounding.maxFrac, sb);
      if (rounding.minSig != -1) {
        sb.append('>');
        sb.append(rounding.minSig);
      } else {
        sb.append('<');
        sb.append(rounding.maxSig);
      }
      mathContext = rounding.mathContext;
    } else if (value instanceof RoundingImplIncrement) {
      RoundingImplIncrement rounding = (RoundingImplIncrement) value;
      sb.append('M');
      sb.append(rounding.increment.toString());
      mathContext = rounding.mathContext;
    } else if (value instanceof RoundingImplCurrency) {
      RoundingImplCurrency rounding = (RoundingImplCurrency) value;
      sb.append('G');
      sb.append(rounding.usage.name());
      mathContext = rounding.mc;
    } else {
      RoundingImplInfinity rounding = (RoundingImplInfinity) value;
      sb.append('Y');
      mathContext = rounding.mathContext;
    }
    // RoundingMode
    RoundingMode roundingMode = mathContext.getRoundingMode();
    if (roundingMode != RoundingMode.HALF_EVEN) {
      sb.append(';');
      sb.append(roundingMode.name());
    }
  }

  private static void minMaxToSkeletonHelper(int minFrac, int maxFrac, StringBuilder sb) {
    if (minFrac == maxFrac) {
      sb.append(minFrac);
    } else {
      boolean showMaxFrac = (maxFrac >= 0 && maxFrac < Integer.MAX_VALUE);
      if (minFrac > 0 || !showMaxFrac) {
        sb.append(minFrac);
      }
      sb.append('-');
      if (showMaxFrac) {
        sb.append(maxFrac);
      }
    }
  }

  private static int skeletonToRounding(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    char c0 = skeleton.charAt(offset++);
    Rounding result = null;
    if (c0 == 'F') {
      int[] minMax = new int[2];
      offset += skeletonToMinMaxHelper(skeleton, offset, minMax);
      FractionRounding temp = RoundingImplFraction.getInstance(minMax[0], minMax[1]);
      char c1 = skeleton.charAt(offset++);
      if (c1 == '<') {
        char c2 = skeleton.charAt(offset++);
        result = temp.withMaxFigures(c2 - '0');
      } else if (c1 == '>') {
        char c2 = skeleton.charAt(offset++);
        result = temp.withMinFigures(c2 - '0');
      } else {
        result = temp;
      }
    } else if (c0 == 'S') {
      int[] minMax = new int[2];
      offset += skeletonToMinMaxHelper(skeleton, offset, minMax);
      result = RoundingImplSignificant.getInstance(minMax[0], minMax[1]);
    } else if (c0 == 'M') {
      StringBuilder sb = new StringBuilder();
      offset += consumeUntil(skeleton, offset, ' ', sb);
      BigDecimal increment = new BigDecimal(sb.toString());
      result = RoundingImplIncrement.getInstance(increment);
    } else if (c0 == 'G') {
      StringBuilder sb = new StringBuilder();
      offset += consumeUntil(skeleton, offset, ' ', sb);
      CurrencyUsage usage = Enum.valueOf(CurrencyUsage.class, sb.toString());
      result = Rounding.currency(usage);
    } else if (c0 == 'Y') {
      result = Rounding.NONE;
    }
    output.rounding = result;
    return offset - originalOffset;
  }

  private static int skeletonToMinMaxHelper(String skeleton, int offset, int[] output) {
    int originalOffset = offset;
    char c0 = safeCharAt(skeleton, offset++);
    char c1 = safeCharAt(skeleton, offset++);
    // TODO: This algorithm breaks if the number is more than 1 char wide.
    if (c1 == '-') {
      output[0] = c0 - '0';
      char c2 = safeCharAt(skeleton, offset++);
      if (c2 == ' ') {
        output[1] = Integer.MAX_VALUE;
      } else {
        output[1] = c2 - '0';
      }
    } else if ('0' <= c1 && c1 <= '9') {
      output[0] = 0;
      output[1] = c1 - '0';
    } else {
      offset--;
      output[0] = c0 - '0';
      output[1] = c0 - '0';
    }
    return offset - originalOffset;
  }

  private static void groupingToSkeleton(IGrouping value, StringBuilder sb) {
    if (!(value instanceof Grouping)) {
      // FIXME: Throw an exception here instead?
      sb.append("custom");
      return;
    }
    if (value.equals(Grouping.DEFAULT)) {
      sb.append("DEFAULT");
    } else if (value.equals(Grouping.MIN_2_DIGITS)) {
      sb.append("DEFAULT_MIN_2_DIGITS");
    } else if (value.equals(Grouping.NONE)) {
      sb.append("NONE");
    } else {
      GroupingImpl grouping = (GroupingImpl) value;
      if (grouping.grouping2 == -1 || grouping.grouping2 == 0) {
        sb.append("NONE");
      } else {
        sb.append(grouping.grouping1);
        if (grouping.grouping2 != grouping.grouping1) {
          sb.append(',');
          sb.append(grouping.grouping2);
        }
        if (grouping.min2) {
          sb.append('&');
        }
      }
    }
  }

  private static int skeletonToGrouping(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    char c0 = skeleton.charAt(offset++);
    Grouping result = null;
    if ('0' <= c0 && c0 <= '9') {
      char c1 = safeCharAt(skeleton, offset++);
      if (c1 == ',') {
        char c2 = safeCharAt(skeleton, offset++);
        char c3 = safeCharAt(skeleton, offset++);
        result = GroupingImpl.getInstance((byte) (c0 - '0'), (byte) (c2 - '0'), c3 == '&');
      } else {
        result = GroupingImpl.getInstance((byte) (c0 - '0'), (byte) (c0 - '0'), c1 == '&');
      }
    } else {
      StringBuilder sb = new StringBuilder();
      offset += consumeUntil(skeleton, --offset, ' ', sb);
      String name = sb.toString();
      if (name.equals("DEFAULT")) {
        result = Grouping.DEFAULT;
      } else if (name.equals("DEFAULT_MIN_2_DIGITS")) {
        result = Grouping.MIN_2_DIGITS;
      } else if (name.equals("NONE")) {
        result = Grouping.NONE;
      }
    }
    output.grouping = result;
    return offset - originalOffset;
  }

  private static void paddingToSkeleton(Padding value, StringBuilder sb) {
    PaddingImpl padding = (PaddingImpl) value;
    if (padding == Padding.NONE) {
      sb.append("NONE");
      return;
    }
    sb.append(padding.targetWidth);
    sb.append(':');
    sb.append(padding.position.name());
    sb.append(':');
    if (!padding.paddingString.equals(" ")) {
      sb.append(padding.paddingString);
    }
  }

  private static int skeletonToPadding(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    char c0 = skeleton.charAt(offset++);
    if (c0 == 'N') {
      offset += consumeUntil(skeleton, --offset, ' ', null);
    } else if ('0' <= c0 && c0 <= '9') {
      long intResult = consumeInt(skeleton, --offset);
      offset += intResult & 0xffffffff;
      int width = (int) (intResult >>> 32);
      char c1 = safeCharAt(skeleton, offset++);
      if (c1 != ':') {
        return offset - originalOffset - 1;
      }
      StringBuilder sb = new StringBuilder();
      offset += consumeUntil(skeleton, offset, ':', sb);
      String padPositionString = sb.toString();
      sb.setLength(0);
      offset += consumeUntil(skeleton, offset, ' ', sb);
      String string = (sb.length() == 0) ? " " : sb.toString();
      PadPosition position = Enum.valueOf(PadPosition.class, padPositionString);
      output.padding = PaddingImpl.getInstance(string, width, position);
    }
    return offset - originalOffset;
  }

  private static void integerWidthToSkeleton(IntegerWidth value, StringBuilder sb) {
    IntegerWidthImpl impl = (IntegerWidthImpl) value;
    sb.append(impl.minInt);
    if (impl.maxInt != impl.minInt) {
      sb.append('-');
      if (impl.maxInt < Integer.MAX_VALUE) {
        sb.append(impl.maxInt);
      }
    }
  }

  private static int skeletonToIntegerWidth(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    long intResult = consumeInt(skeleton, offset);
    offset += intResult & 0xffffffff;
    int minInt = (int) (intResult >>> 32);
    char c1 = safeCharAt(skeleton, --offset);
    int maxInt;
    if (c1 == '-') {
        intResult = consumeInt(skeleton, offset);
        offset += intResult & 0xffffffff;
        maxInt = (int) (intResult >>> 32);
    }
  }

  private static void symbolsToSkeleton(Object value, StringBuilder sb) {
    if (value instanceof DecimalFormatSymbols) {
      // TODO: Check to see if any of the symbols are not default?
      sb.append("loc:");
      sb.append(((DecimalFormatSymbols) value).getULocale());
    } else {
      sb.append("ns:");
      sb.append(((NumberingSystem) value).getName());
    }
  }

  private static void unitWidthToSkeleton(FormatWidth value, StringBuilder sb) {
    sb.append(value.name());
  }

  private static int skeletonToUnitWidth(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    StringBuilder sb = new StringBuilder();
    offset += consumeUntil(skeleton, offset, ' ', sb);
    output.unitWidth = Enum.valueOf(FormatWidth.class, sb.toString());
    return offset - originalOffset;
  }

  private static void signToSkeleton(SignDisplay value, StringBuilder sb) {
    sb.append(value.name());
  }

  private static int skeletonToSign(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    StringBuilder sb = new StringBuilder();
    offset += consumeUntil(skeleton, offset, ' ', sb);
    output.sign = Enum.valueOf(SignDisplay.class, sb.toString());
    return offset - originalOffset;
  }

  private static void decimalToSkeleton(DecimalMarkDisplay value, StringBuilder sb) {
    sb.append(value.name());
  }

  private static int skeletonToDecimal(String skeleton, int offset, MacroProps output) {
    int originalOffset = offset;
    StringBuilder sb = new StringBuilder();
    offset += consumeUntil(skeleton, offset, ' ', sb);
    output.decimal = Enum.valueOf(DecimalMarkDisplay.class, sb.toString());
    return offset - originalOffset;
  }

  private static char safeCharAt(String str, int offset) {
    if (offset < str.length()) {
      return str.charAt(offset);
    } else {
      return ' ';
    }
  }

  private static int consumeUntil(String skeleton, int offset, char brk, StringBuilder sb) {
    int originalOffset = offset;
    char c = safeCharAt(skeleton, offset++);
    while (c != brk) {
      if (sb != null) sb.append(c);
      c = safeCharAt(skeleton, offset++);
    }
    return offset - originalOffset;
  }

  private static long consumeInt(String skeleton, int offset) {
    int originalOffset = offset;
    char c = safeCharAt(skeleton, offset++);
    int result = 0;
    while ('0' <= c && c <= '9') {
      result = (result * 10) + (c - '0');
      c = safeCharAt(skeleton, offset++);
    }
    return (offset - originalOffset) | (((long) result) << 32);
  }
}
