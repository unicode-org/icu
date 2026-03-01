// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/** Truth subject for asserting about resource bundle paths (makes tests much more readable). */
public final class RbPathSubjectFactory implements Subject.Factory<RbPathSubject, RbPath> {
    public static RbPathSubject assertThat(RbPath result) {
        return Truth.assertAbout(new RbPathSubjectFactory()).that(result);
    }

    @Override
    public RbPathSubject createSubject(FailureMetadata failureMetadata, RbPath that) {
        return new RbPathSubject(failureMetadata, that);
    }

    RbPathSubjectFactory() {}
}