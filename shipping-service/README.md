# Shipping Service

Service ini berjalan di port `8082` dan mengelola data shipment pada sistem `OrderManagement`.

Tanggung jawab utamanya:

- menyimpan data `shipment`
- membuat shipment otomatis setelah stok berhasil di-reserve
- membatalkan shipment saat order dibatalkan
- mengirim event `SHIPPED` ke service lain
- menyediakan endpoint manual untuk simulasi dan pengujian

## Prasyarat

- Java 11+
- MySQL
- RabbitMQ

## Menjalankan Service

1. Buat database:

```sql
CREATE DATABASE shipping_db;
```

2. Sesuaikan `src/main/resources/application.properties`.
3. Jalankan:

```powershell
.\mvnw.cmd spring-boot:run
```

Service akan aktif di `http://localhost:8082`.

## Ringkasan Endpoint

| Method | Endpoint | Deskripsi |
| --- | --- | --- |
| `GET` | `/api/shipments` | Ambil semua shipment |
| `GET` | `/api/shipments/{id}` | Ambil detail shipment |
| `POST` | `/api/shipments` | Buat shipment manual |
| `PUT` | `/api/shipments/{id}/status?status={STATUS}` | Update status shipment |

Contoh body `POST /api/shipments`:

```json
{
  "orderId": 99,
  "productId": 1,
  "quantity": 3
}
```

Catatan:

- `POST /api/shipments` adalah endpoint manual dan bypass flow order utama.
- `PUT /api/shipments/{id}/status` hanya mem-publish RabbitMQ jika status yang dikirim adalah `SHIPPED`.
- Guard status sudah dipasang:
  - shipment `CANCELLED` tidak bisa diubah lagi
  - shipment `SHIPPED` tidak bisa diproses ulang
  - transisi manual ke `CANCELLED` hanya boleh dari `PENDING`

## RabbitMQ yang Dipakai oleh Service Ini

Consumer di `shipping-service`:

- `stock_reserved_key`
- `order_cancel_key`

Producer dari `shipping-service`:

- `order_shipped_key`

Flow utama:

1. `POST /api/orders?customerId={id}`
   - `shipping-service` tidak lagi menerima event order mentah secara langsung
   - service ini baru membuat shipment setelah inventory mengirim `stock_reserved_key`
2. `POST /api/orders/{id}/cancel`
   - shipping menerima `order_cancel_key`
   - shipment dengan status `PENDING` akan diubah menjadi `CANCELLED`
3. `PUT /api/shipments/{id}/status?status=SHIPPED`
   - shipment diubah menjadi `SHIPPED`
   - service ini publish `order_shipped_key`
   - `order-management` dan `inventory-service` melanjutkan sinkronisasi status dan finalize stok

## Manual vs Flow Utama

Flow utama yang sebaiknya dipakai untuk demo arsitektur:

- `POST /api/orders?customerId={id}`
- `POST /api/orders/{id}/cancel`
- `PUT /api/shipments/{id}/status?status=SHIPPED`

Endpoint manual yang tetap ada:

- `POST /api/shipments`

Endpoint manual ini cocok untuk requirement tugas dan simulasi, tetapi bukan jalur bisnis utama yang paling aman.

## Catatan Desain

- Shipment otomatis sekarang hanya dibuat setelah `inventory-service` benar-benar berhasil reserve stok.
- `cancelShipment(...)` hanya membatalkan shipment yang masih `PENDING`.
- Listener memakai `closedOrders` in-memory agar order yang sudah ditutup tidak diproses ulang di beberapa kondisi. Guard ini tidak persisten jika service restart.
- Error bisnis pada update status shipment sudah dipetakan ke response `400` lewat `GlobalExceptionHandler`.

## Dokumentasi Terkait

- `../README.md`
- `../DAFTAR_ENDPOINT_LENGKAP.md`
- `../PANDUAN_SIMULASI_DEMO.md`
- `../ANALISIS_RABBITMQ_DEMO.md`
- `../AUDIT_ENDPOINT_DESYNC.md`
