# Spring Boot + PostgreSQL: Complete Student CRUD REST API

**Project:** CRTD  
**Technology:** Java 17, Spring Boot 4.1.2, Spring Data JPA, Hibernate, PostgreSQL 18, Maven  
**Estimated duration:** 5 hours 30 minutes  
**Outcome:** Build and test a complete REST API for creating, reading, updating and deleting student records.

---

## 1. Tutorial overview

In this tutorial, you will extend your existing CRTD Spring Boot project. You will use **student data instead of employee data**, while keeping the same four data fields from the earlier entity:

- `id`
- `firstName`
- `lastName`
- `email`

The database table will be named `students`.

### Learning outcomes

By the end, you should be able to:

1. Explain the Controller → Service → Repository → Database flow.
2. Create a JPA entity and map it to a PostgreSQL table.
3. Use Spring Data JPA to persist and retrieve records.
4. Use request and response DTOs.
5. Validate incoming request data.
6. Implement all five CRUD endpoints.
7. Return suitable HTTP status codes.
8. Handle missing records and duplicate email addresses.
9. Test endpoints using Postman or IntelliJ HTTP Client.
10. Verify saved data directly in PostgreSQL.

### Time allocation

| Tutorial | Activity | Time |
|---|---|---:|
| 1 | Project inspection and setup | 20 minutes |
| 2 | Student entity and database mapping | 30 minutes |
| 3 | Repository | 25 minutes |
| 4 | Request and response DTOs with validation | 40 minutes |
| 5 | Service layer and business rules | 60 minutes |
| 6 | REST controller and CRUD endpoints | 60 minutes |
| 7 | Exception handling and API responses | 35 minutes |
| 8 | Testing all endpoints and database verification | 45 minutes |
| 9 | Review, cleanup and Git submission | 15 minutes |
| **Total** | | **5 hours 30 minutes** |

Take short breaks between tutorials if needed.

---

# Tutorial 1 — Inspect and prepare the project

**Time: 20 minutes**

## Step 1.1 — Confirm your existing project structure

Your current project should be organised approximately like this:

```text
CRTD/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/murendeni/crtd/
│   │   │       ├── CrtdApplication.java
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exceptions/
│   │   │       ├── repository/
│   │   │       └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
└── HELP.md
```

If the `dto` folder does not exist, create it.

## Step 1.2 — Confirm your database configuration

Open:

`src/main/resources/application.properties`

Use your existing PostgreSQL connection details:

```properties
spring.application.name=CRTD

spring.datasource.url=jdbc:postgresql://localhost:5432/ems
spring.datasource.username=postgres
spring.datasource.password=root

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false

logging.level.org.hibernate.SQL=DEBUG
```

**Important:** `application.properties` must be inside `src/main/resources`. Do not name it `app.properties`.

For a real deployment, do not commit database passwords to Git. Use environment variables or a secrets manager.

## Step 1.3 — Check Maven dependencies

Open `pom.xml`. Your project needs dependencies for:

- Spring Web MVC / REST endpoints
- Spring Data JPA
- PostgreSQL JDBC driver
- Lombok
- Spring Boot Validation

If you are missing validation, add this dependency inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Do not add duplicate dependencies if they are already present. Let Maven reload the project.

## Step 1.4 — Start the application

Run `CrtdApplication.java` from IntelliJ.

Confirm that:

- The application starts without errors.
- PostgreSQL connects successfully.
- Tomcat starts on port `8080`.

### Checkpoint

You should have a running application and a working PostgreSQL connection.

**Git commit:**

```bash
git add .
git commit -m "chore: prepare project for student CRUD API"
```

---

# Tutorial 2 — Create the Student entity

**Time: 30 minutes**

An entity is a Java class that JPA maps to a database table.

## Step 2.1 — Create `Student.java`

Location:

`src/main/java/com/murendeni/crtd/entity/Student.java`

```java
package com.murendeni.crtd.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;
}
```

## Step 2.2 — Understand the annotations

| Annotation | Purpose |
|---|---|
| `@Entity` | Marks the class as a JPA entity |
| `@Table` | Specifies the database table name |
| `@Id` | Marks the primary key |
| `@GeneratedValue` | Configures ID generation |
| `@Column` | Configures a database column |
| `@NoArgsConstructor` | Lombok generates a no-argument constructor |
| `@AllArgsConstructor` | Lombok generates a constructor with all fields |
| `@Getter` / `@Setter` | Lombok generates getters and setters |

The `id` is generated by the database. The client must not submit it when creating a student.

## Step 2.3 — Confirm entity scanning

Your main class should remain in the parent package:

`com.murendeni.crtd`

```java
package com.murendeni.crtd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrtdApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrtdApplication.class, args);
    }
}
```

Spring Boot scans the main package and its subpackages by default. Therefore, it should discover `com.murendeni.crtd.entity.Student`.

## Step 2.4 — Verify the database table

Restart the application. With `spring.jpa.hibernate.ddl-auto=update`, Hibernate should create or update the table.

Run this SQL in your PostgreSQL console:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'students';
```

Inspect the columns:

```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'students'
ORDER BY ordinal_position;
```

### Checkpoint

You should have a `students` table with an ID, first name, last name and email.

**Git commit:**

```bash
git add .
git commit -m "feat: add student JPA entity"
```

---

# Tutorial 3 — Create the Student repository

**Time: 25 minutes**

A repository provides methods for interacting with the database. Spring Data JPA implements the repository at runtime.

## Step 3.1 — Create `StudentRepository.java`

Location:

`src/main/java/com/murendeni/crtd/repository/StudentRepository.java`

```java
package com.murendeni.crtd.repository;

import com.murendeni.crtd.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Student> findByEmail(String email);
}
```

## Step 3.2 — Understand `JpaRepository`

By extending `JpaRepository<Student, Long>`, you receive common database operations without writing SQL.

| Method | Purpose |
|---|---|
| `save(student)` | Insert or update a student |
| `findAll()` | Retrieve all students |
| `findById(id)` | Retrieve one student by ID |
| `existsById(id)` | Check whether an ID exists |
| `deleteById(id)` | Delete a student by ID |
| `count()` | Count student records |

The extra methods are derived queries. Spring Data JPA builds the query from the method name.

- `existsByEmail` checks whether an email is already in use.
- `existsByEmailAndIdNot` checks whether another student has the email.
- `findByEmail` searches by email.

## Step 3.3 — Why do we need a repository?

The service layer should not contain SQL connection code. It should use the repository to request database operations.

### Checkpoint

The application should start with no repository errors. You should see one JPA repository interface discovered in the startup logs.

**Git commit:**

```bash
git add .
git commit -m "feat: add student repository"
```

---

# Tutorial 4 — Create request and response DTOs

**Time: 40 minutes**

DTO means Data Transfer Object. DTOs define the data entering and leaving your API.

Do not expose your JPA entity directly as the API request model. Keeping DTOs separate gives you control over validation and the fields clients can send.

## Step 4.1 — Create `StudentRequest.java`

Location:

`src/main/java/com/murendeni/crtd/dto/StudentRequest.java`

```java
package com.murendeni.crtd.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email

) {
}
```

## Step 4.2 — Understand validation

| Annotation | Purpose |
|---|---|
| `@NotBlank` | Rejects null, empty or whitespace-only text |
| `@Email` | Checks that the value has a valid email format |
| `@Size` | Restricts the length of a value |

These checks run when the controller uses `@Valid`.

## Step 4.3 — Create `StudentResponse.java`

Location:

`src/main/java/com/murendeni/crtd/dto/StudentResponse.java`

```java
package com.murendeni.crtd.dto;

public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {
}
```

The response includes the generated ID, while the request does not.

## Step 4.4 — Understand the difference

**Request example:**

```json
{
  "firstName": "Thabo",
  "lastName": "Mokoena",
  "email": "thabo.mokoena@example.com"
}
```

**Response example:**

```json
{
  "id": 1,
  "firstName": "Thabo",
  "lastName": "Mokoena",
  "email": "thabo.mokoena@example.com"
}
```

### Checkpoint

You should now have two DTO records. The request DTO validates incoming data; the response DTO defines what the API returns.

**Git commit:**

```bash
git add .
git commit -m "feat: add student request and response DTOs"
```

---

# Tutorial 5 — Implement the service layer

**Time: 60 minutes**

The service layer contains the application's business logic. The controller should handle HTTP requests, while the service coordinates the work.

## Step 5.1 — Create `StudentService.java`

Location:

`src/main/java/com/murendeni/crtd/service/StudentService.java`

```java
package com.murendeni.crtd.service;

import com.murendeni.crtd.dto.StudentRequest;
import com.murendeni.crtd.dto.StudentResponse;
import com.murendeni.crtd.entity.Student;
import com.murendeni.crtd.exceptions.ResourceNotFoundException;
import com.murendeni.crtd.exceptions.DuplicateResourceException;
import com.murendeni.crtd.repository.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // CREATE
    @Transactional
    public StudentResponse createStudent(StudentRequest request) {

        if (studentRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "A student with this email already exists"
            );
        }

        Student student = new Student();
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());

        Student savedStudent = studentRepository.save(student);

        return mapToResponse(savedStudent);
    }

    // READ ALL
    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {

        return studentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // READ ONE
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {

        Student student = findStudentOrThrow(id);

        return mapToResponse(student);
    }

    // UPDATE
    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {

        Student student = findStudentOrThrow(id);

        boolean emailUsedByAnotherStudent =
                studentRepository.existsByEmailAndIdNot(
                        request.email(),
                        id
                );

        if (emailUsedByAnotherStudent) {
            throw new DuplicateResourceException(
                    "Another student is already using this email"
            );
        }

        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());

        Student updatedStudent = studentRepository.save(student);

        return mapToResponse(updatedStudent);
    }

    // DELETE
    @Transactional
    public void deleteStudent(Long id) {

        Student student = findStudentOrThrow(id);

        studentRepository.delete(student);
    }

    private Student findStudentOrThrow(Long id) {

        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with ID: " + id
                ));
    }

    private StudentResponse mapToResponse(Student student) {

        return new StudentResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail()
        );
    }
}
```

## Step 5.2 — Understand CREATE

```java
Student student = new Student();

student.setFirstName(request.firstName());
student.setLastName(request.lastName());
student.setEmail(request.email());

Student savedStudent = studentRepository.save(student);
```

The request contains the information from the client. The service creates an entity and copies the values into it. The repository then persists that entity.

## Step 5.3 — Understand READ ALL

```java
return studentRepository.findAll()
        .stream()
        .map(this::mapToResponse)
        .toList();
```

This retrieves every student and converts each entity into a response DTO.

## Step 5.4 — Understand READ ONE

```java
studentRepository.findById(id)
```

This returns an `Optional<Student>`. The `orElseThrow()` method allows you to return a meaningful error if the record does not exist.

## Step 5.5 — Understand UPDATE

The service first retrieves the existing student. It then checks whether another student is using the requested email address before changing the record.

It does not create a new student when the supplied ID is missing.

## Step 5.6 — Understand DELETE

The service finds the student first. If it exists, it deletes the entity. If it does not exist, it throws a not-found exception.

### Checkpoint

Review each method and make sure you understand why the repository is called from the service rather than directly from the controller.

**Git commit:**

```bash
git add .
git commit -m "feat: implement student CRUD service logic"
```

---

# Tutorial 6 — Create custom exceptions

**Time: 35 minutes**

Your application needs a consistent way to communicate errors.

Create two exception classes.

## Step 6.1 — Create `ResourceNotFoundException.java`

Location:

`src/main/java/com/murendeni/crtd/exceptions/ResourceNotFoundException.java`

```java
package com.murendeni.crtd.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

## Step 6.2 — Create `DuplicateResourceException.java`

Location:

`src/main/java/com/murendeni/crtd/exceptions/DuplicateResourceException.java`

```java
package com.murendeni.crtd.exceptions;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

## Step 6.3 — Why use custom exceptions?

Instead of returning unclear messages or allowing every error to produce a generic server response, custom exceptions let you describe what went wrong.

For example:

- A student ID does not exist.
- An email address is already registered.

We will map these exceptions to HTTP responses in the next tutorial.

**Git commit:**

```bash
git add .
git commit -m "feat: add student resource exceptions"
```

---

# Tutorial 7 — Build the REST controller

**Time: 60 minutes**

The controller exposes your service functionality through HTTP endpoints.

## Step 7.1 — Create `StudentController.java`

Location:

`src/main/java/com/murendeni/crtd/controller/StudentController.java`

```java
package com.murendeni.crtd.controller;

import com.murendeni.crtd.dto.StudentRequest;
import com.murendeni.crtd.dto.StudentResponse;
import com.murendeni.crtd.service.StudentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody StudentRequest request) {

        StudentResponse response =
                studentService.createStudent(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {

        return ResponseEntity.ok(
                studentService.getAllStudents()
        );
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                studentService.getStudentById(id)
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {

        return ResponseEntity.ok(
                studentService.updateStudent(id, request)
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable Long id) {

        studentService.deleteStudent(id);

        return ResponseEntity.noContent().build();
    }
}
```

## Step 7.2 — Understand the annotations

| Annotation | Purpose |
|---|---|
| `@RestController` | Declares a REST controller |
| `@RequestMapping` | Defines the common URL prefix |
| `@PostMapping` | Handles POST requests |
| `@GetMapping` | Handles GET requests |
| `@PutMapping` | Handles PUT requests |
| `@DeleteMapping` | Handles DELETE requests |
| `@RequestBody` | Converts JSON into a Java object |
| `@PathVariable` | Reads a value from the URL |
| `@Valid` | Triggers DTO validation |

## Step 7.3 — Understand the endpoint design

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/students` | Create a student |
| GET | `/api/v1/students` | Get all students |
| GET | `/api/v1/students/{id}` | Get one student |
| PUT | `/api/v1/students/{id}` | Update a student |
| DELETE | `/api/v1/students/{id}` | Delete a student |

### Checkpoint

Your application should compile. If you encounter errors, check package declarations, imports and class names.

**Git commit:**

```bash
git add .
git commit -m "feat: expose complete student CRUD REST endpoints"
```

---

# Tutorial 8 — Global exception handling

**Time: 35 minutes**

A global exception handler allows your application to return consistent error responses.

## Step 8.1 — Create `GlobalExceptionHandler.java`

Location:

`src/main/java/com/murendeni/crtd/exceptions/GlobalExceptionHandler.java`

```java
package com.murendeni.crtd.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException exception) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateResourceException exception) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> validationErrors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("messages", validationErrors);
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String message) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(status)
                .body(body);
    }
}
```

## Step 8.2 — Understand `@RestControllerAdvice`

`@RestControllerAdvice` allows the class to handle exceptions thrown by your controllers.

Instead of repeating `try/catch` blocks in every endpoint, the application uses one central handler.

## Step 8.3 — Example error responses

**Student does not exist:**

HTTP `404 Not Found`

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Student not found with ID: 99",
  "timestamp": "2026-11-16T12:00:00"
}
```

**Duplicate email:**

HTTP `409 Conflict`

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A student with this email already exists",
  "timestamp": "2026-11-16T12:00:00"
}
```

**Validation error:**

HTTP `400 Bad Request`

```json
{
  "status": 400,
  "error": "Validation failed",
  "messages": {
    "firstName": "First name is required",
    "email": "Enter a valid email address"
  },
  "timestamp": "2026-11-16T12:00:00"
}
```

**Git commit:**

```bash
git add .
git commit -m "feat: handle student API errors globally"
```

---

# Tutorial 9 — Test the complete CRUD API

**Time: 45 minutes**

You can use Postman or IntelliJ's built-in HTTP Client.

Start your Spring Boot application before running these tests.

Base URL:

```text
http://localhost:8080/api/v1/students
```

## Test 1 — Create a student

**Method:** POST

**URL:**

```text
http://localhost:8080/api/v1/students
```

**Body → raw → JSON:**

```json
{
  "firstName": "Thabo",
  "lastName": "Mokoena",
  "email": "thabo.mokoena@example.com"
}
```

Expected status:

```text
201 Created
```

Expected response:

```json
{
  "id": 1,
  "firstName": "Thabo",
  "lastName": "Mokoena",
  "email": "thabo.mokoena@example.com"
}
```

The actual ID may differ.

## Test 2 — Create more students

Create at least five student records in total. Use different email addresses.

Suggested test data:

| First name | Last name | Email |
|---|---|---|
| Thabo | Mokoena | thabo.mokoena@example.com |
| Lerato | Nkosi | lerato.nkosi@example.com |
| Sibusiso | Dlamini | sibusiso.dlamini@example.com |
| Naledi | Molefe | naledi.molefe@example.com |
| Karabo | Khumalo | karabo.khumalo@example.com |

Send a separate POST request for each student.

## Test 3 — Retrieve all students

**Method:** GET

**URL:**

```text
http://localhost:8080/api/v1/students
```

Expected status:

```text
200 OK
```

Expected result: a JSON array containing your saved students.

## Test 4 — Retrieve a student by ID

**Method:** GET

**URL:**

```text
http://localhost:8080/api/v1/students/1
```

Expected status:

```text
200 OK
```

Use an ID that actually exists in your database.

## Test 5 — Update a student

**Method:** PUT

**URL:**

```text
http://localhost:8080/api/v1/students/1
```

**Body:**

```json
{
  "firstName": "Thabo",
  "lastName": "Mokoena Updated",
  "email": "thabo.updated@example.com"
}
```

Expected status:

```text
200 OK
```

Expected response:

```json
{
  "id": 1,
  "firstName": "Thabo",
  "lastName": "Mokoena Updated",
  "email": "thabo.updated@example.com"
}
```

The ID remains unchanged.

## Test 6 — Delete a student

**Method:** DELETE

**URL:**

```text
http://localhost:8080/api/v1/students/1
```

Expected status:

```text
204 No Content
```

The response should have no body.

## Test 7 — Confirm deletion

Try retrieving the deleted student:

```text
GET http://localhost:8080/api/v1/students/1
```

Expected status:

```text
404 Not Found
```

## Test 8 — Test validation

Try creating a student with an invalid email:

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "email": "not-an-email"
}
```

Expected status:

```text
400 Bad Request
```

## Test 9 — Test duplicate email

Try creating two students using the same email address.

Expected status for the second request:

```text
409 Conflict
```

### Database verification

Run:

```sql
SELECT * FROM students ORDER BY id;
```

Check that:

- New students are inserted.
- Updates change the correct records.
- Deleted records no longer appear.
- Email addresses remain unique.

**Git commit:**

```bash
git add .
git commit -m "test: verify complete student CRUD API"
```

---

# Tutorial 10 — Final review and submission

**Time: 15 minutes**

## Final project structure

Your completed project should look like this:

```text
com.murendeni.crtd/
│
├── CrtdApplication.java
│
├── controller/
│   └── StudentController.java
│
├── dto/
│   ├── StudentRequest.java
│   └── StudentResponse.java
│
├── entity/
│   └── Student.java
│
├── exceptions/
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
│
├── repository/
│   └── StudentRepository.java
│
└── service/
    └── StudentService.java
```

## CRUD completion checklist

- [ ] Application starts successfully.
- [ ] PostgreSQL connection works.
- [ ] `students` table exists.
- [ ] Student entity maps correctly.
- [ ] Repository is detected by Spring Data JPA.
- [ ] Request DTO validates input.
- [ ] Response DTO returns the expected fields.
- [ ] POST creates a student.
- [ ] GET retrieves all students.
- [ ] GET by ID retrieves one student.
- [ ] PUT updates an existing student.
- [ ] DELETE removes an existing student.
- [ ] Missing student returns 404.
- [ ] Duplicate email returns 409.
- [ ] Invalid input returns 400.
- [ ] At least five student records have been tested.
- [ ] All changes have been committed to Git.

## Final Git submission

```bash
git add .
git commit -m "feat: complete student CRUD REST API with PostgreSQL"
git push
```

---

# Additional challenges

These are optional extensions after the core CRUD implementation works.

1. **Search by email:** Add an endpoint that retrieves a student using their email address.
2. **Search by surname:** Add a repository method to find students by last name.
3. **Pagination:** Use `Pageable` and `Page<Student>` to return students in pages.
4. **Sorting:** Allow clients to sort students by first name or last name.
5. **Partial updates:** Add a PATCH endpoint that updates only the supplied fields.
6. **Automated tests:** Write unit tests for the service and integration tests for the controller and repository.

For a production application, consider replacing automatic schema updates with version-controlled database migrations using Flyway or Liquibase. Also consider pagination for large datasets and database-level error handling for concurrent duplicate-email requests.

---

# Reference: HTTP status codes

| Status | Meaning | When your API uses it |
|---|---|---|
| `200 OK` | Request succeeded | GET and PUT |
| `201 Created` | Resource created | POST |
| `204 No Content` | Request succeeded with no response body | DELETE |
| `400 Bad Request` | Invalid request | Validation failure |
| `404 Not Found` | Resource does not exist | Student ID not found |
| `409 Conflict` | Request conflicts with existing data | Duplicate email |
| `500 Internal Server Error` | Unexpected server error | Unhandled application failure |

## Important development rule

Keep responsibilities separate:

- **Controller:** receives HTTP requests and returns HTTP responses.
- **Service:** contains business rules and coordinates operations.
- **Repository:** communicates with the database through Spring Data JPA.
- **Entity:** represents the database record.
- **DTO:** defines the data exchanged through the API.
- **Exception handler:** converts application errors into consistent HTTP responses.

A well-structured CRUD API is not just about making database operations work. It is about making the application understandable, maintainable, testable and safe to extend.
