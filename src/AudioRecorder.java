// Copyright © 2019 York Software Solutions. All Rights Reserved.
//
// The recipient may not distribute, license, loan or sell any part
// of the code contained herein under the Copyright, Designs and Patents Act 1988.
//
// Removal, modification or obfuscation of this trademark shall be considered a 
// breach of the terms of the license and may result in legal action
//
// Module Name: AudioRecorder
//
// Description: 
//
// A module for the recording of a .wav (WAVE) file of maximum length 60 minutes
// at CD standard sampling rate (44.1 kHz) using the Windows default input device

// Authors: Louis Cowell
//
// Date Created: 27/02/19

// TODO: user defined file path

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;

public class AudioRecorder
{
	static TargetDataLine targetLine = null;
	static final long maxRecordingTime = 3600000; // One hour in milliseconds
	static Thread recordThread;
	
	public static void main(String[] args)
	{
		try
		{
			// Initialise audio format settings and setup data line matching format specification
			
			initialiseAudioSettings();
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
		}
		
		// TEST
		
		startRecording();
		
		try
		{
			System.out.println("Sleeping for 5s...");
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		System.out.println("About to pause recording...");
		
		pauseRecording();
		targetLine.stop();
		
		try
		{
			System.out.println("Sleeping for 5s...");
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		System.out.println("About to resume recording...");
		
		resumeRecording();
		
		try
		{
			System.out.println("Sleeping for 5s...");
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		System.out.println("About to stop recording...");
		
		stopRecording();
		
		System.out.println("Recording stopped...(in theory)");
		
		// /TEST
	}
	
	private static void initialiseAudioSettings() throws LineUnavailableException
	{
		// Define audio format as:
		// Encoding: Linear PCM
		// Sample Rate: 44.1 kHz
		// Bit Depth: 16-bit
		// Channel Format: Stereo
		// Data Storage: Signed & Big-Endian
		
		final AudioFormat audioFormat = new AudioFormat(44100, 16, 2, true, true);
		
		// Store format metadata in an Info variable 
		
		final DataLine.Info audioFormatInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
		
		if (!AudioSystem.isLineSupported(Port.Info.MICROPHONE))
		{
			throw new LineUnavailableException("No microphone has been detected. Please reconnect the microphone and try again.");
		} else
		{
			System.out.println("Microphone detected. Querying target data line...");
		}
				
		// Use metadata to ascertain whether audio format is supported by default input device
				
		if (AudioSystem.isLineSupported(audioFormatInfo) == false)
		{
			throw new LineUnavailableException("The default input device does not support the specified audio output format");
		}
		
		// Get a line matching the specified audio format 
		
		targetLine = (TargetDataLine)AudioSystem.getLine(audioFormatInfo);
		
		// Instruct the system to allocate resources to the targetLine and switch it on
		
		targetLine.open();
		
		// Prepare line for audio input
		
		targetLine.start();
	}
	
	private static void startRecording()
	{
		TimerTask scheduleRecordingEnd = new TimerTask()
				{
					public void run()
					{
						stopRecording();
					}
				};
				
		Timer recordingTimer = new Timer("Recording Timer");
		
		recordingTimer.schedule(scheduleRecordingEnd, maxRecordingTime);
		
		// Setup recording thread
		
		recordThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
		        {
		        	// Route audio input stream to target data line
					
					AudioInputStream audioInputStream = new AudioInputStream(targetLine);
					
					// Instantiate output filepath & filename
					
					File outputFile = new File("C:/temp/test.wav");
					
					// Write input audio to output .wav file
					
					try
					{
						AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
		        }
			}
		});
		
		// Start recording
		
		recordThread.start();
	}
	
	private static void pauseRecording()
	{
		targetLine.stop();
	}
	
	private static void resumeRecording()
	{
		// TODO: in practice may be able to merge this with pauseRecording() but leave for now
		
		targetLine.start();
		
		if (targetLine.isRunning())
		{
			System.out.println("Running!");
		}
	}
	
	private static void stopRecording()
	{
		// Cease all I/O functions of the line
		
		targetLine.stop();
		
		// Close the line, deallocating system resources and deleting metadata
		
		targetLine.close();
		
		System.out.println("Stopping recording...");
		
		recordThread.stop();
	}
}
