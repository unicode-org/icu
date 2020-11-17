// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012-2013, Google, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.ListFormatter.FormattedList;
import com.ibm.icu.text.ListFormatter.Type;
import com.ibm.icu.text.ListFormatter.Width;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class ListFormatterTest extends TestFmwk {
    String[] HardcodedTestData = {
            "",
            "A",
            "A and B",
            "A; B, and C",
            "A; B, C, and D",
            "A; B, C, D, and E"
    };

    @Test
    public void TestBasic() {
        ListFormatter formatter = new ListFormatter("{0} and {1}", "{0}; {1}", "{0}, {1}", "{0}, and {1}");
        checkData(formatter, HardcodedTestData);
    }

    String[] EnglishTestData = {
            "",
            "A",
            "A and B",
            "A, B, and C",
            "A, B, C, and D",
            "A, B, C, D, and E"
    };

    @Test
    public void TestEnglish() {
        checkData(ListFormatter.getInstance(ULocale.ENGLISH), EnglishTestData);
        checkData(ListFormatter.getInstance(ULocale.US), EnglishTestData);
        // Redundant tests for code coverage.
        checkData(ListFormatter.getInstance(Locale.ENGLISH), EnglishTestData);
        if (isDefaultLocaleEnglishLike()) {
            checkData(ListFormatter.getInstance(), EnglishTestData);
        }
    }

    // Tests resource loading and inheritance when region sublocale
    // has only partial data for the listPattern element (overriding
    // some of the parent data). #12994
    String[] EnglishGBTestData = {
            "",
            "A",
            "A and B",
            "A, B and C",
            "A, B, C and D",
            "A, B, C, D and E"
    };

    @Test
    public void TestEnglishGB() {
        checkData(ListFormatter.getInstance(new ULocale("en_GB")), EnglishGBTestData);
    }

    // Tests resource loading and inheritance when region sublocale
    // has only partial data for the listPattern element (overriding
    // some of the parent data). #12994
    String[] ChineseTradHKTestData = {
            "",
            "A",
            "A\u53CAB",
            "A\u3001B\u53CAC",
            "A\u3001B\u3001C\u53CAD",
            "A\u3001B\u3001C\u3001D\u53CAE"
    };

    @Test
    public void TestChineseTradHK() {
        checkData(ListFormatter.getInstance(new ULocale("zh_Hant_HK")), ChineseTradHKTestData);
    }

    String[] JapaneseTestData = {
            "",
            "A",
            "A、B",
            "A、B、C",
            "A、B、C、D",
            "A、B、C、D、E"
    };

    @Test
    public void TestJapanese() {
        checkData(ListFormatter.getInstance(ULocale.JAPANESE), JapaneseTestData);
    }

    String[] outOfOrderTestData = {
            "",
            "A",
            "B after A",
            "C in the last after B after the first A",
            "D in the last after C after B after the first A",
            "E in the last after D after C after B after the first A"
    };
    @Test
    public void TestPatternOutOfOrder() {
        ListFormatter formatter = new ListFormatter("{1} after {0}", "{1} after the first {0}", "{1} after {0}",
                                                    "{1} in the last after {0}");
        checkData(formatter, outOfOrderTestData);
    }

    String[] RootTestData = {
            "",
            "A",
            "A, B",
            "A, B, C",
            "A, B, C, D",
            "A, B, C, D, E"
    };

    @Test
    public void TestSpecial() {
        checkData(ListFormatter.getInstance(ULocale.ROOT), RootTestData);
        if (isDefaultLocaleEnglishLike()) {
          checkData(ListFormatter.getInstance(new ULocale("xxx")), EnglishTestData);
        }
    }

    public void checkData(ListFormatter listFormat, String[] strings) {
        assertEquals("0", strings[0], listFormat.format());
        assertEquals("1", strings[1], listFormat.format("A"));
        assertEquals("2", strings[2], listFormat.format("A", "B"));
        assertEquals("3", strings[3], listFormat.format("A", "B", "C"));
        assertEquals("4", strings[4], listFormat.format("A", "B", "C", "D"));
        assertEquals("5", strings[5], listFormat.format("A", "B", "C", "D", "E"));
    }

    @Test
    public void TestFromList() {
        ListFormatter listFormatter = ListFormatter.getInstance(ULocale.ENGLISH);
        ArrayList<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        assertEquals("list", "A, B, and C", listFormatter.format(list));
    }

    @Test
    public void TestCreatePatternForNumItems() {
        ListFormatter listFormatter = ListFormatter.getInstance(ULocale.ENGLISH);
        assertEquals(
                "createPatternForNumItems",
                "{0}, {1}, and {2}",
                listFormatter.getPatternForNumItems(3));
    }

    @Test
    public void TestGetPatternForNumItemsException() {
        ListFormatter listFormatter = ListFormatter.getInstance(ULocale.ENGLISH);
        try {
            listFormatter.getPatternForNumItems(0);
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException expected) {
            // expected.
        }
    }

    @Test
    public void TestGetLocale() {
        assertEquals(
                "getLocale", ULocale.ENGLISH, ListFormatter.getInstance(ULocale.ENGLISH).getLocale());
    }

    @Test
    public void Test9946() {
        ListFormatter listFormatter = ListFormatter.getInstance(ULocale.ENGLISH);
        assertEquals("bug 9946", "{0}, {1}, and {2}", listFormatter.format("{0}", "{1}", "{2}"));
    }


    void DoTheRealListStyleTesting(ULocale locale, String items[], ListFormatter.Style style, String expected) {
        ListFormatter listFormatter = ListFormatter.getInstance(locale, style);
        assertEquals("Style \"" + style + "\"", expected, listFormatter.format((Object[])items));
    }

    @Test
    public void TestDifferentStyles() {
        ULocale locale = ULocale.FRENCH;
        String[] input = { "rouge", "jaune", "bleu", "vert" };

        DoTheRealListStyleTesting(locale, input, ListFormatter.Style.STANDARD, "rouge, jaune, bleu et vert");
        DoTheRealListStyleTesting(locale, input, ListFormatter.Style.OR, "rouge, jaune, bleu ou vert");
        DoTheRealListStyleTesting(locale, input, ListFormatter.Style.UNIT, "rouge, jaune, bleu et vert");
        DoTheRealListStyleTesting(locale, input, ListFormatter.Style.UNIT_NARROW, "rouge jaune bleu vert");
        DoTheRealListStyleTesting(locale, input, ListFormatter.Style.UNIT_SHORT, "rouge, jaune, bleu et vert");
    }

    private boolean isDefaultLocaleEnglishLike() {
        ULocale defaultLocale = ULocale.getDefault(ULocale.Category.FORMAT);
        return defaultLocale.equals(ULocale.ENGLISH) || defaultLocale.equals(ULocale.US);
    }

    @Test
    public void TestFormattedValue() {
        {
            ListFormatter fmt = ListFormatter.getInstance(ULocale.ENGLISH);
            String message = "Field position test 1";
            String expectedString = "hello, wonderful, and world";
            String[] inputs = {
                "hello",
                "wonderful",
                "world"
            };
            FormattedList result = fmt.formatToValue(Arrays.asList(inputs));
            Object[][] expectedFieldPositions = new Object[][] {
                // field, begin index, end index
                {ListFormatter.SpanField.LIST_SPAN, 0, 5, 0},
                {ListFormatter.Field.ELEMENT, 0, 5},
                {ListFormatter.Field.LITERAL, 5, 7},
                {ListFormatter.SpanField.LIST_SPAN, 7, 16, 1},
                {ListFormatter.Field.ELEMENT, 7, 16},
                {ListFormatter.Field.LITERAL, 16, 22},
                {ListFormatter.SpanField.LIST_SPAN, 22, 27, 2},
                {ListFormatter.Field.ELEMENT, 22, 27}};
            FormattedValueTest.checkFormattedValue(
                message,
                result,
                expectedString,
                expectedFieldPositions);
        }

        {
            ListFormatter fmt = ListFormatter.getInstance(ULocale.CHINESE, Type.UNITS, Width.SHORT);
            String message = "Field position test 2 (ICU-21340)";
            String expectedString = "aabbbbbbbccc";
            String inputs[] = {
                "aa",
                "bbbbbbb",
                "ccc"
            };
            FormattedList result = fmt.formatToValue(Arrays.asList(inputs));
            Object[][] expectedFieldPositions = {
                // field, begin index, end index
                {ListFormatter.SpanField.LIST_SPAN, 0, 2, 0},
                {ListFormatter.Field.ELEMENT, 0, 2},
                {ListFormatter.SpanField.LIST_SPAN, 2, 9, 1},
                {ListFormatter.Field.ELEMENT, 2, 9},
                {ListFormatter.SpanField.LIST_SPAN, 9, 12, 2},
                {ListFormatter.Field.ELEMENT, 9, 12}};
            if (!logKnownIssue("21351", "Java still coalesces adjacent elements")) {
                FormattedValueTest.checkFormattedValue(
                    message,
                    result,
                    expectedString,
                    expectedFieldPositions);
            }
        }
    
        {
            ListFormatter fmt = ListFormatter.getInstance(ULocale.ENGLISH, Type.UNITS, Width.SHORT);
            String message = "ICU-21383 Long list";
            String expectedString = "a, b, c, d, e, f, g, h, i";
            String inputs[] = {
                "a",
                "b",
                "c",
                "d",
                "e",
                "f",
                "g",
                "h",
                "i",
            };
            FormattedList result = fmt.formatToValue(Arrays.asList(inputs));
            Object[][] expectedFieldPositions = {
                // field, begin index, end index
                {ListFormatter.SpanField.LIST_SPAN, 0, 1, 0},
                {ListFormatter.Field.ELEMENT, 0, 1},
                {ListFormatter.Field.LITERAL, 1, 3},
                {ListFormatter.SpanField.LIST_SPAN, 3, 4, 1},
                {ListFormatter.Field.ELEMENT, 3, 4},
                {ListFormatter.Field.LITERAL, 4, 6},
                {ListFormatter.SpanField.LIST_SPAN, 6, 7, 2},
                {ListFormatter.Field.ELEMENT, 6, 7},
                {ListFormatter.Field.LITERAL, 7, 9},
                {ListFormatter.SpanField.LIST_SPAN, 9, 10, 3},
                {ListFormatter.Field.ELEMENT, 9, 10},
                {ListFormatter.Field.LITERAL, 10, 12},
                {ListFormatter.SpanField.LIST_SPAN, 12, 13, 4},
                {ListFormatter.Field.ELEMENT, 12, 13},
                {ListFormatter.Field.LITERAL, 13, 15},
                {ListFormatter.SpanField.LIST_SPAN, 15, 16, 5},
                {ListFormatter.Field.ELEMENT, 15, 16},
                {ListFormatter.Field.LITERAL, 16, 18},
                {ListFormatter.SpanField.LIST_SPAN, 18, 19, 6},
                {ListFormatter.Field.ELEMENT, 18, 19},
                {ListFormatter.Field.LITERAL, 19, 21},
                {ListFormatter.SpanField.LIST_SPAN, 21, 22, 7},
                {ListFormatter.Field.ELEMENT, 21, 22},
                {ListFormatter.Field.LITERAL, 22, 24},
                {ListFormatter.SpanField.LIST_SPAN, 24, 25, 8},
                {ListFormatter.Field.ELEMENT, 24, 25},
                };
            FormattedValueTest.checkFormattedValue(
                message,
                result,
                expectedString,
                expectedFieldPositions);
        }
    }

    @Test
    public void TestCreateStyled() {
        // Locale en has interesting data
        Object[][] cases = {
            { "pt", Type.AND, Width.WIDE, "A, B e C" },
            { "pt", Type.AND, Width.SHORT, "A, B e C" },
            { "pt", Type.AND, Width.NARROW, "A, B, C" },
            { "pt", Type.OR, Width.WIDE, "A, B ou C" },
            { "pt", Type.OR, Width.SHORT, "A, B ou C" },
            { "pt", Type.OR, Width.NARROW, "A, B ou C" },
            { "pt", Type.UNITS, Width.WIDE, "A, B e C" },
            { "pt", Type.UNITS, Width.SHORT, "A, B e C" },
            { "pt", Type.UNITS, Width.NARROW, "A B C" },
            { "en", Type.AND, Width.WIDE, "A, B, and C" },
            { "en", Type.AND, Width.SHORT, "A, B, & C" },
            { "en", Type.AND, Width.NARROW, "A, B, C" },
            { "en", Type.OR, Width.WIDE, "A, B, or C" },
            { "en", Type.OR, Width.SHORT, "A, B, or C" },
            { "en", Type.OR, Width.NARROW, "A, B, or C" },
            { "en", Type.UNITS, Width.WIDE, "A, B, C" },
            { "en", Type.UNITS, Width.SHORT, "A, B, C" },
            { "en", Type.UNITS, Width.NARROW, "A B C" },
        };
        for (Object[] cas : cases) {
            Locale loc = new Locale((String) cas[0]);
            ULocale uloc = new ULocale((String) cas[0]);
            Type type = (Type) cas[1];
            Width width = (Width) cas[2];
            String expected = (String) cas[3];
            ListFormatter fmt1 = ListFormatter.getInstance(loc, type, width);
            ListFormatter fmt2 = ListFormatter.getInstance(uloc, type, width);
            String message = "TestCreateStyled loc="
                + loc + " type="
                + type + " width="
                + width;
            String[] inputs = {
                "A",
                "B",
                "C"
            };
            String result = fmt1.format(Arrays.asList(inputs));
            assertEquals(message, expected, result);
            // Coverage for the other factory method overload:
            result = fmt2.format(Arrays.asList(inputs));
            assertEquals(message, expected, result);
        }
    }

    @Test
    public void TestContextual() {
        String [] es = { "es", "es_419", "es_PY", "es_DO" };
        String [] he = { "he", "he_IL", "iw", "iw_IL" };
        Width[] widths = {Width.WIDE, Width.SHORT, Width.NARROW};
        Object[][] cases = {
            { es, Type.AND, "fascinante e incre\u00EDblemente", "fascinante", "incre\u00EDblemente"},
            { es, Type.AND, "Comunicaciones Industriales e IIoT", "Comunicaciones Industriales", "IIoT"},
            { es, Type.AND, "Espa\u00F1a e Italia", "Espa\u00F1a", "Italia"},
            { es, Type.AND, "hijas intr\u00E9pidas e hijos solidarios", "hijas intr\u00E9pidas", "hijos solidarios"},
            { es, Type.AND, "a un hombre e hirieron a otro", "a un hombre", "hirieron a otro"},
            { es, Type.AND, "hija e hijo", "hija", "hijo"},
            { es, Type.AND, "esposa, hija e hijo", "esposa", "hija", "hijo"},
            // For 'y' exception
            { es, Type.AND, "oro y hierro", "oro", "hierro"},
            { es, Type.AND, "agua y hielo", "agua", "hielo"},
            { es, Type.AND, "col\u00E1geno y hialur\u00F3nico", "col\u00E1geno", "hialur\u00F3nico"},

            { es, Type.OR, "desierto u oasis", "desierto", "oasis"},
            { es, Type.OR, "oasis, desierto u océano", "oasis", "desierto", "océano"},
            { es, Type.OR, "7 u 8", "7", "8"},
            { es, Type.OR, "7 u 80", "7", "80"},
            { es, Type.OR, "7 u 800", "7", "800"},
            { es, Type.OR, "6, 7 u 8", "6", "7", "8"},
            { es, Type.OR, "10 u 11", "10", "11"},
            { es, Type.OR, "10 o 111", "10", "111"},
            { es, Type.OR, "10 o 11.2", "10", "11.2"},
            { es, Type.OR, "9, 10 u 11", "9", "10", "11"},

            { he, Type.AND, "a, b \u05D5-c", "a", "b", "c" },
            { he, Type.AND, "a \u05D5-b", "a", "b" },
            { he, Type.AND, "1, 2 \u05D5-3", "1", "2", "3" },
            { he, Type.AND, "1 \u05D5-2", "1", "2" },
            { he, Type.AND, "\u05D0\u05D4\u05D1\u05D4 \u05D5\u05DE\u05E7\u05D5\u05D5\u05D4",
              "\u05D0\u05D4\u05D1\u05D4", "\u05DE\u05E7\u05D5\u05D5\u05D4" },
            { he, Type.AND, "\u05D0\u05D4\u05D1\u05D4, \u05DE\u05E7\u05D5\u05D5\u05D4 \u05D5\u05D0\u05DE\u05D5\u05E0\u05D4",
              "\u05D0\u05D4\u05D1\u05D4", "\u05DE\u05E7\u05D5\u05D5\u05D4", "\u05D0\u05DE\u05D5\u05E0\u05D4" },
        };
        for (Width width : widths) {
            for (Object[] cas : cases) {
                String [] locales = (String[]) cas[0];
                Type type = (Type) cas[1];
                String expected = (String) cas[2];
                for (String locale : locales) {
                    ULocale uloc = new ULocale(locale);
                    List inputs = Arrays.asList(cas).subList(3, cas.length);
                    ListFormatter fmt = ListFormatter.getInstance(uloc, type, width);
                    String message = "TestContextual uloc="
                        + uloc + " type="
                        + type + " width="
                        + width + "data=";
                    for (Object i : inputs) {
                        message += i + ",";
                    }
                    String result = fmt.format(inputs);
                    assertEquals(message, expected, result);
                }
            }
        }
    }
}
