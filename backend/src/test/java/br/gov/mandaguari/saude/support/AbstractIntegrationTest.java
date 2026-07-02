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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

/**
 * Base for HTTP integration tests: a real PostgreSQL via Testcontainers (Flyway runs V1 into it),
 * the full Spring context on a random port, and RestAssured. {@link #bearer(String...)} mints a
 * valid JWT directly via {@link JwtService} so tests don't depend on the (not-yet-migrated) SAU_USU
 * auth slice.
 *
 * <p><b>Singleton container.</b> The Postgres container is started ONCE per JVM in the static
 * initializer and shared by every integration test class — NOT via {@code @Container}/
 * {@code @Testcontainers}, which start and stop one container per class and exhaust resources when
 * several IT classes run in the same build. Because the container (and therefore the datasource
 * URL) is identical for all subclasses, Spring reuses a single cached application context across
 * them. The container is reaped by Testcontainers' Ryuk sidecar / the JVM shutdown.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

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

    /**
     * A request spec for a token whose subject is a numeric {@code UsuCod} — required to exercise the
     * per-program RBAC engine ({@code @authz.can} / {@link br.gov.mandaguari.saude.autorizacao.service.PermissionResolver}),
     * which resolves permissions by user id and denies non-numeric principals. Seed the matching
     * {@code SAU_USU} (+ {@code SAU_PRFCON}/{@code SAU_USUCON}) rows in the test's setup.
     */
    protected RequestSpecification asUserId(int usuCod, String... roles) {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + jwt.issueAccessToken(String.valueOf(usuCod), List.of(roles)))
                .build();
    }

    protected String bearer(String... roles) {
        return jwt.issueAccessToken("tester", List.of(roles));
    }
}
