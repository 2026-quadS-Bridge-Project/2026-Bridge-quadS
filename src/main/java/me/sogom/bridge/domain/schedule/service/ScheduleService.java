package me.sogom.bridge.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.domain.schedule.dto.RoutineRequest;
import me.sogom.bridge.domain.schedule.dto.WeeklyTemplateRequest;
import me.sogom.bridge.domain.schedule.entity.DailyTimeAllocation;
import me.sogom.bridge.domain.schedule.entity.WeeklyRoutine;
import me.sogom.bridge.domain.schedule.entity.WeeklyTimeDistribution;
import me.sogom.bridge.domain.schedule.repository.DailyTimeAllocationRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyRoutineRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyTimeDistributionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WeeklyTimeDistributionRepository weeklyRepository;
    private final DailyTimeAllocationRepository dailyRepository;
    private final ChildrenRepository childrenRepository;
    private final TimePolicyRepository timePolicyRepository;
    private final WeeklyRoutineRepository routineRepository;

    //오늘의 실제 제한 시간 조회 (없으면 요일 템플릿에서 동적 복사)
    @Transactional
    public DailyTimeAllocation getOrCreateDailyAllocation(Long childId, LocalDate targetDate) {
        // 오늘 날짜의 데이터가 이미 있는지 DB에서 확인
        return dailyRepository.findByChildIdAndTargetDate(childId, targetDate)
                .orElseGet(() -> {
                    // 없다면 오늘이 무슨 요일인지 확인
                    DayOfWeek dayOfWeek = targetDate.getDayOfWeek();

                    // 해당 요일의 '기본 템플릿(Weekly)'을 가져오기
                    WeeklyTimeDistribution template = weeklyRepository.findByChildIdAndDayOfWeek(childId, dayOfWeek)
                            .orElseThrow(() -> new IllegalArgumentException("해당 요일의 기본 시간표 설정이 없습니다."));

                    // 이번 달 부모 정책 가져오기
                    String yearMonth = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    TimePolicy policy = timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth)
                            .orElseThrow(() -> new IllegalArgumentException("해당 월의 부모 정책이 설정되지 않았습니다."));

                    // 템플릿에 설정된 설정 시간만큼 기본 시간에서 선차감 진행
                    policy.deductAvailableTime(template.getBaseMinutes());

                    // 변경된 상태를 DB에 확실하게 즉시 업데이트(UPDATE)
                    timePolicyRepository.save(policy);

                    // 자녀 엔티티 조회
                    Children child = childrenRepository.findById(childId).orElseThrow();

                    // 템플릿의 시간을 복사해서 '오늘(Daily)' 데이터를 새로 저장
                    DailyTimeAllocation newAllocation = DailyTimeAllocation.builder()
                            .child(child)
                            .targetDate(targetDate)
                            .baseMinutes(template.getBaseMinutes())
                            .extendedMinutes(0) // 처음 생성될 때는 연장 시간이 0분
                            .build();

                    return dailyRepository.save(newAllocation);
                });
    }

    //자녀가 여분(보상) 시간을 사용해 오늘 시간을 연장할 때
    @Transactional
    public DailyTimeAllocation extendDailyTime(Long childId, LocalDate targetDate, int extraMinutes) {

        //날짜 변환 ("yyyy-MM" 형태)
        String yearMonth = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        //DB에서 이번 달 부모 정책 정보 가져오기
        TimePolicy policy = timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth)
                .orElseThrow(() -> new IllegalArgumentException("해당 월의 부모 정책이 설정되지 않았습니다."));

        //엔티티의 차감 로직 호출 (알아서 기본시간->보상시간 순으로 깎고, 부족하면 에러를 던짐)
        policy.deductAvailableTime(extraMinutes);

        //오늘의 스케줄 데이터를 가져오기
        DailyTimeAllocation allocation = getOrCreateDailyAllocation(childId, targetDate);

        //오늘 스케줄에 시간을 연장하기
        allocation.addExtraTime(extraMinutes);

        return allocation;
    }
    //자녀가 최초/매주 요일별 기본 가용 시간(템플릿) 설정 및 수정
    //요일별 기본 템플릿 설정 초과 등록 버그 수정
    @Transactional
    public void updateWeeklyTemplate(Long childId, WeeklyTemplateRequest request) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        TimePolicy policy = timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth)
                .orElseThrow(() -> new IllegalArgumentException("해당 월의 부모 정책이 설정되지 않았습니다."));

        //현재 DB에 저장되어 있는 이 자녀의 월~일 전체 기본 틀 시간의 합을 구함
        List<WeeklyTimeDistribution> allTemplates = weeklyRepository.findAllByChildId(childId);

        int currentWeeklySum = allTemplates.stream()
                // 지금 수정하려는 요일의 기존 시간은 제외하고 나머지 요일들만 합산
                .filter(t -> !t.getDayOfWeek().equals(request.getDayOfWeek()))
                .mapToInt(WeeklyTimeDistribution::getBaseMinutes)
                .sum();

        //여기에 새로 등록/수정하려는 요일의 시간을 더해봄
        int expectedWeeklySum = currentWeeklySum + request.getBaseMinutes();

        // 일주일 기본 틀의 합이 부모 저금통의 한도를 초과하면 즉시 차단!
        if (expectedWeeklySum > policy.getBaseTime()) {
            throw new IllegalArgumentException("주간 시간표의 총합(" + expectedWeeklySum + "분)이 부모님이 설정한 이번 달 총 가용 시간(" + policy.getBaseTime() + "분)을 초과할 수 없습니다!");
        }

        // 안전함이 검증되었을 때만 DB에 반영
        WeeklyTimeDistribution weeklyDist = weeklyRepository.findByChildIdAndDayOfWeek(childId, request.getDayOfWeek())
                .orElseGet(() -> WeeklyTimeDistribution.builder()
                        .child(policy.getChild())
                        .dayOfWeek(request.getDayOfWeek())
                        .build());

        weeklyDist.updateBaseMinutes(request.getBaseMinutes());
        weeklyRepository.save(weeklyDist);
    }

    //\학원/고정 일정(Routine) 등록
    @Transactional
    public void createRoutine(Long childId, RoutineRequest request) {
        Children child = childrenRepository.findById(childId).orElseThrow();

        WeeklyRoutine routine = WeeklyRoutine.builder()
                .child(child)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .title(request.getTitle())
                .build();

        routineRepository.save(routine);
    }

    //자녀의 주간 고정 일정 전체 조회 (시간표 조각들 목록)
    @Transactional(readOnly = true)
    public List<WeeklyRoutine> getWeeklyRoutines(Long childId) {
        return routineRepository.findAllByChild_Id(childId);
    }

    //고정 일정 삭제
    @Transactional
    public void deleteRoutine(Long routineId) {
        WeeklyRoutine routine = routineRepository.findById(routineId).orElseThrow();
        routineRepository.delete(routine);
    }
    /**
     하루 마무리 시 실제 사용 시간을 기록하고 남은 시간을 보상 풀로 환불하기
     * @param actualUsedMinutes 자녀가 오늘 실제로 스마트폰을 사용한 시간 (분 단위)
     */
    @Transactional
    public DailyTimeAllocation settleDailyTime(Long childId, LocalDate targetDate, int actualUsedMinutes) {

        // 1. 오늘의 스케줄 배정 데이터 가져오기
        DailyTimeAllocation allocation = dailyRepository.findByChildIdAndTargetDate(childId, targetDate)
                .orElseThrow(() -> new IllegalArgumentException("오늘 생성된 시간표 데이터가 없습니다."));

        // 오늘 자녀에게 주어졌던 총 가용 시간 계산 (기본시간 + 연장된시간)
        int totalAllocatedTime = allocation.getTotalAvailableTime();

        // 가드 로직: 혹시 실제 사용 시간이 준 시간보다 많으면 연산 오류이므로 방어
        if (actualUsedMinutes > totalAllocatedTime) {
            throw new IllegalArgumentException("실제 사용 시간이 할당된 총 시간을 초과할 수 없습니다.");
        }

        // 남은 시간 계산하기 (할당량 - 실제 사용량)
        int unusedMinutes = totalAllocatedTime - actualUsedMinutes;

        // 남은 시간이 존재한다면 부모 정책(TimePolicy) 데이터에 환불 절차 진행
        if (unusedMinutes > 0) {
            String yearMonth = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            TimePolicy policy = timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth)
                    .orElseThrow(() -> new IllegalArgumentException("해당 월의 부모 정책을 찾을 수 없습니다."));

            // TimePolicy의 보상 풀에 남은 시간만큼 플러스(+) 처리
            policy.refundUnusedTime(unusedMinutes);
        }

        // 오늘자 할당 기록의 baseMinutes와 extendedMinutes를 정산된 결과에 맞게 재조정
        // 가장 깔끔한 방법은 오늘 가용시간 자체를 자녀가 실제 사용한 시간으로 락(Lock)을 걸어두는 것
        // 다음 스케줄 조회 시 정산된 결과가 보이도록 엔티티 값을 세팅
        allocation.updateToSettledTime(actualUsedMinutes);

        return allocation;
    }
}