package com.insomniac.chatdemo.interfaces;


import com.insomniac.chatdemo.types.FriendInfo;
import com.insomniac.chatdemo.types.MessageInfo;

public interface IUpdateData {
	public void updateData(MessageInfo[] messages, FriendInfo[] friends, FriendInfo[] unApprovedFriends, String userKey);

}
