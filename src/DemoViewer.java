import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class DemoViewer extends JPanel {
	
	private double heading = 0;
	private double pitch = 0;
	private double scale = 1.0;
	private double offsetX = 0;
	private double offsetY = 0;
	
	private boolean dragging = false;
	private boolean moving = false;
	private int prevMouseX, prevMouseY;
	
	private List<Triangle> tris;
	
	public DemoViewer() {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Введите название файла модели (например, model.txt): ");
		String fileName = scanner.nextLine();
		
		tris = loadTrianglesFromFile(fileName);
		if (tris == null) {
			tris = getDefaultFigure();
			System.out.println("Файл " + fileName + " не найден — используется стандартная фигура");
		}
		
		setupMouseControls();
	}
	
	public void show() {
		JFrame frame = new JFrame("3D Viewer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(this, BorderLayout.CENTER);
		frame.setSize(600, 600);
		frame.setVisible(true);
	}
	
	private void setupMouseControls() {
		addMouseWheelListener(e -> {
			if (e.getPreciseWheelRotation() < 0) scale *= 1.1;
			else scale /= 1.1;
			repaint();
		});
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) dragging = true;
				if (SwingUtilities.isRightMouseButton(e)) moving = true;
				prevMouseX = e.getX();
				prevMouseY = e.getY();
			}
			
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) dragging = false;
				if (SwingUtilities.isRightMouseButton(e)) moving = false;
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				int dx = e.getX() - prevMouseX;
				int dy = e.getY() - prevMouseY;
				
				if (dragging) {
					heading += dx * 0.01;
					pitch -= dy * 0.01;
					pitch = Math.max(-Math.PI / 2 + 0.01, Math.min(Math.PI / 2 - 0.01, pitch));
				} else if (moving) {
					offsetX += dx;
					offsetY += dy;
				}
				
				prevMouseX = e.getX();
				prevMouseY = e.getY();
				repaint();
			}
		});
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = (Graphics2D) g;
		
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		Matrix3 headingTransform = new Matrix3(new double[]{
				Math.cos(heading), 0, -Math.sin(heading),
				0, 1, 0,
				Math.sin(heading), 0, Math.cos(heading)
		});
		
		Matrix3 pitchTransform = new Matrix3(new double[]{
				1, 0, 0,
				0, Math.cos(pitch), Math.sin(pitch),
				0, -Math.sin(pitch), Math.cos(pitch)
		});
		
		Matrix3 transform = headingTransform.multiply(pitchTransform);
		double[] zBuffer = new double[img.getWidth() * img.getHeight()];
		Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);
		
		for (Triangle t : tris) {
			Vertex[] verts = { t.v1, t.v2, t.v3 };
			Vertex[] proj = new Vertex[3];
			
			for (int i = 0; i < 3; i++) {
				Vertex v = transform.transform(verts[i]);
				v.x = v.x * scale + getWidth() / 2 + offsetX;
				v.y = v.y * scale + getHeight() / 2 + offsetY;
				v.z = v.z * scale;
				proj[i] = v;
			}
			
			Vertex ab = proj[1].subtract(proj[0]);
			Vertex ac = proj[2].subtract(proj[0]);
			Vertex norm = ab.cross(ac).normalize();
			double angleCos = Math.abs(norm.z);
			
			fillTriangle(img, zBuffer, proj[0], proj[1], proj[2], getShade(t.color, angleCos));
		}
		
		g2.drawImage(img, 0, 0, null);
	}
	
	private void fillTriangle(BufferedImage img, double[] zBuffer, Vertex v1, Vertex v2, Vertex v3, Color color) {
		int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
		int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
		int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
		int maxY = (int) Math.min(img.getHeight() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));
		
		double area = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);
		
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / area;
				double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / area;
				double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / area;
				if (b1 >= 0 && b2 >= 0 && b3 >= 0) {
					double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
					int zIndex = y * img.getWidth() + x;
					if (zBuffer[zIndex] < depth) {
						zBuffer[zIndex] = depth;
						img.setRGB(x, y, color.getRGB());
					}
				}
			}
		}
	}
	
	private Color getShade(Color c, double shade) {
		double r = Math.pow(c.getRed(), 2.4) * shade;
		double g = Math.pow(c.getGreen(), 2.4) * shade;
		double b = Math.pow(c.getBlue(), 2.4) * shade;
		int red = (int) Math.pow(r, 1 / 2.4);
		int green = (int) Math.pow(g, 1 / 2.4);
		int blue = (int) Math.pow(b, 1 / 2.4);
		return new Color(clamp(red), clamp(green), clamp(blue));
	}
	
	private int clamp(int value) {
		return Math.max(0, Math.min(255, value));
	}
	
	public static List<Triangle> loadTrianglesFromFile(String filename) {
		List<Triangle> result = new ArrayList<>();
		try (Scanner scanner = new Scanner(new File(filename))) {
			while (scanner.hasNextLine()) {
				String[] parts = scanner.nextLine().trim().split("\\s+");
				if (parts.length != 12) continue;
				
				double[] coords = Arrays.stream(parts).mapToDouble(Double::parseDouble).toArray();
				Vertex v1 = new Vertex(coords[0], coords[1], coords[2]);
				Vertex v2 = new Vertex(coords[3], coords[4], coords[5]);
				Vertex v3 = new Vertex(coords[6], coords[7], coords[8]);
				Color color = new Color((int) coords[9], (int) coords[10], (int) coords[11]);
				result.add(new Triangle(v1, v2, v3, color));
			}
		} catch (Exception e) {
			return null;
		}
		return result;
	}
	
	public static List<Triangle> getDefaultFigure() {
		List<Triangle> tris = new ArrayList<>();
		tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(-100, 100, -100), Color.WHITE));
		tris.add(new Triangle(new Vertex(100, 100, 100), new Vertex(-100, -100, 100), new Vertex(100, -100, -100), Color.RED));
		tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(100, 100, 100), Color.GREEN));
		tris.add(new Triangle(new Vertex(-100, 100, -100), new Vertex(100, -100, -100), new Vertex(-100, -100, 100), Color.BLUE));
		
		for (int i = 0; i < 4; i++) {
			tris = inflate(tris);
		}
		return tris;
	}
	
	public static List<Triangle> inflate(List<Triangle> tris) {
		List<Triangle> result = new ArrayList<>();
		for (Triangle t : tris) {
			Vertex m1 = t.v1.midpoint(t.v2);
			Vertex m2 = t.v2.midpoint(t.v3);
			Vertex m3 = t.v1.midpoint(t.v3);
			result.add(new Triangle(t.v1, m1, m3, t.color));
			result.add(new Triangle(t.v2, m1, m2, t.color));
			result.add(new Triangle(t.v3, m2, m3, t.color));
			result.add(new Triangle(m1, m2, m3, t.color));
		}
		for (Triangle t : result) {
			for (Vertex v : new Vertex[]{t.v1, t.v2, t.v3}) {
				double len = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / Math.sqrt(30000);
				v.x /= len;
				v.y /= len;
				v.z /= len;
			}
		}
		return result;
	}
}
