package com.tvz.java.jamb.rmi;


import com.tvz.java.jamb.model.ChatMessage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatMessagesUtil {

    public static String convertChatMessagesToString(List<ChatMessage> chatMessageList) {
        StringBuilder messagesBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        for(ChatMessage message : chatMessageList) {
            if(message.isFromServer()){
                messagesBuilder.append("SERVER: ");
                messagesBuilder.append(message.getUserName());
                messagesBuilder.append(" je zapisao ");
                messagesBuilder.append(message.getMessage()+".");
                messagesBuilder.append("\n");
            }else {
                messagesBuilder.append(message.getUserName());
                messagesBuilder.append(" (");
                messagesBuilder.append(formatter.format(message.getLocalDateTime()));
                messagesBuilder.append("): ");
                messagesBuilder.append(message.getMessage());
                messagesBuilder.append("\n");
            }
        }

        return messagesBuilder.toString();
    }

}
