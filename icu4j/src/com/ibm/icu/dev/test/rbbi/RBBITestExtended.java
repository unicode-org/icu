/*
 * Created on May 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.icu.dev.test.rbbi;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUData;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * @author andy
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RBBITestExtended extends TestFmwk {
	public static void main(String[] args)throws Exception {
        new RBBITestExtended().run(args);
	}

	
	
	public RBBITestExtended() { 
	}

	public void TestExtended() {
		InputStreamReader isr = null;
		try {
			InputStream is = ICUData.getStream(RBBITestExtended.class, "rbbitst.txt");
            if (is == null) {
                errln("Could not open test data file.");
                return;
            }
			isr = new InputStreamReader(is, "UTF-8");			
			int c;
			for (;;) {
				c = isr.read();
				if (c < 0) {
					break;
				}
				//System.out.print((char)c);
			}
			
		} catch (IOException e) {
			errln(e.toString());
		}
		
	}
}
