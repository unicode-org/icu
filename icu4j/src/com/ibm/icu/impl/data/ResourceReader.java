package com.ibm.text.resources;
import java.io.*;

/**
 * A reader for text resource data in the current package.  The
 * resource data is loaded through the class loader, so it will
 * typically be a file in the same directory as the *.class files, or
 * a file within a JAR file in the corresponding subdirectory.  The
 * file must be a text file in one of the supported encoding; when the
 * resource is opened by constructing a <code>ResourceReader</code>
 * object the encoding is specified.
 *
 * <p>Although this class has a public API, it is designed for
 * internal use by classes in the <code>com.ibm.text</code> package.
 *
 * @author Alan Liu
 */
public class ResourceReader {
    private BufferedReader reader;
    private String resourceName;
    private String encoding;
    private boolean isReset; // TRUE if we are at the start of the file

    /**
     * Construct a reader object for the text file of the given name
     * in this package, in the given encoding.
     * @param resourceName thqe name of the text file located in this
     * package
     * @param encoding the encoding of the text file; if unsupported
     * an exception is thrown
     * @exception UnsupportedEncodingException if
     * <code>encoding</code> is not supported by the JDK.
     */
    public ResourceReader(String resourceName, String encoding)
        throws UnsupportedEncodingException {

        this.resourceName = resourceName;
        this.encoding = encoding;
        isReset = false;
        _reset();
    }

    /**
     * Read and return the next line of the file or <code>null</code>
     * if the end of the file has been reached.
     */
    public String readLine() throws IOException {
        isReset = false;
        return reader.readLine();
    }

    /**
     * Reset this reader so that the next call to
     * <code>readLine()</code> returns the first line of the file
     * again.  This is a somewhat expensive call, however, calling
     * <code>reset()</code> after calling it the first time does
     * nothing if <code>readLine()</code> has not been called in
     * between.
     */
    public void reset() {
        try {
            _reset();
        } catch (UnsupportedEncodingException e) {}
        // We swallow this exception, if there is one.  If the encoding is
        // invalid, the constructor will have thrown this exception already and
        // the caller shouldn't use the object afterwards.
    }

    /**
     * Reset to the start by reconstructing the stream and readers.
     * We could also use mark() and reset() on the stream or reader,
     * but that would cause them to keep the stream data around in
     * memory.  We don't want that because some of the resource files
     * are large, e.g., 400k.
     */
    private void _reset() throws UnsupportedEncodingException {
        if (isReset) {
            return;
        }
        InputStream is = getClass().getResourceAsStream(resourceName);
        if (is == null) {
            throw new IllegalArgumentException("Can't open " + resourceName);
        }
        InputStreamReader isr = new InputStreamReader(is, encoding);
        reader = new BufferedReader(isr);
        isReset = true;
    }
}
