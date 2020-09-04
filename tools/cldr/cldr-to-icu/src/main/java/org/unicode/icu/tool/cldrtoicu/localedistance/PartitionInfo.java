// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.locale.LSR;

/**
 * Provides mapping arrays to quickly lookup partition information for any region
 * code in client libraries.
 *
 * <p>A region's partition is defined by the set of region variables (e.g. "$enUS")
 * in the CLDR data. Each unique combination of variables forms a partition, and
 * groups of partitions uniquely define language distance groupings. In slightly
 * mathematical terms, partition groups form an "equivalence class" for regions
 * with respect to language distance.
 *
 * <p>So by determining the minimum set of partitions and partition groups, and
 * assigning short IDs to them, it's possibe to create data structures which
 * support all region pairings while being small and fast to access in client code.
 */
final class PartitionInfo {
    private static final Logger logger = Logger.getLogger(PartitionInfo.class.getName());

    /**
     * A builder, to which region variables are added in order to define partitions
     * and partition groups based on territory containment.
     */
    static final class Builder {
        // Possible operations to parse from a region expression (e.g. "US+005-BR").
        private static final CharMatcher REGION_OPS = CharMatcher.anyOf("+-");

        private final TerritoryContainment territories;
        private final Set<String> variables = new HashSet<>();
        private final SortedSetMultimap<String, String> regionToVariables = TreeMultimap.create();

        private Builder(TerritoryContainment territories) {
            this.territories = territories;
        }

        // Returns whether the given string is a known variable or the wildcard token.
        // Non variable strings (e.g. plain region codes) can be passed in and simply
        // return false.
        private boolean isKnownVariableOrWildcard(String variable) {
            return variables.contains(variable) || variable.equals("*");
        }

        /**
         * Adds a variable expression (e.g. "$foo = "US+005-BR") from CLDR data and
         * fully resolves all macro regions to their contained leaf regions.
         *
         * <p>The syntax is simple for now:
         * <pre>
         *     regionSet := region ([-+] region)*
         * </pre>
         * There is no precedence, so "x+y-y+z" is "(((x+y)-y)+z)", and <em>not</em>
         * "(x+y)-(y+z)".
         */
        public void addVariableExpression(String variable, String expr) {
            checkState(variable.startsWith("$") && !variable.startsWith("$!"),
                    "invalid variable: %s", variable);
            checkState(!isKnownVariableOrWildcard(variable),
                    "duplicate variable: %s", variable);
            // Parsing also flattens the list to the corresponding leaf regions,
            // so there should be no macro regions here.
            Set<String> regions = parseAndFlattenRegionExpression(expr, territories);
            // Add the mappings ("$foo" -> X) and the inverse ("$!foo" -> not(X)).
            //
            // The reason that the inverse mapping is needed is because some rules use
            // the negated form of a variable (e.g. "$!enUS") and we must be able to
            // resolve the set of associated partition IDs for it.
            //
            // If we only wanted the set of regions for the negated variable, that
            // would be trivial (and there would be no need to store the negated values)
            // but because the set of partition IDs for a negated variable is NOT always
            // the negated set of parition IDs for the original variable (due to the way
            // partitions overlap) it's not straightforward.
            //
            // In other words:
            //     regions-for("$!foo") == !regions-for("$foo))
            // but:
            //     partition-ids-for("$!foo") != !partition-ids-for("$foo")
            addVariable(variable, regions);
            addVariable(
                    "$!" + variable.substring(1),
                    Sets.difference(territories.getLeafRegions(), regions));
        }

        private void addVariable(String variable, Iterable<String> regions) {
            checkArgument(variables.add(variable),
                    "variable '%s' already present in: %s", variable, regions);
            for (String region : regions) {
                checkArgument(!region.isEmpty(), "%s", regions);
                regionToVariables.put(region, variable);
            }
        }

        // Parses a region expression (e.g. "US+005-BR") to a set of resolved "leaf"
        // regions.
        private static Set<String> parseAndFlattenRegionExpression(
                String expr, TerritoryContainment territories) {
            Set<String> regions = new TreeSet<>();
            Consumer<String> operation = regions::add;
            int last = 0;
            for (int i = REGION_OPS.indexIn(expr); i != -1; i = REGION_OPS.indexIn(expr, last)) {
                applyOperation(operation, expr.substring(last, i), territories);
                // Set up the next operation based on the separator char ('+' or '-').
                operation = (expr.charAt(i) == '+') ? regions::add : regions::remove;
                last = i + 1;
            }
            applyOperation(operation, expr.substring(last), territories);
            return regions;
        }

        private static void applyOperation(
                Consumer<String> operation, String region, TerritoryContainment territories) {
            checkArgument(!region.isEmpty(), "invalid region expresson (missing region)");
            ImmutableSet<String> contained = territories.getLeafRegionsOf(region);
            if (!contained.isEmpty()) {
                // For macro regions, add all their contained leaf regions (direct or indirect).
                contained.forEach(operation);
            } else {
                // Leaf regions are just added directly.
                operation.accept(region);
            }
        }

        /**
         * Registers an implicit variable defined by a region code, and returns the new variable
         * name.
         *
         * <p>This method exists because the {@code <languageMatch>} syntax supports referencing
         * regions directly, rather than just as pre-defined variables (e.g. "en_*_GB"). We still
         * want to track these variables however since they may interact with macro-regions.
         *
         * @param regionOrVariable a region or an existing variable reference.
         * @return the name of the registered variable (including '$' prefix).
         */
        public String ensureVariable(String regionOrVariable) {
            if (isKnownVariableOrWildcard(regionOrVariable)) {
                return regionOrVariable;
            }
            // Here we either have a "raw" region (e.g. "GB") or an unknown variable (e.g. "$foo").
            // However all explicit variables should have already been registered, so if this does
            // start with '$', then it's an error.
            checkArgument(!regionOrVariable.startsWith("$"), "unregistered variable: %s", regionOrVariable);

            // This is an implicit variable, referenced by its region code, so we know that it
            // can never be referenced in the negated form (i.e. "$!GB"), so we don't need to add
            // the inverse mapping in the same way we do for explicitly defined variables.
            //
            // We also allow implicit variables to appear more than once in the list of match
            // rules, so don't call addVariable() here, since that prohibits repeated addition.
            // Since 'regionToVariables' is a _set_ multimap, adding implicit variables is an
            // idempotent operation, so it's okay if it's done more than once.
            String variable = "$" + regionOrVariable;
            variables.add(variable);
            regionToVariables.put(regionOrVariable, variable);
            return variable;
        }

        public PartitionInfo build() {
            // Step 1: Map regions to a unique "partition" ID.
            //
            // A region's partition is the set of variables which include it, and
            // variables can be explicit (e.g. "$enUS"), implicit (e.g. "$GB") or
            // negated (e.g. "$!enUS).
            //
            // For example, region "US" is included in the variables "$americas" and
            // "$enUS", but is also referenced in the "negated" variables "$!cnsar"
            // and "$!maghreb", so the "partition" of "US" is:
            //     { $americas, $enUS, $!cnsar, $!maghreb }
            //
            // A partition ID is a token associated with each unique variable partition.
            //
            // Since other regions, such as "PR" (Puerto Rico) and "VI" (U.S. Virgin
            // Islands), are also "in" the same partition as "US", they will share the
            // same partition ID.
            //
            // However, while "CA" is also included in "$americas", it's NOT defined as
            // an "$enUS" (American English) region, so its partition is:
            //     { $americas, $!enUS, $!cnsar, $!maghreb }
            // and it will have a different partition ID.

            // Check that the region-to-partition map covers every leaf region (this
            // is important to ensure partitions form a disjoint covering).
            checkArgument(regionToVariables.keySet().equals(territories.getLeafRegions()),
                    "unexpected variable grouping (should cover all leaf regions): %s",
                    regionToVariables);
            ImmutableMap<String, String> regionToPartitionId =
                    mapLeafRegionsToPartitionIds(regionToVariables);
            logger.fine(() -> String.format("region to partition ID: %s", regionToPartitionId));

            // Step 2: Construct mappings to and from partition IDs, to group regions
            // by the variables that define them.

            // A sorted mapping from every variable ("$foo" or "$!foo") to the IDs of
            // the partitions it exists in.
            //
            // For example, "$americas" exists in partitions for both "$enUS" (American
            // English) and "$!enUS" (non-American English) regions, so will be mapped
            // to (at least) two unique parition IDs (e.g. X & Y).
            //   "$americas" -> { X, Y }
            ImmutableSetMultimap<String, String> variableToPartitionIds =
                    mapVariablesToPartitionIds(regionToPartitionId, regionToVariables);
            logger.fine(() -> String.format("variable to partition IDs: %s", variableToPartitionIds));

            // A sorted mapping of each macro region to the partitions it intersects
            // with. Unlike leaf regions, macro regions can map to groups of partitions
            // rather than just a single one.
            //
            // For example, the macro region "419" (Latin America) intersects with
            // both partitions:
            //     X = {$americas, $enUS, ...}  (i.e. "Americas + American English")
            // and:
            //     Y = {$americas, $!enUS, ...} (i.e. "Americas + non-American English")
            // so this map would contain:
            //     "419" -> { X, Y }
            ImmutableSetMultimap<String, String> macroRegionToPartitionIds =
                    mapMacroRegionsToPartitionIds(regionToPartitionId, territories);

            // Step 3: Write the sparse "region index to partition group index" lookup
            // array. This is the fast lookup array used to go from LSR region index to
            // the partition group IDs for that region.
            //
            // Note that most entries in the array are zero, since the array maps from
            // all possible regions, not just ones which exist. This is a space/time
            // trade-off (and the array is compressed in the ICU data files anyway).
            byte[] partitionLookupArray = new byte[LSR.REGION_INDEX_LIMIT];
            String[] partitionStrings = writePartitionLookupTable(
                    partitionLookupArray, regionToPartitionId, macroRegionToPartitionIds);

            return new PartitionInfo(variableToPartitionIds, partitionLookupArray, partitionStrings);
        }

        private static ImmutableMap<String, String> mapLeafRegionsToPartitionIds(
                SetMultimap<String, String> regionToVariables) {
            // A generator for partition IDs which returns a single ASCII character for
            // each unique partition.
            //
            // Partition IDs are emitted into the ICU data, so it's important they are
            // small and compatible with the ICU data file format.
            Function<Collection<String>, String> partitionToId =
                    Indexer.create(i -> {
                        // Must be a single 7-bit ASCII value and not '*'. This is NOT
                        // used as a numeric value anywhere and could end up being a non
                        // digit character if the number of unique partitions is > 10.
                        // As of June 2020, there are only 7 unique paritions.
                        char partitionChar = (char) ('0' + i);
                        checkState(partitionChar < 0x7f, "too many partitions: %s", i);
                        return String.valueOf(partitionChar);
                    });

            // For each region, find its partition ID (based on the unique combination
            // of variables that define it).
            ImmutableMap.Builder<String, String> regionToId = ImmutableMap.builder();
            regionToVariables.asMap().forEach(
                    (region, variables) -> regionToId.put(region, partitionToId.apply(variables)));
            return regionToId.build();
        }

        private static ImmutableSetMultimap<String, String> mapVariablesToPartitionIds(
                ImmutableMap<String, String> regionToPartitionId,
                SortedSetMultimap<String, String> regionToVariables) {

            // It's vital that this is a sorted multimap (of values as well as keys)
            // since the values are later indexed and turned into partition strings
            // (so stability of ID order in values is necessary).
            SortedSetMultimap<String, String> variableToPartitionIds = TreeMultimap.create();
            regionToVariables.asMap().forEach((region, variables) -> {
                String partitionId = regionToPartitionId.get(region);
                for (String variable : variables) {
                    variableToPartitionIds.put(variable, partitionId);
                }
            });
            return ImmutableSetMultimap.copyOf(variableToPartitionIds);
        }

        private static ImmutableSetMultimap<String, String> mapMacroRegionsToPartitionIds(
                ImmutableMap<String, String> regionToPartitionId,
                TerritoryContainment territories) {

            // A mapping from each unique partition ID to the regions it contains.
            // This mapping forms a disjoint covering of all (non-macro) regions and
            // is just the "inverse" of the initial "region to partition ID" map.
            //
            // For example, following the examples above where:
            //     X = {$americas, $enUS, ...}
            // and:
            //     Y = {$americas, $!enUS, ...}
            //
            // We would get something like:
            //     X -> {"PR", "US", "VI", ...}
            //     Y -> {"CA", ...}
            Map<String, Collection<String>> partitionToRegions =
                    regionToPartitionId.asMultimap().inverse().asMap();

            // Each macro region can then be decomposed to a mapping to the unique set
            // of partitions it overlaps with based on its leaf regions and the regions
            // of all known partitions.
            SortedSetMultimap<String, String> macroToPartitions = TreeMultimap.create();
            for (String macro : territories.getMacroRegions()) {
                ImmutableSet<String> leaves = territories.getLeafRegionsOf(macro);
                partitionToRegions.forEach((partition, regions) -> {
                    if (!Collections.disjoint(leaves, regions)) {
                        macroToPartitions.put(macro, partition);
                    }
                });
            }
            return ImmutableSetMultimap.copyOf(macroToPartitions);
        }

        private static String[] writePartitionLookupTable(
                byte[] partitionLookupArray,
                ImmutableMap<String, String> regionToPartitionId,
                ImmutableSetMultimap<String, String> macroRegionToPartitionIds) {

            // A generator for indices of partition groups, based on partition IDs.
            //
            // For leaf regions this generates a one-to-one mapping with the single
            // partition ID, but macro regions can overlap multiple partitions.
            Indexer<Collection<String>, Byte> partitionGroupIndexer =
                    Indexer.create(i -> {
                        // The partition group index must fit in a byte.
                        // For Java code simplicity, we want it to also be non-negative.
                        // As of June 2020, there are 15 partition groups.
                        checkState(i <= 0x7f, "too many partition groups: %s", i);
                        return (byte) i.intValue();
                    });

            // The default value in the partition lookup array (index 0) is mapped to by
            // any unsupported region (since "LSR.indexForRegion(<invalid region>)" is 0).
            // We must therefore reserve a special parition group index for these cases
            // before adding the rest of the partitions.
            partitionGroupIndexer.apply(ImmutableSet.of("."));

            // Populate the radix-based sparse index array, where each region is converted
            // to the LSR region index (which must correspond to how regions are indexed in
            // the client side code).
            BiConsumer<String, Collection<String>> writePartitionIndex =
                    (region, ids) -> partitionLookupArray[LSR.indexForRegion(region)] =
                            partitionGroupIndexer.apply(ids);

            // Write leaf regions first (mostly to match the original code behaviour)
            // and then macro regions.
            //
            // Convert the Map<String, String> to a Map<String, Collection<String>>
            // to match the macro regions (even though each collection is a singleton).
            regionToPartitionId.asMultimap().asMap().forEach(writePartitionIndex);
            macroRegionToPartitionIds.asMap().forEach(writePartitionIndex);

            // Check invalid reigons will map to the special "missing partition" value.
            checkState(partitionLookupArray[0] == 0);

            // Return the unique partition groups (sets of partition IDs) as strings
            // (as a sequence of single letter partition IDs). Leaf regions will always
            // have a single partition ID, but macro regions can overlap with multiple
            // partitions.
            return partitionGroupIndexer.getValues().stream()
                    .map(ids -> String.join("", ids)).toArray(String[]::new);
        }
    }

    /**
     * Returns a builder to which variable mappings are added, from which partition
     * information is derived.
     */
    public static Builder builder(TerritoryContainment territories) {
        return new Builder(territories);
    }

    private final ImmutableSetMultimap<String, String> variableToPartitionIds;
    private final byte[] partitionLookupArray;
    private final String[] partitionStrings;

    private PartitionInfo(
            ImmutableSetMultimap<String, String> variableToPartitionIds,
            byte[] partitionLookupArray,
            String[] partitionStrings) {
        this.variableToPartitionIds = ImmutableSetMultimap.copyOf(variableToPartitionIds);
        this.partitionLookupArray = partitionLookupArray;
        this.partitionStrings = partitionStrings;
    }

    /**
     * Returns the set of partition IDs for the given variable, or {@code {"*"}} if the
     * speical '*' variable was given. The returned set must be non-empty because every
     * variable includes at least one region, and all regions map to a partition ID.
     */
    public ImmutableSet<String> getPartitionIds(String variable) {
        if (variable.equals("*")) {
            return ImmutableSet.of("*");
        }
        ImmutableSet<String> result = variableToPartitionIds.get(variable);
        checkArgument(!result.isEmpty(), "variable not defined: %s", variable);
        return result;
    }

    /** Returns the sparse lookup array from LSR region index to partition group index. */
    public byte[] getPartitionLookupArray() {
        return partitionLookupArray;
    }

    /**
     * Returns the partition group lookup array from partition group index to partition
     * ID string.
     */
    public String[] getPartitionStrings() {
        return partitionStrings;
    }
}
