CREATE TABLE project_views (
                               project_id UUID NOT NULL,
                               member_id UUID NOT NULL,
                               viewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT pk_project_views
                                   PRIMARY KEY (project_id, member_id),

                               CONSTRAINT fk_project_views_project
                                   FOREIGN KEY (project_id)
                                       REFERENCES project (project_id),

                               CONSTRAINT fk_project_views_member
                                   FOREIGN KEY (member_id)
                                       REFERENCES member (id)
);

CREATE INDEX idx_project_views_member_id
    ON project_views (member_id);