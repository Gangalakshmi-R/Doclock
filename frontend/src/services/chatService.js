import api from "./api";


// =====================================================
// SEND MESSAGE
// =====================================================

export async function sendMessage(
    question,
    conversationId = null
) {

    const body = {
        question
    };

    if (conversationId !== null) {

        body.conversationId =
            conversationId;
    }

    const response = await api.post(
        "/chat",
        body
    );

    return response.data;
}


// =====================================================
// GET CONVERSATIONS
// =====================================================

export async function getConversations() {

    const response =
        await api.get(
            "/conversations"
        );

    return response.data;
}


// =====================================================
// GET ONE CONVERSATION
// =====================================================

export async function getConversation(id) {

    const response =
        await api.get(
            `/conversations/${id}`
        );

    return response.data;
}


// =====================================================
// DELETE CONVERSATION
// =====================================================

export async function deleteConversation(id) {

    const response =
        await api.delete(
            `/conversations/${id}`
        );

    return response.data;
}