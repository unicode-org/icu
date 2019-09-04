// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData.ValueVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.ibm.icu.text.Transliterator;

/**
 * A mapper to collect transliteration data from {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL}
 * data via the paths:
 * <pre>{@code
 *   //supplementalData/transforms/transform/tRule
 * }</pre>
 *
 * <p>This mapper also writes out the transform rule files into a specified directory.
 */
public final class TransformsMapper {
    private static final PathMatcher TRULE =
        PathMatcher.of("supplementalData/transforms/transform/tRule");
    private static final AttributeKey TRANSFORM_SOURCE = keyOf("transform", "source");
    private static final AttributeKey TRANSFORM_TARGET = keyOf("transform", "target");
    private static final AttributeKey TRANSFORM_DIRECTION = keyOf("transform", "direction");
    private static final AttributeKey TRANSFORM_VARIANT = keyOf("transform", "variant");
    private static final AttributeKey TRANSFORM_VISIBILITY = keyOf("transform", "visibility");
    private static final AttributeKey TRANSFORM_ALIAS = keyOf("transform", "alias");
    private static final AttributeKey TRANSFORM_BACKALIAS = keyOf("transform", "backwardAlias");

    private static final RbPath RB_TRANSLITERATOR_IDS = RbPath.of("RuleBasedTransliteratorIDs");

    // This decomposes some accented characters with accents in the "Mn" (Mark, non-spacing)
    // Unicode range by representing the accents in the \u1234 hex form. For example, it converts:
    // "ɪ̈" to "ɪ\u0308" and "ɯ̽" to "ɯ\u033D". This does not affect all accented character (e.g.
    // ä) and the precise reason this is done was never clearly documented in the code from which
    // this code was derived (but it seems necessary to generate the expected output in the
    // transliteration rules).
    //
    // This is one of the only, apparently necessary direct dependencies on the icu4j library.
    // TODO: Make this depend icu4j from this project rather than the older version from CLDR.
    private static final Transliterator FIXUP = Transliterator.getInstance("[:Mn:]any-hex/java");

    // Don't rename these enum constants, they need to match the data directly.
    private enum Direction { forward, backward, both }
    private enum Visibility { internal, external }

    /**
     * Processes data from the given supplier to generate transliteration ICU data, writing
     * auxiliary transliteration rule files in the process. This is a potentially destructive call
     * and will overwrite existing transformation rule files in the specified directory.
     *
     * @param src the CLDR data supplier to process.
     * @param ruleFileOutputDir the directory into which transliteration rule files will be written.
     * @return the IcuData instance to be written to a file.
     */
    public static IcuData process(CldrDataSupplier src, Path ruleFileOutputDir) {
        RuleVisitor visitor = new RuleVisitor(p -> {
            Path file = ruleFileOutputDir.resolve(p);
            try {
                return new PrintWriter(Files.newBufferedWriter(file, CREATE, TRUNCATE_EXISTING));
            } catch (IOException e) {
                throw new RuntimeException("error opening file: " + file, e);
            }
        });
        src.getDataForType(SUPPLEMENTAL).accept(DTD, visitor);
        return visitor.icuData;
    }

    private static class RuleVisitor implements ValueVisitor {
        private final IcuData icuData = new IcuData("root", false);
        private final Function<Path, PrintWriter> outFn;

        RuleVisitor(Function<Path, PrintWriter> outFn) {
            this.outFn = checkNotNull(outFn);
            icuData.setFileComment("File: root.txt");

            // I have _no_ idea what any of this is about, I'm just trying to mimic the original
            // (complex and undocumented) code in "ConvertTransforms.java".
            // TODO: Understand and document each of the cases below.
            icuData.add(RbPath.of("TransliteratorNamePattern"), "{0,choice,0#|1#{1}|2#{1}-{2}}");
            // Note that this quoting of path segments is almost certainly unnecessary. It matches
            // the old "ConvertTransforms" behaviour, but '%' is used elsewhere without quoting, so
            // it seems very likely that it's not needed here.
            // TODO: Once migration done, remove quotes here & check in RbPath for unwanted quotes.
            icuData.add(RbPath.of("\"%Translit%Hex\""), "%Translit%Hex");
            icuData.add(RbPath.of("\"%Translit%UnicodeName\""), "%Translit%UnicodeName");
            icuData.add(RbPath.of("\"%Translit%UnicodeChar\""), "%Translit%UnicodeChar");
            // Special case, where Latin is a no-op.
            icuData.add(RbPath.of("TransliterateLATIN"), RbValue.of("", ""));
            // Some hard-coded special case mappings.
            icuData.add(
                RB_TRANSLITERATOR_IDS.extendBy("Tone-Digit", "alias"),
                "Pinyin-NumericPinyin");
            icuData.add(
                RB_TRANSLITERATOR_IDS.extendBy("Digit-Tone", "alias"),
                "NumericPinyin-Pinyin");
        }

        @Override public void visit(CldrValue value) {
            // The other possible element is "comment" but we currently ignore those.
            if (TRULE.matches(value.getPath())) {
                String source = getExpectedOptionalAttribute(value, TRANSFORM_SOURCE);
                String target = getExpectedOptionalAttribute(value, TRANSFORM_TARGET);
                Optional<String> variant = TRANSFORM_VARIANT.optionalValueFrom(value);
                String baseFilename = source + "_" + target;
                String filename =
                    variant.map(v -> baseFilename + "_" + v).orElse(baseFilename) + ".txt";
                writeRootIndexEntry(value, source, target, variant, filename);
                writeDataFile(filename, value);
            }
        }

        private void writeDataFile(String filename, CldrValue value) {
            try (PrintWriter out = outFn.apply(Paths.get(filename))) {
                out.println("\uFEFF# © 2016 and later: Unicode, Inc. and others.");
                out.println("# License & terms of use: http://www.unicode.org/copyright.html#License");
                out.println("#");
                out.println("# File: " + filename);
                out.println("# Generated from CLDR");
                out.println("#");
                out.println();
                out.println(FIXUP.transliterate(whitespace().trimFrom(value.getValue())));
                out.println();
            }
        }

        private void writeRootIndexEntry(
            CldrValue value, String source, String target, Optional<String> variant, String filename) {
            Visibility visibility = TRANSFORM_VISIBILITY.valueFrom(value, Visibility.class);
            String status = visibility == Visibility.internal ? "internal" : "file";

            Direction dir = TRANSFORM_DIRECTION.valueFrom(value, Direction.class);
            if (dir != Direction.backward) {
                String id = getId(source, target, variant);
                TRANSFORM_ALIAS.listOfValuesFrom(value)
                    .forEach(a -> icuData.add(RB_TRANSLITERATOR_IDS.extendBy(a, "alias"), id));
                RbPath rbPrefix = RB_TRANSLITERATOR_IDS.extendBy(id, status);
                icuData.add(rbPrefix.extendBy("resource:process(transliterator)"), filename);
                icuData.add(rbPrefix.extendBy("direction"), "FORWARD");
            }
            if (dir != Direction.forward) {
                String id = getId(target, source, variant);
                TRANSFORM_BACKALIAS.listOfValuesFrom(value)
                    .forEach(a -> icuData.add(RB_TRANSLITERATOR_IDS.extendBy(a, "alias"), id));
                RbPath rbPrefix = RB_TRANSLITERATOR_IDS.extendBy(id, status);
                icuData.add(rbPrefix.extendBy("resource:process(transliterator)"), filename);
                icuData.add(rbPrefix.extendBy("direction"), "REVERSE");
            }
        }
    }

    private static String getId(String from, String to, Optional<String> variant) {
        String baseId = from + "-" + to;
        return variant.map(v -> baseId + "/" + v).orElse(baseId);
    }

    private static String getExpectedOptionalAttribute(CldrValue value, AttributeKey key) {
        return key.optionalValueFrom(value).orElseThrow(() ->
            new IllegalArgumentException(String.format("missing data for %s in: %s", key, value)));
    }
}
