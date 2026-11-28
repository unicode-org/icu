// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataType.BCP47;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.TreeMap;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.PathMatcher;

/**
 * A mapper to collect the region associated with each time zone from {@link CldrDataType#BCP47
 * BCP47} data under paths matching:
 *
 * <pre>{@code
 * //ldmlBCP47/keyword/key[@name="tz"]/type[@name=*]
 * }</pre>
 *
 * <p>As described in <a href="https://unicode.org/reports/tr35/#Time_Zone_Identifiers">LDML</a>,
 * the first two letters of a length 5 short identifier double as the time zone's associated
 * region, unless the time zone has an explicit {@code region} attribute that overrides this. Short
 * identifiers of length not equal to 5 are not associated with a region, unless the time zone has
 * an explicit {@code region} attribute. The {@code alias} attribute lists the TZ database IDs of a
 * short identifier, the first of which is the CLDR canonical ID.
 *
 * <p>This data is not used at runtime by ICU and is deliberately not converted into any packaged
 * resource bundle. It is emitted as a standalone, line-oriented text file which is only read
 * offline by {@code icu4c/source/tools/tzcode/tz2icu} when it generates {@code zoneinfo64.txt},
 * whose "Regions" array it becomes.
 */
public final class ZoneRegionsMapper {
    private static final PathMatcher TZ_KEY =
            PathMatcher.of("//ldmlBCP47/keyword/key[@name=\"tz\"]");
    private static final String TYPE = "type";

    private static final AttributeKey TYPE_NAME = keyOf(TYPE, "name");
    private static final AttributeKey TYPE_ALIASES = keyOf(TYPE, "alias");
    private static final AttributeKey TYPE_REGION = keyOf(TYPE, "region");

    // The length of a short identifier whose first two letters are the region code.
    private static final int REGION_PREFIXED_LENGTH = 5;

    // UN M.49 code for "World", used for zones which are not associated with a region.
    private static final String NO_REGION = "001";

    private static final String FIELD_SEPARATOR = "\t";

    /**
     * Processes data from the given supplier to generate the zone/region rows.
     *
     * @param src the CLDR data supplier to process.
     * @return one tab separated row per TZ database zone ID, ordered by zone ID.
     */
    public static ImmutableList<String> process(CldrDataSupplier src) {
        return process(src.getDataForType(BCP47));
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static ImmutableList<String> process(CldrData data) {
        TreeMap<String, String> regionsByZoneId = new TreeMap<>();
        data.accept(
                DTD,
                value -> {
                    CldrPath path = value.getPath();
                    if (!path.getName().equals(TYPE) || !TZ_KEY.matchesPrefixOf(path)) {
                        return;
                    }
                    // Deprecated short identifiers which were replaced by a preferred one (e.g.
                    // "umjon" by "ushnl") have no alias, and their zone IDs are listed by the
                    // preferred identifier instead.
                    List<String> zoneIds = TYPE_ALIASES.listOfValuesFrom(value);
                    if (zoneIds.isEmpty()) {
                        return;
                    }
                    String shortId = TYPE_NAME.valueFrom(value);
                    String region =
                            TYPE_REGION
                                    .optionalValueFrom(value)
                                    .orElseGet(
                                            () ->
                                                    shortId.length() == REGION_PREFIXED_LENGTH
                                                            ? Ascii.toUpperCase(
                                                                    shortId.substring(0, 2))
                                                            : NO_REGION);
                    zoneIds.forEach(id -> regionsByZoneId.put(id, region));
                });
        return regionsByZoneId.entrySet().stream()
                .map(e -> e.getKey() + FIELD_SEPARATOR + e.getValue())
                .collect(toImmutableList());
    }

    private ZoneRegionsMapper() {}
}
