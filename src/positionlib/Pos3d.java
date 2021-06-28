package positionlib;


import java.text.NumberFormat;
import java.util.Comparator;

import positionlib.Matrix.Axis;

/**
 * <p><b>In this system, Y is the vertical axis, and Z is the horizontal axis that marks the 0-angle direction in Spherical and Cylindrical.
 * Angles increase counterclockwise around their rotational axis as viewed from the positive end of that axis.</b>
 * 
 * <p>An instance of this class stores a 3D position, involving 6 doubles, an int, a boolean, and two uninitialized Pos3ds (null).
 * 
 * <p>With a Pos3d object, you can easily perform a wide variety of complex transformations on the 3D position stored within it, all of which function
 * properly on any coordinate system type supported by Pos3d.
 * 
 * <p>All transformation methods are chainable, meaning they return either this, or a new Pos3d, with updated values,
 * depending on whether you use the {@linkplain Pos3d#clean()} method or not.
 * 
 * <p>By default, transformation methods <b>do</b> alter the values of the Pos3d you call them on (they will <i>never</i> alter the values of parameters).
 * This is very helpful in various situations, and means less object-creation in general.
 * If you do need a new Pos3d, however, use {@link Pos3d#clean()}, or {@link Pos3d#Pos3d(Pos3d)}, which
 * will return a new Pos3d as a clone of this. The new Pos3d will have the checkpoint of this as well.
 * 
 * <p>Checkpoints can be set in the middle of a method chain by calling {@link Pos3d#setCheckpoint()} or {@link Pos3d#setCheckpoint(Pos3d)}
 * on the Pos3d object. You can revert to a set checkpoint by calling {@link Pos3d#revert()} on the Pos3d object. Reverting does not alter the checkpoint.
 * 
 * <p>Relative operations can be performed by using the {@linkplain Pos3d#relative()} method, and associated
 * members. Transformation matricies for a given Pos3d can be aquired from {@linkplain Pos3d#getTransforms()},
 * which can then be applied with {@linkplain Pos3d#transform(Transform3d)}.
 * 
 * <p>Methods are optimized to not undergo unnecessary conversions, and to generally be as efficient as possible. There may be some minor infractions to this
 * statement, but as I update the library I will optimize where needed. If you are concerned with performance, then I would suggest performing all of your
 * transformations in groups corresponding to the coordinate system required by those transformations. This would remove unnecessary conversions.
 * 
 * <p><i>If you find a bug or want a feature that doesn't currently exist, open an issue on the GitHub project for this library, at <a
 * href="https://github.com/SerrpentDagger/positionlib/issues">this site</a>.</i>
 * 
 * @author SerpentDagger (M. H.)
 * 
 * <p>--------------------------------------------------------------------
 * 
 * <p>Copyright © 2019 Merrick Harmon
 * 
 * <p>This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <a
 * href="https://www.gnu.org/licenses/">this website</a>.
 *
 */
public class Pos3d
{
	public static final double QUARTER = Math.PI / 2, HALF = Math.PI, FULL = 2 * Math.PI;
	/**
	 * A {@linkplain Pos3d} can be operating within any of these systems at a given moment.
	 * <p>CARTESIAN = 0, SPHERICAL = 1, CYLINDRICAL = 2.
	 */
	public static final int CARTESIAN = 0, SPHERICAL = 1, CYLINDRICAL = 2;
	/**
	 * These are used for comparison with the outputs of quadrant functions. N stands for "negative," and P stands for "positive." The quadrants are in the order XYZ.
	 * <p>QNNN = 0, QPNN = 1, QNPN = 2, QNNP = 3, QPPN = 4, QPNP = 5, QNPP = 6, QPPP = 7.
	 */
	public static final int QNNN = 0, QPNN = 1, QNPN = 2, QNNP = 3, QPPN = 4, QPNP = 5, QNPP = 6, QPPP = 7;
	
	/** The accepted error that allows values to be considered 0 to avoid processing. */
	private static double EQUALS_ERROR = 1E-11;
	/** The squared accepted error that allows two {@linkplain Pos3d}s to be considered equal with {@linkplain Pos3d#equals(Object)}. */
	private static double EQUALS_ERROR_SQR = EQUALS_ERROR * EQUALS_ERROR;
	/** The default value used for {@linkplain Pos3d#roundToString} in new {@linkplain Pos3d}s. */
	public static boolean ROUND_TO_STRING = true;
	
	/**
	 * The values of this. These are subject to change, and only the values of the current {@linkplain Pos3d#system} are guaranteed to be up-to-date.
	 * Instead use the getter methods for up-to-date values.
	 */
	private double x = 0, y = 0, z = 0, ang1 = 0, ang2 = 0, mag = 0;
	/** The current coordinate system of this. See {@linkplain Pos3d#CARTESIAN}. */
	public int system = -1;
	/** The checkpoint of this. Relevant in {@linkplain Pos3d#setCheckpoint()}, {@linkplain Pos3d#setCheckpoint(Pos3d)}, and {@linkplain Pos3d#revert()}. */
	public Pos3d checkpoint;
	/** The position to which this {@linkplain Pos3d} is relative. Usually null. Used with {@linkplain Pos3d#relative()} and {@linkplain Pos3d#unrelative()}.*/
	public Pos3d relative;
	
	/** The origin point. {@linkplain Pos3d} methods do not change this. */
	private static final Pos3d ORIGIN = new Pos3d(0, 0, 0);
	/** Helper vectors of layer 0. These are subject to change willy-nilly within layer 0 functions, but should not be used in layers > 0. */
	private static final Pos3d P1 = new Pos3d(), P2 = new Pos3d(), P3 = new Pos3d();
	/** Helper vectors of layer 1. These are subject to change willy-nilly within layer 1 functions, but should not be used in layers > 1. */
	private static final Pos3d P11 = new Pos3d();
	/** Vectors representing different axis. Should not be changed. */
	public static final Pos3d XP_AXIS = new Pos3d(1, 0, 0), YP_AXIS = new Pos3d(0, 1, 0), ZP_AXIS = new Pos3d(0, 0, 1);
	
	/** Whether or not {@linkplain Pos3d#toString()} rounds its output. */
	private boolean roundToString = ROUND_TO_STRING;
		
	
	/** 
	 * New Pos3d with cartesian values x, y, z.
	 * @param x
	 * @param y
	 * @param z 
	 */
	public Pos3d(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		system = CARTESIAN;
	}
	
	/**
	 * New Pos3d with values for specified system.
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param a
	 * @param b
	 * @param c
	 * @param system
	 */
	public Pos3d(double a, double b, double c, int system)
	{
		if (system == CARTESIAN)
		{
			this.x = a;
			this.y = b;
			this.z = c;
		}
		else if (system == SPHERICAL)
		{
			this.mag = a;
			this.ang1 = b;
			this.ang2 = c;
		}
		else if (system == CYLINDRICAL)
		{
			this.mag = a;
			this.ang1 = b;
			this.y = c;
		}
		this.system = system;
	}
	
	/**
	 * New Pos3d with values for specified system.
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param abc
	 * @param system
	 */
	public Pos3d(double[] abc, int system)
	{
		this(abc[0], abc[1], abc[2], system);
	}
	
	public Pos3d()
	{}
	
	/**
	 * Creates a clone of toClone, including the checkpoint if !null.
	 * @param toClone
	 */
	public Pos3d(Pos3d toClone)
	{
		if (toClone == null) { return; }
		this.x = toClone.x;
		this.y = toClone.y;
		this.z = toClone.z;
		this.ang1 = toClone.ang1;
		this.ang2 = toClone.ang2;
		this.mag = toClone.mag;
		this.system = toClone.system;
		if (toClone.checkpoint != null) { checkpoint = new Pos3d().setTo(toClone.checkpoint); }
		
	}
	
	/**
	 * @return A clone of this, including the checkpoint if !null.
	 */
	public Pos3d clean()
	{
		return new Pos3d(this);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Pos3d))
			return false;
		Pos3d pos = (Pos3d) obj;
		P1.setTo(pos).sub(this);
		return P1.getMagSSqr() < EQUALS_ERROR_SQR;
	}
	
	/**
	 * Convert to Cartesian coordinates.
	 * @return this with updated values.
	 */
	public Pos3d toCartesian()
	{
		if (system == SPHERICAL)
		{
			this.x = mag * Math.sin(ang1) * Math.cos(ang2);
			this.y = mag * Math.sin(ang2);
			this.z = mag * Math.cos(ang1) * Math.cos(ang2);
		}
		else if (system == CYLINDRICAL)
		{
			this.x = mag * Math.sin(ang1);
			this.z = mag * Math.cos(ang1);
		}
		system = CARTESIAN;
		return this;
	}
	
	/**
	 * Convert to Spherical coordinates.
	 * @return this with updated values.
	 */
	public Pos3d toSpherical()
	{
		if (system == CARTESIAN)
		{
			mag = Math.sqrt((x*x) + (y*y) + (z*z));
			ang1 = Math.atan2(x, z);
			if (x == 0 && z == 0)
				ang2 = Math.PI / 2;
			else
				ang2 = Math.atan(y / Math.sqrt((x*x) + (z*z)));
		}
		else if (system == CYLINDRICAL)
		{
			ang2 = Math.atan2(y, mag);
			mag = Math.sqrt((mag*mag) + (y*y));
		}
		system = SPHERICAL;
		return this;
	}
	
	/**
	 * Convert to Cylindrical coordinates.
	 * @return this with updated values.
	 */
	public Pos3d toCylindrical()
	{
		if (system == CARTESIAN)
		{
			mag = Math.sqrt((x*x) + (z*z));
			ang1 = Math.atan2(x, z);
		}
		else if (system == SPHERICAL)
		{
			y = mag * Math.sin(ang2);
			mag = mag * Math.cos(ang2);
		}
		system = CYLINDRICAL;
		return this;
	}
	
	/**
	 * Converts to Cartesian and adds toAdd to this.
	 * @param toAdd Pos3d to add to this.
	 * @return Cartesian addition of toAdd onto this.
	 */
	public Pos3d add(Pos3d toAdd)
	{		
		toCartesian();
		toAdd.toCartesian();
		x += toAdd.x;
		y += toAdd.y;
		z += toAdd.z;
		
		return this;
	}
	
	/**
	 * Converts to Cartesian and subtracts toSub from this.
	 * @param toSub Pos3d to subtract from this.
	 * @return Cartesian subtraction of toSub from this.
	 */
	public Pos3d sub(Pos3d toSub)
	{
		toSub.toCartesian();
		toCartesian();
		x -= toSub.x;
		y -= toSub.y;
		z -= toSub.z;
		
		return this;
	}
	
	/**
	 * Calls the add method, using a new Pos3d built from the parameters.
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param a
	 * @param b
	 * @param c
	 * @param system
	 * @return this with updated values.
	 */
	public Pos3d add(double a, double b, double c, int system)
	{
		Pos3d temp = new Pos3d(a, b, c, system);
		
		this.add(temp);
		return this;
	}
	
	/**
	 * Calls the sub method, using a new Pos3d built from the parameters.
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param a
	 * @param b
	 * @param c
	 * @param system
	 * @return this with updated values.
	 */
	public Pos3d sub(double a, double b, double c, int system)
	{
		Pos3d temp = new Pos3d(a, b, c, system);
		
		this.sub(temp);
		return this;
	}
	
	/**
	 * Converts to Cartesian and multiplies this by toMult (x by x, y by y, z by z).
	 * @param toMult Pos3d to multiply this by.
	 * @return Cartesian multiplication of this by toMult.
	 */
	public Pos3d mult(Pos3d toMult)
	{
		toMult.toCartesian();
		toCartesian();
		x *= toMult.x;
		y *= toMult.y;
		z *= toMult.z;
		
		return this;
	}
	
	/**
	 * Scales this by scale.
	 * @param scale Scale to multiply this by.
	 * @return Scaling of this by scale.
	 */
	public Pos3d scale(double scale)
	{
		if (system == CARTESIAN)
		{
			x *= scale;
			y *= scale;
			z *= scale;
		}
		else if (system == SPHERICAL)
		{
			mag *= scale;
		}
		else if (system == CYLINDRICAL)
		{
			mag *= scale;
			y *= scale;
		}
		return this;
	}
	
	/**
	 * Calls the mult method, using a new Pos3d built from the parameters.
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param a
	 * @param b
	 * @param c
	 * @param system
	 * @return this with updated values.
	 */
	public Pos3d mult(double a, double b, double c, int system)
	{
		Pos3d temp = new Pos3d(a, b, c, system);
		
		this.mult(temp);
		return this;
	}
	
	/**
	 * Converts to Cartesian and divides this by toDivi (x by x, y by y, z by z).
	 * @param toDivi Pos3d to divide this by.
	 * @return Cartesian division of this by toDivi.
	 */
	public Pos3d divi(Pos3d toDivi)
	{
		toDivi.toCartesian();
		toCartesian();
		x /= toDivi.x;
		y /= toDivi.y;
		z /= toDivi.z;
		
		return this;
	}
	
	/**
	 * Calls the divi method, using a new Pos3d built from the parameters.
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param a
	 * @param b
	 * @param c
	 * @param system
	 * @return this with updated values.
	 */
	public Pos3d divi(double a, double b, double c, int system)
	{
		Pos3d temp = new Pos3d(a, b, c, system);
		
		this.divi(temp);
		return this;
	}
	
	/**
	 * Converts to Cartesian and squares the individual (x, y, z) values.
	 * @return Cartesian square of this.
	 */
	public Pos3d sqr()
	{
		toCartesian();
		x *= x;
		y *= y;
		z *= z;
		
		return this;
	}
	
	/**
	 * Converts to Spherical and squares the magnitude.
	 * @return this with updated values.
	 */
	public Pos3d sqrMagS()
	{
		toSpherical();
		mag *= mag;
		
		return this;
	}
	
	/**
	 * Converts to Cylindrical and squares the magnitude.
	 * @return this with updated values.
	 */
	public Pos3d sqrMagC()
	{
		toCylindrical();
		mag *= mag;
		
		return this;
	}

	/**
	 * Converts to Cartesian and takes the square root of the individual (x, y, z) values.
	 * @return Cartesian square root of this.
	 */
	public Pos3d sqrt()
	{
		toCartesian();
		x = Math.sqrt(x);
		y = Math.sqrt(y);
		z = Math.sqrt(z);
		
		return this;
	}
	
	/**
	 * Converts to Spherical and takes the square root of the magnitude.
	 * @return this with updated values.
	 */
	public Pos3d sqrtMagS()
	{
		toSpherical();
		mag = Math.sqrt(mag);
		
		return this;
	}
	
	/**
	 * Converts to Cylindrical and takes the square root of the magnitude.
	 * @return this with updated values.
	 */
	public Pos3d sqrtMagC()
	{
		toCylindrical();
		mag = Math.sqrt(mag);
		
		return this;
	}
	
	/**
	 * Converts to Cartesian and takes the square root of the individual (x, y, z) values.
	 * <p>Always returns positive values.
	 * @return Positive Cartesian square root of this.
	 */
	public Pos3d sqrtPositive()
	{
		toCartesian();
		x = (x < 0) ? Math.sqrt(-x) : Math.sqrt(x);
		y = (y < 0) ? Math.sqrt(-y) : Math.sqrt(y);
		z = (z < 0) ? Math.sqrt(-z) : Math.sqrt(z);
		
		return this;
	}
	
	/**
	 * Converts to Cartesian and squares the individual (x, y, z) values while preserving sign.
	 * @return Cartesian square of this, preserving signs.
	 */
	public Pos3d sqrPreserveSign()
	{
		toCartesian();
		x *= (x < 0) ? -x : x;
		y *= (y < 0) ? -y : y;
		z *= (z < 0) ? -z : z;
		
		return this;
	}
	
	/**
	 * Converts to Cartesian and takes the square root of the individual (x, y, z) values while preserving sign.
	 * @return Cartesian square root of this, preserving signs.
	 */
	public Pos3d sqrtPreserveSign()
	{
		toCartesian();
		x = (x < 0) ? -Math.sqrt(-x) : Math.sqrt(x);
		y = (y < 0) ? -Math.sqrt(-y) : Math.sqrt(y);
		z = (z < 0) ? -Math.sqrt(-z) : Math.sqrt(z);
		
		return this;
	}
	
	/**
	 * Set values of this to values of setTo (checkpoint not included).
	 * @param setTo Values to put into this.
	 * @return this with updated values.
	 */
	public Pos3d setTo(Pos3d setTo)
	{
		if (setTo == null) { return null; }
		this.x = setTo.x;
		this.y = setTo.y;
		this.z = setTo.z;
		this.ang1 = setTo.ang1;
		this.ang2 = setTo.ang2;
		this.mag = setTo.mag;
		this.system = setTo.system;
		
		return this;
	}
	
	/**
	 * Set values of this to values given (checkpoint not included).
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param a
	 * @param b
	 * @param c
	 * @param system
	 * @return this with updated values.
	 */
	public Pos3d setTo(double a, double b, double c, int system)
	{
		if (system == CARTESIAN)
		{
			this.x = a;
			this.y = b;
			this.z = c;
		}
		else if (system == SPHERICAL)
		{
			this.mag = a;
			this.ang1 = b;
			this.ang2 = c;
		}
		else if (system == CYLINDRICAL)
		{
			this.mag = a;
			this.ang1 = b;
			this.y = c;
		}
		this.system = system;
		
		return this;
	}
	

	/**
	 * Set values of this to values given (checkpoint not included).
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param abc
	 * @param system
	 * @return this with updated values.
	 */
	public Pos3d setTo(double[] abc, int system)
	{
		return setTo(abc[0], abc[1], abc[2], system);
	}
	
	/**
	 * Converts to coordinate system specified.
	 * @param system System to convert to.
	 * @return this with updated values.
	 */
	public Pos3d toSystem(int system)
	{
		if (system == CARTESIAN)
		{
			toCartesian();
		}
		else if (system == SPHERICAL)
		{
			toSpherical();
		}
		else if (system == CYLINDRICAL)
		{
			toCylindrical();
		}
		return this;
	}
	
	/**
	 * Sets y to 0, ang2 to 0.
	 * @return this with updated values.
	 */
	public Pos3d flatten()
	{
		this.y = 0;
		this.ang2 = 0;
		
		return this;
	}
	
	/**
	 * Rotates this around y axis.
	 * 
	 * <p>All axis-based rotations rotate counter-clockwise around the axis, when said axis is viewed from the positive end looking towards the negative end.
	 * Vertical rotation (which is not axis-based) rotates "upwards†" from the positive direction in the horizontal plane, where the positive
	 * direction is defined by the horizontal angle.
	 * 
	 * <p>†Note that the "upwards" direction will not always be up. It will be up when the vertical angle resides in the first or fourth quadrant, and down when
	 * the vertical angle resides in the second or third quadrant, in the manner one would expect from increasing any angle. The "upwards" direction may also
	 * change if the vector is converted from Spherical and back, if the angle from horizontal is greater than pi/2. This is because the transformation
	 * chooses the lowest of the two angles from horizontal.
	 * 
	 * @param radians Radian value to rotate by.
	 * @return this with updated values.
	 */
	public Pos3d rotateAroundY(double radians)
	{
		if (system == CARTESIAN)
			toCylindrical();
		this.ang1 += radians;
		
		return this;
	}
	
	/**
	 * Rotates this vertically around the origin.
	 * 
	 * <p>All axis-based rotations rotate counter-clockwise around the axis, when said axis is viewed from the positive end looking towards the negative end.
	 * Vertical rotation (which is not axis-based) rotates "upwards†" from the positive direction in the horizontal plane, where the positive
	 * direction is defined by the horizontal angle.
	 * 
	 * <p>†Note that the "upwards" direction will not always be up. It will be up when the vertical angle resides in the first or fourth quadrant, and down when
	 * the vertical angle resides in the second or third quadrant, in the manner one would expect from increasing any angle.
	 * 
	 * @param radians Radian value to rotate by.
	 * @return this with updated values.
	 */
	public Pos3d rotateVertical(double radians)
	{
		toSpherical();
		this.ang2 += radians;
		
		return this;
	}
	
	/**
	 * Sets the Spherical magnitude of this.
	 * @param mag new Spherical magnitude.
	 * @return this with specified Spherical magnitude.
	 */
	public Pos3d setMagS(double mag)
	{
		toSpherical();
		this.mag = mag;
		
		return this;
	}
	
	/**
	 * Sets the Cylindrical magnitude of this.
	 * @param mag new Cylindrical magnitude.
	 * @return this with specified Cylindrical magnitude.
	 */
	public Pos3d setMagC(double mag)
	{
		toCylindrical();
		this.mag = mag;
		
		return this;
	}
	
	/**
	 * Converts angles to radians (Note, most transformations REQUIRE angles to be in radians. It is not recommended to ever have angles in degrees).
	 * @return this with updated values.
	 */
	public Pos3d toRad()
	{
		this.ang1 = ang1 * Math.PI / 180;
		this.ang2 = ang2 * Math.PI / 180;
		
		return this;
	}
	
	/**
	 * Sets this's checkpoint to this.
	 * @return this.
	 */
	public Pos3d setCheckpoint()
	{
		this.checkpoint = (checkpoint == null ? new Pos3d() : checkpoint).setTo(this);
		
		return this;
	}
	
	/**
	 * Sets this's checkpoint to checkpoint.
	 * @param checkpoint Pos3d to become this's checkpoint.
	 * @return this.
	 */
	public Pos3d setCheckpoint(Pos3d checkpoint)
	{
		this.checkpoint = (checkpoint == null ? new Pos3d() : checkpoint).setTo(checkpoint);
		
		return this;
	}
	
	/**
	 * Reverts this to state saved in checkpoint (does not revert checkpoint).
	 * @return this with values of checkpoint.
	 */
	public Pos3d revert()
	{
		this.setTo(checkpoint);
		
		return this;
	}
	
	/**
	 * Sets {@linkplain Pos3d#roundToString}.
	 * @param round
	 * @return this with updated values.
	 */
	public Pos3d setRoundToString(boolean round)
	{
		roundToString = round;
		return this;
	}
	
	/**
	 * @return {@linkplain Pos3d#roundToString}.
	 */
	public boolean isRoundToString()
	{
		return roundToString;
	}
	
	/**
	 * @return String representation of this Pos3d. Value order as follows: {x y z mag ang1 ang2 system ("null" if checkpoint == null, else checkpoint.toString())}
	 */
	@Override
	public String toString()
	{
		NumberFormat f = NumberFormat.getInstance();
		String x, y, z, magC, magS, ang1, ang2;
		if (roundToString)
		{
			f.setMaximumFractionDigits(2);
			x = f.format(getX());
			y = f.format(getY());
			z = f.format(getZ());
			magC = f.format(getMagC());
			magS = f.format(getMagS());
		}
		else
		{
			x = "" + getX();
			y = "" + getY();
			z = "" + getZ();
			magC = "" + getMagC();
			magS = "" + getMagS();
		}
		ang1 = getAng1InPi();
		ang2 = getAng2InPi();
		
		return "{X: " + x + ", Y: " + y + ", Z: " + z
			+ ", MagC: " + magC
			+ ", MagS: " + magS + ", AzmAng: " + ang1 + ", VrtAng: " + ang2
			+ ", Checkpoint: " + (checkpoint == null ? "null" : checkpoint.toString())
			+ ", Relative: " + (relative == null ? "null" : relative.toString()) + "}";
	}
	
	/**
	 * @return The concise String representing this Pos3d, without names, colons, or commas separating values.
	 */
	public String toStringConcise()
	{
		return "{" + x + " " + y + " " + z + " " + mag + " "
				+ ang1 + " " + ang2 + " " + system + " " + ((checkpoint == null) ? "null" : checkpoint.toStringConcise()) + ", " + relative.toStringConcise() + "}";
	}
	
	/**
	 * Flips this across the origin.
	 * @return this with updated values.
	 */
	public Pos3d flip()
	{
		return scale(-1);
	}
	
	/**
	 * Flips this across the origin and doubles.
	 * @return this with updated values.
	 */
	public Pos3d flipAndDouble()
	{
		return scale(-2);
	}
	
	/**
	 * Convenient addition of this onto this.
	 * @return this with updated values.
	 */
	public Pos3d doublePos()
	{
		if (system == CARTESIAN)
		{
			x *= 2;
			y *= 2;
			z *= 2;
		}
		else if (system == SPHERICAL)
		{
			mag *= 2;
		}
		else if (system == CYLINDRICAL)
		{
			y *= 2;
			mag *= 2;
		}
		return this;
	}
	
	/**
	 * @return The Spherical magnitude of this Pos3d.
	 */
	public double getMagS()
	{
		return this.toSpherical().mag;
	}
	
	/**
	 * @return The squared Spherical magnitude of this Pos3d. 
	 */
	public double getMagSSqr()
	{
		if (system == CARTESIAN)
			return (x*x) + (y*y) + (z*z);
		else if (system == CYLINDRICAL)
			return (y*y) + (mag*mag);
		else if (system == SPHERICAL)
			return (mag*mag);
		else
			return this.toSpherical().mag * mag;
	}
	
	/**
	 * @return The Cylindrical magnitude of this Pos3d.
	 */
	public double getMagC()
	{
		return this.toCylindrical().mag;
	}
	
	/**
	 * @return The squared Cylindrical magnitude of this Pos3d.
	 */
	public double getMagCSqr()
	{
		if (system == CARTESIAN)
			return (x*x) + (z*z);
		else if (system == CYLINDRICAL)
			return (mag*mag);
		else 
			return toCylindrical().mag * mag;
	}
	
	/**
	 * @return The Cartesian x of this Pos3d.
	 */
	public double getX()
	{
		return this.toCartesian().x;
	}

	/**
	 * @return The Cartesian y of this Pos3d.
	 */
	public double getY()
	{
		return this.toCartesian().y;
	}
	
	/**
	 * @return The rotation around the Y-Axis.
	 */
	public double getAng1()
	{
		return this.toCylindrical().ang1;
	}
	
	/**
	 * @return The rotation around the Y-Axis in terms of pi.
	 */
	public String getAng1InPi()
	{
		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(3);
		return (roundToString ? f.format(getAng1() / Math.PI) : (getAng1() / Math.PI)) + "pi";
	}
	
	/**
	 * @return The rotation from horizontal.
	 */
	public double getAng2()
	{
		return this.toSpherical().ang2;
	}
	/**
	 * @return The rotation from horizontal in terms of pi.
	 */
	public String getAng2InPi()
	{
		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(3);
		return (roundToString ? f.format(getAng2() / Math.PI) : (getAng2() / Math.PI)) + "pi";
	}
	
	/**
	 * @return The rotation around the X-Axis.
	 */
	public double getAngX()
	{
		return -Math.atan2(this.toCartesian().y, z);
	}
	
	/**
	 * @return The rotation around the Z-Axis.
	 */
	public double getAngZ()
	{
		return Math.atan2(this.toCartesian().y, x);
	}
	
	/**
	 * @return The Cartesian Z of this Pos3d.
	 */
	public double getZ()
	{
		return this.toCartesian().z;
	}
	
	public double getA()
	{
		return system == CARTESIAN ? x : mag;
	}
	
	public double getB()
	{
		return system == CARTESIAN ? y : ang1;
	}
	
	public double getC()
	{
		return system == CARTESIAN ? z : system == CYLINDRICAL ? y : ang2;
	}
	
	/**
	 * Get the quadrant this Pos3d is in. Zeros count as positive.
	 * <p>Converts to Cartesian to get values, then back to original.
	 * @return One of the public static final Q### variables in this class, depending on the quadrant.
	 * <p>(E.G, QNPN -> Quadrant Negative Positive Negative, in Cartesian coordinate order)
	 */
	public int getQuadrantZP()
	{
		toCartesian();

		if (x < 0)
		{
			if (y < 0)
			{
				if (z < 0)
				{
					return QNNN;
				}
				else return QNNP;
			}
			else if (z < 0)
			{
				return QNPN;
			}
			else return QNPP;
		}
		else if (y < 0)
		{
			if (z < 0)
			{
				return QPNN;
			}
			else return QPNP;
		}
		else if (z < 0)
		{
			return QPPN;
		}
		else return QPPP;
	}
	
	/**
	 * Get the quadrant this Pos3d is in. Zeros count as negative.
	 * <p>Converts to Cartesian to get values, then back to original.
	 * @return One of the public static final Q### variables in this class, depending on the quadrant.
	 * <p>(E.G, QNPN -> Quadrant Negative Positive Negative, in Cartesian coordinate order)
	 */
	public int getQuadrantZN()
	{
		toCartesian();
		
		if (x <= 0)
		{
			if (y <= 0)
			{
				if (z <= 0)
				{
					return QNNN;
				}
				else return QNNP;
			}
			else if (z <= 0)
			{
				return QNPN;
			}
			else return QNPP;
		}
		else if (y <= 0)
		{
			if (z <= 0)
			{
				return QPNN;
			}
			else return QPNP;
		}
		else if (z <= 0)
		{
			return QPPN;
		}
		else return QPPP;
	}
	
	/**
	 * Rotates this around x axis.
	 * 
	 * <p>All axis-based rotations rotate counter-clockwise around the axis, when said axis is viewed from the positive end looking towards the negative end.
	 * Vertical rotation (which is not axis-based) rotates "upwards†" from the positive direction in the horizontal plane, where the positive
	 * direction is defined by the horizontal angle.
	 * 
	 * <p>†Note that the "upwards" direction will not always be up. It will be up when the vertical angle resides in the first or fourth quadrant, and down when
	 * the vertical angle resides in the second or third quadrant, in the manner one would expect from increasing any angle.
	 * 
	 * @param radians Radian value to rotate by.
	 * @return this with updated values.
	 */
	public Pos3d rotateAroundX(double radians)
	{
		toCartesian();
		
		double tempMag = Math.sqrt((z*z) + (y*y));
		double angle = Math.atan2(y, z);
		
		angle -= radians;
		
		y = tempMag * Math.sin(angle);
		z = tempMag * Math.cos(angle);
		
		return this;
	}
	
	/**
	 * Rotates this around z axis.
	 * 
	 * <p>All axis-based rotations rotate counter-clockwise around the axis, when said axis is viewed from the positive end looking towards the negative end.
	 * Vertical rotation (which is not axis-based) rotates "upwards†" from the positive direction in the horizontal plane, where the positive
	 * direction is defined by the horizontal angle.
	 * 
	 * <p>†Note that the "upwards" direction will not always be up. It will be up when the vertical angle resides in the first or fourth quadrant, and down when
	 * the vertical angle resides in the second or third quadrant, in the manner one would expect from increasing any angle.
	 * 
	 * @param radians Radian value to rotate by.
	 * @return this with updated values.
	 */
	public Pos3d rotateAroundZ(double radians)
	{
		toCartesian();
		
		double tempMag = Math.sqrt((x*x) + (y*y));
		double angle = Math.atan2(y, x);
		
		angle += radians;
		
		y = tempMag * Math.sin(angle);
		x = tempMag * Math.cos(angle);
		
		return this;
	}
	
	/**
	 * Rotates this around arbitrary axis.
	 *
	 * <p>All axis-based rotations rotate counter-clockwise around the axis, when said axis is viewed from the positive end looking towards the negative end.
	 * Vertical rotation (which is not axis-based) rotates "upwards†" from the positive direction in the horizontal plane, where the positive
	 * direction is defined by the horizontal angle.
	 * 
	 * <p>†Note that the "upwards" direction will not always be up. It will be up when the vertical angle resides in the first or fourth quadrant, and down when
	 * the vertical angle resides in the second or third quadrant, in the manner one would expect from increasing any angle.
	 * 
	 * @param axis One endpoint of the axis of rotation, the other is the origin.
	 * @param radians Radian value to rotate by.
	 * @return this with updated values.
	 */
	public Pos3d rotateAroundAxis(Pos3d axis, double radians)
	{
		double axisAng1 = axis.toSpherical().ang1;
		double axisAng2 = axis.ang2;
		
		toCartesian();
		rotateAroundY(-axisAng1);
		rotateAroundX(-((Math.PI / 2) - axisAng2));
		rotateAroundY(radians);
		rotateAroundX(((Math.PI / 2) - axisAng2));
		rotateAroundY(axisAng1);
		
		return this;
	}
	
	/**
	 * Sets the Cartesian x value of this.
	 * @param x Value to set x to.
	 * @return this with updated values.
	 */
	public Pos3d setX(double x)
	{
		toCartesian().x = x;
		return this;
	}
	
	/**
	 * Sets the Cartesian y value of this.
	 * @param y Value to set y to.
	 * @return this with updated values.
	 */
	public Pos3d setY(double y)
	{
		if (system == SPHERICAL) { toCylindrical(); }
		this.y = y;
		return this;
	}
	
	/**
	 * Sets the Cartesian z value of this.
	 * @param z Value to set z to.
	 * @return this with updated values.
	 */
	public Pos3d setZ(double z)
	{
		toCartesian().z = z;
		return this;
	}
	
	/**
	 * Sets the rotation around the y axis of this.
	 * @param ang1 Value to set ang1 to.
	 * @return this with updated values.
	 */
	public Pos3d setAng1(double ang1)
	{
		if (system == CARTESIAN)
		{
			toCylindrical();
		}
		this.ang1 = ang1;
		return this;
	}
	
	/**
	 * Sets the rotation from the horizontal of this.
	 * @param ang2 Value to set ang2 to.
	 * @return this with updated values.
	 */
	public Pos3d setAng2(double ang2)
	{
		if (system != SPHERICAL)
		{
			toSpherical();
		}
		this.ang2 = ang2;
		return this;
	}
	
	/**
	 * Mirrors this across the XY plane.
	 * @return this with updated values.
	 */
	public Pos3d mirrorAcrossXY()
	{
		if (system != CARTESIAN)
		{
			ang1 = -(ang1 + Math.PI);
		}
		else
		{
			z = -z;
		}
		return this;
	}
	
	/**
	 * Mirrors this across the YZ plane.
	 * @return this with updated values.
	 */
	public Pos3d mirrorAcrossYZ()
	{
		if (system != CARTESIAN)
		{
			ang1 = -ang1;
		}
		else
		{
			x = -x;
		}
		return this;
	}
	
	/**
	 * Mirrors this across the ZX plane.
	 * @return this with updated values.
	 */
	public Pos3d mirrorAcrossZX()
	{
		if (system != SPHERICAL)
		{
			y = -y;
		}
		else
		{
			ang2 = -ang2;
		}
		return this;
	}
	
	/**
	 * Mirrors this across the arbitrary plane defined by the two parameters and the origin.
	 * @param plane1 First point in the plane that will be mirrored across.
	 * @param plane2 Second point in the plane that will be mirrored across.
	 * @return this with updated values.
	 */
	public Pos3d mirrorAcrossPlane(Pos3d plane1, Pos3d plane2)
	{
		P11.setTo(this).distFromPlane(ORIGIN, plane1, plane2);
		
		/*
		P1.setTo(planePoint1);
		P2.setTo(planePoint2);
		double pp1Ang1 = P1.getAng1();
		
		rotateAroundY(-pp1Ang1);
		P1.rotateAroundY(-pp1Ang1);
		P2.rotateAroundY(-pp1Ang1);
		
		rotateAroundX(Math.atan2(P1.getY(), P1.getZ()));
		P2.rotateAroundX(Math.atan2(P1.getY(), P1.getZ()));
		
		rotateAroundZ(-Math.atan2(P2.getY(), P2.getX()));
		
		mirrorAcrossZX();
		
		rotateAroundZ(Math.atan2(P2.getY(), P2.getX()));
		
		rotateAroundX(-Math.atan2(P1.getY(), P1.getZ()));
		
		rotateAroundY(pp1Ang1);*/
		
		return sub(P11.scale(2));
	}
	
	/**
	 * Projects this onto the XY plane.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoXY()
	{
		return this.setZ(0);
	}

	/**
	 * Projects this onto the YZ plane.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoYZ()
	{
		return this.setX(0);
	}

	/**
	 * Projects this onto the XZ plane.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoXZ()
	{
		return this.setY(0);
	}

	/**
	 * Projects this onto the X-axis.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoX()
	{
		return this.setZ(0).setY(0);
	}

	/**
	 * Projects this onto the Y-axis.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoY()
	{
		return this.setX(0).setZ(0);
	}

	/**
	 * Projects this onto the X-axis.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoZ()
	{
		return this.setY(0).setX(0);
	}

	/**
	 * Projects this onto the arbitrary plane defined by the two parameters and the origin.
	 * @param plane1 First point in the plane that will be projected upon.
	 * @param plane2 Second point in the plane that will be projected upon.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoPlane(Pos3d plane1, Pos3d plane2)
	{
		
		/*
		P1.setTo(plane1);
		P2.setTo(plane2);
		double pp1Ang1 = P1.getAng1();
		
		rotateAroundY(-pp1Ang1);
		P1.rotateAroundY(-pp1Ang1);
		P2.rotateAroundY(-pp1Ang1);
		
		rotateAroundX(Math.atan2(P1.getY(), P1.getZ()));
		P2.rotateAroundX(Math.atan2(P1.getY(), P1.getZ()));
		
		rotateAroundZ(-Math.atan2(P2.getY(), P2.getX()));
		
		setY(0);
		
		rotateAroundZ(Math.atan2(P2.getY(), P2.getX()));
		
		rotateAroundX(-Math.atan2(P1.getY(), P1.getZ()));
		
		rotateAroundY(pp1Ang1);*/
		
		return projectOntoPlane(ORIGIN, plane1, plane2);
	}
	
	/**
	 * Projects this onto the arbitrary plane defined by the three parameters.
	 * @param plane1 First point in the plane that will be projected upon.
	 * @param plane2 Second point in the plane that will be projected upon.
	 * @param plane3 Third point in the plane that will be projected upon.
	 * @return this with updated values.
	 */
	public Pos3d projectOntoPlane(Pos3d plane1, Pos3d plane2, Pos3d plane3)
	{
		P11.setTo(this).distFromPlane(plane1, plane2, plane3);
		
		/*P3.setTo(plane1);
		sub(P3);
		plane2.sub(P3);
		plane3.sub(P3);
		
		projectOntoPlane(plane2, plane3);
		
		add(P3);
		plane2.add(P3);
		plane3.add(P3);*/
		
		return sub(P11);
	}

	/**
	 * Becomes the vector offset from the closest point to this on the plane defined by the two parameters and the origin.
	 * @param plane1 First point in the plane.
	 * @param plane2 Second point in the plane.
	 * @param plane3 Third point in the plane.
	 * @return this with updated values.
	 */
	public Pos3d distFromPlane(Pos3d plane1, Pos3d plane2)
	{
		return distFromPlane(ORIGIN, plane1, plane2);
	}
	
	/**
	 * Becomes the vector offset from the closest point to this on the plane defined by the parameters.
	 * @param plane1 First point in the plane.
	 * @param plane2 Second point in the plane.
	 * @param plane3 Third point in the plane.
	 * @return this with updated values.
	 */
	public Pos3d distFromPlane(Pos3d plane1, Pos3d plane2, Pos3d plane3)
	{
		P1.setTo(plane1);
		P2.setTo(plane2).sub(P1);
		P3.setTo(plane3).sub(P1);
		sub(P1);
		
		P2.crossProduct(P3);
		return projectOnto(P2);
	}
	
	/**
	 * Becomes the vector offset from the closest point to this on the line defined by the parameters.
	 * @param line1 First point in the line.
	 * @param line2 Second point in the line.
	 * @return this with updated values.
	 */
	public Pos3d distFromLine(Pos3d line1, Pos3d line2)
	{
		P1.setTo(line1);
		P2.setTo(line2).sub(P1);
		sub(P1);
		P3.setTo(this).projectOnto(P2);
		return sub(P3);
	}
	
	/**
	 * Becomes the vector offset from the closest point to this on the line segment defined by the parameters.
	 * @param seg1 One end of the line segment.
	 * @param seg2 The other end of the line segment.
	 * @return this with updated values.
	 */
	public Pos3d distFromSegment(Pos3d seg1, Pos3d seg2)
	{
		P1.setTo(seg1);
		P2.setTo(seg2).sub(P1);
		sub(P1);
		P3.setTo(this).projectOnto(P2);
		if (!P3.sameQuadrant(P2))
			return this;
		if (P3.getMagSSqr() > P2.getMagSSqr())
			return sub(P2);
		return sub(P3);
	}
	
	/**
	 * Becomes the vector offset from the closest point to this on the line segment defined by seg and the origin.
	 * @param seg One end of the line segment (the other is the origin).
	 * @return this with updated values.
	 */
	public Pos3d distFromSegment(Pos3d seg)
	{
		P1.setTo(this).projectOnto(seg);
		if (!P1.sameQuadrant(seg))
			return this;
		if (P1.getMagSSqr() > seg.getMagSSqr())
			return sub(seg);
		return sub(P1);
	}
	
	/**
	 * @param other Other Pos3d to be checked with.
	 * @return Whether or not this and other lie in the same quadrant.
	 */
	public boolean sameQuadrant(Pos3d other)
	{
			return Math.signum(getX()) == Math.signum(other.getX()) && Math.signum(y) == Math.signum(other.y) && Math.signum(z) == Math.signum(other.z);
	}
	
	/**
	 * Projects this onto other.
	 * @param onto Vector to be projected upon.
	 * @return this with updated values.
	 */
	public Pos3d projectOnto(Pos3d onto)
	{
		double s = dotProduct(onto) / onto.getMagS();
		return setTo(onto).setMagS(s);
		
	/*	P1.setTo(onto);
		double ang1 = P1.getAng1();
		
		rotateAroundY(-ang1);
		P1.rotateAroundY(-ang1);
		
		double ang2 = Math.atan2(P1.getY(), P1.getZ());
		rotateAroundX(ang2);
		
		double ang3 = Math.atan2(getY(), getX());
		rotateAroundZ(-ang3);
		
		setX(0);
		
		rotateAroundX(-ang2);
		
		rotateAroundY(ang1);
		
		return this;*/
	}
	
	/**
	 * Applies the given transformation to this Pos3d.
	 * @param transformation
	 * @return this with updated values.
	 */
	public Pos3d transform(Transform3d transformation)
	{
		transformation.transform(this);
		return this;
	}
	
	/**
	 * @return The transformations to and from the coordinate space of this Pos3d, respectively.
	 */
	public Transform3d[] getTransforms()
	{
		Transform3d trans = new Transform3d(), back = new Transform3d();
		Matrix m1, m2, m3, m4;
		
		m1 = Matrix.rotation3D(Axis.X, getAngX(), true);
		m2 = Matrix.rotation3D(Axis.Y, getAng1(), true);
		m3 = Matrix.rotation3D(Axis.Z, getAngZ(), true);
		m4 = Matrix.translation(3, getX(), getY(), getZ());
		
		trans.addTransforms(m1, m2, m3, m4);
		back.addTransforms(m4, m3, m2, m1);
		return new Transform3d[] { trans, back };
	}
	
	/**
	 * @param other
	 * @return The dot product of this by other.
	 */
	public double dotProduct(Pos3d other)
	{
		return (getX() * other.getX()) + (getY() * other.getY()) + (getZ() + other.getZ());
	}
	
	/**
	 * @param other
	 * @return The angle between this and other.
	 */
	public double angleBetween(Pos3d other)
	{
		return Math.acos(dotProduct(other) / (getMagS() * other.getMagS()));
	}
	
	/**
	 * Sets this to the cross product of this by other.
	 * @param other
	 * @return this with updated values.
	 */
	public Pos3d crossProduct(Pos3d other)
	{
		return setTo((getY() * other.getZ()) - (getZ() * other.getY()), (getZ() * other.getX()) - (getX() * other.getZ()), (getX() * other.getY()) - (getY() * other.getX()), CARTESIAN);
	}
	
	/**
	 *	Fill the given array with values (a, b, c).
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param array
	 * @return this
	 */
	public Pos3d fillArray(double[] array, int system)
	{
		if (system == CARTESIAN)
		{
			array[0] = getX();
			array[1] = getY();
			array[2] = getZ();
		}
		else if (system == CYLINDRICAL)
		{
			array[0] = getMagC();
			array[1] = getAng1();
			array[2] = getY();
		}
		else if (system == SPHERICAL)
		{
			array[0] = getMagS();
			array[1] = getAng1();
			array[2] = getAng2();
		}
		return this;
	}
	
	/**
	 * The float equivelant of {@linkplain Pos3d#fillArray(double[], int)}.
	 */
	public Pos3d fillArray(float[] array, int system)
	{
		if (system == CARTESIAN)
		{
			array[0] = (float) getX();
			array[1] = (float) getY();
			array[2] = (float) getZ();
		}
		else if (system == CYLINDRICAL)
		{
			array[0] = (float) getMagC();
			array[1] = (float) getAng1();
			array[2] = (float) getY();
		}
		else if (system == SPHERICAL)
		{
			array[0] = (float) getMagS();
			array[1] = (float) getAng1();
			array[2] = (float) getAng2();
		}
		return this;
	}
	
	/**
	 * Create and fill a new array with values (a, b, c).
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param system
	 * @return new double[] { a, b, c }
	 */
	public double[] toArray(int system)
	{
		double[] array = new double[3];
		fillArray(array, system);
		return array;
	}
	
	/**
	 * The float equivelant of {@linkplain Pos3d#toArray(int)}.
	 */
	public float[] toArrayF(int system)
	{
		float[] array = new float[3];
		fillArray(array, system);
		return array;
	}
	
	/**
	 * Uses {@linkplain Math#floor(double)} on the values of this.
	 * @return this with updated values.
	 */
	public Pos3d floor()
	{
		x = Math.floor(x);
		y = Math.floor(y);
		z = Math.floor(z);
		ang1 = Math.floor(ang1);
		ang2 = Math.floor(ang2);
		mag = Math.floor(mag);
		
		return this;
	}
	
	/**
	 * Uses {@linkplain Math#ceil(double)} on the values of this.
	 * @return this with updated values.
	 */
	public Pos3d ceil()
	{
		x = Math.ceil(x);
		y = Math.ceil(y);
		z = Math.ceil(z);
		ang1 = Math.ceil(ang1);
		ang2 = Math.ceil(ang2);
		mag = Math.ceil(mag);
		
		return this;
	}
	
	/**
	 * Uses {@linkplain Math#round(double)} on the values of this.
	 * @return this with updated values.
	 */
	public Pos3d round()
	{
		x = Math.round(x);
		y = Math.round(y);
		z = Math.round(z);
		ang1 = Math.round(ang1);
		ang2 = Math.round(ang2);
		mag = Math.round(mag);
		
		return this;
	}
	
	/**
	 * Switches into relative space, so that further manipulations can be applied as if operating
	 * from the coordinate frame of this. No scaling is applied, only rotation and translation.
	 * This method can be called multiple times, and updates {@linkplain Pos3d#relative} to the
	 * result of {@linkplain Pos3d#unrelative()} in this case. This is equivelant to <code>unrelative().relative()</code>
	 * <p> See also: {@linkplain Pos3d#unrelative()}.
	 * @return this with updated values.
	 */
	public Pos3d relative()
	{
		if (relative != null)
			unrelative();
		relative = this.clean();
		return setTo(0, 0, 0, Pos3d.CARTESIAN);
	}
	
	/**
	 * Switches out from relative space, so that operations performed since {@linkplain Pos3d#relative()} was called
	 * are transformed into the coordinate frame of {@linkplain Pos3d#relative}, as originally set.
	 * @return this with updated values.
	 */
	public Pos3d unrelative()
	{
		if (relative == null)
			return this;
		
	/*	toCartesian();
		double tmp = this.x;
		this.x = this.z;
		this.z = tmp; */
		
		/*double y0 = getAng1();
		double y1 = relative.getAng1();
		double y = relative.getAng1();
		if (!(Math.abs(y) < EQUALS_ERROR))
			rotateAroundY(y);
		toCartesian();
		
		double x = relative.getAngX() - getAngX();
		if (!(Math.abs(x) < EQUALS_ERROR))
			rotateAroundX(x);
		double x2 = getAngX();
		
		double z0 = getAngZ();
		double z1 = relative.getAngZ();
		double z = z1 - z0;
		if (!(Math.abs(z) < EQUALS_ERROR))
			rotateAroundZ(z);
		double z2 = getAngZ();*/
		
		Pos3d y = relative.clean().rotateVertical(QUARTER);
		Pos3d n = y.clean().crossProduct(YP_AXIS);
		double gamma = relative.getAng1() - n.getAng1();
		double beta = QUARTER - y.getAng2();
		double alpha = n.getAng1();

		if (Math.abs(gamma) > EQUALS_ERROR)
			rotateAroundY(gamma);
		if (Math.abs(beta) > EQUALS_ERROR)
			rotateAroundZ(beta);
		if (Math.abs(alpha) > EQUALS_ERROR)
			rotateAroundY(alpha);
		
		add(relative);
		
		relative = null;
		return this;
	}
	
	//////////////////////////////
	
	/**
	 * Writes the {@linkplain Pos3d}s to a new double[] which can be loaded from with {@linkplain Pos3d#fromArrayMany(double[], int, int)}.
	 * @param positions The array of Pos3ds to write.
	 * @param system The system in which to write the Pos3ds.
	 * @return The double[] representing the Pos3d[].
	 */
	public static double[] toArrayMany(Pos3d[] positions, int system)
	{
		double[] manyABC = new double[positions.length * 3];
		for (int i = 0; i < positions.length; i++)
		{
			manyABC[i * 3] = positions[i].toSystem(system).getA();
			manyABC[i * 3 + 1] = positions[i].getB();
			manyABC[i * 3 + 2] = positions[i].getC();
		}
		
		return manyABC;
	}
	
	/**
	 * Writes the {@linkplain Pos3d}s to a new double[] which can be loaded from with {@linkplain Pos3d#fromArrayMany(double[], int)}.
	 * @param positions The array of Pos3ds to write.
	 * @return The double[] representing the Pos3d[].
	 */
	public static double[] toArrayMany(Pos3d[] positions)
	{
		double[] manyABCS = new double[positions.length * 4];
		for (int i = 0; i < positions.length; i++)
		{
			manyABCS[i * 4] = positions[i].getA();
			manyABCS[i * 4 + 1] = positions[i].getB();
			manyABCS[i * 4 + 2] = positions[i].getC();
			manyABCS[i * 4 + 3] = positions[i].system;
		}
		
		return manyABCS;
	}
	
	/**
	 * Writes the {@linkplain Pos3d}s to a new double[] which can be loaded from with {@linkplain Pos3d#fromArrayManyXYZ(double[], int)}.
	 * @param positions The array of Pos3ds to write.
	 * @return The double[] representing the Pos3d[].
	 */
	public static double[] toArrayManyXYZ(Pos3d[] positions)
	{
		double[] manyXYZ = new double[positions.length * 3];
		for (int i = 0; i < positions.length; i++)
		{
			manyXYZ[i * 3] = positions[i].getX();
			manyXYZ[i * 3 + 1] = positions[i].getY();
			manyXYZ[i * 3 + 2] = positions[i].getZ();
		}
		
		return manyXYZ;
	}
	
	/**
	 * @param manyABC The array of doubles from which the Pos3d is constructed. This should be ordered as { a0, b0, c0, a1, b1, c1,.. }.
	 * @param index The index of the Pos3d to construct, as numbered in the above example.
	 * @param system The system to which the doubles will be interpereted.
	 * @return A new {@linkplain Pos3d} constructed from the array of doubles, at the index supplied, in the given system.
	 */
	public static Pos3d fromArrayMany(double[] manyABC, int index, int system)
	{
		return new Pos3d(manyABC[index * 3], manyABC[index * 3 + 1], manyABC[index * 3 + 2], system);
	}
	
	/**
	 * @param manyABCS The array of doubles from which the Pos3d is constructed. This should be ordered as { a0, b0, c0, s0, a1, b1, c1, s1,.. }.
	 * @param index The index of the Pos3d to construct, as numbered in the above example.
	 * @return A new {@linkplain Pos3d} constructed from the array of doubles, at the index supplied, in the system of that index.
	 */
	public static Pos3d fromArrayMany(double[] manyABCS, int index)
	{
		return new Pos3d(manyABCS[index * 4], manyABCS[index * 4 + 1], manyABCS[index * 4 + 2], (int) manyABCS[index * 4 + 3]);
	}
	
	/**
	 * @param manyABC The array of doubles from which the Pos3d is constructed. This should be ordered as { x0, y0, z0, x1, y1, z1,.. }.
	 * @param index The index of the Pos3d to construct, as numbered in the above example.
	 * @return A new {@linkplain Pos3d} constructed from the array of doubles, at the index supplied, in the Cartesian system.
	 */
	public static Pos3d fromArrayManyXYZ(double[] manyXYZ, int index)
	{
		return fromArrayMany(manyXYZ, index, CARTESIAN);
	}
	
	/////////
	
	/**
	 * Uses the given {@linkplain Pos3dVal} to compare the set of positions, and returns the minimum among them.
	 * @param val
	 * @param positions
	 * @return The minimum Pos3d from positions, as determined by comparisons with val.
	 */
	public static Pos3d min(Pos3dVal val, Pos3d... positions)
	{
		Pos3d min = positions[0];
		for (Pos3d pos : positions)
			if (val.val(pos) < val.val(min))
				min = pos;
		return min;
	}

	/**
	 * Uses the given {@linkplain Pos3dVal} to compare the set of positions, and returns the maximum among them.
	 * @param val
	 * @param positions
	 * @return The maximum Pos3d from positions, as determined by comparisons with val.
	 */
	public static Pos3d max(Pos3dVal val, Pos3d... positions)
	{
		Pos3d max = positions[0];
		for (Pos3d pos : positions)
			if (val.val(pos) > val.val(max))
				max = pos;
		
		return max;
	}
	
	/**
	 * Calculates the squared distance between two points.
	 * @param p1 First point
	 * @param p2 Second point
	 * @return The distance squared between the two points.
	 */
	public static double distSqr(Pos3d p1, Pos3d p2)
	{
		return sqr(p1.getX() - p2.getX()) + sqr(p1.getY() - p2.getY()) + sqr(p1.getZ() - p2.getZ());
	}
	
	/**
	 * Calculates the average of the given positions.
	 * @param positions
	 * @return The average point from the set.
	 */
	public static Pos3d average(Pos3d... positions)
	{
		Pos3d av = new Pos3d();
		for (Pos3d pos : positions)
			av.add(pos);
		return av.scale(1d / (double) positions.length);
	}
	
	/**
	 * @param val The {@linkplain Pos3dVal} to use in the {@linkplain Comparator}.
	 * @return A {@linkplain Comparator} to operate on a set of {@linkplain Pos3d}s, using the given {@linkplain Pos3dVal}.
	 */
	public static Comparator<Pos3d> comparator(Pos3dVal val)
	{
		return (a, b) -> Double.compare(val.val(a), val.val(b));
	}

	/**
	 * This {@linkplain Comparator} will impose the opposite ordering from {@linkplain Pos3d#comparator(Pos3dVal)}.
	 * @param val The {@linkplain Pos3dVal} to use in the {@linkplain Comparator}.
	 * @return A {@linkplain Comparator} to operate on a set of {@linkplain Pos3d}s, using the given {@linkplain Pos3dVal}.
	 */
	public static Comparator<Pos3d> comparatorFlipped(Pos3dVal val)
	{
		return (a, b) -> Double.compare(val.val(b), val.val(a));
	}
	
	/**
	 * Sets the {@linkplain Pos3d#EQUALS_ERROR} and {@linkplain Pos3d#EQUALS_ERROR_SQR}.
	 * @param val
	 */
	public static void setEqualsError(double val)
	{
		EQUALS_ERROR = val;
		EQUALS_ERROR_SQR = sqr(val);
	}
	
	/** @return val * val */
	private static double sqr(double val)
	{ return val * val; }
	
	//////////////////////////////////
	
	/**
	 * A function which returns some double value that can be used in a comparison of several {@linkplain Pos3d}s, namely
	 * with {@linkplain Pos3d#comparator(Pos3dVal)}, {@linkplain Pos3d#comparatorFlipped(Pos3dVal)},
	 * {@linkplain Pos3d#min(Pos3dVal, Pos3d...)} and {@linkplain Pos3d#max(Pos3dVal, Pos3d...)}.
	 */
	@FunctionalInterface
	public static interface Pos3dVal
	{
		public double val(Pos3d pos);
	}
}