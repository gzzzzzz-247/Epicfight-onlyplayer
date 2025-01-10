package yesman.epicfight.api.utils.math;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MathUtils {
	public static OpenMatrix4f getModelMatrixIntegral(float xPosO, float xPos, float yPosO, float yPos, float zPosO, float zPos, float xRotO, float pitch, float yRotO, float yRot, float partialTick, float scaleX, float scaleY, float scaleZ) {
		OpenMatrix4f modelMatrix = new OpenMatrix4f();
		Vec3f translation = new Vec3f(-(xPosO + (xPos - xPosO) * partialTick), ((yPosO + (yPos - yPosO) * partialTick)), -(zPosO + (zPos - zPosO) * partialTick));
		float partialXRot = lerpBetween(xRotO, pitch, partialTick);
		float partialYRot = lerpBetween(yRotO, yRot, partialTick);
		modelMatrix.translate(translation).rotateDeg(-partialYRot, Vec3f.Y_AXIS).rotateDeg(-partialXRot, Vec3f.X_AXIS).scale(scaleX, scaleY, scaleZ);
		
		return modelMatrix;
	}
	
	/**
	 * Blender 2.79 bezier curve
	 * @param t: 0 ~ 1
	 * @retur
	 */
	public static double bezierCurve(double t) {
		double p1 = 0.0D;
		double p2 = 0.0D;
		double p3 = 1.0D;
		double p4 = 1.0D;
		double v1, v2, v3, v4;
		
		v1 = p1;
		v2 = 3.0D * (p2 - p1);
		v3 = 3.0D * (p1 - 2.0D * p2 + p3);
		v4 = p4 - p1 + 3.0D * (p2 - p3);
		
		return v1 + t * v2 + t * t * v3 + t * t * t * v4;
	}
	
	public static Vec3 getVectorForRotation(float pitch, float yaw) {
		float f = pitch * ((float) Math.PI / 180F);
		float f1 = -yaw * ((float) Math.PI / 180F);
		float f2 = Mth.cos(f1);
		float f3 = Mth.sin(f1);
		float f4 = Mth.cos(f);
		float f5 = Mth.sin(f);

		return new Vec3(f3 * f4, -f5, f2 * f4);
	}
	
	public static float lerpBetween(float f1, float f2, float zero2one) {
		float f = 0;

		for (f = f2 - f1; f < -180.0F; f += 360.0F) {
		}

		while (f >= 180.0F) {
			f -= 360.0F;
		}

		return f1 + zero2one * f;
	}
	
	public static float rotlerp(float from, float to, float limit) {
		float f = Mth.wrapDegrees(to - from);
		
		if (f > limit) {
			f = limit;
		}
		
		if (f < -limit) {
			f = -limit;
		}
		
		float f1 = from + f;
		
		while (f1 >= 180.0F) {
			f1 -= 360.0F;
		}
		
		while (f1 <= -180.0F) {
			f1 += 360.0F;
		}
		
		return f1;
	}

	public static float rotWrap(double d) {
		while (d >= 180.0) {
			d -= 360.0;
		}
		while (d < -180.0) {
			d += 360.0;
		}
		return (float)d;
	}

	public static void translateStack(PoseStack poseStack, OpenMatrix4f mat) {
		Vector3f vector = new Vector3f(mat.m30, mat.m31, mat.m32);
		poseStack.translate(vector.x(), vector.y(), vector.z());
	}
	
	public static void rotateStack(PoseStack poseStack, OpenMatrix4f mat) {
		poseStack.mulPose(getQuaternionFromMatrix(mat));
	}
	
	public static void scaleStack(PoseStack poseStack, OpenMatrix4f mat) {
		Vector3f vector = getScaleVectorFromMatrix(mat);
		poseStack.scale(vector.x(), vector.y(), vector.z());
	}
	
	public static double getAngleBetween(Vec3f a, Vec3f b) {
		double cos = (a.x * b.x + a.y * b.y + a.z * b.z);
		return Math.acos(cos);
	}
	
	public static double getAngleBetween(Vec3 a, Vec3 b) {
		double cos = (a.x * b.x + a.y * b.y + a.z * b.z);
		return Math.acos(cos);
	}
	
	public static double getXRotOfVector(Vec3 vec) {
		Vec3 normalized = vec.normalize();
		return -(Math.atan2(normalized.y, (float)Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z)) * (180D / Math.PI));
	}
	
	public static double getYRotOfVector(Vec3 vec) {
		Vec3 normalized = vec.normalize();
		return Math.atan2(normalized.z, normalized.x) * (180D / Math.PI) - 90.0F;
	}
	
	private static Quaternionf getQuaternionFromMatrix(OpenMatrix4f mat) {
		Quaternionf quat = new Quaternionf(0, 0, 0, 1);
		quat.setFromUnnormalized(OpenMatrix4f.exportToMojangMatrix(mat.transpose(null)));
		return quat;
	}
	
	public static Vec3f lerpVector(Vec3f start, Vec3f end, float weight) {
		float x = start.x + (end.x - start.x) * weight;
		float y = start.y + (end.y - start.y) * weight;
		float z = start.z + (end.z - start.z) * weight;
		return new Vec3f(x, y, z);
	}
	
	public static Vector3f lerpMojangVector(Vector3f start, Vector3f end, float weight) {
		float x = start.x() + (end.x() - start.x()) * weight;
		float y = start.y() + (end.y() - start.y()) * weight;
		float z = start.z() + (end.z() - start.z()) * weight;
		return new Vector3f(x, y, z);
	}
	
	public static Vec3 projectVector(Vec3 from, Vec3 to) {
		double dot = to.dot(from);
		double normalScale = 1.0D / ((to.x * to.x) + (to.y * to.y) + (to.z * to.z));
		return new Vec3(dot * to.x * normalScale, dot * to.y * normalScale, dot * to.z * normalScale);
	}
	
	public static void setQuaternion(Quaternionf quat, float x, float y, float z, float w) {
		quat.set(x, y, z, w);
	}
	
	public static Quaternionf mulQuaternion(Quaternionf left, Quaternionf right, Quaternionf dest) {
		if (dest == null) {
			dest = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
		}
		
		float f = left.x();
	    float f1 = left.y();
	    float f2 = left.z();
	    float f3 = left.w();
	    float f4 = right.x();
	    float f5 = right.y();
	    float f6 = right.z();
	    float f7 = right.w();
	    float i = f3 * f4 + f * f7 + f1 * f6 - f2 * f5;
	    float j = f3 * f5 - f * f6 + f1 * f7 + f2 * f4;
	    float k = f3 * f6 + f * f5 - f1 * f4 + f2 * f7;
	    float r = f3 * f7 - f * f4 - f1 * f5 - f2 * f6;
	    
	    dest.set(i, j, k, r);
	    
	    return dest;
	}
	
	public static Quaternionf lerpQuaternion(Quaternionf from, Quaternionf to, float lerpAmount) {
		float fromX = from.x();
		float fromY = from.y();
		float fromZ = from.z();
		float fromW = from.w();
		float toX = to.x();
		float toY = to.y();
		float toZ = to.z();
		float toW = to.w();
		float resultX;
		float resultY;
		float resultZ;
		float resultW;
		float dot = fromW * toW + fromX * toX + fromY * toY + fromZ * toZ;
		float blendI = 1.0F - lerpAmount;
		
		if (dot < 0.0F) {
			resultW = blendI * fromW + lerpAmount * -toW;
			resultX = blendI * fromX + lerpAmount * -toX;
			resultY = blendI * fromY + lerpAmount * -toY;
			resultZ = blendI * fromZ + lerpAmount * -toZ;
		} else {
			resultW = blendI * fromW + lerpAmount * toW;
			resultX = blendI * fromX + lerpAmount * toX;
			resultY = blendI * fromY + lerpAmount * toY;
			resultZ = blendI * fromZ + lerpAmount * toZ;
		}

		Quaternionf result = new Quaternionf(resultX, resultY, resultZ, resultW);
		normalizeQuaternion(result);
		return result;
	}
	
	private static void normalizeQuaternion(Quaternionf quaternion) {
		float f = quaternion.x() * quaternion.x() + quaternion.y() * quaternion.y() + quaternion.z() * quaternion.z() + quaternion.w() * quaternion.w();
		if (f > 1.0E-6F) {
			float f1 = fastInvSqrt(f);
			setQuaternion(quaternion, quaternion.x() * f1, quaternion.y() * f1, quaternion.z() * f1, quaternion.w() * f1);
		} else {
			setQuaternion(quaternion, 0.0F, 0.0F, 0.0F, 0.0F);
		}
	}
	
	private static Vector3f getScaleVectorFromMatrix(OpenMatrix4f mat) {
		Vec3f a = new Vec3f(mat.m00, mat.m10, mat.m20);
		Vec3f b = new Vec3f(mat.m01, mat.m11, mat.m21);
		Vec3f c = new Vec3f(mat.m02, mat.m12, mat.m22);
		return new Vector3f(a.length(), b.length(), c.length());
	}
	
	private static float fastInvSqrt(float number) {
		float f = 0.5F * number;
		int i = Float.floatToIntBits(number);
		i = 1597463007 - (i >> 1);
		number = Float.intBitsToFloat(i);
		return number * (1.5F - f * number * number);
	}
}