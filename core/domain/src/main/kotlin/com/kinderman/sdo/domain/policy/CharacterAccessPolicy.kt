package com.kinderman.sdo.domain.policy

import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.UserSession

object CharacterAccessPolicy {
    fun canRead(session: UserSession, character: Character, isCampaignHistorian: Boolean = false): Boolean =
        session.isAdmin || isCampaignHistorian || character.ownerId == session.uid

    fun canEdit(session: UserSession, character: Character, isCampaignHistorian: Boolean = false): Boolean =
        session.isAdmin || isCampaignHistorian || character.ownerId == session.uid

    fun canDelete(session: UserSession, character: Character, isCampaignHistorian: Boolean = false): Boolean =
        session.isAdmin || character.ownerId == session.uid

    fun canChangePlayerLock(
        session: UserSession,
        character: Character,
        isCampaignHistorian: Boolean = false,
    ): Boolean = !session.isAdmin && !isCampaignHistorian &&
        character.ownerId == session.uid && character.lockType != CharacterLock.HISTORIAN

    fun canChangeHistorianLock(session: UserSession, isCampaignHistorian: Boolean = false): Boolean =
        session.isAdmin || isCampaignHistorian

    fun canTransferOwnership(session: UserSession, isCampaignResponsible: Boolean = false): Boolean =
        session.isAdmin || isCampaignResponsible
}
