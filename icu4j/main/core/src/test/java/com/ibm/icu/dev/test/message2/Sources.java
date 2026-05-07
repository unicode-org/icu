// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.List;

// Class corresponding to the json test files.
// See Unit.java and StringToListAdapter.java for how this is used.
// Workaround for not being able to get the class of a generic type.

class Sources {
    final List<String> sources;

    Sources(List<String> sources) {
        this.sources = sources;
    }

    @Override
    public String toString() {
        return ("[" + sources.toString() + "]");
    }
}
