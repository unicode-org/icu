/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.richtext.textpanel;

/**
* A <TT>TextRange</TT> represents a range of text bounded by a
* start (inclusive), and a limit (exclusive).  [start,limit)
*/
final class TextRange
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /** the start of the range */
    public int start = 0;
    /** the end of the range */
    public int limit = 0;

    /**
    * Create a text range from two ints.
    * @param start the start of the run
    * @param limit the end of the run
    */
    public TextRange(int start, int limit)
    {
        this.start = start;
        this.limit = limit;
    }

    /**
    * Create a text range of 0, 0.
    */
    public TextRange() {
        this.start = this.limit = 0;
    }
}
