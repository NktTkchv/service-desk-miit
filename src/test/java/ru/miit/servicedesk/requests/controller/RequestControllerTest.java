package ru.miit.servicedesk.requests.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.catalog.service.CatalogService;
import ru.miit.servicedesk.requests.DTO.CreateRequestForm;
import ru.miit.servicedesk.requests.DTO.RequestDetailsDto;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.entity.AudienceEntity;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.service.AudienceService;
import ru.miit.servicedesk.requests.service.RequestService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {
    @Mock private RequestService requestService;
    @Mock private CatalogService catalogService;
    @Mock private AudienceService audienceService;
    @Mock private Model model;
    @Mock private BindingResult bindingResult;
    private RequestController controller;

    @BeforeEach
    void setUp() {
        controller = new RequestController(requestService, catalogService, audienceService);
    }

    @Test
    void getRequestsByAuthorAddsPageAndReturnsListView() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<RequestSummaryDto> page = new PageImpl<>(List.of());
        when(requestService.listByAuthor(5L, pageable)).thenReturn(page);

        String view = controller.getRequestsByAuthor(5L, pageable, model);

        assertThat(view).isEqualTo("requests/list");
        verify(model).addAttribute("requests", page);
    }

    @Test
    void detailsAddsDtoAndReturnsDetailsView() {
        RequestDetailsDto details = mock(RequestDetailsDto.class);
        when(requestService.getById(7L)).thenReturn(details);

        String view = controller.details(7L, model);

        assertThat(view).isEqualTo("requests/details");
        verify(model).addAttribute("req", details);
    }

    @Test
    void createRequestBuildsPrefilledFormAndReferenceLists() {
        List<ServiceCatalogEntity> services = List.of(new ServiceCatalogEntity());
        List<AudienceEntity> audiences = List.of(new AudienceEntity());
        when(catalogService.listAll()).thenReturn(services);
        when(audienceService.listAll()).thenReturn(audiences);

        String view = controller.createRequest(11L, model);

        assertThat(view).isEqualTo("requests/form");
        ArgumentCaptor<CreateRequestForm> formCaptor = ArgumentCaptor.forClass(CreateRequestForm.class);
        verify(model).addAttribute(eq("form"), formCaptor.capture());
        assertThat(formCaptor.getValue().getAuthorId()).isEqualTo(11L);
        verify(model).addAttribute("services", services);
        verify(model).addAttribute("audiences", audiences);
    }

    @Test
    void createRedirectsToCreatedRequestWhenFormIsValid() {
        CreateRequestForm form = form();
        RequestEntity created = new RequestEntity();
        created.setId(77L);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(requestService.create(1L, 2L, 3L, "description")).thenReturn(created);

        String view = controller.create(form, bindingResult, model);

        assertThat(view).isEqualTo("redirect:/requests/77");
        verifyNoInteractions(catalogService, audienceService);
        verifyNoInteractions(model);
    }

    @Test
    void createReturnsFormAndReloadsReferencesWhenBindingHasErrors() {
        CreateRequestForm form = form();
        List<ServiceCatalogEntity> services = List.of(new ServiceCatalogEntity());
        List<AudienceEntity> audiences = List.of(new AudienceEntity());
        when(bindingResult.hasErrors()).thenReturn(true);
        when(catalogService.listAll()).thenReturn(services);
        when(audienceService.listAll()).thenReturn(audiences);

        String view = controller.create(form, bindingResult, model);

        assertThat(view).isEqualTo("requests/form");
        verifyNoInteractions(requestService);
        verify(model).addAttribute("services", services);
        verify(model).addAttribute("audiences", audiences);
    }

    @Test
    void createAddsGlobalErrorAndReturnsFormWhenServiceRejectsRequest() {
        CreateRequestForm form = form();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(requestService.create(1L, 2L, 3L, "description"))
            .thenThrow(new IllegalArgumentException("Услуга не найдена"));
        when(catalogService.listAll()).thenReturn(List.of());
        when(audienceService.listAll()).thenReturn(List.of());

        String view = controller.create(form, bindingResult, model);

        assertThat(view).isEqualTo("requests/form");
        verify(bindingResult).reject("request.create.failed", "Услуга не найдена");
        verify(model).addAttribute("services", List.of());
        verify(model).addAttribute("audiences", List.of());
    }

    private static CreateRequestForm form() {
        CreateRequestForm form = new CreateRequestForm();
        form.setAuthorId(1L);
        form.setAudienceId(2L);
        form.setServiceId(3L);
        form.setDescription("description");
        return form;
    }
}
