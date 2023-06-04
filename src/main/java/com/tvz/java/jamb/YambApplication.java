package com.tvz.java.jamb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class YambApplication extends Application {


    public static Long userId;
    private static String title;

    public static void main(String[] args) {

        String username = args[0];
        title = username;
        try (
                Socket clientSocket = new Socket("localhost", 3333);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            //prvo javljanje na server
            out.println("/userLogin "+username);

            String line;
            while ((line = in.readLine()) != null){
                if(line.startsWith("/userLogin")){
                    String[] items = line.split(" ");
                    if(items[1].equals("success")){
                        System.out.println("-> Klijent "+ username +" je uspješno spojen na server.");
                        userId = Long.parseLong(items[2]);
                    }
                }

                if(line.startsWith("/startGame")){
                    launch();
                }

            }


        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + "localhost");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    "localhost");
            System.exit(1);
        }

    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(YambApplication.class.getResource("yambMainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 900);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}