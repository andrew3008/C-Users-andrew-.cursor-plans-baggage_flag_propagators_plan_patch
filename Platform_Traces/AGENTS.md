# Agent instructions (Codex, Cursor, and other coding agents)

Repository-level contract for AI-generated changes. Local IntelliJ IDEA UI settings alone are not sufficient — agents must read and follow these instructions plus [`.editorconfig`](.editorconfig).

Human-facing IDEA setup: [`../Platform_Traces_Docs/development/idea-code-style.md`](../Platform_Traces_Docs/development/idea-code-style.md).

Cursor-specific rule: [`.cursor/rules/java-imports.mdc`](.cursor/rules/java-imports.mdc).

---

## Java imports and formatting

For all Java changes:

- Follow the repository [`.editorconfig`](.editorconfig).
- Use explicit single-class imports only.
- Do not use wildcard imports.
- Do not use static wildcard imports.
- Keep static imports in a separate import block.
- Do not reorder imports manually according to your own style.
- Do not introduce import-only churn in unrelated files.
- When adding or removing Java references, update imports in the same file.
- Prefer the IDE / formatter / project style over generated import order.
- Do not optimize imports in files you did not otherwise modify.

Expected Java import layout:

```java
import static ...

import java...
import javax...
import jakarta...

import org...

import com...

import space...

import ...
```

Do not introduce:

```java
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
```

For tests, explicit static imports for AssertJ, Mockito, and JUnit are allowed (single member only, no wildcards).

For platform tracing code, avoid ambiguous imports without qualification — prefer fully qualified names or explicit single-class imports when several candidates exist:

- `Context`, `Span`, `Scope`, `Attributes`
- `TraceContext`, `ValidationResult`, `ControlResult`, `RuntimePolicy`
- `Date`, `Duration`, `Configuration`, `Status`

---

## Bulk file edits from PowerShell

Prefer the editor's file tools for source edits. When a scripted bulk edit is unavoidable, the script must be fail-fast and path-safe:

- Set `$ErrorActionPreference = 'Stop'` at the top of the script. A failed read must abort the script, never fall through to a write.
- Never pass relative paths to .NET APIs (`[System.IO.File]::ReadAllText`, `WriteAllText`, `[System.IO.Path]`). `Set-Location` / `cd` changes only `$PWD`; `[Environment]::CurrentDirectory` stays at the process start directory, so a relative path resolves against a different directory than PowerShell's. Resolve first: `$f = (Resolve-Path $name).Path`.
- Write only inside the same `try` block as the successful read, and only if the content actually changed.
- After the script, run `git status --short` and confirm there are no untracked files outside the intended directories before running any build.

Reference shape:

```powershell
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($name in (rg -l 'pattern' $absoluteDir)) {
    $f = (Resolve-Path $name).Path
    $before = [System.IO.File]::ReadAllText($f)
    $after = $before -replace 'pattern', 'replacement'
    if ($after -ne $before) { [System.IO.File]::WriteAllText($f, $after, $utf8) }
}
```

Incident that motivated this section: a loop that used `cd` plus relative paths in `ReadAllText` threw on every read, left `$t` as `$null`, and `WriteAllText` then created 13 empty `*.java` files in the repository root.

---

## Verification after Java edits

Run the narrowest relevant compile task for the affected module:

```powershell
.\gradlew.bat :<affected-module>:compileJava --no-daemon
.\gradlew.bat :<affected-module>:compileTestJava --no-daemon
```

If a task is too expensive, at minimum run the smallest compile task for the affected module.

After agent work, also check:

```powershell
git diff --stat
git diff --check
```

Optional wildcard import scan (expect **0** matches):

```powershell
Select-String -Path (Get-ChildItem -Recurse -Filter *.java) `
  -Pattern "^\s*import\s+(static\s+)?[a-zA-Z0-9_.]+\.\*;"
```

---

## Prompt block (copy into implementation prompts)

```text
Java import policy:
- Follow repository `.editorconfig`.
- Use explicit imports only.
- Do not use wildcard imports or static wildcard imports.
- Keep static imports separate.
- Do not reorder imports in unrelated files.
- Do not create import-only diffs.
- After editing Java files, run the affected module compile task.
```

---

## Why repository instructions matter for agents

`.editorconfig` drives IntelliJ IDEA on **Ctrl+Alt+O**, but agents often generate patches without running the IDEA formatter. Without explicit rules, agents may:

- invent import order manually;
- create import-only diffs;
- use wildcard imports;
- reorder imports in unrelated files.

**Ctrl+Alt+O** fixes imports for humans in IDEA. **AGENTS.md** and **`.cursor/rules`** fix the same problem for Codex/Cursor agents. **Gradle compile tasks** verify that imports are actually correct.
