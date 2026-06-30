package br.gov.mandaguari.saude.openapi;

import br.gov.mandaguari.saude.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministically exports the backend's OpenAPI document to {@code frontend/openapi.json}, which the
 * frontend's {@code orval} consumes to generate its typed API client — so the SPA builds OFFLINE (CI +
 * Docker) without a running backend. Runs on the same Testcontainers context as the other ITs, so no real
 * DB is needed.
 *
 * <p>By default it only FETCHES + validates the spec (no file write → no CI churn). To refresh the
 * committed spec after an API change, run with {@code -Dopenapi.export=true}:
 * <pre>mvn -o -Dit.test=OpenApiSpecExportIT -Dopenapi.export=true -DfailIfNoTests=false verify</pre>
 * then commit the updated {@code frontend/openapi.json} (+ re-run {@code npm run gen:api} if the client
 * is checked locally). {@code /v3/api-docs} is permitAll, so no auth is required.
 */
class OpenApiSpecExportIT extends AbstractIntegrationTest {

    /** Committed spec path, relative to the backend module dir (the surefire/failsafe working dir). */
    static final Path SPEC = Path.of("..", "frontend", "openapi.json");

    @Test
    void exportsOpenApiSpecForFrontend() throws Exception {
        String json = given().spec(anonymous())
                .when().get("/v3/api-docs")
                .then().statusCode(200)
                .extract().asString();

        ObjectMapper om = new ObjectMapper();
        JsonNode tree = om.readTree(json);                 // preserves field order → stable diffs
        assertThat(tree.path("openapi").asText()).startsWith("3.");
        assertThat(tree.path("paths").size()).isGreaterThan(0);

        String pretty = om.writerWithDefaultPrettyPrinter().writeValueAsString(tree) + "\n";

        if (Boolean.getBoolean("openapi.export")) {
            Files.writeString(SPEC, pretty);
            System.out.println("[openapi] wrote " + SPEC.toAbsolutePath().normalize()
                    + " (" + tree.path("paths").size() + " paths)");
        }
    }
}
