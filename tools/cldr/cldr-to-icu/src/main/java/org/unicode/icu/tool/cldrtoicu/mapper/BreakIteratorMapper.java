// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.unicode.cldr.api.AttributeKey.keyOf;

import java.util.Optional;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor;
import org.unicode.icu.tool.cldrtoicu.CldrDataProcessor.SubProcessor;

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

    private static final CldrDataProcessor<BreakIteratorMapper> CLDR_PROCESSOR;
    static {
        CldrDataProcessor.Builder<BreakIteratorMapper> processor = CldrDataProcessor.builder();
        // The "type" attribute in /suppressions/ is not required so cannot be in the matcher. And
        // its default (and only) value is "standard".
        // TODO: Understand and document why this is the case.
        processor.addValueAction(
            "//ldml/segmentations/segmentation[@type=*]/suppressions/suppression",
            BreakIteratorMapper::addSuppression);
        SubProcessor<BreakIteratorMapper> specials =
            processor.addSubprocessor("//ldml/special/icu:breakIteratorData");
        specials.addValueAction("icu:boundaries/*", BreakIteratorMapper::addBoundary);
        specials.addValueAction(
            "icu:dictionaries/icu:dictionary", BreakIteratorMapper::addDictionary);
        specials.addValueAction(
            "icu:extensions/icu:extension", BreakIteratorMapper::addExtension);
        specials.addValueAction(
            "icu:lstm/icu:lstmdata", BreakIteratorMapper::addLstmdata);
        CLDR_PROCESSOR = processor.build();
    }

    private static final AttributeKey SEGMENTATION_TYPE = keyOf("segmentation", "type");
    private static final AttributeKey DICTIONARY_DEP = keyOf("icu:dictionary", "icu:dependency");
    private static final AttributeKey DICTIONARY_TYPE = keyOf("icu:dictionary", "type");
    private static final AttributeKey LSTMDATA_DEP = keyOf("icu:lstmdata", "icu:dependency");
    private static final AttributeKey LSTMDATA_TYPE = keyOf("icu:lstmdata", "type");

    /**
     * Processes data from the given supplier to generate break-iterator data for a set of locale
     * IDs.
     *
     * @param icuData the ICU data to be filled.
     * @param cldrData the unresolved CLDR data to process.
     * @param icuSpecialData additional ICU data (in the "icu:" namespace)
     * @return IcuData containing break-iterator data for the given locale ID.
     */
    public static IcuData process(
        IcuData icuData, CldrData cldrData, Optional<CldrData> icuSpecialData) {

        BreakIteratorMapper mapper = new BreakIteratorMapper(icuData);
        icuSpecialData.ifPresent(d -> CLDR_PROCESSOR.process(d, mapper));
        CLDR_PROCESSOR.process(cldrData, mapper);
        return mapper.icuData;
    }

    // The per-locale ICU data being collected by this visitor.
    private final IcuData icuData;

    private BreakIteratorMapper(IcuData icuData) {
        this.icuData = checkNotNull(icuData);
    }

    private void addSuppression(CldrValue v) {
        //System.out.println("addSuppression: " + v.toString()); // debug
        String type = SEGMENTATION_TYPE.valueFrom(v);
        // TODO: Understand and document why we escape values here, but not for collation data.
        icuData.add(
            RbPath.of("exceptions", type + ":array"), ESCAPE_NON_ASCII.escape(v.getValue()));
    }

    private void addBoundary(CldrValue v) {
        //System.out.println("addBoundary: " + v.toString()); // debug
        addDependency(getDependencyName(v), getBoundaryType(v), getBoundaryDependency(v));
    }

    private void addDictionary(CldrValue v) {
        //System.out.println("addDictionary: " + v.toString()); // debug
        addDependency(
            getDependencyName(v),
            DICTIONARY_TYPE.valueFrom(v),
            DICTIONARY_DEP.optionalValueFrom(v));
    }

    private void addExtension(CldrValue v) {
        //System.out.println("addExtension: " + v.toString()); // debug
        icuData.add(
            RbPath.of("extensions"), v.getValue());
    }

    private void addLstmdata(CldrValue v) {
        //System.out.println("addLstmdata: " + v.toString()); // debug
        addDependency(
            getDependencyName(v),
            LSTMDATA_TYPE.valueFrom(v),
            LSTMDATA_DEP.optionalValueFrom(v));
    }

    private void addDependency(String name, String type, Optional<String> dependency) {
        //System.out.println("addDependency: name " + name + ", type " + type + ", dependency " + dependency);
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
