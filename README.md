# YaPhotos

Sample Android app to demonstrate usage of Yandex.Fotki API.

Displays three public photo albums: "Recent photos", "Popular photos" and "Photos of the day".

This is a version of [YaPhotosNoLib](https://github.com/gmk57/ya-photos-no-lib) with third-party libraries and more features.

![Album view](app/src/main/screen_album.png)  ![Photo view](app/src/main/screen_photo.png)  ![Fullscreen view](app/src/main/screen_full.png)

## Features

* Album and detail view
* Thumbnails caching and preloading (with lower priority)
* Endless scrolling
* Fullscreen mode
* Share button
* Progress and error indicators
* Image quality, thumbnail size and column number auto-adjusted to screen size
* Workaround to calculate next page for "Photos of the day" album

## Technologies used

* [Picasso](https://github.com/square/picasso)
* AsyncTask
* Date, Calendar, SimpleDateFormat
* FileProvider
* Fragment, FragmentManager
* HttpURLConnection
* JSONObject, JSONArray
* RecyclerView, GridLayoutManager
* SharedPreferences
* StrictMode

## Installation

This is an Android Studio project.

## License

Project is distributed under MIT license.

The use of Yandex.Fotki service and its API is regulated by [API User Agreement](https://yandex.ru/legal/fotki_api/), [Yandex.Fotki Service Terms Of Use](https://yandex.ru/legal/fotki_termsofuse/) and general [User Agreement for Yandex Services](https://yandex.com/legal/rules/).