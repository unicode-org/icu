// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.truth.Truth.assertThat;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.BytesTrie;

// NOTE: Remember that here, "region" is synonymous with a "partition group ID".
public class DistanceTableTest {
    @Test
    public void testSimpleMapping() {
        DistanceTable.Builder builder = defaultTable();
        // You need at least one non default mapping.
        builder.addDistance(23, true, "en", "en");
        DistanceTable table = builder.build();
        assertThat(getTrieTable(table)).containsExactly("en-en", 23);
        assertThat(table.getDefaultDistances()).asList().containsExactly(80, 50, 4, 4).inOrder();
    }

    @Test
    public void testReverseMapping() {
        DistanceTable.Builder builder = defaultTable();
        // You need at least one non default mapping.
        builder.addDistance(1, false, "no", "nb");
        DistanceTable table = builder.build();
        assertThat(getTrieTable(table))
                .containsExactly(
                        "nb-no", 1,
                        "no-nb", 1)
                .inOrder();
    }

    @Test
    public void testMinRegionDistance() {
        DistanceTable.Builder builder = defaultTable();
        // You need at least one non default mapping.
        builder.addDistance(2, true, "zh", "zh", "Hant", "Hant", "1", "1");
        builder.addDistance(4, true, "zh", "zh", "Hant", "Hant", "2", "2");
        builder.addDistance(6, true, "zh", "zh", "Hant", "Hant", "*", "*");
        DistanceTable table = builder.build();
        assertThat(getTrieTable(table))
                .containsExactly(
                        // Inferred mappings for "parent" locales.
                        "zh-zh", 0,                 // Equal locales have zero distance.
                        "zh-zh-*-*", 50,            // Default unknown script distance
                        "zh-zh-Hant-Hant", 0,
                        // Trie ordering prefers "*" mapping at the front.
                        "zh-zh-Hant-Hant-*-*", 6,
                        "zh-zh-Hant-Hant-1-1", 2,
                        "zh-zh-Hant-Hant-2-2", 4)
                .inOrder();
        // Minimum region distance is recorded successfully (last value).
        assertThat(table.getDefaultDistances()).asList().containsExactly(80, 50, 4, 2).inOrder();
    }

    @Test
    public void testSkipScript() {
        DistanceTable.Builder builder = defaultTable();
        // You need at least one non default mapping.
        builder.addDistance(2, true, "en", "en", "*", "*", "1", "1");
        builder.addDistance(4, true, "en", "en", "*", "*", "*", "*");
        DistanceTable table = builder.build();
        assertThat(getTrieTable(table))
                .containsExactly(
                        // "en-en" is marked for "skip script" so the remaining "en-en-..."
                        // mappings are correctly interpretted as "language-region".
                        "en-en", 128,
                        "en-en-*-*", 4,
                        "en-en-1-1", 2)
                .inOrder();
    }

    @Test
    public void testFirstOneWins() {
        DistanceTable.Builder builder = defaultTable();
        // Duplicate mappings are only expected for "region" where different rules can
        // produce duplicate mappings by virtue of having non-disjoint region partitions.
        builder.addDistance(2, true, "en", "en", "*", "*", "1", "1");
        builder.addDistance(4, true, "en", "en", "*", "*", "1", "1");  // ignored
        builder.addDistance(6, true, "en", "en", "*", "*", "*", "*");
        DistanceTable table = builder.build();
        assertThat(getTrieTable(table))
                .containsExactly(
                        "en-en", 128,
                        "en-en-*-*", 6,
                        "en-en-1-1", 2)
                .inOrder();
    }

    @Test
    public void testBadDistance() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> defaultTable().addDistance(123, true, "en", "fr"));
        assertThat(e).hasMessageThat().contains("distance");
        assertThat(e).hasMessageThat().contains("123");
    }

    @Test
    public void testBadParameters() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> defaultTable().addDistance(1, true, "en", "en", "*"));
        assertThat(e).hasMessageThat().contains("invalid number of arguments");
    }

    @Test
    public void testBadKeys() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> defaultTable().addDistance(1, true, "en", "*"));
        assertThat(e).hasMessageThat().contains("invalid mapping key");
        assertThat(e).hasMessageThat().contains("en");
        assertThat(e).hasMessageThat().contains("�");
    }

    private static DistanceTable.Builder defaultTable() {
        DistanceTable.Builder table = DistanceTable.builder();
        // Defaults (which are necessary to add, but should always be trimmed from results).
        // The actual distances don't matter (and are copied to the distance array).
        table.addDistance(80, false, "*", "*");
        table.addDistance(50, false, "*", "*", "*", "*");
        table.addDistance(4, false, "*", "*", "*", "*", "*", "*");
        return table;
    }

    @Test
    public void testNoDefaultLanguage() {
        // Don't get the default table, since we need to test without defaults.
        DistanceTable.Builder builder = DistanceTable.builder();
        IllegalStateException e = assertThrows(IllegalStateException.class, builder::build);
        assertThat(e).hasMessageThat().contains("missing default language");
    }

    @Test
    public void testNoDefaultScript() {
        // Don't get the default table, since we need to test without defaults.
        DistanceTable.Builder builder = DistanceTable.builder();
        builder.addDistance(80, false, "*", "*");
        IllegalStateException e = assertThrows(IllegalStateException.class, builder::build);
        assertThat(e).hasMessageThat().contains("missing default script");
    }

    @Test
    public void testNoDefaultRegion() {
        // Don't get the default table, since we need to test without defaults.
        DistanceTable.Builder builder = DistanceTable.builder();
        builder.addDistance(80, false, "*", "*");
        builder.addDistance(50, false, "*", "*", "*", "*");
        IllegalStateException e = assertThrows(IllegalStateException.class, builder::build);
        assertThat(e).hasMessageThat().contains("missing default region");
    }

    // VisibleForTesting
    public ImmutableMap<String, Integer> getTrieTable(DistanceTable table) {
        // We rebuild the Trie from the byte[] data.
        return TestData.getTrieTable(new BytesTrie(table.getTrie().toByteArray(), 0), "*-*", i -> i);
    }
}
