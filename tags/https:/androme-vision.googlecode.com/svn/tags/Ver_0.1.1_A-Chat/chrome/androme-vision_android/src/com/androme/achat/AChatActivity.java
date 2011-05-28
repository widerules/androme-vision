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
 
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
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
	
	private AndromeServer server;
	
	private Button send;
	private Button help;
	private Button changePort;
	private EditText message;
	private static TextView messageBoard;
	private static TextView link;
	private static ScrollView scroll;
    
	private static String ipAddress;
    private static String inputMsg = "";
    private static int port = 8080;
    private static boolean hasWiFi = false;
    private static final int DIALOG_ID_NOWIFI = 0;
    private static final int DIALOG_ID_CHANGEPORT = 1;
    private static final int DIALOG_ID_EXIT = 2;
    private static final int DIALOG_ID_INVALIDPORT = 3;
    private static final int MSGBUFFER_SIZE = 4096;
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
        	startAndromeServer(port);
        }
        catch(Exception e){}
        
        send.setOnClickListener(new OnClickListener(){
        	public void onClick(View v){
        		try{
	        		if(!message.getText().toString().equals("")){
		        		inputMsg += message.getText();
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
	    	        		inputMsg += message.getText();
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
        stopAndromeServer();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	try{
	    	if(hasWiFi == true && fromWiFiSettings == false){
	    		checkWiFi();
	    	}
	    	else{
	    		startAndromeServer(port);
	    		fromWiFiSettings = true;
	    	}
    	}
    	catch(Exception e){}
    }
    
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
        	newPort.setText(port+"");
        	
        	builder.setMessage("Please enter the new port: ")
        	       .setCancelable(false)
        	       .setView(newPort)
        	       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   stopAndromeServer();
        	        	   try{
        	        		   port = Integer.parseInt(newPort.getText().toString());
        	        		   if(port < 1024 || port > 65535){
        	        			   showDialog(DIALOG_ID_INVALIDPORT);
        	        		   }
        	        		   else{
        	        			   startAndromeServer(new Integer(port));
        	        		   }
        	        	   }
        	        	   catch(Exception e){
        	        		   showDialog(DIALOG_ID_INVALIDPORT);
        	        	   }
        	           }
        	       })
        	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   if(checkWiFi() != 0){
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
    
    private void stopAndromeServer() {
    	if( server != null ) {
    		server.stopServer();
    		server.interrupt();
    	}
    }
    
    public static void writeToMessageBoard( String s , String user) {
    	if(!user.equals("")){
    		user += ": ";
    	}
    	messageBoard.setText(user + s + "\n" + messageBoard.getText().toString());
    	// always tracks latest message
    	scroll.fullScroll(ScrollView.FOCUS_UP);
    }
    
    public int checkWiFi(){
    	int ip=0;
    	try{
    		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    		
    		if( wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
    			hasWiFi = false;
    			showDialog(DIALOG_ID_NOWIFI);
    		}
    		else{
    			hasWiFi = true;
    			ip = wifiInfo.getIpAddress();
    		}
    	}
    	catch(Exception e){
    	}
    	return ip;
    }
    
    private void startAndromeServer(int port) {
    	try {
    		int ip_int = checkWiFi();
    		if(ip_int != 0){
	    		ipAddress = ((ip_int       ) & 0xFF) + "." +
	            			((ip_int >>  8 ) & 0xFF) + "." +
	            			((ip_int >> 16 ) & 0xFF) + "." +
	            			( ip_int >> 24   & 0xFF);
	    		
			    server = new AndromeServer(ipAddress,port);
			    server.start();
			    writeToMessageBoard("Starting server "+ipAddress + ":" + port + ".", "SYSTEM");   
    		}
    	} 
    	catch (Exception e) {
    	}
    }//end of startAndromeServer
    
    /**
     * Inner class. For easier access to UI components
     * Create an HTTP server in an separate thread so that UI can stay responsive
     * @author Chen Deng
     */
    public static class AndromeServer extends Thread {
    	
    	private ServerSocket listener = null;
    	private boolean running = true;
    	private BufferedReader inBufReader;
    	BufferedInputStream inBufInputStream;
    	Socket clientSocket;
    	String incomingMsg;
    	
    	public AndromeServer(String ip, int port) throws IOException {
    		super();
    		InetAddress ipadr = InetAddress.getByName(ip);
    		listener = new ServerSocket(port,0,ipadr);
    	}
    	
    	@Override
    	public void run() {
    		while( running ) {
    			try {
    				clientSocket = listener.accept();
    				processRequest();
    				sendResponse();
    			} 
    			catch (Exception e) {
    			}
    		}
    	}
    	
    	public void stopServer() {
    		running = false;
    		try {
    			listener.close();
    		} 
    		catch (Exception e) {
    		}
    	}
    	
    	private void send(String s, String u) {
    		Message msg = new Message();
    		Bundle b = new Bundle();
    		b.putString("msg", s);
    		b.putString("user", u);
    		msg.setData(b);
    		msgHandler.sendMessage(msg);
    	}
    	
    	// Analyze input stream and strip off HTTP header
    	public void processRequest() {
    		incomingMsg = "";
    		try{
    			inBufReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    			String str = inBufReader.readLine().trim();
    			if (!str.equals("")) {
					if (str.substring(0, 3).equals("GET")) {
    					int endPoint = str.indexOf(" HTTP/");
    					incomingMsg = str.substring(5,endPoint);
					}
				}	

	    		// request was sent from google extension
    			// using __req_ is not safe, since it can be a legal user input.
    			// Need to find another way to distinguish url request and extension request.
	    		if(incomingMsg.startsWith("__req_")){
	    			// get the actual message
	    			incomingMsg = incomingMsg.substring(6);
	    			if(incomingMsg.equals("__connection_request")){
	    	    		inputMsg = "Connection established with server " + ipAddress + ":" + port;
	    	    	}
	    			// Chrome extension send empty request periodically in order to
	        		// retrieve message stored in android side message buffer through response.
	    	    	else if(!incomingMsg.equals("")){
	    	    		// More illegal-character-handling should be implemented here
	    	    		incomingMsg = incomingMsg.replaceAll("%20", " ");
	    	    		
	    	    		send(incomingMsg, "CHROME");
	    	    	}

	    			inBufInputStream = new BufferedInputStream(new ByteArrayInputStream(inputMsg.getBytes()));
	    			inputMsg = "";
	    		}
	    		// request sent from normal browser window
	    		else{
	    			String getApp = "<a href=\"https://chrome.google.com/webstore/detail/kfhddchcbfladdbnjcbfefmdamoghnmn\">Get A-Chat from Web Store to experience all features.</a>";
	    			inBufInputStream = new BufferedInputStream(new ByteArrayInputStream(getApp.getBytes()));
	    		}
    		}
    		catch(Exception e){
    	    	try{
    	    		clientSocket.close();
    			}
    	    	catch (Exception ex){}
    	    }
    	} //end of processRequest
    	
    	private void sendResponse(){		
	    	try{
    			BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());
    			ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
    			
    			byte[] buf = new byte[MSGBUFFER_SIZE];
    			int count = 0;
    			
    			while ((count = inBufInputStream.read(buf)) != -1){
    				tempOut.write(buf, 0, count);
    			}
    			
    			tempOut.flush();
    			out.write(tempOut.toByteArray());
    			out.flush();
    			
    			// So far as I know, the message only get sent out upon the closure of socket.
    			// If there is an other way round, maybe the socket connection can be kept open, 
    			// which will greatly improve performance.
    			clientSocket.close();
    		}
    		catch(Exception e){
    	    	try{
    	    		clientSocket.close();
    			}
    	    	catch (Exception ex){}
    	    }
    	}//end of sendResponse
    		
    }//end of AndromeServer class
    
}//end of AChatActivity class