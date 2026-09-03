package com.dxh.learninghub.repo;

import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class S3ObjectReferenceRepository {

    EntityManager entityManager;

    /**
     * Lấy toàn bộ URL hoặc object key S3 đang được database sử dụng.
     * QUAN TRỌNG:
     * Khi thêm field mới có lưu file S3, ví dụ:
     * - Lesson.audioUrl
     * - Course.bannerUrl
     * - User.coverImage
     * phải thêm câu query lấy field đó vào method này.
     * Nếu không thêm, cleanup job sẽ không biết file đang được sử dụng
     * và có thể coi nó là file mồ côi.
     */
    @Transactional(readOnly = true)
    public Set<String> findRetainedReferences() {
        String nativeSql = """
                SELECT avatar
                FROM users
                WHERE avatar IS NOT NULL

                UNION ALL

                SELECT cv_url
                FROM users
                WHERE cv_url IS NOT NULL

                UNION ALL

                SELECT certificate
                FROM users
                WHERE certificate IS NOT NULL

                UNION ALL

                SELECT thumbnail
                FROM courses
                WHERE thumbnail IS NOT NULL
                  AND status <> 'DELETED'

                UNION ALL

                SELECT video_url
                FROM courses
                WHERE video_url IS NOT NULL
                  AND status <> 'DELETED'

                UNION ALL

                SELECT l.content_url
                FROM lessons l
                JOIN chapters ch ON ch.id = l.chapter_id
                JOIN courses c ON c.id = ch.course_id
                WHERE l.content_url IS NOT NULL
                  AND c.status <> 'DELETED'

                UNION ALL

                SELECT image
                FROM advertisements
                WHERE image IS NOT NULL

                UNION ALL

                SELECT payment_proof_url
                FROM withdrawals
                WHERE payment_proof_url IS NOT NULL

                UNION ALL
                
                SELECT content
                FROM messages
                WHERE type = 'IMAGE'
                AND content IS NOT NULL
                """;

        List<?> rawReferences = entityManager.createNativeQuery(nativeSql).getResultList();

        return rawReferences.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(reference -> !reference.isBlank())
                .collect(Collectors.toSet());
    }
}