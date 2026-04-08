package com.ibm.demo.exception;

public class DepartmentNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -124775687737151522L;

	public DepartmentNotFoundException(String message) {
		super(message);
	}

}
