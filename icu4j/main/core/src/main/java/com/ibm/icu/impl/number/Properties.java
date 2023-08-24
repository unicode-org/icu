// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * ICU 59 called the class DecimalFormatProperties as just Properties. We need to keep a thin
 * implementation for the purposes of serialization.
 */
public class Properties implements Serializable {

    /** Same as DecimalFormatProperties. */
    private static final long serialVersionUID = 4095518955889349243L;

    private transient DecimalFormatProperties instance;

    public DecimalFormatProperties getInstance() {
        return instance;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        if (instance == null) {
            instance = new DecimalFormatProperties();
        }
        instance.readObjectImpl(ois);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        if (instance == null) {
            instance = new DecimalFormatProperties();
        }
        instance.writeObjectImpl(oos);
    }
}
