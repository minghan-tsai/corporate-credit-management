# Corporate Credit Management System

## Project Purpose

Corporate Credit Management System is a Java and Spring Boot portfolio project that models a focused subset of a bank-internal corporate lending workflow. The project is intended to demonstrate business-rule design, layered architecture, transaction integrity, authorization, auditability, and automated testing without attempting to reproduce a complete core banking system.

## Planned Features

- Corporate customer management
- Credit application drafting and submission
- Reviewer approval or rejection with Maker-Checker controls
- Approved credit-limit management
- Drawdown processing with transactional consistency
- Role-based access control for RM, REVIEWER, and ADMIN
- Business audit trail
- Validation, exception handling, filtering, and pagination
- Unit, integration, security, and transaction tests

## Planned Tech Stack

- Java 21
- Spring Boot 3.x and Maven
- Spring Web, Spring Data JPA, Spring Security, and Spring Validation
- PostgreSQL, Hibernate, and Flyway
- JUnit 5, Mockito, Spring Boot Test, and optionally Testcontainers
- Later stages: Docker Compose and GitHub Actions

## Current Status

**Stage 0 — Project Planning and Initialization**

Only the minimal Spring Boot application skeleton, build configuration, and planning documentation exist. No business domain, API, security flow, database migration, Docker setup, or CI workflow has been implemented.
