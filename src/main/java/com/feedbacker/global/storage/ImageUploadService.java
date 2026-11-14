package com.feedbacker.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final SupabaseStorageService storageService;

    public List<String> upload(List<MultipartFile> images) {
        return images.stream()
                .map(storageService::uploadImage)
                .toList();
    }
}
