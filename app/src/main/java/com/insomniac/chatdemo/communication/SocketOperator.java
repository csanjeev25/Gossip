package com.insomniac.chatdemo.communication;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.insomniac.chatdemo.interfaces.IAppManager;
import com.insomniac.chatdemo.interfaces.ISocketOperator;


public class SocketOperator implements ISocketOperator
{
	public static final String AUTHENTICATION_SERVER_ADDRESS = "http://172.16.45.25/chats/";
	private int listeningPort = 0;

	private static final String HTTP_REQUEST_FAILED = "HttpRequestFailed";

	private HashMap<InetAddress, Socket> sockets = new HashMap<InetAddress, Socket>();

	private ServerSocket serverSocket = null;

	private boolean listening;

	private class ReceiveConnection extends Thread {
		Socket clientSocket = null;
		public ReceiveConnection(Socket socket)
		{
			this.clientSocket = socket;
			SocketOperator.this.sockets.put(socket.getInetAddress(), socket);
		}

		@Override
		public void run() {
			 try {
				BufferedReader in = new BufferedReader(
						    new InputStreamReader(
						    		clientSocket.getInputStream()));
				String inputLine;

				 while ((inputLine = in.readLine()) != null)
				 {
					 if (inputLine.equals("exit") == false)
					 {
					 }
					 else
					 {
						 clientSocket.shutdownInput();
						 clientSocket.shutdownOutput();
						 clientSocket.close();
						 SocketOperator.this.sockets.remove(clientSocket.getInetAddress());
					 }
				 }

			} catch (IOException e) {
				Log.e("ReceiveCo","Error");
			}
		}
	}

	public SocketOperator(IAppManager appManager) {
	}


	public String sendHttpRequest(String params)
	{
		String TAG="SendHttpRequest";
		URL url;
		String result = new String();
		try
		{


			Log.d(TAG,params);
			url = new URL(AUTHENTICATION_SERVER_ADDRESS);
			HttpURLConnection connection;
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);

			PrintWriter out = new PrintWriter(connection.getOutputStream());

			out.println(params);
			out.close();

			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							connection.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				result = result.concat(inputLine);
			}
			in.close();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		if (result.length() == 0) {
			result = HTTP_REQUEST_FAILED;
		}

		Log.d(TAG,result);

		return result;


	}





	public String UploadVideo(String file,String UserPrams) {

		int serverResponseCode=0;
		String fileName = file;
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;

		String TAG="SendHttpRequestVideo";


		File sourceFile = new File(file);
		if (!sourceFile.isFile()) {
			Log.e("", "Source File Does not exist");
			return null;
		}

		try {


			Log.d(TAG, UserPrams);

			FileInputStream fileInputStream = new FileInputStream(sourceFile);
			URL url = new URL(AUTHENTICATION_SERVER_ADDRESS+"index.php?"+UserPrams);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);


			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("ENCTYPE", "multipart/form-data");
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			conn.setRequestProperty("myFile", fileName);



			dos = new DataOutputStream(conn.getOutputStream());

			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"myFile\";filename=\"" + fileName + "\"" + lineEnd);
			dos.writeBytes(lineEnd);

			bytesAvailable = fileInputStream.available();
			Log.i("", "Initial .available : " + bytesAvailable);

			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];

			bytesRead = fileInputStream.read(buffer, 0, bufferSize);

			while (bytesRead > 0) {
				dos.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}

			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			serverResponseCode = conn.getResponseCode();

			fileInputStream.close();
			dos.flush();
			dos.close();
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(TAG+"",serverResponseCode+"");

		if (serverResponseCode == 200) {
			StringBuilder sb = new StringBuilder();
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(conn
						.getInputStream()));
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
			} catch (IOException ioex) {
			}
			return sb.toString();
		}else {
			return "Could not upload";
		}
	}





	public String DownloadFile(String filelocation)
	{
		try {
			URL u = new URL(AUTHENTICATION_SERVER_ADDRESS+filelocation);
			InputStream is = u.openStream();

			DataInputStream dis = new DataInputStream(is);

			byte[] buffer = new byte[1024];
			int length;

			File file=new File(Environment.getExternalStorageDirectory()+"/android-im/" + filelocation);
			file.mkdir();

			FileOutputStream fos = new FileOutputStream(file);
			while ((length = dis.read(buffer))>0) {
				fos.write(buffer, 0, length);
			}

		} catch (MalformedURLException mue) {
			Log.e("SYNC getUpdate", "malformed url error", mue);
		} catch (IOException ioe) {
			Log.e("SYNC getUpdate", "io error", ioe);
		} catch (SecurityException se) {
			Log.e("SYNC getUpdate", "security error", se);
		}

		return "";
	}



	public String DownloadVideo(String fileLocation)
	{

		class DownloadFileFromURL extends AsyncTask<String, String, String> {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();

			}

			@Override
			protected String doInBackground(String... f_url) {
				int count;
				try {
					URL url = new URL(f_url[0]);
					URLConnection conection = url.openConnection();
					conection.connect();

					int lenghtOfFile = conection.getContentLength();


					InputStream input = new BufferedInputStream(url.openStream(), 8192);


					OutputStream output = new FileOutputStream("/sdcard/android-im/video/.mp4");

					byte data[] = new byte[1024];

					long total = 0;

					while ((count = input.read(data)) != -1) {
						total += count;

						publishProgress(""+(int)((total*100)/lenghtOfFile));


						output.write(data, 0, count);
					}


					output.flush();


					output.close();
					input.close();

				} catch (Exception e) {
					Log.e("Error: ", e.getMessage());
				}

				return null;
			}


			protected void onProgressUpdate(String... progress) {

				Log.d("PRogress", Integer.parseInt(progress[0]) + "");
			}


			@Override
			protected void onPostExecute(String file_url) {

				String imagePath = Environment.getExternalStorageDirectory().toString() + "/downloadedfile.mp4";

			}

		}

		new DownloadFileFromURL().execute(fileLocation);

		return "";
	}

	public int startListening(int portNo) 
	{
		listening = true;
		
		try {
			serverSocket = new ServerSocket(portNo);
			this.listeningPort = portNo;
		} catch (IOException e) {			
			
			//e.printStackTrace();
			this.listeningPort = 0;
			return 0;
		}

		while (listening) {
			try {
				new ReceiveConnection(serverSocket.accept()).start();
				
			} catch (IOException e) {

				return 2;
			}
		}
		
		try {
			serverSocket.close();
		} catch (IOException e) {			
			Log.e("Exception server socket", "Exception when closing server socket");
			return 3;
		}
		
		
		return 1;
	}
	
	
	public void stopListening() 
	{
		this.listening = false;
	}
	
	public void exit() 
	{			
		for (Iterator<Socket> iterator = sockets.values().iterator(); iterator.hasNext();) 
		{
			Socket socket = (Socket) iterator.next();
			try {
				socket.shutdownInput();
				socket.shutdownOutput();
				socket.close();
			} catch (IOException e) 
			{				
			}		
		}
		
		sockets.clear();
		this.stopListening();
	}




	public int getListeningPort() {
		
		return this.listeningPort;
	}	

}
