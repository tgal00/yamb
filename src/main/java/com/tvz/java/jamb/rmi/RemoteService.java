package com.tvz.java.jamb.rmi;



import com.tvz.java.jamb.model.ChatMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteService extends Remote {
    String REMOTE_OBJECT_NAME = "hr.tvz.rmi.service";

    void sendMessage(ChatMessage message) throws RemoteException;

    List<ChatMessage> getAllChatMessages() throws RemoteException;
}

