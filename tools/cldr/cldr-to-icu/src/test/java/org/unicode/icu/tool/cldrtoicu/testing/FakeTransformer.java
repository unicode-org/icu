// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import java.util.HashMap;
import java.util.Map;

import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.collect.ImmutableList;

public final class FakeTransformer extends PathValueTransformer {
    private final Map<CldrValue, ImmutableList<Result>> resultMap = new HashMap<>();
    private final Map<RbPath, ImmutableList<Result>> fallbackMap = new HashMap<>();

    public void addResults(CldrValue value, Result... results) {
        resultMap.put(value, ImmutableList.copyOf(results));
    }

    public void addFallbacks(String path, Result... results) {
        fallbackMap.put(RbPath.parse(path), ImmutableList.copyOf(results));
    }

    @Override public ImmutableList<Result> transform(CldrValue value) {
        return resultMap.getOrDefault(value, ImmutableList.of());
    }

    @Override public ImmutableList<Result> transform(CldrValue value, DynamicVars ignored) {
        return resultMap.getOrDefault(value, ImmutableList.of());
    }

    @Override public ImmutableList<Result> getFallbackResultsFor(RbPath key, DynamicVars ignored) {
        return fallbackMap.getOrDefault(key, ImmutableList.of());
    }
}
