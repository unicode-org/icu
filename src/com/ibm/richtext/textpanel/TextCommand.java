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

import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.textformat.TextOffset;

abstract class TextCommand extends Command {
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    protected TextEditBehavior fBehavior;
    protected MText fOriginalText;
    protected int fAffectedRangeStart;
    protected TextOffset fSelStartBefore;
    protected TextOffset fSelEndBefore;

    public TextCommand(TextEditBehavior behavior,
                       MText originalText,
                       int affectedRangeStart,
                       TextOffset selStartBefore,
                       TextOffset selEndBefore) {

        fBehavior = behavior;
        fOriginalText = originalText;
        fAffectedRangeStart = affectedRangeStart;
        fSelStartBefore = new TextOffset();
        fSelStartBefore.assign(selStartBefore);
        fSelEndBefore = new TextOffset();
        fSelEndBefore.assign(selEndBefore);
    }

    public abstract int affectedRangeEnd();

    public void undo() {
        fBehavior.doReplaceText(fAffectedRangeStart, affectedRangeEnd(), fOriginalText,
                            fSelStartBefore, fSelEndBefore);
    }
}
