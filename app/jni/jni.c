#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <sys/types.h>
#include <inttypes.h>
#include <stdlib.h>
#include <openssl/aes.h>
#include <openssl/evp.h>
#include <unistd.h>
#include <dirent.h>


// int tonLibOnLoad(JavaVM *vm, JNIEnv *env);
/*
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
	JNIEnv *env = 0;
    srand(time(NULL));
    
	if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
		return -1;
	}
        
    // tonLibOnLoad(vm, env);
    
	return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {

}
*/

JNIEXPORT void Java_org_telegram_messenger_Utilities_aesIgeEncryptionByteArray(JNIEnv *env, jclass class, jbyteArray buffer, jbyteArray key, jbyteArray iv, jboolean encrypt, jint offset, jint length) {
    unsigned char *bufferBuff = (unsigned char *) (*env)->GetByteArrayElements(env, buffer, NULL);
    unsigned char *keyBuff = (unsigned char *) (*env)->GetByteArrayElements(env, key, NULL);
    unsigned char *ivBuff = (unsigned char *) (*env)->GetByteArrayElements(env, iv, NULL);

    AES_KEY akey;
    if (!encrypt) {
        AES_set_decrypt_key(keyBuff, 32 * 8, &akey);
        AES_ige_encrypt(bufferBuff, bufferBuff, length, &akey, ivBuff, AES_DECRYPT);
    } else {
        AES_set_encrypt_key(keyBuff, 32 * 8, &akey);
        AES_ige_encrypt(bufferBuff, bufferBuff, length, &akey, ivBuff, AES_ENCRYPT);
    }
    (*env)->ReleaseByteArrayElements(env, key, keyBuff, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, iv, ivBuff, 0);
    (*env)->ReleaseByteArrayElements(env, buffer, bufferBuff, 0);
}

JNIEXPORT jint Java_org_telegram_messenger_Utilities_pbkdf2(JNIEnv *env, jclass class, jbyteArray password, jbyteArray salt, jbyteArray dst, jint iterations) {
    jbyte *passwordBuff = (*env)->GetByteArrayElements(env, password, NULL);
    size_t passwordLength = (size_t) (*env)->GetArrayLength(env, password);
    jbyte *saltBuff = (*env)->GetByteArrayElements(env, salt, NULL);
    size_t saltLength = (size_t) (*env)->GetArrayLength(env, salt);
    jbyte *dstBuff = (*env)->GetByteArrayElements(env, dst, NULL);
    size_t dstLength = (size_t) (*env)->GetArrayLength(env, dst);

    int result = PKCS5_PBKDF2_HMAC((char *) passwordBuff, passwordLength, (uint8_t *) saltBuff, saltLength, (unsigned int) iterations, EVP_sha512(), dstLength, (uint8_t *) dstBuff);

    (*env)->ReleaseByteArrayElements(env, password, passwordBuff, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, salt, saltBuff, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, dst, dstBuff, 0);

    return result;
}

JNIEXPORT jint Java_org_telegram_messenger_Utilities_signEd25519hash(JNIEnv *env, jclass class, jbyteArray hash, jbyteArray secretKey, jbyteArray output) {
    uint8_t *hashBuff = (*env)->GetByteArrayElements(env, hash, NULL);
    uint8_t *secretBuff = (*env)->GetByteArrayElements(env, secretKey, NULL);
    uint8_t *outputBuff = (*env)->GetByteArrayElements(env, output, NULL);
    size_t len = 64;

    EVP_PKEY *pkey = EVP_PKEY_new_raw_private_key(EVP_PKEY_ED25519, NULL, secretBuff, 32);
    EVP_MD_CTX *md_ctx = EVP_MD_CTX_new();
    EVP_DigestSignInit(md_ctx, NULL, NULL, NULL, pkey);

    int r = (int) EVP_DigestSign(md_ctx, outputBuff, &len, hashBuff, 32);

    EVP_MD_CTX_free(md_ctx);
    EVP_PKEY_free(pkey);

    (*env)->ReleaseByteArrayElements(env, hash, hashBuff, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, secretKey, secretBuff, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, output, outputBuff, 0);

    return r;
}

JNIEXPORT jint Java_org_telegram_messenger_Utilities_privateToPublicX25519(JNIEnv *env, jclass class, jbyteArray secretKey, jbyteArray output) {
    uint8_t *secretBuff = (*env)->GetByteArrayElements(env, secretKey, NULL);
    uint8_t *outputBuff = (*env)->GetByteArrayElements(env, output, NULL);
    size_t len = 32;

    EVP_PKEY *pkey = EVP_PKEY_new_raw_private_key(EVP_PKEY_X25519, NULL, secretBuff, 32);

    int r = EVP_PKEY_get_raw_public_key(pkey, outputBuff, &len);

    EVP_PKEY_free(pkey);

    (*env)->ReleaseByteArrayElements(env, secretKey, secretBuff, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, output, outputBuff, 0);

    return r;
}