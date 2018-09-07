// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.locale.XCldrStub.CollectionUtilities;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMultimap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableSet;
import com.ibm.icu.impl.locale.XCldrStub.LinkedHashMultimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimaps;
import com.ibm.icu.impl.locale.XCldrStub.Predicate;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.impl.locale.XCldrStub.TreeMultimap;
import com.ibm.icu.impl.locale.XLikelySubtags.LSR;
import com.ibm.icu.impl.locale.XLocaleDistance.RegionMapper.Builder;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundleIterator;

public class XLocaleDistance {

    static final boolean PRINT_OVERRIDES = false;

    public static final int ABOVE_THRESHOLD = 100;

    // Activates debugging output to stderr with details of GetBestMatch.
    // Be sure to set this to false before checking this in for production!
    private static final boolean TRACE_DISTANCE = false;

    @Deprecated
    public static final String ANY = "�"; // matches any character. Uses value above any subtag.

    private static String fixAny(String string) {
        return "*".equals(string) ? ANY : string;
    }

    static final LocaleDisplayNames english = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    private static List<R4<String, String, Integer, Boolean>> xGetLanguageMatcherData() {
        List<R4<String, String, Integer, Boolean>> distanceList = new ArrayList<>();

        ICUResourceBundle suppData = LocaleMatcher.getICUSupplementalData();
        ICUResourceBundle languageMatchingNew = suppData.findTopLevel("languageMatchingNew");
        ICUResourceBundle written = (ICUResourceBundle) languageMatchingNew.get("written");

        for(UResourceBundleIterator iter = written.getIterator(); iter.hasNext();) {
            ICUResourceBundle item = (ICUResourceBundle) iter.next();
            boolean oneway = item.getSize() > 3 && "1".equals(item.getString(3));
            distanceList.add(
                    (R4<String, String, Integer, Boolean>)            // note: .freeze returning wrong type, so casting.
                    Row.of(
                            item.getString(0),
                            item.getString(1),
                            Integer.parseInt(item.getString(2)),
                            oneway)
                    .freeze());
        }
        return Collections.unmodifiableList(distanceList);
    }

    @SuppressWarnings("unused")
    private static Set<String> xGetParadigmLocales() {
        ICUResourceBundle suppData = LocaleMatcher.getICUSupplementalData();
        ICUResourceBundle languageMatchingInfo = suppData.findTopLevel("languageMatchingInfo");
        ICUResourceBundle writtenParadigmLocales = (ICUResourceBundle) languageMatchingInfo.get("written")
                .get("paradigmLocales");
        //      paradigmLocales{ "en", "en-GB",... }
        HashSet<String> paradigmLocales = new HashSet<>(Arrays.asList(writtenParadigmLocales.getStringArray()));
        return Collections.unmodifiableSet(paradigmLocales);
    }

    @SuppressWarnings("unused")
    private static Map<String, String> xGetMatchVariables() {
        ICUResourceBundle suppData = LocaleMatcher.getICUSupplementalData();
        ICUResourceBundle languageMatchingInfo = suppData.findTopLevel("languageMatchingInfo");
        ICUResourceBundle writtenMatchVariables = (ICUResourceBundle) languageMatchingInfo.get("written")
                .get("matchVariable");
        //        matchVariable{ americas{"019"} cnsar{"HK+MO"} ...}

        HashMap<String,String> matchVariables = new HashMap<>();
        for (Enumeration<String> enumer = writtenMatchVariables.getKeys(); enumer.hasMoreElements(); ) {
            String key = enumer.nextElement();
            matchVariables.put(key, writtenMatchVariables.getString(key));
        }
        return Collections.unmodifiableMap(matchVariables);
    }

    private static Multimap<String, String> xGetContainment() {
        TreeMultimap<String,String> containment = TreeMultimap.create();
        containment
        .putAll("001", "019", "002", "150", "142", "009")
        .putAll("011", "BF", "BJ", "CI", "CV", "GH", "GM", "GN", "GW", "LR", "ML", "MR", "NE", "NG", "SH", "SL", "SN", "TG")
        .putAll("013", "BZ", "CR", "GT", "HN", "MX", "NI", "PA", "SV")
        .putAll("014", "BI", "DJ", "ER", "ET", "KE", "KM", "MG", "MU", "MW", "MZ", "RE", "RW", "SC", "SO", "SS", "TZ", "UG", "YT", "ZM", "ZW")
        .putAll("142", "145", "143", "030", "034", "035")
        .putAll("143", "TM", "TJ", "KG", "KZ", "UZ")
        .putAll("145", "AE", "AM", "AZ", "BH", "CY", "GE", "IL", "IQ", "JO", "KW", "LB", "OM", "PS", "QA", "SA", "SY", "TR", "YE", "NT", "YD")
        .putAll("015", "DZ", "EG", "EH", "LY", "MA", "SD", "TN", "EA", "IC")
        .putAll("150", "154", "155", "151", "039")
        .putAll("151", "BG", "BY", "CZ", "HU", "MD", "PL", "RO", "RU", "SK", "UA", "SU")
        .putAll("154", "GG", "IM", "JE", "AX", "DK", "EE", "FI", "FO", "GB", "IE", "IS", "LT", "LV", "NO", "SE", "SJ")
        .putAll("155", "AT", "BE", "CH", "DE", "FR", "LI", "LU", "MC", "NL", "DD", "FX")
        .putAll("017", "AO", "CD", "CF", "CG", "CM", "GA", "GQ", "ST", "TD", "ZR")
        .putAll("018", "BW", "LS", "NA", "SZ", "ZA")
        .putAll("019", "021", "013", "029", "005", "003", "419")
        .putAll("002", "015", "011", "017", "014", "018")
        .putAll("021", "BM", "CA", "GL", "PM", "US")
        .putAll("029", "AG", "AI", "AW", "BB", "BL", "BQ", "BS", "CU", "CW", "DM", "DO", "GD", "GP", "HT", "JM", "KN", "KY", "LC", "MF", "MQ", "MS", "PR", "SX", "TC", "TT", "VC", "VG", "VI", "AN")
        .putAll("003", "021", "013", "029")
        .putAll("030", "CN", "HK", "JP", "KP", "KR", "MN", "MO", "TW")
        .putAll("035", "BN", "ID", "KH", "LA", "MM", "MY", "PH", "SG", "TH", "TL", "VN", "BU", "TP")
        .putAll("039", "AD", "AL", "BA", "ES", "GI", "GR", "HR", "IT", "ME", "MK", "MT", "RS", "PT", "SI", "SM", "VA", "XK", "CS", "YU")
        .putAll("419", "013", "029", "005")
        .putAll("005", "AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PE", "PY", "SR", "UY", "VE")
        .putAll("053", "AU", "NF", "NZ")
        .putAll("054", "FJ", "NC", "PG", "SB", "VU")
        .putAll("057", "FM", "GU", "KI", "MH", "MP", "NR", "PW")
        .putAll("061", "AS", "CK", "NU", "PF", "PN", "TK", "TO", "TV", "WF", "WS")
        .putAll("034", "AF", "BD", "BT", "IN", "IR", "LK", "MV", "NP", "PK")
        .putAll("009", "053", "054", "057", "061", "QO")
        .putAll("QO", "AQ", "BV", "CC", "CX", "GS", "HM", "IO", "TF", "UM", "AC", "CP", "DG", "TA")
        ;
        //Can't use following, because data from CLDR is discarded
        //        ICUResourceBundle suppData = LocaleMatcher.getICUSupplementalData();
        //        UResourceBundle territoryContainment = suppData.get("territoryContainment");
        //        for (int i = 0 ; i < territoryContainment.getSize(); i++) {
        //            UResourceBundle mapping = territoryContainment.get(i);
        //            String parent = mapping.getKey();
        //            for (int j = 0 ; j < mapping.getSize(); j++) {
        //                String child = mapping.getString(j);
        //                containment.put(parent,child);
        //                System.out.println(parent + " => " + child);
        //            }
        //        }
        TreeMultimap<String,String> containmentResolved = TreeMultimap.create();
        fill("001", containment, containmentResolved);
        return ImmutableMultimap.copyOf(containmentResolved);
    }

    private static Set<String> fill(String region, TreeMultimap<String, String> containment, Multimap<String, String> toAddTo) {
        Set<String> contained = containment.get(region);
        if (contained == null) {
            return Collections.emptySet();
        }
        toAddTo.putAll(region, contained); // do top level
        // then recursively
        for (String subregion : contained) {
            toAddTo.putAll(region, fill(subregion, containment, toAddTo));
        }
        return toAddTo.get(region);
    }


    static final Multimap<String,String> CONTAINER_TO_CONTAINED;
    static final Multimap<String,String> CONTAINER_TO_CONTAINED_FINAL;
    static {
        //         Multimap<String, String> containerToContainedTemp = xGetContainment();
        //         fill(Region.getInstance("001"), containerToContainedTemp);

        CONTAINER_TO_CONTAINED = xGetContainment();
        Multimap<String, String> containerToFinalContainedBuilder = TreeMultimap.create();
        for (Entry<String, Set<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            String container = entry.getKey();
            for (String contained : entry.getValue()) {
                if (CONTAINER_TO_CONTAINED.get(contained) == null) {
                    containerToFinalContainedBuilder.put(container, contained);
                }
            }
        }
        CONTAINER_TO_CONTAINED_FINAL = ImmutableMultimap.copyOf(containerToFinalContainedBuilder);
    }

    final static private Set<String> ALL_FINAL_REGIONS = ImmutableSet.copyOf(CONTAINER_TO_CONTAINED_FINAL.get("001"));

    // end of data from CLDR

    private final DistanceTable languageDesired2Supported;
    private final RegionMapper regionMapper;
    private final int defaultLanguageDistance;
    private final int defaultScriptDistance;
    private final int defaultRegionDistance;

    @Deprecated
    public static abstract class DistanceTable {
        abstract int getDistance(String desiredLang, String supportedlang, Output<DistanceTable> table, boolean starEquals);
        abstract Set<String> getCloser(int threshold);
        abstract String toString(boolean abbreviate);
        public DistanceTable compact() {
            return this;
        }
        //        public Integer getInternalDistance(String a, String b) {
        //            return null;
        //        }
        public DistanceNode getInternalNode(String any, String any2) {
            return null;
        }
        public Map<String, Set<String>> getInternalMatches() {
            return null;
        }
        public boolean isEmpty() {
            return true;
        }
    }

    @Deprecated
    public static class DistanceNode {
        final int distance;

        public DistanceNode(int distance) {
            this.distance = distance;
        }

        public DistanceTable getDistanceTable() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj ||
                    (obj != null
                    && obj.getClass() == this.getClass()
                    && distance == ((DistanceNode) obj).distance);
        }
        @Override
        public int hashCode() {
            return distance;
        }
        @Override
        public String toString() {
            return "\ndistance: " + distance;
        }
    }

    private interface IdMapper<K,V> {
        public V toId(K source);
    }

    static class IdMakerFull<T> implements IdMapper<T,Integer> {
        private final Map<T, Integer> objectToInt = new HashMap<>();
        private final List<T> intToObject = new ArrayList<>();
        final String name; // for debugging

        IdMakerFull(String name) {
            this.name = name;
        }

        IdMakerFull() {
            this("unnamed");
        }

        IdMakerFull(String name, T zeroValue) {
            this(name);
            add(zeroValue);
        }

        /**
         * Return an id, making one if there wasn't one already.
         */
        public Integer add(T source) {
            Integer result = objectToInt.get(source);
            if (result == null) {
                Integer newResult = intToObject.size();
                objectToInt.put(source, newResult);
                intToObject.add(source);
                return newResult;
            } else {
                return result;
            }
        }

        /**
         * Return an id, or null if there is none.
         */
        @Override
        public Integer toId(T source) {
            return objectToInt.get(source);
            //            return value == null ? 0 : value;
        }

        /**
         * Return the object for the id, or null if there is none.
         */
        public T fromId(int id) {
            return intToObject.get(id);
        }

        /**
         * Return interned object
         */
        public T intern(T source) {
            return fromId(add(source));
        }

        public int size() {
            return intToObject.size();
        }
        /**
         * Same as add, except if the object didn't have an id, return null;
         */
        public Integer getOldAndAdd(T source) {
            Integer result = objectToInt.get(source);
            if (result == null) {
                Integer newResult = intToObject.size();
                objectToInt.put(source, newResult);
                intToObject.add(source);
            }
            return result;
        }

        @Override
        public String toString() {
            return size() + ": " + intToObject;
        }
        @Override
        public boolean equals(Object obj) {
            return this == obj ||
                    (obj != null
                    && obj.getClass() == this.getClass()
                    && intToObject.equals(((IdMakerFull<?>) obj).intToObject));
        }
        @Override
        public int hashCode() {
            return intToObject.hashCode();
        }
    }

    static class StringDistanceNode extends DistanceNode {
        final DistanceTable distanceTable;

        public StringDistanceNode(int distance, DistanceTable distanceTable) {
            super(distance);
            this.distanceTable = distanceTable;
        }

        @Override
        public boolean equals(Object obj) {
            StringDistanceNode other;
            return this == obj ||
                    (obj != null
                    && obj.getClass() == this.getClass()
                    && distance == (other = (StringDistanceNode) obj).distance
                    && Objects.equals(distanceTable, other.distanceTable)
                    && super.equals(other));
        }
        @Override
        public int hashCode() {
            return distance ^ Objects.hashCode(distanceTable);
        }

        StringDistanceNode(int distance) {
            this(distance, new StringDistanceTable());
        }

        public void addSubtables(String desiredSub, String supportedSub, CopyIfEmpty r) {
            ((StringDistanceTable) distanceTable).addSubtables(desiredSub, supportedSub, r);
        }
        @Override
        public String toString() {
            return "distance: " + distance + "\n" + distanceTable;
        }

        public void copyTables(StringDistanceTable value) {
            if (value != null) {
                ((StringDistanceTable)distanceTable).copy(value);
            }
        }

        @Override
        public DistanceTable getDistanceTable() {
            return distanceTable;
        }
    }

    public XLocaleDistance(DistanceTable datadistancetable2, RegionMapper regionMapper) {
        languageDesired2Supported = datadistancetable2;
        this.regionMapper = regionMapper;

        StringDistanceNode languageNode = (StringDistanceNode) ((StringDistanceTable) languageDesired2Supported).subtables.get(ANY).get(ANY);
        defaultLanguageDistance = languageNode.distance;
        StringDistanceNode scriptNode = (StringDistanceNode) ((StringDistanceTable)languageNode.distanceTable).subtables.get(ANY).get(ANY);
        defaultScriptDistance = scriptNode.distance;
        DistanceNode regionNode = ((StringDistanceTable)scriptNode.distanceTable).subtables.get(ANY).get(ANY);
        defaultRegionDistance = regionNode.distance;
    }

    @SuppressWarnings("rawtypes")
    private static Map newMap() { // for debugging
        return new TreeMap();
    }

    /**
     * Internal class
     */
    @Deprecated
    public static class StringDistanceTable extends DistanceTable {
        final Map<String, Map<String, DistanceNode>> subtables;

        StringDistanceTable(Map<String, Map<String, DistanceNode>> tables) {
            subtables = tables;
        }
        @SuppressWarnings("unchecked")
        StringDistanceTable() {
            this(newMap());
        }

        @Override
        public boolean isEmpty() {
            return subtables.isEmpty();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj ||
                    (obj != null
                    && obj.getClass() == this.getClass()
                    && subtables.equals(((StringDistanceTable) obj).subtables));
        }
        @Override
        public int hashCode() {
            return subtables.hashCode();
        }

        @Override
        public int getDistance(String desired, String supported, Output<DistanceTable> distanceTable, boolean starEquals) {
            if (TRACE_DISTANCE) {
                System.err.printf("    Entering       getDistance: desired=%s supported=%s starEquals=%s\n",
                    desired, supported, Boolean.toString(starEquals));
            }
            boolean star = false;
            Map<String, DistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                sub2 = subtables.get(ANY); // <*, supported>
                star = true;
            }
            DistanceNode value = sub2.get(supported);   // <*/desired, supported>
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
                distanceTable.value = ((StringDistanceNode) value).distanceTable;
            }
            int result = starEquals && star && desired.equals(supported) ? 0 : value.distance;
            if (TRACE_DISTANCE) {
                System.err.printf("    Returning from getDistance: %d\n", result);
            }
            return result;
        }

        public void copy(StringDistanceTable other) {
            for (Entry<String, Map<String, DistanceNode>> e1 : other.subtables.entrySet()) {
                for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                    DistanceNode value = e2.getValue();
                    @SuppressWarnings("unused")
                    DistanceNode subNode = addSubtable(e1.getKey(), e2.getKey(), value.distance);
                }
            }
        }

        @SuppressWarnings("unchecked")
        DistanceNode addSubtable(String desired, String supported, int distance) {
            Map<String, DistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                subtables.put(desired, sub2 = newMap());
            }
            DistanceNode oldNode = sub2.get(supported);
            if (oldNode != null) {
                return oldNode;
            }

            final StringDistanceNode newNode = new StringDistanceNode(distance);
            sub2.put(supported, newNode);
            return newNode;
        }

        /**
         * Return null if value doesn't exist
         */
        private DistanceNode getNode(String desired, String supported) {
            Map<String, DistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                return null;
            }
            return sub2.get(supported);
        }


        /** add table for each subitem that matches and doesn't have a table already
         */
        public void addSubtables(
                String desired, String supported,
                Predicate<DistanceNode> action) {
            DistanceNode node = getNode(desired, supported);
            if (node == null) {
                // get the distance it would have
                Output<DistanceTable> node2 = new Output<>();
                int distance = getDistance(desired, supported, node2, true);
                // now add it
                node = addSubtable(desired, supported, distance);
                if (node2.value != null) {
                    ((StringDistanceNode)node).copyTables((StringDistanceTable)(node2.value));
                }
            }
            action.test(node);
        }

        public void addSubtables(String desiredLang, String supportedLang,
                String desiredScript, String supportedScript,
                int percentage) {

            // add to all the values that have the matching desiredLang and supportedLang
            @SuppressWarnings("unused")
            boolean haveKeys = false;
            for (Entry<String, Map<String, DistanceNode>> e1 : subtables.entrySet()) {
                String key1 = e1.getKey();
                final boolean desiredIsKey = desiredLang.equals(key1);
                if (desiredIsKey || desiredLang.equals(ANY)) {
                    for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals(ANY)) {
                            DistanceNode value = e2.getValue();
                            ((StringDistanceTable)value.getDistanceTable()).addSubtable(desiredScript, supportedScript, percentage);
                        }
                    }
                }
            }
            // now add the sequence explicitly
            StringDistanceTable dt = new StringDistanceTable();
            dt.addSubtable(desiredScript, supportedScript, percentage);
            CopyIfEmpty r = new CopyIfEmpty(dt);
            addSubtables(desiredLang, supportedLang, r);
        }

        public void addSubtables(String desiredLang, String supportedLang,
                String desiredScript, String supportedScript,
                String desiredRegion, String supportedRegion,
                int percentage) {

            // add to all the values that have the matching desiredLang and supportedLang
            @SuppressWarnings("unused")
            boolean haveKeys = false;
            for (Entry<String, Map<String, DistanceNode>> e1 : subtables.entrySet()) {
                String key1 = e1.getKey();
                final boolean desiredIsKey = desiredLang.equals(key1);
                if (desiredIsKey || desiredLang.equals(ANY)) {
                    for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals(ANY)) {
                            StringDistanceNode value = (StringDistanceNode) e2.getValue();
                            ((StringDistanceTable)value.distanceTable).addSubtables(desiredScript, supportedScript, desiredRegion, supportedRegion, percentage);
                        }
                    }
                }
            }
            // now add the sequence explicitly

            StringDistanceTable dt = new StringDistanceTable();
            dt.addSubtable(desiredRegion, supportedRegion, percentage);
            AddSub r = new AddSub(desiredScript, supportedScript, dt);
            addSubtables(desiredLang,  supportedLang,  r);
        }

        @Override
        public String toString() {
            return toString(false);
        }

        @Override
        public String toString(boolean abbreviate) {
            return toString(abbreviate, "", new IdMakerFull<>("interner"), new StringBuilder()).toString();
        }

        public StringBuilder toString(boolean abbreviate, String indent, IdMakerFull<Object> intern, StringBuilder buffer) {
            String indent2 = indent.isEmpty() ? "" : "\t";
            Integer id = abbreviate ? intern.getOldAndAdd(subtables) : null;
            if (id != null) {
                buffer.append(indent2).append('#').append(id).append('\n');
            } else for (Entry<String, Map<String, DistanceNode>> e1 : subtables.entrySet()) {
                final Map<String, DistanceNode> subsubtable = e1.getValue();
                buffer.append(indent2).append(e1.getKey());
                String indent3 = "\t";
                id = abbreviate ? intern.getOldAndAdd(subsubtable) : null;
                if (id != null) {
                    buffer.append(indent3).append('#').append(id).append('\n');
                } else for (Entry<String, DistanceNode> e2 : subsubtable.entrySet()) {
                    DistanceNode value = e2.getValue();
                    buffer.append(indent3).append(e2.getKey());
                    id = abbreviate ? intern.getOldAndAdd(value) : null;
                    if (id != null) {
                        buffer.append('\t').append('#').append(id).append('\n');
                    } else {
                        buffer.append('\t').append(value.distance);
                        final DistanceTable distanceTable = value.getDistanceTable();
                        if (distanceTable != null) {
                            id = abbreviate ? intern.getOldAndAdd(distanceTable) : null;
                            if (id != null) {
                                buffer.append('\t').append('#').append(id).append('\n');
                            } else {
                                ((StringDistanceTable)distanceTable).toString(abbreviate, indent+"\t\t\t", intern, buffer);
                                buffer.append('\n');
                            }
                        } else {
                            buffer.append('\n');
                        }
                    }
                    indent3 = indent+'\t';
                }
                indent2 = indent;
            }
            return buffer;
        }

        @Override
        public StringDistanceTable compact() {
            return new CompactAndImmutablizer().compact(this);
        }

        @Override
        public Set<String> getCloser(int threshold) {
            Set<String> result = new HashSet<>();
            for (Entry<String, Map<String, DistanceNode>> e1 : subtables.entrySet()) {
                String desired = e1.getKey();
                for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                    if (e2.getValue().distance < threshold) {
                        result.add(desired);
                        break;
                    }
                }
            }
            return result;
        }

        public Integer getInternalDistance(String a, String b) {
            Map<String, DistanceNode> subsub = subtables.get(a);
            if (subsub == null) {
                return null;
            }
            DistanceNode dnode = subsub.get(b);
            return dnode == null ? null : dnode.distance;
        }

        @Override
        public DistanceNode getInternalNode(String a, String b) {
            Map<String, DistanceNode> subsub = subtables.get(a);
            if (subsub == null) {
                return null;
            }
            return subsub.get(b);
        }

        @Override
        public Map<String, Set<String>> getInternalMatches() {
            Map<String, Set<String>> result = new LinkedHashMap<>();
            for (Entry<String, Map<String, DistanceNode>> entry : subtables.entrySet()) {
                result.put(entry.getKey(), new LinkedHashSet<>(entry.getValue().keySet()));
            }
            return result;
        }
    }

    static class CopyIfEmpty implements Predicate<DistanceNode> {
        private final StringDistanceTable toCopy;
        CopyIfEmpty(StringDistanceTable resetIfNotNull) {
            this.toCopy = resetIfNotNull;
        }
        @Override
        public boolean test(DistanceNode node) {
            final StringDistanceTable subtables = (StringDistanceTable) node.getDistanceTable();
            if (subtables.subtables.isEmpty()) {
                subtables.copy(toCopy);
            }
            return true;
        }
    }

    static class AddSub implements Predicate<DistanceNode> {
        private final String desiredSub;
        private final String supportedSub;
        private final CopyIfEmpty r;

        AddSub(String desiredSub, String supportedSub, StringDistanceTable distanceTableToCopy) {
            this.r = new CopyIfEmpty(distanceTableToCopy);
            this.desiredSub = desiredSub;
            this.supportedSub = supportedSub;
        }
        @Override
        public boolean test(DistanceNode node) {
            if (node == null) {
                throw new IllegalArgumentException("bad structure");
            } else {
                ((StringDistanceNode)node).addSubtables(desiredSub, supportedSub, r);
            }
            return true;
        }
    }

    public int distance(ULocale desired, ULocale supported, int threshold, DistanceOption distanceOption) {
        LSR supportedLSR = LSR.fromMaximalized(supported);
        LSR desiredLSR = LSR.fromMaximalized(desired);
        return distanceRaw(desiredLSR, supportedLSR, threshold, distanceOption);
    }

    /**
     * Returns distance, from 0 to ABOVE_THRESHOLD.
     * ULocales must be in canonical, addLikelySubtags format. Returns distance
     */
    public int distanceRaw(LSR desired, LSR supported, int threshold, DistanceOption distanceOption) {
        if (TRACE_DISTANCE) {
            System.err.printf("  Entering       distanceRaw: desired=%s supported=%s "
            + "threshold=%d preferred=%s\n",
            desired, supported, threshold,
            distanceOption.name());
        }
        int result = distanceRaw(desired.language, supported.language,
                desired.script, supported.script,
                desired.region, supported.region,
                threshold, distanceOption);
        if (TRACE_DISTANCE) {
            System.err.printf("  Returning from distanceRaw: %d\n", result);
        }
        return result;
    }

    public enum DistanceOption {REGION_FIRST, SCRIPT_FIRST}
    // NOTE: Replaced "NORMAL" with "REGION_FIRST". By default, scripts have greater weight
    // than regions, so they might be considered the "normal" case.

    /**
     * Returns distance, from 0 to ABOVE_THRESHOLD.
     * ULocales must be in canonical, addLikelySubtags format.
     * (Exception: internal calls may pass any strings. They do this for pseudo-locales.)
     * Returns distance.
     */
    public int distanceRaw(
            String desiredLang, String supportedLang,
            String desiredScript, String supportedScript,
            String desiredRegion, String supportedRegion,
            int threshold,
            DistanceOption distanceOption) {

        Output<DistanceTable> subtable = new Output<>();

        int distance = languageDesired2Supported.getDistance(desiredLang, supportedLang, subtable, true);
        boolean scriptFirst = distanceOption == DistanceOption.SCRIPT_FIRST;
        if (scriptFirst) {
            distance >>= 2;
        }
        if (distance < 0) {
            distance = 0;
        } else if (distance >= threshold) {
            return ABOVE_THRESHOLD;
        }

        int scriptDistance = subtable.value.getDistance(desiredScript, supportedScript, subtable, true);
        if (scriptFirst) {
            scriptDistance >>= 1;
        }
        distance += scriptDistance;
        if (distance >= threshold) {
            return ABOVE_THRESHOLD;
        }

        if (desiredRegion.equals(supportedRegion)) {
            return distance;
        }

        // From here on we know the regions are not equal

        final String desiredPartition = regionMapper.toId(desiredRegion);
        final String supportedPartition = regionMapper.toId(supportedRegion);
        int subdistance;

        // check for macros. If one is found, we take the maximum distance
        // this could be optimized by adding some more structure, but probably not worth it.

        Collection<String> desiredPartitions = desiredPartition.isEmpty() ? regionMapper.macroToPartitions.get(desiredRegion) : null;
        Collection<String> supportedPartitions = supportedPartition.isEmpty() ? regionMapper.macroToPartitions.get(supportedRegion) : null;
        if (desiredPartitions != null || supportedPartitions != null) {
            subdistance = 0;
            // make the code simple for now
            if (desiredPartitions == null) {
                desiredPartitions = Collections.singleton(desiredPartition);
            }
            if (supportedPartitions == null) {
                supportedPartitions = Collections.singleton(supportedPartition);
            }

            for (String desiredPartition2 : desiredPartitions) {
                for (String supportedPartition2 : supportedPartitions) {
                    int tempSubdistance = subtable.value.getDistance(desiredPartition2, supportedPartition2, null, false);
                    if (subdistance < tempSubdistance) {
                        subdistance = tempSubdistance;
                    }
                }
            }
        } else {
            subdistance = subtable.value.getDistance(desiredPartition, supportedPartition, null, false);
        }
        distance += subdistance;
        return distance >= threshold ? ABOVE_THRESHOLD : distance;
    }


    private static final XLocaleDistance DEFAULT;

    public static XLocaleDistance getDefault() {
        return DEFAULT;
    }

    static {
        String[][] variableOverrides = {
                {"$enUS", "AS+GU+MH+MP+PR+UM+US+VI"},

                {"$cnsar", "HK+MO"},

                {"$americas", "019"},

                {"$maghreb", "MA+DZ+TN+LY+MR+EH"},
        };
        String[] paradigmRegions = {
                "en", "en-GB", "es", "es-419", "pt-BR", "pt-PT"
        };
        String[][] regionRuleOverrides = {
                {"ar_*_$maghreb", "ar_*_$maghreb", "96"},
                {"ar_*_$!maghreb", "ar_*_$!maghreb", "96"},
                {"ar_*_*", "ar_*_*", "95"},

                {"en_*_$enUS", "en_*_$enUS", "96"},
                {"en_*_$!enUS", "en_*_$!enUS", "96"},
                {"en_*_*", "en_*_*", "95"},

                {"es_*_$americas", "es_*_$americas", "96"},
                {"es_*_$!americas", "es_*_$!americas", "96"},
                {"es_*_*", "es_*_*", "95"},

                {"pt_*_$americas", "pt_*_$americas", "96"},
                {"pt_*_$!americas", "pt_*_$!americas", "96"},
                {"pt_*_*", "pt_*_*", "95"},

                {"zh_Hant_$cnsar", "zh_Hant_$cnsar", "96"},
                {"zh_Hant_$!cnsar", "zh_Hant_$!cnsar", "96"},
                {"zh_Hant_*", "zh_Hant_*", "95"},

                {"*_*_*", "*_*_*", "96"},
        };

        Builder rmb = new RegionMapper.Builder().addParadigms(paradigmRegions);
        for (String[] variableRule : variableOverrides) {
            rmb.add(variableRule[0], variableRule[1]);
        }
        if (PRINT_OVERRIDES) {
            System.out.println("\t\t<languageMatches type=\"written\" alt=\"enhanced\">");
            System.out.println("\t\t\t<paradigmLocales locales=\"" + XCldrStub.join(paradigmRegions, " ")
            + "\"/>");
            for (String[] variableRule : variableOverrides) {
                System.out.println("\t\t\t<matchVariable id=\"" + variableRule[0]
                        + "\" value=\""
                        + variableRule[1]
                                + "\"/>");
            }
        }

        final StringDistanceTable defaultDistanceTable = new StringDistanceTable();
        final RegionMapper defaultRegionMapper = rmb.build();

        Splitter bar = Splitter.on('_');

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Row.R4<List<String>, List<String>, Integer, Boolean>>[] sorted = new ArrayList[3];
        sorted[0] = new ArrayList<>();
        sorted[1] = new ArrayList<>();
        sorted[2] = new ArrayList<>();

        // sort the rules so that the language-only are first, then the language-script, and finally the language-script-region.
        for (R4<String, String, Integer, Boolean> info : xGetLanguageMatcherData()) {
            String desiredRaw = info.get0();
            String supportedRaw = info.get1();
            List<String> desired = bar.splitToList(desiredRaw);
            List<String> supported = bar.splitToList(supportedRaw);
            Boolean oneway = info.get3();
            int distance = desiredRaw.equals("*_*") ? 50 : info.get2();
            int size = desired.size();

            // for now, skip size == 3
            if (size == 3) continue;

            sorted[size-1].add(Row.of(desired, supported, distance, oneway));
        }

        for (List<Row.R4<List<String>, List<String>, Integer, Boolean>> item1 : sorted) {
            for (Row.R4<List<String>, List<String>, Integer, Boolean> item2 : item1) {
                List<String> desired = item2.get0();
                List<String> supported = item2.get1();
                Integer distance = item2.get2();
                Boolean oneway = item2.get3();
                add(defaultDistanceTable, desired, supported, distance);
                if (oneway != Boolean.TRUE && !desired.equals(supported)) {
                    add(defaultDistanceTable, supported, desired, distance);
                }
                printMatchXml(desired, supported, distance, oneway);
            }
        }

        // add new size=3
        for (String[] rule : regionRuleOverrides) {
            //            if (PRINT_OVERRIDES) System.out.println("\t\t\t<languageMatch desired=\""
            //                + rule[0]
            //                    + "\" supported=\""
            //                    + rule[1]
            //                        + "\" distance=\""
            //                        + rule[2]
            //                            + "\"/>");
            //            if (rule[0].equals("en_*_*") || rule[1].equals("*_*_*")) {
            //                int debug = 0;
            //            }
            List<String> desiredBase = new ArrayList<>(bar.splitToList(rule[0]));
            List<String> supportedBase = new ArrayList<>(bar.splitToList(rule[1]));
            Integer distance = 100-Integer.parseInt(rule[2]);
            printMatchXml(desiredBase, supportedBase, distance, false);

            Collection<String> desiredRegions = defaultRegionMapper.getIdsFromVariable(desiredBase.get(2));
            if (desiredRegions.isEmpty()) {
                throw new IllegalArgumentException("Bad region variable: " + desiredBase.get(2));
            }
            Collection<String> supportedRegions = defaultRegionMapper.getIdsFromVariable(supportedBase.get(2));
            if (supportedRegions.isEmpty()) {
                throw new IllegalArgumentException("Bad region variable: " + supportedBase.get(2));
            }
            for (String desiredRegion2 : desiredRegions) {
                desiredBase.set(2, desiredRegion2.toString()); // fix later
                for (String supportedRegion2 : supportedRegions) {
                    supportedBase.set(2, supportedRegion2.toString()); // fix later
                    add(defaultDistanceTable, desiredBase, supportedBase, distance);
                    add(defaultDistanceTable, supportedBase, desiredBase, distance);
                }
            }
        }

        // Pseudo regions should match no other regions.
        // {"*-*-XA", "*-*-*", "0"},
        // {"*-*-XB", "*-*-*", "0"},
        // {"*-*-XC", "*-*-*", "0"},
        // {"x1-*-*", "*-*-*", "0"},
        // {"x2-*-*", "*-*-*", "0"},
        // ...
        // {"x8-*-*", "*-*-*", "0"},
        List<String> supported = Arrays.asList("*", "*", "*");
        for (String x : Arrays.asList("XA", "XB", "XC")) {
            List<String> desired = Arrays.asList("*", "*", x);
            add(defaultDistanceTable, desired, supported, 100);
            add(defaultDistanceTable, supported, desired, 100);
        }
        // See XLikelySubtags.java for the mapping of pseudo-locales to x1 ... x8.
        for (int i = 1; i <= 8; ++i) {
            List<String> desired = Arrays.asList("x" + String.valueOf(i), "*", "*");
            add(defaultDistanceTable, desired, supported, 100);
            add(defaultDistanceTable, supported, desired, 100);
        }

        if (PRINT_OVERRIDES) {
            System.out.println("\t\t</languageMatches>");
        }

        DEFAULT = new XLocaleDistance(defaultDistanceTable.compact(), defaultRegionMapper);

        if (PRINT_OVERRIDES) {
            System.out.println(defaultRegionMapper);
            System.out.println(defaultDistanceTable);
            throw new IllegalArgumentException();
        }
    }

    private static void printMatchXml(List<String> desired, List<String> supported, Integer distance, Boolean oneway) {
        if (PRINT_OVERRIDES) {
            String desiredStr = CollectionUtilities.join(desired, "_");
            String supportedStr = CollectionUtilities.join(supported, "_");
            String desiredName = fixedName(desired);
            String supportedName = fixedName(supported);
            System.out.println("\t\t\t<languageMatch"
                    + " desired=\"" + desiredStr
                    + "\"\tsupported=\"" + supportedStr
                    + "\"\tdistance=\"" + distance
                    + (!oneway ? "" : "\"\toneway=\"true")
                    + "\"/>\t<!-- " + desiredName + " ⇒ " + supportedName + " -->");
        }
    }

    private static String fixedName(List<String> match) {
        List<String> alt = new ArrayList<>(match);
        int size = alt.size();
        assert size >= 1 && size <= 3;

        StringBuilder result = new StringBuilder();

        if (size >= 3) {
            String region = alt.get(2);
            if (region.equals("*") || region.startsWith("$")) {
                result.append(region);
            } else {
                result.append(english.regionDisplayName(region));
            }
        }
        if (size >= 2) {
            String script = alt.get(1);
            if (script.equals("*")) {
                result.insert(0, script);
            } else {
                result.insert(0, english.scriptDisplayName(script));
            }
        }
        if (size >= 1) {
            String language = alt.get(0);
            if (language.equals("*")) {
                result.insert(0, language);
            } else {
                result.insert(0, english.languageDisplayName(language));
            }
        }
        return CollectionUtilities.join(alt, "; ");
    }

    static public void add(StringDistanceTable languageDesired2Supported, List<String> desired, List<String> supported, int percentage) {
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

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean abbreviate) {
        return regionMapper + "\n" + languageDesired2Supported.toString(abbreviate);
    }


    //    public static XLocaleDistance createDefaultInt() {
    //        IntDistanceTable d = new IntDistanceTable(DEFAULT_DISTANCE_TABLE);
    //        return new XLocaleDistance(d, DEFAULT_REGION_MAPPER);
    //    }

    static Set<String> getContainingMacrosFor(Collection<String> input, Set<String> output) {
        output.clear();
        for (Entry<String, Set<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            if (input.containsAll(entry.getValue())) { // example; if all southern Europe are contained, then add S. Europe
                output.add(entry.getKey());
            }
        }
        return output;
    }

    static class RegionMapper implements IdMapper<String,String> {
        /**
         * Used for processing rules. At the start we have a variable setting like $A1=US+CA+MX. We generate a mapping from $A1 to a set of partitions {P1, P2}
         * When we hit a rule that contains a variable, we replace that rule by multiple rules for the partitions.
         */
        final Multimap<String,String> variableToPartition;
        /**
         * Used for executing the rules. We map a region to a partition before processing.
         */
        final Map<String,String> regionToPartition;
        /**
         * Used to support es_419 compared to es_AR, etc.
         */
        final Multimap<String,String> macroToPartitions;
        /**
         * Used to get the paradigm region for a cluster, if there is one
         */
        final Set<ULocale> paradigms;

        private RegionMapper(
                Multimap<String, String> variableToPartitionIn,
                Map<String, String> regionToPartitionIn,
                Multimap<String,String> macroToPartitionsIn,
                Set<ULocale> paradigmsIn) {
            variableToPartition = ImmutableMultimap.copyOf(variableToPartitionIn);
            regionToPartition = ImmutableMap.copyOf(regionToPartitionIn);
            macroToPartitions = ImmutableMultimap.copyOf(macroToPartitionsIn);
            paradigms = ImmutableSet.copyOf(paradigmsIn);
        }

        @Override
        public String toId(String region) {
            String result = regionToPartition.get(region);
            return result == null ? "" : result;
        }

        public Collection<String> getIdsFromVariable(String variable) {
            if (variable.equals("*")) {
                return Collections.singleton("*");
            }
            Collection<String> result = variableToPartition.get(variable);
            if (result == null || result.isEmpty()) {
                throw new IllegalArgumentException("Variable not defined: " + variable);
            }
            return result;
        }

        public Set<String> regions() {
            return regionToPartition.keySet();
        }

        public Set<String> variables() {
            return variableToPartition.keySet();
        }

        @Override
        public String toString() {
            TreeMultimap<String, String> partitionToVariables = Multimaps.invertFrom(variableToPartition,
                    TreeMultimap.<String, String>create());
            TreeMultimap<String, String> partitionToRegions = TreeMultimap.create();
            for (Entry<String, String> e : regionToPartition.entrySet()) {
                partitionToRegions.put(e.getValue(), e.getKey());
            }
            StringBuilder buffer = new StringBuilder();
            buffer.append("Partition ➠ Variables ➠ Regions (final)");
            for (Entry<String, Set<String>> e : partitionToVariables.asMap().entrySet()) {
                buffer.append('\n');
                buffer.append(e.getKey() + "\t" + e.getValue() + "\t" + partitionToRegions.get(e.getKey()));
            }
            buffer.append("\nMacro ➠ Partitions");
            for (Entry<String, Set<String>> e : macroToPartitions.asMap().entrySet()) {
                buffer.append('\n');
                buffer.append(e.getKey() + "\t" + e.getValue());
            }

            return buffer.toString();
        }

        static class Builder {
            final private Multimap<String, String> regionToRawPartition = TreeMultimap.create();
            final private RegionSet regionSet = new RegionSet();
            final private Set<ULocale> paradigms = new LinkedHashSet<>();

            void add(String variable, String barString) {
                Set<String> tempRegions = regionSet.parseSet(barString);

                for (String region : tempRegions) {
                    regionToRawPartition.put(region, variable);
                }

                // now add the inverse variable

                Set<String> inverse = regionSet.inverse();
                String inverseVariable = "$!" + variable.substring(1);
                for (String region : inverse) {
                    regionToRawPartition.put(region, inverseVariable);
                }
            }

            public Builder addParadigms(String... paradigmRegions) {
                for (String paradigm : paradigmRegions) {
                    paradigms.add(new ULocale(paradigm));
                }
                return this;
            }

            RegionMapper build() {
                final IdMakerFull<Collection<String>> id = new IdMakerFull<>("partition");
                Multimap<String,String> variableToPartitions = TreeMultimap.create();
                Map<String,String> regionToPartition = new TreeMap<>();
                Multimap<String,String> partitionToRegions = TreeMultimap.create();

                for (Entry<String, Set<String>> e : regionToRawPartition.asMap().entrySet()) {
                    final String region = e.getKey();
                    final Collection<String> rawPartition = e.getValue();
                    String partition = String.valueOf((char)('α' + id.add(rawPartition)));

                    regionToPartition.put(region, partition);
                    partitionToRegions.put(partition, region);

                    for (String variable : rawPartition) {
                        variableToPartitions.put(variable, partition);
                    }
                }

                // we get a mapping of each macro to the partitions it intersects with
                Multimap<String,String> macroToPartitions = TreeMultimap.create();
                for (Entry<String, Set<String>> e : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
                    String macro = e.getKey();
                    for (Entry<String, Set<String>> e2 : partitionToRegions.asMap().entrySet()) {
                        String partition = e2.getKey();
                        if (!Collections.disjoint(e.getValue(), e2.getValue())) {
                            macroToPartitions.put(macro, partition);
                        }
                    }
                }

                return new RegionMapper(
                        variableToPartitions,
                        regionToPartition,
                        macroToPartitions,
                        paradigms);
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
    private static class RegionSet {
        private enum Operation {add, remove}
        // temporaries used in processing
        final private Set<String> tempRegions = new TreeSet<>();
        private Operation operation = null;

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
            TreeSet<String> result = new TreeSet<>(ALL_FINAL_REGIONS);
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
            Collection<String> contained = CONTAINER_TO_CONTAINED_FINAL.get(region);
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

    public static <K,V> Multimap<K,V> invertMap(Map<V,K> map) {
        return Multimaps.invertFrom(Multimaps.forMap(map), LinkedHashMultimap.<K,V>create());
    }

    public Set<ULocale> getParadigms() {
        return regionMapper.paradigms;
    }

    public int getDefaultLanguageDistance() {
        return defaultLanguageDistance;
    }

    public int getDefaultScriptDistance() {
        return defaultScriptDistance;
    }

    public int getDefaultRegionDistance() {
        return defaultRegionDistance;
    }

    static class CompactAndImmutablizer extends IdMakerFull<Object> {
        StringDistanceTable compact(StringDistanceTable item) {
            if (toId(item) != null) {
                return (StringDistanceTable) intern(item);
            }
            return new StringDistanceTable(compact(item.subtables, 0));
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        <K,T> Map<K,T> compact(Map<K,T> item, int level) {
            if (toId(item) != null) {
                return (Map<K, T>) intern(item);
            }
            Map<K,T> copy = new LinkedHashMap<>();
            for (Entry<K,T> entry : item.entrySet()) {
                T value = entry.getValue();
                if (value instanceof Map) {
                    copy.put(entry.getKey(), (T)compact((Map)value, level+1));
                } else {
                    copy.put(entry.getKey(), (T)compact((DistanceNode)value));
                }
            }
            return ImmutableMap.copyOf(copy);
        }
        DistanceNode compact(DistanceNode item) {
            if (toId(item) != null) {
                return (DistanceNode) intern(item);
            }
            final DistanceTable distanceTable = item.getDistanceTable();
            if (distanceTable == null || distanceTable.isEmpty()) {
                return new DistanceNode(item.distance);
            } else {
                return new StringDistanceNode(item.distance, compact((StringDistanceTable)((StringDistanceNode)item).distanceTable));
            }
        }
    }

    @Deprecated
    public StringDistanceTable internalGetDistanceTable() {
        return (StringDistanceTable) languageDesired2Supported;
    }

    public static void main(String[] args) {
        //      for (Entry<String, Collection<String>> entry : containerToContained.asMap().entrySet()) {
        //          System.out.println(entry.getKey() + "\t⥢" + entry.getValue() + "; " + containerToFinalContained.get(entry.getKey()));
        //      }
        //      final Multimap<String,String> regionToMacros = ImmutableMultimap.copyOf(Multimaps.invertFrom(containerToContained, TreeMultimap.create()));
        //      for (Entry<String, Collection<String>> entry : regionToMacros.asMap().entrySet()) {
        //          System.out.println(entry.getKey() + "\t⥤ " + entry.getValue());
        //      }
        if (PRINT_OVERRIDES) {
            System.out.println(getDefault().toString(true));
        }
        DistanceTable table = getDefault().languageDesired2Supported;
        DistanceTable compactedTable = table.compact();
        if (!table.equals(compactedTable)) {
            throw new IllegalArgumentException("Compaction isn't equal");
        }
    }
}
