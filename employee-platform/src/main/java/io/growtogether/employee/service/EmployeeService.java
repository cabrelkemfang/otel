package io.upskilling.training.employee.service;

import io.upskilling.training.employee.dto.EmployeeRequest;
import io.upskilling.training.employee.dto.EmployeeResponse;
import io.upskilling.training.employee.dto.PaginatedResponse;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse findById(Long id);

    PaginatedResponse<EmployeeResponse> findAll(Pageable pageable);

    EmployeeResponse update(Long id, EmployeeRequest request);

    void delete(Long id);
}
