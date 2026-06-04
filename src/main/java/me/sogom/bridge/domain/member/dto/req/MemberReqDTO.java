package me.sogom.bridge.domain.member.dto.req;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MemberReqDTO {

    public record LoginRequest(

            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            String password
    ) {}

    public record RefreshRequest(
            @NotBlank(message = "리프레시 토큰을 입력해 주세요.")
            String refreshToken
    ) {}

    public record SignUpRequest(

            @NotBlank(message = "이름을 입력해 주세요.")
            String name,

            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{12,15}$",
                    message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 포함한 12~15자여야 합니다."
            )
            String password
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "기존 비밀번호를 입력해 주세요.")
            String oldPassword,

            @NotBlank(message = "새 비밀번호를 입력해 주세요.")
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{12,15}$",
                    message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 포함한 12~15자여야 합니다."
            )
            String newPassword
    ) {}

    public record RegisterChildRequest(

            @NotBlank(message = "자녀 이름을 입력해 주세요.")
            String childrenName,

            @Nullable
            String childrenBirth,

            @NotBlank(message = "자녀 코드를 입력해 주세요.")
            String childrenCode,

            String profileImageKey // S3 key returned by POST /api/v1/files/photos (optional)
    ) {}

}
