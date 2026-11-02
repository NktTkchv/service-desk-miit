package ru.miit.servicedesk.users;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import ru.miit.servicedesk.requests.entity.RequestEntity;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login")
    private String login;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "name")
    private String name;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "patronymic")
    private String patronymic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @OneToMany(mappedBy = "author")
    private Set<RequestEntity> authoredRequests = new LinkedHashSet<>();

    @OneToMany(mappedBy = "approver")
    private Set<RequestEntity> approvedRequests = new LinkedHashSet<>();

    @OneToMany(mappedBy = "executor")
    private Set<RequestEntity> assignedRequests = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public void setPatronymic(String patronymic) {
        this.patronymic = patronymic;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }

    public Set<RequestEntity> getAuthoredRequests() {
        return authoredRequests;
    }

    public void setAuthoredRequests(Set<RequestEntity> authoredRequests) {
        this.authoredRequests = authoredRequests;
    }

    public Set<RequestEntity> getApprovedRequests() {
        return approvedRequests;
    }

    public void setApprovedRequests(Set<RequestEntity> approvedRequests) {
        this.approvedRequests = approvedRequests;
    }

    public Set<RequestEntity> getAssignedRequests() {
        return assignedRequests;
    }

    public void setAssignedRequests(Set<RequestEntity> assignedRequests) {
        this.assignedRequests = assignedRequests;
    }
}
