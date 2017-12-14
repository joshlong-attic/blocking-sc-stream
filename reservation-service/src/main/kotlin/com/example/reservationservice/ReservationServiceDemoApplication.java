package com.example.reservationservice;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
class Foo {
}

//@SpringBootApplication
public class ReservationServiceDemoApplication {

	public static void main2(String args[]) {
		new SpringApplicationBuilder()
				.sources(ReservationServiceDemoApplication.class)
				.initializers((ApplicationContextInitializer<GenericApplicationContext>) ctx -> {

					ctx.registerBean(RouterFunction.class,
							() -> route(GET("/hi"), request -> ok().body(Flux.just("Hi"), String.class)) );

					if (Math.random() > .5)
						ctx.registerBean(ApplicationRunner.class, () -> a -> System.out.println("Hello, world!"));

				})
				.run(args);
	}
}
