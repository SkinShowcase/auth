# Auth (Skins Showcase)

Сервис аутентификации: **Steam OpenID → JWT (HS256)**, профиль пользователя, приватность, trade-link, пресеты аватарок, юридические документы, админ-операции.

Репозиторий: https://github.com/SkinShowcase/auth  
Полный стек в Docker: https://github.com/SkinShowcase/infrastructure

## Порт

- Локально (`application.yml`): `server.port` = **8081**
- В Docker-профиле `docker` (`application-docker.yml`): **8080** внутри контейнерной сети (наружу маппится в `docker-compose.yml` инфраструктуры)

## Модель данных и «хеширование»

В таблице `users` поле `steam_id` — это **не SteamID64 в открытом виде**, а **SHA-256 hex** от SteamID64 (см. `UserService` / `UserDataHashingService`).  
Наружу API всегда отдаёт **настоящий SteamID64** (из JWT subject или из логики резолва).

Дополнительно:

- `display_name_hash` — SHA-256 от текущего `display_name` (для уникальности/поиска без хранения «сырого» дубля).
- Таблица `user_report`: `reporter_steam_id` / `reported_steam_id` хранятся как **SHA-256 hex**, не как SteamID64.

`steam_trade_link` хранится **как введено пользователем** (это секретная ссылка с token partner) — не логируйте и не кешируйте публично.

## JWT

- Подпись: HS256, subject = SteamID64.
- Секрет: `AUTH_JWT_SECRET` (**обязателен в prod**, минимум 32 байта для стабильной работы HS256).
- TTL: `AUTH_JWT_EXPIRATION_MS` (по умолчанию 24ч).

## Steam OpenID

- `GET /auth/steam` → редирект в Steam.
- `GET /auth/steam/callback` → проверка у Steam, создание пользователя, JWT, редирект на фронт: `{AUTH_FRONTEND_REDIRECT_URL}#token=...`
- `AUTH_STEAM_REALM` и `AUTH_STEAM_RETURN_TO` должны указывать на **публичный** URL (обычно gateway), иначе Steam/OpenID сломается.

## Публичные HTTP эндпоинты (без JWT)

- `GET /auth/steam`, `GET /auth/steam/callback`
- `GET /auth/avatars` — список пресетов `{id,url}`
- `GET /auth/avatars/{presetId}` — JPEG пресета
- `GET /auth/documents` — список юридических документов (метаданные)
- `GET /auth/documents/{slug}?version=` — тело документа

## Эндпоинты с JWT (`Authorization: Bearer …`)

Источник истины: `AuthController`.

- `GET /auth/session` — лёгкая проверка токена + блокировки: `204` / `401` / `403`
- `GET /auth/me` — профиль (`MeResponseDto`: steamId, displayName, privateProfile, successfulTradesCount, lastOnlineAt, steamTradeLink, effectiveAvatarUrl, selectedPresetAvatarId, avatarSource, blocked)
- `GET|PATCH /auth/me/privacy` — читать / переключить приватность (PATCH без тела)
- `PATCH /auth/me/last-online` — heartbeat онлайна
- `PATCH /auth/me/trade-link` — тело `{"tradeUrl":"..."}` или пусто/null для сброса
- `GET /auth/users/{steamId}/trade-link` — чужой trade-link: **404**, если профиль приватный и вы не владелец
- `GET /auth/users/by-username/{username}` — резолв SteamID по **SteamID64** или `display_name` (с учётом приватности)
- `POST /auth/users/preset-avatar-ids` — пакетно preset id по списку steamId (для списка чатов в messaging)
- `PATCH /auth/me/display-name` — смена отображаемого имени
- `PATCH /auth/me/avatar` — смена пресетной аватарки
- `POST /auth/users/{steamId}/report` — репорт пользователя (с cooldown; в БД — хеши steam id)

## Админ API

`AuthAdminController` (`/auth/admin/**`), заголовок `X-Admin-Api-Key` = `ADMIN_API_KEY`.

- `GET /auth/admin/reports`
- `POST /auth/admin/users/{steamId}/block`
- `POST /auth/admin/users/{steamId}/unblock`

## Внутренний API (межсервисный)

`AuthInternalUserController` (`/auth/internal/users/**`), заголовок `X-Internal-Service-Key` = `AUTH_INTERNAL_SERVICE_KEY`  
(если ключ **не задан** в окружении — проверка отключена, только для dev).

- `GET /auth/internal/users/{steamId}/privacy`
- `POST /auth/internal/users/privacy-flags` — батч флагов приватности
- `POST /auth/internal/users/profile-labels` — батч «лейблов» для отображения (используется steam-gateway/trades)

## Интеграции

- `AUTH_STEAM_GATEWAY_BASE_URL` — запрос сводки профиля Steam при логине (см. `SteamGatewayProfileClient`).
- `AUTH_PUBLIC_API_BASE_URL` — база для абсолютных URL пресетов в `/auth/me` (обычно gateway).

## Запуск локально

```bash
./gradlew bootRun
# или с дефолтным dev-секретом из application-local.yml:
./gradlew bootRun --args="--spring.profiles.active=local"
```

## Docker

```bash
docker build -t skins-showcase/auth .
# Важно: внутри JAR порт задаётся Spring'ом (docker-профиль auth слушает 8080).
docker run --rm -p 8081:8080 -e SPRING_PROFILES_ACTIVE=docker skins-showcase/auth
```
