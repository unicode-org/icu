// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import org.unicode.icu.tool.cldrtoicu.RbPath;

public final class RbPathSubject extends Subject {
    // For use when chaining from other subjects.
    public static Subject.Factory<RbPathSubject, RbPath> rbPaths() {
        return RbPathSubject::new;
    }

    private final RbPath actual;

    protected RbPathSubject(FailureMetadata metadata, RbPath actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    /** Asserts the value of the path, as segments (use this if a segment can contain '/'). */
    public final void hasSegments(String... segments) {
        check("<segments>").that(actual).isEqualTo(RbPath.of(segments));
    }

    public final void hasLength(int n) {
        checkArgument(n >= 0, "invalid path length: %s", n);
        check("length()").that(actual.length()).isEqualTo(n);
    }
}
