package com.ibm.demo.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;

@Entity // mandatory annotation - creates table
@Table(name = "employees") // optional - gives custom name to the table
public class Employee {

	@Id // PK column // mandatory annotation
	@GeneratedValue(strategy = GenerationType.IDENTITY) // generated PK values automatically
	@Column(name = "id") // optional - gives custom name to the column
	private int id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;
	
	@Column(name = "salary", nullable = false)
	@Positive(message = "Salary must be greater than 0")
	private double salary;
	
//	@Column(name = "email", unique = true)
//	private String email;
	
//	@CreationTimestamp
//	private LocalDateTime createdAt;

//	@UpdateTimestamp
//	private LocalDateTime updatedAt;

	public Employee() {
		super();
	}

	public Employee(String name, double salary) {
		super();
		this.name = name;
		this.salary = salary;
	}

	public Employee(int id, String name, double salary) {
		super();
		this.id = id;
		this.name = name;
		this.salary = salary;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getSalary() {
		return salary;
	}

	public void setSalary(double salary) {
		this.salary = salary;
	}

	@Override
	public String toString() {
		return "Employee [id=" + id + ", name=" + name + ", salary=" + salary + "]";
	}

}

//package com.ibm.demo.model;
//
//public class Employee {
//
//	private int id;
//	private String name;
//	private double salary;
//
//	public Employee() {
//		super();
//	}
//
//	public Employee(int id, String name, double salary) {
//		super();
//		this.id = id;
//		this.name = name;
//		this.salary = salary;
//	}
//
//	public int getId() {
//		return id;
//	}
//
//	public void setId(int id) {
//		this.id = id;
//	}
//
//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
//
//	public double getSalary() {
//		return salary;
//	}
//
//	public void setSalary(double salary) {
//		this.salary = salary;
//	}
//
//	@Override
//	public String toString() {
//		return "Employee [id=" + id + ", name=" + name + ", salary=" + salary + "]";
//	}
//
//}
