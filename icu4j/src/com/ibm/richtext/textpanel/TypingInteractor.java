/*
 * (C) Copyright IBM Corp. 1998-2007.  All Rights Reserved.
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
/*
    2/25/99 - Now processing characters from keyTyped method (not keyPressed).
              This new way is input-method friendly on 1.2, and is generally
              more correct.

    7/7/97 - the mouseDidSomething methods used to remove the typing interactor.
            This is definitely wrong, but maybe that made sense at one time.  Anyway,
            now the mousePressed / mouseReleased methods remove the interactor;  the
            others do nothing.
*/

package com.ibm.richtext.textpanel;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.FocusEvent;

import java.text.BreakIterator;

import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.textformat.TextOffset;

final class TypingInteractor extends Behavior {

//    static final String COPYRIGHT =
//                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private static final char BACKSPACE = 8;
    private static final char TAB = '\t';
    private static final char RETURN = '\r';
    private static final char LINE_FEED = '\n';
//    private static final char PARAGRAPH_SEP = '\u2029';
    
    private TextComponent fTextComponent;
    private TextSelection fSelection;
    private AttributeMap fTypingStyle;
    private MConstText fText;
    private TextEditBehavior fParent;
    private TextChangeCommand fCommand = null;
    private SimpleCommandLog fCommandLog;
    private PanelEventBroadcaster fListener;
    private BreakIterator fCharBreak = null;
    
    /**
     * Not all characters that come from the keyboard are handled
     * as input.  For example, ctrl-c is not a typable character.
     * This method determines whether a particular character from
     * the keyboard will affect the text.
     */
    private static boolean isTypingInteractorChar(char ch) {

        return ch >= ' ' || 
               ch == LINE_FEED || 
               ch == RETURN ||
               ch == TAB || 
               ch == BACKSPACE;
    }
    
    /**
     * This method determines whether a TypingInteractor should
     * handle the given KeyEvent.
     */
    static boolean handledByTypingInteractor(KeyEvent event) {

        final int id = event.getID();
        
        if (id == KeyEvent.KEY_TYPED) {
            return isTypingInteractorChar(event.getKeyChar());
        }
        else {
            return (id == KeyEvent.KEY_PRESSED && 
                    event.getKeyCode() == KeyEvent.VK_DELETE);
        }
    }

    public TypingInteractor(TextComponent textComponent,
                            TextSelection selection,
                            AttributeMap typingStyle,
                            TextEditBehavior parent,
                            SimpleCommandLog commandLog,
                            PanelEventBroadcaster listener) {
                            
        fTextComponent = textComponent;
        fText = textComponent.getText();
        fSelection = selection;
        fTypingStyle = typingStyle;
        fParent = parent;
        fCommandLog = commandLog;
        fListener = listener;

        fParent.setTypingInteractor(this);
    }

    private void endInteraction() {

        removeFromOwner();
        postTextChangeCommand();

        int selStart = fSelection.getStart().fOffset;
        int selLimit = fSelection.getEnd().fOffset;
        fParent.setSavedTypingStyle(selStart==selLimit? fTypingStyle : null, selStart);
        
        fParent.setTypingInteractor(null);
    }

    public boolean textControlEventOccurred(Behavior.EventType event, Object what) {

        if (fCommand == null && event == Behavior.CHARACTER_STYLE_MOD) {

            pickUpTypingStyle();
            fTypingStyle = ((StyleModifier)what).modifyStyle(fTypingStyle);

            fListener.textStateChanged(TextPanelEvent.SELECTION_STYLES_CHANGED);

            return true;
        }
        else {
            Behavior next = nextBehavior(); // save because removeFromOwner() will trash this

            endInteraction();

            if (next != null)
                return next.textControlEventOccurred(event, what);
            else
                return false;
        }
    }

    private void doBackspace() {

        int selStart = fSelection.getStart().fOffset;
        int selLimit = fSelection.getEnd().fOffset;

        if (selStart == selLimit) {
            if (selStart != 0) {
                fTypingStyle = null;
                pickUpTypingStyle();
                makeTextChangeCommand();
                if (selStart <= fCommand.affectedRangeStart()) {
                    fCommand.prependToOldText(fText.extract(selStart - 1, selStart));
                }
                TextOffset insPt = new TextOffset(selStart - 1);
                fParent.doReplaceText(selStart - 1, selStart, null, insPt, insPt);
            }
        }
        else {
            fTypingStyle = null;
            makeTextChangeCommand();
            TextOffset insPt = new TextOffset(selStart);
            fParent.doReplaceText(selStart, selLimit, null, insPt, insPt);
        }
    }

    private void doFwdDelete(boolean ignoreCharBreak) {

        int selStart = fSelection.getStart().fOffset;
        int selLimit = fSelection.getEnd().fOffset;

        TextOffset insPt = new TextOffset(selStart);

        if (selStart == selLimit) {
            if (selStart != fText.length()) {
                fTypingStyle = null;
                makeTextChangeCommand();
                int numChars;
                if (ignoreCharBreak) {
                    numChars = 1;
                }
                else {
                    if (fCharBreak == null) {
                        fCharBreak = BreakIterator.getCharacterInstance();
                    }
                    fCharBreak.setText(fText.createCharacterIterator());
                    numChars = fCharBreak.following(selStart) - selStart;
                }
                fCommand.appendToOldText(fText.extract(selStart, selStart + numChars));
                fParent.doReplaceText(selStart, selStart + numChars, null, insPt, insPt);
            }
        }
        else {
            fTypingStyle = null;
            makeTextChangeCommand();
            fParent.doReplaceText(selStart, selLimit, null, insPt, insPt);
        }
    }

    private void doNormalKey(char ch) {

        // Sigh - 1.1 reports enter key events as return chars, but
        // 1.2 reports them as linefeeds.
        if (ch == RETURN) {
            ch = LINE_FEED;
        }
        pickUpTypingStyle();
        makeTextChangeCommand();
        fParent.doReplaceSelectedText(ch, fTypingStyle);
    }

    public boolean focusGained(FocusEvent e) {

        // pass through, but stick around...
        return super.focusGained(e);
    }

    public boolean focusLost(FocusEvent e) {

        // pass through, but stick around...
        return super.focusLost(e);
    }

    public boolean keyTyped(KeyEvent e) {

        if (e.getKeyChar() == BACKSPACE) {
            doBackspace();
        }
        else {
            if (isTypingInteractorChar(e.getKeyChar())) {
                KeyRemap remap = fParent.getKeyRemap();
                doNormalKey(remap.remap(e));
            }
        }

        return true;
    }

    public boolean keyPressed(KeyEvent e) {

        int key = e.getKeyCode();
        if (key == KeyEvent.VK_DELETE) {
            doFwdDelete(e.isShiftDown());
            return true;
        }

        Behavior next = nextBehavior();

        if (TextSelection.keyAffectsSelection(e)) {

            endInteraction();
        }

        return next.keyPressed(e);
    }

    public boolean keyReleased(KeyEvent e) {
        return true;
    }

    private void makeTextChangeCommand() {
        if (fCommand == null) {
            TextOffset  selStart = fSelection.getStart();
            TextOffset  selEnd = fSelection.getEnd();

            MText writableText = new StyledText();
            writableText.replace(0, 0, fText, selStart.fOffset, selEnd.fOffset);
            fCommand = new TextChangeCommand(fParent,
                                writableText,
                                null, selStart.fOffset, selStart, selEnd,
                                new TextOffset(), new TextOffset());

            fListener.textStateChanged(TextPanelEvent.UNDO_STATE_CHANGED);
        }
    }

    public boolean mouseDragged(MouseEvent e) {

        return true;
    }

    public boolean mouseEntered(MouseEvent e) {

        return true;
    }

    public boolean mouseExited(MouseEvent e) {

        return true;
    }

    public boolean mouseMoved(MouseEvent e) {

        return true;
    }

    public boolean mousePressed(MouseEvent e) {

        Behavior next = nextBehavior(); // save because removeFromOwner() will trash this

        endInteraction();

        if (next != null)
            return next.mousePressed(e);
        else
            return false;
    }

    public boolean mouseReleased(MouseEvent e) {

        Behavior next = nextBehavior(); // save because removeFromOwner() will trash this

        endInteraction();

        if (next != null)
            return next.mouseReleased(e);
        else
            return false;
    }

    private void pickUpTypingStyle() {
        if (fTypingStyle == null) {
            int selStart = fSelection.getStart().fOffset;
            int selLimit = fSelection.getEnd().fOffset;
            fTypingStyle = TextEditBehavior.typingStyleAt(fText, selStart, selLimit);
        }
    }

    private void postTextChangeCommand() {
        if (fCommand != null) {
            TextOffset  selStart = fSelection.getStart();
            TextOffset  selEnd = fSelection.getEnd();

            fCommand.setNewText(fText.extract(fCommand.affectedRangeStart(), selStart.fOffset));
            fCommand.setSelRangeAfter(selStart, selEnd);
            fCommandLog.add(fCommand);
        }
    }

    boolean hasPendingCommand() {

        return fCommand != null;
    }

    AttributeMap getTypingStyle() {

        pickUpTypingStyle();
        return fTypingStyle;
    }
}
