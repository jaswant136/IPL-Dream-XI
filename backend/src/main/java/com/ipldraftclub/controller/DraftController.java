package com.ipldraftclub.controller;

import com.ipldraftclub.dto.DraftDtos.AddPickRequest;
import com.ipldraftclub.dto.DraftDtos.DraftDetailDto;
import com.ipldraftclub.dto.DraftDtos.DraftResultDto;
import com.ipldraftclub.dto.DraftDtos.DraftSummaryDto;
import com.ipldraftclub.dto.DraftDtos.ShareDto;
import com.ipldraftclub.dto.DraftDtos.StartDraftRequest;
import com.ipldraftclub.dto.PageResponse;
import com.ipldraftclub.entity.DraftStatus;
import com.ipldraftclub.security.AuthenticatedUser;
import com.ipldraftclub.service.DraftService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** All routes require a verified Firebase token; the UID always comes from the token, never the request. */
@RestController
@RequestMapping("/api/v1/drafts")
public class DraftController {

    private final DraftService drafts;

    public DraftController(DraftService drafts) {
        this.drafts = drafts;
    }

    @PostMapping("/start")
    public ResponseEntity<DraftDetailDto> start(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody StartDraftRequest request,
                                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        DraftService.StartResult result = drafts.start(user.uid(), request.mode(), idempotencyKey);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.draft());
    }

    @GetMapping
    public PageResponse<DraftSummaryDto> list(@AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) DraftStatus status) {
        return drafts.list(user.uid(), page, size, status);
    }

    @GetMapping("/{draftId}")
    public DraftDetailDto get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID draftId) {
        return drafts.get(user.uid(), draftId);
    }

    @PostMapping("/{draftId}/picks")
    public ResponseEntity<DraftDetailDto> addPick(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable UUID draftId,
                                                  @Valid @RequestBody AddPickRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(drafts.addPick(user.uid(), draftId, request.playerSeasonId()));
    }

    @DeleteMapping("/{draftId}/picks/{pickId}")
    public DraftDetailDto removePick(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable UUID draftId, @PathVariable UUID pickId) {
        return drafts.removePick(user.uid(), draftId, pickId);
    }

    @PostMapping("/{draftId}/complete")
    public DraftResultDto complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID draftId) {
        return drafts.complete(user.uid(), draftId);
    }

    @GetMapping("/{draftId}/result")
    public DraftResultDto result(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID draftId) {
        return drafts.result(user.uid(), draftId);
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID draftId) {
        drafts.delete(user.uid(), draftId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{draftId}/share")
    public ShareDto share(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID draftId) {
        return drafts.share(user.uid(), draftId);
    }

    @DeleteMapping("/{draftId}/share")
    public ResponseEntity<Void> revokeShare(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID draftId) {
        drafts.revokeShare(user.uid(), draftId);
        return ResponseEntity.noContent().build();
    }
}
