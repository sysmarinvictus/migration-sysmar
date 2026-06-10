package br.gov.mandaguari.saude;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Receituário — modernized backend (migrated from the GeneXus webapp).
 * Spring Boot 3 / Java 21 / Spring Data JPA over the existing {@code saude-mandaguari} schema.
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
