// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/* Helper class that converts an array of objects with key named "type"
 * and value `String` to a List<String>.
 * so that the ExpErrors property can be either a boolean or an array
 * of strings objects.
 *
 * Example (json):
 * ```
 * "expErrors": false,
 * "expErrors": true,
 * "expErrors": [],
 * "expErrors": [{ "type": "syntax-error" }, { "type": "unknown-function" }]
 *
 * Used in the TestUtils class.
 */

// Uses ArrayList instead of List so that when registering, it's possible
// to get ArrayList<String>.class
public class ExpectedErrorAdapter extends TypeAdapter<ExpErrors> {
    public ExpErrors read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            ArrayList<String> result = new ArrayList<String>();
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                String name = reader.nextName();
                assert(name.equals("type"));
                String value = reader.nextString();
                result.add(value);
                reader.endObject();
            }
            reader.endArray();
            return new ExpErrors(result);
        }
        if (reader.peek() == JsonToken.BOOLEAN) {
            return new ExpErrors(reader.nextBoolean());
        }
        throw new IOException();
    }

    public void write(JsonWriter writer, ExpErrors value) throws IOException {
        writer.beginArray();
        for (String s : value.errors) {
            writer.beginObject();
            writer.name("type");
            writer.value(s);
            writer.endObject();
        }
        writer.endArray();
    }
}

