package com.ibm.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

//	view the output on 
//	http://localhost:8080/hello

	@RequestMapping("hello")
	public String hello() {
		System.out.println("Hello");
		return "Hello world!";
	}
	
	@RequestMapping("hi")
	public String hi() {
		System.out.println("Hi");
		return "Hi! How are you?";
	}

	@RequestMapping
	public String welcome() {
		System.out.println("Welcome");
		return "Welcome to IBM Springboot app";
	}
}



