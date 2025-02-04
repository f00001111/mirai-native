/*
 *
 * Mirai Native
 *
 * Copyright (C) 2020-2022 iTX Technologies
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author PeratX
 * @website https://github.com/iTXTech/mirai-native
 *
 */

package org.itxtech.mirainative.bridge

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
//import net.mamoe.mirai.Bot
import net.mamoe.mirai.Mirai
//import net.mamoe.mirai.contact.AvatarSpec
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.data.RequestEventData
//import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
//import net.mamoe.mirai.event.events.MemberJoinRequestEvent
//import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.action.FriendNudge
import net.mamoe.mirai.message.action.MemberNudge
//import net.mamoe.mirai.message.action.UserNudge
//import net.mamoe.mirai.message.data.AudioCodec
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.isContentEmpty
import org.itxtech.mirainative.Bridge
import org.itxtech.mirainative.MiraiNative
import org.itxtech.mirainative.MiraiNative.json
import org.itxtech.mirainative.fromNative
import org.itxtech.mirainative.manager.CacheManager
import org.itxtech.mirainative.manager.PluginManager
import org.itxtech.mirainative.message.ChainCodeConverter
import org.itxtech.mirainative.plugin.FloatingWindowEntry
import org.itxtech.mirainative.plugin.NativePlugin
import org.itxtech.mirainative.toNative
import org.itxtech.mirainative.util.ConfigMan
import java.io.File
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.io.use
import kotlin.text.toByteArray

object MiraiBridge {
    @Serializable
    data class ImageInfo(
        val id: String,
        val type: String,
        val height: Int,
        val width: Int,
        val size: Long,
        val url: String
    )

    @Serializable
    data class RecordInfo(
        val filename: String,
        val md5: ByteArray,
        val codec: String,
        val size: Long,
        val extra: ByteArray?,
        val url: String
    )

    val client = HttpClient(OkHttp) {
        install(UserAgent) {
            agent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36"
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 10000
            requestTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }

        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
    }

    fun logError(id: Int, e: String, err: Exception? = null) {
        val plugin = PluginManager.plugins[id]
        val info = if (plugin == null) {
            e.replace("%0", "$id（未找到该插件）")
        } else {
            e.replace("%0", plugin.detailedIdentifier)
        }
        if (err == null) {
            MiraiNative.logger.error(Exception(info))
        } else {
            MiraiNative.logger.error(info, err)
        }
    }

    fun verifyCall(pluginId: Int): Boolean {
        if (MiraiNative.botOnline) {
            return true
        }
        logError(pluginId, "插件 %0 在机器人登录之前调用了API。")
        return false
    }

    inline fun <reified T> call(
        exportName: String,
        pluginId: Int,
        defaultValue: T,
        errMsg: String = "",
        auth: Int = 0,
        block: () -> T
    ): T {
        if (ConfigMan.config.verboseNativeApiLog) {
            val plugin = PluginManager.plugins[pluginId]
            MiraiNative.logger.verbose("插件 ${plugin?.detailedIdentifier ?: "$pluginId（未找到该插件）"} 调用了 $exportName 。")
        }
        if (auth > 0 && !PluginManager.plugins[pluginId]!!.hasPermission(auth)) {
            logError(pluginId, "插件 %0 调用了 $exportName 但没有权限。")
            return defaultValue
        }
        if (verifyCall(pluginId)) {
            try {
                return block()
            } catch (e: Exception) {
                logError(pluginId, errMsg, e)
            }
        }
        return defaultValue
    }

    fun sendPrivateMessage(pluginId: Int, id: Long, message: String) =
        call("CQ_sendPrivateMsg", pluginId, 0, auth = 106) {
            val internalId = CacheManager.nextId()
            MiraiNative.launch {
                CacheManager.findUser(id)?.apply {
                    val chain = ChainCodeConverter.codeToChain(message, this)
                    if (chain.isContentEmpty()) {
                        MiraiNative.logger.warning("message is empty")
                    } else {
                        sendMessage(chain).apply {
                            CacheManager.cacheMessage(source, internalId, chain)
                        }
                    }
                }
            }
            return internalId
        }

    fun sendGroupMessage(pluginId: Int, id: Long, message: String) = call("CQ_sendGroupMsg", pluginId, 0, auth = 101) {
        val internalId = CacheManager.nextId()
        MiraiNative.launch {
            val contact = MiraiNative.bot.getGroup(id)
            val chain = ChainCodeConverter.codeToChain(message, contact)
            if (chain.isContentEmpty()) {
                MiraiNative.logger.warning("message is empty")
            } else {
                contact?.sendMessage(chain)?.apply {
                    CacheManager.cacheMessage(source, internalId, chain)
                }
            }
        }
        return internalId
    }

    fun setGroupBan(pluginId: Int, groupId: Long, memberId: Long, duration: Int) =
        call("CQ_setGroupBan", pluginId, 0, auth = 121) {
            MiraiNative.launch {
                if (duration == 0) {
                    MiraiNative.bot.getGroup(groupId)?.get(memberId)?.unmute()
                } else {
                    MiraiNative.bot.getGroup(groupId)?.get(memberId)?.mute(duration)
                }
            }
            return 0
        }

    fun setGroupCard(pluginId: Int, groupId: Long, memberId: Long, card: String) =
        call("CQ_setGroupCard", pluginId, 0, auth = 126) {
            MiraiNative.bot.getGroup(groupId)?.get(memberId)?.nameCard = card
            return 0
        }

    fun setGroupLeave(pluginId: Int, groupId: Long) = call("CQ_setGroupLeave", pluginId, 0, auth = 127) {
        MiraiNative.launch {
            MiraiNative.bot.getGroup(groupId)?.quit()
        }
        return 0
    }

    fun setGroupSpecialTitle(pluginId: Int, group: Long, member: Long, title: String) =
        call("CQ_setGroupSpecialTitleV2", pluginId, 0, auth = 128) {
            MiraiNative.bot.getGroup(group)?.get(member)?.specialTitle = title
            return 0
        }

    fun setGroupWholeBan(pluginId: Int, group: Long, enable: Boolean) =
        call("CQ_setGroupWholeBan", pluginId, 0, auth = 123) {
            MiraiNative.bot.getGroup(group)?.settings?.isMuteAll = enable
            return 0
        }

    fun getStrangerInfo(pluginId: Int, account: Long) = call("CQ_getStrangerInfo", pluginId, "", auth = 131) {
        return@call runBlocking {
            val profile = Mirai.queryProfile(MiraiNative.bot, account)
            return@runBlocking buildPacket {
                writeLong(account)
                writeString(profile.nickname)
                writeInt(profile.sex.ordinal)
                writeInt(profile.age)
                writeInt(profile.qLevel)
                writeString(profile.email)
                writeString(profile.sign)
            }.encodeBase64()
        }
    }

    fun getFriendList(pluginId: Int) = call("CQ_getFriendList", pluginId, "", auth = 162) {
        val list = MiraiNative.bot.friends
        return buildPacket {
            writeInt(list.size)
            list.forEach { qq ->
                writeShortLVPacket {
                    writeLong(qq.id)
                    writeString(qq.nick)
                    writeString(qq.remark)
                }
            }
        }.encodeBase64()
    }

    fun getGroupInfo(pluginId: Int, id: Long) = call("CQ_getGroupInfo", pluginId, "", auth = 132) {
        val info = MiraiNative.bot.getGroup(id)
        return if (info != null) {
            buildPacket {
                writeLong(id)
                writeString(info.name)
                writeInt(info.members.size + 1)
                //TODO: 上限
                writeInt(1000)
            }.encodeBase64()
        } else ""
    }

    fun getGroupList(pluginId: Int) = call("CQ_getGroupList", pluginId, "", auth = 161) {
        val list = MiraiNative.bot.groups
        return buildPacket {
            writeInt(list.size)
            list.forEach {
                writeShortLVPacket {
                    writeLong(it.id)
                    writeString(it.name)
                }
            }
        }.encodeBase64()
    }

    fun getGroupMemberInfo(pluginId: Int, groupId: Long, memberId: Long) =
        call("CQ_getGroupMemberInfoV2", pluginId, "", auth = 130) {
            val member = MiraiNative.bot.getGroup(groupId)?.get(memberId) ?: return ""
            return@call runBlocking {
                return@runBlocking buildPacket {
                    writeMember(member)
                }.encodeBase64()
            }
        }

    fun getGroupMemberList(pluginId: Int, groupId: Long) = call("CQ_getGroupMemberList", pluginId, "", auth = 160) {
        val group = MiraiNative.bot.getGroup(groupId) ?: return ""
        return@call runBlocking {
            return@runBlocking buildPacket {
                writeInt(group.members.size)
                group.members.forEach {
                    writeShortLVPacket {
                        writeMember(it)
                    }
                }
            }.encodeBase64()
        }
    }

    fun setGroupAddRequest(
        pluginId: Int,
        requestId: String,
        reqType: Int,
        type: Int,
        reason: String,
        blacklist: Boolean
    ) =
        call("CQ_setGroupAddRequestV3", pluginId, 0, auth = 151) {
            MiraiNative.launch {
                if (reqType == Bridge.REQUEST_GROUP_APPLY) {
                    (CacheManager.getEvent(requestId) as? RequestEventData.MemberJoinRequest)?.apply {
                        when (type) {//1通过，2拒绝
                            1 -> accept(MiraiNative.bot)
                            2 -> reject(MiraiNative.bot, message = reason, blackList = blacklist)
                            //3 -> ignore()
                        }
                    }
                } else {
                    (CacheManager.getEvent(requestId) as? RequestEventData.BotInvitedJoinGroupRequest)?.apply {
                        when (type) {//1通过，2拒绝
                            1 -> accept(MiraiNative.bot)
                            2 -> reject(MiraiNative.bot)
                        }
                    }
                }
            }
            return 0
        }

    fun setFriendAddRequest(pluginId: Int, requestId: String, type: Int, blacklist: Boolean) =
        call("CQ_setFriendAddRequestV2", pluginId, 0, auth = 150) {
            MiraiNative.launch {
                (CacheManager.getEvent(requestId) as? RequestEventData.NewFriendRequest)?.apply {
                    when (type) {//1通过，2拒绝
                        1 -> accept(MiraiNative.bot)
                        2 -> reject(MiraiNative.bot, blacklist)
                    }
                }
            }
            return 0
        }

    fun getImage(pluginId: Int, image: String): String =
        call("CQ_getImage", pluginId, "", "Error occurred when plugin %0 downloading image $image") {
            return@call runBlocking {
                /*val img = image.replace(".mnimg,type=flash", "").replace(".mnimg", "") // fix when get flash image
                val u = Image(img).queryUrl()
                if (u != "") {
                    client.prepareGet(u).execute { response ->
                        if (response.status.isSuccess()) {
                            val md = MessageDigest.getInstance("MD5")
                            val basename = MiraiNative.imageDataPath.absolutePath + File.separatorChar +
                                    BigInteger(1, md.digest(img.toByteArray()))
                                        .toString(16).padStart(32, '0')
                            val ext = when (response.headers[HttpHeaders.ContentType]) {
                                "image/gif" -> "gif"
                                "image/png" -> "png"
                                "image/jpeg" -> "jpg"
                                "image/x-bitmap" -> "bmp"
                                "image/tiff" -> "tiff"
                                else -> "jpg"
                            }

                            val file = File("$basename.$ext")
                            response.bodyAsChannel().copyAndClose(file.writeChannel())
                            file.absolutePath
                        } else {
                            ""
                        }
                    }
                }else {
                    ""
                }*/
                val img = image.replace(".mnimg,type=flash", ".mnimg") // fix when get flash image
                var f = File(MiraiNative.imageDataPath.absolutePath + File.separatorChar + img)
                if (f.exists()) {
                    val imgInfo = json.decodeFromString(ImageInfo.serializer(), f.readText())
                    client.prepareGet(imgInfo.url).execute { response ->
                        if (response.status.isSuccess()) {
                            val file = File(MiraiNative.imageDataPath.absolutePath + File.separatorChar + imgInfo.id)
                            response.bodyAsChannel().copyAndClose(file.writeChannel())
                            file.canonicalPath
                        } else {
                            ""
                        }
                    }
                } else {
                    val u = Image(img.replace(".mnimg", "")).queryUrl()
                    if (u != "") {
                        client.prepareGet(u).execute { response ->
                            if (response.status.isSuccess()) {
                                val file = File(MiraiNative.imageDataPath.absolutePath + File.separatorChar + img)
                                response.bodyAsChannel().copyAndClose(file.writeChannel())
                                file.canonicalPath
                            } else {
                                ""
                            }
                        }
                    } else {
                        ""
                    }
                }
            }
        }

    fun getRecord(pluginId: Int, record: String) =
        call("CQ_getRecordV3", pluginId, "", "Error occurred when plugin %0 downloading record $record", 30) {
            return@call runBlocking {
                /*val rec = CacheManager.getRecord(record.replace(".mnrec", ""))
                if (rec != null) {
                    val file = File(
                        MiraiNative.recDataPath.absolutePath + File.separatorChar +
                                BigInteger(1, rec.fileMd5).toString(16)
                                    .padStart(32, '0') + "."+rec.codec.formatName
                    )
                    client.prepareGet(rec.urlForDownload).execute { response ->
                        if (response.status.isSuccess()) {
                            response.bodyAsChannel().copyAndClose(file.writeChannel())
                            file.absolutePath
                        } else {
                            ""
                        }
                    }
                }else{
                    ""
                }*/
                val f = File(MiraiNative.recDataPath.absolutePath + File.separatorChar + record)
                if (!f.exists()) {
                    ""
                } else {
                    val recInfo = json.decodeFromString(RecordInfo.serializer(), f.readText())
                    client.prepareGet(recInfo.url).execute { response ->
                        if (response.status.isSuccess()) {
                            val file =
                                File(MiraiNative.recDataPath.absolutePath + File.separatorChar + recInfo.filename)
                            response.bodyAsChannel().copyAndClose(file.writeChannel())
                            file.canonicalPath
                        } else {
                            ""
                        }
                    }
                }
            }
        }

    fun setGroupAnonymousBan(pluginId: Int, group: Long, id: String, duration: Long) =
        call("CQ_setGroupAnonymousBan", pluginId, 0, auth = 124) {
            runBlocking {
                CacheManager.findAnonymousMember(group, id)?.mute(duration.toInt())
            }
            return 0
        }

    fun setGroupAdmin(pluginId: Int, group: Long, account: Long, admin: Boolean) =
        call("CQ_setGroupAdmin", pluginId, 0, auth = 122) {
            runBlocking {
                MiraiNative.bot.getGroup(group)?.getMember(account)?.modifyAdmin(admin)
            }
            return 0
        }

    fun addLog(pluginId: Int, priority: Int, type: String, content: String) {
        NativeLoggerHelper.log(PluginManager.plugins[pluginId]!!, priority, type, content)
    }

    fun getPluginDataDir(pluginId: Int) = call("CQ_getAppDirectory", pluginId, "") {
        return PluginManager.plugins[pluginId]!!.appDir.absolutePath + File.separatorChar
    }

    fun getLoginQQ(pluginId: Int) = call("CQ_getLoginQQ", pluginId, 0L) {
        return MiraiNative.bot.id
    }

    fun getLoginNick(pluginId: Int) = call("CQ_getLoginNick", pluginId, "") {
        return MiraiNative.bot.nick
    }

    fun recallMessage(pluginId: Int, id: Long) = call("CQ_deleteMsg", pluginId, -1, auth = 180) {
        return if (CacheManager.recall(id.toInt())) 0 else -1
    }

    fun updateFwe(pluginId: Int, fwe: FloatingWindowEntry) {
        val pk = ByteReadPacket(
            Bridge.callStringMethod(pluginId, fwe.status.function.toNative()).fromNative().decodeBase64Bytes()
        )
        fwe.data = pk.readString()
        fwe.unit = pk.readString()
        fwe.color = pk.readInt()
    }

    fun sendFriendNudge(pluginId: Int, account: Long, target: Long) = call("CQ_sendFriendNudge", pluginId, 0) {
        MiraiNative.launch {
            val friend = MiraiNative.bot.getFriend(account)
            val to = MiraiNative.bot.getFriend(target)
            if (friend != null && to != null) {
                FriendNudge(to).sendTo(friend)
            }
        }
        return 0
    }

    fun sendGroupNudge(pluginId: Int, group: Long, target: Long) = call("CQ_sendGroupNudge", pluginId, 0) {
        MiraiNative.launch {
            val contact = MiraiNative.bot.getGroup(group)
            val member = contact!!.get(target)
            if (member != null) {
                MemberNudge(member).sendTo(contact)
            }
        }
        return 0
    }

    fun getMemberHeadImg(pluginId: Int, group: Long, account: Long) = call("CQ_getMemberHeadImg", pluginId, "") {
        return@call runBlocking {
            val u = MiraiNative.bot.getGroup(group)?.getMember(account)?.avatarUrl ?: ""
            if (u != "") {
                client.prepareGet(u).execute { response ->
                    if (response.status.isSuccess()) {
                        val basename = MiraiNative.imageDataPath.absolutePath + File.separatorChar +
                                account.toString()
                        val ext = when (response.headers[HttpHeaders.ContentType]) {
                            "image/gif" -> "gif"
                            "image/png" -> "png"
                            "image/jpeg" -> "jpg"
                            "image/x-bitmap" -> "bmp"
                            "image/tiff" -> "tiff"
                            else -> "jpg"
                        }

                        val file = File("$basename.$ext")
                        response.bodyAsChannel().copyAndClose(file.writeChannel())
                        file.absolutePath
                    } else {
                        ""
                    }
                }
            } else {
                ""
            }
        }
    }

    fun getFriendHeadImg(pluginId: Int, account: Long) = call("CQ_getFriendHeadImg", pluginId, "") {
        return@call runBlocking {
            val u = MiraiNative.bot.getFriend(account)?.avatarUrl ?: ""
            if (u != "") {
                client.prepareGet(u).execute { response ->
                    if (response.status.isSuccess()) {
                        val basename = MiraiNative.imageDataPath.absolutePath + File.separatorChar +
                                account.toString()
                        val ext = when (response.headers[HttpHeaders.ContentType]) {
                            "image/gif" -> "gif"
                            "image/png" -> "png"
                            "image/jpeg" -> "jpg"
                            "image/x-bitmap" -> "bmp"
                            "image/tiff" -> "tiff"
                            else -> "jpg"
                        }

                        val file = File("$basename.$ext")
                        response.bodyAsChannel().copyAndClose(file.writeChannel())
                        file.absolutePath
                    } else {
                        ""
                    }
                }
            } else {
                ""
            }
        }
    }

    fun ByteReadPacket.readString(): String {
        return String(readBytes(readShort().toInt()))
    }

    private inline fun BytePacketBuilder.writeShortLVPacket(
        lengthOffset: ((Long) -> Long) = { it },
        builder: BytePacketBuilder.() -> Unit
    ): Int =
        BytePacketBuilder().apply(builder).build().use {
            val length = lengthOffset.invoke(it.remaining)
            writeShort(length.toShort())
            writePacket(it)
            return length.toInt()
        }

    private fun BytePacketBuilder.writeString(string: String) {
        val b = string.toByteArray(Charset.forName("GB18030"))
        writeShort(b.size.toShort())
        writeFully(b)
    }

    private fun BytePacketBuilder.writeBool(bool: Boolean) {
        writeInt(if (bool) 1 else 0)
    }

    private suspend fun BytePacketBuilder.writeMember(member: NormalMember) {
        // val profile = member.queryProfile()
        writeLong(member.group.id)
        writeLong(member.id)
        writeString(member.nick)
        writeString(member.nameCard)
        writeInt(0) // TODO: 性别
        writeInt(0) // TODO: 年龄
        writeString("未知") // TODO: 地区
        writeInt(member.joinTimestamp)
        writeInt(member.lastSpeakTimestamp)
        writeString("") // TODO: 等级名称
        writeInt(member.permission.ordinal + 1)
        writeBool(false) // TODO: 不良记录成员
        writeString(member.specialTitle)
        writeInt(-1) // TODO: 头衔过期时间
        writeBool(false) // TODO: 允许修改名片
    }
}

object NativeLoggerHelper {
    private const val LOG_DEBUG = 0
    private const val LOG_INFO = 10
    private const val LOG_INFO_SUCC = 11
    private const val LOG_INFO_RECV = 12
    private const val LOG_INFO_SEND = 13
    private const val LOG_WARNING = 20
    private const val LOG_ERROR = 21
    private const val LOG_FATAL = 22

    private fun getLogger() = MiraiNative.logger

    fun log(plugin: NativePlugin, priority: Int, type: String, content: String) {
        var c = "[" + plugin.getName()
        if ("" != type) {
            c += " $type"
        }
        c += "] $content"
        when (priority) {
            LOG_DEBUG -> getLogger().debug(c)
            LOG_INFO, LOG_INFO_RECV, LOG_INFO_SUCC, LOG_INFO_SEND -> getLogger().info(
                c
            )

            LOG_WARNING -> getLogger().warning(c)
            LOG_ERROR -> getLogger().error(c)
            LOG_FATAL -> getLogger().error("[FATAL] $c")
        }
    }
}
