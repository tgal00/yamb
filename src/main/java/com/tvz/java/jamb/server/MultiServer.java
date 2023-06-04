package com.tvz.java.jamb.server;

import java.io.IOException;
import java.net.ServerSocket;

public class MultiServer {

    private static final int PORT = 3333;


    public static void main(String[] args) {
        boolean listening = true;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.err.println("SERVER: Server je startan na portu: "+PORT+".");
            while (listening) {
                System.err.println("SERVER: Čekam novo spajanje...");
                new ClientHandlerThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT);
            System.exit(-1);
        }

    }
}
