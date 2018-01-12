package Hub;

/**
 * Base64Coder
 * 
 * @author À¯½Â¸ñ(handul32@hanmail.net)
 * @version $Id: Base64Coder.java 16247 2011-08-18 04:54:29Z giljae $
 */
@SuppressWarnings("unchecked")
public final class Base64Coder {

	private static final String systemLineSeparator = System.getProperty("line.separator");

	private static char[] map1 = new char[64];

	private static byte[] map2;

	private Base64Coder() {
		throw new AssertionError();
	}

	public static String encodeString(String paramString) {
		return new String(encode(paramString.getBytes()));
	}

	public static String encodeLines(byte[] paramArrayOfByte) {
		return encodeLines(paramArrayOfByte, 0, paramArrayOfByte.length, 76, systemLineSeparator);
	}

	public static String encodeLines(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3,
			String paramString) {
		int i = paramInt3 * 3 / 4;
		if (i <= 0) {
			throw new IllegalArgumentException();
		}
		int j = (paramInt2 + i - 1) / i;
		int k = (paramInt2 + 2) / 3 * 4 + j * paramString.length();
		StringBuilder localStringBuilder = new StringBuilder(k);
		int m = 0;
		while (m < paramInt2) {
			int n = Math.min(paramInt2 - m, i);
			localStringBuilder.append(encode(paramArrayOfByte, paramInt1 + m, n));
			localStringBuilder.append(paramString);
			m += n;
		}
		return localStringBuilder.toString();
	}

	public static char[] encode(byte[] paramArrayOfByte) {
		return encode(paramArrayOfByte, 0, paramArrayOfByte.length);
	}

	public static char[] encode(byte[] paramArrayOfByte, int paramInt) {
		return encode(paramArrayOfByte, 0, paramInt);
	}

	public static char[] encode(byte[] paramArrayOfByte, int paramInt1, int paramInt2) {
		int i = (paramInt2 * 4 + 2) / 3;
		int j = (paramInt2 + 2) / 3 * 4;
		char[] arrayOfChar = new char[j];
		int k = paramInt1;
		int m = paramInt1 + paramInt2;
		int n = 0;
		while (k < m) {
			int i1 = paramArrayOfByte[(k++)] & 0xFF;
			int i2 = k < m ? paramArrayOfByte[(k++)] & 0xFF : 0;
			int i3 = k < m ? paramArrayOfByte[(k++)] & 0xFF : 0;
			int i4 = i1 >>> 2;
			int i5 = (i1 & 0x3) << 4 | i2 >>> 4;
			int i6 = (i2 & 0xF) << 2 | i3 >>> 6;
			int i7 = i3 & 0x3F;
			arrayOfChar[(n++)] = map1[i4];
			arrayOfChar[(n++)] = map1[i5];
			arrayOfChar[n] = (n < i ? map1[i6] : '=');
			n++;
			arrayOfChar[n] = (n < i ? map1[i7] : '=');
			n++;
		}
		return arrayOfChar;
	}

	public static String decodeString(String paramString) {
		return new String(decode(paramString));
	}

	public static byte[] decodeLines(String paramString) {
		char[] arrayOfChar = new char[paramString.length()];
		int i = 0;
		for (int j = 0; j < paramString.length(); j++) {
			int k = paramString.charAt(j);
			if ((k != 32) && (k != 13) && (k != 10) && (k != 9)) {
				arrayOfChar[(i++)] = (char) (k);
			}
		}
		return decode(arrayOfChar, 0, i);
	}

	public static byte[] decode(String paramString) {
		return decode(paramString.toCharArray());
	}

	public static byte[] decode(char[] paramArrayOfChar) {
		return decode(paramArrayOfChar, 0, paramArrayOfChar.length);
	}

	public static byte[] decode(char[] paramArrayOfChar, int paramInt1, int paramInt2) {

		int param2 = paramInt2;
		if (param2 % 4 != 0) {
			throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
		}
		while ((param2 > 0) && (paramArrayOfChar[(paramInt1 + param2 - 1)] == '=')) {
			param2--;
		}
		int i = param2 * 3 / 4;
		byte[] arrayOfByte = new byte[i];
		int j = paramInt1;
		int k = paramInt1 + param2;
		int m = 0;
		while (j < k) {
			int n = paramArrayOfChar[(j++)];
			int i1 = paramArrayOfChar[(j++)];
			int i2 = j < k ? paramArrayOfChar[(j++)] : 65;
			int i3 = j < k ? paramArrayOfChar[(j++)] : 65;
			if ((n > 127) || (i1 > 127) || (i2 > 127) || (i3 > 127)) {
				throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
			}
			int i4 = map2[n];
			int i5 = map2[i1];
			int i6 = map2[i2];
			int i7 = map2[i3];
			if ((i4 < 0) || (i5 < 0) || (i6 < 0) || (i7 < 0)) {
				throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
			}
			int i8 = i4 << 2 | i5 >>> 4;
			int i9 = (i5 & 0xF) << 4 | i6 >>> 2;
			int i10 = (i6 & 0x3) << 6 | i7;
			arrayOfByte[(m++)] = (byte) i8;
			if (m < i) {
				arrayOfByte[(m++)] = (byte) i9;
			}
			if (m < i) {
				arrayOfByte[(m++)] = (byte) i10;
			}
		}
		return arrayOfByte;
	}

	static {
		int i = 0;
		for (int j = 65; j <= 90; j = (char) (j + 1)) {
			map1[(i++)] = (char) j;
		}
		for (int j = 97; j <= 122; j = (char) (j + 1)) {
			map1[(i++)] = (char) j;
		}
		for (int j = 48; j <= 57; j = (char) (j + 1)) {
			map1[(i++)] = (char) j;
		}
		map1[(i++)] = '+';
		map1[(i++)] = '/';
		
		map2 = new byte[1024];

		for (i = 0; i < map2.length; i++) {
			map2[i] = -1;
		}
		for (i = 0; i < 64; i++) {
			map2[map1[i]] = (byte) i;
		}
	}
}