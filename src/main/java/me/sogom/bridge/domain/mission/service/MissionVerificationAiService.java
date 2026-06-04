package me.sogom.bridge.domain.mission.service;

import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class MissionVerificationAiService {

    private final ChatClient chatClient;
    private final MissionPromptProvider promptProvider; //프롬프트 제공자 추가

    //생성자를 통해 PromptProvider 주입
    public MissionVerificationAiService(ChatClient.Builder builder, MissionPromptProvider promptProvider) {
        this.chatClient = builder.build();
        this.promptProvider = promptProvider;
    }

    //파라미터에 'MissionCategory category' 추가
    public AiVerificationResponse verifyMissionImage(MultipartFile imageFile, MissionCategory category, String parentPrompt) throws IOException {

        // 람다식(u -> u) 안으로 들어가기 전에 미리 getBytes()를 실행
        // 여기서 발생하는 에러는 메서드에 붙은 throws IOException이 처리
        byte[] imageBytes = imageFile.getBytes();

        //카테고리에 맞는 AI 판독 기준 가져오기
        String categoryPrompt = promptProvider.getPrompt(category);

        //부모님이 직접 작성한 요구사항이 있다면 합치기 (없으면 카테고리 기본값만 사용)
        String finalCondition = (parentPrompt != null && !parentPrompt.isBlank())
                ? "부모님 특별 요청: [" + parentPrompt + "]\n기본 검증 기준: [" + categoryPrompt + "]"
                : "기본 검증 기준: [" + categoryPrompt + "]";

        // String.format을 이용해 %s 자리에 finalCondition 주입
        String systemInstruction = String.format("""
    당신은 학생들의 올바른 습관 형성을 돕는 전문적이고 이성적인 'AI 학습 멘토'입니다.
    이번 미션의 통과 기준은 다음과 같습니다:
    %s
    
    [평가 원칙: 융통성과 변별력의 조화]
    1. 객관적 기준 적용: 미션의 핵심 요소가 누락되었다면 냉정하게 거절(false)하세요. 
       - 예: '수학 문제 3쪽 풀기' 미션인데 1쪽만 풀려있거나, 빈 칸이 많다면 미통과입니다.
       - 예: '방 청소' 미션인데 바닥에 쓰레기가 그대로 있다면 미통과입니다.
    2. 합리적 융통성: 사람이 하는 일이기에 생길 수 있는 작은 실수는 허용합니다.
       - 예: 이불이 완벽하게 칼각은 아니더라도 정돈된 상태라면 통과입니다.
       - 예: 문제집 풀이 중 한두 문제 정도 고민한 흔적이 보이나 해결하지 못한 것은 통과입니다.
    3. 부정행위 엄단: 인터넷 화면을 찍었거나, 예전에 찍은 사진을 재활용한 느낌이 들거나, 미션과 전혀 상관없는 사진은 즉시 거절하세요.

    [피드백(reason) 작성 원칙]
    - 논리적 설명: 단순히 좋거나 나쁘다는 감정 표현 대신, "어떤 부분은 기준을 충족했고, 어떤 부분은 보완이 필요하다"는 식으로 논리적으로 설명하세요.
    - 성숙한 말투: 유치한 말투는 지양하고, 학생을 인격적으로 존중하는 예의 바른 '해요체'를 사용하세요. (멘토가 조언해 주는 느낌)
    - 성장 촉진: 통과 시에는 성취감을 느낄 수 있는 짧은 격려를, 미통과 시에는 다음 도전을 위한 명확한 가이드를 제시하세요.

    [출력 형식]
    반드시 JSON 형식으로 응답하세요.
    - isAccepted: (true/false)
    - reason: (논리적이고 성숙한 피드백 메시지)
    """, finalCondition); // 여기에 주입

        return chatClient.prompt()
                .user(u -> u
                        .text(systemInstruction)
                        // 변환해둔 imageBytes를 그대로 사용
                        .media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageBytes))
                )
                .call()
                .entity(AiVerificationResponse.class);
    }
}