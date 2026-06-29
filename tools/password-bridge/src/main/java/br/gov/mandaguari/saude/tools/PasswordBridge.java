package br.gov.mandaguari.saude.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SAU_USU password bridge — migrates the legacy GeneXus REVERSIBLE password scheme to one-way BCrypt.
 *
 * <p>Legacy: {@code UsuSen = com.genexus.util.Encryption.encrypt64(plaintext, UsuKey)} with a per-user
 * key in {@code UsuKey}. This tool, for each not-yet-migrated user, decrypts with the legacy runtime,
 * re-hashes the plaintext with BCrypt, and writes:
 * <pre>UPDATE SAU_USU SET UsuSen = &lt;bcrypt&gt;, UsuKey = NULL WHERE UsuCod = ?</pre>
 * {@code UsuKey IS NULL} is the "migrated" marker — the modern AuthService treats null-key users as BCrypt
 * and never touches the legacy cipher again. Idempotent: rows already migrated (UsuKey null) are skipped.
 *
 * <h2>Why reflection?</h2>
 * {@code com.genexus.util.Encryption} is the GeneXus runtime, NOT in this repo — it lives on the legacy
 * server. We load it via reflection so this tool has no compile-time dependency on it; supply the runtime
 * jar on the classpath at run time (see README). This avoids reimplementing the cipher (which would risk
 * silently-wrong decryptions and locking everyone out).
 *
 * <h2>Safety</h2>
 * <ul>
 *   <li>DRY-RUN by default — pass {@code --commit} to actually write.</li>
 *   <li>Never logs plaintext or any hash; only counts and UsuCod.</li>
 *   <li>Self-verifies (bcrypt.matches(plain, newHash)) before writing.</li>
 *   <li>Skips empty passwords and rows that fail to decrypt (logged, NOT bcrypted as garbage).</li>
 *   <li>Run against a NON-PROD snapshot first.</li>
 * </ul>
 *
 * <h2>Run</h2>
 * <pre>
 *   export DB_URL=jdbc:postgresql://host.docker.internal:5432/saude-mandaguari
 *   export DB_USER=sysmar DB_PASSWORD='...'
 *   mvn -q exec:java -Dgenexus.jar.path=/path/to/genexus-runtime.jar -Dexec.args="--dry-run"
 *   # when satisfied:  -Dexec.args="--commit"
 * </pre>
 */
public final class PasswordBridge {

    private static final String GX_CLASS = "com.genexus.util.Encryption";

    public static void main(String[] args) throws Exception {
        boolean commit = false;
        Integer limit = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--commit" -> commit = true;
                case "--dry-run" -> commit = false;
                case "--limit" -> limit = Integer.parseInt(args[++i]);
                default -> { System.err.println("Unknown arg: " + args[i]); System.exit(2); }
            }
        }

        String url = req("DB_URL"), user = req("DB_USER"), pass = req("DB_PASSWORD");

        // Resolve the legacy decrypt64(String cipher, String key) via reflection (jar provided at runtime).
        Method decrypt64;
        try {
            Class<?> enc = Class.forName(GX_CLASS);
            decrypt64 = enc.getMethod("decrypt64", String.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            System.err.println("""
                FATAL: %s.decrypt64(String,String) not on the classpath.
                Provide the GeneXus runtime jar, e.g. -Dgenexus.jar.path=/path/to/genexus.jar
                (the jar with com/genexus/util/Encryption.class — it lives on the legacy server,
                NOT in this repo). Aborting before touching any data.""".formatted(GX_CLASS));
            System.exit(3);
            return;
        }

        var encoder = new BCryptPasswordEncoder();   // default strength 10
        System.out.printf("MODE = %s | source = %s%n", commit ? "COMMIT (writes!)" : "DRY-RUN (no writes)", url);

        long migrated = 0, skippedEmpty = 0, failedDecrypt = 0, alreadyDone = 0, verifyFail = 0;

        String select = "SELECT usucod, ususen, usukey FROM sau_usu "
                + "WHERE usukey IS NOT NULL AND length(usukey) > 0 AND ususen IS NOT NULL AND length(ususen) > 0 "
                + "ORDER BY usucod" + (limit != null ? " LIMIT " + limit : "");

        try (Connection con = DriverManager.getConnection(url, user, pass)) {
            con.setAutoCommit(false);
            try (PreparedStatement sel = con.prepareStatement(select);
                 ResultSet rs = sel.executeQuery();
                 PreparedStatement upd = con.prepareStatement(
                         "UPDATE sau_usu SET ususen = ?, usukey = NULL WHERE usucod = ? AND usukey IS NOT NULL")) {

                while (rs.next()) {
                    int usucod = rs.getInt("usucod");
                    String cipher = rs.getString("ususen");
                    String key = rs.getString("usukey");

                    String plain;
                    try {
                        plain = (String) decrypt64.invoke(null, cipher, key);
                    } catch (Exception ex) {
                        failedDecrypt++;
                        System.out.printf("  [DECRYPT-FAIL] usucod=%d (%s) — left untouched%n",
                                usucod, ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : ex.getClass().getSimpleName());
                        continue;
                    }
                    if (plain == null || plain.isEmpty()) {       // empty/blank legacy password
                        skippedEmpty++;
                        continue;
                    }
                    String hash = encoder.encode(plain);
                    if (!encoder.matches(plain, hash)) {           // paranoia: never write an unverifiable hash
                        verifyFail++;
                        System.out.printf("  [VERIFY-FAIL] usucod=%d — skipped%n", usucod);
                        continue;
                    }
                    if (commit) {
                        upd.setString(1, hash);
                        upd.setInt(2, usucod);
                        alreadyDone += (upd.executeUpdate() == 0 ? 1 : 0); // 0 rows = someone migrated it meanwhile
                    }
                    migrated++;
                    // plaintext goes out of scope here; never logged.
                }
            }
            if (commit) { con.commit(); } else { con.rollback(); }
        }

        System.out.printf("""
            ── RESULT ──────────────────────────────
              would-migrate / migrated : %d
              skipped (empty password) : %d
              decrypt failures         : %d
              verify failures          : %d
              raced/already-migrated   : %d
            %s
            """, migrated, skippedEmpty, failedDecrypt, verifyFail, alreadyDone,
                commit ? "COMMITTED — these users now use BCrypt (usukey=NULL)."
                       : "DRY-RUN — nothing written. Re-run with --commit when satisfied.");
        if (failedDecrypt > 0 || verifyFail > 0) {
            System.out.println("⚠ Some rows could not be migrated — investigate before cutover (they would be unable to log in).");
        }
    }

    private static String req(String env) {
        String v = System.getenv(env);
        if (v == null || v.isBlank()) { System.err.println("Missing env var: " + env); System.exit(2); }
        return v;
    }

    private PasswordBridge() {}
}
