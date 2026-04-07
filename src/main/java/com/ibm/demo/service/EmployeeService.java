package com.ibm.demo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ibm.demo.model.Employee;

@Service
public class EmployeeService {
	
	// refactor or complete the code in this class 

	private List<Employee> employees = new ArrayList<Employee>(Arrays.asList(new Employee(1, "Sonu", 90000),
			new Employee(2, "Monu", 95000), new Employee(3, "Tonu", 92000), new Employee(4, "Ponu", 94000)));

	public Employee getEmployeeById(int id) {
		// refactor the code below
		Employee emp = employees.get(id);
		System.out.println(emp.toString());
		return emp;
	}

	public List<Employee> getAllEmployees() {
		System.out.println(employees.size());
		return employees;
	}

	public Employee addEmployee(Employee employee) {
		// refactor the code below
		System.out.println();
		return null;
	}
	
	public Employee updateEmployee(Employee employee) {
		// refactor the code below
		System.out.println();
		return null;
	}
	
	public Employee deleteEmployee(int id) {
		// refactor the code below
		System.out.println();
		return null;
	}

}
