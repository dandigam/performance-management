---
name: test-change-policy
description: Require impact analysis and user confirmation before project migrations, updates, fixes, or new functionality, and prevent unsolicited test changes.
---

# Project change and test policy

## Impact analysis and confirmation before implementation

- Before implementing any migration, update, bug fix, or new functionality, identify all affected areas first. Read-only investigation may proceed without confirmation.
- Trace the affected flow end to end, including callers, dependent services, automatic side effects, database tables and migrations, existing records and history, authorization, APIs, imports/exports, UI contracts, configuration, and documentation where relevant. Search for legacy paths that could still invoke the old behavior.
- Present the findings, proposed changes, compatibility and data implications, and planned verification to the user. State any uncertainties or areas that could not be checked.
- Obtain explicit user confirmation of that impact assessment and implementation scope before editing application code, schemas, configuration, or documentation. The initial request to make a change does not replace this confirmation; do not treat silence as approval.
- If investigation during implementation reveals additional material impact outside the confirmed scope, explain it and obtain confirmation before implementing that expanded scope.
- An explicit request to edit this policy authorizes that policy edit directly; do not require a recursive confirmation for the policy edit itself.

## Test changes

- Do not add or update test cases unless the user explicitly requests test changes.
- A request to implement or fix application behavior does not by itself authorize adding or updating tests.
- Existing tests may be run to validate changes. Do not delete, disable, or weaken them to obtain a passing result.
- If an existing test needs an update because of an intentional application change, report that limitation without modifying the test.
- When the user explicitly requests test changes, keep them within the requested scope.
