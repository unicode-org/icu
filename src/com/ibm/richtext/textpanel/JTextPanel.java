/*
 * (C) Copyright IBM Corp. 1998-2005.  All Rights Reserved.
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import java.awt.datatransfer.Clipboard;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.styledtext.MConstText;

/**
 * JTextPanel is an implementation of MTextPanel in a Swing JPanel.
 * @see MTextPanel
 */
public final class JTextPanel extends JPanel implements MTextPanel {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private ATextPanelImpl fImpl;
    
    /**
     * Return a TextPanelSettings instance with all settings set
     * to the default values.  Clients can modify this object;
     * modifications will not affect the default values.
     * @return a TextPanelSettings instance set to default values
     * @see TextPanelSettings
     */
    public static TextPanelSettings getDefaultSettings() {

        return ATextPanelImpl.getDefaultSettings();
    }

    /**
     * Create a new JTextPanel with the default settings.
     * @param initialText the text document.  If null document text is empty.
     * @param clipboard the clipboard to use for cut, copy, and paste
     *  operations.  If null this panel will use a private clipboard.
     */
    public JTextPanel(MConstText initialText,
                      java.awt.datatransfer.Clipboard clipboard) {

        this(ATextPanelImpl.fgDefaultSettings, initialText, clipboard);
    }

    /**
     * Create a new JTextPanel.
     * @param settings the settings for this JTextPanel
     * @param initialText the text document.  If null document text is empty.
     * @param clipboard the clipboard to use for cut, copy, and paste
     *  operations.  If null this panel will use a private clipboard.
     * @see TextPanelSettings
     */
    public JTextPanel(TextPanelSettings settings,
                      MConstText initialText,
                      Clipboard clipboard) {
        
        super(false);
        
        JScrollBar horzSb = null;
        JScrollBar vertSb = null;
        
        if (settings.getScrollable()) {

            setLayout(new ScrollBarLayout());

            boolean scrollBarsVisible = settings.getScrollBarsVisible();

            if (scrollBarsVisible) {
                horzSb = new JScrollBar(Adjustable.HORIZONTAL);
                vertSb = new JScrollBar(Adjustable.VERTICAL);
                add("South", horzSb);
                add("East", vertSb);
            }
        }
        else {
            setLayout(new BorderLayout());
        }
        
        RunStrategy runStrategy = new RunStrategy() {
            void doIt(Runnable r) {
                try {
                    SwingUtilities.invokeAndWait(r);
                }
                catch(InterruptedException e) {
                    // If operation was interrupted, then client
                    // called wait or sleep (or something similar)
                    // which is inappropriate for a client of this
                    // class.  Rethrow error and let client handle it.
                    e.printStackTrace();
                    throw new Error("Interrupted in RunStrategy: " + e);
                }
                catch(InvocationTargetException e) {
                    // Who knows how this one happens...
                    e.printStackTrace();
                    throw new Error("InvocationTargetException in RunStrategy: " + e);
                }
            }
        };
        
        fImpl = new ATextPanelImpl(runStrategy,
                                   settings,
                                   initialText,
                                   clipboard,
                                   this,
                                   horzSb,
                                   vertSb);
                                   
        final FakeComponent textComponent = fImpl.getTextComponent();
        
        JComponent textHost = new JComponent() {
            {
                textComponent.setHost(this);
            }
            public void addNotify() {
                super.addNotify();
                textComponent.addNotify();
            }
            public void paint(Graphics g) {
                textComponent.paint(g);
            }
        };
        
        add("Center", textHost);

        textHost.requestFocus();
    }

    /**
     * Add the given TextPanelListener to the listeners which will
     * receive update notifications from this JTextPanel.
     * @param listener the listener to add
     */
    public void addListener(TextPanelListener listener) {

        fImpl.addListener(listener);
    }

    /**
     * Remove the given TextPanelListener from the listeners which will
     * receive update notifications from this JTextPanel.
     * @param listener the listener to remove
     */
    public void removeListener(TextPanelListener listener) {

        fImpl.removeListener(listener);
    }

//============
// Text Access
//============

    /**
     * Set the document to <tt>newText</tt>.  This operation
     * modifies the text in the JTextPanel.  It does not modify or adopt
     * <tt>newText</tt>.  This method sets the selection an insertion point at
     * the end of the text.
     * @param newText the text which will replace the current text.
     */
    public void setText(MConstText newText) {

        fImpl.setText(newText);
    }

    /**
     * Append the given text to the end of the document.  Equivalent to
     * <tt>insert(newText, getTextLength())</tt>.
     * @param newText the text to append to the document
     */
    public void append(MConstText newText) {

        fImpl.append(newText);
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

        fImpl.insert(newText, position);
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

        fImpl.replaceRange(newText, start, end);
    }

    /**
     * Return the length of the text document in the JTextPanel.
     * @return the length of the text document in the JTextPanel
     */
    public int getTextLength() {

        return fImpl.getTextLength();
    }

    /**
     * Return the text document in the JTextPanel.
     * @return the text document in the JTextPanel.
     */
    public MConstText getText() {

        return fImpl.getText();
    }

//============
// Selection Access
//============

    /**
     * Return the offset of the start of the selection.
     */
    public int getSelectionStart() {

        return fImpl.getSelectionStart();
    }

    /**
     * Return the offset of the end of the selection.
     */
    public int getSelectionEnd() {

        return fImpl.getSelectionEnd();
    }

    /**
     * Set the beginning of the selection range.  This is
     * equivalent to <tt>select(selectionStart, getSelectionEnd())</tt>.
     * @param selectionStart the start of the new selection range
     */
    public void setSelectionStart(int selectionStart) {

        fImpl.setSelectionStart(selectionStart);
    }

    /**
     * Set the end of the selection range.  This is
     * equivalent to <tt>select(getSelectionStart(), selectionEnd)</tt>.
     * @param selectionEnd the end of the new selection range
     */
    public void setSelectionEnd(int selectionEnd) {

        fImpl.setSelectionEnd(selectionEnd);
    }

    /**
     * Set the selection range to an insertion point at the given
     * offset.  This is equivalent to
     * <tt>select(position, position)</tt>.
     * @param position the offset of the new insertion point
     */
    public void setCaretPosition(int position) {

        fImpl.setCaretPosition(position);
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

        fImpl.select(selectionStart, selectionEnd);
    }

    /**
     * Select all of the text in the document.  This method has no effect if
     * the text is not selectable.
     */
    public void selectAll() {

        fImpl.selectAll();
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

        return fImpl.getFormatWidth();
    }

    /**
     * Return true if the paragraph at the given offset is left-to-right.
     * @param offset an offset in the text
     * @return true if the paragraph at the given offset is left-to-right
     */
    public boolean paragraphIsLeftToRight(int offset) {
        
        return fImpl.paragraphIsLeftToRight(offset);
    }

    /**
     * Return true if there is a change which can be undone.
     * @return true if there is a change which can be undone.
     */
    public boolean canUndo() {

        return fImpl.canUndo();
    }

    /**
     * Return true if there is a change which can be redone.
     * @return true if there is a change which can be redone.
     */
    public boolean canRedo() {

        return fImpl.canRedo();
    }

    /**
     * Return true if the clipboard contains contents which could be
     * transfered into the text.
     * @return true if the clipboard has text content.
     */
    public boolean clipboardNotEmpty() {

        return fImpl.clipboardNotEmpty();
    }

    /**
     * Return an AttributeMap of keys with default values.  The default
     * values are used when displaying text for values which are not
     * specified in the text.
     * @return an AttributeMap of default key-value pairs
     */
    public AttributeMap getDefaultValues() {

        return fImpl.getDefaultValues();
    }

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
     * @see MTextPanel#MULTIPLE_VALUES
     */
    public Object getCharacterStyleOverSelection(Object key) {
        
        return fImpl.getCharacterStyleOverSelection(key);
    }

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
     * @see MTextPanel#MULTIPLE_VALUES
     */
    public Object getParagraphStyleOverSelection(Object key) {
        
        return fImpl.getParagraphStyleOverSelection(key);
    }

    /**
     * Remove the selected text from the document and place it
     * on the clipboard.  This method has no effect if the text
     * is not editable, or if no text is selected.
     */
    public void cut() {
        fImpl.cut();
    }

    /**
     * Place the selected text on the clipboard.  This method has
     * no effect if no text is selected.
     */
    public void copy() {
        fImpl.copy();
    }

    /**
     * Replace the currently selected text with the text on the clipboard.
     * This method has no effect if the text is not editable, or if no
     * text is on the clipboard.
     */
    public void paste() {
        fImpl.paste();
    }

    /**
     * Remove selected text from the document, without altering the clipboard.
     * This method has no effect if the
     * text is not editable.
     */
    public void clear() {
        fImpl.clear();
    }

    /**
     * Undo the most recent text change.  This method has no effect if
     * there is no change to undo.
     */
    public void undo() {
        fImpl.undo();
    }

    /**
     * Redo the most recent text change.  This method has no effect if
     * there is no change to redo.
     */
    public void redo() {
        fImpl.redo();
    }

    /**
     * Return the number of commands the command log can hold.
     * @return the number of commands the command log can hold
     */
    public int getCommandLogSize() {

        return fImpl.getCommandLogSize();
    }

    /**
     * Set the number of commands the command log can hold.  All
     * redoable commands are removed when this method is called.
     * @param size the number of commands kept in the command log
     */
    public void setCommandLogSize(int size) {
        fImpl.setCommandLogSize(size);
    }

    /**
     * Remove all commands from the command log.
     */
    public void clearCommandLog() {
        fImpl.clearCommandLog();
    }

    /**
     * Modify the character styles on the selected characters.  If no characters
     * are selected, modify the typing style.
     * @param modifier the StyleModifier with which to modify the styles
     */
    public void modifyCharacterStyleOnSelection(StyleModifier modifier) {
        fImpl.modifyCharacterStyleOnSelection(modifier);
    }

    /**
     * Modify the paragraph styles in paragraphs containing selected characters, or
     * the paragraph containing the insertion point.
     * @param modifier the StyleModifier with which to modify the styles
     */
    public void modifyParagraphStyleOnSelection(StyleModifier modifier) {
        fImpl.modifyParagraphStyleOnSelection(modifier);
    }

    /**
     * Return the KeyRemap used to process key events.
     * @return the key remap used to process key events
     * @see #setKeyRemap
     */
    public KeyRemap getKeyRemap() {

        return fImpl.getKeyRemap();
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

        fImpl.setKeyRemap(remap);
    }

    /**
     * Return the modification flag of the current text change.
     * @see #setModified
     */
    public boolean isModified() {

        return fImpl.isModified();
    }

    /**
     * Set the modification flag of the current text change.
     */
    public void setModified(boolean modified) {
        
        fImpl.setModified(modified);
    }

    /**
     * This method is for KeyEventForwarder's use only!
     */
    ATextPanelImpl getImpl() {
        
        return fImpl;
    }

    public void setBackground(Color color) {
        super.setBackground (color);
        java.awt.Component[] compList = getComponents();
        for (int i = 0; i < compList.length; i++) {
            if (!(compList[i] instanceof JScrollBar)) {
                compList[i].setBackground (color);
            }
        }
    }
}
