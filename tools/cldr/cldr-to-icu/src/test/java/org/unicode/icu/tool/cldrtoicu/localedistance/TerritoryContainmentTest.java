// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.territoryGroup;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;

import org.junit.Test;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;

public class TerritoryContainmentTest {

    @Test
    public void testSimple() {
        CldrData testData = getTestData(
                territoryGroup("001", "002", "003"),
                territoryGroup("002", "GB", "FR"),
                territoryGroup("003", "US", "CA"));
        TerritoryContainment containment = TerritoryContainment.getContainment(testData);
        assertThat(containment.getMacroRegions()).containsExactly("001", "002", "003").inOrder();
        assertThat(containment.getLeafRegions()).containsExactly("CA", "FR", "GB", "US").inOrder();
        assertThat(containment.getLeafRegionsOf("002")).containsExactly("FR", "GB").inOrder();
        assertThat(containment.getLeafRegionsOf("GB")).isEmpty();
    }

    @Test
    public void testOverlappingContainment() {
        CldrData testData = getTestData(
                territoryGroup("001", "002", "003", "004"),
                territoryGroup("002", "GB", "FR"),
                territoryGroup("003", "US", "CA"),
                territoryGroup("004", "CA", "GB"));
        TerritoryContainment containment = TerritoryContainment.getContainment(testData);
        assertThat(containment.getLeafRegions()).containsExactly("CA", "FR", "GB", "US").inOrder();
        assertThat(containment.getLeafRegionsOf("002")).containsExactly("FR", "GB").inOrder();
        assertThat(containment.getLeafRegionsOf("004")).containsExactly("CA", "GB").inOrder();
    }

    @Test
    public void testMultipleRootsFails() {
        CldrData testData = getTestData(
                territoryGroup("001", "002"),
                territoryGroup("002", "GB", "FR"),
                territoryGroup("003", "US", "CA"));
        IllegalArgumentException err =
                assertThrows(IllegalArgumentException.class, () -> TerritoryContainment.getContainment(testData));
        assertThat(err).hasMessageThat().contains("001");
        assertThat(err).hasMessageThat().contains("003");
        assertThat(err).hasMessageThat().doesNotContain("002");
    }

    @Test
    public void testCyclicGraphFails() {
        CldrData testData = getTestData(
                territoryGroup("001", "002"),
                territoryGroup("002", "001"));
        IllegalArgumentException err =
                assertThrows(IllegalArgumentException.class, () -> TerritoryContainment.getContainment(testData));
        assertThat(err).hasMessageThat().contains("world region");
        assertThat(err).hasMessageThat().contains("001");
    }

    private static CldrData getTestData(CldrValue... values) {
        return CldrDataSupplier.forValues(asList(values));
    }
}
