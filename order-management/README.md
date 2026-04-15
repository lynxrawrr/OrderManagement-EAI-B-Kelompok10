# 🛒 Order Management System API (EAI 101)

Sistem Backend sederhana berbasis Java Spring Boot untuk mengelola alur transaksi e-commerce, mulai dari manajemen data master (Produk, Kategori, Pelanggan) hingga integrasi transaksi pesanan (Order) dengan pengurangan stok otomatis.

---

## 🚀 Fitur Utama
- **🛠️ CRUD Master Data**: Manajemen lengkap untuk Produk, Kategori, dan Pelanggan.
- **🔗 Relasi Database**: Implementasi relasi Many-to-One antara Produk-Kategori dan Order-Pelanggan.
- **📝 Transaksi Pesanan**: Pembuatan Order dengan detail banyak item sekaligus.
- **📉 Manajemen Stok**: Pengurangan stok otomatis saat pesanan dibuat (dengan fitur *rollback* jika stok tidak cukup).
- **✅ Validasi Input**: Validasi data menggunakan DTO dan Jakarta Validation.
- **⚠️ Global Error Handling**: Format respon error JSON yang konsisten.

---

## 🛠️ Prasyarat
- **Java 11** atau lebih tinggi (Direkomendasikan Java 17+).
- **Maven** (sudah termasuk dalam wrapper `./mvnw`).
- **MySQL 8.0** (Direkomendasikan menggunakan versi 8.0 untuk kompatibilitas Workbench).

---

## ⚙️ Instalasi & Konfigurasi

### 1. Persiapan Database
Login ke terminal MySQL Anda dan jalankan perintah berikut:
```sql
CREATE DATABASE order_management_db;
```

### 2. Konfigurasi Aplikasi
Buka file `src/main/resources/application.properties` dan sesuaikan kredensial MySQL Anda:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/order_management_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=ISI_PASSWORD_MYSQL_ANDA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 🏃 Cara Menjalankan
Buka terminal di root folder project dan jalankan perintah:
```powershell
.\mvnw.cmd spring-boot:run
```
Aplikasi akan berjalan di `http://localhost:8080`.

---

## 📖 Dokumentasi API (Demo)

### 1. Product API
| Method | Endpoint | Deskripsi |
| :--- | :--- | :--- |
| **GET** | `/api/products` | Mendapatkan semua produk |
| **GET** | `/api/products/{id}` | Mendapatkan detail produk |
| **POST** | `/api/products` | Menambah produk baru (Validation Active) |
| **PUT** | `/api/products/{id}` | Update data produk |
| **DELETE** | `/api/products/{id}` | Menghapus produk |

**Contoh Body POST Product:**
```json
{
    "name": "Laptop Pro",
    "price": 15000000.0,
    "stock": 10,
    "category": {
        "id": 1
    }
}
```

### 2. Category API
| Method | Endpoint | Deskripsi |
| :--- | :--- | :--- |
| **GET** | `/api/categories` | Mendapatkan semua kategori |
| **GET** | `/api/categories/{id}` | Mendapatkan detail kategori |
| **POST** | `/api/categories` | Menambah kategori baru |
| **PUT** | `/api/categories/{id}` | Mengubah data kategori |
| **DELETE** | `/api/categories/{id}` | Menghapus kategori |

### 3. Customer API
| Method | Endpoint | Deskripsi |
| :--- | :--- | :--- |
| **GET** | `/api/customers` | Mendapatkan semua pelanggan |
| **GET** | `/api/customers/{id}` | Mendapatkan detail pelanggan |
| **POST** | `/api/customers` | Menambah pelanggan baru (Email Validation) |
| **PUT** | `/api/customers` | Mengubah data pelanggan (Email Validation Active) |
| **DELETE** | `/api/customers/{id}` | Menghapus pelanggan |

**Contoh Body PUT/POST Customer:**
```json
{
    "name": "Bram",
    "email": "bram@example.com",
    "address": "Jl. Soekarno Hatta No. 10, Malang"
}
```

### 4. Order API (Transaksi)
| Method | Endpoint | Deskripsi |
| :--- | :--- | :--- |
| **POST** | `/api/orders?customerId=1` | Membuat pesanan baru (Mengurangi Stok) |
| **GET** | `/api/orders` | Melihat semua riwayat transaksi |
| **GET** | `/api/orders/{id}` | Mendapatkan pesanan |
| **PUT** | `/api/orders/{id}/status` | Update status (PENDING, SHIPPED, dll) |
| **DELETE** | `/api/orders/{id}` | Menghapus pesanan |

**Contoh Body POST Order:**
```json
[
  {"productId": 1, "quantity": 2},
  {"productId": 2, "quantity": 1}
]
```

---

## ⚠️ Penanganan Masalah (Troubleshooting)

### Infinite JSON Recursion
Jika Anda menemui error *Document nesting depth exceeds the maximum allowed*, pastikan Anda sudah menambahkan anotasi `@JsonIgnore` pada entitas `OrderItem`:

```java
@ManyToOne
@JoinColumn(name = "order_id", nullable = false)
@JsonIgnore
private Order order;
```

### Masalah MySQL Versi 9.x
Jika MySQL Workbench tidak bisa terhubung, disarankan melakukan downgrade ke MySQL 8.0 atau menggunakan client alternatif seperti DBeaver atau HeidiSQL.

---

**👨‍💻 Dibuat oleh:** Brahmantio J P  
**🎓 Mata Kuliah:** Enterprise Application Integration (EAI)  
**🗓️ Tahun:** 2026
