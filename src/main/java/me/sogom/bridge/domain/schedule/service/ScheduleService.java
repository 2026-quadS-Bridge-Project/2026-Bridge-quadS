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
    @Transactional
    public void updateWeeklyTemplate(Long childId, WeeklyTemplateRequest request) {
        WeeklyTimeDistribution weeklyDist = weeklyRepository.findByChildIdAndDayOfWeek(childId, request.getDayOfWeek())
                .orElseGet(() -> {
                    Children child = childrenRepository.findById(childId).orElseThrow();
                    return WeeklyTimeDistribution.builder()
                            .child(child)
                            .dayOfWeek(request.getDayOfWeek())
                            .build();
                });

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
}