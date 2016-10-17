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

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * @author omer
 *
 */
public class TeuriaPreferenceActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
			return;
		}

		/*
		String[] categories = extras.getStringArray("Categories");
		
		// Set aside position 0 for All Categories.
		String[] categoriesWithAll = new String[categories.length + 1];
		System.arraycopy(categories, 0,
				categoriesWithAll, 1, categories.length);
		*/

        addPreferencesFromResource(R.xml.preferences);
        
        /*
        // Update the list of categories.
        ListPreference listPreference = (ListPreference) findPreference("Category");
        categoriesWithAll[0] = getString(R.string.all_categories);
        listPreference.setEntries(categoriesWithAll);
        String[] categ2 = categoriesWithAll.clone();
        categ2[0] = "";  // null is the value that we want but it causes a null pointer exception to be thrown.  So we must fix the preference "by hand".
        listPreference.setEntryValues(categ2);
        */
    }
    
    

}
