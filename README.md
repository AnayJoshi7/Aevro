# Aevro

### Train. Fuel. Progress.

Aevro is a personal fitness companion built to bring workouts, nutrition,
progress, and fitness goals together in one simple Android app.

The idea is simple: make it easier to stay consistent without having to
keep track of your workouts, meals, calories, and progress in different
places.

---

## What is Aevro?

Aevro helps you keep your fitness journey organized in one place.

You can create your fitness profile, keep track of your workouts, monitor
your progress, log meals, and keep an eye on your daily calorie and protein
intake.

The app is designed around a clean, dark interface that keeps the important
information easy to see without making the experience feel cluttered.

---

## Features

###  Workouts

- View your current workout
- Browse other available workouts
- View target muscle groups
- View the number of exercises
- Personalized workout information

###  Nutrition

- Track daily calories
- Track protein intake
- Log meals
- Keep nutrition information alongside your training

###  Progress

- View fitness progress
- Track weekly activity
- Monitor goal completion
- Keep an overview of your fitness journey

###  Personal Profile

- Personal fitness profile
- Age, height, weight and fitness goal
- User-specific data stored through Firebase

---

## Built With

| Technology | Purpose |
|---|---|
| Kotlin | Application development |
| Jetpack Compose | UI development |
| Material 3 | UI components |
| Firebase Authentication | User authentication |
| Firebase Firestore | Cloud data storage |
| Retrofit | REST API communication |
| Kotlin Coroutines | Asynchronous operations |
| ViewModel | UI state management |
| StateFlow | Reactive state handling |
| Gradle | Build system |
| Git & GitHub | Version control |

---

## How Aevro is structured

The app follows a simple layered approach so that the UI does not have to
deal directly with backend or API operations.

```text
                    Aevro
                      │
                      ▼
              Jetpack Compose UI
                      │
                      ▼
                  ViewModel
                      │
                      ▼
                 Repository
                  /       \
                 /         \
                ▼           ▼
           Firebase       REST API
```

The UI displays the current state, ViewModels manage that state, and
repositories handle communication with Firebase or external APIs.

---

## Firebase

Firebase is used for authentication and cloud data.

The application uses Firebase Authentication for user accounts and
Cloud Firestore for storing user-specific fitness information.

This allows information to be retrieved dynamically rather than being
hard-coded into the application.

---

## REST API

Aevro also communicates with a REST API using Retrofit.

The API is currently used to retrieve motivational quotes displayed on
the dashboard.

Network operations run asynchronously using Kotlin Coroutines, with
loading and error states handled by the application.

---

## UI & Design

Aevro follows a minimal dark visual style.

The interface uses:

- Dark backgrounds
- Simple cards
- Green accent elements
- High-contrast text
- Rounded components
- Minimal navigation
- Consistent spacing and typography

The goal is to keep the interface focused on the information that
actually matters while using the app.

---

## Testing

Aevro was tested on a physical Android device throughout development.

Testing included:

- Screen navigation
- User interactions
- Firebase authentication
- Firestore data retrieval
- REST API communication
- Loading states
- Error states
- Retry functionality
- Workout data
- Profile data
- Responsive layouts
- Different data and failure scenarios

---

## Getting Started

### Requirements

- Android Studio
- Android SDK
- JDK
- Android device or emulator
- Firebase project configured for the application

### Clone the repository

```bash
git clone https://github.com/AnayJoshi7/Aevro.git
cd Aevro
```

Open the project in Android Studio and allow Gradle to synchronize.

Connect an Android device or start an emulator, then run the `app`
configuration.

---

## Download

Want to try Aevro without building it yourself?

The latest APK is available through GitHub Releases.

**[Download Aevro](../../releases/latest)**

---

## Project Structure

```text
Aevro/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/anay/fitnesstracker/
│           │       ├── data/
│           │       │   ├── model/
│           │       │   ├── remote/
│           │       │   ├── repository/
│           │       │   └── viewmodel/
│           │       │
│           │       └── ui/
│           │           └── screens/
│           │
│           └── res/
│
├── gradle/
├── .github/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

---

## What's Next?

Aevro is still a work in progress, and there are plenty of things that
could be added in future versions.

Some ideas include:

- More detailed workout customization
- Better progress analytics
- Expanded nutrition tracking
- Push notifications and reminders
- Health platform integrations
- Wearable device integration
- iOS version
- Self-hosted backend support

---

## Version

**1.0.0**

---

## About

Aevro started as an Android development project and gradually grew into
a complete fitness application with authentication, cloud data,
REST API integration, workout tracking, nutrition tracking, and a
Compose-based interface.

The focus throughout development was to keep the app simple, useful,
and something that could actually be used as a day-to-day fitness
companion.

---

## Developer

**Anay Joshi**

Built with Kotlin, Jetpack Compose, Firebase, and a lot of iteration.

---

### Aevro

**Train. Fuel. Progress.**
```
