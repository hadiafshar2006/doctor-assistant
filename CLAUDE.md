# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that implements a medical assistant system with AI-powered query processing and vector search capabilities. The system uses a two-stage LLM approach: LLM1 determines search strategy and LLM2 provides final answers based on retrieved context.

## Architecture

The application follows Spring Boot best practices with clear separation of concerns:

- **Controllers**: Handle HTTP requests and delegate to services
- **Services**: Implement business logic and coordinate between components
- **Repositories**: Provide data access layer using Spring Data JPA
- **Entities**: Define JPA data models
- **DTOs**: Data transfer objects for API requests/responses
- **Config**: Configuration classes for vector stores and properties

### Key Components

- **QueryService**: Core service implementing the two-LLM workflow (LLM1 → Vector Search → LLM2)
- **Vector Stores**: Two separate PostgreSQL vector stores (patient documents and medical knowledge)
- **Patient Management**: CRUD operations for patients and their documents
- **Knowledge Management**: Medical knowledge document management

### Package Structure
```
src/main/java/de/aporz/doctorassistant/
├── config/          # Configuration classes (VectorStoreConfig, Properties)
├── controller/      # REST controllers
├── dto/            # Data transfer objects
├── entity/         # JPA entities (Patient, PatientDocument, KnowledgeDocument)
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic services
└── util/           # Utility classes (QueryUtils for date parsing and filters)
```

## Development Commands

### Build and Run
```bash
# Build the application
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Build executable JAR
./mvnw clean package
```

### Testing
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=QueryServiceTest

# Run tests with coverage
./mvnw test jacoco:report
```

### Database Setup
The application requires PostgreSQL with pgvector extension:
- Database: `medical_system`
- Username/Password: `dev/dev` (configurable in application.properties)
- Tables are auto-created via JPA (ddl-auto=update)

## Key Configuration

### Application Properties
- **Database**: PostgreSQL with Spring Data JPA
- **AI Models**: Configurable between Ollama (local) and OpenAI
- **Vector Stores**: Two separate PgVector stores with custom table names
- **Embeddings**: German language model (jina/jina-embeddings-v2-base-de)

### Important Settings
```properties
spring.jpa.hibernate.ddl-auto=update  # Auto-create/update database schema
vector.topk.patient=5                 # Number of patient documents to retrieve
vector.topk.knowledge=5               # Number of knowledge documents to retrieve
```

## Code Conventions

1. **German Documentation**: Comments and documentation in German (as per project requirements)
2. **English Code**: All class names, method names, and variables in English
3. **No JDBC**: Use JPA exclusively for database operations (no direct JDBC)
4. **Service Layer**: Controllers must delegate to services; only services access repositories
5. **JSON Handling**: Jackson ObjectMapper with proper error handling for LLM responses
6. **XML Escaping**: All user data in prompts must be XML-escaped using StringEscapeUtils

## Important Implementation Details

### Two-LLM Query Processing
1. **LLM1**: Determines if it can answer directly or needs vector search queries
2. **Vector Search**: Parallel searches across patient documents and medical knowledge
3. **LLM2**: Generates final answer based on retrieved context

### Vector Store Architecture
- **Patient Documents**: Filtered by patient ID and date ranges
- **Medical Knowledge**: General medical information without patient-specific filters
- Both use cosine similarity with 768-dimensional embeddings

### Error Handling
- JSON parsing retries for malformed LLM responses
- Graceful fallbacks when vector stores are unavailable
- Proper exception propagation with meaningful error messages

## Testing Strategy

The codebase includes comprehensive tests:
- **Unit Tests**: Service logic and utility functions
- **Integration Tests**: Controller endpoints and database operations
- **H2 Database**: Used for test isolation
- **Spring Boot Test**: Full application context testing

## Development Notes

- No Flyway or Liquibase (use JPA schema generation)
- SpringDoc OpenAPI integration for Swagger UI
- Lombok for reducing boilerplate code
- Jackson for JSON processing with LLM responses
- Apache Commons Text for XML escaping