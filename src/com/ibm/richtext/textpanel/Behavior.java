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

import java.awt.Graphics;
import java.awt.Rectangle;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/** A class that handles events for a BehaviorOwner.
* A behavior enacpsulates some piece of the event-handling logic for a component.
* This allows the client to separate event-handling logic out into separate classes
* according to function, or to dynamically change the way a component handles
* events without adding a lot of special-case code to the panel itself.
* Behaviors are stored in a linked list, and all behaviors get a crack at an event before
* the owner gets a crack at them (right now, we rely on objects that implement
* BehaviorOwner to support these semantics).
* Behavior provides all the same event-handling functions that Component provides, and
* they all have exactly the same syntax and semantics. */
abstract class Behavior {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private Behavior fNextBehavior = null;
    private BehaviorOwner fOwner = null;

    static class EventType {

        EventType() {
        }
    }

    // events - should these be in TextPanel (or elsewhere)?

    // This event's WHAT parameter is a TextRange instance
    static final EventType SELECT = new EventType();

    // No WHAT param for these:
    static final EventType CUT = new EventType();
    static final EventType COPY = new EventType();
    static final EventType PASTE = new EventType();
    static final EventType CLEAR = new EventType();
    static final EventType UNDO = new EventType();
    static final EventType REDO = new EventType();
    static final EventType CLEAR_COMMAND_LOG = new EventType();

    // WHAT param is a StyleModifier
    static final EventType CHARACTER_STYLE_MOD = new EventType();
    static final EventType PARAGRAPH_STYLE_MOD = new EventType();

    // With this event, values of the WHAT parameter are
    // either Boolean.TRUE or Boolean.FALSE
    static final EventType SET_MODIFIED = new EventType();

    // WHAT param is a TextReplacement
    static final EventType REPLACE = new EventType();

    // WHAT param is an Integer
    static final EventType SET_COMMAND_LOG_SIZE = new EventType();

    public Behavior() {
    }

    public void addToOwner(BehaviorOwner owner) {
        removeFromOwner();
        fOwner = owner;
        setNextBehavior(owner.getBehavior());
        owner.setBehavior(this);
    }

    public boolean focusGained(FocusEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.focusGained(e);
        else
            return false;
    }

    public boolean focusLost(FocusEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.focusLost(e);
        else
            return false;
    }

    public boolean keyPressed(KeyEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.keyPressed(e);
        else
            return false;
    }

    public boolean keyTyped(KeyEvent e) {

        if (fNextBehavior != null) {
            return fNextBehavior.keyTyped(e);
        }
        else {
            return false;
        }
    }

    public boolean keyReleased(KeyEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.keyReleased(e);
        else
            return false;
    }

    public boolean mouseDragged(MouseEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.mouseDragged(e);
        else
            return false;
    }

    public boolean mouseEntered(MouseEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.mouseEntered(e);
        else
            return false;
    }

    public boolean mouseExited(MouseEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.mouseExited(e);
        else
            return false;
    }

    public boolean mouseMoved(MouseEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.mouseMoved(e);
        else
            return false;
    }

    public boolean mousePressed(MouseEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.mousePressed(e);
        else
            return false;
    }

    public boolean mouseReleased(MouseEvent e) {
        if (fNextBehavior != null)
            return fNextBehavior.mouseReleased(e);
        else
            return false;
    }

    public final Behavior nextBehavior() {
        return fNextBehavior;
    }

    public boolean paint(Graphics g, Rectangle drawRect) {
        if (fNextBehavior != null)
            return fNextBehavior.paint(g, drawRect);
        else
            return false;
    }

    public void removeFromOwner() {
        if (fOwner != null) {
            if (fOwner.getBehavior() == this)
                fOwner.setBehavior(nextBehavior());
            else {
                Behavior    current = fOwner.getBehavior();

                while (current != null && current.nextBehavior() != this)
                    current = current.nextBehavior();
                if (current != null)
                    current.setNextBehavior(nextBehavior());
            }
            setNextBehavior(null);
            fOwner = null;
        }
    }

    public final void setNextBehavior(Behavior next) {
        fNextBehavior = next;
    }

    public boolean textControlEventOccurred(EventType event, Object data) {
        if (fNextBehavior != null)
            return fNextBehavior.textControlEventOccurred(event, data);
        else
            return false;
    }
}
