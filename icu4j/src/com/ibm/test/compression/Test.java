/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1997 - 1999    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*/
package com.ibm.test.compression;

import java.io.PrintStream;

import com.ibm.text.UnicodeCompressor;
import com.ibm.text.UnicodeDecompressor;

public class Test
{
    /** verbose mode flag */
    private static boolean verbose = false;


    /** Print out a string, if in verbose mode */
    public static void print(String s)
    {
	if(verbose)
	    System.out.print(s);
    }

    /** Print out a string followed by a newline, if in verbose mode */
    public static void println(String s)
    {
	if(verbose)
	    System.out.println(s);
    }

    /** Print out a segment of a character array, if in verbose mode */
    public static void print(PrintStream out,
			     char [] chars,
			     int start,
			     int count)
    {
	if(verbose == false)
	    return;
	
	out.print('|');
	for(int i = start; i < start + count; ++i) {
	    out.print(chars[i]);
	}
	out.print('|');
    }

    /** Print out a segment of a character array, followed by a newline */
    public static void println(PrintStream out,
			       char [] chars,
			       int start,
			       int count)
    {
	if(verbose == false)
	    return;

	print(out, chars, start, count);
	out.println();
    }

    /** Decompress the two segments */
    public static String test(byte [] segment1,
			      byte [] segment2)
    {
	StringBuffer s = new StringBuffer();
	UnicodeDecompressor myDecompressor = new UnicodeDecompressor();

	int [] bytesRead = new int[1];
	char [] charBuffer = new char [2*(segment1.length + segment2.length)];
	int count1 = 0, count2 = 0;

	count1 = myDecompressor.decompress(segment1, 0, segment1.length,
					   bytesRead,
					   charBuffer, 0, charBuffer.length);
	
	println("Segment 1 (" + segment1.length + " bytes) " +
		"decompressed into " + count1  + " chars");
	println("Bytes consumed: " + bytesRead[0]);

	print("Got chars: ");
	println(System.out, charBuffer, 0, count1);
	s.append(charBuffer, 0, count1);

	count2 = myDecompressor.decompress(segment2, 0, segment2.length,
					   bytesRead,
					   charBuffer, count1, 
					   charBuffer.length);
	
	println("Segment 2 (" + segment2.length + " bytes) " +
		"decompressed into " + count2  + " chars");
	println("Bytes consumed: " + bytesRead[0]);

	print("Got chars: ");
	println(System.out, charBuffer, count1, count2);
	
	s.append(charBuffer, count1, count2);

	print("Result: ");
	println(System.out, charBuffer, 0, count1 + count2);
	println("====================");

	return s.toString();
    }


    public static void main(String [] args)
    {
	String result;
	int errCount = 0;

	if(args.length > 0)
	    verbose = true;

	// compressed segment breaking on a define window sequence
	/*                   B     o     o     t     h     SD1  */
	byte [] segment1 = { 0x42, 0x6f, 0x6f, 0x74, 0x68, 0x19 };

	// continuation
	/*                   IDX   ,           S     .          */
	byte [] segment2 = { 0x01, 0x2c, 0x20, 0x53, 0x2e };
	
	result = test(segment1, segment2);
	if(! result.equals("Booth, S."))
	    ++errCount;


	// compressed segment breaking on a quote unicode sequence
	/*                   B     o     o     t     SQU        */
	byte [] segment3 = { 0x42, 0x6f, 0x6f, 0x74, 0x0e, 0x00 };

	// continuation
	/*                   h     ,           S     .          */
	byte [] segment4 = { 0x68, 0x2c, 0x20, 0x53, 0x2e };

	result = test(segment3, segment4);
	if(! result.equals("Booth, S."))
	    ++errCount;


	// compressed segment breaking on a quote unicode sequence
	/*                   SCU   UQU                         */
	byte [] segment5 = { 0x0f, (byte)0xf0, 0x00 };

	// continuation
	/*                   B                                 */
	byte [] segment6 = { 0x42 };

	result = test(segment5, segment6);
	if(! result.equals("B"))
	    ++errCount;

	if(errCount == 0) {
	    System.out.println("All tests passed successfully...");
	}
	else {
	    System.err.println(errCount + " ERROR(S) OCCURRED");
	}
    }

};
