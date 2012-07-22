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
import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.Window;

public class QuestionsLoaderActivity extends Activity {
	private static final String TAG = "QuestionsLoader";

	final XmlParsingProgress fProgress = new XmlParsingProgress() {
		public void setProgressBar(final long currentposition, final long maxposition) {
			QuestionsLoaderActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					long progressRatio = 9999L*currentposition/maxposition;
					Log.d(TAG,"progress bar: current=" + currentposition + ", max=" + maxposition + " i.e. " + ((int) progressRatio) + " units");
					QuestionsLoaderActivity.this.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, (int) progressRatio);
				}
			});
		}
	};

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Entered QuestionsLoader.onCreate()");
        
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
			return;
		}

		final String fDownloadURL = extras.getString("DownloadURL");
		final String fLanguage = extras.getString("Language");
		final long fDate = extras.getLong("Date");
        final TeuriaDatabase fDB = new TeuriaDatabase(this);

		QuestionsLoaderActivity.this.requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_questions_loader);
        //getActionBar().setDisplayHomeAsUpEnabled(true); // Belongs to API level 11
        QuestionsLoaderActivity.this.getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(QuestionsLoaderActivity.this);
        final String fCurrentUser = preferences.getString("CurrentUser", "Me");
		new Thread(new Runnable() {
			public void run() {
				try {
					//QuestionsLoaderActivity.this.requestWindowFeature(Window.FEATURE_PROGRESS);
					QuestionsFetcher.fetchQuestionsDatabase(fDownloadURL,
							new QuestionsFetcher.QuestionListener() {
						public void listenNewQuestion(Question question) {
							question.setDescription(QuestionDescriptionTransform.transform(question.getDescription()));
							int qid = fDB.addQuestion(question);
							fDB.initAnswerRecord(fCurrentUser, qid);
						}
						public void endOfQuestions() {
							QuestionsLoaderActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									QuestionsLoaderActivity.this.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
								}
							});
									
							// Save current values of preferences
							SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(QuestionsLoaderActivity.this);
							SharedPreferences.Editor editor = preferences.edit();
							editor.putLong("LastModifiedTime." + fLanguage, fDate);
							editor.commit();
							finish();
						}
					},
					fProgress);
				} catch (MalformedURLException e) {
					// Auto-generated catch block
					//e.printStackTrace();
				} catch (IOException e) {
					// Auto-generated catch block
					//e.printStackTrace();
				} catch (SAXException e) {
					// Auto-generated catch block
					//e.printStackTrace();
				}
			}
		}).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_questions_loader, menu);
        return true;
    }

    @Override
    public void finish() {
    	Log.d(TAG,"finish() was invoked");
    	Intent data = new Intent();
    	setResult(RESULT_OK, data);
    	super.finish();
    }

   
}
