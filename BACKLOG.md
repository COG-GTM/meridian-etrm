# Open tickets — Trading Desk IT

## Table of contents

1. [MET-1042 — Accrued interest on backdated deals is one day short](#met-1042--accrued-interest-on-backdated-deals-is-one-day-short)
2. [MET-1043 — Add unit tests for risk and rules](#met-1043--add-unit-tests-for-risk-and-rules)
3. [MET-1044 — Consolidate duplicated desk validation rules](#met-1044--consolidate-duplicated-desk-validation-rules)
4. [MET-1045 — Add LNG as a commodity end-to-end](#met-1045--add-lng-as-a-commodity-end-to-end)
5. [MET-1046 — Document the EOD batch job](#met-1046--document-the-eod-batch-job)
6. [MET-1047 — EOD reruns produce inconsistent snapshots](#met-1047--eod-reruns-produce-inconsistent-snapshots)
7. [MET-1048 — Add CI pipeline](#met-1048--add-ci-pipeline)
8. [MET-1049 — Remove fax confirmation export](#met-1049--remove-fax-confirmation-export)
9. [MET-1050 — Add commodity exposure endpoint](#met-1050--add-commodity-exposure-endpoint)
10. [MET-1051 — Settlement exceptions are not visible](#met-1051--settlement-exceptions-are-not-visible)

## MET-1042 — Accrued interest on backdated deals is one day short

- **Type:** Bug
- **Priority:** High
- **Reporter:** R. Kaur, Settlements Ops

Backdated invoices do not reconcile with the operations spreadsheet. Deal
`MET-2024-00007` is one accrued day short when the value date and trade date
are compared with the settlement specification. The specification says the
accrual period includes the value date through the trade date.

### Acceptance criteria

- The accrued-day count includes the value date.
- `MET-2024-00007` reconciles with the operations spreadsheet.
- A unit test covers a backdated deal across ordinary calendar dates.
- Invoice output continues to show the accrued-interest line.

### Pointers

- `com.meridianenergy.etrm.settlement.SettlementEngine`
- `com.meridianenergy.etrm.util.DateUtil`
- `com.meridianenergy.etrm.settlement.InvoiceGenerator`

## MET-1043 — Add unit tests for risk and rules

- **Type:** Test
- **Priority:** Medium
- **Reporter:** S. Malik, Risk Controls

The current test suite has approximately zero coverage in `meridian.risk` and
`com.met.trading.rules`. Changes in market-price calculation and desk
validation are currently checked by manual command-line runs.

### Acceptance criteria

- Add tests for position aggregation and MTM.
- Add tests for VaR-lite inputs and output rounding.
- Add tests for crude and products validation gates.
- Add tests for missing configured rule classes.
- Keep all existing tests green.

### Pointers

- `meridian.risk.PositionKeeper`
- `meridian.risk.MtmValuator`
- `meridian.risk.VarLiteCalculator`
- `meridian.risk.RiskReportService`
- `com.met.trading.rules.RuleEngine`

## MET-1044 — Consolidate duplicated desk validation rules

- **Type:** Refactor
- **Priority:** Medium
- **Reporter:** L. Chen, Trading Technology

Crude and products validation contain repeated checks for quantity, price,
delivery tenor, counterparties, and credit limits. The duplicated sections
have drifted in wording and calculation details, making policy changes
difficult to review.

### Acceptance criteria

- Extract shared validation behavior into a tested component.
- Preserve crude swap price handling.
- Preserve the products credit haircut.
- Preserve desk-specific tenor limits and messages.
- Remove duplicated code without changing rule selection by book.

### Pointers

- `com.met.trading.rules.crude.CrudeDeskValidationRule`
- `com.met.trading.rules.products.ProductsDeskValidationRule`
- `com.met.trading.rules.common.CounterpartyLimitCheck`
- `com.met.trading.rules.common.MaxTenorRule`

## MET-1045 — Add LNG as a commodity end-to-end

- **Type:** Feature
- **Priority:** Medium
- **Reporter:** M. Ortiz, Products Trading

The desks need LNG support for new forward transactions. LNG reference data,
capture validation, valuation, and reporting should work through the same
workflow as the existing commodities. The trading unit is MMBtu.

### Acceptance criteria

- Add LNG instruments with unit `MMBtu`.
- Capture and persist LNG deals.
- Include LNG in position and MTM calculations.
- Include LNG in risk report commodity sections.
- Add market price and volatility seed rows.
- Add focused tests for reference data and capture.

### Pointers

- `com.meridianenergy.etrm.refdata.Instrument`
- `com.meridianenergy.etrm.refdata.RefDataDAO`
- `com.meridianenergy.etrm.deal.DealCaptureService`
- `meridian.risk.MtmValuator`
- `meridian.risk.RiskReportService`
- `src/main/resources/sql/seed.sql`

## MET-1046 — Document the EOD batch job

- **Type:** Docs
- **Priority:** Low
- **Reporter:** J. Rowan, Operations Support

Nobody knows what the EOD batch does anymore. The on-call team needs a short
operational description covering its inputs, business date selection, book
processing, output tables, and rerun procedure.

### Acceptance criteria

- Document startup and database prerequisites.
- Document the market price date used for the run.
- Document book and instrument aggregation.
- Document output rows and run-log status.
- Document how operators inspect a completed run.

### Pointers

- `com.meridianenergy.etrm.batch.EodBatchJob`
- `com.meridianenergy.etrm.batch.BatchRunner`
- `eod_position`
- `eod_run_log`

## MET-1047 — EOD reruns produce inconsistent snapshots

- **Type:** Bug
- **Priority:** High
- **Reporter:** A. Mercer, Operations Support

The EOD job occasionally produces duplicate or missing snapshot IDs and
different book totals when it is rerun. We saw this twice last month after an
operator restarted the batch during a busy processing window.

### Acceptance criteria

- Repeated runs for one business date produce a consistent set of rows.
- Snapshot IDs are unique and complete for a run.
- Book totals match the aggregated deal and market-price inputs.
- Reruns do not retain stale rows from the previous attempt.
- Add a regression test for two consecutive EOD runs.

### Pointers

- `com.meridianenergy.etrm.batch.EodBatchJob`
- `eod_position`
- `eod_run_log`
- `market_price`

## MET-1048 — Add CI pipeline

- **Type:** Infra
- **Priority:** Medium
- **Reporter:** R. Vale, Engineering Enablement

There is no automated build or test pipeline for changes to the trading
application. Reviewers must run Maven commands locally before merging.

### Acceptance criteria

- Run the Maven test suite on every change.
- Run the packaged build after tests pass.
- Publish test results as a build artifact.
- Use the supported JDK and Maven versions.
- Keep credentials and generated database files out of build artifacts.

### Pointers

- `pom.xml`
- `src/main/java`
- `src/test/java`
- `.gitignore`

## MET-1049 — Remove fax confirmation export

- **Type:** Refactor
- **Priority:** Low
- **Reporter:** D. Singh, Settlements Ops

The fax gateway was decommissioned in 2021. The confirmation code still
checks the fax configuration and retains gateway dependencies, which creates
confusion during settlement support.

### Acceptance criteria

- Remove the fax exporter and gateway client.
- Remove the fax branch from confirmation dispatch.
- Remove obsolete fax configuration and queue dependencies where safe.
- Preserve email confirmation behavior.
- Verify that invoice and deal capture workflows remain available.

### Pointers

- `com.meridianenergy.etrm.settlement.ConfirmationDispatcher`
- `com.meridianenergy.etrm.settlement.FaxConfirmationExporter`
- `com.meridianenergy.etrm.settlement.FaxGatewayClient`
- `src/main/resources/fax.properties`
- `fax_queue`

## MET-1050 — Add commodity exposure endpoint

- **Type:** Feature
- **Priority:** Low
- **Reporter:** S. Malik, Risk Controls

Risk users need commodity-level exposure without downloading the complete text
report. Add `GET /risk/exposure/commodity` and make its response match the
existing conventions of the other risk endpoint.

### Acceptance criteria

- Add `GET /risk/exposure/commodity`.
- Return commodity name and exposure rows.
- Include an as-of date.
- Preserve the existing `/risk/exposure` response.
- Add an endpoint test or repeatable curl verification.

### Pointers

- `com.meridianenergy.etrm.web.RestServer`
- `meridian.risk.RiskReportService`
- `instrument`
- `deal`

## MET-1051 — Settlement exceptions are not visible

- **Type:** Bug
- **Priority:** Medium
- **Reporter:** R. Kaur, Settlements Ops

Settlement failures can result in an invoice not being written, but the
command returns without telling the operator what happened. Missing invoice
files are discovered later during reconciliation.

### Acceptance criteria

- Log database and file-writing failures with the deal reference.
- Return a clear failure status to the command-line caller.
- Do not create partial invoice records.
- Add tests for database and file failure paths.
- Preserve successful invoice text and accrued-interest output.

### Pointers

- `com.meridianenergy.etrm.settlement.InvoiceGenerator`
- `com.meridianenergy.etrm.settlement.SettlementEngine`
- `invoice`
- `out/`
