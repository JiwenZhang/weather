package com.example.search.service;


import com.example.search.config.DetailConfig;
import com.example.search.config.EndpointConfig;
import com.example.search.pojo.City;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WeatherServiceImpl implements WeatherService{

    private final RestTemplate restTemplate;
    private final Executor executor;

    private static Logger log = LoggerFactory.getLogger(WeatherServiceImpl.class);

    @Autowired
    public WeatherServiceImpl(RestTemplate getRestTemplate, Executor executor) {
        this.restTemplate = getRestTemplate;
        this.executor = executor;
    }

    @Override
    @Retryable(include = IllegalAccessError.class)
    public List<Integer> findCityIdByName(String city) {
        return restTemplate.getForObject(DetailConfig.queryIdByName + city, List.class);
    }

    @Override
    public Map<String, Map> findCityNameById(int id) {
        Map<String, Map> ans = restTemplate.getForObject(EndpointConfig.queryWeatherById + id, HashMap.class);
        return ans;
    }

    //@Async("threadPoolTaskExecutor")
    @Override
    @Retryable(include = IllegalAccessError.class)
    public Map<String, Map> asyncWeatherService(String city) {
        /*
        log.info("asyncWeatherService starts");
        // assume there is no empty list
        List<Integer> res = findCityIdByName(city);
        Integer targetId = res.iterator().next();
        log.info(city + "'s id, {}", targetId);

        Map<String, Map> ans = findCityNameById(targetId);
        log.info(city + "'s weather data completed");

        //log.info(city + " --> " + ans.toString());
        return CompletableFuture.completedFuture(ans);
         */

        log.info("asyncWeatherService starts");
        // assume there is no empty list
        List<Integer> res = findCityIdByName(city);
        log.info("find " + city + "'s city id");
        Integer id = res.iterator().next();
        CompletableFuture<Map<String, Map>> cf = CompletableFuture.supplyAsync(()-> {
            log.info("find " + city + "'s weather data");
            return findCityNameById(id);
        }, executor);
        return cf.join();
    }

}


/**
 *  -> gateway -> eureka
 *       |
 *   weather-search -> hystrix(thread pool) -> 3rd party weather api
 *
 *
 *  circuit breaker(hystrix)
 * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *
 *   weather-search service should get city id from detail service
 *   and use multi-threading to query city's weather details
 *
 *   gateway
 *     |
 *  weather-service -> 3rd party api(id <-> weather)
 *    |
 *  detail-service -> 3rd party api (city <-> id)
 *
 *  failed situations:
 *      1. 3rd party api timeout -> retry + hystrix
 *      2. 3rd party api available time / rate limit
 *      3. security verification
 *  response
 *      1. no id -> error / empty
 *      2. large response -> pagination / file download (link / email)
 *  performance
 *      1. cache / db
 *
 *   gateway
 *     |
 *  weather-service -> cache(city - id - weather) (LFU)
 *    |
 *   DB (city - id - weather) <-> service <->  message queue  <-> scheduler <-> 3rd party api(city - id)
 *                                                                  |
 *                                                         update id - weather every 30 min
 *                                                         update city - id relation once per day
 *
 *  homework :
 *      deadline -> Wednesday midnight
 *      1. update detail service
 *          a. send request to 3rd party api -> get id by city
 *      2. update search service
 *          a. add ThreadPool
 *          b. send request to detail service -> get id by city
 *          c. use CompletableFuture send request to 3rd party api -> get weather by ids
 *          d. add retry feature
 */