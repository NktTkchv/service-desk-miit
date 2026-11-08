package ru.miit.servicedesk.requests.service;

import ru.miit.servicedesk.requests.entity.AudienceEntity;

import java.util.List;


public interface AudienceService {
    List<AudienceEntity> listAll();
}
