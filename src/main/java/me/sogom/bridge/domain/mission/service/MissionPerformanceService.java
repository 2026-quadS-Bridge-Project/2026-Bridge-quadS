package me.sogom.bridge.domain.mission.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.entity.*;
import me.sogom.bridge.domain.mission.exception.MissionErrorCode;
import me.sogom.bridge.domain.mission.exception.MissionException;
import me.sogom.bridge.domain.mission.repository.MissionPerformanceRepository;
import me.sogom.bridge.domain.mission.repository.MissionRepository;
import me.sogom.bridge.domain.mission.repository.MissionSettingRepository;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class MissionPerformanceService {

    private final MissionPerformanceRepository performanceRepository;
    private final MissionRepository missionRepository;
    private final ChildrenRepository childrenRepository; // 자녀 조회
    private final MissionVerificationAiService aiService;
    private final MissionSettingRepository missionSettingRepository;
    private final TimePolicyRepository timePolicyRepository;

    @Transactional // 데이터를 DB에 반영하기 위해 반드시 필요
    public AiVerificationResponse verifyAndSaveMission(Long missionId, Long childId, MultipartFile image, String prompt, MissionCategory category) throws IOException {
        //미션과 자녀 정보 조회 (예외 던지기 적용)
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));
        Children child = childrenRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("해당 자녀를 찾을 수 없습니다.")); // ChildrenException 생략

        //연관관계 검증 (이 미션이 지금 요청한 아이의 미션이 맞는지 확인하는 로직 추가 가능)
        // 미션 엔티티에 저장된 자녀 ID와 현재 수행하려는 자녀 ID가 일치하는지 바로 확인
        if (!mission.getChild().getId().equals(childId)) {
            throw new MissionException(MissionErrorCode.UNAUTHORIZED_ACCESS);
        }

        //AI 판독 요청 (기존 MissionVerificationAiService 호출)
        AiVerificationResponse aiResponse = aiService.verifyMissionImage(image, category, prompt);

        // 결과에 따른 미션 상태 결정
        MissionStatus status = aiResponse.isAccepted() ? MissionStatus.ACCEPTED : MissionStatus.REJECTED;

        // 미션 판독 결과가 성공(ACCEPTED)일 때만 실시간 보상 시간 정산 진행
        if (status == MissionStatus.ACCEPTED) {
            // 해당 미션에 걸려있는 보상 시간(reward) 가져오기
            MissionSetting setting = missionSettingRepository.findByMissionId(missionId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 미션의 설정 정보를 찾을 수 없습니다."));
            int rewardTime = setting.getReward();

            // 현재 날짜 기준 년월 구하기 (예: "2026-05")
            String currentYearMonth = YearMonth.now().toString();

            // 자녀의 이번 달 시간 정책 가져와서 보상 시간 누적하기
            TimePolicy timePolicy = timePolicyRepository.findByChildIdAndYearMonth(childId, currentYearMonth)
                    .orElseThrow(() -> new IllegalArgumentException("이번 달에 설정된 자녀의 시간 정책(TimePolicy)이 없습니다."));

            // 시간 추가 (엔티티 내부 메서드 호출)
            timePolicy.addReward(rewardTime);

            // @Transactional 환경이므로 Dirty Checking(변경 감지)이 일어나 자동 저장되지만, 명시적인 가독성을 위해 호출
            timePolicyRepository.save(timePolicy);
        }

        // 객체 조립 및 수행 내역 저장
        MissionPerformance performance = MissionPerformance.builder()
                .mission(mission)
                .child(child)
                .status(status)
                .reason(aiResponse.reason())
                .build();
        //사진 URL은 나중에 S3 같은 곳에 올리고 URL을 받아 넣어야 함

        //최종적으로 DB에 영속화 (Save)
        performanceRepository.save(performance);

        //프론트엔드에게 돌려줄 AI 응답 반환
        return aiResponse;
    }
}