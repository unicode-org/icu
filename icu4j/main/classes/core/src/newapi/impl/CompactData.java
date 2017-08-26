// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CompactDecimalFormat.CompactType;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class CompactData implements MultiplierProducer {

  public static CompactData getInstance(
      ULocale locale, CompactType compactType, CompactStyle compactStyle) {
    // TODO: Add a data cache? It would be keyed by locale, compact type, and compact style.
    CompactData data = new CompactData();
    CompactDataSink sink = new CompactDataSink(data, compactType, compactStyle);
    String nsName = NumberingSystem.getInstance(locale).getName();
    ICUResourceBundle rb =
        (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
    CompactData.internalPopulateData(nsName, rb, sink, data);
    if (data.isEmpty() && compactStyle == CompactStyle.LONG) {
      // No long data is available; load short data instead
      sink.compactStyle = CompactStyle.SHORT;
      CompactData.internalPopulateData(nsName, rb, sink, data);
    }
    return data;
  }

  public static CompactData getInstance(
      Map<String, Map<String, String>> powersToPluralsToPatterns) {
    CompactData data = new CompactData();
    for (Map.Entry<String, Map<String, String>> magnitudeEntry :
        powersToPluralsToPatterns.entrySet()) {
      byte magnitude = (byte) (magnitudeEntry.getKey().length() - 1);
      for (Map.Entry<String, String> pluralEntry : magnitudeEntry.getValue().entrySet()) {
        StandardPlural plural = StandardPlural.fromString(pluralEntry.getKey().toString());
        String patternString = pluralEntry.getValue().toString();
        data.setPattern(patternString, magnitude, plural);
        int numZeros = countZeros(patternString);
        if (numZeros > 0) { // numZeros==0 in certain cases, like Somali "Kun"
          data.setMultiplier(magnitude, (byte) (numZeros - magnitude - 1));
        }
      }
    }
    return data;
  }

  private static void internalPopulateData(
      String nsName, ICUResourceBundle rb, CompactDataSink sink, CompactData data) {
    try {
      rb.getAllItemsWithFallback("NumberElements/" + nsName, sink);
    } catch (MissingResourceException e) {
      // Fall back to latn
    }
    if (data.isEmpty() && !nsName.equals("latn")) {
      rb.getAllItemsWithFallback("NumberElements/latn", sink);
    }
    if (sink.exception != null) {
      throw sink.exception;
    }
  }

  // A dummy object used when a "0" compact decimal entry is encountered.  This is necessary
  // in order to prevent falling back to root.  Object equality ("==") is intended.
  private static final String USE_FALLBACK = "<USE FALLBACK>";

  private final String[] patterns;
  private final byte[] multipliers;
  private boolean isEmpty;
  private int largestMagnitude;

  private static final int MAX_DIGITS = 15;

  private CompactData() {
    patterns = new String[(CompactData.MAX_DIGITS + 1) * StandardPlural.COUNT];
    multipliers = new byte[CompactData.MAX_DIGITS + 1];
    isEmpty = true;
    largestMagnitude = 0;
  }

  public boolean isEmpty() {
    return isEmpty;
  }

  @Override
  public int getMultiplier(int magnitude) {
    if (magnitude < 0) {
      return 0;
    }
    if (magnitude > largestMagnitude) {
      magnitude = largestMagnitude;
    }
    return multipliers[magnitude];
  }

  /** Returns the multiplier from the array directly without bounds checking. */
  public int getMultiplierDirect(int magnitude) {
    return multipliers[magnitude];
  }

  private void setMultiplier(int magnitude, byte multiplier) {
    if (multipliers[magnitude] != 0) {
      assert multipliers[magnitude] == multiplier;
      return;
    }
    multipliers[magnitude] = multiplier;
    isEmpty = false;
    if (magnitude > largestMagnitude) largestMagnitude = magnitude;
  }

  public String getPattern(int magnitude, StandardPlural plural) {
    if (magnitude < 0) {
      return null;
    }
    if (magnitude > largestMagnitude) {
      magnitude = largestMagnitude;
    }
    String patternString = patterns[getIndex(magnitude, plural)];
    if (patternString == null && plural != StandardPlural.OTHER) {
      // Fall back to "other" plural variant
      patternString = patterns[getIndex(magnitude, StandardPlural.OTHER)];
    }
    if (patternString == USE_FALLBACK) {
      // Return null if USE_FALLBACK is present
      patternString = null;
    }
    return patternString;
  }

  public Set<String> getAllPatterns() {
    Set<String> result = new HashSet<String>();
    result.addAll(Arrays.asList(patterns));
    result.remove(USE_FALLBACK);
    result.remove(null);
    return result;
  }

  private boolean has(int magnitude, StandardPlural plural) {
    // Return true if USE_FALLBACK is present
    return patterns[getIndex(magnitude, plural)] != null;
  }

  private void setPattern(String patternString, int magnitude, StandardPlural plural) {
    patterns[getIndex(magnitude, plural)] = patternString;
    isEmpty = false;
    if (magnitude > largestMagnitude) largestMagnitude = magnitude;
  }

  private void setNoFallback(int magnitude, StandardPlural plural) {
    setPattern(USE_FALLBACK, magnitude, plural);
  }

  private static final int getIndex(int magnitude, StandardPlural plural) {
    return magnitude * StandardPlural.COUNT + plural.ordinal();
  }

  private static final class CompactDataSink extends UResource.Sink {

    CompactData data;
    CompactStyle compactStyle;
    CompactType compactType;
    IllegalArgumentException exception;

    /*
     * NumberElements{              <-- top (numbering system table)
     *  latn{                       <-- patternsTable (one per numbering system)
     *    patternsLong{             <-- formatsTable (one per pattern)
     *      decimalFormat{          <-- powersOfTenTable (one per format)
     *        1000{                 <-- pluralVariantsTable (one per power of ten)
     *          one{"0 thousand"}   <-- plural variant and template
     */

    public CompactDataSink(CompactData data, CompactType compactType, CompactStyle compactStyle) {
      this.data = data;
      this.compactType = compactType;
      this.compactStyle = compactStyle;
    }

    @Override
    public void put(UResource.Key key, UResource.Value value, boolean isRoot) {
      UResource.Table patternsTable = value.getTable();
      for (int i1 = 0; patternsTable.getKeyAndValue(i1, key, value); ++i1) {
        if (key.contentEquals("patternsShort") && compactStyle == CompactStyle.SHORT) {
        } else if (key.contentEquals("patternsLong") && compactStyle == CompactStyle.LONG) {
        } else {
          continue;
        }

        // traverse into the table of formats
        UResource.Table formatsTable = value.getTable();
        for (int i2 = 0; formatsTable.getKeyAndValue(i2, key, value); ++i2) {
          if (key.contentEquals("decimalFormat") && compactType == CompactType.DECIMAL) {
          } else if (key.contentEquals("currencyFormat") && compactType == CompactType.CURRENCY) {
          } else {
            continue;
          }

          // traverse into the table of powers of ten
          UResource.Table powersOfTenTable = value.getTable();
          for (int i3 = 0; powersOfTenTable.getKeyAndValue(i3, key, value); ++i3) {

            // Assumes that the keys are always of the form "10000" where the magnitude is the
            // length of the key minus one
            byte magnitude = (byte) (key.length() - 1);
            byte multiplier = (byte) data.getMultiplierDirect(magnitude);

            // Silently ignore divisors that are too big.
            if (magnitude >= CompactData.MAX_DIGITS) continue;

            // Iterate over the plural variants ("one", "other", etc)
            UResource.Table pluralVariantsTable = value.getTable();
            for (int i4 = 0; pluralVariantsTable.getKeyAndValue(i4, key, value); ++i4) {

              // Skip this magnitude/plural if we already have it from a child locale.
              StandardPlural plural = StandardPlural.fromString(key.toString());
              if (data.has(magnitude, plural)) {
                continue;
              }

              // The value "0" means that we need to use the default pattern and not fall back
              // to parent locales.  Example locale where this is relevant: 'it'.
              String patternString = value.toString();
              if (patternString.equals("0")) {
                data.setNoFallback(magnitude, plural);
                continue;
              }

              // Save the pattern string.  We will parse it lazily.
              data.setPattern(patternString, magnitude, plural);

              // If necessary, compute the multiplier: the difference between the magnitude
              // and the number of zeros in the pattern.
              if (multiplier == 0) {
                int numZeros = countZeros(patternString);
                if (numZeros > 0) { // numZeros==0 in certain cases, like Somali "Kun"
                  multiplier = (byte) (numZeros - magnitude - 1);
                }
              }
            }

            data.setMultiplier(magnitude, multiplier);
          }

          // We want only one table of compact decimal formats, so if we get here, stop consuming.
          // The data.isEmpty() check will prevent further bundles from being traversed.
          return;
        }
      }
    }
  }

  private static final int countZeros(String patternString) {
    // NOTE: This strategy for computing the number of zeros is a hack for efficiency.
    // It could break if there are any 0s that aren't part of the main pattern.
    int numZeros = 0;
    for (int i = 0; i < patternString.length(); i++) {
      if (patternString.charAt(i) == '0') {
        numZeros++;
      } else if (numZeros > 0) {
        break; // zeros should always be contiguous
      }
    }
    return numZeros;
  }
}
