ALTER TABLE lessons
    ADD COLUMN lesson_order INT NULL AFTER chapter_id;

UPDATE lessons AS lesson
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY chapter_id ORDER BY id) AS generated_order
    FROM lessons
) AS ranked ON ranked.id = lesson.id
SET lesson.lesson_order = ranked.generated_order;

ALTER TABLE lessons
    MODIFY COLUMN lesson_order INT NOT NULL;

CREATE INDEX idx_lessons_chapter_order
    ON lessons (chapter_id, lesson_order, id);
