// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import org.junit.Test;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;

import com.ibm.icu.impl.locale.LSR;

public class PartitionInfoTest {
    @Test
    public void testPartitionInfo() {
        TerritoryContainment territories = territories(
                TestData.territoryGroup("001", "019", "150"),
                // Americas (simplified): North America + Caribbean
                TestData.territoryGroup("019", "003", "029"),
                TestData.territoryGroup("003", "CA", "US"),
                TestData.territoryGroup("029", "PR", "VI"),
                // Sort of Europe
                TestData.territoryGroup("150", "DE", "FR", "GB"));
        PartitionInfo.Builder builder = PartitionInfo.builder(territories);
        // "American English" associated with U.S.A and Puerto Rico.
        builder.addVariableExpression("$enUS", "US+PR");
        // The "Americas" form a different language grouping.
        builder.addVariableExpression("$americas", "019");
        // Also register a separate variable for just the GB region code.
        builder.ensureVariable("GB");

        // In terms of "partitions" (which are assigned in sorted region code order)
        // we should now have:
        //
        // CA, VI -> { $americas, $!enUS } == "0"
        // DE, FR -> { $!americas, $!enUS } == "1"
        // GB     -> { $!americas, $!enUS, $GB } == "2"
        // PR, US -> { $americas, $enUS } == "3"
        //
        // So reversing this to map variables to the partitions they overlap with:
        // "$enUS"      -> { "3" }
        // "$!enUS"     -> { "0", "1", "2" }
        // "$americas"  -> { "0", "3" }
        // "$!americas" -> { "1", "2" }
        // "$GB"        -> { "2" }
        PartitionInfo info = builder.build();
        assertThat(info.getPartitionIds("$enUS")).containsExactly("3");
        assertThat(info.getPartitionIds("$!enUS")).containsExactly("0", "1", "2");
        assertThat(info.getPartitionIds("$americas")).containsExactly("0", "3");
        assertThat(info.getPartitionIds("$!americas")).containsExactly("1", "2");
        assertThat(info.getPartitionIds("$GB")).containsExactly("2");

        // Partition strings are made up of the explicit partition IDs.
        // Indices are also assigned in first encountered region code order.
        assertThat(info.getPartitionStrings()).asList().containsExactly(
                // Default (unmapped) special case must be first.
                ".",      // ??            : index=0
                // Partitions IDs for "leaf" regions (only one partition per region).
                "0",      // CA, VI        : index=1
                "1",      // DE, FR        : index=2
                "2",      // GB            : index=3
                "3",      // PR, US        : index=4
                // Macros regions include paritions of all overlapping regions.
                "0123",   // 001           : index=5
                "03",     // 003, 019, 029 : index=6
                "12")     // 150           : index=7
            .inOrder();

        // The partition lookup array maps regions to the index of their partition string.
        byte[] lookup = info.getPartitionLookupArray();
        assertThat(lookup[LSR.indexForRegion("CA")]).isEqualTo(1);
        assertThat(lookup[LSR.indexForRegion("VI")]).isEqualTo(1);
        assertThat(lookup[LSR.indexForRegion("DE")]).isEqualTo(2);
        assertThat(lookup[LSR.indexForRegion("FR")]).isEqualTo(2);
        assertThat(lookup[LSR.indexForRegion("GB")]).isEqualTo(3);
        assertThat(lookup[LSR.indexForRegion("PR")]).isEqualTo(4);
        assertThat(lookup[LSR.indexForRegion("US")]).isEqualTo(4);
        assertThat(lookup[LSR.indexForRegion("001")]).isEqualTo(5);
        assertThat(lookup[LSR.indexForRegion("003")]).isEqualTo(6);
        assertThat(lookup[LSR.indexForRegion("019")]).isEqualTo(6);
        assertThat(lookup[LSR.indexForRegion("029")]).isEqualTo(6);
        assertThat(lookup[LSR.indexForRegion("150")]).isEqualTo(7);
        // Unknown regions map to index 0.
        assertThat(lookup[LSR.indexForRegion("JP")]).isEqualTo(0);
    }

    private static TerritoryContainment territories(CldrValue... tcs) {
        return TerritoryContainment.getContainment(CldrDataSupplier.forValues(asList(tcs)));
    }
}
