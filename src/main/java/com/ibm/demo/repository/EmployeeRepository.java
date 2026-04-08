package com.ibm.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ibm.demo.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

	public abstract List<Employee> findByName(String name);

	public abstract List<Employee> findByNameIgnoreCase(String name);

	public abstract List<Employee> findByNameStartingWith(String name);

	public abstract List<Employee> findBySalaryGreaterThan(String name);

	public abstract List<Employee> findBySalaryLessThan(String name);

	public abstract List<Employee> findBySalaryInBetween(String name);

	// public abstract List<Employee> findByEmailIgnoreCase(String name);

//	public abstract List<Employee> findByNameIgnoreCase(String name);

//	for general CRUD operations like,
//	find all, find by id, insert, update, delete 
//	no need to declare any methods here 
//	for business specific requirements, 
//	need to declare methods here 
//	for more info - 
//	https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation

}
