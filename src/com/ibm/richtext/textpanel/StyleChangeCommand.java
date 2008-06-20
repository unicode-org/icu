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

import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.textformat.TextOffset;

class StyleChangeCommand extends TextCommand {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private boolean fCharacter;
    private StyleModifier fModifier;

    public StyleChangeCommand(TextEditBehavior behavior,
                              MText originalText,
                              TextOffset selStartBefore,
                              TextOffset selEndBefore,
                              StyleModifier modifier,
                              boolean character) {

        super(behavior, originalText, selStartBefore.fOffset, selStartBefore, selEndBefore);
        fModifier = modifier;
        fCharacter = character;
    }

    public int affectedRangeEnd() {
        return fSelEndBefore.fOffset;
    }

    public void execute() {
        fBehavior.doModifyStyles(fAffectedRangeStart, fSelEndBefore.fOffset,
                                 fModifier, fCharacter, fSelStartBefore, fSelEndBefore);
    }
}
