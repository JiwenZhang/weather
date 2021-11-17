package com.example.search.controller;

import com.example.search.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class TestAsyncController {
    private static Logger log = LoggerFactory.getLogger(TestAsyncController.class);
    private final WeatherService weatherService;

    @Autowired
    public TestAsyncController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/testAsync")
    public void testAsync() throws ExecutionException, InterruptedException {
        log.info("testAsync Start");
        CompletableFuture<Map<String, Map>> ans1 = weatherService.asyncWeatherService("beijing");
        CompletableFuture<Map<String, Map>> ans2 = weatherService.asyncWeatherService("chicago");
        CompletableFuture<Map<String, Map>> ans3 = weatherService.asyncWeatherService("houston");

        CompletableFuture.allOf(ans1, ans2, ans3).join();
        log.info("beijing --> " + ans1.get());
        log.info("chicago --> " + ans2.get());
        log.info("houston --> " + ans3.get());
    }
}
