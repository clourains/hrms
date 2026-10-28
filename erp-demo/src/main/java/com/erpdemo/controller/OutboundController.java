package com.erpdemo.controller;

import com.erpdemo.service.OutboundService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outbound")
public class OutboundController {

    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return outboundService.list();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return outboundService.create(body);
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable long id) {
        return outboundService.confirm(id);
    }

    @PostMapping("/{id}/retry")
    public Map<String, Object> retry(@PathVariable long id) {
        return outboundService.retry(id);
    }
}
