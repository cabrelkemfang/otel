package io.upskilling.training.employee.configuration;

import io.upskilling.training.employee.entity.EmployeeEntity;
import io.upskilling.training.employee.repository.EmployeeRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = EmployeeRepository.class)
@EntityScan(basePackageClasses = EmployeeEntity.class)
@ComponentScan(basePackages = {
        "io.upskilling.training.employee.mapper",
        "io.upskilling.training.employee.service.impl",
        "io.upskilling.training.employee.controller"
})
public class EmployeeConfiguration {
}
