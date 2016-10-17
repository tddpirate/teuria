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
 * The Teuria Database
 * It contains two tables - one for the questions and one for the users' answers.
 */
package il.co.zak.gplv3.hamakor.teuria;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

/**
 * @author omer
 *
 */
public class TeuriaDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "teuria.db";
	private static final int DATABASE_VERSION = 2;
	// Changes from version 1 to version 2:
	// - Removed ANSWER_USERNAME column from ANSWERS_TABLE
	// - Added table LICLEVELS_TABLE

	/* Questions Table */
	
	private static final String QUESTIONS_TABLE = "QUESTIONS";
	
	private static final String QUESTION_ID = "_id";
	private static final String QUESTION_TITLE = "TITLE";
	private static final String QUESTION_DESCRIPTION = "DESCRIPTION";
	private static final String QUESTION_CATEGORY = "CATEGORY";
	
	private static final String SQL_CREATE_QUESTIONS_TABLE = "CREATE TABLE " + QUESTIONS_TABLE + " ("
			+ QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ QUESTION_TITLE + " TEXT,"
			+ QUESTION_DESCRIPTION + " TEXT,"
			+ QUESTION_CATEGORY + " TEXT"
			+ ");";
	
	/* Answers Table */

	private static final String ANSWERS_TABLE = "ANSWERS";

	private static final String ANSWER_RECORD_ID = "_id";
	private static final String ANSWER_QUESTION_ID = "qid";
	private static final String ANSWER_STATUS = "STATUS";
	/*
	 * The possible values of ANSWER_STATUS are:
	 * 0 - the question was not presented to the user
	 * 1 - the most recent answer to the question was wrong
	 * 2 - the most recent answer to the question was correct
	 */

	private static final String SQL_CREATE_ANSWERS_TABLE = "CREATE TABLE " + ANSWERS_TABLE + " ("
			+ ANSWER_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ ANSWER_QUESTION_ID + " INTEGER,"
			+ ANSWER_STATUS + " INTEGER"
			+ ");";

	/* License levels Table */

	private static final String LICLEVELS_TABLE = "LICLEVELS";
	
	private static final String LICLEVELS_RECORD_ID = "_id";
	private static final String LICLEVELS_QUESTION_ID = "qid";
	private static final String LICLEVELS_LICLEVEL = "liclevel";
	
	private static final String SQL_CREATE_LICLEVELS_TABLE = "CREATE TABLE " + LICLEVELS_TABLE + " ("
			+ LICLEVELS_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ LICLEVELS_QUESTION_ID + " INTEGER,"
			+ LICLEVELS_LICLEVEL + " TEXT"
			+ ");";

	/* Other constants and variables */
	
	private Context mContext;
	private String mLicLevel = null;
	private static Random mRandom;

	/**
	 * Variables used for optimizing bulk insertion of questions into the DB.
	 */
	private static SQLiteDatabase mDB = null;  // For use when multiple operations need to be performed efficiently.
	// When mDB != null, single-operation functions will throw an exception.
	private static InsertHelper mQuestionsInsertHelper = null;
	private static int mIHquestionTitleColumn;
	private static int mIHquestionDescriptionColumn;
	private static int mIHquestionCategoryColumn;
	private static InsertHelper mAnswersInsertHelper = null;
	private static int mIHanswerQidColumn;
	private static int mIHanswerStatusColumn;
	private static InsertHelper mLiclevelsInsertHelper = null;
	private static int mLiclevelsQidColumn;
	private static int mLiclevelsLiclevelColumn;
	
	private static final String[] TABLES = { QUESTIONS_TABLE, ANSWERS_TABLE, LICLEVELS_TABLE };

	private static final String TAG = "TeuriaDatabase";

	/* More complicated constants */
	
	private static final String SQL_QUERY_CATEGORIES;
	private static final String SQL_QUERY_LICLEVELS;
	private static final String SQL_QUERY_CATEGORY_STATS = "SELECT " + QUESTIONS_TABLE + "." + QUESTION_CATEGORY
			+ ",sum(1) as Total"
			+ ",sum(case when " + ANSWERS_TABLE + "." + ANSWER_STATUS + "=1 then 1 else 0 end) as Wrong"
			+ ",sum(case when " + ANSWERS_TABLE + "." + ANSWER_STATUS + "=2 then 1 else 0 end) as Right"
			+ " from " + QUESTIONS_TABLE + "," + ANSWERS_TABLE + "," + LICLEVELS_TABLE
			+ " WHERE " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + ANSWERS_TABLE + "." + ANSWER_QUESTION_ID
			+ " AND " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + LICLEVELS_TABLE + "." + LICLEVELS_QUESTION_ID
					+ " AND " + LICLEVELS_TABLE + "." + LICLEVELS_LICLEVEL + "=?"
			+ " GROUP BY " + QUESTIONS_TABLE + "." + QUESTION_CATEGORY;

	/* Used to support random question selection for a given user from all categories. */
	private static final String SQL_QUERY_STATS_FOR_USER = "SELECT "
			+ "sum(case when " + ANSWERS_TABLE + "." + ANSWER_STATUS + "=0 then 1 else 0 end) as Unanswered"
			+ ",sum(case when " + ANSWERS_TABLE + "." + ANSWER_STATUS + "=1 then 1 else 0 end) as Wrong"
			+ ",sum(case when " + ANSWERS_TABLE + "." + ANSWER_STATUS + "=2 then 1 else 0 end) as Right"
			+ " from " + QUESTIONS_TABLE + "," + ANSWERS_TABLE + "," + LICLEVELS_TABLE
			+ " WHERE " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + ANSWERS_TABLE + "." + ANSWER_QUESTION_ID
			+ " AND " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + LICLEVELS_TABLE + "." + LICLEVELS_QUESTION_ID
					+ " AND " + LICLEVELS_TABLE + "." + LICLEVELS_LICLEVEL + "=?"
			;
	/* Used to support random question selection for a given user from one category. */
	/* Note that for this query, the license level argument comes before the category argument. */
	private static final String SQL_QUERY_STATS_FOR_USER_CATEGORY =  SQL_QUERY_STATS_FOR_USER
			+ " AND " + QUESTIONS_TABLE + "." + QUESTION_CATEGORY + "=?"
			;
	
	/* Select a question, given a random offset */
	/* All categories, parameters: status,liclevel,offset */
	private static final String SQL_QUERY_SELECT_QUESTION = " SELECT " + QUESTIONS_TABLE + ".*"
			+ " from " + QUESTIONS_TABLE + "," + ANSWERS_TABLE + "," + LICLEVELS_TABLE
			+ " WHERE " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + ANSWERS_TABLE + "." + ANSWER_QUESTION_ID
			+ " AND " + ANSWERS_TABLE + "." + ANSWER_STATUS + "=?"
			+ " AND " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + LICLEVELS_TABLE + "." + LICLEVELS_QUESTION_ID
					+ " AND " + LICLEVELS_TABLE + "." + LICLEVELS_LICLEVEL + "=?"
			+ " order by " + QUESTIONS_TABLE + "." + QUESTION_ID
			+ " limit 1 offset ?";
	/* Parameters: category,status,liclevel,offset */
	private static final String SQL_QUERY_SELECT_QUESTION_FOR_CATEGORY = " SELECT " + QUESTIONS_TABLE + ".*"
			+ " from " + QUESTIONS_TABLE + "," + ANSWERS_TABLE + "," + LICLEVELS_TABLE
			+ " WHERE " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + ANSWERS_TABLE + "." + ANSWER_QUESTION_ID
			+ " AND " + QUESTIONS_TABLE + "." + QUESTION_CATEGORY + "=?"
			+ " AND " + ANSWERS_TABLE + "." + ANSWER_STATUS + "=?"
			+ " AND " + QUESTIONS_TABLE + "." + QUESTION_ID + "=" + LICLEVELS_TABLE + "." + LICLEVELS_QUESTION_ID
					+ " AND " + LICLEVELS_TABLE + "." + LICLEVELS_LICLEVEL + "=?"
			+ " order by " + QUESTIONS_TABLE + "." + QUESTION_ID
			+ " limit 1 offset ?";
	
	static {
		SQL_QUERY_CATEGORIES = SQLiteQueryBuilder.buildQueryString(true, QUESTIONS_TABLE, new String[] {QUESTION_CATEGORY}, null, null, null, QUESTION_CATEGORY, null);
			// TODO Should qualify categories also by license levels.
		SQL_QUERY_LICLEVELS = SQLiteQueryBuilder.buildQueryString(true, LICLEVELS_TABLE, new String[] {LICLEVELS_LICLEVEL}, null, null, null, LICLEVELS_LICLEVEL, null);
		mRandom = new Random();
	}
	
	/* Standard methods */
	
	public TeuriaDatabase(Context context, String newLicLevel) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
		if (newLicLevel != null) {
			// If newLicLevel==null then the user didn't choose a license level yet.
			setQuestionsFilteringLiclevel(newLicLevel);
		}
	}
	
	public void deleteDatabase() {
		mContext.deleteDatabase(DATABASE_NAME);
	}
	
	/**
	 * Actually create the database tables.
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG,"Creating the database");
		db.execSQL(SQL_CREATE_QUESTIONS_TABLE);
		db.execSQL(SQL_CREATE_ANSWERS_TABLE);
		db.execSQL(SQL_CREATE_LICLEVELS_TABLE);
	}

	public void setQuestionsFilteringLiclevel(String newLicLevel) {
		mLicLevel = newLicLevel;
	}
	
	/**
	 * Clear the database in preparation for re-populating the tables.
	 * @param db
	 */
	public void clearDb(SQLiteDatabase db) {
		// Drop all existing tables, losing all existing data.
		for (String tabname: TABLES) {
			db.execSQL("DROP TABLE IF EXISTS " + tabname);
		}
		// Recreate the database
		onCreate(db);
	}

	public void clearDb() {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			this.clearDb(db);
		}
		finally {
			try { db.close(); } catch (Throwable e) {}
		}
	}
	
	/**
	 * Primitive upgrade - by dropping all tables and recreating them.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Log that the database is being upgraded
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		// Recreate the database with a new version
		clearDb(db);
	}
	
	/* Custom table access functions */

	/**
	 * Retrieve all categories in the DB
	 * @param db
	 * @return
	 */
	public String[] getAllCategories() {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = this.getReadableDatabase();
			cursor = db.rawQuery(SQL_QUERY_CATEGORIES, null);
			List<String> categories = new LinkedList<String>();
			
			if (cursor.moveToFirst()) {
				do {
					categories.add(cursor.getString(0));
				} while (cursor.moveToNext());
			}
			return categories.toArray(new String[categories.size()]);
		}
		finally {
			try { cursor.close(); } catch (Throwable e) {}
			try { db.close(); } catch (Throwable e) {}
		}
	}

	/**
	 * Retrieve all license levels known to the questions database.
	 * @return array of license levels, as strings.
	 */
	public String[] getAllLiclevels() {
	if (mDB != null) {
		throw new TeuriaSQLException("The DB is in fast questions insertion mode");
	}
	SQLiteDatabase db = null;
	Cursor cursor = null;
	try {
		db = this.getReadableDatabase();
		cursor = db.rawQuery(SQL_QUERY_LICLEVELS, null);
		List<String> liclevels = new LinkedList<String>();
		
		if (cursor.moveToFirst()) {
			do {
				liclevels.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}
		return liclevels.toArray(new String[liclevels.size()]);
	}
	finally {
		try { cursor.close(); } catch (Throwable e) {}
		try { db.close(); } catch (Throwable e) {}
	}
}

	/**
	 * Add another question to the QUESTIONS_TABLE
	 * @param question
	 * @return the _id of the newly-added question
	 */
	/*
	public int addQuestionSlow(Question question) {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(QUESTION_TITLE, question.getTitle());
			values.put(QUESTION_DESCRIPTION, question.getDescription());
			values.put(QUESTION_CATEGORY, question.getCategory());
			return (int)db.insert(QUESTIONS_TABLE, null, values);
		}
		finally {
			try { db.close(); } catch (Throwable e) {}
		}
	}
	*/

	/**
	 * Initialize an answer record for the current user.
	 * @param questionID _id of the corresponding question
	 * @return - _id of the newly-inserted answer record
	 */
	/*
	public int initAnswerRecordSlow(int questionID) {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(ANSWER_QUESTION_ID, questionID);
			values.put(ANSWER_STATUS, 0);  // Question was not presented to the user
			return (int)db.insert(ANSWERS_TABLE, null, values);
		}
		finally {
			try { db.close(); } catch (Throwable e) {}
		}
	}
	*/
	
	/*
	 * Fast DB population code - to be used when you need to insert
	 * several questions one after the other.
	 */
	
	/**
	 * Prepare the DB for fast questions addition.
	 * @return pointer to the DB after preparation.
	 */
	public void prepareForAddQuestions() {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is already in fast questions insertion mode");
		}
		mDB = this.getWritableDatabase();
		//mDB.execSQL("PRAGMA synchronous = OFF");  // Did not improve performance.

		mQuestionsInsertHelper = new InsertHelper(mDB, QUESTIONS_TABLE);
		mIHquestionTitleColumn = mQuestionsInsertHelper.getColumnIndex(QUESTION_TITLE);
		mIHquestionDescriptionColumn = mQuestionsInsertHelper.getColumnIndex(QUESTION_DESCRIPTION);
		mIHquestionCategoryColumn = mQuestionsInsertHelper.getColumnIndex(QUESTION_CATEGORY);

		mAnswersInsertHelper = new InsertHelper(mDB, ANSWERS_TABLE);
		mIHanswerQidColumn = mAnswersInsertHelper.getColumnIndex(ANSWER_QUESTION_ID);
		mIHanswerStatusColumn = mAnswersInsertHelper.getColumnIndex(ANSWER_STATUS);

		mLiclevelsInsertHelper = new InsertHelper(mDB, LICLEVELS_TABLE);
		mLiclevelsQidColumn = mLiclevelsInsertHelper.getColumnIndex(LICLEVELS_QUESTION_ID);
		mLiclevelsLiclevelColumn = mLiclevelsInsertHelper.getColumnIndex(LICLEVELS_LICLEVEL);

		mDB.beginTransaction();
	}
	
	/**
	 * Add another question to the DB
	 * @param question - the question to be added
	 * @return the _id of the newly-added question
	 */
	public int addQuestionFast(Question question) {
		if (mDB == null) {
			throw new TeuriaSQLException("The DB is not in fast questions insertion mode");
		}
		mQuestionsInsertHelper.prepareForInsert();
        mQuestionsInsertHelper.bind(mIHquestionTitleColumn, question.getTitle());
        mQuestionsInsertHelper.bind(mIHquestionDescriptionColumn, question.getDescription());
        mQuestionsInsertHelper.bind(mIHquestionCategoryColumn, question.getCategory());
        
		return (int)mQuestionsInsertHelper.execute();
	}

	/**
	 * Initialize an answer record for the current user.
	 * @param questionID _id of the corresponding question
	 * @return - _id of the newly-inserted answer record
	 */
	public int initAnswerRecordFast(int questionID) {
		if (mDB == null) {
			throw new TeuriaSQLException("The DB is not in fast questions insertion mode");
		}
		mAnswersInsertHelper.prepareForInsert();
		mAnswersInsertHelper.bind(mIHanswerQidColumn, questionID);
		mAnswersInsertHelper.bind(mIHanswerStatusColumn, 0);   // Question was not presented to the user
		return (int)mAnswersInsertHelper.execute();
	}

	/**
	 * Associate another driving license level with a question
	 * @param qid - the question for which we want to associate another license level
	 * @param liclevel - the license level string
	 * @return - the _id of the newly-added association record
	 */
	public int addLicenseLevelRecordFast(int qid, String liclevel) {
		if (mDB == null) {
			throw new TeuriaSQLException("The DB is not in fast questions insertion mode");
		}
		mLiclevelsInsertHelper.prepareForInsert();
		mLiclevelsInsertHelper.bind(mLiclevelsQidColumn, qid);
		mLiclevelsInsertHelper.bind(mLiclevelsLiclevelColumn, liclevel);

		return (int)mLiclevelsInsertHelper.execute();
	}

	/**
	 * Restore the DB to normal mode.
	 */
	public void finishAddQuestions() {
		if (mDB == null) {
			throw new TeuriaSQLException("The DB is not in fast questions insertion mode");
		}
		mDB.setTransactionSuccessful();
		mDB.endTransaction();
		mLiclevelsInsertHelper.close();
		mLiclevelsInsertHelper = null;
		mAnswersInsertHelper.close();
		mAnswersInsertHelper = null;
		mQuestionsInsertHelper.close();
		mQuestionsInsertHelper = null;
		//mDB.execSQL("PRAGMA synchronous = FULL");  // Did not improve performance.
		mDB.close();
		mDB = null;
	}

	/*
	 * End of code for fast DB population code
	 */


	/**
	 * Update an answer record by setting the status field according to user's answer
	 * to the given question.
	 * 
	 * @param questionID - _id of the question being answered
	 * @param status - value depends upon the user's answer, see description of the STATUS field.
	 */
	public void modifyAnswer(int questionID, int status) {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(ANSWER_STATUS, status);
			if (1 != db.update(ANSWERS_TABLE, values, ANSWER_QUESTION_ID + " = ?", new String[]{String.valueOf(questionID)})) {
				throw new TeuriaSQLException("Did not update exactly one record in ANSWERS table");
			}
		}
		finally {
			try { db.close(); } catch (Throwable e) {}
		}
	}
	
	/**
	 * Get statistics by category, for all categories
	 * @param username - the user for whom we want the information
	 * @return
	 */
	public CategoryStats[] getCategoryStatistics() {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			db = this.getReadableDatabase();
			cursor = db.rawQuery(SQL_QUERY_CATEGORY_STATS, new String[]{mLicLevel});
			List<CategoryStats> categoryStats = new LinkedList<CategoryStats>();
			CategoryStats all = new CategoryStats();
			all.setCategory(null);
			
			if (cursor.moveToFirst()) {
				do {
					CategoryStats obj = new CategoryStats();
					obj.setCategory(cursor.getString(0)).setQuestions(cursor.getInt(1)).setWrong(cursor.getInt(2)).setCorrect(cursor.getInt(3));
					categoryStats.add(obj);
					all.accumulate(obj);
				} while (cursor.moveToNext());
			}
			categoryStats.add(all);
			return categoryStats.toArray(new CategoryStats[categoryStats.size()]);
		}
		finally {
			try { cursor.close(); } catch (Throwable e) {}
			try { db.close(); } catch (Throwable e) {}
		}
	}

	/**
	 * Get statistics for one user and one category, for use in selecting a random question.
	 * @param username - user for whom we want to select a question
	 * @param category - category from which to select a question (null means all categories)
	 * @return
	 */
	public int[] getUserCategoryStatistics(String category) {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		Cursor cursor = null;
		
		String sqlStatement = (category == null) ? SQL_QUERY_STATS_FOR_USER : SQL_QUERY_STATS_FOR_USER_CATEGORY;
		String[] sqlArgs = (category == null) ? new String[]{mLicLevel} : new String[]{mLicLevel,category};
		
		try {
			db = this.getReadableDatabase();
			cursor = db.rawQuery(sqlStatement, sqlArgs);

			if (cursor.getCount() != 1) {
				throw new TeuriaSQLException("Expected exactly one row, got " + cursor.getCount() + " rows");
			}
			if (!cursor.moveToFirst()) {
				throw new TeuriaSQLException("Row has unexpectedly disappeared");
			}

			return new int[] { cursor.getInt(0), cursor.getInt(1), cursor.getInt(2) };
		}
		finally {
			try { cursor.close(); } catch (Throwable e) {}
			try { db.close(); } catch (Throwable e) {}
		}
	}
	
	/**
	 * Clear all statistics for an user (reset all question answers to STATUS=0 i.e. unanswered).
	 * @param username
	 * @return number of questions for which statistics were cleared.
	 */
	public int clearStatistics() {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(ANSWER_STATUS, 0);
			return db.update(ANSWERS_TABLE, values, null, null);
		}
		finally {
			try { db.close(); } catch (Throwable e) {}
		}
	}

	/**
	 * Given query parameters, select the question to be retrieved.
	 * @param category - used to filter the eligible questions
	 * @param status - used to filter the eligible questions
	 * @param offset - selected randomly by the caller.
	 * @return - Question object containing the question data.
	 */
	public Question getQuestion(String category, int status, int offset) {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		SQLiteDatabase db = null;
		Cursor cursor = null;
		
		String sqlStatement = (category == null) ? SQL_QUERY_SELECT_QUESTION : SQL_QUERY_SELECT_QUESTION_FOR_CATEGORY;
		String[] sqlArgs = (category == null)
				? new String[]{String.valueOf(status),mLicLevel,String.valueOf(offset)}
				: new String[]{category,String.valueOf(status),mLicLevel,String.valueOf(offset)};
		
		try {
			db = this.getReadableDatabase();
			cursor = db.rawQuery(sqlStatement, sqlArgs);
			if (cursor.getCount() != 1) {
				throw new TeuriaSQLException("Expected exactly one row, got " + cursor.getCount() + " rows");
			}
			
			if (!cursor.moveToFirst()) {
				throw new TeuriaSQLException("Row has unexpectedly disappeared");
			}
			
			Question question = new Question();
			if (category == null) {
				category = cursor.getString(3);
			}
			return question.setID(cursor.getInt(0)).setTitle(cursor.getString(1)).setDescription(cursor.getString(2)).setCategory(category);
		}
		finally {
			try { cursor.close(); } catch (Throwable e) {}
			try { db.close(); } catch (Throwable e) {}
		}
	}
	
	private static final int[] WEIGHTS = new int[] {100, 200, 1};
	public Question getRandomQuestion(String category) {
		if (mDB != null) {
			throw new TeuriaSQLException("The DB is in fast questions insertion mode");
		}
		/*
		 * If a question not presented so far has probability 1/X of being presented to the user,
		 * then we want a question already wrongly answered to have probability of 2/X,
		 * and a question correctly answered to have probability of 0.01/X.
		 */
		int[] stats = getUserCategoryStatistics(category);
		Log.d(TAG, "getRandomQuestion(" + category + ") - stats:" + stats);
		
		int sum = 0;
		for (int ind = 0; ind < WEIGHTS.length; ++ind) {
			sum += WEIGHTS[ind] * stats[ind];
			Log.d(TAG, "sum[" + ind + "]: " + sum);
		}
		
		/* Now choose a random integer from range [0,sum-1]. */
		int random = mRandom.nextInt(sum);
		Log.d(TAG, "random number chosen from range [0," + sum + "): " + random);
		for (int ind = 0; ind < WEIGHTS.length; ++ind) {
			if (random >= WEIGHTS[ind] * stats[ind]) {
				random -= WEIGHTS[ind] * stats[ind];
			}
			else {
				int qind = random/WEIGHTS[ind];
				Log.d(TAG, "will retrieve question " + qind + ", status=" + ind + " and category " + category);
				// Retrieve question by category, status=ind, offset=qind.
				return getQuestion(category, ind, qind);
			}
		}
		throw new TeuriaSQLException("Something is wrong - random=" + random + ", seems to be >= sum=" + sum);
	}
}
