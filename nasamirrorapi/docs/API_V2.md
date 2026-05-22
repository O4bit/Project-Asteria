# NASA Proxy API — v2

All responses are JSON. Successful APOD/NeoWs payloads mirror NASA's native
shape so existing clients can deserialize them with their existing models.

Base URL (production): `https://<your-worker-host>`

## Auth

None for clients. The server attaches `NASA_API_KEY` to upstream calls. Clients
**must not** send `api_key` — it is rejected as `invalid_request`.

## Rate limit

100 requests per IP per rolling hour. Enforced **before** any NASA call.
Exceeding returns HTTP 429:

```json
{ "code": "rate_limit_exceeded",
  "message": "Rate limit exceeded (100/hour).",
  "retry_after_seconds": 1234 }
```

## Error contract

Every error response is:

```json
{ "code": "<machine_code>", "message": "<human readable>", "retry_after_seconds": null }
```

Codes: `invalid_request` (400), `rate_limit_exceeded` (429),
`upstream_rate_limited` (502), `upstream_invalid` (502),
`upstream_unavailable` (503), `service_unavailable` (503),
`service_misconfigured` (500).

## Endpoints

### `GET /v2/healthz`

```bash
curl -sS https://api.example.com/v2/healthz
# {"status":"ok","service":"nasa-mirror-api","version":"v2"}
```

### `GET /v2/nasa/apod/today`

```bash
curl -sS https://api.example.com/v2/nasa/apod/today
```

### `GET /v2/nasa/apod?date=YYYY-MM-DD`

```bash
curl -sS "https://api.example.com/v2/nasa/apod?date=2024-08-15"
```

### `GET /v2/nasa/apod/range?start_date=…&end_date=…`

Returns an array. Max 30 days.

```bash
curl -sS "https://api.example.com/v2/nasa/apod/range?start_date=2024-08-10&end_date=2024-08-15"
```

### `GET /v2/nasa/neows/feed?start_date=…&end_date=…`

Max 7 days (NeoWs hard limit; enforced server-side).

```bash
curl -sS "https://api.example.com/v2/nasa/neows/feed?start_date=2024-08-10&end_date=2024-08-12"
```

## Deploy

```bash
cd nasamirrorapi
wrangler secret put NASA_API_KEY      # paste your key
wrangler deploy
```
