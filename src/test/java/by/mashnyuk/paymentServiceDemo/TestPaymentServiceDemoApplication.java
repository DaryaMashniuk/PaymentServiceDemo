package by.mashnyuk.paymentServiceDemo;

import org.springframework.boot.SpringApplication;

public class TestPaymentServiceDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentServiceDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
