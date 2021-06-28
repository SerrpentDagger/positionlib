package positionlib;


import java.util.ArrayList;

/**
 * A class to go along with {@linkplain Pos3d}, utilizing the {@linkplain Matrix} class, allowing for efficient mass-transformation of vectors. 
 * @author SerpentDagger
 */
public class Transform3d
{
	/////// Instance
	private ArrayList<Matrix> transforms = new ArrayList<Matrix>();
	private Matrix transform = new Matrix(4, 4, true);
	private Matrix helper1 = new Matrix(4, 1, false).set(3, 0, 1), helper2 = new Matrix(4, 1, false).set(3, 0, 1);
	
	///////////////////// Constructors
	
	/**
	 * Create a new {@linkplain Transform3d}, a clone of toClone.
	 * @param toClone
	 */
	public Transform3d(Transform3d toClone)
	{
		toClone.transforms.forEach((mat) ->
		{
			transforms.add(new Matrix(mat));
		});
		recalcMatrix();
	}
	
	/**
	 * Create a new {@linkplain Transform3d} with the given transformation {@linkplain Matrix}. 
	 * @param transformMat
	 */
	public Transform3d(Matrix transformMat)
	{
		addTransform(transformMat);
	}
	
	/**
	 * Create a new {@linkplain Transform3d}.
	 */
	public Transform3d()
	{
	}
	
	public Transform3d(Matrix... transformMats)
	{
		addTransforms(transformMats);
	}
	
	///////////////////// Instance
	
	/**
	 * Transform the given {@linkplain Pos3d} with this {@linkplain Transform3d}.
	 * @param pos
	 * @return this
	 */
	public Transform3d transform(Pos3d pos)
	{
		helper1.set(0, 0, pos.getX()).set(1, 0, pos.getY()).set(2, 0, pos.getZ());
		transform.mult(helper1, helper2);
		pos.setX(helper2.get(0, 0)).setY(helper2.get(1, 0)).setZ(helper2.get(2, 0));
		return this;
	}
	
	/**
	 * Add the list of transformations to this {@linkplain Transform3d}.
	 * @param transforms
	 * @return this
	 */
	public Transform3d addTransforms(Matrix... transforms)
	{
		for (Matrix trans : transforms)
			addTransform(trans, false);
		return recalcMatrix();
	}
	
	/**
	 * Add the given transformation to this {@linkplain Transform3d}, and do or don't recalculate the overall transformation.
	 * @param transform
	 * @param recalculate
	 * @return this
	 */
	public Transform3d addTransform(Matrix transform, boolean recalculate)
	{
		transforms.add(transform);
		return recalculate ? recalcMatrix() : this;
	}
	
	/**
	 * Adds the transformation as if with {@linkplain Transform3d#addTransform(Matrix, boolean)}, with true.
	 * @param transform
	 * @return this
	 */
	public Transform3d addTransform(Matrix transform)
	{
		return addTransform(transform, true);
	}
	
	/**
	 * Remove the given transformation from this {@linkplain Transform3d}.
	 * @param transform
	 * @return this
	 */
	public Transform3d removeTransform(Matrix transform)
	{
		transforms.remove(transform);
		return recalcMatrix();
	}
	
	/**
	 * Recalculate the overall transformation accrued from all the added transformations.
	 * @return this
	 */
	public Transform3d recalcMatrix()
	{
		transform.identity();
		transforms.forEach((mat) ->
		{
			transform = mat.mult(transform);
		});
		return this;
	}
	
	////////////////////////////// Super
	
	@Override
	public String toString()
	{
		return transform.toString();
	}
	
	////////////////////////////////////// Access
	
	/**
	 * @return The overall transformation matrix built from all the added transformations.
	 */
	public Matrix getMatrix()
	{
		return transform;
	}
}
