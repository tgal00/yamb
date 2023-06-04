package com.tvz.java.jamb.rmi;

import javafx.scene.control.TextArea;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RefreshChatMessagesThread implements Runnable {

    private final TextArea textArea;

    public RefreshChatMessagesThread(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void run() {

        RemoteService service;
        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry("localhost", 1099);
            service = (RemoteService) registry.lookup(RemoteService.REMOTE_OBJECT_NAME);
        } catch (RemoteException | NotBoundException e) {
            throw new RuntimeException(e);
        }

        while(true) {
            try {
                Thread.sleep(1000);
                textArea.setText(ChatMessagesUtil.convertChatMessagesToString(service.getAllChatMessages()));
            } catch (RemoteException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
