/*
(C) Copyright Taligent, Inc. 1996 - All Rights Reserved
(C) Copyright IBM Corp. 1996 - All Rights Reserved

  The original version of this source code and documentation is copyrighted and
owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These materials are
provided under terms of a License Agreement between Taligent and Sun. This
technology is protected by multiple US and International patents. This notice and
attribution to Taligent may not be removed.
  Taligent is a registered trademark of Taligent, Inc.
*/

package com.ibm.text.utility;

public class FastBinarySearchTest
{
	static boolean printResult(FastIntBinarySearch search, int value)
	{
		int ndx = search.findIndex(value);
		int data[] = search.getData();
		String errorString = null;
		if (ndx < 0) {
			if (!(ndx < data[0])) {
				errorString = "" + value +" < "+data[0];
			}
		}
		else if (ndx < data.length -1) {
			if (!(data[ndx] <= value && value < data[ndx+1])) {
				errorString =  "" + data[ndx]+"<="+value+"<"+data[ndx+1];
			}
		}
		else {
			if (!(data[ndx] <= value)) {
				errorString = ""+data[ndx]+"<"+value;
			}
		}
		if (errorString != null) {
			System.out.println("ERROR: findIndex("+value+") => "+ndx+"  "+errorString);
			return false;
		}
		else {
			return true;
		}
	}

	static void test(int testArray[])
	{
		boolean passed = true;
		FastIntBinarySearch search = new FastIntBinarySearch(testArray);
		for (int i = -1; passed && i < testArray[testArray.length-1]+2; i++) {
			passed = passed && printResult(search, i);
		}
		if (passed) System.out.println("test passed");
		else System.out.println("test failed");
	}

	//							      0, 1, 2, 3, 4, 5,  6,  7,  8,  9,  10, 11, 12, 13, 14, 15, 16, 17
	public static int testArray1[] = {1};
	public static int testArray2[] = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 19, 19, 25, 27, 29, 31};
	public static int testArray3[] = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 19, 19, 25, 27, 29};
	public static int testArray4[] = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 19, 19, 25, 27, 29, 31, 33};

	public static void main(String args[])
	{
		System.out.println("running 4 tests...");
		test(testArray1);
		test(testArray2);
		test(testArray3);
		test(testArray4);
	}
}