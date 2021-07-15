// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import static com.google.common.base.Preconditions.checkArgument;

import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

public final class RbValueSubject extends Subject {
    // For use when chaining from other subjects.
    public static Factory<RbValueSubject, RbValue> rbValues() {
        return RbValueSubject::new;
    }

    private final RbValue actual;

    protected RbValueSubject(FailureMetadata metadata, RbValue actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    /** Asserts the value of the path, as segments (use this if a segment can contain '/'). */
    public final void hasValue(String value) {
        check("getElements()").that(actual.getElements()).containsExactly(value);
    }

    /** Asserts the value of the path, as segments (use this if a segment can contain '/'). */
    public final void hasValues(String... values) {
        check("getElements()").that(actual.getElements()).containsExactlyElementsIn(values).inOrder();
    }

    public final void hasSize(int n) {
        checkArgument(n > 0, "invalid element count: %s", n);
        check("getElements().size()").that(actual.getElements().size()).isEqualTo(n);
    }
}
