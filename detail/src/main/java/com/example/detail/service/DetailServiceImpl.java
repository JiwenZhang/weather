package com.example.detail.service;

import com.example.detail.config.EndpointConfig;
import com.example.detail.pojo.City;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DetailServiceImpl implements DetailService {
    private final RestTemplate restTemplate;

    @Autowired
    public DetailServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retryable(include = IllegalAccessError.class)
    public List<Integer> getIdByCity(String city) {
        City[] cities = restTemplate.getForObject(EndpointConfig.queryWeatherByCity + city, City[].class);
        List<Integer> ans = new ArrayList<>();
        for (City c : cities) {
            if (c != null && c.getWoeid() != null) {
                ans.add(c.getWoeid());
            }
        }
        return ans;
    }
}
