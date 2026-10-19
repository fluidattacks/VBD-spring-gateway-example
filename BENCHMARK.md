# VBD-spring-gateway-example: synthetic vulnerability benchmark

This fork is a **synthetic, openly labelled benchmark**. It is not the upstream project
and must not be used as one. Each sample is an isolated pair of branches off `baseline`:

- `baseline`: the pinned upstream commit plus any hardening; the common ancestor.
- `smpl/<NNN>/safe`: the genuine negative (validated control) of sample `NNN`.
- `smpl/<NNN>/vuln`: `safe` plus exactly one commit that weakens the control.

The class and the coordinates of every sample live only in `GROUND_TRUTH.csv` on this
orphan branch, never in the analysed branches.

| sample | file | class |
|---|---|---|
| 001 | `edge-service/src/main/java/com/basaki/edge/filter/global/AuthenticationGlobalFilter.java` | client_trusted_value |
| 002 | `edge-service/src/main/java/com/basaki/edge/filter/global/RateLimitGlobalFilter.java` | rate_limit_key_trust |
| 003 | `book-service/src/main/java/com/basaki/k8s/service/CreationQuota.java` | client_trusted_value |
| 004 | `edge-service/src/main/java/com/basaki/edge/security/basic/BasicAuthProvider.java` | session_backdoor |
