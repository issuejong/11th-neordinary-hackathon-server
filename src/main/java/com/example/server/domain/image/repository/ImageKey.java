package com.example.server.domain.image.repository;

/** 이미지 바이너리 없이 식별자만 담는 조회용 projection */
public record ImageKey(Long id, String publicId) {
}
