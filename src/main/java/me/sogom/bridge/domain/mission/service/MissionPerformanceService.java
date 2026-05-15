package me.sogom.bridge.domain.mission.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.entity.Mission;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import me.sogom.bridge.domain.mission.entity.MissionPerformance;
import me.sogom.bridge.domain.mission.entity.MissionStatus;
import me.sogom.bridge.domain.mission.exception.MissionErrorCode;
import me.sogom.bridge.domain.mission.exception.MissionException;
import me.sogom.bridge.domain.mission.repository.MissionPerformanceRepository;
import me.sogom.bridge.domain.mission.repository.MissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MissionPerformanceService {

    private final MissionPerformanceRepository performanceRepository;
    private final MissionRepository missionRepository;
    private final ChildrenRepository childrenRepository; // 자녀 조회
    private final MissionVerificationAiService aiService;

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

        //(Builder 패턴을 사용해서 객체 조립 (아직 이미지 X)
        MissionPerformance performance = MissionPerformance.builder()
                .mission(mission)
                .child(child)
                .status(aiResponse.isAccepted() ? MissionStatus.ACCEPTED : MissionStatus.REJECTED)
                .reason(aiResponse.reason())
                .build();
        //사진 URL은 나중에 S3 같은 곳에 올리고 URL을 받아 넣어야 함

        //최종적으로 DB에 영속화 (Save)
        performanceRepository.save(performance);

        //프론트엔드에게 돌려줄 AI 응답 반환
        return aiResponse;
    }
}