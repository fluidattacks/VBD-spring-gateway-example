# Synthetic vulnerability benchmark — NOT FOR PRODUCTION

This repository is a **deliberately vulnerable fork** of
[indrabasak/spring-gateway-example](https://github.com/indrabasak/spring-gateway-example),
maintained by Fluid Attacks as a labeled benchmark for evaluating vulnerability
detection tools (SAST engines and LLM-based security agents).

- The branch `benchmark/synthetic-vulns` contains commits that intentionally
  introduce security weaknesses into otherwise clean code. Each injection is a
  "twin": the parent commit is the clean version, the child commit is the
  vulnerable version.
- Every injected sample is recorded in `GROUND_TRUTH.csv` (file, line range,
  weakness class, trigger, exploitation path, clean/vulnerable commit hashes).
- Candidates that were evaluated but rejected as non-genuine hosts for their
  claimed weakness class are recorded in `GROUND_TRUTH_rejected.csv` with the
  reason, so the benchmark stays honest.
- Samples target the Fluid Attacks category *Access Subversion / Security
  Controls Bypass or Absence* where this codebase has a genuine host: F115
  (rate limiting), F305 (business limit on data creation) and F345 (debug or
  support backdoor), plus one authentication bypass via a spoofable header.
  The mobile-only findings in that category (F206-F210, F374-F376, F436) and
  F212 (Cloudflare) have no possible host in a Spring gateway and were not
  injected.
- Where the clean codebase lacked the control altogether, the control is first
  added as a real feature in its own clean commit, and the next commit makes
  it bypassable. The manifest's `clean_commit` is always the parent of
  `vuln_commit`. The branch is cumulative: a later sample's clean parent still
  contains the earlier samples.
- The code bodies do not announce that they are vulnerable (so a detector cannot
  cheat off a comment); the labeling lives here, in the commit messages and in
  the manifests. Evaluation harnesses must withhold those from the detector.

**Do not deploy this code. Do not contribute these changes upstream.**
