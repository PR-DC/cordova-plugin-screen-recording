/**
 * ScreenRecord JavaScript class for cordova-plugin-screen-recording
 * Author: Milos Petrasinovic <mpetrasinovic@pr-dc.com>
 * PR-DC, Republic of Serbia
 * info@pr-dc.com
 * 
 * --------------------
 * Copyright (C) 2022 PR-DC <info@pr-dc.com>
 *
 * This file is part of ScreenRecord.
 *
 * ScreenRecord is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * ScreenRecord is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ScreenRecord.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
 
var exec = require('cordova/exec');
var argsCheck = require('cordova/argscheck');

var ScreenRecord = {
  startRecord: function(opts, filePath, successCallback, errorCallback) {
    var getValue = argsCheck.getValue;
    opts = {
      recordAudio: getValue(opts.recordAudio, false),
      bitRate: getValue(opts.bitRate, 6 * 1000000),
      title: getValue(opts.title, 'Screen Recording'),
      text: getValue(opts.text, 'Screen recording active...')
    };
    cordova.exec(
      successCallback, 
      errorCallback, 
      "ScreenRecord", 
      "startRecord", 
      [opts, filePath]
    );
  },
  stopRecord: function(successCallback, errorCallback) {
    cordova.exec(
      successCallback, 
      errorCallback, 
      "ScreenRecord", 
      "stopRecord", 
      []
    );
  }
}

module.exports = ScreenRecord;
