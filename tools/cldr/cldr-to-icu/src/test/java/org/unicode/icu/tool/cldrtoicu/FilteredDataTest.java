// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.truth.Truth.assertThat;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;

import com.google.common.collect.ImmutableList;

@RunWith(JUnit4.class)
public class FilteredDataTest {
    @Test
    public void testSimple() {
        CldrValue keep =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "US Dollar");
        CldrValue remove =
            ldml("numbers/currencies/currency[@type=\"USD\"]/symbol", "US$");
        CldrValue replace =
            ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "YYY");
        CldrValue replacement =
            ldml("units/durationUnit[@type=\"foo\"]/durationUnitPattern", "ZZZ");

        CldrData src = CldrDataSupplier.forValues(ImmutableList.of(keep, remove, replace));
        CldrData filtered = new FilteredData(src) {
            @Nullable @Override protected CldrValue filter(CldrValue value) {
                if (value.equals(remove)) {
                    return null;
                } else if (value.equals(replace)) {
                    return replacement;
                } else {
                    return value;
                }
            }
        };

        List<CldrValue> filteredValues = new ArrayList<>();
        filtered.accept(ARBITRARY, filteredValues::add);
        assertThat(filteredValues).containsExactly(keep, replacement).inOrder();

        assertThat(filtered.get(remove.getPath())).isNull();
        assertThat(filtered.get(keep.getPath())).isEqualTo(keep);
        assertThat(filtered.get(replace.getPath())).isEqualTo(replacement);
    }

    @Test
    public void testBadReplacementPath() {
        CldrValue replace =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "VALUE");
        CldrValue replacement =
            ldml("numbers/currencies/currency[@type=\"USD\"]/symbol", "VALUE");

        CldrData src = CldrDataSupplier.forValues(ImmutableList.of(replace));
        CldrData filtered = new FilteredData(src) {
            @Nullable @Override protected CldrValue filter(CldrValue value) {
                return replacement;
            }
        };
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> filtered.accept(ARBITRARY, v -> {}));
        assertThat(e).hasMessageThat().contains("not permitted to modify distinguishing paths");
        assertThat(e).hasMessageThat().contains(replace.toString());
        assertThat(e).hasMessageThat().contains(replacement.toString());
    }

    @Test
    public void testBadReplacementAttributes() {
        CldrValue replace =
            ldml("numbers/currencies/currency[@type=\"USD\"]/displayName", "XXX");
        CldrValue replacement =
            ldml("numbers/currencies/currency[@type=\"GBP\"]/displayName", "XXX");

        CldrData src = CldrDataSupplier.forValues(ImmutableList.of(replace));
        CldrData filtered = new FilteredData(src) {
            @Nullable @Override protected CldrValue filter(CldrValue value) {
                return replacement;
            }
        };
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> filtered.accept(ARBITRARY, v -> {}));
        assertThat(e).hasMessageThat().contains("not permitted to modify distinguishing paths");
        assertThat(e).hasMessageThat().contains(replace.toString());
        assertThat(e).hasMessageThat().contains(replacement.toString());
    }

    private static CldrValue ldml(String path, String value) {
        return CldrValue.parseValue("//ldml/" + path, value);
    }
}
