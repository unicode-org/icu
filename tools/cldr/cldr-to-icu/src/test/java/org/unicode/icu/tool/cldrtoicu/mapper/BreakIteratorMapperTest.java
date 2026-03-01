// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.icu.tool.cldrtoicu.mapper.BreakIteratorMapperTest.BoundaryType.GRAPHEME;
import static org.unicode.icu.tool.cldrtoicu.mapper.BreakIteratorMapperTest.BoundaryType.SENTENCE;
import static org.unicode.icu.tool.cldrtoicu.mapper.BreakIteratorMapperTest.SegmentationType.LINE_BREAK;
import static org.unicode.icu.tool.cldrtoicu.mapper.BreakIteratorMapperTest.SegmentationType.SENTENCE_BREAK;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;

@RunWith(JUnit4.class)
public class BreakIteratorMapperTest {
    enum SegmentationType {
        GRAPHEME_CLUSTER_BREAK, LINE_BREAK, SENTENCE_BREAK, WORD_BREAK;

        @Override public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
        }
    }

    enum BoundaryType {
        GRAPHEME, WORD, LINE, SENTENCE, TITLE;

        // E.g. "icu:grapheme"
        @Override public String toString() {
            return "icu:" + Ascii.toLowerCase(name());
        }
    }

    @Test
    public void testSingleSuppression() {
        int idx = 0;
        CldrData cldrData = cldrData(
            suppression(SENTENCE_BREAK, "L.P.", ++idx),
            suppression(SENTENCE_BREAK, "Alt.", ++idx),
            suppression(SENTENCE_BREAK, "Approx.", ++idx));

        IcuData icuData = new IcuData("xx", true);
        BreakIteratorMapper.process(icuData, cldrData, Optional.empty());

        assertThat(icuData).getPaths().hasSize(1);
        assertThat(icuData).hasValuesFor("/exceptions/SentenceBreak:array",
            RbValue.of("L.P."),
            RbValue.of("Alt."),
            RbValue.of("Approx."));
    }

    // In real data, suppression is only a SentenceBreak thing, but we might as well test it for
    // other types.
    @Test
    public void testMultipleSupressionTypes() {
        int idx = 0;
        CldrData cldrData = cldrData(
            suppression(SENTENCE_BREAK, "L.P.", ++idx),
            suppression(SENTENCE_BREAK, "Alt.", ++idx),
            suppression(SENTENCE_BREAK, "Approx.", ++idx),
            suppression(LINE_BREAK, "Foo", ++idx),
            suppression(LINE_BREAK, "Bar", ++idx),
            suppression(LINE_BREAK, "Baz", ++idx));

        IcuData icuData = new IcuData("xx", true);
        BreakIteratorMapper.process(icuData, cldrData, Optional.empty());

        assertThat(icuData).getPaths().hasSize(2);
        assertThat(icuData).hasValuesFor("/exceptions/SentenceBreak:array",
            RbValue.of("L.P."),
            RbValue.of("Alt."),
            RbValue.of("Approx."));
        assertThat(icuData).hasValuesFor("/exceptions/LineBreak:array",
            RbValue.of("Foo"),
            RbValue.of("Bar"),
            RbValue.of("Baz"));
    }

    @Test
    public void testSpecials_dictionary() {
        CldrData specials = cldrData(
            dictionary("foo", "<foo deps>"),
            dictionary("bar", "<bar deps>"));

        IcuData icuData = new IcuData("xx", true);
        BreakIteratorMapper.process(icuData, cldrData(), Optional.of(specials));

        assertThat(icuData).getPaths().hasSize(2);
        assertThat(icuData).hasValuesFor("/dictionaries/foo:process(dependency)", "<foo deps>");
        assertThat(icuData).hasValuesFor("/dictionaries/bar:process(dependency)", "<bar deps>");
    }

    @Test
    public void testSpecials_boundaries() {
        CldrData specials = cldrData(
            boundaries(GRAPHEME, "<grapheme deps>", null),
            boundaries(SENTENCE, "<sentence deps>", "altName"));

        IcuData icuData = new IcuData("xx", true);
        BreakIteratorMapper.process(icuData, cldrData(), Optional.of(specials));

        assertThat(icuData).getPaths().hasSize(2);
        assertThat(icuData)
            .hasValuesFor("/boundaries/grapheme:process(dependency)", "<grapheme deps>");
        assertThat(icuData)
            .hasValuesFor("/boundaries/sentence_altName:process(dependency)", "<sentence deps>");
    }

    private static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(Arrays.asList(values));
    }

    private static CldrValue suppression(SegmentationType type, String value, int index) {
        StringBuilder cldrPath = new StringBuilder("//ldml/segmentations");
        appendAttribute(cldrPath.append("/segmentation"), "type", type);
        cldrPath.append("/suppressions[@type=\"standard\"]");
        // Suppression is an ordered element, so needs a sort index.
        cldrPath.append("/suppression#").append(index);
        return CldrValue.parseValue(cldrPath.toString(), value);
    }

    private static CldrValue dictionary(String type, String dependency) {
        StringBuilder cldrPath = new StringBuilder("//ldml/special/icu:breakIteratorData");
        cldrPath.append("/icu:dictionaries/icu:dictionary");
        appendAttribute(cldrPath, "type", type);
        appendAttribute(cldrPath, "icu:dependency", dependency);
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static CldrValue boundaries(BoundaryType type, String dependency, String alt) {
        StringBuilder cldrPath = new StringBuilder("//ldml/special/icu:breakIteratorData");
        cldrPath.append("/icu:boundaries/").append(type);
        appendAttribute(cldrPath, "icu:dependency", dependency);
        if (alt != null) {
            appendAttribute(cldrPath, "alt", alt);
        }
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }
}