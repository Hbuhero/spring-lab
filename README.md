# Spring Boot Engineering Reference Platform

A continuously evolving **Spring Boot reference project, reusable foundation, and engineering sandbox** for exploring modern backend development practices and production-ready Spring architecture.

This repository is intentionally more than a boilerplate project. It serves as a place to **implement, experiment with, document, and continuously improve common backend capabilities** that can be reused when starting new Spring projects.

The project evolves alongside the Spring ecosystem, making it both a practical reference implementation and a personal laboratory for learning modern Spring and Java backend engineering.

---

## 🎯 Purpose

When starting a new Spring project, many concerns are repeated:

* Authentication and authorization
* Database configuration
* File storage
* API security
* Caching
* Rate limiting
* Messaging
* Observability
* Exception handling
* Configuration management
* Testing
* Containerization

Instead of implementing these independently for every project, this repository provides a **working reference implementation** for each concern.

The goal is to answer questions such as:

> *How should I implement this capability in a modern Spring application?*

> *What does the production-oriented configuration look like?*

> *What are the trade-offs between different approaches?*

> *How has Spring's recommended approach changed over time?*

The repository is therefore continuously updated rather than treated as a finished product.

---

## 🏗️ What This Project Demonstrates

### 🔐 Authentication & Authorization

Security is one of the core capabilities of the project.

Examples include:

* Spring Security
* Authentication and authorization
* OAuth2 / OpenID Connect
* JWT-based authentication
* Session-based authentication
* Password management
* Role and authority-based access control
* Securing REST endpoints
* Security-related exception handling

The implementation may evolve as Spring Security introduces or recommends new approaches.

---

### 📁 File Management

The project includes a reusable approach for handling files through an API.

Capabilities include:

* File upload
* File download
* File metadata
* File validation
* Access control
* Storage abstraction
* Handling large files
* Safe file naming
* Content-type validation

The storage implementation can be adapted depending on the target application and infrastructure.

---

### ⚡ Caching

Caching is implemented as a practical exploration of application and distributed caching.

Topics include:

* Spring Cache
* Cache abstraction
* Redis
* Cache keys
* TTL
* Cache invalidation
* Cache-aside patterns
* Avoiding stale data
* Caching expensive operations

The objective is not simply to add caching, but to understand **where caching is useful and where it introduces consistency problems**.

---

### 🚦 Rate Limiting

API rate limiting is explored as a mechanism for protecting services from excessive traffic.

Topics include:

* Request throttling
* Per-user limits
* Per-IP limits
* Distributed rate limiting
* Redis-backed rate limiting
* Burst handling
* API protection
* Rate-limit responses

---

### 📊 Observability

The project provides a place to experiment with the three major pillars of observability:

**Logs**

* Structured logging
* JSON logs
* Correlation IDs
* Request logging
* Error logging

**Metrics**

* Application metrics
* JVM metrics
* HTTP metrics
* Custom business metrics
* Prometheus-compatible metrics

**Traces**

* Distributed tracing
* Trace IDs
* Span propagation
* Service-to-service tracing

Additional infrastructure such as OpenTelemetry, Prometheus, Grafana, Elasticsearch, or other observability tools may be introduced as the project evolves.

---

### 📨 Kafka & Event-Driven Architecture

Kafka is included as a foundation for experimenting with asynchronous and event-driven systems.

Topics include:

* Kafka producers
* Kafka consumers
* Topics and partitions
* Consumer groups
* Serialization
* Error handling
* Retry strategies
* Dead-letter topics
* Message ordering
* Event-driven communication

The Kafka implementation is intentionally kept understandable so that it can serve as a starting point for more complex event-driven architectures.

---

### 🗄️ Database & Persistence

The persistence layer demonstrates common Spring Data practices.

Technologies and concepts include:

* PostgreSQL
* Spring Data JPA
* Hibernate
* Transactions
* Database migrations
* Entity relationships
* Query optimization
* Pagination
* Auditing
* Optimistic/pessimistic locking where appropriate

---

### 🧩 API Design

The project follows consistent REST API patterns for common backend requirements.

Examples include:

* Request validation
* Global exception handling
* Consistent error responses
* Pagination
* Filtering
* Sorting
* HTTP status codes
* API documentation
* DTO-based API boundaries

---

### ⚙️ Configuration & Environment Management

The project demonstrates how application configuration can be separated from application code.

Topics include:

* `application.yml`
* Environment-specific configuration
* Environment variables
* Secrets
* Externalized configuration
* Profiles
* Docker-based configuration
* Configuration validation

---

### 🧪 Testing

Testing is treated as part of the architecture rather than an afterthought.

Examples include:

* Unit tests
* Integration tests
* Repository tests
* Controller tests
* Security tests
* Testcontainers
* Database integration tests
* Kafka integration tests

---

## 🛠️ Technology Stack

| Area               | Technology                  |
| ------------------ | --------------------------- |
| Language           | Java                        |
| Framework          | Spring Boot                 |
| Security           | Spring Security             |
| Persistence        | Spring Data JPA / Hibernate |
| Database           | PostgreSQL                  |
| Messaging          | Apache Kafka                |
| Caching            | Redis                       |
| Observability      | Micrometer / OpenTelemetry  |
| Containerization   | Docker                      |
| Database Migration | Liquibase                   |
| Build Tool         | Maven                       |
| API                | REST                        |

The technology stack is intentionally subject to change as new versions, libraries, and Spring practices are evaluated.

---

## 📐 Architecture

The application follows a modular Spring architecture designed to keep infrastructure concerns separated from business logic.

```text
                         ┌─────────────────────┐
                         │       Client        │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │     REST API        │
                         └──────────┬──────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                  │
                 ▼                  ▼                  ▼
          ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
          │  Security   │    │   Service   │    │    Cache    │
          │             │    │   Layer     │    │   / Redis   │
          └─────────────┘    └──────┬──────┘    └─────────────┘
                                    │
                         ┌──────────┴──────────┐
                         │                     │
                         ▼                     ▼
                  ┌─────────────┐       ┌─────────────┐
                  │ PostgreSQL  │       │    Kafka    │
                  └─────────────┘       └─────────────┘

                         ┌─────────────────────┐
                         │    Observability    │
                         │ Logs / Metrics /    │
                         │ Traces              │
                         └─────────────────────┘
```

The architecture is deliberately designed so individual capabilities can be studied and adapted independently.

---

## 🚀 Getting Started

### Prerequisites

Make sure the following are installed:

* Java
* Maven
* Docker
* Docker Compose

Depending on the current modules, you may also need:

* PostgreSQL
* Redis
* Kafka

### Clone the repository

```bash
git clone <repository-url>

cd <repository-directory>
```

### Configure the environment

Create the required environment configuration:

```bash
cp .env.example .env
```

Update the values according to your local environment.

### Start infrastructure

```bash
docker compose up -d
```

### Run the application

Using Maven:

```bash
./mvnw spring-boot:run
```

Or build and run the application:

```bash
./mvnw clean package
java -jar target/*.jar
```

---

## 🧭 Repository Philosophy

This project follows a few principles.

### 1. Prefer understanding over copying

Configurations are not added simply because they are commonly found in Spring projects.

Each major capability should answer:

* Why is this needed?
* How does it work?
* What problem does it solve?
* What are its trade-offs?
* When should it not be used?

---

### 2. Keep infrastructure replaceable

Infrastructure should not unnecessarily leak into business logic.

For example:

```text
Application
    │
    ├── Cache abstraction
    │       └── Redis implementation
    │
    ├── Storage abstraction
    │       └── Local/S3 implementation
    │
    └── Messaging abstraction
            └── Kafka implementation
```

This makes it easier to replace infrastructure without rewriting the application.

---

### 3. Follow the Spring ecosystem

The project is continuously updated to evaluate:

* New Spring Boot releases
* Spring Framework changes
* Spring Security improvements
* Spring Data improvements
* New observability capabilities
* Changes in recommended configuration
* Improvements in Java

Older implementations may occasionally remain in Git history to make architectural changes and migration paths easier to understand.

---

### 4. Experiment deliberately

Not every implementation in this repository is intended to be the final production solution.

Some components exist specifically to answer questions, test an approach, or compare alternatives.

Experimental work should therefore be clearly identified rather than presented as universally recommended architecture.

---

## 🧪 Experiments

The repository can be used to experiment with topics such as:

* Redis vs in-memory caching
* Session-based vs token-based authentication
* Synchronous vs asynchronous communication
* Kafka retry strategies
* Distributed rate limiting
* Different file storage strategies
* Database transaction boundaries
* Observability architectures
* API performance
* Spring Security configurations
* Containerized infrastructure
* Integration testing with Testcontainers

Each experiment should ideally document:

```text
Problem
   ↓
Approach
   ↓
Implementation
   ↓
Results
   ↓
Trade-offs
   ↓
Conclusion
```

---

## 📚 Learning & Reference

This repository is also intended to function as a personal reference when starting new Spring projects.

Instead of searching through previous projects for a particular configuration, the relevant implementation can be found here and adapted to the requirements of the new application.

Examples:

```text
"I need OAuth2 authentication."
        ↓
Check security module

"I need Redis caching."
        ↓
Check caching module

"I need Kafka."
        ↓
Check messaging module

"I need distributed tracing."
        ↓
Check observability module
```

---

## 🗺️ Roadmap

The project will evolve as new technologies and architectural concerns are explored.

### Core

* [x] Spring Boot application foundation
* [x] Authentication
* [x] Authorization
* [x] File upload
* [x] File download
* [ ] API documentation
* [ ] Advanced validation patterns
* [ ] Advanced testing patterns

### Performance & Resilience

* [ ] Application caching
* [ ] Redis
* [ ] Rate limiting
* [ ] Retry mechanisms
* [ ] Circuit breakers
* [ ] Resilience patterns
* [ ] Connection pool tuning

### Messaging

* [ ] Kafka producer
* [ ] Kafka consumer
* [ ] Consumer groups
* [ ] Retry topics
* [ ] Dead-letter topics
* [ ] Event-driven workflows

### Observability

* [ ] Structured logging
* [ ] Correlation IDs
* [ ] Micrometer metrics
* [ ] Prometheus
* [ ] Grafana
* [ ] Distributed tracing
* [ ] OpenTelemetry

### Infrastructure

* [ ] Docker
* [ ] Docker Compose
* [ ] Testcontainers
* [ ] CI/CD
* [ ] Environment-specific deployments

### Spring Evolution

* [ ] Evaluate new Spring Boot releases
* [ ] Evaluate new Spring Security features
* [ ] Evaluate new Spring Data features
* [ ] Evaluate new Java features
* [ ] Document migration strategies

---

## 🔄 Continuous Evolution

This repository is intentionally **never considered finished**.

As Spring Boot, Spring Framework, Spring Security, Java, and the surrounding ecosystem evolve, this project will evolve with them.

Major changes may include:

```text
New Spring release
       ↓
Evaluate new feature
       ↓
Implement in reference project
       ↓
Compare with previous approach
       ↓
Document findings
       ↓
Adopt, modify, or reject
```

This makes the repository a record of both **what works** and **why particular technical decisions were made**.

---

## 📌 Intended Use

This repository is primarily intended for:

* Starting new Spring Boot projects
* Referencing common configurations
* Learning Spring ecosystem features
* Experimenting with backend architecture
* Comparing implementation approaches
* Testing infrastructure integrations
* Keeping up with Spring and Java ecosystem changes

It is **not intended to be a universal production template**. Each application should evaluate its own requirements, security model, infrastructure, performance characteristics, and operational constraints.

---

## 👨‍💻 About the Project

This project is part of my continuous exploration of **Java, Spring Boot, distributed systems, backend architecture, and cloud-native application development**.

Rather than maintaining isolated examples, I use a single evolving system to bring different backend engineering concepts together and understand how they interact in a realistic application.

The repository therefore serves three purposes:

**Reference** — reusable implementations for future projects.

**Laboratory** — a safe environment for experimenting with new technologies and architectural patterns.

**Knowledge base** — a continuously evolving record of lessons, trade-offs, configurations, and migration paths.

---

## 🐳 Docker

The project can be packaged as a Docker image with the included `Dockerfile`.

### Build the image

```bash
docker build -t spring-security-email-verification:latest .
```

### Run with Docker Compose

The simplest local runtime uses `docker-compose.yml`, which starts both the Spring Boot application and PostgreSQL.

```bash
cp .env.example .env
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080
```

### Run only the application image

Use this when PostgreSQL is already running somewhere else.

```bash
docker run --rm \
  --name spring-security-email-verification \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/email_verification \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e APPLICATION_JWTSECRET=replace-with-a-long-random-secret \
  spring-security-email-verification:latest
```

### Publish the image

Tag and push the image to a registry so other people can pull it.

```bash
docker tag spring-security-email-verification:latest your-dockerhub-username/spring-security-email-verification:latest
docker push your-dockerhub-username/spring-security-email-verification:latest
```

Then anyone can run it with:

```bash
docker run --rm -p 8080:8080 your-dockerhub-username/spring-security-email-verification:latest
```

For a useful runtime, they still need to provide database, JWT, email, and OAuth environment variables.

---

## 📄 License

This project is available under the [MIT License](LICENSE).
