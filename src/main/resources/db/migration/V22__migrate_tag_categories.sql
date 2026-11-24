ALTER TABLE project
    ADD COLUMN tag_schema_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE project
    ALTER COLUMN tag_schema_version SET DEFAULT 2;

CREATE TEMP TABLE valid_tag_codes (
    code VARCHAR(20) PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO valid_tag_codes (code)
VALUES
    ('PRODUCTIVITY'),
    ('COMMERCE'),
    ('FINTECH'),
    ('EDUCATION'),
    ('HEALTHCARE'),
    ('CONTENT_MEDIA'),
    ('LIFESTYLE'),
    ('SOCIAL_COMMUNITY'),
    ('PETS'),
    ('CAREER'),
    ('TRAVEL'),
    ('MOBILITY'),
    ('FOOD'),
    ('MARKETING'),
    ('GAME'),
    ('SPORTS'),
    ('REAL_ESTATE');

-- 유효한 프로젝트 태그를 보관하고 순서를 0부터 다시 지정
CREATE TEMP TABLE migrated_project_tags ON COMMIT DROP AS
SELECT
    project_id,
    (
        ROW_NUMBER() OVER (
            PARTITION BY project_id
            ORDER BY first_order, tag
        ) - 1
        )::INTEGER AS tag_order,
    tag
FROM (
         SELECT
             project_id,
             tag,
             MIN(tag_order) AS first_order
         FROM project_tags
         WHERE tag IN (SELECT code FROM valid_tag_codes)
         GROUP BY project_id, tag
     ) retained;

DELETE FROM project_tags;

INSERT INTO project_tags (project_id, tag_order, tag)
SELECT project_id, tag_order, tag
FROM migrated_project_tags;

CREATE TEMP TABLE migrated_member_interests ON COMMIT DROP AS
SELECT
    member_id,
    (
        ROW_NUMBER() OVER (
            PARTITION BY member_id
            ORDER BY first_order, interest
        ) - 1
        )::INTEGER AS interest_order,
    interest
FROM (
         SELECT
             member_id,
             interest,
             MIN(interest_order) AS first_order
         FROM member_interest
         WHERE interest IN (SELECT code FROM valid_tag_codes)
         GROUP BY member_id, interest
     ) retained;

DELETE FROM member_interest;

INSERT INTO member_interest (member_id, interest_order, interest)
SELECT member_id, interest_order, interest
FROM migrated_member_interests;