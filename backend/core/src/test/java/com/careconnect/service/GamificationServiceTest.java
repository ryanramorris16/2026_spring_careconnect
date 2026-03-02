package com.careconnect.service;

import com.careconnect.model.Achievement;
import com.careconnect.model.UserAchievement;
import com.careconnect.model.XPProgress;
import com.careconnect.repository.AchievementRepository;
import com.careconnect.repository.UserAchievementRepository;
import com.careconnect.repository.XPProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GamificationService}.
 */
class GamificationServiceTest {

    @Mock
    private XPProgressRepository xpProgressRepository;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @InjectMocks
    private GamificationService gamificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ──────────────────────────────────────────────
    //  awardXp
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("awardXp_existingProgress_updatesXpAndLevel")
    void awardXp_existingProgress_updatesXpAndLevel() {
        Long userId = 1L;
        int existingXp = 40;
        int amount = 20;

        XPProgress existing = new XPProgress();
        existing.setUserId(userId);
        existing.setXp(existingXp);
        existing.setLevel(1);

        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(xpProgressRepository.save(any(XPProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        XPProgress result = gamificationService.awardXp(userId, amount);

        assertThat(result.getXp()).isEqualTo(60);
        // calculateLevel(60) = 60/50 + 1 = 2
        assertThat(result.getLevel()).isEqualTo(2);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(xpProgressRepository).save(result);
    }

    @Test
    @DisplayName("awardXp_noExistingProgress_createsNewProgressAndAwardsXp")
    void awardXp_noExistingProgress_createsNewProgressAndAwardsXp() {
        Long userId = 2L;
        int amount = 10;

        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(xpProgressRepository.save(any(XPProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        XPProgress result = gamificationService.awardXp(userId, amount);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getXp()).isEqualTo(10);
        // calculateLevel(10) = 10/50 + 1 = 1
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(xpProgressRepository).save(result);
    }

    @Test
    @DisplayName("awardXp_zeroAmount_xpUnchangedLevelStays")
    void awardXp_zeroAmount_xpUnchangedLevelStays() {
        Long userId = 3L;

        XPProgress existing = new XPProgress();
        existing.setUserId(userId);
        existing.setXp(100);
        existing.setLevel(3);

        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(xpProgressRepository.save(any(XPProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        XPProgress result = gamificationService.awardXp(userId, 0);

        assertThat(result.getXp()).isEqualTo(100);
        // calculateLevel(100) = 100/50 + 1 = 3
        assertThat(result.getLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("awardXp_levelBoundary_levelIncreasesCorrectly")
    void awardXp_levelBoundary_levelIncreasesCorrectly() {
        Long userId = 4L;

        XPProgress existing = new XPProgress();
        existing.setUserId(userId);
        existing.setXp(49);
        existing.setLevel(1);

        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(xpProgressRepository.save(any(XPProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        XPProgress result = gamificationService.awardXp(userId, 1);

        assertThat(result.getXp()).isEqualTo(50);
        // calculateLevel(50) = 50/50 + 1 = 2
        assertThat(result.getLevel()).isEqualTo(2);
    }

    // ──────────────────────────────────────────────
    //  grantAchievement
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("grantAchievement_achievementNotFound_throwsRuntimeException")
    void grantAchievement_achievementNotFound_throwsRuntimeException() {
        Long userId = 1L;
        Long achievementId = 99L;

        when(achievementRepository.findById(achievementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.grantAchievement(userId, achievementId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Achievement not found");
    }

    @Test
    @DisplayName("grantAchievement_userAlreadyHasAchievement_doesNotSaveDuplicate")
    void grantAchievement_userAlreadyHasAchievement_doesNotSaveDuplicate() {
        Long userId = 1L;
        Long achievementId = 10L;

        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setTitle("First Check-In");

        UserAchievement existingUa = new UserAchievement();
        existingUa.setUserId(userId);
        existingUa.setAchievement(achievement);

        when(achievementRepository.findById(achievementId)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(existingUa));

        gamificationService.grantAchievement(userId, achievementId);

        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
    }

    @Test
    @DisplayName("grantAchievement_userDoesNotHaveAchievement_savesNewUserAchievement")
    void grantAchievement_userDoesNotHaveAchievement_savesNewUserAchievement() {
        Long userId = 1L;
        Long achievementId = 10L;

        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setTitle("First Check-In");

        when(achievementRepository.findById(achievementId)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        gamificationService.grantAchievement(userId, achievementId);

        ArgumentCaptor<UserAchievement> captor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepository).save(captor.capture());

        UserAchievement saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAchievement()).isEqualTo(achievement);
        assertThat(saved.getEarnedAt()).isNotNull();
    }

    @Test
    @DisplayName("grantAchievement_userHasDifferentAchievement_savesNewOne")
    void grantAchievement_userHasDifferentAchievement_savesNewOne() {
        Long userId = 1L;
        Long achievementId = 10L;
        Long otherAchievementId = 20L;

        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setTitle("First Check-In");

        Achievement otherAchievement = new Achievement();
        otherAchievement.setId(otherAchievementId);
        otherAchievement.setTitle("Other Achievement");

        UserAchievement existingUa = new UserAchievement();
        existingUa.setUserId(userId);
        existingUa.setAchievement(otherAchievement);

        when(achievementRepository.findById(achievementId)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(existingUa));

        gamificationService.grantAchievement(userId, achievementId);

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    // ──────────────────────────────────────────────
    //  getAllAchievements
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getAllAchievements_achievementsExist_returnsAll")
    void getAllAchievements_achievementsExist_returnsAll() {
        Achievement a1 = new Achievement();
        a1.setTitle("First Check-In");
        Achievement a2 = new Achievement();
        a2.setTitle("Streak Master");

        when(achievementRepository.findAll()).thenReturn(List.of(a1, a2));

        List<Achievement> result = gamificationService.getAllAchievements();

        assertThat(result).hasSize(2);
        verify(achievementRepository).findAll();
    }

    @Test
    @DisplayName("getAllAchievements_noAchievements_returnsEmptyList")
    void getAllAchievements_noAchievements_returnsEmptyList() {
        when(achievementRepository.findAll()).thenReturn(Collections.emptyList());

        List<Achievement> result = gamificationService.getAllAchievements();

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────
    //  getUserAchievements
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getUserAchievements_userHasAchievements_returnsList")
    void getUserAchievements_userHasAchievements_returnsList() {
        Long userId = 1L;

        UserAchievement ua = new UserAchievement();
        ua.setUserId(userId);

        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(ua));

        List<UserAchievement> result = gamificationService.getUserAchievements(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("getUserAchievements_userHasNone_returnsEmptyList")
    void getUserAchievements_userHasNone_returnsEmptyList() {
        Long userId = 99L;

        when(userAchievementRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<UserAchievement> result = gamificationService.getUserAchievements(userId);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────
    //  getXpProgress
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getXpProgress_progressExists_returnsOptionalWithValue")
    void getXpProgress_progressExists_returnsOptionalWithValue() {
        Long userId = 1L;
        XPProgress progress = new XPProgress();
        progress.setUserId(userId);
        progress.setXp(150);

        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.of(progress));

        Optional<XPProgress> result = gamificationService.getXpProgress(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getXp()).isEqualTo(150);
    }

    @Test
    @DisplayName("getXpProgress_noProgress_returnsEmptyOptional")
    void getXpProgress_noProgress_returnsEmptyOptional() {
        Long userId = 99L;

        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Optional<XPProgress> result = gamificationService.getXpProgress(userId);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────
    //  unlockAchievement
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("unlockAchievement_achievementNotFoundByTitle_returnsEarlyWithoutSaving")
    void unlockAchievement_achievementNotFoundByTitle_returnsEarlyWithoutSaving() {
        Long userId = 1L;
        String title = "Nonexistent";

        when(achievementRepository.findByTitle(title)).thenReturn(Optional.empty());

        gamificationService.unlockAchievement(userId, title, 25);

        verify(userAchievementRepository, never()).existsByUserIdAndAchievementId(any(), any());
        verify(userAchievementRepository, never()).save(any());
        verify(xpProgressRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlockAchievement_alreadyUnlocked_returnsEarlyWithoutSaving")
    void unlockAchievement_alreadyUnlocked_returnsEarlyWithoutSaving() {
        Long userId = 1L;
        Long achievementId = 5L;
        String title = "Streak Master";

        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setTitle(title);

        when(achievementRepository.findByTitle(title)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)).thenReturn(true);

        gamificationService.unlockAchievement(userId, title, 25);

        verify(userAchievementRepository, never()).save(any());
        verify(xpProgressRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlockAchievement_newUnlock_awardsXpAndSavesUserAchievement")
    void unlockAchievement_newUnlock_awardsXpAndSavesUserAchievement() {
        Long userId = 1L;
        Long achievementId = 5L;
        String title = "Streak Master";
        int xpAward = 25;

        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setTitle(title);

        when(achievementRepository.findByTitle(title)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)).thenReturn(false);

        // awardXp will call xpProgressRepository.findByUserId -- return empty so new XPProgress is created
        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(xpProgressRepository.save(any(XPProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        gamificationService.unlockAchievement(userId, title, xpAward);

        // Verify XP was awarded (xpProgressRepository.save called via awardXp)
        ArgumentCaptor<XPProgress> xpCaptor = ArgumentCaptor.forClass(XPProgress.class);
        verify(xpProgressRepository).save(xpCaptor.capture());
        XPProgress savedXp = xpCaptor.getValue();
        assertThat(savedXp.getXp()).isEqualTo(xpAward);
        assertThat(savedXp.getUserId()).isEqualTo(userId);

        // Verify user achievement was saved
        ArgumentCaptor<UserAchievement> uaCaptor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepository).save(uaCaptor.capture());
        UserAchievement savedUa = uaCaptor.getValue();
        assertThat(savedUa.getUserId()).isEqualTo(userId);
        assertThat(savedUa.getAchievement()).isEqualTo(achievement);
        assertThat(savedUa.getEarnedAt()).isNotNull();
    }

    @Test
    @DisplayName("unlockAchievement_existingXpProgress_addsXpToExisting")
    void unlockAchievement_existingXpProgress_addsXpToExisting() {
        Long userId = 1L;
        Long achievementId = 7L;
        String title = "Health Hero";
        int xpAward = 50;

        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setTitle(title);

        XPProgress existingProgress = new XPProgress();
        existingProgress.setUserId(userId);
        existingProgress.setXp(100);
        existingProgress.setLevel(3);

        when(achievementRepository.findByTitle(title)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)).thenReturn(false);
        when(xpProgressRepository.findByUserId(userId)).thenReturn(Optional.of(existingProgress));
        when(xpProgressRepository.save(any(XPProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        gamificationService.unlockAchievement(userId, title, xpAward);

        ArgumentCaptor<XPProgress> xpCaptor = ArgumentCaptor.forClass(XPProgress.class);
        verify(xpProgressRepository).save(xpCaptor.capture());
        XPProgress savedXp = xpCaptor.getValue();
        assertThat(savedXp.getXp()).isEqualTo(150);
        // calculateLevel(150) = 150/50 + 1 = 4
        assertThat(savedXp.getLevel()).isEqualTo(4);

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }
}
