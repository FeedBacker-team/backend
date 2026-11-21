-- 객관식 보기 순서 보장
-- 답변이 보기 번호(selected_option)로 저장되므로 보기 순서를 컬럼으로 고정한다.

-- 1. 순서 컬럼 추가
ALTER TABLE question_options
    ADD COLUMN option_order INTEGER;

-- 2. 기존 보기: 지금까지 조회되던 물리적 저장 순서(ctid)대로 0부터 순서 채우기
UPDATE question_options qo
SET option_order = numbered.rn
FROM (
    SELECT ctid,
           ROW_NUMBER() OVER (PARTITION BY question_id ORDER BY ctid) - 1 AS rn
    FROM question_options
) numbered
WHERE qo.ctid = numbered.ctid;

-- 3. 제약 추가
ALTER TABLE question_options
    ALTER COLUMN option_order SET NOT NULL;

ALTER TABLE question_options
    ADD CONSTRAINT pk_question_options PRIMARY KEY (question_id, option_order);
