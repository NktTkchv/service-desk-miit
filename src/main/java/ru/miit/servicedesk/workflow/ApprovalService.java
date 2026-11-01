package ru.miit.servicedesk.workflow;

public interface ApprovalService {

    void approveByHead(Long requestId, Long headId);

    void rejectByHead(Long requestId, Long headId, String reason);

    void approveByDepartment(Long requestId, Long staffId, Long executorId);

    void rejectByDepartment(Long requestId, Long staffId, String reason);
}
