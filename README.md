## PR-DC cordova-plugin-screen-recording

Cordova plugin for Android Screen Recording (Screen Capture). This plugin makes it possible to save screen-recording as a video to the device's gallery, it requests neccery permissions when the user starts recording for the first time and creates a notification that is active while the screen is being recorded.

## Requirements
[Apache Cordova](https://cordova.apache.org/)<br>

This library is tested with
**Cordova Android 9.1.0**

## Installation
From the root folder of your Cordova project, run:
```
cordova plugin add https://github.com/PR-DC/cordova-plugin-screen-recording.git
```

## Usage
First, append the following preference to the project's `config.xml` document.
```xml
<preference name="AndroidXEnabled" value="true" />
```

Now you can proceed with the app code, start recording with:
```js
ScreenRecord.startRecord(opts, fileName, function success(), function error());
```
`opts` is a JSON object with the following properties:

- `recordAudio`: whether to record audio or not, defaults to `false`
- `bitRate`: video bitrate, defaults to `6000000`
- `title`: notification title, defaults to `Screen Recording`
- `text`: notification text, defaults to `Screen recording active...`

To stop recording call the following method:
```js
ScreenRecord.stopRecord(function success(), function error());
```
When recording is stopped video is saved to memory and then you can play the video from the gallery.

## Example of Cordova application

An example of Cordova application that uses cordova-plugin-screen-recording is available at: https://github.com/PR-DC/PRDC_TestScreenRecording

## License
Copyright (C) 2022 PR-DC <info@pr-dc.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as 
published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.