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


public class FastLongBinarySearch
{
	private long dataArray[];
	private int auxStart;
	private int power;

	private static final int exp2[] = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072 };

	public FastLongBinarySearch(long data[])
	{
		if (data.length < 1) throw new IllegalArgumentException();
		if (data.length >= exp2[exp2.length-1]) throw new IllegalArgumentException();
		dataArray = data;
		for (power = exp2.length-1; power > 0 && dataArray.length < exp2[power]; power--) {}
		if (exp2[power] != dataArray.length) {
			auxStart = dataArray.length - exp2[power];
		}
		else {
			auxStart = 0;
		}
	}

	public long[] getData()
	{
		return dataArray;
	}

	public int findIndex(int value)
	{
		int index = exp2[power]-1;
		if (value >= dataArray[auxStart]) {
			index += auxStart;
		}

		switch (power) {
		case 17:
			if (value < dataArray[index-65536]) index -= 65536;
		case 16:
			if (value < dataArray[index-32768]) index -= 32768;
		case 15:
			if (value < dataArray[index-16384]) index -= 16384;
		case 14:
			if (value < dataArray[index-8192]) index -= 8192;
		case 13:
			if (value < dataArray[index-4096]) index -= 4096;
		case 12:
			if (value < dataArray[index-2048]) index -= 2048;
		case 11:
			if (value < dataArray[index-1024]) index -= 1024;
		case 10:
			if (value < dataArray[index-512]) index -= 512;
		case 9:
			if (value < dataArray[index-256]) index -= 256;
		case 8:
			if (value < dataArray[index-128]) index -= 128;
		case 7:
			if (value < dataArray[index-64]) index -= 64;
		case 6:
			if (value < dataArray[index-32]) index -= 32;
		case 5:
			if (value < dataArray[index-16]) index -= 16;
		case 4:
			if (value < dataArray[index-8]) index -= 8;
		case 3:
			if (value < dataArray[index-4]) index -= 4;
		case 2:
			if (value < dataArray[index-2]) index -= 2;
		case 1:
			if (value < dataArray[index-1]) index -= 1;
		case 0:
			if (value < dataArray[index]) index -= 1;
		}
		return index;
	}
}