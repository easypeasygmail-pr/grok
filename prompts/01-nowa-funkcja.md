# Nowa funkcja — najpierw plan

Użyj, gdy dodajesz ekran albo przepływ, którego jeszcze nie ma. Wklej blok w czat agenta. Uzupełnij pola w nawiasach ostrokątnych i usuń te, które nie pasują.

```text
Jesteś agentem w projekcie natywnego Androida (Kotlin). Zanim napiszesz kod ekranu, przeczytaj AGENTS.md i trzymaj się tego stosu: Jetpack Compose, Material 3, jedna aktywność, Navigation Compose z trasami @Serializable, MVVM/UDF (ViewModel + StateFlow), Hilt, Coroutines/Flow. Room tam, gdzie są dane lokalne. Jeden klient HTTP — ten, który już jest w projekcie (Retrofit albo Ktor), bez dokładania drugiego.

Funkcja: <NAZWA>
Cel użytkownika: <CO UŻYTKOWNIK MA ZROBIĆ>
Dane: <ENDPOINT, TABELA ROOM, ARGUMENTY NAWIGACJI — ALBO „BRAK, STAN LOKALNY”>
Poza zakresem: <CZEGO NIE RUSZAMY>

Kolejność:
1. Plan, zanim powstanie kod ekranu. Wypisz: trasę, UiState (ładowanie, pusty, błąd, treść), zdarzenia UI, repozytorium, wiązania Hilt, testy. Jeśli w motywie brakuje koloru, typografii albo skali odstępów — najpierw skill design-system-tokens, nie ekran.
2. Potem pionowy wycinek: repozytorium → ViewModel → Composable → nawigacja → test jednostkowy ViewModelu (sukces i błąd).
3. Teksty użytkownika w zasobach stringów. Kolory i odstępy tylko z motywu. Logika poza Composable. Zbieranie stanu przez collectAsStateWithLifecycle.
4. Nie dodawaj biblioteki, jeśli projekt ma już odpowiednik. Nie podnoś targetSdk przy okazji.
5. Na końcu skill verify-on-emulator: build debug, instalacja, zrzut właściwego ekranu. Napisz, co zrzut potwierdza i czego nie udało się sprawdzić.

Skończone jest wtedy, gdy:
- stan ekranu żyje w ViewModelu, a Composable tylko rysuje i wysyła zdarzenia,
- widać stany loading, empty i error z akcją ponowienia,
- treść nie chowa się pod paskiem statusu ani nawigacją,
- test jednostkowy na główną ścieżkę i na błąd został uruchomiony,
- jest zrzut albo jasno napisane, że nie było urządzenia.
```
