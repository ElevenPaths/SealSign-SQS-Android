package es.smartaccess.mobilebiosqssigner;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.ksoap2.SoapFault;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import es.smartaccess.SealSignSQSService.wcf.ArrayOfJobReference;
import es.smartaccess.SealSignSQSService.wcf.BiometricSignatureContext;
import es.smartaccess.SealSignSQSService.wcf.Job;
import es.smartaccess.SealSignSQSService.wcf.JobReference;
import es.smartaccess.mobilebiosqssigner.R;
import es.smartaccess.sealsignbss.SealSignBSSConstants;
import es.smartaccess.utils.FileUtils;
import es.smartaccess.utils.ScreenUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.content.Context;
import android.graphics.Color;

public class MobileBioSQSSignerActivity extends SherlockFragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	static final int NUM_ITEMS = 2;

	final static int SIGNATURE_OK = 1;
	final static int SIGNATURE_FAIL = -1;

	final static int PROGRESS_HIDE = 0xFF01;
	final static int PROGRESS_SHOW = 0xFF02;
	final static int PROGRESS_SHOW_PROGRESS = 0xFF03;
	final static int PROGRESS_SHOW_BEGINSIGNATURE = 0xFF04;
	final static int PROGRESS_SHOW_BEGINSIGNATURECOMPONENTMODE = 0xFF05;
	final static int PROGRESS_SHOW_ENDSIGNATURECOMPONENTMODE = 0xFF06;

	static public byte[] finalBiometricState = null;

	static boolean multiselection = false;

	static DocAdapter[] adapters = null;

	public static int pageInit = 0;
	public static int currentPage = 0;

	public final static int PENDING_PAGE = 0;
	public final static int SIGNED_PAGE = 1;

	private Object CriticalSection = new Object();

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getString("URLSealSign", "") == "") {
			startActivity(new Intent(this, ConnectionActivity.class));

		} else {
			AppHandler.getInstance().setSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this));
			AppHandler.getInstance().setM_MainActivity(this);
			AppHandler.getInstance().setAppContext(this.getApplicationContext());
			AppHandler.getInstance().setMainContext(this);
			AppHandler.getInstance().setmCompomRunSignComponentMode(false);

			getSupportActionBar().setIcon(R.drawable.digital_signature);
			setContentView(R.layout.activity_mobile_signer);

			Intent myIntent = getIntent();
			Uri uri = myIntent.getData();

			if (uri != null) {
				// An external call executes the application
				try {
					String uriOrig = FileUtils.getFilePathByUri(uri);
					new AddJobTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uriOrig);
					// selectActionDialog(this, aux.getName(),false);
				} catch (Exception e) {
					Toast.makeText(this, R.string.file_error, Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}

			adapters = new DocAdapter[NUM_ITEMS];

			if (AppHandler.getInstance().getM_jobsPending() == null
					|| AppHandler.getInstance().getM_jobsProcessed() == null) {
				new GetJobsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
			} else {
				InitializeAdapters(AppHandler.getInstance().getM_jobsPending(),
						AppHandler.getInstance().getM_jobsProcessed());
			}
		}
	}

	@Override
	public void onBackPressed() {
		new AlertDialog.Builder(this).setTitle(R.string.exit_title).setMessage(R.string.exit_msg)
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						AppHandler.getInstance().setmCompomRunSignComponentMode(false);
						AppHandler.getInstance().getMainActivity().finish();
						((Activity) AppHandler.getInstance().getMainContext()).finish();
						pageInit = 0;
						MobileBioSQSSignerActivity.super.onBackPressed();
					}
				}).create().show();
	}

	private void RefreshMenus() {
		invalidateOptionsMenu();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem options = menu.findItem(R.id.options);
		MenuItem multicheck = menu.findItem(R.id.muticheck);
		MenuItem sign = menu.findItem(R.id.sign);
		MenuItem selectall = menu.findItem(R.id.select_all);
		MenuItem delete = menu.findItem(R.id.delete);
		MenuItem reload = menu.findItem(R.id.refreshdocuments);
		MenuItem changeUser = menu.findItem(R.id.changeuser);
		MenuItem changeOwner = menu.findItem(R.id.changeowner);

		try {
			if (AppHandler.getInstance().getmCompomRunSignComponentMode())
				return true;

			if (multiselection) {
				multicheck.setIcon(R.drawable.ic_back);
				reload.setVisible(false);
				changeUser.setVisible(false);
				options.setVisible(true);
				switch (currentPage) {
				case 0:
					sign.setVisible(true); // sign
					changeOwner.setVisible(true);
					break;
				case 1:
					sign.setVisible(false); // sign
					changeOwner.setVisible(false);
					break;
				default:
					Toast.makeText(this, R.string.general_error, Toast.LENGTH_LONG).show();
				}
			} else {
				multicheck.setIcon(R.drawable.ic_m_check);
				options.setVisible(false);
				sign.setVisible(false);// sign
				selectall.setVisible(false); // select all
				delete.setVisible(false); // delete
				changeOwner.setVisible(false);
			}
		} catch (Exception e) {
			Toast.makeText(this, R.string.general_error, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
			MenuInflater inflater = getSupportMenuInflater();
			inflater.inflate(R.menu.mobile_signer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		boolean[] chkBoxState = null;
		MobileBioSQSSignerActivity myActivity = this;
		MobileBioSQSSignerActivity.pageInit = this.mViewPager.getCurrentItem();

		switch (item.getItemId()) {
		case R.id.refreshdocuments:
			new GetJobsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
			return true;

		case R.id.changeuser:
			AppHandler.getInstance().getMainActivity()
					.startActivityForResult(new Intent(this, ConnectionActivity.class), 1);
			return true;

		case R.id.changeowner:
			_changeOwner();
			return true;

		case android.R.id.home:
			break;

		case R.id.Ajustes:
			intent = new Intent(this, ConnectionActivity.class);
			break;

		case R.id.Ayuda:
			intent = new Intent(this, AboutActivity.class);
			break;

		case R.id.muticheck:
			multiselection = !multiselection;
			RefreshAdapters();
			RefreshMenus();
			return true;

		case R.id.options:
			return true;

		case R.id.sign:
			chkBoxState = adapters[currentPage].checkBoxState;

			if (!isAnyChecked(chkBoxState)) {
				Toast.makeText(myActivity, R.string.noItemsSelected, Toast.LENGTH_LONG).show();
				return true;
			}

			if (((MobileBioSQSSignerActivity) myActivity).hasConnection()) {
				this.BiometricSignature();
			} else
				Toast.makeText(myActivity, R.string.net_error, Toast.LENGTH_LONG).show();

			return true;

		case R.id.select_all:
			chkBoxState = adapters[currentPage].checkBoxState;
			adapters[currentPage].flagAll = true;

			if (isAllChecked(chkBoxState))
				adapters[currentPage].flagAllAs = false;
			else
				adapters[currentPage].flagAllAs = true;

			((DocAdapter) adapters[currentPage]).notifyDataSetChanged();
			return true;

		case R.id.delete:
			chkBoxState = adapters[currentPage].checkBoxState;

			if (!isAnyChecked(chkBoxState)) {
				Toast.makeText(myActivity, R.string.noItemsSelected, Toast.LENGTH_LONG).show();
				return true;
			}

			confirmationDeleteDialog(null).show();

			return true;

		default:
			intent = new Intent(this, AboutActivity.class);
		}

		startActivity(intent);
		overridePendingTransition(0, 0);

		return true;
	}

	private void BiometricSignature() {
		if (AppHandler.getInstance().getmCompomRunSignComponentMode()) {
			// SignDocument(null, null);
		} else {
			new DocSignatureTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getJobsSelected(0));
		}
	}

	// Change Owner
	private void _changeOwner() {
		AlertDialog.Builder alert = new AlertDialog.Builder((Context) AppHandler.getInstance().getMainContext());
		alert.setTitle(R.string.changeowner);
		alert.setMessage(R.string.newowner);
		final EditText newOwner = new EditText((Context) AppHandler.getInstance().getMainContext());
		newOwner.setInputType(InputType.TYPE_CLASS_TEXT);
		alert.setView(newOwner);

		alert.setPositiveButton(R.string.msg_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				new ChangeOwnerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getJobsSelected(currentPage),
						newOwner.getText().toString());
				dialog.dismiss();
			}
		});

		alert.setNegativeButton(R.string.msg_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});

		alert.show();
	}

	// Simple delete
	public Dialog confirmationDeleteDialogOnElement(Integer jobId) {
		return confirmationDeleteDialog(jobId);
	}

	// Multiple delete
	private Dialog confirmationDeleteDialog(Integer jobId) {
		final Integer job = jobId;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final Activity mActivity = AppHandler.getInstance().getMainActivity();
		builder.setTitle(R.string.delete_title);
		builder.setMessage(R.string.delete_msg);
		builder.setPositiveButton(R.string.delete_ok, new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					if (job == null) {
						new DeleteJobTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
								getJobsSelected(currentPage));
					} else {
						DeleleJob(job.intValue());
					}
				} catch (Exception e) {
					Toast.makeText(AppHandler.getInstance().getMainActivity(), R.string.delete_error, Toast.LENGTH_LONG)
							.show();
					e.printStackTrace();
				}

				dialog.cancel();
			}
		});

		builder.setNegativeButton(R.string.delete_cancel, new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		return builder.create();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case (0):
			break;

		case (1):
			// Check the user change has success
			new GetJobsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
			break;

		case (2):
			try {
				byte[] finalBiometricState = data.getByteArrayExtra(SealSignBSSConstants.BIOMETRIC_STATE);
				MobileBioSQSSignerActivity.finalBiometricState = finalBiometricState;
				synchronized (AppHandler.getInstance()) {
					AppHandler.getInstance().notifyAll();
				}
			} catch (Exception e) {
				Toast.makeText(this, R.string.msg_error, Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
			break;
		}
	}

	private boolean isAllChecked(boolean[] chkBoxArray) {
		for (int i = 0; i < chkBoxArray.length; i++) {
			if (chkBoxArray[i] == false)
				return false;
		}

		return true;
	}

	private boolean isAnyChecked(boolean[] chkBoxArray) {
		for (int i = 0; i < chkBoxArray.length; i++) {
			if (chkBoxArray[i] == true)
				return true;
		}

		return false;
	}

	@SuppressWarnings("unused")
	private static int numChecked(boolean[] chkBoxArray) {
		int ret = 0;

		for (int i = 0; i < chkBoxArray.length; i++) {
			if (chkBoxArray[i] == true)
				ret++;
		}

		return ret;
	}

	public boolean hasConnection() {
		MobileBioSQSSignerActivity myActivity = this;
		ConnectivityManager cm = (ConnectivityManager) myActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiNetwork != null && wifiNetwork.isConnected()) {
			return true;
		}

		NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (mobileNetwork != null && mobileNetwork.isConnected()) {
			return true;
		}

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isConnected()) {
			return true;
		}

		return false;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		public SectionsPagerAdapter(FragmentManager fm, ArrayOfJobReference jobsPending,
				ArrayOfJobReference jobsProcessed) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return DocsSectionFragment.newInstance(position);
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return NUM_ITEMS;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l) + "("
						+ AppHandler.getInstance().getSignatureServiceUserName() + ")";

			case 1:
				return getString(R.string.title_section2).toUpperCase(l) + "("
						+ AppHandler.getInstance().getSignatureServiceUserName() + ")";
			}
			return null;
		}
	}

	/**
	 * A fragment representing a section of the app, but that simply displays
	 * dummy text.
	 */

	public static class DocsSectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */

		// All static variables
		static final String KEY_DOC = "doc"; // parent node
		static final String KEY_ID = "id";
		static final String KEY_TITLE = "title";
		static final String KEY_DATE = "date";
		static final String KEY_OWNER = "owner";
		static final String KEY_IMAGE = "pdf";
		static final String KEY_COMPUTERNAME = "computerName";
		static final String KEY_QUEUENAME = "queueName";

		// List view
		private ListView lv;

		// ArrayAdapter<String> adapter;
		DocAdapter adapter;

		// Search EditText
		EditText inputSearch;

		public DocsSectionFragment() {
		}

		static DocsSectionFragment newInstance(int num) {
			DocsSectionFragment f = new DocsSectionFragment();
			// Supply num input as an argument.
			Bundle args = new Bundle();
			args.putInt("num", num);
			f.setArguments(args);
			return f;
		}

		@SuppressLint("SimpleDateFormat")
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_mobile_signer, container, false);
			ArrayList<HashMap<String, String>> DocList = new ArrayList<HashMap<String, String>>();
			int currentPage = this.getArguments().getInt("num", 0);
			ArrayOfJobReference m_jobs = (currentPage == 0) ? AppHandler.getInstance().getM_jobsPending()
					: AppHandler.getInstance().getM_jobsProcessed();

			DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy hh:mm:ss");
			if ((m_jobs != null) && (m_jobs.size() != 0)) {
				for (int i = 0; i < m_jobs.size(); i++) {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put(KEY_ID, String.valueOf(i));
					map.put(KEY_TITLE, m_jobs.get(i).jobTitle);
					map.put(KEY_DATE, fmt.print(m_jobs.get(i).time));
					map.put(KEY_COMPUTERNAME, m_jobs.get(i).computerName);
					map.put(KEY_QUEUENAME, m_jobs.get(i).queueName);
					map.put(KEY_OWNER, m_jobs.get(i).owner);
					map.put(KEY_IMAGE, "pdf");
					DocList.add(map);
				}
			}

			boolean[] oldChkBoxState = null;
			if (multiselection) {
				oldChkBoxState = adapters[currentPage].checkBoxState;
			}

			adapter = new DocAdapter(getActivity(), DocList, multiselection);
			adapters[currentPage] = adapter;
			if (multiselection && oldChkBoxState != null) {
				adapters[currentPage].checkBoxState = oldChkBoxState;
			}
			lv = (ListView) rootView.findViewById(R.id.list);
			lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);// CHOICE_MODE_MULTIPLE
															// CHOICE_MODE_NONE
			lv.setItemsCanFocus(false);
			lv.setAdapter(adapter);

			lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					if (!multiselection) {
						MobileBioSQSSignerActivity myActivity = (MobileBioSQSSignerActivity) getActivity();

						int currentPage = myActivity.mViewPager.getCurrentItem();
						DocsSectionFragment currentFragment = (DocsSectionFragment) getFragmentManager()
								.findFragmentByTag("android:switcher:" + R.id.pager + ":" + currentPage);
						ArrayOfJobReference m_jobs = (currentPage == 0) ? AppHandler.getInstance().getM_jobsPending()
								: AppHandler.getInstance().getM_jobsProcessed();
						AppHandler.getInstance().getM_MainActivity().GetJob(m_jobs.get(position).id);
					}
				}
			});

			inputSearch = (EditText) rootView.findViewById(R.id.inputSearch);

			/**
			 * Enabling Search Filter
			 */
			inputSearch.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
					// When user changed the Text
					MobileBioSQSSignerActivity myActivity = (MobileBioSQSSignerActivity) getActivity();
					int currentPage = myActivity.mViewPager.getCurrentItem();
					DocsSectionFragment currentFragment = (DocsSectionFragment) getFragmentManager()
							.findFragmentByTag("android:switcher:" + R.id.pager + ":" + currentPage);
					currentFragment.adapter.getFilter().filter(cs);
				}

				@Override
				public void afterTextChanged(Editable s) {
					// TODO Auto-generated method stub

				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// TODO Auto-generated method stub

				}
			});

			return rootView;
		}
	}// end docsectionfragment

	private void RefreshAdapters() {
		synchronized (CriticalSection) {
			try {
				if (mSectionsPagerAdapter != null) {
					mSectionsPagerAdapter = (SectionsPagerAdapter) mViewPager.getAdapter();
					mViewPager.setAdapter(mSectionsPagerAdapter);
					mViewPager.setCurrentItem(currentPage);
					mSectionsPagerAdapter.notifyDataSetChanged();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Pending Jobs
	private void InitializeAdapters(ArrayOfJobReference jobsPending, ArrayOfJobReference jobsProcessed) {
		// Initialize the View with the pending docs
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		if (mSectionsPagerAdapter == null) {
			mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), jobsPending, jobsProcessed);
			// Set up the ViewPager with the sections adapter.
			mViewPager = (ViewPager) findViewById(R.id.pager);
			mViewPager.setAdapter(mSectionsPagerAdapter);
			mViewPager.setCurrentItem(pageInit);
			currentPage = pageInit;

			mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
				@Override
				public void onPageSelected(int index) {
					currentPage = index;
					supportInvalidateOptionsMenu();
				}

				@Override
				public void onPageScrollStateChanged(int arg0) {
				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
				}
			});
		} else {
			RefreshAdapters();
		}
	}

	@SuppressWarnings("unchecked")
	public void DeleleJob(Integer jobId) {
		List<JobReference> jobs = new ArrayList<JobReference>();
		jobs.add(GetById(jobId));
		new DeleteJobTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jobs);
	}

	public void GetJob(Integer jobId) {
		new GetJobTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jobId);
	}

	List<JobReference> getJobsSelected(int page) {
		ArrayOfJobReference jobs = (page == 0) ? AppHandler.getInstance().getM_jobsPending()
				: AppHandler.getInstance().getM_jobsProcessed();
		boolean[] checkboxState = adapters[page].checkBoxState;
		List<JobReference> result = new ArrayList<JobReference>();
		for (int i = 0; i < checkboxState.length; i++) {
			if (checkboxState[i] == true) {
				result.add(jobs.get(i));
			}
		}

		return result;
	}

	public void DoSignatureTask(Integer jobId) {
		List<JobReference> jobs = new ArrayList<JobReference>();
		jobs.add(GetById(jobId));
		new DocSignatureTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jobs);
	}

	public JobReference GetById(Integer id) {
		ArrayOfJobReference jobs = AppHandler.getInstance().getM_jobsPending();
		for (int i = 0; i < jobs.size(); i++) {
			if (jobs.get(i).id.intValue() == id.intValue()) {
				return jobs.get(i);
			}
		}

		jobs = AppHandler.getInstance().getM_jobsProcessed();
		for (int i = 0; i < jobs.size(); i++) {
			if (jobs.get(i).id.intValue() == id.intValue()) {
				return jobs.get(i);
			}
		}

		return null;
	}

	// Get All Jobs
	private class GetJobsTask extends AsyncTask<Boolean, List<ArrayOfJobReference>, List<ArrayOfJobReference>> {
		private ProgressDialog mProgress = null;
		private String message = null;
		private Boolean bException = false;

		@Override
		protected void onPreExecute() {
			mProgress = new ProgressDialog(AppHandler.getInstance().getMainContext());
			mProgress.setTitle(AppHandler.getInstance().getMainContext().getString(R.string.service_wait));
			mProgress.setMessage(AppHandler.getInstance().getMainContext().getString(R.string.service_gettingpending));
			mProgress.setCancelable(false);
			mProgress.show();
		}

		@Override
		protected List<ArrayOfJobReference> doInBackground(Boolean... params) {
			List<ArrayOfJobReference> result = null;

			if (params[0] == true) {
				// Background task for automatic refresh of 'jobs'
				Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
				while (true) {
					try {
						result = new ArrayList<ArrayOfJobReference>();
						AppHandler.getInstance().setM_jobsPending(
								AppHandler.getInstance().getService().GetPendingJobs(null, null, null));
						AppHandler.getInstance().setM_jobsProcessed(
								AppHandler.getInstance().getService().GetProcessedJobs(null, null, null));
						result.add(0, AppHandler.getInstance().getM_jobsPending());
						result.add(1, AppHandler.getInstance().getM_jobsProcessed());
					} catch (Exception e) {
						bException = true;
						message = e.getMessage();
						e.printStackTrace();

						result = new ArrayList<ArrayOfJobReference>();
						result.add(0, null);
						result.add(1, null);
						AppHandler.getInstance().setM_jobsPending(null);
						AppHandler.getInstance().setM_jobsProcessed(null);
					}

					publishProgress(result);

					try {
						Thread.sleep(AppHandler.getInstance().getSignatureservicerefreshinterval());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				try {
					// Background task for the refresh of jobs on demand
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
					result = new ArrayList<ArrayOfJobReference>();
					AppHandler.getInstance()
							.setM_jobsPending(AppHandler.getInstance().getService().GetPendingJobs(null, null, null));
					AppHandler.getInstance().setM_jobsProcessed(
							AppHandler.getInstance().getService().GetProcessedJobs(null, null, null));
					result.add(0, AppHandler.getInstance().getM_jobsPending());
					result.add(1, AppHandler.getInstance().getM_jobsProcessed());
				} catch (Exception e) {
					bException = true;
					message = e.getMessage();
					e.printStackTrace();

					result = new ArrayList<ArrayOfJobReference>();
					result.add(0, null);
					result.add(1, null);
					AppHandler.getInstance().setM_jobsPending(null);
					AppHandler.getInstance().setM_jobsProcessed(null);
				}
			}

			return result;
		}

		@Override
		protected void onProgressUpdate(List<ArrayOfJobReference>... params) {
			mProgress.hide();
			if (bException) {
				Toast.makeText(AppHandler.getInstance().getMainContext(), getString(R.string.net_error) + ":" + message,
						Toast.LENGTH_LONG).show();
				bException = false;
			}
			InitializeAdapters(params[0].get(0), params[0].get(1));
		}

		@Override
		protected void onPostExecute(List<ArrayOfJobReference> result) {
			mProgress.dismiss();
			if (bException) {
				Toast.makeText(AppHandler.getInstance().getMainContext(), getString(R.string.net_error) + ":" + message,
						Toast.LENGTH_LONG).show();
			}
			InitializeAdapters(result.get(0), result.get(1));
		}
	}

	// Delete Job Multiple
	private class DeleteJobTask extends AsyncTask<List<JobReference>, Integer, Integer> {
		private ProgressDialog mProgress = null;
		private String message = null;
		private Boolean bException = false;
		ArrayOfJobReference jobs = (currentPage == 0) ? AppHandler.getInstance().getM_jobsPending()
				: AppHandler.getInstance().getM_jobsProcessed();

		@Override
		protected void onPreExecute() {
			mProgress = new ProgressDialog(AppHandler.getInstance().getMainContext());
			mProgress.setTitle(AppHandler.getInstance().getMainContext().getString(R.string.service_wait));
			mProgress.setMessage(AppHandler.getInstance().getMainContext().getString(R.string.service_deletingjob));
			mProgress.setCancelable(false);
			mProgress.show();
		}

		@Override
		protected Integer doInBackground(List<JobReference>... params) {
			List<JobReference> removeAll = params[0];
			for (int i = 0; i < removeAll.size(); i++) {
				try {
					AppHandler.getInstance().getService().RemoveJob(removeAll.get(i).id);
					JobCache.getInstance().InvalidateEntry(removeAll.get(i).id);
				} catch (Exception e) {
					bException = true;
					message = e.getMessage();
					e.printStackTrace();
				}
			}

			if (removeAll.size() > 0) {
				jobs.removeAll(removeAll);
			}

			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			mProgress.dismiss();
			if (bException) {
				Toast.makeText(AppHandler.getInstance().getMainContext(), getString(R.string.net_error) + ":" + message,
						Toast.LENGTH_LONG).show();
			}

			RefreshAdapters();
		}
	}

	// Get Job
	private class GetJobTask extends AsyncTask<Integer, Void, Job> {
		private ProgressDialog mProgress = null;
		private String message = null;
		private Boolean bException = false;

		@Override
		protected void onPreExecute() {
			mProgress = new ProgressDialog(AppHandler.getInstance().getMainContext());
			mProgress.setTitle(AppHandler.getInstance().getMainContext().getString(R.string.service_wait));
			mProgress.setMessage(AppHandler.getInstance().getMainContext().getString(R.string.service_gettingjob));
			mProgress.setCancelable(false);
			mProgress.show();
		}

		@Override
		protected Job doInBackground(Integer... params) {
			Job result = null;
			try {
				if (JobCache.getInstance().ExistInCache(params[0])) {
					result = JobCache.getInstance().GetFromCache(params[0]);
				} else {
					result = AppHandler.getInstance().getService().GetJob(params[0]);
					JobCache.getInstance().AddToCache(result);
				}

			} catch (Exception e) {
				bException = true;
				message = e.getMessage();
				e.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(Job result) {
			mProgress.dismiss();
			if (bException) {
				Toast.makeText(AppHandler.getInstance().getMainContext(), getString(R.string.net_error) + ":" + message,
						Toast.LENGTH_LONG).show();
			} else {
				if (result == null) {
					Toast.makeText(AppHandler.getInstance().getMainContext(),
							getString(R.string.service_jobnotexist) + ":" + message, Toast.LENGTH_LONG).show();
					JobCache.getInstance().RemoveCacheItems();
					RefreshAdapters();
				} else {
					Intent intent = new Intent(AppHandler.getInstance().getM_MainActivity(), PdfViewer.class);
					AppHandler.getInstance().setM_JobToShow(result);
					startActivity(intent);
				}
			}
		}
	}

	// Add Job
	private class AddJobTask extends AsyncTask<String, Void, Integer> {
		private ProgressDialog mProgress = null;
		private String message = null;
		private Boolean bException = false;

		@Override
		protected void onPreExecute() {
			mProgress = new ProgressDialog(AppHandler.getInstance().getMainContext());
			mProgress.setTitle(AppHandler.getInstance().getMainContext().getString(R.string.service_wait));
			mProgress.setMessage(AppHandler.getInstance().getMainContext().getString(R.string.service_addingjob));
			mProgress.setCancelable(false);
			mProgress.show();
		}

		@Override
		protected Integer doInBackground(String... params) {
			Integer result = null;
			try {
				File file = new File(params[0]);
				InputStream fileStream = null;
				fileStream = new BufferedInputStream(new FileInputStream(file));
				int size = fileStream.available();
				byte[] buffer = new byte[size];
				fileStream.read(buffer);
				fileStream.close();
				TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				result = AppHandler.getInstance().getService().AddJob(
						android.os.Build.MANUFACTURER + android.os.Build.MODEL + telephonyManager.getDeviceId(), 0,
						params[0], buffer, null);
			} catch (Exception e) {
				bException = true;
				message = e.getMessage();
				e.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(Integer result) {
			mProgress.dismiss();
			if (bException) {
				Toast.makeText(AppHandler.getInstance().getMainContext(), getString(R.string.net_error) + ":" + message,
						Toast.LENGTH_LONG).show();
			} else {
				new GetJobsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
			}
		}
	}

	// Change Owner
	private class ChangeOwnerTask extends AsyncTask<Object, Void, Integer> {
		private ProgressDialog mProgress = null;
		private String message = null;
		private Boolean bException = false;

		@Override
		protected void onPreExecute() {
			mProgress = new ProgressDialog(AppHandler.getInstance().getMainContext());
			mProgress.setTitle(AppHandler.getInstance().getMainContext().getString(R.string.service_wait));
			mProgress.setMessage(AppHandler.getInstance().getMainContext().getString(R.string.service_changingowner));
			mProgress.setCancelable(false);
			mProgress.show();
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Integer doInBackground(Object... params) {
			List<JobReference> jobs = (List<JobReference>) params[0];
			String newOwner = (String) params[1];
			for (int i = 0; i < jobs.size(); i++) {
				try {
					AppHandler.getInstance().getService().ChangeJobOwner(jobs.get(i).id, newOwner);
					JobCache.getInstance().InvalidateEntry(jobs.get(i).id);
				} catch (Exception e) {
					bException = true;
					message = e.getMessage();
					e.printStackTrace();
				}
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			mProgress.dismiss();
			if (bException) {
				Toast.makeText(AppHandler.getInstance().getMainContext(), getString(R.string.net_error) + ":" + message,
						Toast.LENGTH_LONG).show();
			} else {
				new GetJobsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
			}
		}
	}

	private class DocSignatureTask extends AsyncTask<List<JobReference>, Integer, Integer> {
		private ProgressDialog mProgress = null;
		private String message = null;
		private Boolean bException = false;
		ArrayOfJobReference jobs = AppHandler.getInstance().getM_jobsPending();

		@SuppressLint("InlinedApi")
		@Override
		protected void onPreExecute() {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
				int currentOrientation = (AppHandler.getInstance().getMainContext()).getResources()
						.getConfiguration().orientation;
				if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
					((Activity) AppHandler.getInstance().getMainContext())
							.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
				} else {
					((Activity) AppHandler.getInstance().getMainContext())
							.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
				}
			} else {
				((Activity) AppHandler.getInstance().getMainContext())
						.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			}

			super.onPreExecute();

			mProgress = new ProgressDialog(AppHandler.getInstance().getMainContext());
			mProgress.setTitle(
					AppHandler.getInstance().getMainContext().getString(R.string.progress_beginsignature_title));
			mProgress.setMessage(
					AppHandler.getInstance().getMainContext().getString(R.string.progress_beginsignature_text));
			mProgress.setCancelable(false);
			mProgress.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					cancel(true);
				}
			});
		}

		@Override
		protected Integer doInBackground(List<JobReference>... params) {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			AppHandler.getInstance().setmCompomRunSignComponentModeError("");
			AppHandler.getInstance().setmCompomRunSignComponentModeCancelFromDNIe(false);

			try {
				Intent myIntent = ScreenUtils.getBSSPanelIntent();

				publishProgress(PROGRESS_SHOW_BEGINSIGNATURE);

				List<JobReference> jobsToSign = params[0];
				for (int i = 0; i < jobsToSign.size(); i++) {
					Job job = null;
					if (JobCache.getInstance().ExistInCache(jobsToSign.get(i).id)) {
						job = JobCache.getInstance().GetFromCache(jobsToSign.get(i).id);
					} else {
						job = AppHandler.getInstance().getService().GetJob(jobsToSign.get(i).id);
					}

					publishProgress(PROGRESS_SHOW_BEGINSIGNATURE);
					byte[] signedDocument = null;
					for (int j = 0; j < job.signatureClientBehaviour.size(); j++) {
						BiometricSignatureContext context = AppHandler.getInstance().getService()
								.BeginBiometricSignatureProvider(job.jobReferenceEx.id, j, job.jobReferenceEx.queueName,
										job.signatureClientBehaviour.get(j).signatureId,
										job.signatureClientBehaviour.get(j).signatureAccount,
										job.signatureClientBehaviour.get(j).uri,
										job.signatureClientBehaviour.get(j).providerParameter, new byte[0]);

						myIntent.putExtra(SealSignBSSConstants.ETIQUETA_TEXT,
								job.signatureClientBehaviour.get(j).signatureWindowTitle);
						myIntent.putExtra(SealSignBSSConstants.DATE_TEXT, new java.util.Date().toString());
						myIntent.putExtra(SealSignBSSConstants.BIOMETRIC_INSTANCE, context.instance.toString());
						myIntent.putExtra(SealSignBSSConstants.BIOMETRIC_STATE, context.biometricState);

						AppHandler.getInstance().getMainActivity().startActivityForResult(myIntent, 2);
						publishProgress(PROGRESS_HIDE);
						synchronized (AppHandler.getInstance()) {
							AppHandler.getInstance().wait();
						}
						publishProgress(PROGRESS_SHOW_PROGRESS);

						signedDocument = AppHandler.getInstance().getService().EndBiometricSignatureProvider(
								context.instance, finalBiometricState, job.jobReferenceEx.id, j,
								job.jobReferenceEx.queueName, job.signatureClientBehaviour.get(j).uri,
								job.signatureClientBehaviour.get(j).providerParameter);

						publishProgress(PROGRESS_HIDE);
					}

					JobCache.getInstance().InvalidateEntry(job.jobReferenceEx.id);
					job.jobReferenceEx.blob = new byte[signedDocument.length];
					System.arraycopy(signedDocument, 0, job.jobReferenceEx.blob, 0, signedDocument.length);
					JobCache.getInstance().AddToCache(job);
				}
			} catch (SoapFault e) {
				bException = true;
				message = e.getMessage();
				AppHandler.getInstance().setmCompomRunSignComponentModeError(e.faultstring);
				e.printStackTrace();
			} catch (Exception e) {
				bException = true;
				message = e.getMessage();
				AppHandler.getInstance().setmCompomRunSignComponentModeError(AppHandler.getInstance().getMainActivity()
						.getResources().getString(R.string.custom_msg_sig_error));
				e.printStackTrace();
			}

			return 0;
		}

		@Override
		protected void onProgressUpdate(Integer... params) {
			super.onProgressUpdate(params);
			int action = params[0];

			if (action == PROGRESS_HIDE) {
				mProgress.hide();
			} else if (action == PROGRESS_SHOW_PROGRESS) {
				mProgress.setTitle(
						AppHandler.getInstance().getMainContext().getString(R.string.progress_endsignature_title));
				mProgress.setMessage(
						AppHandler.getInstance().getMainContext().getString(R.string.progress_endsignature_text));
				mProgress.show();
			} else if (action == PROGRESS_SHOW_BEGINSIGNATURE) {
				mProgress.setTitle(
						AppHandler.getInstance().getMainContext().getString(R.string.progress_beginsignature_title));
				mProgress.setMessage(
						AppHandler.getInstance().getMainContext().getString(R.string.progress_beginsignature_text));
				mProgress.show();
			} else if (action == PROGRESS_SHOW_BEGINSIGNATURECOMPONENTMODE) {
				mProgress.setTitle(AppHandler.getInstance().getMainContext()
						.getString(R.string.progress_beginsignaturecomponentmode_title));
				mProgress.setMessage(AppHandler.getInstance().getMainContext()
						.getString(R.string.progress_beginsignaturecomponentmode_text));
				mProgress.show();
			} else if (action == PROGRESS_SHOW_ENDSIGNATURECOMPONENTMODE) {
				mProgress.setTitle(AppHandler.getInstance().getMainContext()
						.getString(R.string.progress_endsignaturecomponentmode_title));
				mProgress.setMessage(AppHandler.getInstance().getMainContext()
						.getString(R.string.progress_endsignaturecomponentmode_text));
				mProgress.show();
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			mProgress.dismiss();

			if (bException) {
				Toast.makeText(AppHandler.getInstance().getMainContext(), getString(R.string.net_error) + ":" + message,
						Toast.LENGTH_LONG).show();
			} else {
				MobileBioSQSSignerActivity.pageInit = MobileBioSQSSignerActivity.SIGNED_PAGE;
				MobileBioSQSSignerActivity.currentPage = MobileBioSQSSignerActivity.SIGNED_PAGE;
				MobileBioSQSSignerActivity.multiselection = false;
				// AppHandler.ReturnFromSignComponentMode(res);
				new GetJobsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
			}
		}
	}// Fin BeginSignature
}// end MobileSignerActivity
