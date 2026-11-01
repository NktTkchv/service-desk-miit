package ru.miit.servicedesk.dashboard;

import java.util.Map;

public interface DashboardService {

    long countOpenRequests();

    Map<Long, Long> requestsByExecutor();
}
