package com.erpdemo.controller;

import com.erpdemo.service.DirectoryImportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final DirectoryImportService directoryImportService;

    public ImportController(DirectoryImportService directoryImportService) {
        this.directoryImportService = directoryImportService;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return directoryImportService.info();
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history() {
        return directoryImportService.history(10);
    }

    @PostMapping("/scan")
    public Map<String, Object> scan() {
        return directoryImportService.scan();
    }
}
