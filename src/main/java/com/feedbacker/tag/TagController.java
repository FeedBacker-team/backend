package com.feedbacker.tag;

import com.feedbacker.project.domain.ProjectTag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @GetMapping
    public ResponseEntity<List<TagResponse>> getTags() {
        List<TagResponse> response = Arrays.stream(ProjectTag.values())
                .map(TagResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

}
