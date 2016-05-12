package es.smartaccess.mobilebiosqssigner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


import es.smartaccess.mobilebiosqssigner.R;
import es.smartaccess.mobilebiosqssigner.MobileBioSQSSignerActivity.DocsSectionFragment;
 
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class DocAdapter extends BaseAdapter implements Filterable{
	private Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private ArrayList<HashMap<String, String>> filteredData;
    private static LayoutInflater inflater=null;
    private boolean multichoice = false;
    boolean[] checkBoxState;
    boolean flagAll = false;
    boolean flagAllAs = false;


 
    public DocAdapter(Activity a, ArrayList<HashMap<String, String>> d, boolean multichoice) {
    	this.activity = a;
    	this.data=d;
    	this.filteredData = d;
    	this.inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.multichoice = multichoice;
        checkBoxState=new boolean[d.size()];
    }
 
    public int getCount() {
        return filteredData.size();
    }
 
    public Object getItem(int position) {
        return filteredData.get(position);
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(final int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null){
        	if(multichoice)
        		vi = inflater.inflate(R.layout.list_row_mchoice, null);
        	else vi = inflater.inflate(R.layout.list_row, null);
        }
 
        TextView docTitle = (TextView)vi.findViewById(R.id.DetailTitle); // title
        TextView docDate = (TextView)vi.findViewById(R.id.stateTitle); //date
        TextView docSize = (TextView)vi.findViewById(R.id.size); // size
        TextView docQueue = (TextView)vi.findViewById(R.id.QueueName); // size
        TextView docComputer = (TextView)vi.findViewById(R.id.ComputerName); // size
        ImageView doc_image=(ImageView)vi.findViewById(R.id.list_image); // thumb image
 
        HashMap<String, String> document = new HashMap<String, String>();
        document = filteredData.get(position);
 
        // Setting all values in listview
        docTitle.setText(document.get(DocsSectionFragment.KEY_TITLE));
        docDate.setText(document.get(DocsSectionFragment.KEY_DATE));
        docSize.setText(document.get(DocsSectionFragment.KEY_OWNER));
        docQueue.setText(document.get(DocsSectionFragment.KEY_QUEUENAME));
        docComputer.setText(document.get(DocsSectionFragment.KEY_COMPUTERNAME));
        
        int docimage_id = R.drawable.no_image;
        if(document.get(DocsSectionFragment.KEY_IMAGE).compareTo("pdf") == 0){
        	docimage_id = R.drawable.pdf;
        }
        else{
			docimage_id = R.drawable.no_image;
		}

        doc_image.setImageResource(docimage_id);
        
        if(multichoice){
        	CheckBox chk = (CheckBox) vi.findViewById(R.id.checkedTextView);
        	if(flagAll){
        		for (int i=0;i<checkBoxState.length;i++){
        			checkBoxState[i]=flagAllAs;
        			chk.setChecked(flagAllAs);
        		}
        		flagAll = false;
        	}
        	else
        		chk.setChecked(checkBoxState[position]);
        	
        	
        	chk.setOnClickListener(new View.OnClickListener() {

        		public void onClick(View v) {

        			if(((CheckBox)v).isChecked())
        				checkBoxState[position]=true;

        			else
        				checkBoxState[position]=false;


        		}
        	});
        }

        return vi;
    }

    @Override
    public Filter getFilter()
    {
       return new Filter()
       {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence)
            {
                FilterResults results = new FilterResults();

                if(charSequence == null || charSequence.length() == 0)
                {
                    results.values = data;
                    results.count = data.size();
                }
                else
                {
                    ArrayList<HashMap<String,String>> filterResultsData = new ArrayList<HashMap<String,String>>();

                    for(HashMap<String,String> mydata : data)
                    {

                        if(mydata.get("title").toLowerCase(Locale.getDefault()).contains(charSequence.toString().toLowerCase()))
                        {
                            filterResultsData.add(mydata);
                        }
                    }            

                    results.values = filterResultsData;
                    results.count = filterResultsData.size();
                }

                return results;
            }

            @SuppressWarnings("unchecked")
			@Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults)
            {
                filteredData = (ArrayList<HashMap<String,String>>)filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
    

}
