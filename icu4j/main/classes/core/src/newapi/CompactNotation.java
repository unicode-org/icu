// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CompactDecimalFormat.CompactType;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

import newapi.impl.CompactData;
import newapi.impl.MicroProps;
import newapi.impl.MicroPropsGenerator;
import newapi.impl.MutablePatternModifier;
import newapi.impl.MutablePatternModifier.ImmutablePatternModifier;

public class CompactNotation extends Notation {

    final CompactStyle compactStyle;
    final Map<String, Map<String, String>> compactCustomData;

    public CompactNotation(CompactStyle compactStyle) {
        compactCustomData = null;
        this.compactStyle = compactStyle;
    }

    public CompactNotation(Map<String, Map<String, String>> compactCustomData) {
        compactStyle = null;
        this.compactCustomData = compactCustomData;
    }

    /* package-private */ MicroPropsGenerator withLocaleData(ULocale dataLocale, CompactType compactType, PluralRules rules,
            MutablePatternModifier buildReference, MicroPropsGenerator parent) {
        CompactData data;
        if (compactStyle != null) {
            data = CompactData.getInstance(dataLocale, compactType, compactStyle);
        } else {
            data = CompactData.getInstance(compactCustomData);
        }
        return new CompactImpl(data, rules, buildReference, parent);
    }

    private static class CompactImpl implements MicroPropsGenerator {

        private static class CompactModInfo {
            public ImmutablePatternModifier mod;
            public int numDigits;
        }

        final PluralRules rules;
        final CompactData data;
        final Map<String, CompactModInfo> precomputedMods;
        final MicroPropsGenerator parent;

        private CompactImpl(CompactData data, PluralRules rules, MutablePatternModifier buildReference, MicroPropsGenerator parent) {
            this.data = data;
            this.rules = rules;
            if (buildReference != null) {
                // Safe code path
                precomputedMods = precomputeAllModifiers(data, buildReference);
            } else {
                // Unsafe code path
                precomputedMods = null;
            }
            this.parent = parent;
        }

        /** Used by the safe code path */
        private static Map<String, CompactModInfo> precomputeAllModifiers(CompactData data,
                MutablePatternModifier buildReference) {
            Map<String, CompactModInfo> precomputedMods = new HashMap<String, CompactModInfo>();
            Set<String> allPatterns = data.getAllPatterns();
            for (String patternString : allPatterns) {
                CompactModInfo info = new CompactModInfo();
                ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(patternString);
                buildReference.setPatternInfo(patternInfo);
                info.mod = buildReference.createImmutable();
                info.numDigits = patternInfo.positive.integerTotal;
                precomputedMods.put(patternString, info);
            }
            return precomputedMods;
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity input) {
            MicroProps micros = parent.processQuantity(input);
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
                // Safe code path.
                CompactModInfo info = precomputedMods.get(patternString);
                info.mod.applyToMicros(micros, input);
                numDigits = info.numDigits;
            } else {
                // Unsafe code path.
                // Overwrite the PatternInfo in the existing modMiddle
                assert micros.modMiddle instanceof MutablePatternModifier;
                ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(patternString);
                ((MutablePatternModifier) micros.modMiddle).setPatternInfo(patternInfo);
                numDigits = patternInfo.positive.integerTotal;
            }

            // FIXME: Deal with numDigits == 0 (Awaiting a test case)

            // We already performed rounding. Do not perform it again.
            micros.rounding = Rounder.constructPassThrough();

            return micros;
        }
    }
}