package com.ibm.demo.exception;

public class EmployeeNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -3889658260930060573L;

	public EmployeeNotFoundException(String message) {
		super(message);
	}

}