package me.sogom.bridge.domain.mission.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.sogom.bridge.domain.mission.entity.MissionSetting;
import me.sogom.bridge.domain.mission.repository.MissionSettingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/*
 리셋 주기에 따라 미션 상태를 초기화
 주기가 경과하면 lastResetAt을 현재 시각으로 갱신해 미션 재오픈
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionResetScheduler {

    private final MissionSettingRepository missionSettingRepository;

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void resetMissions() {
        LocalDateTime now = LocalDateTime.now();
        List<MissionSetting> settings = missionSettingRepository.findAll();

        int resetCount = 0;
        for (MissionSetting setting : settings) {
            LocalDateTime nextReset = setting.getResetCycle().nextResetAfter(setting.getLastResetAt());
            if (!now.isBefore(nextReset)) {
                setting.markReset(now);
                resetCount++;
            }
        }
        log.info("MissionResetScheduler: {}건 미션 리셋 완료 (총 {}건 검사)", resetCount, settings.size());
    }
}
