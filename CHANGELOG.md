# Changelog

## [0.17.7] - 2022-06-22

### Added

- Better error reporting

## [0.17.6] - 2022-06-20

### Added

- Deeplink hint to the login screen

### Fixed

- Enabled support for deeplinks with email and phone number

## [0.17.5] - 2022-06-14

### Changed

- Hypertrack SDK 6.1.4
- Minimum supported Android version - 6.0

### Fixed

- Getting legacy user data.
- Minor fixes

## [0.17.4] - 2022-06-14

### Fixed

- Fixed minor issues

## [0.17.3] - 2022-06-10

### Fixed

- Fix crash in case of delay for app state update

## [0.17.2] - 2022-06-06

### Fixed

- Fix error and logout on app update

## [0.17.1] - 2022-06-06

### Fixed

- Fix crash on notification click when user is not logged in

## [0.17.0] - 2022-06-04

### Fixed

- Fix Sign in screen showing on deeplink login
- Fix tracking state indicator

### Changed

- Hypertrack SDK 6.0.4

## [0.16.4] - 2022-05-24

### Changed

- Notifications now use new payload format

## [0.16.3] - 2022-05-17

### Added

- Geofence visit notifications

### Fixed

- Screen flickering when permissions are denied
- Keyboard not showing on Add Integration screen for some devices

## [0.16.2] - 2022-05-10

### Added

- Custom title for service terminated notifications
- Added "Open Dontkillmyapp.com" to Outage screen and Profile screen to open battery saver
  whitelisting instructions

### Fixed

- Fixed activity intent on push notification
- Enabled outage notification auto-cancel on click

## [0.16.1] - 2022-05-06

### Added

- Check permissions on activity resume

### Fixed

- Fixed the system overriding base font color

## [0.16.0] - 2022-05-02

### Added

- Notifications for outages
- More comprehensive error messages on Add geotag

## [0.15.0] - 2022-04-07

## Fixed

- ANR on getting geocoding addresses

## Added

- Measurement unit system selection (meters/miles)

## [0.14.2] - 2022-03-01

### Changed

- Hypertrack SDK 6.0.1

## [0.14.1] - 2022-02-23

### Changed

- Hypertrack SDK 6.0.0

## [0.14.0] - 2022-02-16

### Changed

- Hypertrack SDK 6.0.0-beta.2

## [0.13.4] - 2022-02-11

### Added

- History timeline header arrow is now clickable (click opens the timeline)

### Fixed

- Wrong minutes value in timeline drive duration field

## [0.13.3] - 2022-02-04

### Fixed

- Sign in screen showing when login via deeplink for the first time
- Layout issue in Deeplink issues dialog

## [0.13.2] - 2022-02-04

### Fixed

- Bug with wrong month in date picker
- Crash with NO_POSITION in RecyclerView
- Crash when onBackPressed called on detached fragment

## Added

- Showing locale and measurement units for debug on Profile screen

## [0.13.1] - 2022-01-26

### Fixed

- Remove timeline tile notch

## [0.13.0] - 2022-01-26

### Added

- Showing history for different date

## [0.12.2] - 2022-01-17

### Added

- Deeplink error reporting

## [0.12.1] - 2022-01-10

### Fixed

- Branch.io connection error when device have outdated Security Provider
- Stucking in loading state when logging in using login token

## [0.12.0] - 2021-12-09

### Fixed

- Order details marker icon
- Firebase message icon
- Trip route polyline starting point
- Map padding when displaying trip
- Bug that caused logout on new trip push message click

### Added

- "Completed at" and "Scheduled at" fields on Order details screen
- Hiding empty note for completed order on Order details screen
- Copy integration name feature on Place details screen
- Showing integration id on Add integration screen

## [0.11.2] - 2021-12-03

### Fixed

- Decreased Places visits tab loading time

### Added

- Auto-refresh Places visits on tab opened

## [0.11.1] - 2021-11-18

### Added

- Login by login token

## [0.11.0] - 2021-11-16

### Added

- Login by pasting deeplink

## [0.10.11] - 2021-11-05

### Updated

- HyperTrack SDK 5.4.5

## [0.10.10] - 2021-11-01

### Updated

- HyperTrack SDK 5.4.4

## [0.10.9] - 2021-10-26

### Fixed

- Crash on Summary tab

## [0.10.8] - 2021-10-20

### Added

- Snoozing orders
- Complete trip button

## [0.10.7] - 2021-10-6

- Changed orders sorting on Orders tab

## [0.10.6] - 2021-10-6

- Bugfix

## [0.10.5] - 2021-09-27

- Adding place flow refactoring
- Creating places adjacent to existing ones is now prohibited

## [0.10.4] - 2021-09-26

- Changed max and default geofence radius

## [0.10.3] - 2021-09-03

- HyperTrack SDK 5.4.3
- Bugfixes

## [0.10.2] - 2021-08-30

- HyperTrack SDK 5.4.0
- Polygon geofences
- Bugfixes

## [0.10.1] - 2021-08-13

- Geofence name and address in visits list
- Bugfixes

## [0.10.0] - 2021-08-12

- Geofences visits list
- Daily distance stats
- Order note fix

## [0.9.23] - 2021-08-09

- HyperTrack SDK 4.4.0
- Deeplink fixes

## [0.9.22] - 2021-07-22

- Sign Up removed
- Deeplink fixes

## [0.9.21] - 2021-07-20

- Deeplink fixes

## [0.9.20] - 2021-07-16

- Google place name bug fixed
- New deeplink format

## [0.9.19] - 2021-07-07

- Places and Trips experience fixes
- Email feedback form

## [0.9.18] - 2021-06-25

- Bugfixes

## [0.9.17] - 2021-06-24

### Fixed

- Current trip not displaying on map

## [0.9.16] - 2021-06-21

### Added

- Trips with multiple orders
- Geofences are now displayed on the Current trip map

## [0.9.15] - 2021-06-19

### Added

- Geofences are now displayed on the Add place map

## [0.9.13] - 2021-06-17

### Changed

- Updated to use HyperTrack
  SDK [v5.2.4](https://github.com/hypertrack/sdk-android/blob/master/CHANGELOG.md#524---2021-06-17)

## [0.9.12] - 2021-06-15

### Changed

- Updated to use HyperTrack
  SDK [v5.2.3](https://github.com/hypertrack/sdk-android/blob/master/CHANGELOG.md#523---2021-06-15)

## [0.9.11] - 2021-06-15

- Bugfixes

## [0.9.10] - 2021-06-14

### Fixed

- Fixed geofence device_id error

## [0.9.9] - 2021-06-14

### Fixed

- Fixed crash on login

## [0.9.8] - 2021-06-11

### Changed

- Updated to use HyperTrack
  SDK [v5.2.2](https://github.com/hypertrack/sdk-android/blob/master/CHANGELOG.md#522---2021-06-11)

## [0.9.7] - 2021-06-10

### Fixed

- Fixed crash on login

## [0.9.6] - 2021-06-10

### Fixed

- Tracking service is automatically restarts on reboot now

### Changed

- HyperTrack SDK updated to v5.2.1

## [0.9.5] - 2021-06-08

### Changed

- HyperTrack SDK updated to v5.2.0

## [0.9.4] - 2021-06-04

### Changed

- Clock In geotags removed

## [0.9.3] - 2021-06-03

### Changed

- Background permission rationale changed.

## [0.9.2] - 2021-06-02 (UNRELEASED)

### Changed

- Background permission rationale style changed.

## [0.9.1] - 2021-06-01 (UNRELEASED)

### Fixed

- Driver ID input screen on older Androids UI fix
- Hubspot integration company name

## [0.9.0] - 2021-05-31 (UNRELEASED)

### Added

- Background location permission to use scheduled tracking hours

### Fixed

- Geofences related changes

## [0.8.2] - 2021-05-27

### Fixed

- Geofence list pagination

## [0.8.1] - 2021-05-27

### Fixed

- Geofence list pagination

## [0.8.0] - 2021-05-26

### Added

- Hubspot integration

## [0.7.2] - 2021-05-06

### Added

- New launcher and notification icons

### Fixed

- NPE crash on lateinit property not being initialized.

## [0.7.1] - 2021-04-23

### Fixed

- A crash when no last location is returned from the OS

## [0.7.0] - 2021-04-22

### Added

- Create trip from the app (Where are you going appearence)
- CLOCK_IN / CLOCK_OUT geotags were removed

## [0.6.11] - 2021-04-20

### Added

- Copy button for profile data

## [0.6.10] - 2021-04-19

### Fixed

- Crash on places screen fixed for OPPO devices

## [0.6.9] - 2021-04-16

### Added

- Route info in place visit details
- Copy Visit ID button at Visit details screen

### Changed

- Places list is now sorted by last visit

### Fixed

- Minor text fixes

## [0.6.8] - 2021-04-15

### Fixed

- Crash on visits list fixed

## [0.6.7] - 2021-04-13

### Fixed

- Crash in case if there no internet connection fixed

## [0.6.6] - 2021-04-13

### Fixed

- Visits list text changes
- Local visit UX changes and fixes

## [0.6.5] - 2021-04-09

### Fixed

- Mock locations were disallowed

## [0.6.4] - 2021-04-08

### Added

- Added Place visits timeline

### Changed

- Changed Select location UX

### Fixed

- Fixed missing street number in address when creating geofence

## [0.6.3] - 2021-04-07

### Added

- Added Place creation screen

## [0.6.2] - 2021-04-06

### Updated

- HyperTrack SDK was updated to v4.11.0

## [0.6.1] - 2021-03-31

- Manual Visit creation enabled by default
- Bugfix

## [0.6.0] - 2021-03-31

- Added Places tab
- Added Place details

## [0.5.3] - 2021-03-26

- Minor Sign Up changes

## [0.5.2] - 2021-03-18

- Fixed a bug that could lead to crash and null error snackbar on a map view.

## [0.5.1] - 2021-03-16

- Bug with map focusing on 0,0 coordinates when no history available fixed

## [0.5.0] - 2021-03-16

- Sign Up for HyperTrack without leaving the app.
- Interactive timeline to review your daily history.
- Minor UI improvements

## [0.4.0] - 2021-03-11

- We made the map view default screen
- Tracking now starts automatically for the very first app launch.
- Some labels were replaced with icons to preserve space and improve usability
- Geotags payload was made self-explanatory

## [0.3.1] - 2021-03-03

- Daily stats in the Summary tab
- Profile tab was added to explore the associated data
- You can attach multiple photos to each visit
- UI update to achieve better usability

## [0.2.13] - 2021-01-26

- Added whitelisting prompt for Xiaomi, OnePlus and alike.
- Visit completion events have their expected location attached.

## [0.2.12] - 2021-01-26

- Switched to efficient geofences API with markers included
- Replased Gson and Mockito with Moshi and MockK counterparts
- Visit click crash was fixed

## [0.2.11] - 2021-01-26

- Notification about device being deleted
- Non-blocking visits refresh
- Pull to refresh instead of button click
- Pick-Up button configurability via deeplink parameter.
- Local visit UI fixed.
- Bugfixes

## [0.2.10] - 2021-01-26

- HyperTrack SDK updated to v4.9.0
- Gson nullability issue fixed

## [0.2.9] - 2020-12-21

- Proof of Deliver photo can be attached to each visit
- Crashlytics integration
- Timeouts were incremented to 30 secs.

## [0.2.8] - 2020-12-15

- Auto check in for Trips and Geofences
- Updated visit state model to match Pick Up -> Check In -> Check Out / Cancel graph
- Daily history view added

## [0.2.7] - 2020-11-27

- Added login with HyperTrack Dashboard credentials
- Misc behavior changes

## [0.2.6-devpoc] - 2020-11-04

- Fixed issue with manual visits configuration been ignored

## [0.2.6] - 2020-10-30

- Added local visits and driver id configurability via deeplink

## [0.2.6-rc01] - 2020-10-27

- CheckIn/CheckOut is only available if configured in deeplinks
- Driver id can be passed as a deeplink parameter

## [0.2.5] - 2020-10-06

- Trip metadata entries, that have their keys starts with "ht_" prefix, aren't shown in customer
  notes.

## [0.2.4] - 2020-10-01

- Fixed a crash on network error.


