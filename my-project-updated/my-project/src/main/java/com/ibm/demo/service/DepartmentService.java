package com.ibm.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ibm.demo.exception.DepartmentNotFoundException;
import com.ibm.demo.model.Department;
import com.ibm.demo.repository.DepartmentRepository;

@Service
public class DepartmentService {

	@Autowired
	private DepartmentRepository deptRepository;

	public List<Department> getAllDepartments() {
		return deptRepository.findAll();
	}

	public Department getDepartmentById(Long id) {
		return deptRepository.findById(id)
				.orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));
	}

	public Department getDepartmentByName(String name) {
		return deptRepository.findByNameIgnoreCase(name)
				.orElseThrow(() -> new DepartmentNotFoundException("Department not found with name: " + name));
	}

	public Department addDepartment(Department department) {
		return deptRepository.save(department);
	}

	public Department updateDepartment(Department department) {
		deptRepository.findById(department.getId())
				.orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + department.getId()));
		return deptRepository.save(department);
	}

	public Department deleteDepartment(Long id) {
		Department dept = deptRepository.findById(id)
				.orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));
		deptRepository.delete(dept);
		return dept;
	}

}
