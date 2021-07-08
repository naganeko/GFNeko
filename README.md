GFNeko
=====

![screenshot2](https://naganeko.github.io/GFNeko/images/shark1.jpg)

![screenshot1](https://naganeko.github.io/GFNeko/images/screenshot.jpg)


Download
------------
https://github.com/choiman1559/GFNeko/releases/latest

Introduction
------------
GF version of [ANeko](https://github.com/lllllT/ANeko)
tested on Android 12 Beta 2.1 and Android 8.1

Build
-----

Prerequisites:

* [Android Studio](https://developer.android.com/studio)

How to Build :
* File -> Open -> Select Project
* Sync Project with Gradle

Then clean, compile and run.


To create skin
--------------

See [how-to-make skin file(korean)](https://gall.dcinside.com/mgallery/board/view/?id=micateam&no=857787) for internal file method. 

### Changelogs from legecy GFNeko
 - changed minSDK to 19 (Android 4.4)
 - changed targetSDK to 30 (Android 11)
 - Migrated Support library to AndroidX
 - Changed PreferenceActivity to PreferenceFragmentCompat and AppCompatActivity
 - fixed ACTION_MANAGE_OVERLAY_PERMISSION problem on SDK 23 (Android 6.0) or more
 - Added WRITE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE on manifest
 - Added android.permission.MANAGE_EXTERNAL_STORAGE on manifest (for Android 11+)
 - Deleted SkinPreference.java
 - Migrated Eclipse ADT project to Intelij IDEA Gradle style project
 - Deleted behaviour option and added size option
 - Optimize performance
