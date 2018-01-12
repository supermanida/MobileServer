package Hub;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 암호화, 복호화 처리 클래스
 * 
 * @author 유승목(handul32@hanmail.net)
 * @version $Id: EncryptUtil.java 16247 2011-08-18 04:54:29Z giljae $
 */
@SuppressWarnings("unchecked")
public final class EncryptUtil {

	public static final String ENCRYPT_ALGORITHM = "AES";

	public static final String ENCRYPT_KEY = "ThisIsIkepSecurityKey";
	
	private EncryptUtil() {
		throw new AssertionError();
	}

	/**
	 * 대칭키 암호화
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static String encryptText(String text) {
		String encrypted;

		try {
			SecretKeySpec ks = new SecretKeySpec(generateKey(ENCRYPT_KEY), ENCRYPT_ALGORITHM);
			Cipher cipher = Cipher.getInstance(ENCRYPT_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, ks);
			byte[] encryptedBytes = cipher.doFinal(text.getBytes());
			encrypted = new String(Base64Coder.encode(encryptedBytes));
		} catch (Exception e) {
			encrypted = text;
			e.printStackTrace();
		}

		return encrypted;
	}

	/**
	 * 대칭키 복호화
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static String decryptText(String text) {
		String decrypted;

		try {
			SecretKeySpec ks = new SecretKeySpec(generateKey(ENCRYPT_KEY), ENCRYPT_ALGORITHM);
			Cipher cipher = Cipher.getInstance(ENCRYPT_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, ks);
			byte[] decryptedBytes = cipher.doFinal(Base64Coder.decode(text));
			decrypted = new String(decryptedBytes);
		} catch (Exception e) {
			decrypted = "";
			e.printStackTrace();
		}

		return decrypted;
	}

	/**
	 * 대칭키 암호화
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	private static byte[] generateKey(String key) {
		byte[] desKey = new byte[16];
		byte[] bkey = key.getBytes();

		if (bkey.length < desKey.length) {
			System.arraycopy(bkey, 0, desKey, 0, bkey.length);

			for (int i = bkey.length; i < desKey.length; i++) {
				desKey[i] = 0;
			}
		} else {
			System.arraycopy(bkey, 0, desKey, 0, desKey.length);
		}

		return desKey;
	}

	/**
	 * 비대칭키 SHA 암호화 (복호화 되지 않음)
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static String encryptSha(String text) {
		String encrypted;

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(text.getBytes("UTF-8"));

			byte[] digested = md.digest();
			encrypted = new String(Base64Coder.encode(digested));
		} catch (Exception e) {
			encrypted = "";
			e.printStackTrace();
		}

		return encrypted;
	}

	/**
	 * 비대칭키 MD5 암호화 (복호화 되지 않음)
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static String encryptMd5(String text) {
		String encrypted;

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes("UTF-8"));

			byte[] digested = md.digest();
			encrypted = new String(Base64Coder.encode(digested));
		} catch (Exception e) {
			encrypted = "";
			e.printStackTrace();
		}

		return encrypted;
	}

}
