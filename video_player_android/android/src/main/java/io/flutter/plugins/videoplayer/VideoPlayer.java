// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

//import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
//import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

import android.content.Context;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import io.flutter.Log;
import android.os.Build;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
//import com.google.android.exoplayer2.C;
//import com.google.android.exoplayer2.ExoPlayer;
//import com.google.android.exoplayer2.Format;
//import com.google.android.exoplayer2.MediaItem;
//import com.google.android.exoplayer2.PlaybackException;
//import com.google.android.exoplayer2.PlaybackParameters;
//import com.google.android.exoplayer2.Player;
//import com.google.android.exoplayer2.Player.Listener;
//import com.google.android.exoplayer2.audio.AudioAttributes;
//import com.google.android.exoplayer2.source.MediaSource;
//import com.google.android.exoplayer2.source.ProgressiveMediaSource;
//import com.google.android.exoplayer2.source.dash.DashMediaSource;
//import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
//import com.google.android.exoplayer2.source.hls.HlsMediaSource;
//import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
//import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
//import com.google.android.exoplayer2.upstream.DataSource;
//import com.google.android.exoplayer2.upstream.DefaultDataSource;
//import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
//import com.google.android.exoplayer2.util.Util;
import io.flutter.plugin.common.EventChannel;
import io.flutter.view.TextureRegistry;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class VideoPlayer {
  private static final String FORMAT_SS = "ss";
  private static final String FORMAT_DASH = "dash";
  private static final String FORMAT_HLS = "hls";
  private static final String FORMAT_OTHER = "other";

  private MediaPlayer mediaPlayer;

  private Surface surface;

  private final TextureRegistry.SurfaceTextureEntry textureEntry;

  private QueuingEventSink eventSink;

  private final EventChannel eventChannel;

  @VisibleForTesting boolean isInitialized = false;

  private final VideoPlayerOptions options;

  VideoPlayer(
      Context context,
      EventChannel eventChannel,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      String dataSource,
      String formatHint,
      @NonNull Map<String, String> httpHeaders,
      VideoPlayerOptions options)  {
    this.eventChannel = eventChannel;
    this.textureEntry = textureEntry;
    this.options = options;

    MediaPlayer player = new MediaPlayer();
    try {
      player.setDataSource(dataSource);

      surface = new Surface(textureEntry.surfaceTexture());
      Log.wtf("setUpVideoPlayer", "new Surface: is called");

      player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          Log.wtf("VideoPlayer", "onPrepared: is called");
          mp.setSurface(surface);
          setUpVideoPlayer(mp, new QueuingEventSink());
          mp.start();
          mp.pause();
        }});
      player.prepare();


    }
    catch (IOException e){

    }



  }

  // Constructor used to directly test members of this class.
  @VisibleForTesting
  VideoPlayer(
      MediaPlayer mediaPlayer,
      EventChannel eventChannel,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      VideoPlayerOptions options,
      QueuingEventSink eventSink) {
    this.eventChannel = eventChannel;
    this.textureEntry = textureEntry;
    this.options = options;

    setUpVideoPlayer(mediaPlayer, eventSink);
  }


  private void setUpVideoPlayer(MediaPlayer mediaPlayer, QueuingEventSink eventSink) {
    this.mediaPlayer = mediaPlayer;
    this.eventSink = eventSink;

    eventChannel.setStreamHandler(
            new EventChannel.StreamHandler() {
              @Override
              public void onListen(Object o, EventChannel.EventSink sink) {
                Log.wtf("setUpVideoPlayer", "onListen: is called");
                eventSink.setDelegate(sink);
              }

              @Override
              public void onCancel(Object o) {
                Log.wtf("setUpVideoPlayer", "onCancel: is called");


                eventSink.setDelegate(null);
              }
            });
    Log.wtf("setUpVideoPlayer", "new Surface: is called");

    Log.wtf("setUpVideoPlayer", "setSurface: is called");
    if (!isInitialized) {
      isInitialized = true;
      sendInitialized();
    }

  }


  void play() {
    mediaPlayer.start();
  }

  void pause() {
    mediaPlayer.pause();
  }

  void setLooping(boolean value) {
  }

  void setVolume(double value) {
  }

  void setPlaybackSpeed(double value) {
  }

  void seekTo(int location) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      mediaPlayer.seekTo(location, MediaPlayer.SEEK_CLOSEST);
    }
  }

  long getPosition() {
    return mediaPlayer.getCurrentPosition();
  }

  @SuppressWarnings("SuspiciousNameCombination")
  @VisibleForTesting
  void sendInitialized() {
    if (isInitialized) {
      Map<String, Object> event = new HashMap<>();
      event.put("event", "initialized");
      event.put("duration", mediaPlayer.getDuration());

        int width = mediaPlayer.getVideoWidth();
        int height = mediaPlayer.getVideoHeight();
        event.put("width", width);
        event.put("height", height);



      eventSink.success(event);
    }
  }

  void dispose() {
    if (isInitialized) {
      mediaPlayer.stop();
    }
    textureEntry.release();
    eventChannel.setStreamHandler(null);
    if (surface != null) {
      surface.release();
    }
    if (mediaPlayer != null) {
      mediaPlayer.release();
    }
  }
}
