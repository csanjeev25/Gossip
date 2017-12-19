package com.insomniac.chatdemo.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.insomniac.chatdemo.Login;
import com.insomniac.chatdemo.Messaging;
import com.insomniac.chatdemo.R;
import com.insomniac.chatdemo.communication.SocketOperator;
import com.insomniac.chatdemo.interfaces.IAppManager;
import com.insomniac.chatdemo.interfaces.ISocketOperator;
import com.insomniac.chatdemo.interfaces.IUpdateData;
import com.insomniac.chatdemo.tools.FriendController;
import com.insomniac.chatdemo.tools.ImageHandlet;
import com.insomniac.chatdemo.tools.JsonHandler;
import com.insomniac.chatdemo.tools.LocalStorageHandler;
import com.insomniac.chatdemo.tools.MessageController;
import com.insomniac.chatdemo.types.FriendInfo;
import com.insomniac.chatdemo.types.MessageInfo;
import com.insomniac.chatdemo.types.Profiler;


public class IMService extends Service implements IAppManager, IUpdateData {
	public static final String PROFILE_PIC_UPLOAD_FAILED ="0";
//	private NotificationManager mNM;
	
	public static String USERNAME;
	public static final String TAKE_MESSAGE = "Take_Message";
	public static final String FRIEND_LIST_UPDATED = "Take Friend List";
	public static final String MESSAGE_LIST_UPDATED = "Take Message List";
	public ConnectivityManager conManager = null; 
	private final int UPDATE_TIME_PERIOD = 15000;
	private String rawFriendList = new String();
	private String rawMessageList = new String();

	ISocketOperator socketOperator = new SocketOperator(this);

	private final IBinder mBinder = new IMBinder();
	private String username;
	private String password;
	private boolean authenticatedUser = false;
	private Timer timer;
	

	private LocalStorageHandler localstoragehandler;
	
	private NotificationManager mNM;

	public class IMBinder extends Binder {
		public IAppManager getService() {
			return IMService.this;
		}
		
	}
	   
    @Override
    public void onCreate() 
    {   	
         mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

         localstoragehandler = new LocalStorageHandler(this);

    	conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    	new LocalStorageHandler(this);
    	

		timer = new Timer();   
		
		Thread thread = new Thread()
		{
			@Override
			public void run() {			
				

				Random random = new Random();
				int tryCount = 0;
				while (socketOperator.startListening(10000 + random.nextInt(20000))  == 0 )
				{		
					tryCount++; 
					if (tryCount > 10)
					{

						break;
					}
					
				}
			}
		};		
		thread.start();
    
    }



	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}





    private void showNotification(String username, String msg) 
	{       

    	String title = "You got a new Message! (" + username + ")";
 				
    	String text = username + ": " + 
     				((msg.length() < 5) ? msg : msg.substring(0, 5)+ "...");
    	

    	NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
    	.setSmallIcon(R.drawable.stat_sample)
    	.setContentTitle(title)
    	.setContentText(text); 
    	
    	

        Intent i = new Intent(this, Messaging.class);
        i.putExtra(FriendInfo.USERNAME, username);
        i.putExtra(MessageInfo.MESSAGETEXT, msg);	
        

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                i, 0);


        mBuilder.setContentIntent(contentIntent); 
        
        mBuilder.setContentText("New message from " + username + ": " + msg);

        mNM.notify((username+msg).hashCode(), mBuilder.build());
    }


	@Override
	public Profiler getUserprofile() {


		Profiler profiler=new Profiler();

				try {
					final String result=socketOperator.sendHttpRequest(getAuthenticateUserParamsforProfile(username, password));
					if(result.length()>1)
					{

						JSONObject jsonObject=new JSONObject(result);
						profiler.email=jsonObject.getString(Profiler.EMAIL);
						profiler.pic= ImageHandlet.decodeBase64(jsonObject.getString(Profiler.PIC));

					}


					Log.d("GetuserProfile", result);



				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				finally {

				}






		return profiler;
	}

	public String getUsername() {
		return this.username;
	}


	public String sendMessage(String  username, String  tousername, String message,String messageType,String picPath)
	{

		String params="";
		try {
			params= "username=" + URLEncoder.encode(this.username, "UTF-8") +
					"&password=" + URLEncoder.encode(this.password, "UTF-8") +
					"&to=" + URLEncoder.encode(tousername, "UTF-8") +
					"&message=" + URLEncoder.encode(message, "UTF-8") +
					"&action=" + URLEncoder.encode("sendMessage", "UTF-8") +

					"&" + MessageInfo.MESSAGETYPE + "=" + URLEncoder.encode(messageType, "UTF-8") +
					"&" + MessageInfo.FILE + "=" + URLEncoder.encode(ImageHandlet.BitmapTOString(ImageHandlet.GetBitmapFromPath(picPath)), "UTF-8") +


					"&";
			Log.i("PARAMS", params);
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		return socketOperator.sendHttpRequest(params);
	}

	public String sendVideoMessage(String  username, String  tousername, String message,String messageType,String picPath,String Vediopath)
	{

		String params="";
		try {
			params= "username=" + URLEncoder.encode(this.username, "UTF-8") +
					"&password=" + URLEncoder.encode(this.password, "UTF-8") +
					"&to=" + URLEncoder.encode(tousername, "UTF-8") +
					"&message=" + URLEncoder.encode(message, "UTF-8") +
					"&action=" + URLEncoder.encode("uploadvideo", "UTF-8") +

					"&" + MessageInfo.MESSAGETYPE + "=" + URLEncoder.encode(messageType, "UTF-8") +

					"&";
			Log.i("PARAMS", params);
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		return socketOperator.UploadVideo(Vediopath,params);
	}



	private String getFriendList() throws UnsupportedEncodingException 	{		

		
		 rawFriendList = socketOperator.sendHttpRequest(getAuthenticateUserParams(username, password));
		 if (rawFriendList != null) {
			 this.parseFriendInfo(rawFriendList);
		 }
		 return rawFriendList;
	}
	
	private String getMessageList() throws UnsupportedEncodingException 	{		

		
		 rawMessageList = socketOperator.sendHttpRequest(getAuthenticateUserParams(username, password));
		 if (rawMessageList != null) {
			 this.parseMessageInfo(rawMessageList);
		 }
		 return rawMessageList;
	}
	


	public String authenticateUser(String usernameText, String passwordText) throws UnsupportedEncodingException 
	{
		this.username = usernameText;
		this.password = passwordText;	
		
		this.authenticatedUser = false;
		
		String result = this.getFriendList();
		if (result != null && !result.equals(Login.AUTHENTICATION_FAILED))
		{			

			this.authenticatedUser = true;
			rawFriendList = result;
			USERNAME = this.username;
			Intent i = new Intent(FRIEND_LIST_UPDATED);					
			i.putExtra(FriendInfo.FRIEND_LIST, rawFriendList);
			sendBroadcast(i);
			
			timer.schedule(new TimerTask() {
				public void run() {
					try {

						Intent i = new Intent(FRIEND_LIST_UPDATED);
						Intent i2 = new Intent(MESSAGE_LIST_UPDATED);
						String tmp = IMService.this.getFriendList();
						String tmp2 = IMService.this.getMessageList();
						if (tmp != null) {
							i.putExtra(FriendInfo.FRIEND_LIST, tmp);
							sendBroadcast(i);
							Log.i("friendlistbroad sent ", "");

							if (tmp2 != null) {
								i2.putExtra(MessageInfo.MESSAGE_LIST, tmp2);
								sendBroadcast(i2);
								Log.i("friend list broad sent ", "");
							}
						} else {
							Log.i("friend returned null", "");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, UPDATE_TIME_PERIOD, UPDATE_TIME_PERIOD);
		}
		
		return result;		
	}

	public void messageReceived(String username, String message, String messagetype, String file)
	{				
		

		MessageInfo msg = MessageController.checkMessage(username);
		if ( msg != null)
		{			
			Intent i = new Intent(TAKE_MESSAGE);

			Log.d("-----Messag Recieved---", "------------------");

			i.putExtra(MessageInfo.USERID, msg.userid);			
			i.putExtra(MessageInfo.MESSAGETEXT, msg.messagetext);
			i.putExtra(MessageInfo.MESSAGETYPE,msg.messagetype);
			String saveFilepath=LocalStorageHandler.SavePc(ImageHandlet.decodeBase64(msg.file))+"";
			i.putExtra(MessageInfo.FILE,saveFilepath);


			Log.d(MessageInfo.USERID, msg.userid);
			Log.d(MessageInfo.MESSAGETEXT,msg.messagetext);
			Log.d(MessageInfo.MESSAGETYPE,msg.messagetype);
			Log.d(MessageInfo.FILE,msg.file);

			sendBroadcast(i);

			Log.d("-------------END-----", "--------END-------");
			String activeFriend = FriendController.getActiveFriend();
			if (activeFriend == null || activeFriend.equals(username) == false) 
			{
				if(messagetype.equals(MessageInfo.MESSAGE_TYPE_PIC))
				{


						localstoragehandler.insert(username, this.getUsername(), message.toString(), messagetype.toString(), saveFilepath, LocalStorageHandler.DOWNLOADED);


				}else if(messagetype.equals(MessageInfo.MESSAGE_TYPE_VIDEO))
				{
					localstoragehandler.insert(username,this.getUsername(),message.toString(),messagetype,msg.file,LocalStorageHandler.NotDOWNLOADED);
				}
				else
				{

					localstoragehandler.insert(username, this.getUsername(), message.toString(), messagetype.toString(), "",LocalStorageHandler.DOWNLOADED);
				}
				showNotification(username, message);
			}
			
			Log.i("TAKE_MESSAGE  ", "sdsdsds");
		}	
		
	}



	private String getAuthenticateUserParams(String usernameText, String passwordText) throws UnsupportedEncodingException
	{
		String params = "username=" + URLEncoder.encode(usernameText,"UTF-8") +
				"&password="+ URLEncoder.encode(passwordText,"UTF-8") +
				"&action="  + URLEncoder.encode("authenticateUserJson","UTF-8")+
				"&port="    + URLEncoder.encode(Integer.toString(socketOperator.getListeningPort()),"UTF-8") +
				"&";

		return params;
	}
	private String getAuthenticateUserParamsforVideoUpload(String usernameText, String passwordText) throws UnsupportedEncodingException
	{
		String params = "username=" + URLEncoder.encode(usernameText,"UTF-8") +
				"&password="+ URLEncoder.encode(passwordText,"UTF-8") +
				"&action="  + URLEncoder.encode("authenticateUserJson","UTF-8")+
				"&port="    + URLEncoder.encode(Integer.toString(socketOperator.getListeningPort()),"UTF-8") +
				"&";

		return params;
	}



	private String getAuthenticateUserParamsforProfile(String usernameText, String passwordText) throws UnsupportedEncodingException
	{
		String params = "username=" + URLEncoder.encode(usernameText,"UTF-8") +
				"&password="+ URLEncoder.encode(passwordText,"UTF-8") +
				"&action="  + URLEncoder.encode("getuserprofile","UTF-8")+
				"&port="    + URLEncoder.encode(Integer.toString(socketOperator.getListeningPort()),"UTF-8") +
				"&";

		return params;
	}
	private String getAuthenticateUserParamsforPicUpload(String usernameText, String passwordText,Bitmap bitmap) throws UnsupportedEncodingException
	{
		String params = "username=" + URLEncoder.encode(usernameText,"UTF-8") +
				"&password="+ URLEncoder.encode(passwordText,"UTF-8") +
				"&action="  + URLEncoder.encode("uploadProfilePic","UTF-8")+
				"&port="    + URLEncoder.encode(Integer.toString(socketOperator.getListeningPort()),"UTF-8") +
				"&pic="		+URLEncoder.encode(ImageHandlet.BitmapTOString(bitmap),"UTF-8")+
				"&";

		return params;
	}

	public void setUserKey(String value) 
	{		
	}

	public boolean isNetworkConnected() {
		return conManager.getActiveNetworkInfo().isConnected();
	}
	
	public boolean isUserAuthenticated(){
		return authenticatedUser;
	}
	
	public String getLastRawFriendList() {		
		return this.rawFriendList;
	}
	
	@Override
	public void onDestroy() {
		Log.i("IMService destroyed", "...");
		super.onDestroy();
	}
	
	public void exit() 
	{
		timer.cancel();
		socketOperator.exit(); 
		socketOperator = null;
		this.stopSelf();
	}
	
	public String signUpUser(String usernameText, String passwordText,
			String emailText) 
	{
		String params = "username=" + usernameText +
						"&password=" + passwordText +
						"&action=" + "signUpUser"+
						"&email=" + emailText+
						"&";

		String result = socketOperator.sendHttpRequest(params);		
		
		return result;
	}

	public String addNewFriendRequest(String friendUsername) 
	{
		String params = "username=" + this.username +
		"&password=" + this.password +
		"&action=" + "addNewFriend" +
		"&friendUserName=" + friendUsername +
		"&";

		String result = socketOperator.sendHttpRequest(params);		
		
		return result;
	}

	public String sendFriendsReqsResponse(String approvedFriendNames,
			String discardedFriendNames) 
	{
		String params = "username=" + this.username +
		"&password=" + this.password +
		"&action=" + "responseOfFriendReqs"+
		"&approvedFriends=" + approvedFriendNames +
		"&discardedFriends=" +discardedFriendNames +
		"&";

		String result = socketOperator.sendHttpRequest(params);		
		
		return result;
		
	}




	@Override
	public String UploadPicOfUser(Bitmap bitmap) {


		String result=null;
		try {
			result=socketOperator.sendHttpRequest(getAuthenticateUserParamsforPicUpload(username,password,bitmap));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}


		return result;
	}

	private void parseFriendInfo(String xml)
	{
		JsonHandler jsonHandler=new JsonHandler(xml,IMService.this);
		jsonHandler.Parseit();
	}
	private void parseMessageInfo(String xml)
	{

		JsonHandler jsonHandler=new JsonHandler(xml,IMService.this);
		jsonHandler.Parseit();


	}

	public void updateData(MessageInfo[] messages,FriendInfo[] friends,
			FriendInfo[] unApprovedFriends, String userKey) 
	{
		this.setUserKey(userKey);
		MessageController.setMessagesInfo(messages);
		Log.i("MESSAGEIMSERVICE", "messages.length=" + messages.length);
		
		int i = 0;
		while (i < messages.length){
			messageReceived(messages[i].userid,messages[i].messagetext,messages[i].messagetype,messages[i].file);
			i++;
		}
		
		
		FriendController.setFriendsInfo(friends);
		FriendController.setUnapprovedFriendsInfo(unApprovedFriends);
		
	}










}