package dev.eric_muganga.cinema;

import org.springframework.boot.SpringApplication;

public class TestCinemaApplication {

	public static void main(String[] args) {
		SpringApplication.from(CinemaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
