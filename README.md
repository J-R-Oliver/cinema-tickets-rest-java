# Cinema Tickets Code Test

[![Build](https://github.com/J-R-Oliver/cinema-tickets-rest-java/actions/workflows/build.yaml/badge.svg)](https://github.com/J-R-Oliver/cinema-tickets-rest-java/actions/workflows/build.yaml)
[![Java Version](https://img.shields.io/badge/Java-v21-informational)](https://openjdk.org/projects/jdk/21/)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)
[![License: MIT](https://img.shields.io/github/license/J-R-Oliver/cinema-tickets-rest-java)](LICENSE)

<table>
<tr>
<td>
DWP Senior Software Engineer coding exercise — Cinema Tickets Code Test
</td>
</tr>
</table>

## Contents

- [Objective](#objective)
    - [Business Rules](#business-rules)
    - [Constraints](#constraints)
    - [Assumptions](#assumptions)
    - [Your Task](#your-task)
- [Solution](#solution)
    - [Getting Started](#getting-started)
    - [Assumptions](#assumptions-1)
    - [Implementation](#implementation)
    - [Conventional Commits](#conventional-commits)
    - [GitHub Actions](#github-actions)

## Objective

This is a coding exercise which will allow you to demonstrate how you code and your approach to a given problem.

You will be assessed on:

- Your ability to write clean, well-tested and reusable code.
- How you have understood, interpreted the business rules and ensured they are correctly met.
- Identifying the ambiguity within the business rules and handle accordingly

### Business Rules

- There are 3 types of tickets i.e. INFANT, CHILD, and ADULT.
- The ticket prices are based on the type of ticket (see table below).
- The ticket purchaser declares how many and what type of tickets they want to buy.
- Multiple tickets can be purchased at any given time.
- Only a maximum of 25 tickets that can be purchased at a time.
- An INFANT does not pay for a ticket and are not allocated a seat. They will be sitting on an ADULT lap.
- CHILD and INFANT tickets cannot be purchased without purchasing an ADULT ticket.

| Ticket Type | Price  |
|-------------|--------|
| INFANT      | £0     |
| CHILD       | £17.50 |
| ADULT       | £25.99 |

- There is an existing `PaymentService` responsible for taking payments.
- There is an existing `SeatReservationService` responsible for reserving seats.

### Constraints

- The `CinemaTicketsService` interface MUST not be modified.
- The code in `uk.gov.dwp.engineering.recruitment.domain` package MUST not be modified with the exception of
  BookingConfirmation, which may be extended in a non-breaking manner if you choose to do so.

### Assumptions

You can assume:

- All accounts with an id greater than zero are valid. They also have sufficient funds to pay for any number of tickets.
- The `PaymentService` implementation is an external provider with no defects.
- You do not need to worry about how the actual payment happens or integrating that service.
- The payment will always go through once a payment request has been made to the `PaymentService`.
- The `SeatReservationService` implementation is an external provider with no defects.
- You do not need to worry about how the seat reservation algorithm works or integrating that service.
- The seats will always be reserved once a reservation request has been made to the `SeatReservationService`.
- Any assumptions outside of the list above should be correctly and thoroughly documented.

### Your Task

Provide a working implementation of `CinemaTicketsServiceImpl` that:

- Considers the above objective, business rules, constraints & assumptions.
- Calculates the correct amount for the requested tickets and makes a payment request to the `PaymentService`.
- Calculates the correct number of seats to reserve and makes a seat reservation request to the
  `SeatReservationService`.
- Rejects any invalid ticket purchase requests.
- It is up to you to identify what should be deemed as an invalid purchase request.
- Please ensure you record your approach and any assumptions you make in the `README.md` of your final solution and
  within your submitted repository.
- Ensure appropriate error handling and testing is implemented for the ‘CinemaTicketsController’ referring to all
  business rules and rejected ticket requests.

## Solution

### Getting Started

#### Prerequisites

To install and modify this project you will need to have:

- [Java 21](https://www.java.com)
- [Maven 3.10](http://maven.apache.org)
- [Git 2.52.0](https://git-scm.com)

#### Testing

All tests have been written using [JUnit5](https://junit.org/junit5/). To run the tests, execute the _Maven_ `test`
phase.

```bash
mvn clean test
```

Code coverage is measured by [Pitest](https://pitest.org) and set to a minimum of 100%.

Mutation testing is provided by [Pitest](https://pitest.org). Mutation testing gauges the quality of testing by creating
random mutations that should cause tests to fail. This ensures that your tests are working as expected. The mutation
threshold is currently set to 100%.

The easiest way to generate reports is to execute the _Maven_ `verify` phase since _Pitest_ has been bound to this
phase.

```bash
mvn clean verify
```

Reports can be found in the `/target/` directory.

#### Formatting

[Spotless](https://github.com/diffplug/spotless) has been used to standardise formatting of Java files in the project.
`mvn spotless:apply` can be used to automatically format Java files. Correctly formatted files is enforced as part of a
`mvn verify`.

#### Linting

[Apache Maven PMD](https://maven.apache.org/plugins/maven-pmd-plugin/) has been used to lint Java source code
and [Spotbugs](https://spotbugs.github.io) has been used for bytecode analysis. Both generate reports as part of a
`mvn verify` and any failures result in a build failure.

#### Starting the service

`mvn spring-boot:run` to start the service.

### Assumptions

- A `TicketRequest` is only valid if the `TicketType` is not `null` and the `ticketCount` is greater than `0`.
- An ADULT cannot have more than one INFANT sitting on their lap.
- INFANT tickets count towards the maximum of 25 tickets.
- Multiple CHILD tickets can be purchased with one ADULT.
- Multiple `TicketRequest` for the same `TicketType` are added together.
- Error handling was added to `CinemaTicketsServiceImpl.java` in spite of the provided assumption stating external
  services are free from defects as this was identified as a gap in a previous technical exercise.
- Payment happens before seat reservation.
- Payment can be taken and a seat reservation can fail (Ideally payment would be authorised prior to reserving seats, if
  seat reservation was successful then the funds will be taken, otherwise authorisation is voided and the customer is
  never charged).

### Implementation

- GitLab, cspell, markdownlint and `catalog-info.yaml` were removed as they are not implemented in this
  repository.
- Prices are hard coded in `CinemaTicketsServiceImpl.java`. If this service was expected to be white labeled and
  deployed for multiple cinema clients then providing prices as configuration would have been appropriate.
- Prices were not added to the `TicketType` enum due to the limitation of not changing the code in
  `uk.gov.dwp.engineering.recruitment.domain`.
- A one argument constructor was added to `BookingConfirmation` to satisfy adding the required changes in a
  "non-breaking manner". Ideally this domain class would have been updated in a breaking manner with validation in its
  constructor to ensure `seatCount` and `totalCost` were valid.
- SpotBugs identified that `Booking.ticketRequests()` exposes its internal array. Had I been able to change this source
  code, I would have implemented `Arrays.copyOf()` in both the constructor and getter of the record.
- Updated the Open API specification to match the provided source code.
- Ideally I would have introduced `DTO` objects to be used in the controller layer. These would then have mapped to the
  domain objects which would have validated themselves in the constructor. This would have meant that the domain object
  could never be in an invalid state, and the service layer would purely call methods on the domain and pass results to
  the infrastructure layer (external services).
- `CinemaTicketsControllerTest.java` is a `@WebMvcTest` ensuring that exceptions are successfully handled by
  `@ExceptionHandler` methods and then mapped to the appropriate `Problem` response.
- `CinemaTicketsIntegrationTest` exists to test end to end functionality delivered so far. External services have been
  mocked (implementations haven't been written) and tests written to cover the business rules. Ideally these tests would
  be rewritten into component tests, asserting against a running containerised service using WireMock to stub out
  external dependencies.

### What I'd do next

- I'd introduce DTOs to be used by the controller layer and refactor validation into the domain objects.
- Introduce Hexagonal Architecture to standardise concern boundaries and ensure separation of domain and infrastructure
  code.
- Containerise the service, using a Distroless base image to reduce attack surface, and
  add [Trivy](https://trivy.dev), [dive](https://github.com/wagoodman/dive),
  and [Container Structure Tests](https://github.com/googlecontainertools/container-structure-test) to my CI pipeline to
  enforce vulnerability scanning, keeping image sizes small, and ensuring container contents match our expectations.
- Create component tests against the container with WireMock.
- Introduce payment authorisation then capture to handle any errors with seat reservation.
- Once payment and seat reservation clients have been implemented, I would introduce contract testing
  with [Pact](https://pact.io) for this component and the providers.

### Conventional Commits

This project uses the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification for commit
messages. The specification provides a simple rule set for creating commit messages, documenting features, fixes, and
breaking changes in commit messages.

A [pre-commit](https://pre-commit.com) [configuration file](.pre-commit-config.yaml) has been provided to automate
commit linting. Ensure that *pre-commit* has been [installed](https://pre-commit.com/#install) and
execute...

```shell
pre-commit install
```

...to add a commit [Git hook](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks) to your local machine.

An automated pipeline job has been [configured](.github/workflows/build.yaml) to lint commit messages on a push.

### GitHub Actions

A CI/CD pipeline has been created using [GitHub Actions](https://github.com/features/actions) to automate tasks such as
linting and testing.

#### Build Workflow

The [build](./.github/workflows/build.yaml) workflow handles integration tasks. This workflow consists of two jobs,
`Git` and `Maven`, that run in parallel. This workflow is triggered on a push to a branch.

##### Git

This job automates tasks relating to repository linting and enforcing best practices.

##### Maven

This job automates `Maven` specific tasks.
