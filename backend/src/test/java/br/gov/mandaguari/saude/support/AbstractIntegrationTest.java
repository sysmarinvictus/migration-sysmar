package br.gov.mandaguari.saude.support;

import br.gov.mandaguari.saude.security.JwtService;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

/**
 * Base for HTTP integration tests: a real PostgreSQL via Testcontainers (Flyway runs V1 into it),
 * the full Spring context on a random port, and RestAssured. {@link #bearer(String...)} mints a
 * valid JWT directly via {@link JwtService} so tests don't depend on the (not-yet-migrated) SAU_USU
 * auth slice.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort int port;
    @Autowired protected JwtService jwt;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /** A request spec carrying a Bearer token for a user with the given roles. */
    protected RequestSpecification asUser(String... roles) {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + bearer(roles))
                .build();
    }

    /** A request spec with no authentication. */
    protected RequestSpecification anonymous() {
        return new RequestSpecBuilder().setContentType(ContentType.JSON).build();
    }

    protected String bearer(String... roles) {
        return jwt.issueAccessToken("tester", List.of(roles));
    }
}
