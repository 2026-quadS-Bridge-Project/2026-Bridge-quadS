package me.sogom.bridge.domain.member.util;

import java.util.Random;

public class ChildrenCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final Random RANDOM = new Random();

    /**
     * 자녀 연동 코드 생성 (영문 대문자 + 숫자 조합, 8자리)
     * @return 생성된 자녀 코드 (예: A7K123Q9)
     */
    public static String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
