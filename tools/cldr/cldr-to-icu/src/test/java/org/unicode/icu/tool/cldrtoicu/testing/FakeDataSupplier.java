// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

/**
 * Fake data supplier for testing (especially the path value mappers).
 */
public final class FakeDataSupplier extends CldrDataSupplier {
    private final Map<CldrPath, CldrValue> nonLocaleData = new LinkedHashMap<>();
    private final Table<String, CldrPath, CldrValue> unresolvedData = TreeBasedTable.create();
    private final Map<String, String> parentLocales = new HashMap<>();

    public FakeDataSupplier addLocaleData(String localeId, CldrValue... values) {
        Arrays.stream(values).forEach(v -> {
            unresolvedData.put(localeId, v.getPath(), v);
        });
        return this;
    }

    public FakeDataSupplier setLocaleParent(String localeId, String parentId) {
        parentLocales.put(localeId, parentId);
        return this;
    }

    public FakeDataSupplier addSupplementalData(CldrValue... values) {
        Arrays.stream(values).forEach(v -> nonLocaleData.put(v.getPath(), v));
        return this;
    }

    @Override public CldrData getDataForLocale(String localeId, CldrResolution resolution) {
        Collection<CldrValue> values;
        if (resolution == CldrResolution.UNRESOLVED) {
            values = unresolvedData.row(localeId).values();
        } else {
            // This is not "real" resolving since it doesn't handle aliases etc. but it's good
            // enough for tests.
            Map<CldrPath, CldrValue> resolved = new HashMap<>();
            while (true) {
                unresolvedData.row(localeId).forEach((p, v) -> {
                    if (!resolved.containsKey(p)) {
                        resolved.put(p, v);
                    }
                });
                if (localeId.equals("root")) {
                    break;
                }
                localeId = parentLocales.getOrDefault(localeId, "root");
            }
            values = resolved.values();
        }
        return CldrDataSupplier.forValues(values);
    }

    @Override public CldrData getDataForType(CldrDataType type) {
        return CldrDataSupplier.forValues(
            Iterables.filter(nonLocaleData.values(), v -> v.getPath().getDataType() == type));
    }

    @Override public Set<String> getAvailableLocaleIds() {
        return Collections.unmodifiableSet(
            Sets.union(unresolvedData.rowKeySet(), ImmutableSet.of("root")));
    }

    @Override public CldrDataSupplier withDraftStatusAtLeast(CldrDraftStatus cldrDraftStatus) {
        throw new UnsupportedOperationException("not supported in fake data supplier");
    }
}
