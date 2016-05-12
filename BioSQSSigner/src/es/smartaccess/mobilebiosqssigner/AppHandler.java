package es.smartaccess.mobilebiosqssigner;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.ksoap2.HeaderProperty;

import es.smartaccess.SealSignSQSService.wcf.ArrayOfJobReference;
import es.smartaccess.SealSignSQSService.wcf.BasicHttpBinding_ISignatureQueueServiceBasic;
import es.smartaccess.SealSignSQSService.wcf.Job;
import es.smartaccess.SealSignSQSService.wcf.SSLConection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class AppHandler {
	final static String signatureServiceSVC = "SignatureQueueServiceBasic.svc";
	final int    		signatureServiceRefreshInterval = 15000;
	private Context AppContext = null;
	private Context mainContext = null;
	private String  uriFrontend = null;
	private String  providerParameter = null;
	private Boolean mCompomRunSignComponentMode = false;
	private String  mCompomRunSignComponentModeError = null;
	private Boolean mCompomRunSignComponentModeCancelFromDNIe = false;
	private SharedPreferences sharedPrefs = null;
	private List<HeaderProperty> httpHeaders = null;
	private ArrayOfJobReference m_jobsPending = null;
	private ArrayOfJobReference m_jobsProcessed = null;
	private MobileBioSQSSignerActivity m_MainActivity = null;
	private Job m_JobToShow = null;
	private Object CriticalSection = new Object();

	// Singleton object to manage the instance
	private static AppHandler singleton;
	
	public AppHandler() 
	{
		// For SSL connections
		SSLConection.allowAllSSL();		
		AppContext = null;
		mainContext = null;
		uriFrontend = null;
		providerParameter = null;
		mCompomRunSignComponentMode = false;
		mCompomRunSignComponentModeError = null;
		mCompomRunSignComponentModeCancelFromDNIe = false;
		sharedPrefs = null;
	}

	public static AppHandler getInstance() 
	{
		if (singleton == null)
		{
			singleton = new AppHandler();
		}
	
		return singleton;
	}
	
	public static List<HeaderProperty> PrepareConnectionParameters()
	{
		return AppHandler.getInstance().getHttpHeaders();
	}
	
	
	public Activity getMainActivity() {
		return (Activity)mainContext;
	}

	public Context getMainContext() {
		return mainContext;
	}

	public void setMainContext(Context mainContext) {
		this.mainContext = mainContext;
	}

	public Context getAppContext() {
		return AppContext;
	}

	public void setAppContext(Context appContext) {
		AppContext = appContext;
	}

	public String getUriFrontend() {
		return uriFrontend;
	}

	public void setUriFrontend(String uriFrontend) {
		this.uriFrontend = uriFrontend;
	}

	public Boolean getmCompomRunSignComponentMode() {
		return mCompomRunSignComponentMode;
	}

	public void setmCompomRunSignComponentMode(
			Boolean mCompomRunSignComponentMode) {
		this.mCompomRunSignComponentMode = mCompomRunSignComponentMode;
	}

	public String getProviderParameter() {
		return providerParameter;
	}

	public void setProviderParameter(String providerParameter) {
		this.providerParameter = providerParameter;
	}

	public String getmCompomRunSignComponentModeError() {
		return mCompomRunSignComponentModeError;
	}

	public void setmCompomRunSignComponentModeError(
			String mCompomRunSignComponentModeError) {
		this.mCompomRunSignComponentModeError = mCompomRunSignComponentModeError;
	}

	public Boolean getmCompomRunSignComponentModeCancelFromDNIe() {
		return mCompomRunSignComponentModeCancelFromDNIe;
	}

	public void setmCompomRunSignComponentModeCancelFromDNIe(
			Boolean mCompomRunSignComponentModeCancelFromDNIe) {
		this.mCompomRunSignComponentModeCancelFromDNIe = mCompomRunSignComponentModeCancelFromDNIe;
	}


	public SharedPreferences getSharedPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this.getAppContext());
	}

	public void setSharedPrefs(SharedPreferences sharedPrefs) {
		this.sharedPrefs = sharedPrefs;
	}
	
	public List<HeaderProperty> getHttpHeaders() {
		if(httpHeaders==null)
		{
			// Add user and name for basic authentication
			List<HeaderProperty> headers = new ArrayList<HeaderProperty>(); 
			headers.add(new HeaderProperty("Connection", "keep-alive"));
			headers.add(new HeaderProperty("Authorization", "Basic " + 
	        	org.kobjects.base64.Base64.encode(
	        			(getSignatureServiceUserName() + ":" + getSignatureServicePassword()).getBytes())));
	    	return headers;
		}
		else
		{	    	
			return httpHeaders;
		}
	}
	
	public String getWsURL() {
		String wsurl = sharedPrefs.getString("URLSealSign", "");
		if (wsurl.toLowerCase().endsWith(".svc"))
			return wsurl;
		else
			return wsurl + "/" + signatureServiceSVC;
	}

	public ArrayOfJobReference getM_jobsPending() {
		synchronized (CriticalSection) {
			return m_jobsPending;
		}
	}

	public ArrayOfJobReference getM_jobsProcessed() {
		synchronized (CriticalSection) {
			return m_jobsProcessed;
		}
	}

	public void setM_jobsPending(ArrayOfJobReference m_jobsPending) {
		synchronized (CriticalSection) {
			this.m_jobsPending = m_jobsPending;
		}
	}

	public void setM_jobsProcessed(ArrayOfJobReference m_jobsProcessed) {
		synchronized (CriticalSection) {
			this.m_jobsProcessed = m_jobsProcessed;
		}
	}

	public BasicHttpBinding_ISignatureQueueServiceBasic getService() {
		Integer timeout = Integer.parseInt(sharedPrefs.getString("ServiceTimeout", "60"), 10)*1000;
		BasicHttpBinding_ISignatureQueueServiceBasic service = new BasicHttpBinding_ISignatureQueueServiceBasic(null, getWsURL(), timeout);
		service.httpHeaders = getHttpHeaders();
		return service;
	}

	public int getSignatureservicerefreshinterval() {
		return signatureServiceRefreshInterval;
	}

	public String getSignatureServiceUserName() {
		String userName = sharedPrefs.getString("UserSealSign", "");
		return userName;
	}

	public String getSignatureServicePassword() {
		String password = sharedPrefs.getString("PassSealSign", "");		
		return password;
	}

	public MobileBioSQSSignerActivity getM_MainActivity() {
		return m_MainActivity;
	}

	public void setM_MainActivity(MobileBioSQSSignerActivity m_MainActivity) {
		this.m_MainActivity = m_MainActivity;
	}

	public Job getM_JobToShow() {
		return m_JobToShow;
	}

	public void setM_JobToShow(Job m_JobToShow) {
		this.m_JobToShow = m_JobToShow;
	}
}
