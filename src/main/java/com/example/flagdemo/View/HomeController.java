package com.example.flagdemo.View;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "StartScreen"; // maps to templates/StartScreen.html
    }

    @GetMapping("/credits")
    public String credits() {
        return "CreditsScreen"; // music, flag images and libraries the site uses, as their licenses require
    }
}