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

/**
 * This interface is implemented by classes which
 * receive change notifications from an MTextPanel.
 * @see MTextPanel
 * @see TextPanelEvent
 */
public interface TextPanelListener {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
     * Notify listener of an MTextPanel change.
     * @param event a TextPanelEvent indicating what happened
     */
    public void textEventOccurred(TextPanelEvent event);
    
    /**
     * Return true if listener needs to be notified of 
     * the given event type.  This allows a text panel to avoid
     * sending events to uninterested parties.
     * @param type an event ID from TextPanelEvent
     * @return true if this listener needs to be notified of
     * events of the given type
     */
    public boolean respondsToEventType(int type);
}
