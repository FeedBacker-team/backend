-- 기존 회원은 재선택 필요
ALTER TABLE member
    ADD COLUMN interest_schema_version INTEGER NOT NULL DEFAULT 1;

-- 이후 생성되는 회원은 현재 태그 체계 사용
ALTER TABLE member
    ALTER COLUMN interest_schema_version SET DEFAULT 2;