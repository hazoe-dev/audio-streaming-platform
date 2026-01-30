# Run docker compose

## 1️⃣ `docker compose up --build` 

Khi bạn chạy:

```bash
docker compose up --build
```

Docker làm **liền một mạch**:

1. Build image (API)
2. Pull image (Postgres)
3. Tạo containers
4. Start containers
5. Attach log ra terminal

👉 Nên:

* Container **đã tồn tại**
* Container **đang chạy**


## 2️⃣ Check nhanh để chắc chắn

### ✅ Xem container đang chạy

```bash
docker compose ps
```

Ví dụ output:

```
NAME                    STATUS
audiostreaming-postgres Up (healthy)
audiostreaming-api      Up
```


### ✅ Xem log 

```bash
docker compose logs -f
```

Hoặc chỉ API:

```bash
docker compose logs -f api
```


## 3️⃣ Các lệnh

### 🔹 Chạy nền (detach)

Nếu bạn muốn **thoát terminal mà container vẫn chạy**:

```bash
docker compose up -d
```


### 🔹 Rebuild lại API

Khi bạn sửa code:

```bash
docker compose up --build
```

hoặc:

```bash
docker compose up -d --build
```


### 🔹 Stop container

```bash
docker compose down
```


### 🔹 Stop + xoá volume (xoá DB)

```bash
docker compose down -v
```

⚠️ **Cẩn thận**: mất dữ liệu Postgres


## 4️⃣ Flow 

```
docker compose up
      ↓
image (build/pull)
      ↓
container (create)
      ↓
container (running)
```

👉 **Không có bước “chạy container” riêng**

## 5️⃣ Quick sanity check cho project của bạn

Sau khi up xong:

* 🌐 API: [http://localhost:8080](http://localhost:8080)
* ❤️ Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
* 🐘 DB: chạy healthy


## Tổng kết

> `docker compose up --build`  
> 👉 **build + create + start containers luôn**

