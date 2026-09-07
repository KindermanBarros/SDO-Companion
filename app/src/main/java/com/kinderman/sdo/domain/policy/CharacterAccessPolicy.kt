package com.kinderman.sdo.domain.policy

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.UserSession

object CharacterAccessPolicy {
    fun canRead(session: UserSession, character: Character): Boolean =
        session.isMaster || character.ownerId == session.uid

    fun canEdit(session: UserSession, character: Character): Boolean =
        session.isMaster || character.ownerId == session.uid

    fun canDelete(session: UserSession, character: Character): Boolean =
        session.isMaster || (character.ownerId == session.uid && !character.isLocked)

    fun canChangePlayerLock(session: UserSession, character: Character): Boolean =
        !session.isMaster && character.ownerId == session.uid && character.lockType != CharacterLock.HISTORIAN

    fun canChangeHistorianLock(session: UserSession): Boolean = session.isMaster
}
