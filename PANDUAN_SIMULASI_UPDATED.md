# Panduan Simulasi dan Demo Order Management

Dokumen ini adalah versi susun ulang dari `PANDUAN_SIMULASI.md` untuk dua tujuan yang berbeda:

1. simulasi end-to-end alur bisnis
2. demo presentasi, terutama bagian RabbitMQ

Di akhir dokumen ini juga ada audit singkat: apakah simulasi yang ada sudah mencakup seluruh fitur project atau belum.

## Ringkasan singkat

Jika tujuan Anda adalah menunjukkan inti sistem, jalankan 4 flow ini:

1. buat kategori dan customer
2. buat produk
3. buat order
4. cancel order atau ubah shipment menjadi `SHIPPED`

Dengan 4 flow itu, Anda sudah menunjukkan:

- master data dasar
- integrasi antar 3 service
- event-driven architecture
- penggunaan RabbitMQ
- perubahan stok
- pembuatan shipment
- rollback saat cancel
- sinkronisasi status saat shipped

Namun, itu belum berarti seluruh endpoint project sudah diuji. Untuk cakupan penuh, tetap perlu checklist fitur tambahan di bagian akhir dokumen.

---

## 1. Cakupan dan tujuan

Project ini punya 3 service:

- `order-management` di `8080`
- `inventory-service` di `8081`
- `shipping-service` di `8082`

RabbitMQ dipakai untuk alur berikut:

- produk baru -> sinkronisasi ke inventory
- order baru -> reserve stok dulu di inventory
- reserve stok sukses -> shipping membuat shipment
- reserve stok gagal -> order ditandai `FAILED` lalu rollback otomatis
- order dibatalkan -> release stok + cancel shipment
- shipment `SHIPPED` -> finalize stok + update status order

Jadi, jika ingin demo RabbitMQ, jangan fokus ke endpoint manual semata. Fokus utamanya harus pada flow yang mem-publish dan mem-consume event.

---

## 2. Setup awal

### 2.1 Database MySQL

Buat tiga database:

```sql
CREATE DATABASE order_management_db;
CREATE DATABASE inventory_db;
CREATE DATABASE shipping_db;
```

Pastikan kredensial MySQL pada file berikut sesuai dengan environment Anda:

- `order-management/src/main/resources/application.properties`
- `inventory-service/src/main/resources/application.properties`
- `shipping-service/src/main/resources/application.properties`

### 2.2 RabbitMQ

Jalankan RabbitMQ Management:

```bash
docker run -d --name rabbitmq -p 5673:5672 -p 15673:15672 rabbitmq:3-management
```

Gunakan:

- AMQP: `localhost:5673`
- Dashboard: `http://localhost:15673`
- username: `guest`
- password: `guest`

### 2.3 Menjalankan service

PowerShell:

```powershell
cd .\order-management
.\mvnw.cmd spring-boot:run
```

```powershell
cd .\inventory-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd .\shipping-service
.\mvnw.cmd spring-boot:run
```

Catatan penting:

- `DataInitializer` tidak aktif karena anotasi `@Component` sedang dikomentari.
- Artinya, data master awal tidak otomatis dibuat. Anda memang perlu membuat category, customer, dan product sendiri saat simulasi.

### 2.4 Verifikasi awal

Pastikan:

1. MySQL aktif
2. RabbitMQ aktif
3. ketiga service berhasil startup

Verifikasi cepat:

- `GET http://localhost:8080/api/categories`
- `GET http://localhost:8080/api/customers`
- `GET http://localhost:8080/api/products`
- `GET http://localhost:8081/api/inventory`
- `GET http://localhost:8082/api/shipments`

---

## 3. Peta demo RabbitMQ

Sebelum mulai, pahami dulu event yang akan muncul saat simulasi:

| Event bisnis | Producer | Routing key | Consumer | Dampak |
| --- | --- | --- | --- | --- |
| Product Created | `order-management` | `product_sync_key` | `inventory-service` | membuat inventory awal |
| Order Created | `order-management` | `order_routing_key` | `inventory-service` | memulai reserve stok |
| Stock Reserved | `inventory-service` | `stock_reserved_key` | `shipping-service` | membuat shipment setelah reserve sukses |
| Reserve Failed | `inventory-service` | `order_reserve_failed_key` | `order-management` | menandai order `FAILED` dan memicu rollback |
| Order Cancelled | `order-management` | `order_cancel_key` | `inventory-service`, `shipping-service` | release stok dan cancel shipment |
| Shipment Shipped | `shipping-service` | `order_shipped_key` | `order-management`, `inventory-service` | update status order dan finalize stok |

Yang perlu ditunjukkan di RabbitMQ Management UI:

- exchange `order_exchange`
- queue:
  - `product_inventory_queue`
  - `order_stock_queue`
  - `order_reserve_failed_queue`
  - `order_shipping_queue`
  - `order_cancel_queue`
  - `order_shipping_cancel_queue`
  - `order_status_queue`
  - `order_shipped_inventory_queue`

Jika message terlalu cepat habis dikonsumsi dan queue terlihat `0`, itu normal. Untuk demo yang lebih visual, Anda bisa mematikan sementara salah satu consumer, kirim event, lalu tunjukkan message tertahan di queue terkait.

---

## 4. Simulasi inti end-to-end

Bagian ini adalah simulasi utama yang paling layak dijadikan alur presentasi.

### Langkah 1: Buat kategori

Endpoint:

- `POST http://localhost:8080/api/categories`

Body:

```json
{
  "name": "Electronics",
  "description": "Kategori perangkat elektronik"
}
```

Verifikasi:

- `GET http://localhost:8080/api/categories`

### Langkah 2: Buat customer

Endpoint:

- `POST http://localhost:8080/api/customers`

Body:

```json
{
  "name": "Bram",
  "email": "bram@gmail.com",
  "address": "Jakarta Timur"
}
```

Verifikasi:

- `GET http://localhost:8080/api/customers`

### Langkah 3: Buat produk

Endpoint:

- `POST http://localhost:8080/api/products`

Body:

```json
{
  "name": "Laptop",
  "price": 10000000,
  "stock": 50,
  "categoryId": 1
}
```

Apa yang terjadi:

1. produk disimpan di `order-management`
2. `order-management` mengirim event ke RabbitMQ
3. `inventory-service` menerima event dan membuat inventory awal

Verifikasi:

- `GET http://localhost:8080/api/products`
- `GET http://localhost:8081/api/inventory/1`

Hal yang ditunjukkan saat demo:

- log `order-management` saat publish event
- log `inventory-service` saat menerima `product_sync_key`
- queue `product_inventory_queue` di RabbitMQ UI

### Langkah 4: Buat order

Endpoint:

- `POST http://localhost:8080/api/orders?customerId=1`

Body:

```json
[
  {
    "productId": 1,
    "quantity": 5
  }
]
```

Apa yang terjadi:

1. order tersimpan di `order-management`
2. event dipublish ke RabbitMQ
3. `inventory-service` me-reserve stok
4. jika reserve sukses, `inventory-service` publish `stock_reserved_key`
5. `shipping-service` membuat shipment dengan status `PENDING`
6. jika reserve gagal, `inventory-service` publish `order_reserve_failed_key`, order ditandai `FAILED`, lalu rollback otomatis dipicu

Verifikasi:

- `GET http://localhost:8080/api/orders`
- `GET http://localhost:8081/api/inventory/1`
- `GET http://localhost:8082/api/shipments`

Expected result:

- `availableStock` turun `5`
- `reservedStock` naik `5`
- shipment baru muncul dengan status `PENDING`

Ini tetap menjadi demo RabbitMQ terbaik, tetapi sekarang bentuknya chain event:

1. `order-management -> inventory`
2. `inventory -> shipping`

### Langkah 5A: Cancel order

Endpoint:

- `POST http://localhost:8080/api/orders/1/cancel`

Apa yang terjadi:

1. status order menjadi `CANCELLED`
2. event cancel dikirim ke RabbitMQ
3. inventory me-release stok
4. shipping membatalkan shipment yang masih `PENDING`

Verifikasi:

- `GET http://localhost:8080/api/orders/1`
- `GET http://localhost:8081/api/inventory/1`
- `GET http://localhost:8082/api/shipments`

Expected result:

- `availableStock` naik lagi
- `reservedStock` turun lagi
- shipment `PENDING` berubah menjadi `CANCELLED`

### Langkah 5B: Ubah shipment menjadi SHIPPED

Langkah ini sebaiknya dijalankan pada order lain yang belum dicancel, atau ulangi lagi langkah 4 untuk membuat order baru.

Endpoint:

- `PUT http://localhost:8082/api/shipments/2/status?status=SHIPPED`

Apa yang terjadi:

1. `shipping-service` update shipment ke `SHIPPED`
2. `shipping-service` publish event `order_shipped_key`
3. `order-management` menerima event dan update status order jadi `SHIPPED`
4. `inventory-service` finalize stok

Verifikasi:

- `GET http://localhost:8082/api/shipments/2`
- `GET http://localhost:8080/api/orders/2`
- `GET http://localhost:8081/api/inventory/1`

Expected result:

- shipment berstatus `SHIPPED`
- order ikut berstatus `SHIPPED`
- `reservedStock` berkurang saat stok difinalisasi

---

## 5. Format demo presentasi yang disarankan

Jika tujuan Anda adalah demo singkat 3-7 menit, pakai urutan ini:

1. tunjukkan arsitektur singkat: 3 service + RabbitMQ
2. buka RabbitMQ Management UI
3. buat produk
4. tunjukkan inventory otomatis terbuat
5. buat order
6. tunjukkan stok di-reserve dan shipment otomatis muncul
7. pilih salah satu:
   - cancel order untuk menunjukkan rollback
   - shipped untuk menunjukkan sinkronisasi balik

Kalau ingin menunjukkan RabbitMQ lebih jelas:

1. buka tab `Exchanges`
2. buka `order_exchange`
3. buka tab `Queues`
4. tunjukkan binding queue
5. jalankan request
6. tunjukkan log producer dan consumer

Kalau ingin menunjukkan message benar-benar sempat antre:

1. matikan sementara `inventory-service`
2. lakukan `POST /api/orders`
3. buka queue `order_stock_queue`
4. hidupkan kembali `inventory-service`
5. tunjukkan queue kembali diproses

---

## 6. Cek fitur tambahan di luar flow inti

Bagian ini dipakai jika Anda ingin membuktikan bahwa seluruh fitur REST yang diekspos controller juga bisa diakses, bukan hanya flow integrasi.

### 6.1 Order Management Service (`8080`)

#### Category

- `GET /api/categories`
- `GET /api/categories/{id}`
- `GET /api/categories/{id}/products`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`

Contoh update category:

```json
{
  "name": "Electronics Updated",
  "description": "Kategori elektronik revisi"
}
```

#### Customer

- `GET /api/customers`
- `GET /api/customers/{id}`
- `GET /api/customers/{id}/orders`
- `POST /api/customers`
- `PUT /api/customers/{id}`
- `DELETE /api/customers/{id}`

Contoh update customer:

```json
{
  "name": "Bram Update",
  "email": "bram.update@gmail.com",
  "address": "Bekasi"
}
```

#### Product

- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`

Catatan:

- endpoint update product sudah dipulihkan agar tetap memenuhi CRUD Product
- saat `PUT /api/products/{id}` dipanggil, total stok terbaru ikut disinkronkan ke `inventory-service`
- `DELETE /api/products/{id}` sekarang ikut menghapus inventory terkait, tetapi hanya aman untuk product yang belum pernah dipakai order

Contoh update product:

```json
{
  "name": "Laptop Pro",
  "price": 12000000,
  "stock": 70,
  "categoryId": 1
}
```

#### Order

- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders?customerId={id}`
- `PUT /api/orders/{id}/status?status={STATUS}`
- `POST /api/orders/{id}/cancel`

Catatan:

- `PUT /api/orders/{id}/status` adalah update langsung di service order.
- Endpoint ini bukan demo RabbitMQ utama, karena tidak publish event.
- Endpoint ini sekarang diberi guard agar status final seperti `CANCELLED` dan `SHIPPED` tidak diubah manual sembarangan.

### 6.2 Inventory Service (`8081`)

- `GET /api/inventory`
- `GET /api/inventory/{productId}`
- `POST /api/inventory/reserve`
- `POST /api/inventory/release`

Contoh reserve atau release:

```json
{
  "productId": 1,
  "quantity": 10
}
```

Catatan:

- endpoint manual inventory ini bekerja langsung di service inventory
- ini berguna untuk requirement operasi stok manual
- ini bukan flow RabbitMQ utama
- gunakan sebagai endpoint manual atau simulasi, bukan jalur bisnis utama

### 6.3 Shipping Service (`8082`)

- `GET /api/shipments`
- `GET /api/shipments/{id}`
- `POST /api/shipments`
- `PUT /api/shipments/{id}/status?status={STATUS}`

Contoh create shipment manual:

```json
{
  "orderId": 99,
  "productId": 1,
  "quantity": 3
}
```

Catatan:

- create shipment manual memakai JSON body, bukan query parameter
- endpoint ini berguna untuk uji API shipping secara langsung
- ini bukan jalur integrasi RabbitMQ utama
- gunakan sebagai endpoint manual atau simulasi, bukan jalur bisnis utama

---

## 7. Kesimpulan praktis

Jika target Anda adalah:

- **simulasi inti sistem**
  - panduan lama sebenarnya sudah menyentuh flow utama
- **demo presentasi yang rapi**
  - panduan lama perlu disusun ulang
- **mencakup seluruh fitur project**
  - panduan lama belum lengkap

Jadi, struktur yang paling aman adalah:

1. jalankan simulasi inti end-to-end
2. tunjukkan demo RabbitMQ
3. tutup dengan checklist fitur tambahan

Dengan urutan itu, Anda bisa menjawab dua kebutuhan sekaligus:

- sistem ini benar-benar berjalan end-to-end
- fitur yang tersedia di codebase memang tercakup dan bisa diuji

---

## 8. Reset database

Jika ingin mengulang dari awal:

```sql
SET FOREIGN_KEY_CHECKS = 0;

USE order_management_db;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE products;
TRUNCATE TABLE categories;
TRUNCATE TABLE customers;

USE inventory_db;
TRUNCATE TABLE inventories;

USE shipping_db;
TRUNCATE TABLE shipments;

SET FOREIGN_KEY_CHECKS = 1;
```
