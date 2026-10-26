// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.Optional;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.PathMatcher;

/**
 * A mapper to collect the standard and daylight-savings offsets which are optionally associated
 * with metazone usage in {@link CldrDataType#SUPPLEMENTAL SUPPLEMENTAL} data via the paths:
 *
 * <pre>{@code
 * //supplementalData/metaZones/metazoneInfo/timezone/usesMetazone
 * }</pre>
 *
 * <p>These offsets record what CLDR believes the offsets of a time zone to have been during the
 * given period, which can differ from the TZ database (typically because the TZ database only
 * models "wall clock" time, or because it retroactively changes how a historic offset is split
 * into standard and daylight-savings parts).
 *
 * <p>This data is not used at runtime by ICU and is deliberately not converted into any packaged
 * resource bundle. It is emitted as a standalone, line-oriented text file which is only read
 * offline by {@code icu4c/source/tools/tzcode/tz2icu} when it generates {@code zoneinfo64.txt}.
 */
public final class MetazoneOffsetsMapper {
    private static final PathMatcher TIMEZONE =
            PathMatcher.of("//supplementalData/metaZones/metazoneInfo/timezone[@type=*]");
    private static final String USES_METAZONE = "usesMetazone";

    private static final AttributeKey TIMEZONE_TYPE = keyOf("timezone", "type");
    private static final AttributeKey FROM = keyOf(USES_METAZONE, "from");
    private static final AttributeKey TO = keyOf(USES_METAZONE, "to");
    private static final AttributeKey STD_OFFSET = keyOf(USES_METAZONE, "stdOffset");
    private static final AttributeKey DST_OFFSET = keyOf(USES_METAZONE, "dstOffset");

    // The values implied by CLDR when the (optional) "from"/"to" attributes are absent. Writing
    // them out explicitly keeps every row in the generated file the same shape.
    private static final String DEFAULT_FROM = "1970-01-01 00:00";
    private static final String DEFAULT_TO = "9999-12-31 23:59";

    private static final String FIELD_SEPARATOR = "\t";

    /**
     * Processes data from the given supplier to generate the metazone offset rows.
     *
     * @param src the CLDR data supplier to process.
     * @return one tab separated row per metazone usage with explicit offsets, ordered by zone ID
     *     and then by start time.
     */
    public static ImmutableList<String> process(CldrDataSupplier src) {
        return process(src.getDataForType(SUPPLEMENTAL));
    }

    @VisibleForTesting // It's easier to supply a fake data instance than a fake supplier.
    static ImmutableList<String> process(CldrData data) {
        // Rows are sorted by zone ID and then lexicographically, which (since a row starts with
        // the fixed width "from" timestamp) also orders the rows of a zone chronologically.
        SortedSetMultimap<String, String> rowsByZoneId = TreeMultimap.create();
        data.accept(
                DTD,
                value -> {
                    CldrPath path = value.getPath();
                    if (!path.getName().equals(USES_METAZONE) || !TIMEZONE.matchesPrefixOf(path)) {
                        return;
                    }
                    // Both offsets are optional, and only a small number of metazone usages
                    // specify them (they are only present where CLDR differs from the TZ data).
                    Optional<String> stdOffset = STD_OFFSET.optionalValueFrom(value);
                    Optional<String> dstOffset = DST_OFFSET.optionalValueFrom(value);
                    if (!stdOffset.isPresent() || !dstOffset.isPresent()) {
                        return;
                    }
                    rowsByZoneId.put(
                            TIMEZONE_TYPE.valueFrom(value),
                            String.join(
                                    FIELD_SEPARATOR,
                                    FROM.valueFrom(value, DEFAULT_FROM),
                                    TO.valueFrom(value, DEFAULT_TO),
                                    stdOffset.get(),
                                    dstOffset.get()));
                });
        return rowsByZoneId.entries().stream()
                .map(e -> e.getKey() + FIELD_SEPARATOR + e.getValue())
                .collect(toImmutableList());
    }

    private MetazoneOffsetsMapper() {}
}
