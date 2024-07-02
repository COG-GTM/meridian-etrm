# Meridian ETRM Architecture

## Overview
Meridian ETRM is a single Maven module packaged as a runnable jar. It exposes
command-line workflows and a small HTTP service from the same process. The
application uses JDK HTTP classes, raw JDBC, H2, hand-built JSON, and a mixture
of `java.util.logging` and standard output.

The runtime begins in `com.meridianenergy.etrm.Main`. Startup creates database
objects, optionally loads seed rows, and then dispatches a command. There is no
separate application container.

## Module layout
### `com.meridianenergy.etrm`
This is the entry-point naming convention. `Main` handles command selection for
the server, EOD, risk report, invoices, and trade listing.

### `com.meridianenergy.etrm.core`
This package follows the central platform team convention. `AppContext` owns
static lazy service instances. `DbConnectionManager` creates a new JDBC
connection on each request. `SchemaBootstrap` executes SQL resources in order.

### `com.meridianenergy.etrm.refdata`
This package contains reference data POJOs and `RefDataDAO`. It reads
counterparties and instruments with direct SQL and maps selected columns by
position as well as by name.

### `com.meridianenergy.etrm.deal`
This is the deal desk convention. `Deal` is a mutable JavaBean, while
`DealCaptureService` provides capture, validation, persistence, queries,
formatting, confirmation dispatch, amendment, cancellation, and audit logging.
`DealCaptureService` is roughly 550 lines.

### `com.met.trading.rules`
This package uses the older plugin naming convention. It defines
`TradingRule`, `RuleContext`, `RuleResult`, and `RuleEngine`, with desk rules
under `crude`, `products`, and `common`.

### `meridian.risk`
This package follows a separate risk team convention without the `com.` prefix.
`PositionKeeper` aggregates positions, `MtmValuator` reads market prices,
`VarLiteCalculator` calculates VaR-lite, and `RiskReportService` builds reports.
`RiskReportService` is roughly 530 lines.

### `com.meridianenergy.etrm.settlement`
This package contains `SettlementEngine`, `InvoiceGenerator`,
`ConfirmationDispatcher`, `FaxConfirmationExporter`, and `FaxGatewayClient`.
The fax classes remain in the settlement area because the confirmation path is
owned by that operational workflow.

### `com.meridianenergy.etrm.batch`
`EodBatchJob` and `BatchRunner` run the end-of-day flow. The job creates worker
tasks for the configured books and writes rolled-up position rows.

### `com.meridianenergy.etrm.web`
`RestServer` uses `com.sun.net.httpserver.HttpServer`. `JsonUtil` renders JSON
without an external serialization dependency.

### `com.meridianenergy.etrm.util`
`DateUtil`, `NumberFmt`, and `StringUtils` contain shared static helpers used
throughout reporting and settlement.

## Runtime wiring
`AppContext` is a static holder with synchronized lazy getters. It manually
constructs `DealCaptureService`, `RefDataDAO`, the risk services, settlement
services, and invoice generation. There is no dependency injection container.

`DbConnectionManager.getConnection()` obtains credentials and the URL from the
process properties, then opens a fresh H2 connection. Services own their
statements and result sets with try-with-resources where the old code has been
updated to do so.

`SchemaBootstrap` reads `/sql/schema.sql` and `/sql/seed.sql` from the classpath.
Each resource is read as text and split on semicolons. The seed script runs only
when the counterparty table has no rows.

## Rules layer

`RuleEngine` reads comma-separated class names from `rules.properties`. It
loads classes with `Class.forName(...).newInstance()` and logs a warning when a
configured class is unavailable.

Validation and pricing are separate phases:

- `validation.rules` contains crude, products, and shared validation entries.
- `pricing.rules` contains crude and refined-products pricing entries.
- `CrudeDeskValidationRule` applies only to books beginning with `CRUDE`.
- `ProductsDeskValidationRule` applies to the `PRODUCTS-HOU` book.
- `BrentPremiumAdjustmentRule` applies to crude books.
- `RefinedProductsPricingAdjRule` applies to the products book.
- `MaxTenorRule` provides a shared tenor check.
- `CounterpartyLimitCheck` provides a shared credit check.

The properties file includes `DubaiSpreadRule`, which is not present in the
source tree. A warning is logged when that entry is loaded. The crude and
products desk rules retain similar validation structure while differing in
tenor limits, price handling, credit calculations, and desk wording.

## Data model

- `counterparty` — legal entities, codes, status, country, and credit limits.
- `instrument` — traded instrument definitions and delivery metadata.
- `deal` — captured trades, dates, quantities, prices, books, and status.
- `market_price` — dated instrument market prices.
- `instrument_vol` — annualized volatility by instrument.
- `settlement_config` — named settlement values such as interest rate.
- `invoice` — generated invoice amounts and text details.
- `fax_queue` — legacy confirmation queue records.
- `deal_audit` — deal actions, timestamps, and summary details.
- `eod_position` — book and instrument position snapshots.
- `eod_run_log` — batch execution timestamps and status.

## REST layer

The web layer exposes `/health`, `/positions`, `/positions/{book}`,
`/trades?counterparty=CODE`, `/risk/exposure`, and `/deals/{dealRef}`.

The service preserves different response formats:

- `/health` is plain text.
- `/positions` wraps rows under `positions` and uses camelCase fields.
- `/positions/{book}` returns a bare array and uses snake_case fields.
- `/trades` wraps rows under `status`, `count`, and `data`.
- `/risk/exposure` wraps rows under `result`.
- `/deals/{dealRef}` returns an object or an error object with HTTP 200.
- Unknown paths return plain-text HTTP 404.

`JsonUtil` uses `StringBuilder` and a small set of escaping rules. No external
JSON library is present.

## Batch

The EOD flow first finds the latest market price date. It submits one worker
task per book to a fixed thread pool. Each worker groups active deal rows by
instrument, calculates net quantity and market-value adjustment, and replaces
the current book rows for that date. A run-log row is written after workers
finish.

## Design characteristics

The repository exhibits three naming conventions from different teams and
eras. Core, deal, settlement, and web packages use one convention; the plugin
layer uses a second; the risk package uses a third.

The two large service classes combine orchestration, SQL, mapping, formatting,
file output, and operational logging. This keeps workflows close to their
historical entry points but gives them broad responsibilities.

Persistence is raw JDBC with SQL strings in service and DAO classes. Static
utilities are used for dates, number formatting, string padding, and context
access. Mutable JavaBeans and explicit loops are common.

Comments are sparse and include stale operational notes. The focused tests
concentrate on reference data, deal capture, date helpers, JSON rendering, and
the normal settlement path. Risk, rules, and batch packages have no dedicated
test classes.

There is no CI configuration in the repository. The deprecated fax path remains
wired through confirmation dispatch, although its configuration disables the
branch for normal operation.
