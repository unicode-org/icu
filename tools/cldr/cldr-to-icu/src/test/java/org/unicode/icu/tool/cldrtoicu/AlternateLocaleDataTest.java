// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.truth.Truth.assertThat;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.RESOLVED;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

@RunWith(JUnit4.class)
public class AlternateLocaleDataTest {
    @Test
    public void testLocaleData() {
        // Target and source values.
        CldrValue target =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "Full Display Name");
        CldrValue source =
            ldml("numbers/currencies/currency[@type=\"USD\"][@alt=\"short\"]/displayName", "Name");
        // The target path with the source value we expect to be seen in the transformed data.
        CldrValue altValue =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "Name");

        // Something that's not transformed.
        CldrValue other =
            ldml("numbers/currencies/currency[@type=\"USD\"]/symbol", "US$");
        // Something that should only exist in the resolved data.
        CldrValue inherited =
            ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "YYY");

        FakeDataSupplier src = new FakeDataSupplier()
            .addLocaleData("xx", target, source, other)
            .addLocaleData("root", inherited);
        CldrDataSupplier transformed =
            AlternateLocaleData.transform(
                src, ImmutableMap.of(target.getPath(), source.getPath()), ImmutableTable.of());

        CldrData unresolved = transformed.getDataForLocale("xx", UNRESOLVED);
        CldrData resolved = transformed.getDataForLocale("xx", RESOLVED);

        // Note that the source is always removed (unless it's also a target).
        assertValuesUnordered(unresolved, altValue, other);
        assertValuesUnordered(resolved, altValue, other, inherited);
    }

    @Test
    public void testMissingSource() {
        // Target and source values.
        CldrValue target =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "Full Display Name");
        CldrValue source =
            ldml("numbers/currencies/currency[@type=\"USD\"][@alt=\"short\"]/displayName", "Name");

        FakeDataSupplier src = new FakeDataSupplier().addLocaleData("xx", target);
        CldrDataSupplier transformed =
            AlternateLocaleData.transform(
                src, ImmutableMap.of(target.getPath(), source.getPath()), ImmutableTable.of());

        CldrData unresolved = transformed.getDataForLocale("xx", UNRESOLVED);
        CldrData resolved = transformed.getDataForLocale("xx", RESOLVED);

        // No change because there's nothing to get an alternate value from.
        assertValuesUnordered(unresolved, target);
        assertValuesUnordered(resolved, target);
    }

    @Test
    public void testMissingTarget() {
        // Target and source values.
        CldrValue target =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "Full Display Name");
        CldrValue source =
            ldml("numbers/currencies/currency[@type=\"USD\"][@alt=\"short\"]/displayName", "Name");
        CldrValue other =
            ldml("numbers/currencies/currency[@type=\"EUR\"]/displayName", "Euro");

        FakeDataSupplier src = new FakeDataSupplier().addLocaleData("xx", source, other);
        CldrDataSupplier transformed =
            AlternateLocaleData.transform(
                src, ImmutableMap.of(target.getPath(), source.getPath()), ImmutableTable.of());

        CldrData unresolved = transformed.getDataForLocale("xx", UNRESOLVED);
        CldrData resolved = transformed.getDataForLocale("xx", RESOLVED);

        // If there's no target the alt-path mapping is incomplete and we do nothing (this matches
        // the old CLDR tool behaviour and reasonable but can hide inconsistencies in CLDR data).
        assertValuesUnordered(unresolved, source, other);
        assertValuesUnordered(resolved, source, other);
    }

    @Test
    public void testBadPaths() {
        // Target and source values.
        CldrPath target = CldrPath.parseDistinguishingPath(
            "//ldml/numbers/currencies/currency[@type=\"USD\"]/displayName");
        CldrPath source = CldrPath.parseDistinguishingPath(
            "//ldml/numbers/currencies/currency[@type=\"USD\"]/symbol");

        FakeDataSupplier src = new FakeDataSupplier();
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> AlternateLocaleData.transform(
                src, ImmutableMap.of(target, source), ImmutableTable.of()));
        assertThat(e).hasMessageThat().contains("alternate paths must have the same namespace");
        assertThat(e).hasMessageThat().contains(target.toString());
        assertThat(e).hasMessageThat().contains(source.toString());
    }

    @Test
    public void testNonLdml() {
        // Real supplemental data with "values" in the value attributes:
        // target: territories=[AG AR AS AU ...]
        // source: territories=[GB]
        // where GB is also listed as having "mon" as the first day in it's primary path.
        //
        // You can see why swapping paths based on 'alt' for supplemental data would be very wrong,
        // because it would remove "XX" and "YY" by replacing the value attribute. Supplemental
        // and BCP-47 data doesn't have a single value per path, so isn't suitable for swapping.
        //
        // The right way to do this would be to merge the 'territories' attribute and remove the
        // alt territoy from its original list, but that's very complex and depends on the specific
        // meaning of each path in question, and will probably never be supported.
        CldrPath target = CldrPath.parseDistinguishingPath(
            "//supplementalData/weekData/firstDay[@day=\"sun\"]");
        CldrPath source = CldrPath.parseDistinguishingPath(
            "//supplementalData/weekData/firstDay[@day=\"sun\"][@alt=\"variant\"]");

        FakeDataSupplier src = new FakeDataSupplier();
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> AlternateLocaleData.transform(
                src, ImmutableMap.of(target, source), ImmutableTable.of()));
        assertThat(e).hasMessageThat().contains("only locale data (LDML) is supported");
        // At least one of the paths should be in the error message, so look for common substring.
        assertThat(e).hasMessageThat().contains("/weekData/firstDay[@day=\"sun\"]");
    }

    public static void assertValuesUnordered(CldrData data, CldrValue... values) {
        Set<CldrValue> captured = new HashSet<>();
        data.accept(ARBITRARY, captured::add);
        assertThat(captured).containsExactlyElementsIn(values);
    }

    private static CldrValue ldml(String path, String value) {
        return CldrValue.parseValue("//ldml/" + path, value);
    }
}
