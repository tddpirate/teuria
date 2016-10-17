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

/**
 * Callback for driving a progress bar which allows the user to monitor
 * the progress of parsing XML string.
 * @author omer
 *
 */
public interface XmlParsingProgress {
	/**
	 * Callback for updating a progress bar status.
	 * @param currentposition - current position in string
	 * @param maxposition - string length
	 */
	public void setProgressBar(final long currentposition, final long maxposition);
}