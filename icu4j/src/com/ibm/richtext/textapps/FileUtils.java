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
package com.ibm.richtext.textapps;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.*;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;

public class FileUtils {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
     * Present the user with a file dialog, and replace
     * dest with the MText in the selected file, and return
     * the file objct.  If any errors occur, return null and
     * do not modify dest.
     */
    public static File userLoadMText(String title, MText dest, Frame owner) {

        FileDialog dialog = new FileDialog(owner, title, FileDialog.LOAD);
        dialog.show();
        String fileStr = dialog.getFile();
        String dirStr = dialog.getDirectory();

        if (fileStr != null) {
            File rval = new File(dirStr, fileStr);
            MConstText src = loadMText(rval);
            if (src != null) {
                dest.replaceAll(src);
                return rval;
            }
        }

        return null;
    }

    /**
     * Return the MText serialized in the given file.
     * In case of an error return null.
     */
    public static MConstText loadMText(File file) {

        Throwable error;

        try {
            FileInputStream inStream = new FileInputStream(file);
            ObjectInputStream objStream = new ObjectInputStream(inStream);

            MConstText text = (MConstText) objStream.readObject();
            inStream.close();
            return text;
        }
        catch(IOException e) {
            error = e;
        }
        catch(ClassNotFoundException e) {
            error = e;
        }
        catch(ClassCastException e) {
            error = e;
        }

        error.printStackTrace();
        return null;
    }

    /**
     * Prompt the user for the file if file is null.  Then save the
     * text in the file, if any.
     */
    public static File userSaveMText(File file, String title, MConstText text, Frame owner) {

        if (file == null) {

            FileDialog dialog = new FileDialog(owner, title, FileDialog.SAVE);
            dialog.show();
            String fileStr = dialog.getFile();
            String dirStr = dialog.getDirectory();

            if (fileStr != null) {
                file = new File(dirStr, fileStr);
            }
        }

        if (file != null) {

            saveMText(file, text);
        }

        return file;
    }

    public static void saveMText(File file, MConstText text) {

        Throwable error;

        try {
            OutputStream outStream = new FileOutputStream(file);
            ObjectOutputStream objStream = new ObjectOutputStream(outStream);

            objStream.writeObject(text);
            outStream.close();
            return;
        }
        catch(IOException e) {
            error = e;
        }
        catch(ClassCastException e) {
            error = e;
        }

        error.printStackTrace();
    }
}