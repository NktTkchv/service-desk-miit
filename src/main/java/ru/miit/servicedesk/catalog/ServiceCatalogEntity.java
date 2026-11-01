package ru.miit.servicedesk.catalog;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import ru.miit.servicedesk.requests.RequestEntity;

@Entity
@Table(name = "service_catalog")
public class ServiceCatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_service")
    private String nameService;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "deadline")
    private Integer deadline;

    @OneToMany(mappedBy = "service")
    private Set<RequestEntity> requests = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNameService() {
        return nameService;
    }

    public void setNameService(String nameService) {
        this.nameService = nameService;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDeadline() {
        return deadline;
    }

    public void setDeadline(Integer deadline) {
        this.deadline = deadline;
    }

    public Set<RequestEntity> getRequests() {
        return requests;
    }

    public void setRequests(Set<RequestEntity> requests) {
        this.requests = requests;
    }
}
