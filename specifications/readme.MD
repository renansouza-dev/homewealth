# specifications

This module holds all OpenAPI specifications for the **HomeWealth** platform. It acts as the single source of truth for API contracts — no implementation lives here, only specs and the tooling to bundle them.

## Available Specifications

### Transactions API (`transactions-api.yaml`)

Manages investment transactions for a given portfolio. Base URL: `http://localhost:8081/api/v1`.

| Method   | Path                            | Description                                   |
|----------|---------------------------------|-----------------------------------------------|
| `POST`   | `/transactions`                 | Create a new transaction                      |
| `GET`    | `/transactions`                 | List transactions with filters and pagination |
| `DELETE` | `/transactions/{transactionId}` | Remove a transaction by ID                    |

All endpoints require a Bearer JWT token.

**Key schemas:**

- `TransactionRequest` — requires `brokerId`, `portfolioId`, `assetId`, `operationDate`, `operationType`, `quantity`, and `unitPrice`
- `TransactionResponse` — includes calculated fields: `fees` and `netValue`
- `OperationType` — enum: `BUY` or `SELL`

## Build

The build uses `openapi-generator-maven-plugin` to bundle all `$ref` components into a single self-contained file:

```
target/generated-resources/openapi/transactions-api-bundle.yaml
```

This bundle is what the `transactions` module consumes for code generation.

```bash
./mvnw generate-resources
```

## How It Fits in the Monorepo

```
homewealth/
├── specifications/   ← you are here (API contracts)
└── transactions/     ← consumes the bundled spec to generate interfaces
```

The `transactions` module copies `transactions-api-bundle.yaml` during its `initialize` phase and uses it as input for Spring MVC interface generation.