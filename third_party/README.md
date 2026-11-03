# Skille Google (Android skills)

Katalog `android-skills/` to niezmieniony snapshot repozytorium [android/skills](https://github.com/android/skills) (Google LLC).

| | |
| --- | --- |
| Commit | [`6daa53ef27fa2aa87827d183a85c06cedceca833`](https://github.com/android/skills/commit/6daa53ef27fa2aa87827d183a85c06cedceca833) |
| Data commita | 2026-10-29 |
| Wersja pluginu z tego commita | v1.0.19 |
| Licencja | [Apache License 2.0](android-skills/LICENSE.txt) — [tekst na apache.org](https://www.apache.org/licenses/LICENSE-2.0) |
| Dokumentacja | [Android skills](https://developer.android.com/tools/agents/android-skills), [przegląd skilli](https://developer.android.com/tools/agents/android-skills/browse), [Android CLI](https://developer.android.com/tools/agents/android-cli) |

Plików w `android-skills/` nie edytujemy. Dopisana polska notka jest tylko przy skillach tego zestawu (`.cursor/skills/compose-screen-from-design`, `design-system-tokens`, `verify-on-emulator`). Skille Google zostają bajt w bajt, żeby dało się je podmienić nowym snapshotem i żeby zostały spełnione warunki Apache 2.0 (licencja i atrybucja w plikach źródłowych, w tym `author: Google LLC` w `SKILL.md`).

`scripts/link-agent-skills.sh` wystawia każdy skill jako `.cursor/skills/<nazwa>` i `.claude/skills/<nazwa>`.

## Aktualizacja snapshotu

```bash
git clone --depth 1 https://github.com/android/skills.git /tmp/android-skills
rm -rf third_party/android-skills
cp -a /tmp/android-skills third_party/android-skills
rm -rf third_party/android-skills/.git
# Wpisz nowy SHA (git -C /tmp/android-skills rev-parse HEAD) w tej tabeli.
scripts/link-agent-skills.sh
```

Inna droga, bez tego snapshotu: [Android CLI](https://developer.android.com/tools/agents/android-cli) instaluje skille prosto do wykrytych agentów i może zastąpić symlinki kopiami.

```bash
android skills add --all --project=.
```

Claude Code i Codex mogą też wziąć plugin z marketplace (aktualna wersja z GitHuba, nie ten pin):

```text
/plugin marketplace add android/skills
/plugin install android-skills@android-skills
```

```text
codex plugin marketplace add android/skills
codex plugin add android-skills@android-skills
```

Lista skilli i lokalne ścieżki są w [README zestawu](../README.md#oficjalne-skille-androida).
