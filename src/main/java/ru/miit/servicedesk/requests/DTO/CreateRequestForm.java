package ru.miit.servicedesk.requests.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateRequestForm {

    @NotNull(message = "Не определен автор заявки")
    private Long authorId;

    @NotNull(message = "Выберите аудиторию")
    private Long audienceId;

    @NotNull(message = "Выберите услугу")
    private Long serviceId;

    @Size(max = 4000, message = "Описание не должно превышать 4000 символов")
    private String description;

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public Long getAudienceId() { return audienceId; }
    public void setAudienceId(Long audienceId) { this.audienceId = audienceId; }

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
