package com.amalitech.communityboard.service;

import com.amalitech.communityboard.exception.InvalidFileException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;

    @Value("${cloudinary.upload-folder}")
    private String uploadFolder;

    /** Validates and uploads an image to Cloudinary, returning the served URL and public id. */
    public ImageUploadResult upload(MultipartFile file) {
        validate(file);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", uploadFolder, "resource_type", "image"));
            return new ImageUploadResult(
                    String.valueOf(result.get("secure_url")),
                    String.valueOf(result.get("public_id")));
        } catch (IOException e) {
            throw new InvalidFileException("Failed to upload image: " + e.getMessage());
        }
    }

    /** Best-effort deletion of a previously uploaded image. Never throws. */
    public void delete(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Failed to delete Cloudinary image {}: {}", publicId, e.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("An image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                    "Unsupported image type. Allowed types: JPEG, PNG, WEBP, GIF");
        }
    }
}
