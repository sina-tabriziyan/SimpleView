/**
 * Created by ST on 5/21/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.enms

enum class FileType(val directoryName: String, val fileExtension: String) {
    IMAGE("Teamyar/Teamyar Images", ".jpg"),
    VIDEO("Teamyar/Teamyar Videos", ".mp4"),
    DOCUMENT("Teamyar/Teamyar Documents", ".pdf"),
    AUDIO("Teamyar/Teamyar Audio", ".mp3")
}
