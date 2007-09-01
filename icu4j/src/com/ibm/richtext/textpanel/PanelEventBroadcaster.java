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

import java.util.Vector;

/**
 * This class listens for text state change notifications
 * and broadcasts them to all of its listeners.
 */
final class PanelEventBroadcaster {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int FIRST = TextPanelEvent.TEXT_PANEL_FIRST;

    private final Vector[] fListeners;
    private final TextPanelEvent[] fEvents;

    /**
     * Construct a new PanelEventBroadcaster.
     * @param panel the TextPanel for which events are broadcasted
     */
    public PanelEventBroadcaster(MTextPanel panel) {

        int count = TextPanelEvent.TEXT_PANEL_LAST - FIRST + 1;

        fEvents = new TextPanelEvent[count];
        fListeners = new Vector[count];
        
        for (int i=0; i < fListeners.length; i++) {
            fEvents[i] = new TextPanelEvent(panel, i+FIRST);
            fListeners[i] = new Vector();
        }
    }

    /**
     * Add the given TextPanelListener to the TextPanelListeners to
     * which notifications are forwarded.
     * @param listener the listener to add
     */
    public synchronized void addListener(TextPanelListener listener) {

        for (int i=FIRST; i <= TextPanelEvent.TEXT_PANEL_LAST; i++) {
            Vector listeners = fListeners[i-FIRST];
            if (listener.respondsToEventType(i)) {
                if (!listeners.contains(listener)) {
                    listeners.addElement(listener);
                }
            }
        }
    }

    /**
     * Remove the given TextPanelListener from the TextPanelListeners to
     * which notifications are forwarded.
     * @param listener the listener to remove
     */
    public synchronized void removeListener(TextPanelListener listener) {

        for (int i=FIRST; i <= TextPanelEvent.TEXT_PANEL_LAST; i++) {
            Vector listeners = fListeners[i-FIRST];
            if (listener.respondsToEventType(i)) {
                listeners.removeElement(listener);
            }
        }
    }

    /**
     * Receive a notification and forward it to all listeners.
     * @changeCode one of the constants in the TextPanelListener class
     */
    public synchronized void textStateChanged(int id) {

        int index = id-FIRST;
        TextPanelEvent event = fEvents[index];
        Vector listeners = fListeners[index];
        
        int size = listeners.size();

        for (int i=0; i < size; i++) {

            TextPanelListener listener =
                            (TextPanelListener) listeners.elementAt(i);
            listener.textEventOccurred(event);
        }
    }
}
