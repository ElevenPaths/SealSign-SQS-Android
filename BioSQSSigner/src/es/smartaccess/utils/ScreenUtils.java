package es.smartaccess.utils;

import es.CNT;
import es.smartaccess.mobilebiosqssigner.AppHandler;
import es.smartaccess.mobilebiosqssigner.R;
import es.smartaccess.sealsignbss.SealSignBSSConstants;
import es.smartaccess.sealsignbss.SealSignBSSPanel;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class ScreenUtils {

	public ScreenUtils() {
		// TODO Auto-generated constructor stub
	}
	
	private static int getScreenWidthInPixels()
    {
        Resources resources = AppHandler.getInstance().getM_MainActivity().getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int lSize = 0;
        
        double screenWidthInPixels = (double)config.screenWidthDp * dm.density;
        lSize = (int)(screenWidthInPixels + .5);
        return lSize;
    }

    private static int getScreenHeightInPixels()
    {
        Resources resources = AppHandler.getInstance().getM_MainActivity().getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int lSize = 0;
        
        double screenWidthInPixels = (double)config.screenWidthDp * dm.density;
        double screenHeightInPixels = screenWidthInPixels * dm.heightPixels / dm.widthPixels;
        lSize = (int)(screenHeightInPixels + .5);
        return lSize;
    }
    
    public static Intent getBSSPanelIntent()
    {
    	// Dimensions horizontal
        int AnchoP = (int)( getScreenWidthInPixels() * 0.90 );
        int AltoP =  (int)( getScreenHeightInPixels() * 0.35 );
        
        // Dimensions vertical
        int AnchoL = (int)( getScreenWidthInPixels() * 0.60 );
        int AltoL =  (int)( getScreenHeightInPixels() * 0.50 );
        
        Intent myIntent = new Intent(AppHandler.getInstance().getMainContext(), SealSignBSSPanel.class);
		myIntent.putExtra(SealSignBSSConstants.TRANSPARENT_SIGNATURE, true);
		myIntent.putExtra(SealSignBSSConstants.BIOMETRIC_REQUEST_IMAGE, true);
	    myIntent.putExtra(SealSignBSSConstants.BUTTON_UPPER_MARGIN, 10); // Default value = 6
	    myIntent.putExtra(SealSignBSSConstants.BUTTON_SIDE_MARGIN, 10); // Default value = 4
	    myIntent.putExtra(SealSignBSSConstants.BOTTOM_MARGIN, 15); // Default value =  5
	    myIntent.putExtra(SealSignBSSConstants.BUTTON_M_SAVE_TEXT, AppHandler.getInstance().getMainContext().getString(R.string.bsspanel_save_text)); // Default value = "Salvar"
	    myIntent.putExtra(SealSignBSSConstants.BUTTON_M_CLEAR_TEXT, AppHandler.getInstance().getMainContext().getString(R.string.bsspanel_clear_text)); // Default value = "Borrar"
	    myIntent.putExtra(SealSignBSSConstants.DIALOG_TITLE_TEXT, AppHandler.getInstance().getMainContext().getString(R.string.progress_bsspanel_save_title)); // Default value = "Salvando firma"
	    myIntent.putExtra(SealSignBSSConstants.DIALOG_MSG_TEXT, AppHandler.getInstance().getMainContext().getString(R.string.progress_bsspanel_save_text)); // Default value = "Por favor, espere..."
	    myIntent.putExtra(SealSignBSSConstants.DATE_TEXT, new java.util.Date().toString());
	    myIntent.putExtra(SealSignBSSConstants.WIDTH_PORT, AnchoP ); 
        myIntent.putExtra(SealSignBSSConstants.HEIGHT_PORT, AltoP ); 
        myIntent.putExtra(SealSignBSSConstants.WIDTH_LAND, AnchoL ); 
        myIntent.putExtra(SealSignBSSConstants.HEIGHT_LAND, AltoL ); 
        myIntent.putExtra(SealSignBSSConstants.BUTTON_UPPER_MARGIN, 10);
        myIntent.putExtra(SealSignBSSConstants.BUTTON_SIDE_MARGIN, 10);
        
        return myIntent;
    }
}
