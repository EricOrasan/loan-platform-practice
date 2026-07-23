package com.btproject.loanplatform.loan_application_service;

import org.springframework.boot.SpringApplication;

public class TestLoanApplicationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(LoanApplicationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
