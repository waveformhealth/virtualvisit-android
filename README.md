# waveform-health-android

## About

This is the android component of Virtual Visit, a product for making video calls simple for patients in hospitals.
This project was created during the [Twilio x DEV community hackathon](https://dev.to/devteam/announcing-the-twilio-hackathon-on-dev-2lh8).

### How it works

This android application communicates with the [Virtual Visit service](https://virtualvisit-twilio-serverless-1113-dev.twil.io/room) to
allow a patient to easily connect with a friend or family member for a Virtual Visit (video call).


### Permissions

- Camera access
- Microphone access

These are required to join a Virtual Visit, but the camera and microphone can be turned off once connected to a Visit.


## Features

- Easy video call with [Twilio Programmable Video](https://www.twilio.com/docs/video)
- Minimal design for ease of use
- No sign up or login needed for patient

### Libraries

- Twilio Programmable Video
- Coroutines for multithreading
- Retrofit for interfacing with the server
- Dagger2 for dependency injection
- Dexter for easy permission requesting

## Set up

### Requirements

- A Twilio account - [sign up](https://www.twilio.com/try-twilio)
- Twilio api key
- Running instance of [virtualvisit-twilio-serverless](https://github.com/waveformhealth/virtualvisit-twilio-serverless)

### Environment Variables

Create a `local.properties` file at the root of the project and set `app.api.url` to the base URL of a running instance of [virtualvisit-twilio-serverless](https://github.com/waveformhealth/virtualvisit-twilio-serverless):

```plaintext
app.api.url="https://mysite-123-dev.twil.io"
```

### Building and Installing

Use Android Studio to build and install the application or run gradle from the command line:

    $ ./gradlew build
    $ ./gradlew installDebug
