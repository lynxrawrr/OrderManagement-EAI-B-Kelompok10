# Inventory Service

Service ini berjalan di port `8081` dan menjadi sumber utama data stok pada sistem `OrderManagement`.

Tanggung jawab utamanya:

- menyimpan data `inventory` per `productId`
- menjaga `availableStock` dan `reservedStock`
- menerima event sinkronisasi product dari `order-management`
- menerima request reserve, cancel, dan finalize dari flow order
- meneruskan hasil reserve ke service lain lewat RabbitMQ

## Prasyarat

- Java 11+
- MySQL
- RabbitMQ

## Menjalankan Service

1. Buat database:

```sql
CREATE DATABASE inventory_db;
```

2. Sesuaikan `src/main/resources/application.properties`.
3. Jalankan:

```powershell
.\mvnw.cmd spring-boot:run
```

Service akan aktif di `http://localhost:8081`.

## Ringkasan Endpoint

| Method | Endpoint | Deskripsi |
| --- | --- | --- |
| `GET` | `/api/inventory` | Ambil semua data inventory |
| `GET` | `/api/inventory/{productId}` | Ambil inventory berdasarkan product |
| `POST` | `/api/inventory/reserve` | Reserve stok manual |
| `POST` | `/api/inventory/release` | Release stok manual |

Contoh body `POST /api/inventory/reserve` atau `POST /api/inventory/release`:

```json
{
  "productId": 1,
  "quantity": 10
}
```

Catatan:

- Endpoint `reserve` dan `release` adalah endpoint manual.
- Endpoint manual ini berguna untuk requirement tugas dan simulasi teknis, tetapi bukan flow bisnis utama yang paling aman.

## RabbitMQ yang Dipakai oleh Service Ini

Consumer di `inventory-service`:

- `product_sync_key`
- `product_delete_key`
- `order_routing_key`
- `order_cancel_key`
- `order_shipped_key`

Producer dari `inventory-service`:

- `stock_reserved_key`
- `order_reserve_failed_key`

Flow utama:

1. `POST /api/products` atau `PUT /api/products/{id}` dari `order-management`
   - inventory menerima `product_sync_key`
   - inventory membuat row baru atau menyinkronkan total stok terbaru
2. `DELETE /api/products/{id}` dari `order-management`
   - inventory menerima `product_delete_key`
   - inventory menghapus row terkait jika ada
3. `POST /api/orders?customerId={id}` dari `order-management`
   - inventory menerima `order_routing_key`
   - inventory mencoba reserve stok
   - jika sukses, inventory publish `stock_reserved_key` ke `shipping-service`
   - jika gagal, inventory publish `order_reserve_failed_key` ke `order-management`
4. `POST /api/orders/{id}/cancel`
   - inventory menerima `order_cancel_key`
   - inventory me-release reserved stock
5. `PUT /api/shipments/{id}/status?status=SHIPPED`
   - inventory menerima `order_shipped_key`
   - inventory finalize reserved stock

## Manual vs Flow Utama

Flow utama yang sebaiknya dipakai untuk demo arsitektur:

- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `POST /api/orders?customerId={id}`
- `POST /api/orders/{id}/cancel`
- `PUT /api/shipments/{id}/status?status=SHIPPED`

Endpoint manual yang tetap ada:

- `POST /api/inventory/reserve`
- `POST /api/inventory/release`

Endpoint manual ini bekerja langsung di service inventory dan tidak otomatis memberi tahu service lain.

## Catatan Desain

- `inventory-service` adalah sumber utama stok operasional.
- `syncProductStock(...)` memperlakukan `stock` dari product sebagai total stok, lalu menjaga `reservedStock` yang sudah ada.
- `releaseStock(...)` sekarang dibatasi oleh `reservedStock` yang tersedia agar tidak turun ke nilai negatif.
- Listener memakai `closedOrders` in-memory untuk mencegah sebagian event lanjutan pada order yang sudah ditutup. Guard ini tidak persisten jika service restart.

## Dokumentasi Terkait

- `../README.md`
- `../DAFTAR_ENDPOINT_LENGKAP.md`
- `../PANDUAN_SIMULASI_DEMO.md`
- `../ANALISIS_RABBITMQ_DEMO.md`
- `../AUDIT_ENDPOINT_DESYNC.md`
