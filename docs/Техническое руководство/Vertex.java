public class Vertex {
	public double x, y, z;
	
	public Vertex(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vertex subtract(Vertex v) {
		return new Vertex(x - v.x, y - v.y, z - v.z);
	}
	
	public Vertex cross(Vertex v) {
		return new Vertex(
				y * v.z - z * v.y,
				z * v.x - x * v.z,
				x * v.y - y * v.x
		);
	}
	
	public Vertex normalize() {
		double length = Math.sqrt(x * x + y * y + z * z);
		if (length == 0) return new Vertex(0, 0, 0);
		return new Vertex(x / length, y / length, z / length);
	}
	
	public Vertex midpoint(Vertex v) {
		return new Vertex(
				(x + v.x) / 2.0,
				(y + v.y) / 2.0,
				(z + v.z) / 2.0
		);
	}
	
	@Override
	public String toString() {
		return String.format("Vertex(%.2f, %.2f, %.2f)", x, y, z);
	}
}
