package com.ibm.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ibm.demo.model.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

	public abstract Optional<Department> findByName(String name);

	public abstract Optional<Department> findByNameIgnoreCase(String name);

}
