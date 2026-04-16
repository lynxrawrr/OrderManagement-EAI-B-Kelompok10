# tPanduan Lengkap Simulasi Microservices Order Management

Dokumen ini berisi panduan detail untuk mensimulasikan sistem Order Management yang terintegrasi menggunakan RabbitMQ, mencakup operasi manual dan otomatis sesuai requirement tugas EAI.

---

## 0. Persiapan Awal (Setup Project)

Ikuti langkah-langkah berikut setelah Anda melakukan `git clone` dari GitHub:

### 1. Persiapan Database MySQL

Buka MySQL Workbench atau terminal MySQL, kemudian buat tiga database kosong:

```sql
CREATE DATABASE order_management_db;
CREATE DATABASE inventory_db;
CREATE DATABASE shipping_db;
```

### 2. Konfigurasi Koneksi Database

Cek file `src/main/resources/application.properties` di **setiap folder service** (`order-management`, `inventory-service`, `shipping-service`). Pastikan `spring.datasource.username` dan `spring.datasource.password` sudah sesuai dengan user MySQL Anda.

### 3. Setup RabbitMQ (via Docker)

Pastikan Docker Desktop sudah aktif, lalu jalankan perintah berikut di terminal untuk menjalankan RabbitMQ:

```bash
docker run -d --name rabbitmq -p 5673:5672 -p 15673:15672 rabbitmq:3-management
```

- **Port 5673**: Digunakan untuk koneksi aplikasi (sesuai konfigurasi `application.properties`).
- **Port 15673**: Digunakan untuk mengakses Dashboard RabbitMQ di browser (`http://localhost:15673`, User/Pass: `guest`).

### 4. Menjalankan Service

Buka 3 terminal berbeda untuk menjalankan ketiga service secara bersamaan:

- **Terminal 1 (Order Management):**
  ```bash
  cd order-management
  ./mvnw spring-boot:run
  ```
- **Terminal 2 (Inventory Service):**
  ```bash
  cd inventory-service
  ./mvnw spring-boot:run
  ```
- **Terminal 3 (Shipping Service):**
  ```bash
  cd shipping-service
  ./mvnw spring-boot:run
  ```

---

## 1. Persiapan Lingkungan

1. **Database**: Pastikan MySQL aktif dengan database `order_management_db`, `inventory_db`, dan `shipping_db`.
2. **RabbitMQ**: Pastikan berjalan di port `5673`.
3. **Urutan Service**: Jalankan Order (8080) -> Inventory (8081) -> Shipping (8082).

---

## 2. Langkah-Langkah Simulasi End-to-End

### Langkah 1: Persiapan Data Master (Order Service - 8080)

- **Tambah Kategori**: `POST /api/categories`
  ```json
  {
    "name": "Electronics",
    "description": "Kategori untuk barang-barang elektronik"
  }
  ```
- **Tambah Customer**: `POST /api/customers`
  ```json
  {
    "name": "Bram",
    "email": "bram@gmail.com",
    "address": "Jakarta Timur"
  }
  ```
- **Lihat Data**:
  - `GET /api/customers` (Cek semua customer)
  - `GET /api/categories` (Cek semua kategori)

### Langkah 2: Tambah Produk & Sinkronisasi Stok

- **Tambah Produk**: `POST /api/products`
  ```json
  {
    "name": "Laptop",
    "price": 10000000,
    "stock": 50,
    "categoryId": 1
  }
  ```
- **Verifikasi Otomatis**: Cek di Inventory Service (8081) `GET /api/inventory/1`. Data stok akan otomatis terbuat dengan `availableStock: 50`.

### Langkah 3: Operasi Manual Inventory (Requirement 1 - 8081)

Gunakan Port 8081 untuk mencoba fitur manual menggunakan **JSON Body**:

- **Cek Semua Stok**: `GET /api/inventory`
- **Reserve Manual**:
  - **Endpoint**: `POST /api/inventory/reserve`
  - **JSON Body**: `{ "productId": 1, "quantity": 10 }`
- **Release Manual**:
  - **Endpoint**: `POST /api/inventory/release`
  - **JSON Body**: `{ "productId": 1, "quantity": 10 }`

### Langkah 4: Proses Transaksi Pesanan (8080)

- **Buat Order**: `POST /api/orders?customerId=1`
  ```json
  [
    {
      "productId": 1,
      "quantity": 5
    }
  ]
  ```
- **Dampak Otomatis**:
  - **Inventory**: `availableStock` berkurang 5, `reservedStock` bertambah 5.
  - **Shipping**: Otomatis membuat jadwal pengiriman baru dengan status `PENDING`.

### Langkah 6: Pembatalan Pesanan (Optional)

Jika pesanan dibatalkan, stok di inventory harus kembali normal.

- **POST** `http://localhost:8080/api/orders/1/cancel`
- **Verifikasi Inventory (8081)**:
  - **GET** `http://localhost:8081/api/inventory/1`
  - _Hasil: `availableStock` bertambah, `reservedStock` berkurang._
    Gunakan Port 8082 untuk mengelola pengiriman:
- **Lihat Semua Pengiriman**: `GET /api/shipments`
- **Create Shipment Manual**:
  - **Endpoint**: `POST /api/shipments`
  - **JSON Body**: `{ "orderId": 1, "productId": 1, "quantity": 5 }`
- **Update Status ke SHIPPED**: `PUT /api/shipments/1/status?status=SHIPPED`
- **Verifikasi Finalize Stok**: Setelah status jadi `SHIPPED`, cek Inventory `GET /api/inventory/1`. `reservedStock` akan otomatis kembali ke **0** karena barang sudah benar-benar terkirim.

---

## 3. Analisis Requirement Tugas

### Requirement 1: Inventory API

- [X] **Cek Stok**: Tersedia di `GET /api/inventory` (All) dan `GET /api/inventory/{id}` (Single).
- [X] **Reserve Stok**: Tersedia manual di `POST /api/inventory/reserve` dan otomatis saat Order.
- [X] **Release Stok**: Tersedia manual di `POST /api/inventory/release` dan otomatis saat Cancel Order.

### Requirement 2: Shipping API

- [X] **Create Shipment**: Otomatis saat Order dan manual di `POST /api/shipments`.
- [X] **Get Shipment**: Tersedia di `GET /api/shipments` (All) dan `GET /api/shipments/{id}` (Single).
- [X] **Update Shipment Status**: Tersedia di `PUT /api/shipments/{id}/status`.

### Detail Entitas & Integrasi

- [X] **Category Description**: Field `description` tersedia dan berfungsi.
- [X] **Finalize Stock**: Stok cadangan otomatis dihapus saat pengiriman berhasil (`SHIPPED`).

---

## 4. Daftar Endpoint Lengkap

| Service                    | Fitur                    | Method | Endpoint                                                    |
| :------------------------- | :----------------------- | :----- | :---------------------------------------------------------- |
| **Order (8080)**     | List Products            | GET    |                                                             |
|                            | List Customers           | GET    | `/api/customers`                                          |
|                            | Sub-resource Category    | GET    | `/api/categories/{id}/products`                           |
|                            | Sub-resource Customer    | GET    | `/api/customers/{id}/orders`                              |
|                            | Create Order             | POST   | `/api/orders?customerId={id}`                             |
|                            | Cancel Order             | POST   | `/api/orders/{id}/cancel`                                 |
| **Inventory (8081)** | Check All Stock          | GET    | `/api/inventory`                                          |
|                            | Check Single             | GET    | `/api/inventory/{productId}`                              |
|                            | Reserve Manual           | POST   | `/api/inventory/reserve`                                  |
|                            | Release Manual           | POST   | `/api/inventory/release`                                  |
| **Shipping (8082)**  | List Shipments           | GET    | `/api/shipments`                                          |
|                            | Check Single             | GET    | `/api/shipments/{id}`                                     |
|                            | Create Shipment (Manual) | POST   | `/api/shipments?orderId={id}&productId={id}&quantity={n}` |
|                            | Update Status            | PUT    | `/api/shipments/{id}/status`                              |

---

## 5. Prosedur Reset Database (Mulai Dari Nol)

Jika Anda ingin mengulang simulasi dari awal dengan database kosong, jalankan perintah SQL berikut di MySQL Anda:

```sql
-- Nonaktifkan pengecekan foreign key agar bisa mengosongkan tabel yang berelasi
SET FOREIGN_KEY_CHECKS = 0;

-- Kosongkan Order Management DB
USE order_management_db;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE products;
TRUNCATE TABLE categories;
TRUNCATE TABLE customers;

-- Kosongkan Inventory DB
USE inventory_db;
TRUNCATE TABLE inventories;

-- Kosongkan Shipping DB
USE shipping_db;
TRUNCATE TABLE shipments;

-- Aktifkan kembali pengecekan foreign key
SET FOREIGN_KEY_CHECKS = 1;
```

_(Catatan: Pastikan urutan TRUNCATE pada `order_management_db` sesuai agar tidak terjadi error relasi)._
