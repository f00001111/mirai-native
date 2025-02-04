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

package org.itxtech.mirainative.message

import io.ktor.client.call.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.Bot
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.action.BotNudge
import net.mamoe.mirai.message.action.FriendNudge
import net.mamoe.mirai.message.action.MemberNudge
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.PokeMessage.Key.ChuoYiChuo
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import org.itxtech.mirainative.MiraiNative
import org.itxtech.mirainative.MiraiNative.eventDispatcher
import org.itxtech.mirainative.MiraiNative.json
import org.itxtech.mirainative.bridge.MiraiBridge
import org.itxtech.mirainative.manager.CacheManager
import org.itxtech.mirainative.message.ChainCodeConverter.useExternalResource
import org.itxtech.mirainative.util.Music
import org.itxtech.mirainative.util.NeteaseMusic
import org.itxtech.mirainative.util.QQMusic
import java.io.File

@OptIn(MiraiExperimentalApi::class)
object ChainCodeConverter {
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

    private val MSG_EMPTY = PlainText("")

    fun String.escape(comma: Boolean): String {
        val s = replace("&", "&amp;")
            .replace("[", "&#91;")
            .replace("]", "&#93;")
        return if (comma) s.replace(",", "&#44;") else s
    }

    private fun String.unescape(comma: Boolean): String {
        val s = replace("&amp;", "&")
            .replace("&#91;", "[")
            .replace("&#93;", "]")
        return if (comma) s.replace("&#44;", ",") else s
    }

    private fun String.toMap() = HashMap<String, String>().apply {
        this@toMap.split(",").forEach {
            val parts = it.split(delimiters = arrayOf("="), limit = 2)
            this[parts[0].trim()] = parts[1].unescape(true).trim()
        }
    }

    private suspend inline fun <T> String.useExternalResource(block: (ExternalResource) -> T): T {
        return MiraiBridge.client.get(this).body<ByteArray>().toExternalResource().use(block)
    }

    private inline fun <T> String.useFileResource(block: (ExternalResource) -> T): T {
        return File(this).toExternalResource().use(block)
    }

    private suspend fun String.toMessageInternal(contact: Contact?): Message {
        if (startsWith("[CQ:") && endsWith("]")) {
            val parts = substring(4, length - 1).split(delimiters = arrayOf(","), limit = 2)
            val args = if (parts.size == 2) {
                parts[1].toMap()
            } else {
                HashMap()
            }
            when (parts[0]) {
                "at" -> {
                    if (args["qq"] == "all") {
                        return AtAll
                    } else {
                        return if (contact !is Group) {
                            MiraiNative.logger.debug("不能在私聊中发送 At。")
                            MSG_EMPTY
                        } else {
                            val member = contact.get(args["qq"]!!.toLong())
                            if (member == null) {
                                MiraiNative.logger.debug("无法找到群员：${args["qq"]}")
                                MSG_EMPTY
                            } else {
                                At(member)
                            }
                        }
                    }
                }

                "face" -> {
                    return Face(args["id"]!!.toInt())
                }

                "emoji" -> {
                    return PlainText(String(Character.toChars(args["id"]!!.toInt())))
                }

                "image" -> {
                    var image: Image? = null
                    if (args.containsKey("file")) {
                        if (args["file"]!!.endsWith(".mnimg")) {
                            //Image(args["file"]!!.replace(".mnimg", ""))
                            var f = File(MiraiNative.imageDataPath.absolutePath + File.separatorChar + args["file"]!!)
                            if (!f.exists()) {
                                image = Image(args["file"]!!.replace(".mnimg", ""))
                            } else {
                                val imgInfo = json.decodeFromString(ImageInfo.serializer(), f.readText())
                                f = File(MiraiNative.imageDataPath.absolutePath + File.separatorChar + imgInfo.id)
                                image = if (!f.exists()) {
                                    imgInfo.url.useExternalResource { contact!!.uploadImage(it) }
                                } else {
                                    f.canonicalPath.useFileResource { contact!!.uploadImage(it) }
                                }
                            }
                        } else {
                            image = MiraiNative.getDataFile("image", args["file"]!!)?.use {
                                contact!!.uploadImage(it)
                            }
                        }
                    } else if (args.containsKey("url")) {
                        image = args["url"]!!.useExternalResource {
                            it.uploadAsImage(contact!!)
                        }
                    }
                    if (image != null) {
                        if (args["type"] == "flash") {
                            return image.flash()
                        }
                        return image
                    }
                    return MSG_EMPTY
                }

                "share" -> {
                    return RichMessageHelper.share(
                        args["url"]!!,
                        args["title"],
                        args["content"],
                        args["image"]
                    )
                }

                "contact" -> {
                    return if (args["type"] == "qq") {
                        RichMessageHelper.contactQQ(args["id"]!!.toLong())
                    } else {
                        RichMessageHelper.contactGroup(args["id"]!!.toLong())
                    }
                }

                "music" -> {
                    when (args["type"]) {
                        "qq" -> return QQMusic.send(args["id"]!!)
                        "163" -> return NeteaseMusic.send(args["id"]!!)
                        "custom" -> return Music.custom(
                            args["url"]!!,
                            args["audio"]!!,
                            args["title"]!!,
                            args["content"],
                            args["image"]
                        )
                    }
                }

                "shake" -> {
                    return ChuoYiChuo
                }

                "poke" -> {
                    PokeMessage.values.forEach {
                        if (it.pokeType == args["type"]!!.toInt() && it.id == args["id"]!!.toInt()) {
                            return it
                        }
                    }
                    return MSG_EMPTY
                }

                "xml" -> {
                    return xmlMessage(args["data"]!!)
                }

                "json" -> {
                    return jsonMessage(args["data"]!!)
                }

                "app" -> {
                    return LightApp(args["data"]!!)
                }

                "rich" -> {
                    return SimpleServiceMessage(args["id"]!!.toInt(), args["data"]!!)
                }

                "record" -> {
                    var rec: Audio? = null
                    if (contact is AudioSupported) {
                        if (args.containsKey("file")) {
                            if (args["file"]!!.endsWith(".mnrec")) {
                                //cacheManager.getRecord(args["file"]!!)
                                var f = File(MiraiNative.recDataPath.absolutePath + File.separatorChar + args["file"]!!)
                                if (!f.exists()) {
                                    MiraiNative.logger.debug("无法找到语音文件：${args["file"]}")
                                    return MSG_EMPTY
                                } else {
                                    val recInfo = json.decodeFromString(RecordInfo.serializer(), f.readText())
                                    f =
                                        File(MiraiNative.recDataPath.absolutePath + File.separatorChar + recInfo.filename)
                                    rec = if (f.exists()) {
                                        f.canonicalPath.useFileResource { contact.uploadAudio(it) }
                                    } else {
                                        recInfo.url.useExternalResource { contact.uploadAudio(it) }
                                    }
                                }
                            } else {
                                rec = MiraiNative.getDataFile("record", args["file"]!!)?.use {
                                    contact.uploadAudio(it)
                                }
                            }
                        } else if (args.containsKey("url")) {
                            rec = args["url"]!!.useExternalResource {
                                contact.uploadAudio(it)
                            }
                        }
                    }
                    return rec ?: MSG_EMPTY
                }

                "dice" -> {
                    return Dice(args["type"]!!.toInt())
                }

                "recall" -> {
                    if (args.containsKey("id")) {
                        CacheManager.getMessage(args["id"]!!.toInt())!!.recall()
                    }
                    return MSG_EMPTY
                }

                "reply" -> {
                    if (args.containsKey("id")) {
                        return CacheManager.getMessage(args["id"]!!.toInt())!!.quote()
                    }
                    return MSG_EMPTY
                }

                "nudge" -> {
                    if (args.containsKey("qq")) {
                        if (contact is Group) {
                            val member = contact.get(args["qq"]!!.toLong())
                            if (member != null) {
                                MemberNudge(member).sendTo(contact)
                            }
                        } else {
                            val friend = MiraiNative.bot.getFriend(args["qq"]!!.toLong())
                            if (friend != null) {
                                FriendNudge(friend).sendTo(contact!!)
                            }
                        }
                    } else {
                        BotNudge(Bot.instances.first()).sendTo(contact!!)
                    }
                }

                "mute" -> {
                    if (args.containsKey("qq")) {
                        if (contact is Group) {
                            val member = contact.get(args["qq"]!!.toLong())
                            if (member != null) {
                                if (member.permission.level >= contact.botPermission.level) {
                                    MiraiNative.logger.warning("权限不足，无法禁言该成员。")
                                    return MSG_EMPTY
                                }
                                if (args.containsKey("time")) {
                                    var time = args["time"]?.toInt() ?: 0
                                    if (time <= 0) {
                                        member.unmute()
                                        return MSG_EMPTY
                                    }
                                    if (time > 2592000) {
                                        time = 2592000
                                    }
                                    member.mute(time)
                                }
                            }
                        }
                    }
                    return MSG_EMPTY
                }

                "unmute" -> {
                    if (args.containsKey("qq")) {
                        if (contact is Group) {
                            val member = contact.get(args["qq"]!!.toLong())
                            if (member != null) {
                                if (member.permission.level >= contact.botPermission.level) {
                                    MiraiNative.logger.warning("权限不足，无法解除禁言该成员。")
                                    return MSG_EMPTY
                                }
                                member.unmute()
                            }
                        }
                    }
                    return MSG_EMPTY
                }

                else -> {
                    MiraiNative.logger.warning("不支持的 CQ码：${parts[0]}")
                }
            }
            return MSG_EMPTY
        }
        return PlainText(unescape(false))
    }

    @OptIn(MiraiInternalApi::class)
    fun chainToCode(chain: MessageChain): String {
        return chain.joinToString(separator = "") {
            when (it) {
                is At -> "[CQ:at,qq=${it.target}]"
                is AtAll -> "[CQ:at,qq=all]"
                is PlainText -> it.content.escape(false)
                is Face -> "[CQ:face,id=${it.id}]"
                is VipFace -> "[CQ:vipface,id=${it.kind.id},name=${it.kind.name},count=${it.count}]"
                is Image -> {
                    MiraiNative.launch {
                        File(MiraiNative.imageDataPath.absolutePath + File.separatorChar + it.imageId + ".mnimg").writeText(
                            json.encodeToString(
                                ImageInfo.serializer(),
                                ImageInfo(
                                    it.imageId,
                                    it.imageType.formatName,
                                    it.height,
                                    it.width,
                                    it.size,
                                    it.queryUrl()
                                )
                            )
                        )
                    }
                    return@joinToString "[CQ:image,file=${it.imageId}.mnimg]"
                } // Real file not supported
                is RichMessage -> {
                    val content = it.content.escape(true)
                    return@joinToString when (it) {
                        is LightApp -> "[CQ:app,data=$content]"
                        is ServiceMessage -> when (it.serviceId) {
                            60 -> "[CQ:xml,data=$content]"
                            1 -> "[CQ:json,data=$content]"
                            else -> "[CQ:rich,data=${content},id=${it.serviceId}]"
                        }

                        else -> "[CQ:rich,data=$content]" // Which is impossible
                    }
                }

                is OnlineAudio -> {
                    File(MiraiNative.imageDataPath.absolutePath + File.separatorChar + it.filename + ".mnrec").writeText(
                        json.encodeToString(
                            RecordInfo.serializer(),
                            RecordInfo(
                                it.filename,
                                it.fileMd5,
                                it.codec.formatName,
                                it.fileSize,
                                it.extraData,
                                it.urlForDownload
                            )
                        )
                    )
                    return@joinToString "[CQ:record,file=${it.filename}.mnrec]"
                }

                is OfflineAudio -> "[CQ:record,file=${it.filename}.mnrec]"
                is PokeMessage -> "[CQ:poke,id=${it.id},type=${it.pokeType},name=${it.name}]"
                is FlashImage -> "[CQ:image,file=${it.image.imageId}.mnimg,type=flash]"
                is MarketFace -> "[CQ:bface,id=${it.id},name=${it.name}]"
                is Dice -> "[CQ:dice,type=${it.value}]"
                is QuoteReply -> "[CQ:reply,id=${it.source.ids[0]},from=${it.source.fromId}]"
                else -> ""//error("不支持的消息类型：${it::class.simpleName}")
            }
        }
    }

    suspend fun codeToChain(message: String, contact: Contact?): MessageChain {
        return buildMessageChain {
            if (message.contains("[CQ:")) {
                var interpreting = false
                val sb = StringBuilder()
                var index = 0
                message.forEach { c: Char ->
                    if (c == '[') {
                        if (interpreting) {
                            MiraiNative.logger.error("CQ消息解析失败：$message，索引：$index")
                            return@forEach
                        } else {
                            interpreting = true
                            if (sb.isNotEmpty()) {
                                val lastMsg = sb.toString()
                                sb.delete(0, sb.length)
                                +lastMsg.toMessageInternal(contact)
                            }
                            sb.append(c)
                        }
                    } else if (c == ']') {
                        if (!interpreting) {
                            MiraiNative.logger.error("CQ消息解析失败：$message，索引：$index")
                            return@forEach
                        } else {
                            interpreting = false
                            sb.append(c)
                            if (sb.isNotEmpty()) {
                                val lastMsg = sb.toString()
                                sb.delete(0, sb.length)
                                +lastMsg.toMessageInternal(contact)
                            }
                        }
                    } else {
                        sb.append(c)
                    }
                    index++
                }
                if (sb.isNotEmpty()) {
                    +sb.toString().toMessageInternal(contact)
                }
            } else {
                +PlainText(message.unescape(false))
            }
        }
    }
}
