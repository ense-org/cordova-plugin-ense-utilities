
var exec = require('cordova/exec');

var PLUGIN_NAME = 'EnseUtilities';

var EnseUtilities = {
  echo: function(phrase, successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'echo', [phrase]);
  },
  getDate: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'getDate', []);
  },
  getDeviceSecretKey: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'getDeviceSecretKey', []);
  },
  startRecording: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'startRecording', []);
  },
  stopRecording: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'stopRecording', []);
  },
  playAudioFile: function(filepath, successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'playAudioFile', [filepath]);
  },
  pauseAudioPlayback: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'pauseAudioPlayback', []);
  },
  resumeAudioPlayback: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'resumeAudioPlayback', []);
  },
  stopAudioPlayback: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'stopAudioPlayback', []);
  },
  getPlaybackPosition: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'getPlaybackPosition', []);
  },
  getPlaybackDuration: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'getPlaybackDuration', []);
  },
  uploadFile: function(uploadFilePath, mimetype, uploadKey, policyDoc, policySig, successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'uploadFile', [uploadFilePath, mimetype, uploadKey, policyDoc, policySig]);
  },
  logOut: function(successCallback) {
    exec(successCallback, null, PLUGIN_NAME, 'logOut', []);
  },

};

module.exports = EnseUtilities;
