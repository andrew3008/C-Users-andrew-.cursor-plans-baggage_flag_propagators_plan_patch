# Результат реализации baggage flag propagators

Дата: 28 октября 2026 года. Базовый commit: `c7941755919dd008da10a8dc8a0c8045cb5efb14`.

Выполнены и отмечены первые четыре пункта `baggage_flag_propagators_bef836ab.plan.md`.

1. Добавлен единый контракт допустимых имён `otel.propagators`: `tracecontext`, `baggage`, `b3`, `b3multi`, `platform-trace-control`, `none`. Неподдерживаемые имена отклоняются при подготовке сессии с `CONFIGURATION_INITIALIZATION_FAILED` и `propertyName=otel.propagators`. Зависимость сторонних пропагаторов доступна тестам только во время исполнения.
2. `BaggageKeyPolicy` объединяет allowlist и deny-паттерны. Регулярные выражения проверяются при чтении конфигурации. Зарезервированный `platform.correlation.id` запрещён в outbound allowlist независимо от регистра и удалён из его дефолта. Имена baggage-атрибутов span строятся из нормализованного ключа.
3. `PlatformBaggagePropagator` заменяет два прежних декоратора. Зарезервированный ключ удаляется в обе стороны; outbound policy применяется только при inject. Неизменённый Context сохраняет identity. `SafeTextMapPropagator` защищает только W3C delegate.
4. Добавлены и перенесены контрактные тесты, SDK-проверки каждого разрешённого пропагатора при обоих состояниях флага и проверки структурированной диагностики ошибок конфигурации. Сохранены тесты W3C-кодирования, metadata и лимитов baggage.

Независимое ревью усилило тест allowlist ключом, не совпадающим с deny-паттернами, и тестом ошибки getter, защищающим порядок декораторов. Список SPI проверен по локальному агенту OpenTelemetry 2.32.0. Исправлена старая bootstrap-фикстура: отсутствующий список теперь возвращает пустой список согласно контракту ConfigProperties; production fallback не добавлялся.

Кодировка существующих файлов сохранена. Новые Java-файлы — UTF-8 без BOM. Комментарии на русском, новые gwt-комментарии — `given`, `when`, `then`; сообщения исключений на английском. Аннотации Deprecated не добавлялись. Архитектурные правила не ослаблялись.

## Проверки

| Проверка | Результат |
| --- | --- |
| extension:compileJava | PASS |
| extension:compileTestJava | PASS |
| extension:test, включая ArchUnit | 1980 тестов, 0 failures, 0 errors, 0 skipped |
| distribution:controlledAgentRuntimeTest, InvalidExtensionConfigurationTest | 9 тестов, 0 failures, 0 errors, 0 skipped |
| declarative_configuration_file_fails_bootstrap_without_platform_export | PASS |
| git diff --check | PASS |
| Wildcard imports в extension/src | 0 |
| Проверка patch относительно рабочего дерева | PASS |

Команды Gradle:

```powershell
.\gradlew.bat :platform-starters-tracing:platform-tracing-otel-javaagent-extension:compileJava --offline --no-daemon
.\gradlew.bat :platform-starters-tracing:platform-tracing-otel-javaagent-extension:compileTestJava --offline --no-daemon
.\gradlew.bat :platform-starters-tracing:platform-tracing-otel-javaagent-extension:test -PrunTests --offline --no-daemon
.\gradlew.bat :platform-starters-tracing:platform-tracing-otel-javaagent-distribution:controlledAgentRuntimeTest --tests space.br1440.platform.tracing.otel.javaagent.distribution.InvalidExtensionConfigurationTest --offline --no-daemon
```

Первый полный прогон обнаружил нарушение контракта в тестовой фикстуре; приведённые XML и счётчики относятся к успешному повторному прогону после исправления. Общая квалификация всей платформы и gRPC не запускались. Документационный пункт 5 остаётся вне выполненного объёма. В исходном плане изменены только статусы первых четырёх пунктов; необходимые проверки из gate выполнены для подтверждения реализации.

## Состав архива

- `Platform_Traces/` — полный снимок отслеживаемых исходников с текущими изменениями и новыми файлами, без Git-каталога, кэшей и build-выходов; включены локальные правила AGENTS и Java imports.
- `changes.patch` — все изменения, включая новые и удалённые файлы, относительно указанного commit.
- `baggage_flag_propagators_bef836ab.plan.md` — план с четырьмя выполненными пунктами.
- `verification/` — XML-результаты обоих успешных прогонов.
- `source-manifest.json` — SHA-256 каждого файла снимка исходников.

Снимок следует распаковывать в отдельный каталог. Для существующего checkout можно применить `changes.patch` через `git apply --check` и `git apply`: patch удаляет старые декораторы и тесты, которые простое копирование файлов поверх checkout оставило бы на месте.
