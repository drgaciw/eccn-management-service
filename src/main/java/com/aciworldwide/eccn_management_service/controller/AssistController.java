package com.aciworldwide.eccn_management_service.controller;

import com.aciworldwide.eccn_management_service.council.CouncilOrchestrator;
import com.aciworldwide.eccn_management_service.council.CouncilResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assist")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "LLM Council deliberation endpoint")
public class AssistController {

    private final CouncilOrchestrator orchestrator;

    @PostMapping("/council")
    public ResponseEntity<CouncilResponse> deliberate(@RequestBody String prompt) {
        CouncilResponse response = orchestrator.deliberate(prompt);
        return ResponseEntity.ok(response);
    }
}
