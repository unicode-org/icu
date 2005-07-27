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

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.datatransfer.Clipboard;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.textformat.TextOffset;

/**
 * Implementation class for TextPanel and JTextPanel.
 */
final class ATextPanelImpl {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
                
    private RunStrategy fRunStrategy = null;
    private TextComponent fTextComponent = null;
    private TextSelection fSelection = null;
    private TextEditBehavior fEditBehavior = null;
    private MText fText = null;

    private PanelEventBroadcaster fBroadcaster;
    private KeyRemap fRemap = KeyRemap.getIdentityRemap();

    // This is a little ugly.  TextPanel supports its modified
    // flag whether or not it is editable, or even selectable.
    // So if there's no command log to keep track of the flag
    // state then its done right here in TextPanel.  If the
    // panel is editable this flag is ignored.
    private boolean fModified = false;

    static final TextPanelSettings fgDefaultSettings = new TextPanelSettings();

    static TextPanelSettings getDefaultSettings() {

        return (TextPanelSettings) fgDefaultSettings.clone();
    }
    
    ATextPanelImpl(RunStrategy runStrategy,
                   TextPanelSettings settings,
                   MConstText initialText,
                   Clipboard clipboard,
                   MTextPanel client,
                   Adjustable horzSb,
                   Adjustable vertSb) {
                    
        fRunStrategy = runStrategy;
        fBroadcaster = new PanelEventBroadcaster(client);

        Scroller scroller = null;
        if (settings.getScrollable()) {
            scroller = new Scroller(horzSb, vertSb);
        }

        StyledTextClipboard textClipboard =
                            StyledTextClipboard.getClipboardFor(clipboard);

        fText = new StyledText();
        if (initialText != null) {
            fText.append(initialText);
        }

        fTextComponent = new TextComponent(fText,
                                           settings.getDefaultValues(),
                                           settings.getWraps(),
                                           TextComponent.WINDOW_WIDTH,
                                           TextComponent.DEFAULT_INSET,
                                           textClipboard,
                                           settings.getScrollable(),
                                           scroller,
                                           fBroadcaster);

        if (scroller != null) {
            scroller.setClient(fTextComponent);
        }
        
        // May have to wait until component has host to do this:
        if (settings.getSelectable()) {
            fSelection = new TextSelection(fTextComponent, 
                                           fBroadcaster,
                                           fRunStrategy);
            fSelection.addToOwner(fTextComponent);
            if (settings.getEditable()) {
                fEditBehavior = new TextEditBehavior(
                            fTextComponent, fSelection, fBroadcaster, fRemap);
                fEditBehavior.addToOwner(fTextComponent);
            }
        }
    }
    
    FakeComponent getTextComponent() {
        
        return fTextComponent;
    }
    
    /**
     * Add the given TextPanelListener to the listeners which will
     * receive update notifications from this TextPanel.
     * @param listener the listener to add
     */
    public void addListener(TextPanelListener listener) {

        fBroadcaster.addListener(listener);
    }

    /**
     * Remove the given TextPanelListener from the listeners which will
     * receive update notifications from this TextPanel.
     * @param listener the listener to remove
     */
    public void removeListener(TextPanelListener listener) {

        fBroadcaster.removeListener(listener);
    }

    /**
     * You know what this does...
     */
    private static int pin(int value, int min, int max) {

        if (min > max) {
            throw new IllegalArgumentException("Invalid range");
        }

        if (value < min) {
            value = min;
        }
        else if (value > max) {
            value = max;
        }
        return value;
    }

//============
// Text Access
//============

    /**
     * Set the document to <tt>newText</tt>.  This operation
     * modifies the text in the TextPanel.  It does not modify or adopt
     * <tt>newText</tt>.  This method sets the selection an insertion point at
     * the end of the text.
     * @param newText the text which will replace the current text.
     */
    public void setText(MConstText newText) {

        replaceRange(newText, 0, getTextLength());
    }

    /**
     * Append the given text to the end of the document.  Equivalent to
     * <tt>insert(newText, getTextLength())</tt>.
     * @param newText the text to append to the document
     */
    public void append(MConstText newText) {

        int length = getTextLength();
        replaceRange(newText, length, length);
    }

    /**
     * Insert the given text into the document at the given position.
     * Equivalent to
     * <tt>replaceRange(newText, position, position)</tt>.
     * @param newText the text to insert into the document.
     * @param position the position in the document where the
     *     text will be inserted
     */
    public void insert(MConstText newText, int position) {

        replaceRange(newText, position, position);
    }

    /**
     * Replace the given range with <tt>newText</tt>.  After this
     * operation the selection range is an insertion point at the
     * end of the new text.
     * @param newText the text with which to replace the range
     * @param start the beginning of the range to replace
     * @param end the end of the range to replace
     */
    public void replaceRange(MConstText newText, int start, int end) {

        int length = getTextLength();

        start = pin(start, 0, length);
        end = pin(end, start, length);

        if (fSelection != null) {

            // If we're selectable, but not editable, we'll temporarily
            // make ourselves editable to change the text.  A little funny
            // but there's a lot of code for getting caret stuff right,
            // and this is not a common operation anyway.

            TextEditBehavior behavior;

            if (fEditBehavior == null) {
                behavior = new TextEditBehavior(fTextComponent, fSelection, fBroadcaster, fRemap);
                behavior.addToOwner(fTextComponent);
            }
            else {
                behavior = fEditBehavior;
            }

            TextOffset newSelection = new TextOffset(start + newText.length(),
                                                     TextOffset.AFTER_OFFSET);

            TextReplacement replacement = new TextReplacement(start, end,
                                                              newText,
                                                              newSelection,
                                                              newSelection);

            fTextComponent.textControlEventOccurred(Behavior.REPLACE,
                                                    replacement);
            if (fEditBehavior == null) {
                behavior.removeFromOwner();
            }
        }
        else {

            MText oldText = fTextComponent.getModifiableText();
            fTextComponent.stopBackgroundFormatting();
            oldText.replaceAll(newText);
            fTextComponent.reformatAndDrawText(0, newText.length(), null, null, null, null);
        }
    }

    /**
     * Return the length of the text document in the TextPanel.
     * @return the length of the text document in the TextPanel
     */
    public int getTextLength() {

        return fTextComponent.getText().length();
    }

    /**
     * Return the text document in the TextPanel.
     * @return the text document in the TextPanel.
     */
    public MConstText getText() {

        return fTextComponent.getText();
    }

//============
// Selection Access
//============

    /**
     * Return the offset of the start of the selection.
     */
    public int getSelectionStart() {

        if (fSelection != null) {
            return fSelection.getStart().fOffset;
        }
        else {
            return 0;
        }
    }

    /**
     * Return the offset of the end of the selection.
     */
    public int getSelectionEnd() {

        if (fSelection != null) {
            return fSelection.getEnd().fOffset;
        }
        else {
            return 0;
        }
    }

    /**
     * Set the beginning of the selection range.  This is
     * equivalent to <tt>select(selectionStart, getSelectionEnd())</tt>.
     * @param selectionStart the start of the new selection range
     */
    public void setSelectionStart(int selectionStart) {

        select(selectionStart, getSelectionEnd());
    }

    /**
     * Set the end of the selection range.  This is
     * equivalent to <tt>select(getSelectionStart(), selectionEnd)</tt>.
     * @param selectionStart the start of the new selection range
     */
    public void setSelectionEnd(int selectionEnd) {

        select(getSelectionStart(), selectionEnd);
    }

    /**
     * Set the selection range to an insertion point at the given
     * offset.  This is equivalent to
     * <tt>select(position, position)</tt>.
     * @param position the offset of the new insertion point
     */
    public void setCaretPosition(int position) {

        select(position, position);
    }

    /**
     * Set the selection range to the given range.  The range start
     * is pinned between 0 and the text length;  the range end is pinned
     * between the range start and the end of the text.  These semantics
     * are identical to those of <tt>java.awt.TextComponent</tt>.
     * This method has no effect if the text is not selectable.
     * @param selectionStart the beginning of the selection range
     * @param selectionEnd the end of the selection range
     */
    public void select(int selectionStart, int selectionEnd) {

        int length = getTextLength();

        selectionStart = pin(selectionStart, 0, length);
        selectionEnd = pin(selectionEnd, selectionStart, length);

        TextRange range = new TextRange(selectionStart, selectionEnd);
        fTextComponent.textControlEventOccurred(Behavior.SELECT, range);
    }

    /**
     * Select all of the text in the document.  This method has no effect if
     * the text is not selectable.
     */
    public void selectAll() {

        select(0, getTextLength());
    }


//============
// Format Width
//============

    /**
     * Return the total format width, in pixels.  The format width is the
     * width to which text is wrapped.
     * @return the format width
     */
    public int getFormatWidth() {

        return fTextComponent.getFormatWidth();
    }

    /**
     * Return true if the paragraph at the given offset is left-to-right.
     * @param offset an offset in the text
     * @return true if the paragraph at the given offset is left-to-right
     */
    public boolean paragraphIsLeftToRight(int offset) {
        
        return fTextComponent.paragraphIsLeftToRight(offset);
    }

    /**
     * Return true if there is a change which can be undone.
     * @return true if there is a change which can be undone.
     */
    public boolean canUndo() {

        if (fEditBehavior != null) {
            return fEditBehavior.canUndo();
        }
        else {
            return false;
        }
    }

    /**
     * Return true if there is a change which can be redone.
     * @return true if there is a change which can be redone.
     */
    public boolean canRedo() {

        if (fEditBehavior != null) {
            return fEditBehavior.canRedo();
        }
        else {
            return false;
        }
    }

    /**
     * Return true if the clipboard contains contents which could be
     * transfered into the text.
     * @return true if the clipboard has text content.
     */
    public boolean clipboardNotEmpty() {

        return fTextComponent.getClipboard().hasContents();
    }

    /**
     * Return an AttributeMap of keys with default values.  The default
     * values are used when displaying text for values which are not
     * specified in the text.
     * @return an AttributeMap of default key-value pairs
     */
    public AttributeMap getDefaultValues() {

        return fTextComponent.getDefaultValues();
    }
    
    private static boolean objectsAreEqual(Object lhs, Object rhs) {
    
        if (lhs == null) {
            return rhs == null;
        }
        else {
            return lhs.equals(rhs);
        }
    }

    private static Object consistentCharStyle(MConstText text,
                                              int start,
                                              int limit,
                                              Object key,
                                              Object defaultValue) {

        if (start >= limit) {
            throw new IllegalArgumentException("Invalid range.");
        }

        int runStart = start;
        Object initialValue = text.characterStyleAt(runStart).get(key);

        if (initialValue == null) {
            initialValue = defaultValue;
        }

        for (runStart = text.characterStyleLimit(runStart);
             runStart < limit;
             runStart = text.characterStyleLimit(runStart)) {

            Object nextValue = text.characterStyleAt(runStart).get(key);

            if (nextValue == null) {
                nextValue = defaultValue;
            }

            if (!objectsAreEqual(initialValue, nextValue)) {
                return MTextPanel.MULTIPLE_VALUES;
            }
        }

        return initialValue;
    }

    /**
     * This method inspects the character style runs in the selection
     * range (or the typing style at the insertion point) and returns:
     * <ul>
     * <li>The value of <tt>key</tt>, if the value of <tt>key</tt>
     * is the same in all of the style runs in the selection, or</li>
     * <li>null, if two or more style runs have different values for <tt>key</tt>.</li>
     * </ul>
     * If a style run does not contain <tt>key</tt>,
     * its value is considered to be <tt>defaultStyle</tt>.
     * This method is useful for configuring style menus.
     * @param key the key used to retrieve values for comparison
     * @param defaultValue the implicit value of <tt>key</tt> in
     *     style runs where <tt>key</tt> is not defined
     */
    public Object getCharacterStyleOverSelection(Object key) {

        TextRange selRange;
        if (fSelection != null)
            selRange = fSelection.getSelectionRange();
        else
            selRange = new TextRange(0, 0);

        if (selRange.start == selRange.limit) {

            AttributeMap compStyle;

            if (fEditBehavior != null) {
                compStyle = fEditBehavior.getInsertionPointStyle();
            }
            else {
                compStyle = TextEditBehavior.typingStyleAt(fText, selRange.start, selRange.limit);
            }

            Object value = compStyle.get(key);
            return value==null? getDefaultValues().get(key) : value;
        }
        else {
            return consistentCharStyle(fText, 
                                       selRange.start,
                                       selRange.limit,
                                       key,
                                       getDefaultValues().get(key));
        }
    }

    /**
     * This method inspects the paragraph style runs in the selection
     * range (or the typing style at the insertion point) and returns:
     * <ul>
     * <li>The value of <tt>key</tt>, if the value of <tt>key</tt>
     * is the same in all of the style runs in the selection, or</li>
     * <li>null, if two or more style runs have different values for <tt>key</tt>.</li>
     * </ul>
     * If a style run does not contain <tt>key</tt>,
     * its value is considered to be <tt>defaultStyle</tt>.
     * This method is useful for configuring style menus.
     * @param key the key used to retrieve values for comparison
     * @param defaultValue the implicit value of <tt>key</tt> in
     *     style runs where <tt>key</tt> is not defined
     */
    public Object getParagraphStyleOverSelection(Object key) {

        TextRange selRange;
        if (fSelection != null) {
            selRange = fSelection.getSelectionRange();
        }
        else {
            selRange = new TextRange(0, 0);
        }

        if (selRange.start == selRange.limit) {
            AttributeMap pStyle = fText.paragraphStyleAt(selRange.start);
            Object value = pStyle.get(key);
            return value==null? getDefaultValues().get(key) : value;
        }
        else {
            int paragraphStart = selRange.start;
            Object defaultValue = getDefaultValues().get(key);
            Object initialValue = fText.paragraphStyleAt(paragraphStart).get(key);
            if (initialValue == null) {
                initialValue = defaultValue;
            }

            for (paragraphStart = fText.paragraphLimit(paragraphStart);
                 paragraphStart < selRange.limit;
                 paragraphStart = fText.paragraphLimit(paragraphStart)) {

                Object nextValue = fText.paragraphStyleAt(paragraphStart).get(key);
                if (nextValue == null) {
                    nextValue = defaultValue;
                }

                if (!objectsAreEqual(initialValue, nextValue)) {
                    return MTextPanel.MULTIPLE_VALUES;
                }
            }

            return initialValue;
        }
    }

    /**
     * Remove the selected text from the document and place it
     * on the clipboard.  This method has no effect if the text
     * is not editable, or if no text is selected.
     */
    public void cut() {
        fTextComponent.textControlEventOccurred(Behavior.CUT, null);
    }

    /**
     * Place the selected text on the clipboard.  This method has
     * no effect if no text is selected.
     */
    public void copy() {
        fTextComponent.textControlEventOccurred(Behavior.COPY, null);
    }

    /**
     * Replace the currently selected text with the text on the clipboard.
     * This method has no effect if the text is not editable, or if no
     * text is on the clipboard.
     */
    public void paste() {
        fTextComponent.textControlEventOccurred(Behavior.PASTE, null);
    }

    /**
     * Remove selected text from the document, without altering the clipboard.
     * This method has no effect if the
     * text is not editable.
     */
    public void clear() {
        fTextComponent.textControlEventOccurred(Behavior.CLEAR, null);
    }

    /**
     * Undo the most recent text change.  This method has no effect if
     * there is no change to undo.
     */
    public void undo() {
        fTextComponent.textControlEventOccurred(Behavior.UNDO, null);
    }

    /**
     * Redo the most recent text change.  This method has no effect if
     * there is no change to redo.
     */
    public void redo() {
        fTextComponent.textControlEventOccurred(Behavior.REDO, null);
    }

    /**
     * Return the number of commands the command log can hold.
     * @return the number of commands the command log can hold
     */
    public int getCommandLogSize() {

        if (fEditBehavior != null) {
            return fEditBehavior.getCommandLogSize();
        }
        else {
            return 0;
        }
    }

    /**
     * Set the number of commands the command log can hold.  All
     * redoable commands are removed when this method is called.
     * @param size the number of commands kept in the command log
     */
    public void setCommandLogSize(int size) {
        fTextComponent.textControlEventOccurred(Behavior.SET_COMMAND_LOG_SIZE,
                                                new Integer(size));
    }

    /**
     * Remove all commands from the command log.
     */
    public void clearCommandLog() {
        fTextComponent.textControlEventOccurred(Behavior.CLEAR_COMMAND_LOG, null);
    }

    /**
     * Modify the character styles on the selected characters.  If no characters
     * are selected, modify the typing style.
     * @param modifier the StyleModifier with which to modify the styles
     */
    public void modifyCharacterStyleOnSelection(StyleModifier modifier) {
        fTextComponent.textControlEventOccurred(Behavior.CHARACTER_STYLE_MOD, modifier);
    }

    /**
     * Modify the paragraph styles in paragraphs containing selected characters, or
     * the paragraph containing the insertion point.
     * @param modifier the StyleModifier with which to modify the styles
     */
    public void modifyParagraphStyleOnSelection(StyleModifier modifier) {
        fTextComponent.textControlEventOccurred(Behavior.PARAGRAPH_STYLE_MOD, modifier);
    }

    /**
     * Return the KeyRemap used to process key events.
     * @return the key remap used to process key events
     * @see #setKeyRemap
     */
    public KeyRemap getKeyRemap() {

        return fRemap;
    }

    /**
     * Use the given KeyRemap to map key events to characters.
     * Only key
     * events are affected by the remap;  other text entering the
     * control (via the clipboard, for example) is not affected
     * by the KeyRemap.
     * <p>
     * Do not pass <tt>null</tt> to this method to leave key
     * events unmapped.  Instead, use <tt>KeyRemap.getIdentityRemap()</tt>
     * @param remap the KeyRemap to use for mapping key events to characters
     * @exception java.lang.NullPointerException if parameter is null
     * @see KeyRemap
     */
    public void setKeyRemap(KeyRemap remap) {

        if (remap == null) {
            throw new NullPointerException("remap can't be null");
        }

        fRemap = remap;
        if (fEditBehavior != null) {
            fEditBehavior.setKeyRemap(remap);
        }

        fBroadcaster.textStateChanged(TextPanelEvent.KEYREMAP_CHANGED);
    }

    /**
     * Return the modification flag of the current text change.
     * @see #setModified
     */
    public boolean isModified() {

        if (fEditBehavior != null) {
            return fEditBehavior.isModified();
        }
        else {
            return fModified;
        }
    }

    /**
     * Set the modification flag of the current text change.
     */
    public void setModified(boolean modified) {

        boolean handled = fTextComponent.textControlEventOccurred(
                                    Behavior.SET_MODIFIED,
                                    modified? Boolean.TRUE : Boolean.FALSE);
        if (!handled) {
            fModified = modified;
        }
    }

    /**
     * This method is for perf-testing only!
     */
    void handleKeyEvent(java.awt.event.KeyEvent keyEvent) {
    
        Component host = fTextComponent.getHost();
        if (host != null) {
            host.dispatchEvent(keyEvent);
        }
    }
}
