package com.feedbacker.project.service;

import com.feedbacker.global.storage.SupabaseStorageService;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/** 프로젝트 물리 삭제 (회원 탈퇴용) */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDeleteService {

    private final ProjectRepository projectRepository;
    private final SupabaseStorageService storageService;

    /**
     * 회원의 모든 프로젝트를 물리 삭제한다 (삭제 상태 포함). 썸네일은 커밋 후 스토리지에서 삭제.
     * 태그는 JPA로, 조회 기록(project_views, project_guest_views)은 DB cascade로 함께 삭제된다.
     * 프로젝트를 참조하는 모집글은 먼저 지워져 있어야 한다
     */
    @Transactional
    public void deleteAllByOwner(UUID ownerId) {
        List<Project> projects = projectRepository.findAllByOwner_Id(ownerId);
        if (projects.isEmpty()) {
            return;
        }
        List<String> thumbnailPaths = projects.stream()
                .map(Project::getThumbnailImage)
                .filter(StringUtils::hasText)
                .toList();
        projectRepository.deleteAll(projects);
        deleteThumbnailsAfterCommit(ownerId, thumbnailPaths);
    }

    /**
     * 스토리지 삭제는 롤백할 수 없으므로 DB 삭제가 커밋된 뒤에 지운다.
     * 실패해도 DB 삭제는 이미 끝났으므로 로그만 남긴다
     */
    private void deleteThumbnailsAfterCommit(UUID ownerId, List<String> thumbnailPaths) {
        if (thumbnailPaths.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storageService.deleteImages(thumbnailPaths);
                } catch (Exception e) {
                    log.warn("프로젝트 썸네일 삭제 실패: ownerId={}, paths={}", ownerId, thumbnailPaths, e);
                }
            }
        });
    }
}
