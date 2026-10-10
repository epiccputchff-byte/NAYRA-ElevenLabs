# NAYRA AI - Hindi setup guide

## Is project mein kya hai?
- Kotlin + Jetpack Compose Android app
- Existing anime-boy portrait aur holographic-style UI effects
- Gemini API ke zariye online AI chat
- Android SpeechRecognizer aur Text-to-Speech integration
- Hindi/English language selection aur voice speed/pitch settings
- Basic in-session chat history

## Zaroori limitations
- Avatar abhi static image + Compose effects hai, full 3D model nahi.
- Male-sounding voice device ke installed TTS engine/voice par depend karti hai. Har phone par same voice guaranteed nahi.
- Offline AI model is package mein included nahi hai.
- Gemini ke liye valid API key aur internet chahiye.
- Public release ke liye API key ko app ke andar rakhna safe nahi; production mein backend proxy use karo.
- Is ZIP ko yahan Android SDK/Gradle environment available na hone ki wajah se compile ya device par run-test nahi kiya gaya. GitHub Actions workflow add kiya gaya hai, jo repository mein push hone par build aur unit tests chalane ki koshish karega.

## API key set karna
App ke Settings mein API key field maujood hai. Gemini API key ko private rakho aur GitHub par commit mat karo. Settings mein key dene se is app instance ki runtime repository use karegi; app data clear ya app restart hone par key persist hona guaranteed nahi hai.

## Acode par kaise use karein?
1. ZIP extract karo.
2. Acode mein extracted project folder open karke Kotlin/XML/Gradle files edit kar sakte ho.
3. Acode khud native Android Gradle project ko APK mein compile nahi karta. APK build ke liye Android SDK + compatible Gradle/AGP chahiye.
4. GitHub repository mein project push karne ke baad Actions tab mein `Android build and tests` workflow ka result dekho. Successful run hone par `nayra-ai-debug-apk` artifact download kar sakte ho.

## Local build
Android SDK aur Gradle 9.3.1 install hone ke baad project root se:

```bash
gradle testDebugUnitTest --no-daemon
gradle assembleDebug --no-daemon
```

APK path: `app/build/outputs/apk/debug/`.
