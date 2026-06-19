# Phileas Trino connector demo

A self-contained demo that stands up [Trino](https://trino.io/) with the Phileas connector and a
Postgres source seeded with synthetic PII, so you can run column-level PII redaction in SQL and see
before and after output. All seed data is fabricated; there is no real PII here.

It pairs with the blog post
[PII Masking and Redaction in Trino with the Phileas Connector](https://philterd.ai/blog/the-phileas-trino-connector-redaction-as-sql/),
turning its SQL snippets into something you can actually run.

## What you get

- A Postgres database with two tables of synthetic data: `customers` (name, email, SSN, phone) and
  `support_transcripts` (free-text notes that mention the same details).
- Trino with the Phileas connector loaded and a sample redaction policy
  ([`policy/demo-policy.json`](policy/demo-policy.json)) covering emails, phone numbers, SSNs,
  credit cards, and names.
- The `phileas_redact(varchar)` SQL function, which applies the policy to any text column.

## Prerequisites

- Docker with Docker Compose.
- A JDK and Maven, to build the connector plugin once.

## 1. Build the connector plugin

From the repository root (one level up from this directory):

```sh
mvn -DskipTests package
```

This produces `target/phileas-trino-connector-481-SNAPSHOT/`, the plugin directory the demo mounts into
Trino. If the connector version changes, update the matching path in
[`docker-compose.yml`](docker-compose.yml).

## 2. Start the stack

From this `demo/` directory:

```sh
docker compose up
```

Postgres seeds itself from [`postgres/init.sql`](postgres/init.sql), and Trino loads the Phileas and
Postgres catalogs. Trino takes roughly half a minute to become ready. It is up when
`http://localhost:8080` responds.

## 3. Open a SQL session

Use the Trino CLI inside the running container:

```sh
docker compose exec trino trino
```

You can also point any Trino client at `http://localhost:8080` (no authentication in this demo).

## 4. Redact a column in a SELECT

Compare each raw value with its redacted form:

```sql
SELECT
    full_name,
    phileas_redact(full_name) AS full_name_redacted,
    email,
    phileas_redact(email)     AS email_redacted,
    phileas_redact(ssn)       AS ssn_redacted,
    phileas_redact(phone)     AS phone_redacted
FROM postgresql.public.customers;
```

The redacted columns replace detected values with the policy labels (`[NAME]`, `[EMAIL]`, `[SSN]`,
`[PHONE]`) while leaving the rest of the text intact.

## 5. Redact while joining across the query

The function works anywhere a `varchar` expression is valid, including across a join. Here the
free-text transcript is redacted as it is joined to the customer it belongs to:

```sql
SELECT
    c.id,
    phileas_redact(c.full_name) AS customer,
    phileas_redact(t.transcript) AS transcript_redacted
FROM postgresql.public.customers c
JOIN postgresql.public.support_transcripts t
  ON t.customer_id = c.id
ORDER BY c.id;
```

The transcripts mention names, emails, SSNs, phone numbers, and a card number in running prose; the
redacted column shows each detected value replaced inline.

## 6. Expose a redacted view

The production pattern from the blog post is to expose a view instead of the underlying table, so
downstream consumers query clean data without knowing redaction happened:

```sql
CREATE VIEW postgresql.public.customers_redacted AS
SELECT
    id,
    phileas_redact(full_name) AS full_name,
    phileas_redact(email)     AS email,
    phileas_redact(ssn)       AS ssn,
    phileas_redact(phone)     AS phone
FROM postgresql.public.customers;

SELECT * FROM postgresql.public.customers_redacted;
```

Grant access to the view rather than the table, and consumers only ever see redacted values. If your
Trino build does not permit creating a view in the Postgres catalog, run the same `SELECT` directly,
or create the view in a catalog that supports views.

## The policy

[`policy/demo-policy.json`](policy/demo-policy.json) is a standard Phileas redaction policy. Each
entity type maps to a filter strategy; here every type uses `REDACT` with a labeled format so the
output is easy to read. Edit the policy to change behavior: switch a type to `MASK`, keep the last
four digits of card numbers with `LAST_4`, or add and remove entity types. See the
[Phileas policy documentation](https://philterd.github.io/phileas/filter_policies/filter_policies/)
and the [redaction policy schema guide](https://philterd.ai/guides/redaction-policy-schema/) for the
full set of options.

## Notes and limits

- This demo uses pattern-based and dictionary-based detection only (emails, phones, SSNs, and card
  numbers are pattern-matched; names are matched against a dictionary), so it is fully self-contained
  with no model server to run. Dictionary-based name detection catches common names, not every name.
  For broader name detection, Phileas can call [PhEye](https://philterd.ai/ph-eye/), which is out of
  scope for this self-contained demo.
- The connector reads the policy file once at startup. After editing the policy, restart Trino
  (`docker compose restart trino`) for the change to take effect.
- Detection is probabilistic and configurable. Validate any policy against your own representative
  data before relying on it; you remain responsible for what reaches your query results.

## Shut down

```sh
docker compose down
```
