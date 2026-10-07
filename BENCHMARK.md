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
- The code bodies do not announce that they are vulnerable (so a detector cannot
  cheat off a comment); the labeling lives here, in the commit messages and in
  the manifests. Evaluation harnesses must withhold those from the detector.

**Do not deploy this code. Do not contribute these changes upstream.**
