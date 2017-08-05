// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.Map;

import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;

import newapi.NumberFormatter.NotationCompact;
import newapi.NumberFormatter.NotationScientific;
import newapi.NumberFormatter.SignDisplay;

@SuppressWarnings("deprecation")
public class NotationImpl {

  public static class NotationScientificImpl extends NotationScientific.Internal
      implements Cloneable {

    int engineeringInterval;
    boolean requireMinInt;
    int minExponentDigits;
    SignDisplay exponentSignDisplay;

    public NotationScientificImpl(int engineeringInterval) {
      this.engineeringInterval = engineeringInterval;
      requireMinInt = false;
      minExponentDigits = 1;
      exponentSignDisplay = SignDisplay.AUTO;
    }

    public NotationScientificImpl(
        int engineeringInterval,
        boolean requireMinInt,
        int minExponentDigits,
        SignDisplay exponentSignDisplay) {
      this.engineeringInterval = engineeringInterval;
      this.requireMinInt = requireMinInt;
      this.minExponentDigits = minExponentDigits;
      this.exponentSignDisplay = exponentSignDisplay;
    }

    @Override
    public NotationScientific withMinExponentDigits(int minExponentDigits) {
      NotationScientificImpl other = (NotationScientificImpl) this.clone();
      other.minExponentDigits = minExponentDigits;
      return other;
    }

    @Override
    public NotationScientific withExponentSignDisplay(SignDisplay exponentSignDisplay) {
      NotationScientificImpl other = (NotationScientificImpl) this.clone();
      other.exponentSignDisplay = exponentSignDisplay;
      return other;
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        // Should not happen since parent is Object
        throw new AssertionError(e);
      }
    }
  }

  public static class NotationCompactImpl extends NotationCompact.Internal {
    final CompactStyle compactStyle;
    final Map<String, Map<String, String>> compactCustomData;

    public NotationCompactImpl(CompactStyle compactStyle) {
      compactCustomData = null;
      this.compactStyle = compactStyle;
    }

    public NotationCompactImpl(Map<String, Map<String, String>> compactCustomData) {
      compactStyle = null;
      this.compactCustomData = compactCustomData;
    }
  }
}
