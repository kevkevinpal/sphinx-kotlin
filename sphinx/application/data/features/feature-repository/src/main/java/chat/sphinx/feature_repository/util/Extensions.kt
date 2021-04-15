package chat.sphinx.feature_repository.util

import chat.sphinx.concept_network_query_chat.model.ChatDto
import chat.sphinx.concept_network_query_contact.model.ContactDto
import chat.sphinx.concept_network_query_lightning.model.balance.BalanceDto
import chat.sphinx.concept_network_query_message.model.MessageDto
import chat.sphinx.conceptcoredb.SphinxDatabaseQueries
import chat.sphinx.wrapper_chat.*
import chat.sphinx.wrapper_common.chat.ChatId
import chat.sphinx.wrapper_common.chat.ChatUUID
import chat.sphinx.wrapper_common.contact.ContactId
import chat.sphinx.wrapper_common.invite.InviteId
import chat.sphinx.wrapper_common.invite.toInviteStatus
import chat.sphinx.wrapper_common.lightning.*
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.toDateTime
import chat.sphinx.wrapper_common.toPhotoUrl
import chat.sphinx.wrapper_common.toSeen
import chat.sphinx.wrapper_contact.*
import chat.sphinx.wrapper_invite.InviteString
import chat.sphinx.wrapper_lightning.NodeBalance
import chat.sphinx.wrapper_message.*
import chat.sphinx.wrapper_message.media.MediaToken
import chat.sphinx.wrapper_message.media.toMediaKey
import chat.sphinx.wrapper_message.media.toMediaKeyDecrypted
import chat.sphinx.wrapper_message.media.toMediaType
import chat.sphinx.wrapper_rsa.RsaPublicKey
import com.squareup.moshi.Moshi

@Suppress("NOTHING_TO_INLINE")
inline fun BalanceDto.toNodeBalanceOrNull(): NodeBalance? =
    try {
        toNodeBalance()
    } catch (e: IllegalArgumentException) {
        null
    }

@Suppress("NOTHING_TO_INLINE")
inline fun BalanceDto.toNodeBalance(): NodeBalance =
    NodeBalance(
        Sat(reserve),
        Sat(full_balance),
        Sat(balance),
        Sat(pending_open_balance),
    )

@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertChat(dto: ChatDto, moshi: Moshi) {
    chatUpsert(
        dto.name?.toChatName(),
        dto.photo_url?.toPhotoUrl(),
        dto.status.toChatStatus(),
        dto.contact_ids.map { ContactId(it) },
        dto.isMutedActual.toChatMuted(),
        dto.group_key?.toChatGroupKey(),
        dto.host?.toChatHost(),
        dto.price_per_message?.toSat(),
        dto.escrow_amount?.toSat(),
        dto.unlistedActual.toChatUnlisted(),
        dto.privateActual.toChatPrivate(),
        dto.owner_pub_key?.toLightningNodePubKey(),
        dto.seenActual.toSeen(),
        dto.meta?.toChatMetaDataOrNull(moshi),
        dto.my_photo_url?.toPhotoUrl(),
        dto.my_alias?.toChatAlias(),
        dto.pending_contact_ids?.map { ContactId(it) },
        ChatId(dto.id),
        ChatUUID(dto.uuid),
        dto.type.toChatType(),
        dto.created_at.toDateTime()
    )
}

/**
 * Always use [SphinxDatabaseQueries.transaction] with this extension function.
 * */
@Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection")
inline fun SphinxDatabaseQueries.upsertContact(dto: ContactDto) {

    if (dto.fromGroupActual) {
        return
    }

    contactUpsert(
        dto.route_hint?.toLightningRouteHint(),
        dto.public_key?.toLightningNodePubKey(),
        dto.node_alias?.toLightningNodeAlias(),
        dto.alias.toContactAlias(),
        dto.photo_url?.toPhotoUrl(),
        dto.privatePhotoActual.toPrivatePhoto(),
        dto.status.toContactStatus(),
        dto.contact_key?.let { RsaPublicKey(it.toCharArray()) },
        dto.device_id?.toDeviceId(),
        dto.updated_at.toDateTime(),
        dto.notification_sound?.toNotificationSound(),
        dto.tip_amount?.toSat(),
        dto.invite?.id?.let { InviteId(it) },
        dto.invite?.status?.toInviteStatus(),
        ContactId(dto.id),
        dto.isOwnerActual.toOwner(),
        dto.created_at.toDateTime()
    )
    dto.invite?.let { inviteDto ->
        inviteUpsert(
            InviteString(inviteDto.invite_string),
            inviteDto.invoice?.toLightningPaymentRequest(),
            inviteDto.status.toInviteStatus(),
            inviteDto.price?.toSat(),
            InviteId(inviteDto.id),
            ContactId(inviteDto.contact_id),
            inviteDto.created_at.toDateTime(),
        )
    }
}

@Suppress("SpellCheckingInspection")
fun SphinxDatabaseQueries.upsertMessage(dto: MessageDto) {

    // This handles the old method for sending boost payments (they were sent as
    // type 0 [MESSAGE]). Will update the MessageType to the correct value and
    // store the feed data properly for display.
    var type: MessageType = dto.type.toMessageType()
    val decryptedContent: String? = dto.messageContentDecrypted?.let { decrypted ->
        if (decrypted.contains("boost::{\"feedID\":")) {
            type = MessageType.Boost
            decrypted.split("::")[1]
        } else {
            decrypted
        }
    }

    val chatId: ChatId = dto.chat_id?.let {
        ChatId(it)
    } ?: dto.chat?.id?.let {
        ChatId(it)
    } ?: ChatId(ChatId.NULL_CHAT_ID.toLong())

    messageUpsert(
        dto.status.toMessageStatus(),
        dto.seenActual.toSeen(),
        dto.sender_alias?.toSenderAlias(),
        dto.sender_pic?.toPhotoUrl(),
        dto.original_muid?.toMessageMUID(),
        dto.reply_uuid?.toReplyUUID(),
        MessageId(dto.id),
        dto.uuid?.toMessageUUID(),
        chatId,
        type,
        ContactId(dto.sender),
        dto.receiver?.let { ContactId(it) },
        Sat(dto.amount),
        dto.payment_hash?.toLightningPaymentHash(),
        dto.payment_request?.toLightningPaymentRequest(),
        dto.date.toDateTime(),
        dto.expiration_date?.toDateTime(),
        dto.message_content?.toMessageContent(),
        decryptedContent?.toMessageContentDecrypted(),
    )

    dto.media_token?.let { mediaToken ->
        dto.media_type?.let { mediaType ->

            if (mediaToken.isEmpty() || mediaType.isEmpty()) return

            messageMediaUpsert(
                dto.media_key?.toMediaKey(),
                mediaType.toMediaType(),
                MediaToken(mediaToken),
                MessageId(dto.id),
                chatId,
                dto.mediaKeyDecrypted?.toMediaKeyDecrypted()
            )

        }
    }
}