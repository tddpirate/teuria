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

/**
 * Encapsulates a question item, as retrieved from the questions xml file.
 */
package il.co.zak.gplv3.hamakor.teuria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author omer
 *
 */
public class Question {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.US);
		// Based upon the following example date: "09-11-2011 10:44:20"
	private String mTitle;
	private String mLink;
	private String mGuid;
	private String mDescription;
	private String mAuthor;
	private String mCategory;
	private Date mPubDate;
	
	private int mId;
	
	private boolean mIsClear = false;
	
	public Question() {
		clear();
	}

	/**
	 * @return the mTitle
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * @param mTitle the mTitle to set
	 */
	public Question setTitle(String mTitle) {
		this.mTitle = mTitle;
		mIsClear = false;
		return this;
	}

	/**
	 * @return the mLink
	 */
	public String getLink() {
		return mLink;
	}

	/**
	 * @param mLink the mLink to set
	 */
	public Question setLink(String mLink) {
		this.mLink = mLink;
		mIsClear = false;
		return this;
	}

	/**
	 * @return the mGuid
	 */
	public String getGuid() {
		return mGuid;
	}

	/**
	 * @param mGuid the mGuid to set
	 */
	public Question setGuid(String mGuid) {
		this.mGuid = mGuid;
		mIsClear = false;
		return this;
	}

	/**
	 * @return the mDescription
	 */
	public String getDescription() {
		return mDescription;
	}

	/**
	 * @param mDescription the mDescription to set
	 */
	public Question setDescription(String mDescription) {
		this.mDescription = mDescription;
		mIsClear = false;
		return this;
	}

	/**
	 * @return the mAuthor
	 */
	public String getAuthor() {
		return mAuthor;
	}

	/**
	 * @param mAuthor the mAuthor to set
	 */
	public Question setAuthor(String mAuthor) {
		this.mAuthor = mAuthor;
		mIsClear = false;
		return this;
	}

	/**
	 * @return the mCategory
	 */
	public String getCategory() {
		return mCategory;
	}

	/**
	 * @param mCategory the mCategory to set
	 */
	public Question setCategory(String mCategory) {
		this.mCategory = mCategory;
		mIsClear = false;
		return this;
	}

	/**
	 * @return the mPubDate
	 */
	public Date getPubDate() {
		return mPubDate;
	}

	/**
	 * @param mPubDate the mPubDate to set
	 */
	public Question setPubDate(Date mPubDate) {
		this.mPubDate = mPubDate;
		mIsClear = false;
		return this;
	}

	/**
	 * 
	 * @param dateString the string to be parsed for getting the date.
	 * The format is "09-11-2011 10:44:20"
	 */
	public Question setPubDate(String dateString) throws ParseException {
		this.mPubDate = DATE_FORMAT.parse(dateString);
		mIsClear = false;
		return this;
	}
	
	public int getID() {
		return mId;
	}
	
	public Question setID(int id) {
		mId = id;
		mIsClear = false;
		return this;
	}
	
	/**
	 * Clear the current Question instance.
	 * @return this instance.
	 */
	public Question clear() {
		mTitle = null;
		mLink = null;
		mGuid = null;
		mDescription = null;
		mAuthor = null;
		mCategory = null;
		mPubDate = null;
		mId = -1;  // No ID initially assigned.
		mIsClear = true;
		return this;
	}

	public boolean isClear() {
		return mIsClear;
	}
	
	@Override
	public String toString() {
		return mIsClear
				? "Cleared"
				: ("Question " + mId + ":"
				+ ((mTitle != null) && (mTitle.length() > 0) ? " Title:" + mTitle : "")
				+ ((mLink != null) && (mLink.length() > 0) ? " Link:" + mLink : "")
				+ ((mGuid != null) && (mGuid.length() > 0) ? " Guid:" + mGuid : "")
				//+ (mDescription.length() > 0 ? " Description:" + mDescription : "")
				+ ((mAuthor != null) && (mAuthor.length() > 0) ? " Author:" + mAuthor : "")
				+ ((mCategory != null) && (mCategory.length() > 0) ? " Category:" + mCategory : "")
				+ " Pubdate:" + mPubDate);
	}
}
