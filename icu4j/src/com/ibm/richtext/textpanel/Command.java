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

abstract class Command {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private Command fPreviousCommand = null;

    // fModified is used to keep a textModified flag for
    // clients
    private boolean fModified;

    public Command() {
        fModified = true;
    }

    public Command previousCommand() {
        return fPreviousCommand;
    }

    public void setPreviousCommand(Command  previousCommand) {
        fPreviousCommand = previousCommand;
    }

    public abstract void execute();
    public abstract void undo();

    public void redo() {
        execute();
    }

    public final boolean isModified() {

        return fModified;
    }

    public final void setModified(boolean modified) {

        fModified = modified;
    }
}
