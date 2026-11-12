// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format.measureunitgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Models ICU's built-in {@code MeasureUnit} types, subtypes, naming overrides, and backward
 * compatibility aliases.
 */
public final class BuiltInUnits {

    public static final String CURRENCY_TYPE = "currency";
    public static final String NONE_TYPE = "none";

    // Constant and function names are public API, so they must stay stable across CLDR renames.
    private static final Map<String, String> NAME_OVERRIDES =
            Map.of(
                    // These CLDR IDs are too generic on their own.
                    "angle-revolution", "revolution-angle",
                    "temperature-generic", "generic-temperature",
                    // CLDR 37 renamed these units; ICU kept the existing API names.
                    "acceleration-meter-per-square-second", "meter-per-second-squared",
                    "consumption-liter-per-100-kilometer", "liter-per-100kilometers",
                    "pressure-inch-ofhg", "inch-hg",
                    "pressure-millimeter-ofhg", "millimeter-of-mercury",
                    "pressure-pound-force-per-square-inch", "pound-per-square-inch",
                    "torque-pound-force-foot", "pound-foot");

    // ICU4J had TimeUnit constants before MeasureUnit constants, so these keep type TimeUnit.
    private static final Set<String> TIME_UNITS =
            Set.of(
                    "duration-year",
                    "duration-month",
                    "duration-week",
                    "duration-day",
                    "duration-hour",
                    "duration-minute",
                    "duration-second");

    private static final List<Alias> ALIASES =
            List.of(
                    new Alias(
                            "concentr-milligram-ofglucose-per-deciliter",
                            "milligram-per-deciliter",
                            "CLDR 39 / ICU 69",
                            false),
                    new Alias(
                            "concentr-part-per-1e6", "part-per-million", "CLDR 48 / ICU 78", false),
                    new Alias("mass-tonne", "metric-ton", "CLDR 42 / ICU 72", true));

    private final Map<String, List<String>> sortedTypes;
    private final List<UnitConstant> constants;

    private BuiltInUnits(Map<String, List<String>> sortedTypes, List<UnitConstant> constants) {
        this.sortedTypes = sortedTypes;
        this.constants = constants;
    }

    public static BuiltInUnits fromData(UnitDataSource.Data data) {
        Map<String, Set<String>> unitsByType = data.getUnitsByType();
        if (unitsByType.containsKey(CURRENCY_TYPE) || unitsByType.containsKey(NONE_TYPE)) {
            throw new IllegalStateException(
                    "Unit data must not define reserved types 'currency' or 'none'.");
        }

        TreeMap<String, List<String>> byType = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : unitsByType.entrySet()) {
            byType.put(entry.getKey(), List.copyOf(new TreeSet<>(entry.getValue())));
        }
        byType.put(CURRENCY_TYPE, List.copyOf(new TreeSet<>(data.getCurrencyCodes())));
        byType.put(NONE_TYPE, List.of(""));

        Map<String, List<String>> orderedTypes =
                Collections.unmodifiableMap(new LinkedHashMap<>(byType));

        Map<String, Alias> aliasByTarget = new LinkedHashMap<>();
        for (Alias alias : ALIASES) {
            aliasByTarget.put(alias.unit, alias);
        }

        List<UnitConstant> constants = new ArrayList<>();
        Set<String> presentLongIds = new TreeSet<>();
        int typeIndex = 0;
        for (Map.Entry<String, List<String>> entry : orderedTypes.entrySet()) {
            String type = entry.getKey();
            if (!type.equals(CURRENCY_TYPE) && !type.equals(NONE_TYPE)) {
                List<String> subtypes = entry.getValue();
                for (int subtypeIndex = 0; subtypeIndex < subtypes.size(); subtypeIndex++) {
                    String subtype = subtypes.get(subtypeIndex);
                    String longId = type + "-" + subtype;
                    presentLongIds.add(longId);
                    String nameId = NAME_OVERRIDES.getOrDefault(longId, subtype);
                    UnitConstant primary =
                            new UnitConstant(
                                    type,
                                    subtype,
                                    typeIndex,
                                    subtypeIndex,
                                    toJavaName(nameId),
                                    toCppName(nameId),
                                    TIME_UNITS.contains(longId),
                                    null,
                                    null);
                    constants.add(primary);

                    Alias alias = aliasByTarget.get(longId);
                    if (alias != null) {
                        constants.add(
                                new UnitConstant(
                                        type,
                                        subtype,
                                        typeIndex,
                                        subtypeIndex,
                                        toJavaName(alias.oldUnit),
                                        toCppName(alias.oldUnit),
                                        false,
                                        alias,
                                        primary.getCppName()));
                    }
                }
            }
            typeIndex++;
        }

        checkTargetsExist(presentLongIds, NAME_OVERRIDES.keySet());
        checkTargetsExist(presentLongIds, aliasByTarget.keySet());
        checkUniqueNames(constants);

        return new BuiltInUnits(orderedTypes, Collections.unmodifiableList(constants));
    }

    private static void checkTargetsExist(Set<String> presentLongIds, Set<String> requiredTargets) {
        for (String longId : requiredTargets) {
            if (!presentLongIds.contains(longId)) {
                throw new IllegalStateException(
                        "Unit data no longer contains '"
                                + longId
                                + "'. Update NAME_OVERRIDES or ALIASES in BuiltInUnits so its"
                                + " public API constants remain available.");
            }
        }
    }

    private static void checkUniqueNames(List<UnitConstant> constants) {
        Set<String> seenJava = new TreeSet<>();
        Set<String> seenCpp = new TreeSet<>();
        for (UnitConstant c : constants) {
            if (!seenJava.add(c.getJavaName())) {
                throw new IllegalStateException("Duplicate Java constant name: " + c.getJavaName());
            }
            if (!seenCpp.add(c.getCppName())) {
                throw new IllegalStateException("Duplicate C++ method name: " + c.getCppName());
            }
        }
    }

    static String toJavaName(String nameId) {
        return nameId.toUpperCase(Locale.ENGLISH).replace('-', '_');
    }

    /** Uppercases the start of {@code nameId} and any letter after '-' or a digit. */
    static String toCppName(String nameId) {
        StringBuilder sb = new StringBuilder(nameId.length());
        boolean upperNext = true;
        for (int i = 0; i < nameId.length(); i++) {
            char ch = nameId.charAt(i);
            if (ch == '-') {
                upperNext = true;
            } else if (Character.isDigit(ch)) {
                upperNext = true;
                sb.append(ch);
            } else if (upperNext) {
                sb.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** Sorted types (including {@code currency} and {@code none}) mapped to sorted subtypes. */
    public Map<String, List<String>> getSortedTypes() {
        return sortedTypes;
    }

    /** Non-currency, non-none unit constants in generation order, including aliases. */
    public List<UnitConstant> getConstants() {
        return constants;
    }

    /** Old constant retained as an alias after CLDR renamed a unit. */
    public static final class Alias {
        private final String unit;
        private final String oldUnit;
        private final String renamedIn;
        private final boolean shortCppComment;

        Alias(String unit, String oldUnit, String renamedIn, boolean shortCppComment) {
            this.unit = unit;
            this.oldUnit = oldUnit;
            this.renamedIn = renamedIn;
            this.shortCppComment = shortCppComment;
        }

        public String getOldUnit() {
            return oldUnit;
        }

        public String getRenamedIn() {
            return renamedIn;
        }

        public boolean hasShortCppComment() {
            return shortCppComment;
        }
    }

    /** Single generated Java constant and C++ {@code createXXX}/{@code getXXX} pair. */
    public static final class UnitConstant {
        private final String type;
        private final String subtype;
        private final int typeIndex;
        private final int subtypeIndex;
        private final String javaName;
        private final String cppName;
        private final boolean timeUnit;
        private final Alias alias;
        private final String targetCppName;

        UnitConstant(
                String type,
                String subtype,
                int typeIndex,
                int subtypeIndex,
                String javaName,
                String cppName,
                boolean timeUnit,
                Alias alias,
                String targetCppName) {
            this.type = type;
            this.subtype = subtype;
            this.typeIndex = typeIndex;
            this.subtypeIndex = subtypeIndex;
            this.javaName = javaName;
            this.cppName = cppName;
            this.timeUnit = timeUnit;
            this.alias = alias;
            this.targetCppName = targetCppName;
        }

        public String getType() {
            return type;
        }

        public String getSubtype() {
            return subtype;
        }

        public int getTypeIndex() {
            return typeIndex;
        }

        public int getSubtypeIndex() {
            return subtypeIndex;
        }

        public String getJavaName() {
            return javaName;
        }

        public String getCppName() {
            return cppName;
        }

        public boolean isTimeUnit() {
            return timeUnit;
        }

        public Alias getAlias() {
            return alias;
        }

        public String getTargetCppName() {
            return targetCppName;
        }
    }
}
