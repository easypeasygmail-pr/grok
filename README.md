# Odpisz.de

Aplikacja na Androida dla Polaków w Niemczech. Robi zdjęcie niemieckiego pisma urzędowego, mówi po polsku kto pisze, czego chce, ile i do kiedy, i składa niemiecki list z faktów, które wpiszesz.

Kod leży w katalogu głównym tego repozytorium (`app`, `core`, `feature`). Zestaw reguł dla agentów (`AGENTS.md`, skille, prompty) zostaje obok i nie został przeniesiony.

Identyfikator pakietu: `de.odpisz.app`. Nazwa na telefonie: **Odpisz.de**.

## Jak zainstalować wersję testową na telefonie

Nie potrzebujesz Android Studio ani konta programisty. Potrzebujesz telefonu z Androidem (od wersji 8) i przeglądarki.

1. Otwórz to repozytorium na GitHubie.
2. Wejdź w zakładkę **Actions**.
3. Po lewej kliknij workflow **Android**.
4. Kliknij ostatnie uruchomienie z zielonym znaczkiem (albo uruchomienie na swoim pull requeście).
5. Na dole strony, w sekcji **Artifacts**, pobierz **odpisz-debug-apk**.
6. Na komputerze rozpakuj plik zip. W środku jest `app-debug.apk`.
7. Prześlij ten plik na telefon (mailem do siebie, kablem albo Dyskiem Google).
8. Na telefonie otwórz plik. Android zapyta, czy wolno instalować aplikacje z tego źródła (przeglądarka albo Menedżer plików). Zezwól.
9. Potwierdź instalację. Ikona nazywa się Odpisz.de.
10. Przy pierwszej instalacji system może napisać, że aplikacja jest niepodpisana sklepem. To normalne dla tej wersji testowej.

Gdy Google Play Protect zablokuje instalację, wybierz „Zainstaluj mimo to” / „Install anyway”. To nadal jest Twoja własna kompilacja z GitHuba, nie plik ze sklepu.

## Co w tej wersji jest prawdziwe, a co jest próbką

Prawdziwe, na telefonie, bez wysyłania pisma na serwer:

- trzy karty startowe, w tym zgoda na dane szczególnej kategorii (art. 9 RODO) i informacja, że to nie jest porada prawna,
- maskowanie IBAN, niemieckiego identyfikatora podatkowego, telefonu, e-maila, kodu pocztowego oraz imienia i adresu z profilu,
- odczyt tekstu ze zdjęcia i PDF (ML Kit, na urządzeniu) oraz import przez udostępnianie,
- lista pism, terminy z cytatem, potwierdzenie albo poprawka daty, kalendarz i lokalne przypomnienia (7 dni, 2 dni, w dniu terminu),
- szablony listów po niemiecku i polskie tłumaczenie zwrotne szablonu, PDF w układzie zbliżonym do DIN 5008,
- czerwona kartka „idź do poradni”, która blokuje generator przy sądzie, komorniku, nakazie zapłaty, pobycie i merytorycznym odwołaniu podatkowym,
- blokada biometryczna i „Usuń wszystkie dane”.

Nadal sztuczne, aż do fazy z backendem:

- analiza treści nie woła modelu. Rozpoznaje słowa kluczowe i wyciąga daty, kwoty i znak sprawy, a potem opisuje pismo po polsku według stałych reguł. To próbka, nie prawdziwe zrozumienie pisma.
- fakty wpisane po polsku trafiają do niemieckiego listu w cudzysłowie. Aplikacja ich nie tłumaczy i nie dopisuje argumentu prawnego.
- w ustawieniach Impressum i polityka prywatności są oznaczone jako TODO. Właściciel musi wpisać swoje dane prawne, zanim aplikacja pójdzie do sklepu.
- nie ma płatności ani konta.

W aplikacji są dwa przykładowe pisma (Finanzamt oraz sąd), żeby dało się przejść cały przepływ bez aparatu.

## Moduły

| Moduł | Rola |
| --- | --- |
| `app` | Jedna aktywność, nawigacja, intencja „udostępnij” |
| `core:model` | Pismo, termin, kwota, szkic odpowiedzi, token PII |
| `core:privacy` | Maskowanie i przywracanie na urządzeniu |
| `core:network` | Interfejsy `LetterAnalyzer` i `ReplyGenerator`, próbka offline, schemat JSON |
| `core:database` | Room i SQLCipher, klucz z Android Keystore |
| `core:ocr` | ML Kit Document Scanner, rozpoznawanie tekstu, PDF przez PdfRenderer |
| `core:pdf` | List PDF zbliżony do DIN 5008 |
| `core:data` | Repozytoria, WorkManager, kalendarz i udostępnianie |
| `core:designsystem` | Material 3, duża typografia, jasny i ciemny motyw |
| `feature:*` | Onboarding, skan, przegląd zamaskowanego tekstu, wynik, odpowiedź, czerwona kartka, historia, ustawienia |

Schemat JSON analizy: `core/network/src/main/resources/schema/letter-analysis.schema.json`. W aplikacji nie ma klucza API. Klient HTTP (Ktor) jest przygotowany z timeoutami, ale faza 0 podpina implementację offline.

# Zestaw startowy AI: natywny Android (Kotlin)

Poniżej zostaje opis zestawu reguł, skilli i promptów. Agenty piszą Kotlin tak: Jetpack Compose, Material 3, jedna aktywność, MVVM.

Właściciel opisuje pracę po polsku. Ten plik, [prompty](prompts/README.md) i [instrukcja MCP](mcp/README.md) są po polsku. [`AGENTS.md`](AGENTS.md) i skille są po angielsku — agenty trzymają się ich ściślej — z krótką polską notką na górze własnych skilli.

## Co tu jest

| Element | Po co |
| --- | --- |
| [`AGENTS.md`](AGENTS.md) | Obowiązujący stos i kolejność pracy: plan, tokeny, ekran, build, zrzut. |
| [`.cursor/skills/`](.cursor/skills/compose-screen-from-design/SKILL.md) i [`.claude/skills/`](.claude/skills/compose-screen-from-design/SKILL.md) | Skille agentów. Trzy własne oraz oficjalne skille Google, podlinkowane z [snapshotu](third_party/README.md). |
| [`prompts/`](prompts/README.md) | Sześć gotowych promptów do wklejenia. |
| [`mcp/`](mcp/README.md) | Mobile MCP, Figma, Mobbin. Bez kluczy w repozytorium. |
| [`third_party/android-skills/`](third_party/android-skills/LICENSE.txt) | Niezmieniony snapshot [android/skills](https://github.com/android/skills), Apache 2.0. |
| [`scripts/link-agent-skills.sh`](scripts/link-agent-skills.sh) | Odtwarza symlinki skilli po aktualizacji snapshotu. |

Stos, którego broni `AGENTS.md`: [Jetpack Compose](https://developer.android.com/jetpack/compose), [Material 3](https://developer.android.com/develop/ui/compose/designsystems/material), [nawigacja w Compose](https://developer.android.com/develop/ui/compose/navigation), [architektura UI](https://developer.android.com/topic/architecture), [Hilt](https://developer.android.com/training/dependency-injection/hilt-android), [korutyny](https://developer.android.com/kotlin/coroutines), [Room](https://developer.android.com/training/data-storage/room), [Retrofit](https://github.com/square/retrofit) albo [Ktor Client](https://ktor.io/docs/client-create-and-configure.html), [version catalog](https://developer.android.com/build/migrate-to-catalogs), [edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge), [dostępność w Compose](https://developer.android.com/develop/ui/compose/accessibility), [testy Compose](https://developer.android.com/develop/ui/compose/testing), [detekt](https://detekt.dev/), [ktlint](https://ktlint.github.io/ktlint/latest/).

## Czego tu nie ma w zestawie reguł

Sam zestaw reguł nie jest osobną aplikacją. Aplikacja Odpisz.de jest już w tym repozytorium, w katalogu głównym, razem z Gradle Wrapperem. Zestaw (`AGENTS.md`, skille, prompty, MCP) leży obok kodu i nie należy go przenosić.

## Nowy projekt

1. Utwórz projekt w Android Studio.
2. Skopiuj do jego katalogu (nie nadpisuj README aplikacji — ten plik opisuje zestaw):

   `AGENTS.md`, `.cursor/`, `.claude/`, `.mcp.json`, `prompts/`, `mcp/`, `third_party/`, `scripts/`, `.gitattributes`. `.gitignore` scal z tym, który już ma aplikacja — nie podmieniaj go w całości, jeśli trzyma reguły projektu.

   Symlinki w `.cursor/skills` i `.claude/skills` mają zostać symlinkami. `cp -a` na macOS i Linuksie je zachowuje. Na Windowsie patrz niżej.
3. Otwórz projekt w Cursorze. Wejdź w Settings → Tools & MCP i połącz **figma** oraz **mobbin** (przeglądarka, OAuth). **mobile-mcp** startuje przez `npx`, bez klucza.
4. Włącz emulator (`adb devices` pokazuje urządzenie). Szczegóły: [mcp/README.md](mcp/README.md).
5. Wklej prompt z [`prompts/`](prompts/README.md).

Cursor czyta `.cursor/skills` i `AGENTS.md`. Claude Code czyta `.claude/skills` i [`.mcp.json`](.mcp.json). Codex: skopiuj fragment z [`mcp/codex.config.toml`](mcp/codex.config.toml) do `~/.codex/config.toml` albo zainstaluj plugin Google opisany w [third_party/README.md](third_party/README.md).

## Istniejący projekt

1. Skopiuj te same ścieżki. Gdy masz już `.cursor/mcp.json` albo `.mcp.json`, **scal** obiekty `mcpServers` z [`.cursor/mcp.json`](.cursor/mcp.json) i [`.mcp.json`](.mcp.json). Nie kasuj innych serwerów.
2. Gdy masz własny `AGENTS.md`, dopisz odsyłacz do reguł tego zestawu albo przenieś sekcje, których w projekcie nie ma (stany ekranu, tokeny, pętla ze zrzutem). Nie wymieniaj działającej architektury hurtowo.
3. Domyślna nawigacja zestawu to Navigation Compose. Navigation 3 jest w skillu `navigation-3` — użyj go, gdy projekt już na nim stoi albo świadomie migrujesz.
4. Skille Google nie ruszają kodu same z siebie. Agent sięga po nie, gdy zadanie pasuje do opisu w `SKILL.md`.

## Skille tego zestawu

Kanoniczna treść jest w `.cursor/skills/<nazwa>/SKILL.md`. `.claude/skills/<nazwa>` to symlink do niej.

| Skill | Kiedy |
| --- | --- |
| [`compose-screen-from-design`](.cursor/skills/compose-screen-from-design/SKILL.md) | Ekran z Figmy, Mobbin albo zrzutu. Stany, tokeny, ViewModel, różnice względem referencji. |
| [`design-system-tokens`](.cursor/skills/design-system-tokens/SKILL.md) | Material 3: schemat jasny i ciemny, typografia, kształty, odstępy 4/8/16/24/32. Rozszerza istniejący motyw. |
| [`verify-on-emulator`](.cursor/skills/verify-on-emulator/SKILL.md) | Build debug, instalacja, zrzut, ocena insetów i kontrastu. Kompilacja nie zamyka zadania. |

Format pliku jest zgodny ze [specyfikacją Agent Skills](https://agentskills.io/specification): katalog, `SKILL.md`, frontmatter `name` i `description`.

## Oficjalne skille Androida

Źródło: [github.com/android/skills](https://github.com/android/skills), licencja [Apache 2.0](third_party/android-skills/LICENSE.txt). Pin: [`6daa53ef27fa2aa87827d183a85c06cedceca833`](https://github.com/android/skills/commit/6daa53ef27fa2aa87827d183a85c06cedceca833) (2026-10-29, plugin v1.0.19). Jak podmienić snapshot i jak doinstalować skille przez Android CLI albo marketplace: [third_party/README.md](third_party/README.md). Przegląd na developer.android.com: [Android skills](https://developer.android.com/tools/agents/android-skills), [lista](https://developer.android.com/tools/agents/android-skills/browse).

Każdy skill jest też widoczny dla agenta jako `.cursor/skills/<nazwa>` i `.claude/skills/<nazwa>`.

### Build system

- [`agp-9-upgrade`](third_party/android-skills/build-system/agp/agp-9-upgrade/SKILL.md) — migracja do Android Gradle Plugin 9. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/build-system/agp/agp-9-upgrade/SKILL.md)

### Aparat

- [`camerax`](third_party/android-skills/camera/camerax/SKILL.md) — CameraX, nagrywanie, efekty. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/camera/camerax/SKILL.md)

### Android w samochodzie

- [`android-for-cars-parked-apps`](third_party/android-skills/cars/android-for-cars-parked-apps/SKILL.md) — aplikacje parked na Android Auto i Automotive. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/cars/android-for-cars-parked-apps/SKILL.md)

### AI na urządzeniu

- [`appfunctions`](third_party/android-skills/device-ai/appfunctions/SKILL.md) — funkcje aplikacji dla agentów systemowych. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/device-ai/appfunctions/SKILL.md)
- [`litert-lm`](third_party/android-skills/device-ai/litert/litert-lm/SKILL.md) — lokalny LLM (`.litertlm`) na CPU/GPU. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/device-ai/litert/litert-lm/SKILL.md)
- [`litert-runtime`](third_party/android-skills/device-ai/litert/litert-runtime/SKILL.md) — inferencja `.tflite` (CompiledModel). [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/device-ai/litert/litert-runtime/SKILL.md)
- [`ml-kit-genai-prompt-api`](third_party/android-skills/device-ai/ml-kit-genai-prompt-api/SKILL.md) — ML Kit GenAI Prompt API (Gemini Nano). [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/device-ai/ml-kit-genai-prompt-api/SKILL.md)
- [`mlkit-agent-api`](third_party/android-skills/device-ai/mlkit-agent-api/SKILL.md) — agenci on-device na Gemini Nano. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/device-ai/mlkit-agent-api/SKILL.md)

### Narzędzia

- [`android-cli`](third_party/android-skills/devtools/android-cli/SKILL.md) — oficjalne CLI `android` (projekty, urządzenia, zrzuty, skille). [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/devtools/android-cli/SKILL.md)

### Tożsamość

- [`restore-credentials`](third_party/android-skills/identity/restore-credentials/SKILL.md) — ciche logowanie po przywróceniu urządzenia. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/identity/restore-credentials/SKILL.md)
- [`verified-email`](third_party/android-skills/identity/verified-email/SKILL.md) — zweryfikowany e-mail bez OTP (Credential Manager). [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/identity/verified-email/SKILL.md)

### Jetpack Compose

- [`adaptive`](third_party/android-skills/jetpack-compose/adaptive/SKILL.md) — okna, foldable, układy wielopanelowe. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/jetpack-compose/adaptive/SKILL.md)
- [`ink-app-builder`](third_party/android-skills/jetpack-compose/ink/ink-app-builder/SKILL.md) — rysowanie odręczne (Jetpack Ink). [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/jetpack-compose/ink/ink-app-builder/SKILL.md)
- [`ink-custom-brush-builder`](third_party/android-skills/jetpack-compose/ink/ink-custom-brush-builder/SKILL.md) — własne pędzle Ink. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/jetpack-compose/ink/ink-custom-brush-builder/SKILL.md)
- [`jetpack-compose-performance`](third_party/android-skills/jetpack-compose/jetpack-compose-performance/SKILL.md) — recomposition i zgubione klatki. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/jetpack-compose/jetpack-compose-performance/SKILL.md)
- [`migrate-xml-views-to-jetpack-compose`](third_party/android-skills/jetpack-compose/migration/migrate-xml-views-to-jetpack-compose/SKILL.md) — migracja XML Views do Compose. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/jetpack-compose/migration/migrate-xml-views-to-jetpack-compose/SKILL.md)

### Media

- [`media3-cast-integration`](third_party/android-skills/media/media3-cast-integration/SKILL.md) — Google Cast przez Media3. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/media/media3-cast-integration/SKILL.md)

### Nawigacja

- [`navigation-3`](third_party/android-skills/navigation/navigation-3/SKILL.md) — Navigation 3, sceny, głębokie linki, Hilt. Domyślnie ten zestaw zostaje przy Navigation Compose. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/navigation/navigation-3/SKILL.md)
- [`navigation-event`](third_party/android-skills/navigation/navigation-event/SKILL.md) — predictive back (`NavigationEvent`). [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/navigation/navigation-event/SKILL.md)

### Wydajność

- [`anr-audit-and-fix`](third_party/android-skills/performance/anr-audit-and-fix/SKILL.md) — audyt i diagnoza ANR. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/performance/anr-audit-and-fix/SKILL.md)
- [`r8-analyzer`](third_party/android-skills/performance/r8-analyzer/SKILL.md) — reguły R8 i ProGuard. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/performance/r8-analyzer/SKILL.md)

### Google Play

- [`engage-sdk-integration`](third_party/android-skills/play/engage-sdk-integration/SKILL.md) — Play Engage SDK. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/play/engage-sdk-integration/SKILL.md)
- [`play-billing-library-version-upgrade`](third_party/android-skills/play/play-billing-library-version-upgrade/SKILL.md) — aktualizacja Play Billing. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/play/play-billing-library-version-upgrade/SKILL.md)
- [`play-policy-insights`](third_party/android-skills/play/play-policy-insights/SKILL.md) — zgodność z polityką Play. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/play/play-policy-insights/SKILL.md)

### Profilery

- [`android-profiler`](third_party/android-skills/profilers/android-profiler/SKILL.md) — ślady Perfetto, jank, pamięć, bateria. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/profilers/android-profiler/SKILL.md)

### Bezpieczeństwo

- [`android-intent-security`](third_party/android-skills/security/android-intent-security/SKILL.md) — intenty i komponenty. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/security/android-intent-security/SKILL.md)
- [`android-permissions-security`](third_party/android-skills/security/android-permissions-security/SKILL.md) — uprawnienia i IPC. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/security/android-permissions-security/SKILL.md)

### System

- [`edge-to-edge`](third_party/android-skills/system/edge-to-edge/SKILL.md) — insety, paski systemowe, klawiatura. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/system/edge-to-edge/SKILL.md)
- [`glance-appwidget-updates-and-memory`](third_party/android-skills/system/glance-appwidget-updates-and-memory/SKILL.md) — odświeżanie widgetów Glance i limit bitmap. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/system/glance-appwidget-updates-and-memory/SKILL.md)
- [`glance-canonical-layouts`](third_party/android-skills/system/glance-canonical-layouts/SKILL.md) — kanoniczne układy widgetów. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/system/glance-canonical-layouts/SKILL.md)

### Testy

- [`testing-setup`](third_party/android-skills/testing/testing-setup/SKILL.md) — strategia testów jednostkowych, UI i screenshotów. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/testing/testing-setup/SKILL.md)

### Android TV

- [`leanback-to-compose-tv-migration`](third_party/android-skills/tv/leanback-to-compose-tv-migration/SKILL.md) — Leanback → Compose for TV. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/tv/leanback-to-compose-tv-migration/SKILL.md)

### Wear OS

- [`wear-compose-m3`](third_party/android-skills/wear/wear-compose-m3/SKILL.md) — Wear OS, Compose Material 3. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/wear/wear-compose-m3/SKILL.md)
- [`wear-widgets`](third_party/android-skills/wear/wear-widgets/SKILL.md) — widżety Wear (Glance Wear, Remote Compose). [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/wear/wear-widgets/SKILL.md)

### Android XR

- [`display-glasses-with-jetpack-compose-glimmer`](third_party/android-skills/xr/display-glasses-with-jetpack-compose-glimmer/SKILL.md) — okulary z wyświetlaczem, Jetpack Compose Glimmer. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/xr/display-glasses-with-jetpack-compose-glimmer/SKILL.md)
- [`projected-hardware`](third_party/android-skills/xr/projected-hardware/SKILL.md) — kamera i mikrofon na okularach. [Źródło](https://github.com/android/skills/blob/6daa53ef27fa2aa87827d183a85c06cedceca833/xr/projected-hardware/SKILL.md)

## Dodatki MCP

Konfiguracja jest w repozytorium i nie zawiera sekretów. Figma i Mobbin logują się OAuth w przeglądarce. Mobile MCP to lokalny proces `npx`.

| Serwer | Do czego | Konfiguracja |
| --- | --- | --- |
| [mobile-mcp](https://github.com/mobile-next/mobile-mcp) | Emulator i urządzenie: zrzut, tap, instalacja APK | [`npx -y @mobilenext/mobile-mcp@latest`](https://github.com/mobile-next/mobile-mcp/blob/main/llms-install.md) w [`.cursor/mcp.json`](.cursor/mcp.json) |
| [Figma](https://developers.figma.com/docs/figma-mcp-server/) | Kontekst klatki i zmienne do kodu | `https://mcp.figma.com/mcp` — [instalacja](https://developers.figma.com/docs/figma-mcp-server/remote-server-installation/), [plugin Cursora](https://cursor.com/marketplace/mcp/figma) |
| [Mobbin](https://mobbin.com/mcp) | Ekrany i flow z wypuszczonych aplikacji | `https://api.mobbin.com/mcp` — [Cursor](https://docs.mobbin.com/mcp/clients/cursor), [plugin](https://cursor.com/marketplace/mcp/mobbin) |

Claude Code dostaje te same trzy serwery z [`.mcp.json`](.mcp.json) (przy pierwszym uruchomieniu pyta o zgodę). Codex: [`mcp/codex.config.toml`](mcp/codex.config.toml). Pełne kroki, wymagania Node.js 20+ i `adb`, serwer desktopowy Figmy oraz limity Mobbin: [mcp/README.md](mcp/README.md).

## Prompty

Wklej jeden plik, uzupełnij `<NAWIASY>`.

- [Nowa funkcja, najpierw plan](prompts/01-nowa-funkcja.md)
- [Ekran z Figmy lub Mobbin](prompts/02-ekran-z-figmy-lub-mobbin.md)
- [Design system](prompts/03-design-system.md)
- [Pętla weryfikacji na emulatorze](prompts/04-weryfikacja-emulator.md)
- [Naprawa błędu](prompts/05-naprawa-bledu.md)
- [Refaktoryzacja](prompts/06-refaktoryzacja.md)

## Windows i symlinki

Git zapisuje dowiązania. Przed klonem na Windowsie włącz tryb dewelopera i:

```bash
git config --global core.symlinks true
```

Gdy checkout zamienił linki na zwykłe pliki tekstowe, odtwórz kopie:

```bash
scripts/link-agent-skills.sh --copy
```

Na macOS i Linuksie samo `scripts/link-agent-skills.sh` stawia symlinki z powrotem.

## Licencja

Tekst tego zestawu (reguły, własne skille, prompty, konfiguracja MCP) możesz kopiować do swoich projektów Androida.

Skille w `third_party/android-skills/` należą do Google LLC i są na [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). Zostaw `LICENSE.txt` i nagłówki w `SKILL.md`, gdy je rozpowszechniasz. Nie dopisuj do nich polskiej notki — wtedy przestałyby być czystym snapshotem.
