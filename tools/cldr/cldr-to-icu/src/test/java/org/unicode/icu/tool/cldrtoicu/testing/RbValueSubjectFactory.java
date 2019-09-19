// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/** Truth subject for asserting about resource bundle paths (makes tests much more readable). */
public final class RbValueSubjectFactory implements Subject.Factory<RbValueSubject, RbValue> {
    public static RbValueSubject assertThat(RbValue result) {
        return Truth.assertAbout(new RbValueSubjectFactory()).that(result);
    }

    @Override
    public RbValueSubject createSubject(FailureMetadata failureMetadata, RbValue that) {
        return new RbValueSubject(failureMetadata, that);
    }

    RbValueSubjectFactory() {}
}