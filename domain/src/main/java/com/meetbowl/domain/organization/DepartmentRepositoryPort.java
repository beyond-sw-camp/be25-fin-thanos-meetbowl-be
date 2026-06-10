package com.meetbowl.domain.organization;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepositoryPort {

    Department save(Department department);

    Optional<Department> findById(UUID departmentId);
}
