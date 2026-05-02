package me.sogom.bridge.domain.member.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.code.MemberErrorCode;
import me.sogom.bridge.domain.member.MemberException;
import me.sogom.bridge.domain.member.dto.req.MemberReqDTO;
import me.sogom.bridge.domain.member.dto.res.MemberResDTO;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.member.repository.ParentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final ChildrenRepository childrenRepository;

    /**
     * 부모 - 자녀 등록
     * @param parentId 부모 ID
     * @param request 자녀 등록 요청 (자녀 이름, 자녀 코드, 프로필 사진 URL)
     */
    @Transactional
    public void registerChild(Long parentId, MemberReqDTO.RegisterChildRequest request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Children children = childrenRepository.findByCode(request.childrenCode())
                .orElseThrow(() -> new MemberException(MemberErrorCode.CHILDREN_NOT_FOUND));

        // 이미 다른 부모와 연결된 자녀인지 확인
        if (children.getParent() != null) {
            throw new MemberException(MemberErrorCode.CHILDREN_ALREADY_REGISTERED);
        }

        // 자녀 정보 업데이트
        children.setParent(parent);
        children.setBirth(request.birth());
        children.setProfileImageUrl(request.profileImageUrl());

        childrenRepository.save(children);
        parent.getChildren().add(children);
        parentRepository.save(parent);
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
                .map(MemberResDTO.ChildrenInfoResponse::of)
                .collect(Collectors.toList());
    }
}

