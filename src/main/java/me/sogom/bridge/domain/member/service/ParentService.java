package me.sogom.bridge.domain.member.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.fcm.service.FcmService;
import me.sogom.bridge.domain.member.MemberException;
import me.sogom.bridge.domain.member.code.MemberErrorCode;
import me.sogom.bridge.domain.member.dto.req.MemberReqDTO;
import me.sogom.bridge.domain.member.dto.res.MemberResDTO;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.MemberStatus;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import me.sogom.bridge.domain.policy.dto.PolicyReqDTO;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.global.security.entity.MemberRole;
import me.sogom.bridge.global.storage.PhotoUrlResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;
    private final TimePolicyRepository timePolicyRepository;

    // FCM 서비스
    private final FcmService fcmService;

    // 프로필 이미지 key → presigned URL 변환
    private final PhotoUrlResolver photoUrlResolver;

    /**
     * 부모 - 자녀 등록
     * @param parentId 부모 ID
     * @param request 자녀 등록 요청 (자녀 이름, 자녀 코드, 프로필 사진 URL)
     */
    @Transactional
    public MemberResDTO.ChildrenInfoResponse registerChild(Long parentId, MemberReqDTO.RegisterChildRequest request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Children children = childrenRepository.findByCodeAndStatus(request.childrenCode(), MemberStatus.ACTIVE)
                .orElseThrow(() -> new MemberException(MemberErrorCode.CHILDREN_NOT_FOUND));

        // 이미 다른 부모와 연결된 자녀인지 확인
        if (children.getParent() != null) {
            throw new MemberException(MemberErrorCode.CHILDREN_ALREADY_REGISTERED);
        }

        // 자녀 정보 업데이트
        children.setParent(parent);
        children.setBirth(request.childrenBirth());
        children.setProfileImageKey(request.profileImageKey());

        childrenRepository.save(children);
        parent.getChildren().add(children);
        parentRepository.save(parent);

        return MemberResDTO.ChildrenInfoResponse.of(children);
    }

    /**
     * 부모와 연결된 자녀 목록 조회
     * @param parentId 부모 ID
     * @return 자녀 정보 목록 (자녀 ID, 이름, 프로필 URL)
     */
    @Transactional(readOnly = true)
    public List<MemberResDTO.ChildrenInfoResponse> getChildrenList(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        return parent.getChildren().stream()
                .map(child -> MemberResDTO.ChildrenInfoResponse.of(
                        child,
                        photoUrlResolver.resolveOrNull(child.getProfileImageKey())
                ))
                .collect(Collectors.toList());
    }

    /**
     * 자녀의 총시간 설정
     *
     * @param parentId 부모 ID
     * @param request  시간 정책 설정 요청 (자녀 ID, 년월, 기본 시간)
     */
    @Transactional
    public void setTimePolicy(Long parentId, PolicyReqDTO.SetTimePolicyRequest request) {

        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Children child = childrenRepository.findById(request.childId())
                .orElseThrow(() -> new MemberException(MemberErrorCode.CHILDREN_NOT_FOUND));

        // 자녀가 해당 부모와 연결되어 있는지 확인
        if (!child.getParent().getId().equals(parentId)) {
            throw new MemberException(MemberErrorCode.CHILDREN_PARENT_MISMATCH);
        }

        timePolicyRepository.findByChildIdAndYearMonth(request.childId(), request.yearMonth())
                .ifPresentOrElse(

                        // 이미 존재하는 경우 기본 시간 수정
                        policy -> policy.updateBaseTime(request.baseTime()),

                        // 존재하지 않는 경우 새 정책 생성
                        () -> {
                            TimePolicy newPolicy = TimePolicy.builder()
                                    .parent(parent)
                                    .child(child)
                                    .yearMonth(request.yearMonth())
                                    .baseTime(request.baseTime())
                                    .accumulatedRewardTime(0)
                                    .build();
                            timePolicyRepository.save(newPolicy);
                        }
                );

        // 자녀 앱 정책 갱신 Silent Push 전송
        fcmService.sendSilentPush(
                child.getId(),
                MemberRole.CHILDREN,
                "TIME_POLICY_UPDATED"
        );
    }
}