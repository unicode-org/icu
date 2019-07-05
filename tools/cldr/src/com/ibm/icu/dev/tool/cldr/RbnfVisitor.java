package com.ibm.icu.dev.tool.cldr;

import com.google.common.collect.ImmutableMap;
import com.google.common.escape.UnicodeEscaper;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;
import static org.unicode.cldr.api.CldrDataType.LDML;
import static org.unicode.cldr.api.CldrDraftStatus.CONTRIBUTED;
import static org.unicode.cldr.api.CldrDraftStatus.UNCONFIRMED;

// TODO: This class can almost certainly be written using RegexTransformer and a small config.
final class RbnfVisitor implements PrefixVisitor {
    private static final PathMatcher PARENT_LOCALE =
        PathMatcher.of("supplementalData/parentLocales/parentLocale[@parent=*]");
    private static final AttributeKey PARENT = keyOf("parentLocale", "parent");
    private static final AttributeKey LOCALES = keyOf("parentLocale", "locales");

    private static final PathMatcher RULE_SET =
        PathMatcher.of("ldml/rbnf/rulesetGrouping[@type=*]/ruleset[@type=*]");
    private static final AttributeKey GROUPING_TYPE = keyOf("rulesetGrouping", "type");
    private static final AttributeKey RULESET_TYPE = keyOf("ruleset", "type");

    private static final PathMatcher RBNF_RULE = PathMatcher.of("rbnfrule");
    private static final AttributeKey RBNF_VALUE = keyOf("rbnfrule", "value");
    private static final AttributeKey RBNF_RADIX = keyOf("rbnfrule", "radix");
    private static final AttributeKey RULESET_ACCESS = keyOf("ruleset", "access");

    /** Processes data from the given supplier to generate day period data. */
    public static List<IcuData> process(CldrDataSupplier supplier, Path specialsDir) {
        List<IcuData> icuData = new ArrayList<>();
        supplier = supplier.withDraftStatus(CONTRIBUTED);

        ImmutableMap<String, String> parentLocaleMap = loadParentLocaleData(supplier);
        for (String localeId : supplier.getAvailableLocaleIds()) {
            RbnfVisitor v = new RbnfVisitor(localeId);

            // Using DTD order is essential here because the RBNF paths contain ordered elements,
            // so we must ensure that they appear in sorted order (otherwise we'd have to do more
            // work at this end to re-sort the results).
            Path specialsXml = specialsDir.resolve(localeId + ".xml");
            if (Files.exists(specialsXml)) {
                v.icuData.setFileComment(
                    "ICU <specials> source: <path>/xml/rbnf/" + localeId + ".xml");
                CldrDataSupplier.forCldrFile(LDML, specialsXml, UNCONFIRMED).accept(DTD, v);
            }
            supplier.getDataForLocale(localeId, UNRESOLVED).accept(DTD, v);
            if (v.hasData()) {
                String parentLocaleId = parentLocaleMap.get(localeId);
                if (parentLocaleId != null) {
                    v.icuData.add("/%%Parent", parentLocaleId);
                }
                v.icuData.add("/Version", CldrDataSupplier.getCldrVersionString());
                icuData.add(v.icuData);
            }
        }
        return icuData;
    }

    private final IcuData icuData;

    private RbnfVisitor(String localeId) {
        this.icuData = new IcuData("common/rbnf/" + localeId + ".xml", localeId, true);
    }

    private boolean hasData() {
        return !icuData.keySet().isEmpty();
    }

    @Override public void visitPrefixStart(CldrPath prefix, Context context) {
        if (RULE_SET.matchesPrefixOf(prefix)) {
            String rbPath = "/RBNFRules/" + GROUPING_TYPE.valueFrom(prefix);
            String rulesetType = RULESET_TYPE.valueFrom(prefix);
            boolean isStrict = !"lenient-parse".equals(rulesetType);

            // This is rather hacky because the access attribute lives on the parent path element,
            // but we cannot use it until we visit the child values (because it's a value attribute
            // and will not be in the prefix path. So we need to add the header only once, just
            // before we start adding the values relating to the child elements, so we need a flag.
            // This cannot be a boolean field due to the fact it's not "effectively final".
            AtomicBoolean hasHeader = new AtomicBoolean(false);
            context.install(
                value -> {
                    if (RBNF_RULE.matchesSuffixOf(value.getPath())) {
                        if (!hasHeader.get()) {
                            boolean isPrivate =
                                RULESET_ACCESS.valueFrom(value, "public").equals("private");
                            icuData.add(rbPath, (isPrivate ? "%%" : "%") + rulesetType + ":");
                            hasHeader.set(true);
                        }
                        String rulePrefix = "";
                        if (isStrict) {
                            String basePrefix = RBNF_VALUE.valueFrom(value);
                            rulePrefix = RBNF_RADIX.optionalValueFrom(value)
                                .map(r -> basePrefix + "/" + r)
                                .orElse(basePrefix);
                            rulePrefix += ": ";
                        }
                        icuData.add(rbPath, rulePrefix + ESCAPE_RBNF_DATA.escape(value.getValue()));
                    }
                });
        }
    }

    private static ImmutableMap<String, String> loadParentLocaleData(
        CldrDataSupplier supplier) {
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        supplier
            .getDataForType(CldrDataType.SUPPLEMENTAL)
            .accept(ARBITRARY,
                value -> {
                    if (PARENT_LOCALE.matches(value.getPath())) {
                        String parent = PARENT.valueFrom(value);
                        LOCALES.listOfValuesFrom(value).forEach(loc -> map.put(loc, parent));
                    }
                });
        return map.build();
    }

    /*
     * Convert characters outside the range U+0020 to U+007F to Unicode escapes, and convert
     * backslash to a double backslash. This class is super slow for non-ASCII escaping due to
     * using "String.format()", however there's < 100 values that need any escaping, so it's fine.
     * Don't copy this code for use with millions of values (have reusable char[] for that).
     */
    private static final UnicodeEscaper ESCAPE_RBNF_DATA = new UnicodeEscaper() {
        private final char[] DOUBLE_BACKSLASH = "\\\\".toCharArray();
        private final char[] LEFT_ANGLE = "<".toCharArray();
        private final char[] RIGHT_ANGLE = ">".toCharArray();

        @Override
        protected char[] escape(int cp) {
            // Returning null means "do not escape".
            switch (cp) {
            case '\\':
                return DOUBLE_BACKSLASH;
            case '←':
                return LEFT_ANGLE;
            case '→':
                return RIGHT_ANGLE;
            default:
                if (0x0020 <= cp && cp <= 0x007F) {
                    return null;
                } else if (cp <= 0xFFFF) {
                    return String.format("\\u%04X", cp).toCharArray();
                }
                return String.format("\\U%08X", cp).toCharArray();
            }
        }
    };
}
