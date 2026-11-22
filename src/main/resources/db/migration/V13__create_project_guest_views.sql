CREATE TABLE project_guest_views (
    project_id UUID NOT NULL,
    guest_id   UUID NOT NULL,
    viewed_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_project_guest_views
        PRIMARY KEY (project_id, guest_id),

    CONSTRAINT fk_project_guest_views_project
        FOREIGN KEY (project_id)
        REFERENCES project (project_id)
);