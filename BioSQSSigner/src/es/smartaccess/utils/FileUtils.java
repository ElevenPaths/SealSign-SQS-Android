package es.smartaccess.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.PatternSyntaxException;

import es.smartaccess.mobilebiosqssigner.AppHandler;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;


/**
 * File utility methods.
 *<P>
 * {@link #getFileNames(String, String, int)}, {@link #copyFile(File, File)} and {@link #copyFile(String, String, boolean)} are
 * adapted from the AndiCar project; you can see their version at
 * <A href="https://code.google.com/p/andicar/source/browse/src/org/andicar/persistence/FileUtils.java"
 *  >https://code.google.com/p/andicar/source/browse/src/org/andicar/persistence/FileUtils.java</A> .
 */
public class FileUtils
{
	public static final String rootDir = "MobileBIOSQSSigner";
	public static final String pendingDocs = "pendingDocs";
	public static final String sentDocs = "sentDocs";
	public static final String signedDocs = "signedDocs";
	public static final String tempDocs = "TempDocs";
	
  /**
   * Get a list of filenames in this folder.
   * @param folder  Full path of directory 
   * @param fileNameFilterPattern  Regular expression suitable for {@link String#matches(String)}, or null.
   *      See {@link java.util.regex.Pattern} for more details.
   * @param sort  Use 1 for {@link String#CASE_INSENSITIVE_ORDER}, 0 for no sort, -1 for reverse sort
   * @return list of filenames (the names only, not the full path),
   *     or <tt>null</tt> if <tt>folder</tt> doesn't exist or isn't a directory,
   *     or if nothing matches <tt>fileNameFilterPattern</tt>
   * @throws PatternSyntaxException if <tt>fileNameFilterPattern</tt> is non-null and isn't a
   *     valid Java regular expression
   */
    public static ArrayList<String> getFileNames
        (final String folder, final String fileNameFilterPattern, final int sort)
      throws PatternSyntaxException
    {
        ArrayList<String> myData = new ArrayList<String>();
        File fileDir = new File(folder);
        if(!fileDir.exists() || !fileDir.isDirectory()){
            return null;
        }

        String[] files = fileDir.list();

        if(files.length == 0){
            return null;
        }
        for (int i = 0; i < files.length; i++) {
            if(fileNameFilterPattern == null ||
                    files[i].matches(fileNameFilterPattern))
            myData.add(files[i]);
        }
        if (myData.size() == 0)
          return null;

        if (sort != 0)
        {
          Collections.sort(myData, String.CASE_INSENSITIVE_ORDER);
          if (sort < 0)
            Collections.reverse(myData);
        }

        return myData;
    }

    /**
     * Copy a file's contents.
     * @param fromFilePath  Full path to source file
     * @param toFilePath    Full path to destination file
     * @param overwriteExisting if true, toFile will be deleted before the copy
     * @return true if OK, false if couldn't copy (SecurityException, etc)
     * @throws IOException if an error occurred when opening, closing, reading, or writing;
     *     even after an exception, copyFile will close the files before returning.
     */
    public static boolean copyFile
      (String fromFilePath, String toFilePath, final boolean overwriteExisting)
      throws IOException
    {
        try{
            File fromFile = new File(fromFilePath);
            File toFile = new File(toFilePath);
            if(overwriteExisting && toFile.exists())
                toFile.delete();
            return copyFile(fromFile, toFile);
        }
        catch(SecurityException e){
        	e.printStackTrace();
            return false;
        }
    }

    /**
     * Copy a file's contents.
     *TODO per API lookup: FileOutputStream(file) will overwrite desti if exists
     * @param fromFilePath  Full path to source file; should not be open.
     * @param toFilePath    Full path to destination file; should not be open.
     * @return true if OK, false if couldn't copy (SecurityException, etc)
     * @throws IOException if an error occurred when opening, closing, reading, or writing;
     *     even after an exception, copyFile will close the files before returning.
     */
    @SuppressWarnings("resource")
	public static boolean copyFile(File source, File dest)
      throws IOException
    {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();

            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);

            out.write(buf);
            
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            return true;
        } 
        catch(IOException e){
        	e.printStackTrace();
          try {
              if (in != null)
                  in.close();
          } catch (IOException e2) {
        	  e2.printStackTrace();
          }
          try {
              if (out != null)
                  out.close();
          } catch (IOException e2) {
        	  e2.printStackTrace();
          }
          throw e;
        }
    }
    
    public static void writeFileToExternalStorage()
    {
    	try
    	{
    		String rootDir = Environment.getExternalStorageDirectory() + File.separator + FileUtils.rootDir;
    		File folder = new File(rootDir);
    		boolean success = true;
    		if (!folder.exists()) 
    		{
    		    success = folder.mkdir();
    		}
    		if (success) 
    		{
    			File f = new File(rootDir, "prueba_sd.txt");
    	    	 
        	    OutputStreamWriter fout =
        	        new OutputStreamWriter(
        	            new FileOutputStream(f));
        	 
        	    fout.write("Texto de prueba.");
        	    fout.close();
    		} 
    		else 
    		{
    		    // Do something else on failure 
    		}
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	    Log.e("Ficheros", "Error al escribir fichero a tarjeta SD");
    	}
    }

    public static boolean createDirs(String DNAME,Context context)
    {
    	String state = Environment.getExternalStorageState(); 
    
    	if (Environment.MEDIA_MOUNTED.equals(state)) 
    	{ 

    		try {
				File rootPath = new File(Environment.getExternalStorageDirectory(), DNAME);
				if (!rootPath.exists()) 
				{
					rootPath.mkdirs();
				}

				File pendingPath = new File(Environment.getExternalStorageDirectory(), DNAME+File.separator+pendingDocs);
				if (!pendingPath.exists()) 
				{
					pendingPath.mkdirs();
				}
				
				File sentPath = new File(Environment.getExternalStorageDirectory(), DNAME+File.separator+sentDocs);
				if (!sentPath.exists()) 
				{
					sentPath.mkdirs();
				}
				
				File signedPath = new File(Environment.getExternalStorageDirectory(), DNAME+File.separator+signedDocs);
				if (!signedPath.exists()) 
				{
					signedPath.mkdirs();
				}
				
				File tempPath = new File(Environment.getExternalStorageDirectory(), DNAME+File.separator+tempDocs);
				if (!tempPath.exists()) 
				{
					tempPath.mkdirs();
				}
			} 
    		catch (Exception e) 
    		{
				e.printStackTrace();
				return false;
			}

    	} 
    	else 
    	{ 
    		try 
    		{
    			File pendingDocs = context.getApplicationContext().getDir(FileUtils.pendingDocs, Context.MODE_PRIVATE);
    			pendingDocs.mkdirs();
    			
    			File sentDocs = context.getApplicationContext().getDir(FileUtils.sentDocs, Context.MODE_PRIVATE);
    			sentDocs.mkdirs();
    			
    			File signedDocs = context.getApplicationContext().getDir(FileUtils.signedDocs, Context.MODE_PRIVATE);
    			signedDocs.mkdirs();
    			
    			File tempDocs = context.getApplicationContext().getDir(FileUtils.tempDocs, Context.MODE_PRIVATE);
    			tempDocs.mkdirs();
    		} 
    		catch (Exception e) 
    		{
    			e.printStackTrace();
    			return false;
    		}
    	}
    	return true;
    }
    
    public static String getFilePathByUri(Uri uri)
    {
        String fileName=null;//default fileName
        Uri filePathUri = uri;
        if (uri.getScheme().toString().compareTo("content")==0)
        {      
        	Cursor cursor;
        	CursorLoader loader = new CursorLoader(AppHandler.getInstance().getAppContext(), uri, null, null, null, null);
            cursor = loader.loadInBackground();    
            if (cursor.moveToFirst())
            {
                int column_index = cursor.getColumnIndexOrThrow("_data");//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                filePathUri = Uri.parse(cursor.getString(column_index));
                fileName = filePathUri.getPath();
            }
        }
        else if (uri.getScheme().compareTo("file")==0)
        {
            fileName = filePathUri.getPath();
        }
        else
        {
            fileName = fileName+"_"+filePathUri.getPath();
        }
        return fileName;
    }
    
	public static void writeToFile(byte[] array, String outputFilePath) 
	{ 
		try 
		{ 			
			@SuppressWarnings("resource")
			FileOutputStream stream = new FileOutputStream(outputFilePath); 
			try 
			{
				stream.write(array);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
		} 
		catch (FileNotFoundException e1) 
		{ 
			e1.printStackTrace(); 
		} 
	} 
	
	public static String getTempDir()
	{
		String path  = Environment.getExternalStorageDirectory() + File.separator + rootDir + File.separator + tempDocs + File.separator;
		return path;
	}
}