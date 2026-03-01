// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import java.util.List;

import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Subject;

public final class IcuDataSubject extends Subject {
    private final IcuData actual;

    protected IcuDataSubject(FailureMetadata metadata, IcuData actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    public void hasName(String name) {
        check("getName()").that(actual.getName()).isEqualTo(name);
    }

    public void hasFallback(boolean fallback) {
        check("hasFallback()").that(actual.hasFallback()).isEqualTo(fallback);
    }

    public IterableSubject getPaths() {
        return check("getPaths()").that(actual.getPaths());
    }

    public void hasEmptyValue(String rbPath) {
        hasValuesFor(rbPath, RbValue.of(""));
    }

    public void hasValuesFor(String rbPath, String... values) {
        hasValuesFor(rbPath, RbValue.of(values));
    }

    public void hasValuesFor(String rbPath, RbValue... values) {
        hasValuesFor(RbPath.parse(rbPath), values);
    }

    public void hasValuesFor(RbPath p, String... values) {
        hasValuesFor(p, RbValue.of(values));
    }

    public void hasValuesFor(RbPath p, RbValue... values) {
        List<RbValue> rbValues = actual.get(p);
        check("get('%s')", p).that(rbValues).isNotNull();
        check("get('%s')", p).that(rbValues).containsExactlyElementsIn(values).inOrder();
    }
}
