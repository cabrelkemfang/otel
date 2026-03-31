package io.upskilling.training.employee.service.impl;

import io.upskilling.training.employee.dto.EmployeeRequest;
import io.upskilling.training.employee.dto.EmployeeResponse;
import io.upskilling.training.employee.dto.PaginatedResponse;
import io.upskilling.training.employee.entity.EmployeeEntity;
import io.upskilling.training.employee.exception.DuplicateEmailException;
import io.upskilling.training.employee.exception.EmployeeNotFoundException;
import io.upskilling.training.employee.mapper.EmployeeMapper;
import io.upskilling.training.employee.repository.EmployeeRepository;
import io.upskilling.training.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }
        EmployeeEntity entity = employeeMapper.toEntity(request);
        EmployeeEntity savedEntity = employeeRepository.save(entity);
        return employeeMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return employeeRepository.findById(id)
                .map(employeeMapper::toResponse)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<EmployeeResponse> findAll(Pageable pageable) {
        Page<EmployeeResponse> page = employeeRepository.findAll(pageable)
                .map(employeeMapper::toResponse);
        return PaginatedResponse.<EmployeeResponse>builder()
                .response(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .build();
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        EmployeeEntity entity = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        if (!entity.getEmail().equals(request.getEmail()) && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }
        employeeMapper.updateEntity(entity, request);
        EmployeeEntity save = employeeRepository.save(entity);
        return employeeMapper.toResponse(save);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRepository.deleteById(id);
    }
}
