package com.example.reservationclient

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.filters
import org.springframework.cloud.gateway.route.builder.routes
import org.springframework.cloud.netflix.hystrix.HystrixCommands
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.context.support.beans
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux

/*

interface ProducerChannels {

	@Output("output")
	fun output(): MessageChannel
}
*/

@EnableBinding(Source::class)
@SpringBootApplication
class ReservationClientApplication

fun main(args: Array<String>) {
	SpringApplicationBuilder()
			.sources(ReservationClientApplication::class.java)
			.initializers(beans {

				bean {
					val lbf = ref<LoadBalancerExchangeFilterFunction>()
					WebClient.builder().filter(lbf).build()
				}
				bean {

					val client = ref<WebClient>()
					val outputChannel = ref<Source>().output()
					router {

						POST("/reservations") {
							it.bodyToMono<Reservation>()
									.map { it.reservationName }
									.map { MessageBuilder.withPayload(it!!).build() }
									.map { outputChannel.send(it) }
									.flatMap { ServerResponse.ok().build() }
						}

						GET("/reservations/names") {

							val reservations: Publisher<String> = client
									.get()
									.uri("http://reservation-service/reservations")
									.retrieve()
									.bodyToFlux<Reservation>()
									.map { it.reservationName }

							val cb = HystrixCommands
									.from(reservations)
									.commandName("reservation-names")
									.eager()
									.fallback(Flux.just("EEEEK!!!"))
									.build()

							ServerResponse.ok().body(cb)
						}
					}
				}
				bean {
					DiscoveryClientRouteDefinitionLocator(ref())
				}
				bean {
					// authentication
					val usr = User.withDefaultPasswordEncoder()
							.username("user")
							.password("pw")
							.roles("USER")
							.build()
					MapReactiveUserDetailsService(usr)
				}
				bean {
					//@formatter:off
					ref<ServerHttpSecurity>()
							.csrf().disable()
							.httpBasic()
							.and()
							.authorizeExchange()
								.pathMatchers("/rl").authenticated()
								.anyExchange().permitAll()
							.and()
							.build()
					//@formatter:on
				}
				bean {
					val locatorBuilder = ref<RouteLocatorBuilder>()
					locatorBuilder.routes {
						route {
							val rl = ref<RequestRateLimiterGatewayFilterFactory>()
									.apply(RedisRateLimiter.args(2, 4))
							path("/rl")
							filters {
								filter(rl)
							}
							uri("lb://reservation-service/reservations")
						}
					}
				}
			})
			.run(*args)
}

data class Reservation(var id: String? = null, var reservationName: String? = null)