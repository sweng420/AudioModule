// Copyright © 2019 York Software Solutions. All Rights Reserved.
//
// The recipient may not distribute, license, loan or sell any part
// of the code contained herein under the Copyright, Designs and Patents Act 1988.
//
// Removal, modification or obfuscation of this trademark shall be considered a 
// breach of the terms of the license and may result in legal action.
//
// Module Name: AudioRecorder
//
// Description: 
//
// A module for the recording of a .wav (WAVE) file of maximum length 60 minutes
// at CD standard sampling rate (44.1 kHz) using the Windows default input device.

// Authors: Louis Cowell, Sam Merryweather
//
// Date Created: 27/02/19

// TODO: user defined file path
// TODO: implement gain control: https://docs.oracle.com/javase/tutorial/sound/controls.html 
// TODO: Generate 'session ID' to implement pausing across given time period?
// TODO: implement pause function using concatenation: https://stackoverflow.com/questions/653861/join-two-wav-files-from-java 

// USEFUL LINKS

// TODO: https://github.com/frohoff/jdk8u-dev-jdk/blob/master/src/share/classes/com/sun/media/sound/WaveFileWriter.java
// TODO: https://stackoverflow.com/questions/3297749/java-reading-manipulating-and-writing-wav-files
// TODO: https://docs.oracle.com/javase/tutorial/sound/converters.html

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
	static boolean pauseFlag = false;
	
	// String fileName = "e:/new/recording_" + sdf.format(new Date()) + ".wav";
	static File outputFile = new File("C:/temp/test.wav");
	static File outputFileTwo = new File("C:/temp/test2.wav");
	static AudioInputStream audioInputStream;
	
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
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		stopRecording();
		
		try
		{
			Thread.sleep(500);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		startRecording();
		
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		stopRecording();
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
	}
	
	private static void startRecording()
	{
		// Instruct the system to allocate resources to the targetLine and switch it on
		
		try
		{
			targetLine.open();
		} catch (LineUnavailableException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Instruct audio stream to listen to target line 
		
		audioInputStream = new AudioInputStream(targetLine);
		
		TimerTask scheduleRecordingEnd = new TimerTask()
			{
				public void run()
				{
					stopRecording();
				}
			};
				
		Timer recordingTimer = new Timer("Recording Timer");
		
		recordingTimer.schedule(scheduleRecordingEnd, maxRecordingTime);
		
		// Prepare line for audio input
		
		targetLine.start();
		
		// Setup recording thread
		
		recordThread = new Thread(new Runnable()
		{	
			@Override
			public void run()
			{
				try
				{
					// If output file already exists, we must append it

					if (outputFile.exists() && !outputFile.isDirectory())
					{
						//System.out.println("TEST: File existed!");

						AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFileTwo);

						// FileOutputStream outputStream = new
						// FileOutputStream(outputFile, true);

						// System.out.println(audioInputStream.getFormat().toString());
						// System.out.println(audioInputStream.getFrameLength());

						// This is -1 because audioInputStream records 'live'
						// data of unknown length
						// This causes IOException 'stream length not
						// specified'.

						// Could write to a temp file then append existing file?
						// Could write to AU then convert to WAVE?

						// https://stackoverflow.com/questions/598344/java-audiosystem-and-targetdataline

						// AudioSystem.write(audioInputStream,
						// AudioFileFormat.Type.WAVE, outputStream);
						
					} else
					{
						//System.out.println("TEST: File did not yet exist!");

						// If output file does not yet exist, write audio input stream to file

						AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
					}
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		// Start recording
		
		recordThread.start();
		
		System.out.println("Recording started...");
	}
	
	private static void stopRecording()
	{	
		// Cease all I/O functions of the line
		
		targetLine.stop();
		
		// Close the line, deallocating system resources and deleting metadata
		
		targetLine.close();
		
		// Close the input stream and deallocate system resources
		
		try
		{
			audioInputStream.close();
		} catch (IOException e)
		{
			System.out.println("Could not close input stream");
			e.printStackTrace();
		}
		
		System.out.println("Stopping recording...");
		
		recordThread.stop();
		
		if (pauseFlag == true)
		{
			appendWavFiles(outputFile, outputFileTwo);
		}
		
		System.out.println("Recording stopped...");
		
		pauseFlag = true;
	}
	
	// Append two .wav files and store them in a separate file
	
	private static void appendWavFiles(File fileOne, File fileTwo)
	{
		try
		{
			File appendedFile = new File("C:/temp/appended.wav");
			
			AudioInputStream streamOne = AudioSystem.getAudioInputStream(fileOne);
			AudioInputStream streamTwo = AudioSystem.getAudioInputStream(fileTwo);
			
			AudioInputStream appendedStreams = new AudioInputStream(
													new SequenceInputStream(streamOne, streamTwo),
													streamOne.getFormat(),
													streamOne.getFrameLength() + streamTwo.getFrameLength());
			
			AudioSystem.write(appendedStreams, AudioFileFormat.Type.WAVE, appendedFile);
			
		} catch (Exception e)
		{
			System.out.println("An error occurred while appending files");
			e.printStackTrace();
		}
	}
}
