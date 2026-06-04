package me.sogom.bridge.domain.mission.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import me.sogom.bridge.domain.mission.entity.ResetCycle;
import me.sogom.bridge.domain.mission.entity.VerificationType;

public class MissionReqDTO {

    public record CreateMissionRequest(

            @NotNull(message = "자녀 ID를 입력해 주세요.")
            Long childId,

            @NotBlank(message = "미션 이름을 입력해 주세요.")
            @Size(max = 50, message = "미션 이름은 50자 이하로 입력해 주세요.")
            String title,

            @NotNull(message = "카테고리를 선택해 주세요.")
            MissionCategory category,

            @NotNull(message = "리셋 주기를 선택해 주세요.")
            ResetCycle resetCycle,

            @NotNull(message = "확인 방식을 선택해 주세요.")
            VerificationType verificationType,

            @NotNull(message = "지급 시간을 입력해 주세요.")
            @PositiveOrZero(message = "지급 시간은 0 이상이어야 합니다.")
            Integer reward,

            @Size(max = 500, message = "상세 설명은 500자 이하로 입력해 주세요.")
            String description
    ) {}
}
