package me.sogom.bridge.domain.policy.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.policy.dto.PolicyResponse;
import me.sogom.bridge.domain.policy.entity.AppBlock;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.AppBlockRepository;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildPolicyService {

    private final TimePolicyRepository timePolicyRepository;
    private final AppBlockRepository appBlockRepository;

    //자녀 앱 구동 시 현재 정책(시간 및 차단 앱) 조회
    public PolicyResponse getChildPolicy(Long childId) {
        //현재 년월 구하기 (예: "2026-04")
        String currentYearMonth = YearMonth.now().toString();

        //이번 달 시간 정책 조회 (없으면 예외 발생)
        TimePolicy timePolicy = timePolicyRepository.findByChildIdAndYearMonth(childId, currentYearMonth)
                .orElseThrow(() -> new IllegalArgumentException("이번 달에 설정된 시간 정책이 없습니다."));

        //차단 앱 목록 조회
        List<AppBlock> appBlocks = appBlockRepository.findAllByChildId(childId);

        //엔티티들을 DTO로 변환하여 반환
        return PolicyResponse.builder()
                .totalAvailableTime(timePolicy.getTotalAvailableTime())
                .baseTime(timePolicy.getBaseTime())
                .accumulatedRewardTime(timePolicy.getAccumulatedRewardTime())
                .blockedApps(appBlocks.stream()
                        .map(app -> PolicyResponse.AppBlockInfo.builder()
                                .appName(app.getAppName())
                                .packageName(app.getPackageName())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}