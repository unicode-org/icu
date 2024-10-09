// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

// See https://github.com/unicode-org/conformance/blob/main/schema/message_fmt2/testgen_schema.json

// Class corresponding to the json test files.
// Since this is serialized by Gson, the field names should match the keys in the .json files.
class DefaultTestProperties {
    private static final Object[] NO_ERRORS = {};
    // Unused fields ignored
    private final String locale;
    private final JsonElement expErrors;

    DefaultTestProperties(String locale, JsonElement expErrors) {
        this.locale = locale;
        this.expErrors = expErrors;
    }

    String getLocale() {
        return this.locale;
    }

    Object[] getExpErrors() {
        if (expErrors == null || !expErrors.isJsonArray()) {
            return NO_ERRORS;
        }
        JsonArray arr = expErrors.getAsJsonArray();
        Object [] result = new Object[arr.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = arr.get(i);
        }
        return result;
    }
}
