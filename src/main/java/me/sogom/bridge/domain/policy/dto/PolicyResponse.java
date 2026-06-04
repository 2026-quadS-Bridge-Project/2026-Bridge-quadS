package me.sogom.bridge.domain.policy.dto;

import lombok.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyResponse {

    //시간 정책 관련 데이터
    private int totalAvailableTime;    // 이번 달 쓸 수 있는 총 시간 (기본 + 보상 합계)
    private int baseTime;              // 부모님이 설정한 순수 기본 시간
    private int accumulatedRewardTime; // 미션으로 모은 보상 시간

    //앱 차단 관련 데이터
    private List<AppBlockInfo> blockedApps; // 차단된 앱들의 상세 리스트

    //차단 앱의 정보를 담는 내부 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AppBlockInfo {
        private String appName;     // 앱 이름 (예: 인스타그램)
        private String packageName; // 패키지명 (예: com.instagram.android)
    }
}