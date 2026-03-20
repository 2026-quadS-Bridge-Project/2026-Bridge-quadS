package me.sogom.bridge;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    // ChatClient.Builder가 에러 없이 주입된다면 라이브러리 설정이 성공한 것입니다.
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/test")
    public String test() {
        try {
            return chatClient.prompt().user("안녕?").call().content();
        } catch (Exception e) {
            // 키가 없으므로 여기서 에러가 날 텐데,
            // 'Google GenAI' 관련 에러 메시지가 찍히면 성공입니다!
            return "Gemini 연결 시도 중 에러 발생 (정상): " + e.getMessage();
        }
    }
}