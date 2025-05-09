# Overview

This repository contains sample Android application demonstrating the use of the GC Mobile Messenger SDK. 

## Getting Started
To get started with these samples, clone this repository to your local machine:

```bash
git clone git@github.com:MyPureCloud/mobiledx-samples-android.git
```

## Prerequisites
- Android Studio Flamingo 2022.2.1 Patch 2 (+)
- Gradle version 7.4.1 (+)
- Gradle plugin 7.3.1 (+)
- Kotlin plugin version 1.7.10 (+)
- Java 11
- An Android device or emulator running Android API level 21 (Lollipop) or higher

## Installation
- Open Android Studio.
- Select "Open an existing Android Studio project".
- Navigate to the cloned repository and open "GCMessengerSDKSample".
- Wait for Gradle to sync and build the project.
- Run the application on your device or emulator.

## Documentation
[Mobile Messenger SDK](https://developer.genesys.cloud/commdigital/digital/webmessaging/mobile-messaging/messenger-mobile-sdk/)

[Transport Mobile SDK ](https://developer.genesys.cloud/commdigital/digital/webmessaging/mobile-messaging/messenger-transport-mobile-sdk/)

[Genesys Cloud Developer Forum](https://developer.genesys.cloud/forum/c/web-messaging/39)

# License
This project is licensed under the MIT License - see the LICENSE file for details.

# Setup application

## Authenticated messaging
The app integrates Okta authentication. To use this authentication service, you must configure okta.properties file with the necessary data to the path GCMessengerSDKSample/app. See an example in GCMessengerSDKSample/app/okta.properties.example
