ALTER TABLE project
    DROP CONSTRAINT fk_project_owner,
    ADD CONSTRAINT fk_project_owner
        FOREIGN KEY (owner_id) REFERENCES member (id) ON DELETE CASCADE;

ALTER TABLE project_tags
    DROP CONSTRAINT fk_project_tags_project,
    ADD CONSTRAINT fk_project_tags_project
        FOREIGN KEY (project_id) REFERENCES project (project_id) ON DELETE CASCADE;

ALTER TABLE project_views
    DROP CONSTRAINT fk_project_views_project,
    ADD CONSTRAINT fk_project_views_project
        FOREIGN KEY (project_id) REFERENCES project (project_id) ON DELETE CASCADE;

ALTER TABLE project_guest_views
    DROP CONSTRAINT fk_project_guest_views_project,
    ADD CONSTRAINT fk_project_guest_views_project
        FOREIGN KEY (project_id) REFERENCES project (project_id) ON DELETE CASCADE;
