package io.tolgee.model

import org.hibernate.envers.Audited
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["code"], name = "invitation_code_unique")])
@Audited
data class Invitation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,
  var code: @NotBlank String? = null
) : AuditModel() {

  @OneToOne(mappedBy = "invitation", cascade = [CascadeType.ALL])
  var permission: Permission? = null

  @OneToOne(mappedBy = "invitation", cascade = [CascadeType.ALL])
  var organizationRole: OrganizationRole? = null

  constructor(id: Long?, @NotBlank code: String?, permission: Permission?) : this(id = id, code = code) {
    this.permission = permission
  }
}
