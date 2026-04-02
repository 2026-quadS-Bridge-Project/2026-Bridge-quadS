package me.sogom.bridge.domain.mission.service;

import me.sogom.bridge.domain.mission.dto.AiVerificationResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class MissionVerificationAiService {

    private final ChatClient chatClient;

    public MissionVerificationAiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public AiVerificationResponse verifyMissionImage(MultipartFile imageFile, String parentPrompt) throws IOException {

        // 람다식(u -> u) 안으로 들어가기 전에 미리 getBytes()를 실행
        // 여기서 발생하는 에러는 메서드에 붙은 throws IOException이 처리
        byte[] imageBytes = imageFile.getBytes();

        String systemInstruction = "당신은 아이의 미션 수행 여부를 판단하는 AI 감독관입니다. " +
                "다음은 부모님이 설정한 통과 기준입니다: [" + parentPrompt + "] " +
                "사진을 보고 기준에 부합하는지 엄격하게 판단하여 결과를 알려주세요.";

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