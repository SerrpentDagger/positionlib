package pos3d;

/**
 * 
 * <p>An instance of this class stores a 2D position, involving 4 doubles, an int, and an uninitialized Pos2d (null).
 * 
 * <p>With a Pos2d object, you can easily perform a wide variety of complex transformations on the 2D position stored within it, all of which function
 * properly on any coordinate system type supported by Pos2d.
 * 
 * <p>All transformation methods are chainable, meaning they return either this, or a new Pos2d, with updated values,
 * depending on whether you use a clean method or not.
 * 
 * <p>By default, transformation methods <b>do</b> alter the values of the Pos2d you call them on (they will <i>never</i> alter the values of parameters).
 * This is very helpful in various situations, and means less object-creation in general.
 * If you do need a new Pos2d, however, there is a "clean" version of every transformation method that
 * will return a new Pos2d with the values that would otherwise have been assigned to this. The new Pos2d will have the checkpoint of this as well.
 * 
 * <p>Checkpoints can be set in the middle of a method chain by calling {@link Pos2d#setCheckpoint()} or {@link Pos2d#setCheckpoint(Pos2d)}
 * on the Pos2d object. You can revert to a set checkpoint by calling {@link Pos2d#revert()} on the Pos2d object. Reverting does not alter the checkpoint.
 * 
 * <p>Methods are well optimized to not undergo unnecessary conversions, and to generally be as efficient as possible. There may be some minor infractions to this
 * statement, but as I update the library I will optimize where needed. If you are concerned with performance, then I would suggest performing all of your
 * transformations in groups corresponding to the coordinate system required by those transformations. This would remove unnecessary conversions.
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
public class Pos2d
{
	public static final int CARTESIAN = 0, POLAR = 1;
	public static final int QPP = 1, QNP = 2, QNN = 3, QPN = 4;
	
	private double x = 0, y = 0, ang = 0, mag = 0;
	private int system = -1;
	public Pos2d checkpoint;
	
	/** 
	 * New Alt3d with cartesian values x, y, z.
	 * @param x
	 * @param y
	 * @param z 
	 */
	public Pos2d(double x, double y)
	{
		this.x = x;
		this.y = y;
		system = CARTESIAN;
	}
	
	/**
	 * New Alt3d with values for specified system.
	 * <p>If system is...
	 * <p>Cartesian, (a, b, c) -> (x, y, z)
	 * <p>Spherical, (a, b, c) -> (spherical magnitude, rotation around y-axis, rotation from horizontal)
	 * <p>Cylindrical, (a, b, c) -> (cylindrical magnitude, rotation around y-axis, y)
	 * @param a
	 * @param b
	 * @param c
	 * @param system
	 */
	public Pos2d(double a, double b, int system)
	{
		if (system == CARTESIAN)
		{
			this.x = a;
			this.y = b;
		}
		else if (system == POLAR)
		{
			this.ang = a;
			this.mag = b;
		}
		this.system = system;
	}

	public Pos2d()
	{
	}

	/**
	 * Creates a clone of toClone, including the checkpoint if !null.
	 * @param toClone
	 */
	public Pos2d(Pos2d toClone)
	{
		if (toClone == null) { return; }
		this.x = toClone.x;
		this.y = toClone.y;
		this.ang = toClone.ang;
		this.mag = toClone.mag;
		this.system = toClone.system;
		if (toClone.checkpoint != null) { checkpoint = new Pos2d().setTo(toClone.checkpoint); }
	}
	
	/**
	 * Convert to Cartesian coordinates.
	 * @return this with updated values.
	 */
	public Pos2d toCartesian()
	{
		if (system == POLAR)
		{
			x = mag * Math.cos(ang);
			y = mag * Math.sin(ang);
		}
		system = CARTESIAN;
		return this;
	}
	
	/**
	 * Convert to Polar coordinates.
	 * @return this with updated values.
	 */
	public Pos2d toPolar()
	{
		if (system == CARTESIAN)
		{
			mag = Math.sqrt((x*x) + (y*y));
			ang = Math.atan2(y, x);
		}
		system = POLAR;
		return this;
	}
	
	/**
	 * Convert to system.
	 * @param system System to convert to.
	 * @return this with updated values.
	 */
	public Pos2d toSystem(int system)
	{
		if (system == CARTESIAN)
		{
			toCartesian();
		}
		else
		{
			toPolar();
		}
		return this;
	}

	/**
	 * Converts to Cartesian, adds toAdd to this, and converts back to the inputted system.
	 * @param toAdd Alt3d to add to this.
	 * @return Cartesian addition of toAdd onto this, in this's origional system.
	 */
	public Pos2d add(Pos2d toAdd)
	{
		toCartesian().x += toAdd.toCartesian().x;
		y += toAdd.y;
		
		return this;
	}

	/**
	 * Calls the add method, using a new Alt3d built from the parameters.
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
	public Pos2d add(double a, double b, int system)
	{
		Pos2d temp = new Pos2d(a, b, system);
		
		return add(temp);
	}

	/**
	 * Converts to Cartesian, subtracts toSub from this, and converts back to the inputted system.
	 * @param toSub Alt3d to subtract from this.
	 * @return Cartesian subtraction of toSub from this, in this's original system.
	 */
	public Pos2d sub(Pos2d toSub)
	{
		toCartesian().x -= toSub.toCartesian().x;
		y -= toSub.y;
		
		return this;
	}

	/**
	 * Calls the sub method, using a new Alt3d built from the parameters.
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
	public Pos2d sub(double a, double b, int system)
	{
		Pos2d temp = new Pos2d(a, b, system);
		
		return sub(temp);
	}

	/**
	 * Converts to Cartesian, multiplies this by toMult (x by x, y by y, z by z), and converts back to the inputted system.
	 * @param toMult Alt3d to multiply this by.
	 * @return Cartesian multiplication of this by toMult, in this's original system.
	 */
	public Pos2d mult(Pos2d toMult)
	{
		toCartesian().x *= toMult.toCartesian().x;
		y *= toMult.y;
		
		return this;
	}

	/**
	 * Calls the mult method, using a new Alt3d built from the parameters.
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
	public Pos2d mult(double a, double b, int system)
	{
		Pos2d temp = new Pos2d(a, b, system);
		
		return mult(temp);
	}

	/**
	 * Converts to Cartesian, divides this by toDivi (x by x, y by y, z by z), and converts back to the inputted system.
	 * @param toDivi Alt3d to divide this by.
	 * @return Cartesian division of this by toDivi, in this's original system.
	 */
	public Pos2d divi(Pos2d toDivi)
	{
		toCartesian().x /= toDivi.toCartesian().x;
		y /= toDivi.toCartesian().y;
		
		return this;
	}

	/**
	 * Calls the divi method, using a new Alt3d built from the parameters.
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
	public Pos2d divi(double a, double b, int system)
	{
		Pos2d temp = new Pos2d(a, b, system);
		
		return divi(temp);
	}

	/**
	 * Scales this by scale.
	 * @param scale Scale to multiply this by.
	 * @return Scaling of this by scale.
	 */
	public Pos2d scale(double scale)
	{
		if (system == CARTESIAN)
		{
			x *= scale;
			y *= scale;
		}
		else
		{
			mag *= scale;
		}
		return this;
	}

	/**
	 * Converts to Cartesian, squares the individual (x, y, z) values, and converts back to inputted system.
	 * @return Cartesian square of this, in this's original system.
	 */
	public Pos2d sqr()
	{
		toCartesian();
		x *= x;
		y *= y;
		
		return this;
	}

	/**
	 * Converts to Cartesian, takes the square root of the individual (x, y, z) values, and converts back to inputted system.
	 * @return Cartesian square root of this, in this's original system.
	 */
	public Pos2d sqrt()
	{
		toCartesian();
		x = Math.sqrt(x);
		y = Math.sqrt(y);
		
		return this;
	}

	/**
	 * Converts to Cartesian, takes the square root of the individual (x, y, z) values, and converts back to inputted system.
	 * <p>Always returns positive values.
	 * @return Positive Cartesian square root of this, in this's original system.
	 */
	public Pos2d sqrtPositive()
	{
		toCartesian();
		x = (x < 0) ? Math.sqrt(-x) : Math.sqrt(x);
		y = (y < 0) ? Math.sqrt(-y) : Math.sqrt(y);
		
		return this;
	}

	/**
	 * Converts to Cartesian, squares the individual (x, y, z) values while preserving sign, and converts back to inputted system.
	 * @return Cartesian square of this, preserving signs, in this's original system.
	 */
	public Pos2d sqrPreserveSign()
	{
		toCartesian();
		x *= (x < 0) ? -x : x;
		y *= (y < 0) ? -y : y;
		
		return this;
	}

	/**
	 * Converts to Cartesian, takes the square root of the individual (x, y, z) values while preserving sign, and converts back to inputted system.
	 * @return Cartesian square root of this, preserving signs, in this's original system.
	 */
	public Pos2d sqrtPreserveSign()
	{
		toCartesian();
		x = (x < 0) ? -Math.sqrt(-x) : Math.sqrt(x);
		y = (y < 0) ? -Math.sqrt(-y) : Math.sqrt(y);
		
		return this;
	}
	
	public Pos2d setTo(Pos2d setTo)
	{
		if (setTo == null) { return null; }
		this.x = setTo.x;
		this.y = setTo.y;
		this.ang = setTo.ang;
		this.mag = setTo.mag;
		this.system = setTo.system;
		
		return this;
	}

	/**
	 * Set values of this to values of setTo (checkpoint not included).
	 * @param setTo Values to put into this.
	 * @return this with updated values.
	 */
	public Pos2d setTo(double a, double b, int system)
	{
		if (system == CARTESIAN)
		{
			x = a;
			y = b;
		}
		else
		{
			ang = a;
			mag = b;
		}
		this.system = system;
		
		return this;
	}
	
	/**
	 * Rotates the Pos2d counter-clockwise around the origin.
	 * 
	 * @param radians Radian value to rotate by.
	 * @return this with updated values.
	 */
	public Pos2d rotateAroundOrigin(double radians)
	{
		toPolar();
		ang += radians;
		
		return this;
	}
	
	/**
	 * Rotates the Pos2d counter-clockwise around an arbitrary point.
	 * @param point Arbitrary point to rotate around.
	 * @param radians Radian value to rotate by.
	 * @return this with updated values.
	 */
	public Pos2d rotateAroundPoint(Pos2d point, double radians)
	{
		sub(point);
		rotateAroundOrigin(radians);
		add(point);
		
		return this;
	}
	
	/**
	 * Mirrors the Pos2d across the Y-axis.
	 * @return this with updated values.
	 */
	public Pos2d mirrorAcrossY()
	{
		if (system == CARTESIAN)
		{
			x = -x;
		}
		else if (system == POLAR)
		{
			ang = -(ang + Math.PI);
		}
		return this;
	}
	
	/**
	 * Mirrors the Pos2d across the X-axis.
	 * @return this with updated values.
	 */
	public Pos2d mirrorAcrossX()
	{
		if (system == CARTESIAN)
		{
			y = -y;
		}
		else if (system == POLAR)
		{
			ang = -ang;
		}
		return this;
	}
	
	/**
	 * Mirrors the Pos2d across an arbitrary line.
	 * @param line One point defining the line to mirror across. The other is the origin.
	 * @return
	 */
	public Pos2d mirrorAcrossLine(Pos2d line)
	{
		double tempAng = line.getAng();
		rotateAroundOrigin(-tempAng);
		mirrorAcrossX();
		return rotateAroundOrigin(tempAng);
	}
	
	/**
	 * Mirrors the Pos2d across an arbitrary line.
	 * @param line1 One point defining the line to mirror across.
	 * @param line2 The other point defining the line to mirror across.
	 * @return
	 */
	public Pos2d mirrorAcrossLine(Pos2d line1, Pos2d line2)
	{
		sub(line1);
		line2.sub(line1);
		rotateAroundOrigin(-line2.getAng());
		mirrorAcrossX();
		rotateAroundOrigin(line2.ang);
		add(line1);
		return this;
	}
	
	/**
	 * Converts angles to radians (Note, most transformations REQUIRE angles to be in radians. It is not recommended to ever have angles in degrees).
	 * @return this with updated values.
	 */
	public Pos2d toRad()
	{
		ang = ang * Math.PI / 180;
		return this;
	}
	
	/**
	 * Sets this's checkpoint to this.
	 * @return this.
	 */
	public Pos2d setCheckpoint()
	{
		checkpoint = new Pos2d().setTo(this);
		
		return this;
	}

	/**
	 * Sets this's checkpoint to checkpoint.
	 * @param checkpoint Alt3d to become this's checkpoint.
	 * @return this.
	 */
	public Pos2d setCheckpoint(Pos2d checkpoint)
	{
		this.checkpoint = new Pos2d().setTo(checkpoint);
		return this;
	}

	/**
	 * Reverts this to state saved in checkpoint (does not revert checkpoint).
	 * @return this with values of checkpoint.
	 */
	public Pos2d revert()
	{
		return setTo(checkpoint);
	}
	
	/**
	 * @return The Cartesian x of this Alt3d.
	 */
	public double getX()
	{
		return toCartesian().x;
	}

	/**
	 * @return The Cartesian y of this Alt3d.
	 */
	public double getY()
	{
		return toCartesian().y;
	}
	
	/**
	 * @return The rotation around the origin of this Alt3d.
	 */
	public double getAng()
	{
		return toPolar().ang;
	}
	
	/**
	 * @return The Polar magnitude of this Alt3d;
	 */
	public double getMag()
	{
		return toPolar().mag;
	}
	
	/**
	 * @return The current coordinate system of this Alt3d.
	 */
	public int getSystem()
	{
		return system;
	}
	
	/**
	 * Sets the Cartesian x value of this, and converts back to this's original system.
	 * @param x Value to set x to.
	 * @return this with updated values, in this's original system.
	 */
	public Pos2d setX(double x)
	{
		toCartesian().x = x;
		return this;
	}

	/**
	 * Sets the Cartesian y value of this, and converts back to this's original system.
	 * @param y Value to set y to.
	 * @return this with updated values, in this's original system.
	 */
	public Pos2d setY(double y)
	{
		toCartesian().y = y;
		return this;
	}
	
	/**
	 * Sets the Polar angle of this.
	 * @param radians Angle in radians to set ang to.
	 */
	public Pos2d setAng(double radians)
	{
		toPolar().ang = ang;
		return this;
	}
	
	/**
	 * Sets the Polar magnitude of this.
	 * @param mag Magnitude to set mag to.
	 */
	public Pos2d setMag(double mag)
	{
		toPolar().mag = mag;
		return this;
	}

	/**
	 * Flips this across the origin.
	 * @return this with updated values.
	 */
	public Pos2d flip()
	{
		return scale(-1);
	}

	/**
	 * Flips this across the origin and doubles.
	 * @return this with updated values.
	 */
	public Pos2d flipAndDouble()
	{
		return scale(-2);
	}

	/**
	 * Convenient addition of this onto this.
	 * @return this with updated values.
	 */
	public Pos2d doublePos()
	{
		return scale(2);
	}
	
	public Pos2d projectOntoX()
	{
		toCartesian().y = 0;
		return this;
	}
	
	public Pos2d projectOntoY()
	{
		toCartesian().x = 0;
		return this;
	}
	
	public Pos2d projectOnto(Pos2d toProjectOnto)
	{
		double tempAng = toProjectOnto.getAng();
		rotateAroundOrigin(-tempAng);
		projectOntoX();
		rotateAroundOrigin(tempAng);
		return this;
	}
	
	public Pos2d toCartesianClean()
	{
		return new Pos2d(this).toCartesian();
	}
	
	public Pos2d toPolarClean()
	{
		return new Pos2d(this).toPolar();
	}
	
	public Pos2d toSystemClean(int system)
	{
		return new Pos2d(this).toSystem(system);
	}
	
	public Pos2d addClean(Pos2d toAdd)
	{
		return new Pos2d(this).add(toAdd);
	}
	
	public Pos2d addClean(double a, double b, int system)
	{
		return addClean(new Pos2d(a, b, system));
	}
	
	public Pos2d subClean(Pos2d toSub)
	{
		return new Pos2d(this).sub(toSub);
	}
	
	public Pos2d subClean(double a, double b, int system)
	{
		return subClean(new Pos2d(a, b, system));
	}
	
	public Pos2d multClean(Pos2d toMult)
	{
		return new Pos2d(this).mult(toMult);
	}
	
	public Pos2d multClean(double a, double b, int system)
	{
		return multClean(new Pos2d(a, b, system));
	}
	
	public Pos2d diviClean(Pos2d toDivi)
	{
		return new Pos2d(this).divi(toDivi);
	}
	
	public Pos2d diviClean(double a, double b, int system)
	{
		return diviClean(new Pos2d(a, b, system));
	}
	
	public Pos2d scaleClean(double scale)
	{
		return new Pos2d(this).scale(scale);
	}
	
	public Pos2d sqrClean()
	{
		return new Pos2d(this).sqr();
	}
	
	public Pos2d sqrtClean()
	{
		return new Pos2d(this).sqrt();
	}
	
	public Pos2d sqrtPositiveClean()
	{
		return new Pos2d(this).sqrtPositive();
	}
	
	public Pos2d sqrPreserveSignClean()
	{
		return new Pos2d(this).sqrPreserveSign();
	}
	
	public Pos2d sqrtPreserveSignClean()
	{
		return new Pos2d(this).sqrtPreserveSign();
	}
	
	public Pos2d setToClean(Pos2d setTo)
	{
		return new Pos2d(this).setTo(setTo);
	}
	
	public Pos2d setToClean(double a, double b, int system)
	{
		return new Pos2d(this).setTo(a, b, system);
	}
	
	public Pos2d rotateAroundOriginClean(double radians)
	{
		return new Pos2d(this).rotateAroundOrigin(radians);
	}
	
	public Pos2d rotateAroundPointClean(Pos2d point, double radians)
	{
		return new Pos2d(this).rotateAroundPoint(point, radians);
	}
	
	public Pos2d mirrorAcrossYClean()
	{
		return new Pos2d(this).mirrorAcrossY();
	}
	
	public Pos2d mirrorAcrossXClean()
	{
		return new Pos2d(this).mirrorAcrossX();
	}
	
	public Pos2d mirrorAcrossLineClean(Pos2d line)
	{
		return new Pos2d(this).mirrorAcrossLine(line);
	}
	
	public Pos2d mirrorAcrossLineClean(Pos2d line1, Pos2d line2)
	{
		return new Pos2d(this).mirrorAcrossLine(line1, line2);
	}
	
	public Pos2d projectOntoXClean()
	{
		return new Pos2d(this).projectOntoX();
	}
	
	public Pos2d projectOntoYClean()
	{
		return new Pos2d(this).projectOntoY();
	}
	
	public Pos2d projectOntoClean(Pos2d toProjectOnto)
	{
		return new Pos2d(this).projectOnto(toProjectOnto);
	}
	
	public Pos2d setCheckpointClean()
	{
		return new Pos2d(this).setCheckpoint();
	}
	
	public Pos2d setCheckpointClean(Pos2d checkpoint)
	{
		return new Pos2d(this).setCheckpoint(checkpoint);
	}
	
	public Pos2d revertClean()
	{
		return new Pos2d(this).revert();
	}
	
	public Pos2d setXClean(double x)
	{
		return new Pos2d(this).setX(x);
	}
	
	public Pos2d setYClean(double y)
	{
		return new Pos2d(this).setY(y);
	}
	
	public Pos2d setAngClean(double radians)
	{
		return new Pos2d(this).setAng(radians);
	}
	
	public Pos2d setMagClean(double mag)
	{
		return new Pos2d(this).setMag(mag);
	}
	
	public Pos2d flipClean()
	{
		return new Pos2d(this).flip();
	}
	
	public Pos2d flipAndDoubleClean()
	{
		return new Pos2d(this).flipAndDouble();
	}
	
	public Pos2d doublePosClean()
	{
		return new Pos2d(this).doublePos();
	}
	
	public int getQuadrandZP()
	{
		toCartesian();
		if (x < 0)
		{
			if (y < 0)
			{
				return QNN;
			}
			else return QNP;
		}
		else if (y < 0)
		{
			return QPN;
		}
		else return QPP;
	}
	
	public int getQuadrandZN()
	{
		toCartesian();
		if (x <= 0)
		{
			if (y <= 0)
			{
				return QNN;
			}
			else return QNP;
		}
		else if (y <= 0)
		{
			return QPN;
		}
		else return QPP;
	}
	
	@Override
	public String toString()
	{
		return "{" + x + " " + y + " " + mag + " "
				 + ang + " " + system + " " + ((checkpoint == null) ? "null" : checkpoint.toString()) + "}";
	}
	
	/**
	 * @return The "programmer friendly" String representing this Pos2d, including names, colons, and commas separating values.
	 */
	public String toStringPF()
	{
		return "{X: " + x + ", Y: " + y + ", mag: " + mag + ","
				+ "ang: " + ang + ", system: " + system + ", checkpoint: " + ((checkpoint == null) ? "null" : checkpoint.toStringPF()) + "}";
	}
}
