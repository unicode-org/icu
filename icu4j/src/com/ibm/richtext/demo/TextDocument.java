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
package com.ibm.richtext.demo;

import java.io.*;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.textpanel.MTextPanel;
import com.ibm.richtext.textlayout.attributes.AttributeMap;

/**
 * A TextDocument handles the association between a file on disk
 * and a TextPanel.
 */
public final class TextDocument {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int BUF_SIZE = 1024;
    
    private String fTitle;
    private MConstText fText;
    private File fFile;
    private MTextPanel fTextPanel = null;
    private boolean isModified = false;
    private int fFormat = STYLED_TEXT;
    
    private TextDocument(String title,
                         MConstText text,
                         File file,
                         int format) {
        
        fTitle = title;
        fText = text;
        fFile = file;
        setFormat(format);
    }
    
    public static final int STYLED_TEXT = 0;
    public static final int PLAIN_TEXT = 1;
    
    /** 
     * Return a new TextDocument with no associated file and
     * empty text.
     */
    public static TextDocument createEmpty(String title, int format) {
        
        return new TextDocument(title, new StyledText(), null, format);
    }
    
    /**
     * Return a TextDocument created from the contents of the given
     * file.  This method may throw an exception if the file cannot
     * be read.  In particular, if the format is given as STYLED_TEXT
     * but the file does not contain a serialized MConstText, 
     * this method will throw StreamCorruptedException.
     */
    public static TextDocument createFromFile(File file, int format) throws Exception {
        
        if (format != STYLED_TEXT && format != PLAIN_TEXT) {
            throw new IllegalArgumentException("Invalid format");
        }
        
        MConstText text;
        if (format == STYLED_TEXT) {
            text = readMText(file);
        }
        else {
            text = readMTextFromTextFile(file);
        }
        
        TextDocument document = new TextDocument(file.getName(), 
                                                 text,
                                                 file,
                                                 format);
        return document;
    }
    
    /**
     * Return true if this document's text differs from the contents
     * of its file.
     */
    public boolean isModified() {
        
        if (fTextPanel == null) {
            return isModified;
        }
        else {
            return fTextPanel.isModified();
        }
    }
    
    /**
     * Set the MTextPanel that will be used to edit the document's
     * text.  The document's text becomes the contents of the
     * MTextPanel.
     */
    public void setTextPanel(MTextPanel textPanel) {

        if (fTextPanel != null) {
            fText = fTextPanel.getText();
            isModified = fTextPanel.isModified();
        }
        
        fTextPanel = textPanel;
        
        if (fTextPanel != null) {
            fTextPanel.setText(fText);
            fText = null;
            fTextPanel.setModified(isModified);
            fTextPanel.clearCommandLog();
        }
    }
    
    public File getFile() {
    
        return fFile;
    }
    
    /**
     * Set this document's file.  The document's title will
     * change to the file name.  The file cannot be null.
     */
    public void setFile(File file) {
        
        fFile = file;
        fTitle = file.getName();
    }
    
    /**
     * Set the format of this document.  The format determines
     * whether the document will be written to files as styled
     * text or plain characters.
     */
    public void setFormat(int format) {
        
        if (format != STYLED_TEXT && format != PLAIN_TEXT) {
            throw new IllegalArgumentException("Invalid format");
        }
        fFormat = format;
    }
    
    /**
     * Return the format of this document.
     */
    public int getFormat() {
        
        return fFormat;
    }
    
    /**
     * Write the document's text to its file.  If the document does
     * not have an associated file then this method is equivalent to
     * saveAs.  This method returns true if the save operation succeeds.
     */
    public boolean save() {

        if (fFile == null) {
            throw new RuntimeException("Can't save without a file.");
        }
                
        MConstText text = getText();
        boolean success = fFormat==STYLED_TEXT? writeMText(fFile, text) :
                                                writePlainMText(fFile, text);
        if (success && fTextPanel != null) {
            fTextPanel.setModified(false);
        }
        return success;
    }
    
    /** 
     * Return this document's styled text.
     */
    public MConstText getText() {
        
        if (fTextPanel == null) {
            return fText;
        }
        else {
            return fTextPanel.getText();
        }
    }
    
    /**
     * Return the title of this document.
     */
    public String getTitle() {
        
        return fTitle;
    }
    
    /**
     * Return the MText serialized in the given file.
     * In case of an error return null.
     */
    private static MConstText readMText(File file) throws Exception {

        FileInputStream inStream = null;
        
        try {
            inStream = new FileInputStream(file);
            ObjectInputStream objStream = new ObjectInputStream(inStream);

            return (MConstText) objStream.readObject();
        }
        finally {
            if (inStream != null) {
                try {
                    inStream.close();
                }
                catch(IOException e) {
                    System.out.print("");
                }
            }
        }
    }
    
    /**
     * Read the given file as a plain text file, and return its
     * contents as an MConstText.  The character and paragraph styles in 
     * the returned text will be EMPTY_ATTRIBUTE_MAP.
     */
    private static MConstText readMTextFromTextFile(File file) throws Exception {
        
        InputStreamReader in = null;
        
        try {
            in = new FileReader(file);
            
            MText text = new StyledText();
            
            char[] buf = new char[BUF_SIZE];
            int read;
            while ((read=in.read(buf, 0, buf.length)) != -1) {
                int len = text.length();
                text.replace(len, len, buf, 0, read, AttributeMap.EMPTY_ATTRIBUTE_MAP);
            }
            return text;
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch(IOException e) {
                    System.out.print("");
                }
            }
        }
    }

    /**
     * Attempt to save the given text in the given file.
     * @return true if the operation succeeded
     */
    private static boolean writeMText(File file, MConstText text) {

        Throwable error = null;
        OutputStream outStream = null;
        
        try {
            outStream = new FileOutputStream(file);
            ObjectOutputStream objStream = new ObjectOutputStream(outStream);

            objStream.writeObject(text);
        }
        catch(IOException e) {
            error = e;
        }
        catch(ClassCastException e) {
            error = e;
        }
        finally {
            if (outStream != null) {
                try {
                    outStream.close();
                }
                catch(IOException e) {
                    System.out.print("");
                }
            }
        }


        if (error != null) {
            error.printStackTrace();
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Write the given MConstText to the given file as plain text.
     */
    private static boolean writePlainMText(File file, MConstText text) {

        Throwable error = null;
        OutputStreamWriter outStream = null;
        
        try {
            outStream = new FileWriter(file);
            char[] buf = new char[BUF_SIZE];
            int length = text.length();
            int start = 0;
            do {
                int count = Math.min(length-start, buf.length);
                text.extractChars(start, start+count, buf, 0);
                outStream.write(buf, 0, count);
                start += count;
            } while (start < length);
        }
        catch(IOException e) {
            error = e;
        }
        finally {
            if (outStream != null) {
                try {
                    outStream.close();
                }
                catch(IOException e) {
                    System.out.print("");
                }
            }
        }


        if (error != null) {
            error.printStackTrace();
            return false;
        }
        else {
            return true;
        }
    }
}