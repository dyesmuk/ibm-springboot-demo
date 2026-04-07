package com.ibm.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.demo.model.Employee;
import com.ibm.demo.service.EmployeeService;

@RestController
public class EmployeeController {

	private EmployeeService empService;

	@GetMapping("employees")
	public Employee getEmployeeById() {
		int employeeId = 1;
		return empService.getEmployeeById(employeeId);
	}

//	@GetMapping("employees")
//	public List<Employee> getAllEmployees() {
//		return empService.getAllEmployees();
//	}
//
//	public Employee addEmployee(Employee employee) {
//		return empService.addEmployee(employee);
//	}
//
//	public Employee updateEmployee(Employee employee) {
//		return empService.updateEmployee(employee);
//	}
//
//	public Employee deleteEmployee(int id) {
//		int employeeId = 2;
//		return empService.deleteEmployee(employeeId);
//	}

}
