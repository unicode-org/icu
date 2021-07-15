// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.PathMatcher;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Territory containment graph. This is built from CLDR supplemental data and
 * represents all territories and their containment, including macro regions
 * such as {@code "016"}. The root node of the graph is {@code "001"}.
 */
final class TerritoryContainment {
    // CLDR paths for containment data.
    private static final PathMatcher CONTAINMENT_PATH =
        PathMatcher.of("//supplementalData/territoryContainment/group[@type=*]");
    private static final AttributeKey TYPE = AttributeKey.keyOf("group", "type");
    private static final AttributeKey CONTAINS = AttributeKey.keyOf("group", "contains");

    // Standard CLDR list values are split by space.
    // NOTE: You must omit empty strings, since otherwise " foo " becomes ("", "foo", "").
    private static final Splitter LIST_SPLITTER =
            Splitter.on(' ').trimResults().omitEmptyStrings();
    // The world region must be the only root in the graph.
    private static final String WORLD = "001";
    private static final Pattern REGION = Pattern.compile("[A-Z]{2}|[0-9]{3}");

    /**
     * Returns the territory containment information described by the given CLDR
     * supplemental data.
     */
    public static TerritoryContainment getContainment(CldrData supplementalData) {
        // Directed, acyclic containment graph. Maps each territory to its direct contents.
        // Note that since things like deprecated regions are included here, this allows
        // sub-regions to have more than one parent.
        SortedSetMultimap<String, String> graph = TreeMultimap.create();
        supplementalData.accept(CldrData.PathOrder.DTD, v -> {
            CldrPath path = v.getPath();
            if (CONTAINMENT_PATH.matches(path)) {
                graph.putAll(v.get(TYPE), LIST_SPLITTER.split(v.get(CONTAINS)));
            }
        });
        return new TerritoryContainment(ImmutableSetMultimap.copyOf(graph));
    }

    /** Maps each macro-region to all its leaf contents (direct and indirect). */
    private final ImmutableSetMultimap<String, String> macroToLeafRegions;

    private TerritoryContainment(ImmutableSetMultimap<String, String> graph) {
        // Do some double checking of the CLDR data.
        graph.values().forEach(
                r -> checkArgument(REGION.matcher(r).matches(), "bad region '%s' in: %s", r, graph));
        checkArgument(graph.containsKey(WORLD), "missing world region '%s'", WORLD);
        // There should be only one "root" in the graph, so every other region should be
        // contained by something.
        Set<String> allContained = ImmutableSet.copyOf(graph.values());
        Set<String> roots = ImmutableSet.copyOf(Sets.difference(graph.keySet(), allContained));
        checkArgument(roots.equals(ImmutableSet.of(WORLD)),
            "world region '%s' must be the only containment graph root (was %s)", WORLD, roots);

        // Start with a copy of the direct containment graph (but still pass in the direct
        // graph to avoid issues with concurrent modification).
        // If the graph is cyclic, this step will never terminate and run out of memory
        // (and since this is a build-time tool, that's probably fine).
        SortedSetMultimap<String, String> resolved = TreeMultimap.create(graph);
        resolve(WORLD, graph, resolved);
        // For leaf regions (direct or indirect) just retain any sub-regions which don't
        // have child regions from the resolved graph.
        this.macroToLeafRegions = resolved.entries().stream()
            // Only keep macro regions (leaf regions don't have child regions by definition).
            .filter(e -> !graph.get(e.getKey()).isEmpty())
            // Only keep the single-region e.getValue() if it is a leaf region.
            .filter(e -> graph.get(e.getValue()).isEmpty())
            .collect(toImmutableSetMultimap(Entry::getKey, Entry::getValue));
    }

    // Recursively resolve the region and its child regions.
    private static Set<String> resolve(
        String region, SetMultimap<String, String> graph, SetMultimap<String, String> resolved) {
        graph.get(region).forEach(sub -> resolved.putAll(region, resolve(sub, graph, resolved)));
        return resolved.get(region);
    }

    /**
     * Returns the leaf regions contained in the given region (if the given region is a
     * leaf region, then the empty set is returned).
     */
    public ImmutableSet<String> getLeafRegionsOf(String region) {
        return macroToLeafRegions.get(region);
    }

    /** Returns all leaf regions. */
    public ImmutableSet<String> getLeafRegions() {
        return macroToLeafRegions.get(WORLD);
    }

    /** Returns all macro regions. */
    public ImmutableSet<String> getMacroRegions() {
        return macroToLeafRegions.keySet();
    }
}
