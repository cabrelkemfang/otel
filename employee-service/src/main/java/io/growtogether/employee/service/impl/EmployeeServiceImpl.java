package io.growtogether.employee.service.impl;

import io.growtogether.employee.dto.EmployeeRequest;
import io.growtogether.employee.dto.EmployeeResponse;
import io.growtogether.employee.dto.PaginatedResponse;
import io.growtogether.employee.entity.EmployeeEntity;
import io.growtogether.employee.exception.DuplicateEmailException;
import io.growtogether.employee.exception.EmployeeNotFoundException;
import io.growtogether.employee.mapper.EmployeeMapper;
import io.growtogether.employee.repository.EmployeeRepository;
import io.growtogether.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(cacheNames = "employeeById", key = "#id")
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
    @CachePut(cacheNames = "employeeById", key = "#id")
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
    @CacheEvict(cacheNames = "employeeById", key = "#id")
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        employeeRepository.deleteById(id);
    }
}
