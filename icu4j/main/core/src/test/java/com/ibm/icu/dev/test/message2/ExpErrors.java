// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.ArrayList;
import java.util.List;

// Class corresponding to the json test files.
// See Unit.java and StringToListAdapter.java for how this is used.
// Workaround for not being able to get the class of a generic type.

class ExpErrors {
    public final static String ANY_ERROR = "*-any";
    final List<String> errors;
    final boolean hasErrors;

    ExpErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
        this.errors = new ArrayList<>();
        if (hasErrors) {
            // This is problematic if we start testing for the exact error we expect.
            // Unlikely, since in Java we only report the error by throwing,
            // without a good way to specify the exact error type.
            // If we get there we might change this to an enum, so there is some
            // refactoring to be done anyway.
            this.errors.add(ANY_ERROR);
        }
    }

    ExpErrors(List<String> errors) {
        this.errors = errors == null ? new ArrayList<>() : errors;
        this.hasErrors = errors != null && !errors.isEmpty();
    }

    boolean expectErrors() {
        return this.hasErrors;
    }

    @Override
    public String toString() {
        return ("[" + errors.toString() + "]");
    }
}
