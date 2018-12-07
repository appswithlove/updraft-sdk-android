![Updraft: Mobile App Distribution](updraft.png)
[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](https://raw.githubusercontent.com/appswithlove/updraft-sdk-ios/master/LICENSE)
[![Twitter](https://img.shields.io/badge/twitter-@GetUpdraft-blue.svg?style=flat)](https://twitter.com/GetUpdraft)


# Updraft SDK

Updraft SDK for Android

Updraft is a super easy app delivery tool that allows you to simply and quickly distribute your app. It's super useful during beta-testing or if you want to deliver an app without going through the app store review processes. Your users get a link and are guided through the installation process with in a comprehensive web-app. Updraft works with Android and iOS apps and easily integrates with your IDE.
The SDK adds additional features to apps that are delivered with Updraft: Auto-update for your distributed apps and most importantly the collection of user feedback.

Updraft is built by App Agencies [Apps with love](https://appswithlove.com/) and [Moqod](https://moqod.com/). Learn more at [getupdraft.com](https://getupdraft.com/) or follow the latest news on [twitter](https://twitter.com/GetUpdraft).


## Requirements

- minSdkVersion >=19

## Installation

Add the repository into your top-level build.gradle file:

```
allproject {
    repositories {
        maven {
            url 'https://artifactory.appswithlove.net/artifactory'
            credentials {
                username = "${artifactory_user}"
                password = "${artifactory_password}"
            }
        }
    }
}
```

Add this line to your dependencies in app-level build.gradle file:

```
dependencies {
    compile 'com.appswithlove.updraftsdk:updraft_sdk:1.0.3'
}
```

## Setup

```
    @Override
    public void onCreate() {
        super.onCreate();
        Settings settings = new Settings();
        settings.setAppKey(YOUR_APP_KEY);
        settings.setSdkKey(YOUR_SDK_KEY);
        //Should app be in store
        settings.setStoreRelease(false);
        //Optional base url for updraft
        settings.setBaseUrl(Settings.BASE_URL_STAGING);
        //Optional set log level
        settings.setLogLevel(Settings.LOG_LEVEL_DEBUG);
        //Optional set if should show start alert
        settings.setShowStartAlert(false);
        Updraft.initialize(this, settings);
        Updraft.getInstance().start();
    }
}
```
### Parameters
- <b>YOUR_SDK_KEY</b>: Your sdk key obtained on [Updraft](https://getupdraft.com)
- <b>YOUR_APP_KEY</b>: You app key obtained on [Updraft](https://getupdraft.com)

## AutoUpdate
Auto Update functionality can be enabled/disabled on [getupdraft.com](https://getupdraft.com/) in your app edit menu.

AutoUpdate work by comparing the build number of the app installed on the user's device and the app uploaded on GetUpdraft.

A prompt is displayed to the user if his installed version is lower than the version on Updraft.
Thus, the build number must be incremented for each new build release to trigger the auto-update process.

Micro version comparison is supported, for example version 1.2.3.2018080**4** is greater than version 1.2.3.2018080**3**

## Feedback

Feedback functionality can be enabled/disabled on [getupdraft.com](https://getupdraft.com/) in your app edit menu.

A prompt is shown to the user to inform him of the change of state of the feedback functionality.

If enabled, the user is explained how he can give feedback.
User can take a screenshot to give a feedback.

## Advanced setup:  Logging

To check if data is send properly to Updraft and also see some additional SDK log data in the console, you can set different log levels.

To change the log level, add the following line before starting the SDK:

```
    settings.setLogLevel(Settings.LOG_LEVEL_DEBUG);
```


Default level: <b>LOG_LEVEL_ERROR</b> => Only warnings and errors will be printed.
