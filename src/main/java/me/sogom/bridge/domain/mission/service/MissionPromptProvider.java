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
            MissionCategory.ERRAND, "사진에 구매한 물품, 영수증, 심부름 장소(마트, 우체국 등)의 배경, 또는 심부름을 수행 중인 명확한 증거가 있는지 분석해 줘. 요청받은 심부름 내용과 사진 속 물품이 일치하지 않는 경우엔 fail 처리 해줘.",
            MissionCategory.ROUTINE, "사진에 영양제/물 섭취, 일기장 작성, 기상 인증 등 부모가 입력한 미션에 대해 완수한 명확한 증거가 있는지 분석해 줘. 무관한 일상 사진이거나 성의가 없는 경우엔 fail 처리 해줘."
    );

    public String getPrompt(MissionCategory category) { if (category == null || !PROMPTS.containsKey(category)) {
        throw new IllegalArgumentException("유효하지 않은 미션 카테고리입니다."); }
        return PROMPTS.get(category); }
}