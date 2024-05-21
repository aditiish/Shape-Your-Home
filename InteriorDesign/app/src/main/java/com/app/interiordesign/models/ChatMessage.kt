package com.app.interiordesign.models


class ChatMessage(
        val id: String,
        val text: String,
        val fromId: String,
        val toId: String,
        val timestamp: Long, // message time
        val imageUrl: String
) {
    constructor() : this("", "", "", "", -1,"")
}