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

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.styledtext.MConstText;

/**
 * MTextPanel is implemented by Components which provide selectable
 * editable styled text.
 * <p>
 * Implementations of MTextPanel provide a simple, standard user interface
 * for text editing.  MTextPanel supplies scrollable display, typing, 
 * arrow-key support, character selection, word-
 * and sentence-selection (by double-clicking and triple-clicking, 
 * respectively), text styles, clipboard operations (cut, copy and paste)
 * and a log of changes for undo-redo.
 * <p>
 * MTextPanel implementations do not provide user interface elements
 * such as an edit menu or style menu.  This support is provided in
 * different packages, and is implemented with MTextPanel's API.
 * MTextPanel includes methods for setting selections and styles on text,
 * and using the clipboard and command-log functionality.
 * MTextPanel's API for selection and text handling is similar to that
 * of <tt>java.awt.TextArea</tt> and
 * <tt>java.awt.TextComponent</tt>.
 * <p>
 * MTextPanel supports bidirectional and complex text.  In bidirectional
 * text, offsets at direction boundaries have dual carets.  Logical selection
 * is used, so selections across run directions may not be contiguous in
 * display.
 */
public interface MTextPanel {
    
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    /**
     * This value is returned from <tt>getCharacterStyleOverSelection</tt>
     * and <tt>getParagraphStyleOverSelection</tt> to indicate that the
     * selection range contains multiple values for a key.
     * <p>
     * There is no reason for this Object ever to appear in an AttributeMap
     * as a value.  Obviously, if it does there will be no way to distinguish
     * between multiple values across the selection and a consistent value of
     * <tt>MULTIPLE_VALUES</tt> for the key.
     * @see #getCharacterStyleOverSelection
     * @see #getParagraphStyleOverSelection
     */
    public static final Object MULTIPLE_VALUES = new Object();
    
    /**
     * Add the given TextPanelListener to the listeners which will
     * receive update notifications from this MTextPanel.
     * @param listener the listener to add
     */
    public void addListener(TextPanelListener listener);

    /**
     * Remove the given TextPanelListener from the listeners which will
     * receive update notifications from this MTextPanel.
     * @param listener the listener to remove
     */
    public void removeListener(TextPanelListener listener);

    /**
     * Set the document to <tt>newText</tt>.  This operation
     * modifies the text in the MTextPanel.  It does not modify or adopt
     * <tt>newText</tt>.  This method sets the selection an insertion point at
     * the end of the text.
     * @param newText the text which will replace the current text.
     */
    public void setText(MConstText newText);
    
    /**
     * Append the given text to the end of the document.  Equivalent to
     * <tt>insert(newText, getTextLength())</tt>.
     * @param newText the text to append to the document
     */
    public void append(MConstText newText);
    
    /**
     * Insert the given text into the document at the given position.
     * Equivalent to
     * <tt>replaceRange(newText, position, position)</tt>.
     * @param newText the text to insert into the document.
     * @param position the position in the document where the
     *     text will be inserted
     */
    public void insert(MConstText newText, int position);

    /**
     * Replace the given range with <tt>newText</tt>.  After this
     * operation the selection range is an insertion point at the
     * end of the new text.
     * @param newText the text with which to replace the range
     * @param start the beginning of the range to replace
     * @param end the end of the range to replace
     */
    public void replaceRange(MConstText newText, int start, int end);

    /**
     * Return the length of the text document in the MTextPanel.
     * @return the length of the text document in the MTextPanel
     */
    public int getTextLength();

    /**
     * Return the text document in the MTextPanel.
     * @return the text document in the MTextPanel.
     */
    public MConstText getText();
    
//============
// Selection Access
//============

    /**
     * Return the offset of the start of the selection.
     */
    public int getSelectionStart();

    /**
     * Return the offset of the end of the selection.
     */
    public int getSelectionEnd();
    
    /**
     * Set the beginning of the selection range.  This is
     * equivalent to <tt>select(selectionStart, getSelectionEnd())</tt>.
     * @param selectionStart the start of the new selection range
     */
    public void setSelectionStart(int selectionStart);
    
    /**
     * Set the end of the selection range.  This is
     * equivalent to <tt>select(getSelectionStart(), selectionEnd)</tt>.
     * @param selectionEnd the end of the new selection range
     */
    public void setSelectionEnd(int selectionEnd);
    
    /**
     * Set the selection range to an insertion point at the given
     * offset.  This is equivalent to
     * <tt>select(position, position)</tt>.
     * @param position the offset of the new insertion point
     */
    public void setCaretPosition(int position);
    
    /**
     * Set the selection range to the given range.  The range start
     * is pinned between 0 and the text length;  the range end is pinned
     * between the range start and the end of the text.  These semantics
     * are identical to those of <tt>java.awt.TextComponent</tt>.
     * This method has no effect if the text is not selectable.
     * @param selectionStart the beginning of the selection range
     * @param selectionEnd the end of the selection range
     */
    public void select(int selectionStart, int selectionEnd);
    
    /**
     * Select all of the text in the document.  This method has no effect if
     * the text is not selectable.
     */
    public void selectAll();
    

//============
// Format Width
//============

    /**
     * Return the total format width, in pixels.  The format width is the
     * width to which text is wrapped.
     * @return the format width
     */
    public int getFormatWidth();
    
    /**
     * Return true if the paragraph at the given offset is left-to-right.
     * @param offset an offset in the text
     * @return true if the paragraph at the given offset is left-to-right
     */
    public boolean paragraphIsLeftToRight(int offset);

    /**
     * Return true if there is a change which can be undone.
     * @return true if there is a change which can be undone.
     */
    public boolean canUndo();
    
    /**
     * Return true if there is a change which can be redone.
     * @return true if there is a change which can be redone.
     */
    public boolean canRedo();
    
    /**
     * Return true if the clipboard contains contents which could be
     * transfered into the text.
     * @return true if the clipboard has text content.
     */
    public boolean clipboardNotEmpty();
    

//============
// Styles
//============

    /**
     * Return an AttributeMap of keys with default values.  The default
     * values are used when displaying text for values which are not
     * specified in the text.
     * @return an AttributeMap of default key-value pairs
     */
    public AttributeMap getDefaultValues();
    
    /**
     * This method inspects the character style runs in the selection
     * range (or the typing style at the insertion point).  It returns:
     * <ul>
     * <li>The value of <tt>key</tt>, if the value of <tt>key</tt>
     * is the same in all of the style runs in the selection, or</li>
     * <li><tt>MULTIPLE_VALUES</tt>, if two or more style runs have different 
     * values for <tt>key</tt>.</li>
     * </ul>
     * If a style run does not contain <tt>key</tt>,
     * its value is considered to be the default style for <tt>key</tt>,
     * as defined by the default values AttributeMap.  Note that if
     * <tt>key</tt> does not have a default value this method may return
     * null.
     * This method is useful for configuring style menus.
     * @param key the key used to retrieve values for comparison
     * @see #MULTIPLE_VALUES
     */
    public Object getCharacterStyleOverSelection(Object key);
    
    /**
     * This method inspects the paragraph style runs in the selection
     * range (or the typing style at the insertion point).  It returns:
     * <ul>
     * <li>The value of <tt>key</tt>, if the value of <tt>key</tt>
     * is the same in all of the style runs in the selection, or</li>
     * <li><tt>MULTIPLE_VALUES</tt>, if two or more style runs have 
     * different values for <tt>key</tt>.</li>
     * </ul>
     * If a style run does not contain <tt>key</tt>,
     * its value is considered to be the default style for <tt>key</tt>,
     * as defined by the default values AttributeMap.  Note that if
     * <tt>key</tt> does not have a default value this method may return
     * null.
     * This method is useful for configuring style menus.
     * @param key the key used to retrieve values for comparison
     * @see #MULTIPLE_VALUES
     */
    public Object getParagraphStyleOverSelection(Object key);
    
    /**
     * Remove the selected text from the document and place it
     * on the clipboard.  This method has no effect if the text
     * is not editable, or if no text is selected.
     */
    public void cut();
    
    /**
     * Place the selected text on the clipboard.  This method has
     * no effect if no text is selected.
     */
    public void copy();
    
    /**
     * Replace the currently selected text with the text on the clipboard.
     * This method has no effect if the text is not editable, or if no
     * text is on the clipboard.
     */
    public void paste();
    
    /**
     * Remove selected text from the document, without altering the clipboard.
     * This method has no effect if the
     * text is not editable.
     */
    public void clear();
    
    /**
     * Undo the most recent text change.  This method has no effect if
     * there is no change to undo.
     */
    public void undo();
    
    /**
     * Redo the most recent text change.  This method has no effect if
     * there is no change to redo.
     */
    public void redo();
    
    /**
     * Return the number of commands the command log can hold.
     * @return the number of commands the command log can hold
     */
    public int getCommandLogSize();
    
    /**
     * Set the number of commands the command log can hold.  All
     * redoable commands are removed when this method is called.
     * @param size the number of commands kept in the command log
     */
    public void setCommandLogSize(int size);
    
    /**
     * Remove all commands from the command log.
     */
    public void clearCommandLog();
    
    /**
     * Modify the character styles on the selected characters.  If no characters
     * are selected, modify the typing style.
     * @param modifier the StyleModifier with which to modify the styles
     */
    public void modifyCharacterStyleOnSelection(StyleModifier modifier);
    
    /**
     * Modify the paragraph styles in paragraphs containing selected characters, or
     * the paragraph containing the insertion point.
     * @param modifier the StyleModifier with which to modify the styles
     */
    public void modifyParagraphStyleOnSelection(StyleModifier modifier);
    
    /**
     * Return the KeyRemap used to process key events.
     * @return the key remap used to process key events
     * @see #setKeyRemap
     */
    public KeyRemap getKeyRemap();
    
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
    public void setKeyRemap(KeyRemap remap);
    
    /**
     * Return the modification flag of the current text change.
     * @see #setModified
     */
    public boolean isModified();
    
    /**
     * Set the modification flag of the current text change.
     */
    public void setModified(boolean modified);
}
