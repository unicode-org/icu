// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

// See https://github.com/unicode-org/conformance/blob/main/schema/message_fmt2/testgen_schema.json

// Class corresponding to the json test files.
// Since this is serialized by Gson, the field names should match the keys in the .json files.
class DefaultTestProperties {
    private static final ExpErrors NO_ERRORS = new ExpErrors(false);
    // Unused fields ignored
    private final String locale;
    private final ExpErrors expErrors;

    DefaultTestProperties(String locale, ExpErrors expErrors) {
        this.locale = locale;
        this.expErrors = expErrors;
    }

    String getLocale() {
        return this.locale;
    }

    ExpErrors getExpErrors() {
        return expErrors == null ? NO_ERRORS : expErrors;
    }
}
