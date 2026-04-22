# Analisis RabbitMQ pada Project Order Management

## Jawaban singkat

RabbitMQ dipakai sebagai penghubung asinkron antar 3 service:

- `order-management` (`8080`) sebagai producer utama
- `inventory-service` (`8081`) sebagai consumer untuk stok
- `shipping-service` (`8082`) sebagai consumer untuk pengiriman

RabbitMQ paling jelas terlihat saat 6 momen ini:

1. membuat produk
2. mengubah produk
3. menghapus produk
4. membuat order
5. membatalkan order
6. mengubah shipment menjadi `SHIPPED`

Untuk demo, bagian terbaik yang ditunjukkan adalah:

- RabbitMQ Management UI: exchange `order_exchange` dan queue-queue yang ter-binding
- log dari masing-masing service
- perubahan data hasil konsumsi event:
  - inventory berubah
  - shipment dibuat / dibatalkan
  - status order berubah menjadi `SHIPPED`

## Konfigurasi inti RabbitMQ

Semua service mengarah ke RabbitMQ lokal pada port `5673`.

- `order-management/src/main/resources/application.properties:18-24`
- `inventory-service/src/main/resources/application.properties:15-17`
- `shipping-service/src/main/resources/application.properties:16-20`

Konfigurasi utama producer ada di `order-management/src/main/java/com/example/ordermanagement/config/RabbitMQConfig.java`.

- Exchange: `order_exchange` pada baris `17`, bean di baris `41-43`
- Routing key order baru: `order_routing_key` pada baris `18`
- Routing key stok berhasil di-reserve: `stock_reserved_key`
- Routing key reserve gagal: `order_reserve_failed_key`
- Routing key cancel: `order_cancel_key` pada baris `19`
- Routing key sinkronisasi produk: `product_sync_key` pada baris `20`
- Routing key hapus produk: `product_delete_key`
- Queue yang dideklarasikan langsung di service ini:
  - `order_stock_queue` pada baris `15`, bean `23-26`
  - `order_cancel_queue` pada baris `16`, bean `29-32`
  - `product_inventory_queue` pada baris `36-37`
  - `product_inventory_delete_queue`

Service lain tidak mendefinisikan exchange/queue lewat bean terpisah. Mereka mendengarkan RabbitMQ lewat `@RabbitListener` dengan `@QueueBinding`, sehingga queue lain muncul dari sisi consumer.

## RabbitMQ dipakai di mana dan saat apa

### 1. Saat membuat atau mengubah produk

Trigger:

- Endpoint `POST /api/products`
- Endpoint `PUT /api/products/{id}`
- File: `order-management/src/main/java/com/example/ordermanagement/controller/ProductController.java`

Alur:

1. Produk disimpan ke database order-management.
2. `ProductService.createProduct(...)` atau `ProductService.updateProduct(...)` mengirim event ke RabbitMQ.
3. `inventory-service` menerima event itu dan menyinkronkan data inventory.

Producer:

- `order-management/src/main/java/com/example/ordermanagement/service/ProductService.java`
- exchange: `order_exchange`
- routing key: `product_sync_key`

Consumer:

- `inventory-service/src/main/java/com/example/inventoryservice/listener/OrderEventListener.java`
- Queue: `product_inventory_queue`
- Aksi bisnis: `inventoryService.syncProductStock(...)`

Efek yang bisa ditunjukkan saat demo:

- jika produk baru dibuat:
  - produk baru muncul di `order-management`
  - baris inventory otomatis muncul di `inventory-service`
- jika produk diubah:
  - data produk berubah di `order-management`
  - total stok ikut disinkronkan ke `inventory-service` dengan tetap menjaga `reservedStock` yang sudah ada

### 2. Saat menghapus produk

Trigger:

- Endpoint `DELETE /api/products/{id}`
- File: `order-management/src/main/java/com/example/ordermanagement/controller/ProductController.java`

Alur:

1. `order-management` mengecek apakah product sudah pernah dipakai pada `order_items`.
2. Jika belum pernah dipakai, product dihapus dari database order-management.
3. `ProductService.deleteProduct(...)` mengirim event ke RabbitMQ.
4. `inventory-service` menerima event itu dan menghapus row inventory terkait.

Producer:

- `order-management/src/main/java/com/example/ordermanagement/service/ProductService.java`
- exchange: `order_exchange`
- routing key: `product_delete_key`

Consumer:

- `inventory-service/src/main/java/com/example/inventoryservice/listener/OrderEventListener.java`
- Queue: `product_inventory_delete_queue`
- Aksi bisnis: `inventoryService.deleteInventory(...)`

Efek yang bisa ditunjukkan saat demo:

- jika product belum pernah dipakai order:
  - product hilang dari `order-management`
  - inventory terkait ikut hilang
- jika product sudah pernah dipakai order:
  - request delete ditolak

### 3. Saat membuat order

Trigger:

- Endpoint `POST /api/orders?customerId={id}`
- File: `order-management/src/main/java/com/example/ordermanagement/controller/OrderController.java:20-24`

Alur:

1. Order disimpan ke database order-management.
2. Untuk setiap item order, `OrderService.createOrder(...)` mengirim event awal ke RabbitMQ.
3. `inventory-service` menerima event itu dan mencoba reserve stok.
4. Jika reserve sukses, `inventory-service` mengirim event `stock_reserved_key` ke `shipping-service`.
5. Jika reserve gagal, `inventory-service` mengirim event `order_reserve_failed_key` ke `order-management`.
6. `order-management` menandai order menjadi `FAILED` lalu mengirim event rollback.

Producer:

- `order-management/src/main/java/com/example/ordermanagement/service/OrderService.java:67-76`
- Pengiriman event ada di baris `73`:
  - exchange: `order_exchange`
  - routing key: `order_routing_key`

Consumer 1:

- `inventory-service/src/main/java/com/example/inventoryservice/listener/OrderEventListener.java:35-50`
- Queue: `order_stock_queue`
- Aksi bisnis: `inventoryService.reserveStock(...)`

Consumer lanjutan saat reserve sukses:

- `shipping-service/src/main/java/com/example/shippingservice/listener/OrderEventListener.java`
- Queue: `order_shipping_queue`
- Aksi bisnis: `shipmentService.createShipment(...)`

Consumer lanjutan saat reserve gagal:

- `order-management/src/main/java/com/example/ordermanagement/listener/InventoryEventListener.java`
- Queue: `order_reserve_failed_queue`
- Aksi bisnis: `orderService.markAsFailedFromInventory(...)`

Efek yang bisa ditunjukkan saat demo:

- di inventory:
  - `availableStock` turun
  - `reservedStock` naik
- di shipping:
  - shipment baru otomatis tercipta setelah reserve stok sukses
- jika reserve gagal:
  - order berubah menjadi `FAILED`
  - event cancel otomatis dipublish untuk rollback

Ini tetap menjadi flow demo RabbitMQ paling kuat, tetapi sekarang bentuknya chain event:

1. `order-management -> inventory`
2. `inventory -> shipping` atau `inventory -> order-management`

### 4. Saat membatalkan order

Trigger:

- Endpoint `POST /api/orders/{id}/cancel`
- File: `order-management/src/main/java/com/example/ordermanagement/controller/OrderController.java:49-53`

Alur:

1. Status order diubah menjadi `CANCELLED`.
2. Untuk setiap item order, `OrderService.cancelOrder(...)` mengirim event cancel ke RabbitMQ.
3. Event cancel dikonsumsi oleh:
   - inventory untuk melepas stok cadangan
   - shipping untuk membatalkan shipment yang masih `PENDING`

Producer:

- `order-management/src/main/java/com/example/ordermanagement/service/OrderService.java:107-129`
- Pengiriman event ada di baris `121-124`:
  - exchange: `order_exchange`
  - routing key: `order_cancel_key`

Consumer 1:

- `inventory-service/src/main/java/com/example/inventoryservice/listener/OrderEventListener.java:53-66`
- Queue: `order_cancel_queue`
- Aksi bisnis: `inventoryService.releaseStock(...)`

Consumer 2:

- `shipping-service/src/main/java/com/example/shippingservice/listener/OrderEventListener.java:36-49`
- Queue: `order_shipping_cancel_queue`
- Aksi bisnis: `shipmentService.cancelShipment(...)`

Efek yang bisa ditunjukkan saat demo:

- status order menjadi `CANCELLED`
- reserved stock turun, available stock naik lagi
- shipment yang masih `PENDING` menjadi `CANCELLED`

### 5. Saat shipment diubah menjadi SHIPPED

Trigger:

- Endpoint `PUT /api/shipments/{id}/status?status=SHIPPED`
- File: `shipping-service/src/main/java/com/example/shippingservice/controller/ShipmentController.java:41-44`

Alur:

1. Shipping service mengubah status shipment menjadi `SHIPPED`.
2. `ShipmentService.updateShipmentStatus(...)` mengirim event ke RabbitMQ hanya pada transisi valid ke `SHIPPED`.
3. Event itu dikonsumsi oleh:
   - order-management untuk update status order menjadi `SHIPPED`
   - inventory-service untuk finalize stok

Producer:

- `shipping-service/src/main/java/com/example/shippingservice/service/ShipmentService.java`
- exchange: `order_exchange`
- routing key: `order_shipped_key`

Consumer 1:

- `order-management/src/main/java/com/example/ordermanagement/listener/ShippingEventListener.java`
- Queue: `order_status_queue`
- Aksi bisnis: `orderService.markAsShippedFromShipping(...)`

Consumer 2:

- `inventory-service/src/main/java/com/example/inventoryservice/listener/OrderEventListener.java`
- Queue: `order_shipped_inventory_queue`
- Aksi bisnis: `inventoryService.finalizeStock(...)`

Efek yang bisa ditunjukkan saat demo:

- shipment status jadi `SHIPPED`
- order status ikut menjadi `SHIPPED`
- reserved stock inventory berkurang saat stok difinalisasi

## Ringkasan queue, routing key, producer, consumer

| Routing Key | Queue | Producer | Consumer | Kapan dipakai |
| --- | --- | --- | --- | --- |
| `product_sync_key` | `product_inventory_queue` | `order-management` | `inventory-service` | saat create atau update product |
| `product_delete_key` | `product_inventory_delete_queue` | `order-management` | `inventory-service` | saat delete product |
| `order_routing_key` | `order_stock_queue` | `order-management` | `inventory-service` | saat create order |
| `stock_reserved_key` | `order_shipping_queue` | `inventory-service` | `shipping-service` | saat reserve stok sukses |
| `order_reserve_failed_key` | `order_reserve_failed_queue` | `inventory-service` | `order-management` | saat reserve stok gagal |
| `order_cancel_key` | `order_cancel_queue` | `order-management` | `inventory-service` | saat cancel order |
| `order_cancel_key` | `order_shipping_cancel_queue` | `order-management` | `shipping-service` | saat cancel order |
| `order_shipped_key` | `order_status_queue` | `shipping-service` | `order-management` | saat shipment jadi `SHIPPED` |
| `order_shipped_key` | `order_shipped_inventory_queue` | `shipping-service` | `inventory-service` | saat shipment jadi `SHIPPED` |

## Bagian mana yang ditunjukkan saat demo

Jika ingin benar-benar menunjukkan RabbitMQ, fokuskan demo ke 3 layer ini:

### A. RabbitMQ Management UI

Jalankan broker seperti di dokumentasi project:

```bash
docker run -d --name rabbitmq -p 5673:5672 -p 15673:15672 rabbitmq:3-management
```

Lalu buka:

- `http://localhost:15673`
- username: `guest`
- password: `guest`

Yang perlu ditunjukkan:

1. tab `Exchanges`:
   - ada `order_exchange`
2. tab `Queues`:
   - `product_inventory_queue`
   - `product_inventory_delete_queue`
   - `order_stock_queue`
   - `order_reserve_failed_queue`
   - `order_shipping_queue`
   - `order_cancel_queue`
   - `order_shipping_cancel_queue`
   - `order_status_queue`
   - `order_shipped_inventory_queue`
3. detail binding queue ke exchange:
   - routing key mana masuk ke queue mana

### B. Log service

Log yang relevan untuk presentasi:

- `order-management`
  - `Pesan Event dikirim ke RabbitMQ...`
  - `Pesan Cancel dikirim...`
  - `Menerima reserve gagal dari Inventory...`
  - `Menerima update status dari Shipping...`
- `inventory-service`
  - `Menerima pesan sinkronisasi produk...`
  - `Menerima pesan dari RabbitMQ! Memproses reserve stok...`
  - `Mengirim event stock reserved...`
  - `Mengirim event reserve failed...`
  - `Menerima pesan CANCEL...`
  - `Pesanan telah dikirim! Mengurangi reserved stock...`
- `shipping-service`
  - `Menerima pesanan baru! Memproses jadwal pengiriman...`
  - `Menerima pembatalan pesanan...`
  - `Pesanan DIKIRIM! Mengirim pesan finalize stok ke Inventory...`

### C. Data hasil konsumsi event

Setelah memicu event, tampilkan endpoint pengecekan:

- inventory:
  - `GET /api/inventory`
  - `GET /api/inventory/{productId}`
- shipping:
  - `GET /api/shipments`
  - `GET /api/shipments/{id}`
- order:
  - `GET /api/orders`
  - `GET /api/orders/{id}`

## Skenario demo yang paling disarankan

### Demo 1: Create order

Ini demo terbaik karena 1 request memicu rantai event lintas 3 service.

Langkah:

1. Pastikan RabbitMQ, MySQL, dan 3 service hidup.
2. Pastikan ada `customer` dan `product`.
3. Buka RabbitMQ Management UI.
4. Lakukan `POST /api/orders?customerId=...`.
5. Tunjukkan:
   - log producer di `order-management`
   - log consumer di `inventory-service`
   - log lanjutan `stock_reserved_key` atau `order_reserve_failed_key`
   - log consumer di `shipping-service`
   - inventory berubah
   - shipment baru muncul

Contoh body:

```json
[
  { "productId": 1, "quantity": 2 }
]
```

### Demo 2: Cancel order

Langkah:

1. Gunakan order yang baru dibuat.
2. Panggil `POST /api/orders/{id}/cancel`.
3. Tunjukkan:
   - order berubah ke `CANCELLED`
   - inventory me-release stok
   - shipment `PENDING` dibatalkan
4. Ulangi request yang sama sekali lagi untuk menunjukkan bahwa guard sekarang menolak repeat cancel.

### Demo 3: Shipment jadi SHIPPED

Langkah:

1. Ambil `shipmentId` dari `GET /api/shipments`.
2. Panggil `PUT /api/shipments/{id}/status?status=SHIPPED`.
3. Tunjukkan:
   - shipping mengirim event ke RabbitMQ
   - order-management menerima event dan update order
   - inventory finalize stok
4. Ulangi request `SHIPPED` yang sama untuk menunjukkan bahwa guard mencegah repeat finalize.

## Bagian yang bukan demo RabbitMQ

Beberapa endpoint ada, tetapi tidak menunjukkan penggunaan RabbitMQ secara utama karena langsung memanggil service lokal:

- `POST /api/shipments`
  - membuat shipment langsung via REST ke shipping service
  - file: `shipping-service/src/main/java/com/example/shippingservice/controller/ShipmentController.java:20-25`
- `POST /api/inventory/reserve`
  - reserve stok langsung tanpa lewat broker
  - file: `inventory-service/src/main/java/com/example/inventoryservice/controller/InventoryController.java:28-31`
- `POST /api/inventory/release`
  - release stok langsung tanpa broker
  - file: `inventory-service/src/main/java/com/example/inventoryservice/controller/InventoryController.java:34-37`
- `PUT /api/orders/{id}/status`
  - update status order langsung di order-management
  - file: `order-management/src/main/java/com/example/ordermanagement/controller/OrderController.java:39-46`

Kalau tujuan presentasi adalah menunjukkan event-driven architecture, jangan jadikan endpoint-endpoint ini sebagai demo utama.

## Tips agar RabbitMQ terlihat jelas saat presentasi

Ada satu hal penting: pada kondisi normal, message bisa habis sangat cepat karena langsung dikonsumsi listener. Jadi queue kadang terlihat `0` walaupun RabbitMQ memang bekerja.

Agar lebih meyakinkan saat demo:

1. tampilkan dulu struktur exchange dan queue di RabbitMQ UI
2. lalu jalankan request dan perlihatkan log producer + consumer
3. lalu tunjukkan perubahan data
4. jika ingin queue benar-benar terlihat menumpuk, matikan sementara salah satu consumer, kirim event, lalu buka queue yang terkait

Contoh demo yang sangat jelas:

- matikan sementara `inventory-service`
- lakukan `POST /api/orders`
- buka queue `order_stock_queue` di RabbitMQ UI
- message akan tertahan di queue inventory
- hidupkan lagi `inventory-service`
- tunjukkan message diproses dan queue berkurang

## Kesimpulan

RabbitMQ di project ini bukan aksesoris, tetapi jalur utama koordinasi antar-service.

Bagian paling penting untuk ditunjukkan saat demo adalah:

1. `POST /api/orders` untuk chain event `order-management -> inventory -> shipping`
2. `POST /api/orders/{id}/cancel` untuk rollback lintas service
3. `PUT /api/shipments/{id}/status?status=SHIPPED` untuk event balik dari shipping ke order dan inventory
4. `DELETE /api/products/{id}` sebagai demo sinkronisasi penghapusan product ke inventory

Kalau harus memilih satu demo saja, pilih `create order`, karena di situ paling jelas terlihat konsep producer, exchange, routing key, queue, dan multi-consumer.
