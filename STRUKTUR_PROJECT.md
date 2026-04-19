# Struktur Project

Project ini disusun menggunakan pendekatan microservices dengan tiga service utama, yaitu `order-management`, `inventory-service`, dan `shipping-service`. Masing-masing service memiliki tanggung jawab yang berbeda, sehingga alur pemesanan, pengelolaan stok, dan proses pengiriman dapat dipisahkan dengan lebih jelas. Struktur ini membantu pengembangan aplikasi menjadi lebih modular, mudah dipahami, dan lebih sederhana saat dilakukan maintenance maupun pengujian.

Secara umum, setiap service memiliki pola folder yang serupa, seperti `config`, `controller`, `dto`, `entity`, `repository`, `service`, serta `resources` dan `test`. Pola ini menunjukkan bahwa setiap service dibangun dengan arsitektur Spring Boot yang terstruktur, sehingga pembagian logika bisnis, akses data, konfigurasi, dan endpoint API menjadi lebih rapi. Pada dokumentasi struktur berikut, file `.md` tidak dimasukkan agar tampilan tetap fokus pada folder dan file inti project.
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
