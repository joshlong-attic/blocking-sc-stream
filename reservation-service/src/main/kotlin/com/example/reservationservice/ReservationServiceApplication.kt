package com.example.reservationservice

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux


@SpringBootApplication
class ReservationServiceApplication


@Configuration
@EnableBinding(Sink::class)
class Processor(private val reservationRepository: ReservationRepository) {

	@StreamListener
	fun incomingReservations(@Input(Sink.INPUT) names: Flux<String>) {
		names
				.map { it.toUpperCase() }
				.flatMap { reservationRepository.save(Reservation(reservationName = it)) }
				.subscribe {
					println("processing ${it.reservationName}.")
				}
	}
}

fun main(args: Array<String>) {
	SpringApplicationBuilder()
			.sources(ReservationServiceApplication::class.java)
			.initializers(beans {
				bean {
					val repo = ref<ReservationRepository>()
					val env = ref<Environment>()
					router {
						GET("/message") { ok().body(Flux.just(env["message"])) }
						GET("/reservations") { ok().body(repo.findAll()) }
					}
				}
				bean {
					ApplicationRunner {
						val reservationRepository = ref<ReservationRepository>()
						val names = Flux.just("Mario", "Josh", "Dave", "Dave",
								"Jon", "Pickle", "Rick", "Aaron", "Debin", "Santo", "Chris",
								"Brian", "Mark", "Bill", "Ben", "Oscar", "Vinesh", "Chris", "Kevin")
								.map { Reservation(reservationName = it) }
								.flatMap { reservationRepository.save(it) }
						reservationRepository
								.deleteAll()
								.thenMany(names)
								.thenMany(reservationRepository.findAll())
								.subscribe()
					}
				}
			})
			.run(*args)
}

interface ReservationRepository : ReactiveMongoRepository<Reservation, String>

@Document
data class Reservation(@Id var id: String? = null, var reservationName: String? = null)