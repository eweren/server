package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.api.v2.hateoas.organization.LanguageModel
import io.tolgee.model.Permission
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.*

@Relation(collectionRelation = "invitations", itemRelation = "invitation")
open class ProjectInvitationModel(
  val id: Long,
  val code: String,
  val type: Permission.ProjectPermissionType,
  val languages: List<LanguageModel>?,
  val createdAt: Date,
  val invitedUserName: String?,
  val invitedUserEmail: String?
) : RepresentationModel<ProjectInvitationModel>()