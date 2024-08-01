package com.simibubi.create;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {
	
	public static boolean verifySignature(String data, String sign) {
		try {
			byte[] dataArray = data.getBytes();
			byte[] digitalSignature = backToArray(sign);
			KeyFactory keyFactory = KeyFactory.getInstance("DSA");
			X509EncodedKeySpec keySpec2 = new X509EncodedKeySpec(backToArray(publicKey));
			PublicKey pubKey = keyFactory.generatePublic(keySpec2);
			
			Signature signature = Signature.getInstance("SHA256WithDSA");
			signature.initVerify(pubKey);
			signature.update(dataArray);
			boolean verified = signature.verify(digitalSignature);
			return verified;
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static String decryptAES(String data, byte[] keyArray) {
		byte[] dataArray = backToArray(data);
		try {
			Cipher cip = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] ivByte = new byte[cip.getBlockSize()];
	        IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
	        SecretKeySpec keys = new SecretKeySpec(keyArray, "AES");
	        cip.init(Cipher.DECRYPT_MODE, keys, ivParamsSpec);
			byte[] result = cip.doFinal(dataArray);
			return new String(result);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String encryptAES(String data, byte[] keyArray) {
		byte[] dataArray = data.getBytes();
		try {
			Cipher cip = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] ivByte = new byte[cip.getBlockSize()];
	        IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
	        SecretKeySpec keys = new SecretKeySpec(keyArray, "AES");
	        cip.init(Cipher.ENCRYPT_MODE, keys, ivParamsSpec);
			byte[] result = cip.doFinal(dataArray);
			return getStringKey(result);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static byte[] backToArray(String st) {
		char[] ca = st.toCharArray();
		byte[] ar = new byte[st.length()/2];
		for(int i=0;i<st.length()/2;i++) {
			int val = 0;
			val += toHEXCount(ca[i*2]) * 16;
			val += toHEXCount(ca[i*2+1]);
			ar[i] = (byte)(val-128);
		}
		return ar;
	}
	
	public static String getStringKey(byte[] ar) {
		String res = "";
		for(byte b : ar) {
			res += toHEX(b);
		}
		return res;
	}
	
	private static char[] letters = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'}; 
	
	private static int toHEXCount(char let) {
		for(int i=0;i<16;i++) {
			if(letters[i] == let) return i;
		}
		return -478237483;
	}
	
	private static String toHEX(byte b) {
		int i = b + 128;
		String res = "";
		res += letters[i / 16];
		res += letters[i % 16];
		return res;
	}
	
	public static String publicKey = "B00283C3B00282B58687AA06C84EB88481B00282A882028181800FF9B559392A693F2B6D08FA4FC9D13673AE451EBB2FB798686A44169FBE7DB68667C3D1294498B3B9388967422E9CD31B27C7DB0550912D3834F907F5C904E9DC2C8E0F9433B688A822AF7AA7918ABDE22913C5B489207EE9ECC6D878CB5DA0011CB7892090D731152D4D80A3BD3AD40436A99F1DE40E7803C406F7171C6C8434B4262CAEF56918DD62BD30A9AF41910C1F7A1D010167B30D371237B0573963C9D9AFE88918F295B9956ABDEB0BC6D346B3C50F00BBB224426072F210A5EECEBF0ABB88B82144D064610C9AA923FD5FDE21C35ECBE67F8410BE554F96A361D85407468869FFA19C589D4AA34BEEB887E578A263C23EC8CC85F6B9B9E09C56E7829D803A76162605F8775F5E677AE749F747056FB232B33A6500403C55E9DD820281809626DCD8A0C8D0F0CEF5822317D7848DB45ABAB4F841D454642540ADA4AE60CF16669ECB5010CA3D2C0FB76E31601FB10252BC10C34BE4AF0880C1E06D794A8933A0F6271CB226A772C7BE11071B224467C43DA001D4CC35DB00ACB60D9F28BE540969CE8F20E80EB2C20ADCF844F8460D85A7379C1ABA3B8B8B61ACC4E816B967534EF45B909AE52AAB0776CCE8A65BBE47AFCBD51903CB346D30AFFC106924165325DDD3DB6B7CC554769976BFBD6D3B07B9A54272A460F7B1A9ED28076C9EC7C878FE7BDF5E37D404B1EBA2B25E65D35D2F8291AB8D9F825AB017B2A47EA72E5A0B1DCBA9A2593A0B631E59618326BCD2018B460837626DC396616F975B5E830281868082028181800091E9805DDA33CE7B659CCED1CFB953EF1B70A60B0CFFF84643440F0C5B8262B92A7B4F4FFB2786C931C3ECC67EA79A3B607E971E945C42799551FF3821721A014F47331B8C3A74E9DD30CE13A37DED9B5BA569B87DC15B0E2C1691228AA8B5D6CA35441FADE582A008F697F6873E7BC1204B0EAFF0235B0C60648B2B60C82057E32EA6C33499E43E44E35AF4F0BD9501684CD4D6403A4827C21EB8DEA9840D46E917FED65017B273ACE9216E4A951366AC43AD0AAE7AB8BA19AAF960B5E51F5C53160580CA81045E40E1775F0CACD5A467278047EF4B94A1C55EFED7E9443ADEA3823533E739D4ED0A1A3642591742E07B610E1E2E02C70A10796BF57A8E8A";
	
}
