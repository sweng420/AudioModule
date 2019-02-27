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

import java.io.*;
import javax.sound.sampled.*;

public class AudioRecorder
{
	public static void main(String[] args)
	{
		try
		{
			initialiseAudioSettings();
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
		}
	}
	
	private static void initialiseAudioSettings() throws LineUnavailableException
	{
		// Define audio format as:
		// Encoding: Linear PCM
		// Sample Rate: 44.1 kHz
		// Bit Depth: 16-bit
		// Channel Format: Mono
		// Data Storage: Signed & Big-Endian
		
		final AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, true);
		
		// Store format metadata in an Info variable 
		
		final DataLine.Info audioFormatInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
				
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
		
		// Setup recording thread
		
		Thread recordThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
		        {
		        	// Route audio input stream to target data line
					
					AudioInputStream audioInputStream = new AudioInputStream(targetLine);
					
					// Instantiate output filepath & filename
					
					File outputFile = new File("C:\temp\test.wav");
					
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
		
		// 
		// TEST
		//
		
		// Record for 5 seconds
		// In practice we will have a startRecording / stopRecording functionality
		
		try
		{
			recordThread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		// Cease all I/O functions of the line
		
		targetLine.stop();
		
		// Close the line, deallocating system resources and deleting metadata
		
		targetLine.close();
		
		//
		// /TEST
		//
		
		recordThread.stop();
			
	}
	
	private void startRecording()
	{
		
	}
	
	private void pauseRecording()
	{
		
	}
	
	private void stopRecording()
	{
		
	}
	
	private void saveRecording()
	{
		
	}
}
