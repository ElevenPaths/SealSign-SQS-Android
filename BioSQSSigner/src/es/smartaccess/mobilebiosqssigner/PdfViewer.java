package es.smartaccess.mobilebiosqssigner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;

import es.smartaccess.SealSignSQSService.wcf.ArrayOfJobReference;
import es.smartaccess.SealSignSQSService.wcf.Job;
import es.smartaccess.mobilebiosqssigner.R;
import es.smartaccess.sealsignbss.SealSignBSSConstants;
import es.smartaccess.utils.FileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.FrameLayout.LayoutParams;

public class PdfViewer extends SherlockActivity 
{
    public static Context context = null;
	
	MuPDFReaderView mPDFView = null;
	MuPDFCore pdfCore = null;
	
	int selectJobId = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		context = this;
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pdf_viewer_layout);
		
		RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    
		mPDFView = new MuPDFReaderView(this);
		mPDFView.setBackgroundColor(Color.WHITE);
		mPDFView.setLayoutParams(relativeParams);
		
		RelativeLayout pdfViewContainer = (RelativeLayout)findViewById(R.id.pdfviewcontainer);
		pdfViewContainer.addView(mPDFView);
		
		Job job = AppHandler.getInstance().getM_JobToShow();
		selectJobId =  job.jobReferenceEx.id;
		
		getSupportActionBar().setIcon(R.drawable.green_pdf);
		
		InitializeView(job);
	}
	
	private void InitializeView(Job job)
	{
		setTitle(job.jobReferenceEx.jobTitle);
		
		try 
		{
			pdfCore = new MuPDFCore(job.jobReferenceEx.blob);
			pdfCore.countPages();
			mPDFView.setAdapter(new MuPDFPageAdapter(this, pdfCore));
		}
        catch (Exception ex) 
        {
        	Toast.makeText(this, getString(R.string.error_viewing_pdf), Toast.LENGTH_SHORT).show();
        	ex.printStackTrace();
        }
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu)
	{
		MenuItem sign = menu.findItem(R.id.sign);
		MenuItem delete = menu.findItem(R.id.delete);
		
		try 
		{
			int currentPage = MobileBioSQSSignerActivity.currentPage;
			switch (currentPage) 
			{
			case 0:
				sign.setVisible(true); //sign
				delete.setVisible(true);
				break;
				
			case 1:
				sign.setVisible(false); //sign
				delete.setVisible(true);
				break;

			default:
			}
		} 
		catch (Exception e) 
		{
			Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_pdf_viewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem)
	{  
		switch (menuItem.getItemId()) 
		{
		case android.R.id.home:
			finish();
			return true;

		case R.id.sign:
			if (AppHandler.getInstance().getM_MainActivity().hasConnection())
			{
				AppHandler.getInstance().getM_MainActivity().DoSignatureTask(selectJobId);
				finish();
			}
			else
				Toast.makeText(this, R.string.net_error, Toast.LENGTH_LONG).show();
			break;

		case R.id.delete:			
			Dialog dialog = AppHandler.getInstance().getM_MainActivity().confirmationDeleteDialogOnElement(selectJobId);
			dialog.show();
			finish();
			break;
			
		default:

		}
		return true;
	}
}
