# Api

Aplikacja klient-serwer napisana w Javie z użyciem surowych gniazd TCP (bez frameworków). Architektura mikroserwisowa z bramką API routującą żądania do osobnych usług. Klient CLI i warstwa persystencji danych.

## Technologie

![Java](https://img.shields.io/badge/Java-100%25-ED8B00?style=flat&logo=openjdk&logoColor=white)
![TCP Sockets](https://img.shields.io/badge/Transport-TCP%20Sockets-lightgrey?style=flat)

## Funkcjonalności

- Komunikacja klient-serwer po protokole TCP (surowe gniazda)
- API Gateway routujący żądania do właściwej usługi
- Osobne mikroserwisy na dedykowanych portach: rejestracja, logowanie, posty (add/get)
- `UnifiedServer` obsługujący połączenia
- Klient CLI (`CLI.java`) komunikujący się z systemem
- Persystencja danych (`Database.java`)
- Przesyłanie plików (`uploads/`)

## Struktura projektu

```
Api/
├── src/
│   ├── client/
│   │   └── CLI.java              # Klient wiersza poleceń
│   ├── server/
│   │   ├── APIGateway.java       # Bramka API
│   │   ├── UnifiedServer.java    # Główny serwer
│   │   ├── RegistrationServer.java
│   │   ├── Login.java
│   │   ├── AddPost.java
│   │   └── GetPosts.java
│   └── database/
│       └── Database.java         # Warstwa persystencji
├── uploads/                      # Przesyłane pliki
└── serwer/                       # Pliki uruchomieniowe
```

## Uruchomienie

### Wymagania
- Java 17+

### Kroki

```bash
git clone https://github.com/Marvi217/Api.git
cd Api
```

Skompiluj projekt (z IntelliJ IDEA lub ręcznie):
```bash
javac -d out src/database/*.java src/server/*.java src/client/*.java
```

Uruchom serwer:
```bash
java -cp out server.UnifiedServer
```

Uruchom klienta (w osobnym terminalu):
```bash
java -cp out client.CLI
```
