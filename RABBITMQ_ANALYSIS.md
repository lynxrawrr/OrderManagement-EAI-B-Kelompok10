# Analisis Konfigurasi RabbitMQ - Order Management System

Berikut adalah daftar konfigurasi RabbitMQ yang digunakan dalam sistem ini beserta penjelasan alur kerjanya.

## Infrastruktur (Docker)
RabbitMQ dijalankan menggunakan kontainer **Docker** untuk mempermudah orkestrasi dan manajemen layanan. Konfigurasi yang digunakan adalah:
*   **Image**: `rabbitmq:3-management`
*   **Port AMQP**: `5673` (Dipetakan dari `5672` di dalam kontainer)
*   **Port Management (Dashboard)**: `15673` (Dipetakan dari `15672` di dalam kontainer)
*   **Akses Dashboard**: `http://localhost:15673` (User/Pass: `guest`)

## Daftar Konfigurasi RabbitMQ

| Komponen | Nama / Nilai | Tipe / Deskripsi |
| :--- | :--- | :--- |
| **Exchange** | `order_exchange` | **Topic Exchange** (Pusat distribusi pesan) |
| **Queue 1** | `order_stock_queue` | Antrean untuk reservasi stok di *Inventory Service* |
| **Queue 2** | `order_shipping_queue` | Antrean untuk pembuatan jadwal pengiriman setelah stok berhasil di-reserve |
| **Queue 3** | `order_cancel_queue` | Antrean untuk pembatalan reservasi stok di *Inventory Service* |
| **Queue 4** | `order_shipping_cancel_queue` | Antrean untuk pembatalan pengiriman di *Shipping Service* |
| **Queue 5** | `order_status_queue` | Antrean untuk update status pesanan di *Order Service* |
| **Queue 6** | `order_shipped_inventory_queue` | Antrean untuk finalisasi stok di *Inventory Service* |
| **Queue 7** | `product_inventory_queue` | Antrean untuk sinkronisasi stok produk ke *Inventory Service* |
| **Queue 8** | `product_inventory_delete_queue` | Antrean untuk sinkronisasi hapus produk ke *Inventory Service* |
| **Queue 9** | `order_reserve_failed_queue` | Antrean untuk notifikasi reserve stok gagal ke *Order Service* |
| **Routing Key 1** | `order_routing_key` | Digunakan saat pesanan baru dibuat dan dikirim ke Inventory (*New Order*) |
| **Routing Key 2** | `stock_reserved_key` | Digunakan saat Inventory berhasil reserve stok dan meneruskan ke Shipping |
| **Routing Key 3** | `order_reserve_failed_key` | Digunakan saat Inventory gagal reserve stok dan memberi tahu Order |
| **Routing Key 4** | `order_cancel_key` | Digunakan saat pesanan dibatalkan (*Cancel Order*) |
| **Routing Key 5** | `order_shipped_key` | Digunakan saat barang telah dikirim (*Shipped*) |
| **Routing Key 6** | `product_sync_key` | Digunakan saat produk dibuat atau diubah (*Product Sync*) |
| **Routing Key 7** | `product_delete_key` | Digunakan saat produk dihapus (*Product Delete*) |

---

## Penjelasan Alur Kerja (Paragraph)

Sistem ini mengadopsi arsitektur berbasis event (*event-driven architecture*) menggunakan RabbitMQ sebagai broker pesan untuk mengoordinasikan tiga layanan utama: **Order Service**, **Inventory Service**, dan **Shipping Service**. Alur dimulai ketika `Order Service` mengirimkan pesan ke `order_exchange` dengan routing key tertentu. Pada flow order terbaru, pesan `order_routing_key` hanya dikirim lebih dulu ke `order_stock_queue` untuk diproses oleh Inventory Service. Jika reserve stok berhasil, Inventory Service mengirim event `stock_reserved_key` ke `order_shipping_queue`, baru kemudian Shipping Service membuat jadwal pengiriman. Jika reserve stok gagal, Inventory Service mengirim `order_reserve_failed_key` ke Order Service agar status order diubah menjadi `FAILED` dan rollback dipicu. Jika terjadi pembatalan pesanan, sistem menggunakan `order_cancel_key` untuk memicu aksi rollback stok dan pembatalan pengiriman di masing-masing layanan. Selain itu, terdapat mekanisme sinkronisasi stok saat produk dibuat atau diubah, sinkronisasi penghapusan produk ke inventory, serta pembaruan status pengiriman saat barang berstatus `SHIPPED`, yang memastikan konsistensi data stok akhir dan status pesanan di seluruh ekosistem microservices ini secara asinkron tanpa adanya ketergantungan langsung antar layanan.
