package com.ibm.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.ibm.demo.model.Department;
import com.ibm.demo.model.Employee;
import com.ibm.demo.repository.DepartmentRepository;
import com.ibm.demo.repository.EmployeeRepository;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	CommandLineRunner initData(EmployeeRepository empRepo, DepartmentRepository deptRepo) {
		return args -> {

			Department it = new Department();
			it.setName("IT");
			it.setLocation("Bangalore");

			Department hr = new Department();
			hr.setName("HR");
			hr.setLocation("Mumbai");

			deptRepo.save(it);
			deptRepo.save(hr);

			Employee e1 = new Employee("Sonu", 90000, it);
			Employee e2 = new Employee("Monu", 98000, it);
			Employee e3 = new Employee("Tonu", 95000, hr);

			empRepo.save(e1);
			empRepo.save(e2);
			empRepo.save(e3);

			System.out.println("Departments and Employees inserted");
		};
	}
}

//package com.ibm.demo;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//
//import com.ibm.demo.model.Employee;
//import com.ibm.demo.repository.EmployeeRepository;
//
//@SpringBootApplication
//public class Application {
//
//    public static void main(String[] args) {
//        SpringApplication.run(Application.class, args);
//    }
//
//    @Bean
//    CommandLineRunner initData(EmployeeRepository repo) {
//        return args -> {
//            repo.save(new Employee("Sonu", 90000));
//            repo.save(new Employee("Monu", 98000));
//            repo.save(new Employee("Tonu", 95000));
//        };
//    }
//}