package com.example.tss_api.controller;

import com.example.tss_api.model.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class MessageController {

    @GetMapping("/")
    public String home() {
        return "✅ TSS API is working!";
    }

    @GetMapping("/messages")
    public List<Message> getMessages() {
        return Arrays.asList(
            new Message(1, "SMS", "+254700000001", "Hello Cesy, your code is working!", false),
            new Message(2, "EMAIL", "cesy@email.com", "You have mail from TSS Worker!", true)
        );
    }
}
