package ru.miit.servicedesk.requests.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.miit.servicedesk.catalog.service.CatalogService;
import ru.miit.servicedesk.requests.DTO.CreateRequestForm;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.entity.RequestEntity;
import ru.miit.servicedesk.requests.service.AudienceService;
import ru.miit.servicedesk.requests.service.RequestService;

@Controller
@RequestMapping("/requests")
public class RequestController {
    private final RequestService requestService;
    private final CatalogService catalogService;
    private final AudienceService audienceService;

    public RequestController(RequestService requestService, CatalogService catalogService, AudienceService audienceService) {
        this.requestService = requestService;
        this.catalogService = catalogService;
        this.audienceService = audienceService;
    }

    @GetMapping("/by-author-id")
    public String getRequestsByAuthor(@RequestParam("authorId") Long authorId, Pageable pageable, Model model) {
        Page<RequestSummaryDto> requestsPage = requestService.listByAuthor(authorId, pageable);
        model.addAttribute("requests", requestsPage);
        return "requests/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("req", requestService.getById(id));
        return "requests/details";
    }

    @GetMapping("/create")
    public String createRequest(@RequestParam("authorId") Long authorId, Model model) {
        CreateRequestForm form = new CreateRequestForm();
        form.setAuthorId(authorId);
        model.addAttribute("form", form);
        model.addAttribute("services", catalogService.listAll());
        model.addAttribute("audiences", audienceService.listAll());
        return "requests/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CreateRequestForm form,
                         BindingResult bindingResult,
                         Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                RequestEntity created = requestService.create(
                    form.getAuthorId(), form.getAudienceId(), form.getServiceId(), form.getDescription());
                return "redirect:/requests/" + created.getId();
            } catch (IllegalArgumentException e) {
                bindingResult.reject("request.create.failed", e.getMessage());
            }
        }
        model.addAttribute("services", catalogService.listAll());
        model.addAttribute("audiences", audienceService.listAll());
        return "requests/form";
    }

}
