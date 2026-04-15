# 🛒 Microservices Order Management System (EAI Project)

Sistem Backend berbasis **Microservices** dan **Event-Driven Architecture (EDA)** menggunakan Java Spring Boot untuk mengelola alur transaksi e-commerce secara terintegrasi dan asinkron melalui **RabbitMQ**.

---

## 🏗️ Arsitektur Sistem

Sistem ini terdiri dari tiga layanan mandiri dengan database terpisah:

1.  **Order Management Service (Port 8080)**: Mengelola data master (Produk, Kategori, Customer) dan transaksi pemesanan.
2.  **Inventory Service (Port 8081)**: Mengelola ketersediaan stok fisik, reservasi stok, dan sinkronisasi produk baru.
3.  **Shipping Service (Port 8082)**: Mengelola jadwal pengiriman dan pelacakan status pesanan.

Semua layanan berkomunikasi secara asinkron menggunakan **RabbitMQ** sebagai Message Broker.

---

## 🚀 Fitur Utama

- **📦 Sinkronisasi Produk Otomatis**: Menambah produk di Order Service otomatis membuat data stok di Inventory Service.
- **🔐 Reservasi Stok (Two-Phase Stock)**: Memisahkan `available_stock` dan `reserved_stock` untuk mencegah *overselling*.
- **🚚 Pengiriman Otomatis**: Pembuatan pesanan otomatis memicu pembuatan jadwal pengiriman di Shipping Service.
- **✅ Finalisasi Stok**: Stok cadangan (`reserved`) otomatis dihapus saat status pengiriman berubah menjadi `SHIPPED`.
- **🔄 Rollback Stok**: Pembatalan pesanan otomatis mengembalikan stok cadangan ke stok tersedia.
- **📡 JSON-Based API**: Seluruh komunikasi manual menggunakan JSON Body (DTO).

---

## 🛠️ Prasyarat & Instalasi

### 1. Database MySQL
Buat tiga database berikut:
```sql
CREATE DATABASE order_management_db;
CREATE DATABASE inventory_db;
CREATE DATABASE shipping_db;
```

### 2. RabbitMQ (via Docker)
Jalankan RabbitMQ Management menggunakan Docker:
```bash
docker run -d --name rabbitmq -p 5673:5672 -p 15673:15672 rabbitmq:3-management
```
- **Dashboard**: `http://localhost:15673` (Guest/Guest)

### 3. Menjalankan Aplikasi
Jalankan perintah berikut di folder masing-masing service:
```bash
./mvnw spring-boot:run
```

---

## 🔄 Flow Integrasi (Event-Driven)

| Event | Producer | Consumer | Dampak |
| :--- | :--- | :--- | :--- |
| **Product Created** | Order (8080) | Inventory (8081) | Inisialisasi stok awal produk baru. |
| **Order Placed** | Order (8080) | Inventory (8081) & Shipping (8082) | Reserve stok & Buat jadwal pengiriman. |
| **Order Cancelled** | Order (8080) | Inventory (8081) | Kembalikan stok dari Reserved ke Available. |
| **Order Shipped** | Shipping (8082) | Inventory (8081) | Hapus stok cadangan (Barang keluar gudang). |

---

## 📖 Dokumentasi Lanjutan

Untuk detail cara mensimulasikan sistem dan memahami relasi arsitektural, silakan merujuk pada dokumen berikut:

1.  **[PANDUAN_SIMULASI.md](./PANDUAN_SIMULASI.md)**: Langkah-demi-langkah pengujian API (Postman/Curl) dari hulu ke hilir.
2.  **[ARSITEKTUR_DAN_RELASI.md](./ARSITEKTUR_DAN_RELASI.md)**: Penjelasan alasan desain, diagram Mermaid, dan detail teknis integrasi.

---

## 🛠️ Tech Stack
- **Framework**: Spring Boot 3.x
- **Message Broker**: RabbitMQ
- **Database**: MySQL (JPA/Hibernate)
- **Container**: Docker
- **Validation**: Jakarta Bean Validation
