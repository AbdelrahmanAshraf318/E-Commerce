sequenceDiagram
    autonumber

    participant User
    participant Gateway as API Gateway
    participant Cart as Cart Service
    participant Order as Order Service
    participant Kafka
    participant Inventory as Inventory Service
    participant Payment as Payment Service
    participant Ship as Shipping Service
    participant Notify as Notification Service

    User->>Gateway: POST /checkout
    Gateway->>Cart: GET /cart/{userId}
    Cart-->>Gateway: Cart Items

    Gateway->>Order: createOrder(cartItems)
    Order->>Order: Save Order (PENDING)
    Order->>Kafka: publish(order.created)

    Kafka-->>Inventory: consume(order.created)
    Inventory->>Inventory: reserveStock()
    Inventory->>Kafka: publish(inventory.reserved or failed)

    Kafka-->>Payment: consume(inventory.reserved)
    Payment->>Payment: process payment
    Payment->>Kafka: publish(payment.succeeded or failed)

    Kafka-->>Order: consume(payment + inventory events)
    Order->>Order: update order state (CONFIRMED/FAILED)
    Order->>Kafka: publish(order.confirmed or order.failed)

    Kafka-->>Ship: consume(order.confirmed)
    Ship->>Ship: create shipment
    Ship->>Notify: send email notification

    Notify->>User: Order Confirmation Email
