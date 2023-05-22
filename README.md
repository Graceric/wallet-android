## Toncoin Wallet (Android)

This is the source code and build instructions for a TON Wallet implementation for Android.

**Special for [TON Wallet Contest](https://t.me/toncontests/111 "TON Wallet Contest")**
- Technical requirements met
- Layout is as close as possible to design
- TON Connect recommendations for wallet developers followed

### How to build

1. Run `git clone --recursive --depth=1 --shallow-submodules -b ton-contest-2023 https://github.com/Arseny271/wallet-android.git`
2. Install Android Studio 3.5.1, Android 10.0 SDK and Android NDK 20.0.5594570 
3. Place your release.keystore file into the **app/config** folder.
4. Open gradle.properties and fill **RELEASE_KEY_PASSWORD**, **RELEASE_KEY_ALIAS** and **RELEASE_STORE_PASSWORD** with values to access your keystore.
5. Open the project in the Android Studio (note that it should be opened, NOT imported) and build.

### Bonus features

These features are mentioned in the design layouts, but they are not in the direct technical requirements of the competition. However, I decided to implement them. The last two features described below may potentially violate the requirement of independence from third-party APIs. These dependencies are non-critical and can be easily removed if necessary.

##### 1. Jettons Support
The Jetton wallet can be added through the settings menu. To switch from Toncoin to Jetton, simply tap on the balance displayed on the main screen. In the future, we plan to introduce a pop-up hint for  instructions user. The wallet supports receiving, sending, and sending with a comment. It also supports tokens with non-standard `decimal` values. Currently, there might be potential issues with long overflow, which is non-critical for the majority of tokens.

##### 2. Notifications (alpha)
To track transactions and send notifications, the application uses tonobserver.com blockchain indexer. As of the contest deadline, the server-side component is relatively basic, lacking the ability to differentiate between incoming and outgoing wallet transactions and may behave unstably. In the future, there are plans to further develop and release the server-side component as an open-source.

##### 3. Exchange Rate
To provide the wallet's value estimation, the application uses the Coingecko API. You have the option to view the value in USD, EUR, or Bitcoin.
