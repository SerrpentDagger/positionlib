package positionlib;


import java.io.PrintStream;
import java.text.NumberFormat;

public class Matrix
{
	//@fm:off
	
	////// Static
	private boolean formatPrintNumbers = true;
	
	////// Instance
	private final double mat[][];
	private final int height, width;
	
	////////////////////////////// Constructors
	
	/**
	 * Create a new {@linkplain Matrix} with the same characteristics of toClone.
	 * @param toClone
	 */
	public Matrix(Matrix toClone)
	{
		height = toClone.height;
		width = toClone.width;
		mat = new double[height][width];
		setTo(toClone);
	}
	
	/**
	 * Wrap the two-dimensional array with a new {@linkplain Matrix}.
	 * @param matrix
	 */
	public Matrix(double[][] matrix)
	{
		mat = matrix;
		height = mat.length;
		width = mat[0].length;
	}
	
	/**
	 * Fill a new {@linkplain Matrix} of the given width and height with values from the one-dimensional array. Values from matrix will
	 * be added to the new Matrix left-to-right, top-to-bottom. Values will be taken from matrix left-to-right.
	 * @param matrix
	 * @param width
	 * @param height
	 */
	public Matrix(double[] matrix, int width, int height)
	{
		this.height = height;
		this.width = width;
		mat = new double[height][width];
		operate((i, j) -> { set(i, j, matrix[(i * width) + j]); });
	}
	
	/**
	 * Create a new {@linkplain Matrix} with the given height and width. Conditionally make it an identity matrix.
	 * @param height
	 * @param width
	 * @param identity
	 */
	public Matrix(int height, int width, boolean identity)
	{
		this.height = height;
		this.width = width;
		mat = new double[height][width];
		if (identity)
		{
			operate((i, j) -> { if (i == j) set(i, j, 1); });
		}
	}
	
	/////////////////////////////	Instance
	
	/**
	 * Run the given {@linkplain Operation} on this {@linkplain Matrix}.
	 * @param operation
	 * @return this
	 */
	public Matrix operate(Operation operation)
	{
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				operation.operate(i, j);
		return this;
	}
	
	/**
	 * @param i
	 * @param j
	 * @return Value within the {@linkplain Matrix} at (i, j).
	 */
	public double get(int i, int j)
	{
		return mat[i][j];
	}
	
	/**
	 * Set the value within the {@linkplain Matrix} at (i, j) to val.
	 * @param i
	 * @param j
	 * @param val
	 * @return this
	 */
	public Matrix set(int i, int j, double val)
	{
		mat[i][j] = val;
		return this;
	}
	
	/**
	 * Set this to an identity matrix.
	 * @return this
	 */
	public Matrix identity()
	{
		return operate((i, j) -> { set(i, j, i == j ? 1 : 0); });
	}

	/**
	 * Add val to the value within the {@linkplain Matrix} at (i, j).
	 * @param i
	 * @param j
	 * @param val
	 * @return this
	 */
	public Matrix add(int i, int j, double val)
	{
		mat[i][j] += val;
		return this;
	}
	
	/**
	 * Multiply the value within the {@linkplain Matrix} at (i, j) by val.
	 * @param i
	 * @param j
	 * @param val
	 * @return
	 */
	public Matrix mult(int i, int j, double val)
	{
		mat[i][j] *= val;
		return this;
	}
	
	/**
	 * Set the values of this to the corresponding values of source. source must be at least as large as this.
	 * @param source
	 * @return this
	 */
	public Matrix setTo(Matrix source)
	{
		return operate((i, j) -> { mat[i][j] = source.get(i, j); });
	}

	/**
	 * Add corresponding values of other to this. Does not alter other.
	 * @param other
	 * @return this with updated values.
	 */
	public Matrix add(Matrix other)
	{
		return operate((i, j) -> { mat[i][j] += other.get(i, j); });
	}

	/**
	 * Subtract corresponding values of other from this. Does not alter other.
	 * @param other
	 * @return this with updated values.
	 */
	public Matrix sub(Matrix other)
	{
		return operate((i, j) -> { mat[i][j] -= other.get(i, j); });
	}
	
	/**
	 * Scale all values within this by scalar.
	 * @param scalar
	 * @return this
	 */
	public Matrix scalar(double scalar)
	{
		return operate((i, j) -> { mat[i][j] *= scalar; });
	}
	
	/**
	 * Multiplies this by (other on the right). Produces a new Matrix(height, other.height, false).
	 * @param other
	 * @return 
	 */
	public Matrix mult(Matrix other)
	{
		Matrix result = new Matrix(height, other.width, false);
		return mult(other, result);
	}
	
	public Matrix mult(Matrix other, Matrix output)
	{
		if (!(width == other.height))
			throw new IllegalStateException("Impossible operation requested.");
		if (!(output.height == height && output.width == other.width))
			throw new IllegalStateException("Invalid output matrix.");
		
		return output.operate((i, j) ->
		{
			for (int k = 0; k < width; k++)
				output.add(i, j, this.get(i, k) * other.get(k, j));
		});
	}
	
	/////////// Logging
	
	public void print(PrintStream print)
	{
		spacer = false;
		print.println(toString());
		spacer = true;
	}
	
	public void print()
	{
		print(System.out);
	}
	
	public void println(PrintStream print)
	{
		print(print);
		print.println();
	}
	
	public void println()
	{
		println(System.out);
	}
	
	//////////////////////////// Super
	
	private boolean spacer = true;
	@Override
	public String toString()
	{
		String str = "";
		NumberFormat f = NumberFormat.getInstance();
		if (formatPrintNumbers)
		{
			f.setMaximumFractionDigits(1);
		}
		
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				str += (j == 0 ? "|" : "") + (formatPrintNumbers ? f.format(get(i, j)) : get(i, j))
					+ (j == width - 1 ? "|" + (i == height - 1 ? "" : "\n") : "; ");
		if (spacer)
			str += "\n";
		
		return str;
	}
	
	/////////////////////////// Static
	
	public static Matrix vector(double... vals)
	{
		Matrix mat = new Matrix(vals.length, 1, false);
		mat.operate((i, j) -> { mat.set(i, j, vals[i]); });
		return mat;
	}
	
	public static Matrix translation(int dimensions, double... trans)
	{
		Matrix mat = new Matrix(dimensions + 1, dimensions + 1, true);
		return mat.operate((i, j) ->
		{
			if (j == dimensions && i != dimensions)
				mat.set(i, j, trans[i]);
		});
	}
	
	public static Matrix scaling(int dimensions, double... scale)
	{
		Matrix mat = new Matrix(dimensions + 1, dimensions + 1, true);
		return mat.operate((i, j) ->
		{
			if (i == j && i != dimensions)
				mat.set(i, j, scale[i]);
		});
	}
	
	public static Matrix rotation2D(double angle, boolean padded)
	{
		int size = padded ? 3 : 2;
		Matrix mat = new Matrix(size, size, padded);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		mat.set(0, 0, cos).set(0, 1, -sin);
		mat.set(1, 0, sin).set(1, 1, cos);
		
		return mat;
	}
	
	public static Matrix rotation3D(Axis axis, double angle, boolean padded)
	{
		int size = padded ? 4 : 3;
		Matrix mat = new Matrix(size, size, true);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		if (axis == Axis.X)
		{
			mat.set(1, 1, cos).set(1, 2, -sin);
			mat.set(2, 1, sin).set(2, 2, cos);
		}
		else if (axis == Axis.Y)
		{
			mat.set(0, 0, cos).set(0, 2, sin);
			mat.set(2, 0, -sin).set(2, 2, cos);
		}
		else if (axis == Axis.Z)
		{
			mat.set(0, 0, cos).set(0, 1, -sin);
			mat.set(1, 0, sin).set(1, 1, cos);
		}
		return mat;
	}

	public static Matrix transpose(Matrix mat)
	{
		Matrix trans = new Matrix(mat.width, mat.height, false);
		return trans.operate((i, j) -> { trans.set(i, j, mat.get(j, i)); });
	}
	
	//////////////////////////////////////// Access
	
	public boolean isFormatPrintNumbers()
	{
		return formatPrintNumbers;
	}
	
	public void setFormatPrintNumbers(boolean formatPrintNumbers)
	{
		this.formatPrintNumbers = formatPrintNumbers;
	}
	
	//////////////////////////// Functional Interfaces

	@FunctionalInterface
	public static interface Operation
	{
		public void operate(int i, int j);
	}
	
	//////////////////////////// Enums
	
	public static enum Axis
	{
		X,
		Y,
		Z
	}
}
