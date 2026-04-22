# Microservices Order Management System

Project ini adalah backend `Order Management` berbasis microservices menggunakan Spring Boot, MySQL, dan RabbitMQ.

Sistem terdiri dari tiga service:

- `order-management` (`8080`)
- `inventory-service` (`8081`)
- `shipping-service` (`8082`)

Komunikasi antar-service dilakukan secara asynchronous melalui RabbitMQ dengan `topic exchange` bernama `order_exchange`.

## Ringkasan Arsitektur

| Service | Port | Tanggung jawab utama | Database |
| --- | --- | --- | --- |
| `order-management` | `8080` | master data, order, publish event utama | `order_management_db` |
| `inventory-service` | `8081` | cek stok, reserve stock, release stock, finalize stock | `inventory_db` |
| `shipping-service` | `8082` | create shipment, get shipment, update shipment status | `shipping_db` |

## Flow Utama Aplikasi

Flow bisnis utama yang saat ini dipakai:

1. `POST /api/products`
   - produk dibuat di `order-management`
   - event `product_sync_key` dikirim ke RabbitMQ
   - `inventory-service` membuat inventory awal

2. `PUT /api/products/{id}`
   - produk diperbarui di `order-management`
   - event `product_sync_key` dikirim ke RabbitMQ
   - `inventory-service` menyinkronkan total stok terbaru

3. `DELETE /api/products/{id}`
   - product hanya boleh dihapus jika belum pernah dipakai pada order
   - event `product_delete_key` dikirim ke RabbitMQ
   - `inventory-service` ikut menghapus row inventory terkait

4. `POST /api/orders?customerId={id}`
   - order dibuat di `order-management`
   - event `order_routing_key` dikirim ke RabbitMQ
   - `inventory-service` mencoba reserve stok
   - jika reserve sukses, `inventory-service` mengirim `stock_reserved_key`
   - `shipping-service` baru membuat shipment `PENDING` setelah menerima `stock_reserved_key`
   - jika reserve gagal, `inventory-service` mengirim `order_reserve_failed_key` dan order ditandai `FAILED`

5. `POST /api/orders/{id}/cancel`
   - status order diubah menjadi `CANCELLED`
   - event `order_cancel_key` dikirim ke RabbitMQ
   - `inventory-service` me-release stok
   - `shipping-service` membatalkan shipment yang masih `PENDING`

6. `PUT /api/shipments/{id}/status?status=SHIPPED`
   - shipment diubah ke `SHIPPED`
   - event `order_shipped_key` dikirim ke RabbitMQ
   - `order-management` mengubah status order menjadi `SHIPPED`
   - `inventory-service` melakukan finalize stok

## Endpoint Inti vs Endpoint Manual

Untuk penggunaan normal, endpoint yang direkomendasikan adalah:

- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `POST /api/orders?customerId={id}`
- `POST /api/orders/{id}/cancel`
- `PUT /api/shipments/{id}/status?status=SHIPPED`
- seluruh endpoint `GET`

Endpoint berikut masih ada, tetapi lebih cocok untuk simulasi teknis, demo, atau pengujian lokal:

- `POST /api/inventory/reserve`
- `POST /api/inventory/release`
- `POST /api/shipments`
- `PUT /api/orders/{id}/status`

Alasannya:

- endpoint manual inventory dapat mengubah stok tanpa order flow
- endpoint manual shipment dapat membuat shipment tanpa order flow
- update status order manual hanya mengubah data lokal di `order-management`
- endpoint manual tersebut tetap berguna untuk requirement tugas, tetapi bukan jalur bisnis utama yang paling aman

Jadi, untuk demo arsitektur yang konsisten, gunakan flow utama dan hindari endpoint manual sebagai jalur bisnis utama.

## Status Kesesuaian Flow

Secara umum, flow aplikasi ini sudah sesuai untuk:

- tugas lanjutan `Inventory API` dan `Shipping API`
- demo integrasi `order -> inventory -> shipping`
- demo RabbitMQ dan event-driven architecture

Namun, flow saat ini belum sepenuhnya aman dari sisi konsistensi state.

Beberapa catatan penting:

- `POST /api/orders` sekarang lebih aman karena shipment baru dibuat setelah reserve stok sukses
- order memang masih tersimpan lebih dulu, tetapi jika reserve gagal status akan berubah menjadi `FAILED` dan rollback dijalankan otomatis
- `POST /api/orders/{id}/cancel` kini sudah diberi guard agar tidak dipanggil berulang
- `PUT /api/shipments/{id}/status` kini sudah diberi guard agar `SHIPPED` tidak diproses berulang
- `DELETE /api/products/{id}` kini ikut menyinkronkan penghapusan ke `inventory-service` dan ditolak jika product sudah pernah dipakai order
- `PUT /api/products/{id}` telah dipulihkan dan sekarang mengirim sinkronisasi stok ke `inventory-service`

Artinya:

- untuk kebutuhan kuliah, demo, dan simulasi: flow ini sudah cukup baik
- untuk sistem yang lebih ketat atau production-like: masih perlu hardening pada beberapa endpoint

## Prasyarat

- Java 17 atau lebih baru
- MySQL
- Docker
- Maven Wrapper

## Setup

### 1. Buat database

```sql
CREATE DATABASE order_management_db;
CREATE DATABASE inventory_db;
CREATE DATABASE shipping_db;
```

### 2. Jalankan RabbitMQ

```bash
docker run -d --name rabbitmq -p 5673:5672 -p 15673:15672 rabbitmq:3-management
```

Dashboard:

- `http://localhost:15673`
- username: `guest`
- password: `guest`

### 3. Sesuaikan konfigurasi database

Periksa file berikut:

- `order-management/src/main/resources/application.properties`
- `inventory-service/src/main/resources/application.properties`
- `shipping-service/src/main/resources/application.properties`

### 4. Jalankan semua service

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

## Quick Demo

Urutan demo yang paling jelas:

1. buat category
2. buat customer
3. buat product
4. cek inventory awal
5. buat order
6. cek inventory dan shipment
7. lakukan salah satu:
   - cancel order
   - update shipment ke `SHIPPED`

## Dokumentasi yang Disarankan

Gunakan dokumen berikut sebagai referensi utama:

1. [PANDUAN_SIMULASI_DEMO.md](./PANDUAN_SIMULASI_DEMO.md)
   - panduan simulasi dan demo end-to-end
2. [DAFTAR_ENDPOINT_LENGKAP.md](./DAFTAR_ENDPOINT_LENGKAP.md)
   - daftar endpoint lengkap seluruh service
3. [ARSITEKTUR_DAN_RELASI.md](./ARSITEKTUR_DAN_RELASI.md)
   - alasan desain dan relasi `order-inventory-shipping`
4. [ANALISIS_RABBITMQ_DEMO.md](./ANALISIS_RABBITMQ_DEMO.md)
   - titik penggunaan RabbitMQ dan cara mendemokannya
5. [AUDIT_ENDPOINT_DESYNC.md](./AUDIT_ENDPOINT_DESYNC.md)
   - audit endpoint yang masih rawan desync
6. [order-management/README.md](./order-management/README.md)
   - ringkasan service order-management
7. [inventory-service/README.md](./inventory-service/README.md)
   - ringkasan service inventory-service
8. [shipping-service/README.md](./shipping-service/README.md)
   - ringkasan service shipping-service

## Tech Stack

- Spring Boot 3.x
- Spring Data JPA
- MySQL
- RabbitMQ
- Docker
- Jakarta Bean Validation

## Catatan

- `DataInitializer` di `order-management` sedang tidak aktif, jadi data awal tidak otomatis dibuat.
- Endpoint `PUT /api/products/{id}` telah dipulihkan agar tetap sesuai requirement CRUD Product, dan update stoknya ikut disinkronkan ke `inventory-service`.
- Endpoint `DELETE /api/products/{id}` sekarang ikut menghapus data inventory lewat event RabbitMQ, tetapi akan ditolak jika product sudah dipakai pada order.
