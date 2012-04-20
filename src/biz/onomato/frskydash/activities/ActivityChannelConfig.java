package biz.onomato.frskydash.activities;

import java.util.ArrayList;
import java.util.List;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.FrSkyServer.MyBinder;
import biz.onomato.frskydash.R.id;
import biz.onomato.frskydash.R.layout;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.hub.FrSkyHub;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class ActivityChannelConfig extends Activity implements OnClickListener {
	private static final String TAG = "ChannelConfig";
	//private static final boolean DEBUG=true;
	private long _channelId = -1;
	//private int _idInModel = -1;
	private int _modelId=-1;
	private Model _model;
	private FrSkyServer server;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	private Channel channel;
	private TextView tvName;
	private EditText edDesc,edUnit,edShortUnit,edOffset,edFactor,edPrecision,edMovingAverage;
	private CheckBox chkSpeechEnabled;
	private Spinner spSourceChannel;
	private Button btnSave;
	private boolean first=true;
	
	//chConf_edVoice
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		doBindService();
		
		Intent launcherIntent = getIntent();
		try
		{
			channel = launcherIntent.getParcelableExtra("channel");
			//_idInModel = launcherIntent.getIntExtra("idInModel", -1);
			_channelId = channel.getId();
			_modelId = channel.getModelId();
			if(FrSkyServer.D)Log.d(TAG,"Channel config launched with attached channel: "+channel.getDescription());
			if(FrSkyServer.D)Log.d(TAG,"channel context is: "+channel.getContext());
			//channel.setContext(getApplicationContext());
			if(FrSkyServer.D)Log.d(TAG,"channel context is: "+channel.getContext());

		}
		catch(Exception e)
		{
			if(FrSkyServer.D)Log.d(TAG,"Channel config launched without attached channel");
			channel = null;
			_channelId = launcherIntent.getIntExtra("channelId", -1);
			_modelId = -1;
			
		}
		
		//_modelId = launcherIntent.getIntExtra("modelId", -1);
		//if(DEBUG)Log.d(TAG, "working model has id: "+_modelId);
		
		if(FrSkyServer.D)Log.d(TAG, "Channel Id is: "+_channelId);
		
		// Show the form
		setContentView(R.layout.activity_channelconfig);

		// Find all form elements
		spSourceChannel		= (Spinner)  findViewById(R.id.chConf_spSourceChannel);
		edDesc 				= (EditText) findViewById(R.id.chConf_edDescription);
		edUnit 				= (EditText) findViewById(R.id.chConf_edUnit);
		edShortUnit			= (EditText) findViewById(R.id.chConf_edShortUnit);
		edOffset 			= (EditText) findViewById(R.id.chConf_edOffset);
		edFactor 			= (EditText) findViewById(R.id.chConf_edFactor);
		edPrecision 		= (EditText) findViewById(R.id.chConf_edPrecision);
		edMovingAverage 	= (EditText) findViewById(R.id.chConf_edMovingAverage);
		chkSpeechEnabled 	= (CheckBox) findViewById(R.id.chConf_chkSpeechEnabled);
		
		btnSave				= (Button) findViewById(R.id.chConf_btnSave);
		
		
		btnSave.setOnClickListener(this);
	
	}
	
	
	void doBindService() {
		if(FrSkyServer.D)Log.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		if(FrSkyServer.D)Log.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
    }
    
    void doUnbindService() {
            if (server != null) {
            // Detach our existing connection.
	        	try {
	        		unbindService(mConnection);
	        	}
	        	catch (Exception e)
	        	{}
        }
    }
    
    
    

    
    private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			server = ((FrSkyServer.MyBinder) binder).getService();
			if(FrSkyServer.D)Log.i(TAG,"Bound to Service");
			if(FrSkyServer.D)Log.i(TAG,"Fetch channel "+_channelId+" from Server");
			
			settings = server.getSettings();
	        editor = settings.edit();
	        
	        
	        if(_modelId==-1)
			{
	        	if(FrSkyServer.D)Log.d(TAG,"Configure new Model object");
				//_model = new Model(getApplicationContext());
				_model = server.getCurrentModel();
			}
			else
			{
				if(FrSkyServer.D)Log.d(TAG,"Configure existing Model object (id:"+_modelId+")");
				//_model = new Model(getApplicationContext());
				//_model.loadFromDatabase(_modelId);
				//_model = FrSkyServer.database.getModel(_modelId);
				_model = FrSkyServer.modelMap.get(_modelId);
			}
	        
			
	        if(channel!=null)
	        {
	        	ArrayList<Channel> sourceChannels = _model.getAllowedSourceChannels();
	        	
	        	/**
	        	 * Prototype Hub support
	        	 * eso
	        	 * 
	        	 * Add channel list from Hub to source list
	        	 * set this to false to hide hub channels
	        	 */
	        	if(true)
	        	{
		        	for(Channel ch : FrSkyHub.getInstance().getSourceChannels().values())
		        	{
		        		sourceChannels.add(ch);
		        	}
	        	}
	        	
	        	int n =0;
	        	
   	
	        	/**
	        	 * remove self from list
	        	 */
	        	sourceChannels.remove(channel);

	        	ArrayAdapter<Channel> channelDescriptionAdapter  = new ArrayAdapter<Channel> (getApplicationContext(),android.R.layout.simple_spinner_item,sourceChannels);
				channelDescriptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				
				spSourceChannel.setAdapter(channelDescriptionAdapter);
				
				
				// eso: set correct startup channel
				long len = channelDescriptionAdapter.getCount();
				for(int i=0;i<len;i++)
				{
					Channel c = (Channel) spSourceChannel.getItemAtPosition(i);
					if(c.getId()==channel.getSourceChannelId())
					{
						spSourceChannel.setSelection(i);
						break;
					}
				}
				
				spSourceChannel.setOnItemSelectedListener(new OnItemSelectedListener() {
				    @Override
				    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				        // your code here
				    	if(first)
				    	{
				    		first = false;
				    	}
				    	else
				    	{
					    	if(parentView.getSelectedItem()instanceof Channel)
					    	{
					    		Channel channel = (Channel) parentView.getSelectedItem();
					    		if(FrSkyServer.D)Log.d(TAG,"User selecte channel "+channel+" the units are: "+channel.getLongUnit());
					    		String longUnit = channel.getLongUnit();
					    		String shortUnit = channel.getShortUnit();
					    		
					    		if(longUnit.length()>0)
					    		{
					    			edUnit.setText(longUnit);
					    		}
					    		
					    		if(shortUnit.length()>0)
					    		{
					    			edShortUnit.setText(shortUnit);
					    		}
					    	}
				    	}
				    }

				    @Override
				    public void onNothingSelected(AdapterView<?> parentView) {
				        // your code here
				    }

				});
				
				edDesc.setText(channel.getDescription());
				edUnit.setText(channel.getLongUnit());
				edShortUnit.setText(channel.getShortUnit());
				edOffset.setText(Float.toString(channel.getOffset()));
				//edFactor.setText(Double.toString(channel.getFactor()));
				edFactor.setText(Float.toString(channel.getFactor()));
				edPrecision.setText(Integer.toString(channel.getPrecision()));
				edMovingAverage.setText(Integer.toString(channel.getMovingAverage()));
				chkSpeechEnabled.setChecked(channel.getSpeechEnabled());
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	
	
	
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.chConf_btnSave:
				Log.i(TAG,"Apply settings to channel: "+_channelId);
				applyChannel();
				
				
				// Enable the listener:
				//channel.registerListener();

				
				//Intent i = new Intent(getApplicationContext(), ActivityModelConfig.class);
	    		//i.putExtra("channelId", 1);
				Intent i = new Intent();
				i.putExtra("channel", channel);
				//i.putExtra("idInModel",_idInModel);
				
				if(FrSkyServer.D)Log.i(TAG,"Go back to dashboard");
				if(server.getCurrentModel().getId() == channel.getModelId())
				{
					//if(DEBUG) Log.d(TAG,"This is the current model, please replace run setChannel ");
					//server.getCurrentModel().setChannel(_idInModel,channel);
					//server.getCurrentModel().setChannel(channel);
				}
				else
				{
					//if(DEBUG) Log.d(TAG,"This is NOT the current model");
				}
				if(FrSkyServer.D)Log.d(TAG,"Sending Parcelled channel back: Description:"+channel.getDescription()+", silent: "+channel.getSilent());
				this.setResult(RESULT_OK,i);
				
				this.finish();
				break;
		}
	}
	
	private void applyChannel()
	{
		if(FrSkyServer.D)Log.i(TAG,"Apply the settings");
		
		int prec = Integer.parseInt(edPrecision.getText().toString());
		channel.setPrecision(prec);
		
		float fact = Float.valueOf(edFactor.getText().toString());
		channel.setFactor(fact);
		
		float offs = Float.valueOf(edOffset.getText().toString());
		channel.setOffset(offs);

		channel.setLongUnit(edUnit.getText().toString());
		channel.setShortUnit(edShortUnit.getText().toString());
		
		channel.setDescription(edDesc.getText().toString());
		
		channel.setSpeechEnabled(chkSpeechEnabled.isChecked());
		
		//needs to be done last to clean out "buffer"
		int ma = Integer.parseInt(edMovingAverage.getText().toString());
		channel.setMovingAverage(ma);
		
		Channel c = (Channel) spSourceChannel.getSelectedItem();
		if(FrSkyServer.D)Log.d(TAG,"Try to set source channel to:"+c.toString()+" (ID: "+c.getId()+")");
		channel.setSourceChannel(c);
		
		//channel.setDirtyFlag(true);
		
		//Save to regular persistant settings only if this is a "raw/server" channel
//		if(_channelId>-1)
//		{
//			//TODO: remove at some point
//			if(DEBUG) Log.d(TAG,"This is a server channel, save settings to persistant store (not database)");
//			channel.saveToConfig(settings);
//		}
//		else
//		{
		if(FrSkyServer.D)Log.d(TAG,"This is a model channel for modelId: "+channel.getModelId());
		if(channel.getModelId()>-1)
		{
			if(FrSkyServer.D)Log.d(TAG,"This is an existing model, feel free to save");
			//channel.saveToDatabase();
			//FrSkyServer.database.saveChannel(channel);
			channel.registerListener();
			FrSkyServer.modelMap.get(channel.getModelId()).setChannel(channel);
			FrSkyServer.saveChannel(channel);
			//or SAVE_MODEL, modelId
		}
		else
		{
			if(FrSkyServer.D)Log.d(TAG,"This is a new model, delay saving");
		}
	}
}
