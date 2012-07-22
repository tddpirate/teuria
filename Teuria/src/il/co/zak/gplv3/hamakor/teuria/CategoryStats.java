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

import java.util.Locale;

/**
 * Class for holding category statistics information.
 * @author omer
 *
 */
public class CategoryStats {
	static private String mToStringFormat = "%1$s(correct:%2$d, wrong:%3$d)/%4$d";
	static private String mNullCategoryName = "All";
	
	private String mCategory;  // When null, the statistics encompass all categories.
	private int mQuestions; // Total questions in that category
	private int mCorrect;   // Correct answers
	private int mWrong;     // Wrong answers
	
	/**
	 * Set the format to be used by toString() (which will be used by Adapter
	 * for displaying the category statistics). 
	 * 
	 * @param toStringFormat - the format to be used.
	 * The default format is equivalent to:
	 * Format("%1$s(correct:%2$d, wrong:%3$d)/%4$d", mCategory, mCorrect, mWrong, mQuestions)
	 */
	public static void setToStringFormat(String toStringFormat) {
		mToStringFormat = toStringFormat;
	}
	
	/**
	 * Set the string to be displayed for denoting "all categories".
	 * @param nullCategoryName
	 */
	public static void setNullCategoryName(String nullCategoryName)	{
		mNullCategoryName = nullCategoryName;
	}
	
	public CategoryStats() {
		mQuestions = 0;
		mCorrect = 0;
		mWrong = 0;
	}
	
	public CategoryStats setCategory(String category) {
		mCategory = category;
		return this;
	}
	
	public CategoryStats setQuestions(int questions) {
		mQuestions = questions;
		return this;
	}

	public CategoryStats setCorrect(int correct) {
		mCorrect = correct;
		return this;
	}

	public CategoryStats setWrong(int wrong) {
		mWrong = wrong;
		return this;
	}
	
	/**
	 * Accumulate statistics.
	 * The purpose is to get also statistics over all categories, without having
	 * to run a separate query for this.
	 * @param that - Instance for another category, whose values are added to this instance.
	 * @return
	 */
	public CategoryStats accumulate(CategoryStats that) {
		mQuestions += that.mQuestions;
		mCorrect += that.mCorrect;
		mWrong += that.mWrong;
		return this;
	}
	
	/**
	 * Retrieve a nice row of statistics for display.
	 * @return {total, unanswered, correct, wrong}
	 */
	public int[] getRow() {
		int[] answer = new int[] {mQuestions, mQuestions-mCorrect-mWrong, mCorrect, mWrong};
		return answer;
	}
	
	public String getCategory() {
		return mCategory;
	}
	
	@Override
	public String toString() {
		return String.format(Locale.US, mToStringFormat,
						(mCategory == null) ? mNullCategoryName : mCategory,
						mCorrect, mWrong, mQuestions);
	}
}