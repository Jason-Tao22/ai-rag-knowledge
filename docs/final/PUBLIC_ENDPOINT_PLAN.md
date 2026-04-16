# Public Endpoint Plan

## What we are using

For presentation day, the easiest public-access path is a Cloudflare Quick Tunnel that forwards a public URL to our local frontend at `http://localhost:80`.

This means:

- we do not need to buy a domain
- we do not need to move the whole stack to a separate VM just to demo it
- the instructional staff can open a public HTTPS URL from their own laptops

## Important limitation

This gives us a temporary public URL, not a permanent public IP. For the class requirement, that is still acceptable because the project only needs to be reachable on presentation day.

## How to start it

From the repository root:

```bash
bash scripts/start_public_endpoint.sh
```

The script prints a `https://...trycloudflare.com` URL.

## How to stop it

```bash
bash scripts/stop_public_endpoint.sh
```

## Recommended presentation-day workflow

1. start Docker services
2. run the smoke test
3. start the public endpoint script
4. copy the generated public URL into the slide notes or a shared doc
5. keep the laptop awake and the containers running during the demo

## Suggested full checklist

```bash
docker compose -f docker-compose.final.yml up --build -d
bash scripts/presentation_smoke_test.sh http://localhost
bash scripts/start_public_endpoint.sh
```

## If we need something more stable

If the team later wants a fixed hostname or a long-lived endpoint, we would need either:

- a Cloudflare account with a named tunnel and DNS
- a cloud VM with a public IP

For now, the quick tunnel is the fastest presentation-ready option.
