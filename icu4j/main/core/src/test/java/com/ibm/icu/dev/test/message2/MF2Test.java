// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

// See https://github.com/unicode-org/conformance/blob/main/schema/message_fmt2/testgen_schema.json

// Class corresponding to the json test files.
// Since this is serialized by Gson, the field names should match the keys in the .json files.
class MF2Test {
    // Unused fields ignored
    final DefaultTestProperties defaultTestProperties;
    final Unit[] tests;

    MF2Test(DefaultTestProperties defaultTestProperties, Unit[] tests) {
        this.defaultTestProperties = defaultTestProperties;
        this.tests = tests;
    }
}
