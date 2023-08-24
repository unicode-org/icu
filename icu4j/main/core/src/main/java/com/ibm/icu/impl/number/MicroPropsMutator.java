// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

/**
 * @author sffc
 *
 */
public interface MicroPropsMutator<T> {

    public void mutateMicros(MicroProps micros, T value);

}
