package ru.miit.servicedesk.requests.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.miit.servicedesk.requests.entity.AudienceEntity;
import ru.miit.servicedesk.requests.repository.AudienceRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AudienceServiceImpl implements AudienceService{

    private final AudienceRepository audienceRepository;

    public AudienceServiceImpl(AudienceRepository audienceRepository) {
        this.audienceRepository = audienceRepository;
    }

    @Override
    public List<AudienceEntity> listAll() {
        return audienceRepository.findAll();
    }
}
