// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

/**
 * A class which allows data from some underlying {@link CldrData} source to be filtered or
 * removed (but not added).
 */
// TODO: Once DTD ordering is the only allowed order, this can be extended to allow adding paths.
abstract class FilteredData implements CldrData {
    private final CldrData src;

    public FilteredData(CldrData src) {
        this.src = checkNotNull(src);
    }

    /** For sub-classes to access the underlying source data. */
    protected CldrData getSourceData() {
        return src;
    }

    /**
     * Returns a filtered CLDR value, replacing or removing the original value during visitation.
     * The filtered value can only differ in it's base value or value attributes, and must have
     * the same {@link CldrPath} associated with it.
     *
     * @return the filtered to be replaced, or {@code null} to remove the value.
     */
    @Nullable
    protected abstract CldrValue filter(CldrValue value);

    @Override
    public void accept(PathOrder order, ValueVisitor visitor) {
        src.accept(order, v -> visitFiltered(v, visitor));
    }

    @Override
    public CldrValue get(CldrPath path) {
        CldrValue value = src.get(path);
        return value != null ? checkFiltered(value) : null;
    }

    private void visitFiltered(CldrValue value, ValueVisitor visitor) {
        CldrValue filteredValue = checkFiltered(value);
        if (filteredValue != null) {
            visitor.visit(filteredValue);
        }
    }

    @Nullable
    private CldrValue checkFiltered(CldrValue value) {
        CldrValue filteredValue = filter(value);
        checkArgument(filteredValue == null || filteredValue.getPath().equals(value.getPath()),
            "filtering is not permitted to modify distinguishing paths: source=%s, filtered=%s",
            value, filteredValue);
        return filteredValue;
    }
}
