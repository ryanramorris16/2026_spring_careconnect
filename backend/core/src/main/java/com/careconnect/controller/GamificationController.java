package com.careconnect.controller;

import com.careconnect.model.*;
import com.careconnect.security.AuthorizationService;
import com.careconnect.security.UnauthorizedException;
import com.careconnect.service.GamificationService;
import com.careconnect.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final GamificationService gamificationService;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Autowired
    public GamificationController(GamificationService gamificationService,
                                  SecurityUtil securityUtil,
                                  AuthorizationService authorizationService) {
        this.gamificationService = gamificationService;
        this.securityUtil = securityUtil;
        this.authorizationService = authorizationService;
    }

    // 1. Award XP to user
    @PostMapping("/award-xp")
    public ResponseEntity<?> awardXp(@RequestBody Map<String, Object> body) throws UnauthorizedException {
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requireAdmin(currentUser);

        Long userId = Long.valueOf(body.get("userId").toString());
        int amount = Integer.parseInt(body.get("amount").toString());

        XPProgress updatedProgress = gamificationService.awardXp(userId, amount);
        return ResponseEntity.ok(updatedProgress);
    }

    @GetMapping("/progress/{userId}")
    public ResponseEntity<?> getXpProgress(@PathVariable Long userId) throws UnauthorizedException {
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requireSelfOrAdmin(currentUser, userId);

        return gamificationService.getXpProgress(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(null));
    }

    // 3. Get earned achievements for a user
    @GetMapping("/achievements/{userId}")
    public ResponseEntity<List<UserAchievement>> getUserAchievements(@PathVariable Long userId) throws UnauthorizedException {
        User currentUser = securityUtil.resolveCurrentUser();
        authorizationService.requireSelfOrAdmin(currentUser, userId);

        return ResponseEntity.ok(gamificationService.getUserAchievements(userId));
    }

    // 4. Get full list of all achievements (earned + unearned)
    @GetMapping("/all-achievements")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        return ResponseEntity.ok(gamificationService.getAllAchievements());
    }
}
