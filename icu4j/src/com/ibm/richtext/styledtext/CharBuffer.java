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
/** An implementation of MCharBuffer that stores chars in an array with an insertion gap. */
/*
    Change history
    072396 jf   - fixed a bug in replace(int, int, char[], int, int) so that it correctly
                inserted into the middle of the buffer.
    080296 jf   - added timestamp.  This is strictly a debugging device to help catch
                stale iterators.

    082296 jbr  added check for 0-length iterator in replace
*/

package com.ibm.richtext.styledtext;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

import java.text.CharacterIterator;

final class CharBuffer
    extends MCharBuffer implements Externalizable
{

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
  private static final int kGrowSize = 0x80; // small size for testing
  private static final int CURRENT_VERSION = 1; // version code for streaming
  private static final long serialVersionUID = 563174;

  transient Validation fValidation = null;
  private char[] fArray;
  transient private int fArraySize;
  transient private int fGap;

  /** Create an empty char buffer. */
  public CharBuffer()
  {
  }

  /** Create a char buffer that can hold at least capacity chars. */

  public CharBuffer(int capacity)
  {
    fArray = allocate(capacity);
  }

  public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {

    if (in.readInt() != CURRENT_VERSION) {
        throw new IOException("Invalid version of CharBuffer");
    }

    fArray = (char[]) in.readObject();
    if (fArray != null) {
        fArraySize = fArray.length;
        fGap = fArraySize;
    }
    else {
        fArraySize = 0;
        fGap = 0;
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {

    compress();
    out.writeInt(CURRENT_VERSION);
    out.writeObject(fArray);
  }

  private void invalidate() {

    if (fValidation != null) {
        fValidation.invalidate();
        fValidation = null;
    }
  }

  // not ThreadSafe - could end up with two Validations
  // being generated
  private Validation getValidation() {

    if (fValidation == null) {
        fValidation = new Validation();
    }
    return fValidation;
  }

  /** Replace the chars from start to limit with the chars from srcStart to srcLimit in srcBuffer. */

  /** Replace the chars from start to limit with the chars from srcStart to srcLimit in srcChars.
  * This is the core routine for manipulating the buffer.
  */
  public void replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit)
  {
    invalidate();
    int dstLength = limit - start;
    int srcLength = srcLimit - srcStart;

    if (dstLength < 0 || srcLength < 0) {
        throw new IllegalArgumentException("replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit)");
    }

    int gapAlloc = 0;
    if (srcChars == null) {
        gapAlloc = srcLength;
        srcLength = 0;
    }

    int newSize = fArraySize - dstLength + srcLength;

    if (fArray == null) {
        if (start != 0 || limit != 0) {
            throw new IllegalArgumentException("replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit)");
        }
        if (newSize + gapAlloc > 0) {
            fArray = allocate(newSize + gapAlloc);
            if (srcLength > 0) {
                System.arraycopy(srcChars, srcStart, fArray, 0, srcLength);
                fArraySize = srcLength;
                fGap = srcLength;
            }
        }
    } else {
        int newGap = start + srcLength;
        int gapLimit = fArray.length - fArraySize + fGap;

        if (newSize + gapAlloc > fArray.length) {
            char[] temp = allocate(newSize + gapAlloc);

            //move stuff at beginning that we aren't writing over
            if (start > 0) {
                at(0, start, temp, 0);
            }
            //move stuff from src array that we are copying
            if (srcLength > 0) {
                System.arraycopy(srcChars, srcStart, temp, start, srcLength);
            }
            //move stuff at end that we aren't copying over
            if (limit < fArraySize) {
                at(limit, fArraySize, temp, temp.length - newSize + newGap);
            //change 7-23-96
            //    at(limit, fArraySize - limit, temp, temp.length - newSize + newGap);
            }

            fArray = temp;
        } else {
            if (start > fGap) {
                System.arraycopy(fArray, gapLimit, fArray, fGap, start - fGap);
            }
            if (limit < fGap) {
                System.arraycopy(fArray, limit, fArray, fArray.length - newSize + newGap, fGap - limit);
            }
            if (srcLength > 0) {
                System.arraycopy(srcChars, srcStart, fArray, start, srcLength);
            }
        }

        fArraySize = newSize;
        fGap = newGap;
    }
  }

  /** Replace the chars from start to limit with the chars from srcStart to srcLimit in srcString. */

  /* This implements optimizations for null text or inserting text that fits at the gap,
     and defaults to call the core replace routine if these optimizations fail. */

  public void replace(int start, int limit, String srcString, int srcStart, int srcLimit)
  {
    invalidate();
    int length = limit - start;
    int srcLength = srcLimit - srcStart;

    if (fArray == null) {
        if (start != 0 || limit != 0) {
            throw new IllegalArgumentException("replace(int start, int limit, String srcString, int srcStart, int srcLimit)");
        }
        if (srcLength > 0) {
            fArray = allocate(srcLength);
            srcString.getChars(srcStart, srcLimit, fArray, 0);
            fArraySize = srcLength;
            fGap = srcLength;
        }
    } else {
        if (start == fGap && fArray.length >= fArraySize - length + srcLength) {
            if (srcLimit > 0) {
                srcString.getChars(srcStart, srcLimit, fArray, fGap);
                fGap += srcLength;
            }
            fArraySize += srcLength - length;
        } else {
            replace(start, limit, srcString != null ? srcString.toCharArray() : null, srcStart, srcLimit);
        }
    }
  }

  public void replace(int start, int limit, MConstText srcText, int srcStart, int srcLimit)
  {
    invalidate();
    int length = limit - start;
    int srcLength = srcLimit - srcStart;

    if (fArray == null) {
        if (start != 0 || limit != 0) {
            throw new IllegalArgumentException("replace(int start, int limit, String srcString, int srcStart, int srcLimit)");
        }
        if (srcLength > 0) {
            fArray = allocate(srcLength);
            srcText.extractChars(srcStart, srcLimit, fArray, 0);
            fArraySize = srcLength;
            fGap = srcLength;
        }
    } else {
        if (start == fGap && fArray.length >= fArraySize - length + srcLength) {
            if (srcLimit > 0) {
                srcText.extractChars(srcStart, srcLimit, fArray, fGap);
                fGap += srcLength;
            }
            fArraySize += srcLength - length;
        } else {
            char[] temp = srcLength == 0? null : new char[srcLength];
            if (temp != null) {
                srcText.extractChars(srcStart, srcLimit, temp, 0);
            }
            replace(start, limit, temp, 0, srcLimit - srcStart);
        }
    }
  }

  /** Replace the chars from start to limit with srcChar. */

  /* This implements optimizations for null text or replacing a character that fits into the gap,
     and defaults to call the core replace routine if these optimizations fail. */

  public void replace(int start, int limit, char srcChar)
  {
    invalidate();
    if (fArray == null) {
        if (start != 0 || limit != 0) {
            throw new IllegalArgumentException("replace(int start, int limit, char srcChar)");
        }
        fArray = allocate(1);
        fArray[0] = srcChar;
        fArraySize = 1;
        fGap = 1;
    } else {
        int length = limit - start;
        if (start == fGap && fArray.length > fArraySize - length) {
            fArray[fGap] = srcChar;
            fGap += 1;
            fArraySize += 1 - length;
        } else {
            replace(start, limit, new char[] { srcChar} , 0, 1);
        }
    }
  }

  /** Return the char at pos. */

  public char at(int pos)
  {
    if (pos < 0 || pos >= fArraySize) {
      throw new IllegalArgumentException();
    }
    return pos < fGap ? fArray[pos] : fArray[fArray.length - fArraySize + pos];
  }

  /** Copy the chars from start to limit to dst starting at dstStart. */

  public void at(int start, int limit, char[] dst, int dstStart)
  {
    int length = limit - start;

    if (start < 0 || limit < start || limit > fArraySize) {
        throw new IllegalArgumentException();
    }

    if (limit <= fGap) {
        System.arraycopy(fArray, start, dst, dstStart, length);
    } else if (start >= fGap) {
        System.arraycopy(fArray, fArray.length - fArraySize + start, dst, dstStart, length);
    } else {
        System.arraycopy(fArray, start, dst, dstStart, fGap - start);
        System.arraycopy(fArray, fArray.length - fArraySize + fGap, dst, dstStart + fGap - start, limit - fGap);
    }
  }

  /** Return the number of chars in the buffer. */

  public final int length()
  {
    return fArraySize;
  }

  /** Return the number of chars the buffer can hold before it must reallocate. */

  public final int capacity()
  {
    return fArray != null ? fArray.length : 0;
  }

  /** Reserve capacity chars at start. Utility to optimize a sequence of operations at start. */

  public void reserveCapacity(int start, int capacity)
  {
    replace(start, start, (char[])null, 0, capacity);
  }

  /** Minimize the storage used by the buffer. */

  public void compress()
  {
    invalidate();
    if (fArraySize == 0) {
        fArray = null;
        fGap = 0;
    } else if (fArraySize != fArray.length) {
        char[] temp = new char[fArraySize];
        at(0, fArraySize, temp, 0);
        fArray = temp;
        fGap = fArraySize;
    }
  }

  /** Display the buffer. */

  public String toString()
  {
    if (fArray != null) {
        return new StringBuffer()
        .append("limit: ").append(fArray.length)
        .append(", size: ").append(fArraySize)
        .append(", gap: ").append(fGap)
        .append(", ").append(fArray, 0, fGap)
        .append(fArray, fArray.length - fArraySize + fGap, fArraySize - fGap)
        .toString();
    } else {
        return new String("The buffer is empty.");
    }
  }

  public CharacterIterator createCharacterIterator(int start, int limit) {

    Validation val = getValidation();
    return new CharBufferIterator(start, limit, fArray, fArraySize, fGap, val);
  }

  /** The resizing algorithm. Return a value >= minSize. */

  protected int allocation(int minSize)
  {
    //    return (minSize + kGrowSize) & ~(kGrowSize - 1);
    return minSize < kGrowSize ? kGrowSize : (minSize * 2 + kGrowSize) & ~(kGrowSize - 1);
  }

  /** Allocate a new character array of limit >= minSize. */

  protected char[] allocate(int minSize)
  {
    return new char[allocation(minSize)];
  }
}
