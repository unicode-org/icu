// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

// Helper class that converts a single String to a List<String>
// so that the `src` property can be either a single string or an array of strings.
// Used in the TestUtils class.

// Uses ArrayList instead of List so that when registering, it's possible
// to get ArrayList<String>.class
public class StringToListAdapter extends TypeAdapter<Sources> {
    public Sources read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            ArrayList<String> result = new ArrayList<String>();
            reader.beginArray();
            while (reader.hasNext()) {
                result.add(reader.nextString());
            }
            reader.endArray();
            return new Sources(result);
        }
        if (reader.peek() == JsonToken.STRING) {
            String str = reader.nextString();
            ArrayList<String> result = new ArrayList<String>();
            result.add(str);
            return new Sources(result);
        }
        throw new IOException();
    }

    public void write(JsonWriter writer, Sources value) throws IOException {
        writer.beginArray();
        for (String s : value.sources) {
            writer.value(s);
        }
        writer.endArray();
    }
}

