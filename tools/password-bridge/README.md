# SAU_USU Password Bridge (reversible → BCrypt)

One-time migration tool for the **SAU_USU** auth slice. Converts the legacy GeneXus **reversible**
password scheme (`UsuSen = encrypt64(plaintext, UsuKey)`, per-user key) into one-way **BCrypt**, so the
modern app never stores recoverable passwords. **Not part of the app build** — a standalone module.

## What it does (per user, idempotent)
1. Reads rows where `UsuKey IS NOT NULL` (i.e. not yet migrated) and `UsuSen` non-empty.
2. `plain = com.genexus.util.Encryption.decrypt64(UsuSen, UsuKey)` (legacy runtime, via reflection).
3. `UPDATE SAU_USU SET UsuSen = <bcrypt(plain)>, UsuKey = NULL WHERE UsuCod = ?`.
   - **`UsuKey IS NULL` = the "migrated" marker.** The modern `AuthService` verifies null-key users with
     BCrypt and never touches the legacy cipher. No schema change (reuses `UsuSen`/`UsuKey`).
4. Self-verifies each new hash; skips empty passwords and decrypt failures (logged, never bcrypted as
   garbage). **Never logs plaintext or hashes.** DRY-RUN by default.

## ⚠️ The one prerequisite you must provide: the GeneXus runtime jar
`com.genexus.util.Encryption` is **NOT in this repo** — it ships with the legacy app on the server
(Tomcat). Reimplementing the cipher blind would risk wrong decryptions and locking everyone out, so this
tool calls the real class. Get the jar that contains `com/genexus/util/Encryption.class` from the legacy
server (typically under the GeneXus webapp / Tomcat `lib`, e.g. a `gxclassR*.jar` / GeneXus runtime jar),
**or run this tool on the legacy host** where that jar is already on the classpath.

## Run (against a NON-PROD snapshot first)
```bash
cd receituario-modern/tools/password-bridge
export DB_URL='jdbc:postgresql://host.docker.internal:5432/saude-mandaguari'
export DB_USER='sysmar'
export DB_PASSWORD='...'                      # literal; do not commit
export GENEXUS_JAR='/abs/path/to/genexus-runtime.jar'

# 1) DRY-RUN — decrypts + bcrypts in memory, writes NOTHING, prints counts:
mvn -q exec:java -Dgenexus.jar.path="$GENEXUS_JAR" -Dexec.args="--dry-run --limit 20"

# 2) Full dry-run (all users):
mvn -q exec:java -Dgenexus.jar.path="$GENEXUS_JAR" -Dexec.args="--dry-run"

# 3) COMMIT (writes bcrypt + nulls UsuKey) — only after the dry-run looks right:
mvn -q exec:java -Dgenexus.jar.path="$GENEXUS_JAR" -Dexec.args="--commit"
```

Expected on the snapshot: ~1228 migrated, a couple "empty password" skipped (the 2 zero-length rows).
**Investigate any `decrypt failures`** before cutover — those users would be unable to log in.

## How the app consumes the result
- After a successful bridge run, every user has `UsuKey = NULL` and `UsuSen = <bcrypt>`.
- The modern `AuthService` (built in `/migrate-slice SAU_USU`): `UsuKey IS NULL` → verify with BCrypt;
  `UsuKey IS NOT NULL` → **not yet migrated** → reject with "redefina sua senha" (or re-run the bridge).
- New passwords (set via `/auth/change-password`) are stored as BCrypt with `UsuKey = NULL`.

## Safety checklist
- [ ] Run on the **non-prod snapshot** first; verify a few logins manually before prod.
- [ ] Take a DB backup of `SAU_USU` before `--commit` on any environment you can't rebuild.
- [ ] Coordinate with the legacy app: while both run on the same DB, legacy still writes reversible
      passwords for users changed there — re-run the bridge at cutover to catch stragglers.
- [ ] Confirm the **SYSMAR backdoor** decision (OQ2) separately — this tool does not touch it.
