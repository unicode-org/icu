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

package com.ibm.richtext.test;

import java.util.Random;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.KeyEvent;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import com.ibm.richtext.textpanel.KeyRemap;
import com.ibm.richtext.textpanel.KeyEventForwarder;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textpanel.TextPanel;
import com.ibm.richtext.textpanel.TextPanelEvent;
import com.ibm.richtext.textpanel.TextPanelListener;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.textlayout.attributes.TextAttribute;
import com.ibm.richtext.textlayout.attributes.AttributeMap;

import com.ibm.richtext.styledtext.StyleModifier;
import com.ibm.richtext.textlayout.attributes.AttributeSet;

// Note:  this used to be a TestFmwk test.  If you add
// more tests to it, be sure to add them to 
// com.ibm.richtext.test.unit.FmwkTestTextPanel.test() -
// otherwise they won't get run!

public class TestTextPanel /*extends TestFmwk*/ {

    private final class TestListener implements TextPanelListener {
        
        private int NO_WAY = 0;
        private int DEFINITELY = 1;
        private int MAYBE = 2;
        
        private final int[] status;
        
        TestListener(MTextPanel textPanel) {
            
            int length = TextPanelEvent.TEXT_PANEL_LAST -
                         TextPanelEvent.TEXT_PANEL_FIRST + 1;
            status = new int[length];
            textPanel.addListener(this);
            allowAll();
        }
        
        void refuseAll() {
            
            for (int i=0; i < status.length; i++) {
                status[i] = NO_WAY;
            }
        }
        
        void allowAll() {
            
            for (int i=0; i < status.length; i++) {
                status[i] = MAYBE;
            }
        }
        
        void expectEvent(int type) {
            
            int index = type - TextPanelEvent.TEXT_PANEL_FIRST;
            status[index] = DEFINITELY;
        }
        
        void allowEvent(int type) {
            int index = type - TextPanelEvent.TEXT_PANEL_FIRST;
            status[index] = MAYBE;
        }
        
        void assertNotExpectingEvents() {
            assertNotExpectingEvents(false, 0, false);
        }
        
        void assertNotExpectingEvents(int iterCount, boolean exp) {
            assertNotExpectingEvents(true, iterCount, exp);
        }
        
        private void assertNotExpectingEvents(boolean logDetails, int iterCount, boolean exp) {
            
            boolean e = false;
            for (int i=0; i < status.length; i++) {
                if (status[i] == DEFINITELY) {
                    if (logDetails) {
                        logMessage("Expecting event " +
                                        (i+TextPanelEvent.TEXT_PANEL_FIRST));
                        logMessage("iterationCount="+iterCount+";  expexting="+exp);
                    }
                    e = true;
                }
            }
            if (e) {
                reportError("Some events pending");
            }
        }
        
        public void textEventOccurred(TextPanelEvent event) {
            
            int index = event.getID() - TextPanelEvent.TEXT_PANEL_FIRST;
            if (status[index] == NO_WAY) {
                reportError("Unexpected event: " + event);
            }
            else if (status[index] == DEFINITELY) {
                status[index] = NO_WAY;
            }
        }
        
        public boolean respondsToEventType(int type) {
            
            return true;
        }
    }

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    private static final String STRING_CONTENT = "Some content";
    private static final int TEST_ITERS = 2;
    public static final MConstText MOD_TEXT =
                new StyledText("Styled", AttributeMap.EMPTY_ATTRIBUTE_MAP);

    private Clipboard fClipboard;
    private MTextPanel fTextPanel = null;
    private TestListener fListener = null;
    private int fRandSeed = 0;
    private Random rand;
    
    private static final int BIG_COMMAND_LOG_SIZE = 40;
    private static final int SMALL_COMMAND_LOG_SIZE = 8;

    private static final StyleModifier[] paraMods = {
        StyleModifier.createAddModifier(
                new AttributeMap(TextAttribute.LINE_FLUSH,
                                 TextAttribute.FLUSH_LEADING)),
        StyleModifier.createAddModifier(TextAttribute.LINE_FLUSH,
                                 TextAttribute.FLUSH_CENTER),
        StyleModifier.createAddModifier(TextAttribute.LINE_FLUSH,
                                 TextAttribute.FLUSH_TRAILING),
        StyleModifier.createAddModifier(TextAttribute.LINE_FLUSH,
                                 TextAttribute.FULLY_JUSTIFIED),
        StyleModifier.createAddModifier(TextAttribute.RUN_DIRECTION,
                                 TextAttribute.RUN_DIRECTION_RTL),
        StyleModifier.createAddModifier(TextAttribute.RUN_DIRECTION,
                                 TextAttribute.RUN_DIRECTION_LTR),
        StyleModifier.createRemoveModifier(
                new AttributeSet(TextAttribute.LINE_FLUSH)),
        StyleModifier.createRemoveModifier(
                new AttributeSet(TextAttribute.RUN_DIRECTION))
    };

    private static final int SELECT = 0;
    private static final int SET_CARET_POS = 1;
    private static final int SET_START = 2;
    private static final int SET_END = 3;

    // using both styles of add modifier: AttributeMap and
    // key-value, just for variety...
    private static final StyleModifier[] charMods = {
        StyleModifier.createAddModifier(
                new AttributeMap(TextAttribute.WEIGHT,
                                 TextAttribute.WEIGHT_BOLD)),
        StyleModifier.createAddModifier(TextAttribute.FOREGROUND,
                                 Color.green),
        StyleModifier.createAddModifier(
                new AttributeMap(TextAttribute.UNDERLINE,
                                 TextAttribute.UNDERLINE_ON).addAttributes(
                new AttributeMap(TextAttribute.SIZE, new Float(6)))),
        StyleModifier.createReplaceModifier(
                new AttributeMap(TextAttribute.FAMILY, "Dialog")),
        StyleModifier.createRemoveModifier(
                new AttributeSet(
                new Object[] { TextAttribute.WEIGHT,
                           TextAttribute.POSTURE,
                           TextAttribute.UNDERLINE,
                           TextAttribute.STRIKETHROUGH,
                           TextAttribute.SUPERSCRIPT })),
        StyleModifier.IDENTITY
    };

    private static final char[] TYPED_CHARS = new char[128 - ' ' + 3];
    static {
        TYPED_CHARS[0] = 8; // backspace
        TYPED_CHARS[1] = '\t';
        TYPED_CHARS[2] = '\n';
        for (int i=3; i < TYPED_CHARS.length; i++) {
            TYPED_CHARS[i] = (char) (' ' + i - 3);
        }
    }

    public TestTextPanel() {

        fClipboard = new Clipboard("TestTextPanel");
        incRandSeed();
    }

    protected void reportError(String message) {
    
        System.err.println(message);
        throw new RuntimeException(message);
        //super.errln(message);
    }
    
    protected void logMessage(String message) {
    
        System.err.println(message);
        //super.logMessage(message);
    }
    
    public TestTextPanel(MTextPanel panel) {
        
        this();
        setTextPanel(panel);
    }
    
    void incRandSeed() {
        
        rand = new Random(++fRandSeed);
    }
    
    int getRandSeed() {
        
        return fRandSeed;
    }

    int randInt(int limit) {

        return randInt(0, limit);
    }

    int randInt(int start, int limit) {

        if (start > limit) {
            throw new IllegalArgumentException("Range is 0-length.");
        }
        else if (start == limit) {
            return start;
        }

        return start + (Math.abs(rand.nextInt())%(limit-start)) ;
    }

    public void test() {

        AttributeMap bold = new AttributeMap(TextAttribute.WEIGHT,
                                             TextAttribute.WEIGHT_BOLD);
        MConstText text1 = new StyledText("Test contents. 1234\nHow about it?",
                                              AttributeMap.EMPTY_ATTRIBUTE_MAP);
        MConstText text2 = new StyledText("Another test string.", bold);

        _testWithText(text1);
        _testWithText(text2);
        _testWithText(new StyledText());

        StyledText big1 = new StyledText();
        for (int i=0; i < 50; i++) {
            big1.append(text1);
        }

        _testWithText(big1);
        StyledText big2 = new StyledText(text1);
        for (int i=0; i < 80; i++) {
            big2.append(text2);
        }

        _testWithText(big2);
    }
    
    private void setTextPanel(MTextPanel panel) {
        
        fTextPanel = panel;
        fListener = new TestListener(panel);
    }
    
    private void _testWithText(MConstText text) {

        setTextPanel(new TextPanel(text, fClipboard));

        for (int i=0; i < TEST_ITERS; i++) {
            _testSetSelection();
            _testModifications(MOD_TEXT, true);
            _testEditMenuOperations(fClipboard);
            _testModFlag(fTextPanel.getCommandLogSize());
            _testCommandLogControl();
        }
    }

    private void _testSelection(int function, 
                               final int aStart,
                               final int aLimit) {

        int oldStart = fTextPanel.getSelectionStart();
        int oldLimit = fTextPanel.getSelectionEnd();

        final int length = fTextPanel.getTextLength();
        
        int start = aStart;
        int limit = aLimit;
        
        if (start < 0) {
            start = 0;
        }
        else if (start > length) {
            start = length;
        }

        if (limit < start) {
            limit = start;
        }
        else if (limit > length) {
            limit = length;
        }
        
        fListener.refuseAll();
        
        if (oldStart != start || oldLimit != limit) {
            fListener.expectEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
            fListener.allowEvent(TextPanelEvent.SELECTION_STYLES_CHANGED);
        }
        if ((oldStart==oldLimit) != (start==limit)) {
            fListener.expectEvent(TextPanelEvent.SELECTION_EMPTY_CHANGED);
        }
        if (oldStart==oldLimit) {
            fListener.allowEvent(TextPanelEvent.UNDO_STATE_CHANGED);
        }

        switch(function) {
            case SELECT:
                fTextPanel.select(aStart, aLimit);
                break;
            case SET_CARET_POS:
                fTextPanel.setCaretPosition(aStart);
                break;
            case SET_START:
                fTextPanel.setSelectionStart(aStart);
                break;
            case SET_END:
                fTextPanel.setSelectionEnd(aLimit);
                break;
            default:
                throw new IllegalArgumentException("Invalid function");
        }
        
        if (fTextPanel.getSelectionStart() != start) {
            reportError("getSelectionStart is incorrect after set");
        }
        if (fTextPanel.getSelectionEnd() != limit) {
            reportError("getSelectionEnd is incorrect after set");
        }
        fListener.assertNotExpectingEvents();
        fListener.allowAll();
    }

    private void setAndTestSelection(int start, int limit) {
        
        _testSelection(SELECT, start, limit);
    }

    private void setAndTestCaret(int caretPos) {

        _testSelection(SET_CARET_POS, caretPos, caretPos);
    }

    private void setAndTestSelectionStart(int selStart) {

        int limit = fTextPanel.getSelectionEnd();
        _testSelection(SET_START, selStart, limit);
    }

    private void setAndTestSelectionEnd(int selEnd) {

        int start = fTextPanel.getSelectionStart();
        _testSelection(SET_END, start, selEnd);
    }

    public void _testSetSelection() {

        int textLength = fTextPanel.getTextLength();
        if (textLength != fTextPanel.getText().length()) {
            reportError("Text panel length is not correct");
        }

        setAndTestSelection(0, textLength / 2);
        setAndTestSelection(textLength / 2, textLength);
        setAndTestSelection(0, textLength);
        setAndTestSelection(-1, textLength+1);
        if (textLength > 0) {
            setAndTestSelection(0, textLength - 1);
            setAndTestSelection(0, 1);
        }

        final int incAmount = Math.max(1, textLength/5);
        for (int index = 0; index <= textLength; index += incAmount) {

            setAndTestCaret(index);
            setAndTestSelectionStart(textLength-index);
            setAndTestSelectionEnd(textLength);
            setAndTestSelectionStart(0);
            setAndTestSelectionEnd(textLength-index);
        }
    }

    /**
     * Text must be editable to pass this test.
     */
    public void _testModifications(MConstText insertionText,
                                   boolean restoreOldText) {

        MConstText oldText = new StyledText(fTextPanel.getText());
        final int insLength = insertionText.length();

        fListener.allowAll();
        fListener.expectEvent(TextPanelEvent.TEXT_CHANGED);
        fListener.expectEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
        fTextPanel.append(insertionText);
        fListener.assertNotExpectingEvents();
        
        if (fTextPanel.getSelectionStart() != oldText.length() + insLength) {
            reportError("Append didn't result in correct selection");
        }

        fListener.expectEvent(TextPanelEvent.TEXT_CHANGED);
        fListener.expectEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
        fTextPanel.insert(insertionText, 0);
        fListener.assertNotExpectingEvents();
        fListener.allowAll();
        
        if (fTextPanel.getSelectionStart() != insLength) {
            reportError("Insert didn't result in correct selection");
        }

        fTextPanel.replaceRange(insertionText, insLength, insLength+oldText.length());
        if (fTextPanel.getSelectionStart() != insLength*2) {
            reportError("Replace didn't result in correct selection");
        }
        if (fTextPanel.getSelectionEnd() != insLength*2) {
            reportError("Replace didn't result in correct selection");
        }
        if (fTextPanel.getTextLength() != insLength*3) {
            reportError("textLength is incorrect");
        }

        if (restoreOldText) {
            fTextPanel.setText(oldText);
            if (fTextPanel.getSelectionStart() != oldText.length()) {
                reportError("setText didn't result in correct selection");
            }
            if (fTextPanel.getTextLength() != oldText.length()) {
                reportError("length incorrect after setText");
            }
        }
        
        fListener.allowAll();
    }

    private static int iterationCount = 0;
    public void _testCommandLogControl() {

        fListener.refuseAll();
        iterationCount++;
        boolean exp = false;
        if (fTextPanel.canRedo() || BIG_COMMAND_LOG_SIZE==0) {
            fListener.expectEvent(TextPanelEvent.UNDO_STATE_CHANGED);
            exp = true;
        }
        //try {
            fTextPanel.setCommandLogSize(BIG_COMMAND_LOG_SIZE);
            
            if (fTextPanel.canRedo()) {
                reportError("canRedo after setCommandLogSize");
            }
            fListener.assertNotExpectingEvents(iterationCount, exp);
        //}
        //catch(Error e) {
        //    logMessage("iterationCount="+iterationCount+";  expexting="+exp);
        //    throw e;
        //}

        MConstText insText = new StyledText("7",
                                AttributeMap.EMPTY_ATTRIBUTE_MAP);

        final int origLength = fTextPanel.getTextLength();
        int start = origLength / 3;
        
        fListener.allowEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
        fListener.allowEvent(TextPanelEvent.SELECTION_STYLES_CHANGED);

        for (int i=start; i < BIG_COMMAND_LOG_SIZE+start; i++) {
            fListener.expectEvent(TextPanelEvent.UNDO_STATE_CHANGED);
            fListener.expectEvent(TextPanelEvent.TEXT_CHANGED);
            if (fTextPanel.getSelectionStart() != fTextPanel.getSelectionEnd()) {
                fListener.expectEvent(TextPanelEvent.SELECTION_EMPTY_CHANGED);
            }
            fTextPanel.insert(insText, i);
            fListener.assertNotExpectingEvents();
        }

        fListener.allowEvent(TextPanelEvent.SELECTION_EMPTY_CHANGED);
        
        for (int i=0; i < BIG_COMMAND_LOG_SIZE-1; i++) {
            fListener.expectEvent(TextPanelEvent.UNDO_STATE_CHANGED);
            fListener.expectEvent(TextPanelEvent.TEXT_CHANGED);
            fTextPanel.undo();
            fListener.assertNotExpectingEvents();
        }
        if (!fTextPanel.canUndo()) {
            reportError("Command log is too small");
        }
        
        fListener.allowAll();
        fTextPanel.undo();
        if (fTextPanel.canUndo()) {
            reportError("Command log is too large");
        }

        if (fTextPanel.getTextLength() != origLength * insText.length()) {
            reportError("Text length was not restored");
        }

        for (int i=0; i < BIG_COMMAND_LOG_SIZE; i++) {
            fTextPanel.redo();
        }

        if (fTextPanel.getTextLength() != origLength+BIG_COMMAND_LOG_SIZE) {
            reportError("Text length was not restored after redo");
        }

        if (fTextPanel.canRedo()) {
            reportError("Should not be able to redo");
        }

        fTextPanel.undo();

        fTextPanel.setCommandLogSize(SMALL_COMMAND_LOG_SIZE);

        if (fTextPanel.canRedo()) {
            reportError("canRedo after setCommandLogSize(small)");
        }

        for (int i=0; i < SMALL_COMMAND_LOG_SIZE; i++) {
            if (!fTextPanel.canUndo()) {
                reportError("should be able to undo");
            }
            fTextPanel.undo();
        }
        if (fTextPanel.canUndo()) {
            reportError("should not be able to undo after setCommandLogSize(small)");
        }
        if (!fTextPanel.canRedo()) {
            reportError("why can't this redo???");
        }
        fTextPanel.redo();

        fTextPanel.clearCommandLog();

        if (fTextPanel.canUndo() || fTextPanel.canRedo()) {
            reportError("Command log wasn't cleared");
        }
    }

    /**
     * Test cut, copy, paste, undo, redo, clear, canUndo, canRedo.
     * Text must be editable to pass this test.
     */
    public void _testEditMenuOperations(Clipboard clipboard) {
        
        if (clipboard != null) {
            // test paste and undo / redo
            Transferable tr = new StringSelection(STRING_CONTENT);
            clipboard.setContents(tr, new ClipboardOwner() {
                public void lostOwnership(Clipboard c, Transferable t) {
                }
            });
            if (!fTextPanel.clipboardNotEmpty()) {
                reportError("MTextPanel doesn't recognize string content.");
            }

            fTextPanel.setCaretPosition(fTextPanel.getSelectionStart());
            int oldLength = fTextPanel.getTextLength();
            fTextPanel.paste();
            if (fTextPanel.getTextLength() != oldLength + STRING_CONTENT.length()) {
                reportError("Text length is wrong after paste.");
            }

            if (!fTextPanel.canUndo()) {
                reportError("canUndo should be true");
            }
            fTextPanel.undo();
            if (fTextPanel.getTextLength() != oldLength) {
                reportError("Length is wrong after undo");
            }
            if (!fTextPanel.canRedo()) {
                reportError("canRedo should be true");
            }
            fTextPanel.redo();
            if (fTextPanel.getTextLength() != oldLength + STRING_CONTENT.length()) {
                reportError("Text length is wrong after redo.");
            }
        }

        int origLength = fTextPanel.getTextLength();
        fTextPanel.selectAll();
        fTextPanel.clear();
        if (fTextPanel.getTextLength() != 0) {
            reportError("Length is nonzero after clear");
        }
        if (!fTextPanel.canUndo()) {
            reportError("canUndo should be true");
        }
        fTextPanel.undo();
        if (fTextPanel.getTextLength() != origLength) {
            reportError("Old text not restored");
        }

        if (origLength > 0) {

            fTextPanel.select(0, 1);
            fTextPanel.cut();
            if (fTextPanel.getTextLength() != origLength-1) {
                reportError("Length wrong after cut");
            }
            fTextPanel.paste();
            if (fTextPanel.getTextLength() != origLength) {
                reportError("Length wrong after paste");
            }
            fTextPanel.select(0, origLength);
            fTextPanel.copy();
            fTextPanel.setCaretPosition(0);
            fTextPanel.paste();
            if (fTextPanel.getTextLength() != 2*origLength) {
                reportError("Length wrong after paste");
            }
            fTextPanel.undo();
            if (fTextPanel.getTextLength() != origLength) {
                reportError("Length wrong after undo");
            }
        }
    }

    private void setAndTestModFlag(final int depth,
                                   boolean modified) {

        fTextPanel.setModified(modified);
        for (int i=0; i < depth; i++) {
            if (!fTextPanel.canUndo()) {
                reportError("Panel cannot undo at valid depth.  Depth=" + i);
            }
            fTextPanel.undo();
            fTextPanel.setModified(modified);
        }

        // check that all mod flags are false:
        if (fTextPanel.isModified() != modified) {
            reportError("isModified is not correct");
        }

        for (int i=0; i < depth; i++) {
            fTextPanel.redo();
            if (fTextPanel.isModified() != modified) {
                reportError("isModified is not correct");
            }
        }
    }

    /**
     * Make <code>depth</code> modifications to the text in textfTextPanel.
     * Set the modified flag on each operation, and then retrieve its
     * value.  Finally, undo the modifications.
     */
    public void _testModFlag(final int depth) {

        final int oldLength = fTextPanel.getTextLength();

        for (int i=0; i < depth; i++) {
            fTextPanel.insert(MOD_TEXT, 0);
        }

        setAndTestModFlag(depth, false);
        setAndTestModFlag(depth, true);

        for (int i=0; i < depth; i++) {
            fTextPanel.undo();
        }

        if (fTextPanel.getTextLength() != oldLength) {
            reportError("Undo did not restore old text.");
        }
    }

    void applyCharacterStyle() {

        StyleModifier stMod = charMods[randInt(charMods.length)];
        fListener.refuseAll();
        fListener.expectEvent(TextPanelEvent.SELECTION_STYLES_CHANGED);
        if (fTextPanel.getSelectionStart() != fTextPanel.getSelectionEnd()) {
            fListener.expectEvent(TextPanelEvent.TEXT_CHANGED);
            fListener.allowEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
        }
        fListener.allowEvent(TextPanelEvent.UNDO_STATE_CHANGED);
        fTextPanel.modifyCharacterStyleOnSelection(stMod);
        fListener.assertNotExpectingEvents();
        fListener.allowAll();
    }


    void applyParagraphStyle() {

        fListener.refuseAll();
        fListener.expectEvent(TextPanelEvent.SELECTION_STYLES_CHANGED);
        fListener.expectEvent(TextPanelEvent.TEXT_CHANGED);
        fListener.allowEvent(TextPanelEvent.UNDO_STATE_CHANGED);
        fListener.allowEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
        StyleModifier stMod = paraMods[randInt(paraMods.length)];
        fTextPanel.modifyParagraphStyleOnSelection(stMod);
        fListener.assertNotExpectingEvents();
        fListener.allowAll();
    }

    void applyKeyRemap() {

        fListener.refuseAll();
        fListener.expectEvent(TextPanelEvent.KEYREMAP_CHANGED);
        int op = randInt(5);
        switch (op) {

            case 0:
                fTextPanel.setKeyRemap(KeyRemap.getIdentityRemap());
                break;

            case 1:
                fTextPanel.setKeyRemap(KeyRemap.getArabicTransliteration());
                break;

            case 2:
                fTextPanel.setKeyRemap(KeyRemap.getHebrewTransliteration());
                break;

            case 3:
                fTextPanel.setKeyRemap(KeyRemap.getIsraelNikud());
                break;
            
            case 4:
                //fTextPanel.setKeyRemap(KeyRemap.getThaiKetmanee());
                fTextPanel.setKeyRemap(KeyRemap.getIsraelNikud());
                break;
            
            default:
                reportError("Invalid operation!");
        }
        fListener.assertNotExpectingEvents();
        fListener.allowAll();
    }

    void resizeFrame(Frame frame) {

        fListener.refuseAll();
        fListener.allowEvent(TextPanelEvent.FORMAT_WIDTH_CHANGED);
        int wd = randInt(50, 1000);
        int ht = randInt(20, 800);

        frame.setSize(wd, ht);
        fListener.allowAll();
    }

    void selectText() {

        int selStart = randInt(-10, fTextPanel.getTextLength());
        int selLimit = randInt(0, fTextPanel.getTextLength() + 10);
        _testSelection(SELECT, selStart, selLimit);
    }

    void undoRedo() {

        final int opCount = randInt(-10, 15);

        for (int i=opCount; i <= 0; i++) {
            fTextPanel.redo();
        }
        for (int i=0; i < opCount; i++) {
            fTextPanel.undo();
        }
    }
    
    void typeKeys() {

        final int keyCount = randInt(1, 100);
        TextPanel textPanel = (TextPanel) fTextPanel;
        
        KeyEventForwarder forwarder = new KeyEventForwarder(textPanel);
        
        fListener.refuseAll();
        fListener.allowEvent(TextPanelEvent.UNDO_STATE_CHANGED);
        
        if (fTextPanel.getSelectionStart() != fTextPanel.getSelectionEnd()) {
            fListener.expectEvent(TextPanelEvent.SELECTION_EMPTY_CHANGED);
        }
        
        for (int i=0; i < keyCount; i++) {
            char typedChar = TYPED_CHARS[randInt(TYPED_CHARS.length)];
            KeyEvent event = new KeyEvent(textPanel,
                                          KeyEvent.KEY_TYPED,
                                          0,
                                          0,
                                          KeyEvent.VK_UNDEFINED,
                                          typedChar);
            if (typedChar == 8 || typedChar == 0x7f) {
                fListener.allowEvent(TextPanelEvent.TEXT_CHANGED);
                fListener.allowEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
            }
            else {
                fListener.expectEvent(TextPanelEvent.TEXT_CHANGED);
                fListener.expectEvent(TextPanelEvent.SELECTION_RANGE_CHANGED);
            }
            forwarder.handleKeyEvent(event);
            //try {
                fListener.assertNotExpectingEvents(i, false);
            //}
            //catch(Error e) {
            //    logMessage("i="+i+"; typedChar="+Integer.toHexString(typedChar));
            //    throw e;
            //}
        }
        fListener.allowAll();
    }
}
