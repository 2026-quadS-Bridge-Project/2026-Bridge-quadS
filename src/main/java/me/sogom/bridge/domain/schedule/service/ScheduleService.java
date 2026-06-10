package me.sogom.bridge.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.member.entity.Children;
import me.sogom.bridge.domain.member.entity.Parent;
import me.sogom.bridge.domain.member.repository.ChildrenRepository;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.domain.policy.entity.TimePolicy;
import me.sogom.bridge.domain.policy.repository.TimePolicyRepository;
import me.sogom.bridge.domain.schedule.dto.RoutineRequest;
import me.sogom.bridge.domain.schedule.dto.WeeklyBudgetRequest;
import me.sogom.bridge.domain.schedule.dto.WeeklyTemplateRequest;
import me.sogom.bridge.domain.schedule.dto.DailyScheduleResponse;
import me.sogom.bridge.domain.schedule.entity.DailyTimeAllocation;
import me.sogom.bridge.domain.schedule.entity.WeeklyBudget;
import me.sogom.bridge.domain.schedule.entity.WeeklyRoutine;
import me.sogom.bridge.domain.schedule.entity.WeeklyTimeDistribution;
import me.sogom.bridge.domain.schedule.repository.DailyTimeAllocationRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyBudgetRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyRoutineRepository;
import me.sogom.bridge.domain.schedule.repository.WeeklyTimeDistributionRepository;
import me.sogom.bridge.global.apiPayload.code.GeneralErrorCode;
import me.sogom.bridge.global.apiPayload.exception.ProjectException;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WeeklyTimeDistributionRepository weeklyRepository;
    private final DailyTimeAllocationRepository dailyRepository;
    private final ChildrenRepository childrenRepository;
    private final TimePolicyRepository timePolicyRepository;
    private final WeeklyRoutineRepository routineRepository;
    private final WeeklyBudgetRepository weeklyBudgetRepository;
    private final NotificationService notificationService;

    //오늘의 실제 제한 시간 조회 (없으면 요일 템플릿에서 동적 복사)
    @Transactional
    public DailyTimeAllocation getOrCreateDailyAllocation(Long childId, LocalDate targetDate) {
        // 오늘 날짜의 데이터가 이미 있는지 DB에서 확인
        return dailyRepository.findByChildIdAndTargetDate(childId, targetDate)
                .orElseGet(() -> {
                    DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
                    String yearMonth = yearMonthOf(targetDate);
                    int weekNumber = weekNumberOf(targetDate);

                    WeeklyTimeDistribution template = weeklyRepository.findByChildIdAndDayOfWeekAndYearMonthAndWeekNumber(
                                    childId, dayOfWeek, yearMonth, weekNumber)
                            .orElseThrow(() -> new IllegalArgumentException("해당 요일의 기본 시간표 설정이 없습니다."));

                    // 이번 달 부모 정책 가져오기
                    TimePolicy policy = timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth)
                            .orElseThrow(() -> new IllegalArgumentException("해당 월의 부모 정책이 설정되지 않았습니다."));

                    // 템플릿에 설정된 시간만큼 기본 시간에서 선차감 진행
                    policy.deductAvailableTime(template.getBaseMinutes());
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

    @Transactional(readOnly = true)
    public boolean hasChildPlan(Long childId, String yearMonth) {
        if (timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth).isEmpty()) {
            return false;
        }

        List<WeeklyBudget> budgets = weeklyBudgetRepository.findAllByChildIdAndYearMonth(childId, yearMonth);
        Set<Integer> budgetWeeks = budgets.stream()
                .map(WeeklyBudget::getWeekNumber)
                .collect(Collectors.toSet());
        Map<Integer, Integer> templateMinutesByWeek = weeklyRepository.findAllByChildIdAndYearMonth(childId, yearMonth).stream()
                .collect(Collectors.groupingBy(
                        WeeklyTimeDistribution::getWeekNumber,
                        Collectors.summingInt(WeeklyTimeDistribution::getBaseMinutes)
                ));

        Set<Integer> requiredWeeks = Set.of(1, 2, 3, 4);
        if (!budgetWeeks.containsAll(requiredWeeks) || !templateMinutesByWeek.keySet().containsAll(requiredWeeks)) {
            return false;
        }

        boolean weeklyTemplatesMatchBudgets = budgets.stream()
                .filter(budget -> requiredWeeks.contains(budget.getWeekNumber()))
                .allMatch(budget ->
                        templateMinutesByWeek.getOrDefault(budget.getWeekNumber(), 0)
                                == budget.getAllocatedMinutes()
                );
        if (!weeklyTemplatesMatchBudgets) {
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public Optional<Integer> findMonthlyBudgetTotal(Long childId, String yearMonth) {
        List<WeeklyBudget> budgets = weeklyBudgetRepository.findAllByChildIdAndYearMonth(childId, yearMonth);
        if (budgets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(budgets.stream()
                .mapToInt(WeeklyBudget::getAllocatedMinutes)
                .sum());
    }

    @Transactional
    public void clearChildPlan(Long childId, String yearMonth) {
        weeklyBudgetRepository.deleteAll(weeklyBudgetRepository.findAllByChildIdAndYearMonth(childId, yearMonth));
        weeklyRepository.deleteAll(weeklyRepository.findAllByChildIdAndYearMonth(childId, yearMonth));

        YearMonth targetMonth = YearMonth.parse(yearMonth);
        dailyRepository.deleteAll(dailyRepository.findAllByChildIdAndTargetDateBetween(
                childId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        ));
    }

    @Transactional
    public void completeTimePlan(Long childId, String yearMonth) {
        if (!hasChildPlan(childId, yearMonth)) {
            throw new IllegalArgumentException("시간 계획이 아직 완료되지 않았습니다.");
        }

        Children child = childrenRepository.findById(childId).orElseThrow();
        Parent parent = child.getParent();
        if (parent == null) {
            return;
        }

        notificationService.createNotification(
                parent.getId(),
                MemberRole.PARENT,
                "시간 계획 완료",
                child.getName() + "님이 " + yearMonth + " 사용 시간 설정을 완료했습니다.",
                NotificationType.GENERAL,
                child.getId(),
                null,
                null,
                "/today-time?childrenId=" + child.getId()
        );
    }

    @Transactional(readOnly = true)
    public Optional<DailyScheduleResponse> findDailySchedulePreview(Long childId, LocalDate targetDate) {
        Optional<DailyTimeAllocation> allocation = dailyRepository.findByChildIdAndTargetDate(childId, targetDate);
        if (allocation.isPresent()) {
            return allocation.map(DailyScheduleResponse::from);
        }

        String yearMonth = yearMonthOf(targetDate);
        int weekNumber = weekNumberOf(targetDate);
        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();

        return weeklyRepository.findByChildIdAndDayOfWeekAndYearMonthAndWeekNumber(
                        childId, dayOfWeek, yearMonth, weekNumber)
                .map(template -> DailyScheduleResponse.preview(targetDate, template.getBaseMinutes(), 0));
    }

    public String yearMonthOf(LocalDate targetDate) {
        return targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    public int weekNumberOf(LocalDate targetDate) {
        return Math.min(((targetDate.getDayOfMonth() - 1) / 7) + 1, 4);
    }

    //자녀가 여분(보상) 시간을 사용해 오늘 시간을 연장할 때
    @Transactional
    public DailyTimeAllocation extendDailyTime(Long childId, LocalDate targetDate, int extraMinutes) {
        if (targetDate == null) {
            throw new IllegalArgumentException("연장할 날짜를 입력해 주세요.");
        }
        if (extraMinutes <= 0) {
            throw new IllegalArgumentException("연장할 시간은 1분 이상이어야 합니다.");
        }

        //날짜 변환 ("yyyy-MM" 형태)
        String yearMonth = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        //DB에서 이번 달 부모 정책 정보 가져오기
        TimePolicy policy = timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth)
                .orElseThrow(() -> new IllegalArgumentException("해당 월의 부모 정책이 설정되지 않았습니다."));

        // 기본 시간에서 먼저 차감하고, 부족한 경우 보상 시간에서 차감한다.
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
        validateWeeklyTemplateRequest(request);

        // 해당 월/주차에 자녀가 설정해둔 예산(WeeklyBudget) 가져오기
        WeeklyBudget weeklyBudget = weeklyBudgetRepository.findByChildIdAndYearMonthAndWeekNumber(
                        childId, request.getYearMonth(), request.getWeekNumber())
                .orElseThrow(() -> new IllegalArgumentException(request.getWeekNumber() + "주차의 예산이 설정되지 않았습니다. 주차별 예산을 먼저 분배해주세요."));

        // 해당 주차에 이미 등록된 월~일 템플릿 시간의 합 구하기
        List<WeeklyTimeDistribution> weekTemplates = weeklyRepository.findAllByChildIdAndYearMonthAndWeekNumber(
                childId, request.getYearMonth(), request.getWeekNumber());

        int currentSum = weekTemplates.stream()
                .filter(t -> !t.getDayOfWeek().equals(request.getDayOfWeek())) // 지금 설정하려는 요일은 제외하고 합산
                .mapToInt(WeeklyTimeDistribution::getBaseMinutes)
                .sum();

        int expectedSum = currentSum + request.getBaseMinutes();

        // 요일 합계가 '해당 주차의 예산'을 넘는지 검증
        if (expectedSum > weeklyBudget.getAllocatedMinutes()) {
            throw new IllegalArgumentException(request.getWeekNumber() + "주차의 설정 총합(" + expectedSum + "분)이 이번 주 예산(" + weeklyBudget.getAllocatedMinutes() + "분)을 초과할 수 없습니다.");
        }

        // 검증 통과 시 DB에 저장 (또는 기존 데이터 업데이트)
        WeeklyTimeDistribution template = weeklyRepository.findByChildIdAndDayOfWeekAndYearMonthAndWeekNumber(
                        childId, request.getDayOfWeek(), request.getYearMonth(), request.getWeekNumber())
                .orElseGet(() -> WeeklyTimeDistribution.builder()
                        .child(weeklyBudget.getChild())
                        .yearMonth(request.getYearMonth())
                        .weekNumber(request.getWeekNumber())
                        .dayOfWeek(request.getDayOfWeek())
                        .build());

        template.updateBaseMinutes(request.getBaseMinutes());
        weeklyRepository.save(template);
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
                .title("고정 시간") // 프론트에서 받지 않고 서버에서 기본값 할당
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
    public void deleteRoutine(Long childId, Long routineId) {
        WeeklyRoutine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
        if (routine.getChild() == null || !routine.getChild().getId().equals(childId)) {
            throw new ProjectException(GeneralErrorCode.FORBIDDEN);
        }
        routineRepository.delete(routine);
    }
    /**
     하루 마무리 시 실제 사용 시간을 기록하고 남은 시간을 보상 풀로 환불한다.
     * @param actualUsedMinutes 자녀가 오늘 실제로 스마트폰을 사용한 시간 (분 단위)
     */
    @Transactional
    public DailyTimeAllocation settleDailyTime(Long childId, LocalDate targetDate, int actualUsedMinutes) {
        if (actualUsedMinutes < 0) {
            throw new IllegalArgumentException("실제 사용 시간은 음수일 수 없습니다.");
        }

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

            policy.refundUnusedTime(unusedMinutes);
        }

        // 오늘자 할당 기록의 baseMinutes와 extendedMinutes를 정산된 결과에 맞게 재조정
        // 가장 깔끔한 방법은 오늘 가용시간 자체를 자녀가 실제 사용한 시간으로 락(Lock)을 걸어두는 것
        // 다음 스케줄 조회 시 정산된 결과가 보이도록 엔티티 값을 세팅
        allocation.updateToSettledTime(actualUsedMinutes);

        return allocation;
    }
    @Transactional
    public void createWeeklyBudgets(Long childId, String yearMonth, List<WeeklyBudgetRequest> requests) {
        // 1. 부모가 설정한 이번 달 기본 시간 풀 조회
        TimePolicy policy = timePolicyRepository.findByChildIdAndYearMonth(childId, yearMonth)
                .orElseThrow(() -> new IllegalArgumentException("정책이 없습니다."));

        validateFourWeekBudgetRequests(requests);

        // 2. 자녀가 요청한 1~4주차 시간의 총합 계산
        int totalRequestedMinutes = requests.stream()
                .mapToInt(WeeklyBudgetRequest::getAllocatedMinutes)
                .sum();

        // 3. 한 달 총량을 초과하는지 검증
        if (totalRequestedMinutes > policy.getBaseTime()) {
            throw new IllegalArgumentException("주차별 분배 시간이 한 달 총량을 초과할 수 없습니다.");
        }

        // 기존 데이터 조회
        List<WeeklyBudget> existingBudgets =
                weeklyBudgetRepository.findAllByChildIdAndYearMonth(
                        childId,
                        yearMonth
                );

        // 기존 데이터가 있다면 삭제 후 새로 저장한다.
        // 템플릿도 함께 비워야 재시도/재설정 시 이전 요일이 섞이지 않는다.
        weeklyBudgetRepository.deleteAll(existingBudgets);
        weeklyRepository.deleteAll(weeklyRepository.findAllByChildIdAndYearMonth(childId, yearMonth));

        Children child = childrenRepository.findById(childId).orElseThrow();

        List<WeeklyBudget> budgets = requests.stream()
                .map(req -> WeeklyBudget.builder()
                        .child(child)
                        .yearMonth(yearMonth)
                        .weekNumber(req.getWeekNumber())
                        .allocatedMinutes(req.getAllocatedMinutes())
                        .build())
                .toList();

        weeklyBudgetRepository.saveAll(budgets);
    }

    private void validateFourWeekBudgetRequests(List<WeeklyBudgetRequest> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("1~4주차 예산을 모두 한 번씩 분배해야 합니다.");
        }
        if (requests.stream().anyMatch(request -> request == null)) {
            throw new IllegalArgumentException("1~4주차 예산을 모두 한 번씩 분배해야 합니다.");
        }

        Set<Integer> requiredWeeks = Set.of(1, 2, 3, 4);
        Set<Integer> requestedWeeks = requests.stream()
                .map(WeeklyBudgetRequest::getWeekNumber)
                .collect(Collectors.toSet());

        if (requests.size() != requiredWeeks.size() || !requestedWeeks.equals(requiredWeeks)) {
            throw new IllegalArgumentException("1~4주차 예산을 모두 한 번씩 분배해야 합니다.");
        }

        boolean hasNonPositiveBudget = requests.stream()
                .anyMatch(request -> request.getAllocatedMinutes() <= 0);
        if (hasNonPositiveBudget) {
            throw new IllegalArgumentException("각 주차 예산은 0분보다 커야 합니다.");
        }
    }

    private void validateWeeklyTemplateRequest(WeeklyTemplateRequest request) {
        if (request == null
                || request.getYearMonth() == null
                || request.getYearMonth().isBlank()
                || request.getDayOfWeek() == null) {
            throw new IllegalArgumentException("요일별 시간 설정 값이 올바르지 않습니다.");
        }
        if (request.getWeekNumber() < 1 || request.getWeekNumber() > 4) {
            throw new IllegalArgumentException("주차는 1~4주차만 설정할 수 있습니다.");
        }
        if (request.getBaseMinutes() < 0) {
            throw new IllegalArgumentException("요일별 기본 시간은 음수일 수 없습니다.");
        }
    }
}
