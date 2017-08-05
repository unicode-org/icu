// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.math.BigDecimal;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.LdmlPatternInfo.PatternParseResult;

import newapi.NumberFormatter.Grouping;
import newapi.NumberFormatter.IGrouping;

public class GroupingImpl extends Grouping.Internal {

  // Conveniences for Java handling of shorts
  private static final short S2 = 2;
  private static final short S3 = 3;

  // For the "placeholder constructor"
  public static final char TYPE_PLACEHOLDER = 0;
  public static final char TYPE_MIN2 = 1;
  public static final char TYPE_NONE = 2;

  // Statically initialized objects (cannot be used statically by other ICU classes)
  static final GroupingImpl NONE = new GroupingImpl(TYPE_NONE);
  static final GroupingImpl GROUPING_3 = new GroupingImpl(S3, S3, false);
  static final GroupingImpl GROUPING_3_2 = new GroupingImpl(S3, S2, false);
  static final GroupingImpl GROUPING_3_MIN2 = new GroupingImpl(S3, S3, true);
  static final GroupingImpl GROUPING_3_2_MIN2 = new GroupingImpl(S3, S2, true);

  static GroupingImpl getInstance(short grouping1, short grouping2, boolean min2) {
    if (grouping1 == -1) {
      return NONE;
    } else if (!min2 && grouping1 == 3 && grouping2 == 3) {
      return GROUPING_3;
    } else if (!min2 && grouping1 == 3 && grouping2 == 2) {
      return GROUPING_3_2;
    } else if (min2 && grouping1 == 3 && grouping2 == 3) {
      return GROUPING_3_MIN2;
    } else if (min2 && grouping1 == 3 && grouping2 == 2) {
      return GROUPING_3_2_MIN2;
    } else {
      return new GroupingImpl(grouping1, grouping2, min2);
    }
  }

  public static GroupingImpl normalizeType(IGrouping grouping, PatternParseResult patternInfo) {
    assert grouping != null;
    if (grouping instanceof GroupingImpl) {
      return ((GroupingImpl) grouping).withLocaleData(patternInfo);
    } else {
      return new GroupingImpl(grouping);
    }
  }

  final IGrouping lambda;
  final short grouping1; // -2 means "needs locale data"; -1 means "no grouping"
  final short grouping2;
  final boolean min2;

  /** The "placeholder constructor". Pass in one of the GroupingImpl.TYPE_* variables. */
  public GroupingImpl(char type) {
    lambda = null;
    switch (type) {
      case TYPE_PLACEHOLDER:
        grouping1 = -2;
        grouping2 = -2;
        min2 = false;
        break;
      case TYPE_MIN2:
        grouping1 = -2;
        grouping2 = -2;
        min2 = true;
        break;
      case TYPE_NONE:
        grouping1 = -1;
        grouping2 = -1;
        min2 = false;
        break;
      default:
        throw new AssertionError();
    }
  }

  private GroupingImpl(short grouping1, short grouping2, boolean min2) {
    this.lambda = null;
    this.grouping1 = grouping1;
    this.grouping2 = grouping2;
    this.min2 = min2;
  }

  private GroupingImpl(IGrouping lambda) {
    this.lambda = lambda;
    this.grouping1 = -3;
    this.grouping2 = -3;
    this.min2 = false;
  }

  GroupingImpl withLocaleData(PatternParseResult patternInfo) {
    if (grouping1 != -2) {
      return this;
    }
    assert lambda == null;
    short grouping1 = (short) (patternInfo.positive.groupingSizes & 0xffff);
    short grouping2 = (short) ((patternInfo.positive.groupingSizes >>> 16) & 0xffff);
    short grouping3 = (short) ((patternInfo.positive.groupingSizes >>> 32) & 0xffff);
    if (grouping2 == -1) {
      grouping1 = -1;
    }
    if (grouping3 == -1) {
      grouping2 = grouping1;
    }
    return getInstance(grouping1, grouping2, min2);
  }

  boolean groupAtPosition(int position, FormatQuantity value) {
    // Check for lambda function
    if (lambda != null) {
      // TODO: Cache the BigDecimal
      BigDecimal temp = value.toBigDecimal();
      return lambda.groupAtPosition(position, temp);
    }

    assert grouping1 != -2;
    if (grouping1 == -1 || grouping1 == 0) {
      // Either -1 or 0 means "no grouping"
      return false;
    }
    position -= grouping1;
    return position >= 0
        && (position % grouping2) == 0
        && value.getUpperDisplayMagnitude() - grouping1 + 1 >= (min2 ? 2 : 1);
  }
}
