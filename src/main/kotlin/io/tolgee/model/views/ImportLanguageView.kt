package io.tolgee.model.views

interface ImportLanguageView {
    val id: Long
    val name: String
    val existingLanguageId: Long?
    val existingLanguageAbbreviation: String?
    val existingLanguageName: String?
    val importFileName: String
    val importFileId: Long
    val importFileIssueCount: Int
    val totalCount: Int
    val conflictCount: Int
    val resolvedCount: Int
}