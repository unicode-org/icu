// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.locale.XCldrStub.Multimap;
import com.ibm.icu.impl.locale.XCldrStub.Predicate;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.impl.locale.XCldrStub.TreeMultimap;
import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.BytesTrieBuilder;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public final class LocaleDistanceBuilder {
    private static final String ANY = "�"; // matches any character. Uses value above any subtag.

    private static final boolean DEBUG_OUTPUT = false;

    private static String fixAny(String string) {
        return "*".equals(string) ? ANY : string;
    }

    private static ICUResourceBundle getSupplementalDataBundle(String name) {
        return ICUResourceBundle.getBundleInstance(
            ICUData.ICU_BASE_NAME, name,
            ICUResourceBundle.ICU_DATA_CLASS_LOADER, ICUResourceBundle.OpenType.DIRECT);
    }

    private static final class TerritoryContainment {
        /** Directed, acyclic containment graph. Maps each container to its direct contents. */
        final Multimap<String, String> graph = TreeMultimap.create();
        /** Maps each container to all of its contents, direct and indirect. */
        final Multimap<String, String> resolved = TreeMultimap.create();
        /** Maps each container only to its leaf contents. */
        final Multimap<String, String> toLeavesOnly = TreeMultimap.create();
        /** The leaves of the graph. */
        final Set<String> leaves;

        TerritoryContainment(ICUResourceBundle supplementalData) {
            UResource.Value value = supplementalData.getValueWithFallback("territoryContainment");
            UResource.Key key = new UResource.Key();
            addContainments(key, value);
            resolve("001");

            for (Map.Entry<String, Set<String>> entry : resolved.asMap().entrySet()) {
                String container = entry.getKey();
                for (String contained : entry.getValue()) {
                    if (resolved.get(contained) == null) {  // a leaf node (usually a country)
                        toLeavesOnly.put(container, contained);
                    }
                }
            }
            leaves = toLeavesOnly.get("001");
        }

        private void addContainments(UResource.Key key, UResource.Value value) {
            UResource.Table containers = value.getTable();
            for (int i = 0; containers.getKeyAndValue(i, key, value); ++i) {
                if (key.length() <= 3) {
                    String container = key.toString();
                    String[] contents = value.getStringArrayOrStringAsArray();
                    for (String s : contents) {
                        graph.put(container, s);
                    }
                } else {
                    addContainments(key, value);  // containedGroupings etc.
                }
            }
        }

        private Set<String> resolve(String region) {
            Set<String> contained = graph.get(region);
            if (contained == null) {
                return Collections.emptySet();
            }
            resolved.putAll(region, contained); // do top level
            // then recursively
            for (String subregion : contained) {
                resolved.putAll(region, resolve(subregion));
            }
            return resolved.get(region);
        }
    }

    private static final class Rule {
        final List<String> desired;
        final List<String> supported;
        final int distance;
        final boolean oneway;

        Rule(List<String> desired, List<String> supported, int distance, boolean oneway) {
            this.desired = desired;
            this.supported = supported;
            this.distance = distance;
            this.oneway = oneway;
        }
    }

    private static final <T> int makeUniqueIndex(Map<T, Integer> objectToInt, T source) {
        Integer result = objectToInt.get(source);
        if (result == null) {
            int newResult = objectToInt.size();
            objectToInt.put(source, newResult);
            return newResult;
        } else {
            return result;
        }
    }

    private static final class TrieBuilder {
        byte[] bytes = new byte[24];
        int length = 0;
        BytesTrieBuilder tb = new BytesTrieBuilder();

        void addStar(int value) {
            assert value >= 0;
            bytes[length++] = '*';
            tb.add(bytes, length, value);
        }

        void addSubtag(String s, int value) {
            assert !s.isEmpty();
            assert value >= 0;
            assert !s.equals(ANY);
            int end = s.length() - 1;
            for (int i = 0;; ++i) {
                char c = s.charAt(i);
                assert c <= 0x7f;
                if (i < end) {
                    bytes[length++] = (byte) c;
                } else {
                    // Mark the last character as a terminator to avoid overlap matches.
                    bytes[length++] = (byte) (c | 0x80);
                    break;
                }
            }
            tb.add(bytes, length, value);
        }

        BytesTrie build() {
            ByteBuffer buffer = tb.buildByteBuffer(BytesTrieBuilder.Option.SMALL);
            // Allocate an array with just the necessary capacity,
            // so that we do not hold on to a larger array for a long time.
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            if (DEBUG_OUTPUT) {
                System.out.println("distance trie size: " + bytes.length + " bytes");
            }
            return new BytesTrie(bytes, 0);
        }
    }

    private static final class DistanceTable {
        final int nodeDistance;  // distance for the lookup so far
        final Map<String, Map<String, DistanceTable>> subtables;

        DistanceTable(int distance) {
            nodeDistance = distance;
            subtables = new TreeMap<>();
        }

        @Override
        public boolean equals(Object obj) {
            DistanceTable other;
            return this == obj ||
                    (obj != null
                    && obj.getClass() == this.getClass()
                    && nodeDistance == (other = (DistanceTable) obj).nodeDistance
                    && subtables.equals(other.subtables));
        }
        @Override
        public int hashCode() {
            return nodeDistance ^ subtables.hashCode();
        }

        public int getDistance(String desired, String supported, Output<DistanceTable> distanceTable, boolean starEquals) {
            boolean star = false;
            Map<String, DistanceTable> sub2 = subtables.get(desired);
            if (sub2 == null) {
                sub2 = subtables.get(ANY); // <*, supported>
                star = true;
            }
            DistanceTable value = sub2.get(supported);   // <*/desired, supported>
            if (value == null) {
                value = sub2.get(ANY);  // <*/desired, *>
                if (value == null && !star) {
                    sub2 = subtables.get(ANY);   // <*, supported>
                    value = sub2.get(supported);
                    if (value == null) {
                        value = sub2.get(ANY);   // <*, *>
                    }
                }
                star = true;
            }
            if (distanceTable != null) {
                distanceTable.value = value;
            }
            int result = starEquals && star && desired.equals(supported) ? 0 : value.nodeDistance;
            return result;
        }

        void copy(DistanceTable other) {
            for (Map.Entry<String, Map<String, DistanceTable>> e1 : other.subtables.entrySet()) {
                for (Map.Entry<String, DistanceTable> e2 : e1.getValue().entrySet()) {
                    DistanceTable value = e2.getValue();
                    addSubtable(e1.getKey(), e2.getKey(), value.nodeDistance);
                }
            }
        }

        DistanceTable addSubtable(String desired, String supported, int distance) {
            Map<String, DistanceTable> sub2 = subtables.get(desired);
            if (sub2 == null) {
                subtables.put(desired, sub2 = new TreeMap<>());
            }
            DistanceTable oldNode = sub2.get(supported);
            if (oldNode != null) {
                return oldNode;
            }

            final DistanceTable newNode = new DistanceTable(distance);
            sub2.put(supported, newNode);
            return newNode;
        }

        /**
         * Return null if value doesn't exist
         */
        private DistanceTable getNode(String desired, String supported) {
            Map<String, DistanceTable> sub2 = subtables.get(desired);
            if (sub2 == null) {
                return null;
            }
            return sub2.get(supported);
        }


        /** add table for each subitem that matches and doesn't have a table already
         */
        void addSubtables(
                String desired, String supported,
                Predicate<DistanceTable> action) {
            DistanceTable node = getNode(desired, supported);
            if (node == null) {
                // get the distance it would have
                Output<DistanceTable> node2 = new Output<>();
                int distance = getDistance(desired, supported, node2, true);
                // now add it
                node = addSubtable(desired, supported, distance);
                if (node2.value != null) {
                    DistanceTable nextTable = node2.value;
                    node.copy(nextTable);
                }
            }
            action.test(node);
        }

        void addSubtables(String desiredLang, String supportedLang,
                String desiredScript, String supportedScript,
                int percentage) {

            // add to all the values that have the matching desiredLang and supportedLang
            @SuppressWarnings("unused")
            boolean haveKeys = false;
            for (Map.Entry<String, Map<String, DistanceTable>> e1 : subtables.entrySet()) {
                String key1 = e1.getKey();
                final boolean desiredIsKey = desiredLang.equals(key1);
                if (desiredIsKey || desiredLang.equals(ANY)) {
                    for (Map.Entry<String, DistanceTable> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals(ANY)) {
                            DistanceTable value = e2.getValue();
                            value.addSubtable(desiredScript, supportedScript, percentage);
                        }
                    }
                }
            }
            // now add the sequence explicitly
            DistanceTable dt = new DistanceTable(-1);
            dt.addSubtable(desiredScript, supportedScript, percentage);
            CopyIfEmpty r = new CopyIfEmpty(dt);
            addSubtables(desiredLang, supportedLang, r);
        }

        void addSubtables(String desiredLang, String supportedLang,
                String desiredScript, String supportedScript,
                String desiredRegion, String supportedRegion,
                int percentage) {

            // add to all the values that have the matching desiredLang and supportedLang
            @SuppressWarnings("unused")
            boolean haveKeys = false;
            for (Map.Entry<String, Map<String, DistanceTable>> e1 : subtables.entrySet()) {
                String key1 = e1.getKey();
                final boolean desiredIsKey = desiredLang.equals(key1);
                if (desiredIsKey || desiredLang.equals(ANY)) {
                    for (Map.Entry<String, DistanceTable> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals(ANY)) {
                            DistanceTable value = e2.getValue();
                            value.addSubtables(desiredScript, supportedScript, desiredRegion, supportedRegion, percentage);
                        }
                    }
                }
            }
            // now add the sequence explicitly

            DistanceTable dt = new DistanceTable(-1);
            dt.addSubtable(desiredRegion, supportedRegion, percentage);
            AddSub r = new AddSub(desiredScript, supportedScript, dt);
            addSubtables(desiredLang,  supportedLang,  r);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("distance: ").append(nodeDistance).append('\n');
            return toString("", sb).toString();
        }

        private StringBuilder toString(String indent, StringBuilder buffer) {
            String indent2 = indent.isEmpty() ? "" : "\t";
            for (Map.Entry<String, Map<String, DistanceTable>> e1 : subtables.entrySet()) {
                final Map<String, DistanceTable> subsubtable = e1.getValue();
                buffer.append(indent2).append(e1.getKey());
                String indent3 = "\t";
                for (Map.Entry<String, DistanceTable> e2 : subsubtable.entrySet()) {
                    DistanceTable value = e2.getValue();
                    buffer.append(indent3).append(e2.getKey());
                    buffer.append('\t').append(value.nodeDistance);
                    value.toString(indent+"\t\t\t", buffer);
                    buffer.append('\n');
                    indent3 = indent+'\t';
                }
                indent2 = indent;
            }
            return buffer;
        }

        void toTrie(TrieBuilder builder) {
            int startLength = builder.length;
            for (Map.Entry<String, Map<String, DistanceTable>> desSuppNode : subtables.entrySet()) {
                String desired = desSuppNode.getKey();
                Map<String, DistanceTable> suppNodeMap = desSuppNode.getValue();
                // Collapse ANY-ANY into one single *.
                if (desired.equals(ANY)) {
                    assert suppNodeMap.size() == 1;
                    DistanceTable node = suppNodeMap.get(ANY);
                    builder.addStar(node.nodeDistance);
                    node.toTrie(builder);
                } else {
                    builder.addSubtag(desired, 0);
                    int desiredLength = builder.length;
                    for (Map.Entry<String, DistanceTable> suppNode : suppNodeMap.entrySet()) {
                        String supported = suppNode.getKey();
                        assert !supported.equals(ANY);
                        DistanceTable node = suppNode.getValue();
                        builder.addSubtag(supported, node.nodeDistance);
                        node.toTrie(builder);
                        builder.length = desiredLength;
                    }
                }
                builder.length = startLength;
            }
        }
    }

    private static final class CopyIfEmpty implements Predicate<DistanceTable> {
        private final DistanceTable toCopy;
        CopyIfEmpty(DistanceTable resetIfNotNull) {
            this.toCopy = resetIfNotNull;
        }
        @Override
        public boolean test(DistanceTable node) {
            if (node.subtables.isEmpty()) {
                node.copy(toCopy);
            }
            return true;
        }
    }

    private static final class AddSub implements Predicate<DistanceTable> {
        private final String desiredSub;
        private final String supportedSub;
        private final CopyIfEmpty r;

        AddSub(String desiredSub, String supportedSub, DistanceTable distanceTableToCopy) {
            this.r = new CopyIfEmpty(distanceTableToCopy);
            this.desiredSub = desiredSub;
            this.supportedSub = supportedSub;
        }
        @Override
        public boolean test(DistanceTable node) {
            if (node == null) {
                throw new IllegalArgumentException("bad structure");
            } else {
                node.addSubtables(desiredSub, supportedSub, r);
            }
            return true;
        }
    }

    private static Collection<String> getIdsFromVariable(
            Multimap<String, String> variableToPartition, String variable) {
        if (variable.equals("*")) {
            return Collections.singleton("*");
        }
        Collection<String> result = variableToPartition.get(variable);
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("Variable not defined: " + variable);
        }
        return result;
    }

    static LocaleDistance build() {
        // From CLDR supplementalData/languageMatching/languageMatches type="written_new"/
        //   and then paradigmLocales, matchVariable, and the last languageMatch items.
        ICUResourceBundle supplementalData = getSupplementalDataBundle("supplementalData");
        String[] paradigms = supplementalData.getValueWithFallback(
                "languageMatchingInfo/written/paradigmLocales").getStringArray();
        Set<LSR> paradigmLSRs = new HashSet<>();  // could be TreeSet if LSR were Comparable
        for (String paradigm : paradigms) {
            ULocale pl = new ULocale(paradigm);
            paradigmLSRs.add(XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(pl));
        }

        TerritoryContainment tc = new TerritoryContainment(supplementalData);

        RegionMapperBuilder rmb = new RegionMapperBuilder(tc);
        UResource.Value value = supplementalData.getValueWithFallback(
                "languageMatchingInfo/written/matchVariable");
        UResource.Table variables = value.getTable();
        UResource.Key key = new UResource.Key();
        for (int i = 0; variables.getKeyAndValue(i, key, value); ++i) {
            String variable = "$" + key.toString();
            String regions = value.getString();
            rmb.add(variable, regions);
        }

        // Parse the rules.
        // We could almost process them while reading them from the source data,
        // but a rule may contain a region code rather than a variable.
        // We need to create a variable for each such region code
        // before rmb.build() and before processing the rules.
        Splitter bar = Splitter.on('_');

        int prevSize = 0;
        value = supplementalData.getValueWithFallback("languageMatchingNew/written");
        UResource.Array matches = value.getArray();
        List<Rule> rules = new ArrayList<>(matches.getSize());
        for (int i = 0; matches.getValue(i, value); ++i) {
            String[] tuple = value.getStringArray();
            int distance = Integer.parseInt(tuple[2]);
            boolean oneway = tuple.length >= 4 && tuple[3].equals("1");
            List<String> desired = new ArrayList<>(bar.splitToList(tuple[0]));
            List<String> supported = new ArrayList<>(bar.splitToList(tuple[1]));
            int size = desired.size();
            if (size != supported.size()) {
                throw new IllegalArgumentException("uneven languageMatches pair");
            }
            if (size < prevSize) {
                throw new IllegalArgumentException("languageMatches out of order");
            }
            prevSize = size;
            // Implementation shortcuts assume:
            // - At any level, either both or neither rule subtags are *.
            // - If the rule language subtags are *, the other-level subtags must also be *.
            // If there are rules that do not fit these constraints,
            // then we need to revise the implementation.
            int langStars = checkStars(desired.get(0), supported.get(0), false);
            if (size >= 2) {
                checkStars(desired.get(1), supported.get(1), langStars == 2);
            }
            if (size == 3) {
                checkStars(desired.get(2), supported.get(2), langStars == 2);
                rmb.ensureRegionIsVariable(desired);
                rmb.ensureRegionIsVariable(supported);
            }
            rules.add(new Rule(desired, supported, distance, oneway));
        }

        rmb.build();

        /**
         * Used for processing rules. At the start we have a variable setting like $A1=US+CA+MX.
         * We generate a mapping from $A1 to a set of partitions {P1, P2}
         * When we hit a rule that contains a variable,
         * we replace that rule by multiple rules for the partitions.
         */
        final Multimap<String, String> variableToPartition = rmb.variableToPartitions;

        final DistanceTable defaultDistanceTable = new DistanceTable(-1);
        for (Rule rule : rules) {
            List<String> desired = rule.desired;
            List<String> supported = rule.supported;
            if (rule.desired.size() <= 2) {
                // language-only or language-script
                add(defaultDistanceTable, desired, supported, rule.distance);
                if (!rule.oneway && !desired.equals(supported)) {
                    add(defaultDistanceTable, supported, desired, rule.distance);
                }
            } else {
                // language-script-region
                Collection<String> desiredRegions = getIdsFromVariable(variableToPartition, desired.get(2));
                Collection<String> supportedRegions = getIdsFromVariable(variableToPartition, supported.get(2));
                for (String desiredRegion2 : desiredRegions) {
                    desired.set(2, desiredRegion2.toString()); // fix later
                    for (String supportedRegion2 : supportedRegions) {
                        supported.set(2, supportedRegion2.toString()); // fix later
                        add(defaultDistanceTable, desired, supported, rule.distance);
                        if (!rule.oneway) {
                            add(defaultDistanceTable, supported, desired, rule.distance);
                        }
                    }
                }
            }
        }

        TrieBuilder trieBuilder = new TrieBuilder();
        defaultDistanceTable.toTrie(trieBuilder);
        BytesTrie trie = trieBuilder.build();
        return new LocaleDistance(
                trie, rmb.regionToPartitionsIndex, rmb.partitionArrays, paradigmLSRs);
    }

    private static int checkStars(String desired, String supported, boolean allStars) {
        int stars = (desired.equals("*") ? 1 : 0) + (supported.equals("*") ? 1 : 0);
        if (stars == 1) {
            throw new IllegalArgumentException("either both or neither rule subtags must be *: " +
                    desired + ", " + supported);
        }
        if (allStars && stars != 2) {
            throw new IllegalArgumentException("both language subtags are * --> " +
                    "both rule subtags on all levels must be *: " +
                    desired + ", " + supported);
        }
        return stars;
    }

    private static void add(DistanceTable languageDesired2Supported,
            List<String> desired, List<String> supported, int percentage) {
        int size = desired.size();
        if (size != supported.size() || size < 1 || size > 3) {
            throw new IllegalArgumentException();
        }
        final String desiredLang = fixAny(desired.get(0));
        final String supportedLang = fixAny(supported.get(0));
        if (size == 1) {
            languageDesired2Supported.addSubtable(desiredLang, supportedLang, percentage);
        } else {
            final String desiredScript = fixAny(desired.get(1));
            final String supportedScript = fixAny(supported.get(1));
            if (size == 2) {
                languageDesired2Supported.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, percentage);
            } else {
                final String desiredRegion = fixAny(desired.get(2));
                final String supportedRegion = fixAny(supported.get(2));
                languageDesired2Supported.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, desiredRegion, supportedRegion, percentage);
            }
        }
    }

    private static final class RegionMapperBuilder {
        private final Set<String> variables = new HashSet<>();
        final private Multimap<String, String> regionToRawPartition = TreeMultimap.create();
        final private RegionSet regionSet;
        private final TerritoryContainment tc;

        // build() output
        Multimap<String, String> variableToPartitions;
        private byte[] regionToPartitionsIndex;
        private String[][] partitionArrays;

        RegionMapperBuilder(TerritoryContainment tc) {
            regionSet = new RegionSet(tc);
            this.tc = tc;
        }

        private boolean isKnownVariable(String variable) {
            return variables.contains(variable) || variable.equals("*");
        }

        void add(String variable, String barString) {
            assert !isKnownVariable(variable);
            assert variable.startsWith("$");
            assert !variable.startsWith("$!");
            variables.add(variable);
            Set<String> tempRegions = regionSet.parseSet(barString);

            for (String region : tempRegions) {
                regionToRawPartition.put(region, variable);
            }

            // now add the inverse variable

            Set<String> inverse = regionSet.inverse();
            String inverseVariable = "$!" + variable.substring(1);
            assert !isKnownVariable(inverseVariable);
            variables.add(inverseVariable);
            for (String region : inverse) {
                regionToRawPartition.put(region, inverseVariable);
            }
        }

        void ensureRegionIsVariable(List<String> lsrList) {
            String region = lsrList.get(2);
            if (!isKnownVariable(region)) {
                assert LSR.indexForRegion(region) >= 0;  // well-formed region subtag
                String variable = "$" + region;
                add(variable, region);
                lsrList.set(2, variable);
            }
        }

        void build() {
            // Partitions as sets of variables.
            // LinkedHashMap to store & number unique sets.
            // Example: {"$!cnsar", "$!enUS", "$!maghreb", "$americas"}
            Map<Collection<String>, Integer> partitionVariables = new LinkedHashMap<>();
            // Partitions as sets of lookup ID strings.
            // Example: {"1", "5"}
            Map<Collection<String>, Integer> partitionStrings = new LinkedHashMap<>();
            // pIndex 0: default value in regionToPartitionsIndex
            Collection<String> noPartitions = Collections.singleton("");
            makeUniqueIndex(partitionStrings, noPartitions);

            // Example: "$americas" -> {"1", "5"}
            variableToPartitions = TreeMultimap.create();
            // Maps the index of each region code to a pIndex into partitionStrings.
            regionToPartitionsIndex = new byte[LSR.REGION_INDEX_LIMIT];
            // Maps a partition string to the set of region codes in that partition.
            // Example: "5" -> {"PR", "US", "VI"}
            Multimap<String, String> partitionToRegions = TreeMultimap.create();

            for (Map.Entry<String, Set<String>> e : regionToRawPartition.asMap().entrySet()) {
                final String region = e.getKey();
                final Collection<String> rawPartition = e.getValue();
                // Single-character string.
                // Must be an ASCII character and must not be '*'.
                // Used to start with α.
                char partitionChar = (char) ('0' + makeUniqueIndex(partitionVariables, rawPartition));
                assert partitionChar <= 0x7f;
                String partition = String.valueOf(partitionChar);
                int pIndex = makeUniqueIndex(partitionStrings, Collections.singleton(partition));
                // The pIndex must fit into a byte.
                // For Java code simplicity, we want it to also be non-negative.
                assert pIndex <= 0x7f;

                regionToPartitionsIndex[LSR.indexForRegion(region)] = (byte) pIndex;
                partitionToRegions.put(partition, region);

                for (String variable : rawPartition) {
                    variableToPartitions.put(variable, partition);
                }
            }

            // We get a mapping of each macro to the partitions it intersects with.
            // Example: "419" -> {"1", "5"}
            Multimap<String,String> macroToPartitions = TreeMultimap.create();
            for (Map.Entry<String, Set<String>> e : tc.resolved.asMap().entrySet()) {
                String macro = e.getKey();
                for (Map.Entry<String, Set<String>> e2 : partitionToRegions.asMap().entrySet()) {
                    String partition = e2.getKey();
                    if (!Collections.disjoint(e.getValue(), e2.getValue())) {
                        macroToPartitions.put(macro, partition);
                    }
                }
            }

            // Create a combined mapping from a region code, which can be a macro region,
            // via the getRegionIndex() of that region code,
            // to a set of single-character partition strings.
            for (Map.Entry<String, Set<String>> m2p : macroToPartitions.asMap().entrySet()) {
                String macro = m2p.getKey();
                int regionIndex = LSR.indexForRegion(macro);
                if (regionToPartitionsIndex[regionIndex] == 0) {
                    Set<String> partitions = m2p.getValue();
                    int pIndex = makeUniqueIndex(partitionStrings, partitions);
                    regionToPartitionsIndex[regionIndex] = (byte) pIndex;
                }
            }

            // Turn the Collection of Collections into an array of arrays.
            Collection<Collection<String>> list = partitionStrings.keySet();
            partitionArrays = new String[list.size()][];
            int i = 0;
            for (Collection<String> partitions : list) {
                partitionArrays[i++] = partitions.toArray(new String[partitions.size()]);
            }
        }
    }

    /**
     * Parses a string of regions like "US+005-BR" and produces a set of resolved regions.
     * All macroregions are fully resolved to sets of non-macro regions.
     * <br>Syntax is simple for now:
     * <pre>regionSet := region ([-+] region)*</pre>
     * No precedence, so "x+y-y+z" is (((x+y)-y)+z) NOT (x+y)-(y+z)
     */
    private static final class RegionSet {
        private enum Operation {add, remove}
        private final TerritoryContainment tc;
        // temporaries used in processing
        final private Set<String> tempRegions = new TreeSet<>();
        private Operation operation = null;

        RegionSet(TerritoryContainment tc) {
            this.tc = tc;
        }

        private Set<String> parseSet(String barString) {
            operation = Operation.add;
            int last = 0;
            tempRegions.clear();
            int i = 0;
            for (; i < barString.length(); ++i) {
                char c = barString.charAt(i); // UTF16 is ok, since syntax is only ascii
                switch(c) {
                case '+':
                    add(barString, last, i);
                    last = i+1;
                    operation = Operation.add;
                    break;
                case '-':
                    add(barString, last, i);
                    last = i+1;
                    operation = Operation.remove;
                    break;
                }
            }
            add(barString, last, i);
            return tempRegions;
        }

        private Set<String> inverse() {
            TreeSet<String> result = new TreeSet<>(tc.leaves);
            result.removeAll(tempRegions);
            return result;
        }

        private void add(String barString, int last, int i) {
            if (i > last) {
                String region = barString.substring(last,i);
                changeSet(operation, region);
            }
        }

        private void changeSet(Operation operation, String region) {
            Collection<String> contained = tc.toLeavesOnly.get(region);
            if (contained != null && !contained.isEmpty()) {
                if (Operation.add == operation) {
                    tempRegions.addAll(contained);
                } else {
                    tempRegions.removeAll(contained);
                }
            } else if (Operation.add == operation) {
                tempRegions.add(region);
            } else {
                tempRegions.remove(region);
            }
        }
    }
}
