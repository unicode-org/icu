// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/** Truth subject for asserting about transformation results (makes tests much more readable). */
public class ResultSubjectFactory implements Subject.Factory<ResultSubject, Result> {
    public static ResultSubject assertThat(Result result) {
        return Truth.assertAbout(new ResultSubjectFactory()).that(result);
    }

    @Override
    public ResultSubject createSubject(FailureMetadata failureMetadata, Result that) {
        return new ResultSubject(failureMetadata, that);
    }

    private ResultSubjectFactory() {}
}