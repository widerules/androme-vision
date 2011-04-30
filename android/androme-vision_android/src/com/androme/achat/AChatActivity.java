/**
 * Copyright 2011 Chen Deng    Part of Androme-Vision Project
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
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
    private static final int DIALOG_ID_NOWIFI = 0;
    private static final int DIALOG_ID_HELP = 1;
    private static final int DIALOG_ID_CHANGEPORT = 2;
    private static final int DIALOG_ID_EXIT = 3;
    private static final int DIALOG_ID_INVALIDPORT = 4;
    private static final int MSGBUFFER_SIZE = 4096;
    
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
        catch(Exception e){
        }
        
        send.setOnClickListener(new OnClickListener(){
        	public void onClick(View arg0){
        		if(!message.getText().toString().equals("")){
	        		inputMsg += message.getText();
	        		writeToMessageBoard(message.getText().toString(), "ME");
	        		message.setText("");
        		}
        	}
        });
        
        message.setOnKeyListener(new OnKeyListener(){
        	public boolean onKey(View v, int keyCode, KeyEvent event) {
        		if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
        			if(!message.getText().toString().equals("")){
    	        		inputMsg += message.getText();
    	        		writeToMessageBoard(message.getText().toString(), "ME");
    	        		message.setText("");
            		}
            		return true;
        		}
        		return false;
        	}
        });
        
        changePort.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
                showDialog(DIALOG_ID_CHANGEPORT);
            }
        });
        link.setMovementMethod(LinkMovementMethod.getInstance());
        link.setText(Html.fromHtml("<a href=\"http://code.google.com/p/androme-vision/\">Androme-Vision Project</a>"));
        
    }//end of onCreate()
    
    /**
     * msgHandler is used by inner classes to access messageBoard component.
     */
    final static Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			writeToMessageBoard(b.getString("msg"), "");
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
    
    /**
     * Contains three dialogs:
     * 	0. no wifi alert 
     * 	1. help	message
     * 	2. change port prompt
     * 	3. exit	confirmation
     * 	4. invalid port number alert
     */
    @Override
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder;
    	AlertDialog alert;
    	
        switch (id) {

        case DIALOG_ID_NOWIFI:
        	builder = new AlertDialog.Builder(this);
        	builder.setTitle("Error")
        		   .setMessage("Please connect to a WIFI-network, then click Change Port.")
        		   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        	        	   
        	           }
        		   });
        	alert = builder.create();
        	return alert;
        	
        case DIALOG_ID_HELP:
        	builder = new AlertDialog.Builder(this);
        	builder.setTitle("How to use")
        		   .setMessage("Please connect to a WIFI-network, then click Change Port.")
        		   .setPositiveButton("OK", null);
        	alert = builder.create();
        	return alert;	
        
        case DIALOG_ID_CHANGEPORT:
        	builder = new AlertDialog.Builder(this);
        	
        	final EditText newPort = new EditText(this);
        	newPort.setSingleLine(true);
        	newPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        	
        	builder.setMessage("Please enter the new port: ")
        	       .setCancelable(true)
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
        	                dialog.cancel();
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
        	builder.setTitle("Error")
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
    
    private void startAndromeServer(int port) {
    	try {
    		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    		
    		int ip_int = wifiInfo.getIpAddress();
    		ipAddress = ((ip_int       ) & 0xFF) + "." +
            			((ip_int >>  8 ) & 0xFF) + "." +
            			((ip_int >> 16 ) & 0xFF) + "." +
            			( ip_int >> 24   & 0xFF);

    		if( wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
    			showDialog(DIALOG_ID_NOWIFI);
    		}
            
		    try{
		    	server = new AndromeServer(ipAddress,port);
		    	server.start();
		    	writeToMessageBoard("Starting server "+ipAddress + ":" + port + ".", "SYSTEM");
		    }
		    catch(Exception e){
		    	writeToMessageBoard("Cannot start server on port " + port + ", please choose another one.","SYSTEM");
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
    	// ClientList is used for handling multiple connections simultaneously.
    	public static LinkedList<Socket> clientList = new LinkedList<Socket>();
    	
    	public AndromeServer(String ip, int port) throws IOException {
    		super();
    		InetAddress ipadr = InetAddress.getByName(ip);
    		listener = new ServerSocket(port,0,ipadr);
    	}
    	
    	@Override
    	public void run() {
    		while( running ) {
    			try {
    				Socket client = listener.accept();
    				new AndromeServerHandler(client).start();
    				clientList.add(client);
    			} catch (Exception e) {
    			}
    		}
    	}
    	
    	public void stopServer() {
    		running = false;
    		try {
    			listener.close();
    		} catch (Exception e) {
    		}
    	}
    	
    	public static void remove(Socket s) {
            clientList.remove(s);      
        }
    }//end of AndromeServer class
    
    /**
     * Create separate thread for each request.
     * Can be further developed to support multiple clients.
     * @author Chen Deng
     *
     */
    public static class AndromeServerHandler extends Thread {
    	private BufferedReader in;
    	private Socket socket;
    	
    	AndromeServerHandler(Socket s) {
    		socket = s;
    	}
    	
    	private void send(String s) {
    		Message msg = new Message();
    		Bundle b = new Bundle();
    		b.putString("msg", s);
    		msg.setData(b);
    		msgHandler.sendMessage(msg);
    	}
    	
    	public void run() {
    		String incomingMsg = "";
    		// check client is using Chrome extension or browser only
    		boolean isChrome = true;
    		
    		try{
    			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    			while (true) {
    				isChrome = true;
    				String str = in.readLine().trim();
    				if (str.equals("")) {
    					break;
    				}
    				if (str.substring(0, 3).equals("GET")) {
    					int endPoint = str.indexOf(" HTTP/");
    					incomingMsg = str.substring(5,endPoint);
    				}
    			}	
    		}
    		catch(Exception e){
    			AndromeServer.remove(socket);
    	    	try{
    	    		socket.close();
    			}
    	    	catch (Exception ex){}
    	    }
    		
    		// "__req" is empty request. Chrome extension send empty request periodically in order to
    		// retrieve message stored in android side message buffer.
    		if(!incomingMsg.equals("__req")){
    			if(incomingMsg.equals("__connection_request")){
    	    		inputMsg = "Connection established with server " + ipAddress + ":" + port;
    	    	}
    	    	else{
    	    		// More illegal-character-handling should be implemented here
    	    		incomingMsg = incomingMsg.replaceAll("%20", " ");
    	    		
    	    		send("CHROME: " + incomingMsg);
    	    	}
    	    }
    		
    		try {
    			BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(inputMsg.getBytes()));
    			inputMsg = "";
    			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
    			ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
    			
    			byte[] buf = new byte[MSGBUFFER_SIZE];
    			int count = 0;
    			
    			while ((count = in.read(buf)) != -1){
    				tempOut.write(buf, 0, count);
    			}
    			
    			tempOut.flush();
    			out.write(tempOut.toByteArray());
    			out.flush();
    			
    			// So far as I know, the message only get sent out upon the closure of socket.
    			// If there is an other way round, maybe the socket connection can be kept open, 
    			// which will greatly improve performance.
    			AndromeServer.remove(socket);
    			socket.close();
    		}
    		catch(Exception e){
    		}	
    	}//end of run
 	
    }//end of AndromeServerHandler class
    
}//end of AChatActivity class