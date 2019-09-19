// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

/**
 * Fake data supplier for testing (especially the path value mappers).
 */
public final class FakeDataSupplier extends CldrDataSupplier {
    private final Map<CldrPath, CldrValue> nonLocaleData = new LinkedHashMap<>();
    private final Table<String, CldrPath, CldrValue> unresolvedData = TreeBasedTable.create();
    private final Table<String, CldrPath, CldrValue> resolvedData = TreeBasedTable.create();

    public FakeDataSupplier addLocaleData(String localeId, CldrValue... values) {
        Arrays.stream(values).forEach(v -> {
            unresolvedData.put(localeId, v.getPath(), v);
            resolvedData.put(localeId, v.getPath(), v);
        });
        return this;
    }

    public FakeDataSupplier addInheritedData(String localeId, CldrValue... values) {
        Arrays.stream(values)
            .forEach(v -> checkArgument(resolvedData.put(localeId, v.getPath(), v) == null,
                "path already present in unresolved CLDR data: %s", v.getPath()));
        return this;
    }

    public FakeDataSupplier addSupplementalData(CldrValue... values) {
        Arrays.stream(values).forEach(v -> nonLocaleData.put(v.getPath(), v));
        return this;
    }

    @Override public CldrData getDataForLocale(String localeId, CldrResolution resolution) {
        Table<String, CldrPath, CldrValue> data =
            resolution == CldrResolution.UNRESOLVED ? unresolvedData : resolvedData;
        return CldrDataSupplier.forValues(data.row(localeId).values());
    }

    @Override public CldrData getDataForType(CldrDataType type) {
        return CldrDataSupplier.forValues(
            Iterables.filter(nonLocaleData.values(), v -> v.getPath().getDataType() == type));
    }

    @Override public Set<String> getAvailableLocaleIds() {
        return Collections.unmodifiableSet(resolvedData.rowKeySet());
    }

    @Override public CldrDataSupplier withDraftStatusAtLeast(CldrDraftStatus cldrDraftStatus) {
        throw new UnsupportedOperationException("not supported in fake data supplier");
    }
}
