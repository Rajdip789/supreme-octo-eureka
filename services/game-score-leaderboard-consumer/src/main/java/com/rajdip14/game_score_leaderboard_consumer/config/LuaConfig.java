package com.rajdip14.game_score_leaderboard_consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static com.rajdip14.game_score_leaderboard_consumer.utils.AppConstants.LUA_SCRIPT_PATH;

@Configuration
public class LuaConfig {

    @Bean
    public DefaultRedisScript<Long> updateScoreScript() {

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(LUA_SCRIPT_PATH));
        script.setResultType(Long.class);

        return script;
    }
}
