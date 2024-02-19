// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.truth.ComparableSubject;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Subject;

public final class ResultSubject extends ComparableSubject<Result> {
    // For use when chaining from other subjects.
    public static Subject.Factory<ResultSubject, Result> results() {
        return ResultSubject::new;
    }

    private final Result actual;

    protected ResultSubject(FailureMetadata metadata, Result result) {
        super(metadata, checkNotNull(result));
        this.actual = result;
    }

    public final void isGrouped(boolean grouped) {
        if (grouped != actual.isGrouped()) {
            check("isGrouped()").that(actual.isGrouped()).isEqualTo(grouped);
        }
    }

    public final IterableSubject hasValueListThat() {
        return check("getValues()").that(actual.getValues());
    }

    public final void hasValues(String... values) {
        hasValueListThat().containsExactlyElementsIn(values);
    }

    public final RbPathSubject hasKeyThat() {
        return check("getKey()").about(RbPathSubject.rbPaths()).that(actual.getKey());
    }

    public final void hasKey(RbPath path) {
        hasKeyThat().isEqualTo(path);
    }

    public final void hasKey(String path) {
        hasKey(RbPath.parse(path));
    }
}
