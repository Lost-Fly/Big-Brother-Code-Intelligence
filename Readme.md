## Описание сервиса автоматического код ревью и оценки компетенций разработчиков Big Brother Code Intelligence

**Big Brother Code Intelligence** — это сервис для проведения автоматического код ревью, а также для автоматизированного анализа компетенций разработчиков на базе их активности в системах контроля версий (Git). Сервис может использоваться в HR-процессах для оценки навыков программистов на основе данных о коммитах и кода, при следовании за repository-driven подходом.

### Основная задача сервиса
С помощью анализа истории коммитов и кода разработчиков на Git, сервис оценивает знания и навыки разработчиков по различным категориям компетенций, таким как владение языками программирования, алгоритмами, навыки баз данных, брокеров сообщений и т.д. Для состоятельной оценки используется языковая модель, которая на основе заданных параметров и структуры принимает кодовые фрагменты и возвращает детализированные метрики компетенций.

## Основные процессы и алгоритмы сервиса

### 1. **Прием данных о разработчике и репозиториях**
- На вход через HTTP-запрос поступают данные о разработчике, включая его имя, персональные токены доступа (если требуются), ссылки на репозитории. Принимает эти данные контроллер `DeveloperController`.
- Эти данные дальше передаются на сервер для дальнейшей обработки через сервисный класс `DeveloperService`.

### 2. **Получение данных из Git**
- Компонент `GitIntegration`, используя библиотеку JGit, клонирует репозиторий и начинает процесс анализа коммитов. Важный момент заключается в необходимости аутентификации (при использовании личных токенов). Из каждого коммита извлекаются файлы, которые являются основой для дальнейшего анализа.

### 3. **Анализ файлов**
- Класс `RepositoryAnalyzer` обрабатывает каждый коммит с точки зрения кода разработчика.
- Файлы предварительно фильтруются, чтобы исключить нефункциональные файлы (например, конфигурационные файлы `.conf`, `.md`, `.yaml` и т.д.).
- Если файл слишком большой, он разбивается на несколько частей фиксированного размера.
- Для каждого файла или его части определяется его язык программирования на основе расширения.
- Разработанный файл отправляется для анализа в языковую модель (метод `analyzeCode` в классе `LLMIntegration`).

### 4. **Интеграция с языковой моделью (LLM)**
- Языковая модель получает код или его часть из файлов, а также информацию о языке программирования.
- Далее модель строит запрос в формат JSON, используя схемы для оценки компетенции. Языковая модель анализирует код на предмет сложных конструкций, алгоритмов, взаимодействий с базами данных и брокерами сообщений.
- По завершению анализа возвращаются метрики компетенций (набор параметров в виде JSON документа, содержащего оценки по разным компетенциям).

### 5. **Слияние оценок**
- Процесс слияния оценок важен при работе с большим количеством коммитов либо файлов. Для этого в сервисе предусмотрено многократное обращение к языковой модели, чтобы объединить предыдущие результаты с новыми.
- Результаты из разных файлов и коммитов объединяются в единую компетентностную матрицу на языке программирования на основе алгоритма глубокого дерева (deepTreeWalkMergingAlgorithm).

### 6. **Генерация отчета**
- Генерация отчета ведется сервисом `ReportGenerator`.
- В отчете представлена информация по каждому языку программирования, а также общий итоговый вывод.
- Помимо оценок за навыки программирования, в отчете содержится текстовое описание способностей разработчика, сформированное из данных компетентностной матрицы.

### 7. **Хранение и предоставление данных**
- Компонент `MongoDbClient` обеспечивает взаимодействие с NoSQL базой данных MongoDB.
- В базе данных хранятся как информация о разработчиках, так и результаты анализа их коммитов для дальнейшего доступа.

---

## Технологический стек
- **Kotlin:** основной язык разработки
- **Ktor:** легковесный фреймворк для написания сервера, поддерживающий асинхронные процессы.
- **MongoDB:** NoSQL база данных для хранения результатов анализа.
- **LLM:** интеграция с языковой моделью для анализа кода.
- **JGit:** библиотека для взаимодействия с Git-репозиториями.

## Диаграмма классов

```plaintext
+-------------------+        +---------------------------------+         +--------------------------+
|  DeveloperService |<------>|       RepositoryAnalyzer         |<--+--->|    GitIntegration         |
+-------------------+        +---------------------------------+   |     +--------------------------+
      |                                 |                            |          
      |                                 |                            |          
+-----------------+           +--------------------+             +--------------+       
| Developer       |           | LLMIntegration     |             | MongoDbClient|       
| class           |           | class              |             +--------------+ 
+-----------------+           +--------------------+                  |     
      |                                                                  |
+------------------------------------------------------------------+    |
| DeveloperRepository                                               |<--+
+------------------------------------------------------------------+

```

### Описание классов

#### `DeveloperService`
- **Роль**: Сервисный класс для организации взаимодействий между компонентами.
- **Задачи**: Управление основными бизнес-процессами (оценка разработчика).

#### `RepositoryAnalyzer`
- **Роль**: Сервисный класс для анализа репозиториев.
- **Задачи**: Анализировать коммиты, файлы и передавать код на анализ в языковую модель.

#### `GitIntegration`
- **Роль**: Интеграция с системами контроля версий.
- **Задачи**: Получение коммитов, извлечение файлов из репозиториев Git.

#### `LLMIntegration`
- **Роль**: Интеграция с языковой моделью.
- **Задачи**: Отправка кода на анализ и получение компетенций.

#### `MongoDbClient`
- **Роль**: Работа с базой данных MongoDB.
- **Задачи**: Хранение и получение информации о разработчиках и результатах анализа.
