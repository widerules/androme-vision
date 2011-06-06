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

import android.app.Service;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

public class MyService extends Service {
	protected final IBinder mBinder = new LocalBinder();
	protected boolean serviceOn = false;
	protected static Handler msgHandler = null;
	public static final int START_STICKY = 1;
    protected static final int MSGBUFFER_SIZE = 4096;
    protected AndromeServer server = null;
    protected static boolean hasWiFi = false;
    protected static String ipAddress;
    protected static int port = 8080;
    protected static String inputMsg = "";  
	
	public class LocalBinder extends Binder {
		MyService getService() {
			return MyService.this;
		}
	}
	
	public IBinder onBind(Intent intent) {
		Toast.makeText(this, "Starting server "+ipAddress + ":" + port + ".", Toast.LENGTH_LONG).show();
		return mBinder;
	}

	public void setHandler(Handler mHandler) {
		msgHandler = mHandler;
		checkWiFi();
		send("Starting server "+ipAddress + ":" + port + ".", "SYSTEM");
	}
	
	protected void send(String s, String u) {
		//if(hasWiFi == false) {		
			/*
			// Check if the caller is send itself. 
			Throwable t = new Throwable(); 
			StackTraceElement[] elements = t.getStackTrace(); 
			if(!elements[1].getMethodName().equals("send")){
				send("DIALOG_ID_NOWIFI", "sys");
			}
			*/
		//}
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putString("msg", s);
		b.putString("user", u);
		msg.setData(b);
		msgHandler.sendMessage(msg);
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) { 
	      serviceOn = true;
	      //Runnable runnable = new ServerThread();
	      //Thread thread = new Thread(runnable);
	      //thread.start();
	      startAndromeServer(port);
	      return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		stopAndromeServer();
		serviceOn = false;
	}
	
	protected void startAndromeServer(int port) {
    	try {
    		int ip_int = checkWiFi();
    		if(ip_int != 0){
	    		ipAddress = ((ip_int       ) & 0xFF) + "." +
	            			((ip_int >>  8 ) & 0xFF) + "." +
	            			((ip_int >> 16 ) & 0xFF) + "." +
	            			( ip_int >> 24   & 0xFF);
	    		
			    server = new AndromeServer(ipAddress,port);
			    server.start();
			    send("Starting server "+ipAddress + ":" + port + ".", "SYSTEM");
			    //Toast.makeText(this, "Starting server "+ipAddress + ":" + port + ".", Toast.LENGTH_SHORT).show();
    		}
    	} 
    	catch (Exception e) {
    	}
    }//end of startAndromeServer
	
	protected void stopAndromeServer() {
		if( server != null ) {
	    		server.stopServer();
	    		server.interrupt();
	    }
	}
	
	public int checkWiFi(){
    	int ip=0;
    	try{
    		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    		
    		if( wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
    			hasWiFi = false;
    			send("DIALOG_ID_NOWIFI", "sys");
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
	
	/**
     * Inner class. For easier access to UI components
     * Create an HTTP server in an separate thread so that UI can stay responsive
     * @author Chen Deng
     */
    public class AndromeServer extends Thread {
    	
    	protected ServerSocket listener = null;
    	protected boolean running = true;
    	protected BufferedReader inBufReader;
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
    				checkWiFi();
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
    	
    	protected void send(String s, String u) {
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
    	
    	protected void sendResponse(){		
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
} //end of myService