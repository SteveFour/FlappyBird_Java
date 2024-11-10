import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import java.io.IOException;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
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

	// Biến có "def_" lưu các biến mặc định, giá trị được áp dụng khi khởi tạo game mới
	// Biến ko "def_" lưu trữ giá trị tạm thời của game

	ArrayList<Pipe> pipesList;
	Bird bird;
	Bonus bonus = null;

	Timer gameLoop;
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
	int defBirdX = frameHeight / 8;
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

	// Biến mặc định khởi tạo trọng lực và vận tốc tức thời
	double defBirdVelocityY = -10;
	double birdFlapForce = defBirdVelocityY;
	double defPipeVelocityX = -4;
	double defGravity = 1;
	double gravity = defGravity;
	double birdVelocityY = defBirdVelocityY;
	double pipeVelocityX = defPipeVelocityX;

	// Biến lưu điểm và trạng thái game
	double score = 0;
	double difficultyScale = 1.01;
	boolean gameOver = false;
	boolean delayGameOver = false;

	// Biến lưu index của các ảnh/sprite
	int backgroundIndex = 0;
	int pipeTopIndex = 0;
	int pipeBottomIndex = 0;
	int birdColorIndex = 0;
	int birdAnimationIndex = 0;

	Image[] backgroundImages = new Image[] {
			new ImageIcon("imgs/bg_day.png").getImage(),
			new ImageIcon("imgs/bg_night.png").getImage()
	};
	Image[] pipeTopImages = new Image[] {
			new ImageIcon("imgs/pipe_top_day.png").getImage(),
			new ImageIcon("imgs/pipe_top_night.png").getImage()
	};
	Image[] pipeBottomImages = new Image[] {
			new ImageIcon("imgs/pipe_bottom_day.png").getImage(),
			new ImageIcon("imgs/pipe_bottom_night.png").getImage()

	};
	Image[][] birdImages = new Image[][] {
			{
					new ImageIcon("imgs/bird_one_1.png").getImage(),
					new ImageIcon("imgs/bird_one_2.png").getImage(),
					new ImageIcon("imgs/bird_one_3.png").getImage(),
					new ImageIcon("imgs/bird_one_2.png").getImage(),
			},
			{
					new ImageIcon("imgs/bird_two_1.png").getImage(),
					new ImageIcon("imgs/bird_two_2.png").getImage(),
					new ImageIcon("imgs/bird_two_3.png").getImage(),
					new ImageIcon("imgs/bird_two_2.png").getImage(),
			},
			{
					new ImageIcon("imgs/bird_three_1.png").getImage(),
					new ImageIcon("imgs/bird_three_2.png").getImage(),
					new ImageIcon("imgs/bird_three_3.png").getImage(),
					new ImageIcon("imgs/bird_three_2.png").getImage(),
			},
	};
	Image bonusScoreImage = new ImageIcon("imgs/bonus_score.png").getImage();

	// ------------------------------

	FlappyBird() {
		// Set kích cỡ frame và focus event bàn phím
		setPreferredSize(new Dimension(frameWidth, frameHeight));
		setFocusable(true);
		addKeyListener(this);

		// Tạo bird, gán animation = Timer animateBird, loop qua các ảnh của bird
		bird = new Bird(defBirdX, defBirdY, birdImages[birdColorIndex][birdAnimationIndex]);
		animateBird = new Timer(defAnimateBirdDelay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				bird.img = birdImages[birdColorIndex][birdAnimationIndex];
				birdAnimationIndex = (birdAnimationIndex + 1) % 4;
			}
		});
		animateBird.start();

		// Tạo ống, thêm ống vào danh sách các ống, và tăng độ khó game theo các ống
		pipesList = new ArrayList<Pipe>();
		placePipesTimer = new Timer(defPlacePipesDelay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				increaseDifficult();
				placePipes();
			}
		});
		placePipesTimer.start();

		// Timer để vẽ và cập nhật khung hình
		gameLoop = new Timer(defGameLoopDelay, this);
		gameLoop.start();
	}

	// -----------------------------

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
			if (x < 0.4) {
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
		if (gravity <= 2) {
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
		draw(g);
	}

	// Chuyển đổi vận tốc Oy của Bird -> Góc nghiêng của Bird
	public double convertGravityToAngle(double birdVelocityY2) {
		double angle = birdVelocityY2 * 3;

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
			g2d.drawString("Game over. Score: " + String.valueOf((int) score), 10, 35);
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

	// Cập nhật các sự kiện game mỗi khung hình
	public void move() {
		// Cập nhật toạ độ của Bird
		birdVelocityY += gravity;
		bird.y += birdVelocityY;
		bird.y = Math.max(bird.y, 0);

		// Cập nhật toạ độ từng ống
		Iterator<Pipe> iterator = pipesList.iterator();
		while (iterator.hasNext()) {
			Pipe pipe = iterator.next();
			pipe.x += pipeVelocityX;

			if (!pipe.passed && bird.x > pipe.x + defPipeWidth) {
				playSound("./audios/sfx_point.wav");
				pipe.passed = true;
				score += 0.5;
			}

			if (collision(bird, pipe)) {
				gameOver = true;
			}

			// Tối ưu: Giải phóng ống nếu như ống đi ngoài màn hình
			if (pipe.x + defPipeWidth < 0) {
				iterator.remove();
			}
		}

		// Cập nhật toạ độ cho ống
		if (bonus != null) {
			bonus.x += pipeVelocityX;

			if (collisionBonus(bird, bonus)) {
				score += bonusScoreValue;
				playSound("./audios/sfx_powerup_bonus.wav");
				bonus = null;
				for (int count = 1; count <= bonusScoreValue; count++)
					increaseDifficult();
			}

			// Tối ưu: Giải phóng powerup nếu như powerup đi ngoài màn hình
			if (bonus != null && bonus.x + defBonusWidth < 0) {
				bonus = null;
			}
		}

		// Nếu rơi xuống thì gameover
		if (bird.y + defBirdHeight > frameHeight) {
			gameOver = true;
		}
	}

	// Va chạm Bird - ống
	public boolean collision(Bird a, Pipe b) {
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
		// Cập nhật sự kiện khi bấm phím SPACE
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (!gameOver) {
				birdVelocityY = birdFlapForce;
				playSound("/audios/sfx_wing.wav");
			}

			// Nếu đã gameOver & qua delayGameOver, phím SPACE để restart game
			// 1. Random theme và bird
			// 2. Reset các biến
			if (gameOver && !delayGameOver) {
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
				pipesList.clear();
				gameLoop.start();
				placePipesTimer.start();
				animateBird.start();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		move();
		repaint();

		if (gameOver) {
			// Nếu gameOver:
			// 1. delayGameOver để chặn điều khiển người dùng
			// 2. Ngừng di chuyển cho Pipe
			// 3. Timer delayTimer để chờ mở lại điều khiển người dùng
			if (gameOver && !delayGameOver) {
				delayGameOver = true;
				pipeVelocityX = 0;
				birdVelocityY = 0;
				playSound("/audios/sfx_impact.wav");
				playSound("/audios/sfx_die.wav");

				Timer delayTimer = new Timer(gameOverDelay, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent evt) {
						placePipesTimer.stop();
						animateBird.stop();
						gameLoop.stop();
						delayGameOver = false;
					}
				});
				delayTimer.setRepeats(false); // Đảm bảo Timer ko bị lặp lại
				delayTimer.start();
			}
		}
	}

}
