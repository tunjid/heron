package com.tunjid.heron.fakes

import com.tunjid.heron.data.core.models.Message
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.MessageId

fun sampleMessageReactionAdd(): Message.UpdateReaction.Add {
    return Message.UpdateReaction.Add(
        value = "like",
        messageId = MessageId("msg-1"),
        convoId = ConversationId("convo-1"),
    )
}

fun sampleMessageReactionRemove(): Message.UpdateReaction.Remove {
    return Message.UpdateReaction.Remove(
        value = "like",
        messageId = MessageId("msg-1"),
        convoId = ConversationId("convo-1"),
    )
}
