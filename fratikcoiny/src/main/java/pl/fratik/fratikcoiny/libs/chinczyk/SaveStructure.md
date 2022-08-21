# Struktura zapisu Chińczyka

## Nagłówek:
| Nazwa                   | Typ           | Wielkość            | Opis                                         |
|-------------------------|---------------|---------------------|----------------------------------------------|
| Magic bytes             | unsigned byte | 2 bajty             | zawsze 0x21, 0x37                            |
| Wersja                  | unsigned byte | 1 bajt              | wersja — obecnie 6                           |
| ID wykonawcy komendy    | long          | 8 bajtów            | ID wykonawcy komendy                         |
| ID kanału               | long          | 8 bajtów            | ID kanału z grą                              |
| Seed                    | long          | 8 bajtów            | Seed do Random                               |
| Sekwencja               | long          | 8 bajtów            | Sekwencja — ilość rzutów do wysymulowania    |
| Język                   | string        | patrz opis string'a | aktualny język gry — klucz enuma             |
| Ilość graczy            | unsigned byte | 1 bajt              | ilość graczy - 1-4 (*n*)                     |
| Gracze (*n* obiektów)   | Gracz         | patrz opis Gracza   | gracze w grze                                |
| Zasady                  | long          | 8 bajtów            | Raw flagi włączonych zasad                   |
| Oszustwa                | boolean       | 1 bajt              | Flaga oszustw                                |
| Skin                    | Skin          | patrz opis          | Wybrany skin                                 |
| Thread ID               | long          | 8 bajtów            | ID wątku                                     |
| Czas gry                | unsigned int  | 4 bajty             | Zarejestrowany czas gry w momencie zapisu    |
| Start                   | long          | 8 bajtów            | UNIX timestamp rozpoczęcia gry               |
| Timestamp               | long          | 8 bajtów            | UNIX timestamp momentu zapisu                |

## Zawartość:
| Nazwa                                  | Typ           | Wielkość | Opis                                                                                                             |
|----------------------------------------|---------------|----------|------------------------------------------------------------------------------------------------------------------|
| Typ wydarzenia                         | unsigned byte | 1 bajt   | Raw typ wydarzenia                                                                                               |
| Miejsce gracza wykonującego wydarzenie | byte          | 1 bajt   | Miejsce gracza "odpowiedzialnego" za to wydarzenie, wg. offsetu (0 oznacza, że event nie ma przypisanego gracza) |
| Wyrzucona liczba oczek                 | byte          | 1 bajt   | Wyrzucona liczba oczek (1-6, lub 0 jeżeli null)                                                                  |
| Pionek                                 | byte          | 1 bajt   | Bajt oznaczający indeks pionka "odpowiedzialnego"                                                                |
| Pionek 2 - gracz                       | byte          | 1 bajt   | Miejsce gracza pionka 2 (2, 12, 22, 32, lub 0 jeżeli null)                                                       |
| Pionek 2                               | byte?         | 1 bajt   | Indeks pionka 2 (0-3) - **to pole jest pominięte jeżeli bajt wcześniej jest 0!**                                 |
| FastRoll                               | boolean       | 1 bajt   | Czy ruch został wykonany automatycznie w ramach szybkich rzutów                                                  |
| Timestamp                              | long?         | 8 bajtów | UNIX timestamp eventu - **tylko jeżeli jest to koniec gry (typ 6)!**                                             |

## Skin:
| Nazwa  | Typ                         | Wielkość | Opis                                       |
|--------|-----------------------------|----------|--------------------------------------------|
| custom | boolean                     | 1 bajt   | Czy jest to skin domyślny, czy skin własny |
| dane   | Skin domyślny / Skin własny | ?        | patrz niżej                                |

## Skin domyślny:
| Nazwa | Typ  | Wielkość | Opis                             |
|-------|------|----------|----------------------------------|
| Flaga | long | 8 bajtów | Wybrany skin domyślny, po fladze |

## Skin własny:
| Nazwa                 | Typ    | Wielkość     | Opis                                                                                                                                      |
|-----------------------|--------|--------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Klasa deserializująca | string | patrz string | Klasa, w której jest funkcja `public static Skin deserialize(InputStream is)`, użyta do deserializacji własnego skinu                     |
| Długość danych        | int    | 4 bajty      | Długość danych skina własnego (*n*) - **maksymalna długość skina to 2147483647 bajtów**                                                   |
| Dane skina własnego   | ?      | *n* bajtów   | Faktyczne dane skina - deserializacja jest zależna od implementacji. Zewnętrzne programy mogą pominąć *n* bajtów i ustawić skin domyślny. |

## String:
| Nazwa   | Typ            | Wielkość   | Opis                                                                 |
|---------|----------------|------------|----------------------------------------------------------------------|
| Długość | unsigned short | 2 bajty    | Długość tekstu (*n*) - **maksymalna długość tekstu to 65535 znaków** |
| Tekst   | bajty w UTF-8  | *n* bajtów | Tekst                                                                |

## Gracz:
| Nazwa          | Typ    | Wielkość            | Opis                                                                          |
|----------------|--------|---------------------|-------------------------------------------------------------------------------|
| Miejsce        | byte   | 1 bajt              | Miejsce na planszy, wg. offsetu (2 - BLUE, 12 - GREEN, 22 - YELLOW, 32 - RED) |
| ID użytkownika | long   | 8 bajtów            | ID użytkownika                                                                |
| Język          | string | patrz opis string'a | Język gracza                                                                  |
