package com.ibm.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ibm.demo.model.Employee;
import com.ibm.demo.repository.EmployeeRepository;

@Service
public class EmployeeService {

	@Autowired
	private EmployeeRepository empRepository;

	public Employee getEmployeeById(int id) {
		return empRepository.findById(id).orElse(null);
	}

	public List<Employee> getAllEmployees() {
		return empRepository.findAll();
	}

	public Employee addEmployee(Employee employee) {
		return empRepository.save(employee);
	}

	public Employee updateEmployee(Employee employee) {
		return empRepository.save(employee);
	}

	public Employee deleteEmployee(int id) {
		Employee emp = empRepository.findById(id).orElse(null);
		if (emp != null) {
			empRepository.deleteById(id);
		}
		return emp;
	}
}

//package com.ibm.demo.service;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import org.springframework.stereotype.Service;
//
//import com.ibm.demo.model.Employee;
//
//@Service
//public class EmployeeService {
//
//	// implement as applicable -
//	// can some error message be given in the following cases?
//	// What if the given employee id in getEmployeeById is not found?
//	// What if the given employee id in addEmployee is already present?
//	// What if the given employee id in updateEmployee is not found?
//	// What if the given employee id in deleteEmployee is not found?
//
//	private List<Employee> employees = new ArrayList<Employee>(Arrays.asList(new Employee(101, "Sonu", 90000),
//			new Employee(102, "Monu", 95000), new Employee(103, "Tonu", 92000), new Employee(104, "Ronu", 94000)));
//
//	public Employee getEmployeeById(int id) {
//		for (Employee emp : employees) {
//			if (emp.getId() == id) {
//				return emp;
//			}
//		}
//		return null;
//	}
//
//	public List<Employee> getAllEmployees() {
//		System.out.println(employees.size());
//		return employees;
//	}
//
//	public Employee addEmployee(Employee employee) {
//		employees.add(employee);
//		return employee;
//	}
//
//	public Employee updateEmployee(Employee employee) {
//		for (int i = 0; i < employees.size(); i++) {
//			if (employees.get(i).getId() == employee.getId()) {
//				employees.set(i, employee);
//				return employee;
//			}
//		}
//		return null;
//	}
//
//	public Employee deleteEmployee(int id) {
//		for (int i = 0; i < employees.size(); i++) {
//			if (employees.get(i).getId() == id) {
//				return employees.remove(i);
//			}
//		}
//		return null;
//	}
//}
//
////
////package com.ibm.demo.service;
////
////import java.util.ArrayList;
////import java.util.Arrays;
////import java.util.List;
////
////import org.springframework.stereotype.Service;
////
////import com.ibm.demo.model.Employee;
////
////@Service
////public class EmployeeService {
////	
////	// refactor or complete the code in this class 
////
////	private List<Employee> employees = new ArrayList<Employee>(Arrays.asList(new Employee(1, "Sonu", 90000),
////			new Employee(2, "Monu", 95000), new Employee(3, "Tonu", 92000), new Employee(4, "Ponu", 94000)));
////
////	public Employee getEmployeeById(int id) {
////		// refactor the code below
////		Employee emp = employees.get(id);
////		System.out.println(emp.toString());
////		return emp;
////	}
////
////	public List<Employee> getAllEmployees() {
////		System.out.println(employees.size());
////		return employees;
////	}
////
////	public Employee addEmployee(Employee employee) {
////		// refactor the code below
////		System.out.println();
////		return null;
////	}
////	
////	public Employee updateEmployee(Employee employee) {
////		// refactor the code below
////		System.out.println();
////		return null;
////	}
////	
////	public Employee deleteEmployee(int id) {
////		// refactor the code below
////		System.out.println(id + "deleted");
////		return null;
////	}
////
////}
