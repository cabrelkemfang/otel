package io.growtogether.employee.service;

import io.growtogether.employee.dto.EmployeeRequest;
import io.growtogether.employee.dto.EmployeeResponse;
import io.growtogether.employee.dto.PaginatedResponse;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse findById(Long id);

    PaginatedResponse<EmployeeResponse> findAll(Pageable pageable);

    EmployeeResponse update(Long id, EmployeeRequest request);

    void delete(Long id);
}
