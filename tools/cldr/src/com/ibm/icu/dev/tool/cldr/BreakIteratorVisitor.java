package com.ibm.icu.dev.tool.cldr;

import com.google.common.escape.UnicodeEscaper;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;
import static org.unicode.cldr.api.CldrDataType.LDML;
import static org.unicode.cldr.api.CldrDraftStatus.CONTRIBUTED;
import static org.unicode.cldr.api.CldrDraftStatus.UNCONFIRMED;

/**
 *
 */
// TODO: This class can almost certainly be written using RegexTransformer and a small config.
final class BreakIteratorVisitor {
    // The "type" attribute is not required here, so cannot appear in the matcher.
    private static final PathMatcher SUPPRESSION =
        PathMatcher.of("ldml/segmentations/segmentation/suppressions/suppression");
    private static final AttributeKey SEGMENTATION_TYPE = keyOf("segmentation", "type");

    // Note: This could be done with an intermediate matcher for
    // "ldml/special/icu:breakIteratorData" but there are so few "special" values it's not worth it
    private static final PathMatcher BOUNDARIES =
        PathMatcher.of("ldml/special/icu:breakIteratorData/icu:boundaries/*");
    private static final PathMatcher DICTIONARY =
        PathMatcher.of("ldml/special/icu:breakIteratorData/icu:dictionaries/icu:dictionary");

    private static final AttributeKey DICTIONARY_DEP = keyOf("icu:dictionary", "icu:dependency");
    private static final AttributeKey DICTIONARY_TYPE = keyOf("icu:dictionary", "type");

    /** Processes data from the given supplier to generate day period data. */
    public static List<IcuData> process(CldrDataSupplier supplier, Path specialsDir) {
        List<IcuData> icuData = new ArrayList<>();

        // The choice to use only "CONTRIBUTED" data comes originally from the legacy CldrMapper
        // class, which checked the draft status of the collation element.
        supplier = supplier.withDraftStatus(CONTRIBUTED);
        for (String localeId : supplier.getAvailableLocaleIds()) {
            BreakIteratorVisitor v = new BreakIteratorVisitor(localeId);

            // Add any suppression data for the current locale.
            CldrData cldrData = supplier.getDataForLocale(localeId, UNRESOLVED);
            cldrData.accept(CldrData.PathOrder.DTD, v::addSuppression);

            // And (if present) blend in any additional "special" ICU data in the "icu:" namespace.
            Path specialsXml = specialsDir.resolve(localeId + ".xml");
            if (Files.exists(specialsXml)) {
                CldrData specialData = CldrDataSupplier.forCldrFile(LDML, specialsXml, UNCONFIRMED);
                specialData.accept(CldrData.PathOrder.ARBITRARY, v::addSpecials);
            }

            if (v.hasData()) {
                v.icuData.add("/Version", CldrDataSupplier.getCldrVersionString());
                icuData.add(v.icuData);
            }
        }
        return icuData;
    }

    // The per-locale ICU data being collected by this visitor.
    private final IcuData icuData;

    private BreakIteratorVisitor(String localeId) {
        this.icuData = new IcuData(
            "common/segments/" + localeId + ".xml ../../xml/brkitr/" + localeId + ".xml",
            localeId,
            true);
    }

    private boolean hasData() {
        return !icuData.keySet().isEmpty();
    }

    private void addSuppression(CldrValue v) {
        if (SUPPRESSION.matches(v.getPath())) {
            String type = SEGMENTATION_TYPE.valueFrom(v);
            // Odd that we escape values here, but not for collation data.
            icuData.add("/exceptions/" + type + ":array", ESCAPE_NON_ASCII.escape(v.getValue()));
        }
    }

    private void addSpecials(CldrValue v) {
        // Note that technically the "type" attribute is required here, but since it is part
        // of an external element, we don't have the DTD information avaliable, so it is
        // assumed to be optional. The "icu:dependency" attribute is actually optional, but
        // currently always has a value, so for now complain if either are missing.
        CldrPath p = v.getPath();
        if (BOUNDARIES.matches(p)) {
            addDependency(
                getDependencyName(v), getBoundaryType(v), getBoundaryDependency(v));
        } else if (DICTIONARY.matches(p)) {
            addDependency(
                getDependencyName(v),
                DICTIONARY_TYPE.valueFrom(v),
                DICTIONARY_DEP.optionalValueFrom(v));
        }
    }

    private void addDependency(String name, String type, Optional<String> dependency) {
        icuData.add(
            String.format("/%s/%s:process(dependency)", name, type),
            dependency.orElseThrow(() -> new IllegalArgumentException("missing dependency")));
    }

    // Must match the BOUNDARIES or DICTIONARY path.
    private static String getDependencyName(CldrValue value) {
        return stripXmlNamespace(value.getPath().getParent().getName());
    }

    // Must match the BOUNDARIES path.
    private static String getBoundaryType(CldrValue value) {
        String elementName = value.getPath().getName();
        String type = stripXmlNamespace(elementName);
        return keyOf(elementName, "alt")
            .optionalValueFrom(value).map(a -> type + "_" + a).orElse(type);
    }

    // Must match the BOUNDARIES path.
    private static Optional<String> getBoundaryDependency(CldrValue value) {
        return keyOf(value.getPath().getName(), "icu:dependency").optionalValueFrom(value);
    }

    // Strips the first prefix of the form "xxx:" from a string.
    private static String stripXmlNamespace(String s) {
        return s.substring(s.indexOf(':') + 1);
    }

    /*
     * Convert characters outside the range U+0020 to U+007F to Unicode escapes, and convert
     * backslash to a double backslash. This class is super slow for non-ASCII escaping due to
     * using "String.format()", however there's < 100 values that need any escaping, so it's fine.
     * Don't copy this code for use with millions of values (have reusable char[] for that).
     */
    private static final UnicodeEscaper ESCAPE_NON_ASCII = new UnicodeEscaper() {
        private final char[] DOUBLE_BACKSLASH = "\\\\".toCharArray();

        @Override
        protected char[] escape(int cp) {
            // Returning null means "do not escape".
            if (0x0020 <= cp && cp <= 0x007F) {
                return cp == '\\' ? DOUBLE_BACKSLASH : null;
            } else if (cp <= 0xFFFF) {
                return String.format("\\u%04X", cp).toCharArray();
            }
            return String.format("\\U%08X", cp).toCharArray();
        }
    };
}
