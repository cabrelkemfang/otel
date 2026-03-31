package io.growtogether.employee.mapper;

import io.growtogether.employee.dto.EmployeeRequest;
import io.growtogether.employee.dto.EmployeeResponse;
import io.growtogether.employee.entity.EmployeeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface EmployeeMapper {

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    EmployeeEntity toEntity(EmployeeRequest request);

    EmployeeResponse toResponse(EmployeeEntity entity);

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget EmployeeEntity entity, EmployeeRequest request);
}
