package com.schoenberger.gdx.util;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
public class AudioManager 
{
	public static final AudioManager instance = new AudioManager();
	
	private Music playingMusic;
	
	// singleton: prevent instantiation from other classes
	private AudioManager(){}
	
	/**
	 * Plays a sound.
	 * @param sound
	 */
	public void play (Sound sound)
	{
		play(sound , 1);
	}
	
	/**
	 * Plays a sound with adjustable volume.
	 * @param sound
	 * @param volume
	 */
	public void play (Sound sound, float volume)
	{
		play(sound, volume, 1);
	}
	
	/**
	 * Plays a sound with adjustable volume and pitch.
	 * @param sound
	 * @param volume
	 * @param pitch
	 */
	public void play (Sound sound, float volume, float pitch) 
	{
		play(sound, volume, pitch, 0);
	}
	
	/**
	 * Finally, plays sound with adjustable volume pitch and pan.r
	 * @param sound
	 * @param volume
	 * @param pitch
	 * @param pan
	 */
	public void play (Sound sound, float volume, float pitch, float pan) 
	{
		if (!GamePreferences.instance.sound) return;
		sound.play(GamePreferences.instance.volSound*volume, pitch, pan);
	}
	
	/**
	 * Plays music
	 * @param music
	 */
	public void play (Music music) 
	{
		stopMusic();
		playingMusic = music;
		if (GamePreferences.instance.music)
		{
			music.setLooping(true);
			music.setVolume(GamePreferences.instance.volMusic);
			music.play();
		}
	}
	
	/**
	 * Stops the music.
	 */
	public void stopMusic()
	{
		if (playingMusic != null) playingMusic.stop();
	}
	
	public void onSettingsUpdated()
	{
		if (playingMusic == null) return;
		playingMusic.setVolume(GamePreferences.instance.volMusic);
		if (GamePreferences.instance.music)
		{
			if (!playingMusic.isPlaying()) playingMusic.play();
		} 
		else
		{
			playingMusic.pause();
		}
	}
}
