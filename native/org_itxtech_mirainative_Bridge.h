/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_itxtech_mirainative_Bridge */

#ifndef _Included_org_itxtech_mirainative_Bridge
#define _Included_org_itxtech_mirainative_Bridge
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    shutdown
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_shutdown
  (JNIEnv *, jclass);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    setCurrentDirectory
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_setCurrentDirectory
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    loadNativePlugin
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_loadNativePlugin
  (JNIEnv *, jclass, jbyteArray, jint);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    freeNativePlugin
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_freeNativePlugin
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvPrivateMessage
 * Signature: (I[BIIJ[BI)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvPrivateMessage
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jbyteArray, jint);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupMessage
 * Signature: (I[BIIJJ[B[BI)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupMessage
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jlong, jbyteArray, jbyteArray, jint);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupAdmin
 * Signature: (I[BIIJJ)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupAdmin
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jlong);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupMember
 * Signature: (I[BIIJJJ)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupMember
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jlong, jlong);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupBan
 * Signature: (I[BIIJJJJ)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupBan
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jlong, jlong, jlong);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvRequestAddGroup
 * Signature: (I[BIIJJ[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvRequestAddGroup
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvRequestAddFriend
 * Signature: (I[BIIJ[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvRequestAddFriend
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvFriendAdd
 * Signature: (I[BIIJ)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvFriendAdd
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    callIntMethod
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_callIntMethod
  (JNIEnv *, jclass, jint, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    callStringMethod
 * Signature: (I[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_itxtech_mirainative_Bridge_callStringMethod
  (JNIEnv *, jclass, jint, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    processMessage
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_itxtech_mirainative_Bridge_processMessage
  (JNIEnv *, jclass);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvFriendRecall
 * Signature: (I[BIIJ[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvFriendRecall
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupRecall
 * Signature: (I[BIIJJJ[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupRecall
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jlong, jlong, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupNudge
 * Signature: (I[BIJJJ[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupNudge
  (JNIEnv *, jclass, jint, jbyteArray, jint, jlong, jlong, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvFriendNudge
 * Signature: (I[BIJJ[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvFriendNudge
  (JNIEnv *, jclass, jint, jbyteArray, jint, jlong, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvFriendNickChanged
 * Signature: (I[BIJ[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvFriendNickChanged
  (JNIEnv *, jclass, jint, jbyteArray, jint, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupNameChanged
 * Signature: (I[BIJJ[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupNameChanged
  (JNIEnv *, jclass, jint, jbyteArray, jint, jlong, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupMemberCardChanged
 * Signature: (I[BIJJ[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupMemberCardChanged
  (JNIEnv *, jclass, jint, jbyteArray, jint, jlong, jlong, jbyteArray, jbyteArray);

/*
 * Class:     org_itxtech_mirainative_Bridge
 * Method:    pEvGroupMemberHonorChanged
 * Signature: (I[BIIJJI)I
 */
JNIEXPORT jint JNICALL Java_org_itxtech_mirainative_Bridge_pEvGroupMemberHonorChanged
  (JNIEnv *, jclass, jint, jbyteArray, jint, jint, jlong, jlong, jint);

#ifdef __cplusplus
}
#endif
#endif
