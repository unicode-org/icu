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

import java.awt.Image;
import java.awt.Component;

class OffscreenBufferCache {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private Image fOffscreenBuffer = null;
    private Component fHost;

    OffscreenBufferCache(Component host) {

        fHost = host;
    }

    private Image makeBuffer(int width, int height) {

        return fHost.createImage(Math.max(width, 1), Math.max(height, 1));
    }

    Image getBuffer(int width, int height) {

        Image buffer = fOffscreenBuffer;

        if (buffer != null) {
            if (buffer.getWidth(fHost) >= width &&
                    buffer.getHeight(fHost) >= height) {
                return buffer;
            }
        }

        buffer = makeBuffer(width, height);
        fOffscreenBuffer = buffer;
        return buffer;
    }
}