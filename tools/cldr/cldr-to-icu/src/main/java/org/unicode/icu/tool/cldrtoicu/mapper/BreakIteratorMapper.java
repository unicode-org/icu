// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;

import java.util.Optional;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.escape.UnicodeEscaper;

/**
 * A mapper to collect break-iterator data from {@link CldrDataType#LDML LDML} data under
 * paths matching:
 * <pre>{@code
 *   //ldml/segmentations/segmentation/suppressions/suppression
 *   //ldml/special/icu:breakIteratorData/...
 * }</pre>
 */
// TODO: This class can almost certainly be replace with a small RegexTransformer config.
public final class BreakIteratorMapper {
    // The "type" attribute in /suppressions/ is not required so cannot be in the matcher. And
    // its default (and only) value is "standard".
    // TODO: Understand and document why this is the case.
    private static final PathMatcher SUPPRESSION = PathMatcher.of(
        "ldml/segmentations/segmentation[@type=*]/suppressions/suppression");
    private static final AttributeKey SEGMENTATION_TYPE = keyOf("segmentation", "type");

    // Note: This could be done with an intermediate matcher for
    // "ldml/special/icu:breakIteratorData" but there are so few "special" values it's not worth it
    private static final PathMatcher BOUNDARIES =
        PathMatcher.of("ldml/special/icu:breakIteratorData/icu:boundaries/*");
    private static final PathMatcher DICTIONARY =
        PathMatcher.of("ldml/special/icu:breakIteratorData/icu:dictionaries/icu:dictionary");

    private static final AttributeKey DICTIONARY_DEP = keyOf("icu:dictionary", "icu:dependency");
    private static final AttributeKey DICTIONARY_TYPE = keyOf("icu:dictionary", "type");

    /**
     * Processes data from the given supplier to generate break-iterator data for a set of locale
     * IDs.
     *
     * @param localeId the locale ID to generate data for.
     * @param src the CLDR data supplier to process.
     * @param icuSpecialData additional ICU data (in the "icu:" namespace)
     * @return IcuData containing break-iterator data for the given locale ID.
     */
    public static IcuData process(
        String localeId, CldrDataSupplier src, Optional<CldrData> icuSpecialData) {

        CldrData cldrData = src.getDataForLocale(localeId, UNRESOLVED);
        return process(localeId, cldrData, icuSpecialData);
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static IcuData process(String localeId, CldrData cldrData, Optional<CldrData> icuSpecialData) {
        BreakIteratorMapper mapper = new BreakIteratorMapper(localeId);
        icuSpecialData.ifPresent(s -> s.accept(DTD, mapper::addSpecials));
        cldrData.accept(DTD, mapper::addSuppression);
        return mapper.icuData;
    }

    // The per-locale ICU data being collected by this visitor.
    private final IcuData icuData;

    private BreakIteratorMapper(String localeId) {
        this.icuData = new IcuData(localeId, true);
    }

    private void addSuppression(CldrValue v) {
        if (SUPPRESSION.matches(v.getPath())) {
            String type = SEGMENTATION_TYPE.valueFrom(v);
            // TODO: Understand and document why we escape values here, but not for collation data.
            icuData.add(
                RbPath.of("exceptions", type + ":array"),
                ESCAPE_NON_ASCII.escape(v.getValue()));
        }
    }

    private void addSpecials(CldrValue v) {
        CldrPath p = v.getPath();
        if (BOUNDARIES.matches(p)) {
            addDependency(
                getDependencyName(v),
                getBoundaryType(v),
                getBoundaryDependency(v));
        } else if (DICTIONARY.matches(p)) {
            addDependency(
                getDependencyName(v),
                DICTIONARY_TYPE.valueFrom(v),
                DICTIONARY_DEP.optionalValueFrom(v));
        }
    }

    private void addDependency(String name, String type, Optional<String> dependency) {
        icuData.add(
            RbPath.of(name, type + ":process(dependency)"),
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
