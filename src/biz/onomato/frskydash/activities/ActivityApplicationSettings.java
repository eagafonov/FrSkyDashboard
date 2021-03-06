/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.onomato.frskydash.activities;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.util.ExportUtils;
import biz.onomato.frskydash.util.Logger;

public class ActivityApplicationSettings extends Activity implements OnClickListener, OnEditorActionListener, OnFocusChangeListener {

	private static final String TAG = "Application-Settings";
	private FrSkyServer server;
	//SharedPreferences settings;
	//SharedPreferences.Editor editor;
	
	private View btnDeleteLogs;
	private CheckBox chkCyclicSpeakerEnabled;
	private CheckBox chkLogToRaw; 
	private CheckBox chkLogToCsv; 
	private CheckBox chkLogToHuman;
	private CheckBox chkBtAutoEnable;
	private CheckBox chkBtAutoConnect;
	private CheckBox chkAutoSetVolume;
	private CheckBox chkBtAutoSendAlarms;
	private EditText edCyclicInterval;
	private Button btnSave;
    private SeekBar sbInitialMinimumVolume;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.activity_applicationsettings);

		// Setup components from screen
		Logger.i(TAG,"Setup widgets");
		btnDeleteLogs = findViewById(R.id.btnDeleteLogs);
		chkCyclicSpeakerEnabled = (CheckBox) findViewById(R.id.chkCyclicSpeakerEnabled);
		chkLogToRaw = (CheckBox) findViewById(R.id.chkLogToRaw); 
		chkLogToCsv = (CheckBox) findViewById(R.id.chkLogToCsv); 
		chkLogToHuman = (CheckBox) findViewById(R.id.chkLogToHuman); 
		chkBtAutoEnable = (CheckBox) findViewById(R.id.chkBtAutoEnable);
		chkBtAutoConnect = (CheckBox) findViewById(R.id.chkBtAutoConnect);
		chkBtAutoSendAlarms = (CheckBox) findViewById(R.id.chkBtAutoSendAlarms);
		chkAutoSetVolume = (CheckBox) findViewById(R.id.chkAutoSetVolume);
		sbInitialMinimumVolume = (SeekBar) findViewById(R.id.sbInitialMinimumVolume);
		edCyclicInterval = (EditText) findViewById(R.id.edCyclicSpeakerInterval);
		edCyclicInterval.setOnEditorActionListener(this);
		//edCyclicInterval.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_ACTION_UNSPECIFIED);
		btnSave = (Button) findViewById(R.id.btnSave);
		//eso: as long as all components forces a save, and navigating out forces a save, i dont need a save btn
		btnSave.setVisibility(View.GONE);
		
		// Add listeners
		Logger.i(TAG,"Add Listeners");
		btnDeleteLogs.setOnClickListener(this);
		chkCyclicSpeakerEnabled.setOnClickListener(this);
		chkLogToRaw.setOnClickListener(this);
		chkLogToCsv.setOnClickListener(this);
		chkLogToHuman.setOnClickListener(this);
		chkBtAutoEnable.setOnClickListener(this);
		chkBtAutoConnect.setOnClickListener(this);
		chkAutoSetVolume.setOnClickListener(this);
		chkBtAutoSendAlarms.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		//edCyclicSpeakerInterval.addTextChangedListener(this);
		edCyclicInterval.setOnFocusChangeListener(this);
		
		
		// Load settings
        //settings = getPreferences(MODE_PRIVATE);

		// export models option
		((Button) findViewById(R.id.btnExportModels))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						try {
							// TODO work with some fixed location for now
							// (outside
							// the app directory since it would be deleted upon
							// uninstall otherwise)
							ExportUtils.exportModelsToFile(FrSkyServer.modelMap
									.values(), new File(
									"/sdcard/frskydash/export-models.json"));
							Toast.makeText(getApplicationContext(),
									"Export completed", Toast.LENGTH_SHORT)
									.show();
							// TODO check if this doesn't take too much time,
							// otherwise in background thread
						} catch (Exception e) {
							Log.e(TAG, "Error on exporting models, message: "
									+ e.getMessage(), e);
							Toast.makeText(
									getApplicationContext(),
									"Error on exporting models, message: "
											+ e.getMessage(),
									Toast.LENGTH_SHORT).show();
						}
					}
				});
		
		// import models option
		((Button) findViewById(R.id.btnImportModels))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						try {
							// TODO work with some fixed location for now
							// (outside
							// the app directory since it would be deleted upon
							// uninstall otherwise)
							ExportUtils.importModelsFromFile(new File(
									"/sdcard/frskydash/export-models.json"));
							Toast.makeText(getApplicationContext(),
									"Import finished", Toast.LENGTH_SHORT)
									.show();
						} catch (Exception e) {
							Log.e(TAG, "Error on importing models, message: "
									+ e.getMessage(), e);
							Toast.makeText(
									getApplicationContext(),
									"Error on importing models, message: "
											+ e.getMessage(),
									Toast.LENGTH_SHORT).show();
						}
					}
				});

        doBindService();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		//test.setText(oAd1.toString());
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		save();
		
		//test.setText(oAd1.toString());
	}
	
	public void onClick(View v)
	{
		switch(v.getId())
		{
		///TODO: Replace with server setters
			case R.id.btnDeleteLogs:
				showDeleteDialog();
				break;
			case R.id.chkCyclicSpeakerEnabled:
				//Log.i(TAG,"Store cyclic speaker: ");
				//CheckBox chkCyclicSpeakerEnabled = (CheckBox) v;
				//editor.putBoolean("cyclicSpeakerEnabledAtStartup", chkCyclicSpeakerEnabled.isChecked());
				//editor.commit();
				server.setCyclicSpeechEnabledAtStartup(((CheckBox) v).isChecked());
				
				break;
			case R.id.chkLogToCsv:
				//Log.i(TAG,"Store Log to Csv ");
				//editor.putBoolean("logToCsv", ((CheckBox) v).isChecked());
				//editor.commit();
				server.setLogToCsv(((CheckBox) v).isChecked());
				break;
			case R.id.chkLogToRaw:
				//Log.i(TAG,"Store Log to Raw ");
				//editor.putBoolean("logToRaw", ((CheckBox) v).isChecked());
				//editor.commit();
				server.setLogToRaw(((CheckBox) v).isChecked());
				break;
			case R.id.chkLogToHuman:
				//Log.i(TAG,"Store Log to Human");
				//editor.putBoolean("logToHuman", ((CheckBox) v).isChecked());
				//editor.commit();
				server.setLogToHuman(((CheckBox) v).isChecked());
				break;
			case R.id.chkBtAutoEnable:
				//editor.putBoolean("btAutoEnable", ((CheckBox) v).isChecked());
				//editor.commit();
				server.setBtAutoEnable(((CheckBox) v).isChecked());
				break;
			case R.id.chkBtAutoConnect:
				//editor.putBoolean("btAutoConnect", ((CheckBox) v).isChecked());
				//editor.commit();
				server.setBtAutoConnect(((CheckBox) v).isChecked());
				break;
			case R.id.chkAutoSetVolume:
				//editor.putBoolean("autoSetVolume", ((CheckBox) v).isChecked());
				//editor.commit();
				server.setAutoSetVolume(((CheckBox) v).isChecked());
				sbInitialMinimumVolume.setEnabled(((CheckBox) v).isChecked());
				break;
			case R.id.chkBtAutoSendAlarms:
				server.setAutoSendAlarms(((CheckBox) v).isChecked());
				break;
			case R.id.btnSave:
				Logger.i(TAG,"Store new interval");
				save();
				break;
		}
	}
	
	private void save()
	{
		Logger.i(TAG,"Save current settings");
		try
		{
			server.setCyclicSpeechEnabledAtStartup(chkCyclicSpeakerEnabled.isChecked());
			server.setBtAutoEnable(chkBtAutoEnable.isChecked());
			server.setBtAutoConnect(chkBtAutoConnect.isChecked());
			server.setLogToCsv(chkLogToCsv.isChecked());
			server.setLogToRaw(chkLogToRaw.isChecked());
			server.setLogToHuman(chkLogToHuman.isChecked());
			server.setAutoSetVolume(chkAutoSetVolume.isChecked());
			server.setMinimumVolume(sbInitialMinimumVolume.getProgress());
			server.setCyclicSpeechInterval(Integer.parseInt(edCyclicInterval.getText().toString()));
			server.setAutoSendAlarms(chkBtAutoSendAlarms.isChecked());
			
			//getApplicationContext();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(edCyclicInterval.getWindowToken(), 0);
			
			Toast.makeText(this, "Saved...", Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			
		}
	}
	
	private void showDeleteDialog()
	{
		Logger.i(TAG,"Delete all logs please");
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setTitle("Delete Logs");

		dialog.setMessage("Do you really want to delete all logs?");
		
		dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Stop the activity
                server.deleteAllLogFiles();
            }

        });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Stop the activity
            	Logger.i(TAG,"Cancel Deletion");
            }

        });
        dialog.show();
	}
	
	void doBindService() {
		Logger.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Logger.i(TAG,"Try to bind to the service");
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
			Logger.i(TAG,"Bound to Service");
			
	        int interval = server.getCyclicSpeechInterval();
	        Logger.i(TAG,"Set interval to +"+interval);
	        Logger.i(TAG,"Edit field currently at +"+edCyclicInterval.getText().toString());
	        edCyclicInterval.setText(String.valueOf(interval));
	        //edCyclicInterval.setText(interval);
	        chkCyclicSpeakerEnabled.setChecked(server.getCyclicSpeechEnabledAtStartup());
	        chkLogToRaw.setChecked(server.getLogToRaw());
	        chkLogToHuman.setChecked(server.getLogToHuman());
	        chkLogToCsv.setChecked(server.getLogToCsv());
	        chkBtAutoEnable.setChecked(server.getBtAutoEnable());
	        chkBtAutoConnect.setChecked(server.getBtAutoConnect());
	        //chkAutoSetVolume.setChecked(settings.getBoolean("autoSetVolume", false));
	        
	        chkAutoSetVolume.setChecked(server.getAutoSetVolume());
	        chkBtAutoSendAlarms.setChecked(server.getAutoSendAlarms());

	        sbInitialMinimumVolume.setEnabled(chkAutoSetVolume.isChecked());
	        sbInitialMinimumVolume.setProgress(server.getMinimumVolume());
	        
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	@Override
	public boolean onEditorAction(TextView editView, int actionId, KeyEvent event) {
		// Detect "ENTER"
		if(actionId == EditorInfo.IME_NULL)
		{
			save();
		}
		return true;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		// TODO Auto-generated method stub
		save();
	}
	
}
