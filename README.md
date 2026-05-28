# Api

Aplikacja klient-serwer napisana w Javie z użyciem surowych gniazd TCP (bez frameworków). Projekt obejmuje warstwę klienta, serwera oraz persystencji danych, a także obsługę przesyłania plików.

## Technologie

![Java](https://img.shields.io/badge/Java-100%25-ED8B00?style=flat&logo=openjdk&logoColor=white)
![TCP Sockets](https://img.shields.io/badge/Transport-TCP%20Sockets-lightgrey?style=flat)

## Funkcjonalności

- Komunikacja klient-serwer po protokole TCP
- Persystencja danych (warstwa `database`)
- Przesyłanie i przechowywanie plików (`uploads/`)
- Architektura wielowarstwowa: klient / serwer / baza danych

## Struktura projektu

```
Api/
├── src/
│   ├── client/       # Logika klienta TCP
│   ├── server/       # Logika serwera TCP
│   └── database/     # Warstwa persystencji danych
├── uploads/          # Przesyłane pliki
└── serwer/           # Pliki uruchomieniowe serwera
```

## Uruchomienie

### Wymagania
- Java 17+

### Kroki

```bash
git clone https://github.com/Marvi217/Api.git
cd Api
```

Skompiluj projekt:
```bash
javac -d out src/server/*.java src/database/*.java src/client/*.java
```

Uruchom serwer:
```bash
java -cp out server.Main
```

Uruchom klienta (w osobnym terminalu):
```bash
java -cp out client.Main
```
