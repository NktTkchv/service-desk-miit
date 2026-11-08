package ru.miit.servicedesk.catalog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.miit.servicedesk.catalog.service.CatalogService;

@Controller
@RequestMapping("/catalog")
public class ServiceCatalogController {

    private final CatalogService catalogService;

    public ServiceCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }
}
