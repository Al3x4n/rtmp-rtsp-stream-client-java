/*
 * Copyright (C) 2021 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.rtsp.rtp.packets

import android.media.MediaCodec
import com.pedro.rtsp.rtsp.RtpFrame
import com.pedro.rtsp.utils.RtpConstants
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Created by pedro on 27/11/18.
 *
 * RFC 3640.
 */
class AacPacket(
  sampleRate: Int
): BasePacket(
  sampleRate.toLong(),
  RtpConstants.payloadType + RtpConstants.trackAudio
) {

  init {
    channelIdentifier = RtpConstants.trackAudio
  }

  override fun createAndSendPacket(
    byteBuffer: ByteBuffer,
    bufferInfo: MediaCodec.BufferInfo,
    callback: (RtpFrame) -> Unit
  ) {
    val length = bufferInfo.size - byteBuffer.position()
    if (length > 0) {
      val buffer = getBuffer(length + RtpConstants.RTP_HEADER_LENGTH + 4)
      byteBuffer.get(buffer, RtpConstants.RTP_HEADER_LENGTH + 4, length)
      val ts = bufferInfo.presentationTimeUs * 1000
      markPacket(buffer)
      val rtpTs = updateTimeStamp(buffer, ts)

      // AU-headers-length field: contains the size in bits of a AU-header
      // 13+3 = 16 bits -> 13bits for AU-size and 3bits for AU-Index / AU-Index-delta
      // 13 bits will be enough because ADTS uses 13 bits for frame length
      buffer[RtpConstants.RTP_HEADER_LENGTH] = 0.toByte()
      buffer[RtpConstants.RTP_HEADER_LENGTH + 1] = 0x10.toByte()

      // AU-size
      buffer[RtpConstants.RTP_HEADER_LENGTH + 2] = (length shr 5).toByte()
      buffer[RtpConstants.RTP_HEADER_LENGTH + 3] = (length shl 3).toByte()

      // AU-Index
      buffer[RtpConstants.RTP_HEADER_LENGTH + 3] = buffer[RtpConstants.RTP_HEADER_LENGTH + 3] and 0xF8.toByte()
      buffer[RtpConstants.RTP_HEADER_LENGTH + 3] = buffer[RtpConstants.RTP_HEADER_LENGTH + 3] or 0x00
      updateSeq(buffer)
      val rtpFrame = RtpFrame(buffer, rtpTs, RtpConstants.RTP_HEADER_LENGTH + length + 4, rtpPort, rtcpPort, channelIdentifier)
      callback(rtpFrame)
    }
  }
}