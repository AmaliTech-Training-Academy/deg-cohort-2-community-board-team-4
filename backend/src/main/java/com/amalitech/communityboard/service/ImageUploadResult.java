package com.amalitech.communityboard.service;

/** Result of a successful Cloudinary upload: the served URL and the public id used for deletion. */
public record ImageUploadResult(String url, String publicId) {
}
