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

final class SimpleCommandLog {
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private Command fLastCommand = null;
    private Command fCurrentCommand = null;
    private PanelEventBroadcaster fListener;

    private boolean fBaseIsModified;

    private int fLogSize = 14;

    public SimpleCommandLog(PanelEventBroadcaster listener) {
        fListener = listener;
        fBaseIsModified = false;
    }

    /** adds the specfied command to the top of the command stack
    * (any undone commands on the stack are removed)
    * This function assumes the command has already been executed (i.e., its execute() method
    * has been called, or an equivalent action has been taken) */
    void add(Command newCommand) {
        // if there are commands on the stack that have been undone, they are
        // dropped on the floor here
        newCommand.setPreviousCommand(fCurrentCommand);
        
        final Command oldLastCommand = fLastCommand;
        fLastCommand = null;
        
        fCurrentCommand = newCommand;
        limitCommands(fLogSize);
        
        if (oldLastCommand != null) {
            fListener.textStateChanged(TextPanelEvent.UNDO_STATE_CHANGED);
        }
    }

    /**
     * If the command list is longer than logSize, truncate it.
     * This method traverses the list each time, and is not a model
     * of efficiency.  It's a temporary way to plug this memory leak
     * until I can implement a bounded command log.
     */
    private void limitCommands(int logSize) {

        if (logSize == 0) {
            fCurrentCommand = null;
        }
        else {
            Command currentCommand = fCurrentCommand;
            int remaining = logSize-1;
            while (currentCommand != null && remaining > 0) {
                currentCommand = currentCommand.previousCommand();
                remaining -= 1;
            }
            if (currentCommand != null) {
                currentCommand.setPreviousCommand(null);
            }
        }
    }

    /** adds the specfied command to the top of the command stack and executes it */
    void addAndDo(Command newCommand) {
        add(newCommand);
        newCommand.execute();

        fListener.textStateChanged(TextPanelEvent.UNDO_STATE_CHANGED);
    }

    /** undoes the command on the top of the command stack, if there is one */
    void undo() {
        if (fCurrentCommand != null) {
            Command current = fCurrentCommand;
            current.undo();

            fCurrentCommand = current.previousCommand();

            current.setPreviousCommand(fLastCommand);
            fLastCommand = current;

            fListener.textStateChanged(TextPanelEvent.UNDO_STATE_CHANGED);
        }
    }

    /** redoes the last undone command on the command stack, if there are any */
    void redo() {
        if (fLastCommand != null) {
            Command last = fLastCommand;
            last.redo();

            fLastCommand = last.previousCommand();

            last.setPreviousCommand(fCurrentCommand);
            fCurrentCommand = last;

            fListener.textStateChanged(TextPanelEvent.UNDO_STATE_CHANGED);
        }
    }

    public boolean canUndo() {
        return fCurrentCommand != null;
    }

    public boolean canRedo() {
        return fLastCommand != null;
    }

    public boolean isModified() {

        if (fCurrentCommand == null) {
            return fBaseIsModified;
        }
        else {
            return fCurrentCommand.isModified();
        }
    }

    public void setModified(boolean modified) {

        if (fCurrentCommand == null) {
            fBaseIsModified = modified;
        }
        else {
            fCurrentCommand.setModified(modified);
        }
    }

    public void clearLog() {

        if (fCurrentCommand != null) {
            fBaseIsModified = fCurrentCommand.isModified();
        }
        // variable not used boolean changed = fCurrentCommand != null || fLastCommand != null;
        fCurrentCommand = null;
        fLastCommand = null;
        
        fListener.textStateChanged(TextPanelEvent.UNDO_STATE_CHANGED);
    }

    public void setLogSize(int size) {

        if (size < 0) {
            throw new IllegalArgumentException("log size cannot be negative");
        }
        
        if (size < fLogSize) {
            limitCommands(size);
        }
        
        fLogSize = size;
        
        if (fLastCommand != null || size == 0) {
            fLastCommand = null;
            fListener.textStateChanged(TextPanelEvent.UNDO_STATE_CHANGED);
        }
    }

    public int getLogSize() {

        return fLogSize;
    }
}
