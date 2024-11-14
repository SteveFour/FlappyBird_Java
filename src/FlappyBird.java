import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class FlappyBird extends JPanel implements KeyListener {
	class Bird {
		int x;
		int y;
		Image img;

		Bird(int x, int y, Image img) {
			this.x = x;
			this.y = y;
			this.img = img;
		}
	}

	class Pipe {
		int x;
		int y;
		Image img;
		boolean passed = false;

		Pipe(int x, int y, Image img) {
			this.x = x;
			this.y = y;
			this.img = img;
		}
	}

	// Powerup - Bonus
	// Chức năng: Cộng thêm điểm + tăng độ khó tương ứng
	class Bonus {
		int x;
		int y;
		Image img;

		Bonus(int x, int y, Image img) {
			this.x = x;
			this.y = y;
			this.img = img;
		}
	}

	// ------------------------------

	// Biến có "def_" lưu các biến mặc định, giá trị được áp dụng khi khởi tạo game
	// mới
	// Biến ko "def_" lưu trữ giá trị tạm thời của game

	ArrayList<Pipe> pipesList;
	Bird bird;
	Bonus bonus = null;

	Timer placePipesTimer;
	Timer animateBird;

	// Các Timer mặc định
	int defGameLoopDelay = 1000 / 60;
	int defPlacePipesDelay = 1500;
	int defAnimateBirdDelay = 50;
	int placePipesDelay = defPlacePipesDelay;
	int gameOverDelay = 2000;

	// Khung mặc định
	int frameWidth = 432;
	int frameHeight = 768;

	// Biến mặc định khởi tạo bird
	int defBirdWidth = 51;
	int defBirdHeight = 36;
	int defBirdX = frameWidth / 4;
	int defBirdY = frameHeight / 2;

	// Biến mặc định khởi tạo ống
	int defPipeX = frameWidth;
	int defPipeY = 0;
	int defPipeWidth = 78;
	int defPipeHeight = 480;

	// Biến mặc định khởi tạo bonus powerup
	int defBonusWidth = 40;
	int defBonusHeight = 40;
	int bonusScoreValue = 5;
	double bonusLuckValue = 0.4;

	// Biến mặc định khởi tạo trọng lực và vận tốc tức thời
	double defPipeVelocityX = -120; // -4 pixels/frame * 60 frames/sec = -240 pixels/sec
	double defBirdVelocityY = -300;  // -10 pixels/frame * 60 frames/sec = -600 pixels/sec
	double defGravity = 1000;          // 1 pixel/frame^2 * 60 frames/sec = 60 pixels/sec^2

	double gravity = defGravity;
	double birdVelocityY = defBirdVelocityY;
	double birdFlapForce = defBirdVelocityY;
	double pipeVelocityX = defPipeVelocityX;

	// Biến lưu điểm và trạng thái game
	double score = 0;
	double difficultyScale = 1.01;
	boolean gameOver = false;
	boolean delayGameOver = false;
	double highScore;
	boolean readyNextGame = false;

	// Biến lưu index của các ảnh/sprite
	int backgroundIndex = 0;
	int pipeTopIndex = 0;
	int pipeBottomIndex = 0;
	int birdColorIndex = 0;
	int birdAnimationIndex = 0;

	// Chạy lần đầu
	boolean isIntro = true;
	Image introInstructionImage;

	Image[] backgroundImages = new Image[] {
			new ImageIcon(getClass().getResource("imgs/bg_day.png")).getImage(),
			new ImageIcon(getClass().getResource("imgs/bg_night.png")).getImage()
	};
	Image[] pipeTopImages = new Image[] {
			new ImageIcon(getClass().getResource("imgs/pipe_top_day.png")).getImage(),
			new ImageIcon(getClass().getResource("imgs/pipe_top_night.png")).getImage()
	};
	Image[] pipeBottomImages = new Image[] {
			new ImageIcon(getClass().getResource("imgs/pipe_bottom_day.png")).getImage(),
			new ImageIcon(getClass().getResource("imgs/pipe_bottom_night.png")).getImage()

	};
	Image[][] birdImages = new Image[][] {
			{
					new ImageIcon(getClass().getResource("imgs/bird_one_1.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_one_2.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_one_3.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_one_2.png")).getImage(),
			},
			{
					new ImageIcon(getClass().getResource("imgs/bird_two_1.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_two_2.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_two_3.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_two_2.png")).getImage(),
			},
			{
					new ImageIcon(getClass().getResource("imgs/bird_three_1.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_three_2.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_three_3.png")).getImage(),
					new ImageIcon(getClass().getResource("imgs/bird_three_2.png")).getImage(),
			},
	};
	Image bonusScoreImage = new ImageIcon(getClass().getResource("imgs/bonus_score.png")).getImage();
	Image resultOverlayImage = new ImageIcon(getClass().getResource("imgs/result_overlay_bg.png")).getImage();
	List<Pipe> pipesToRemove = new ArrayList<>();

	// ------------------------------

	FlappyBird() throws FileNotFoundException {
		Scanner sc = new Scanner(new File("highScore.txt"));
		highScore = sc.nextDouble();

		// Set frame size and focus
		setPreferredSize(new Dimension(frameWidth, frameHeight));
		setFocusable(true);
		addKeyListener(this);

		// Load the intro instruction image
		introInstructionImage = new ImageIcon(getClass().getResource("imgs/intro_instruction.png")).getImage();

		// Initialize bird
		bird = new Bird(defBirdX, defBirdY, birdImages[birdColorIndex][birdAnimationIndex]);

		// Set up timers but don't start them yet
		animateBird = new Timer(defAnimateBirdDelay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				bird.img = birdImages[birdColorIndex][birdAnimationIndex];
				birdAnimationIndex = (birdAnimationIndex + 1) % 4;
			}
		});

		pipesList = new ArrayList<Pipe>();
		placePipesTimer = new Timer(defPlacePipesDelay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				increaseDifficult();
				placePipes();
			}
		});

		// Start the game thread
		new Thread(() -> {
			long lastTime = System.nanoTime();
			while (true) {
				long currentTime = System.nanoTime();
				double deltaTime = (currentTime - lastTime) / 1_000_000_000.0; // In seconds
				lastTime = currentTime;

				update(deltaTime);
				repaint();

				try {
					Thread.sleep(16); // Approximate 60 FPS
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		sc.close();
	}

	// -----------------------------

	public void startGame() {
		animateBird.start();
		placePipesTimer.start();
	}

	// Hàm khởi tạo để có thể phát âm thanh
	public void playSound(String audioFile) {
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource(audioFile));
			Clip clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			clip.start();
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	// Hàm phục vụ cho quá trình đặt ống
	// 1. Đặt tạo độ ống (trên & dưới)
	// 2. Đặt bonus powerup nếu random cho phép
	public void placePipes() {
		int randomPipeY = (int) (defPipeY - defPipeHeight / 4 - Math.random() * (defPipeHeight / 2));
		int openingSpace = frameHeight / 4;

		int topPipeY = randomPipeY;
		int bottomPipeY = randomPipeY + defPipeHeight + openingSpace;

		Image topPipeImage = pipeTopImages[pipeTopIndex];
		Image bottomPipeImage = pipeBottomImages[pipeBottomIndex];

		Pipe topPipe = new Pipe(defPipeX, topPipeY, topPipeImage);
		pipesList.add(topPipe);

		Pipe bottomPipe = new Pipe(defPipeX, bottomPipeY, bottomPipeImage);
		pipesList.add(bottomPipe);

		if (bonus == null) {
			Random random = new Random();
			double x = random.nextDouble();
			if (x < bonusLuckValue) {
				int bonusY = topPipe.y + defPipeHeight + openingSpace / 2 - defBonusHeight / 2;
				int bonusX = defPipeX + defPipeWidth / 2 - defBonusWidth / 2;
				bonus = new Bonus(bonusX, bonusY, bonusScoreImage);
			}
		}
	}

	// Hàm phục vụ cho tăng độ khó
	// 1. Tăng lực bay của chim (có giới hạn)
	// 2. Tăng trọng lực
	// 3. Tăng vận tốc ống
	// 4. Giảm delay đặt các ống
	public void increaseDifficult() {
		if (gravity <= 2000) {
			birdFlapForce *= difficultyScale;
		}
		gravity *= difficultyScale;
		pipeVelocityX *= difficultyScale;
		placePipesDelay /= difficultyScale;
		placePipesTimer.setDelay(placePipesDelay); // Phải có setDelay để cập nhật Timer
	}

	// Gọi hàm repaint() -> Yêu cầu Swing gọi hàm paintComponent()
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (isIntro) {
			// Draw background
			g.drawImage(backgroundImages[backgroundIndex], 0, 0, frameWidth, frameHeight, null);

			// Draw bird at default position
			g.drawImage(bird.img, bird.x, bird.y, defBirdWidth, defBirdHeight, null);

			// Draw intro instruction image at center
			int introWidth = introInstructionImage.getWidth(null) * 3;
			int introHeight = introInstructionImage.getHeight(null) * 3;
			int introX = frameWidth / 4 - introWidth / 2 + 25;
			int introY = frameHeight / 2 - introHeight / 2 + 90;
			
			g.drawImage(introInstructionImage, introX, introY, introWidth, introHeight, null);
		} else {
			draw(g);
		}
	}

	// Chuyển đổi vận tốc Oy của Bird -> Góc nghiêng của Bird
	public double convertGravityToAngle(double birdVelocityY2) {
		double angle = birdVelocityY2 / 10;

		if (angle < -60)
			angle = -60;
		else if (angle > 60)
			angle = 60;
		return angle;
	}

	// Vẽ các ảnh trong game theo yêu cầu
	public void draw(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		Image backgroundImg = backgroundImages[backgroundIndex];

		g2d.drawImage(backgroundImg, 0, 0, frameWidth, frameHeight, null);

		for (int pos = 0; pos < pipesList.size(); pos++) {
			Pipe pipe = pipesList.get(pos);
			g2d.drawImage(pipe.img, pipe.x, pipe.y, defPipeWidth, defPipeHeight, null);
		}

		if (bonus != null) {
			g2d.drawImage(bonus.img, bonus.x, bonus.y, defBonusWidth, defBonusHeight, null);
		}

		g2d.setColor(Color.black);
		g2d.setFont(new Font("Arial", Font.PLAIN, 32));
		if (gameOver) {
			if (score > highScore) {
				highScore = score;
			}
			
			int overlayWidth = 113 * 3;
			int overlayHeight = 57 * 3;
			g2d.drawImage(resultOverlayImage, frameWidth / 2 - overlayWidth / 2, frameHeight / 2 - overlayHeight / 2 - 40, overlayWidth, overlayHeight, null);

			g2d.drawString("GAME OVER!", 75, 305);
			g2d.drawString("Your score: " + String.valueOf((int) score), 75, 355);
			g2d.drawString("Highest score: " + String.valueOf((int) highScore), 75, 405);
		} else {
			g2d.drawString("Score: " + (int) score, 10, 35);
		}

		// Để nghiêng đc sprite/ảnh của Bird, cần xác định:
		// 1. Tâm điểm của Bird (centerX, centerY)
		// 2. Góc nghiêng (theo radian)
		double centerX = bird.x + defBirdWidth / 2.0;
		double centerY = bird.y + defBirdHeight / 2.0;
		double rotationDegrees = convertGravityToAngle(birdVelocityY);
		double rotationRadians = Math.toRadians(rotationDegrees);

		g2d.rotate(rotationRadians, centerX, centerY);
		g2d.drawImage(bird.img, bird.x, bird.y, defBirdWidth, defBirdHeight, null);
		g2d.dispose();
	}

	// Va chạm Bird - ống
	public boolean collision(Bird a, Pipe b) {
		if (gameOver)
			return false;
		// int noHeadBumb = 1;
		return a.x + 8 < b.x + defPipeWidth &&
				a.x + defBirdWidth - 8 > b.x &&
				a.y + 4 < b.y + defPipeHeight &&
				a.y + defBirdHeight - 4 > b.y; // Basically how collision works
	}

	// Va chạm Bird - powerup
	public boolean collisionBonus(Bird a, Bonus b) {
		return a.x < b.x + defBonusWidth &&
				a.x + defBirdWidth > b.x &&
				a.y < b.y + defBonusHeight &&
				a.y + defBirdHeight > b.y;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// Handle space key press
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (isIntro) {
				isIntro = false;
				startGame();
			} else if (!gameOver) {
				birdVelocityY = birdFlapForce;
				playSound("/audios/sfx_wing.wav");
			} else if (gameOver && readyNextGame) {
				// Existing game reset code
				Random random = new Random();
				birdColorIndex = random.nextInt(birdImages.length);
				backgroundIndex = random.nextInt(backgroundImages.length);
				pipeTopIndex = backgroundIndex;
				pipeBottomIndex = backgroundIndex;

				bonus = null;
				gravity = defGravity;
				bird.y = defBirdY;
				score = 0;
				birdVelocityY = defBirdVelocityY;
				birdFlapForce = defBirdVelocityY;
				pipeVelocityX = defPipeVelocityX;
				placePipesDelay = defPlacePipesDelay;
				placePipesTimer.setDelay(placePipesDelay);
				gameOver = false;
				readyNextGame = false;
				pipesList.clear();

				startGame();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	// Add update method to replace actionPerformed
	public void update(double deltaTime) {
		// Update bird position
		birdVelocityY += gravity * deltaTime;
		bird.y += birdVelocityY * deltaTime;
		bird.y = Math.max(bird.y, 0);

		// Update pipes position
		synchronized (pipesList) {
			Iterator<Pipe> iterator = pipesList.iterator();
			while (iterator.hasNext()) {
				Pipe pipe = iterator.next();
				pipe.x += pipeVelocityX * deltaTime;
				if (!pipe.passed && bird.x > pipe.x + defPipeWidth) {
					playSound("./audios/sfx_point.wav");
					pipe.passed = true;
					score += 0.5;
				}
				if (collision(bird, pipe)) {
					gameOver = true;
				}
				if (pipe.x + defPipeWidth < 0) {
					iterator.remove();
				}
			}
		}

		// Update bonus position
		if (bonus != null) {
			bonus.x += pipeVelocityX * deltaTime;

			if (collisionBonus(bird, bonus)) {
				score += bonusScoreValue;
				playSound("./audios/sfx_powerup_bonus.wav");
				bonus = null;
				for (int count = 1; count <= bonusScoreValue; count++)
					increaseDifficult();
			}

			// Remove bonus if it moves off-screen
			if (bonus != null && bonus.x + defBonusWidth < 0) {
				bonus = null;
			}
		}

		// Check for game over conditions
		if (bird.y + defBirdHeight > frameHeight) {
			gameOver = true;
		}

		// Handle game over logic
		if (gameOver) {
			if (!delayGameOver) {
				delayGameOver = true;
				pipeVelocityX = 0;
				birdVelocityY = 0;
				playSound("/audios/sfx_impact.wav");
				playSound("/audios/sfx_die.wav");

				if (score > highScore) {
					highScore = score;
					try (PrintWriter out = new PrintWriter("highScore.txt")) {
						out.println(highScore);
					} catch (FileNotFoundException ex) {
						ex.printStackTrace();
					}
				}

				Timer delayTimer = new Timer(gameOverDelay, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent evt) {
						placePipesTimer.stop();
						animateBird.stop();
						delayGameOver = false;
						readyNextGame = true;
					}
				});
				delayTimer.setRepeats(false); // Ensure the Timer doesn't repeat
				delayTimer.start();
			}
		}
	}

}
