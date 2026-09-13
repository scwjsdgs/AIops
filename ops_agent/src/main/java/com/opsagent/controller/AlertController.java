package com.opsagent.controller;

import com.opsagent.dto.ApiResponse;
import com.opsagent.model.Alert;
import com.opsagent.service.AlertIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {


    private final AlertIngestionService alertIngestionService;

    @PostMapping
    public Mono<ApiResponse<Void>> receiveAlert(@RequestBody Alert alert) {
        return alertIngestionService.processAlert(alert)
                .then(Mono.just(ApiResponse.success(null)));
    }
}
