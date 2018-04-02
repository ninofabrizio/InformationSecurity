import javax.crypto.Cipher;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import javax.xml.bind.DatatypeConverter;

import javax.crypto.IllegalBlockSizeException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;
import java.security.InvalidKeyException;

public class MySignature {

	private byte[] dataToSign = null;
	private PublicKey publicKey = null;
	private PrivateKey privateKey = null;
	private byte[] digest = null;

	// Updates dataToSign's content with data
	public void update ( byte[] data ) {
		dataToSign = data;
	}

	// Initializes privateKey for signing
	public void initSign ( PrivateKey key ) {
		privateKey = key;
	}

	// Performs and returns data signed
	public byte[] sign ( String messageDigestAlgorithm, String cipherAlgorithm, int digestByteSize )  {

		// Creating digest
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(messageDigestAlgorithm);
		} catch ( NoSuchAlgorithmException e ) {
			System.err.println("[ERROR-SIGN] Non-existing algorithm for MessageDigest: " + messageDigestAlgorithm);
			System.exit(1);
		}
	    digest = new byte[digestByteSize];
	    md.update(dataToSign);
	    digest = md.digest();
	    System.out.println("Digest in hexadecimal:\n" + DatatypeConverter.printHexBinary(digest));

	    // Creating signature
	    Cipher cipher = null;
	    byte[] signature = null;
	    try {
	    	cipher = Cipher.getInstance(cipherAlgorithm);
	    	cipher.init(Cipher.ENCRYPT_MODE, privateKey);
	    	signature = cipher.doFinal(digest);
	    } catch ( NoSuchAlgorithmException e ) {
	    	System.err.println("[ERROR-SIGN] Non-existing algorithm for Cipher: " + cipherAlgorithm);
			System.exit(1);
	    } catch ( NoSuchPaddingException e ) {
	    	System.err.println("[ERROR-SIGN] Non-existing padding for Cipher: " + cipherAlgorithm);
			System.exit(1);
	    } catch ( BadPaddingException e ) {
	    	System.err.println("[ERROR-SIGN] Bad padding in digest data expected by Cipher");
			System.exit(1);
	    } catch ( IllegalBlockSizeException e ) {
	    	System.err.println("[ERROR-SIGN] Actual digest byte size (" + Integer.toString(digestByteSize) + ") doesn't match with expected by Cipher");
			System.exit(1);
	    } catch ( InvalidKeyException e ) {
	    	System.err.println("[ERROR-SIGN] Invalid key used");
			System.exit(1);
	    }
        
        return signature;
	}

	// Initializes publicKey for verification
	public void initVerify( PublicKey key ) {
		publicKey = key;
	}

	// Verifies the signature using publicKey
	public boolean verify ( byte[] signature, String cipherAlgorithm ) {

		// Creating digest from clear text
		Cipher cipher = null;
		byte[] messageDigest = null;
		try {
	    	cipher = Cipher.getInstance(cipherAlgorithm);
	    	cipher.init(Cipher.DECRYPT_MODE, publicKey);
        	messageDigest = cipher.doFinal(signature);
	    } catch ( NoSuchAlgorithmException e ) {
	    	System.err.println("[ERROR-SIGNING VERIFY] Non-existing algorithm for Cipher: " + cipherAlgorithm);
			System.exit(1);
	    } catch ( InvalidKeyException e ) {
	    	System.err.println("[ERROR-SIGNING VERIFY] Invalid key used");
			System.exit(1);
	    } catch ( IllegalBlockSizeException e ) {
	    	System.err.println("[ERROR-SIGNING VERIFY] Signature byte size (" + Integer.toString(signature.length) + ") doesn't match with expected by Cipher");
			System.exit(1);
	    } catch ( BadPaddingException e ) {
	    	System.err.println("[ERROR-SIGNING VERIFY] Bad padding in signature used in Cipher");
			System.exit(1);
	    } catch ( NoSuchPaddingException e ) {
	    	System.err.println("[ERROR-SIGNING VERIFY] Non-existing padding for Cipher: " + cipherAlgorithm);
			System.exit(1);
	    }

        if(digest.length != messageDigest.length)
        	return false;

        for(int i = 0; i < digest.length; i++)
        	if(digest[i] != messageDigest[i])
        		return false;

		return true;
	}

	public static void main (String[] args) throws Exception {

		// Testing for missing text input
		if(args.length != 1) {
			System.err.println("No message!\nProgram usage: java MySignature \"some text\"");
			System.exit(1);
		}
		byte[] clearText = args[0].getBytes("UTF8");

		MySignature ms = new MySignature();

		// Generating key pair
		System.out.println("Starting key generation\t+++++");
		System.out.println("Building RSA key generator");
		KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
		System.out.println("Key size is 1024 bits");
		keyGenerator.initialize(1024);
		System.out.println("Generating key pair");
		KeyPair keyPair = keyGenerator.generateKeyPair();
		System.out.println("Finished key generation\t-----\n");

		// Generating signature
		System.out.println("Starting signature creation\t+++++");
		System.out.println("Updating clear text data");
		ms.update(clearText);
		System.out.println("Updating private key");
		ms.initSign(keyPair.getPrivate());
		System.out.println("Performing signature in MD5");
		byte[] signature = ms.sign("MD5", "RSA", 16);
		
		System.out.println("Signature in hexadecimal:\n" + DatatypeConverter.printHexBinary(signature));
		System.out.println("Finished signature creation\t-----\n");
	
		// Verifying signature
		System.out.println("Starting signature verification\t+++++");
		System.out.println("Updating public key");
		ms.initVerify(keyPair.getPublic());
		System.out.println("Verifying signature");
		if(ms.verify(signature, "RSA"))
			System.out.println("\tSIGNATURE PASSED");
		else
			System.out.println("\tSIGNATURE FAILED");
		System.out.println("Finished signature verification\t-----\n");
	}
}