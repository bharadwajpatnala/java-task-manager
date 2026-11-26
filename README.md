# Task Manager Service

A dependency-free Java 17 task-management application. The project
provides an in-memory task service with CRUD operations, subtasks,
natural-language task parsing, task breakdown, idempotency,
search/filter/sort/pagination, and observer notifications.

> **Architecture note:** This project exposes an in-process Java
> `TaskApi` facade. It is not an HTTP server and does not start REST
> endpoints. `ApiResponse` carries an HTTP-style status code and a Java
> response body so the operations can be tested against the assignment
> contract.

## Features

-   Create, retrieve, list, update, and delete tasks.
-   Create subtasks and list a task's direct subtasks.
-   Delete a parent task and its descendants recursively.
-   Create a task from natural-language text.
-   Break down a top-level task into subtasks.
-   Use optional idempotency keys for selected create/breakdown
    operations.
-   Filter by completion and priority.
-   Search tasks and sort/paginate query results.
-   Notify event listeners after successful task mutations.
-   Use either offline rule-based strategies or an optional LLM
    provider.
-   Test LLM integration behavior with fake and faulty clients without
    making paid API calls.

## Requirements

-   JDK 17 or newer.
-   Windows PowerShell instructions are provided below.
-   No third-party Java libraries are required.
-   Internet access and a provider API key are required only for live
    LLM calls.

Check Java installation:

``` powershell
java -version
javac -version
```

Both commands should report JDK 17 or a newer version.

## Project structure

``` text
task-manager/
├── src/
│   └── app/
│       ├── api/          # ApiResponse and TaskApi facade
│       ├── event/        # Task events, listeners, audit and console listeners
│       ├── exception/    # API exception hierarchy
│       ├── llm/          # LlmClient, HttpLlmClient, FakeLlmClient, FaultyLlmClient
│       ├── model/        # Task, TaskRequest, TaskQuery, Priority, ParsedTask
│       ├── parser/       # Rule-based and LLM parser/breakdown strategies
│       ├── repository/   # Repository abstraction, in-memory repository, idempotency store
│       ├── service/      # TaskService, filters, sorting strategies
│       ├── util/         # MiniJson, DateUtil, ErrorResponseFactory
│       ├── Main.java
│       └── TestRunner.java
├── API_CONTRACT.md
├── README.md
├── build.bat
└── run.bat
```

## Compile the project (Windows PowerShell)

Open PowerShell in the `task-manager` project root (the directory
containing `src`).

The following commands work even when the project path contains spaces:

``` powershell
New-Item -ItemType Directory -Force -Path out | Out-Null
$sources = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $sources
```

If compilation succeeds, the class files are written to `out`.

## Run the application

``` powershell
java -cp out app.Main
```

`Main` creates a service configured for offline mode by default,
registers console/audit observers, creates one sample task, and prints
the result.

## Run the automated tests

``` powershell
java -cp out app.TestRunner
```

The test runner prints each passing assertion and finishes with an
`ALL TESTS PASSED` summary when successful. It exits with an error if an
assertion fails.

The current test runner covers: - Task creation and task field values. -
Idempotent retry and idempotency-key conflict. - Get/list task
operations. - Subtask creation and listing. - Priority filtering and
invalid-priority validation. - Request validation. - Task update and
recursive deletion. - Observer event notifications. - Search/query and
pagination. - Fake LLM task parsing and breakdown. - Offline
natural-language date parsing. - Mapping missing LLM configuration to
`503`. - Mapping invalid LLM output to `502`.

The test suite does not call a paid external LLM provider.

## Offline mode (recommended for local development and free testing)

Offline mode is the default. It does not require an API key or API
credits.

In PowerShell:

``` powershell
$env:USE_LLM = "false"
$env:LLM_API_KEY = ""
java -cp out app.Main
```

The default `TaskService` uses: - `RuleBasedTaskParser` for
natural-language task creation. - `RuleBasedBreakdownStrategy` for task
breakdown.

`FakeLlmClient` and `FaultyLlmClient` are test doubles used to test
successful and failing LLM integration paths without calling a real
provider.

**Important:** Fake LLM tests validate the application's integration and
error-handling logic. They do not demonstrate a live response from
Claude, OpenAI, or Gemini.

## Optional live LLM configuration

Live LLM calls are optional. They require a valid provider API key and
sufficient provider API credits. A subscription to a provider's chat
product does not necessarily include API usage.

The project supports these providers: - `anthropic` - `openai` -
`gemini`

Example PowerShell configuration for Anthropic:

``` powershell
$env:USE_LLM = "true"
$env:LLM_PROVIDER = "anthropic"
$env:LLM_API_KEY = "YOUR_API_KEY"
$env:LLM_MODEL = "YOUR_SUPPORTED_MODEL"
java -cp out app.Main
```

Example for OpenAI:

``` powershell
$env:USE_LLM = "true"
$env:LLM_PROVIDER = "openai"
$env:LLM_API_KEY = "YOUR_API_KEY"
$env:LLM_MODEL = "gpt-4o-mini"
java -cp out app.Main
```

Example for Gemini:

``` powershell
$env:USE_LLM = "true"
$env:LLM_PROVIDER = "gemini"
$env:LLM_API_KEY = "YOUR_API_KEY"
$env:LLM_MODEL = "gemini-2.0-flash"
java -cp out app.Main
```

`LLM_MODEL` is optional; when omitted, the client uses the provider
default configured in `HttpLlmClient`. Confirm that the selected model
is currently available to your provider account.

The client reads `LLM_API_KEY`, not `ANTHROPIC_API_KEY`.

### API key security

-   Never hard-code a key in Java source code.
-   Never commit a key to Git.
-   Never paste a key into README files, chat messages, or screenshots.
-   Prefer setting the key in the current terminal session or a secure
    secret manager.
-   If a key is exposed, revoke it in the provider console and create a
    replacement.
-   Do not enable live mode unless you intend to make provider API
    calls.

## API usage example

`TaskApi` is constructed around a `TaskService`. Example:

``` java
TaskService service = new TaskService(
    InMemoryTaskRepository.getInstance()
);
TaskApi api = new TaskApi(service);

TaskRequest request = new TaskRequest(
    "Pay rent",
    "Pay the monthly apartment rent",
    "false",
    "2026-12-01",
    "HIGH"
);

ApiResponse response = api.createTask(request, "rent-create-001");

System.out.println("Status: " + response.getStatus());
System.out.println("Body: " + response.getBody());
```

See [`API_CONTRACT.md`](API_CONTRACT.md) for the Java signatures,
parameters, status codes, and request/response examples for all 11
operations.

## Supported API operations

1.  `createTask(request, idempotencyKey)`
2.  `getTask(id)`
3.  `listTasks()`
4.  `updateTask(id, request)`
5.  `deleteTask(id)`
6.  `createTaskFromText(text, today, idempotencyKey)`
7.  `createSubtask(parentId, request, idempotencyKey)`
8.  `listSubtasks(parentId)`
9.  `breakDownTask(id, idempotencyKey)`
10. `listByPriority(priority)`
11. `queryTasks(query)`

## Validation and business rules

-   `title` and `description` must not be blank.
-   `completed` must be exactly the string `"true"` or `"false"`.
-   `dueDate` is optional; if supplied, it must be a real date in
    `YYYY-MM-DD` format.
-   `priority` is optional and defaults to `MEDIUM`; accepted values are
    `LOW`, `MEDIUM`, and `HIGH`.
-   A subtask's parent must exist.
-   A subtask cannot itself have subtasks.
-   A subtask cannot be broken down.
-   Deleting a parent deletes its descendants recursively.
-   Breakdown output is validated before any generated subtasks are
    saved.
-   Query pagination requires `page >= 1` and `1 <= pageSize <= 1000`.
-   Query sorting supports `createdAt` and `dueDate`.

## Design patterns used

  -----------------------------------------------------------------------
Pattern                             Implementation
  ----------------------------------- -----------------------------------
Builder                             `Task.Builder`, `TaskQuery.Builder`

Singleton                           `InMemoryTaskRepository`,
`IdempotencyStore`

Factory                             `ParserFactory`,
`ErrorResponseFactory`

Strategy                            `TaskParser`, `BreakdownStrategy`,
`TaskFilter`, `SortStrategy`

Observer                            `TaskEventListener`,
`ConsoleLogListener`,
`AuditTrailListener`
  -----------------------------------------------------------------------

## Storage and limitations

-   Tasks are stored in memory. They are lost when the process exits.
-   The idempotency store is also in memory and is scoped to the running
    application instance.
-   The project is a Java service facade, not a deployed REST/HTTP
    server.
-   Rule-based parsing and breakdown are intentionally simple offline
    fallbacks; they are not equivalent to a production LLM.
-   Live LLM calls depend on provider credentials, available API
    credits, network connectivity, and model availability.


## AI prompts and rationale

The following are the exact prompt templates currently used by the source code.

### 1. Natural-language task creation

**Source file:** `src/app/parser/LlmTaskParser.java`

```text
Today is [today]. Convert the user's request into exactly one JSON object with string fields title, description, dueDate, priority. Use null for unknown dueDate. priority must be LOW, MEDIUM, or HIGH. Return only JSON, no markdown. User request: [user input]
```

**Why this prompt is structured this way:**

- The current date is included so relative dates such as "tomorrow" can be interpreted.
- The model is asked for one JSON object with known fields so the application can parse the result.
- Priority is restricted to the application's supported values: `LOW`, `MEDIUM`, and `HIGH`.
- An unknown due date is represented as `null`.
- Asking for JSON only, without Markdown, reduces formatting that could interfere with parsing.

### 2. Task breakdown

**Source file:** `src/app/parser/LlmBreakdownStrategy.java`

```text
Break this task into 2 to 6 actionable subtasks. Return only a JSON array. Each item must have string fields title, description, priority (LOW, MEDIUM, HIGH) and dueDate (null if unknown). Task: [task description]
```

**Why this prompt is structured this way:**

- The requested range of 2 to 6 keeps the breakdown to a manageable number of steps.
- "Actionable subtasks" encourages steps that can be completed independently.
- A JSON array provides a predictable structure for multiple subtasks.
- Each item uses the same task fields and supported priority values as the rest of the application.
- An unknown due date is represented as `null`.

The application parses the response and validates generated task fields before saving the breakdown. The prompt itself does not guarantee that the model will always return valid data; invalid output is handled by the application's error handling.

## AI-generated test review

The project does not retain a separate record of the original AI-generated test cases or their initial gaps. Therefore, this README does not claim specific omissions from those original tests.

The current `TestRunner.java` includes coverage for the following scenarios:

- Creating tasks and checking task fields.
- Retrying a request with the same idempotency key.
- Reusing an idempotency key with different request data.
- Retrieving and listing tasks.
- Creating and listing subtasks.
- Rejecting invalid task data and invalid priority values.
- Updating tasks and recursively deleting a parent task's descendants.
- Verifying observer notifications.
- Searching, filtering, sorting, and pagination.
- Parsing tasks and breaking down tasks with a fake LLM client.
- Offline natural-language date parsing.
- Handling missing LLM configuration with a `503` response.
- Handling invalid LLM output with a `502` response.

These are the scenarios verified by the current test runner. The test suite uses fake/faulty clients and does not make paid calls to a live LLM provider.

## API operation examples and printed responses

`TaskApi` is an in-process Java facade. It returns an `ApiResponse`; it does not expose HTTP routes. The examples below show the Java calls and the status/body values returned by that facade. IDs and timestamps are generated at runtime, so the values shown are illustrative.

The full signatures, parameters, error conditions, and JSON-style examples for all 11 operations are documented in [`API_CONTRACT.md`](API_CONTRACT.md).

| Operation | Example Java call | Expected status on success |
|---|---|---:|
| Create task | `api.createTask(request, "create-001")` | 201 |
| Get task | `api.getTask(taskId)` | 200 |
| List tasks | `api.listTasks()` | 200 |
| Update task | `api.updateTask(taskId, updatedRequest)` | 200 |
| Delete task | `api.deleteTask(taskId)` | 200 |
| Create from text | `api.createTaskFromText("Pay rent tomorrow", "2026-11-26", "text-001")` | 201 |
| Create subtask | `api.createSubtask(parentId, subtaskRequest, "sub-001")` | 201 |
| List subtasks | `api.listSubtasks(parentId)` | 200 |
| Break down task | `api.breakDownTask(parentId, "breakdown-001")` | 201 |
| List by priority | `api.listByPriority("HIGH")` | 200 |
| Query tasks | `api.queryTasks(query)` | 200 |

A successful call can be inspected as follows:

```java
ApiResponse response = api.createTask(request, "create-001");
System.out.println("Status: " + response.getStatus());
System.out.println("Body: " + response.getBody());
```

Illustrative console output:

```text
Status: 201
Body: app.model.Task@<runtime-object-reference>
```

`Task` does not currently override `toString()`, so Java's default object representation is printed by this example. The actual runtime reference will differ between runs. For a clearer demonstration, print individual getters such as `getId()`, `getTitle()`, `getPriority()`, and `getDueDate()` as shown in `Main.java`.

A validation failure is returned as an error body rather than a successful task:

```text
Status: 400
Body: {error={code=VALIDATION_ERROR, message=Validation failed, details=[...]}}
```

For the exact error response fields and the documented error codes/statuses, see Sections 2.3 and 4 of `API_CONTRACT.md`.
