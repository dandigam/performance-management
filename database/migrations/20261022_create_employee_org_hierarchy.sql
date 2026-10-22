-- Current reporting relationships only; no performance/SOW migration is performed.
CREATE TABLE IF NOT EXISTS employee_org_hierarchy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    reporting_manager_id BIGINT NULL,
    created_by BIGINT NULL,
    created_on DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_on DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_org_hierarchy_employee UNIQUE (employee_id),
    CONSTRAINT fk_org_hierarchy_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_org_hierarchy_manager FOREIGN KEY (reporting_manager_id) REFERENCES employees(id)
);
