# Task Manager Service --- API Contract

## 1. Overview

This document describes the operations exposed by the Java `TaskApi`
facade.

**Important:** `TaskApi` is an in-process Java API facade. The project
does not start an HTTP server or expose network routes. Each method
returns an `ApiResponse` containing: - `status`: an HTTP-style numeric
status code. - `body`: a Java object or an error map.

The JSON examples below illustrate the logical request and response
shapes. They are documentation examples, not actual HTTP endpoints.

## 2. Common conventions

### 2.1 Task request fields

`TaskRequest` is constructed with the following fields:

  ------------------------------------------------------------------------
Field            Type                          Required Rules
  ---------------- ---------------- --------------------- ----------------
`title`          String                             Yes Must not be
null, empty, or
blank

`description`    String                             Yes Must not be
null, empty, or
blank

`completed`      String                             Yes Must be exactly
`"true"` or
`"false"`

`dueDate`        String                              No If supplied,
must be a real
calendar date in
`YYYY-MM-DD`
format

`priority`       String                              No `LOW`, `MEDIUM`,
or `HIGH`;
defaults to
`MEDIUM`
  ------------------------------------------------------------------------

Tasks created from natural-language parsing or breakdown are initially
marked `completed=false`.

### 2.2 Task representation

A task returned by the service contains these fields:

``` json
{
  "id": 1,
  "title": "Pay rent",
  "description": "Monthly rent",
  "completed": false,
  "dueDate": "2026-12-01",
  "priority": "HIGH",
  "parentId": null,
  "createdAt": 1790000000000
}
```

`dueDate` and `parentId` may be `null`. `createdAt` is a timestamp in
milliseconds. The exact generated ID and timestamp depend on the running
application.

### 2.3 Error response shape

All API-level failures are returned using this structure:

``` json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "title",
        "issue": "must not be empty or blank"
      }
    ]
  }
}
```

`details` is included for validation errors. Other error types return
`code` and `message`.

### 2.4 Idempotency

The optional `idempotencyKey` is supported by: - `createTask` -
`createTaskFromText` - `createSubtask` - `breakDownTask`

When a nonblank key is reused for the same operation and same request,
the original successful response is returned. Reusing the key for a
different request returns `422 CONFLICT`. The idempotency store is
in-memory and scoped to the running application instance.

## 3. Operations

### 3.1 Create a task --- `createTask`

**Java signature**

``` java
ApiResponse createTask(TaskRequest request, String idempotencyKey)
```

**Purpose:** Creates a top-level task from structured task fields.

**Parameters** - `request`: task fields described in Section 2.1. -
`idempotencyKey`: optional key used to make retries safe.

**Success status:** `201 Created`

**Possible errors** - `400 VALIDATION_ERROR` --- one or more task fields
are invalid. - `422 CONFLICT` --- an idempotency key was reused with a
different request. - `500 INTERNAL_ERROR` --- unexpected internal error.

**Request example**

``` json
{
  "title": "Pay rent",
  "description": "Pay the monthly apartment rent",
  "completed": "false",
  "dueDate": "2026-12-01",
  "priority": "HIGH"
}
```

**Success response example**

``` json
{
  "status": 201,
  "body": {
    "id": 1,
    "title": "Pay rent",
    "description": "Pay the monthly apartment rent",
    "completed": false,
    "dueDate": "2026-12-01",
    "priority": "HIGH",
    "parentId": null,
    "createdAt": 1790000000000
  }
}
```

**Validation error example**

``` json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "title",
        "issue": "must not be empty or blank"
      }
    ]
  }
}
```

### 3.2 Get a task --- `getTask`

**Java signature**

``` java
ApiResponse getTask(int id)
```

**Purpose:** Retrieves one task by its ID.

**Parameters** - `id`: task ID.

**Success status:** `200 OK`

**Possible errors** - `404 NOT_FOUND` --- no task exists with the
supplied ID. - `500 INTERNAL_ERROR` --- unexpected internal error.

**Request example**

``` text
getTask(1)
```

**Success response example**

``` json
{
  "status": 200,
  "body": {
    "id": 1,
    "title": "Pay rent",
    "description": "Pay the monthly apartment rent",
    "completed": false,
    "dueDate": "2026-12-01",
    "priority": "HIGH",
    "parentId": null,
    "createdAt": 1790000000000
  }
}
```

**Not-found response example**

``` json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Task 999 not found"
  }
}
```

### 3.3 List tasks --- `listTasks`

**Java signature**

``` java
ApiResponse listTasks()
```

**Purpose:** Returns all tasks in the repository, including subtasks.

**Parameters:** None.

**Success status:** `200 OK`

**Possible errors** - `500 INTERNAL_ERROR` --- unexpected internal
error.

**Request example**

``` text
listTasks()
```

**Success response example**

``` json
{
  "status": 200,
  "body": [
    {
      "id": 1,
      "title": "Pay rent",
      "description": "Pay the monthly apartment rent",
      "completed": false,
      "dueDate": "2026-12-01",
      "priority": "HIGH",
      "parentId": null,
      "createdAt": 1790000000000
    }
  ]
}
```

### 3.4 Update a task --- `updateTask`

**Java signature**

``` java
ApiResponse updateTask(int id, TaskRequest request)
```

**Purpose:** Replaces the editable fields of an existing task. The task
ID, original creation timestamp, and parent relationship are preserved.

**Parameters** - `id`: ID of the task to update. - `request`:
replacement task fields, using the rules in Section 2.1.

**Success status:** `200 OK`

**Possible errors** - `400 VALIDATION_ERROR` --- request fields are
invalid. - `404 NOT_FOUND` --- task does not exist. -
`500 INTERNAL_ERROR` --- unexpected internal error.

**Request example**

``` json
{
  "id": 1,
  "request": {
    "title": "Pay rent - updated",
    "description": "Pay rent after salary credit",
    "completed": "true",
    "dueDate": "2026-12-02",
    "priority": "MEDIUM"
  }
}
```

**Success response example**

``` json
{
  "status": 200,
  "body": {
    "id": 1,
    "title": "Pay rent - updated",
    "description": "Pay rent after salary credit",
    "completed": true,
    "dueDate": "2026-12-02",
    "priority": "MEDIUM",
    "parentId": null,
    "createdAt": 1790000000000
  }
}
```

**Not-found response example**

``` json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Task 999 not found"
  }
}
```

### 3.5 Delete a task --- `deleteTask`

**Java signature**

``` java
ApiResponse deleteTask(int id)
```

**Purpose:** Deletes a task. Any descendant subtasks are deleted
recursively before the parent task, preventing orphaned subtasks.

**Parameters** - `id`: ID of the task to delete.

**Success status:** `200 OK`

**Possible errors** - `404 NOT_FOUND` --- task does not exist. -
`500 INTERNAL_ERROR` --- unexpected internal error.

**Request example**

``` text
deleteTask(1)
```

**Success response example**

``` json
{
  "status": 200,
  "body": {
    "message": "Task deleted successfully"
  }
}
```

### 3.6 Create a task from natural language --- `createTaskFromText`

**Java signature**

``` java
ApiResponse createTaskFromText(
    String text,
    String today,
    String idempotencyKey
)
```

**Purpose:** Parses a natural-language instruction into structured task
fields and creates a task. The configured parser may use the offline
rule-based parser or an LLM-backed parser.

**Parameters** - `text`: nonblank natural-language task description. -
`today`: optional reference date in real `YYYY-MM-DD` format. If `null`,
the current local date of the JVM is used. - `idempotencyKey`: optional
idempotency key.

**Success status:** `201 Created`

**Possible errors** - `400 VALIDATION_ERROR` --- blank text or invalid
reference date. - `422 CONFLICT` --- idempotency key reused with a
different request. - `502 BAD_GATEWAY` --- parsing provider failed or
returned unusable output. - `503 SERVICE_UNAVAILABLE` --- LLM
key/configuration is unavailable. - `500 INTERNAL_ERROR` --- unexpected
internal error.

**Request example**

``` json
{
  "text": "Remind me to pay rent tomorrow with high priority",
  "today": "2026-11-26"
}
```

**Success response example**

``` json
{
  "status": 201,
  "body": {
    "id": 2,
    "title": "Pay rent",
    "description": "Pay rent",
    "completed": false,
    "dueDate": "2026-11-27",
    "priority": "HIGH",
    "parentId": null,
    "createdAt": 1790000000000
  }
}
```

The exact parsed title, description, due date, and priority depend on
the selected parser and input.

**LLM configuration error example**

``` json
{
  "error": {
    "code": "SERVICE_UNAVAILABLE",
    "message": "LLM_API_KEY is not configured"
  }
}
```

### 3.7 Create a subtask --- `createSubtask`

**Java signature**

``` java
ApiResponse createSubtask(
    int parentId,
    TaskRequest request,
    String idempotencyKey
)
```

**Purpose:** Creates a child task under an existing top-level task. A
subtask cannot itself be a parent.

**Parameters** - `parentId`: ID of an existing top-level parent task. -
`request`: task fields described in Section 2.1. - `idempotencyKey`:
optional idempotency key.

**Success status:** `201 Created`

**Possible errors** - `400 VALIDATION_ERROR` --- task fields are
invalid. - `404 NOT_FOUND` --- parent task does not exist. -
`422 CONFLICT` --- parent is already a subtask, or idempotency key
conflicts. - `500 INTERNAL_ERROR` --- unexpected internal error.

**Request example**

``` json
{
  "parentId": 1,
  "request": {
    "title": "Collect rent receipt",
    "description": "Download the receipt after payment",
    "completed": "false",
    "dueDate": "2026-12-02",
    "priority": "MEDIUM"
  }
}
```

**Success response example**

``` json
{
  "status": 201,
  "body": {
    "id": 3,
    "title": "Collect rent receipt",
    "description": "Download the receipt after payment",
    "completed": false,
    "dueDate": "2026-12-02",
    "priority": "MEDIUM",
    "parentId": 1,
    "createdAt": 1790000000000
  }
}
```

### 3.8 List subtasks --- `listSubtasks`

**Java signature**

``` java
ApiResponse listSubtasks(int parentId)
```

**Purpose:** Returns the direct subtasks belonging to the specified
parent task.

**Parameters** - `parentId`: ID of an existing task.

**Success status:** `200 OK`

**Possible errors** - `404 NOT_FOUND` --- parent task does not exist. -
`500 INTERNAL_ERROR` --- unexpected internal error.

**Request example**

``` text
listSubtasks(1)
```

**Success response example**

``` json
{
  "status": 200,
  "body": [
    {
      "id": 3,
      "title": "Collect rent receipt",
      "description": "Download the receipt after payment",
      "completed": false,
      "dueDate": "2026-12-02",
      "priority": "MEDIUM",
      "parentId": 1,
      "createdAt": 1790000000000
    }
  ]
}
```

### 3.9 Break down a task --- `breakDownTask`

**Java signature**

``` java
ApiResponse breakDownTask(int id, String idempotencyKey)
```

**Purpose:** Generates subtasks from the title and description of an
existing top-level task. All generated items are validated before any
are saved.

**Parameters** - `id`: ID of the parent task to break down. -
`idempotencyKey`: optional idempotency key.

**Success status:** `201 Created`

**Possible errors** - `400 VALIDATION_ERROR` --- generated breakdown is
empty or contains invalid items. - `404 NOT_FOUND` --- task does not
exist. - `422 CONFLICT` --- task is itself a subtask, or idempotency key
conflicts. - `502 BAD_GATEWAY` --- LLM breakdown provider failed or
returned unusable output. - `503 SERVICE_UNAVAILABLE` --- LLM
key/configuration is unavailable. - `500 INTERNAL_ERROR` --- unexpected
internal error.

**Request example**

``` json
{
  "id": 1
}
```

**Success response example**

``` json
{
  "status": 201,
  "body": [
    {
      "id": 4,
      "title": "Review rent amount",
      "description": "Confirm the amount due",
      "completed": false,
      "dueDate": null,
      "priority": "MEDIUM",
      "parentId": 1,
      "createdAt": 1790000000000
    },
    {
      "id": 5,
      "title": "Make rent payment",
      "description": "Complete the rent transfer",
      "completed": false,
      "dueDate": null,
      "priority": "MEDIUM",
      "parentId": 1,
      "createdAt": 1790000000001
    }
  ]
}
```

The generated subtasks depend on the configured breakdown strategy. The
offline strategy is a simple fallback; an LLM-backed strategy requires
provider credentials and available API usage.

### 3.10 List tasks by priority --- `listByPriority`

**Java signature**

``` java
ApiResponse listByPriority(String priority)
```

**Purpose:** Returns tasks whose priority exactly matches the supplied
value.

**Parameters** - `priority`: `LOW`, `MEDIUM`, or `HIGH`.

**Success status:** `200 OK`

**Possible errors** - `400 VALIDATION_ERROR` --- priority is missing or
not one of the supported values. - `500 INTERNAL_ERROR` --- unexpected
internal error.

**Request example**

``` text
listByPriority("HIGH")
```

**Success response example**

``` json
{
  "status": 200,
  "body": [
    {
      "id": 1,
      "title": "Pay rent",
      "description": "Pay the monthly apartment rent",
      "completed": false,
      "dueDate": "2026-12-01",
      "priority": "HIGH",
      "parentId": null,
      "createdAt": 1790000000000
    }
  ]
}
```

### 3.11 Query tasks --- `queryTasks`

**Java signature**

``` java
ApiResponse queryTasks(TaskQuery query)
```

**Purpose:** Searches, filters, sorts, and paginates tasks.

**Parameters**

`TaskQuery` is created using `TaskQuery.Builder`.

  -----------------------------------------------------------------------
Field             Type              Default           Rules
  ----------------- ----------------- ----------------- -----------------
`search`          String            `null`            Optional search
text

`completed`       Boolean           `null`            Optional
completion filter

`priority`        `Priority`        `null`            Optional `LOW`,
`MEDIUM`, or
`HIGH` filter

`sortBy`          String            `createdAt`       Must be
`createdAt` or
`dueDate`

`page`            int               `1`               Must be at least
1

`pageSize`        int               `20`              Must be between 1
and 1000
  -----------------------------------------------------------------------

**Success status:** `200 OK`

**Possible errors** - `400 VALIDATION_ERROR` --- query is null, sort
field is unsupported, or pagination values are invalid. -
`500 INTERNAL_ERROR` --- unexpected internal error.

**Request example**

``` java
TaskQuery query = new TaskQuery.Builder()
    .search("rent")
    .completed(false)
    .priority(Priority.HIGH)
    .sortBy("dueDate")
    .page(1)
    .pageSize(10)
    .build();

ApiResponse response = api.queryTasks(query);
```

**Success response example**

``` json
{
  "status": 200,
  "body": [
    {
      "id": 1,
      "title": "Pay rent",
      "description": "Pay the monthly apartment rent",
      "completed": false,
      "dueDate": "2026-12-01",
      "priority": "HIGH",
      "parentId": null,
      "createdAt": 1790000000000
    }
  ]
}
```

## 4. Status code reference

  --------------------------------------------------------------------------
                        Status Code                    Meaning
  ---------------------------- ----------------------- ---------------------
                           200 ---                     Successful read,
                                                       update, or delete

                           201 ---                     Task or subtasks
                                                       created

                           400 `VALIDATION_ERROR`      Input or generated
                                                       data failed
                                                       validation

                           404 `NOT_FOUND`             Requested task or
                                                       parent task does not
                                                       exist

                           422 `CONFLICT`              Idempotency conflict
                                                       or invalid
                                                       parent/operation
                                                       relationship

                           502 `BAD_GATEWAY`           LLM provider,
                                                       network, or response
                                                       failure

                           503 `SERVICE_UNAVAILABLE`   LLM API key missing
                                                       or provider
                                                       configuration
                                                       unavailable

                           500 `INTERNAL_ERROR`        Unexpected internal
                                                       error
  --------------------------------------------------------------------------

## 5. LLM configuration

The project supports these environment variables:

  ------------------------------------------------------------------------
Variable                                 Required? Description
  --------------------- ---------------------------- ---------------------
`USE_LLM`                                       No Set to `true` to
select LLM-backed
parsing and breakdown
in `Main`; defaults
to `false`

`LLM_API_KEY`                     For live LLM use API key for the
selected provider

`LLM_PROVIDER`                                  No `anthropic`
(default), `openai`,
or `gemini`

`LLM_MODEL`                                     No Optional provider
model override
  ------------------------------------------------------------------------

When `USE_LLM` is false, `Main` uses the rule-based offline parser and
breakdown strategy. The test suite uses fake/faulty LLM clients to test
integration behavior without paid API calls.

Never commit API keys or include them in source files, documentation,
screenshots, or Git history.


## 6. Implementation notes and contract boundaries

- `TaskApi` is a Java facade, not an HTTP server. The status values in this document are HTTP-style status codes returned in `ApiResponse`; no HTTP routes are started by `Main`.
- `ApiResponse` contains `status` and `body`. Successful bodies are Java `Task` objects or lists of tasks; error bodies are maps with an `error` object.
- Validation errors use `400 VALIDATION_ERROR` and include `details`, where each detail contains `field` and `issue`.
- Other defined application errors use `code` and `message`. Unexpected exceptions are converted to `500 INTERNAL_ERROR`.
- `createTaskFromText` rethrows configured `ApiException` types from its parser. `breakDownTask` currently catches exceptions from its breakdown strategy and wraps them as `502 BAD_GATEWAY`; see the implementation note in Operation 3.9.
- `listTasks` returns all repository tasks, including subtasks. `listSubtasks(parentId)` returns only direct children of the specified parent.
- The repository and idempotency store are in-memory. Data and idempotency records do not survive application restarts.
