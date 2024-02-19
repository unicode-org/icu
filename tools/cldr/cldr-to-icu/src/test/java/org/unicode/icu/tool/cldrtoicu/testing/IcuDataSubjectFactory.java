// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import org.unicode.icu.tool.cldrtoicu.IcuData;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/** Truth subject for asserting about ICU data instances (makes tests much more readable). */
public final class IcuDataSubjectFactory implements Subject.Factory<IcuDataSubject, IcuData> {
    public static IcuDataSubject assertThat(IcuData result) {
        return Truth.assertAbout(new IcuDataSubjectFactory()).that(result);
    }

    @Override
    public IcuDataSubject createSubject(FailureMetadata failureMetadata, IcuData that) {
        return new IcuDataSubject(failureMetadata, that);
    }

    IcuDataSubjectFactory() {}
}