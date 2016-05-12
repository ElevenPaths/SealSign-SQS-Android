package es.smartaccess.mobilebiosqssigner;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import java.util.List;
import java.util.Locale;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import es.smartaccess.mobilebiosqssigner.R;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class ConnectionActivity extends SherlockPreferenceActivity {
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	final static String[] preferenceList ={"URLSealSign","ServiceTimeout","UserSealSign","PassSealSign"};
	
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	
	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);	      
	 }
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem menuItem)
	 {       
		 startActivity(new Intent(ConnectionActivity.this,MobileBioSQSSignerActivity.class)); 
		 finish();
		 return true;
	 }
	 

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
		
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	@SuppressWarnings("deprecation")
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		addPreferencesFromResource(R.xml.pref_general);
		
		PreferenceCategory preferencesHeader = new PreferenceCategory(this);
		preferencesHeader.setTitle(R.string.title_connection_parameters);
		getPreferenceScreen().addPreference(preferencesHeader);
		addPreferencesFromResource(R.xml.pref_connection);
		
		for(int i=0;i<preferenceList.length;i++){
			final EditTextPreference listPreference = (EditTextPreference) findPreference(preferenceList[i]);
			bindPreferences(listPreference);
		}
		
	}
	
	public static void bindPreferences(final EditTextPreference listPreference){

		if(listPreference.getText() == null) {
			listPreference.setText("undefined");
		}
		listPreference.setSummary(listPreference.getText());

		if(listPreference.getKey().toLowerCase(Locale.getDefault()).contains("pass"))
		{
			listPreference.setSummary("******");	
		}
		else
		{
			listPreference.setSummary(listPreference.getText());	
		}

		listPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {					

				if(preference.getKey().toLowerCase(Locale.getDefault()).contains("pass"))
				{
					preference.setSummary("******");	
				}
				else
				{
					preference.setSummary(newValue.toString());	
				}
				listPreference.setText(newValue.toString());
				return false;
			}
		});
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers_con, target);
		}
	}


	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class ConnectionPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);
			addPreferencesFromResource(R.xml.pref_connection);

			for(int i=0;i<preferenceList.length;i++){
				final EditTextPreference listPreference = (EditTextPreference) findPreference(preferenceList[i]);
				bindPreferences(listPreference);
			}
		}
	}


}
