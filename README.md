# Tulbot
## Instrukcja uruchomienia:
1. Zainstaluj Ollama
2. Pobierz modele używane w programie przy pomocy Ollamy:
   - SpeakLeash/bielik-11b-v2.3-instruct:Q4_K_M
   ```
   ollama pull SpeakLeash/bielik-11b-v2.3-instruct:Q4_K_M
   ```
   - mxbai-embed-large:335m
   ```
   ollama pull mxbai-embed-large:335m
   ```
3. W katalogu głównym projektu utwórz plik .env i umieść w nim następującą konfigurację:
```
   MONGO_INITDB_ROOT_USERNAME=root
   MONGO_INITDB_ROOT_PASSWORD=rootpassword
   ME_CONFIG_MONGODB_ADMINUSERNAME=root
   ME_CONFIG_MONGODB_ADMINPASSWORD=rootpassword
   MONGODB_URI=mongodb://root:rootpassword@localhost:27017/chatbotdb?authSource=admin
  ```
4. Uruchom MongoDB i Qdrant z pliku docker-compose:
```
    docker-compose up
```
5. Przejdź do katalogu frontend:
```
  cd frontend
```
6. Zainstaluj zależności, a następnie uruchom aplikacje
```
  npm install
  npm run dev
```
7. W nowym terminalu przejdź do katalogu backend, następnie uruchom aplikację
``` 
   cd backend
   .\mvnw spring-boot:run
```
8. Aplikacja będzie dostępna pod adresem podanym w konsoli np. (http://localhost:5173)