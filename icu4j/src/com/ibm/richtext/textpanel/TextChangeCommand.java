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
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.textformat.TextOffset;

class TextChangeCommand extends TextCommand {
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private MConstText fNewText;
    private TextOffset fSelStartAfter;
    private TextOffset fSelEndAfter;

    public TextChangeCommand(TextEditBehavior behavior,
                             MText originalText,
                             MConstText newText,
                             int affectedRangeStart,
                             TextOffset selStartBefore,
                             TextOffset selEndBefore,
                             TextOffset selStartAfter,
                             TextOffset selEndAfter) {
        super(behavior, originalText, affectedRangeStart, selStartBefore, selEndBefore);
        fNewText = newText;
        fSelStartAfter = new TextOffset();
        fSelStartAfter.assign(selStartAfter);
        fSelEndAfter = new TextOffset();
        fSelEndAfter.assign(selEndAfter);
    }

    public int affectedRangeEnd() {
        if (fNewText == null)
            return fAffectedRangeStart;
        else
            return fAffectedRangeStart + fNewText.length();
    }

    public void execute() {
        fBehavior.doReplaceText(fAffectedRangeStart, fAffectedRangeStart + fOriginalText.length(),
                            fNewText, fSelStartAfter, fSelEndAfter);
    }

    public int affectedRangeStart() {
        return fAffectedRangeStart;
    }

    public void setNewText(MConstText newText) {
        fNewText = newText;
    }

    public void setSelRangeAfter(TextOffset start, TextOffset end) {
        if (fSelStartAfter == null)
            fSelStartAfter = new TextOffset();
        if (fSelEndAfter == null)
            fSelEndAfter = new TextOffset();
        fSelStartAfter.assign(start);
        fSelEndAfter.assign(end);
    }

    public void prependToOldText(MConstText newText) {
        fOriginalText.insert(0, newText);
        fAffectedRangeStart -= newText.length();
    }

    public void appendToOldText(MConstText newText) {
        fOriginalText.append(newText);
    }
}
