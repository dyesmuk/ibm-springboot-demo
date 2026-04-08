package com.ibm.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ibm.demo.model.Employee;
import com.ibm.demo.repository.EmployeeRepository;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner initData(EmployeeRepository repo) {
        return args -> {
            repo.save(new Employee("Sonu", 90000));
            repo.save(new Employee("Monu", 98000));
            repo.save(new Employee("Tonu", 95000));
        };
    }
}