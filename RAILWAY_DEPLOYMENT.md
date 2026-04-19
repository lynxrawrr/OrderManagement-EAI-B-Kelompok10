# Deploy ke Railway

Project ini terdiri dari tiga service Spring Boot yang dideploy terpisah di Railway:

- `order-management`
- `inventory-service`
- `shipping-service`

Selain itu, project ini membutuhkan:

- `3` service MySQL
- `1` service RabbitMQ

## Struktur Service di Railway

Saat membuat service dari repository GitHub yang sama, atur `Root Directory` seperti berikut:

- `order-management` -> `/order-management`
- `inventory-service` -> `/inventory-service`
- `shipping-service` -> `/shipping-service`

Agar deployment lebih efisien, tambahkan `Watch Paths` sesuai folder masing-masing service:

- `/order-management/**`
- `/inventory-service/**`
- `/shipping-service/**`

## Konfigurasi Database

Buat tiga database MySQL terpisah di Railway:

- `order-db`
- `inventory-db`
- `shipping-db`

Lalu pasang environment variable berikut pada masing-masing service aplikasi.

### order-management

```env
SPRING_DATASOURCE_URL=jdbc:mysql://${{order-db.MYSQLHOST}}:${{order-db.MYSQLPORT}}/${{order-db.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=${{order-db.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{order-db.MYSQLPASSWORD}}
SPRING_RABBITMQ_HOST=${{rabbitmq.RAILWAY_PRIVATE_DOMAIN}}
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=${{rabbitmq.RABBITMQ_DEFAULT_USER}}
SPRING_RABBITMQ_PASSWORD=${{rabbitmq.RABBITMQ_DEFAULT_PASS}}
```

### inventory-service

```env
SPRING_DATASOURCE_URL=jdbc:mysql://${{inventory-db.MYSQLHOST}}:${{inventory-db.MYSQLPORT}}/${{inventory-db.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=${{inventory-db.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{inventory-db.MYSQLPASSWORD}}
SPRING_RABBITMQ_HOST=${{rabbitmq.RAILWAY_PRIVATE_DOMAIN}}
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=${{rabbitmq.RABBITMQ_DEFAULT_USER}}
SPRING_RABBITMQ_PASSWORD=${{rabbitmq.RABBITMQ_DEFAULT_PASS}}
```

### shipping-service

```env
SPRING_DATASOURCE_URL=jdbc:mysql://${{shipping-db.MYSQLHOST}}:${{shipping-db.MYSQLPORT}}/${{shipping-db.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=${{shipping-db.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{shipping-db.MYSQLPASSWORD}}
SPRING_RABBITMQ_HOST=${{rabbitmq.RAILWAY_PRIVATE_DOMAIN}}
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=${{rabbitmq.RABBITMQ_DEFAULT_USER}}
SPRING_RABBITMQ_PASSWORD=${{rabbitmq.RABBITMQ_DEFAULT_PASS}}
```

## Konfigurasi RabbitMQ

Deploy RabbitMQ melalui template Railway. Gunakan `Railway Private Domain` agar komunikasi antar service tetap berada di private network Railway.

Catatan:

- pada local environment project ini sebelumnya menggunakan port `5673`
- pada Railway template RabbitMQ menggunakan port AMQP `5672`
- karena itu nilai `SPRING_RABBITMQ_PORT` pada Railway harus diisi `5672`

## Port Aplikasi

Project ini sudah dikonfigurasi agar otomatis mengikuti environment variable `PORT` dari Railway, dengan fallback port lokal:

- `order-management` -> `8080`
- `inventory-service` -> `8081`
- `shipping-service` -> `8082`

## Domain Public

Minimal generate public domain untuk `order-management` jika service ini dijadikan API utama untuk pengujian. `inventory-service` dan `shipping-service` bisa tetap private kecuali memang ingin diakses langsung dari luar.

## Catatan Lokal

Secret database lokal tidak lagi di-hardcode di repository. Untuk menjalankan project secara lokal, set environment variable seperti `SPRING_DATASOURCE_PASSWORD` jika MySQL lokal menggunakan password.
