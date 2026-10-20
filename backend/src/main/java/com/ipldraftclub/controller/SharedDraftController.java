package com.ipldraftclub.controller;

import com.ipldraftclub.dto.DraftDtos.SharedDraftDto;
import com.ipldraftclub.service.DraftService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only view of a draft its owner chose to share. Revoked tokens return 404. */
@RestController
@RequestMapping("/api/v1/shared")
public class SharedDraftController {

    private final DraftService drafts;

    public SharedDraftController(DraftService drafts) {
        this.drafts = drafts;
    }

    @GetMapping("/{shareToken}")
    public SharedDraftDto shared(@PathVariable String shareToken) {
        return drafts.shared(shareToken);
    }
}
