/**
 * Copyright 2011	Chen Deng
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.androme.achat;
 
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.androme.achat.MyService.LocalBinder;

/**
 * AChatActivity is the only activity in this application.
 * It contains two inner classes: AndromeServer and AndromeServerHandler.
 * AChatActivity handles all UI components, and starts AndromeServer.
 * 
 * @author Chen Deng
 * @version 0.1.0
 * 
 */
public class AChatActivity extends Activity {
	
	private Intent intent;
	private MyService mService;
	private boolean mBound = false;
	
	private Button send;
	private Button help;
	private Button changePort;
	private EditText message;
	private static TextView messageBoard;
	private static TextView link;
	private static ScrollView scroll;
    
    private static final int DIALOG_ID_NOWIFI = 0;
    private static final int DIALOG_ID_CHANGEPORT = 1;
    private static final int DIALOG_ID_EXIT = 2;
    private static final int DIALOG_ID_INVALIDPORT = 3;
    private static boolean fromWiFiSettings = false; 
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        send = (Button) findViewById(R.id.send);
        help = (Button) findViewById(R.id.help);
        message = (EditText) findViewById(R.id.message);
        changePort = (Button) findViewById(R.id.changePort);
        messageBoard = (TextView) findViewById(R.id.log);
        link = (TextView) findViewById(R.id.link);
        messageBoard.setLineSpacing(0, (float) 1.5);
        scroll = (ScrollView) findViewById(R.id.ScrollView01);
        
        // start server on default port upon application entry
        try{
        	intent = new Intent(this, MyService.class);
    		startService(intent);
    		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    		if(mBound){
    			mService.setHandler(msgHandler);
    		}
    		else{
    			writeToMessageBoard("unbound", "sys");
    		}
        }
        catch(Exception e){}

        send.setOnClickListener(new OnClickListener(){
        	public void onClick(View v){
        		try{
	        		if(!message.getText().toString().equals("")){
		        		mService.inputMsg += message.getText();
		        		writeToMessageBoard(message.getText().toString(), "ME");
		        		message.setText("");
	        		}
        		}
	        	catch(Exception e){}
        	}
        });
        
        message.setOnKeyListener(new OnKeyListener(){
        	public boolean onKey(View v, int keyCode, KeyEvent event) {
        		try{
	        		if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
	        			if(!message.getText().toString().equals("")){
	        				mService.inputMsg += message.getText();
	    	        		writeToMessageBoard(message.getText().toString(), "ME");
	    	        		message.setText("");
	            		}
	            		return true;
	        		}
        		}
	        	catch(Exception e){}
	        	return false;
        	}
        });
        
        help.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		try{
        			Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://code.google.com/p/androme-vision/wiki/AChatManual?ts=1304189102&updated=AChatManual"));
        			startActivity(browserIntent);
        		}
        		catch(Exception e){}
        	}
        });
        
        changePort.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
                try{
                	showDialog(DIALOG_ID_CHANGEPORT);
                }
                catch(Exception e){}
            }
        });
        
        try{
        	link.setMovementMethod(LinkMovementMethod.getInstance());
        	link.setText(Html.fromHtml("<a href=\"http://code.google.com/p/androme-vision/\">Androme-Vision Project</a>"));
        }
        catch(Exception e){} 
    }//end of onCreate()
    
    /**
     * msgHandler is used by inner classes to access messageBoard component.
     */
    final static Handler msgHandler = new Handler() {
		
    	@Override
		public void handleMessage(Message msg) {
    		try{
				Bundle b = msg.getData();
				writeToMessageBoard(b.getString("msg"), b.getString("user"));
    		}
    		catch(Exception e){}
		}
    };
    
    /**
     * Ask for confirmation upon exit. Will become necessary in later implementations.
     */
    public void onBackPressed() {
    	showDialog(DIALOG_ID_EXIT);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //intent = new Intent(AChatActivity.this, myService.class);
        unbindService(mConnection);
		stopService(intent);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	try{
	    	if(mService.hasWiFi == true && fromWiFiSettings == false){
	    		mService.checkWiFi();
	    	}
	    	else{
	    		mService.startAndromeServer(mService.port);
	    		fromWiFiSettings = true;
	    	}
    	}
    	catch(Exception e){}
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (MyService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    /**
     * Contains five dialogs:
     * 	0. no Wi-Fi alert 
     * 	1. change port prompt
     * 	2. exit	confirmation
     * 	3. invalid port number alert
     */
    @Override
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder;
    	AlertDialog alert;
    	
        switch (id) {

        case DIALOG_ID_NOWIFI:
        	builder = new AlertDialog.Builder(this);
        	builder.setTitle(" Alert")
        		   .setCancelable(false)
        		   .setMessage("Wi-Fi network unavailable.")
        		   .setPositiveButton("Wi-Fi settings", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   fromWiFiSettings = true;
        	        	   startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));   
        	           }
        		   })
        		   .setNeutralButton("Retry", new DialogInterface.OnClickListener() {
        			   public void onClick(DialogInterface dialog, int id) {
        					   showDialog(DIALOG_ID_CHANGEPORT);
        			   }
        		   })
        		   .setNegativeButton("Quit A-Chat", new DialogInterface.OnClickListener() {
        			   public void onClick(DialogInterface dialog, int id) {
        				   AChatActivity.this.finish();
        			   }
        		   });	   
        	alert = builder.create();
        	return alert;
        }
        switch (id) {	
        case DIALOG_ID_CHANGEPORT:
        	builder = new AlertDialog.Builder(this);
        	final EditText newPort = new EditText(this);
        	newPort.setSingleLine(true);
        	newPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        	newPort.setText(mService.port+"");
        	
        	builder.setMessage("Please enter the new port: ")
        	       .setCancelable(false)
        	       .setView(newPort)
        	       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   mService.stopAndromeServer();
        	        	   try{
        	        		   mService.port = Integer.parseInt(newPort.getText().toString());
        	        		   if(mService.port < 1024 || mService.port > 65535){
        	        			   showDialog(DIALOG_ID_INVALIDPORT);
        	        		   }
        	        		   else{
        	        			   mService.startAndromeServer(new Integer(mService.port));
        	        		   }
        	        	   }
        	        	   catch(Exception e){
        	        		   showDialog(DIALOG_ID_INVALIDPORT);
        	        	   }
        	           }
        	       })
        	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   if(mService.checkWiFi() != 0){
        	        		   dialog.cancel();
        	        	   }
        	        	   else{
        	        		   showDialog(DIALOG_ID_NOWIFI);
        	        	   }
        	           }
        	       });
        	alert = builder.create();
        	return alert;
        	
        case DIALOG_ID_EXIT:
        	builder = new AlertDialog.Builder(this);
        	builder.setMessage("Are you sure you want to exit?")
        	       .setCancelable(true)
        	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                AChatActivity.this.finish();
        	           }
        	       })
        	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	alert = builder.create();
        	return alert;
        
        case DIALOG_ID_INVALIDPORT:
        	builder = new AlertDialog.Builder(this);
        	builder.setTitle(" Alert")
        		   .setMessage("Range of valid port number is 1024~65535.")
        		   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   showDialog(DIALOG_ID_CHANGEPORT);
       	           }
       	       });
        	alert = builder.create();
        	return alert;	
        }
        return null;
    }//end of onCreateDialog
    
    public static void writeToMessageBoard( String s , String user) {
    	if(!user.equals("")){
    		user += ": ";
    	}
    	messageBoard.setText(user + s + "\n" + messageBoard.getText().toString());
    	// always tracks latest message
    	scroll.fullScroll(ScrollView.FOCUS_UP);
    }
    
}//end of AChatActivity class