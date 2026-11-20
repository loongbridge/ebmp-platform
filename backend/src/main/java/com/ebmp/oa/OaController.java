package com.ebmp.oa;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.BizException;
import com.ebmp.common.PageResult;
import com.ebmp.common.SecurityUtils;
import com.ebmp.system.DeptRepository;
import com.ebmp.system.User;
import com.ebmp.system.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oa")
@RequiredArgsConstructor
public class OaController {

    private final NoticeRepository noticeRepository;
    private final OaTaskRepository taskRepository;
    private final ScheduleRepository scheduleRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingAttendeeRepository meetingAttendeeRepository;
    private final LeaveRepository leaveRepository;
    private final LeaveService leaveService;
    private final AttendanceRepository attendanceRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DeptRepository deptRepository;

    @GetMapping("/notices")
    public ApiResponse<PageResult<Notice>> notices(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        var result = noticeRepository.findAllByOrderByIdDesc(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        result.getContent().forEach(this::enrichNotice);
        return ApiResponse.ok(PageResult.of(result));
    }

    @PostMapping("/notices")
    public ApiResponse<Notice> createNotice(@RequestBody Notice body) {
        body.setId(null);
        body.setPublisherId(SecurityUtils.currentUserId());
        body.setPublishTime(body.getPublishTime() == null ? LocalDateTime.now() : body.getPublishTime());
        return ApiResponse.ok(noticeRepository.save(body));
    }

    @PutMapping("/notices/{id}")
    public ApiResponse<Notice> updateNotice(@PathVariable Long id, @RequestBody Notice body) {
        Notice n = noticeRepository.findById(id).orElseThrow(() -> new BizException("公告不存在"));
        n.setTitle(body.getTitle());
        n.setContent(body.getContent());
        n.setCategory(body.getCategory());
        n.setStatus(body.getStatus());
        return ApiResponse.ok(noticeRepository.save(n));
    }

    @DeleteMapping("/notices/{id}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long id) {
        noticeRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/tasks")
    public ApiResponse<List<OaTask>> tasks(@RequestParam(defaultValue = "false") boolean mine,
                                           @RequestParam(required = false) String status) {
        List<OaTask> list = mine ? taskRepository.findByAssigneeIdOrderByIdDesc(SecurityUtils.currentUserId())
                : taskRepository.findAllByOrderByIdDesc();
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(t -> status.equals(t.getStatus())).toList();
        }
        list.forEach(this::enrichTask);
        return ApiResponse.ok(list);
    }

    @PostMapping("/tasks")
    public ApiResponse<OaTask> createTask(@RequestBody OaTask body) {
        body.setId(null);
        body.setCreatorId(SecurityUtils.currentUserId());
        if (body.getStatus() == null) {
            body.setStatus("TODO");
        }
        return ApiResponse.ok(taskRepository.save(body));
    }

    @PutMapping("/tasks/{id}")
    public ApiResponse<OaTask> updateTask(@PathVariable Long id, @RequestBody OaTask body) {
        OaTask t = taskRepository.findById(id).orElseThrow(() -> new BizException("任务不存在"));
        t.setTitle(body.getTitle());
        t.setDescription(body.getDescription());
        t.setAssigneeId(body.getAssigneeId());
        t.setDueDate(body.getDueDate());
        t.setPriority(body.getPriority());
        t.setStatus(body.getStatus());
        if ("DONE".equals(body.getStatus())) {
            t.setCompletedAt(LocalDateTime.now());
        }
        return ApiResponse.ok(taskRepository.save(t));
    }

    @PostMapping("/tasks/{id}/complete")
    public ApiResponse<OaTask> completeTask(@PathVariable Long id) {
        OaTask t = taskRepository.findById(id).orElseThrow(() -> new BizException("任务不存在"));
        t.setStatus("DONE");
        t.setCompletedAt(LocalDateTime.now());
        return ApiResponse.ok(taskRepository.save(t));
    }

    @DeleteMapping("/tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        taskRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/schedules")
    public ApiResponse<List<Schedule>> schedules(@RequestParam(defaultValue = "false") boolean mine) {
        List<Schedule> list = mine
                ? scheduleRepository.findByOwnerIdOrderByStartTimeAsc(SecurityUtils.currentUserId())
                : scheduleRepository.findAll();
        list.forEach(s -> userRepository.findById(s.getOwnerId() == null ? -1L : s.getOwnerId())
                .ifPresent(u -> s.setOwnerName(u.getRealName())));
        return ApiResponse.ok(list);
    }

    @PostMapping("/schedules")
    public ApiResponse<Schedule> createSchedule(@RequestBody Schedule body) {
        body.setId(null);
        if (body.getOwnerId() == null) {
            body.setOwnerId(SecurityUtils.currentUserId());
        }
        return ApiResponse.ok(scheduleRepository.save(body));
    }

    @PutMapping("/schedules/{id}")
    public ApiResponse<Schedule> updateSchedule(@PathVariable Long id, @RequestBody Schedule body) {
        Schedule s = scheduleRepository.findById(id).orElseThrow(() -> new BizException("日程不存在"));
        s.setTitle(body.getTitle());
        s.setDescription(body.getDescription());
        s.setStartTime(body.getStartTime());
        s.setEndTime(body.getEndTime());
        s.setType(body.getType());
        s.setLocation(body.getLocation());
        s.setStatus(body.getStatus());
        return ApiResponse.ok(scheduleRepository.save(s));
    }

    @DeleteMapping("/schedules/{id}")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/meetings")
    public ApiResponse<List<Meeting>> meetings() {
        List<Meeting> list = meetingRepository.findAllByOrderByStartTimeDesc();
        list.forEach(m -> {
            userRepository.findById(m.getOrganizerId() == null ? -1L : m.getOrganizerId())
                    .ifPresent(u -> m.setOrganizerName(u.getRealName()));
            List<String> names = new ArrayList<>();
            List<Long> ids = new ArrayList<>();
            for (MeetingAttendee a : meetingAttendeeRepository.findByMeetingId(m.getId())) {
                ids.add(a.getUserId());
                userRepository.findById(a.getUserId()).ifPresent(u -> names.add(u.getRealName()));
            }
            m.setAttendeeNames(names);
            m.setAttendeeIds(ids);
        });
        return ApiResponse.ok(list);
    }

    @PostMapping("/meetings")
    public ApiResponse<Meeting> createMeeting(@RequestBody Meeting body) {
        body.setId(null);
        body.setOrganizerId(SecurityUtils.currentUserId());
        Meeting saved = meetingRepository.save(body);
        saveAttendees(saved.getId(), body.getAttendeeIds());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/meetings/{id}")
    public ApiResponse<Meeting> updateMeeting(@PathVariable Long id, @RequestBody Meeting body) {
        Meeting m = meetingRepository.findById(id).orElseThrow(() -> new BizException("会议不存在"));
        m.setTitle(body.getTitle());
        m.setRoom(body.getRoom());
        m.setStartTime(body.getStartTime());
        m.setEndTime(body.getEndTime());
        m.setStatus(body.getStatus());
        m.setRemark(body.getRemark());
        meetingRepository.save(m);
        saveAttendees(id, body.getAttendeeIds());
        return ApiResponse.ok(m);
    }

    @DeleteMapping("/meetings/{id}")
    public ApiResponse<Void> deleteMeeting(@PathVariable Long id) {
        meetingAttendeeRepository.deleteByMeetingId(id);
        meetingRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/leaves")
    public ApiResponse<List<Leave>> leaves(@RequestParam(defaultValue = "false") boolean mine) {
        List<Leave> list = mine ? leaveRepository.findByUserIdOrderByIdDesc(SecurityUtils.currentUserId())
                : leaveRepository.findAllByOrderByIdDesc();
        list.forEach(this::enrichLeave);
        return ApiResponse.ok(list);
    }

    @PostMapping("/leaves")
    public ApiResponse<Leave> createLeave(@RequestBody Leave body) {
        return ApiResponse.ok(leaveService.create(body));
    }

    @PostMapping("/leaves/{id}/submit")
    public ApiResponse<Leave> submitLeave(@PathVariable Long id) {
        return ApiResponse.ok(leaveService.submit(id));
    }

    @GetMapping("/attendance")
    public ApiResponse<List<Attendance>> attendance(@RequestParam(required = false) String date,
                                                    @RequestParam(defaultValue = "false") boolean mine) {
        List<Attendance> list;
        if (mine) {
            list = attendanceRepository.findByUserIdOrderByWorkDateDesc(SecurityUtils.currentUserId());
        } else if (date != null && !date.isBlank()) {
            list = attendanceRepository.findByWorkDateOrderByIdAsc(LocalDate.parse(date));
        } else {
            list = attendanceRepository.findAll(Sort.by(Sort.Direction.DESC, "workDate"));
        }
        list.forEach(a -> userRepository.findById(a.getUserId())
                .ifPresent(u -> a.setUserName(u.getRealName())));
        return ApiResponse.ok(list);
    }

    @PostMapping("/attendance/check-in")
    public ApiResponse<Attendance> checkIn() {
        LocalDate today = LocalDate.now();
        Attendance a = attendanceRepository.findByUserIdAndWorkDate(SecurityUtils.currentUserId(), today)
                .orElseGet(() -> {
                    Attendance na = new Attendance();
                    na.setUserId(SecurityUtils.currentUserId());
                    na.setWorkDate(today);
                    return na;
                });
        if (a.getCheckInTime() != null) {
            throw new BizException("今日已打卡上班");
        }
        LocalDateTime now = LocalDateTime.now();
        a.setCheckInTime(now);
        a.setStatus(now.toLocalTime().isAfter(LocalTime.of(9, 5)) ? "LATE" : "NORMAL");
        return ApiResponse.ok(attendanceRepository.save(a));
    }

    @PostMapping("/attendance/check-out")
    public ApiResponse<Attendance> checkOut() {
        LocalDate today = LocalDate.now();
        Attendance a = attendanceRepository.findByUserIdAndWorkDate(SecurityUtils.currentUserId(), today)
                .orElseThrow(() -> new BizException("请先打卡上班"));
        if (a.getCheckOutTime() != null) {
            throw new BizException("今日已打卡下班");
        }
        a.setCheckOutTime(LocalDateTime.now());
        if ("NORMAL".equals(a.getStatus()) && a.getCheckOutTime().toLocalTime().isBefore(LocalTime.of(18, 0))) {
            a.setStatus("EARLY");
        }
        return ApiResponse.ok(attendanceRepository.save(a));
    }

    @GetMapping("/attendance/summary")
    public ApiResponse<Map<String, Object>> attendanceSummary() {
        LocalDate today = LocalDate.now();
        List<Attendance> todayList = attendanceRepository.findByWorkDateOrderByIdAsc(today);
        long normal = todayList.stream().filter(a -> "NORMAL".equals(a.getStatus())).count();
        long late = todayList.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", today);
        map.put("checkedIn", todayList.stream().filter(a -> a.getCheckInTime() != null).count());
        map.put("normal", normal);
        map.put("late", late);
        map.put("totalUsers", userRepository.countByStatus(1));
        return ApiResponse.ok(map);
    }

    @GetMapping("/documents")
    public ApiResponse<List<Document>> documents() {
        List<Document> list = documentRepository.findAllByOrderByIdDesc();
        list.forEach(d -> userRepository.findById(d.getUploaderId() == null ? -1L : d.getUploaderId())
                .ifPresent(u -> d.setUploaderName(u.getRealName())));
        return ApiResponse.ok(list);
    }

    @PostMapping("/documents")
    public ApiResponse<Document> createDocument(@RequestBody Document body) {
        body.setId(null);
        body.setUploaderId(SecurityUtils.currentUserId());
        return ApiResponse.ok(documentRepository.save(body));
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        documentRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void saveAttendees(Long meetingId, List<Long> userIds) {
        meetingAttendeeRepository.deleteByMeetingId(meetingId);
        if (userIds != null) {
            for (Long userId : userIds) {
                MeetingAttendee a = new MeetingAttendee();
                a.setMeetingId(meetingId);
                a.setUserId(userId);
                meetingAttendeeRepository.save(a);
            }
        }
    }

    private void enrichNotice(Notice n) {
        userRepository.findById(n.getPublisherId() == null ? -1L : n.getPublisherId())
                .ifPresent(u -> n.setPublisherName(u.getRealName()));
    }

    private void enrichTask(OaTask t) {
        userRepository.findById(t.getAssigneeId() == null ? -1L : t.getAssigneeId())
                .ifPresent(u -> t.setAssigneeName(u.getRealName()));
        userRepository.findById(t.getCreatorId() == null ? -1L : t.getCreatorId())
                .ifPresent(u -> t.setCreatorName(u.getRealName()));
    }

    private void enrichLeave(Leave l) {
        User u = userRepository.findById(l.getUserId() == null ? -1L : l.getUserId()).orElse(null);
        if (u != null) {
            l.setUserName(u.getRealName());
            if (u.getDeptId() != null) {
                deptRepository.findById(u.getDeptId()).ifPresent(d -> l.setDeptName(d.getName()));
            }
        }
    }
}
