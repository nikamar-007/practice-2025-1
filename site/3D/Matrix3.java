public class Matrix3 {
	private final double[] m;
	
	public Matrix3(double[] values) {
		if (values.length != 9) {
			throw new IllegalArgumentException("Matrix3 must have exactly 9 elements");
		}
		this.m = values.clone();
	}
	
	public Matrix3 multiply(Matrix3 other) {
		double[] result = new double[9];
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				result[row * 3 + col] =
						m[row * 3 + 0] * other.m[col + 0] +
								m[row * 3 + 1] * other.m[col + 3] +
								m[row * 3 + 2] * other.m[col + 6];
			}
		}
		return new Matrix3(result);
	}
	
	public Vertex transform(Vertex v) {
		return new Vertex(
				v.x * m[0] + v.y * m[3] + v.z * m[6],
				v.x * m[1] + v.y * m[4] + v.z * m[7],
				v.x * m[2] + v.y * m[5] + v.z * m[8]
		);
	}
}