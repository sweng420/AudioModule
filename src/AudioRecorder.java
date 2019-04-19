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

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;
import java.util.Date;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFileFormat.Type;

public class AudioRecorder
{
	static TargetDataLine targetLine = null;
	static final long maxRecordingTime = 3600000; // One hour in milliseconds
	static Thread recordThread;
	static boolean pauseFlag = false;
	
	static String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // Today's date
	static File dir = new File("C:/temp/"); // Root directory
	static String fileNameWithSessionID; // Output filename 
	static File outputFile = new File("C:/temp/" + date + ".wav"); 
	static File tempFile = new File("C:/temp/temp.wav"); 
	static AudioInputStream audioInputStream;
	
	static int sessionID = 1;
	
	public static void main(String[] args)
	{
		File[] matches = dir.listFiles(new FilenameFilter()
		{
		  public boolean accept(File dir, String name)
		  {
		     return name.startsWith(date) && name.endsWith(".wav");
		  }
		});
		
		System.out.println(Arrays.toString(matches));
		
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
		
		try 
		{
			Files.delete(outputFile.toPath());
			Files.delete(tempFile.toPath());
		} catch (IOException e)
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

						AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, tempFile);
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
		
		recordThread.stop();
		
		if (pauseFlag == true)
		{
			appendWavFiles(outputFile, tempFile);
		}
		
		System.out.println("Recording stopped...");
		
		pauseFlag = true;
	}
	
	// Append two .wav files and store them in a separate file
	
	private static void appendWavFiles(File fileOne, File fileTwo)
	{
		try
		{
			fileNameWithSessionID = date.concat("_Session" + sessionID + ".wav");
			File appendedFile = new File("C:/temp/" + fileNameWithSessionID);
			
			AudioInputStream streamOne = AudioSystem.getAudioInputStream(fileOne);
			AudioInputStream streamTwo = AudioSystem.getAudioInputStream(fileTwo);
			
			AudioInputStream appendedStreams = new AudioInputStream(
													new SequenceInputStream(streamOne, streamTwo),
													streamOne.getFormat(),
													streamOne.getFrameLength() + streamTwo.getFrameLength());
			
			AudioSystem.write(appendedStreams, AudioFileFormat.Type.WAVE, appendedFile);
			
			streamOne.close();
			streamTwo.close();
			appendedStreams.close();
			
			System.gc(); // lol
		} catch (Exception e)
		{
			System.out.println("An error occurred while appending files");
			e.printStackTrace();
		}
	}
}
