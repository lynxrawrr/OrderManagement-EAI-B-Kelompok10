# Panduan Struktur Folder & Komponen Service

Dokumen ini menjelaskan fungsi dari setiap folder dan file kunci di ketiga service agar Anda mudah memahaminya saat ditanya.

---

## 🏗️ Pola Arsitektur Umum
Setiap service mengikuti pola **Layered Architecture**:
1.  **Controller**: Pintu masuk API (menerima HTTP Request).
2.  **Service**: Tempat logika bisnis utama (perhitungan, pengiriman pesan ke RabbitMQ).
3.  **Repository**: Berhubungan langsung dengan database (SQL Queries).
4.  **Entity**: Definisi tabel database (Java Object).
5.  **DTO (Data Transfer Object)**: Objek khusus untuk menerima input JSON agar lebih aman dan rapi.
6.  **Listener**: Menunggu dan memproses pesan yang masuk dari RabbitMQ (khusus di Microservices).

---

## 📡 Konfigurasi RabbitMQ (Routing Keys)

Sistem ini menggunakan **Topic Exchange** bernama `order_exchange`. Berikut adalah daftar kunci (Routing Keys) yang digunakan untuk menghubungkan antar service:

| Routing Key | Pengirim (Producer) | Penerima (Consumer) | Fungsi / Dampak |
| :--- | :--- | :--- | :--- |
| **`product_sync_key`** | Order Service | Inventory Service | Membuat baris stok awal saat produk baru ditambah. |
| **`order_routing_key`** | Order Service | Inventory & Shipping | **Reserve Stok** (Inventory) & **Buat Jadwal Kirim** (Shipping). |
| **`order_cancel_key`** | Order Service | Inventory & Shipping | **Release Stok** (Inventory) & **Batalkan Pengiriman** (Shipping). |
| **`order_shipped_key`** | Shipping Service | Inventory & Order | **Finalize Stok** (Inventory) & **Update Status Order ke SHIPPED** (Order). |

---

## 1. Order Management Service (Port 8080)
Service ini adalah "Otak" utama sistem yang menyimpan data master.

-   **`config/RabbitMQConfig.java`**: Konfigurasi nama Exchange, Queue, dan Routing Key. Di sinilah "jalur pipa" RabbitMQ didefinisikan (termasuk `order_cancel_queue` untuk pembatalan).
-   **`controller/`**: Berisi API untuk Produk, Customer, dan Order.
-   **`entity/`**:
    -   `Product`: Data barang dan harga.
    -   `Category`: Pengelompokan produk (memiliki deskripsi).
    -   `Order` & `OrderItem`: Data transaksi belanja.
-   **`service/OrderService.java`**: 
    -   `createOrder`: Menyimpan pesanan dan mengirim pesan "Order Baru" ke RabbitMQ.
    -   `cancelOrder`: Mengubah status jadi `CANCELLED` (menggunakan `@Transactional` agar data konsisten) dan mengirim pesan pembatalan ke RabbitMQ.
-   **`listener/ShippingEventListener.java`**: Menunggu sinyal `order_shipped_key` dari Shipping Service untuk mengubah status pesanan menjadi `SHIPPED` secara otomatis.

---

## 2. Inventory Service (Port 8081)
Service ini bertugas menjaga akurasi stok fisik.

-   **`entity/Inventory.java`**: Memiliki tiga kolom kunci: `productId`, `availableStock` (bisa dibeli), dan `reservedStock` (sudah dipesan tapi belum dikirim).
-   **`listener/OrderEventListener.java`**: Menangani berbagai event stok:
    -   Pesan Produk Baru -> Buat stok awal.
    -   Pesan Order Baru -> Pindahkan stok ke *Reserved* (Reserve).
    -   Pesan Shipped -> Hapus stok *Reserved* (Finalize).
    -   Pesan Cancel -> Kembalikan *Reserved* ke *Available* (Release).
-   **`service/InventoryService.java`**: Berisi logika matematika untuk menambah/mengurangi stok di database.

---

## 3. Shipping Service (Port 8082)
Service ini mengelola proses pengiriman barang ke kurir.

-   **`entity/Shipment.java`**: Menyimpan informasi `orderId`, `productId`, `quantity`, `trackingNumber`, dan `status`.
-   **`listener/OrderEventListener.java`**: 
    -   Menunggu pesan "Order Baru" untuk membuat jadwal pengiriman (`status: PENDING`).
    -   Menunggu pesan "Cancel Order" untuk membatalkan pengiriman (`status: CANCELLED`).
-   **`service/ShipmentService.java`**: 
    -   `updateShipmentStatus`: Saat status jadi `SHIPPED`, mengirim pesan balik ke RabbitMQ agar Inventory Service melakukan Finalize stok dan Order Service melakukan update status pesanan.
    -   `cancelShipment`: Mencari pengiriman berdasarkan `orderId` dan mengubahnya menjadi `CANCELLED`.

---

## 🚀 Flow Perjalanan Data (Traceability)

Bagian ini menjelaskan bagaimana data "mengalir" dari satu file ke file lainnya.

### 1. Alur Tambah Produk (Sync ke Inventory)
- **MULAI**: `ProductController.java` (menerima request).
- **PROSES**: `ProductService.java` menyimpan data ke database.
- **KIRIM PESAN**: `ProductService.java` menggunakan `RabbitTemplate` mengirim pesan ke RabbitMQ.
- **TERIMA PESAN**: `OrderEventListener.java` (di Inventory Service 8081) menangkap pesan.
- **SELESAI**: `InventoryService.java` (8081) membuat baris stok baru di database.

### 2. Alur Checkout Pesanan (Sync ke Inventory & Shipping)
- **MULAI**: `OrderController.java` (menerima request belanja).
- **PROSES**: `OrderService.java` menyimpan data pesanan.
- **KIRIM PESAN**: `OrderService.java` mengirim pesan "Pesanan Baru" ke RabbitMQ.
- **TERIMA PESAN (CABANG A)**: `OrderEventListener.java` (8081) memanggil `InventoryService.reserveStock()` untuk mengunci stok.
- **TERIMA PESAN (CABANG B)**: `OrderEventListener.java` (8082) memanggil `ShipmentService.createShipment()` untuk membuat jadwal kurir.

### 3. Alur Pembatalan Pesanan (Cancel Order)
- **MULAI**: `OrderController.java` (menerima request `/cancel`).
- **PROSES**: `OrderService.java` mengubah status order di database (menggunakan `@Transactional`).
- **KIRIM PESAN**: `OrderService.java` mengirim pesan "Cancel Order" (routing key: `order_cancel_key`).
- **TERIMA PESAN (CABANG A)**: `OrderEventListener.java` (8081) memanggil `InventoryService.releaseStock()` untuk mengembalikan stok dari *Reserved* ke *Available*.
- **TERIMA PESAN (CABANG B)**: `OrderEventListener.java` (8082) memanggil `ShipmentService.cancelShipment()` untuk mengubah status pengiriman menjadi `CANCELLED`.

### 4. Alur Update Pengiriman (Sync ke Inventory & Order)
- **MULAI**: `ShipmentController.java` (update status ke SHIPPED).
- **PROSES**: `ShipmentService.java` mengubah status di database.
- **KIRIM PESAN**: `ShipmentService.java` mengirim pesan "Barang Terkirim" (routing key: `order_shipped_key`).
- **TERIMA PESAN (CABANG A)**: `OrderEventListener.java` (8081) menangkap pesan tersebut untuk Finalize stok.
- **TERIMA PESAN (CABANG B)**: `ShippingEventListener.java` (8080) menangkap pesan tersebut untuk mengupdate status Order menjadi `SHIPPED`.

---

## 💡 Istilah Kunci Jika Ditanya Penguji:

1.  **Kenapa pakai DTO?**
    "Agar kita bisa mengontrol data apa saja yang boleh masuk dari user, tidak langsung mengubah entitas database secara mentah."
2.  **Apa fungsi RabbitTemplate?**
    "Ini adalah tool dari Spring untuk mengirim pesan JSON ke RabbitMQ Exchange."
3.  **Apa itu @RabbitListener?**
    "Anatosi ini membuat aplikasi kita selalu 'siaga' menunggu pesan masuk dari antrean (Queue) tertentu di RabbitMQ."
4.  **Kenapa DB-nya dipisah?**
    "Ini prinsip Microservices (Database per Service) agar jika satu database rusak atau lambat, service yang lain tetap bisa berjalan."
5.  **Apa bedanya Exchange dan Queue?**
    "Exchange itu seperti Kantor Pos yang menerima surat dan menentukan arahnya (Routing), sedangkan Queue adalah Kotak Surat tujuan tempat pesan disimpan sampai dibaca oleh aplikasi."
6.  **Kenapa pakai @Transactional?**
    "Untuk memastikan integritas data. Jika ada error di tengah proses simpan, database akan otomatis melakukan Rollback agar data tidak rusak/setengah tersimpan."
