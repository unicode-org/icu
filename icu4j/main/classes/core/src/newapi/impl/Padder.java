// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import com.ibm.icu.impl.number.NumberStringBuilder;

public class Padder {
    public static final String FALLBACK_PADDING_STRING = "\u0020"; // i.e. a space

    public enum PadPosition {
      BEFORE_PREFIX,
      AFTER_PREFIX,
      BEFORE_SUFFIX,
      AFTER_SUFFIX;

      public static PadPosition fromOld(int old) {
        switch (old) {
          case com.ibm.icu.text.DecimalFormat.PAD_BEFORE_PREFIX:
            return PadPosition.BEFORE_PREFIX;
          case com.ibm.icu.text.DecimalFormat.PAD_AFTER_PREFIX:
            return PadPosition.AFTER_PREFIX;
          case com.ibm.icu.text.DecimalFormat.PAD_BEFORE_SUFFIX:
            return PadPosition.BEFORE_SUFFIX;
          case com.ibm.icu.text.DecimalFormat.PAD_AFTER_SUFFIX:
            return PadPosition.AFTER_SUFFIX;
          default:
            throw new IllegalArgumentException("Don't know how to map " + old);
        }
      }

      public int toOld() {
        switch (this) {
          case BEFORE_PREFIX:
            return com.ibm.icu.text.DecimalFormat.PAD_BEFORE_PREFIX;
          case AFTER_PREFIX:
            return com.ibm.icu.text.DecimalFormat.PAD_AFTER_PREFIX;
          case BEFORE_SUFFIX:
            return com.ibm.icu.text.DecimalFormat.PAD_BEFORE_SUFFIX;
          case AFTER_SUFFIX:
            return com.ibm.icu.text.DecimalFormat.PAD_AFTER_SUFFIX;
          default:
            return -1; // silence compiler errors
        }
      }
    }

    private static final Padder NONE = new Padder(null, -1, null);

    String paddingString;
    int targetWidth;
    PadPosition position;

    public Padder(String paddingString, int targetWidth, PadPosition position) {
        // TODO: Add a few default instances
        this.paddingString = (paddingString == null) ? " " : paddingString;
        this.targetWidth = targetWidth;
        this.position = (position == null) ? PadPosition.BEFORE_PREFIX : position;
    }

    public static Padder none() {
        return NONE;
    }

    public static Padder codePoints(int cp, int targetWidth, PadPosition position) {
        // TODO: Validate the code point
        if (targetWidth >= 0) {
            String paddingString = String.valueOf(Character.toChars(cp));
            return new Padder(paddingString, targetWidth, position);
        } else {
            throw new IllegalArgumentException("Padding width must not be negative");
        }
    }

    public int applyModsAndMaybePad(MicroProps micros, NumberStringBuilder string, int leftIndex, int rightIndex) {
        // Apply modInner (scientific notation) before padding
        int innerLength = micros.modInner.apply(string, leftIndex, rightIndex);

        // No padding; apply the mods and leave.
        if (targetWidth < 0) {
            return applyMicroMods(micros, string, leftIndex, rightIndex + innerLength);
        }

        // Estimate the padding width needed.
        // TODO: Make this more efficient (less copying)
        // TODO: How to handle when padding is inserted between a currency sign and the number
        // when currency spacing is in play?
        NumberStringBuilder backup = new NumberStringBuilder(string);
        int length = innerLength + applyMicroMods(micros, string, leftIndex, rightIndex + innerLength);
        int requiredPadding = targetWidth - string.codePointCount();

        if (requiredPadding <= 0) {
            // Padding is not required.
            return length;
        }

        length = innerLength;
        string.copyFrom(backup);
        if (position == PadPosition.AFTER_PREFIX) {
            length += addPaddingHelper(paddingString, requiredPadding, string, leftIndex);
        } else if (position == PadPosition.BEFORE_SUFFIX) {
            length += addPaddingHelper(paddingString, requiredPadding, string, rightIndex + length);
        }
        length += applyMicroMods(micros, string, leftIndex, rightIndex + length);
        if (position == PadPosition.BEFORE_PREFIX) {
            length = addPaddingHelper(paddingString, requiredPadding, string, leftIndex);
        } else if (position == PadPosition.AFTER_SUFFIX) {
            length = addPaddingHelper(paddingString, requiredPadding, string, rightIndex + length);
        }

        // The length might not be exactly right due to currency spacing.
        // Make an adjustment if needed.
        while (string.codePointCount() < targetWidth) {
            int insertIndex;
            switch (position) {
            case AFTER_PREFIX:
                insertIndex = leftIndex + length;
                break;
            case BEFORE_SUFFIX:
                insertIndex = rightIndex + length;
                break;
            default:
                // Should not happen since currency spacing is always on the inside.
                throw new AssertionError();
            }
            length += string.insert(insertIndex, paddingString, null);
        }

        return length;
    }

    private static int applyMicroMods(MicroProps micros, NumberStringBuilder string, int leftIndex, int rightIndex) {
        int length = micros.modMiddle.apply(string, leftIndex, rightIndex);
        length += micros.modOuter.apply(string, leftIndex, rightIndex + length);
        return length;
    }

    private static int addPaddingHelper(String paddingString, int requiredPadding, NumberStringBuilder string,
            int index) {
        for (int i = 0; i < requiredPadding; i++) {
            string.insert(index, paddingString, null);
        }
        return paddingString.length() * requiredPadding;
    }
}