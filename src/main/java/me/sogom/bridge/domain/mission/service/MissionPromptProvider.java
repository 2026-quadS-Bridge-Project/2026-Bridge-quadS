package me.sogom.bridge.domain.mission.service;

import me.sogom.bridge.domain.mission.entity.MissionCategory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MissionPromptProvider {

    // 카테고리별 맞춤형 검증 프롬프트 맵핑
    private static final Map<MissionCategory, String> PROMPTS = Map.of(
            MissionCategory.CLEANING, "사진에 쓰레기가 없고, 물건들이 정돈되어 있으며, 청소가 완료된 깨끗한 공간인지 분석해 줘.",
            MissionCategory.STUDY, "사진에 펼쳐진 책, 필기구, 노트 등 공부를 한 명확한 증거가 있는지 분석해 줘. 덜 풀린 문제가 있는 경우엔 fail 처리 해줘",
            MissionCategory.EXERCISE, "사진에 헬스장, 운동 기구, 혹은 운동복을 입고 운동 중인 모습 등 명확한 증거가 있는지 분석해 줘. 등장하는 인물이 있다면 운동을 한 모습인지 분석해줘",
            MissionCategory.READING, "사진에 글자가 선명하게 보이는 펼쳐진 책 페이지나 독서 중인 모습, 또는 독후감이 포함되어 있는지 분석해 줘. 독후감이라면 독후감의 내용이 너무 짧지 않은지 파악해줘",
            MissionCategory.ETC, "주어진 사진이 특정 미션을 성공적으로 완수한 모습인지 일반적인 기준으로 분석해 줘."
    );

    public String getPrompt(MissionCategory category) {
        // 카테고리가 null이거나 목록에 없으면 ETC(기타) 프롬프트를 기본으로 반환
        if (category == null || !PROMPTS.containsKey(category)) {
            return PROMPTS.get(MissionCategory.ETC);
        }
        return PROMPTS.get(category);
    }
}