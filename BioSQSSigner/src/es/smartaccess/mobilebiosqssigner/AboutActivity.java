package es.smartaccess.mobilebiosqssigner;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
//import android.app.Activity;
import android.content.Intent;
//import android.view.Menu;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import es.smartaccess.mobilebiosqssigner.R;

public class AboutActivity extends SherlockActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {  //Build.VERSION_CODES.ICE_CREAM_SANDWICH
//			getSupportActionBar().setHomeButtonEnabled(true); 
//			getSupportActionBar().setDisplayHomeAsUpEnabled(true); // show back arrow on title icon
//			getSupportActionBar().setDisplayShowHomeEnabled(true);
	    }
		
		TextView aboutText = (TextView)findViewById(R.id.aboutText);
		aboutText.setText(Html.fromHtml(getString(R.string.about_text)));
		aboutText.setMovementMethod(LinkMovementMethod.getInstance());
 }
 
 @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {       
        startActivity(new Intent(AboutActivity.this,MobileBioSQSSignerActivity.class)); 
        finish();
        return true;
    }
 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		
	   //getSupportMenuInflater().inflate(R.menu.about, menu);
		return true;
	}

}
