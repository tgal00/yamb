package com.tvz.java.jamb.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientHandlerThread extends Thread {

    private static final int NUM_OF_PLAYERS = 2;

    public static ArrayList<ClientHandlerThread> handlerThreads = new ArrayList<>();
    private static Long id = 0L;
    private static int numOfLoggedPlayers = 0;
    private final Socket socket;
    private String username;
    private Long userId;
    private PrintWriter out;
    private BufferedReader in;
    private static Long idOfPlayerWhoseTurnItIs = 0L;

    public ClientHandlerThread(Socket socket) throws IOException {
        super("ClientHandlerThread");
        this.socket = socket;
        handlerThreads.add(this);
    }


    public void run() {
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {

                if (line.startsWith("/userLogin")) {
                    numOfLoggedPlayers++;
                    this.userId = id;
                    id++;
                    this.username = line.split(" ")[1];
                    System.out.println("-> Prijavljen je novi korisnik: " + username + "(" + id + ")");
                    out.println("/userLogin success "+this.userId);
                }

                if (numOfLoggedPlayers == NUM_OF_PLAYERS){
                    handlerThreads.forEach(t -> t.out.println("/startGame"));
                    numOfLoggedPlayers = 0;
                }

                if(line.startsWith("/game") && line.split(" ")[1].equals("started")){
                    Long playerId = Long.parseLong(line.split(" ")[2]);
                    String playerUsername = handlerThreads.stream().filter(t -> t.userId == playerId).findFirst().get().getUsername();
                    out.println("/game username "+playerUsername);
                }

                if(line.startsWith("/ready")){
                    numOfLoggedPlayers++;

                    if(numOfLoggedPlayers == NUM_OF_PLAYERS){
                        setPlayersTurn();
                    }
                }

                if(line.startsWith("/turnend")){
                    System.out.println("  TURN END  ");
                    setPlayersTurn();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getUsername() {
        return this.username;
    }

    public void setPlayersTurn(){
        String usernameOfPlayerWhoseTurnItIs = handlerThreads.stream().filter(t -> t.userId == idOfPlayerWhoseTurnItIs).findFirst().get().getUsername();
        handlerThreads.forEach(t -> t.out.println("/turn "+idOfPlayerWhoseTurnItIs+" "+usernameOfPlayerWhoseTurnItIs));

        idOfPlayerWhoseTurnItIs++;
        if(idOfPlayerWhoseTurnItIs == NUM_OF_PLAYERS){
            idOfPlayerWhoseTurnItIs = 0L;
        }
    }
}
