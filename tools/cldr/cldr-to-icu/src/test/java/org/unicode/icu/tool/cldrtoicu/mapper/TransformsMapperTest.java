// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;
import static org.unicode.icu.tool.cldrtoicu.mapper.TransformsMapperTest.Direction.BACKWARD;
import static org.unicode.icu.tool.cldrtoicu.mapper.TransformsMapperTest.Direction.BOTH;
import static org.unicode.icu.tool.cldrtoicu.mapper.TransformsMapperTest.Direction.FORWARD;
import static org.unicode.icu.tool.cldrtoicu.mapper.TransformsMapperTest.Visibility.EXTERNAL;
import static org.unicode.icu.tool.cldrtoicu.mapper.TransformsMapperTest.Visibility.INTERNAL;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;

@RunWith(JUnit4.class)
public class TransformsMapperTest {

    private static final ImmutableList<String> HEADER_LINES = ImmutableList.of(
        "First header line",
        "Second header line");

    private static final String FILE_HEADER =
        "\uFEFF# First header line\n"
            + "# Second header line\n"
            + "#\n";

    private static final int DEFAULT_PATH_COUNT = 7;

    enum Direction {
        FORWARD, BACKWARD, BOTH;

        @Override public String toString() {
            return Ascii.toLowerCase(name());
        }
    }

    enum Visibility {
        INTERNAL, EXTERNAL;

        @Override public String toString() {
            return Ascii.toLowerCase(name());
        }
    }

    @Test
    public void testDefaultContent() {
        Map<String, String> fileMap = new TreeMap<>();
        IcuData icuData = TransformsMapper.process(cldrData(), wrap(fileMap), HEADER_LINES);

        assertThat(fileMap).isEmpty();

        assertThat(icuData).getPaths().hasSize(DEFAULT_PATH_COUNT);
        assertThat(icuData).hasValuesFor("/\"%Translit%Hex\"", "%Translit%Hex");
        assertThat(icuData).hasValuesFor("/\"%Translit%UnicodeChar\"", "%Translit%UnicodeChar");
        assertThat(icuData).hasValuesFor("/\"%Translit%UnicodeName\"", "%Translit%UnicodeName");
        assertThat(icuData)
            .hasValuesFor("/RuleBasedTransliteratorIDs/Digit-Tone/alias", "NumericPinyin-Pinyin");
        assertThat(icuData)
            .hasValuesFor("/RuleBasedTransliteratorIDs/Tone-Digit/alias", "Pinyin-NumericPinyin");
        assertThat(icuData).hasValuesFor("TransliterateLATIN", "", "");
        assertThat(icuData)
            .hasValuesFor("TransliteratorNamePattern", "{0,choice,0#|1#{1}|2#{1}-{2}}");
    }

    @Test
    public void testForward() {
        int idx = 0;
        CldrData cldrData =
            cldrData(oneWay("foo", "bar", FORWARD, null, INTERNAL, "first second third", ++idx));

        Map<String, String> fileMap = new TreeMap<>();
        IcuData icuData = TransformsMapper.process(cldrData, wrap(fileMap), HEADER_LINES);

        assertThat(icuData).getPaths().hasSize(DEFAULT_PATH_COUNT + 5);
        assertThat(icuData).hasValuesFor("RuleBasedTransliteratorIDs/first/alias", "foo-bar");
        assertThat(icuData).hasValuesFor("RuleBasedTransliteratorIDs/second/alias", "foo-bar");
        assertThat(icuData).hasValuesFor("RuleBasedTransliteratorIDs/third/alias", "foo-bar");
        assertThat(icuData)
            .hasValuesFor("RuleBasedTransliteratorIDs/foo-bar/internal/direction", "FORWARD");
        assertThat(icuData)
            .hasValuesFor(
                "RuleBasedTransliteratorIDs/foo-bar/internal/resource:process(transliterator)",
                "foo_bar.txt");

        assertThat(fileMap).hasSize(1);
        assertThat(fileMap).containsEntry("foo_bar.txt", headerPlusLines(
            "# File: foo_bar.txt",
            "# Generated from CLDR",
            "#",
            "",
            "foo --> bar [internal]:",
            "first second third"));
    }

    @Test
    public void testBackward() {
        int idx = 0;
        CldrData cldrData =
            cldrData(oneWay("foo", "bar", BACKWARD, "variant", EXTERNAL, "one two three", ++idx));

        Map<String, String> fileMap = new TreeMap<>();
        IcuData icuData = TransformsMapper.process(cldrData, wrap(fileMap), HEADER_LINES);

        assertThat(icuData).getPaths().hasSize(DEFAULT_PATH_COUNT + 5);
        assertThat(icuData).hasValuesFor("RuleBasedTransliteratorIDs/one/alias", "bar-foo/variant");
        assertThat(icuData).hasValuesFor("RuleBasedTransliteratorIDs/two/alias", "bar-foo/variant");
        assertThat(icuData).hasValuesFor("RuleBasedTransliteratorIDs/three/alias", "bar-foo/variant");

        // Since the variant uses a '/' in the path element (not a path separator) we cannot just
        // parse a string to get the expected path, so we do it the "hard way".
        RbPath prefix = RbPath.of("RuleBasedTransliteratorIDs", "bar-foo/variant", "file");
        assertThat(icuData).hasValuesFor(prefix.extendBy("direction"), "REVERSE");
        assertThat(icuData)
            .hasValuesFor(prefix.extendBy("resource:process(transliterator)"), "foo_bar_variant.txt");

        assertThat(fileMap).hasSize(1);
        assertThat(fileMap).containsEntry("foo_bar_variant.txt", headerPlusLines(
            "# File: foo_bar_variant.txt",
            "# Generated from CLDR",
            "#",
            "",
            "foo <-- bar [external]:",
            "one two three"));
    }

    @Test
    public void testBoth() {
        int idx = 0;
        CldrData cldrData = cldrData(
            both("foo", "bar", null, INTERNAL, "forward-alias", "backward-alias", ++idx));

        Map<String, String> fileMap = new TreeMap<>();
        IcuData icuData = TransformsMapper.process(cldrData, wrap(fileMap), HEADER_LINES);

        // 3 for each direction.
        assertThat(icuData).getPaths().hasSize(DEFAULT_PATH_COUNT + 6);

        // Both directions.
        assertThat(icuData)
            .hasValuesFor("RuleBasedTransliteratorIDs/foo-bar/internal/direction", "FORWARD");
        assertThat(icuData)
            .hasValuesFor("RuleBasedTransliteratorIDs/bar-foo/internal/direction", "REVERSE");

        // Both aliases.
        assertThat(icuData)
            .hasValuesFor("RuleBasedTransliteratorIDs/forward-alias/alias", "foo-bar");
        assertThat(icuData)
            .hasValuesFor("RuleBasedTransliteratorIDs/backward-alias/alias", "bar-foo");

        // But the file is the same (obvious really since there's only one).
        assertThat(icuData).hasValuesFor(
            "RuleBasedTransliteratorIDs/foo-bar/internal/resource:process(transliterator)",
            "foo_bar.txt");
        assertThat(icuData).hasValuesFor(
            "RuleBasedTransliteratorIDs/bar-foo/internal/resource:process(transliterator)",
            "foo_bar.txt");

        assertThat(fileMap).hasSize(1);
        assertThat(fileMap).containsEntry("foo_bar.txt", headerPlusLines(
            "# File: foo_bar.txt",
            "# Generated from CLDR",
            "#",
            "",
            "foo <-> bar [internal]:",
            "forward-alias",
            "backward-alias"));
    }

    private String headerPlusLines(String... lines) {
        // For now the files always contain a blank line at the end (to match legacy behaviour) but
        // this can, and probably should be changed.
        return Arrays.stream(lines).collect(joining("\n", FILE_HEADER, "\n\n"));
    }

    private static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(Arrays.asList(values));
    }

    private static CldrValue oneWay(
        String src, String dst, Direction dir, String var, Visibility vis, String alias, int idx) {

        checkArgument(dir != BOTH, "use both() for bidirectional transforms");
        StringBuilder cldrPath = new StringBuilder("//supplementalData/transforms/transform");
        appendAttribute(cldrPath, "source", src);
        appendAttribute(cldrPath, "target", dst);
        appendAttribute(cldrPath, "direction", dir);
        if (var != null) {
            appendAttribute(cldrPath, "variant", var);
        }
        appendAttribute(cldrPath, "visibility", vis);
        appendAttribute(cldrPath, dir == FORWARD ? "alias" : "backwardAlias", alias);
        cldrPath.append("/tRule#").append(idx);

        String arrow = dir == FORWARD ? "-->" : "<--";
        return CldrValue.parseValue(
            cldrPath.toString(),
            String.format("%s %s %s [%s]:\n%s", src, arrow, dst, vis, alias));
    }

    private static CldrValue both(
        String src, String dst, String var, Visibility vis, String alias, String backAlias, int idx) {

        StringBuilder cldrPath = new StringBuilder("//supplementalData/transforms/transform");
        appendAttribute(cldrPath, "source", src);
        appendAttribute(cldrPath, "target", dst);
        appendAttribute(cldrPath, "direction", BOTH);
        if (var != null) {
            appendAttribute(cldrPath, "variant", var);
        }
        appendAttribute(cldrPath, "visibility", vis);
        appendAttribute(cldrPath, "alias", alias);
        appendAttribute(cldrPath, "backwardAlias", backAlias);
        cldrPath.append("/tRule#").append(idx);

        return CldrValue.parseValue(
            cldrPath.toString(),
            String.format("%s <-> %s [%s]:\n%s\n%s", src, dst, vis, alias, backAlias));
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }

    private static Function<Path, PrintWriter> wrap(Map<String, String> data) {
        return path -> {
            Writer writer = new Writer() {
                StringWriter buffer = new StringWriter();
                @Override public void write(char[] chars, int offset, int length) {
                    buffer.write(chars, offset, length);
                }

                @Override public void flush() {
                    buffer.flush();
                }

                @Override public void close() throws IOException {
                    buffer.close();
                    data.put(path.toString(), buffer.toString());
                }
            };
            return new PrintWriter(writer);
        };
    }
}