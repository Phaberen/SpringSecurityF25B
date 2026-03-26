package org.example.springsecurityf25b.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {

    @GetMapping("/test")
    public String hello() {
        return "Hello World!";
    }

}
