# WireMock Session Commands & Fixtures

Use `hlens mock` with a session ID matching `.scratch/qa/<run-id>/` and WireMock 3 (`HLS_WIREMOCK_JAR` or `--jar`).

## Session Lifecycle

```bash
mkdir -p .scratch/qa/qa-run-01/wiremock
wm() { hlens mock --url http://127.0.0.1:8080 --session qa-run-01 --exclusive "$@"; }
trap 'wm reset > .scratch/qa/qa-run-01/wiremock/cleanup.json' EXIT
wm start
wm apply "$HLS_ROOT/examples/wiremock/rest-query.json" > .scratch/qa/qa-run-01/wiremock/apply.json
wm list
# Drive the real app UI with agent-device; never use curl to manufacture app evidence.
wm requests > .scratch/qa/qa-run-01/wiremock/requests.json
wm verify --name accounts-empty --min-hits 1 --max-hits 1 > .scratch/qa/qa-run-01/wiremock/verify.json
wm disable --name accounts-empty
wm reset
```

- `apply` upserts a stable ID per session/name, records the raw fixture SHA-256, and clears prior matched hits for that mapping.
- `disable` removes one mapping and its hits; `reset` removes all session mappings, restores scenarios, and deletes matched events without touching global WireMock state.
- For shared servers, replace `--exclusive` with `--scope-header X-QA-Session` and configure the debug app to send `X-QA-Session: <run-id>`.

## Matching, Faults & Passthrough

| Fixture pattern | Use case |
|---|---|
| `rest-query.json` / `rest-post.json` | REST query/body matching and status codes (`200`, `204`, `401/403`, `422`) |
| `graphql.json` / `graphql-error.json` | GraphQL `operationName`, JSONPath variables, or HTTP-200 GraphQL `errors` |
| `delayed.json` / `retry.json` | `fixedDelayMilliseconds` above client read timeout, or stateful `503 -> 200` retry |
| `passthrough.json` | Priority-10000 fallback (`wm apply passthrough.json --upstream https://staging.example.test`) |

## Mock Coverage Inventory & Exploratory QA Integration

When a scenario declares `apiMocks` or `expectations.wiremock`, include `expectations.mock_coverage` (`mode: "feature_api"` with `id`, `check_id`, `operation`, `method`, `path`, `api_source`, `contract_source`, `fixture_ids`, or `mode: "routing_only"` with reason) and `expectations.wiremock` (`[{"name":"...","min_hits":1,"max_hits":1,"fixture_sha256":"<sha256>"}]`).

Run the Exploratory QA reporter before `wm reset`:

```bash
python3 .agents/skills/exploratory-qa/scripts/report.py \
  --run-dir .scratch/qa/qa-run-01 \
  --scenario .scratch/qa/qa-run-01/scenario.json \
  --harnesslens-root /path/to/HarnessLens \
  --wiremock-url http://127.0.0.1:8080 --wiremock-session qa-run-01
```

## Discovery and validation
- **0 hits**: Check app base URL, cache invalidation, scope header, priority, and fixture hash; trigger via real UI.
- **Interrupted run**: Run `wm reset`, restore app routing, and stop owned WireMock process. Failed cleanup is never a PASS.
- **Adapter CLI tests**: Run `python3 -m unittest discover -s tests -p test_wiremock_cli.py -v` in the HarnessLens checkout.
