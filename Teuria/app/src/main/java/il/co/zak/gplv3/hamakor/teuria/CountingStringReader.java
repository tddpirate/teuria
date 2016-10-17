/*
    Teuria - Practice for Israeli Driving Theory Test
    Copyright (C) 2012  Omer Zak

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see http://www.gnu.org/licenses/

    The author can be contacted via his Website at http://www.zak.co.il/
*/

package il.co.zak.gplv3.hamakor.teuria;

import java.io.IOException;
import java.io.StringReader;

/**
 * Read from a string and notify the listener each 1% of bytes read.
 * @author omer
 *
 */
public class CountingStringReader extends StringReader {
	XmlParsingProgress mListener;
	long mTotalLength;
	long mCurrentposition; // Current position in the input string
	long mLastposition;    // Last reported position in the input string
	
	CountingStringReader(String str, XmlParsingProgress listener) {
		super(str);
		mListener = listener;
		mTotalLength = str.length();
		mCurrentposition = 0;
		mLastposition = 0;
	}
	
	@Override
	public int read() throws IOException {
		if (null != mListener) {
			mCurrentposition++;
			if (101*(mCurrentposition - mLastposition) > mTotalLength) {
				mListener.setProgressBar(mCurrentposition,  mTotalLength);
				mLastposition = mCurrentposition;
			}
		}
		return super.read();
	}
	
	@Override
	public int read(char[] buf, int offset, int len) throws IOException {
		int retval = super.read(buf, offset, len);
		if (null != mListener) {
			mCurrentposition += retval;
			if (101*(mCurrentposition - mLastposition) > mTotalLength) {
				mListener.setProgressBar(mCurrentposition,  mTotalLength);
				mLastposition = mCurrentposition;
			}
		}
		return retval;
	}
}