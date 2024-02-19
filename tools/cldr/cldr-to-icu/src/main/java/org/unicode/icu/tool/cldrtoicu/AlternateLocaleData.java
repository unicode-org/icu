// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.unicode.cldr.api.CldrDataType.LDML;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

/**
 * A factory for data suppliers which can filter CLDR values by substituting values from one path
 * to another. The replaced value must retain the original "target" path but will have the value
 * and value attributes of the "source". A value will only be replaced if both the source and
 * target paths have associated values. The replacement retains its original position in the value
 * ordering.
 *
 * <p>This class DOES NOT transform supplemental or BCP-47 data, because the use of "alt" values
 * is completely different for that data (it would require merging specific attributes together).
 *
 * <p>Note that this is not a general purpose transformation of CLDR data, since it is generally
 * not possible to "move" values between arbitrary paths. Target and source paths must be in the
 * same "namespace" (i.e. share the same element names) but attributes can differ.
 *
 * <p>Note also that the mapping is not recursive, so mapping {@code A -> B} and {@code B -> C}
 * will NOT cause {@code A} to be mapped to {@code C}.
 *
 * <p>Typically this class is expected to be used for selecting alternate values of locale data
 * based on the {@code "alt"} path attribute (e.g. selecting the short form of a region name).
 */
public final class AlternateLocaleData {
    /**
     * Returns a wrapped data supplier which will transform any {@link CldrValue}s according to the
     * supplied {@link CldrPath} mapping. Keys in the path map are the "target" paths of values to
     * be modified, and the values in the map are the "source" paths from which the replacement
     * values are obtained. For each map entry, the target and source paths must be in the same
     * namespace (i.e. have the same path element names).
     */
    public static CldrDataSupplier transform(
        CldrDataSupplier src,
        Map<CldrPath, CldrPath> globalAltPaths,
        Table<String, CldrPath, CldrPath> localeAltPaths) {
        return new CldrDataFilter(src, globalAltPaths, localeAltPaths);
    }

    private static final class CldrDataFilter extends CldrDataSupplier {
        private final CldrDataSupplier src;
        // Mapping from target (destination) to source path. This is necessary since two targets
        // could come from the same source).
        private final ImmutableMap<CldrPath, CldrPath> globalAltPaths;
        private final ImmutableTable<String, CldrPath, CldrPath> localeAltPaths;

        CldrDataFilter(
        CldrDataSupplier src,
        Map<CldrPath, CldrPath> globalAltPaths,
        Table<String, CldrPath, CldrPath> localeAltPaths) {
            this.src = checkNotNull(src);
            this.globalAltPaths = ImmutableMap.copyOf(globalAltPaths);
            this.localeAltPaths = ImmutableTable.copyOf(localeAltPaths);
            this.globalAltPaths
                .forEach((t, s) -> checkArgument(hasSameNamespace(checkLdml(t), checkLdml(s)),
                "alternate paths must have the same namespace: target=%s, source=%s", t, s));
            this.localeAltPaths.cellSet()
                .forEach(c -> checkArgument(
                    hasSameNamespace(checkLdml(c.getColumnKey()), checkLdml(c.getValue())),
                "alternate paths must have the same namespace: locale=%s, target=%s, source=%s",
                    c.getRowKey(), c.getColumnKey(), c.getValue()));
        }

        @Override
        public CldrDataSupplier withDraftStatusAtLeast(CldrDraftStatus draftStatus) {
            return new CldrDataFilter(
                src.withDraftStatusAtLeast(draftStatus), globalAltPaths, localeAltPaths);
        }

        @Override
        public CldrData getDataForLocale(String localeId, CldrResolution resolution) {
            return new AltData(src.getDataForLocale(localeId, resolution), localeId);
        }

        @Override
        public Set<String> getAvailableLocaleIds() {
            return src.getAvailableLocaleIds();
        }

        @Override
        public CldrData getDataForType(CldrDataType type) {
            return src.getDataForType(type);
        }

        private final class AltData extends FilteredData {
            // Calculated per locale/data instance to make lookup as fast as possible.
            private final ImmutableMap<CldrPath, CldrPath> altPaths;
            // Any source paths which are not also target paths are removed. This is legacy
            // behaviour inherited from the original build tools, the reason for which is not
            // known. If it becomes desirable to retain the source values in their original
            // locations, this can just be removed.
            private final ImmutableSet<CldrPath> toRemove;

            AltData(CldrData srcData, String localeId) {
                super(srcData);
                ImmutableMap<CldrPath, CldrPath> altPaths  = globalAltPaths;
                if (!localeAltPaths.row(localeId).isEmpty()) {
                    Map<CldrPath, CldrPath> combinedPaths = new HashMap<>();
                    // Locale specific path mappings overwrite global ones.
                    combinedPaths.putAll(globalAltPaths);
                    combinedPaths.putAll(localeAltPaths.row(localeId));
                    altPaths = ImmutableMap.copyOf(combinedPaths);
                }
                this.altPaths = altPaths;
                this.toRemove = altPaths.entrySet().stream()
                    // Only remove source paths that are not also target paths...
                    .filter(e -> !this.altPaths.containsKey(e.getValue()))
                    // ... and if the target path it will be transformed to actually exists.
                    .filter(e -> getSourceData().get(e.getKey()) != null)
                    // The value in the mapping is the source path (it's target->source for lookup).
                    .map(Map.Entry::getValue)
                    .collect(toImmutableSet());
            }

            @Override
            protected CldrValue filter(CldrValue value) {
                CldrPath altPath = altPaths.get(value.getPath());
                if (altPath != null) {
                    CldrValue altValue = getSourceData().get(altPath);
                    if (altValue != null) {
                        return altValue.replacePath(value.getPath());
                    }
                }
                return toRemove.contains(value.getPath()) ? null : value;
            }
        }
    }

    private static boolean hasSameNamespace(CldrPath x, CldrPath y) {
        if (x.getLength() != y.getLength()) {
            return false;
        }
        do {
            if (!x.getName().equals(y.getName())) {
                return false;
            }
            x = x.getParent();
            y = y.getParent();
        } while (x != null);
        return true;
    }

    private static CldrPath checkLdml(CldrPath path) {
        checkArgument(path.getDataType() == LDML, "only locale data (LDML) is supported: %s", path);
        return path;
    }

    private AlternateLocaleData() {}
}
