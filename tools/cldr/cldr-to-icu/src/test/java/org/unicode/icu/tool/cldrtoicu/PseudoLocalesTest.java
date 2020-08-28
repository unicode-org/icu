// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.truth.Truth.assertThat;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.testing.FakeDataSupplier;

@RunWith(JUnit4.class)
public class PseudoLocalesTest {
    @Test
    public void testExpansion() {
        // Target and source values.
        CldrPath included =
            ldmlPath("localeDisplayNames/languages/language[@type=\"xx\"]");
        CldrPath excluded =
            ldmlPath("localeDisplayNames/localeDisplayPattern/localePattern[@alt=\"testing\"]");
        CldrPath pattern =
            ldmlPath("dates/timeZoneNames/hourFormat[@alt=\"testing\"]");
        CldrPath narrow =
            ldmlPath("dates/fields/field[@type=\"sun-narrow\"]/relative[@type=\"0\"]");
        CldrPath inherited =
            ldmlPath("dates/timeZoneNames/zone[@type=\"Etc/UTC\"]/short/standard");

        FakeDataSupplier src = new FakeDataSupplier()
            .addLocaleData("en",
                value(included, "{Hello} {0} {World} 100x"),
                value(excluded, "Skipped"),
                value(pattern, "'plus' HH:mm; 'minus' HH:mm"),
                value(narrow, "Skipped"))
            .addLocaleData("en_001", value(inherited, "UTC"))
            .setLocaleParent("en", "en_001")
            // Root is the eventual parent of everything but its value should not appear, even
            // though the expansion would apply if the paths were overridden.
            .addLocaleData("root", value(ldmlPath("delimiters/quotationStart"), "“"))
            .addLocaleData("root", value(ldmlPath("delimiters/quotationEnd"), "”"));

        CldrDataSupplier pseudo = PseudoLocales.addPseudoLocalesTo(src);
        assertThat(pseudo.getAvailableLocaleIds()).containsAtLeast("en_XA", "ar_XB");

        // The pseudo locale should combine both explicit and inherited data from 'en'.
        CldrData unresolved = pseudo.getDataForLocale("en_XA", UNRESOLVED);

        assertValuesUnordered(unresolved,
            // Note how {n} placeholders are not affected, but digits elsewhere are.
            value(included, "[{Ĥéļļö} {0} {Ŵöŕļð} ①⓪⓪ẋ one two three]"),
            // Note the quoting of any padding added to a pattern string.
            value(pattern, "['þļûš' HH:mm; 'ɱîñûš' HH:mm 'one' 'two' 'three' 'four']"),
            // Value obtained from the resolved "en" data is here in unresolved data.
            value(inherited, "[ÛŢÇ one]"));
    }

    // This tests behaviour expected by Android (previously patched in earlier ICU versions).
    // https://android-review.googlesource.com/c/platform/external/cldr/+/689949
    // In particular the use of "ALM" (U+061c) rather than "RLM" (U+200F) as the BiDi marker.
    @Test
    public void testBidi() {
        // Target and source values (same as above but not including the skipped paths).
        CldrPath included =
            ldmlPath("localeDisplayNames/languages/language[@type=\"xx\"]");
        CldrPath pattern =
            ldmlPath("dates/timeZoneNames/hourFormat[@alt=\"testing\"]");
        CldrPath inherited =
            ldmlPath("dates/timeZoneNames/zone[@type=\"Etc/UTC\"]/short/standard");

        FakeDataSupplier src = new FakeDataSupplier()
            .addLocaleData("en",
                value(included, "{Hello} {0} {World} 100x"),
                value(pattern, "'plus' HH:mm; 'minus' HH:mm"))
            .addLocaleData("en_001", value(inherited, "UTC"))
            .setLocaleParent("en", "en_001");

        CldrDataSupplier pseudo = PseudoLocales.addPseudoLocalesTo(src);

        // The pseudo locale should combine both explicit and inherited data from 'en' and 'en_001'.
        CldrData unresolved = pseudo.getDataForLocale("ar_XB", UNRESOLVED);

        // These are a kind of golden data test because it's super hard to really reason about
        // what should be coming out (note how direction markers are added for the 'x' in 100x).
        assertValuesUnordered(unresolved,
            value(included,
                "{\u061C\u202EHello\u202C\u061C} {0}"
                    + " {\u061C\u202EWorld\u202C\u061C}"
                    + " 100\u061C\u202Ex\u202C\u061C"),
            value(pattern,
                "'\u061C\u202Eplus\u202C\u061C' HH:mm;"
                    + " '\u061C\u202Eminus\u202C\u061C' HH:mm"),
            value(inherited, "\u061C\u202EUTC\u202C\u061C"));
    }

    // This tests behaviour expected by Android (previously patched in earlier ICU versions).
    // https://android-review.googlesource.com/c/platform/external/cldr/+/689949
    @Test
    public void testLatinNumbering() {
        CldrValue latn = value(ldmlPath("numbers/defaultNumberingSystem"), "latn");
        FakeDataSupplier src = new FakeDataSupplier().addLocaleData("root", latn);

        CldrDataSupplier pseudo = PseudoLocales.addPseudoLocalesTo(src);

        CldrData unresolved = pseudo.getDataForLocale("ar_XB", UNRESOLVED);
        assertValuesUnordered(unresolved, latn);
    }

    @Test
    public void testExemplars() {
        CldrPath exemplarsPath = ldmlPath("characters/exemplarCharacters[@type=\"auxiliary\"]");
        FakeDataSupplier src =
            new FakeDataSupplier().addLocaleData("en", value(exemplarsPath, "[ignored]"));

        CldrDataSupplier pseudo = PseudoLocales.addPseudoLocalesTo(src);

        assertValuesUnordered(pseudo.getDataForLocale("ar_XB", UNRESOLVED),
            value(exemplarsPath, "[a b c d e f g h i j k l m n o p q r s t u v w x y z \\u061C \\u202E \\u202C]"));
        assertValuesUnordered(pseudo.getDataForLocale("en_XA", UNRESOLVED),
            value(exemplarsPath,
                "[a å b ƀ c ç d ð e é f ƒ g ĝ h ĥ i î j ĵ k ķ l ļ m ɱ"
                    + " n ñ o ö p þ q ǫ r ŕ s š t ţ u û v ṽ w ŵ x ẋ y ý z ž]"));
    }

    public static void assertValuesUnordered(CldrData data, CldrValue... values) {
        Set<CldrValue> captured = new HashSet<>();
        data.accept(ARBITRARY, captured::add);
        assertThat(captured).containsExactlyElementsIn(values);
    }

    private static CldrPath ldmlPath(String path) {
        return CldrPath.parseDistinguishingPath("//ldml/" + path);
    }

    private static CldrValue value(CldrPath path, String value) {
        return CldrValue.parseValue(path.toString(), value);
    }
}
