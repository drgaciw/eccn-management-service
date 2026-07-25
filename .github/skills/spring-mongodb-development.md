---
name: spring-mongodb-development
description: Use when modifying Java Spring MVC controllers, services, MongoDB repositories, models, validation, or Spring configuration in this repository.
---

# Spring MongoDB Development

- Follow controller → service → repository → MongoDB layering.
- Prefer constructor injection and explicit validation.
- Use Spring Data `MongoRepository` derived queries or clear `@Query` usage.
- Be cautious with Mongo transactions; local Compose is a single Mongo container.
- Preserve Java 21 and Spring Boot 3.4.1 idioms.
- Before editing Java symbols, run GitNexus impact analysis when available.
- Validate Lombok-generated accessors/builders with Maven.
