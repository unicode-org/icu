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

import java.awt.Rectangle;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.textformat.TextOffset;
import com.ibm.richtext.styledtext.StyleModifier;

// All changes to the text should happen in this class, or in
// its TypingInteractor.

class TextEditBehavior extends Behavior {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private TextComponent fTextComponent;
    private TextSelection fSelection;
    private MText fText;
    private SimpleCommandLog fCommandLog;
    private PanelEventBroadcaster fListener;
    private TypingInteractor fTypingInteractor = null;
    private KeyRemap fRemap;
    
    private AttributeMap fSavedTypingStyle = null;
    private int fSavedInsPt = 0;
    
    public TextEditBehavior(TextComponent textComponent,
                            TextSelection selection,
                            PanelEventBroadcaster listener,
                            KeyRemap remap) {

        fTextComponent = textComponent;
        fSelection = selection;
        fText = textComponent.getModifiableText();
        fCommandLog = new SimpleCommandLog(listener);
        fListener = listener;
        fRemap = remap;
    }

    public KeyRemap getKeyRemap() {

        return fRemap;
    }

    public void setKeyRemap(KeyRemap remap) {

        fRemap = remap;
    }

    public boolean textControlEventOccurred(Behavior.EventType event, Object what) {

        boolean handled = true;
        
        if (event == Behavior.CHARACTER_STYLE_MOD ||
            event == Behavior.PARAGRAPH_STYLE_MOD) {
            doStyleChange(event, what);
        }
        else if (event == Behavior.CUT) {
            doCut();
        }
        else if (event == Behavior.PASTE) {
            doPaste();
        }
        else if (event == Behavior.CLEAR) {
            doClear();
        }
        else if (event == Behavior.REPLACE) {
            doUndoableReplace((TextReplacement) what);
        }
        else if (event == Behavior.UNDO) {
            fCommandLog.undo();
        }
        else if (event == Behavior.REDO) {
            fCommandLog.redo();
        }
        else if (event == Behavior.SET_MODIFIED) {
            fCommandLog.setModified(what == Boolean.TRUE);
        }
        else if (event == Behavior.CLEAR_COMMAND_LOG) {
            fCommandLog.clearLog();
        }
        else if (event == Behavior.SET_COMMAND_LOG_SIZE) {
            fCommandLog.setLogSize(((Integer)what).intValue());
        }
        else {
            handled = super.textControlEventOccurred(event, what);
        }

        checkSavedTypingStyle();
        
        return handled;
    }

    /**
     * It's unfortunate that the text is modified and reformatted in
     * three different methods.  This method is the "common prologue"
     * for all text modifications.
     *
     * This method should be called before modifying and reformatting
     * the text.  It does three things:  stops caret blinking, stops
     * background formatting, and returns the Rectangle containing the
     * current (soon-to-be obsolete) selection.
     */
    private Rectangle prepareForTextEdit() {

        fSelection.stopCaretBlinking();
        fTextComponent.stopBackgroundFormatting();
        return fTextComponent.getBoundingRect(fSelection.getStart(), fSelection.getEnd());
    }

    private void doClear() {
        TextRange selRange = fSelection.getSelectionRange();

        if (selRange.start == selRange.limit)
            return;

        doUndoableTextChange(selRange.start, selRange.limit, null, new TextOffset(selRange.
                            start), new TextOffset(selRange.start));
    }

    private void doCut() {
        TextRange selRange = fSelection.getSelectionRange();

        if (selRange.start == selRange.limit)
            return;

        fTextComponent.getClipboard().setContents(fText.extract(selRange.start, selRange.limit));
        doUndoableTextChange(selRange.start, selRange.limit, null, new TextOffset(selRange.start), new TextOffset(selRange.start));

        fListener.textStateChanged(TextPanelEvent.CLIPBOARD_CHANGED);
    }

    private void doPaste() {
        TextRange selRange = fSelection.getSelectionRange();
        MConstText clipText = fTextComponent.getClipboard().getContents(AttributeMap.EMPTY_ATTRIBUTE_MAP);

        if (clipText != null) {
            doUndoableTextChange(selRange.start, selRange.limit, clipText,
                                new TextOffset(selRange.start + clipText.length()),
                                new TextOffset(selRange.start + clipText.length()));
        }
        else {
            fListener.textStateChanged(TextPanelEvent.CLIPBOARD_CHANGED);
        }
    }

    private void doUndoableReplace(TextReplacement replacement) {

        doUndoableTextChange(replacement.getStart(),
                             replacement.getLimit(),
                             replacement.getText(),
                             replacement.getSelectionStart(),
                             replacement.getSelectionLimit());
    }

    /**
     * Only TypingInteractor and TextCommand should call this!
     */
    void doReplaceText(int start,
                       int limit,
                       MConstText newText,
                       TextOffset newSelStart,
                       TextOffset newSelEnd) {

        int textLength;

        fText.resetDamagedRange();

        Rectangle oldSelRect = prepareForTextEdit();

        if (newText == null) {
            textLength = 0;
            fText.remove(start, limit);
        }
        else {
            textLength = newText.length();
            fText.replace(start, limit, newText, 0, textLength);
        }
        fSelection.setSelectionRange(newSelStart, newSelEnd, newSelStart);
        reformatAndDrawText(fSelection.getStart(),
                            fSelection.getEnd(),
                            oldSelRect);
    }

    /**
     * Only the typing interactor should call this!
     */
    void doReplaceSelectedText(char ch, AttributeMap charStyle) {

        int start = fSelection.getStart().fOffset;
        int limit = fSelection.getEnd().fOffset;
        TextOffset newOffset = new TextOffset(start + 1);
        doReplaceText(start, limit, ch, charStyle, newOffset, newOffset);
    }

    private void doReplaceText(int start,
                               int limit,
                               char ch,
                               AttributeMap charStyle,
                               TextOffset newSelStart,
                               TextOffset newSelEnd) {

        fText.resetDamagedRange();

        Rectangle oldSelRect = prepareForTextEdit();

        fText.replace(start, limit, ch, charStyle);

        fSelection.setSelectionRange(newSelStart, newSelEnd, newSelStart);
        reformatAndDrawText(fSelection.getStart(),
                            fSelection.getEnd(),
                            oldSelRect);
    }

    private void doStyleChange(Behavior.EventType event, Object what) {

        TextRange selRange = fSelection.getSelectionRange();
        boolean character = (event == Behavior.CHARACTER_STYLE_MOD);

        if (selRange.start != selRange.limit || !character) {
            doUndoableStyleChange(what, character);
        }
        else {
            TypingInteractor interactor =
                new TypingInteractor(fTextComponent, 
                                     fSelection,
                                     fSavedTypingStyle,
                                     this,
                                     fCommandLog,
                                     fListener);

            interactor.addToOwner(fTextComponent);
            interactor.textControlEventOccurred(event, what);
        }
    }

    /**
     * Only text commands should call this method!
     */
    void doModifyStyles(int start,
                        int limit,
                        StyleModifier modifier,
                        boolean character,
                        TextOffset newSelStart,
                        TextOffset newSelEnd) {

        fText.resetDamagedRange();

        Rectangle oldSelRect = prepareForTextEdit();

        if (character) {
            fText.modifyCharacterStyles(start, limit, modifier);
        }
        else {
            fText.modifyParagraphStyles(start, limit, modifier);
        }

        fSelection.setSelectionRange(newSelStart, newSelEnd, newSelStart);
        reformatAndDrawText(newSelStart,
                            newSelEnd,
                            oldSelRect);
    }

    private void doUndoableStyleChange(Object what,
                                       boolean character) {

        TextOffset selStart = fSelection.getStart();
        TextOffset selEnd = fSelection.getEnd();

        MText oldText = fText.extractWritable(selStart.fOffset, selEnd.fOffset);
        StyleChangeCommand command = new StyleChangeCommand(
                this, oldText, selStart, selEnd, (StyleModifier) what, character);

        fCommandLog.addAndDo(command);

        fListener.textStateChanged(TextPanelEvent.SELECTION_STYLES_CHANGED);
    }

    private void doUndoableTextChange(int start,
                                      int limit,
                                      MConstText newText,
                                      TextOffset newSelStart,
                                      TextOffset newSelEnd) {

        TextChangeCommand command = new TextChangeCommand(this, fText.extractWritable(start, limit),
                                newText, start, fSelection.getStart(), fSelection.getEnd(),
                                newSelStart, newSelEnd);

        fCommandLog.addAndDo(command);
    }

    public boolean canUndo() {

        boolean canUndo = false;

        if (fTypingInteractor != null) {
            canUndo = fTypingInteractor.hasPendingCommand();
        }

        if (!canUndo) {
            canUndo = fCommandLog.canUndo();
        }

        return canUndo;
    }

    public boolean canRedo() {

        return fCommandLog.canRedo();
    }

    public boolean isModified() {

        if (fTypingInteractor != null) {
            if (fTypingInteractor.hasPendingCommand()) {
                return true;
            }
        }
        return fCommandLog.isModified();
    }

    public int getCommandLogSize() {

        return fCommandLog.getLogSize();
    }

    public AttributeMap getInsertionPointStyle() {

        if (fTypingInteractor != null) {
            return fTypingInteractor.getTypingStyle();
        }

        if (fSavedTypingStyle != null) {
            return fSavedTypingStyle;
        }
        
        TextRange range = fSelection.getSelectionRange();
        return typingStyleAt(fText, range.start, range.limit);
    }
    
    public boolean keyPressed(KeyEvent e) {

        boolean handled = true;
        if (TypingInteractor.handledByTypingInteractor(e)) {
            TypingInteractor interactor = new TypingInteractor(fTextComponent,
                                                               fSelection,
                                                               fSavedTypingStyle,
                                                               this,
                                                               fCommandLog,
                                                               fListener);

            interactor.addToOwner(fTextComponent);
            interactor.keyPressed(e);
        }
        else {
            handled = super.keyPressed(e);
            checkSavedTypingStyle();
        }
        
        return handled;
    }

    public boolean keyTyped(KeyEvent e) {
        
        boolean handled = true;
        if (TypingInteractor.handledByTypingInteractor(e)) {
            TypingInteractor interactor = new TypingInteractor(fTextComponent, 
                                                               fSelection,
                                                               fSavedTypingStyle,
                                                               this,
                                                               fCommandLog,
                                                               fListener);

            interactor.addToOwner(fTextComponent);
            interactor.keyTyped(e);
        }
        else {
            handled = super.keyTyped(e);
            checkSavedTypingStyle();
        }
        
        return handled;
    }

    public boolean mouseReleased(MouseEvent e) {
        
        boolean result = super.mouseReleased(e);
        checkSavedTypingStyle();
        return result;
    }
    
    private void reformatAndDrawText(TextOffset selStart,
                                     TextOffset selLimit,
                                     Rectangle oldSelRect)
    {
        if (!fSelection.enabled()) {
            selStart = selLimit = null;
        }

        int reformatStart = fText.damagedRangeStart();
        int reformatLength = fText.damagedRangeLimit() - reformatStart;

        if (reformatStart != Integer.MAX_VALUE) {
            fTextComponent.reformatAndDrawText(reformatStart,
                                               reformatLength,
                                               selStart,
                                               selLimit,
                                               oldSelRect,
                                               fSelection.getHighlightColor());
        }

        fSelection.scrollToShowSelection();
        
        // sometimes this should send SELECTION_STYLES_CHANGED
        fListener.textStateChanged(TextPanelEvent.TEXT_CHANGED);

        fSelection.restartCaretBlinking(true);
    }

    /**
     * Only TypingInteractor should call this.
     */
    void setTypingInteractor(TypingInteractor interactor) {
        fTypingInteractor = interactor;
    }

    /**
     * Only TypingInteractor should call this.
     */
    void setSavedTypingStyle(AttributeMap style, int insPt) {
    
        fSavedTypingStyle = style;
        fSavedInsPt = insPt;
    }
    
    private void checkSavedTypingStyle() {
    
        if (fSavedTypingStyle != null) {
            int selStart = fSelection.getStart().fOffset;
            int selLimit = fSelection.getEnd().fOffset;
            if (selStart != fSavedInsPt || selStart != selLimit) {
                fSavedTypingStyle = null;
            }
        }            
    }
    
    /**
     * Return the style appropriate for typing on the given selection
     * range.
     */
    public static AttributeMap typingStyleAt(MConstText text, int start, int limit) {

        if (start < limit) {
            return text.characterStyleAt(start);
        }
        else if (start > 0) {
            return text.characterStyleAt(start - 1);
        }
        else {
            return text.characterStyleAt(0);
        }
    }
}
