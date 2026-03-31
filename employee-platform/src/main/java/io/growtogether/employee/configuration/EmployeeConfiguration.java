package io.growtogether.employee.configuration;

import io.growtogether.employee.controller.EmployeeController;
import io.growtogether.employee.entity.EmployeeEntity;
import io.growtogether.employee.mapper.EmployeeMapper;
import io.growtogether.employee.repository.EmployeeRepository;
import io.growtogether.employee.service.impl.EmployeeServiceImpl;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = EmployeeRepository.class)
@EntityScan(basePackageClasses = EmployeeEntity.class)
@ComponentScan(basePackageClasses = {
        EmployeeMapper.class,
        EmployeeServiceImpl.class,
        EmployeeController.class
})
public class EmployeeConfiguration {
}
