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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	TeuriaDatabase mDB;
	public static final int SHOW_QUESTION_REQUEST_CODE = 42;
	public static final int QUESTIONS_LOADER_REQUEST_CODE = 43;
	public static final int INITIAL_ACTIVATION_QUESTIONS_LOADER_REQUEST_CODE = 44;
	ArrayAdapter<CategoryStats> mStatsAdapter;
	
	private static final String TAG = "TeuriaActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mDB = new TeuriaDatabase(this);
        
        // GUI work
        setContentView(R.layout.activity_main);

        CategoryStats.setToStringFormat(getString(R.string.stats_format));
        CategoryStats.setNullCategoryName(getString(R.string.null_category_name));
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
    	//CategoryStats[] stats = mDB.getCategoryStatistics(preferences.getString("CurrentUser", "Me"));
		mStatsAdapter = new ArrayAdapter<CategoryStats>(this, R.layout.display_stat_as_button, R.id.button1) {
			@Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            Log.d(TAG,"creating view at position " + position);
	            View view = super.getView(position, convertView, parent);
	            view.setTag(getItem(position));
	            return view;
			}
		};
		ListView listView = (ListView) findViewById(R.id.listCategoryStats);
		listView.setAdapter(mStatsAdapter);
		if (1 == displayStats()) {
			/*
			 * First activation procedure.  If there are no per-category stats records,
			 * it means that the DB is empty.
			 */
			performFirstActivation();
		}
    }

	/*
	 * First activation procedure.  If there are no per-category stats records,
	 * it means that the DB is empty.
	 * 
	 * First activation consists of three dialogs:
	 * 1. Select language
	 * 2. DB loading - a progress bar dialog
	 * (There used to be also step 3. Select category, but it was made redundant.)
	 */
	private void performFirstActivation() {
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.select_a_language)
				.setCancelable(true)
				.setSingleChoiceItems(R.array.langnames, -1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.d(TAG,"Chose language id " + id);
						dialog.dismiss();

						if (-1 == id) {
							id = 0; // Force Hebrew if nothing was selected.
						}
						String[] languages = MainActivity.this.getResources().getStringArray(R.array.langs);
						// The chosen language is languages[id]
						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString("Language", languages[id]);
						editor.commit();
						
						// DB loading - category will be selected in onActivityResult().
						Intent questionsLoaderActivity = new Intent(getBaseContext(), QuestionsLoaderActivity.class);
						questionsLoaderActivity.putExtra("DownloadURL", preferences.getString("DownloadURL." + languages[id], getString(R.string.download_url_he_il)))
											.putExtra("Language", languages[id])
											.putExtra("Date", new Date().getTime());
						startActivityForResult(questionsLoaderActivity,INITIAL_ACTIVATION_QUESTIONS_LOADER_REQUEST_CODE);
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    
    /* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.init_db:
			if (initDb()) {
				performFirstActivation();
				return true;
			}
			else {
				return false;
			}
		case R.id.menu_settings:
			return preferencesSetup();
		case R.id.check_updates:
			try {
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				String language = preferences.getString("Language", getString(R.string.lang_he_il));
				return checkUpdates(language, preferences.getString("DownloadURL." + language, getString(R.string.download_url_he_il)));
				// See in strings.xml: download_url_he_il, download_url_ar_aa, download_url_ru_ru.
				// Temporary default for testing: "http://zak.homedns.org/TheoryExamHE.zip"
        		// Permanent default for Hebrew: "http://media.mot.gov.il/XML/TheoryExamHE.zip" in "http://data.gov.il/dataset/249"
			}
			catch (MalformedURLException e) {
				return false;
			}
		case R.id.clear_stats:
			return clearStats();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
    protected void onStop() {
    	super.onStop();
    }

	/* Activities invokable from the menu */
	
	private boolean initDb() {
		mDB.clearDb();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		SharedPreferences.Editor editor = preferences.edit();
		for (String language: getResources().getStringArray(R.array.langs)) {
			editor.putLong("LastModifiedTime." + language, 0L);
			Log.d(TAG,"Cleared LastModifiedTime for language " + language);
		}
		editor.commit();
		return true;
	}

	private boolean preferencesSetup() {
		Intent preferencesActivity = new Intent(getBaseContext(), TeuriaPreferenceActivity.class);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		String initialLanguage = preferences.getString("Language", getString(R.string.lang_he_il));

		// Prepare a list of categories for preferencesActivity
		String[] categories = mDB.getAllCategories();
		preferencesActivity.putExtra("Categories", categories);

		startActivity(preferencesActivity);
		Log.d(TAG,"preferencesSetup has been invoked");

		// Check if the Category preference needs to be fixed up.
		preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		// The following does not work because:
		// 1. Executed before preferencesActivity has started.
		// 2. We lose the current preferences selection in the dialog if we modify from "" to null.
		//if ("" == preferences.getString("Category", null)) {
		//	// Need to fix up (TeuriaPreferenceActivity does not allow values to be set to null)
	    //    SharedPreferences.Editor editor = preferences.edit();
	    //    editor.putString("Category", null);
	    //    editor.commit();
	    //    Log.d(TAG, "fixed up Category preference - empty string -> null");
		//}
		
		if (preferences.getString("Language", getString(R.string.lang_he_il)) != initialLanguage) {
			// A different language was chosen.
			// For now, we deal with this by reinitializing the DB.
			initDb();
			// !!! Currently, the user cannot change the currentUser setting.
		}
		return true;
	}

	/**
	 * Check whether the questions DB for the current language has been updated.
	 * 
	 * @param language - current language (in form like he_IL)
	 * @param downloadURL - from where we download update if necessary.
	 * @return
	 * @throws MalformedURLException
	 */
	private boolean checkUpdates(final String language, final String downloadURL) throws MalformedURLException {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		long lastModifiedTime = preferences.getLong("LastModifiedTime." + language, 0L);
		long date;
		try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) new URL(downloadURL).openConnection();
			date = con.getLastModified();
		}
		catch (IOException e) {
			date = new Date().getTime();
		}
		final long fDate = date;
		
		Log.d(TAG,"checkUpdates - last modified time:" + lastModifiedTime + ", URL last modification time:" + fDate);

		if (fDate > lastModifiedTime) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.xml_changed)
					.setCancelable(false)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Log.d(TAG,"yes to update DB");
							dialog.dismiss();

							Intent questionsLoaderActivity = new Intent(getBaseContext(), QuestionsLoaderActivity.class);
							questionsLoaderActivity.putExtra("DownloadURL", downloadURL)
												.putExtra("Language", language)
												.putExtra("Date", fDate);
							startActivityForResult(questionsLoaderActivity,QUESTIONS_LOADER_REQUEST_CODE);
						}
					})
					.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Log.d(TAG,"no to update DB");
							dialog.cancel();
							Toast toast = Toast.makeText(MainActivity.this, getString(R.string.xml_fetch_not_now), Toast.LENGTH_SHORT);
							toast.show();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
		}
		else {
			Toast toast = Toast.makeText(this, getString(R.string.xml_unchanged), Toast.LENGTH_SHORT);
			toast.show();
		}

		return true;
	}
	
	private boolean nextQuestion() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		String category = preferences.getString("Category", null);
		if (category == "") {
			// In the preferences dialog, cannot have null as one of the choices.
			category = null;
		}
		Question question = mDB.getRandomQuestion(preferences.getString("CurrentUser", "Me"),
												category);
		Intent showQuestionActivity = new Intent(getBaseContext(), ShowQuestionActivity.class);
		showQuestionActivity.putExtra("Title", question.getTitle())
							.putExtra("Content", question.getDescription())
							.putExtra("Category", question.getCategory())
							.putExtra("questionid", question.getID());
		startActivityForResult(showQuestionActivity,SHOW_QUESTION_REQUEST_CODE);

		return true;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG,"onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
		if (resultCode == RESULT_OK && requestCode == SHOW_QUESTION_REQUEST_CODE) {
			if (data.hasExtra("answer") && data.hasExtra("nextstep") && data.hasExtra("questionid")) {
				Log.d(TAG, "ShowQuestion returned questionid=" + data.getExtras().getInt("questionid")
						+ ", answer=" + data.getExtras().getInt("answer")
						+ ", nextstep=" + ShowQuestionActivity.ShowQuestionNextStep.values()[data.getExtras().getInt("nextstep")]
						);
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
				mDB.modifyAnswer(preferences.getString("CurrentUser", "Me"),
						data.getExtras().getInt("questionid"),
						data.getExtras().getInt("answer"));
				switch (ShowQuestionActivity.ShowQuestionNextStep.values()[data.getExtras().getInt("nextstep")]) {
				case NEXTSTEP_QUESTION: // Display another question
					nextQuestion();
					break;
				case NEXTSTEP_STATS:    // Display statistics
					displayStats();
					break;
				case NEXTSTEP_EXIT:     // Exit the application
					finish();
					break;
				default:
					throw new RuntimeException("ShowQuestion returned unknown next step: " + data.getExtras().getInt("nextstep"));
				}
			}
		}
		else if (resultCode == RESULT_OK && requestCode == QUESTIONS_LOADER_REQUEST_CODE) {
			Log.d(TAG, "Finished loading the questions database");
			// After loading the questions database, should reset the Category setting
			// because we may have changed language so the previous setting is invalid.
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("Category", "");
			editor.commit();

			displayStats();
		}
		else if (resultCode == RESULT_OK && requestCode == INITIAL_ACTIVATION_QUESTIONS_LOADER_REQUEST_CODE) {
			Log.d(TAG, "Finished loading the questions database during initial activation");
			displayStats();
			Log.d(TAG,"Now ready to work");

/*			
			// Third and final step of first activation - select a category.

			String[] categories = mDB.getAllCategories();
			String[] categoriesWithAll = new String[categories.length + 1];
			System.arraycopy(categories, 0,
					categoriesWithAll, 1, categories.length);
	        categoriesWithAll[0] = getString(R.string.all_categories);
	        final String[] fCategoriesWithAll = categoriesWithAll;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_a_category)
					.setCancelable(false)
					.setSingleChoiceItems(fCategoriesWithAll, 0, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Log.d(TAG,"Category " + id + "th was chosen");
							dialog.dismiss();

							String category = (id == 0) ? "" : fCategoriesWithAll[id];

							SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
							SharedPreferences.Editor editor = preferences.edit();
							editor.putString("Category", category);
							editor.commit();
							
							displayStats();
							Log.d(TAG,"Now ready to work");
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
*/
		}
	} 
	
	/**
	 * This function has two roles:
	 * 1. Display the current user's statistics of correctly and wrongly answered questions.
	 * 2. Help determine whether the first activation procedure needs to be followed.
	 * @return the number of rows displayed for the user's statistics.
	 * The number of rows is 1 if the DB is empty i.e. it is first activation.
	 */
	private int displayStats() {
		// Update the stats.
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		CategoryStats[] stats = mDB.getCategoryStatistics(preferences.getString("CurrentUser", "Me"));
		for (CategoryStats stat: stats) {
			Log.d(TAG,stat.toString());
		}
		mStatsAdapter.clear();
		
		for (CategoryStats stat: stats) {
			mStatsAdapter.add(stat);
		}
		
		if (false) {
			// !!! The following is tests of low level functions, that need testing
			// !!! before the problem with running Android unit tests is solved.
			String xmltext = "<span>Nothing</span>";
			String xmlresult = QuestionDescriptionTransform.transform(xmltext);
			Log.d(TAG,"Test 1: transform nothing: result=" + (xmlresult.equals(QuestionDescriptionTransform.JS_DOLI + xmltext) ? ", equal" : ", different"));
			Log.d(TAG,"[|" + xmlresult + "|]");
			Log.d(TAG,"expected nothing=");
			Log.d(TAG,"[|" + QuestionDescriptionTransform.JS_DOLI + xmltext + "|]");
			
			xmltext = "<li>wrong</li>";
			xmlresult = QuestionDescriptionTransform.transform(xmltext);
			Log.d(TAG,"Test 2: transform wrong: result=" + xmlresult + (xmlresult.equals(QuestionDescriptionTransform.JS_DOLI + "<li onclick=\"doli(this);\">wrong</li>") ? ", equal" : ", different"));
			xmltext = "<li id=\"whatever\">correct</li>";
			xmlresult = QuestionDescriptionTransform.transform(xmltext);
			Log.d(TAG,"Test 3: transform correct: result=" + xmlresult + (xmlresult.equals(QuestionDescriptionTransform.JS_DOLI + "<li id=\"T\" onclick=\"doli(this);\">correct</li>") ? ", equal" : ", different"));
	
			xmltext = "<li><span id=\"corre\">wrong</span></li>";
			xmlresult = QuestionDescriptionTransform.transform(xmltext);
			Log.d(TAG,"Test 4: 2nd wrong answer: result=" + (xmlresult.equals(QuestionDescriptionTransform.JS_DOLI + "<li onclick=\"doli(this);\"><span id=\"corre\">wrong</span></li>") ? "equal" : "different"));
	
			xmltext = "<li><span id=\"correct+\">corr2</span></li>";
			xmlresult = QuestionDescriptionTransform.transform(xmltext);
			Log.d(TAG,"Test 5: 2nd correct answer: result=" + (xmlresult.equals(QuestionDescriptionTransform.JS_DOLI + "<li onclick=\"doli(this);\" id=\"T\"><span id=\"correct+\">corr2</span></li>") ? "equal" : "different"));
			Log.d(TAG,"Result of test 5: " + xmlresult);
	
			xmltext = "<span>only a button<button>buttext</button></span>";
			xmlresult = QuestionDescriptionTransform.transform(xmltext);
			Log.d(TAG,"Test 6: remove a button: result=" + (xmlresult.equals(QuestionDescriptionTransform.JS_DOLI + "<span>only a button</span>") ? "equal" : "different"));
	
			xmltext = "<span><button>buttext</button>Nothing<button>second buttext</button></span>";
			xmlresult = QuestionDescriptionTransform.transform(xmltext);
			Log.d(TAG,"Test 7: do not remove 2 buttons: result=" + (xmlresult.equals(QuestionDescriptionTransform.JS_DOLI + xmltext) ? "equal" : "different"));
		}
		
		return stats.length;
	}
	
	private boolean clearStats() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		mDB.clearStatistics(preferences.getString("CurrentUser", "Me"));
		displayStats();
		return true;
	}
	
	// Button callbacks

	public void onClickSelectCategory(View view) {
		String category = ((CategoryStats) view.getTag()).getCategory();
		Log.d(TAG,"Selected category " + category + " from stats window");

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("Category", category);
		editor.commit();

		nextQuestion();
	}

	public void onClickExit(View view) {
		finish();
	}
}
