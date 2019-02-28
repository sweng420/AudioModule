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
import java.util.Arrays;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;

public class AudioRecorder
{
	static TargetDataLine targetLine = null;
	static final int maxRecordingTime = 3600000;
	Thread recordThread;
	
	public static void main(String[] args)
	{
		try
		{
			// Initialise audio format settings and setup data line matching format specification
			
			targetLine = initialiseAudioSettings();
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
		}
	}
	
	private static TargetDataLine initialiseAudioSettings() throws LineUnavailableException
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
		
		TargetDataLine targetLine = (TargetDataLine)AudioSystem.getLine(audioFormatInfo);
		
		// Instruct the system to allocate resources to the targetLine and switch it on
		
		targetLine.open();
		
		// Prepare line for audio input
		
		targetLine.start();
		
		return targetLine;
	}
	
	private void startRecording()
	{
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
		
		// Record for max recording time of 60 minutes
		
		try
		{
			recordThread.sleep(maxRecordingTime);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	private void pauseRecording()
	{
		targetLine.stop();
	}
	
	private void resumeRecording()
	{
		// TODO: in practice may be able to merge this with pauseRecording() but leave for now
		
		targetLine.start();
	}
	
	private void stopRecording()
	{
		// Cease all I/O functions of the line
		
		targetLine.stop();
		
		// Close the line, deallocating system resources and deleting metadata
		
		targetLine.close();
		
		//
		// /TEST
		//
		
		recordThread.stop();
	}
}
