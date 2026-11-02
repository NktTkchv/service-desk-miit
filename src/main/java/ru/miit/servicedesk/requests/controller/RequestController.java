package ru.miit.servicedesk.requests.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.miit.servicedesk.requests.DTO.RequestSummaryDto;
import ru.miit.servicedesk.requests.service.RequestService;

@Controller
@RequestMapping("/requests")
public class RequestController {
    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
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

}
