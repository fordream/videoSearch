import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollBar;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.MouseMotionAdapter;

public class frmMainPage extends JFrame {

	private JPanel contentPane;
	private int resultVideoFrameCount = 0, queryVideoFrameCount = 0,
			resultAudioFrameCount = 0, queryAudioFrameCount = 0;
	private JPanel panelResultVideo, panelQueryVideo, panelWaveform;
	private Timer queryTimer = new Timer(), resultTimer = new Timer();
	private JScrollBar scrollBar;
	private String resultFileName, queryFileName;
	private Clip clipResultVideo, clipQueryVideo;
	private JButton btnPlayQueryVideo, btnPauseQueryVideo, btnStopQueryVideo;
	private JButton btnPlayResultVideo, btnPauseResultVideo,
			btnStopResultVideo;
	private JList<String> listMatchedList;
	private ArrayList<Integer> startFrames = new ArrayList<Integer>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frmMainPage frame = new frmMainPage();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public frmMainPage() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 599, 759);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JButton btnOpenFile = new JButton("Open File");
		btnOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File selectedFile;
				JFileChooser chooser = new JFileChooser(".");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int ret = chooser.showOpenDialog(null);
				if (ret == JFileChooser.APPROVE_OPTION) {
					selectedFile = chooser.getSelectedFile();
					queryFileName = selectedFile.getName();
					stopVideo(panelQueryVideo);
					playVideo(queryFileName, panelQueryVideo);
					
					BufferedReader br;
					DefaultListModel<String> matchedListModel = new DefaultListModel<String>();
					try {
						br = new BufferedReader(new FileReader(queryFileName + "_score.txt"));
						String line = br.readLine();

						startFrames.clear();
						while (line != null) {
							String[] args = line.split(",");
							matchedListModel.addElement(args[0] + ": " + args[1] + "%");
							startFrames.add(Integer.parseInt(args[2]));
							line = br.readLine();
						}
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} 
					listMatchedList.setModel(matchedListModel);
				}
			}
		});
		btnOpenFile.setBounds(20, 20, 157, 34);
		contentPane.add(btnOpenFile);

		JLabel lblQueryFileName = new JLabel("");
		lblQueryFileName.setBounds(20, 66, 194, 22);
		contentPane.add(lblQueryFileName);

		JLabel lblMatched = new JLabel("Matched List");
		lblMatched.setBounds(15, 398, 162, 16);
		contentPane.add(lblMatched);

		listMatchedList = new JList<String>();
		listMatchedList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String[] splits = ((String) listMatchedList.getSelectedValue()).split(":");
				String selectedDirName = splits[0];
				float percent = Float.parseFloat(splits[1].substring(0, splits[1].length() - 1));
				int startFrame = startFrames.get(listMatchedList.getSelectedIndex());
				Graphics g = panelWaveform.getGraphics();
				g.clearRect(0, 0, panelWaveform.getWidth(), panelWaveform.getHeight());
				g.setColor(Color.RED);
				int height = (int) (panelWaveform.getHeight() * percent / 100);
				g.fillRect(startFrame * panelWaveform.getWidth() / 600, panelWaveform.getHeight() - height, 88, height);
				resultFileName = selectedDirName;
				
				stopVideo(panelResultVideo);
				playVideo(selectedDirName, panelResultVideo);
			}
		});
		listMatchedList.setBounds(15, 425, 162, 123);
		contentPane.add(listMatchedList);

		panelQueryVideo = new JPanel();
		panelQueryVideo.setBounds(221, 20, 352, 288);
		contentPane.add(panelQueryVideo);

		panelResultVideo = new JPanel();
		panelResultVideo.setBounds(221, 398, 352, 288);
		contentPane.add(panelResultVideo);

		scrollBar = new JScrollBar();
		scrollBar.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				resultVideoFrameCount = scrollBar.getValue();
				resultAudioFrameCount = (int) (((double) resultVideoFrameCount) / 600 * clipResultVideo.getFrameLength());
				if (clipResultVideo != null)
					clipResultVideo.setFramePosition(resultAudioFrameCount);
			}
		});
		scrollBar.setMinimum(1);
		scrollBar.setMaximum(600);
		scrollBar.setOrientation(JScrollBar.HORIZONTAL);
		scrollBar.setBounds(221, 372, 352, 16);
		contentPane.add(scrollBar);

		panelWaveform = new JPanel();
		panelWaveform.setBounds(221, 338, 352, 34);
		contentPane.add(panelWaveform);

		btnPlayQueryVideo = new JButton("Play");
		btnPlayQueryVideo.setEnabled(false);
		btnPlayQueryVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playVideo(queryFileName, panelQueryVideo);
			}
		});
		btnPlayQueryVideo.setBounds(20, 286, 50, 22);
		contentPane.add(btnPlayQueryVideo);

		btnPauseQueryVideo = new JButton("Pause");
		btnPauseQueryVideo.setEnabled(false);
		btnPauseQueryVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopVideo(panelQueryVideo, true);
			}
		});
		btnPauseQueryVideo.setBounds(76, 286, 50, 22);
		contentPane.add(btnPauseQueryVideo);

		btnStopQueryVideo = new JButton("Stop");
		btnStopQueryVideo.setEnabled(false);
		btnStopQueryVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopVideo(panelQueryVideo);
			}
		});
		btnStopQueryVideo.setBounds(132, 286, 50, 22);
		contentPane.add(btnStopQueryVideo);

		btnPlayResultVideo = new JButton("Play");
		btnPlayResultVideo.setEnabled(false);
		btnPlayResultVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playVideo(resultFileName, panelResultVideo);
			}
		});
		btnPlayResultVideo.setBounds(20, 664, 50, 22);
		contentPane.add(btnPlayResultVideo);

		btnPauseResultVideo = new JButton("Pause");
		btnPauseResultVideo.setEnabled(false);
		btnPauseResultVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopVideo(panelResultVideo, true);
			}
		});
		btnPauseResultVideo.setBounds(76, 664, 50, 22);
		contentPane.add(btnPauseResultVideo);

		btnStopResultVideo = new JButton("Stop");
		btnStopResultVideo.setEnabled(false);
		btnStopResultVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopVideo(panelResultVideo);
			}
		});
		btnStopResultVideo.setBounds(132, 664, 50, 22);
		contentPane.add(btnStopResultVideo);

//		String path = "database";
//
//		String files;
//		File folder = new File(path);
//		File[] listOfFiles = folder.listFiles();
//		DefaultListModel<String> matchedListModel = new DefaultListModel<String>();
//		for (int i = 0; i < listOfFiles.length; i++) {
//			if (listOfFiles[i].isDirectory()) {
//				files = listOfFiles[i].getName();
//				matchedListModel.addElement(files);
//			}
//		}
//		listMatchedList.setModel(matchedListModel);
	}

	public void playVideo(final String dirName, final JPanel targetPanel) {
		Timer timer;
		String audioFileName;
		Clip audioClip = null;
		int audioFramePos;
		if (targetPanel == panelResultVideo) {
			resultTimer = new Timer();
			timer = resultTimer;
			audioFileName = "database/" + dirName + "/" + dirName + ".wav";
			audioFramePos = resultAudioFrameCount;
			File audioFile = new File(audioFileName);
			try {
				AudioInputStream sound = AudioSystem
						.getAudioInputStream(audioFile);
				clipResultVideo = AudioSystem.getClip();
				audioClip = clipResultVideo;
				audioClip.open(sound);
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			btnPlayResultVideo.setEnabled(false);
			btnPauseResultVideo.setEnabled(true);
			btnStopResultVideo.setEnabled(true);
		} else {
			queryTimer = new Timer();
			timer = queryTimer;
			audioFileName = "query/" + dirName + "/" + dirName + ".wav";
			audioFramePos = queryAudioFrameCount;
			File audioFile = new File(audioFileName);
			try {
				AudioInputStream sound = AudioSystem
						.getAudioInputStream(audioFile);
				clipQueryVideo = AudioSystem.getClip();
				audioClip = clipQueryVideo;
				audioClip.open(sound);
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			btnPlayQueryVideo.setEnabled(false);
			btnPauseQueryVideo.setEnabled(true);
			btnStopQueryVideo.setEnabled(true);
		}

		audioClip.setFramePosition(audioFramePos);
		audioClip.loop(Clip.LOOP_CONTINUOUSLY);

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				String baseDirName;
				int frameCount;
				if (targetPanel == panelResultVideo) {
					if (resultVideoFrameCount == 600)
						resultVideoFrameCount = 0;
					resultVideoFrameCount++;
					scrollBar.setValue(resultVideoFrameCount);
					baseDirName = "database";
					frameCount = resultVideoFrameCount;
				} else {
					if (queryVideoFrameCount == 150)
						queryVideoFrameCount = 0;
					queryVideoFrameCount++;
					baseDirName = "query";
					frameCount = queryVideoFrameCount;
				}

				String fileName = baseDirName + "/" + dirName + "/" + dirName
						+ String.format("%3d", frameCount).replace(' ', '0')
						+ ".rgb";
				int width = 352;
				int height = 288;

				BufferedImage image_original = new BufferedImage(width, height,
						BufferedImage.TYPE_INT_RGB);

				try {
					File file = new File(fileName);
					InputStream is = new FileInputStream(file);

					long len = file.length();
					byte[] bytes = new byte[(int) len];

					int offset = 0;
					int numRead = 0;
					while (offset < bytes.length
							&& (numRead = is.read(bytes, offset, bytes.length
									- offset)) >= 0) {
						offset += numRead;
					}

					int ind = 0;
					for (int iy = 0; iy < height; iy++) {

						for (int ix = 0; ix < width; ix++) {

							byte a = 0;
							byte r = bytes[ind];
							byte g = bytes[ind + height * width];
							byte b = bytes[ind + height * width * 2];
							int pix = 0xff000000 | ((r & 0xff) << 16)
									| ((g & 0xff) << 8) | (b & 0xff);
							image_original.setRGB(ix, iy, pix);
							ind++;
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				targetPanel.getGraphics().drawImage(image_original, 0, 0, null);
			}
		}, 0, (long) 33.33);
	}

	private void stopVideo(final JPanel targetPanel) {
		stopVideo(targetPanel, false);
	}

	private void stopVideo(final JPanel targetPanel, boolean isPause) {
		if (targetPanel == panelResultVideo) {
			resultTimer.cancel();
			resultTimer.purge();
			if (isPause) {
				resultAudioFrameCount = clipResultVideo.getFramePosition();
				btnPauseResultVideo.setEnabled(false);
				btnPlayResultVideo.setEnabled(true);
				btnStopResultVideo.setEnabled(true);
			} else {
				resultVideoFrameCount = 0;
				resultAudioFrameCount = 0;
				btnStopResultVideo.setEnabled(false);
				btnPauseResultVideo.setEnabled(false);
				btnPlayResultVideo.setEnabled(true);
			}
			if (clipResultVideo != null)
				clipResultVideo.stop();
		} else {
			queryTimer.cancel();
			queryTimer.purge();
			if (isPause) {
				queryAudioFrameCount = clipQueryVideo.getFramePosition();
				btnPauseQueryVideo.setEnabled(false);
				btnPlayQueryVideo.setEnabled(true);
				btnStopQueryVideo.setEnabled(true);
			} else {
				queryVideoFrameCount = 0;
				queryAudioFrameCount = 0;
				btnStopQueryVideo.setEnabled(false);
				btnPauseQueryVideo.setEnabled(false);
				btnPlayQueryVideo.setEnabled(true);
			}
			if (clipQueryVideo != null)
				clipQueryVideo.stop();
		}
	}
}
