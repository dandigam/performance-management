---
name: test-change-policy
description: Apply this project's preference against unsolicited test changes when implementing features, fixing bugs, or considering adding or updating test cases.
---

# Test change policy

- Do not add or update test cases unless the user explicitly requests test changes.
- A request to implement or fix application behavior does not by itself authorize adding or updating tests.
- Existing tests may be run to validate changes. Do not delete, disable, or weaken them to obtain a passing result.
- If an existing test needs an update because of an intentional application change, report that limitation without modifying the test.
- When the user explicitly requests test changes, keep them within the requested scope.
