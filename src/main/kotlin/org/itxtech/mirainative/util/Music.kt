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

package org.itxtech.mirainative.util

import io.ktor.client.call.body
import io.ktor.client.call.body
import io.ktor.client.call.body
import io.ktor.client.statement.*
import io.ktor.client.request.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MusicKind
import net.mamoe.mirai.message.data.MusicShare
import net.mamoe.mirai.message.data.SimpleServiceMessage
import net.mamoe.mirai.utils.MiraiExperimentalApi
import org.itxtech.mirainative.bridge.MiraiBridge
import org.itxtech.mirainative.message.xmlMessage

abstract class MusicProvider {
    val http = MiraiBridge.client

    abstract suspend fun send(id: String): Message
}

object Music {
    @OptIn(MiraiExperimentalApi::class)
    fun custom(url: String, audio: String, title: String, content: String?, image: String?) =
        xmlMessage(
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                    "<msg serviceID=\"2\" templateID=\"1\" action=\"web\" brief=\"[分享] $title\" sourceMsgId=\"0\" " +
                    "url=\"$url\" " +
                    "flag=\"0\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"2\">" +
                    "<audio cover=\"$image\" " +
                    "src=\"$audio\" /><title>$title</title><summary>$content</summary></item><source name=\"Mirai\" " +
                    "icon=\"https://i.gtimg.cn/open/app_icon/01/07/98/56/1101079856_100_m.png\" " +
                    "url=\"http://web.p.qq.com/qqmpmobile/aio/app.html?id=1101079856\" action=\"app\" " +
                    "a_actionData=\"com.tencent.qqmusic\" i_actionData=\"tencent1101079856://\" appid=\"1101079856\" /></msg>"
        )
}

object QQMusic : MusicProvider() {
    suspend fun search(name: String, page: Int, cnt: Int): JsonElement {
        val result =
            http.get("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?aggr=1&cr=1&flag_qc=0&p=$page&n=$cnt&w=$name")
                .bodyAsText()
        return Json.parseToJsonElement(result.substring(8, result.length - 1))
    }

    private suspend fun getPlayUrl(mid: String): String {
        val result = http.get(
            "https://c.y.qq.com/base/fcgi-bin/fcg_music_express_mobile3.fcg?&jsonpCallback=MusicJsonCallback&cid=205361747&songmid=" +
                    mid + "&filename=C400" + mid + ".m4a&guid=7549058080"
        ).bodyAsText()
        val json =
            Json.parseToJsonElement(result).jsonObject.getValue("data").jsonObject.getValue("items").jsonArray[0].jsonObject
        if (json["subcode"]?.jsonPrimitive?.int == 0) {
            return "http://aqqmusic.tc.qq.com/amobile.music.tc.qq.com/C400$mid.m4a?guid=7549058080&amp;vkey=${json["vkey"]!!.jsonPrimitive.content}&amp;uin=0&amp;fromtag=38"
        }
        return ""
    }

    private suspend fun getSongInfo(id: String = "", mid: String = ""): JsonObject {
        val result = http.get(
            "https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&inCharset=utf8&outCharset=utf-8&notice=0&" +
                    "platform=yqq.json&needNewCode=0&data=" +
                    "{%22comm%22:{%22ct%22:24,%22cv%22:0},%22songinfo%22:{%22method%22:%22get_song_detail_yqq%22,%22param%22:" +
                    "{%22song_type%22:0,%22song_mid%22:%22$mid%22,%22song_id%22:$id},%22module%22:%22music.pf_song_detail_svr%22}}"
        ).bodyAsText()
        return Json.parseToJsonElement(result).jsonObject.getValue("songinfo").jsonObject.getValue("data").jsonObject
    }

    override suspend fun send(id: String): Message {
        val info = getSongInfo(id)
        val trackInfo = info.getValue("track_info").jsonObject
        val url = getPlayUrl(trackInfo.getValue("file").jsonObject["media_mid"]!!.jsonPrimitive.content)
        val albumId = trackInfo.getValue("album").jsonObject["id"]!!.jsonPrimitive.content
        return MusicShare(
            kind = MusicKind.QQMusic,
            title = trackInfo["name"]!!.jsonPrimitive.content,
            summary = trackInfo.getValue("singer").jsonArray[0].jsonObject["name"]!!.jsonPrimitive.content,
            jumpUrl = "https://i.y.qq.com/v8/playsong.html?_wv=1&amp;songid=$id&amp;souce=qqshare&amp;source=qqshare&amp;ADTAG=qqshare",
            pictureUrl = "http://imgcache.qq.com/music/photo/album_500/${albumId.substring(albumId.length - 2)}/500_albumpic_${albumId}_0.jpg",
            musicUrl = url,
        )
        /*return toXmlMessage(
            trackInfo["name"]!!.jsonPrimitive.content,
            trackInfo.getValue("singer").jsonArray[0].jsonObject["name"]!!.jsonPrimitive.content,
            id,
            trackInfo.getValue("album").jsonObject["id"]!!.jsonPrimitive.content,
            url
        )*/
    }

    /*fun toXmlMessage(song: String, singer: String, songId: String, albumId: String, playUrl: String) =
        xmlMessage(
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                    "<msg serviceID=\"2\" templateID=\"1\" action=\"web\" brief=\"[分享] $song\" sourceMsgId=\"0\" " +
                    "url=\"https://i.y.qq.com/v8/playsong.html?_wv=1&amp;songid=$songId&amp;souce=qqshare&amp;source=qqshare&amp;ADTAG=qqshare\" " +
                    "flag=\"0\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"2\">" +
                    "<audio cover=\"http://imgcache.qq.com/music/photo/album_500/${albumId.substring(albumId.length - 2)}/500_albumpic_${albumId}_0.jpg\" " +
                    "src=\"$playUrl\" /><title>$song</title><summary>$singer</summary></item><source name=\"QQ音乐\" " +
                    "icon=\"https://i.gtimg.cn/open/app_icon/01/07/98/56/1101079856_100_m.png\" " +
                    "url=\"http://web.p.qq.com/qqmpmobile/aio/app.html?id=1101079856\" action=\"app\" " +
                    "a_actionData=\"com.tencent.qqmusic\" i_actionData=\"tencent1101079856://\" appid=\"1101079856\" /></msg>"
        )*/
}

@OptIn(MiraiExperimentalApi::class)
object NeteaseMusic : MusicProvider() {
    override suspend fun send(id: String): SimpleServiceMessage {
        TODO("Not yet implemented")
    }
}
