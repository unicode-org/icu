// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.CompactData;
import com.ibm.icu.impl.number.CompactData.CompactType;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.MicroProps;
import com.ibm.icu.impl.number.MicroPropsGenerator;
import com.ibm.icu.impl.number.MutablePatternModifier;
import com.ibm.icu.impl.number.MutablePatternModifier.ImmutablePatternModifier;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;


/**
 * A class that defines the scientific notation style to be used when formatting numbers in NumberFormatter.
 *
 * <p>
 * This class exposes no public functionality. To create a CompactNotation, use one of the factory methods in
 * {@link Notation}.
 *
 * @draft ICU 60
 * @see NumberFormatter
 */
public class CompactNotation extends Notation {

    final CompactStyle compactStyle;
    final Map<String, Map<String, String>> compactCustomData;

    /* package-private */ CompactNotation(CompactStyle compactStyle) {
        compactCustomData = null;
        this.compactStyle = compactStyle;
    }

    /* package-private */ CompactNotation(Map<String, Map<String, String>> compactCustomData) {
        compactStyle = null;
        this.compactCustomData = compactCustomData;
    }

    /* package-private */ MicroPropsGenerator withLocaleData(ULocale locale, String nsName, CompactType compactType,
            PluralRules rules, MutablePatternModifier buildReference, MicroPropsGenerator parent) {
        // TODO: Add a data cache? It would be keyed by locale, nsName, compact type, and compact style.
        return new CompactHandler(this, locale, nsName, compactType, rules, buildReference, parent);
    }

    private static class CompactHandler implements MicroPropsGenerator {

        private static class CompactModInfo {
            public ImmutablePatternModifier mod;
            public int numDigits;
        }

        final PluralRules rules;
        final MicroPropsGenerator parent;
        final Map<String, CompactModInfo> precomputedMods;
        final CompactData data;

        private CompactHandler(CompactNotation notation, ULocale locale, String nsName, CompactType compactType,
                PluralRules rules, MutablePatternModifier buildReference, MicroPropsGenerator parent) {
            this.rules = rules;
            this.parent = parent;
            this.data = new CompactData();
            if (notation.compactStyle != null) {
                data.populate(locale, nsName, notation.compactStyle, compactType);
            } else {
                data.populate(notation.compactCustomData);
            }
            if (buildReference != null) {
                // Safe code path
                precomputedMods = new HashMap<String, CompactModInfo>();
                precomputeAllModifiers(buildReference);
            } else {
                // Unsafe code path
                precomputedMods = null;
            }
        }

        /** Used by the safe code path */
        private void precomputeAllModifiers(MutablePatternModifier buildReference) {
            Set<String> allPatterns = new HashSet<String>();
            data.getUniquePatterns(allPatterns);

            for (String patternString : allPatterns) {
                CompactModInfo info = new CompactModInfo();
                ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(patternString);
                buildReference.setPatternInfo(patternInfo);
                info.mod = buildReference.createImmutable();
                info.numDigits = patternInfo.positive.integerTotal;
                precomputedMods.put(patternString, info);
            }
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity quantity) {
            MicroProps micros = parent.processQuantity(quantity);
            assert micros.rounding != null;

            // Treat zero as if it had magnitude 0
            int magnitude;
            if (quantity.isZero()) {
                magnitude = 0;
                micros.rounding.apply(quantity);
            } else {
                // TODO: Revisit chooseMultiplierAndApply
                int multiplier = micros.rounding.chooseMultiplierAndApply(quantity, data);
                magnitude = quantity.isZero() ? 0 : quantity.getMagnitude();
                magnitude -= multiplier;
            }

            StandardPlural plural = quantity.getStandardPlural(rules);
            String patternString = data.getPattern(magnitude, plural);
            int numDigits = -1;
            if (patternString == null) {
                // Use the default (non-compact) modifier.
                // No need to take any action.
            } else if (precomputedMods != null) {
                // Safe code path.
                // Java uses a hash set here for O(1) lookup. C++ uses a linear search.
                CompactModInfo info = precomputedMods.get(patternString);
                info.mod.applyToMicros(micros, quantity);
                numDigits = info.numDigits;
            } else {
                // Unsafe code path.
                // Overwrite the PatternInfo in the existing modMiddle.
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