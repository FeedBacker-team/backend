package com.feedbacker.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class SupabaseStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String secretKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.storage.secret-key:}") String secretKey,
            @Value("${supabase.storage.bucket:}") String bucket
    ) {
        this.supabaseUrl = removeTrailingSlash(supabaseUrl);
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.restClient = RestClient.builder()
                .baseUrl(this.supabaseUrl)
                .defaultHeader("apikey", secretKey)
                .build();
    }

    /**
     * @param file 파일
     * @return 파일 저장 경로
     */
    public String uploadImage(MultipartFile file) {
        validateConfiguration();
        validateImage(file);

        String extension = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + extension;
        String folder = "images";

        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{folder}/{fileName}", bucket, folder, fileName)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header("x-upsert", "false")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "파일을 읽을 수 없습니다.", exception);
        }

        return folder + "/" + fileName;
    }

    /**
     * @param path "folder/uuidFileName"
     * @return url
     */
    public String createPublicUrl(String path) {
        return UriComponentsBuilder.fromUriString(supabaseUrl)
                .pathSegment("storage", "v1", "object", "public", bucket)
                .path("/")
                .path(path)
                .build()
                .toUriString();
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(supabaseUrl)
                || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(bucket)) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Supabase Storage 환경변수가 설정되지 않았습니다.");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "업로드할 파일이 비어 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(BAD_REQUEST, "jpg, jpeg, png, gif, webp 파일만 업로드할 수 있습니다.");
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new ResponseStatusException(BAD_REQUEST, "파일 확장자가 필요합니다.");
        }

        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
