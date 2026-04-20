package com.rajdip14.leaderboard_aggregator.serivce;

import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;

import java.util.List;
import java.util.Map;

public interface ProfileService {
    Map<String, PlayerProfile> getProfiles(List<String> playerIds);
}
