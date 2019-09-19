// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.collect.ListMultimap;

/**
 * An abstract parent class for any mappers based on {@code PathValueTransformer}. This ensures
 * that transformation results are correctly processed when being added to IcuData instances.
 */
public abstract class AbstractPathValueMapper {
    private static final Pattern ARRAY_INDEX = Pattern.compile("(/[^\\[]++)(?:\\[(\\d++)])?$");

    private final IcuData icuData;

    AbstractPathValueMapper(String name, boolean hasFallback) {
        this.icuData = new IcuData(name, hasFallback);
    }

    /** Implemented by sub-classes to return all results to be added to the IcuData instance. */
    abstract ListMultimap<RbPath, Result> getResults();

    /**
     * Adds results to the IcuData instance according to expected {@code PathValueTransformer}
     * semantics. This method must only be called once per mapper.
     */
    final IcuData transform() {
        checkState(icuData.getPaths().isEmpty(),
            "transform() method cannot be called multiple times: %s", icuData);

        // This subclass mostly exists to control the fact that results need to be added in one go
        // to the IcuData because of how referenced paths are handled. If results could be added in
        // multiple passes, you could have confusing situations in which values has path references
        // in them but the referenced paths have not been transformed yet. Forcing the subclass to
        // implement a single method to generate all results at once ensures that we control the
        // lifecycle of the data and how results are processed as they are added to the IcuData.
        addResults(getResults());
        return icuData;
    }

    /**
     * Adds transformation results on the specified multi-map to this data instance. Results are
     * handled differently according to whether they are grouped, or represent an alias value. If
     * the value of an ungrouped result is itself a resource bundle path (including possibly having
     * an array index) then the referenced value is assumed to be an existing path whose value is
     * then substituted.
     */
    // TODO: Fix this to NOT implicitly rely of ordering of referenced values.
    private void addResults(ListMultimap<RbPath, Result> resultsByRbPath) {
        for (RbPath rbPath : resultsByRbPath.keySet()) {
            for (Result r : resultsByRbPath.get(rbPath)) {
                if (r.isGrouped()) {
                    // Grouped results have all the values in a single value entry.
                    icuData.add(rbPath, RbValue.of(r.getValues()));
                } else {
                    if (rbPath.getSegment(rbPath.length() - 1).endsWith(":alias")) {
                        r.getValues().forEach(v -> icuData.add(rbPath, RbValue.of(v)));
                    } else {
                        // Ungrouped results are one value per entry, but might be expanded into
                        // grouped results if they are a path referencing a grouped entry.
                        r.getValues().forEach(v -> icuData.add(rbPath, replacePathValues(v)));
                    }
                }
            }
        }
    }

    /**
     * Replaces an ungrouped CLDR value for the form "/foo/bar" or "/foo/bar[N]" which is assumed
     * to be a reference to an existing value in a resource bundle. Note that the referenced bundle
     * might be grouped (i.e. an array with more than one element).
     */
    private RbValue replacePathValues(String value) {
        Matcher m = ARRAY_INDEX.matcher(value);
        if (!m.matches()) {
            return RbValue.of(value);
        }
        // The only constraint is that the "path" value starts with a leading '/', but parsing into
        // the RbPath ignores this. We must use "parse()" here, rather than RbPath.of(), since the
        // captured value contains '/' characters to represent path delimiters.
        RbPath replacePath = RbPath.parse(m.group(1));
        List<RbValue> replaceValues = icuData.get(replacePath);
        checkArgument(replaceValues != null, "Path %s is missing from IcuData", replacePath);
        // If no index is given (e.g. "/foo/bar") then treat it as index 0 (i.e. "/foo/bar[0]").
        int replaceIndex = m.groupCount() > 1 ? Integer.parseInt(m.group(2)) : 0;
        return replaceValues.get(replaceIndex);
    }
}
