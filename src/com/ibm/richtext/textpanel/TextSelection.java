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
/*
    7/1/97 - caret blinks

    7/3/97 - fAnchor is no longer restricted to the start or end of the selection. {jbr}
            Also, removed fVisible - it was identical to enabled().
*/

package com.ibm.richtext.textpanel;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Rectangle;

import java.text.BreakIterator;

import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.textformat.TextOffset;

import com.ibm.richtext.textformat.MFormatter;

class TextSelection extends Behavior implements Runnable {
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    static final Color          HIGHLIGHTCOLOR = Color.pink;

    private TextComponent       fTextComponent;
    private MConstText          fText;
    private TextOffset          fStart;
    private TextOffset          fLimit;
    private TextOffset          fAnchor;
    private TextOffset          fUpDownAnchor = null;
    private BreakIterator       fBoundaries = null;
    private Color               fHighlightColor = HIGHLIGHTCOLOR;
    private PanelEventBroadcaster   fListener;
    private RunStrategy         fRunStrategy;
    private boolean             fMouseDown = false;
    private boolean             fHandlingKeyOrCommand = false;
    
    private boolean fCaretShouldBlink;
    private boolean fCaretIsVisible;
    private int fCaretCount;

    // formerly in base class
    private boolean fEnabled;

    private MouseEvent fPendingMouseEvent = null;

    private static final int kCaretInterval = 500;

    public void run() {

        final Runnable blinkCaret = new Runnable() {
            public void run() {
                fCaretIsVisible = !fCaretIsVisible;
                Graphics g = fTextComponent.getGraphics();
                if (g != null) {
                    //System.out.println("caretIsVisible: " + fCaretIsVisible);
                    drawSelection(g, fCaretIsVisible);
                }
                else {
                    // Not sure what else to do:
                    fCaretShouldBlink = false;
                }
            }
        };
        
        // blink caret
        while (true) {

            synchronized(this) {

                while (!fCaretShouldBlink) {
                    try {
                        wait();
                    }
                    catch(InterruptedException e) {
                        System.out.println("Caught InterruptedException in caret thread.");
                    }
                }

                ++fCaretCount;

                if (fCaretCount % 2 == 0) {
                    fRunStrategy.doIt(blinkCaret);
                }
            }

            try {
                Thread.sleep(kCaretInterval);
            }
            catch(InterruptedException e) {
            }
        }
    }



    public TextSelection(TextComponent textComponent,
                         PanelEventBroadcaster listener,
                         RunStrategy runStrategy) {
                            
        fTextComponent = textComponent;
        fText = textComponent.getText();
        fListener = listener;
        fRunStrategy = runStrategy;
        
        fStart = new TextOffset();
        fLimit = new TextOffset();
        fAnchor = new TextOffset();
        fMouseDown = false;

        fCaretCount = 0;
        fCaretIsVisible = true;
        fCaretShouldBlink = false;
        setEnabled(false);

        Thread caretThread = new Thread(this);
        caretThread.setDaemon(true);
        caretThread.start();
    }

    boolean enabled() {

        return fEnabled;
    }

    private void setEnabled(boolean enabled) {

        fEnabled = enabled;
    }

    public boolean textControlEventOccurred(Behavior.EventType event, Object what) {

        boolean result;
        fHandlingKeyOrCommand = true;
        
        if (event == Behavior.SELECT) {
            select((TextRange) what);
            result = true;
        }
        else if (event == Behavior.COPY) {
            fTextComponent.getClipboard().setContents(fText.extract(fStart.fOffset, fLimit.fOffset));
            fListener.textStateChanged(TextPanelEvent.CLIPBOARD_CHANGED);
            result = true;
        }
        else {
            result = false;
        }
        
        fHandlingKeyOrCommand = false;
        return result;
    }

    protected void advanceToNextBoundary(TextOffset offset) {
    
        // If there's no boundaries object, or if position at the end of the
        // document, return the offset unchanged
        if (fBoundaries == null) {
            return;
        }
        
        int position = offset.fOffset;
        
        if (position >= fText.length()) {
            return;
        }

        // If position is at a boundary and offset is before position,
        // leave it unchanged.  Otherwise move to next boundary.
        int nextPos = fBoundaries.following(position);
        if (fBoundaries.previous() == position && 
                offset.fPlacement==TextOffset.BEFORE_OFFSET) {
            return;
        }
        
        offset.setOffset(nextPos, TextOffset.AFTER_OFFSET);
    }

    protected void advanceToPreviousBoundary(TextOffset offset) {
    
        advanceToPreviousBoundary(offset, false);
    }
    
    private void advanceToPreviousBoundary(TextOffset offset, boolean alwaysMove) {
        // if there's no boundaries object, or if we're sitting at the beginning
        // of the document, return the offset unchanged
        if (fBoundaries == null) {
            return;
        }
        
        int position = offset.fOffset;

        if (position == 0) {
            return;
        }
        
        // If position is at a boundary, leave it unchanged.  Otherwise
        // move to previous boundary.
        if (position == fText.length()) {
            fBoundaries.last();
        }
        else {
            fBoundaries.following(position);
        }
        
        int prevPos = fBoundaries.previous();
        
        if (prevPos == position) {
            if (!alwaysMove && offset.fPlacement==TextOffset.AFTER_OFFSET) {
                return;
            }

            prevPos = fBoundaries.previous();
        }
                
        // and finally update the real offset with this new position we've found
        offset.setOffset(prevPos, TextOffset.AFTER_OFFSET);
    }

    private void doArrowKey(KeyEvent e, int key) {

        // when there's a selection range, the left and up arrow keys place an
        // insertion point at the beginning of the range, and the right and down
        // keys place an insertion point at the end of the range (unless the shift
        // key is down, of course)

        if (!fStart.equals(fLimit) && !e.isShiftDown()) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_UP)
                setSelRangeAndDraw(fStart, fStart, fStart);
            else
                setSelRangeAndDraw(fLimit, fLimit, fLimit);
        }
        else {
            if (!fAnchor.equals(fStart))
                fAnchor.assign(fLimit);

            TextOffset  liveEnd = (fStart.equals(fAnchor)) ? fLimit : fStart;
            TextOffset  newPos = new TextOffset();

            // if the control key is down, the left and right arrow keys move by whole
            // word in the appropriate direction (we use a line break object so that we're
            // not treating spaces and punctuation as words)
            if (e.isControlDown() && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT)) {
                fUpDownAnchor = null;
                fBoundaries = BreakIterator.getLineInstance();
                fBoundaries.setText(fText.createCharacterIterator());

                newPos.assign(liveEnd);
                if (key == KeyEvent.VK_RIGHT)
                    advanceToNextBoundary(newPos);
                else
                    advanceToPreviousBoundary(newPos, true);
            }

            // if we get down to here, this is a plain-vanilla insertion-point move,
            // or the shift key is down and we're extending or shortening the selection
            else {

                // fUpDownAnchor is used to keep track of the horizontal position
                // across a run of up or down arrow keys (this prevents accumulated
                // error from destroying our horizontal position)
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT)
                    fUpDownAnchor = null;
                else {
                    if (fUpDownAnchor == null) {
                        fUpDownAnchor = new TextOffset(liveEnd);
                    }
                }

                short   direction = MFormatter.eRight;  // just to have a default...

                switch (key) {
                    case KeyEvent.VK_UP: direction = MFormatter.eUp; break;
                    case KeyEvent.VK_DOWN: direction = MFormatter.eDown; break;
                    case KeyEvent.VK_LEFT: direction = MFormatter.eLeft; break;
                    case KeyEvent.VK_RIGHT: direction = MFormatter.eRight; break;
                }

                // use the formatter to determine the actual effect of the arrow key
                fTextComponent.findNewInsertionOffset(newPos, fUpDownAnchor, liveEnd, direction);
            }

            // if the shift key is down, the selection range is from the anchor point
            // the site of the last insertion point or the beginning point of the last
            // selection drag operation) to the newly-calculated position; if the
            // shift key is down, the newly-calculated position is the insertion point position
            if (!e.isShiftDown())
                setSelRangeAndDraw(newPos, newPos, newPos);
            else {
                if (newPos.lessThan(fAnchor))
                    setSelRangeAndDraw(newPos, fAnchor, fAnchor);
                else
                    setSelRangeAndDraw(fAnchor, newPos, fAnchor);
            }
        }

        scrollToShowSelectionEnd();
        fBoundaries = null;
    }

    private void doEndKey(KeyEvent e) {
        // ctrl-end moves the insertsion point to the end of the document,
        // ctrl-shift-end extends the selection so that it ends at the end
        // of the document

        TextOffset activeEnd, anchor;

        if (fAnchor.equals(fStart)) {
            activeEnd = new TextOffset(fStart);
            anchor = new TextOffset(fLimit);
        }
        else {
            activeEnd = new TextOffset(fLimit);
            anchor = new TextOffset(fStart);
        }

        if (e.isControlDown()) {
            TextOffset end = new TextOffset(fText.length(), TextOffset.BEFORE_OFFSET);

            if (e.isShiftDown())
                setSelRangeAndDraw(anchor, end, anchor);
            else
                setSelRangeAndDraw(end, end, end);
        }

        // end moves the insertion point to the end of the line containing
        // the end of the current selection
        // shift-end extends the selection to the end of the line containing
        // the end of the current selection

        else {

            int oldOffset = activeEnd.fOffset;

            activeEnd.fOffset = fTextComponent.lineRangeLimit(fTextComponent.lineContaining(activeEnd));
            activeEnd.fPlacement = TextOffset.BEFORE_OFFSET;

            if (fText.paragraphLimit(oldOffset) == activeEnd.fOffset &&
                    activeEnd.fOffset != fText.length() && activeEnd.fOffset > oldOffset) {
                activeEnd.fOffset--;
                activeEnd.fPlacement = TextOffset.AFTER_OFFSET;
            }

            if (!e.isShiftDown())
                setSelRangeAndDraw(activeEnd, activeEnd, activeEnd);
            else {
                if (activeEnd.lessThan(anchor))
                    setSelRangeAndDraw(activeEnd, anchor, anchor);
                else
                    setSelRangeAndDraw(anchor, activeEnd, anchor);
            }
        }

        scrollToShowSelectionEnd();
        fBoundaries = null;
        fUpDownAnchor = null;
    }

    private void doHomeKey(KeyEvent e) {
        // ctrl-home moves the insertion point to the beginning of the document,
        // ctrl-shift-home extends the selection so that it begins at the beginning
        // of the document

        TextOffset activeEnd, anchor;

        if (fAnchor.equals(fStart)) {
            activeEnd = new TextOffset(fStart);
            anchor = new TextOffset(fLimit);
        }
        else {
            activeEnd = new TextOffset(fLimit);
            anchor = new TextOffset(fStart);
        }

        if (e.isControlDown()) {

            TextOffset start = new TextOffset(0, TextOffset.AFTER_OFFSET);
            if (e.isShiftDown())
                setSelRangeAndDraw(start, anchor, anchor);
            else
                setSelRangeAndDraw(start, start, start);
        }

        // home moves the insertion point to the beginning of the line containing
        // the beginning of the current selection
        // shift-home extends the selection to the beginning of the line containing
        // the beginning of the current selection

        else {

            activeEnd.fOffset = fTextComponent.lineRangeLow(fTextComponent.lineContaining(activeEnd));
            activeEnd.fPlacement = TextOffset.AFTER_OFFSET;

            if (!e.isShiftDown())
                setSelRangeAndDraw(activeEnd, activeEnd, activeEnd);
            else {
                if (activeEnd.lessThan(anchor))
                    setSelRangeAndDraw(activeEnd, anchor, anchor);
                else
                    setSelRangeAndDraw(anchor, activeEnd, anchor);
            }
        }

        scrollToShowSelectionEnd();
        fBoundaries = null;
        fUpDownAnchor = null;
    }

    /** draws or erases the current selection
    * Draws or erases the highlight region or insertion caret for the current selection
    * range.
    * @param g The graphics environment to draw into
    * @param visible If true, draw the selection; if false, erase it
    */
    protected void drawSelection(Graphics g, boolean visible) {
        drawSelectionRange(g, fStart, fLimit, visible);
    }

    /** draws or erases a selection highlight at the specfied positions
    * Draws or erases a selection highlight or insertion caret corresponding to
    * the specified selecion range
    * @param g The graphics environment to draw into.  If null, this method does nothing.
    * @param start The beginning of the range to highlight
    * @param limit The end of the range to highlight
    * @param visible If true, draw; if false, erase
    */
    protected void drawSelectionRange(  Graphics    g,
                                        TextOffset  start,
                                        TextOffset  limit,
                                        boolean     visible) {
        if (g == null) {
            return;
        }
        Rectangle   selBounds = fTextComponent.getBoundingRect(start, limit);

        selBounds.width = Math.max(1, selBounds.width);
        selBounds.height = Math.max(1, selBounds.height);

        fTextComponent.drawText(g, selBounds, visible, start, limit, fHighlightColor);
    }

    protected TextOffset getAnchor() {
        return fAnchor;
    }

    public TextOffset getEnd() {
        return fLimit;
    }

    public Color getHighlightColor() {
        return fHighlightColor;
    }

    public TextOffset getStart() {
        return fStart;
    }

    public TextRange getSelectionRange() {

        return new TextRange(fStart.fOffset, fLimit.fOffset);
    }

    public boolean focusGained(FocusEvent e) {

        setEnabled(true);
        drawSelection(fTextComponent.getGraphics(), true);

        restartCaretBlinking(true);
        if (fPendingMouseEvent != null) {
            mousePressed(fPendingMouseEvent);
            fPendingMouseEvent = null;
        }
        fListener.textStateChanged(TextPanelEvent.CLIPBOARD_CHANGED);
 
        return true;
    }

    public boolean focusLost(FocusEvent e) {
        stopCaretBlinking();
        setEnabled(false);
        drawSelection(fTextComponent.getGraphics(), false);
        return true;
    }

    /**
     * Return true if the given key event can affect the selection
     * range.
     */
    public static boolean keyAffectsSelection(KeyEvent e) {

        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }

        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_HOME:
            case KeyEvent.VK_END:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
                return true;

            default:
                return false;
        }
    }

    public boolean keyPressed(KeyEvent e) {

        fHandlingKeyOrCommand = true;
        int key = e.getKeyCode();
        boolean result = true;
        
        switch (key) {
            case KeyEvent.VK_HOME:
                doHomeKey(e);
                break;
                
            case KeyEvent.VK_END:
                doEndKey(e);
                break;

            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
                doArrowKey(e, key);
                break;
                
            default:
                fUpDownAnchor = null;
                result = false;
                break;
        }
        
        fHandlingKeyOrCommand = false;
        return result;
    }

    public boolean mousePressed(MouseEvent e) {

        if (!enabled()) {
            fPendingMouseEvent = e;
            fTextComponent.requestFocus();
            return false;
        }

        if (fMouseDown)
            throw new Error("fMouseDown is out of sync with mouse in TextSelection.");

        fMouseDown = true;
        stopCaretBlinking();

        int x = e.getX(), y = e.getY();
        boolean wasZeroLength = rangeIsZeroLength(fStart, fLimit, fAnchor);
        
        TextOffset current = fTextComponent.pointToTextOffset(null, x, y, null, true);
        TextOffset anchorStart = new TextOffset();
        TextOffset anchorEnd = new TextOffset();

        fUpDownAnchor = null;

        // if we're not extending the selection...
        if (!e.isShiftDown()) {

            // if there are multiple clicks, create the appopriate type of BreakIterator
            // object for finding text boundaries (single clicks don't use a BreakIterator
            // object)
            if (e.getClickCount() == 2)
                fBoundaries = BreakIterator.getWordInstance();
            else if (e.getClickCount() == 3)
                fBoundaries = BreakIterator.getSentenceInstance();
            else
                fBoundaries = null;

            // if we're using a BreakIterator object, use it to find the nearest boundaries
            // on either side of the mouse-click position and make them our anchor range
            if (fBoundaries != null)
                fBoundaries.setText(fText.createCharacterIterator());

            anchorStart.assign(current);
            advanceToPreviousBoundary(anchorStart);
            anchorEnd.assign(current);
            advanceToNextBoundary(anchorEnd);
        }

        // if we _are_ extending the selection, determine our anchor range as follows:
        // fAnchor is the start of the anchor range;
        // the next boundary (after fAnchor) is the limit of the anchor range.

        else {

            if (fBoundaries != null)
                fBoundaries.setText(fText.createCharacterIterator());

            anchorStart.assign(fAnchor);
            anchorEnd.assign(anchorStart);

            advanceToNextBoundary(anchorEnd);
        }

        SelectionDragInteractor interactor = new SelectionDragInteractor(this, 
                                                                         fTextComponent,
                                                                         fRunStrategy,
                                                                         anchorStart,
                                                                         anchorEnd,
                                                                         current,
                                                                         x,
                                                                         y,
                                                                         wasZeroLength);

        interactor.addToOwner(fTextComponent);

        return true;
    }

    public boolean mouseReleased(MouseEvent e) {

        fPendingMouseEvent = null;
        return false;
    }

    // drag interactor calls this
    void mouseReleased(boolean zeroLengthChange) {

        fMouseDown = false;

        if (zeroLengthChange) {
            fListener.textStateChanged(TextPanelEvent.SELECTION_EMPTY_CHANGED);
        }
        fListener.textStateChanged(TextPanelEvent.SELECTION_RANGE_CHANGED);
        fListener.textStateChanged(TextPanelEvent.SELECTION_STYLES_CHANGED);

        // if caret drawing during mouse drags is supressed, draw caret now.

        restartCaretBlinking(true);
    }


    /** draws the selection
    * Provided, of course, that the selection is visible, the adorner is enabled,
    * and we're calling it to adorn the view it actually belongs to
    * @param g The graphics environment to draw into
    * @return true if we actually drew
    */
    public boolean paint(Graphics g, Rectangle drawRect) {
        // don't draw anything unless we're enabled and the selection is visible
        if (!enabled())
            return false;

        fTextComponent.drawText(g, drawRect, true, fStart, fLimit, fHighlightColor);
        return true;
    }

    /** scrolls the view to reveal the live end of the selection
    * (i.e., the end that moves if you use the arrow keys with the shift key down)
    */
    public void scrollToShowSelection() {
        Rectangle   selRect = fTextComponent.getBoundingRect(fStart, fLimit);

        fTextComponent.scrollToShow(selRect);
    }

    /** scrolls the view to reveal the live end of the selection
    * (i.e., the end that moves if you use the arrow keys with the shift key down)
    */
    public void scrollToShowSelectionEnd() {
        TextOffset  liveEnd;
        // variable not used Point[]     points;
        Rectangle   caret;

        if (fAnchor.equals(fStart))
            liveEnd = fLimit;
        else
            liveEnd = fStart;

        //points = fTextComponent.textOffsetToPoint(liveEnd);
        //caret = new Rectangle(points[0]);
        //caret = caret.union(new Rectangle(points[1]));
        caret = fTextComponent.getCaretRect(liveEnd);
        fTextComponent.scrollToShow(caret);
    }

    private void select(TextRange range) {
        // variable not used int textLength = fTextComponent.getText().length();

        TextOffset start = new TextOffset(range.start);

        stopCaretBlinking();
        setSelRangeAndDraw(start, new TextOffset(range.limit), start);
        restartCaretBlinking(true);
    }

    public void setHighlightColor(Color newColor) {
        fHighlightColor = newColor;
        if (enabled())
            drawSelection(fTextComponent.getGraphics(), true);
    }
    
    static boolean rangeIsZeroLength(TextOffset start, TextOffset limit, TextOffset anchor) {
        
        return start.fOffset == limit.fOffset && anchor.fOffset == limit.fOffset;
    }

    // sigh... look out for aliasing
    public void setSelectionRange(TextOffset newStart, TextOffset newLimit, TextOffset newAnchor) {

        boolean zeroLengthChange = rangeIsZeroLength(newStart, newLimit, newAnchor) != 
                                    rangeIsZeroLength(fStart, fLimit, fAnchor);
        TextOffset tempNewAnchor;
        if (newAnchor == fStart || newAnchor == fLimit) {
            tempNewAnchor = new TextOffset(newAnchor); // clone in case of aliasing
        }
        else {
            tempNewAnchor = newAnchor;
        }

        // DEBUG {jbr}

        if (newStart.greaterThan(newLimit))
            throw new IllegalArgumentException("Selection limit is before selection start.");

        if (newLimit != fStart) {
            fStart.assign(newStart);
            fLimit.assign(newLimit);
        }
        else {
            fLimit.assign(newLimit);
            fStart.assign(newStart);
        }

        fAnchor.assign(tempNewAnchor);

        if (fStart.fOffset == fLimit.fOffset) {
            fStart.fPlacement = fAnchor.fPlacement;
            fLimit.fPlacement = fAnchor.fPlacement;
        }
        
        if (!fMouseDown) {
            if (zeroLengthChange) {
                fListener.textStateChanged(TextPanelEvent.SELECTION_EMPTY_CHANGED);
            }
            fListener.textStateChanged(TextPanelEvent.SELECTION_RANGE_CHANGED);
            if (fHandlingKeyOrCommand) {
                fListener.textStateChanged(TextPanelEvent.SELECTION_STYLES_CHANGED);
            }
        }
    }

    private void sortOffsets(TextOffset offsets[]) {

        int i, j;

        for (i=0; i < offsets.length-1; i++) {
            for (j=i+1; j < offsets.length; j++) {
                if (offsets[j].lessThan(offsets[i])) {
                    TextOffset temp = offsets[j];
                    offsets[j] = offsets[i];
                    offsets[i] = temp;
                }
            }
        }

        // DEBUG {jbr}
        for (i=0; i < offsets.length-1; i++)
            if (offsets[i].greaterThan(offsets[i+1]))
                throw new Error("sortOffsets failed!");
    }

    private Rectangle getSelectionChangeRect(
                                    TextOffset rangeStart, TextOffset rangeLimit,
                                    TextOffset oldStart, TextOffset oldLimit,
                                    TextOffset newStart, TextOffset newLimit,
                                    boolean drawIfInsPoint) {

        if (!rangeStart.equals(rangeLimit))
            return fTextComponent.getBoundingRect(rangeStart, rangeLimit);

        // here, rangeStart and rangeLimit are equal

        if (rangeStart.equals(oldLimit)) {

            // range start is OLD insertion point.  Redraw if caret is currently visible.

            if (fCaretIsVisible)
                return fTextComponent.getBoundingRect(rangeStart, rangeStart);
        }
        else if (rangeStart.equals(newLimit)) {

            // range start is NEW insertion point.

            if (drawIfInsPoint)
                return fTextComponent.getBoundingRect(rangeStart, rangeStart);
        }

        return null;
    }

    private static boolean rectanglesOverlapVertically(Rectangle r1, Rectangle r2) {
        
        if (r1 == null || r2 == null) {
            return false;
        }
        
        return r1.y <= r2.y + r2.height || r2.y <= r1.y + r1.height;
    }
    
    // Update to show new selection, redrawing as little as possible

    private void updateSelectionDisplay(
                            TextOffset oldStart, TextOffset oldLimit,
                            TextOffset newStart, TextOffset newLimit, boolean drawIfInsPoint) {

        //System.out.println("newStart:" + newStart + "; newLimit:" + newLimit);

        TextOffset off[] = new TextOffset[4];

        off[0] = oldStart;
        off[1] = oldLimit;
        off[2] = newStart;
        off[3] = newLimit;

        sortOffsets(off);

        Rectangle r1 = getSelectionChangeRect(off[0], off[1], oldStart, oldLimit, newStart, newLimit, drawIfInsPoint);
        Rectangle r2 = getSelectionChangeRect(off[2], off[3], oldStart, oldLimit, newStart, newLimit, drawIfInsPoint);

        boolean drawSelection = drawIfInsPoint || !newStart.equals(newLimit);

        if (rectanglesOverlapVertically(r1, r2)) {

            fTextComponent.drawText(fTextComponent.getGraphics(), r1.union(r2), drawSelection, newStart, newLimit, fHighlightColor);
        }
        else {
            if (r1 != null)
                fTextComponent.drawText(fTextComponent.getGraphics(), r1, drawSelection, newStart, newLimit, fHighlightColor);
            if (r2 != null)
                fTextComponent.drawText(fTextComponent.getGraphics(), r2, drawSelection, newStart, newLimit, fHighlightColor);
        }
    }

    public void setSelRangeAndDraw(TextOffset newStart, TextOffset newLimit, TextOffset newAnchor) {

        // if the old and new selection ranges are the same, don't do anything
        if (fStart.equals(newStart) && fLimit.equals(newLimit) && fAnchor.equals(newAnchor))
            return;

        if (enabled())
            stopCaretBlinking();

        // update the selection on screen if we're enabled and visible

        TextOffset oldStart = new TextOffset(fStart), oldLimit = new TextOffset(fLimit);

        setSelectionRange(newStart, newLimit, newAnchor);

        if (enabled()) {

                // To supress drawing a caret during a mouse drag, pass !fMouseDown instead of true:
                updateSelectionDisplay(oldStart, oldLimit, fStart, fLimit, true);
        }

        if (!fMouseDown && enabled())
            restartCaretBlinking(true);
    }

    public void stopCaretBlinking() {

        synchronized(this) {
            fCaretShouldBlink = false;
        }
    }

/**
* Resume blinking the caret, if the selection is an insertion point.
* @param caretIsVisible true if the caret is displayed when this is called.
* This method relies on the client to display (or not display) the caret.
*/
    public void restartCaretBlinking(boolean caretIsVisible) {

        synchronized(this) {
            fCaretShouldBlink = fStart.equals(fLimit);
            fCaretCount = 0;
            fCaretIsVisible = caretIsVisible;

            if (fCaretShouldBlink) {
                try {
                    notify();
                }
                catch (IllegalMonitorStateException e) {
                    System.out.println("Caught IllegalMonitorStateException: "+e);
                }
            }
        }
    }

    public void removeFromOwner() {

        stopCaretBlinking();
        super.removeFromOwner();
    }

}
