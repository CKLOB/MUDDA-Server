package team.cklob.mudda

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class MuddaApplication

fun main(args: Array<String>) {
	runApplication<MuddaApplication>(*args)
}
