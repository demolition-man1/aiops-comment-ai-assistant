package com.aiops.controller;

import com.aiops.exception.BusinessException;
import com.aiops.result.Result;
import com.aiops.service.RagKnowledgeService;
import com.aiops.vo.RagIndexStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/rag")
@RequiredArgsConstructor
@Tag(name = "RAG 知识索引", description = "查看并手动重建差评回复知识索引")
public class RagKnowledgeController {

    private final RagKnowledgeService ragKnowledgeService;

    @GetMapping("/status")
    @Operation(summary = "查看知识索引状态")
    public Result<RagIndexStatusVO> getStatus() {
        return Result.success(ragKnowledgeService.getStatus());
    }

    @PostMapping("/reindex")
    @Operation(summary = "手动重建知识索引")
    public ResponseEntity<Result<RagIndexStatusVO>> reindex() {
        try {
            return ResponseEntity.accepted().body(Result.success(ragKnowledgeService.reindex()));
        } catch (BusinessException exception) {
            if (Integer.valueOf(409).equals(exception.getCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Result.error(exception.getCode(), exception.getMessage()));
            }
            throw exception;
        }
    }
}
