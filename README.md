# YaPhotos

Sample Android app to demonstrate usage of Yandex.Fotki API.

Displays three public photo albums: "Recent photos", "Popular photos" and "Photos of the day".

This is a version of [YaPhotosNoLib](https://github.com/gmk57/ya-photos-no-lib) with third-party libraries and more features.

![Album view](app/src/main/screen_album.png)  ![Photo view](app/src/main/screen_photo.png)  ![Fullscreen view](app/src/main/screen_full.png)

## Features

- Album view:
  - Swipe and tab navigation
  - App bar scrolling off-screen
  - Thumbnails caching and preloading (with lower priority)
  - Endless scrolling
  - Thumbnail size and column number auto-adjusted to screen size
  - Progress and error indicators
- Detail view:
  - Swipe navigation
  - Fullscreen mode
  - Share button
  - Endless scrolling
  - Image size auto-adjusted to screen size
  - Progress and error indicators
- Workaround to calculate next page for "Photos of the day" album

## Technologies used

- [EventBus](https://github.com/greenrobot/EventBus)
- [Parceler](https://github.com/johncarl81/parceler)
- [Picasso](https://github.com/square/picasso)
- AtomicBoolean, AtomicReferenceArray
- CoordinatorLayout, AppBarLayout, Toolbar, TabLayout
- Date, Calendar, SimpleDateFormat
- FileProvider
- Fragment, FragmentManager
- HttpURLConnection
- JSONObject, JSONArray
- RecyclerView, GridLayoutManager
- StrictMode
- Thread
- ViewPager, FragmentStatePagerAdapter, FragmentPagerAdapter

## Installation

This is an Android Studio project.

## License

Project is distributed under MIT license.

Third-party libraries are distributed under their own terms, please see their repositories & websites.

The use of Yandex.Fotki service and its API is regulated by [API User Agreement](https://yandex.ru/legal/fotki_api/), [Yandex.Fotki Service Terms Of Use](https://yandex.ru/legal/fotki_termsofuse/) and general [User Agreement for Yandex Services](https://yandex.com/legal/rules/).