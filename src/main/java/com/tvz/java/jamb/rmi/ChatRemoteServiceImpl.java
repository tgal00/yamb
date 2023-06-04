package com.tvz.java.jamb.rmi;


import com.tvz.java.jamb.model.ChatMessage;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ChatRemoteServiceImpl implements RemoteService {

    List<ChatMessage> chatMessageList;

    public ChatRemoteServiceImpl() {
        chatMessageList = new ArrayList<>();
    }

    @Override
    public void sendMessage(ChatMessage message) throws RemoteException {
        chatMessageList.add(message);
    }

    @Override
    public List<ChatMessage> getAllChatMessages() throws RemoteException {
        return chatMessageList;
    }
}
