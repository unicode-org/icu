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

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.textformat.TextOffset;

/**
 * This class is used to pass a REPLACE command to Behaviors.
 */
final class TextReplacement {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private int fStart;
    private int fLimit;
    private MConstText fText;
    private TextOffset fSelStart;
    private TextOffset fSelLimit;

    TextReplacement(int start,
                    int limit,
                    MConstText text,
                    TextOffset selStart,
                    TextOffset selLimit) {

        fStart = start;
        fLimit = limit;
        fText = text;
        fSelStart = selStart;
        fSelLimit = selLimit;
    }

    int getStart() {

        return fStart;
    }

    int getLimit() {

        return fLimit;
    }

    MConstText getText() {

        return fText;
    }

    TextOffset getSelectionStart() {

        return fSelStart;
    }

    TextOffset getSelectionLimit() {

        return fSelLimit;
    }
}
