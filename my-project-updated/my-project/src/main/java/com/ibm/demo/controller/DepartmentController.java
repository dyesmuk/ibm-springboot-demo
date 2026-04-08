package com.ibm.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

import com.ibm.demo.model.Department;
import com.ibm.demo.service.DepartmentService;

@RestController
@RequestMapping("api")
public class DepartmentController {

	@Autowired
	private DepartmentService deptService;

	@GetMapping("departments")
	public ResponseEntity<List<Department>> getAllDepartments() {
		List<Department> deptList = deptService.getAllDepartments();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Message", "Departments returned successfully.");
		return new ResponseEntity<>(deptList, headers, HttpStatus.OK);
	}

	@GetMapping("departments/{id}")
	public ResponseEntity<Department> getDepartmentById(@PathVariable(name = "id") Long id) {
		return ResponseEntity.status(200)
				.header("Message", "Department returned successfully.")
				.body(deptService.getDepartmentById(id));
	}

	@GetMapping("departments/name/{name}")
	public ResponseEntity<Department> getDepartmentByName(@PathVariable(name = "name") String name) {
		return ResponseEntity.status(200)
				.header("Message", "Department returned successfully.")
				.body(deptService.getDepartmentByName(name));
	}

	@PostMapping("departments")
	public ResponseEntity<Department> addDepartment(@RequestBody Department department) {
		return ResponseEntity.status(201)
				.header("Message", "Department added successfully.")
				.body(deptService.addDepartment(department));
	}

	@PutMapping("departments")
	public ResponseEntity<Department> updateDepartment(@RequestBody Department department) {
		return ResponseEntity.status(200)
				.header("Message", "Department updated successfully.")
				.body(deptService.updateDepartment(department));
	}

	@DeleteMapping("departments/{id}")
	public ResponseEntity<Department> deleteDepartment(@PathVariable(name = "id") Long id) {
		return ResponseEntity.status(200)
				.header("Message", "Department deleted successfully.")
				.body(deptService.deleteDepartment(id));
	}

}
