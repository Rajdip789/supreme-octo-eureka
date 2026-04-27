package com.rajdip14.leaderboard_aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LeaderboardAggregatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaderboardAggregatorApplication.class, args);
	}

}
