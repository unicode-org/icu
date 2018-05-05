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
import com.ibm.icu.impl.number.DecimalFormatProperties;
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
 * A class that defines the scientific notation style to be used when formatting numbers in
 * NumberFormatter.
 *
 * <p>
 * This class exposes no public functionality. To create a CompactNotation, use one of the factory
 * methods in {@link Notation}.
 *
 * @draft ICU 60
 * @provisional This API might change or be removed in a future release.
 * @see NumberFormatter
 */
public class CompactNotation extends Notation {

    final CompactStyle compactStyle;
    final Map<String, Map<String, String>> compactCustomData;

    /**
     * Create a compact notation with custom data.
     * @internal
     * @deprecated This API is ICU internal only.
     * @see DecimalFormatProperties#setCompactCustomData
     */
    @Deprecated
    public static CompactNotation forCustomData(Map<String, Map<String, String>> compactCustomData) {
        return new CompactNotation(compactCustomData);
    }

    /* package-private */ CompactNotation(CompactStyle compactStyle) {
        compactCustomData = null;
        this.compactStyle = compactStyle;
    }

    /* package-private */ CompactNotation(Map<String, Map<String, String>> compactCustomData) {
        compactStyle = null;
        this.compactCustomData = compactCustomData;
    }

    /* package-private */ MicroPropsGenerator withLocaleData(
            ULocale locale,
            String nsName,
            CompactType compactType,
            PluralRules rules,
            MutablePatternModifier buildReference,
            MicroPropsGenerator parent) {
        // TODO: Add a data cache? It would be keyed by locale, nsName, compact type, and compact style.
        return new CompactHandler(this, locale, nsName, compactType, rules, buildReference, parent);
    }

    private static class CompactHandler implements MicroPropsGenerator {

        final PluralRules rules;
        final MicroPropsGenerator parent;
        final Map<String, ImmutablePatternModifier> precomputedMods;
        final CompactData data;

        private CompactHandler(
                CompactNotation notation,
                ULocale locale,
                String nsName,
                CompactType compactType,
                PluralRules rules,
                MutablePatternModifier buildReference,
                MicroPropsGenerator parent) {
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
                precomputedMods = new HashMap<String, ImmutablePatternModifier>();
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
                ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(patternString);
                buildReference.setPatternInfo(patternInfo);
                precomputedMods.put(patternString, buildReference.createImmutable());
            }
        }

        @Override
        public MicroProps processQuantity(DecimalQuantity quantity) {
            MicroProps micros = parent.processQuantity(quantity);
            assert micros.rounder != null;

            // Treat zero as if it had magnitude 0
            int magnitude;
            if (quantity.isZero()) {
                magnitude = 0;
                micros.rounder.apply(quantity);
            } else {
                // TODO: Revisit chooseMultiplierAndApply
                int multiplier = micros.rounder.chooseMultiplierAndApply(quantity, data);
                magnitude = quantity.isZero() ? 0 : quantity.getMagnitude();
                magnitude -= multiplier;
            }

            StandardPlural plural = quantity.getStandardPlural(rules);
            String patternString = data.getPattern(magnitude, plural);
            if (patternString == null) {
                // Use the default (non-compact) modifier.
                // No need to take any action.
            } else if (precomputedMods != null) {
                // Safe code path.
                // Java uses a hash set here for O(1) lookup. C++ uses a linear search.
                ImmutablePatternModifier mod = precomputedMods.get(patternString);
                mod.applyToMicros(micros, quantity);
            } else {
                // Unsafe code path.
                // Overwrite the PatternInfo in the existing modMiddle.
                assert micros.modMiddle instanceof MutablePatternModifier;
                ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(patternString);
                ((MutablePatternModifier) micros.modMiddle).setPatternInfo(patternInfo);
            }

            // We already performed rounding. Do not perform it again.
            micros.rounder = Precision.constructPassThrough();

            return micros;
        }
    }
}