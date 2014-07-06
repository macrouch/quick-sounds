package com.sleekcoder.quick_sounds;

import android.media.MediaPlayer;

import java.io.IOException;

public class SoundPlayer {
    static MediaPlayer mp;

    public static void playSound(String filepath, boolean loop){
        mp = new MediaPlayer();
        prepareMedia(filepath,loop,mp);
        mp.start();
    }

    /** This method prepares the media player with the given file */
    public static void prepareMedia(String file, boolean loop, MediaPlayer player) {
        try {
            player.reset();
            player.setDataSource(file);
            player.setLooping(loop);
            player.prepare();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
