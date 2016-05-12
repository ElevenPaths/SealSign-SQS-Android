package es.smartaccess.mobilebiosqssigner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.Period;

import es.smartaccess.SealSignSQSService.wcf.Job;

public class JobCache {
	
	public static Integer CacheTimeOut = 5; //Minutes
	private static JobCache singleton;
	
	class CacheObject
	{
		private Job _job;
		private DateTime _time;
		
		public CacheObject(Job job, DateTime time) {
			_job = job;
			_time = time;
		}
		
		public Job get_job() {
			return _job;
		}
		public DateTime get_time() {
			return _time;
		}
	}
	
	private HashMap<Integer, CacheObject> m_cache = null; 

	public JobCache() {
		m_cache = new HashMap<Integer, CacheObject>();
	}
	
	public static JobCache getInstance() 
	{
		if (singleton == null)
		{
			singleton = new JobCache();
		}
	
		return singleton;
	}
	
	public Boolean ExistInCache(Job job)
	{
		RemoveInvalidCacheItems();
		return m_cache.containsKey(job.jobReferenceEx.id);
	}
	
	public Boolean ExistInCache(Integer id)
	{
		RemoveInvalidCacheItems();
		return m_cache.containsKey(id);
	}
	
	public void AddToCache(Job job)
	{
		RemoveInvalidCacheItems();
		m_cache.put(job.jobReferenceEx.id, new CacheObject(job, DateTime.now()));
	}
	
	public void RemoveFromCache(Job job)
	{
		RemoveInvalidCacheItems();
		m_cache.remove(job.jobReferenceEx.id);
	}
	
	public void RemoveFromCache(Integer id)
	{
		RemoveInvalidCacheItems();
		m_cache.remove(id);
	}
	
	public Job GetFromCache(Job job)
	{
		RemoveInvalidCacheItems();
		return m_cache.get(job.jobReferenceEx.id).get_job();
	}
	
	public Job GetFromCache(Integer id)
	{
		RemoveInvalidCacheItems();
		return m_cache.get(id).get_job();
	}
	
	public void RemoveInvalidCacheItems()
	{
		Iterator<Entry<Integer,CacheObject>> it = m_cache.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<Integer, CacheObject> pairs = it.next();
	    	CacheObject cacheObject = (CacheObject)pairs.getValue();
	    	Period timeSpan = new Period(cacheObject.get_time(), DateTime.now());
	    	if(timeSpan.getMinutes()>=CacheTimeOut)
	    	{
	    		it.remove();
	    	}
	    }
	}
	
	public void RemoveCacheItems()
	{
		m_cache.clear();
	}
	
	public void InvalidateEntry(Integer id)
	{
		RemoveFromCache(id);
	}
	
	public void InvalidateEntry(Job job)
	{
		RemoveFromCache(job);
	}
}
