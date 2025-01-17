# README: Сервис сокращения ссылок

## Описание
Этот сервис предоставляет API для создания коротких ссылок, управления их параметрами, получения статистики и удаления ссылок. Каждая ссылка имеет ограничения по количеству переходов и времени жизни.

---

## Как пользоваться сервисом

### 1. Запуск сервиса
Убедитесь, что у вас установлен Java и Maven.

1. Склонируйте проект:
   ```bash
   git clone <ссылка-на-репозиторий>
   cd <папка-с-проектом>
   ```

2. Запустите сервис с помощью Maven:
   ```bash
   mvn spring-boot:run
   ```

Сервис будет доступен по адресу: `http://localhost:8080`

---

## Поддерживаемые команды

### Создание короткой ссылки
**Метод:** `POST`
**URL:** `/api/shorten`

**Параметры запроса:**
- `originalUrl` (обязательно): оригинальная ссылка.
- `maxClicks` (необязательно): максимальное количество переходов (по умолчанию: 10).
- `expiryDurationInHours` (необязательно): время жизни ссылки в часах (по умолчанию: 24).
- `userId` (необязательно): идентификатор пользователя. Если не указан, генерируется новый.

**Пример запроса:**
```bash
curl -X POST "http://localhost:8080/api/shorten" \
-d "originalUrl=https://example.com" \
-d "maxClicks=5" \
-H "userId:your-uuid"
```

**Пример ответа:**
```json
{
  "shortUrl": "http://localhost:8080/api/d0c48dfd",
  "userId": "1f01cce7-849d-4880-9ed6-522b3b85d9ae"
}
```

---

### Получение статистики по ссылке
**Метод:** `GET`
**URL:** `/{id}/stats`

**Пример запроса:**
```bash
curl "http://localhost:8080/api/d0c48dfd/stats" \
-H "userId:1f01cce7-849d-4880-9ed6-522b3b85d9ae"
```

**Пример ответа:**
```json
{
  "expiryTime": "2025-01-18T23:35:13.792832",
  "clickCount": 0,
  "originalUrl": "https://example.com",
  "maxClicks": 5
}
```

---

### Обновление максимального количества переходов
**Метод:** `PUT`
**URL:** `/update/{id}`

**Параметры запроса:**
- `newMaxClicks`: новое максимальное количество переходов.
- Заголовок `userId`: идентификатор пользователя.

**Пример запроса:**
```bash
curl -X PUT "http://localhost:8080/api/update/d0c48dfd" \
-d "newMaxClicks=10" \
-H "userId:1f01cce7-849d-4880-9ed6-522b3b85d9ae"
```

**Пример ответа:**
```json
{
  "message": "Max clicks updated successfully."
}
```

---

### Переход по ссылке
**Метод:** `GET`
**URL:** `/{id}`

**Пример запроса:**
```bash
curl "http://localhost:8080/api/d0c48dfd"
```

**Пример ответа:**
```text
Redirecting to: https://example.com
```

---

### Удаление ссылки
**Метод:** `DELETE`
**URL:** `/delete/{id}`

**Пример запроса:**
```bash
curl -X DELETE "http://localhost:8080/api/delete/d0c48dfd" \
-H "userId:1f01cce7-849d-4880-9ed6-522b3b85d9ae"
```

**Пример ответа:**
```json
{
  "message": "Link deleted successfully."
}
```

---

