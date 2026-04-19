# Struktur Project

Berikut struktur project berdasarkan folder dan file yang ada saat ini.
```text
.
|-- .vscode
|   `-- settings.json
|-- inventory-service
|   |-- .gitattributes
|   |-- .gitignore
|   |-- .mvn
|   |   `-- wrapper
|   |       `-- maven-wrapper.properties
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- pom.xml
|   `-- src
|       |-- main
|       |   |-- java
|       |   |   `-- com
|       |   |       `-- example
|       |   |           `-- inventoryservice
|       |   |               |-- InventoryServiceApplication.java
|       |   |               |-- config
|       |   |               |   `-- RabbitMQConfig.java
|       |   |               |-- controller
|       |   |               |   `-- InventoryController.java
|       |   |               |-- dto
|       |   |               |   |-- InventoryRequest.java
|       |   |               |   `-- OrderEvent.java
|       |   |               |-- entity
|       |   |               |   `-- Inventory.java
|       |   |               |-- listener
|       |   |               |   `-- OrderEventListener.java
|       |   |               |-- repository
|       |   |               |   `-- InventoryRepository.java
|       |   |               `-- service
|       |   |                   `-- InventoryService.java
|       |   `-- resources
|       |       `-- application.properties
|       `-- test
|           `-- java
|               `-- com
|                   `-- example
|                       `-- inventoryservice
|                           `-- InventoryServiceApplicationTests.java
|-- order-management
|   |-- .gitattributes
|   |-- .gitignore
|   |-- .mvn
|   |   `-- wrapper
|   |       `-- maven-wrapper.properties
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- pom.xml
|   `-- src
|       |-- main
|       |   |-- java
|       |   |   `-- com
|       |   |       `-- example
|       |   |           `-- ordermanagement
|       |   |               |-- DataInitializer.java
|       |   |               |-- OrderManagementApplication.java
|       |   |               |-- config
|       |   |               |   `-- RabbitMQConfig.java
|       |   |               |-- controller
|       |   |               |   |-- CategoryController.java
|       |   |               |   |-- CustomerController.java
|       |   |               |   |-- OrderController.java
|       |   |               |   `-- ProductController.java
|       |   |               |-- dto
|       |   |               |   |-- CustomerRequest.java
|       |   |               |   |-- OrderEvent.java
|       |   |               |   `-- ProductRequest.java
|       |   |               |-- entity
|       |   |               |   |-- Category.java
|       |   |               |   |-- Customer.java
|       |   |               |   |-- Order.java
|       |   |               |   |-- OrderItem.java
|       |   |               |   `-- Product.java
|       |   |               |-- exception
|       |   |               |   `-- GlobalExceptionHandler.java
|       |   |               |-- listener
|       |   |               |   `-- ShippingEventListener.java
|       |   |               |-- repository
|       |   |               |   |-- CategoryRepository.java
|       |   |               |   |-- CustomerRepository.java
|       |   |               |   |-- OrderRepository.java
|       |   |               |   `-- ProductRepository.java
|       |   |               `-- service
|       |   |                   |-- CategoryService.java
|       |   |                   |-- OrderService.java
|       |   |                   `-- ProductService.java
|       |   `-- resources
|       |       `-- application.properties
|       `-- test
|           `-- java
|               `-- com
|                   `-- example
|                       `-- ordermanagement
|                           `-- OrderManagementApplicationTests.java
`-- shipping-service
    |-- .gitattributes
    |-- .gitignore
    |-- .mvn
    |   `-- wrapper
    |       `-- maven-wrapper.properties
    |-- mvnw
    |-- mvnw.cmd
    |-- pom.xml
    `-- src
        |-- main
        |   |-- java
        |   |   `-- com
        |   |       `-- example
        |   |           `-- shippingservice
        |   |               |-- ShippingServiceApplication.java
        |   |               |-- config
        |   |               |   `-- RabbitMQConfig.java
        |   |               |-- controller
        |   |               |   `-- ShipmentController.java
        |   |               |-- dto
        |   |               |   |-- OrderEvent.java
        |   |               |   `-- ShipmentRequest.java
        |   |               |-- entity
        |   |               |   `-- Shipment.java
        |   |               |-- listener
        |   |               |   `-- OrderEventListener.java
        |   |               |-- repository
        |   |               |   `-- ShipmentRepository.java
        |   |               `-- service
        |   |                   `-- ShipmentService.java
        |   `-- resources
        |       `-- application.properties
        `-- test
            `-- java
                `-- com
                    `-- example
                        `-- shippingservice
                            `-- ShippingServiceApplicationTests.java
```
