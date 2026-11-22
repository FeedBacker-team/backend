ALTER TABLE project
    ALTER COLUMN owner_id DROP NOT NULL;

ALTER TABLE project
DROP CONSTRAINT fk_project_owner;

ALTER TABLE project
    ADD CONSTRAINT fk_project_owner
        FOREIGN KEY (owner_id)
            REFERENCES member (id)
            ON DELETE SET NULL;