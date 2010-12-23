package library;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.HashMap;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class SecurityHelper {

    public static final int ITERATION_NUMBER = 3;
    public static final long EXPIRE_SECONDS = 60;

    public SecurityHelper()
    {}

    /**
    * From a password, a number of iterations and a salt,
    * returns the corresponding digest
    * @param iterationNb int The number of iterations of the algorithm
    * @param password String The password to encrypt
    * @param salt byte[] The salt
    * @return byte[] The digested password
    * @throws NoSuchAlgorithmException If the algorithm doesn't exist
    */
    public static byte[] GetHash(int iterationNb, String password, byte[] salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
       MessageDigest digest = MessageDigest.getInstance("SHA-1");
       digest.reset();
       digest.update(salt);

       byte[] input = digest.digest(password.getBytes("UTF-8"));
       for (int i = 0; i < iterationNb; i++) {
           digest.reset();
           input = digest.digest(input);
       }
       return input;
   }

    /**
    * From a password string, return a salted hash of the password
    * @param password String The password to hash
    * @return HashMap["password", "salt"]
    * @throws NoSuchAlgorithmException, UnsupportedEncodingException
    */
   public static HashMap GetHashedPassword(String password)
           throws NoSuchAlgorithmException, UnsupportedEncodingException
   {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        // Salt generation 64 bits long
        byte[] bSalt = new byte[8];
        random.nextBytes(bSalt);

        // Digest computation
        byte[] bDigest = GetHash(ITERATION_NUMBER,password,bSalt);
        
        String sDigest = ByteToBase64(bDigest);
        String sSalt = ByteToBase64(bSalt);

        HashMap retval = new HashMap();
        retval.put("password", sDigest);
        retval.put("salt", sSalt);

        return retval;
   }

    /**
    * From a base 64 representation, returns the corresponding byte[]
    * @param data String The base64 representation
    * @return byte[]
    * @throws IOException
    */
   public static byte[] Base64ToByte(String data) throws IOException {
       BASE64Decoder decoder = new BASE64Decoder();
       return decoder.decodeBuffer(data);
   }

   /**
    * From a byte[] returns a base 64 representation
    * @param data byte[]
    * @return String
    * @throws IOException
    */
   public static String ByteToBase64(byte[] data){
       BASE64Encoder endecoder = new BASE64Encoder();
       return endecoder.encode(data);
   }

   /**
    * From a String returns a sha1 hash
    * @param input String
    * @return String
    * @throws NoSuchAlgorithmException
    */
    public static String MakeSHA1Hash(String input)
		throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.reset();
        byte[] buffer = input.getBytes();
        md.update(buffer);
        byte[] digest = md.digest();

        String hexStr = "";
        for (int i = 0; i < digest.length; i++) {
                hexStr +=  Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return hexStr;
    }

    /**
     * Generate an API authentication signature
     * @param apiId     String  the api id
     * @param userAgent String  the user agent
     * @param timestamp String  the timestamp in format YYYYMMDDHHmmss
     * @return generated sha1 hash of signature
     */
    public static String GenerateSignature(String apiId, String userAgent, String timestamp, String secretKey)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(apiId);
        sb.append(userAgent);
        sb.append(timestamp);
        sb.append(secretKey);

        String signature = HashSha1Base64FromBytes(sb.toString());
        return signature;
    }

    /**
     * Given a String input return a sha1 base-64 hash from binary
     * @param input String the input string
     * @return the hash value
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String HashSha1Base64FromBytes(String input)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();

        byte[] bSignature = digest.digest(input.toString().getBytes("UTF-8"));
        return ByteToBase64(bSignature);
    }
}
