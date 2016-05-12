package es.smartaccess.mobilebiosqssigner;

/**
 * Events for blocking runnable executing on UI thread
 * 
 * @author 
 *
 */
public interface BlockingOnUIRunnableListener
{

    /**
     * Code to execute on UI thread
     */
    public void onRunOnUIThread();
}
