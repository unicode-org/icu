// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.FormatQuantity4;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.rounders.SignificantDigitsRounder;
import com.ibm.icu.impl.number.rounders.SignificantDigitsRounder.SignificantDigitsMode;

public class RounderTest {

  @Test
  public void testSignificantDigitsRounder() {
    Object[][][][] cases = {
      {
        {{1, -1}, {0, 2}, {2, 4}}, // minInt, maxInt, minFrac, maxFrac, minSig, maxSig
        {
          {0.0, "0.0", "0.0", "0"},
          {0.054321, "0.05432", "0.05", "0.054"},
          {0.54321, "0.5432", "0.54", "0.54"},
          {1.0, "1.0", "1.0", "1"},
          {5.4321, "5.432", "5.43", "5.43"},
          {10.0, "10", "10", "10"},
          {11.0, "11", "11", "11"},
          {100.0, "100", "100", "100"},
          {100.23, "100.2", "100.2", "100.2"},
          {543210.0, "543200", "543200", "543200"},
        }
      },
      {
        {{1, -1}, {0, 0}, {2, -1}}, // minInt, maxInt, minFrac, maxFrac, minSig, maxSig
        {
          {0.0, "0.0", "0", "0"},
          {0.054321, "0.054321", "0", "0.054"},
          {0.54321, "0.54321", "1", "0.54"},
          {1.0, "1.0", "1", "1"},
          {5.4321, "5.4321", "5", "5.4"},
          {10.0, "10", "10", "10"},
          {11.0, "11", "11", "11"},
          {100.0, "100", "100", "100"},
          {100.23, "100.23", "100", "100"},
          {543210.0, "543210", "543210", "543210"},
        }
      },
      {
        {{0, 2}, {1, 2}, {3, 3}}, // minInt, maxInt, minFrac, maxFrac, minSig, maxSig
        {
          {0.0, ".000", ".00", ".0"},
          {0.054321, ".0543", ".05", ".0543"},
          {0.54321, ".543", ".54", ".543"},
          {1.0, "1.00", "1.00", "1.0"},
          {5.4321, "5.43", "5.43", "5.43"},
          {10.0, "10.0", "10.0", "10.0"},
          {11.0, "11.0", "11.0", "11.0"},
          {100.0, "00.0", "00.0", "00.0"},
          {100.23, "00.2", "00.2", "00.2"},
          {543210.0, "10.0", "10.0", "10.0"}
        }
      }
    };

    int caseNumber = 0;
    for (Object[][][] cas : cases) {
      int minInt = (Integer) cas[0][0][0];
      int maxInt = (Integer) cas[0][0][1];
      int minFrac = (Integer) cas[0][1][0];
      int maxFrac = (Integer) cas[0][1][1];
      int minSig = (Integer) cas[0][2][0];
      int maxSig = (Integer) cas[0][2][1];

      Properties properties = new Properties();
      FormatQuantity4 fq = new FormatQuantity4();
      properties.setMinimumIntegerDigits(minInt);
      properties.setMaximumIntegerDigits(maxInt);
      properties.setMinimumFractionDigits(minFrac);
      properties.setMaximumFractionDigits(maxFrac);
      properties.setMinimumSignificantDigits(minSig);
      properties.setMaximumSignificantDigits(maxSig);

      int runNumber = 0;
      for (Object[] run : cas[1]) {
        double input = (Double) run[0];
        String expected1 = (String) run[1];
        String expected2 = (String) run[2];
        String expected3 = (String) run[3];

        properties.setSignificantDigitsMode(SignificantDigitsMode.OVERRIDE_MAXIMUM_FRACTION);
        fq.setToDouble(input);
        SignificantDigitsRounder.getInstance(properties).apply(fq);
        assertEquals(
            "Case " + caseNumber + ", run " + runNumber + ", mode 0: " + fq,
            expected1,
            formatQuantityToString(fq));

        properties.setSignificantDigitsMode(SignificantDigitsMode.RESPECT_MAXIMUM_FRACTION);
        fq.setToDouble(input);
        SignificantDigitsRounder.getInstance(properties).apply(fq);
        assertEquals(
            "Case " + caseNumber + ", run " + runNumber + ", mode 1: " + fq,
            expected2,
            formatQuantityToString(fq));

        properties.setSignificantDigitsMode(SignificantDigitsMode.ENSURE_MINIMUM_SIGNIFICANT);
        fq.setToDouble(input);
        SignificantDigitsRounder.getInstance(properties).apply(fq);
        assertEquals(
            "Case " + caseNumber + ", run " + runNumber + ", mode 2: " + fq,
            expected3,
            formatQuantityToString(fq));

        runNumber++;
      }

      caseNumber++;
    }
  }

  private String formatQuantityToString(FormatQuantity fq) {
    StringBuilder sb = new StringBuilder();
    int udm = fq.getUpperDisplayMagnitude();
    int ldm = fq.getLowerDisplayMagnitude();
    if (udm == -1) sb.append('.');
    for (int m = udm; m >= ldm; m--) {
      sb.append(fq.getDigit(m));
      if (m == 0 && m > ldm) sb.append('.');
    }
    return sb.toString();
  }
}
