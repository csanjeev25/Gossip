package com.insomniac.chatdemo.interfaces;


public interface ISocketOperator {
	
	public String sendHttpRequest(String params);
	public int startListening(int port);
	public void stopListening();
	public void exit();
	public int getListeningPort();

	public String UploadVideo(String filePath,String userprams);
}
