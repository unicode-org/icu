// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

// See https://github.com/unicode-org/conformance/blob/main/schema/message_fmt2/testgen_schema.json

// Class corresponding to the json test files.
// Since this is serialized by Gson, the field names should match the keys in the .json files.
class Param {
    // Unused fields ignored
    final String name;
    final Object value;

    Param(String name, Object value) {
        this.name = name;
        this.value = value;
    }
}
