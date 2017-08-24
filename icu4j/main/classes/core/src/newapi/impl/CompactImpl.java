// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.LdmlPatternInfo;
import com.ibm.icu.impl.number.LdmlPatternInfo.PatternParseResult;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CompactDecimalFormat.CompactType;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

import newapi.impl.MurkyModifier.ImmutableMurkyModifier;
import newapi.impl.RoundingImpl.RoundingImplDummy;

public class CompactImpl implements QuantityChain {

  final PluralRules rules;
  final CompactData data;
  final Map<String, CompactModInfo> precomputedMods;
  final QuantityChain parent;

  public static CompactImpl getInstance(
      ULocale dataLocale,
      CompactType compactType,
      CompactStyle compactStyle,
      PluralRules rules,
      MurkyModifier buildReference,
      QuantityChain parent) {
    CompactData data = CompactData.getInstance(dataLocale, compactType, compactStyle);
    return new CompactImpl(data, rules, buildReference, parent);
  }

  public static CompactImpl getInstance(
      Map<String, Map<String, String>> compactCustomData,
      PluralRules rules,
      MurkyModifier buildReference,
      QuantityChain parent) {
    CompactData data = CompactData.getInstance(compactCustomData);
    return new CompactImpl(data, rules, buildReference, parent);
  }

  private CompactImpl(
      CompactData data, PluralRules rules, MurkyModifier buildReference, QuantityChain parent) {
    this.data = data;
    this.rules = rules;
    if (buildReference != null) {
      precomputedMods = precomputeAllModifiers(data, buildReference);
    } else {
      precomputedMods = null;
    }
    this.parent = parent;
  }

  /** To be used by the building code path */
  public static Map<String, CompactModInfo> precomputeAllModifiers(
      CompactData data, MurkyModifier buildReference) {
    Map<String, CompactModInfo> precomputedMods = new HashMap<String, CompactModInfo>();
    Set<String> allPatterns = data.getAllPatterns();
    for (String patternString : allPatterns) {
      CompactModInfo info = new CompactModInfo();
      PatternParseResult patternInfo = LdmlPatternInfo.parse(patternString);
      buildReference.setPatternInfo(patternInfo);
      info.mod = buildReference.createImmutable();
      info.numDigits = patternInfo.positive.totalIntegerDigits;
      precomputedMods.put(patternString, info);
    }
    return precomputedMods;
  }

  private static class CompactModInfo {
    public ImmutableMurkyModifier mod;
    public int numDigits;
  }

  @Override
  public MicroProps withQuantity(FormatQuantity input) {
    MicroProps micros = parent.withQuantity(input);
    assert micros.rounding != null;

    // Treat zero as if it had magnitude 0
    int magnitude;
    if (input.isZero()) {
      magnitude = 0;
      micros.rounding.apply(input);
    } else {
      // TODO: Revisit chooseMultiplierAndApply
      int multiplier = micros.rounding.chooseMultiplierAndApply(input, data);
      magnitude = input.isZero() ? 0 : input.getMagnitude();
      magnitude -= multiplier;
    }

    StandardPlural plural = input.getStandardPlural(rules);
    String patternString = data.getPattern(magnitude, plural);
    int numDigits = -1;
    if (patternString == null) {
      // Use the default (non-compact) modifier.
      // No need to take any action.
    } else if (precomputedMods != null) {
      // Build code path.
      CompactModInfo info = precomputedMods.get(patternString);
      info.mod.applyToMicros(micros, input);
      numDigits = info.numDigits;
    } else {
      // Non-build code path.
      // Overwrite the PatternInfo in the existing modMiddle
      assert micros.modMiddle instanceof MurkyModifier;
      PatternParseResult patternInfo = LdmlPatternInfo.parse(patternString);
      ((MurkyModifier) micros.modMiddle).setPatternInfo(patternInfo);
      numDigits = patternInfo.positive.totalIntegerDigits;
    }

    // FIXME: Deal with numDigits == 0  (Awaiting a test case)

    // We already performed rounding.  Do not perform it again.
    micros.rounding = RoundingImplDummy.INSTANCE;

    return micros;
  }
}
