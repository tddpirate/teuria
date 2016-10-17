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

import android.database.SQLException;

/**
 * @author omer
 *
 */
public class TeuriaSQLException extends SQLException {
	private static final long serialVersionUID = 1L; /* To silence a warning. */
	
	/**
	 * 
	 */
	public TeuriaSQLException() {
		super();
	}

	/**
	 * @param error
	 */
	public TeuriaSQLException(String error) {
		super(error);
	}
}
