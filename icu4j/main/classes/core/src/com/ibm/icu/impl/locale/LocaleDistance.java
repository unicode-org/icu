// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.ULocale;

/**
 * Off-line-built data for LocaleMatcher.
 * Mostly but not only the data for mapping locales to their maximized forms.
 */
public class LocaleDistance {
    private static final int ABOVE_THRESHOLD = 100;

    private static final boolean DEBUG_OUTPUT = false;

    // The trie maps each dlang+slang+dscript+sscript+dregion+sregion
    // (encoded in ASCII with bit 7 set on the last character of each subtag) to a distance.
    // There is also a trie value for each subsequence of whole subtags.
    // One '*' is used for a (desired, supported) pair of "und", "Zzzz"/"", or "ZZ"/"".
    private final BytesTrie trie;

    /**
     * Maps each region to zero or more single-character partitions.
     */
    private final byte[] regionToPartitionsIndex;
    private final String[][] partitionArrays;

    /**
     * Used to get the paradigm region for a cluster, if there is one.
     */
    private final Set<LSR> paradigmLSRs;

    private final int defaultLanguageDistance;
    private final int defaultScriptDistance;
    private final int defaultRegionDistance;

    // TODO: Load prebuilt data from a resource bundle
    // to avoid the dependency on the builder code.
    // VisibleForTesting
    public static final LocaleDistance INSTANCE = LocaleDistanceBuilder.build();

    LocaleDistance(BytesTrie trie,
            byte[] regionToPartitionsIndex, String[][] partitionArrays,
            Set<LSR> paradigmLSRs) {
        this.trie = trie;
        if (DEBUG_OUTPUT) {
            System.out.println("*** locale distance");
            testOnlyPrintDistanceTable();
        }
        this.regionToPartitionsIndex = regionToPartitionsIndex;
        this.partitionArrays = partitionArrays;
        this.paradigmLSRs = paradigmLSRs;

        BytesTrie iter = new BytesTrie(trie);
        BytesTrie.Result result = iter.next('*');
        assert result == BytesTrie.Result.INTERMEDIATE_VALUE;
        defaultLanguageDistance = iter.getValue();
        result = iter.next('*');
        assert result == BytesTrie.Result.INTERMEDIATE_VALUE;
        defaultScriptDistance = iter.getValue();
        result = iter.next('*');
        assert result.hasValue();
        defaultRegionDistance = iter.getValue();
    }

    // VisibleForTesting
    public int testOnlyDistance(ULocale desired, ULocale supported,
            int threshold, DistanceOption distanceOption) {
        LSR supportedLSR = XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(supported);
        LSR desiredLSR = XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(desired);
        return getBestIndexAndDistance(desiredLSR, new LSR[] { supportedLSR },
                threshold, distanceOption) & 0xff;
    }

    public enum DistanceOption {REGION_FIRST, SCRIPT_FIRST}
    // NOTE: Replaced "NORMAL" with "REGION_FIRST". By default, scripts have greater weight
    // than regions, so they might be considered the "normal" case.

    /**
     * Finds the supported LSR with the smallest distance from the desired one.
     * Equivalent LSR subtags must be normalized into a canonical form.
     *
     * <p>Returns the index of the lowest-distance supported LSR in bits 31..8
     * (negative if none has a distance below the threshold),
     * and its distance (0..ABOVE_THRESHOLD) in bits 7..0.
     */
    int getBestIndexAndDistance(LSR desired, LSR[] supportedLsrs,
            int threshold, DistanceOption distanceOption) {
        BytesTrie iter = new BytesTrie(trie);
        // Look up the desired language only once for all supported LSRs.
        // Its "distance" is either a match point value of 0, or a non-match negative value.
        // Note: The data builder verifies that there are no <*, supported> or <desired, *> rules.
        // Set wantValue=true so that iter reads & skips the match point value.
        int desLangDistance = trieNext(iter, desired.language, true, true);
        long desLangState = desLangDistance >= 0 && supportedLsrs.length > 1 ? iter.getState64() : 0;
        // Index of the supported LSR with the lowest distance.
        int bestIndex = -1;
        for (int slIndex = 0; slIndex < supportedLsrs.length; ++slIndex) {
            LSR supported = supportedLsrs[slIndex];
            boolean star = false;
            int distance = desLangDistance;
            if (distance >= 0) {
                if (slIndex != 0) {
                    iter.resetToState64(desLangState);
                }
                distance = trieNext(iter, supported.language, true, true);
            }
            // Note: The data builder verifies that there are no rules with "any" (*) language and
            // real (non *) script or region subtags.
            // This means that if the lookup for either language fails we can use
            // the default distances without further lookups.
            if (distance < 0) {  // <*, *>
                if (desired.language.equals(supported.language)) {
                    distance = 0;
                } else {
                    distance = defaultLanguageDistance;
                }
                star = true;
            }
            assert 0 <= distance && distance <= 100;
            boolean scriptFirst = distanceOption == DistanceOption.SCRIPT_FIRST;
            if (scriptFirst) {
                distance >>= 2;
            }
            if (distance >= threshold) {
                continue;
            }

            int scriptDistance;
            if (star) {
                if (desired.script.equals(supported.script)) {
                    scriptDistance = 0;
                } else {
                    scriptDistance = defaultScriptDistance;
                }
            } else {
                scriptDistance = getDesSuppDistance(iter, iter.getState64(),
                        desired.script, supported.script, false);
            }
            if (scriptFirst) {
                scriptDistance >>= 1;
            }
            distance += scriptDistance;
            if (distance >= threshold) {
                continue;
            }

            if (desired.region.equals(supported.region)) {
                // regionDistance = 0
            } else if (star) {
                distance += defaultRegionDistance;
            } else {
                long startState = iter.getState64();

                // From here on we know the regions are not equal.
                // Map each region to zero or more partitions. (zero = one empty string)
                // If either side has more than one, then we find the maximum distance.
                // This could be optimized by adding some more structure, but probably not worth it.
                final String[] desiredPartitions = partitionsForRegion(desired);
                final String[] supportedPartitions = partitionsForRegion(supported);
                int regionDistance;

                if (desiredPartitions.length > 1 || supportedPartitions.length > 1) {
                    regionDistance = getRegionPartitionsDistance(iter, startState,
                            desiredPartitions, supportedPartitions, threshold - distance);
                } else {
                    regionDistance = getDesSuppDistance(iter, startState,
                            desiredPartitions[0], supportedPartitions[0], true);
                }
                distance += regionDistance;
            }
            if (distance < threshold) {
                if (distance == 0) {
                    return slIndex << 8;
                }
                bestIndex = slIndex;
                threshold = distance;
            }
        }
        return bestIndex >= 0 ? (bestIndex << 8) | threshold : 0xffffff00 | ABOVE_THRESHOLD;
    }

    private int getRegionPartitionsDistance(BytesTrie iter, long startState,
            String[] desiredPartitions, String[] supportedPartitions, int threshold) {
        int regionDistance = -1;
        for (String dp : desiredPartitions) {
            for (String sp : supportedPartitions) {
                if (regionDistance >= 0) {  // no need to reset in first iteration
                    iter.resetToState64(startState);
                }
                int d = getDesSuppDistance(iter, startState, dp, sp, true);
                if (regionDistance < d) {
                    if (d >= threshold) {
                        return d;
                    }
                    regionDistance = d;
                }
            }
        }
        assert regionDistance >= 0;
        return regionDistance;
    }

    // Modified from
    // DistanceTable#getDistance(desired, supported, Output distanceTable, starEquals).
    private static final int getDesSuppDistance(BytesTrie iter, long startState,
            String desired, String supported, boolean finalSubtag) {
        // Note: The data builder verifies that there are no <*, supported> or <desired, *> rules.
        int distance = trieNext(iter, desired, false, true);
        if (distance >= 0) {
            distance = trieNext(iter, supported, true, !finalSubtag);
        }
        if (distance < 0) {
            BytesTrie.Result result = iter.resetToState64(startState).next('*');  // <*, *>
            assert finalSubtag ? result.hasValue() : result == BytesTrie.Result.INTERMEDIATE_VALUE;
            if (!finalSubtag && desired.equals(supported)) {
                distance = 0;  // same language or script
            } else {
                distance = iter.getValue();
                assert distance >= 0;
            }
        }
        return distance;
    }

    private static final int trieNext(BytesTrie iter, String s, boolean wantValue, boolean wantNext) {
        if (s.isEmpty()) {
            return -1;  // no empty subtags in the distance data
        }
        BytesTrie.Result result;
        int end = s.length() - 1;
        for (int i = 0;; ++i) {
            int c = s.charAt(i);
            assert c <= 0x7f;
            if (i < end) {
                result = iter.next(c);
                if (!result.hasNext()) {
                    return -1;
                }
            } else {
                // last character of this subtag
                result = iter.next(c | 0x80);
                break;
            }
        }
        if (wantValue) {
            if (wantNext) {
                if (result == BytesTrie.Result.INTERMEDIATE_VALUE) {
                    return iter.getValue();
                }
            } else {
                if (result.hasValue()) {
                    return iter.getValue();
                }
            }
        } else {
            if (wantNext) {
                if (result == BytesTrie.Result.INTERMEDIATE_VALUE) {
                    return 0;
                }
            } else {
                if (result.hasValue()) {
                    return 0;
                }
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return testOnlyGetDistanceTable(true).toString();
    }

    private String[] partitionsForRegion(LSR lsr) {
        // ill-formed region -> one empty string
        int pIndex = lsr.regionIndex >= 0 ? regionToPartitionsIndex[lsr.regionIndex] : 0;
        return partitionArrays[pIndex];
    }

    boolean isParadigmLSR(LSR lsr) {
        return paradigmLSRs.contains(lsr);
    }

    // VisibleForTesting
    public int getDefaultScriptDistance() {
        return defaultScriptDistance;
    }

    int getDefaultRegionDistance() {
        return defaultRegionDistance;
    }

    // VisibleForTesting
    public Map<String, Integer> testOnlyGetDistanceTable(boolean skipIntermediateMatchPoints) {
        Map<String, Integer> map = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        for (BytesTrie.Entry entry : trie) {
            sb.setLength(0);
            int numSubtags = 0;
            int length = entry.bytesLength();
            for (int i = 0; i < length; ++i) {
                byte b = entry.byteAt(i);
                if (b == '*') {
                    // One * represents a (desired, supported) = (ANY, ANY) pair.
                    sb.append("*-*-");
                    numSubtags += 2;
                } else {
                    if (b >= 0) {
                        sb.append((char) b);
                    } else {  // end of subtag
                        sb.append((char) (b & 0x7f)).append('-');
                        ++numSubtags;
                    }
                }
            }
            assert sb.length() > 0 && sb.charAt(sb.length() - 1) == '-';
            if (!skipIntermediateMatchPoints || (numSubtags & 1) == 0) {
                sb.setLength(sb.length() - 1);
                String s = sb.toString();
                if (!skipIntermediateMatchPoints && s.endsWith("*-*")) {
                    // Re-insert single-ANY match points to show consistent structure
                    // for the test code.
                    map.put(s.substring(0, s.length() - 2), 0);
                }
                map.put(s, entry.value);
            }
        }
        return map;
    }

    // VisibleForTesting
    public void testOnlyPrintDistanceTable() {
        for (Map.Entry<String, Integer> mapping : testOnlyGetDistanceTable(true).entrySet()) {
            System.out.println(mapping);
        }
    }
}
