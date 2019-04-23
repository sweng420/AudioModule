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

// TODO: implement gain control: https://docs.oracle.com/javase/tutorial/sound/controls.html 

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;

import javax.sound.sampled.*;

public class AudioRecorder
{
	static TargetDataLine targetLine = null;
	static final long maxRecordingTime = 3600000; 								// One hour in milliseconds
	static Thread recordThread;
	static boolean pauseFlag = false;
	
	static String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // Today's date
	static File dir = new File("C:/temp/"); 									// Root directory
	static String fileNameWithSessionID; 										// Output filename 
	static File recordFile = new File("C:/temp/" + date + ".wav"); 				// First file to be recorded to
	static File tempFile = new File("C:/temp/temp.wav"); 						// Second file to be appended to first file
	static File copyFile;														// File to be copied into output file
	static File appendedFile;													// Output file
	static AudioInputStream audioInputStream; 									// Microphone input stream 
	
	static int sessionID = 1;
	
	public static void main(String[] args)
	{
		// Ensure that there is a unique session ID for this session 
		
		generateSessionID();
		
//		Alternatively...
//		manuallyEditSessionID();
		
		// Create output file
		
		createNewOutputFile();
		
		try
		{
			// Initialise audio format settings and setup data line matching format specification
			
			initialiseAudioSettings();
		}
		catch (Exception e)
		{
			// Do nothing, since error message is handled inside function 
		}
		
		// TEST
		
		for (int i = 0; i < 4; i++)
		{
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
				
		// Use metadata variable to ascertain whether audio format is supported by default input device
				
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
			System.out.println("No line available for audio capture");
			e1.printStackTrace();
		}
		
		// Instruct audio stream to listen to target line 
		
		audioInputStream = new AudioInputStream(targetLine);
		
		// Ensure recording cannot exceed one hour
		
		scheduleRecordingEnd();
		
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

					if (recordFile.exists() || appendedFile.length() != 0)
					{
						AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, tempFile);
					} else
					{
						// If output file does not yet exist, write audio input stream to file

						AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, recordFile);
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
			if (appendedFile.exists())
			{
				appendWavFiles(appendedFile, tempFile, false);
			} else
			{
				appendWavFiles(recordFile, tempFile, true);
			}
		}
		
		System.out.println("Recording stopped...");
		
		pauseFlag = true;
	}
	
	// Append two .wav files and store them in a separate file
	
	private static void appendWavFiles(File fileOne, File fileTwo, boolean deleteFileOneFlag)
	{
		try
		{	
			AudioInputStream streamOne = AudioSystem.getAudioInputStream(fileOne);
			AudioInputStream streamTwo = AudioSystem.getAudioInputStream(fileTwo);
			
			AudioInputStream appendedStreams = new AudioInputStream(
													new SequenceInputStream(streamOne, streamTwo),
													streamOne.getFormat(),
													streamOne.getFrameLength() + streamTwo.getFrameLength());
			
			String fileNameWithSessionIDCopy = date.concat("_Session" + sessionID);
			copyFile = new File("C:/temp/" + fileNameWithSessionIDCopy + "_Copy.wav");
			
			AudioSystem.write(appendedStreams, AudioFileFormat.Type.WAVE, copyFile);
		
			streamOne.close();
			streamTwo.close();
			appendedStreams.close();
			
			System.gc(); // lol
			
			Files.copy(copyFile.toPath(), appendedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.delete(copyFile.toPath());
			
			try 
			{
				if (deleteFileOneFlag == true)
					{
						Files.delete(fileOne.toPath());
					}
				
				Files.delete(fileTwo.toPath());
				
			} catch (IOException e)
			{
				//e.printStackTrace();
			}
		} catch (Exception e)
		{
			System.out.println("An error occurred while appending files");
			e.printStackTrace();
		}
	}
	
	// Function to generate unique session ID every time the system is run 
	
	private static void generateSessionID()
	{
		// Query root directory for all files time-stamped with todays date
		// Store the result in an array of file references
		
		File[] matches = dir.listFiles(new FilenameFilter()
		{
		  public boolean accept(File dir, String name)
		  {
			  // Since we are not interested in sessions from previous dates
			  // We can safely ignore these and only search for today's sessions
			  // So we return all WAVE files stamped with todays date 
					  
		     return name.startsWith(date) && name.endsWith(".wav");
		  }
		});
		
		// Instantiate ArrayList to store information on Session IDs currently in use for this date

		ArrayList<Integer> sessionIDsInUse = new ArrayList<Integer>();
		
		// Extract session IDs from matching audio files
		
		for (int i = 0; i < matches.length; i++)
		{
			// Extract filepath from matches
			
			String name = matches[i].toString();
			
			// Extract session ID from filepath and convert it to integer
			
			int extractedSessionID;
			
			try
			{
				extractedSessionID = Integer.parseInt(name.substring(26, name.length() - 4));
				
				// Add it to list of session IDs already being used
				
				sessionIDsInUse.add(extractedSessionID);
			
			} catch (NumberFormatException e)
			{
				// Do nothing...
			}
		}
		
		// If there are already files from today, find next available
		// session ID, else do nothing
		
		if (sessionIDsInUse.isEmpty() == false)
		{
			// Find maximum session ID in use
			
			int maxIDInUse = Collections.max(sessionIDsInUse);
			
			// Set the session number equal to the next highest available session ID
			
			sessionID = maxIDInUse + 1;
		}
	}
	
	// Function to select session ID number manually
	
	private static void manuallyEditSessionID()
	{
		Scanner input = new Scanner(System.in);
		
		System.out.println("Enter new Session ID : ");
		String s = input.next();
		
		try
		{
			sessionID = Integer.parseInt(s);
		} catch (NumberFormatException e)
		{
			System.out.println("Illegal session ID format - please enter a number");
			e.printStackTrace();
		} finally
		{
			input.close();
		}
	}
	
	private static void scheduleRecordingEnd()
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
	}
	
	private static void createNewOutputFile()
	{	
		fileNameWithSessionID = date.concat("_Session" + sessionID + ".wav");
		appendedFile = new File("C:/temp/" + fileNameWithSessionID);
		System.out.println("Created output file with name: " + fileNameWithSessionID);
	}
}
