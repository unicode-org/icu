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

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.ibm.richtext.textformat.TextOffset;

final class SelectionDragInteractor extends Behavior implements Runnable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private TextComponent fTextComponent;
    private TextSelection fSelection;
    private RunStrategy fRunStrategy;
    
    private TextOffset fAnchorStart; // aliases text offsets - client beware
    private TextOffset fAnchorEnd;
    private TextOffset fCurrent;

    private final boolean fWasZeroLength;

    private int fCurrentX;
    private int fCurrentY;
    private boolean fMouseOutside;

    private Thread fAutoscrollThread = null;
    private boolean fThreadRun = true;

    private static final int kScrollSleep = 300;

    public SelectionDragInteractor(TextSelection selection,
                                   TextComponent textComponent,
                                   RunStrategy runStrategy,
                                   TextOffset anchorStart,
                                   TextOffset anchorEnd,
                                   TextOffset current,
                                   int initialX,
                                   int initialY,
                                   boolean wasZeroLength) {

        fTextComponent = textComponent;
        fSelection = selection;
        fRunStrategy = runStrategy;
        fAnchorStart = anchorStart;
        fAnchorEnd = anchorEnd;
        fCurrent = current;

        fCurrentX = initialX;
        fCurrentY = initialY;
        fMouseOutside = false;

        fWasZeroLength = wasZeroLength;
        
        setSelection();
    }
    
    public boolean textControlEventOccurred(Behavior.EventType event, Object what) {

        return true;
    }

    public boolean focusGained(FocusEvent event) {

        return true;
    }

    public boolean focusLost(FocusEvent event) {

        return true;
    }

    public boolean keyPressed(KeyEvent event) {

        return true;
    }

    public boolean keyTyped(KeyEvent event) {

        return true;
    }

    public boolean keyReleased(KeyEvent event) {

        return true;
    }

    public synchronized boolean mouseDragged(MouseEvent e) {

        int x = e.getX(), y = e.getY();
        if (fCurrentX != x || fCurrentY != y) {
            fCurrentX = x;
            fCurrentY = y;
            processMouseLocation();
        }
        return true;
    }

    public synchronized boolean mouseEnter(MouseEvent e) {

        fMouseOutside = false;
        return true;
    }

    public synchronized boolean mouseExited(MouseEvent e) {

        if (fAutoscrollThread == null) {
            fAutoscrollThread = new Thread(this);
            fAutoscrollThread.start();
        }
        fMouseOutside = true;
        notify();

        return true;
    }

    public synchronized boolean mouseReleased(MouseEvent e) {

        fMouseOutside = false;
        fThreadRun = false;
        if (fAutoscrollThread != null) {
            fAutoscrollThread.interrupt();
        }
        
        removeFromOwner();
        boolean isZeroLength = TextSelection.rangeIsZeroLength(fAnchorStart,
                                                               fAnchorEnd,
                                                               fCurrent);
        fSelection.mouseReleased(isZeroLength != fWasZeroLength);

        return true;
    }

    private void processMouseLocation() {

        fTextComponent.scrollToShow(fCurrentX, fCurrentY);
        fTextComponent.pointToTextOffset(fCurrent, fCurrentX, fCurrentY, null, true);
        setSelection();
    }

    private void setSelection() {

        if (fCurrent.greaterThan(fAnchorEnd)) {
            fSelection.advanceToNextBoundary(fCurrent);
            fSelection.setSelRangeAndDraw(fAnchorStart, fCurrent, fAnchorStart);
        }
        else if (fCurrent.lessThan(fAnchorStart)) {
            fSelection.advanceToPreviousBoundary(fCurrent);
            fSelection.setSelRangeAndDraw(fCurrent, fAnchorEnd, fAnchorStart);
        }
        else {
            fCurrent.assign(fAnchorEnd);
            fSelection.setSelRangeAndDraw(fAnchorStart, fAnchorEnd, fAnchorStart);
        }
    }

    public void run() {

        Runnable doMouseLoc = new Runnable() {
            public void run() {
                processMouseLocation();
            }
        };
        
        while (fThreadRun) {

            try {
                Thread.sleep(kScrollSleep);
            }
            catch(InterruptedException e) {
                return; // just quit scrolling
            }

            synchronized(this) {

                while (!fMouseOutside) {
                    try {
                        wait();
                    }
                    catch(InterruptedException e) {
                        return; // just quit scrolling
                    }
                }

                fRunStrategy.doIt(doMouseLoc);
            }
        }
    }
}
