package com.github.steveice10.mc.protocol.packet.legacy;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;

public class LegacyEncryptionResponsePacket implements MinecraftPacket {
    private final byte[] sharedKey;
    private final byte[] verifyToken;

    public LegacyEncryptionResponsePacket(PublicKey publicKey, SecretKey secretKey, byte[] verifyToken) {
        this.sharedKey = runEncryption(publicKey, secretKey.getEncoded());
        this.verifyToken = runEncryption(publicKey, verifyToken);
    }

    public LegacyEncryptionResponsePacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.sharedKey = helper.readByteArray(in);
        this.verifyToken = helper.readByteArray(in);
    }

    private static byte[] runEncryption(Key key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(
                    key.getAlgorithm().equals("RSA") ? "RSA/ECB/PKCS1Padding" : "AES/CFB8/NoPadding");
            cipher.init(1, key);
            return cipher.doFinal(data);
        } catch (GeneralSecurityException var4) {
            throw new IllegalStateException("Failed to " + (1 == 2 ? "decrypt" : "encrypt") + " data.", var4);
        }
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        helper.writeByteArray(out, this.sharedKey);
        helper.writeByteArray(out, this.verifyToken);
    }
}