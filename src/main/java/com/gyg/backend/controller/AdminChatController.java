package com.gyg.backend.controller;

import com.gyg.backend.dto.ChatConsultaDTO;
import com.gyg.backend.dto.ChatConsultasPageDTO;
import com.gyg.backend.repository.LogChatRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/consultas-chat")
@Validated
public class AdminChatController {

    private final LogChatRepository logChatRepository;

    public AdminChatController(LogChatRepository logChatRepository) {
        this.logChatRepository = logChatRepository;
    }

    @GetMapping
    public ResponseEntity<ChatConsultasPageDTO> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = logChatRepository
                .findAllByOrderByFechaDesc(PageRequest.of(page, size))
                .map(ChatConsultaDTO::from);
        return ResponseEntity.ok(new ChatConsultasPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()));
    }
}
