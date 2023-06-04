package com.tvz.java.jamb.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatMessage implements Serializable {

    private String userName;
    private LocalDateTime localDateTime;
    private String message;
    private boolean fromServer;

}
