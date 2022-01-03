/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.model.validation;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.cesecore.internal.InternalResources;
import org.cesecore.util.CertTools;

/**
 * Domain class representing a public key blacklist entry.
 *  
 *
 * @version $Id: PublicKeyBlacklistEntry.java 26302 2017-08-14 14:35:32Z anatom $
 */
public class PublicKeyBlacklistEntry extends BlacklistEntry implements Serializable, Cloneable {

    private static final long serialVersionUID = -315759758359854900L;

    /** Class logger. */
    private static final Logger log = Logger.getLogger(PublicKeyBlacklistEntry.class);

    /** Public key fingerprint digest algorithm. Matching blacklists created by debian:
     * https://launchpad.net/ubuntu/+source/openssl-blacklist/
     * https://launchpad.net/ubuntu/+source/openssh-blacklist
     * https://launchpad.net/ubuntu/+source/openvpn-blacklist/
     * The blacklists contains the SHA1 hash with the first 20 bytes remove, i.e. the last 20 bytes
     */
    public static final String DIGEST_ALGORITHM = "SHA-256";

    public static final String TYPE="PUBLICKEY";

    protected static final InternalResources intres = InternalResources.getInstance();

    /** Public key reference (set while validate). */
    private transient PublicKey publicKey;

    /**
     * Creates a new instance.
     */
    public PublicKeyBlacklistEntry() {
        super(PublicKeyBlacklistEntry.TYPE);
    }

    /**
     * Creates a new instance.
     */
    public PublicKeyBlacklistEntry(int id, String fingerprint, String keyspec) {
        super(id, PublicKeyBlacklistEntry.TYPE, fingerprint, keyspec);
    }

    @Override
    public String getType() {
        return PublicKeyBlacklistEntry.TYPE;
    }


    /**
     * Gets the key spec
     * @return the key spec string (i.e. 'RSA2048', 'secp256r1').
     */
    public String getKeyspec() {
        return getData();
    }

    /**
     * Sets the key spec, RSA2048, secp256r1 etc
     * @param keyspec the key spec string.
     */
    public void setKeyspec(String keyspec) {
        setData(keyspec);
    }
    
    /**
     * Gets the fingerprint.
     * Fingerprint of the key to compare, this is not just a normal fingerprint over the DER encoding
     * For RSA keys this is the hash (see {@link #DIGEST_ALGORITHM}) over the binary bytes of the public key modulus.
     * This because blacklist is typically due to weak random number generator (Debian weak keys) and we then want to capture all keys 
     * generated by this, so we don't want to include the chosen e, only the randomly generated n.
     * For other keys (ECDSA and DSA) it's a fingerprint over the public key encoding.
     * @return fingerprint
     */
    public String getFingerprint() {
        return getValue();
    }

    /**
     * Sets the fingerprint
     * Fingerprint of the key to compare, this is not just a normal fingerprint over the DER encoding
     * For RSA keys this is the hash (see {@link #DIGEST_ALGORITHM}) over the binary bytes of the public key modulus.
     * This because blacklist is typically due to weak random number generator (Debian weak keys) and we then want to capture all keys 
     * generated by this, so we don't want to include the chosen e, only the randomly generated n.
     * For other keys (ECDSA and DSA) it's a fingerprint over the public key encoding.
     * @param fingerprint
     */
    public void setFingerprint(String fingerprint) {
        setValue(fingerprint);
    }

    /** 
     * Sets the fingerprint in the correct format from a public key object
     * see {@link #setFingerprint(String)}
     * @param publicKey an RSA public key
     */
    public void setFingerprint(PublicKey publicKey) {
        setValue(createFingerprint(publicKey));
    }
    
    /** Creates the fingerprint in the correct format from a public key object
     * see {@link #setFingerprint(String)}
     * @param pk a public key, can be RSA, ECDSA or DSA 
     * @return public key fingerprint, as required by Blacklist, or null of no fingerprint can be created (due to unhandled key type for example)
     */
    public static String createFingerprint(PublicKey pk) {
        if (pk == null) {
            return null;
        }
        if (pk instanceof RSAPublicKey) {
            // Fingerprint of the key to compare, this is not just a normal fingerprint over the DER encoding
            // For RSA keys this is the hash (see {@link #DIGEST_ALGORITHM}) over the binary bytes of the public key modulus.
            // This because blacklist is typically due to weak random number generator (Debian weak keys) and we then want to capture all keys 
            // generated by this, so we don't want to include the chosen e, only the randomly generated n.
            RSAPublicKey rsapk = (RSAPublicKey)pk;
            byte[] modulusBytes = rsapk.getModulus().toByteArray();
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(PublicKeyBlacklistEntry.DIGEST_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to create hash "+PublicKeyBlacklistEntry.DIGEST_ALGORITHM, e);
            }
            digest.reset();
            digest.update(modulusBytes);
            final String fingerprint = Hex.toHexString(digest.digest());
            if (log.isTraceEnabled()) {
                log.trace("Created fingerprint for RSA public key: "+fingerprint);
            }
            return fingerprint;
        } else {
            final String fingerprint = CertTools.createPublicKeyFingerprint(pk, PublicKeyBlacklistEntry.DIGEST_ALGORITHM);
            if (log.isTraceEnabled()) {
                log.trace("Created fingerprint for "+pk.getFormat()+" public key: "+fingerprint);
            }
            return fingerprint;
        }
    }
    
    /**
     * Gets the public key, have to be set transient with {@link #setPublicKey(PublicKey)}, not available after serialization
     * or storage.
     * @return the public key.
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Sets the transient public key, see {@link #getPublicKey()}
     * @param publicKey the public key.
     */
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

}
