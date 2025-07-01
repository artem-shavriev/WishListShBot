# WishListShBot 🎁🤖

Telegram-бот для создания и управления вишлистами (списками желаний).
Позволяет добавлять желаемые подарки, делиться своими списками с друзьями и отмечать подарки как "забронированные".

## 🔹 Функционал

- 📝 Создание и редактирование вишлистов
- � Добавление/удаление подарков со ссылками
- 👥 Просмотр вишлистов друзей
- ✅ Отметка подарков как "забронированных"

## 🚀 Технологии

- **Java 22** - основной язык разработки
- **Spring Boot** - фреймворк для backend-разработки
- **Telegram Bot API** - интеграция с Telegram
- **PostgreSQL** - хранение данных
- **Maven** - управление зависимостями

## 🛠 Установка и запуск

### Требования:
- Java 17+
- PostgreSQL 14+
- Telegram bot token (получить у [@BotFather](https://t.me/BotFather))

### Инструкция:

1. Клонировать репозиторий:

 ```bash
git clone https://github.com/artem-shavriev/WishListShBot.git
cd WishListShBot
```

2. Настроить подключение к БД в application.yml:
```yaml
spring:
  datasource:
  url: jdbc:postgresql://localhost:5432/wishlist_db
  username: your_username
  password: your_password
  ```

3. Указать Telegram bot token:

```yaml
telegram:
  bot:
    token: YOUR_BOT_TOKEN
    username: YourBotUsername
```
4. Запустить приложение:

```bash
mvn spring-boot:run
```

## 📄 Конфигурация
Основные настройки можно изменить в файле application.yml:

- Порт сервера
- Настройки базы данных
- Логирование
- Параметры бота