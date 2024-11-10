import javax.swing.JFrame;
import java.net.URL;
import javax.swing.ImageIcon;

// Nếu VSCode Java có vấn đề với Code Runner, vào settings.json và chỉnh execute code của Code Runner cho Java
// Trong "code-runner.executorMap": { ... }, chỉnh của Java thành:
// "java": "cd $dir && javac -encoding ISO-8859-1 $fileName && java $fileNameWithoutExt",


public class App {
	public static void main(String[] args) throws Exception {
		URL url = ClassLoader.getSystemResource("./imgs/bird_one_1.png");
		JFrame frame = new JFrame("Flappy Bird");
		frame.setIconImage(new ImageIcon(url).getImage());
		frame.setSize(432, 768);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		FlappyBird flappyBird = new FlappyBird();
		frame.add(flappyBird);
		frame.pack();
		frame.setVisible(true);
	}
}
