---
name: kotlin-spring-arch
description: Architecture reference for this project — Controller/Service/Repository layer responsibilities, @Transactional strategy (readOnly optimization, N+1 prevention), exception handling, and Entity↔DTO conversion patterns.
---

# Kotlin + Spring Boot Architecture Guide

## Layer Structure

### Controller
- Role: Request validation, DTO conversion, HTTP response
- Annotations: `@RestController`, `@RequestMapping`
- Validation: `@Valid`, `@Validated`
- Response: return the DTO directly unless the project has a common response wrapper (see "Exception Handling" below for how to check)

```kotlin
fun getStudent(): StudentResDto = queryStudentService.execute(id)
```

### Service
- Role: Business logic, transaction management
- Pattern: interface + implementation
- Transaction:
  - Read: `@Transactional(readOnly = true)`
  - Write: `@Transactional`
- Dependencies: Inject Repository via constructor injection

### Repository
- Role: Data access
- JPA: Extend `JpaRepository`
- QueryDSL: Complex queries
- Avoid N+1: Fetch Join, `@EntityGraph`

## Transaction Strategy

### Read-only Optimization
```kotlin
@Transactional(readOnly = true)
fun findApiKeys(): List<ApiKeyResDto> {
    return repository.findAll()
        .map { it.toResDto() }
}
```

### N+1 Problem Resolution
```kotlin
// ❌ N+1 occurs
repository.findAll() // 1 query
entity.relatedEntity // N queries

// ✅ Fetch Join
@Query("SELECT e FROM Entity e JOIN FETCH e.relatedEntity")
fun findAllWithRelated(): List<Entity>
```

## Exception Handling

이 프로젝트는 아직 초기 단계라 공통 예외 클래스(`ExpectedException` 류)나 공통 응답 래퍼(`CommonApiResponse` 류)가 존재하지 않을 수 있다. 아래 순서로 확인 후 적용한다:

1. 먼저 존재 여부를 확인한다:
   ```bash
   find src/main -name "ExpectedException.kt" -o -name "CommonApiResponse.kt" -o -name "GlobalExceptionHandler.kt"
   ```
2. 위 파일들이 존재하면 그 구현을 그대로 따른다(공통 예외 클래스로 던지고, 컨트롤러는 DTO를 직접 반환한다).
3. 존재하지 않으면(현재 MUDDA 상태) 이 섹션은 **향후 도입 시 참고용 권장 패턴**으로만 취급한다. 지금은 표준 Spring 방식(`@ExceptionHandler` + `ResponseEntity<T>`, 또는 있는 그대로의 코드 스타일)을 따르고, 임의로 `ExpectedException` 같은 클래스를 새로 만들지 않는다.

## DTO Conversion Pattern

```kotlin
// Entity → ResDto
fun Entity.toResDto() = EntityResDto(
    id = this.id,
    name = this.name
)

// ReqDto → Entity
fun EntityReqDto.toEntity() = Entity(
    name = this.name
)
```