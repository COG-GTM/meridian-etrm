# Meridian ETRM

## What this is
Meridian ETRM is a Java trading desk application for crude and refined
products. It covers deal capture, reference data, positions and MTM, risk
reporting, settlement, invoicing, and end-of-day processing for Meridian Energy
Trading.

## Prerequisites
- JDK 8 or newer
- Maven 3.6 or newer
- Permission to create local data and output directories

The project uses Java 8 source and target settings and runs on the supported JDK
versions.

## Build
```bash
mvn -DskipTests package
```
## Test
```bash
mvn test
```
The build creates `target/meridian-etrm.jar`.

## Command line operations
| Command | Prints or writes |
| --- | --- |
| `java -jar target/meridian-etrm.jar server [port]` | Starts HTTP service, default port 8080. |
| `java -jar target/meridian-etrm.jar eod` | Prints book totals and writes `eod_position` rows. |
| `java -jar target/meridian-etrm.jar risk-report` | Prints the fixed-width risk report. |
| `java -jar target/meridian-etrm.jar invoice <dealRef>` | Prints and writes an invoice under `out/`. |
| `java -jar target/meridian-etrm.jar list-trades` | Prints one summary line per deal. |
The invoice command was run for `MET-2024-00007` and printed:

```text
MERIDIAN ENERGY TRADING INVOICE
Deal: MET-2024-00007
Invoice date: 2026-09-04
Amount: 4,704,000.00
Accrued interest: 20,580.00
Total: 4,724,580.00
```
## Database
The default H2 database is `jdbc:h2:./data/etrm;AUTO_SERVER=FALSE`. The schema
is created on startup and seed data is loaded when `counterparty` is empty.
Remove `data/` to reset local state:

```bash
rm -rf data/
```

Override the database URL with `-Detrm.db.url=`:

```bash
java -Detrm.db.url=jdbc:h2:./data/etrm_alt \
  -jar target/meridian-etrm.jar risk-report
```

Tests use `jdbc:h2:mem:test;DB_CLOSE_DELAY=-1`.

## REST endpoints
Start the service with:

```bash
java -jar target/meridian-etrm.jar server 18080
```

Response shapes differ between endpoints.

### `GET /health`
```bash
curl http://127.0.0.1:18080/health
```

```text
OK
```

### `GET /positions`
```bash
curl http://127.0.0.1:18080/positions
```

```json
{"positions":[{"book":"CRUDE-LDN","instrumentCode":"BRENT_PHYS","netQty":10000.0000},{"book":"CRUDE-LDN","instrumentCode":"DUBAI_PHYS","netQty":150000.0000},{"book":"PRODUCTS-HOU","instrumentCode":"GASOIL_10PPM","netQty":123000.0000},{"book":"PRODUCTS-HOU","instrumentCode":"JET_A1","netQty":38000.0000},{"book":"PRODUCTS-HOU","instrumentCode":"FUEL_OIL_380","netQty":27000.0000},{"book":"PRODUCTS-HOU","instrumentCode":"NAPHTHA_FULL","netQty":-90000.0000},{"book":"PRODUCTS-HOU","instrumentCode":"GASOLINE_95","netQty":36000.0000},{"book":"PRODUCTS-HOU","instrumentCode":"GASOLINE_87","netQty":-44000.0000}]}
```

### `GET /positions/{book}`
```bash
curl http://127.0.0.1:18080/positions/CRUDE-LDN
```

```json
[{"book":"CRUDE-LDN","instrument_code":"BRENT_PHYS","net_qty":10000.0000},{"book":"CRUDE-LDN","instrument_code":"DUBAI_PHYS","net_qty":150000.0000}]
```

This endpoint returns a bare array with snake_case fields.

### `GET /trades?counterparty=CODE`
```bash
curl 'http://127.0.0.1:18080/trades?counterparty=NORD'
```

```json
{"status":"OK","count":5,"data":[{"dealRef":"MET-2024-00001","book":"CRUDE-LDN","side":"BUY","quantity":100000.0000,"price":81.2500},{"dealRef":"MET-2024-00006","book":"CRUDE-LDN","side":"BUY","quantity":90000.0000,"price":83.7000},{"dealRef":"MET-2024-00010","book":"CRUDE-LDN","side":"SELL","quantity":45000.0000,"price":85.3000},{"dealRef":"MET-2024-00016","book":"PRODUCTS-HOU","side":"BUY","quantity":55000.0000,"price":465.0000},{"dealRef":"MET-2024-00021","book":"PRODUCTS-HOU","side":"SELL","quantity":28000.0000,"price":455.5000}]}
```

### `GET /risk/exposure`
```bash
curl http://127.0.0.1:18080/risk/exposure
```

```json
{"result":{"asOf":"2026-09-04","byCounterparty":[{"counterparty":"AURE","exposure":4473300.00000000},{"counterparty":"CAST","exposure":-23838300.00000000},{"counterparty":"KEST","exposure":-29300100.00000000},{"counterparty":"NORD","exposure":24640500.00000000},{"counterparty":"SOLR","exposure":5970850.00000000}]}}
```

### `GET /deals/{dealRef}`
```bash
curl http://127.0.0.1:18080/deals/MET-2024-00007
```

```json
{"dealRef":"MET-2024-00007","tradeDate":"2024-04-05","valueDate":"2024-03-01","tradeType":"FORWARD","side":"BUY","quantity":60000.0000,"price":78.4000,"book":"CRUDE-LDN","status":"CONFIRMED"}
```

An unknown deal reference returns HTTP 200 with `{"error":"not found"}`.
Unknown paths return HTTP 404 with `Not Found`.

## Sample data
Counterparties: Nordhaven Commodities AG (`NORD`), Solaris Refining LLC
(`SOLR`), Castellan Marine Fuels (`CAST`), Aurelia Petroleum Trading (`AURE`),
Kestrel Energy Partners (`KEST`), and Tidewater Products Ltd (`TIDE`).

Instruments: `BRENT_PHYS`, `DUBAI_PHYS`, `GASOIL_10PPM`, `JET_A1`,
`FUEL_OIL_380`, `NAPHTHA_FULL`, `GASOLINE_95`, and `GASOLINE_87`.

There are 25 deals across `CRUDE-LDN` and `PRODUCTS-HOU`, with spot, forward,
swap, physical, and financial transactions. Backdated examples include
`MET-2024-00007`, `MET-2024-00008`, `MET-2024-00013`, and `MET-2024-00019`.
Prices use business date `2024-06-28`; volatility, settlement configuration,
invoice support, and an empty fax queue are seeded.

## Configuration

- `rules.properties` configures validation and pricing rule classes.
- `fax.properties` controls the legacy fax branch.
- `settlement_config` stores accrued-interest settings.
- `etrm.db.url` overrides the database location.

## Module map

- `com.meridianenergy.etrm` — command-line entry point.
- `com.meridianenergy.etrm.core` — static wiring and database bootstrap.
- `com.meridianenergy.etrm.refdata` — counterparty and instrument access.
- `com.meridianenergy.etrm.deal` — deal model and capture service.
- `com.met.trading.rules` — validation and pricing plugins.
- `meridian.risk` — positions, MTM, VaR, and reports.
- `com.meridianenergy.etrm.settlement` — settlement, invoice, and confirmations.
- `com.meridianenergy.etrm.batch` — end-of-day processing.
- `com.meridianenergy.etrm.web` — HTTP handlers and JSON formatting.
- `com.meridianenergy.etrm.util` — dates, numbers, and string padding.

## Project layout

```text
meridian-etrm/
├── .gitignore
├── ARCHITECTURE.md
├── BACKLOG.md
├── README.md
├── pom.xml
└── src/
    ├── main/java/
    │   ├── com/meridianenergy/etrm/
    │   ├── com/met/trading/rules/
    │   └── meridian/risk/
    └── test/java/
```
