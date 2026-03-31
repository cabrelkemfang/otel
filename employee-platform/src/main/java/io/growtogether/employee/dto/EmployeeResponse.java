package io.upskilling.training.employee.dto;

import io.upskilling.training.employee.entity.Department;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EmployeeResponse(Long employeeId,
                               String firstName,
                               String lastName,
                               String phoneNumber,
                               String email,
                               Department department,
                               LocalDateTime createdAt) {
}
