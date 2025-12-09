package com.example.scheduler;

import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootApplication
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}

@Service
class DogAdoptionScheduler {

    @McpTool(description = "schedule an appointment to pick up or adopt a dog from a Pooch Palace location")
    String schedule(@McpArg(description = "the id of the dog") int dogId,
                    @McpArg(description = "the name of the dog") String dogName) {
        var i = Instant
                .now()
                .plus(3, ChronoUnit.DAYS)
                .toString();
        IO.println("scheduling " + dogName + '/' + dogId + " for adoption " + i);
        return i;

    }
}