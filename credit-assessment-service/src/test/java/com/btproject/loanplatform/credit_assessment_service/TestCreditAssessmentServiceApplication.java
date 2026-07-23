package com.btproject.loanplatform.credit_assessment_service;

import org.springframework.boot.SpringApplication;

public class TestCreditAssessmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(CreditAssessmentServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
