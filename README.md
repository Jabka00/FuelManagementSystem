# Fuel Management System

Комплексна система обліку та управління паливом для автотранспортних підприємств. Проєкт складається з трьох основних компонентів: REST API сервера, десктопного клієнта та мобільного Android-додатку.

## Архітектура проєкту

```
FuelManagementSystem/
├── FuelManagementAPI/        # REST API сервер (Spring Boot)
├── FuelManagementApp/        # Мобільний додаток (Android)
├── FuelManagementDesktop/    # Десктопний клієнт (JavaFX)
└── IndarFuel/                # Додаткові конфігурації
```

## Технології

| Компонент | Технології |
|-----------|------------|
| **API** | Java 17, Spring Boot 3.2, Spring Data JPA, MySQL |
| **Desktop** | Java 22, JavaFX 22, HikariCP, OkHttp, Apache POI |
| **Mobile** | Java 11, Android SDK 34, Retrofit 2, Firebase |

## Функціональність

### Основні можливості
- Облік рейсів та маршрутів транспортних засобів
- Розрахунок витрат палива з урахуванням сезонності
- Управління автопарком (транспортні засоби, водії)
- Інтеграція з Google Maps API для розрахунку маршрутів
- Генерація звітів у форматі Excel
- Моніторинг email для автоматичного створення заявок

### Десктопний клієнт
- Повнофункціональний інтерфейс для диспетчерів
- Створення та редагування рейсів
- Перегляд статистики та історії
- Управління водіями та транспортом
- Експорт даних у Excel

### Мобільний додаток
- Перегляд активних рейсів
- Фіксація початку та завершення рейсу
- Перегляд інформації про водіїв та транспорт

---

## Встановлення та налаштування

### Вимоги

- **Java**: JDK 17+ (для API), JDK 22+ (для Desktop)
- **Android Studio**: Arctic Fox або новіше (для Mobile)
- **MySQL**: 8.0+
- **Maven**: 3.8+

### 1. База даних

Створіть базу даних MySQL:

```sql
CREATE DATABASE fuel_management 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;
```

### 2. FuelManagementAPI

```bash
cd FuelManagementAPI

# Скопіюйте та налаштуйте конфігурацію
cp .env.example .env
# Відредагуйте .env з вашими даними підключення до БД

# Запуск
./mvnw spring-boot:run
```

**Конфігурація `.env`:**
```properties
DATABASE_URL=jdbc:mysql://localhost:3306/fuel_management?useSSL=false&serverTimezone=Europe/Kiev
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password
PORT=8080
```

API буде доступний за адресою: `http://localhost:8080`

### 3. FuelManagementDesktop

```bash
cd FuelManagementDesktop

# Скопіюйте та налаштуйте конфігурацію
cp .env.example .env
# Відредагуйте .env

# Запуск
./mvnw javafx:run
```

**Конфігурація `.env`:**
```properties
# База даних
DATABASE_URL=jdbc:mysql://localhost:3306/fuel_management
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password

# Google Maps API (для розрахунку маршрутів)
GOOGLE_MAPS_API_KEY=your_api_key

# Email (для моніторингу заявок)
EMAIL_SERVER=imap.gmail.com
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
```

### 4. FuelManagementApp (Android)

```bash
cd FuelManagementApp

# Скопіюйте та налаштуйте конфігурацію
cp local.properties.example local.properties
# Відредагуйте local.properties

# Для Firebase (опціонально)
# Додайте google-services.json у папку app/
```

**Конфігурація `local.properties`:**
```properties
sdk.dir=/path/to/Android/sdk
API_BASE_URL=http://your-server-ip:8080/api/v1/
```

Відкрийте проєкт в Android Studio та запустіть на емуляторі або пристрої.

---

## API Endpoints

### Рейси (Trips)

| Метод | Endpoint | Опис |
|-------|----------|------|
| GET | `/api/v1/trips` | Отримати всі рейси |
| GET | `/api/v1/trips/{id}` | Отримати рейс за ID |
| POST | `/api/v1/trips` | Створити новий рейс |
| PUT | `/api/v1/trips/{id}/start` | Розпочати рейс |
| PUT | `/api/v1/trips/{id}/finish` | Завершити рейс |

### Водії (Drivers)

| Метод | Endpoint | Опис |
|-------|----------|------|
| GET | `/api/v1/drivers` | Отримати всіх водіїв |
| GET | `/api/v1/drivers/{id}` | Отримати водія за ID |

### Транспорт (Vehicles)

| Метод | Endpoint | Опис |
|-------|----------|------|
| GET | `/api/v1/vehicles` | Отримати всі транспортні засоби |
| GET | `/api/v1/vehicles/{id}` | Отримати транспорт за ID |

---

