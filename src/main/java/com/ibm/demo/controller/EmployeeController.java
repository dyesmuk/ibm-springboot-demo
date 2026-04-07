package com.ibm.demo.controller;

import java.net.http.HttpHeaders;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.demo.model.Employee;
import com.ibm.demo.service.EmployeeService;

@RestController
@RequestMapping("api")
public class EmployeeController {

	@Autowired
	private EmployeeService empService;

	@GetMapping("employees/{id}")
	public Employee getEmployeeById(@PathVariable(name = "id") int employeeId) {
		System.out.println(employeeId);
		return empService.getEmployeeById(employeeId);
	}

	@GetMapping("employees")
	public ResponseEntity<List<Employee>> getAllEmployees() {

		List<Employee> empList = empService.getAllEmployees();
		HttpStatus status = HttpStatus.OK;

		ResponseEntity<List<Employee>> response = new ResponseEntity<List<Employee>>(empList, status);
		return response;
	}

//	@GetMapping("employees")
//	public List<Employee> getAllEmployees() {
//		return empService.getAllEmployees();
//	}

	@PostMapping("employees")
	public Employee addEmployee(@RequestBody Employee employee) {
		return empService.addEmployee(employee);
	}

	@PutMapping("employees")
	public Employee updateEmployee(@RequestBody Employee employee) {
		return empService.updateEmployee(employee);
	}

	@DeleteMapping("employees/{id}")
	public Employee deleteEmployee(@PathVariable(name = "id") int employeeId) {
		System.out.println(employeeId);
		return empService.deleteEmployee(employeeId);
	}
}

//package com.ibm.demo.controller;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.ibm.demo.model.Employee;
//import com.ibm.demo.service.EmployeeService;
//
//@RestController
//@RequestMapping("api")
//public class EmployeeController {
//
//	@Autowired
//	private EmployeeService empService;
//
//	@GetMapping("employees/{id}")
//	public Employee getEmployeeById(@PathVariable(name = "id") int employeeId) {
//		System.out.println(employeeId);
//		return empService.getEmployeeById(employeeId);
//	}
//
//	@GetMapping("employees")
//	public List<Employee> getAllEmployees() {
//		return empService.getAllEmployees();
//	}
//
//	@PostMapping("employees")
//	public Employee addEmployee(@RequestBody Employee employee) {
//		return empService.addEmployee(employee);
//	}
//
//	@PutMapping("employees")
//	public Employee updateEmployee(@RequestBody Employee employee) {
//		return empService.updateEmployee(employee);
//	}
//
//	@DeleteMapping("employees/{id}")
//	public Employee deleteEmployee(@PathVariable(name = "id") int employeeId) {
//		System.out.println(employeeId);
//		return empService.deleteEmployee(employeeId);
//	}
//}
//
////package com.ibm.demo.controller;
////
////import java.util.List;
////
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.web.bind.annotation.GetMapping;
////import org.springframework.web.bind.annotation.PostMapping;
////import org.springframework.web.bind.annotation.RestController;
////
////import com.ibm.demo.model.Employee;
////import com.ibm.demo.service.EmployeeService;
////
////@RestController
////public class EmployeeController {
////
//////	private EmployeeService empService = new EmployeeService();
////	
////	@Autowired
////	private EmployeeService empService;
////
////	@GetMapping("employees")
////	public Employee getEmployeeById() {
////		int employeeId = 1;
////		return empService.getEmployeeById(employeeId);
////	}
////
//////	@GetMapping("employees")
//////	public List<Employee> getAllEmployees() {
//////		return empService.getAllEmployees();
//////	}
//////
//////	public Employee addEmployee(Employee employee) {
//////		return empService.addEmployee(employee);
//////	}
//////
//////	public Employee updateEmployee(Employee employee) {
//////		return empService.updateEmployee(employee);
//////	}
//////
//////	public Employee deleteEmployee(int id) {
//////		int employeeId = 2;
//////		return empService.deleteEmployee(employeeId);
//////	}
////
////}
