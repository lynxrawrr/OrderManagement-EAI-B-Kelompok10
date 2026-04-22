# Order Management Service

Service ini berjalan di port `8080` dan menjadi pusat data master serta koordinasi flow order pada sistem `OrderManagement`.

Tanggung jawab utamanya:

- menyimpan data `product`, `category`, `customer`, `order`, dan `order_item`
- membuat order baru
- membatalkan order
- menerima event reserve gagal dari `inventory-service`
- menerima event `SHIPPED` dari `shipping-service`
- mengirim event RabbitMQ utama ke `inventory-service` dan event rollback ke `inventory-service` serta `shipping-service`

## Prasyarat

- Java 11+
- MySQL
- RabbitMQ

## Menjalankan Service

1. Buat database:

```sql
CREATE DATABASE order_management_db;
```

2. Sesuaikan `src/main/resources/application.properties`.
3. Jalankan:

```powershell
.\mvnw.cmd spring-boot:run
```

Service akan aktif di `http://localhost:8080`.

## Ringkasan Endpoint

### Product API

| Method | Endpoint | Deskripsi |
| --- | --- | --- |
| `GET` | `/api/products` | Ambil semua produk |
| `GET` | `/api/products/{id}` | Ambil detail produk |
| `POST` | `/api/products` | Buat produk baru dan sync stok awal ke inventory |
| `PUT` | `/api/products/{id}` | Update produk dan sync total stok ke inventory |
| `DELETE` | `/api/products/{id}` | Hapus produk dan sync hapus inventory |

Contoh body `POST` atau `PUT /api/products/{id}`:

```json
{
  "name": "Laptop Pro",
  "price": 15000000.0,
  "stock": 10,
  "categoryId": 1
}
```

Catatan:

- `POST /api/products` dan `PUT /api/products/{id}` mengirim event `product_sync_key`.
- Sinkronisasi ke inventory memperlakukan `stock` sebagai total stok produk. `inventory-service` akan menjaga `reservedStock` yang sudah ada.
- `DELETE /api/products/{id}` akan ditolak jika product sudah dipakai pada order, lalu mengirim event `product_delete_key` agar inventory ikut dihapus.

### Category API

| Method | Endpoint | Deskripsi |
| --- | --- | --- |
| `GET` | `/api/categories` | Ambil semua kategori |
| `GET` | `/api/categories/{id}` | Ambil detail kategori |
| `POST` | `/api/categories` | Buat kategori baru |
| `PUT` | `/api/categories/{id}` | Update kategori |
| `DELETE` | `/api/categories/{id}` | Hapus kategori |

### Customer API

| Method | Endpoint | Deskripsi |
| --- | --- | --- |
| `GET` | `/api/customers` | Ambil semua customer |
| `GET` | `/api/customers/{id}` | Ambil detail customer |
| `POST` | `/api/customers` | Buat customer baru |
| `PUT` | `/api/customers/{id}` | Update customer |
| `DELETE` | `/api/customers/{id}` | Hapus customer |

Contoh body `POST` atau `PUT /api/customers/{id}`:

```json
{
  "name": "Bram",
  "email": "bram@example.com",
  "address": "Jl. Soekarno Hatta No. 10, Malang"
}
```

### Order API

| Method | Endpoint | Deskripsi |
| --- | --- | --- |
| `POST` | `/api/orders?customerId={id}` | Buat order baru dan publish event awal ke inventory |
| `GET` | `/api/orders` | Ambil semua order |
| `GET` | `/api/orders/{id}` | Ambil detail order |
| `PUT` | `/api/orders/{id}/status?status={STATUS}` | Update status manual untuk status non-final |
| `POST` | `/api/orders/{id}/cancel` | Batalkan order dan publish rollback event |

Contoh body `POST /api/orders?customerId=1`:

```json
[
  { "productId": 1, "quantity": 2 },
  { "productId": 2, "quantity": 1 }
]
```

Catatan penting:

- `PUT /api/orders/{id}/status` sekarang hanya aman untuk status non-final seperti `PROCESSING`.
- Status `CANCELLED` harus lewat `POST /api/orders/{id}/cancel`.
- Status `SHIPPED` hanya boleh berasal dari event `order_shipped_key` yang dikirim oleh `shipping-service`.
- Status `FAILED` hanya boleh berasal dari event `order_reserve_failed_key` yang dikirim oleh `inventory-service`.
- `POST /api/orders/{id}/cancel` sudah diberi guard agar order yang sudah `CANCELLED`, `SHIPPED`, atau `FAILED` tidak diproses ulang.

## RabbitMQ yang Dipakai oleh Service Ini

Producer dari `order-management`:

- `product_sync_key`
- `product_delete_key`
- `order_routing_key`
- `order_cancel_key`

Consumer di `order-management`:

- `order_reserve_failed_key`
- `order_shipped_key`

Flow utama:

1. `POST /api/products` atau `PUT /api/products/{id}`
   - publish `product_sync_key`
   - inventory membuat atau menyinkronkan stok
2. `DELETE /api/products/{id}`
   - publish `product_delete_key`
   - inventory menghapus row inventory terkait
3. `POST /api/orders?customerId={id}`
   - publish `order_routing_key`
   - inventory reserve stok
   - jika reserve sukses, inventory mengirim `stock_reserved_key` ke shipping
   - jika reserve gagal, inventory mengirim `order_reserve_failed_key` ke service ini
4. `POST /api/orders/{id}/cancel`
   - publish `order_cancel_key`
   - inventory release stok
   - shipping cancel shipment
5. `PUT /api/shipments/{id}/status?status=SHIPPED`
   - event dari shipping diterima di service ini
   - status order diubah menjadi `SHIPPED`

## Endpoint Manual vs Flow Utama

Flow utama yang sebaiknya dipakai untuk demo arsitektur:

- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `POST /api/orders?customerId={id}`
- `POST /api/orders/{id}/cancel`
- `PUT /api/shipments/{id}/status?status=SHIPPED`

Endpoint manual yang tetap ada untuk requirement tugas, tetapi bukan jalur bisnis utama:

- `PUT /api/orders/{id}/status?status={STATUS}`

Endpoint ini tidak mengirim event ke service lain, jadi jangan dipakai untuk `CANCELLED` atau `SHIPPED`.

## Catatan Desain

- `POST /api/orders` sekarang lebih aman karena shipping baru membuat shipment setelah inventory benar-benar berhasil reserve stok.
- Order tetap disimpan lebih dulu, tetapi jika reserve gagal, status akan berubah menjadi `FAILED` dan rollback otomatis dipublish.
- `DELETE /api/products/{id}` sekarang sudah mengirim event penghapusan ke inventory, tetapi tetap diblok jika product sudah pernah dipakai pada order.

## Dokumentasi Terkait

- `../README.md`
- `../DAFTAR_ENDPOINT_LENGKAP.md`
- `../PANDUAN_SIMULASI_DEMO.md`
- `../ANALISIS_RABBITMQ_DEMO.md`
- `../AUDIT_ENDPOINT_DESYNC.md`
