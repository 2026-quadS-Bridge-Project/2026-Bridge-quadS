package me.sogom.bridge.domain.mission.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.dto.res.MissionPerformanceResDTO;
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

        //해당 미션의 가장 최근 수행 내역을 확인합니다.
        performanceRepository.findTopByMissionIdOrderByIdDesc(missionId)
                .ifPresent(lastPerformance -> {
                    // 이미 부모나 AI가 승인(ACCEPTED)한 미션이라면 더 이상 진행하지 못하게 차단!
                    if (lastPerformance.getStatus() == MissionStatus.ACCEPTED) {
                        throw new MissionException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
                        // ➡️ 익셉션 핸들러를 통해 포스트맨에 "이미 완료된 미션입니다" (400) 에러가 예쁘게 나감
                    }
                });

        // 2. [PENDING 먼저 저장] AI를 호출하기 전에 우선 수행 내역을 'PENDING' 상태로 DB에 저장합니다.
        MissionPerformance performance = MissionPerformance.builder()
                .mission(mission)
                .child(child)
                .status(MissionStatus.PENDING) // 무조건 대기 상태로 시작!
                .reason("AI가 사진을 분석하고 있습니다.")
                .build();
        performanceRepository.save(performance);

        // try-catch 블록 내부와 외부 모두에서 사용하기 위해 변수를 미리 선언합니다.
        AiVerificationResponse aiResponse;

        try {
            // 구글 Gemini AI 멀티모달 판독 요청
            aiResponse = aiService.verifyMissionImage(image, category, prompt);

            // AI 판독 성공 시 결과 결정 (ACCEPTED 또는 REJECTED)
            MissionStatus finalStatus = aiResponse.isAccepted() ? MissionStatus.ACCEPTED : MissionStatus.REJECTED;

            // 아까 저장해둔 performance 엔티티의 상태를 최종 판정 상태로 업데이트 (변경 감지 반영)
            performance.updateStatusAndReason(finalStatus, aiResponse.reason());

            // 미션 판독 결과가 성공(ACCEPTED)일 때만 실시간 보상 시간 정산 진행
            if (finalStatus == MissionStatus.ACCEPTED) {
                // 해당 미션에 걸려있는 보상 시간(reward) 가져오기
                MissionSetting setting = missionSettingRepository.findByMissionId(missionId)
                        .orElseThrow(() -> new IllegalArgumentException("해당 미션의 설정 정보를 찾을 수 없습니다."));
                int rewardTime = setting.getReward();

                // 현재 날짜 기준 년월 구하기 (예: "2026-05")
                String currentYearMonth = YearMonth.now().toString();

                // 자녀의 이번 달 시간 정책 정책판 가져오기
                TimePolicy timePolicy = timePolicyRepository.findByChildIdAndYearMonth(childId, currentYearMonth)
                        .orElseThrow(() -> new IllegalArgumentException("이번 달에 설정된 자녀의 시간 정책(TimePolicy)이 없습니다."));

                // 시간 추가 정책 반영 (엔티티 내부 비즈니스 메서드 호출)
                timePolicy.addReward(rewardTime);

                // @Transactional 환경이므로 자동 반영되지만 가독성을 위해 호출 유지
                timePolicyRepository.save(timePolicy);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // [AI 예외 처리] 구글 AI 서버 오류나 네트워크 장애 발생 시 Failsafe 우회
            // DB에는 2번 단계에서 넣은 PENDING 데이터가 그대로 안전하게 유지
            aiResponse = new AiVerificationResponse(false, "AI 서버 일시 오류로 인해 부모님 확인 대기 상태로 전환되었습니다.");
            performance.updateStatusAndReason(MissionStatus.PENDING, aiResponse.reason());
        }

        //최종적으로 DB에 영속화 (Save)
        performanceRepository.save(performance);

        //프론트엔드에게 돌려줄 AI 응답 반환
        return aiResponse;
    }

    @Transactional(readOnly = true)
    public MissionPerformanceResDTO.MissionPerformanceResponse getMissionPerformance(Long missionId, Long parentId) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        if (!mission.getParent().getId().equals(parentId)) {
            throw new MissionException(MissionErrorCode.UNAUTHORIZED_ACCESS);
        }

        MissionPerformance performance = performanceRepository.findTopByMissionIdOrderByIdDesc(missionId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        return MissionPerformanceResDTO.MissionPerformanceResponse.of(performance);
    }
}