# Dodatki MCP

Trzy serwery, zero sekretów w repozytorium. Logowanie do Figmy i Mobbin idzie przez OAuth w przeglądarce. Mobile MCP nie wymaga klucza.

Gotowe pliki, które ten zestaw już podpina:

- [`.cursor/mcp.json`](../.cursor/mcp.json) — Cursor (kopia: [`cursor.mcp.json`](cursor.mcp.json))
- [`.mcp.json`](../.mcp.json) — Claude Code, zakres projektu (kopia: [`claude.mcp.json`](claude.mcp.json))
- [`codex.config.toml`](codex.config.toml) — fragment do `~/.codex/config.toml` ([składnia MCP w Codex](https://developers.openai.com/codex/mcp)); nie jest włączany automatycznie

Po skopiowaniu zestawu do istniejącego projektu scal obiekty `mcpServers` ze swoim plikiem. Nie nadpisuj innych serwerów.

## Mobile MCP

Steruje emulatorem Androida i podłączonym urządzeniem: zrzut, elementy UI, tap, instalacja APK, logcat.

- Repo: [mobile-next/mobile-mcp](https://github.com/mobile-next/mobile-mcp)
- Instrukcja dla agentów: [llms-install.md](https://github.com/mobile-next/mobile-mcp/blob/main/llms-install.md)
- Cursor: [Getting Started with Cursor](https://github.com/mobile-next/mobile-mcp/wiki/Getting-Started-with-Cursor)
- Emulator: [Getting Started with Android Emulator](https://github.com/mobile-next/mobile-mcp/wiki/Getting-Started-with-Android-Emulator)

Wymaga Node.js 20+, [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) (`adb` na `PATH`) i włączonego emulatora albo urządzenia z debugowaniem USB. Sprawdzenie: `adb devices`, potem w agencie „wylistuj dostępne urządzenia” (`mobile_list_available_devices`).

Telemetria anonimowa jest w tym zestawie wyłączona (`MOBILEMCP_DISABLE_TELEMETRY=1`). Usuń tę zmienną, jeśli chcesz ją zostawić włączoną.

Szybka instalacja wykrywająca klienty (nadpisuje ręczną konfigurację tylko tam, gdzie ją uruchomisz):

```bash
npx @mobilenext/mobile-mcp@latest install
```

Codex, gdy nie chcesz edytować TOML:

```bash
codex mcp add mobile-mcp -- npx -y @mobilenext/mobile-mcp@latest
```

## Figma MCP

Zdalny serwer jest zalecany: `https://mcp.figma.com/mcp`. Nie potrzebuje aplikacji desktopowej. Przy pierwszym użyciu Cursor / Claude / Codex otwiera przeglądarkę i prosi o dostęp do konta Figma.

- Dokumentacja: [Figma MCP server](https://developers.figma.com/docs/figma-mcp-server/)
- Instalacja zdalna: [remote server](https://developers.figma.com/docs/figma-mcp-server/remote-server-installation/)
- Plugin Cursora (serwer, skille i reguły): [marketplace](https://cursor.com/marketplace/mcp/figma) albo w czacie `/add-plugin figma`, potem Connect
- Claude Code: `claude plugin install figma@claude-plugins-official` albo `claude mcp add --transport http figma https://mcp.figma.com/mcp`
- Codex: `codex mcp add figma --url https://mcp.figma.com/mcp`

Do implementacji ekranu wklej link do klatki (w Figmie: Copy link to selection). Sam adres pliku bez klatki daje słabszy kontekst.

Serwer desktopowy (`http://127.0.0.1:3845/mcp`) jest opcją dla planów, które nie mogą użyć serwera zdalnego. Włączasz go w Dev Mode aplikacji Figma. Instrukcja: [desktop server](https://developers.figma.com/docs/figma-mcp-server/local-server-installation/). Nie dokładaj go do wspólnego `mcp.json`, dopóki aplikacja Figma nie jest wymagana u całego zespołu — bez niej klient będzie zgłaszał rozłączony serwer.

## Mobbin MCP

Wyszukuje prawdziwe ekrany, flow i sekcje stron. Endpoint: `https://api.mobbin.com/mcp` (Streamable HTTP, OAuth). Klucza API nie ma i nie wklejaj go nigdzie.

- Opis: [mobbin.com/mcp](https://mobbin.com/mcp)
- Cursor: [docs](https://docs.mobbin.com/mcp/clients/cursor) i [plugin](https://cursor.com/marketplace/mcp/mobbin) albo `/add-plugin mobbin`, potem Authenticate
- Claude Code: [docs](https://docs.mobbin.com/mcp/clients/claude-code-cli) — `claude mcp add mobbin --scope user --transport http https://api.mobbin.com/mcp`
- Codex: [docs](https://docs.mobbin.com/mcp/clients/codex-cli) — `codex mcp add mobbin --url https://api.mobbin.com/mcp`, potem `codex mcp login mobbin`
- Pozostałe klienty: [overview](https://docs.mobbin.com/mcp/clients/overview)

Plan Mobbin: Pro, Team albo Enterprise. Limit 60 żądań na 60 sekund. Agent ma zapisać nazwę produktu i URL referencji, której naprawdę użył, i nie kopiować marki ani assetów.
