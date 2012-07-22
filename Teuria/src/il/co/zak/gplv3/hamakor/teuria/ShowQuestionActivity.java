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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;

/**
 * @author omer
 *
 */
public class ShowQuestionActivity extends Activity {
    private static final String mimeType = "text/html";
    private ShowQuestionResultCode mAnswer;
    private int mQuestionId; // Value that the calling activity wants us to remember for them.
    
    public enum ShowQuestionResultCode {
    	ANSWER_SKIP(0),
    	ANSWER_WRONG(1),
    	ANSWER_CORRECT(2);
    	
    	private final int cvalue;
    	ShowQuestionResultCode(int cvalue) {
    		this.cvalue = cvalue;
    	}
    	public int getCValue() {
    		return cvalue;
    	}
    }
    
    public enum ShowQuestionNextStep {
    	// !!! Coordinate numeric values with TeuriaDatabase.java if you change them.
    	NEXTSTEP_QUESTION(0), // Display another question
    	NEXTSTEP_STATS(1),    // Display statistics
    	NEXTSTEP_EXIT(2);     // Exit the application

    	private final int cvalue;
    	ShowQuestionNextStep(int cvalue) {
    		this.cvalue = cvalue;
    	}
    	public int getCValue() {
    		return cvalue;
    	}
    }

	private static final String TAG = "ShowQuestion";
	
	public class JavaScriptInterface {
		final ShowQuestionActivity mContext;
		
		JavaScriptInterface(ShowQuestionActivity context) {
			mContext = context;
		}
		
		/**
		 * Set the answer's result code
		 * @param resstatus - true for ANSWER_CORRECT, false for ANSWER_WRONG.
		 */
		public void setResultCode(boolean resStatus) {
			Log.d(TAG, "JavaScript returned: " + resStatus);
			mContext.mAnswer =  resStatus ? ShowQuestionResultCode.ANSWER_CORRECT
					: ShowQuestionResultCode.ANSWER_WRONG;
			// We also want to scroll down once the user has chosen an answer.
			final ScrollView scrollView = (ScrollView) mContext.findViewById(R.id.scrollview);
			scrollView.post(new Runnable() {
				public void run() {
					scrollView.smoothScrollBy(0,200);   // !!! Need a way to compute height, instead of guessing.
				}
			});
		}
	}
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
			return;
		}

		String title = extras.getString("Title");
		String content = extras.getString("Content");
		String category = extras.getString("Category");
		mQuestionId = extras.getInt("questionid");

		mAnswer = ShowQuestionResultCode.ANSWER_SKIP;
		
        setContentView(R.layout.show_question);
        setTitle(category);
        WebView wv = (WebView) findViewById(R.id.webview);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(new JavaScriptInterface(this), "androidresult");
        content = "<html><style type='text/css'>.text-highlight { background: #FFFFAA }</style><body><div dir='rtl'>" + title + "</div><div>" + content + "</div></body></html>";
        //wv.loadData(content, mimeType, null);
        wv.loadDataWithBaseURL("http://tqpic.mot.gov.il/", content, mimeType, "utf-8", null);
	}
    
    public void onClickNextQuestion(View view) {
    	Log.d(TAG,"onClickNextQuestion was invoked");
    	Intent data = new Intent();
    	data.putExtra("answer", mAnswer.getCValue());
    	data.putExtra("nextstep", ShowQuestionNextStep.NEXTSTEP_QUESTION.getCValue());
    	data.putExtra("questionid", mQuestionId);
    	setResult(RESULT_OK, data);
    	super.finish();
    	
    }
    public void onClickShowStats(View view) {
    	Log.d(TAG,"onClickShowStats was invoked");
    	Intent data = new Intent();
    	data.putExtra("answer", mAnswer.getCValue());
    	data.putExtra("nextstep", ShowQuestionNextStep.NEXTSTEP_STATS.getCValue());
    	data.putExtra("questionid", mQuestionId);
    	setResult(RESULT_OK, data);
    	super.finish();
    	
    }
    public void onClickExit(View view) {
    	Log.d(TAG,"onClickExit was invoked");
    	finish();
    }
    @Override
    public void finish() {
    	Log.d(TAG,"finish() was invoked");
    	Intent data = new Intent();
    	data.putExtra("answer", mAnswer.getCValue());
    	data.putExtra("nextstep", ShowQuestionNextStep.NEXTSTEP_EXIT.getCValue());
    	data.putExtra("questionid", mQuestionId);
    	setResult(RESULT_OK, data);
    	super.finish();
    }
}
